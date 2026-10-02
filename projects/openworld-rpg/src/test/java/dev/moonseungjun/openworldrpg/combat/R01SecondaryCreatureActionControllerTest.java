package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01CaveCentipedeActionController;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01NatureSpiritActionController;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01SecondaryCreatureEncounterData;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01SecondaryCreatureEncounterDataLoader;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01SecondaryCreatureActionControllerTest {
    private static final UUID ACTOR =
            UUID.fromString("99999999-2222-3333-4444-555555555555");

    @Test
    void caveCentipedeBundledContractMatchesCanon() {
        var data = R01SecondaryCreatureEncounterDataLoader.loadCaveCentipede();
        var rules = data.rulesById();

        assertEquals(4, data.contentLevel());
        assertEquals("alexsmobs:centipede_head", data.entityId());
        assertEquals(3, rules.size());

        var bite = rules.get(R01SecondaryCreatureEncounterData.ActionId.SCUTTLE_BITE);
        assertEquals(60, bite.weight());
        assertEquals(7, bite.tellTicks());
        assertEquals(8, bite.recoveryTicks());
        assertEquals(0.11, bite.benchmarkDamageShare(), 0.000001);
        assertEquals(30.0, bite.poisonBuildup(), 0.000001);
        assertEquals(PlayerDefenseAuthority.GuardPressureBand.LIGHT, bite.guardPressure());

        var rake = rules.get(R01SecondaryCreatureEncounterData.ActionId.BODY_RAKE);
        assertEquals(5, rake.activeTicks());
        assertEquals(28.0, rake.playerPoisePressure(), 0.000001);

        var drop = rules.get(R01SecondaryCreatureEncounterData.ActionId.CEILING_DROP);
        assertEquals(160, drop.cooldownTicks());
        assertEquals(14, drop.tellTicks());
        assertEquals(15, drop.recoveryTicks());
        assertEquals(2.3, drop.areaRadius(), 0.000001);
        assertTrue(drop.requiresMeaningfullyAboveTarget());
        assertEquals(20.0, drop.poisonBuildup(), 0.000001);

        assertTrue(bite.toIncomingHit(4).guardable());
        assertTrue(drop.toIncomingHit(4).perfectGuardable());
    }

    @Test
    void caveCentipedeCeilingDropCooldownIsExactAndNeverTeleportsIntoLegality() {
        var controller = new R01CaveCentipedeActionController(
                R01SecondaryCreatureEncounterDataLoader.loadCaveCentipede(),
                "cave-room-1",
                ACTOR
        );
        var onlyDrop = R01CaveCentipedeActionController.Legality.only(
                R01SecondaryCreatureEncounterData.ActionId.CEILING_DROP
        );

        var first = controller.select(onlyDrop, 20);
        assertEquals(
                R01SecondaryCreatureEncounterData.ActionId.CEILING_DROP,
                first.action().orElseThrow()
        );
        assertEquals(160L, controller.cooldownRemainingTicks(
                R01SecondaryCreatureEncounterData.ActionId.CEILING_DROP,
                20
        ));

        var blocked = controller.select(onlyDrop, 179);
        assertTrue(blocked.reposition());
        assertEquals(1L, controller.actionCounter());

        var ready = controller.select(onlyDrop, 180);
        assertEquals(
                R01SecondaryCreatureEncounterData.ActionId.CEILING_DROP,
                ready.action().orElseThrow()
        );

        var noPhysicalAbovePosition = controller.select(
                new R01CaveCentipedeActionController.Legality(false, false, false),
                400
        );
        assertTrue(noPhysicalAbovePosition.reposition());
    }

    @Test
    void caveCentipedeWeightedSelectionIsDeterministicForSameEncounterActorAndCounter() {
        var data = R01SecondaryCreatureEncounterDataLoader.loadCaveCentipede();
        var legality = new R01CaveCentipedeActionController.Legality(true, true, true);
        var a = new R01CaveCentipedeActionController(data, "same-seed", ACTOR);
        var b = new R01CaveCentipedeActionController(data, "same-seed", ACTOR);

        assertEquals(a.select(legality, 0).action(), b.select(legality, 0).action());
        assertEquals(a.select(legality, 200).action(), b.select(legality, 200).action());
    }

    @Test
    void natureSpiritBundledContractMatchesCanon() {
        var data = R01SecondaryCreatureEncounterDataLoader.loadNatureSpirit();
        var rules = data.rulesById();

        assertEquals(7, data.contentLevel());
        assertEquals("threateningly_mobs:nature_hamony", data.entityId());
        assertEquals(3, rules.size());

        var swipe = rules.get(R01SecondaryCreatureEncounterData.ActionId.ROOTED_SWIPE);
        assertEquals(60, swipe.weight());
        assertEquals(9, swipe.tellTicks());
        assertEquals(0.13, swipe.benchmarkDamageShare(), 0.000001);

        var ram = rules.get(R01SecondaryCreatureEncounterData.ActionId.EARTHEN_RAM);
        assertEquals(80, ram.cooldownTicks());
        assertEquals(3.0, ram.committedMovementBlocks(), 0.000001);
        assertEquals(45.0, ram.playerPoisePressure(), 0.000001);

        var quake = rules.get(R01SecondaryCreatureEncounterData.ActionId.BLOOM_QUAKE);
        assertEquals(160, quake.cooldownTicks());
        assertEquals(20, quake.tellTicks());
        assertEquals(4.0, quake.areaRadius(), 0.000001);
        assertFalse(quake.guardable());
        assertFalse(quake.perfectGuardable());

        var shell = data.livingShell();
        assertEquals(50, shell.durationTicks());
        assertEquals(240, shell.reuseTicks());
        assertEquals(0.65, shell.directDamageTakenMultiplier(), 0.000001);
        assertEquals(1.25, shell.poiseDamageTakenMultiplier(), 0.000001);
        assertEquals(0.20, shell.recentDamageTriggerFraction(), 0.000001);
        assertEquals(80, shell.recentDamageWindowTicks());
        assertEquals(0.40, shell.poiseTriggerFraction(), 0.000001);
    }

    @Test
    void natureSpiritLivingShellTriggersFromExactDamageOrPoiseThresholds() {
        var data = R01SecondaryCreatureEncounterDataLoader.loadNatureSpirit();
        var damageTrigger = new R01NatureSpiritActionController(
                data, "damage-trigger", ACTOR
        );
        var enteredByDamage = damageTrigger.select(
                new R01NatureSpiritActionController.Context(
                        true, true, true, 3.0, 0.20, 1.0
                ),
                100
        );
        assertEquals(
                R01NatureSpiritActionController.Mode.ENTER_LIVING_SHELL,
                enteredByDamage.mode()
        );
        assertEquals(150L, enteredByDamage.livingShellEndTick());
        assertEquals(340L, enteredByDamage.livingShellReuseEndTick());
        assertEquals(0.65, damageTrigger.directDamageTakenMultiplier(120), 0.000001);
        assertEquals(1.25, damageTrigger.poiseDamageTakenMultiplier(120), 0.000001);

        var poiseTrigger = new R01NatureSpiritActionController(
                data, "poise-trigger", ACTOR
        );
        var enteredByPoise = poiseTrigger.select(
                new R01NatureSpiritActionController.Context(
                        true, true, true, 3.0, 0.0, 0.40
                ),
                200
        );
        assertEquals(
                R01NatureSpiritActionController.Mode.ENTER_LIVING_SHELL,
                enteredByPoise.mode()
        );
    }

    @Test
    void naturalLivingShellEndForcesBloomQuakeOnlyWhenTargetIsInRange() {
        var controller = new R01NatureSpiritActionController(
                R01SecondaryCreatureEncounterDataLoader.loadNatureSpirit(),
                "forced-quake",
                ACTOR
        );
        controller.select(
                new R01NatureSpiritActionController.Context(
                        true, true, true, 3.0, 0.20, 1.0
                ),
                0
        );

        var holding = controller.select(
                new R01NatureSpiritActionController.Context(
                        true, true, true, 3.0, 0.0, 1.0
                ),
                49
        );
        assertEquals(R01NatureSpiritActionController.Mode.HOLD_LIVING_SHELL, holding.mode());

        var forced = controller.select(
                new R01NatureSpiritActionController.Context(
                        true, true, true, 4.0, 0.0, 1.0
                ),
                50
        );
        assertEquals(R01NatureSpiritActionController.Mode.ATTACK, forced.mode());
        assertEquals(
                R01SecondaryCreatureEncounterData.ActionId.BLOOM_QUAKE,
                forced.action().orElseThrow()
        );
        assertTrue(forced.forcedAfterShell());
    }

    @Test
    void poiseBreakEndsLivingShellWithoutGrantingTheForcedQuake() {
        var controller = new R01NatureSpiritActionController(
                R01SecondaryCreatureEncounterDataLoader.loadNatureSpirit(),
                "shell-break",
                ACTOR
        );
        controller.select(
                new R01NatureSpiritActionController.Context(
                        true, true, true, 3.0, 0.20, 1.0
                ),
                0
        );
        assertTrue(controller.onPoiseBroken(20));
        assertEquals(1.0, controller.directDamageTakenMultiplier(20), 0.000001);

        var next = controller.select(
                new R01NatureSpiritActionController.Context(
                        true, false, true, 3.0, 0.0, 1.0
                ),
                21
        );
        assertFalse(next.forcedAfterShell());
    }

    @Test
    void bloomQuakeCannotBeTheImmediateNextActionAgain() {
        var controller = new R01NatureSpiritActionController(
                R01SecondaryCreatureEncounterDataLoader.loadNatureSpirit(),
                "quake-repeat",
                ACTOR
        );

        var first = controller.select(
                new R01NatureSpiritActionController.Context(
                        false, false, true, 3.0, 0.0, 1.0
                ),
                0
        );
        assertEquals(
                R01SecondaryCreatureEncounterData.ActionId.BLOOM_QUAKE,
                first.action().orElseThrow()
        );

        var afterCooldown = controller.select(
                new R01NatureSpiritActionController.Context(
                        false, false, true, 3.0, 0.0, 1.0
                ),
                160
        );
        assertEquals(R01NatureSpiritActionController.Mode.REPOSITION, afterCooldown.mode());

        var other = controller.select(
                new R01NatureSpiritActionController.Context(
                        true, false, true, 3.0, 0.0, 1.0
                ),
                161
        );
        assertEquals(
                R01SecondaryCreatureEncounterData.ActionId.ROOTED_SWIPE,
                other.action().orElseThrow()
        );
    }

    @Test
    void natureSpiritRamAndQuakeUseCanonicalIncomingPressureBands() {
        var rules = R01SecondaryCreatureEncounterDataLoader
                .loadNatureSpirit()
                .rulesById();

        var ram = rules.get(R01SecondaryCreatureEncounterData.ActionId.EARTHEN_RAM);
        var quake = rules.get(R01SecondaryCreatureEncounterData.ActionId.BLOOM_QUAKE);

        assertEquals(
                PlayerDefenseAuthority.GuardPressureBand.HEAVY,
                ram.toIncomingHit(7).guardPressure().orElseThrow()
        );
        assertTrue(quake.toIncomingHit(7).guardPressure().isEmpty());
        assertEquals(45.0, quake.playerPoisePressure(), 0.000001);
    }
}
