package kr.moonseungjun.riftfrontier.expedition;

import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Authoritative immutable state for one expedition run.
 *
 * This domain record deliberately has no Minecraft/DFU dependency. Serialization belongs to
 * ExpeditionRunCodec so pure lifecycle tests can load and exercise the state machine without
 * constructing a Minecraft runtime.
 */
public record ExpeditionRun(
    long sequence,
    ContentId regionId,
    ContentId contractId,
    String contentFingerprint,
    Status status,
    Map<ContentId, Integer> recoveredResources,
    long startedGameTime,
    long endedGameTime
) {
    public enum Status {
        PREPARING("preparing"),
        DEPLOYED("deployed"),
        EXTRACTION_REQUESTED("extraction_requested"),
        EXTRACTED("extracted"),
        FAILED("failed");

        private final String serializedName;

        Status(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public boolean terminal() {
            return this == EXTRACTED || this == FAILED;
        }

        public static Status parse(String value) {
            for (Status status : values()) {
                if (status.serializedName.equals(value)) return status;
            }
            throw new IllegalArgumentException("Unknown expedition status '" + value + "'");
        }
    }

    public ExpeditionRun {
        if (sequence <= 0) throw new IllegalArgumentException("sequence must be > 0");
        Objects.requireNonNull(regionId, "regionId");
        Objects.requireNonNull(contractId, "contractId");
        contentFingerprint = Objects.requireNonNull(contentFingerprint, "contentFingerprint");
        Objects.requireNonNull(status, "status");
        Map<ContentId, Integer> normalized = new LinkedHashMap<>();
        Objects.requireNonNull(recoveredResources, "recoveredResources").forEach((resource, amount) -> {
            Objects.requireNonNull(resource, "resource id");
            if (amount == null || amount <= 0) throw new IllegalArgumentException("recovered resource amount must be > 0");
            normalized.put(resource, amount);
        });
        recoveredResources = Map.copyOf(normalized);
        if (startedGameTime < 0) throw new IllegalArgumentException("startedGameTime must be >= 0");
        if (endedGameTime < -1) throw new IllegalArgumentException("endedGameTime must be -1 or >= 0");
        if (status.terminal() && endedGameTime < startedGameTime) {
            throw new IllegalArgumentException("terminal expedition must end at or after start time");
        }
        if (!status.terminal() && endedGameTime != -1L) {
            throw new IllegalArgumentException("non-terminal expedition cannot have endedGameTime");
        }
    }

    public static ExpeditionRun preparing(long sequence, ContentId regionId, ContentId contractId, String contentFingerprint, long gameTime) {
        return new ExpeditionRun(sequence, regionId, contractId, contentFingerprint, Status.PREPARING, Map.of(), gameTime, -1L);
    }

    public ExpeditionRun deploy() {
        requireStatus(Status.PREPARING);
        return copy(Status.DEPLOYED, recoveredResources, -1L);
    }

    public ExpeditionRun recover(ContentId resourceId, int amount) {
        if (status != Status.DEPLOYED && status != Status.EXTRACTION_REQUESTED) {
            throw new IllegalStateException("Resources can only be recovered during a deployed expedition");
        }
        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        Map<ContentId, Integer> next = new LinkedHashMap<>(recoveredResources);
        next.merge(Objects.requireNonNull(resourceId, "resourceId"), amount, Math::addExact);
        return copy(status, next, -1L);
    }

    public ExpeditionRun requestExtraction() {
        requireStatus(Status.DEPLOYED);
        return copy(Status.EXTRACTION_REQUESTED, recoveredResources, -1L);
    }

    public ExpeditionRun extract(long gameTime) {
        requireStatus(Status.EXTRACTION_REQUESTED);
        return copy(Status.EXTRACTED, recoveredResources, gameTime);
    }

    public ExpeditionRun fail(long gameTime) {
        if (status.terminal()) throw new IllegalStateException("Expedition is already terminal: " + status);
        return copy(Status.FAILED, recoveredResources, gameTime);
    }

    private void requireStatus(Status expected) {
        if (status != expected) throw new IllegalStateException("Expected expedition status " + expected + " but was " + status);
    }

    private ExpeditionRun copy(Status nextStatus, Map<ContentId, Integer> resources, long endTime) {
        return new ExpeditionRun(sequence, regionId, contractId, contentFingerprint, nextStatus, resources, startedGameTime, endTime);
    }
}
