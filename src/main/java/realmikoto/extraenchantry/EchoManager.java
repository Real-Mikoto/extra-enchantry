package realmikoto.extraenchantry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 声纹系统（1.8.1「声」）：幽渊的核心玩法层。
 *
 * 三条法则（DESIGN/1.8.0-design.md §1.3 / §6）：
 * 1. 声即光——玩家的移动/挖掘/攻击/跳跃产生「声纹」：以粒子涟漪在幽匿上显形，
 *    声纹半径越大，周围生物的「觉醒」（索敌）范围越广；
 * 2. 记忆沉淀——声纹事件写入每玩家「最近声纹强度」，供生物 AI 与凝视值读取；
 * 3. 沉默悖论——完全静止（无声纹）累积「深渊凝视」：满值给予黑暗 + 缓慢（幻听压迫），
 *    迫使玩家必须「在动的危险与静的恐惧之间」做出节奏选择。
 *
 * 环境危害：
 * - 深渊凝视（The Gaze）：60 秒无任何声纹 → 黑暗 II + 缓慢 II（凝视值三档递进提示）；
 * - 声纹风暴（Echo Storm）：周期事件（每 8–12 分钟，持续 60 秒），声纹传播距离 ×2.5；
 * - 静默区（Dead Zone）：以「静默之石」方块群为源（worldgen 静默区 patch + 玩家摆放
 *   的 3×3 范围）——声纹被吸收、生物退避、禁刷。
 *
 * 传导规则（服务端权威）：
 * - 只在幽渊维度生效；
 * - 潜行移动 → 半径 2；普通移动 → 半径 5；跳跃落地 → 半径 9；
 *   受伤 → 半径 12；破坏/放置方块与攻击由各自事件显式上报；
 * - 静默之石 3×3 内声纹直接归零。
 */
public final class EchoManager {

	/** 声纹回廊（§6.9）：每玩家最近 24 个声纹位置（环形，用于"重播前人走过的声纹"） */
	private static final Map<UUID, java.util.Deque<net.minecraft.core.BlockPos>> TRAILS = new ConcurrentHashMap<>();
	/** 回廊回放计时 */
	private static int corridorCounter = 0;

	/** 每玩家最近一次声纹强度（0 = 静默；普通移动 = 5） */
	private static final Map<UUID, Float> LAST_ECHO = new ConcurrentHashMap<>();
	/** 每玩家最近声纹发生的游戏刻 */
	private static final Map<UUID, Long> LAST_ECHO_TIME = new ConcurrentHashMap<>();
	/** 深渊凝视累积（tick） */
	private static final Map<UUID, Integer> GAZE = new ConcurrentHashMap<>();
	/** 跳跃/落地检测（§6.1：全动作声纹） */
	private static final Map<UUID, Boolean> PREV_ON_GROUND = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> AIR_TICKS = new ConcurrentHashMap<>();
	/** 尖塔"开花"节流 */
	private static long lastBloomTime = 0;
	/** 声纹风暴剩余 tick（>0 = 风暴进行中） */
	private static volatile int stormTicks = 0;
	/** 下次风暴倒计时（tick，8–12 分钟随机） */
	private static volatile int stormCooldown = 20 * 60 * 8;

	private EchoManager() {
	}

	// ============ 声纹上报（各系统调用） ============

	/**
	 * 上报一次声纹事件。
	 *
	 * @param strength 声纹半径（格）；0 表示被静默之石吸收
	 */
	public static void emit(ServerPlayer player, float strength) {
		if (!AbyssKey.isIn(player)) {
			return;
		}
		// 无相之铭（T0，1.8.3）：声纹完全隐匿
		if (AbyssEnchantments.hasAethericInscription(player)) {
			strength = 0;
		}
		// 共鸣调式（§6.6）：静默 ×0.6 / 轰鸣 ×1.5 / 无相 ×0
		float effective = strength * AbyssTuning.multiplierFor(player);
		// 液态幽匿（§6.5）：水下声纹传播加速——几乎等于全图广播
		if (effective > 0 && LiquidSculkBlock.isInLiquidSculk(player)) {
			effective *= 2.0F;
		}
		if (effective > 0 && inDeadZone(player)) {
			effective = 0;
		}
		if (isStormActive()) {
			effective *= 2.5F;
		}
		LAST_ECHO.put(player.getUUID(), effective);
		LAST_ECHO_TIME.put(player.getUUID(), player.level().getGameTime());
		// 凝视清零：发出声纹即「存在」
		GAZE.remove(player.getUUID());
		// 声纹回廊（§6.9）：记录有声纹时的位置轨迹
		if (effective >= 5.0F) {
			java.util.Deque<net.minecraft.core.BlockPos> trail =
					TRAILS.computeIfAbsent(player.getUUID(), id -> new java.util.ArrayDeque<>());
			trail.addLast(player.blockPosition());
			while (trail.size() > 24) {
				trail.removeFirst();
			}
		}

		if (effective >= 5) {
			// 可见涟漪：声纹越大越亮（幽匿灵魂粒子 + 音波圈）
			int count = (int) (4 + effective * 1.2);
			FxHelper.burst(player.level(), player, ParticleTypes.SCULK_SOUL, count, effective * 0.18D);
			if (effective >= 9) {
				FxHelper.ring(player.level(), player, effective, ParticleTypes.SONIC_BOOM, count / 2);
			}
			// 声纹事件 → 客户端同步（§6.2）：让附近玩家的回声视觉随服务端声纹起伏
			AbyssNetworking.broadcastEchoPulse(player, effective);
		}
		// 幽匿尖塔「随声纹开花」（§4.3）：大声纹令附近幽匿绽放脉络
		if (effective >= 9) {
			bloomSpires(player);
		}
	}

	/**
	 * 尖塔开花（§4.3）：大声纹传播到附近幽匿——绽放 SCULK_CHARGE 粒子，
	 * 低概率在幽匿表面长出新的幽匿脉络（幽匿是会"呼吸"的地形）。
	 */
	private static void bloomSpires(ServerPlayer player) {
		long now = player.level().getGameTime();
		if (now - lastBloomTime < 40) {
			return; // 每秒最多一次
		}
		lastBloomTime = now;
		BlockPos center = player.blockPosition();
		int blooms = 0;
		for (BlockPos p : BlockPos.withinManhattan(center, 8, 4, 8)) {
			if (!player.level().getBlockState(p).is(Blocks.SCULK)) {
				continue;
			}
			BlockPos above = p.above();
			if (player.level().getBlockState(above).isAir() && player.level().getRandom().nextInt(6) == 0) {
				player.level().setBlock(above,
						Blocks.SCULK_VEIN.defaultBlockState(), 3);
				blooms++;
			}
			if (blooms >= 3) {
				break;
			}
		}
	}

	/** 上报并广播到幽渊内所有玩家（用于挖掘/放置等位置声源） */
	public static void emitAt(ServerLevel level, double x, double y, double z, float strength) {
		if (level.dimension() != AbyssKey.ABYSS) {
			return;
		}
		float effective = strength * (isStormActive() ? 2.5F : 1.0F);
		if (effective >= 5) {
			FxHelper.burstAt(level, x, y, z, ParticleTypes.SCULK_SOUL,
					(int) (4 + effective), effective * 0.18D);
		}
	}

	// ============ 查询（生物 AI / 其他系统读取） ============

	/** 最近声纹半径（幽渊外恒 0） */
	public static float lastEcho(Player player) {
		return AbyssKey.isIn(player) ? LAST_ECHO.getOrDefault(player.getUUID(), 0.0F) : 0.0F;
	}

	/** 距上次声纹经过的 tick 数（静默判定：长时间无声 = 当前静默） */
	public static int ticksSinceEcho(ServerPlayer player) {
		Long t = LAST_ECHO_TIME.get(player.getUUID());
		long now = player.level().getGameTime();
		return t == null ? Integer.MAX_VALUE : (int) Math.min(Integer.MAX_VALUE, now - t);
	}

	/** 声纹风暴是否进行中 */
	public static boolean isStormActive() {
		return stormTicks > 0;
	}

	/** 玩家是否处于静默区（3×3×3 内有静默之石） */
	/**
	 * 是否处于"共鸣地面"（设计稿 §6.1）：脚下为幽匿或骨块——
	 * 声纹沿幽匿网络扩散、在骨林共鸣腔中被放大。
	 */
	public static boolean inResonantGround(ServerPlayer player) {
		var below = player.level().getBlockState(player.blockPosition().below());
		return below.is(net.minecraft.world.level.block.Blocks.SCULK)
				|| below.is(net.minecraft.world.level.block.Blocks.SCULK_CATALYST)
				|| below.is(net.minecraft.world.level.block.Blocks.BONE_BLOCK)
				|| below.is(net.minecraft.world.level.block.Blocks.SCULK_VEIN);
	}

	public static boolean inDeadZone(ServerPlayer player) {
		BlockPos pos = player.blockPosition();
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (player.level().getBlockState(pos.offset(dx, dy, dz))
							.is(AbyssBlocks.SILENCE_STONE)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// ============ 每 tick（ServerTickEvents，per-player 分发） ============

	/** 由 ExtraEnchantry.onInitialize 的 per-player tick 循环调用 */
	public static void tick(ServerPlayer player) {
		if (!AbyssKey.isIn(player)) {
			GAZE.remove(player.getUUID());
			return;
		}
		// 1.8.3 深潜（Deepdive）：幽渊内夜视补给 + 水环境呼吸补偿
		AbyssEnchantments.applyDeepdive(player);
		// 四层探索：首次抵达某层时授予进度 + 播报层名（设计稿 §4.2）
		AbyssLayerTracker.track(player);
		// 倒悬重力区（§1.4）：渊心（Y>176）局部重力反转——玩家缓慢上浮
		if (player.getBlockY() > AbyssSpawning.LAYER_MEMORY_MAX) {
			player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0, true, false, false));
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, true, false, false));
		}
		UUID id = player.getUUID();
		long now = player.level().getGameTime();
		Long lastTime = LAST_ECHO_TIME.get(id);

		// 移动检测：每 tick 位移 > 0.005 视为「移动」→ 上报基础声纹
		// （潜行减半，冲刺 +40%；跳跃/落地/受伤/攻击由检测与事件显式上报）
		if (!player.isSilent() && !player.isSpectator()) {
			double dx = player.getX() - player.xOld;
			double dy = player.getY() - player.yOld;
			double dz = player.getZ() - player.zOld;
			double moved = dx * dx + dy * dy + dz * dz;
			if (moved > 0.000025D) {
				// 静默（附魔，§5.3）：移动完全不产生声纹——代价是失去回声视觉
				if (AbyssEnchantments.hasStillness(player)) {
					// 无声移动：不上报（凝视照常累积——完全无声即是凝视的养料）
				} else {
					float strength = player.isShiftKeyDown() ? 2.0F : 5.0F;
					if (player.isSprinting()) {
						strength *= 1.4F;
					}
					// 静默斗篷（装备）×0.35 叠加
					if (AbyssGearItem.holds(player, AbyssGearItem.Effect.HUSH)) {
						strength *= 0.35F;
					}
					// 静默之石（物品随身，§5.2）×0.25 叠加
					if (carriesSilenceStone(player)) {
						strength *= 0.25F;
					}
					// 共鸣放大（§6.1）：脚踏幽匿网络 / 骨林共鸣腔 → 声纹放大 ×1.6
					if (inResonantGround(player)) {
						strength *= 1.6F;
					}
					emit(player, strength);
				}
			}
		}

		// 跳跃/落地声纹（§6.1：移动/跳跃/挖掘/攻击皆有声纹）
		boolean onGround = player.onGround();
		int prevAir = AIR_TICKS.getOrDefault(id, 0);
		int air = onGround ? 0 : prevAir + 1;
		AIR_TICKS.put(id, air);
		Boolean prevGround = PREV_ON_GROUND.put(id, onGround);
		if (prevGround != null && !player.isSpectator() && !AbyssEnchantments.hasStillness(player)) {
			if (prevGround && !onGround && player.getDeltaMovement().y > 0.1D) {
				// 起跳：半径 6
				emit(player, 6.0F);
			} else if (!prevGround && onGround && prevAir >= 5) {
				// 落地：半径 9
				emit(player, 9.0F);
			}
		}

		// 液态幽匿（§6.5）：浸没 → 氧气消耗（深息机制）+ 声纹传播加速；离开时清理涟漪标记
		boolean inLiquid = player.level().getBlockState(
				BlockPos.containing(player.getEyePosition())).is(AbyssBlocks.LIQUID_SCULK);
		if (inLiquid) {
			// 深息：氧气缓慢消耗（原版溺水节律）——深潜手段（applyDeepdive）会补偿
			int airSupply = player.getAirSupply();
			if (airSupply > -20) {
				player.setAirSupply(airSupply - 2);
			}
			if (airSupply <= -20 && player.tickCount % 20 == 0) {
				player.hurt(player.damageSources().drown(), 2.0F);
			}
			// 涟漪标记持续存在（离开时清除）
		} else if (LiquidSculkBlock.isInLiquidSculk(player)) {
			player.removeTag(LiquidSculkBlock.LIQUID_SCULK_TAG);
			// 浮出水面：正常呼吸恢复
			if (player.getAirSupply() < player.getMaxAirSupply()) {
				player.setAirSupply(player.getMaxAirSupply());
			}
		}

		// 静默调式被动（§6.6）：轻声细步——潜行时移动速度 +15%（瞬态属性修饰）
		var speedAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
		if (speedAttr != null) {
			var modId = ExtraEnchantry.id("abyss_tuning_sneak");
			var existing = speedAttr.getModifier(modId);
			if (player.isShiftKeyDown() && AbyssTuning.sneakBonus(player)) {
				if (existing == null) {
					speedAttr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
							modId, 0.15D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
				}
			} else if (existing != null) {
				speedAttr.removeModifier(modId);
			}
		}

		// 深渊凝视（§4.5）：完全无声（静止/静默移动）累积——60s / 90s / 120s 三档；
		// 任何声纹上报即清零（emit 内）；无相调式免疫（第九相被动——凝视无从凝聚）
		if ((lastTime == null || now - lastTime > 20) && !AbyssTuning.gazeImmune(player)) {
			int gaze = GAZE.merge(id, 1, Integer::sum);
			if (gaze == 20 * 60) {
				warn(player, "message.extra-enchantry.echo.gaze_1");
			} else if (gaze == 20 * 90) {
				warn(player, "message.extra-enchantry.echo.gaze_2");
				player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false));
			} else if (gaze >= 20 * 120 && gaze % 100 == 0) {
				// 凝视满档：黑暗 + 缓慢循环压迫 + 幻听（§4.5「幻觉与幻听」）
				player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 1, true, false));
				player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1, true, false));
				gazeHallucination(player);
			}
		}

		// 「无声的行者」（§5.5）：每 tick 结算——回声层内零声纹累计
		SilentWalkerTracker.tick(player);
	}

	/** 静默之石（物品）随身携带检测（§5.2：消除声纹） */
	private static boolean carriesSilenceStone(ServerPlayer player) {
		return player.getInventory().hasAnyOf(java.util.Set.of(AbyssResources.SILENCE_STONE));
	}

	// ============ 声音捕获（§5.1 回响幽灵模仿用；ServerLevelMixin 调用） ============

	/** 玩家最近发出的声音（与 noteSound/lastSound 同区声明，避免被外部工具选择性还原） */
	private static final Map<UUID, net.minecraft.sounds.SoundEvent> LAST_SOUND = new ConcurrentHashMap<>();

	/** 记录玩家在幽渊内最近发出的声音（玩家本人播出的声音事件） */
	public static void noteSound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound) {
		if (AbyssKey.isIn(player) && sound != null) {
			LAST_SOUND.put(player.getUUID(), sound);
		}
	}

	/** 玩家最近发出的声音（幽灵模仿源；无记录返回 null） */
	public static net.minecraft.sounds.SoundEvent lastSound(Player player) {
		return LAST_SOUND.get(player.getUUID());
	}

	private static void warn(ServerPlayer player, String key) {
		player.sendSystemMessage(Component.translatable(key));
	}

	/**
	 * 凝视满档幻象（§4.5「幻觉与幻听」）：远处回响幽灵式粒子逼近 +
	 * 伪监守者心跳/洞穴低语——纯粹的感知压迫，不造成实体伤害。
	 */
	private static void gazeHallucination(ServerPlayer player) {
		FxHelper.play(player.level(), player, SoundEvents.AMBIENT_CAVE.value(), 1.0F, 0.5F);
		if (player.level() instanceof ServerLevel server) {
			// 视野边缘的"伪显形"：幽灵粒子圈缓慢收缩（不存在真实实体）
			double angle = server.getRandom().nextDouble() * Math.PI * 2;
			double dist = 10.0 + server.getRandom().nextDouble() * 4.0;
			double x = player.getX() + Math.cos(angle) * dist;
			double z = player.getZ() + Math.sin(angle) * dist;
			for (int i = 0; i < 3; i++) {
				server.sendParticles(ParticleTypes.SCULK_SOUL,
						x + (player.getX() - x) * i / 3.0, player.getY() + 1.0,
						z + (player.getZ() - z) * i / 3.0, 2, 0.2, 0.4, 0.2, 0.0);
			}
			FxHelper.play(server, player, SoundEvents.WARDEN_HEARTBEAT, 0.7F, 0.6F);
		}
	}

	// ============ 服务器级 tick（风暴推进） ============

	/** 跨维度声纹（§2.1）：主世界深暗之域制造的噪音，会从幽渊天顶"漏"下来 */
	private static void leakFromDeepDark(MinecraftServer server) {
		ServerLevel overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
		ServerLevel abyss = server.getLevel(AbyssKey.ABYSS);
		if (overworld == null || abyss == null) {
			return;
		}
		for (ServerPlayer p : overworld.players()) {
			if (!overworld.getBiome(p.blockPosition())
					.is(net.minecraft.world.level.biome.Biomes.DEEP_DARK)) {
				continue;
			}
			// 深暗中的"响"动作（冲刺/受伤）→ 幽渊天顶掉落幽匿尘
			if (p.isSprinting() || p.hurtTime > 0) {
				for (ServerPlayer abyssPlayer : abyss.players()) {
					ServerLevel al = (ServerLevel) abyssPlayer.level();
					al.sendParticles(ParticleTypes.SCULK_SOUL,
							abyssPlayer.getX(), 250.0D, abyssPlayer.getZ(),
							6, 3.0D, 1.0D, 3.0D, 0.02D);
				}
			}
		}
	}

	/** 声纹回廊回放（§6.9）：沿记录轨迹播放幽蓝残影粒子，引导方向 */
	private static void replayCorridors(MinecraftServer server) {
		if (++corridorCounter < 40) {
			return;
		}
		corridorCounter = 0;
		ServerLevel abyss = server.getLevel(AbyssKey.ABYSS);
		if (abyss == null) {
			return;
		}
		for (ServerPlayer player : abyss.players()) {
			// 只有"安静"的玩家才能看到回廊（沉默时才看得见前人的声音）
			if (lastEcho(player) >= 5.0F) {
				continue;
			}
			java.util.Deque<net.minecraft.core.BlockPos> trail = TRAILS.get(player.getUUID());
			if (trail == null || trail.size() < 4) {
				continue;
			}
			int i = 0;
			for (net.minecraft.core.BlockPos p : trail) {
				if (i++ % 3 != 0) {
					continue;
				}
				abyss.sendParticles(ParticleTypes.SCULK_SOUL,
						p.getX() + 0.5, p.getY() + 0.3, p.getZ() + 0.5,
						1, 0.1, 0.1, 0.1, 0.0);
			}
		}
	}

	/** ServerTickEvents.END_SERVER_TICK（整服一次）调用 */
	public static void tickServer(MinecraftServer server) {
		leakFromDeepDark(server);
		replayCorridors(server);
		if (stormTicks > 0) {
			stormTicks--;
			if (stormTicks == 0) {
				for (ServerPlayer player : playersInAbyss(server)) {
					player.sendSystemMessage(Component.translatable("message.extra-enchantry.echo.storm_end"));
				}
			}
			return;
		}
		if (--stormCooldown <= 0) {
			stormCooldown = 20 * 60 * (8 + server.overworld().getRandom().nextInt(5));
			stormTicks = 20 * 60;
			for (ServerPlayer player : playersInAbyss(server)) {
				player.sendSystemMessage(Component.translatable("message.extra-enchantry.echo.storm_start"));
				FxHelper.play(player.level(), player, SoundEvents.WARDEN_SONIC_CHARGE, 0.8F, 0.6F);
			}
		}
	}

	private static List<ServerPlayer> playersInAbyss(MinecraftServer server) {
		List<ServerPlayer> list = new ArrayList<>();
		ServerLevel abyss = server.getLevel(AbyssKey.ABYSS);
		if (abyss != null) {
			for (Player p : abyss.players()) {
				if (p instanceof ServerPlayer sp) {
					list.add(sp);
				}
			}
		}
		return list;
	}

	/** 服务器停止 / 玩家登出清理（v1.7.4 纪律；调式为 Attachment 持久化，无需清理） */
	public static void onDisconnect(UUID playerId) {
		LAST_ECHO.remove(playerId);
		LAST_ECHO_TIME.remove(playerId);
		GAZE.remove(playerId);
		TRAILS.remove(playerId);
		PREV_ON_GROUND.remove(playerId);
		AIR_TICKS.remove(playerId);
		LAST_SOUND.remove(playerId);
		SilentWalkerTracker.onDisconnect(playerId);
	}

	public static void onServerStopped() {
		AbyssLayerTracker.onServerStopped();
		SilentWalkerTracker.onServerStopped();
		TRAILS.clear();
		LAST_ECHO.clear();
		LAST_ECHO_TIME.clear();
		GAZE.clear();
		PREV_ON_GROUND.clear();
		AIR_TICKS.clear();
		stormTicks = 0;
		stormCooldown = 20 * 60 * 8;
	}

	/** AABB 工具（供生物 AI 查询声纹吸引范围；预留 1.8.2 使用） */
	public static AABB attractionBox(Player player, float multiplier) {
		float r = lastEcho(player) * multiplier;
		return new AABB(player.blockPosition()).inflate(r);
	}

	/** 幽匿方块群查询占位（1.8.2 生物索敌扩展） */
	public static boolean sculkNearby(ServerLevel level, BlockPos pos, int radius) {
		for (BlockPos cur : BlockPos.withinManhattan(pos, radius, radius, radius)) {
			if (level.getBlockState(cur).is(Blocks.SCULK)) {
				return true;
			}
		}
		return false;
	}

	/** 静默之石物品（真实物品，v1.8.0.2 起；旧的磁石叙事位已废弃） */
	public static ItemStack silenceStoneItem() {
		return new ItemStack(AbyssResources.SILENCE_STONE);
	}
}
