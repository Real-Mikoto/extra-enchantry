package realmikoto.extraenchantry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

/**
 * 霆霓（Stormsurge）：三叉戟投掷专属的环境增伤。
 *
 * 效果（I/II 级）：雨天 / 雷雨天 / 目标在水中时，掷出的三叉戟命中
 * 额外 +2/+4 伤害，并连锁至 2 格内最近的 1 个其他实体（连锁伤害减半）。
 * 仅投掷触发（!source.isDirect()），近战戳刺不触发；与引雷互斥。
 *
 * 防递归：连锁共用主目标的 DamageSource（武器快照仍是三叉戟），
 * 连锁命中的 hurtServer 会再次进入本管理器——用 ThreadLocal 标记位短路，
 * 与破阵（CleaveManager）同一模式。
 */
public final class StormsurgeManager {

	/** 各等级的主目标额外伤害（下标 = 等级 - 1） */
	private static final float[] BONUS_DAMAGE = {2.0F, 4.0F};

	/** 连锁目标选取范围：主目标碰撞箱外扩格数 */
	private static final double CHAIN_RANGE = 2.0D;

	private static final ThreadLocal<Boolean> CHAINING = new ThreadLocal<>();

	private StormsurgeManager() {
	}

	/** 当前是否正在执行霆霓连锁（hurtServer 注入点据此短路） */
	public static boolean isChaining() {
		return Boolean.TRUE.equals(CHAINING.get());
	}

	/**
	 * 霆霓入口（挂在 LivingEntity#hurtServer 入参 amount 的 @ModifyVariable，
	 * 定义在蚀命/冲阵之后、断罪之前——霆霓加伤参与断罪斩杀阈值结算）。
	 */
	public static float applyBonus(LivingEntity victim, ServerLevel level, DamageSource source, float amount) {
		if (amount <= 0.0F || source.isDirect() || isChaining() || source.getEntity() == victim) {
			return amount;
		}
		if (!(source.getEntity() instanceof LivingEntity attacker)) {
			return amount;
		}
		int enchantLevel = ExtraEnchantry.getStormsurgeLevel(source.getWeaponItem());
		if (enchantLevel <= 0 || enchantLevel > BONUS_DAMAGE.length) {
			return amount;
		}
		// 环境门：降雨（含雷雨）或目标在水中
		if (!level.isRaining() && !victim.isInWater()) {
			return amount;
		}
		float bonus = BONUS_DAMAGE[enchantLevel - 1];
		chainLightning(victim, level, source, attacker, bonus * 0.5F);
		spawnEffects(victim, level);
		return amount + bonus;
	}

	/** 连锁：2 格内最近的 1 个其他实体承受减半伤害（排除攻击者/主目标/友方/死者/旁观者） */
	private static void chainLightning(LivingEntity victim, ServerLevel level, DamageSource source,
			LivingEntity attacker, float chainDamage) {
		AABB searchBox = victim.getBoundingBox().inflate(CHAIN_RANGE);
		List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox,
				entity -> entity != victim && entity != attacker
						&& entity.isAlive() && !entity.isSpectator()
						&& !entity.isAlliedTo(attacker));
		if (candidates.isEmpty()) {
			return;
		}
		candidates.sort(Comparator.comparingDouble(target -> target.distanceToSqr(victim)));
		LivingEntity chained = candidates.get(0);
		CHAINING.set(Boolean.TRUE);
		try {
			chained.hurt(source, chainDamage);
		} finally {
			CHAINING.remove();
		}
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				chained.getX(), chained.getY(0.5D), chained.getZ(),
				12, 0.3D, 0.4D, 0.3D, 0.05D);
		// 实战成就「雷霆万钧」：霆霓首次感电连锁命中第二目标
		if (attacker instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			Advancements.award(serverPlayer, Advancements.THUNDER_CHAIN);
		}
	}

	/** 霆霓视听：主目标身上电弧粒子 + 引雷同款雷声（仅附近可闻，无真实闪电） */
	private static void spawnEffects(LivingEntity victim, ServerLevel level) {
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
				victim.getX(), victim.getY(0.5D), victim.getZ(),
				20, 0.4D, 0.5D, 0.4D, 0.08D);
		// 性能修复：雷声节流（60 tick）——旧实现每次命中音量 2.0 全功率，雨天连掷三叉戟即音效轰炸
		if (FxHelper.throttle(victim, "stormsurge_thunder", 60)) {
			level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
					SoundEvents.TRIDENT_THUNDER, SoundSource.WEATHER, 1.0F, 1.4F);
		}
	}
}
