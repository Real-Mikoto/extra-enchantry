package realmikoto.extraenchantry.mixin;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import realmikoto.extraenchantry.EchoManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 声音捕获（§5.1「回响幽灵：会模仿玩家刚发出的声音」）：
 * 拦截服务端声音广播（ServerLevel#playSeededSound）——发出者是幽渊内的玩家时，
 * 记录到 EchoManager.LAST_SOUND，供回响幽灵 playback（模仿引诱）使用。
 */
@Mixin(ServerLevel.class)
public class ServerLevelMixin {

	@Inject(method = "playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;"
			+ "Lnet/minecraft/sounds/SoundSource;FFJ)V", at = @At("TAIL"))
	private void extraenchantry$capturePlayerSound(Entity except, double x, double y, double z,
			Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed,
			CallbackInfo ci) {
		if (except instanceof ServerPlayer player && sound != null) {
			EchoManager.noteSound(player, sound.value());
		}
	}
}
