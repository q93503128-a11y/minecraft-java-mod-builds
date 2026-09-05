package io.github.q93503128.turnbound.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleCommandScopePolicyTest {
    @Test
    void onlyLiveBattlePresentationCommandsArePrivate() {
        assertTrue(BattleCommandScopePolicy.privatePresentation("ACT"));
        assertTrue(BattleCommandScopePolicy.privatePresentation("FOCUS"));
        assertTrue(BattleCommandScopePolicy.privatePresentation("AUTO"));
        assertTrue(BattleCommandScopePolicy.privatePresentation("SPEED"));
    }

    @Test
    void battleExitAndUnknownCommandsStayOutsidePrivateScope() {
        assertFalse(BattleCommandScopePolicy.privatePresentation("FLEE"));
        assertFalse(BattleCommandScopePolicy.privatePresentation("TRAVEL"));
        assertFalse(BattleCommandScopePolicy.privatePresentation(""));
        assertFalse(BattleCommandScopePolicy.privatePresentation(null));
    }
}
