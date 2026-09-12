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
 * 共鸣音叉（v1.8.0.2，设计稿 §6.6）：幽渊内右键循环切换共鸣调式。
 */
public class TuningForkItem extends Item {

	public TuningForkItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		if (!AbyssKey.isIn(serverPlayer)) {
			serverPlayer.sendSystemMessage(Component.literal("§7音叉在光下无声——它只回应深渊。"));
			return InteractionResult.FAIL;
		}
		AbyssTuning.cycle(serverPlayer);
		if (level instanceof ServerLevel server) {
			FxHelper.ring(server, serverPlayer, 8.0D, ParticleTypes.SCULK_SOUL, 16);
			FxHelper.play(server, serverPlayer, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 0.5F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.extra-enchantry.tuning_fork")
				.withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
	}
}
