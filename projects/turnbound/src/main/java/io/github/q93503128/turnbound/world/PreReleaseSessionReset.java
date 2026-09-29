package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Temporary repeated-playtest reset for the single-instance alpha workflow.
 *
 * <p>Enabled only while the game is unfinished. Every login to the bound Drehmal world starts from the same
 * campaign/world discovery state and opening position. This class and its login hook MUST be removed/disabled
 * before release completion so real progression persists normally.</p>
 */
public final class PreReleaseSessionReset {
    public static final boolean ENABLED = true;

    private PreReleaseSessionReset() {}

    public static boolean enabled(ServerPlayer player) {
        if (!ENABLED || player == null) return false;
        MinecraftServer server = player.level().getServer();
        return server != null && DrehmalWorldBinding.isBound(server);
    }

    public static void apply(ServerPlayer player) {
        if (!enabled(player)) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        ExternalWorldBootstrap.remove(player);
        CampaignProgressStore.resetToNewGame(player.getUUID());

        ExternalWorldSavedData external = ExternalWorldSavedData.get(server);
        external.resetPlayer(player.getUUID());
        TurnboundWorldSavedData.get(server).resetForPreReleaseSession();

        if (!DrehmalStartArrival.forceFreshEntry(player, external)) {
            Turnbound.LOGGER.error("TURNBOUND pre-release session reset could not place {} at the opening entry",
                    player.getUUID());
        }
        CampaignPersistence.save(player);
        Turnbound.LOGGER.info("TURNBOUND pre-release session reset applied for {}", player.getUUID());
    }
}
