package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.StatusDefinition;

/** Shared status mutation entry point for deterministic battle state. */
public final class StatusService {
    public static final String GUARD = "turnbound_re:guard";
    public static final String EXPOSED = "turnbound_re:exposed";
    public static final String POISE_GUARD = "turnbound_re:poise_guard";
    public static final String BURN = "turnbound_re:burn";
    public static final String SLOW = "turnbound_re:slow";
    public static final String ATK_UP = "turnbound_re:atk_up";
    public static final String DEF_DOWN = "turnbound_re:def_down";

    private StatusService() {}

    public static void applySingle(StatusRuntime runtime, String statusId) {
        requireRuntime(runtime).apply(statusId, 1, 1);
    }

    /** Applies an explicit stack count using the data definition's maxStacks contract. */
    public static void apply(StatusRuntime runtime, StatusDefinition definition, int stacks) {
        if (definition == null) throw new IllegalArgumentException("definition must not be null");
        requireRuntime(runtime).apply(definition.id(), stacks, definition.maxStacks());
    }

    public static boolean remove(StatusRuntime runtime, String statusId) {
        return requireRuntime(runtime).remove(statusId);
    }

    private static StatusRuntime requireRuntime(StatusRuntime runtime) {
        if (runtime == null) throw new IllegalArgumentException("runtime must not be null");
        return runtime;
    }
}
