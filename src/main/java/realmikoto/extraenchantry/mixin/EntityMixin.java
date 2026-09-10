package realmikoto.extraenchantry.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
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
import realmikoto.extraenchantry.TideheartAir;

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

	/**
	 * 渊息（Tideheart）I~III 级——氧气上限提升：
	 * 26.2 反编译确认，氧气上限就是 {@code Entity#getMaxAirSupply()} 硬编码返回 300（15 秒），
	 * 且 increaseAirSupply 以它为钳制上限——单点 HEAD 注入放大后，
	 * 消耗、水面换气回满、客户端气泡 HUD 全部自动跟随。
	 * 每级 +300 tick（+15 秒）：I/II/III 级 → 30/45/60 秒。
	 *
	 * 构造时序陷阱（26.2 实测堆栈）：{@code Entity#<init>} 的 defineSyncker
	 * 在第 322 行就回调 getMaxAirSupply()（定义 DATA_AIR_SUPPLY_ID 的初值），
	 * 而 {@code LivingEntity#equipment} 字段要到子类构造体才初始化——
	 * 此时 instanceof LivingEntity 已为真但 getItemBySlot 必然 NPE，
	 * 实体构造直接失败（新世界/登录时 "Couldn't place player in world"）。
	 * 防御：装备未就绪视为无渊息，走原版上限；实体构造完成后调用路径全部正常。
	 */
	@Inject(method = "getMaxAirSupply", at = @At("HEAD"), cancellable = true)
	private void extraenchantry$tideheartMaxAir(CallbackInfoReturnable<Integer> cir) {
		if ((Object) this instanceof LivingEntity living) {
			int level;
			try {
				level = ExtraEnchantry.getTideheartLevel(living.getItemBySlot(EquipmentSlot.HEAD));
			} catch (NullPointerException e) {
				return;
			}
			if (level > 0) {
				cir.setReturnValue(TideheartAir.airCap(level));
			}
		}
	}

	/**
	 * 挑战坐骑豁免"入水甩下骑手"（1.7.3 修复骑兵异常下马）。
	 *
	 * <p>26.2 反编译确认：{@code Entity#dismountsUnderwater()} 唯一判据是
	 * {@code is(EntityTypeTags.DISMOUNTS_UNDERWATER)}，而僵尸马/骷髅马都在该标签内；
	 * {@code LivingEntity#baseTick} 一旦发现载具满足该条件就 {@code stopRiding()}。
	 * 骑兵若生成在水面、或被寻路带入水中，骑手会被瞬间甩下（玩家看到的"异常下马"）。
	 * 挑战坐骑（CavalryManager 登记）在此返回 false，保持骑乘关系。</p>
	 */
	@Inject(method = "dismountsUnderwater", at = @At("HEAD"), cancellable = true)
	private void extraenchantry$challengeMountKeepsRider(CallbackInfoReturnable<Boolean> cir) {
		if (realmikoto.extraenchantry.CavalryManager.isChallengeMount((Entity) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
