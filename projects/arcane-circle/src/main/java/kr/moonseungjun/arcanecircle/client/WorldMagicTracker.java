package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.SpellMetrics;
import kr.moonseungjun.arcanecircle.magic.SpellPresentationProfile;
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
    private static final int MAX_CHARGE_GEOMETRY = 5200;
    private static final int MAX_RELEASE_GEOMETRY = 7200;
    private static final int MAX_VISUALS = 14;
    private static final int MAX_FRAME = 46000;
    private static final double MAX_DISTANCE_SQR = 192.0 * 192.0;
    private static final long CHARGE_TTL = 2_250_000_000L;

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

    record CasterPoseSnapshot(int family, float progress, boolean release) {}

    static CasterPoseSnapshot castingPose(UUID caster) {
        Visual charge = CHARGES.get(caster);
        if (charge != null) return new CasterPoseSnapshot(castingFamily(charge.spell),
                (float) clamp(charge.progress, 0.0, 1.0), false);
        long now = System.nanoTime();
        for (int i = RELEASES.size() - 1; i >= 0; i--) {
            Visual visual = RELEASES.get(i);
            if (!visual.caster.equals(caster)) continue;
            float age = (float) clamp((now - visual.startedAt) /
                    (double) Math.max(1L, visual.expiresAt - visual.startedAt), 0.0, 1.0);
            return new CasterPoseSnapshot(castingFamily(visual.spell), age, true);
        }
        return new CasterPoseSnapshot(0, 0F, false);
    }

    private static int castingFamily(SpellDefinition spell) {
        SpellPresentationProfile.Profile p = SpellPresentationProfile.profile(spell);
        return switch (p.motion()) {
            case PORTAL -> CastingSilhouetteRenderer.PORTAL;
            case SKY_DROP, STORM -> CastingSilhouetteRenderer.RITUAL;
            case WALL, FIELD -> CastingSilhouetteRenderer.GROUND;
            case AURA, PRISON -> CastingSilhouetteRenderer.WARD;
            case HEAVY_ORB -> CastingSilhouetteRenderer.HEAVY;
            case BEAM, LANCE, DART, BOLT, MISSILE_SWARM -> CastingSilhouetteRenderer.AIM;
            case WAVE, TARGET_BURST, SNAP -> spell.circle() >= 7
                    ? CastingSilhouetteRenderer.RITUAL : CastingSilhouetteRenderer.SNAP;
        };
    }

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
        Vec3 target = new Vec3(decimal(values, "tx", center.x + direction.x * range),
                decimal(values, "ty", center.y + direction.y * range),
                decimal(values, "tz", center.z + direction.z * range));
        double power = Math.max(0.1, decimal(values, "power", Math.max(0.1, spell.power())));
        double progress = clamp(decimal(values, "progress", 1.0), 0.0, 1.0);
        int duration = Math.max(3, integer(values, "duration", 10));
        int impactTicks = Math.max(0, integer(values, "impact", 0));
        double impactAge = clamp(impactTicks / (double) Math.max(1, duration), 0.04, 0.92);
        long now = System.nanoTime();

        if ("charge".equals(kind)) {
            Visual previous = CHARGES.get(caster);
            long started = previous != null && previous.spell.id().equals(spell.id())
                    ? previous.startedAt : now;
            CHARGES.put(caster, new Visual(caster, spell, fusion, ingredients, center, target, direction,
                    range, power, progress, started, now + CHARGE_TTL, false, 0.0));
            return;
        }
        if ("release".equals(kind)) {
            while (RELEASES.size() >= MAX_VISUALS) RELEASES.removeFirst();
            RELEASES.add(new Visual(caster, spell, fusion, ingredients, center, target, direction,
                    range, power, 1.0, now, now + duration * 50_000_000L, true, impactAge));
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
            if (SpellVisualSignature.isPrismatic(visual.spell)) {
                Vec3 targetOffset = targetOffset(visual);
                for (int layer = 0; layer < 7; layer++) {
                    ArcaneWorldMesh accent = SpellVisualSignature.prismaticAccent(
                            visual.spell, visual.direction, targetOffset, visual.range, age, layer);
                    entries.add(new RenderEntry(visual.center, accent,
                            SpellVisualSignature.prismaticColor(layer)));
                }
            }
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
        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(spell);
        ArcaneWorldMesh.Builder mesh = ArcaneWorldMesh.builder(MAX_CHARGE_GEOMETRY);
        ArcaneWorldMesh.Basis basis = presentationBasis(profile, visual.direction);
        int complexity = profile.complexity();
        double p = Math.max(0.0, visual.progress);
        double time = Math.max(0.0, (System.nanoTime() - visual.startedAt) / 1_000_000_000.0);
        double outer = profile.radius() * (visual.fusion ? 1.12 : 1.0)
                * (0.985 + Math.sin(time * 2.2) * 0.015);
        double rotation = Math.floorMod(spell.id().hashCode(), 360) * Math.PI / 180.0 + time * 0.34;
        double counter = -rotation * 0.73;
        double core = sigilPhase(p, 0.00, 0.28);
        double lock = sigilPhase(p, 0.18, 0.62);
        double formula = sigilPhase(p, 0.36, 0.82);
        double release = sigilPhase(p, 0.68, 1.00);

        if (LowCircleVisualIdentity.owns(spell)) {
            LowCircleVisualIdentity.appendCharge(spell, profile, basis, outer, rotation, p, mesh);
            return mesh.build();
        }
        if (MidCircleVisualIdentity.owns(spell)) {
            MidCircleVisualIdentity.appendCharge(spell, profile, outer, rotation, p,
                    visual.direction, targetOffset(visual), mesh);
            return mesh.build();
        }
        if (FifthCircleVisualIdentity.owns(spell)) {
            FifthCircleVisualIdentity.appendCharge(spell, profile, outer, rotation, p,
                    visual.direction, targetOffset(visual), visual.range, mesh);
            return mesh.build();
        }
        if (SixthCircleVisualIdentity.owns(spell)) {
            SixthCircleVisualIdentity.appendCharge(spell, profile, outer, rotation, p,
                    visual.direction, targetOffset(visual), visual.range, mesh);
            return mesh.build();
        }
        if (ArchmageVisualIdentity.owns(spell)) {
            ArchmageVisualIdentity.appendCharge(spell, profile, outer, rotation, p,
                    visual.direction, targetOffset(visual), visual.range, mesh);
            return mesh.build();
        }

        // There is deliberately no universal disc/ring prelude here. Each placement grammar earns
        // its own silhouette so a portal, target curse and sky ritual cannot read as the same circle.
        switch (profile.sigil()) {
            case FRONT_COMPACT -> {
                double r = outer * (0.38 + 0.62 * lock);
                schoolSeal(mesh, spell, basis, outer * (0.20 + 0.16 * core), rotation, core);
                mesh.brokenBand(basis, Vec3.ZERO, r * 0.82, r, 44 + complexity * 7,
                        5, 1.20F, (float) (0.22 + formula * 0.28));
                if (formula > 0.04)
                    mesh.runeRing(basis, Vec3.ZERO, outer * 0.72, 6 + complexity * 3,
                            outer * 0.026, spell.id().hashCode(), counter, 0.78F);
                if (release > 0.05)
                    mesh.star(basis, Vec3.ZERO, outer * 0.54, outer * 0.24,
                            4 + complexity, -rotation * 0.42, 1.10F);
            }
            case FRONT_LANCE -> {
                Vec3 normal = basis.normal();
                schoolSeal(mesh, spell, basis, outer * 0.26, rotation, core);
                int gates = 1 + (int) Math.floor(lock * Math.min(3, 1 + complexity / 2));
                for (int i = 0; i < gates && !mesh.full(); i++) {
                    double z = outer * (-0.12 + i * 0.18);
                    double r = outer * (0.62 - i * 0.09);
                    mesh.brokenBand(basis, normal.scale(z), r * 0.86, r,
                            42 + complexity * 6, 4 + i, 1.18F, 0.34F);
                }
                if (formula > 0.04)
                    mesh.runeChords(basis, Vec3.ZERO, outer * 0.70, 7 + complexity * 2,
                            2 + complexity % 3, counter, 0.86F);
                if (release > 0.02)
                    buildLanceArray(mesh, basis, spell, outer, complexity, rotation, p);
            }
            case GROUND_SEAL -> {
                schoolSeal(mesh, spell, basis, outer * 0.30, rotation, core);
                mesh.polygon(basis, Vec3.ZERO, outer * (0.44 + 0.44 * lock),
                        5 + complexity, rotation * 0.32, 1.04F);
                if (formula > 0.03) {
                    mesh.brokenBand(basis, Vec3.ZERO, outer * 0.72, outer * 0.80,
                            62 + complexity * 6, 6, 1.14F, 0.34F);
                    mesh.runeRing(basis, Vec3.ZERO, outer * 0.60, 8 + complexity * 3,
                            outer * 0.024, spell.id().hashCode(), counter, 0.72F);
                }
            }
            case FEET_RUNE -> {
                mesh.diamond(basis, Vec3.ZERO, outer * (0.34 + lock * 0.34), rotation,
                        1.12F, (float) (0.16 + core * 0.24));
                schoolSeal(mesh, spell, basis, outer * 0.24, -rotation, core);
                if (formula > 0.08)
                    mesh.runeRing(basis, Vec3.ZERO, outer * 0.68, 6 + complexity * 2,
                            outer * 0.024, spell.id().hashCode(), rotation, 0.70F);
            }
            case TARGET_SEAL -> {
                schoolSeal(mesh, spell, basis, outer * 0.28, rotation, core);
                buildTargetArray(mesh, basis, spell, outer, complexity, rotation, p);
                if (release > 0.12)
                    mesh.brokenBand(basis, Vec3.ZERO, outer * 1.02, outer * 1.10,
                            72, 7, 1.24F, 0.40F);
            }
            case BODY_HALO -> {
                schoolSeal(mesh, spell, basis, outer * 0.24, rotation, core);
                if (lock > 0.02) buildHaloArray(mesh, basis, outer, complexity, rotation, p);
                if (formula > 0.08)
                    mesh.runeRing(basis, Vec3.ZERO, outer * 0.76, 7 + complexity * 2,
                            outer * 0.022, spell.id().hashCode(), counter, 0.72F);
            }
            case SKY_RITUAL -> {
                schoolSeal(mesh, spell, basis, outer * 0.22, rotation, core);
                if (lock > 0.02)
                    mesh.polygon(basis, Vec3.ZERO, outer * (0.32 + 0.28 * lock),
                            6 + complexity, rotation * 0.24, 1.04F);
                buildSkyRitualArray(mesh, basis, spell, outer, complexity,
                        Math.max(1, profile.satellites()), rotation, p);
            }
            case QUAD_ARRAY -> {
                schoolSeal(mesh, spell, basis, outer * 0.24, rotation, core);
                if (lock > 0.01)
                    mesh.diamond(basis, Vec3.ZERO, outer * (0.30 + 0.18 * lock),
                            rotation * 0.33, 1.04F, 0.18F);
                buildQuadArray(mesh, basis, spell, outer, complexity, rotation, p);
            }
            case WALL_MATRIX -> {
                schoolSeal(mesh, spell, basis, outer * 0.20, rotation, core);
                if (lock > 0.01)
                    mesh.polygon(basis, Vec3.ZERO, outer * (0.34 + 0.24 * lock),
                            4, Math.PI / 4.0, 1.18F);
                buildQuadArray(mesh, basis, spell, outer * 0.82, complexity, rotation, p);
            }
            case PORTAL_GATE -> {
                schoolSeal(mesh, spell, basis, outer * 0.18, rotation, core);
                if (lock > 0.01) buildPortalArray(mesh, basis, spell, outer, complexity, rotation, p);
            }
        }

        SpellVisualSignature.appendCharge(spell, profile, basis, outer, rotation, p, mesh);
        if (visual.fusion && release > 0.02) {
            mesh.brokenBand(basis, Vec3.ZERO, outer * 1.08, outer * 1.14,
                    72 + complexity * 10, 6, 1.30F, (float) (0.24 + release * 0.24));
        }
        return mesh.build();
    }

    private static double sigilPhase(double progress, double start, double end) {
        if (end <= start) return progress >= end ? 1.0 : 0.0;
        return clamp((progress - start) / (end - start), 0.0, 1.0);
    }

    private static void buildQuadArray(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                                       SpellDefinition spell, double outer, int complexity,
                                       double rotation, double p) {
        double awaken = sigilPhase(p, 0.24, 0.88);
        int active = Math.min(4, (int) Math.ceil(awaken * 4.0));
        double d = outer * (0.56 + 0.22 * awaken);
        Vec3[] nodes = {basis.right().scale(d).add(basis.up().scale(d)),
                basis.right().scale(-d).add(basis.up().scale(d)),
                basis.right().scale(-d).add(basis.up().scale(-d)),
                basis.right().scale(d).add(basis.up().scale(-d))};
        for (int i = 0; i < active && !mesh.full(); i++) {
            Vec3 a = nodes[i];
            if (i > 0) mesh.line(nodes[i - 1], a, 1.22F);
            if (active == 4 && i == 3) mesh.line(a, nodes[0], 1.22F);
            double sub = outer * 0.27 * (0.68 + 0.32 * awaken);
            mesh.band(basis, a, sub * 0.78, sub, 40, 1.28F, (float) (0.26 + p * 0.24));
            mesh.runeRing(basis, a, sub * 0.61, 8 + complexity * 2, sub * 0.055,
                    spell.id().hashCode() + i * 17, -rotation + i, 0.72F);
            schoolSeal(mesh, spell, basis, sub * 0.34, rotation + i, p);
        }
    }

    private static void buildSkyRitualArray(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                                            SpellDefinition spell, double outer, int complexity,
                                            int satellites, double rotation, double p) {
        double outerLock = sigilPhase(p, 0.30, 0.78);
        double runeLock = sigilPhase(p, 0.42, 0.88);
        double satelliteLock = sigilPhase(p, 0.50, 0.98);
        if (outerLock > 0.02)
            mesh.brokenBand(basis, Vec3.ZERO, outer * (0.86 + 0.17 * outerLock),
                    outer * (0.91 + 0.175 * outerLock), 88 + complexity * 14, 7,
                    1.34F, (float) (0.18 + outerLock * 0.34));
        if (runeLock > 0.02)
            mesh.runeRing(basis, Vec3.ZERO, outer * (0.70 + 0.20 * runeLock),
                    16 + complexity * 6, outer * 0.020, spell.id().hashCode() ^ 0x5A17,
                    rotation * 0.42, 0.82F);
        int activeSatellites = Math.min(Math.max(1, satellites),
                (int) Math.ceil(Math.max(0.0, satelliteLock) * Math.max(1, satellites)));
        if (satelliteLock > 0.02)
            buildOrbitingSubArrays(mesh, basis, spell, outer, activeSatellites, rotation, p);

        if (p > 0.62) {
            ArcaneWorldMesh.Basis tiltA = ArcaneWorldMesh.Basis.fromNormal(
                    basis.right().add(basis.normal().scale(0.72)), basis.up());
            mesh.brokenBand(tiltA, Vec3.ZERO, outer * 0.56, outer * 0.61,
                    76, 6, 1.16F, (float) (0.18 + sigilPhase(p, 0.62, 0.90) * 0.24));
        }
        if (complexity >= 5 && p > 0.76) {
            ArcaneWorldMesh.Basis tiltB = ArcaneWorldMesh.Basis.fromNormal(
                    basis.up().add(basis.normal().scale(0.64)), basis.right());
            mesh.brokenBand(tiltB, Vec3.ZERO, outer * 0.42, outer * 0.47,
                    70, 5, 1.10F, (float) (0.16 + sigilPhase(p, 0.76, 1.0) * 0.22));
        }
    }

    private static void buildOrbitingSubArrays(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                                               SpellDefinition spell, double outer, int satellites,
                                               double rotation, double p) {
        int count = Math.max(1, Math.min(12, satellites));
        for (int i = 0; i < count && !mesh.full(); i++) {
            double angle = rotation * 0.36 + Math.PI * 2.0 * i / count;
            Vec3 node = basis.point(angle, outer * 1.22);
            double sub = outer * (count <= 4 ? 0.18 : 0.115);
            mesh.band(basis, node, sub * 0.74, sub, 34, 1.32F, (float) (0.36 + p * 0.18));
            mesh.runeRing(basis, node, sub * 0.55, 6 + Math.min(8, count), sub * 0.06,
                    spell.id().hashCode() + i * 31, -rotation + i, 0.68F);
            mesh.line(basis.point(angle, outer * 0.96), node, i % 3 == 0 ? 1.18F : 0.68F);
        }
    }

    private static void buildLanceArray(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                                        SpellDefinition spell, double outer, int complexity,
                                        double rotation, double p) {
        mesh.star(basis, Vec3.ZERO, outer * 0.82, outer * 0.34,
                4 + complexity, rotation, 1.34F);
        for (int i = 0; i < Math.max(2, complexity - 1); i++) {
            double a = rotation + Math.PI * 2.0 * i / Math.max(2, complexity - 1);
            mesh.line(basis.point(a, outer * 0.18), basis.point(a, outer * 1.04), 1.06F);
        }
        mesh.runeRing(basis, Vec3.ZERO, outer * 0.68, 10 + complexity * 3,
                outer * 0.025, spell.id().hashCode(), -rotation, 0.80F);
    }

    private static void buildPortalArray(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                                         SpellDefinition spell, double outer, int complexity,
                                         double rotation, double p) {
        mesh.band(basis, Vec3.ZERO, outer * 0.77, outer, 72 + complexity * 8,
                1.32F, (float) (0.38 + p * 0.22));
        mesh.brokenBand(basis, Vec3.ZERO, outer * 1.08, outer * 1.15,
                86, 7, 1.18F, 0.38F);
        mesh.runeRing(basis, Vec3.ZERO, outer * 0.88, 18 + complexity * 5,
                outer * 0.022, spell.id().hashCode(), rotation, 0.82F);
    }

    private static void buildHaloArray(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                                       double outer, int complexity, double rotation, double p) {
        ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(
                basis.right().add(basis.normal().scale(0.82)), basis.up());
        mesh.brokenBand(tilt, Vec3.ZERO, outer * 0.72, outer * 0.80,
                68 + complexity * 6, 6, 1.18F, 0.34F);
        mesh.star(basis, Vec3.ZERO, outer * 0.62, outer * 0.26,
                4 + complexity, -rotation, 1.18F);
    }

    private static void buildTargetArray(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                                         SpellDefinition spell, double outer, int complexity,
                                         double rotation, double p) {
        mesh.star(basis, Vec3.ZERO, outer * 0.86, outer * 0.48,
                5 + complexity, rotation * 0.56, 1.34F);
        mesh.runeRing(basis, Vec3.ZERO, outer * 0.64, 12 + complexity * 4,
                outer * 0.026, spell.id().hashCode() ^ 0x77, -rotation, 0.84F);
        if (complexity >= 5)
            mesh.brokenBand(basis, Vec3.ZERO, outer * 1.02, outer * 1.10,
                    82, 6, 1.34F, 0.44F);
    }

    private static ArcaneWorldMesh buildRelease(Visual visual, double age) {
        ArcaneWorldMesh.Builder mesh = ArcaneWorldMesh.builder(MAX_RELEASE_GEOMETRY);
        SpellDefinition spell = visual.spell;
        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(spell);
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(visual.direction);
        double powerFactor = clamp(Math.pow(Math.max(0.08,
                visual.power / Math.max(1.0, spell.power())), 0.18), 0.82, 2.0) * profile.releaseScale();
        if (LowCircleVisualIdentity.owns(spell)) {
            LowCircleVisualIdentity.appendRelease(spell, visual.direction, targetOffset(visual), age,
                    motionProgress(visual, age), powerFactor, mesh);
            RangeReactivePresentation.appendRelease(spell, visual.direction, targetOffset(visual),
                    visual.range, age, powerFactor, mesh);
            return mesh.build();
        }
        if (MidCircleVisualIdentity.owns(spell)) {
            MidCircleVisualIdentity.appendRelease(spell, visual.direction, targetOffset(visual), age,
                    motionProgress(visual, age), powerFactor, mesh);
            RangeReactivePresentation.appendRelease(spell, visual.direction, targetOffset(visual),
                    visual.range, age, powerFactor, mesh);
            return mesh.build();
        }
        if (FifthCircleVisualIdentity.owns(spell)) {
            FifthCircleVisualIdentity.appendRelease(spell, visual.direction, targetOffset(visual), visual.range,
                    age, motionProgress(visual, age), powerFactor, mesh);
            return mesh.build();
        }
        if (SixthCircleVisualIdentity.owns(spell)) {
            SixthCircleVisualIdentity.appendRelease(spell, visual.direction, targetOffset(visual), visual.range,
                    age, motionProgress(visual, age), powerFactor, mesh);
            return mesh.build();
        }
        if (ArchmageVisualIdentity.owns(spell)) {
            ArchmageVisualIdentity.appendRelease(spell, visual.direction, targetOffset(visual), visual.range,
                    age, motionProgress(visual, age), powerFactor, mesh);
            return mesh.build();
        }
        switch (profile.motion()) {
            case MISSILE_SWARM -> buildMissileSwarm(mesh, visual, facing, age, powerFactor);
            case HEAVY_ORB -> buildElementalOrb(mesh, visual, facing, age, powerFactor);
            case DART, BOLT -> buildProjectile(mesh, visual, facing, age, powerFactor);
            case LANCE -> buildLance(mesh, visual, facing, age, powerFactor);
            case BEAM -> buildBeam(mesh, visual, facing, age, powerFactor);
            case WAVE -> buildWave(mesh, visual, facing, age, powerFactor);
            case SKY_DROP -> buildMeteor(mesh, visual, age, powerFactor);
            case STORM -> buildStorm(mesh, visual, age, powerFactor);
            case PORTAL -> buildPortal(mesh, visual, age, powerFactor);
            case PRISON -> buildPrison(mesh, visual, age, powerFactor);
            case WALL -> buildWall(mesh, visual, age, powerFactor);
            case FIELD -> buildField(mesh, visual, age, powerFactor);
            case AURA -> buildAura(mesh, visual, age, powerFactor);
            case TARGET_BURST -> buildTargetBurst(mesh, visual, age, powerFactor);
            case SNAP -> {
                switch (spell.sigilAnchor()) {
                    case FEET, GROUND_SELF, GROUND_TARGET -> buildField(mesh, visual, age, powerFactor);
                    case BODY -> buildAura(mesh, visual, age, powerFactor);
                    case TARGET -> buildTargetBurst(mesh, visual, age, powerFactor);
                    case FRONT -> buildProjectile(mesh, visual, facing, age, powerFactor);
                }
            }
        }
        SpellVisualSignature.appendRelease(spell, visual.direction, targetOffset(visual),
                visual.range, visual.power, age, powerFactor, mesh);
        return mesh.build();
    }

    private static ArcaneWorldMesh.Basis presentationBasis(SpellPresentationProfile.Profile profile, Vec3 direction) {
        return switch (profile.sigil()) {
            case SKY_RITUAL, GROUND_SEAL, QUAD_ARRAY, FEET_RUNE -> ArcaneWorldMesh.Basis.ground();
            case WALL_MATRIX, PORTAL_GATE, TARGET_SEAL, FRONT_COMPACT, FRONT_LANCE -> ArcaneWorldMesh.Basis.facing(direction);
            case BODY_HALO -> ArcaneWorldMesh.Basis.ground();
        };
    }

    private static Vec3 targetOffset(Visual visual) {
        Vec3 delta = visual.target.subtract(visual.center);
        if (delta.lengthSqr() < 1.0E-8) return visual.direction.scale(Math.max(1.0, visual.range));
        return delta;
    }

    private static double travelAge(Visual visual, double age) {
        double impact = visual.impactAge <= 0.0 ? 0.78 : visual.impactAge;
        return clamp(age / Math.max(0.04, impact), 0.0, 1.0);
    }

    private static double motionProgress(Visual visual, double age) {
        double t = travelAge(visual, age);
        return switch (SpellPresentationProfile.profile(visual.spell).motion()) {
            case DART, LANCE -> 1.0 - Math.pow(1.0 - t, 2.35);
            case BOLT -> 1.0 - Math.pow(1.0 - t, 1.72);
            case HEAVY_ORB -> Math.pow(t, 1.12);
            case MISSILE_SWARM -> 1.0 - Math.pow(1.0 - t, 1.55);
            default -> t;
        };
    }

    private static void buildProjectile(ArcaneWorldMesh.Builder mesh, Visual visual,
                                        ArcaneWorldMesh.Basis facing, double age, double powerFactor) {
        SpellDefinition spell = visual.spell;
        int circle = clampCircle(spell.circle());
        Vec3 path = targetOffset(visual);
        double eased = motionProgress(visual, age);
        Vec3 position = path.scale(eased);
        double core = (0.17 + circle * 0.052) * powerFactor;

        // Detached echoes communicate speed without turning every projectile into a continuous line.
        int echoes = Math.min(4, 2 + circle / 3);
        for (int i = 1; i <= echoes; i++) {
            double back = core * (1.1 + i * 1.18) + path.length() * 0.018 * i;
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
            Vec3 end = path;
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
        Vec3 path = targetOffset(visual);
        double body = (0.12 + circle * 0.028) * powerFactor;
        for (int i = 0; i < count && !mesh.full(); i++) {
            double phase = Math.PI * 2.0 * i / count + age * (2.8 + i * 0.08);
            double launchAge = clamp((age - i * 0.025) / 0.22, 0.0, 1.0);
            double spread = body * (1.3 + (i % 2) * 0.38) * Math.sin(Math.PI * age);
            double stagger = clamp((travelAge(visual, age) - i * 0.055) / Math.max(0.35, 1.0 - i * 0.055), 0.0, 1.0);
            Vec3 launch = facing.point(Math.PI * 2.0 * i / count, body * 2.6);
            if (age < 0.30)
                mesh.brokenBand(facing, launch, body * (0.62 + launchAge * 0.28),
                        body * (0.82 + launchAge * 0.34), 24, 4, 1.12F,
                        (float) (0.42 * (1.0 - clamp(age / 0.30, 0.0, 1.0))));
            Vec3 position = launch.scale(1.0 - stagger)
                    .add(path.scale(1.0 - Math.pow(1.0 - stagger, 1.55)))
                    .add(facing.point(phase, spread));
            Vec3 tangent = safeDirection(path.subtract(position).add(facing.point(phase + Math.PI / 2.0, body * 0.8)));
            ArcaneWorldMesh.Basis missileBasis = ArcaneWorldMesh.Basis.facing(tangent);
            mesh.shard(position, tangent, missileBasis, body * 3.4, body * 0.32, 1.22F, 0.54F);
            mesh.orb(position, body * 0.52, 16, 1.32F, 0.42F);
        }
        if (age > 0.80) {
            double burst = clamp((age - 0.80) / 0.20, 0.0, 1.0);
            Vec3 end = path;
            mesh.starPlate(facing, end, body * (2.2 + burst * 3.8),
                    body * (0.8 + burst), 7, age * 2.0, 1.28F,
                    (float) (0.44 * (1.0 - burst)));
        }
    }

    private static void buildElementalOrb(ArcaneWorldMesh.Builder mesh, Visual visual,
                                          ArcaneWorldMesh.Basis facing, double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        Vec3 path = targetOffset(visual);
        double eased = motionProgress(visual, age);
        Vec3 position = path.scale(eased);
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
            Vec3 end = path;
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
        Vec3 path = targetOffset(visual);
        double eased = motionProgress(visual, age);
        Vec3 position = path.scale(eased);
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
            Vec3 end = path;
            mesh.starPlate(facing, end, width * (3.0 + burst * 4.0),
                    width * (1.0 + burst), 5 + circle / 2, age * 2.2,
                    1.24F, (float) (0.46 * (1.0 - burst)));
        }
    }

    private static void buildMeteor(ArcaneWorldMesh.Builder mesh, Visual visual,
                                    double age, double powerFactor) {
        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(visual.spell);
        ArcaneWorldMesh.Basis sky = ArcaneWorldMesh.Basis.ground();
        int count = Math.max(1, profile.satellites());
        if ("meteor_swarm".equals(visual.spell.id())) count = 4;
        Vec3 target = targetOffset(visual);
        double fall = travelAge(visual, age);
        double body = Math.max(0.55, (0.55 + visual.spell.circle() * 0.10) * powerFactor);
        double spread = "meteor_swarm".equals(visual.spell.id()) ? 11.5
                : "fire_storm".equals(visual.spell.id()) ? 6.0 : 0.0;

        double sealFade = clamp((visual.impactAge + 0.14 - age) / 0.20, 0.0, 1.0);
        double sealRadius = profile.radius();
        mesh.brokenBand(sky, Vec3.ZERO, sealRadius * 0.92, sealRadius,
                112, 7, 1.30F, (float) (0.42 * sealFade));
        mesh.runeRing(sky, Vec3.ZERO, sealRadius * 0.82, 24 + profile.complexity() * 5,
                sealRadius * 0.018, visual.spell.id().hashCode(), age * 0.7, 0.84F);

        for (int i = 0; i < count && !mesh.full(); i++) {
            double angle = Math.PI * 2.0 * i / count + Math.PI / 4.0;
            Vec3 impact = target.add(sky.point(angle, count == 1 ? 0.0 : spread));
            Vec3 mouth = sky.point(angle, sealRadius * (count == 1 ? 0.0 : 0.54));
            double local = clamp((fall - i * 0.040) / Math.max(0.52, 1.0 - i * 0.040), 0.0, 1.0);
            double mouthFade = clamp((0.40 - local) / 0.40, 0.0, 1.0);
            double mouthSize = body * (1.35 + i * 0.08);
            if (mouthFade > 0.01) {
                mesh.band(sky, mouth, mouthSize * 0.72, mouthSize, 34, 1.28F, (float) (0.46 * mouthFade));
                mesh.runeRing(sky, mouth, mouthSize * 0.58, 8, mouthSize * 0.055,
                        visual.spell.id().hashCode() + i * 41, -age * 1.6, 0.72F);
            }
            Vec3 meteor = mouth.scale(1.0 - local).add(impact.scale(local));
            Vec3 descent = impact.subtract(mouth);
            Vec3 axis = descent.lengthSqr() < 1.0E-8 ? new Vec3(0.0, -1.0, 0.0) : descent.normalize();
            ArcaneWorldMesh.Basis bodyBasis = ArcaneWorldMesh.Basis.facing(axis);
            mesh.orb(meteor, body * (0.72 + i * 0.04), 34, 1.28F, 0.66F);
            mesh.shard(meteor.subtract(axis.scale(body * 1.2)), axis, bodyBasis,
                    body * 5.4, body * 0.68, 1.18F, 0.52F);
            mesh.ribbon(meteor.subtract(axis.scale(body * 4.2)), axis, bodyBasis,
                    body * 4.4, body * 1.25, 2, 24, 1.08F, 0.34F);

            if (local > 0.62 && age < visual.impactAge) {
                double warning = clamp((local - 0.62) / 0.38, 0.0, 1.0);
                mesh.brokenBand(sky, impact, body * (0.8 + warning * 1.9),
                        body * (1.0 + warning * 2.2), 38, 5, 1.20F,
                        (float) (0.18 + warning * 0.28));
            }
        }

        if (age >= visual.impactAge) {
            double impactAge = clamp((age - visual.impactAge) / Math.max(0.08, 1.0 - visual.impactAge), 0.0, 1.0);
            double effect = Math.min(64.0, SpellMetrics.effectRadius(visual.spell.id(), visual.range,
                    clampCircle(visual.spell.circle())));
            double ring = Math.max(body * 1.6, effect * Math.min(1.0, impactAge * 2.2));
            mesh.band(sky, target, ring * 0.84, ring, 84, 1.34F,
                    (float) (0.62 * (1.0 - impactAge)));
            mesh.orb(target.add(0.0, body * 0.45, 0.0),
                    body * (1.2 + impactAge * 4.0), 38, 1.30F,
                    (float) (0.58 * (1.0 - impactAge)));
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
        Vec3 horizontal = new Vec3(visual.direction.x, 0.0, visual.direction.z);
        if (horizontal.lengthSqr() < 1.0E-8) horizontal = new Vec3(0.0, 0.0, 1.0);
        ArcaneWorldMesh.Basis vertical = ArcaneWorldMesh.Basis.fromNormal(horizontal.normalize(), new Vec3(0.0, 1.0, 0.0));
        double size = (0.86 + circle * 0.24) * powerFactor;
        if ("gate".equals(visual.spell.id())) size *= 2.0;
        double open = Math.sin(Math.min(1.0, age / 0.30) * Math.PI / 2.0);
        double fade = clamp((1.0 - age) / 0.18, 0.0, 1.0);
        Vec3 center = Vec3.ZERO;
        mesh.band(vertical, center, size * 0.76 * open, size * open,
                72, 1.26F, (float) (0.54 * fade));
        mesh.brokenBand(vertical, center, size * 1.08 * open, size * 1.20 * open,
                72, 6, 0.96F, (float) (0.34 * fade));
        mesh.runeRing(vertical, center, size * 0.88 * open, 12 + circle * 2,
                size * 0.030, visual.spell.id().hashCode(), age * 1.2, 0.76F);
        ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(
                horizontal.add(new Vec3(0.48, 0.0, 0.48)), new Vec3(0.0, 1.0, 0.0));
        mesh.brokenBand(tilt, center, size * 0.88 * open, size * 0.98 * open,
                58, 5, 1.12F, (float) (0.26 * fade));
        mesh.starPlate(vertical, center, size * 0.42 * open, size * 0.16 * open,
                6 + circle / 2, age * 1.8, 1.18F, (float) (0.28 * fade));
        if (age > 0.34 && age < 0.82)
            mesh.brokenBand(vertical, center.add(horizontal.normalize().scale(size * 0.18)),
                    size * 0.48, size * 0.62, 42, 5, 1.08F, 0.22F);
    }

    private static void buildPrison(ArcaneWorldMesh.Builder mesh, Visual visual,
                                    double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double radius = (0.90 + circle * 0.18) * powerFactor;
        double height = 1.8 + circle * 0.24;
        double appear = clamp(age / 0.22, 0.0, 1.0);
        double fade = clamp((1.0 - age) / 0.18, 0.0, 1.0);
        mesh.star(ground, Vec3.ZERO, radius * 0.92 * appear, radius * 0.46 * appear,
                5 + circle / 2, age * 0.6, 1.12F);
        mesh.runeRing(ground, Vec3.ZERO, radius * 0.72 * appear, 8 + circle,
                radius * 0.045, visual.spell.id().hashCode(), -age * 0.8, 0.72F);
        int levels = 4 + circle / 2;
        for (int level = 0; level < levels && !mesh.full(); level++) {
            double localAppear = clamp((appear * levels - level) / 1.25, 0.0, 1.0);
            if (localAppear <= 0.0) continue;
            double y = height * level / Math.max(1.0, levels - 1.0);
            double local = radius * (0.92 + 0.08 * Math.sin(Math.PI * level / levels));
            mesh.band(ground, new Vec3(0.0, y, 0.0), local * 0.88 * localAppear,
                    local * localAppear, 52, 1.14F, (float) (0.36 * fade));
        }
        int sides = 6 + circle / 2;
        for (int side = 0; side < sides && !mesh.full(); side++) {
            double sideAppear = clamp((appear * sides - side) / 1.2, 0.0, 1.0);
            if (sideAppear <= 0.0) continue;
            double a = Math.PI * 2.0 * side / sides;
            Vec3 lower = ground.point(a, radius * sideAppear);
            Vec3 tangent = ground.point(a + Math.PI / 2.0, radius * 0.11);
            Vec3 upper = lower.add(0.0, height * sideAppear, 0.0);
            mesh.face(lower.subtract(tangent), lower.add(tangent),
                    upper.add(tangent), upper.subtract(tangent),
                    side % 2 == 0 ? 1.16F : 0.88F, (float) (0.20 * fade));
        }
        if (appear > 0.82)
            mesh.polygonPlate(ground, new Vec3(0.0, height, 0.0), radius * appear,
                    sides, age * 0.8, 0.82F, (float) (0.18 * fade));
    }

    private static void buildWall(ArcaneWorldMesh.Builder mesh, Visual visual,
                                  double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(visual.direction);
        Vec3 right = facing.right();
        double width = Math.min(72.0, SpellMetrics.wallWidth(visual.spell.id(), visual.range, circle));
        double height = (1.7 + circle * 0.34) * powerFactor;
        double appear = clamp(age / 0.24, 0.0, 1.0);
        double fade = clamp((1.0 - age) / 0.18, 0.0, 1.0);
        int panels = Math.min(18, 6 + circle * 2);
        boolean prismatic = "prismatic_wall".equals(visual.spell.id());
        for (int i = 0; i < panels && !mesh.full(); i++) {
            double panelAppear = clamp((appear * panels - i) / 1.35, 0.0, 1.0);
            if (panelAppear <= 0.0) continue;
            double a = (i / (double) panels - 0.5) * width;
            double b = ((i + 1) / (double) panels - 0.5) * width;
            Vec3 p0 = right.scale(a);
            Vec3 p1 = right.scale(b);
            double crestA = height * panelAppear * (0.88 + 0.12 * Math.sin(i * 1.7));
            double crestB = height * panelAppear * (0.88 + 0.12 * Math.sin((i + 1) * 1.7));
            mesh.face(p0, p1, p1.add(0.0, crestB, 0.0), p0.add(0.0, crestA, 0.0),
                    i % 2 == 0 ? 1.18F : 0.84F, (float) ((prismatic ? 0.34 : 0.28) * fade));
            if (prismatic) {
                Vec3 mid = right.scale((a + b) * 0.5).add(0.0, (crestA + crestB) * 0.52, 0.0);
                mesh.diamond(facing, mid, Math.max(0.16, (b - a) * 0.38), age + i * 0.27,
                        1.24F, (float) (0.28 * fade));
            } else if (i % 2 == 0) {
                Vec3 center = right.scale((a + b) * 0.5).add(0.0, (crestA + crestB) * 0.28, 0.0);
                mesh.orb(center, 0.12 + circle * 0.018, 12, 1.24F, (float) (0.30 * fade));
            }
        }
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        mesh.brokenBand(ground, Vec3.ZERO, width * 0.44, width * 0.50,
                64, 7, 1.10F, (float) (0.24 * fade));
    }

    private static void buildBeam(ArcaneWorldMesh.Builder mesh, Visual visual,
                                  ArcaneWorldMesh.Basis facing, double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        double length = Math.max(0.6, targetOffset(visual).length());
        double reveal = clamp(age / 0.12, 0.0, 1.0);
        double fade = clamp((1.0 - age) / 0.22, 0.0, 1.0);
        double shown = length * reveal;
        double pulse = 0.90 + Math.sin(age * Math.PI * 14.0) * 0.10;
        double radius = (0.05 + circle * 0.013) * powerFactor * pulse;
        float alpha = (float) (0.48 * fade);
        mesh.band(facing, Vec3.ZERO, radius * 2.5, radius * 4.1, 34, 1.22F, alpha * 0.78F);
        mesh.runeRing(facing, Vec3.ZERO, radius * 3.35, 6 + circle / 2, radius * 0.34,
                visual.spell.id().hashCode(), -age * 3.0, 0.72F);
        mesh.beamPrism(Vec3.ZERO, visual.direction, facing, shown, radius * 1.8, 0.78F, alpha * 0.55F);
        mesh.beamPrism(Vec3.ZERO, visual.direction, facing, shown, radius, 1.25F, alpha);
        int nodes = Math.min(8, 3 + circle);
        for (int i = 1; i <= nodes; i++) {
            Vec3 node = visual.direction.scale(shown * i / (nodes + 1.0));
            mesh.brokenBand(facing, node, radius * 2.2, radius * 3.1,
                    24, 4 + i % 3, 1.08F, alpha * 0.65F);
        }
        Vec3 tip = visual.direction.scale(shown);
        mesh.orb(tip, radius * (4.5 + circle * 0.3), 22, 1.18F, alpha);
        if (reveal > 0.92)
            mesh.brokenBand(facing, tip, radius * 3.0, radius * 5.4,
                    34, 5, 1.16F, alpha * 0.72F);
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
        return switch (spell.id()) {
            case "disintegrate" -> 0xFF66FF19;
            case "sunbeam", "sunburst", "foresight", "true_seeing", "solar_guard" -> 0xFFFFE34F;
            case "flame_strike" -> 0xFFFF6A18;
            case "circle_of_death", "finger_of_death", "power_word_kill", "eyebite" -> 0xFFFF174D;
            case "weird", "phantasmal_killer", "feeblemind" -> 0xFFFF22C8;
            case "flesh_to_stone" -> 0xFFD2DAE8;
            case "move_earth", "earthquake" -> 0xFFFFA52E;
            case "time_stop" -> 0xFF55E8FF;
            case "wish" -> 0xFFF0A0FF;
            case "prismatic_spray", "prismatic_wall" -> 0xFFFFFFFF;
            case "control_weather", "reverse_gravity" -> 0xFF24D8FF;
            case "clone", "simulacrum" -> 0xFF9AF4FF;
            case "shapechange", "true_polymorph" -> 0xFF20FFB4;
            default -> switch (spell.school()) {
                case FIRE -> 0xFFFF2100;
                case FROST -> 0xFF00CFFF;
                case WIND -> 0xFF00FF9C;
                case WARD -> 0xFF8E22FF;
                case LIFE -> 0xFF18F044;
                case SPACE -> 0xFFD000FF;
                default -> 0xFF3454FF;
            };
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
                          Vec3 center, Vec3 target, Vec3 direction, double range, double power, double progress,
                          long startedAt, long expiresAt, boolean release, double impactAge) {}
    private record RenderEntry(Vec3 center, ArcaneWorldMesh mesh, int argb) {}
}
