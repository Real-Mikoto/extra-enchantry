package realmikoto.extraenchantry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * 回响灯（1.8.1「声」）：主动发声照明——幽渊里「看」的主要手段。
 *
 * 右键：以自身为中心爆出一圈声纹（EchoManager.emit，半径 14），
 * 短暂「照亮」周围（大范围粒子涟漪 + 临时发光效果），同时必然引来生物——
 * 这就是「沉默悖论」的具象：想看清，就得喊。
 *
 * 冷却：100 tick（5 秒），防止连点刷显形。
 */
public class EchoLanternItem extends Item {

	private static final int COOLDOWN_TICKS = 100;
	private static final float ECHO_RADIUS = 14.0F;

	public EchoLanternItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		if (serverPlayer.getCooldowns().isOnCooldown(stack)) {
			return InteractionResult.FAIL;
		}
		if (!AbyssKey.isIn(serverPlayer)) {
			serverPlayer.sendSystemMessage(Component.translatable("message.extra-enchantry.lantern.not_in_abyss"));
			return InteractionResult.FAIL;
		}

		// 大声纹 + 发光：照亮世界，也广播自己
		EchoManager.emit(serverPlayer, ECHO_RADIUS);
		// 静默附魔代价（§5.3）：失去回声视觉——自身不获得发光显形
		if (!AbyssEnchantments.hasStillness(serverPlayer)) {
			serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					net.minecraft.world.effect.MobEffects.GLOWING, 60, 0, true, false));
		}
		// 回声视觉：以声纹「显形」周围生物（无声者例外——它不回应声音）
		for (net.minecraft.world.entity.LivingEntity nearby : serverLevel(level)
				.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
						serverPlayer.getBoundingBox().inflate(ECHO_RADIUS),
						e -> e != serverPlayer
								&& !realmikoto.extraenchantry.entity.abyss.SoundlessEntity.isSoundless(e))) {
			nearby.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					net.minecraft.world.effect.MobEffects.GLOWING, 80, 0, true, false));
		}
		FxHelper.ring(serverLevel(level), serverPlayer, ECHO_RADIUS,
				ParticleTypes.END_ROD, 32);
		FxHelper.play((ServerLevel) level, serverPlayer, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 0.4F);
		serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
		return InteractionResult.SUCCESS;
	}

	private static ServerLevel serverLevel(Level level) {
		return (ServerLevel) level;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.extra-enchantry.echo_lantern")
				.withStyle(net.minecraft.ChatFormatting.GRAY));
	}
}
