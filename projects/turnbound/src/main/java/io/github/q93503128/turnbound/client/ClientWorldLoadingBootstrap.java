package io.github.q93503128.turnbound.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Covers the first rendered world frame while TURNBOUND field state is still crossing the network. */
public final class ClientWorldLoadingBootstrap {
    private static ClientLevel lastLevel;

    private ClientWorldLoadingBootstrap() {}

    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            if (lastLevel != null) ClientFieldState.beginWorld();
            lastLevel = null;
            return;
        }
        if (minecraft.level != lastLevel) {
            lastLevel = minecraft.level;
            // On a normal join no snapshot exists yet. If networking won the race and already supplied
            // the new field snapshot, do not erase it here and wait forever for a second packet.
            if (!ClientFieldState.initialSnapshotReceived()) ClientFieldState.beginWorld();
        }
        if (!ClientFieldState.initialSnapshotReceived() && minecraft.gui.screen() == null) {
            minecraft.gui.setScreen(new WorldLoadingScreen());
        }
    }
}
