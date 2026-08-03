package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.SpellMetrics;
import kr.moonseungjun.arcanecircle.network.WorldMagicPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Filled, animated spell bodies. Only spells that are actually beams draw a continuous beam;
 * bolts and missiles are compact travelling forms rather than a line stretched to max range.
 */
public final class WorldMagicTracker {
    private static final ContextKey<List<RenderEntry>> DATA_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "world_magic_mesh_v2"));
    private static final Map<UUID, Visual> CHARGES = new HashMap<>();
    private static final List<Visual> RELEASES = new ArrayList<>();
    private static final int MAX_CHARGE_GEOMETRY = 520;
    private static final int MAX_RELEASE_GEOMETRY = 760;
    private static final int MAX_VISUALS = 18;
    private static final int MAX_FRAME = 2800;
    private static final double MAX_DISTANCE_SQR = 192.0 * 192.0;
    private static final long CHARGE_TTL = 2_250_000_000L;

    private static final Set<String> TRUE_BEAMS = Set.of(
            "ray_of_frost", "scorching_ray", "lightning_bolt", "disintegrate",
            "sunbeam", "chain_lightning", "prismatic_spray");
    private static final Set<String> WAVES = Set.of(
            "thunderwave", "gust_of_wind", "burning_hands", "cone_of_cold",
            "shatter", "steam_burst", "world_sunder", "flame_wave", "wind_blade");

    private WorldMagicTracker() {}

    public static void accept(WorldMagicPayload payload) {
        Map<String, String> values = parse(payload.state());
        String kind = values.getOrDefault("kind", "");
        UUID caster;
        try {
            caster = UUID.fromString(values.getOrDefault("caster", ""));
        } catch (Exception ignored) {
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
        Vec3 center = new Vec3(decimal(values, "x", 0.0), decimal(values, "y", 0.0),
                decimal(values, "z", 0.0));
        Vec3 direction = safeDirection(new Vec3(decimal(values, "dx", 0.0),
                decimal(values, "dy", 0.0), decimal(values, "dz", 1.0)));
        double range = Math.max(0.1, decimal(values, "range", spell.range()));
        double power = Math.max(0.1, decimal(values, "power", Math.max(0.1, spell.power())));
        double progress = clamp(decimal(values, "progress", 1.0), 0.0, 1.0);
        int duration = Math.max(3, integer(values, "duration", 10));
        long now = System.nanoTime();

        if ("charge".equals(kind)) {
            Visual previous = CHARGES.get(caster);
            long started = previous != null && previous.spell.id().equals(spell.id())
                    ? previous.startedAt : now;
            CHARGES.put(caster, new Visual(caster, spell, fusion, ingredients, center, direction,
                    range, power, progress, started, now + CHARGE_TTL, false));
            return;
        }
        if ("release".equals(kind)) {
            while (RELEASES.size() >= MAX_VISUALS) RELEASES.removeFirst();
            RELEASES.add(new Visual(caster, spell, fusion, ingredients, center, direction,
                    range, power, 1.0, now, now + duration * 50_000_000L, true));
            CHARGES.remove(caster);
        }
    }

    public static void onExtract(ExtractLevelRenderStateEvent event) {
        long now = System.nanoTime();
        CHARGES.values().removeIf(value -> value.expiresAt < now);
        RELEASES.removeIf(value -> value.expiresAt < now);
        if (CHARGES.isEmpty() && RELEASES.isEmpty()) return;

        List<RenderEntry> entries = new ArrayList<>(CHARGES.size() + RELEASES.size());
        for (Visual visual : CHARGES.values()) {
            entries.add(new RenderEntry(visual.center, buildCharge(visual), color(visual.spell)));
        }
        for (Visual visual : RELEASES) {
            double age = clamp((now - visual.startedAt) / (double) Math.max(1L,
                    visual.expiresAt - visual.startedAt), 0.0, 1.0);
            entries.add(new RenderEntry(visual.center, buildRelease(visual, age), color(visual.spell)));
        }
        event.getRenderState().setRenderData(DATA_KEY, List.copyOf(entries));
    }

    public static void onSubmit(SubmitCustomGeometryEvent event) {
        List<RenderEntry> entries = event.getLevelRenderState().getRenderData(DATA_KEY);
        if (entries == null || entries.isEmpty()) return;
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        float base = Minecraft.getInstance().gameRenderer.gameRenderState()
                .windowRenderState.appropriateLineWidth;
        float scale = Math.max(0.72F, base * 0.82F);
        int used = 0;
        for (RenderEntry entry : entries) {
            if (used >= MAX_FRAME) break;
            Vec3 offset = entry.center.subtract(camera);
            if (offset.lengthSqr() > MAX_DISTANCE_SQR) continue;
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(offset.x, offset.y, offset.z);
            entry.mesh.submit(event.getPoseStack(), event.getSubmitNodeCollector(), entry.argb, scale);
            event.getPoseStack().popPose();
            used += entry.mesh.size();
        }
    }

    private static ArcaneWorldMesh buildCharge(Visual visual) {
        SpellDefinition spell = visual.spell;
        ArcaneWorldMesh.Builder mesh = ArcaneWorldMesh.builder(MAX_CHARGE_GEOMETRY);
        ArcaneWorldMesh.Basis basis = basis(spell, visual.direction);
        int circle = clampCircle(spell.circle());
        double p = Math.max(0.08, visual.progress);
        double scale = clamp(Math.pow(Math.max(0.2, visual.range / Math.max(4.0, spell.range())), 0.22),
                0.86, 1.55);
        double outer = (0.54 + circle * 0.105 + circle * circle * 0.006
                + (visual.fusion ? 0.20 : 0.0)) * scale;
        double rotation = Math.floorMod(spell.id().hashCode(), 360) * Math.PI / 180.0
                + p * 0.22;

        // Broad translucent plate first: this makes the circle read as a surface, not wire art.
        mesh.disc(basis, Vec3.ZERO, outer * (0.94 + p * 0.06), 40 + circle * 4,
                0.46F, (float) (0.035 + p * 0.055));
        mesh.band(basis, Vec3.ZERO, outer * 0.82, outer, 44 + circle * 4,
                1.10F, (float) (0.18 + p * 0.20));
        mesh.band(basis, Vec3.ZERO, outer * 0.48, outer * 0.56, 36 + circle * 3,
                0.88F, (float) (0.12 + p * 0.16));
        mesh.polygonPlate(basis, Vec3.ZERO, outer * 0.70, 4 + circle / 2,
                rotation, 0.58F, (float) (0.055 + p * 0.095));
        schoolSeal(mesh, spell, basis, outer * 0.45, rotation, p);

        int glyphs = Math.min(12, 4 + circle);
        int visible = Math.max(1, (int) Math.ceil(glyphs * p));
        for (int i = 0; i < visible && !mesh.full(); i++) {
            double angle = rotation + Math.PI * 2.0 * i / glyphs;
            Vec3 glyph = basis.point(angle, outer * 0.68);
            double size = outer * (0.055 + (i % 3) * 0.010);
            if ((i & 1) == 0) mesh.diamond(basis, glyph, size, -angle, 1.12F, 0.34F);
            else mesh.polygonPlate(basis, glyph, size, 3 + i % 3, angle,
                    0.98F, 0.28F);
        }

        if (visual.fusion && visual.ingredients >= 2) {
            int count = Math.min(3, visual.ingredients);
            for (int i = 0; i < count && !mesh.full(); i++) {
                double angle = rotation + Math.PI * 2.0 * i / count;
                Vec3 node = basis.point(angle, outer * 1.22);
                double radius = outer * 0.14;
                mesh.disc(basis, node, radius, 24, 0.78F, 0.10F);
                mesh.band(basis, node, radius * 0.70, radius, 28, 1.18F, 0.34F);
                mesh.starPlate(basis, node, radius * 0.58, radius * 0.23,
                        3 + i, rotation, 1.05F, 0.30F);
            }
            mesh.brokenBand(basis, Vec3.ZERO, outer * 1.34, outer * 1.39,
                    64, 6, 1.05F, 0.25F);
        }
        return mesh.build();
    }

    private static ArcaneWorldMesh buildRelease(Visual visual, double age) {
        ArcaneWorldMesh.Builder mesh = ArcaneWorldMesh.builder(MAX_RELEASE_GEOMETRY);
        SpellDefinition spell = visual.spell;
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(visual.direction);
        int circle = clampCircle(spell.circle());
        double powerFactor = clamp(Math.pow(Math.max(0.08,
                visual.power / Math.max(1.0, spell.power())), 0.18), 0.82, 2.0);

        if (TRUE_BEAMS.contains(spell.id())) {
            buildBeam(mesh, visual, facing, age, powerFactor);
        } else if (WAVES.contains(spell.id())) {
            buildWave(mesh, visual, facing, age, powerFactor);
        } else {
            switch (spell.sigilAnchor()) {
                case FEET, GROUND_SELF, GROUND_TARGET -> buildField(mesh, visual, age, powerFactor);
                case BODY -> buildAura(mesh, visual, age, powerFactor);
                case TARGET -> buildTargetBurst(mesh, visual, age, powerFactor);
                case FRONT -> buildProjectile(mesh, visual, facing, age, powerFactor);
            }
        }

        if (visual.fusion) {
            double ring = 0.48 + circle * 0.07;
            mesh.brokenBand(facing, Vec3.ZERO, ring, ring + 0.055,
                    48, 5, 1.20F, (float) (0.18 * (1.0 - age)));
        }
        return mesh.build();
    }

    private static void buildProjectile(ArcaneWorldMesh.Builder mesh, Visual visual,
                                        ArcaneWorldMesh.Basis facing, double age, double powerFactor) {
        SpellDefinition spell = visual.spell;
        int circle = clampCircle(spell.circle());
        double travel = Math.min(72.0, Math.max(3.0, visual.range));
        double eased = 1.0 - Math.pow(1.0 - age, 1.35);
        Vec3 position = visual.direction.scale(travel * eased);
        double core = (0.18 + circle * 0.055) * powerFactor;

        // Three separated afterimages communicate velocity without a continuous line.
        for (int i = 1; i <= 3; i++) {
            double back = core * (1.0 + i * 1.15) + travel * 0.025 * i;
            Vec3 tail = position.subtract(visual.direction.scale(back));
            double radius = core * (0.48 - i * 0.09);
            if (radius > 0.03) mesh.orb(tail, radius, 14, 0.72F, 0.16F);
        }

        switch (spell.school()) {
            case FIRE -> {
                mesh.orb(position, core, 24 + circle * 2, 1.20F, 0.52F);
                mesh.starPlate(facing, position, core * 1.25, core * 0.48,
                        5 + circle / 3, age * 2.5, 1.12F, 0.36F);
                for (int i = 0; i < 3; i++) {
                    Vec3 ember = position.add(facing.point(age * 4.0 + i * 2.09, core * 0.80));
                    mesh.orb(ember, core * 0.22, 12, 1.28F, 0.34F);
                }
            }
            case FROST -> {
                mesh.shard(position, visual.direction, facing, core * 4.4, core * 0.62,
                        1.18F, 0.56F);
                for (int i = 0; i < 4; i++) {
                    Vec3 crystal = position.add(facing.point(i * Math.PI / 2.0 + age,
                            core * 0.82));
                    mesh.shard(crystal, visual.direction, facing, core * 1.25,
                            core * 0.18, 1.08F, 0.34F);
                }
            }
            case WIND -> {
                mesh.disc(facing, position, core * 1.30, 30, 0.92F, 0.24F);
                mesh.band(facing, position, core * 0.72, core * 1.18,
                        34, 1.15F, 0.34F);
                mesh.brokenBand(facing, position, core * 1.42, core * 1.55,
                        36, 5, 0.92F, 0.24F);
            }
            case SPACE -> {
                mesh.orb(position, core * 0.82, 24, 0.72F, 0.48F);
                mesh.brokenBand(facing, position, core * 1.08, core * 1.30,
                        36, 4, 1.20F, 0.40F);
                mesh.brokenBand(facing, position, core * 1.48, core * 1.62,
                        42, 6, 0.86F, 0.28F);
            }
            case WARD -> {
                mesh.polygonPlate(facing, position, core * 1.35, 6 + circle / 3,
                        age * 1.8, 1.08F, 0.42F);
                mesh.band(facing, position, core * 0.75, core * 1.10,
                        34, 1.24F, 0.32F);
            }
            case LIFE -> {
                mesh.orb(position, core, 24, 1.18F, 0.44F);
                mesh.starPlate(facing, position, core * 1.10, core * 0.38,
                        4, age * 1.4, 1.30F, 0.38F);
            }
            default -> {
                mesh.orb(position, core * 0.72, 22, 0.92F, 0.42F);
                mesh.shard(position, visual.direction, facing, core * 3.2, core * 0.45,
                        1.18F, 0.46F);
                mesh.starPlate(facing, position, core * 1.12, core * 0.40,
                        5 + circle / 3, age * 2.0, 1.10F, 0.34F);
            }
        }

        if (age > 0.78) {
            double burst = clamp((age - 0.78) / 0.22, 0.0, 1.0);
            Vec3 end = visual.direction.scale(travel);
            mesh.orb(end, core * (0.75 + burst * 1.5), 28,
                    1.20F, (float) (0.34 * (1.0 - burst)));
            mesh.brokenBand(facing, end, core * (0.8 + burst * 1.8),
                    core * (1.0 + burst * 2.0), 44, 5, 1.18F,
                    (float) (0.42 * (1.0 - burst)));
        }
    }

    private static void buildBeam(ArcaneWorldMesh.Builder mesh, Visual visual,
                                  ArcaneWorldMesh.Basis facing, double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        double length = Math.min(72.0, Math.max(4.0, visual.range));
        double reveal = clamp(age / 0.18, 0.0, 1.0);
        double fade = clamp((1.0 - age) / 0.22, 0.0, 1.0);
        double shown = length * reveal;
        double radius = (0.05 + circle * 0.013) * powerFactor;
        float alpha = (float) (0.48 * fade);
        mesh.beamPrism(Vec3.ZERO, visual.direction, facing, shown, radius * 1.8,
                0.78F, alpha * 0.55F);
        mesh.beamPrism(Vec3.ZERO, visual.direction, facing, shown, radius,
                1.25F, alpha);
        int nodes = Math.min(8, 3 + circle);
        for (int i = 1; i <= nodes; i++) {
            Vec3 node = visual.direction.scale(shown * i / (nodes + 1.0));
            mesh.brokenBand(facing, node, radius * 2.2, radius * 3.1,
                    24, 4 + i % 3, 1.08F, alpha * 0.65F);
        }
        mesh.orb(visual.direction.scale(shown), radius * (4.5 + circle * 0.3),
                22, 1.18F, alpha);
    }

    private static void buildWave(ArcaneWorldMesh.Builder mesh, Visual visual,
                                  ArcaneWorldMesh.Basis facing, double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        double length = Math.min(64.0, SpellMetrics.waveLength(visual.range));
        double endRadius = SpellMetrics.waveEndRadius(visual.spell.id(), visual.range, circle);
        double progress = 1.0 - Math.pow(1.0 - age, 1.25);
        double shownLength = Math.max(0.4, length * progress);
        double radius = Math.max(0.2, endRadius * progress);
        mesh.cone(Vec3.ZERO, visual.direction, facing, shownLength, radius,
                8 + circle, 2 + circle / 2, 0.0F);
        int slices = Math.min(6, 3 + circle / 2);
        for (int i = 1; i <= slices; i++) {
            double t = i / (double) slices;
            Vec3 center = visual.direction.scale(shownLength * t);
            double local = radius * t;
            mesh.band(facing, center, local * 0.78, local,
                    30 + circle * 2, 1.05F, (float) (0.22 * (1.0 - age)));
        }
    }

    private static void buildField(ArcaneWorldMesh.Builder mesh, Visual visual,
                                   double age, double powerFactor) {
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        int circle = clampCircle(visual.spell.circle());
        double maxRadius = Math.min(48.0,
                SpellMetrics.effectRadius(visual.spell.id(), visual.range, circle));
        double progress = clamp(age / 0.30, 0.0, 1.0);
        double radius = Math.max(0.35, maxRadius * (0.28 + progress * 0.72));
        double fade = clamp((1.0 - age) / 0.18, 0.0, 1.0);
        mesh.disc(ground, Vec3.ZERO, radius, 56, 0.62F,
                (float) (0.08 * fade));
        mesh.band(ground, Vec3.ZERO, radius * 0.86, radius, 64,
                1.18F, (float) (0.42 * fade));
        mesh.brokenBand(ground, Vec3.ZERO, radius * 0.60, radius * 0.68,
                52, 5 + circle % 3, 0.95F, (float) (0.28 * fade));
        schoolSeal(mesh, visual.spell, ground, radius * 0.47,
                age * 1.5, fade);
    }

    private static void buildAura(ArcaneWorldMesh.Builder mesh, Visual visual,
                                  double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        double radius = (0.92 + circle * 0.16) * powerFactor;
        double pulse = 0.88 + Math.sin(age * Math.PI * 5.0) * 0.08;
        double fade = clamp((1.0 - age) / 0.18, 0.0, 1.0);
        mesh.orb(new Vec3(0.0, -0.72, 0.0), radius * pulse, 26,
                1.0F, (float) (0.24 * fade));
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        for (int i = 0; i < 3; i++) {
            double y = -0.86 + i * 0.76;
            double ring = radius * (0.62 + i * 0.12);
            mesh.brokenBand(ground, new Vec3(0.0, y, 0.0), ring * 0.88, ring,
                    38, 4 + i, 1.12F, (float) (0.30 * fade));
        }
    }

    private static void buildTargetBurst(ArcaneWorldMesh.Builder mesh, Visual visual,
                                         double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double radius = (0.62 + circle * 0.14) * powerFactor;
        double expand = 0.45 + age * 1.55;
        double fade = 1.0 - age;
        mesh.orb(new Vec3(0.0, 0.20, 0.0), radius * expand, 24,
                1.15F, (float) (0.34 * fade));
        int layers = 3 + circle / 2;
        for (int i = 0; i < layers; i++) {
            double t = i / (double) Math.max(1, layers - 1);
            double y = -0.9 + t * (2.0 + circle * 0.10);
            double ring = radius * expand * (0.72 + Math.sin(Math.PI * t) * 0.18);
            mesh.brokenBand(ground, new Vec3(0.0, y, 0.0), ring * 0.82, ring,
                    36, 4 + i, 1.08F, (float) (0.30 * fade));
        }
    }

    private static void schoolSeal(ArcaneWorldMesh.Builder mesh, SpellDefinition spell,
                                   ArcaneWorldMesh.Basis basis, double radius,
                                   double rotation, double opacity) {
        float alpha = (float) (0.16 + 0.20 * clamp(opacity, 0.0, 1.0));
        switch (spell.school()) {
            case FIRE -> {
                mesh.starPlate(basis, Vec3.ZERO, radius, radius * 0.38,
                        3 + spell.circle() / 3, rotation, 1.16F, alpha);
                mesh.polygonPlate(basis, Vec3.ZERO, radius * 0.43, 3,
                        -rotation, 0.82F, alpha * 0.65F);
            }
            case FROST -> {
                int arms = 6 + (spell.circle() >= 7 ? 2 : 0);
                for (int i = 0; i < arms && !mesh.full(); i++) {
                    double angle = rotation + Math.PI * 2.0 * i / arms;
                    Vec3 tip = basis.point(angle, radius);
                    mesh.shard(tip.scale(0.55), tip, basis,
                            radius * 0.72, radius * 0.085, 1.14F, alpha);
                }
            }
            case WIND -> {
                for (int i = 0; i < 3; i++) {
                    Vec3 node = basis.point(rotation + i * 2.09, radius * 0.58);
                    mesh.disc(basis, node, radius * 0.34, 22,
                            0.88F + i * 0.08F, alpha * 0.72F);
                }
                mesh.brokenBand(basis, Vec3.ZERO, radius * 0.72, radius,
                        40, 5, 1.12F, alpha);
            }
            case WARD -> {
                mesh.polygonPlate(basis, Vec3.ZERO, radius, 6 + spell.circle() / 4,
                        rotation, 0.96F, alpha);
                mesh.band(basis, Vec3.ZERO, radius * 0.68, radius * 0.78,
                        38, 1.20F, alpha);
            }
            case LIFE -> {
                mesh.disc(basis, basis.right().scale(radius * 0.20),
                        radius * 0.58, 24, 0.92F, alpha * 0.65F);
                mesh.disc(basis, basis.right().scale(-radius * 0.20),
                        radius * 0.58, 24, 1.08F, alpha * 0.65F);
                mesh.starPlate(basis, Vec3.ZERO, radius * 0.42, radius * 0.16,
                        4, rotation, 1.24F, alpha);
            }
            case SPACE -> {
                mesh.brokenBand(basis, basis.right().scale(radius * 0.10),
                        radius * 0.58, radius * 0.76, 42, 5, 1.16F, alpha);
                mesh.brokenBand(basis, basis.right().scale(-radius * 0.10),
                        radius * 0.38, radius * 0.54, 34, 4, 0.78F, alpha * 0.72F);
            }
            default -> {
                mesh.polygonPlate(basis, Vec3.ZERO, radius, 4 + spell.circle() / 2,
                        rotation, 0.88F, alpha);
                mesh.diamond(basis, Vec3.ZERO, radius * 0.30,
                        -rotation, 1.18F, alpha);
            }
        }
    }

    private static ArcaneWorldMesh.Basis basis(SpellDefinition spell, Vec3 direction) {
        return switch (spell.sigilAnchor()) {
            case FEET, GROUND_SELF, GROUND_TARGET -> ArcaneWorldMesh.Basis.ground();
            default -> ArcaneWorldMesh.Basis.facing(direction);
        };
    }

    private static int color(SpellDefinition spell) {
        return switch (spell.school()) {
            case FIRE -> 0xEFFF633D;
            case FROST -> 0xEF69E4FF;
            case WIND -> 0xE873E8C2;
            case WARD -> 0xEFC89AFF;
            case LIFE -> 0xEF74E894;
            case SPACE -> 0xEFA778FF;
            default -> 0xEF829FFF;
        };
    }

    private static int clampCircle(int circle) {
        return Math.max(1, Math.min(9, circle));
    }

    private static Map<String, String> parse(String state) {
        Map<String, String> result = new HashMap<>();
        for (String part : state.split(";")) {
            int split = part.indexOf('=');
            if (split > 0) result.put(part.substring(0, split), part.substring(split + 1));
        }
        return result;
    }

    private static int integer(Map<String, String> values, String key, int fallback) {
        try {
            return Integer.parseInt(values.getOrDefault(key, Integer.toString(fallback)));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double decimal(Map<String, String> values, String key, double fallback) {
        try {
            return Double.parseDouble(values.getOrDefault(key, Double.toString(fallback)));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static Vec3 safeDirection(Vec3 value) {
        return value == null || value.lengthSqr() < 1.0E-8
                ? new Vec3(0.0, 0.0, 1.0) : value.normalize();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Visual(UUID caster, SpellDefinition spell, boolean fusion, int ingredients,
                          Vec3 center, Vec3 direction, double range, double power, double progress,
                          long startedAt, long expiresAt, boolean release) {}
    private record RenderEntry(Vec3 center, ArcaneWorldMesh mesh, int argb) {}
}
