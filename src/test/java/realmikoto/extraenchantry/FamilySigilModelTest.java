package realmikoto.extraenchantry;

import org.junit.jupiter.api.Test;
import realmikoto.extraenchantry.testutil.TestPng;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 家族铭印模型分发完整性测试（1.4.0 补完：铭印专属贴图 + component select 分发）。
 *
 * 结构：单物品类型 {@code family_sigil} + {@code family_id} 组件——
 * {@code items/family_sigil.json} 用 26.2 原生 {@code minecraft:select} +
 * {@code property: minecraft:component} 按 {@code family_id} 字符串分发九型模型
 * （八族令牌 + 失效风化 fallback，纯数据零代码）。
 *
 * 不变量：
 * <ol>
 *   <li>cases 的 when 值与 {@link FamilyResonanceManager.Family} 枚举一一对应
 *       （少一族 → 该族铭印渲染成风化贴图；多一族 → 死条目）；</li>
 *   <li>每个 case 引用的模型 JSON 存在，且模型 layer0 指向的贴图存在且结构合法
 *       （缺贴图 → items 图集 stitch 失败 missing sprite）。</li>
 * </ol>
 */
class FamilySigilModelTest {

	private static final Path ASSETS = Path.of(
			"src", "main", "resources", "assets", "extra-enchantry");
	private static final Pattern WHEN_PATTERN = Pattern.compile("\"when\"\\s*:\\s*\"([a-z]+)\"");
	private static final Pattern MODEL_REF = Pattern.compile("\"model\"\\s*:\\s*\"([a-z0-9_/:.-]+)\"");

	/**
	 * 八族家族 path 清单。静态硬编码而非读 FamilyResonanceManager.Family：
	 * 该类的静态初始化依赖 Minecraft 运行时类（TagKey/注册表），单测环境加载即
	 * ExceptionInInitializerError。新增家族时两处同步（枚举 + 本清单），由本测试兜底提醒。
	 */
	private static final List<String> FAMILIES = List.of(
			"soul", "storm", "blade", "nature", "guard", "wind", "fire", "water");

	@Test
	void everyFamilyHasASigilModelCase() throws IOException {
		Path itemsFile = ASSETS.resolve(Path.of("items", "family_sigil.json"));
		assertTrue(Files.isRegularFile(itemsFile), "items/family_sigil.json 缺失");
		String json = Files.readString(itemsFile, StandardCharsets.UTF_8);
		assertTrue(Pattern.compile("\"property\"\\s*:\\s*\"minecraft:component\"").matcher(json).find(),
				"分发必须走 minecraft:component select（按 family_id 值分发）");
		assertTrue(Pattern.compile("\"component\"\\s*:\\s*\"extra-enchantry:family_id\"").matcher(json).find(),
				"分发键必须是 extra-enchantry:family_id 组件");

		List<String> whenValues = new ArrayList<>();
		Matcher matcher = WHEN_PATTERN.matcher(json);
		while (matcher.find()) {
			whenValues.add(matcher.group(1));
		}
		assertEquals(FAMILIES.stream().sorted().toList(), whenValues.stream().sorted().toList(),
				"select cases 的 when 值必须与八族清单一一对应（少一族该族铭印渲染成风化贴图）");
	}

	@Test
	void everyCaseModelAndTextureExists() throws IOException {
		Path itemsFile = ASSETS.resolve(Path.of("items", "family_sigil.json"));
		String json = Files.readString(itemsFile, StandardCharsets.UTF_8);
		Matcher models = MODEL_REF.matcher(json);
		int checked = 0;
		while (models.find()) {
			String ref = models.group(1);
			if (!ref.startsWith("extra-enchantry:")) {
				continue;
			}
			Path modelFile = ASSETS.resolve(
					Path.of("models", ref.substring("extra-enchantry:".length()) + ".json"));
			assertTrue(Files.isRegularFile(modelFile), "分发引用的模型缺失: " + ref);
			// 模型 layer0 → textures/item/<name>.png 存在且结构合法（Corrupt PNG 回归）
			String modelJson = Files.readString(modelFile, StandardCharsets.UTF_8);
			Matcher layer0 = Pattern.compile("\"layer0\"\\s*:\\s*\"extra-enchantry:item/([a-z0-9_]+)\"")
					.matcher(modelJson);
			assertTrue(layer0.find(), "模型未引用 layer0 贴图: " + ref);
			Path texture = ASSETS.resolve(
					Path.of("textures", "item", layer0.group(1) + ".png"));
			assertTrue(Files.isRegularFile(texture), "模型 layer0 贴图缺失: " + layer0.group(1));
			TestPng.read(texture);
			checked++;
		}
		assertTrue(checked >= 9, "应至少校验 8 族 + fallback 共 9 个模型，实际 " + checked);
	}
}
