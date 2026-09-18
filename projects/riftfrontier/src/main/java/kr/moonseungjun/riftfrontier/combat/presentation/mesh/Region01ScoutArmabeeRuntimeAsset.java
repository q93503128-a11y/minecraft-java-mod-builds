package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

import java.util.List;

/**
 * Exact-source review gate for the Quaternius CC0 Armabee Scout candidate.
 *
 * <p>This class deliberately does not imply production selection or flight gameplay. Armabee has
 * no Walk/Run source clips; the winged source is being reviewed as presentation for the existing
 * server-authoritative Scout ranged-pressure role, not as permission to change that role.</p>
 */
public record Region01ScoutArmabeeRuntimeAsset(SkinnedMeshAsset skinnedMesh, AnimationClipInventory animations) {
    public static final String SOURCE_SHA256 = "10ca05955ab7f7f6e2f9bd8fd85f28ed1351c945e390b1fd0fa84428b43a5916";
    public static final int VERTEX_COUNT = 1260;
    public static final int TRIANGLE_COUNT = 2280;
    public static final List<String> EXPECTED_CLIPS = List.of(
        "Death", "Fast_Flying", "Flying_Idle", "Headbutt", "HitReact", "No", "Punch", "Yes"
    );
    public static final List<String> FIELD_REVIEW_SEQUENCE = List.of(
        "Flying_Idle", "Fast_Flying", "Punch", "Headbutt", "HitReact", "Death"
    );

    public static Region01ScoutArmabeeRuntimeAsset importExactSource(byte[] sourceBytes) {
        String sha = GltfSkinnedMeshImporter.sha256(sourceBytes);
        if (!SOURCE_SHA256.equals(sha)) {
            throw new IllegalArgumentException("Region 01 Scout Armabee source SHA-256 mismatch: " + sha);
        }
        SkinnedMeshAsset mesh = GltfSkinnedMeshImporter.importArtNeutral(sourceBytes);
        if (mesh.mesh().vertexCount() != VERTEX_COUNT || mesh.mesh().triangleCount() != TRIANGLE_COUNT) {
            throw new IllegalArgumentException("Region 01 Scout Armabee source structure does not match inspection receipt");
        }
        AnimationClipInventory clips = AnimationClipInventory.fromImported(
            GltfAnimationImporter.importClips(sourceBytes, mesh.rig())
        );
        clips.requireExactNames(EXPECTED_CLIPS);
        return new Region01ScoutArmabeeRuntimeAsset(mesh, clips);
    }

    /** Source evidence must stay explicit: grounded locomotion cannot be fabricated from this asset. */
    public static boolean hasGroundedLocomotionSource() {
        return EXPECTED_CLIPS.contains("Walk") || EXPECTED_CLIPS.contains("Run");
    }
}
