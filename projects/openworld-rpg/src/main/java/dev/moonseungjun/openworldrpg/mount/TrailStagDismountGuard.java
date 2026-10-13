package dev.moonseungjun.openworldrpg.mount;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Prevents recursive interception when the server executes an already-authorized Stag dismount. */
public final class TrailStagDismountGuard {
    private static final ThreadLocal<Integer> AUTHORIZED_DEPTH = ThreadLocal.withInitial(() -> 0);

    private TrailStagDismountGuard() {
    }

    public static boolean authorized() {
        return AUTHORIZED_DEPTH.get() > 0;
    }

    public static boolean runAuthorized(BooleanSupplier action) {
        Objects.requireNonNull(action, "action");
        AUTHORIZED_DEPTH.set(AUTHORIZED_DEPTH.get() + 1);
        try {
            return action.getAsBoolean();
        } finally {
            int depth = AUTHORIZED_DEPTH.get() - 1;
            if (depth <= 0) {
                AUTHORIZED_DEPTH.remove();
            } else {
                AUTHORIZED_DEPTH.set(depth);
            }
        }
    }
}
