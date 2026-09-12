package realmikoto.extraenchantry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 渊心守望者（1.8.5「心」）：幽渊维度的终局 Boss——静/响节奏战。
 *
 * 叙事（对齐 LORE_CANON）：幽渊的意志本身；越响它越强（它听见的越多），
 * 安静时进入「蓄力之空」（玩家输出窗口）——战斗即「沉默悖论」的终极演绎。
 *
 * 机制（对齐 DESIGN/1.8.0-design.md §6.7）：
 * - Boss 越响越强：对 Boss 的攻击会累积它的「愤怒」（伤害 ×1 + 0.15/层，上限 ×2）；
 * - 蓄力之空：Boss 每 12 秒进入 4 秒「聆听窗口」（免伤蓄力→爆发音波），
 *   窗口内玩家保持静默（声纹 <5）→ 爆发被「憋回去」，Boss 受到重击脆弱（易伤 3 秒）；
 * - 咆哮阶段：生命 50%/25% 时释放全屏音波（黑暗 + 缓慢）；
 * - 血条：紫红色 Boss 条「觉醒·渊心守望者」；
 * - 掉落：深渊之心 100% + 五境同辉书 I 级 25%（对齐 §5.2 终局材料）。
 *
 * 生成：创造蛋（管理/测试）或后续版本渊心结构触发；`shouldBeSaved=false` 遵循
 * 领主「遭遇事件而非世界生物」纪律——Boss 重启消失，可重新挑战。
 */
public class HeartWardenEntity extends Warden {

	/** 挑战冷却 attachment（§6.7：击败/唤醒后 7 主世界日内不再被同一玩家唤醒） */
	public static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Long> CHALLENGE_COOLDOWN =
			net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.<Long>create(
					realmikoto.extraenchantry.ExtraEnchantry.id("heart_warden_cooldown"),
					builder -> builder.persistent(com.mojang.serialization.Codec.LONG));

	/** 显式触发静态初始化（ExtraEnchantry.onInitialize 调用） */
	public static void register() {
	}

	/** 状态（运行时）：愤怒层数（0–~7 对应 ×1.0–×2.0） */
	private static final Map<UUID, Integer> RAGE = new ConcurrentHashMap<>();
	/** 聆听窗口剩余 tick（>0 = 蓄力中） */
	private static final Map<UUID, Integer> LISTENING = new ConcurrentHashMap<>();

	public HeartWardenEntity(EntityType<? extends Warden> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
	}

	/**
	 * 生存内触发（v1.8.0 §6.7）：玩家深入渊心（Y>200，无相之空所在高度带）——
	 * 若当前无守望者活动且玩家挑战冷却已过 → 在附近唤醒 Boss（全维度播报）。
	 * 由 AbyssSpawning 渊心分支调用。
	 */
	public static void tryAwaken(ServerLevel abyss, ServerPlayer player) {
		// 已有守望者活动时不重复唤醒
		if (!abyss.getEntities(realmikoto.extraenchantry.AbyssEntities.HEART_WARDEN, e -> e.isAlive()).isEmpty()) {
			return;
		}
		long now = abyss.getGameTime();
		Long until = player.getAttached(CHALLENGE_COOLDOWN);
		if (until != null && now < until) {
			return;
		}
		player.setAttached(CHALLENGE_COOLDOWN, now + 20L * 60 * 24 * 7);
		HeartWardenEntity boss = realmikoto.extraenchantry.AbyssEntities.HEART_WARDEN.create(
				abyss, net.minecraft.world.entity.EntitySpawnReason.EVENT);
		if (boss == null) {
			return;
		}
		double angle = player.getRandom().nextDouble() * Math.PI * 2;
		boss.snapTo(player.getX() + Math.cos(angle) * 14.0D, player.getY() + 2.0D,
				player.getZ() + Math.sin(angle) * 14.0D, 0.0F, 0.0F);
		abyss.addFreshEntity(boss);
		for (ServerPlayer p : abyss.players()) {
			p.sendSystemMessage(Component.translatable("message.extra-enchantry.heart_warden.awaken"));
			FxHelper.play(abyss, p, SoundEvents.WARDEN_ROAR, 1.6F, 0.6F);
		}
		FxHelper.ring(abyss, boss, 24.0D, ParticleTypes.SONIC_BOOM, 32);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Warden.createAttributes()
				.add(Attributes.MAX_HEALTH, 600.0D)
				.add(Attributes.ATTACK_DAMAGE, 30.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.35D)
				.add(Attributes.FOLLOW_RANGE, 48.0D);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide() || !(level() instanceof ServerLevel server)) {
			return;
		}
		HeartFight fight = HeartFightState.get(this, server);
		fight.tick(server, this);

		// 氛围：Boss 周身幽匿灵魂脉动
		if (this.tickCount % 8 == 0) {
			server.sendParticles(ParticleTypes.SCULK_SOUL,
					getX(), getY(1.5D), getZ(), 3, 0.8D, 1.0D, 0.8D, 0.02D);
		}
	}

	/** 受击结算（LivingEntityMixin 调用）：受击也是"响动"——少量愤怒累积 */
	public void noteStruck(float amount) {
		int rage = RAGE.merge(getUUID(), 1, Integer::sum);
		if (rage % 3 == 0 && getTarget() instanceof ServerPlayer p) {
			p.sendSystemMessage(Component.translatable("message.extra-enchantry.heart_warden.rage"));
		}
	}

	/** 声纹愤怒（§6.7「Boss 会监听玩家声纹，玩家越响，Boss 越强」）：HeartFight.tick 周期采样 */
	void notePlayerEcho(float echo) {
		if (echo >= 5.0F) {
			int before = RAGE.getOrDefault(getUUID(), 0);
			int rage = RAGE.merge(getUUID(), 1, Integer::sum);
			if (rage != before && rage % 4 == 0) {
				// 它听见的越多，黑暗就越亮
				for (Player p : level().players()) {
					if (p instanceof ServerPlayer sp && sp.distanceTo(this) < 48) {
						sp.sendSystemMessage(Component.translatable("message.extra-enchantry.heart_warden.listen"));
					}
				}
			}
		}
	}

	/** 当前伤害倍率（×1.0–×2.0）——作用于 Boss 对玩家的输出（LivingEntityMixin 受伤侧读取） */
	public float rageMultiplier() {
		return Math.min(2.0F, 1.0F + RAGE.getOrDefault(getUUID(), 0) * 0.15F);
	}

	/** 死亡清理运行时 + 终局演出（§6.7 DoD「有完整掉落与终局演出」） */
	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		// 深渊之心 100%（§5.2 终局材料）
		this.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(AbyssResources.ABYSSAL_HEART));
		// 终局演出：无相之尘雨 + 光柱 + 全维度讣告
		for (int i = 0; i < 24; i++) {
			double dx = (getRandom().nextDouble() - 0.5D) * 8.0D;
			double dz = (getRandom().nextDouble() - 0.5D) * 8.0D;
			level.sendParticles(ParticleTypes.END_ROD,
					getX() + dx, getY(2.0D), getZ() + dz, 2, 0.1D, 6.0D, 0.1D, -0.12D);
		}
		level.sendParticles(ParticleTypes.SCULK_SOUL,
				getX(), getY(1.5D), getZ(), 64, 2.0D, 2.0D, 2.0D, 0.05D);
		for (ServerPlayer p : level.players()) {
			p.sendSystemMessage(Component.translatable("message.extra-enchantry.heart_warden.defeated"));
			p.sendSystemMessage(Component.translatable("message.extra-enchantry.heart_warden.finale"));
			FxHelper.play(level, p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
		}
		HeartFightState.remove(getUUID(), level);
		super.dropCustomDeathLoot(level, source, recentlyHit);
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	// ============ 运行时状态容器（静态生命周期按 v1.7.4 纪律治理） ============

	/** Boss 战运行时（每 Boss 一份） */
	public static final class HeartFightState {

		private static final Map<UUID, HeartFight> ACTIVE = new ConcurrentHashMap<>();

		public static HeartFight get(HeartWardenEntity boss, ServerLevel level) {
			return ACTIVE.computeIfAbsent(boss.getUUID(),
					id -> new HeartFight(level, boss));
		}

		public static void remove(UUID id) {
			ACTIVE.remove(id);
		}

		public static void remove(UUID id, ServerLevel level) {
			HeartFight fight = ACTIVE.remove(id);
			if (fight != null) {
				fight.dispose(level);
			}
		}

		public static void onServerStopped() {
			ACTIVE.clear();
			RAGE.clear();
			LISTENING.clear();
		}
	}

	/** Boss 战逻辑：血条 + 节奏窗口 + 咆哮阶段 */
	public static final class HeartFight {

		private final ServerBossEvent bossEvent;
		private int listenCountdown = 12 * 20;
		private int listenRemain = 0;
		private boolean roared50 = false;
		private boolean roared25 = false;
		/** 四阶段（设计稿 §6.7）：潮汐 → 回声 → 记忆 → 无相 */
		private int phase = 0;

		private HeartFight(ServerLevel level, HeartWardenEntity boss) {
			this.bossEvent = new ServerBossEvent(
					boss.getUUID(),
					Component.translatable("bossbar.extra-enchantry.heart_warden"),
					BossEvent.BossBarColor.PURPLE,
					BossEvent.BossBarOverlay.NOTCHED_10);
			for (Player p : level.players()) {
				if (p instanceof ServerPlayer sp) {
					bossEvent.addPlayer(sp);
				}
			}
		}

		public void tick(ServerLevel level, HeartWardenEntity boss) {
			// 血条
			bossEvent.setProgress(boss.getHealth() / boss.getMaxHealth());
			for (Player p : level.players()) {
				if (p instanceof ServerPlayer sp && sp.distanceTo(boss) < 64) {
					bossEvent.addPlayer(sp);
				}
			}

			// 声纹愤怒（§6.7）：每秒采样附近玩家声纹——玩家越响，Boss 越强（愤怒上限 8 层）
			if (++echoSample >= 20) {
				echoSample = 0;
				float loudest = 0.0F;
				for (Player p : level.players()) {
					if (p instanceof ServerPlayer sp && sp.distanceTo(boss) < 32) {
						loudest = Math.max(loudest, EchoManager.lastEcho(sp));
					}
				}
				if (loudest >= 5.0F && RAGE.getOrDefault(boss.getUUID(), 0) < 8) {
					boss.notePlayerEcho(loudest);
				}
			}

			// 聆听窗口（蓄力之空）
			if (listenRemain > 0) {
				listenRemain--;
				// 静默判定（§6.8 多人：仅统计 Boss 附近 32 格的参战玩家，而非全维度连坐）
				boolean quiet = level.players().stream()
						.filter(p -> p instanceof ServerPlayer sp && sp.distanceTo(boss) < 32)
						.map(p -> (ServerPlayer) p)
						.allMatch(p -> EchoManager.lastEcho(p) < 5.0F);
				if (quiet) {
					// 全员静默 → 爆发被憋回去 → Boss 脆弱
					endListening(level, boss, true);
				} else if (listenRemain == 0) {
					endListening(level, boss, false);
				}
				return;
			}
			if (--listenCountdown <= 0) {
				startListening(level, boss);
			}

			// 四阶段推进（潮汐 → 回声 → 记忆 → 无相）：每降 25% 切换机制与视觉
			float hpPct = boss.getHealth() / boss.getMaxHealth();
			int newPhase = hpPct > 0.75F ? 0 : hpPct > 0.5F ? 1 : hpPct > 0.25F ? 2 : 3;
			if (newPhase != phase) {
				phase = newPhase;
				enterPhase(level, boss, phase);
			}

			// 咆哮阶段（50% / 25%）
			float pct = boss.getHealth() / boss.getMaxHealth();
			if (pct <= 0.5F && !roared50) {
				roared50 = true;
				roar(level, boss);
			} else if (pct <= 0.25F && !roared25) {
				roared25 = true;
				roar(level, boss);
			}

			// 潮汐阶段：周期性潮涌（每 8 秒一次冲击波，压迫玩家位移）
			if (phase == 0 && --tideCountdown <= 0) {
				tideCountdown = 8 * 20;
				tideWave(level, boss);
			}
		}

		/** 声纹愤怒采样节拍 */
		private int echoSample = 0;
		/** 潮涌倒计时 */
		private int tideCountdown = 8 * 20;

		/** 阶段切换：视觉 + 机制（设计稿 §6.7） */
		private void enterPhase(ServerLevel level, HeartWardenEntity boss, int phase) {
			String key = switch (phase) {
				case 0 -> "tidal";
				case 1 -> "echo";
				case 2 -> "memory";
				default -> "aether";
			};
			for (Player p : level.players()) {
				if (p instanceof ServerPlayer sp && sp.distanceTo(boss) < 64) {
					sp.sendSystemMessage(Component.translatable(
							"message.extra-enchantry.heart_warden.phase." + key));
				}
			}
			// 阶段专属机制
			switch (phase) {
				case 0 -> {
					// 潮汐：液幽匿般的潮涌——周期性冲击波击退玩家（§6.7 阶段机制）
					tideWave(level, boss);
				}
				case 1 -> {
					// 回声：聆听窗口更频繁（12s → 8s）
					listenCountdown = Math.min(listenCountdown, 8 * 20);
					FxHelper.ring(level, boss, 16.0D, ParticleTypes.SCULK_SOUL, 24);
				}
				case 2 -> {
					// 记忆：召唤记忆残影助战（视觉压迫）
					for (int i = 0; i < 2; i++) {
						var shade = realmikoto.extraenchantry.AbyssEntities.MEMORY_SHADE.create(
								level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
						if (shade != null) {
							shade.snapTo(boss.getX() + (i == 0 ? 3 : -3), boss.getY(), boss.getZ(),
									level.getRandom().nextFloat() * 360.0F, 0.0F);
							level.addFreshEntity(shade);
						}
					}
					FxHelper.ring(level, boss, 20.0D, ParticleTypes.SOUL_FIRE_FLAME, 24);
				}
				case 3 -> {
					// 无相：声纹完全隐匿的压迫——全场黑暗
					for (Player p : level.players()) {
						if (p instanceof ServerPlayer sp && sp.distanceTo(boss) < 48) {
							sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
						}
					}
					FxHelper.ring(level, boss, 28.0D, ParticleTypes.SCULK_SOUL, 32);
				}
				default -> FxHelper.ring(level, boss, 14.0D, ParticleTypes.BUBBLE_POP, 16);
			}
			FxHelper.play(level, boss, SoundEvents.WARDEN_ROAR, 1.6F, 0.7F + phase * 0.1F);
		}

		private void startListening(ServerLevel level, HeartWardenEntity boss) {
			listenRemain = 4 * 20;
			for (Player p : level.players()) {
				if (p instanceof ServerPlayer sp && sp.distanceTo(boss) < 48) {
					sp.sendSystemMessage(Component.translatable("message.extra-enchantry.heart_warden.listen_start"));
				}
			}
			FxHelper.play(level, boss, SoundEvents.WARDEN_LISTENING, 1.5F, 0.5F);
		}

		private void endListening(ServerLevel level, HeartWardenEntity boss, boolean stilled) {
			listenCountdown = 12 * 20;
			if (stilled) {
				// 玩家赢了节奏：Boss 脆弱 3 秒（易伤，受击双倍）+ 清空部分愤怒
				boss.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, true));
				RAGE.put(boss.getUUID(), Math.max(0, RAGE.getOrDefault(boss.getUUID(), 0) - 3));
				for (Player p : level.players()) {
					if (p instanceof ServerPlayer sp && sp.distanceTo(boss) < 48) {
						sp.sendSystemMessage(Component.translatable("message.extra-enchantry.heart_warden.window_won"));
					}
				}
				FxHelper.ring(level, boss, 12.0D, ParticleTypes.END_ROD, 24);
			} else {
				// 未保持静默 → 音波爆发（黑暗 + 缓慢）
				for (Player p : level.players()) {
					if (p instanceof ServerPlayer sp && sp.distanceTo(boss) < 24) {
						sp.hurt(level.damageSources().sonicBoom(boss), 12.0F);
						sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
						sp.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
					}
				}
				FxHelper.play(level, boss, SoundEvents.WARDEN_SONIC_BOOM, 2.0F, 0.8F);
			}
		}

		/** 潮汐阶段（§6.7）：周期性潮涌冲击波——推开玩家（必须位移躲避节奏） */
		private void tideWave(ServerLevel level, HeartWardenEntity boss) {
			for (Player p : level.players()) {
				if (p instanceof ServerPlayer sp && sp.distanceTo(boss) < 20) {
					double dx = sp.getX() - boss.getX();
					double dz = sp.getZ() - boss.getZ();
					double d = Math.max(1.0D, Math.sqrt(dx * dx + dz * dz));
					sp.push(dx / d * 2.2D, 0.45D, dz / d * 2.2D);
					sp.hurtMarked = true;
					sp.hurt(level.damageSources().sonicBoom(boss), 6.0F);
				}
			}
			FxHelper.ring(level, boss, 20.0D, ParticleTypes.BUBBLE_POP, 32);
			FxHelper.play(level, boss, SoundEvents.WARDEN_ANGRY, 1.4F, 0.8F);
		}

		private void roar(ServerLevel level, HeartWardenEntity boss) {
			for (Player p : level.players()) {
				if (p instanceof ServerPlayer sp && sp.distanceTo(boss) < 32) {
					sp.hurt(level.damageSources().sonicBoom(boss), 16.0F);
					sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0));
					// 咆哮冲击：击退（§6.7「咆哮时位移」——玩家必须躲开节奏）
					double dx = sp.getX() - boss.getX();
					double dz = sp.getZ() - boss.getZ();
					double d = Math.max(1.0D, Math.sqrt(dx * dx + dz * dz));
					sp.push(dx / d * 1.8D, 0.5D, dz / d * 1.8D);
					sp.hurtMarked = true;
				}
			}
			FxHelper.ring(level, boss, 24.0D, ParticleTypes.SONIC_BOOM, 32);
			FxHelper.play(level, boss, SoundEvents.WARDEN_ROAR, 2.0F, 0.6F);
		}

		public void dispose(ServerLevel level) {
			// 血条移除：对该维度玩家撤销显示
			for (Player p : level.players()) {
				if (p instanceof ServerPlayer sp) {
					bossEvent.removePlayer(sp);
				}
			}
		}
	}
}
