package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.Turnbound;
import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import io.github.q93503128.turnbound.combat.EndgameEncounterCatalog;
import io.github.q93503128.turnbound.world.CampaignPersistence;
import io.github.q93503128.turnbound.world.FieldSessionManager;
import io.github.q93503128.turnbound.world.RewardGrantService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BattleSessionManager {
    private static final Map<UUID, BattleSession> SESSIONS = new HashMap<>();

    private BattleSessionManager() {}

    public static void start(ServerPlayer player) {
        if (!endAndPersist(player)) return;
        BattleSession session = new BattleSession(player);
        SESSIONS.put(player.getUUID(), session);
        BattleNetwork.sync(player, session);
    }

    public static void startEncounter(ServerPlayer player, String encounterId) {
        startEncounter(player, encounterId, false, false);
    }

    public static void startEncounter(ServerPlayer player, String encounterId, boolean autoAllowed, boolean speedAllowed) {
        if (!endAndPersist(player)) return;
        boolean endgame = EndgameEncounterCatalog.contains(encounterId);
        if (endgame && !EndgameEncounterCatalog.unlocked(player.getUUID(), encounterId)) {
            throw new IllegalStateException("Endgame encounter is locked: " + encounterId);
        }
        boolean resolvedAuto = endgame ? EndgameEncounterCatalog.autoAllowed(encounterId) : autoAllowed;
        boolean resolvedSpeed = endgame ? EndgameEncounterCatalog.speedAllowed(encounterId) : speedAllowed;
        boolean fleeAllowed = endgame ? EndgameEncounterCatalog.fleeAllowed(encounterId) : !CampaignEncounterCatalog.spec(encounterId).boss();
        BattleSession session = new BattleSession(player, encounterId, resolvedAuto, resolvedSpeed, fleeAllowed);
        SESSIONS.put(player.getUUID(), session);
        BattleNetwork.sync(player, session);
    }

    public static boolean startEncounterAt(ServerPlayer player, String encounterId, boolean autoAllowed, boolean speedAllowed,
                                           Vec3 center, float yaw) {
        BattleArenaLocator.Arena arena = BattleArenaLocator.fixedIfOpen(player, center, yaw);
        if (arena == null || !endAndPersist(player)) return false;
        boolean endgame = EndgameEncounterCatalog.contains(encounterId);
        if (endgame && !EndgameEncounterCatalog.unlocked(player.getUUID(), encounterId)) return false;
        boolean resolvedAuto = endgame ? EndgameEncounterCatalog.autoAllowed(encounterId) : autoAllowed;
        boolean resolvedSpeed = endgame ? EndgameEncounterCatalog.speedAllowed(encounterId) : speedAllowed;
        boolean fleeAllowed = endgame ? EndgameEncounterCatalog.fleeAllowed(encounterId) : !CampaignEncounterCatalog.spec(encounterId).boss();
        BattleSession session = new BattleSession(player, encounterId, resolvedAuto, resolvedSpeed, fleeAllowed, arena);
        SESSIONS.put(player.getUUID(), session);
        BattleNetwork.sync(player, session);
        return true;
    }

    public static boolean active(ServerPlayer player) {
        BattleSession session = SESSIONS.get(player.getUUID());
        return session != null && !session.finished();
    }

    public static boolean exists(ServerPlayer player) { return SESSIONS.containsKey(player.getUUID()); }

    public static void tick(ServerPlayer player) {
        BattleSession session = SESSIONS.get(player.getUUID());
        if (session != null) {
            session.tick(player);
            if (player.tickCount % 5 == 0) BattleNetwork.sync(player, session);
        }
    }

    public static void command(ServerPlayer player, String command) {
        BattleSession session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        String[] parts = command.split("\\|", -1);
        switch (parts[0]) {
            case "ACT" -> { if (parts.length >= 4) session.action(player, parts[1], parts[2], parts[3]); }
            case "FOCUS" -> session.focusTarget(player, parts.length >= 2 ? parts[1] : "");
            case "AUTO" -> session.toggleAuto(player);
            case "SPEED" -> session.toggleSpeed(player);
            case "FLEE" -> { if (session.finished() || session.fleeAllowed()) end(player); }
            default -> { }
        }
    }

    public static void end(ServerPlayer player) {
        endAndPersist(player);
    }

    private static boolean endAndPersist(ServerPlayer player) {
        BattleSession old = SESSIONS.get(player.getUUID());
        if (old != null) {
            String encounterId = old.encounterId();
            BattleOutcome outcome = old.state().outcome();
            if (!encounterId.isBlank()) {
                if (outcome == BattleOutcome.ALLY_VICTORY) {
                    try {
                        RewardGrantService.commitAndSave(player, old.rewardTransactionId(), encounterId, old.state(), outcome);
                        CampaignPersistence.saveIfDirty(player);
                    } catch (RuntimeException ex) {
                        Turnbound.LOGGER.error("TURNBOUND failed to settle battle reward transaction {} for {}",
                                old.rewardTransactionId(), player.getUUID(), ex);
                        player.sendSystemMessage(Component.literal(
                                "TURNBOUND 전투 보상을 안전하게 저장하지 못했습니다. 잠시 후 다시 나가기를 시도해 주세요."));
                        BattleNetwork.sync(player, old);
                        return false;
                    }
                } else {
                    CampaignPersistence.saveIfDirty(player);
                }
            }
            SESSIONS.remove(player.getUUID());
            old.cleanup(player);
            if (!encounterId.isBlank() && CampaignEncounterCatalog.contains(encounterId)) {
                FieldSessionManager.onBattleEnded(player, encounterId, outcome);
            }
        }
        BattleNetwork.close(player);
        return true;
    }

    public static void clearAll(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) end(player);
    }
}
