package realmikoto.extraenchantry.client;

import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import realmikoto.extraenchantry.ExtraEnchantry;
import realmikoto.extraenchantry.client.mixin.ClientAdvancementsAccessor;

/**
 * 臻藏标记（客户端本地判定）：本地玩家完成「收藏家 · 全家福」成就
 * （collector/all：集齐八系家族附魔书）即获得臻藏资格。
 * 进度状态由服务器同步进 {@code ClientAdvancements#progress}，
 * 无需额外网络包；成就本身即服务端持久化标记，获得不撤销。
 */
public final class CollectorState {

	private CollectorState() {
	}

	/** 缓存有效期（毫秒）：热路径（每字形 accept）调用，进度同步频率极低，1 秒缓存无感知 */
	private static final long CACHE_MS = 1000;

	private static boolean cached;
	private static long cachedAt;

	/** 本地玩家是否拥有臻藏资格（1 秒缓存：热路径每字形调用，避免 Map 查找放大） */
	public static boolean isCollector() {
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
		AdvancementHolder holder = advancements.get(ExtraEnchantry.id("collector/all"));
		if (holder == null) {
			return false;
		}
		AdvancementProgress progress = ((ClientAdvancementsAccessor) advancements).extraenchantry$progress().get(holder);
		return progress != null && progress.isDone();
	}
}
