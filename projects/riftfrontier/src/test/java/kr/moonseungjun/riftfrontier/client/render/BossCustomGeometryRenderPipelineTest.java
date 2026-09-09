package kr.moonseungjun.riftfrontier.client.render;

import kr.moonseungjun.riftfrontier.client.BossPresentationClientState;
import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;
import kr.moonseungjun.riftfrontier.combat.presentation.BossAnimationSampleBridge;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationProfile;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationResolver;
import kr.moonseungjun.riftfrontier.combat.presentation.BossSkinnedMeshFrameSampler;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.Affine3x4;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClip;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.JointRig;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.SkinnedMeshAsset;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.SkinnedTriangleMesh;
import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossCustomGeometryRenderPipelineTest {
    private static final ContentId BOSS = ContentId.parse("riftfrontier:boss/region_01/test");
    private static final ContentId PRESENTATION = ContentId.parse("riftfrontier:presentation/boss/region_01/test");
    private static final ContentId MODEL = ContentId.parse("riftfrontier:model/boss/region_01/test");
    private static final ContentId ANIMATION = ContentId.parse("riftfrontier:animation/boss/region_01/test_telegraph");
    private static final ContentId VFX = ContentId.parse("riftfrontier:vfx/boss/region_01/test");
    private static final ContentId SOUND = ContentId.parse("riftfrontier:sound/boss/region_01/test");

    @AfterEach
    void clearClientState() {
        BossPresentationClientState.clearAll();
    }

    @Test
    void authoritativeSemanticSnapshotResolvesThroughSkinningIntoPreparedGeometry() {
        BossCustomGeometryRenderPipeline pipeline = pipeline();
        BossPresentationSemanticState state = state(42, 100L, 0.5D);

        var prepared = pipeline.prepare(state).orElseThrow();

        assertEquals(42, prepared.entityId());
        assertEquals(ANIMATION, prepared.resolvedPresentation().animationKey());
        assertEquals(1.0f, prepared.frameSample().authoritativeSample().sampleTimeSeconds(), 1.0e-6f);
        assertEquals(3, prepared.geometry().vertexCount());
        assertEquals(1, prepared.geometry().triangleCount());
        assertEquals(1.0f, prepared.geometry().positions()[0], 1.0e-6f);
    }

    @Test
    void currentEntityPathUsesMonotonicServerSnapshotCache() {
        BossCustomGeometryRenderPipeline pipeline = pipeline();
        assertTrue(BossPresentationClientState.accept(state(7, 20L, 0.75D)));
        assertFalse(BossPresentationClientState.accept(state(7, 19L, 0.25D)));

        var prepared = pipeline.prepareCurrent(7).orElseThrow();

        assertEquals(1.5f, prepared.frameSample().authoritativeSample().sampleTimeSeconds(), 1.0e-6f);
        assertEquals(1.5f, prepared.geometry().positions()[0], 1.0e-6f);
    }

    @Test
    void missingOrInactivePresentationDoesNotInventFallbackGeometry() {
        BossCustomGeometryRenderPipeline pipeline = pipeline();
        assertTrue(pipeline.prepare(BossPresentationSemanticState.clear(3, 1L)).isEmpty());
        assertTrue(pipeline.prepareCurrent(999).isEmpty());

        BossPresentationSemanticState unresolved = new BossPresentationSemanticState(
            3, 2L, true, 1, "riftfrontier:attack/test", AttackTimeline.Phase.TELEGRAPH.name(), 0.5D,
            "other_cue", "melee", List.of("dodge"), false
        );
        assertTrue(pipeline.prepare(unresolved).isEmpty());
    }

    @Test
    void pipelineExposesTheExactMeshIdentityConsumedByItsSampler() {
        SkinnedMeshAsset accepted = asset();
        BossCustomGeometryRenderPipeline pipeline = pipeline(accepted);

        assertSame(accepted, pipeline.skinnedMeshAsset());
    }

    private static BossCustomGeometryRenderPipeline pipeline() {
        return pipeline(asset());
    }

    private static BossCustomGeometryRenderPipeline pipeline(SkinnedMeshAsset meshAsset) {
        var key = new BossPresentationProfile.BindingKey("test_cue", "melee", AttackTimeline.Phase.TELEGRAPH);
        var profile = new BossPresentationProfile(
            PRESENTATION,
            BOSS,
            "default",
            MODEL,
            Map.of(key, new BossPresentationProfile.AssetBinding(ANIMATION, VFX, SOUND))
        );
        var resolver = new BossPresentationResolver(List.of(profile));
        var clip = clip();
        var bridge = new BossAnimationSampleBridge(Map.of(ANIMATION, clip));
        return new BossCustomGeometryRenderPipeline(
            BOSS,
            "default",
            resolver,
            bridge,
            new BossSkinnedMeshFrameSampler(meshAsset)
        );
    }

    private static BossPresentationSemanticState state(int entityId, long tick, double progress) {
        return new BossPresentationSemanticState(
            entityId,
            tick,
            true,
            1,
            "riftfrontier:attack/test",
            AttackTimeline.Phase.TELEGRAPH.name(),
            progress,
            "test_cue",
            "melee",
            List.of("dodge"),
            false
        );
    }

    private static AnimationClip clip() {
        return new AnimationClip("telegraph", 2.0f, List.of(new AnimationClip.Channel(
            0,
            AnimationClip.Path.TRANSLATION,
            AnimationClip.Interpolation.LINEAR,
            new float[]{0.0f, 2.0f},
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
