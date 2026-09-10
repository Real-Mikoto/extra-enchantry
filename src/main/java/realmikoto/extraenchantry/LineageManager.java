package realmikoto.extraenchantry;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 谱系主线（1.7.0「谱系与传承」§1）：lineage/ 树的代码授予与「谱系圆满」终局判定。
 *
 * lineage 树四章 16 节点：数据驱动 2 个（root / first_book / first_sigil 按 inventory_changed），
 * 其余 13 个由本类在对应系统的完成时机授予（requirements 跨树引用不可行——26.2
 * AdvancementRequirements 仅绑定本节点 criteria，见设计 §5.1 验证结论①）。
 *
 * 授予来源（分散在各系统完成处调用本类静态方法）：
 *   - FamilyResonanceManager 升档钩子 → first_partial / first_full
 *   - AttunementManager.tryAttune 成功 → first_attune
 *   - FamilySigils.grant（试炼铭印）→ first_sigil（补授）/ two_sigils / 试炼章计数
 *   - FamilyTrialsManager.complete → 试炼章（first_trial / four_trials / all_trials）
 *   - EliteEncounterManager.start / startChallenged → first_realm / first_war
 *   - 觉醒击杀 → first_awakened
 *   - ConvergenceManager.succeed → convergence
 *   - 大共鸣者进度完成 → grand_resonator
 *   - 「谱系圆满」= 八试炼 + 大共鸣者 + 归一 三者全 done（本类轮询判定）
 */
public final class LineageManager {

	// ============ lineage 树进度常量（advancement/lineage/ 子目录路径） ============

	private static ResourceKey<net.minecraft.advancements.Advancement> key(String name) {
		return ResourceKey.create(Registries.ADVANCEMENT, ExtraEnchantry.id("lineage/" + name));
	}

	private static final ResourceKey<net.minecraft.advancements.Advancement> FIRST_PARTIAL = key("first_partial");
	private static final ResourceKey<net.minecraft.advancements.Advancement> FIRST_FULL = key("first_full");
	private static final ResourceKey<net.minecraft.advancements.Advancement> FIRST_ATTUNE = key("first_attune");
	private static final ResourceKey<net.minecraft.advancements.Advancement> TWO_SIGILS = key("two_sigils");
	private static final ResourceKey<net.minecraft.advancements.Advancement> FIRST_TRIAL = key("first_trial");
	private static final ResourceKey<net.minecraft.advancements.Advancement> FOUR_TRIALS = key("four_trials");
	private static final ResourceKey<net.minecraft.advancements.Advancement> ALL_TRIALS = key("all_trials");
	private static final ResourceKey<net.minecraft.advancements.Advancement> FIRST_REALM = key("first_realm");
	private static final ResourceKey<net.minecraft.advancements.Advancement> FIRST_WAR = key("first_war");
	private static final ResourceKey<net.minecraft.advancements.Advancement> FIRST_AWAKENED = key("first_awakened");
	private static final ResourceKey<net.minecraft.advancements.Advancement> CONVERGENCE = key("convergence");
	private static final ResourceKey<net.minecraft.advancements.Advancement> GRAND_RESONATOR = key("grand_resonator");
	private static final ResourceKey<net.minecraft.advancements.Advancement> LINEAGE_COMPLETE = key("lineage_complete");

	/** 大共鸣者进度（lineage 树）——对外公开以便 FamilySigils 等检测首次达成 */
	public static final ResourceKey<net.minecraft.advancements.Advancement> GRAND_RESONATOR_KEY = GRAND_RESONATOR;

	/** 大共鸣者进度（family_trials 树既有节点）——谱系圆满的组件之一 */
	private static final ResourceKey<net.minecraft.advancements.Advancement> TRIALS_GRAND =
			ResourceKey.create(Registries.ADVANCEMENT,
					ExtraEnchantry.id("lineage/grand_resonator"));

	/** 归一之战进度节点（1.7.0 重定向至 lineage 树，避免与 family_trials/grand_resonator 重复） */
	private static final ResourceKey<net.minecraft.advancements.Advancement> CONVERGENCE_DONE =
			ResourceKey.create(Registries.ADVANCEMENT, ExtraEnchantry.id("lineage/convergence"));

	/** 1.6.0+ 隐藏进度（hidden_challenges 树） */
	private static final ResourceKey<net.minecraft.advancements.Advancement> FIVE_REALMS_VICTOR =
			ResourceKey.create(Registries.ADVANCEMENT, ExtraEnchantry.id("hidden_challenges/five_realms_victor"));
	private static final ResourceKey<net.minecraft.advancements.Advancement> FINAL_AFTERGLOW =
			ResourceKey.create(Registries.ADVANCEMENT, ExtraEnchantry.id("hidden_challenges/final_afterglow"));
	private static final ResourceKey<net.minecraft.advancements.Advancement> SOUL_COLLECTOR =
			ResourceKey.create(Registries.ADVANCEMENT, ExtraEnchantry.id("hidden_challenges/soul_collector"));

	/** 五境觉醒击杀记录（per-player，达成五个不同境 → 授予五境全胜） */
	private static final Map<UUID, Set<String>> AWAKENED_REALMS_KILLED = new ConcurrentHashMap<>();

	/** 试炼完成进度（family_trials/ 单系节点，与 FamilySigils 同路径） */
	private static final String TRIAL_ADV_SUFFIX = "";

	/** 玩家已持有铭印计数缓存（grant 处累加——幂等：进度授予天然幂等，计数取进度表更准） */
	private static final Map<UUID, Integer> SIGIL_COUNTS = new ConcurrentHashMap<>();

	private LineageManager() {
	}

	// ============ 授予入口（各系统调用） ============

	/** 家族升至 PARTIAL（首次）——FamilyResonanceManager 升档钩子调用 */
	public static void onFirstPartial(ServerPlayer player) {
		Advancements.award(player, FIRST_PARTIAL);
	}

	/** 家族升至 FULL（首次）——FamilyResonanceManager 升档钩子调用 */
	public static void onFirstFull(ServerPlayer player) {
		Advancements.award(player, FIRST_FULL);
	}

	/** 首次铭刻成功——AttunementManager.tryAttune OK 分支调用 */
	public static void onFirstAttune(ServerPlayer player) {
		Advancements.award(player, FIRST_ATTUNE);
	}

	/** 铭印授予（FamilySigils.grant 调用）：两枚节点按进度表实况授予 */
	public static void onSigilGranted(ServerPlayer player) {
		int count = countDoneTrials(player);
		if (count >= 2) {
			Advancements.award(player, TWO_SIGILS);
		}
	}

	/** 试炼完成（FamilyTrialsManager.complete 调用）：试炼章三节点按完成数推进 */
	public static void onTrialComplete(ServerPlayer player, FamilyResonanceManager.Family family) {
		int done = countDoneTrials(player);
		if (done >= 1) {
			Advancements.award(player, FIRST_TRIAL);
		}
		if (done >= 4) {
			Advancements.award(player, FOUR_TRIALS);
		}
		if (done >= 8) {
			Advancements.award(player, ALL_TRIALS);
		}
	}

	/** 触遇五境（EliteEncounterManager.start / startChallenged 调用） */
	public static void onRealmEncounter(ServerPlayer player) {
		Advancements.award(player, FIRST_REALM);
	}

	/** 首次宣战（startChallenged 成功路径调用） */
	public static void onFirstWar(ServerPlayer player) {
		Advancements.award(player, FIRST_WAR);
	}

	/** 首杀觉醒境主（EliteEncounterManager.onLordDeath 觉醒分支调用） */
	public static void onFirstAwakenedSlain(ServerPlayer player) {
		Advancements.award(player, FIRST_AWAKENED);
	}

	/** 归一之战完成（ConvergenceManager.succeed 调用） */
	public static void onConvergenceDone(ServerPlayer player) {
		Advancements.award(player, CONVERGENCE);
		checkLineageComplete(player);
	}

	/** 大共鸣者达成（FamilySigils.awardGrandResonatorIfComplete 调用） */
	public static void onGrandResonator(ServerPlayer player) {
		Advancements.award(player, GRAND_RESONATOR);
		checkLineageComplete(player);
	}

	// ============ 谱系圆满（终局节点：八试炼 + 大共鸣者 + 归一 全 done） ============

	/** 三组件查询（谱系圆满判定 + status 命令复用）：已完成试炼数 */
	public static int countDoneTrials(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return 0;
		}
		int done = 0;
		for (FamilyResonanceManager.Family family : FamilyResonanceManager.Family.values()) {
			AdvancementHolder holder = server.getAdvancements().get(
					net.minecraft.resources.Identifier.fromNamespaceAndPath(
							ExtraEnchantry.MOD_ID,
							"family_trials/" + family.name().toLowerCase(java.util.Locale.ROOT)));
			if (holder != null && player.getAdvancements().getOrStartProgress(holder).isDone()) {
				done++;
			}
		}
		return done;
	}

	/** 谱系圆满判定：八试炼 + 大共鸣者 + 归一之战三者全 done */
	public static void checkLineageComplete(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null || countDoneTrials(player) < 8) {
			return;
		}
		AdvancementHolder grand = server.getAdvancements().get(
				TRIALS_GRAND.identifier());
		AdvancementHolder convergence = server.getAdvancements().get(
				CONVERGENCE_DONE.identifier());
		if (grand != null && convergence != null
				&& player.getAdvancements().getOrStartProgress(grand).isDone()
				&& player.getAdvancements().getOrStartProgress(convergence).isDone()) {
			Advancements.award(player, LINEAGE_COMPLETE);
			player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
					"message.extra-enchantry.lineage.complete"));
		}
	}

	// ============ 1.6.0+ 隐藏进度授予入口 ============

	/** 觉醒境主击杀记录（per-player 集合）。EliteEncounterManager.onLordDeath 觉醒分支调用。
	 *  集齐五个不同境即授予「五境全胜」隐藏进度。 */
	public static void noteAwakenedRealmKill(ServerPlayer player, String realmId) {
		Set<String> killed = AWAKENED_REALMS_KILLED.computeIfAbsent(player.getUUID(),
				key -> ConcurrentHashMap.newKeySet());
		killed.add(realmId);
		if (killed.size() >= EncounterDef.ALL.size()) {
			Advancements.award(player, FIVE_REALMS_VICTOR);
		}
	}

	/** 归一之战终局击杀瞬间生命 ≤5 并存活（ConvergenceManager.succeed 调用）。 */
	public static void onFinalAfterglow(ServerPlayer player) {
		if (player.isAlive() && player.getHealth() <= 5.0F) {
			Advancements.award(player, FINAL_AFTERGLOW);
		}
	}

	/** 五魂俱全：持有 41–45 器魂附魔（任意等级，附魔书或装备形态均可）。
	 *  RealmTreasures.dropAwakenedLoot 与 on-tick 都会调用：附魔书落入背包立即触发，
	 *  玩家把书附到装备后由 on-tick 兜底扫描。 */
	public static void checkSoulCollector(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		boolean have = hasAnyLevel(player, ExtraEnchantry.CLEARSIGHT)
				&& hasAnyLevel(player, ExtraEnchantry.WITHERBLADE)
				&& hasAnyLevel(player, ExtraEnchantry.TIDESURGE)
				&& hasAnyLevel(player, ExtraEnchantry.HEXBREAK)
				&& hasAnyLevel(player, ExtraEnchantry.VOIDBLINK);
		if (have) {
			Advancements.award(player, SOUL_COLLECTOR);
		}
	}

	/** 玩家是否在任何物品（背包/装备）上持有指定附魔（任意等级） */
	private static boolean hasAnyLevel(ServerPlayer player, ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
		// 遍历 Container 接口覆盖全部槽位（26.2 Inventory.items 为私有，装备槽经映射同样可达）
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (has(player.getInventory().getItem(i), key)) {
				return true;
			}
		}
		for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
			if (has(player.getItemBySlot(slot), key)) {
				return true;
			}
		}
		return false;
	}

	private static boolean has(net.minecraft.world.item.ItemStack stack,
			ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		var stored = stack.get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS);
		if (stored != null) {
			for (var holder : stored.keySet()) {
				if (holder.is(key)) {
					return true;
				}
			}
		}
		var ench = stack.getEnchantments();
		for (var holder : ench.keySet()) {
			if (holder.is(key)) {
				return true;
			}
		}
		return false;
	}

	// 1.7.3 性能：取消每 2 秒的全背包+装备轮询扫描（checkSoulCollector 含
	// 4 个附魔 × 40+ 槽位组件遍历）；改为纯事件驱动——
	//   附魔书入包：RealmTreasures.dropAwakenedLoot / dropLordLoot 掉落时机即时触发；
	//   装备形态：LivingEntityMixin#tick 的活力注入点旁路（仅 ServerPlayer、1 秒节流）。
}
