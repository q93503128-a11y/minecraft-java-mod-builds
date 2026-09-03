package io.github.q93503128.turnbound.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AwakeningRouteRulesTest {
    @Test
    void coreHeroesUseTheEightAuthoredSignatureTrials() {
        for (int i = 1; i <= 8; i++) {
            String id = "P0" + i;
            assertEquals(AwakeningRouteRules.Route.SIGNATURE_TRIAL, AwakeningRouteRules.route(id), id);
            assertFalse(AwakeningRouteRules.canonGap(id), id);
        }
    }

    @Test
    void materialCharactersStayBlockedAtTheCanonConflict() {
        for (int i = 1; i <= 4; i++) {
            String id = "F0" + i;
            assertEquals(AwakeningRouteRules.Route.CANON_GAP, AwakeningRouteRules.route(id), id);
            assertTrue(AwakeningRouteRules.blockReason(id).startsWith("CANON GAP"), id);
        }
    }

    @Test
    void unknownIdsAreNotSilentlyClassified() {
        assertFalse(AwakeningRouteRules.defined("P09"));
        assertFalse(AwakeningRouteRules.canonGap("P09"));
    }
}
