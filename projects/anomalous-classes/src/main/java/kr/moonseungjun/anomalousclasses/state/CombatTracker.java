package kr.moonseungjun.anomalousclasses.state;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class CombatTracker {
    /** D11 lists eight seconds as an adjustable starting value, not a final balance decision. */
    public static final int COMBAT_CLEAR_TICKS = 8 * 20;
    private static final Map<UUID, Integer> LAST_COMBAT_TICK = new ConcurrentHashMap<>();

    private CombatTracker() {}

    public static void onDamage(LivingDamageEvent.Post event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer player) mark(player);
        if (event.getEntity() instanceof ServerPlayer player) mark(player);
    }

    public static boolean isLocked(ServerPlayer player) {
        Integer last = LAST_COMBAT_TICK.get(player.getUUID());
        if (last == null || player.getServer() == null) return false;
        int elapsed = player.getServer().getTickCount() - last;
        if (elapsed < 0 || elapsed >= COMBAT_CLEAR_TICKS) {
            LAST_COMBAT_TICK.remove(player.getUUID());
            return false;
        }
        return true;
    }

    public static void remove(UUID playerId) {
        LAST_COMBAT_TICK.remove(playerId);
    }

    private static void mark(ServerPlayer player) {
        if (player.getServer() != null) LAST_COMBAT_TICK.put(player.getUUID(), player.getServer().getTickCount());
    }
}
