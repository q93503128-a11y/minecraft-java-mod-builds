package dev.moonseungjun.augmentationsurvival.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.network.AuthorityReleasePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Rebindable dedicated key; Authority Theft never occupies one of the ten normal active slots. */
public final class AuthorityTheftInputClient {
    private static final String AUTHORITY_THEFT_ID = AugmentationSurvival.MOD_ID + ":authority_theft";
    private static final KeyMapping AUTHORITY_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.augmentation_survival.authority_theft",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            ActiveAbilityInputClient.CATEGORY
    ));
    private static boolean wasDown;

    private AuthorityTheftInputClient() { }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean down = client.player != null && client.gui.screen() == null && AUTHORITY_KEY.isDown();
            boolean unlocked = ClientAugmentProgressState.level(AUTHORITY_THEFT_ID) > 0;
            if (down && !wasDown && unlocked) ClientPlayNetworking.send(new AuthorityReleasePayload(true));
            wasDown = down;
        });
    }
}
