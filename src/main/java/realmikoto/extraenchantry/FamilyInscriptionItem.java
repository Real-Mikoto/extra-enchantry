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

import java.util.Locale;
import java.util.function.Consumer;

/**
 * 家族铭文（1.3.1「铭文纪元」§3.2）：八系家族的初民纪元证词。
 *
 * 设计与 {@link FamilySigils} 同款：一个物品类型 + {@code family_id} 数据组件表达八面铭文。
 * 某家族首次达成 FULL 时由 {@link OnboardingManager#onFamilyFull} 派发；右键研读 lore 文本
 * （{@link LoreLoader#inscription}，数据驱动 / 缺省内置默认）；家族铭文亦可由来者手札第 2 页重读。
 */
public class FamilyInscriptionItem extends Item {

	public FamilyInscriptionItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			FamilyResonanceManager.Family family = FamilySigils.familyOf(player.getItemInHand(hand));
			if (family != null) {
				serverPlayer.sendSystemMessage(buildLore(family));
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** 拼装一面铭文的研读输出 */
	static MutableComponent buildLore(FamilyResonanceManager.Family family) {
		LoreLoader.FamilyInscription inscription = LoreLoader.inscription(family);
		MutableComponent out = Component.empty();
		out.append(Component.literal("── " + inscription.title() + " ──")
				.withStyle(FamilySigils.familyColor(family))).append("\n");
		for (String line : inscription.lines()) {
			out.append(Component.literal(line).withStyle(ChatFormatting.GRAY)).append("\n");
		}
		out.append(Component.translatable("lore.extra-enchantry.inscription.footer")
				.withStyle(ChatFormatting.DARK_GRAY));
		return out;
	}

	@Override
	public Component getName(ItemStack stack) {
		FamilyResonanceManager.Family family = FamilySigils.familyOf(stack);
		if (family == null) {
			return Component.translatable("item.extra-enchantry.family_inscription.invalid")
					.withStyle(ChatFormatting.DARK_GRAY);
		}
		return Component.translatable("item.extra-enchantry.family_inscription",
						Component.translatable("family.extra-enchantry." + family.name().toLowerCase(Locale.ROOT)))
				.withStyle(FamilySigils.familyColor(family));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		FamilyResonanceManager.Family family = FamilySigils.familyOf(stack);
		if (family == null) {
			tooltip.accept(Component.translatable("tooltip.extra-enchantry.family_inscription.invalid")
					.withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		tooltip.accept(Component.translatable("tooltip.extra-enchantry.family_inscription",
				Component.translatable("family.extra-enchantry." + family.name().toLowerCase(Locale.ROOT)))
				.withStyle(ChatFormatting.GRAY));
	}
}
