package realmikoto.extraenchantry;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
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
 * 编年史卷轴（1.3.1「铭文纪元」§3.4）：达成大共鸣者时派发的完整编年。
 *
 * 八家族统一史 + 玩家被铭文承认的纪录。全文只此一份——不做数据驱动，
 * 文本硬编码在 lang（{@code chronicle.extra-enchantry.*}），右键展开。
 */
public class ChronicleScrollItem extends Item {

	public ChronicleScrollItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.extra-enchantry.chronicle_scroll")
				.withStyle(ChatFormatting.GRAY));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			MutableComponent out = Component.empty();
			out.append(Component.translatable("chronicle.extra-enchantry.header")
					.withStyle(ChatFormatting.GOLD)).append("\n");
			for (int i = 1; i <= 14; i++) {
				out.append(Component.translatable("chronicle.extra-enchantry.line" + i)
						.withStyle(ChatFormatting.GRAY)).append("\n");
			}
			out.append(Component.translatable("chronicle.extra-enchantry.footer")
					.withStyle(ChatFormatting.DARK_GRAY));
			serverPlayer.sendSystemMessage(out);
		}
		return InteractionResult.SUCCESS;
	}
}
