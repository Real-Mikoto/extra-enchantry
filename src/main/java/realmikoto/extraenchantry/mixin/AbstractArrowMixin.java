package realmikoto.extraenchantry.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Invoker;
import realmikoto.extraenchantry.HomingPlumeAccess;
import realmikoto.extraenchantry.HomingPlumeManager;

/**
 * 归羽（Homing Plume）：发射时快照的等级挂在箭实体上，
 * 命中实体置标记（命中不返还），插地方（未命中）时按等级概率
 * 登记延迟返还（I 级 50% / II 级 100%）。
 *
 * 26.2 反编译确认：AbstractArrow 已移至 projectile.arrow 分包；
 * onHitEntity / onHitBlock / pickup 字段（ALLOWED/DISALLOWED/CREATIVE_ONLY）
 * 为本实现提供的全部钩子。
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin implements HomingPlumeAccess {

	@Shadow
	public AbstractArrow.Pickup pickup;

	/** 发射时写入的归羽等级快照（0 = 无归羽） */
	@Unique
	private int extraenchantry$homingLevel;

	/** 是否命中过实体（命中的箭不返还） */
	@Unique
	private boolean extraenchantry$hitEntity;

	/** 发射点坐标（百步穿杨挑战测距用；null = 未记录） */
	@Unique
	private double[] extraenchantry$launchPos;

	@Override
	public void extraenchantry$setHomingLevel(int level) {
		this.extraenchantry$homingLevel = level;
	}

	@Override
	public int extraenchantry$getHomingLevel() {
		return this.extraenchantry$homingLevel;
	}

	@Override
	public void extraenchantry$setLaunchPos(double x, double y, double z) {
		this.extraenchantry$launchPos = new double[]{x, y, z};
	}

	@Override
	public double[] extraenchantry$getLaunchPos() {
		return this.extraenchantry$launchPos;
	}

	/** 透传 protected 的 getPickupItem（@Invoker 标准做法） */
	@Invoker("getPickupItem")
	@Override
	public abstract ItemStack extraenchantry$getPickupItem();

	@Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
	private void extraenchantry$voidblinkDodge(EntityHitResult hitResult, CallbackInfo ci) {
		AbstractArrow self = (AbstractArrow) (Object) this;
		if (!(hitResult.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
			return;
		}
		int level = realmikoto.extraenchantry.VoidblinkManager.blinkLevel(player);
		if (level <= 0) {
			return;
		}
		if (!realmikoto.extraenchantry.VoidblinkManager.tryBlink(player, level)) {
			return; // 内冷却中或概率未命中
		}
		// 闪避成立：取消本次命中（伤害 / 撞击 / 归羽登记全部跳过）+ 侧移瞬移 + 反馈
		this.extraenchantry$hitEntity = true; // 归羽复用：闪避的箭不登记返还（§5.4 设计意图）
		ci.cancel();
		realmikoto.extraenchantry.VoidblinkManager.dodgeFx(player);
	}

	@Inject(method = "onHitEntity", at = @At("HEAD"))
	private void extraenchantry$markEntityHit(EntityHitResult hitResult, CallbackInfo ci) {
		this.extraenchantry$hitEntity = true;
		// 隐秘挑战「百步穿杨」：归羽箭命中距发射点 ≥40 格的生物
		if (this.extraenchantry$homingLevel > 0 && this.extraenchantry$launchPos != null) {
			AbstractArrow self = (AbstractArrow) (Object) this;
			double dx = self.getX() - this.extraenchantry$launchPos[0];
			double dy = self.getY() - this.extraenchantry$launchPos[1];
			double dz = self.getZ() - this.extraenchantry$launchPos[2];
			if (dx * dx + dy * dy + dz * dz >= 40.0D * 40.0D
					&& self.getOwner() instanceof net.minecraft.server.level.ServerPlayer owner) {
				realmikoto.extraenchantry.FamilyResonanceManager.onHomingLongShot(owner);
			}
		}
	}

	@Inject(method = "onHitBlock", at = @At("RETURN"))
	private void extraenchantry$scheduleReturn(BlockHitResult hitResult, CallbackInfo ci) {
		if (this.extraenchantry$homingLevel <= 0 || this.extraenchantry$hitEntity
				|| this.pickup == AbstractArrow.Pickup.DISALLOWED) {
			return;
		}
		AbstractArrow self = (AbstractArrow) (Object) this;
		if (!(self.getOwner() instanceof ServerPlayer owner)) {
			return;
		}
		double chance = this.extraenchantry$homingLevel >= 2 ? 1.0D : 0.5D;
		if (owner.getRandom().nextDouble() < chance) {
			HomingPlumeManager.schedule(self, owner);
			// 归羽起飞反馈（P1）：声音提前到起飞（1 秒飞回过程，声音先行更有期待感）+ 末地烛轨迹
			if (self.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
				realmikoto.extraenchantry.FxHelper.burst(serverLevel, self,
						net.minecraft.core.particles.ParticleTypes.END_ROD, 3, 0.1D);
				realmikoto.extraenchantry.FxHelper.play(serverLevel, self,
						net.minecraft.sounds.SoundEvents.ITEM_PICKUP, 0.3F, 1.4F);
			}
		}
	}
}
