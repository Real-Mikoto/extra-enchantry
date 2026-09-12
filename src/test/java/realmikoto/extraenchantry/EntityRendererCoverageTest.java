package realmikoto.extraenchantry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 实体渲染器覆盖测试（回归「自定义实体无渲染器 → 渲染帧 NPE 崩溃」）。
 *
 * 背景：{@code EntityRenderers} 未注册的自定义 EntityType 会让
 * {@code EntityRenderDispatcher.getRenderer()} 返回 null，
 * 进而在 {@code LevelExtractor.isEntityVisible} 抛 NullPointerException，
 * 玩家一进入含该实体的世界即崩溃。
 *
 * 本测试静态扫描：{@code AbyssEntities} 中声明的每个自定义 EntityType
 * 常量，都必须在客户端入口的渲染器注册处出现。
 */
class EntityRendererCoverageTest {

	private static Path root() {
		return Path.of("src");
	}

	@Test
	void everyAbyssEntityTypeHasClientRenderer() throws IOException {
		String entities = Files.readString(
				root().resolve("main/java/realmikoto/extraenchantry/AbyssEntities.java"),
				StandardCharsets.UTF_8);
		String client = Files.readString(
				root().resolve("client/java/realmikoto/extraenchantry/client/ExtraEnchantryClient.java"),
				StandardCharsets.UTF_8);

		// 收集 AbyssEntities 中声明的 EntityType 常量名
		List<String> declared = new ArrayList<>();
		Matcher m = Pattern.compile("public static final EntityType<[^>]+>\\s+(\\w+)\\s*=")
				.matcher(entities);
		while (m.find()) {
			declared.add(m.group(1));
		}
		assertTrue(declared.size() >= 7,
				"应至少声明 7 个幽渊实体，实际: " + declared);

		// 每个常量都必须在客户端注册渲染器（形如 AbyssEntities.XXX）
		List<String> missing = new ArrayList<>();
		for (String name : declared) {
			if (!client.contains("AbyssEntities." + name)) {
				missing.add(name);
			}
		}
		assertTrue(missing.isEmpty(),
				"以下自定义实体缺少客户端渲染器注册（会导致渲染帧 NPE 崩溃）: " + missing);
	}
}
