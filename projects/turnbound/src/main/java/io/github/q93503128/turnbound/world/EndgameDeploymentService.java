package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import io.github.q93503128.turnbound.combat.EndgameEncounterCatalog;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Server-authoritative contract for Radia's replay/endgame UI.
 * START requests a briefing; DEPLOY commits the already-reviewed encounter into battle.
 */
public final class EndgameDeploymentService {
    private EndgameDeploymentService() {}

    static boolean normalBossRematch(String encounterId) {
        if (!EndgameDeploymentIdRules.normalBossRematch(encounterId)) return false;
        if (!CampaignEncounterCatalog.contains(encounterId)) return false;
        return CampaignEncounterCatalog.spec(encounterId).boss();
    }

    static boolean supported(String encounterId) {
        if (!EndgameDeploymentIdRules.supported(encounterId)) return false;
        return normalBossRematch(encounterId) || EndgameEncounterCatalog.contains(encounterId);
    }

    static boolean unlocked(UUID playerId, String encounterId) {
        if (playerId == null || !supported(encounterId)) return false;
        if (normalBossRematch(encounterId)) {
            return CampaignProgressStore.snapshot(playerId).clearedEncounters().contains(encounterId);
        }
        return EndgameEncounterCatalog.unlocked(playerId, encounterId);
    }

    public static boolean brief(ServerPlayer player, String encounterId) {
        String denial = denial(player, encounterId);
        if (!denial.isBlank()) {
            deny(player, denial);
            return false;
        }
        EndgameBriefing.send(player, EndgameBriefing.build(player.getUUID(), encounterId));
        return true;
    }

    public static boolean deploy(ServerPlayer player, String encounterId) {
        String denial = denial(player, encounterId);
        if (!denial.isBlank()) {
            deny(player, denial);
            return false;
        }

        // Normal rematches occur after B01+, so the story's Auto/2x unlock is already available.
        // Hard/Rift override these booleans inside BattleSessionManager with their canonical rules.
        BattleSessionManager.startEncounter(player, encounterId, true, true);
        return true;
    }

    private static String denial(ServerPlayer player, String encounterId) {
        if (player == null) return "출전 정보를 확인할 수 없습니다.";
        if (!supported(encounterId)) return "지원하지 않는 도전 전투입니다.";
        if (BattleSessionManager.exists(player)) return "이미 진행 중인 전투가 있습니다.";
        if (!RadiaHubSessionManager.active(player) || !inRadiaBounds(player)) {
            return "보스 재전과 균열 관문은 라디아에서만 시작할 수 있습니다.";
        }
        if (!unlocked(player.getUUID(), encounterId)) return "아직 잠긴 도전 전투입니다.";
        return "";
    }

    private static boolean inRadiaBounds(ServerPlayer player) {
        return player.getX() >= -128 && player.getX() <= 128
                && player.getZ() >= -112 && player.getZ() <= 128;
    }

    private static void deny(ServerPlayer player, String reason) {
        player.sendSystemMessage(Component.literal("TURNBOUND · " + reason));
        MetaNetwork.sync(player);
    }
}
