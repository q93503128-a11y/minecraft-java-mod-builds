package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Routes the persistent world shell between Radia hub and authored chapter field sessions. */
public final class WorldSessionRouter {
    private WorldSessionRouter() {}

    public static boolean active(ServerPlayer player) {
        return RadiaHubSessionManager.active(player) || FieldSessionManager.active(player) || GloamwoodSessionManager.active(player);
    }

    public static void enterInitial(ServerPlayer player) { RadiaHubSessionManager.enter(player); }

    public static void tick(ServerPlayer player) {
        if (RadiaHubSessionManager.active(player)) RadiaHubSessionManager.tick(player);
        else if (GloamwoodSessionManager.active(player)) GloamwoodSessionManager.tick(player);
        else FieldSessionManager.tick(player);
    }

    public static boolean interactEntity(ServerPlayer player, Entity entity) {
        if (RadiaHubSessionManager.active(player)) return RadiaHubSessionManager.interactEntity(player, entity);
        if (GloamwoodSessionManager.active(player)) return GloamwoodSessionManager.interactEntity(player, entity);
        return FieldSessionManager.interactEntity(player, entity);
    }

    public static void command(ServerPlayer player, String command) {
        if (RadiaHubSessionManager.active(player)) {
            RadiaHubSessionManager.command(player, command);
            return;
        }
        if (GloamwoodSessionManager.active(player)) {
            GloamwoodSessionManager.command(player, command);
            return;
        }
        if (FieldSessionManager.active(player) && command != null && command.equals("TRAVEL|" + AsterMarchRegionCatalog.FT_RADIA)) {
            FieldSessionManager.remove(player);
            RadiaHubSessionManager.enter(player);
            return;
        }
        FieldSessionManager.command(player, command);
    }

    public static void onBattleEnded(ServerPlayer player, String encounterId, BattleOutcome outcome) {
        if (RadiaHubSessionManager.active(player)) RadiaHubSessionManager.onBattleEnded(player, encounterId, outcome);
        else if (GloamwoodSessionManager.active(player)) GloamwoodSessionManager.onBattleEnded(player, encounterId, outcome);
        else FieldSessionManager.onBattleEnded(player, encounterId, outcome);
    }

    public static void remove(ServerPlayer player) {
        RadiaHubSessionManager.remove(player);
        GloamwoodSessionManager.remove(player);
        FieldSessionManager.remove(player);
    }

    public static void clearAll(Iterable<ServerPlayer> players) {
        RadiaHubSessionManager.clearAll(players);
        GloamwoodSessionManager.clearAll(players);
        FieldSessionManager.clearAll(players);
    }
}
