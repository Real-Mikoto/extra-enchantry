package realmikoto.extraenchantry;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 精英遭遇战统一调度（1.5.0「五境领主」§0.2）——EliteEncounterManager。
 *
 * 职责：
 *   1. 遭遇注册表（{@link EncounterDef#ALL}）与通用阶段机
 *      （蓄势 PRELUDE → 蔓延 UNFOLD → 裂缝 CRACK → 觉醒 AWAKEN）；
 *   2. 每玩家冷却（10 分钟 / 被打断 5 分钟）与每维度并发锁（≤1）；
 *   3. 五境触发计数（愤怒持续 / 凋零累积 / 浸泡 + 激光 / 夜晚双路径 / 珍珠使用）；
 *   4. 觉醒：生成领主、锁定仇恨 10 秒、施加登场效果；
 *   5. 取消路径（触发者死亡 / 离开群系 / 离开领主 64 格）与进度授予；
 *   6. 对外接口：{@link #isLord} / {@link #resolveLordTarget} / {@link #lordExperience}
 *      / {@link #isActiveIn}（供 MobMixin / LivingEntityMixin / CavalryManager 调用）。
 *
 * 全部状态为运行时（不持久化）：服务器重启后进度与冷却按原设计语义丢失，
 * 领主实体 {@code shouldBeSaved=false}，不会残留孤儿领主。
 */
public final class EliteEncounterManager {

	/** 阶段机 */
	public enum Phase { PRELUDE, UNFOLD, CRACK, AWAKEN }

	/** 进度树根「境主降临」（{@code lords/root}） */
	private static final ResourceKey<net.minecraft.advancements.Advancement> LORDS_ROOT =
			ResourceKey.create(Registries.ADVANCEMENT, ExtraEnchantry.id("lords/root"));

	/** 觉醒后仇恨锁定时长（tick，设计 §0.2.2） */
	private static final int LOCK_TICKS = 200;

	/** 取消判定：距领主超过 64 格（设计 §0.2.4） */
	private static final double CANCEL_DISTANCE = 64.0;

	/** 环境异变方块上限（设计 §6.4 性能） */
	private static final int MAX_UNFOLD_BLOCKS = 125;

	/** 深暗境愤怒扫描间隔（tick）：64³ 实体搜索降频，5 tick 采样精度足够（1.7.3） */
	private static final int WARDEN_SCAN_INTERVAL_TICKS = 5;

	private static final long MINUTE_MS = 60_000L;

	// ============ 运行时状态 ============

	private static final Map<UUID, Encounter> ACTIVE = new ConcurrentHashMap<>();
	private static final Map<ResourceKey<Level>, UUID> DIMENSION_LOCK = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> COOLDOWN_UNTIL = new ConcurrentHashMap<>();
	/** 领主 UUID → 触发者 UUID + 锁定到期 tick */
	private static final Map<UUID, TargetLock> LOCKS = new ConcurrentHashMap<>();

	// 触发计数（按玩家）
	private static final Map<UUID, SlidingCounter> WITHER_TICKS = new HashMap<>();
	private static final Map<UUID, SlidingCounter> SOAK_TICKS = new HashMap<>();
	private static final Map<UUID, SlidingCounter> LASER_HITS = new HashMap<>();
	private static final Map<UUID, SlidingCounter> POTION_HITS = new HashMap<>();
	private static final Map<UUID, SlidingCounter> WITCH_KILLS = new HashMap<>();
	private static final Map<UUID, SlidingCounter> PEARL_USES = new HashMap<>();
	/** 深暗境：连续愤怒 tick（中断即清零） */
	private static final Map<UUID, Integer> WARDEN_ANGER = new HashMap<>();

	private EliteEncounterManager() {
	}

	// ============ 服务端节拍 ============

	/** 挂 {@code ServerTickEvents.END_SERVER_TICK}：推进触发计数与进行中的遭遇 */
	public static void tick(MinecraftServer server) {
		EliteEncounterConfig.Global global = EliteEncounterConfig.global();
		if (!global.enabled()) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			try {
				tickPlayer(player, global);
			} catch (Exception e) {
				ExtraEnchantry.LOGGER.error("[extra-enchantry] 精英遭遇节拍异常（玩家 {}）",
						player.getName().getString(), e);
			}
		}
		// 维度锁清理：该维度已无进行中的遭遇则释放
		DIMENSION_LOCK.entrySet().removeIf(entry -> {
			Encounter encounter = ACTIVE.get(entry.getValue());
			return encounter == null;
		});
	}

	private static void tickPlayer(ServerPlayer player, EliteEncounterConfig.Global global) {
		if (!EncounterDef.canTrigger(player)) {
			return;
		}
		ServerLevel level = (ServerLevel) player.level();
		long nowTick = level.getGameTime();

		Encounter active = ACTIVE.get(player.getUUID());
		if (active != null) {
			if (!player.isAlive()) {
				cancel(active, player, true);
				return;
			}
			advance(active, player, nowTick);
			return;
		}
		if (isCoolingDown(player)) {
			return;
		}
		// 连续型触发采样（深暗境愤怒持续 / 海洋境浸泡累积）
		if (EncounterDef.byId("overwarden").matches(level, player.blockPosition())) {
			// 性能（1.7.3）：扫描含 64³ AABB 实体搜索，每 tick 跑会持续吃满一个采样槽；
			// 愤怒判定窗口是 600 tick，5 tick 采样（=600±5）精度足够。清零分支仍每 tick
			// 保持响应（玩家跑出 32 格立即断连）。
			if (nowTick % WARDEN_SCAN_INTERVAL_TICKS == 0L) {
				scanWardenAnger(player, level);
			}
		} else {
			WARDEN_ANGER.remove(player.getUUID());
		}
		if (EncounterDef.byId("tidal").matches(level, player.blockPosition())) {
			noteSoakTick(player);
		}
		// 触发检测：按难度梯度顺序（沼泽 → 下界 → 海洋 → 深暗 → 末地），命中即开始
		for (EncounterDef def : EncounterDef.ALL) {
			if (matchesTrigger(def, player, level, nowTick)) {
				start(def, player, level, nowTick);
				return;
			}
		}
	}

	// ============ 触发判定 ============

	private static boolean matchesTrigger(EncounterDef def, ServerPlayer player, ServerLevel level,
			long nowTick) {
		EliteEncounterConfig.Realm rules = EliteEncounterConfig.realm(def.id());
		if (!rules.enabled()) {
			return false;
		}
		if (!def.matches(level, player.blockPosition())) {
			return false;
		}
		UUID id = player.getUUID();
		var thresholds = rules.thresholds();
		return switch (def.trigger()) {
			case WARDEN_ANGER -> WARDEN_ANGER.getOrDefault(id, 0) >= thresholds.wardenAngerTicks();
			case WITHER_ACCUMULATION -> counter(WITHER_TICKS, id).sum(nowTick,
					thresholds.witherWindowTicks()) >= thresholds.witherHoldTicks();
			case SOAK_OR_LASER -> counter(SOAK_TICKS, id).sum(nowTick, thresholds.soakWindowTicks())
					>= thresholds.soakTicks()
					|| counter(LASER_HITS, id).sum(nowTick, thresholds.laserWindowTicks())
					>= thresholds.laserHits();
			case NIGHT_WITCH -> isNight(level, thresholds)
					&& (counter(POTION_HITS, id).sum(nowTick, thresholds.hagWindowTicks())
					>= thresholds.potionHits()
					|| counter(WITCH_KILLS, id).sum(nowTick, thresholds.hagWindowTicks())
					>= thresholds.witchKills());
			case PEARL_USES -> counter(PEARL_USES, id).sum(nowTick,
					thresholds.pearlWindowTicks()) >= thresholds.pearlUses();
		};
	}

	private static boolean isNight(ServerLevel level, EliteEncounterConfig.Realm.Thresholds thresholds) {
		long time = level.getDefaultClockTime() % 24_000L;
		return time >= thresholds.nightStart() && time <= thresholds.nightEnd();
	}

	// ============ 1.6.0 主动宣战（图腾入口） ============

	/** 图腾开启结果（WarArtifacts.TotemItem 反馈统一发送） */
	public enum ChallengeResult { OK, NOT_IN_REALM, ACTIVE_OR_COOLDOWN, DISABLED }

	/** 图腾独立冷却（与普通遭遇冷却互不占用，设计 §1.3：每玩家 30 分钟） */
	private static final Map<UUID, Long> TOTEM_COOLDOWN_UNTIL = new ConcurrentHashMap<>();
	private static final long TOTEM_COOLDOWN_MINUTES = 30;

	/**
	 * 主动宣战（1.6.0 §1.3）：跳过触发条件累积直接进入蓄势，生成觉醒变体。
	 * 前置：本境限定群系内；互斥：当前维度已有激活遭遇（普通 / 归一）或图腾冷却中。
	 */
	public static ChallengeResult startChallenged(ServerPlayer player, String realmId) {
		EncounterDef def = EncounterDef.byId(realmId);
		if (def == null || !EliteEncounterConfig.realm(realmId).enabled()) {
			return ChallengeResult.DISABLED;
		}
		ServerLevel level = (ServerLevel) player.level();
		if (!def.matches(level, player.blockPosition())) {
			return ChallengeResult.NOT_IN_REALM;
		}
		Long until = TOTEM_COOLDOWN_UNTIL.get(player.getUUID());
		if (until != null && until > System.currentTimeMillis()) {
			return ChallengeResult.ACTIVE_OR_COOLDOWN;
		}
		if (ACTIVE.containsKey(player.getUUID())
				|| DIMENSION_LOCK.containsKey(level.dimension())
				|| ConvergenceManager.isRunning()) {
			return ChallengeResult.ACTIVE_OR_COOLDOWN;
		}
		long nowTick = level.getGameTime();
		Encounter encounter = new Encounter(def, level, player.getUUID(), nowTick);
		encounter.awakened = true; // 觉醒修饰（§1.4 数值在 awaken() 内应用）
		encounter.anchor = findAnchor(level, player.position());
		ACTIVE.put(player.getUUID(), encounter);
		DIMENSION_LOCK.put(level.dimension(), player.getUUID());
		TOTEM_COOLDOWN_UNTIL.put(player.getUUID(),
				System.currentTimeMillis() + TOTEM_COOLDOWN_MINUTES * MINUTE_MS);
		player.sendOverlayMessage(Component.translatable(
				"message.extra-enchantry.elite.prelude." + def.id()));
		Advancements.award(player, LORDS_ROOT);
		Advancements.award(player, def.triggerAdv());
		// 1.7.0 谱系主线：触遇五境 + 首次宣战（图腾路径）
		LineageManager.onRealmEncounter(player);
		LineageManager.onFirstWar(player);
		ExtraEnchantry.LOGGER.info("[extra-enchantry] 宣战图腾：{} 觉醒遭遇开始（玩家 {}）", def.id(),
				player.getName().getString());
		return ChallengeResult.OK;
	}

	/** 归一之战借用维度锁（ConvergenceManager 调用） */
	public static void lockDimension(ResourceKey<Level> dimension, UUID playerId) {
		DIMENSION_LOCK.put(dimension, playerId);
	}

	public static void unlockDimension(ResourceKey<Level> dimension, UUID playerId) {
		DIMENSION_LOCK.remove(dimension, playerId);
	}

	/** 归一回响的仇恨锁定（复用 LOCKS） */
	public static void lockLord(UUID lordId, UUID playerId) {
		LOCKS.put(lordId, new TargetLock(playerId, Long.MAX_VALUE >> 1));
	}

	// ============ 事件入口（Mixin 回调） ============

	/**
	 * 凋零效果施加（与凋零保护共用 {@code LivingEntity#addEffect} 的 @ModifyVariable 注入点，
	 * 禁止新增第二个同方法 @ModifyVariable——见设计 §7.4 注入冲突预防）。
	 * 同时作为烬骨王隐藏进度「炽焰无伤」的判定依据（全程未受凋零即不成立）。
	 */
	public static void noteWitherApplication(ServerPlayer player, int ticks) {
		if (ticks <= 0) {
			return;
		}
		counter(WITHER_TICKS, player.getUUID()).add(((ServerLevel) player.level()).getGameTime(), ticks);
		Encounter active = ACTIVE.get(player.getUUID());
		if (active != null) {
			active.witherApplied = true;
		}
	}

	/** 监守者对该玩家增加愤怒（{@code Warden#increaseAngerAt} 钩子） */
	public static void noteWardenAnger(ServerPlayer player) {
		Encounter active = ACTIVE.get(player.getUUID());
		if (active != null) {
			active.angerHits = true;
		}
	}

	/** 被守卫者 / 远古守卫者命中（海洋境路径 B） */
	public static void noteGuardianHit(ServerPlayer player) {
		counter(LASER_HITS, player.getUUID()).add(((ServerLevel) player.level()).getGameTime(), 1);
	}

	/** 被女巫投掷药水命中（沼泽境路径 A） */
	public static void noteWitchPotionHit(ServerPlayer player) {
		counter(POTION_HITS, player.getUUID()).add(((ServerLevel) player.level()).getGameTime(), 1);
	}

	/** 击杀女巫（沼泽境路径 B） */
	public static void noteWitchKill(ServerPlayer player) {
		counter(WITCH_KILLS, player.getUUID()).add(((ServerLevel) player.level()).getGameTime(), 1);
	}

	/** 使用末影珍珠（末地境触发） */
	public static void notePearlUse(ServerPlayer player) {
		counter(PEARL_USES, player.getUUID()).add(((ServerLevel) player.level()).getGameTime(), 1);
	}

	/** 玩家受伤（隐藏进度：末地「无惧虚空」记录最低血量比例） */
	public static void notePlayerHurt(ServerPlayer player, DamageSource source) {
		Encounter active = ACTIVE.get(player.getUUID());
		if (active == null) {
			return;
		}
		float ratio = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
		active.minHealthRatio = Math.min(active.minHealthRatio, ratio);
	}

	/** 玩家死亡：取消进行中的遭遇（环境异变保留为代价） */
	public static void onPlayerDeath(ServerPlayer player) {
		Encounter active = ACTIVE.get(player.getUUID());
		if (active != null) {
			cancel(active, player, true);
		}
	}

	// ============ 开始 / 推进 / 取消 ============

	private static void start(EncounterDef def, ServerPlayer player, ServerLevel level, long nowTick) {
		EliteEncounterConfig.Global global = EliteEncounterConfig.global();
		UUID dimensionOwner = DIMENSION_LOCK.get(level.dimension());
		if (dimensionOwner != null && !dimensionOwner.equals(player.getUUID())) {
			return; // 每维度同时 ≤1 个激活遭遇
		}
		Encounter encounter = new Encounter(def, level, player.getUUID(), nowTick);
		Vec3 anchor = findAnchor(level, player.position());
		encounter.anchor = anchor;
		ACTIVE.put(player.getUUID(), encounter);
		DIMENSION_LOCK.put(level.dimension(), player.getUUID());
		player.sendOverlayMessage(Component.translatable("message.extra-enchantry.elite.prelude."
				+ def.id()));
		// 进度树根「境主降临」+ 本境触发进度（幂等）
		Advancements.award(player, LORDS_ROOT);
		Advancements.award(player, def.triggerAdv());
		// 1.7.0 谱系主线：触遇五境（普通遭遇路径）
		LineageManager.onRealmEncounter(player);
		ExtraEnchantry.LOGGER.info("[extra-enchantry] 精英遭遇开始：{}（玩家 {}）", def.id(),
				player.getName().getString());
	}

	private static void advance(Encounter encounter, ServerPlayer player, long nowTick) {
		EncounterDef def = encounter.def;
		EliteEncounterConfig.Realm rules = EliteEncounterConfig.realm(def.id());
		ServerLevel level = encounter.level;

		// 取消路径：离开限定群系 / 距领主 64 格之外
		if (!def.matches(level, player.blockPosition())) {
			cancel(encounter, player, false);
			return;
		}
		if (encounter.lordId != null) {
			Entity lord = level.getEntity(encounter.lordId);
			if (lord == null || !lord.isAlive()) {
				cancel(encounter, player, false);
				return;
			}
			if (lord.distanceTo(player) > CANCEL_DISTANCE) {
				cancel(encounter, player, false);
				return;
			}
		}
		encounter.elapsed++;
		int prelude = rules.preludeTicks();
		int unfold = prelude + rules.unfoldTicks();
		int crack = unfold + rules.crackTicks();

		if (encounter.elapsed == 1) {
			preludeFx(encounter, player);
		} else if (encounter.elapsed == prelude) {
			applyUnfold(encounter, player);
		} else if (rules.crackTicks() > 0 && encounter.elapsed == unfold) {
			crackFx(encounter, player);
		} else if (encounter.elapsed >= crack) {
			awaken(encounter, player);
		}
	}

	/** 蓄势：前兆粒子 + 低沉音（各境不同，逃离窗口） */
	private static void preludeFx(Encounter encounter, ServerPlayer player) {
		ServerLevel level = encounter.level;
		Vec3 pos = player.position();
		switch (encounter.def.id()) {
			case "overwarden" -> {
				LordRuntime.ring(level, pos, 6.0D, 40, ParticleTypes.SCULK_SOUL);
				LordRuntime.sound(level, player, net.minecraft.sounds.SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.6F);
			}
			case "emberbone" -> {
				LordRuntime.ring(level, pos, 5.0D, 30, ParticleTypes.SOUL_FIRE_FLAME);
				LordRuntime.sound(level, player, net.minecraft.sounds.SoundEvents.WITHER_AMBIENT, 0.6F);
			}
			case "tidal" -> {
				LordRuntime.ring(level, pos, 6.0D, 40, ParticleTypes.BUBBLE);
				LordRuntime.sound(level, player, net.minecraft.sounds.SoundEvents.GUARDIAN_AMBIENT, 0.6F);
			}
			case "hag" -> {
				LordRuntime.ring(level, pos, 5.0D, 30, ParticleTypes.WITCH);
				LordRuntime.sound(level, player, net.minecraft.sounds.SoundEvents.WITCH_AMBIENT, 0.8F);
			}
			default -> {
				LordRuntime.ring(level, pos, 6.0D, 40, ParticleTypes.PORTAL);
				LordRuntime.sound(level, player, net.minecraft.sounds.SoundEvents.ENDERMAN_AMBIENT, 0.6F);
			}
		}
	}

	/** 蔓延：环境异变（有界，设计 §6.4） */
	private static void applyUnfold(Encounter encounter, ServerPlayer player) {
		ServerLevel level = encounter.level;
		BlockPos center = player.blockPosition();
		int changed = 0;
		int radius = switch (encounter.def.id()) {
			case "overwarden" -> 2;   // 5×5×5
			case "emberbone" -> 3;     // 7×7
			case "hag" -> 2;           // 5×5
			default -> 0;              // 海洋 / 末地：仅粒子，不改方块
		};
		for (int dx = -radius; dx <= radius && changed < MAX_UNFOLD_BLOCKS; dx++) {
			for (int dz = -radius; dz <= radius && changed < MAX_UNFOLD_BLOCKS; dz++) {
				for (int dy = -1; dy <= 1 && changed < MAX_UNFOLD_BLOCKS; dy++) {
					BlockPos pos = center.offset(dx, dy, dz);
					if (pos.equals(player.blockPosition().below())) {
						continue; // 排除脚下，避免卡住玩家
					}
					var state = level.getBlockState(pos);
					if (state.isAir()) {
						continue;
					}
					boolean applied = switch (encounter.def.id()) {
						case "overwarden" -> (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE))
								&& level.setBlock(pos, Blocks.SCULK_VEIN.defaultBlockState(), 3);
						case "emberbone" -> state.is(Blocks.NETHERRACK)
								&& level.setBlock(pos, level.getRandom().nextBoolean()
								? Blocks.SOUL_SAND.defaultBlockState()
								: Blocks.SOUL_SOIL.defaultBlockState(), 3);
						case "hag" -> state.is(Blocks.GRASS_BLOCK)
								&& level.setBlock(pos, Blocks.MYCELIUM.defaultBlockState(), 3);
						default -> false;
					};
					if (applied) {
						changed++;
					}
				}
			}
		}
		encounter.unfoldApplied = true;
		LordRuntime.ring(level, player.position(), radius + 2.0D, 30, ParticleTypes.LARGE_SMOKE);
	}

	/** 裂缝：最终定位点粒子柱（仅深暗境有独立裂缝阶段） */
	private static void crackFx(Encounter encounter, ServerPlayer player) {
		encounter.anchor = findAnchor(encounter.level, encounter.anchor);
		LordRuntime.column(encounter.level, encounter.anchor, 2.0D, 16, ParticleTypes.SCULK_SOUL);
		LordRuntime.sound(encounter.level, player,
				net.minecraft.sounds.SoundEvents.WARDEN_EMERGE, 0.7F);
	}

	/** 觉醒：生成领主 + 锁定仇恨 10 秒 + 登场效果 */
	private static void awaken(Encounter encounter, ServerPlayer player) {
		ServerLevel level = encounter.level;
		Mob lord = encounter.def.factory().create(level);
		if (lord == null) {
			cancel(encounter, player, false);
			return;
		}
		Vec3 spawn = encounter.anchor;
		lord.snapTo(spawn.x, spawn.y + 0.5D, spawn.z,
				level.getRandom().nextFloat() * 360.0F, 0.0F);
		lord.finalizeSpawn(level, level.getCurrentDifficultyAt(player.blockPosition()),
				EntitySpawnReason.TRIGGERED, null);
		lord.setPersistenceRequired();
		lord.invulnerableTime = 40;
		level.addFreshEntity(lord);
		// lord 静态类型即 Mob（setTarget 在 Mob 上；instanceof LivingEntity 反而降级看不到 setTarget）
		lord.setHealth(lord.getMaxHealth());
		lord.setTarget(player);
		// 1.6.0 图腾路径：觉醒修饰（×1.4 生命 / ×1.2 伤害 / ×0.8 技能冷却，§1.4）
		if (encounter.awakened && lord instanceof EliteLord eliteLord) {
			eliteLord.lordRuntime().markAwakened();
		}		encounter.lordId = lord.getUUID();
		LOCKS.put(lord.getUUID(), new TargetLock(player.getUUID(),
				level.getGameTime() + LOCK_TICKS));
		applyAwakenEffects(encounter, player);
		player.sendOverlayMessage(Component.translatable("message.extra-enchantry.elite.awaken."
				+ encounter.def.id()));
		Advancements.award(player, encounter.def.triggerAdv());
		ExtraEnchantry.LOGGER.info("[extra-enchantry] 领主觉醒：{}（玩家 {}）", encounter.def.id(),
				player.getName().getString());
	}

	private static void applyAwakenEffects(Encounter encounter, ServerPlayer player) {
		switch (encounter.def.id()) {
			case "overwarden" -> {
				player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, true, false));
				// 裂缝处留 3×3 幽匿催发体（环境代价）
				BlockPos center = BlockPos.containing(encounter.anchor);
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						BlockPos pos = center.offset(dx, 0, dz);
						if (encounter.level.getBlockState(pos).isAir()
								|| encounter.level.getBlockState(pos).is(Blocks.SCULK_VEIN)) {
							encounter.level.setBlock(pos, Blocks.SCULK_CATALYST.defaultBlockState(), 3);
						}
					}
				}
			}
			case "emberbone" -> player.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0, true, false));
			case "tidal" -> player.addEffect(
					new MobEffectInstance(MobEffects.MINING_FATIGUE, 100, 2, true, false));
			case "hag" -> {
				player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1, true, false));
				player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 0, true, false));
			}
			default -> player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 0, true, false));
		}
	}

	private static void cancel(Encounter encounter, ServerPlayer player, boolean byDeath) {
		if (ACTIVE.remove(player.getUUID()) == null) {
			return;
		}
		DIMENSION_LOCK.remove(encounter.level.dimension(), player.getUUID());
		// 已生成的领主随事件取消而消散（环境异变保留为代价）
		if (encounter.lordId != null) {
			Entity lord = encounter.level.getEntity(encounter.lordId);
			if (lord != null) {
				if (lord instanceof EliteLord eliteLord) {
					eliteLord.lordRuntime().dispose();
				}
				lord.discard();
			}
			LOCKS.remove(encounter.lordId);
		}
		long minutes = EliteEncounterConfig.global().interruptedCooldownMinutes();
		COOLDOWN_UNTIL.put(player.getUUID(), System.currentTimeMillis() + minutes * MINUTE_MS);
		player.sendOverlayMessage(Component.translatable(byDeath
				? "message.extra-enchantry.elite.cancel.death"
				: "message.extra-enchantry.elite.cancel.left"));
	}

	// ============ 领主死亡 ============

	/**
	 * 领主死亡（{@code ServerLivingEntityEvents.AFTER_DEATH}）：授予击杀进度、
	 * 判定隐藏进度、进入完整冷却；五境全部击杀后授予隐藏「五境巡礼」。
	 */
	public static void onLordDeath(LivingEntity lord, DamageSource source) {
		if (!(lord instanceof EliteLord eliteLord)) {
			return;
		}
		// 1.6.0 归一回响优先转发（中途回响零掉落——防 farming 旁路，设计 §3.6）
		ServerPlayer echoKiller = source.getEntity() instanceof ServerPlayer sp ? sp : null;
		if (ConvergenceManager.isEcho(lord)) {
			eliteLord.lordRuntime().dispose();
			LOCKS.remove(lord.getUUID());
			if (echoKiller != null) {
				ConvergenceManager.onEchoDeath(lord, echoKiller);
			}
			return;
		}
		eliteLord.lordRuntime().dispose();
		LOCKS.remove(lord.getUUID());
		EncounterDef def = eliteLord.lordRuntime().def();
		if (def == null) {
			return;
		}
		// 找到该领主对应的遭遇（同一玩家可能只进行一个）
		Encounter encounter = null;
		for (Encounter candidate : ACTIVE.values()) {
			if (lord.getUUID().equals(candidate.lordId)) {
				encounter = candidate;
				break;
			}
		}
		ServerPlayer killer = source.getEntity() instanceof ServerPlayer sp ? sp : null;
		if (encounter != null) {
			ACTIVE.remove(encounter.playerId);
			DIMENSION_LOCK.remove(encounter.level.dimension(), encounter.playerId);
			if (killer == null) {
				killer = (ServerPlayer) encounter.level.getPlayerByUUID(encounter.playerId);
			}
		}
		if (killer == null) {
			return;
		}
		Advancements.award(killer, def.killAdv());
		if (encounter != null && qualifiesHidden(def, encounter, killer)) {
			Advancements.award(killer, def.hiddenAdv());
		}
		long minutes = EliteEncounterConfig.global().cooldownMinutes();
		COOLDOWN_UNTIL.put(killer.getUUID(), System.currentTimeMillis() + minutes * MINUTE_MS);
		if (allLordsSlain(killer)) {
			Advancements.award(killer, GRAND_TOUR);
		}
		// 1.6.0 觉醒掉落分支（§1.5）：材料 ×2 + 器魂书 II–III 100% + 觉醒徽记 100% + 进度
		if (encounter != null && encounter.awakened) {
			// 1.7.0 谱系主线：首杀觉醒 + 五境觉醒击杀记录（隐藏进度「五境全胜」）
			LineageManager.onFirstAwakenedSlain(killer);
			LineageManager.noteAwakenedRealmKill(killer, def.id());
			RealmTreasures.dropAwakenedLoot(lord, def, encounter.level, killer);
		}
	}

	/** 五境巡礼（全局隐藏）：五境领主全部击杀 */
	private static final ResourceKey<net.minecraft.advancements.Advancement> GRAND_TOUR =
			ResourceKey.create(Registries.ADVANCEMENT, ExtraEnchantry.id("lords/grand_tour"));

	private static boolean qualifiesHidden(EncounterDef def, Encounter encounter, ServerPlayer killer) {
		return switch (def.id()) {
			// 无声狩猎：全程未被任何监守者系 increaseAngerAt 命中
			case "overwarden" -> !encounter.angerHits;
			// 炽焰无伤：全程未受到凋零效果
			case "emberbone" -> !encounter.witherApplied;
			// 旱地屠龙：非水中状态下击杀
			case "tidal" -> !killer.isInWater();
			// 以毒攻毒：击杀时自身携带中毒
			case "hag" -> killer.hasEffect(MobEffects.POISON);
			// 无惧虚空：全程生命值未低于 50%
			default -> encounter.minHealthRatio >= 0.5F;
		};
	}

	private static boolean allLordsSlain(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return false;
		}
		for (EncounterDef def : EncounterDef.ALL) {
			AdvancementHolder holder = server.getAdvancements().get(def.killAdv().identifier());
			if (holder == null
					|| !player.getAdvancements().getOrStartProgress(holder).isDone()) {
				return false;
			}
		}
		return true;
	}

	// ============ 对外接口（Mixin / 其他 Manager 调用） ============

	/** 是否为五境领主（觉醒后进入 boss 判定：断罪不斩杀改 ×2、曳钩不可拉拽） */
	public static boolean isLord(Entity entity) {
		return entity instanceof EliteLord;
	}

	/** 刷怪蛋生成领主（LordSpawnEggs 调用）：按境 ID 构造领主子类实例（不登记遭遇） */
	public static Mob createLordForEgg(ServerLevel level, String realmId) {
		EncounterDef def = EncounterDef.byId(realmId);
		return def != null ? def.factory().create(level) : null;
	}

	/**
	 * 领主目标裁决（MobMixin 调用）：锁定期间强制以触发者为目标。
	 * 与诸界浩劫裁决同级——领主不受浩劫注册表影响，故先判领主。
	 */
	public static LivingEntity resolveLordTarget(Mob mob, LivingEntity target) {
		TargetLock lock = LOCKS.get(mob.getUUID());
		if (lock == null) {
			return target;
		}
		if (mob.level().getGameTime() > lock.untilTick) {
			LOCKS.remove(mob.getUUID());
			return target;
		}
		LivingEntity owner = mob.level().getPlayerByUUID(lock.playerId);
		return owner != null && owner.isAlive() ? owner : target;
	}

	/** 领主击杀经验（{@code getExperienceReward} 注入点：按配置结算） */
	public static int lordExperience(LivingEntity lord) {
		if (!(lord instanceof EliteLord eliteLord)) {
			return -1;
		}
		EncounterDef def = eliteLord.lordRuntime().def();
		return def != null ? EliteEncounterConfig.realm(def.id()).stats().xp() : -1;
	}

	/** 该维度是否有进行中的精英遭遇（诸界浩劫在此期间冻结，设计 §6.2） */
	public static boolean isActiveIn(ResourceKey<Level> dimension) {
		return DIMENSION_LOCK.containsKey(dimension);
	}

	// ============ 冷却与工具 ============

	private static boolean isCoolingDown(ServerPlayer player) {
		Long until = COOLDOWN_UNTIL.get(player.getUUID());
		return until != null && until > System.currentTimeMillis();
	}

	/** 玩家脚下最近的安全地面（裂缝 / 觉醒定位点） */
	private static Vec3 findAnchor(ServerLevel level, Vec3 origin) {
		BlockPos base = BlockPos.containing(origin);
		for (int d = 0; d <= 6; d++) {
			for (int offset : (d == 0 ? new int[]{0} : new int[]{d, -d})) {
				BlockPos pos = base.offset(0, -offset, 0);
				if (level.getBlockState(pos).isSolid() && level.getBlockState(pos.above()).isAir()
						&& level.getBlockState(pos.above(1)).isAir()) {
					return new Vec3(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
				}
			}
		}
		return origin;
	}

	private static SlidingCounter counter(Map<UUID, SlidingCounter> map, UUID id) {
		return map.computeIfAbsent(id, key -> new SlidingCounter());
	}

	/** 每秒分桶的滑动窗口计数器（窗口以 tick 计，内部换算为秒） */
	private static final class SlidingCounter {
		private final TreeMap<Long, Integer> buckets = new TreeMap<>();

		void add(long nowTick, int amount) {
			buckets.merge(nowTick / 20L, amount, Integer::sum);
		}

		int sum(long nowTick, int windowTicks) {
			long from = (nowTick - windowTicks) / 20L;
			int total = 0;
			for (Map.Entry<Long, Integer> entry : buckets.tailMap(from).entrySet()) {
				total += entry.getValue();
			}
			return total;
		}
	}

	/** 领主仇恨锁定（触发者 + 到期 tick） */
	private record TargetLock(UUID playerId, long untilTick) {
	}

	/** 单场遭遇运行时状态 */
	private static final class Encounter {
		final EncounterDef def;
		final ServerLevel level;
		final UUID playerId;
		final long startedTick;
		int elapsed;
		Vec3 anchor;
		UUID lordId;
		boolean unfoldApplied;
		/** 1.6.0 图腾路径：觉醒变体（×1.4 生命 / ×1.2 伤害 / ×0.8 技能冷却） */
		boolean awakened;
		/** 隐藏进度判定：是否被监守者愤怒命中 / 是否受过凋零 / 最低血量比例 */
		boolean angerHits;
		boolean witherApplied;
		float minHealthRatio = 1.0F;

		Encounter(EncounterDef def, ServerLevel level, UUID playerId, long startedTick) {
			this.def = def;
			this.level = level;
			this.playerId = playerId;
			this.startedTick = startedTick;
		}
	}

	// ============ 深暗境：监守者愤怒持续检测（每 tick 由 tickPlayer 前的扫描触发） ============

	/**
	 * 深暗境触发检测（在 {@link #tick} 中对深暗之域玩家调用）：
	 * 32 格内存在普通监守者对该玩家愤怒 ≥ ANGRY 档 → 连续 tick 累加，否则清零。
	 * 达阈值后由 {@link #matchesTrigger} 完成触发。
	 */
	public static void scanWardenAnger(ServerPlayer player, ServerLevel level) {
		UUID id = player.getUUID();
		boolean angry = false;
		for (Warden warden : level.getEntitiesOfClass(Warden.class,
				net.minecraft.world.phys.AABB.ofSize(player.position(), 64.0D, 64.0D, 64.0D))) {
			if (warden.isAlive() && !(warden instanceof EliteLord)
					&& warden.getAngerManagement().getActiveAnger(player) >= AngerLevel.ANGRY.getMinimumAnger()
					&& warden.distanceTo(player) <= 32.0D) {
				angry = true;
				break;
			}
		}
		int value = angry ? WARDEN_ANGER.getOrDefault(id, 0) + 1 : 0;
		WARDEN_ANGER.put(id, value);
	}

	/** 浸泡 / 游泳累积（海洋境路径 A，每秒 20 tick 计） */
	public static void noteSoakTick(ServerPlayer player) {
		if (player.isInWater() && player.isSwimming()) {
			counter(SOAK_TICKS, player.getUUID()).add(((ServerLevel) player.level()).getGameTime(), 1);
		}
	}

	/** 旁观者 / 创造模式玩家清理（避免残留计数与状态） */
	public static void onPlayerLeave(ServerPlayer player) {
		Encounter encounter = ACTIVE.remove(player.getUUID());
		if (encounter != null) {
			DIMENSION_LOCK.remove(encounter.level.dimension(), player.getUUID());
			if (encounter.lordId != null) {
				LOCKS.remove(encounter.lordId);
			}
		}
		WITHER_TICKS.remove(player.getUUID());
		SOAK_TICKS.remove(player.getUUID());
		LASER_HITS.remove(player.getUUID());
		POTION_HITS.remove(player.getUUID());
		WITCH_KILLS.remove(player.getUUID());
		PEARL_USES.remove(player.getUUID());
		WARDEN_ANGER.remove(player.getUUID());
	}

	/** 遗弃物品清理（兜底）：领主死亡后场上不应残留本 mod 的临时生成物 */
	public static void purgeOrphans(ServerLevel level) {
		Iterator<UUID> iterator = LOCKS.keySet().iterator();
		while (iterator.hasNext()) {
			UUID lordId = iterator.next();
			Entity entity = null;
			for (Encounter encounter : ACTIVE.values()) {
				if (lordId.equals(encounter.lordId)) {
					entity = encounter.level.getEntity(lordId);
					break;
				}
			}
			if (entity == null || !entity.isAlive()) {
				iterator.remove();
			}
		}
	}

	/** 调试信息（命令 /extraenchantry 可拓展）：当前激活数与维度锁 */
	public static List<String> debugLines() {
		List<String> lines = new ArrayList<>();
		lines.add("active=" + ACTIVE.size() + " locks=" + LOCKS.size());
		for (Map.Entry<ResourceKey<Level>, UUID> entry : DIMENSION_LOCK.entrySet()) {
			lines.add("dimension " + entry.getKey().identifier() + " -> " + entry.getValue());
		}
		return lines;
	}

	/** 铁砧修复材料判定（AnvilMenuMixin 调用）：五种材料之一 */
	public static boolean isRepairMaterial(ItemStack stack) {
		return !stack.isEmpty() && RealmTreasures.MATERIALS.contains(stack.getItem());
	}

	/** 未使用的占位（保持 API 完整：领主召唤物统一走 LordRuntime.spawnMinion） */
	public static ItemStack soulSandStack() {
		return new ItemStack(Items.SOUL_SAND);
	}

	static {
		// 触发扫描依赖的常量校验：五境定义必须齐全
		for (EncounterDef def : EncounterDef.ALL) {
			if (def == null) {
				throw new IllegalStateException("[extra-enchantry] 精英遭遇定义缺失");
			}
		}
	}
}
