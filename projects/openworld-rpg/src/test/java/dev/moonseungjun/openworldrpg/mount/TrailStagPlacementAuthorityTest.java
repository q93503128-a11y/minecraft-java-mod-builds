package dev.moonseungjun.openworldrpg.mount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrailStagPlacementAuthorityTest {
    @Test
    void summonSearchUsesExactEightCandidatePriority() {
        assertEquals(8, TrailStagPlacementAuthority.SUMMON_ORDER.size());
        assertEquals(
                new TrailStagPlacementAuthority.SummonCandidate(
                        TrailStagPlacementAuthority.RelativeDirection.BEHIND_RIGHT,
                        2.5
                ),
                TrailStagPlacementAuthority.SUMMON_ORDER.getFirst()
        );
        assertEquals(
                new TrailStagPlacementAuthority.SummonCandidate(
                        TrailStagPlacementAuthority.RelativeDirection.BEHIND,
                        3.5
                ),
                TrailStagPlacementAuthority.SUMMON_ORDER.getLast()
        );

        List<TrailStagPlacementAuthority.SummonProbe> probes = new ArrayList<>();
        for (var candidate : TrailStagPlacementAuthority.SUMMON_ORDER) {
            probes.add(new TrailStagPlacementAuthority.SummonProbe(
                    candidate,
                    false,
                    true,
                    0.0,
                    true,
                    true
            ));
        }
        var third = TrailStagPlacementAuthority.SUMMON_ORDER.get(2);
        probes.set(2, new TrailStagPlacementAuthority.SummonProbe(
                third,
                true,
                true,
                0.75,
                true,
                true
        ));

        assertEquals(
                third,
                TrailStagPlacementAuthority.firstLegalSummon(probes).orElseThrow()
        );
    }

    @Test
    void supportVarianceAbovePointSevenFiveIsRejected() {
        List<TrailStagPlacementAuthority.SummonProbe> probes = new ArrayList<>();
        for (var candidate : TrailStagPlacementAuthority.SUMMON_ORDER) {
            probes.add(new TrailStagPlacementAuthority.SummonProbe(
                    candidate,
                    true,
                    true,
                    0.750001,
                    true,
                    true
            ));
        }
        assertTrue(TrailStagPlacementAuthority.firstLegalSummon(probes).isEmpty());
    }

    @Test
    void dismountSearchUsesLeftRightRearDiagonalsThenRear() {
        assertEquals(
                List.of(
                        TrailStagPlacementAuthority.RelativeDirection.LEFT,
                        TrailStagPlacementAuthority.RelativeDirection.RIGHT,
                        TrailStagPlacementAuthority.RelativeDirection.BEHIND_LEFT,
                        TrailStagPlacementAuthority.RelativeDirection.BEHIND_RIGHT,
                        TrailStagPlacementAuthority.RelativeDirection.BEHIND
                ),
                TrailStagPlacementAuthority.DISMOUNT_ORDER
        );

        List<TrailStagPlacementAuthority.DismountProbe> probes = new ArrayList<>();
        for (var direction : TrailStagPlacementAuthority.DISMOUNT_ORDER) {
            probes.add(new TrailStagPlacementAuthority.DismountProbe(direction, false, true));
        }
        probes.set(3, new TrailStagPlacementAuthority.DismountProbe(
                TrailStagPlacementAuthority.RelativeDirection.BEHIND_RIGHT,
                true,
                true
        ));
        assertEquals(
                TrailStagPlacementAuthority.RelativeDirection.BEHIND_RIGHT,
                TrailStagPlacementAuthority.firstLegalDismount(probes).orElseThrow()
        );
    }
}
