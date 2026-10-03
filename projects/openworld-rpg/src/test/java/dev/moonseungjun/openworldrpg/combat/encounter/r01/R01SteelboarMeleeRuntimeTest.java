package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01SteelboarMeleeRuntimeTest {
    private static final UUID ACTOR =
            UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
    private static final UUID TARGET =
            UUID.fromString("aaaaaaaa-1111-2222-3333-bbbbbbbbbbbb");

    @Test
    void ironTuskImpactContractUsesCanonicalMediumGuardAndPoiseBand() {
        var rule = R01SteelboarImpactAuthority.rule(
                R01SteelboarEncounterData.ActionId.IRON_TUSK
        ).orElseThrow();
        var hit = rule.toIncomingHit(6);

        assertEquals(8, rule.tellTicks());
        assertEquals(7, rule.recoveryTicks());
        assertEquals(0.13, rule.benchmarkDamageShare(), 0.000001);
        assertEquals(28.0, rule.playerPoisePressure(), 0.000001);
        assertEquals(
                PlayerDefenseAuthority.GuardPressureBand.MEDIUM,
                hit.guardPressure().orElseThrow()
        );
        assertTrue(hit.guardable());
        assertTrue(hit.perfectGuardable());
    }

    @Test
    void shoulderHookImpactContractUsesCanonicalHeavyGuardAndPoiseBand() {
        var rule = R01SteelboarImpactAuthority.rule(
                R01SteelboarEncounterData.ActionId.SHOULDER_HOOK
        ).orElseThrow();
        var hit = rule.toIncomingHit(6);

        assertEquals(12, rule.tellTicks());
        assertEquals(14, rule.recoveryTicks());
        assertEquals(0.20, rule.benchmarkDamageShare(), 0.000001);
        assertEquals(45.0, rule.playerPoisePressure(), 0.000001);
        assertEquals(
                PlayerDefenseAuthority.GuardPressureBand.HEAVY,
                hit.guardPressure().orElseThrow()
        );
        assertTrue(hit.guardable());
        assertTrue(hit.perfectGuardable());
    }

    @Test
    void ironRushRemainsExplicitlyBlockedFromImpactAuthority() {
        var rush = R01SteelboarImpactAuthority.rule(
                R01SteelboarEncounterData.ActionId.IRON_RUSH
        ).orElseThrow();

        assertFalse(R01SteelboarImpactAuthority.impactReadyAction(
                R01SteelboarEncounterData.ActionId.IRON_RUSH
        ));
        assertThrows(
                IllegalStateException.class,
                () -> rush.toIncomingHit(6)
        );
    }

    @Test
    void ironTuskOpensOnlyItsExactEightTickImpactFrameAndSevenTickRecovery() {
        var state = new R01SteelboarMeleeExecutionState(
                R01SteelboarEncounterDataLoader.load()
        );
        var decision = decision(
                R01SteelboarEncounterData.ActionId.IRON_TUSK,
                11L,
                false
        );

        assertTrue(state.begin(decision, 100));
        assertEquals(
                R01SteelboarMeleeExecutionState.Phase.WIND_UP,
                state.snapshot(107).orElseThrow().phase()
        );
        assertTrue(state.confirmContact(11L, TARGET, 107).isEmpty());

        var impact = state.snapshot(108).orElseThrow();
        assertEquals(
                R01SteelboarMeleeExecutionState.Phase.IMPACT_FRAME,
                impact.phase()
        );
        assertEquals(108L, impact.impactTick());
        assertEquals(115L, impact.recoveryEndTick());
        assertEquals(
                R01SteelboarEncounterData.ActionId.IRON_TUSK,
                state.confirmContact(11L, TARGET, 108).orElseThrow()
        );
        assertTrue(state.confirmContact(11L, TARGET, 108).isEmpty());
        assertEquals(
                R01SteelboarMeleeExecutionState.Phase.RECOVERY,
                state.snapshot(114).orElseThrow().phase()
        );
        assertTrue(state.snapshot(115).isEmpty());
    }

    @Test
    void shoulderHookUsesTwelveTickTellAndFourteenTickRecovery() {
        var state = new R01SteelboarMeleeExecutionState(
                R01SteelboarEncounterDataLoader.load()
        );
        var decision = decision(
                R01SteelboarEncounterData.ActionId.SHOULDER_HOOK,
                7L,
                false
        );

        assertTrue(state.begin(decision, 200));
        var impact = state.snapshot(212).orElseThrow();
        assertEquals(
                R01SteelboarMeleeExecutionState.Phase.IMPACT_FRAME,
                impact.phase()
        );
        assertEquals(226L, impact.recoveryEndTick());
        assertEquals(
                R01SteelboarEncounterData.ActionId.SHOULDER_HOOK,
                state.confirmContact(7L, TARGET, 212).orElseThrow()
        );
        assertTrue(state.snapshot(226).isEmpty());
    }

    @Test
    void executionGateRejectsIronRushFuriousRouteAndConcurrentMelee() {
        var state = new R01SteelboarMeleeExecutionState(
                R01SteelboarEncounterDataLoader.load()
        );

        assertFalse(state.begin(
                decision(
                        R01SteelboarEncounterData.ActionId.IRON_RUSH,
                        0L,
                        false
                ),
                0
        ));
        assertFalse(state.begin(
                decision(
                        R01SteelboarEncounterData.ActionId.IRON_RUSH,
                        1L,
                        true
                ),
                1
        ));

        assertTrue(state.begin(
                decision(
                        R01SteelboarEncounterData.ActionId.IRON_TUSK,
                        2L,
                        false
                ),
                10
        ));
        assertFalse(state.begin(
                decision(
                        R01SteelboarEncounterData.ActionId.SHOULDER_HOOK,
                        3L,
                        false
                ),
                11
        ));
    }

    private static R01SteelboarActionController.Decision decision(
            R01SteelboarEncounterData.ActionId action,
            long actionCounter,
            boolean furiousRoute
    ) {
        return new R01SteelboarActionController.Decision(
                Optional.of(action),
                actionCounter,
                furiousRoute,
                false,
                0L,
                0L
        );
    }
}
