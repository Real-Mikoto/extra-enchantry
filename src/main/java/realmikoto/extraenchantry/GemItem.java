package realmikoto.extraenchantry;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * 家族宝石（1.4.0）：消耗性合成材料——配饰合成的核心材料（合成时即决定被动）。
 * 首次合成触发家族格言 chat（数据驱动 lore/gems/，静默开关沿用 silence_onboarding）。
 */
public class GemItem extends Item {

	private final String gemPath;

	public GemItem(String gemPath, Properties properties) {
		super(properties);
		this.gemPath = gemPath;
	}

	public String gemPath() {
		return gemPath;
	}

	@Override
	public void onCraftedBy(ItemStack stack, net.minecraft.world.entity.player.Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			OnboardingManager.onGemCrafted(serverPlayer, gemPath);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context,
			net.minecraft.world.item.component.TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.extra-enchantry.gem." + gemPath)
				.withStyle(net.minecraft.ChatFormatting.GRAY));
	}
}
