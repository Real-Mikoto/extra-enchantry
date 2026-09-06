package realmikoto.extraenchantry.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import realmikoto.extraenchantry.ExtraEnchantry;
import realmikoto.extraenchantry.StarfallAccess;

/**
 * 坠星（Starfall）发射快照：
 * 26.2 反编译确认，弩发射烟花火箭走
 * {@code CrossbowItem#createProjectile} 的烟花分支
 * （new FireworkRocketEntity(level, 烟花栈, 射手, ...)），
 * 在 RETURN 处给火箭打坠星标记——爆炸时发射者可能已切换武器，必须快照。
 */
@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {

	@Inject(method = "createProjectile", at = @At("RETURN"))
	private void extraenchantry$markStarfallRocket(Level level, LivingEntity shooter, ItemStack weapon,
			ItemStack projectile, boolean crit, CallbackInfoReturnable<Projectile> cir) {
		if (cir.getReturnValue() instanceof FireworkRocketEntity rocket
				&& ExtraEnchantry.getStarfallLevel(weapon) > 0) {
			((StarfallAccess) rocket).extraenchantry$setStarfall();
		}
	}
}
