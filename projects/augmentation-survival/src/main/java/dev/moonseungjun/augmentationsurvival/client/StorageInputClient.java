package dev.moonseungjun.augmentationsurvival.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.network.StorageOpenRequestPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Rebindable key for opening Pockets / Remote Storage. */
public final class StorageInputClient {
    private static final String POCKETS_ID = AugmentationSurvival.MOD_ID + ":pockets";
    private static final String REMOTE_STORAGE_ID = AugmentationSurvival.MOD_ID + ":remote_storage";
    private static final KeyMapping OPEN_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.augmentation_survival.storage",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            ActiveAbilityInputClient.CATEGORY
    ));
    private static boolean wasDown;

    private StorageInputClient() { }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean down = client.player != null && client.gui.screen() == null && OPEN_KEY.isDown();
            boolean unlocked = ClientAugmentProgressState.level(POCKETS_ID) > 0
                    || ClientAugmentProgressState.level(REMOTE_STORAGE_ID) > 0;
            if (down && !wasDown && unlocked) ClientPlayNetworking.send(new StorageOpenRequestPayload(true));
            wasDown = down;
        });
    }
}
