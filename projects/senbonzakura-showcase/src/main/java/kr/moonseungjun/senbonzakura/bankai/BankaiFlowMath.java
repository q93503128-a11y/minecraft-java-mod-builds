package kr.moonseungjun.senbonzakura.bankai;

import net.minecraft.world.phys.Vec3;

/**
 * Shared deterministic motion for the seven Senbonzakura blade currents.
 *
 * This class deliberately lives outside the client package so the visual renderer and the
 * authoritative server hit logic sample the same macro-current positions. The currents are
 * asymmetric rivers that cross the battlefield; they do not orbit the caster or park on a fixed
 * target coordinate.
 */
public final class BankaiFlowMath {
    public static final int CLUSTER_COUNT = 7;
    public static final double VISUAL_SECONDS = 13.0;

    private BankaiFlowMath() {}

    public static Vec3 currentCenter(
            Vec3 origin,
            Vec3 forwardInput,
            int cluster,
            double seconds,
            double progress,
            double speedScale) {
        Vec3 forward = horizontal(forwardInput);
        Vec3 right = right(forward);
        int c = Math.floorMod(cluster, CLUSTER_COUNT);
        double lane = c - 3.0;
        double time = seconds * Math.max(0.15, speedScale);
        double p = clamp(progress, 0.0, 1.0);

        // Ambient state: seven offset rivers breathe and drift through the arena. There is no
        // polar/orbit coordinate here, so the silhouette never collapses back into a ring.
        double along = 1.6
                + Math.sin(time * (0.46 + c * 0.021) + c * 0.91) * 5.2
                + Math.sin(time * 0.19 + c * 1.37) * 1.75;
        double side = lane * 2.55
                + Math.sin(time * (0.30 + c * 0.013) + c * 0.73) * 0.92;
        double height = 4.15
                + Math.sin(time * (0.55 + c * 0.017) + c * 0.67) * 1.32
                + Math.abs(lane) * 0.14;

        Vec3 base = origin
                .add(forward.scale(along))
                .add(right.scale(side))
                .add(0.0, height, 0.0);

        // First bank: the three left currents tear diagonally across the caster's front.
        if (c <= 2) {
            double influence = smooth(0.510, 0.552, p)
                    * (1.0 - smooth(0.718, 0.772, p));
            if (influence > 0.001) {
                double t = smooth(0.532, 0.710, p);
                double bankLane = c - 1.0;
                Vec3 path = origin
                        .add(forward.scale(
                                mix(-5.8, 9.4, t)
                                        + Math.sin(time * 0.71 + c * 0.8) * 1.25))
                        .add(right.scale(
                                mix(-11.4, 4.8, t)
                                        + bankLane * (1.45 - 0.35 * t)))
                        .add(0.0,
                                4.0
                                        + bankLane * 0.28
                                        + Math.sin(time * 0.84 + c) * 1.15,
                                0.0);
                base = base.lerp(path, influence * 0.94);
            }
        }

        // Second bank: the three right currents answer from the opposite direction. Its timing
        // overlaps the tail of the first sweep so the mass reads as interlocking rivers.
        if (c >= 4) {
            double influence = smooth(0.630, 0.675, p)
                    * (1.0 - smooth(0.848, 0.892, p));
            if (influence > 0.001) {
                double t = smooth(0.652, 0.842, p);
                double bankLane = c - 5.0;
                Vec3 path = origin
                        .add(forward.scale(
                                mix(-4.2, 11.0, t)
                                        + Math.sin(time * 0.68 + c * 0.74) * 1.35))
                        .add(right.scale(
                                mix(11.6, -4.5, t)
                                        + bankLane * (1.42 - 0.32 * t)))
                        .add(0.0,
                                4.25
                                        - bankLane * 0.25
                                        + Math.sin(time * 0.79 + c * 0.9) * 1.22,
                                0.0);
                base = base.lerp(path, influence * 0.94);
            }
        }

        // Finale: all seven currents narrow into a broad forward river, surge past the visual
        // origin, then peel back into separate lanes before the effect fades.
        double finalInfluence = smooth(0.770, 0.812, p)
                * (1.0 - smooth(0.962, 1.0, p));
        if (finalInfluence > 0.001) {
            double t = smooth(0.792, 0.952, p);
            double narrowedLane = lane * mix(1.18, 0.72, t);
            Vec3 river = origin
                    .add(forward.scale(
                            mix(-2.5, 21.5, t)
                                    + Math.sin(time * 0.91 + c * 0.51) * 1.25))
                    .add(right.scale(
                            narrowedLane
                                    + Math.sin(time * 0.53 + c * 1.11) * 0.70))
                    .add(0.0,
                            3.7
                                    + Math.sin(time * 0.96 + c * 0.63) * 1.35
                                    + Math.abs(lane) * 0.10,
                            0.0);
            base = base.lerp(river, finalInfluence * 0.97);
        }

        return base;
    }

    public static Vec3 horizontal(Vec3 direction) {
        Vec3 flat = new Vec3(direction.x, 0.0, direction.z);
        return flat.lengthSqr() < 1.0E-8
                ? new Vec3(0.0, 0.0, 1.0)
                : flat.normalize();
    }

    public static Vec3 right(Vec3 forwardInput) {
        Vec3 forward = horizontal(forwardInput);
        return new Vec3(-forward.z, 0.0, forward.x).normalize();
    }

    public static double smooth(double from, double to, double value) {
        if (to <= from) return value >= to ? 1.0 : 0.0;
        double t = clamp((value - from) / (to - from), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double mix(double a, double b, double t) {
        return a + (b - a) * clamp(t, 0.0, 1.0);
    }
}
