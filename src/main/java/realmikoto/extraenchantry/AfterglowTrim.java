package realmikoto.extraenchantry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;

/**
 * 遗辉纹饰模板（1.7.0 §3.2，补 1.6.0 设计 §3.6 的心核第三去向）。
 *
 * 心核 + 境材料 ×2 合成模板（五条分境配方）；模板走原版 armor trim 机制
 * （26.2 SmithingTemplateItem.createArmorTrimTemplate 工厂 + trim_pattern 数据注册表），
 * 五境材料作纹饰材料上色（复用原版 TrimMaterial 的 ingredient 由模板的
 * 物品标签判定——默认材质集足够，不注册自定义 TrimMaterial）。
 */
public final class AfterglowTrim {

	/** 模板物品：附带 realm_id 无关——纹饰模板通用（材料区分色彩，模板单件） */
	public static final Item TEMPLATE = SmithingTemplateItem.createArmorTrimTemplate(
			new Item.Properties()
					.setId(ResourceKey.create(Registries.ITEM, ExtraEnchantry.id("afterglow_trim")))
					.stacksTo(2).rarity(Rarity.EPIC).fireResistant());

	private AfterglowTrim() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, ExtraEnchantry.id("afterglow_trim"), TEMPLATE);
		// TrimPattern 走数据注册表（data/<ns>/trim_pattern/afterglow.json），无需代码注册
	}
}
