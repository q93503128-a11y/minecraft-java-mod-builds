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
 * Second-generation presentation for Senbonzakura Kageyoshi.
 *
 * The scene is deliberately composed as a ritual, not as a generic particle storm:
 * sword release -> ground ripple -> monumental blade rows -> silence -> dissolution -> blade torrents.
 * All visible fragments are client geometry; no visual shard is an entity.
 */
public final class BankaiWorldRenderer {
    private static final ContextKey<List<RenderEntry>> DATA_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(SenbonzakuraShowcase.MOD_ID, "bankai_geometry_v3"));
    private static final Map<UUID, Visual> ACTIVE = new HashMap<>();
    private static final int MAX_GEOMETRY = 19_000;
    private static final double MAX_DISTANCE_SQR = 196.0 * 196.0;

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
        int duration = Math.max(80, integer(values, "duration", 260));
        long now = System.nanoTime();
        ACTIVE.put(caster, new Visual(caster, origin, facing, now, now + duration * 50_000_000L + 800_000_000L));
    }

    public static void onExtract(ExtractLevelRenderStateEvent event) {
        long now = System.nanoTime();
        ACTIVE.values().removeIf(visual -> visual.expiresAt() < now);
        if (ACTIVE.isEmpty()) return;

        List<RenderEntry> entries = new ArrayList<>(ACTIVE.size());
        for (Visual visual : ACTIVE.values()) {
            double duration = Math.max(0.1, (visual.expiresAt() - visual.startedAt() - 800_000_000L) / 1_000_000_000.0);
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
        float lineScale = Math.max(0.70F, base * 0.78F);
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
        appendDisintegration(mesh, origin, forward, right, p, seconds);
        appendBladeSea(mesh, origin, forward, right, p, seconds);
        appendTorrentHighlights(mesh, origin, forward, right, p);
        return mesh.build();
    }

    private static void appendAtmosphere(BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right, double p) {
        double fadeIn = smooth(0.075, 0.23, p);
        double fadeOut = 1.0 - smooth(0.92, 1.0, p);
        double alpha = fadeIn * fadeOut;
        if (alpha <= 0.001) return;

        int floor = BladeMesh.withAlpha(0x0004070E, alpha * 0.24);
        double half = 24.0;
        Vec3 a = origin.add(forward.scale(half)).add(right.scale(half)).add(0.0, 0.022, 0.0);
        Vec3 b = origin.add(forward.scale(half)).subtract(right.scale(half)).add(0.0, 0.022, 0.0);
        Vec3 c = origin.subtract(forward.scale(half)).subtract(right.scale(half)).add(0.0, 0.022, 0.0);
        Vec3 d = origin.subtract(forward.scale(half)).add(right.scale(half)).add(0.0, 0.022, 0.0);
        mesh.quad(a, b, c, d, floor);

        int curtain = BladeMesh.withAlpha(0x00070A15, alpha * 0.12);
        double h = 16.0;
        verticalCurtain(mesh, origin.add(right.scale(half)), forward, h, half * 2.0, curtain);
        verticalCurtain(mesh, origin.subtract(right.scale(half)), forward, h, half * 2.0, curtain);
        verticalCurtain(mesh, origin.add(forward.scale(half)), right, h, half * 2.0, curtain);
        verticalCurtain(mesh, origin.subtract(forward.scale(half)), right, h, half * 2.0, curtain);
    }

    private static void verticalCurtain(BladeMesh.Builder mesh, Vec3 center, Vec3 axis, double height, double width, int color) {
        Vec3 halfAxis = axis.normalize().scale(width * 0.5);
        Vec3 a = center.add(halfAxis);
        Vec3 b = center.subtract(halfAxis);
        Vec3 c = b.add(0.0, height, 0.0);
        Vec3 d = a.add(0.0, height, 0.0);
        mesh.quad(a, b, c, d, color);
    }

    private static void appendReleasedSword(BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right, double p) {
        double visible = 1.0 - smooth(0.135, 0.165, p);
        if (visible <= 0.001) return;

        double drop = smooth(0.0, 0.088, p);
        double sink = smooth(0.088, 0.155, p);
        double y = mix(1.76, 0.48, easeIn(drop)) - sink * 4.4;
        Vec3 guard = origin.add(forward.scale(0.82)).add(0.0, y, 0.0);
        int alpha = (int) Math.round(255.0 * visible);
        mesh.katana(guard, new Vec3(0.0, -1.0, 0.0), right, 0.78, alpha);
    }

    private static void appendGroundRipples(BladeMesh.Builder mesh, Vec3 origin, double p) {
        double base = smooth(0.075, 0.18, p) * (1.0 - smooth(0.23, 0.32, p));
        if (base <= 0.001) return;

        for (int i = 0; i < 3; i++) {
            double local = smooth(0.075 + i * 0.018, 0.20 + i * 0.018, p)
                    * (1.0 - smooth(0.205 + i * 0.020, 0.30 + i * 0.020, p));
            if (local <= 0.001) continue;
            double radius = 0.22 + local * (2.6 + i * 0.78);
            int outer = BladeMesh.withAlpha(0x005D183E, local * 0.22);
            int core = BladeMesh.withAlpha(0x00F4D9E7, local * (0.72 - i * 0.12));
            mesh.groundRing(origin.add(0.0, 0.035 + i * 0.003, 0.0), radius, 62, outer, core, 0.82F - i * 0.10F);
        }
    }

    private static void appendMonumentalRows(BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right, double p) {
        final int perSide = 42;
        final double middle = (perSide - 1) * 0.5;
        for (int side : new int[]{-1, 1}) {
            for (int i = 0; i < perSide && !mesh.full(); i++) {
                double distanceFromCenter = Math.abs(i - middle) / middle;
                double delay = distanceFromCenter * 0.030;
                double rise = smooth(0.115 + delay, 0.305 + delay, p);
                double dissolve = smooth(0.405 + delay * 0.20, 0.535 + delay * 0.14, p);
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
                Vec3 base = origin.add(forward.scale(longitudinal)).add(right.scale(lateral)).add(0.0, baseY, 0.0);
                Vec3 axis = new Vec3(side * 0.025, 1.0, 0.0).normalize();
                int face = BladeMesh.withAlpha(0x00E8E7EB, alpha * 0.92);
                int edge = BladeMesh.withAlpha(0x00FFE6F1, alpha * 0.98);
                mesh.longBlade(base, axis, right, length, width, 0.13, face, edge);
            }
        }
    }

    private static void appendDisintegration(BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right,
                                             double p, double seconds) {
        double phase = smooth(0.385, 0.50, p) * (1.0 - smooth(0.535, 0.62, p));
        if (phase <= 0.001) return;

        final int count = 150;
        for (int i = 0; i < count && !mesh.full(); i++) {
            int side = (i & 1) == 0 ? -1 : 1;
            double n0 = noise(i * 83 + 7);
            double n1 = noise(i * 149 + 19);
            double n2 = noise(i * 227 + 31);
            double longitudinal = (n0 - 0.5) * 43.0;
            double lateral = side * (3.5 + n1 * 3.8 + phase * (1.2 + n2 * 2.2));
            double y = 0.35 + n2 * 9.0 + phase * (0.6 + n0 * 2.1);
            Vec3 pos = origin.add(forward.scale(longitudinal)).add(right.scale(lateral)).add(0.0, y, 0.0);
            Vec3 tangent = forward.scale((n0 - 0.5) * 0.55)
                    .add(right.scale(side * (0.7 + n1 * 0.9)))
                    .add(0.0, 0.15 + n2 * 0.25, 0.0);
            double flicker = 0.72 + 0.28 * Math.sin(seconds * 8.0 + i * 1.37);
            int face = BladeMesh.withAlpha((i % 6 == 0) ? 0x00FFF8FC : 0x00D68BAF, phase * flicker * 0.76);
            int edge = BladeMesh.withAlpha(0x00FFEAF4, phase * flicker * 0.90);
            mesh.shard(pos, tangent, right, 0.22 + n0 * 0.32, 0.05 + n1 * 0.07,
                    0.018 + n2 * 0.014, face, edge);
        }
    }

    private static void appendBladeSea(BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right,
                                       double p, double seconds) {
        double appear = smooth(0.445, 0.56, p);
        double vanish = 1.0 - smooth(0.93, 1.0, p);
        double global = appear * vanish;
        if (global <= 0.001) return;

        final int count = 520;
        for (int i = 0; i < count && !mesh.full(); i++) {
            int side = (i & 1) == 0 ? -1 : 1;
            int group = (i / 2) % 4;
            double n0 = noise(i * 71 + 13);
            double n1 = noise(i * 137 + 29);
            double n2 = noise(i * 211 + 47);
            double n3 = noise(i * 307 + 61);

            double longitudinal = (n0 - 0.5) * 44.0;
            double lateral = side * (5.1 + n1 * 7.3);
            double vertical = 0.45 + n2 * 10.4;
            double drift = Math.sin(seconds * (0.42 + n3 * 0.20) + i * 0.73) * 0.38;
            Vec3 start = origin.add(forward.scale(longitudinal + drift))
                    .add(right.scale(lateral))
                    .add(0.0, vertical, 0.0);

            double t = 0.0;
            Vec3 control = start;
            Vec3 end = start;
            if (group == 0 && side < 0) {
                t = smooth(0.565 + n3 * 0.022, 0.675 + n3 * 0.020, p);
                control = origin.add(forward.scale((n0 - 0.5) * 15.0)).add(right.scale(-0.8)).add(0.0, 4.8 + n2 * 4.0, 0.0);
                end = origin.add(forward.scale((n0 - 0.5) * 18.0)).add(right.scale(9.0 + n1 * 3.0)).add(0.0, 0.9 + n2 * 5.6, 0.0);
            } else if (group == 1 && side > 0) {
                t = smooth(0.675 + n3 * 0.022, 0.785 + n3 * 0.020, p);
                control = origin.add(forward.scale((n0 - 0.5) * 15.0)).add(right.scale(0.8)).add(0.0, 4.6 + n2 * 4.2, 0.0);
                end = origin.add(forward.scale((n0 - 0.5) * 18.0)).add(right.scale(-9.0 - n1 * 3.0)).add(0.0, 0.9 + n2 * 5.6, 0.0);
            } else if (group >= 2) {
                t = smooth(0.79 + n3 * 0.025, 0.915 + n3 * 0.018, p);
                control = origin.add(forward.scale(3.0 + n0 * 8.0)).add(right.scale(side * (2.4 + n1 * 2.4))).add(0.0, 5.2 + n2 * 3.6, 0.0);
                end = origin.add(forward.scale(12.0 + n0 * 8.0)).add(right.scale((n1 - 0.5) * 6.0)).add(0.0, 0.8 + n2 * 5.8, 0.0);
            }

            Vec3 position = t <= 0.001 ? start : bezier(start, control, end, easeOut(t));
            position = enforceSafeVoid(position, origin, right.scale(side), 2.45);

            Vec3 tangent;
            if (t > 0.02) {
                tangent = end.subtract(position);
            } else {
                tangent = forward.scale(0.62 + n0 * 0.36)
                        .add(right.scale(side * (0.16 + n1 * 0.24)))
                        .add(0.0, (n2 - 0.5) * 0.16, 0.0);
            }
            if (tangent.lengthSqr() < 1.0E-7) tangent = forward;

            double brightness = 0.68 + 0.32 * Math.sin(seconds * 8.5 + i * 1.19);
            double alpha = clamp(global * brightness, 0.0, 1.0);
            int face = BladeMesh.withAlpha((i % 9 == 0) ? 0x00FFFDFE : 0x00D98CAF, alpha * ((i % 5 == 0) ? 0.88 : 0.68));
            int edge = BladeMesh.withAlpha((i % 7 == 0) ? 0x00FFFFFF : 0x00F9D5E6, alpha * 0.92);
            double length = 0.20 + n2 * 0.30;
            double width = 0.050 + n3 * 0.072;
            mesh.shard(position, tangent, right, length, width, 0.016 + n0 * 0.018, face, edge);
        }
    }

    private static void appendTorrentHighlights(BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right, double p) {
        double leftWave = pulse(p, 0.565, 0.675, 0.735);
        double rightWave = pulse(p, 0.675, 0.785, 0.845);
        double converge = pulse(p, 0.79, 0.915, 0.965);

        if (leftWave > 0.001) {
            for (int i = 0; i < 10 && !mesh.full(); i++) {
                double lane = (i - 4.5) * 1.55;
                Vec3 a = origin.add(forward.scale(lane)).add(right.scale(-12.0)).add(0.0, 1.0 + (i % 4) * 1.35, 0.0);
                Vec3 b = origin.add(forward.scale(lane * 0.75)).add(right.scale(10.0)).add(0.0, 1.4 + (i % 5) * 1.15, 0.0);
                mesh.glowEdge(a, b, BladeMesh.withAlpha(0x005A193C, leftWave * 0.18),
                        BladeMesh.withAlpha(0x00F7C8DE, leftWave * 0.62), 1.08F);
            }
        }
        if (rightWave > 0.001) {
            for (int i = 0; i < 10 && !mesh.full(); i++) {
                double lane = (i - 4.5) * 1.55;
                Vec3 a = origin.add(forward.scale(lane)).add(right.scale(12.0)).add(0.0, 1.0 + (i % 4) * 1.35, 0.0);
                Vec3 b = origin.add(forward.scale(lane * 0.75)).add(right.scale(-10.0)).add(0.0, 1.4 + (i % 5) * 1.15, 0.0);
                mesh.glowEdge(a, b, BladeMesh.withAlpha(0x005A193C, rightWave * 0.18),
                        BladeMesh.withAlpha(0x00F7C8DE, rightWave * 0.62), 1.08F);
            }
        }
        if (converge > 0.001) {
            Vec3 focus = origin.add(forward.scale(15.0)).add(0.0, 2.2, 0.0);
            for (int side : new int[]{-1, 1}) {
                for (int i = 0; i < 12 && !mesh.full(); i++) {
                    Vec3 a = origin.add(right.scale(side * (7.0 + (i % 5) * 1.4)))
                            .add(forward.scale((i - 5.5) * 1.7)).add(0.0, 0.9 + (i % 4) * 1.45, 0.0);
                    Vec3 b = focus.add(right.scale((i - 5.5) * 0.32)).add(0.0, (i % 3) * 0.55, 0.0);
                    mesh.glowEdge(a, b, BladeMesh.withAlpha(0x006B2047, converge * 0.18),
                            BladeMesh.withAlpha(0x00FFE4F0, converge * 0.72), 0.96F);
                }
            }
        }
    }

    private static double pulse(double value, double in, double peakEnd, double out) {
        return smooth(in, peakEnd, value) * (1.0 - smooth(peakEnd, out, value));
    }

    private static Vec3 enforceSafeVoid(Vec3 point, Vec3 origin, Vec3 fallback, double radius) {
        Vec3 delta = point.subtract(origin);
        Vec3 flat = new Vec3(delta.x, 0.0, delta.z);
        double length = flat.length();
        if (length >= radius) return point;
        Vec3 direction = length < 1.0E-6 ? fallback.normalize() : flat.scale(1.0 / length);
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

    private static double easeOut(double t) {
        double c = clamp(t, 0.0, 1.0);
        double inv = 1.0 - c;
        return 1.0 - inv * inv * inv;
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
