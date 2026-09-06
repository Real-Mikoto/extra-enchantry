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
import realmikoto.extraenchantry.client.CollectorState;
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

		// 臻藏奖励（1.2.0）：收藏家玩家的本模附魔名以专属金渐变渲染（覆盖族色）
		if (resource.id().getNamespace().equals("extra-enchantry")
				&& resource.id().getPath().startsWith("fancy")
				&& CollectorState.isCollector()) {
			return extraenchantry$collectorShine(index);
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
		// T1 主题族波浪（色相中心 / 振幅 / 饱和度 / 亮度基线各不相同，见 DESIGN_aesthetics 字体章）
		TextColor family = extraenchantry$familyShine(resource, index);
		return family != null ? family : original;
	}

	/** 金色邻域色相波动（33°~57°）+ 亮度脉动，沿文字方向传播的波浪 */
	private static TextColor extraenchantry$goldShine(int index) {
		return extraenchantry$shine(index, 45.0F, 12.0F, 1.0F, 0.81F, 0.19F);
	}

	/**
	 * 臻藏专属金渐变（1.2.0 收藏家奖励）：更宽的金域 + 饱和度呼吸（偶发近白高光），
	 * 与普通金色波浪一眼可分。
	 */
	private static TextColor extraenchantry$collectorShine(int index) {
		float time = (System.currentTimeMillis() % 6000L) / 1000.0F;
		float phase = time * (float) (Math.PI * 2.0) + index * 0.5F;
		float hue = (50.0F + 15.0F * Mth.sin(phase)) / 360.0F;
		float saturation = 0.55F + 0.25F * Mth.sin(phase * 1.3F);
		float brightness = 0.92F + 0.08F * Mth.sin(phase * 3.0F);
		return TextColor.fromRgb(Mth.hsvToRgb(hue, saturation, brightness));
	}

	/**
	 * 主题族波浪分发：按字体 ID 匹配族色相参数，未命中返回 null。
	 * 各族的设计意图：
	 * - 灵魂族（185°青蓝）：高饱和 + 深亮度波动，幽冥流转；
	 * - 雷光族（190°青白）：低饱和高亮度，接近白光的电弧感；
	 * - 锋刃族（230°银白）：极低饱和 + 剧烈亮度波动，金属寒光一闪一闪；
	 * - 自然族（120°翠绿）：中等饱和，平和的生命脉动；
	 * - 深渊族（220°深蓝）：高饱和低亮度，深海暗涌；
	 * - 风族（180°风白）：低饱和高亮度基线，近乎透明的气流；
	 * - 守护族（210°钢青）：中饱和低波动，沉稳的盾墙质感；
	 * - 火焰族（20°焰橙）：高饱和 + 快速闪烁（波动频率翻倍），火苗跳动。
	 */
	private static TextColor extraenchantry$familyShine(FontDescription.Resource resource, int index) {
		String path = resource.id().getPath();
		return switch (path) {
			case "fancy_soul" -> extraenchantry$shine(index, 185.0F, 10.0F, 0.85F, 0.70F, 0.25F);
			case "fancy_storm" -> extraenchantry$shine(index, 190.0F, 8.0F, 0.35F, 0.90F, 0.10F);
			case "fancy_blade" -> extraenchantry$shine(index, 230.0F, 8.0F, 0.15F, 0.75F, 0.25F);
			case "fancy_nature" -> extraenchantry$shine(index, 120.0F, 15.0F, 0.80F, 0.78F, 0.18F);
			case "fancy_water" -> extraenchantry$shine(index, 220.0F, 8.0F, 0.90F, 0.62F, 0.20F);
			case "fancy_wind" -> extraenchantry$shine(index, 180.0F, 10.0F, 0.25F, 0.92F, 0.08F);
			case "fancy_guard" -> extraenchantry$shine(index, 210.0F, 6.0F, 0.50F, 0.72F, 0.10F);
			case "fancy_fire" -> extraenchantry$shineFast(index, 20.0F, 12.0F, 0.95F, 0.80F, 0.20F);
			default -> null;
		};
	}

	/** 通用波浪：色相中心 ± 振幅 + 亮度基线 ± 亮度振幅，沿文字方向传播 */
	private static TextColor extraenchantry$shine(int index, float hueCenter, float hueRange,
			float saturation, float brightBase, float brightRange) {
		float time = (System.currentTimeMillis() % 4000L) / 1000.0F;
		float phase = time * (float) (Math.PI * 2.0) * 1.5F + index * 0.7F;
		float hue = (hueCenter + hueRange * Mth.sin(phase)) / 360.0F;
		float brightness = brightBase + brightRange * Mth.sin(phase * 2.0F);
		return TextColor.fromRgb(Mth.hsvToRgb(hue, saturation, brightness));
	}

	/** 快速波浪（火焰族专用）：波动频率翻倍，火苗跳动感 */
	private static TextColor extraenchantry$shineFast(int index, float hueCenter, float hueRange,
			float saturation, float brightBase, float brightRange) {
		float time = (System.currentTimeMillis() % 4000L) / 1000.0F;
		float phase = time * (float) (Math.PI * 2.0) * 3.0F + index * 1.1F;
		float hue = (hueCenter + hueRange * Mth.sin(phase)) / 360.0F;
		float brightness = brightBase + brightRange * Mth.sin(phase * 2.0F);
		return TextColor.fromRgb(Mth.hsvToRgb(hue, saturation, brightness));
	}
}
