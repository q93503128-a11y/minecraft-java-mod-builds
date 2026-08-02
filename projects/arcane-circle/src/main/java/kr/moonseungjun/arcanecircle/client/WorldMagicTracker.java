package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.network.WorldMagicPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;

/**
 * Multiplayer-synchronised world-space magic renderer.
 *
 * <p>Alpha.8 deliberately stops using VoxelShape/AABB point clouds. Circles, runes and spell
 * bodies are now continuous line meshes. The renderer remains particle-free and keeps hard
 * segment/visual budgets so high-circle rituals cannot freeze the render thread.</p>
 */
public final class WorldMagicTracker {
    private static final ContextKey<List<RenderEntry>> DATA_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "world_magic_mesh"));
    private static final Map<UUID, Visual> CHARGES = new HashMap<>();
    private static final List<Visual> RELEASES = new ArrayList<>();

    private static final int MAX_CHARGE_SEGMENTS = 460;
    private static final int MAX_RELEASE_SEGMENTS = 820;
    private static final int MAX_RELEASE_VISUALS = 18;
    private static final int MAX_FRAME_SEGMENTS = 1900;
    private static final double MAX_RENDER_DISTANCE_SQR = 160.0 * 160.0;
    private static final long CHARGE_TTL_NS = 650_000_000L;

    private static final Set<String> BEAMS = Set.of(
            "ray_of_frost", "scorching_ray", "lightning_bolt", "disintegrate", "sunbeam",
            "chain_lightning", "prismatic_spray", "void_lance");
    private static final Set<String> PROJECTILES = Set.of(
            "magic_missile", "fire_bolt", "fireball", "ice_knife", "chromatic_orb",
            "delayed_blast_fireball", "freezing_sphere", "arcane_hand", "phoenix_requiem");
    private static final Set<String> WAVES = Set.of(
            "thunderwave", "gust_of_wind", "burning_hands", "cone_of_cold", "shatter",
            "steam_burst", "world_sunder");

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
        double power = Math.max(0.1, decimal(values, "power", Math.max(0.1, spell.power())));
        double progress = clamp(decimal(values, "progress", 1.0), 0.0, 1.0);
        int duration = Math.max(2, integer(values, "duration", 10));
        long now = System.nanoTime();

        if ("charge".equals(kind)) {
            int progressStep = Math.max(0, Math.min(24, (int) Math.floor(progress * 24.0 + 0.0001)));
            GeometryKey key = GeometryKey.charge(spell.id(), fusion, ingredients, progressStep, direction, range);
            Visual previous = CHARGES.get(caster);
            ArcaneWorldMesh mesh;
            if (previous != null && key.equals(previous.key)) {
                mesh = previous.mesh;
            } else {
                mesh = buildCharge(spell, fusion, ingredients, direction, progressStep / 24.0, range);
            }
            CHARGES.put(caster, new Visual(caster, center, mesh, color(spell), now + CHARGE_TTL_NS, key));
            return;
        }

        if ("release".equals(kind)) {
            ArcaneWorldMesh mesh = buildRelease(spell, fusion, ingredients, direction, range, power);
            while (RELEASES.size() >= MAX_RELEASE_VISUALS) RELEASES.removeFirst();
            RELEASES.add(new Visual(
                    caster,
                    center,
                    mesh,
                    color(spell),
                    now + duration * 50_000_000L,
                    GeometryKey.release(spell.id(), fusion, ingredients, direction, range, power)));
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
        float windowScale = Math.max(0.72F, baseWidth * 0.82F);
        int submitted = 0;

        for (RenderEntry entry : entries) {
            if (submitted >= MAX_FRAME_SEGMENTS) break;
            Vec3 offset = entry.center.subtract(camera);
            if (offset.lengthSqr() > MAX_RENDER_DISTANCE_SQR) continue;

            event.getPoseStack().pushPose();
            event.getPoseStack().translate(offset.x, offset.y, offset.z);
            entry.mesh.submit(event.getPoseStack(), event.getSubmitNodeCollector(), entry.argb, windowScale);
            event.getPoseStack().popPose();
            submitted += entry.mesh.size();
        }
    }

    private static ArcaneWorldMesh buildCharge(
            SpellDefinition spell,
            boolean fusion,
            int ingredients,
            Vec3 normal,
            double progress,
            double range) {
        ArcaneWorldMesh.Builder mesh = ArcaneWorldMesh.builder(MAX_CHARGE_SEGMENTS);
        appendCharge(mesh, spell, fusion, ingredients, normal, progress, range);
        return mesh.build();
    }

    private static void appendCharge(
            ArcaneWorldMesh.Builder mesh,
            SpellDefinition spell,
            boolean fusion,
            int ingredients,
            Vec3 normal,
            double progress,
            double range) {
        ArcaneWorldMesh.Basis basis = basis(spell, normal);
        int circle = Math.max(1, Math.min(9, spell.circle()));
        double scale = rangeScale(spell, range);
        double outer = (0.46 + circle * 0.115 + circle * circle * 0.0125) * scale
                + (fusion ? 0.24 * scale : 0.0);
        float coreWidth = (float) (0.70 + circle * 0.055);
        double rotation = Math.floorMod(spell.id().hashCode(), 360) * Math.PI / 180.0;

        // Lore rule remains exact: a completed n-circle spell has exactly n primary rings.
        for (int ring = 0; ring < circle && !mesh.full(); ring++) {
            double radius = outer * (1.0 - ring * 0.62 / Math.max(1.0, circle));
            double local = clamp(progress * circle - ring, 0.0, 1.0);
            if (local <= 0.0) continue;
            int segments = 36 + circle * 5 + ring * 2;
            mesh.arc(basis, Vec3.ZERO, radius, -Math.PI / 2.0 + rotation * 0.08,
                    Math.PI * 2.0 * local, Math.max(3, (int) Math.ceil(segments * local)),
                    coreWidth * (ring == 0 ? 1.08F : 0.82F));
        }

        if (progress >= 0.13) {
            double p = clamp((progress - 0.13) / 0.37, 0.0, 1.0);
            appendSchoolSeal(mesh, spell, basis, outer, rotation, p, coreWidth);
        }

        if (progress >= 0.38) {
            double p = clamp((progress - 0.38) / 0.34, 0.0, 1.0);
            int runes = Math.min(14, 4 + circle + Math.floorMod(spell.id().hashCode(), 3));
            int visible = Math.max(1, (int) Math.floor(runes * p));
            for (int i = 0; i < visible && !mesh.full(); i++) {
                double angle = rotation + Math.PI * 2.0 * i / runes;
                double inner = outer * (0.60 + (i % 2) * 0.06);
                double outerPoint = outer * (0.88 + (i % 3) * 0.025);
                Vec3 a = basis.point(angle, inner);
                Vec3 b = basis.point(angle + ((i & 1) == 0 ? 0.09 : -0.09), outerPoint);
                mesh.line(a, b, coreWidth * 0.72F);
                if (circle >= 4) {
                    Vec3 c = basis.point(angle + 0.15, outerPoint * 0.96);
                    mesh.line(b, c, coreWidth * 0.56F);
                }
            }
        }

        if (circle >= 3 && progress >= 0.55) {
            int satellites = Math.min(7, 2 + circle / 2);
            int visible = Math.max(1, (int) Math.floor(satellites * clamp((progress - 0.55) / 0.30, 0, 1)));
            for (int i = 0; i < visible && !mesh.full(); i++) {
                double angle = rotation + Math.PI * 2.0 * i / satellites;
                Vec3 center = basis.point(angle, outer * 1.22);
                double satelliteRadius = outer * (0.095 + circle * 0.006);
                mesh.circle(basis, center, satelliteRadius, 24 + circle * 2, coreWidth * 0.68F);
                mesh.polygon(basis, center, satelliteRadius * 0.66,
                        3 + Math.floorMod(i + circle, 4), rotation * 0.5, coreWidth * 0.50F);
                mesh.line(basis.point(angle, outer * 0.93),
                        center.add(basis.point(angle + Math.PI / 2.0, satelliteRadius * 0.18)),
                        coreWidth * 0.50F);
            }
        }

        // High-circle circles are not just larger: they gain independent tilted orbital layers.
        if (circle >= 6 && progress >= 0.66) {
            int orbits = circle - 5;
            int visible = Math.max(1, (int) Math.floor(orbits * clamp((progress - 0.66) / 0.30, 0, 1)));
            Vec3 facing = basis.normal();
            for (int i = 0; i < visible && !mesh.full(); i++) {
                double tilt = 0.28 + i * 0.19;
                Vec3 orbitNormal = facing.add(basis.right().scale(Math.sin(tilt) * 0.62))
                        .add(basis.up().scale(Math.cos(tilt) * 0.42)).normalize();
                ArcaneWorldMesh.Basis orbit = ArcaneWorldMesh.Basis.fromNormal(orbitNormal, basis.right());
                mesh.circle(orbit, Vec3.ZERO, outer * (1.04 + i * 0.10),
                        54 + circle * 5, coreWidth * 0.62F);
            }
            mesh.runeChords(basis, Vec3.ZERO, outer * 0.48,
                    7 + circle, 2 + circle % 4, rotation, coreWidth * 0.56F);
        }

        if (fusion && ingredients >= 2 && progress >= 0.42) {
            int count = Math.min(3, ingredients);
            for (int i = 0; i < count && !mesh.full(); i++) {
                double angle = -Math.PI / 2.0 + rotation + Math.PI * 2.0 * i / count;
                Vec3 center = basis.point(angle, outer * 1.52);
                double radius = outer * 0.24;
                mesh.circle(basis, center, radius, 38, coreWidth * 0.88F);
                mesh.star(basis, center, radius * 0.72, radius * 0.34,
                        3 + i, rotation * (i + 1), coreWidth * 0.58F);
                mesh.line(basis.point(angle, outer * 0.74), center, coreWidth * 0.62F);
            }
            mesh.circle(basis, Vec3.ZERO, outer * 1.78, 68 + circle * 4, coreWidth * 0.50F);
        }
    }

    private static void appendSchoolSeal(
            ArcaneWorldMesh.Builder mesh,
            SpellDefinition spell,
            ArcaneWorldMesh.Basis basis,
            double outer,
            double rotation,
            double progress,
            float width) {
        double radius = outer * 0.54;
        int visibleSegments = Math.max(3, (int) Math.ceil((28 + spell.circle() * 3) * progress));
        switch (spell.school()) {
            case FIRE -> {
                mesh.star(basis, Vec3.ZERO, radius, radius * 0.42,
                        3 + spell.circle() / 3, rotation, width * 0.82F);
                if (progress > 0.55) mesh.polygon(basis, Vec3.ZERO, radius * 0.72,
                        3, -rotation, width * 0.58F);
            }
            case FROST -> {
                int arms = 6 + (spell.circle() >= 7 ? 2 : 0);
                for (int i = 0; i < Math.max(1, (int) Math.ceil(arms * progress)); i++) {
                    double angle = rotation + Math.PI * 2.0 * i / arms;
                    Vec3 tip = basis.point(angle, radius);
                    Vec3 root = basis.point(angle, radius * 0.12);
                    mesh.line(root, tip, width * 0.70F);
                    Vec3 branch = basis.point(angle, radius * 0.68);
                    mesh.line(branch, branch.add(basis.point(angle + 2.35, radius * 0.22)), width * 0.46F);
                    mesh.line(branch, branch.add(basis.point(angle - 2.35, radius * 0.22)), width * 0.46F);
                }
            }
            case WIND -> {
                int turns = 2 + spell.circle() / 3;
                List<Vec3> spiral = new ArrayList<>(visibleSegments + 1);
                for (int i = 0; i <= visibleSegments; i++) {
                    double t = i / (double) Math.max(1, visibleSegments);
                    spiral.add(basis.point(rotation + Math.PI * 2.0 * turns * t, radius * t));
                }
                mesh.polyline(spiral, width * 0.72F, false);
            }
            case WARD -> {
                mesh.polygon(basis, Vec3.ZERO, radius, 6 + spell.circle() / 4, rotation, width * 0.84F);
                mesh.runeChords(basis, Vec3.ZERO, radius * 0.78,
                        6 + spell.circle(), 2, -rotation, width * 0.52F);
            }
            case LIFE -> {
                ArcaneWorldMesh.Basis tiltedA = ArcaneWorldMesh.Basis.fromNormal(
                        basis.normal().add(basis.right().scale(0.18)), basis.up());
                ArcaneWorldMesh.Basis tiltedB = ArcaneWorldMesh.Basis.fromNormal(
                        basis.normal().add(basis.right().scale(-0.18)), basis.up());
                mesh.circle(tiltedA, basis.right().scale(radius * 0.19), radius * 0.64,
                        visibleSegments, width * 0.68F);
                mesh.circle(tiltedB, basis.right().scale(-radius * 0.19), radius * 0.64,
                        visibleSegments, width * 0.68F);
                mesh.line(basis.up().scale(-radius), basis.up().scale(radius), width * 0.54F);
            }
            case SPACE -> {
                mesh.circle(basis, basis.right().scale(radius * 0.16), radius * 0.77,
                        visibleSegments, width * 0.80F);
                mesh.circle(basis, basis.right().scale(-radius * 0.16), radius * 0.77,
                        visibleSegments, width * 0.58F);
                mesh.runeChords(basis, Vec3.ZERO, radius * 0.58,
                        7 + spell.circle(), 3, rotation, width * 0.48F);
            }
            default -> {
                mesh.polygon(basis, Vec3.ZERO, radius, 4 + spell.circle() / 2,
                        rotation, width * 0.76F);
                mesh.runeChords(basis, Vec3.ZERO, radius * 0.72,
                        7 + spell.circle(), 2 + spell.circle() % 3,
                        -rotation, width * 0.52F);
            }
        }
    }

    private static ArcaneWorldMesh buildRelease(
            SpellDefinition spell,
            boolean fusion,
            int ingredients,
            Vec3 direction,
            double range,
            double power) {
        ArcaneWorldMesh.Builder mesh = ArcaneWorldMesh.builder(MAX_RELEASE_SEGMENTS);
        appendCharge(mesh, spell, fusion, ingredients, direction, 1.0, range);
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(direction);
        int circle = Math.max(1, Math.min(9, spell.circle()));
        double rangeFactor = rangeScale(spell, range);
        double powerFactor = powerScale(spell, power);
        float width = (float) (0.82 + circle * 0.075 + powerFactor * 0.12);

        if (BEAMS.contains(spell.id())) {
            appendBeam(mesh, spell, direction, facing, range, rangeFactor, powerFactor, width);
        } else if (PROJECTILES.contains(spell.id())) {
            appendProjectile(mesh, spell, direction, facing, range, rangeFactor, powerFactor, width);
        } else if (WAVES.contains(spell.id())) {
            appendWave(mesh, spell, direction, facing, range, rangeFactor, powerFactor, width);
        } else {
            switch (spell.sigilAnchor()) {
                case FRONT -> appendProjectile(mesh, spell, direction, facing, range,
                        rangeFactor, powerFactor, width);
                case FEET, GROUND_SELF, GROUND_TARGET -> appendGroundField(mesh, spell,
                        range, rangeFactor, powerFactor, width);
                case BODY -> appendBodyAura(mesh, spell, rangeFactor, powerFactor, width);
                case TARGET -> appendTargetSeal(mesh, spell, rangeFactor, powerFactor, width);
            }
        }

        SignatureGeometry.append(spell, direction, range, power, mesh);
        return mesh.build();
    }

    private static void appendBeam(
            ArcaneWorldMesh.Builder mesh,
            SpellDefinition spell,
            Vec3 direction,
            ArcaneWorldMesh.Basis facing,
            double range,
            double rangeFactor,
            double powerFactor,
            float width) {
        double length = Math.min(72.0, Math.max(4.0, range));
        int strands = Math.min(7, 1 + spell.circle() / 2);
        for (int i = 0; i < strands && !mesh.full(); i++) {
            double phase = Math.PI * 2.0 * i / strands;
            double offset = (0.035 + spell.circle() * 0.008) * powerFactor;
            Vec3 side = facing.point(phase, offset);
            mesh.line(side, direction.scale(length).add(side), width * (i == 0 ? 1.0F : 0.62F));
        }
        double impact = (0.34 + spell.circle() * 0.10) * rangeFactor * powerFactor;
        Vec3 end = direction.scale(length);
        mesh.sphere(end, impact, spell.circle(), width * 0.62F);
        mesh.circle(facing, end, impact * 1.45, 36 + spell.circle() * 4, width * 0.48F);
    }

    private static void appendProjectile(
            ArcaneWorldMesh.Builder mesh,
            SpellDefinition spell,
            Vec3 direction,
            ArcaneWorldMesh.Basis facing,
            double range,
            double rangeFactor,
            double powerFactor,
            float width) {
        double length = Math.min(58.0, Math.max(3.2, range));
        double trailRadius = (0.10 + spell.circle() * 0.018) * powerFactor;
        int turns = 2 + spell.circle() / 2;
        int segments = 34 + spell.circle() * 7;
        mesh.helix(Vec3.ZERO, direction, facing, length, trailRadius,
                turns, segments, width * 0.58F, true);
        if (spell.circle() >= 3) {
            mesh.helix(Vec3.ZERO, direction, facing, length, trailRadius,
                    -turns, segments, width * 0.42F, true);
        }
        Vec3 end = direction.scale(length);
        double core = (0.20 + spell.circle() * 0.075) * rangeFactor * powerFactor;
        mesh.sphere(end, core, spell.circle(), width * 0.74F);
        mesh.star(facing, end, core * 1.46, core * 0.58,
                Math.min(9, 4 + spell.circle() / 2), 0.0, width * 0.52F);
    }

    private static void appendWave(
            ArcaneWorldMesh.Builder mesh,
            SpellDefinition spell,
            Vec3 direction,
            ArcaneWorldMesh.Basis facing,
            double range,
            double rangeFactor,
            double powerFactor,
            float width) {
        double length = Math.min(36.0, Math.max(4.5, range * 0.78));
        double endRadius = (1.15 + spell.circle() * 0.38) * rangeFactor * powerFactor;
        mesh.cone(Vec3.ZERO, direction, facing, length, endRadius,
                5 + spell.circle(), 3 + spell.circle() / 2, width * 0.58F);
        for (int i = 1; i <= Math.min(5, 1 + spell.circle() / 2); i++) {
            double t = i / (double) Math.min(5, 1 + spell.circle() / 2);
            mesh.circle(facing, direction.scale(length * t), endRadius * t,
                    28 + spell.circle() * 4, width * (float) (0.72 - t * 0.18));
        }
    }

    private static void appendGroundField(
            ArcaneWorldMesh.Builder mesh,
            SpellDefinition spell,
            double range,
            double rangeFactor,
            double powerFactor,
            float width) {
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double radius = Math.min(32.0,
                Math.max(1.8, (range * 0.24 + spell.circle() * 0.52) * rangeFactor));
        int rings = Math.max(2, Math.min(9, spell.circle()));
        for (int i = 1; i <= rings && !mesh.full(); i++) {
            mesh.circle(ground, Vec3.ZERO, radius * i / rings,
                    44 + i * 4, width * (i == rings ? 0.82F : 0.46F));
        }
        appendSchoolSeal(mesh, spell, ground, radius * 0.88,
                Math.floorMod(spell.id().hashCode(), 360) * Math.PI / 180.0,
                1.0, width * 0.68F);
        int pillars = Math.min(12, 4 + spell.circle());
        for (int i = 0; i < pillars && !mesh.full(); i++) {
            double angle = Math.PI * 2.0 * i / pillars;
            Vec3 base = ground.point(angle, radius * 0.82);
            mesh.helix(base, new Vec3(0, 1, 0),
                    ArcaneWorldMesh.Basis.facing(new Vec3(0, 1, 0)),
                    (1.1 + spell.circle() * 0.38) * powerFactor,
                    0.08 + spell.circle() * 0.012, 2 + spell.circle() / 3,
                    20 + spell.circle() * 3, width * 0.38F, true);
        }
    }

    private static void appendBodyAura(
            ArcaneWorldMesh.Builder mesh,
            SpellDefinition spell,
            double rangeFactor,
            double powerFactor,
            float width) {
        double radius = (1.05 + spell.circle() * 0.24) * rangeFactor * powerFactor;
        mesh.sphere(new Vec3(0, -0.72, 0), radius, spell.circle() + 2, width * 0.58F);
        if (spell.circle() >= 5) {
            mesh.circle(ArcaneWorldMesh.Basis.ground(), new Vec3(0, -0.72, 0),
                    radius * 1.15, 58 + spell.circle() * 4, width * 0.42F);
        }
    }

    private static void appendTargetSeal(
            ArcaneWorldMesh.Builder mesh,
            SpellDefinition spell,
            double rangeFactor,
            double powerFactor,
            float width) {
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double radius = (0.72 + spell.circle() * 0.18) * rangeFactor * powerFactor;
        double bottom = -0.92;
        double top = 1.45 + spell.circle() * 0.16;
        int layers = Math.min(8, 3 + spell.circle() / 2);
        for (int level = 0; level < layers && !mesh.full(); level++) {
            double t = level / (double) Math.max(1, layers - 1);
            mesh.circle(ground, new Vec3(0, bottom + (top - bottom) * t, 0),
                    radius * (0.92 + 0.08 * Math.sin(Math.PI * t)),
                    42 + spell.circle() * 3, width * 0.52F);
        }
        int bars = Math.min(16, 6 + spell.circle());
        for (int i = 0; i < bars && !mesh.full(); i++) {
            double angle = Math.PI * 2.0 * i / bars;
            mesh.line(ground.point(angle, radius).add(0, bottom, 0),
                    ground.point(angle + 0.08 * Math.sin(i), radius).add(0, top, 0),
                    width * 0.66F);
        }
    }

    private static ArcaneWorldMesh.Basis basis(SpellDefinition spell, Vec3 normal) {
        return switch (spell.sigilAnchor()) {
            case FEET, GROUND_SELF, GROUND_TARGET -> ArcaneWorldMesh.Basis.ground();
            default -> ArcaneWorldMesh.Basis.facing(normal);
        };
    }

    private static double rangeScale(SpellDefinition spell, double range) {
        double base = Math.max(4.0, spell.range());
        double ratio = Math.max(0.12, range / base);
        return clamp(Math.pow(ratio, 0.58), 0.78, 3.40);
    }

    private static double powerScale(SpellDefinition spell, double power) {
        double base = Math.max(1.0, spell.power());
        return clamp(Math.pow(Math.max(0.08, power / base), 0.22), 0.82, 2.65);
    }

    private static int color(SpellDefinition spell) {
        return switch (spell.school()) {
            case FIRE -> 0xE8FF7048;
            case FROST -> 0xE86DE4FF;
            case WIND -> 0xE876E6BD;
            case WARD -> 0xE8C595FF;
            case LIFE -> 0xE873E38E;
            case SPACE -> 0xE8A382FF;
            default -> 0xE882A8FF;
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
            ArcaneWorldMesh mesh,
            int argb,
            long expiresAt,
            GeometryKey key) {
        RenderEntry renderEntry() {
            return new RenderEntry(center, mesh, argb);
        }
    }

    private record RenderEntry(Vec3 center, ArcaneWorldMesh mesh, int argb) {}

    private record GeometryKey(
            String spellId,
            boolean fusion,
            int ingredients,
            int progressStep,
            int directionX,
            int directionY,
            int directionZ,
            int rangeStep,
            int powerStep,
            boolean release) {
        static GeometryKey charge(
                String spellId,
                boolean fusion,
                int ingredients,
                int progressStep,
                Vec3 direction,
                double range) {
            return new GeometryKey(
                    spellId, fusion, ingredients, progressStep,
                    quantize(direction.x, 24), quantize(direction.y, 24), quantize(direction.z, 24),
                    quantize(range, 5), 0, false);
        }

        static GeometryKey release(
                String spellId,
                boolean fusion,
                int ingredients,
                Vec3 direction,
                double range,
                double power) {
            return new GeometryKey(
                    spellId, fusion, ingredients, 24,
                    quantize(direction.x, 24), quantize(direction.y, 24), quantize(direction.z, 24),
                    quantize(range, 5), quantize(power, 2), true);
        }

        private static int quantize(double value, int scale) {
            return (int) Math.round(value * scale);
        }
    }
}
