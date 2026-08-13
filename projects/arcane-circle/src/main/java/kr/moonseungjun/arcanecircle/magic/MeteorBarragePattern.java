package kr.moonseungjun.arcanecircle.magic;

/** Shared deterministic Meteor Swarm timing/offset grammar for authoritative hits and client VFX. */
public final class MeteorBarragePattern {
    public record Strike(double offsetX, double offsetZ, int impactTick, double scale, double fallHeight) {}

    private static final Strike[] STRIKES = {
            new Strike(-3.2, -1.1, 18, .72, 29), new Strike(4.5, 2.7, 21, .78, 32),
            new Strike(-7.4, 5.4, 24, .68, 28), new Strike(1.9, -6.6, 27, .86, 35),
            new Strike(8.6, -3.0, 30, .74, 31), new Strike(-10.2, -4.9, 33, .92, 38),
            new Strike(6.7, 8.7, 36, .82, 35), new Strike(-2.7, 10.5, 39, .70, 30),
            new Strike(11.1, 4.1, 42, 1.02, 41), new Strike(-8.7, .9, 45, .76, 33),
            new Strike(3.3, 7.2, 48, .88, 36), new Strike(.1, -2.3, 51, 1.12, 43),
            new Strike(-5.6, -9.2, 55, .84, 34), new Strike(9.5, -8.1, 59, .94, 39),
            new Strike(-11.4, 8.2, 63, .80, 33), new Strike(5.2, -.8, 68, 1.18, 44)
    };

    private MeteorBarragePattern() {}
    public static int count() { return STRIKES.length; }
    public static Strike strike(int index) { return STRIKES[Math.max(0, Math.min(STRIKES.length - 1, index))]; }
    public static int firstImpactTick() { return STRIKES[0].impactTick(); }
    public static int lastImpactTick() { return STRIKES[STRIKES.length - 1].impactTick(); }
    public static int durationTicks() { return lastImpactTick() + 12; }
}
