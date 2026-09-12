package realmikoto.extraenchantry.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/**
 * 幽渊生物通用渲染器（v1.8.0.3 修复「实体无渲染器导致渲染帧 NPE 崩溃」）。
 *
 * 背景：自定义 EntityType 必须在客户端注册渲染器，否则
 * {@code EntityRenderDispatcher.getRenderer()} 返回 null →
 * {@code LevelExtractor.isEntityVisible} 抛 NullPointerException（渲染帧崩溃）。
 *
 * 实现：以人形模型（ZOMBIE 层）+ 每生物独立贴图 + 可配缩放，
 * 覆盖幽渊全部非 Boss 生物（回响幽灵 / 渊息者 / 声纹兽 / 幽匿幼体 / 记忆残影 / 无声者）。
 * Boss（渊心守望者）复用原版 {@code WardenRenderer}。
 */
public class AbyssMobRenderer
		extends HumanoidMobRenderer<Mob, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

	private final Identifier texture;
	private final float modelScale;

	public AbyssMobRenderer(EntityRendererProvider.Context context, Identifier texture, float shadowRadius,
			float modelScale) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), shadowRadius);
		this.texture = texture;
		this.modelScale = modelScale;
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return texture;
	}

	@Override
	protected void scale(HumanoidRenderState state, PoseStack poseStack) {
		poseStack.scale(modelScale, modelScale, modelScale);
	}
}
