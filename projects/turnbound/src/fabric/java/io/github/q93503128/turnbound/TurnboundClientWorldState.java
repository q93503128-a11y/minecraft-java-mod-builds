package io.github.q93503128.turnbound;

/** Client-only server-synchronized flag, cleared on disconnect. */
public final class TurnboundClientWorldState {
    private static boolean enabled;
    private TurnboundClientWorldState() {}
    public static void setEnabled(boolean active) { enabled = active; }
    public static boolean enabled() { return enabled; }
}
