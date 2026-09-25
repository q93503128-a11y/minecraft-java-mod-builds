package dev.moonseungjun.openworldrpg.recovery;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-owned quick-recovery action lifecycle.
 *
 * <p>The future accepted client motion/input adapter may request this action, but never owns timing
 * or consumption. Until that presentation binding exists, this class is intentionally not exposed
 * through a placeholder player-facing key path.</p>
 */
public final class RecoveryUseRuntime {
    private static final ConcurrentHashMap<UUID, RecoveryUseActionState> ACTIVE =
            new ConcurrentHashMap<>();

    private RecoveryUseRuntime() {
    }

    public static StartResult tryStart(ServerPlayer player, UseContext context) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(context, "context");
        UUID playerId = player.getUUID();
        long nowTick = player.level().getGameTime();

        RecoveryUseActionState existing = ACTIVE.get(playerId);
        if (existing != null && !existing.isComplete(nowTick)) {
            return new StartResult(StartStatus.ALREADY_ACTIVE, Optional.of(existing));
        }
        if (existing != null) {
            ACTIVE.remove(playerId, existing);
        }

        if (!player.isAlive() || !context.eligible()) {
            return new StartResult(StartStatus.INVALID_STATE, Optional.empty());
        }

        RecoveryBeltState belt = RecoveryBeltService.state(player);
        if (belt.isLockedOut(nowTick)) {
            return new StartResult(StartStatus.LOCKED_OUT, Optional.empty());
        }
        Optional<RecoveryConsumable> selected = belt.selectedConsumable();
        if (selected.isEmpty()) {
            return new StartResult(StartStatus.EMPTY_SLOT, Optional.empty());
        }

        RecoveryUseActionState action = new RecoveryUseActionState(
                belt.selectedSlot(),
                selected.orElseThrow(),
                nowTick,
                false
        );
        ACTIVE.put(playerId, action);
        return new StartResult(StartStatus.STARTED, Optional.of(action));
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            RecoveryUseActionState action = ACTIVE.get(playerId);
            if (action == null) {
                continue;
            }

            long nowTick = player.level().getGameTime();
            if (!action.resolved() && !player.isAlive()) {
                ACTIVE.remove(playerId, action);
                continue;
            }

            if (action.shouldResolve(nowTick)) {
                try {
                    RecoveryBeltState.Resolution resolution =
                            RecoveryBeltService.consumeSlotAtResolution(
                                    player,
                                    action.slot(),
                                    action.consumable(),
                                    nowTick
                            );
                    RecoveryEffectRuntime.apply(
                            player,
                            resolution.consumable(),
                            nowTick
                    );
                    RecoveryUseActionState resolved = action.markResolved(nowTick);
                    ACTIVE.replace(playerId, action, resolved);
                    action = resolved;
                } catch (IllegalStateException exception) {
                    ACTIVE.remove(playerId, action);
                    continue;
                }
            }

            if (action.isComplete(nowTick)) {
                ACTIVE.remove(playerId, action);
            }
        }
    }

    public static boolean cancelPreResolution(
            ServerPlayer player,
            long nowTick
    ) {
        Objects.requireNonNull(player, "player");
        RecoveryUseActionState action = ACTIVE.get(player.getUUID());
        if (action == null || !action.isPreResolution(nowTick)) {
            return false;
        }
        return ACTIVE.remove(player.getUUID(), action);
    }

    public static boolean blocksDefense(UUID playerId, long nowTick) {
        RecoveryUseActionState action = ACTIVE.get(playerId);
        return action != null && action.isPreResolution(nowTick);
    }

    public static boolean actionActive(UUID playerId, long nowTick) {
        RecoveryUseActionState action = ACTIVE.get(playerId);
        return action != null && !action.isComplete(nowTick);
    }

    public static double movementMultiplier(UUID playerId, long nowTick) {
        return actionActive(playerId, nowTick)
                ? RecoveryActionRules.ACTION_MOVEMENT_MULTIPLIER
                : 1.0;
    }

    public static Optional<RecoveryUseActionState> action(UUID playerId) {
        return Optional.ofNullable(ACTIVE.get(playerId));
    }

    public static void disconnect(UUID playerId) {
        ACTIVE.remove(playerId);
    }

    public enum StartStatus {
        STARTED,
        ALREADY_ACTIVE,
        INVALID_STATE,
        LOCKED_OUT,
        EMPTY_SLOT
    }

    public record StartResult(
            StartStatus status,
            Optional<RecoveryUseActionState> action
    ) {
        public StartResult {
            Objects.requireNonNull(status, "status");
            action = Objects.requireNonNull(action, "action");
            if ((status == StartStatus.STARTED || status == StartStatus.ALREADY_ACTIVE)
                    != action.isPresent()) {
                throw new IllegalArgumentException(
                        "Recovery start result/action presence is inconsistent."
                );
            }
        }
    }

    public record UseContext(
            boolean downed,
            boolean mounted,
            boolean climbing,
            boolean incompatibleCommittedAction
    ) {
        public boolean eligible() {
            return !downed
                    && !mounted
                    && !climbing
                    && !incompatibleCommittedAction;
        }

        public static UseContext clear() {
            return new UseContext(false, false, false, false);
        }
    }
}
