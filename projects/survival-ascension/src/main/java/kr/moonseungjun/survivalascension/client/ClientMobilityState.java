package kr.moonseungjun.survivalascension.client;

import kr.moonseungjun.survivalascension.network.MobilityCooldownPayload;

public final class ClientMobilityState {
    private static volatile int totalCooldownTicks;
    private static volatile int remainingCooldownTicks;

    private ClientMobilityState() {}

    public static void onCooldown(MobilityCooldownPayload payload) {
        int incoming = Math.max(0, payload.cooldownTicks());
        if (incoming <= 0) {
            totalCooldownTicks = 0;
            remainingCooldownTicks = 0;
            return;
        }
        // A successful dash arrives as a fresh full-duration value. Follow-up packets are the
        // authoritative remaining server ticks; never run a separate real-time clock on the client.
        if (remainingCooldownTicks <= 0 || incoming > remainingCooldownTicks + 1) {
            totalCooldownTicks = incoming;
        } else {
            totalCooldownTicks = Math.max(totalCooldownTicks, incoming);
        }
        remainingCooldownTicks = incoming;
    }

    public static int remainingTicks() { return Math.max(0, remainingCooldownTicks); }

    public static float readyProgress() {
        int total = totalCooldownTicks;
        if (total <= 0) return 1.0F;
        return Math.max(0.0F, Math.min(1.0F, 1.0F - remainingTicks() / (float)total));
    }
}
