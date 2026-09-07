package kr.moonseungjun.turnboundre.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M5CommandOverlayStateTest {
    @AfterEach
    void resetOverlayState() {
        BattleCommandOverlayState.close();
    }

    @Test
    void interactiveCommandOverlaySuppressesPassiveCommandHudOnlyWhileOpen() {
        BattleCommandOverlayState.close();
        assertFalse(BattleCommandOverlayState.isOpen());

        BattleCommandOverlayState.open();
        assertTrue(BattleCommandOverlayState.isOpen());

        BattleCommandOverlayState.close();
        assertFalse(BattleCommandOverlayState.isOpen());
    }
}
