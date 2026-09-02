package realmikoto.extraenchantry.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DeathProtection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import realmikoto.extraenchantry.AfterglowManager;
import realmikoto.extraenchantry.ExtraEnchantry;

/**
 * 劫后余辉（Afterglow）触发点：
 * 不死图腾生效时，原版 LivingEntity#checkTotemDeathProtection 会
 * ① copy 一份图腾栈 ② 消耗原栈（shrink 1）③ setHealth(1)
 * ④ 调用 {@code deathProtection.applyEffects(stackCopy, this)}。
 *
 * 挂在 applyEffects 的 RETURN 上有两个必要理由：
 *   - 参数是图腾栈的副本（附魔信息完整），而原栈此时已被消耗，读不到附魔
 *   - 第一个死亡效果是 ClearAllStatusEffects，注入点必须晚于它，否则效果被清空
 *
 * 消耗原栈发生在注入点之前，因此"效果随图腾一起消失"由原版机制天然保证。
 */
@Mixin(DeathProtection.class)
public abstract class DeathProtectionMixin {

	@Inject(method = "applyEffects", at = @At("RETURN"))
	private void extraenchantry$triggerAfterglow(ItemStack stack, LivingEntity entity, CallbackInfo ci) {
		if (!(entity instanceof Player player) || ExtraEnchantry.getAfterglowLevel(stack) <= 0) {
			return;
		}
		AfterglowManager.trigger(player);
	}
}
