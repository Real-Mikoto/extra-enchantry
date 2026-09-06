package realmikoto.extraenchantry.mixin;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import realmikoto.extraenchantry.ExtraEnchantry;

/**
 * 破限 / 拓阶 附魔名称特殊渲染：
 * 亮金色 (#FFD700) 基础色 + extra-enchantry:fancy / fancy_lb 字体（凯尔特/篆书风格）。
 * 拓阶用 fancy；破限用 fancy_lb——客户端 FontPreparedTextBuilderMixin 据此区分二者：
 * 破限在本地玩家完成「无敌」进度前按锁定态渲染（灰色无闪光），完成后再变金色波浪闪光。
 */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {

	@Inject(method = "getFullname", at = @At("RETURN"), cancellable = true)
	private static void extraenchantry$fancyEnchantmentName(Holder<Enchantment> enchanted, int level,
			CallbackInfoReturnable<Component> cir) {
		Identifier fontId = enchanted.is(ExtraEnchantry.LIMIT_BREAK)
				? ExtraEnchantry.id("fancy_lb")
				: ExtraEnchantry.id("fancy");
		if (enchanted.is(ExtraEnchantry.LIMIT_BREAK) || enchanted.is(ExtraEnchantry.TIER_BREAK)) {
			Style fancy = Style.EMPTY
					.withColor(0xFFD700)
					.withFont(new FontDescription.Resource(fontId))
					.withItalic(false);
			// 用带样式的空父组件包裹原名，子组件继承样式
			MutableComponent styled = Component.empty().withStyle(fancy).append(cir.getReturnValue());
			cir.setReturnValue(styled);
		}
	}
}
