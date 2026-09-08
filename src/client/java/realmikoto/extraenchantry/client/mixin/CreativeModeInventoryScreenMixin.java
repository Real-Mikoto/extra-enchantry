package realmikoto.extraenchantry.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import realmikoto.extraenchantry.client.AccessoryColumnRenderer;
import realmikoto.extraenchantry.client.AccessoryHudState;
import realmikoto.extraenchantry.client.SlotReposition;

/**
 * 创造背包集成（1.4.0 玩家页签）：
 * <ul>
 *   <li>盾牌栏（副手）下移与胸甲平齐：(35,20) → (35,33)；展开时随按钮让位滑到面板左缘外 (-3,33)；</li>
 *   <li>配饰按钮放在头盔栏左侧、与头盔平齐、同尺寸：(35,6)；展开时滑到 (-3,6)；</li>
 *   <li>展开后配饰 2×2 出现在装备栏左侧（(16,6)/(35,6)/(16,33)/(35,33)）。</li>
 * </ul>
 * 原版 selectTab 会把 ItemPickerMenu 的槽位列表整体替换为 InventoryMenu 槽位的
 * SlotWrapper 包装（点击按 containerId 0 路由到服务端 InventoryMenu）。
 *
 * <p><b>重定位必须原地改坐标</b>（{@link SlotReposition}）：创造界面
 * slotClicked 将点击槽硬转为 SlotWrapper——替换列表条目会
 * ClassCastException（每次点击必炸，配饰与背包格全部无法交互，
 * 踩坑记录见 README）。</p>
 *
 * <p>渲染要点：副手原版槽框烤入 tab_inventory.png 的 (35,20)——副手移位后
 * 该框成为残影，以面板色衬底覆盖（右侧止于护甲框 x=53 之前）。</p>
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends AbstractContainerScreen<AbstractContainerMenu> {

	protected CreativeModeInventoryScreenMixin(AbstractContainerMenu menu,
			net.minecraft.world.entity.player.Inventory inventory, net.minecraft.network.chat.Component title) {
		super(menu, inventory, title);   // mixin 构造器不参与合并
	}

	@Inject(method = "selectTab", at = @At("TAIL"))
	private void extraenchantry$fixupPlayerTab(net.minecraft.world.item.CreativeModeTab tab, CallbackInfo ci) {
		CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
		if (!screen.isInventoryOpen()) {
			return;
		}
		extraenchantry$repositionAccessories();
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void extraenchantry$drawAccessoryPanel(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
			float partialTick, CallbackInfo ci) {
		CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
		if (!screen.isInventoryOpen()) {
			return;
		}
		AccessoryColumnRenderer.tickAnimation(partialTick);
		// 按钮/副手共用滑出 X：折叠 35 → 展开 -3
		int slideX = Math.round(35 - 38.0F * AccessoryHudState.eased());
		// 1) 衬底：按钮 + 副手列区域（面板色；在面板上不可见；同时覆盖烤入贴图的旧副手框残影）
		AccessoryColumnRenderer.drawBacking(extractor, this.leftPos, this.topPos, slideX - 2, 3, 20, 50);
		// 2) 副手槽框（当前位：折叠 (35,33) / 展开随列滑出）
		AccessoryColumnRenderer.drawSlotBg(extractor, this.leftPos, this.topPos, slideX, 33);
		// 3) 配饰 2×2（装备栏左侧，与装备 2×2 同风格同尺寸；面板内无需衬底）
		AccessoryColumnRenderer.drawColumn(extractor, this.leftPos, this.topPos, this.getMenu(),
				new int[]{16, 35, 16, 35},
				new int[]{6, 6, 33, 33},
				0, 0, 0, 0);
		// 4) 配饰按钮（头盔左侧头盔行）
		AccessoryColumnRenderer.drawButton(extractor, this.leftPos, this.topPos, slideX, 6);
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void extraenchantry$accessoryButtonClick(MouseButtonEvent event, boolean inside,
			CallbackInfoReturnable<Boolean> cir) {
		CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
		if (!screen.isInventoryOpen() || event.button() != 0) {
			return;
		}
		int slideX = Math.round(35 - 38.0F * AccessoryHudState.eased());
		if (AccessoryColumnRenderer.buttonHit(this.leftPos, this.topPos, event.x(), event.y(), slideX, 6)) {
			AccessoryColumnRenderer.toggle();
			extraenchantry$repositionAccessories();   // 副手列立即让位/复位
			cir.setReturnValue(true);
		}
	}

	/**
	 * 原地重定位玩家页签槽位（不替换列表条目——见类 javadoc 的 CCE 踩坑）：
	 * 副手（下标 45）下移与胸甲平齐（展开时让位至面板左缘外）；
	 * 配饰槽（下标 46~49，原版包装顺序保证）就位到装备左侧 2×2。
	 * 折叠时配饰槽 isActive=false 不渲染不可交互——位置仅为展开就位。
	 */
	private void extraenchantry$repositionAccessories() {
		AbstractContainerMenu menu = this.getMenu();
		if (menu.slots.size() < 50) {
			return;   // 玩家页签 = 50 包装槽 + 1 销毁槽；其他页签不动
		}
		int offhandX = AccessoryHudState.expanded() ? -3 : 35;
		((SlotReposition) (Object) menu.slots.get(45)).extraenchantry$reposition(offhandX, 33);

		int[] xs = {16, 35, 16, 35};
		int[] ys = {6, 6, 33, 33};
		for (int i = 0; i < xs.length; i++) {
			((SlotReposition) (Object) menu.slots.get(46 + i)).extraenchantry$reposition(xs[i], ys[i]);
		}
	}
}
