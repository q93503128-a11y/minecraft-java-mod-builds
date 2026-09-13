package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.progression.PlayerProgress;

/** Pure access policy shared by preview and authoritative launch validation for authored world anchors. */
public final class WorldEncounterAnchorAccessPolicy {
    private WorldEncounterAnchorAccessPolicy() {}

    public static boolean repeatable(WorldEncounterAnchorResolver.Resolved resolved) {
        if (resolved == null) throw new IllegalArgumentException("resolved anchor required");
        return resolved.anchor().repeatable() && resolved.encounter().repeatable();
    }

    public static boolean cleared(PlayerProgress progress, WorldEncounterAnchorResolver.Resolved resolved) {
        if (progress == null) throw new IllegalArgumentException("progress required");
        return !repeatable(resolved) && progress.hasCompletedEncounterLocator(resolved.anchor().locator());
    }
}
