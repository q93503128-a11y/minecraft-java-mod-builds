package dev.moonseungjun.openworldrpg.travel;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerActionRuntime;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerActionRuntimeState;
import dev.moonseungjun.openworldrpg.multiplayer.ProjectDownedRuntime;
import dev.moonseungjun.openworldrpg.progression.r01.R01PlayerState;
import dev.moonseungjun.openworldrpg.progression.r01.R01PlayerStateService;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Server-authoritative R01 node-to-node fast travel.
 *
 * <p>The client never supplies coordinates. Downed state is resolved from the project server
 * runtime; the caller's legacy block-state flag can only over-block, never bypass it. Coordinates
 * come exclusively from {@link R01FastTravelNodeRegistry}'s accepted production bindings.</p>
 */
public final class R01FastTravelService {
    public static final double ORIGIN_RADIUS = 6.0;
    public static final int CHANNEL_TICKS = 20;
    private static final String ACTION_ID =
            "openworld_rpg:action/fast_travel";

    private static final Map<UUID, ActiveTravel> ACTIVE =
            new ConcurrentHashMap<>();

    private R01FastTravelService() {
    }

    public static StartResult tryStart(
            ServerPlayer player,
            String originNodeId,
            String destinationNodeId,
            BlockState authoritativeBlockState
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(originNodeId, "originNodeId");
        Objects.requireNonNull(destinationNodeId, "destinationNodeId");
        Objects.requireNonNull(
                authoritativeBlockState,
                "authoritativeBlockState"
        );

        UUID playerId = player.getUUID();
        ActiveTravel existing = ACTIVE.get(playerId);
        if (existing != null) {
            return new StartResult(
                    StartStatus.ALREADY_CHANNELING,
                    Optional.of(existing)
            );
        }
        if (!player.isAlive()) {
            return rejected(StartStatus.INVALID_STATE);
        }
        if (ProjectDownedRuntime.isDowned(player)
                || authoritativeBlockState.downed()) {
            return rejected(StartStatus.DOWNED);
        }
        if (authoritativeBlockState.projectMounted()
                || player.isPassenger()) {
            return rejected(StartStatus.MOUNTED);
        }
        if (originNodeId.equals(destinationNodeId)) {
            return rejected(StartStatus.SAME_NODE);
        }

        var origin = R01FastTravelNodeRegistry
                .node(originNodeId)
                .orElse(null);
        var destination = R01FastTravelNodeRegistry
                .node(destinationNodeId)
                .orElse(null);
        if (origin == null || destination == null) {
            return rejected(StartStatus.NODE_NOT_PRODUCTION);
        }
        if (!Level.OVERWORLD.equals(player.level().dimension())) {
            return rejected(StartStatus.WRONG_DIMENSION);
        }

        R01PlayerState personalState =
                R01PlayerStateService.state(player);
        if (!personallyActivated(
                personalState,
                origin.personalUnlock()
        )) {
            return rejected(StartStatus.ORIGIN_NOT_ACTIVATED);
        }
        if (!personallyActivated(
                personalState,
                destination.personalUnlock()
        )) {
            return rejected(StartStatus.DESTINATION_NOT_ACTIVATED);
        }
        if (!withinOriginRadius(
                player.position(),
                origin.interactionAnchor()
        )) {
            return rejected(StartStatus.TOO_FAR_FROM_ORIGIN);
        }

        long nowTick = player.level().getGameTime();
        if (CombatStateServices.states()
                .getOrCreate(playerId, nowTick)
                .isCombatActive(nowTick)) {
            return rejected(StartStatus.IN_COMBAT);
        }

        var begin = ProjectPlayerActionRuntime.beginAction(
                player,
                new ProjectPlayerActionRuntime.ActionSpec(
                        ACTION_ID,
                        CHANNEL_TICKS,
                        CHANNEL_TICKS,
                        1.0
                )
        );
        if (!begin.accepted()) {
            return rejected(StartStatus.BUSY);
        }

        ActiveTravel active = new ActiveTravel(
                originNodeId,
                destinationNodeId,
                nowTick,
                begin.endTick()
        );
        ACTIVE.put(playerId, active);
        return new StartResult(
                StartStatus.STARTED,
                Optional.of(active)
        );
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");

        for (ServerPlayer player
                : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            ActiveTravel active = ACTIVE.get(playerId);
            if (active == null) {
                continue;
            }

            long nowTick = player.level().getGameTime();
            if (!player.isAlive()
                    || ProjectDownedRuntime.isDowned(player)
                    || player.isPassenger()
                    || CombatStateServices.states()
                            .getOrCreate(playerId, nowTick)
                            .isCombatActive(nowTick)) {
                cancel(player, active);
                continue;
            }

            if (nowTick < active.completeAtTick()) {
                PlayerActionRuntimeState.Snapshot snapshot =
                        ProjectPlayerActionRuntime.snapshot(
                                playerId,
                                nowTick
                        );
                if (!snapshot.active()
                        || snapshot.kind()
                                != PlayerActionRuntimeState.WindowKind.ACTION
                        || !ACTION_ID.equals(snapshot.actionId())) {
                    ACTIVE.remove(playerId, active);
                }
                continue;
            }

            var destination = R01FastTravelNodeRegistry
                    .node(active.destinationNodeId())
                    .orElse(null);
            if (destination == null
                    || !Level.OVERWORLD.equals(player.level().dimension())
                    || !personallyActivated(
                            R01PlayerStateService.state(player),
                            destination.personalUnlock()
                    )) {
                cancel(player, active);
                continue;
            }

            Optional<Vec3> arrival =
                    firstSafeArrival(player, destination);
            if (arrival.isEmpty()) {
                cancel(player, active);
                continue;
            }

            Vec3 target = arrival.orElseThrow();
            player.teleportTo(
                    target.x(),
                    target.y(),
                    target.z()
            );
            ACTIVE.remove(playerId, active);
        }
    }

    public static void reset(UUID playerId) {
        ACTIVE.remove(
                Objects.requireNonNull(playerId, "playerId")
        );
    }

    public static void disconnect(UUID playerId) {
        reset(playerId);
    }

    static boolean personallyActivated(
            R01PlayerState state,
            R01FastTravelNodeRegistry.PersonalUnlock unlock
    ) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(unlock, "unlock");
        return switch (unlock) {
            case ALDERFORD_GATE_SHRINE ->
                    state.opening().firstShrineActivated();
            case QUARRY_WAYSTONE ->
                    state.quarry().waystoneActivated();
        };
    }

    static boolean withinOriginRadius(
            Vec3 playerPosition,
            Vec3 anchor
    ) {
        Objects.requireNonNull(playerPosition, "playerPosition");
        Objects.requireNonNull(anchor, "anchor");
        return playerPosition.distanceToSqr(anchor)
                <= ORIGIN_RADIUS * ORIGIN_RADIUS;
    }

    private static Optional<Vec3> firstSafeArrival(
            ServerPlayer player,
            R01FastTravelNodeRegistry.ProductionNode destination
    ) {
        ServerLevel level = (ServerLevel) player.level();
        for (Vec3 candidate : destination.arrivalPoints()) {
            level.getChunkAt(BlockPos.containing(candidate));

            double dx = candidate.x() - player.getX();
            double dy = candidate.y() - player.getY();
            double dz = candidate.z() - player.getZ();
            AABB arrivalBox = player.getBoundingBox()
                    .move(dx, dy, dz);

            if (!level.noCollision(player, arrivalBox)) {
                continue;
            }
            /*
             * A slight downward probe must collide. This rejects authored coordinates suspended
             * over empty space while remaining tolerant of slabs/stairs and other legal ground.
             */
            if (level.noCollision(
                    player,
                    arrivalBox.move(0.0, -0.125, 0.0)
            )) {
                continue;
            }
            return Optional.of(candidate);
        }
        return Optional.empty();
    }

    private static void cancel(
            ServerPlayer player,
            ActiveTravel active
    ) {
        ACTIVE.remove(player.getUUID(), active);
        ProjectPlayerActionRuntime.cancelAction(
                player,
                ACTION_ID
        );
    }

    private static StartResult rejected(StartStatus status) {
        return new StartResult(status, Optional.empty());
    }

    public record BlockState(
            boolean downed,
            boolean projectMounted
    ) {
        public static BlockState clear() {
            return new BlockState(false, false);
        }
    }

    public enum StartStatus {
        STARTED,
        ALREADY_CHANNELING,
        INVALID_STATE,
        DOWNED,
        MOUNTED,
        SAME_NODE,
        NODE_NOT_PRODUCTION,
        WRONG_DIMENSION,
        ORIGIN_NOT_ACTIVATED,
        DESTINATION_NOT_ACTIVATED,
        TOO_FAR_FROM_ORIGIN,
        IN_COMBAT,
        BUSY;

        public boolean accepted() {
            return this == STARTED;
        }
    }

    public record StartResult(
            StartStatus status,
            Optional<ActiveTravel> activeTravel
    ) {
        public StartResult {
            Objects.requireNonNull(status, "status");
            activeTravel = Objects.requireNonNull(
                    activeTravel,
                    "activeTravel"
            );
            if (status.accepted() != activeTravel.isPresent()) {
                throw new IllegalArgumentException(
                        "Fast-travel start status/action mismatch."
                );
            }
        }
    }

    public record ActiveTravel(
            String originNodeId,
            String destinationNodeId,
            long startedAtTick,
            long completeAtTick
    ) {
        public ActiveTravel {
            if (originNodeId == null
                    || originNodeId.isBlank()
                    || destinationNodeId == null
                    || destinationNodeId.isBlank()) {
                throw new IllegalArgumentException(
                        "Fast-travel node ids must be present."
                );
            }
            if (startedAtTick < 0L
                    || completeAtTick
                            != startedAtTick + CHANNEL_TICKS) {
                throw new IllegalArgumentException(
                        "Fast-travel channel timing mismatch."
                );
            }
        }
    }
}
