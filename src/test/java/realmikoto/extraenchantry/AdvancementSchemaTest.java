package realmikoto.extraenchantry;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 进度 JSON 结构纪律（1.7.3 回归锁）：
 * criteria 键集合必须与 requirements 覆盖的键集合完全一致——
 * 不一致时 26.2 在数据包加载期整条进度解析失败（创建世界时刷 ERROR，
 * "Missing: [...] Unknown: [...]"），且该进度永不可完成。
 * 历史缺陷：lineage 13 条与 hidden_challenges 6 条曾因改 criteria 未同步 requirements 触发。
 */
class AdvancementSchemaTest {

	private static final Gson GSON = new Gson();

	private static final Path ROOT = Path.of("src", "main", "resources", "data",
			"extra-enchantry", "advancement");

	@Test
	void requirementsMatchCriteriaKeys() throws IOException {
		assertTrue(Files.isDirectory(ROOT), "进度目录不存在: " + ROOT);
		try (Stream<Path> files = Files.walk(ROOT)) {
			List<Path> jsons = files.filter(p -> p.toString().endsWith(".json")).toList();
			assertTrue(jsons.size() >= 80, "进度 JSON 数量异常: " + jsons.size());
			for (Path file : jsons) {
				JsonObject json = GSON.fromJson(Files.readString(file), JsonObject.class);
				Set<String> criteriaKeys = new HashSet<>();
				json.getAsJsonObject("criteria").keySet().forEach(criteriaKeys::add);
				Set<String> requirementKeys = new HashSet<>();
				json.getAsJsonArray("requirements").forEach(element -> {
					for (var item : element.getAsJsonArray()) {
						requirementKeys.add(item.getAsString());
					}
				});
				assertEquals(criteriaKeys, requirementKeys,
						file.getFileName() + " criteria/requirements 不一致");
			}
		}
	}
}
