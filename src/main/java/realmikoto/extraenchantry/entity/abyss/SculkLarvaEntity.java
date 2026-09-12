package realmikoto.extraenchantry.entity.abyss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import realmikoto.extraenchantry.EchoManager;

/**
 * 幽匿幼体（1.8.2「群」补齐）：幽渊的"细胞"——成群蔓延、被声音激活的小型敌对生物。
 *
 * 机制（对齐设计稿 §5.1）：
 * - 成群刷新（AbyssSpawning 一次生成 2–4 只）；
 * - 只有目标玩家的声纹 ≥5 时才索敌（安静时无害爬行）；
 * - 死亡掉幽匿丝（30%）——"细胞"的产物。
 */
public class SculkLarvaEntity extends Monster {

	private static final float ACTIVATE_ECHO = 5.0F;

	public SculkLarvaEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 8.0D)
				.add(Attributes.ATTACK_DAMAGE, 2.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.32D)
				.add(Attributes.FOLLOW_RANGE, 16.0D);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7D));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class,
				true, (net.minecraft.world.entity.LivingEntity target, ServerLevel lvl) ->
						target instanceof Player p && EchoManager.lastEcho(p) >= ACTIVATE_ECHO));
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel server && this.tickCount % 20 == 0) {
			server.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
					getX(), getY(0.3D), getZ(), 1, 0.2D, 0.1D, 0.2D, 0.0D);
		}
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, net.minecraft.world.damagesource.DamageSource source,
			boolean recentlyHit) {
		if (level.getRandom().nextFloat() < 0.30F) {
			this.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(
					realmikoto.extraenchantry.AbyssResources.SCULK_SILK));
		}
		super.dropCustomDeathLoot(level, source, recentlyHit);
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return distance > 64.0D;
	}
}
