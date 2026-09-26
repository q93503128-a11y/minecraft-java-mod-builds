package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.progression.r01.R01QuarryRoomEncounterRules.RoomId;
import dev.moonseungjun.openworldrpg.progression.r01.R01QuarryRoomEncounterRules.SpawnPlan;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server authority for the non-spatial part of the three pre-boss R01 Quarry encounters.
 *
 * <p>Spatial adapters own room/threshold volumes and concrete donor entity spawning. This
 * controller owns sequence, authored counts, staggered activation, participant class capture,
 * room clear persistence and reconnect-safe Quarry attribution.</p>
 */
public final class R01QuarryRoomEncounterController {
    private R01QuarryRoomEncounterController() {
    }

    public static R01QuarryRoomEncounterState state(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        ServerLevel overworld = server.overworld();
        return overworld.getAttachedOrSet(
                R01QuarryRoomEncounterAttachments.QUARRY_ROOMS,
                R01QuarryRoomEncounterState.initial()
        );
    }

    public static StartResult beginRoom(
            ServerPlayer trigger,
            String encounterId,
            RoomId room,
            int engagedPlayers
    ) {
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(room, "room");
        MinecraftServer server = requireServer(trigger);
        R01PlayerState personal = R01PlayerStateService.state(trigger);
        if (personal.opening().mainStage() != R01MainStage.QUARRY_DUNGEON_ACTIVE
                || personal.quarry().runId() <= 0L
                || personal.quarry().runState().filter("active"::equals).isEmpty()) {
            return StartResult.rejected();
        }

        long runId = personal.quarry().runId();
        R01QuarryRoomEncounterState before = state(server);
        var existing = before.run(encounterId).orElse(null);
        if (existing != null
                && existing.runId() == runId
                && existing.clearedRooms().contains(room)) {
            return new StartResult(false, true, Optional.empty());
        }

        R01QuarryRoomEncounterState next = before.beginRoom(
                encounterId,
                runId,
                room,
                engagedPlayers
        );
        replace(server, next);
        if (before.equals(next)) {
            return new StartResult(false, false, Optional.empty());
        }
        return new StartResult(
                true,
                false,
                Optional.of(R01QuarryRoomEncounterRules.initialPlan(
                        room,
                        engagedPlayers
                ))
        );
    }

    public static Optional<SpawnPlan> activateUpperGallerySecondWave(
            MinecraftServer server,
            String encounterId,
            int engagedPlayers
    ) {
        Objects.requireNonNull(server, "server");
        R01QuarryRoomEncounterState before = state(server);
        R01QuarryRoomEncounterState next =
                before.activateUpperGallerySecondWave(
                        encounterId,
                        engagedPlayers
                );
        replace(server, next);
        if (before.equals(next)) {
            return Optional.empty();
        }
        return Optional.of(
                R01QuarryRoomEncounterRules.upperGallerySecondWave(
                        engagedPlayers
                )
        );
    }

    /**
     * Captures the class active on the first legal combat contribution in this room.
     * The attribution unit commits only if the room later clears.
     */
    public static boolean recordCombatContribution(
            ServerPlayer player,
            String encounterId
    ) {
        Objects.requireNonNull(player, "player");
        MinecraftServer server = requireServer(player);
        R01QuarryRoomEncounterState current = state(server);
        var run = current.run(encounterId).orElse(null);
        if (run == null) {
            return false;
        }

        R01PlayerState personal = R01PlayerStateService.state(player);
        if (personal.quarry().runId() != run.runId()
                || personal.quarry().runState().filter("active"::equals).isEmpty()) {
            return false;
        }

        RootClass activeClass = PlayerProgressionService.state(player)
                .activeClass()
                .orElse(null);
        if (activeClass == null) {
            return false;
        }

        String uuid = player.getUUID().toString();
        var activeRoom = run.activeRoom().orElse(null);
        if (activeRoom == null) {
            return false;
        }
        boolean already = activeRoom.contributionClasses().containsKey(uuid);

        R01QuarryRoomEncounterState next = current.recordParticipation(
                encounterId,
                uuid,
                activeClass
        );
        replace(server, next);
        return !already;
    }

    public static DefeatResult recordEnemyDefeat(
            MinecraftServer server,
            String encounterId,
            int defeatedCount
    ) {
        Objects.requireNonNull(server, "server");
        R01QuarryRoomEncounterState before = state(server);
        var runBefore = before.run(encounterId).orElseThrow(
                () -> new IllegalStateException("Unknown Quarry encounter.")
        );
        var roomBefore = runBefore.activeRoom().orElseThrow(
                () -> new IllegalStateException("No active Quarry room.")
        );

        R01QuarryRoomEncounterState next = before.recordEnemyDefeat(
                encounterId,
                defeatedCount
        );
        replace(server, next);

        var runAfter = next.run(encounterId).orElseThrow();
        boolean completedNow = runAfter.activeRoom().isEmpty()
                && runAfter.clearedRooms().contains(roomBefore.room());

        if (completedNow) {
            reconcileOnlineAttributions(server);
        }

        int remaining = runAfter.activeRoom()
                .map(R01QuarryRoomEncounterState.RoomSnapshot::remainingEnemies)
                .orElse(0);
        return new DefeatResult(roomBefore.room(), completedNow, remaining);
    }

    public static boolean resetCurrentUnclearedRoom(
            MinecraftServer server,
            String encounterId
    ) {
        Objects.requireNonNull(server, "server");
        R01QuarryRoomEncounterState before = state(server);
        R01QuarryRoomEncounterState next = before.resetActiveRoom(encounterId);
        replace(server, next);
        return !before.equals(next);
    }

    public static boolean resetEncounterCycle(
            MinecraftServer server,
            String encounterId
    ) {
        Objects.requireNonNull(server, "server");
        R01QuarryRoomEncounterState before = state(server);
        R01QuarryRoomEncounterState next = before.clearEncounter(encounterId);
        replace(server, next);
        return !before.equals(next);
    }

    public static int reconcilePendingAttributions(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        MinecraftServer server = requireServer(player);
        String uuid = player.getUUID().toString();
        List<R01QuarryRoomEncounterState.PendingAttribution> pending =
                List.copyOf(state(server).pendingFor(uuid));
        int reconciled = 0;

        for (var value : pending) {
            if (!R01QuarryRunAttributionService.ensureCapturedContribution(
                    player,
                    value.runId(),
                    value.contribution(),
                    value.owner()
            )) {
                continue;
            }
            R01QuarryRoomEncounterState next = state(server).clearPending(
                    uuid,
                    value
            );
            replace(server, next);
            reconciled++;
        }
        return reconciled;
    }

    private static void reconcileOnlineAttributions(MinecraftServer server) {
        for (String uuidText
                : new ArrayList<>(state(server).pendingAttributions().keySet())) {
            ServerPlayer player =
                    server.getPlayerList().getPlayer(UUID.fromString(uuidText));
            if (player != null) {
                reconcilePendingAttributions(player);
            }
        }
    }

    private static MinecraftServer requireServer(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            throw new IllegalStateException(
                    "R01 Quarry room encounter requires a live server."
            );
        }
        return server;
    }

    private static R01QuarryRoomEncounterState replace(
            MinecraftServer server,
            R01QuarryRoomEncounterState next
    ) {
        ServerLevel overworld = server.overworld();
        R01QuarryRoomEncounterState current = state(server);
        if (!current.equals(next)) {
            overworld.setAttached(
                    R01QuarryRoomEncounterAttachments.QUARRY_ROOMS,
                    next
            );
        }
        return next;
    }

    public record StartResult(
            boolean started,
            boolean alreadyCleared,
            Optional<SpawnPlan> spawnPlan
    ) {
        public StartResult {
            spawnPlan = Objects.requireNonNull(spawnPlan, "spawnPlan");
            if (started != spawnPlan.isPresent()) {
                throw new IllegalArgumentException(
                        "Started Quarry room must publish exactly one initial spawn plan."
                );
            }
            if (started && alreadyCleared) {
                throw new IllegalArgumentException(
                        "Cleared Quarry room cannot start again in the same run."
                );
            }
        }

        public static StartResult rejected() {
            return new StartResult(false, false, Optional.empty());
        }
    }

    public record DefeatResult(
            RoomId room,
            boolean completedNow,
            int remainingEnemies
    ) {
        public DefeatResult {
            Objects.requireNonNull(room, "room");
            if (remainingEnemies < 0) {
                throw new IllegalArgumentException(
                        "remainingEnemies must be non-negative."
                );
            }
        }
    }
}
