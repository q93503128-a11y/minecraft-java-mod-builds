package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;

/** Physical Radia facility gate kept separate from pure progression rules and their unit-test classpath. */
public final class MetaFacilityActionGate {
    private MetaFacilityActionGate() {}

    public static String denial(ServerPlayer player, String rawCommand) {
        if (player == null || rawCommand == null || rawCommand.isBlank()) return "";
        String action = rawCommand.split("\\|", -1)[0];
        return switch (action) {
            case "SUMMON1", "SUMMON10", "STARTER" -> near(player, -56.0, 8.0, 22.0)
                    ? "" : "소환은 라디아 Echo Archive에서만 이용할 수 있습니다.";
            case "BUY" -> near(player, -57.0, 55.0, 24.0)
                    ? "" : "상점은 라디아 Market Row에서만 이용할 수 있습니다.";
            case "ENHANCE" -> near(player, 56.0, 8.0, 22.0)
                    ? "" : "장비 강화는 라디아 Forge Annex에서만 이용할 수 있습니다.";
            default -> "";
        };
    }

    private static boolean near(ServerPlayer player, double x, double z, double radius) {
        if (!RadiaHubSessionManager.active(player)) return false;
        double dx = player.position().x - x;
        double dz = player.position().z - z;
        return dx * dx + dz * dz <= radius * radius;
    }
}
