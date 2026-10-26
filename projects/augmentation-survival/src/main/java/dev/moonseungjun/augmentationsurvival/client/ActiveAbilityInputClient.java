package dev.moonseungjun.augmentationsurvival.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.AugmentPresentationCatalog;
import dev.moonseungjun.augmentationsurvival.client.screen.ActiveLoadoutScreen;
import dev.moonseungjun.augmentationsurvival.network.ActiveAbilityRequestPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Toggle augment skill mode, then consume 1..0 as active-slot input instead of also changing hotbar slots. */
public final class ActiveAbilityInputClient {
    static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "augmentation")
    );
    private static final String AUTHORITY_THEFT_ID = AugmentationSurvival.MOD_ID + ":authority_theft";
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
    private static boolean modeEnabled;

    private ActiveAbilityInputClient() { }

    public static void initialize() {
        // START_CLIENT_TICK runs before vanilla drains hotbar key clicks. Raw 1..0 edge detection
        // preserves slot 10, while draining the nine vanilla hotbar mappings prevents dual input.
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            boolean hasPlayer = client.player != null;
            boolean hasActive = hasPlayer && hasLoadoutActive();

            if (!hasPlayer || client.player.isDeadOrDying() || !hasActive) {
                modeEnabled = false;
                drain(MODE_KEY);
                drain(LOADOUT_KEY);
                rememberSlotState(client);
                return;
            }

            if (client.gui.screen() != null) {
                drain(MODE_KEY);
                drain(LOADOUT_KEY);
                rememberSlotState(client);
                return;
            }

            while (LOADOUT_KEY.consumeClick()) {
                client.gui.setScreen(new ActiveLoadoutScreen());
            }
            if (client.gui.screen() != null) {
                rememberSlotState(client);
                return;
            }

            while (MODE_KEY.consumeClick()) {
                modeEnabled = !modeEnabled;
            }

            for (int index = 0; index < SLOT_KEYS.length; index++) {
                boolean down = InputConstants.isKeyDown(client.getWindow(), SLOT_KEYS[index]);
                if (modeEnabled && down && !SLOT_WAS_DOWN[index]) {
                    ClientPlayNetworking.send(new ActiveAbilityRequestPayload(index));
                }
                SLOT_WAS_DOWN[index] = down;
            }

            if (modeEnabled) {
                for (KeyMapping hotbarKey : client.options.keyHotbarSlots) {
                    drain(hotbarKey);
                }
            }
        });
    }

    /** Existing HUD hook name retained for compatibility; now means toggled skill mode is enabled. */
    public static boolean isModeDown() {
        return hasLoadoutActive() && modeEnabled;
    }

    private static boolean hasLoadoutActive() {
        for (String id : ClientAugmentProgressState.levels().keySet()) {
            if (AUTHORITY_THEFT_ID.equals(id)) continue;
            try {
                AugmentDefinition definition = AugmentCatalog.require(id);
                if (AugmentPresentationCatalog.get(definition).kind() != AugmentPresentationCatalog.Kind.PASSIVE) {
                    return true;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return false;
    }

    private static void rememberSlotState(net.minecraft.client.Minecraft client) {
        for (int index = 0; index < SLOT_KEYS.length; index++) {
            SLOT_WAS_DOWN[index] = client.player != null
                    && InputConstants.isKeyDown(client.getWindow(), SLOT_KEYS[index]);
        }
    }

    private static void drain(KeyMapping mapping) {
        while (mapping.consumeClick()) {
            // Intentionally discard stale clicks when the key cannot be used in the current state.
        }
    }
}
