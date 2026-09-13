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
    private static long eventEpoch;

    private BattleClientState() {}

    public static void accept(BattleNetworkPayloads.BattleSnapshotS2C payload) {
        if (payload == null) return;
        BattleNetworkPayloads.DecodedSnapshot decoded = payload.decode();
        synchronized (LOCK) {
            if (latestSnapshot == null
                    || !latestSnapshot.battleId().equals(decoded.battleId())
                    || decoded.revision() >= latestSnapshot.revision()) {
                if (latestSnapshot == null || !latestSnapshot.battleId().equals(decoded.battleId())) {
                    BattleActionTimelineState.beginBattle(decoded.battleId());
                }
                BattleStageFeedbackState.acceptSnapshot(latestSnapshot, decoded);
                BattleImpactPresentationState.acceptSnapshot(latestSnapshot, decoded, eventEpoch);
                latestSnapshot = decoded;
                if (latestEvents != null && !latestEvents.battleId().equals(decoded.battleId())) {
                    latestEvents = null;
                    eventEpoch++;
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
                eventEpoch++;
                BattleStageFeedbackState.clear();
                BattleImpactPresentationState.clear();
                BattleActionTimelineState.clear();
                return;
            }
            if (latestEvents == null
                    || !latestEvents.battleId().equals(decoded.battleId())
                    || decoded.resultingRevision() >= latestEvents.resultingRevision()) {
                if (!decoded.equals(latestEvents)) eventEpoch++;
                latestEvents = decoded;
                BattleActionTimelineState.acceptEvents(decoded);
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
            if (latestSnapshot == null) return Optional.empty();
            BattleNetworkPayloads.DecodedSnapshot projected = BattleImpactPresentationState.project(latestSnapshot, eventEpoch);
            return Optional.of(BattlePresentationModel.from(projected));
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            latestSnapshot = null;
            latestEvents = null;
            eventEpoch++;
            BattleStageFeedbackState.clear();
            BattleImpactPresentationState.clear();
            BattleActionTimelineState.clear();
        }
    }
}
