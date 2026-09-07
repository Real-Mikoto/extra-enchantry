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
 * 来者手札（1.3.1「铭文纪元」§3.1）：动态 lore 容器。
 *
 * 右键翻阅——按玩家持久化的 lore 触发状态开放页码：
 * <ul>
 *   <li>第 1 页（默认）：铭文纪元总纲 + 秘典合成引导；</li>
 *   <li>第 2 页：每个首次达成 FULL 的家族，追加该家族铭文（家族铭文物被丢弃也可在此重读）；</li>
 *   <li>第 3 页：浩劫四幕全部完成后追加「极限之器」铭刻图样；</li>
 *   <li>第 4 页：达成大共鸣者后追加「铭文纪元·终章」。</li>
 * </ul>
 *
 * 页码进度存于玩家（LoreTriggerManager attachment），物品本身无状态——
 * 手札被丢弃不影响解锁，重新获得（或补发）后仍按最新进度翻阅。
 */
public class WelcomeLetterItem extends Item {

	public WelcomeLetterItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendSystemMessage(buildLetter(serverPlayer));
		}
		return InteractionResult.SUCCESS;
	}

	/** 按玩家 lore 进度拼装手札全文 */
	private static MutableComponent buildLetter(ServerPlayer player) {
		MutableComponent out = Component.empty();
		appendPage(out, LoreLoader.letterIntro());

		// 第 2 页：已解锁的家族铭文（FULL 触发过的家族各一段）
		boolean anyFamily = false;
		for (FamilyResonanceManager.Family family : FamilyResonanceManager.Family.values()) {
			if (LoreTriggerManager.hasFired(player, LoreTriggerManager.familyFull(family))) {
				if (!anyFamily) {
					anyFamily = true;
					out.append(Component.translatable("lore.extra-enchantry.letter.family_header")
							.withStyle(ChatFormatting.LIGHT_PURPLE)).append("\n");
				}
				LoreLoader.FamilyInscription inscription = LoreLoader.inscription(family);
				out.append(Component.literal("◈ " + inscription.title())
						.withStyle(FamilySigils.familyColor(family))).append("\n");
				for (String line : inscription.lines()) {
					out.append(Component.literal(line).withStyle(ChatFormatting.GRAY)).append("\n");
				}
			}
		}

		// 第 3 页：极限之器铭刻图样（浩劫全部完成）
		if (LoreTriggerManager.hasFired(player, LoreTriggerManager.CATACLYSM_COMPLETE)) {
			appendPage(out, LoreLoader.letterCataclysm());
		}

		// 第 4 页：终章（大共鸣者）
		if (LoreTriggerManager.hasFired(player, LoreTriggerManager.GRAND_RESONATOR)) {
			appendPage(out, LoreLoader.letterFinale());
		}

		out.append(Component.translatable("lore.extra-enchantry.letter.footer")
				.withStyle(ChatFormatting.DARK_GRAY));
		return out;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.extra-enchantry.welcome_letter")
				.withStyle(ChatFormatting.GRAY));
	}

	private static void appendPage(MutableComponent out, LoreLoader.LetterPage page) {
		out.append(Component.literal("── " + page.title() + " ──")
				.withStyle(ChatFormatting.LIGHT_PURPLE)).append("\n");
		for (String line : page.lines()) {
			out.append(Component.literal(line).withStyle(ChatFormatting.GRAY)).append("\n");
		}
	}
}
