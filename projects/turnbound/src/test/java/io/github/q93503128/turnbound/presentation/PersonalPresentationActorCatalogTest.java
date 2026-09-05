package io.github.q93503128.turnbound.presentation;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class PersonalPresentationActorCatalogTest {
    @Test
    void ownerTagRoundTrips() {
        UUID owner = UUID.fromString("12345678-1234-5678-9abc-def012345678");
        String tag = PersonalPresentationActorCatalog.ownerTag(owner);
        assertTrue(tag.startsWith(PersonalPresentationActorCatalog.OWNER_PREFIX));
        assertEquals(owner, PersonalPresentationActorCatalog.ownerFromTag(tag));
    }

    @Test
    void malformedAndUnrelatedTagsAreRejected() {
        assertNull(PersonalPresentationActorCatalog.ownerFromTag(null));
        assertNull(PersonalPresentationActorCatalog.ownerFromTag(""));
        assertNull(PersonalPresentationActorCatalog.ownerFromTag(PersonalPresentationActorCatalog.OWNER_PREFIX));
        assertNull(PersonalPresentationActorCatalog.ownerFromTag(PersonalPresentationActorCatalog.OWNER_PREFIX + "not-a-uuid"));
        assertNull(PersonalPresentationActorCatalog.ownerFromTag("turnbound_other:12345678-1234-5678-9abc-def012345678"));
    }
}
