package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01NatureSpiritActionExecutionStateTest {
    private static final UUID ACTOR =
            UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID TARGET_A =
            UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID TARGET_B =
            UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff");

    @Test
    void rootedSwipeUsesExactNineTickImpactFrameAndNoAuthoredRecovery() {
        var runtime = runtimeState("rooted-swipe");
        var decision = runtime.trySelectAndBeginAtDecision(
                true, false, false,
                2.0,
                78.0, 78.0,
                790.0,
                100
        ).orElseThrow();

        assertEquals(
                R01SecondaryCreatureEncounterData.ActionId.ROOTED_SWIPE,
                decision.action().orElseThrow()
        );
        var windup = runtime.executionSnapshot(108).orElseThrow();
        assertEquals(
                R01NatureSpiritActionExecutionState.Phase.WIND_UP,
                windup.phase()
        );
        assertEquals(109L, windup.impactTick());
        assertEquals(109L, windup.recoveryEndTick());

        assertTrue(runtime.confirmScheduledContact(
                decision.actionCounter(),
                TARGET_A,
                108
        ).isEmpty());
        assertEquals(
                R01SecondaryCreatureEncounterData.ActionId.ROOTED_SWIPE,
                runtime.confirmScheduledContact(
                        decision.actionCounter(),
                        TARGET_A,
                        109
                ).orElseThrow()
        );
        assertTrue(runtime.confirmScheduledContact(
                decision.actionCounter(),
                TARGET_A,
                109
        ).isEmpty());
        assertTrue(runtime.executionSnapshot(110).isEmpty());
    }

    @Test
    void earthenRamLocksThreeBlockCommitmentAndSeventeenTickRecovery() {
        var runtime = runtimeState("earthen-ram");
        var decision = runtime.trySelectAndBeginAtDecision(
                false, true, false,
                3.0,
                78.0, 78.0,
                790.0,
                200
        ).orElseThrow();

        var committed = runtime.executionSnapshot(200).orElseThrow();
        assertEquals(
                R01SecondaryCreatureEncounterData.ActionId.EARTHEN_RAM,
                committed.action()
        );
        assertEquals(3.0, committed.committedMovementBlocks(), 0.000001);
        assertEquals(215L, committed.impactTick());
        assertEquals(232L, committed.recoveryEndTick());

        assertEquals(
                R01NatureSpiritActionExecutionState.Phase.IMPACT_FRAME,
                runtime.executionSnapshot(215).orElseThrow().phase()
        );
        assertEquals(
                R01SecondaryCreatureEncounterData.ActionId.EARTHEN_RAM,
                runtime.confirmScheduledContact(
                        decision.actionCounter(),
                        TARGET_A,
                        215
                ).orElseThrow()
        );
        assertEquals(
                R01NatureSpiritActionExecutionState.Phase.RECOVERY,
                runtime.executionSnapshot(231).orElseThrow().phase()
        );
        assertTrue(runtime.executionSnapshot(232).isEmpty());
    }

    @Test
    void bloomQuakeAllowsMultipleTargetsOnceOnTheSameFourBlockImpactFrame() {
        var runtime = runtimeState("bloom-quake");
        var decision = runtime.trySelectAndBeginAtDecision(
                false, false, true,
                3.5,
                78.0, 78.0,
                790.0,
                0
        ).orElseThrow();

        var committed = runtime.executionSnapshot(0).orElseThrow();
        assertEquals(4.0, committed.areaRadius(), 0.000001);
        assertEquals(20L, committed.impactTick());
        assertEquals(38L, committed.recoveryEndTick());

        assertEquals(
                R01SecondaryCreatureEncounterData.ActionId.BLOOM_QUAKE,
                runtime.confirmScheduledContact(
                        decision.actionCounter(),
                        TARGET_A,
                        20
                ).orElseThrow()
        );
        assertEquals(
                R01SecondaryCreatureEncounterData.ActionId.BLOOM_QUAKE,
                runtime.confirmScheduledContact(
                        decision.actionCounter(),
                        TARGET_B,
                        20
                ).orElseThrow()
        );
        assertTrue(runtime.confirmScheduledContact(
                decision.actionCounter(),
                TARGET_A,
                20
        ).isEmpty());
        assertTrue(runtime.confirmScheduledContact(
                decision.actionCounter(),
                UUID.randomUUID(),
                21
        ).isEmpty());
    }

    @Test
    void committedActionBlocksASecondDecisionUntilRecoveryEnds() {
        var runtime = runtimeState("decision-lock");
        var first = runtime.trySelectAndBeginAtDecision(
                false, true, false,
                3.0,
                78.0, 78.0,
                790.0,
                50
        );
        assertTrue(first.isPresent());

        var blocked = runtime.trySelectAndBeginAtDecision(
                true, false, false,
                2.0,
                78.0, 78.0,
                790.0,
                60
        );
        assertTrue(blocked.isEmpty());

        var ready = runtime.trySelectAndBeginAtDecision(
                true, false, false,
                2.0,
                78.0, 78.0,
                790.0,
                82
        );
        assertTrue(ready.isPresent());
        assertEquals(
                R01SecondaryCreatureEncounterData.ActionId.ROOTED_SWIPE,
                ready.orElseThrow().action().orElseThrow()
        );
    }

    private static R01NatureSpiritCombatRuntimeState runtimeState(
            String encounterId
    ) {
        return new R01NatureSpiritCombatRuntimeState(
                encounterId,
                ACTOR
        );
    }
}
