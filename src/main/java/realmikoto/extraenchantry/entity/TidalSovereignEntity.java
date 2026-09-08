package realmikoto.extraenchantry.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import realmikoto.extraenchantry.EncounterDef;
import realmikoto.extraenchantry.EliteLord;
import realmikoto.extraenchantry.LordRuntime;

/**
 * 海洋境领主 · 渊潮之主（Tidal Sovereign）—— 复用原版 ELDER_GUARDIAN 实体类型
 * （零实体注册），深海神殿守卫者的远古王者（1.5.0 §3）。主题：深海的主权与怒潮。
 *
 * 属性倍率见设计 §3.4（生命 ×7.5 / 激光 ×2.5 / 荆棘 ×3 / 水中移速 +20% / 索敌 ×2 / 体型 1.7×）。
 *
 * 技能（4）：
 *   1 激光连射：连续 3 道激光（每道 20，间隔 0.4 s），每道 1 s 蓄力光点前兆；
 *   2 水龙卷：10 格内目标被拉向自身（3 格 /s，持续 3 s）；
 *   3 守卫者潮：生成 3 只原版守卫者（生命 30）；
 *   4 深海黑暗：30 格内所有玩家 8 s 黑暗 II + 3 s 挖掘疲劳 II。
 */
public class TidalSovereignEntity extends ElderGuardian implements EliteLord {

	private static final int SLOT_LASER = 0;
	private static final int SLOT_VORTEX = 1;
	private static final int SLOT_TIDE = 2;
	private static final int SLOT_DUSK = 3;

	private final LordRuntime runtime;
	/** 激光连射剩余发数与间隔计时 */
	private int pendingShots = 0;
	private int shotInterval = 0;
	private LivingEntity laserTarget;
	private int vortexTicks = 0;

	public TidalSovereignEntity(EntityType<? extends ElderGuardian> type, Level level) {
		super(type, level);
		this.runtime = new LordRuntime(EncounterDef.byId("tidal"));
	}

	public TidalSovereignEntity(ServerLevel level) {
		this(EntityTypes.ELDER_GUARDIAN, level);
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
			laserBarrage(level, target);
			vortexPull(level, target);
		}
		guardianTide(level);
		abyssalDusk(level);
		tickPending(level);
	}

	// ============ 技能 1：激光连射 ============

	private void laserBarrage(ServerLevel level, LivingEntity target) {
		if (!runtime.ready(SLOT_LASER) || !LordRuntime.inRange(this, target, 24.0)) {
			return;
		}
		runtime.cooldown(SLOT_LASER, 120); // 6 s
		pendingShots = 3;
		shotInterval = 20; // 首发射前有 1 s 蓄力光点
		laserTarget = target;
	}

	// ============ 技能 2：水龙卷 ============

	private void vortexPull(ServerLevel level, LivingEntity target) {
		if (!runtime.ready(SLOT_VORTEX) || !LordRuntime.inRange(this, target, 10.0)) {
			return;
		}
		runtime.cooldown(SLOT_VORTEX, 300); // 15 s
		vortexTicks = 60; // 3 s
		LordRuntime.ring(level, position(), 10.0D, 40, ParticleTypes.BUBBLE);
		LordRuntime.sound(level, this, net.minecraft.sounds.SoundEvents.GUARDIAN_ATTACK, 0.7F);
	}

	// ============ 技能 3：守卫者潮 ============

	private void guardianTide(ServerLevel level) {
		if (!runtime.ready(SLOT_TIDE)) {
			return;
		}
		runtime.cooldown(SLOT_TIDE, 500); // 25 s
		for (int i = 0; i < 3; i++) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
			Vec3 pos = position().add(Math.cos(angle) * 6.0D, 0.0D, Math.sin(angle) * 6.0D);
			LordRuntime.spawnMinion(level, EntityTypes.GUARDIAN, pos, 1.0F, 30.0F);
			LordRuntime.column(level, pos, 2.0D, 10, ParticleTypes.BUBBLE);
		}
	}

	// ============ 技能 4：深海黑暗 ============

	private void abyssalDusk(ServerLevel level) {
		if (!runtime.ready(SLOT_DUSK)) {
			return;
		}
		runtime.cooldown(SLOT_DUSK, 400); // 20 s
		for (ServerPlayer player : level.getPlayers(p -> p.distanceTo(this) <= 30.0, Integer.MAX_VALUE)) {
			player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 1, true, false));
			player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 60, 1, true, false));
		}
		LordRuntime.ring(level, position(), 12.0D, 30, ParticleTypes.DRIPPING_WATER);
	}

	// ============ 延迟结算 ============

	private void tickPending(ServerLevel level) {
		if (vortexTicks > 0) {
			vortexTicks--;
			LivingEntity target = getTarget();
			if (target != null && target.isAlive() && target.distanceTo(this) <= 10.0) {
				Vec3 pull = position().subtract(target.position()).normalize().scale(0.15D); // ≈3 格/s
				target.setDeltaMovement(target.getDeltaMovement().add(pull));
			}
			if (tickCount % 5 == 0) {
				LordRuntime.ring(level, position(), 6.0D, 20, ParticleTypes.BUBBLE);
			}
		}
		if (pendingShots <= 0 || laserTarget == null) {
			return;
		}
		shotInterval--;
		if (shotInterval > 0) {
			// 蓄力前兆：目标处光点（看光点侧移即可躲）
			LordRuntime.column(level, laserTarget.position(), 1.5D, 4, ParticleTypes.END_ROD);
			return;
		}
		if (!laserTarget.isAlive()) {
			pendingShots = 0;
			laserTarget = null;
			return;
		}
		// 激光穿透护甲（原版守卫者激光特性）：走 indirectMagic
		laserTarget.hurt(level.damageSources().indirectMagic(this, this), 20.0F);
		LordRuntime.column(level, laserTarget.position(), 2.0D, 8, ParticleTypes.END_ROD);
		LordRuntime.sound(level, this, net.minecraft.sounds.SoundEvents.GUARDIAN_ATTACK, 1.2F);
		pendingShots--;
		shotInterval = 8; // 0.4 s
		if (pendingShots <= 0) {
			laserTarget = null;
		}
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
}
