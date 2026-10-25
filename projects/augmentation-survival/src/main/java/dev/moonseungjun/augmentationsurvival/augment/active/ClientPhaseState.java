package dev.moonseungjun.augmentationsurvival.augment.active;

/** Client-side prediction bit for Phase Shift collision rules. */
public final class ClientPhaseState {
    private static volatile boolean phaseShiftActive;

    private ClientPhaseState() {
    }

    public static boolean phaseShiftActive() {
        return phaseShiftActive;
    }

    public static void apply(boolean active) {
        phaseShiftActive = active;
    }

    public static void clear() {
        phaseShiftActive = false;
    }
}
