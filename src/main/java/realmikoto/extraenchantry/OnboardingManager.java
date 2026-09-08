package realmikoto.extraenchantry;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 被动引导管理器（1.3.1「铭文纪元」§5）：新手指引 + lore 物品派发 + 老玩家追发。
 *
 * 设计原则（DESIGN/1.3.1-design.md §一）：
 * <ul>
 *   <li>被动引导——不强制读 lore、不卡进度；触发时只 chat 提示 + 物品入背包（可丢弃）；</li>
 *   <li>每个触发点仅首次（{@link LoreTriggerManager} 持久化），静默降级可整体关闭提示；</li>
 *   <li>升档兼容——首次登录按已有成就进度追发应得物品（reconcile），幂等安全；</li>
 *   <li>性能——提示检查只在秒级 tick 边界做 O(玩家数) 的已触发查询，
 *       背包扫描仅对未触发「首次拾书」的玩家进行，触发后永不再扫。</li>
 * </ul>
 *
 * 钩子入口（由既有管理器调用，零核心逻辑改动）：
 * {@link #onFamilyPartial} / {@link #onFamilyFull} / {@link #onCataclysmWave} /
 * {@link #onCataclysmComplete} / {@link #onGrandResonator} / {@link #onFirstAttune} /
 * {@link #onCodexOpened} / {@link #onCodexCrafted}。
 */
public final class OnboardingManager {

	/** JOIN → 延迟处理的游戏刻（等待玩家实体与进度就绪，"首次 tick 检测"） */
	private static final int LOGIN_DELAY_TICKS = 40;

	/** 待处理的登录追发（UUID → 处理时刻 gameTime） */
	private static final Map<UUID, Long> PENDING_RECONCILE = new ConcurrentHashMap<>();

	/** 「无敌」（诸界浩劫全部完成）进度：残页追发依据 */
	private static final ResourceKey<net.minecraft.advancements.Advancement> ADVANCE_INVINCIBLE =
			ResourceKey.create(net.minecraft.core.registries.Registries.ADVANCEMENT,
					ExtraEnchantry.id("hidden_challenges/defeat_limit_break_cavalry"));

	/** 「大共鸣者」进度：编年史卷轴追发依据 */
	private static final ResourceKey<net.minecraft.advancements.Advancement> ADVANCE_GRAND_RESONATOR =
			ResourceKey.create(net.minecraft.core.registries.Registries.ADVANCEMENT,
					ExtraEnchantry.id("family_trials/grand_resonator"));

	/** 家族试炼进度：家族铭文追发依据（试炼完成 ⇒ 该家族必然达成过 FULL） */
	private static final ResourceKey<net.minecraft.advancements.Advancement> trialKey(
			FamilyResonanceManager.Family family) {
		return ResourceKey.create(net.minecraft.core.registries.Registries.ADVANCEMENT,
				ExtraEnchantry.id("family_trials/" + family.name().toLowerCase(Locale.ROOT)));
	}

	private OnboardingManager() {
	}

	// ============ 生命周期（ExtraEnchantry 注册） ============

	/** 玩家登录（ServerPlayConnectionEvents.JOIN）：登记延迟追发 */
	public static void onJoin(ServerPlayer player) {
		PENDING_RECONCILE.put(player.getUUID(),
				player.level().getGameTime() + LOGIN_DELAY_TICKS);
	}

	/** 秒级节拍（END_SERVER_TICK）：处理登录追发 + 首次拾书检测 */
	public static void tick(MinecraftServer server) {
		long gameTime = server.overworld().getGameTime();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			// 登录追发
			Long due = PENDING_RECONCILE.get(player.getUUID());
			if (due != null && gameTime >= due) {
				PENDING_RECONCILE.remove(player.getUUID());
				reconcile(player);
			}
			// 首次拾取本模附魔书（触发后永不再扫该玩家背包）
			if (gameTime % 20 == 0 && !LoreTriggerManager.hasFired(player, LoreTriggerManager.BOOK_PICKUP)
					&& hasModEnchantedBook(player)) {
				if (LoreTriggerManager.fireOnce(player, LoreTriggerManager.BOOK_PICKUP)) {
					hint(player, "message.extra-enchantry.onboarding.book_pickup");
				}
			}
		}
	}

	/**
	 * 升级追发（首次登录 + 每次登录幂等重查）：
	 * 首次进游戏派发来者手札；按已有成就进度补发残页 / 编年史 / 家族铭文。
	 */
	private static void reconcile(ServerPlayer player) {
		// 来者手札：仅首次进入世界（1.3.0 升级玩家视为"老新玩家"——同样补上手札）
		int backIssued = 0;
		if (LoreTriggerManager.fireOnce(player, LoreTriggerManager.FIRST_LOGIN)) {
			grantItem(player, new ItemStack(ExtraEnchantry.WELCOME_LETTER));
			hint(player, "message.extra-enchantry.onboarding.welcome");
		}
		// 诸界浩劫已完成 → 追发四张残页
		if (hasAdvancement(player, ADVANCE_INVINCIBLE)) {
			for (int act = 1; act <= 4; act++) {
				if (LoreTriggerManager.fireOnce(player, LoreTriggerManager.cataclysmWave(act))) {
					grantItem(player, shardStack(act));
					backIssued++;
				}
			}
			LoreTriggerManager.fireOnce(player, LoreTriggerManager.CATACLYSM_COMPLETE);
		}
		// 大共鸣者已达成 → 追发编年史卷轴
		if (hasAdvancement(player, ADVANCE_GRAND_RESONATOR)) {
			if (LoreTriggerManager.fireOnce(player, LoreTriggerManager.GRAND_RESONATOR)) {
				grantItem(player, new ItemStack(ExtraEnchantry.CHRONICLE_SCROLL));
				backIssued++;
			}
		}
		// 家族试炼已完成 → 该家族必然达成过 FULL，追发铭文
		for (FamilyResonanceManager.Family family : FamilyResonanceManager.Family.values()) {
			if (hasAdvancement(player, trialKey(family))
					&& LoreTriggerManager.fireOnce(player, LoreTriggerManager.familyFull(family))) {
				grantItem(player, inscriptionStack(family));
				backIssued++;
			}
		}
		// 一次性汇总提示（仅补发到进度物品时；纯新手只收到手札不算"补发"）
		if (backIssued > 0) {
			hint(player, "message.extra-enchantry.onboarding.reconciled", Component.literal(String.valueOf(backIssued)));
		}
	}

	// ============ 钩子（既有管理器调用） ============

	/** 任意家族首次达到 PARTIAL（全局一次）：共鸣入门提示 */
	public static void onFamilyPartial(ServerPlayer player) {
		if (LoreTriggerManager.fireOnce(player, LoreTriggerManager.RESONANCE_PARTIAL)) {
			hint(player, "message.extra-enchantry.onboarding.resonance_partial");
		}
	}

	/** 家族首次达成 FULL：派发家族铭文 + 提示（FamilyResonanceManager 升档处调用） */
	public static void onFamilyFull(ServerPlayer player, FamilyResonanceManager.Family family) {
		if (LoreTriggerManager.fireOnce(player, LoreTriggerManager.familyFull(family))) {
			grantItem(player, inscriptionStack(family));
			hint(player, "message.extra-enchantry.onboarding.family_full",
					Component.translatable("family.extra-enchantry." + family.name().toLowerCase(Locale.ROOT)));
		}
	}

	/** 浩劫一波清空：派发破限残页 + 幕格言（CavalryManager 清波处调用） */
	public static void onCataclysmWave(ServerPlayer player, int wave) {
		if (LoreTriggerManager.fireOnce(player, LoreTriggerManager.cataclysmWave(wave))) {
			grantItem(player, shardStack(wave));
			// 幕格言（数据驱动叙事，与铭印授予同级的结算反馈，不受静默影响）
			player.sendSystemMessage(Component.literal(LoreLoader.act(wave).waveCompleteChat()));
			hint(player, "message.extra-enchantry.onboarding.cataclysm_wave",
					Component.literal(String.valueOf(wave)), Component.literal("4"));
		}
	}

	/** 浩劫全部完成（CavalryManager 成功结算处调用）：铭刻图样收录提示 */
	public static void onCataclysmComplete(ServerPlayer player) {
		if (LoreTriggerManager.fireOnce(player, LoreTriggerManager.CATACLYSM_COMPLETE)) {
			hint(player, "message.extra-enchantry.onboarding.cataclysm_complete");
		}
	}

	/** 达成大共鸣者（FamilySigils 授予处调用）：派发编年史卷轴 */
	public static void onGrandResonator(ServerPlayer player) {
		if (LoreTriggerManager.fireOnce(player, LoreTriggerManager.GRAND_RESONATOR)) {
			grantItem(player, new ItemStack(ExtraEnchantry.CHRONICLE_SCROLL));
			hint(player, "message.extra-enchantry.onboarding.grand_resonator");
		}
	}

	/** 首次铭刻主调（AttunementManager 成功铭刻处调用）：家族格言 + 引导进度 */
	public static void onFirstAttune(ServerPlayer player, FamilyResonanceManager.Family family) {
		if (!LoreTriggerManager.fireOnce(player, LoreTriggerManager.firstAttune())) {
			return;
		}
		awardOnboarding(player, "first_attunement");
		if (LoreLoader.silenced()) {
			return;
		}
		List<String> lines = LoreLoader.firstAttuneLines(family);
		for (String line : lines) {
			player.sendSystemMessage(Component.literal(line)
					.withStyle(FamilySigils.familyColor(family)));
		}
	}

	/** 首次打开共鸣秘典（ResonanceCodexItem 右键处调用）：授予「初见共鸣」 */
	public static void onCodexOpened(ServerPlayer player) {
		awardOnboarding(player, "open_codex");
	}

	/** 首次合成共鸣秘典（ResonanceCodexItem.onCraftedBy 调用）：翻页操作提示 */
	public static void onCodexCrafted(ServerPlayer player) {
		if (LoreTriggerManager.fireOnce(player, LoreTriggerManager.CODEX_CRAFTED)) {
			hint(player, "message.extra-enchantry.onboarding.codex_crafted");
		}
	}

	// ============ 1.4.0「环佩与獠牙」钩子 ============

	/** 首次穿戴任意配饰（AccessoryItem.use 服务端成功路径调用）：授予「环佩琳琅」 */
	public static void onAccessoryEquipped(ServerPlayer player) {
		Advancements.award(player, Advancements.ACCESSORY_ATTIRE);
	}

	/**
	 * 首次合成家族宝石（GemItem.onCraftedBy 调用）：播一句家族格言。
	 * 触发键按宝石区分（gem_crafted:{path}），静默开关沿用 silence_onboarding。
	 */
	public static void onGemCrafted(ServerPlayer player, String gemPath) {
		if (!LoreTriggerManager.fireOnce(player, "gem_crafted:" + gemPath)) {
			return;
		}
		if (LoreLoader.silenced()) {
			return;
		}
		player.sendSystemMessage(Component.translatable("message.extra-enchantry.gem." + gemPath)
				.withStyle(net.minecraft.ChatFormatting.GRAY));
	}

	// ============ 内部辅助 ============

	/** 静默规则：silence_onboarding 开启时跳过 chat（物品照发） */
	private static void hint(ServerPlayer player, String key, Component... args) {
		if (LoreLoader.silenced()) {
			return;
		}
		player.sendSystemMessage(args.length == 0
				? Component.translatable(key)
				: Component.translatable(key, (Object[]) args));
	}

	/** 物品入包：背包满掉落脚下（不丢失、不复制），配一声轻响 */
	private static void grantItem(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
		if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
			FxHelper.play(serverLevel, player, SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
		}
	}

	private static ItemStack inscriptionStack(FamilyResonanceManager.Family family) {
		ItemStack stack = new ItemStack(ExtraEnchantry.FAMILY_INSCRIPTION);
		stack.set(FamilySigils.FAMILY_ID, family.name().toLowerCase(Locale.ROOT));
		return stack;
	}

	private static ItemStack shardStack(int act) {
		ItemStack stack = new ItemStack(ExtraEnchantry.LIMIT_BREAK_SHARD);
		stack.set(LimitBreakShardItem.SHARD_ID, act);
		return stack;
	}

	/** onboarding/ 树代码授予（impossible 触发器，award 幂等） */
	private static void awardOnboarding(ServerPlayer player, String name) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		AdvancementHolder holder = server.getAdvancements().get(ExtraEnchantry.id("onboarding/" + name));
		if (holder != null) {
			player.getAdvancements().award(holder, "triggered");
		}
	}

	/** 进度是否已完成（holder 缺失 → false，数据包未加载时静默跳过） */
	private static boolean hasAdvancement(ServerPlayer player, ResourceKey<net.minecraft.advancements.Advancement> key) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return false;
		}
		AdvancementHolder holder = server.getAdvancements().get(key.identifier());
		if (holder == null) {
			return false;
		}
		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
		return progress.isDone();
	}

	/** 背包中是否持有本模附魔书（stored_enchantments 含本模任意附魔） */
	private static boolean hasModEnchantedBook(ServerPlayer player) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
			if (enchantments == null) {
				continue;
			}
			for (var holder : enchantments.keySet()) {
				if (holder.is(ExtraEnchantry.ANY_FAMILY_TAG)) {
					return true;
				}
			}
		}
		return false;
	}
}
