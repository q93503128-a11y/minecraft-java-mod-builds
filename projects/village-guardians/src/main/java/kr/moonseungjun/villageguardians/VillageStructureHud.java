package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Emergency-only structure damage alert.
 * Persistent defense comparison lives in VillageMainHudOverlay, so this bar never cycles unrelated facilities.
 */
public final class VillageStructureHud {
    private static final UUID BAR_ID = UUID.nameUUIDFromBytes(
            "villageguardians:structure_health".getBytes(StandardCharsets.UTF_8));
    private static final ServerBossEvent HEALTH_BAR = new ServerBossEvent(
            BAR_ID,
            Component.literal("시설 피해 경보"),
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.PROGRESS);
    private static final int DAMAGE_FOCUS_TICKS = 86;

    private static int focusTicks;

    private VillageStructureHud() {}

    public static void reset() {
        focusTicks = 0;
        HEALTH_BAR.removeAllPlayers();
        HEALTH_BAR.setVisible(false);
    }

    public static void showDamage(
            MinecraftServer server,
            VillageProgressionSystem.Building building,
            int current,
            int maximum) {
        updateBar(building, current, maximum);
        showToAll(server);
        focusTicks = DAMAGE_FOCUS_TICKS;
    }

    public static void tick(MinecraftServer server) {
        if (focusTicks <= 0) {
            hide();
            return;
        }
        focusTicks--;
        showToAll(server);
        if (focusTicks <= 0) hide();
    }

    private static void updateBar(
            VillageProgressionSystem.Building building,
            int current,
            int maximum) {
        float progress = maximum <= 0 ? 0.0f : Math.max(0.0f, Math.min(1.0f, current / (float) maximum));
        String state = progress <= 0.25f ? "긴급" : progress <= 0.55f ? "주의" : "피격";
        HEALTH_BAR.setName(Component.literal(
                "⚠ " + state + " · " + building.displayName() + "  " + current + " / " + maximum));
        HEALTH_BAR.setProgress(progress);
        HEALTH_BAR.setColor(progress <= 0.25f
                ? BossEvent.BossBarColor.RED
                : progress <= 0.55f
                ? BossEvent.BossBarColor.YELLOW
                : BossEvent.BossBarColor.GREEN);
    }

    private static void showToAll(MinecraftServer server) {
        if (server == null) return;
        HEALTH_BAR.setVisible(true);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) HEALTH_BAR.addPlayer(player);
    }

    private static void hide() {
        if (!HEALTH_BAR.isVisible()) return;
        HEALTH_BAR.setVisible(false);
        HEALTH_BAR.removeAllPlayers();
    }
}
