package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldSharedInteractionActorsTest {
    @Test
    void sharedRolesHaveStableUniqueTagsAndRoundTrip() {
        Set<String> tags = new HashSet<>();
        for (FieldSharedActorCatalog.Role role : FieldSharedActorCatalog.Role.values()) {
            String tag = FieldSharedActorCatalog.roleTag(role);
            assertTrue(tag.startsWith("turnbound_field_role:"));
            assertTrue(tags.add(tag), "duplicate shared field actor tag: " + tag);
            assertEquals(role, FieldSharedActorCatalog.fromTag(tag));
        }
        assertEquals(19, FieldSharedActorCatalog.Role.values().length);
        assertEquals(19, tags.size());
        assertNull(FieldSharedActorCatalog.fromTag("not_turnbound"));
    }

    @Test
    void orderedQuestActorsKeepCanonicalIndexes() {
        assertEquals(0, FieldSharedActorCatalog.gloamSporeIndex(FieldSharedActorCatalog.Role.GLOAM_SPORE_1));
        assertEquals(1, FieldSharedActorCatalog.gloamSporeIndex(FieldSharedActorCatalog.Role.GLOAM_SPORE_2));
        assertEquals(2, FieldSharedActorCatalog.gloamSporeIndex(FieldSharedActorCatalog.Role.GLOAM_SPORE_3));
        assertEquals(-1, FieldSharedActorCatalog.gloamSporeIndex(FieldSharedActorCatalog.Role.GLOAM_RELAY));

        assertEquals(0, FieldSharedActorCatalog.aqueductValveIndex(FieldSharedActorCatalog.Role.AQUEDUCT_VALVE_1));
        assertEquals(1, FieldSharedActorCatalog.aqueductValveIndex(FieldSharedActorCatalog.Role.AQUEDUCT_VALVE_2));
        assertEquals(-1, FieldSharedActorCatalog.aqueductValveIndex(FieldSharedActorCatalog.Role.AQUEDUCT_RELAY));

        assertEquals(0, FieldSharedActorCatalog.quarryCoreIndex(FieldSharedActorCatalog.Role.QUARRY_CORE_1));
        assertEquals(1, FieldSharedActorCatalog.quarryCoreIndex(FieldSharedActorCatalog.Role.QUARRY_CORE_2));
        assertEquals(-1, FieldSharedActorCatalog.quarryCoreIndex(FieldSharedActorCatalog.Role.QUARRY_RELAY));

        assertEquals(0, FieldSharedActorCatalog.oldRelayRecordIndex(FieldSharedActorCatalog.Role.OLD_RELAY_RECORD_1));
        assertEquals(1, FieldSharedActorCatalog.oldRelayRecordIndex(FieldSharedActorCatalog.Role.OLD_RELAY_RECORD_2));
        assertEquals(2, FieldSharedActorCatalog.oldRelayRecordIndex(FieldSharedActorCatalog.Role.OLD_RELAY_RECORD_3));
        assertEquals(3, FieldSharedActorCatalog.oldRelayRecordIndex(FieldSharedActorCatalog.Role.OLD_RELAY_RECORD_4));
        assertEquals(-1, FieldSharedActorCatalog.oldRelayRecordIndex(FieldSharedActorCatalog.Role.OLD_RELAY_FT));
    }
}
