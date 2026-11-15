package kr.moonseungjun.anomalousclasses.client;

import java.util.UUID;
import kr.moonseungjun.anomalousclasses.network.payload.ParasiteHostViewPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Local visual camera only. The real player remains the single inventory and
 * interaction owner; the server is the authority for host movement and combat.
 */
public final class ParasiteViewClient {
    private static int desiredId = -1;
    private static UUID desiredUuid = ParasiteHostViewPayload.NONE;
    private static Mob applied;
    private static ClientLevel lastLevel;
    private static int pendingTicks;
    private ParasiteViewClient() {}

    public static void receive(ParasiteHostViewPayload payload) {
        desiredId = payload.entityId();
        desiredUuid = payload.hostUuid();
        pendingTicks = 0;
        if (desiredId < 0) restore(Minecraft.getInstance());
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            restore(minecraft);
            desiredId = -1;
            lastLevel = null;
            return;
        }
        // Never carry an entity-id lookup across a dimension or world swap.
        if (lastLevel != null && lastLevel != minecraft.level) {
            restore(minecraft);
            desiredId = -1;
        }
        lastLevel = minecraft.level;
        if (desiredId < 0) {
            restore(minecraft);
            return;
        }
        Entity entity = minecraft.level.getEntity(desiredId);
        if (!(entity instanceof Mob host) || !host.getUUID().equals(desiredUuid)
                || minecraft.player.getVehicle() != host || !host.isAlive()) {
            restore(minecraft);
            if (++pendingTicks > 200) desiredId = -1;
            return;
        }
        pendingTicks = 0;
        // Minecraft's MouseHandler rotates the active camera entity. Once the
        // camera is the host, copying LocalPlayer -> host here would reset every
        // mouse movement. Seed the first frame, then copy host -> player so the
        // standard input/movement packet carries the real camera direction.
        boolean firstFrame = applied != host || minecraft.getCameraEntity() != host;
        if (firstFrame) {
            host.setYRot(minecraft.player.getYRot());
            host.setYHeadRot(minecraft.player.getYRot());
            host.setXRot(minecraft.player.getXRot());
            minecraft.setCameraEntity(host);
        } else {
            minecraft.player.setYRot(host.getYRot());
            minecraft.player.setXRot(host.getXRot());
        }
        applied = host;
    }

    public static boolean isControlling() {
        Minecraft minecraft = Minecraft.getInstance();
        return applied != null && minecraft.player != null
                && minecraft.getCameraEntity() == applied
                && minecraft.player.getVehicle() == applied;
    }

    private static void restore(Minecraft minecraft) {
        if (applied != null && minecraft.player != null
                && minecraft.getCameraEntity() == applied) {
            minecraft.setCameraEntity(minecraft.player);
        }
        applied = null;
    }
}
