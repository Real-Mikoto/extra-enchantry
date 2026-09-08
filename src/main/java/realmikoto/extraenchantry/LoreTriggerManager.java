package realmikoto.extraenchantry;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lore 一次性触发器（1.3.1「铭文纪元」§5.2）：被动引导的"仅首次"语义支撑。
 *
 * 每个引导触发点（首次进游戏 / 首次拾取本模附魔书 / 家族首次 FULL / 浩劫波次……）
 * 调用 {@link #fireOnce}——返回 true 表示该玩家此生第一次触发，调用方据此发提示 / 发物品；
 * 之后无论扫描重放多少次（重登、重启、共鸣扫描每秒重检）都不会再触发。
 *
 * 持久化：Fabric Data Attachment（{@code copyOnDeath} 覆盖死亡重生与跨维度），
 * 与 {@link AttunementManager} 同款；旧存档首次加载默认空集。
 */
public final class LoreTriggerManager {

	// ============ 触发器 ID 常量（触发语义见 OnboardingManager 各钩子） ============

	/** 首次进入世界（来者手札派发） */
	public static final String FIRST_LOGIN = "first_login";
	/** 首次拾取本模附魔书 */
	public static final String BOOK_PICKUP = "book_pickup";
	/** 首次合成共鸣秘典 */
	public static final String CODEX_CRAFTED = "codex_crafted";
	/** 任意家族首次达到 PARTIAL（共鸣入门提示，全局一次） */
	public static final String RESONANCE_PARTIAL = "resonance_partial";
	/** 浩劫全部完成（破限解锁） */
	public static final String CATACLYSM_COMPLETE = "cataclysm_complete";
	/** 达成大共鸣者 */
	public static final String GRAND_RESONATOR = "grand_resonator";

	/**
	 * 已触发集合持久化：{@code Set<String>}（DFU 无原生 Set codec，经 List 往返）。
	 */
	private static final AttachmentType<Set<String>> FIRED =
			AttachmentRegistry.<Set<String>>create(ExtraEnchantry.id("lore_triggers"),
					builder -> builder.persistent(Codec.STRING.listOf()
									.xmap(HashSet::new, List::copyOf))
							.copyOnDeath());

	private LoreTriggerManager() {
	}

	/**
	 * 显式触发本类静态初始化（onInitialize 早期调用）：Attachment 注册在静态块中，
	 * 若依赖首个调用方的类加载时机会导致「玩家数据读取时 lore_triggers 尚未注册」——
	 * 已存的触发记录被当未知类型丢弃（Fabric 日志 "Skipping invalid attachments"），
	 * 手札 / 拾书提示等一次性引导每次重进世界重复触发。注册门禁见 v1.3.1 修复。
	 */
	public static void register() {
		// 类加载即完成 AttachmentRegistry.create——本方法体为空，仅保证 <clinit> 执行
	}

	// ============ 核心 API ============

	/**
	 * 触发一次：首次返回 true 并记录；此后恒 false。
	 * 读取-判空-写回非原子，但所有调用都在主服务器线程（共鸣扫描 / 事件钩子），无并发风险。
	 */
	public static boolean fireOnce(ServerPlayer player, String triggerId) {
		if (hasFired(player, triggerId)) {
			return false;
		}
		Set<String> fired = player.getAttachedOrCreate(FIRED, () -> new HashSet<>());
		fired.add(triggerId);
		player.setAttached(FIRED, fired);
		return true;
	}

	/** 该触发器是否已发生过（来者手札翻页 / 追发判定复用） */
	public static boolean hasFired(ServerPlayer player, String triggerId) {
		Set<String> fired = player.getAttached(FIRED);
		return fired != null && fired.contains(triggerId);
	}

	// ============ 组合 ID 辅助 ============

	/** 家族首次 FULL 的触发器 ID */
	public static String familyFull(FamilyResonanceManager.Family family) {
		return "family_full:" + family.name().toLowerCase(java.util.Locale.ROOT);
	}

	/** 浩劫第 act 幕（波）清空的触发器 ID */
	public static String cataclysmWave(int act) {
		return "cataclysm_wave:" + act;
	}

	/** 首次铭刻主调（任意家族）的触发器 ID */
	public static String firstAttune() {
		return "first_attune";
	}
}
