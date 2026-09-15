package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

import java.util.Set;

/** Exact-source field-review gate for the Quaternius CC0 Alien Hunter candidate. */
public record Region01HunterAlienRuntimeAsset(SkinnedMeshAsset skinnedMesh, AnimationClipInventory animations) {
    public static final String SOURCE_SHA256 = "e6fec42f9d4db3c3177da9027c5cdb2c4abd71cb934ea4f4788aea155f267124";
    public static final int VERTEX_COUNT = 4227;
    public static final int TRIANGLE_COUNT = 7676;
    public static final int JOINT_COUNT = 43;
    public static final Set<String> EXPECTED_CLIPS = Set.of(
        "Death", "Duck", "HitReact", "Idle", "Jump", "Jump_Idle", "Jump_Land",
        "No", "Punch", "Run", "Walk", "Wave", "Weapon", "Yes"
    );

    public static Region01HunterAlienRuntimeAsset importExactSource(byte[] sourceBytes) {
        String sha = GltfSkinnedMeshImporter.sha256(sourceBytes);
        if (!SOURCE_SHA256.equals(sha)) throw new IllegalArgumentException("Region 01 Hunter Alien source SHA-256 mismatch: " + sha);
        SkinnedMeshAsset mesh = GltfSkinnedMeshImporter.importArtNeutral(sourceBytes);
        if (mesh.mesh().vertexCount() != VERTEX_COUNT || mesh.mesh().triangleCount() != TRIANGLE_COUNT || mesh.rig().jointCount() != JOINT_COUNT) {
            throw new IllegalArgumentException("Region 01 Hunter Alien source structure does not match inspection receipt");
        }
        AnimationClipInventory clips = GltfAnimationImporter.importClips(sourceBytes, mesh.rig());
        clips.requireExactNames(EXPECTED_CLIPS);
        return new Region01HunterAlienRuntimeAsset(mesh, clips);
    }
}
