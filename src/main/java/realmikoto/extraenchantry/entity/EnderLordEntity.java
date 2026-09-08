package realmikoto.extraenchantry.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import realmikoto.extraenchantry.EncounterDef;
import realmikoto.extraenchantry.EliteLord;
import realmikoto.extraenchantry.LordRuntime;

/**
 * 末地境领主 · 末影领主（Ender Lord）—— 复用原版 ENDERMAN 实体类型（零实体注册），
 * 外岛末影人的王（1.5.0 §5）。主题：虚空的秩序与惩戒。
 *
 * 属性倍率见设计 §5.4（生命 ×12.5 / 近战 ×2.9 / 移速 +33% / 索敌 ×2 / 体型 1.5×；
 * **保留怕水弱点**——水伤害 2 + 瞬移中断 + 攻击终止，水桶是核心反制装备）。
 *
 * 技能（4）：
 *   1 虚空折射：瞬移至目标背后 2 格立即攻击（20 伤害偷袭，0.5 s 紫色粒子前兆）；
 *   2 影分身：生成 3 个幻影分身（末影人、生命 1、一次 6 伤害后消失）；
 *   3 虚空之握：16 格内目标被拉至面前 3 格（泼水可中断）；
 *   4 末影螨潮：生成 5 只末影螨（生命 8、攻击 3）。
 */
public class EnderLordEntity extends EnderMan implements EliteLord {

	private static final int SLOT_REFRACTION = 0;
	private static final int SLOT_CLONES = 1;
	private static final int SLOT_GRIP = 2;
	private static final int SLOT_SWARM = 3;

	private final LordRuntime runtime;
	private int refractionWindup = 0;
	private LivingEntity refractionTarget;

	public EnderLordEntity(EntityType<? extends EnderMan> type, Level level) {
		super(type, level);
		this.runtime = new LordRuntime(EncounterDef.byId("ender"));
	}

	public EnderLordEntity(ServerLevel level) {
		this(EntityTypes.ENDERMAN, level);
	}

	@Override
	public LordRuntime lordRuntime() {
		return runtime;
	}

	@Override
	public boolean shouldBeSaved() {
		return LordRuntime.shouldSave();
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		runtime.serverTick(this, level);
		LivingEntity target = getTarget();
		if (target != null && target.isAlive()) {
			voidRefraction(level, target);
			voidGrip(level, target);
		}
		shadowClones(level);
		endermiteSwarm(level);
		tickPending(level);
	}

	// ============ 技能 1：虚空折射 ============

	private void voidRefraction(ServerLevel level, LivingEntity target) {
		if (!runtime.ready(SLOT_REFRACTION) || !LordRuntime.inRange(this, target, 24.0)) {
			return;
		}
		runtime.cooldown(SLOT_REFRACTION, 160); // 8 s
		refractionWindup = 10; // 0.5 s 紫色粒子前兆
		refractionTarget = target;
	}

	// ============ 技能 2：影分身 ============

	private void shadowClones(ServerLevel level) {
		if (!runtime.ready(SLOT_CLONES)) {
			return;
		}
		runtime.cooldown(SLOT_CLONES, 400); // 20 s
		for (int i = 0; i < 3; i++) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
			Vec3 pos = position().add(Math.cos(angle) * 3.0D, 0.0D, Math.sin(angle) * 3.0D);
			var clone = LordRuntime.spawnMinion(level, EntityTypes.ENDERMAN, pos, 1.0F, 1.0F);
			if (clone != null) {
				// 分身一击即灭（生命 1），真身有 boss 血条可辨认
				LordRuntime.column(level, pos, 1.8D, 10, ParticleTypes.PORTAL);
			}
		}
	}

	// ============ 技能 3：虚空之握 ============

	private void voidGrip(ServerLevel level, LivingEntity target) {
		if (!runtime.ready(SLOT_GRIP) || !LordRuntime.inRange(this, target, 16.0)) {
			return;
		}
		runtime.cooldown(SLOT_GRIP, 300); // 15 s
		Vec3 front = position().add(getLookAngle().normalize().scale(3.0D));
		target.setDeltaMovement(Vec3.ZERO);
		target.snapTo(front.x, front.y, front.z);
		LordRuntime.sound(level, this, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 0.9F);
		LordRuntime.column(level, front, 1.5D, 10, ParticleTypes.PORTAL);
	}

	// ============ 技能 4：末影螨潮 ============

	private void endermiteSwarm(ServerLevel level) {
		if (!runtime.ready(SLOT_SWARM)) {
			return;
		}
		runtime.cooldown(SLOT_SWARM, 600); // 30 s
		for (int i = 0; i < 5; i++) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
			Vec3 pos = position().add(Math.cos(angle) * 3.0D, 0.0D, Math.sin(angle) * 3.0D);
			LordRuntime.spawnMinion(level, EntityTypes.ENDERMITE, pos, 1.0F, 8.0F);
		}
	}

	// ============ 延迟结算 ============

	private void tickPending(ServerLevel level) {
		if (refractionWindup <= 0 || refractionTarget == null) {
			return;
		}
		LordRuntime.column(level, position(), 2.0D, 8, ParticleTypes.PORTAL);
		refractionWindup--;
		if (refractionWindup > 0) {
			return;
		}
		if (refractionTarget.isAlive()) {
			// 瞬移至目标背后 2 格（怕水弱点保留：水中时瞬移与攻击被原版机制中断）
			Vec3 behind = refractionTarget.position()
					.subtract(refractionTarget.getLookAngle().normalize().scale(2.0D));
			this.snapTo(behind.x, behind.y, behind.z);
			LordRuntime.meleeHit(this, refractionTarget, 20.0F);
			LordRuntime.sound(level, this, net.minecraft.sounds.SoundEvents.ENDERMAN_SCREAM, 1.0F);
		}
		refractionTarget = null;
	}

	// ============ 掉落 ============

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		super.dropCustomDeathLoot(level, source, recentlyHit);
		EncounterDef def = runtime.def();
		if (def != null) {
			realmikoto.extraenchantry.RealmTreasures.dropLordLoot(this, def, level);
		}
	}

	@Override
	protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean recentlyHit) {
		// 领主掉落全部走 dropCustomDeathLoot（设计 §9.2：不覆盖任何原版掉落表）
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		// 怕水弱点保留：受到水相关伤害时中断瞬移类技能（水桶是核心反制装备）
		if (source.is(net.minecraft.world.damagesource.DamageTypes.DROWN)
				|| source.is(net.minecraft.world.damagesource.DamageTypes.FREEZE)) {
			refractionWindup = 0;
			refractionTarget = null;
		}
		return super.hurtServer(level, source, amount);
	}
}
