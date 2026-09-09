package kr.moonseungjun.riftfrontier.client.render;

import kr.moonseungjun.riftfrontier.combat.presentation.mesh.SkinnedMeshFrame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkinnedMeshCustomGeometryAdapterTest {
    @Test
    void preparesDefensiveImmutableSnapshotWithoutChangingTopology() {
        SkinnedMeshFrame frame = new SkinnedMeshFrame(
            new float[]{0, 0, 0, 1, 0, 0, 0, 1, 0},
            new float[]{0, 0, 1, 0, 0, 1, 0, 0, 1},
            new float[]{0, 0, 1, 0, 0, 1},
            new int[]{0, 1, 2}
        );

        SkinnedMeshCustomGeometryAdapter.PreparedGeometry prepared = SkinnedMeshCustomGeometryAdapter.prepare(frame);
        float[] positions = prepared.positions();
        positions[0] = 99.0f;

        assertEquals(3, prepared.vertexCount());
        assertEquals(1, prepared.triangleCount());
        assertArrayEquals(new float[]{0, 0, 0, 1, 0, 0, 0, 1, 0}, prepared.positions());
        assertArrayEquals(new int[]{0, 1, 2}, prepared.indices());
    }

    @Test
    void frameRejectsOutOfRangeIndicesBeforeRendererSubmission() {
        assertThrows(IllegalArgumentException.class, () -> new SkinnedMeshFrame(
            new float[]{0, 0, 0, 1, 0, 0, 0, 1, 0},
            new float[]{0, 0, 1, 0, 0, 1, 0, 0, 1},
            new float[]{0, 0, 1, 0, 0, 1},
            new int[]{0, 1, 3}
        ));
    }

    @Test
    void frameRejectsNonFiniteRendererStreams() {
        assertThrows(IllegalArgumentException.class, () -> new SkinnedMeshFrame(
            new float[]{Float.NaN, 0, 0, 1, 0, 0, 0, 1, 0},
            new float[]{0, 0, 1, 0, 0, 1, 0, 0, 1},
            new float[]{0, 0, 1, 0, 0, 1},
            new int[]{0, 1, 2}
        ));
    }
}
