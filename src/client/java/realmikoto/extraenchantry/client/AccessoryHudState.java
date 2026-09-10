package realmikoto.extraenchantry.client;

import realmikoto.extraenchantry.Accessories;

/**
 * 配饰栏展开动画状态（1.4.0 集成背包，纯客户端）。
 * 进度 0~1：展开时 0→1（滑入），折叠时 1→0（滑出）。
 * 真实槽位（isActive）仅在 EXPANDED && 进度完成时渲染；
 * 动画期间由各 Screen Mixin 自绘槽位底纹/幽灵图标/物品帧。
 */
public final class AccessoryHudState {

	/** 动画速度（进度/秒）：约 170ms 完成 */
	private static final float SPEED = 6.0F;

	private static float progress = 0.0F;

	private AccessoryHudState() {
	}

	public static boolean expanded() {
		return Accessories.EXPANDED;
	}

	public static float progress() {
		return progress;
	}

	/** 每渲染帧推进（partialTick 为帧间隔秒）；同步两静态量到 Accessories（公共侧 isActive 读） */
	public static void tick(float partialTick) {
		float target = Accessories.EXPANDED ? 1.0F : 0.0F;
		if (progress < target) {
			progress = Math.min(target, progress + partialTick * SPEED);
		} else if (progress > target) {
			progress = Math.max(target, progress - partialTick * SPEED * 1.4F);   // 收起略快
		}
		Accessories.ANIMATION_DONE = progress >= 1.0F && Accessories.EXPANDED;
	}

	/** 切换展开状态（点击按钮处调用） */
	public static void toggle() {
		Accessories.EXPANDED = !Accessories.EXPANDED;
		Accessories.ANIMATION_DONE = false;
	}

	/** 修复 #29：离开世界时重置全部动画状态（旧实现跨世界残留导致物品画在偏移位置） */
	public static void reset() {
		progress = 0.0F;
		Accessories.EXPANDED = false;
		Accessories.ANIMATION_DONE = false;
	}

	/** 缓动：ease-out（滑入前快后慢） */
	public static float eased() {
		float p = progress;
		return 1.0F - (1.0F - p) * (1.0F - p);
	}
}
