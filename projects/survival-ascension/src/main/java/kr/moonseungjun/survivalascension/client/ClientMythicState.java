package kr.moonseungjun.survivalascension.client;

import kr.moonseungjun.survivalascension.network.MythicTargetPayload;

public final class ClientMythicState {
    private static final long STALE_MILLIS = 1_600L;
    private static volatile Target target;

    private ClientMythicState() {}

    public static void onTarget(MythicTargetPayload payload) {
        target = new Target(payload.x(), payload.z(), System.currentTimeMillis());
    }

    public static Target current() {
        Target value = target;
        if (value == null) return null;
        if (System.currentTimeMillis() - value.updatedAtMillis() > STALE_MILLIS) {
            target = null;
            return null;
        }
        return value;
    }

    public record Target(double x, double z, long updatedAtMillis) {}
}
