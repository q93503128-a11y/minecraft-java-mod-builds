package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Explicit playtest reset used only when an operator requests a fresh TURNBOUND run.
 *
 * <p>Normal login never resets campaign progress. The reset command deliberately clears the current player's
 * campaign/world discovery state and returns them to the opening position so the same test instance can be reused
 * without sacrificing persistence between ordinary reconnects.</p>
 */
public final class PreReleaseSessionReset {
    private PreReleaseSessionReset() {}

    public static boolean canApply(ServerPlayer player) {
        if (player == null) return false;
        MinecraftServer server = player.level().getServer();
        return server != null && DrehmalWorldBinding.isBound(server);
    }

    public static boolean apply(ServerPlayer player) {
        if (!canApply(player)) return false;
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;

        ExternalWorldBootstrap.remove(player);
        CampaignProgressStore.resetToNewGame(player.getUUID());

        ExternalWorldSavedData external = ExternalWorldSavedData.get(server);
        external.resetPlayer(player.getUUID());
        TurnboundWorldSavedData.get(server).resetForPreReleaseSession();

        boolean placed = DrehmalStartArrival.forceFreshEntry(player, external);
        if (!placed) {
            Turnbound.LOGGER.error("TURNBOUND manual playtest reset could not place {} at the opening entry",
                    player.getUUID());
        }
        CampaignPersistence.save(player);
        Turnbound.LOGGER.info("TURNBOUND manual playtest reset applied for {}", player.getUUID());
        return placed;
    }
}
