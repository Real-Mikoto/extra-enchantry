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
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import realmikoto.extraenchantry.EliteEncounterConfig;
import realmikoto.extraenchantry.EliteLord;
import realmikoto.extraenchantry.EncounterDef;
import realmikoto.extraenchantry.LordRuntime;

import java.util.ArrayList;
import java.util.List;

/**
 * 下界境领主 · 烬骨王（Emberbone King）—— 复用原版 WITHER_SKELETON 实体类型（零实体注册），
 * 下界战死的凋灵骷髅残魂在灵魂火中重新站立（1.5.0 §2）。主题：凋零与火焰的复仇。
 *
 * 属性倍率见设计 §2.4（生命 ×20 / 近战 ×3 / 移速 +29% / 索敌 +50% / 体型 1.6×）。
 *
 * 技能（4）：
 *   1 熔岩吐息：面前 5×8 锥形 12 伤害 + 5 s 燃烧，地面留 2 个岩浆方块（4 s 后冷却为黑曜石）；
 *   2 凋零光环：常驻，8 格内生物每 2 s 获得 2 s 凋零 I；
 *   3 烬魂召唤：16 格内生成 2 只烬魂（烈焰人 scale 0.8、生命 30），召唤期间蓄力 2 s；
 *   4 灵魂火墙：目标与自身之间直线铺 7 格灵魂火（8 s 后消失），穿行持续受伤。
 */
public class EmberboneKingEntity extends WitherSkeleton implements EliteLord {

	private static final int SLOT_BREATH = 0;
	private static final int SLOT_SUMMON = 2;
	private static final int SLOT_FIRE_WALL = 3;

	/** 吐息：锥形半角（度）与长度（格） */
	private static final double BREATH_RANGE = 8.0;
	private static final double BREATH_COS = Math.cos(Math.toRadians(30.0));

	private final LordRuntime runtime;
	private final List<PendingCooling> coolingLava = new ArrayList<>();
	private final List<PendingFire> soulFires = new ArrayList<>();

	public EmberboneKingEntity(EntityType<? extends WitherSkeleton> type, Level level) {
		super(type, level);
		this.runtime = new LordRuntime(EncounterDef.byId("emberbone"));
	}

	public EmberboneKingEntity(ServerLevel level) {
		this(EntityTypes.WITHER_SKELETON, level);
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
		witherAura(level);
		tickPending(level);
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) {
			return;
		}
		lavaBreath(level, target);
		emberSummon(level);
		soulFireWall(level, target);
	}

	// ============ 常驻：凋零光环 ============

	private void witherAura(ServerLevel level) {
		if (tickCount % 40 != 0) {
			return;
		}
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
				net.minecraft.world.phys.AABB.ofSize(position(), 16.0D, 16.0D, 16.0D))) {
			if (entity != this && !(entity instanceof WitherSkeleton)) {
				entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0, true, false));
			}
		}
	}

	// ============ 技能 1：熔岩吐息 ============

	private void lavaBreath(ServerLevel level, LivingEntity target) {
		if (!runtime.ready(SLOT_BREATH) || !LordRuntime.inRange(this, target, BREATH_RANGE)) {
			return;
		}
		runtime.cooldown(SLOT_BREATH, 160); // 8 s
		Vec3 look = getLookAngle().normalize();
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
				net.minecraft.world.phys.AABB.ofSize(position(), BREATH_RANGE * 2,
						BREATH_RANGE * 2, BREATH_RANGE * 2))) {
			if (entity == this) {
				continue;
			}
			Vec3 toEntity = entity.position().subtract(position()).normalize();
			if (toEntity.dot(look) < BREATH_COS) {
				continue; // 锥形之外：侧面绕开即可躲
			}
			LordRuntime.magicHit(this, entity, 12.0F);
			entity.setRemainingFireTicks(100); // 5 s 燃烧
		}
		LordRuntime.ring(level, position().add(look.scale(3.0D)), 2.0D, 30, ParticleTypes.FLAME);
		LordRuntime.impactSound(level, this);
		// 吐息末端留 2 个岩浆方块（4 s 后冷却为黑曜石，永久改变地形——设计的地形代价）
		for (int i = 0; i < 2; i++) {
			BlockPos pos = blockPosition().offset(
					(int) (look.x * (BREATH_RANGE - i * 2)), 0, (int) (look.z * (BREATH_RANGE - i * 2)));
			if (level.getBlockState(pos).isAir()) {
				level.setBlock(pos, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
				coolingLava.add(new PendingCooling(pos.immutable(), 80));
			}
		}
	}

	// ============ 技能 3：烬魂召唤 ============

	private void emberSummon(ServerLevel level) {
		if (!runtime.ready(SLOT_SUMMON)) {
			return;
		}
		runtime.cooldown(SLOT_SUMMON, 500); // 25 s
		runtime.standStill(40); // 2 s 蓄力不动
		for (int i = 0; i < 2; i++) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
			double dist = 4.0D + level.getRandom().nextDouble() * 12.0D;
			Vec3 pos = position().add(Math.cos(angle) * dist, 0.0D, Math.sin(angle) * dist);
			LordRuntime.spawnMinion(level, EntityTypes.BLAZE, pos, 0.8F, 30.0F);
			LordRuntime.column(level, pos, 2.0D, 12, ParticleTypes.SOUL_FIRE_FLAME);
		}
	}

	// ============ 技能 4：灵魂火墙 ============

	private void soulFireWall(ServerLevel level, LivingEntity target) {
		if (!runtime.ready(SLOT_FIRE_WALL) || !LordRuntime.inRange(this, target, 16.0)) {
			return;
		}
		runtime.cooldown(SLOT_FIRE_WALL, 360); // 18 s
		Vec3 from = position();
		Vec3 to = target.position();
		Vec3 step = to.subtract(from).normalize();
		for (int i = 1; i <= 7; i++) {
			BlockPos pos = BlockPos.containing(from.add(step.scale(i)));
			if (!level.getBlockState(pos).isAir()) {
				continue;
			}
			level.setBlock(pos, Blocks.SOUL_FIRE.defaultBlockState(), 3);
			soulFires.add(new PendingFire(pos.immutable(), 160)); // 8 s 后自灭
		}
		LordRuntime.sound(level, this, net.minecraft.sounds.SoundEvents.FIRECHARGE_USE, 0.8F);
	}

	// ============ 延迟结算 ============

	private void tickPending(ServerLevel level) {
		List<PendingCooling> cooled = new ArrayList<>();
		for (PendingCooling entry : coolingLava) {
			entry.remaining--;
			if (entry.remaining <= 0) {
				cooled.add(entry);
				if (level.getBlockState(entry.pos).is(Blocks.MAGMA_BLOCK)) {
					level.setBlock(entry.pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
				}
			}
		}
		coolingLava.removeAll(cooled);
		List<PendingFire> expired = new ArrayList<>();
		for (PendingFire entry : soulFires) {
			entry.remaining--;
			if (entry.remaining <= 0) {
				expired.add(entry);
				level.setBlock(entry.pos, Blocks.AIR.defaultBlockState(), 3);
			}
		}
		soulFires.removeAll(expired);
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

	private static final class PendingCooling {
		final BlockPos pos;
		int remaining;

		PendingCooling(BlockPos pos, int remaining) {
			this.pos = pos;
			this.remaining = remaining;
		}
	}

	private static final class PendingFire {
		final BlockPos pos;
		int remaining;

		PendingFire(BlockPos pos, int remaining) {
			this.pos = pos;
			this.remaining = remaining;
		}
	}
}
