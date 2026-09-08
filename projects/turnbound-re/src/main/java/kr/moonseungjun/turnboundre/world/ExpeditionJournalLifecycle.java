package kr.moonseungjun.turnboundre.world;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Gives the Expedition Journal once when a player joins the server. */
public final class ExpeditionJournalLifecycle {
    public void register(IEventBus bus) {
        if (bus == null) throw new IllegalArgumentException("bus required");
        bus.addListener(this::onPlayerLoggedIn);
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ExpeditionJournalAccess.ensure(player);
    }
}
