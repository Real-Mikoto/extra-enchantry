package realmikoto.extraenchantry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 深渊之门方块（v1.8.0.1）：门框内填充体。
 *
 * - 无碰撞（可走入，同下界传送门）；
 * - 实体踏入（EntityInside）→ 传送（AbyssGate.onEntityInPortal）；
 * - 客户端常驻幽匿灵魂粒子 + 传送粒子（幽渊风格）；
 * - 光照：发光 11（唯一稳定光源，呼应"声即光"的例外——门本身在低语）；
 * - 徒手即可破坏（无掉落）；框架被破坏时连带检查门体悬空（简化：不做连锁检测，
 *   破坏门块本身即可关闭通道）。
 */
public class AbyssPortalBlock extends Block {

	/** 无碰撞 + 可行走穿过 */
	private static final VoxelShape SHAPE = Shapes.empty();

	public AbyssPortalBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
			net.minecraft.world.entity.InsideBlockEffectApplier applier, boolean moved) {
		if (level instanceof ServerLevel server
				&& entity instanceof ServerPlayer player
				&& !player.isShiftKeyDown()) {
			// 门状态机（§3.3）：仅"开启/稳定"态可通行；待唤醒与收束期禁止传送
			if (AbyssGateState.isPassable(server, pos)) {
				AbyssGate.onEntityInPortal(server, player);
			}
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		AbyssGate.portalParticles(level, pos, random);
	}
}
