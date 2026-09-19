package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative physical facility gate for economy actions. */
public final class MetaFacilityActionGate {
    private MetaFacilityActionGate() {}

    public static String denial(ServerPlayer player, String rawCommand) {
        if (player == null || rawCommand == null || rawCommand.isBlank()) return "";
        String action = rawCommand.split("\\|", -1)[0];
        return switch (action) {
            case "SUMMON1", "SUMMON10", "STARTER" ->
                    nearDrabyel(player, "ARCHIVE") || nearLegacy(player, -56.0, 8.0, 22.0)
                            ? "" : "소환은 정령 소환 시설에서만 이용할 수 있습니다.";
            case "BUY" ->
                    nearDrabyel(player, "MARKET") || nearLegacy(player, -57.0, 55.0, 24.0)
                            ? "" : "구매는 장비 상인과 대화 중일 때만 이용할 수 있습니다.";
            case "ENHANCE" ->
                    nearDrabyel(player, "FORGE") || nearLegacy(player, 56.0, 8.0, 22.0)
                            ? "" : "장비 강화는 대장장이와 대화 중일 때만 이용할 수 있습니다.";
            default -> "";
        };
    }

    private static boolean nearDrabyel(ServerPlayer player, String facilityHint) {
        return ExternalWorldBootstrap.active(player) && DrabyelHubServiceRuntime.nearFacility(player, facilityHint);
    }

    private static boolean nearLegacy(ServerPlayer player, double x, double z, double radius) {
        if (!RadiaHubSessionManager.active(player)) return false;
        double dx = player.position().x - x;
        double dz = player.position().z - z;
        return dx * dx + dz * dz <= radius * radius;
    }
}
