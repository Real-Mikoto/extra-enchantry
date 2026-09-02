package realmikoto.extraenchantry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 断罪（Judgement）斩杀管理：近战武器专属。
 *
 * 效果（I/II 级）：目标当前生命（含伤害吸收）≤ 最大生命的 10%/20% 时直接斩杀；
 * 对 boss（末影龙/凋灵/监守者）不斩杀，改为该次伤害 ×2；
 * 斩杀触发后攻击者进入 5 秒冷却，触发瞬间目标身上爆发灵魂粒子。
 *
 * 调用点：{@code LivingEntity#hurtServer} 入参 amount 的 @ModifyVariable，
 * 定义在蚀命加成之后 → 蚀命追加的百分比伤害也参与斩杀判定与结算。
 *
 * 与壁垒的关系：斩杀通过"把伤害放大到必死量"实现，仍会经过壁垒在
 * {@code getDamageAfterMagicAbsorb} 的上限钳制——即壁垒可以挡下断罪（有意的攻防克制）。
 *
 * 防滚雪球：破阵溅射期间（CleaveManager.isCleaving）不触发，避免一次挥砍连环斩杀。
 */
public final class JudgementManager {

	/** 各等级的斩杀生命阈值（占目标最大生命比例，下标 = 等级 - 1） */
	private static final float[] EXECUTE_THRESHOLD = {0.10F, 0.20F};

	/** 对 boss 的伤害倍率（不斩杀，改为双倍伤害） */
	private static final float BOSS_DAMAGE_MULTIPLIER = 2.0F;

	/** 斩杀冷却（毫秒）——按攻击者计 */
	private static final long COOLDOWN_MS = 5000L;

	/** 斩杀粒子数量 */
	private static final int EXECUTE_PARTICLES = 30;

	/** 攻击者 UUID → 上次斩杀时间戳（服务端单线程访问） */
	private static final Map<UUID, Long> LAST_EXECUTE_MS = new HashMap<>();

	private JudgementManager() {
	}

	/**
	 * 断罪入口：返回本次伤害应采用的值（不触发时原样返回 amount）。
	 */
	public static float applyJudgement(ServerLevel serverLevel, LivingEntity victim, DamageSource source, float amount) {
		if (amount <= 0.0F || CleaveManager.isCleaving() || source.getEntity() == victim) {
			return amount;
		}
		if (!(source.getEntity() instanceof LivingEntity attacker)) {
			return amount;
		}
		int enchantLevel = ExtraEnchantry.getJudgementLevel(source.getWeaponItem());
		if (enchantLevel <= 0 || enchantLevel > EXECUTE_THRESHOLD.length) {
			return amount;
		}
		// boss 不斩杀，改为双倍伤害（无冷却）
		if (ExtraEnchantry.isBossLike(victim)) {
			return amount * BOSS_DAMAGE_MULTIPLIER;
		}
		float current = victim.getHealth() + victim.getAbsorptionAmount();
		if (current > victim.getMaxHealth() * EXECUTE_THRESHOLD[enchantLevel - 1]) {
			return amount;
		}
		if (!tryConsumeCooldown(attacker)) {
			return amount; // 冷却中：不斩杀，按普通伤害结算
		}
		spawnExecuteEffects(serverLevel, victim);
		// 放大到必死量：足以穿透伤害吸收与护甲减免（壁垒仍会在减免后钳制，见类注释）
		return victim.getMaxHealth() * 4.0F + 100.0F;
	}

	/** 冷却判定并记账：距上次斩杀不足 5 秒则返回 false */
	private static boolean tryConsumeCooldown(LivingEntity attacker) {
		UUID id = attacker.getUUID();
		long now = System.currentTimeMillis();
		Long last = LAST_EXECUTE_MS.get(id);
		if (last != null && now - last < COOLDOWN_MS) {
			return false;
		}
		LAST_EXECUTE_MS.put(id, now);
		return true;
	}

	/** 斩杀视觉：目标身体中心爆发一圈灵魂粒子（服务端生成，自动广播附近玩家） */
	private static void spawnExecuteEffects(ServerLevel level, LivingEntity victim) {
		level.sendParticles(ParticleTypes.SOUL,
				victim.getX(), victim.getY(victim.getBbHeight() * 0.5D), victim.getZ(),
				EXECUTE_PARTICLES,
				victim.getBbWidth() * 0.5D, victim.getBbHeight() * 0.4D, victim.getBbWidth() * 0.5D, 0.03D);
	}
}
