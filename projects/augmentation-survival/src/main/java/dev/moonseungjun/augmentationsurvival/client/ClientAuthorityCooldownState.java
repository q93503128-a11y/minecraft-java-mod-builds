package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.network.AuthorityCollectionPayload;

/** Client-only countdown extrapolated from the latest server-owned collection snapshot. */
public final class ClientAuthorityCooldownState {
    private static long readyMillis;
    private static int ownedCount;
    private static int enabledCount;
    private ClientAuthorityCooldownState() { }

    public static void apply(AuthorityCollectionPayload payload) {
        readyMillis = System.currentTimeMillis() + payload.remainingCooldownTicks() * 50L;
        ownedCount = payload.ownedIds().size();
        enabledCount = Math.max(0, ownedCount - payload.disabledIds().size());
    }

    public static int remainingSeconds() {
        return (int) Math.min(Integer.MAX_VALUE,
                (Math.max(0L, readyMillis - System.currentTimeMillis()) + 999L) / 1000L);
    }

    public static int ownedCount() { return ownedCount; }
    public static int enabledCount() { return enabledCount; }
    public static void clear() { readyMillis = 0L; ownedCount = 0; enabledCount = 0; }
}
