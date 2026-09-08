package realmikoto.extraenchantry.client.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import realmikoto.extraenchantry.client.SlotReposition;

/**
 * 槽位原地重定位（1.4.0 客户端）：向 Slot 基类织入
 * {@link SlotReposition#extraenchantry$reposition}——运行时改写
 * final 的 x/y（@Mutable 解除 final 约束，mixin 标准手法）。
 *
 * <p>用于创造玩家页签：原版 selectTab 用 SlotWrapper 包装 InventoryMenu
 * 槽位，而 slotClicked 又把点击槽硬转为 SlotWrapper——替换列表条目会
 * ClassCastException（每次点击必炸）。因此重定位只能原地改坐标，
 * 列表结构保持原样。</p>
 */
@Mixin(Slot.class)
public abstract class SlotMixin implements SlotReposition {

	@Shadow
	@Final
	@Mutable
	public int x;

	@Shadow
	@Final
	@Mutable
	public int y;

	@Override
	public void extraenchantry$reposition(int x, int y) {
		this.x = x;
		this.y = y;
	}
}
