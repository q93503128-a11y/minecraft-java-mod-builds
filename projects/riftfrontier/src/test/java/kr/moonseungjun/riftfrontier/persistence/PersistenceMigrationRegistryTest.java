package kr.moonseungjun.riftfrontier.persistence;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceMigrationRegistryTest {
    @Test
    void legacySchemaZeroMigratesToCurrentAndStampsVersion() {
        var migrated = PersistenceMigrationRegistry.defaults().migrate(0, Map.of("marker", "legacy"));
        assertEquals("legacy", migrated.get("marker"));
        assertEquals(PersistenceSchema.CURRENT, migrated.get(PersistenceSchema.VERSION_KEY));
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
