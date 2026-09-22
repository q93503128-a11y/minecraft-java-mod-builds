package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongActionController;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongEncounterData;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongEncounterDataLoader;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongSpatialAuthority;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01EarthloongActionControllerTest {
    private static final UUID ACTOR =
            UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    void bundledDataMatchesLockedR01ControllerRules() {
        var data = R01EarthloongEncounterDataLoader.loadBundled();
        var rules = data.rulesById();

        assertEquals(8, data.contentLevel());
        assertEquals(2, data.decisionDelayTicks());
        assertEquals(0.55, data.phaseTwoHealthThreshold(), 0.000001);
        assertEquals(7, rules.size());

        assertEquals(50, rules.get(R01EarthloongEncounterData.ActionId.CLAW_SWEEP).weight());
        assertEquals(55, rules.get(R01EarthloongEncounterData.ActionId.TAIL_SCYTHE).weight());
        assertEquals(140, rules.get(R01EarthloongEncounterData.ActionId.QUARRY_RUSH).cooldownTicks());
        assertEquals(160, rules.get(R01EarthloongEncounterData.ActionId.LIGHTNING_FURROW).cooldownTicks());
        assertEquals(180, rules.get(R01EarthloongEncounterData.ActionId.ROOT_BREAKER).cooldownTicks());
        assertEquals(200, rules.get(R01EarthloongEncounterData.ActionId.FORKED_HEAVEN).cooldownTicks());
        assertEquals(140, rules.get(R01EarthloongEncounterData.ActionId.EARTHLINE_SURGE).cooldownTicks());
    }

    @Test
    void bundledPhaseOnePhysicalImpactRulesMatchCanon() {
        var data = R01EarthloongEncounterDataLoader.loadBundled();
        var impacts = data.impactRulesById();

        assertEquals(3, impacts.size());

        var claw = impacts.get(R01EarthloongEncounterData.ActionId.CLAW_SWEEP);
        var tail = impacts.get(R01EarthloongEncounterData.ActionId.TAIL_SCYTHE);
        var rush = impacts.get(R01EarthloongEncounterData.ActionId.QUARRY_RUSH);

        assertEquals(0.10, claw.benchmarkDamageShare(), 0.000001);
        assertEquals(0.20, tail.benchmarkDamageShare(), 0.000001);
        assertEquals(0.24, rush.benchmarkDamageShare(), 0.000001);

        assertTrue(claw.guardable());
        assertTrue(claw.perfectGuardable());
        assertTrue(tail.guardable());
        assertTrue(tail.perfectGuardable());
        assertFalse(rush.guardable());
        assertTrue(rush.perfectGuardable());

        assertEquals(19.0666666667, claw.toIncomingHit(8).rawDamage(), 0.000001);
        assertEquals(38.1333333333, tail.toIncomingHit(8).rawDamage(), 0.000001);
        assertEquals(45.76, rush.toIncomingHit(8).rawDamage(), 0.000001);
    }

    @Test
    void phaseOnePhysicalSpatialBindingsMatchLockedRangesAnglesAndPresentationCandidates() {
        var data = R01EarthloongEncounterDataLoader.loadBundled();
        var bindings = data.physicalBindingsById();

        assertEquals(3, bindings.size());

        var claw = bindings.get(R01EarthloongEncounterData.ActionId.CLAW_SWEEP);
        assertEquals(9, claw.tellTicks());
        assertEquals(3.5, claw.maximumRange(), 0.000001);
        assertEquals(120.0, claw.maximumAbsoluteAngleDegrees(), 0.000001);
        assertEquals(1, claw.donorSkillNumber());
        assertEquals(10, claw.donorAnimationTicks());
        assertTrue(claw.hasDonorPresentationCandidate());

        var tail = bindings.get(R01EarthloongEncounterData.ActionId.TAIL_SCYTHE);
        assertEquals(13, tail.tellTicks());
        assertEquals(13, tail.recoveryTicks());
        assertEquals(60.0, tail.minimumAbsoluteAngleDegrees(), 0.000001);
        assertEquals(180.0, tail.maximumAbsoluteAngleDegrees(), 0.000001);
        assertFalse(tail.hasDonorPresentationCandidate());

        var rush = bindings.get(R01EarthloongEncounterData.ActionId.QUARRY_RUSH);
        assertEquals(16, rush.tellTicks());
        assertEquals(18, rush.recoveryTicks());
        assertEquals(5.0, rush.minimumRange(), 0.000001);
        assertEquals(9.0, rush.maximumRange(), 0.000001);
        assertTrue(rush.requiresClearLine());
        assertEquals(9.0, rush.forwardPathBlocks(), 0.000001);
        assertEquals(2, rush.donorSkillNumber());
        assertEquals(40, rush.donorAnimationTicks());
        assertTrue(rush.hasDonorPresentationCandidate());
    }

    @Test
    void phaseOneSpatialAuthorityKeepsFlankOverlapAndClearLineRushGate() {
        var bindings = R01EarthloongEncounterDataLoader.loadBundled().physicalBindingsById();
        var claw = bindings.get(R01EarthloongEncounterData.ActionId.CLAW_SWEEP);
        var tail = bindings.get(R01EarthloongEncounterData.ActionId.TAIL_SCYTHE);
        var rush = bindings.get(R01EarthloongEncounterData.ActionId.QUARRY_RUSH);

        var front = R01EarthloongSpatialAuthority.evaluate(
                0.0, 0.0, 0.0, 1.0, 0.0, 3.0, true
        );
        assertTrue(R01EarthloongSpatialAuthority.isLegal(claw, front));
        assertFalse(R01EarthloongSpatialAuthority.isLegal(tail, front));

        var flank = R01EarthloongSpatialAuthority.evaluate(
                0.0, 0.0, 0.0, 1.0, 3.0, 0.0, true
        );
        assertTrue(R01EarthloongSpatialAuthority.isLegal(claw, flank));
        assertTrue(R01EarthloongSpatialAuthority.isLegal(tail, flank));

        var rear = R01EarthloongSpatialAuthority.evaluate(
                0.0, 0.0, 0.0, 1.0, 0.0, -4.0, true
        );
        assertFalse(R01EarthloongSpatialAuthority.isLegal(claw, rear));
        assertTrue(R01EarthloongSpatialAuthority.isLegal(tail, rear));

        var rushClear = R01EarthloongSpatialAuthority.evaluate(
                0.0, 0.0, 1.0, 0.0, -6.0, 0.0, true
        );
        assertTrue(R01EarthloongSpatialAuthority.isLegal(rush, rushClear));

        var rushBlocked = new R01EarthloongSpatialAuthority.SpatialSnapshot(
                rushClear.horizontalDistance(),
                rushClear.absoluteAngleDegrees(),
                false
        );
        assertFalse(R01EarthloongSpatialAuthority.isLegal(rush, rushBlocked));
    }

    @Test
    void benchmarkDamageAuthoringUsesLockedSameLevelHealthAnchors() {
        assertEquals(100, dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules
                .benchmarkPlayerHealth(1));
        assertEquals(143, dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules
                .benchmarkPlayerHealth(8));
        assertEquals(223, dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules
                .benchmarkPlayerHealth(20));
        assertEquals(403, dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules
                .benchmarkPlayerHealth(44));
        assertEquals(575, dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules
                .benchmarkPlayerHealth(64));
        assertEquals(727, dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules
                .benchmarkPlayerHealth(80));
    }

    @Test
    void sameEncounterActorAndCounterProduceSameWeightedDecision() {
        var data = R01EarthloongEncounterDataLoader.loadBundled();
        var legality = new R01EarthloongActionController.Legality(
                true, true, true, true, true, false, false
        );

        var a = new R01EarthloongActionController(data, "run-42", ACTOR);
        var b = new R01EarthloongActionController(data, "run-42", ACTOR);

        assertEquals(
                a.select(R01EarthloongEncounterData.Phase.ONE, legality, 100).action(),
                b.select(R01EarthloongEncounterData.Phase.ONE, legality, 100).action()
        );
    }

    @Test
    void phaseTwoActionsNeverLeakIntoPhaseOne() {
        var controller = new R01EarthloongActionController(
                R01EarthloongEncounterDataLoader.loadBundled(),
                "phase-gate",
                ACTOR
        );

        var decision = controller.select(
                R01EarthloongEncounterData.Phase.ONE,
                new R01EarthloongActionController.Legality(
                        false, false, false, false, false, true, true
                ),
                0
        );

        assertTrue(decision.reposition());
        assertTrue(decision.action().isEmpty());
        assertEquals(0L, controller.actionCounter());
    }

    @Test
    void cooldownRemovesCommittedActionUntilExactEndTick() {
        var controller = new R01EarthloongActionController(
                R01EarthloongEncounterDataLoader.loadBundled(),
                "cooldown",
                ACTOR
        );
        var onlyQuarry = R01EarthloongActionController.Legality.only(
                R01EarthloongEncounterData.ActionId.QUARRY_RUSH
        );

        var first = controller.select(R01EarthloongEncounterData.Phase.ONE, onlyQuarry, 10);
        assertEquals(
                R01EarthloongEncounterData.ActionId.QUARRY_RUSH,
                first.action().orElseThrow()
        );
        assertEquals(140L, controller.cooldownRemainingTicks(
                R01EarthloongEncounterData.ActionId.QUARRY_RUSH,
                10
        ));

        var blocked = controller.select(R01EarthloongEncounterData.Phase.ONE, onlyQuarry, 149);
        assertTrue(blocked.reposition());
        assertEquals(1L, controller.actionCounter());

        var ready = controller.select(R01EarthloongEncounterData.Phase.ONE, onlyQuarry, 150);
        assertEquals(
                R01EarthloongEncounterData.ActionId.QUARRY_RUSH,
                ready.action().orElseThrow()
        );
    }

    @Test
    void thirdConsecutiveSpaceControlIsForbiddenAndForcesPhysicalOrReposition() {
        var controller = new R01EarthloongActionController(
                R01EarthloongEncounterDataLoader.loadBundled(),
                "anti-spam",
                ACTOR
        );
        var onlyFurrow = R01EarthloongActionController.Legality.only(
                R01EarthloongEncounterData.ActionId.LIGHTNING_FURROW
        );

        controller.select(R01EarthloongEncounterData.Phase.ONE, onlyFurrow, 0);
        controller.select(R01EarthloongEncounterData.Phase.ONE, onlyFurrow, 160);
        assertEquals(2, controller.consecutiveSpaceControlActions());

        var forcedReposition =
                controller.select(R01EarthloongEncounterData.Phase.ONE, onlyFurrow, 320);
        assertTrue(forcedReposition.reposition());
        assertEquals(2L, controller.actionCounter());

        var physicalAvailable = new R01EarthloongActionController.Legality(
                true, false, false, true, true, false, false
        );
        var forcedPhysical =
                controller.select(R01EarthloongEncounterData.Phase.ONE, physicalAvailable, 320);
        assertEquals(
                R01EarthloongEncounterData.ActionId.CLAW_SWEEP,
                forcedPhysical.action().orElseThrow()
        );
        assertEquals(0, controller.consecutiveSpaceControlActions());
    }

    @Test
    void phaseTwoFurrowStartsAtFourThenAlternatesThreeFourWithoutPhaseOneReset() {
        var controller = new R01EarthloongActionController(
                R01EarthloongEncounterDataLoader.loadBundled(),
                "furrow-pattern",
                ACTOR
        );
        var furrow = R01EarthloongActionController.Legality.only(
                R01EarthloongEncounterData.ActionId.LIGHTNING_FURROW
        );
        var claw = R01EarthloongActionController.Legality.only(
                R01EarthloongEncounterData.ActionId.CLAW_SWEEP
        );

        var phaseOne = controller.select(R01EarthloongEncounterData.Phase.ONE, furrow, 0);
        assertEquals(3, phaseOne.lightningFurrowLaneCount().orElseThrow());

        controller.select(R01EarthloongEncounterData.Phase.ONE, claw, 1);

        var firstPhaseTwo = controller.select(R01EarthloongEncounterData.Phase.TWO, furrow, 160);
        assertEquals(4, firstPhaseTwo.lightningFurrowLaneCount().orElseThrow());

        controller.select(R01EarthloongEncounterData.Phase.TWO, claw, 161);

        var secondPhaseTwo = controller.select(R01EarthloongEncounterData.Phase.TWO, furrow, 320);
        assertEquals(3, secondPhaseTwo.lightningFurrowLaneCount().orElseThrow());

        controller.select(R01EarthloongEncounterData.Phase.TWO, claw, 321);

        var thirdPhaseTwo = controller.select(R01EarthloongEncounterData.Phase.TWO, furrow, 480);
        assertEquals(4, thirdPhaseTwo.lightningFurrowLaneCount().orElseThrow());
        assertEquals(3, controller.phaseTwoFurrowCastCount());
    }

    @Test
    void phaseTransitionDoesNotResetExistingCooldowns() {
        var controller = new R01EarthloongActionController(
                R01EarthloongEncounterDataLoader.loadBundled(),
                "phase-cooldown",
                ACTOR
        );
        var quarry = R01EarthloongActionController.Legality.only(
                R01EarthloongEncounterData.ActionId.QUARRY_RUSH
        );

        controller.select(R01EarthloongEncounterData.Phase.ONE, quarry, 0);
        var phaseTwoStillCooling =
                controller.select(R01EarthloongEncounterData.Phase.TWO, quarry, 100);

        assertTrue(phaseTwoStillCooling.reposition());
        assertEquals(40L, controller.cooldownRemainingTicks(
                R01EarthloongEncounterData.ActionId.QUARRY_RUSH,
                100
        ));
    }

    @Test
    void ordinaryPhysicalAttackCannotCommitThreeTimesWhenAnotherLegalAttackExists() {
        var controller = new R01EarthloongActionController(
                R01EarthloongEncounterDataLoader.loadBundled(), "ordinary-repeat", ACTOR);
        var claw = R01EarthloongActionController.Legality.only(
                R01EarthloongEncounterData.ActionId.CLAW_SWEEP);
        controller.select(R01EarthloongEncounterData.Phase.ONE, claw, 0);
        controller.select(R01EarthloongEncounterData.Phase.ONE, claw, 1);
        assertEquals(2, controller.consecutiveSameActionCount());

        var clawAndTail = new R01EarthloongActionController.Legality(
                true, true, false, false, false, false, false);
        var third = controller.select(
                R01EarthloongEncounterData.Phase.ONE, clawAndTail, 2);
        assertEquals(
                R01EarthloongEncounterData.ActionId.TAIL_SCYTHE,
                third.action().orElseThrow());
    }

    @Test
    void signatureMovementCannotImmediatelyRepeatWhenAnotherLegalAttackExists() {
        var controller = new R01EarthloongActionController(
                R01EarthloongEncounterDataLoader.loadBundled(), "rush-repeat", ACTOR);
        controller.select(
                R01EarthloongEncounterData.Phase.ONE,
                R01EarthloongActionController.Legality.only(
                        R01EarthloongEncounterData.ActionId.QUARRY_RUSH),
                0);

        var next = controller.select(
                R01EarthloongEncounterData.Phase.ONE,
                new R01EarthloongActionController.Legality(
                        true, false, true, false, false, false, false),
                140);
        assertEquals(
                R01EarthloongEncounterData.ActionId.CLAW_SWEEP,
                next.action().orElseThrow());
    }

}
