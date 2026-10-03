package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import org.junit.jupiter.api.Test;

class R01NatureSpiritImpactAuthorityTest {
    @Test
    void onlyNatureSpiritActionsAreAcceptedByTheImpactAuthority() {
        assertTrue(R01NatureSpiritImpactAuthority.isNatureSpiritAction(
                R01SecondaryCreatureEncounterData.ActionId.ROOTED_SWIPE
        ));
        assertTrue(R01NatureSpiritImpactAuthority.isNatureSpiritAction(
                R01SecondaryCreatureEncounterData.ActionId.EARTHEN_RAM
        ));
        assertTrue(R01NatureSpiritImpactAuthority.isNatureSpiritAction(
                R01SecondaryCreatureEncounterData.ActionId.BLOOM_QUAKE
        ));

        assertFalse(R01NatureSpiritImpactAuthority.isNatureSpiritAction(
                R01SecondaryCreatureEncounterData.ActionId.SCUTTLE_BITE
        ));
        assertFalse(R01NatureSpiritImpactAuthority.isNatureSpiritAction(
                R01SecondaryCreatureEncounterData.ActionId.BODY_RAKE
        ));
        assertFalse(R01NatureSpiritImpactAuthority.isNatureSpiritAction(
                R01SecondaryCreatureEncounterData.ActionId.CEILING_DROP
        ));
    }

    @Test
    void rootedSwipeKeepsTheCanonicalMediumGuardedImpact() {
        var rule = R01NatureSpiritImpactAuthority.rule(
                R01SecondaryCreatureEncounterData.ActionId.ROOTED_SWIPE
        ).orElseThrow();
        var hit = rule.toIncomingHit(7);

        assertEquals(0.13, rule.benchmarkDamageShare(), 0.000001);
        assertEquals(9, rule.tellTicks());
        assertEquals(
                PlayerDefenseAuthority.GuardPressureBand.MEDIUM,
                hit.guardPressure().orElseThrow()
        );
        assertTrue(hit.guardable());
        assertTrue(hit.perfectGuardable());
        assertEquals(0.0, rule.playerPoisePressure(), 0.000001);
    }

    @Test
    void earthenRamKeepsCanonicalHeavyGuardAndPlayerPoisePressure() {
        var rule = R01NatureSpiritImpactAuthority.rule(
                R01SecondaryCreatureEncounterData.ActionId.EARTHEN_RAM
        ).orElseThrow();
        var hit = rule.toIncomingHit(7);

        assertEquals(0.22, rule.benchmarkDamageShare(), 0.000001);
        assertEquals(15, rule.tellTicks());
        assertEquals(17, rule.recoveryTicks());
        assertEquals(3.0, rule.committedMovementBlocks(), 0.000001);
        assertEquals(
                PlayerDefenseAuthority.GuardPressureBand.HEAVY,
                hit.guardPressure().orElseThrow()
        );
        assertTrue(hit.guardable());
        assertTrue(hit.perfectGuardable());
        assertEquals(45.0, rule.playerPoisePressure(), 0.000001);
    }

    @Test
    void bloomQuakeKeepsCanonicalUnguardableAreaImpact() {
        var rule = R01NatureSpiritImpactAuthority.rule(
                R01SecondaryCreatureEncounterData.ActionId.BLOOM_QUAKE
        ).orElseThrow();
        var hit = rule.toIncomingHit(7);

        assertEquals(0.25, rule.benchmarkDamageShare(), 0.000001);
        assertEquals(20, rule.tellTicks());
        assertEquals(18, rule.recoveryTicks());
        assertEquals(4.0, rule.areaRadius(), 0.000001);
        assertFalse(hit.guardable());
        assertFalse(hit.perfectGuardable());
        assertTrue(hit.guardPressure().isEmpty());
        assertEquals(45.0, rule.playerPoisePressure(), 0.000001);
    }
}
