package dev.moonseungjun.augmentationsurvival.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.client.screen.AuthorityCollectionScreen;
import dev.moonseungjun.augmentationsurvival.network.AuthorityCollectionPayload;
import dev.moonseungjun.augmentationsurvival.network.AuthorityReleasePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** J releases acquired powers; Shift+J safely inspects server-owned collection without casting. */
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
        ClientPlayNetworking.registerGlobalReceiver(AuthorityCollectionPayload.TYPE, (payload, context) -> {
            var client = context.client();
            if (client.player != null && ClientAugmentProgressState.level(AUTHORITY_THEFT_ID) > 0) {
                client.gui.setScreen(new AuthorityCollectionScreen(payload));
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean down = client.player != null && client.gui.screen() == null && AUTHORITY_KEY.isDown();
            boolean unlocked = ClientAugmentProgressState.level(AUTHORITY_THEFT_ID) > 0;
            if (down && !wasDown && unlocked) {
                boolean inspect = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                        || InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
                ClientPlayNetworking.send(new AuthorityReleasePayload(!inspect));
            }
            wasDown = down;
        });
    }
}
