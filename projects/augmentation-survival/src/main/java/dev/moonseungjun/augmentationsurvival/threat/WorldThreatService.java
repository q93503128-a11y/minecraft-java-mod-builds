package dev.moonseungjun.augmentationsurvival.threat;

import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Server authority for the save-wide augmentation threat high-water mark. */
public final class WorldThreatService {
    private WorldThreatService() {
    }

    public static WorldThreatState state(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.overworld().getAttachedOrSet(
                WorldThreatAttachments.THREAT_STATE,
                WorldThreatState.initial()
        );
    }

    public static WorldThreatState observePowerScore(
            MinecraftServer server,
            double playerPowerScore
    ) {
        Objects.requireNonNull(server, "server");
        WorldThreatState current = state(server);
        WorldThreatState next = current.observe(playerPowerScore);
        if (next.equals(current)) {
            return current;
        }
        ServerLevel overworld = server.overworld();
        overworld.setAttached(WorldThreatAttachments.THREAT_STATE, next);
        return next;
    }
}
