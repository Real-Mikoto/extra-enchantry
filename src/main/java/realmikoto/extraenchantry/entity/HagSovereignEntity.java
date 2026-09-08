package realmikoto.extraenchantry.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import realmikoto.extraenchantry.EncounterDef;
import realmikoto.extraenchantry.EliteLord;
import realmikoto.extraenchantry.LordRuntime;

/**
 * 沼泽境领主 · 巫后（Hag Sovereign）—— 复用原版 WITCH 实体类型（零实体注册），
 * 沼泽女巫部落的统治者（1.5.0 §4）。主题：药水、咒术与巫术的盛宴。
 *
 * 属性倍率见设计 §4.4（生命 ×12.3 / 药水伤害 15 / 移速 +40% / 索敌 +50% / 体型 1.35×；
 * 保留 85% 药水抗性——强制物理输出）。
 *
 * 技能（4）：
 *   1 魔药暴雨：3 s 内投掷 9 瓶随机药水（伤害 / 中毒 / 缓慢 / 虚弱，0.3 s 一瓶）；
 *   2 迷雾遮蔽：自身 8 格绿色迷雾 6 s，雾内非女巫系生物致盲 I + 移速 −20%；
 *   3 咒术反噬：对攻击者施加 4 s 虚弱 II + 4 s 缓慢 II（假象诱饵可代吃）；
 *   4 魔仆召唤：生成 2 只沼泽毒蛛（蜘蛛 scale 0.9、生命 24）。
 */
public class HagSovereignEntity extends Witch implements EliteLord {

	private static final int SLOT_DOWNPOUR = 0;
	private static final int SLOT_MIST = 1;
	private static final int SLOT_HEX = 2;
	private static final int SLAT_FAMILIAR = 3;

	/** 药水暴雨：每瓶间隔（tick，0.3 s）与总瓶数 */
	private static final int DOWNPOUR_INTERVAL = 6;
	private static final int DOWNPOUR_BOTTLES = 9;

	private final LordRuntime runtime;
	private int downpourBottles = 0;
	private int downpourTimer = 0;
	private LivingEntity downpourTarget;
	private int mistTicks = 0;

	public HagSovereignEntity(EntityType<? extends Witch> type, Level level) {
		super(type, level);
		this.runtime = new LordRuntime(EncounterDef.byId("hag"));
	}

	public HagSovereignEntity(ServerLevel level) {
		this(EntityTypes.WITCH, level);
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
			potionDownpour(level, target);
		}
		mistShroud(level);
		hexBounce(level);
		familiarCall(level);
		tickPending(level);
	}

	// ============ 技能 1：魔药暴雨 ============

	private void potionDownpour(ServerLevel level, LivingEntity target) {
		if (!runtime.ready(SLOT_DOWNPOUR) || !LordRuntime.inRange(this, target, 20.0)) {
			return;
		}
		runtime.cooldown(SLOT_DOWNPOUR, 200); // 10 s
		downpourBottles = DOWNPOUR_BOTTLES;
		downpourTimer = DOWNPOUR_INTERVAL;
		downpourTarget = target;
	}

	// ============ 技能 2：迷雾遮蔽 ============

	private void mistShroud(ServerLevel level) {
		if (!runtime.ready(SLOT_MIST)) {
			return;
		}
		runtime.cooldown(SLOT_MIST, 320); // 16 s
		mistTicks = 120; // 6 s
		LordRuntime.ring(level, position(), 8.0D, 30, ParticleTypes.WITCH);
		LordRuntime.sound(level, this, net.minecraft.sounds.SoundEvents.WITCH_AMBIENT, 1.0F);
	}

	// ============ 技能 3：咒术反噬 ============

	private void hexBounce(ServerLevel level) {
		if (!runtime.ready(SLOT_HEX)) {
			return;
		}
		LivingEntity attacker = getLastHurtByMob();
		if (attacker == null || !attacker.isAlive()) {
			return;
		}
		runtime.cooldown(SLOT_HEX, 240); // 12 s
		attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1, true, false));
		attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1, true, false));
		LordRuntime.ring(level, attacker.position(), 2.0D, 16, ParticleTypes.WITCH);
	}

	// ============ 技能 4：魔仆召唤 ============

	private void familiarCall(ServerLevel level) {
		if (!runtime.ready(SLAT_FAMILIAR)) {
			return;
		}
		runtime.cooldown(SLAT_FAMILIAR, 560); // 28 s
		for (int i = 0; i < 2; i++) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
			Vec3 pos = position().add(Math.cos(angle) * 4.0D, 0.0D, Math.sin(angle) * 4.0D);
			LordRuntime.spawnMinion(level, EntityTypes.SPIDER, pos, 0.9F, 24.0F);
			LordRuntime.column(level, pos, 1.2D, 8, ParticleTypes.WITCH);
		}
	}

	// ============ 延迟结算 ============

	private void tickPending(ServerLevel level) {
		if (mistTicks > 0) {
			mistTicks--;
			for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
					net.minecraft.world.phys.AABB.ofSize(position(), 16.0D, 16.0D, 16.0D))) {
				if (entity == this || entity instanceof Witch) {
					continue;
				}
				entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, true, false));
				entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1, true, false));
			}
			if (tickCount % 10 == 0) {
				LordRuntime.ring(level, position(), 8.0D, 24, ParticleTypes.WITCH);
			}
		}
		if (downpourBottles <= 0 || downpourTarget == null) {
			return;
		}
		downpourTimer--;
		if (downpourTimer > 0) {
			return;
		}
		downpourTimer = DOWNPOUR_INTERVAL;
		if (!downpourTarget.isAlive()) {
			downpourBottles = 0;
			downpourTarget = null;
			return;
		}
		// 随机药水：伤害 / 中毒 / 缓慢 / 虚弱（弹道有弧线，横向移动可躲）
		int roll = level.getRandom().nextInt(4);
		switch (roll) {
			case 0 -> LordRuntime.magicHit(this, downpourTarget, 15.0F);
			case 1 -> downpourTarget.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, true, false));
			case 2 -> downpourTarget.addEffect(
					new MobEffectInstance(MobEffects.SLOWNESS, 100, 0, true, false));
			default -> downpourTarget.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, true, false));
		}
		LordRuntime.column(level, downpourTarget.position(), 1.5D, 6, ParticleTypes.WITCH);
		downpourBottles--;
		if (downpourBottles <= 0) {
			downpourTarget = null;
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
