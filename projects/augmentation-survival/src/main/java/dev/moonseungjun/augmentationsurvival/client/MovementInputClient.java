package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.network.AirJumpHoldPayload;
import dev.moonseungjun.augmentationsurvival.network.AirJumpRequestPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Detects airborne jump input and mirrors held state for Aerial Combo auto-consumption. */
public final class MovementInputClient {
    private static final String DOUBLE_JUMP_ID = AugmentationSurvival.MOD_ID + ":double_jump";
    private static final String AERIAL_COMBO_ID = AugmentationSurvival.MOD_ID + ":aerial_combo";
    private static boolean jumpWasDown;
    private static boolean resetContactWasActive = true;

    private MovementInputClient() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                jumpWasDown = false;
                resetContactWasActive = true;
                return;
            }

            boolean inputAllowed = client.gui.screen() == null;
            boolean jumpDown = inputAllowed && client.options.keyJump.isDown();
            boolean resetContact = client.player.onGround()
                    || client.player.isInWater()
                    || client.player.onClimbable();
            boolean unlocked = ClientAugmentProgressState.level(DOUBLE_JUMP_ID) > 0
                    || ClientAugmentProgressState.level(AERIAL_COMBO_ID) > 0;

            if (jumpDown != jumpWasDown) {
                ClientPlayNetworking.send(new AirJumpHoldPayload(jumpDown));
            }

            // The previous-contact guard prevents the initial ground/water/climb jump press from
            // being reinterpreted as the air jump after client movement clears the contact flag.
            if (unlocked
                    && jumpDown
                    && !jumpWasDown
                    && !resetContact
                    && !resetContactWasActive) {
                ClientPlayNetworking.send(new AirJumpRequestPayload(true));
            }

            jumpWasDown = jumpDown;
            resetContactWasActive = resetContact;
        });
    }
}
