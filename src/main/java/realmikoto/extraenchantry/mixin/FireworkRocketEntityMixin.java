package realmikoto.extraenchantry.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import realmikoto.extraenchantry.ExtraEnchantry;

/**
 * 御风（Windrider）II/III 级——烟花推进强化：
 * 26.2 反编译确认，烟花火箭在 {@code FireworkRocketEntity#tick} 中对**正在滑翔的实体**
 * 做一次朝视线方向的插值加速，随后调用 {@code LivingEntity#setDeltaMovement}
 * （该方法内仅此一处以 LivingEntity 为 owner 的调用，其余是火箭自身的移动）。
 * 这里重定向该调用：取出"本次推进增量"（新速度 - 旧速度）按倍率放大后写回，
 * 不改动原版的插值公式本身。
 *
 * 倍率：I 级 1.0（原版）、II 级 1.5、III 级 1.75（II 级效果再强化 50%）。
 */
@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin {

	@Redirect(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
			)
	)
	private void extraenchantry$windriderBoost(LivingEntity glider, Vec3 boosted) {
		double factor = ExtraEnchantry.getWindriderBoostFactor(glider);
		if (factor == 1.0D) {
			glider.setDeltaMovement(boosted);
			return;
		}
		Vec3 previous = glider.getDeltaMovement();
		Vec3 impulse = boosted.subtract(previous);
		glider.setDeltaMovement(previous.add(
				impulse.x * factor, impulse.y * factor, impulse.z * factor));
	}
}
