package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldSharedEncounterRulesTest {
    @Test
    void regionClassificationCoversAuthoredFieldChapters() {
        assertEquals(FieldSharedEncounterRules.Region.GLOAMWOOD, FieldSharedEncounterRules.regionOf("ENC_G03"));
        assertEquals(FieldSharedEncounterRules.Region.GLOAMWOOD, FieldSharedEncounterRules.regionOf("BATTLE_B02"));
        assertEquals(FieldSharedEncounterRules.Region.AQUEDUCT, FieldSharedEncounterRules.regionOf("ENC_A05"));
        assertEquals(FieldSharedEncounterRules.Region.QUARRY, FieldSharedEncounterRules.regionOf("BATTLE_B04"));
        assertEquals(FieldSharedEncounterRules.Region.RELAY, FieldSharedEncounterRules.regionOf("ENC_R01"));
        assertEquals(FieldSharedEncounterRules.Region.OTHER, FieldSharedEncounterRules.regionOf("ENC_M01"));
    }

    @Test
    void chapterUnlocksMatchSessionProgressionRules() {
        assertTrue(FieldSharedEncounterRules.unlocked("ENC_G01", Set.of(), Set.of()));
        assertFalse(FieldSharedEncounterRules.unlocked("ENC_G03", Set.of(), Set.of()));
        assertTrue(FieldSharedEncounterRules.unlocked("ENC_G03", Set.of("MQ_C02_01_spores"), Set.of()));
        assertTrue(FieldSharedEncounterRules.unlocked("BATTLE_B02", Set.of(), Set.of("B02_GATE")));

        assertTrue(FieldSharedEncounterRules.unlocked("ENC_A02", Set.of(), Set.of()));
        assertFalse(FieldSharedEncounterRules.unlocked("ENC_A04", Set.of(), Set.of()));
        assertTrue(FieldSharedEncounterRules.unlocked("ENC_A04", Set.of(), Set.of("AQUEDUCT_LOWER")));
        assertTrue(FieldSharedEncounterRules.unlocked("BATTLE_B03", Set.of("MQ_C03_02_old_orders"), Set.of()));

        assertTrue(FieldSharedEncounterRules.unlocked("ENC_Q01", Set.of(), Set.of()));
        assertFalse(FieldSharedEncounterRules.unlocked("ENC_Q05", Set.of(), Set.of()));
        assertTrue(FieldSharedEncounterRules.unlocked("ENC_Q05", Set.of("MQ_C04_01_ash_route"), Set.of()));
        assertTrue(FieldSharedEncounterRules.unlocked("BATTLE_B04", Set.of("MQ_C04_02_core_fragment"), Set.of()));

        assertTrue(FieldSharedEncounterRules.unlocked("ENC_R05", Set.of(), Set.of()));
        assertFalse(FieldSharedEncounterRules.unlocked("BATTLE_B05", Set.of(), Set.of()));
        assertTrue(FieldSharedEncounterRules.unlocked("BATTLE_B05", Set.of("MQ_C05_02_serak_record"), Set.of()));
    }
}
