package realmikoto.extraenchantry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * 幽渊资源注册中心（1.8.3「藏」）。
 */
public final class AbyssResources {

	public static final AbyssResourceItem ECHO_CRYSTAL = new AbyssResourceItem(AbyssResourceItem.Kind.ECHO_CRYSTAL);
	public static final AbyssResourceItem TIDE_TEAR = new AbyssResourceItem(AbyssResourceItem.Kind.TIDE_TEAR);
	public static final AbyssResourceItem SCULK_SILK = new AbyssResourceItem(AbyssResourceItem.Kind.SCULK_SILK);
	public static final AbyssResourceItem MEMORY_DUST = new AbyssResourceItem(AbyssResourceItem.Kind.MEMORY_DUST);
	public static final AbyssResourceItem SILENCE_STONE = new AbyssResourceItem(AbyssResourceItem.Kind.SILENCE_STONE);
	public static final AbyssResourceItem ABYSSAL_HEART = new AbyssResourceItem(AbyssResourceItem.Kind.ABYSSAL_HEART);
	/** 无相之尘（第九相线索实体化，渊心产出） */
	public static final AbyssResourceItem AETHER_DUST = new AbyssResourceItem(AbyssResourceItem.Kind.AETHER_DUST);

	// ============ 1.8.4 装备三件套（设计稿 §5.4） ============

	/** 静默斗篷：随身降低声纹半径（潜行时近乎无声） */
	public static final Item HUSH_CLOAK = new AbyssGearItem("hush_cloak",
			AbyssGearItem.Effect.HUSH, net.minecraft.world.item.Rarity.RARE);
	/** 深息面罩：延长深息 / 无光微光视野 */
	public static final Item TIDEHEART_MASK = new AbyssGearItem("tideheart_mask",
			AbyssGearItem.Effect.DEEPDIVE, net.minecraft.world.item.Rarity.RARE);
	/** 记忆之匣：存放 / 重放一段记忆残片（收藏向） */
	public static final Item MEMORY_CASKET = new AbyssGearItem("memory_casket",
			AbyssGearItem.Effect.CASKET, net.minecraft.world.item.Rarity.UNCOMMON);

	/** 共鸣音叉（§6.6 共鸣调式入口） */
	public static final Item TUNING_FORK = new TuningForkItem(new Item.Properties()
			.setId(net.minecraft.resources.ResourceKey.create(
					net.minecraft.core.registries.Registries.ITEM, ExtraEnchantry.id("tuning_fork")))
			.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));

	/** 创造栏顺序 */
	public static final List<Item> ALL = List.of(
			ECHO_CRYSTAL, TIDE_TEAR, SCULK_SILK, MEMORY_DUST, SILENCE_STONE, ABYSSAL_HEART,
			AETHER_DUST, HUSH_CLOAK, TIDEHEART_MASK, MEMORY_CASKET, TUNING_FORK);

	private AbyssResources() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("echo_crystal"), ECHO_CRYSTAL);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("tide_tear"), TIDE_TEAR);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("sculk_silk"), SCULK_SILK);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("memory_dust"), MEMORY_DUST);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("silence_stone"), SILENCE_STONE);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("abyssal_heart"), ABYSSAL_HEART);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("hush_cloak"), HUSH_CLOAK);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("tideheart_mask"), TIDEHEART_MASK);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("memory_casket"), MEMORY_CASKET);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("aether_dust"), AETHER_DUST);
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("tuning_fork"), TUNING_FORK);
	}
}
