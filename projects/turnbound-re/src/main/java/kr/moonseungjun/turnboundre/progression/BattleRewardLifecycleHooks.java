package kr.moonseungjun.turnboundre.progression;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.battle.BattleManager;
import kr.moonseungjun.turnboundre.network.BattleResultPresentationService;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Common terminal lifecycle hook for reward-bearing and rewardless battles.
 * It is intentionally independent of the command/AI/status path that caused the terminal outcome.
 */
public final class BattleRewardLifecycleHooks {
    private final BattleManager battles;
    private final BattleRewardSettlementService settlements;
    private final BattleResultPresentationService results;
    private final Set<UUID> loggedPersistenceFailures = new HashSet<>();

    public BattleRewardLifecycleHooks(
            BattleManager battles,
            BattleRewardSettlementService settlements,
            BattleResultPresentationService results
    ) {
        if (battles == null || settlements == null || results == null) {
            throw new IllegalArgumentException("battles/settlements/results required");
        }
        this.battles = battles;
        this.settlements = settlements;
        this.results = results;
    }

    public void register(IEventBus bus) {
        if (bus == null) throw new IllegalArgumentException("bus must not be null");
        bus.addListener(this::onServerTickPost);
    }

    private void onServerTickPost(ServerTickEvent.Post event) {
        Set<UUID> stillPersistencePending = new HashSet<>();
        for (UUID battleId : battles.terminalRewardStateBattleIds()) {
            if (results.hasPending(battleId)) {
                results.trySend(event.getServer(), battleId);
                continue;
            }

            BattleInstance battle = battles.battle(battleId).orElse(null);
            if (battle == null) continue;
            if (battle.outcome() == BattleInstance.Outcome.DEFEAT || battles.rewardContext(battleId).isEmpty()) {
                results.presentRewardlessTerminal(event.getServer(), battleId);
                continue;
            }

            stillPersistencePending.add(battleId);
            try {
                var settlement = settlements.settleIfReady(event.getServer(), battleId);
                if (settlement.isPresent()) {
                    loggedPersistenceFailures.remove(battleId);
                    stillPersistencePending.remove(battleId);
                    results.presentSettlement(event.getServer(), battleId, settlement.get());
                }
            } catch (RuntimeException failure) {
                if (loggedPersistenceFailures.add(battleId)) {
                    TurnboundRe.LOGGER.error(
                            "TURNBOUND reward persistence failed for battle {}; claim remains pending and result presentation waits",
                            battleId,
                            failure);
                }
            }
        }
        loggedPersistenceFailures.retainAll(stillPersistencePending);
        results.pruneClosedBattles(event.getServer());
    }
}
