package realmikoto.extraenchantry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 渊息（Tideheart）氧气上限公式测试。
 *
 * 公式由 {@link TideheartAir#airCap(int)} 固化，{@code EntityMixin} 的
 * {@code getMaxAirSupply} 注入按它放大（每级 +15 秒）。
 * 构造期 NPE 防御（defineSyncker 早于 LivingEntity#equipment 初始化）依赖
 * Minecraft 运行时、无法脱离游戏单测——其行为由 v1.3.2 dev 客户端实测覆盖
 * （创建世界 → 玩家正常放置）；本测试只锁数值公式不被回归。
 */
class TideheartAirTest {

	@Test
	void level0KeepsVanillaAirCap() {
		// 与原版兼容的基石：无渊息时公式必须还原 300 tick（15 秒）
		assertEquals(300, TideheartAir.airCap(0));
	}

	@Test
	void levelsOneToThreeAddFifteenSecondsEach() {
		assertEquals(600, TideheartAir.airCap(1), "I 级 = 30 秒");
		assertEquals(900, TideheartAir.airCap(2), "II 级 = 45 秒");
		assertEquals(1200, TideheartAir.airCap(3), "III 级 = 60 秒（渊息附魔 max_level）");
	}

	@Test
	void formulaIsLinearPerLevel() {
		// 差分恒为 300 tick/级——若未来改阶梯曲线，此断言会强制重审附魔文档
		for (int level = 1; level <= 3; level++) {
			assertEquals(300, TideheartAir.airCap(level) - TideheartAir.airCap(level - 1),
					"第 " + level + " 级增量");
		}
	}
}
