package realmikoto.extraenchantry;

import org.junit.jupiter.api.Test;
import realmikoto.extraenchantry.testutil.TestPng;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 状态效果图标 ↔ GUI 图集注册一致性测试（回归 v1.3.2 "Using missing texture,
 * unable to load extra-enchantry:mob_effect/resonance"）。
 *
 * 事故根因（26.2 反编译确认）：{@code Hud#getMobEffectSprite} 把效果 ID 前缀
 * {@code mob_effect/} 后交给 {@code graphics.blitSprite}——图标 sprite 必须注册进
 * GUI 图集，mod 需自带 {@code assets/<ns>/atlases/gui.json} 的 directory source。
 * v1.2.0~v1.3.1 缺该注册，全部效果图标走 missing sprite fallback。
 *
 * 本测试固化三条不变量：
 * <ol>
 *   <li>语言文件声明的每个效果（effect.extra-enchantry.*）在 textures/mob_effect/
 *       恰有一张同名 18×18 图标（Hud 按 18×18 blit）；</li>
 *   <li>图标目录无多余孤儿文件（多余图标 = 拼写错误的死贴图）；</li>
 *   <li>atlases/gui.json 含 directory source：source="mob_effect"、prefix="mob_effect/"——
 *       由 directory source 语义，目录内每个文件的 sprite id = prefix + 文件名，
 *       与 Hud 请求的 mob_effect/&lt;效果id&gt; 精确对齐。</li>
 * </ol>
 */
class MobEffectIconAtlasTest {

	private static final Path ASSETS = Path.of(
			"src", "main", "resources", "assets", "extra-enchantry");

	/** 语言键 "effect.extra-enchantry.<name>" → 效果 ID（数据驱动的效果清单唯一可信源） */
	private static final Pattern EFFECT_KEY = Pattern.compile("\"effect\\.extra-enchantry\\.([a-z0-9_]+)\"\\s*:");

	@Test
	void everyDeclaredEffectHasExactlyOneIcon() throws IOException {
		Set<String> effects = declaredEffects();
		Set<String> icons = iconFileNames();
		assertEquals(effects, icons,
				"语言键声明的效果集与 textures/mob_effect 图标集必须一致："
						+ "缺图标的效果在状态栏显示 missing texture，孤儿图标则是拼错的死贴图");
		for (String effect : effects) {
			TestPng png = TestPng.read(ASSETS.resolve(
					Path.of("textures", "mob_effect", effect + ".png")));
			assertEquals(18, png.width, "效果图标必须 18×18（Hud blitSprite 固定 18×18）: " + effect);
			assertEquals(18, png.height, "效果图标必须 18×18（Hud blitSprite 固定 18×18）: " + effect);
		}
	}

	@Test
	void guiAtlasRegistersMobEffectDirectorySource() throws IOException {
		Path atlasFile = ASSETS.resolve(Path.of("atlases", "gui.json"));
		assertTrue(Files.isRegularFile(atlasFile),
				"assets/extra-enchantry/atlases/gui.json 缺失——26.2 效果图标经 GUI 图集渲染，"
						+ "无此注册则全部走 missing sprite（v1.3.2 修复对象）");
		String json = Files.readString(atlasFile, StandardCharsets.UTF_8);
		assertMatches(json, "\"type\"\\s*:\\s*\"minecraft:directory\"",
				"gui.json 需含 minecraft:directory 类型 source");
		assertMatches(json, "\"source\"\\s*:\\s*\"mob_effect\"",
				"gui.json 需声明 source=mob_effect（textures/mob_effect 目录）");
		assertMatches(json, "\"prefix\"\\s*:\\s*\"mob_effect/\"",
				"gui.json 需声明 prefix=mob_effect/ —— directory source 生成 sprite id = "
						+ "prefix+文件名，须与 Hud 请求的 mob_effect/<效果id> 对齐");
	}

	@Test
	void guiAtlasRegistersGuiSpritesDirectoryForGhostIcons() throws IOException {
		// v1.4.0 踩坑回归：幽灵图标 blitSprite 走 GUI 图集，sprite id = gui/ghost_<slot>
		// （原 Renderer 误用完整贴图路径 textures/gui/ghost_*.png——blitSprite 找不到 sprite）
		Path atlasFile = ASSETS.resolve(Path.of("atlases", "gui.json"));
		assertTrue(Files.isRegularFile(atlasFile), "atlases/gui.json 缺失");
		String json = Files.readString(atlasFile, StandardCharsets.UTF_8);
		assertMatches(json, "\"source\"\\s*:\\s*\"gui/sprites\"",
				"gui.json 需声明 source=gui/sprites（textures/gui/sprites 目录的 "
						+ "directory source，与原版 gui.json 同款；幽灵图标 blitSprite 依赖）");
		// 四槽位幽灵贴图必须存在（slot id → 贴图文件一一对应）
		for (String slot : new String[]{"earring", "necklace", "ring", "bracelet"}) {
			Path ghost = ASSETS.resolve(Path.of("textures", "gui", "sprites", "ghost_" + slot + ".png"));
			assertTrue(Files.isRegularFile(ghost),
					"幽灵贴图缺失: " + ghost + "（sprite id gui/ghost_" + slot + "）");
			TestPng.read(ghost); // 结构校验（Corrupt PNG 回归）
		}
	}

	// ============ 辅助 ============

	/** 语言文件中声明的效果 ID 集（非空断言防 lang 丢失时测试静默通过） */
	private static Set<String> declaredEffects() throws IOException {
		Path lang = ASSETS.resolve(Path.of("lang", "zh_cn.json"));
		assertTrue(Files.isRegularFile(lang), "语言文件缺失: " + lang.toAbsolutePath());
		Matcher matcher = EFFECT_KEY.matcher(Files.readString(lang, StandardCharsets.UTF_8));
		Set<String> effects = matcher.results()
				.map(m -> m.group(1))
				.collect(Collectors.toUnmodifiableSet());
		assertTrue(!effects.isEmpty(), "语言文件中未解析到任何 effect.extra-enchantry.* 键");
		return effects;
	}

	/** textures/mob_effect 目录下的图标文件名集（去 .png） */
	private static Set<String> iconFileNames() throws IOException {
		Path dir = ASSETS.resolve(Path.of("textures", "mob_effect"));
		assertTrue(Files.isDirectory(dir), "效果图标目录缺失: " + dir.toAbsolutePath());
		try (Stream<Path> list = Files.list(dir)) {
			return list.filter(p -> p.getFileName().toString().endsWith(".png"))
					.map(p -> p.getFileName().toString().replace(".png", ""))
					.collect(Collectors.toUnmodifiableSet());
		}
	}

	private static void assertMatches(String json, String regex, String message) {
		assertTrue(Pattern.compile(regex).matcher(json).find(), message + "（当前内容: " + json + "）");
	}
}
