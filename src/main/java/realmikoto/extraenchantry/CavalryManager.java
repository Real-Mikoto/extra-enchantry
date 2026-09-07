package realmikoto.extraenchantry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.raid.Raider;
import realmikoto.extraenchantry.mixin.CreeperAccessor;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 僵尸马骑兵队与「诸界浩劫」挑战，并接管原版骷髅马陷阱。
 *
 * ============ 诸界浩劫（破限附魔书守护挑战） ============
 *
 * 玩家获得破限附魔书（LimitBreakManager 回调）即开启四波挑战：
 *   - 第 1 波：精英骑兵队（5 名僵尸马骑兵 + 后方 4 名强化骷髅骑士）+ 5 只从地底钻出的监守者；
 *   - 第 2 波：与游戏难度挂钩的原版袭击全部 n 波（3/5/7）一次性生成（含劫掠兽骑手，
 *     按 26.2 Raid 反编译表；非真实袭击，无袭击进度条）；
 *   - 第 3 波：僵尸猪灵 / 僵尸疣猪兽 / 猪灵蛮兵 / 岩浆怪部队 + 3 只凋灵；
 *   - 第 4 波：20 末影人 + 50 末影螨（该挑战下无仇恨，永不索敌）+ 2 只末影龙
 *     （26.2 代码生成的龙无 DragonFight，天然没有 boss 血条）。
 * 每一波额外 5 只幻翼 + 5 只恼鬼；全程每 10 秒在玩家四周生成 10 只闪电苦力怕
 * （2 倍速冲向玩家，引信 30 → 8 tick，即减少 3/4）。
 *
 * 限制：总时限 = 原版袭击超时 ×1.5（Raid.RAID_TIMEOUT_TICKS 48000 → 72000 tick = 60 分钟）；
 * 玩家死亡或超时即挑战失败——残余挑战生物消散，背包中的破限附魔书化为灰烬；
 * 全部四波清空即挑战成功，授予「无敌」进度并解锁破限（进度持久化即解锁持久化）。
 *
 * 仇恨锁定：所有挑战生物 {@code Mob#setTarget} 被 MobMixin 拦截——锁定成员强制
 * 以挑战发起者为唯一目标，怪物间误伤不改变仇恨；无仇恨成员强制空目标。
 * 掉落经验：挑战生物经 getExperienceReward 注入翻倍。
 *
 * ============ 陷阱混编骑兵队（原版雷雨陷阱，弱一档） ============
 *
 * 骷髅马/僵尸马 50/50 混编 + 同类骑手，钻石全套保护 II、铁长矛/普通弓、铁马铠、
 * 自然随机移速；玩家击杀整支（4 名骑手）授予「全歼」进度（跨多支累计）。
 *
 * 掉落：骑手装备掉落率全部为 0；马铠随马死亡照常掉落（原版行为）。
 */
public final class CavalryManager {

	// ============ 精英骑兵队编成 ============

	/** 第 1 波僵尸马骑兵数量 */
	private static final int ELITE_CAVALRY_SIZE = 5;

	/** 第 1 波后方强化骷髅小队数量 */
	private static final int ELITE_SKELETON_GUARD = 4;

	/** 精英队生成距离（格）：以玩家为圆心的环形区间 */
	private static final double SPAWN_DISTANCE_MIN = 20.0;
	private static final double SPAWN_DISTANCE_MAX = 30.0;

	/** 强化骷髅小队在骑兵队后方（远离玩家一侧）的固定偏移 */
	private static final double SKELETON_REAR_OFFSET = 8.0;

	/** 同一横队相邻两骑的间距（格） */
	private static final double LINE_SPACING = 3.0;

	/**
	 * 僵尸马可自然生成的最高移速：ZombieHorse#generateZombieHorseSpeed 满值
	 * (9 + 1 + 1 + 1) / 42.16 ≈ 0.2846。精英队坐骑统一用该值（骷髅马原版固定 0.2）。
	 */
	private static final double MAX_NATURAL_ZOMBIE_HORSE_SPEED = (9.0 + 3.0) / 42.16F;

	// ============ 陷阱混编队编成 ============

	/** 陷阱混编骑兵队数量（与原版陷阱一致） */
	private static final int TRAP_SQUAD_SIZE = 4;

	/** 陷阱混编队新增坐骑相对陷阱马的最大散布半径（格） */
	private static final double TRAP_SPREAD_RADIUS = 2.0;

	// ============ 诸界浩劫参数 ============

	/** 挑战总波数 */
	private static final int CATACLYSM_WAVES = 4;

	/** 原版袭击超时（反编译 Raid.RAID_TIMEOUT_TICKS，2 个游戏日） */
	private static final int RAID_TIMEOUT_TICKS = 48_000;

	/** 挑战总时限 = 原版袭击 ×1.5 = 72000 tick（60 分钟） */
	private static final int CATACLYSM_TIMEOUT_TICKS = (int) (RAID_TIMEOUT_TICKS * 1.5);

	/** 闪电苦力怕生成间隔（tick）＝ 10 秒 */
	private static final int CREEPER_BEAT_TICKS = 200;

	/** 每次生成的闪电苦力怕数量 */
	private static final int CREEPERS_PER_BEAT = 10;

	/** 闪电苦力怕引信（tick）：原版 30 减少 3/4 → 8 */
	private static final int CHARGED_CREEPER_FUSE = 8;

	/** 苦力怕速度加成（ADD_MULTIPLIED_TOTAL +1.0 = 原版两倍速） */
	private static final double CREEPER_SPEED_BONUS = 1.0D;

	/** 每波额外幻翼 / 恼鬼数量 */
	private static final int FODDER_PER_WAVE = 5;

	/** 波与波之间的间隔（tick）＝ 5 秒 */
	private static final int WAVE_DELAY_TICKS = 100;

	/** 第 1 波监守者数量 */
	private static final int WARDENS_WAVE1 = 5;

	/** 第 3 波编成（僵尸猪灵 / 僵尸疣猪兽 / 猪灵蛮兵 / 岩浆怪 / 凋灵） */
	private static final int ZOMBIFIED_PIGLINS_WAVE3 = 15;
	private static final int ZOGLINS_WAVE3 = 6;
	private static final int PIGLIN_BRUTES_WAVE3 = 8;
	private static final int MAGMA_CUBES_WAVE3 = 12;
	private static final int WITHERS_WAVE3 = 3;

	/** 第 4 波编成（末影人 / 末影螨 / 末影龙） */
	private static final int ENDERMEN_WAVE4 = 20;
	private static final int ENDERMITES_WAVE4 = 50;
	private static final int ENDER_DRAGONS_WAVE4 = 2;

	/** 挑战生物生成时相对玩家的最大水平距离（格） */
	private static final double MOB_SPAWN_RADIUS = 20.0;

	// ============ 26.2 原版袭击波次表（反编译 Raid.RaiderType，下标 = 波数） ============

	private static final int[] RAID_BASE_VINDICATOR = {0, 0, 2, 0, 1, 4, 2, 5};
	private static final int[] RAID_BASE_EVOKER = {0, 0, 0, 0, 0, 1, 1, 2};
	private static final int[] RAID_BASE_PILLAGER = {0, 4, 3, 3, 4, 4, 4, 2};
	private static final int[] RAID_BASE_WITCH = {0, 0, 0, 0, 3, 0, 0, 1};
	private static final int[] RAID_BASE_RAVAGER = {0, 0, 0, 1, 0, 1, 0, 2};

	// ============ 状态 ============

	/** 破限书检测的双钩子同 tick 去重（Player#addItem 与 Inventory#add 对同一次拾取都会触发） */
	private static final Map<UUID, Long> LAST_TRIGGER_TICKS = new ConcurrentHashMap<>();

	/** 已处理过的原版陷阱马（同一陷阱只触发一次骑兵队） */
	private static final Set<UUID> PROCESSED_TRAPS = ConcurrentHashMap.newKeySet();

	/** 陷阱混编队成员（骑手）UUID，供「全歼」击败计数（运行时跟踪，重启丢失） */
	private static final Set<UUID> TRAP_SQUAD_MEMBERS = ConcurrentHashMap.newKeySet();

	/** 各玩家对陷阱混编队的累计击杀数 */
	private static final Map<UUID, Integer> TRAP_KILL_COUNTS = new ConcurrentHashMap<>();

	/**
	 * 挑战生物注册表（成员 UUID → 挑战发起者 UUID）：
	 * MobMixin 据此做仇恨锁定/无仇恨，getExperienceReward 据此经验翻倍，
	 * 挑战结束据此清扫残余（含苦力怕与幻翼/恼鬼侧翼，不含坐骑——马铠战利品保留）。
	 */
	private static final Map<UUID, UUID> CHALLENGE_LOCK = new ConcurrentHashMap<>();

	/** 无仇恨挑战成员（第 4 波末影人/末影螨）：强制空目标 */
	private static final Set<UUID> NO_AGGRO_MEMBERS = ConcurrentHashMap.newKeySet();

	/** 进行中的诸界浩劫（挑战发起者 UUID → 状态） */
	private static final Map<UUID, Cataclysm> CATACLYSMS = new ConcurrentHashMap<>();

	/** 骑手装备的槽位（统一掉落率清零用） */
	private static final List<EquipmentSlot> RIDER_SLOTS = List.of(
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
			EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND);

	/** 「无敌」进度：完成诸界浩劫挑战；同时是破限附魔书的解锁标记 */
	private static final ResourceKey<Advancement> ADVANCE_INVINCIBLE =
			ResourceKey.create(Registries.ADVANCEMENT, ExtraEnchantry.id("hidden_challenges/defeat_limit_break_cavalry"));

	/** 「全歼」进度：击败雷雨陷阱召唤的整支混编骑兵队 */
	private static final ResourceKey<Advancement> ADVANCE_ANNIHILATION =
			ResourceKey.create(Registries.ADVANCEMENT, ExtraEnchantry.id("hidden_challenges/defeat_trap_cavalry"));

	private static final String CRITERION_DEFEATED = "extra-enchantry:defeated";

	/** 原版保护附魔（骑手护甲用） */
	private static final ResourceKey<Enchantment> VANILLA_PROTECTION =
			ResourceKey.create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace("protection"));

	private CavalryManager() {
	}

	// ============ 服务端节拍（ExtraEnchantry 注册） ============

	/** 挂 Fabric {@code ServerTickEvents.END_SERVER_TICK}：推进所有进行中的诸界浩劫 */
	public static void tickCataclysms(MinecraftServer server) {
		if (CATACLYSMS.isEmpty()) {
			return;
		}
		for (UUID ownerId : List.copyOf(CATACLYSMS.keySet())) {
			Cataclysm state = CATACLYSMS.get(ownerId);
			if (state == null) {
				continue;
			}
			try {
			ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
			if (player == null) {
				continue; // 玩家离线：挑战冻结（重新上线后继续）
			}
			long now = state.level.getGameTime();
			// 玩家死亡 → 失败
			if (!player.isAlive()) {
				failCataclysm(state, player, true);
				continue;
			}
			// 超时 → 失败
			if (now >= state.deadlineTick) {
				failCataclysm(state, player, false);
				continue;
			}
			// 闪电苦力怕节拍（每 10 秒）+ 监守者补怒
			if (now >= state.nextCreeperTick) {
				state.nextCreeperTick = now + CREEPER_BEAT_TICKS;
				spawnCreeperBeat(state, player);
			}
			// 下一波启动
			if (state.nextWaveTick > 0 && now >= state.nextWaveTick) {
				state.nextWaveTick = -1;
				spawnWave(state, player);
			}
			// 当前波清空检测（每秒扫一次）
			if (state.nextWaveTick < 0 && now % 20 == 0 && isWaveCleared(state)) {
				if (state.wave >= CATACLYSM_WAVES) {
					succeedCataclysm(state, player);
				} else {
					// 该幕清空 → 破限残页派发 + 幕格言（1.3.1「铭文纪元」）
					OnboardingManager.onCataclysmWave(player, state.wave);
					state.nextWaveTick = now + WAVE_DELAY_TICKS;
					state.bossEvent.setProgress(state.wave / (float) CATACLYSM_WAVES);
				}
			}
			} catch (Exception e) {
				ExtraEnchantry.LOGGER.error("[extra-enchantry] 诸界浩劫节拍异常(挑战: {})", ownerId, e);
			}
		}
	}

	/** 当前波是否已清空（同步移除死亡/消失成员的引用后判空） */
	private static boolean isWaveCleared(Cataclysm state) {
		state.waveMembers.removeIf(memberId -> {
			Entity entity = state.level.getEntity(memberId);
			return entity == null || !entity.isAlive();
		});
		return state.waveMembers.isEmpty();
	}

	// ============ 诸界浩劫流程 ============

	/**
	 * 玩家死亡瞬间结算（ServerPlayerMixin 挂 ServerPlayer#die HEAD，背包掉落之前）：
	 * 此刻背包尚在，破限附魔书可就地销毁——若等 tick 级失败检查，背包已掉落，书会留在地上。
	 */
	public static void onPlayerDie(ServerPlayer player) {
		Cataclysm state = CATACLYSMS.get(player.getUUID());
		if (state != null) {
			failCataclysm(state, player, true);
		}
	}
	/**
	 * 精英挑战召唤：玩家获得破限附魔书（LimitBreakManager 回调，每次拾取/给予书籍都会触发）。
	 * 破限已解锁（无敌进度达成）或有进行中的挑战时不再召唤；
	 * 同一 tick 内的双钩子（addItem + Inventory#add）按游戏刻去重。
	 */
	public static void onLimitBreakBookAchieved(ServerPlayer player) {
		if (isLimitBreakUnlocked(player) || CATACLYSMS.containsKey(player.getUUID())) {
			return;
		}
		long tick = player.level().getGameTime();
		Long last = LAST_TRIGGER_TICKS.get(player.getUUID());
		if (last != null && last == tick) {
			return;
		}
		LAST_TRIGGER_TICKS.put(player.getUUID(), tick);
		try {
			startCataclysm(player);
		} catch (Exception e) {
			ExtraEnchantry.LOGGER.error("[extra-enchantry] 诸界浩劫开启失败", e);
		}
	}

	/** 开启诸界浩劫：创建 boss 血条（诸界浩劫）并进入第 1 波 */
	private static void startCataclysm(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		ServerBossEvent bossEvent = new ServerBossEvent(
				UUID.randomUUID(),
				Component.translatable("event.extra-enchantry.cataclysm"),
				BossEvent.BossBarColor.RED,
				BossEvent.BossBarOverlay.PROGRESS);
		Cataclysm state = new Cataclysm(level, player.getUUID(), bossEvent,
				level.getGameTime() + CATACLYSM_TIMEOUT_TICKS);
		CATACLYSMS.put(player.getUUID(), state);
		bossEvent.addPlayer(player);
		player.sendSystemMessage(Component.translatable("message.extra-enchantry.cataclysm.started"));
		spawnWave(state, player);
	}

	/** 开始第 wave 波（每波开始：公告 + 幻翼/恼鬼侧翼；各波编成见类注释） */
	private static void spawnWave(Cataclysm state, ServerPlayer player) {
		state.wave++;
		state.bossEvent.setProgress((state.wave - 1) / (float) CATACLYSM_WAVES);
		player.sendSystemMessage(
				Component.translatable("message.extra-enchantry.cataclysm.wave_start", state.wave));
		switch (state.wave) {
			case 1 -> spawnWaveOne(state, player);
			case 2 -> spawnWaveTwo(state, player);
			case 3 -> spawnWaveThree(state, player);
			default -> spawnWaveFour(state, player);
		}
		spawnWaveFodder(state, player);
	}

	/** 第 1 波：精英骑兵队（含后方强化骷髅小队）+ 5 只从地底钻出的监守者 */
	private static void spawnWaveOne(Cataclysm state, ServerPlayer player) {
		ServerLevel level = state.level;
		Vec3 center = pickSpawnCenter(player, level);
		Vec3 away = new Vec3(center.x - player.getX(), 0.0, center.z - player.getZ());
		if (away.lengthSqr() < 1.0E-4) {
			away = new Vec3(0.0, 0.0, 1.0);
		}
		away = away.normalize();
		Vec3 side = new Vec3(-away.z, 0.0, away.x);
		spawnRiderLine(state, center, side, ELITE_CAVALRY_SIZE, true);
		Vec3 rear = center.add(away.scale(SKELETON_REAR_OFFSET));
		spawnRiderLine(state, rear, side, ELITE_SKELETON_GUARD, false);
		playSquadSound(level, center, true);
		playSquadSound(level, rear, false);

		// 5 只监守者：TRIGGERED 生成 → EMERGING 钻出动画（与尖啸体召唤一致）
		for (int i = 0; i < WARDENS_WAVE1; i++) {
			Warden warden = EntityTypes.WARDEN.create(level, EntitySpawnReason.TRIGGERED);
			if (warden == null) {
				continue;
			}
			DifficultyInstance difficulty = level.getCurrentDifficultyAt(player.blockPosition());
			warden.finalizeSpawn(level, difficulty, EntitySpawnReason.TRIGGERED, null);
			Vec3 pos = randomPosAround(player, 8.0, 14.0);
			Double y = findSpawnY(level, pos.x, player.getY(), pos.z);
			warden.snapTo(pos.x, y != null ? y : player.getY(), pos.z,
					level.getRandom().nextFloat() * 360.0F, 0.0F);
			warden.setPersistenceRequired();
			warden.invulnerableTime = 60;
			registerChallengeMob(state, warden, false);
			state.wardenIds.add(warden.getUUID());
			// 拉满对发起者的愤怒（监守者原生"已有玩家目标时不切换"，配合定期补怒即恒定锁定）
			warden.increaseAngerAt(player, AngerLevel.ANGRY.getMinimumAnger() + 20, true);
			level.addFreshEntity(warden);
		}
	}

	/**
	 * 第 2 波：与游戏难度挂钩的原版袭击全部 n 波一次性生成（易 3 / 普通 5 / 困难 7 波），
	 * 数量与构成按 26.2 反编译 Raid.RaiderType 表 + 难度加成；劫掠兽骑手沿用原版规则
	 * （第 5 波骑掠夺者、第 7 波起骑唤魔者/卫道士）。非真实袭击：无袭击进度条。
	 */
	private static void spawnWaveTwo(Cataclysm state, ServerPlayer player) {
		Difficulty difficulty = state.level.getDifficulty();
		int totalWaves = switch (difficulty) {
			case EASY -> 3;
			case NORMAL -> 5;
			case HARD -> 7;
			default -> 0;
		};
		RandomSource random = state.level.getRandom();
		for (int wave = 1; wave <= totalWaves; wave++) {
			spawnRaidUnits(state, player, EntityTypes.VINDICATOR, RAID_BASE_VINDICATOR[wave],
					raidBonus(random, difficulty, true, wave));
			spawnRaidUnits(state, player, EntityTypes.PILLAGER, RAID_BASE_PILLAGER[wave],
					raidBonus(random, difficulty, true, wave));
			spawnRaidUnits(state, player, EntityTypes.WITCH, RAID_BASE_WITCH[wave],
					raidBonus(random, difficulty, false, wave));
			// 劫掠兽 + 骑手（第 5 波掠夺者；第 7 波起首只唤魔者、其余卫道士——原版规则）
			int ravagersSpawned = 0;
			for (int i = 0; i < RAID_BASE_RAVAGER[wave]; i++) {
				Raider ravager = spawnRaider(state, player, EntityTypes.RAVAGER);
				if (ravager == null) {
					continue;
				}
				Raider rider = null;
				if (wave == 5) {
					rider = spawnRaider(state, player, EntityTypes.PILLAGER);
				} else if (wave >= 7) {
					rider = spawnRaider(state, player,
							ravagersSpawned == 0 ? EntityTypes.EVOKER : EntityTypes.VINDICATOR);
				}
				ravagersSpawned++;
				if (rider != null) {
					rider.snapTo(ravager.getX(), ravager.getY(), ravager.getZ(), ravager.getYRot(), 0.0F);
					rider.startRiding(ravager, false, false);
				}
			}
			spawnRaidUnits(state, player, EntityTypes.EVOKER, RAID_BASE_EVOKER[wave], 0);
		}
	}

	/** 原版袭击的人形/女巫难度加成（26.2 Raid#getPotentialBonusSpawns；无袭击预兆 → 无奖励波） */
	private static int raidBonus(RandomSource random, Difficulty difficulty, boolean humanoid, int wave) {
		if (humanoid) {
			return switch (difficulty) {
				case EASY -> random.nextInt(2);
				case NORMAL -> 1;
				case HARD -> 2;
				default -> 0;
			};
		}
		// 女巫：非简单难度且非第 1/2/4 波时 +1
		if (difficulty == Difficulty.EASY || wave <= 2 || wave == 4) {
			return 0;
		}
		return 1;
	}

	/** 生成一只袭击者（EVENT 生成原因 → 原版自带装备与附魔）并登记为当前波成员 */
	private static Raider spawnRaider(Cataclysm state, ServerPlayer player, EntityType<? extends Raider> type) {
		Raider raider = type.create(state.level, EntitySpawnReason.EVENT);
		if (raider == null) {
			return null;
		}
		raider.finalizeSpawn(state.level,
				state.level.getCurrentDifficultyAt(player.blockPosition()), EntitySpawnReason.EVENT, null);
		Vec3 pos = randomPosAround(player, 12.0, MOB_SPAWN_RADIUS);
		Double y = findSpawnY(state.level, pos.x, player.getY(), pos.z);
		raider.snapTo(pos.x, y != null ? y : player.getY(), pos.z,
				state.level.getRandom().nextFloat() * 360.0F, 0.0F);
		raider.setPersistenceRequired();
		raider.invulnerableTime = 60;
		registerChallengeMob(state, raider, false);
		state.level.addFreshEntity(raider);
		return raider;
	}

	/** 按基础数量 + 难度加成生成一批同类袭击者 */
	private static void spawnRaidUnits(Cataclysm state, ServerPlayer player, EntityType<? extends Raider> type,
			int base, int bonus) {
		for (int i = 0; i < base + bonus; i++) {
			spawnRaider(state, player, type);
		}
	}

	/** 第 3 波：下界部队（僵尸猪灵/僵尸疣猪兽/猪灵蛮兵/岩浆怪）+ 3 只凋灵 */
	private static void spawnWaveThree(Cataclysm state, ServerPlayer player) {
		for (int i = 0; i < ZOMBIFIED_PIGLINS_WAVE3; i++) {
			spawnChallengeMob(state, player, EntityTypes.ZOMBIFIED_PIGLIN, false, 0.0D);
		}
		for (int i = 0; i < ZOGLINS_WAVE3; i++) {
			spawnChallengeMob(state, player, EntityTypes.ZOGLIN, false, 0.0D);
		}
		for (int i = 0; i < PIGLIN_BRUTES_WAVE3; i++) {
			spawnChallengeMob(state, player, EntityTypes.PIGLIN_BRUTE, false, 0.0D);
		}
		for (int i = 0; i < MAGMA_CUBES_WAVE3; i++) {
			spawnChallengeMob(state, player, EntityTypes.MAGMA_CUBE, false, 0.0D);
		}
		for (int i = 0; i < WITHERS_WAVE3; i++) {
			spawnChallengeMob(state, player, EntityTypes.WITHER, false, 10.0D);
		}
	}

	/** 第 4 波：20 末影人 + 50 末影螨（无仇恨）+ 2 末影龙（26.2 代码生成无 boss 血条） */
	private static void spawnWaveFour(Cataclysm state, ServerPlayer player) {
		for (int i = 0; i < ENDERMEN_WAVE4; i++) {
			spawnChallengeMob(state, player, EntityTypes.ENDERMAN, true, 0.0D);
		}
		for (int i = 0; i < ENDERMITES_WAVE4; i++) {
			spawnChallengeMob(state, player, EntityTypes.ENDERMITE, true, 0.0D);
		}
		for (int i = 0; i < ENDER_DRAGONS_WAVE4; i++) {
			spawnChallengeMob(state, player, EntityTypes.ENDER_DRAGON, false, 24.0D);
		}
	}

	/** 每波侧翼：5 只幻翼 + 5 只恼鬼（仇恨锁定，不计入清波判定） */
	private static void spawnWaveFodder(Cataclysm state, ServerPlayer player) {
		for (int i = 0; i < FODDER_PER_WAVE; i++) {
			spawnChallengeMob(state, player, EntityTypes.PHANTOM, false, 14.0D, false);
			spawnChallengeMob(state, player, EntityTypes.VEX, false, 5.0D, false);
		}
	}

	/**
	 * 通用挑战生物生成：玩家四周随机落点（liftUp > 0 时直接抬升 Y，供飞行/空中生物），
	 * 登记仇恨锁定并计入当前波成员。
	 */
	private static <T extends Mob> void spawnChallengeMob(Cataclysm state, ServerPlayer player,
			EntityType<T> type, boolean noAggro, double liftUp) {
		spawnChallengeMob(state, player, type, noAggro, liftUp, true);
	}

	/** 生成重载：countTowardWave 控制是否计入清波判定（幻翼/恼鬼侧翼为 false） */
	private static <T extends Mob> void spawnChallengeMob(Cataclysm state, ServerPlayer player,
			EntityType<T> type, boolean noAggro, double liftUp, boolean countTowardWave) {
		T mob = type.create(state.level, EntitySpawnReason.EVENT);
		if (mob == null) {
			return;
		}
		mob.finalizeSpawn(state.level,
				state.level.getCurrentDifficultyAt(player.blockPosition()), EntitySpawnReason.EVENT, null);
		Vec3 pos = randomPosAround(player, 10.0, MOB_SPAWN_RADIUS);
		double y;
		if (liftUp > 0.0D) {
			y = player.getY() + liftUp;
		} else {
			Double groundY = findSpawnY(state.level, pos.x, player.getY(), pos.z);
			y = groundY != null ? groundY : player.getY();
		}
		mob.snapTo(pos.x, y, pos.z, state.level.getRandom().nextFloat() * 360.0F, 0.0F);
		mob.setPersistenceRequired();
		mob.invulnerableTime = 60;
		registerChallengeMob(state, mob, noAggro, countTowardWave);
		state.level.addFreshEntity(mob);
	}

	/** 闪电苦力怕节拍：10 只闪电苦力怕（2 倍速 + 引信 8 tick）从玩家四周冲来；监守者补怒 */
	private static void spawnCreeperBeat(Cataclysm state, ServerPlayer player) {
		ServerLevel level = state.level;
		for (int i = 0; i < CREEPERS_PER_BEAT; i++) {
			net.minecraft.world.entity.monster.Creeper creeper =
					EntityTypes.CREEPER.create(level, EntitySpawnReason.EVENT);
			if (creeper == null) {
				continue;
			}
			Vec3 pos = randomPosAround(player, 10.0, 18.0);
			Double y = findSpawnY(level, pos.x, player.getY(), pos.z);
			creeper.snapTo(pos.x, y != null ? y : player.getY(), pos.z,
					level.getRandom().nextFloat() * 360.0F, 0.0F);
			// 闪电充能（DATA_IS_POWERED）+ 引信 30 → 8（减少 3/4）+ 2 倍速
			creeper.getEntityData().set(CreeperAccessor.extraenchantry$dataIsPowered(), true);
			((CreeperAccessor) (Object) creeper).extraenchantry$setMaxSwell(CHARGED_CREEPER_FUSE);
			AttributeInstance speed = creeper.getAttribute(Attributes.MOVEMENT_SPEED);
			if (speed != null) {
				speed.addOrUpdateTransientModifier(new AttributeModifier(
						ExtraEnchantry.id("cataclysm_speed"), CREEPER_SPEED_BONUS,
						AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			}
			registerChallengeMob(state, creeper, false, false);
			level.addFreshEntity(creeper);
		}
		// 监守者补怒（愤怒随时间衰减，定期拉回 ANGRY 档维持锁定）
		for (UUID wardenId : state.wardenIds) {
			if (level.getEntity(wardenId) instanceof Warden warden && warden.isAlive()) {
				warden.increaseAngerAt(player, AngerLevel.ANGRY.getMinimumAnger() + 20, false);
			}
		}
	}

	/** 挑战结束清扫：消散该挑战的全部挑战生物（成员+苦力怕+幻翼恼鬼；坐骑保留=马铠战利品） */
	private static void clearChallengeMobs(Cataclysm state) {
		for (Map.Entry<UUID, UUID> entry : List.copyOf(CHALLENGE_LOCK.entrySet())) {
			if (entry.getValue().equals(state.playerId)) {
				Entity entity = state.level.getEntity(entry.getKey());
				if (entity != null) {
					entity.discard();
				}
				NO_AGGRO_MEMBERS.remove(entry.getKey());
				CHALLENGE_LOCK.remove(entry.getKey());
			}
		}
		state.waveMembers.clear();
		state.wardenIds.clear();
	}

	/** 挑战失败：生物消散 + 背包中的破限附魔书化为灰烬（幂等：死亡瞬间已结算则跳过） */
	private static void failCataclysm(Cataclysm state, ServerPlayer player, boolean byDeath) {
		if (CATACLYSMS.remove(state.playerId) == null) {
			return;
		}
		clearChallengeMobs(state);
		destroyLimitBreakBooks(player);
		player.sendSystemMessage(Component.translatable(byDeath
				? "message.extra-enchantry.cataclysm.fail_death"
				: "message.extra-enchantry.cataclysm.fail_timeout"));
		state.bossEvent.removePlayer(player);
		CATACLYSMS.remove(state.playerId);
	}

	/** 挑战成功：授予「无敌」进度（破限解锁标记），残余生物消散（幂等） */
	private static void succeedCataclysm(Cataclysm state, ServerPlayer player) {
		if (CATACLYSMS.remove(state.playerId) == null) {
			return;
		}
		clearChallengeMobs(state);
		awardAdvancement(player, ADVANCE_INVINCIBLE);
		player.sendSystemMessage(Component.translatable("message.extra-enchantry.cataclysm.success"));
		// 第四幕清空 → 残页·肆派发 + 图样收录提示（1.3.1「铭文纪元」）
		OnboardingManager.onCataclysmWave(player, state.wave);
		OnboardingManager.onCataclysmComplete(player);
		state.bossEvent.removePlayer(player);
		CATACLYSMS.remove(state.playerId);
	}

	/** 把背包里的破限附魔书全部移除（仅附魔书，不动已附魔的装备） */
	private static void destroyLimitBreakBooks(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(Items.ENCHANTED_BOOK) && carriesLimitBreak(stack)) {
				inventory.setItem(i, ItemStack.EMPTY);
			}
		}
	}

	private static void registerChallengeMob(Cataclysm state, Mob mob, boolean noAggro) {
		registerChallengeMob(state, mob, noAggro, true);
	}

	/**
	 * 登记挑战生物：仇恨锁定注册表 + 双倍经验。
	 * countTowardWave = 是否计入当前波清空判定（苦力怕与幻翼/恼鬼侧翼不计，否则波永远清不完）。
	 */
	private static void registerChallengeMob(Cataclysm state, Mob mob, boolean noAggro, boolean countTowardWave) {
		CHALLENGE_LOCK.put(mob.getUUID(), state.playerId);
		if (noAggro) {
			NO_AGGRO_MEMBERS.add(mob.getUUID());
		}
		if (countTowardWave) {
			state.waveMembers.add(mob.getUUID());
		}
	}

	// ============ 仇恨锁定（MobMixin 调用） ============

	/** 是否为挑战生物（锁定/无仇恨/双倍经验判定） */
	public static boolean isChallengeMob(Mob mob) {
		return CHALLENGE_LOCK.containsKey(mob.getUUID());
	}

	/**
	 * 挑战生物的目标裁决：
	 *   - 无仇恨成员（末影人/末影螨）→ 永远空目标；
	 *   - 锁定成员 → 强制以挑战发起者为目标（怪物间误伤不改变仇恨）；
	 *   - 发起者离线/死亡后的短暂窗口 → 回落原目标（挑战即将被清扫）。
	 */
	public static LivingEntity resolveChallengeTarget(Mob mob, LivingEntity target) {
		UUID ownerId = CHALLENGE_LOCK.get(mob.getUUID());
		if (ownerId == null) {
			return target;
		}
		if (NO_AGGRO_MEMBERS.contains(mob.getUUID())) {
			return null;
		}
		LivingEntity owner = mob.level().getPlayerByUUID(ownerId);
		return owner != null && owner.isAlive() ? owner : target;
	}

	// ============ 精英骑兵队生成（第 1 波） ============

	/** 沿垂直于推进方向的横队生成一排骑兵（每个生成点独立找安全 Y + 播放生成粒子） */
	private static void spawnRiderLine(Cataclysm state, Vec3 center, Vec3 side, int count, boolean zombieCavalry) {
		for (int i = 0; i < count; i++) {
			double offset = (i - (count - 1) / 2.0) * LINE_SPACING;
			Vec3 pos = center.add(side.scale(offset));
			boolean spawned = zombieCavalry ? spawnZombieCavalry(state, pos) : spawnSkeletonRider(state, pos);
			if (spawned) {
				playSpawnFx(state.level, pos);
			}
		}
	}

	/** 一名精英僵尸马骑兵：坐骑 + 僵尸骑手（骑手 AI 经 26.2 乘客控车机制驱动坐骑） */
	private static boolean spawnZombieCavalry(Cataclysm state, Vec3 pos) {
		ZombieHorse horse = EntityTypes.ZOMBIE_HORSE.create(state.level, EntitySpawnReason.TRIGGERED);
		if (horse == null) {
			return false;
		}
		DifficultyInstance difficulty = state.level.getCurrentDifficultyAt(BlockPos.containing(pos));
		horse.finalizeSpawn(state.level, difficulty, EntitySpawnReason.TRIGGERED, null);
		double y = findSpawnY(state.level, pos.x, pos.y, pos.z);
		horse.snapTo(pos.x, y, pos.z, state.level.getRandom().nextFloat() * 360.0F, 0.0F);
		prepareEliteMount(horse);
		horse.setItemSlot(EquipmentSlot.BODY, rollHorseArmor(state.level));
		horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));

		Zombie rider = buildZombieRider(state.level, difficulty);
		if (rider != null) {
			rider.snapTo(pos.x, y, pos.z, horse.getYRot(), 0.0F);
			rider.startRiding(horse, false, false);
			registerChallengeMob(state, rider, false);
		}
		state.level.addFreshEntityWithPassengers(horse);
		return true;
	}

	/** 一名精英骷髅骑士：坐骑 + 骷髅骑手（弓带断罪） */
	private static boolean spawnSkeletonRider(Cataclysm state, Vec3 pos) {
		SkeletonHorse horse = EntityTypes.SKELETON_HORSE.create(state.level, EntitySpawnReason.TRIGGERED);
		if (horse == null) {
			return false;
		}
		DifficultyInstance difficulty = state.level.getCurrentDifficultyAt(BlockPos.containing(pos));
		horse.finalizeSpawn(state.level, difficulty, EntitySpawnReason.TRIGGERED, null);
		double y = findSpawnY(state.level, pos.x, pos.y, pos.z);
		horse.snapTo(pos.x, y, pos.z, state.level.getRandom().nextFloat() * 360.0F, 0.0F);
		prepareEliteMount(horse);
		horse.setItemSlot(EquipmentSlot.BODY, rollHorseArmor(state.level));
		horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));

		Skeleton rider = buildSkeletonRider(state.level, difficulty);
		if (rider != null) {
			rider.snapTo(pos.x, y, pos.z, horse.getYRot(), 0.0F);
			rider.startRiding(horse, false, false);
			registerChallengeMob(state, rider, false);
		}
		state.level.addFreshEntityWithPassengers(horse);
		return true;
	}

	/** 精英坐骑共性：驯服、持久化、生成保护、满自然移速 */
	private static void prepareEliteMount(AbstractHorse horse) {
		horse.setTamed(true);
		horse.setPersistenceRequired();
		horse.invulnerableTime = 60;
		horse.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(MAX_NATURAL_ZOMBIE_HORSE_SPEED);
	}

	private static Zombie buildZombieRider(ServerLevel level, DifficultyInstance difficulty) {
		Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.TRIGGERED);
		if (zombie == null) {
			return null;
		}
		zombie.finalizeSpawn(level, difficulty, EntitySpawnReason.TRIGGERED, null);
		zombie.setBaby(false);
		equipEliteRider(zombie, level, false);
		return zombie;
	}

	private static Skeleton buildSkeletonRider(ServerLevel level, DifficultyInstance difficulty) {
		Skeleton skeleton = EntityTypes.SKELETON.create(level, EntitySpawnReason.TRIGGERED);
		if (skeleton == null) {
			return null;
		}
		skeleton.finalizeSpawn(level, difficulty, EntitySpawnReason.TRIGGERED, null);
		equipEliteRider(skeleton, level, true);
		return skeleton;
	}

	/**
	 * 精英骑手装备：下界合金四件套每件保护 IV + 活力 III（活力对生物经
	 * LivingEntityMixin 的活力 tick 生效，一名骑兵 +12 HP）；
	 * 僵尸持下界合金长矛（触及 VI + 断罪 I），骷髅持弓（断罪 I）。
	 * 掉落率全部清零——纯挑战，战利品只有随马死亡掉落的马铠。
	 */
	private static void equipEliteRider(Mob rider, ServerLevel level, boolean skeleton) {
		rider.setPersistenceRequired();
		rider.invulnerableTime = 60;
		rider.setItemSlot(EquipmentSlot.HEAD, eliteArmor(level, Items.NETHERITE_HELMET));
		rider.setItemSlot(EquipmentSlot.CHEST, eliteArmor(level, Items.NETHERITE_CHESTPLATE));
		rider.setItemSlot(EquipmentSlot.LEGS, eliteArmor(level, Items.NETHERITE_LEGGINGS));
		rider.setItemSlot(EquipmentSlot.FEET, eliteArmor(level, Items.NETHERITE_BOOTS));
		ItemStack weapon = new ItemStack(skeleton ? Items.BOW : Items.NETHERITE_SPEAR);
		if (!skeleton) {
			weapon.enchant(enchantment(level, ExtraEnchantry.REACH), 6);
		}
		weapon.enchant(enchantment(level, ExtraEnchantry.JUDGEMENT), 1);
		rider.setItemSlot(EquipmentSlot.MAINHAND, weapon);
		for (EquipmentSlot slot : RIDER_SLOTS) {
			rider.setDropChance(slot, 0.0F);
		}
	}

	/** 下界合金一件套：保护 IV + 活力 III */
	private static ItemStack eliteArmor(ServerLevel level, Item item) {
		ItemStack stack = new ItemStack(item);
		stack.enchant(enchantment(level, VANILLA_PROTECTION), 4);
		stack.enchant(enchantment(level, ExtraEnchantry.VITALITY), 3);
		return stack;
	}

	/** 精英骑兵马铠：钻石或金 50/50；金马铠必带余烬 I */
	private static ItemStack rollHorseArmor(ServerLevel level) {
		ItemStack armor = new ItemStack(
				level.getRandom().nextBoolean() ? Items.GOLDEN_HORSE_ARMOR : Items.DIAMOND_HORSE_ARMOR);
		if (armor.is(Items.GOLDEN_HORSE_ARMOR)) {
			armor.enchant(enchantment(level, ExtraEnchantry.EMBERFALL), 1);
		}
		return armor;
	}

	// ============ 陷阱混编骑兵队（雷雨陷阱召唤，弱一档） ============

	/**
	 * 陷阱混编骑兵队：原版骷髅马陷阱触发（SkeletonTrapGoalMixin 在 tick HEAD 取消
	 * 原版生成后回调）。完全接管：复刻原版的状态复位与视觉闪电，把 4 名铁甲骑士
	 * 替换为较弱的骷髅马/僵尸马混编骑兵队。
	 */
	public static void onVanillaTrapTriggered(SkeletonHorse trapHorse) {
		if (!(trapHorse.level() instanceof ServerLevel level)) {
			return;
		}
		// 和平难度直接不消费陷阱（原版生成的骑士也会立即消失），保留给之后的难度
		if (level.getDifficulty() == Difficulty.PEACEFUL) {
			return;
		}
		if (!PROCESSED_TRAPS.add(trapHorse.getUUID())) {
			return;
		}
		// 原版 tick 的首行状态处理：setTrap(false) 会把触发 Goal 从目标选择器移除（防重复触发）
		trapHorse.setTrap(false);
		trapHorse.setTamed(true);
		// 原版视觉闪电（不点燃、保留雷声，是陷阱的招牌提示）
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.snapTo(trapHorse.getX(), trapHorse.getY(), trapHorse.getZ());
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}
		DifficultyInstance difficulty = level.getCurrentDifficultyAt(trapHorse.blockPosition());
		// 陷阱马本体承载第一名骷髅骑手（原版行为），其余 3 骑随机骷髅马/僵尸马
		equipTrapMount(trapHorse);
		spawnTrapRiderOn(level, trapHorse, difficulty, true);
		for (int i = 0; i < TRAP_SQUAD_SIZE - 1; i++) {
			spawnTrapRider(level, trapHorse.position(), difficulty);
		}
	}

	/** 在既有坐骑（陷阱马）上放置一名弱化骷髅骑手并登记击败计数 */
	private static void spawnTrapRiderOn(ServerLevel level, AbstractHorse horse, DifficultyInstance difficulty,
			boolean skeleton) {
		Mob rider = skeleton ? buildTrapSkeleton(level, difficulty) : buildTrapZombie(level, difficulty);
		if (rider == null) {
			return;
		}
		rider.snapTo(horse.getX(), horse.getY(), horse.getZ(), horse.getYRot(), 0.0F);
		rider.startRiding(horse, false, false);
		level.addFreshEntityWithPassengers(rider);
	}

	/** 一名陷阱混编骑兵：随机骷髅马/僵尸马坐骑 + 同类骑手（位置相对陷阱马散布） */
	private static void spawnTrapRider(ServerLevel level, Vec3 center, DifficultyInstance difficulty) {
		RandomSource random = level.getRandom();
		boolean skeletonUnit = random.nextBoolean();
		AbstractHorse horse = skeletonUnit
				? EntityTypes.SKELETON_HORSE.create(level, EntitySpawnReason.TRIGGERED)
				: EntityTypes.ZOMBIE_HORSE.create(level, EntitySpawnReason.TRIGGERED);
		if (horse == null) {
			return;
		}
		horse.finalizeSpawn(level, difficulty, EntitySpawnReason.TRIGGERED, null);
		double x = center.x + (random.nextDouble() - 0.5) * 2.0 * TRAP_SPREAD_RADIUS;
		double z = center.z + (random.nextDouble() - 0.5) * 2.0 * TRAP_SPREAD_RADIUS;
		Double y = findSpawnY(level, x, center.y, z);
		horse.snapTo(x, y != null ? y : center.y, z, random.nextFloat() * 360.0F, 0.0F);
		horse.setTamed(true);
		equipTrapMount(horse);

		Mob rider = skeletonUnit ? buildTrapSkeleton(level, difficulty) : buildTrapZombie(level, difficulty);
		if (rider != null) {
			rider.snapTo(x, y != null ? y : center.y, z, horse.getYRot(), 0.0F);
			rider.startRiding(horse, false, false);
		}
		level.addFreshEntityWithPassengers(horse);
	}

	private static Zombie buildTrapZombie(ServerLevel level, DifficultyInstance difficulty) {
		Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.TRIGGERED);
		if (zombie == null) {
			return null;
		}
		zombie.finalizeSpawn(level, difficulty, EntitySpawnReason.TRIGGERED, null);
		zombie.setBaby(false);
		equipTrapRider(zombie, level, false);
		TRAP_SQUAD_MEMBERS.add(zombie.getUUID());
		return zombie;
	}

	private static Skeleton buildTrapSkeleton(ServerLevel level, DifficultyInstance difficulty) {
		Skeleton skeleton = EntityTypes.SKELETON.create(level, EntitySpawnReason.TRIGGERED);
		if (skeleton == null) {
			return null;
		}
		skeleton.finalizeSpawn(level, difficulty, EntitySpawnReason.TRIGGERED, null);
		equipTrapRider(skeleton, level, true);
		TRAP_SQUAD_MEMBERS.add(skeleton.getUUID());
		return skeleton;
	}

	/**
	 * 陷阱骑手装备（弱于精英队）：钻石全套 + 保护 II（无活力），铁长矛 / 普通弓（无附魔），
	 * 掉落率清零。
	 */
	private static void equipTrapRider(Mob rider, ServerLevel level, boolean skeleton) {
		rider.setPersistenceRequired();
		rider.invulnerableTime = 60;
		rider.setItemSlot(EquipmentSlot.HEAD, trapArmor(level, Items.DIAMOND_HELMET));
		rider.setItemSlot(EquipmentSlot.CHEST, trapArmor(level, Items.DIAMOND_CHESTPLATE));
		rider.setItemSlot(EquipmentSlot.LEGS, trapArmor(level, Items.DIAMOND_LEGGINGS));
		rider.setItemSlot(EquipmentSlot.FEET, trapArmor(level, Items.DIAMOND_BOOTS));
		rider.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(skeleton ? Items.BOW : Items.IRON_SPEAR));
		for (EquipmentSlot slot : RIDER_SLOTS) {
			rider.setDropChance(slot, 0.0F);
		}
	}

	/** 钻石一件套：保护 II */
	private static ItemStack trapArmor(ServerLevel level, Item item) {
		ItemStack stack = new ItemStack(item);
		stack.enchant(enchantment(level, VANILLA_PROTECTION), 2);
		return stack;
	}

	/** 陷阱坐骑：铁马铠（弱于精英队的钻石/金，且防僵尸马日光燃烧），不覆盖自然移速 */
	private static void equipTrapMount(AbstractHorse horse) {
		horse.setPersistenceRequired();
		horse.invulnerableTime = 60;
		horse.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.IRON_HORSE_ARMOR));
		horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
	}

	// ============ 击败判定与进度 ============

	/**
	 * 陷阱混编队成员死亡判定：挂 Fabric {@code ServerLivingEntityEvents.AFTER_DEATH}。
	 * 凶手为玩家时累计击杀数，凑满整队（4 名骑手，跨多支累计）授予「全歼」进度。
	 */
	public static void onTrapRiderDeath(LivingEntity entity, DamageSource source) {
		if (!TRAP_SQUAD_MEMBERS.remove(entity.getUUID())) {
			return;
		}
		if (source.getEntity() instanceof ServerPlayer player
				&& increment(TRAP_KILL_COUNTS, player.getUUID()) >= TRAP_SQUAD_SIZE) {
			TRAP_KILL_COUNTS.remove(player.getUUID());
			awardAdvancement(player, ADVANCE_ANNIHILATION);
		}
	}

	private static int increment(Map<UUID, Integer> counts, UUID playerId) {
		return counts.merge(playerId, 1, Integer::sum);
	}

	/**
	 * 授予进度（幂等：原版 award 已完成时不再触发）。
	 * 26.2 的进度不是注册表而是 {@code ServerAdvancementManager}（按 Identifier 查
	 * AdvancementHolder），走 level.registryAccess() 会报 Missing registry。
	 */
	private static void awardAdvancement(ServerPlayer player, ResourceKey<Advancement> key) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		AdvancementHolder holder = server.getAdvancements().get(key.identifier());
		if (holder == null) {
			ExtraEnchantry.LOGGER.warn("[extra-enchantry] 进度未找到: {}", key.identifier());
			return;
		}
		player.getAdvancements().award(holder, CRITERION_DEFEATED);
	}

	/** 玩家是否已达成指定进度（26.2 经 ServerAdvancementManager 按 Identifier 查询） */
	private static boolean hasAdvancement(ServerPlayer player, ResourceKey<Advancement> key) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return false;
		}
		AdvancementHolder holder = server.getAdvancements().get(key.identifier());
		if (holder == null) {
			return false;
		}
		return player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	// ============ 破限门禁 ============

	/**
	 * 破限是否已解锁：以「无敌」进度为准（进度持久化 = 解锁状态持久化）。
	 * 未解锁时铁砧应用破限附魔书被 AnvilMenuMixin 拦截。
	 */
	public static boolean isLimitBreakUnlocked(ServerPlayer player) {
		return hasAdvancement(player, ADVANCE_INVINCIBLE);
	}

	/** 物品是否携带破限（装备上的附魔或附魔书内的存储附魔） */
	public static boolean carriesLimitBreak(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		if (ExtraEnchantry.hasLimitBreak(stack)) {
			return true;
		}
		ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
		if (stored == null) {
			return false;
		}
		for (Holder<Enchantment> enchantment : stored.keySet()) {
			if (enchantment.is(ExtraEnchantry.LIMIT_BREAK)) {
				return true;
			}
		}
		return false;
	}

	/** 门禁拒绝反馈：动作栏提示 + 村民拒绝音 */
	public static void notifyLimitBreakLocked(ServerPlayer player) {
		player.sendOverlayMessage(Component.translatable("message.extra-enchantry.limit_break_locked"));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 0.8F);
	}

	// ============ 选点与音画 ============

	/**
	 * 在玩家 20~30 格环形区间内挑一个安全中心点：随机 4 次尝试，取第一个找到
	 * 3 格净空的落点；全部失败时退回玩家高度（低概率，接受瞬时卡墙）。
	 */
	private static Vec3 pickSpawnCenter(ServerPlayer player, ServerLevel level) {
		RandomSource random = level.getRandom();
		Vec3 fallback = null;
		for (int attempt = 0; attempt < 4; attempt++) {
			double dist = SPAWN_DISTANCE_MIN + random.nextDouble() * (SPAWN_DISTANCE_MAX - SPAWN_DISTANCE_MIN);
			double angle = random.nextDouble() * Math.PI * 2.0;
			double x = player.getX() + Math.cos(angle) * dist;
			double z = player.getZ() + Math.sin(angle) * dist;
			Double y = findSpawnY(level, x, player.getY(), z);
			if (y != null) {
				return new Vec3(x, y, z);
			}
			if (fallback == null) {
				fallback = new Vec3(x, player.getY(), z);
			}
		}
		return fallback;
	}

	/** 玩家四周随机环形落点（min~max 格） */
	private static Vec3 randomPosAround(ServerPlayer player, double minDist, double maxDist) {
		RandomSource random = player.getRandom();
		double dist = minDist + random.nextDouble() * (maxDist - minDist);
		double angle = random.nextDouble() * Math.PI * 2.0;
		return new Vec3(player.getX() + Math.cos(angle) * dist, player.getY(),
				player.getZ() + Math.sin(angle) * dist);
	}

	/** 从基准高度上下各 8 格扫描 3 格净空（马 + 骑手），找不到返回 null */
	private static Double findSpawnY(ServerLevel level, double x, double baseY, double z) {
		int blockX = (int) Math.floor(x);
		int blockZ = (int) Math.floor(z);
		int baseBlockY = (int) Math.floor(baseY);
		for (int d = 0; d <= 8; d++) {
			int[] offsets = d == 0 ? new int[]{0} : new int[]{d, -d};
			for (int offset : offsets) {
				int y = baseBlockY + offset;
				if (hasClearance(level, blockX, y, blockZ)) {
					return (double) y;
				}
			}
		}
		return null;
	}

	/** 纵向 3 格是否全部无碰撞（传入真实 level 与坐标，避免部分方块形状计算时空参数 NPE） */
	private static boolean hasClearance(ServerLevel level, int x, int y, int z) {
		for (int i = 0; i < 3; i++) {
			if (!level.getBlockState(new BlockPos(x, y + i, z))
					.getCollisionShape(level, new BlockPos(x, y + i, z)).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/** 单个生成点的粒子：竖直幽魂火柱 + 浓烟（服务端生成，自动广播） */
	private static void playSpawnFx(ServerLevel level, Vec3 pos) {
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
				pos.x, pos.y + 0.5D, pos.z, 60, 1.0D, 2.0D, 1.0D, 0.04D);
		level.sendParticles(ParticleTypes.LARGE_SMOKE,
				pos.x, pos.y + 1.0D, pos.z, 16, 0.6D, 1.2D, 0.6D, 0.01D);
	}

	/** 每支队伍一次的登场音：监守者现身声（音量 2.0）+ 对应生物嘶吼，替代闪电雷声 */
	private static void playSquadSound(ServerLevel level, Vec3 pos, boolean zombieSquad) {
		level.playSound(null, pos.x, pos.y, pos.z,
				SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2.0F, zombieSquad ? 0.7F : 0.8F);
		level.playSound(null, pos.x, pos.y, pos.z,
				zombieSquad ? SoundEvents.ZOMBIE_AMBIENT : SoundEvents.SKELETON_AMBIENT,
				SoundSource.HOSTILE, 2.0F, 0.6F);
	}

	/** 查附魔 Holder 的小工具 */
	private static Holder<Enchantment> enchantment(ServerLevel level, ResourceKey<Enchantment> key) {
		return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}

	// ============ 诸界浩劫状态 ============

	/** 单场挑战的全部运行时状态（重启丢失；以挑战发起者的维度为准） */
	private static final class Cataclysm {
		final ServerLevel level;
		final UUID playerId;
		final ServerBossEvent bossEvent;
		/** 当前已开始的波（1-based） */
		int wave;
		/** 挑战超时时刻（游戏刻） */
		final long deadlineTick;
		/** 待开始的下一波时刻（游戏刻；-1 = 无待启动波） */
		long nextWaveTick = -1L;
		/** 下一次闪电苦力怕节拍（游戏刻） */
		long nextCreeperTick;
		/** 当前波必须消灭的成员（清波判定；不含苦力怕与幻翼/恼鬼侧翼） */
		final Set<UUID> waveMembers = ConcurrentHashMap.newKeySet();
		/** 第 1 波监守者（供定期补怒） */
		final Set<UUID> wardenIds = ConcurrentHashMap.newKeySet();

		private Cataclysm(ServerLevel level, UUID playerId, ServerBossEvent bossEvent, long deadlineTick) {
			this.level = level;
			this.playerId = playerId;
			this.bossEvent = bossEvent;
			this.deadlineTick = deadlineTick;
			this.nextCreeperTick = level.getGameTime() + CREEPER_BEAT_TICKS;
		}
	}
}
