package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01RegalhartActionControllerTest {
    private static final UUID ACTOR =
            UUID.fromString("10000000-2000-3000-4000-500000000000");

    @Test
    void bundledRegalhartSelectionAndPhaseDataMatchCanonWithoutInventingImpactFields() {
        var data = R01RegalhartEncounterDataLoader.load();
        var rules = data.rulesById();

        assertEquals("threateningly_mobs:the_regalhart", data.entityId());
        assertEquals(8, data.contentLevel());
        assertEquals(1.25, data.weakPointMultiplier(), 0.000001);

        var sweep = rules.get(R01RegalhartEncounterData.ActionId.ANTLER_SWEEP);
        assertEquals(9, sweep.tellTicks());
        assertEquals(8, sweep.recoveryTicks());
        assertEquals(0.11, sweep.benchmarkDamageShare(), 0.000001);
        assertEquals(null, sweep.playerPoisePressure());

        var charge = rules.get(R01RegalhartEncounterData.ActionId.CROWN_CHARGE);
        assertEquals(140, charge.cooldownTicks());
        assertEquals(18, charge.tellTicks());
        assertEquals(16.0, charge.movementEnvelopeBlocks(), 0.000001);
        assertFalse(charge.impactContractClosed());

        var kick = rules.get(R01RegalhartEncounterData.ActionId.REAR_KICK);
        assertEquals(60, kick.cooldownTicks());
        assertEquals(3.5, kick.maximumRange(), 0.000001);
        assertFalse(kick.impactContractClosed());

        var bound = rules.get(R01RegalhartEncounterData.ActionId.ROYAL_BOUND);
        assertEquals(180, bound.cooldownTicks());
        assertEquals(22, bound.tellTicks());
        assertEquals(4.5, bound.areaRadius(), 0.000001);

        var sovereign = data.sovereign();
        assertEquals(0.40, sovereign.healthFractionInclusive(), 0.000001);
        assertEquals(30, sovereign.transitionTicks());
        assertEquals(0.50, sovereign.transitionDamageTakenMultiplier(), 0.000001);
        assertEquals(1.10, sovereign.movementSpeedMultiplier(), 0.000001);
        assertEquals(2, sovereign.crownChargeComboEvery());
        assertEquals(13, sovereign.secondChargeMinimumPivotTellTicks());
        assertEquals(28, sovereign.chainedChargeRecoveryTicks());
    }

    @Test
    void rearKickOwnsRearProtectionBeforeCloseSweep() {
        var decision = controller("rear").select(
                context(3.0, true, false, true, true, 1.0),
                0
        );
        assertEquals(
                R01RegalhartEncounterData.ActionId.REAR_KICK,
                decision.action().orElseThrow()
        );

        var closeFront = controller("front").select(
                context(3.0, false, false, true, true, 1.0),
                0
        );
        assertEquals(
                R01RegalhartEncounterData.ActionId.ANTLER_SWEEP,
                closeFront.action().orElseThrow()
        );
    }

    @Test
    void exactFourPointFiveDistanceRemainsCloseSweep() {
        var decision = controller("close-boundary").select(
                context(4.5, false, false, true, true, 1.0),
                0
        );
        assertEquals(
                R01RegalhartEncounterData.ActionId.ANTLER_SWEEP,
                decision.action().orElseThrow()
        );
    }

    @Test
    void unresolvedSevenAndTwelveEndpointsFailClosedInsteadOfInventingWeights() {
        assertTrue(R01RegalhartActionController.exactUnresolvedWeightBoundary(7.0));
        assertTrue(R01RegalhartActionController.exactUnresolvedWeightBoundary(12.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> R01RegalhartActionController.distanceWeights(7.0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> R01RegalhartActionController.distanceWeights(12.0)
        );

        assertTrue(controller("seven").select(
                context(7.0, false, false, true, true, 1.0),
                0
        ).reposition());
        assertTrue(controller("twelve").select(
                context(12.0, false, false, true, true, 1.0),
                0
        ).reposition());

        assertArrayEquals(
                new int[] {45, 55},
                R01RegalhartActionController.distanceWeights(6.999)
        );
        assertArrayEquals(
                new int[] {60, 40},
                R01RegalhartActionController.distanceWeights(7.001)
        );
        assertArrayEquals(
                new int[] {75, 25},
                R01RegalhartActionController.distanceWeights(12.001)
        );
    }

    @Test
    void coolingOrIllegalMidFarActionsAreRemovedAndNoLegalActionRepositions() {
        var controller = controller("availability");
        var first = controller.select(
                context(10.0, false, false, true, false, 1.0),
                0
        );
        assertEquals(
                R01RegalhartEncounterData.ActionId.CROWN_CHARGE,
                first.action().orElseThrow()
        );

        var boundOnly = controller.select(
                context(10.0, false, false, true, true, 1.0),
                1
        );
        assertEquals(
                R01RegalhartEncounterData.ActionId.ROYAL_BOUND,
                boundOnly.action().orElseThrow()
        );

        assertTrue(controller.select(
                context(10.0, false, false, false, false, 1.0),
                2
        ).reposition());
        assertTrue(controller.select(
                context(16.01, false, false, true, true, 1.0),
                3
        ).reposition());
    }

    @Test
    void signatureMovementCannotRepeatWhenTheOtherSignatureIsLegal() {
        var controller = controller("signature-repeat");
        var first = controller.select(
                context(10.0, false, false, true, false, 1.0),
                0
        );
        assertEquals(
                R01RegalhartEncounterData.ActionId.CROWN_CHARGE,
                first.action().orElseThrow()
        );

        var second = controller.select(
                context(10.0, false, false, true, true, 1.0),
                140
        );
        assertEquals(
                R01RegalhartEncounterData.ActionId.ROYAL_BOUND,
                second.action().orElseThrow()
        );
    }

    @Test
    void sovereignTransitionOccursOnceAndLocksSelectionForThirtyTicks() {
        var controller = controller("sovereign");

        var start = controller.select(
                context(3.0, false, false, true, true, 0.40),
                100
        );
        assertEquals(
                R01RegalhartActionController.Mode.SOVEREIGN_TRANSITION_START,
                start.mode()
        );
        assertEquals(130L, start.sovereignTransitionEndTick());
        assertEquals(30L, controller.sovereignTransitionRemainingTicks(100));

        var hold = controller.select(
                context(3.0, false, false, true, true, 0.40),
                129
        );
        assertEquals(
                R01RegalhartActionController.Mode.SOVEREIGN_TRANSITION_HOLD,
                hold.mode()
        );

        var attack = controller.select(
                context(3.0, false, false, true, true, 0.40),
                130
        );
        assertEquals(
                R01RegalhartEncounterData.ActionId.ANTLER_SWEEP,
                attack.action().orElseThrow()
        );
    }

    @Test
    void sweepCadenceAdvancesEvenWhenDueFollowUpHasNoLegalArc() {
        var normal = controller("normal-sweep");
        assertFalse(normal.select(
                context(4.0, false, true, true, true, 1.0), 0
        ).mirroredSweepFollowUp());
        assertFalse(normal.select(
                context(4.0, false, true, true, true, 1.0), 17
        ).mirroredSweepFollowUp());

        var skippedDue = normal.select(
                context(4.0, false, false, true, true, 1.0), 34
        );
        assertFalse(skippedDue.mirroredSweepFollowUp());
        assertEquals(3, normal.antlerSweepCounter());

        assertFalse(normal.select(
                context(4.0, false, true, true, true, 1.0), 51
        ).mirroredSweepFollowUp());

        var sovereign = controller("sovereign-sweep");
        sovereign.select(
                context(4.0, false, true, true, true, 0.40), 0
        );
        assertFalse(sovereign.select(
                context(4.0, false, true, true, true, 0.40), 30
        ).mirroredSweepFollowUp());
        assertTrue(sovereign.select(
                context(4.0, false, true, true, true, 0.40), 47
        ).mirroredSweepFollowUp());
    }

    @Test
    void everySecondSovereignCrownChargeMarksInternalSecondChargeDue() {
        var controller = controller("sovereign-charge");
        controller.select(
                context(8.0, false, false, false, false, 0.40), 0
        );

        var first = controller.select(
                context(8.0, false, false, true, false, 0.40), 30
        );
        assertFalse(first.sovereignSecondChargeDue());

        var second = controller.select(
                context(8.0, false, false, true, false, 0.40), 170
        );
        assertTrue(second.sovereignSecondChargeDue());
        assertEquals(2, controller.sovereignCrownChargeCounter());
    }

    private static R01RegalhartActionController.Context context(
            double distance,
            boolean rear,
            boolean sweepFollowUpArc,
            boolean charge,
            boolean bound,
            double health
    ) {
        return new R01RegalhartActionController.Context(
                distance,
                rear,
                sweepFollowUpArc,
                charge,
                bound,
                health
        );
    }

    private static R01RegalhartActionController controller(String id) {
        return new R01RegalhartActionController(
                R01RegalhartEncounterDataLoader.load(),
                id,
                ACTOR
        );
    }
}
