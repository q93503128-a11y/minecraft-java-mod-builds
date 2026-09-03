package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JVM-only contract test for the identifier surface used by EndgameDeploymentService.
 * Minecraft-backed catalog/unlock authority remains inside the production service and integration path.
 */
class EndgameDeploymentServiceTest {
    @Test
    void normalStoryBossesAreValidRematches() {
        for (int i = 1; i <= 5; i++) {
            String id = "BATTLE_B0" + i;
            assertTrue(EndgameDeploymentIdRules.normalBossRematch(id), id);
            assertTrue(EndgameDeploymentIdRules.supported(id), id);
        }
    }

    @Test
    void ordinaryCampaignBattlesCannotMasqueradeAsRematches() {
        assertFalse(EndgameDeploymentIdRules.normalBossRematch("ENC_M01"));
        assertFalse(EndgameDeploymentIdRules.normalBossRematch("TUTORIAL_1"));
        assertFalse(EndgameDeploymentIdRules.supported("ENC_M01"));
        assertFalse(EndgameDeploymentIdRules.supported("BATTLE_B06"));
        assertFalse(EndgameDeploymentIdRules.supported(null));
    }

    @Test
    void hardAndRiftIdsShareTheSameDeploymentSurface() {
        assertTrue(EndgameDeploymentIdRules.supported("HARD_B01"));
        assertTrue(EndgameDeploymentIdRules.supported("HARD_B05"));
        assertTrue(EndgameDeploymentIdRules.supported("RIFT_F01"));
        assertTrue(EndgameDeploymentIdRules.supported("RIFT_F30"));
        assertFalse(EndgameDeploymentIdRules.supported("HARD_B06"));
        assertFalse(EndgameDeploymentIdRules.supported("RIFT_F00"));
        assertFalse(EndgameDeploymentIdRules.supported("RIFT_F31"));
        assertFalse(EndgameDeploymentIdRules.supported("RIFT_F1"));
    }
}
