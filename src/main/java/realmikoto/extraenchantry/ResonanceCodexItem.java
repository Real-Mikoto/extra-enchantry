package realmikoto.extraenchantry;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 共鸣秘典（1.3.0「铭刻与试炼」§2.1）：八系共鸣的统一状态入口。
 *
 * 交互（不做自定义 Screen，纯聊天组件输出）：
 * <ul>
 *   <li>普通右键：输出「总览」页——八系累计等级与档位；</li>
 *   <li>潜行右键：循环切换 总览 → 家族详情 → 试炼 三页（页码运行时按玩家记忆）；</li>
 *   <li>家族详情页对 FULL 家族提供可点击「铭刻」按钮（{@code /extraenchantry attune}）。</li>
 * </ul>
 *
 * 口径约束（P0）：全部数值来自 {@link FamilyResonanceManager} 的唯一计算结果，
 * 本类不做任何独立的等级统计；高频查询不播放音效（档位变化反馈仍在扫描侧）。
 */
public final class ResonanceCodexItem extends Item {

	/** 页码：0 总览 / 1 家族详情 / 2 试炼（运行时，不持久化） */
	private static final Map<UUID, Integer> PAGES = new ConcurrentHashMap<>();

	private static final int PAGE_COUNT = 3;

	/** 玩家登出清理（DISCONNECT 调用）：页码状态不跨会话残留 */
	public static void onDisconnect(UUID playerId) {
		PAGES.remove(playerId);
	}

	public ResonanceCodexItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			int page;
			if (player.isShiftKeyDown()) {
				page = (PAGES.getOrDefault(player.getUUID(), 0) + 1) % PAGE_COUNT;
			} else {
				page = 0;
			}
			PAGES.put(player.getUUID(), page);
			sendPage(serverPlayer, page);
			// 首次打开秘典 → 授予「初见共鸣」引导进度（1.3.1，award 幂等）
			OnboardingManager.onCodexOpened(serverPlayer);
		}
		return InteractionResult.SUCCESS;
	}

	/** 首次合成秘典 → 翻页操作提示（1.3.1「铭文纪元」） */
	@Override
	public void onCraftedBy(ItemStack stack, Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			OnboardingManager.onCodexCrafted(serverPlayer);
		}
	}

	// ============ 页面输出（包内可见：/extraenchantry resonance 命令复用同一口径） ============

	/** 输出指定页（0 总览 / 1 家族详情 / 2 试炼）——秘典右键与查询命令共用 */
	static void sendPage(ServerPlayer player, int page) {
		MutableComponent out = Component.empty();
		switch (page) {
			case 1 -> appendFamilyDetails(player, out);
			case 2 -> appendTrials(player, out);
			default -> appendOverview(player, out);
		}
		out.append(Component.translatable("codex.extra-enchantry.footer")
				.withStyle(ChatFormatting.DARK_GRAY));
		player.sendSystemMessage(out);
	}

	private static void appendOverview(ServerPlayer player, MutableComponent out) {
		out.append(Component.translatable("codex.extra-enchantry.header.overview")
				.withStyle(ChatFormatting.LIGHT_PURPLE)).append("\n");
		for (FamilyResonanceManager.Family family : FamilyResonanceManager.Family.values()) {
			int total = FamilyResonanceManager.familyTotal(player, family);
			FamilyResonanceManager.Tier tier = FamilyResonanceManager.tierOf(player, family);
			out.append(Component.translatable("codex.extra-enchantry.line.overview",
					familyName(family), total, tierText(tier))
					.withStyle(tier == FamilyResonanceManager.Tier.FULL ? ChatFormatting.WHITE : ChatFormatting.GRAY))
					.append("\n");
		}
	}

	/** 页 1：家族详情——等级/差值/主调，FULL 家族附可点击铭刻按钮 */
	private static void appendFamilyDetails(ServerPlayer player, MutableComponent out) {
		out.append(Component.translatable("codex.extra-enchantry.header.details")
				.withStyle(ChatFormatting.LIGHT_PURPLE)).append("\n");
		for (FamilyResonanceManager.Family family : FamilyResonanceManager.Family.values()) {
			int total = FamilyResonanceManager.familyTotal(player, family);
			FamilyResonanceManager.Tier tier = FamilyResonanceManager.tierOf(player, family);
			int toNext = switch (tier) {
				case NONE -> Math.max(0, ResonanceConfig.rules(family).partialThreshold() - total);
				case PARTIAL -> Math.max(0, ResonanceConfig.rules(family).fullThreshold() - total);
				case FULL -> 0;
			};
			out.append(Component.translatable("codex.extra-enchantry.line.details",
					familyName(family), total, toNext, attuneText(player, family))
					.withStyle(tier == FamilyResonanceManager.Tier.FULL ? ChatFormatting.WHITE : ChatFormatting.GRAY));
			// FULL 且尚未铭刻该家族 → 附加可点击「铭刻」按钮
			if (tier == FamilyResonanceManager.Tier.FULL
					&& AttunementManager.attunedFamily(player) != family) {
				out.append(Component.literal(" "))
					.append(Component.translatable("codex.extra-enchantry.attune.button")
								.withStyle(style -> style
										.withColor(ChatFormatting.GREEN)
										.withClickEvent(new ClickEvent.RunCommand(
												"/extraenchantry attune " + family.name().toLowerCase(Locale.ROOT)))));
			}
			out.append("\n");
		}
	}

	/** 页 2：试炼——八项试炼名、完成状态与进行中窗口的实时进度（修复 #28） */
	private static void appendTrials(ServerPlayer player, MutableComponent out) {
		out.append(Component.translatable("codex.extra-enchantry.header.trials")
				.withStyle(ChatFormatting.LIGHT_PURPLE)).append("\n");
		for (FamilyResonanceManager.Family family : FamilyResonanceManager.Family.values()) {
			String path = family.name().toLowerCase(Locale.ROOT);
			boolean done = FamilyTrialsManager.isTrialComplete(player, family);
			out.append(Component.translatable("codex.extra-enchantry.line.trial",
					Component.translatable("trial.extra-enchantry." + path),
					Component.translatable(done
							? "codex.extra-enchantry.trial.done"
							: "codex.extra-enchantry.trial.undone"))
					.withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GRAY));
			if (!done) {
				// 修复 #28：进行中的窗口显示当前进度 / 目标（数值来自 FamilyTrialsManager 唯一口径）
				String progress = FamilyTrialsManager.progressText(player, family);
				if (!progress.isEmpty()) {
					out.append(Component.literal("  [" + progress + "]")
							.withStyle(ChatFormatting.AQUA));
				}
			}
			out.append("\n");
		}
	}

	// ============ 文案辅助 ============

	private static MutableComponent familyName(FamilyResonanceManager.Family family) {
		return Component.translatable("family.extra-enchantry." + family.name().toLowerCase(Locale.ROOT));
	}

	private static MutableComponent tierText(FamilyResonanceManager.Tier tier) {
		return Component.translatable("codex.extra-enchantry.tier." + tier.name().toLowerCase(Locale.ROOT));
	}

	/** 主调列显示：— / 当前主调 / 主调（暂停） */
	private static MutableComponent attuneText(ServerPlayer player, FamilyResonanceManager.Family family) {
		FamilyResonanceManager.Family attuned = AttunementManager.attunedFamily(player);
		if (attuned != family) {
			return Component.translatable("codex.extra-enchantry.attune.none");
		}
		boolean active = AttunementManager.attunementActive(player, family);
		return Component.translatable(active
				? "codex.extra-enchantry.attune.active"
				: "codex.extra-enchantry.attune.paused", familyName(family));
	}
}
