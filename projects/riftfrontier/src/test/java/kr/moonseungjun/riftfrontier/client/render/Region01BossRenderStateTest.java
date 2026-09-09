package kr.moonseungjun.riftfrontier.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Region01BossRenderStateTest {
    @Test
    void entityIdentityMustBeExtractedAsNonNegativeMinecraftId() {
        Region01BossRenderIdentity identity = new Region01BossRenderIdentity();
        assertEquals(-1, identity.entityId());
        assertThrows(IllegalArgumentException.class, () -> identity.setEntityId(-1));

        identity.setEntityId(42);
        assertEquals(42, identity.entityId());
    }
}
