package dev.moonseungjun.openworldrpg.progression.r01;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Persistent shared Earthloong participation/finalization state.
 *
 * <p>One valid authored contribution fixes the reward class for that encounter. Completion moves
 * eligible participants to a reconnect-safe pending map so a brief disconnect at boss death cannot
 * lose or duplicate a personal resolution.</p>
 */
public record R01EarthloongEncounterState(
        int schemaVersion,
        Map<String, EncounterSnapshot> activeEncounters,
        Map<String, PendingFinalization> pendingFinalizations
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<R01EarthloongEncounterState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("earthloong_encounter_schema_version")
                            .forGetter(R01EarthloongEncounterState::schemaVersion),
                    Codec.unboundedMap(Codec.STRING, EncounterSnapshot.CODEC)
                            .fieldOf("active_encounters")
                            .forGetter(R01EarthloongEncounterState::activeEncounters),
                    Codec.unboundedMap(Codec.STRING, PendingFinalization.CODEC)
                            .fieldOf("pending_finalizations")
                            .forGetter(R01EarthloongEncounterState::pendingFinalizations)
            ).apply(instance, R01EarthloongEncounterState::new));

    public R01EarthloongEncounterState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported Earthloong encounter schema version: " + schemaVersion
            );
        }
        activeEncounters = Map.copyOf(
                Objects.requireNonNull(activeEncounters, "activeEncounters")
        );
        pendingFinalizations = Map.copyOf(
                Objects.requireNonNull(pendingFinalizations, "pendingFinalizations")
        );
        activeEncounters.forEach((encounterId, snapshot) -> {
            requireStableId(encounterId, "encounterId");
            Objects.requireNonNull(snapshot, "snapshot");
        });
        pendingFinalizations.forEach((playerUuid, snapshot) -> {
            requireUuid(playerUuid);
            Objects.requireNonNull(snapshot, "snapshot");
        });
    }

    public static R01EarthloongEncounterState initial() {
        return new R01EarthloongEncounterState(
                CURRENT_SCHEMA_VERSION,
                Map.of(),
                Map.of()
        );
    }

    public boolean hasParticipant(String encounterId, String playerUuid) {
        requireStableId(encounterId, "encounterId");
        requireUuid(playerUuid);
        EncounterSnapshot encounter = activeEncounters.get(encounterId);
        return encounter != null && encounter.participants().containsKey(playerUuid);
    }

    public R01EarthloongEncounterState recordParticipation(
            String encounterId,
            String playerUuid,
            RootClass rewardClass
    ) {
        requireStableId(encounterId, "encounterId");
        requireUuid(playerUuid);
        Objects.requireNonNull(rewardClass, "rewardClass");

        EncounterSnapshot current = activeEncounters.getOrDefault(
                encounterId,
                EncounterSnapshot.empty()
        );
        EncounterSnapshot nextEncounter =
                current.recordFirstContribution(playerUuid, rewardClass);
        if (current.equals(nextEncounter)) {
            return this;
        }

        Map<String, EncounterSnapshot> next = new HashMap<>(activeEncounters);
        next.put(encounterId, nextEncounter);
        return new R01EarthloongEncounterState(
                schemaVersion,
                Map.copyOf(next),
                pendingFinalizations
        );
    }

    public R01EarthloongEncounterState completeEncounter(String encounterId) {
        requireStableId(encounterId, "encounterId");
        EncounterSnapshot encounter = activeEncounters.get(encounterId);
        if (encounter == null) {
            return this;
        }

        Map<String, EncounterSnapshot> nextActive = new HashMap<>(activeEncounters);
        nextActive.remove(encounterId);

        Map<String, PendingFinalization> nextPending =
                new HashMap<>(pendingFinalizations);
        for (Map.Entry<String, RootClass> entry
                : encounter.participants().entrySet()) {
            nextPending.putIfAbsent(
                    entry.getKey(),
                    new PendingFinalization(encounterId, entry.getValue())
            );
        }

        return new R01EarthloongEncounterState(
                schemaVersion,
                Map.copyOf(nextActive),
                Map.copyOf(nextPending)
        );
    }

    public Optional<PendingFinalization> pendingFinalization(String playerUuid) {
        requireUuid(playerUuid);
        return Optional.ofNullable(pendingFinalizations.get(playerUuid));
    }

    public R01EarthloongEncounterState clearPendingFinalization(
            String playerUuid,
            String encounterId
    ) {
        requireUuid(playerUuid);
        requireStableId(encounterId, "encounterId");

        PendingFinalization current = pendingFinalizations.get(playerUuid);
        if (current == null) {
            return this;
        }
        if (!current.encounterId().equals(encounterId)) {
            throw new IllegalStateException(
                    "Earthloong pending encounter changed before finalization."
            );
        }

        Map<String, PendingFinalization> next =
                new HashMap<>(pendingFinalizations);
        next.remove(playerUuid);
        return new R01EarthloongEncounterState(
                schemaVersion,
                activeEncounters,
                Map.copyOf(next)
        );
    }

    private static void requireStableId(String value, String name) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    name + " must be a stable namespaced id."
            );
        }
    }

    private static void requireUuid(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Player UUID must be non-blank.");
        }
        try {
            java.util.UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid player UUID: " + value);
        }
    }

    public record EncounterSnapshot(
            Map<String, RootClass> participants
    ) {
        public static final Codec<EncounterSnapshot> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.unboundedMap(Codec.STRING, RootClass.CODEC)
                                .fieldOf("participants")
                                .forGetter(EncounterSnapshot::participants)
                ).apply(instance, EncounterSnapshot::new));

        public EncounterSnapshot {
            participants = Map.copyOf(
                    Objects.requireNonNull(participants, "participants")
            );
            participants.forEach((playerUuid, rewardClass) -> {
                requireUuid(playerUuid);
                Objects.requireNonNull(rewardClass, "rewardClass");
            });
        }

        public static EncounterSnapshot empty() {
            return new EncounterSnapshot(Map.of());
        }

        public EncounterSnapshot recordFirstContribution(
                String playerUuid,
                RootClass rewardClass
        ) {
            requireUuid(playerUuid);
            Objects.requireNonNull(rewardClass, "rewardClass");
            if (participants.containsKey(playerUuid)) {
                return this;
            }
            Map<String, RootClass> next = new HashMap<>(participants);
            next.put(playerUuid, rewardClass);
            return new EncounterSnapshot(Map.copyOf(next));
        }
    }

    public record PendingFinalization(
            String encounterId,
            RootClass rewardClass
    ) {
        public static final Codec<PendingFinalization> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("encounter_id")
                                .forGetter(PendingFinalization::encounterId),
                        RootClass.CODEC.fieldOf("reward_class")
                                .forGetter(PendingFinalization::rewardClass)
                ).apply(instance, PendingFinalization::new));

        public PendingFinalization {
            requireStableId(encounterId, "encounterId");
            Objects.requireNonNull(rewardClass, "rewardClass");
        }
    }
}
