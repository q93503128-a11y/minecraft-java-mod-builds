package dev.moonseungjun.openworldrpg.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.moonseungjun.openworldrpg.entity.MeadowViperEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Client-only exact GLB skinning renderer for Meadow Viper. */
public final class MeadowViperRenderer
        extends EntityRenderer<MeadowViperEntity, MeadowViperRenderState> {
    private static final Identifier WHITE_TEXTURE = Identifier.fromNamespaceAndPath(
            "openworld_rpg",
            "textures/entity/meadow_viper/white.png"
    );
    private static final RenderType LAYER = RenderTypes.entityCutout(WHITE_TEXTURE);

    /** Source-bounds review seed; final production acceptance requires Minecraft client review. */
    public static final float REVIEW_MODEL_SCALE = 0.35F;

    private final MeadowViperGltfModel model;

    public MeadowViperRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.35F;
        this.model = MeadowViperGltfModel.load();
    }

    @Override
    public MeadowViperRenderState createRenderState() {
        return new MeadowViperRenderState();
    }

    @Override
    public void extractRenderState(
            MeadowViperEntity entity,
            MeadowViperRenderState state,
            float partialTick
    ) {
        super.extractRenderState(entity, state, partialTick);
        state.phase = entity.phase();
        state.phaseAgeSeconds = Math.max(
                0.0F,
                (float) ((entity.level().getGameTime() - entity.phaseStartTick() + partialTick) / 20.0)
        );
        state.ageSeconds = (entity.tickCount + partialTick) / 20.0F;
        state.bodyYawDegrees = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        double horizontalSpeedSquared = entity.getDeltaMovement().x * entity.getDeltaMovement().x
                + entity.getDeltaMovement().z * entity.getDeltaMovement().z;
        state.horizontalSpeed = (float) Math.sqrt(horizontalSpeedSquared);
        state.onGround = entity.onGround();
    }

    @Override
    public void submit(
            MeadowViperRenderState state,
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
