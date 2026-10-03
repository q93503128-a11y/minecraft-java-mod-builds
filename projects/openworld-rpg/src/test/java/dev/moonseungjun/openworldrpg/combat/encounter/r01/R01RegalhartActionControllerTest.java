package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01RegalhartActionControllerTest {
    private static final UUID ACTOR =
            UUID.fromString("10000000-2000-3000-4000-500000000000");

    @Test
    void bundledRegalhartSelectionAndPhaseDataMatchCanon() {
        var data = R01RegalhartEncounterDataLoader.load();
        var rules = data.rulesById();

        assertEquals("threateningly_mobs:the_regalhart", data.entityId());
        assertEquals(8, data.contentLevel());
        assertEquals(1.25, data.weakPointMultiplier(), 0.000001);

        var sweep = rules.get(R01RegalhartEncounterData.ActionId.ANTLER_SWEEP);
        assertEquals(9, sweep.tellTicks());
        assertEquals(8, sweep.recoveryTicks());
        assertEquals(0.11, sweep.benchmarkDamageShare(), 0.000001);
        assertTrue(sweep.impactContractClosed());

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
        assertTrue(bound.impactContractClosed());

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
        var controller = controller("rear");
        var decision = controller.select(
                new R01RegalhartActionController.Context(
                        3.0,
                        true,
                        true,
                        true,
                        1.0
                ),
                0
        );
        assertEquals(
                R01RegalhartEncounterData.ActionId.REAR_KICK,
                decision.action().orElseThrow()
        );

        var closeFront = controller("front").select(
                new R01RegalhartActionController.Context(
                        3.0,
                        false,
                        true,
                        true,
                        1.0
                ),
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
                new R01RegalhartActionController.Context(
                        4.5,
                        false,
                        true,
                        true,
                        1.0
                ),
                0
        );
        assertEquals(
                R01RegalhartEncounterData.ActionId.ANTLER_SWEEP,
                decision.action().orElseThrow()
        );
    }

    @Test
    void weightedDistanceBandsAreDeterministicAndUseFartherSharedEndpoint() {
        assertArrayEquals(
                new int[] {45, 55},
                R01RegalhartActionController.distanceWeights(6.999)
        );
        assertArrayEquals(
                new int[] {60, 40},
                R01RegalhartActionController.distanceWeights(7.0)
        );
        assertArrayEquals(
                new int[] {75, 25},
                R01RegalhartActionController.distanceWeights(12.0)
        );
        assertArrayEquals(
                new int[] {75, 25},
                R01RegalhartActionController.distanceWeights(16.0)
        );

        var a = controller("weighted");
        var b = controller("weighted");
        assertEquals(
                a.select(midContext(9.0), 0).action(),
                b.select(midContext(9.0), 0).action()
        );
    }

    @Test
    void coolingOrIllegalMidFarActionsAreRemovedAndNoLegalActionRepositions() {
        var controller = controller("availability");
        var first = controller.select(
                new R01RegalhartActionController.Context(
                        10.0,
                        false,
                        true,
                        false,
                        1.0
                ),
                0
        );
        assertEquals(
                R01RegalhartEncounterData.ActionId.CROWN_CHARGE,
                first.action().orElseThrow()
        );

        var boundOnly = controller.select(
                new R01RegalhartActionController.Context(
                        10.0,
                        false,
                        true,
                        true,
                        1.0
                ),
                1
        );
        assertEquals(
                R01RegalhartEncounterData.ActionId.ROYAL_BOUND,
                boundOnly.action().orElseThrow()
        );

        assertTrue(controller.select(
                new R01RegalhartActionController.Context(
                        10.0,
                        false,
                        false,
                        false,
                        1.0
                ),
                2
        ).reposition());
        assertTrue(controller.select(
                new R01RegalhartActionController.Context(
                        16.01,
                        false,
                        true,
                        true,
                        1.0
                ),
                3
        ).reposition());
    }

    @Test
    void sovereignTransitionOccursOnceAtFortyPercentAndChangesSweepCadence() {
        var controller = controller("sovereign");

        var transition = controller.select(
                new R01RegalhartActionController.Context(
                        3.0,
                        false,
                        true,
                        true,
                        0.40
                ),
                0
        );
        assertTrue(transition.sovereignTransition());
        assertTrue(controller.sovereignEntered());

        var firstSweep = controller.select(closeFrontContext(0.40), 30);
        assertFalse(firstSweep.mirroredSweepFollowUp());
        var secondSweep = controller.select(closeFrontContext(0.40), 47);
        assertTrue(secondSweep.mirroredSweepFollowUp());

        var thirdSweep = controller.select(closeFrontContext(0.40), 64);
        assertFalse(thirdSweep.mirroredSweepFollowUp());
        assertEquals(3, controller.antlerSweepCounter());
    }

    @Test
    void normalSweepFollowUpIsEveryThirdAndSkippedGeometryStillAdvancesCounter() {
        var controller = controller("normal-sweep");

        assertFalse(controller.select(closeFrontContext(1.0), 0)
                .mirroredSweepFollowUp());
        assertFalse(controller.select(closeFrontContext(1.0), 17)
                .mirroredSweepFollowUp());
        assertTrue(controller.select(closeFrontContext(1.0), 34)
                .mirroredSweepFollowUp());
        assertEquals(3, controller.antlerSweepCounter());
    }

    @Test
    void everySecondSovereignCrownChargeAttemptsInternalSecondCharge() {
        var controller = controller("sovereign-charge");
        controller.select(
                new R01RegalhartActionController.Context(
                        8.0,
                        false,
                        false,
                        false,
                        0.40
                ),
                0
        );

        var first = controller.select(
                new R01RegalhartActionController.Context(
                        8.0,
                        false,
                        true,
                        false,
                        0.40
                ),
                30
        );
        assertFalse(first.sovereignSecondChargeAttempt());

        var second = controller.select(
                new R01RegalhartActionController.Context(
                        8.0,
                        false,
                        true,
                        false,
                        0.40
                ),
                170
        );
        assertTrue(second.sovereignSecondChargeAttempt());
        assertEquals(2, controller.sovereignCrownChargeCounter());
    }

    private static R01RegalhartActionController.Context midContext(double distance) {
        return new R01RegalhartActionController.Context(
                distance,
                false,
                true,
                true,
                1.0
        );
    }

    private static R01RegalhartActionController.Context closeFrontContext(double health) {
        return new R01RegalhartActionController.Context(
                4.0,
                false,
                true,
                true,
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
