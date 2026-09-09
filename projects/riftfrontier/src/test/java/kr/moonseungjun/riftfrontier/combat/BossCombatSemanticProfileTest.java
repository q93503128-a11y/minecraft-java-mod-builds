package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationProfile;
import kr.moonseungjun.riftfrontier.content.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BossCombatSemanticProfileTest {
    private static final ContentId BOSS = ContentId.parse("riftfrontier:test_boss");
    private static final ContentId A = ContentId.parse("riftfrontier:attack_a");
    private static final ContentId B = ContentId.parse("riftfrontier:attack_b");

    @Test
    void codecAndValidatorRequireExactPhaseCoverageAndLogicalAnimationCoverage() {
        ContentRegistry registry = registry();
        CombatRuntimeCatalog catalog = new CombatRuntimeCatalog(registry);
        BossCombatSemanticProfile semantics = BossCombatSemanticProfile.decode("""
            {"boss_profile":"riftfrontier:test_boss","phase_attack_patterns":{
              "1":["riftfrontier:attack_a"],"2":["riftfrontier:attack_b"]
            }}
            """);

        ValidatedBossCombatSemantics validated = ValidatedBossCombatSemantics.validate(
            catalog, semantics, presentation(true)
        );

        assertEquals(List.of(A), validated.candidateAttacks(1));
        assertEquals(List.of(B), validated.candidateAttacks(2));
        assertEquals(ContentId.parse("riftfrontier:a_active"),
            validated.logicalAnimationKey(A, AttackTimeline.Phase.ACTIVE));
    }

    @Test
    void validationFailsClosedForMissingPhaseOrAttackOutsideBoss() {
        CombatRuntimeCatalog catalog = new CombatRuntimeCatalog(registry());
        BossCombatSemanticProfile missingPhase = new BossCombatSemanticProfile(BOSS, Map.of(1, Set.of(A)));
        assertThrows(IllegalArgumentException.class,
            () -> ValidatedBossCombatSemantics.validate(catalog, missingPhase, presentation(true)));

        ContentId outsider = ContentId.parse("riftfrontier:outsider");
        BossCombatSemanticProfile outside = new BossCombatSemanticProfile(
            BOSS, Map.of(1, Set.of(A), 2, Set.of(outsider))
        );
        assertThrows(IllegalArgumentException.class,
            () -> ValidatedBossCombatSemantics.validate(catalog, outside, presentation(true)));
    }

    @Test
    void validationFailsClosedWhenLogicalAnimationPhaseIsMissing() {
        BossCombatSemanticProfile semantics = new BossCombatSemanticProfile(
            BOSS, Map.of(1, Set.of(A), 2, Set.of(B))
        );
        assertThrows(IllegalArgumentException.class,
            () -> ValidatedBossCombatSemantics.validate(
                new CombatRuntimeCatalog(registry()), semantics, presentation(false)
            ));
    }

    @Test
    void authoredPolicyUsesOnlyCurrentPhasePool() {
        BossCombatSemanticProfile semantics = new BossCombatSemanticProfile(
            BOSS, Map.of(1, Set.of(A), 2, Set.of(B))
        );
        ValidatedBossCombatSemantics validated = ValidatedBossCombatSemantics.validate(
            new CombatRuntimeCatalog(registry()), semantics, presentation(true)
        );
        BossAttackSelectionPolicy policy = BossAttackSelectionPolicy.authoredPhases(validated);

        var phase1 = new BossAttackSelectionPolicy.SelectionContext(BOSS, 1, 2, List.of(A, B), 7, Optional.of(B));
        var phase2 = new BossAttackSelectionPolicy.SelectionContext(BOSS, 2, 2, List.of(A, B), 8, Optional.of(A));
        assertEquals(A, policy.select(phase1));
        assertEquals(B, policy.select(phase2));
    }

    private static ContentRegistry registry() {
        ContentRegistry registry = new ContentRegistry();
        registry.register(new CoreDefinition.AttackPattern(A, "melee", 1, 1, 0, Set.of("dodge"), "swipe"));
        registry.register(new CoreDefinition.AttackPattern(B, "melee", 1, 1, 0, Set.of("dodge"), "slam"));
        registry.register(new CoreDefinition.BossProfile(BOSS, 2, Set.of(A, B), "sealed_arena"));
        return registry;
    }

    private static BossPresentationProfile presentation(boolean complete) {
        Map<BossPresentationProfile.BindingKey, BossPresentationProfile.AssetBinding> bindings = new LinkedHashMap<>();
        add(bindings, "swipe", "melee", "a");
        add(bindings, "slam", "melee", "b");
        if (!complete) {
            bindings.remove(new BossPresentationProfile.BindingKey("slam", "melee", AttackTimeline.Phase.RECOVERY));
        }
        return new BossPresentationProfile(
            ContentId.parse("riftfrontier:test_boss_presentation"),
            BOSS,
            "default",
            ContentId.parse("riftfrontier:test_model"),
            bindings
        );
    }

    private static void add(
        Map<BossPresentationProfile.BindingKey, BossPresentationProfile.AssetBinding> bindings,
        String cue, String delivery, String prefix
    ) {
        for (AttackTimeline.Phase phase : List.of(
            AttackTimeline.Phase.TELEGRAPH, AttackTimeline.Phase.ACTIVE, AttackTimeline.Phase.RECOVERY
        )) {
            String suffix = phase.name().toLowerCase(Locale.ROOT);
            bindings.put(
                new BossPresentationProfile.BindingKey(cue, delivery, phase),
                new BossPresentationProfile.AssetBinding(
                    ContentId.parse("riftfrontier:" + prefix + "_" + suffix),
                    ContentId.parse("riftfrontier:" + prefix + "_vfx_" + suffix),
                    ContentId.parse("riftfrontier:" + prefix + "_sound_" + suffix)
                )
            );
        }
    }
}
