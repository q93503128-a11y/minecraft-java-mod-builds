package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class VillageStructureHud {
    private static final UUID BAR_ID = UUID.nameUUIDFromBytes(
            "villageguardians:structure_health".getBytes(StandardCharsets.UTF_8));
    private static final ServerBossEvent HEALTH_BAR = new ServerBossEvent(
            BAR_ID,
            Component.literal("시설 내구도"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS);
    private static final int DISPLAY_TICKS = 90;

    private static int visibleTicks;

    private VillageStructureHud() {
    }

    public static void reset() {
        visibleTicks = 0;
        HEALTH_BAR.removeAllPlayers();
        HEALTH_BAR.setVisible(false);
    }

    public static void showDamage(
            MinecraftServer server,
            VillageProgressionSystem.Building building,
            int current,
            int maximum) {
        float progress = maximum <= 0 ? 0.0f : Math.max(0.0f, Math.min(1.0f, current / (float) maximum));
        HEALTH_BAR.setName(Component.literal(building.displayName() + "  " + current + " / " + maximum));
        HEALTH_BAR.setProgress(progress);
        HEALTH_BAR.setColor(progress <= 0.25f
                ? BossEvent.BossBarColor.RED
                : progress <= 0.55f
                ? BossEvent.BossBarColor.YELLOW
                : BossEvent.BossBarColor.GREEN);
        HEALTH_BAR.setVisible(true);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            HEALTH_BAR.addPlayer(player);
        }
        visibleTicks = DISPLAY_TICKS;
    }

    public static void tick(MinecraftServer server) {
        if (visibleTicks <= 0) {
            if (HEALTH_BAR.isVisible()) {
                HEALTH_BAR.setVisible(false);
                HEALTH_BAR.removeAllPlayers();
            }
            return;
        }

        visibleTicks--;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            HEALTH_BAR.addPlayer(player);
        }
        if (visibleTicks == 0) {
            HEALTH_BAR.setVisible(false);
            HEALTH_BAR.removeAllPlayers();
        }
    }
}
