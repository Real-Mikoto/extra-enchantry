package realmikoto.extraenchantry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 液态幽匿（v1.8.0 §4.2/§6.5）：幽渊的「液态幽匿」——声纹湖与潮汐层水体。
 *
 * 仿原版细雪的"拟流体"实现（无流体注册，规避原版流体状态表引导期冻结风险）：
 * - 无碰撞：生物踏入即缓慢下沉——湖底沉有大量资源（§4.3 声纹湖）；
 * - 踏入即产生涟漪声纹（§6.5「踏入即产生涟漪声纹（极响）」）；
 * - 移动减速：液体阻力（潮汐层缓冲 / 回声层高风险探索区）；
 * - 深息：浸没时消耗氧气（EchoManager 结算，深潜手段可补偿）；
 * - 传播加速：浸没期间声纹 ×2（§6.5「水下几乎等于全图广播」）。
 */
public class LiquidSculkBlock extends Block {

	protected static final VoxelShape EMPTY_SHAPE = Shapes.empty();

	public LiquidSculkBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level,
			BlockPos pos, CollisionContext context) {
		// 液体：无碰撞——沉入湖底
		return EMPTY_SHAPE;
	}

	@Override
	protected VoxelShape getVisualShape(BlockState state, net.minecraft.world.level.BlockGetter level,
			BlockPos pos, CollisionContext context) {
		return EMPTY_SHAPE;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
			InsideBlockEffectApplier applier, boolean pastFallDistance) {
		if (level.isClientSide() || !(level instanceof ServerLevel server)) {
			return;
		}
		// 液体阻力：减速
		entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.55D, 0.75D, 0.55D));
		if (entity instanceof ServerPlayer player) {
			// 踏入即产生涟漪声纹（每次进入湖体触发一次大涟漪；持续浸泡不刷屏）
			if (!player.entityTags().contains(LIQUID_SCULK_TAG)) {
				EchoManager.emit(player, 12.0F);
				FxHelper.ring(server, player, 10.0D, ParticleTypes.SCULK_SOUL, 16);
				FxHelper.play(server, player, SoundEvents.SCULK_BLOCK_BREAK, 0.8F, 0.5F);
			}
			player.addTag(LIQUID_SCULK_TAG);
		}
	}

	/** 浸没标记（实体 scoreboard tag——含 NBT 持久化） */
	public static final String LIQUID_SCULK_TAG = "extra_enchantry_in_liquid_sculk";

	/** 离开液体的涟漪标记清理（EchoManager.tick 调用） */
	public static boolean isInLiquidSculk(Entity entity) {
		return entity.entityTags().contains(LIQUID_SCULK_TAG);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(6) == 0) {
			level.addParticle(ParticleTypes.SCULK_SOUL,
					pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble() * 0.8D,
					pos.getZ() + random.nextDouble(), 0.0D, 0.01D, 0.0D);
		}
	}
}
