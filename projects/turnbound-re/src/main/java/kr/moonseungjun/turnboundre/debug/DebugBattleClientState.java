package kr.moonseungjun.turnboundre.debug;

import kr.moonseungjun.turnboundre.client.BattleClientState;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;

import java.util.Optional;

/**
 * DEBUG_ONLY inspection facade over the production BattleClientState.
 * It intentionally owns no duplicate snapshot/event cache.
 */
public final class DebugBattleClientState {
    public static final String CLEAR_EVENT_TYPE = BattleClientState.CLEAR_EVENT_TYPE;

    private DebugBattleClientState() {}

    public static void accept(BattleNetworkPayloads.BattleSnapshotS2C payload) {
        BattleClientState.accept(payload);
    }

    public static void accept(BattleNetworkPayloads.BattleEventsS2C payload) {
        BattleClientState.accept(payload);
    }

    public static Optional<BattleNetworkPayloads.DecodedSnapshot> latestSnapshot() {
        return BattleClientState.latestSnapshot();
    }

    public static Optional<BattleNetworkPayloads.DecodedEvents> latestEvents() {
        return BattleClientState.latestEvents();
    }

    public static String summary() {
        BattleNetworkPayloads.DecodedSnapshot snapshot = BattleClientState.latestSnapshot().orElse(null);
        if (snapshot == null) return "DEBUG_ONLY no battle snapshot received";
        int eventCount = BattleClientState.latestEvents()
                .filter(events -> events.battleId().equals(snapshot.battleId()))
                .map(events -> events.events().size())
                .orElse(0);
        return "DEBUG_ONLY battle=" + snapshot.battleId()
                + " rev=" + snapshot.revision()
                + " state=" + snapshot.state()
                + " cycle=" + snapshot.cycle()
                + " actor=" + snapshot.currentActorId()
                + " participants=" + snapshot.participants().size()
                + " latestEvents=" + eventCount;
    }

    public static void clear() {
        BattleClientState.clear();
    }
}
