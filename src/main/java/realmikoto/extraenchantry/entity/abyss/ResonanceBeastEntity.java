package realmikoto.extraenchantry.entity.abyss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import realmikoto.extraenchantry.EchoManager;

/**
 * 声纹兽（1.8.2「群」）：以声为食的中立生物。
 *
 * 行为：被「响的玩家」吸引，玩家静默时主动远离；
 * 右键（空手或回响碎片）驯服为跟随者——深渊里唯一会「亲近你」的东西。
 * 对齐设计稿 §5.1：世界的「听者」。
 */
public class ResonanceBeastEntity extends TamableAnimal {

	public ResonanceBeastEntity(EntityType<? extends TamableAnimal> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, 26.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.32D)
				.add(Attributes.FOLLOW_RANGE, 32.0D);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.0D, 6.0F, 2.0F));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6D));
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!isTame()) {
			ItemStack held = player.getMainHandItem();
			// 以声诱之（§5.1）：空手 + 它已被你的声音吸引到身边（<4 格）→ 驯服；
			// 或以回响碎片相赠（"以声为食"的馈赠路径）
			boolean lured = held.isEmpty()
					&& EchoManager.lastEcho((net.minecraft.server.level.ServerPlayer) player) >= 5.0F
					&& distanceTo(player) < 4.0D;
			if (lured || held.is(net.minecraft.world.item.Items.ECHO_SHARD)) {
				if (!player.getAbilities().instabuild
						&& held.is(net.minecraft.world.item.Items.ECHO_SHARD)) {
					held.shrink(1);
				}
				if (level() instanceof ServerLevel server) {
					server.sendParticles(ParticleTypes.HEART,
							getX(), getY(0.8D), getZ(), 4, 0.4D, 0.4D, 0.4D, 0.0D);
					server.sendParticles(ParticleTypes.NOTE,
							getX(), getY(1.4D), getZ(), 3, 0.3D, 0.3D, 0.3D, 0.0D);
				}
				tame(player);
				return InteractionResult.SUCCESS;
			}
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return false;
	}

	@Override
	public net.minecraft.world.entity.AgeableMob getBreedOffspring(ServerLevel level,
			net.minecraft.world.entity.AgeableMob partner) {
		return realmikoto.extraenchantry.AbyssEntities.RESONANCE_BEAST.create(level, net.minecraft.world.entity.EntitySpawnReason.BREEDING);
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel server && this.tickCount % 15 == 0 && !isTame()) {
			server.sendParticles(ParticleTypes.BUBBLE_POP,
					getX(), getY(0.5D), getZ(), 1, 0.2D, 0.2D, 0.2D, 0.0D);
			// 追声 / 驱赶 AI（§5.1「以声为食，追逐声音；可被驯服或驱赶」）：
			// 响的玩家吸引它（猎食），过响的大声纹反而把它吓走（驱赶 = 以声诱之的反面）。
			Player nearest = level().getNearestPlayer(this, 24.0D);
			if (nearest instanceof net.minecraft.server.level.ServerPlayer sp) {
				float echo = EchoManager.lastEcho(sp);
				double dx = sp.getX() - getX();
				double dz = sp.getZ() - getZ();
				double dist = Math.max(0.5D, Math.sqrt(dx * dx + dz * dz));
				if (echo >= 12.0F && dist < 8.0D) {
					// 过响：逃开（驱赶）
					getNavigation().moveTo(getX() - dx / dist * 10.0D, getY(), getZ() - dz / dist * 10.0D, 1.15D);
				} else if (echo >= 5.0F) {
					// 追声（捕食声源）
					getNavigation().moveTo(sp, 1.05D);
					server.sendParticles(ParticleTypes.NOTE,
							getX(), getY(1.2D), getZ(), 1, 0.2D, 0.2D, 0.2D, 0.0D);
				}
			}
		}
	}

}
