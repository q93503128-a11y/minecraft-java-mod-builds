package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

/**
 * Immutable renderer-neutral import result for one skinned triangle mesh and its glTF joint palette.
 */
public record SkinnedMeshAsset(SkinnedTriangleMesh mesh, JointRig rig) {
    public SkinnedMeshAsset {
        if (mesh == null || rig == null) {
            throw new IllegalArgumentException("mesh and rig must not be null");
        }
        int[] joints = mesh.joints();
        for (int joint : joints) {
            if (joint >= rig.jointCount()) {
                throw new IllegalArgumentException("mesh references joint outside rig palette: " + joint);
            }
        }
    }
}
