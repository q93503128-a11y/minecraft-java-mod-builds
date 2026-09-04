package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Automatic campaign bootstrap: prepare TURNBOUND terrain, then place the player into canonical Radia. */
public final class StarterSliceBootstrap {
    private StarterSliceBootstrap() {}

    public static void tick(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return;
        if (WorldSessionRouter.active(player) || BattleSessionManager.exists(player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        if (!AsterMarchFoundationBuilder.ready(level)) {
            holdDuringStartup(player);
            if (player.tickCount < 40) return;
            if (!AsterMarchFoundationBuilder.step(level, player)) return;
        }

        player.setNoGravity(false);
        player.setDeltaMovement(Vec3.ZERO);
        WorldSessionRouter.enterInitial(player);
        RadiaSafeSpawn.place(level, player);
    }

    public static boolean building(ServerPlayer player) {
        return player != null
                && player.level().dimension() == Level.OVERWORLD
                && player.level() instanceof ServerLevel level
                && !AsterMarchFoundationBuilder.ready(level);
    }

    private static void holdDuringStartup(ServerPlayer player) {
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setPos(0.5, 250.0, 20.5);
    }

    public static void remove(ServerPlayer player) { if (player != null) player.setNoGravity(false); }
    public static void clearAll(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) remove(player);
    }
}
