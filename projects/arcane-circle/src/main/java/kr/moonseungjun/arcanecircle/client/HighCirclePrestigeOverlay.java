package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.world.phys.Vec3;

/**
 * Alpha.62 high-circle prestige layer.
 *
 * Circle 7 is fortress/planar authority, 8 is regional/reality authority and 9 is world-law
 * authority. Size is not the only discriminator: single-target law spells use precise multi-plane
 * seals while battlefield catastrophes use sky/terrain scale geometry.
 */
final class HighCirclePrestigeOverlay {
    private HighCirclePrestigeOverlay() {}

    static ArcaneWorldMesh charge(SpellDefinition spell, Vec3 direction, Vec3 target, double range,
                                  double progress, long startedAtNanos, long seed) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(1500);
        if (spell == null || spell.circle() < 7) return m.build();
        double p = clamp(progress, 0.0, 1.0);
        double t = Math.max(0.0, (System.nanoTime() - startedAtNanos) / 1_000_000_000.0);
        switch (spell.id()) {
            case "meteor_swarm" -> meteorCrown(m, target, p, t, false);
            case "power_word_kill" -> executionLaw(m, target, direction, p, t, 9);
            case "finger_of_death" -> executionLaw(m, target, direction, p, t, 7);
            case "time_stop" -> temporalLaw(m, p, t);
            case "wish" -> realityRewrite(m, p, t);
            case "gate" -> worldGate(m, direction, target, p, t);
            case "world_sunder" -> worldAxis(m, direction, target, range, p, t);
            case "earthquake" -> regionalFault(m, target, range, p, t);
            case "sunburst" -> solarJudgment(m, target, p, t);
            case "control_weather" -> weatherThrone(m, target, range, p, t);
            case "prismatic_wall" -> prismAuthority(m, direction, target, range, p, t);
            default -> tierScaffold(m, spell, direction, target, range, p, t);
        }
        return m.build();
    }

    static ArcaneWorldMesh release(SpellDefinition spell, Vec3 direction, Vec3 target, double range,
                                   double elapsedSeconds, double durationSeconds, long seed) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(1200);
        if (spell == null || spell.circle() < 7) return m.build();
        double t = Math.max(0.0, elapsedSeconds);
        double early = clamp(1.0 - t / 2.2, 0.0, 1.0);
        if (early <= 0.0 && spell.circle() < 9) return m.build();
        switch (spell.id()) {
            case "meteor_swarm" -> meteorCrown(m, target, 1.0, t, true);
            case "power_word_kill" -> executionLaw(m, target, direction, Math.max(.18, early), t, 9);
            case "finger_of_death" -> executionLaw(m, target, direction, Math.max(.12, early), t, 7);
            case "time_stop" -> temporalLaw(m, 1.0, t);
            case "wish" -> realityRewrite(m, Math.max(.15, early), t);
            case "gate" -> worldGate(m, direction, target, 1.0, t);
            case "world_sunder" -> worldAxis(m, direction, target, range, Math.max(.20, early), t);
            case "earthquake" -> regionalFault(m, target, range, Math.max(.20, early), t);
            case "sunburst" -> solarJudgment(m, target, Math.max(.15, early), t);
            case "control_weather" -> weatherThrone(m, target, range, 1.0, t);
            case "prismatic_wall" -> prismAuthority(m, direction, target, range, 1.0, t);
            default -> tierScaffold(m, spell, direction, target, range, Math.max(.12, early), t);
        }
        return m.build();
    }

    private static void tierScaffold(ArcaneWorldMesh.Builder m, SpellDefinition spell, Vec3 direction,
                                     Vec3 target, double range, double p, double t) {
        Vec3 anchor = targetAnchor(spell) ? target : Vec3.ZERO;
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis face = ArcaneWorldMesh.Basis.facing(safe(direction));
        int circle = spell.circle();

        if (circle == 7) {
            double r = 2.6 + Math.min(2.2, range * .035);
            m.brokenBand(ground, anchor, r * .78, r, 52, 6, 1.05F, (float)(.18 + .16 * p));
            m.polygon(ground, anchor, r * .66, 7, t * .035, 1.02F);
            int pylons = Math.max(2, (int)Math.floor(p * 7.0));
            for (int i = 0; i < pylons; i++) {
                double a = i * Math.PI * 2.0 / 7.0;
                Vec3 foot = anchor.add(ground.point(a, r * .82));
                m.line(foot, foot.add(0, 1.3 + .35 * (i % 2), 0), i % 3 == 0 ? .92F : .38F);
            }
            m.circle(face, anchor.add(0, .95, 0), r * .34, 32, .42F);
            return;
        }

        if (circle == 8) {
            double r = 4.6 + Math.min(3.8, range * .045);
            m.brokenBand(ground, anchor, r * .78, r, 72, 7, 1.10F, (float)(.20 + .18 * p));
            m.star(ground, anchor, r * .72, r * .37, 8, -t * .024, .72F);
            m.runeRing(ground, anchor, r * .90, 12, .22, spell.id().hashCode(), t * .012, .46F);
            int pillars = Math.max(2, (int)Math.floor(p * 8.0));
            for (int i = 0; i < pillars; i++) {
                double a = i * Math.PI / 4.0;
                Vec3 foot = anchor.add(ground.point(a, r * .72));
                m.line(foot, foot.add(0, 2.4 + .55 * (i % 3), 0), i % 2 == 0 ? .94F : .38F);
            }
            m.circle(face, anchor.add(0, 1.65, 0), r * .42, 44, .46F);
            return;
        }

        double r = 7.0 + Math.min(5.0, range * .045);
        m.brokenBand(ground, anchor, r * .80, r, 92, 9, 1.14F, (float)(.22 + .20 * p));
        m.polygon(ground, anchor, r * .70, 9, t * .010, .84F);
        m.runeRing(ground, anchor, r * .89, 18, .27, spell.id().hashCode(), -t * .008, .55F);
        Vec3 hub = anchor.add(0, 3.0, 0);
        m.circle(face, hub, r * .40, 54, .62F);
        m.line(anchor.add(0, .05, 0), anchor.add(0, 6.2, 0), 1.22F);
        for (int i = 0; i < 9; i++) {
            double a = i * Math.PI * 2.0 / 9.0;
            Vec3 edge = anchor.add(ground.point(a, r * .73));
            m.line(edge, hub.add(face.point(a, r * .17)), i % 3 == 0 ? .95F : .34F);
        }
    }

    private static void meteorCrown(ArcaneWorldMesh.Builder m, Vec3 target, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        Vec3 sky = target.add(0, 36.0, 0);
        double r = 15.5;
        double fade = release ? clamp(1.0 - t / 2.0, .12, 1.0) : Math.max(.14, p);
        m.brokenBand(g, sky, r * .86, r, 112, 8, 1.18F, (float)(.22 * fade));
        m.star(g, sky, r * .76, r * .38, 9, t * .008, 1.05F);
        m.circle(g, sky, r * .55, 72, .56F);
        int ports = release ? 16 : Math.max(4, Math.min(16, 4 + (int)Math.floor(p * 12.0)));
        for (int i = 0; i < ports; i++) {
            double a = i * Math.PI / 8.0;
            double rr = r * (.57 + .25 * (i % 4) / 3.0);
            Vec3 port = sky.add(g.point(a, rr));
            m.diamond(g, port, .38 + .08 * (i % 4), a, 1.18F, (float)(.30 * fade));
            m.line(port, port.add(0, -(release ? 7.0 : 2.0 + 6.0 * p), 0), i % 4 == 0 ? 1.20F : .44F);
        }
        Vec3 crown = sky.add(g.point(Math.PI * 1.76, r * .28));
        m.circle(g, crown, 2.15, 34, 1.32F);
        m.line(crown, target.add(0, release ? 2.0 : 14.0, 0), 1.42F);
    }

    private static void executionLaw(ArcaneWorldMesh.Builder m, Vec3 target, Vec3 direction,
                                     double p, double t, int tier) {
        Vec3 normal = safe(direction);
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(normal);
        double r = tier >= 9 ? 3.25 : 2.15;
        int rings = tier >= 9 ? 9 : 7;
        for (int i = 0; i < rings; i++) {
            double rr = r * (1.0 - i * (tier >= 9 ? .067 : .083));
            double depth = (i - (rings - 1) / 2.0) * (tier >= 9 ? .18 : .11);
            m.polygon(f, target.add(normal.scale(depth)), rr, tier,
                    t * ((i & 1) == 0 ? .018 : -.014) + i * .11,
                    i % 3 == 0 ? 1.18F : .38F);
        }
        m.line(target.add(f.right().scale(-r * 1.45)), target.add(f.right().scale(r * 1.45)), 1.16F);
        m.line(target.add(f.up().scale(-r * 1.45)), target.add(f.up().scale(r * 1.45)), 1.16F);
        if (tier >= 9) {
            m.runeRing(f, target, r * 1.18, 18, .18, 0x9A11, t * .012, .52F);
            m.line(target.subtract(normal.scale(6.0)), target.add(normal.scale(6.0)), 1.30F);
        }
    }

    private static void temporalLaw(ArcaneWorldMesh.Builder m, double p, double t) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis x = ArcaneWorldMesh.Basis.facing(new Vec3(1, 0, 0));
        ArcaneWorldMesh.Basis z = ArcaneWorldMesh.Basis.facing(new Vec3(0, 0, 1));
        double r = 7.2;
        m.brokenBand(g, Vec3.ZERO, r * .86, r, 96, 9, 1.20F, .25F);
        m.polygon(g, Vec3.ZERO, r * .74, 12, 0, .92F);
        Vec3 hub = new Vec3(0, 2.4, 0);
        m.circle(x, hub, 2.45, 52, .75F);
        m.circle(z, hub, 2.45, 52, .46F);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6.0;
            Vec3 base = g.point(a, r * .72);
            m.line(base, base.add(0, 3.3, 0), i % 3 == 0 ? 1.12F : .36F);
        }
        m.line(hub, hub.add(g.point(-Math.PI / 2.0, 2.1)), 1.28F);
        m.line(hub, hub.add(g.point(1.88, 1.55)), .82F);
    }

    private static void realityRewrite(ArcaneWorldMesh.Builder m, double p, double t) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        double r = 6.4;
        Vec3[] levels = {Vec3.ZERO, new Vec3(0, 2.2, 0), new Vec3(0, 4.4, 0), new Vec3(0, 6.6, 0)};
        for (int layer = 0; layer < levels.length; layer++) {
            double rr = r * (1.0 - layer * .14);
            m.polygon(g, levels[layer], rr, 9, t * ((layer & 1) == 0 ? .009 : -.007) + layer * .09,
                    layer == 0 ? 1.18F : .56F);
            m.runeRing(g, levels[layer], rr * .82, 9, .20, 0x5715 + layer * 91, t * .006, .40F);
        }
        for (int i = 0; i < 9; i++) {
            double a = i * Math.PI * 2.0 / 9.0;
            Vec3 low = g.point(a, r * .72);
            Vec3 high = levels[3].add(g.point(a + .12, r * .45));
            m.line(low, high, i % 3 == 0 ? 1.08F : .38F);
        }
    }

    private static void worldGate(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target, double p, double t) {
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(safe(direction));
        gateFrame(m, f, Vec3.ZERO.add(0, 1.2, 0), 4.4, t);
        gateFrame(m, f, target.add(0, 1.2, 0), 5.6, -t);
        for (int i = 0; i < 9; i++) {
            double a = i * Math.PI * 2.0 / 9.0;
            Vec3 a0 = f.point(a, 4.0).add(0, 1.2, 0);
            Vec3 a1 = target.add(f.point(a + .08, 5.0)).add(0, 1.2, 0);
            m.line(a0, a1, i % 3 == 0 ? .92F : .28F, i % 3 == 0 ? 1.0F : .70F, .34F);
        }
    }

    private static void gateFrame(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis f,
                                  Vec3 center, double radius, double t) {
        m.circle(f, center, radius, 72, 1.16F);
        m.polygon(f, center, radius * .82, 9, t * .012, .62F);
        m.runeRing(f, center, radius * .92, 9, .22, center.hashCode(), -t * .01, .42F);
    }

    private static void worldAxis(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target,
                                  double range, double p, double t) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        Vec3 d = horizontal(direction);
        Vec3 right = new Vec3(-d.z, 0, d.x);
        double length = Math.max(24.0, Math.min(76.0, range * .82));
        double half = length * .5;
        Vec3 a = target.subtract(d.scale(half));
        Vec3 b = target.add(d.scale(half));
        m.line(a, b, 1.40F);
        m.line(a.add(right.scale(2.2)), b.add(right.scale(-2.2)), .54F);
        m.line(a.add(right.scale(-2.2)), b.add(right.scale(2.2)), .54F);
        for (int i = -4; i <= 4; i++) {
            Vec3 node = target.add(d.scale(i * half / 4.0));
            double r = 2.5 + (4 - Math.abs(i)) * .55;
            m.brokenBand(g, node, r * .72, r, 34, 5, i == 0 ? 1.18F : .52F, .22F);
            m.line(node, node.add(0, 2.2 + (4 - Math.abs(i)) * .45, 0), i == 0 ? 1.18F : .34F);
        }
    }

    private static void regionalFault(ArcaneWorldMesh.Builder m, Vec3 target, double range, double p, double t) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        double r = Math.max(10.0, Math.min(26.0, range * .44));
        m.brokenBand(g, target, r * .82, r, 88, 7, 1.08F, .20F);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6.0 + Math.sin(i * 1.7) * .09;
            Vec3 inner = target.add(g.point(a, r * .16));
            Vec3 outer = target.add(g.point(a + .06 * ((i & 1) == 0 ? 1 : -1), r * (.70 + .16 * (i % 3) / 2.0)));
            m.line(inner, outer, i % 3 == 0 ? 1.16F : .46F);
        }
    }

    private static void solarJudgment(ArcaneWorldMesh.Builder m, Vec3 target, double p, double t) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        Vec3 sky = target.add(0, 18.0, 0);
        double r = 8.8;
        m.starPlate(g, sky, r, r * .46, 12, t * .01, 1.18F, .10F);
        m.brokenBand(g, sky, r * .78, r, 84, 8, 1.22F, .18F);
        for (int i = 0; i < 12; i++) {
            Vec3 ray = sky.add(g.point(i * Math.PI / 6.0, r * .58));
            m.line(ray, target.add(g.point(i * Math.PI / 6.0, 3.0)), i % 3 == 0 ? .94F : .32F);
        }
    }

    private static void weatherThrone(ArcaneWorldMesh.Builder m, Vec3 target, double range, double p, double t) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        Vec3 sky = target.add(0, 24.0, 0);
        double r = Math.max(9.0, Math.min(18.0, range * .28));
        m.brokenBand(g, sky, r * .82, r, 96, 6, 1.06F, .18F);
        m.polygon(g, sky, r * .68, 8, t * .012, .64F);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0;
            Vec3 cloud = sky.add(g.point(a, r * .76));
            m.helix(cloud, new Vec3(0, -1, 0), ArcaneWorldMesh.Basis.facing(new Vec3(0, -1, 0)),
                    8.0 + (i % 3) * 2.0, .45, 2, 18, i % 2 == 0 ? .64F : .28F, true);
        }
    }

    private static void prismAuthority(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target,
                                       double range, double p, double t) {
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(safe(direction));
        double width = Math.max(10.0, Math.min(28.0, range * .45));
        double height = 9.0;
        Vec3 right = f.right();
        for (int layer = 0; layer < 7; layer++) {
            double x = -width * .5 + width * (layer + .5) / 7.0;
            Vec3 base = target.add(right.scale(x));
            m.line(base, base.add(0, height, 0), layer == 3 ? 1.16F : .46F);
            m.diamond(f, base.add(0, height + .8, 0), .40, layer * .33, 1.08F, .22F);
        }
        m.line(target.add(right.scale(-width * .5)), target.add(right.scale(width * .5)), 1.18F);
        m.line(target.add(right.scale(-width * .5)).add(0, height, 0),
                target.add(right.scale(width * .5)).add(0, height, 0), 1.18F);
    }

    private static boolean targetAnchor(SpellDefinition spell) {
        return switch (spell.sigilAnchor()) {
            case TARGET, GROUND_TARGET -> true;
            default -> false;
        };
    }

    private static Vec3 safe(Vec3 value) {
        return value == null || value.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : value.normalize();
    }

    private static Vec3 horizontal(Vec3 value) {
        Vec3 d = value == null ? Vec3.ZERO : new Vec3(value.x, 0, value.z);
        return d.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : d.normalize();
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
