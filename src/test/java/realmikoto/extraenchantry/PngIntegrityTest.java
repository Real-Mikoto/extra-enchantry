package realmikoto.extraenchantry;

import org.junit.jupiter.api.Test;
import realmikoto.extraenchantry.testutil.TestPng;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 纹理 PNG 结构完整性测试（回归 v1.3.2 "Using missing texture / Corrupt PNG" bug）。
 *
 * 事故：resonance.png 由脚本生成时每扫描行多写 1 字节（IDAT 解压 1332 ≠ 期望 1314），
 * System.Drawing 能读但 Minecraft NativeImage(STB) 判 Corrupt PNG，
 * atlas stitch 阶段 IOException → sprite 永久 missing。
 *
 * 本测试对 {@code assets/extra-enchantry/textures/} 下全部 PNG 做 STB 同等严格度校验：
 * 签名 / 截断 / chunk CRC / IHDR 唯一 / IDAT 可解压且长度精确对账。
 */
class PngIntegrityTest {

	/** 纹理根目录（gradle test 工作目录 = 项目根） */
	private static final Path TEXTURES = Path.of(
			"src", "main", "resources", "assets", "extra-enchantry", "textures");

	private static List<Path> allTextures() {
		assertTrue(Files.isDirectory(TEXTURES), "纹理目录不存在: " + TEXTURES.toAbsolutePath());
		try (Stream<Path> walk = Files.walk(TEXTURES)) {
			List<Path> pngs = walk
					.filter(p -> p.getFileName().toString().endsWith(".png"))
					.sorted()
					.toList();
			assertFalse(pngs.isEmpty(), "纹理目录中未发现任何 PNG——目录被移动或资源丢失");
			return pngs;
		} catch (IOException e) {
			throw new IllegalStateException("遍历纹理目录失败: " + TEXTURES, e);
		}
	}

	@Test
	void allPngsAreStructurallyValid() {
		for (Path png : allTextures()) {
			// read 内部完成签名 / 截断 / CRC / IHDR / zlib 校验，失败抛 IllegalStateException（消息含路径）
			TestPng image = TestPng.read(png);
			assertAll(png.toString(),
					() -> assertEquals(8, image.bitDepth, "本项目纹理统一 8 位深（脚本生成约定）"),
					() -> assertEquals(6, image.colorType, "本项目纹理统一 RGBA（colortype=6，脚本生成约定）"),
					() -> assertTrue(image.bytesPerPixel() > 0, "颜色类型非法: " + image.colorType),
					() -> assertEquals(image.expectedRawBytes(), image.decompressedRawBytes,
							"IDAT 解压长度应为 高×(1+宽×4)——上代生成脚本每行多写 1 字节即在此失败"
									+ "（v1.3.2 Corrupt PNG 事故，STB 拒载而 System.Drawing 能读）"));
		}
	}

	@Test
	void knownTexturesAreCovered() {
		// 覆盖面冒烟：三张状态效果图标 + 四张 1.3.1 lore 物品贴图必须存在（被校验）
		List<Path> pngs = allTextures();
		assertTrue(pngs.stream().anyMatch(p -> p.endsWith(Path.of("mob_effect", "resonance.png"))),
				"共鸣效果图标缺失（v1.3.2 修复对象）");
		assertTrue(pngs.stream().anyMatch(p -> p.endsWith(Path.of("mob_effect", "afterglow.png"))),
				"余辉效果图标缺失");
		assertTrue(pngs.stream().anyMatch(p -> p.endsWith(Path.of("mob_effect", "emberfall.png"))),
				"余烬效果图标缺失");
		assertTrue(pngs.stream().anyMatch(p -> p.endsWith(Path.of("item", "welcome_letter.png"))),
				"来者手札贴图缺失（1.3.1）");
		assertTrue(pngs.stream().anyMatch(p -> p.endsWith(Path.of("item", "chronicle_scroll.png"))),
				"编年史卷轴贴图缺失（1.3.1）");
	}
}
