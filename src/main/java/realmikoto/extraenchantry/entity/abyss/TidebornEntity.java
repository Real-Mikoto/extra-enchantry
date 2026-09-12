package realmikoto.extraenchantry.entity.abyss;

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
import realmikoto.extraenchantry.AbyssTradeOffers;

import java.util.List;

/**
 * 渊息者（1.8.2「群」）：渊族的活口，半人半鱼的中立居民。
 *
 * 不索敌、不攻击（Monster 基底仅为了共享水生/黑暗行为，注册为 CREATURE 性格）；
 * 右键交互触发「以物易物」：接受幽匿块 / 回响碎片，给出幽渊资源（对齐 §5.1）。
 * 交易内容由 {@link AbyssTradeOffers} 按个体随机决定（1.8.3 扩展资源类型）。
 */
public class TidebornEntity extends Monster {

	public TidebornEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 30.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.3D)
				.add(Attributes.FOLLOW_RANGE, 16.0D);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.5D));
		this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
	}

	@Override
	public boolean isInvulnerableTo(ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
		// 深渊的居民不惧溺水（它们本就属于水）
		return source.is(net.minecraft.world.damagesource.DamageTypes.DROWN)
				|| super.isInvulnerableTo(level, source);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		ItemStack offered = player.getItemInHand(hand);
		List<ItemStack> result = AbyssTradeOffers.trade(offered);
		if (result == null) {
			// 探索引导（§6.9）：不可交易时给出方向性线索；空手 → 「教深息」（§5.1）
			if (player instanceof net.minecraft.server.level.ServerPlayer sp && offered.isEmpty()) {
				teachDeepBreath(sp);
				swing(InteractionHand.MAIN_HAND);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		}
		if (!player.getAbilities().instabuild) {
			offered.shrink(1);
		}
		for (ItemStack give : result) {
			if (!player.getInventory().add(give)) {
				this.spawnAtLocation((ServerLevel) level(), give);
			}
		}
		swing(InteractionHand.MAIN_HAND);
		player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
				"message.extra-enchantry.tideborn.trade"));
		return InteractionResult.SUCCESS;
	}

	/**
	 * 教「深息」（§5.1「教玩家深息」）：渊息者传授渊族的呼吸法——
	 * 授予水呼吸（液幽匿/水中生存），首次传授附深息面罩的线索。
	 */
	private void teachDeepBreath(net.minecraft.server.level.ServerPlayer sp) {
		boolean first = !realmikoto.extraenchantry.LoreTriggerManager.hasFired(sp, "tideborn_deep_breath");
		realmikoto.extraenchantry.LoreTriggerManager.fireOnce(sp, "tideborn_deep_breath");
		sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(
				net.minecraft.world.effect.MobEffects.WATER_BREATHING,
				first ? 20 * 120 : 20 * 60, 0, true, false));
		sp.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
				first ? "message.extra-enchantry.tideborn.teach_first"
						: "message.extra-enchantry.tideborn.teach_again"));
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return distance > 96.0D;
	}

}
