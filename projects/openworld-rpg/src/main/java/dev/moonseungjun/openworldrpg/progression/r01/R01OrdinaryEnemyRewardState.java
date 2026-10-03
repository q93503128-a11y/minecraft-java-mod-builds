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

public record R01OrdinaryEnemyRewardState(
        int schemaVersion,
        Map<String, EncounterSnapshot> activeEncounters,
        Map<String, PendingFinalization> pendingFinalizations
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final Codec<R01OrdinaryEnemyRewardState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, CURRENT_SCHEMA_VERSION).fieldOf("ordinary_enemy_reward_schema_version").forGetter(R01OrdinaryEnemyRewardState::schemaVersion),
            Codec.unboundedMap(Codec.STRING, EncounterSnapshot.CODEC).fieldOf("active_encounters").forGetter(R01OrdinaryEnemyRewardState::activeEncounters),
            Codec.unboundedMap(Codec.STRING, PendingFinalization.CODEC).fieldOf("pending_finalizations").forGetter(R01OrdinaryEnemyRewardState::pendingFinalizations)
    ).apply(instance, R01OrdinaryEnemyRewardState::new));

    public R01OrdinaryEnemyRewardState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported ordinary-enemy reward schema version: " + schemaVersion);
        activeEncounters = Map.copyOf(Objects.requireNonNull(activeEncounters, "activeEncounters"));
        pendingFinalizations = Map.copyOf(Objects.requireNonNull(pendingFinalizations, "pendingFinalizations"));
        activeEncounters.forEach((id, snapshot) -> {
            requireStableId(id, "encounterId");
            Objects.requireNonNull(snapshot, "snapshot");
        });
        pendingFinalizations.forEach((id, pending) -> {
            requireStableId(id, "rewardId");
            Objects.requireNonNull(pending, "pending");
            if (!id.equals(pending.rewardId())) throw new IllegalArgumentException("Ordinary-enemy pending map key/rewardId mismatch.");
        });
    }

    public static R01OrdinaryEnemyRewardState initial() {
        return new R01OrdinaryEnemyRewardState(CURRENT_SCHEMA_VERSION, Map.of(), Map.of());
    }

    public boolean hasParticipant(String encounterId, String playerUuid) {
        requireStableId(encounterId, "encounterId");
        requireUuid(playerUuid);
        EncounterSnapshot snapshot = activeEncounters.get(encounterId);
        return snapshot != null && snapshot.participants().containsKey(playerUuid);
    }

    public Optional<EncounterSnapshot> encounter(String encounterId) {
        requireStableId(encounterId, "encounterId");
        return Optional.ofNullable(activeEncounters.get(encounterId));
    }

    public R01OrdinaryEnemyRewardState recordParticipation(
            String encounterId,
            R01OrdinaryEnemyRewardRules.EnemySource source,
            String playerUuid,
            RootClass rewardClass
    ) {
        requireStableId(encounterId, "encounterId");
        Objects.requireNonNull(source, "source");
        requireUuid(playerUuid);
        Objects.requireNonNull(rewardClass, "rewardClass");
        EncounterSnapshot current = activeEncounters.get(encounterId);
        if (current != null && current.source() != source) {
            throw new IllegalStateException("Ordinary-enemy encounter source cannot change for " + encounterId);
        }
        if (current == null) current = new EncounterSnapshot(source, Map.of());
        EncounterSnapshot nextEncounter = current.recordFirstContribution(playerUuid, rewardClass);
        if (nextEncounter.equals(current)) return this;
        Map<String, EncounterSnapshot> next = new HashMap<>(activeEncounters);
        next.put(encounterId, nextEncounter);
        return new R01OrdinaryEnemyRewardState(schemaVersion, Map.copyOf(next), pendingFinalizations);
    }

    public R01OrdinaryEnemyRewardState completeEncounter(
            String encounterId,
            Map<String, R01OrdinaryEnemyRewardRules.RewardPlan> onlinePlans
    ) {
        requireStableId(encounterId, "encounterId");
        Objects.requireNonNull(onlinePlans, "onlinePlans");
        EncounterSnapshot encounter = activeEncounters.get(encounterId);
        if (encounter == null) return this;
        if (!encounter.participants().keySet().containsAll(onlinePlans.keySet())) {
            throw new IllegalArgumentException("Ordinary-enemy plans contain a player who never qualified.");
        }

        Map<String, EncounterSnapshot> nextActive = new HashMap<>(activeEncounters);
        nextActive.remove(encounterId);
        Map<String, PendingFinalization> nextPending = new HashMap<>(pendingFinalizations);
        for (var entry : onlinePlans.entrySet()) {
            String playerUuid = entry.getKey();
            RootClass rewardClass = encounter.participants().get(playerUuid);
            if (rewardClass == null) throw new IllegalArgumentException("Missing qualifying reward class.");
            String rewardId = rewardId(encounterId, playerUuid);
            PendingFinalization pending = new PendingFinalization(
                    rewardId, encounterId, playerUuid, rewardClass, encounter.source(),
                    Objects.requireNonNull(entry.getValue(), "rewardPlan")
            );
            PendingFinalization previous = nextPending.putIfAbsent(rewardId, pending);
            if (previous != null && !previous.equals(pending)) {
                throw new IllegalStateException("Ordinary-enemy reward plan cannot reroll: " + rewardId);
            }
        }
        return new R01OrdinaryEnemyRewardState(schemaVersion, Map.copyOf(nextActive), Map.copyOf(nextPending));
    }

    public List<PendingFinalization> pendingForPlayer(String playerUuid) {
        requireUuid(playerUuid);
        List<PendingFinalization> result = new ArrayList<>();
        for (PendingFinalization pending : pendingFinalizations.values()) {
            if (pending.playerUuid().equals(playerUuid)) result.add(pending);
        }
        result.sort(Comparator.comparing(PendingFinalization::rewardId));
        return List.copyOf(result);
    }

    public R01OrdinaryEnemyRewardState clearPending(String rewardId) {
        requireStableId(rewardId, "rewardId");
        if (!pendingFinalizations.containsKey(rewardId)) return this;
        Map<String, PendingFinalization> next = new HashMap<>(pendingFinalizations);
        next.remove(rewardId);
        return new R01OrdinaryEnemyRewardState(schemaVersion, activeEncounters, Map.copyOf(next));
    }

    public static String rewardId(String encounterId, String playerUuid) {
        requireStableId(encounterId, "encounterId");
        requireUuid(playerUuid);
        return encounterId + "/reward/" + playerUuid;
    }

    private static void requireStableId(String value, String name) {
        if (value == null || value.isBlank() || value.indexOf(':') <= 0 || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(name + " must be a stable namespaced id.");
        }
    }

    private static void requireUuid(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Player UUID must be non-blank.");
        java.util.UUID.fromString(value);
    }

    public record EncounterSnapshot(
            R01OrdinaryEnemyRewardRules.EnemySource source,
            Map<String, RootClass> participants
    ) {
        public static final Codec<EncounterSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                R01OrdinaryEnemyRewardRules.EnemySource.CODEC.fieldOf("source").forGetter(EncounterSnapshot::source),
                Codec.unboundedMap(Codec.STRING, RootClass.CODEC).fieldOf("participants").forGetter(EncounterSnapshot::participants)
        ).apply(instance, EncounterSnapshot::new));

        public EncounterSnapshot {
            Objects.requireNonNull(source, "source");
            participants = Map.copyOf(Objects.requireNonNull(participants, "participants"));
            participants.forEach((playerUuid, rewardClass) -> {
                requireUuid(playerUuid);
                Objects.requireNonNull(rewardClass, "rewardClass");
            });
        }

        EncounterSnapshot recordFirstContribution(String playerUuid, RootClass rewardClass) {
            if (participants.containsKey(playerUuid)) return this;
            Map<String, RootClass> next = new HashMap<>(participants);
            next.put(playerUuid, rewardClass);
            return new EncounterSnapshot(source, Map.copyOf(next));
        }
    }

    public record PendingFinalization(
            String rewardId,
            String encounterId,
            String playerUuid,
            RootClass rewardClass,
            R01OrdinaryEnemyRewardRules.EnemySource source,
            R01OrdinaryEnemyRewardRules.RewardPlan plan
    ) {
        public static final Codec<PendingFinalization> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("reward_id").forGetter(PendingFinalization::rewardId),
                Codec.STRING.fieldOf("encounter_id").forGetter(PendingFinalization::encounterId),
                Codec.STRING.fieldOf("player_uuid").forGetter(PendingFinalization::playerUuid),
                RootClass.CODEC.fieldOf("reward_class").forGetter(PendingFinalization::rewardClass),
                R01OrdinaryEnemyRewardRules.EnemySource.CODEC.fieldOf("source").forGetter(PendingFinalization::source),
                R01OrdinaryEnemyRewardRules.RewardPlan.CODEC.fieldOf("plan").forGetter(PendingFinalization::plan)
        ).apply(instance, PendingFinalization::new));

        public PendingFinalization {
            requireStableId(rewardId, "rewardId");
            requireStableId(encounterId, "encounterId");
            requireUuid(playerUuid);
            Objects.requireNonNull(rewardClass, "rewardClass");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(plan, "plan");
            if (!rewardId.equals(R01OrdinaryEnemyRewardState.rewardId(encounterId, playerUuid))) {
                throw new IllegalArgumentException("Ordinary-enemy rewardId does not match encounter/player.");
            }
        }
    }
}