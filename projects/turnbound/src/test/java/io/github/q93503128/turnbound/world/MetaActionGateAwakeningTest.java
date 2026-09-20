package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetaActionGateAwakeningTest {
    @Test
    void legacyMaterialAwakeningIsBlockedWithPlayerFacingCopy() {
        UUID playerId = UUID.randomUUID();
        for (int i = 1; i <= 4; i++) {
            assertEquals("현재 이 동료의 각성 경로는 열려 있지 않습니다.",
                    MetaActionGate.denial(playerId, "AWAKEN|F0" + i));
        }
    }

    @Test
    void authoredHeroAwakeningContinuesToNormalServerValidation() {
        assertEquals("", MetaActionGate.denial(UUID.randomUUID(), "AWAKEN|P01"));
    }
}
