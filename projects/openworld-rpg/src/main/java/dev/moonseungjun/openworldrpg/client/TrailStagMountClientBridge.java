package dev.moonseungjun.openworldrpg.client;

import dev.moonseungjun.openworldrpg.network.TrailStagMountRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.LocalPlayer;

/** Client bridge for the rebindable mount action. The client sends intent only. */
public final class TrailStagMountClientBridge {
    private static long nextSequence;

    private TrailStagMountClientBridge() {
    }

    public static void request(LocalPlayer player) {
        if (player == null) {
            return;
        }
        ClientPlayNetworking.send(
                new TrailStagMountRequestPayload(
                        nextSequence++,
                        TrailStagMountRequestPayload.CURRENT_PROTOCOL_VERSION
                )
        );
    }
}
