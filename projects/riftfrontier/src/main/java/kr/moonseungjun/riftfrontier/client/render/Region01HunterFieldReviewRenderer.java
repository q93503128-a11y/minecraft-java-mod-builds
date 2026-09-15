package kr.moonseungjun.riftfrontier.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.Affine3x4;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClip;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.JointPoseSampler;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.LinearBlendSkinner;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.Region01HunterAlienRuntimeAsset;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.SkinnedMeshFrame;
import kr.moonseungjun.riftfrontier.entity.Region01HunterFieldReviewEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/** Explicit Minecraft field-review renderer for the exact Quaternius Alien Hunter candidate. */
public final class Region01HunterFieldReviewRenderer extends EntityRenderer<Region01HunterFieldReviewEntity, Region01HunterFieldReviewRenderState> {
    private static final Identifier SOURCE = Identifier.fromNamespaceAndPath("riftfrontier", "hunter_presentation/region_01/alien.source.gltf");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("riftfrontier", "textures/entity/region_01/alien_source_v1.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);
    private static final String[] REVIEW_SEQUENCE = {"Idle", "Walk", "Run", "Punch", "HitReact", "Death"};
    private static final float SLOT_SECONDS = 2.0F;
    private static final float REVIEW_SCALE = 0.45F;
    private static volatile Region01HunterAlienRuntimeAsset cachedAsset;

    public Region01HunterFieldReviewRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.75F;
    }

    @Override public Region01HunterFieldReviewRenderState createRenderState() { return new Region01HunterFieldReviewRenderState(); }

    @Override
    public void extractRenderState(Region01HunterFieldReviewEntity entity, Region01HunterFieldReviewRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.setPreviewTimeSeconds((entity.tickCount + partialTick) / 20.0F);
    }

    @Override
    public void submit(Region01HunterFieldReviewRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);
        Region01HunterAlienRuntimeAsset asset = asset();
        if (asset == null) return;
        float cycle = state.previewTimeSeconds() % (SLOT_SECONDS * REVIEW_SEQUENCE.length);
        int slot = Math.min(REVIEW_SEQUENCE.length - 1, (int)(cycle / SLOT_SECONDS));
        AnimationClip clip = asset.animations().requireClip(REVIEW_SEQUENCE[slot]);
        float duration = clip.durationSeconds();
        float local = cycle - slot * SLOT_SECONDS;
        float sampleTime = duration <= 0.0F ? 0.0F : local % duration;
        Affine3x4[] skinMatrices = JointPoseSampler.sampleSkinMatrices(asset.skinnedMesh().rig(), clip, sampleTime);
        SkinnedMeshFrame frame = LinearBlendSkinner.skin(asset.skinnedMesh().mesh(), Arrays.asList(skinMatrices));
        poseStack.pushPose();
        poseStack.scale(REVIEW_SCALE, REVIEW_SCALE, REVIEW_SCALE);
        try {
            SkinnedMeshCustomGeometryAdapter.submit(frame, poseStack, collector, RENDER_TYPE, state.lightCoords, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        } finally {
            poseStack.popPose();
        }
    }

    private static Region01HunterAlienRuntimeAsset asset() {
        Region01HunterAlienRuntimeAsset current = cachedAsset;
        if (current != null) return current;
        synchronized (Region01HunterFieldReviewRenderer.class) {
            if (cachedAsset != null) return cachedAsset;
            try {
                var resource = Minecraft.getInstance().getResourceManager().getResource(SOURCE).orElse(null);
                if (resource == null) return null;
                try (InputStream input = resource.open()) {
                    cachedAsset = Region01HunterAlienRuntimeAsset.importExactSource(input.readAllBytes());
                    return cachedAsset;
                }
            } catch (IOException | IllegalArgumentException failure) {
                return null;
            }
        }
    }
}
