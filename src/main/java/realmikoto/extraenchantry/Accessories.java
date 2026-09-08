package realmikoto.extraenchantry;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.Locale;

/**
 * 环佩体系（1.4.0「环佩与獠牙」）：配饰槽位 / 材质 / 宝石的注册中心与查询 API。
 *
 * <p>物品布局：4 槽位 × 4 材质 = 16 件配饰（{@link AccessoryItem}）+ 8 家族宝石
 * （{@link GemItem}）。宝石在合成时即决定家族被动（{@code socketed_gem} 组件经
 * 配方 result 的 components 写入，不可事后更换）；材质只影响被动传导率。
 *
 * <p>槽位绑定（宝石与配饰附魔同族）：耳环 = 灵魂 / 守护，项链 = 风暴 / 自然，
 * 戒指 = 锋刃 / 风，手镯 = 火焰 / 水渊。
 */
public final class Accessories {

	/** 配饰槽位（Attachment 下标即 ordinal） */
	public enum SlotType {
		EARRING("earring", "soul_amber", "guard_jade"),
		NECKLACE("necklace", "storm_stone", "sprout_crystal"),
		RING("ring", "blade_shard", "wind_feather"),
		BRACELET("bracelet", "ember_heart", "tide_pearl");

		public final String path;
		/** 该槽位可用的两颗宝石（与槽位附魔同族） */
		public final String[] gems;

		SlotType(String path, String... gems) {
			this.path = path;
			this.gems = gems;
		}

		public static SlotType ofPath(String path) {
			try {
				return valueOf(path.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
	}

	/** 配饰材质（决定宝石被动传导率） */
	public enum Material {
		COPPER("copper", 1.0D),
		IRON("iron", 1.25D),
		GOLD("gold", 1.5D),
		DIAMOND("diamond", 2.0D);

		public final String path;
		public final double conductivity;

		Material(String path, double conductivity) {
			this.path = path;
			this.conductivity = conductivity;
		}

		public static Material ofPath(String path) {
			try {
				return valueOf(path.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
	}

	/** 镶嵌宝石组件（Identifier path，如 "soul_amber"；缺省 = 未镶嵌无被动） */
	public static final DataComponentType<String> SOCKETED_GEM =
			DataComponentType.<String>builder().persistent(Codec.STRING).build();

	// ============ 物品实例（onInitialize 注册进注册表） ============

	/** 16 件配饰（material × slot，注册 ID = {material}_{slot}，如 iron_ring） */
	public static final Item[][] ACCESSORY_ITEMS = new Item[Material.values().length][SlotType.values().length];

	/** 8 颗家族宝石（注册 ID = 宝石 path） */
	public static final Item[] GEM_ITEMS = new Item[8];

	/** 槽位准入物品标签（data/extra-enchantry/tags/item/accessory/{slot}.json） */
	public static final TagKey<Item>[] SLOT_TAGS = new TagKey[SlotType.values().length];

	static {
		for (SlotType slot : SlotType.values()) {
			SLOT_TAGS[slot.ordinal()] = TagKey.create(Registries.ITEM, id("accessory/" + slot.path));
		}
	}

	// ============ 配饰栏展开状态（客户端写，两静态量；服务端零读取） ============
	// isActive 为 26.2 纯客户端渲染/悬停概念（AbstractContainerMenu 与 Slot 内部零引用，
	// 反编译确认），折叠/展开门控只影响渲染与点击命中，服务端逻辑不受影响。
	/** 配饰栏是否展开（客户端会话级，默认折叠） */
	public static volatile boolean EXPANDED = false;
	/** 滑入动画是否已完成（未完成期间真实槽位不渲染，由 Screen 自绘动画帧） */
	public static volatile boolean ANIMATION_DONE = false;

	private Accessories() {
	}

	/** 注册 24 个物品 + socketed_gem 组件（onInitialize 调用一次） */
	public static void register() {
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("socketed_gem"), SOCKETED_GEM);

		for (Material material : Material.values()) {
			for (SlotType slot : SlotType.values()) {
				Item item = new AccessoryItem(slot, material, new Item.Properties()
						.setId(ResourceKey.create(Registries.ITEM,
								id(material.path + "_" + slot.path)))
						.stacksTo(1));
				ACCESSORY_ITEMS[material.ordinal()][slot.ordinal()] = item;
				Registry.register(BuiltInRegistries.ITEM,
						id(material.path + "_" + slot.path), item);
			}
		}

		String[] gems = {"soul_amber", "storm_stone", "blade_shard", "sprout_crystal",
				"guard_jade", "wind_feather", "ember_heart", "tide_pearl"};
		for (int i = 0; i < gems.length; i++) {
			Item item = new GemItem(gems[i], new Item.Properties()
					.setId(ResourceKey.create(Registries.ITEM, id(gems[i])))
					.rarity(Rarity.UNCOMMON));
			GEM_ITEMS[i] = item;
			Registry.register(BuiltInRegistries.ITEM, id(gems[i]), item);
		}
	}

	public static Identifier id(String path) {
		return ExtraEnchantry.id(path);
	}

	// ============ 查询 API ============

	/** 配饰物品的槽位（非配饰物品 → null） */
	public static SlotType slotOf(ItemStack stack) {
		if (stack.getItem() instanceof AccessoryItem accessory) {
			return accessory.slotType();
		}
		return null;
	}

	/** 配饰物品的材质（非配饰物品 → null） */
	public static Material materialOf(ItemStack stack) {
		if (stack.getItem() instanceof AccessoryItem accessory) {
			return accessory.material();
		}
		return null;
	}

	/** 配饰上镶嵌的宝石 path（未镶嵌 → null） */
	public static String socketedGem(ItemStack stack) {
		String gem = stack.get(SOCKETED_GEM);
		return gem == null || gem.isEmpty() ? null : gem;
	}

	/** 该槽位是否接受这颗宝石（槽位绑定两家族） */
	public static boolean gemFitsSlot(SlotType slot, String gemPath) {
		for (String gem : slot.gems) {
			if (gem.equals(gemPath)) {
				return true;
			}
		}
		return false;
	}

	/** 宝石被动传导率（配饰材质决定；非配饰 / 无宝石 → 0） */
	public static double conductivityOf(ItemStack stack) {
		Material material = materialOf(stack);
		return material == null ? 0.0D : material.conductivity;
	}
}
