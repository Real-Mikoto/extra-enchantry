package realmikoto.extraenchantry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 幽渊维度数据完整性测试（1.8.0「门」）。
 *
 * 校验维度数据包 JSON：dimension_type / dimension / biome / noise_settings /
 * density_function 的关键字段与相互引用（命名空间、generator 链、density 引用）。
 * 维度数据错误只在运行时爆（"Unknown dimension" / 生成器崩溃），故提前在此把关。
 */
class AbyssDimensionTest {

	private static InputStream resource(String path) {
		return AbyssDimensionTest.class.getClassLoader().getResourceAsStream(path);
	}

	private static JsonObject json(String resourcePath) throws IOException {
		try (InputStream in = resource(resourcePath)) {
			assertNotNull(in, "缺少资源: " + resourcePath);
			return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
					.getAsJsonObject();
		}
	}

	@Test
	void dimensionTypeAbyssIsLightlessFixedTime() throws IOException {
		JsonObject type = json("data/extra-enchantry/dimension_type/abyss.json");
		assertEquals("none", type.get("skybox").getAsString(), "幽渊天空盒必须为 none（无光）");
		assertEquals(0.0, type.get("ambient_light").getAsDouble(), 1e-6, "幽渊环境光必须为 0");
		assertTrue(type.get("has_skylight").getAsBoolean() == false, "幽渊不得有天光");
		assertTrue(type.get("has_fixed_time").getAsBoolean(), "幽渊为固定时间");
	}

	@Test
	void dimensionReferencesOwnTypeAndBiome() throws IOException {
		JsonObject dimension = json("data/extra-enchantry/dimension/abyss.json");
		assertEquals("extra-enchantry:abyss", dimension.get("type").getAsString(),
				"dimension 必须引用本模 dimension_type:abyss");
		JsonObject generator = dimension.getAsJsonObject("generator");
		assertEquals("minecraft:noise", generator.get("type").getAsString());
		JsonObject biomeSource = generator.getAsJsonObject("biome_source");
		assertEquals("extra-enchantry:abyss", biomeSource.get("biome").getAsString(),
				"biome_source 必须指向本模 biome:abyss");
		assertEquals("extra-enchantry:abyss", generator.get("settings").getAsString(),
				"generator 必须引用本模 noise_settings:abyss");
	}

	@Test
	void noiseSettingsFinalDensityReferencesAbyssFunction() throws IOException {
		JsonObject settings = json("data/extra-enchantry/worldgen/noise_settings/abyss.json");
		JsonObject router = settings.getAsJsonObject("noise_router");
		String ref = flatten(router.getAsJsonObject("final_density"));
		assertTrue(ref.contains("extra-enchantry:abyss/base_terrain"),
				"final_density 必须引用 abyss/base_terrain density function");
		assertEquals(-64, settings.get("sea_level").getAsInt(), "液幽匿海平面（声纹湖）设为 -64");
	}

	@Test
	void baseTerrainFunctionStructure() throws IOException {
		JsonObject fn = json("data/extra-enchantry/worldgen/density_function/abyss/base_terrain.json");
		assertEquals("minecraft:add", fn.get("type").getAsString(),
				"base_terrain 顶层应为 add（底面梯度 + 顶面梯度 + 3D 噪声）");
		String flat = flatten(fn);
		assertTrue(flat.contains("minecraft:old_blended_noise"), "应使用 old_blended_noise 做大地形");
	}

	/** 递归展平 JSON 便于子串检查 */
	private static String flatten(com.google.gson.JsonElement element) {
		if (element.isJsonPrimitive()) {
			return element.getAsString();
		}
		StringBuilder sb = new StringBuilder();
		if (element.isJsonObject()) {
			for (var entry : element.getAsJsonObject().entrySet()) {
				sb.append(flatten(entry.getValue()));
			}
		} else if (element.isJsonArray()) {
			for (var child : element.getAsJsonArray()) {
				sb.append(flatten(child));
			}
		}
		return sb.toString();
	}
}
