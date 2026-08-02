package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Set;

final class SignatureGeometry {
    private static final Set<String> PORTALS = Set.of(
            "gate", "dimension_door", "plane_shift", "teleport", "demiplane", "misty_step", "passwall");
    private static final Set<String> WALLS = Set.of("wall_of_force", "prismatic_wall");
    private static final Set<String> DOMES = Set.of(
            "antimagic_field", "globe_of_invulnerability", "resilient_sphere");

    private SignatureGeometry() {}

    static void append(
            SpellDefinition spell,
            Vec3 direction,
            double range,
            List<VoxelShape> shapes,
            int budget) {
        String id = spell.id();
        if ("meteor_swarm".equals(id)) {
            meteor(direction, range, spell.circle(), shapes, budget);
        } else if ("control_weather".equals(id) || "gust_of_wind".equals(id)) {
            tornado(range, spell.circle(), shapes, budget);
        } else if ("forcecage".equals(id)) {
            cage(spell.circle(), shapes, budget);
        } else if (PORTALS.contains(id)) {
            portal(direction, spell.circle(), shapes, budget);
        } else if (WALLS.contains(id)) {
            wall(direction, range, spell.circle(), shapes, budget);
        } else if (DOMES.contains(id)) {
            dome(range, spell.circle(), shapes, budget);
        }
    }

    private static void meteor(
            Vec3 direction,
            double range,
            int circle,
            List<VoxelShape> shapes,
            int budget) {
        Basis ground = Basis.ground();
        Vec3 horizontal = horizontal(direction);
        int count = Math.min(6, 3 + circle / 2);
        double field = Math.min(7.5, 2.8 + range * 0.08 + circle * 0.22);
        for (int i = 0; i < count && shapes.size() < budget; i++) {
            double angle = Math.PI * 2.0 * i / count + 0.35;
            Vec3 impact = ground.point(angle, field * (0.45 + (i % 3) * 0.18));
            Vec3 head = impact.add(horizontal.scale(-2.4 - i * 0.22)).add(0, 5.2 + i * 0.48, 0);
            sphere(head, 0.18 + circle * 0.025, shapes, budget);
            line(head, head.add(horizontal.scale(-1.5)).add(0, 1.8, 0), 10, 0.015, shapes, budget);
            ring(ground, impact, 0.45 + circle * 0.08, 24, 0.014, shapes, budget);
        }
    }

    private static void tornado(
            double range,
            int circle,
            List<VoxelShape> shapes,
            int budget) {
        double height = Math.min(10.5, 4.8 + circle * 0.58 + range * 0.025);
        double top = Math.min(4.5, 1.8 + circle * 0.24 + range * 0.025);
        for (int i = 0; i <= 64 && shapes.size() < budget; i++) {
            double t = i / 64.0;
            double radius = 0.22 + top * Math.pow(t, 1.18);
            double angle = t * Math.PI * (8.0 + circle * 0.35);
            point(
                    new Vec3(Math.cos(angle) * radius, t * height, Math.sin(angle) * radius),
                    0.014,
                    shapes,
                    budget);
        }
        Basis ground = Basis.ground();
        for (int i = 1; i <= 4 && shapes.size() < budget; i++) {
            double t = i / 4.0;
            ring(
                    ground,
                    new Vec3(0, t * height, 0),
                    0.28 + top * Math.pow(t, 1.15),
                    28,
                    0.013,
                    shapes,
                    budget);
        }
    }

    private static void portal(
            Vec3 direction,
            int circle,
            List<VoxelShape> shapes,
            int budget) {
        Basis basis = Basis.facing(direction);
        Vec3 normal = basis.normal();
        double radius = 1.1 + circle * 0.17;
        Vec3 front = normal.scale(-0.24);
        Vec3 back = normal.scale(0.24);
        ring(basis, front, radius, 36, 0.014, shapes, budget);
        ring(basis, back, radius * 0.88, 32, 0.013, shapes, budget);
        int spokes = Math.min(14, 8 + circle);
        for (int i = 0; i < spokes && shapes.size() < budget; i++) {
            double angle = Math.PI * 2.0 * i / spokes;
            line(
                    front.add(basis.point(angle, radius)),
                    back.add(basis.point(angle + 0.12, radius * 0.88)),
                    4,
                    0.012,
                    shapes,
                    budget);
        }
    }

    private static void cage(int circle, List<VoxelShape> shapes, int budget) {
        Basis ground = Basis.ground();
        double radius = 1.15 + circle * 0.11;
        double bottom = -1.0;
        double top = 2.25 + circle * 0.10;
        int bars = Math.min(16, 10 + circle);
        ring(ground, new Vec3(0, bottom, 0), radius, 32, 0.015, shapes, budget);
        ring(ground, new Vec3(0, top, 0), radius, 32, 0.015, shapes, budget);
        for (int i = 0; i < bars && shapes.size() < budget; i++) {
            double angle = Math.PI * 2.0 * i / bars;
            line(
                    ground.point(angle, radius).add(0, bottom, 0),
                    ground.point(angle, radius).add(0, top, 0),
                    12,
                    0.015,
                    shapes,
                    budget);
        }
    }

    private static void wall(
            Vec3 direction,
            double range,
            int circle,
            List<VoxelShape> shapes,
            int budget) {
        Vec3 normal = horizontal(direction);
        Vec3 right = new Vec3(-normal.z, 0, normal.x);
        double half = Math.min(8.0, 2.4 + circle * 0.42 + range * 0.035);
        double height = 2.6 + circle * 0.24;
        int columns = Math.min(12, 7 + circle);
        int rows = Math.min(8, 4 + circle / 2);

        for (int i = 0; i <= columns && shapes.size() < budget; i++) {
            double x = -half + half * 2.0 * i / columns;
            line(
                    wallPoint(right, normal, x, -0.7, half),
                    wallPoint(right, normal, x, height, half),
                    12,
                    0.014,
                    shapes,
                    budget);
        }
        for (int i = 0; i <= rows && shapes.size() < budget; i++) {
            double y = -0.7 + (height + 0.7) * i / rows;
            Vec3 previous = wallPoint(right, normal, -half, y, half);
            for (int step = 1; step <= 18 && shapes.size() < budget; step++) {
                double x = -half + half * 2.0 * step / 18.0;
                Vec3 current = wallPoint(right, normal, x, y, half);
                line(previous, current, 2, 0.013, shapes, budget);
                previous = current;
            }
        }
    }

    private static Vec3 wallPoint(Vec3 right, Vec3 normal, double x, double y, double half) {
        double ratio = x / Math.max(0.01, half);
        return right.scale(x)
                .add(normal.scale(0.22 * (1.0 - ratio * ratio)))
                .add(0, y, 0);
    }

    private static void dome(
            double range,
            int circle,
            List<VoxelShape> shapes,
            int budget) {
        double radius = Math.min(5.8, 1.85 + circle * 0.27 + range * 0.025);
        Vec3 center = new Vec3(0, 0.15, 0);
        Basis ground = Basis.ground();
        Basis verticalX = new Basis(new Vec3(1, 0, 0), new Vec3(0, 1, 0));
        Basis verticalZ = new Basis(new Vec3(0, 0, 1), new Vec3(0, 1, 0));
        ring(ground, center, radius, 32, 0.013, shapes, budget);
        ring(verticalX, center, radius, 32, 0.013, shapes, budget);
        ring(verticalZ, center, radius, 32, 0.013, shapes, budget);
        for (int i = 1; i < 4 && shapes.size() < budget; i++) {
            double y = radius * (-0.62 + i * 1.24 / 4.0);
            double r = Math.sqrt(Math.max(0.05, radius * radius - y * y));
            ring(ground, center.add(0, y, 0), r, 26, 0.012, shapes, budget);
        }
    }

    private static Vec3 horizontal(Vec3 direction) {
        Vec3 result = new Vec3(direction.x, 0, direction.z);
        return result.lengthSqr() < 0.00001 ? new Vec3(0, 0, 1) : result.normalize();
    }

    private static void ring(
            Basis basis,
            Vec3 center,
            double radius,
            int points,
            double size,
            List<VoxelShape> shapes,
            int budget) {
        for (int i = 0; i < points && shapes.size() < budget; i++) {
            point(
                    center.add(basis.point(Math.PI * 2.0 * i / points, radius)),
                    size,
                    shapes,
                    budget);
        }
    }

    private static void sphere(
            Vec3 center,
            double radius,
            List<VoxelShape> shapes,
            int budget) {
        ring(Basis.ground(), center, radius, 18, 0.014, shapes, budget);
        ring(
                new Basis(new Vec3(1, 0, 0), new Vec3(0, 1, 0)),
                center,
                radius,
                18,
                0.014,
                shapes,
                budget);
    }

    private static void line(
            Vec3 start,
            Vec3 end,
            int points,
            double size,
            List<VoxelShape> shapes,
            int budget) {
        int safePoints = Math.max(1, points);
        for (int i = 0; i <= safePoints && shapes.size() < budget; i++) {
            point(start.lerp(end, i / (double) safePoints), size, shapes, budget);
        }
    }

    private static void point(
            Vec3 point,
            double size,
            List<VoxelShape> shapes,
            int budget) {
        if (shapes.size() >= budget) return;
        shapes.add(Shapes.create(new AABB(
                point.x - size,
                point.y - size,
                point.z - size,
                point.x + size,
                point.y + size,
                point.z + size)));
    }

    private record Basis(Vec3 right, Vec3 up) {
        static Basis ground() {
            return new Basis(new Vec3(1, 0, 0), new Vec3(0, 0, 1));
        }

        static Basis facing(Vec3 normal) {
            Vec3 safe = normal.lengthSqr() < 0.00001 ? new Vec3(0, 0, 1) : normal.normalize();
            Vec3 reference = Math.abs(safe.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 right = safe.cross(reference).normalize();
            return new Basis(right, right.cross(safe).normalize());
        }

        Vec3 point(double angle, double radius) {
            return right.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
        }

        Vec3 normal() {
            Vec3 normal = up.cross(right);
            return normal.lengthSqr() < 0.00001 ? new Vec3(0, 1, 0) : normal.normalize();
        }
    }
}
