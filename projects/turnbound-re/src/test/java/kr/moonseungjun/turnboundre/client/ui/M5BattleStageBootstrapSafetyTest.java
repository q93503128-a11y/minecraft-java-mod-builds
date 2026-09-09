package kr.moonseungjun.turnboundre.client.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class M5BattleStageBootstrapSafetyTest {
    @Test
    void battleStageHudCanInitializeBeforeRegistryBoundItemStacksAreSafe() {
        assertDoesNotThrow(() -> Class.forName(
                BattleStageHud.class.getName(),
                true,
                BattleStageHud.class.getClassLoader()));
    }
}
