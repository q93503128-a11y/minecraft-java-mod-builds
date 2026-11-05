package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Server-owned lifecycle for the intentionally empty rebuild. Unlike the old campaign
 * loader, this listener NEVER imports, rewrites, quarantines or migrates old save data.
 * Entity attachment copyOnDeath covers ordinary player cloning on death.
 */
public final class RebuildCampaignLifecycle {
    private static final int AUTOSAVE_TICKS = 100;

    private RebuildCampaignLifecycle() {}

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID playerId = player.getUUID();
        try {
            Path original = CampaignPersistence.playerFile(player);
            boolean oldFile = Files.exists(original)
                    || Files.exists(original.resolveSibling(original.getFileName() + ".bak"));
            String legacy = player.getData(TurnboundAttachments.CAMPAIGN_PROFILE);
            String rebuild = player.getData(TurnboundAttachments.REBUILD_CAMPAIGN_PROFILE);
            RebuildSaveAdmission.Decision decision = RebuildSaveAdmission.decide(
                    rebuild != null && !rebuild.isBlank(),
                    legacy != null && !legacy.isBlank(),
                    oldFile);

            if (decision == RebuildSaveAdmission.Decision.REJECT_LEGACY
                    || decision == RebuildSaveAdmission.Decision.REJECT_CONFLICT) {
                Turnbound.LOGGER.warn("TURNBOUND rebuild refused legacy save admission for {}", playerId);
                player.connection.disconnect(Component.literal(
                        "TURNBOUND의 이전 저장 데이터가 발견되었습니다. 이 버전은 새로운 월드에서 시작해야 합니다. 기존 데이터는 변경하지 않았습니다."));
                return;
            }
            if (CampaignProgressStore.hasRuntime(playerId)) {
                throw new IllegalStateException("A player runtime already exists at rebuild login");
            }
            if (decision == RebuildSaveAdmission.Decision.RESTORE_REBUILD) {
                CampaignProgressStore.restoreEmptyRebuildGame(playerId, rebuild);
            } else {
                if (!CampaignProgressStore.startEmptyRebuildGame(playerId)) {
                    throw new IllegalStateException("Failed to allocate fresh rebuild runtime");
                }
                persist(player);
            }
        } catch (RuntimeException exception) {
            // Never replace malformed or unsupported attached data with a blank profile.
            CampaignProgressStore.removeRuntime(playerId);
            Turnbound.LOGGER.error("TURNBOUND rebuild profile login rejected for {}", playerId, exception);
            player.connection.disconnect(Component.literal(
                    "TURNBOUND 저장 데이터를 안전하게 불러오지 못했습니다. 기존 데이터는 변경하지 않았습니다."));
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % AUTOSAVE_TICKS != 0) return;
        if (!CampaignProgressStore.isEmptyRebuildGame(player.getUUID())
                || !CampaignProgressStore.isDirty(player.getUUID())) return;
        try {
            persist(player);
        } catch (RuntimeException exception) {
            Turnbound.LOGGER.error("TURNBOUND rebuild autosave failed for {}", player.getUUID(), exception);
        }
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        try {
            if (CampaignProgressStore.isEmptyRebuildGame(player.getUUID())) persist(player);
        } catch (RuntimeException exception) {
            Turnbound.LOGGER.error("TURNBOUND rebuild logout save failed for {}", player.getUUID(), exception);
        } finally {
            CampaignProgressStore.removeRuntime(player.getUUID());
        }
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            try {
                if (CampaignProgressStore.isEmptyRebuildGame(player.getUUID())) persist(player);
            } catch (RuntimeException exception) {
                Turnbound.LOGGER.error("TURNBOUND rebuild shutdown save failed for {}", player.getUUID(), exception);
            }
        }
        CampaignProgressStore.clearRuntime();
    }

    private static void persist(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (!CampaignProgressStore.isEmptyRebuildGame(playerId))
            throw new IllegalStateException("Refusing rebuild save without an active rebuild profile");
        // Serialize and validate BEFORE mutating the attachment or its dirty flag.
        String encoded = CampaignProgressStore.encodeEmptyRebuildGame(playerId);
        player.setData(TurnboundAttachments.REBUILD_CAMPAIGN_PROFILE, encoded);
        CampaignProgressStore.markClean(playerId);
    }
}
