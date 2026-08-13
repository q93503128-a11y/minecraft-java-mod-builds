package kr.moonseungjun.senbonzakura.client;

import kr.moonseungjun.senbonzakura.SenbonzakuraShowcase;
import kr.moonseungjun.senbonzakura.ability.ShowcaseAbility;
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

/** Eight deliberately different high-impact showcase presentations. */
public final class AbilityWorldRenderer {
    private static final ContextKey<List<RenderEntry>> DATA_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(SenbonzakuraShowcase.MOD_ID, "showcase_ability_geometry_v1"));
    private static final Map<UUID, Visual> ACTIVE = new HashMap<>();
    private static final int MAX_GEOMETRY = 82_000;
    private static final double MAX_DISTANCE_SQR = 224.0 * 224.0;
    private static final double TAU = Math.PI * 2.0;

    private AbilityWorldRenderer() {}

    public static void accept(BankaiVisualPayload payload) {
        Map<String, String> values = parse(payload.state());
        String action = values.getOrDefault("action", "");
        if (!action.startsWith("ability_")) return;

        UUID caster;
        try { caster = UUID.fromString(values.getOrDefault("caster", "")); }
        catch (Exception ignored) { return; }

        if ("ability_stop".equals(action)) {
            ACTIVE.remove(caster);
            return;
        }
        if (!"ability_start".equals(action)) return;

        ShowcaseAbility ability = ShowcaseAbility.byId(values.get("ability"));
        if (ability == null) return;
        Vec3 origin = new Vec3(decimal(values, "x", 0.0), decimal(values, "y", 0.0), decimal(values, "z", 0.0));
        Vec3 facing = safeHorizontal(new Vec3(decimal(values, "dx", 0.0), 0.0, decimal(values, "dz", 1.0)));
        int duration = Math.max(40, integer(values, "duration", ability.durationTicks()));
        long now = System.nanoTime();
        ACTIVE.put(caster, new Visual(ability, origin, facing, now,
                now + duration * 50_000_000L + 450_000_000L));
    }

    public static void onExtract(ExtractLevelRenderStateEvent event) {
        long now = System.nanoTime();
        ACTIVE.values().removeIf(visual -> visual.expiresAt() < now);
        if (ACTIVE.isEmpty()) return;

        List<RenderEntry> entries = new ArrayList<>(ACTIVE.size());
        for (Visual visual : ACTIVE.values()) {
            double duration = Math.max(0.1,
                    (visual.expiresAt() - visual.startedAt() - 450_000_000L) / 1_000_000_000.0);
            double seconds = Math.max(0.0, (now - visual.startedAt()) / 1_000_000_000.0);
            double p = clamp(seconds / duration, 0.0, 1.0);
            entries.add(new RenderEntry(visual.origin(), build(visual, p, seconds)));
        }
        event.getRenderState().setRenderData(DATA_KEY, List.copyOf(entries));
    }

    public static void onSubmit(SubmitCustomGeometryEvent event) {
        List<RenderEntry> entries = event.getLevelRenderState().getRenderData(DATA_KEY);
        if (entries == null || entries.isEmpty()) return;
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        float base = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
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
        BladeMesh.Builder mesh = BladeMesh.builder(MAX_GEOMETRY);
        Vec3 forward = visual.facing();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x).normalize();
        switch (visual.ability()) {
            case SKYFALL -> skyfall(mesh, forward, right, p, seconds);
            case WORLD_DIVIDE -> worldDivide(mesh, forward, right, p, seconds);
            case BLACK_SUN -> blackSun(mesh, forward, right, p, seconds);
            case SWORD_GRAVE -> swordGrave(mesh, forward, right, p, seconds);
            case GRAVITY_REVERSAL -> gravityReversal(mesh, forward, right, p, seconds);
            case LAST_SECOND -> lastSecond(mesh, forward, right, p, seconds);
            case HEAVEN_JUDGMENT -> heavenJudgment(mesh, forward, right, p, seconds);
            case STELLAR_LANCE -> stellarLance(mesh, forward, right, p, seconds);
        }
        return mesh.build();
    }

    private static void skyfall(BladeMesh.Builder mesh, Vec3 forward, Vec3 right, double p, double seconds) {
        Vec3 impact = forward.scale(9.0);
        Vec3 rift = impact.add(0.0, 24.0, 0.0);
        double open = smooth(0.04, 0.26, p) * (1.0 - smooth(0.78, 0.92, p));
        if (open > 0.001) {
            disc(mesh, rift, right, forward, 5.2 * open + 0.4, BladeMesh.withAlpha(0x00030710, open * 0.82), 42);
            annulus(mesh, rift.add(0.0, -0.04, 0.0), right, forward,
                    5.0 * open, 6.2 * open + 0.5, BladeMesh.withAlpha(0x00795BFF, open * 0.58), 48);
            annulus(mesh, rift.add(0.0, -0.07, 0.0), right, forward,
                    6.0 * open, 7.4 * open + 0.6, BladeMesh.withAlpha(0x00243C85, open * 0.42), 48);
        }

        double reveal = smooth(0.16, 0.34, p);
        double fall = smooth(0.40, 0.70, p);
        double bladeAlpha = reveal * (1.0 - smooth(0.735, 0.79, p));
        if (bladeAlpha > 0.001) {
            double baseY = mix(39.0, 17.2, fall);
            Vec3 base = impact.add(0.0, baseY, 0.0);
            mesh.longBlade(base, new Vec3(0.0, -1.0, 0.0), right, 16.0, 1.75, 0.46,
                    BladeMesh.withAlpha(0x00E9EEFF, bladeAlpha * 0.96),
                    BladeMesh.withAlpha(0x009BB7FF, bladeAlpha));
            mesh.box(base.add(0.0, 1.3, 0.0), right, forward, 5.4, 0.55, 0.55,
                    BladeMesh.withAlpha(0x005869A8, bladeAlpha * 0.85),
                    BladeMesh.withAlpha(0x00DCE6FF, bladeAlpha * 0.9));
        }

        double shock = smooth(0.68, 0.92, p);
        if (shock > 0.001) {
            annulus(mesh, impact.add(0.0, 0.06, 0.0), right, forward,
                    Math.max(0.0, shock * 15.0 - 0.8), shock * 15.0 + 0.7,
                    BladeMesh.withAlpha(0x00D9E4FF, (1.0 - shock) * 0.75), 64);
            for (int i = 0; i < 110 && !mesh.full(); i++) {
                double n0 = noise(i * 71 + 3), n1 = noise(i * 113 + 9), n2 = noise(i * 197 + 17);
                double angle = n0 * TAU;
                double radius = (2.0 + n1 * 12.0) * shock;
                double y = Math.sin(shock * Math.PI) * (0.8 + n2 * 5.5);
                Vec3 pos = impact.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
                solidBox(mesh, pos, new Vec3(Math.cos(angle), 0.25 + n2, Math.sin(angle)), right,
                        0.24 + n1 * 0.42, 0.12 + n2 * 0.20, 0.10 + n0 * 0.18,
                        BladeMesh.withAlpha(0x006C748A, (1.0 - shock * 0.65) * 0.72));
            }
        }
    }

    private static void worldDivide(BladeMesh.Builder mesh, Vec3 forward, Vec3 right, double p, double seconds) {
        Vec3 center = forward.scale(13.0).add(0.0, 4.2, 0.0);
        Vec3 diagonal = right.scale(0.91).add(0.0, 0.41, 0.0).normalize();
        double draw = smooth(0.12, 0.38, p) * (1.0 - smooth(0.72, 0.88, p));
        if (draw > 0.001) {
            flatBlade(mesh, center, diagonal, forward, 34.0, 0.52,
                    BladeMesh.withAlpha(0x00F3F6FF, draw * 0.88));
            flatBlade(mesh, center.subtract(forward.scale(0.12)), diagonal, forward, 33.0, 1.15,
                    BladeMesh.withAlpha(0x0010182C, draw * 0.58));
        }

        double split = smooth(0.54, 0.80, p);
        if (split > 0.001) {
            Vec3 side = diagonal.scale(16.0);
            Vec3 verticalSide = new Vec3(-diagonal.y, diagonal.x * 0.3, diagonal.z).normalize();
            double gap = split * 1.15;
            Vec3 a = center.subtract(side).subtract(forward.scale(gap)).subtract(verticalSide.scale(6.5));
            Vec3 b = center.add(side).subtract(forward.scale(gap)).subtract(verticalSide.scale(6.5));
            Vec3 c = center.add(side).subtract(forward.scale(gap)).add(verticalSide.scale(6.5));
            Vec3 d = center.subtract(side).subtract(forward.scale(gap)).add(verticalSide.scale(6.5));
            mesh.quad(a, b, c, d, BladeMesh.withAlpha(0x00050A13, split * 0.32));
            Vec3 offset = forward.scale(gap * 2.0);
            mesh.quad(a.add(offset), b.add(offset), c.add(offset), d.add(offset),
                    BladeMesh.withAlpha(0x003B4F78, split * 0.18));

            for (int i = 0; i < 90 && !mesh.full(); i++) {
                double n0 = noise(i * 53 + 7), n1 = noise(i * 109 + 19), n2 = noise(i * 181 + 31);
                Vec3 pos = center.add(diagonal.scale((n0 - 0.5) * 31.0))
                        .add(forward.scale((n1 - 0.5) * 3.0 * split))
                        .add(0.0, (n2 - 0.5) * 4.0, 0.0);
                flatBlade(mesh, pos, diagonal.add(forward.scale((n1 - 0.5) * 0.6)), forward,
                        0.25 + n2 * 0.55, 0.035 + n0 * 0.07,
                        BladeMesh.withAlpha(0x00DCE9FF, (1.0 - split * 0.55) * 0.68));
            }
        }
    }

    private static void blackSun(BladeMesh.Builder mesh, Vec3 forward, Vec3 right, double p, double seconds) {
        Vec3 center = forward.scale(10.0).add(0.0, 7.0, 0.0);
        double grow = smooth(0.08, 0.52, p);
        double collapse = smooth(0.66, 0.78, p);
        double radius = mix(0.45, 5.0, grow) * (1.0 - collapse * 0.94);
        double visible = smooth(0.04, 0.18, p) * (1.0 - smooth(0.79, 0.86, p));
        if (visible > 0.001) {
            sphere(mesh, center, radius, 12, 20, BladeMesh.withAlpha(0x0000060B, visible * 0.94));
            sphere(mesh, center, radius * 1.07, 9, 18, BladeMesh.withAlpha(0x004D174E, visible * 0.22));
        }

        double pull = smooth(0.20, 0.66, p) * (1.0 - smooth(0.70, 0.79, p));
        for (int i = 0; i < 220 && pull > 0.001 && !mesh.full(); i++) {
            double n0 = noise(i * 61 + 5), n1 = noise(i * 127 + 17), n2 = noise(i * 199 + 29);
            double angle = n0 * TAU + seconds * (0.8 + n2 * 1.8);
            double orbit = mix(14.0 + n1 * 8.0, radius + 0.5, smooth(0.18 + n2 * 0.18, 0.72, p));
            Vec3 pos = center.add(right.scale(Math.cos(angle) * orbit))
                    .add(forward.scale(Math.sin(angle) * orbit))
                    .add(0.0, (n2 - 0.5) * 10.0 * (1.0 - pull * 0.7), 0.0);
            Vec3 tangent = right.scale(-Math.sin(angle)).add(forward.scale(Math.cos(angle))).add(center.subtract(pos).normalize().scale(0.65));
            flatBlade(mesh, pos, tangent, right, 0.16 + n1 * 0.34, 0.04 + n2 * 0.07,
                    BladeMesh.withAlpha((i % 9 == 0) ? 0x00C9A4FF : 0x005E407E, pull * 0.70));
        }

        double burst = smooth(0.76, 0.92, p);
        if (burst > 0.001) {
            annulus(mesh, center, right, forward, burst * 8.5, burst * 9.4 + 0.4,
                    BladeMesh.withAlpha(0x00E4D7FF, (1.0 - burst) * 0.78), 56);
            sphere(mesh, center, burst * 6.0, 8, 16,
                    BladeMesh.withAlpha(0x006B4C9C, (1.0 - burst) * 0.18));
        }
    }

    private static void swordGrave(BladeMesh.Builder mesh, Vec3 forward, Vec3 right, double p, double seconds) {
        Vec3 center = forward.scale(9.0);
        final int count = 48;
        for (int i = 0; i < count && !mesh.full(); i++) {
            double n0 = noise(i * 73 + 7), n1 = noise(i * 131 + 19), n2 = noise(i * 211 + 37);
            double angle = TAU * i / count + n0 * 0.16;
            double radius = 5.0 + (i % 4) * 2.4 + n1 * 1.4;
            Vec3 ground = center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
            double rise = smooth(0.08 + n0 * 0.08, 0.43 + n0 * 0.05, p);
            double launch = smooth(0.62 + n1 * 0.06, 0.88 + n1 * 0.04, p);
            double length = 4.6 + n2 * 4.4;
            Vec3 base = ground.add(0.0, -length + rise * (length + 1.5), 0.0);
            Vec3 target = center.add(forward.scale(2.0 + (n0 - 0.5) * 7.0))
                    .add(right.scale((n1 - 0.5) * 10.0)).add(0.0, 0.2, 0.0);
            Vec3 hover = base.add(0.0, Math.sin(seconds * 1.7 + i) * 0.35 * rise, 0.0);
            Vec3 moved = hover.lerp(target, launch);
            Vec3 axis = launch > 0.03 ? target.subtract(hover).normalize() : new Vec3(0.0, 1.0, 0.0);
            double alpha = rise * (1.0 - smooth(0.90 + n0 * 0.02, 0.99, p));
            mesh.longBlade(moved, axis, right, length, 0.38 + n1 * 0.28, 0.12,
                    BladeMesh.withAlpha((i % 6 == 0) ? 0x00D7C8B0 : 0x009CA5B2, alpha * 0.92),
                    BladeMesh.withAlpha(0x00F3E4C7, alpha * 0.90));
        }
        double impact = pulse(p, 0.78, 0.88, 0.98);
        if (impact > 0.001) {
            annulus(mesh, center.add(0.0, 0.05, 0.0), right, forward, impact * 11.0, impact * 11.8 + 0.4,
                    BladeMesh.withAlpha(0x00D6C39A, impact * 0.42), 52);
        }
    }

    private static void gravityReversal(BladeMesh.Builder mesh, Vec3 forward, Vec3 right, double p, double seconds) {
        Vec3 center = forward.scale(6.0);
        double field = smooth(0.05, 0.26, p) * (1.0 - smooth(0.86, 0.98, p));
        if (field > 0.001) {
            annulus(mesh, center.add(0.0, 0.05, 0.0), right, forward, 10.5, 11.4,
                    BladeMesh.withAlpha(0x008EB9CE, field * 0.40), 56);
            disc(mesh, center.add(0.0, 10.5, 0.0), right, forward, 7.5,
                    BladeMesh.withAlpha(0x00213946, field * 0.16), 44);
        }

        double rise = smooth(0.15, 0.58, p);
        double slam = smooth(0.63, 0.77, p);
        for (int i = 0; i < 128 && !mesh.full(); i++) {
            double n0 = noise(i * 47 + 3), n1 = noise(i * 101 + 11), n2 = noise(i * 173 + 23);
            double angle = n0 * TAU;
            double radius = 1.5 + n1 * 10.2;
            double yPeak = 2.0 + n2 * 9.5;
            double y = yPeak * rise * (1.0 - slam) + (1.0 - slam) * Math.sin(seconds * 1.8 + i) * 0.18;
            Vec3 pos = center.add(Math.cos(angle) * radius, Math.max(0.12, y), Math.sin(angle) * radius);
            Vec3 axis = new Vec3(Math.sin(angle + seconds * 0.4), 0.4 + n2, Math.cos(angle - seconds * 0.3));
            solidBox(mesh, pos, axis, right, 0.28 + n1 * 0.62, 0.14 + n2 * 0.32, 0.12 + n0 * 0.26,
                    BladeMesh.withAlpha(0x007A8792, field * 0.72));
        }

        double crush = pulse(p, 0.72, 0.82, 0.93);
        if (crush > 0.001) {
            disc(mesh, center.add(0.0, 0.07, 0.0), right, forward, 10.8 * crush,
                    BladeMesh.withAlpha(0x00D8F5FF, crush * 0.18), 48);
        }
    }

    private static void lastSecond(BladeMesh.Builder mesh, Vec3 forward, Vec3 right, double p, double seconds) {
        Vec3 center = forward.scale(5.0).add(0.0, 2.8, 0.0);
        double freeze = smooth(0.10, 0.30, p) * (1.0 - smooth(0.74, 0.84, p));
        if (freeze > 0.001) {
            sphere(mesh, center, 12.0, 8, 20, BladeMesh.withAlpha(0x007DB5D0, freeze * 0.055));
            for (int ring = 0; ring < 3; ring++) {
                annulus(mesh, center.add(0.0, (ring - 1) * 2.7, 0.0), right, forward,
                        8.0 + ring * 1.3, 8.2 + ring * 1.3,
                        BladeMesh.withAlpha(0x00CDEEFF, freeze * 0.26), 42);
            }
        }

        for (int i = 0; i < 54 && !mesh.full(); i++) {
            double n0 = noise(i * 67 + 5), n1 = noise(i * 139 + 17), n2 = noise(i * 223 + 31);
            double appear = smooth(0.18 + n2 * 0.34, 0.30 + n2 * 0.38, p);
            double fire = smooth(0.74 + n0 * 0.025, 0.84 + n0 * 0.02, p);
            double angle = n0 * TAU;
            Vec3 pos = center.add(right.scale(Math.cos(angle) * (3.0 + n1 * 8.0)))
                    .add(forward.scale(Math.sin(angle) * (3.0 + n1 * 8.0)))
                    .add(0.0, (n2 - 0.5) * 7.0, 0.0);
            Vec3 dir = new Vec3(Math.cos(angle + n2), (n1 - 0.5) * 0.65, Math.sin(angle + n2)).normalize();
            pos = pos.add(dir.scale(fire * (4.0 + n1 * 6.0)));
            double alpha = appear * (1.0 - smooth(0.84, 0.94, p));
            flatBlade(mesh, pos, dir, right, 1.3 + n1 * 2.4, 0.09 + n2 * 0.16,
                    BladeMesh.withAlpha((i % 5 == 0) ? 0x00FFFFFF : 0x009FD9F4, alpha * 0.72));
        }

        double release = pulse(p, 0.76, 0.84, 0.94);
        if (release > 0.001) {
            sphere(mesh, center, 12.2 * release, 7, 18,
                    BladeMesh.withAlpha(0x00E7FAFF, release * 0.10));
        }
    }

    private static void heavenJudgment(BladeMesh.Builder mesh, Vec3 forward, Vec3 right, double p, double seconds) {
        Vec3 strike = forward.scale(8.0);
        Vec3 cloud = strike.add(0.0, 21.0, 0.0);
        double gather = smooth(0.06, 0.46, p) * (1.0 - smooth(0.84, 0.96, p));
        for (int i = 0; i < 7 && gather > 0.001; i++) {
            double angle = TAU * i / 7.0 + seconds * 0.07 * (i % 2 == 0 ? 1 : -1);
            Vec3 c = cloud.add(Math.cos(angle) * (2.0 + i * 0.55), (i % 3) * 0.35, Math.sin(angle) * (2.0 + i * 0.55));
            disc(mesh, c, right, forward, 4.2 + (i % 3) * 1.2,
                    BladeMesh.withAlpha(0x00131B2B, gather * (0.28 + (i % 2) * 0.08)), 30);
        }

        double strikeP = pulse(p, 0.55, 0.69, 0.84);
        if (strikeP > 0.001) {
            Vec3 previous = cloud;
            for (int i = 1; i <= 13 && !mesh.full(); i++) {
                double t = i / 13.0;
                double jitter = (1.0 - Math.abs(t - 0.5) * 1.2);
                Vec3 current = cloud.lerp(strike.add(0.0, 0.2, 0.0), t)
                        .add(right.scale((noise(i * 83 + 9) - 0.5) * 2.2 * jitter))
                        .add(forward.scale((noise(i * 137 + 21) - 0.5) * 1.5 * jitter));
                bolt(mesh, previous, current, right, 0.42 + strikeP * 0.46,
                        BladeMesh.withAlpha(0x00EAF8FF, strikeP * 0.96));
                if (i > 3 && i < 12 && i % 2 == 0) {
                    Vec3 branchEnd = current.add(right.scale((i % 4 == 0 ? 1 : -1) * (2.0 + i * 0.22)))
                            .add(forward.scale((i % 3 - 1) * 1.5)).add(0.0, -1.0 - i * 0.15, 0.0);
                    bolt(mesh, current, branchEnd, forward, 0.16 + strikeP * 0.18,
                            BladeMesh.withAlpha(0x008FCFFF, strikeP * 0.72));
                }
                previous = current;
            }
            annulus(mesh, strike.add(0.0, 0.05, 0.0), right, forward,
                    strikeP * 8.0, strikeP * 9.0 + 0.3,
                    BladeMesh.withAlpha(0x00DDF8FF, strikeP * 0.55), 52);
        }
    }

    private static void stellarLance(BladeMesh.Builder mesh, Vec3 forward, Vec3 right, double p, double seconds) {
        Vec3 focus = forward.scale(-4.0).add(0.0, 3.4, 0.0);
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        double charge = smooth(0.04, 0.52, p) * (1.0 - smooth(0.68, 0.80, p));
        if (charge > 0.001) {
            for (int i = 0; i < 6; i++) {
                double radius = mix(8.0 - i * 0.8, 1.2 + i * 0.14, charge);
                annulus(mesh, focus.add(forward.scale(-i * 0.18)), right, up,
                        Math.max(0.05, radius - 0.12), radius + 0.12,
                        BladeMesh.withAlpha(i % 2 == 0 ? 0x007CC8FF : 0x00D9B8FF, charge * 0.48), 44);
            }
            sphere(mesh, focus, mix(3.8, 0.42, charge), 8, 16,
                    BladeMesh.withAlpha(0x00E8F6FF, charge * 0.26));
        }

        double launch = smooth(0.52, 0.72, p);
        double lanceAlpha = smooth(0.42, 0.54, p) * (1.0 - smooth(0.76, 0.84, p));
        if (lanceAlpha > 0.001) {
            Vec3 center = focus.lerp(forward.scale(34.0).add(0.0, 2.2, 0.0), launch);
            flatBlade(mesh, center, forward, up, 6.8, 0.62,
                    BladeMesh.withAlpha(0x00F7FBFF, lanceAlpha));
            flatBlade(mesh, center.subtract(forward.scale(0.15)), forward, up, 8.4, 1.4,
                    BladeMesh.withAlpha(0x006EACFF, lanceAlpha * 0.35));
        }

        for (int i = 0; i < 5; i++) {
            double local = pulse(p, 0.62 + i * 0.035, 0.72 + i * 0.035, 0.84 + i * 0.035);
            if (local <= 0.001) continue;
            Vec3 center = forward.scale(10.0 + i * 6.0).add(0.0, 2.2, 0.0);
            sphere(mesh, center, local * (2.6 + i * 0.22), 7, 14,
                    BladeMesh.withAlpha(0x007FC4FF, local * 0.16));
            annulus(mesh, center, right, up, local * 2.8, local * 3.3 + 0.2,
                    BladeMesh.withAlpha(0x00E6F7FF, local * 0.42), 34);
        }
    }

    private static void flatBlade(BladeMesh.Builder mesh, Vec3 center, Vec3 axis, Vec3 broadHint,
                                  double length, double width, int color) {
        Vec3 n = safe(axis, new Vec3(0.0, 0.0, 1.0));
        Vec3 side = projectedSide(n, broadHint);
        Vec3 front = center.add(n.scale(length * 0.55));
        Vec3 back = center.subtract(n.scale(length * 0.45));
        Vec3 left = center.add(side.scale(width * 0.5));
        Vec3 right = center.subtract(side.scale(width * 0.5));
        mesh.quad(front, left, back, right, color);
    }

    private static void solidBox(BladeMesh.Builder mesh, Vec3 center, Vec3 axis, Vec3 broadHint,
                                 double length, double width, double thickness, int color) {
        Vec3 n = safe(axis, new Vec3(0.0, 1.0, 0.0));
        Vec3 s = projectedSide(n, broadHint).scale(width * 0.5);
        Vec3 t = n.cross(safe(s, new Vec3(1.0, 0.0, 0.0))).normalize().scale(thickness * 0.5);
        Vec3 a = n.scale(length * 0.5);
        Vec3[] q = {
                center.add(a).add(s).add(t), center.add(a).subtract(s).add(t),
                center.add(a).subtract(s).subtract(t), center.add(a).add(s).subtract(t),
                center.subtract(a).add(s).add(t), center.subtract(a).subtract(s).add(t),
                center.subtract(a).subtract(s).subtract(t), center.subtract(a).add(s).subtract(t)
        };
        mesh.quad(q[0], q[1], q[2], q[3], color);
        mesh.quad(q[7], q[6], q[5], q[4], color);
        mesh.quad(q[0], q[4], q[5], q[1], color);
        mesh.quad(q[1], q[5], q[6], q[2], color);
        mesh.quad(q[2], q[6], q[7], q[3], color);
        mesh.quad(q[3], q[7], q[4], q[0], color);
    }

    private static void bolt(BladeMesh.Builder mesh, Vec3 a, Vec3 b, Vec3 hint, double width, int color) {
        Vec3 delta = b.subtract(a);
        if (delta.lengthSqr() < 1.0E-8) return;
        solidBox(mesh, a.add(b).scale(0.5), delta, hint, delta.length(), width, width, color);
    }

    private static void disc(BladeMesh.Builder mesh, Vec3 center, Vec3 axisA, Vec3 axisB,
                             double radius, int color, int segments) {
        if (radius <= 0.001) return;
        Vec3 a = safe(axisA, new Vec3(1.0, 0.0, 0.0));
        Vec3 b = safe(axisB.subtract(a.scale(axisB.dot(a))), new Vec3(0.0, 0.0, 1.0));
        for (int i = 0; i < segments && !mesh.full(); i++) {
            double t0 = TAU * i / segments, t1 = TAU * (i + 1) / segments;
            Vec3 p0 = center.add(a.scale(Math.cos(t0) * radius)).add(b.scale(Math.sin(t0) * radius));
            Vec3 p1 = center.add(a.scale(Math.cos(t1) * radius)).add(b.scale(Math.sin(t1) * radius));
            mesh.triangle(center, p0, p1, color);
        }
    }

    private static void annulus(BladeMesh.Builder mesh, Vec3 center, Vec3 axisA, Vec3 axisB,
                                double inner, double outer, int color, int segments) {
        if (outer <= 0.001 || outer <= inner) return;
        inner = Math.max(0.0, inner);
        Vec3 a = safe(axisA, new Vec3(1.0, 0.0, 0.0));
        Vec3 b = safe(axisB.subtract(a.scale(axisB.dot(a))), new Vec3(0.0, 0.0, 1.0));
        for (int i = 0; i < segments && !mesh.full(); i++) {
            double t0 = TAU * i / segments, t1 = TAU * (i + 1) / segments;
            Vec3 i0 = center.add(a.scale(Math.cos(t0) * inner)).add(b.scale(Math.sin(t0) * inner));
            Vec3 i1 = center.add(a.scale(Math.cos(t1) * inner)).add(b.scale(Math.sin(t1) * inner));
            Vec3 o1 = center.add(a.scale(Math.cos(t1) * outer)).add(b.scale(Math.sin(t1) * outer));
            Vec3 o0 = center.add(a.scale(Math.cos(t0) * outer)).add(b.scale(Math.sin(t0) * outer));
            mesh.quad(i0, i1, o1, o0, color);
        }
    }

    private static void sphere(BladeMesh.Builder mesh, Vec3 center, double radius,
                               int latitudes, int longitudes, int color) {
        if (radius <= 0.001) return;
        for (int y = 0; y < latitudes && !mesh.full(); y++) {
            double lat0 = -Math.PI * 0.5 + Math.PI * y / latitudes;
            double lat1 = -Math.PI * 0.5 + Math.PI * (y + 1) / latitudes;
            for (int x = 0; x < longitudes && !mesh.full(); x++) {
                double lon0 = TAU * x / longitudes;
                double lon1 = TAU * (x + 1) / longitudes;
                Vec3 a = spherePoint(center, radius, lat0, lon0);
                Vec3 b = spherePoint(center, radius, lat0, lon1);
                Vec3 c = spherePoint(center, radius, lat1, lon1);
                Vec3 d = spherePoint(center, radius, lat1, lon0);
                mesh.quad(a, b, c, d, color);
            }
        }
    }

    private static Vec3 spherePoint(Vec3 c, double r, double lat, double lon) {
        double cos = Math.cos(lat);
        return c.add(Math.cos(lon) * cos * r, Math.sin(lat) * r, Math.sin(lon) * cos * r);
    }

    private static Vec3 projectedSide(Vec3 axis, Vec3 hint) {
        Vec3 projected = hint.subtract(axis.scale(hint.dot(axis)));
        if (projected.lengthSqr() < 1.0E-7) {
            Vec3 fallback = Math.abs(axis.y) < 0.82 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
            projected = fallback.subtract(axis.scale(fallback.dot(axis)));
        }
        return projected.normalize();
    }

    private static Vec3 safe(Vec3 value, Vec3 fallback) {
        return value.lengthSqr() < 1.0E-8 ? fallback : value.normalize();
    }

    private static double pulse(double value, double in, double peak, double out) {
        return smooth(in, peak, value) * (1.0 - smooth(peak, out, value));
    }

    private static double smooth(double from, double to, double value) {
        if (to <= from) return value >= to ? 1.0 : 0.0;
        double t = clamp((value - from) / (to - from), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double mix(double a, double b, double t) {
        return a + (b - a) * clamp(t, 0.0, 1.0);
    }

    private static double noise(int seed) {
        double value = Math.sin(seed * 12.9898 + 78.233) * 43758.5453123;
        return value - Math.floor(value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
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
            if (split > 0) result.put(token.substring(0, split), token.substring(split + 1));
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

    private record Visual(ShowcaseAbility ability, Vec3 origin, Vec3 facing, long startedAt, long expiresAt) {}
    private record RenderEntry(Vec3 origin, BladeMesh mesh) {}
}
