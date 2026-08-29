package kr.moonseungjun.titanbreak.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AnalysisJammingService {
    private static final Map<UUID, Long> JAM_UNTIL = new ConcurrentHashMap<>();

    private AnalysisJammingService() {}

    public static void apply(ServerPlayer player, int ticks) {
        if (!(player.level() instanceof ServerLevel level)) return;
        long until = level.getGameTime() + Math.max(1, ticks);
        JAM_UNTIL.merge(player.getUUID(), until, Math::max);
    }

    public static int remainingTicks(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return 0;
        long remaining = JAM_UNTIL.getOrDefault(player.getUUID(), 0L) - level.getGameTime();
        if (remaining <= 0L) {
            JAM_UNTIL.remove(player.getUUID());
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    public static void clear(UUID playerId) {
        JAM_UNTIL.remove(playerId);
    }

    public static void clearAll() {
        JAM_UNTIL.clear();
    }
}
