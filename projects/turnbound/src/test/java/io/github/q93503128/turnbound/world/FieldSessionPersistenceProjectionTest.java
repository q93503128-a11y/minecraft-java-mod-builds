package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.SouthgateEncounterCatalog;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldSessionPersistenceProjectionTest {
    @Test
    void relogProjectionRestoresOnlyStarterFieldClears() {
        Set<String> projected = StarterFieldProgress.project(Set.of(
                SouthgateEncounterCatalog.ENC_M01,
                SouthgateEncounterCatalog.ENC_M02,
                "BATTLE_B01",
                "ENC_RIFT_01"));

        assertEquals(Set.of(SouthgateEncounterCatalog.ENC_M01, SouthgateEncounterCatalog.ENC_M02), projected);
    }

    @Test
    void freshProfileStartsWithNoDefeatedStarterPatrols() {
        assertTrue(StarterFieldProgress.project(Set.of()).isEmpty());
        assertTrue(StarterFieldProgress.project(null).isEmpty());
    }
}
