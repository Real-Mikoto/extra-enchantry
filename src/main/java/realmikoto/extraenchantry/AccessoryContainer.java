package realmikoto.extraenchantry;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 配饰 Attachment 的 Container 适配器（1.4.0）：
 * InventoryMenu 的 4 个 AccessorySlot 经此容器读写玩家 {@link AccessoryAttachments}。
 * Fabric Attachment 快照语义——所有写路径（setItem/removeItem/clear）统一经
 * {@link AccessoryAttachments#set} 落盘。服务端实例持久写回；客户端实例承接
 * 同步包写入（内容同样落到客户端 attachment，仅供菜单预测，无实际用途）。
 */
public final class AccessoryContainer implements Container {

	private final Player owner;

	public AccessoryContainer(Player owner) {
		this.owner = owner;
	}

	public Player owner() {
		return owner;
	}

	private void writeBack(ItemStack[] slots) {
		AccessoryAttachments.set(owner, slots);
	}

	@Override
	public int getContainerSize() {
		return Accessories.SlotType.values().length;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : AccessoryAttachments.slots(owner)) {
			if (!stack.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int index) {
		return AccessoryAttachments.slots(owner)[index];
	}

	@Override
	public ItemStack removeItem(int index, int count) {
		ItemStack[] slots = AccessoryAttachments.slots(owner);
		ItemStack removed = slots[index].split(count);
		writeBack(slots);
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		ItemStack[] slots = AccessoryAttachments.slots(owner);
		ItemStack out = slots[index];
		slots[index] = ItemStack.EMPTY;
		writeBack(slots);
		return out;
	}

	@Override
	public void setItem(int index, ItemStack stack) {
		ItemStack[] slots = AccessoryAttachments.slots(owner);
		ItemStack previous = slots[index];
		slots[index] = stack == null ? ItemStack.EMPTY : stack;
		writeBack(slots);
		// 家族铭印穿戴爆发（GUI 拖拽 / shift 移入路径）：空槽 → 已镶嵌宝石的配饰
		if (previous.isEmpty() && stack != null && !stack.isEmpty()
				&& stack.getItem() instanceof AccessoryItem accessory
				&& owner instanceof net.minecraft.server.level.ServerPlayer serverPlayer
				&& serverPlayer.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
			String gem = Accessories.socketedGem(stack);
			if (gem != null) {
				GemFx.onEquip(serverLevel, serverPlayer, gem, accessory.material());
			}
		}
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		List<ItemStack> empty = new ArrayList<>();
		for (int i = 0; i < getContainerSize(); i++) {
			empty.add(ItemStack.EMPTY);
		}
		AccessoryAttachments.set(owner, empty.toArray(ItemStack[]::new));
	}
}
