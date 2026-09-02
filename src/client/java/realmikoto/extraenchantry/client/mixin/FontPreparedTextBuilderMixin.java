package realmikoto.extraenchantry.client.mixin;

import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import realmikoto.extraenchantry.ExtraEnchantry;

/**
 * 破限 / 拓阶 附魔名称的波浪式金色闪光：
 * 文本渲染时（每帧重新 prepare），对 fancy 字体样式的文字逐字符计算颜色：
 * 色相在金色邻域（33°~57°）随时间与字符位置呈正弦波动，亮度同步脉动，
 * 形成"流光溢彩"的波浪闪光效果。
 */
@Mixin(targets = "net.minecraft.client.gui.Font$PreparedTextBuilder")
public abstract class FontPreparedTextBuilderMixin {

	@Redirect(
			method = "accept(ILnet/minecraft/network/chat/Style;Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;)Z",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/network/chat/Style;getColor()Lnet/minecraft/network/chat/TextColor;"
			)
	)
	private TextColor extraenchantry$waveShine(Style style, int index, Style styleArg, BakedGlyph glyph) {
		TextColor original = style.getColor();

		// 仅对 extra-enchantry:fancy 字体样式生效（附魔名专用标记）
		FontDescription font = style.getFont();
		if (!(font instanceof FontDescription.Resource resource) || !resource.id().equals(ExtraEnchantry.id("fancy"))) {
			return original;
		}

		// 波浪相位：时间驱动 + 字符索引偏移（形成沿文字方向传播的波浪）
		float time = (System.currentTimeMillis() % 4000L) / 1000.0F;
		float phase = time * (float) (Math.PI * 2.0) * 1.5F + index * 0.7F;

		// 金色邻域色相波动：33° ~ 57°（橙金 ↔ 柠檬金）
		float hue = (45.0F + 12.0F * Mth.sin(phase)) / 360.0F;
		// 亮度脉动：0.62 ~ 1.0（闪光感）
		float brightness = 0.81F + 0.19F * Mth.sin(phase * 2.0F);

		return TextColor.fromRgb(Mth.hsvToRgb(hue, 1.0F, brightness));
	}
}
