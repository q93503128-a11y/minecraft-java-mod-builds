package kr.moonseungjun.anomalousclasses.state;

/** Candidate flight capacity and native flight speed, independent of Minecraft rendering. */
public final class SwarmFlightRules {
    private SwarmFlightRules() {}

    public static int requiredLift(int level) {
        if (level < 25) return 0;
        return 24 + (int) Math.min(6L, Math.max(0L, ((long) level - 25L) * 6L / 75L));
    }

    public static float flyingSpeed(int level) {
        return .024F + Math.min(.020F, Math.max(0, level - 25) * .00009F);
    }

    public static boolean qualified(int level, int alive) {
        int required = requiredLift(level);
        return required > 0 && alive >= required;
    }
}
