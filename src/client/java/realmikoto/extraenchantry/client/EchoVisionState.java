package realmikoto.extraenchantry.client;

/**
 * 回声视觉客户端状态（v1.8.0.2 完整实现，设计稿 §6.2 + §4.4）。
 *
 * 设计稿要求："世界以线框 + 涟漪的形式被勾勒，静止时几乎全黑，移动时万物显形"，
 * 且"你的行动本身就是手电筒，也是警报"。
 *
 * 实现（26.2 渲染管线重构后无 WorldRenderEvents，改用光照层钩子）：
 * - 玩家做出"响"动作（移动/攻击/使用物品）→ 触发一次"回声脉冲"；
 * - 脉冲期间通过 LightmapRenderState 提升 blockFactor / brightness，
 *   使周围地形在黑暗中**短暂显形**（等同于"声纹照亮数格"）；
 * - 脉冲强度随时间衰减，形成"涟漪"般的明暗起伏；
 * - **可访问性**（§10）：{@link #reduceFlashing} 开启时削弱闪烁幅度并延长脉冲，
 *   避免光敏不适（对应设计稿"降低闪烁、增强轮廓"的可访问性选项）。
 */
public final class EchoVisionState {

	/** 当前脉冲强度 0.0–1.0 */
	private static float pulse = 0.0F;
	/** 每次脉冲的衰减速率 */
	private static final float DECAY = 0.06F;
	/** 可访问性：降低闪烁（由按键切换） */
	private static volatile boolean reduceFlashing = false;
	/** 可访问性：黑暗防护（§10 无光选项——免疫强黑暗视觉压迫） */
	private static volatile boolean darknessProtection = false;
	/** 上一次的移动基准 */
	private static double lastX, lastY, lastZ;
	private static boolean hasLast = false;

	private EchoVisionState() {
	}

	public static boolean darknessProtection() {
		return darknessProtection;
	}

	public static void toggleDarknessProtection() {
		darknessProtection = !darknessProtection;
		ClientConfig.save();
	}

	/** 触发一次回声脉冲（强度 0–1） */
	public static void trigger(float strength) {
		pulse = Math.min(1.0F, Math.max(pulse, strength));
	}

	/** 每客户端 tick：衰减 + 由本地动作自动触发 */
	public static void tick(net.minecraft.client.player.LocalPlayer player) {
		if (player == null) {
			hasLast = false;
			pulse = 0.0F;
			return;
		}
		// 静默附魔（§5.3）代价：自身失去回声视觉——本地脉冲完全抑制
		if (hasStillness(player)) {
			lastX = player.getX(); lastY = player.getY(); lastZ = player.getZ();
			hasLast = true;
			pulse = 0.0F;
			return;
		}
		// 移动检测（本地即时反馈，无需等服务端）
		if (!hasLast) {
			lastX = player.getX(); lastY = player.getY(); lastZ = player.getZ();
			hasLast = true;
		}
		double dx = player.getX() - lastX;
		double dy = player.getY() - lastY;
		double dz = player.getZ() - lastZ;
		double moved = dx * dx + dy * dy + dz * dz;
		lastX = player.getX(); lastY = player.getY(); lastZ = player.getZ();

		if (moved > 0.0004D) {
			float s = player.isShiftKeyDown() ? 0.25F : 0.6F;
			if (player.isSprinting()) {
				s = 0.9F;
			}
			trigger(s);
		}
		// 衰减
		if (pulse > 0.0F) {
			float decay = reduceFlashing ? DECAY * 0.4F : DECAY;
			pulse = Math.max(0.0F, pulse - decay);
		}
	}

	/** 静默附魔检测（客户端侧：靴子附魔读取） */
	private static boolean hasStillness(net.minecraft.client.player.LocalPlayer player) {
		var stack = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET);
		if (stack.isEmpty()) {
			return false;
		}
		for (var holder : stack.getEnchantments().keySet()) {
			if (holder.is(realmikoto.extraenchantry.ExtraEnchantry.STILLNESS)) {
				return true;
			}
		}
		return false;
	}

	/** 当前光照增益（供 Lightmap 混入使用） */
	public static float brightnessBoost() {
		if (pulse <= 0.0F) {
			return 0.0F;
		}
		// 降低闪烁模式：压低峰值、抬高地平（"增强轮廓"而非"闪烁"）
		float peak = reduceFlashing ? 0.45F : 1.0F;
		float floor = reduceFlashing ? 0.25F : 0.0F;
		return floor + pulse * peak * 0.55F;
	}

	public static boolean isActive() {
		return pulse > 0.0F;
	}

	public static boolean reduceFlashing() {
		return reduceFlashing;
	}

	public static void setReduceFlashing(boolean value) {
		reduceFlashing = value;
	}

	public static void setDarknessProtection(boolean value) {
		darknessProtection = value;
	}

	public static void toggleReduceFlashing() {
		reduceFlashing = !reduceFlashing;
		ClientConfig.save();
	}

	public static void reset() {
		pulse = 0.0F;
		hasLast = false;
	}
}
