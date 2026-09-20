package io.github.q93503128.turnbound.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AwakeningRouteRulesTest {
    @Test
    void coreHeroesUsePersonalQuestAwakeningInsteadOfSignatureTrialGate() {
        for (int i = 1; i <= 8; i++) {
            String id = "P0" + i;
            assertEquals(AwakeningRouteRules.Route.PERSONAL_QUEST, AwakeningRouteRules.route(id), id);
            assertFalse(AwakeningRouteRules.signatureTrialRoute(id), id);
            assertFalse(AwakeningRouteRules.canonGap(id), id);
        }
    }

    @Test
    void legacyMaterialCharactersStayUnavailableWithPlayerFacingCopy() {
        for (int i = 1; i <= 4; i++) {
            String id = "F0" + i;
            assertEquals(AwakeningRouteRules.Route.LEGACY_UNAVAILABLE, AwakeningRouteRules.route(id), id);
            assertTrue(AwakeningRouteRules.canonGap(id), id);
            assertEquals("현재 이 동료의 각성 경로는 열려 있지 않습니다.", AwakeningRouteRules.blockReason(id));
        }
    }

    @Test
    void unknownIdsAreNotSilentlyClassified() {
        assertFalse(AwakeningRouteRules.defined("P09"));
        assertFalse(AwakeningRouteRules.canonGap("P09"));
    }
}
