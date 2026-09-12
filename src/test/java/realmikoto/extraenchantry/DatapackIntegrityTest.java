package realmikoto.extraenchantry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据包交叉引用完整性测试（回归 v1.8.x「数据包错误 / 无法启动世界」）。
 *
 * 覆盖两类真实事故：
 * 1. biome 的 features 引用了不存在的原版 placed_feature（如臆造的 {@code ore_iron_lower}）
 *    → 世界创建时注册表加载失败；
 * 2. density_function 引用不存在的 noise 参数 ID（{@code abyss/pillar} vs {@code abyss_pillar}）
 *    → 同上。
 *
 * 另校验：placed→configured 引用、density 引用、advancement parent、recipe 结构。
 */
class DatapackIntegrityTest {

	private static Path dataRoot() {
		return Path.of("src/main/resources/data/extra-enchantry");
	}

	/** 原版 data 路径集合（来自 minecraft-common.jar 的 classpath 资源清单） */
	private static Set<String> vanillaDataPaths() throws IOException {
		// loom 会把原版 jar 放在 gradle 缓存；此处通过 classpath 上是否存在资源判断
		return Set.of();   // 由 existsVanilla() 逐路径判定替代
	}

	private static boolean existsVanilla(String path) {
		return DatapackIntegrityTest.class.getClassLoader().getResource(path) != null;
	}

	private static JsonObject read(Path p) throws IOException {
		return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
	}

	private static List<Path> jsons(String sub) throws IOException {
		Path root = dataRoot().resolve(sub);
		if (!Files.isDirectory(root)) {
			return List.of();
		}
		try (Stream<Path> s = Files.walk(root)) {
			return s.filter(x -> x.toString().endsWith(".json")).toList();
		}
	}

	@Test
	void biomeFeaturesAllResolvable() throws IOException {
		JsonObject biome = read(dataRoot().resolve("worldgen/biome/abyss.json"));
		JsonArray steps = biome.getAsJsonArray("features");
		List<String> missing = new ArrayList<>();
		for (JsonElement step : steps) {
			for (JsonElement ref : step.getAsJsonArray()) {
				String id = ref.getAsString();
				String[] parts = id.split(":", 2);
				String ns = parts[0];
				String name = parts[1];
				if (ns.equals("extra-enchantry")) {
					if (!Files.exists(dataRoot().resolve("worldgen/placed_feature/" + name + ".json"))) {
						missing.add(id);
					}
				} else if (!existsVanilla("data/minecraft/worldgen/placed_feature/" + name + ".json")) {
					missing.add(id);
				}
			}
		}
		assertTrue(missing.isEmpty(), "biome 引用了不存在的 placed_feature: " + missing);
	}

	@Test
	void placedFeaturesResolveConfigured() throws IOException {
		List<String> missing = new ArrayList<>();
		for (Path p : jsons("worldgen/placed_feature")) {
			String feature = read(p).get("feature").getAsString();
			String[] parts = feature.split(":", 2);
			if (parts[0].equals("extra-enchantry")) {
				if (!Files.exists(dataRoot().resolve("worldgen/configured_feature/" + parts[1] + ".json"))) {
					missing.add(p.getFileName() + " -> " + feature);
				}
			} else if (!existsVanilla("data/minecraft/worldgen/configured_feature/" + parts[1] + ".json")) {
				missing.add(p.getFileName() + " -> " + feature);
			}
		}
		assertTrue(missing.isEmpty(), "placed_feature 引用缺失: " + missing);
	}

	@Test
	void densityFunctionsAndNoiseResolve() throws IOException {
		List<String> missing = new ArrayList<>();
		for (Path p : jsons("worldgen/density_function")) {
			JsonObject root = read(p);
			walk(root, node -> {
				if (node.has("type") && node.get("type").getAsString().equals("minecraft:noise")
						&& node.has("noise")) {
					String nz = node.get("noise").getAsString();
					String[] parts = nz.split(":", 2);
					if (parts[0].equals("extra-enchantry")
							&& !Files.exists(dataRoot().resolve("worldgen/noise/" + parts[1] + ".json"))) {
						missing.add("noise " + nz);
					}
				}
			});
		}
		assertTrue(missing.isEmpty(), "noise 引用缺失: " + missing);
	}

	@Test
	void advancementParentsAndIconsExist() throws IOException {
		List<String> missing = new ArrayList<>();
		for (Path p : jsons("advancement")) {
			JsonObject adv = read(p);
			if (adv.has("parent")) {
				String parent = adv.get("parent").getAsString();
				String[] parts = parent.split(":", 2);
				if (parts[0].equals("extra-enchantry")
						&& !Files.exists(dataRoot().resolve("advancement/" + parts[1] + ".json"))) {
					missing.add(p.getFileName() + " parent -> " + parent);
				}
			}
			JsonObject display = adv.has("display") ? adv.getAsJsonObject("display") : null;
			if (display != null && display.has("icon")) {
				String icon = display.getAsJsonObject("icon").get("id").getAsString();
				String[] parts = icon.split(":", 2);
				if (parts[0].equals("extra-enchantry")) {
					// 物品模型（items/<id>.json）必须存在，否则进度图标显示 missing texture
					if (!Files.exists(Path.of("src/main/resources/assets/extra-enchantry/items/"
							+ parts[1] + ".json"))) {
						missing.add(p.getFileName() + " icon 无模型 -> " + icon);
					}
				}
			}
		}
		assertTrue(missing.isEmpty(), "advancement 引用缺失: " + missing);
	}

	@Test
	void shapelessRecipesUseStringIngredients() throws IOException {
		List<String> bad = new ArrayList<>();
		for (Path p : jsons("recipe")) {
			JsonObject r = read(p);
			String type = r.get("type").getAsString();
			if (type.contains("shapeless")) {
				JsonArray ing = r.getAsJsonArray("ingredients");
				if (ing == null || ing.isEmpty()) {
					bad.add(p.getFileName() + " ingredients 为空");
					continue;
				}
				for (JsonElement e : ing) {
					if (!e.isJsonPrimitive()) {
						bad.add(p.getFileName() + " ingredients 元素非字符串: " + e);
					}
				}
			}
			if (type.contains("shaped")) {
				assertNotNull(r.get("pattern"), p.getFileName() + " 缺 pattern");
				assertNotNull(r.get("key"), p.getFileName() + " 缺 key");
			}
		}
		assertTrue(bad.isEmpty(), "配方结构非法: " + bad);
	}

	private interface NodeVisitor {
		void visit(JsonObject node);
	}

	private static void walk(JsonElement el, NodeVisitor v) {
		if (el.isJsonObject()) {
			JsonObject o = el.getAsJsonObject();
			v.visit(o);
			for (var e : o.entrySet()) {
				walk(e.getValue(), v);
			}
		} else if (el.isJsonArray()) {
			for (JsonElement e : el.getAsJsonArray()) {
				walk(e, v);
			}
		}
	}

	/** 配方引用的物品 ID 必须真实存在（本模有 assets/items/<id>.json，或原版有对应资源） */
	@Test
	void recipeItemIdsResolvable() throws IOException {
		Path assetsItems = Path.of("src/main/resources/assets/extra-enchantry/items");
		List<String> missing = new ArrayList<>();
		for (Path p : jsons("recipe")) {
			JsonObject r = read(p);
			// 收集：ingredients / key / result.id
			List<String> ids = new ArrayList<>();
			if (r.has("ingredients")) {
				for (JsonElement e : r.getAsJsonArray("ingredients")) {
					if (e.isJsonPrimitive()) {
						ids.add(e.getAsString());
					}
				}
			}
			if (r.has("key")) {
				for (var e : r.getAsJsonObject("key").entrySet()) {
					if (e.getValue().isJsonPrimitive()) {
						ids.add(e.getValue().getAsString());
					}
				}
			}
			if (r.has("result") && r.getAsJsonObject("result").has("id")) {
				ids.add(r.getAsJsonObject("result").get("id").getAsString());
			}
			for (String id : ids) {
				if (id.startsWith("#")) {
					continue;   // 标签引用跳过（另有标签校验）
				}
				String[] parts = id.split(":", 2);
				if (parts.length < 2) {
					missing.add(p.getFileName() + " -> 非法 id: " + id);
					continue;
				}
				if (parts[0].equals("extra-enchantry")) {
					if (!Files.exists(assetsItems.resolve(parts[1] + ".json"))) {
						missing.add(p.getFileName() + " -> 本模物品不存在: " + id);
					}
				} else if (!existsVanilla("assets/minecraft/items/" + parts[1] + ".json")) {
					missing.add(p.getFileName() + " -> 原版物品不存在: " + id);
				}
			}
		}
		assertTrue(missing.isEmpty(), "配方引用了不存在的物品: " + missing);
	}
}
