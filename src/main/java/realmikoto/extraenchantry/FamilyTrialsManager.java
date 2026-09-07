package realmikoto.extraenchantry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 八系共鸣试炼（1.3.0「铭刻与试炼」§2.3）。
 *
 * 计数前提（P0 验收 6.4）：对应家族 FULL 且被设为主调；失去 FULL 暂停——
 * 连续窗口类条件中断即重置（提示一次），已完成进度（进度树）永久保留。
 * 所有计时/距离/伤害判定以服务端为准，时间一律用服务器游戏时间（不使用系统毫秒）。
 *
 * <pre>
 * 灵魂·不息  单次隐身持续期间连续击杀 5 个敌对生物（AFTER_DEATH 钩子）
 * 风暴·逐雷  雷暴中 120 秒内移动 600 格且期间不乘坐载具（秒级采样）
 * 锋刃·百炼  连击窗口内增伤叠至上限并击败生命值 ≥100 的目标（命中/击杀钩子）
 * 守护·不动  30 秒内承受至少 80 点原始伤害且位移不超过 8 格并存活（hurtServer 钩子）
 * 自然·复苏  单次站定期间累计恢复 30 HP，期间不得主动攻击（回血/移动/攻击回调）
 * 水·深潜    不换气连续潜水 180 秒并击败一只远古守卫者（秒级采样 + AFTER_DEATH）
 * 风·无坠    从至少 80 格高度落地并存活，落地前不得使用鞘翅或缓降（逐 tick 采样）
 * 火焰·浴火  在熔岩中连续停留 60 秒并移动至少 100 格（秒级采样）
 * </pre>
 *
 * 运行时状态按活跃玩家创建，离线即释放（每 30 秒清理一次）。
 */
public final class FamilyTrialsManager {

	private FamilyTrialsManager() {
	}

	// ============ 每玩家运行时状态 ============

	private static final Map<UUID, TrialState> STATES = new ConcurrentHashMap<>();

	private static final class TrialState {
		// 灵魂：单次隐身期内连续击杀数
		int soulKills;
		// 锋刃：连击增伤叠至上限的时刻（gameTime，-1 = 无）
		long bladeMaxSince = -1L;
		// 风暴：雷暴窗口起始 tick（-1 = 无）+ 累计水平移动距离
		long stormStart = -1L;
		double stormDistance;
		// 守护：窗口起始 tick（-1 = 无）+ 累计原始伤害 + 位移锚点
		long guardStart = -1L;
		float guardDamage;
		Vec3 guardAnchor;
		// 自然：单次站定期间累计回复量
		float natureHeal;
		// 水：连续潜水起始 tick（-1 = 无）+ 时长已达标标记
		long waterDiveStart = -1L;
		boolean waterReady;
		// 风：本次下落采样的最大 fallDistance
		double windMaxFall;
		// 火焰：熔岩窗口起始 tick（-1 = 无）+ 累计水平移动距离
		long fireStart = -1L;
		double fireDistance;
		// 秒级采样通用：上次位置（风暴/火焰距离采样共用，每秒刷新）
		Vec3 lastPos;
	}

	private static TrialState state(ServerPlayer player) {
		return STATES.computeIfAbsent(player.getUUID(), id -> new TrialState());
	}

	/** 单家族窗口清理（失去资格 / 中断重置均走这里，不误伤其他家族） */
	private static void clearFamily(TrialState s, FamilyResonanceManager.Family family) {
		switch (family) {
			case SOUL -> s.soulKills = 0;
			case BLADE -> s.bladeMaxSince = -1L;
			case STORM -> {
				s.stormStart = -1L;
				s.stormDistance = 0.0D;
			}
			case GUARD -> {
				s.guardStart = -1L;
				s.guardDamage = 0.0F;
				s.guardAnchor = null;
			}
			case NATURE -> s.natureHeal = 0.0F;
			case WATER -> {
				s.waterDiveStart = -1L;
				s.waterReady = false;
			}
			case WIND -> s.windMaxFall = 0.0D;
			case FIRE -> {
				s.fireStart = -1L;
				s.fireDistance = 0.0D;
			}
		}
	}

	// ============ 主循环（ServerTickEvents 注册） ============

	public static void tick(MinecraftServer server) {
		long gameTime = server.overworld().getGameTime();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.isDeadOrDying() || player.isSpectator()) {
				continue;
			}
			// 风试炼：逐 tick 采样 fallDistance（秒级采样会错过 80 格坠落的峰值）
			tickWind(player);
			// 其余试炼：每秒结算
			if (gameTime % 20L == 0L) {
				tickSecondly(player, gameTime);
			}
		}
		// 离线释放运行时缓存（每 30 秒）
		if (gameTime % 600L == 0L) {
			Set<UUID> online = new HashSet<>();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				online.add(player.getUUID());
			}
			STATES.keySet().removeIf(uuid -> !online.contains(uuid));
		}
	}

	/** 秒级结算：风暴/守护/水/火焰四项窗口型试炼（自然走回调，锋刃/灵魂走击杀钩子） */
	private static void tickSecondly(ServerPlayer player, long gameTime) {
		TrialState s = state(player);
		Vec3 pos = player.position();
		// 本次秒级采样的水平位移（无论窗口是否激活都刷新 lastPos，避免重开窗口时吃进陈旧位移）
		double moved = s.lastPos == null ? 0.0D : Math.sqrt(
				(pos.x - s.lastPos.x) * (pos.x - s.lastPos.x) + (pos.z - s.lastPos.z) * (pos.z - s.lastPos.z));
		s.lastPos = pos;

		// ---- 风暴·逐雷：雷暴中计时 + 累计水平距离，不乘坐载具 ----
		FamilyResonanceManager.Family storm = FamilyResonanceManager.Family.STORM;
		if (eligible(player, storm)) {
			ResonanceConfig.FamilyRules rules = ResonanceConfig.rules(storm);
			if (player.level().isThundering() && !player.isPassenger()) {
				if (s.stormStart < 0L) {
					s.stormStart = gameTime;
					s.stormDistance = 0.0D;
				}
				s.stormDistance += moved;
				if (gameTime - s.stormStart >= rules.trialStormSeconds() * 20L) {
					if (s.stormDistance >= rules.trialStormDistance()) {
						complete(player, storm);
					} else {
						clearFamily(s, storm);
						notifyReset(player, storm);
					}
				}
			} else if (s.stormStart >= 0L) {
				// 天气中断或上了载具 → 窗口重置（提示一次）
				clearFamily(s, storm);
				notifyReset(player, storm);
			}
		} else {
			clearFamily(s, storm);
		}

		// ---- 守护·不动：窗口到期判定（累计伤害在 onHurtServer 中累加） ----
		FamilyResonanceManager.Family guard = FamilyResonanceManager.Family.GUARD;
		if (eligible(player, guard)) {
			ResonanceConfig.FamilyRules rules = ResonanceConfig.rules(guard);
			if (s.guardStart >= 0L) {
				if (gameTime - s.guardStart >= rules.trialGuardSeconds() * 20L) {
					if (s.guardDamage >= rules.trialGuardDamage()) {
						complete(player, guard);
					} else {
						// 伤害不足：静默重启窗口（避免反复提示）
						s.guardStart = gameTime;
						s.guardDamage = 0.0F;
						s.guardAnchor = player.position();
					}
				} else if (s.guardAnchor != null
						&& player.position().distanceTo(s.guardAnchor) > rules.trialGuardMaxDisplacement()) {
					// 位移超标：明确重置提示
					clearFamily(s, guard);
					notifyReset(player, guard);
				}
			}
		} else {
			clearFamily(s, guard);
		}

		// ---- 水·深潜：连续保持眼睛在水中；浮出（换气）即中断 ----
		FamilyResonanceManager.Family water = FamilyResonanceManager.Family.WATER;
		if (eligible(player, water)) {
			ResonanceConfig.FamilyRules rules = ResonanceConfig.rules(water);
			if (player.isEyeInFluid(FluidTags.WATER)) {
				if (s.waterDiveStart < 0L) {
					s.waterDiveStart = gameTime;
				}
				if (!s.waterReady && gameTime - s.waterDiveStart >= rules.trialWaterSeconds() * 20L) {
					s.waterReady = true;
					player.sendSystemMessage(Component.translatable("message.extra-enchantry.trial.water_ready",
							Component.translatable("family.extra-enchantry.water")));
				}
			} else if (s.waterDiveStart >= 0L) {
				clearFamily(s, water);
				notifyReset(player, water);
			}
		} else {
			clearFamily(s, water);
		}

		// ---- 火焰·浴火：熔岩中计时 + 累计水平距离 ----
		FamilyResonanceManager.Family fire = FamilyResonanceManager.Family.FIRE;
		if (eligible(player, fire)) {
			ResonanceConfig.FamilyRules rules = ResonanceConfig.rules(fire);
			if (player.isInLava()) {
				if (s.fireStart < 0L) {
					s.fireStart = gameTime;
					s.fireDistance = 0.0D;
				}
				s.fireDistance += moved;
				if (gameTime - s.fireStart >= rules.trialFireSeconds() * 20L) {
					if (s.fireDistance >= rules.trialFireDistance()) {
						complete(player, fire);
					} else {
						clearFamily(s, fire);
						notifyReset(player, fire);
					}
				}
			} else if (s.fireStart >= 0L) {
				clearFamily(s, fire);
				notifyReset(player, fire);
			}
		} else {
			clearFamily(s, fire);
		}
	}

	/** 风·无坠：逐 tick 采样最大 fallDistance，落地判定（自然试炼不在此，见回调） */
	private static void tickWind(ServerPlayer player) {
		FamilyResonanceManager.Family wind = FamilyResonanceManager.Family.WIND;
		TrialState s = state(player);
		if (!eligible(player, wind)) {
			s.windMaxFall = 0.0D;
			return;
		}
		if (player.isFallFlying() || player.hasEffect(MobEffects.SLOW_FALLING)) {
			// 使用鞘翅或缓降 → 本次下落作废（提示一次）
			if (s.windMaxFall > 0.0D) {
				s.windMaxFall = 0.0D;
				notifyReset(player, wind);
			}
			return;
		}
		double fall = player.fallDistance;
		if (fall > 0.0D) {
			s.windMaxFall = Math.max(s.windMaxFall, fall);
		} else if (player.onGround() && s.windMaxFall > 0.0D) {
			double max = s.windMaxFall;
			s.windMaxFall = 0.0D;
			if (max >= ResonanceConfig.rules(wind).trialWindMinFallDistance() && player.isAlive()) {
				complete(player, wind);
			}
		}
	}

	// ============ 事件钩子（由 FamilyResonanceManager / LivingEntityMixin 调用） ============

	/** 灵魂·不息 / 锋刃·百炼 / 水·深潜的击杀判定（AFTER_DEATH） */
	public static void onLivingDeath(LivingEntity victim, DamageSource source) {
		if (!(source.getEntity() instanceof ServerPlayer killer) || killer.isSpectator()) {
			return;
		}
		TrialState s = state(killer);

		// 灵魂·不息：隐身期（含猎魂隐身）内击杀敌对生物连续计数
		FamilyResonanceManager.Family soul = FamilyResonanceManager.Family.SOUL;
		if (eligible(killer, soul) && victim instanceof Enemy) {
			if (killer.hasEffect(MobEffects.INVISIBILITY)) {
				s.soulKills++;
				if (s.soulKills >= ResonanceConfig.rules(soul).trialSoulKills()) {
					clearFamily(s, soul);
					complete(killer, soul);
				}
			} else {
				// 非隐身击杀：开启新一轮（本次为第 1 杀，猎魂隐身自此生效）
				s.soulKills = 1;
			}
		}

		// 锋刃·百炼：连击满层窗口内击败高生命目标
		FamilyResonanceManager.Family blade = FamilyResonanceManager.Family.BLADE;
		if (eligible(killer, blade) && s.bladeMaxSince > 0L) {
			ResonanceConfig.FamilyRules rules = ResonanceConfig.rules(blade);
			long now = gameTime(killer);
			if (now - s.bladeMaxSince <= rules.bladeComboWindowTicks()
					&& victim.getMaxHealth() >= rules.trialBladeTargetHealth()) {
				clearFamily(s, blade);
				complete(killer, blade);
			}
		}

		// 水·深潜：潜水时长达标后击败远古守卫者
		if (victim.getType() == EntityTypes.ELDER_GUARDIAN && s.waterReady
				&& eligible(killer, FamilyResonanceManager.Family.WATER)) {
			clearFamily(s, FamilyResonanceManager.Family.WATER);
			complete(killer, FamilyResonanceManager.Family.WATER);
		}
	}

	/** 锋刃连击叠至上限标记（由 {@link FamilyResonanceManager#bladeComboMultiplier} 调用） */
	public static void onBladeComboMax(ServerPlayer attacker) {
		if (!attacker.isSpectator()) {
			state(attacker).bladeMaxSince = gameTime(attacker);
		}
	}

	/**
	 * 承伤/攻击结算（LivingEntityMixin hurtServer HEAD 回调，raw = 减免前原始伤害）。
	 * 守护·不动累计原始承伤；自然·复苏的「不得主动攻击」在此重置。
	 */
	public static void onHurtServer(LivingEntity victim, DamageSource source, float raw) {
		// 自然·复苏：主动攻击他人 → 单次站定累计清零（自伤不算攻击）
		if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim
				&& eligible(attacker, FamilyResonanceManager.Family.NATURE)) {
			TrialState as = state(attacker);
			if (as.natureHeal > 0.0F) {
				as.natureHeal = 0.0F;
				attacker.sendSystemMessage(Component.translatable("message.extra-enchantry.trial.reset",
						Component.translatable("family.extra-enchantry.nature")));
			}
		}
		// 守护·不动：累计原始承伤（窗口由本回调开启）
		if (victim instanceof ServerPlayer player
				&& eligible(player, FamilyResonanceManager.Family.GUARD) && raw > 0.0F) {
			TrialState s = state(player);
			if (s.guardStart < 0L) {
				s.guardStart = gameTime(player);
				s.guardDamage = 0.0F;
				s.guardAnchor = player.position();
			}
			s.guardDamage += raw;
		}
	}

	/** 自然·复苏：一次扎根回复（由 tickNatureRegen 回调） */
	public static void onNatureHeal(ServerPlayer player, float amount) {
		FamilyResonanceManager.Family nature = FamilyResonanceManager.Family.NATURE;
		if (!eligible(player, nature)) {
			return;
		}
		TrialState s = state(player);
		s.natureHeal += amount;
		if (s.natureHeal >= ResonanceConfig.rules(nature).trialNatureHealTotal()) {
			clearFamily(s, nature);
			complete(player, nature);
		}
	}

	/** 自然·复苏：站定被移动打断（由 tickNatureRegen 的 moved 分支回调，避免双份位移判定） */
	public static void onNatureMoved(ServerPlayer player) {
		TrialState s = STATES.get(player.getUUID());
		if (s != null && s.natureHeal > 0.0F) {
			s.natureHeal = 0.0F;
			player.sendSystemMessage(Component.translatable("message.extra-enchantry.trial.reset",
					Component.translatable("family.extra-enchantry.nature")));
		}
	}

	/** 首次达到任一 FULL → 显示试炼进度树根节点（幂等，award 已完成时无副作用） */
	public static void onReachFull(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		Identifier rootId = ExtraEnchantry.id("family_trials/root");
		net.minecraft.advancements.AdvancementHolder root = server.getAdvancements().get(rootId);
		if (root != null) {
			player.getAdvancements().award(root, "triggered");
		}
	}

	// ============ 完成结算 ============

	/** 试炼完成：授予进度；首次完成时发放家族铭印 + 反馈 */
	private static void complete(ServerPlayer player, FamilyResonanceManager.Family family) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		String path = family.name().toLowerCase(Locale.ROOT);
		Identifier trialId = ExtraEnchantry.id("family_trials/" + path);
		net.minecraft.advancements.AdvancementHolder holder = server.getAdvancements().get(trialId);
		if (holder == null) {
			return;
		}
		boolean first = player.getAdvancements().award(holder, "triggered");
		if (!first) {
			return;
		}
		// 首次完成：铭印 + 反馈（进度树自带 toast，聊天补一条总结 + 音效 + 粒子）
		FamilySigils.grant(player, family);
		FamilySigils.awardGrandResonatorIfComplete(player);
		player.sendSystemMessage(Component.translatable("message.extra-enchantry.trial.complete",
				Component.translatable("family.extra-enchantry." + path)));
		if (player.level() instanceof ServerLevel serverLevel) {
			FxHelper.play(serverLevel, player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.0F);
			FxHelper.burst(serverLevel, player, ParticleTypes.END_ROD, 20, 0.6D);
		}
	}

	/** 窗口中断重置提示（每次中断只提示一次，避免刷屏） */
	private static void notifyReset(ServerPlayer player, FamilyResonanceManager.Family family) {
		player.sendSystemMessage(Component.translatable("message.extra-enchantry.trial.reset",
				Component.translatable("family.extra-enchantry." + family.name().toLowerCase(Locale.ROOT))));
	}

	// ============ 资格与辅助 ============

	/** 试炼计数资格：对应家族 FULL 且为主调（收益暂停时不计数） */
	public static boolean eligible(ServerPlayer player, FamilyResonanceManager.Family family) {
		return FamilyResonanceManager.tierOf(player, family) == FamilyResonanceManager.Tier.FULL
				&& AttunementManager.attunementActive(player, family);
	}

	/** 试炼是否已完成（进度树状态，供秘典/命令展示；进度未加载 → false） */
	public static boolean isTrialComplete(ServerPlayer player, FamilyResonanceManager.Family family) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return false;
		}
		Identifier trialId = ExtraEnchantry.id("family_trials/" + family.name().toLowerCase(Locale.ROOT));
		net.minecraft.advancements.AdvancementHolder holder = server.getAdvancements().get(trialId);
		return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	/** 诊断：各系试炼运行时计数概要（debug 命令用） */
	public static String debugState(ServerPlayer player) {
		TrialState s = STATES.get(player.getUUID());
		if (s == null) {
			return "no runtime state";
		}
		return "soulKills=" + s.soulKills
				+ " bladeMax=" + s.bladeMaxSince
				+ " storm=" + (s.stormStart < 0 ? "-" : s.stormDistance + "g")
				+ " guard=" + (s.guardStart < 0 ? "-" : s.guardDamage + "hp")
				+ " natureHeal=" + s.natureHeal
				+ " dive=" + (s.waterDiveStart < 0 ? "-" : (s.waterReady ? "ready" : "counting"))
				+ " wind=" + s.windMaxFall
				+ " fire=" + (s.fireStart < 0 ? "-" : s.fireDistance + "g");
	}

	/** 玩家所在服务器的主世界 gameTime（冷却/窗口计时唯一时钟；server 不可达时 0） */
	private static long gameTime(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		return server != null ? server.overworld().getGameTime() : 0L;
	}
}
