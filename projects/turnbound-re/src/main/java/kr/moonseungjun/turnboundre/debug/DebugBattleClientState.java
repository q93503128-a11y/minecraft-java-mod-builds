package kr.moonseungjun.turnboundre.debug;

import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;

import java.util.Optional;

/**
 * DEBUG_ONLY client-side inspection cache for authoritative S2C battle data.
 * This is deliberately not production presentation.
 */
public final class DebugBattleClientState {
    public static final String CLEAR_EVENT_TYPE = "DEBUG_CLIENT_CLEAR";

    private static final Object LOCK = new Object();
    private static BattleNetworkPayloads.DecodedSnapshot latestSnapshot;
    private static BattleNetworkPayloads.DecodedEvents latestEvents;

    private DebugBattleClientState() {}

    public static void accept(BattleNetworkPayloads.BattleSnapshotS2C payload) {
        if (payload == null) return;
        BattleNetworkPayloads.DecodedSnapshot decoded = payload.decode();
        synchronized (LOCK) {
            if (latestSnapshot == null
                    || !latestSnapshot.battleId().equals(decoded.battleId())
                    || decoded.revision() >= latestSnapshot.revision()) {
                latestSnapshot = decoded;
            }
        }
    }

    public static void accept(BattleNetworkPayloads.BattleEventsS2C payload) {
        if (payload == null) return;
        BattleNetworkPayloads.DecodedEvents decoded = payload.decode();
        synchronized (LOCK) {
            if (decoded.events().stream().anyMatch(event -> CLEAR_EVENT_TYPE.equals(event.type()))) {
                latestSnapshot = null;
                latestEvents = null;
                return;
            }
            if (latestEvents == null
                    || !latestEvents.battleId().equals(decoded.battleId())
                    || decoded.resultingRevision() >= latestEvents.resultingRevision()) {
                latestEvents = decoded;
            }
        }
    }

    public static Optional<BattleNetworkPayloads.DecodedSnapshot> latestSnapshot() {
        synchronized (LOCK) {
            return Optional.ofNullable(latestSnapshot);
        }
    }

    public static Optional<BattleNetworkPayloads.DecodedEvents> latestEvents() {
        synchronized (LOCK) {
            return Optional.ofNullable(latestEvents);
        }
    }

    public static String summary() {
        synchronized (LOCK) {
            if (latestSnapshot == null) return "DEBUG_ONLY no battle snapshot received";
            int eventCount = latestEvents != null && latestEvents.battleId().equals(latestSnapshot.battleId())
                    ? latestEvents.events().size() : 0;
            return "DEBUG_ONLY battle=" + latestSnapshot.battleId()
                    + " rev=" + latestSnapshot.revision()
                    + " state=" + latestSnapshot.state()
                    + " cycle=" + latestSnapshot.cycle()
                    + " actor=" + latestSnapshot.currentActorId()
                    + " participants=" + latestSnapshot.participants().size()
                    + " latestEvents=" + eventCount;
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            latestSnapshot = null;
            latestEvents = null;
        }
    }
}
