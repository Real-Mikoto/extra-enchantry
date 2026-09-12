package realmikoto.extraenchantry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记忆回溯（v1.8.0.2 完整实现，设计稿 §6.4）：可玩闪回。
 *
 * 玩家进入「记忆之门」后以**旁观者**身份重历封印纪元片段：
 * - **可看**：场景由服务端即时搭建（记忆舞台 + 残影 NPC）；
 * - **可走**：自由移动、观察、与残影对话；
 * - **不可改变**：进入后施加"不可破坏/放置"限制（MemoryRestriction），
 *   且不产生声纹（历史不会被你的声音扰动）；
 * - 退出：60 秒后自动返回，或连续潜行 3 次主动退出。
 *
 * 实现要点：闪回舞台建在幽渊高处（Y=248，避开正常地形），
 * 返回时精确还原玩家的位置/朝向/维度。
 */
public final class MemoryFlashback {

	/** 正在闪回中的玩家 → 返回点 */
	private static final Map<UUID, ReturnPoint> ACTIVE = new ConcurrentHashMap<>();
	/** 潜行退出计数 */
	private static final Map<UUID, Integer> SNEAK_COUNT = new ConcurrentHashMap<>();
	/** 剩余 tick */
	private static final Map<UUID, Integer> REMAIN = new ConcurrentHashMap<>();

	private static final int DURATION = 20 * 60;
	/** 记忆舞台基准坐标（幽渊高处，远离正常地形） */
	private static final int STAGE_X = 4096;
	private static final int STAGE_Y = 248;
	private static final int STAGE_Z = 4096;

	private record ReturnPoint(ServerLevel level, Vec3 pos, float yaw, float pitch, int memory) {
	}

	private MemoryFlashback() {
	}

	/** 是否处于闪回中（供破坏/放置拦截查询） */
	public static boolean isInFlashback(ServerPlayer player) {
		return ACTIVE.containsKey(player.getUUID());
	}

	/** 进入闪回（MemoryGateBlock 右键调用） */
	public static void enter(ServerPlayer player, int memory) {
		if (isInFlashback(player)) {
			return;
		}
		ServerLevel level = player.level() instanceof ServerLevel sl ? sl : null;
		ServerLevel abyss = player.level().getServer().getLevel(AbyssKey.ABYSS);
		if (level == null || abyss == null) {
			return;
		}
		// 记录返回点
		ACTIVE.put(player.getUUID(), new ReturnPoint(level, player.position(),
				player.getYRot(), player.getXRot(), memory));
		REMAIN.put(player.getUUID(), DURATION);
		SNEAK_COUNT.put(player.getUUID(), 0);

		// 搭建记忆舞台（幂等：已存在则复用）
		buildStage(abyss, memory);

		// 传送 + 旁观者限制
		player.teleport(new net.minecraft.world.level.portal.TeleportTransition(
				abyss, new Vec3(STAGE_X + 0.5, STAGE_Y + 1, STAGE_Z + 0.5), Vec3.ZERO, 0.0F, 0.0F,
				net.minecraft.world.level.portal.TeleportTransition.PLAY_PORTAL_SOUND));
		player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, DURATION + 100, 0, false, false, false));
		player.sendSystemMessage(Component.literal("§8════ 你以旁观者之身，走入一段渊之记忆 ════"));
		for (String line : AbyssMemories.get(memory)) {
			player.sendSystemMessage(Component.literal(line));
		}
		player.sendSystemMessage(Component.literal("§7（可看、可走、不可改变——潜行三次或 60 秒后返回）"));
		FxHelper.play(abyss, player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 0.4F);
	}

	/** 搭建记忆舞台：一段"封印纪元现场"的静态场景 */
	private static void buildStage(ServerLevel level, int memory) {
		BlockPos base = new BlockPos(STAGE_X, STAGE_Y, STAGE_Z);
		// 舞台地面（5x5 深板岩砖平台）
		for (int dx = -8; dx <= 8; dx++) {
			for (int dz = -8; dz <= 8; dz++) {
				level.setBlock(base.offset(dx, 0, dz), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
			}
		}
		// 场景装饰（按记忆编号不同）
		switch (memory) {
			case 1 -> {
				// 记忆一「沉渊」：声纹湖岸 + 下沉的柱
				for (int i = 0; i < 8; i++) {
					level.setBlock(base.offset(-6 + i, 1, -6), Blocks.SCULK.defaultBlockState(), 2);
					level.setBlock(base.offset(-6 + i, 2, -6), Blocks.SCULK_VEIN.defaultBlockState(), 2);
				}
				for (int dy = 1; dy <= 4; dy++) {
					level.setBlock(base.offset(6, dy, 6), Blocks.DEEPSLATE_TILES.defaultBlockState(), 2);
				}
			}
			case 2 -> {
				// 记忆二「苏醒」：裂隙
				for (int dz = -7; dz <= 7; dz++) {
					level.setBlock(base.offset(0, 1, dz), Blocks.AIR.defaultBlockState(), 2);
					level.setBlock(base.offset(1, 1, dz), Blocks.SCULK.defaultBlockState(), 2);
				}
			}
			default -> {
				// 记忆三「无相」：空——只剩一圈石柱
				for (int deg = 0; deg < 360; deg += 45) {
					double r = 6;
					int x = (int) Math.round(Math.cos(Math.toRadians(deg)) * r);
					int z = (int) Math.round(Math.sin(Math.toRadians(deg)) * r);
					for (int dy = 1; dy <= 3; dy++) {
						level.setBlock(base.offset(x, dy, z), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
					}
				}
			}
		}
		// 记忆残影（观察对象）
		for (int i = 0; i < 3; i++) {
			var shade = AbyssEntities.MEMORY_SHADE.create(level,
					net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
			if (shade != null) {
				shade.snapTo(STAGE_X + 2.0 - i * 2, STAGE_Y + 1, STAGE_Z + 3.0, 180.0F, 0.0F);
				level.addFreshEntity(shade);
			}
		}
	}

	/** 每 tick（EchoManager per-player 循环调用） */
	public static void tick(ServerPlayer player) {
		Integer remain = REMAIN.get(player.getUUID());
		if (remain == null) {
			return;
		}
		// 潜行三次退出
		if (player.isShiftKeyDown()) {
			int n = SNEAK_COUNT.merge(player.getUUID(), 1, Integer::sum);
			if (n >= 60) {   // 连续潜行约 3 秒
				exit(player);
				return;
			}
		} else {
			SNEAK_COUNT.put(player.getUUID(), 0);
		}
		// 旁观者锚定（§6.4）：舞台边界（17×17，Y ±2）——走出边界即回到舞台中心，
		// 记忆不会让旁观者坠落。
		if (Math.abs(player.getBlockX() - STAGE_X) > 8 || Math.abs(player.getBlockZ() - STAGE_Z) > 8
				|| Math.abs(player.getBlockY() - (STAGE_Y + 1)) > 2) {
			player.teleport(new net.minecraft.world.level.portal.TeleportTransition(
					player.level(), new Vec3(STAGE_X + 0.5, STAGE_Y + 1, STAGE_Z + 0.5), Vec3.ZERO,
					player.getYRot(), player.getXRot(),
					net.minecraft.world.level.portal.TeleportTransition.PLAY_PORTAL_SOUND));
			player.sendSystemMessage(Component.translatable("message.extra-enchantry.memory.boundary"));
		}
		// 氛围粒子
		if (player.level() instanceof ServerLevel sl && remain % 10 == 0) {
			sl.sendParticles(ParticleTypes.SCULK_SOUL,
					player.getX(), player.getY() + 1, player.getZ(),
					3, 2.0, 1.0, 2.0, 0.01);
		}
		if (--remain <= 0) {
			exit(player);
			return;
		}
		REMAIN.put(player.getUUID(), remain);
	}

	/** 退出并精确返回 */
	public static void exit(ServerPlayer player) {
		ReturnPoint rp = ACTIVE.remove(player.getUUID());
		REMAIN.remove(player.getUUID());
		SNEAK_COUNT.remove(player.getUUID());
		if (rp == null) {
			return;
		}
		player.teleport(new net.minecraft.world.level.portal.TeleportTransition(
				rp.level(), rp.pos(), Vec3.ZERO, rp.yaw(), rp.pitch(),
				net.minecraft.world.level.portal.TeleportTransition.PLAY_PORTAL_SOUND));
		player.removeEffect(MobEffects.NIGHT_VISION);
		player.sendSystemMessage(Component.literal("§8记忆合拢——你回到了自己的时代。"));
	}

	public static void onDisconnect(UUID id) {
		ACTIVE.remove(id);
		REMAIN.remove(id);
		SNEAK_COUNT.remove(id);
	}

	public static void onServerStopped() {
		ACTIVE.clear();
		REMAIN.clear();
		SNEAK_COUNT.clear();
	}
}
