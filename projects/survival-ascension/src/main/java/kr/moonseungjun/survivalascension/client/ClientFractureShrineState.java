package kr.moonseungjun.survivalascension.client;

import kr.moonseungjun.survivalascension.network.FractureShrineTargetPayload;

public final class ClientFractureShrineState {
    private static final long STALE_MILLIS = 5_000L;
    private static volatile Target target;
    private ClientFractureShrineState() {}
    public static void onTarget(FractureShrineTargetPayload p) { target = p.active() ? new Target(p.exact(), p.x(), p.z(), System.currentTimeMillis()) : null; }
    public static void clear() { target = null; }
    public static Target current() {
        Target value = target;
        if (value != null && System.currentTimeMillis() - value.updatedAtMillis() > STALE_MILLIS) { target = null; return null; }
        return value;
    }
    public record Target(boolean exact, int x, int z, long updatedAtMillis) {}
}
