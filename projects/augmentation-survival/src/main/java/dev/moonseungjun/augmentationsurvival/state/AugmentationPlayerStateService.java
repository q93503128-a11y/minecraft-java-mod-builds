package dev.moonseungjun.augmentationsurvival.state;

import java.util.Objects;
import java.util.function.UnaryOperator;
import net.minecraft.server.level.ServerPlayer;

/** Server-only mutation boundary for persistent augmentation player state. */
public final class AugmentationPlayerStateService {
    private AugmentationPlayerStateService() {
    }

    public static AugmentationPlayerState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                AugmentationAttachments.PLAYER_STATE,
                AugmentationPlayerState.initial()
        );
    }

    public static AugmentationPlayerState update(
            ServerPlayer player,
            UnaryOperator<AugmentationPlayerState> operation
    ) {
        Objects.requireNonNull(operation, "operation");
        return replace(player, operation.apply(state(player)));
    }

    public static AugmentationPlayerState replace(
            ServerPlayer player,
            AugmentationPlayerState next
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(next, "next");
        AugmentationPlayerState current = state(player);
        if (current.equals(next)) {
            return current;
        }
        player.setAttached(AugmentationAttachments.PLAYER_STATE, next);
        return next;
    }
}
