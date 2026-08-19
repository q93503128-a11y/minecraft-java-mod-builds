package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Alpha.44 authored staging for the spells whose finished alpha.43 formula was too eager during charge.
 * This is deliberately small and explicit: it does not infer visuals from circle number.
 */
final class HighCircleMaintenanceOverlay {
    private static final Set<String> REPLACED_CHARGE = Set.of(
            "plane_shift", "antimagic_field", "meteor_swarm", "shapechange", "time_stop",
            "wish", "gate", "foresight", "world_sunder"
    );

    private HighCircleMaintenanceOverlay() {}

    static boolean replacesChargeTimeline(SpellDefinition spell) {
        return spell != null && REPLACED_CHARGE.contains(spell.id());
    }

    static ArcaneWorldMesh charge(SpellDefinition spell, Vec3 direction, Vec3 target, double progress,
                                  long startedAtNanos, long seed) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(980);
        if (spell == null || !REPLACED_CHARGE.contains(spell.id())) return m.build();
        double p = clamp(progress, 0.0, 1.0);
        double t = Math.max(0.0, (System.nanoTime() - startedAtNanos) / 1_000_000_000.0);
        switch (spell.id()) {
            case "plane_shift" -> planeShift(m, direction, p, t);
            case "antimagic_field" -> antimagic(m, p, t, false);
            case "meteor_swarm" -> meteorSwarm(m, target, p, t, false);
            case "shapechange" -> shapechange(m, p, t, false);
            case "time_stop" -> timeStop(m, p, t, false);
            case "wish" -> wish(m, p, t, false);
            case "gate" -> gate(m, direction, target, p, t, false);
            case "foresight" -> foresight(m, direction, p, t, false);
            case "world_sunder" -> worldSunder(m, direction, target, p, t, false, seed);
            default -> { }
        }
        return m.build();
    }

    static ArcaneWorldMesh release(SpellDefinition spell, Vec3 direction, Vec3 target,
                                   double elapsedSeconds, double durationSeconds, long seed) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(620);
        if (spell == null || spell.circle() < 7) return m.build();
        double t = Math.max(0.0, elapsedSeconds);
        switch (spell.id()) {
            case "antimagic_field" -> antimagic(m, 1.0, t, true);
            case "meteor_swarm" -> meteorSwarm(m, target, 1.0, t, true);
            case "shapechange" -> shapechange(m, 1.0, t, true);
            case "time_stop" -> timeStop(m, 1.0, t, true);
            case "wish" -> wish(m, 1.0, t, true);
            case "gate" -> gate(m, direction, target, 1.0, t, true);
            case "foresight" -> foresight(m, direction, 1.0, t, true);
            case "world_sunder" -> worldSunder(m, direction, target, 1.0, t, true, seed);
            case "true_seeing" -> seeingHeartbeat(m, t);
            case "solar_guard" -> solarHeartbeat(m, t);
            default -> { }
        }
        return m.build();
    }

    private static void planeShift(ArcaneWorldMesh.Builder m, Vec3 direction, double p, double t) {
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(safe(direction));
        int[] order = {0, -1, 1, -2, 2, -3, 3};
        int visible = Math.max(1, Math.min(7, 1 + (int) Math.floor(p * 6.8)));
        double r = 1.58;
        for (int i = 0; i < visible; i++) {
            int layer = order[i];
            Vec3 c = f.normal().scale(layer * .15).add(0, .92, 0);
            m.polygon(f, c, r * (1.0 - Math.abs(layer) * .045), 7,
                    t * .025 + layer * .13, layer == 0 ? 1.18F : .46F);
        }
        if (p > .58) {
            int links = Math.max(1, Math.min(7, 1 + (int) Math.floor((p - .58) / .42 * 6.0)));
            for (int i = 0; i < links; i++) {
                double a = i * Math.PI * 2.0 / 7.0;
                m.line(f.point(a, r * .94).add(f.normal().scale(-.44)).add(0, .92, 0),
                        f.point(a + .08, r * .72).add(f.normal().scale(.44)).add(0, .92, 0),
                        i % 3 == 0 ? 1.02F : .42F, i % 3 == 0 ? 1.0F : .72F, i % 3 == 0 ? .72F : .34F);
            }
        }
    }

    private static void antimagic(ArcaneWorldMesh.Builder m, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis x = ArcaneWorldMesh.Basis.facing(new Vec3(1, 0, 0));
        double r = 3.05;
        double spin = release ? 0.0 : t * .018;
        double open = release ? 1.0 : Math.max(.18, p);
        m.arc(g, Vec3.ZERO, r, .18 + spin, Math.PI * 1.28 * open, 48, 1.16F);
        if (release || p > .34) m.arc(g, Vec3.ZERO, r * .72, -2.30 - spin,
                Math.PI * .78 * Math.min(1.0, p + .20), 34, .66F);
        if (release || p > .60) m.arc(g, new Vec3(0, .85, 0), r * .44, .9,
                Math.PI * 1.05 * Math.min(1.0, p + .12), 30, .38F);
        int spokes = release ? 8 : Math.max(1, (int) Math.floor(p * 8.0));
        for (int i = 0; i < spokes; i++) {
            double a = i * Math.PI / 4.0;
            Vec3 out = g.point(a, r * .90);
            m.line(out, g.point(a + .16 * ((i & 1) == 0 ? 1 : -1), r * .22),
                    (i & 1) == 0 ? .82F : .36F);
        }
        if (release) {
            double scan = (t * .62) % (Math.PI * 2.0);
            m.arc(x, new Vec3(0, .92, 0), r * .54, scan, Math.PI * .46, 22, .52F);
        }
    }

    private static void meteorSwarm(ArcaneWorldMesh.Builder m, Vec3 target, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        Vec3 sky = target.add(0, 22, 0);
        double r = 7.8;
        if (release) {
            // The casting seal does not remain as a rotating HUD-like target marker. It fractures
            // into sixteen short apertures, then disappears so the falling meteor bodies dominate.
            double q = clamp(t / 1.05, 0.0, 1.0);
            if (q >= 1.0) return;
            double fadeRadius = r * (1.0 - q * .16);
            for (int quadrant = 0; quadrant < 4; quadrant++) {
                double a = quadrant * Math.PI / 2.0 + .10 + q * .08;
                m.arc(g, sky, fadeRadius, a, Math.PI * (.34 - q * .10), 18,
                        quadrant == 0 ? 1.04F : .42F);
            }
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8.0;
                Vec3 port = sky.add(g.point(a, r * (.70 + .18 * (i % 4) / 3.0)));
                double size = .24 * (1.0 - q * .62);
                m.diamond(g, port, Math.max(.07, size), a, 1.08F, (float) (.20 * (1.0 - q)));
                m.line(port, port.add(0, -(1.0 + q * 3.2), 0), i % 4 == 0 ? .78F : .28F,
                        i % 4 == 0 ? 1.0F : .72F, (float) (.70 * (1.0 - q)));
            }
            return;
        }
        m.circle(g, sky, r, 84, 1.14F);
        if (p > .28) m.circle(g, sky, r * .66, 60, .44F);
        int ticks = Math.max(4, Math.min(16,
                4 + (int) Math.floor(Math.max(0.0, p - .38) / .62 * 12.0)));
        for (int i = 0; i < ticks; i++) {
            double a = i * Math.PI / 8.0;
            Vec3 outer = sky.add(g.point(a, r * .92));
            Vec3 inner = sky.add(g.point(a, r * (i % 4 == 0 ? .72 : .82)));
            m.line(outer, inner, i % 4 == 0 ? 1.02F : .34F);
            if (i % 4 == 0) m.diamond(g, outer, .20, a, 1.08F, .18F);
        }
    }

    private static void shapechange(ArcaneWorldMesh.Builder m, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(new Vec3(0, 0, 1));
        int visible = release ? 7 : Math.max(1, Math.min(7, 1 + (int) Math.floor(p * 6.8)));
        double breathe = release ? .92 + .08 * Math.sin(t * 1.30) : 1.0;
        for (int layer = 0; layer < visible; layer++) {
            double y = .16 + layer * .28;
            double rr = (.46 + layer * .10 + .04 * Math.sin(t + layer)) * breathe;
            m.polygon(g, new Vec3(0, y, 0), rr, 3 + layer % 5,
                    t * ((layer & 1) == 0 ? .06 : -.05) + layer * .21,
                    layer % 3 == 0 ? .88F : .36F);
        }
        if (release || p > .64) {
            Vec3 shoulder = new Vec3(0, 1.34, 0);
            for (int side : new int[]{-1, 1}) {
                Vec3 root = shoulder.add(f.right().scale(side * .24));
                Vec3 tip = root.add(f.right().scale(side * .86 * breathe)).add(0, .18, 0);
                m.line(root, tip, 1.08F);
                for (int k = 0; k < 3; k++) m.line(tip,
                        tip.add(f.right().scale(side * (.22 + .09 * k))).add(f.up().scale(.16 - .10 * k)), .32F);
            }
        }
    }

    private static void timeStop(ArcaneWorldMesh.Builder m, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis x = ArcaneWorldMesh.Basis.facing(new Vec3(1, 0, 0));
        double r = 4.2;
        double spin = release ? 0.0 : t * .018;
        m.circle(g, Vec3.ZERO, r, 88, 1.16F);
        if (release || p > .24) m.polygon(g, Vec3.ZERO, r * .82, 12, spin, .70F);
        if (release || p > .46) m.circle(g, Vec3.ZERO, r * .58, 62, .36F);
        int ticks = release ? 12 : Math.max(3, Math.min(12, (int) Math.floor(p * 12.0)));
        for (int i = 0; i < ticks; i++) {
            double a = i * Math.PI / 6.0;
            m.line(g.point(a, r * .80), g.point(a, r), i % 3 == 0 ? 1.02F : .30F);
        }
        Vec3 hub = new Vec3(0, 1.48, 0);
        if (release || p > .58) {
            m.circle(x, hub, r * .32, 48, .62F);
            double hand = release ? -.72 : -Math.PI / 2.0 + t * .09;
            m.line(hub, hub.add(g.point(hand, r * .28)), 1.18F);
            m.line(hub, hub.add(g.point(release ? 1.92 : -Math.PI / 2.0 - t * .18, r * .19)), .72F);
        }
        if (release || p > .80) {
            ArcaneWorldMesh.Basis z = ArcaneWorldMesh.Basis.facing(new Vec3(0, 0, 1));
            m.circle(z, hub, r * .23, 34, .34F);
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 4.0 + i * Math.PI / 2.0;
                m.line(g.point(a, r * .52), g.point(a, r * .52).add(0, 1.55, 0), .30F);
            }
        }
    }

    private static void wish(ArcaneWorldMesh.Builder m, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        double r = 3.45;
        Vec3 middle = new Vec3(0, 2.15, 0);
        Vec3 top = new Vec3(0, 4.15, 0);
        m.polygon(g, Vec3.ZERO, r, 9, t * .008, 1.16F);
        if (release || p > .30) m.polygon(g, middle, r * .52, 9, -t * .006, .68F);
        if (release || p > .56) m.polygon(g, top, r * .66, 9, t * .005 + .13, .72F);
        if (release || p > .62) {
            int links = release ? 9 : Math.max(1, Math.min(9,
                    1 + (int) Math.floor((p - .62) / .38 * 8.0)));
            for (int i = 0; i < links; i++) {
                double a = i * Math.PI * 2.0 / 9.0;
                Vec3 low = g.point(a, r * .88).add(0, .42 + .18 * (i % 3), 0);
                Vec3 mid = middle.add(g.point(a + .06 * ((i & 1) == 0 ? 1 : -1), r * .42));
                Vec3 high = top.add(g.point(a, r * .52));
                m.diamond(g, low, .14, a, 1.05F, .13F);
                m.line(low, mid, .70F);
                m.line(mid, high, .36F);
            }
        }
        if (release || p > .82) m.circle(g, top, r * .28, 36, .34F);
        if (release) {
            double contract = .18 + .12 * (.5 + .5 * Math.sin(t * 1.25));
            for (int i = 0; i < 3; i++) {
                double a = i * Math.PI * 2.0 / 3.0;
                m.line(top.add(g.point(a, r * .26)), middle.add(g.point(a + .18, r * contract)), .82F);
            }
        }
    }

    private static void gate(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target,
                             double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(flat(direction));
        Vec3 base = safe(direction).scale(2.4).add(0, 3.0, 0);
        double r = 3.0;
        int frames = release ? 7 : Math.max(1, Math.min(7, 1 + (int) Math.floor(p * 6.8)));
        for (int d = 0; d < frames; d++) {
            Vec3 c = base.add(f.normal().scale(d * .25));
            double rr = r * (1.0 - d * .078);
            m.polygon(f, c, rr, 12 - d % 2 * 4,
                    t * ((d & 1) == 0 ? .008 : -.007) + d * .12,
                    d == 0 ? 1.18F : .38F);
        }
        if (release || p > .56) for (int i = 0; i < 4; i++) {
            double a = Math.PI / 4.0 + i * Math.PI / 2.0;
            m.line(base.add(f.point(a, r * .90)),
                    base.add(f.normal().scale(1.55)).add(f.point(a, r * .44)), .66F);
        }
        if ((release || p > .82) && target.lengthSqr() > 4.0) m.line(base, target.add(0, 2.6, 0), .30F);
    }

    private static void foresight(ArcaneWorldMesh.Builder m, Vec3 direction, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(safe(direction));
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        Vec3 eye = new Vec3(0, 2.0, 0);
        double r = .88;
        m.arc(f, eye, r, .10, Math.PI * .82, 32, 1.10F);
        if (release || p > .20) m.arc(f, eye, r, Math.PI + .10, Math.PI * .82, 32, 1.10F);
        if (release || p > .34) m.circle(f, eye, .25, 22, .64F);
        int lanes = release ? 4 : Math.max(1, Math.min(4,
                1 + (int) Math.floor(Math.max(0.0, p - .42) / .58 * 3.0)));
        if (release || p > .42) for (int lane = 0; lane < lanes; lane++) {
            double side = (lane - 1.5) * .28;
            Vec3 a = f.right().scale(side * .25).add(0, .08, 0);
            Vec3 b = safe(direction).scale(1.4).add(f.right().scale(side)).add(0, .32 + .16 * lane, 0);
            Vec3 c = safe(direction).scale(3.0).add(f.right().scale(side * (1.5 + (lane % 2) * .35)))
                    .add(0, .50 + .10 * lane, 0);
            m.line(a, b, lane == 1 ? .72F : .32F);
            m.line(b, c, lane == 1 ? 1.12F : .30F);
            m.line(c, eye, .24F);
        }
        if (release || p > .72) m.circle(g, Vec3.ZERO, 1.22, 48, .34F);
        if (release) {
            double beat = .5 + .5 * Math.sin(t * Math.PI * 2.0 / 3.0);
            if (beat > .90) m.circle(f, eye, .34 + (beat - .90) * 2.2, 26, .30F);
        }
    }

    private static void worldSunder(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target,
                                    double p, double t, boolean release, long seed) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        Vec3 forward = flat(direction);
        Vec3 sideAxis = new Vec3(-forward.z, 0, forward.x);
        double length = 54.0;
        double grow = release ? clamp(t / 1.15, 0.0, 1.0) : .12 + .72 * p;
        Vec3 origin = target.subtract(forward.scale(length * .42));
        Vec3 previous = origin;
        int visible = release ? 13 : Math.max(2, Math.min(13, 2 + (int) Math.floor(p * 11.0)));
        for (int i = 0; i < visible; i++) {
            double q = i / 12.0;
            Vec3 along = origin.add(forward.scale(length * q));
            Vec3 side = sideAxis.scale(Math.sin(i * 1.73 + (seed & 15) * .03) * (1.1 + .18 * (i % 3)));
            Vec3 node = along.add(side);
            if (i > 0) m.line(previous, node, i % 3 == 0 ? 1.16F : .62F);
            if (release && q <= grow) m.line(node, node.add(0, 1.0 + .24 * (i % 4), 0),
                    i % 3 == 0 ? .70F : .30F);
            m.diamond(g, node, .13 + (i % 3) * .02, i * .37, 1.02F, .12F);
            previous = node;
        }
        if (release || p > .72) m.circle(g, target, 2.2, 42, .30F);
    }

    private static void seeingHeartbeat(ArcaneWorldMesh.Builder m, double t) {
        double phase = t % 4.0;
        if (phase > .65) return;
        double q = phase / .65;
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(new Vec3(0, 0, 1));
        Vec3 eye = new Vec3(0, 1.65, 0);
        m.arc(f, eye, .78 + q * .42, .10, Math.PI * .82, 28, .62F);
        m.arc(f, eye, .78 + q * .42, Math.PI + .10, Math.PI * .82, 28, .62F);
    }

    private static void solarHeartbeat(ArcaneWorldMesh.Builder m, double t) {
        double phase = t % 3.6;
        if (phase > .55) return;
        double q = phase / .55;
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        double r = .86 + q * .72;
        m.polygon(g, new Vec3(0, .08, 0), r, 8, q * .22, .76F);
        for (int i = 0; i < 8; i += 2) {
            double a = i * Math.PI / 4.0;
            m.line(g.point(a, r * .82).add(0, .08, 0), g.point(a, r * 1.16).add(0, .35, 0), .46F);
        }
    }

    private static Vec3 safe(Vec3 value) {
        return value == null || value.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : value.normalize();
    }

    private static Vec3 flat(Vec3 value) {
        Vec3 flat = value == null ? Vec3.ZERO : new Vec3(value.x, 0, value.z);
        return flat.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : flat.normalize();
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
