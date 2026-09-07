package kr.moonseungjun.riftfrontier.expedition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FieldPlayReviewSnapshotTest {
    private static final String OWNER = "123e4567-e89b-12d3-a456-426614174000";

    @Test
    void reportLineIsStableAndCarriesFieldPlayEvidence() {
        FieldPlayReviewSnapshot snapshot = new FieldPlayReviewSnapshot(
            7L, OWNER, "deployed", "none", false, 240L, 2, 3,
            2, 2, 1, 4, 120, 1,
            5, 6, 3, true, "1234567890abcdef"
        );

        assertEquals(5, snapshot.plannedThreats());
        assertEquals(
            "run=7;owner=" + OWNER + ";status=deployed;endReason=none;elapsedTicks=240;salvage=2;pressure=3;plan=5[hunter=2,scout=2,elite=1]"
                + ";liveThreats=4;hazard=120t@2;hubSalvage=5;supply=6;nextCost=3;content=current;fingerprint=1234567890ab",
            snapshot.reportLine()
        );
    }

    @Test
    void terminalSnapshotsCarryCauseWithoutPretendingToKnowPostCleanupThreatCount() {
        FieldPlayReviewSnapshot snapshot = new FieldPlayReviewSnapshot(
            8L, OWNER, "failed", "server_restart", true, 400L, 3, 4,
            3, 2, 1, -1, 140, 1,
            8, 4, 4, false, "abcdef"
        );

        assertTrue(snapshot.reportLine().contains("owner=" + OWNER));
        assertTrue(snapshot.reportLine().contains("endReason=server_restart"));
        assertTrue(snapshot.reportLine().contains("liveThreats=terminal"));
        assertTrue(snapshot.reportLine().contains("content=stale"));
        assertThrows(IllegalArgumentException.class, () -> new FieldPlayReviewSnapshot(
            8L, OWNER, "failed", "server_restart", true, 400L, 3, 4,
            3, 2, 1, 0, 140, 1,
            8, 4, 4, false, "abcdef"
        ));
        assertThrows(IllegalArgumentException.class, () -> new FieldPlayReviewSnapshot(
            9L, OWNER, "deployed", "player_logout", false, 12L, 0, 0,
            1, 1, 1, 3, 100, 0,
            0, 1, 1, true, "abcdef"
        ));
        assertThrows(IllegalArgumentException.class, () -> new FieldPlayReviewSnapshot(
            10L, "", "deployed", "none", false, 12L, 0, 0,
            1, 1, 1, 3, 100, 0,
            0, 1, 1, true, "abcdef"
        ));
    }
}
