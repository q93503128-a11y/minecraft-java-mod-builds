package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.SpellPresentationProfile;
import net.minecraft.world.phys.Vec3;

/**
 * Alpha.62 scale grammar for circles 1-6.
 *
 * The lower half of the spell ladder should not look like the same circle copied at a different
 * radius. 1-2C are hand-sized and immediate, 3-4C clearly occupy combat space, and 5-6C introduce
 * multi-plane/sky/ground authority so the transition into 7C prestige is continuous.
 *
 * This layer is presentation-only. It never changes the authoritative damage/field footprint.
 */
final class CircleScaleEnvelope {
    private CircleScaleEnvelope() {}

    static ArcaneWorldMesh charge(SpellDefinition spell, Vec3 direction, Vec3 target, double range,
                                  double progress, long startedAtNanos) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(720);
        if (spell == null || spell.circle() < 1 || spell.circle() > 6) return m.build();
        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(spell);
        double p = smooth(clamp(progress, 0.0, 1.0));
        double t = Math.max(0.0, (System.nanoTime() - startedAtNanos) / 1_000_000_000.0);
        double r = radius(spell, profile, range) * (.52 + .48 * p);
        Vec3 anchor = anchor(spell, target);
        ArcaneWorldMesh.Basis face = ArcaneWorldMesh.Basis.facing(safe(direction));
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();

        switch (profile.sigil()) {
            case FRONT_COMPACT, FRONT_LANCE -> front(m, face, r, p, t, spell.circle());
            case BODY_HALO -> body(m, face, ground, r, p, t, spell.circle());
            case FEET_RUNE -> ground(m, ground, Vec3.ZERO, r, p, t, spell.circle(), false);
            case GROUND_SEAL, QUAD_ARRAY -> ground(m, ground, anchor, r, p, t, spell.circle(), true);
            case TARGET_SEAL -> target(m, face, anchor, r, p, t, spell.circle());
            case SKY_RITUAL -> sky(m, ground, anchor, r, p, t, spell.circle(), profile.skyHeight());
            case WALL_MATRIX -> wall(m, face, anchor, r, p, t, spell.circle());
            case PORTAL_GATE -> portal(m, face, target, r, p, t, spell.circle());
        }
        return m.build();
    }

    static ArcaneWorldMesh release(SpellDefinition spell, Vec3 direction, Vec3 target, double range,
                                   double elapsedSeconds) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(520);
        if (spell == null || spell.circle() < 1 || spell.circle() > 6) return m.build();
        double life = switch (spell.circle()) {
            case 1 -> .22;
            case 2 -> .30;
            case 3 -> .42;
            case 4 -> .58;
            case 5 -> .82;
            default -> 1.12;
        };
        if (elapsedSeconds < 0.0 || elapsedSeconds > life) return m.build();
        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(spell);
        double q = clamp(elapsedSeconds / life, 0.0, 1.0);
        double fade = 1.0 - q;
        double r = radius(spell, profile, range) * (1.0 + q * (.18 + spell.circle() * .025));
        Vec3 anchor = anchor(spell, target);
        ArcaneWorldMesh.Basis face = ArcaneWorldMesh.Basis.facing(safe(direction));
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();

        switch (profile.sigil()) {
            case FRONT_COMPACT, FRONT_LANCE -> {
                m.circle(face, safe(direction).scale(.18), r * (.58 + .18 * q), 28 + spell.circle() * 4,
                        spell.circle() >= 5 ? .86F : .52F);
                if (spell.circle() >= 4)
                    m.polygon(face, safe(direction).scale(.24), r * .36, spell.circle() + 2,
                            q * .26, .38F);
            }
            case BODY_HALO -> {
                m.brokenBand(ground, Vec3.ZERO, r * .70, r * .82, 42 + spell.circle() * 5,
                        5, .82F, (float)(.18 * fade));
                if (spell.circle() >= 5) m.circle(face, new Vec3(0, .85, 0), r * .56, 34, .42F);
            }
            case FEET_RUNE, GROUND_SEAL, QUAD_ARRAY -> {
                m.brokenBand(ground, anchor, r * .76, r, 48 + spell.circle() * 6,
                        5 + spell.circle() / 2, .90F, (float)(.20 * fade));
                if (spell.circle() >= 5)
                    m.circle(ground, anchor, r * (.52 + .10 * q), 40, .40F);
            }
            case TARGET_SEAL -> {
                m.circle(face, anchor, r * (.72 + .12 * q), 34 + spell.circle() * 4, .84F);
                if (spell.circle() >= 5)
                    m.polygon(face, anchor, r * .52, spell.circle() + 1, -q * .18, .44F);
            }
            case SKY_RITUAL -> {
                double y = skyHeight(spell.circle(), profile.skyHeight());
                Vec3 high = anchor.add(0, y, 0);
                m.brokenBand(ground, high, r * .76, r, 58 + spell.circle() * 7,
                        6, .94F, (float)(.18 * fade));
                int drops = Math.max(3, spell.circle());
                for (int i = 0; i < drops; i++) {
                    double a = i * Math.PI * 2.0 / drops;
                    Vec3 from = high.add(ground.point(a, r * .62));
                    m.line(from, anchor.add(ground.point(a, r * .18)), i % 2 == 0 ? .58F : .26F,
                            1.0F, (float)(.45 * fade));
                }
            }
            case WALL_MATRIX -> {
                double half = Math.max(r, Math.min(12.0, range * .22));
                Vec3 right = face.right();
                m.line(anchor.add(right.scale(-half)), anchor.add(right.scale(half)), .92F);
                if (spell.circle() >= 5)
                    m.line(anchor.add(right.scale(-half)).add(0, 3.6, 0),
                            anchor.add(right.scale(half)).add(0, 3.6, 0), .54F);
            }
            case PORTAL_GATE -> {
                m.circle(face, Vec3.ZERO.add(0, .8, 0), r * .80, 44 + spell.circle() * 4, .86F);
                m.circle(face, target.add(0, .8, 0), r, 48 + spell.circle() * 4, .68F);
            }
        }
        return m.build();
    }

    private static void front(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis face, double r,
                              double p, double t, int circle) {
        double z = .28 + circle * .055;
        Vec3 center = new Vec3(0, 0, z);
        m.circle(face, center, r, 34 + circle * 6, circle <= 2 ? .48F : .72F);
        if (circle >= 3 && p > .30)
            m.polygon(face, center, r * .62, Math.min(8, circle + 2), t * .06, .48F);
        if (circle >= 5 && p > .56) {
            m.circle(face, safeNormal(face).scale(.18).add(center), r * .46, 36, .38F);
            m.circle(face, safeNormal(face).scale(.36).add(center), r * .28, 30, .30F);
        }
    }

    private static void body(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis face,
                             ArcaneWorldMesh.Basis ground, double r, double p, double t, int circle) {
        m.brokenBand(ground, Vec3.ZERO, r * .70, r * .82, 40 + circle * 5,
                5, .80F, .16F);
        m.circle(face, new Vec3(0, .95, 0), r * .54, 32 + circle * 4, .62F);
        if (circle >= 5 && p > .48) {
            m.polygon(ground, new Vec3(0, .08, 0), r * .58, circle + 1, t * .035, .48F);
            m.line(new Vec3(0, .10, 0), new Vec3(0, 1.9 + .18 * circle, 0), .42F);
        }
    }

    private static void ground(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis g, Vec3 center,
                               double r, double p, double t, int circle, boolean field) {
        m.brokenBand(g, center, r * .78, r, 42 + circle * 7, 5 + circle / 2,
                circle >= 5 ? 1.0F : .66F, .18F);
        int spokes = Math.max(3, circle + 1);
        for (int i = 0; i < spokes; i++) {
            double a = i * Math.PI * 2.0 / spokes + t * .018;
            m.line(center.add(g.point(a, r * .18)), center.add(g.point(a, r * .74)),
                    i % 3 == 0 ? .68F : .28F);
        }
        if (circle >= 5 && p > .50) {
            double high = field ? 1.25 + .28 * circle : .75 + .18 * circle;
            for (int i = 0; i < Math.min(8, circle + 1); i++) {
                double a = i * Math.PI * 2.0 / Math.min(8, circle + 1);
                Vec3 foot = center.add(g.point(a, r * .68));
                m.line(foot, foot.add(0, high + .20 * (i % 3), 0), i % 2 == 0 ? .52F : .24F);
            }
        }
    }

    private static void target(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis f, Vec3 center,
                               double r, double p, double t, int circle) {
        m.circle(f, center, r, 38 + circle * 5, circle >= 5 ? .88F : .62F);
        m.polygon(f, center, r * .62, Math.min(8, circle + 2), t * .045, .48F);
        if (circle >= 5 && p > .45) {
            Vec3 normal = safeNormal(f);
            int planes = circle == 5 ? 3 : 5;
            for (int i = 0; i < planes; i++) {
                double depth = (i - (planes - 1) / 2.0) * .14;
                m.circle(f, center.add(normal.scale(depth)), r * (.48 - i * .035), 30, .30F);
            }
        }
    }

    private static void sky(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis g, Vec3 center,
                            double r, double p, double t, int circle, double authoredHeight) {
        double height = skyHeight(circle, authoredHeight);
        Vec3 high = center.add(0, height, 0);
        m.brokenBand(g, high, r * .78, r, 56 + circle * 8, 6,
                circle >= 5 ? 1.02F : .72F, .18F);
        m.polygon(g, high, r * .63, Math.min(10, circle + 3), t * .024, .46F);
        int shafts = Math.max(3, circle);
        for (int i = 0; i < shafts; i++) {
            double a = i * Math.PI * 2.0 / shafts;
            Vec3 from = high.add(g.point(a, r * .65));
            m.line(from, from.add(0, -(1.6 + circle * .55) * p, 0), i % 2 == 0 ? .56F : .24F);
        }
    }

    private static void wall(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis f, Vec3 center,
                             double r, double p, double t, int circle) {
        Vec3 right = f.right();
        double half = r * (1.2 + circle * .04);
        double height = 1.9 + circle * .38;
        m.line(center.add(right.scale(-half)), center.add(right.scale(half)), .86F);
        if (p > .36) {
            m.line(center.add(right.scale(-half)).add(0, height, 0),
                    center.add(right.scale(half)).add(0, height, 0), .54F);
            int columns = Math.max(3, circle + 1);
            for (int i = 0; i < columns; i++) {
                double x = -half + 2.0 * half * i / Math.max(1, columns - 1);
                Vec3 foot = center.add(right.scale(x));
                m.line(foot, foot.add(0, height, 0), i % 3 == 0 ? .56F : .22F);
            }
        }
    }

    private static void portal(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis f, Vec3 target,
                               double r, double p, double t, int circle) {
        Vec3 first = new Vec3(0, .85, 0);
        Vec3 second = target.add(0, .85, 0);
        m.circle(f, first, r * .72, 42 + circle * 5, .76F);
        if (p > .34) m.circle(f, second, r, 46 + circle * 5, circle >= 5 ? .88F : .58F);
        if (circle >= 5 && p > .60) {
            int links = circle + 1;
            for (int i = 0; i < links; i++) {
                double a = i * Math.PI * 2.0 / links;
                m.line(first.add(f.point(a, r * .60)), second.add(f.point(a + .08, r * .82)),
                        i % 2 == 0 ? .44F : .20F, 1.0F, .42F);
            }
        }
    }

    private static double radius(SpellDefinition spell, SpellPresentationProfile.Profile profile, double range) {
        int c = Math.max(1, Math.min(6, spell.circle()));
        double tier = switch (c) {
            case 1 -> .62;
            case 2 -> .86;
            case 3 -> 1.16;
            case 4 -> 1.55;
            case 5 -> 2.15;
            default -> 2.95;
        };
        double footprint = switch (profile.sigil()) {
            case FRONT_COMPACT, FRONT_LANCE -> .78;
            case TARGET_SEAL -> .84;
            case BODY_HALO, FEET_RUNE -> .92;
            case PORTAL_GATE -> 1.20;
            case GROUND_SEAL -> 1.34;
            case QUAD_ARRAY, WALL_MATRIX -> 1.50;
            case SKY_RITUAL -> 1.78;
        };
        double authored = Math.min(3.2, Math.max(.0, profile.radius() * .15));
        double rangeLift = switch (profile.sigil()) {
            case SKY_RITUAL, GROUND_SEAL, QUAD_ARRAY, WALL_MATRIX -> Math.min(2.6, Math.max(0.0, range - 12.0) * .018);
            default -> 0.0;
        };
        return (tier + authored + rangeLift) * footprint;
    }

    private static double skyHeight(int circle, double authored) {
        return Math.max(authored, switch (circle) {
            case 1, 2 -> 5.0;
            case 3 -> 7.0;
            case 4 -> 10.0;
            case 5 -> 14.0;
            default -> 19.0;
        });
    }

    private static Vec3 anchor(SpellDefinition spell, Vec3 target) {
        return switch (spell.sigilAnchor()) {
            case TARGET, GROUND_TARGET -> target;
            default -> Vec3.ZERO;
        };
    }

    private static Vec3 safe(Vec3 direction) {
        return direction == null || direction.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : direction.normalize();
    }

    /** Basis has right/up accessors in every supported client revision; cross product gives its normal. */
    private static Vec3 safeNormal(ArcaneWorldMesh.Basis basis) {
        Vec3 normal = basis.right().cross(basis.up());
        return normal.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : normal.normalize();
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double smooth(double value) {
        double t = clamp(value, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }
}
