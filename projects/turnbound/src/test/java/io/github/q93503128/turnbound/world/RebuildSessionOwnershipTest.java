package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

final class RebuildSessionOwnershipTest {
    @Test void rejectedDuplicateDoesNotOwnOrReleaseAnotherConnection() {
        UUID id = UUID.randomUUID();
        Object first = new Object();
        Object duplicate = new Object();
        RebuildSessionOwnership<Object> owners = new RebuildSessionOwnership<>();
        assertTrue(owners.claim(id, first));
        assertFalse(owners.claim(id, duplicate));
        assertTrue(owners.owns(id, first));
        assertFalse(owners.owns(id, duplicate));
        assertFalse(owners.release(id, duplicate), "duplicate logout must not clear active session");
        assertTrue(owners.owns(id, first));
        assertTrue(owners.release(id, first));
        assertFalse(owners.hasOwner(id));
    }

    @Test void deathCloneCanTakeOwnershipWithoutChangingPlayerIdentityKey() {
        UUID id = UUID.randomUUID();
        Object previous = new Object();
        Object clone = new Object();
        Object unrelated = new Object();
        RebuildSessionOwnership<Object> owners = new RebuildSessionOwnership<>();
        owners.claim(id, previous);
        assertFalse(owners.transfer(id, unrelated, clone));
        assertTrue(owners.transfer(id, previous, clone));
        assertFalse(owners.owns(id, previous));
        assertTrue(owners.owns(id, clone));
        assertFalse(owners.release(id, previous));
        assertTrue(owners.release(id, clone));
        assertTrue(owners.claim(id, new Object()));
        owners.clear();
        assertFalse(owners.hasOwner(id));
    }
}
