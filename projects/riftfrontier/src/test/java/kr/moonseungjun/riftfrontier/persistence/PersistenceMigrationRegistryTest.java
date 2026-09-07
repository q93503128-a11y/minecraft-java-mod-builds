package kr.moonseungjun.riftfrontier.persistence;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceMigrationRegistryTest {
    @Test
    void legacySchemaZeroMigratesToCurrentAndStampsVersion() {
        var migrated = PersistenceMigrationRegistry.defaults().migrate(0, Map.of("marker", "legacy"));
        assertEquals("legacy", migrated.get("marker"));
        assertEquals(PersistenceSchema.CURRENT, migrated.get(PersistenceSchema.VERSION_KEY));
        assertEquals(List.of(), migrated.get("expeditions"));
        assertEquals(0, migrated.get("secured_region_01_salvage"));
        assertEquals(2, migrated.get("expedition_supply"));
        assertEquals(0, migrated.get("region_01_pressure"));
    }

    @Test
    void schemaOneExplicitlyAddsExpeditionAndHubEconomyDomains() {
        var migrated = PersistenceMigrationRegistry.defaults().migrate(
            1,
            Map.of(PersistenceSchema.VERSION_KEY, 1, "world_revision", 4L)
        );
        assertEquals(PersistenceSchema.CURRENT, migrated.get(PersistenceSchema.VERSION_KEY));
        assertEquals(4L, migrated.get("world_revision"));
        assertEquals(List.of(), migrated.get("expeditions"));
        assertEquals(2, migrated.get("expedition_supply"));
    }

    @Test
    void schemaTwoAddsFirstVerticalSliceHubStateWithoutLosingExpeditions() {
        var expeditions = List.of("preserved-marker");
        var migrated = PersistenceMigrationRegistry.defaults().migrate(
            2,
            Map.of(PersistenceSchema.VERSION_KEY, 2, "expeditions", expeditions)
        );
        assertEquals(PersistenceSchema.CURRENT, migrated.get(PersistenceSchema.VERSION_KEY));
        assertEquals(expeditions, migrated.get("expeditions"));
        assertEquals(0, migrated.get("secured_region_01_salvage"));
        assertEquals(2, migrated.get("expedition_supply"));
        assertEquals(0, migrated.get("region_01_pressure"));
    }

    @Test
    void currentSchemaPassesThroughWithoutMutationLoss() {
        var migrated = PersistenceMigrationRegistry.defaults().migrate(
            PersistenceSchema.CURRENT,
            Map.of(PersistenceSchema.VERSION_KEY, PersistenceSchema.CURRENT, "marker", "current")
        );
        assertEquals("current", migrated.get("marker"));
        assertEquals(PersistenceSchema.CURRENT, migrated.get(PersistenceSchema.VERSION_KEY));
    }

    @Test
    void futureSchemaIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> PersistenceMigrationRegistry.defaults().migrate(PersistenceSchema.CURRENT + 1, Map.of())
        );
    }

    @Test
    void missingMigrationStepFailsLoudly() {
        var registry = new PersistenceMigrationRegistry();
        var error = assertThrows(IllegalStateException.class, () -> registry.migrate(0, Map.of()));
        assertTrue(error.getMessage().contains("Missing persistence migration"));
    }
}
