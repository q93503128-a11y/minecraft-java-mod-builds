package dev.moonseungjun.openworldrpg.progression.r01;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Persistent Regalhart participation and reconnect-safe personal reward plans. */
public record R01RegalhartRewardState(
        int schemaVersion,
        Map<String, EncounterSnapshot> activeEncounters,
        Map<String, PendingFinalization> pendingFinalizations,
        Set<String> firstDefeatCommittedPlayers
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<R01RegalhartRewardState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("regalhart_reward_schema_version")
                            .forGetter(R01RegalhartRewardState::schemaVersion),
                    Codec.unboundedMap(Codec.STRING, EncounterSnapshot.CODEC)
                            .fieldOf("active_encounters")
                            .forGetter(R01RegalhartRewardState::activeEncounters),
                    Codec.unboundedMap(Codec.STRING, PendingFinalization.CODEC)
                            .fieldOf("pending_finalizations")
                            .forGetter(R01RegalhartRewardState::pendingFinalizations),
                    Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf)
                            .fieldOf("first_defeat_committed_players")
                            .forGetter(R01RegalhartRewardState::firstDefeatCommittedPlayers)
            ).apply(instance, R01RegalhartRewardState::new));

    public R01RegalhartRewardState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported Regalhart reward schema version: " + schemaVersion
            );
        }
        activeEncounters = Map.copyOf(
                Objects.requireNonNull(activeEncounters, "activeEncounters")
        );
        pendingFinalizations = Map.copyOf(
                Objects.requireNonNull(pendingFinalizations, "pendingFinalizations")
        );
        firstDefeatCommittedPlayers = Set.copyOf(
                Objects.requireNonNull(
                        firstDefeatCommittedPlayers,
                        "firstDefeatCommittedPlayers"
                )
        );

        activeEncounters.forEach((encounterId, snapshot) -> {
            requireStableId(encounterId, "encounterId");
            Objects.requireNonNull(snapshot, "snapshot");
        });
        pendingFinalizations.forEach((rewardId, pending) -> {
            requireStableId(rewardId, "rewardId");
            Objects.requireNonNull(pending, "pending");
            if (!rewardId.equals(pending.rewardId())) {
                throw new IllegalArgumentException(
                        "Regalhart pending map key/rewardId mismatch."
                );
            }
        });
        firstDefeatCommittedPlayers.forEach(
                R01RegalhartRewardState::requireUuid
        );
    }

    public static R01RegalhartRewardState initial() {
        return new R01RegalhartRewardState(
                CURRENT_SCHEMA_VERSION,
                Map.of(),
                Map.of(),
                Set.of()
        );
    }

    public Optional<EncounterSnapshot> encounter(String encounterId) {
        requireStableId(encounterId, "encounterId");
        return Optional.ofNullable(activeEncounters.get(encounterId));
    }

    public boolean hasParticipant(String encounterId, String playerUuid) {
        requireStableId(encounterId, "encounterId");
        requireUuid(playerUuid);
        EncounterSnapshot snapshot = activeEncounters.get(encounterId);
        return snapshot != null && snapshot.participants().containsKey(playerUuid);
    }

    public boolean firstDefeatCommitted(String playerUuid) {
        requireUuid(playerUuid);
        return firstDefeatCommittedPlayers.contains(playerUuid);
    }

    public R01RegalhartRewardState recordParticipation(
            String encounterId,
            long cycleIndex,
            String playerUuid,
            RootClass rewardClass
    ) {
        requireStableId(encounterId, "encounterId");
        requireUuid(playerUuid);
        Objects.requireNonNull(rewardClass, "rewardClass");
        if (cycleIndex < 0L) {
            throw new IllegalArgumentException(
                    "Regalhart reward cycle must be non-negative."
            );
        }

        EncounterSnapshot current = activeEncounters.get(encounterId);
        if (current == null) {
            current = new EncounterSnapshot(cycleIndex, Map.of());
        } else if (current.cycleIndex() != cycleIndex) {
            throw new IllegalStateException(
                    "Regalhart encounter cycle changed before resolution."
            );
        }

        EncounterSnapshot nextEncounter =
                current.recordFirstContribution(playerUuid, rewardClass);
        if (nextEncounter.equals(current)) {
            return this;
        }

        Map<String, EncounterSnapshot> next = new HashMap<>(activeEncounters);
        next.put(encounterId, nextEncounter);
        return new R01RegalhartRewardState(
                schemaVersion,
                Map.copyOf(next),
                pendingFinalizations,
                firstDefeatCommittedPlayers
        );
    }

    public R01RegalhartRewardState completeEncounter(
            String encounterId,
            Map<String, R01RegalhartRewardRules.RewardPlan> plans
    ) {
        requireStableId(encounterId, "encounterId");
        Objects.requireNonNull(plans, "plans");
        EncounterSnapshot encounter = activeEncounters.get(encounterId);
        if (encounter == null) {
            return this;
        }
        if (!plans.keySet().equals(encounter.participants().keySet())) {
            throw new IllegalArgumentException(
                    "Regalhart reward plans must match the exact participant set."
            );
        }

        Map<String, EncounterSnapshot> nextActive =
                new HashMap<>(activeEncounters);
        nextActive.remove(encounterId);
        Map<String, PendingFinalization> nextPending =
                new HashMap<>(pendingFinalizations);
        Set<String> nextFirstCommitted =
                new HashSet<>(firstDefeatCommittedPlayers);

        for (Map.Entry<String, RootClass> participant
                : encounter.participants().entrySet()) {
            String playerUuid = participant.getKey();
            R01RegalhartRewardRules.RewardPlan plan =
                    Objects.requireNonNull(plans.get(playerUuid), "rewardPlan");
            boolean expectedFirst = !firstDefeatCommittedPlayers.contains(
                    playerUuid
            );
            if (plan.firstEligibleDefeat() != expectedFirst) {
                throw new IllegalArgumentException(
                        "Regalhart first/repeat reward plan does not match committed player history."
                );
            }

            String rewardId = rewardId(
                    encounterId,
                    encounter.cycleIndex(),
                    playerUuid
            );
            PendingFinalization pending = new PendingFinalization(
                    rewardId,
                    encounterId,
                    encounter.cycleIndex(),
                    playerUuid,
                    participant.getValue(),
                    plan
            );
            PendingFinalization previous =
                    nextPending.putIfAbsent(rewardId, pending);
            if (previous != null && !previous.equals(pending)) {
                throw new IllegalStateException(
                        "Regalhart reward plan cannot reroll: " + rewardId
                );
            }
            if (plan.firstEligibleDefeat()) {
                nextFirstCommitted.add(playerUuid);
            }
        }

        return new R01RegalhartRewardState(
                schemaVersion,
                Map.copyOf(nextActive),
                Map.copyOf(nextPending),
                Set.copyOf(nextFirstCommitted)
        );
    }

    public List<PendingFinalization> pendingForPlayer(String playerUuid) {
        requireUuid(playerUuid);
        List<PendingFinalization> result = new ArrayList<>();
        for (PendingFinalization pending : pendingFinalizations.values()) {
            if (pending.playerUuid().equals(playerUuid)) {
                result.add(pending);
            }
        }
        result.sort(Comparator.comparing(PendingFinalization::rewardId));
        return List.copyOf(result);
    }

    public R01RegalhartRewardState clearPending(String rewardId) {
        requireStableId(rewardId, "rewardId");
        if (!pendingFinalizations.containsKey(rewardId)) {
            return this;
        }
        Map<String, PendingFinalization> next =
                new HashMap<>(pendingFinalizations);
        next.remove(rewardId);
        return new R01RegalhartRewardState(
                schemaVersion,
                activeEncounters,
                Map.copyOf(next),
                firstDefeatCommittedPlayers
        );
    }

    public static String rewardId(
            String encounterId,
            long cycleIndex,
            String playerUuid
    ) {
        requireStableId(encounterId, "encounterId");
        requireUuid(playerUuid);
        if (cycleIndex < 0L) {
            throw new IllegalArgumentException(
                    "Regalhart reward cycle must be non-negative."
            );
        }
        return encounterId + "/cycle/" + cycleIndex + "/reward/" + playerUuid;
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
            throw new IllegalArgumentException(
                    "Player UUID must be non-blank."
            );
        }
        try {
            java.util.UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid player UUID: " + value
            );
        }
    }

    public record EncounterSnapshot(
            long cycleIndex,
            Map<String, RootClass> participants
    ) {
        public static final Codec<EncounterSnapshot> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.LONG.fieldOf("cycle_index")
                                .forGetter(EncounterSnapshot::cycleIndex),
                        Codec.unboundedMap(Codec.STRING, RootClass.CODEC)
                                .fieldOf("participants")
                                .forGetter(EncounterSnapshot::participants)
                ).apply(instance, EncounterSnapshot::new));

        public EncounterSnapshot {
            if (cycleIndex < 0L) {
                throw new IllegalArgumentException(
                        "Regalhart encounter cycle must be non-negative."
                );
            }
            participants = Map.copyOf(
                    Objects.requireNonNull(participants, "participants")
            );
            participants.forEach((playerUuid, rewardClass) -> {
                requireUuid(playerUuid);
                Objects.requireNonNull(rewardClass, "rewardClass");
            });
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
            return new EncounterSnapshot(cycleIndex, Map.copyOf(next));
        }
    }

    public record PendingFinalization(
            String rewardId,
            String encounterId,
            long cycleIndex,
            String playerUuid,
            RootClass rewardClass,
            R01RegalhartRewardRules.RewardPlan plan
    ) {
        public static final Codec<PendingFinalization> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("reward_id")
                                .forGetter(PendingFinalization::rewardId),
                        Codec.STRING.fieldOf("encounter_id")
                                .forGetter(PendingFinalization::encounterId),
                        Codec.LONG.fieldOf("cycle_index")
                                .forGetter(PendingFinalization::cycleIndex),
                        Codec.STRING.fieldOf("player_uuid")
                                .forGetter(PendingFinalization::playerUuid),
                        RootClass.CODEC.fieldOf("reward_class")
                                .forGetter(PendingFinalization::rewardClass),
                        R01RegalhartRewardRules.RewardPlan.CODEC
                                .fieldOf("plan")
                                .forGetter(PendingFinalization::plan)
                ).apply(instance, PendingFinalization::new));

        public PendingFinalization {
            requireStableId(rewardId, "rewardId");
            requireStableId(encounterId, "encounterId");
            requireUuid(playerUuid);
            if (cycleIndex < 0L) {
                throw new IllegalArgumentException(
                        "Regalhart pending cycle must be non-negative."
                );
            }
            Objects.requireNonNull(rewardClass, "rewardClass");
            Objects.requireNonNull(plan, "plan");
            if (!rewardId.equals(
                    R01RegalhartRewardState.rewardId(
                            encounterId,
                            cycleIndex,
                            playerUuid
                    )
            )) {
                throw new IllegalArgumentException(
                        "Regalhart pending rewardId does not match encounter/cycle/player."
                );
            }
        }
    }
}
