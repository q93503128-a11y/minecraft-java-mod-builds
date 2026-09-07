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
 * Presentation-only world target marker cache.
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
    private static Map<UUID, MarkerEntry> markers = Map.of();

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

        Map<UUID, MarkerEntry> next = new LinkedHashMap<>();
        for (int index = 0; index < participants.size(); index++) {
            BattleNetworkPayloads.SnapshotParticipant participant = participants.get(index);
            if (participant == null || participant.entityId() == null) continue;
            MarkerKind kind = selected.contains(participant.id())
                    ? MarkerKind.SELECTED
                    : participant.id().equals(hovered)
                    ? MarkerKind.HOVERED
                    : MarkerKind.ELIGIBLE;
            next.put(participant.entityId(), new MarkerEntry(kind, index + 1));
        }

        synchronized (LOCK) {
            battleId = sourceBattleId;
            revision = sourceRevision;
            markers = Map.copyOf(next);
        }
    }

    public static Optional<MarkerKind> markerFor(
            UUID expectedBattleId,
            long expectedRevision,
            UUID entityId
    ) {
        MarkerEntry entry = entryFor(expectedBattleId, expectedRevision, entityId);
        return entry == null ? Optional.empty() : Optional.of(entry.kind());
    }

    public static OptionalInt markerOrdinalFor(
            UUID expectedBattleId,
            long expectedRevision,
            UUID entityId
    ) {
        MarkerEntry entry = entryFor(expectedBattleId, expectedRevision, entityId);
        return entry == null ? OptionalInt.empty() : OptionalInt.of(entry.ordinal());
    }

    private static MarkerEntry entryFor(UUID expectedBattleId, long expectedRevision, UUID entityId) {
        if (expectedBattleId == null || entityId == null) return null;
        synchronized (LOCK) {
            if (!expectedBattleId.equals(battleId) || expectedRevision != revision) return null;
            return markers.get(entityId);
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            battleId = null;
            revision = Long.MIN_VALUE;
            markers = Map.of();
        }
    }
}
