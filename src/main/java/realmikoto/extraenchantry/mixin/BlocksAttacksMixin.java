package realmikoto.extraenchantry.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import realmikoto.extraenchantry.ExtraEnchantry;

/**
 * 不屈（Defiance）——26.2 的盾牌机制已组件化（{@code DataComponents.BLOCKS_ATTACKS}），
 * 破盾与耐久消耗都在 {@code BlocksAttacks} 上结算，注入组件方法即覆盖所有盾牌：
 *
 * 1. 不屈 II：完全免疫破盾——拦截 {@code disable}（26.2 破盾 = 给盾牌上物品冷却，
 *    时长来自攻击者武器组件的 disableBlockingForSeconds，斧为 5 秒）；
 * 2. 不屈 I：破盾冷却时长减半（5s→2.5s），并获得等长的抗性提升 I 作为补偿；
 * 3. 不屈 II：格挡耐久消耗 ×2（{@code hurtBlockingItem} 的 damage 参数翻倍，
 *    经 ItemDamageFunction 换算后耐久消耗翻倍）。
 *
 * 两个 handler 按等级互斥（I 只走减半、II 只走免疫+双倍），注入顺序无影响。
 */
@Mixin(BlocksAttacks.class)
public abstract class BlocksAttacksMixin {

	/** 不屈 II：完全免疫破盾 */
	@Inject(method = "disable", at = @At("HEAD"), cancellable = true)
	private void extraenchantry$defianceImmunity(ServerLevel level, LivingEntity user, float baseSeconds,
			ItemStack blockingWith, CallbackInfo ci) {
		if (ExtraEnchantry.getDefianceLevel(blockingWith) >= 2) {
			ci.cancel();
		}
	}

	/**
	 * 不屈 I：破盾时长减半 + 等长抗性提升 I。
	 * 踩坑：@ModifyVariable 的 handler 参数 = [被修改变量] + [目标方法全部参数按原顺序]，
	 * 被修改变量不在参数末尾时，其原位参数会再出现一次——
	 * disable(ServerLevel, LivingEntity, float, ItemStack) 的 handler 必须是
	 * (float, ServerLevel, LivingEntity, float, ItemStack)。
	 */
	@ModifyVariable(method = "disable", at = @At("HEAD"), argsOnly = true)
	private float extraenchantry$defianceHalve(float baseSeconds, ServerLevel level, LivingEntity user,
			float originalSeconds, ItemStack blockingWith) {
		if (ExtraEnchantry.getDefianceLevel(blockingWith) == 1 && baseSeconds > 0.0F) {
			user.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
					Math.round(baseSeconds * 10.0F), 0));
			return baseSeconds * 0.5F;
		}
		return baseSeconds;
	}

	/** 不屈 II：格挡耐久消耗 ×2（变量在参数末尾，handler = 变量 + 其余参数） */
	@ModifyVariable(method = "hurtBlockingItem", at = @At("HEAD"), argsOnly = true)
	private float extraenchantry$defianceDoubleDurability(float damage, Level level, ItemStack item) {
		if (damage > 0.0F && ExtraEnchantry.getDefianceLevel(item) >= 2) {
			return damage * 2.0F;
		}
		return damage;
	}
}
