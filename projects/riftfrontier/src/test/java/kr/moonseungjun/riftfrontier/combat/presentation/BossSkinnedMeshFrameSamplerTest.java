package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.Affine3x4;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClip;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.JointRig;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.SkinnedMeshAsset;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.SkinnedTriangleMesh;
import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BossSkinnedMeshFrameSamplerTest {
    private static final float EPSILON = 1.0e-5f;
    private static final ContentId KEY = ContentId.parse("riftfrontier:boss/region_01/test_motion");

    @Test
    void deformsAcceptedTopologyAtAuthoritativeSampleTime() {
        BossSkinnedMeshFrameSampler sampler = new BossSkinnedMeshFrameSampler(asset());
        BossAnimationSampleBridge.Sample source = sample(clip(0, 2.0f), 0.5f, 11L);

        BossSkinnedMeshFrameSampler.FrameSample result = sampler.sample(source);

        assertSame(source, result.authoritativeSample());
        assertArrayEquals(
            new float[]{1.0f, 0.0f, 0.0f, 2.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f},
            result.frame().positions(),
            EPSILON
        );
        assertArrayEquals(new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f}, result.frame().uvs(), 0.0f);
        assertArrayEquals(new int[]{0, 1, 2}, result.frame().indices());
        assertEquals(3, result.frame().vertexCount());
        assertEquals(1, result.frame().triangleCount());
    }

    @Test
    void differentAuthoritativeTimesProduceDifferentFramesWithoutInternalClock() {
        BossSkinnedMeshFrameSampler sampler = new BossSkinnedMeshFrameSampler(asset());

        float[] early = sampler.sample(sample(clip(0, 2.0f), 0.25f, 20L)).frame().positions();
        float[] late = sampler.sample(sample(clip(0, 2.0f), 0.75f, 21L)).frame().positions();

        assertEquals(0.5f, early[0], EPSILON);
        assertEquals(1.5f, late[0], EPSILON);
    }

    @Test
    void animationChannelOutsideImportedRigFailsClosed() {
        BossSkinnedMeshFrameSampler sampler = new BossSkinnedMeshFrameSampler(asset());
        BossAnimationSampleBridge.Sample invalid = sample(clip(1, 1.0f), 0.5f, 30L);

        assertThrows(IllegalArgumentException.class, () -> sampler.sample(invalid));
    }

    private static BossAnimationSampleBridge.Sample sample(AnimationClip clip, float seconds, long tick) {
        return new BossAnimationSampleBridge.Sample(
            KEY,
            clip,
            seconds,
            AttackTimeline.Phase.TELEGRAPH,
            clip.durationSeconds() == 0.0f ? 0.0D : seconds / clip.durationSeconds(),
            false,
            tick
        );
    }

    private static AnimationClip clip(int joint, float duration) {
        return new AnimationClip("translation", duration, List.of(new AnimationClip.Channel(
            joint,
            AnimationClip.Path.TRANSLATION,
            AnimationClip.Interpolation.LINEAR,
            new float[]{0.0f, duration},
            new float[]{0.0f, 0.0f, 0.0f, 2.0f, 0.0f, 0.0f}
        )));
    }

    private static SkinnedMeshAsset asset() {
        SkinnedTriangleMesh mesh = new SkinnedTriangleMesh(
            new float[]{0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f},
            new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f},
            new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f},
            new int[]{0, 1, 2},
            new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            new float[]{1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f}
        );
        JointRig rig = new JointRig(
            new int[]{0},
            new int[]{-1},
            new String[]{"root"},
            new Affine3x4[]{Affine3x4.identity()},
            new Affine3x4[]{Affine3x4.identity()}
        );
        return new SkinnedMeshAsset(mesh, rig);
    }
}
