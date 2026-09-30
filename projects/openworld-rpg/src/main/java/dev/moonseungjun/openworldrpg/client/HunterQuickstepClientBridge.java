package dev.moonseungjun.openworldrpg.client;

import dev.moonseungjun.openworldrpg.network.HunterQuickstepIntentPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class HunterQuickstepClientBridge {
    private static long nextSequence;

    private HunterQuickstepClientBridge() {
    }

    public static void publishMovementIntent(LocalPlayer player) {
        if (player == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        float forward = (client.options.keyUp.isDown() ? 1.0F : 0.0F)
                - (client.options.keyDown.isDown() ? 1.0F : 0.0F);
        float strafe = (client.options.keyRight.isDown() ? 1.0F : 0.0F)
                - (client.options.keyLeft.isDown() ? 1.0F : 0.0F);
        ClientPlayNetworking.send(
                new HunterQuickstepIntentPayload(
                        forward,
                        strafe,
                        nextSequence++
                )
        );
    }
}
