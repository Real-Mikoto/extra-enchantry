package realmikoto.extraenchantry.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import realmikoto.extraenchantry.ExtraEnchantry;

/**
 * 活力（Vitality）HUD 显示：
 * 生命值条过长会多行堆叠遮挡视野，故心形栏只显示基础生命（扣除活力加成后的上限），
 * 活力新增生命改在盔甲行上方左对齐以"心图标×n/N"紧凑显示。
 * 26.2 HUD 为渲染状态提取架构：Hud.extractPlayerHealth 计算行数并提取贴图/文本元素。
 */
@Mixin(Hud.class)
public abstract class HudMixin {

	/** 本帧盔甲图标行的 y（由 extractArmor 捕获，armorIconY = y - (rows-1)*rowHeight - 10） */
	@Unique
	private static int extraenchantry$armorIconY = Integer.MIN_VALUE;

	@Shadow
	private Player getCameraPlayer() {
		throw new AssertionError();
	}

	/**
	 * 修复 #30：客户端活力加成与服务端一致——共鸣 +1 级是服务端快照数据，客户端
	 * getVitalityBonus 算不出，导致心形行数与 ×n/N 分母偏小。
	 * 改为从服务端同步的 MAX_HEALTH 属性反推加成：bonus = ceil(syncedMax) - ceil(本地计算的基础+加成前值)。
	 * 服务端瞬态修改器经属性同步到达客户端，本地 getAttributeValue 已含真实加成；
	 * getVitalityBonus 本地值只包含附魔基础部分，差值即为漏掉的共鸣加成。
	 */
	@Unique
	private static int extraenchantry$syncedVitalityBonus(Player player) {
		int local = ExtraEnchantry.getVitalityBonus(player);
		float syncedMax = (float) player.getAttributeValue(Attributes.MAX_HEALTH);
		float baseMax = syncedMax - local;
		int extra = Mth.ceil(syncedMax) - Mth.ceil(baseMax);
		return Math.max(local, Math.max(0, local + extra));
	}

	/**
	 * 计算心形行数时从 MAX_HEALTH 中扣除活力加成，
	 * 使血条保持原版单行（10 颗心），额外生命由下方 TAIL 注入单独显示。
	 */
	@Redirect(
			method = "extractPlayerHealth",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D"
			)
	)
	private double extraenchantry$hideVitalityFromHeartBar(Player player, Holder<Attribute> attribute) {
		double value = player.getAttributeValue(attribute);
		if (Attributes.MAX_HEALTH.equals(attribute)) {
			value -= extraenchantry$syncedVitalityBonus(player);
		}
		return value;
	}

	/**
	 * 心形栏的血量也要钳制到基础上限：extractHearts 的红心绘制条件是 i×2 < health、
	 * 循环上限含 displayHealth = ceil(health/2)——若不钳制，满血 50HP 会直接画出
	 * 25 颗红心（3 行），伤害吸收（黄心）时行数变化会使该问题显形。
	 * 钳制后：红心/黑底心/黄心均按基础生命行数绘制，活力部分只由 TAIL 标签显示。
	 */
	@Redirect(
			method = "extractPlayerHealth",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Player;getHealth()F"
			)
	)
	private float extraenchantry$clampHealthForHeartBar(Player player) {
		float health = player.getHealth();
		float baseMax = player.getMaxHealth() - extraenchantry$syncedVitalityBonus(player);
		return Math.min(health, baseMax);
	}

	/**
	 * 捕获盔甲图标行的实际 y（含心行数上移补偿）：
	 * extractArmor(extractor, player, y, rows, rowHeight, x) 中盔甲行 y = y - (rows-1)*rowHeight - 10。
	 * 心行增多（如伤害吸收黄心）时盔甲行自动上移，活力行锚定其上，永不错位重叠。
	 */
	@Inject(method = "extractArmor", at = @At("HEAD"))
	private static void extraenchantry$captureArmorRow(GuiGraphicsExtractor extractor, Player player,
			int y, int rows, int rowHeight, int x, CallbackInfo ci) {
		extraenchantry$armorIconY = y - (rows - 1) * rowHeight - 10;
	}

	/**
	 * 在盔甲图标行上方（固定锚定）左对齐绘制"❤×n/N"：
	 * N = 盔甲活力提供的最大❤；n = 当前活力剩余❤（总血量超出基础上限的部分），
	 * 两者均取半心粒度（整数 HP 向上取整），避免浮点伤害残差产生长小数。
	 * 心形与血条一致（container 黑底 + full 叠加）；
	 * 文字颜色必须带 alpha（ARGB，26.2 text() 对 alpha==0 直接丢弃不渲染）。
	 */
	@Inject(method = "extractPlayerHealth", at = @At("TAIL"))
	private void extraenchantry$renderVitalityHearts(GuiGraphicsExtractor extractor, CallbackInfo ci) {
		Player player = this.getCameraPlayer();
		if (player == null) {
			return;
		}
		int bonus = extraenchantry$syncedVitalityBonus(player);
		if (bonus <= 0 || extraenchantry$armorIconY == Integer.MIN_VALUE) {
			return;
		}
		// 活力部分位于血量池顶端：总血量 - 基础上限 = 活力剩余，钳制到 [0, bonus]；
		// 伤害吸收（absorption）不计入 getHealth，不影响该值
		float vitalityHealth = Mth.clamp(player.getHealth() - (player.getMaxHealth() - bonus), 0.0F, bonus);
		// 半心粒度：整数 HP 向上取整后除 2（与原版心形显示的 ceil 一致），27.9HP→14❤，27HP→13.5❤
		float currentHearts = Mth.ceil(vitalityHealth) / 2.0F;
		int maxHearts = bonus / 2;
		String current = currentHearts == Mth.floor(currentHearts)
				? String.valueOf((int) currentHearts)
				: String.valueOf(currentHearts);
		int x = extractor.guiWidth() / 2 - 91;
		int y = extraenchantry$armorIconY - 10;
		extractor.blitSprite(RenderPipelines.GUI_TEXTURED,
				Identifier.withDefaultNamespace("hud/heart/container"), x, y, 9, 9);
		extractor.blitSprite(RenderPipelines.GUI_TEXTURED,
				Identifier.withDefaultNamespace("hud/heart/full"), x, y, 9, 9);
		Font font = Minecraft.getInstance().font;
		extractor.text(font, "×" + current + "/" + maxHearts, x + 11, y, 0xFFFFFFFF);
	}
}
