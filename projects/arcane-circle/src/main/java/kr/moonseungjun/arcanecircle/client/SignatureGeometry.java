package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Set;

final class SignatureGeometry {
    private static final Set<String> PORTALS = Set.of(
            "gate", "dimension_door", "plane_shift", "teleport", "demiplane", "misty_step", "passwall");
    private static final Set<String> WALLS = Set.of("wall_of_force", "prismatic_wall");
    private static final Set<String> DOMES = Set.of(
            "antimagic_field", "globe_of_invulnerability", "resilient_sphere");

    private SignatureGeometry() {}

    static VoxelShape build(SpellDefinition spell, Vec3 direction, double range) {
        String id = spell.id();
        if ("meteor_swarm".equals(id)) return meteor(direction, range, spell.circle());
        if ("control_weather".equals(id) || "gust_of_wind".equals(id)) return tornado(range, spell.circle());
        if ("forcecage".equals(id)) return cage(spell.circle());
        if (PORTALS.contains(id)) return portal(direction, spell.circle());
        if (WALLS.contains(id)) return wall(direction, range, spell.circle());
        if (DOMES.contains(id)) return dome(range, spell.circle());
        return Shapes.empty();
    }

    private static VoxelShape meteor(Vec3 direction, double range, int circle) {
        VoxelShape shape = Shapes.empty();
        Basis ground = Basis.ground();
        Vec3 horizontal = horizontal(direction);
        int count = Math.min(7, 3 + circle / 2);
        double field = Math.min(7.5, 2.8 + range * 0.08 + circle * 0.22);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count + 0.35;
            Vec3 impact = ground.point(angle, field * (0.45 + (i % 3) * 0.18));
            Vec3 head = impact.add(horizontal.scale(-2.4 - i * 0.22)).add(0, 5.2 + i * 0.48, 0);
            shape = Shapes.or(shape, sphere(head, 0.18 + circle * 0.025));
            shape = Shapes.or(shape, line(head, head.add(horizontal.scale(-1.5)).add(0, 1.8, 0), 18, 0.014));
            shape = Shapes.or(shape, ring(ground, impact, 0.45 + circle * 0.08, 52, 0.012));
        }
        return shape;
    }

    private static VoxelShape tornado(double range, int circle) {
        VoxelShape shape = Shapes.empty();
        double height = Math.min(10.5, 4.8 + circle * 0.58 + range * 0.025);
        double top = Math.min(4.5, 1.8 + circle * 0.24 + range * 0.025);
        Vec3 previous = null;
        for (int i = 0; i <= 112; i++) {
            double t = i / 112.0;
            double radius = 0.22 + top * Math.pow(t, 1.18);
            double angle = t * Math.PI * (8.0 + circle * 0.35);
            Vec3 point = new Vec3(Math.cos(angle) * radius, t * height, Math.sin(angle) * radius);
            if (previous != null) shape = Shapes.or(shape, line(previous, point, 2, 0.012));
            previous = point;
        }
        Basis ground = Basis.ground();
        for (int i = 1; i <= 5; i++) {
            double t = i / 5.0;
            shape = Shapes.or(shape, ring(ground, new Vec3(0, t * height, 0),
                    0.28 + top * Math.pow(t, 1.15), 56, 0.011));
        }
        return shape;
    }

    private static VoxelShape portal(Vec3 direction, int circle) {
        VoxelShape shape = Shapes.empty();
        Basis basis = Basis.facing(direction);
        Vec3 normal = basis.normal();
        double radius = 1.1 + circle * 0.17;
        Vec3 front = normal.scale(-0.24);
        Vec3 back = normal.scale(0.24);
        shape = Shapes.or(shape, ring(basis, front, radius, 80, 0.012));
        shape = Shapes.or(shape, ring(basis, back, radius * 0.88, 72, 0.011));
        int spokes = 8 + circle;
        for (int i = 0; i < spokes; i++) {
            double angle = Math.PI * 2.0 * i / spokes;
            shape = Shapes.or(shape, line(front.add(basis.point(angle, radius)),
                    back.add(basis.point(angle + 0.12, radius * 0.88)), 7, 0.010));
        }
        return shape;
    }

    private static VoxelShape cage(int circle) {
        VoxelShape shape = Shapes.empty();
        Basis ground = Basis.ground();
        double radius = 1.15 + circle * 0.11;
        double bottom = -1.0;
        double top = 2.25 + circle * 0.10;
        int bars = 10 + circle;
        shape = Shapes.or(shape, ring(ground, new Vec3(0, bottom, 0), radius, 64, 0.013));
        shape = Shapes.or(shape, ring(ground, new Vec3(0, top, 0), radius, 64, 0.013));
        for (int i = 0; i < bars; i++) {
            double angle = Math.PI * 2.0 * i / bars;
            shape = Shapes.or(shape, line(ground.point(angle, radius).add(0, bottom, 0),
                    ground.point(angle, radius).add(0, top, 0), 22, 0.013));
        }
        return shape;
    }

    private static VoxelShape wall(Vec3 direction, double range, int circle) {
        VoxelShape shape = Shapes.empty();
        Vec3 normal = horizontal(direction);
        Vec3 right = new Vec3(-normal.z, 0, normal.x);
        double half = Math.min(8.0, 2.4 + circle * 0.42 + range * 0.035);
        double height = 2.6 + circle * 0.24;
        int columns = 7 + circle;
        int rows = 4 + circle / 2;
        for (int i = 0; i <= columns; i++) {
            double x = -half + half * 2.0 * i / columns;
            shape = Shapes.or(shape, line(wallPoint(right, normal, x, -0.7, half),
                    wallPoint(right, normal, x, height, half), 20, 0.012));
        }
        for (int i = 0; i <= rows; i++) {
            double y = -0.7 + (height + 0.7) * i / rows;
            Vec3 previous = null;
            for (int step = 0; step <= 32; step++) {
                double x = -half + half * 2.0 * step / 32.0;
                Vec3 point = wallPoint(right, normal, x, y, half);
                if (previous != null) shape = Shapes.or(shape, line(previous, point, 2, 0.011));
                previous = point;
            }
        }
        return shape;
    }

    private static Vec3 wallPoint(Vec3 right, Vec3 normal, double x, double y, double half) {
        double ratio = x / Math.max(0.01, half);
        return right.scale(x).add(normal.scale(0.22 * (1.0 - ratio * ratio))).add(0, y, 0);
    }

    private static VoxelShape dome(double range, int circle) {
        VoxelShape shape = Shapes.empty();
        double radius = Math.min(5.8, 1.85 + circle * 0.27 + range * 0.025);
        Vec3 center = new Vec3(0, 0.15, 0);
        Basis ground = Basis.ground();
        Basis verticalX = new Basis(new Vec3(1, 0, 0), new Vec3(0, 1, 0));
        Basis verticalZ = new Basis(new Vec3(0, 0, 1), new Vec3(0, 1, 0));
        shape = Shapes.or(shape, ring(ground, center, radius, 72, 0.011));
        shape = Shapes.or(shape, ring(verticalX, center, radius, 72, 0.011));
        shape = Shapes.or(shape, ring(verticalZ, center, radius, 72, 0.011));
        for (int i = 1; i < 4; i++) {
            double y = radius * (-0.62 + i * 1.24 / 4.0);
            double r = Math.sqrt(Math.max(0.05, radius * radius - y * y));
            shape = Shapes.or(shape, ring(ground, center.add(0, y, 0), r, 56, 0.010));
        }
        return shape;
    }

    private static Vec3 horizontal(Vec3 direction) {
        Vec3 result = new Vec3(direction.x, 0, direction.z);
        return result.lengthSqr() < 0.00001 ? new Vec3(0, 0, 1) : result.normalize();
    }

    private static VoxelShape ring(Basis basis, Vec3 center, double radius, int points, double size) {
        VoxelShape shape = Shapes.empty();
        Vec3 previous = center.add(basis.point(0, radius));
        for (int i = 1; i <= points; i++) {
            Vec3 point = center.add(basis.point(Math.PI * 2.0 * i / points, radius));
            shape = Shapes.or(shape, line(previous, point, 2, size));
            previous = point;
        }
        return shape;
    }

    private static VoxelShape sphere(Vec3 center, double radius) {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.or(shape, ring(Basis.ground(), center, radius, 32, 0.012));
        shape = Shapes.or(shape, ring(new Basis(new Vec3(1, 0, 0), new Vec3(0, 1, 0)), center, radius, 32, 0.012));
        return shape;
    }

    private static VoxelShape line(Vec3 start, Vec3 end, int points, double size) {
        VoxelShape shape = Shapes.empty();
        for (int i = 0; i <= points; i++) {
            Vec3 point = start.lerp(end, i / (double) points);
            shape = Shapes.or(shape, Shapes.create(new AABB(point.x - size, point.y - size, point.z - size,
                    point.x + size, point.y + size, point.z + size)));
        }
        return shape;
    }

    private record Basis(Vec3 right, Vec3 up) {
        static Basis ground() { return new Basis(new Vec3(1, 0, 0), new Vec3(0, 0, 1)); }
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
