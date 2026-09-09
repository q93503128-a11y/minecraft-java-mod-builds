package kr.moonseungjun.riftfrontier.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Region01BossRenderStateTest {
    @Test
    void entityIdentityMustBeExtractedAsNonNegativeMinecraftId() {
        Region01BossRenderState state = new Region01BossRenderState();
        assertEquals(-1, state.entityId());
        assertThrows(IllegalArgumentException.class, () -> state.setEntityId(-1));

        state.setEntityId(42);
        assertEquals(42, state.entityId());
    }
}
