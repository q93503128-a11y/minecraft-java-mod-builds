package kr.moonseungjun.survivalascension.client;

import kr.moonseungjun.survivalascension.network.MythicTargetPayload;

import java.util.UUID;

public final class ClientMythicState {
    // Explicit server clear packets are authoritative. This is only a failsafe for a severed connection.
    private static final long FAILSAFE_STALE_MILLIS = 10_000L;
    private static volatile Target target;

    private ClientMythicState() {}

    public static void onTarget(MythicTargetPayload payload) {
        if (!payload.active()) {
            target = null;
            return;
        }
        target = new Target(payload.targetId(), payload.x(), payload.z(), System.currentTimeMillis());
    }

    public static void clear() { target = null; }

    public static Target current() {
        Target value = target;
        if (value == null) return null;
        if (System.currentTimeMillis() - value.updatedAtMillis() > FAILSAFE_STALE_MILLIS) {
            target = null;
            return null;
        }
        return value;
    }

    public record Target(UUID targetId, double x, double z, long updatedAtMillis) {}
}
