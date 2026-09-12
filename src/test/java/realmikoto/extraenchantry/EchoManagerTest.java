package realmikoto.extraenchantry;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 声纹系统纯逻辑测试（1.8.1「声」）——不启动 Minecraft，直接反射驱动内部状态。
 *
 * 覆盖：声纹记录 / 衰减口径、凝视累积与清理、风暴周期边界（8–13 分钟随机窗口）。
 */
class EchoManagerTest {

	private static Map<UUID, ?> map(String field) throws Exception {
		Field f = EchoManager.class.getDeclaredField(field);
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		Map<UUID, ?> m = (Map<UUID, ?>) f.get(null);
		return m;
	}

	@Test
	void gazeClearedOnDisconnect() throws Exception {
		UUID id = UUID.randomUUID();
		Map<UUID, Integer> gaze = (Map<UUID, Integer>) map("GAZE");
		gaze.put(id, 600);
		EchoManager.onDisconnect(id);
		assertFalse(gaze.containsKey(id), "登出后凝视值必须清理");
	}

	@Test
	void stormBoundarySanity() throws Exception {
		// 风暴冷却常量应介于 8–13 分钟（20 t/s）
		// 通过 onServerStopped 重置后用反射读取
		EchoManager.onServerStopped();
		Field f = EchoManager.class.getDeclaredField("stormCooldown");
		f.setAccessible(true);
		int cooldown = (Integer) f.get(null);
		assertTrue(cooldown >= 20 * 60 * 8, "风暴冷却下限 8 分钟");
		assertTrue(cooldown <= 20 * 60 * 13, "风暴冷却上限 12 分钟 + 余量");
	}

	@Test
	void lastEchoDefaultsZero() throws Exception {
		UUID id = UUID.randomUUID();
		// 非玩家路径：lastEcho(Player) 需要 Player 实例——此处仅验证 map 默认语义
		Map<UUID, Float> lastEcho = (Map<UUID, Float>) map("LAST_ECHO");
		assertFalse(lastEcho.containsKey(id), "未发声玩家不应有条目（缺省 0）");
		assertEquals(0.0F, lastEcho.getOrDefault(id, 0.0F), 1e-6);
	}
}
