package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndgameDeploymentServiceTest {
    @Test
    void normalStoryBossesAreValidRematches() {
        for (int i = 1; i <= 5; i++) {
            String id = "BATTLE_B0" + i;
            assertTrue(EndgameDeploymentService.normalBossRematch(id), id);
            assertTrue(EndgameDeploymentService.supported(id), id);
        }
    }

    @Test
    void ordinaryCampaignBattlesCannotMasqueradeAsRematches() {
        assertFalse(EndgameDeploymentService.normalBossRematch("ENC_M01"));
        assertFalse(EndgameDeploymentService.normalBossRematch("TUTORIAL_1"));
        assertFalse(EndgameDeploymentService.supported("ENC_M01"));
        assertFalse(EndgameDeploymentService.supported("BATTLE_B06"));
    }

    @Test
    void hardAndRiftIdsShareTheSameDeploymentSurface() {
        assertTrue(EndgameDeploymentService.supported("HARD_B01"));
        assertTrue(EndgameDeploymentService.supported("HARD_B05"));
        assertTrue(EndgameDeploymentService.supported("RIFT_F01"));
        assertTrue(EndgameDeploymentService.supported("RIFT_F30"));
        assertFalse(EndgameDeploymentService.supported("RIFT_F31"));
    }
}
