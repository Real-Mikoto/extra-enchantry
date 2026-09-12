package realmikoto.extraenchantry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 传送门框架尺寸校验测试（回归「门框无法完整」故障）。
 *
 * 背景：旧实现从"门洞内空格"出发沿负方向找框架方块，但相邻格仍是空格 →
 * 扫描循环立即退出 → 宽度恒为 1 → 校验永远失败（玩家搭得再标准也点不着）。
 *
 * 本测试锁定修正后的尺寸规则（与下界传送门一致：内洞宽 ≥2、高 ≥3）。
 */
class AbyssGateFrameTest {

	@Test
	void minimumVanillaLikeSizeAccepted() {
		// 内洞 2 宽 × 3 高（下界传送门最小尺寸）
		assertTrue(AbyssGateRules.isValidDimensions(2, 3));
	}

	@Test
	void largerSizesAccepted() {
		assertTrue(AbyssGateRules.isValidDimensions(3, 4));
		assertTrue(AbyssGateRules.isValidDimensions(4, 3));
		assertTrue(AbyssGateRules.isValidDimensions(AbyssGateRules.MAX_FRAME, AbyssGateRules.MAX_FRAME));
	}

	@Test
	void tooNarrowRejected() {
		// 1 宽 = 只有一列，无法通行
		assertFalse(AbyssGateRules.isValidDimensions(1, 3));
		assertFalse(AbyssGateRules.isValidDimensions(0, 3));
		assertFalse(AbyssGateRules.isValidDimensions(-1, 3));
	}

	@Test
	void tooShortRejected() {
		// 高 1–2 = 玩家无法站立通过
		assertFalse(AbyssGateRules.isValidDimensions(2, 1));
		assertFalse(AbyssGateRules.isValidDimensions(2, 2));
	}

	@Test
	void oversizedRejected() {
		assertFalse(AbyssGateRules.isValidDimensions(AbyssGateRules.MAX_FRAME + 1, 3));
		assertFalse(AbyssGateRules.isValidDimensions(2, AbyssGateRules.MAX_FRAME + 1));
	}

	// ============ 扫描算法（合成网格）============

	/**
	 * 用字符网格构造探测接口：'#' = 框架（非空气），'.' = 空气。
	 * 点火点固定在网格中心的空气格。
	 */
	private static AbyssGateRules.BlockProbe grid(String... rows) {
		// rows[0] 是最上方一行；列方向为 x，行方向为 y（向下为负）
		int h = rows.length;
		int w = rows[0].length();
		return (dx, dy) -> {
			// 原点取网格中"空气区域"的中心，这里简化为网格正中心
			int cx = w / 2;
			int cy = h / 2;
			int x = cx + dx;
			int y = cy - dy;   // dy 向上为正 → 行号减小
			if (x < 0 || x >= w || y < 0 || y >= h) {
				return true;   // 出界视为开阔空气（无框架）
			}
			return rows[y].charAt(x) == '.';
		};
	}

	@Test
	void measureHoleReadsMinimumFrame() {
		// 2 宽 × 3 高内洞（左壁、右壁各 1 列；顶、底各 1 行）
		AbyssGateRules.BlockProbe probe = grid(
				"####",
				"#..#",
				"#..#",
				"#..#",
				"####");
		int[] size = AbyssGateRules.measureHole(probe);
		assertNotNull(size, "标准 2×3 门洞必须被识别");
		assertEquals(2, size[0], "净宽应为 2");
		assertEquals(3, size[1], "净高应为 3");
		assertTrue(AbyssGateRules.isValidDimensions(size[0], size[1]));
	}

	@Test
	void measureHoleRejectsSolidIgnitionPoint() {
		AbyssGateRules.BlockProbe probe = grid(
				"###",
				"###",
				"###");
		assertNull(AbyssGateRules.measureHole(probe), "点火点在实心方块上必须拒绝");
	}

	@Test
	void measureHoleRejectsOpenSky() {
		// 没有框架：四个方向都扫不到实心 → null
		AbyssGateRules.BlockProbe probe = grid(
				".....",
				".....",
				".....",
				".....",
				".....");
		assertNull(AbyssGateRules.measureHole(probe), "无框架时必须拒绝");
	}

	@Test
	void measureHoleReadsLargerFrame() {
		// 4 宽 × 5 高内洞
		AbyssGateRules.BlockProbe probe = grid(
				"######",
				"#....#",
				"#....#",
				"#....#",
				"#....#",
				"#....#",
				"######");
		int[] size = AbyssGateRules.measureHole(probe);
		assertNotNull(size);
		assertEquals(4, size[0]);
		assertEquals(5, size[1]);
	}
}
