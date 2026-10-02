package dev.moonseungjun.openworldrpg.progression.r01;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Persistent per-instance Nature Spirit participation and pre-rolled personal reward plans. */
public record R01NatureSpiritRewardState(
        int schemaVersion,
        Map<String, EncounterSnapshot> activeEncounters,
        Map<String, PendingFinalization> pendingFinalizations
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<R01NatureSpiritRewardState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("nature_spirit_reward_schema_version")
                            .forGetter(R01NatureSpiritRewardState::schemaVersion),
                    Codec.unboundedMap(Codec.STRING, EncounterSnapshot.CODEC)
                            .fieldOf("active_encounters")
                            .forGetter(R01NatureSpiritRewardState::activeEncounters),
                    Codec.unboundedMap(Codec.STRING, PendingFinalization.CODEC)
                            .fieldOf("pending_finalizations")
                            .forGetter(R01NatureSpiritRewardState::pendingFinalizations)
            ).apply(instance, R01NatureSpiritRewardState::new));

    public R01NatureSpiritRewardState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported Nature Spirit reward schema version: " + schemaVersion
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
        pendingFinalizations.forEach((rewardId, pending) -> {
            requireStableId(rewardId, "rewardId");
            Objects.requireNonNull(pending, "pending");
            if (!rewardId.equals(pending.rewardId())) {
                throw new IllegalArgumentException(
                        "Nature Spirit pending map key/rewardId mismatch."
                );
            }
        });
    }

    public static R01NatureSpiritRewardState initial() {
        return new R01NatureSpiritRewardState(
                CURRENT_SCHEMA_VERSION,
                Map.of(),
                Map.of()
        );
    }

    public boolean hasParticipant(String encounterId, String playerUuid) {
        requireStableId(encounterId, "encounterId");
        requireUuid(playerUuid);
        EncounterSnapshot snapshot = activeEncounters.get(encounterId);
        return snapshot != null
                && snapshot.participants().containsKey(playerUuid);
    }

    public Optional<EncounterSnapshot> encounter(String encounterId) {
        requireStableId(encounterId, "encounterId");
        return Optional.ofNullable(activeEncounters.get(encounterId));
    }

    public R01NatureSpiritRewardState recordParticipation(
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
        if (nextEncounter.equals(current)) {
            return this;
        }
        Map<String, EncounterSnapshot> next =
                new HashMap<>(activeEncounters);
        next.put(encounterId, nextEncounter);
        return new R01NatureSpiritRewardState(
                schemaVersion,
                Map.copyOf(next),
                pendingFinalizations
        );
    }

    public R01NatureSpiritRewardState completeEncounter(
            String encounterId,
            Map<String, R01NatureSpiritRewardRules.RewardPlan> plans
    ) {
        requireStableId(encounterId, "encounterId");
        Objects.requireNonNull(plans, "plans");
        EncounterSnapshot encounter = activeEncounters.get(encounterId);
        if (encounter == null) {
            return this;
        }
        Set<String> participants = encounter.participants().keySet();
        if (!plans.keySet().equals(participants)) {
            throw new IllegalArgumentException(
                    "Nature Spirit reward plans must match the exact eligible participant set."
            );
        }

        Map<String, EncounterSnapshot> nextActive =
                new HashMap<>(activeEncounters);
        nextActive.remove(encounterId);
        Map<String, PendingFinalization> nextPending =
                new HashMap<>(pendingFinalizations);
        for (Map.Entry<String, RootClass> participant
                : encounter.participants().entrySet()) {
            String playerUuid = participant.getKey();
            String rewardId = rewardId(encounterId, playerUuid);
            PendingFinalization pending = new PendingFinalization(
                    rewardId,
                    encounterId,
                    playerUuid,
                    participant.getValue(),
                    Objects.requireNonNull(plans.get(playerUuid), "rewardPlan")
            );
            PendingFinalization previous =
                    nextPending.putIfAbsent(rewardId, pending);
            if (previous != null && !previous.equals(pending)) {
                throw new IllegalStateException(
                        "Nature Spirit reward plan cannot reroll: " + rewardId
                );
            }
        }

        return new R01NatureSpiritRewardState(
                schemaVersion,
                Map.copyOf(nextActive),
                Map.copyOf(nextPending)
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

    public R01NatureSpiritRewardState clearPending(String rewardId) {
        requireStableId(rewardId, "rewardId");
        if (!pendingFinalizations.containsKey(rewardId)) {
            return this;
        }
        Map<String, PendingFinalization> next =
                new HashMap<>(pendingFinalizations);
        next.remove(rewardId);
        return new R01NatureSpiritRewardState(
                schemaVersion,
                activeEncounters,
                Map.copyOf(next)
        );
    }

    public static String rewardId(
            String encounterId,
            String playerUuid
    ) {
        requireStableId(encounterId, "encounterId");
        requireUuid(playerUuid);
        return encounterId + "/reward/" + playerUuid;
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
            String rewardId,
            String encounterId,
            String playerUuid,
            RootClass rewardClass,
            R01NatureSpiritRewardRules.RewardPlan plan
    ) {
        public static final Codec<PendingFinalization> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("reward_id")
                                .forGetter(PendingFinalization::rewardId),
                        Codec.STRING.fieldOf("encounter_id")
                                .forGetter(PendingFinalization::encounterId),
                        Codec.STRING.fieldOf("player_uuid")
                                .forGetter(PendingFinalization::playerUuid),
                        RootClass.CODEC.fieldOf("reward_class")
                                .forGetter(PendingFinalization::rewardClass),
                        R01NatureSpiritRewardRules.RewardPlan.CODEC
                                .fieldOf("plan")
                                .forGetter(PendingFinalization::plan)
                ).apply(instance, PendingFinalization::new));

        public PendingFinalization {
            requireStableId(rewardId, "rewardId");
            requireStableId(encounterId, "encounterId");
            requireUuid(playerUuid);
            Objects.requireNonNull(rewardClass, "rewardClass");
            Objects.requireNonNull(plan, "plan");
            if (!rewardId.equals(
                    R01NatureSpiritRewardState.rewardId(
                            encounterId,
                            playerUuid
                    )
            )) {
                throw new IllegalArgumentException(
                        "Nature Spirit pending rewardId does not match encounter/player."
                );
            }
        }
    }
}
