package kr.moonseungjun.riftfrontier.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import kr.moonseungjun.riftfrontier.entity.Region01BossEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;

/** Minecraft 26.2 entity-renderer bridge for the Region 01 boss custom-geometry pipeline. */
public final class Region01BossRenderer extends EntityRenderer<Region01BossEntity, Region01BossRenderState> {
    public Region01BossRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Region01BossRenderState createRenderState() {
        return new Region01BossRenderState();
    }

    @Override
    public void extractRenderState(Region01BossEntity entity, Region01BossRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.setEntityId(entity.getId());
    }

    @Override
    public void submit(
        Region01BossRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        CameraRenderState cameraState
    ) {
        super.submit(state, poseStack, collector, cameraState);
        if (state.entityId() < 0) {
            throw new IllegalStateException("Region 01 boss render state was submitted before entity extraction");
        }
        Region01BossClientRenderRuntime.submit(state.entityId(), poseStack, collector, state.lightCoords);
    }
}
