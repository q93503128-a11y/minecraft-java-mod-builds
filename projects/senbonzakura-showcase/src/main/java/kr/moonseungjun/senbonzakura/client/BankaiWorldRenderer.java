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
 * Client-only bankai presentation. Hundreds of visible shards are geometry, not entities.
 */
public final class BankaiWorldRenderer {
    private static final ContextKey<List<RenderEntry>> DATA_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(SenbonzakuraShowcase.MOD_ID, "bankai_geometry_v1"));
    private static final Map<UUID, Visual> ACTIVE = new HashMap<>();
    private static final int MAX_GEOMETRY = 18_000;
    private static final double MAX_DISTANCE_SQR = 176.0 * 176.0;

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

        Vec3 origin = new Vec3(decimal(values, "x", 0.0), decimal(values, "y", 0.0), decimal(values, "z", 0.0));
        Vec3 facing = safeHorizontal(new Vec3(decimal(values, "dx", 0.0), 0.0, decimal(values, "dz", 1.0)));
        int duration = Math.max(40, integer(values, "duration", 200));
        long now = System.nanoTime();
        ACTIVE.put(caster, new Visual(caster, origin, facing, now, now + duration * 50_000_000L + 600_000_000L));
    }

    public static void onExtract(ExtractLevelRenderStateEvent event) {
        long now = System.nanoTime();
        ACTIVE.values().removeIf(visual -> visual.expiresAt() < now);
        if (ACTIVE.isEmpty()) return;
        List<RenderEntry> entries = new ArrayList<>(ACTIVE.size());
        for (Visual visual : ACTIVE.values()) {
            double duration = Math.max(0.1, (visual.expiresAt() - visual.startedAt() - 600_000_000L) / 1_000_000_000.0);
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
        float base = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
        float lineScale = Math.max(0.70F, base * 0.82F);
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

        appendFallingZanpakuto(mesh, origin, forward, right, p);
        appendGroundRipple(mesh, origin, p);
        appendBladeRows(mesh, origin, forward, right, p);
        appendBladeStorm(mesh, origin, forward, right, p, seconds);
        appendFinalCut(mesh, origin, forward, right, p);
        return mesh.build();
    }

    private static void appendFallingZanpakuto(BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right, double p) {
        double appear = 1.0 - smooth(0.115, 0.155, p);
        if (appear <= 0.001) return;
        double drop = smooth(0.0, 0.105, p);
        double y = mix(5.4, 0.72, easeIn(drop));
        Vec3 guard = origin.add(forward.scale(1.15)).add(0.0, y, 0.0);
        mesh.katana(guard, new Vec3(0.0, -1.0, 0.0), right, 0.94, (int) Math.round(255.0 * appear));
    }

    private static void appendGroundRipple(BladeMesh.Builder mesh, Vec3 origin, double p) {
        double phase = smooth(0.065, 0.22, p) * (1.0 - smooth(0.20, 0.32, p));
        if (phase <= 0.001) return;
        double radius = 0.35 + smooth(0.065, 0.24, p) * 6.6;
        int outer = BladeMesh.withAlpha(0x00A23F74, phase * 0.38);
        int core = BladeMesh.withAlpha(0x00F7B6D5, phase * 0.95);
        mesh.groundRing(origin.add(0.0, 0.035, 0.0), radius, 72, outer, core, 1.05F);
        if (radius > 1.6) mesh.groundRing(origin.add(0.0, 0.028, 0.0), radius * 0.63, 52,
                BladeMesh.withAlpha(0x007D2B58, phase * 0.25), BladeMesh.withAlpha(0x00ED96C2, phase * 0.70), 0.74F);
    }

    private static void appendBladeRows(BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right, double p) {
        int perSide = 17;
        for (int sideSign : new int[]{-1, 1}) {
            for (int i = 0; i < perSide && !mesh.full(); i++) {
                double delay = i * 0.0042;
                double rise = smooth(0.105 + delay, 0.235 + delay, p);
                double dissolve = smooth(0.285 + delay * 0.32, 0.405 + delay * 0.22, p);
                double alpha = rise * (1.0 - dissolve);
                if (alpha <= 0.002) continue;

                double longitudinal = (i - (perSide - 1) * 0.5) * 1.08;
                double lateral = sideSign * (3.15 + 0.18 * Math.sin(i * 0.83));
                double height = -5.0 + rise * 5.0;
                Vec3 base = origin.add(forward.scale(longitudinal)).add(right.scale(lateral)).add(0.0, height, 0.0);
                double length = 6.6 + noise(i * 31 + sideSign * 7) * 1.6;
                double width = 0.62 + noise(i * 17 + 5) * 0.22;
                int face = BladeMesh.withAlpha(0x00E9E7EF, alpha * 0.90);
                int edge = BladeMesh.withAlpha(0x00FF9DCC, alpha);
                mesh.longBlade(base, new Vec3(0.0, 1.0, 0.0), right, length, width, 0.14, face, edge);
            }
        }
    }

    private static void appendBladeStorm(BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right,
                                         double p, double seconds) {
        double emerge = smooth(0.27, 0.39, p);
        double vanish = 1.0 - smooth(0.89, 0.985, p);
        double globalAlpha = emerge * vanish;
        if (globalAlpha <= 0.001) return;

        int count = 360;
        double attackMaster = smooth(0.47, 0.79, p);
        double release = smooth(0.76, 0.89, p);
        for (int i = 0; i < count && !mesh.full(); i++) {
            double n0 = noise(i * 73 + 11);
            double n1 = noise(i * 151 + 23);
            double n2 = noise(i * 211 + 47);
            double n3 = noise(i * 307 + 59);
            double spin = (i & 1) == 0 ? 1.0 : -1.0;
            double angle = n0 * Math.PI * 2.0 + seconds * (0.62 + n1 * 0.86) * spin;
            double radius = 2.7 + n1 * 10.4;
            double vertical = 0.45 + n2 * 7.0 + Math.sin(angle * 1.7 + i) * 0.38;
            Vec3 orbit = origin.add(Math.cos(angle) * radius, vertical, Math.sin(angle) * radius);

            double lane = (i % 18) / 17.0;
            double localAttack = smooth(0.46 + lane * 0.105, 0.70 + lane * 0.08, p);
            localAttack *= attackMaster;
            Vec3 focus = origin.add(forward.scale(5.4 + n2 * 4.0))
                    .add(right.scale((n0 - 0.5) * 2.3))
                    .add(0.0, 0.8 + n3 * 3.0, 0.0);
            Vec3 attackPos = bezier(orbit,
                    origin.add(right.scale((n3 - 0.5) * 12.0)).add(forward.scale(2.0 + n1 * 3.0)).add(0.0, 4.5 + n2 * 4.0, 0.0),
                    focus, localAttack);
            Vec3 through = focus.add(forward.scale(3.0 + n1 * 5.0))
                    .add(right.scale((n3 - 0.5) * 3.5));
            Vec3 position = attackPos.lerp(through, release * (0.32 + 0.68 * localAttack));
            position = enforceSafeVoid(position, origin, right, 2.25);

            Vec3 tangent;
            if (localAttack > 0.04) tangent = focus.subtract(position);
            else tangent = new Vec3(-Math.sin(angle) * spin, 0.09 * Math.cos(angle * 2.0 + i), Math.cos(angle) * spin);
            if (tangent.lengthSqr() < 1.0E-7) tangent = forward;

            double flicker = 0.72 + 0.28 * Math.sin(seconds * 11.0 + i * 1.31);
            double alpha = clamp(globalAlpha * flicker, 0.0, 1.0);
            int face = BladeMesh.withAlpha((i % 5 == 0) ? 0x00FFF4FA : 0x00D96EAA, alpha * ((i % 7 == 0) ? 0.96 : 0.74));
            int edge = BladeMesh.withAlpha((i % 4 == 0) ? 0x00FFFFFF : 0x00FF9BCB, alpha);
            double length = 0.19 + n2 * 0.27;
            double width = 0.055 + n3 * 0.075;
            mesh.shard(position, tangent, right, length, width, 0.018 + n0 * 0.018, face, edge);

            if (localAttack > 0.48 && i % 14 == 0) {
                Vec3 streakBack = position.subtract(tangent.normalize().scale(0.9 + n1 * 1.5));
                mesh.glowEdge(streakBack, position,
                        BladeMesh.withAlpha(0x008A245B, alpha * 0.22),
                        BladeMesh.withAlpha(0x00FFD0E7, alpha * 0.78), 0.72F);
            }
        }
    }

    private static void appendFinalCut(BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right, double p) {
        double phase = smooth(0.845, 0.915, p) * (1.0 - smooth(0.94, 0.995, p));
        if (phase <= 0.001) return;
        Vec3 focus = origin.add(forward.scale(7.2)).add(0.0, 1.55, 0.0);
        int outer = BladeMesh.withAlpha(0x00811F53, phase * 0.36);
        int core = BladeMesh.withAlpha(0x00FFF2F9, phase);
        for (int i = 0; i < 28 && !mesh.full(); i++) {
            double angle = Math.PI * 2.0 * i / 28.0 + p * 12.0;
            Vec3 radial = right.scale(Math.cos(angle) * (3.4 + (i % 3) * 0.45))
                    .add(0.0, Math.sin(angle) * (2.2 + (i % 4) * 0.20), 0.0);
            Vec3 start = focus.add(radial).subtract(forward.scale(1.4 + (i % 5) * 0.24));
            Vec3 end = focus.subtract(radial.scale(0.18)).add(forward.scale(1.2));
            mesh.glowEdge(start, end, outer, core, 0.90F);
        }
        double ringRadius = 0.8 + smooth(0.86, 0.93, p) * 4.1;
        mesh.groundRing(origin.add(forward.scale(7.2)).add(0.0, 0.05, 0.0), ringRadius, 64, outer, core, 0.86F);
    }

    private static Vec3 enforceSafeVoid(Vec3 point, Vec3 origin, Vec3 fallback, double radius) {
        Vec3 delta = point.subtract(origin);
        Vec3 flat = new Vec3(delta.x, 0.0, delta.z);
        double length = flat.length();
        if (length >= radius) return point;
        Vec3 direction = length < 1.0E-6 ? fallback : flat.scale(1.0 / length);
        return new Vec3(origin.x + direction.x * radius, point.y, origin.z + direction.z * radius);
    }

    private static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, double t) {
        double u = 1.0 - clamp(t, 0.0, 1.0);
        double tt = 1.0 - u;
        return a.scale(u * u).add(b.scale(2.0 * u * tt)).add(c.scale(tt * tt));
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
        return flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    private static Map<String, String> parse(String state) {
        Map<String, String> result = new HashMap<>();
        if (state == null || state.isBlank()) return result;
        for (String token : state.split(";")) {
            int split = token.indexOf('=');
            if (split <= 0) continue;
            result.put(token.substring(0, split), token.substring(split + 1));
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
