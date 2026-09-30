package dev.moonseungjun.openworldrpg.client;

import dev.moonseungjun.openworldrpg.network.DodgeAcceptedPayload;
import dev.moonseungjun.openworldrpg.network.DodgeRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Client input/presentation bridge. Dodge mechanics remain entirely server-authoritative. */
public final class ProjectDodgeClientBridge {
    private static long nextSequence;

    private ProjectDodgeClientBridge() {
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(
                DodgeAcceptedPayload.TYPE,
                (payload, context) -> context.client().execute(
                        () -> ProjectDodgeAnimationBridge.playAccepted(
                                payload.entityId(),
                                payload.directionCode()
                        )
                )
        );
    }

    public static void request(LocalPlayer player) {
        if (player == null
                || !ProjectDodgeAnimationBridge.presentationAvailable()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        float forward = (client.options.keyUp.isDown() ? 1.0F : 0.0F)
                - (client.options.keyDown.isDown() ? 1.0F : 0.0F);
        float strafe = (client.options.keyRight.isDown() ? 1.0F : 0.0F)
                - (client.options.keyLeft.isDown() ? 1.0F : 0.0F);

        ClientPlayNetworking.send(
                new DodgeRequestPayload(
                        forward,
                        strafe,
                        nextSequence++
                )
        );
    }

    public static void resetSequence() {
        nextSequence = 0L;
    }
}
