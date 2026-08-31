package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldSessionPersistenceProjectionTest {
    @Test
    void relogProjectionRestoresWholeSouthgateChapterUsingCanonicalIds() {
        Set<String> projected = StarterFieldProgress.project(Set.of(
                "ENC_M01", "ENC_M02", "ENC_M03", "ENC_M04", "ENC_M05", "BATTLE_B01",
                "ENC_G01", "ENC_RIFT_01"));

        assertEquals(Set.of("ENC_M01", "ENC_M02", "ENC_M03", "ENC_M04", "ENC_M05", "BATTLE_B01"), projected);
        assertEquals(5, StarterFieldProgress.normalClearCount(projected));
        assertTrue(StarterFieldProgress.starterPatrolComplete(projected));
        assertTrue(StarterFieldProgress.bossUnlocked(projected));
        assertTrue(StarterFieldProgress.chapterComplete(projected));
    }

    @Test
    void legacySouthgateSaveIdsAreCanonicalizedByFieldProjection() {
        Set<String> projected = StarterFieldProgress.project(Set.of(
                "southgate_enc_m01", "southgate_enc_m02", "southgate_enc_m04", "southgate_b01_graul"));
        assertEquals(Set.of("ENC_M01", "ENC_M02", "ENC_M04", "BATTLE_B01"), projected);
    }

    @Test
    void chapterGatesFollowCanonicalMainQuestBattleMilestones() {
        Set<String> early = Set.of("ENC_M01", "ENC_M02", "ENC_M03");
        assertTrue(StarterFieldProgress.starterPatrolComplete(early));
        assertFalse(StarterFieldProgress.bossUnlocked(early));
        assertFalse(StarterFieldProgress.chapterComplete(early));

        Set<String> bossOpen = Set.of("ENC_M01", "ENC_M02", "ENC_M04");
        assertTrue(StarterFieldProgress.bossUnlocked(bossOpen));
        assertFalse(StarterFieldProgress.chapterComplete(bossOpen));
    }

    @Test
    void freshProfileStartsWithNoSouthgateClears() {
        assertTrue(StarterFieldProgress.project(Set.of()).isEmpty());
        assertTrue(StarterFieldProgress.project(null).isEmpty());
    }
}
