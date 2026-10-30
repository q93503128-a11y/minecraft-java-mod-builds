package dev.moonseungjun.augmentationsurvival.augment.utility;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;

/** Remembers sprint intent before ServerPlayer movement logic can clear the sprint flag at a fluid edge. */
public final class WaterRunSprintIntentService {
    private static final Map<ServerPlayer, Long> LAST_SPRINT_TICK = new WeakHashMap<>();

    private WaterRunSprintIntentService() { }

    public static void capture(ServerPlayer player) {
        if (player.isSprinting()) {
            LAST_SPRINT_TICK.put(player, player.level().getGameTime());
        }
    }

    public static boolean wasRecent(ServerPlayer player, long graceTicks) {
        Long last = LAST_SPRINT_TICK.get(player);
        if (last == null) return false;
        long now = player.level().getGameTime();
        return now >= last && now - last <= Math.max(0L, graceTicks);
    }
}
