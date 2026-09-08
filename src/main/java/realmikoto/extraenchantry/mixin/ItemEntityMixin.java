package realmikoto.extraenchantry.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import realmikoto.extraenchantry.ExtraEnchantry;

/**
 * 火焰家族烧毁免疫（1.4.0 新增设定「烬火不侵」）：
 * 带火焰家族附魔的装备 / 烬心石配饰作为掉落物时，免疫火焰与岩浆造成的
 * 伤害（物品着火损耗走 ItemEntity#hurtServer，免疫即不会被烧毁——下界合金物品同款行为）。
 * 判定为动态 {@link ExtraEnchantry#isFireFamilyItem}，砂轮磨掉附魔即时失效。
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void extraenchantry$fireFamilyBurnImmune(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> cir) {
		if (amount <= 0.0F || !source.is(DamageTypeTags.IS_FIRE)) {
			return;
		}
		ItemStack stack = ((ItemEntity) (Object) this).getItem();
		if (ExtraEnchantry.isFireFamilyItem(stack)) {
			cir.setReturnValue(false);   // 烬火不侵：不掉耐久、不烧毁
		}
	}
}
