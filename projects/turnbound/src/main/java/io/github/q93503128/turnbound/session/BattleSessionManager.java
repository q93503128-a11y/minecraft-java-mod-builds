package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.Turnbound;
import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import io.github.q93503128.turnbound.combat.EndgameEncounterCatalog;
import io.github.q93503128.turnbound.presentation.PersonalPresentationIsolation;
import io.github.q93503128.turnbound.world.CampaignPersistence;
import io.github.q93503128.turnbound.world.RewardGrantService;
import io.github.q93503128.turnbound.world.WorldSessionRouter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class BattleSessionManager {
    private static final Map<UUID, BattleSession> SESSIONS = new HashMap<>();

    private BattleSessionManager() {}

    public static void start(ServerPlayer player) {
        if (!endAndPersist(player, false)) return;
        BattleSession session = privateSession(player, () -> new BattleSession(player));
        SESSIONS.put(player.getUUID(), session);
        BattleNetwork.sync(player, session);
    }

    public static void startEncounter(ServerPlayer player, String encounterId) {
        startEncounter(player, encounterId, false, false);
    }

    public static void startEncounter(ServerPlayer player, String encounterId, boolean autoAllowed, boolean speedAllowed) {
        if (!endAndPersist(player, false)) return;
        boolean endgame = EndgameEncounterCatalog.contains(encounterId);
        if (endgame && !EndgameEncounterCatalog.unlocked(player.getUUID(), encounterId)) {
            throw new IllegalStateException("Endgame encounter is locked: " + encounterId);
        }
        boolean resolvedAuto = endgame ? EndgameEncounterCatalog.autoAllowed(encounterId) : autoAllowed;
        boolean resolvedSpeed = endgame ? EndgameEncounterCatalog.speedAllowed(encounterId) : speedAllowed;
        boolean fleeAllowed = endgame ? EndgameEncounterCatalog.fleeAllowed(encounterId) : !CampaignEncounterCatalog.spec(encounterId).boss();
        BattleSession session = privateSession(player,
                () -> new BattleSession(player, encounterId, resolvedAuto, resolvedSpeed, fleeAllowed));
        SESSIONS.put(player.getUUID(), session);
        BattleNetwork.sync(player, session);
    }

    public static boolean startEncounterAt(ServerPlayer player, String encounterId, boolean autoAllowed, boolean speedAllowed,
                                           Vec3 center, float yaw) {
        BattleArenaLocator.Arena arena = BattleArenaLocator.fixedIfOpen(player, center, yaw);
        if (arena == null || !endAndPersist(player, false)) return false;
        boolean endgame = EndgameEncounterCatalog.contains(encounterId);
        if (endgame && !EndgameEncounterCatalog.unlocked(player.getUUID(), encounterId)) return false;
        boolean resolvedAuto = endgame ? EndgameEncounterCatalog.autoAllowed(encounterId) : autoAllowed;
        boolean resolvedSpeed = endgame ? EndgameEncounterCatalog.speedAllowed(encounterId) : speedAllowed;
        boolean fleeAllowed = endgame ? EndgameEncounterCatalog.fleeAllowed(encounterId) : !CampaignEncounterCatalog.spec(encounterId).boss();
        BattleSession session = privateSession(player,
                () -> new BattleSession(player, encounterId, resolvedAuto, resolvedSpeed, fleeAllowed, arena));
        SESSIONS.put(player.getUUID(), session);
        BattleNetwork.sync(player, session);
        return true;
    }

    public static boolean active(ServerPlayer player) {
        BattleSession session = SESSIONS.get(player.getUUID());
        return session != null && !session.finished();
    }

    public static boolean exists(ServerPlayer player) { return SESSIONS.containsKey(player.getUUID()); }
    public static boolean finished(ServerPlayer player) {
        BattleSession session = player == null ? null : SESSIONS.get(player.getUUID());
        return session != null && session.finished();
    }

    public static boolean resumeIfPresent(ServerPlayer player) {
        BattleSession session = SESSIONS.get(player.getUUID());
        if (session == null) return false;
        Vec3 anchor = session.battleAnchor();
        player.setPos(anchor.x, anchor.y, anchor.z);
        player.setYRot(session.battleYaw());
        player.setXRot(18.0F);
        player.setDeltaMovement(Vec3.ZERO);
        BattleNetwork.sync(player, session);
        return true;
    }

    public static void tick(ServerPlayer player) {
        BattleSession session = SESSIONS.get(player.getUUID());
        if (session != null) {
            PersonalPresentationIsolation.withPrivateActorOwner(player.getUUID(), () -> session.tick(player));
            if (player.tickCount % 5 == 0) BattleNetwork.sync(player, session);
        }
    }

    public static void command(ServerPlayer player, String command) {
        BattleSession session = SESSIONS.get(player.getUUID());
        if (session == null || command == null) return;
        String[] parts = command.split("\\|", -1);

        // Battle exit can rebuild shared field actors and world state. Never let that work inherit the private
        // battle-presentation owner or the respawned field silhouettes could become visible only to this player.
        if ("FLEE".equals(parts[0])) {
            if (session.finished() || session.fleeAllowed()) end(player);
            return;
        }

        PersonalPresentationIsolation.withPrivateActorOwner(player.getUUID(), () -> {
            switch (parts[0]) {
                case "ACT" -> { if (parts.length >= 4) session.action(player, parts[1], parts[2], parts[3]); }
                case "FOCUS" -> session.focusTarget(player, parts.length >= 2 ? parts[1] : "");
                case "AUTO" -> session.toggleAuto(player);
                case "SPEED" -> session.toggleSpeed(player);
                default -> { }
            }
        });
    }

    public static void end(ServerPlayer player) { endAndPersist(player, false); }
    public static boolean endForLifecycle(ServerPlayer player) { return endAndPersist(player, true); }

    private static boolean endAndPersist(ServerPlayer player, boolean lifecycle) {
        BattleSession old = SESSIONS.get(player.getUUID());
        boolean deferredReward = false;
        if (old != null) {
            String encounterId = old.encounterId();
            BattleOutcome outcome = old.state().outcome();
            if (!encounterId.isBlank()) {
                if (outcome == BattleOutcome.ALLY_VICTORY) {
                    try {
                        RewardGrantService.commitAndSave(player, old.rewardTransactionId(), encounterId, old.state(), outcome);
                        CampaignPersistence.saveIfDirty(player);
                    } catch (RewardGrantService.SettlementException ex) {
                        if (lifecycle && ex.recoverableFromJournal()) {
                            deferredReward = true;
                            Turnbound.LOGGER.warn("TURNBOUND deferred reward transaction {} for {} to the durable journal",
                                    old.rewardTransactionId(), player.getUUID());
                        } else {
                            return settlementFailed(player, old, ex);
                        }
                    } catch (RuntimeException ex) {
                        return settlementFailed(player, old, ex);
                    }
                } else {
                    CampaignPersistence.saveIfDirty(player);
                }
            }

            // From this point onward settlement is complete (or safely journaled for lifecycle shutdown). Never leave
            // the client trapped on the result screen because presentation cleanup or shared-world restoration failed.
            SESSIONS.remove(player.getUUID());
            try {
                PersonalPresentationIsolation.withPrivateActorOwner(player.getUUID(), () -> old.cleanup(player));
            } catch (RuntimeException ex) {
                Turnbound.LOGGER.error("TURNBOUND failed to clean battle presentation for {} after settlement",
                        player.getUUID(), ex);
            }
            if (!deferredReward && !encounterId.isBlank() && CampaignEncounterCatalog.contains(encounterId)) {
                try {
                    WorldSessionRouter.onBattleEnded(player, encounterId, outcome);
                } catch (RuntimeException ex) {
                    Turnbound.LOGGER.error("TURNBOUND failed to restore field state for {} after encounter {}",
                            player.getUUID(), encounterId, ex);
                }
            }
        }
        BattleNetwork.close(player);
        return true;
    }

    private static boolean settlementFailed(ServerPlayer player, BattleSession session, RuntimeException ex) {
        Turnbound.LOGGER.error("TURNBOUND failed to settle battle reward transaction {} for {}",
                session.rewardTransactionId(), player.getUUID(), ex);
        player.sendSystemMessage(Component.literal(
                "TURNBOUND 전투 보상을 안전하게 저장하지 못했습니다. 잠시 후 다시 나가기를 시도해 주세요."));
        BattleNetwork.sync(player, session);
        return false;
    }

    private static BattleSession privateSession(ServerPlayer player, Supplier<BattleSession> factory) {
        BattleSession[] box = new BattleSession[1];
        boolean wasInvisible = player.isInvisible();
        try {
            PersonalPresentationIsolation.withPrivateActorOwner(player.getUUID(), () -> box[0] = factory.get());
            return box[0];
        } finally {
            // BattleSession historically hid the physical player shell for its local third-person camera. Keep the
            // authoritative server visibility unchanged; client rendering now isolates the private battle view.
            player.setInvisible(wasInvisible);
        }
    }

    public static void clearAll(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            if (!endForLifecycle(player)) {
                Turnbound.LOGGER.error("TURNBOUND could not durably settle an in-memory battle while the server was stopping for {}",
                        player.getUUID());
            }
        }
        SESSIONS.clear();
    }
}
