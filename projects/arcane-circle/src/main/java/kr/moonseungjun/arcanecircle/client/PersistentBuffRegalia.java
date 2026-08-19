package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Long-lived self buffs stop drawing a full casting circle after the release afterglow.
 * Instead they wear compact geometry on the back, shoulders, legs or above the head so
 * first-person sight stays clear while other players can still read the active spell.
 */
final class PersistentBuffRegalia {
    private static final Set<String> MAINTAINED = Set.of(
            "shield", "feather_fall", "light", "mage_armor", "mirror_image", "invisibility",
            "blur", "fly", "haste", "protection_from_energy", "greater_invisibility",
            "resilient_sphere", "stoneskin", "freedom_of_movement", "true_seeing",
            "globe_of_invulnerability", "simulacrum", "fire_shield", "solar_guard",
            "etherealness", "shapechange", "foresight");

    private PersistentBuffRegalia() {}

    static boolean handles(SpellDefinition spell) {
        return spell != null && MAINTAINED.contains(spell.id());
    }

    static ArcaneWorldMesh release(SpellDefinition spell, Vec3 direction,
                                   double elapsedSeconds, double durationSeconds, long seed) {
        ArcaneWorldMesh.Builder b = spell.circle() >= 7
                ? ArcaneWorldMesh.detailBuilder(780)
                : ArcaneWorldMesh.fineBuilder(540);
        Vec3 forward = flat(direction);
        Vec3 right = new Vec3(forward.z, 0.0, -forward.x).normalize();
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 back = forward.scale(-1.0);
        double t = elapsedSeconds;

        switch (spell.id()) {
            case "feather_fall" -> featherWings(b, forward, right, up, back, .78, .72, t, false);
            case "fly" -> featherWings(b, forward, right, up, back, 1.34, 1.22, t, true);
            case "etherealness" -> {
                featherWings(b, forward, right, up, back, 1.52, 1.34, t * .72, true);
                veilShards(b, forward, right, up, back, 6, 1.20, t * .46);
            }
            case "shield" -> armorMantle(b, forward, right, up, back, 2, .46, .84, t);
            case "mage_armor" -> armorMantle(b, forward, right, up, back, 6, .58, 1.08, t);
            case "protection_from_energy" -> armorMantle(b, forward, right, up, back, 8, .64, 1.16, t);
            case "resilient_sphere" -> armorMantle(b, forward, right, up, back, 10, .70, 1.30, t);
            case "globe_of_invulnerability" -> {
                armorMantle(b, forward, right, up, back, 12, .78, 1.42, t * .70);
                crown(b, forward, right, up, back, 6, 1.34, .20, t * .45);
            }
            case "stoneskin" -> stoneCarapace(b, forward, right, up, back, t);
            case "fire_shield" -> flameBlades(b, forward, right, up, back, 6, 1.02, t);
            case "solar_guard" -> {
                flameBlades(b, forward, right, up, back, 8, 1.18, t * .78);
                crown(b, forward, right, up, back, 8, 1.42, .22, t * .55);
            }
            case "haste" -> speedFins(b, forward, right, up, back, t, 1.0);
            case "freedom_of_movement" -> {
                speedFins(b, forward, right, up, back, t * .65, .88);
                trailingRibbons(b, forward, right, up, back, t, 4);
            }
            case "invisibility" -> veilShards(b, forward, right, up, back, 4, .82, t * .45);
            case "greater_invisibility" -> veilShards(b, forward, right, up, back, 8, 1.08, t * .62);
            case "blur" -> ghostOffsets(b, forward, right, up, back, t);
            case "mirror_image" -> mirrorMantle(b, forward, right, up, back, t);
            case "true_seeing" -> sightCrown(b, forward, right, up, back, t, false);
            case "foresight" -> sightCrown(b, forward, right, up, back, t, true);
            case "simulacrum" -> reserveBody(b, forward, right, up, back, t, false);
            case "shapechange" -> shapechangeMantle(b, forward, right, up, back, t);
            case "light" -> shoulderWisp(b, forward, right, up, back, t);
            default -> armorMantle(b, forward, right, up, back, 4, .48, .92, t);
        }

        if (spell.circle() >= 7) {
            int points = spell.circle() == 7 ? 5 : spell.circle() == 8 ? 7 : 9;
            crown(b, forward, right, up, back, points, spell.circle() == 9 ? 1.74 : 1.54,
                    .13 + (spell.circle() - 7) * .025, t * .28);
        }
        return b.build();
    }

    private static void featherWings(ArcaneWorldMesh.Builder b, Vec3 f, Vec3 r, Vec3 u, Vec3 back,
                                     double span, double height, double t, boolean grand) {
        double flap = Math.sin(t * (grand ? 2.0 : 1.45)) * (grand ? .12 : .06);
        int feathers = grand ? 7 : 5;
        Vec3 root = u.scale(grand ? .96 : .84).add(back.scale(grand ? .34 : .26));
        for (int side : new int[]{-1, 1}) {
            Vec3 hinge = root.add(r.scale(side * .18));
            for (int i = 0; i < feathers; i++) {
                double q = i / (double) Math.max(1, feathers - 1);
                double lateral = span * (.45 + q * .55);
                double lift = height * (.46 - q * .33) + flap * (1.0 - q * .35);
                double rear = .24 + q * (grand ? .68 : .48);
                Vec3 base = hinge.add(r.scale(side * (.08 + q * .14)));
                Vec3 mid = root.add(r.scale(side * lateral * .68)).add(u.scale(lift * .70)).add(back.scale(rear * .55));
                Vec3 tip = root.add(r.scale(side * lateral)).add(u.scale(lift)).add(back.scale(rear));
                Vec3 lower = mid.add(u.scale(-.13 - q * .05)).add(r.scale(-side * .05));
                b.triangle(base, mid, lower, grand ? 1.10F : 1.02F, grand ? .30F : .22F);
                b.line(base, tip, grand ? .82F : .62F, 1.18F, .72F);
                b.line(lower, tip, grand ? .54F : .42F, .96F, .48F);
            }
        }
    }

    private static void armorMantle(ArcaneWorldMesh.Builder b, Vec3 f, Vec3 r, Vec3 u, Vec3 back,
                                    int plates, double width, double height, double t) {
        Vec3 anchor = u.scale(.76).add(back.scale(.24));
        ArcaneWorldMesh.Basis face = ArcaneWorldMesh.Basis.facing(f);
        int n = Math.max(2, plates);
        for (int i = 0; i < n; i++) {
            int side = (i & 1) == 0 ? -1 : 1;
            int row = i / 2;
            double y = height - row * .25;
            double x = width * (.62 + (row % 2) * .20);
            double bob = Math.sin(t * 1.7 + i * 1.15) * .028;
            Vec3 at = anchor.add(r.scale(side * x)).add(u.scale(y - .70 + bob)).add(back.scale(.05 + row * .045));
            double radius = Math.max(.13, .22 - row * .012);
            b.diamond(face, at, radius, t * .16 * side, 1.08F, .28F);
            b.line(anchor.add(r.scale(side * .20)).add(u.scale(.15)), at, .40F, .82F, .28F);
        }
    }

    private static void speedFins(ArcaneWorldMesh.Builder b, Vec3 f, Vec3 r, Vec3 u, Vec3 back,
                                  double t, double scale) {
        for (int side : new int[]{-1, 1}) {
            for (int i = 0; i < 3; i++) {
                Vec3 root = r.scale(side * (.18 + i * .035)).add(u.scale(.12 + i * .18));
                Vec3 tip = root.add(back.scale((.50 + i * .20) * scale))
                        .add(r.scale(side * (.11 + i * .04))).add(u.scale(.04 * Math.sin(t * 4.0 + i)));
                Vec3 lower = root.add(back.scale(.16 * scale)).add(u.scale(-.10));
                b.triangle(root, lower, tip, 1.10F, .20F);
                b.line(root, tip, .46F, 1.24F, .68F);
            }
        }
    }

    private static void trailingRibbons(ArcaneWorldMesh.Builder b, Vec3 f, Vec3 r, Vec3 u, Vec3 back,
                                        double t, int count) {
        for (int i = 0; i < count; i++) {
            int side = (i & 1) == 0 ? -1 : 1;
            Vec3 root = r.scale(side * (.18 + (i / 2) * .10)).add(u.scale(.54 - (i / 2) * .34)).add(back.scale(.18));
            Vec3 p1 = root.add(back.scale(.45)).add(r.scale(side * .12));
            Vec3 p2 = root.add(back.scale(.92)).add(r.scale(side * (.20 + .07 * Math.sin(t * 2.5 + i))))
                    .add(u.scale(-.10 + .07 * Math.sin(t * 2.1 + i)));
            b.line(root, p1, .40F, 1.12F, .58F);
            b.line(p1, p2, .34F, .92F, .42F);
        }
    }

    private static void veilShards(ArcaneWorldMesh.Builder b, Vec3 f, Vec3 r, Vec3 u, Vec3 back,
                                   int count, double scale, double t) {
        ArcaneWorldMesh.Basis face = ArcaneWorldMesh.Basis.facing(f);
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2.0 * i / count + t * ((i & 1) == 0 ? .12 : -.09);
            double side = Math.cos(a) * .50 * scale;
            double rear = .24 + (.5 + .5 * Math.sin(a)) * .42 * scale;
            double y = .18 + (i % 4) * .28;
            Vec3 at = r.scale(side).add(back.scale(rear)).add(u.scale(y));
            b.diamond(face, at, .10 + .032 * (i % 3), a * .35, .96F, .12F);
            b.line(at.add(u.scale(-.08)), at.add(u.scale(.12)), .28F, 1.05F, .30F);
        }
    }

    private static void ghostOffsets(ArcaneWorldMesh.Builder b, Vec3 f, Vec3 r, Vec3 u, Vec3 back, double t) {
        for (int side : new int[]{-1, 1}) {
            double sway = .08 * Math.sin(t * 2.8 + side);
            Vec3 shoulder = r.scale(side * (.44 + sway)).add(u.scale(.76)).add(back.scale(.28));
            Vec3 hip = r.scale(side * (.32 + sway)).add(u.scale(.12)).add(back.scale(.42));
            Vec3 head = r.scale(side * (.38 + sway)).add(u.scale(1.18)).add(back.scale(.32));
            b.line(head, shoulder, .44F, 1.02F, .32F);
            b.line(shoulder, hip, .38F, .88F, .24F);
            b.line(shoulder, shoulder.add(r.scale(side * .24)).add(u.scale(-.26)), .32F, .92F, .26F);
        }
    }

    private static void mirrorMantle(ArcaneWorldMesh.Builder b, Vec3 f, Vec3 r, Vec3 u, Vec3 back, double t) {
        ArcaneWorldMesh.Basis face = ArcaneWorldMesh.Basis.facing(f);
        for (int i = -1; i <= 1; i++) {
            Vec3 at = r.scale(i * .58).add(u.scale(.70 + .04 * Math.sin(t * 1.9 + i))).add(back.scale(.42 + Math.abs(i) * .12));
            b.diamond(face, at, .24, t * .18 + i, 1.06F, .18F);
            b.line(at.add(u.scale(-.38)), at.add(u.scale(.32)), .40F, 1.02F, .40F);
        }
    }

    private static void sightCrown(ArcaneWorldMesh.Builder b, Vec3 f, Vec3 r, Vec3 u, Vec3 back,
                                   double t, boolean grand) {
        ArcaneWorldMesh.Basis face = ArcaneWorldMesh.Basis.facing(f);
        Vec3 eye = u.scale(grand ? 1.50 : 1.30).add(back.scale(.36));
        b.diamond(face, eye, grand ? .30 : .22, Math.PI / 4.0, 1.22F, .26F);
        b.line(eye.add(r.scale(-.42)), eye, .44F, 1.10F, .54F);
        b.line(eye, eye.add(r.scale(.42)), .44F, 1.10F, .54F);
        if (grand) {
            for (int i = 0; i < 5; i++) {
                double a = -1.0 + i * .5;
                Vec3 at = eye.add(r.scale(Math.sin(a) * .66)).add(u.scale(.22 + Math.cos(a) * .22)).add(back.scale(.10));
                b.diamond(face, at, .09, t * .13 + i, 1.08F, .22F);
            }
        }
    }

    private static void reserveBody(ArcaneWorldMesh.Builder b, Vec3 f, Vec3 r, Vec3 u, Vec3 back,
                                    double t, boolean clone) {
        ArcaneWorldMesh.Basis face = ArcaneWorldMesh.Basis.facing(f);
        Vec3 core = back.scale(clone ? .80 : .64).add(u.scale(.64));
        b.diamond(face, core.add(u.scale(.35)), clone ? .34 : .28, t * .12, 1.12F, .22F);
        b.line(core.add(u.scale(.18)), core.add(u.scale(-.52)), .56F, 1.06F, .48F);
        for (int side : new int[]{-1, 1}) {
            b.line(core.add(u.scale(.02)), core.add(r.scale(side * .34)).add(u.scale(-.22)), .42F, .94F, .38F);
            b.line(core.add(u.scale(-.48)), core.add(r.scale(side * .25)).add(u.scale(-.82)), .42F, .94F, .38F);
        }
        if (clone) b.polygon(face, core.add(u.scale(.35)), .42, 6, t * .10, .42F);
    }

    private static void shapechangeMantle(ArcaneWorldMesh.Builder b, Vec3 f, Vec3 r, Vec3 u, Vec3 back, double t) {
        Vec3 spine = back.scale(.40).add(u.scale(.44));
        for (int i = 0; i < 6; i++) {
            Vec3 root = spine.add(u.scale(-.22 + i * .24));
            Vec3 tip = root.add(back.scale(.30 + i * .055)).add(u.scale(.10 + i * .025));
            b.triangle(root.add(r.scale(-.12)), root.add(r.scale(.12)), tip, 1.04F, .23F);
            b.line(root, tip, .50F, 1.18F, .60F);
        }
        for (int side : new int[]{-1, 1}) {
            Vec3 hornRoot = spine.add(r.scale(side * .24)).add(u.scale(.82));
            Vec3 hornTip = hornRoot.add(r.scale(side * .54)).add(u.scale(.38)).add(back.scale(.28));
            b.line(hornRoot, hornTip, .76F, 1.24F, .74F);
            Vec3 claw = spine.add(r.scale(side * .34)).add(u.scale(-.12));
            for (int i = 0; i < 3; i++)
                b.line(claw, claw.add(r.scale(side * (.30 + i * .08))).add(u.scale(-.20 - i * .08)).add(back.scale(.16)), .44F, 1.08F, .52F);
        }
        featherWings(b, f, r, u, back, .88, .78, t * .55, false);
    }

    private static void stoneCarapace(ArcaneWorldMesh.Builder b, Vec3 f, Vec3 r, Vec3 u, Vec3 back, double t) {
        ArcaneWorldMesh.Basis face = ArcaneWorldMesh.Basis.facing(f);
        Vec3 center = u.scale(.68).add(back.scale(.30));
        for (int i = 0; i < 7; i++) {
            int side = (i & 1) == 0 ? -1 : 1;
            double row = i / 2.0;
            Vec3 at = center.add(r.scale(side * (.24 + .08 * (i % 3)))).add(u.scale(.52 - row * .23)).add(back.scale(.04 * i));
            b.polygonPlate(face, at, .20 + .025 * (i % 2), 5, t * .04 + i, .82F, .30F);
            b.polygon(face, at, .22 + .025 * (i % 2), 5, t * .04 + i, .46F);
        }
    }

    private static void flameBlades(ArcaneWorldMesh.Builder b, Vec3 f, Vec3 r, Vec3 u, Vec3 back,
                                    int count, double scale, double t) {
        Vec3 base = u.scale(.72).add(back.scale(.30));
        for (int i = 0; i < count; i++) {
            int side = (i & 1) == 0 ? -1 : 1;
            int row = i / 2;
            Vec3 root = base.add(r.scale(side * (.32 + row * .12))).add(u.scale(.30 - row * .18));
            Vec3 inner = root.add(r.scale(-side * .09)).add(u.scale(.22 * scale)).add(back.scale(.08));
            Vec3 tip = root.add(r.scale(side * (.18 + row * .06))).add(u.scale((.60 - row * .07) * scale)).add(back.scale(.16 + row * .08));
            b.triangle(root, inner, tip, 1.20F, .30F);
            b.line(root, tip, .70F, 1.30F, .78F);
        }
    }

    private static void shoulderWisp(ArcaneWorldMesh.Builder b, Vec3 f, Vec3 r, Vec3 u, Vec3 back, double t) {
        ArcaneWorldMesh.Basis face = ArcaneWorldMesh.Basis.facing(f);
        Vec3 at = r.scale(.48).add(u.scale(.88 + .06 * Math.sin(t * 2.1))).add(back.scale(.24));
        b.starPlate(face, at, .17, .07, 5, t * .30, 1.18F, .28F);
        b.line(at, at.add(u.scale(.28)), .34F, 1.22F, .46F);
    }

    private static void crown(ArcaneWorldMesh.Builder b, Vec3 f, Vec3 r, Vec3 u, Vec3 back,
                              int points, double height, double size, double t) {
        ArcaneWorldMesh.Basis face = ArcaneWorldMesh.Basis.facing(f);
        Vec3 center = u.scale(height).add(back.scale(.40));
        double width = Math.max(.11, size);
        for (int i = 0; i < points; i++) {
            double x = (i - (points - 1) / 2.0) * width * .72;
            double arch = 1.0 - Math.min(1.0, Math.abs(x) / Math.max(.01, points * width * .35));
            Vec3 at = center.add(r.scale(x)).add(u.scale(arch * .18 + .02 * Math.sin(t * 2.0 + i)));
            b.diamond(face, at, width * (.46 + (i & 1) * .08), t * .15 + i, 1.12F, .20F);
            if (i > 0) {
                double px = (i - 1 - (points - 1) / 2.0) * width * .72;
                double parch = 1.0 - Math.min(1.0, Math.abs(px) / Math.max(.01, points * width * .35));
                Vec3 prev = center.add(r.scale(px)).add(u.scale(parch * .18));
                b.line(prev, at, .32F, 1.02F, .38F);
            }
        }
    }

    private static Vec3 flat(Vec3 value) {
        Vec3 v = value == null ? Vec3.ZERO : new Vec3(value.x, 0.0, value.z);
        return v.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : v.normalize();
    }
}
