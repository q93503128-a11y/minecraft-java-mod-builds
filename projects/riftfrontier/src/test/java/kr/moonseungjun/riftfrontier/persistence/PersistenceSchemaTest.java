package kr.moonseungjun.riftfrontier.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceSchemaTest {
    @Test
    void currentSchemaIsStableAndAccepted() {
        assertEquals(3, PersistenceSchema.CURRENT);
        assertEquals("riftfrontier_schema_version", PersistenceSchema.VERSION_KEY);
        assertDoesNotThrow(() -> PersistenceSchema.requireSupported(PersistenceSchema.CURRENT));
    }

    @Test
    void unknownSchemaIsRejectedInsteadOfSilentlyRead() {
        assertThrows(IllegalArgumentException.class, () -> PersistenceSchema.requireSupported(0));
        assertThrows(IllegalArgumentException.class, () -> PersistenceSchema.requireSupported(1));
        assertThrows(IllegalArgumentException.class, () -> PersistenceSchema.requireSupported(2));
        assertThrows(IllegalArgumentException.class, () -> PersistenceSchema.requireSupported(4));
    }
}
