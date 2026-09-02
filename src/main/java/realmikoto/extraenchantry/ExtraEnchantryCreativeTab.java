package realmikoto.extraenchantry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;

/**
 * 创造模式物品栏：本 mod 全部附魔的附魔书单列一页。
 * displayItems 回调在打开物品栏时执行，届时数据包已加载，
 * 可通过 ItemDisplayParameters.holders() 查询附魔注册表获取 Holder。
 */
public final class ExtraEnchantryCreativeTab {

	private record EnchantmentEntry(ResourceKey<Enchantment> key, int maxLevel) {
	}

	/** 本 mod 全部附魔及其最大等级 */
	private static final List<EnchantmentEntry> ENCHANTMENTS = List.of(
			new EnchantmentEntry(ExtraEnchantry.WITHER_PROTECTION, 4),
			new EnchantmentEntry(ExtraEnchantry.BLAZING_WALKER, 2),
			new EnchantmentEntry(ExtraEnchantry.LIMIT_BREAK, 1),
			new EnchantmentEntry(ExtraEnchantry.TIER_BREAK, 3),
			new EnchantmentEntry(ExtraEnchantry.REACH, 10),
			new EnchantmentEntry(ExtraEnchantry.DECOY, 3),
			new EnchantmentEntry(ExtraEnchantry.SIPHON, 3),
			new EnchantmentEntry(ExtraEnchantry.LIFE_EROSION, 3),
			new EnchantmentEntry(ExtraEnchantry.VITALITY, 5),
			new EnchantmentEntry(ExtraEnchantry.BULWARK, 10),
			new EnchantmentEntry(ExtraEnchantry.AFTERGLOW, 1),
			new EnchantmentEntry(ExtraEnchantry.OATHBOUND, 1),
			new EnchantmentEntry(ExtraEnchantry.SKYWARD, 2),
			new EnchantmentEntry(ExtraEnchantry.CLEAVE, 3),
			new EnchantmentEntry(ExtraEnchantry.WINDRIDER, 3),
			new EnchantmentEntry(ExtraEnchantry.UNSEEN, 2),
			new EnchantmentEntry(ExtraEnchantry.JUDGEMENT, 2),
			new EnchantmentEntry(ExtraEnchantry.GALE, 3),
			new EnchantmentEntry(ExtraEnchantry.EMBERFALL, 1)
	);

	public static final ResourceKey<CreativeModeTab> TAB_KEY =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, ExtraEnchantry.id("enchantments"));

	public static void register() {
		CreativeModeTab tab = FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.extra-enchantry.enchantments"))
				.icon(() -> new ItemStack(Items.ENCHANTED_BOOK))
				.displayItems((parameters, output) -> {
					for (EnchantmentEntry entry : ENCHANTMENTS) {
						Holder<Enchantment> holder = parameters.holders()
								.lookupOrThrow(Registries.ENCHANTMENT)
								.getOrThrow(entry.key());
						for (int level = 1; level <= entry.maxLevel(); level++) {
							output.accept(createEnchantedBook(holder, level));
						}
					}
				})
				.build();
		net.minecraft.core.Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, tab);
	}

	/** 构造指定等级的附魔书（STORED_ENCHANTMENTS 组件） */
	private static ItemStack createEnchantedBook(Holder<Enchantment> enchantment, int level) {
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		stored.set(enchantment, level);
		book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
		return book;
	}

	private ExtraEnchantryCreativeTab() {
	}
}
