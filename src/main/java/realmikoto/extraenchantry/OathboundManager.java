package realmikoto.extraenchantry;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 誓约（Oathbound）状态管理：随身 keepInventory。
 *
 * 效果（单级）：
 *   1. 带誓约的物品在玩家死亡时不掉落、不消失（消失诅咒同样无效）——
 *      重生后物品仍在背包原槽位
 *   2. 四件护甲（头/胸/腿/脚）全部带誓约时，死亡时经验值与分数也全额保留
 *
 * 实现原理（26.2 死亡链路，反编译确认）：
 *   - 玩家死亡掉落全部集中在 {@code Player#dropEquipment}（keepInventory 判断 →
 *     destroyVanishingCursedItems → inventory.dropAll，且 26.2 的
 *     LivingEntity#dropEquipment 是空壳）。在 HEAD 提取带誓约的物品、
 *     TAIL 原槽位放回，dropAll 与消失诅咒销毁都碰不到它们。
 *   - 重生时 {@code ServerPlayer#restoreFrom(old, keepEverything)} 仅在
 *     keepInventory 开启（或旁观者）时才搬运背包与经验；誓约存在时按需补一次搬运：
 *     四件套 → transferInventoryXpAndScore（背包+经验+分数全保留，
 *     经验掉落实体已在 LivingEntityMixin 中按条件跳过，防止重复）；
 *     部分誓约 → 仅 replaceWith 搬运背包，经验照常掉落。
 */
public final class OathboundManager {

	/** 四件护甲槽位（顺序无影响，仅用于全件判定） */
	private static final EquipmentSlot[] ARMOR_SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private OathboundManager() {
	}

	/** 判断物品是否带誓约 */
	public static boolean hasOathbound(ItemStack stack) {
		return ExtraEnchantry.getOathboundLevel(stack) > 0;
	}

	/** 判断四件护甲是否全部带誓约（经验保留条件） */
	public static boolean hasFullOathboundArmor(Player player) {
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			if (!hasOathbound(player.getItemBySlot(slot))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 死亡掉落前提取背包中全部带誓约的物品（含护甲与副手——26.2 的
	 * Inventory#getContainerSize 覆盖 items + 装备槽映射），
	 * 槽位清空以躲开 dropAll 与消失诅咒销毁。返回 槽位索引 → 物品快照。
	 */
	public static Map<Integer, ItemStack> extractOathbound(Player player) {
		Map<Integer, ItemStack> kept = new HashMap<>();
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty() && hasOathbound(stack)) {
				kept.put(i, stack.copy());
				inventory.setItem(i, ItemStack.EMPTY);
			}
		}
		return kept;
	}

	/** 将提取的誓约物品放回原槽位（原槽位此时必为空） */
	public static void restoreOathbound(Player player, Map<Integer, ItemStack> kept) {
		Inventory inventory = player.getInventory();
		for (Map.Entry<Integer, ItemStack> entry : kept.entrySet()) {
			inventory.setItem(entry.getKey(), entry.getValue());
		}
	}

	/** 判断背包（含装备槽）里是否还剩任何带誓约的物品——重生搬运的触发条件 */
	public static boolean hasAnyOathboundItem(Player player) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (hasOathbound(inventory.getItem(i))) {
				return true;
			}
		}
		return false;
	}
}
