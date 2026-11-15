package kr.moonseungjun.anomalousclasses.client;

import com.mojang.blaze3d.vertex.PoseStack;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import kr.moonseungjun.anomalousclasses.entity.PuppetEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class PuppetRenderer extends MobRenderer<PuppetEntity, PuppetModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID,
                    "textures/entity/marionette.png");

    public PuppetRenderer(EntityRendererProvider.Context context) {
        super(context, new PuppetModel(context.bakeLayer(PuppetModel.LAYER)), 0.35F);
    }

    @Override public ResourceLocation getTextureLocation(PuppetEntity entity) { return TEXTURE; }

    @Override protected void scale(PuppetEntity entity, PoseStack stack, float partialTick) {
        float scale = entity.puppetScale();
        stack.scale(scale, scale, scale);
    }
}
