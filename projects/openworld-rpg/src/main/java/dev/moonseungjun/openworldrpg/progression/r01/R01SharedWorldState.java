package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/** Persistent shared R01 world-loop state stored on the server Overworld. */
public record R01SharedWorldState(
        int schemaVersion,
        long activeWorldTicks,
        long roadsideCycle,
        boolean roadsideActive,
        long roadsideLastEndActiveTicks,
        int roadsideRequirementBits,
        long roadsideEmptySinceActiveTicks,
        Map<String, ParticipantSnapshot> roadsideParticipants,
        Map<String, ParticipantSnapshot> pendingRoadsideFinalizations
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final int ALL_ROADSIDE_REQUIREMENTS = 0b111;

    public static final Codec<R01SharedWorldState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("shared_r01_schema_version")
                            .forGetter(R01SharedWorldState::schemaVersion),
                    Codec.LONG.fieldOf("active_world_ticks")
                            .forGetter(R01SharedWorldState::activeWorldTicks),
                    Codec.LONG.fieldOf("roadside_cycle")
                            .forGetter(R01SharedWorldState::roadsideCycle),
                    Codec.BOOL.fieldOf("roadside_active")
                            .forGetter(R01SharedWorldState::roadsideActive),
                    Codec.LONG.fieldOf("roadside_last_end_active_ticks")
                            .forGetter(R01SharedWorldState::roadsideLastEndActiveTicks),
                    Codec.intRange(0, ALL_ROADSIDE_REQUIREMENTS)
                            .fieldOf("roadside_requirement_bits")
                            .forGetter(R01SharedWorldState::roadsideRequirementBits),
                    Codec.LONG.fieldOf("roadside_empty_since_active_ticks")
                            .forGetter(R01SharedWorldState::roadsideEmptySinceActiveTicks),
                    Codec.unboundedMap(Codec.STRING, ParticipantSnapshot.CODEC)
                            .fieldOf("roadside_participants")
                            .forGetter(R01SharedWorldState::roadsideParticipants),
                    Codec.unboundedMap(Codec.STRING, ParticipantSnapshot.CODEC)
                            .fieldOf("pending_roadside_finalizations")
                            .forGetter(R01SharedWorldState::pendingRoadsideFinalizations)
            ).apply(instance, R01SharedWorldState::new)
    );

    public R01SharedWorldState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported shared R01 state schema version: " + schemaVersion
            );
        }
        if (activeWorldTicks < 0L || roadsideCycle < 0L) {
            throw new IllegalArgumentException("R01 shared counters must be non-negative.");
        }
        if (roadsideLastEndActiveTicks < -1L || roadsideEmptySinceActiveTicks < -1L) {
            throw new IllegalArgumentException("R01 shared time sentinels must be >= -1.");
        }
        if ((roadsideRequirementBits & ~ALL_ROADSIDE_REQUIREMENTS) != 0) {
            throw new IllegalArgumentException("Unknown Roadside Trouble requirement bits.");
        }
        roadsideParticipants = Map.copyOf(
                Objects.requireNonNull(roadsideParticipants, "roadsideParticipants")
        );
        pendingRoadsideFinalizations = Map.copyOf(
                Objects.requireNonNull(
                        pendingRoadsideFinalizations,
                        "pendingRoadsideFinalizations"
                )
        );
        roadsideParticipants.forEach(R01SharedWorldState::validateParticipantEntry);
        pendingRoadsideFinalizations.forEach(R01SharedWorldState::validateParticipantEntry);
        if (!roadsideActive) {
            if (roadsideRequirementBits != 0
                    || roadsideEmptySinceActiveTicks != -1L
                    || !roadsideParticipants.isEmpty()) {
                throw new IllegalArgumentException(
                        "Inactive Roadside Trouble cannot retain live encounter state."
                );
            }
        }
    }

    public static R01SharedWorldState initial() {
        return new R01SharedWorldState(
                CURRENT_SCHEMA_VERSION,
                0L,
                0L,
                false,
                -1L,
                0,
                -1L,
                Map.of(),
                Map.of()
        );
    }

    public R01SharedWorldState advanceActiveWorld(long ticks) {
        if (ticks < 0L) {
            throw new IllegalArgumentException("ticks must be non-negative.");
        }
        if (ticks == 0L) {
            return this;
        }
        return copy(
                Math.addExact(activeWorldTicks, ticks),
                roadsideCycle,
                roadsideActive,
                roadsideLastEndActiveTicks,
                roadsideRequirementBits,
                roadsideEmptySinceActiveTicks,
                roadsideParticipants,
                pendingRoadsideFinalizations
        );
    }

    public R01SharedWorldState startRoadside() {
        if (roadsideActive) {
            throw new IllegalStateException("Roadside Trouble is already active.");
        }
        return copy(
                activeWorldTicks,
                Math.addExact(roadsideCycle, 1L),
                true,
                roadsideLastEndActiveTicks,
                0,
                -1L,
                Map.of(),
                pendingRoadsideFinalizations
        );
    }

    public R01SharedWorldState recordRoadsideParticipation(
            String playerUuid,
            boolean dustQuestEligible,
            Optional<RootClass> rewardClass
    ) {
        requireUuid(playerUuid);
        rewardClass = Objects.requireNonNull(rewardClass, "rewardClass");
        if (!roadsideActive) {
            throw new IllegalStateException(
                    "Cannot record Roadside Trouble participation while event is inactive."
            );
        }

        ParticipantSnapshot nextSnapshot = new ParticipantSnapshot(
                roadsideCycle,
                dustQuestEligible,
                rewardClass
        );
        ParticipantSnapshot current = roadsideParticipants.get(playerUuid);
        if (nextSnapshot.equals(current)) {
            return this;
        }

        Map<String, ParticipantSnapshot> next = new HashMap<>(roadsideParticipants);
        if (current != null && current.cycle() == roadsideCycle) {
            nextSnapshot = new ParticipantSnapshot(
                    roadsideCycle,
                    current.dustQuestEligible() || dustQuestEligible,
                    current.rewardClass()
            );
        }
        next.put(playerUuid, nextSnapshot);
        return copy(
                activeWorldTicks,
                roadsideCycle,
                roadsideActive,
                roadsideLastEndActiveTicks,
                roadsideRequirementBits,
                -1L,
                Map.copyOf(next),
                pendingRoadsideFinalizations
        );
    }

    public R01SharedWorldState markRoadsideRequirement(RoadsideRequirement requirement) {
        Objects.requireNonNull(requirement, "requirement");
        if (!roadsideActive) {
            throw new IllegalStateException(
                    "Cannot complete Roadside Trouble requirement while event is inactive."
            );
        }
        int nextBits = roadsideRequirementBits | requirement.mask();
        if (nextBits == roadsideRequirementBits) {
            return this;
        }
        return copy(
                activeWorldTicks,
                roadsideCycle,
                roadsideActive,
                roadsideLastEndActiveTicks,
                nextBits,
                roadsideEmptySinceActiveTicks,
                roadsideParticipants,
                pendingRoadsideFinalizations
        );
    }

    public boolean roadsideRequirementsComplete() {
        return roadsideActive
                && roadsideRequirementBits == ALL_ROADSIDE_REQUIREMENTS;
    }

    public R01SharedWorldState completeRoadside() {
        if (!roadsideRequirementsComplete()) {
            throw new IllegalStateException(
                    "Roadside Trouble cannot complete before all three requirements."
            );
        }

        Map<String, ParticipantSnapshot> nextPending =
                new HashMap<>(pendingRoadsideFinalizations);
        for (Map.Entry<String, ParticipantSnapshot> entry
                : roadsideParticipants.entrySet()) {
            nextPending.put(entry.getKey(), entry.getValue());
        }

        return copy(
                activeWorldTicks,
                roadsideCycle,
                false,
                activeWorldTicks,
                0,
                -1L,
                Map.of(),
                Map.copyOf(nextPending)
        );
    }

    public R01SharedWorldState updateRoadsidePresence(boolean anyParticipantInside) {
        if (!roadsideActive) {
            return this;
        }
        if (anyParticipantInside) {
            if (roadsideEmptySinceActiveTicks == -1L) {
                return this;
            }
            return copy(
                    activeWorldTicks,
                    roadsideCycle,
                    true,
                    roadsideLastEndActiveTicks,
                    roadsideRequirementBits,
                    -1L,
                    roadsideParticipants,
                    pendingRoadsideFinalizations
            );
        }
        if (roadsideEmptySinceActiveTicks == -1L) {
            return copy(
                    activeWorldTicks,
                    roadsideCycle,
                    true,
                    roadsideLastEndActiveTicks,
                    roadsideRequirementBits,
                    activeWorldTicks,
                    roadsideParticipants,
                    pendingRoadsideFinalizations
            );
        }
        return this;
    }

    public boolean roadsideShouldAbandon() {
        return roadsideActive
                && roadsideEmptySinceActiveTicks >= 0L
                && activeWorldTicks - roadsideEmptySinceActiveTicks
                        >= R01RoadsideTroubleRules.ABANDON_EMPTY_TICKS;
    }

    public R01SharedWorldState abandonRoadside() {
        if (!roadsideActive) {
            return this;
        }
        return copy(
                activeWorldTicks,
                roadsideCycle,
                false,
                activeWorldTicks,
                0,
                -1L,
                Map.of(),
                pendingRoadsideFinalizations
        );
    }

    public OptionalLong pendingRoadsideCycle(String playerUuid) {
        requireUuid(playerUuid);
        ParticipantSnapshot snapshot = pendingRoadsideFinalizations.get(playerUuid);
        return snapshot == null
                ? OptionalLong.empty()
                : OptionalLong.of(snapshot.cycle());
    }

    public ParticipantSnapshot pendingRoadsideFinalization(String playerUuid) {
        requireUuid(playerUuid);
        return pendingRoadsideFinalizations.get(playerUuid);
    }

    public R01SharedWorldState clearPendingRoadsideFinalization(
            String playerUuid,
            long cycle
    ) {
        requireUuid(playerUuid);
        ParticipantSnapshot current = pendingRoadsideFinalizations.get(playerUuid);
        if (current == null) {
            return this;
        }
        if (current.cycle() != cycle) {
            throw new IllegalStateException(
                    "Roadside finalization cycle changed before commit."
            );
        }
        Map<String, ParticipantSnapshot> next =
                new HashMap<>(pendingRoadsideFinalizations);
        next.remove(playerUuid);
        return copy(
                activeWorldTicks,
                roadsideCycle,
                roadsideActive,
                roadsideLastEndActiveTicks,
                roadsideRequirementBits,
                roadsideEmptySinceActiveTicks,
                roadsideParticipants,
                Map.copyOf(next)
        );
    }

    private R01SharedWorldState copy(
            long nextActiveWorldTicks,
            long nextRoadsideCycle,
            boolean nextRoadsideActive,
            long nextRoadsideLastEnd,
            int nextRequirementBits,
            long nextEmptySince,
            Map<String, ParticipantSnapshot> nextParticipants,
            Map<String, ParticipantSnapshot> nextPending
    ) {
        return new R01SharedWorldState(
                schemaVersion,
                nextActiveWorldTicks,
                nextRoadsideCycle,
                nextRoadsideActive,
                nextRoadsideLastEnd,
                nextRequirementBits,
                nextEmptySince,
                nextParticipants,
                nextPending
        );
    }

    private static void validateParticipantEntry(
            String playerUuid,
            ParticipantSnapshot snapshot
    ) {
        requireUuid(playerUuid);
        Objects.requireNonNull(snapshot, "snapshot");
    }

    private static void requireUuid(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Player UUID string must be non-blank.");
        }
        try {
            java.util.UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid player UUID string: " + value);
        }
    }

    public enum RoadsideRequirement {
        THREAT_PACK_DEFEATED(1 << 0),
        WHEEL_BRACED(1 << 1),
        CARGO_RETURNED(1 << 2);

        private final int mask;

        RoadsideRequirement(int mask) {
            this.mask = mask;
        }

        public int mask() {
            return mask;
        }
    }

    public record ParticipantSnapshot(
            long cycle,
            boolean dustQuestEligible,
            Optional<RootClass> rewardClass
    ) {
        public static final Codec<ParticipantSnapshot> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.LONG.fieldOf("cycle")
                                .forGetter(ParticipantSnapshot::cycle),
                        Codec.BOOL.fieldOf("dust_quest_eligible")
                                .forGetter(ParticipantSnapshot::dustQuestEligible),
                        RootClass.CODEC.optionalFieldOf("reward_class")
                                .forGetter(ParticipantSnapshot::rewardClass)
                ).apply(instance, ParticipantSnapshot::new));

        public ParticipantSnapshot {
            if (cycle <= 0L) {
                throw new IllegalArgumentException(
                        "Roadside participant cycle must be positive."
                );
            }
            rewardClass = Objects.requireNonNull(rewardClass, "rewardClass");
        }
    }
}
