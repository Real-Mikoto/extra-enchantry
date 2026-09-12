package realmikoto.extraenchantry.entity.abyss;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 无声者（1.8.2「群」补齐）：对"声即光"法则的反噬——完全无声的精英。
 *
 * 机制（对齐设计稿 §5.1）：
 * - **永不产生声纹**（自身不触发任何 EchoManager 上报）；
 * - **不被回声视觉显形**（回响灯的 GLOWING 显形对它无效——由 EchoLanternItem 侧排除）；
 * - 只能靠肉眼（原版光照/夜视）发现；
 * - 高伤害低血量，稀有刷新（AbyssSpawning 极低概率）。
 */
public class SoundlessEntity extends Monster {

	public SoundlessEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 30.0D)
				.add(Attributes.ATTACK_DAMAGE, 9.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.34D)
				.add(Attributes.FOLLOW_RANGE, 28.0D);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6D));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/** 无声者：不受回声显形影响（供回响灯/共鸣器查询） */
	public boolean isSoundless() {
		return true;
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, net.minecraft.world.damagesource.DamageSource source,
			boolean recentlyHit) {
		// 掉落：静默之石（"寂静"的结晶）
		this.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(
				realmikoto.extraenchantry.AbyssResources.SILENCE_STONE));
		// 掉落：10% 无相之尘（无声者是"声即光"法则的反噬——其残骸偶尔凝出第九相的尘）
		if (getRandom().nextFloat() < 0.10F) {
			this.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(
					realmikoto.extraenchantry.AbyssResources.AETHER_DUST));
		}
		super.dropCustomDeathLoot(level, source, recentlyHit);
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return distance > 80.0D;
	}

	/** 辅助：判断某实体是否为无声者（跨系统查询入口） */
	public static boolean isSoundless(LivingEntity entity) {
		return entity instanceof SoundlessEntity;
	}
}
