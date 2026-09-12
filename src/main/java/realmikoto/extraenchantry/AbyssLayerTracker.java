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
 * 四层探索追踪（v1.8.0.2 补齐，设计稿 §4.2）：深度 = 年代。
 *
 * 层划分（与 AbyssSpawning 一致）：
 * 潮汐层 Y<64 / 回声层 64–128 / 记忆层 128–176 / 渊心 Y>176
 *
 * 行为：玩家首次抵达某层 → 授予对应进度 + 播报层名与叙事一句。
 */
public final class AbyssLayerTracker {

	private static final Map<UUID, Integer> LAST_LAYER = new ConcurrentHashMap<>();

	private static final String[] NAMES = {"潮汐层", "回声层", "记忆层", "渊心"};
	private static final String[] FLAVOR = {
			"§8潮汐层——沉没的初民遗迹在水下呼吸。",
			"§7回声层——声纹在这里传得最远，也最危险。",
			"§5记忆层——你踩在封印纪元的骨上。",
			"§5渊心——八相之外的那片空，就在这里。"
	};
	private static final String[] ADV = {"layer_tidal", "layer_echo", "layer_memory", "layer_heart"};

	private AbyssLayerTracker() {
	}

	public static void track(ServerPlayer player) {
		int layer = AbyssSpawning.layerOf(player);
		Integer last = LAST_LAYER.put(player.getUUID(), layer);
		if (last != null && last == layer) {
			return;
		}
		// 首次进入该层（或跨层）：授予进度 + 播报
		award(player, ADV[layer]);
		player.sendSystemMessage(Component.literal(FLAVOR[layer]));
	}

	private static void award(ServerPlayer player, String node) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		AdvancementHolder holder = server.getAdvancements().get(
				Identifier.fromNamespaceAndPath(ExtraEnchantry.MOD_ID, "hollow/" + node));
		if (holder != null) {
			player.getAdvancements().award(holder, "tick");
		}
	}

	public static void onDisconnect(UUID playerId) {
		LAST_LAYER.remove(playerId);
	}

	public static void onServerStopped() {
		LAST_LAYER.clear();
	}
}
