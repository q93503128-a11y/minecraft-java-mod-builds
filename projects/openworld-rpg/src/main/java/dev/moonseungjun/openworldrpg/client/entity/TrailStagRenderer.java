package dev.moonseungjun.openworldrpg.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.moonseungjun.openworldrpg.entity.TrailStagEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Client-only renderer for the exact repository-pinned Quaternius Stag GLB. */
public final class TrailStagRenderer
        extends EntityRenderer<TrailStagEntity, TrailStagRenderState> {
    private static final Identifier WHITE_TEXTURE = Identifier.fromNamespaceAndPath(
            "openworld_rpg",
            "textures/entity/meadow_viper/white.png"
    );
    private static final RenderType LAYER = RenderTypes.entityCutout(WHITE_TEXTURE);

    /** Source-space review seed only; integrated client review owns final scale acceptance. */
    public static final float REVIEW_MODEL_SCALE = 1.0F;

    private final TrailStagGltfModel model;

    public TrailStagRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.70F;
        this.model = TrailStagGltfModel.load();
    }

    @Override
    public TrailStagRenderState createRenderState() {
        return new TrailStagRenderState();
    }

    @Override
    public void extractRenderState(
            TrailStagEntity entity,
            TrailStagRenderState state,
            float partialTick
    ) {
        super.extractRenderState(entity, state, partialTick);
        state.ageSeconds = (entity.tickCount + partialTick) / 20.0F;
        state.bodyYawDegrees = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        var motion = entity.getDeltaMovement();
        state.horizontalSpeed = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        state.verticalVelocity = (float) motion.y;
        state.onGround = entity.onGround();
        state.authoredAirborne = entity.authoredAirborne();
    }

    @Override
    public void submit(
            TrailStagRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState
    ) {
        super.submit(state, matrices, queue, cameraState);
        matrices.pushPose();
        matrices.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyYawDegrees));
        matrices.scale(REVIEW_MODEL_SCALE, REVIEW_MODEL_SCALE, REVIEW_MODEL_SCALE);
        queue.submitCustomGeometry(
                matrices,
                LAYER,
                (pose, vertices) -> model.emit(pose, vertices, state, state.lightCoords)
        );
        matrices.popPose();
    }
}
