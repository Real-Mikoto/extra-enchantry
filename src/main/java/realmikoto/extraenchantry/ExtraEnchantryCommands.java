package realmikoto.extraenchantry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * /extraenchantry 命令组（1.3.0「铭刻与试炼」§2.1/§2.2/§2.5）：
 *
 * <ul>
 *   <li>{@code /extraenchantry resonance [player]}：八系状态查询（无物品回退入口）。
 *       普通玩家只能查自己；权限等级 2 可指定他人，目标不存在时 Brigadier 报明确错误；</li>
 *   <li>{@code /extraenchantry attune <family>}：铭刻主调（无权限门槛，只能对自己），
 *       FULL 门槛与冷却由 {@link AttunementManager#tryAttune} 统一校验与反馈；</li>
 *   <li>{@code /extraenchantry debug resonance <player>}：管理员诊断（权限等级 2），
 *       输出八系等级/档位/主调/冷却/缓存脏状态与试炼运行时计数。</li>
 * </ul>
 *
 * 共鸣查询复用 {@link ResonanceCodexItem} 的页面输出（P0 唯一口径，命令无第二套算法）。
 */
public final class ExtraEnchantryCommands {

	private ExtraEnchantryCommands() {
	}

	/** 家族名 Tab 补全（attune <family>） */
	private static final SuggestionProvider<CommandSourceStack> FAMILY_SUGGESTIONS = (context, builder) -> {
		for (FamilyResonanceManager.Family family : FamilyResonanceManager.Family.values()) {
			builder.suggest(family.name().toLowerCase(Locale.ROOT));
		}
		return builder.buildFuture();
	};

	/** 挂 Fabric {@code CommandRegistrationCallback}（onInitialize 调用一次） */
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) ->
				registerCommands(dispatcher));
	}

	private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("extraenchantry")
				// ---- resonance [player]：状态查询 ----
				.then(Commands.literal("resonance")
						.executes(ctx -> {
							ServerPlayer self = ctx.getSource().getPlayerOrException();
							return showResonance(self, self);
						})
						.then(Commands.argument("player", EntityArgument.player())
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(ctx -> {
									ServerPlayer viewer = ctx.getSource().getPlayerOrException();
									return showResonance(viewer, EntityArgument.getPlayer(ctx, "player"));
								})))
				// ---- attune <family>：铭刻主调 ----
				.then(Commands.literal("attune")
						.then(Commands.argument("family", StringArgumentType.word())
								.suggests(FAMILY_SUGGESTIONS)
								.executes(ExtraEnchantryCommands::attune)))
				// ---- debug resonance <player>：管理员诊断 ----
				.then(Commands.literal("debug")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("resonance")
								.then(Commands.argument("player", EntityArgument.player())
										.executes(ExtraEnchantryCommands::debugResonance)))));
	}

	// ============ 子命令实现 ============

	/** resonance：向 viewer 展示 target 的八系状态（viewer == target 时为普通玩家自查） */
	private static int showResonance(ServerPlayer viewer, ServerPlayer target) {
		if (viewer == target) {
			// 与秘典潜行右键同级的完整输出：总览页
			ResonanceCodexItem.sendPage(viewer, 0);
		} else {
			viewer.sendSystemMessage(Component.translatable("command.extra-enchantry.resonance.of",
					target.getName()).withStyle(ChatFormatting.LIGHT_PURPLE));
			ResonanceCodexItem.sendPage(viewer, 0);
		}
		return 1;
	}

	/** attune：铭刻主调（只能对自己执行） */
	private static int attune(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		FamilyResonanceManager.Family family = parseFamily(StringArgumentType.getString(ctx, "family"));
		if (family == null) {
			ctx.getSource().sendFailure(Component.translatable("command.extra-enchantry.unknown_family",
					StringArgumentType.getString(ctx, "family")));
			return 0;
		}
		AttunementManager.tryAttune(player, family);
		return 1;
	}

	/** debug resonance：八系等级/档位/主调/冷却/缓存/试炼计数一次性输出 */
	private static int debugResonance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
		CommandSourceStack source = ctx.getSource();
		source.sendSuccess(() -> Component.translatable("command.extra-enchantry.debug.header",
				target.getName()), false);
		for (FamilyResonanceManager.Family family : FamilyResonanceManager.Family.values()) {
			String path = family.name().toLowerCase(Locale.ROOT);
			FamilyResonanceManager.Tier tier = FamilyResonanceManager.tierOf(target, family);
			String attuned = AttunementManager.attunedFamily(target) == family
					? (AttunementManager.attunementActive(target, family) ? "active" : "paused") : "-";
			boolean trialDone = FamilyTrialsManager.isTrialComplete(target, family);
			source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
					"  %-7s total=%d tier=%s attune=%s trial=%s",
					path,
					FamilyResonanceManager.familyTotal(target, family),
					tier.name().toLowerCase(Locale.ROOT),
					attuned,
					trialDone ? "done" : "pending")), false);
		}
		source.sendSuccess(() -> Component.literal("  cooldown=" + AttunementManager.cooldownRemaining(target)
				+ "t cache=[" + FamilyResonanceManager.debugCacheState(target) + "]"), false);
		source.sendSuccess(() -> Component.literal("  trials=[" + FamilyTrialsManager.debugState(target) + "]"), false);
		return 1;
	}

	/** 家族名解析（word → 枚举；非法 → null） */
	private static FamilyResonanceManager.Family parseFamily(String word) {
		if (word == null || word.isEmpty()) {
			return null;
		}
		try {
			return FamilyResonanceManager.Family.valueOf(word.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
