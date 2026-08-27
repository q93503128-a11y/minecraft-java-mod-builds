package kr.moonseungjun.titanbreak.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Keeps the local user's input/game-tick cadence responsive while the authoritative
 * server simulation is slowed by Reflex Drive. Remote world simulation still follows
 * the server tick rate.
 */
public final class ClientTimeCompensation {
    private static final float LOCAL_TARGET_RATE = 20.0F;

    private ClientTimeCompensation() {}

    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (!TitanClientState.flag("active")) return;

        if (Math.abs(mc.level.tickRateManager().tickrate() - LOCAL_TARGET_RATE) > 0.001F) {
            mc.level.tickRateManager().setTickRate(LOCAL_TARGET_RATE);
        }
    }
}
