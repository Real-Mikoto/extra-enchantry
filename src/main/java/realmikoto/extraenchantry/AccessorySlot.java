package realmikoto.extraenchantry;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 配饰槽（1.4.0 集成背包）：InventoryMenu 追加的 4 个槽位（下标 46~49）。
 * 服务端读写 Attachment；客户端经菜单同步包接收内容。
 *
 * <p>位置：生存背包渲染坐标（盔甲列左侧一列 x=-11，y 与四件护甲同排）；
 * 创造玩家页签的渲染位置由 SlotWrapper 重包装决定（见 CreativeModeInventoryScreenMixin），
 * 本类 x/y 不参与该路径。</p>
 *
 * <p>{@link #isActive()} 为 26.2 纯客户端概念（AbstractContainerMenu/Slot 内部零引用）：
 * 返回「配饰栏已展开且滑入动画完成」——折叠时不渲染、不可悬停/点击；
 * 服务端不读取该值，点击/放入不受影响。</p>
 */
public class AccessorySlot extends Slot {

	private final Accessories.SlotType slotType;

	public AccessorySlot(Accessories.SlotType slotType, AccessoryContainer container, int index, int x, int y) {
		super(container, index, x, y);
		this.slotType = slotType;
	}

	public Accessories.SlotType slotType() {
		return slotType;
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return Accessories.slotOf(stack) == slotType;
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return 1;
	}

	@Override
	public boolean mayPickup(Player player) {
		return true;
	}

	/**
	 * 空槽幽灵图标（26.2 走 GUI 图集 blitSprite）：
	 * 精灵 id 对应 assets/…/textures/gui/sprites/ghost_{slot}.png。
	 * 动画期间由 Screen 自绘（extractBackground 层）；动画完成后本图标接管。
	 */
	@Override
	public net.minecraft.resources.Identifier getNoItemIcon() {
		return ExtraEnchantry.id("gui/ghost_" + slotType.path);
	}

	/** 纯客户端判定：已展开且动画完成（服务端零调用，26.2 反编译确认） */
	@Override
	public boolean isActive() {
		return Accessories.EXPANDED && Accessories.ANIMATION_DONE;
	}
}
