package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

import kr.moonseungjun.riftfrontier.combat.presentation.Region01BossDragonEvolvedAnimationContract;

import java.util.Objects;

/**
 * Immutable runtime bundle for the exact accepted Region 01 Dragon Evolved derivation.
 *
 * <p>This is the single production import seam for geometry/rig and source animation channels. It intentionally
 * performs no logical attack binding, material selection, texture fallback, or presentation timing. Those remain
 * separate gates. Construction fails closed unless the derivation SHA/structure and the exact eight-clip source
 * inventory all satisfy their independently verified contracts.</p>
 */
public record Region01BossRuntimeAsset(
    SkinnedMeshAsset skinnedMesh,
    AnimationClipInventory animations
) {
    public Region01BossRuntimeAsset {
        Objects.requireNonNull(skinnedMesh, "skinnedMesh");
        Objects.requireNonNull(animations, "animations");
        if (skinnedMesh.mesh().vertexCount() != Region01BossDerivationContract.VERTEX_COUNT
            || skinnedMesh.mesh().triangleCount() != Region01BossDerivationContract.TRIANGLE_COUNT
            || skinnedMesh.rig().jointCount() != Region01BossDerivationContract.JOINT_COUNT) {
            throw new IllegalArgumentException("Region 01 boss runtime asset no longer matches accepted mesh/rig structure");
        }
        animations.requireExactNames(Region01BossDragonEvolvedAnimationContract.EXPECTED_SOURCE_CLIPS);
    }

    /** Imports one exact accepted art-neutral glTF into the renderer-neutral production runtime bundle. */
    public static Region01BossRuntimeAsset importAccepted(byte[] derivationBytes) {
        SkinnedMeshAsset mesh = Region01BossDerivationContract.importAccepted(derivationBytes);
        AnimationClipInventory animations = Region01BossDragonEvolvedAnimationContract.verifyImportedClips(
            GltfAnimationImporter.importClips(derivationBytes, mesh.rig())
        );
        return new Region01BossRuntimeAsset(mesh, animations);
    }
}
