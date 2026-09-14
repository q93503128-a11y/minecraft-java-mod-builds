package kr.moonseungjun.riftfrontier.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.Affine3x4;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClip;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.JointPoseSampler;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.LinearBlendSkinner;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.Region01BossRuntimeAsset;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.SkinnedMeshFrame;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import java.util.Arrays;

/**
 * Non-production visual fallback for the explicit Region 01 field-review actor.
 *
 * <p>The production renderer remains gated on exact semantic-animation coverage and a reviewed final material. Until
 * those gates are satisfied, this preview renders the already accepted Dragon Evolved geometry with the already
 * reviewed cyclic {@code Flying_Idle} source motion and Minecraft's own stone texture. It exists only so human field
 * review can see the accepted silhouette instead of testing an invisible entity. It is not an attack binding, final
 * material, palette, VFX language or server-authoritative animation clock.</p>
 *
 * <p>Once the production presentation publishes, {@link Region01BossClientRenderRuntime#submit} wins and this path is
 * not reached. A resource reload that invalidates the prepared geometry also makes this preview fail closed.</p>
 */
public final class Region01BossFieldReviewRenderPreview {
    private static final String REVIEWED_IDLE_CLIP = "Flying_Idle";
    private static final Identifier PREVIEW_TEXTURE =
        Identifier.fromNamespaceAndPath("minecraft", "textures/block/stone.png");
    private static final RenderType PREVIEW_RENDER_TYPE = RenderType.entityCutout(PREVIEW_TEXTURE);

    private Region01BossFieldReviewRenderPreview() {}

    public static boolean submit(
        Region01BossGeometryPreparation.PreparedGeometry preparedGeometry,
        float previewTimeSeconds,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int packedLight
    ) {
        if (!Float.isFinite(previewTimeSeconds) || previewTimeSeconds < 0.0F) {
            throw new IllegalArgumentException("previewTimeSeconds must be finite and >= 0");
        }

        final Region01BossRuntimeAsset runtimeAsset;
        try {
            runtimeAsset = preparedGeometry.runtimeAsset();
        } catch (Region01BossGeometryPreparation.StaleReloadException stale) {
            return false;
        }

        AnimationClip clip = runtimeAsset.animations().requireClip(REVIEWED_IDLE_CLIP);
        float duration = clip.durationSeconds();
        float sampleTime = duration <= 0.0F ? 0.0F : previewTimeSeconds % duration;
        Affine3x4[] skinMatrices = JointPoseSampler.sampleSkinMatrices(
            runtimeAsset.skinnedMesh().rig(), clip, sampleTime
        );
        SkinnedMeshFrame frame = LinearBlendSkinner.skin(
            runtimeAsset.skinnedMesh().mesh(), Arrays.asList(skinMatrices)
        );

        SkinnedMeshCustomGeometryAdapter.submit(
            frame,
            poseStack,
            collector,
            PREVIEW_RENDER_TYPE,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            0xFFFFFFFF
        );
        return true;
    }
}
