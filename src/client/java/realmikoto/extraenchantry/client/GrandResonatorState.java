package realmikoto.extraenchantry.client;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import realmikoto.extraenchantry.ExtraEnchantry;
import realmikoto.extraenchantry.client.mixin.ClientAdvancementsAccessor;

/**
 * 大共鸣者标记（客户端本地判定，1.3.0 P1）：本地玩家完成
 * 「大共鸣者」进度（family_trials/grand_resonator：集齐八枚家族铭印）
 * 即获得棱彩臻藏资格——本模附魔书与共鸣秘典恒显附魔光效。
 *
 * 与 {@link CollectorState} 同构：进度状态由服务器同步进
 * {@code ClientAdvancements#progress}，进度本身即服务端持久化标记，获得不撤销；
 * 缺少客户端模组时仅视觉降级，服务端逻辑不受影响。
 */
public final class GrandResonatorState {

	private GrandResonatorState() {
	}

	/** 缓存有效期（毫秒）：热路径（hasFoil 每物品每帧）调用，1 秒缓存无感知 */
	private static final long CACHE_MS = 1000;

	private static boolean cached;
	private static long cachedAt;

	/** 本地玩家是否拥有大共鸣者资格（1 秒缓存；兼容新旧两棵树的节点） */
	public static boolean isGrandResonator() {
		long now = System.currentTimeMillis();
		if (now - cachedAt < CACHE_MS) {
			return cached;
		}
		cached = query();
		cachedAt = now;
		return cached;
	}

	private static boolean query() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.getConnection() == null) {
			return false;
		}
		ClientAdvancements advancements = minecraft.getConnection().getAdvancements();
		// 1.7.0 重定向后正式节点在 lineage 树；family_trials 节点保留兼容旧存档已完成的进度
		AdvancementHolder holder = advancements.get(ExtraEnchantry.id("lineage/grand_resonator"));
		if (holder != null) {
			AdvancementProgress progress = ((ClientAdvancementsAccessor) advancements)
					.extraenchantry$progress().get(holder);
			if (progress != null && progress.isDone()) {
				return true;
			}
		}
		AdvancementHolder legacy = advancements.get(ExtraEnchantry.id("family_trials/grand_resonator"));
		if (legacy == null) {
			return false;
		}
		AdvancementProgress legacyProgress = ((ClientAdvancementsAccessor) advancements)
				.extraenchantry$progress().get(legacy);
		return legacyProgress != null && legacyProgress.isDone();
	}
}
