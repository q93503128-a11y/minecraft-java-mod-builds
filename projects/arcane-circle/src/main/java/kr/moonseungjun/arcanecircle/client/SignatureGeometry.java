package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Spell-specific 3D silhouettes layered over the shared circle grammar. */
final class SignatureGeometry {
    private static final Set<String> PORTALS = Set.of(
            "gate", "dimension_door", "plane_shift", "teleport", "demiplane", "misty_step", "passwall");
    private static final Set<String> WALLS = Set.of(
            "wall_of_force", "prismatic_wall", "wall_of_fire", "wall_of_ice", "wind_wall");
    private static final Set<String> DOMES = Set.of(
            "antimagic_field", "globe_of_invulnerability", "resilient_sphere", "solar_guard");
    private static final Set<String> STORMS = Set.of(
            "control_weather", "gust_of_wind", "sleet_storm", "ice_storm", "fire_storm",
            "incendiary_cloud", "winter_domain");

    private SignatureGeometry() {}

    static void append(
            SpellDefinition spell,
            Vec3 direction,
            double range,
            double power,
            ArcaneWorldMesh.Builder mesh) {
        String id = spell.id();
        if ("meteor_swarm".equals(id)) {
            meteor(direction, range, power, spell.circle(), mesh);
        } else if (STORMS.contains(id)) {
            storm(spell, range, power, mesh);
        } else if ("forcecage".equals(id) || "thunder_cage".equals(id) || "astral_prison".equals(id)) {
            cage(spell, range, power, mesh);
        } else if (PORTALS.contains(id)) {
            portal(direction, spell.circle(), range, mesh);
        } else if (WALLS.contains(id)) {
            wall(spell, direction, range, power, mesh);
        } else if (DOMES.contains(id)) {
            dome(spell, range, power, mesh);
        } else if ("earthquake".equals(id) || "world_sunder".equals(id)) {
            faultField(spell, direction, range, power, mesh);
        } else if ("power_word_kill".equals(id) || "finger_of_death".equals(id)) {
            executionSeal(spell, direction, power, mesh);
        } else if ("weird".equals(id) || "phantasmal_killer".equals(id)) {
            nightmare(spell, direction, range, mesh);
        } else if ("sunburst".equals(id) || "flame_strike".equals(id)) {
            celestialPillar(spell, range, power, mesh);
        }
    }

    private static void meteor(
            Vec3 direction,
            double range,
            double power,
            int circle,
            ArcaneWorldMesh.Builder mesh) {
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        Vec3 horizontal = horizontal(direction);
        double rangeScale = scale(range, 18.0, 0.62, 2.8);
        double powerScale = scale(power, 170.0, 0.76, 2.4);
        int count = Math.min(9, 4 + circle / 2);
        double field = (3.4 + circle * 0.42) * rangeScale;
        float width = (float) (0.84 + circle * 0.07);

        for (int i = 0; i < count && !mesh.full(); i++) {
            double angle = Math.PI * 2.0 * i / count + 0.37;
            double lane = field * (0.42 + (i % 4) * 0.14);
            Vec3 impact = ground.point(angle, lane);
            Vec3 head = impact.add(horizontal.scale(-3.2 - i * 0.28)).add(0, 6.5 + i * 0.55, 0);
            Vec3 control = head.lerp(impact, 0.50).add(ground.point(angle + 1.1, 1.1 + i * 0.08));
            List<Vec3> trajectory = quadratic(head, control, impact, 18 + circle * 2);
            mesh.polyline(trajectory, width * 0.62F, false);
            mesh.sphere(head, (0.24 + circle * 0.035) * powerScale, circle, width * 0.68F);
            mesh.circle(ground, impact, (0.58 + circle * 0.10) * powerScale,
                    42 + circle * 3, width * 0.54F);
            mesh.star(ground, impact, (0.92 + circle * 0.13) * powerScale,
                    (0.32 + circle * 0.045) * powerScale, 6, angle, width * 0.40F);
        }
    }

    private static void storm(
            SpellDefinition spell,
            double range,
            double power,
            ArcaneWorldMesh.Builder mesh) {
        int circle = spell.circle();
        double rangeScale = scale(range, Math.max(8.0, spell.range()), 0.75, 2.8);
        double powerScale = scale(power, Math.max(1.0, spell.power()), 0.78, 2.3);
        double height = (4.0 + circle * 0.72) * rangeScale;
        double top = (1.25 + circle * 0.32) * rangeScale * powerScale;
        ArcaneWorldMesh.Basis horizontal = ArcaneWorldMesh.Basis.ground();
        float width = (float) (0.64 + circle * 0.06);
        int strands = Math.min(5, 2 + circle / 2);

        for (int strand = 0; strand < strands && !mesh.full(); strand++) {
            double phase = Math.PI * 2.0 * strand / strands;
            List<Vec3> spiral = new ArrayList<>();
            int segments = 44 + circle * 8;
            for (int i = 0; i <= segments; i++) {
                double t = i / (double) segments;
                double radius = 0.12 + top * Math.pow(t, 1.16);
                double angle = phase + t * Math.PI * (8.0 + circle * 0.85);
                spiral.add(new Vec3(Math.cos(angle) * radius, t * height, Math.sin(angle) * radius));
            }
            mesh.polyline(spiral, width * (strand == 0 ? 0.75F : 0.48F), false);
        }
        for (int i = 1; i <= Math.min(7, 3 + circle / 2) && !mesh.full(); i++) {
            double t = i / (double) Math.min(7, 3 + circle / 2);
            mesh.circle(horizontal, new Vec3(0, t * height, 0),
                    0.16 + top * Math.pow(t, 1.14), 40 + circle * 4, width * 0.42F);
        }
    }

    private static void portal(
            Vec3 direction,
            int circle,
            double range,
            ArcaneWorldMesh.Builder mesh) {
        ArcaneWorldMesh.Basis basis = ArcaneWorldMesh.Basis.facing(direction);
        Vec3 normal = basis.normal();
        double scale = scale(range, 16.0, 0.80, 2.5);
        double radius = (0.92 + circle * 0.20) * scale;
        float width = (float) (0.72 + circle * 0.065);
        int depthRings = Math.min(7, 2 + circle / 2);
        for (int i = 0; i < depthRings && !mesh.full(); i++) {
            double t = depthRings == 1 ? 0.0 : i / (double) (depthRings - 1);
            Vec3 center = normal.scale(-0.38 + t * 0.76);
            mesh.circle(basis, center, radius * (1.0 - Math.abs(t - 0.5) * 0.22),
                    52 + circle * 5, width * (i == 0 || i == depthRings - 1 ? 0.62F : 0.40F));
        }
        mesh.runeChords(basis, Vec3.ZERO, radius * 0.78,
                9 + circle, 3 + circle % 4, circle * 0.27, width * 0.44F);
        int spokes = Math.min(18, 8 + circle);
        for (int i = 0; i < spokes && !mesh.full(); i++) {
            double angle = Math.PI * 2.0 * i / spokes;
            mesh.line(normal.scale(-0.38).add(basis.point(angle, radius)),
                    normal.scale(0.38).add(basis.point(angle + 0.12, radius * 0.90)), width * 0.38F);
        }
    }

    private static void cage(
            SpellDefinition spell,
            double range,
            double power,
            ArcaneWorldMesh.Builder mesh) {
        int circle = spell.circle();
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double radius = (1.0 + circle * 0.14) * scale(range, 15.0, 0.82, 2.4);
        double bottom = -1.0;
        double top = 2.1 + circle * 0.18;
        float width = (float) (0.72 + circle * 0.06);
        int floors = Math.min(8, 3 + circle / 2);
        int bars = Math.min(20, 8 + circle * 2);
        for (int floor = 0; floor < floors && !mesh.full(); floor++) {
            double t = floor / (double) Math.max(1, floors - 1);
            mesh.circle(ground, new Vec3(0, bottom + (top - bottom) * t, 0), radius,
                    46 + circle * 4, width * (floor == 0 || floor == floors - 1 ? 0.64F : 0.38F));
        }
        for (int i = 0; i < bars && !mesh.full(); i++) {
            double angle = Math.PI * 2.0 * i / bars;
            mesh.line(ground.point(angle, radius).add(0, bottom, 0),
                    ground.point(angle + 0.10 * Math.sin(i * 1.7), radius).add(0, top, 0),
                    width * 0.56F);
        }
        mesh.star(ground, new Vec3(0, top, 0), radius * 0.84, radius * 0.38,
                Math.min(9, 4 + circle / 2), 0.0, width * 0.48F);
    }

    private static void wall(
            SpellDefinition spell,
            Vec3 direction,
            double range,
            double power,
            ArcaneWorldMesh.Builder mesh) {
        Vec3 normal = horizontal(direction);
        Vec3 right = new Vec3(-normal.z, 0, normal.x);
        int circle = spell.circle();
        double half = Math.min(16.0, (2.5 + circle * 0.62) * scale(range, 20.0, 0.82, 2.6));
        double height = (2.4 + circle * 0.34) * scale(power, Math.max(1.0, spell.power()), 0.84, 1.8);
        float width = (float) (0.68 + circle * 0.055);
        int columns = Math.min(18, 7 + circle * 2);
        int rows = Math.min(12, 4 + circle);

        for (int i = 0; i <= columns && !mesh.full(); i++) {
            double x = -half + half * 2.0 * i / columns;
            mesh.line(wallPoint(right, normal, x, -0.65, half),
                    wallPoint(right, normal, x, height, half), width * 0.44F);
        }
        for (int i = 0; i <= rows && !mesh.full(); i++) {
            double y = -0.65 + (height + 0.65) * i / rows;
            List<Vec3> curve = new ArrayList<>();
            for (int step = 0; step <= 32; step++) {
                double x = -half + half * 2.0 * step / 32.0;
                curve.add(wallPoint(right, normal, x, y, half));
            }
            mesh.polyline(curve, width * (i == 0 || i == rows ? 0.62F : 0.38F), false);
        }
    }

    private static Vec3 wallPoint(Vec3 right, Vec3 normal, double x, double y, double half) {
        double ratio = x / Math.max(0.01, half);
        return right.scale(x)
                .add(normal.scale(0.30 * (1.0 - ratio * ratio)))
                .add(0, y, 0);
    }

    private static void dome(
            SpellDefinition spell,
            double range,
            double power,
            ArcaneWorldMesh.Builder mesh) {
        int circle = spell.circle();
        double radius = (1.65 + circle * 0.38)
                * scale(range, Math.max(6.0, spell.range()), 0.80, 2.5)
                * scale(power, Math.max(1.0, spell.power()), 0.86, 1.8);
        float width = (float) (0.65 + circle * 0.055);
        Vec3 center = new Vec3(0, 0.12, 0);
        mesh.sphere(center, radius, circle + 4, width * 0.52F);
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        mesh.star(ground, center.add(0, -radius * 0.78, 0), radius * 0.66,
                radius * 0.28, Math.min(9, 5 + circle / 2), circle * 0.19, width * 0.42F);
    }

    private static void faultField(
            SpellDefinition spell,
            Vec3 direction,
            double range,
            double power,
            ArcaneWorldMesh.Builder mesh) {
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        Vec3 forward = horizontal(direction);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        double length = Math.min(42.0, Math.max(8.0, range));
        double spread = (1.4 + spell.circle() * 0.32) * scale(power, Math.max(1.0, spell.power()), 0.8, 2.2);
        float width = (float) (0.72 + spell.circle() * 0.07);
        int faults = Math.min(9, 3 + spell.circle() / 2);
        for (int lane = 0; lane < faults && !mesh.full(); lane++) {
            List<Vec3> crack = new ArrayList<>();
            double offset = (lane - (faults - 1) / 2.0) * spread / Math.max(1.0, faults - 1);
            for (int step = 0; step <= 24; step++) {
                double t = step / 24.0;
                double jitter = Math.sin(step * 2.31 + lane * 1.77) * spread * 0.18;
                crack.add(forward.scale(length * t).add(right.scale(offset + jitter)));
            }
            mesh.polyline(crack, width * (lane == faults / 2 ? 0.72F : 0.42F), false);
        }
        mesh.circle(ground, forward.scale(length), spread * 1.2, 46, width * 0.46F);
    }

    private static void executionSeal(
            SpellDefinition spell,
            Vec3 direction,
            double power,
            ArcaneWorldMesh.Builder mesh) {
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(direction);
        double radius = (1.0 + spell.circle() * 0.17)
                * scale(power, Math.max(1.0, spell.power()), 0.85, 2.0);
        float width = (float) (0.78 + spell.circle() * 0.08);
        Vec3 center = direction.scale(Math.min(8.0, Math.max(3.0, spell.range() * 0.45)));
        mesh.circle(facing, center, radius, 64, width * 0.62F);
        mesh.star(facing, center, radius * 0.92, radius * 0.30, 9,
                Math.PI / 2.0, width * 0.58F);
        mesh.runeChords(facing, center, radius * 0.64, 13, 5,
                spell.id().hashCode() * 0.001, width * 0.42F);
    }

    private static void nightmare(
            SpellDefinition spell,
            Vec3 direction,
            double range,
            ArcaneWorldMesh.Builder mesh) {
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(direction);
        double radius = (0.85 + spell.circle() * 0.18) * scale(range, 12.0, 0.85, 2.2);
        Vec3 center = direction.scale(Math.min(10.0, Math.max(3.0, range * 0.55)));
        float width = (float) (0.62 + spell.circle() * 0.055);
        int eyes = Math.min(7, 2 + spell.circle() / 2);
        for (int i = 0; i < eyes && !mesh.full(); i++) {
            double angle = Math.PI * 2.0 * i / eyes;
            Vec3 eye = center.add(facing.point(angle, radius * 1.20));
            mesh.circle(facing, eye, radius * 0.32, 30, width * 0.46F);
            mesh.line(eye.add(facing.right().scale(-radius * 0.28)),
                    eye.add(facing.right().scale(radius * 0.28)), width * 0.54F);
        }
        mesh.runeChords(facing, center, radius, 11 + spell.circle(), 4,
                0.4, width * 0.42F);
    }

    private static void celestialPillar(
            SpellDefinition spell,
            double range,
            double power,
            ArcaneWorldMesh.Builder mesh) {
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double radius = (1.5 + spell.circle() * 0.34) * scale(range, 18.0, 0.8, 2.6);
        double height = (5.0 + spell.circle() * 0.80) * scale(power, Math.max(1.0, spell.power()), 0.82, 2.0);
        float width = (float) (0.70 + spell.circle() * 0.065);
        int rays = Math.min(18, 7 + spell.circle());
        for (int i = 0; i < rays && !mesh.full(); i++) {
            double angle = Math.PI * 2.0 * i / rays;
            Vec3 base = ground.point(angle, radius * (0.45 + (i % 3) * 0.16));
            mesh.line(base, base.add(0, height, 0), width * (i % 3 == 0 ? 0.62F : 0.36F));
        }
        mesh.circle(ground, Vec3.ZERO, radius, 56, width * 0.52F);
        mesh.star(ground, Vec3.ZERO, radius * 0.86, radius * 0.34,
                Math.min(9, 5 + spell.circle() / 2), 0.0, width * 0.48F);
    }

    private static List<Vec3> quadratic(Vec3 a, Vec3 b, Vec3 c, int segments) {
        List<Vec3> result = new ArrayList<>(segments + 1);
        for (int i = 0; i <= segments; i++) {
            double t = i / (double) Math.max(1, segments);
            double u = 1.0 - t;
            result.add(a.scale(u * u).add(b.scale(2.0 * u * t)).add(c.scale(t * t)));
        }
        return result;
    }

    private static Vec3 horizontal(Vec3 direction) {
        Vec3 result = new Vec3(direction.x, 0, direction.z);
        return result.lengthSqr() < 0.00001 ? new Vec3(0, 0, 1) : result.normalize();
    }

    private static double scale(double value, double base, double min, double max) {
        return Math.max(min, Math.min(max,
                Math.pow(Math.max(0.08, value / Math.max(0.1, base)), 0.30)));
    }
}
