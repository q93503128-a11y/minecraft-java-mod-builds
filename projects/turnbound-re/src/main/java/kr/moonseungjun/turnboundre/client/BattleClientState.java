package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;

import java.util.Optional;

/**
 * Sole client-side cache of authoritative battle presentation data.
 * It stores server snapshots/events only and never derives combat results locally.
 */
public final class BattleClientState {
    public static final String CLEAR_EVENT_TYPE = "BATTLE_CLIENT_CLEAR";

    private static final Object LOCK = new Object();
    private static BattleNetworkPayloads.DecodedSnapshot latestSnapshot;
    private static BattleNetworkPayloads.DecodedEvents latestEvents;

    private BattleClientState() {}

    public static void accept(BattleNetworkPayloads.BattleSnapshotS2C payload) {
        if (payload == null) return;
        BattleNetworkPayloads.DecodedSnapshot decoded = payload.decode();
        synchronized (LOCK) {
            if (latestSnapshot == null
                    || !latestSnapshot.battleId().equals(decoded.battleId())
                    || decoded.revision() >= latestSnapshot.revision()) {
                latestSnapshot = decoded;
                if (latestEvents != null && !latestEvents.battleId().equals(decoded.battleId())) {
                    latestEvents = null;
                }
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

    public static Optional<BattlePresentationModel> presentation() {
        synchronized (LOCK) {
            return latestSnapshot == null ? Optional.empty() : Optional.of(BattlePresentationModel.from(latestSnapshot));
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            latestSnapshot = null;
            latestEvents = null;
        }
    }
}
