package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01SteelboarActionControllerTest {
    private static final UUID ACTOR =
            UUID.fromString("0f0f0f0f-1111-2222-3333-444444444444");

    @Test
    void bundledSteelboarSelectionContractMatchesCanonWithoutInventingRushGuardability() {
        var data = R01SteelboarEncounterDataLoader.load();
        var rules = data.rulesById();

        assertEquals("threateningly_mobs:steelboar", data.entityId());
        assertEquals(6, data.contentLevel());
        assertEquals(2, data.decisionDelayTicks());

        var tusk = rules.get(R01SteelboarEncounterData.ActionId.IRON_TUSK);
        assertEquals(8, tusk.tellTicks());
        assertEquals(7, tusk.recoveryTicks());
        assertEquals(0.13, tusk.benchmarkDamageShare(), 0.000001);
        assertEquals(28.0, tusk.playerPoisePressure(), 0.000001);
        assertTrue(tusk.impactContractClosed());

        var hook = rules.get(R01SteelboarEncounterData.ActionId.SHOULDER_HOOK);
        assertEquals(60, hook.cooldownTicks());
        assertEquals(12, hook.tellTicks());
        assertEquals(14, hook.recoveryTicks());
        assertEquals(45.0, hook.playerPoisePressure(), 0.000001);

        var rush = rules.get(R01SteelboarEncounterData.ActionId.IRON_RUSH);
        assertEquals(140, rush.cooldownTicks());
        assertEquals(17, rush.tellTicks());
        assertEquals(24, rush.recoveryTicks());
        assertEquals(5.5, rush.minimumRange(), 0.000001);
        assertEquals(12.0, rush.maximumRange(), 0.000001);
        assertEquals(12.0, rush.committedMovementBlocks(), 0.000001);
        assertEquals(70.0, rush.playerPoisePressure(), 0.000001);
        assertEquals(1.40, rush.perfectGuardPoiseMultiplier(), 0.000001);
        assertEquals(18.0, rush.obstacleSelfPoiseDamage(), 0.000001);
        assertNull(rush.guardable());
        assertFalse(rush.impactContractClosed());

        var furious = data.furiousRoute();
        assertEquals(0.35, furious.healthFractionExclusive(), 0.000001);
        assertEquals(280, furious.cooldownTicks());
        assertEquals(6.0, furious.minimumRange(), 0.000001);
        assertEquals(12, furious.minimumSecondChargePivotTellTicks());
        assertEquals(28, furious.recoveryAfterSecondChargeTicks());
    }

    @Test
    void ironRushOwnsClearLineMidFarPriorityAndUsesExactCooldown() {
        var controller = controller("rush-priority");

        var rush = controller.select(
                new R01SteelboarActionController.Context(
                        5.5,
                        true,
                        1.0
                ),
                100
        );
        assertEquals(
                R01SteelboarEncounterData.ActionId.IRON_RUSH,
                rush.action().orElseThrow()
        );
        assertFalse(rush.furiousRoute());
        assertEquals(140L, controller.cooldownRemainingTicks(
                R01SteelboarEncounterData.ActionId.IRON_RUSH,
                100
        ));

        var cooling = controller.select(
                new R01SteelboarActionController.Context(
                        7.0,
                        true,
                        1.0
                ),
                239
        );
        assertTrue(cooling.reposition());

        var ready = controller.select(
                new R01SteelboarActionController.Context(
                        12.0,
                        true,
                        1.0
                ),
                240
        );
        assertEquals(
                R01SteelboarEncounterData.ActionId.IRON_RUSH,
                ready.action().orElseThrow()
        );

        var blockedLine = controller("blocked-line").select(
                new R01SteelboarActionController.Context(
                        8.0,
                        false,
                        1.0
                ),
                0
        );
        assertTrue(blockedLine.reposition());
    }

    @Test
    void furiousRouteDeterministicallyReplacesNextLegalRushOnlyBelowThirtyFivePercent() {
        var controller = controller("furious-route");

        var atThreshold = controller.select(
                new R01SteelboarActionController.Context(
                        6.0,
                        true,
                        0.35
                ),
                0
        );
        assertEquals(
                R01SteelboarEncounterData.ActionId.IRON_RUSH,
                atThreshold.action().orElseThrow()
        );
        assertFalse(atThreshold.furiousRoute());

        var below = controller.select(
                new R01SteelboarActionController.Context(
                        6.0,
                        true,
                        0.34
                ),
                140
        );
        assertTrue(below.furiousRoute());
        assertEquals(280L, controller.furiousRouteCooldownRemainingTicks(140));
        assertEquals(140L, controller.cooldownRemainingTicks(
                R01SteelboarEncounterData.ActionId.IRON_RUSH,
                140
        ));

        var routeCooling = controller.select(
                new R01SteelboarActionController.Context(
                        6.0,
                        true,
                        0.20
                ),
                280
        );
        assertEquals(
                R01SteelboarEncounterData.ActionId.IRON_RUSH,
                routeCooling.action().orElseThrow()
        );
        assertFalse(routeCooling.furiousRoute());
    }

    @Test
    void furiousRouteDoesNotStealTheFivePointFiveToSixBlockRushWindow() {
        var controller = controller("furious-narrow-gap");
        var decision = controller.select(
                new R01SteelboarActionController.Context(
                        5.75,
                        true,
                        0.10
                ),
                0
        );
        assertEquals(
                R01SteelboarEncounterData.ActionId.IRON_RUSH,
                decision.action().orElseThrow()
        );
        assertFalse(decision.furiousRoute());
    }

    @Test
    void closeSelectionIsDeterministicAndGeneralTwoRepeatCapIsEnforced() {
        R01SteelboarActionController found = null;
        for (int i = 0; i < 2048; i++) {
            var candidate = controller("repeat-seed-" + i);
            var first = candidate.select(closeContext(), 0);
            var second = candidate.select(closeContext(), 60);
            if (first.action().orElseThrow()
                            == R01SteelboarEncounterData.ActionId.IRON_TUSK
                    && second.action().orElseThrow()
                            == R01SteelboarEncounterData.ActionId.IRON_TUSK) {
                found = candidate;
                break;
            }
        }
        assertTrue(found != null, "Expected a deterministic seed with two initial Iron Tusks.");

        var third = found.select(closeContext(), 120);
        assertEquals(
                R01SteelboarEncounterData.ActionId.SHOULDER_HOOK,
                third.action().orElseThrow()
        );

        var a = controller("deterministic-close");
        var b = controller("deterministic-close");
        assertEquals(
                a.select(closeContext(), 0).action(),
                b.select(closeContext(), 0).action()
        );
    }

    @Test
    void threeToFivePointFiveGapRepositionsInsteadOfInventingAChargeOrMeleeReach() {
        var controller = controller("gap");
        assertTrue(controller.select(
                new R01SteelboarActionController.Context(
                        3.01,
                        true,
                        1.0
                ),
                0
        ).reposition());
        assertTrue(controller.select(
                new R01SteelboarActionController.Context(
                        5.49,
                        true,
                        1.0
                ),
                1
        ).reposition());
    }

    private static R01SteelboarActionController.Context closeContext() {
        return new R01SteelboarActionController.Context(
                2.5,
                true,
                1.0
        );
    }

    private static R01SteelboarActionController controller(String id) {
        return new R01SteelboarActionController(
                R01SteelboarEncounterDataLoader.load(),
                id,
                ACTOR
        );
    }
}
