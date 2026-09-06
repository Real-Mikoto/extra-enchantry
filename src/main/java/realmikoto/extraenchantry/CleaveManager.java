package realmikoto.extraenchantry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

/**
 * 破阵（Cleave）溅射管理：近战命中主目标后，对周围额外目标造成百分比伤害。
 *
 * 效果（I/II/III 级）：最多 1/2/3 个额外目标，各受到主目标伤害的 50%/60%/70%
 * （走完整护甲结算），按与主目标的距离由近到远选取，范围为主目标碰撞箱外扩 2 格。
 *
 * 视觉：每个溅射目标身上生成横扫攻击粒子（原版横扫之刃同款），
 * 主目标脚下生成一圈以 2 格为半径、沿圆周均布的暴击粒子范围指示。
 *
 * 设计取舍：
 *   - 溅射共用主目标的 DamageSource，但通过 ThreadLocal 标记位短路本 mod 在
 *     hurtServer 上的三个注入（破阵自身防递归、汲取与蚀命防滚雪球）——
 *     三处都在 hurtServer，统一开关最省心；
 *   - 排除攻击者本人、旁观者、友方（与原版横扫之刃的目标筛选一致）。
 */
public final class CleaveManager {

	/** 各等级的溅射伤害比例（下标 = 等级 - 1，v1.1.0 由 50/60/70% 下调，缩小与横扫之刃的差距） */
	private static final float[] SPLASH_RATIO = {0.45F, 0.55F, 0.65F};

	/** 溅射目标选取范围：主目标碰撞箱外扩格数 */
	private static final double SPLASH_RANGE = 2.0D;

	/** 范围指示圈沿圆周均布的粒子数 */
	private static final int RING_PARTICLES = 16;

	private static final ThreadLocal<Boolean> CLEAVING = new ThreadLocal<>();

	private CleaveManager() {
	}

	/** 当前是否正在执行破阵溅射（hurtServer 各注入点据此短路） */
	public static boolean isCleaving() {
		return Boolean.TRUE.equals(CLEAVING.get());
	}

	/**
	 * 破阵溅射入口（挂在 LivingEntity#hurtServer 的 RETURN）。
	 * 仅在伤害实际生效（succeeded）、近战直伤、武器带破阵时触发。
	 */
	public static void tryCleave(ServerLevel level, LivingEntity victim, DamageSource source,
			float amount, boolean succeeded) {
		if (!succeeded || isCleaving() || amount <= 0.0F || !source.isDirect()) {
			return;
		}
		if (!(source.getEntity() instanceof LivingEntity attacker)) {
			return;
		}
		int enchantLevel = ExtraEnchantry.getCleaveLevel(source.getWeaponItem());
		if (enchantLevel <= 0 || enchantLevel > SPLASH_RATIO.length) {
			return;
		}
		List<LivingEntity> targets = findTargets(level, victim, attacker, enchantLevel);
		if (targets.isEmpty()) {
			return;
		}
		float splashDamage = amount * SPLASH_RATIO[enchantLevel - 1];
		CLEAVING.set(Boolean.TRUE);
		try {
			for (LivingEntity target : targets) {
				target.hurt(source, splashDamage);
			}
		} finally {
			CLEAVING.remove();
		}
		spawnSplashEffects(level, victim, targets);
	}

	/**
	 * 破阵视觉（服务端生成，自动广播给附近玩家）：
	 * 每个溅射目标身体中心打一个横扫攻击粒子（原版横扫之刃同款，体现"扫到了谁"）；
	 * 主目标脚下画一圈 2 格半径的范围指示（暴击粒子沿圆周均布，体现攻击范围）。
	 */
	private static void spawnSplashEffects(ServerLevel level, LivingEntity victim, List<LivingEntity> targets) {
		for (LivingEntity target : targets) {
			level.sendParticles(ParticleTypes.SWEEP_ATTACK,
					target.getX(), target.getY(0.5D), target.getZ(),
					1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
		double ringY = victim.getY(0.2D);
		for (int i = 0; i < RING_PARTICLES; i++) {
			double angle = (Math.PI * 2.0D * i) / RING_PARTICLES;
			level.sendParticles(ParticleTypes.CRIT,
					victim.getX() + Math.cos(angle) * SPLASH_RANGE, ringY,
					victim.getZ() + Math.sin(angle) * SPLASH_RANGE,
					1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
	}

	/** 以主目标为中心选取溅射目标：按距离排序，排除主目标/攻击者/旁观者/友方/死者 */
	private static List<LivingEntity> findTargets(ServerLevel level, LivingEntity victim,
			LivingEntity attacker, int maxCount) {
		AABB searchBox = victim.getBoundingBox().inflate(SPLASH_RANGE);
		List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox,
				entity -> entity != victim && entity != attacker
						&& entity.isAlive() && !entity.isSpectator()
						&& !entity.isAlliedTo(attacker));
		candidates.sort(Comparator.comparingDouble(target -> target.distanceToSqr(victim)));
		if (candidates.size() > maxCount) {
			return candidates.subList(0, maxCount);
		}
		return candidates;
	}
}
