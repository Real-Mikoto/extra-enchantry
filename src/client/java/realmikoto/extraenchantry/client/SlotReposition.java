package realmikoto.extraenchantry.client;

/**
 * 槽位原地重定位接口（1.4.0 客户端）：
 * 由 {@code SlotMixin} 织入 {@code Slot} 基类实现——任何 Slot 实例
 * （含创造页签的原版 SlotWrapper）都可运行时改 x/y。
 *
 * <p>动机：创造界面 slotClicked 将点击的槽硬转为 SlotWrapper
 * （ClassCastException 修复记录，见 README 踩坑），替换列表条目的
 * 重定位方案不可行；本接口提供原地改坐标的安全通道。</p>
 */
public interface SlotReposition {

	/** 原地修改槽位渲染/交互坐标（物品区左上角，相对面板原点） */
	void extraenchantry$reposition(int x, int y);
}
