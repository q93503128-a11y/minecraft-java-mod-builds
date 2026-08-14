package kr.moonseungjun.senbonzakura.client;

import kr.moonseungjun.senbonzakura.SenbonzakuraShowcase;
import kr.moonseungjun.senbonzakura.bankai.BankaiFlowMath;
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
 * Filled pressure-wave support layer for Senbonzakura.
 *
 * Expansion, reverse contraction and lifetime fade are adapted from Goety-2's MIT-licensed
 * ShockwaveParticle and its rotated-ground presentation. Upstream reference:
 * https://github.com/Polarice3/Goety-2
 * commit e31b13045638ac7897b43d844ca3f3c6c50f1813
 * src/main/java/com/Polarice3/Goety/client/particles/ShockwaveParticle.java
 * src/main/java/com/Polarice3/Goety/client/particles/GroundCircleParticle.java
 *
 * No Goety texture or binary asset is copied. The imported timing math is rendered through this
 * project's existing filled custom geometry so the result is a physical annular pressure sheet,
 * not another speed-line effect.
 */
public final class ExternalShockwaveVfx {
    private static final ContextKey<List<RenderEntry>> DATA_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(SenbonzakuraShowcase.MOD_ID, "external_shockwave_v1"));
    private static final Map<UUID, Visual> ACTIVE = new HashMap<>();
    private static final int GEOMETRY_BUDGET = 6_400;
    private static final double MAX_DISTANCE_SQR = 224.0 * 224.0;
    private static final double TAU = Math.PI * 2.0;

    private ExternalShockwaveVfx() {}

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
        Vec3 facing = BankaiFlowMath.horizontal(new Vec3(
                decimal(values, "dx", 0.0),
                0.0,
                decimal(values, "dz", 1.0)));
        int duration = Math.max(80, integer(values, "duration", 260));
        long now = System.nanoTime();
        ACTIVE.put(caster, new Visual(
                origin,
                facing,
                now,
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
                    (visual.expiresAt() - visual.startedAt() - 850_000_000L) / 1_000_000_000.0);
            double seconds = Math.max(0.0, (now - visual.startedAt()) / 1_000_000_000.0);
            double progress = clamp(seconds / duration, 0.0, 1.0);
            BladeMesh mesh = build(visual, progress, seconds);
            entries.add(new RenderEntry(visual.origin(), mesh));
        }
        event.getRenderState().setRenderData(DATA_KEY, List.copyOf(entries));
    }

    public static void onSubmit(SubmitCustomGeometryEvent event) {
        List<RenderEntry> entries = event.getLevelRenderState().getRenderData(DATA_KEY);
        if (entries == null || entries.isEmpty()) return;

        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        float base = Minecraft.getInstance()
                .gameRenderer
                .gameRenderState()
                .windowRenderState
                .appropriateLineWidth;
        float lineScale = Math.max(0.70F, base * 0.72F);

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
        BladeMesh.Builder mesh = BladeMesh.builder(GEOMETRY_BUDGET);
        Vec3 localOrigin = Vec3.ZERO;

        // Sword-sink release: staggered filled sheets use Goety's growth/fade grammar.
        appendOutward(mesh, localOrigin, p, 0.055, 0.155, 17.0, 0.44, 0.52);
        appendOutward(mesh, localOrigin, p, 0.082, 0.182, 22.0, 0.36, 0.66);
        appendOutward(mesh, localOrigin, p, 0.112, 0.212, 28.0, 0.28, 0.82);

        // A reverse echo collapses toward the release point rather than adding another expanding ring.
        double reverse = window(p, 0.145, 0.275);
        if (reverse >= 0.0) {
            double radius = goetyReverse(reverse, 21.0, 3.25);
            double alpha = Math.sin(Math.PI * reverse) * 0.28;
            annulus(mesh, localOrigin.add(0.0, 0.050, 0.0), radius, 0.72, 96,
                    BladeMesh.withAlpha(0x00F1B4D2, alpha));
        }

        // The finale pressure wave is placed under the actual shared blade-river convergence rather
        // than at an arbitrary target point. Client geometry and server hit lanes already share
        // BankaiFlowMath, so this support effect follows the same choreography.
        double finalPhase = window(p, 0.810, 0.935);
        if (finalPhase >= 0.0) {
            Vec3 center = averageCurrentCenter(visual, seconds, p);
            center = new Vec3(center.x, 0.065, center.z);
            double radius = goetyExpand(finalPhase, 24.0);
            double alpha = goetyFade(finalPhase) * 0.34;
            annulus(mesh, center, radius, 0.94 + finalPhase * 0.42, 112,
                    BladeMesh.withAlpha(0x00FFD6E8, alpha));
            if (finalPhase > 0.09) {
                double secondary = clamp((finalPhase - 0.09) / 0.91, 0.0, 1.0);
                annulus(mesh, center, goetyExpand(secondary, 30.0), 0.56, 112,
                        BladeMesh.withAlpha(0x00B775A2, goetyFade(secondary) * 0.22));
            }
        }
        return mesh.build();
    }

    private static void appendOutward(
            BladeMesh.Builder mesh,
            Vec3 center,
            double p,
            double start,
            double end,
            double sourceSize,
            double peakAlpha,
            double width) {
        double phase = window(p, start, end);
        if (phase < 0.0) return;
        double radius = goetyExpand(phase, sourceSize);
        double alpha = goetyFade(phase) * peakAlpha;
        annulus(mesh, center.add(0.0, 0.042, 0.0), radius, width + phase * 0.34, 96,
                BladeMesh.withAlpha(0x00F6C6DD, alpha));
    }

    /** Goety ShockwaveParticle normal-size grammar: size * clamp(lifeRatio * 0.75, 0, 2). */
    private static double goetyExpand(double lifeRatio, double sourceSize) {
        return sourceSize * clamp(lifeRatio * 0.75, 0.0, 2.0);
    }

    /** Goety ShockwaveParticle fade grammar. */
    private static double goetyFade(double lifeRatio) {
        return 1.0 - clamp(lifeRatio, 0.0, 1.0);
    }

    /** Goety reverse-size grammar: max(originSize / (age + 1), floorSize). */
    private static double goetyReverse(double lifeRatio, double originSize, double floorSize) {
        double simulatedAge = clamp(lifeRatio, 0.0, 1.0) * 11.0;
        return Math.max(originSize / (simulatedAge + 1.0), floorSize);
    }

    private static Vec3 averageCurrentCenter(Visual visual, double seconds, double progress) {
        Vec3 sum = Vec3.ZERO;
        for (int cluster = 0; cluster < BankaiFlowMath.CLUSTER_COUNT; cluster++) {
            Vec3 world = BankaiFlowMath.currentCenter(
                    visual.origin(), visual.facing(), cluster, seconds, progress, 1.0);
            sum = sum.add(world.subtract(visual.origin()));
        }
        return sum.scale(1.0 / BankaiFlowMath.CLUSTER_COUNT);
    }

    private static void annulus(
            BladeMesh.Builder mesh,
            Vec3 center,
            double radius,
            double width,
            int segments,
            int color) {
        if (radius <= 0.02 || width <= 0.01) return;
        int count = Math.max(24, segments);
        double inner = Math.max(0.02, radius - width * 0.5);
        double outer = radius + width * 0.5;
        for (int i = 0; i < count && !mesh.full(); i++) {
            double a0 = TAU * i / count;
            double a1 = TAU * (i + 1) / count;
            Vec3 o0 = center.add(Math.cos(a0) * outer, 0.0, Math.sin(a0) * outer);
            Vec3 o1 = center.add(Math.cos(a1) * outer, 0.0, Math.sin(a1) * outer);
            Vec3 i1 = center.add(Math.cos(a1) * inner, 0.0, Math.sin(a1) * inner);
            Vec3 i0 = center.add(Math.cos(a0) * inner, 0.0, Math.sin(a0) * inner);
            mesh.quad(o0, o1, i1, i0, color);
        }
    }

    private static double window(double p, double start, double end) {
        if (p < start || p > end || end <= start) return -1.0;
        return clamp((p - start) / (end - start), 0.0, 1.0);
    }

    private static Map<String, String> parse(String raw) {
        Map<String, String> values = new HashMap<>();
        for (String part : raw.split(";")) {
            int equals = part.indexOf('=');
            if (equals <= 0 || equals >= part.length() - 1) continue;
            values.put(part.substring(0, equals), part.substring(equals + 1));
        }
        return values;
    }

    private static double decimal(Map<String, String> values, String key, double fallback) {
        try {
            return Double.parseDouble(values.getOrDefault(key, Double.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int integer(Map<String, String> values, String key, int fallback) {
        try {
            return Integer.parseInt(values.getOrDefault(key, Integer.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Visual(Vec3 origin, Vec3 facing, long startedAt, long expiresAt) {}
    private record RenderEntry(Vec3 origin, BladeMesh mesh) {}
}
