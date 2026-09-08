package realmikoto.extraenchantry.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import realmikoto.extraenchantry.EliteEncounterManager;

/**
 * 深暗境·守望者触发钩子（1.5.0 §1.2 / §7.2）。
 *
 * {@code Warden#increaseAngerAt} 是监守者愤怒增加的统一入口（诸界浩劫补怒同一入口，
 * 技术路线已验证）。此处只做两件事：
 *   1. 通报"该玩家被监守者愤怒锁定"（隐藏进度「无声狩猎」的反向判定依据）；
 *   2. 不改动任何愤怒数值——持续 30 s 的判定在 EliteEncounterManager#scanWardenAnger 按 tick 采样。
 */
@Mixin(Warden.class)
public abstract class WardenMixin {

	@Inject(method = "increaseAngerAt(Lnet/minecraft/world/entity/Entity;IZ)V", at = @At("HEAD"))
	private void extraenchantry$noteAnger(Entity target, int offset, boolean bypassCooldown,
			CallbackInfo ci) {
		if (target instanceof ServerPlayer player && !player.level().isClientSide()) {
			EliteEncounterManager.noteWardenAnger(player);
		}
	}
}
