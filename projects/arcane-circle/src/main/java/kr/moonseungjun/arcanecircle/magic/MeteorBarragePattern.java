package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

/** Shared seeded Meteor Swarm timing/offset grammar for authoritative hits and client VFX. */
public final class MeteorBarragePattern {
    public record Strike(double offsetX, double offsetZ, int impactTick, double scale, double fallHeight) {}

    private static final Strike[] BASE_STRIKES = {
            new Strike(-3.2, -1.1, 18, .72, 29), new Strike(4.5, 2.7, 21, .78, 32),
            new Strike(-7.4, 5.4, 24, .68, 28), new Strike(1.9, -6.6, 27, .86, 35),
            new Strike(8.6, -3.0, 30, .74, 31), new Strike(-10.2, -4.9, 33, .92, 38),
            new Strike(6.7, 8.7, 36, .82, 35), new Strike(-2.7, 10.5, 39, .70, 30),
            new Strike(11.1, 4.1, 42, 1.02, 41), new Strike(-8.7, .9, 45, .76, 33),
            new Strike(3.3, 7.2, 48, .88, 36), new Strike(.1, -2.3, 51, 1.12, 43),
            new Strike(-5.6, -9.2, 55, .84, 34), new Strike(9.5, -8.1, 59, .94, 39),
            new Strike(-11.4, 8.2, 63, .88, 35),
            // The final crown meteor is a separate ninth-circle event, not merely strike sixteen.
            new Strike(0.0, 0.0, 74, 2.04, 58)
    };
    private static final ThreadLocal<Long> ACTIVE_SEED = new ThreadLocal<>();
    private static final double MIN_SEPARATION = 2.15;

    private MeteorBarragePattern() {}

    public static int count() { return BASE_STRIKES.length; }
    public static int crownIndex() { return BASE_STRIKES.length - 1; }
    public static boolean isCrownStrike(int index) { return index == crownIndex(); }

    public static List<Strike> strikes() {
        Long seed = ACTIVE_SEED.get();
        return strikes(seed == null ? 0L : seed);
    }

    public static List<Strike> strikes(long seed) {
        if (seed == 0L) return List.of(BASE_STRIKES);
        Random random = new Random(seed ^ 0x5F3759DF4A7C15L);
        double rotation = random.nextDouble() * Math.PI * 2.0;
        double cos = Math.cos(rotation);
        double sin = Math.sin(rotation);
        List<Strike> result = new ArrayList<>(BASE_STRIKES.length);
        int previousTick = 0;
        for (int index = 0; index < BASE_STRIKES.length; index++) {
            Strike base = BASE_STRIKES[index];
            boolean crown = isCrownStrike(index);
            double rotatedX = base.offsetX() * cos - base.offsetZ() * sin;
            double rotatedZ = base.offsetX() * sin + base.offsetZ() * cos;
            double jitter = crown ? .36 : 1.10;
            double x = rotatedX + (random.nextDouble() - .5) * jitter;
            double z = rotatedZ + (random.nextDouble() - .5) * jitter;
            if (!crown && tooClose(result, x, z)) {
                x = rotatedX;
                z = rotatedZ;
            }
            double scale;
            double fallHeight;
            int tick;
            if (crown) {
                scale = clamp(base.scale() * (.97 + random.nextDouble() * .06), 1.94, 2.16);
                fallHeight = clamp(base.fallHeight() + (random.nextDouble() - .5) * 5.0, 54.0, 64.0);
                tick = Math.max(previousTick + 8, base.impactTick() + random.nextInt(3) - 1);
            } else {
                double rhythm = index >= 12 ? 1.08 + (index - 12) * .03 : 1.0;
                scale = clamp(base.scale() * (.93 + random.nextDouble() * .14) * rhythm, .62, 1.46);
                fallHeight = clamp(base.fallHeight() + (random.nextDouble() - .5) * 7.0, 27.0, 48.0);
                tick = base.impactTick() + random.nextInt(3) - 1;
                tick = Math.max(index == 0 ? 16 : previousTick + 2, tick);
            }
            previousTick = tick;
            result.add(new Strike(x, z, tick, scale, fallHeight));
        }
        return List.copyOf(result);
    }

    public static Strike strike(int index) {
        List<Strike> strikes = strikes();
        return strikes.get(Math.max(0, Math.min(strikes.size() - 1, index)));
    }

    public static Strike strike(long seed, int index) {
        List<Strike> strikes = strikes(seed);
        return strikes.get(Math.max(0, Math.min(strikes.size() - 1, index)));
    }

    public static int firstImpactTick() { return strikes().getFirst().impactTick(); }
    public static int firstImpactTick(long seed) { return strikes(seed).getFirst().impactTick(); }
    public static int lastImpactTick() { return strikes().getLast().impactTick(); }
    public static int lastImpactTick(long seed) { return strikes(seed).getLast().impactTick(); }
    public static int durationTicks() { return lastImpactTick() + 16; }
    public static int durationTicks(long seed) { return lastImpactTick(seed) + 16; }

    public static Vec3 position(Vec3 center, Strike strike) {
        return center.add(strike.offsetX(), 0.0, strike.offsetZ());
    }

    public static <T> T withSeed(long seed, Supplier<T> action) {
        Long previous = ACTIVE_SEED.get();
        ACTIVE_SEED.set(seed);
        try {
            return action.get();
        } finally {
            if (previous == null) ACTIVE_SEED.remove();
            else ACTIVE_SEED.set(previous);
        }
    }

    public static long castSeed(UUID caster, long gameTime) {
        long value = caster.getMostSignificantBits()
                ^ Long.rotateLeft(caster.getLeastSignificantBits(), 19)
                ^ gameTime * 0x9E3779B97F4A7C15L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return value == 0L ? 0x6A09E667F3BCC909L : value;
    }

    private static boolean tooClose(List<Strike> existing, double x, double z) {
        double minimumSq = MIN_SEPARATION * MIN_SEPARATION;
        for (Strike strike : existing) {
            double dx = strike.offsetX() - x;
            double dz = strike.offsetZ() - z;
            if (dx * dx + dz * dz < minimumSq) return true;
        }
        return false;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
