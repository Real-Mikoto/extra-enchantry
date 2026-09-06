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
import realmikoto.extraenchantry.client.LimitBreakLockState;

/**
 * 附魔名称的波浪式金色闪光（文本渲染时每帧逐字符计算颜色）：
 *
 * - 拓阶（extra-enchantry:fancy 字体）：恒定金色波浪闪光；
 * - 破限（extra-enchantry:fancy_lb 字体）：分锁定态——
 *   · 未完成「无敌」进度：锁定态渲染，暗灰色平色（无闪光），
 *     与相邻金色附魔名形成"这本书还锁着"的直观视觉对比；
 *   · 完成后：与拓阶同款金色波浪闪光（服务器触发挑战即认可，解锁即变色）。
 */
@Mixin(targets = "net.minecraft.client.gui.Font$PreparedTextBuilder")
public abstract class FontPreparedTextBuilderMixin {

	/** 锁定态的破限名颜色：暗灰 */
	private static final int LOCKED_COLOR = 0x8B8B8B;

	@Redirect(
			method = "accept(ILnet/minecraft/network/chat/Style;Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;)Z",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/network/chat/Style;getColor()Lnet/minecraft/network/chat/TextColor;"
			)
	)
	private TextColor extraenchantry$waveShine(Style style, int index, Style styleArg, BakedGlyph glyph) {
		TextColor original = style.getColor();

		// 仅对 extra-enchantry 自有字体样式生效（附魔名专用标记）
		FontDescription font = style.getFont();
		if (!(font instanceof FontDescription.Resource resource)) {
			return original;
		}

		if (resource.id().equals(ExtraEnchantry.id("fancy_lb"))) {
			// 破限：锁定态 = 暗灰平色；解锁后 = 金色波浪闪光
			if (LimitBreakLockState.isLocked()) {
				return TextColor.fromRgb(LOCKED_COLOR);
			}
			return extraenchantry$goldShine(index);
		}
		if (resource.id().equals(ExtraEnchantry.id("fancy"))) {
			return extraenchantry$goldShine(index);
		}
		return original;
	}

	/** 金色邻域色相波动（33°~57°）+ 亮度脉动，沿文字方向传播的波浪 */
	private static TextColor extraenchantry$goldShine(int index) {
		float time = (System.currentTimeMillis() % 4000L) / 1000.0F;
		float phase = time * (float) (Math.PI * 2.0) * 1.5F + index * 0.7F;
		float hue = (45.0F + 12.0F * Mth.sin(phase)) / 360.0F;
		float brightness = 0.81F + 0.19F * Mth.sin(phase * 2.0F);
		return TextColor.fromRgb(Mth.hsvToRgb(hue, 1.0F, brightness));
	}
}
