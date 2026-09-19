package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.AwakeningRouteRules;

import java.util.UUID;

/** Server-side progression gate for meta actions; intentionally Minecraft-class-free for pure unit tests. */
public final class MetaActionGate {
    private MetaActionGate() {}

    /**
     * Runtime-aware meta gate. Drehmal production must never inherit retired Aster chapter requirements simply
     * because the underlying account/progression model is shared.
     */
    public static String denial(net.minecraft.server.level.ServerPlayer player, String rawCommand) {
        if (player == null) return "";
        if (!ExternalWorldBootstrap.active(player)) return denial(player.getUUID(), rawCommand);
        if (rawCommand == null || rawCommand.isBlank()) return "";

        String action = rawCommand.split("\\|", -1)[0];
        if ("SUMMON1".equals(action) || "SUMMON10".equals(action) || "STARTER".equals(action)) {
            return DrehmalContentUnlocks.summonUnlocked(player.getUUID())
                    ? ""
                    : "소환은 Capital Valley의 첫 강적 또는 New Drabyel 진입로 전투를 넘은 뒤 개방됩니다.";
        }
        if ("ENHANCE".equals(action)) {
            // New Drabyel onboarding explicitly includes equipment/upgrade; the physical blacksmith gate remains.
            return "";
        }
        return denial(player.getUUID(), rawCommand);
    }

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
