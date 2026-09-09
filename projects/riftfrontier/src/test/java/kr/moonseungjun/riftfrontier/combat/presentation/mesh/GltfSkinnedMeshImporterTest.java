package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GltfSkinnedMeshImporterTest {
    @Test
    void importsTriangleSkinAndRestHierarchyWithoutCollapsingWeights() {
        SkinnedMeshAsset asset = GltfSkinnedMeshImporter.importArtNeutral(fixture(false));

        assertEquals(3, asset.mesh().vertexCount());
        assertEquals(1, asset.mesh().triangleCount());
        assertEquals(1, asset.rig().jointCount());
        assertEquals(0, asset.rig().nodeIndex(0));
        assertEquals(-1, asset.rig().parentJoint(0));
        assertEquals("root_joint", asset.rig().name(0));
        assertEquals(1.0f, asset.rig().restLocalTransform(0).m03());
        assertEquals(2.0f, asset.rig().restLocalTransform(0).m13());
        assertEquals(3.0f, asset.rig().restLocalTransform(0).m23());
        assertEquals(Affine3x4.identity(), asset.rig().inverseBindMatrix(0));
        assertEquals(1.0f, asset.mesh().weights()[0]);
        assertEquals(0, asset.mesh().joints()[0]);
    }

    @Test
    void rejectsMaterialPayloadBeforeItCanBecomeRuntimeArt() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> GltfSkinnedMeshImporter.importArtNeutral(fixture(true))
        );
        assertTrue(error.getMessage().contains("materials"));
    }

    @Test
    void acceptedRegionContractRejectsAnyUnpinnedDerivation() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> Region01BossDerivationContract.importAccepted(fixture(false))
        );
        assertTrue(error.getMessage().contains("SHA-256 mismatch"));
    }

    private static byte[] fixture(boolean withMaterial) {
        ByteBuffer bytes = ByteBuffer.allocate(228).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : new float[]{0, 0, 0, 1, 0, 0, 0, 1, 0}) bytes.putFloat(value);
        for (int i = 0; i < 3; i++) {
            bytes.putFloat(0).putFloat(0).putFloat(1);
        }
        for (float value : new float[]{0, 0, 1, 0, 0, 1}) bytes.putFloat(value);
        for (int i = 0; i < 12; i++) bytes.put((byte) 0);
        for (int i = 0; i < 3; i++) bytes.putFloat(1).putFloat(0).putFloat(0).putFloat(0);
        bytes.putShort((short) 0).putShort((short) 1).putShort((short) 2);
        bytes.putShort((short) 0); // four-byte alignment for MAT4
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                bytes.putFloat(row == column ? 1.0f : 0.0f);
            }
        }
        String payload = Base64.getEncoder().encodeToString(bytes.array());
        String materials = withMaterial ? ",\"materials\":[{\"name\":\"forbidden\"}]" : "";
        String json = """
                {
                  "asset":{"version":"2.0"},
                  "buffers":[{"byteLength":228,"uri":"data:application/octet-stream;base64,%s"}],
                  "bufferViews":[
                    {"buffer":0,"byteOffset":0,"byteLength":36},
                    {"buffer":0,"byteOffset":36,"byteLength":36},
                    {"buffer":0,"byteOffset":72,"byteLength":24},
                    {"buffer":0,"byteOffset":96,"byteLength":12},
                    {"buffer":0,"byteOffset":108,"byteLength":48},
                    {"buffer":0,"byteOffset":156,"byteLength":6},
                    {"buffer":0,"byteOffset":164,"byteLength":64}
                  ],
                  "accessors":[
                    {"bufferView":0,"componentType":5126,"count":3,"type":"VEC3"},
                    {"bufferView":1,"componentType":5126,"count":3,"type":"VEC3"},
                    {"bufferView":2,"componentType":5126,"count":3,"type":"VEC2"},
                    {"bufferView":3,"componentType":5121,"count":3,"type":"VEC4"},
                    {"bufferView":4,"componentType":5126,"count":3,"type":"VEC4"},
                    {"bufferView":5,"componentType":5123,"count":3,"type":"SCALAR"},
                    {"bufferView":6,"componentType":5126,"count":1,"type":"MAT4"}
                  ],
                  "meshes":[{"primitives":[{"attributes":{"POSITION":0,"NORMAL":1,"TEXCOORD_0":2,"JOINTS_0":3,"WEIGHTS_0":4},"indices":5,"mode":4}]}],
                  "skins":[{"joints":[0],"inverseBindMatrices":6}],
                  "nodes":[
                    {"name":"root_joint","translation":[1,2,3]},
                    {"name":"mesh_node","mesh":0,"skin":0}
                  ]
                  %s
                }
                """.formatted(payload, materials);
        return json.getBytes(StandardCharsets.UTF_8);
    }
}
