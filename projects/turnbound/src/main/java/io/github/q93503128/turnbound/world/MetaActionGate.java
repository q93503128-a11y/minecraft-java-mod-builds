package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.AwakeningRouteRules;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Server-side gate that prevents management-menu shortcuts from bypassing progression or physical facilities. */
public final class MetaActionGate {
    private MetaActionGate() {}

    /** Empty string means allowed; otherwise returns a player-facing denial reason. */
    public static String denial(UUID playerId, String rawCommand) {
        if (playerId == null || rawCommand == null || rawCommand.isBlank()) return "";
        String[] parts = rawCommand.split("\\|", -1);
        String action = parts[0];
        return switch (action) {
            case "SUMMON1", "SUMMON10", "STARTER" -> CampaignContentUnlocks.archive(playerId)
                    ? "" : "Echo Archive는 B01 그라울 클리어 후 해금됩니다.";
            case "ENHANCE" -> CampaignContentUnlocks.forge(playerId)
                    ? "" : "장비 강화는 Chapter 1 완료 후 Forge Annex에서 해금됩니다.";
            case "AWAKEN" -> parts.length > 1 && AwakeningRouteRules.canonGap(parts[1])
                    ? AwakeningRouteRules.blockReason(parts[1]) : "";
            default -> "";
        };
    }

    /** Runtime gate adds the physical-facility requirement after canonical progression checks. */
    public static String denial(ServerPlayer player, String rawCommand) {
        if (player == null) return "";
        String progression = denial(player.getUUID(), rawCommand);
        if (!progression.isBlank() || rawCommand == null || rawCommand.isBlank()) return progression;
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
