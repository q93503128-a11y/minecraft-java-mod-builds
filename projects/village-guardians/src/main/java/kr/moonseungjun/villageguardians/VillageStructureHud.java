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
            Component.literal("방어 시설 내구도"),
            BossEvent.BossBarColor.GREEN,
            BossEvent.BossBarOverlay.PROGRESS);
    private static final int DAMAGE_FOCUS_TICKS = 100;
    private static final int CYCLE_INTERVAL_TICKS = 50;

    private static int focusTicks;
    private static int cycleTicks;
    private static int cycleIndex;

    private VillageStructureHud() {
    }

    public static void reset() {
        focusTicks = 0;
        cycleTicks = 0;
        cycleIndex = 0;
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
        if (focusTicks > 0) {
            focusTicks--;
            showToAll(server);
            return;
        }

        if (!VillageRaidSystem.isActive()) {
            hide();
            return;
        }

        cycleTicks++;
        if (cycleTicks >= CYCLE_INTERVAL_TICKS || !HEALTH_BAR.isVisible()) {
            cycleTicks = 0;
            VillageProgressionSystem.Building building = nextBuilding();
            updateBar(
                    building,
                    VillageProgressionSystem.durability(building),
                    VillageProgressionSystem.maxDurability(building));
        }
        showToAll(server);
    }

    private static VillageProgressionSystem.Building nextBuilding() {
        VillageProgressionSystem.Building[] buildings = VillageProgressionSystem.Building.values();
        VillageProgressionSystem.Building mostDamaged = buildings[0];
        float lowestRatio = 2.0f;
        for (VillageProgressionSystem.Building building : buildings) {
            int maximum = Math.max(1, VillageProgressionSystem.maxDurability(building));
            float ratio = VillageProgressionSystem.durability(building) / (float) maximum;
            if (ratio < lowestRatio) {
                lowestRatio = ratio;
                mostDamaged = building;
            }
        }
        if (lowestRatio < 0.999f) {
            return mostDamaged;
        }
        VillageProgressionSystem.Building selected = buildings[Math.floorMod(cycleIndex, buildings.length)];
        cycleIndex++;
        return selected;
    }

    private static void updateBar(
            VillageProgressionSystem.Building building,
            int current,
            int maximum) {
        float progress = maximum <= 0 ? 0.0f : Math.max(0.0f, Math.min(1.0f, current / (float) maximum));
        HEALTH_BAR.setName(Component.literal(
                "방어 시설 · " + building.displayName() + "  " + current + " / " + maximum));
        HEALTH_BAR.setProgress(progress);
        HEALTH_BAR.setColor(progress <= 0.25f
                ? BossEvent.BossBarColor.RED
                : progress <= 0.55f
                ? BossEvent.BossBarColor.YELLOW
                : BossEvent.BossBarColor.GREEN);
    }

    private static void showToAll(MinecraftServer server) {
        HEALTH_BAR.setVisible(true);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            HEALTH_BAR.addPlayer(player);
        }
    }

    private static void hide() {
        if (HEALTH_BAR.isVisible()) {
            HEALTH_BAR.setVisible(false);
            HEALTH_BAR.removeAllPlayers();
        }
    }
}
