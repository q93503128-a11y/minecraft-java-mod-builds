package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01RegalhartClosedImpactRuntimeTest {
    private static final UUID TARGET =
            UUID.fromString("abcdefab-cdef-abcd-efab-cdefabcdefab");

    @Test
    void antlerSweepImpactContractIsClosedWithoutInventingPlayerPoise() {
        var data = R01RegalhartEncounterDataLoader.load();
        var rule = data.rulesById().get(
                R01RegalhartEncounterData.ActionId.ANTLER_SWEEP
        );
        var hit = rule.toIncomingHit(8);

        assertEquals(0.11, rule.benchmarkDamageShare(), 0.000001);
        assertEquals(9, rule.tellTicks());
        assertEquals(8, rule.recoveryTicks());
        assertEquals(
                PlayerDefenseAuthority.GuardPressureBand.MEDIUM,
                hit.guardPressure().orElseThrow()
        );
        assertTrue(hit.guardable());
        assertTrue(hit.perfectGuardable());
        assertEquals(null, rule.playerPoisePressure());
        assertTrue(R01RegalhartImpactAuthority.impactReadyAction(
                R01RegalhartEncounterData.ActionId.ANTLER_SWEEP
        ));
    }

    @Test
    void royalBoundImpactIsUnguardableAndKeepsExactFourPointFiveRadius() {
        var data = R01RegalhartEncounterDataLoader.load();
        var rule = data.rulesById().get(
                R01RegalhartEncounterData.ActionId.ROYAL_BOUND
        );
        var hit = rule.toIncomingHit(8);

        assertEquals(0.30, rule.benchmarkDamageShare(), 0.000001);
        assertEquals(4.5, rule.areaRadius(), 0.000001);
        assertFalse(hit.guardable());
        assertFalse(hit.perfectGuardable());
        assertTrue(hit.guardPressure().isEmpty());
        assertTrue(R01RegalhartImpactAuthority.impactReadyAction(
                R01RegalhartEncounterData.ActionId.ROYAL_BOUND
        ));

        assertTrue(R01RegalhartCombatRuntime.insideHorizontalRadius(
                0.0, 0.0, 4.5, 0.0, 4.5
        ));
        assertFalse(R01RegalhartCombatRuntime.insideHorizontalRadius(
                0.0, 0.0, 4.500001, 0.0, 4.5
        ));
    }

    @Test
    void crownChargeAndRearKickImpactContractsAreClosed() {
        var data = R01RegalhartEncounterDataLoader.load();

        var charge = data.rulesById().get(
                R01RegalhartEncounterData.ActionId.CROWN_CHARGE
        );
        var chargeHit = charge.toIncomingHit(8);
        assertFalse(chargeHit.guardable());
        assertTrue(chargeHit.perfectGuardable());
        assertEquals(
                PlayerDefenseAuthority.GuardPressureBand.HEAVY,
                chargeHit.guardPressure().orElseThrow()
        );
        assertEquals(70.0, charge.playerPoisePressure(), 0.000001);
        assertEquals(1.25, charge.perfectGuardPoiseMultiplier(), 0.000001);
        assertTrue(R01RegalhartImpactAuthority.impactReadyAction(
                R01RegalhartEncounterData.ActionId.CROWN_CHARGE
        ));

        var kick = data.rulesById().get(
                R01RegalhartEncounterData.ActionId.REAR_KICK
        );
        var kickHit = kick.toIncomingHit(8);
        assertTrue(kickHit.guardable());
        assertTrue(kickHit.perfectGuardable());
        assertEquals(
                PlayerDefenseAuthority.GuardPressureBand.MEDIUM,
                kickHit.guardPressure().orElseThrow()
        );
        assertEquals(28.0, kick.playerPoisePressure(), 0.000001);
        assertTrue(R01RegalhartImpactAuthority.impactReadyAction(
                R01RegalhartEncounterData.ActionId.REAR_KICK
        ));
    }

    @Test
    void nonComboSweepUsesExactNineTickImpactAndEightTickRecovery() {
        var state = new R01RegalhartSweepExecutionState(
                R01RegalhartEncounterDataLoader.load()
        );
        var decision = decision(false, 9L);

        assertTrue(state.begin(decision, 100));
        assertEquals(
                R01RegalhartSweepExecutionState.Phase.WIND_UP,
                state.snapshot(108).orElseThrow().phase()
        );
        assertTrue(state.confirmContact(9L, TARGET, 108).isEmpty());

        var impact = state.snapshot(109).orElseThrow();
        assertEquals(
                R01RegalhartSweepExecutionState.Phase.IMPACT_FRAME,
                impact.phase()
        );
        assertEquals(109L, impact.impactTick());
        assertEquals(0L, impact.secondImpactTick());
        assertEquals(117L, impact.recoveryEndTick());
        assertEquals(
                R01RegalhartEncounterData.ActionId.ANTLER_SWEEP,
                state.confirmContact(9L, TARGET, 109).orElseThrow()
        );
        assertTrue(state.confirmContact(9L, TARGET, 109).isEmpty());
        assertTrue(state.snapshot(117).isEmpty());
    }

    @Test
    void mirroredSweepOwnsTwoReadableImpactFramesAndPerFrameDeduplication() {
        var state = new R01RegalhartSweepExecutionState(
                R01RegalhartEncounterDataLoader.load()
        );
        assertTrue(state.begin(decision(true, 8, 10L), 100));

        var first = state.snapshot(109).orElseThrow();
        assertEquals(R01RegalhartSweepExecutionState.Phase.IMPACT_FRAME, first.phase());
        assertEquals(109L, first.impactTick());
        assertEquals(117L, first.secondImpactTick());
        assertEquals(125L, first.recoveryEndTick());
        assertTrue(state.confirmContact(10L, TARGET, 109).isPresent());
        assertTrue(state.confirmContact(10L, TARGET, 109).isEmpty());
        assertEquals(
                R01RegalhartSweepExecutionState.Phase.FOLLOW_UP_GAP,
                state.snapshot(110).orElseThrow().phase()
        );
        assertTrue(state.confirmContact(10L, TARGET, 117).isPresent());
        assertTrue(state.confirmContact(10L, TARGET, 117).isEmpty());
        assertTrue(state.snapshot(125).isEmpty());
    }

    @Test
    void sovereignMirroredSweepUsesSixTickFollowUpDelay() {
        var state = new R01RegalhartSweepExecutionState(
                R01RegalhartEncounterDataLoader.load()
        );
        assertTrue(state.begin(decision(true, 6, 11L), 0));
        var snapshot = state.snapshot(9).orElseThrow();
        assertEquals(15L, snapshot.secondImpactTick());
        assertEquals(23L, snapshot.recoveryEndTick());
    }

    private static R01RegalhartActionController.Decision decision(
            boolean mirroredFollowUp,
            long counter
    ) {
        return decision(mirroredFollowUp, mirroredFollowUp ? 8 : 0, counter);
    }

    private static R01RegalhartActionController.Decision decision(
            boolean mirroredFollowUp,
            int followUpDelayTicks,
            long counter
    ) {
        return new R01RegalhartActionController.Decision(
                R01RegalhartActionController.Mode.ATTACK,
                Optional.of(
                        R01RegalhartEncounterData.ActionId.ANTLER_SWEEP
                ),
                counter,
                mirroredFollowUp,
                followUpDelayTicks,
                false,
                0L
        );
    }
}
