package realmikoto.extraenchantry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 幽匿母株（v1.8.0.2 补齐，设计稿 §2.2）：所有幽匿的源头。
 *
 * 机制：
 * - randomTick：向周围 3 格内的深板岩/石头扩散幽匿（缓慢生长，形成"会呼吸"的地形）；
 * - 破坏时（onRemove）：清空周围 6 格内的幽匿——**反向机制**：
 *   母株死亡会削弱幽渊的"神经末梢"（叙事上对应主世界幽匿活动的减弱）。
 */
public class SculkRootMotherBlock extends Block {

	public SculkRootMotherBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (random.nextInt(4) != 0) {
			return;
		}
		// 向周围扩散幽匿（3 格内随机 1 处）
		BlockPos target = pos.offset(random.nextInt(7) - 3, random.nextInt(5) - 2, random.nextInt(7) - 3);
		BlockState cur = level.getBlockState(target);
		if (cur.is(Blocks.DEEPSLATE) || cur.is(Blocks.STONE) || cur.is(Blocks.COBBLED_DEEPSLATE)) {
			level.setBlock(target, Blocks.SCULK.defaultBlockState(), 2);
		} else if (cur.isAir() && level.getBlockState(target.below()).is(Blocks.SCULK)) {
			level.setBlock(target, Blocks.SCULK_VEIN.defaultBlockState(), 2);
		}
		// 氛围粒子
		level.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
				pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
				2, 0.4, 0.4, 0.4, 0.0);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel server, BlockPos pos,
			boolean movedByPiston) {
		{
			// 反向机制：母株被破坏 → 周围幽匿大面积失活
			int removed = 0;
			for (BlockPos p : BlockPos.withinManhattan(pos, 6, 4, 6)) {
				BlockState s = server.getBlockState(p);
				if (s.is(Blocks.SCULK) || s.is(Blocks.SCULK_VEIN)) {
					server.setBlock(p, Blocks.DEEPSLATE.defaultBlockState(), 2);
					removed++;
				}
			}
			// 全服播报（幽渊内玩家）
			final int removedCount = removed;
			server.getServer().getPlayerList().getPlayers().forEach(player -> {
				if (AbyssKey.isIn(player)) {
					player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
							"§8幽匿母株被斩断——" + removedCount + " 处脉络同时失活。"));
				}
			});
			server.sendParticles(ParticleTypes.SCULK_SOUL,
					pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
					40, 3.0, 2.0, 3.0, 0.05);
		}
		super.affectNeighborsAfterRemoval(state, server, pos, movedByPiston);
	}
}
