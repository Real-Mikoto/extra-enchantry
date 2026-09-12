package realmikoto.extraenchantry.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 倒悬城市（v1.8.0.2 补齐，设计稿 §4.3）：被"翻过来"挂在顶部的初民遗迹。
 *
 * 生成结构（自天顶向下悬挂）：
 * - 顶部平台（深板岩砖 + 幽匿）；
 * - 向下垂挂的支柱（3–5 根，长度随机）；
 * - 平台底面附着幽匿脉络 + 骨块装饰；
 * - 玩家需在"上下颠倒"的遗迹中穿行（配合渊心倒悬重力区）。
 */
public class InvertedCityFeature extends Feature<NoneFeatureConfiguration> {

	public InvertedCityFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();

		// 平台尺寸 7–11 见方
		int size = 7 + random.nextInt(5);
		int half = size / 2;
		BlockState brick = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
		BlockState sculk = Blocks.SCULK.defaultBlockState();
		BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
		BlockState vein = Blocks.SCULK_VEIN.defaultBlockState();

		// 1) 顶部平台（厚度 2，倒悬的"地板"）
		for (int dx = -half; dx <= half; dx++) {
			for (int dz = -half; dz <= half; dz++) {
				for (int dy = 0; dy < 2; dy++) {
					BlockPos p = origin.offset(dx, dy, dz);
					if (Math.abs(dx) == half || Math.abs(dz) == half) {
						level.setBlock(p, brick, 2);
					} else {
						level.setBlock(p, random.nextInt(6) == 0 ? sculk : brick, 2);
					}
				}
			}
		}
		// 2) 向下垂挂的支柱
		int pillars = 3 + random.nextInt(3);
		for (int i = 0; i < pillars; i++) {
			int px = random.nextInt(size) - half;
			int pz = random.nextInt(size) - half;
			int len = 3 + random.nextInt(6);
			for (int dy = -1; dy >= -len; dy--) {
				level.setBlock(origin.offset(px, dy, pz), brick, 2);
			}
			// 柱端骨块装饰
			level.setBlock(origin.offset(px, -len - 1, pz), bone, 2);
		}
		// 3) 平台底面幽匿脉络 + 零星骨块（"倒悬城市"的废墟质感）
		for (int dx = -half; dx <= half; dx++) {
			for (int dz = -half; dz <= half; dz++) {
				BlockPos under = origin.offset(dx, -1, dz);
				if (level.getBlockState(under).isAir()) {
					int roll = random.nextInt(10);
					if (roll < 4) {
						level.setBlock(under, vein, 2);
					} else if (roll == 9) {
						level.setBlock(under, bone, 2);
					}
				}
			}
		}
		return true;
	}
}
