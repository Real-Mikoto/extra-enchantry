package realmikoto.extraenchantry.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import realmikoto.extraenchantry.client.AccessoryColumnRenderer;

/**
 * 生存背包点击网关（1.4.0）：InventoryScreen 未覆写 mouseClicked，
 * 注入基类 AbstractContainerScreen 并按实例门控——仅生存背包内的
 * 配饰按钮（盾牌列 x=77 头盔行 y=8）消费点击。
 *
 * <p>leftPos/topPos 声明于目标类自身，@Shadow 可直接解析
 * （踩坑记录：@Shadow 只解析目标类本类成员——本例目标类恰是声明者）。</p>
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

	@Shadow
	protected int leftPos;

	@Shadow
	protected int topPos;

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void extraenchantry$accessoryButtonClick(MouseButtonEvent event, boolean inside,
			CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof InventoryScreen)) {
			return;
		}
		if (event.button() == 0
				&& AccessoryColumnRenderer.buttonHit(this.leftPos, this.topPos, event.x(), event.y(), 77, 8)) {
			// 主键（左键）点击配饰按钮 → 切换展开
			AccessoryColumnRenderer.toggle();
			cir.setReturnValue(true);
		}
	}
}
