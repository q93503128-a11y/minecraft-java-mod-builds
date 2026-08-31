package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Minecraft-server adapter for persistent TURNBOUND campaign profiles. */
public final class CampaignPersistence {
    private static final LevelResource TURNBOUND_DATA = new LevelResource("turnbound");
    private static final int AUTOSAVE_TICKS = 100;
    private static final Set<UUID> BLOCKED = new LinkedHashSet<>();

    private CampaignPersistence() {}

    public static void load(ServerPlayer player) {
        var playerId = player.getUUID();
        BLOCKED.remove(playerId);
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
            BLOCKED.add(playerId);
            Turnbound.LOGGER.error("TURNBOUND campaign save migration/load failed for {}; preserving unreadable data and blocking session entry", playerId, ex);
            try {
                CampaignSaveFiles.quarantine(file);
            } catch (IOException quarantineFailure) {
                Turnbound.LOGGER.error("TURNBOUND failed to quarantine unreadable campaign save for {}", playerId, quarantineFailure);
            }
            player.connection.disconnect(Component.literal(
                    "TURNBOUND 저장 데이터를 안전하게 불러오지 못했습니다. 원본은 보존되었으며 새 게임으로 덮어쓰지 않았습니다."));
        }
    }

    public static boolean blocked(ServerPlayer player) {
        return player != null && BLOCKED.contains(player.getUUID());
    }

    public static void forget(ServerPlayer player) {
        if (player != null) BLOCKED.remove(player.getUUID());
    }

    public static void autosave(ServerPlayer player) {
        if (!blocked(player) && player.tickCount % AUTOSAVE_TICKS == 0) saveIfDirty(player);
    }

    public static void saveIfDirty(ServerPlayer player) {
        if (!blocked(player) && CampaignProgressStore.isDirty(player.getUUID())) save(player);
    }

    public static void save(ServerPlayer player) {
        var playerId = player.getUUID();
        if (blocked(player) || !CampaignProgressStore.hasRuntime(playerId)) return;
        try {
            saveOrThrow(player);
        } catch (IOException ex) {
            Turnbound.LOGGER.error("TURNBOUND failed to save campaign profile for {}", playerId, ex);
        }
    }

    static void saveOrThrow(ServerPlayer player) throws IOException {
        var playerId = player.getUUID();
        if (blocked(player)) throw new IOException("TURNBOUND campaign persistence is blocked for " + playerId);
        if (!CampaignProgressStore.hasRuntime(playerId)) throw new IOException("TURNBOUND campaign runtime is missing for " + playerId);
        CampaignSaveFiles.save(playerFile(player), CampaignProgressStore.snapshot(playerId));
        CampaignProgressStore.markClean(playerId);
    }

    public static Path playerFile(ServerPlayer player) {
        MinecraftServer server = Objects.requireNonNull(player.level().getServer(), "TURNBOUND player has no logical server");
        return server.getWorldPath(TURNBOUND_DATA)
                .resolve("playerdata")
                .resolve(player.getUUID() + ".json");
    }
}
