package dev.moonseungjun.openworldrpg.progression.r01;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.progression.r01.R01QuarryRoomEncounterRules.RoomId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Persistent shared non-spatial state for R01 Quarry room encounters.
 *
 * <p>Physical volumes/entities call this state through the controller. Cleared rooms survive
 * ordinary-room defeat/reconnect inside the same run, while the current uncleared room can reset
 * without erasing earlier clears.</p>
 */
public record R01QuarryRoomEncounterState(
        int schemaVersion,
        Map<String, RunSnapshot> encounters,
        Map<String, List<PendingAttribution>> pendingAttributions
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static final Codec<Set<RoomId>> ROOM_SET_CODEC = RoomId.CODEC.listOf().xmap(
            Set::copyOf,
            value -> value.stream().sorted().toList()
    );

    public static final Codec<R01QuarryRoomEncounterState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("quarry_room_encounter_schema_version")
                            .forGetter(R01QuarryRoomEncounterState::schemaVersion),
                    Codec.unboundedMap(Codec.STRING, RunSnapshot.CODEC)
                            .fieldOf("encounters")
                            .forGetter(R01QuarryRoomEncounterState::encounters),
                    Codec.unboundedMap(
                                    Codec.STRING,
                                    PendingAttribution.CODEC.listOf()
                            )
                            .fieldOf("pending_attributions")
                            .forGetter(R01QuarryRoomEncounterState::pendingAttributions)
            ).apply(instance, R01QuarryRoomEncounterState::new));

    public R01QuarryRoomEncounterState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported Quarry room encounter schema: " + schemaVersion
            );
        }
        encounters = Map.copyOf(Objects.requireNonNull(encounters, "encounters"));

        Map<String, List<PendingAttribution>> pendingCopy = new HashMap<>();
        Objects.requireNonNull(pendingAttributions, "pendingAttributions")
                .forEach((uuid, values) -> {
                    requireUuid(uuid);
                    pendingCopy.put(uuid, List.copyOf(values));
                });
        pendingAttributions = Map.copyOf(pendingCopy);

        encounters.forEach((encounterId, snapshot) -> {
            requireStableId(encounterId);
            Objects.requireNonNull(snapshot, "snapshot");
        });
    }

    public static R01QuarryRoomEncounterState initial() {
        return new R01QuarryRoomEncounterState(
                CURRENT_SCHEMA_VERSION,
                Map.of(),
                Map.of()
        );
    }

    public Optional<RunSnapshot> run(String encounterId) {
        requireStableId(encounterId);
        return Optional.ofNullable(encounters.get(encounterId));
    }

    public List<PendingAttribution> pendingFor(String playerUuid) {
        requireUuid(playerUuid);
        return pendingAttributions.getOrDefault(playerUuid, List.of());
    }

    public R01QuarryRoomEncounterState beginRoom(
            String encounterId,
            long runId,
            RoomId room,
            int engagedPlayers
    ) {
        requireStableId(encounterId);
        requireRunId(runId);
        Objects.requireNonNull(room, "room");
        R01QuarryRoomEncounterRules.initialPlan(room, engagedPlayers);

        RunSnapshot current = encounters.get(encounterId);
        if (current == null || current.runId() != runId) {
            current = RunSnapshot.fresh(runId);
        }

        if (current.clearedRooms().contains(room)) {
            return withRun(encounterId, current);
        }
        if (current.activeRoom().isPresent()) {
            RoomSnapshot active = current.activeRoom().orElseThrow();
            if (active.room() == room) {
                return withRun(encounterId, current);
            }
            throw new IllegalStateException(
                    "Cannot start another Quarry room while one is active."
            );
        }
        requireSequence(current.clearedRooms(), room);

        RoomSnapshot active = RoomSnapshot.start(room, engagedPlayers);
        return withRun(encounterId, current.withActive(active));
    }

    public R01QuarryRoomEncounterState activateUpperGallerySecondWave(
            String encounterId,
            int engagedPlayers
    ) {
        requireStableId(encounterId);
        RunSnapshot run = requireRun(encounterId);
        RoomSnapshot active = run.activeRoom().orElseThrow(
                () -> new IllegalStateException("No active Quarry room.")
        );
        RoomSnapshot next = active.activateUpperSecondWave(engagedPlayers);
        if (active.equals(next)) {
            return this;
        }
        return withRun(encounterId, run.withActive(next));
    }

    public R01QuarryRoomEncounterState recordParticipation(
            String encounterId,
            String playerUuid,
            RootClass contributionClass
    ) {
        requireStableId(encounterId);
        requireUuid(playerUuid);
        Objects.requireNonNull(contributionClass, "contributionClass");

        RunSnapshot run = requireRun(encounterId);
        RoomSnapshot active = run.activeRoom().orElseThrow(
                () -> new IllegalStateException("No active Quarry room.")
        );
        RoomSnapshot next = active.recordFirstContribution(
                playerUuid,
                contributionClass
        );
        if (active.equals(next)) {
            return this;
        }
        return withRun(encounterId, run.withActive(next));
    }

    public R01QuarryRoomEncounterState recordEnemyDefeat(
            String encounterId,
            int defeatedCount
    ) {
        requireStableId(encounterId);
        if (defeatedCount <= 0) {
            throw new IllegalArgumentException("defeatedCount must be positive.");
        }

        RunSnapshot run = requireRun(encounterId);
        RoomSnapshot active = run.activeRoom().orElseThrow(
                () -> new IllegalStateException("No active Quarry room.")
        );
        RoomSnapshot nextRoom = active.defeat(defeatedCount);
        if (!nextRoom.complete()) {
            return withRun(encounterId, run.withActive(nextRoom));
        }

        Set<RoomId> cleared = new HashSet<>(run.clearedRooms());
        cleared.add(active.room());
        RunSnapshot nextRun = new RunSnapshot(
                run.runId(),
                Set.copyOf(cleared),
                Optional.empty()
        );

        Map<String, List<PendingAttribution>> nextPending =
                new HashMap<>(pendingAttributions);
        R01QuarryRunContribution contribution = contributionFor(active.room());
        active.contributionClasses().forEach((uuid, owner) -> {
            List<PendingAttribution> values =
                    new ArrayList<>(nextPending.getOrDefault(uuid, List.of()));
            PendingAttribution pending = new PendingAttribution(
                    run.runId(),
                    contribution,
                    owner
            );
            if (!values.contains(pending)) {
                values.add(pending);
            }
            nextPending.put(uuid, List.copyOf(values));
        });

        return new R01QuarryRoomEncounterState(
                schemaVersion,
                replaceRun(encounterId, nextRun),
                Map.copyOf(nextPending)
        );
    }

    public R01QuarryRoomEncounterState resetActiveRoom(String encounterId) {
        requireStableId(encounterId);
        RunSnapshot run = requireRun(encounterId);
        if (run.activeRoom().isEmpty()) {
            return this;
        }
        return withRun(
                encounterId,
                new RunSnapshot(run.runId(), run.clearedRooms(), Optional.empty())
        );
    }

    public R01QuarryRoomEncounterState clearEncounter(String encounterId) {
        requireStableId(encounterId);
        if (!encounters.containsKey(encounterId)) {
            return this;
        }
        Map<String, RunSnapshot> next = new HashMap<>(encounters);
        next.remove(encounterId);
        return new R01QuarryRoomEncounterState(
                schemaVersion,
                Map.copyOf(next),
                pendingAttributions
        );
    }

    public R01QuarryRoomEncounterState clearPending(
            String playerUuid,
            PendingAttribution expected
    ) {
        requireUuid(playerUuid);
        Objects.requireNonNull(expected, "expected");
        List<PendingAttribution> current = pendingAttributions.get(playerUuid);
        if (current == null || !current.contains(expected)) {
            return this;
        }

        ArrayList<PendingAttribution> nextValues = new ArrayList<>(current);
        nextValues.remove(expected);
        Map<String, List<PendingAttribution>> next =
                new HashMap<>(pendingAttributions);
        if (nextValues.isEmpty()) {
            next.remove(playerUuid);
        } else {
            next.put(playerUuid, List.copyOf(nextValues));
        }
        return new R01QuarryRoomEncounterState(
                schemaVersion,
                encounters,
                Map.copyOf(next)
        );
    }

    private RunSnapshot requireRun(String encounterId) {
        RunSnapshot run = encounters.get(encounterId);
        if (run == null) {
            throw new IllegalStateException("Unknown Quarry room encounter.");
        }
        return run;
    }

    private R01QuarryRoomEncounterState withRun(
            String encounterId,
            RunSnapshot run
    ) {
        Map<String, RunSnapshot> next = replaceRun(encounterId, run);
        if (next.equals(encounters)) {
            return this;
        }
        return new R01QuarryRoomEncounterState(
                schemaVersion,
                next,
                pendingAttributions
        );
    }

    private Map<String, RunSnapshot> replaceRun(
            String encounterId,
            RunSnapshot run
    ) {
        Map<String, RunSnapshot> next = new HashMap<>(encounters);
        next.put(encounterId, run);
        return Map.copyOf(next);
    }

    private static void requireSequence(Set<RoomId> cleared, RoomId room) {
        boolean valid = switch (room) {
            case UPPER_GALLERY -> true;
            case COLLAPSED_HOIST -> cleared.contains(RoomId.UPPER_GALLERY);
            case ROOT_BREACHED -> cleared.contains(RoomId.COLLAPSED_HOIST);
        };
        if (!valid) {
            throw new IllegalStateException(
                    "Quarry room started before its canonical predecessor cleared."
            );
        }
    }

    private static R01QuarryRunContribution contributionFor(RoomId room) {
        return switch (room) {
            case UPPER_GALLERY ->
                    R01QuarryRunContribution.UPPER_GALLERY_COMBAT;
            case COLLAPSED_HOIST ->
                    R01QuarryRunContribution.COLLAPSED_HOIST_COMBAT;
            case ROOT_BREACHED ->
                    R01QuarryRunContribution.ROOT_BREACHED_COMBAT;
        };
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "encounterId must be a stable namespaced id."
            );
        }
    }

    private static void requireRunId(long runId) {
        if (runId <= 0L) {
            throw new IllegalArgumentException("runId must be positive.");
        }
    }

    private static void requireUuid(String value) {
        try {
            java.util.UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid player UUID: " + value);
        }
    }

    public record RunSnapshot(
            long runId,
            Set<RoomId> clearedRooms,
            Optional<RoomSnapshot> activeRoom
    ) {
        public static final Codec<RunSnapshot> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.LONG.fieldOf("run_id").forGetter(RunSnapshot::runId),
                        ROOM_SET_CODEC.fieldOf("cleared_rooms")
                                .forGetter(RunSnapshot::clearedRooms),
                        RoomSnapshot.CODEC.optionalFieldOf("active_room")
                                .forGetter(RunSnapshot::activeRoom)
                ).apply(instance, RunSnapshot::new));

        public RunSnapshot {
            requireRunId(runId);
            clearedRooms = Set.copyOf(
                    Objects.requireNonNull(clearedRooms, "clearedRooms")
            );
            activeRoom = Objects.requireNonNull(activeRoom, "activeRoom");
        }

        public static RunSnapshot fresh(long runId) {
            return new RunSnapshot(runId, Set.of(), Optional.empty());
        }

        public RunSnapshot withActive(RoomSnapshot room) {
            return new RunSnapshot(
                    runId,
                    clearedRooms,
                    Optional.of(Objects.requireNonNull(room, "room"))
            );
        }
    }

    public record RoomSnapshot(
            RoomId room,
            int engagedPlayers,
            boolean secondWaveActivated,
            int spawnedEnemies,
            int remainingEnemies,
            Map<String, RootClass> contributionClasses
    ) {
        public static final Codec<RoomSnapshot> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        RoomId.CODEC.fieldOf("room").forGetter(RoomSnapshot::room),
                        Codec.intRange(1, 4).fieldOf("engaged_players")
                                .forGetter(RoomSnapshot::engagedPlayers),
                        Codec.BOOL.fieldOf("second_wave_activated")
                                .forGetter(RoomSnapshot::secondWaveActivated),
                        Codec.INT.fieldOf("spawned_enemies")
                                .forGetter(RoomSnapshot::spawnedEnemies),
                        Codec.INT.fieldOf("remaining_enemies")
                                .forGetter(RoomSnapshot::remainingEnemies),
                        Codec.unboundedMap(Codec.STRING, RootClass.CODEC)
                                .fieldOf("contribution_classes")
                                .forGetter(RoomSnapshot::contributionClasses)
                ).apply(instance, RoomSnapshot::new));

        public RoomSnapshot {
            Objects.requireNonNull(room, "room");
            R01QuarryRoomEncounterRules.initialPlan(room, engagedPlayers);
            if (spawnedEnemies <= 0
                    || remainingEnemies < 0
                    || remainingEnemies > spawnedEnemies) {
                throw new IllegalArgumentException("Invalid Quarry room enemy counts.");
            }
            contributionClasses = Map.copyOf(
                    Objects.requireNonNull(
                            contributionClasses,
                            "contributionClasses"
                    )
            );
            contributionClasses.keySet().forEach(
                    R01QuarryRoomEncounterState::requireUuid
            );
            if (room != RoomId.UPPER_GALLERY && !secondWaveActivated) {
                secondWaveActivated = true;
            }
        }

        public static RoomSnapshot start(RoomId room, int engagedPlayers) {
            int initialCount = R01QuarryRoomEncounterRules
                    .initialPlan(room, engagedPlayers)
                    .actorCount();
            return new RoomSnapshot(
                    room,
                    engagedPlayers,
                    room != RoomId.UPPER_GALLERY,
                    initialCount,
                    initialCount,
                    Map.of()
            );
        }

        public RoomSnapshot activateUpperSecondWave(int currentEngagedPlayers) {
            if (room != RoomId.UPPER_GALLERY) {
                throw new IllegalStateException(
                        "Only Upper Gallery has a staggered second activation."
                );
            }
            if (secondWaveActivated) {
                return this;
            }
            int added = R01QuarryRoomEncounterRules
                    .upperGallerySecondWave(currentEngagedPlayers)
                    .actorCount();
            return new RoomSnapshot(
                    room,
                    Math.max(engagedPlayers, currentEngagedPlayers),
                    true,
                    spawnedEnemies + added,
                    remainingEnemies + added,
                    contributionClasses
            );
        }

        public RoomSnapshot recordFirstContribution(
                String uuid,
                RootClass owner
        ) {
            requireUuid(uuid);
            Objects.requireNonNull(owner, "owner");
            if (contributionClasses.containsKey(uuid)) {
                return this;
            }
            Map<String, RootClass> next =
                    new HashMap<>(contributionClasses);
            next.put(uuid, owner);
            return new RoomSnapshot(
                    room,
                    engagedPlayers,
                    secondWaveActivated,
                    spawnedEnemies,
                    remainingEnemies,
                    Map.copyOf(next)
            );
        }

        public RoomSnapshot defeat(int count) {
            if (count <= 0 || count > remainingEnemies) {
                throw new IllegalArgumentException(
                        "Defeated enemy count exceeds active room state."
                );
            }
            return new RoomSnapshot(
                    room,
                    engagedPlayers,
                    secondWaveActivated,
                    spawnedEnemies,
                    remainingEnemies - count,
                    contributionClasses
            );
        }

        public boolean complete() {
            return remainingEnemies == 0
                    && (room != RoomId.UPPER_GALLERY || secondWaveActivated);
        }
    }

    public record PendingAttribution(
            long runId,
            R01QuarryRunContribution contribution,
            RootClass owner
    ) {
        public static final Codec<PendingAttribution> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.LONG.fieldOf("run_id")
                                .forGetter(PendingAttribution::runId),
                        R01QuarryRunContribution.CODEC.fieldOf("contribution")
                                .forGetter(PendingAttribution::contribution),
                        RootClass.CODEC.fieldOf("owner")
                                .forGetter(PendingAttribution::owner)
                ).apply(instance, PendingAttribution::new));

        public PendingAttribution {
            requireRunId(runId);
            Objects.requireNonNull(contribution, "contribution");
            Objects.requireNonNull(owner, "owner");
        }
    }
}
