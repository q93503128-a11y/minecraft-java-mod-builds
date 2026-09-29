package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.client.RadiantLanceMeshData;
import org.junit.jupiter.api.Test;

class RadiantLanceMeshDataTest {
    @Test
    void admittedKayKitMeshTopologyRemainsExact() {
        assertEquals(73, RadiantLanceMeshData.VERTEX_COUNT);
        assertEquals(52, RadiantLanceMeshData.TRIANGLE_COUNT);
        assertEquals(
                RadiantLanceMeshData.VERTEX_COUNT * 3,
                RadiantLanceMeshData.POSITIONS.length
        );
        assertEquals(
                RadiantLanceMeshData.VERTEX_COUNT * 2,
                RadiantLanceMeshData.TEXCOORDS.length
        );
        assertEquals(
                RadiantLanceMeshData.VERTEX_COUNT * 3,
                RadiantLanceMeshData.NORMALS.length
        );
        assertEquals(
                RadiantLanceMeshData.TRIANGLE_COUNT * 3,
                RadiantLanceMeshData.INDICES.length
        );
        for (int index : RadiantLanceMeshData.INDICES) {
            assertTrue(index >= 0);
            assertTrue(index < RadiantLanceMeshData.VERTEX_COUNT);
        }
    }
}
