package realmikoto.extraenchantry.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import realmikoto.extraenchantry.client.mixin.ClientAdvancementsAccessor;

public class ExtraEnchantryClient implements ClientModInitializer {

	/** 进入世界后的称号提示倒计时（tick）；0 = 未在等待 */
	private static int extraenchantry$welcomeCountdown = 0;

	@Override
	public void onInitializeClient() {
		// 大共鸣者称号提示（1.3.0 P1）：进入世界 5 秒后检查一次
		// （延迟等待进度同步包到达；进度未完成则静默，不重复打扰）
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
				extraenchantry$welcomeCountdown = 100);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (extraenchantry$welcomeCountdown > 0 && --extraenchantry$welcomeCountdown == 0
					&& client.player != null && GrandResonatorState.isGrandResonator()) {
				client.player.sendSystemMessage(
						Component.translatable("message.extra-enchantry.grand_resonator.welcome"));
			}
		});
	}
}
