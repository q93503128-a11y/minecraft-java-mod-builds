package kr.moonseungjun.turnboundre.client;

/**
 * Presentation-only lifecycle flag for the non-pausing battle command screen.
 * It exists only to prevent the passive HUD command strip from rendering underneath
 * the interactive command/target picker. No battle authority or selection data lives here.
 */
public final class BattleCommandOverlayState {
    private static boolean open;

    private BattleCommandOverlayState() {}

    public static synchronized void open() {
        open = true;
    }

    public static synchronized void close() {
        open = false;
    }

    public static synchronized boolean isOpen() {
        return open;
    }
}
