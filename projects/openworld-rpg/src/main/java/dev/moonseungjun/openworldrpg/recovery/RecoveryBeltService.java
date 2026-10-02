package dev.moonseungjun.openworldrpg.recovery;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-only mutation/read boundary for persistent Recovery Belt state.
 *
 * <p>Reserve inventory consumption is intentionally not faked here. Loading requires a caller that
 * has already committed the corresponding real-item transaction.</p>
 */
public final class RecoveryBeltService {
    private RecoveryBeltService() {
    }

    public static RecoveryBeltState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                RecoveryBeltAttachments.RECOVERY_BELT,
                RecoveryBeltState.empty()
        );
    }

    public static RecoveryBeltSetupState setup(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                RecoveryBeltAttachments.RECOVERY_BELT_SETUP,
                RecoveryBeltSetupState.empty()
        );
    }

    public static RecoveryBeltState selectSlot(ServerPlayer player, int slot) {
        return replace(player, state(player).withSelectedSlot(slot));
    }

    public static RecoveryBeltSetupState configureSetupSlot(
            ServerPlayer player,
            int slot,
            RecoveryBeltSlot desired
    ) {
        Objects.requireNonNull(desired, "desired");
        return replaceSetup(
                player,
                setup(player).withSlot(slot, desired)
        );
    }

    /**
     * Save-compatible migration for the pre-setup attachment era. It mirrors only doses that are
     * already physically loaded, so a legacy empty belt never receives an invented default ratio.
     */
    public static RecoveryBeltSetupState reconcileSetupFromLoadedBeltIfEmpty(
            ServerPlayer player
    ) {
        RecoveryBeltSetupState current = setup(player);
        if (!current.isEmpty()) {
            return current;
        }
        RecoveryBeltState belt = state(player);
        if (belt.loadedCount() == 0) {
            return current;
        }
        return replaceSetup(
                player,
                RecoveryBeltSetupState.fromLoadedBelt(belt)
        );
    }

    public static RecoveryBeltState loadCommittedReserveDoseOnce(
            ServerPlayer player,
            String transactionId,
            RecoveryConsumable consumable
    ) {
        return replace(
                player,
                state(player).loadCommittedReserveDoseOnce(transactionId, consumable)
        );
    }

    public static RecoveryBeltState.Resolution consumeSelectedAtResolution(
            ServerPlayer player,
            long nowTick
    ) {
        RecoveryBeltState.Resolution resolution =
                state(player).consumeSelectedAtResolution(nowTick);
        replace(player, resolution.state());
        return resolution;
    }

    public static RecoveryBeltState.Resolution consumeSlotAtResolution(
            ServerPlayer player,
            int slot,
            RecoveryConsumable expectedConsumable,
            long nowTick
    ) {
        RecoveryBeltState.Resolution resolution =
                state(player).consumeSlotAtResolution(slot, expectedConsumable, nowTick);
        replace(player, resolution.state());
        return resolution;
    }

    static RecoveryBeltState replace(
            ServerPlayer player,
            RecoveryBeltState next
    ) {
        RecoveryBeltState current = state(player);
        if (current.equals(next)) {
            return current;
        }
        player.setAttached(RecoveryBeltAttachments.RECOVERY_BELT, next);
        return next;
    }

    private static RecoveryBeltSetupState replaceSetup(
            ServerPlayer player,
            RecoveryBeltSetupState next
    ) {
        RecoveryBeltSetupState current = setup(player);
        if (current.equals(next)) {
            return current;
        }
        player.setAttached(
                RecoveryBeltAttachments.RECOVERY_BELT_SETUP,
                next
        );
        return next;
    }
}
