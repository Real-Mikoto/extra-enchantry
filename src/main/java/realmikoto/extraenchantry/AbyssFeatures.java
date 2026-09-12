package realmikoto.extraenchantry;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import realmikoto.extraenchantry.worldgen.BreathingChasmFeature;
import realmikoto.extraenchantry.worldgen.InvertedCityFeature;

/**
 * 幽渊自定义地形 feature 注册（v1.8.0.2，设计稿 §4.3）。
 *
 * - inverted_city 倒悬城市：悬挂在天顶的初民遗迹；
 * - breathing_chasm 呼吸裂谷：周期性张合的深谷。
 */
public final class AbyssFeatures {

	public static final Feature<NoneFeatureConfiguration> INVERTED_CITY =
			new InvertedCityFeature(NoneFeatureConfiguration.CODEC);
	public static final Feature<NoneFeatureConfiguration> BREATHING_CHASM =
			new BreathingChasmFeature(NoneFeatureConfiguration.CODEC);
	public static final Feature<NoneFeatureConfiguration> AETHER_PLATFORM =
			new realmikoto.extraenchantry.worldgen.AetherPlatformFeature(NoneFeatureConfiguration.CODEC);

	private AbyssFeatures() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.FEATURE,
				ExtraEnchantry.id("inverted_city"), INVERTED_CITY);
		Registry.register(BuiltInRegistries.FEATURE,
				ExtraEnchantry.id("breathing_chasm"), BREATHING_CHASM);
		Registry.register(BuiltInRegistries.FEATURE,
				ExtraEnchantry.id("aether_platform"), AETHER_PLATFORM);
	}
}
