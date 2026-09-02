package realmikoto.extraenchantry.mixin;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import realmikoto.extraenchantry.ExtraEnchantry;

/**
 * 破限 / 拓阶 附魔名称特殊渲染：
 * 亮金色 (#FFD700) 基础色 + extra-enchantry:fancy 字体（凯尔特/篆书风格）。
 * 金色波浪闪光动画由客户端 FontPreparedTextBuilderMixin 在渲染时逐字符实现。
 */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {

	@Inject(method = "getFullname", at = @At("RETURN"), cancellable = true)
	private static void extraenchantry$fancyEnchantmentName(Holder<Enchantment> enchanted, int level,
			CallbackInfoReturnable<Component> cir) {
		if (enchanted.is(ExtraEnchantry.LIMIT_BREAK) || enchanted.is(ExtraEnchantry.TIER_BREAK)) {
			Style fancy = Style.EMPTY
					.withColor(0xFFD700)
					.withFont(new FontDescription.Resource(ExtraEnchantry.id("fancy")))
					.withItalic(false);
			// 用带样式的空父组件包裹原名，子组件继承样式
			MutableComponent styled = Component.empty().withStyle(fancy).append(cir.getReturnValue());
			cir.setReturnValue(styled);
		}
	}
}
