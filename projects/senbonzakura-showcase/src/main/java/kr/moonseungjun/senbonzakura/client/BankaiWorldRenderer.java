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
 * Continuous-flow Senbonzakura Kageyoshi presentation.
 *
 * Petals never fly toward a fixed target point. Attack beats deform the same continuous
 * flow field, so blades sweep through an area and keep moving after the hit window.
 */
public final class BankaiWorldRenderer {
    private static final ContextKey<List<RenderEntry>> DATA_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(SenbonzakuraShowcase.MOD_ID, "bankai_geometry_v6"));
    private static final Map<UUID, Visual> ACTIVE = new HashMap<>();
    private static final int MAX_GEOMETRY = 42_000;
    private static final double MAX_DISTANCE_SQR = 216.0 * 216.0;
    private static final double TAU = Math.PI * 2.0;
    private static final double VISUAL_SECONDS = 13.0;
    private static final double SAMPLE_DT = 0.035;
    private static final double SAMPLE_DP = SAMPLE_DT / VISUAL_SECONDS;

    private BankaiWorldRenderer() {}

    public static void accept(BankaiVisualPayload payload) {
        Map<String, String> values = parse(payload.state());
        UUID caster;
        try { caster = UUID.fromString(values.getOrDefault("caster", "")); }
        catch (Exception ignored) { return; }

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
        ACTIVE.put(caster, new Visual(caster, origin, facing, now,
                now + duration * 50_000_000L + 850_000_000L));
    }

    public static void onExtract(ExtractLevelRenderStateEvent event) {
        long now = System.nanoTime();
        ACTIVE.values().removeIf(visual -> visual.expiresAt() < now);
        if (ACTIVE.isEmpty()) return;

        List<RenderEntry> entries = new ArrayList<>(ACTIVE.size());
        for (Visual visual : ACTIVE.values()) {
            double duration = Math.max(0.1,
                    (visual.expiresAt() - visual.startedAt() - 850_000_000L) / 1_000_000_000.0);
            double seconds = Math.max(0.0, (now - visual.startedAt()) / 1_000_000_000.0);
            double progress = clamp(seconds / duration, 0.0, 1.0);
            entries.add(new RenderEntry(visual.origin(), build(visual, progress, seconds)));
        }
        event.getRenderState().setRenderData(DATA_KEY, List.copyOf(entries));
    }

    public static void onSubmit(SubmitCustomGeometryEvent event) {
        List<RenderEntry> entries = event.getLevelRenderState().getRenderData(DATA_KEY);
        if (entries == null || entries.isEmpty()) return;

        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        float base = Minecraft.getInstance().gameRenderer.gameRenderState()
                .windowRenderState.appropriateLineWidth;
        float lineScale = Math.max(0.70F, base * 0.76F);
        for (RenderEntry entry : entries) {
            Vec3 offset = entry.origin().subtract(camera);
            if (offset.lengthSqr() > MAX_DISTANCE_SQR) continue;
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(offset.x, offset.y, offset.z);
            entry.mesh().submit(event.getPoseStack(), event.getSubmitNodeCollector(), lineScale);
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
        appendFlowPetals(mesh, origin, forward, right, p, seconds);
        appendCorePetals(mesh, origin, forward, right, p, seconds);
        appendDustPetals(mesh, origin, forward, right, p, seconds);
        return mesh.build();
    }

    private static void appendAtmosphere(
            BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right, double p) {
        double alpha = smooth(0.075, 0.23, p) * (1.0 - smooth(0.94, 1.0, p));
        if (alpha <= 0.001) return;

        int floor = BladeMesh.withAlpha(0x0004070E, alpha * 0.27);
        double half = 25.0;
        Vec3 a = origin.add(forward.scale(half)).add(right.scale(half)).add(0.0, 0.022, 0.0);
        Vec3 b = origin.add(forward.scale(half)).subtract(right.scale(half)).add(0.0, 0.022, 0.0);
        Vec3 c = origin.subtract(forward.scale(half)).subtract(right.scale(half)).add(0.0, 0.022, 0.0);
        Vec3 d = origin.subtract(forward.scale(half)).add(right.scale(half)).add(0.0, 0.022, 0.0);
        mesh.quad(a, b, c, d, floor);
    }

    private static void appendReleasedSword(
            BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right, double p) {
        double visible = 1.0 - smooth(0.135, 0.165, p);
        if (visible <= 0.001) return;

        double drop = smooth(0.0, 0.088, p);
        double sink = smooth(0.088, 0.155, p);
        double y = mix(1.76, 0.48, easeIn(drop)) - sink * 4.4;
        Vec3 guard = origin.add(forward.scale(0.82)).add(0.0, y, 0.0);
        mesh.katana(guard, new Vec3(0.0, -1.0, 0.0), right, 0.78,
                (int) Math.round(255.0 * visible));
    }

    private static void appendGroundRipples(BladeMesh.Builder mesh, Vec3 origin, double p) {
        for (int i = 0; i < 3; i++) {
            double local = smooth(0.075 + i * 0.018, 0.20 + i * 0.018, p)
                    * (1.0 - smooth(0.205 + i * 0.020, 0.30 + i * 0.020, p));
            if (local <= 0.001) continue;

            double radius = 0.22 + local * (2.6 + i * 0.78);
            mesh.groundRing(origin.add(0.0, 0.035 + i * 0.003, 0.0), radius, 62,
                    BladeMesh.withAlpha(0x005D183E, local * 0.22),
                    BladeMesh.withAlpha(0x00F4D9E7, local * (0.72 - i * 0.12)),
                    0.82F - i * 0.10F);
        }
    }

    private static void appendMonumentalRows(
            BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right, double p) {
        final int perSide = 42;
        final double middle = (perSide - 1) * 0.5;
        for (int side : new int[]{-1, 1}) {
            for (int i = 0; i < perSide && !mesh.full(); i++) {
                double distanceFromCenter = Math.abs(i - middle) / middle;
                double delay = distanceFromCenter * 0.030;
                double rise = smooth(0.115 + delay, 0.305 + delay, p);
                double fracture = smooth(0.372 + delay * 0.12, 0.455 + delay * 0.08, p);
                double dissolve = smooth(0.425 + delay * 0.16, 0.555 + delay * 0.10, p);
                double alpha = rise * (1.0 - dissolve);
                if (alpha <= 0.002) continue;

                double longitudinal = (i - middle) * 1.10;
                double flare = Math.pow(distanceFromCenter, 1.7) * 1.55;
                double lateral = side * (3.65 + flare);
                double n0 = noise(i * 41 + side * 13);
                double n1 = noise(i * 73 + side * 29);
                double length = 8.0 + n0 * 2.7;
                double width = 0.72 + n1 * 0.26;
                double baseY = -length * 1.02 + rise * length * 1.02;

                Vec3 base = origin.add(forward.scale(longitudinal))
                        .add(right.scale(lateral))
                        .add(0.0, baseY, 0.0);
                Vec3 axis = new Vec3(side * 0.025, 1.0, 0.0).normalize();
                int face = BladeMesh.withAlpha(
                        fracture > 0.15 ? 0x00E9CBD9 : 0x00E8E7EB,
                        alpha * (0.94 - fracture * 0.18));
                int edge = BladeMesh.withAlpha(
                        fracture > 0.15 ? 0x00FFD2E5 : 0x00FFE6F1,
                        alpha * 0.98);
                mesh.longBlade(base, axis, right, length, width, 0.13, face, edge);
            }
        }
    }

    private static void appendBladeBreak(
            BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right,
            double p, double seconds) {
        double master = smooth(0.392, 0.505, p) * (1.0 - smooth(0.590, 0.670, p));
        if (master <= 0.001) return;

        final int count = 480;
        for (int i = 0; i < count && !mesh.full(); i++) {
            int side = (i & 1) == 0 ? -1 : 1;
            int bladeIndex = (i / 2) % 42;
            int layer = (i / 84) % 6;
            double n0 = noise(i * 83 + 7);
            double n1 = noise(i * 149 + 19);
            double n2 = noise(i * 227 + 31);
            double n3 = noise(i * 337 + 53);

            double row = (bladeIndex - 20.5) * 1.10;
            double baseLateral = side * (3.7
                    + Math.pow(Math.abs(bladeIndex - 20.5) / 20.5, 1.7) * 1.5);
            double localStart = 0.396 + layer * 0.012 + n0 * 0.014;
            double burst = smooth(localStart, localStart + 0.135, p);
            double fade = 1.0 - smooth(0.595 + n1 * 0.018, 0.680 + n1 * 0.012, p);
            double alpha = burst * fade;
            if (alpha <= 0.002) continue;

            double height = 0.70 + layer * 1.48 + n2 * 1.55;
            double phase = seconds * (0.72 + n1 * 0.52) + n3 * TAU;
            Vec3 pos = origin
                    .add(forward.scale(row + Math.sin(phase * 0.58) * burst * (0.55 + n2)))
                    .add(right.scale(baseLateral + side * burst * (0.75 + n1 * 2.6)))
                    .add(0.0, height + burst * (0.30 + n0 * 1.35), 0.0);
            Vec3 next = origin
                    .add(forward.scale(row + Math.sin((phase + 0.035) * 0.58) * burst * (0.55 + n2)))
                    .add(right.scale(baseLateral + side * burst * (0.75 + n1 * 2.6)))
                    .add(0.0, height + burst * (0.30 + n0 * 1.35), 0.0);
            Vec3 tangent = next.subtract(pos);
            if (tangent.lengthSqr() < 1.0E-7) {
                tangent = right.scale(side).add(forward.scale(0.25));
            }

            int face = BladeMesh.withAlpha(
                    (i % 9 == 0) ? 0x00FFF9FC : 0x00D985AD,
                    alpha * 0.84);
            int edge = BladeMesh.withAlpha(0x00FFE7F2, alpha * 0.96);
            mesh.shard(pos, tangent, right,
                    0.30 + n0 * 0.44,
                    0.065 + n1 * 0.090,
                    0.020 + n2 * 0.017,
                    face, edge);
        }
    }

    private static void appendFlowPetals(
            BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right,
            double p, double seconds) {
        double global = smooth(0.475, 0.585, p) * (1.0 - smooth(0.945, 1.0, p));
        if (global <= 0.001) return;

        final int count = 820;
        for (int i = 0; i < count && !mesh.full(); i++) {
            int side = (i & 1) == 0 ? -1 : 1;
            int group = (i / 2) % 6;
            double n0 = noise(i * 71 + 13);
            double n1 = noise(i * 137 + 29);
            double n2 = noise(i * 211 + 47);
            double n3 = noise(i * 307 + 61);

            Vec3 position = advectedPoint(origin, forward, right, side, group,
                    n0, n1, n2, n3, seconds, p, 1.0, 1.0, 1.0);
            Vec3 next = advectedPoint(origin, forward, right, side, group,
                    n0, n1, n2, n3, seconds + SAMPLE_DT, p + SAMPLE_DP, 1.0, 1.0, 1.0);
            position = softenSafeVoid(position, origin, right.scale(side), 2.35);
            Vec3 tangent = next.subtract(position);
            if (tangent.lengthSqr() < 1.0E-7) tangent = forward;

            double shimmer = 0.80
                    + 0.20 * Math.sin(seconds * (3.1 + n3 * 1.5) + n0 * TAU);
            double alpha = clamp(global * shimmer, 0.0, 1.0);
            int face = BladeMesh.withAlpha(
                    (i % 13 == 0) ? 0x00FFFDFE : 0x00D879A7,
                    alpha * ((i % 5 == 0) ? 0.92 : 0.74));
            int edge = BladeMesh.withAlpha(
                    (i % 9 == 0) ? 0x00FFFFFF : 0x00F8C9DF,
                    alpha * 0.94);
            mesh.shard(position, tangent, right,
                    0.20 + n2 * 0.31,
                    0.050 + n3 * 0.072,
                    0.015 + n0 * 0.017,
                    face, edge);
        }
    }

    private static void appendCorePetals(
            BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right,
            double p, double seconds) {
        double global = smooth(0.49, 0.59, p) * (1.0 - smooth(0.95, 1.0, p));
        if (global <= 0.001) return;

        final int count = 220;
        for (int i = 0; i < count && !mesh.full(); i++) {
            int side = (i & 1) == 0 ? -1 : 1;
            int group = (i / 2) % 5;
            double n0 = noise(i * 101 + 17);
            double n1 = noise(i * 191 + 37);
            double n2 = noise(i * 271 + 59);
            double n3 = noise(i * 353 + 71);

            Vec3 position = advectedPoint(origin, forward, right, side, group,
                    n0, n1, n2, n3, seconds, p, 1.34, 0.92, 1.14);
            Vec3 next = advectedPoint(origin, forward, right, side, group,
                    n0, n1, n2, n3, seconds + SAMPLE_DT, p + SAMPLE_DP, 1.34, 0.92, 1.14);
            position = softenSafeVoid(position, origin, right.scale(side), 2.35);
            Vec3 tangent = next.subtract(position);
            if (tangent.lengthSqr() < 1.0E-7) tangent = forward;

            double alpha = clamp(global
                    * (0.90 + 0.10 * Math.sin(seconds * 3.8 + n1 * TAU)),
                    0.0, 1.0);
            int face = BladeMesh.withAlpha(
                    (i % 4 == 0) ? 0x00FFFDFE : 0x00E08CB6,
                    alpha * 0.94);
            int edge = BladeMesh.withAlpha(0x00FFFFFF, alpha * 0.98);
            mesh.shard(position, tangent, right,
                    0.31 + n2 * 0.40,
                    0.068 + n3 * 0.082,
                    0.019 + n0 * 0.019,
                    face, edge);
        }
    }

    private static void appendDustPetals(
            BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right,
            double p, double seconds) {
        double global = smooth(0.50, 0.61, p) * (1.0 - smooth(0.94, 1.0, p));
        if (global <= 0.001) return;

        final int count = 620;
        for (int i = 0; i < count && !mesh.full(); i++) {
            int side = (i & 1) == 0 ? -1 : 1;
            int group = (i / 2) % 6;
            double n0 = noise(i * 59 + 5);
            double n1 = noise(i * 127 + 23);
            double n2 = noise(i * 199 + 41);
            double n3 = noise(i * 281 + 67);
            double offset = n0 * 0.75;

            Vec3 position = advectedPoint(origin, forward, right, side, group,
                    n0, n1, n2, n3, seconds + offset, p, 1.62, 1.10, 0.62);
            Vec3 next = advectedPoint(origin, forward, right, side, group,
                    n0, n1, n2, n3, seconds + offset + SAMPLE_DT, p + SAMPLE_DP, 1.62, 1.10, 0.62);
            position = softenSafeVoid(position, origin, right.scale(side), 2.20);
            Vec3 tangent = next.subtract(position);
            if (tangent.lengthSqr() < 1.0E-7) tangent = forward;

            double flicker = 0.74
                    + 0.26 * Math.sin(seconds * (4.0 + n3 * 1.8) + n2 * TAU);
            double alpha = clamp(global * flicker, 0.0, 1.0);
            int face = BladeMesh.withAlpha(
                    (i % 17 == 0) ? 0x00FFF8FC : 0x00E59ABF,
                    alpha * 0.70);
            int edge = BladeMesh.withAlpha(
                    (i % 11 == 0) ? 0x00FFFFFF : 0x00F7C5DC,
                    alpha * 0.78);
            mesh.shard(position, tangent, right,
                    0.105 + n1 * 0.145,
                    0.030 + n3 * 0.040,
                    0.010 + n0 * 0.010,
                    face, edge);
        }
    }

    /**
     * Continuous field plus a persistent transport offset.
     * No petal owns a terminal endpoint, so no attack can leave a petal parked in mid-air.
     */
    private static Vec3 advectedPoint(
            Vec3 origin, Vec3 forward, Vec3 right,
            int side, int group,
            double n0, double n1, double n2, double n3,
            double seconds, double p,
            double speedScale, double spanScale, double attackScale) {
        Vec3 base = flowPoint(
                origin, forward, right, side,
                n0, n1, n2, n3,
                seconds, speedScale, spanScale);

        double delay = n3 * 0.018;
        Vec3 offset = Vec3.ZERO;

        if (group == 0 && side < 0) {
            double travel = smooth(0.570 + delay, 0.700 + delay, p);
            offset = offset
                    .add(right.scale((16.0 + n1 * 5.0) * travel * attackScale))
                    .add(forward.scale((2.0 + n0 * 4.0) * travel * attackScale))
                    .add(0.0, Math.sin(travel * Math.PI) * (1.0 + n2 * 2.0) * attackScale, 0.0);
        } else if (group == 1 && side > 0) {
            double travel = smooth(0.680 + delay, 0.810 + delay, p);
            offset = offset
                    .subtract(right.scale((16.0 + n1 * 5.0) * travel * attackScale))
                    .add(forward.scale((2.0 + n0 * 4.0) * travel * attackScale))
                    .add(0.0, Math.sin(travel * Math.PI) * (1.0 + n2 * 2.0) * attackScale, 0.0);
        } else if (group == 2 || group == 3) {
            double travel = smooth(0.795 + delay, 0.935 + delay, p);
            offset = offset
                    .add(forward.scale((11.0 + n0 * 8.0) * travel * attackScale))
                    .add(right.scale(-side * (2.0 + n1 * 3.5) * travel * attackScale))
                    .add(0.0, Math.sin(travel * Math.PI) * (0.8 + n2 * 1.6) * attackScale, 0.0);
        }

        return base.add(offset);
    }

    private static Vec3 flowPoint(
            Vec3 origin, Vec3 forward, Vec3 right,
            int side,
            double n0, double n1, double n2, double n3,
            double seconds, double speedScale, double spanScale) {
        double phase = n0 * TAU + seconds * (0.62 + n3 * 0.38) * speedScale;
        double slow = n2 * TAU + seconds * (0.20 + n1 * 0.11) * speedScale;
        double cross = n1 * TAU + seconds * (0.34 + n0 * 0.18) * speedScale;

        double longitudinal =
                Math.sin(phase * 0.66 + n2 * 2.1) * (16.0 * spanScale)
                + Math.sin(slow * 0.78) * (4.8 * spanScale)
                + Math.sin(cross * 0.42 + phase * 0.19) * 2.6;

        double lateral =
                side * (6.2 + n1 * 5.8) * spanScale
                + Math.cos(phase * 0.88 + n3) * (2.2 + n0 * 1.6)
                + Math.sin(slow * 0.94 + n2) * 1.25
                + Math.sin(cross * 0.57) * 0.9;

        double vertical =
                1.0 + n2 * (7.4 * spanScale)
                + Math.sin(phase * 1.03 + n1 * TAU) * (0.65 + n3 * 1.0)
                + Math.sin(slow * 1.27) * 0.48
                + Math.cos(cross * 0.71) * 0.32;

        return origin
                .add(forward.scale(longitudinal))
                .add(right.scale(lateral))
                .add(0.0, vertical, 0.0);
    }

    private static Vec3 softenSafeVoid(
            Vec3 point, Vec3 origin, Vec3 fallback, double radius) {
        Vec3 delta = point.subtract(origin);
        Vec3 flat = new Vec3(delta.x, 0.0, delta.z);
        double length = flat.length();
        if (length >= radius) return point;

        Vec3 direction = length < 1.0E-6
                ? fallback.normalize()
                : flat.scale(1.0 / length);
        double normalized = clamp(length / radius, 0.0, 1.0);
        double adjusted = radius * (0.82 + 0.18 * normalized);
        return new Vec3(
                origin.x + direction.x * adjusted,
                point.y,
                origin.z + direction.z * adjusted);
    }

    private static double smooth(double from, double to, double value) {
        if (to <= from) return value >= to ? 1.0 : 0.0;
        double t = clamp((value - from) / (to - from), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double easeIn(double t) {
        double c = clamp(t, 0.0, 1.0);
        return c * c * c;
    }

    private static double mix(double a, double b, double t) {
        return a + (b - a) * clamp(t, 0.0, 1.0);
    }

    private static double noise(int seed) {
        double value = Math.sin(seed * 12.9898 + 78.233) * 43758.5453123;
        return value - Math.floor(value);
    }

    private static Vec3 safeHorizontal(Vec3 direction) {
        Vec3 flat = new Vec3(direction.x, 0.0, direction.z);
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
                result.put(token.substring(0, split), token.substring(split + 1));
            }
        }
        return result;
    }

    private static double decimal(Map<String, String> values, String key, double fallback) {
        try { return Double.parseDouble(values.getOrDefault(key, Double.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static int integer(Map<String, String> values, String key, int fallback) {
        try { return Integer.parseInt(values.getOrDefault(key, Integer.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Visual(UUID caster, Vec3 origin, Vec3 facing, long startedAt, long expiresAt) {}
    private record RenderEntry(Vec3 origin, BladeMesh mesh) {}
}
