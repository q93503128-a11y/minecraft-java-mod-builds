package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

/**
 * Immutable acceptance gate for the exact art-neutral Dragon Evolved derivation verified in M3.
 */
public final class Region01BossDerivationContract {
    public static final String SOURCE_SHA256 = "39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c";
    public static final String DERIVATION_SHA256 = "ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac";
    public static final int VERTEX_COUNT = 4437;
    public static final int TRIANGLE_COUNT = 7440;
    public static final int JOINT_COUNT = 46;

    private Region01BossDerivationContract() {
    }

    public static SkinnedMeshAsset importAccepted(byte[] derivationBytes) {
        String sha256 = GltfSkinnedMeshImporter.sha256(derivationBytes);
        if (!DERIVATION_SHA256.equals(sha256)) {
            throw new IllegalArgumentException("Region 01 boss derivation SHA-256 mismatch: " + sha256);
        }
        SkinnedMeshAsset asset = GltfSkinnedMeshImporter.importArtNeutral(derivationBytes);
        if (asset.mesh().vertexCount() != VERTEX_COUNT
                || asset.mesh().triangleCount() != TRIANGLE_COUNT
                || asset.rig().jointCount() != JOINT_COUNT) {
            throw new IllegalArgumentException("Region 01 boss derivation structure does not match accepted receipt");
        }
        return asset;
    }
}
