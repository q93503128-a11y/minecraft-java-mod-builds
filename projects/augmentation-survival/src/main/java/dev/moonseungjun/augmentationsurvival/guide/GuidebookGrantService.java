package dev.moonseungjun.augmentationsurvival.guide;

import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Grants the crafting guide once per persistent player state. */
public final class GuidebookGrantService {
    private static final String GRANT_KEY = "guidebook_grant_v1";

    private GuidebookGrantService() {
    }

    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> grantIfNeeded(listener.player));
    }

    private static void grantIfNeeded(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (state.persistentInt(GRANT_KEY) > 0) return;

        ItemStack guide = new ItemStack(GuidebookItems.GUIDEBOOK);
        if (!player.getInventory().add(guide)) {
            player.drop(guide, false);
        }
        AugmentationPlayerStateService.update(player, current -> current.withPersistentInt(GRANT_KEY, 1));
    }
}
