package realmikoto.extraenchantry.entity.abyss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions.Selector;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import realmikoto.extraenchantry.AbyssKey;
import realmikoto.extraenchantry.EchoManager;

/**
 * 回响幽灵（1.8.2「群」）：平时「无形」（半透明 + 不主动索敌），
 * 只有当目标玩家的「声纹」足够响（≥5）时才会显形并追击——
 * 对齐设计稿 §5.1：在声纹中显形、模仿玩家刚发出的声音。
 *
 * 显形机制（服务端权威）：tick 中按 EchoManager.lastEcho(目标) 决定 visible + 发光；
 * 声纹安静时幽灵隐形且 AI 停摆（目标选择器挂声纹门控）。
 */
public class EchoWraithEntity extends Monster {

	/** 触发显形的声纹阈值（与静默悖论数值一致：普通移动即危险） */
	private static final float REVEAL_ECHO_THRESHOLD = 5.0F;

	public EchoWraithEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 24.0D)
				.add(Attributes.ATTACK_DAMAGE, 5.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.28D)
				.add(Attributes.FOLLOW_RANGE, 24.0D);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1D, false));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6D));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class,
				true, (LivingEntity target, ServerLevel lvl) -> target instanceof Player p
						&& EchoManager.lastEcho(p) >= REVEAL_ECHO_THRESHOLD));
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide() || !(level() instanceof ServerLevel server)) {
			return;
		}
		LivingEntity target = getTarget();
		boolean revealed = target != null
				&& EchoManager.lastEcho(target instanceof Player p ? p : null) >= REVEAL_ECHO_THRESHOLD;
		// 声纹中显形；安静时隐形但保留缓慢游荡
		setInvisible(!revealed);
		if (revealed && this.tickCount % 10 == 0) {
			server.sendParticles(ParticleTypes.SCULK_SOUL,
					getX(), getY(0.6D), getZ(), 2, 0.3D, 0.3D, 0.3D, 0.01D);
		}
		// 模仿引诱（§5.1「会模仿玩家刚发出的声音」）：每 6 秒回放最近听到的声音，
		// 诱使玩家循声而来——它偷走你的声音，再用自己的位置还给你。
		if (this.tickCount % 120 == 0) {
			Player nearest = level().getNearestPlayer(this, 32.0D);
			if (nearest != null) {
				net.minecraft.sounds.SoundEvent mimic = EchoManager.lastSound(nearest);
				if (mimic != null) {
					level().playSound(null, getX(), getY(), getZ(), mimic,
							net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.85F);
					server.sendParticles(ParticleTypes.NOTE,
							getX(), getY(1.6D), getZ(), 3, 0.3D, 0.3D, 0.3D, 0.0D);
					// 循声靠近——模仿的目的是引你过来
					this.getNavigation().moveTo(nearest, 0.9D);
				}
			}
		}
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		// 离开幽渊维度半径较远即可消失（维度怪物不跨维度流浪）
		return AbyssKey.isIn(this) && distance > 64.0D;
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		// 掉落：20% 回响碎片（模仿主题：它偷走的声音归还原主）
		if (getRandom().nextFloat() < 0.20F) {
			this.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(
					net.minecraft.world.item.Items.ECHO_SHARD));
		}
		super.dropCustomDeathLoot(level, source, recentlyHit);
	}
}
