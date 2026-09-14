package kr.moonseungjun.riftfrontier.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import kr.moonseungjun.riftfrontier.client.BossPresentationClientState;
import kr.moonseungjun.riftfrontier.combat.presentation.Region01BossArenaPressureFieldMotionCandidate;
import kr.moonseungjun.riftfrontier.combat.presentation.Region01BossReviewedFieldAnimationPreview;
import kr.moonseungjun.riftfrontier.combat.presentation.Region01BossReviewedReactionPreview;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.Affine3x4;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClip;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.JointPoseSampler;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.LinearBlendSkinner;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.Region01BossRuntimeAsset;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.SkinnedMeshFrame;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Non-production visual fallback for the explicit Region 01 field-review actor.
 *
 * <p>The production renderer remains gated on exact semantic-animation coverage and a reviewed final material. Until
 * those gates are satisfied, this preview renders the accepted Dragon Evolved geometry with an explicit
 * Riftfrontier material-review candidate. The candidate exists only to let a human judge UV fit, silhouette and
 * material readability in Minecraft; it is not a final-material approval and does not authorize production
 * publication. Reviewed source attacks consume their synced server semantic state. Arena pressure deliberately does
 * not pretend that an existing source clip is a reviewed attack: while its motion remains unresolved, an authored
 * whole-body scale candidate is layered over neutral Flying_Idle only for human field review. Directly reviewed
 * HitReact/Death source motion remains presentation-only. Terminal Death has priority, an executing reviewed source
 * attack has priority over HitReact, then HitReact may play while no reviewed source attack sample is active.</p>
 *
 * <p>Once the production presentation publishes, {@link Region01BossClientRenderRuntime#submit} wins and this path is
 * not reached. A resource reload that invalidates the prepared geometry also makes this preview fail closed.</p>
 */
public final class Region01BossFieldReviewRenderPreview {
    private static final String REVIEWED_IDLE_CLIP = "Flying_Idle";
    private static final Identifier PREVIEW_TEXTURE = Identifier.fromNamespaceAndPath(
        "riftfrontier", "textures/entity/region_01/boss_dark_rock_candidate.png"
    );
    private static final RenderType PREVIEW_RENDER_TYPE = RenderTypes.entityCutout(PREVIEW_TEXTURE);

    private Region01BossFieldReviewRenderPreview() {}

    public static boolean submit(
        Region01BossGeometryPreparation.PreparedGeometry preparedGeometry,
        int entityId,
        UUID entityUuid,
        float previewTimeSeconds,
        int hurtTime,
        int hurtDuration,
        int deathTime,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int packedLight
    ) {
        Objects.requireNonNull(entityUuid, "entityUuid");
        if (entityId < 0) throw new IllegalArgumentException("entityId must be >= 0");
        if (!Float.isFinite(previewTimeSeconds) || previewTimeSeconds < 0.0F) {
            throw new IllegalArgumentException("previewTimeSeconds must be finite and >= 0");
        }

        final Region01BossRuntimeAsset runtimeAsset;
        try {
            runtimeAsset = preparedGeometry.runtimeAsset();
        } catch (Region01BossGeometryPreparation.StaleReloadException stale) {
            return false;
        }

        AnimationClip clip;
        float sampleTime;

        var semanticState = BossPresentationClientState.current(entityId, entityUuid);
        var reactionSample = Region01BossReviewedReactionPreview.sample(
            hurtTime, hurtDuration, deathTime, runtimeAsset.animations()
        );
        var reviewedAttackSample = semanticState
            .flatMap(state -> Region01BossReviewedFieldAnimationPreview.sample(state, runtimeAsset.animations()));
        var arenaPressureCandidate = semanticState
            .flatMap(Region01BossArenaPressureFieldMotionCandidate::sample);

        boolean terminalDeath = reactionSample.isPresent()
            && reactionSample.orElseThrow().kind() == Region01BossReviewedReactionPreview.ReactionKind.DEATH;

        if (terminalDeath) {
            var sample = reactionSample.orElseThrow();
            clip = sample.clip();
            sampleTime = sample.sampleTimeSeconds();
        } else if (reviewedAttackSample.isPresent()) {
            var sample = reviewedAttackSample.orElseThrow();
            clip = sample.clip();
            sampleTime = sample.sampleTimeSeconds();
        } else if (reactionSample.isPresent()) {
            var sample = reactionSample.orElseThrow();
            clip = sample.clip();
            sampleTime = sample.sampleTimeSeconds();
        } else {
            clip = runtimeAsset.animations().requireClip(REVIEWED_IDLE_CLIP);
            float duration = clip.durationSeconds();
            sampleTime = duration <= 0.0F ? 0.0F : previewTimeSeconds % duration;
        }

        Affine3x4[] skinMatrices = JointPoseSampler.sampleSkinMatrices(
            runtimeAsset.skinnedMesh().rig(), clip, sampleTime
        );
        SkinnedMeshFrame frame = LinearBlendSkinner.skin(
            runtimeAsset.skinnedMesh().mesh(), Arrays.asList(skinMatrices)
        );

        boolean applyArenaPressureCandidate = !terminalDeath
            && reviewedAttackSample.isEmpty()
            && reactionSample.isEmpty()
            && arenaPressureCandidate.isPresent();
        if (applyArenaPressureCandidate) {
            var scale = arenaPressureCandidate.orElseThrow();
            poseStack.pushPose();
            poseStack.scale(scale.horizontal(), scale.vertical(), scale.horizontal());
        }
        try {
            SkinnedMeshCustomGeometryAdapter.submit(
                frame,
                poseStack,
                collector,
                PREVIEW_RENDER_TYPE,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF
            );
        } finally {
            if (applyArenaPressureCandidate) {
                poseStack.popPose();
            }
        }
        return true;
    }
}
