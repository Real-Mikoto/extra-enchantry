package realmikoto.extraenchantry;

/**
 * 渊息（Tideheart）氧气上限公式（纯函数，独立成类以便单元测试）。
 *
 * 每级 +300 tick（+15 秒）：I / II / III 级 → 600 / 900 / 1200 tick（30 / 45 / 60 秒）；
 * {@code airCap(0)} 恒等于原版上限 300 tick（15 秒）——公式与原版值的兼容性是测试的一部分。
 *
 * 使用方：{@code EntityMixin#extraenchantry$tideheartMaxAir}（注入 {@code Entity#getMaxAirSupply}）。
 * 26.2 构造期 NPE 防护（defineSyncker 早于 equipment 初始化）见该 Mixin 处注释——
 * 该时序行为依赖 Minecraft 运行时，由实测覆盖；此处只固化数值公式。
 */
public final class TideheartAir {

	private TideheartAir() {
	}

	/** 渊息等级 → 氧气上限（tick）；level = 0 即原版 300（15 秒） */
	public static int airCap(int level) {
		return 300 + 300 * level;
	}
}
