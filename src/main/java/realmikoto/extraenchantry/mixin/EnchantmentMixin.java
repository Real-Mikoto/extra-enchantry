package realmikoto.extraenchantry.mixin;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import realmikoto.extraenchantry.ExtraEnchantry;

import java.util.Map;

/**
 * 附魔名称的着色体系（DESIGN_aesthetics 字体章；特殊字体已移除，仅保留颜色 + 波浪动效）：
 *
 * T0 传说级 —— fancy / fancy_lb 字体标记 + 金色 (#FFD700)：
 *   破限 / 拓阶；客户端渲染为金色波浪闪光，破限另有锁定态（灰）。
 *   （fancy / fancy_lb 仍保留专属 TTF 字体——特殊字体仅对破限体系保留。）
 * T1 史诗级 —— 族字体标记 + 族色波浪（客户端 FontPreparedTextBuilderMixin 逐帧演算）：
 *   灵魂族：断罪 / 蚀命 / 余烬 / 劫后余辉 / 无踪
 *   雷光族：坠星 / 假象
 *   锋刃族：藏锋 / 触及
 *   自然族：汲取 / 庇护
 *   深渊族：渊息
 *   风族：御风 / 疾风
 *   守护族：壁垒 / 坚壁 / 不屈 / 誓约
 *   火焰族：炽焰行者
 *   注：族字体 JSON（fancy_soul 等）已改为 reference 引用原版 default 字体——
 *   字形与原版完全一致，但字体 ID 保留作为客户端波浪动效的识别标记。
 * T2 普通级（uncommon/common：凋零保护 / 活力 / 破阵 / 冲阵 / 霆霓 / 丰壤 / 空跃）保持原版样式，
 *   维持"稀有度视觉阶梯"——附魔列表里一眼分出高下。
 */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {

	/** T1 族字体标记与基础色（客户端波浪以此字体 ID 为标记匹配色相；字体字形=原版 default） */
	private record FamilyStyle(String fontPath, int baseColor) {
	}

	private static final Map<ResourceKey<Enchantment>, FamilyStyle> FAMILY_STYLES = Map.ofEntries(
			// 灵魂族（青蓝 #4FD8E8）
			Map.entry(ExtraEnchantry.JUDGEMENT, new FamilyStyle("fancy_soul", 0x4FD8E8)),
			Map.entry(ExtraEnchantry.LIFE_EROSION, new FamilyStyle("fancy_soul", 0x4FD8E8)),
			Map.entry(ExtraEnchantry.EMBERFALL, new FamilyStyle("fancy_soul", 0x4FD8E8)),
			Map.entry(ExtraEnchantry.AFTERGLOW, new FamilyStyle("fancy_soul", 0x4FD8E8)),
			Map.entry(ExtraEnchantry.UNSEEN, new FamilyStyle("fancy_soul", 0x4FD8E8)),
			// 雷光族（青白 #B8F4FF）
			Map.entry(ExtraEnchantry.STARFALL, new FamilyStyle("fancy_storm", 0xB8F4FF)),
			Map.entry(ExtraEnchantry.DECOY, new FamilyStyle("fancy_storm", 0xB8F4FF)),
			// 锋刃族（银白 #E8E8F0）
			Map.entry(ExtraEnchantry.SHEATHED_EDGE, new FamilyStyle("fancy_blade", 0xE8E8F0)),
			Map.entry(ExtraEnchantry.REACH, new FamilyStyle("fancy_blade", 0xE8E8F0)),
			// 自然族（翠绿 #6FE86F）
			Map.entry(ExtraEnchantry.SIPHON, new FamilyStyle("fancy_nature", 0x6FE86F)),
			Map.entry(ExtraEnchantry.SANCTUARY, new FamilyStyle("fancy_nature", 0x6FE86F)),
			// 深渊族（深蓝 #3F76E4）
			Map.entry(ExtraEnchantry.TIDEHEART, new FamilyStyle("fancy_water", 0x3F76E4)),
			// 风族（风白 #D8F0F0）
			Map.entry(ExtraEnchantry.WINDRIDER, new FamilyStyle("fancy_wind", 0xD8F0F0)),
			Map.entry(ExtraEnchantry.GALE, new FamilyStyle("fancy_wind", 0xD8F0F0)),
			Map.entry(ExtraEnchantry.HOMING_PLUME, new FamilyStyle("fancy_wind", 0xD8F0F0)),
			// 守护族（钢青 #7FA8C9）
			Map.entry(ExtraEnchantry.BULWARK, new FamilyStyle("fancy_guard", 0x7FA8C9)),
			Map.entry(ExtraEnchantry.AEGIS, new FamilyStyle("fancy_guard", 0x7FA8C9)),
			Map.entry(ExtraEnchantry.DEFIANCE, new FamilyStyle("fancy_guard", 0x7FA8C9)),
			Map.entry(ExtraEnchantry.OATHBOUND, new FamilyStyle("fancy_guard", 0x7FA8C9)),
			// 火焰族（焰橙 #FF7A2A）
			Map.entry(ExtraEnchantry.BLAZING_WALKER, new FamilyStyle("fancy_fire", 0xFF7A2A))
	);

	@Inject(method = "getFullname", at = @At("RETURN"), cancellable = true)
	private static void extraenchantry$fancyEnchantmentName(Holder<Enchantment> enchanted, int level,
			CallbackInfoReturnable<Component> cir) {
		// T0：破限 / 拓阶（金色 + 专属字体，破限带锁定态）
		if (enchanted.is(ExtraEnchantry.LIMIT_BREAK) || enchanted.is(ExtraEnchantry.TIER_BREAK)) {
			Identifier fontId = enchanted.is(ExtraEnchantry.LIMIT_BREAK)
					? ExtraEnchantry.id("fancy_lb")
					: ExtraEnchantry.id("fancy");
			cir.setReturnValue(wrap(cir.getReturnValue(), fontId, 0xFFD700));
			return;
		}
		// T1：主题族字体 + 族色（客户端按字体 ID 演算波浪）
		for (Map.Entry<ResourceKey<Enchantment>, FamilyStyle> entry : FAMILY_STYLES.entrySet()) {
			if (enchanted.is(entry.getKey())) {
				FamilyStyle family = entry.getValue();
				cir.setReturnValue(wrap(cir.getReturnValue(),
						ExtraEnchantry.id(family.fontPath()), family.baseColor()));
				return;
			}
		}
	}

	/** 用带样式的空父组件包裹原名，子组件继承样式（字体 + 基础色） */
	private static Component wrap(Component original, Identifier fontId, int baseColor) {
		Style fancy = Style.EMPTY
				.withColor(baseColor)
				.withFont(new FontDescription.Resource(fontId))
				.withItalic(false);
		return Component.empty().withStyle(fancy).append(original);
	}
}
