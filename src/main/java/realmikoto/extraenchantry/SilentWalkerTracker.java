package realmikoto.extraenchantry;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「无声的行者」（1.8.5「心」隐藏进度，§5.5）：幽渊内的静默挑战。
 *
 * 规则（设计原文：「全程不发出任何声纹通关回声层」）：
 * 在【回声层（Y 64–128）】内持续保持【零声纹】累计 5 分钟——
 * 任何有效声纹（普通移动/冲刺/跳跃/受伤/攻击/发声键…）或离开回声层都会重置。
 * 潜行（2→非零会重置！必须完全静默路径：静默附魔/无相调式/静默之石/静默区）辅助。
 */
public final class SilentWalkerTracker {

	/** 累计静默 tick（仅当处于回声层且最近声纹为 0 时累积） */
	private static final Map<UUID, Integer> SILENT_TICKS = new ConcurrentHashMap<>();

	/** 目标：5 分钟（6000 tick） */
	private static final int GOAL_TICKS = 20 * 60 * 5;

	private SilentWalkerTracker() {
	}

	/** EchoManager.tick 每玩家每 tick 调用：以最近声纹强度结算 */
	public static void tick(ServerPlayer player) {
		if (!AbyssKey.isIn(player)) {
			SILENT_TICKS.remove(player.getUUID());
			return;
		}
		boolean inEchoLayer = AbyssSpawning.layerOf(player) == 1;
		// 静默 = 最近声纹为 0，或距上次发声已超过 2 秒（"此刻没在响"）
		boolean zeroEcho = EchoManager.lastEcho(player) <= 0.0F
				|| EchoManager.ticksSinceEcho(player) > 40;
		if (inEchoLayer && zeroEcho) {
			int ticks = SILENT_TICKS.merge(player.getUUID(), 1, Integer::sum);
			if (ticks == GOAL_TICKS) {
				award(player);
			}
		} else {
			SILENT_TICKS.remove(player.getUUID());
		}
	}

	private static void award(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		AdvancementHolder holder = server.getAdvancements().get(
				Identifier.fromNamespaceAndPath(ExtraEnchantry.MOD_ID, "hollow/silent_walker"));
		if (holder != null) {
			player.getAdvancements().award(holder, "silent");
		}
		player.sendSystemMessage(Component.translatable("message.extra-enchantry.silent_walker.award"));
	}

	/** 登出 / 服务器停止清理 */
	public static void onDisconnect(UUID playerId) {
		SILENT_TICKS.remove(playerId);
	}

	public static void onServerStopped() {
		SILENT_TICKS.clear();
	}
}
