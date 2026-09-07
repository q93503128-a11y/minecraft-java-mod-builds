package kr.moonseungjun.turnboundre.progression;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.battle.BattleManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Common terminal lifecycle hook for reward-bearing battles.
 * It is intentionally independent of the command/AI/status path that caused VICTORY.
 */
public final class BattleRewardLifecycleHooks {
    private final BattleManager battles;
    private final BattleRewardSettlementService settlements;
    private final Set<UUID> loggedPersistenceFailures = new HashSet<>();

    public BattleRewardLifecycleHooks(BattleManager battles, BattleRewardSettlementService settlements) {
        if (battles == null || settlements == null) throw new IllegalArgumentException("battles/settlements required");
        this.battles = battles;
        this.settlements = settlements;
    }

    public void register(IEventBus bus) {
        if (bus == null) throw new IllegalArgumentException("bus must not be null");
        bus.addListener(this::onServerTickPost);
    }

    private void onServerTickPost(ServerTickEvent.Post event) {
        Set<UUID> stillPending = new HashSet<>();
        for (UUID battleId : battles.rewardReadyBattleIds()) {
            stillPending.add(battleId);
            try {
                if (settlements.settleIfReady(event.getServer(), battleId).isPresent()) {
                    loggedPersistenceFailures.remove(battleId);
                }
            } catch (RuntimeException failure) {
                if (loggedPersistenceFailures.add(battleId)) {
                    TurnboundRe.LOGGER.error(
                            "TURNBOUND reward persistence failed for battle {}; claim remains pending and will retry",
                            battleId,
                            failure);
                }
            }
        }
        loggedPersistenceFailures.retainAll(stillPending);
    }
}
