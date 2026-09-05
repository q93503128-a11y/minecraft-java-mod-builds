package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadiaHubActorCatalogTest {
    @Test
    void allSharedRolesHaveUniqueStableTagsAndRoundTrip() {
        Set<String> tags = new HashSet<>();
        assertEquals(14, RadiaHubActorCatalog.roles().size());
        for (RadiaHubActorCatalog.Role role : RadiaHubActorCatalog.roles()) {
            String tag = RadiaHubActorCatalog.roleTag(role);
            assertTrue(tags.add(tag), "duplicate Radia actor tag: " + tag);
            assertEquals(role, RadiaHubActorCatalog.fromTag(tag));
        }
    }

    @Test
    void tutorialAndFacilityRolesRemainDisjoint() {
        assertEquals(0, RadiaHubActorCatalog.Role.TUTORIAL_1.tutorialIndex());
        assertEquals(1, RadiaHubActorCatalog.Role.TUTORIAL_2.tutorialIndex());
        assertEquals(2, RadiaHubActorCatalog.Role.TUTORIAL_3.tutorialIndex());
        assertTrue(RadiaHubActorCatalog.Role.TUTORIAL_1.tutorial());
        assertFalse(RadiaHubActorCatalog.Role.TUTORIAL_1.facility());

        assertEquals(RadiaHubActorCatalog.Role.ECHO_ARCHIVE,
                RadiaHubActorCatalog.facilityRole("ECHO_ARCHIVE"));
        assertEquals(RadiaHubActorCatalog.Role.BARRACKS,
                RadiaHubActorCatalog.facilityRole("BARRACKS"));
        assertTrue(RadiaHubActorCatalog.Role.RIFT_GATE.facility());
        assertFalse(RadiaHubActorCatalog.Role.RIFT_GATE.tutorial());
    }

    @Test
    void malformedOrForeignTagsDoNotBecomeRadiaActors() {
        assertNull(RadiaHubActorCatalog.fromTag(null));
        assertNull(RadiaHubActorCatalog.fromTag("some_other_actor:DIRECTOR"));
        assertNull(RadiaHubActorCatalog.fromTag(RadiaHubActorCatalog.ROLE_TAG_PREFIX + "UNKNOWN"));
        assertNull(RadiaHubActorCatalog.facilityRole("NOT_A_FACILITY"));
    }
}
