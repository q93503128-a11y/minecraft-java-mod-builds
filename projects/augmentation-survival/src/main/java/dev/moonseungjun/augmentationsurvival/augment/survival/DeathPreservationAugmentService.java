package dev.moonseungjun.augmentationsurvival.augment.survival;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;

/** Actual-death inventory preservation. Death prevention never enters this boundary. */
public final class DeathPreservationAugmentService {
    private static final String KEEP_INVENTORY_ID = id("keep_inventory");

    private DeathPreservationAugmentService() {
    }

    public static void initialize() {
        AugmentCatalog.require(KEEP_INVENTORY_ID);
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (!alive && shouldKeepInventory(oldPlayer)) {
                newPlayer.getInventory().replaceWith(oldPlayer.getInventory());
            }
        });
    }

    public static boolean shouldKeepInventory(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = state.level(KEEP_INVENTORY_ID);
        return level > 0 && level <= AugmentCatalog.require(KEEP_INVENTORY_ID).maxLevel();
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }
}
