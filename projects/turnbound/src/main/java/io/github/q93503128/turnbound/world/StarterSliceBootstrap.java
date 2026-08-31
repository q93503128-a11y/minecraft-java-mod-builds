package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Compatibility-named automatic campaign bootstrap.
 * The legacy alpha.16 starter-slice terrain build is gone: after campaign load the player enters canonical Radia.
 */
public final class StarterSliceBootstrap {
    private StarterSliceBootstrap() {}

    public static void tick(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD || player.tickCount < 40) return;
        if (WorldSessionRouter.active(player) || BattleSessionManager.exists(player)) return;
        player.setNoGravity(false);
        WorldSessionRouter.enterInitial(player);
    }

    public static boolean building(ServerPlayer player) { return false; }
    public static void remove(ServerPlayer player) { if (player != null) player.setNoGravity(false); }
    public static void clearAll(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) remove(player);
    }
}
