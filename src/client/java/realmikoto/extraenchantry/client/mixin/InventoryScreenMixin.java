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

	/**
	 * 配饰列槽位坐标（性能：extractBackground 是每帧路径，
	 * 原先每帧 new 两个 int[] 造成无谓的 GC 压力；坐标恒定故提升为常量。
	 * drawColumn 只读遍历这两个数组，不做写入，共享安全）。
	 */
	private static final int[] EXTRAENCHANTRY$SLOT_X = {-10, -10, -10, -10};
	private static final int[] EXTRAENCHANTRY$SLOT_Y = {8, 26, 44, 62};

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
				EXTRAENCHANTRY$SLOT_X,
				EXTRAENCHANTRY$SLOT_Y,
				-13, 5, 20, 76);
		// 配饰按钮（盾牌列头盔行；贴图 18×18 全不透明自带槽框底，直接绘制即可。
		// 不得加 panel_fill 衬底：原版纸娃娃渲染区右缘在 x=75（extractEntityInInventory
		// 参数 26~75），纯色衬底左缘若 <76 会压进纸娃娃 1~2px——且纯色与面板纹理
		// 存在色阶差，四面边缘都会显出色缝）
		AccessoryColumnRenderer.drawButton(extractor, this.leftPos, this.topPos, 77, 8);
		// 修复 #29：按钮悬停提示——旧实现无 tooltip、无按键、无引导，配饰槽位难以被发现
		if (AccessoryColumnRenderer.buttonHit(this.leftPos, this.topPos, mouseX, mouseY, 77, 8)) {
			extractor.setTooltipForNextFrame(net.minecraft.network.chat.Component.translatable(
					"tooltip.extra-enchantry.accessory_panel"), mouseX, mouseY);
		}
	}
}
