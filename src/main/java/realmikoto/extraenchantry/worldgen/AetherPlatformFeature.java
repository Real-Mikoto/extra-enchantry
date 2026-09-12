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
 * 无相之空（v1.8.0 §4.2/§6.7）：渊心层的终局平台——「无相」的实体化现场。
 *
 * 生成：9×9 悬空平台（深板岩砖 + 磨制深板岩纹样），中央一枚「空座」
 * （切块深板岩 + 幽紫微光），四周稀布无相之尘矿。渊心守望者在此被唤醒
 * （触发逻辑见 HeartWardenEntity.tryAwaken，由 AbyssSpawning 渊心分支调用）。
 */
public class AetherPlatformFeature extends Feature<NoneFeatureConfiguration> {

	public AetherPlatformFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();

		// 9×9 悬空平台（Y = origin）
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				BlockPos p = origin.offset(dx, 0, dz);
				boolean rim = Math.abs(dx) == 4 || Math.abs(dz) == 4;
				level.setBlock(p, rim
						? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
						: Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
				// 平台下方短柱支撑（垂坠感）
				if ((dx + dz) % 2 == 0 && random.nextInt(3) == 0) {
					for (int dy = 1; dy <= 2 + random.nextInt(3); dy++) {
						level.setBlock(p.below(dy), Blocks.DEEPSLATE.defaultBlockState(), 2);
					}
				}
			}
		}
		// 中央「空座」：切块深板岩基座 + 微光
		level.setBlock(origin, Blocks.CHISELED_DEEPSLATE.defaultBlockState(), 2);
		level.setBlock(origin.below(), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
		// 四角无相之尘矿（渊心资源集中点）
		for (int[] c : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
			if (random.nextInt(2) == 0) {
				level.setBlock(origin.offset(c[0], 0, c[1]),
						realmikoto.extraenchantry.AbyssBlocks.AETHER_DUST_ORE.defaultBlockState(), 2);
			}
		}
		// 「无相」的低语粒子在方块层不可用——运行时由 HeartWardenEntity/Boss 触发器补充
		return true;
	}
}
