package realmikoto.extraenchantry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * 幽渊传送门（v1.8.0.1 改版）：深渊祭坛框架 + 祭钥点火。
 *
 * 结构（玩家搭建，类似下界传送门）：
 * - 框架：**强化深板岩**（原版方块，远古城市产出）任意 4×5 内空 2×3 的竖直门框；
 * - 点火：手持**深渊祭钥**右键门框内空位 → 校验强化深板岩框架 → 空位填充「深渊之门」方块；
 * - 传送：步入深渊之门（方块 onEntityInside）→ 抵达幽渊；幽渊侧返回门同理。
 *
 * 祭钥耐久 = 打火石（64），每次点火 -1（创造无限）。
 * 粒子：门体常驻幽匿灵魂粒子（clientSide 随机 tick）+ 点火时 SONIC_BOOM 环。
 */
public final class AbyssGate {

	/** 框架主方块：强化深板岩（原版，最优解） */
	public static final Block FRAME_BLOCK = Blocks.REINFORCED_DEEPSLATE;

	/**
	 * 框架方块家族（v1.8.0.5 收紧，对齐 §3.1「被幽匿缝合的深板岩门框」＋用户要求）：
	 * **仅强化深板岩**——远古城市原生框架方块（幽匿缝合由城市结构本身提供）。
	 * 早期放宽为深板岩家族，现按设计收紧：祭钥只回应强化深板岩。
	 */
	public static boolean isFrameBlock(BlockState state) {
		return state.is(Blocks.REINFORCED_DEEPSLATE);
	}
	/** 门内空位填充方块（AbyssPortalBlock 实例，见 AbyssBlocks） */
	public static Block PORTAL_BLOCK;
	/** 框架判定半径（点火点向四周找框架）——委托纯逻辑类 */
	public static final int MAX_FRAME = AbyssGateRules.MAX_FRAME;

	/** 回响锚（§3.4）：持久化于 AbyssRitual.AbyssRitualData（幽渊侧建造，跨重启保留） */

	private AbyssGate() {
	}

	// ============ 框架校验与填充 ============

	/**
	 * 从点火空位出发，校验 XY 平面（竖直）的强化深板岩门框，成功则填充门方块。
	 *
	 * @return true = 点火成功（框架有效且已填充）
	 */
	public static boolean tryIgnite(ServerLevel level, BlockPos inside) {
		// 点火点必须是空气（门洞内）
		if (!level.getBlockState(inside).isAir()) {
			return false;
		}
		// 门框横轴：先试 X 向（东西门面），失败再试 Z 向（南北门面）
		return igniteAxis(level, inside, Direction.Axis.X) || igniteAxis(level, inside, Direction.Axis.Z);
	}

	private static boolean igniteAxis(ServerLevel level, BlockPos inside, Direction.Axis axis) {
		// 纯逻辑扫描（AbyssGateRules.measureHole）：点火点为原点，四个方向找第一个非空气方块
		AbyssGateRules.BlockProbe probe = (dx, dy) -> {
			// dx 沿门框横轴，dy 沿 Y 轴
			BlockPos p = inside.relative(axis, dx).above(dy);
			return level.getBlockState(p).isAir();
		};
		int[] size = AbyssGateRules.measureHole(probe);
		if (size == null) {
			return false;
		}
		int width = size[0];
		int height = size[1];
		if (!AbyssGateRules.isValidDimensions(width, height)) {
			return false;
		}
		// 四个方向的框架方块必须是"深板岩/幽匿"家族（按实际距离取壁面，避免奇偶误差）
		BlockPos leftWall = wallPos(level, inside, axis, -1, true);
		BlockPos rightWall = wallPos(level, inside, axis, 1, true);
		BlockPos bottomWall = wallPos(level, inside, Direction.Axis.Y, -1, false);
		BlockPos topWall = wallPos(level, inside, Direction.Axis.Y, 1, false);
		if (leftWall == null || rightWall == null || bottomWall == null || topWall == null) {
			return false;
		}
		if (!isFrameBlock(level.getBlockState(leftWall))
				|| !isFrameBlock(level.getBlockState(rightWall))
				|| !isFrameBlock(level.getBlockState(bottomWall))
				|| !isFrameBlock(level.getBlockState(topWall))) {
			return false;
		}
		// 校验内部全为空气（不允许夹带杂物）
		for (int dx = 1; dx <= width; dx++) {
			for (int dy = 1; dy <= height; dy++) {
				if (!level.getBlockState(bottomWall.relative(axis, dx).above(dy)).isAir()) {
					return false;
				}
			}
		}
		// 填充门方块
		for (int dx = 1; dx <= width; dx++) {
			for (int dy = 1; dy <= height; dy++) {
				level.setBlock(bottomWall.relative(axis, dx).above(dy),
						PORTAL_BLOCK.defaultBlockState(), 3);
			}
		}
		return true;
	}

	/** 找指定方向的第一个非空气方块（框架壁） */
	private static BlockPos wallPos(ServerLevel level, BlockPos from, Direction.Axis axis, int step,
			boolean alongAxis) {
		BlockPos cursor = from;
		for (int i = 0; i < AbyssGateRules.MAX_FRAME; i++) {
			cursor = alongAxis ? cursor.relative(axis, step) : cursor.above(step);
			if (!level.getBlockState(cursor).isAir()) {
				return cursor;
			}
		}
		return null;
	}

	// ============ 传送（AbyssPortalBlock 调用） ============

	/**
	 * 传送结算（§3.4 锚点与返回 / 防迷航）：
	 * - 返回主世界：幽渊侧已建回响锚 → 落点精确（床/重生点）；未建锚 → 迷航
	 *   （随机落点 + 短暂失明 + 回声失聪）；
	 * - 进入幽渊：存在回响锚 → 精确落至最近锚点；否则默认落点。
	 */
	public static void onEntityInPortal(ServerLevel level, ServerPlayer player) {
		boolean fromAbyss = level.dimension() == AbyssKey.ABYSS;
		ServerLevel target = player.level().getServer().getLevel(fromAbyss ? Level.OVERWORLD : AbyssKey.ABYSS);
		if (target == null) {
			return;
		}
		ServerLevel abyss = player.level().getServer().getLevel(AbyssKey.ABYSS);
		Vec3 dest;
		boolean lost = false;
		if (fromAbyss) {
			// 返回主世界：无锚 → 迷航（§3.4）；有锚 → 精确返回（床/重生点）
			if (abyss != null && AbyssRitual.hasAnyAnchor(abyss)) {
				var spawn = target.getRespawnData().pos();
				dest = findArrivalSpot(target, spawn.getX(), spawn.getZ(), spawn.getY() + 4);
			} else {
				var spawn = target.getRespawnData().pos();
				int angle = target.getRandom().nextInt(360);
				double dist = 128 + target.getRandom().nextInt(384);
				dest = findArrivalSpot(target,
						spawn.getX() + Math.cos(Math.toRadians(angle)) * dist,
						spawn.getZ() + Math.sin(Math.toRadians(angle)) * dist,
						spawn.getY() + 8);
				lost = true;
			}
		} else {
			// 进入幽渊：有锚 → 落至最近锚点；无锚 → 默认落点
			BlockPos anchor = abyss != null ? AbyssRitual.nearestAnchor(abyss, player.getX(), player.getZ()) : null;
			if (anchor != null) {
				dest = new Vec3(anchor.getX() + 0.5, anchor.getY() + 1.0, anchor.getZ() + 0.5);
			} else {
				dest = findArrivalSpot(target, 0, 0, 96);
			}
		}
		target.getChunkSource().addTicketWithRadius(TicketType.PORTAL,
				new ChunkPos((int) dest.x >> 4, (int) dest.z >> 4), 2);
		player.teleport(new TeleportTransition(target, dest, Vec3.ZERO,
				player.getYRot(), 0.0F, TeleportTransition.PLAY_PORTAL_SOUND));
		if (lost) {
			// 迷航（§3.4）：随机落点 + 短暂失明 + 回声失聪
			player.sendSystemMessage(Component.translatable("message.extra-enchantry.gate.lost"));
			player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0, false, false));
			player.addEffect(new MobEffectInstance(ExtraEnchantryEffects.TINNITUS, 300, 0, false, false));
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 0, false, false));
		} else {
			player.sendSystemMessage(Component.translatable(fromAbyss
					? "message.extra-enchantry.gate.return" : "message.extra-enchantry.gate.enter"));
		}
	}

	/** 在目标坐标附近找可站立点（x/z 独立传入——修复旧版 x/z 共用入参的坐标错位 bug） */
	public static Vec3 findArrivalSpot(ServerLevel level, double x, double z, double y) {
		int bx = net.minecraft.util.Mth.floor(x) + 8;
		int bz = net.minecraft.util.Mth.floor(z) + 8;
		var chunk = level.getChunk(bx >> 4, bz >> 4);
		for (int yy = Math.max(level.getMinY() + 2, (int) y - 40); yy <= Math.min(level.getMaxY() - 2, (int) y + 40); yy++) {
			var below = chunk.getBlockState(new BlockPos(bx, yy - 1, bz));
			if (!below.isAir()
					&& chunk.getBlockState(new BlockPos(bx, yy, bz)).isAir()
					&& chunk.getBlockState(new BlockPos(bx, yy + 1, bz)).isAir()) {
				return new Vec3(bx + 0.5, yy, bz + 0.5);
			}
		}
		return new Vec3(bx + 0.5, y, bz + 0.5);
	}

	// ============ 门方块粒子（client tick 由 AbyssPortalBlock.animateTick 提供） ============

	public static void portalParticles(Level level, BlockPos pos, RandomSource random) {
		double x = pos.getX() + random.nextDouble();
		double y = pos.getY() + random.nextDouble();
		double z = pos.getZ() + random.nextDouble();
		level.addParticle(ParticleTypes.SCULK_SOUL, x, y, z, 0.0D, 0.05D, 0.0D);
		if (random.nextInt(6) == 0) {
			level.addParticle(ParticleTypes.PORTAL, x, y, z,
					(random.nextDouble() - 0.5D) * 0.4D, 0.1D, (random.nextDouble() - 0.5D) * 0.4D);
		}
	}

	/** 是否存在任一玩家在幽渊建立了回响锚（§3.3「稳定态」判定；持久化于 AbyssRitual） */
	public static boolean hasAnyAnchor() {
		// 兼容旧签名：由门状态机以 ServerLevel 调用 hasAnyAnchor(ServerLevel)
		return false;
	}

	/** 是否存在回响锚（持久化） */
	public static boolean hasAnyAnchor(ServerLevel anyLevel) {
		return AbyssRitual.hasAnyAnchor(anyLevel);
	}
}
