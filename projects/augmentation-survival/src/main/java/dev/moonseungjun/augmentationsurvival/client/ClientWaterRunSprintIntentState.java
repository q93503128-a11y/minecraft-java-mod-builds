package dev.moonseungjun.augmentationsurvival.client;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.player.LocalPlayer;

/** Client prediction mirror of the server Water Run sprint-intent grace. */
public final class ClientWaterRunSprintIntentState {
    private static final Map<LocalPlayer, Long> LAST_SPRINT_TICK = new WeakHashMap<>();

    private ClientWaterRunSprintIntentState() { }

    public static void capture(LocalPlayer player) {
        if (player.isSprinting()) {
            LAST_SPRINT_TICK.put(player, player.level().getGameTime());
        }
    }

    public static boolean wasRecent(LocalPlayer player, long graceTicks) {
        Long last = LAST_SPRINT_TICK.get(player);
        if (last == null) return false;
        long now = player.level().getGameTime();
        return now >= last && now - last <= Math.max(0L, graceTicks);
    }
}
