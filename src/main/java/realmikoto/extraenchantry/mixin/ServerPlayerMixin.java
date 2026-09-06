package realmikoto.extraenchantry.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.damagesource.DamageSource;
import realmikoto.extraenchantry.CavalryManager;
import realmikoto.extraenchantry.OathboundManager;

/**
 * 誓约（Oathbound）的重生搬运（26.2 死亡重生链路，反编译确认）：
 *
 * {@code ServerPlayer#restoreFrom(oldPlayer, keepEverything)} 只在
 * keepEverything 为真（非死亡重生）、keepInventory 开启、或旧玩家为旁观者时
 * 才调用 {@code transferInventoryXpAndScore}（背包 + 经验 + 分数整体搬运）。
 * 死亡重生（keepEverything=false）且 keepInventory 关闭时什么都不搬。
 *
 * 誓约物品已由 PlayerMixin 在 dropEquipment 中提取并放回旧玩家背包，
 * 这里按覆盖情况补上搬运：
 * - 四件护甲全带誓约 → transferInventoryXpAndScore 全量搬运（经验掉落实体
 *   已在 LivingEntityMixin#extraenchantry$skipOathboundXpDrop 中按条件跳过，
 *   不会出现"掉经验球 + 保留经验"的重复）；
 * - 部分誓约 → 仅 replaceWith 搬运背包，经验照常掉落。
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

	/**
	 * private 方法 @Shadow 需要方法体（仅签名有意义，运行时被替换为目标实现）。
	 * 该方法声明于目标类 ServerPlayer 自身，@Shadow 可以解析
	 * （踩坑记录：@Shadow 只解析不到父类继承的成员）。
	 */
	@Shadow
	private void transferInventoryXpAndScore(Player player) {
		throw new AssertionError("Shadowed method body was not transformed");
	}

	/**
	 * 诸界浩劫：玩家死亡瞬间（die HEAD，背包掉落之前）结算挑战失败——
	 * 此刻背包尚在，破限附魔书可就地销毁；若等 tick 级检查，背包已掉落，书会留在地上。
	 */
	@Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"))
	private void extraenchantry$cataclysmFailOnDeath(DamageSource source, CallbackInfo ci) {
		CavalryManager.onPlayerDie((ServerPlayer) (Object) this);
	}

	@Inject(method = "restoreFrom", at = @At("TAIL"))
	private void extraenchantry$oathboundCarryOver(ServerPlayer oldPlayer, boolean keepEverything,
			CallbackInfo ci) {
		if (keepEverything || oldPlayer.isSpectator()) {
			return;
		}
		if (oldPlayer.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
			return;
		}
		if (!OathboundManager.hasAnyOathboundItem(oldPlayer)) {
			return;
		}
		if (OathboundManager.hasFullOathboundArmor(oldPlayer)) {
			this.transferInventoryXpAndScore(oldPlayer);
		} else {
			((Player) (Object) this).getInventory().replaceWith(oldPlayer.getInventory());
		}
	}
}
