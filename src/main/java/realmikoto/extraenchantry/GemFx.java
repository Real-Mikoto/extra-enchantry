package realmikoto.extraenchantry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * 家族铭印美术效果（1.4.0「环佩与獠牙」）：8 颗家族宝石的专属视觉表现。
 *
 * <p>预算对齐 FxHelper 三层规范：穿戴为 L2（爆发 10~14 粒 + 1 音效）、
 * 常驻环境为 L1（每 2 秒 ≤1 粒，40 tick 节流）。</p>
 *
 * <p>家族配色（粒子 → 意象）：
 * <pre>
 *   魂珀   SOUL           幽蓝灵魂火，贴身升腾
 *   雷光石 ELECTRIC_SPARK 金色电花，急促炸裂
 *   刃晶   CRIT           锋刃寒星，贴身环绕
 *   萌芽晶 HAPPY_VILLAGER 生命绿光，脚下生辉
 *   盾纹玉 END_ROD         白玉光柱，庄重拱卫
 *   风羽晶 CLOUD           流云随身，向上飘散
 *   烬心石 FLAME + LAVA    余烬火星，明灭可掬
 *   潮汐珠 WATER_WAKE      水面涟漪，环形扩散
 * </pre></p>
 */
public final class GemFx {

	/** L1 常驻节流（tick）：每 2 秒一次环境粒子 */
	private static final int AMBIENT_INTERVAL_TICKS = 40;

	private GemFx() {
	}

	/** 穿戴爆发（L2）：家族专属粒子 + 点缀音（音高按材质档位变调：铜铁 1.0 / 金 1.1 / 钻 1.2） */
	public static void onEquip(ServerLevel level, ServerPlayer player, String gemPath,
			Accessories.Material material) {
		float pitch = switch (material) {
			case GOLD -> 1.1F;
			case DIAMOND -> 1.2F;
			default -> 1.0F;
		};
		switch (gemPath) {
			case "soul_amber" -> {                                  // 灵魂：幽蓝升腾
				FxHelper.burst(level, player, ParticleTypes.SOUL, 10, 0.25D);
				FxHelper.play(level, player, SoundEvents.SOUL_ESCAPE, 0.6F, pitch);
			}
			case "storm_stone" -> {                                 // 风暴：金色电花
				FxHelper.burst(level, player, ParticleTypes.ELECTRIC_SPARK, 14, 0.3D);
				FxHelper.play(level, player, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.3F, 1.4F * pitch);
			}
			case "blade_shard" -> {                                 // 锋刃：寒星贴身
				FxHelper.ring(level, player, 0.8D, ParticleTypes.CRIT, 10);
				FxHelper.play(level, player, SoundEvents.TRIDENT_THROW, 0.4F, 1.5F * pitch);
			}
			case "sprout_crystal" -> {                              // 自然：生命绿光
				FxHelper.ring(level, player, 0.6D, ParticleTypes.HAPPY_VILLAGER, 8);
				FxHelper.play(level, player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 0.8F * pitch);
			}
			case "guard_jade" -> {                                  // 守护：白玉光柱拱卫
				FxHelper.ring(level, player, 1.0D, ParticleTypes.END_ROD, 6);
				FxHelper.play(level, player, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, pitch);
			}
			case "wind_feather" -> {                                // 风：流云飘散
				FxHelper.burst(level, player, ParticleTypes.CLOUD, 6, 0.4D);
				FxHelper.play(level, player, SoundEvents.ELYTRA_FLYING, 0.5F, 1.2F * pitch);
			}
			case "ember_heart" -> {                                 // 火焰：余烬火星
				FxHelper.burst(level, player, ParticleTypes.FLAME, 8, 0.25D);
				FxHelper.burst(level, player, ParticleTypes.LAVA, 4, 0.2D);
				FxHelper.play(level, player, SoundEvents.FIRECHARGE_USE, 0.5F, pitch);
			}
			case "tide_pearl" -> {                                  // 水渊：水花涟漪
				FxHelper.ring(level, player, 0.9D, ParticleTypes.SPLASH, 12);
				FxHelper.play(level, player, SoundEvents.PLAYER_SPLASH, 0.4F, 1.3F * pitch);
			}
			default -> {
			}
		}
	}

	/**
	 * 常驻环境粒子（L1）：佩戴已镶嵌宝石的配饰时每 2 秒一粒家族色氛围。
	 * 逐宝石独立节流；全身至多 4 颗（每槽一颗），峰值 4 粒/2 秒仍在预算内。
	 */
	public static void ambient(ServerLevel level, ServerPlayer player, String gemPath) {
		if (!FxHelper.throttle(player, "gem_ambient:" + gemPath, AMBIENT_INTERVAL_TICKS)) {
			return;
		}
		switch (gemPath) {
			case "soul_amber" -> level.sendParticles(ParticleTypes.SOUL,
					player.getX(), player.getY(0.6D), player.getZ(), 1, 0.15D, 0.2D, 0.15D, 0.01D);
			case "storm_stone" -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
					player.getX(), player.getY(0.8D), player.getZ(), 1, 0.2D, 0.1D, 0.2D, 0.01D);
			case "blade_shard" -> level.sendParticles(ParticleTypes.CRIT,
					player.getX(), player.getY(0.5D), player.getZ(), 1, 0.3D, 0.2D, 0.3D, 0.01D);
			case "sprout_crystal" -> level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
					player.getX(), player.getY(0.1D), player.getZ(), 1, 0.3D, 0.1D, 0.3D, 0.01D);
			case "guard_jade" -> level.sendParticles(ParticleTypes.END_ROD,
					player.getX(), player.getY(0.5D), player.getZ(), 1, 0.25D, 0.3D, 0.25D, 0.0D);
			case "wind_feather" -> level.sendParticles(ParticleTypes.CLOUD,
					player.getX(), player.getY(0.9D), player.getZ(), 1, 0.2D, 0.15D, 0.2D, 0.0D);
			case "ember_heart" -> level.sendParticles(ParticleTypes.FLAME,
					player.getX(), player.getY(0.4D), player.getZ(), 1, 0.15D, 0.15D, 0.15D, 0.01D);
			case "tide_pearl" -> level.sendParticles(ParticleTypes.SPLASH,
					player.getX(), player.getY(0.1D), player.getZ(), 1, 0.1D, 0.0D, 0.1D, 0.0D);
			default -> {
			}
		}
	}

	/** 音效重播入口（SoundSource.PLAYERS 统一，供外层如 GUI 内穿戴复用） */
	static void playChime(ServerLevel level, ServerPlayer player, float pitch) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5F, pitch);
	}
}
