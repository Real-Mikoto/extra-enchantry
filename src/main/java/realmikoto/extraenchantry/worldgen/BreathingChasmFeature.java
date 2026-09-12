package realmikoto.extraenchantry.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 呼吸裂谷（v1.8.0.2 补齐，设计稿 §4.3）：会周期性张合的深谷。
 *
 * 生成结构：
 * - 沿单一轴向切开的深槽（宽度 3–5，深度 12–20）；
 * - 槽壁铺设幽匿（声纹放大效果）；
 * - 槽底与顶部入口处嵌「呼吸之门」方块（运行时张合，见 BreathingGateBlock）。
 *
 * 玩法：张开时可进入、闭合时压碎一切——节奏解谜。
 */
public class BreathingChasmFeature extends Feature<NoneFeatureConfiguration> {

	public BreathingChasmFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();

		boolean alongX = random.nextBoolean();
		int width = 3 + random.nextInt(3);
		int depth = 12 + random.nextInt(9);
		int length = 10 + random.nextInt(11);
		int halfW = width / 2;

		for (int t = 0; t < length; t++) {
			for (int w = -halfW; w <= halfW; w++) {
				int x = alongX ? origin.getX() + t : origin.getX() + w;
				int z = alongX ? origin.getZ() + w : origin.getZ() + t;
				for (int dy = 0; dy > -depth; dy--) {
					BlockPos p = new BlockPos(x, origin.getY() + dy, z);
					boolean wall = Math.abs(w) == halfW;
					if (wall) {
						// 槽壁：幽匿（声纹放大）
						level.setBlock(p, random.nextInt(4) == 0
								? Blocks.SCULK.defaultBlockState()
								: Blocks.DEEPSLATE.defaultBlockState(), 2);
					} else {
						level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
					}
				}
				// 槽底
				level.setBlock(new BlockPos(x, origin.getY() - depth, z),
						Blocks.SCULK.defaultBlockState(), 2);
			}
			// 呼吸之门（§4.3「张开时可进入、闭合时压碎一切」）：每隔 5 格在入口横嵌一排，
			// 闭合相位全局同步——玩家要读准裂谷的呼吸节拍。
			if (t % 5 == 2) {
				for (int w = -halfW; w <= halfW; w++) {
					int x = alongX ? origin.getX() + t : origin.getX() + w;
					int z = alongX ? origin.getZ() + w : origin.getZ() + t;
					level.setBlock(new BlockPos(x, origin.getY(), z),
							realmikoto.extraenchantry.AbyssBlocks.BREATHING_GATE.defaultBlockState(), 2);
					// 槽底闸门：通往底部的"喉咙"
					if (random.nextInt(3) == 0) {
						level.setBlock(new BlockPos(x, origin.getY() - depth + 1, z),
								realmikoto.extraenchantry.AbyssBlocks.BREATHING_GATE.defaultBlockState(), 2);
					}
				}
			}
		}
		return true;
	}
}
