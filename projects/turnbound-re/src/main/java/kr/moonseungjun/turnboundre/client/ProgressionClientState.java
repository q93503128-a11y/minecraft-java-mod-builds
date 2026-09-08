package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.network.CharacterPresentationNetworkPayloads;
import kr.moonseungjun.turnboundre.network.ProgressionNetworkPayloads;

import java.util.Map;
import java.util.Optional;

/** Client presentation cache only. Persistent progression truth remains server-owned. */
public final class ProgressionClientState {
    private static ProgressionNetworkPayloads.Snapshot snapshot;
    private static Map<String, String> sourceEntities = Map.of();
    private static long generation;

    private ProgressionClientState() {}

    public static void accept(ProgressionNetworkPayloads.ProgressSnapshotS2C payload) {
        if (payload == null) return;
        try {
            snapshot = payload.decode();
            generation++;
        } catch (RuntimeException invalidPayload) {
            snapshot = null;
            generation++;
        }
    }

    public static void accept(CharacterPresentationNetworkPayloads.CatalogS2C payload) {
        if (payload == null) return;
        try {
            sourceEntities = payload.decode();
            generation++;
        } catch (RuntimeException invalidPayload) {
            sourceEntities = Map.of();
            generation++;
        }
    }

    public static Optional<ProgressionNetworkPayloads.Snapshot> snapshot() {
        return Optional.ofNullable(snapshot);
    }

    public static Optional<String> sourceEntity(String characterId) {
        if (characterId == null || characterId.isBlank()) return Optional.empty();
        String sourceEntity = sourceEntities.get(characterId);
        return sourceEntity == null || sourceEntity.isBlank() ? Optional.empty() : Optional.of(sourceEntity);
    }

    public static long generation() {
        return generation;
    }

    public static void clear() {
        snapshot = null;
        sourceEntities = Map.of();
        generation++;
    }
}
