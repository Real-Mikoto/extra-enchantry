package realmikoto.extraenchantry.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import realmikoto.extraenchantry.EliteEncounterManager;

/**
 * 末地境·末影领主触发钩子（1.5.0 §5.2）。
 *
 * 末影珍珠使用入口：26.2 的 {@code ThrownEnderpearl(Level, LivingEntity, ItemStack)}
 * 构造函数即"玩家投出珍珠"的统一入口（反编译确认），在 TAIL 处计数一次即可。
 * 使用次数由 EliteEncounterManager 的 120 s 滑动窗口累计，达 10 次触发遭遇。
 */
@Mixin(ThrownEnderpearl.class)
public abstract class ThrownEnderpearlMixin {

	@Inject(method = "<init>(Lnet/minecraft/world/level/Level;"
			+ "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V",
			at = @At("TAIL"))
	private void extraenchantry$notePearlUse(Level level, LivingEntity owner, ItemStack stack,
			CallbackInfo ci) {
		if (owner instanceof ServerPlayer player && !level.isClientSide()) {
			EliteEncounterManager.notePearlUse(player);
		}
	}
}
