package realmikoto.extraenchantry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 藏锋（Sheathed Edge）：剑 / 斧专属的拔刀一击。
 *
 * 效果（I/II/III 级）：脱离战斗（未造成且未承受伤害）满 5 秒后，
 * 首次近战命中额外 +2/+4/+6 伤害，伴随拔刀音效与横向刀光粒子；
 * 触发后重新计时。与断罪不互斥——藏锋加伤参与断罪斩杀阈值结算
 * （与冲阵同一设计逻辑，注入点定义在断罪之前）。
 *
 * 计时基准：每个生物 UUID 记录最近一次"参与战斗"（造成或承受伤害）的
 * 时间戳（服务端 wall-clock，与断罪冷却同一风格）；无记录视为就绪
 * （开局第一刀即拔刀斩）。
 */
public final class SheathedEdgeManager {

	/** 各等级的拔刀额外伤害（下标 = 等级 - 1） */
	private static final float[] BONUS_DAMAGE = {2.0F, 4.0F, 6.0F};

	/** 脱战判定时长（毫秒）：5 秒 */
	private static final long SHEATH_MS = 5000L;

	/** 生物 UUID → 最近一次参与战斗的时间戳（服务端单线程访问） */
	private static final Map<UUID, Long> LAST_COMBAT_MS = new HashMap<>();

	private SheathedEdgeManager() {
	}

	/**
	 * 记账：每次 hurtServer 实际生效后调用（受害者与攻击者双方都记）。
	 * 任何伤害（含摔落/燃烧）都算"承受伤害"——脱战判定严格按字面执行。
	 */
	public static void recordCombat(LivingEntity entity) {
		LAST_COMBAT_MS.put(entity.getUUID(), System.currentTimeMillis());
	}

	/**
	 * 藏锋入口（挂在 LivingEntity#hurtServer 入参 amount 的 @ModifyVariable，
	 * 定义在冲阵之后、断罪之前）。
	 */
	public static float applyBonus(LivingEntity victim, ServerLevel level, DamageSource source, float amount) {
		if (amount <= 0.0F || !source.isDirect() || CleaveManager.isCleaving()
				|| source.getEntity() == victim) {
			return amount;
		}
		if (!(source.getEntity() instanceof LivingEntity attacker)) {
			return amount;
		}
		int enchantLevel = ExtraEnchantry.getSheathedEdgeLevel(source.getWeaponItem());
		if (enchantLevel <= 0 || enchantLevel > BONUS_DAMAGE.length) {
			return amount;
		}
		long now = System.currentTimeMillis();
		Long last = LAST_COMBAT_MS.get(attacker.getUUID());
		if (last != null && now - last < SHEATH_MS) {
			return amount;
		}
		// 拔刀：立刻重新计时（本次命中即"造成伤害"，双记账合一）
		LAST_COMBAT_MS.put(attacker.getUUID(), now);
		spawnEffects(victim, level);
		// 实战成就「拔刀斩」：藏锋首次拔刀一击
		if (attacker instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			Advancements.award(serverPlayer, Advancements.DRAW_STRIKE);
		}
		return amount + BONUS_DAMAGE[enchantLevel - 1];
	}

	/** 拔刀视听：横扫刀光粒子 + 拔刀音效（横扫音提速升调模拟出鞘声） */
	private static void spawnEffects(LivingEntity victim, ServerLevel level) {
		level.sendParticles(ParticleTypes.SWEEP_ATTACK,
				victim.getX(), victim.getY(0.5D), victim.getZ(),
				3, 0.3D, 0.2D, 0.3D, 0.0D);
		level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.4F);
	}
}
