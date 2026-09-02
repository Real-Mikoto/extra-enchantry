package realmikoto.extraenchantry.client.mixin;

import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import realmikoto.extraenchantry.ExtraEnchantry;

/**
 * 空跃（Skyward）空中跳跃——客户端侧：
 *
 * 26.2 反编译确认：玩家的跳跃输入是客户端权威（原版玩家跳跃也由客户端
 * aiStep 发起，服务端仅按位置包接受结果），服务端看不到跳跃键状态，
 * 多段跳必须在客户端注入。挂在 {@code LocalPlayer#aiStep} 的 HEAD：
 * 原版本体稍后会读取 input.keyPresses 并执行地面跳跃，时序天然对齐。
 *
 * - 按键沿检测（本 tick 按下、上 tick 未按）才触发，按住空格不会连烧次数；
 * - 触地 / 进水 / 攀爬立即重置次数；骑乘、鞘翅滑翔、创造飞行时不触发；
 * - 跳跃本体直接复用 {@code jumpFromGround()}（26.2 为 public，含跳跃力度与
 *   疾跑加跳），手感与原版跳一致；
 * - 次数上限 = 靴子空跃等级（I → 1 次，II → 2 次）。
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

	/** 剩余空中跳跃次数 */
	@Unique
	private int extraenchantry$skywardJumpsLeft = 0;

	/** 上一 tick 是否按住跳跃键（沿检测用） */
	@Unique
	private boolean extraenchantry$prevJumpPressed = false;

	@Inject(method = "aiStep", at = @At("HEAD"))
	private void extraenchantry$skywardAirJump(CallbackInfo ci) {
		LocalPlayer self = (LocalPlayer) (Object) this;
		ClientInput input = self.input;
		boolean jumpPressed = input != null && input.keyPresses.jump();
		int maxJumps = ExtraEnchantry.getSkywardLevel(self.getItemBySlot(EquipmentSlot.FEET));
		if (maxJumps <= 0) {
			extraenchantry$skywardJumpsLeft = 0;
			extraenchantry$prevJumpPressed = jumpPressed;
			return;
		}
		if (self.onGround() || self.isInWater() || self.onClimbable()) {
			// 落地 / 入水 / 攀爬即重置次数
			extraenchantry$skywardJumpsLeft = maxJumps;
		} else if (jumpPressed && !extraenchantry$prevJumpPressed
				&& extraenchantry$skywardJumpsLeft > 0
				&& !self.isPassenger()
				&& !self.isFallFlying()
				&& !self.getAbilities().flying) {
			extraenchantry$skywardJumpsLeft--;
			self.jumpFromGround();
		}
		// 装备等级变化（脱靴 / 换靴）时收敛剩余次数
		if (extraenchantry$skywardJumpsLeft > maxJumps) {
			extraenchantry$skywardJumpsLeft = maxJumps;
		}
		extraenchantry$prevJumpPressed = jumpPressed;
	}
}
