package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.MeteorBarragePattern;
import kr.moonseungjun.arcanecircle.magic.NinthCircleMagnitude;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Alpha.65 ninth-circle individual prestige layer.
 *
 * IMPORTANT: this class deliberately has no generic high-circle scaffold, no shared grand-array
 * appearance and no default magic-circle fallback. Every rendered spell below owns a separately
 * authored spatial grammar. Seventh/eighth-circle presentation remains in their existing authored
 * timeline/cinematic layers instead of receiving a common ornament pass.
 */
final class HighCirclePrestigeOverlay {
    private static final int CHARGE_BUDGET = 2200;
    private static final int RELEASE_BUDGET = 2400;

    private HighCirclePrestigeOverlay() {}

    static ArcaneWorldMesh charge(SpellDefinition spell, Vec3 direction, Vec3 target, double range,
                                  double progress, long startedAtNanos, long seed) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(CHARGE_BUDGET);
        if (spell == null || spell.circle() != 9) return m.build();
        double p = clamp(progress, 0.0, 1.0);
        double t = Math.max(0.0, (System.nanoTime() - startedAtNanos) / 1_000_000_000.0);
        switch (spell.id()) {
            case "meteor_swarm" -> meteorArtillery(m, target, range, p, t, seed, false);
            case "power_word_kill" -> executionJudgment(m, target, direction, p, t, false);
            case "prismatic_wall" -> sevenLawWall(m, direction, target, range, p, t, false);
            case "shapechange" -> mythicBody(m, direction, p, t, false);
            case "time_stop" -> frozenClockwork(m, range, p, t, false);
            case "true_polymorph" -> morphBlueprint(m, direction, target, p, t, false);
            case "weird" -> nightmareVerdict(m, target, range, p, t, false);
            case "wish" -> realityManuscript(m, direction, p, t, false);
            case "gate" -> pairedWorldDoor(m, direction, target, p, t, false);
            case "foresight" -> causalityFan(m, direction, p, t, false);
            default -> { }
        }
        return m.build();
    }

    static ArcaneWorldMesh release(SpellDefinition spell, Vec3 direction, Vec3 target, double range,
                                   double elapsedSeconds, double durationSeconds, long seed) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(RELEASE_BUDGET);
        if (spell == null || spell.circle() != 9) return m.build();
        double t = Math.max(0.0, elapsedSeconds);
        double life = Math.max(.05, durationSeconds);
        double p = clamp(1.0 - t / life, 0.0, 1.0);
        switch (spell.id()) {
            case "meteor_swarm" -> meteorArtillery(m, target, range, 1.0, t, seed, true);
            case "power_word_kill" -> executionJudgment(m, target, direction, Math.max(.08, p), t, true);
            case "prismatic_wall" -> sevenLawWall(m, direction, target, range, 1.0, t, true);
            case "shapechange" -> mythicBody(m, direction, 1.0, t, true);
            case "time_stop" -> frozenClockwork(m, range, 1.0, t, true);
            case "true_polymorph" -> morphBlueprint(m, direction, target, Math.max(.12, p), t, true);
            case "weird" -> nightmareVerdict(m, target, range, 1.0, t, true);
            case "wish" -> realityManuscript(m, direction, Math.max(.10, p), t, true);
            case "gate" -> pairedWorldDoor(m, direction, target, 1.0, t, true);
            case "foresight" -> causalityFan(m, direction, 1.0, t, true);
            default -> { }
        }
        return m.build();
    }

    /** Meteor: a celestial artillery map — constellation ports and trajectories, not a generic circle. */
    private static void meteorArtillery(ArcaneWorldMesh.Builder m, Vec3 groundOffset, double range,
                                        double p, double t, long seed, boolean release) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        double field = NinthCircleMagnitude.meteorFieldRadius(range);
        double skyY = Math.min(26.0, 12.0 + field * .08);
        Vec3 skyHub = new Vec3(0, skyY, 0);
        Vec3 ground = groundOffset;
        double mapRadius = Math.min(42.0, field * .34);
        List<MeteorBarragePattern.Strike> strikes = MeteorBarragePattern.strikes(seed, range);
        int ordinary = Math.max(1, strikes.size() - 1);
        int visible = release ? ordinary : Math.max(6, (int) Math.ceil(ordinary * (.10 + .90 * p)));
        int stride = Math.max(1, ordinary / Math.max(1, visible));

        // Three offset nine-point constellations identify this as a targeting observatory, not a seal.
        for (int arm = 0; arm < 3; arm++) {
            double rot = arm * Math.PI * 2.0 / 3.0 + (arm == 1 ? .18 : -.08);
            double outer = mapRadius * (1.0 - arm * .12);
            for (int i = 0; i < 9; i++) {
                double a = rot + i * Math.PI * 2.0 / 9.0;
                Vec3 node = skyHub.add(g.point(a, outer));
                Vec3 next = skyHub.add(g.point(rot + (i + 1) * Math.PI * 2.0 / 9.0, outer));
                m.line(node, next, arm == 0 ? .72F : .34F);
                if ((i + arm) % 3 == 0) m.diamond(g, node, .42 + arm * .10, a, 1.06F, .24F);
            }
        }
        m.star(g, skyHub, mapRadius * .58, mapRadius * .16, 9, -t * .004, 1.16F);
        m.runeChords(g, skyHub, mapRadius * .48, 18, 7, t * .003, .30F);

        int shown = 0;
        for (int i = 0; i < ordinary && shown < visible; i += stride, shown++) {
            MeteorBarragePattern.Strike s = strikes.get(i);
            double qx = s.offsetX() / Math.max(1.0, field);
            double qz = s.offsetZ() / Math.max(1.0, field);
            Vec3 port = skyHub.add(qx * mapRadius, 0, qz * mapRadius);
            double node = .28 + s.scale() * .13;
            m.diamond(g, port, node, i * .37, i % 11 == 0 ? 1.26F : .62F, .26F);
            Vec3 projected = ground.add(s.offsetX(), 0, s.offsetZ());
            Vec3 vector = projected.subtract(port);
            double lineFraction = release ? 1.0 : .12 + .70 * p;
            m.line(port, port.add(vector.scale(lineFraction)), i % 11 == 0 ? .82F : .28F);
        }

        // Crown is a distinct descending nine-bladed key at the map center.
        double crownSize = 3.2 + NinthCircleMagnitude.crownScale(range) * .72;
        Vec3 crown = skyHub.add(0, 3.0, 0);
        m.star(g, crown, crownSize, crownSize * .32, 9, t * .009, 1.46F);
        m.polygon(g, crown.add(0, -.42, 0), crownSize * .68, 9, -t * .007, .92F);
        m.line(crown, ground.add(0, release ? .2 : 3.0 + (1.0 - p) * 6.0, 0), 1.58F);
    }

    /** Power Word Kill: compact judicial planes that close like an execution guillotine. */
    private static void executionJudgment(ArcaneWorldMesh.Builder m, Vec3 target, Vec3 direction,
                                          double p, double t, boolean release) {
        Vec3 normal = safe(direction);
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(normal);
        Vec3 mark = target.lengthSqr() > 1.0E-6 ? target : Vec3.ZERO;
        double close = release ? Math.max(.04, 1.0 - Math.min(1.0, t * 2.2)) : 1.0 - p * .52;
        double r = 3.1;
        for (int blade = 0; blade < 9; blade++) {
            double a = blade * Math.PI * 2.0 / 9.0;
            Vec3 outer = mark.add(f.point(a, r * (1.0 + close * .42)));
            Vec3 inner = mark.add(f.point(a + .035, r * (.16 + close * .18)));
            m.line(outer, inner, blade % 3 == 0 ? 1.42F : .56F);
            m.diamond(f, outer, .32, a, 1.18F, .24F);
        }
        for (int plane = -2; plane <= 2; plane++) {
            Vec3 c = mark.add(normal.scale(plane * .32 * close));
            m.polygon(f, c, r * (.58 - Math.abs(plane) * .045), 9,
                    plane * .13, plane == 0 ? 1.32F : .42F);
        }
        m.runeGlyph(f, mark, .72, 0xDEAD09, 0, 1.54F);
        m.line(mark.add(f.up().scale(r * 1.42)), mark.add(f.up().scale(-r * 1.42)), 1.64F);
    }

    /** Prismatic Wall: seven independent vertical laws; no enclosing circle. */
    private static void sevenLawWall(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target,
                                     double range, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(safe(direction));
        Vec3 right = f.right();
        double width = Math.max(18.0, Math.min(38.0, range * .62));
        double height = 14.0;
        for (int law = 0; law < 7; law++) {
            double x0 = -width * .5 + width * law / 7.0;
            double x1 = -width * .5 + width * (law + 1) / 7.0;
            Vec3 a = target.add(right.scale(x0));
            Vec3 b = target.add(right.scale(x1));
            Vec3 at = a.add(0, height, 0);
            Vec3 bt = b.add(0, height, 0);
            m.line(a, at, law == 3 ? 1.38F : .66F);
            m.line(at, bt, law == 3 ? 1.18F : .48F);
            m.line(bt, b, law == 3 ? 1.38F : .66F);
            for (int glyph = 0; glyph < 5; glyph++) {
                double y = 1.5 + glyph * (height - 3.0) / 4.0;
                Vec3 c = target.add(right.scale((x0 + x1) * .5)).add(0, y, 0);
                m.runeGlyph(f, c, .34 + law * .012, law * 131 + glyph * 17, t * .006 + law * .11, .40F);
                if (glyph < 4) m.line(c, c.add(right.scale((law % 2 == 0 ? 1 : -1) * width / 26.0)).add(0, 1.8, 0), .28F);
            }
        }
        // Cross-law braces make a woven wall rather than seven disconnected bars.
        for (int row = 1; row <= 4; row++) {
            double y = row * height / 5.0;
            Vec3 a = target.add(right.scale(-width * .5)).add(0, y, 0);
            Vec3 b = target.add(right.scale(width * .5)).add(0, y + (row % 2 == 0 ? .5 : -.5), 0);
            m.line(a, b, row == 2 ? .82F : .34F);
        }
    }

    /** Shapechange: a mythic anatomy diagram—spine, ribs, horns, wings and four limb chains. */
    private static void mythicBody(ArcaneWorldMesh.Builder m, Vec3 direction, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(safe(direction));
        double grow = .45 + .55 * p;
        Vec3 pelvis = new Vec3(0, .55, 0);
        Vec3 chest = new Vec3(0, 1.75 + .55 * grow, 0);
        Vec3 skull = new Vec3(0, 3.15 + .85 * grow, 0);
        m.line(pelvis, skull, 1.34F);
        for (int rib = 0; rib < 5; rib++) {
            double q = rib / 4.0;
            Vec3 c = pelvis.add(skull.subtract(pelvis).scale(.24 + q * .48));
            double span = (.72 + Math.sin(q * Math.PI) * 1.15) * grow;
            Vec3 l = c.add(f.right().scale(-span));
            Vec3 r = c.add(f.right().scale(span));
            m.line(l, c.add(f.normal().scale(.22)), .46F);
            m.line(c.add(f.normal().scale(.22)), r, .46F);
        }
        for (int side : new int[]{-1, 1}) {
            Vec3 shoulder = chest.add(f.right().scale(side * .65));
            Vec3 wingJoint = shoulder.add(f.right().scale(side * (1.3 + 1.4 * grow))).add(f.up().scale(.65));
            Vec3 wingTip = chest.add(f.right().scale(side * (3.1 + 2.4 * grow))).add(f.up().scale(.10));
            m.line(chest, shoulder, .62F);
            m.line(shoulder, wingJoint, 1.12F);
            m.line(wingJoint, wingTip, 1.28F);
            for (int vane = 1; vane <= 4; vane++) {
                double q = vane / 4.0;
                Vec3 base = wingJoint.add(wingTip.subtract(wingJoint).scale(q));
                Vec3 feather = base.add(f.up().scale(-(.55 + q * 1.45) * grow));
                m.line(base, feather, .36F);
            }
            Vec3 hip = pelvis.add(f.right().scale(side * .42));
            Vec3 knee = hip.add(f.right().scale(side * .48)).add(0, -1.0, 0);
            Vec3 claw = knee.add(f.right().scale(side * .22)).add(f.normal().scale(.35)).add(0, -.95, 0);
            m.line(hip, knee, .82F); m.line(knee, claw, .82F);
            Vec3 hornBase = skull.add(f.right().scale(side * .28));
            Vec3 hornTip = skull.add(f.right().scale(side * .85)).add(0, .95 + .35 * grow, 0).subtract(f.normal().scale(.30));
            m.line(hornBase, hornTip, 1.04F);
        }
        m.polygon(f, skull, .58 + .16 * grow, 6, t * .018, .72F);
    }

    /** Time Stop: frozen clock architecture whose hands cease motion on release. */
    private static void frozenClockwork(ArcaneWorldMesh.Builder m, double range, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis x = ArcaneWorldMesh.Basis.facing(new Vec3(1, 0, 0));
        ArcaneWorldMesh.Basis z = ArcaneWorldMesh.Basis.facing(new Vec3(0, 0, 1));
        double r = Math.max(20.0, Math.min(48.0, range * .75));
        double h = Math.min(12.0, 5.5 + r * .13);
        for (int hour = 0; hour < 12; hour++) {
            double a = hour * Math.PI / 6.0;
            Vec3 foot = g.point(a, r * .92);
            Vec3 top = foot.add(0, h + (hour % 3) * .55, 0);
            m.line(foot, top, hour % 3 == 0 ? 1.24F : .46F);
            m.diamond(g, top, .42, a, 1.10F, .24F);
            if (hour % 2 == 0) m.line(top, new Vec3(0, h * .62, 0), .24F);
        }
        Vec3 hub = new Vec3(0, h * .62, 0);
        double dial = Math.min(7.5, r * .18);
        m.circle(x, hub, dial, 72, 1.04F);
        m.circle(z, hub, dial * .82, 64, .56F);
        for (int mark = 0; mark < 12; mark++) {
            double a = mark * Math.PI / 6.0;
            Vec3 a0 = hub.add(x.point(a, dial * .80));
            Vec3 a1 = hub.add(x.point(a, dial * .98));
            m.line(a0, a1, mark % 3 == 0 ? .92F : .36F);
        }
        double second = release ? -Math.PI / 2.0 : -Math.PI / 2.0 + t * .38;
        double minute = release ? 1.86 : 1.86 + t * .045;
        m.line(hub, hub.add(x.point(second, dial * .84)), 1.52F);
        m.line(hub, hub.add(x.point(minute, dial * .62)), 1.02F);
        // Radius marker is angular, not a rune circle: four frozen cross-axes show actual field size.
        for (int axis = 0; axis < 4; axis++) {
            double a = axis * Math.PI / 2.0;
            Vec3 edge = g.point(a, r);
            m.line(g.point(a, r * .72), edge, axis % 2 == 0 ? .84F : .48F);
            m.line(edge.add(g.point(a + Math.PI / 2.0, -1.2)), edge.add(g.point(a + Math.PI / 2.0, 1.2)), .62F);
        }
    }

    /** True Polymorph: two offset anatomical blueprints connected by rewriting correspondences. */
    private static void morphBlueprint(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target,
                                       double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(safe(direction));
        Vec3 c = target.lengthSqr() > 1.0E-6 ? target : Vec3.ZERO;
        double separation = 2.2 + .8 * p;
        Vec3 oldForm = c.add(f.right().scale(-separation));
        Vec3 newForm = c.add(f.right().scale(separation));
        double oldScale = release ? Math.max(.28, 1.0 - Math.min(1.0, t * 1.6) * .72) : 1.0;
        double newScale = release ? 1.18 : .42 + .58 * p;
        drawHumanoidBlueprint(m, f, oldForm, oldScale, false);
        drawHumanoidBlueprint(m, f, newForm, newScale, true);
        for (int link = -3; link <= 3; link++) {
            double y = link * .48;
            Vec3 a = oldForm.add(0, y, 0);
            Vec3 b = newForm.add(0, y + Math.sin(link * 1.7) * .18, 0);
            m.line(a, b, link == 0 ? 1.08F : .34F);
            m.diamond(f, a.add(b.subtract(a).scale(.5)), .18 + .02 * Math.abs(link), link * .31 + t * .014, .82F, .20F);
        }
        m.runeGlyph(f, c, .58, 0xB10E, t * .012, 1.20F);
    }

    private static void drawHumanoidBlueprint(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis f,
                                              Vec3 c, double s, boolean transformed) {
        Vec3 head = c.add(0, 1.55 * s, 0), chest = c.add(0, .65 * s, 0), hip = c.add(0, -.35 * s, 0);
        m.polygon(f, head, .34 * s, transformed ? 7 : 6, transformed ? .18 : 0, transformed ? 1.04F : .54F);
        m.line(head, hip, transformed ? .92F : .46F);
        double arm = (transformed ? 1.22 : .88) * s;
        m.line(chest.add(f.right().scale(-arm)), chest.add(f.right().scale(arm)), transformed ? .86F : .42F);
        m.line(hip, hip.add(f.right().scale(-.48 * s)).add(0, -.95 * s, 0), transformed ? .82F : .40F);
        m.line(hip, hip.add(f.right().scale(.48 * s)).add(0, -.95 * s, 0), transformed ? .82F : .40F);
        if (transformed) {
            m.line(head.add(f.right().scale(-.16 * s)), head.add(f.right().scale(-.56 * s)).add(0, .62 * s, 0), .64F);
            m.line(head.add(f.right().scale(.16 * s)), head.add(f.right().scale(.56 * s)).add(0, .62 * s, 0), .64F);
        }
    }

    /** Weird: fractured inward-facing nightmare perimeter plus a central verdict eye. */
    private static void nightmareVerdict(ArcaneWorldMesh.Builder m, Vec3 target, double range,
                                         double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(new Vec3(0, 0, 1));
        Vec3 c = target;
        double r = Math.max(18.0, Math.min(34.0, range * .52));
        int fractures = 21;
        for (int i = 0; i < fractures; i++) {
            double a = i * Math.PI * 2.0 / fractures;
            double wobble = 1.0 + .055 * Math.sin(i * 2.17 + t * .14);
            Vec3 outer = c.add(g.point(a, r * wobble));
            Vec3 kink = c.add(g.point(a + ((i & 1) == 0 ? .08 : -.07), r * (.72 + .06 * (i % 3))));
            Vec3 fang = c.add(g.point(a - .03, r * (.50 + .04 * (i % 4)))).add(0, 2.5 + (i % 3) * .65, 0);
            m.line(outer, kink, i % 3 == 0 ? 1.12F : .44F);
            m.line(kink, fang, i % 4 == 0 ? .88F : .34F);
            if (i % 3 == 0) m.shard(fang, new Vec3(0, -1, 0), ArcaneWorldMesh.Basis.facing(new Vec3(0, -1, 0)), 1.0 + .18 * (i % 4), .15, 1.12F, .24F);
        }
        Vec3 eye = c.add(0, 5.5, 0);
        m.polygon(f, eye, 4.2, 6, 0, 1.16F);
        m.arc(f, eye, 3.1, -.82, 1.64, 42, .72F);
        m.arc(f, eye, 3.1, Math.PI - .82, 1.64, 42, .72F);
        m.diamond(f, eye, 1.15, t * .012, 1.42F, .30F);
        double verdict = release ? clamp(t / 15.0, 0.0, 1.0) : p;
        for (int mark = 0; mark < 15; mark++) {
            double a = mark * Math.PI * 2.0 / 15.0;
            Vec3 pos = c.add(g.point(a, r * .34));
            double h = .35 + 3.2 * (mark / 14.0 <= verdict ? 1.0 : .18);
            m.line(pos, pos.add(0, h, 0), mark / 14.0 <= verdict ? .86F : .22F);
        }
    }

    /** Wish: four manuscript planes whose strokes are rewritten into one new reality line. */
    private static void realityManuscript(ArcaneWorldMesh.Builder m, Vec3 direction,
                                          double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(safe(direction));
        double width = 7.5, height = 4.8;
        for (int page = 0; page < 4; page++) {
            double depth = (page - 1.5) * .72;
            Vec3 c = f.normal().scale(depth).add(0, 1.5 + page * .55, 0);
            Vec3 left = c.add(f.right().scale(-width * (1.0 - page * .08) * .5));
            Vec3 right = c.add(f.right().scale(width * (1.0 - page * .08) * .5));
            Vec3 lu = left.add(f.up().scale(height * .5)), ld = left.add(f.up().scale(-height * .5));
            Vec3 ru = right.add(f.up().scale(height * .5)), rd = right.add(f.up().scale(-height * .5));
            m.line(lu, ru, page == 0 ? 1.08F : .42F); m.line(ru, rd, .34F);
            m.line(rd, ld, .42F); m.line(ld, lu, .34F);
            for (int row = 0; row < 5; row++) {
                double y = -.32 + row * .16;
                double erase = release ? clamp(t * .55 - page * .12, 0, 1) : p * .35;
                Vec3 a = c.add(f.right().scale(-width * .34 + erase * width * .18)).add(f.up().scale(y * height));
                Vec3 b = c.add(f.right().scale(width * (.20 + .05 * ((row + page) % 3)))).add(f.up().scale(y * height + .12 * Math.sin(row + page)));
                m.line(a, b, row == 2 ? .72F : .28F);
            }
            m.runeGlyph(f, c.add(f.right().scale(width * .34)), .36, 0x5715 + page * 97, t * .006, .60F);
        }
        Vec3 origin = new Vec3(0, 1.5, 0), rewritten = origin.add(f.up().scale(5.2));
        m.line(origin, rewritten, 1.46F);
        m.star(f, rewritten, 1.35, .42, 9, -t * .008, 1.28F);
    }

    /** Gate: two physical doorway frames and a perspective corridor between them. */
    private static void pairedWorldDoor(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target,
                                        double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(safe(direction));
        Vec3 near = new Vec3(0, 2.25, 0);
        Vec3 far = target.add(0, 2.25, 0);
        double nearW = 3.4, nearH = 4.5;
        double farW = 4.2, farH = 5.2;
        drawDoorFrame(m, f, near, nearW, nearH, t, 0x61A7);
        if (target.lengthSqr() > 8.0) drawDoorFrame(m, f, far, farW, farH, -t, 0xB37E);
        if (target.lengthSqr() > 8.0) {
            int rails = 9;
            for (int i = 0; i < rails; i++) {
                double q = (i - 4) / 4.0;
                Vec3 a = near.add(f.right().scale(q * nearW)).add(f.up().scale((1.0 - Math.abs(q)) * nearH * .82));
                Vec3 b = far.add(f.right().scale(q * farW)).add(f.up().scale((1.0 - Math.abs(q)) * farH * .82));
                m.line(a, b, i == 4 ? .94F : .28F, i == 4 ? 1.0F : .72F, .40F);
            }
            Vec3 floorNearL = near.add(f.right().scale(-nearW)).add(f.up().scale(-nearH));
            Vec3 floorNearR = near.add(f.right().scale(nearW)).add(f.up().scale(-nearH));
            Vec3 floorFarL = far.add(f.right().scale(-farW)).add(f.up().scale(-farH));
            Vec3 floorFarR = far.add(f.right().scale(farW)).add(f.up().scale(-farH));
            m.line(floorNearL, floorFarL, .62F); m.line(floorNearR, floorFarR, .62F);
        }
    }

    private static void drawDoorFrame(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis f, Vec3 c,
                                      double w, double h, double t, int seed) {
        Vec3 l0 = c.add(f.right().scale(-w)).add(f.up().scale(-h));
        Vec3 l1 = c.add(f.right().scale(-w)).add(f.up().scale(h));
        Vec3 r0 = c.add(f.right().scale(w)).add(f.up().scale(-h));
        Vec3 r1 = c.add(f.right().scale(w)).add(f.up().scale(h));
        m.line(l0, l1, 1.34F); m.line(r0, r1, 1.34F); m.line(l1, r1, 1.34F);
        m.line(l0, r0, .56F);
        for (int rung = 1; rung <= 7; rung++) {
            double y = -h + rung * h * 2.0 / 8.0;
            Vec3 l = c.add(f.right().scale(-w)).add(f.up().scale(y));
            Vec3 r = c.add(f.right().scale(w)).add(f.up().scale(y + (rung % 2 == 0 ? .18 : -.18)));
            m.line(l, r, rung == 4 ? .72F : .22F);
        }
        m.runeGlyph(f, c, .72, seed, t * .008, 1.10F);
        m.diamond(f, c.add(f.up().scale(h + .72)), .48, t * .011, 1.12F, .24F);
    }

    /** Foresight: one eye and multiple already-calculated future trajectories. */
    private static void causalityFan(ArcaneWorldMesh.Builder m, Vec3 direction,
                                     double p, double t, boolean release) {
        Vec3 forward = safe(direction);
        ArcaneWorldMesh.Basis f = ArcaneWorldMesh.Basis.facing(forward);
        Vec3 eye = forward.scale(.82).add(0, 1.0, 0);
        m.arc(f, eye, 1.25, -.92, 1.84, 44, 1.10F);
        m.arc(f, eye, 1.25, Math.PI - .92, 1.84, 44, 1.10F);
        m.diamond(f, eye, .40, 0, 1.34F, .28F);
        int futures = 9;
        for (int path = 0; path < futures; path++) {
            double q = (path - 4) / 4.0;
            Vec3 start = eye.add(f.right().scale(q * .22));
            Vec3 mid = forward.scale(4.5 + path * .32).add(f.right().scale(q * (2.0 + p * 2.4))).add(f.up().scale(Math.sin(path * .8) * .45));
            Vec3 end = forward.scale(9.0 + path * .55).add(f.right().scale(q * (4.0 + p * 4.5))).add(f.up().scale(Math.sin(path * 1.7) * .8));
            m.line(start, mid, path == 4 ? 1.08F : .32F);
            m.line(mid, end, path == 4 ? 1.22F : .42F);
            m.diamond(f, end, path == 4 ? .34 : .18, path * .27, path == 4 ? 1.16F : .66F, .20F);
        }
        // Three discarded futures cross out while the central one stays continuous.
        for (int path : new int[]{1, 3, 7}) {
            double q = (path - 4) / 4.0;
            Vec3 c = forward.scale(7.0 + path * .35).add(f.right().scale(q * (3.2 + p * 3.4)));
            m.line(c.add(f.right().scale(-.45)).add(f.up().scale(-.45)), c.add(f.right().scale(.45)).add(f.up().scale(.45)), .52F);
            m.line(c.add(f.right().scale(-.45)).add(f.up().scale(.45)), c.add(f.right().scale(.45)).add(f.up().scale(-.45)), .52F);
        }
    }

    private static Vec3 safe(Vec3 value) {
        return value == null || value.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : value.normalize();
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
