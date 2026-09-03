package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.AwakeningRouteRules;

import java.util.UUID;

/** Server-side gate that prevents E-menu shortcuts from bypassing canonical chapter unlocks. */
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
}
