package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/**
 * Presentation-only target marker cache.
 * Live world entities use UUID lookup while virtual stage participants use stable participant-id lookup.
 * Target legality still comes exclusively from the current authoritative server snapshot.
 */
public final class BattleTargetMarkerState {
    public enum MarkerKind {
        ELIGIBLE,
        HOVERED,
        SELECTED
    }

    private record MarkerEntry(MarkerKind kind, int ordinal) {}

    private static final Object LOCK = new Object();
    private static UUID battleId;
    private static long revision = Long.MIN_VALUE;
    private static Map<UUID, MarkerEntry> entityMarkers = Map.of();
    private static Map<String, MarkerEntry> participantMarkers = Map.of();

    private BattleTargetMarkerState() {}

    public static void publish(
            UUID sourceBattleId,
            long sourceRevision,
            List<BattleNetworkPayloads.SnapshotParticipant> eligibleParticipants,
            Set<String> selectedTargetIds,
            String hoveredTargetId
    ) {
        if (sourceBattleId == null) throw new IllegalArgumentException("sourceBattleId must not be null");
        List<BattleNetworkPayloads.SnapshotParticipant> participants = eligibleParticipants == null
                ? List.of() : List.copyOf(eligibleParticipants);
        Set<String> selected = selectedTargetIds == null ? Set.of() : Set.copyOf(selectedTargetIds);
        String hovered = hoveredTargetId == null ? "" : hoveredTargetId;

        Map<UUID, MarkerEntry> nextEntities = new LinkedHashMap<>();
        Map<String, MarkerEntry> nextParticipants = new LinkedHashMap<>();
        for (int index = 0; index < participants.size(); index++) {
            BattleNetworkPayloads.SnapshotParticipant participant = participants.get(index);
            if (participant == null) continue;
            MarkerKind kind = selected.contains(participant.id())
                    ? MarkerKind.SELECTED
                    : participant.id().equals(hovered)
                    ? MarkerKind.HOVERED
                    : MarkerKind.ELIGIBLE;
            MarkerEntry entry = new MarkerEntry(kind, index + 1);
            if (nextParticipants.putIfAbsent(participant.id(), entry) != null) {
                throw new IllegalArgumentException("duplicate target participant id: " + participant.id());
            }
            if (participant.entityId() != null) nextEntities.put(participant.entityId(), entry);
        }

        synchronized (LOCK) {
            battleId = sourceBattleId;
            revision = sourceRevision;
            entityMarkers = Map.copyOf(nextEntities);
            participantMarkers = Map.copyOf(nextParticipants);
        }
    }

    public static Optional<MarkerKind> markerFor(
            UUID expectedBattleId,
            long expectedRevision,
            UUID entityId
    ) {
        MarkerEntry entry = entityEntryFor(expectedBattleId, expectedRevision, entityId);
        return entry == null ? Optional.empty() : Optional.of(entry.kind());
    }

    public static OptionalInt markerOrdinalFor(
            UUID expectedBattleId,
            long expectedRevision,
            UUID entityId
    ) {
        MarkerEntry entry = entityEntryFor(expectedBattleId, expectedRevision, entityId);
        return entry == null ? OptionalInt.empty() : OptionalInt.of(entry.ordinal());
    }

    public static Optional<MarkerKind> markerForParticipant(
            UUID expectedBattleId,
            long expectedRevision,
            String participantId
    ) {
        MarkerEntry entry = participantEntryFor(expectedBattleId, expectedRevision, participantId);
        return entry == null ? Optional.empty() : Optional.of(entry.kind());
    }

    public static OptionalInt markerOrdinalForParticipant(
            UUID expectedBattleId,
            long expectedRevision,
            String participantId
    ) {
        MarkerEntry entry = participantEntryFor(expectedBattleId, expectedRevision, participantId);
        return entry == null ? OptionalInt.empty() : OptionalInt.of(entry.ordinal());
    }

    public static boolean isPublishedFor(UUID expectedBattleId, long expectedRevision) {
        if (expectedBattleId == null) return false;
        synchronized (LOCK) {
            return expectedBattleId.equals(battleId) && expectedRevision == revision;
        }
    }

    private static MarkerEntry entityEntryFor(UUID expectedBattleId, long expectedRevision, UUID entityId) {
        if (expectedBattleId == null || entityId == null) return null;
        synchronized (LOCK) {
            if (!expectedBattleId.equals(battleId) || expectedRevision != revision) return null;
            return entityMarkers.get(entityId);
        }
    }

    private static MarkerEntry participantEntryFor(
            UUID expectedBattleId,
            long expectedRevision,
            String participantId
    ) {
        if (expectedBattleId == null || participantId == null || participantId.isBlank()) return null;
        synchronized (LOCK) {
            if (!expectedBattleId.equals(battleId) || expectedRevision != revision) return null;
            return participantMarkers.get(participantId);
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            battleId = null;
            revision = Long.MIN_VALUE;
            entityMarkers = Map.of();
            participantMarkers = Map.of();
        }
    }
}
