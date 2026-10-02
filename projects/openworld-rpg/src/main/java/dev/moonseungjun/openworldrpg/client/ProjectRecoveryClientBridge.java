package dev.moonseungjun.openworldrpg.client;

import dev.moonseungjun.openworldrpg.network.RecoveryRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.LocalPlayer;

/**
 * Recovery Belt input bridge. The client submits only a monotonic intent sequence; it never chooses
 * timing, slot contents, consumption or effect values.
 */
public final class ProjectRecoveryClientBridge {
    private static long nextSequence;

    private ProjectRecoveryClientBridge() {
    }

    public static void request(LocalPlayer player) {
        if (player == null) {
            return;
        }
        ClientPlayNetworking.send(
                new RecoveryRequestPayload(nextSequence++)
        );
    }

    public static void resetSequence() {
        nextSequence = 0L;
    }
}
