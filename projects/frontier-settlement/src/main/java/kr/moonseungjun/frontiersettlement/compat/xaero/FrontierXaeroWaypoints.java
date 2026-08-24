package kr.moonseungjun.frontiersettlement.compat.xaero;

import kr.moonseungjun.frontiersettlement.client.ClientSettlementState;
import kr.moonseungjun.frontiersettlement.client.SettlementNoticeQueue;
import kr.moonseungjun.frontiersettlement.network.SettlementContextPayload;
import kr.moonseungjun.frontiersettlement.network.SettlementContextTarget;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaero.hud.minimap.waypoint.WaypointColor;

/**
 * Optional Xaero custom-waypoint bridge. The class is registered only when Xaero is actually loaded.
 * It owns only Xaero's custom waypoint table for Frontier and never mutates settlement authority.
 */
public final class FrontierXaeroWaypoints {
    private static final String MOD_KEY = "frontier_settlement";
    private static final int RESYNC_TICKS = 20;
    private static int cooldown;
    private static int lastFingerprint = Integer.MIN_VALUE;
    private static boolean populated;
    private static boolean disabled;

    private FrontierXaeroWaypoints() {}

    public static void tick(ClientTickEvent.Post event) {
        if (disabled) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            if (populated) clearOwnedWaypoints();
            populated = false;
            lastFingerprint = Integer.MIN_VALUE;
            cooldown = 0;
            return;
        }
        if (cooldown-- > 0) return;
        cooldown = RESYNC_TICKS;

        SettlementContextPayload context = ClientSettlementState.context();
        int fingerprint = markerFingerprint(context);
        if (fingerprint == lastFingerprint) return;

        try {
            var customWaypoints = WaypointsManager.getCustomWaypoints(MOD_KEY);
            if (customWaypoints == null) return;
            customWaypoints.clear();
            for (SettlementContextTarget target : context.targets()) {
                if (!"stockpile".equals(target.kind()) && !"outpost".equals(target.kind())) continue;
                boolean settlement = "stockpile".equals(target.kind());
                String name = settlement ? "개척마을" : target.title();
                String symbol = settlement ? "M" : "O";
                customWaypoints.put(target.key(), new Waypoint(
                        target.markerX(), target.markerY(), target.markerZ(),
                        name, symbol, WaypointColor.GRAY));
            }
            populated = !customWaypoints.isEmpty();
            lastFingerprint = fingerprint;
        } catch (LinkageError error) {
            disabled = true;
            SettlementNoticeQueue.push("Xaero 마커 연동 비활성 · 버전 확인 필요");
        }
    }

    private static void clearOwnedWaypoints() {
        try {
            var customWaypoints = WaypointsManager.getCustomWaypoints(MOD_KEY);
            if (customWaypoints != null) customWaypoints.clear();
        } catch (LinkageError ignored) {
            disabled = true;
        }
    }

    private static int markerFingerprint(SettlementContextPayload context) {
        int hash = 1;
        for (SettlementContextTarget target : context.targets()) {
            if (!"stockpile".equals(target.kind()) && !"outpost".equals(target.kind())) continue;
            hash = 31 * hash + target.key().hashCode();
            hash = 31 * hash + target.markerX();
            hash = 31 * hash + target.markerY();
            hash = 31 * hash + target.markerZ();
            hash = 31 * hash + target.title().hashCode();
            hash = 31 * hash + target.detail().hashCode();
        }
        return hash;
    }
}
