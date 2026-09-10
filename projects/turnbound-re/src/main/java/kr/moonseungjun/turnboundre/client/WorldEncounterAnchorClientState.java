package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.network.WorldEncounterAnchorPayloads;

import java.util.Optional;

/** Client cache for the latest server-authored in-world encounter anchor preview. */
public final class WorldEncounterAnchorClientState {
    private static WorldEncounterAnchorPayloads.PreviewView latest;
    private static long generation;

    private WorldEncounterAnchorClientState() {}

    public static synchronized void accept(WorldEncounterAnchorPayloads.AnchorPreviewS2C payload) {
        if (payload == null) return;
        latest = payload.decode();
        generation++;
    }

    public static synchronized void reject(WorldEncounterAnchorPayloads.AnchorRejectedS2C payload) {
        if (payload == null || latest == null) return;
        latest = new WorldEncounterAnchorPayloads.PreviewView(
                latest.anchorEntityId(), latest.locator(), latest.encounterId(), latest.difficulty(),
                latest.enemySources(), latest.rewardKinds(), latest.repeatable(), latest.partySize(),
                payload.code(), payload.detail());
        generation++;
    }

    public static synchronized Optional<WorldEncounterAnchorPayloads.PreviewView> view() {
        return Optional.ofNullable(latest);
    }

    public static synchronized long generation() { return generation; }

    public static synchronized void clear() {
        latest = null;
        generation++;
    }
}
