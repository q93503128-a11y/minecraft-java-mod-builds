package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongEncounterDataLoader;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongPhaseTwoPatternAuthority;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01EarthloongPhaseTwoPatternAuthorityTest {
    private static final UUID A = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID B = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID C = UUID.fromString("30000000-0000-0000-0000-000000000003");

    @Test
    void forkedHeavenDistributionMatchesCanon() {
        assertEquals(List.of(A, A, A),
                R01EarthloongPhaseTwoPatternAuthority.forkedHeavenAssignments(List.of(A)));
        assertEquals(List.of(A, B, A),
                R01EarthloongPhaseTwoPatternAuthority.forkedHeavenAssignments(List.of(A, B)));
        assertEquals(List.of(A, B, C),
                R01EarthloongPhaseTwoPatternAuthority.forkedHeavenAssignments(List.of(A, B, C)));
    }

    @Test
    void forkedMarkersEachReceiveFullTell() {
        var p = R01EarthloongEncounterDataLoader.loadBundled().forkedHeaven();
        assertEquals(0, R01EarthloongPhaseTwoPatternAuthority.forkedMarkerSpawnOffsetTicks(p, 0));
        assertEquals(8, R01EarthloongPhaseTwoPatternAuthority.forkedMarkerSpawnOffsetTicks(p, 1));
        assertEquals(16, R01EarthloongPhaseTwoPatternAuthority.forkedMarkerSpawnOffsetTicks(p, 2));
        assertEquals(22, R01EarthloongPhaseTwoPatternAuthority.forkedMarkerImpactOffsetTicks(p, 0));
        assertEquals(30, R01EarthloongPhaseTwoPatternAuthority.forkedMarkerImpactOffsetTicks(p, 1));
        assertEquals(38, R01EarthloongPhaseTwoPatternAuthority.forkedMarkerImpactOffsetTicks(p, 2));
        assertEquals(58, R01EarthloongPhaseTwoPatternAuthority.forkedCastCompleteOffsetTicks(p));
    }

    @Test
    void laterMarkerRepeatRequiresEnteringAfterTelegraph() {
        assertFalse(R01EarthloongPhaseTwoPatternAuthority.forkedLaterMarkerMayHitAgain(true, true));
        assertTrue(R01EarthloongPhaseTwoPatternAuthority.forkedLaterMarkerMayHitAgain(true, false));
        assertTrue(R01EarthloongPhaseTwoPatternAuthority.forkedLaterMarkerMayHitAgain(false, true));
    }

    @Test
    void earthlineKeepsElevenTickGapAndRecovery() {
        var p = R01EarthloongEncounterDataLoader.loadBundled().earthlineSurge();
        assertEquals(16, R01EarthloongPhaseTwoPatternAuthority.earthlinePhysicalImpactOffsetTicks(p));
        assertEquals(27, R01EarthloongPhaseTwoPatternAuthority.earthlineLightningImpactOffsetTicks(p));
        assertEquals(45, R01EarthloongPhaseTwoPatternAuthority.earthlineCastCompleteOffsetTicks(p));
    }
}
