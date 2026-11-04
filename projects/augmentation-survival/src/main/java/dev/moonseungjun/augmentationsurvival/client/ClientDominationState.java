package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.network.DominationRosterPayload;

/** Display-only snapshot. All mutations must be validated on the server. */
public final class ClientDominationState {
    private static DominationRosterPayload roster = new DominationRosterPayload(0, 0, java.util.List.of());
    private ClientDominationState() { }
    public static DominationRosterPayload roster() { return roster; }
    public static void apply(DominationRosterPayload value) { roster = value; }
    public static void clear() {
        roster = new DominationRosterPayload(0, 0, java.util.List.of());
    }
}
