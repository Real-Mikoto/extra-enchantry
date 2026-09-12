package realmikoto.extraenchantry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 幽渊 biome JSON 合法性测试（回归 1.8.x "No key water_color in environment_attribute" 崩溃）。
 *
 * 26.2 的 biome 拆成 attributes（命名空间键）+ effects（旧字段）两处：
 * - water_color 必须在 {@code effects}，绝不能写成 {@code minecraft:visual/water_color}
 *   （该 attribute 键不存在 → 注册表加载失败 → 进世界/删世界崩溃）；
 * - mood_sound 必须挂在 {@code minecraft:audio/ambient_sounds.mood}。
 */
class AbyssBiomeTest {

	private static JsonObject biome() throws IOException {
		try (InputStream in = AbyssBiomeTest.class.getClassLoader()
				.getResourceAsStream("data/extra-enchantry/worldgen/biome/abyss.json")) {
			assertNotNull(in, "缺少 biome JSON");
			return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
					.getAsJsonObject();
		}
	}

	@Test
	void waterColorLivesInEffectsNotAttributes() throws IOException {
		JsonObject root = biome();
		assertTrue(root.getAsJsonObject("effects").has("water_color"),
				"water_color 必须在 effects 中");
		JsonObject attributes = root.getAsJsonObject("attributes");
		assertFalse(attributes.has("minecraft:visual/water_color"),
				"attributes 不得包含不存在的 minecraft:visual/water_color 键");
	}

	@Test
	void moodSoundLivesUnderAmbientSoundsAttribute() throws IOException {
		JsonObject attributes = biome().getAsJsonObject("attributes");
		assertFalse(attributes.getAsJsonObject("effects") != null, "attributes 内不得嵌套 effects");
		assertFalse(attributes.has("mood_sound"), "mood_sound 不得直接挂在 attributes 根");
		assertTrue(attributes.getAsJsonObject("minecraft:audio/ambient_sounds")
				.getAsJsonObject("mood").has("sound"), "mood 必须在 audio/ambient_sounds 下且带 sound");
	}


	@Test
	void noiseSettingsKeySetMatchesVanillaEnd() throws IOException {
		JsonObject ours;
		try (InputStream in = AbyssBiomeTest.class.getClassLoader()
				.getResourceAsStream("data/extra-enchantry/worldgen/noise_settings/abyss.json")) {
			assertNotNull(in, "缺少 noise_settings JSON");
			ours = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
					.getAsJsonObject();
		}
		// 回归："No key legacy_random_source" → 点击单人游戏卡死
		assertTrue(ours.has("legacy_random_source"), "noise_settings 必须含 legacy_random_source（26.2 必填）");
		// 键集合与原版 end 一致（防再次漏键）
		java.util.Set<String> required = java.util.Set.of(
				"aquifers_enabled", "default_block", "default_fluid", "disable_mob_generation",
				"legacy_random_source", "noise", "noise_router", "ore_veins_enabled",
				"sea_level", "spawn_target", "surface_rule");
		for (String k : required) {
			assertTrue(ours.has(k), "noise_settings 缺少必填键: " + k);
		}
		assertFalse(ours.entrySet().stream().anyMatch(e -> !required.contains(e.getKey())),
				"noise_settings 含未知键: " + ours.keySet());
	}
}
