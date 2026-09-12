package realmikoto.extraenchantry.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/**
 * 幽渊四足生物渲染器（v1.8.0.4）：声纹兽（狼模型）/ 幽匿幼体（猪模型缩小）。
 *
 * 设计稿 §5.1 形态对齐：声纹兽是"兽"（四足），幼体是小型生物——
 * 不再与人形生物共用僵尸模型。成体/幼体模型分别烘焙，覆盖所有生长阶段。
 */
public class AbyssQuadrupedRenderer
		extends AgeableMobRenderer<Mob, LivingEntityRenderState, AbyssQuadrupedRenderer.AbyssQuadrupedModel> {

	private final Identifier texture;
	private final float modelScale;

	/** QuadrupedModel 构造器为 protected——以子类暴露（模型部件名与原版四足层一致） */
	public static final class AbyssQuadrupedModel extends QuadrupedModel<LivingEntityRenderState> {
		AbyssQuadrupedModel(ModelPart root) {
			super(root);
		}
	}

	public AbyssQuadrupedRenderer(EntityRendererProvider.Context context, Identifier texture,
			float shadowRadius, float modelScale, boolean adultWolf) {
		super(context,
				new AbyssQuadrupedModel(context.bakeLayer(adultWolf ? ModelLayers.WOLF : ModelLayers.PIG)),
				new AbyssQuadrupedModel(context.bakeLayer(adultWolf ? ModelLayers.WOLF_BABY : ModelLayers.PIG_BABY)),
				shadowRadius);
		this.texture = texture;
		this.modelScale = modelScale;
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return texture;
	}

	@Override
	protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
		poseStack.scale(modelScale, modelScale, modelScale);
	}
}
