package realmikoto.extraenchantry.entity.abyss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import realmikoto.extraenchantry.AbyssMemories;
import realmikoto.extraenchantry.LoreTriggerManager;

/**
 * 记忆残影（1.8.4「忆」补齐）：封印纪元人物的残影，中立、可对话。
 *
 * 机制（对齐设计稿 §5.1「可对话、可交易情报」）：
 * - 不索敌、不攻击；
 * - 右键 → 依序讲述一段"渊之记忆"（与记忆残片共用 AbyssMemories 文本，
 *   但残影是"活的"——可反复聆听，不消耗物品）；
 * - 周身常驻幽匿灵魂粒子（半透明的"回声"质感）。
 */
public class MemoryShadeEntity extends Monster {

	public MemoryShadeEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 20.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.22D)
				.add(Attributes.FOLLOW_RANGE, 12.0D);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.4D));
		this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10.0F));
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (level().isClientSide() || !(player instanceof net.minecraft.server.level.ServerPlayer sp)) {
			return InteractionResult.SUCCESS;
		}
		// 交易情报（§5.1「可对话、可交易情报」）：以记忆之尘换取封印旋律的顺序
		ItemStack held = player.getItemInHand(hand);
		if (held.is(realmikoto.extraenchantry.AbyssResources.MEMORY_DUST)) {
			if (!sp.getAbilities().instabuild) {
				held.shrink(1);
			}
			sp.sendSystemMessage(Component.literal("§8════ 记忆残影的交易 ════"));
			sp.sendSystemMessage(Component.translatable("message.extra-enchantry.memory_shade.intel"));
			if (level() instanceof ServerLevel server) {
				server.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY(1.2D), getZ(),
						12, 0.4D, 0.6D, 0.4D, 0.01D);
			}
			return InteractionResult.SUCCESS;
		}
		// 依序讲述（记忆残影可反复听；用独立触发 id 记录进度）
		int next = 1;
		for (int i = 1; i <= 3; i++) {
			if (!LoreTriggerManager.hasFired(sp, "abyss_shade_" + i)) {
				next = i;
				break;
			}
			next = 1;
		}
		LoreTriggerManager.fireOnce(sp, "abyss_shade_" + next);
		sp.sendSystemMessage(Component.literal("§8════ 记忆残影的低语 ════"));
		for (String line : AbyssMemories.get(next)) {
			sp.sendSystemMessage(Component.literal(line));
		}
		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY(1.2D), getZ(),
					12, 0.4D, 0.6D, 0.4D, 0.01D);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel server && this.tickCount % 12 == 0) {
			server.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY(1.0D), getZ(),
					2, 0.3D, 0.5D, 0.3D, 0.005D);
		}
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return distance > 96.0D;
	}

	@Override
	public boolean isInvulnerable() {
		return true;   // 残影不可被伤害（"不可改变的历史"）
	}

	@Override
	public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
		return false;
	}
}
