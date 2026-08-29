package io.github.q93503128.turnbound.session;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BattleSessionManager {
    private static final Map<UUID, BattleSession> SESSIONS = new HashMap<>();

    private BattleSessionManager() {
    }

    public static void start(ServerPlayer player) {
        end(player);
        BattleSession session = new BattleSession(player);
        SESSIONS.put(player.getUUID(), session);
        BattleNetwork.sync(player, session);
    }

    public static boolean active(ServerPlayer player) {
        BattleSession session = SESSIONS.get(player.getUUID());
        return session != null && !session.finished();
    }

    public static boolean exists(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        BattleSession session = SESSIONS.get(player.getUUID());
        if (session != null) {
            session.tick(player);
            if (player.tickCount % 5 == 0) BattleNetwork.sync(player, session);
        }
    }

    public static void command(ServerPlayer player, String command) {
        BattleSession session = SESSIONS.get(player.getUUID());
        if (session == null) return;

        String[] parts = command.split("\\|", -1);
        switch (parts[0]) {
            case "ACT" -> {
                if (parts.length >= 4) session.action(player, parts[1], parts[2], parts[3]);
            }
            case "AUTO" -> session.toggleAuto(player);
            case "SPEED" -> session.toggleSpeed(player);
            case "FLEE" -> {
                // P0 has no mid-battle flee. This packet is the post-result return action only.
                if (session.finished()) end(player);
            }
            default -> {
            }
        }
    }

    public static void end(ServerPlayer player) {
        BattleSession old = SESSIONS.remove(player.getUUID());
        if (old != null) old.cleanup(player);
        BattleNetwork.close(player);
    }

    public static void clearAll(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) end(player);
        SESSIONS.clear();
    }
}
