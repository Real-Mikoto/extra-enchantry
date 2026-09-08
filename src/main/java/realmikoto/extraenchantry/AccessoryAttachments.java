package realmikoto.extraenchantry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 配饰栏持久化（1.4.0）：服务端权威 Fabric Data Attachment，存 4 槽 ItemStack。
 *
 * <p>刻意 <b>非 copyOnDeath</b>——死亡掉落 / keepInventory 保留 / 誓约保护
 * 全部由 {@link OathboundManager} 的显式死亡链路处理（dropEquipment / restoreFrom）。
 *
 * <p>写回规则（Fabric Attachment 快照语义）：任何对槽内 ItemStack 的修改后必须
 * {@code setAttached} 写回；{@link #set} 是唯一安全写入口。
 */
public final class AccessoryAttachments {

	/** 4 槽数据（固定 4 元素，缺省全空） */
	public record AccessoryData(List<ItemStack> slots) {
		public static final Codec<AccessoryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ItemStack.OPTIONAL_CODEC.listOf()
						.optionalFieldOf("slots", List.of())
						.forGetter(AccessoryData::slots)
		).apply(instance, AccessoryData::new));

		public static final AccessoryData EMPTY = new AccessoryData(List.of());

		public ItemStack get(int index) {
			return index < slots.size() ? slots.get(index) : ItemStack.EMPTY;
		}
	}

	public static final AttachmentType<AccessoryData> ACCESSORIES =
			AttachmentRegistry.<AccessoryData>create(ExtraEnchantry.id("accessories"),
					builder -> builder.persistent(AccessoryData.CODEC));

	private AccessoryAttachments() {
	}

	/** 读取 4 槽快照（无 attachment → 全空数组） */
	public static ItemStack[] slots(net.minecraft.world.entity.LivingEntity entity) {
		ItemStack[] out = new ItemStack[Accessories.SlotType.values().length];
		for (int i = 0; i < out.length; i++) {
			out[i] = ItemStack.EMPTY;
		}
		AccessoryData data = entity.getAttached(ACCESSORIES);
		if (data != null) {
			for (int i = 0; i < out.length; i++) {
				out[i] = data.get(i);
			}
		}
		return out;
	}

	/** 读单个槽位 */
	public static ItemStack get(net.minecraft.world.entity.LivingEntity entity, Accessories.SlotType slot) {
		AccessoryData data = entity.getAttached(ACCESSORIES);
		return data == null ? ItemStack.EMPTY : data.get(slot.ordinal());
	}

	/**
	 * 唯一安全写入口：整体写入 4 槽（快照语义，改后必须 setAttached）。
	 * 调用方持有数组引用原地修改后经此方法落盘。
	 */
	public static void set(net.minecraft.world.entity.LivingEntity entity, ItemStack[] slots) {
		List<ItemStack> list = new ArrayList<>(slots.length);
		for (ItemStack stack : slots) {
			list.add(stack == null ? ItemStack.EMPTY : stack);
		}
		entity.setAttached(ACCESSORIES, new AccessoryData(list));
	}

	/** 单槽写入便捷入口 */
	public static void set(net.minecraft.world.entity.LivingEntity entity,
			Accessories.SlotType slot, ItemStack stack) {
		ItemStack[] slots = slots(entity);
		slots[slot.ordinal()] = stack == null ? ItemStack.EMPTY : stack;
		set(entity, slots);
	}
}
