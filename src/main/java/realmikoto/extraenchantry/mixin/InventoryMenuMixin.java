package realmikoto.extraenchantry.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import realmikoto.extraenchantry.Accessories;
import realmikoto.extraenchantry.AccessoryContainer;
import realmikoto.extraenchantry.AccessoryItem;
import realmikoto.extraenchantry.AccessorySlot;

/**
 * 配饰槽进背包菜单（1.4.0 集成背包）：InventoryMenu 构造 TAIL 追加 4 个 AccessorySlot
 * （下标 46~49，副手 45 之后）。双端各持有自己的菜单实例，槽位内容经容器同步包
 * （containerId 0）自动同步——生存背包与创造玩家页签（其槽位列表整体替换为
 * InventoryMenu 槽位的包装，点击按 containerId 0 路由回本菜单）都因此直接可用，
 * 无需独立 GUI。
 *
 * <p>继承 AbstractContainerMenu 以访问 protected 的 addSlot / moveItemStackTo
 * （mixin 继承目标父类是访问受保护成员的标准手法，与目标类层级兼容）。</p>
 *
 * <p>quickMoveStack：shift 智能移动——配饰物品自动进匹配的空配饰槽；
 * 从配饰槽 shift 出去落到背包/快捷栏。原版实现的 0~45 区间逻辑不受影响。</p>
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {

	protected InventoryMenuMixin(MenuType<?> type, int containerId) {
		super(type, containerId);   // mixin 构造器不参与合并，仅为访问 protected 成员服务
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void extraenchantry$addAccessorySlots(net.minecraft.world.entity.player.Inventory inventory,
			boolean active, Player owner, CallbackInfo ci) {
		AccessoryContainer container = new AccessoryContainer(owner);
		for (Accessories.SlotType slotType : Accessories.SlotType.values()) {
			int i = slotType.ordinal();
			this.addSlot(new AccessorySlot(slotType, container, i, -11, 8 + i * 18));
		}
	}

	/**
	 * shift 智能移动（HEAD 拦截，不影响原版 0~45 逻辑）：
	 * 46~49（配饰槽）→ 背包 / 快捷栏；配饰物品 → 匹配类型的空配饰槽。
	 * 不做展开状态门禁（服务端无客户端状态；折叠时放入的物品在展开后可见，属可接受便利）。
	 */
	@Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
	private void extraenchantry$accessoryQuickMove(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
		int accessoryBase = this.slots.size() - Accessories.SlotType.values().length;   // 46
		if (index >= accessoryBase) {
			// 配饰槽 → 背包（9~35）/快捷栏（36~45）
			ItemStack stack = this.slots.get(index).getItem().copy();
			if (stack.isEmpty()) {
				cir.setReturnValue(ItemStack.EMPTY);
				return;
			}
			this.moveItemStackTo(stack, 9, 45, true);   // stack 原地缩减为余量
			this.slots.get(index).setByPlayer(stack);
			cir.setReturnValue(ItemStack.EMPTY);
			return;
		}
		ItemStack stack = this.slots.get(index).getItem();
		if (stack.isEmpty() || !(stack.getItem() instanceof AccessoryItem accessory)) {
			return;   // 非配饰物品：走原版逻辑
		}
		// 配饰物品 → 匹配类型的空配饰槽
		for (Accessories.SlotType type : Accessories.SlotType.values()) {
			if (type != accessory.slotType()) {
				continue;
			}
			AccessorySlot target = (AccessorySlot) this.slots.get(accessoryBase + type.ordinal());
			if (!target.getItem().isEmpty()) {
				continue;
			}
			ItemStack toMove = stack.copy();
			toMove.setCount(1);
			target.setByPlayer(toMove);
			stack.shrink(1);
			this.slots.get(index).setByPlayer(stack);
			cir.setReturnValue(stack);
			return;
		}
		// 无匹配空位：交还原版（背包内 shift 走背包逻辑，无副作用）
	}
}
