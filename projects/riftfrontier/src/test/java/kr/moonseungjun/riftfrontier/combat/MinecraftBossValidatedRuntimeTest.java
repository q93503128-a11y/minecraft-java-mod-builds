package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationProfile;
import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentRegistry;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class MinecraftBossValidatedRuntimeTest {
    private static final ContentId BOSS = ContentId.parse("riftfrontier:validated_runtime_boss");
    private static final ContentId PHASE_ONE = ContentId.parse("riftfrontier:validated_runtime_phase_one");
    private static final ContentId PHASE_TWO = ContentId.parse("riftfrontier:validated_runtime_phase_two");

    @Test
    void productionFactoryDerivesBossIdentityAndPhasePolicyFromExactSemanticCapability() {
        ContentRegistry registry = registry();
        CombatRuntimeCatalog catalog = new CombatRuntimeCatalog(registry);
        ValidatedBossCombatSemantics semantics = ValidatedBossCombatSemantics.validate(
            catalog,
            new BossCombatSemanticProfile(BOSS, Map.of(1, Set.of(PHASE_ONE), 2, Set.of(PHASE_TWO))),
            presentation()
        );

        MinecraftBossCombatAdapter.ValidatedRuntime runtime = MinecraftBossCombatAdapter.validated(
            catalog,
            semantics,
            (level, attacker, snapshot) -> List.of(),
            1.0F
        );

        assertSame(semantics, runtime.semanticCapability());
        assertEquals(BOSS, runtime.bossProfile());
        assertEquals(1, runtime.phase());
        assertEquals(PHASE_ONE, runtime.beginNextAttack(100L).patternId());

        BossCombatController.PhaseTransition transition = runtime.transitionToPhase(2);
        assertEquals(1, transition.previousPhase());
        assertEquals(2, transition.newPhase());
        assertEquals(PHASE_ONE, transition.interruptedAttack().orElseThrow());
        assertEquals(PHASE_TWO, runtime.beginNextAttack(200L).patternId());
    }

    private static ContentRegistry registry() {
        ContentRegistry registry = new ContentRegistry();
        registry.register(new CoreDefinition.AttackPattern(
            PHASE_ONE, "arc_melee", 1, 1, 0, Set.of("dodge"), "phase_one_cue"
        ));
        registry.register(new CoreDefinition.AttackPattern(
            PHASE_TWO, "line_charge", 1, 1, 0, Set.of("sidestep"), "phase_two_cue"
        ));
        registry.register(new CoreDefinition.BossProfile(BOSS, 2, Set.of(PHASE_ONE, PHASE_TWO), "sealed_arena"));
        return registry;
    }

    private static BossPresentationProfile presentation() {
        Map<BossPresentationProfile.BindingKey, BossPresentationProfile.AssetBinding> bindings = new LinkedHashMap<>();
        addBindings(bindings, "phase_one_cue", "arc_melee", "phase_one");
        addBindings(bindings, "phase_two_cue", "line_charge", "phase_two");
        return new BossPresentationProfile(
            ContentId.parse("riftfrontier:validated_runtime_presentation"),
            BOSS,
            "default",
            ContentId.parse("riftfrontier:validated_runtime_model"),
            bindings
        );
    }

    private static void addBindings(
        Map<BossPresentationProfile.BindingKey, BossPresentationProfile.AssetBinding> bindings,
        String cue,
        String delivery,
        String prefix
    ) {
        for (AttackTimeline.Phase phase : List.of(
            AttackTimeline.Phase.TELEGRAPH,
            AttackTimeline.Phase.ACTIVE,
            AttackTimeline.Phase.RECOVERY
        )) {
            String suffix = phase.name().toLowerCase(Locale.ROOT);
            bindings.put(
                new BossPresentationProfile.BindingKey(cue, delivery, phase),
                new BossPresentationProfile.AssetBinding(
                    ContentId.parse("riftfrontier:" + prefix + "_anim_" + suffix),
                    ContentId.parse("riftfrontier:" + prefix + "_vfx_" + suffix),
                    ContentId.parse("riftfrontier:" + prefix + "_sound_" + suffix)
                )
            );
        }
    }
}
