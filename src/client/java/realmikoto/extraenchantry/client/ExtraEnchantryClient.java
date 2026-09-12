package realmikoto.extraenchantry.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.chat.Component;

public class ExtraEnchantryClient implements ClientModInitializer {

	/** 幽渊实体渲染器注册 */
	private static void extraenchantry$registerEntityRenderers() {
		// Boss：渊心守望者复用原版监守者渲染器（HeartWardenEntity extends Warden）
		@SuppressWarnings({"unchecked", "rawtypes"})
		var bossType = (net.minecraft.world.entity.EntityType) realmikoto.extraenchantry.AbyssEntities.HEART_WARDEN;
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(bossType,
				(net.minecraft.client.renderer.entity.EntityRendererProvider)
						net.minecraft.client.renderer.entity.WardenRenderer::new);

		// 其余生物：人形模型 + 独立贴图 + 按体型缩放；四足生物用专属模型（§5.1 形态对齐）
		extraenchantry$mob(realmikoto.extraenchantry.AbyssEntities.ECHO_WRAITH, "echo_wraith", 0.5F, 1.0F);
		extraenchantry$mob(realmikoto.extraenchantry.AbyssEntities.TIDEBORN, "tideborn", 0.5F, 1.0F);
		extraenchantry$quadruped(realmikoto.extraenchantry.AbyssEntities.RESONANCE_BEAST, "resonance_beast", 0.5F, 0.8F, true);
		extraenchantry$quadruped(realmikoto.extraenchantry.AbyssEntities.SCULK_LARVA, "sculk_larva", 0.3F, 0.5F, false);
		extraenchantry$mob(realmikoto.extraenchantry.AbyssEntities.MEMORY_SHADE, "memory_shade", 0.5F, 1.0F);
		extraenchantry$mob(realmikoto.extraenchantry.AbyssEntities.SOUNDLESS, "soundless", 0.5F, 1.0F);
	}

	private static <T extends net.minecraft.world.entity.Mob> void extraenchantry$quadruped(
			net.minecraft.world.entity.EntityType<T> type, String textureName, float shadowRadius,
			float scale, boolean adultWolf) {
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(type,
				context -> new realmikoto.extraenchantry.client.render.AbyssQuadrupedRenderer(context,
						net.minecraft.resources.Identifier.fromNamespaceAndPath(
								"extra-enchantry", "textures/entity/" + textureName + ".png"),
						shadowRadius, scale, adultWolf));
	}

	private static <T extends net.minecraft.world.entity.Mob> void extraenchantry$mob(
			net.minecraft.world.entity.EntityType<T> type, String textureName, float shadowRadius, float scale) {
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(type,
				context -> new realmikoto.extraenchantry.client.render.AbyssMobRenderer(context,
						net.minecraft.resources.Identifier.fromNamespaceAndPath(
								"extra-enchantry", "textures/entity/" + textureName + ".png"),
						shadowRadius, scale));
	}

	/** 自定义按键分类 */
	private static final net.minecraft.client.KeyMapping.Category ECHO_CATEGORY =
			net.minecraft.client.KeyMapping.Category.register(
					net.minecraft.resources.Identifier.fromNamespaceAndPath(
							"extra-enchantry", "abyss"));

	/** 回声视觉可访问性按键（§10）：默认 R，可于控制设置中修改 */
	public static final net.minecraft.client.KeyMapping ECHO_ACCESSIBILITY_KEY =
			net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(
					new net.minecraft.client.KeyMapping(
							"key.extra-enchantry.echo_accessibility",
							com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
							org.lwjgl.glfw.GLFW.GLFW_KEY_R,
							ECHO_CATEGORY));

	/** 「发声键」（§6.2）：主动广播一次大涟漪——世界显形，也广播自己（默认 G） */
	public static final net.minecraft.client.KeyMapping ECHO_SHOUT_KEY =
			net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(
					new net.minecraft.client.KeyMapping(
							"key.extra-enchantry.echo_shout",
							com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
							org.lwjgl.glfw.GLFW.GLFW_KEY_G,
							ECHO_CATEGORY));

	/** 进入世界后的称号提示倒计时（tick）；0 = 未在等待 */
	private static int extraenchantry$welcomeCountdown = 0;

	@Override
	public void onInitializeClient() {
		// 可访问性配置持久化（§10）：启动加载（修复"断线即重置"）
		ClientConfig.load();

		// ============ 幽渊实体渲染器注册（v1.8.0.3 修复渲染帧 NPE 崩溃） ============
		// 自定义 EntityType 必须注册客户端渲染器，否则 EntityRenderDispatcher 返回 null
		// → LevelExtractor.isEntityVisible 抛 NPE（进入含该实体的世界即崩溃）。
		extraenchantry$registerEntityRenderers();

		// ============ 声纹网络同步（§6.2） ============
		// 服务端声纹事件 → 回声视觉脉冲（他人动作 / 回响灯 / 发声键 / 仪式均会驱动）
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
				.registerGlobalReceiver(realmikoto.extraenchantry.AbyssNetworking.EchoPulsePayload.TYPE,
						(payload, context) -> context.client().execute(() ->
								EchoVisionState.trigger(payload.strength())));
		// 「发声键」：向服务端请求一次主动发声
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (ECHO_SHOUT_KEY.consumeClick()) {
				if (client.player != null) {
					net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
							new realmikoto.extraenchantry.AbyssNetworking.EchoShoutPayload());
				}
			}
		});

		// 大共鸣者称号提示（1.3.0 P1）：进入世界 5 秒后检查一次
		// （延迟等待进度同步包到达；进度未完成则静默，不重复打扰）
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
				extraenchantry$welcomeCountdown = 100);
		// 修复 #29：离开世界时重置配饰栏动画/展开状态——旧实现静态量跨世界残留，
		// 重进后物品画在偏移位置而 isActive 仍为 false
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
				AccessoryHudState.reset());
		// 回声视觉（§6.2）：每 tick 推进脉冲 + 可访问性切换（默认按键 R）
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			EchoVisionState.tick(client.player);
			while (ECHO_ACCESSIBILITY_KEY.consumeClick()) {
				EchoVisionState.toggleReduceFlashing();
				if (client.player != null) {
					client.player.sendSystemMessage(Component.translatable(
							EchoVisionState.reduceFlashing()
									? "message.extra-enchantry.echo_vision.reduced"
									: "message.extra-enchantry.echo_vision.normal"));
				}
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> EchoVisionState.reset());
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (extraenchantry$welcomeCountdown > 0 && --extraenchantry$welcomeCountdown == 0
					&& client.player != null && GrandResonatorState.isGrandResonator()) {
				client.player.sendSystemMessage(
						Component.translatable("message.extra-enchantry.grand_resonator.welcome"));
			}
		});
	}
}
