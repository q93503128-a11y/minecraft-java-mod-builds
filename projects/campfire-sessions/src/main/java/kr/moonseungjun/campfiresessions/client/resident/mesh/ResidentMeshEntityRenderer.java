package kr.moonseungjun.campfiresessions.client.resident.mesh;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * Multi-material resident renderer.
 *
 * <p>One custom geometry submission is created per source material so each glTF
 * base-color texture can remain independent. All passes share the same GeckoLib
 * bone snapshots and hierarchy.</p>
 */
public final class ResidentMeshEntityRenderer<T extends Entity & GeoAnimatable>
        extends GeoEntityRenderer<T, EntityRenderState> {
    private final ResidentMeshGeoModel<T> residentModel;

    public ResidentMeshEntityRenderer(
            EntityRendererProvider.Context context,
            ResidentMeshGeoModel<T> model,
            float shadowRadius
    ) {
        super(context, model);
        this.residentModel = model;
        this.shadowRadius = shadowRadius;
    }

    @Override
    public void submitRenderTasks(
            RenderPassInfo<EntityRenderState> renderPassInfo,
            OrderedSubmitNodeCollector renderTasks,
            @Nullable RenderType ignoredDefaultRenderType
    ) {
        int packedLight = renderPassInfo.packedLight();
        int packedOverlay = renderPassInfo.packedOverlay();
        int entityColor = renderPassInfo.renderColor();

        for (int index = 0; index < this.residentModel.materialCount(); index++) {
            final int materialIndex = index;
            ResidentMeshMaterial material = this.residentModel.material(materialIndex);
            RenderType renderType = RenderTypes.entityCutout(material.texture());
            int materialColor = multiplyArgb(entityColor, material.baseColorArgb());

            renderTasks.submitCustomGeometry(renderPassInfo.poseStack(), renderType, (pose, vertexConsumer) -> {
                PoseStack poseStack = renderPassInfo.poseStack();

                poseStack.pushPose();
                poseStack.last().set(pose);
                renderPassInfo.renderPosed(() -> this.residentModel.renderMaterial(
                        renderPassInfo,
                        materialIndex,
                        vertexConsumer,
                        packedLight,
                        packedOverlay,
                        materialColor
                ));
                poseStack.popPose();
            });
        }
    }

    private static int multiplyArgb(int left, int right) {
        int a = multiplyChannel(left >>> 24, right >>> 24);
        int r = multiplyChannel(left >>> 16, right >>> 16);
        int g = multiplyChannel(left >>> 8, right >>> 8);
        int b = multiplyChannel(left, right);

        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int multiplyChannel(int left, int right) {
        return ((left & 0xFF) * (right & 0xFF) + 127) / 255;
    }
}
