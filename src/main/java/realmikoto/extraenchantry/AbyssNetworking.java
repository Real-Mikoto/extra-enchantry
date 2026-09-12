package realmikoto.extraenchantry;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * 声纹网络同步（v1.8.0 §6.2）：
 *
 * - S2C「echo_pulse」：服务端声纹事件 → 附近玩家的客户端——
 *   回声视觉随真实声纹起伏（此前仅本地移动近似，未消费他人/工具声纹）；
 * - C2S「echo_shout」：「发声键」（§6.2「按下『发声』键，世界以声波涟漪短暂显形」）——
 *   主动广播一次大涟漪（照亮世界 + 显形周围生物 + 引来危险）。
 */
public final class AbyssNetworking {

	// ============ S2C：声纹脉冲 ============

	public record EchoPulsePayload(float strength) implements CustomPacketPayload {
		public static final Type<EchoPulsePayload> TYPE =
				new Type<>(ExtraEnchantry.id("echo_pulse"));
		public static final StreamCodec<RegistryFriendlyByteBuf, EchoPulsePayload> CODEC =
				StreamCodec.composite(
						net.minecraft.network.codec.ByteBufCodecs.FLOAT,
						EchoPulsePayload::strength, EchoPulsePayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	// ============ C2S：主动发声 ============

	public record EchoShoutPayload() implements CustomPacketPayload {
		public static final Type<EchoShoutPayload> TYPE =
				new Type<>(ExtraEnchantry.id("echo_shout"));
		public static final StreamCodec<RegistryFriendlyByteBuf, EchoShoutPayload> CODEC =
				StreamCodec.unit(new EchoShoutPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private AbyssNetworking() {
	}

	/** 主初始化注册（ExtraEnchantry.onInitialize 调用） */
	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(EchoPulsePayload.TYPE, EchoPulsePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(EchoShoutPayload.TYPE, EchoShoutPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(EchoShoutPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayer player = context.player();
				if (!AbyssKey.isIn(player)) {
					return;
				}
				// 主动发声（§6.2）：大涟漪照亮世界——同时显形周围生物（无声者不回应）
				EchoManager.emit(player, 12.0F);
				FxHelper.ring(player.level(), player, 12.0D,
						net.minecraft.core.particles.ParticleTypes.SONIC_BOOM, 20);
				FxHelper.play(player.level(), player,
						net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, 1.1F, 0.5F);
				for (net.minecraft.world.entity.LivingEntity nearby : player.level()
						.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
								player.getBoundingBox().inflate(12.0D),
								e -> e != player
										&& !realmikoto.extraenchantry.entity.abyss.SoundlessEntity.isSoundless(e))) {
					nearby.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, true, false));
				}
			});
		});
	}

	/** 声纹事件广播：把有效声纹强度（归一化 0–1）发给声源附近的玩家（含声源本人） */
	public static void broadcastEchoPulse(ServerPlayer source, float effective) {
		if (effective <= 0) {
			return;
		}
		float normalized = Math.min(1.0F, effective / 14.0F);
		EchoPulsePayload payload = new EchoPulsePayload(normalized);
		double range = Math.max(24.0D, effective * 3.0D);
		for (ServerPlayer sp : source.level().players()) {
			if (sp.distanceToSqr(source) < range * range) {
				ServerPlayNetworking.send(sp, payload);
			}
		}
	}
}
