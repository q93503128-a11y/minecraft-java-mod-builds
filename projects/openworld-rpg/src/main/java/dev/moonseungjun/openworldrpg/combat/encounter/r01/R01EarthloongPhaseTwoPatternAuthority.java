package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class R01EarthloongPhaseTwoPatternAuthority {
    private R01EarthloongPhaseTwoPatternAuthority() {}

    public static List<UUID> forkedHeavenAssignments(List<UUID> rankedValidParticipants) {
        Objects.requireNonNull(rankedValidParticipants, "rankedValidParticipants");
        if (rankedValidParticipants.isEmpty()) return List.of();
        UUID first = Objects.requireNonNull(rankedValidParticipants.get(0), "participant");
        if (rankedValidParticipants.size() == 1) return List.of(first, first, first);
        UUID second = Objects.requireNonNull(rankedValidParticipants.get(1), "participant");
        if (rankedValidParticipants.size() == 2) return List.of(first, second, first);
        UUID third = Objects.requireNonNull(rankedValidParticipants.get(2), "participant");
        return List.of(first, second, third);
    }

    public static int forkedMarkerSpawnOffsetTicks(
            R01EarthloongEncounterData.ForkedHeavenPattern pattern, int markerIndex) {
        Objects.requireNonNull(pattern, "pattern");
        requireMarkerIndex(pattern, markerIndex);
        return markerIndex * pattern.markerIntervalTicks();
    }

    public static int forkedMarkerImpactOffsetTicks(
            R01EarthloongEncounterData.ForkedHeavenPattern pattern, int markerIndex) {
        return forkedMarkerSpawnOffsetTicks(pattern, markerIndex) + pattern.markerTellTicks();
    }

    public static int forkedCastCompleteOffsetTicks(
            R01EarthloongEncounterData.ForkedHeavenPattern pattern) {
        Objects.requireNonNull(pattern, "pattern");
        return forkedMarkerImpactOffsetTicks(pattern, pattern.markerCount() - 1)
                + pattern.recoveryTicks();
    }

    public static boolean forkedLaterMarkerMayHitAgain(
            boolean alreadyHitThisCast, boolean insideThisMarkerWhenTelegraphed) {
        return !alreadyHitThisCast || !insideThisMarkerWhenTelegraphed;
    }

    public static int earthlinePhysicalImpactOffsetTicks(
            R01EarthloongEncounterData.EarthlineSurgePattern pattern) {
        return Objects.requireNonNull(pattern, "pattern").tellTicks();
    }

    public static int earthlineLightningImpactOffsetTicks(
            R01EarthloongEncounterData.EarthlineSurgePattern pattern) {
        return earthlinePhysicalImpactOffsetTicks(pattern) + pattern.secondHitDelayTicks();
    }

    public static int earthlineCastCompleteOffsetTicks(
            R01EarthloongEncounterData.EarthlineSurgePattern pattern) {
        return earthlineLightningImpactOffsetTicks(pattern) + pattern.recoveryTicks();
    }

    private static void requireMarkerIndex(
            R01EarthloongEncounterData.ForkedHeavenPattern pattern, int markerIndex) {
        if (markerIndex < 0 || markerIndex >= pattern.markerCount()) {
            throw new IllegalArgumentException("Forked Heaven marker index out of range.");
        }
    }
}
