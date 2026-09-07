package kr.moonseungjun.riftfrontier.expedition;

import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
    Optional<UUID> ownerId,
    String contentFingerprint,
    Status status,
    Map<ContentId, Integer> recoveredResources,
    long startedGameTime,
    long endedGameTime,
    EndReason endReason
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

    /** Stable machine-readable terminal cause for review, recovery UX and future analytics. */
    public enum EndReason {
        NONE("none"),
        EXTRACTION("extraction"),
        PLAYER_ABORT("player_abort"),
        PLAYER_DEATH("player_death"),
        PLAYER_LOGOUT("player_logout"),
        SERVER_RESTART("server_restart"),
        OTHER_FAILURE("other_failure");

        private final String serializedName;

        EndReason(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public boolean failure() {
            return this != NONE && this != EXTRACTION;
        }

        public static EndReason parse(String value) {
            for (EndReason reason : values()) {
                if (reason.serializedName.equals(value)) return reason;
            }
            throw new IllegalArgumentException("Unknown expedition end reason '" + value + "'");
        }
    }

    public ExpeditionRun {
        if (sequence <= 0) throw new IllegalArgumentException("sequence must be > 0");
        Objects.requireNonNull(regionId, "regionId");
        Objects.requireNonNull(contractId, "contractId");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        contentFingerprint = Objects.requireNonNull(contentFingerprint, "contentFingerprint");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(endReason, "endReason");
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
        if (!status.terminal() && endReason != EndReason.NONE) {
            throw new IllegalArgumentException("non-terminal expedition cannot have an end reason");
        }
        if (status == Status.EXTRACTED && endReason != EndReason.EXTRACTION && endReason != EndReason.NONE) {
            throw new IllegalArgumentException("EXTRACTED expedition cannot have failure end reason " + endReason);
        }
        if (status == Status.FAILED && endReason == EndReason.EXTRACTION) {
            throw new IllegalArgumentException("FAILED expedition cannot use extraction end reason");
        }
    }

    /** Legacy/test fixture constructor. New gameplay runs should use the owner-aware overload. */
    public static ExpeditionRun preparing(long sequence, ContentId regionId, ContentId contractId, String contentFingerprint, long gameTime) {
        return preparing(sequence, regionId, contractId, Optional.empty(), contentFingerprint, gameTime);
    }

    public static ExpeditionRun preparing(long sequence, ContentId regionId, ContentId contractId, UUID ownerId, String contentFingerprint, long gameTime) {
        return preparing(sequence, regionId, contractId, Optional.of(Objects.requireNonNull(ownerId, "ownerId")), contentFingerprint, gameTime);
    }

    private static ExpeditionRun preparing(long sequence, ContentId regionId, ContentId contractId, Optional<UUID> ownerId, String contentFingerprint, long gameTime) {
        return new ExpeditionRun(sequence, regionId, contractId, ownerId, contentFingerprint, Status.PREPARING, Map.of(), gameTime, -1L, EndReason.NONE);
    }

    public boolean ownedBy(UUID playerId) {
        return ownerId.map(value -> value.equals(playerId)).orElse(false);
    }

    public ExpeditionRun deploy() {
        requireStatus(Status.PREPARING);
        return copy(Status.DEPLOYED, recoveredResources, -1L, EndReason.NONE);
    }

    public ExpeditionRun recover(ContentId resourceId, int amount) {
        if (status != Status.DEPLOYED && status != Status.EXTRACTION_REQUESTED) {
            throw new IllegalStateException("Resources can only be recovered during a deployed expedition");
        }
        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        Map<ContentId, Integer> next = new LinkedHashMap<>(recoveredResources);
        next.merge(Objects.requireNonNull(resourceId, "resourceId"), amount, Math::addExact);
        return copy(status, next, -1L, EndReason.NONE);
    }

    public ExpeditionRun requestExtraction() {
        requireStatus(Status.DEPLOYED);
        return copy(Status.EXTRACTION_REQUESTED, recoveredResources, -1L, EndReason.NONE);
    }

    public ExpeditionRun extract(long gameTime) {
        requireStatus(Status.EXTRACTION_REQUESTED);
        return copy(Status.EXTRACTED, recoveredResources, gameTime, EndReason.EXTRACTION);
    }

    public ExpeditionRun fail(long gameTime, EndReason reason) {
        if (status.terminal()) throw new IllegalStateException("Expedition is already terminal: " + status);
        Objects.requireNonNull(reason, "reason");
        if (!reason.failure()) throw new IllegalArgumentException("Failure transition requires a failure end reason");
        return copy(Status.FAILED, recoveredResources, gameTime, reason);
    }

    private void requireStatus(Status expected) {
        if (status != expected) throw new IllegalStateException("Expected expedition status " + expected + " but was " + status);
    }

    private ExpeditionRun copy(Status nextStatus, Map<ContentId, Integer> resources, long endTime, EndReason reason) {
        return new ExpeditionRun(sequence, regionId, contractId, ownerId, contentFingerprint, nextStatus, resources, startedGameTime, endTime, reason);
    }
}
