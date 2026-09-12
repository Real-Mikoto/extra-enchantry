package realmikoto.extraenchantry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 呼吸之门（v1.8.0.2 补齐，设计稿 §4.3「呼吸裂谷」）。
 *
 * 机制：周期性张合——
 * - 张开（默认态）：无碰撞，可通行；
 * - 闭合（每 200 tick 持续 60 tick）：有碰撞，把实体挤压（造成伤害）。
 *
 * 玩家必须观察节奏，在张开窗口内通过。
 */
public class BreathingGateBlock extends Block {

	private static final int PERIOD = 200;
	private static final int CLOSED_TICKS = 60;

	public BreathingGateBlock(Properties properties) {
		super(properties);
	}

	/** 当前是否闭合（由世界时间推导，无需存状态） */
	public static boolean isClosed(Level level) {
		long t = level.getGameTime() % PERIOD;
		return t < CLOSED_TICKS;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		if (level instanceof Level lvl && isClosed(lvl)) {
			return Shapes.block();
		}
		return Shapes.empty();
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
			net.minecraft.world.entity.InsideBlockEffectApplier applier, boolean moved) {
		if (!(level instanceof ServerLevel server) || !isClosed(level)) {
			return;
		}
		// 闭合时挤压：伤害 + 上推（"压碎一切"）
		entity.hurtServer(server, server.damageSources().generic(), 4.0F);
		entity.setDeltaMovement(entity.getDeltaMovement().add(0.0D, 0.35D, 0.0D));
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (isClosed(level) && random.nextInt(6) == 0) {
			level.addParticle(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL,
					pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(),
					0.0, 0.0, 0.0);
		}
	}
}
