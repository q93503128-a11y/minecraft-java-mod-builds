package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Minecraft-server adapter for canonical TURNBOUND campaign player attachments. */
public final class CampaignPersistence {
    /** Legacy alpha location; read-only after the attachment migration, except for reward-journal recovery. */
    private static final LevelResource TURNBOUND_DATA = new LevelResource("turnbound");
    private static final int AUTOSAVE_TICKS = 100;
    private static final Set<UUID> BLOCKED = new LinkedHashSet<>();

    private CampaignPersistence() {}

    public static void load(ServerPlayer player) {
        UUID playerId = player.getUUID();
        BLOCKED.remove(playerId);
        CampaignProgressStore.removeRuntime(playerId);

        try {
            String attached = player.getData(TurnboundAttachments.CAMPAIGN_PROFILE);
            if (attached != null && !attached.isBlank()) {
                CampaignProgressStore.restore(playerId, CampaignSaveCodec.decode(attached));
            } else if (!importLegacySave(player)) {
                CampaignProgressStore.ensureNewGame(playerId);
                writeAttachment(player);
            }
        } catch (Exception ex) {
            blockCampaignLoad(player, playerFile(player), ex);
            return;
        }

        // The reward journal remains an intentionally separate write-ahead file. It is not authoritative profile state.
        // If an old build crashed between battle completion and profile commit, recover it once and immediately rewrite
        // the canonical attachment so subsequent loads no longer depend on the legacy profile file.
        try {
            RewardTransactionJournal.Recovery recovery = RewardTransactionJournal.recover(playerFile(player), playerId);
            if (recovery == RewardTransactionJournal.Recovery.APPLIED) {
                writeAttachment(player);
                CampaignProgressStore.markClean(playerId);
                Turnbound.LOGGER.warn("TURNBOUND recovered pending battle rewards for {} from the write-ahead journal", playerId);
            }
        } catch (Exception ex) {
            BLOCKED.add(playerId);
            Turnbound.LOGGER.error("TURNBOUND pending reward recovery failed for {}; canonical campaign attachment was left intact", playerId, ex);
            player.connection.disconnect(Component.literal(
                    "TURNBOUND 보상 복구 데이터를 안전하게 처리하지 못했습니다. 기존 저장 데이터는 보존되었습니다."));
        }
    }

    /** Imports a pre-attachment alpha profile exactly once. The old JSON is never used as the live canonical store. */
    private static boolean importLegacySave(ServerPlayer player) throws IOException {
        Path file = playerFile(player);
        if (!Files.exists(file) && !Files.exists(file.resolveSibling(file.getFileName() + ".bak"))) return false;

        var loaded = CampaignSaveFiles.load(file);
        if (loaded.isEmpty()) return false;
        CampaignProgressStore.restore(player.getUUID(), loaded.get().snapshot());
        writeAttachment(player);
        CampaignProgressStore.markClean(player.getUUID());
        Turnbound.LOGGER.info("TURNBOUND imported legacy JSON campaign profile for {} into the canonical player attachment", player.getUUID());

        // Preserve old files as migration backups rather than deleting user data.
        try {
            CampaignSaveFiles.quarantinePrimary(file);
        } catch (IOException ex) {
            Turnbound.LOGGER.warn("TURNBOUND imported legacy profile for {} but could not archive the old primary JSON", player.getUUID(), ex);
        }
        return true;
    }

    private static void blockCampaignLoad(ServerPlayer player, Path legacyFile, Exception ex) {
        UUID playerId = player.getUUID();
        BLOCKED.add(playerId);
        Turnbound.LOGGER.error("TURNBOUND campaign attachment migration/load failed for {}; preserving data and blocking session entry", playerId, ex);
        // Only quarantine a legacy file if one exists. Attachment data is owned by Minecraft and must never be destroyed here.
        if (Files.exists(legacyFile)) {
            try {
                CampaignSaveFiles.quarantine(legacyFile);
            } catch (IOException quarantineFailure) {
                Turnbound.LOGGER.error("TURNBOUND failed to quarantine unreadable legacy campaign save for {}", playerId, quarantineFailure);
            }
        }
        player.connection.disconnect(Component.literal(
                "TURNBOUND 저장 데이터를 안전하게 불러오지 못했습니다. 원본은 보존되었으며 새 게임으로 덮어쓰지 않았습니다."));
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

    public static boolean saveIfDirtyForLifecycle(ServerPlayer player) {
        if (blocked(player)) return false;
        if (!CampaignProgressStore.isDirty(player.getUUID())) return true;
        try {
            saveOrThrow(player);
            return true;
        } catch (IOException ex) {
            Turnbound.LOGGER.error("TURNBOUND failed to flush campaign attachment for {} during lifecycle transition", player.getUUID(), ex);
            return false;
        }
    }

    public static void save(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (blocked(player) || !CampaignProgressStore.hasRuntime(playerId)) return;
        try {
            saveOrThrow(player);
        } catch (IOException ex) {
            Turnbound.LOGGER.error("TURNBOUND failed to save campaign attachment for {}", playerId, ex);
        }
    }

    static void saveOrThrow(ServerPlayer player) throws IOException {
        UUID playerId = player.getUUID();
        if (blocked(player)) throw new IOException("TURNBOUND campaign persistence is blocked for " + playerId);
        if (!CampaignProgressStore.hasRuntime(playerId)) throw new IOException("TURNBOUND campaign runtime is missing for " + playerId);
        try {
            writeAttachment(player);
            CampaignProgressStore.markClean(playerId);
        } catch (RuntimeException ex) {
            throw new IOException("TURNBOUND campaign attachment serialization failed for " + playerId, ex);
        }
    }

    private static void writeAttachment(ServerPlayer player) {
        String encoded = CampaignSaveCodec.encode(CampaignProgressStore.snapshot(player.getUUID()));
        player.setData(TurnboundAttachments.CAMPAIGN_PROFILE, encoded);
    }

    /** Legacy path used only for one-time import and the crash-recovery reward journal. */
    public static Path playerFile(ServerPlayer player) {
        MinecraftServer server = Objects.requireNonNull(player.level().getServer(), "TURNBOUND player has no logical server");
        return server.getWorldPath(TURNBOUND_DATA)
                .resolve("playerdata")
                .resolve(player.getUUID() + ".json");
    }
}
