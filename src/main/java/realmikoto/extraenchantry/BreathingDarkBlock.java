package realmikoto.extraenchantry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 会呼吸的黑暗（v1.8.0 §3.1）：深渊祭坛门框中央的半透明暗幕。
 *
 * 待唤醒 / 闭合状态下的门洞填充体：半透明、随呼吸明暗起伏（客户端粒子）；
 * 仪式完成时被替换为「深渊之门」（AbyssRitual.openGate）。
 */
public class BreathingDarkBlock extends Block {

	public BreathingDarkBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		// 呼吸：极淡幽光明暗起伏（低速幽匿灵魂粒子）
		if (random.nextInt(3) == 0) {
			double phase = (level.getGameTime() % 80) / 80.0D;
			double intensity = 0.4D + 0.6D * (0.5D + 0.5D * Math.sin(phase * Math.PI * 2));
			if (random.nextDouble() < intensity) {
				level.addParticle(ParticleTypes.SCULK_SOUL,
						pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
						pos.getZ() + random.nextDouble(), 0.0D, 0.01D, 0.0D);
			}
		}
	}
}
