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
    private static final int MAX_CHARGE_GEOMETRY = 3200;
    private static final int MAX_RELEASE_GEOMETRY = 2200;
    private static final int MAX_VISUALS = 14;
    private static final int MAX_FRAME = 18000;
    private static final double MAX_DISTANCE_SQR = 192.0 * 192.0;
    private static final long CHARGE_TTL = 2_250_000_000L;
    private static final double[] GRAND_ARRAY_RADIUS = {0.0, 0.68, 0.82, 0.98, 1.18, 1.45, 1.82, 2.30, 2.92, 3.72};

    private static final Set<String> TRUE_BEAMS = Set.of(
            "ray_of_frost", "scorching_ray", "lightning_bolt", "disintegrate",
            "sunbeam", "chain_lightning", "prismatic_spray");
    private static final Set<String> WAVES = Set.of(
            "thunderwave", "gust_of_wind", "burning_hands", "cone_of_cold",
            "shatter", "steam_burst", "world_sunder", "flame_wave", "wind_blade");
    private static final Set<String> METEOR_FORMS = Set.of(
            "flame_strike", "delayed_blast_fireball", "fire_storm", "meteor_swarm", "sunburst");
    private static final Set<String> PORTAL_FORMS = Set.of(
            "misty_step", "blink", "dimension_door", "passwall", "plane_shift",
            "teleport", "demiplane", "gate");
    private static final Set<String> PRISON_FORMS = Set.of(
            "hold_person", "hold_monster", "resilient_sphere", "forcecage",
            "astral_prison", "maze");
    private static final Set<String> WALL_FORMS = Set.of(
            "wall_of_fire", "wind_wall", "wall_of_ice", "wall_of_force", "prismatic_wall");
    private static final Set<String> STORM_FORMS = Set.of(
            "sleet_storm", "ice_storm", "cloudkill", "insect_plague", "control_weather",
            "incendiary_cloud", "winter_domain");
    private static final Set<String> ORB_FORMS = Set.of(
            "fireball", "chromatic_orb", "ice_knife", "freezing_sphere",
            "delayed_blast_fireball", "solar_guard");
    private static final Set<String> LANCE_FORMS = Set.of(
            "fire_bolt", "void_lance", "finger_of_death", "arcane_hand");

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
        double p = Math.max(0.04, visual.progress);
        double time = Math.max(0.0, (System.nanoTime() - visual.startedAt) / 1_000_000_000.0);
        double rangeScale = clamp(Math.pow(Math.max(0.25,
                visual.range / Math.max(4.0, spell.range())), 0.08), 0.92, 1.12);
        double outer = GRAND_ARRAY_RADIUS[circle] * rangeScale * (visual.fusion ? 1.16 : 1.0);
        double rotation = Math.floorMod(spell.id().hashCode(), 360) * Math.PI / 180.0
                + time * (0.30 + circle * 0.045);
        double counter = -rotation * (0.72 + circle * 0.018);
        double pulse = 0.965 + Math.sin(time * (2.4 + circle * 0.12)) * 0.035;
        outer *= pulse;

        // A dark translucent plate gives the saturated circuitry enough contrast against bright
        // terrain. It stays subtle; the emissive-looking edges carry the spectacle.
        mesh.disc(basis, Vec3.ZERO, outer * (0.94 + p * 0.04), 52 + circle * 7,
                0.30F, (float) (0.035 + p * 0.075));

        // Circle count remains semantically readable: a 1C spell has one complete circuit and a
        // 9C spell has nine, but radius and line density grow much faster after 5C.
        for (int layer = 1; layer <= circle && !mesh.full(); layer++) {
            double t = layer / (double) circle;
            double radius = outer * (0.15 + 0.80 * t);
            double thickness = outer * (0.012 + (layer % 3) * 0.007 + circle * 0.0016);
            float brightness = (float) (0.90 + 0.30 * t + (layer % 2) * 0.09);
            float alpha = (float) ((0.20 + 0.30 * p) * (0.76 + 0.24 * t));
            mesh.band(basis, Vec3.ZERO, Math.max(0.01, radius - thickness), radius,
                    48 + layer * 6, brightness, alpha);
        }

        // Outer crown and school seal establish the main silhouette before small runes appear.
        mesh.brokenBand(basis, Vec3.ZERO, outer * 0.965, outer * 1.015,
                68 + circle * 7, 5 + circle % 4, 1.24F, (float) (0.28 + p * 0.24));
        double sealRadius = outer * (0.31 + Math.min(0.10, circle * 0.011));
        schoolSeal(mesh, spell, basis, sealRadius, rotation, Math.min(1.0, p * 1.22));

        if (circle >= 2) {
            mesh.runeChords(basis, Vec3.ZERO, outer * 0.49,
                    6 + circle * 2, 2 + circle % 4, counter,
                    (float) (0.72 + circle * 0.045));
        }
        if (circle >= 3) {
            int nodes = 3 + circle / 2;
            for (int i = 0; i < nodes && !mesh.full(); i++) {
                double angle = rotation * 0.62 + Math.PI * 2.0 * i / nodes;
                Vec3 node = basis.point(angle, outer * 0.70);
                double r = outer * (0.035 + circle * 0.0025);
                mesh.diamond(basis, node, r * 1.55, -angle, 1.30F, 0.48F);
                mesh.line(node, basis.point(angle, outer * 0.86), 0.84F);
            }
        }
        if (circle >= 4) {
            mesh.starPlate(basis, Vec3.ZERO, outer * 0.61, outer * 0.39,
                    4 + circle / 2, counter, 0.72F, (float) (0.10 + p * 0.13));
            mesh.star(basis, Vec3.ZERO, outer * 0.76, outer * 0.57,
                    5 + circle / 2, rotation * 0.48, 1.12F);
        }

        int glyphs = Math.min(28, 5 + circle * 3);
        int visible = Math.max(1, (int) Math.ceil(glyphs * Math.min(1.0, p * 1.08)));
        for (int i = 0; i < visible && !mesh.full(); i++) {
            double angle = counter + Math.PI * 2.0 * i / glyphs;
            Vec3 glyph = basis.point(angle, outer * (0.79 + (i % 2) * 0.045));
            double size = outer * (0.022 + (i % 4) * 0.005);
            if ((i % 4) == 0) mesh.diamond(basis, glyph, size * 1.48, -angle, 1.34F, 0.52F);
            else if ((i % 4) == 1) mesh.disc(basis, glyph, size, 14, 1.18F, 0.42F);
            else if ((i % 4) == 2) mesh.polygonPlate(basis, glyph, size * 1.24, 3, angle, 1.24F, 0.44F);
            else mesh.starPlate(basis, glyph, size * 1.38, size * 0.48, 4, -angle, 1.28F, 0.42F);
        }

        // 5C is the first visibly "advanced" array: a second counter-rotating script band and
        // radial circuit spokes appear, rather than only adding another ring.
        if (circle >= 5) {
            mesh.brokenBand(basis, Vec3.ZERO, outer * 0.57, outer * 0.615,
                    72 + circle * 6, 4, 1.12F, (float) (0.23 + p * 0.22));
            int spokes = 8 + circle * 2;
            for (int i = 0; i < spokes && !mesh.full(); i++) {
                double angle = rotation * 0.38 + Math.PI * 2.0 * i / spokes;
                Vec3 a = basis.point(angle, outer * (0.34 + (i % 3) * 0.035));
                Vec3 b = basis.point(angle + (i % 2 == 0 ? 0.045 : -0.045), outer * 0.54);
                mesh.line(a, b, i % 4 == 0 ? 1.34F : 0.72F);
            }
        }

        // 6C+ breaks out of the flat plane. Multiple gyroscopic circuits make the jump in mage
        // tier readable even from the side and prevent high circles from looking like scaled 1C art.
        if (circle >= 6) {
            Vec3 normal = basis.normal();
            ArcaneWorldMesh.Basis tiltA = ArcaneWorldMesh.Basis.fromNormal(
                    basis.right().add(normal.scale(0.66)), basis.up());
            ArcaneWorldMesh.Basis tiltB = ArcaneWorldMesh.Basis.fromNormal(
                    basis.up().add(normal.scale(0.58)), basis.right());
            mesh.brokenBand(tiltA, Vec3.ZERO, outer * 0.79, outer * 0.86,
                    72 + circle * 5, 5, 1.24F, (float) (0.24 + p * 0.22));
            mesh.brokenBand(tiltB, Vec3.ZERO, outer * 0.63, outer * 0.70,
                    66 + circle * 5, 6, 1.02F, (float) (0.20 + p * 0.19));
            int orbiters = 4 + circle / 2;
            for (int i = 0; i < orbiters && !mesh.full(); i++) {
                double angle = -time * (0.52 + circle * 0.03) + Math.PI * 2.0 * i / orbiters;
                Vec3 node = tiltA.point(angle, outer * 0.94);
                mesh.orb(node, outer * 0.035, 16, 1.24F, 0.48F);
            }
        }

        if (circle >= 7) {
            Vec3 normal = basis.normal();
            ArcaneWorldMesh.Basis crown = ArcaneWorldMesh.Basis.fromNormal(
                    basis.right().add(basis.up()).add(normal.scale(0.82)), basis.up());
            mesh.brokenBand(crown, Vec3.ZERO, outer * 1.05, outer * 1.10,
                    84 + circle * 6, 7, 1.18F, (float) (0.18 + p * 0.20));
            mesh.star(crown, Vec3.ZERO, outer * 0.47, outer * 0.28,
                    7, time * 0.62, 1.05F);
        }

        // 8C receives orbiting sub-arrays rather than just more line density.
        if (circle >= 8) {
            int satellites = circle == 9 ? 9 : 6;
            for (int i = 0; i < satellites && !mesh.full(); i++) {
                double angle = rotation * 0.34 + Math.PI * 2.0 * i / satellites;
                Vec3 node = basis.point(angle, outer * 1.18);
                double sub = outer * (circle == 9 ? 0.105 : 0.090);
                mesh.disc(basis, node, sub, 22, 0.54F, 0.16F);
                mesh.band(basis, node, sub * 0.68, sub, 30, 1.34F, 0.50F);
                mesh.star(basis, node, sub * 0.62, sub * 0.26,
                        circle == 9 ? 5 : 4, -counter + i, 0.94F);
            }
            mesh.orb(Vec3.ZERO, outer * (circle == 9 ? 0.34 : 0.27),
                    32 + circle * 3, 0.86F, (float) (0.10 + p * 0.12));
        }

        // 9C grand array: three mutually tilted crowns, nine satellite sigils and a dense central
        // seal. This is deliberately several times the physical size and geometry budget of 1C.
        if (circle == 9) {
            Vec3 normal = basis.normal();
            ArcaneWorldMesh.Basis axisA = ArcaneWorldMesh.Basis.fromNormal(
                    basis.right().add(normal.scale(0.92)), basis.up());
            ArcaneWorldMesh.Basis axisB = ArcaneWorldMesh.Basis.fromNormal(
                    basis.up().add(normal.scale(0.88)), basis.right());
            ArcaneWorldMesh.Basis axisC = ArcaneWorldMesh.Basis.fromNormal(
                    basis.right().subtract(basis.up()).add(normal.scale(0.74)), basis.up());
            mesh.brokenBand(axisA, Vec3.ZERO, outer * 1.24, outer * 1.30, 104, 8, 1.32F, 0.46F);
            mesh.brokenBand(axisB, Vec3.ZERO, outer * 1.10, outer * 1.16, 96, 7, 1.18F, 0.40F);
            mesh.brokenBand(axisC, Vec3.ZERO, outer * 0.91, outer * 0.97, 92, 6, 1.08F, 0.36F);
            mesh.starPlate(basis, Vec3.ZERO, outer * 0.29, outer * 0.105,
                    9, -time * 0.86, 1.36F, (float) (0.24 + p * 0.22));
            for (int i = 0; i < 9 && !mesh.full(); i++) {
                double angle = time * 0.24 + Math.PI * 2.0 * i / 9.0;
                Vec3 outerNode = basis.point(angle, outer * 1.38);
                Vec3 innerNode = basis.point(angle + (i % 2 == 0 ? 0.08 : -0.08), outer * 0.92);
                mesh.line(innerNode, outerNode, i % 3 == 0 ? 1.62F : 0.92F);
                mesh.diamond(basis, outerNode, outer * 0.037, angle, 1.42F, 0.56F);
            }
        }

        if (visual.fusion && visual.ingredients >= 2) {
            int count = Math.min(3, visual.ingredients);
            for (int i = 0; i < count && !mesh.full(); i++) {
                double angle = -rotation + Math.PI * 2.0 * i / count;
                Vec3 node = basis.point(angle, outer * 1.48);
                double radius = outer * (0.10 + count * 0.010);
                mesh.disc(basis, node, radius, 24, 0.62F, 0.16F);
                mesh.band(basis, node, radius * 0.64, radius, 34, 1.38F, 0.54F);
                mesh.star(basis, node, radius * 0.62, radius * 0.24,
                        3 + i, rotation + i, 1.08F);
            }
            mesh.brokenBand(basis, Vec3.ZERO, outer * 1.53, outer * 1.59,
                    88, 7, 1.30F, 0.42F);
        }
        return mesh.build();
    }

    private static ArcaneWorldMesh buildRelease(Visual visual, double age) {
        ArcaneWorldMesh.Builder mesh = ArcaneWorldMesh.builder(MAX_RELEASE_GEOMETRY);
        SpellDefinition spell = visual.spell;
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(visual.direction);
        int circle = clampCircle(spell.circle());
        double powerFactor = clamp(Math.pow(Math.max(0.08,
                visual.power / Math.max(1.0, spell.power())), 0.18), 0.82, 2.0)
                * spectacleScale(circle);
        String id = spell.id();

        // Spell identity decides the body first. Anchor is only a fallback, so a portal, wall,
        // meteor and prison can no longer collapse into the same generic ground burst.
        if (METEOR_FORMS.contains(id)) buildMeteor(mesh, visual, age, powerFactor);
        else if (PORTAL_FORMS.contains(id)) buildPortal(mesh, visual, age, powerFactor);
        else if (PRISON_FORMS.contains(id)) buildPrison(mesh, visual, age, powerFactor);
        else if (WALL_FORMS.contains(id)) buildWall(mesh, visual, age, powerFactor);
        else if (STORM_FORMS.contains(id)) buildStorm(mesh, visual, age, powerFactor);
        else if (TRUE_BEAMS.contains(id)) buildBeam(mesh, visual, facing, age, powerFactor);
        else if (WAVES.contains(id)) buildWave(mesh, visual, facing, age, powerFactor);
        else if ("magic_missile".equals(id)) buildMissileSwarm(mesh, visual, facing, age, powerFactor);
        else if (ORB_FORMS.contains(id)) buildElementalOrb(mesh, visual, facing, age, powerFactor);
        else if (LANCE_FORMS.contains(id)) buildLance(mesh, visual, facing, age, powerFactor);
        else {
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
        addReleaseCrown(mesh, visual, age);
        return mesh.build();
    }

    private static void addReleaseCrown(ArcaneWorldMesh.Builder mesh, Visual visual, double age) {
        int circle = clampCircle(visual.spell.circle());
        if (circle < 6) return;
        double fade = clamp((1.0 - age) / 0.36, 0.0, 1.0);
        if (fade <= 0.0) return;
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(visual.direction);
        double base = GRAND_ARRAY_RADIUS[circle] * (0.54 + (circle - 6) * 0.045);
        double rot = (System.nanoTime() - visual.startedAt) / 1_000_000_000.0 * 0.74;
        mesh.brokenBand(facing, Vec3.ZERO, base * 0.83, base, 68 + circle * 5,
                5 + circle % 3, 1.30F, (float) (0.42 * fade));
        mesh.star(facing, Vec3.ZERO, base * 0.66, base * 0.42,
                5 + circle / 2, rot, 1.22F);
        if (circle >= 8) {
            ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(
                    facing.right().add(visual.direction.scale(0.70)), facing.up());
            mesh.brokenBand(tilt, Vec3.ZERO, base * 0.72, base * 0.80,
                    72, 6, 1.14F, (float) (0.32 * fade));
        }
        if (circle == 9) {
            ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(
                    facing.up().add(visual.direction.scale(0.78)), facing.right());
            mesh.brokenBand(tilt, Vec3.ZERO, base * 0.95, base * 1.03,
                    84, 7, 1.28F, (float) (0.36 * fade));
        }
    }

    private static double spectacleScale(int circle) {
        return switch (clampCircle(circle)) {
            case 1, 2, 3 -> 1.0;
            case 4 -> 1.04;
            case 5 -> 1.10;
            case 6 -> 1.18;
            case 7 -> 1.29;
            case 8 -> 1.43;
            case 9 -> 1.62;
            default -> 1.0;
        };
    }

    private static void buildProjectile(ArcaneWorldMesh.Builder mesh, Visual visual,
                                        ArcaneWorldMesh.Basis facing, double age, double powerFactor) {
        SpellDefinition spell = visual.spell;
        int circle = clampCircle(spell.circle());
        double travel = Math.min(72.0, Math.max(3.0, visual.range));
        double eased = 1.0 - Math.pow(1.0 - age, 1.35);
        Vec3 position = visual.direction.scale(travel * eased);
        double core = (0.17 + circle * 0.052) * powerFactor;

        // Detached echoes communicate speed without turning every projectile into a continuous line.
        int echoes = Math.min(4, 2 + circle / 3);
        for (int i = 1; i <= echoes; i++) {
            double back = core * (1.1 + i * 1.18) + travel * 0.018 * i;
            Vec3 echo = position.subtract(visual.direction.scale(back))
                    .add(facing.point(age * 5.2 + i * 2.17, core * 0.16 * i));
            double radius = core * (0.42 - i * 0.055);
            if (radius > 0.04) mesh.orb(echo, radius, 12, 0.68F, 0.13F);
        }

        switch (spell.school()) {
            case FIRE -> {
                mesh.shard(position, visual.direction, facing, core * 3.5, core * 0.52,
                        1.20F, 0.52F);
                mesh.orb(position.add(visual.direction.scale(core * 0.45)), core * 0.72,
                        22, 1.28F, 0.42F);
                mesh.ribbon(position.subtract(visual.direction.scale(core * 2.8)), visual.direction,
                        facing, core * 3.2, core * 0.92, 2, 18, 1.08F, 0.24F);
            }
            case FROST -> {
                mesh.shard(position, visual.direction, facing, core * 4.8, core * 0.68,
                        1.18F, 0.60F);
                for (int i = 0; i < 4; i++) {
                    Vec3 crystal = position.add(facing.point(i * Math.PI / 2.0 + age,
                            core * 0.82));
                    mesh.shard(crystal, visual.direction, facing, core * 1.30,
                            core * 0.17, 1.05F, 0.36F);
                }
            }
            case WIND -> {
                mesh.ribbon(position.subtract(visual.direction.scale(core * 2.2)), visual.direction,
                        facing, core * 4.0, core * 1.4, 3, 24, 0.94F, 0.25F);
                mesh.brokenBand(facing, position, core * 0.82, core * 1.24,
                        38, 5, 1.12F, 0.34F);
            }
            case SPACE -> {
                mesh.orb(position, core * 0.76, 26, 0.66F, 0.48F);
                mesh.brokenBand(facing, position, core * 1.05, core * 1.28,
                        40, 4, 1.22F, 0.42F);
                ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(
                        facing.right().add(visual.direction.scale(0.6)), facing.up());
                mesh.brokenBand(tilt, position, core * 1.34, core * 1.48,
                        42, 6, 0.84F, 0.30F);
            }
            case WARD -> {
                mesh.polygonPlate(facing, position, core * 1.38, 6 + circle / 3,
                        age * 1.8, 1.08F, 0.40F);
                mesh.band(facing, position, core * 0.74, core * 1.10,
                        34, 1.24F, 0.30F);
            }
            case LIFE -> {
                mesh.orb(position, core, 24, 1.18F, 0.44F);
                mesh.starPlate(facing, position, core * 1.10, core * 0.38,
                        4, age * 1.4, 1.30F, 0.38F);
            }
            default -> {
                mesh.shard(position, visual.direction, facing, core * 3.5, core * 0.50,
                        1.18F, 0.48F);
                mesh.starPlate(facing, position, core * 1.10, core * 0.38,
                        5 + circle / 3, age * 2.0, 1.08F, 0.34F);
            }
        }

        if (age > 0.78) {
            double burst = clamp((age - 0.78) / 0.22, 0.0, 1.0);
            Vec3 end = visual.direction.scale(travel);
            mesh.orb(end, core * (0.72 + burst * 1.55), 28,
                    1.20F, (float) (0.34 * (1.0 - burst)));
            mesh.brokenBand(facing, end, core * (0.82 + burst * 1.85),
                    core * (1.02 + burst * 2.05), 44, 5, 1.18F,
                    (float) (0.42 * (1.0 - burst)));
        }
    }

    private static void buildMissileSwarm(ArcaneWorldMesh.Builder mesh, Visual visual,
                                          ArcaneWorldMesh.Basis facing, double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        int count = Math.min(7, 3 + circle / 2);
        double travel = Math.min(72.0, Math.max(4.0, visual.range));
        double eased = 1.0 - Math.pow(1.0 - age, 1.45);
        double body = (0.12 + circle * 0.028) * powerFactor;
        for (int i = 0; i < count && !mesh.full(); i++) {
            double phase = Math.PI * 2.0 * i / count + age * (2.8 + i * 0.08);
            double spread = body * (1.3 + (i % 2) * 0.38) * Math.sin(Math.PI * age);
            Vec3 position = visual.direction.scale(travel * eased - i * body * 0.55)
                    .add(facing.point(phase, spread));
            mesh.shard(position, visual.direction, facing, body * 3.4, body * 0.32,
                    1.22F, 0.54F);
            mesh.orb(position, body * 0.52, 16, 1.32F, 0.42F);
        }
        if (age > 0.80) {
            double burst = clamp((age - 0.80) / 0.20, 0.0, 1.0);
            Vec3 end = visual.direction.scale(travel);
            mesh.starPlate(facing, end, body * (2.2 + burst * 3.8),
                    body * (0.8 + burst), 7, age * 2.0, 1.28F,
                    (float) (0.44 * (1.0 - burst)));
        }
    }

    private static void buildElementalOrb(ArcaneWorldMesh.Builder mesh, Visual visual,
                                          ArcaneWorldMesh.Basis facing, double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        double travel = Math.min(72.0, Math.max(4.0, visual.range));
        double eased = 1.0 - Math.pow(1.0 - age, 1.28);
        Vec3 position = visual.direction.scale(travel * eased);
        double radius = (0.26 + circle * 0.075) * powerFactor;
        double pulse = 0.92 + Math.sin(age * Math.PI * 8.0) * 0.08;
        mesh.orb(position, radius * pulse, 30 + circle * 2, 1.18F, 0.52F);
        mesh.brokenBand(facing, position, radius * 1.08, radius * 1.28,
                46, 5, 1.26F, 0.42F);
        ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(
                facing.up().add(visual.direction.scale(0.58)), facing.right());
        mesh.brokenBand(tilt, position, radius * 1.30, radius * 1.47,
                48, 6, 0.88F, 0.30F);
        for (int i = 1; i <= 3; i++) {
            Vec3 echo = position.subtract(visual.direction.scale(radius * (1.8 + i * 1.45)));
            mesh.orb(echo, radius * (0.42 - i * 0.07), 14, 0.72F, 0.14F);
        }
        if (age > 0.72) {
            double burst = clamp((age - 0.72) / 0.28, 0.0, 1.0);
            Vec3 end = visual.direction.scale(travel);
            mesh.orb(end, radius * (1.0 + burst * 2.8), 34, 1.20F,
                    (float) (0.48 * (1.0 - burst)));
            mesh.band(facing, end, radius * (1.2 + burst * 2.4),
                    radius * (1.5 + burst * 2.9), 58, 1.24F,
                    (float) (0.40 * (1.0 - burst)));
        }
    }

    private static void buildLance(ArcaneWorldMesh.Builder mesh, Visual visual,
                                   ArcaneWorldMesh.Basis facing, double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        double travel = Math.min(72.0, Math.max(4.0, visual.range));
        double eased = 1.0 - Math.pow(1.0 - age, 1.45);
        Vec3 position = visual.direction.scale(travel * eased);
        double width = (0.10 + circle * 0.026) * powerFactor;
        double length = width * (6.0 + circle * 0.45);
        mesh.shard(position, visual.direction, facing, length, width, 1.24F, 0.62F);
        for (int i = 0; i < 3; i++) {
            Vec3 fin = position.subtract(visual.direction.scale(length * 0.18))
                    .add(facing.point(age * 5.0 + i * Math.PI * 2.0 / 3.0, width * 1.7));
            mesh.shard(fin, visual.direction, facing, length * 0.34, width * 0.24,
                    0.92F, 0.34F);
        }
        mesh.brokenBand(facing, position, width * 1.45, width * 1.86,
                32, 5, 1.18F, 0.32F);
        if (age > 0.82) {
            double burst = clamp((age - 0.82) / 0.18, 0.0, 1.0);
            Vec3 end = visual.direction.scale(travel);
            mesh.starPlate(facing, end, width * (3.0 + burst * 4.0),
                    width * (1.0 + burst), 5 + circle / 2, age * 2.2,
                    1.24F, (float) (0.46 * (1.0 - burst)));
        }
    }

    private static void buildMeteor(ArcaneWorldMesh.Builder mesh, Visual visual,
                                    double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        int count = "meteor_swarm".equals(visual.spell.id()) ? 4
                : "fire_storm".equals(visual.spell.id()) ? Math.min(6, 2 + circle / 2) : 1;
        double radius = (0.50 + circle * 0.15) * powerFactor;
        double fall = clamp(age / 0.72, 0.0, 1.0);
        for (int i = 0; i < count && !mesh.full(); i++) {
            double angle = Math.PI * 2.0 * i / count + 0.45;
            double spread = count == 1 ? 0.0 : radius * (2.4 + (i % 2) * 0.7);
            Vec3 target = ground.point(angle, spread);
            Vec3 meteor = target.add(ground.point(angle + 1.1, radius * (1.5 - fall)))
                    .add(0.0, (11.0 + circle * 1.2) * (1.0 - fall), 0.0);
            Vec3 descent = target.subtract(meteor).normalize();
            ArcaneWorldMesh.Basis bodyBasis = ArcaneWorldMesh.Basis.facing(descent);
            mesh.orb(meteor, radius * (0.66 + i * 0.05), 28, 1.24F, 0.54F);
            mesh.shard(meteor.subtract(descent.scale(radius * 0.65)), descent, bodyBasis,
                    radius * 3.6, radius * 0.58, 1.12F, 0.42F);
        }
        if (age >= 0.62) {
            double impact = clamp((age - 0.62) / 0.38, 0.0, 1.0);
            double effect = Math.min(48.0,
                    SpellMetrics.effectRadius(visual.spell.id(), visual.range, circle));
            double ring = Math.max(radius, effect * impact);
            mesh.disc(ground, Vec3.ZERO, ring, 64, 0.82F,
                    (float) (0.15 * (1.0 - impact)));
            mesh.band(ground, Vec3.ZERO, ring * 0.82, ring, 72, 1.30F,
                    (float) (0.52 * (1.0 - impact)));
            mesh.orb(new Vec3(0.0, radius * 0.32, 0.0),
                    radius * (0.8 + impact * 3.2), 34, 1.22F,
                    (float) (0.44 * (1.0 - impact)));
        }
    }

    private static void buildStorm(ArcaneWorldMesh.Builder mesh, Visual visual,
                                   double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double effect = Math.min(48.0,
                SpellMetrics.effectRadius(visual.spell.id(), visual.range, circle));
        double radius = Math.max(1.2, effect * (0.35 + 0.65 * clamp(age / 0.24, 0.0, 1.0)));
        double fade = clamp((1.0 - age) / 0.16, 0.0, 1.0);
        mesh.disc(ground, Vec3.ZERO, radius, 64, 0.58F, (float) (0.08 * fade));
        for (int layer = 0; layer < 4 + circle / 2 && !mesh.full(); layer++) {
            double y = 0.18 + layer * (0.48 + circle * 0.035);
            double local = radius * (0.92 - layer * 0.055);
            mesh.brokenBand(ground, new Vec3(0.0, y, 0.0), local * 0.84, local,
                    54, 5 + layer % 3, 1.06F,
                    (float) ((0.19 + layer * 0.018) * fade));
        }
        Vec3 vertical = new Vec3(0.0, 1.0, 0.0);
        mesh.helix(Vec3.ZERO, vertical, ground, 2.8 + circle * 0.38,
                radius * 0.82, 2 + circle / 3, 52 + circle * 4,
                0.76F, true);
        for (int i = 0; i < Math.min(8, 3 + circle / 2); i++) {
            double a = age * 5.0 + Math.PI * 2.0 * i / Math.min(8, 3 + circle / 2);
            Vec3 mote = ground.point(a, radius * (0.35 + (i % 3) * 0.16))
                    .add(0.0, 0.5 + (i % 4) * 0.62, 0.0);
            mesh.orb(mote, 0.12 + circle * 0.018, 12, 1.16F, (float) (0.30 * fade));
        }
    }

    private static void buildPortal(ArcaneWorldMesh.Builder mesh, Visual visual,
                                    double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        ArcaneWorldMesh.Basis vertical = ArcaneWorldMesh.Basis.facing(new Vec3(0.0, 0.0, 1.0));
        double size = (0.86 + circle * 0.24) * powerFactor;
        if ("gate".equals(visual.spell.id())) size *= 2.0;
        double open = Math.sin(Math.min(1.0, age / 0.30) * Math.PI / 2.0);
        double fade = clamp((1.0 - age) / 0.18, 0.0, 1.0);
        Vec3 center = new Vec3(0.0, size * 0.95, 0.0);
        mesh.disc(vertical, center, size * open, 64, 0.48F,
                (float) (0.20 * fade));
        mesh.band(vertical, center, size * 0.78 * open, size * open,
                72, 1.26F, (float) (0.56 * fade));
        mesh.brokenBand(vertical, center, size * 1.08 * open, size * 1.20 * open,
                72, 6, 0.96F, (float) (0.34 * fade));
        ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(
                new Vec3(0.58, 0.0, 1.0), new Vec3(0.0, 1.0, 0.0));
        mesh.brokenBand(tilt, center, size * 0.88 * open, size * 0.98 * open,
                58, 5, 1.12F, (float) (0.26 * fade));
        mesh.starPlate(vertical, center, size * 0.42 * open, size * 0.16 * open,
                6 + circle / 2, age * 1.8, 1.18F, (float) (0.32 * fade));
    }

    private static void buildPrison(ArcaneWorldMesh.Builder mesh, Visual visual,
                                    double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double radius = (0.90 + circle * 0.18) * powerFactor;
        double height = 1.8 + circle * 0.24;
        double appear = clamp(age / 0.22, 0.0, 1.0);
        double fade = clamp((1.0 - age) / 0.18, 0.0, 1.0);
        int levels = 4 + circle / 2;
        for (int level = 0; level < levels && !mesh.full(); level++) {
            double y = height * level / Math.max(1.0, levels - 1.0);
            double local = radius * (0.92 + 0.08 * Math.sin(Math.PI * level / levels));
            mesh.band(ground, new Vec3(0.0, y, 0.0), local * 0.88 * appear,
                    local * appear, 52, 1.14F, (float) (0.36 * fade));
        }
        int sides = 6 + circle / 2;
        for (int side = 0; side < sides && !mesh.full(); side++) {
            double a = Math.PI * 2.0 * side / sides;
            Vec3 lower = ground.point(a, radius * appear);
            Vec3 tangent = ground.point(a + Math.PI / 2.0, radius * 0.11);
            Vec3 upper = lower.add(0.0, height, 0.0);
            mesh.face(lower.subtract(tangent), lower.add(tangent),
                    upper.add(tangent), upper.subtract(tangent),
                    side % 2 == 0 ? 1.16F : 0.88F, (float) (0.20 * fade));
        }
        mesh.polygonPlate(ground, new Vec3(0.0, height, 0.0), radius * appear,
                sides, age * 0.8, 0.82F, (float) (0.18 * fade));
    }

    private static void buildWall(ArcaneWorldMesh.Builder mesh, Visual visual,
                                  double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(visual.direction);
        Vec3 right = facing.right();
        double width = Math.min(40.0,
                Math.max(4.0, SpellMetrics.effectRadius(visual.spell.id(), visual.range, circle) * 2.0));
        double height = (1.7 + circle * 0.34) * powerFactor;
        double appear = clamp(age / 0.24, 0.0, 1.0);
        double fade = clamp((1.0 - age) / 0.18, 0.0, 1.0);
        int panels = Math.min(18, 6 + circle * 2);
        for (int i = 0; i < panels && !mesh.full(); i++) {
            double a = (i / (double) panels - 0.5) * width;
            double b = ((i + 1) / (double) panels - 0.5) * width;
            Vec3 p0 = right.scale(a);
            Vec3 p1 = right.scale(b);
            double crestA = height * appear * (0.88 + 0.12 * Math.sin(i * 1.7));
            double crestB = height * appear * (0.88 + 0.12 * Math.sin((i + 1) * 1.7));
            mesh.face(p0, p1, p1.add(0.0, crestB, 0.0), p0.add(0.0, crestA, 0.0),
                    i % 2 == 0 ? 1.18F : 0.84F, (float) (0.30 * fade));
            if (i % 2 == 0) {
                Vec3 center = right.scale((a + b) * 0.5).add(0.0, (crestA + crestB) * 0.28, 0.0);
                mesh.orb(center, 0.12 + circle * 0.018, 12, 1.24F,
                        (float) (0.30 * fade));
            }
        }
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        mesh.brokenBand(ground, Vec3.ZERO, width * 0.44, width * 0.50,
                64, 7, 1.10F, (float) (0.24 * fade));
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
        // Fully saturated school palette. Geometry alpha is controlled per face, so the base color
        // itself should never start washed out.
        return switch (spell.school()) {
            case FIRE -> 0xFFFF2A08;
            case FROST -> 0xFF18C8FF;
            case WIND -> 0xFF00F0A8;
            case WARD -> 0xFF8A35FF;
            case LIFE -> 0xFF25E85A;
            case SPACE -> 0xFFD51CFF;
            default -> 0xFF3E63FF;
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
