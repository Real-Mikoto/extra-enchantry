package realmikoto.extraenchantry;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 冲阵（Shield Charge）：疾跑持盾近战撞击。
 *
 * 触发条件（三者同时满足）：攻击者为玩家、处于疾跑状态、主手或副手
 * 持有带冲阵的盾牌。命中时额外造成 4/6/8 伤害（I/II/III）并强力击退
 * （击退方向 = 攻击者 → 受击者，26.2 knockback(power, xd, zd, source, damage)
 * 的语义为"实体被推离 (xd, zd) 指向的击退源"，参照原版 blockedByItem 传参），
 * 每名攻击者 1 秒冷却。
 *
 * 伤害追加挂 LivingEntity#hurtServer 的 HEAD（定义在蚀命之后、断罪之前——
 * 冲阵加伤参与断罪斩杀阈值结算）。
 */
public final class ShieldChargeManager {

	/** 撞击冷却（毫秒）＝ 1 秒 */
	private static final long COOLDOWN_MS = 1000L;

	/** 各等级的额外撞击伤害（下标 = 等级 - 1） */
	private static final float[] BONUS_DAMAGE = {4.0F, 6.0F, 8.0F};

	/** 各等级的击退强度（原版近战击退约 0.4 档，冲阵显著更强） */
	private static final double[] KNOCKBACK_POWER = {0.9D, 1.1D, 1.3D};

	/** 攻击者 UUID → 上次撞击时刻 */
	private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();

	private ShieldChargeManager() {
	}

	/** 读取实体双手盾牌上的冲阵最高等级（无则 0） */
	public static int getChargeLevel(LivingEntity entity) {
		return Math.max(
				chargeOn(entity.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND)),
				chargeOn(entity.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND)));
	}

	private static int chargeOn(ItemStack stack) {
		return stack.is(Items.SHIELD) ? ExtraEnchantry.getShieldChargeLevel(stack) : 0;
	}

	/** 冲阵判定与结算：命中额外伤害 + 强力击退（冷却中/条件不满足则原样返回） */
	public static float applyCharge(LivingEntity victim, DamageSource source, float amount) {
		if (amount <= 0.0F || CleaveManager.isCleaving()) {
			return amount;
		}
		if (!(source.getEntity() instanceof Player attacker) || !attacker.isSprinting()) {
			return amount;
		}
		long now = System.currentTimeMillis();
		Long last = COOLDOWNS.get(attacker.getUUID());
		if (last != null && now - last < COOLDOWN_MS) {
			return amount;
		}
		int level = getChargeLevel(attacker);
		if (level <= 0 || level > BONUS_DAMAGE.length) {
			return amount;
		}
		COOLDOWNS.put(attacker.getUUID(), now);
		// 击退方向：攻击者 → 受击者（被击者被推离攻击者）
		victim.knockback(KNOCKBACK_POWER[level - 1],
				attacker.getX() - victim.getX(), attacker.getZ() - victim.getZ(), source, amount);
		return amount + BONUS_DAMAGE[level - 1];
	}
}
