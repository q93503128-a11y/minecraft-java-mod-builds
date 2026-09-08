package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.battle.BattleManager;
import kr.moonseungjun.turnboundre.battle.BattleState;
import kr.moonseungjun.turnboundre.battle.EntityParticipantBinding;
import kr.moonseungjun.turnboundre.progression.BattleRewardSettlementService;
import kr.moonseungjun.turnboundre.progression.PlayerProgress;
import kr.moonseungjun.turnboundre.progression.RewardService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Server-owned terminal presentation queue. A result is published only from settled server facts;
 * the client never re-rolls rewards and never decides when battle ownership is released.
 */
public final class BattleResultPresentationService {
    public record Acknowledgement(boolean accepted, String code) {
        public Acknowledgement { code = code == null ? "" : code; }
    }

    private record Pending(UUID ownerPlayerId, BattleResultNetworkPayloads.ResultView view, boolean sent) {}

    private final BattleManager battles;
    private final Map<UUID, Pending> pending = new HashMap<>();

    public BattleResultPresentationService(BattleManager battles) {
        if (battles == null) throw new IllegalArgumentException("battles required");
        this.battles = battles;
    }

    public boolean hasPending(UUID battleId) {
        return battleId != null && pending.containsKey(battleId);
    }

    /** Publishes a victory only after the persistence-backed settlement has succeeded. */
    public void presentSettlement(
            MinecraftServer server,
            UUID battleId,
            BattleRewardSettlementService.Settlement settlement
    ) {
        if (server == null || battleId == null || settlement == null) {
            throw new IllegalArgumentException("server/battleId/settlement required");
        }
        BattleInstance battle = requireTerminalBattle(battleId, BattleInstance.Outcome.VICTORY);
        RewardService.RewardGrant grant = settlement.grant();
        PlayerProgress state = settlement.state();
        List<BattleResultNetworkPayloads.ShardView> shards = grant.shards().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new BattleResultNetworkPayloads.ShardView(
                        entry.getKey(), entry.getValue(), state.shards().getOrDefault(entry.getKey(), 0)))
                .toList();
        BattleResultNetworkPayloads.ResultView view = new BattleResultNetworkPayloads.ResultView(
                battleId,
                battle.revision(),
                "VICTORY",
                grant.coin(),
                grant.essence(),
                state.coin(),
                state.essence(),
                shards);
        putAndSend(server, settlement.ownerPlayerId(), view);
    }

    /** Defeat or rewardless/debug victory. This never fabricates a reward table or roll. */
    public void presentRewardlessTerminal(MinecraftServer server, UUID battleId) {
        if (server == null || battleId == null) throw new IllegalArgumentException("server/battleId required");
        BattleInstance battle = battles.battle(battleId).orElseThrow(() ->
                new IllegalArgumentException("unknown battle " + battleId));
        if (battle.state() != BattleState.REWARD || battle.outcome() == BattleInstance.Outcome.ONGOING) {
            throw new IllegalStateException("battle is not terminal REWARD: " + battleId);
        }
        Optional<UUID> owner = presentationOwner(server, battleId);
        if (owner.isEmpty()) return;
        BattleResultNetworkPayloads.ResultView view = new BattleResultNetworkPayloads.ResultView(
                battleId,
                battle.revision(),
                battle.outcome().name(),
                0, 0, 0, 0, List.of());
        putAndSend(server, owner.get(), view);
    }

    public void trySend(MinecraftServer server, UUID battleId) {
        if (server == null || battleId == null) return;
        Pending current = pending.get(battleId);
        if (current == null || current.sent()) return;
        ServerPlayer player = server.getPlayerList().getPlayer(current.ownerPlayerId());
        if (player == null) return;
        PacketDistributor.sendToPlayer(player, BattleResultNetworkPayloads.ResultS2C.from(current.view()));
        pending.put(battleId, new Pending(current.ownerPlayerId(), current.view(), true));
    }

    public Acknowledgement acknowledge(
            MinecraftServer server,
            UUID playerId,
            BattleResultNetworkPayloads.DecodedAcknowledgement acknowledgement
    ) {
        if (server == null || playerId == null || acknowledgement == null) {
            return new Acknowledgement(false, "INVALID_REQUEST");
        }
        Pending current = pending.get(acknowledgement.battleId());
        if (current == null) return new Acknowledgement(false, "NO_RESULT");
        if (!current.ownerPlayerId().equals(playerId)) return new Acknowledgement(false, "NOT_OWNER");
        if (!current.sent()) return new Acknowledgement(false, "NOT_PUBLISHED");
        if (current.view().revision() != acknowledgement.revision()
                || !current.view().outcome().equals(acknowledgement.outcome())) {
            return new Acknowledgement(false, "STALE_RESULT");
        }

        BattleInstance battle = battles.battle(acknowledgement.battleId()).orElse(null);
        if (battle == null) {
            pending.remove(acknowledgement.battleId());
            return new Acknowledgement(true, "ALREADY_CLOSED");
        }
        if (battle.state() != BattleState.REWARD || battle.outcome() == BattleInstance.Outcome.ONGOING) {
            return new Acknowledgement(false, "WRONG_PHASE");
        }
        if (battle.revision() != acknowledgement.revision()
                || !battle.outcome().name().equals(acknowledgement.outcome())) {
            return new Acknowledgement(false, "STALE_RESULT");
        }

        battle.cleanup();
        battles.cleanup(battle.battleId());
        pending.remove(battle.battleId());
        return new Acknowledgement(true, "CLOSED");
    }

    /** Releases stale presentation records when an exceptional world lifecycle already removed a battle. */
    public void pruneClosedBattles(MinecraftServer server) {
        if (server == null || pending.isEmpty()) return;
        List<UUID> closed = pending.keySet().stream()
                .filter(id -> battles.battle(id).isEmpty())
                .toList();
        for (UUID battleId : closed) {
            Pending current = pending.remove(battleId);
            if (current == null || !current.sent()) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(current.ownerPlayerId());
            if (player != null) {
                PacketDistributor.sendToPlayer(player,
                        BattleResultNetworkPayloads.ResultClosedS2C.of(battleId, true, "BATTLE_CLOSED"));
            }
        }
    }

    private void putAndSend(
            MinecraftServer server,
            UUID ownerPlayerId,
            BattleResultNetworkPayloads.ResultView view
    ) {
        Pending previous = pending.putIfAbsent(view.battleId(), new Pending(ownerPlayerId, view, false));
        if (previous != null) {
            if (!previous.ownerPlayerId().equals(ownerPlayerId) || !previous.view().equals(view)) {
                throw new IllegalStateException("conflicting result presentation for battle " + view.battleId());
            }
        }
        trySend(server, view.battleId());
    }

    private BattleInstance requireTerminalBattle(UUID battleId, BattleInstance.Outcome expected) {
        BattleInstance battle = battles.battle(battleId).orElseThrow(() ->
                new IllegalArgumentException("unknown battle " + battleId));
        if (battle.state() != BattleState.REWARD || battle.outcome() != expected) {
            throw new IllegalStateException("battle terminal state mismatch for " + battleId);
        }
        return battle;
    }

    private Optional<UUID> presentationOwner(MinecraftServer server, UUID battleId) {
        var rewardContext = battles.rewardContext(battleId).orElse(null);
        if (rewardContext != null) return Optional.of(rewardContext.ownerPlayerId());

        Set<UUID> playerIds = new HashSet<>();
        for (EntityParticipantBinding binding : battles.bindings(battleId)) {
            if (server.getPlayerList().getPlayer(binding.entityId()) != null) playerIds.add(binding.entityId());
        }
        return playerIds.size() == 1 ? Optional.of(playerIds.iterator().next()) : Optional.empty();
    }
}
