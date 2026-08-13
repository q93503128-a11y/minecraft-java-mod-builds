package kr.moonseungjun.senbonzakura.client;

import kr.moonseungjun.senbonzakura.SenbonzakuraShowcase;
import kr.moonseungjun.senbonzakura.network.BankaiVisualPayload;
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
import java.util.UUID;

/**
 * Clustered-flow Senbonzakura Kageyoshi presentation.
 *
 * The storm is intentionally rendered as several dense, living blade currents instead of
 * thousands of unrelated particles. Each current keeps moving continuously, can merge with
 * neighboring currents during attack beats, and separates again without parking at a fixed end.
 */
public final class BankaiWorldRenderer {
    private static final ContextKey<List<RenderEntry>> DATA_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(SenbonzakuraShowcase.MOD_ID, "bankai_geometry_v7"));
    private static final Map<UUID, Visual> ACTIVE = new HashMap<>();

    private static final int MAX_GEOMETRY = 54_000;
    private static final double MAX_DISTANCE_SQR = 224.0 * 224.0;
    private static final double TAU = Math.PI * 2.0;
    private static final double VISUAL_SECONDS = 13.0;
    private static final double SAMPLE_DT = 0.032;
    private static final double SAMPLE_DP = SAMPLE_DT / VISUAL_SECONDS;
    private static final int CLUSTER_COUNT = 7;

    private BankaiWorldRenderer() {}

    public static void accept(BankaiVisualPayload payload) {
        Map<String, String> values = parse(payload.state());
        UUID caster;
        try {
            caster = UUID.fromString(values.getOrDefault("caster", ""));
        } catch (Exception ignored) {
            return;
        }

        String action = values.getOrDefault("action", "");
        if ("stop".equals(action)) {
            ACTIVE.remove(caster);
            return;
        }
        if (!"start".equals(action)) return;

        Vec3 origin = new Vec3(
                decimal(values, "x", 0.0),
                decimal(values, "y", 0.0),
                decimal(values, "z", 0.0));
        Vec3 facing = safeHorizontal(new Vec3(
                decimal(values, "dx", 0.0),
                0.0,
                decimal(values, "dz", 1.0)));
        int duration = Math.max(80, integer(values, "duration", 260));
        long now = System.nanoTime();
        ACTIVE.put(caster, new Visual(
                caster, origin, facing, now,
                now + duration * 50_000_000L + 850_000_000L));
    }

    public static void onExtract(ExtractLevelRenderStateEvent event) {
        long now = System.nanoTime();
        ACTIVE.values().removeIf(visual -> visual.expiresAt() < now);
        if (ACTIVE.isEmpty()) return;

        List<RenderEntry> entries = new ArrayList<>(ACTIVE.size());
        for (Visual visual : ACTIVE.values()) {
            double duration = Math.max(
                    0.1,
                    (visual.expiresAt() - visual.startedAt() - 850_000_000L)
                            / 1_000_000_000.0);
            double seconds = Math.max(
                    0.0,
                    (now - visual.startedAt()) / 1_000_000_000.0);
            double progress = clamp(seconds / duration, 0.0, 1.0);
            entries.add(new RenderEntry(
                    visual.origin(),
                    build(visual, progress, seconds)));
        }
        event.getRenderState().setRenderData(DATA_KEY, List.copyOf(entries));
    }

    public static void onSubmit(SubmitCustomGeometryEvent event) {
        List<RenderEntry> entries =
                event.getLevelRenderState().getRenderData(DATA_KEY);
        if (entries == null || entries.isEmpty()) return;

        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        float base = Minecraft.getInstance()
                .gameRenderer
                .gameRenderState()
                .windowRenderState
                .appropriateLineWidth;
        float lineScale = Math.max(0.70F, base * 0.76F);

        for (RenderEntry entry : entries) {
            Vec3 offset = entry.origin().subtract(camera);
            if (offset.lengthSqr() > MAX_DISTANCE_SQR) continue;

            event.getPoseStack().pushPose();
            event.getPoseStack().translate(offset.x, offset.y, offset.z);
            entry.mesh().submit(
                    event.getPoseStack(),
                    event.getSubmitNodeCollector(),
                    lineScale);
            event.getPoseStack().popPose();
        }
    }

    private static BladeMesh build(Visual visual, double p, double seconds) {
        BladeMesh.Builder mesh = BladeMesh.builder(MAX_GEOMETRY);
        Vec3 origin = Vec3.ZERO;
        Vec3 forward = visual.facing();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x).normalize();

        appendAtmosphere(mesh, origin, forward, right, p);
        appendReleasedSword(mesh, origin, forward, right, p);
        appendGroundRipples(mesh, origin, p);
        appendMonumentalRows(mesh, origin, forward, right, p);
        appendBladeBreak(mesh, origin, forward, right, p, seconds);
        appendDenseClusters(mesh, origin, forward, right, p, seconds);
        appendMidPetals(mesh, origin, forward, right, p, seconds);
        appendFarPetals(mesh, origin, forward, right, p, seconds);
        return mesh.build();
    }

    private static void appendAtmosphere(
            BladeMesh.Builder mesh,
            Vec3 origin,
            Vec3 forward,
            Vec3 right,
            double p) {
        double alpha =
                smooth(0.075, 0.23, p)
                        * (1.0 - smooth(0.94, 1.0, p));
        if (alpha <= 0.001) return;

        int floor = BladeMesh.withAlpha(0x0004070E, alpha * 0.27);
        double half = 25.0;
        Vec3 a = origin.add(forward.scale(half))
                .add(right.scale(half))
                .add(0.0, 0.022, 0.0);
        Vec3 b = origin.add(forward.scale(half))
                .subtract(right.scale(half))
                .add(0.0, 0.022, 0.0);
        Vec3 c = origin.subtract(forward.scale(half))
                .subtract(right.scale(half))
                .add(0.0, 0.022, 0.0);
        Vec3 d = origin.subtract(forward.scale(half))
                .add(right.scale(half))
                .add(0.0, 0.022, 0.0);
        mesh.quad(a, b, c, d, floor);
    }

    private static void appendReleasedSword(
            BladeMesh.Builder mesh,
            Vec3 origin,
            Vec3 forward,
            Vec3 right,
            double p) {
        double visible = 1.0 - smooth(0.135, 0.165, p);
        if (visible <= 0.001) return;

        double drop = smooth(0.0, 0.088, p);
        double sink = smooth(0.088, 0.155, p);
        double y = mix(1.76, 0.48, easeIn(drop)) - sink * 4.4;
        Vec3 guard = origin.add(forward.scale(0.82)).add(0.0, y, 0.0);
        mesh.katana(
                guard,
                new Vec3(0.0, -1.0, 0.0),
                right,
                0.78,
                (int) Math.round(255.0 * visible));
    }

    private static void appendGroundRipples(
            BladeMesh.Builder mesh,
            Vec3 origin,
            double p) {
        for (int i = 0; i < 3; i++) {
            double local =
                    smooth(0.075 + i * 0.018, 0.20 + i * 0.018, p)
                            * (1.0
                            - smooth(
                            0.205 + i * 0.020,
                            0.30 + i * 0.020,
                            p));
            if (local <= 0.001) continue;

            double radius = 0.22 + local * (2.6 + i * 0.78);
            mesh.groundRing(
                    origin.add(0.0, 0.035 + i * 0.003, 0.0),
                    radius,
                    62,
                    BladeMesh.withAlpha(0x005D183E, local * 0.22),
                    BladeMesh.withAlpha(
                            0x00F4D9E7,
                            local * (0.72 - i * 0.12)),
                    0.82F - i * 0.10F);
        }
    }

    private static void appendMonumentalRows(
            BladeMesh.Builder mesh,
            Vec3 origin,
            Vec3 forward,
            Vec3 right,
            double p) {
        final int perSide = 42;
        final double middle = (perSide - 1) * 0.5;

        for (int side : new int[]{-1, 1}) {
            for (int i = 0; i < perSide && !mesh.full(); i++) {
                double distanceFromCenter =
                        Math.abs(i - middle) / middle;
                double delay = distanceFromCenter * 0.030;
                double rise =
                        smooth(0.115 + delay, 0.305 + delay, p);
                double fracture =
                        smooth(
                                0.372 + delay * 0.12,
                                0.455 + delay * 0.08,
                                p);
                double dissolve =
                        smooth(
                                0.425 + delay * 0.16,
                                0.555 + delay * 0.10,
                                p);
                double alpha = rise * (1.0 - dissolve);
                if (alpha <= 0.002) continue;

                double longitudinal = (i - middle) * 1.10;
                double flare =
                        Math.pow(distanceFromCenter, 1.7) * 1.55;
                double lateral = side * (3.65 + flare);
                double n0 = noise(i * 41 + side * 13);
                double n1 = noise(i * 73 + side * 29);
                double length = 8.0 + n0 * 2.7;
                double width = 0.72 + n1 * 0.26;
                double baseY =
                        -length * 1.02 + rise * length * 1.02;

                Vec3 base = origin
                        .add(forward.scale(longitudinal))
                        .add(right.scale(lateral))
                        .add(0.0, baseY, 0.0);
                Vec3 axis =
                        new Vec3(side * 0.025, 1.0, 0.0)
                                .normalize();

                int face = BladeMesh.withAlpha(
                        fracture > 0.15
                                ? 0x00E9CBD9
                                : 0x00E8E7EB,
                        alpha * (0.94 - fracture * 0.18));
                int edge = BladeMesh.withAlpha(
                        fracture > 0.15
                                ? 0x00FFD2E5
                                : 0x00FFE6F1,
                        alpha * 0.98);

                mesh.longBlade(
                        base,
                        axis,
                        right,
                        length,
                        width,
                        0.13,
                        face,
                        edge);
            }
        }
    }

    /**
     * The large swords no longer simply vanish. Their fragments are already assigned to one of
     * seven future blade currents and are gradually captured by those moving currents.
     */
    private static void appendBladeBreak(
            BladeMesh.Builder mesh,
            Vec3 origin,
            Vec3 forward,
            Vec3 right,
            double p,
            double seconds) {
        double master =
                smooth(0.392, 0.505, p)
                        * (1.0 - smooth(0.620, 0.700, p));
        if (master <= 0.001) return;

        final int count = 760;
        for (int i = 0; i < count && !mesh.full(); i++) {
            int side = (i & 1) == 0 ? -1 : 1;
            int bladeIndex = (i / 2) % 42;
            int layer = (i / 84) % 10;
            int cluster = Math.floorMod(
                    bladeIndex + layer * 2 + (side > 0 ? 3 : 0),
                    CLUSTER_COUNT);

            double n0 = noise(i * 83 + 7);
            double n1 = noise(i * 149 + 19);
            double n2 = noise(i * 227 + 31);
            double n3 = noise(i * 337 + 53);

            double row = (bladeIndex - 20.5) * 1.10;
            double baseLateral =
                    side * (3.7
                            + Math.pow(
                            Math.abs(bladeIndex - 20.5) / 20.5,
                            1.7) * 1.5);
            double localStart =
                    0.396 + layer * 0.007 + n0 * 0.013;
            double burst =
                    smooth(localStart, localStart + 0.120, p);
            double fade =
                    1.0
                            - smooth(
                            0.615 + n1 * 0.020,
                            0.700 + n1 * 0.015,
                            p);
            double alpha = burst * fade;
            if (alpha <= 0.002) continue;

            double height =
                    0.55 + layer * 0.88 + n2 * 1.25;
            double phase =
                    seconds * (0.66 + n1 * 0.46)
                            + n3 * TAU;

            Vec3 loose = origin
                    .add(forward.scale(
                            row
                                    + Math.sin(phase * 0.58)
                                    * burst
                                    * (0.45 + n2 * 0.90)))
                    .add(right.scale(
                            baseLateral
                                    + side
                                    * burst
                                    * (0.65 + n1 * 2.15)))
                    .add(
                            0.0,
                            height
                                    + burst
                                    * (0.25 + n0 * 1.10),
                            0.0);

            double gather =
                    smooth(
                            localStart + 0.045,
                            localStart + 0.205,
                            p);
            Vec3 captured = clusterPetalPoint(
                    origin,
                    forward,
                    right,
                    cluster,
                    n0,
                    n1,
                    n2,
                    n3,
                    seconds,
                    p,
                    0.82,
                    0.78);
            Vec3 position = loose.lerp(captured, gather);

            Vec3 capturedNext = clusterPetalPoint(
                    origin,
                    forward,
                    right,
                    cluster,
                    n0,
                    n1,
                    n2,
                    n3,
                    seconds + SAMPLE_DT,
                    p + SAMPLE_DP,
                    0.82,
                    0.78);
            Vec3 looseNext = origin
                    .add(forward.scale(
                            row
                                    + Math.sin(
                                    (phase + SAMPLE_DT) * 0.58)
                                    * burst
                                    * (0.45 + n2 * 0.90)))
                    .add(right.scale(
                            baseLateral
                                    + side
                                    * burst
                                    * (0.65 + n1 * 2.15)))
                    .add(
                            0.0,
                            height
                                    + burst
                                    * (0.25 + n0 * 1.10),
                            0.0);
            Vec3 next =
                    looseNext.lerp(capturedNext, gather);
            Vec3 tangent = next.subtract(position);
            if (tangent.lengthSqr() < 1.0E-7) {
                tangent = right.scale(side)
                        .add(forward.scale(0.25));
            }

            int face = BladeMesh.withAlpha(
                    (i % 9 == 0)
                            ? 0x00FFF9FC
                            : 0x00D985AD,
                    alpha * 0.84);
            int pale = BladeMesh.withAlpha(
                    0x00FFE7F2,
                    alpha * 0.91);

            if (i < 150) {
                solidPetal(
                        mesh,
                        position,
                        tangent,
                        right,
                        0.30 + n0 * 0.42,
                        0.065 + n1 * 0.090,
                        0.018 + n2 * 0.016,
                        face,
                        pale);
            } else {
                lightPetal(
                        mesh,
                        position,
                        tangent,
                        right,
                        0.22 + n0 * 0.34,
                        0.050 + n1 * 0.075,
                        face);
            }
        }
    }

    /**
     * Close layer: 420 solid blade-petals. This gives the swarm depth close to the camera while
     * the majority of visible mass is supplied by the cheaper mid/far layers.
     */
    private static void appendDenseClusters(
            BladeMesh.Builder mesh,
            Vec3 origin,
            Vec3 forward,
            Vec3 right,
            double p,
            double seconds) {
        double global =
                smooth(0.475, 0.585, p)
                        * (1.0 - smooth(0.955, 1.0, p));
        if (global <= 0.001) return;

        final int count = 420;
        for (int i = 0; i < count && !mesh.full(); i++) {
            int cluster =
                    Math.floorMod(i * 5 + (i / 11), CLUSTER_COUNT);
            double n0 = noise(i * 101 + 17);
            double n1 = noise(i * 191 + 37);
            double n2 = noise(i * 271 + 59);
            double n3 = noise(i * 353 + 71);

            Vec3 position = clusterPetalPoint(
                    origin,
                    forward,
                    right,
                    cluster,
                    n0,
                    n1,
                    n2,
                    n3,
                    seconds,
                    p,
                    1.00,
                    1.00);
            Vec3 next = clusterPetalPoint(
                    origin,
                    forward,
                    right,
                    cluster,
                    n0,
                    n1,
                    n2,
                    n3,
                    seconds + SAMPLE_DT,
                    p + SAMPLE_DP,
                    1.00,
                    1.00);
            position =
                    softenSafeVoid(
                            position,
                            origin,
                            right,
                            2.25);
            Vec3 tangent = next.subtract(position);
            if (tangent.lengthSqr() < 1.0E-7) {
                tangent = forward;
            }

            double shimmer =
                    0.86
                            + 0.14
                            * Math.sin(
                            seconds * (3.0 + n3 * 1.4)
                                    + n0 * TAU);
            double alpha =
                    clamp(global * shimmer, 0.0, 1.0);

            int face = BladeMesh.withAlpha(
                    (i % 7 == 0)
                            ? 0x00FFFDFE
                            : 0x00DE83AD,
                    alpha * 0.94);
            int sideColor = BladeMesh.withAlpha(
                    (i % 5 == 0)
                            ? 0x00FFF3FA
                            : 0x00F4BCD5,
                    alpha * 0.90);

            solidPetal(
                    mesh,
                    position,
                    tangent,
                    right,
                    0.28 + n2 * 0.42,
                    0.060 + n3 * 0.085,
                    0.016 + n0 * 0.019,
                    face,
                    sideColor);
        }
    }

    /**
     * Main visual mass: more than two thousand inexpensive two-sided blade faces.
     */
    private static void appendMidPetals(
            BladeMesh.Builder mesh,
            Vec3 origin,
            Vec3 forward,
            Vec3 right,
            double p,
            double seconds) {
        double global =
                smooth(0.485, 0.600, p)
                        * (1.0 - smooth(0.955, 1.0, p));
        if (global <= 0.001) return;

        final int count = 2_100;
        for (int i = 0; i < count && !mesh.full(); i++) {
            int cluster =
                    Math.floorMod(
                            i * 3 + (i / 17),
                            CLUSTER_COUNT);
            double n0 = noise(i * 67 + 11);
            double n1 = noise(i * 131 + 23);
            double n2 = noise(i * 197 + 43);
            double n3 = noise(i * 283 + 61);

            Vec3 position = clusterPetalPoint(
                    origin,
                    forward,
                    right,
                    cluster,
                    n0,
                    n1,
                    n2,
                    n3,
                    seconds + n0 * 0.28,
                    p,
                    1.10,
                    1.18);
            Vec3 next = clusterPetalPoint(
                    origin,
                    forward,
                    right,
                    cluster,
                    n0,
                    n1,
                    n2,
                    n3,
                    seconds + n0 * 0.28 + SAMPLE_DT,
                    p + SAMPLE_DP,
                    1.10,
                    1.18);
            position =
                    softenSafeVoid(
                            position,
                            origin,
                            right,
                            2.18);
            Vec3 tangent = next.subtract(position);
            if (tangent.lengthSqr() < 1.0E-7) {
                tangent = forward;
            }

            double alpha =
                    clamp(
                            global
                                    * (0.72
                                    + 0.28
                                    * Math.sin(
                                    seconds
                                            * (2.2 + n3)
                                            + n2 * TAU)),
                            0.0,
                            1.0);
            int face = BladeMesh.withAlpha(
                    (i % 19 == 0)
                            ? 0x00FFFDFE
                            : 0x00DB8AB1,
                    alpha * 0.78);

            lightPetal(
                    mesh,
                    position,
                    tangent,
                    right,
                    0.17 + n2 * 0.27,
                    0.043 + n3 * 0.060,
                    face);
        }
    }

    /**
     * Far layer: tiny blade faces fill the current so each macro-cluster reads as a continuous
     * torrent rather than a handful of visible objects.
     */
    private static void appendFarPetals(
            BladeMesh.Builder mesh,
            Vec3 origin,
            Vec3 forward,
            Vec3 right,
            double p,
            double seconds) {
        double global =
                smooth(0.495, 0.615, p)
                        * (1.0 - smooth(0.950, 1.0, p));
        if (global <= 0.001) return;

        final int count = 2_400;
        for (int i = 0; i < count && !mesh.full(); i++) {
            int cluster =
                    Math.floorMod(
                            i * 4 + (i / 13),
                            CLUSTER_COUNT);
            double n0 = noise(i * 53 + 5);
            double n1 = noise(i * 109 + 19);
            double n2 = noise(i * 173 + 37);
            double n3 = noise(i * 251 + 71);

            Vec3 position = clusterPetalPoint(
                    origin,
                    forward,
                    right,
                    cluster,
                    n0,
                    n1,
                    n2,
                    n3,
                    seconds + n1 * 0.55,
                    p,
                    1.30,
                    1.48);
            Vec3 next = clusterPetalPoint(
                    origin,
                    forward,
                    right,
                    cluster,
                    n0,
                    n1,
                    n2,
                    n3,
                    seconds + n1 * 0.55 + SAMPLE_DT,
                    p + SAMPLE_DP,
                    1.30,
                    1.48);
            position =
                    softenSafeVoid(
                            position,
                            origin,
                            right,
                            2.12);
            Vec3 tangent = next.subtract(position);
            if (tangent.lengthSqr() < 1.0E-7) {
                tangent = forward;
            }

            double flicker =
                    0.64
                            + 0.36
                            * Math.sin(
                            seconds * (2.6 + n3 * 1.4)
                                    + n0 * TAU);
            double alpha =
                    clamp(global * flicker, 0.0, 1.0);

            int face = BladeMesh.withAlpha(
                    (i % 31 == 0)
                            ? 0x00FFF8FC
                            : 0x00E29ABC,
                    alpha * 0.58);

            lightPetal(
                    mesh,
                    position,
                    tangent,
                    right,
                    0.085 + n1 * 0.125,
                    0.022 + n3 * 0.034,
                    face);
        }
    }

    /**
     * Petal position inside one of seven macro currents. A current has an animated center and
     * a rotating elongated local field. Hundreds of petals share the same current center, which
     * makes the storm visibly bunch together without all blades occupying one exact point.
     */
    private static Vec3 clusterPetalPoint(
            Vec3 origin,
            Vec3 forward,
            Vec3 right,
            int cluster,
            double n0,
            double n1,
            double n2,
            double n3,
            double seconds,
            double p,
            double speedScale,
            double spreadScale) {
        Vec3 center = clusterCenter(
                origin,
                forward,
                right,
                cluster,
                seconds,
                p,
                speedScale);

        double phase =
                n0 * TAU
                        + seconds
                        * (1.05 + n3 * 1.05)
                        * speedScale
                        + cluster * 0.77;
        double slow =
                n2 * TAU
                        + seconds
                        * (0.33 + n1 * 0.24)
                        * speedScale;

        double along =
                Math.sin(phase)
                        * (2.0 + n1 * 3.4)
                        * spreadScale
                        + Math.sin(slow * 0.72)
                        * 1.25
                        * spreadScale;
        double across =
                Math.cos(phase * 0.73 + n3)
                        * (0.55 + n0 * 1.55)
                        * spreadScale
                        + Math.sin(slow)
                        * 0.55
                        * spreadScale;
        double vertical =
                (n2 - 0.5)
                        * 3.8
                        * spreadScale
                        + Math.sin(phase * 0.87 + n1 * TAU)
                        * (0.45 + n3 * 0.75)
                        * spreadScale;

        double angle =
                seconds * (0.29 + cluster * 0.018)
                        + cluster * TAU / CLUSTER_COUNT;
        Vec3 streamForward =
                forward.scale(Math.cos(angle))
                        .add(right.scale(Math.sin(angle)))
                        .normalize();
        Vec3 streamSide =
                new Vec3(
                        -streamForward.z,
                        0.0,
                        streamForward.x)
                        .normalize();

        return center
                .add(streamForward.scale(along))
                .add(streamSide.scale(across))
                .add(0.0, vertical, 0.0);
    }

    /**
     * Seven macro currents circle, merge in two large banks, then sweep forward as a broad river.
     * Nothing targets an entity or owns a final point; every center remains time-dependent.
     */
    private static Vec3 clusterCenter(
            Vec3 origin,
            Vec3 forward,
            Vec3 right,
            int cluster,
            double seconds,
            double p,
            double speedScale) {
        double seed = cluster / (double) CLUSTER_COUNT;
        double orbit =
                seed * TAU
                        + seconds
                        * (0.22 + 0.018 * cluster)
                        * speedScale;

        double radial =
                8.8
                        + 2.0 * Math.sin(seconds * 0.38 + cluster * 1.13)
                        + (cluster % 2) * 1.35;
        double forwardBias =
                3.5
                        + Math.sin(seconds * 0.27 + cluster * 0.61)
                        * 2.2;

        Vec3 base = origin
                .add(forward.scale(
                        forwardBias
                                + Math.cos(orbit) * radial))
                .add(right.scale(
                        Math.sin(orbit)
                                * (7.4
                                + 1.2
                                * Math.sin(
                                seconds * 0.31
                                        + cluster))))
                .add(
                        0.0,
                        4.2
                                + Math.sin(
                                orbit * 1.35
                                        + cluster * 0.42)
                                * 2.4
                                + (cluster % 3) * 0.55,
                        0.0);

        int bankSide =
                cluster <= 2
                        ? -1
                        : (cluster <= 5 ? 1 : 0);

        double leftMerge =
                bankSide < 0
                        ? pulse(p, 0.535, 0.615, 0.720)
                        : 0.0;
        double rightMerge =
                bankSide > 0
                        ? pulse(p, 0.655, 0.755, 0.855)
                        : 0.0;
        double finalMerge =
                pulse(p, 0.785, 0.885, 0.970);

        if (bankSide != 0) {
            double merge =
                    Math.max(leftMerge, rightMerge) * 0.82;
            Vec3 macro = macroBankCenter(
                    origin,
                    forward,
                    right,
                    bankSide,
                    seconds,
                    p);
            double lane =
                    (cluster % 3 - 1)
                            * (1.25 - merge * 0.45);
            Vec3 target =
                    macro.add(forward.scale(lane))
                            .add(0.0, lane * 0.28, 0.0);
            base = base.lerp(target, merge);
        }

        if (finalMerge > 0.001) {
            int laneSide =
                    cluster == 6
                            ? 0
                            : (cluster <= 2 ? -1 : 1);
            Vec3 river = origin
                    .add(forward.scale(
                            8.0
                                    + Math.sin(
                                    seconds * 0.52
                                            + cluster)
                                    * 3.0))
                    .add(right.scale(
                            laneSide
                                    * (4.3
                                    + (cluster % 3 - 1)
                                    * 0.85)))
                    .add(
                            0.0,
                            3.8
                                    + Math.sin(
                                    seconds * 0.78
                                            + cluster * 0.7)
                                    * 1.6,
                            0.0);

            double surge =
                    Math.sin(
                            clamp(
                                    (p - 0.785)
                                            / (0.970 - 0.785),
                                    0.0,
                                    1.0)
                                    * Math.PI);
            river = river.add(
                    forward.scale(
                            surge
                                    * (7.0
                                    + (cluster % 3) * 1.4)));
            base =
                    base.lerp(
                            river,
                            finalMerge * 0.76);
        }

        return base;
    }

    private static Vec3 macroBankCenter(
            Vec3 origin,
            Vec3 forward,
            Vec3 right,
            int side,
            double seconds,
            double p) {
        double wave =
                seconds * 0.48
                        + (side < 0 ? 0.0 : 2.2);
        double cross =
                side < 0
                        ? pulse(p, 0.555, 0.640, 0.735)
                        : pulse(p, 0.680, 0.765, 0.860);

        return origin
                .add(forward.scale(
                        2.0 + Math.sin(wave) * 8.5))
                .add(right.scale(
                        side * (8.8 - cross * 6.2)
                                - side
                                * Math.sin(wave * 0.73)
                                * 2.4))
                .add(
                        0.0,
                        4.3
                                + Math.cos(wave * 1.21) * 1.9
                                + cross * 0.9,
                        0.0);
    }

    /**
     * Close petals are true tiny 3D blades, but have no explicit line primitive.
     */
    private static void solidPetal(
            BladeMesh.Builder mesh,
            Vec3 center,
            Vec3 axis,
            Vec3 broadHint,
            double length,
            double width,
            double thickness,
            int faceColor,
            int sideColor) {
        Vec3 n = safe(axis, new Vec3(0.0, 0.0, 1.0));
        Vec3 side = orthogonalSide(n, broadHint);
        Vec3 thick = n.cross(side);
        if (thick.lengthSqr() < 1.0E-8) {
            thick = new Vec3(0.0, 1.0, 0.0);
        }
        thick = thick.normalize().scale(thickness * 0.5);

        Vec3 front = center.add(n.scale(length * 0.56));
        Vec3 back = center.subtract(n.scale(length * 0.44));
        Vec3 left = center.add(side.scale(width * 0.5));
        Vec3 right = center.subtract(side.scale(width * 0.5));

        Vec3 fu = front.add(thick);
        Vec3 fd = front.subtract(thick);
        Vec3 bu = back.add(thick);
        Vec3 bd = back.subtract(thick);
        Vec3 lu = left.add(thick);
        Vec3 ld = left.subtract(thick);
        Vec3 ru = right.add(thick);
        Vec3 rd = right.subtract(thick);

        mesh.quad(fu, lu, bu, ru, faceColor);
        mesh.quad(rd, bd, ld, fd, faceColor);
        mesh.quad(fu, fd, ld, lu, sideColor);
        mesh.quad(lu, ld, bd, bu, faceColor);
        mesh.quad(bu, bd, rd, ru, faceColor);
        mesh.quad(ru, rd, fd, fu, sideColor);
    }

    /**
     * Mid/far petals are double-sided filled blade faces. No RenderTypes.lines geometry is added.
     */
    private static void lightPetal(
            BladeMesh.Builder mesh,
            Vec3 center,
            Vec3 axis,
            Vec3 broadHint,
            double length,
            double width,
            int color) {
        Vec3 n = safe(axis, new Vec3(0.0, 0.0, 1.0));
        Vec3 side = orthogonalSide(n, broadHint);

        Vec3 front = center.add(n.scale(length * 0.56));
        Vec3 back = center.subtract(n.scale(length * 0.44));
        Vec3 left = center.add(side.scale(width * 0.5));
        Vec3 right = center.subtract(side.scale(width * 0.5));

        mesh.quad(front, left, back, right, color);
    }

    private static Vec3 orthogonalSide(
            Vec3 axis,
            Vec3 broadHint) {
        Vec3 projected =
                broadHint.subtract(
                        axis.scale(broadHint.dot(axis)));
        if (projected.lengthSqr() < 1.0E-7) {
            Vec3 helper =
                    Math.abs(axis.y) < 0.82
                            ? new Vec3(0.0, 1.0, 0.0)
                            : new Vec3(1.0, 0.0, 0.0);
            projected =
                    helper.subtract(
                            axis.scale(helper.dot(axis)));
        }
        return projected.normalize();
    }

    private static Vec3 softenSafeVoid(
            Vec3 point,
            Vec3 origin,
            Vec3 fallback,
            double radius) {
        Vec3 delta = point.subtract(origin);
        Vec3 flat = new Vec3(delta.x, 0.0, delta.z);
        double length = flat.length();
        if (length >= radius) return point;

        Vec3 direction =
                length < 1.0E-6
                        ? safe(
                        fallback,
                        new Vec3(1.0, 0.0, 0.0))
                        : flat.scale(1.0 / length);
        double normalized =
                clamp(length / radius, 0.0, 1.0);
        double adjusted =
                radius * (0.80 + 0.20 * normalized);
        return new Vec3(
                origin.x + direction.x * adjusted,
                point.y,
                origin.z + direction.z * adjusted);
    }

    private static double pulse(
            double value,
            double in,
            double peak,
            double out) {
        return smooth(in, peak, value)
                * (1.0 - smooth(peak, out, value));
    }

    private static double smooth(
            double from,
            double to,
            double value) {
        if (to <= from) {
            return value >= to ? 1.0 : 0.0;
        }
        double t =
                clamp(
                        (value - from) / (to - from),
                        0.0,
                        1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double easeIn(double t) {
        double c = clamp(t, 0.0, 1.0);
        return c * c * c;
    }

    private static double mix(
            double a,
            double b,
            double t) {
        return a
                + (b - a)
                * clamp(t, 0.0, 1.0);
    }

    private static double noise(int seed) {
        double value =
                Math.sin(seed * 12.9898 + 78.233)
                        * 43758.5453123;
        return value - Math.floor(value);
    }

    private static Vec3 safe(
            Vec3 value,
            Vec3 fallback) {
        return value.lengthSqr() < 1.0E-8
                ? fallback.normalize()
                : value.normalize();
    }

    private static Vec3 safeHorizontal(Vec3 direction) {
        Vec3 flat =
                new Vec3(
                        direction.x,
                        0.0,
                        direction.z);
        return flat.lengthSqr() < 1.0E-8
                ? new Vec3(0.0, 0.0, 1.0)
                : flat.normalize();
    }

    private static Map<String, String> parse(String state) {
        Map<String, String> result = new HashMap<>();
        if (state == null || state.isBlank()) return result;

        for (String token : state.split(";")) {
            int split = token.indexOf('=');
            if (split > 0) {
                result.put(
                        token.substring(0, split),
                        token.substring(split + 1));
            }
        }
        return result;
    }

    private static double decimal(
            Map<String, String> values,
            String key,
            double fallback) {
        try {
            return Double.parseDouble(
                    values.getOrDefault(
                            key,
                            Double.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int integer(
            Map<String, String> values,
            String key,
            int fallback) {
        try {
            return Integer.parseInt(
                    values.getOrDefault(
                            key,
                            Integer.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double clamp(
            double value,
            double min,
            double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Visual(
            UUID caster,
            Vec3 origin,
            Vec3 facing,
            long startedAt,
            long expiresAt) {}

    private record RenderEntry(
            Vec3 origin,
            BladeMesh mesh) {}
}
