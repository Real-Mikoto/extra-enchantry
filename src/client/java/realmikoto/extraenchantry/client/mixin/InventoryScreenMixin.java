package realmikoto.extraenchantry.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import realmikoto.extraenchantry.client.AccessoryColumnRenderer;

/**
 * 生存背包集成（1.4.0）：配饰列出现在四件护甲左侧一列（x=-11，与护甲同排同尺寸），
 * 配饰按钮位于盾牌列（x=77）头盔行（y=8）——与盾牌同列、与头盔平齐、尺寸相同。
 * 点击展开后平滑滑入；真实槽位（菜单 46~49）动画完成后接管渲染与交互
 * （isActive 为 26.2 纯客户端概念，折叠时不渲染、不可悬停/点击——反编译确认）。
 *
 * <p>衬底：配饰列悬浮于面板左缘外（游戏世界上方），以面板色打底避免"漂浮暗块"观感；
 * 槽框绘制在槽位 -1,-1 偏移处与原版护甲槽框对齐（修复"低几个像素"）。</p>
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {

	protected InventoryScreenMixin(InventoryMenu menu, net.minecraft.world.entity.player.Inventory inventory,
			net.minecraft.network.chat.Component title) {
		super(menu, inventory, title);   // mixin 构造器不参与合并
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void extraenchantry$drawAccessoryColumn(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
			float partialTick, CallbackInfo ci) {
		AccessoryColumnRenderer.tickAnimation(partialTick);
		// 配饰列（x=-11 列；衬底外扩 2px，右侧止于护甲框 x=7 之前）
		AccessoryColumnRenderer.drawColumn(extractor, this.leftPos, this.topPos, this.getMenu(),
				new int[]{-10, -10, -10, -10},
				new int[]{8, 26, 44, 62},
				-13, 5, 20, 76);
		// 配饰按钮（盾牌列头盔行；衬底盖住按钮区域——面板上不可见）
		AccessoryColumnRenderer.drawBacking(extractor, this.leftPos, this.topPos, 74, 6, 22, 22);
		AccessoryColumnRenderer.drawButton(extractor, this.leftPos, this.topPos, 77, 8);
	}
}
