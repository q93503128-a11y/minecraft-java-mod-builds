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
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WorldMagicTracker {
    private static final ContextKey<List<RenderEntry>> DATA_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "world_magic_geometry"));
    private static final Map<UUID, Visual> CHARGES = new HashMap<>();
    private static final List<Visual> RELEASES = new ArrayList<>();

    private static final int MAX_CHARGE_PRIMITIVES = 320;
    private static final int MAX_RELEASE_PRIMITIVES = 512;
    private static final int MAX_RELEASE_VISUALS = 24;
    private static final int MAX_FRAME_PRIMITIVES = 1024;
    private static final double MAX_RENDER_DISTANCE_SQR = 128.0 * 128.0;
    private static final long CHARGE_TTL_NS = 650_000_000L;

    private WorldMagicTracker() {}

    public static void accept(WorldMagicPayload payload) {
        Map<String, String> values = parse(payload.state());
        String kind = values.getOrDefault("kind", "");
        UUID caster;
        try {
            caster = UUID.fromString(values.getOrDefault("caster", ""));
        } catch (IllegalArgumentException ignored) {
            return;
        }

        if ("stop".equals(kind)) {
            CHARGES.remove(caster);
            return;
        }

        SpellDefinition spell = SpellCatalog.spell(values.getOrDefault("spell", "")).orElse(null);
        if (spell == null) return;

        boolean fusion = integer(values, "fusion", 0) != 0;
        int ingredients = Math.max(0, integer(values, "ingredients", 0));
        Vec3 center = new Vec3(
                decimal(values, "x", 0),
                decimal(values, "y", 0),
                decimal(values, "z", 0));
        Vec3 direction = new Vec3(
                decimal(values, "dx", 0),
                decimal(values, "dy", 0),
                decimal(values, "dz", 1));
        if (direction.lengthSqr() < 0.00001) direction = new Vec3(0, 0, 1);
        direction = direction.normalize();

        double range = Math.max(0.1, decimal(values, "range", spell.range()));
        double power = Math.max(0.1, decimal(values, "power", spell.power()));
        double progress = clamp(decimal(values, "progress", 1.0), 0.0, 1.0);
        int duration = Math.max(2, integer(values, "duration", 10));
        long now = System.nanoTime();

        if ("charge".equals(kind)) {
            int progressStep = Math.max(0, Math.min(20, (int) Math.floor(progress * 20.0 + 0.0001)));
            GeometryKey key = GeometryKey.charge(spell.id(), fusion, ingredients, progressStep, direction, range);
            Visual previous = CHARGES.get(caster);
            List<VoxelShape> geometry;
            if (previous != null && key.equals(previous.key)) {
                geometry = previous.geometry;
            } else {
                double quantizedProgress = progressStep / 20.0;
                geometry = buildCharge(spell, fusion, ingredients, direction, quantizedProgress, range);
            }
            CHARGES.put(caster, new Visual(
                    caster, center, geometry, color(spell), now + CHARGE_TTL_NS, key));
            return;
        }

        if ("release".equals(kind)) {
            List<VoxelShape> geometry = buildRelease(spell, fusion, ingredients, direction, range, power);
            while (RELEASES.size() >= MAX_RELEASE_VISUALS) RELEASES.remove(0);
            RELEASES.add(new Visual(
                    caster,
                    center,
                    geometry,
                    color(spell),
                    now + duration * 50_000_000L,
                    GeometryKey.release(spell.id(), direction, range)));
            CHARGES.remove(caster);
        }
    }

    public static void onExtract(ExtractLevelRenderStateEvent event) {
        long now = System.nanoTime();
        CHARGES.values().removeIf(visual -> visual.expiresAt < now);
        RELEASES.removeIf(visual -> visual.expiresAt < now);
        if (CHARGES.isEmpty() && RELEASES.isEmpty()) return;

        List<RenderEntry> entries = new ArrayList<>(CHARGES.size() + RELEASES.size());
        for (Visual visual : CHARGES.values()) entries.add(visual.renderEntry());
        for (Visual visual : RELEASES) entries.add(visual.renderEntry());
        event.getRenderState().setRenderData(DATA_KEY, List.copyOf(entries));
    }

    public static void onSubmit(SubmitCustomGeometryEvent event) {
        List<RenderEntry> entries = event.getLevelRenderState().getRenderData(DATA_KEY);
        if (entries == null || entries.isEmpty()) return;

        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        float baseWidth = Minecraft.getInstance().gameRenderer.gameRenderState()
                .windowRenderState.appropriateLineWidth;
        float lineWidth = Math.max(0.85F, baseWidth * 0.90F);
        int submitted = 0;

        for (RenderEntry entry : entries) {
            Vec3 offset = entry.center.subtract(camera);
            if (offset.lengthSqr() > MAX_RENDER_DISTANCE_SQR) continue;

            event.getPoseStack().pushPose();
            event.getPoseStack().translate(offset.x, offset.y, offset.z);
            for (VoxelShape primitive : entry.geometry) {
                if (submitted >= MAX_FRAME_PRIMITIVES) break;
                event.getSubmitNodeCollector().submitShapeOutline(
                        event.getPoseStack(),
                        primitive,
                        RenderTypes.lines(),
                        entry.argb,
                        lineWidth,
                        false);
                submitted++;
            }
            event.getPoseStack().popPose();
            if (submitted >= MAX_FRAME_PRIMITIVES) break;
        }
    }

    private static List<VoxelShape> buildCharge(
            SpellDefinition spell,
            boolean fusion,
            int ingredients,
            Vec3 normal,
            double progress,
            double range) {
        List<VoxelShape> shapes = new ArrayList<>(Math.min(MAX_CHARGE_PRIMITIVES, 128));
        Basis basis = basis(spell, normal);
        double outer = 0.42
                + spell.circle() * 0.095
                + Math.min(0.32, range * 0.008)
                + (fusion ? 0.18 : 0.0);
        int ringPoints = 16 + spell.circle();

        // The primary concentric count remains exact: 1C=1 ring ... 9C=9 rings.
        for (int ring = 0; ring < spell.circle(); ring++) {
            double radius = outer * (1.0 - ring * 0.66 / Math.max(1.0, spell.circle()));
            double localProgress = clamp(progress * spell.circle() - ring, 0.0, 1.0);
            addPartialCircle(
                    shapes, basis, Vec3.ZERO, radius, ringPoints, localProgress, 0.013,
                    MAX_CHARGE_PRIMITIVES);
        }

        if (progress >= 0.16) {
            int sides = 3 + Math.floorMod(spell.id().hashCode(), 6);
            addPolygon(
                    shapes,
                    basis,
                    Vec3.ZERO,
                    outer * 0.56,
                    sides,
                    clamp((progress - 0.16) / 0.46, 0.0, 1.0),
                    0.017,
                    MAX_CHARGE_PRIMITIVES);
        }

        if (progress >= 0.36) {
            int spokes = Math.min(11, 3 + spell.circle() + Math.floorMod(spell.id().hashCode(), 3));
            int shown = (int) Math.floor(spokes * Math.min(1.0, (progress - 0.36) / 0.42));
            for (int i = 0; i < shown; i++) {
                double angle = Math.PI * 2.0 * i / spokes;
                addSegment(
                        shapes,
                        basis.point(angle, outer * 0.28),
                        basis.point(angle, outer * 0.92),
                        5,
                        0.015,
                        MAX_CHARGE_PRIMITIVES);
            }
        }

        if (spell.circle() >= 3 && progress >= 0.60) {
            int satellites = Math.min(5, spell.circle() - 1);
            for (int i = 0; i < satellites; i++) {
                double angle = Math.PI * 2.0 * i / satellites;
                Vec3 satelliteCenter = basis.point(angle, outer * 1.22);
                addCircle(
                        shapes,
                        basis,
                        satelliteCenter,
                        outer * 0.16,
                        14 + spell.circle(),
                        0.014,
                        MAX_CHARGE_PRIMITIVES);
                addSegment(
                        shapes,
                        basis.point(angle, outer),
                        satelliteCenter,
                        4,
                        0.014,
                        MAX_CHARGE_PRIMITIVES);
            }
        }

        if (fusion && ingredients >= 2 && progress >= 0.42) {
            int visibleIngredients = Math.min(3, ingredients);
            for (int i = 0; i < visibleIngredients; i++) {
                double angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / visibleIngredients;
                Vec3 satelliteCenter = basis.point(angle, outer * 1.48);
                addCircle(
                        shapes,
                        basis,
                        satelliteCenter,
                        outer * 0.25,
                        20,
                        0.016,
                        MAX_CHARGE_PRIMITIVES);
                addPolygon(
                        shapes,
                        basis,
                        satelliteCenter,
                        outer * 0.17,
                        3 + i,
                        1.0,
                        0.015,
                        MAX_CHARGE_PRIMITIVES);
                addSegment(
                        shapes,
                        Vec3.ZERO,
                        satelliteCenter,
                        6,
                        0.014,
                        MAX_CHARGE_PRIMITIVES);
            }
        }

        return List.copyOf(shapes);
    }

    private static List<VoxelShape> buildRelease(
            SpellDefinition spell,
            boolean fusion,
            int ingredients,
            Vec3 direction,
            double range,
            double power) {
        List<VoxelShape> shapes = new ArrayList<>(MAX_RELEASE_PRIMITIVES);
        append(shapes, buildCharge(spell, fusion, ingredients, direction, 1.0, range), MAX_RELEASE_PRIMITIVES);
        Basis basis = basis(spell, direction);

        switch (spell.sigilAnchor()) {
            case FRONT -> {
                double length = Math.min(34.0, Math.max(3.0, range));
                Vec3 end = direction.scale(length);
                addSegment(
                        shapes,
                        Vec3.ZERO,
                        end,
                        Math.min(40, Math.max(12, (int) Math.ceil(length * 1.5))),
                        0.030 + spell.circle() * 0.004,
                        MAX_RELEASE_PRIMITIVES);
                if (spell.circle() >= 4) {
                    Vec3 side = basis.right.scale(0.14 + spell.circle() * 0.025);
                    addSegment(shapes, side, end.add(side), 20, 0.021, MAX_RELEASE_PRIMITIVES);
                    addSegment(
                            shapes,
                            side.scale(-1),
                            end.add(side.scale(-1)),
                            20,
                            0.021,
                            MAX_RELEASE_PRIMITIVES);
                }
                addSphereLattice(
                        shapes,
                        end,
                        0.28 + spell.circle() * 0.09,
                        spell.circle(),
                        MAX_RELEASE_PRIMITIVES);
            }
            case FEET, GROUND_SELF, GROUND_TARGET -> {
                Basis ground = Basis.ground();
                double radius = Math.min(17.0, Math.max(1.8, range * 0.24 + spell.circle() * 0.42));
                int rings = Math.max(2, Math.min(9, spell.circle()));
                for (int i = 1; i <= rings; i++) {
                    addCircle(
                            shapes,
                            ground,
                            Vec3.ZERO,
                            radius * i / rings,
                            20 + Math.min(12, i * 2),
                            0.023,
                            MAX_RELEASE_PRIMITIVES);
                }
                int pillars = Math.min(10, 4 + spell.circle());
                for (int i = 0; i < pillars; i++) {
                    double angle = Math.PI * 2.0 * i / pillars;
                    Vec3 base = ground.point(angle, radius * 0.82);
                    addSegment(
                            shapes,
                            base,
                            base.add(0, 1.2 + spell.circle() * 0.32, 0),
                            8,
                            0.027,
                            MAX_RELEASE_PRIMITIVES);
                }
            }
            case BODY -> addSphereLattice(
                    shapes,
                    new Vec3(0, -0.75, 0),
                    1.15 + spell.circle() * 0.22,
                    spell.circle() + 2,
                    MAX_RELEASE_PRIMITIVES);
            case TARGET -> {
                double radius = 0.75 + spell.circle() * 0.15;
                Basis ground = Basis.ground();
                int levels = Math.min(6, 3 + spell.circle() / 2);
                for (int level = 0; level < levels; level++) {
                    Vec3 offset = new Vec3(0, -0.8 + level * 0.48, 0);
                    addCircle(
                            shapes,
                            ground,
                            offset,
                            radius,
                            24,
                            0.022,
                            MAX_RELEASE_PRIMITIVES);
                }
                int bars = Math.min(12, 5 + spell.circle());
                for (int i = 0; i < bars; i++) {
                    double angle = Math.PI * 2.0 * i / bars;
                    Vec3 low = ground.point(angle, radius).add(0, -0.8, 0);
                    addSegment(
                            shapes,
                            low,
                            low.add(0, 1.9 + spell.circle() * 0.08, 0),
                            9,
                            0.024,
                            MAX_RELEASE_PRIMITIVES);
                }
            }
        }

        if (spell.school() == SpellDefinition.School.SPACE) {
            Basis portal = basis(spell, direction);
            double portalRadius = 0.9 + spell.circle() * 0.16;
            addCircle(
                    shapes,
                    portal,
                    Vec3.ZERO,
                    portalRadius,
                    36,
                    0.024,
                    MAX_RELEASE_PRIMITIVES);
            addCircle(
                    shapes,
                    portal,
                    direction.scale(0.28),
                    portalRadius * 0.82,
                    32,
                    0.022,
                    MAX_RELEASE_PRIMITIVES);
        }

        SignatureGeometry.append(spell, direction, range, shapes, MAX_RELEASE_PRIMITIVES);
        return List.copyOf(shapes);
    }

    private static Basis basis(SpellDefinition spell, Vec3 normal) {
        return switch (spell.sigilAnchor()) {
            case FEET, GROUND_SELF, GROUND_TARGET -> Basis.ground();
            default -> Basis.facing(normal);
        };
    }

    private static void addSphereLattice(
            List<VoxelShape> shapes,
            Vec3 center,
            double radius,
            int detail,
            int budget) {
        Basis ground = Basis.ground();
        addCircle(shapes, ground, center, radius, 28, 0.023, budget);
        addCircle(
                shapes,
                new Basis(new Vec3(1, 0, 0), new Vec3(0, 1, 0)),
                center,
                radius,
                28,
                0.023,
                budget);
        addCircle(
                shapes,
                new Basis(new Vec3(0, 0, 1), new Vec3(0, 1, 0)),
                center,
                radius,
                28,
                0.023,
                budget);
        int latitudes = Math.min(3, Math.max(0, detail - 1));
        for (int i = 1; i <= latitudes; i++) {
            double y = radius * (-0.55 + i * 1.10 / (latitudes + 1));
            double r = Math.sqrt(Math.max(0.05, radius * radius - y * y));
            addCircle(shapes, ground, center.add(0, y, 0), r, 22, 0.019, budget);
        }
    }

    private static void addPartialCircle(
            List<VoxelShape> shapes,
            Basis basis,
            Vec3 center,
            double radius,
            int points,
            double progress,
            double size,
            int budget) {
        int shown = Math.max(0, Math.min(points, (int) Math.ceil(points * progress)));
        for (int i = 0; i < shown && shapes.size() < budget; i++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / points;
            addPoint(shapes, center.add(basis.point(angle, radius)), size, budget);
        }
    }

    private static void addCircle(
            List<VoxelShape> shapes,
            Basis basis,
            Vec3 center,
            double radius,
            int points,
            double size,
            int budget) {
        addPartialCircle(shapes, basis, center, radius, points, 1.0, size, budget);
    }

    private static void addPolygon(
            List<VoxelShape> shapes,
            Basis basis,
            Vec3 center,
            double radius,
            int sides,
            double progress,
            double size,
            int budget) {
        int shownEdges = Math.max(0, Math.min(sides, (int) Math.ceil(sides * progress)));
        List<Vec3> vertices = new ArrayList<>(sides);
        for (int i = 0; i < sides; i++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / sides;
            vertices.add(center.add(basis.point(angle, radius)));
        }
        for (int i = 0; i < shownEdges && shapes.size() < budget; i++) {
            addSegment(
                    shapes,
                    vertices.get(i),
                    vertices.get((i + 1) % sides),
                    6,
                    size,
                    budget);
        }
    }

    private static void addSegment(
            List<VoxelShape> shapes,
            Vec3 start,
            Vec3 end,
            int points,
            double size,
            int budget) {
        int safePoints = Math.max(1, points);
        for (int i = 0; i <= safePoints && shapes.size() < budget; i++) {
            addPoint(shapes, start.lerp(end, i / (double) safePoints), size, budget);
        }
    }

    private static void addPoint(
            List<VoxelShape> shapes,
            Vec3 point,
            double half,
            int budget) {
        if (shapes.size() >= budget) return;
        shapes.add(Shapes.create(new AABB(
                point.x - half,
                point.y - half,
                point.z - half,
                point.x + half,
                point.y + half,
                point.z + half)));
    }

    private static void append(List<VoxelShape> target, List<VoxelShape> source, int budget) {
        for (VoxelShape shape : source) {
            if (target.size() >= budget) return;
            target.add(shape);
        }
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
        try {
            return Integer.parseInt(values.getOrDefault(key, Integer.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double decimal(Map<String, String> values, String key, double fallback) {
        try {
            return Double.parseDouble(values.getOrDefault(key, Double.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Visual(
            UUID caster,
            Vec3 center,
            List<VoxelShape> geometry,
            int argb,
            long expiresAt,
            GeometryKey key) {
        RenderEntry renderEntry() {
            return new RenderEntry(center, geometry, argb);
        }
    }

    private record RenderEntry(Vec3 center, List<VoxelShape> geometry, int argb) {}

    private record GeometryKey(
            String spellId,
            boolean fusion,
            int ingredients,
            int progressStep,
            int directionX,
            int directionY,
            int directionZ,
            int rangeStep,
            boolean release) {
        static GeometryKey charge(
                String spellId,
                boolean fusion,
                int ingredients,
                int progressStep,
                Vec3 direction,
                double range) {
            return new GeometryKey(
                    spellId,
                    fusion,
                    ingredients,
                    progressStep,
                    quantize(direction.x, 18),
                    quantize(direction.y, 18),
                    quantize(direction.z, 18),
                    quantize(range, 4),
                    false);
        }

        static GeometryKey release(String spellId, Vec3 direction, double range) {
            return new GeometryKey(
                    spellId,
                    false,
                    0,
                    20,
                    quantize(direction.x, 18),
                    quantize(direction.y, 18),
                    quantize(direction.z, 18),
                    quantize(range, 4),
                    true);
        }

        private static int quantize(double value, int scale) {
            return (int) Math.round(value * scale);
        }
    }

    private record Basis(Vec3 right, Vec3 up) {
        static Basis ground() {
            return new Basis(new Vec3(1, 0, 0), new Vec3(0, 0, 1));
        }

        static Basis facing(Vec3 normal) {
            Vec3 safe = normal.lengthSqr() < 0.00001 ? new Vec3(0, 0, 1) : normal.normalize();
            Vec3 reference = Math.abs(safe.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 right = safe.cross(reference).normalize();
            Vec3 up = right.cross(safe).normalize();
            return new Basis(right, up);
        }

        Vec3 point(double angle, double radius) {
            return right.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
        }
    }
}
