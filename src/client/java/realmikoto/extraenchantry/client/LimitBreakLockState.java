package realmikoto.extraenchantry.client;

import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import realmikoto.extraenchantry.ExtraEnchantry;
import realmikoto.extraenchantry.client.mixin.ClientAdvancementsAccessor;

/**
 * 破限锁定态（客户端本地判定）：本地玩家未完成「无敌」进度即视为锁定。
 * 进度状态由服务器同步进 {@code ClientAdvancements#progress}，渲染线程直接查询。
 */
public final class LimitBreakLockState {

	private LimitBreakLockState() {
	}

	/** 缓存有效期（毫秒）：热路径（每字形 accept）调用，1 秒缓存无感知 */
	private static final long CACHE_MS = 1000;

	private static boolean cachedLocked = true;
	private static long cachedAt;

	/** 本地玩家是否未解锁破限（1 秒缓存：进度未同步/未完成均视为锁定） */
	public static boolean isLocked() {
		long now = System.currentTimeMillis();
		if (now - cachedAt < CACHE_MS) {
			return cachedLocked;
		}
		cachedLocked = query();
		cachedAt = now;
		return cachedLocked;
	}

	private static boolean query() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.getConnection() == null) {
			return true;
		}
		ClientAdvancements advancements = minecraft.getConnection().getAdvancements();
		AdvancementHolder holder = advancements.get(ExtraEnchantry.id("hidden_challenges/defeat_limit_break_cavalry"));
		if (holder == null) {
			return true;
		}
		AdvancementProgress progress = ((ClientAdvancementsAccessor) advancements).extraenchantry$progress().get(holder);
		return progress == null || !progress.isDone();
	}
}
