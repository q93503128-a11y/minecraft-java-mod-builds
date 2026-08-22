package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.MeteorBarragePattern;
import kr.moonseungjun.arcanecircle.magic.NinthCircleMagnitude;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * High-circle prestige layer. The alpha.64 pass preserves the accepted silhouettes from alpha.62
 * but makes giant arrays information-dense: multiple border bands, rune belts, chords, satellite
 * seals and radial bindings grow with occupied space instead of stretching one empty circle.
 */
final class HighCirclePrestigeOverlay {
    private HighCirclePrestigeOverlay() {}

    static ArcaneWorldMesh charge(SpellDefinition spell, Vec3 direction, Vec3 target, double range,
                                  double progress, long startedAtNanos, long seed) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(2200);
        if (spell == null || spell.circle() < 7) return m.build();
        double p = clamp(progress, 0.0, 1.0);
        double t = Math.max(0.0, (System.nanoTime() - startedAtNanos) / 1_000_000_000.0);
        switch (spell.id()) {
            case "meteor_swarm" -> meteorCrown(m, target, range, p, t, seed, false);
            case "power_word_kill" -> executionLaw(m, target, direction, p, t, 9);
            case "finger_of_death" -> executionLaw(m, target, direction, p, t, 7);
            case "fire_storm" -> fireStormDominion(m, target, p, t);
            case "reverse_gravity" -> gravityCathedral(m, target, p, t);
            case "plane_shift" -> planarTransit(m, direction, target, p, t);
            case "forcecage" -> forceCitadel(m, target, direction, p, t);
            case "prismatic_spray" -> prismaticFan(m, direction, p, t);
            case "time_stop" -> temporalLaw(m, range, p, t);
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
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(2000);
        if (spell == null || spell.circle() < 7) return m.build();
        double t = Math.max(0.0, elapsedSeconds);
        double life = spell.circle() == 7 ? 2.0 : spell.circle() == 8 ? 2.8 : 4.6;
        double early = clamp(1.0 - t / life, 0.0, 1.0);
        if (early <= 0.0 && spell.circle() < 9) return m.build();
        switch (spell.id()) {
            case "meteor_swarm" -> meteorCrown(m, target, range, 1.0, t, seed, true);
            case "power_word_kill" -> executionLaw(m, target, direction, Math.max(.18, early), t, 9);
            case "finger_of_death" -> executionLaw(m, target, direction, Math.max(.12, early), t, 7);
            case "fire_storm" -> fireStormDominion(m, target, Math.max(.20, early), t);
            case "reverse_gravity" -> gravityCathedral(m, target, 1.0, t);
            case "plane_shift" -> planarTransit(m, direction, target, Math.max(.18, early), t);
            case "forcecage" -> forceCitadel(m, target, direction, 1.0, t);
            case "prismatic_spray" -> prismaticFan(m, direction, Math.max(.18, early), t);
            case "time_stop" -> temporalLaw(m, range, 1.0, t);
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
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(safe(direction));
        int c = spell.circle();
        double r = switch (c) {
            case 7 -> 4.2 + Math.min(2.6, range * .040);
            case 8 -> 7.2 + Math.min(4.0, range * .050);
            default -> 11.2 + Math.min(5.8, range * .055);
        };
        m.brokenBand(g, anchor, r * .80, r, 66 + c * 5, c, 1.14F, (float)(.18 + .17 * p));
        m.polygon(g, anchor, r * .69, c, t * (c == 9 ? .008 : .018), c == 9 ? .94F : .76F);
        m.runeRing(g, anchor, r * .90, c == 7 ? 10 : c == 8 ? 14 : 18,
                .18 + .025 * (c - 7), spell.id().hashCode(), -t * .010, .48F);
        denseGrandArray(m, g, anchor, r, c, spell.id().hashCode(), t, (float)(.18 + .12 * p));
        double tower = c == 7 ? 2.8 : c == 8 ? 5.2 : 8.2;
        int pillars = c;
        for (int i = 0; i < pillars; i++) {
            double a = i * Math.PI * 2.0 / pillars;
            Vec3 foot = anchor.add(g.point(a, r * .72));
            m.line(foot, foot.add(0, tower + .45 * (i % 3), 0), i % 3 == 0 ? 1.02F : .36F);
        }
        Vec3 hub = anchor.add(0, tower * .58, 0);
        m.circle(f, hub, r * (c == 9 ? .36 : .28), 38 + c * 3, .48F);
        if (c == 9) m.line(anchor, anchor.add(0, tower + 2.0, 0), 1.20F);
    }

    private static void denseGrandArray(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis basis, Vec3 center,
                                        double radius, int tier, int seed, double rotation, float alpha) {
        if (radius < 5.0) return;
        int runes = Math.max(tier * 3, Math.min(72, (int)Math.round(radius * 1.15)));
        int spokes = tier >= 9 ? 18 : tier >= 8 ? 16 : 14;
        m.brokenBand(basis, center, radius * .91, radius * .955, 96 + tier * 8, tier, 1.06F, alpha * .66F);
        m.brokenBand(basis, center, radius * .74, radius * .785, 84 + tier * 6, tier - 1, .88F, alpha * .48F);
        m.brokenBand(basis, center, radius * .57, radius * .615, 72 + tier * 5, tier - 2, .78F, alpha * .38F);
        m.runeRing(basis, center, radius * .972, runes, Math.max(.16, radius * .012),
                seed ^ 0x51A71, -rotation * .70, .38F);
        m.runeRing(basis, center, radius * .805, Math.max(tier * 2, runes / 2),
                Math.max(.13, radius * .009), seed ^ 0x19C3D, rotation * .46, .28F);
        m.runeChords(basis, center, radius * .52, tier * 2, Math.max(3, tier - 2), rotation * .31, .24F);
        for (int i = 0; i < spokes; i++) {
            double a = rotation * .12 + i * Math.PI * 2.0 / spokes;
            Vec3 inner = center.add(basis.point(a, radius * .18));
            Vec3 outer = center.add(basis.point(a + ((i & 1) == 0 ? .018 : -.018), radius * .88));
            m.line(inner, outer, i % tier == 0 ? .72F : .22F, 1.0F, alpha * .86F);
            if (i % 3 == 0) {
                Vec3 node = center.add(basis.point(a, radius * .68));
                double nodeRadius = Math.max(.28, Math.min(1.35, radius * .035));
                m.circle(basis, node, nodeRadius, 18, .34F);
                m.diamond(basis, node, nodeRadius * .58, a, 1.08F, alpha * .64F);
            }
        }
    }

    private static void meteorCrown(ArcaneWorldMesh.Builder m, Vec3 target, double range,
                                    double p, double t, long seed, boolean release) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        double field = NinthCircleMagnitude.meteorFieldRadius(range);
        double crownRadius = NinthCircleMagnitude.cityfallCrownRadius(range);
        Vec3 ground = target.add(0, .06, 0);
        Vec3 sky = target.add(0, NinthCircleMagnitude.cityfallSkyHeight(range), 0);
        double fade = release ? clamp(1.0 - t / 7.5, .10, 1.0) : Math.max(.14, p);
        m.brokenBand(g, ground, field * .965, field, 180, 11, 1.18F, (float)(.15 * fade));
        m.brokenBand(g, ground, field * .865, field * .90, 162, 9, .88F, (float)(.10 * fade));
        m.runeRing(g, ground, field * .982, Math.min(72, Math.max(36, (int)Math.round(field * .46))),
                Math.max(.45, field * .008), 0xC17F411, -t * .0025, .34F);
        m.brokenBand(g, sky, crownRadius * .86, crownRadius, 168, 9, 1.22F, (float)(.23 * fade));
        m.star(g, sky, crownRadius * .78, crownRadius * .36, 9, t * .004, 1.10F);
        m.circle(g, sky, crownRadius * .58, 96, .56F);
        denseGrandArray(m, g, sky, crownRadius, 9, 0x4D37E0, t, (float)(.24 * fade));
        List<MeteorBarragePattern.Strike> strikes = MeteorBarragePattern.strikes(seed, range);
        int ordinary = Math.max(1, strikes.size() - 1);
        int visible = release ? ordinary : Math.max(7, Math.min(ordinary, (int)Math.ceil(ordinary * (.12 + .88 * p))));
        int stride = visible >= ordinary ? 1 : Math.max(1, ordinary / visible);
        int shown = 0;
        for (int i = 0; i < ordinary && shown < visible; i += stride, shown++) {
            MeteorBarragePattern.Strike s = strikes.get(i);
            Vec3 port = sky.add(s.offsetX(), 0, s.offsetZ());
            double size = .34 + s.scale() * .18;
            m.diamond(g, port, size, i * .37, i % 9 == 0 ? 1.30F : .80F, (float)(.26 * fade));
            m.circle(g, port, size * 1.42, 14, i % 9 == 0 ? .74F : .26F);
            m.line(port, port.add(0, -(release ? 12.0 + s.scale() * 2.5 : 3.0 + 14.0 * p), 0),
                    i % 9 == 0 ? 1.02F : .28F, 1.0F, (float)(.72 * fade));
        }
        MeteorBarragePattern.Strike crownStrike = strikes.getLast();
        Vec3 crown = sky.add(crownStrike.offsetX(), 0, crownStrike.offsetZ());
        double crownNode = Math.max(6.5, crownStrike.scale() * 1.75);
        m.circle(g, crown, crownNode, 72, 1.44F);
        m.polygon(g, crown, crownNode * .82, 9, -t * .006, .88F);
        m.runeRing(g, crown, crownNode * .94, 27, Math.max(.24, crownNode * .045), 0xC20A9, t * .009, .58F);
        m.line(crown, target.add(0, release ? 1.0 : field * .18, 0), 1.65F);
    }

    private static void executionLaw(ArcaneWorldMesh.Builder m, Vec3 target, Vec3 direction,
                                     double p, double t, int tier) {
        Vec3 normal = safe(direction);
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(normal);
        double r = tier >= 9 ? 4.8 : 2.55;
        int rings = tier >= 9 ? 9 : 7;
        for (int i = 0; i < rings; i++) {
            double rr = r * (1.0 - i * (tier >= 9 ? .058 : .080));
            double depth = (i - (rings - 1) / 2.0) * (tier >= 9 ? .28 : .13);
            m.polygon(f, target.add(normal.scale(depth)), rr, tier,
                    t * ((i & 1) == 0 ? .014 : -.011) + i * .11, i % 3 == 0 ? 1.22F : .38F);
        }
        m.line(target.add(f.right().scale(-r * 1.55)), target.add(f.right().scale(r * 1.55)), 1.20F);
        m.line(target.add(f.up().scale(-r * 1.55)), target.add(f.up().scale(r * 1.55)), 1.20F);
        if (tier >= 9) {
            m.runeRing(f, target, r * 1.32, 36, .24, 0x9A11, t * .010, .58F);
            m.runeChords(f, target, r * .92, 18, 7, -t * .006, .34F);
            for (int i = 0; i < 9; i++) {
                double a = i * Math.PI * 2.0 / 9.0;
                m.diamond(f, target.add(f.point(a, r * 1.12)), .34, a, 1.14F, .24F);
            }
            m.line(target.subtract(normal.scale(9.0)), target.add(normal.scale(9.0)), 1.38F);
        }
    }

    private static void fireStormDominion(ArcaneWorldMesh.Builder m, Vec3 target, double p, double t) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        Vec3 sky = target.add(0, 28.0, 0);
        double r = 14.5;
        m.brokenBand(g, sky, r * .80, r, 92, 7, 1.18F, .22F);
        m.polygon(g, sky, r * .68, 7, t * .012, .74F);
        denseGrandArray(m, g, sky, r, 7, 0xF173, t, .16F);
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3.0;
            Vec3 port = sky.add(g.point(a, r * .62));
            Vec3 floor = target.add(g.point(a, 5.0));
            m.circle(g, port, 1.25, 28, i % 2 == 0 ? 1.10F : .52F);
            m.line(port, floor.add(0, 1.0, 0), i % 2 == 0 ? 1.12F : .44F);
            m.brokenBand(g, floor, 2.7, 3.4, 30, 5, .72F, .18F);
        }
    }

    private static void gravityCathedral(ArcaneWorldMesh.Builder m, Vec3 target, double p, double t) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis x = ArcaneWorldMesh.Basis.facing(new Vec3(1, 0, 0));
        double r = 10.8;
        m.brokenBand(g, target, r * .80, r, 82, 7, 1.10F, .20F);
        m.polygon(g, target, r * .67, 7, -t * .018, .62F);
        denseGrandArray(m, g, target, r, 7, 0x6A17, -t, .14F);
        for (int i = 0; i < 7; i++) {
            double a = i * Math.PI * 2.0 / 7.0;
            Vec3 foot = target.add(g.point(a, r * .68));
            m.line(foot, foot.add(0, 13.5, 0), i % 2 == 0 ? .82F : .34F);
        }
        Vec3 high = target.add(0, 13.5, 0);
        m.circle(x, high, 4.5, 52, .70F);
        m.circle(g, high, r * .54, 58, .44F);
    }

    private static void planarTransit(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target, double p, double t) {
        Vec3 normal = safe(direction);
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(normal);
        double r = 5.4;
        for (int i = 0; i < 7; i++) {
            double depth = (i - 3) * .28;
            Vec3 center = normal.scale(depth).add(0, 1.0, 0);
            m.polygon(f, center, r * (1.0 - Math.abs(i - 3) * .035), 7,
                    t * ((i & 1) == 0 ? .018 : -.016) + i * .09, i == 3 ? 1.18F : .42F);
        }
        m.runeRing(f, new Vec3(0,1.0,0), r * .92, 21, .20, 0x71A6E, -t * .011, .36F);
        if (target.lengthSqr() > 4.0) {
            Vec3 far = target.add(0, 1.0, 0);
            m.circle(f, far, r * 1.12, 62, .86F);
            m.runeRing(f, far, r * .98, 21, .20, 0x71A6F, t * .010, .34F);
            for (int i = 0; i < 7; i++) {
                double a = i * Math.PI * 2.0 / 7.0;
                m.line(f.point(a, r * .80).add(0, 1.0, 0), far.add(f.point(a + .08, r * .90)),
                        i % 2 == 0 ? .56F : .24F, 1.0F, .40F);
            }
        }
    }

    private static void forceCitadel(ArcaneWorldMesh.Builder m, Vec3 target, Vec3 direction, double p, double t) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(safe(direction));
        double r = 4.4;
        m.brokenBand(g, target, r * .82, r, 58, 7, 1.08F, .18F);
        for (int i = 0; i < 7; i++) {
            double a = i * Math.PI * 2.0 / 7.0;
            Vec3 foot = target.add(g.point(a, r * .72));
            Vec3 top = foot.add(0, 6.0, 0);
            m.line(foot, top, i % 2 == 0 ? .86F : .34F);
            m.line(top, target.add(0, 6.8, 0), .28F);
        }
        m.polygon(f, target.add(0, 3.0, 0), r * .58, 7, t * .015, .48F);
        m.runeRing(g, target, r * .92, 21, .16, 0xF0ACE, -t*.008, .34F);
    }

    private static void prismaticFan(ArcaneWorldMesh.Builder m, Vec3 direction, double p, double t) {
        Vec3 normal = safe(direction);
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(normal);
        double r = 4.2;
        m.arc(f, normal.scale(.8), r, -Math.PI * .42, Math.PI * .84, 56, 1.08F);
        for (int i = 0; i < 7; i++) {
            double q = (i - 3) / 3.0;
            Vec3 root = normal.scale(.65).add(f.right().scale(q * .28));
            Vec3 end = normal.scale(5.0 + i * .16).add(f.right().scale(q * 3.6)).add(f.up().scale((i % 2) * .28));
            m.line(root, end, i == 3 ? 1.18F : .48F);
            m.diamond(f, end, .30, i * .34, 1.10F, .20F);
        }
        m.runeChords(f, normal.scale(.8), r * .88, 14, 5, t*.009, .28F);
    }

    private static void temporalLaw(ArcaneWorldMesh.Builder m, double range, double p, double t) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis x = ArcaneWorldMesh.Basis.facing(new Vec3(1, 0, 0));
        ArcaneWorldMesh.Basis z = ArcaneWorldMesh.Basis.facing(new Vec3(0, 0, 1));
        double r = Math.max(20.0, Math.min(48.0, range * .75));
        m.brokenBand(g, Vec3.ZERO, r * .965, r, 156, 9, 1.22F, .19F);
        m.brokenBand(g, Vec3.ZERO, r * .79, r * .84, 132, 9, .92F, .13F);
        m.polygon(g, Vec3.ZERO, r * .72, 12, 0, .94F);
        denseGrandArray(m, g, Vec3.ZERO, r, 9, 0x710E, t, .16F);
        Vec3 hub = new Vec3(0, Math.min(11.0, r * .22), 0);
        double clock = Math.min(8.0, r * .19);
        m.circle(x, hub, clock, 72, .78F);
        m.circle(z, hub, clock, 72, .48F);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6.0;
            Vec3 base = g.point(a, r * .72);
            m.line(base, base.add(0, Math.min(16.0, r * .33), 0), i % 3 == 0 ? 1.16F : .38F);
        }
        m.line(hub, hub.add(g.point(-Math.PI / 2.0, clock * .86)), 1.34F);
        m.line(hub, hub.add(g.point(1.88, clock * .64)), .88F);
    }

    private static void realityRewrite(ArcaneWorldMesh.Builder m, double p, double t) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        double r = 11.5;
        Vec3[] levels = {Vec3.ZERO, new Vec3(0, 3.2, 0), new Vec3(0, 6.4, 0), new Vec3(0, 9.6, 0)};
        for (int layer = 0; layer < levels.length; layer++) {
            double rr = r * (1.0 - layer * .12);
            m.polygon(g, levels[layer], rr, 9, t * ((layer & 1) == 0 ? .007 : -.006) + layer * .09,
                    layer == 0 ? 1.20F : .58F);
            m.runeRing(g, levels[layer], rr * .88, 18 + layer * 3, .24, 0x5715 + layer * 91, t * .005, .42F);
            if (layer == 0) denseGrandArray(m, g, levels[layer], rr, 9, 0x5715, t, .15F);
        }
        for (int i = 0; i < 9; i++) {
            double a = i * Math.PI * 2.0 / 9.0;
            Vec3 low = g.point(a, r * .72);
            Vec3 high = levels[3].add(g.point(a + .12, r * .45));
            m.line(low, high, i % 3 == 0 ? 1.12F : .40F);
        }
    }

    private static void worldGate(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target, double p, double t) {
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(safe(direction));
        gateFrame(m, f, Vec3.ZERO.add(0, 2.0, 0), 7.2, t);
        gateFrame(m, f, target.add(0, 2.0, 0), 9.0, -t);
        for (int i = 0; i < 9; i++) {
            double a = i * Math.PI * 2.0 / 9.0;
            Vec3 a0 = f.point(a, 6.5).add(0, 2.0, 0);
            Vec3 a1 = target.add(f.point(a + .08, 8.1)).add(0, 2.0, 0);
            m.line(a0, a1, i % 3 == 0 ? .98F : .30F, i % 3 == 0 ? 1.0F : .70F, .36F);
        }
    }

    private static void gateFrame(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis f, Vec3 center, double radius, double t) {
        m.circle(f, center, radius, 88, 1.20F);
        m.polygon(f, center, radius * .82, 9, t * .010, .66F);
        m.runeRing(f, center, radius * .94, 27, .28, center.hashCode(), -t * .008, .44F);
        m.runeRing(f, center, radius * .70, 18, .20, center.hashCode() ^ 0x71, t * .006, .30F);
        m.runeChords(f, center, radius * .58, 18, 7, -t * .004, .25F);
    }

    private static void worldAxis(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target,
                                  double range, double p, double t) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        Vec3 d = horizontal(direction);
        Vec3 right = new Vec3(-d.z, 0, d.x);
        double length = Math.max(42.0, Math.min(104.0, range * 1.06));
        double half = length * .5;
        Vec3 a = target.subtract(d.scale(half));
        Vec3 b = target.add(d.scale(half));
        m.line(a, b, 1.62F);
        m.line(a.add(right.scale(3.6)), b.add(right.scale(-3.6)), .62F);
        m.line(a.add(right.scale(-3.6)), b.add(right.scale(3.6)), .62F);
        for (int i = -6; i <= 6; i++) {
            Vec3 node = target.add(d.scale(i * half / 6.0));
            double rr = 3.4 + (6 - Math.abs(i)) * .62;
            m.brokenBand(g, node, rr * .72, rr, 40, 5, i == 0 ? 1.22F : .54F, .22F);
            m.runeRing(g, node, rr * .86, 9, .15, 0xA115 + i*31, t*.006, .26F);
            m.line(node, node.add(0, 3.0 + (6 - Math.abs(i)) * .55, 0), i == 0 ? 1.24F : .36F);
        }
    }

    private static void regionalFault(ArcaneWorldMesh.Builder m, Vec3 target, double range, double p, double t) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        double r = Math.max(18.0, Math.min(34.0, range * .58));
        m.brokenBand(g, target, r * .82, r, 112, 8, 1.14F, .20F);
        denseGrandArray(m, g, target, r, 8, 0xEA871, t, .13F);
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8.0 + Math.sin(i * 1.7) * .09;
            Vec3 inner = target.add(g.point(a, r * .12));
            Vec3 outer = target.add(g.point(a + .06 * ((i & 1) == 0 ? 1 : -1), r * (.72 + .15 * (i % 3) / 2.0)));
            m.line(inner, outer, i % 4 == 0 ? 1.20F : .46F);
        }
        for (int i = 0; i < 8; i++) {
            Vec3 node = target.add(g.point(i * Math.PI / 4.0, r * .54));
            m.brokenBand(g, node, 2.8, 3.6, 28, 4, .62F, .16F);
        }
    }

    private static void solarJudgment(ArcaneWorldMesh.Builder m, Vec3 target, double p, double t) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        Vec3 sky = target.add(0, 26.0, 0);
        double r = 13.0;
        m.starPlate(g, sky, r, r * .46, 12, t * .008, 1.20F, .10F);
        m.brokenBand(g, sky, r * .78, r, 100, 8, 1.24F, .18F);
        denseGrandArray(m, g, sky, r, 8, 0x501A, t, .13F);
        for (int i = 0; i < 12; i++) {
            Vec3 ray = sky.add(g.point(i * Math.PI / 6.0, r * .58));
            m.line(ray, target.add(g.point(i * Math.PI / 6.0, 4.5)), i % 3 == 0 ? 1.00F : .34F);
        }
    }

    private static void weatherThrone(ArcaneWorldMesh.Builder m, Vec3 target, double range, double p, double t) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        Vec3 sky = target.add(0, 34.0, 0);
        double r = Math.max(15.0, Math.min(25.0, range * .38));
        m.brokenBand(g, sky, r * .82, r, 118, 8, 1.12F, .18F);
        m.polygon(g, sky, r * .68, 8, t * .010, .68F);
        m.runeRing(g, sky, r * .90, 24, .30, 0xC10D, -t * .007, .40F);
        denseGrandArray(m, g, sky, r, 8, 0xC10D, -t, .13F);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0;
            Vec3 cloud = sky.add(g.point(a, r * .76));
            m.helix(cloud, new Vec3(0, -1, 0), ArcaneWorldMesh.Basis.facing(new Vec3(0, -1, 0)),
                    14.0 + (i % 3) * 3.0, .70, 3, 24, i % 2 == 0 ? .72F : .30F, true);
        }
    }

    private static void prismAuthority(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target,
                                       double range, double p, double t) {
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(safe(direction));
        double width = Math.max(18.0, Math.min(38.0, range * .62));
        double height = 14.0;
        Vec3 right = f.right();
        for (int layer = 0; layer < 7; layer++) {
            double x = -width * .5 + width * (layer + .5) / 7.0;
            Vec3 base = target.add(right.scale(x));
            m.line(base, base.add(0, height, 0), layer == 3 ? 1.22F : .48F);
            m.diamond(f, base.add(0, height + 1.1, 0), .56, layer * .33, 1.10F, .22F);
            for (int rune = 0; rune < 5; rune++) {
                Vec3 glyph = base.add(0, 1.6 + rune * (height - 3.2) / 4.0, 0);
                m.runeGlyph(f, glyph, .34, layer * 97 + rune * 13, t*.006 + layer*.11, .30F);
            }
        }
        Vec3 lowA = target.add(right.scale(-width * .5));
        Vec3 lowB = target.add(right.scale(width * .5));
        Vec3 highA = lowA.add(0,height,0);
        Vec3 highB = lowB.add(0,height,0);
        m.line(lowA, lowB, 1.22F);
        m.line(highA, highB, 1.22F);
        m.runeRing(f, target.add(0,height*.5,0), Math.min(8.5, width*.22), 21, .20, 0x7A11, t*.005, .30F);
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
