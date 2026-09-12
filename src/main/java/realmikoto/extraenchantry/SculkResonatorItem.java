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
 * 幽匿共鸣器（1.8.1「声」）：便携「扫描」装置——回声视觉的主动版。
 *
 * 右键：中等声纹（半径 8），但额外回报环境情报——
 * 标记周围 24 格内的幽匿（SCULK）分布（动作栏提示），用于寻找尖塔与静默区。
 *
 * 与回响灯的分工：灯 = 大声纹 + 强光（高风险侦察）；
 * 共鸣器 = 中声纹 + 情报（低噪声测绘）。
 */
public class SculkResonatorItem extends Item {

	private static final int COOLDOWN_TICKS = 60;
	private static final float ECHO_RADIUS = 8.0F;
	private static final int SCAN_RADIUS = 24;

	public SculkResonatorItem(Properties properties) {
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
			serverPlayer.sendSystemMessage(Component.literal(
					"§7共鸣器无声——它只回应深渊的神经。"));
			return InteractionResult.FAIL;
		}

		EchoManager.emit(serverPlayer, ECHO_RADIUS);
		int sculk = countSculk(serverPlayer);
		String report = sculk > 64 ? "§5共鸣强烈——周围幽匿密布（" + sculk + "）"
				: sculk > 16 ? "§7有幽匿的回声（" + sculk + "）"
				: "§8几乎听不到幽匿——这里是静默之地（" + sculk + "）";
		serverPlayer.sendSystemMessage(Component.literal(report));
		FxHelper.ring((ServerLevel) level, serverPlayer, ECHO_RADIUS,
				ParticleTypes.SCULK_SOUL, 16);
		FxHelper.play((ServerLevel) level, serverPlayer, SoundEvents.SCULK_SENSOR_PLACE,
				1.0F, 0.6F);
		serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
		return InteractionResult.SUCCESS;
	}

	private static int countSculk(ServerPlayer player) {
		int count = 0;
		var pos = player.blockPosition();
		for (var cur : net.minecraft.core.BlockPos.withinManhattan(pos, SCAN_RADIUS, 8, SCAN_RADIUS)) {
			if (player.level().getBlockState(cur).is(net.minecraft.world.level.block.Blocks.SCULK)) {
				count++;
			}
		}
		return count;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.extra-enchantry.sculk_resonator")
				.withStyle(net.minecraft.ChatFormatting.GRAY));
	}
}
