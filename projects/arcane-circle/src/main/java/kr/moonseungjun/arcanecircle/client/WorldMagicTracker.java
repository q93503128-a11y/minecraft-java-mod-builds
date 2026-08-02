package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.network.WorldMagicPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WorldMagicTracker {
    private static final ContextKey<List<RenderEntry>> DATA_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "world_magic_geometry"));
    private static final Map<UUID, Visual> CHARGES = new HashMap<>();
    private static final List<Visual> RELEASES = new ArrayList<>();

    private WorldMagicTracker() {}

    public static void accept(WorldMagicPayload payload) {
        Map<String, String> values = parse(payload.state());
        String kind = values.getOrDefault("kind", "");
        UUID caster;
        try { caster = UUID.fromString(values.getOrDefault("caster", "")); }
        catch (IllegalArgumentException ignored) { return; }
        if ("stop".equals(kind)) {
            CHARGES.remove(caster);
            return;
        }
        SpellDefinition spell = SpellCatalog.spell(values.getOrDefault("spell", "")).orElse(null);
        if (spell == null) return;
        boolean fusion = integer(values, "fusion", 0) != 0;
        int ingredients = Math.max(0, integer(values, "ingredients", 0));
        Vec3 center = new Vec3(decimal(values, "x", 0), decimal(values, "y", 0), decimal(values, "z", 0));
        Vec3 direction = new Vec3(decimal(values, "dx", 0), decimal(values, "dy", 0), decimal(values, "dz", 1));
        if (direction.lengthSqr() < 0.00001) direction = new Vec3(0, 0, 1);
        direction = direction.normalize();
        double range = Math.max(0.1, decimal(values, "range", spell.range()));
        double power = Math.max(0.1, decimal(values, "power", spell.power()));
        double progress = Math.max(0.0, Math.min(1.0, decimal(values, "progress", 1.0)));
        int duration = Math.max(2, integer(values, "duration", 10));
        long now = System.nanoTime();
        if ("charge".equals(kind)) {
            VoxelShape geometry = buildCharge(spell, fusion, ingredients, direction, progress, range);
            CHARGES.put(caster, new Visual(caster, center, geometry, color(spell), now + 550_000_000L));
        } else if ("release".equals(kind)) {
            VoxelShape geometry = buildRelease(spell, fusion, ingredients, direction, range, power);
            RELEASES.add(new Visual(caster, center, geometry, color(spell),
                    now + duration * 50_000_000L));
            CHARGES.remove(caster);
        }
    }

    public static void onExtract(ExtractLevelRenderStateEvent event) {
        long now = System.nanoTime();
        CHARGES.values().removeIf(visual -> visual.expiresAt < now);
        RELEASES.removeIf(visual -> visual.expiresAt < now);
        if (CHARGES.isEmpty() && RELEASES.isEmpty()) return;
        List<RenderEntry> entries = new ArrayList<>();
        for (Visual visual : CHARGES.values()) entries.add(visual.renderEntry());
        for (Visual visual : RELEASES) entries.add(visual.renderEntry());
        event.getRenderState().setRenderData(DATA_KEY, List.copyOf(entries));
    }

    public static void onSubmit(SubmitCustomGeometryEvent event) {
        List<RenderEntry> entries = event.getLevelRenderState().getRenderData(DATA_KEY);
        if (entries == null || entries.isEmpty()) return;
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        float baseWidth = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
        for (RenderEntry entry : entries) {
            Vec3 offset = entry.center.subtract(camera);
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(offset.x, offset.y, offset.z);
            event.getSubmitNodeCollector().submitShapeOutline(event.getPoseStack(), entry.geometry,
                    RenderTypes.lines(), entry.argb, Math.max(0.85F, baseWidth * 0.90F), false);
            event.getPoseStack().popPose();
        }
    }

    private static VoxelShape buildCharge(SpellDefinition spell, boolean fusion, int ingredients,
                                          Vec3 normal, double progress, double range) {
        Basis basis = basis(spell, normal);
        VoxelShape shape = Shapes.empty();
        double outer = 0.42 + spell.circle() * 0.095 + Math.min(0.32, range * 0.008) + (fusion ? 0.18 : 0.0);
        int ringPoints = 48 + spell.circle() * 5;
        // The primary concentric count is exactly the spell circle: 1C=1, 2C=2 ... 9C=9.
        for (int ring = 0; ring < spell.circle(); ring++) {
            double radius = outer * (1.0 - ring * 0.66 / Math.max(1.0, spell.circle()));
            double localProgress = Math.max(0.0, Math.min(1.0, progress * spell.circle() - ring));
            shape = Shapes.or(shape, partialCircle(basis, radius, ringPoints, localProgress, 0.011));
        }
        if (progress >= 0.16) {
            int sides = 3 + Math.floorMod(spell.id().hashCode(), 6);
            shape = Shapes.or(shape, polygon(basis, outer * 0.56, sides,
                    Math.max(0.0, Math.min(1.0, (progress - 0.16) / 0.46)), 0.019));
        }
        if (progress >= 0.36) {
            int spokes = Math.min(13, 3 + spell.circle() + Math.floorMod(spell.id().hashCode(), 3));
            int shown = (int) Math.floor(spokes * Math.min(1.0, (progress - 0.36) / 0.42));
            for (int i = 0; i < shown; i++) {
                double angle = Math.PI * 2.0 * i / spokes;
                shape = Shapes.or(shape, segment(basis.point(angle, outer * 0.28),
                        basis.point(angle, outer * 0.92), 8, 0.017));
            }
        }
        if (spell.circle() >= 3 && progress >= 0.60) {
            int satellites = Math.min(6, spell.circle() - 1);
            for (int i = 0; i < satellites; i++) {
                double angle = Math.PI * 2.0 * i / satellites;
                Vec3 satelliteCenter = basis.point(angle, outer * 1.22);
                shape = Shapes.or(shape, circleAround(basis, satelliteCenter, outer * 0.16,
                        20 + spell.circle(), 0.016));
                shape = Shapes.or(shape, segment(basis.point(angle, outer), satelliteCenter, 5, 0.015));
            }
        }
        if (fusion && ingredients >= 2 && progress >= 0.42) {
            for (int i = 0; i < ingredients; i++) {
                double angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / ingredients;
                Vec3 satelliteCenter = basis.point(angle, outer * 1.48);
                shape = Shapes.or(shape, circleAround(basis, satelliteCenter, outer * 0.25,
                        28, 0.022));
                shape = Shapes.or(shape, polygonAround(basis, satelliteCenter, outer * 0.17,
                        3 + i, 0.019));
                shape = Shapes.or(shape, segment(Vec3.ZERO, satelliteCenter, 9, 0.017));
            }
        }
        return shape;
    }

    private static VoxelShape buildRelease(SpellDefinition spell, boolean fusion, int ingredients,
                                           Vec3 direction, double range, double power) {
        Basis basis = basis(spell, direction);
        VoxelShape shape = buildCharge(spell, fusion, ingredients, direction, 1.0, range);
        double scale = 0.65 + spell.circle() * 0.13 + Math.min(0.8, power * 0.015);
        switch (spell.sigilAnchor()) {
            case FRONT -> {
                double length = Math.min(34.0, Math.max(3.0, range));
                Vec3 end = direction.scale(length);
                shape = Shapes.or(shape, segment(Vec3.ZERO, end, Math.max(20, (int) (length * 3)),
                        0.035 + spell.circle() * 0.008));
                if (spell.circle() >= 4) {
                    Vec3 side = basis.right.scale(0.14 + spell.circle() * 0.025);
                    shape = Shapes.or(shape, segment(side, end.add(side), 30, 0.024));
                    shape = Shapes.or(shape, segment(side.scale(-1), end.add(side.scale(-1)), 30, 0.024));
                }
                shape = Shapes.or(shape, sphereLattice(end, 0.28 + spell.circle() * 0.09, spell.circle()));
            }
            case FEET, GROUND_SELF, GROUND_TARGET -> {
                Basis ground = Basis.ground();
                double radius = Math.min(17.0, Math.max(1.8, range * 0.24 + spell.circle() * 0.42));
                int rings = Math.max(2, Math.min(9, spell.circle()));
                for (int i = 1; i <= rings; i++) {
                    shape = Shapes.or(shape, circle(ground, radius * i / rings, 48 + i * 4, 0.035));
                }
                int pillars = 4 + spell.circle();
                for (int i = 0; i < pillars; i++) {
                    double angle = Math.PI * 2.0 * i / pillars;
                    Vec3 base = ground.point(angle, radius * 0.82);
                    shape = Shapes.or(shape, segment(base, base.add(0, 1.2 + spell.circle() * 0.32, 0),
                            10 + spell.circle(), 0.04));
                }
            }
            case BODY -> {
                double radius = 1.15 + spell.circle() * 0.22;
                shape = Shapes.or(shape, sphereLattice(new Vec3(0, -0.75, 0), radius, spell.circle() + 2));
            }
            case TARGET -> {
                double radius = 0.75 + spell.circle() * 0.15;
                Basis ground = Basis.ground();
                for (int level = 0; level < 3 + spell.circle() / 2; level++) {
                    Vec3 offset = new Vec3(0, -0.8 + level * 0.48, 0);
                    shape = Shapes.or(shape, circleAround(ground, offset, radius, 36, 0.028));
                }
                int bars = 5 + spell.circle();
                for (int i = 0; i < bars; i++) {
                    double angle = Math.PI * 2.0 * i / bars;
                    Vec3 low = ground.point(angle, radius).add(0, -0.8, 0);
                    shape = Shapes.or(shape, segment(low, low.add(0, 1.9 + spell.circle() * 0.08, 0),
                            12, 0.03));
                }
            }
        }
        if (spell.school() == SpellDefinition.School.SPACE) {
            Basis portal = basis(spell, direction);
            double portalRadius = 0.9 + spell.circle() * 0.16;
            shape = Shapes.or(shape, circle(portal, portalRadius, 72, 0.035));
            shape = Shapes.or(shape, circleAround(portal, direction.scale(0.28), portalRadius * 0.82, 60, 0.03));
        }
        shape = Shapes.or(shape, SignatureGeometry.build(spell, direction, range));
        return shape;
    }

    private static Basis basis(SpellDefinition spell, Vec3 normal) {
        return switch (spell.sigilAnchor()) {
            case FEET, GROUND_SELF, GROUND_TARGET -> Basis.ground();
            default -> Basis.facing(normal);
        };
    }

    private static VoxelShape sphereLattice(Vec3 center, double radius, int detail) {
        VoxelShape shape = Shapes.empty();
        Basis ground = Basis.ground();
        shape = Shapes.or(shape, circleAround(ground, center, radius, 48, 0.032));
        Basis verticalX = new Basis(new Vec3(1, 0, 0), new Vec3(0, 1, 0));
        Basis verticalZ = new Basis(new Vec3(0, 0, 1), new Vec3(0, 1, 0));
        shape = Shapes.or(shape, circleAround(verticalX, center, radius, 48, 0.032));
        shape = Shapes.or(shape, circleAround(verticalZ, center, radius, 48, 0.032));
        for (int i = 1; i < Math.min(5, detail); i++) {
            double y = radius * (-0.65 + i * 1.3 / Math.min(5, detail));
            double r = Math.sqrt(Math.max(0.05, radius * radius - y * y));
            shape = Shapes.or(shape, circleAround(ground, center.add(0, y, 0), r, 36, 0.022));
        }
        return shape;
    }

    private static VoxelShape partialCircle(Basis basis, double radius, int points, double progress, double size) {
        VoxelShape shape = Shapes.empty();
        int shown = Math.max(0, Math.min(points, (int) Math.ceil(points * progress)));
        for (int i = 0; i < shown; i++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / points;
            shape = Shapes.or(shape, pointBox(basis.point(angle, radius), size));
        }
        return shape;
    }

    private static VoxelShape circle(Basis basis, double radius, int points, double size) {
        return partialCircle(basis, radius, points, 1.0, size);
    }

    private static VoxelShape circleAround(Basis basis, Vec3 center, double radius, int points, double size) {
        VoxelShape shape = Shapes.empty();
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            shape = Shapes.or(shape, pointBox(center.add(basis.point(angle, radius)), size));
        }
        return shape;
    }

    private static VoxelShape polygon(Basis basis, double radius, int sides, double progress, double size) {
        return polygonAround(basis, Vec3.ZERO, radius, sides, size, progress);
    }

    private static VoxelShape polygonAround(Basis basis, Vec3 center, double radius, int sides, double size) {
        return polygonAround(basis, center, radius, sides, size, 1.0);
    }

    private static VoxelShape polygonAround(Basis basis, Vec3 center, double radius, int sides,
                                            double size, double progress) {
        VoxelShape shape = Shapes.empty();
        int shownEdges = Math.max(0, Math.min(sides, (int) Math.ceil(sides * progress)));
        List<Vec3> vertices = new ArrayList<>();
        for (int i = 0; i < sides; i++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / sides;
            vertices.add(center.add(basis.point(angle, radius)));
        }
        for (int i = 0; i < shownEdges; i++) {
            shape = Shapes.or(shape, segment(vertices.get(i), vertices.get((i + 1) % sides), 10, size));
        }
        return shape;
    }

    private static VoxelShape segment(Vec3 start, Vec3 end, int points, double size) {
        VoxelShape shape = Shapes.empty();
        for (int i = 0; i <= points; i++) {
            shape = Shapes.or(shape, pointBox(start.lerp(end, i / (double) points), size));
        }
        return shape;
    }

    private static VoxelShape pointBox(Vec3 point, double half) {
        return Shapes.create(new AABB(point.x - half, point.y - half, point.z - half,
                point.x + half, point.y + half, point.z + half));
    }

    private static int color(SpellDefinition spell) {
        return switch (spell.school()) {
            case FIRE -> 0xD8FF7048;
            case FROST -> 0xD86DE4FF;
            case WIND -> 0xD876E6BD;
            case WARD -> 0xD8C595FF;
            case LIFE -> 0xD873E38E;
            case SPACE -> 0xD8A382FF;
            default -> 0xD882A8FF;
        };
    }

    private static Map<String, String> parse(String state) {
        Map<String, String> values = new HashMap<>();
        for (String part : state.split(";")) {
            int index = part.indexOf('=');
            if (index > 0) values.put(part.substring(0, index), part.substring(index + 1));
        }
        return values;
    }

    private static int integer(Map<String, String> values, String key, int fallback) {
        try { return Integer.parseInt(values.getOrDefault(key, Integer.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static double decimal(Map<String, String> values, String key, double fallback) {
        try { return Double.parseDouble(values.getOrDefault(key, Double.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private record Visual(UUID caster, Vec3 center, VoxelShape geometry, int argb, long expiresAt) {
        RenderEntry renderEntry() { return new RenderEntry(center, geometry, argb); }
    }
    private record RenderEntry(Vec3 center, VoxelShape geometry, int argb) {}
    private record Basis(Vec3 right, Vec3 up) {
        static Basis ground() { return new Basis(new Vec3(1, 0, 0), new Vec3(0, 0, 1)); }
        static Basis facing(Vec3 normal) {
            Vec3 reference = Math.abs(normal.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 right = normal.cross(reference).normalize();
            Vec3 up = right.cross(normal).normalize();
            return new Basis(right, up);
        }
        Vec3 point(double angle, double radius) {
            return right.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
        }
    }
}
