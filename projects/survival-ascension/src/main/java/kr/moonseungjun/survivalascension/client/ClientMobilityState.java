package kr.moonseungjun.survivalascension.client;

import kr.moonseungjun.survivalascension.network.MobilityCooldownPayload;

public final class ClientMobilityState {
    private static volatile int cooldownTicks;
    private static volatile long startedAtMillis;

    private ClientMobilityState() {}

    public static void onCooldown(MobilityCooldownPayload payload) {
        cooldownTicks = Math.max(0, payload.cooldownTicks());
        startedAtMillis = System.currentTimeMillis();
    }

    public static int remainingTicks() {
        int total = cooldownTicks;
        if (total <= 0) return 0;
        long remainingMillis = total * 50L - Math.max(0L, System.currentTimeMillis() - startedAtMillis);
        if (remainingMillis <= 0L) return 0;
        return (int)Math.min(total, (remainingMillis + 49L) / 50L);
    }

    public static float readyProgress() {
        int total = cooldownTicks;
        if (total <= 0) return 1.0F;
        return Math.max(0.0F, Math.min(1.0F, 1.0F - remainingTicks() / (float)total));
    }
}
