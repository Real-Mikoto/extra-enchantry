package realmikoto.extraenchantry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 归一之战（The Convergence，1.6.0 §3）—— 五段链式终局战。
 *
 * 归一印记右键开启：五回合按难度梯度升序依次降临五个回响领主
 * （巫后 → 烬骨王 → 渊潮之主 → 守望者 → 末影领主），全程单一「归一之战」
 * boss 血条按回合推进 20% 并随回合换色（26.2 setColor 已验证）。
 *
 * 规则（设计 §3.5）：
 *   - 单回合超时 5 分钟（6000 tick）未击杀 → 失败；
 *   - 失败三路径：死亡 / 离开主世界 / 离当前回响 64 格 → 全部回响消散；
 *   - 印记：挑战期间誓约式锁定（不可丢弃、死亡不掉）；失败保留、成功结算消耗；
 *   - 中途回响零掉落（防 farming 旁路）；第 5 回合结算：心核 + 同辉书 + 随机境材料。
 *
 * 回响领主 = 复用 1.5.0 领主实体（零新实体），觉醒数值的 ×0.75 折减（设计 §3.4）。
 */
public final class ConvergenceManager {

	/** 开启结果（图腾类反馈统一由 WarArtifacts 发送） */
	public enum StartResult { OK, ACTIVE, COOLDOWN }

	/** 回合顺序（难度梯度升序，设计 §3.3） */
	private static final List<String> ROUND_REALMS =
			List.of("hag", "emberbone", "tidal", "overwarden", "ender");

	/** 回合血条颜色（与各境领主一致） */
	private static final List<BossEvent.BossBarColor> ROUND_COLORS = List.of(
			BossEvent.BossBarColor.GREEN, BossEvent.BossBarColor.RED,
			BossEvent.BossBarColor.BLUE, BossEvent.BossBarColor.PURPLE,
			BossEvent.BossBarColor.PURPLE);

	/** 单回合超时（tick）＝5 分钟 */
	private static final int ROUND_TIMEOUT_TICKS = 6_000;

	/** 蓄势时长（tick）＝10 s（每回合开场，无环境异变——回响不改地形） */
	private static final int ROUND_PRELUDE_TICKS = 200;

	/** 余辉沉淀时长（tick）＝10 s（击杀后的间歇） */
	private static final int SETTLE_TICKS = 200;

	/** 挑战期锁定距离（格，与 1.5.0 取消路径一致） */
	private static final double CANCEL_DISTANCE = 64.0;

	/** 每玩家锁（分钟）：成功与失败同值 */
	private static final long LOCKOUT_MINUTES = 30;
	private static final long MINUTE_MS = 60_000L;

	// ============ 运行时状态 ============

	private static final Map<UUID, Convergence> ACTIVE = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> LOCKOUT_UNTIL = new ConcurrentHashMap<>();

	private ConvergenceManager() {
	}

	// ============ 生命周期 ============

	/** 印记右键开启（WarArtifacts.MarkItem 调用） */
	public static StartResult start(ServerPlayer player, ServerLevel level) {
		if (EliteEncounterManager.isActiveIn(level.dimension())) {
			return StartResult.ACTIVE; // 普通遭遇 / 另一场归一占用维度
		}
		Long until = LOCKOUT_UNTIL.get(player.getUUID());
		if (until != null && until > System.currentTimeMillis()) {
			return StartResult.COOLDOWN;
		}
		if (!ACTIVE.isEmpty()) {
			return StartResult.ACTIVE; // 全局同时仅一场归一（回响复用维度锁）
		}
		Convergence convergence = new Convergence(level, player.getUUID());
		ACTIVE.put(player.getUUID(), convergence);
		EliteEncounterManager.lockDimension(level.dimension(), player.getUUID());
		convergence.bossEvent.addPlayer(player);
		player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
				"message.extra-enchantry.convergence.started"));
		beginRound(convergence, player);
		return StartResult.OK;
	}

	/** 挂 {@code ServerTickEvents.END_SERVER_TICK}（ExtraEnchantry 注册） */
	public static void tick(net.minecraft.server.MinecraftServer server) {
		for (UUID ownerId : List.copyOf(ACTIVE.keySet())) {
			Convergence convergence = ACTIVE.get(ownerId);
			if (convergence == null) {
				continue;
			}
			ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
			if (player == null) {
				continue; // 离线冻结
			}
			try {
				advance(convergence, player);
			} catch (Exception e) {
				ExtraEnchantry.LOGGER.error("[extra-enchantry] 归一之战节拍异常（玩家 {}）",
						player.getName().getString(), e);
				fail(convergence, player);
			}
		}
	}

	private static void advance(Convergence convergence, ServerPlayer player) {
		ServerLevel level = convergence.level;
		if (!player.isAlive()) {
			fail(convergence, player);
			return;
		}
		// 修复：先比较玩家所在维度——旧实现比较 encounter.level 自身的 dimension()（常量），
		// "离开主世界 → 失败" 永远不会触发
		if (player.level() != level || level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
			fail(convergence, player); // 离开主世界
			return;
		}
		long now = level.getGameTime();
		long roundElapsed = now - convergence.roundStart;

		// 回响存在：取消路径 + 超时
		if (convergence.lordId != null) {
			Entity lord = level.getEntity(convergence.lordId);
			if (lord == null || !lord.isAlive()) {
				// 正常击杀走 onEchoDeath（先于本 tick 清 lordId）；异常消散兜底
				advanceRound(convergence, player);
				return;
			}
			if (lord.distanceTo(player) > CANCEL_DISTANCE) {
				fail(convergence, player);
				return;
			}
			if (roundElapsed >= ROUND_PRELUDE_TICKS + ROUND_TIMEOUT_TICKS) {
				fail(convergence, player); // 单回合超时（蓄势 + 战斗合计）
				return;
			}
			return; // 战斗进行中
		}

		// 余辉沉淀（击杀后的 10 s 间歇）
		if (convergence.settleTicks > 0) {
			convergence.settleTicks--;
			if (convergence.settleTicks == 0) {
				beginRound(convergence, player); // 沉淀结束 → 下一回合蓄势
			}
			return;
		}

		// 蓄势阶段（roundStart 即回合起点）
		if (roundElapsed == 1) {
			roundPreludeFx(convergence, player);
		}
		if (roundElapsed >= ROUND_PRELUDE_TICKS) {
			spawnEcho(convergence, player);
		}
	}

	/** 每回合开场：环形遗辉粒子 + 各境号角（无环境异变） */
	private static void roundPreludeFx(Convergence convergence, ServerPlayer player) {
		ServerLevel level = convergence.level;
		String realm = ROUND_REALMS.get(convergence.round);
		LordRuntime.ring(level, player.position(), 8.0D, 32, ParticleTypes.END_ROD);
		LordRuntime.sound(level, player, switch (realm) {
			case "hag" -> SoundEvents.WITCH_AMBIENT;
			case "emberbone" -> SoundEvents.WITHER_AMBIENT;
			case "tidal" -> SoundEvents.GUARDIAN_AMBIENT;
			case "overwarden" -> SoundEvents.WARDEN_AGITATED;
			default -> SoundEvents.ENDERMAN_AMBIENT;
		}, 0.7F);
		player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
				"message.extra-enchantry.convergence.round." + realm));
	}

	/** 生成回响领主（1.5.0 领主实体 + 回响折减参数） */
	private static void spawnEcho(Convergence convergence, ServerPlayer player) {
		ServerLevel level = convergence.level;
		String realm = ROUND_REALMS.get(convergence.round);
		EncounterDef def = EncounterDef.byId(realm);
		Mob lord = def.factory().create(level);
		if (lord == null) {
			fail(convergence, player);
			return;
		}
		net.minecraft.world.phys.Vec3 spawn = player.position().add(
				(level.getRandom().nextDouble() - 0.5D) * 8.0D, 0.0D,
				(level.getRandom().nextDouble() - 0.5D) * 8.0D);
		lord.snapTo(spawn.x, spawn.y, spawn.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		lord.finalizeSpawn(level, level.getCurrentDifficultyAt(player.blockPosition()),
				EntitySpawnReason.TRIGGERED, null);
		lord.setPersistenceRequired();
		lord.invulnerableTime = 40;
		level.addFreshEntity(lord);
		lord.setHealth(lord.getMaxHealth());
		lord.setTarget(player);
		// 回响标记：LordRuntime 挂 echo 折减（×0.75 生命 / ×1.2 技能冷却，§3.4）
		if (lord instanceof EliteLord eliteLord) {
			eliteLord.lordRuntime().markEcho();
		}
		convergence.lordId = lord.getUUID();
		EliteEncounterManager.lockLord(lord.getUUID(), player.getUUID());
		LordRuntime.column(level, spawn, 3.0D, 24, ParticleTypes.END_ROD);
	}

	/** 回响死亡（EliteEncounterManager.onLordDeath 转发）：血条推进 → 沉淀 → 下一回合 / 结算 */
	public static void onEchoDeath(LivingEntity lord, ServerPlayer killer) {
		Convergence convergence = null;
		for (Convergence candidate : ACTIVE.values()) {
			if (lord.getUUID().equals(candidate.lordId)) {
				convergence = candidate;
				break;
			}
		}
		if (convergence == null) {
			return;
		}
		convergence.lordId = null;
		convergence.settleTicks = SETTLE_TICKS;
		// 血条推进 20%（换色在 beginRound 统一处理）
		convergence.round++;
		convergence.bossEvent.setProgress(convergence.round / (float) ROUND_REALMS.size());
		LordRuntime.ring(convergence.level, lord.position(), 6.0D, 32, ParticleTypes.END_ROD);
		LordRuntime.sound(convergence.level, killer, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8F);
		if (convergence.round >= ROUND_REALMS.size()) {
			// 最终回响击杀 → 跳过沉淀直接结算（终局仪式由 succeed 全权接管）
			succeed(convergence, killer);
		}
	}

	/** 回响异常消散兜底（正常击杀走 onEchoDeath）：视同沉淀开始 */
	private static void advanceRound(Convergence convergence, ServerPlayer player) {
		convergence.lordId = null;
		convergence.settleTicks = SETTLE_TICKS;
	}

	private static void beginRound(Convergence convergence, ServerPlayer player) {
		convergence.roundStart = convergence.level.getGameTime();
		convergence.bossEvent.setColor(ROUND_COLORS.get(convergence.round));
	}

	/** 成功结算：消耗印记 + 终局掉落（心核 + 同辉书 + 随机境材料）+ 进度 */
	private static void succeed(Convergence convergence, ServerPlayer killer) {
		ServerLevel level = convergence.level;
		// 修复：掉落与进度归属发起人（killer 可能是协作者最后一击）
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(convergence.playerId);
		if (owner == null) {
			owner = killer; // 极端情况（owner 瞬间离线）：按最后一击者结算，避免物品丢失
		}
		remove(convergence);
		// 掉落
		owner.spawnAtLocation(level, new ItemStack(WarArtifacts.CONVERGENCE_CORE));
		owner.spawnAtLocation(level, RealmTreasures.enchantedBook(level,
				ExtraEnchantry.REALMS_UNITY, 1));
		String material = ROUND_REALMS.get(level.getRandom().nextInt(ROUND_REALMS.size()));
		owner.spawnAtLocation(level,
				new ItemStack(EncounterDef.byId(material).material()));
		owner.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
				"message.extra-enchantry.convergence.success"));
		FxHelper.burstAt(level, owner.getX(), owner.getY(0.5D), owner.getZ(),
				ParticleTypes.TOTEM_OF_UNDYING, 48, 0.8D);
		FxHelper.play(level, owner, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
		// 1.7.0 谱系主线：归一节点（含谱系圆满判定链）
		LineageManager.onConvergenceDone(owner);
		// 1.6.0 隐藏进度：终局击杀瞬间生命 ≤5 并存活
		LineageManager.onFinalAfterglow(owner);
		LOCKOUT_UNTIL.put(convergence.playerId,
				System.currentTimeMillis() + LOCKOUT_MINUTES * MINUTE_MS);
	}

	/** 失败：全部回响消散、印记保留、30 分钟锁 */
	private static void fail(Convergence convergence, ServerPlayer player) {
		ServerLevel level = convergence.level;
		if (convergence.lordId != null) {
			Entity lord = level.getEntity(convergence.lordId);
			if (lord != null) {
				if (lord instanceof EliteLord eliteLord) {
					eliteLord.lordRuntime().dispose();
				}
				lord.discard();
			}
		}
		remove(convergence);
		player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
				"message.extra-enchantry.convergence.fail"));
		FxHelper.play(level, player, SoundEvents.WITHER_DEATH, 2.0F, 0.6F);
		LOCKOUT_UNTIL.put(convergence.playerId,
				System.currentTimeMillis() + LOCKOUT_MINUTES * MINUTE_MS);
	}

	/**
	 * 修复：remove 一律以 convergence.playerId（发起人）为键——
	 * 旧实现用传入的 player（可能是最后一击者），多人协作时 ACTIVE 残留 →
	 * settle 后 ROUND_COLORS.get(5) 越界 → 误判失败 + 维度锁泄漏。
	 */
	private static void remove(Convergence convergence) {
		ACTIVE.remove(convergence.playerId);
		EliteEncounterManager.unlockDimension(convergence.level.dimension(), convergence.playerId);
		convergence.bossEvent.removeAllPlayers();
	}

	/**
	 * 玩家登出清理（DISCONNECT 调用）。
	 * 修复：旧实现 tick 里 player == null 仅 continue，ACTIVE 永不清理——
	 * isRunning() 恒为真 → 全服永久无法开启归一，维度锁同时压制该维度五境遭遇。
	 * 走 fail 等效路径：回响消散、锁释放、bossEvent 清观众、印记保留、不加锁（离线非失败）。
	 */
	public static void onDisconnect(UUID playerId) {
		Convergence convergence = ACTIVE.remove(playerId);
		if (convergence == null) {
			return;
		}
		if (convergence.lordId != null) {
			Entity lord = convergence.level.getEntity(convergence.lordId);
			if (lord != null) {
				if (lord instanceof EliteLord eliteLord) {
					eliteLord.lordRuntime().dispose();
				}
				lord.discard();
			}
		}
		EliteEncounterManager.unlockDimension(convergence.level.dimension(), playerId);
		convergence.bossEvent.removeAllPlayers();
		ExtraEnchantry.LOGGER.info("[extra-enchantry] 归一之战因玩家离线中止（{}），回响已消散、印记保留",
				playerId);
	}

	/** 是否进行中（供 EliteEncounterManager 的互斥判定与浩劫冻结） */
	public static boolean isRunning() {
		return !ACTIVE.isEmpty();
	}

	/** 指定玩家是否持有进行中的归一（供维度锁回收判定：归一的锁不得被当成孤儿锁回收） */
	public static boolean isActiveOwner(UUID playerId) {
		return ACTIVE.containsKey(playerId);
	}

	/** 服务器停止全清（ServerLifecycleEvents.SERVER_STOPPED 调用） */
	public static void onServerStopped() {
		ACTIVE.clear();
		LOCKOUT_UNTIL.clear();
	}

	/**
	 * 玩家上线检查（1.7.0 §4 重启提示）：若有未完成的归一印记在背包（说明挑战被重启打断），
	 * 动作栏提示一次「归一之辉被打断，印记仍在」。挑战状态本身不持久化（按打断计，30 分钟锁同语义）。
	 */
	public static void onJoin(ServerPlayer player) {
		boolean hasMark = false;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (player.getInventory().getItem(i).getItem() == WarArtifacts.CONVERGENCE_MARK) {
				hasMark = true;
				break;
			}
		}
		Long until = LOCKOUT_UNTIL.get(player.getUUID());
		if (hasMark && until == null) {
			// 有印记且不在锁内：大概率是重启丢失了进行中的挑战（重启不写锁）
			player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
					"message.extra-enchantry.convergence.interrupted"));
		}
	}

	/** 是否为归一回响（isBossLike 同领主：断罪 ×2 / 不可拉拽） */
	public static boolean isEcho(LivingEntity entity) {
		if (!(entity instanceof EliteLord eliteLord)) {
			return false;
		}
		for (Convergence convergence : ACTIVE.values()) {
			if (entity.getUUID().equals(convergence.lordId)) {
				return true;
			}
		}
		return false;
	}

	/** 单场归一运行时 */
	private static final class Convergence {
		final ServerLevel level;
		final UUID playerId;
		final ServerBossEvent bossEvent;
		/** 当前回合（0-based） */
		int round;
		/** 本回合起始 gameTime */
		long roundStart;
		/** 当前回响实体（null = 蓄势 / 沉淀中） */
		UUID lordId;
		/** 余辉沉淀剩余 tick */
		int settleTicks;

		Convergence(ServerLevel level, UUID playerId) {
			this.level = level;
			this.playerId = playerId;
			this.bossEvent = new ServerBossEvent(UUID.randomUUID(),
					net.minecraft.network.chat.Component.translatable(
							"event.extra-enchantry.convergence"),
					ROUND_COLORS.get(0), BossEvent.BossBarOverlay.PROGRESS);
		}
	}
}
