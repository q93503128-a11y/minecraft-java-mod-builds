package kr.moonseungjun.villageguardians;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

public final class VillageSkillEffectRenderer
        extends EntityRenderer<VillageSkillEffectEntity, VillageSkillEffectRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            VillageGuardians.MOD_ID, "textures/effect/skill_mesh.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentEmissive(TEXTURE);

    public VillageSkillEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
        shadowStrength = 0.0f;
    }

    @Override
    public VillageSkillEffectRenderState createRenderState() {
        return new VillageSkillEffectRenderState();
    }

    @Override
    public void extractRenderState(
            VillageSkillEffectEntity entity,
            VillageSkillEffectRenderState state,
            float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.kind = entity.kind();
        state.ownerEntityId = entity.ownerEntityId();
        state.duration = entity.duration();
        state.age = entity.tickCount + partialTick;
        state.direction = entity.direction();
        state.seed = entity.seed();
        state.extra = entity.extra();
    }

    @Override
    public void submit(
            VillageSkillEffectRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        if (state.kind == null || state.kind.isBlank()) return;
        collector.submitCustomGeometry(
                poseStack,
                RENDER_TYPE,
                (pose, consumer) -> VillageSkillMeshLibrary.render(state, pose, consumer));
    }
}
