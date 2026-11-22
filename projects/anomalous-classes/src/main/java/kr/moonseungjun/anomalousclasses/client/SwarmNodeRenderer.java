package kr.moonseungjun.anomalousclasses.client;

import com.mojang.blaze3d.vertex.PoseStack;
import kr.moonseungjun.anomalousclasses.entity.SwarmNodeEntity;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Collision-visibility fallback using a vanilla creature model. Codex owns the
 * production organism model, rig, animation, VFX and role visual language.
 */
public final class SwarmNodeRenderer extends MobRenderer<SwarmNodeEntity, SilverfishModel<SwarmNodeEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/silverfish.png");

    public SwarmNodeRenderer(EntityRendererProvider.Context context) {
        super(context, new SilverfishModel<>(context.bakeLayer(ModelLayers.SILVERFISH)), .08F);
    }

    @Override public ResourceLocation getTextureLocation(SwarmNodeEntity entity) { return TEXTURE; }

    @Override protected void scale(SwarmNodeEntity entity, PoseStack stack, float partialTick) {
        stack.scale(.25F, .25F, .25F);
    }

    @Override
    public void render(SwarmNodeEntity entity, float yaw, float partialTick,
                       PoseStack stack, MultiBufferSource buffer, int packedLight) {
        int samples = Math.min(7, Math.max(1, entity.count()));
        for (int i = 0; i < samples; i++) {
            stack.pushPose();
            double x = (i % 3 - 1) * .23;
            double z = (i / 3 - 1) * .22;
            stack.translate(x, (i % 2) * .045, z);
            super.render(entity, yaw, partialTick, stack, buffer, packedLight);
            stack.popPose();
        }
    }
}
