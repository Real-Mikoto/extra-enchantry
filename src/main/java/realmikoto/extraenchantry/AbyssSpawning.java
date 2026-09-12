package realmikoto.extraenchantry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 幽渊生态刷新（1.8.2「群」，v1.8.0.2 补四层结构）：声纹驱动 + **Y 轴分层**。
 *
 * 四层（设计稿 §4.2，深度 = 年代）：
 * | 层 | Y 区间 | 主题 | 生物 |
 * | 潮汐层 | < 64 | 入口/缓冲/叙事 | 渊息者（居民）、声纹兽 |
 * | 回声层 | 64–128 | 探索/狩猎/资源 | 回响幽灵（声纹驱动）、幽匿幼体（群） |
 * | 记忆层 | 128–176 | 剧情/解谜 | 记忆残影（中立可对话） |
 * | 渊心 | > 176 | 终局/无相 | 无声者（精英）、渊心守望者（Boss，仅召唤） |
 *
 * 规则：
 * - 声纹 ≥9 → 回声层高概率回响幽灵；≥5 → 低概率幽灵；
 * - 回声层另有幽匿幼体成群刷新（2–4 只）；
 * - 潮汐层安静时小概率声纹兽；渊心极低概率无声者；
 * - 静默区禁刷；上限 40 只。
 */
public final class AbyssSpawning {

	private static final int MAX_ABYSS_MOBS = 40;
	/** 四层分界（Y） */
	public static final int LAYER_TIDAL_MAX = 64;
	public static final int LAYER_ECHO_MAX = 128;
	public static final int LAYER_MEMORY_MAX = 176;

	private static int counter = 0;

	private AbyssSpawning() {
	}

	/** 玩家所在层（0=潮汐 1=回声 2=记忆 3=渊心） */
	public static int layerOf(ServerPlayer player) {
		int y = player.getBlockY();
		if (y < LAYER_TIDAL_MAX) {
			return 0;
		}
		if (y < LAYER_ECHO_MAX) {
			return 1;
		}
		if (y < LAYER_MEMORY_MAX) {
			return 2;
		}
		return 3;
	}

	public static String layerName(int layer) {
		return switch (layer) {
			case 0 -> "潮汐层";
			case 1 -> "回声层";
			case 2 -> "记忆层";
			default -> "渊心";
		};
	}

	/** ServerTickEvents.END_SERVER_TICK（整服）调用 */
	public static void tick(MinecraftServer server) {
		if (++counter < 100) {
			return;
		}
		counter = 0;
		ServerLevel abyss = server.getLevel(AbyssKey.ABYSS);
		if (abyss == null) {
			return;
		}
		int existing = abyss.getEntities(AbyssEntities.ECHO_WRAITH, e -> true).size()
				+ abyss.getEntities(AbyssEntities.RESONANCE_BEAST, e -> true).size()
				+ abyss.getEntities(AbyssEntities.SCULK_LARVA, e -> true).size()
				+ abyss.getEntities(AbyssEntities.SOUNDLESS, e -> true).size();
		if (existing >= MAX_ABYSS_MOBS) {
			return;
		}
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (ServerPlayer player : abyss.players()) {
			if (EchoManager.inDeadZone(player)) {
				continue;
			}
			float echo = EchoManager.lastEcho(player);
			int layer = layerOf(player);

			switch (layer) {
				case 0 -> {
					// 潮汐层：安静时小概率声纹兽（觅食者）+ 渊息者居民（§5.1 潮汐层的活口）
					if (echo < 5.0F && random.nextInt(24) == 0) {
						trySpawn(abyss, player, AbyssEntities.RESONANCE_BEAST, 20, 32);
					}
					if (random.nextInt(40) == 0) {
						trySpawn(abyss, player, AbyssEntities.TIDEBORN, 16, 28);
					}
				}
				case 1 -> {
					// 回声层：声纹驱动的主战场
					if (echo >= 9.0F && random.nextInt(4) == 0) {
						trySpawn(abyss, player, AbyssEntities.ECHO_WRAITH, 16, 32);
					} else if (echo >= 5.0F) {
						if (random.nextInt(16) == 0) {
							trySpawn(abyss, player, AbyssEntities.ECHO_WRAITH, 12, 24);
						}
						// 幽匿幼体：成群"蔓延"（§5.1）——以单一落点为中心聚集扩散
						if (random.nextInt(12) == 0) {
							int count = 2 + random.nextInt(3);
							BlockPos nest = tryFindSpawn(abyss, player, 8, 18);
							if (nest != null) {
								for (int i = 0; i < count; i++) {
									trySpawnAt(abyss, nest, AbyssEntities.SCULK_LARVA, 2);
								}
							}
						}
					} else if (random.nextInt(28) == 0) {
						trySpawn(abyss, player, AbyssEntities.RESONANCE_BEAST, 20, 32);
					}
				}
				case 2 -> {
					// 记忆层：只刷记忆残影（中立，低概率，引导叙事）
					if (random.nextInt(60) == 0) {
						trySpawn(abyss, player, AbyssEntities.MEMORY_SHADE, 16, 28);
					}
				}
				default -> {
					// 渊心：极低概率无声者（精英）
					if (random.nextInt(90) == 0) {
						trySpawn(abyss, player, AbyssEntities.SOUNDLESS, 20, 36);
					}
					// 渊心终局触发（§6.7）：深入无相之空高度带（Y>200）→ 唤醒渊心守望者
					if (player.getBlockY() > 200) {
						HeartWardenEntity.tryAwaken(abyss, player);
					}
				}
			}
		}

		// 静默区「生物退避」（§4.5）：附近的敌对生物本能地离开绝对无声之地
		avoidDeadZones(abyss);
	}

	/** 静默区生物退避：处于静默区附近的怪物获得向外的移动意图 */
	private static void avoidDeadZones(ServerLevel abyss) {
		for (var mob : abyss.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
				new net.minecraft.world.phys.AABB(-1024, 0, -1024, 1024, 256, 1024)
						.inflate(0))) {
			if (!(mob instanceof net.minecraft.world.entity.monster.Monster monster) || !monster.isAlive()) {
				continue;
			}
			// 距静默之石 8 格内 → 向远离方向寻路
			BlockPos pos = monster.blockPosition();
			BlockPos nearestStone = null;
			double best = 8 * 8;
			for (BlockPos p : BlockPos.withinManhattan(pos, 8, 4, 8)) {
				if (abyss.getBlockState(p).is(AbyssBlocks.SILENCE_STONE)) {
					double d = p.distSqr(pos);
					if (d < best) {
						best = d;
						nearestStone = p.immutable();
					}
				}
			}
			if (nearestStone != null) {
				double dx = monster.getX() - (nearestStone.getX() + 0.5);
				double dz = monster.getZ() - (nearestStone.getZ() + 0.5);
				double len = Math.max(0.1D, Math.sqrt(dx * dx + dz * dz));
				var nav = monster.getNavigation();
				nav.moveTo(monster.getX() + dx / len * 10.0D, monster.getY(),
						monster.getZ() + dz / len * 10.0D, 1.1D);
			}
		}
	}

	private static <T extends net.minecraft.world.entity.Mob> void trySpawn(
			ServerLevel level, ServerPlayer anchor, EntityType<T> type, int min, int max) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		double angle = random.nextDouble() * Math.PI * 2;
		double dist = min + random.nextDouble() * (max - min);
		int x = (int) (anchor.getX() + Math.cos(angle) * dist);
		int z = (int) (anchor.getZ() + Math.sin(angle) * dist);
		int baseY = anchor.getBlockY();
		for (int dy = -12; dy <= 12; dy++) {
			BlockPos pos = new BlockPos(x, baseY + dy, z);
			if (isValidSpawn(level, pos)) {
				T mob = type.create(level, EntitySpawnReason.NATURAL);
				if (mob != null) {
					mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
							random.nextFloat() * 360.0F, 0.0F);
					mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos),
							EntitySpawnReason.NATURAL, null);
					level.addFreshEntity(mob);
				}
				return;
			}
		}
	}

	/** 幼体"蔓延"：在锚点附近找一个聚集落点（同一次生成共用一个巢穴中心） */
	private static BlockPos tryFindSpawn(ServerLevel level, ServerPlayer anchor, int min, int max) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		double angle = random.nextDouble() * Math.PI * 2;
		double dist = min + random.nextDouble() * (max - min);
		int x = (int) (anchor.getX() + Math.cos(angle) * dist);
		int z = (int) (anchor.getZ() + Math.sin(angle) * dist);
		int baseY = anchor.getBlockY();
		for (int dy = -12; dy <= 12; dy++) {
			BlockPos pos = new BlockPos(x, baseY + dy, z);
			if (isValidSpawn(level, pos)) {
				return pos;
			}
		}
		return null;
	}

	/** 幼体"蔓延"：在巢穴中心周围 2 格内生成个体 */
	private static <T extends net.minecraft.world.entity.Mob> void trySpawnAt(
			ServerLevel level, BlockPos center, EntityType<T> type, int spread) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		int x = center.getX() + random.nextInt(-spread, spread + 1);
		int z = center.getZ() + random.nextInt(-spread, spread + 1);
		for (int dy = -4; dy <= 4; dy++) {
			BlockPos pos = new BlockPos(x, center.getY() + dy, z);
			if (isValidSpawn(level, pos)) {
				T mob = type.create(level, EntitySpawnReason.NATURAL);
				if (mob != null) {
					mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
							random.nextFloat() * 360.0F, 0.0F);
					mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos),
							EntitySpawnReason.NATURAL, null);
					level.addFreshEntity(mob);
				}
				return;
			}
		}
	}

	private static boolean isValidSpawn(ServerLevel level, BlockPos pos) {
		var below = level.getBlockState(pos.below());
		var at = level.getBlockState(pos);
		var above = level.getBlockState(pos.above());
		boolean solidGround = !below.isAir() && (below.is(Blocks.DEEPSLATE)
				|| below.is(Blocks.SCULK) || below.is(Blocks.BONE_BLOCK)
				|| below.is(Blocks.COBBLED_DEEPSLATE));
		return solidGround && at.isAir() && above.isAir();
	}
}
