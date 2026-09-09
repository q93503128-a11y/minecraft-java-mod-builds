package kr.moonseungjun.riftfrontier.client.render;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Region01BossRenderStateTest {
    @Test
    void entityIdentityRequiresBothMinecraftIdAndUuid() {
        Region01BossRenderIdentity identity = new Region01BossRenderIdentity();
        UUID uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");

        assertEquals(-1, identity.entityId());
        assertFalse(identity.extracted());
        assertThrows(IllegalStateException.class, identity::entityUuid);
        assertThrows(IllegalArgumentException.class, () -> identity.set(-1, uuid));
        assertThrows(NullPointerException.class, () -> identity.set(42, null));

        identity.set(42, uuid);
        assertTrue(identity.extracted());
        assertEquals(42, identity.entityId());
        assertEquals(uuid, identity.entityUuid());
    }
}
