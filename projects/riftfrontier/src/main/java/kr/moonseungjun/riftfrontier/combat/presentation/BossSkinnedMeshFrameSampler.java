package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.presentation.mesh.Affine3x4;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.JointPoseSampler;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.LinearBlendSkinner;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.SkinnedMeshAsset;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.SkinnedMeshFrame;

import java.util.Arrays;
import java.util.Objects;

/**
 * Renderer-neutral final deformation stage for one authoritative boss animation sample.
 *
 * <p>This class deliberately owns no animation clock and performs no logical clip resolution. The only accepted
 * input is a {@link BossAnimationSampleBridge.Sample} that has already been derived from server-authored semantic
 * phase progress. It samples the imported joint rig, applies the project-owned four-influence linear-blend skinning
 * path, and returns an immutable triangle frame ready for a client renderer adapter.</p>
 */
public final class BossSkinnedMeshFrameSampler {
    private final SkinnedMeshAsset asset;

    public BossSkinnedMeshFrameSampler(SkinnedMeshAsset asset) {
        this.asset = Objects.requireNonNull(asset, "asset");
    }

    /**
     * Exact immutable mesh/rig source consumed by this sampler.
     *
     * <p>This is intentionally exposed read-only so higher-level publication gates can prove that a renderer pipeline
     * was assembled from the exact accepted geometry prepared from the current resource reload, instead of from a
     * separately imported or stale mesh.</p>
     */
    public SkinnedMeshAsset asset() {
        return asset;
    }

    public FrameSample sample(BossAnimationSampleBridge.Sample authoritativeSample) {
        Objects.requireNonNull(authoritativeSample, "authoritativeSample");

        Affine3x4[] skinMatrices = JointPoseSampler.sampleSkinMatrices(
            asset.rig(),
            authoritativeSample.clip(),
            authoritativeSample.sampleTimeSeconds()
        );
        if (skinMatrices.length != asset.rig().jointCount()) {
            throw new IllegalStateException("pose sampler returned a skin palette with the wrong joint count");
        }

        SkinnedMeshFrame frame = LinearBlendSkinner.skin(asset.mesh(), Arrays.asList(skinMatrices));
        if (frame.vertexCount() != asset.mesh().vertexCount()
            || frame.triangleCount() != asset.mesh().triangleCount()) {
            throw new IllegalStateException("skinning changed accepted mesh topology");
        }
        return new FrameSample(authoritativeSample, frame);
    }

    /** Keeps the exact authoritative sample metadata attached to the immutable deformed frame. */
    public record FrameSample(BossAnimationSampleBridge.Sample authoritativeSample, SkinnedMeshFrame frame) {
        public FrameSample {
            Objects.requireNonNull(authoritativeSample, "authoritativeSample");
            Objects.requireNonNull(frame, "frame");
        }
    }
}
