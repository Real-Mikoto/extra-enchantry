package realmikoto.extraenchantry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * 领主实体共享运行时（1.5.0「五境领主」§7.2）。
 *
 * 五境领主各自继承不同的原版实体类型（监守者 / 凋灵骷髅 / 远古守卫者 / 女巫 / 末影人），
 * Java 单继承无法抽公共父类，故把"属性修改器 / 体型 / boss 血条 / 技能冷却 / 站立引导"
 * 收敛到本运行时，各领主持有一份并在 {@code customServerAiStep} 中委托。
 *
 * 数值全部来自 {@link EliteEncounterConfig}（数据包可覆盖）：
 * 生命 / 近战伤害 / 体型 / 索敌范围 / 击杀经验。
 */
public final class LordRuntime {

	/** 技能槽数量（设计 §1.4 等：每领主 4 个技能） */
	public static final int SKILL_SLOTS = 4;

	/** boss 血条同步半径（格） */
	private static final double BAR_RANGE = 64.0;

	/** 全局召唤物登记 → 维度（跨维度查询用；修复：召唤物持久化且无清理） */
	private static final java.util.Map<UUID, ServerLevel> MINION_LEVELS = new java.util.concurrent.ConcurrentHashMap<>();

	/** 全局召唤物登记（修复：召唤物不再持久化，统一登记以便遭遇结束时清散） */
	private static final java.util.Set<UUID> MINIONS = java.util.concurrent.ConcurrentHashMap.newKeySet();

	private final EncounterDef def;
	private final int[] cooldowns = new int[SKILL_SLOTS];
	private ServerBossEvent bossEvent;
	/** 站立不动的剩余 tick（召唤 / 蓄力技能引导窗口） */
	private int standTicks;
	// ============ 1.6.0 变体修饰（宣战图腾 / 归一回响，§1.4 / §3.4） ============
	/** 觉醒变体（图腾路径）：生命 ×1.4 / 伤害 ×1.2 / 技能冷却 ×0.8 */
	private boolean awakened;
	/** 归一回响（链式终局）：生命 ×0.75 / 技能冷却 ×1.2 / 体型 −0.1 */
	private boolean echo;

	public LordRuntime(EncounterDef def) {
		this.def = def;
	}

	/** 图腾路径觉醒修饰（EliteEncounterManager.awaken 调用） */
	public void markAwakened() {
		this.awakened = true;
	}

	/** 归一回响修饰（ConvergenceManager.spawnEcho 调用） */
	public void markEcho() {
		this.echo = true;
	}

	/** 是否为觉醒变体（掉落分支与名称前缀判定） */
	public boolean isAwakened() {
		return awakened;
	}

	public EncounterDef def() {
		return def;
	}

	/** 是否在站立引导中（召唤 / 蓄力技能期间领主不移动，给玩家输出窗口） */
	public boolean isStanding() {
		return standTicks > 0;
	}

	public void standStill(int ticks) {
		this.standTicks = Math.max(this.standTicks, ticks);
	}

	// ============ 每 tick ============

	/** 服务端推进：属性 / 体型 / 血条 / 冷却 / 站立引导 */
	public void serverTick(LivingEntity self, ServerLevel level) {
		applyStats(self, level);
		for (int i = 0; i < cooldowns.length; i++) {
			if (cooldowns[i] > 0) {
				cooldowns[i]--;
			}
		}
		if (standTicks > 0) {
			standTicks--;
			if (self instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			self.setDeltaMovement(self.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
		}
		// 归一回响：无独立血条（ConvergenceManager 单血条接管）
		if (!echo) {
			updateBossBar(self, level);
		}
	}

	/** 技能是否就绪 */
	public boolean ready(int slot) {
		return slot >= 0 && slot < SKILL_SLOTS && cooldowns[slot] <= 0;
	}

	/** 设置技能冷却（tick；觉醒 ×0.8 更紧凑 / 回响 ×1.2 更从容，§1.4 / §3.4） */
	public void cooldown(int slot, int ticks) {
		if (slot >= 0 && slot < SKILL_SLOTS) {
			if (awakened) {
				ticks = Math.max(1, (int) (ticks * 0.8D));
			} else if (echo) {
				ticks = (int) (ticks * 1.2D);
			}
			cooldowns[slot] = ticks;
		}
	}

	/** 领主死亡 / 移除：拆除血条 + 清散召唤物，避免玩家客户端与世界残留 */
	public void dispose() {
		if (bossEvent != null) {
			bossEvent.removeAllPlayers();
			bossEvent = null;
		}
		discardMinions();
	}

	/**
	 * 清散全部已登记召唤物（dispose 调用；全局表——召唤物数量有限且随遭遇一对一清空）。
	 * 修复：旧实现召唤物持久化且无清理，领主战每场向世界永久加怪。
	 */
	public static void discardMinions() {
		for (java.util.Iterator<UUID> it = MINIONS.iterator(); it.hasNext(); ) {
			UUID minionId = it.next();
			ServerLevel sl = MINION_LEVELS.remove(minionId);
			if (sl != null) {
				Entity minion = sl.getEntity(minionId);
				if (minion != null && minion.isAlive()) {
					minion.discard();
				}
			}
			it.remove();
		}
	}

	// ============ 属性与血条 ============

	private void applyStats(LivingEntity self, ServerLevel level) {
		EliteEncounterConfig.Realm rules = EliteEncounterConfig.realm(def.id());
		// 1.6.0 变体修饰：觉醒 ×1.4 生命（回响与其互斥——图腾与归一不会同时产生同一领主）
		double healthTarget = rules.stats().health();
		double damageTarget = rules.stats().meleeDamage();
		double scaleTarget = rules.stats().scale();
		if (awakened) {
			healthTarget *= 1.4D;
			damageTarget *= 1.2D;
		} else if (echo) {
			healthTarget *= 0.75D;
			scaleTarget = Math.max(1.0D, scaleTarget - 0.1D);
		}
		setTransient(self, Attributes.MAX_HEALTH, "lord_health",
				healthTarget - baseValue(self, Attributes.MAX_HEALTH),
				AttributeModifier.Operation.ADD_VALUE);
		setTransient(self, Attributes.ATTACK_DAMAGE, "lord_damage",
				damageTarget - baseValue(self, Attributes.ATTACK_DAMAGE),
				AttributeModifier.Operation.ADD_VALUE);
		setTransient(self, Attributes.FOLLOW_RANGE, "lord_range",
				rules.stats().followRange() - baseValue(self, Attributes.FOLLOW_RANGE),
				AttributeModifier.Operation.ADD_VALUE);
		setTransient(self, Attributes.SCALE, "lord_scale",
				scaleTarget - 1.0D, AttributeModifier.Operation.ADD_VALUE);
	}

	private void updateBossBar(LivingEntity self, ServerLevel level) {
		if (bossEvent == null) {
			// 觉醒变体名称前缀「觉醒·」（§1.4；归一回响沿用原名——血条由 ConvergenceManager 接管，
			// 此处仅在 echo 时隐藏独立血条避免双血条）
			if (echo) {
				return;
			}
			Component name = Component.translatable(def.nameKey());
			if (awakened) {
				name = Component.translatable("event.extra-enchantry.awakened_prefix")
						.append(" ").append(name);
			}
			bossEvent = new ServerBossEvent(UUID.randomUUID(),
					name, def.barColor(), BossEvent.BossBarOverlay.PROGRESS);
		}
		List<ServerPlayer> nearby = level.getPlayers(
				player -> player.distanceTo(self) <= BAR_RANGE
						&& player.level().dimension().equals(level.dimension()),
				Integer.MAX_VALUE);
		for (ServerPlayer player : nearby) {
			if (!bossEvent.getPlayers().contains(player)) {
				bossEvent.addPlayer(player);
			}
		}
		for (ServerPlayer player : List.copyOf(bossEvent.getPlayers())) {
			if (!nearby.contains(player)) {
				bossEvent.removePlayer(player);
			}
		}
		float progress = self.getMaxHealth() > 0.0F ? self.getHealth() / self.getMaxHealth() : 0.0F;
		bossEvent.setProgress(Math.max(0.0F, Math.min(1.0F, progress)));
	}

	// ============ 技能公共工具 ============

	/** 魔法伤害（无视护甲；受壁垒单次伤害上限钳制——壁垒注入点在 getDamageAfterMagicAbsorb） */
	public static void magicHit(LivingEntity attacker, LivingEntity target, float amount) {
		DamageSource source = attacker.level().damageSources().magic();
		target.hurt(source, amount);
	}

	/** 音波伤害（原版特性：无视护甲与魔抗，仍受壁垒上限钳制） */
	public static void sonicHit(LivingEntity attacker, LivingEntity target, float amount) {
		target.hurt(attacker.level().damageSources().sonicBoom(attacker), amount);
	}

	/** 近战伤害（可被护甲减免、可被格挡） */
	public static void meleeHit(LivingEntity attacker, LivingEntity target, float amount) {
		target.hurt(attacker.level().damageSources().mobAttack(attacker), amount);
	}

	/** 半径内所有生物施加效果 */
	public static void applyAreaEffect(ServerLevel level, Vec3 center, double radius,
			net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int ticks, int amplifier,
			LivingEntity exclude) {
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
				net.minecraft.world.phys.AABB.ofSize(center, radius * 2, radius * 2, radius * 2))) {
			if (entity == exclude) {
				continue;
			}
			entity.addEffect(new MobEffectInstance(effect, ticks, amplifier));
		}
	}

	/** 环形粒子（走 FxHelper 定点爆发，保证与全局反馈纪律一致） */
	public static void ring(ServerLevel level, Vec3 center, double radius, int count,
			net.minecraft.core.particles.ParticleOptions particle) {
		for (int i = 0; i < count; i++) {
			double angle = (Math.PI * 2.0) * i / count;
			FxHelper.burstAt(level,
					center.x + Math.cos(angle) * radius, center.y + 0.5D, center.z + Math.sin(angle) * radius,
					particle, 1, 0.0D);
		}
	}

	/** 竖直粒子柱（前兆 / 定位点） */
	public static void column(ServerLevel level, Vec3 bottom, double height, int count,
			net.minecraft.core.particles.ParticleOptions particle) {
		for (int i = 0; i < count; i++) {
			FxHelper.burstAt(level, bottom.x, bottom.y + height * i / count, bottom.z,
					particle, 1, 0.05D);
		}
	}

	/** 生成召唤物（复用原版实体类型 + 体型/生命覆盖，不持久化） */
	public static <T extends Mob> T spawnMinion(ServerLevel level, EntityType<T> type, Vec3 pos,
			float scale, float health) {
		T minion = type.create(level, EntitySpawnReason.TRIGGERED);
		if (minion == null) {
			return null;
		}
		minion.snapTo(pos.x, pos.y, pos.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		// 修复：召唤物不再 setPersistenceRequired（写存档且无清理路径，领主战每场永久加怪）。
		// 登记到运行时，dispose / cancel / onLordDeath 时统一 discard；区块卸载自然消失。
		MINIONS.add(minion.getUUID());
		MINION_LEVELS.put(minion.getUUID(), level);
		setTransient(minion, Attributes.SCALE, "lord_minion_scale", scale - 1.0D,
				AttributeModifier.Operation.ADD_VALUE);
		setTransient(minion, Attributes.MAX_HEALTH, "lord_minion_health",
				health - baseValue(minion, Attributes.MAX_HEALTH), AttributeModifier.Operation.ADD_VALUE);
		minion.setHealth(health);
		level.addFreshEntity(minion);
		return minion;
	}

	/** 音效（统一走 FxHelper，保证音量 / 音源纪律；Entity 形态） */
	public static void sound(ServerLevel level, Entity at, net.minecraft.sounds.SoundEvent event, float pitch) {
		FxHelper.play(level, at, event, 1.5F, pitch);
	}

	/** 通用打击反馈音（技能未指定专属音效时） */
	public static void impactSound(ServerLevel level, Entity at) {
		FxHelper.play(level, at, SoundEvents.WARDEN_SONIC_BOOM, 1.2F, 1.0F);
	}

	// ============ 内部工具 ============

	private static double baseValue(LivingEntity self,
			net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
		AttributeInstance instance = self.getAttribute(attribute);
		return instance != null ? instance.getBaseValue() : 0.0D;
	}

	private static void setTransient(LivingEntity self,
			net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
			String id, double amount, AttributeModifier.Operation operation) {
		if (self == null) {
			return;
		}
		AttributeInstance instance = self.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		instance.addOrUpdateTransientModifier(new AttributeModifier(
				ExtraEnchantry.id(id), amount, operation));
	}

	/** 领主与召唤物均不持久化（重启后不残留孤儿领主，符合 DecoyEntity 模式） */
	public static boolean shouldSave() {
		return false;
	}

	/** 目标距离判定（技能射程） */
	public static boolean inRange(Entity self, Entity target, double range) {
		return target != null && target.isAlive() && self.distanceTo(target) <= range;
	}
}
