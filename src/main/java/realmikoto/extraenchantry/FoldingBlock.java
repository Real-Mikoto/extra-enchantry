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
 * 记忆折叠方块（v1.8.0.2 补齐，设计稿 §1.4「空间折叠」）。
 *
 * 机制：同一段路在两次经过时呈现不同年代的样貌——
 * randomTick 时以极低概率在"完好（深板岩砖）"与"废墟（幽匿）"两态间切换，
 * 玩家回望时会看到同一处地形"变了样子"（折叠的错觉）。
 */
public class FoldingBlock extends Block {

	public FoldingBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		// 每 random tick 1/100 概率翻转（"偶尔变化"的折叠错觉，不至于像素级闪烁）
		if (random.nextInt(100) != 0) {
			return;
		}
		boolean ruined = state.is(Blocks.SCULK);
		level.setBlock(pos, ruined
				? Blocks.DEEPSLATE_BRICKS.defaultBlockState()
				: Blocks.SCULK.defaultBlockState(), 2);
		// 折叠时的"年代切换"粒子
		level.sendParticles(ParticleTypes.SCULK_SOUL,
				pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
				6, 0.4, 0.4, 0.4, 0.01);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(20) == 0) {
			level.addParticle(ParticleTypes.SCULK_SOUL,
					pos.getX() + random.nextDouble(), pos.getY() + 1.0, pos.getZ() + random.nextDouble(),
					0.0, 0.02, 0.0);
		}
	}
}
