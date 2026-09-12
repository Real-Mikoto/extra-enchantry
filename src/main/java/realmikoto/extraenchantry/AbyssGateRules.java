package realmikoto.extraenchantry;

/**
 * 传送门框架规则（纯逻辑，零 Minecraft 依赖——便于单元测试）。
 *
 * 门洞尺寸要求（与下界传送门一致）：
 * - 内洞净宽 ≥ 2（至少两列才能通行）；
 * - 内洞净高 ≥ 3（玩家身高 1.8，需 3 格才能站立穿过）；
 * - 上限 {@link #MAX_FRAME}（避免扫描成本失控）。
 */
public final class AbyssGateRules {

	/** 扫描半径上限（门洞净尺寸与框架厚度之和的上限） */
	public static final int MAX_FRAME = 6;

	private AbyssGateRules() {
	}

	/** 门洞净尺寸是否合法 */
	public static boolean isValidDimensions(int interiorWidth, int interiorHeight) {
		return interiorWidth >= 2 && interiorHeight >= 3
				&& interiorWidth <= MAX_FRAME && interiorHeight <= MAX_FRAME;
	}

	/**
	 * 方块探测接口：以点火点为原点，报告相对坐标处是否为空气。
	 * 由 {@code AbyssGate} 用真实 Level 实现；单测用合成网格实现。
	 */
	public interface BlockProbe {
		boolean isAir(int dx, int dy);
	}

	/**
	 * 测量门洞净尺寸（核心扫描算法，可单测）。
	 *
	 * 从点火点（必须是空气）沿四个方向扫描到第一个**非空气**方块：
	 * - 左右距离之和减 1 = 净宽；
	 * - 上下距离之和减 1 = 净高。
	 *
	 * @return {width, height}；任一方向超过 {@link #MAX_FRAME} 或点火点非空气 → null
	 */
	public static int[] measureHole(BlockProbe probe) {
		if (!probe.isAir(0, 0)) {
			return null;
		}
		int left = scan(probe, -1, 0);
		int right = scan(probe, 1, 0);
		int down = scan(probe, 0, -1);
		int up = scan(probe, 0, 1);
		if (left < 0 || right < 0 || down < 0 || up < 0) {
			return null;
		}
		// 距离含框架本身，故净尺寸 = 左+右-1 / 下+上-1
		return new int[]{left + right - 1, down + up - 1};
	}

	/** 沿方向扫描到第一个非空气方块，返回步数；超过 MAX_FRAME 返回 -1 */
	private static int scan(BlockProbe probe, int sx, int sy) {
		for (int i = 1; i <= MAX_FRAME; i++) {
			if (!probe.isAir(sx * i, sy * i)) {
				return i;
			}
		}
		return -1;
	}
}
