package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Minecraft-server adapter for persistent TURNBOUND campaign profiles. */
public final class CampaignPersistence {
    private static final LevelResource TURNBOUND_DATA = new LevelResource("turnbound");
    private static final int AUTOSAVE_TICKS = 100;

    private CampaignPersistence() {}

    public static void load(ServerPlayer player) {
        var playerId = player.getUUID();
        CampaignProgressStore.removeRuntime(playerId);
        Path file = playerFile(player);
        try {
            var loaded = CampaignSaveFiles.load(file);
            if (loaded.isPresent()) {
                CampaignProgressStore.restore(playerId, loaded.get().snapshot());
                if (loaded.get().recoveredBackup()) {
                    // Do not let the next save copy the corrupt primary over the last-known-good backup.
                    CampaignSaveFiles.quarantinePrimary(file);
                    CampaignProgressStore.markDirty(playerId);
                    Turnbound.LOGGER.warn("TURNBOUND recovered campaign save for {} from backup", playerId);
                }
            } else {
                CampaignProgressStore.ensureNewGame(playerId);
            }
        } catch (Exception ex) {
            Turnbound.LOGGER.error("TURNBOUND failed to load campaign save for {}; quarantining unreadable data", playerId, ex);
            try {
                CampaignSaveFiles.quarantine(file);
            } catch (IOException quarantineFailure) {
                Turnbound.LOGGER.error("TURNBOUND failed to quarantine unreadable campaign save for {}", playerId, quarantineFailure);
            }
            CampaignProgressStore.ensureNewGame(playerId);
        }
    }

    public static void autosave(ServerPlayer player) {
        if (player.tickCount % AUTOSAVE_TICKS == 0) saveIfDirty(player);
    }

    public static void saveIfDirty(ServerPlayer player) {
        if (CampaignProgressStore.isDirty(player.getUUID())) save(player);
    }

    public static void save(ServerPlayer player) {
        var playerId = player.getUUID();
        if (!CampaignProgressStore.hasRuntime(playerId)) return;
        try {
            CampaignSaveFiles.save(playerFile(player), CampaignProgressStore.snapshot(playerId));
            CampaignProgressStore.markClean(playerId);
        } catch (IOException ex) {
            Turnbound.LOGGER.error("TURNBOUND failed to save campaign profile for {}", playerId, ex);
        }
    }

    public static Path playerFile(ServerPlayer player) {
        MinecraftServer server = Objects.requireNonNull(player.level().getServer(), "TURNBOUND player has no logical server");
        return server.getWorldPath(TURNBOUND_DATA)
                .resolve("playerdata")
                .resolve(player.getUUID() + ".json");
    }
}
