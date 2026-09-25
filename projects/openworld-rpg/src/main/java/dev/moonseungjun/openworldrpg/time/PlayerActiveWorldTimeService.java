package dev.moonseungjun.openworldrpg.time;

import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server-only personal active-world-time authority. */
public final class PlayerActiveWorldTimeService {
    public static final String ALDERFORD_SHRINE_EPOCH =
            "openworld_rpg:r01/alderford_shrine";
    private static final int PERSIST_INTERVAL_TICKS = 20;

    private PlayerActiveWorldTimeService() {
    }

    public static PlayerActiveWorldTimeState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                PlayerActiveWorldTimeAttachments.ACTIVE_WORLD_TIME,
                PlayerActiveWorldTimeState.initial()
        );
    }

    public static PlayerActiveWorldTimeState markEpochOnce(
            ServerPlayer player,
            String epochId
    ) {
        return replace(player, state(player).markEpochOnce(epochId));
    }

    public static void ensureAlderfordShrineEpoch(ServerPlayer player) {
        markEpochOnce(player, ALDERFORD_SHRINE_EPOCH);
    }

    /**
     * Persists in one-second chunks rather than writing one attachment mutation every server tick.
     */
    public static void tickLoadedPlayers(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        if (Math.floorMod(server.getTickCount(), PERSIST_INTERVAL_TICKS) != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            replace(player, state(player).advance(PERSIST_INTERVAL_TICKS));
        }
    }

    private static PlayerActiveWorldTimeState replace(
            ServerPlayer player,
            PlayerActiveWorldTimeState next
    ) {
        PlayerActiveWorldTimeState current = state(player);
        if (current.equals(next)) {
            return current;
        }
        player.setAttached(PlayerActiveWorldTimeAttachments.ACTIVE_WORLD_TIME, next);
        return next;
    }
}
