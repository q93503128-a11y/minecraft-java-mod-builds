package io.github.q93503128.turnbound.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.q93503128.turnbound.Turnbound;
import io.github.q93503128.turnbound.world.FieldUiSnapshot;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.api.v2.common.waypoint.WaypointFactory;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.Objects;

@JourneyMapPlugin(apiVersion = "2.0.0")
public final class TurnboundJourneyMapPlugin implements IClientPlugin {
    private static IClientAPI api;
    private static int fingerprint;
    private static KeyMapping fullscreenKey;

    @Override
    public void initialize(IClientAPI jmClientApi) {
        api = jmClientApi;
        rebindFullscreenToM();
        sync(ClientFieldState.snapshot());
    }

    @Override
    public String getModId() { return Turnbound.MOD_ID; }

    public static void sync(FieldUiSnapshot snapshot) {
        if (api == null || snapshot == null) return;
        int next = Objects.hash(snapshot.active(), snapshot.mapPoints());
        if (next == fingerprint) return;
        fingerprint = next;
        try {
            api.removeAllWaypoints(Turnbound.MOD_ID);
            if (!snapshot.active()) return;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) return;
            int y = minecraft.player.blockPosition().getY();
            for (FieldUiSnapshot.MapPoint point : snapshot.mapPoints()) {
                if (!point.active()) continue;
                Waypoint waypoint = WaypointFactory.createWaypoint(
                        Turnbound.MOD_ID,
                        new BlockPos((int)Math.floor(point.x()), y, (int)Math.floor(point.z())),
                        point.label(),
                        Level.OVERWORLD,
                        false);
                int color = waypointColor(point);
                waypoint.setColor(color);
                waypoint.setIconColor(color);
                waypoint.setShowBeacon(false);
                waypoint.setShowInWorld(false);
                waypoint.setShowOnMap(true);
                waypoint.setShowLabel(true);
                waypoint.setShowDeviation(false);
                waypoint.setDescription(point.objective() ? "TURNBOUND 목표" : "TURNBOUND " + point.kind());
                api.addWaypoint(Turnbound.MOD_ID, waypoint);
            }
        } catch (Throwable throwable) {
            Turnbound.LOGGER.warn("TURNBOUND could not sync JourneyMap waypoints", throwable);
        }
    }

    public static void toggleMinimap() {
        if (api != null) api.toggleMinimap(!api.minimapEnabled());
    }

    public static boolean openFullscreenMap() {
        rebindFullscreenToM();
        if (fullscreenKey == null) return false;
        KeyMapping.click(fullscreenKey.getKey());
        return true;
    }

    private static void rebindFullscreenToM() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options == null) return;
        for (KeyMapping mapping : minecraft.options.keyMappings) {
            String name = mapping.getName().toLowerCase(Locale.ROOT);
            if (!name.contains("journeymap")) continue;
            if (!name.contains("fullscreen") && !name.contains("full_map") && !name.contains("map.full")) continue;
            fullscreenKey = mapping;
            InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_M);
            if (!mapping.getKey().equals(key)) {
                mapping.setKey(key);
                KeyMapping.resetMapping();
                minecraft.options.save();
            }
            return;
        }
    }

    private static int waypointColor(FieldUiSnapshot.MapPoint point) {
        if (point.objective() || "QUEST".equals(point.kind())) return 0xFFD35A;
        return switch (point.kind()) {
            case "SERVICE" -> 0x62D39A;
            case "SECRET" -> 0xC794FF;
            default -> 0x6DC6FF;
        };
    }
}
