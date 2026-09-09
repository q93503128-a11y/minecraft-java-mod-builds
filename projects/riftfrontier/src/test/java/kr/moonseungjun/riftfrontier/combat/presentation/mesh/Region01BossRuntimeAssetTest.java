package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

import kr.moonseungjun.riftfrontier.combat.presentation.Region01BossDragonEvolvedAnimationContract;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Region01BossRuntimeAssetTest {
    @Test
    void rejectsAnyNonAcceptedDerivationBeforeRuntimeImport() {
        assertThrows(IllegalArgumentException.class,
            () -> Region01BossRuntimeAsset.importAccepted("not-the-accepted-gltf".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test
    void acceptsOnlyExactVerifiedStructureAndEightClipInventory() {
        Region01BossRuntimeAsset asset = new Region01BossRuntimeAsset(acceptedShape(), expectedInventory());
        assertEquals(Region01BossDerivationContract.VERTEX_COUNT, asset.skinnedMesh().mesh().vertexCount());
        assertEquals(Region01BossDerivationContract.TRIANGLE_COUNT, asset.skinnedMesh().mesh().triangleCount());
        assertEquals(Region01BossDerivationContract.JOINT_COUNT, asset.skinnedMesh().rig().jointCount());
        assertEquals(Region01BossDragonEvolvedAnimationContract.EXPECTED_SOURCE_CLIPS, asset.animations().names());
    }

    @Test
    void rejectsSourceInventoryDriftEvenWhenMeshRigShapeMatches() {
        AnimationClipInventory drifted = AnimationClipInventory.fromImported(List.of(clip("Headbutt")));
        assertThrows(IllegalArgumentException.class, () -> new Region01BossRuntimeAsset(acceptedShape(), drifted));
    }

    private static SkinnedMeshAsset acceptedShape() {
        int vertices = Region01BossDerivationContract.VERTEX_COUNT;
        float[] positions = new float[vertices * 3];
        float[] normals = new float[vertices * 3];
        float[] uvs = new float[vertices * 2];
        int[] indices = new int[Region01BossDerivationContract.TRIANGLE_COUNT * 3];
        int[] joints = new int[vertices * 4];
        float[] weights = new float[vertices * 4];
        for (int vertex = 0; vertex < vertices; vertex++) weights[vertex * 4] = 1.0f;
        SkinnedTriangleMesh mesh = new SkinnedTriangleMesh(positions, normals, uvs, indices, joints, weights);

        int count = Region01BossDerivationContract.JOINT_COUNT;
        int[] nodeIndices = new int[count];
        int[] parentJoints = new int[count];
        String[] names = new String[count];
        Affine3x4[] inverseBind = new Affine3x4[count];
        Affine3x4[] rest = new Affine3x4[count];
        java.util.Arrays.fill(parentJoints, -1);
        for (int joint = 0; joint < count; joint++) {
            nodeIndices[joint] = joint;
            names[joint] = "joint_" + joint;
            inverseBind[joint] = Affine3x4.identity();
            rest[joint] = Affine3x4.identity();
        }
        return new SkinnedMeshAsset(mesh, new JointRig(nodeIndices, parentJoints, names, inverseBind, rest));
    }

    private static AnimationClipInventory expectedInventory() {
        List<AnimationClip> clips = new ArrayList<>();
        for (String name : Region01BossDragonEvolvedAnimationContract.EXPECTED_SOURCE_CLIPS) clips.add(clip(name));
        return AnimationClipInventory.fromImported(clips);
    }

    private static AnimationClip clip(String name) {
        return new AnimationClip(name, 0.0f, List.of(new AnimationClip.Channel(
            0, AnimationClip.Path.TRANSLATION, AnimationClip.Interpolation.STEP,
            new float[]{0.0f}, new float[]{0.0f, 0.0f, 0.0f}
        )));
    }
}
