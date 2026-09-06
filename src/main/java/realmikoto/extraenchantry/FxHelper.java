package realmikoto.extraenchantry;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 表现层工具（DESIGN_aesthetics.md）：全模组粒子 / 音效模板与触发节流。
 *
 * 三层反馈预算：L1 常驻 ≤2 粒子/秒、L2 触发 3~16 粒子 + 1 音效、L3 重大时刻 20~40 粒子。
 * 一切粒子走服务端 sendParticles 自动广播；音效统一 SoundSource.PLAYERS。
 */
public final class FxHelper {

	private FxHelper() {
	}

	// ============ 粒子模板 ============

	/** 实体中心爆发（y 取身体半高处） */
	public static void burst(ServerLevel level, Entity entity, ParticleOptions particle, int count, double spread) {
		burstAt(level, entity.getX(), entity.getY(0.5D), entity.getZ(), particle, count, spread);
	}

	/** 定点爆发 */
	public static void burstAt(ServerLevel level, double x, double y, double z,
			ParticleOptions particle, int count, double spread) {
		level.sendParticles(particle, x, y, z, count, spread, spread, spread, 0.02D);
	}

	/** 以实体脚下为中心的水平圆环（沿圆周均布） */
	public static void ring(ServerLevel level, Entity center, double radius, ParticleOptions particle, int count) {
		double ringY = center.getY(0.2D);
		for (int i = 0; i < count; i++) {
			double angle = (Math.PI * 2.0D * i) / count;
			level.sendParticles(particle,
					center.getX() + Math.cos(angle) * radius, ringY,
					center.getZ() + Math.sin(angle) * radius,
					1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
	}

	/** 两点之间的线段拖尾（剑气延长线 / 飞回轨迹） */
	public static void trail(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle, int points) {
		for (int i = 0; i < points; i++) {
			double t = points <= 1 ? 0.5D : (double) i / (points - 1);
			Vec3 p = from.lerp(to, t);
			level.sendParticles(particle, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
	}

	// ============ 音效模板（SoundEvent / Holder 双形态，26.2 两版 playSound 重载） ============

	/** 在实体位置播放音效（SoundEvent 形态） */
	public static void play(ServerLevel level, Entity entity, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
				sound, SoundSource.PLAYERS, volume, pitch);
	}

	/** 在实体位置播放音效（Holder 形态：NOTE_BLOCK_* / SHIELD_BLOCK / SOUL_ESCAPE 等） */
	public static void play(ServerLevel level, Entity entity, Holder<SoundEvent> sound, float volume, float pitch) {
		level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
				sound, SoundSource.PLAYERS, volume, pitch);
	}

	/** 等级变调惯例：I/II/III 级 → 1.0 / 1.1 / 1.2 */
	public static float pitchForLevel(int level) {
		return 1.0F + (level - 1) * 0.1F;
	}

	// ============ 表现层节流（同玩家同效果 N tick 内只放行一次，防群战刷屏） ============

	private static final Map<String, Long> LAST_FX_GAME_TIME = new HashMap<>();

	/**
	 * 节流判定：距上次同键表现不足 minIntervalTicks 返回 false。
	 * 传入 overworld 的 gameTime（全维度统一时钟）。
	 */
	public static boolean throttle(UUID player, String key, int minIntervalTicks, long gameTime) {
		String mapKey = player + ":" + key;
		Long last = LAST_FX_GAME_TIME.get(mapKey);
		if (last != null && gameTime - last < minIntervalTicks) {
			return false;
		}
		LAST_FX_GAME_TIME.put(mapKey, gameTime);
		return true;
	}

	/** 便捷重载：按实体取 overworld gameTime */
	public static boolean throttle(Entity entity, String key, int minIntervalTicks) {
		if (!(entity.level() instanceof ServerLevel serverLevel)) {
			return true;
		}
		return throttle(entity.getUUID(), key, minIntervalTicks,
				serverLevel.getServer().overworld().getGameTime());
	}
}
