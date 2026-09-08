package realmikoto.extraenchantry.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import realmikoto.extraenchantry.AccessoryManager;
import realmikoto.extraenchantry.AfterglowManager;
import realmikoto.extraenchantry.EmberfallManager;
import realmikoto.extraenchantry.ExtraEnchantry;
import realmikoto.extraenchantry.LimitBreakManager;
import realmikoto.extraenchantry.OathboundManager;

import java.util.Map;

/**
 * 所有针对**玩家**扣血链路的注入都必须挂在这里，而不是 LivingEntityMixin：
 * `Player` 重写了 `actuallyHurt` 且**不调用 super**（自己完整实现了护甲/魔抗/
 * 吸收结算与 setHealth），挂在 LivingEntity 的 actuallyHurt 上的注入对玩家完全无效。
 *
 * 注意两点相反的路径：
 * - `Player#hurtServer` 仍然 super 到 `LivingEntity#hurtServer`，
 *   所以 LivingEntityMixin 里 hurtServer 的 @Inject/@ModifyVariable 对玩家依然有效；
 * - `Player#actuallyHurt` 内部对 `getDamageAfterMagicAbsorb` 是虚调用，
 *   派发到 LivingEntity 的唯一实现——壁垒钳制已注入在该方法的 RETURN
 *   （见 LivingEntityMixin#extraenchantry$bulwarkCapDamage），玩家自动覆盖，无需在此重复。
 *
 * 另：26.2 反编译确认，玩家死亡的全部物品掉落集中在
 * {@code Player#dropEquipment}（keepInventory 判断 → destroyVanishingCursedItems →
 * inventory.dropAll），父类 LivingEntity#dropEquipment 是空壳——
 * 誓约（Oathbound）的物品保留就挂在这一个方法上。
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

	/** dropEquipment HEAD 提取出的誓约物品快照（槽位索引 → 物品），TAIL 回插后置空 */
	@Unique
	private Map<Integer, ItemStack> extraenchantry$oathboundKept = null;

	/**
	 * 锁血：在 {@code Player#actuallyHurt} 的 HEAD 取消调用，
	 * 使锁血期间生命值（与伤害吸收心）完全不因伤害而降低；
	 * 击退、受伤音效、盔甲耐久等反馈仍照常发生（比整体取消 hurtServer 更贴近"锁血"手感）。
	 * 两个来源：劫后余辉（图腾触发后 10 秒）与余烬（金胸甲免死后前 5 秒）。
	 */
	@Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
	private void extraenchantry$afterglowLockHealth(ServerLevel level, DamageSource source, float amount,
			CallbackInfo ci) {
		Player player = (Player) (Object) this;
		if (AfterglowManager.isLocked(player) || EmberfallManager.isLocked(player)) {
			ci.cancel();
		}
	}

	/**
	 * 誓约（Oathbound）第一步——死亡掉落前提取：
	 * keepInventory 关闭时，把背包（含装备槽）中所有带誓约的物品先摘出来，
	 * 让随后的 destroyVanishingCursedItems（消失诅咒销毁）与 inventory.dropAll
	 * 都碰不到它们；keepInventory 开启时原版本全保留，无需处理。
	 */
	@Inject(method = "dropEquipment", at = @At("HEAD"))
	private void extraenchantry$extractOathbound(ServerLevel level, CallbackInfo ci) {
		if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
			return;
		}
		extraenchantry$oathboundKept = OathboundManager.extractOathbound((Player) (Object) this);
	}

	/**
	 * 配饰栏死亡掉落（1.4.0）：keepInventory 关闭时——非誓约配饰掉落尸体处、
	 * 誓约配饰暂存（restoreFrom 回插）、Attachment 清空。与背包掉落同点位同时机。
	 */
	@Inject(method = "dropEquipment", at = @At("HEAD"))
	private void extraenchantry$dropAccessories(ServerLevel level, CallbackInfo ci) {
		if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
			return;
		}
		AccessoryManager.onDropEquipment((Player) (Object) this);
	}

	/**
	 * 誓约第二步——掉落完成后原槽位放回：
	 * 物品随旧玩家对象进入重生流程，由 ServerPlayerMixin 在 restoreFrom 时
	 * 按誓约覆盖情况搬运到重生玩家（四件套连带经验与分数，部分誓约仅背包）。
	 */
	@Inject(method = "dropEquipment", at = @At("TAIL"))
	private void extraenchantry$restoreOathbound(ServerLevel level, CallbackInfo ci) {
		if (extraenchantry$oathboundKept == null) {
			return;
		}
		if (!extraenchantry$oathboundKept.isEmpty()) {
			OathboundManager.restoreOathbound((Player) (Object) this, extraenchantry$oathboundKept);
		}
		extraenchantry$oathboundKept = null;
	}

	/**
	 * 破限（Limit Break）隐藏进度判定：玩家获得物品的统一入口 {@code Player#addItem}
	 * （拾取、漏斗、合成、命令都会走到）。若拿到的是破限附魔书，授予对应隐藏进度，
	 * 并由 CavalryManager 召唤守护骑兵队（破限需击败骑兵队后方可使用）。
	 * 见 {@link LimitBreakManager#onItemObtained}。
	 */
	@Inject(method = "addItem", at = @At("HEAD"))
	private void extraenchantry$detectLimitBreakBook(ItemStack stack,
			CallbackInfoReturnable<Boolean> cir) {
		try {
			if ((Object) this instanceof ServerPlayer serverPlayer) {
				LimitBreakManager.onItemObtained(serverPlayer, stack);
			}
		} catch (Exception e) {
			ExtraEnchantry.LOGGER.error("[extra-enchantry] 破限书获取检测(Player#addItem)异常", e);
		}
	}
}
