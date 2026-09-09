package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LinearBlendSkinnerTest {
    private static final float EPSILON = 1.0e-5f;

    @Test
    void blendsTwoJointTranslationsWithoutChangingTopologyOrUvs() {
        SkinnedTriangleMesh mesh = triangle(
                new int[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                new float[]{0.5f, 0.5f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f}
        );

        SkinnedMeshFrame frame = LinearBlendSkinner.skin(mesh, List.of(
                Affine3x4.translation(2.0f, 0.0f, 0.0f),
                Affine3x4.translation(0.0f, 4.0f, 0.0f)
        ));

        assertArrayEquals(new float[]{1.0f, 2.0f, 0.0f, 3.0f, 0.0f, 0.0f, 2.0f, 1.0f, 0.0f}, frame.positions(), EPSILON);
        assertArrayEquals(mesh.uvs(), frame.uvs(), 0.0f);
        assertArrayEquals(mesh.indices(), frame.indices());
        assertEquals(3, frame.vertexCount());
        assertEquals(1, frame.triangleCount());
    }

    @Test
    void preservesIdentityPoseAndNormalDirection() {
        SkinnedTriangleMesh mesh = triangle(
                new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                new float[]{1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f}
        );

        SkinnedMeshFrame frame = LinearBlendSkinner.skin(mesh, List.of(Affine3x4.identity()));

        assertArrayEquals(mesh.positions(), frame.positions(), EPSILON);
        assertArrayEquals(mesh.normals(), frame.normals(), EPSILON);
    }

    @Test
    void rejectsJointOutsideSuppliedPalette() {
        SkinnedTriangleMesh mesh = triangle(
                new int[]{2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                new float[]{1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f}
        );

        assertThrows(IllegalArgumentException.class,
                () -> LinearBlendSkinner.skin(mesh, List.of(Affine3x4.identity())));
    }

    @Test
    void rejectsNonNormalizedWeightsAtMeshBoundary() {
        assertThrows(IllegalArgumentException.class, () -> triangle(
                new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                new float[]{0.75f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f}
        ));
    }

    private static SkinnedTriangleMesh triangle(int[] joints, float[] weights) {
        return new SkinnedTriangleMesh(
                new float[]{0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f},
                new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f},
                new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f},
                new int[]{0, 1, 2},
                joints,
                weights
        );
    }
}
