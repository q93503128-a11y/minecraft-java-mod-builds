package dev.moonseungjun.augmentationsurvival.augment.resource;

/** Client prediction mirror for only the consumption values that affect local input/use timing. */
public final class ClientConsumptionState {
    private static volatile int fastEatingLevel;
    private static volatile int eatWhileWalkingLevel;
    private static volatile int reserveMealLevel;
    private static volatile int reservePoints;

    private ClientConsumptionState() {
    }

    public static void apply(int fastEating, int eatWhileWalking, int reserveMeal, int reserve) {
        fastEatingLevel = Math.max(0, fastEating);
        eatWhileWalkingLevel = Math.max(0, eatWhileWalking);
        reserveMealLevel = Math.max(0, reserveMeal);
        reservePoints = Math.max(0, reserve);
    }

    public static void clear() {
        apply(0, 0, 0, 0);
    }

    public static int fastEatingLevel() {
        return fastEatingLevel;
    }

    public static int eatWhileWalkingLevel() {
        return eatWhileWalkingLevel;
    }

    public static int reserveMealLevel() {
        return reserveMealLevel;
    }

    public static int reservePoints() {
        return reservePoints;
    }
}
