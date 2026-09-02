package realmikoto.extraenchantry.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import realmikoto.extraenchantry.ExtraEnchantry;

/**
 * 无踪（Unseen）I 级——声音与震动屏蔽：
 * 26.2 反编译确认，行走的脚步声与 STEP 震动事件都由
 * {@code Entity#vibrationAndSoundEffectsFromBlock} 一个方法负责
 * （param3 控制播放脚步音、param4 控制发送 GameEvent.STEP）；
 * 落地的 HIT_GROUND 震动则在 {@code Entity#checkFallDamage} 内经 Level#gameEvent 发出。
 * 两处屏蔽后，幽匿感测体与监守者都收不到穿戴者的行走/落地震动。
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

	/**
	 * 脚步声 + STEP 震动一并屏蔽（该方法返回 false 即"未产生声音与震动"，
	 * 与原版在空气中/游泳时的返回一致，调用方无副作用）。
	 */
	@Inject(method = "vibrationAndSoundEffectsFromBlock", at = @At("HEAD"), cancellable = true)
	private void extraenchantry$unseenSilenceStep(BlockPos pos, BlockState state, boolean playSound,
			boolean sendEvent, Vec3 movement, CallbackInfoReturnable<Boolean> cir) {
		if (extraenchantry$wearsUnseen()) {
			cir.setReturnValue(false);
		}
	}

	/**
	 * 落地震动屏蔽：仅拦下 HIT_GROUND 的 gameEvent 发送，
	 * 摔落伤害、落地粒子与 Block#fallOn 行为全部保持原样。
	 */
	@Redirect(
			method = "checkFallDamage",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/Level;gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V"
			)
	)
	private void extraenchantry$unseenSilenceLanding(Level level, Holder<GameEvent> event, Vec3 pos,
			GameEvent.Context context) {
		if (extraenchantry$wearsUnseen()) {
			return;
		}
		level.gameEvent(event, pos, context);
	}

	/** 穿戴者（须为生物）靴子上是否带无踪 */
	@Unique
	private boolean extraenchantry$wearsUnseen() {
		return (Object) this instanceof LivingEntity living && ExtraEnchantry.getUnseenLevelOnFeet(living) > 0;
	}
}
