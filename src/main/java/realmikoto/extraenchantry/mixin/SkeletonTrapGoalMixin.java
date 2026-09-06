package realmikoto.extraenchantry.mixin;

import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.equine.SkeletonTrapGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import realmikoto.extraenchantry.CavalryManager;

/**
 * 原版骷髅马陷阱触发挂钩：26.2 反编译确认，陷阱生成逻辑集中在
 * {@code SkeletonTrapGoal#tick}（canUse = 玩家进入 10 格）。
 * 在 HEAD 取消原版生成并完全接管：CavalryManager 复刻原版的状态复位
 * （setTrap(false) 防重复触发）与视觉闪电，把 4 名铁甲骑士替换为
 * 较弱的骷髅马/僵尸马混编骑兵队（详见 CavalryManager#onVanillaTrapTriggered）。
 * CavalryManager 内部以陷阱马 UUID 去重，防御性兜底重复 tick。
 */
@Mixin(SkeletonTrapGoal.class)
public abstract class SkeletonTrapGoalMixin {

	@Shadow
	@Final
	private SkeletonHorse horse;

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void extraenchantry$onVanillaTrapTriggered(CallbackInfo ci) {
		CavalryManager.onVanillaTrapTriggered(this.horse);
		ci.cancel();
	}
}
