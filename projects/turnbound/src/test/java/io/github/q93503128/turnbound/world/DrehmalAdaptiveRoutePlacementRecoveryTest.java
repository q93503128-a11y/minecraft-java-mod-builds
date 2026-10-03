package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalAdaptiveRoutePlacementRecoveryTest {
    @Test
    void incompleteOpeningSnapshotRetriesOnlyAfterDelay() {
        var empty = new DrehmalAdaptiveRoutePlacement.Snapshot(Map.of(), Map.of(), Map.of(), List.of());

        assertFalse(DrehmalAdaptiveRoutePlacement.openingEncounterReady(empty));
        assertFalse(DrehmalAdaptiveRoutePlacement.openingRecoveryDue(
                empty, 0, 79L, DrehmalAdaptiveRoutePlacement.OPENING_RECOVERY_INTERVAL_TICKS));
        assertTrue(DrehmalAdaptiveRoutePlacement.openingRecoveryDue(
                empty, 0, DrehmalAdaptiveRoutePlacement.OPENING_RECOVERY_INTERVAL_TICKS,
                DrehmalAdaptiveRoutePlacement.OPENING_RECOVERY_INTERVAL_TICKS));
        assertFalse(DrehmalAdaptiveRoutePlacement.openingRecoveryDue(
                empty, DrehmalAdaptiveRoutePlacement.OPENING_RECOVERY_MAX_ATTEMPTS,
                10_000L, 0L));
    }

    @Test
    void readyOpeningSnapshotNeverSchedulesRecovery() {
        var slot = new DrehmalFirstRouteCatalog.EncounterSlot(
                DrabyelOpeningTutorial.ENCOUNTER_SLOT,
                DrabyelOpeningTutorial.ENCOUNTER_SITE,
                "COMMON",
                DrabyelOpeningTutorial.FOOTPRINT_ID,
                "",
                DrabyelOpeningTutorial.ENCOUNTER_ID,
                "뉴 드라비엘 북문 순찰대",
                2,
                true,
                true);
        var ready = new DrehmalAdaptiveRoutePlacement.Snapshot(
                Map.of(), Map.of(), Map.of(), List.of(slot));

        assertTrue(DrehmalAdaptiveRoutePlacement.openingEncounterReady(ready));
        assertFalse(DrehmalAdaptiveRoutePlacement.openingRecoveryDue(ready, 0, 10_000L, 0L));
    }
}
