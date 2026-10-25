package dev.moonseungjun.augmentationsurvival.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.client.screen.ActiveLoadoutScreen;
import dev.moonseungjun.augmentationsurvival.network.ActiveAbilityRequestPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Hold the augment skill-mode key and press 1..0 to request active slots 1..10. */
public final class ActiveAbilityInputClient {
    static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "augmentation")
    );
    private static final KeyMapping MODE_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.augmentation_survival.active_mode", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY
    ));
    private static final KeyMapping LOADOUT_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.augmentation_survival.active_loadout", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY
    ));
    private static final int[] SLOT_KEYS = {
            InputConstants.KEY_1, InputConstants.KEY_2, InputConstants.KEY_3, InputConstants.KEY_4,
            InputConstants.KEY_5, InputConstants.KEY_6, InputConstants.KEY_7, InputConstants.KEY_8,
            InputConstants.KEY_9, InputConstants.KEY_0
    };
    private static final boolean[] SLOT_WAS_DOWN = new boolean[SLOT_KEYS.length];
    private static boolean loadoutWasDown;

    private ActiveAbilityInputClient() { }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean loadoutDown = client.player != null && LOADOUT_KEY.isDown();
            if (client.player != null && client.gui.screen() == null && loadoutDown && !loadoutWasDown) {
                client.gui.setScreen(new ActiveLoadoutScreen());
            }
            loadoutWasDown = loadoutDown;

            if (client.player == null || client.gui.screen() != null) {
                reset(client);
                return;
            }
            boolean modeDown = MODE_KEY.isDown();
            for (int index = 0; index < SLOT_KEYS.length; index++) {
                boolean down = InputConstants.isKeyDown(client.getWindow(), SLOT_KEYS[index]);
                if (modeDown && down && !SLOT_WAS_DOWN[index]) {
                    ClientPlayNetworking.send(new ActiveAbilityRequestPayload(index));
                }
                SLOT_WAS_DOWN[index] = down;
            }
        });
    }

    private static void reset(net.minecraft.client.Minecraft client) {
        for (int index = 0; index < SLOT_KEYS.length; index++) {
            SLOT_WAS_DOWN[index] = client.player != null && InputConstants.isKeyDown(client.getWindow(), SLOT_KEYS[index]);
        }
    }
}
