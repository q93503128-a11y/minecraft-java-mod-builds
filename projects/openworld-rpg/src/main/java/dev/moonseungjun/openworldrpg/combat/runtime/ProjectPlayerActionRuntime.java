package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.state.PlayerActionRuntimeState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerMovementRuntime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shared server action/reaction commitment runtime.
 *
 * <p>Skills and future weapon actions publish commitment here. Dodge/basic-attack authority consumes
 * the same state so every class does not invent its own recovery/cancel gate.</p>
 */
public final class ProjectPlayerActionRuntime {
    public static final int DODGE_BUFFER_TICKS = 2;

    private static final ConcurrentHashMap<UUID, PlayerActionRuntimeState>
            STATES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Double>
            APPLIED_MOVEMENT_MULTIPLIERS = new ConcurrentHashMap<>();

    private ProjectPlayerActionRuntime() {
    }

    public static boolean canStartAction(
            ServerPlayer player
    ) {
        Objects.requireNonNull(player, "player");
        return state(player.getUUID())
                .canStartAction(
                        player.level().getGameTime()
                );
    }

    public static PlayerActionRuntimeState.BeginResult beginAction(
            ServerPlayer player,
            ActionSpec spec
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(spec, "spec");
        long nowTick = player.level().getGameTime();
        var result = state(player.getUUID()).beginAction(
                spec.actionId(),
                nowTick,
                spec.totalTicks(),
                spec.dodgeCancelOffsetTicks(),
                spec.movementMultiplier()
        );
        if (result.accepted()) {
            synchronizeMovement(
                    player,
                    spec.movementMultiplier()
            );
        }
        return result;
    }

    public static boolean canDodgeCancel(
            UUID playerId,
            long nowTick
    ) {
        Objects.requireNonNull(playerId, "playerId");
        var state = STATES.get(playerId);
        return state == null
                || state.canDodgeCancel(nowTick);
    }

    public static boolean canBufferDodge(
            UUID playerId,
            long nowTick
    ) {
        Objects.requireNonNull(playerId, "playerId");
        var state = STATES.get(playerId);
        if (state == null) {
            return false;
        }
        var snapshot = state.snapshot(nowTick);
        return snapshot.active()
                && snapshot.kind()
                        == PlayerActionRuntimeState.WindowKind.ACTION
                && nowTick < snapshot.dodgeCancelAtTick()
                && snapshot.dodgeCancelAtTick() - nowTick
                        <= DODGE_BUFFER_TICKS;
    }

    public static boolean commitDodgeCancel(
            UUID playerId,
            long nowTick
    ) {
        Objects.requireNonNull(playerId, "playerId");
        var state = STATES.get(playerId);
        return state == null
                || state.commitDodgeCancel(nowTick);
    }

    public static boolean cancelAction(
            ServerPlayer player,
            String actionId
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(actionId, "actionId");
        var state = STATES.get(player.getUUID());
        if (state == null) {
            return false;
        }
        boolean canceled = state.cancelAction(
                actionId,
                player.level().getGameTime()
        );
        if (canceled) {
            synchronizeMovement(player, 1.0);
        }
        return canceled;
    }

    public static boolean basicAttackAllowed(
            UUID playerId,
            long nowTick
    ) {
        Objects.requireNonNull(playerId, "playerId");
        var state = STATES.get(playerId);
        return state == null
                || state.basicAttackAllowed(nowTick);
    }

    public static boolean guardStartAllowed(
            UUID playerId,
            long nowTick
    ) {
        Objects.requireNonNull(playerId, "playerId");
        var state = STATES.get(playerId);
        return state == null
                || state.canStartAction(nowTick);
    }

    public static PlayerActionRuntimeState.ReactionResult applyReaction(
            ServerPlayer player,
            String reactionId,
            int durationTicks
    ) {
        Objects.requireNonNull(player, "player");
        var result = state(player.getUUID()).applyReaction(
                reactionId,
                player.level().getGameTime(),
                durationTicks
        );
        synchronizeMovement(player, 1.0);
        return result;
    }

    public static boolean hardReactionActive(
            UUID playerId,
            long nowTick
    ) {
        Objects.requireNonNull(playerId, "playerId");
        var state = STATES.get(playerId);
        return state != null
                && state.hardReactionActive(nowTick);
    }

    public static PlayerActionRuntimeState.Snapshot snapshot(
            UUID playerId,
            long nowTick
    ) {
        Objects.requireNonNull(playerId, "playerId");
        var state = STATES.get(playerId);
        return state == null
                ? new PlayerActionRuntimeState().snapshot(nowTick)
                : state.snapshot(nowTick);
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        for (ServerPlayer player
                : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            var state = STATES.get(playerId);
            double multiplier = state == null
                    ? 1.0
                    : state.movementMultiplier(
                            player.level().getGameTime()
                    );
            synchronizeMovement(player, multiplier);
        }
    }

    public static void reset(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        STATES.remove(player.getUUID());
        synchronizeMovement(player, 1.0);
    }

    public static void disconnect(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        STATES.remove(playerId);
        APPLIED_MOVEMENT_MULTIPLIERS.remove(playerId);
    }

    static PlayerActionRuntimeState state(UUID playerId) {
        return STATES.computeIfAbsent(
                Objects.requireNonNull(playerId, "playerId"),
                ignored -> new PlayerActionRuntimeState()
        );
    }

    private static void synchronizeMovement(
            ServerPlayer player,
            double multiplier
    ) {
        UUID playerId = player.getUUID();
        double previous = APPLIED_MOVEMENT_MULTIPLIERS
                .getOrDefault(playerId, 1.0);
        if (Double.compare(previous, multiplier) == 0) {
            return;
        }
        PlayerMovementRuntime.synchronizeActionMovementMultiplier(
                player,
                multiplier
        );
        if (Double.compare(multiplier, 1.0) == 0) {
            APPLIED_MOVEMENT_MULTIPLIERS.remove(playerId);
        } else {
            APPLIED_MOVEMENT_MULTIPLIERS.put(
                    playerId,
                    multiplier
            );
        }
    }

    public record ActionSpec(
            String actionId,
            int totalTicks,
            int dodgeCancelOffsetTicks,
            double movementMultiplier
    ) {
        public ActionSpec {
            Objects.requireNonNull(actionId, "actionId");
            if (actionId.isBlank()
                    || totalTicks <= 0
                    || dodgeCancelOffsetTicks < 0
                    || dodgeCancelOffsetTicks > totalTicks
                    || !Double.isFinite(movementMultiplier)
                    || movementMultiplier <= 0.0
                    || movementMultiplier > 1.0) {
                throw new IllegalArgumentException(
                        "Invalid project action spec."
                );
            }
        }

        public static int cancelOffset(
                int totalTicks,
                double fraction
        ) {
            if (totalTicks <= 0
                    || !Double.isFinite(fraction)
                    || fraction < 0.0
                    || fraction > 1.0) {
                throw new IllegalArgumentException(
                        "Invalid dodge-cancel timing."
                );
            }
            return Math.min(
                    totalTicks,
                    (int) Math.ceil(totalTicks * fraction)
            );
        }
    }
}
