package realmikoto.extraenchantry.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import realmikoto.extraenchantry.ExtraEnchantry;
import realmikoto.extraenchantry.HomingPlumeAccess;

/**
 * 归羽（Homing Plume）发射快照：
 * 26.2 反编译确认，弓与弩的箭矢都经
 * {@code ProjectileWeaponItem#createProjectile(Level, LivingEntity, ItemStack weapon, ItemStack projectile, boolean)}
 * 生成（弩的 createProjectile 重写仅处理烟花火箭，箭矢走 super）。
 * 在 RETURN 处把武器上的归羽等级写入箭实体——此后发射者换武器也不影响本次快照。
 */
@Mixin(ProjectileWeaponItem.class)
public abstract class ProjectileWeaponItemMixin {

	@Inject(method = "createProjectile", at = @At("RETURN"))
	private void extraenchantry$snapshotHomingPlume(Level level, LivingEntity shooter, ItemStack weapon,
			ItemStack projectile, boolean crit, CallbackInfoReturnable<Projectile> cir) {
		if (!(cir.getReturnValue() instanceof AbstractArrow arrow)) {
			return;
		}
		int homingLevel = ExtraEnchantry.getHomingPlumeLevel(weapon);
		if (homingLevel > 0) {
			((HomingPlumeAccess) arrow).extraenchantry$setHomingLevel(homingLevel);
		}
	}
}
