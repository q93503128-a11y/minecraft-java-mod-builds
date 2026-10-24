package dev.moonseungjun.augmentationsurvival.augment.active;

/** Client-side prediction bit for time-acceleration use-duration rules. */
public final class ClientTemporalState {
    private static volatile boolean timeAccelerationActive;

    private ClientTemporalState() {
    }

    public static boolean timeAccelerationActive() {
        return timeAccelerationActive;
    }

    public static void apply(boolean active) {
        timeAccelerationActive = active;
    }

    public static void clear() {
        timeAccelerationActive = false;
    }
}
