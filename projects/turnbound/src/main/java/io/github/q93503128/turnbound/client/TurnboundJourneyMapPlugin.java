package io.github.q93503128.turnbound.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.q93503128.turnbound.Turnbound;
import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import io.github.q93503128.turnbound.network.MetaCommandPayload;
import io.github.q93503128.turnbound.world.FieldUiSnapshot;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.Context;
import journeymap.api.v2.client.display.DisplayType;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.event.DisplayUpdateEvent;
import journeymap.api.v2.client.event.EntityRadarUpdateEvent;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.client.model.TextProperties;
import journeymap.api.v2.client.util.UIState;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.ClientEventRegistry;
import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.api.v2.common.waypoint.WaypointFactory;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@JourneyMapPlugin(apiVersion = "2.0.0")
public final class TurnboundJourneyMapPlugin implements IClientPlugin {
    private static final String FIELD_ENEMY_TAG = "turnbound_drehmal_field_enemy";
    private static final List<MarkerOverlay> LABEL_OVERLAYS = new ArrayList<>();

    private static IClientAPI api;
    private static int fingerprint;
    private static KeyMapping fullscreenKey;
    private static boolean fullscreenActive;

    @Override
    public void initialize(IClientAPI jmClientApi) {
        api = jmClientApi;
        ClientEventRegistry.DISPLAY_UPDATE_EVENT.subscribe(Turnbound.MOD_ID, TurnboundJourneyMapPlugin::onDisplayUpdate);
        ClientEventRegistry.ENTITY_RADAR_UPDATE_EVENT.subscribe(Turnbound.MOD_ID, TurnboundJourneyMapPlugin::onRadarUpdate);
        findFullscreenKey();
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
            api.removeAll(Turnbound.MOD_ID);
            api.removeAllWaypoints(Turnbound.MOD_ID);
            LABEL_OVERLAYS.clear();
            if (!snapshot.active()) return;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) return;
            int y = minecraft.player.blockPosition().getY();
            boolean customMarkers = api.playerAccepts(Turnbound.MOD_ID, DisplayType.Marker);
            UIState fullscreenState = api.getUIState(Context.UI.Fullscreen);
            float scale = labelScale(fullscreenState);

            for (FieldUiSnapshot.MapPoint point : snapshot.mapPoints()) {
                if (!point.active()) continue;
                String label = point.objective() ? "★ " + point.label() : point.label();
                BlockPos pos = new BlockPos((int)Math.floor(point.x()), y, (int)Math.floor(point.z()));
                Waypoint waypoint = WaypointFactory.createWaypoint(
                        Turnbound.MOD_ID,
                        pos,
                        label,
                        Level.OVERWORLD,
                        true);
                int color = waypointColor(point);
                waypoint.setColor(color);
                waypoint.setIconIdentifier(Identifier.fromNamespaceAndPath("minecraft", "textures/block/white_wool.png"));
                waypoint.setIconTextureSize(16, 16);
                waypoint.setIconColor(color);
                waypoint.setLabelColor(point.objective() ? 0xFFE7A3 : 0xFFFFFF);
                waypoint.setShowBeacon(false);
                waypoint.setShowInWorld(false);
                waypoint.setShowOnMap(true);
                waypoint.setShowLabel(!customMarkers);
                waypoint.setShowDeviation(false);
                waypoint.setDescription(point.objective() ? "TURNBOUND 목표" : waypointDescription(point));
                api.addWaypoint(Turnbound.MOD_ID, waypoint);

                if (customMarkers) {
                    MarkerOverlay marker = labelOverlay(pos, label, point, scale);
                    api.show(marker);
                    LABEL_OVERLAYS.add(marker);
                }
            }
            Turnbound.LOGGER.info("TURNBOUND JourneyMap synced {} map point(s); custom markers={}",
                    snapshot.mapPoints().size(), customMarkers);
        } catch (Throwable throwable) {
            Turnbound.LOGGER.warn("TURNBOUND could not sync JourneyMap markers", throwable);
        }
    }

    public static void toggleMinimap() {
        if (api != null) api.toggleMinimap(!api.minimapEnabled());
    }

    public static boolean minimapEnabled() {
        return api != null && api.minimapEnabled();
    }

    public static boolean openFullscreenMap() {
        if (api == null) return false;
        findFullscreenKey();
        if (fullscreenKey == null) return false;
        KeyMapping.click(fullscreenKey.getKey());
        return true;
    }

    private static MarkerOverlay labelOverlay(
            BlockPos pos, String label, FieldUiSnapshot.MapPoint point, float scale
    ) {
        int iconSize = point.objective() ? 16 : 11;
        MapImage icon = new MapImage(
                Identifier.fromNamespaceAndPath("minecraft", "textures/block/white_wool.png"),
                16, 16);
        icon.setColor(waypointColor(point) & 0xFFFFFF)
                .setOpacity(1.0F)
                .setDisplayWidth(iconSize)
                .setDisplayHeight(iconSize)
                .setRotation(45)
                .centerAnchors()
                .setBlur(false);

        MarkerOverlay marker = new MarkerOverlay(Turnbound.MOD_ID, pos, icon);
        marker.setDimension(Level.OVERWORLD);
        marker.setTitle(label);
        marker.setLabel(label);
        marker.setOverlayGroupName("TURNBOUND");
        marker.setDisplayOrder(point.objective() ? 2400 : 2200);
        marker.setActiveUIs(Context.UI.Fullscreen);
        marker.setMinZoom(UIState.FULLSCREEN_ZOOM_MIN);
        marker.setMaxZoom(UIState.ZOOM_IN_MAX);
        marker.setTextProperties(new TextProperties()
                .setScale(scale)
                .setColor(point.objective() ? 0xFFE7A3 : 0xFFFFFF)
                .setBackgroundColor(0x101318)
                .setBackgroundOpacity(point.objective() ? 0.84F : 0.72F)
                .setFontShadow(true)
                .setOffsetY(point.objective() ? -16 : -12)
                .setActiveUIs(Context.UI.Fullscreen));
        return marker;
    }

    private static void onDisplayUpdate(DisplayUpdateEvent event) {
        if (event == null || event.uiState == null || event.uiState.ui != Context.UI.Fullscreen) return;
        boolean activeNow = event.uiState.active;
        if (activeNow && !fullscreenActive) {
            ClientPacketDistributor.sendToServer(new MetaCommandPayload("HUB_ROUTE_REVIEW"));
        }
        fullscreenActive = activeNow;
        float scale = labelScale(event.uiState);
        for (MarkerOverlay marker : List.copyOf(LABEL_OVERLAYS)) {
            if (Math.abs(marker.getTextProperties().getScale() - scale) < 0.01F) continue;
            marker.getTextProperties().setScale(scale);
            marker.flagForRerender();
        }
    }

    private static float labelScale(UIState state) {
        int zoom = state == null ? 1024 : Math.max(UIState.FULLSCREEN_ZOOM_MIN, state.zoom);
        if (zoom >= 4096) return 4.0F;
        return 2.0F;
    }

    private static void onRadarUpdate(EntityRadarUpdateEvent event) {
        if (event == null || event.getType() != EntityRadarUpdateEvent.EntityType.MOB) return;
        Entity entity = event.getWrappedEntity().getEntityRef().get();
        if (entity instanceof BattleActorEntity && entity.entityTags().contains(FIELD_ENEMY_TAG)) {
            event.getWrappedEntity().setColor(0xE65C5C);
            event.getWrappedEntity().setLabelColor(0xFFF0F0);
            return;
        }
        event.cancel();
    }

    private static void findFullscreenKey() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options == null) return;
        fullscreenKey = null;
        for (KeyMapping mapping : minecraft.options.keyMappings) {
            String name = mapping.getName().toLowerCase(Locale.ROOT);
            if (!name.contains("journeymap")) continue;
            if (!name.contains("fullscreen") && !name.contains("full_map") && !name.contains("map.full")) continue;
            fullscreenKey = mapping;
            InputConstants.Key reservedMinimapKey = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_M);
            if (mapping.getKey().equals(reservedMinimapKey)) {
                // Build #1013 temporarily forced fullscreen JourneyMap onto M. M is now TURNBOUND's minimap toggle,
                // so migrate only that known collision back to JourneyMap's normal full-map key.
                mapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_J));
                KeyMapping.resetMapping();
                minecraft.options.save();
            }
            return;
        }
    }

    private static String waypointDescription(FieldUiSnapshot.MapPoint point) {
        return switch (point.kind()) {
            case "SERVICE" -> "TURNBOUND 시설";
            case "SECRET" -> "TURNBOUND 발견";
            case "QUEST" -> "TURNBOUND 목표";
            default -> "TURNBOUND 위치";
        };
    }

    private static int waypointColor(FieldUiSnapshot.MapPoint point) {
        if (point.objective() || "QUEST".equals(point.kind())) return 0xFFE07A;
        return switch (point.kind()) {
            case "SERVICE" -> 0xFFF2D5;
            case "NPC" -> 0xFFEFFB;
            case "SECRET" -> 0xFFD4B4FF;
            default -> 0xFFBFE8FF;
        };
    }
}
