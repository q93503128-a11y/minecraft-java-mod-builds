package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GltfAnimationImporterTest {
    @Test
    void importsLinearTranslationAndSamplesSkinMatrix() {
        byte[] gltf = linearTranslationGltf("LINEAR");
        JointRig rig = singleJointRig();

        List<AnimationClip> clips = GltfAnimationImporter.importClips(gltf, rig);

        assertEquals(1, clips.size());
        assertEquals("Move", clips.getFirst().name());
        assertEquals(1.0f, clips.getFirst().durationSeconds(), 1.0e-6f);
        Affine3x4[] matrices = JointPoseSampler.sampleSkinMatrices(rig, clips.getFirst(), 0.5f);
        assertEquals(1.0f, matrices[0].m03(), 1.0e-5f);
        assertEquals(0.0f, matrices[0].m13(), 1.0e-5f);
    }

    @Test
    void rejectsInterpolationInsteadOfSilentlyApproximatingIt() {
        assertThrows(IllegalArgumentException.class,
                () -> GltfAnimationImporter.importClips(linearTranslationGltf("CATMULLROMSPLINE"), singleJointRig()));
    }

    @Test
    void quaternionLinearUsesShortestPath() {
        AnimationClip.Channel rotation = new AnimationClip.Channel(
                0, AnimationClip.Path.ROTATION, AnimationClip.Interpolation.LINEAR,
                new float[]{0.0f, 1.0f},
                new float[]{0,0,0,1, 0,0,0,-1}
        );
        AnimationClip clip = new AnimationClip("FlipSign", 1.0f, List.of(rotation));
        Affine3x4 matrix = JointPoseSampler.sampleSkinMatrices(singleJointRig(), clip, 0.5f)[0];
        assertEquals(1.0f, matrix.m00(), 1.0e-5f);
        assertEquals(1.0f, matrix.m11(), 1.0e-5f);
        assertEquals(1.0f, matrix.m22(), 1.0e-5f);
    }

    @Test
    void cubicSplineUsesGltfHermiteLayout() {
        AnimationClip.Channel translation = new AnimationClip.Channel(
                0, AnimationClip.Path.TRANSLATION, AnimationClip.Interpolation.CUBICSPLINE,
                new float[]{0.0f, 1.0f},
                new float[]{
                        0,0,0,  0,0,0,  0,0,0,
                        0,0,0,  2,0,0,  0,0,0
                }
        );
        AnimationClip clip = new AnimationClip("Cubic", 1.0f, List.of(translation));
        Affine3x4 matrix = JointPoseSampler.sampleSkinMatrices(singleJointRig(), clip, 0.5f)[0];
        assertEquals(1.0f, matrix.m03(), 1.0e-5f);
    }

    @Test
    void duplicateJointPathChannelsFailClosed() {
        AnimationClip.Channel a = new AnimationClip.Channel(0, AnimationClip.Path.TRANSLATION,
                AnimationClip.Interpolation.STEP, new float[]{0}, new float[]{0,0,0});
        AnimationClip.Channel b = new AnimationClip.Channel(0, AnimationClip.Path.TRANSLATION,
                AnimationClip.Interpolation.STEP, new float[]{0}, new float[]{1,0,0});
        AnimationClip clip = new AnimationClip("Duplicate", 0, List.of(a,b));
        assertThrows(IllegalArgumentException.class,
                () -> JointPoseSampler.sampleSkinMatrices(singleJointRig(), clip, 0));
    }

    private static JointRig singleJointRig() {
        return new JointRig(new int[]{0}, new int[]{-1}, new String[]{"root"},
                new Affine3x4[]{Affine3x4.identity()}, new Affine3x4[]{Affine3x4.identity()});
    }

    private static byte[] linearTranslationGltf(String interpolation) {
        ByteBuffer bytes = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN);
        bytes.putFloat(0.0f).putFloat(1.0f);
        bytes.putFloat(0).putFloat(0).putFloat(0);
        bytes.putFloat(2).putFloat(0).putFloat(0);
        String encoded = Base64.getEncoder().encodeToString(bytes.array());
        String json = """
                {
                  "buffers":[{"byteLength":32,"uri":"data:application/octet-stream;base64,%s"}],
                  "bufferViews":[{"buffer":0,"byteOffset":0,"byteLength":8},{"buffer":0,"byteOffset":8,"byteLength":24}],
                  "accessors":[
                    {"bufferView":0,"componentType":5126,"count":2,"type":"SCALAR"},
                    {"bufferView":1,"componentType":5126,"count":2,"type":"VEC3"}
                  ],
                  "animations":[{
                    "name":"Move",
                    "samplers":[{"input":0,"output":1,"interpolation":"%s"}],
                    "channels":[{"sampler":0,"target":{"node":0,"path":"translation"}}]
                  }]
                }
                """.formatted(encoded, interpolation);
        return json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
