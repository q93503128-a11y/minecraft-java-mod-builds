package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.SpellPresentationProfile;
import net.minecraft.world.phys.Vec3;

/**
 * Secondary notation grammar layered onto authored spell silhouettes.
 * It adds readable construction marks without replacing each spell's bespoke director geometry.
 */
final class ArcaneSigilDetailGrammar {
    private ArcaneSigilDetailGrammar() {}

    static void appendCharge(SpellDefinition spell, SpellPresentationProfile.Profile profile,
                             ArcaneWorldMesh.Basis basis, double outer, double rotation,
                             double progress, ArcaneWorldMesh.Builder mesh) {
        if (mesh.full() || progress < 0.10) return;
        double p = smooth(progress);
        int seed = spell.id().hashCode();
        int complexity = profile.complexity();

        notationKernel(mesh, basis, outer, rotation, p, seed, complexity);
        switch (profile.sigil()) {
            case FRONT_COMPACT -> compact(mesh, basis, outer, rotation, p, seed, complexity);
            case FRONT_LANCE -> lance(mesh, basis, outer, rotation, p, seed, complexity);
            case GROUND_SEAL, FEET_RUNE -> ground(mesh, basis, outer, rotation, p, seed, complexity);
            case TARGET_SEAL -> target(mesh, basis, outer, rotation, p, seed, complexity);
            case BODY_HALO -> halo(mesh, basis, outer, rotation, p, seed, complexity);
            case SKY_RITUAL -> sky(mesh, basis, outer, rotation, p, seed, complexity);
            case QUAD_ARRAY -> quad(mesh, basis, outer, rotation, p, seed, complexity);
            case WALL_MATRIX -> wall(mesh, basis, outer, rotation, p, seed, complexity);
            case PORTAL_GATE -> portal(mesh, basis, outer, rotation, p, seed, complexity);
        }

        if (spell.circle() >= 6 && p > 0.48 && !mesh.full()) {
            ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(
                    basis.normal().add(basis.right().scale(0.72)), basis.up());
            double a = phase(p, 0.48, 0.90);
            mesh.brokenBand(tilt, Vec3.ZERO, outer * 0.42, outer * 0.455,
                    48 + complexity * 4, 7, 0.62F, (float) (0.10 + a * 0.18));
        }
        if (spell.circle() >= 8 && p > 0.66 && !mesh.full()) {
            ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(
                    basis.normal().add(basis.up().scale(0.64)), basis.right());
            double a = phase(p, 0.66, 1.0);
            mesh.runeRing(tilt, Vec3.ZERO, outer * 0.34, 7 + complexity,
                    outer * 0.013, seed ^ 0x51A7, -rotation * 0.61, 0.48F);
            if (a > 0.35) mesh.polygon(tilt, Vec3.ZERO, outer * 0.27,
                    5 + Math.floorMod(seed, 3), rotation * 0.22, 0.48F);
        }
    }

    static void appendRelease(SpellDefinition spell, SpellPresentationProfile.Profile profile,
                              Vec3 direction, double age, double powerFactor,
                              ArcaneWorldMesh.Builder mesh) {
        if (mesh.full() || age > 0.30) return;
        ArcaneWorldMesh.Basis basis = basis(profile, direction);
        double fade = 1.0 - phase(age, 0.0, 0.30);
        double radius = profile.radius() * (0.76 + Math.min(0.45, powerFactor * 0.08));
        double rotation = Math.floorMod(spell.id().hashCode(), 360) * Math.PI / 180.0 + age * 2.2;
        int seed = spell.id().hashCode();

        switch (profile.sigil()) {
            case FRONT_COMPACT, FRONT_LANCE -> {
                mesh.brokenBand(basis, Vec3.ZERO, radius * (0.62 + age),
                        radius * (0.67 + age), 42, 5, 0.62F, (float) (0.20 * fade));
                for (int i = 0; i < 4 && !mesh.full(); i++) {
                    double a = rotation + Math.PI * 0.5 * i;
                    mesh.line(basis.point(a, radius * 0.36), basis.point(a, radius * (0.76 + age)),
                            i == 0 ? 0.72F : 0.42F);
                }
            }
            case GROUND_SEAL, FEET_RUNE, QUAD_ARRAY -> {
                mesh.runeRing(basis, Vec3.ZERO, radius * (0.70 + age * 0.55),
                        6 + profile.complexity(), radius * 0.018, seed ^ 0x2B7,
                        rotation, 0.42F);
            }
            case TARGET_SEAL -> {
                double r = radius * (0.52 + age * 0.70);
                bracket(mesh, basis, Vec3.ZERO, r, 0.42F);
            }
            case BODY_HALO -> {
                ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(
                        basis.right().add(basis.normal().scale(0.8)), basis.up());
                mesh.brokenBand(tilt, Vec3.ZERO, radius * 0.54, radius * 0.59,
                        40, 6, 0.44F, (float) (0.16 * fade));
            }
            case SKY_RITUAL -> {
                mesh.runeRing(basis, Vec3.ZERO, radius * (0.74 + age * 0.42),
                        8 + profile.complexity() * 2, radius * 0.014, seed ^ 0x715A,
                        rotation, 0.44F);
            }
            case WALL_MATRIX -> {
                double r = radius * (0.50 + age * 0.44);
                matrixCorners(mesh, basis, r, 0.44F);
            }
            case PORTAL_GATE -> {
                Vec3 n = basis.normal();
                for (int i = 0; i < 3 && !mesh.full(); i++) {
                    Vec3 center = n.scale(radius * age * (0.25 + i * 0.16));
                    mesh.brokenBand(basis, center, radius * (0.45 + i * 0.12),
                            radius * (0.49 + i * 0.12), 42, 5 + i, 0.44F,
                            (float) (0.16 * fade));
                }
            }
        }
    }

    private static void notationKernel(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                                       double outer, double rotation, double p, int seed, int complexity) {
        double a = phase(p, 0.10, 0.52);
        if (a <= 0.01) return;
        mesh.runeRing(basis, Vec3.ZERO, outer * 0.315,
                5 + Math.max(1, complexity), outer * 0.014,
                seed ^ 0x39A5, -rotation * 0.48, 0.44F);
        if (p > 0.31) {
            int nodes = 4 + Math.min(4, complexity / 2);
            for (int i = 0; i < nodes && !mesh.full(); i++) {
                double angle = rotation * 0.18 + Math.PI * 2.0 * i / nodes;
                Vec3 from = basis.point(angle, outer * 0.38);
                Vec3 to = basis.point(angle + ((i & 1) == 0 ? 0.19 : -0.19), outer * 0.52);
                mesh.line(from, to, i % 3 == 0 ? 0.62F : 0.38F);
            }
        }
    }

    private static void compact(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                                double o, double r, double p, int seed, int c) {
        if (p < 0.36) return;
        mesh.brokenBand(basis, Vec3.ZERO, o * 0.78, o * 0.82,
                46 + c * 3, 5 + Math.floorMod(seed, 3), 0.50F, 0.16F);
        if (p > 0.70) mesh.polygon(basis, Vec3.ZERO, o * 0.61,
                5 + Math.floorMod(seed >>> 3, 3), -r * 0.31, 0.46F);
    }

    private static void lance(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                              double o, double r, double p, int seed, int c) {
        if (p < 0.30) return;
        Vec3 n = basis.normal();
        int gates = 2 + Math.min(2, c / 2);
        for (int i = 0; i < gates && !mesh.full(); i++) {
            Vec3 center = n.scale(o * (-0.12 + i * 0.12));
            double radius = o * (0.56 - i * 0.055);
            mesh.polygon(basis, center, radius, 4 + Math.floorMod(seed + i, 3),
                    r * (i % 2 == 0 ? 0.22 : -0.17), 0.48F);
        }
        if (p > 0.62) {
            mesh.line(basis.right().scale(-o * 0.88), basis.right().scale(o * 0.88), 0.48F);
            mesh.line(basis.up().scale(-o * 0.88), basis.up().scale(o * 0.88), 0.48F);
        }
    }

    private static void ground(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                               double o, double r, double p, int seed, int c) {
        if (p < 0.34) return;
        int nodes = 4 + Math.min(4, c);
        for (int i = 0; i < nodes && !mesh.full(); i++) {
            double a = r * 0.12 + Math.PI * 2.0 * i / nodes;
            Vec3 node = basis.point(a, o * 0.82);
            mesh.diamond(basis, node, o * 0.055, a, 0.82F, 0.18F);
            if (p > 0.58) mesh.line(node, basis.point(a, o * 0.61), 0.44F);
        }
    }

    private static void target(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                               double o, double r, double p, int seed, int c) {
        if (p < 0.28) return;
        bracket(mesh, basis, Vec3.ZERO, o * (0.67 + 0.08 * phase(p, 0.28, 0.78)), 0.56F);
        if (p > 0.58) mesh.runeRing(basis, Vec3.ZERO, o * 0.50,
                6 + c, o * 0.014, seed ^ 0x971, -r, 0.42F);
    }

    private static void halo(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                             double o, double r, double p, int seed, int c) {
        if (p < 0.34) return;
        ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(
                basis.right().add(basis.normal().scale(0.9)), basis.up());
        mesh.runeRing(tilt, Vec3.ZERO, o * 0.61, 6 + c,
                o * 0.014, seed ^ 0x411, r * 0.37, 0.44F);
        if (p > 0.68) mesh.polygon(basis, Vec3.ZERO, o * 0.48, 4 + c / 2, -r * 0.24, 0.42F);
    }

    private static void sky(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                            double o, double r, double p, int seed, int c) {
        if (p < 0.32) return;
        int nodes = Math.max(4, Math.min(10, 3 + c));
        for (int i = 0; i < nodes && !mesh.full(); i++) {
            double a = r * 0.11 + Math.PI * 2.0 * i / nodes;
            Vec3 inner = basis.point(a, o * 0.82);
            Vec3 outer = basis.point(a, o * (0.93 + (i % 2) * 0.05));
            mesh.line(inner, outer, i % 3 == 0 ? 0.68F : 0.40F);
        }
        if (p > 0.62) mesh.runeRing(basis, Vec3.ZERO, o * 0.89,
                10 + c * 2, o * 0.012, seed ^ 0x715A, -r * 0.28, 0.42F);
    }

    private static void quad(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                             double o, double r, double p, int seed, int c) {
        if (p < 0.34) return;
        double d = o * 0.67;
        Vec3[] nodes = {
                basis.right().scale(d).add(basis.up().scale(d)),
                basis.right().scale(-d).add(basis.up().scale(d)),
                basis.right().scale(-d).add(basis.up().scale(-d)),
                basis.right().scale(d).add(basis.up().scale(-d))
        };
        for (int i = 0; i < nodes.length && !mesh.full(); i++) {
            mesh.runeGlyph(basis, nodes[i], o * 0.045, seed + i * 29,
                    r + i * Math.PI / 2.0, 0.44F);
            if (p > 0.60) mesh.line(nodes[i], nodes[(i + 1) % nodes.length], 0.40F);
        }
    }

    private static void wall(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                             double o, double r, double p, int seed, int c) {
        if (p < 0.32) return;
        matrixCorners(mesh, basis, o * 0.70, 0.52F);
        if (p > 0.56) {
            for (int i = -2; i <= 2 && !mesh.full(); i++) {
                double t = i / 2.0;
                mesh.line(basis.right().scale(-o * 0.63).add(basis.up().scale(o * 0.30 * t)),
                        basis.right().scale(o * 0.63).add(basis.up().scale(o * 0.30 * t)), 0.34F);
            }
        }
    }

    private static void portal(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                               double o, double r, double p, int seed, int c) {
        if (p < 0.28) return;
        Vec3 n = basis.normal();
        int depth = 2 + Math.min(3, c / 2);
        for (int i = 0; i < depth && !mesh.full(); i++) {
            Vec3 center = n.scale(o * (0.06 + i * 0.11) * phase(p, 0.28, 0.82));
            double radius = o * (0.66 - i * 0.055);
            mesh.brokenBand(basis, center, radius * 0.94, radius,
                    48 + c * 3, 5 + i, 0.48F, 0.16F);
        }
        if (p > 0.64) mesh.runeRing(basis, Vec3.ZERO, o * 0.56,
                8 + c, o * 0.014, seed ^ 0x6641, r, 0.42F);
    }

    private static void bracket(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis b,
                                Vec3 center, double r, float width) {
        Vec3 x = b.right().scale(r), y = b.up().scale(r);
        double k = 0.38;
        Vec3[] corners = {center.add(x).add(y), center.subtract(x).add(y),
                center.subtract(x).subtract(y), center.add(x).subtract(y)};
        for (int i = 0; i < 4 && !mesh.full(); i++) {
            Vec3 c = corners[i];
            double sx = (i == 0 || i == 3) ? -k : k;
            double sy = (i < 2) ? -k : k;
            mesh.line(c, c.add(b.right().scale(r * sx)), width);
            mesh.line(c, c.add(b.up().scale(r * sy)), width);
        }
    }

    private static void matrixCorners(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis b,
                                      double r, float width) {
        Vec3 x = b.right().scale(r), y = b.up().scale(r * 0.62);
        Vec3[] c = {x.add(y), x.scale(-1).add(y), x.scale(-1).subtract(y), x.subtract(y)};
        for (int i = 0; i < c.length && !mesh.full(); i++) {
            mesh.diamond(b, c[i], r * 0.075, Math.PI / 4.0, 0.78F, 0.16F);
            mesh.line(c[i], c[(i + 1) % c.length], width);
        }
    }

    private static ArcaneWorldMesh.Basis basis(SpellPresentationProfile.Profile profile, Vec3 direction) {
        return switch (profile.sigil()) {
            case SKY_RITUAL, GROUND_SEAL, QUAD_ARRAY, FEET_RUNE, BODY_HALO -> ArcaneWorldMesh.Basis.ground();
            default -> ArcaneWorldMesh.Basis.facing(direction);
        };
    }

    private static double smooth(double x) {
        double p = Math.max(0.0, Math.min(1.0, x));
        return p * p * (3.0 - 2.0 * p);
    }

    private static double phase(double value, double start, double end) {
        if (end <= start) return value >= end ? 1.0 : 0.0;
        return Math.max(0.0, Math.min(1.0, (value - start) / (end - start)));
    }
}
