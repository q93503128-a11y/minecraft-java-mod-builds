package dev.moonseungjun.augmentationsurvival.augment.utility;

import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Server authority for the dedicated Instant Mining toggle key. */
public final class InstantMiningToggleService {
    private InstantMiningToggleService() { }

    public static boolean toggle(ServerPlayer player) {
        if (player == null || player.isDeadOrDying() || player.isSpectator()) return false;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (state.level(UtilityAugmentService.INSTANT_MINING_ID) <= 0) return false;

        boolean enabled = state.persistentInt(UtilityAugmentService.INSTANT_MINING_TOGGLE_KEY) <= 0;
        AugmentationPlayerStateService.replace(
                player,
                state.withPersistentInt(UtilityAugmentService.INSTANT_MINING_TOGGLE_KEY, enabled ? 1 : 0)
        );
        player.sendSystemMessage(Component.translatable(enabled
                ? "message.augmentation_survival.instant_mining.enabled"
                : "message.augmentation_survival.instant_mining.disabled"));
        return true;
    }
}
