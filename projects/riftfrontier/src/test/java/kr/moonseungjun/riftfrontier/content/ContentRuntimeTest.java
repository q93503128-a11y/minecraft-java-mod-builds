package kr.moonseungjun.riftfrontier.content;

import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationProfile;
import kr.moonseungjun.riftfrontier.content.bootstrap.CoreContentBootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ContentRuntimeTest {
    @AfterEach
    void reset() {
        ContentRuntime.resetForTests();
    }

    @Test
    void publishesValidatedSnapshotWithDeterministicCatalog() {
        var pack = CoreContentBootstrap.bootstrapAndValidate();
        var snapshot = ContentRuntime.installValidated(pack.registry(), List.of(pack.packId()));

        assertEquals(1, snapshot.generation());
        assertEquals(10, snapshot.definitionCount());
        assertEquals(List.of(pack.packId()), snapshot.packIds());
        assertEquals(ContentCatalog.from(pack.registry()).fingerprint(), snapshot.fingerprint());
        assertEquals(0, snapshot.bossPresentationProfileCount());
        assertSame(snapshot, ContentRuntime.requireCurrent());
    }

    @Test
    void rejectedCandidateDoesNotReplaceLastKnownGoodSnapshot() {
        var pack = CoreContentBootstrap.bootstrapAndValidate();
        var good = ContentRuntime.installValidated(pack.registry(), List.of(pack.packId()));

        ContentRegistry broken = new ContentRegistry();
        broken.register(new CoreDefinition.Creature(
            ContentId.rift("creature/broken_runtime"),
            ContentId.rift("region/missing"),
            ContentId.rift("archetype/missing"),
            ContentId.rift("loot/missing"),
            Set.of("pursue")
        ));

        assertThrows(IllegalStateException.class, () -> ContentRuntime.installValidated(broken, List.of("riftfrontier:broken")));
        assertSame(good, ContentRuntime.requireCurrent());
        assertEquals(1, ContentRuntime.requireCurrent().generation());
    }

    @Test
    void successfulReplacementAdvancesGenerationAtomically() {
        var firstPack = CoreContentBootstrap.bootstrapAndValidate();
        var first = ContentRuntime.installValidated(firstPack.registry(), List.of(firstPack.packId()));

        var secondPack = CoreContentBootstrap.bootstrapAndValidate();
        var second = ContentRuntime.installValidated(secondPack.registry(), List.of(secondPack.packId()));

        assertEquals(first.generation() + 1, second.generation());
        assertSame(second, ContentRuntime.requireCurrent());
    }

    @Test
    void presentationProfilesPublishInSameGenerationAsCoreContent() {
        ContentRegistry registry = bossRegistry();
        BossPresentationProfile profile = completePresentationProfile();

        var snapshot = ContentRuntime.installValidated(registry, List.of("riftfrontier:test"), List.of(profile));

        assertEquals(1, snapshot.generation());
        assertEquals(1, snapshot.bossPresentationProfileCount());
        assertEquals(profile, snapshot.bossPresentationProfiles().getFirst());
        assertTrue(snapshot.bossPresentationResolver().resolve(
            profile.bossProfile(),
            profile.variant(),
            new kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState(
                1, 40L, true, 1, ContentId.rift("attack/runtime_test").toString(), "ACTIVE", 0.5D,
                "slam", "radial", List.of("move_out"), true
            )
        ).isPresent());
    }

    @Test
    void invalidPresentationCandidatePreservesLastKnownGoodSnapshotAndGeneration() {
        ContentRegistry registry = bossRegistry();
        BossPresentationProfile valid = completePresentationProfile();
        var good = ContentRuntime.installValidated(registry, List.of("riftfrontier:test"), List.of(valid));

        Map<BossPresentationProfile.BindingKey, BossPresentationProfile.AssetBinding> incomplete = new LinkedHashMap<>(valid.bindings());
        incomplete.remove(new BossPresentationProfile.BindingKey("slam", "radial", AttackTimeline.Phase.ACTIVE));
        BossPresentationProfile broken = new BossPresentationProfile(
            valid.id(), valid.bossProfile(), valid.variant(), valid.modelKey(), incomplete
        );

        assertThrows(IllegalStateException.class, () ->
            ContentRuntime.installValidated(registry, List.of("riftfrontier:test"), List.of(broken))
        );
        assertSame(good, ContentRuntime.requireCurrent());
        assertEquals(1, ContentRuntime.requireCurrent().generation());
        assertEquals(1, ContentRuntime.requireCurrent().bossPresentationProfileCount());
    }

    private static ContentRegistry bossRegistry() {
        ContentRegistry registry = new ContentRegistry();
        ContentId attack = ContentId.rift("attack/runtime_test");
        ContentId boss = ContentId.rift("boss/runtime_test");
        registry.register(new CoreDefinition.AttackPattern(attack, "radial", 4, 3, 5, Set.of("move_out"), "slam"));
        registry.register(new CoreDefinition.BossProfile(boss, 2, Set.of(attack), "bounded_arena"));
        return registry;
    }

    private static BossPresentationProfile completePresentationProfile() {
        Map<BossPresentationProfile.BindingKey, BossPresentationProfile.AssetBinding> bindings = new LinkedHashMap<>();
        for (AttackTimeline.Phase phase : List.of(AttackTimeline.Phase.TELEGRAPH, AttackTimeline.Phase.ACTIVE, AttackTimeline.Phase.RECOVERY)) {
            String suffix = phase.name().toLowerCase(java.util.Locale.ROOT);
            bindings.put(
                new BossPresentationProfile.BindingKey("slam", "radial", phase),
                new BossPresentationProfile.AssetBinding(
                    ContentId.rift("animations/slam_" + suffix),
                    ContentId.rift("vfx/slam_" + suffix),
                    ContentId.rift("sounds/slam_" + suffix)
                )
            );
        }
        return new BossPresentationProfile(
            ContentId.rift("presentation/runtime_test"),
            ContentId.rift("boss/runtime_test"),
            "base",
            ContentId.rift("models/runtime_test"),
            bindings
        );
    }
}
