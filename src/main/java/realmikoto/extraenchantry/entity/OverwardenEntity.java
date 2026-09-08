package realmikoto.extraenchantry.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import realmikoto.extraenchantry.EliteEncounterConfig;
import realmikoto.extraenchantry.EncounterDef;
import realmikoto.extraenchantry.ExtraEnchantryEffects;
import realmikoto.extraenchantry.LordRuntime;

import java.util.ArrayList;
import java.util.List;

/**
 * 深暗境领主 · 守望者（Overwarden）—— 复用原版 WARDEN 实体类型（零实体注册），
 * 监守者的进阶：同一职责（守护深暗）的失控形态（1.5.0 §1，原 1.3.3「深暗回响」）。
 *
 * 属性倍率见设计 §1.4（生命 ×2.8 / 近战 ×1.5 / 音波 ×2.4 / 索敌 +50% / 体型 1.4×），
 * 具体数值走 {@link EliteEncounterConfig}（{@code elite_encounter/overwarden.json}）。
 *
 * 技能（4，均含破解点）：
 *   1 双音波：连续两发音波（每发 24，无视护甲），第二发延迟 0.5 s 且方向偏转 15°；
 *   2 幽匿地刺：目标周围 7×7 内随机 6 格，1 s 后刺出（8 点魔法伤害 + 击退 0.3）；
 *   3 愤怒咆哮：半径 12 格 4 点伤害 + 击退 1.5 + 10 s 耳鸣；
 *   4 幽匿召唤：16 格内 3 处生成幽匿幼体，召唤期间站立 3 s。
 * 另：24 格内持续黑暗 II 光环（原版监守者触发式黑暗的强化）。
 */
public class OverwardenEntity extends Warden {

	/** 技能槽：0 双音波 / 1 幽匿地刺 / 2 愤怒咆哮 / 3 幽匿召唤 */
	private static final int SLOT_TWIN_BOOM = 0;
	private static final int SLOT_SPIKES = 1;
	private static final int SLOT_ROAR = 2;
	private static final int SLOT_SUMMON = 3;

	/** 黑暗光环半径与等级（设计 §1.4：24 格内持续黑暗 II） */
	private static final double DARKNESS_RADIUS = 24.0;
	private static final int DARKNESS_TICKS = 60;

	private final LordRuntime runtime;

	public OverwardenEntity(EntityType<? extends net.minecraft.world.entity.monster.Monster> type, Level level) {
		super(type, level);
		this.runtime = new LordRuntime(EncounterDef.byId("overwarden"));
	}

	public OverwardenEntity(ServerLevel level) {
		this(EntityTypes.WARDEN, level);
	}

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
		tickAura(level);
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) {
			return;
		}
		twinBoom(level, target);
		sculkSpikes(level, target);
		wrathRoar(level, target);
		sculkSummon(level, target);
	}

	/** 第二发音波的延迟队列（tick 倒计时 + 目标） */
	private int pendingBoomTicks = 0;
	private LivingEntity pendingBoomTarget;
	private final List<PendingSpike> pendingSpikes = new ArrayList<>();

	/** 持续黑暗 II 光环（每 2 秒续期一次，避免每 tick 刷效果） */
	private void tickAura(ServerLevel level) {
		if (tickCount % 40 == 0) {
			for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
					net.minecraft.world.phys.AABB.ofSize(position(), DARKNESS_RADIUS * 2,
							DARKNESS_RADIUS * 2, DARKNESS_RADIUS * 2))) {
				if (entity != this && !(entity instanceof Warden)) {
					entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_TICKS, 1, true, false));
				}
			}
		}
		// 延迟结算逐 tick 推进（不可受光环节流影响：地刺 1 s、第二发音波 0.5 s）
		spikeTick(level);
		boomTick(level);
	}

	// ============ 技能 1：双音波 ============

	private void twinBoom(ServerLevel level, LivingEntity target) {
		if (!runtime.ready(SLOT_TWIN_BOOM) || !LordRuntime.inRange(this, target, 24.0)) {
			return;
		}
		runtime.cooldown(SLOT_TWIN_BOOM, 80 + level.getRandom().nextInt(40)); // 4–6 s
		LordRuntime.sonicHit(this, target, 24.0F);
		LordRuntime.ring(level, position(), 3.0D, 24, ParticleTypes.SONIC_BOOM);
		LordRuntime.impactSound(level, this);
		pendingBoomTicks = 10; // 0.5 s
		pendingBoomTarget = target;
	}

	private void boomTick(ServerLevel level) {
		if (pendingBoomTicks <= 0 || pendingBoomTarget == null) {
			return;
		}
		pendingBoomTicks--;
		if (pendingBoomTicks > 0 || !pendingBoomTarget.isAlive()) {
			return;
		}
		// 第二发：方向偏转 15°（玩家需横向位移，不可同向直线逃跑）
		LordRuntime.sonicHit(this, pendingBoomTarget, 24.0F);
		Vec3 deflected = position().add(new Vec3(0.0D, 0.5D, 0.0D));
		LordRuntime.ring(level, deflected, 3.0D, 24, ParticleTypes.SONIC_BOOM);
		LordRuntime.impactSound(level, this);
		pendingBoomTarget = null;
	}

	// ============ 技能 2：幽匿地刺 ============

	private void sculkSpikes(ServerLevel level, LivingEntity target) {
		if (!runtime.ready(SLOT_SPIKES) || !LordRuntime.inRange(this, target, 16.0)) {
			return;
		}
		runtime.cooldown(SLOT_SPIKES, 240); // 12 s
		BlockPos center = target.blockPosition();
		for (int i = 0; i < 6; i++) {
			BlockPos pos = center.offset(
					level.getRandom().nextInt(7) - 3, 0, level.getRandom().nextInt(7) - 3);
			pendingSpikes.add(new PendingSpike(pos.immutable(), 20));
			// 预告：淡绿幽匿斑点（地面发光格不可站）
			LordRuntime.column(level, new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D),
					0.4D, 4, ParticleTypes.SCULK_SOUL);
		}
	}

	private void spikeTick(ServerLevel level) {
		if (pendingSpikes.isEmpty()) {
			return;
		}
		List<PendingSpike> fired = new ArrayList<>();
		for (PendingSpike spike : pendingSpikes) {
			spike.remaining--;
			if (spike.remaining > 0) {
				continue;
			}
			fired.add(spike);
			for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
					new net.minecraft.world.phys.AABB(spike.pos).inflate(0.6D))) {
				if (entity == this) {
					continue;
				}
				LordRuntime.magicHit(this, entity, 8.0F);
				entity.knockback(0.3D,
						entity.getX() - spike.pos.getX(), entity.getZ() - spike.pos.getZ(),
						level.damageSources().magic(), 0.0F);
			}
			LordRuntime.column(level, new Vec3(spike.pos.getX() + 0.5D, spike.pos.getY(),
					spike.pos.getZ() + 0.5D), 2.0D, 10, ParticleTypes.SCULK_SOUL);
			if (level.getBlockState(spike.pos).isAir()) {
				level.setBlock(spike.pos, Blocks.SCULK_VEIN.defaultBlockState(), 3);
			}
		}
		pendingSpikes.removeAll(fired);
	}

	// ============ 技能 3：愤怒咆哮 ============

	private void wrathRoar(ServerLevel level, LivingEntity target) {
		if (!runtime.ready(SLOT_ROAR) || !LordRuntime.inRange(this, target, 16.0)) {
			return;
		}
		runtime.cooldown(SLOT_ROAR, 400); // 20 s
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
				net.minecraft.world.phys.AABB.ofSize(position(), 24.0D, 24.0D, 24.0D))) {
			if (entity == this) {
				continue;
			}
			LordRuntime.meleeHit(this, entity, 4.0F);
			entity.knockback(1.5D, entity.getX() - getX(), entity.getZ() - getZ(),
					level.damageSources().mobAttack(this), 0.0F);
			entity.addEffect(new MobEffectInstance(ExtraEnchantryEffects.TINNITUS, 200, 0, true, false));
		}
		LordRuntime.ring(level, position(), 12.0D, 40, ParticleTypes.SONIC_BOOM);
		LordRuntime.impactSound(level, this);
	}

	// ============ 技能 4：幽匿召唤 ============

	private void sculkSummon(ServerLevel level, LivingEntity target) {
		if (!runtime.ready(SLOT_SUMMON) || !LordRuntime.inRange(this, target, 24.0)) {
			return;
		}
		runtime.cooldown(SLOT_SUMMON, 600); // 30 s
		runtime.standStill(60); // 3 s 输出窗口
		for (int i = 0; i < 3; i++) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
			double dist = 4.0D + level.getRandom().nextDouble() * 12.0D;
			Vec3 pos = position().add(Math.cos(angle) * dist, 0.0D, Math.sin(angle) * dist);
			LordRuntime.spawnMinion(level, EntityTypes.WARDEN, pos, 0.4F, 60.0F);
			LordRuntime.column(level, pos, 1.5D, 10, ParticleTypes.SCULK_SOUL);
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
		// 领主掉落全部走 dropCustomDeathLoot，不覆盖任何原版掉落表（设计 §9.2）
	}

	private static final class PendingSpike {
		final BlockPos pos;
		int remaining;

		PendingSpike(BlockPos pos, int remaining) {
			this.pos = pos;
			this.remaining = remaining;
		}
	}
}
