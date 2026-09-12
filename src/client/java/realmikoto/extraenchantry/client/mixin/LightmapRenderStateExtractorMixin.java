package realmikoto.extraenchantry.client.mixin;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import realmikoto.extraenchantry.client.EchoVisionState;

/**
 * 回声视觉光照混入（v1.8.0.2，设计稿 §6.2）。
 *
 * 在光照状态提取完成后，按当前"回声脉冲"强度提升方块光/亮度因子——
 * 玩家发声时周围地形短暂显形（"声即光"），静止时恢复全黑。
 */
@Mixin(LightmapRenderStateExtractor.class)
public class LightmapRenderStateExtractorMixin {

	@Inject(method = "extract", at = @At("TAIL"))
	private void extraenchantry$echoVision(LightmapRenderState state, float partialTick, CallbackInfo ci) {
		float boost = EchoVisionState.brightnessBoost();
		// 可访问性「黑暗防护」（§10）：Boss 无相阶段等强黑暗效果不施加——避免强制全黑
		if (EchoVisionState.darknessProtection()) {
			state.darknessEffectScale = 0.0F;
		}
		if (boost <= 0.0F) {
			return;
		}
		// 提升方块光因子与整体亮度：黑暗中"点亮"数格
		state.blockFactor = Math.min(1.0F, state.blockFactor + boost);
		state.brightness = Math.min(1.0F, state.brightness + boost * 0.5F);
		// 同时削弱黑暗效果（回声视觉穿透黑暗效果）
		state.darknessEffectScale = Math.max(0.0F, state.darknessEffectScale - boost);
	}
}
