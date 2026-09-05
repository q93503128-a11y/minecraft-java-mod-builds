package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldSharedInteractionActorsTest {
    @Test
    void sharedRolesHaveStableUniqueTags() {
        Set<String> tags = new HashSet<>();
        for (FieldSharedInteractionActors.Role role : FieldSharedInteractionActors.Role.values()) {
            String tag = FieldSharedInteractionActors.roleTag(role);
            assertTrue(tag.startsWith("turnbound_field_role:"));
            assertTrue(tags.add(tag), "duplicate shared field actor tag: " + tag);
        }
        assertEquals(19, FieldSharedInteractionActors.Role.values().length);
        assertEquals(19, tags.size());
    }

    @Test
    void orderedQuestActorsKeepCanonicalIndexes() {
        assertEquals(0, FieldSharedInteractionActors.gloamSporeIndex(FieldSharedInteractionActors.Role.GLOAM_SPORE_1));
        assertEquals(1, FieldSharedInteractionActors.gloamSporeIndex(FieldSharedInteractionActors.Role.GLOAM_SPORE_2));
        assertEquals(2, FieldSharedInteractionActors.gloamSporeIndex(FieldSharedInteractionActors.Role.GLOAM_SPORE_3));
        assertEquals(-1, FieldSharedInteractionActors.gloamSporeIndex(FieldSharedInteractionActors.Role.GLOAM_RELAY));

        assertEquals(0, FieldSharedInteractionActors.aqueductValveIndex(FieldSharedInteractionActors.Role.AQUEDUCT_VALVE_1));
        assertEquals(1, FieldSharedInteractionActors.aqueductValveIndex(FieldSharedInteractionActors.Role.AQUEDUCT_VALVE_2));
        assertEquals(-1, FieldSharedInteractionActors.aqueductValveIndex(FieldSharedInteractionActors.Role.AQUEDUCT_RELAY));

        assertEquals(0, FieldSharedInteractionActors.quarryCoreIndex(FieldSharedInteractionActors.Role.QUARRY_CORE_1));
        assertEquals(1, FieldSharedInteractionActors.quarryCoreIndex(FieldSharedInteractionActors.Role.QUARRY_CORE_2));
        assertEquals(-1, FieldSharedInteractionActors.quarryCoreIndex(FieldSharedInteractionActors.Role.QUARRY_RELAY));

        assertEquals(0, FieldSharedInteractionActors.oldRelayRecordIndex(FieldSharedInteractionActors.Role.OLD_RELAY_RECORD_1));
        assertEquals(1, FieldSharedInteractionActors.oldRelayRecordIndex(FieldSharedInteractionActors.Role.OLD_RELAY_RECORD_2));
        assertEquals(2, FieldSharedInteractionActors.oldRelayRecordIndex(FieldSharedInteractionActors.Role.OLD_RELAY_RECORD_3));
        assertEquals(3, FieldSharedInteractionActors.oldRelayRecordIndex(FieldSharedInteractionActors.Role.OLD_RELAY_RECORD_4));
        assertEquals(-1, FieldSharedInteractionActors.oldRelayRecordIndex(FieldSharedInteractionActors.Role.OLD_RELAY_FT));
    }
}
