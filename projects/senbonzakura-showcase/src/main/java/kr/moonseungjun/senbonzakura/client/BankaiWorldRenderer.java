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
 * Third-generation Senbonzakura Kageyoshi presentation.
 *
 * The key visual rule is force: monumental blades visibly break apart into medium fragments,
 * those fragments resolve into three layers of blade-petals, and the layers are driven by
 * a turbulent field with short compression beats and violent acceleration windows.
 */
public final class BankaiWorldRenderer {
    private static final ContextKey<List<RenderEntry>> DATA_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(SenbonzakuraShowcase.MOD_ID, "bankai_geometry_v4"));
    private static final Map<UUID, Visual> ACTIVE = new HashMap<>();
    private static final int MAX_GEOMETRY = 26_000;
    private static final double MAX_DISTANCE_SQR = 208.0 * 208.0;

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
        ACTIVE.put(caster, new Visual(caster, origin, facing, now, now + duration * 50_000_000L + 850_000_000L));
    }

    public static void onExtract(ExtractLevelRenderStateEvent event) {
        long now = System.nanoTime();
        ACTIVE.values().removeIf(visual -> visual.expiresAt() < now);
        if (ACTIVE.isEmpty()) return;

        List<RenderEntry> entries = new ArrayList<>(ACTIVE.size());
        for (Visual visual : ACTIVE.values()) {
            double duration = Math.max(0.1, (visual.expiresAt() - visual.startedAt() - 850_000_000L) / 1_000_000_000.0);
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
        appendBladeBreak(mesh, origin, forward, right, p, seconds);
        appendFlowPetals(mesh, origin, forward, right, p, seconds);
        appendCorePetals(mesh, origin, forward, right, p, seconds);
        appendDustPetals(mesh, origin, forward, right, p, seconds);
        appendTorrentHighlights(mesh, origin, forward, right, p);
        return mesh.build();
    }

    private static void appendAtmosphere(BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right, double p) {
        double fadeIn = smooth(0.075, 0.23, p);
        double fadeOut = 1.0 - smooth(0.94, 1.0, p);
        double alpha = fadeIn * fadeOut;
        if (alpha <= 0.001) return;

        int floor = BladeMesh.withAlpha(0x0004070E, alpha * 0.27);
        double half = 25.0;
        Vec3 a = origin.add(forward.scale(half)).add(right.scale(half)).add(0.0, 0.022, 0.0);
        Vec3 b = origin.add(forward.scale(half)).subtract(right.scale(half)).add(0.0, 0.022, 0.0);
        Vec3 c = origin.subtract(forward.scale(half)).subtract(right.scale(half)).add(0.0, 0.022, 0.0);
        Vec3 d = origin.subtract(forward.scale(half)).add(right.scale(half)).add(0.0, 0.022, 0.0);
        mesh.quad(a, b, c, d, floor);
    }

    private static void appendReleasedSword(BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right, double p) {
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
                    BladeMesh.withAlpha(0x00F4D9E7, local * (0.72 - i * 0.12)), 0.82F - i * 0.10F);
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
                double crack = smooth(0.372 + delay * 0.12, 0.455 + delay * 0.08, p);
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
                Vec3 base = origin.add(forward.scale(longitudinal)).add(right.scale(lateral)).add(0.0, baseY, 0.0);
                Vec3 axis = new Vec3(side * 0.025, 1.0, 0.0).normalize();
                int face = BladeMesh.withAlpha(0x00E8E7EB, alpha * (0.94 - crack * 0.20));
                int edge = BladeMesh.withAlpha(crack > 0.02 ? 0x00FFB8D6 : 0x00FFE6F1,
                        alpha * (0.98 + crack * 0.02));
                mesh.longBlade(base, axis, right, length, width, 0.13, face, edge);

                if (crack > 0.03) {
                    for (int s = 0; s < 3 && !mesh.full(); s++) {
                        double y = length * (0.28 + s * 0.22);
                        Vec3 ca = base.add(axis.scale(y)).subtract(right.scale(width * 0.42));
                        Vec3 cb = base.add(axis.scale(y + 0.7 + n1 * 0.55)).add(right.scale(width * 0.40));
                        mesh.glowEdge(ca, cb,
                                BladeMesh.withAlpha(0x00681844, crack * alpha * 0.25),
                                BladeMesh.withAlpha(0x00FFD4E7, crack * alpha * 0.90), 0.58F);
                    }
                }
            }
        }
    }

    private static void appendBladeBreak(BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right,
                                         double p, double seconds) {
        double master = smooth(0.392, 0.505, p) * (1.0 - smooth(0.575, 0.655, p));
        if (master <= 0.001) return;

        final int count = 320;
        for (int i = 0; i < count && !mesh.full(); i++) {
            int side = (i & 1) == 0 ? -1 : 1;
            int bladeIndex = (i / 2) % 42;
            int layer = (i / 84) % 4;
            double n0 = noise(i * 83 + 7);
            double n1 = noise(i * 149 + 19);
            double n2 = noise(i * 227 + 31);
            double row = (bladeIndex - 20.5) * 1.10;
            double baseLateral = side * (3.7 + Math.pow(Math.abs(bladeIndex - 20.5) / 20.5, 1.7) * 1.5);
            double localStart = 0.398 + layer * 0.020 + n0 * 0.012;
            double burst = smooth(localStart, localStart + 0.105, p);
            double fade = 1.0 - smooth(0.57 + n1 * 0.015, 0.655 + n1 * 0.012, p);
            double alpha = burst * fade;
            if (alpha <= 0.002) continue;

            double height = 1.0 + layer * 2.15 + n2 * 2.0;
            double eject = fastEase(burst);
            double curl = seconds * (1.4 + n1 * 1.1) + i * 0.31;
            Vec3 pos = origin.add(forward.scale(row + Math.sin(curl) * eject * 0.95))
                    .add(right.scale(baseLateral + side * eject * (1.1 + n1 * 3.6)))
                    .add(0.0, height + eject * (0.45 + n0 * 2.2), 0.0);
            Vec3 tangent = right.scale(side * (1.2 + n1 * 1.5))
                    .add(forward.scale(Math.cos(curl) * (0.45 + n0 * 0.9)))
                    .add(0.0, 0.18 + n2 * 0.34, 0.0);
            int face = BladeMesh.withAlpha((i % 7 == 0) ? 0x00FFF9FC : 0x00D985AD, alpha * 0.86);
            int edge = BladeMesh.withAlpha(0x00FFE7F2, alpha);
            double length = 0.34 + n0 * 0.48;
            mesh.shard(pos, tangent, right, length, 0.075 + n1 * 0.105, 0.022 + n2 * 0.020, face, edge);
            if (i % 5 == 0) {
                Vec3 tail = pos.subtract(tangent.normalize().scale(0.45 + n1 * 0.95));
                mesh.glowEdge(tail, pos,
                        BladeMesh.withAlpha(0x00741B4A, alpha * 0.20),
                        BladeMesh.withAlpha(0x00FFD0E6, alpha * 0.72), 0.64F);
            }
        }
    }

    private static void appendFlowPetals(BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right,
                                         double p, double seconds) {
        double appear = smooth(0.485, 0.585, p);
        double vanish = 1.0 - smooth(0.94, 1.0, p);
        double global = appear * vanish;
        if (global <= 0.001) return;

        final int count = 430;
        for (int i = 0; i < count && !mesh.full(); i++) {
            int side = (i & 1) == 0 ? -1 : 1;
            int wave = (i / 2) % 3;
            double n0 = noise(i * 71 + 13);
            double n1 = noise(i * 137 + 29);
            double n2 = noise(i * 211 + 47);
            double n3 = noise(i * 307 + 61);

            double cycle = fract(n0 + seconds * (0.17 + n3 * 0.18));
            double stormAngle = seconds * (1.15 + n1 * 1.75) + n2 * Math.PI * 2.0;
            double longitudinal = mix(-22.0, 22.0, cycle) + Math.sin(stormAngle * 0.55) * (0.8 + n0 * 1.9);
            double lateral = side * (5.0 + n1 * 7.6) + Math.cos(stormAngle) * (0.9 + n3 * 2.8);
            double vertical = 0.45 + n2 * 9.6 + Math.sin(stormAngle * 0.82 + i) * (0.45 + n1 * 1.15);
            Vec3 start = origin.add(forward.scale(longitudinal)).add(right.scale(lateral)).add(0.0, vertical, 0.0);

            double t = 0.0;
            Vec3 control = start;
            Vec3 end = start;
            double delay = n3 * 0.026;
            if (wave == 0 && side < 0) {
                t = smooth(0.575 + delay, 0.655 + delay, p);
                double compressed = smooth(0.555 + delay, 0.585 + delay, p) * (1.0 - smooth(0.585 + delay, 0.615 + delay, p));
                start = start.add(right.scale(compressed * 2.6));
                control = origin.add(forward.scale((n0 - 0.5) * 13.0)).add(right.scale(-0.4)).add(0.0, 4.5 + n2 * 4.8, 0.0);
                end = origin.add(forward.scale((n0 - 0.5) * 18.0 + 2.0)).add(right.scale(11.5 + n1 * 3.0)).add(0.0, 0.7 + n2 * 6.0, 0.0);
            } else if (wave == 1 && side > 0) {
                t = smooth(0.685 + delay, 0.765 + delay, p);
                double compressed = smooth(0.665 + delay, 0.695 + delay, p) * (1.0 - smooth(0.695 + delay, 0.725 + delay, p));
                start = start.subtract(right.scale(compressed * 2.6));
                control = origin.add(forward.scale((n0 - 0.5) * 13.0)).add(right.scale(0.4)).add(0.0, 4.4 + n2 * 4.9, 0.0);
                end = origin.add(forward.scale((n0 - 0.5) * 18.0 + 2.0)).add(right.scale(-11.5 - n1 * 3.0)).add(0.0, 0.7 + n2 * 6.0, 0.0);
            } else if (wave == 2) {
                t = smooth(0.80 + delay, 0.895 + delay, p);
                control = origin.add(forward.scale(4.0 + n0 * 7.0)).add(right.scale(side * (2.1 + n1 * 2.2))).add(0.0, 5.6 + n2 * 3.8, 0.0);
                end = origin.add(forward.scale(14.5 + n0 * 8.0)).add(right.scale((n1 - 0.5) * 5.2)).add(0.0, 0.8 + n2 * 5.8, 0.0);
            }

            double accel = t <= 0.001 ? 0.0 : fastEase(t);
            Vec3 position = t <= 0.001 ? start : bezier(start, control, end, accel);
            position = enforceSafeVoid(position, origin, right.scale(side), 2.45);
            Vec3 tangent = t > 0.02 ? end.subtract(position)
                    : forward.scale(1.0 + n0 * 0.8)
                    .add(right.scale(side * (0.24 + n1 * 0.48)))
                    .add(0.0, Math.cos(stormAngle) * 0.28, 0.0);
            if (tangent.lengthSqr() < 1.0E-7) tangent = forward;

            double brightness = 0.70 + 0.30 * Math.sin(seconds * 10.5 + i * 1.19);
            double alpha = clamp(global * brightness, 0.0, 1.0);
            int face = BladeMesh.withAlpha((i % 11 == 0) ? 0x00FFFDFE : 0x00D879A7,
                    alpha * ((i % 5 == 0) ? 0.92 : 0.72));
            int edge = BladeMesh.withAlpha((i % 7 == 0) ? 0x00FFFFFF : 0x00F8C9DF, alpha * 0.96);
            mesh.shard(position, tangent, right, 0.21 + n2 * 0.33, 0.052 + n3 * 0.076,
                    0.016 + n0 * 0.018, face, edge);
        }
    }

    private static void appendCorePetals(BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right,
                                         double p, double seconds) {
        double appear = smooth(0.50, 0.59, p);
        double vanish = 1.0 - smooth(0.95, 1.0, p);
        double global = appear * vanish;
        if (global <= 0.001) return;

        final int count = 108;
        for (int i = 0; i < count && !mesh.full(); i++) {
            int side = (i & 1) == 0 ? -1 : 1;
            int wave = (i / 2) % 3;
            double n0 = noise(i * 101 + 17);
            double n1 = noise(i * 191 + 37);
            double n2 = noise(i * 271 + 59);
            double n3 = noise(i * 353 + 71);
            double cycle = fract(n0 + seconds * (0.32 + n1 * 0.34));
            double theta = seconds * (1.8 + n3 * 2.3) + n2 * Math.PI * 2.0;
            Vec3 start = origin.add(forward.scale(mix(-20.0, 20.0, cycle)))
                    .add(right.scale(side * (5.3 + n1 * 6.8) + Math.cos(theta) * (1.0 + n2 * 2.1)))
                    .add(0.0, 0.7 + n2 * 8.7 + Math.sin(theta) * 0.9, 0.0);

            double delay = n3 * 0.018;
            double t = 0.0;
            Vec3 end = start;
            if (wave == 0 && side < 0) {
                t = smooth(0.585 + delay, 0.635 + delay, p);
                end = origin.add(forward.scale((n0 - 0.5) * 16.0 + 3.0)).add(right.scale(13.0 + n1 * 2.8)).add(0.0, 1.0 + n2 * 5.0, 0.0);
            } else if (wave == 1 && side > 0) {
                t = smooth(0.695 + delay, 0.745 + delay, p);
                end = origin.add(forward.scale((n0 - 0.5) * 16.0 + 3.0)).add(right.scale(-13.0 - n1 * 2.8)).add(0.0, 1.0 + n2 * 5.0, 0.0);
            } else if (wave == 2) {
                t = smooth(0.815 + delay, 0.865 + delay, p);
                end = origin.add(forward.scale(17.0 + n0 * 8.0)).add(right.scale((n1 - 0.5) * 4.6)).add(0.0, 0.9 + n2 * 5.3, 0.0);
            }

            double accel = t <= 0.001 ? 0.0 : fastEase(t);
            Vec3 position = start.lerp(end, accel);
            position = enforceSafeVoid(position, origin, right.scale(side), 2.45);
            Vec3 tangent = t > 0.02 ? end.subtract(start)
                    : forward.scale(1.6 + n0).add(right.scale(side * (0.4 + n1 * 0.7))).add(0.0, Math.sin(theta) * 0.3, 0.0);
            if (tangent.lengthSqr() < 1.0E-7) tangent = forward;

            double alpha = global * (0.82 + 0.18 * Math.sin(seconds * 12.0 + i));
            int face = BladeMesh.withAlpha((i % 4 == 0) ? 0x00FFFDFE : 0x00E08CB6, alpha * 0.93);
            int edge = BladeMesh.withAlpha(0x00FFFFFF, alpha);
            mesh.shard(position, tangent, right, 0.34 + n2 * 0.42, 0.075 + n3 * 0.085,
                    0.020 + n0 * 0.020, face, edge);

            Vec3 direction = tangent.normalize();
            double trail = t > 0.02 ? 2.3 + n1 * 3.5 : 0.75 + n1 * 1.6;
            Vec3 tail = position.subtract(direction.scale(trail));
            mesh.glowEdge(tail, position,
                    BladeMesh.withAlpha(0x006C1945, alpha * (t > 0.02 ? 0.34 : 0.16)),
                    BladeMesh.withAlpha(0x00FFE0EE, alpha * (t > 0.02 ? 0.92 : 0.58)), 0.88F);
        }
    }

    private static void appendDustPetals(BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right,
                                         double p, double seconds) {
        double appear = smooth(0.505, 0.605, p);
        double vanish = 1.0 - smooth(0.94, 1.0, p);
        double global = appear * vanish;
        if (global <= 0.001) return;

        final int count = 340;
        for (int i = 0; i < count && !mesh.full(); i++) {
            int side = (i & 1) == 0 ? -1 : 1;
            double n0 = noise(i * 59 + 5);
            double n1 = noise(i * 127 + 23);
            double n2 = noise(i * 199 + 41);
            double n3 = noise(i * 281 + 67);
            double cycle = fract(n0 + seconds * (0.26 + n3 * 0.30));
            double theta = seconds * (1.6 + n1 * 2.5) + n2 * Math.PI * 2.0;
            Vec3 pos = origin.add(forward.scale(mix(-23.0, 23.0, cycle)))
                    .add(right.scale(side * (4.6 + n1 * 8.8) + Math.cos(theta) * (1.2 + n0 * 2.8)))
                    .add(0.0, 0.25 + n2 * 10.2 + Math.sin(theta) * (0.5 + n3), 0.0);
            Vec3 dir = forward.scale(1.5 + n0 * 1.7)
                    .add(right.scale(side * (0.32 + Math.cos(theta) * 0.62)))
                    .add(0.0, Math.sin(theta) * 0.26, 0.0).normalize();
            double len = 0.30 + n1 * 0.95;
            Vec3 tail = pos.subtract(dir.scale(len));
            double flicker = 0.45 + 0.55 * Math.abs(Math.sin(seconds * 13.0 + i * 0.91));
            mesh.glowEdge(tail, pos,
                    BladeMesh.withAlpha(0x00681945, global * flicker * 0.12),
                    BladeMesh.withAlpha((i % 6 == 0) ? 0x00FFF7FB : 0x00F6A9CC, global * flicker * 0.52), 0.42F);
        }
    }

    private static void appendTorrentHighlights(BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right, double p) {
        double leftWave = pulse(p, 0.565, 0.640, 0.705);
        double rightWave = pulse(p, 0.675, 0.750, 0.815);
        double converge = pulse(p, 0.795, 0.875, 0.945);

        if (leftWave > 0.001) appendWallStreaks(mesh, origin, forward, right, -1, leftWave);
        if (rightWave > 0.001) appendWallStreaks(mesh, origin, forward, right, 1, rightWave);
        if (converge > 0.001) {
            Vec3 focus = origin.add(forward.scale(17.0)).add(0.0, 2.4, 0.0);
            for (int side : new int[]{-1, 1}) {
                for (int i = 0; i < 14 && !mesh.full(); i++) {
                    Vec3 a = origin.add(right.scale(side * (8.0 + (i % 6) * 1.35)))
                            .add(forward.scale((i - 6.5) * 1.8)).add(0.0, 0.7 + (i % 5) * 1.25, 0.0);
                    Vec3 b = focus.add(right.scale((i - 6.5) * 0.30)).add(0.0, (i % 3) * 0.55, 0.0);
                    mesh.glowEdge(a, b,
                            BladeMesh.withAlpha(0x006B2047, converge * 0.24),
                            BladeMesh.withAlpha(0x00FFE7F2, converge * 0.82), 1.05F);
                }
            }
        }
    }

    private static void appendWallStreaks(BladeMesh.Builder mesh, Vec3 origin, Vec3 forward, Vec3 right,
                                          int sourceSide, double power) {
        for (int i = 0; i < 14 && !mesh.full(); i++) {
            double lane = (i - 6.5) * 1.55;
            Vec3 a = origin.add(forward.scale(lane)).add(right.scale(sourceSide * 14.0)).add(0.0, 0.8 + (i % 5) * 1.25, 0.0);
            Vec3 b = origin.add(forward.scale(lane * 0.78 + 2.0)).add(right.scale(-sourceSide * 12.0)).add(0.0, 1.1 + (i % 4) * 1.35, 0.0);
            mesh.glowEdge(a, b,
                    BladeMesh.withAlpha(0x005A193C, power * 0.24),
                    BladeMesh.withAlpha(0x00FAD4E5, power * 0.78), 1.16F);
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

    private static double fastEase(double t) {
        double c = clamp(t, 0.0, 1.0);
        return 1.0 - Math.pow(1.0 - c, 5.0);
    }

    private static double mix(double a, double b, double t) {
        return a + (b - a) * clamp(t, 0.0, 1.0);
    }

    private static double fract(double value) {
        return value - Math.floor(value);
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
