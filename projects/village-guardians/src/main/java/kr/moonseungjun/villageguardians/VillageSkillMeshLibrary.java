package kr.moonseungjun.villageguardians;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Original procedural skill meshes.
 *
 * Geometry is authored here as vertices, faces, ribbons, curved shields,
 * runes, projectiles and volume shells. No vanilla item model, block model,
 * display entity or particle renderer is involved.
 */
public final class VillageSkillMeshLibrary {
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final double TAU = Math.PI * 2.0;

    private VillageSkillMeshLibrary() {}

    public static void render(
            VillageSkillEffectRenderState state,
            PoseStack.Pose pose,
            VertexConsumer out) {
        double age = state.age;
        double progress = clamp(age / Math.max(1.0, state.duration), 0.0, 1.0);
        Basis basis = Basis.from(state.direction);
        Random random = new Random(state.seed);

        switch (state.kind) {
            case "vanguard_spin" -> renderVanguardSpin(pose, out, basis, age, progress);
            case "vanguard_rally" -> renderVanguardRally(pose, out, basis, age, progress);
            case "vanguard_blade_charge" -> renderBladeCharge(pose, out, basis, age, progress);
            case "vanguard_slam_charge" -> renderSlamCharge(pose, out, basis, age, progress);
            case "vanguard_blade_wave" -> renderBladeWave(pose, out, basis, age, progress);
            case "vanguard_slam_impact" -> renderSlamImpact(pose, out, basis, age, progress, random);

            case "ranger_rapid" -> renderRapidFire(pose, out, basis, age, progress);
            case "ranger_focus" -> renderRangerFocus(pose, out, basis, age, progress);
            case "ranger_lock" -> renderTargetLock(pose, out, basis, age, progress);
            case "ranger_rain_field" -> renderArrowRainField(pose, out, basis, age, progress, random);
            case "ranger_rain_impact" -> renderArrowRainImpact(pose, out, basis, age, progress, random);
            case "ranger_energy_charge" -> renderEnergyCharge(pose, out, basis, age, progress);
            case "ranger_energy_projectile" -> renderEnergyProjectile(pose, out, basis, age, progress);
            case "ranger_ricochet_path" -> renderPath(pose, out, state, age, progress, 0x91D7FF, false);

            case "arcanist_fire_orb" -> renderFireOrb(pose, out, basis, age, progress);
            case "arcanist_fire_impact" -> renderFireImpact(pose, out, basis, age, progress, state.extra);
            case "arcanist_frost" -> renderFrostField(pose, out, basis, age, progress);
            case "arcanist_tornado" -> renderTornado(pose, out, basis, age, progress);
            case "arcanist_lightning" -> renderLightningField(pose, out, basis, age, progress, random);

            case "luminar_heal_cast" -> renderHealCast(pose, out, basis, age, progress);
            case "luminar_heal_link" -> renderPath(pose, out, state, age, progress, 0xFFF2A8, true);
            case "luminar_cleanse_cast" -> renderCleanseCast(pose, out, basis, age, progress);
            case "luminar_cleanse_wave" -> renderCleanseWave(pose, out, state, age, progress);
            case "luminar_healing_field" -> renderHealingField(pose, out, basis, age, progress);
            case "luminar_miracle_cast" -> renderMiracleCast(pose, out, basis, age, progress);
            case "luminar_miracle_wave" -> renderMiracleWave(pose, out, state, age, progress);

            case "warden_charge_cast" -> renderShieldCharge(pose, out, basis, age, progress);
            case "warden_taunt" -> renderTaunt(pose, out, basis, age, progress);
            case "warden_fortress" -> renderFortress(pose, out, basis, age, progress, true);
            case "warden_aegis" -> renderFortress(pose, out, basis, age, progress, false);
            default -> renderFallbackRune(pose, out, basis, age, progress);
        }
    }

    private static void renderVanguardSpin(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double fade = envelope(progress, 0.08, 0.16);
        for (int layer = 0; layer < 4; layer++) {
            double angle = age * 0.78 + layer * TAU / 4.0;
            double radius = 1.35 + layer * 0.34;
            double y = 0.72 + layer * 0.24;
            slashArc(pose, out, b, angle, radius, y, 1.18, 0.14 + layer * 0.018,
                    rgba(255, 188 - layer * 14, 72, (int) (190 * fade)));
        }
        ring(pose, out, b, 1.05, 0.06, 0.08, 56,
                rgba(255, 103, 38, (int) (120 * fade)), -age * 0.035);
    }

    private static void renderVanguardRally(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double pulse = 0.82 + 0.18 * Math.sin(age * 0.34);
        ring(pose, out, b, 1.2 + progress * 2.2, 0.12, 0.14, 56,
                rgba(255, 171, 58, (int) (170 * (1.0 - progress))), age * 0.02);
        ring(pose, out, b, 0.95 * pulse, 1.45, 0.09, 40,
                rgba(255, 224, 130, 185), -age * 0.025);
        for (int i = 0; i < 6; i++) {
            double angle = i * TAU / 6.0 + age * 0.025;
            chevron(pose, out, b, angle, 1.0 + 0.16 * Math.sin(age * 0.2 + i),
                    1.2 + i % 2 * 0.35, 0.42,
                    rgba(255, 187, 68, 185));
        }
        for (int i = 0; i < 4; i++) {
            double a = i * TAU / 4.0 + age * 0.018;
            verticalBlade(pose, out, b, polar(a, 0.65, 0.25), 1.9, 0.10,
                    rgba(255, 236, 165, 150));
        }
    }

    private static void renderBladeCharge(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double fade = envelope(progress, 0.10, 0.20);
        Vec3 core = b.local(0.42, 1.05, 0.52);
        sphere(pose, out, core, 0.16 + progress * 0.10, 8, 12,
                rgba(165, 228, 255, (int) (205 * fade)));
        for (int i = 0; i < 3; i++) {
            slashArc(pose, out, b, age * 0.16 + i * TAU / 3.0,
                    0.72 + i * 0.16, 0.92 + i * 0.13,
                    0.82, 0.055,
                    rgba(128, 211, 255, (int) ((150 - i * 20) * fade)));
        }
    }

    private static void renderSlamCharge(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double height = 3.0 - progress * 1.25;
        Vec3 root = b.local(0.0, height, 0.15);
        Vec3 tip = b.local(0.0, 0.35, 0.45);
        energyBlade(pose, out, root, tip, 0.36 + progress * 0.22,
                rgba(255, 85, 52, 220));
        for (int i = 0; i < 3; i++) {
            ring(pose, out, b, 0.45 + i * 0.38 + progress * 0.55,
                    0.16 + i * 0.18, 0.07, 36,
                    rgba(255, 96 + i * 28, 70, 125), -age * (0.06 + i * 0.01));
        }
        for (int i = 0; i < 6; i++) {
            double a = i * TAU / 6.0 + age * 0.04;
            spike(pose, out, b.local(Math.cos(a) * 1.0, 0.08, Math.sin(a) * 1.0),
                    b.local(Math.cos(a) * 1.5, 0.75 + progress * 0.45,
                            Math.sin(a) * 1.5),
                    0.12, rgba(214, 45, 44, 155));
        }
    }

    private static void renderBladeWave(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double fade = 1.0 - progress * 0.70;
        horizontalSlash(pose, out, b, 3.4, 1.05, 0.20, 0.30,
                rgba(120, 215, 255, (int) (225 * fade)));
        horizontalSlash(pose, out, b, 2.8, 1.06, 0.09, 0.20,
                rgba(236, 252, 255, (int) (190 * fade)));
    }

    private static void renderSlamImpact(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress, Random random) {
        double radius = 0.8 + progress * 5.5;
        ring(pose, out, b, radius, 0.06, 0.18 + progress * 0.22, 64,
                rgba(255, 74, 48, (int) (210 * (1.0 - progress))), age * 0.01);
        random.setSeed(random.nextLong() ^ 0x5A17L);
        for (int i = 0; i < 18; i++) {
            double a = i * TAU / 18.0 + random.nextDouble() * 0.16;
            double inner = 0.35 + random.nextDouble() * 0.4;
            double outer = radius * (0.72 + random.nextDouble() * 0.35);
            groundCrack(pose, out, b, a, inner, outer, 0.05 + random.nextDouble() * 0.06,
                    rgba(255, 103, 48, (int) (190 * (1.0 - progress))));
        }
        for (int i = 0; i < 9; i++) {
            double a = i * TAU / 9.0;
            double r = radius * (0.55 + 0.18 * Math.sin(i * 2.3));
            spike(pose, out, b.local(Math.cos(a) * r, 0.02, Math.sin(a) * r),
                    b.local(Math.cos(a) * r, 0.35 + (1.0 - progress) * 1.6,
                            Math.sin(a) * r),
                    0.12 + (1.0 - progress) * 0.14,
                    rgba(173, 48, 52, (int) (150 * (1.0 - progress))));
        }
    }

    private static void renderRapidFire(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double fade = envelope(progress, 0.08, 0.16);
        for (int i = 0; i < 3; i++) {
            ringVertical(pose, out, b, 0.58 + i * 0.19,
                    1.20, 0.035, 48,
                    rgba(112, 220, 255, (int) ((155 - i * 24) * fade)),
                    age * (0.045 + i * 0.012));
        }
        helixRibbon(pose, out, b, age * 0.18, 0.78, 2.1, 28,
                rgba(103, 206, 255, (int) (125 * fade)));
        helixRibbon(pose, out, b, Math.PI + age * 0.18, 0.78, 2.1, 28,
                rgba(205, 246, 255, (int) (105 * fade)));
    }

    private static void renderRangerFocus(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double fade = envelope(progress, 0.08, 0.20);
        Vec3 core = b.local(0.0, 1.52, 0.65);
        sphere(pose, out, core, 0.18 + Math.sin(age * 0.25) * 0.035,
                8, 12, rgba(255, 211, 88, (int) (210 * fade)));
        ringVertical(pose, out, b, 0.48, 1.52, 0.035, 44,
                rgba(255, 235, 150, (int) (160 * fade)), age * 0.06);
    }

    private static void renderFireImpact(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, String encodedRadius) {
        double radius = 4.8;
        try { radius = Double.parseDouble(encodedRadius); }
        catch (NumberFormatException ignored) {}
        double spread = 0.45 + progress * radius;
        sphere(pose, out, Vec3.ZERO, Math.max(0.35, spread * 0.42), 10, 16,
                rgba(255, 78, 31, (int) (125 * (1.0 - progress))));
        ring(pose, out, b, spread, 0.10, 0.16, 72,
                rgba(255, 146, 52, (int) (220 * (1.0 - progress))), 0.0);
        ring(pose, out, b, spread * 0.72, 0.42, 0.09, 56,
                rgba(255, 229, 118, (int) (160 * (1.0 - progress))), age * 0.02);
    }

    private static void renderTargetLock(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double fade = envelope(progress, 0.05, 0.18);
        double radius = 0.72 + Math.sin(age * 0.32) * 0.06;
        ringVertical(pose, out, b, radius, 0.65, 0.045, 56,
                rgba(255, 207, 76, (int) (220 * fade)), -age * 0.05);
        ringVertical(pose, out, b, radius * 0.45, 0.65, 0.028, 40,
                rgba(255, 247, 180, (int) (175 * fade)), age * 0.07);
        for (int i = 0; i < 4; i++) {
            reticleBracket(pose, out, b, i * Math.PI / 2.0, radius + 0.22, 0.65,
                    rgba(255, 224, 112, (int) (205 * fade)));
        }
    }

    private static void renderArrowRainField(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress, Random random) {
        double radius = 8.5;
        ring(pose, out, b, radius, 0.045, 0.11, 96,
                rgba(88, 188, 255, 170), 0.0);
        ring(pose, out, b, radius * 0.72, 0.052, 0.045, 72,
                rgba(149, 223, 255, 90), -age * 0.008);
        for (int i = 0; i < 26; i++) {
            double a = i * 2.399963229728653 + (i % 3) * 0.17;
            double r = Math.sqrt((i + 0.5) / 26.0) * radius * 0.92;
            double cycle = fract(progress * 3.4 + i * 0.137);
            double y = 11.0 - cycle * 13.0;
            Vec3 p = b.local(Math.cos(a) * r, y, Math.sin(a) * r);
            customArrow(pose, out, Basis.DOWN, p, 0.95 + (i % 4) * 0.09, 0.07,
                    rgba(164, 228, 255, (int) (205 * (1.0 - Math.max(0.0, cycle - 0.86) / 0.14))));
        }
    }

    private static void renderArrowRainImpact(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress, Random random) {
        ring(pose, out, b, 0.45 + progress * 2.8, 0.045, 0.10, 56,
                rgba(124, 211, 255, (int) (180 * (1.0 - progress))), 0.0);
        ring(pose, out, b, 0.25 + progress * 1.5, 0.06, 0.045, 40,
                rgba(225, 249, 255, (int) (130 * (1.0 - progress))), age * 0.02);
    }

    private static void renderEnergyCharge(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double pulse = 0.34 + Math.sin(age * 0.20) * 0.05;
        Vec3 core = b.local(0.0, 0.0, 0.0);
        sphere(pose, out, core, pulse, 10, 16, rgba(91, 255, 104, 225));
        Vec3 top = b.local(0.0, 1.15, 0.18);
        Vec3 middle = b.local(0.0, 0.0, 0.70);
        Vec3 bottom = b.local(0.0, -1.15, 0.18);
        prism(pose, out, top, middle, 0.065, rgba(131, 255, 139, 190));
        prism(pose, out, middle, bottom, 0.065, rgba(131, 255, 139, 190));
        prism(pose, out, top, bottom, 0.025, rgba(220, 255, 221, 135));
        for (int i = 0; i < 3; i++) {
            ringVertical(pose, out, b, 0.55 + i * 0.22, 0.0, 0.035, 42,
                    rgba(80, 255, 96, 125 - i * 20), age * (0.055 + i * 0.012));
        }
    }

    private static void renderEnergyProjectile(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double pulse = 1.0 + Math.sin(age * 0.42) * 0.05;
        customArrow(pose, out, b, Vec3.ZERO, 5.2 * pulse, 0.42,
                rgba(91, 255, 104, 245));
        sphere(pose, out, b.local(0.0, 0.0, -1.55), 0.48, 9, 14,
                rgba(195, 255, 198, 170));
        for (int i = 0; i < 4; i++) {
            double a = age * 0.20 + i * TAU / 4.0;
            Vec3 start = b.local(Math.cos(a) * 0.65, Math.sin(a) * 0.65, -2.4);
            Vec3 end = b.local(Math.cos(a) * 0.12, Math.sin(a) * 0.12, 0.9);
            taperedRibbon(pose, out, b, start, end, 0.13,
                    rgba(154, 255, 160, 145));
        }
    }

    private static void renderFireOrb(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double pulse = 0.95 + Math.sin(age * 0.42) * 0.12;
        sphere(pose, out, Vec3.ZERO, 0.62 * pulse, 12, 18,
                rgba(255, 75, 32, 220));
        sphere(pose, out, Vec3.ZERO, 0.35 * pulse, 10, 14,
                rgba(255, 224, 92, 245));
        for (int i = 0; i < 5; i++) {
            double a = age * (0.15 + i * 0.012) + i * TAU / 5.0;
            helixRibbon(pose, out, b, a, 0.48 + i * 0.035, 1.8, 20,
                    rgba(255, 125 + i * 12, 42, 150));
        }
    }

    private static void renderFrostField(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        runeDisc(pose, out, b, 5.2, 0.035, age * 0.012,
                rgba(112, 218, 255, 105));
        ring(pose, out, b, 4.2, 0.06, 0.09, 72,
                rgba(189, 246, 255, 155), -age * 0.018);
        for (int i = 0; i < 12; i++) {
            double a = i * TAU / 12.0 + (i % 2) * 0.12;
            double r = 2.1 + (i % 3) * 0.85;
            double h = 0.7 + (i % 4) * 0.33 + Math.sin(age * 0.12 + i) * 0.12;
            crystal(pose, out, b.local(Math.cos(a) * r, 0.02, Math.sin(a) * r),
                    h, 0.18 + (i % 3) * 0.04,
                    rgba(146, 228, 255, 175));
        }
    }

    private static void renderTornado(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        for (int strand = 0; strand < 8; strand++) {
            double phase = strand * TAU / 8.0 + age * (0.19 + strand * 0.006);
            int shade = 118 + (strand % 4) * 22;
            tornadoRibbon(pose, out, b, phase, 5.8, 46,
                    rgba(shade, shade + 4, shade + 9, 125 + strand * 8));
        }
        for (int i = 0; i < 24; i++) {
            double cycle = fract(age * 0.035 + i * 0.117);
            double y = 0.18 + cycle * 5.4;
            double radius = 0.65 + cycle * 2.7 + (i % 3) * 0.14;
            double angle = age * 0.17 + i * 2.399963229728653;
            Vec3 start = b.local(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            Vec3 end = start.add(b.local(-Math.sin(angle) * 0.28, 0.10,
                    Math.cos(angle) * 0.28));
            int shade = 105 + (i % 5) * 18;
            prism(pose, out, start, end, 0.06 + (i % 3) * 0.018,
                    rgba(shade, shade, shade + 5, 155));
        }
        ring(pose, out, b, 1.25 + Math.sin(age * 0.18) * 0.18, 0.06, 0.18, 56,
                rgba(174, 178, 186, 165), age * 0.07);
    }

    private static void renderLightningField(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress, Random random) {
        ring(pose, out, b, 14.0, 0.035, 0.13, 112,
                rgba(188, 128, 255, 120), -age * 0.012);
        ring(pose, out, b, 9.0, 0.045, 0.055, 88,
                rgba(229, 207, 255, 95), age * 0.018);
        for (int i = 0; i < 8; i++) {
            double a = i * TAU / 8.0 + Math.sin(age * 0.05 + i) * 0.55;
            double r = 2.0 + (i % 4) * 3.0;
            Vec3 end = b.local(Math.cos(a) * r, 0.05, Math.sin(a) * r);
            Vec3 start = end.add(0.0, 8.5 + (i % 3) * 1.2, 0.0);
            jaggedBolt(pose, out, start, end, 11, 0.11,
                    rgba(236, 220, 255, 205), stateSeed(random, i, (int) age / 2));
        }
    }

    private static void renderHealCast(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double radius = 0.55 + progress * 1.1;
        runeDisc(pose, out, b, radius, 0.10 + progress * 0.8, age * 0.045,
                rgba(255, 246, 176, (int) (210 * (1.0 - progress * 0.35))));
        for (int i = 0; i < 4; i++) {
            double a = i * TAU / 4.0 + age * 0.035;
            verticalBlade(pose, out, b, polar(a, radius * 0.75, 0.2),
                    1.35 + progress * 0.55, 0.06,
                    rgba(255, 255, 215, 145));
        }
    }

    private static void renderCleanseCast(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        for (int i = 0; i < 3; i++) {
            ring(pose, out, b, 0.8 + i * 0.4 + progress * 1.2,
                    0.25 + i * 0.5, 0.07, 56,
                    rgba(255, 244, 178, 155 - i * 22), age * (0.03 + i * 0.01));
        }
        for (int side : new int[]{-1, 1}) {
            wing(pose, out, b, side, 1.15, 1.0 + progress * 0.45,
                    rgba(255, 255, 223, 165));
        }
    }

    private static void renderCleanseWave(
            PoseStack.Pose pose, VertexConsumer out, VillageSkillEffectRenderState state,
            double age, double progress) {
        Basis b = Basis.from(state.direction);
        for (Vec3 point : parsePoints(state.extra, new Vec3(state.x, state.y, state.z))) {
            Vec3 local = point.subtract(state.x, state.y, state.z);
            localRune(pose, out, b, local, 0.65 + progress * 0.95,
                    rgba(255, 247, 183, (int) (190 * (1.0 - progress))));
        }
    }

    private static void renderHealingField(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        runeDisc(pose, out, b, 5.8, 0.03, age * 0.008,
                rgba(255, 239, 153, 105));
        for (int i = 0; i < 3; i++) {
            ring(pose, out, b, 1.7 + i * 1.35, 0.055 + i * 0.02, 0.08, 64,
                    rgba(255, 248, 188, 125 - i * 18), age * (0.015 + i * 0.006));
        }
        for (int i = 0; i < 8; i++) {
            double a = i * TAU / 8.0 + age * 0.014;
            Vec3 root = b.local(Math.cos(a) * 3.6, 0.05, Math.sin(a) * 3.6);
            verticalBlade(pose, out, b, root, 1.0 + 0.35 * Math.sin(age * 0.13 + i),
                    0.055, rgba(255, 255, 215, 115));
        }
    }

    private static void renderMiracleCast(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double alpha = envelope(progress, 0.12, 0.25);
        sphere(pose, out, b.local(0.0, 2.4, 0.0), 0.34 + 0.18 * Math.sin(age * 0.18),
                10, 16, rgba(255, 250, 198, (int) (220 * alpha)));
        for (int side : new int[]{-1, 1}) {
            wing(pose, out, b, side, 1.55, 1.75 + progress * 0.65,
                    rgba(255, 255, 225, (int) (190 * alpha)));
        }
        verticalPillar(pose, out, b, 0.38 + progress * 0.75, 6.0,
                rgba(255, 240, 142, (int) (150 * alpha)));
        runeDisc(pose, out, b, 2.4 + progress * 1.4, 0.04, -age * 0.025,
                rgba(255, 238, 137, (int) (155 * alpha)));
    }

    private static void renderMiracleWave(
            PoseStack.Pose pose, VertexConsumer out, VillageSkillEffectRenderState state,
            double age, double progress) {
        Basis b = Basis.from(state.direction);
        for (Vec3 point : parsePoints(state.extra, new Vec3(state.x, state.y, state.z))) {
            Vec3 local = point.subtract(state.x, state.y, state.z);
            localRune(pose, out, b, local, 0.5 + progress * 1.5,
                    rgba(255, 244, 162, (int) (220 * (1.0 - progress))));
            verticalPillarAt(pose, out, b, local, 0.15 + (1.0 - progress) * 0.28,
                    2.0 + (1.0 - progress) * 3.2,
                    rgba(255, 255, 210, (int) (135 * (1.0 - progress))));
        }
    }

    private static void renderShieldCharge(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double forward = 1.15 + progress * 1.65;
        Vec3 center = b.local(0.0, 1.25, forward);
        curvedShield(pose, out, b, center, 3.5, 3.1, 0.58,
                rgba(62, 157, 255, 205));
        curvedShield(pose, out, b, center.add(b.forward.scale(0.055)), 2.75, 2.35, 0.46,
                rgba(171, 227, 255, 125));
        shieldFrame(pose, out, b, center.add(b.forward.scale(0.09)), 3.5, 3.1, 0.58,
                rgba(220, 246, 255, 220));
    }

    private static void renderTaunt(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        for (int i = 0; i < 4; i++) {
            ring(pose, out, b, 1.0 + progress * (7.5 + i * 1.4),
                    0.45 + i * 0.24, 0.11, 80,
                    rgba(78, 171, 255, (int) ((185 - i * 24) * (1.0 - progress))),
                    age * 0.012);
        }
        sphere(pose, out, b.local(0.0, 1.1, 0.0), 0.65 + progress * 0.9,
                8, 14, rgba(150, 218, 255, (int) (80 * (1.0 - progress))));
    }

    private static void renderFortress(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress, boolean compact) {
        double width = compact ? 5.2 : 17.5;
        double height = compact ? 4.0 : 11.5;
        double distance = compact ? 2.35 : 5.0;
        double curve = compact ? 0.62 : 1.45;
        Vec3 center = b.local(0.0, height * 0.48, distance);
        curvedShield(pose, out, b, center, width, height, curve,
                rgba(48, 137, 244, compact ? 210 : 195));
        curvedShield(pose, out, b, center.add(b.forward.scale(0.07)),
                width * 0.88, height * 0.84, curve * 0.82,
                rgba(133, 209, 255, compact ? 115 : 105));
        shieldFrame(pose, out, b, center.add(b.forward.scale(0.12)),
                width, height, curve, rgba(220, 246, 255, 220));
        int ribs = compact ? 3 : 7;
        for (int i = 1; i <= ribs; i++) {
            double u = i / (double) (ribs + 1);
            Vec3 low = shieldPoint(b, center.add(b.forward.scale(0.14)), width, height, curve, u, 0.05);
            Vec3 high = shieldPoint(b, center.add(b.forward.scale(0.14)), width, height, curve, u, 0.95);
            prism(pose, out, low, high, compact ? 0.045 : 0.07,
                    rgba(188, 232, 255, compact ? 120 : 105));
        }
    }

    private static void renderPath(
            PoseStack.Pose pose, VertexConsumer out, VillageSkillEffectRenderState state,
            double age, double progress, int rgb, boolean healing) {
        Vec3 origin = new Vec3(state.x, state.y, state.z);
        List<Vec3> points = parsePoints(state.extra, origin);
        if (points.size() < 2) return;
        int color = rgba((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255,
                (int) (205 * (1.0 - progress * 0.65)));
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 a = points.get(i).subtract(origin);
            Vec3 b = points.get(i + 1).subtract(origin);
            braidedBeam(pose, out, a, b, age + i * 1.7,
                    healing ? 0.10 : 0.075, color);
        }
    }

    private static void renderFallbackRune(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        runeDisc(pose, out, b, 1.2, 0.25, age * 0.04,
                rgba(206, 157, 255, (int) (180 * (1.0 - progress))));
    }

    private static void horizontalSlash(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double width, double y, double thickness, double arch, int color) {
        int segments = 30;
        for (int i = 0; i < segments; i++) {
            double n0 = i / (double) segments;
            double n1 = (i + 1) / (double) segments;
            double t0 = n0 * 2.0 - 1.0;
            double t1 = n1 * 2.0 - 1.0;
            double x0 = t0 * width * 0.5;
            double x1 = t1 * width * 0.5;
            double center0 = y + (1.0 - t0 * t0) * arch;
            double center1 = y + (1.0 - t1 * t1) * arch;
            double half0 = Math.max(0.018, thickness * Math.sin(Math.PI * n0));
            double half1 = Math.max(0.018, thickness * Math.sin(Math.PI * n1));
            Vec3 p0 = b.local(x0, center0 - half0, 0.0);
            Vec3 p1 = b.local(x0, center0 + half0, 0.055);
            Vec3 p2 = b.local(x1, center1 + half1, 0.055);
            Vec3 p3 = b.local(x1, center1 - half1, 0.0);
            quadTwoSided(pose, out, p0, p1, p2, p3, color);
        }
    }

    private static void shieldFrame(
            PoseStack.Pose pose, VertexConsumer out, Basis b, Vec3 center,
            double width, double height, double curve, int color) {
        Vec3 bottomLeft = shieldPoint(b, center, width, height, curve, 0.0, 0.0);
        Vec3 topLeft = shieldPoint(b, center, width, height, curve, 0.0, 1.0);
        Vec3 topRight = shieldPoint(b, center, width, height, curve, 1.0, 1.0);
        Vec3 bottomRight = shieldPoint(b, center, width, height, curve, 1.0, 0.0);
        double thickness = Math.max(0.055, Math.min(width, height) * 0.025);
        prism(pose, out, bottomLeft, topLeft, thickness, color);
        prism(pose, out, topLeft, topRight, thickness, color);
        prism(pose, out, topRight, bottomRight, thickness, color);
        prism(pose, out, bottomRight, bottomLeft, thickness, color);
    }

    // ---- Mesh primitives -------------------------------------------------

    private static void slashArc(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double angle, double radius, double y, double sweep, double thickness, int color) {
        int segments = 20;
        for (int i = 0; i < segments; i++) {
            double t0 = i / (double) segments;
            double t1 = (i + 1) / (double) segments;
            double a0 = angle - sweep * 0.5 + sweep * t0;
            double a1 = angle - sweep * 0.5 + sweep * t1;
            double taper0 = Math.sin(Math.PI * t0);
            double taper1 = Math.sin(Math.PI * t1);
            double r0o = radius + thickness * taper0;
            double r0i = radius - thickness * taper0;
            double r1o = radius + thickness * taper1;
            double r1i = radius - thickness * taper1;
            Vec3 p0 = b.local(Math.cos(a0) * r0i, y, Math.sin(a0) * r0i);
            Vec3 p1 = b.local(Math.cos(a0) * r0o, y + thickness * 0.6, Math.sin(a0) * r0o);
            Vec3 p2 = b.local(Math.cos(a1) * r1o, y + thickness * 0.6, Math.sin(a1) * r1o);
            Vec3 p3 = b.local(Math.cos(a1) * r1i, y, Math.sin(a1) * r1i);
            quadTwoSided(pose, out, p0, p1, p2, p3, color);
        }
    }

    private static void crescent(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double radius, double y, double z, double sweep, int color) {
        int segments = 28;
        double start = -sweep * 0.5;
        for (int i = 0; i < segments; i++) {
            double t0 = i / (double) segments;
            double t1 = (i + 1) / (double) segments;
            double a0 = start + sweep * t0;
            double a1 = start + sweep * t1;
            double w0 = 0.04 + Math.sin(Math.PI * t0) * 0.32;
            double w1 = 0.04 + Math.sin(Math.PI * t1) * 0.32;
            Vec3 p0 = b.local(Math.sin(a0) * radius, y + Math.cos(a0) * radius * 0.52, z);
            Vec3 p1 = b.local(Math.sin(a0) * (radius + w0),
                    y + Math.cos(a0) * (radius + w0) * 0.52, z + 0.04);
            Vec3 p2 = b.local(Math.sin(a1) * (radius + w1),
                    y + Math.cos(a1) * (radius + w1) * 0.52, z + 0.04);
            Vec3 p3 = b.local(Math.sin(a1) * radius, y + Math.cos(a1) * radius * 0.52, z);
            quadTwoSided(pose, out, p0, p1, p2, p3, color);
        }
    }

    private static void ring(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double radius, double y, double width, int segments, int color, double phase) {
        for (int i = 0; i < segments; i++) {
            double a0 = phase + i * TAU / segments;
            double a1 = phase + (i + 1) * TAU / segments;
            Vec3 p0 = b.local(Math.cos(a0) * (radius - width), y, Math.sin(a0) * (radius - width));
            Vec3 p1 = b.local(Math.cos(a0) * (radius + width), y, Math.sin(a0) * (radius + width));
            Vec3 p2 = b.local(Math.cos(a1) * (radius + width), y, Math.sin(a1) * (radius + width));
            Vec3 p3 = b.local(Math.cos(a1) * (radius - width), y, Math.sin(a1) * (radius - width));
            quadTwoSided(pose, out, p0, p1, p2, p3, color);
        }
    }

    private static void ringVertical(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double radius, double y, double width, int segments, int color, double phase) {
        for (int i = 0; i < segments; i++) {
            double a0 = phase + i * TAU / segments;
            double a1 = phase + (i + 1) * TAU / segments;
            Vec3 p0 = b.local(Math.cos(a0) * (radius - width), y + Math.sin(a0) * (radius - width), 0.7);
            Vec3 p1 = b.local(Math.cos(a0) * (radius + width), y + Math.sin(a0) * (radius + width), 0.7);
            Vec3 p2 = b.local(Math.cos(a1) * (radius + width), y + Math.sin(a1) * (radius + width), 0.7);
            Vec3 p3 = b.local(Math.cos(a1) * (radius - width), y + Math.sin(a1) * (radius - width), 0.7);
            quadTwoSided(pose, out, p0, p1, p2, p3, color);
        }
    }

    private static void runeDisc(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double radius, double y, double phase, int color) {
        ring(pose, out, b, radius, y, Math.max(0.035, radius * 0.018), 72, color, phase);
        ring(pose, out, b, radius * 0.58, y + 0.006, Math.max(0.028, radius * 0.014), 56, color, -phase * 1.7);
        for (int i = 0; i < 6; i++) {
            double a0 = phase + i * TAU / 6.0;
            double a1 = phase + (i + 2) * TAU / 6.0;
            Vec3 p0 = b.local(Math.cos(a0) * radius * 0.62, y + 0.012,
                    Math.sin(a0) * radius * 0.62);
            Vec3 p1 = b.local(Math.cos(a1) * radius * 0.62, y + 0.012,
                    Math.sin(a1) * radius * 0.62);
            prism(pose, out, p0, p1, Math.max(0.018, radius * 0.010), color);
        }
    }

    private static void localRune(
            PoseStack.Pose pose, VertexConsumer out, Basis b, Vec3 center, double radius, int color) {
        int segments = 40;
        for (int i = 0; i < segments; i++) {
            double a0 = i * TAU / segments;
            double a1 = (i + 1) * TAU / segments;
            Vec3 p0 = center.add(Math.cos(a0) * (radius - 0.035), 0.02, Math.sin(a0) * (radius - 0.035));
            Vec3 p1 = center.add(Math.cos(a0) * (radius + 0.035), 0.02, Math.sin(a0) * (radius + 0.035));
            Vec3 p2 = center.add(Math.cos(a1) * (radius + 0.035), 0.02, Math.sin(a1) * (radius + 0.035));
            Vec3 p3 = center.add(Math.cos(a1) * (radius - 0.035), 0.02, Math.sin(a1) * (radius - 0.035));
            quadTwoSided(pose, out, p0, p1, p2, p3, color);
        }
    }

    private static void customArrow(
            PoseStack.Pose pose, VertexConsumer out, Basis b, Vec3 center,
            double length, double thickness, int color) {
        Vec3 start = center.add(b.forward.scale(-length * 0.45));
        Vec3 neck = center.add(b.forward.scale(length * 0.28));
        Vec3 tip = center.add(b.forward.scale(length * 0.62));
        prism(pose, out, start, neck, thickness, color);
        Vec3 r = b.right.scale(thickness * 2.6);
        Vec3 u = new Vec3(0, thickness * 2.6, 0);
        triangleTwoSided(pose, out, neck.add(r), neck.add(u), tip, color);
        triangleTwoSided(pose, out, neck.add(u), neck.subtract(r), tip, color);
        triangleTwoSided(pose, out, neck.subtract(r), neck.subtract(u), tip, color);
        triangleTwoSided(pose, out, neck.subtract(u), neck.add(r), tip, color);
        Vec3 tail = start.add(b.forward.scale(length * 0.18));
        triangleTwoSided(pose, out, start.add(r.scale(1.2)), tail, start.subtract(r.scale(1.2)), color);
        triangleTwoSided(pose, out, start.add(u.scale(1.2)), tail, start.subtract(u.scale(1.2)), color);
    }

    private static void customArrowBetween(
            PoseStack.Pose pose, VertexConsumer out, Vec3 start, Vec3 end,
            double thickness, int color) {
        Vec3 delta = end.subtract(start);
        Basis b = Basis.from(delta);
        double length = delta.length();
        Vec3 center = start.add(delta.scale(0.5));
        customArrow(pose, out, b, center, length, thickness, color);
    }

    private static void energyBlade(
            PoseStack.Pose pose, VertexConsumer out, Vec3 root, Vec3 tip,
            double width, int color) {
        Vec3 axis = tip.subtract(root);
        Basis b = Basis.from(axis);
        Vec3 mid = root.add(axis.scale(0.72));
        prism(pose, out, root, mid, width, color);
        Vec3 r = b.right.scale(width * 2.2);
        triangleTwoSided(pose, out, mid.add(r), mid.subtract(r), tip, color);
        Vec3 u = new Vec3(0, width * 1.7, 0);
        triangleTwoSided(pose, out, mid.add(u), mid.subtract(u), tip, color);
    }

    private static void prism(
            PoseStack.Pose pose, VertexConsumer out, Vec3 start, Vec3 end,
            double radius, int color) {
        Vec3 axis = end.subtract(start);
        Basis b = Basis.from(axis);
        Vec3 r = b.right.scale(radius);
        Vec3 u = b.up.scale(radius);
        Vec3[] a = {start.add(r), start.add(u), start.subtract(r), start.subtract(u)};
        Vec3[] z = {end.add(r), end.add(u), end.subtract(r), end.subtract(u)};
        for (int i = 0; i < 4; i++) {
            int n = (i + 1) % 4;
            quadTwoSided(pose, out, a[i], a[n], z[n], z[i], color);
        }
        quadTwoSided(pose, out, a[0], a[1], a[2], a[3], color);
        quadTwoSided(pose, out, z[3], z[2], z[1], z[0], color);
    }

    private static void sphere(
            PoseStack.Pose pose, VertexConsumer out, Vec3 center, double radius,
            int latitudes, int longitudes, int color) {
        for (int lat = 0; lat < latitudes; lat++) {
            double v0 = lat / (double) latitudes;
            double v1 = (lat + 1) / (double) latitudes;
            double p0 = -Math.PI / 2.0 + Math.PI * v0;
            double p1 = -Math.PI / 2.0 + Math.PI * v1;
            for (int lon = 0; lon < longitudes; lon++) {
                double u0 = lon / (double) longitudes;
                double u1 = (lon + 1) / (double) longitudes;
                double a0 = TAU * u0;
                double a1 = TAU * u1;
                Vec3 q0 = center.add(Math.cos(p0) * Math.cos(a0) * radius,
                        Math.sin(p0) * radius, Math.cos(p0) * Math.sin(a0) * radius);
                Vec3 q1 = center.add(Math.cos(p0) * Math.cos(a1) * radius,
                        Math.sin(p0) * radius, Math.cos(p0) * Math.sin(a1) * radius);
                Vec3 q2 = center.add(Math.cos(p1) * Math.cos(a1) * radius,
                        Math.sin(p1) * radius, Math.cos(p1) * Math.sin(a1) * radius);
                Vec3 q3 = center.add(Math.cos(p1) * Math.cos(a0) * radius,
                        Math.sin(p1) * radius, Math.cos(p1) * Math.sin(a0) * radius);
                quadTwoSided(pose, out, q0, q1, q2, q3, color);
            }
        }
    }

    private static void curvedShield(
            PoseStack.Pose pose, VertexConsumer out, Basis b, Vec3 center,
            double width, double height, double curve, int color) {
        int columns = 12;
        int rows = 6;
        for (int y = 0; y < rows; y++) {
            double v0 = y / (double) rows;
            double v1 = (y + 1) / (double) rows;
            for (int x = 0; x < columns; x++) {
                double u0 = x / (double) columns;
                double u1 = (x + 1) / (double) columns;
                Vec3 p0 = shieldPoint(b, center, width, height, curve, u0, v0);
                Vec3 p1 = shieldPoint(b, center, width, height, curve, u1, v0);
                Vec3 p2 = shieldPoint(b, center, width, height, curve, u1, v1);
                Vec3 p3 = shieldPoint(b, center, width, height, curve, u0, v1);
                int alpha = (color >>> 24);
                int edge = (x == 0 || x == columns - 1 || y == 0 || y == rows - 1)
                        ? Math.min(255, alpha + 45) : alpha;
                int c = (color & 0x00FFFFFF) | (edge << 24);
                quadTwoSided(pose, out, p0, p1, p2, p3, c);
            }
        }
    }

    private static Vec3 shieldPoint(
            Basis b, Vec3 center, double width, double height, double curve, double u, double v) {
        double x = (u - 0.5) * width;
        double y = (v - 0.5) * height;
        double edge = Math.pow(Math.abs(x) / Math.max(0.001, width * 0.5), 1.7);
        double z = -Math.abs(curve) * edge + 0.05 * Math.cos((v - 0.5) * Math.PI);
        return center.add(b.local(x, y, z));
    }

    private static void hexGrid(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double width, double height, double z, double age, int color) {
        double size = 0.42;
        int rows = Math.max(2, (int) (height / (size * 1.45)));
        int cols = Math.max(3, (int) (width / (size * 1.7)));
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                double x = (col - (cols - 1) * 0.5) * size * 1.72
                        + (row % 2) * size * 0.86;
                double y = 0.35 + row * size * 1.48;
                hexagonOutline(pose, out, b, b.local(x, y, z), size,
                        color, age * 0.01 + row * 0.07);
            }
        }
    }

    private static void hexagonOutline(
            PoseStack.Pose pose, VertexConsumer out, Basis b, Vec3 center,
            double radius, int color, double phase) {
        for (int i = 0; i < 6; i++) {
            double a0 = phase + i * TAU / 6.0;
            double a1 = phase + (i + 1) * TAU / 6.0;
            Vec3 p0 = center.add(b.right.scale(Math.cos(a0) * radius))
                    .add(0, Math.sin(a0) * radius, 0);
            Vec3 p1 = center.add(b.right.scale(Math.cos(a1) * radius))
                    .add(0, Math.sin(a1) * radius, 0);
            prism(pose, out, p0, p1, radius * 0.055, color);
        }
    }

    private static void tornadoRibbon(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double phase, double height, int segments, int color) {
        for (int i = 0; i < segments; i++) {
            double t0 = i / (double) segments;
            double t1 = (i + 1) / (double) segments;
            double r0 = 0.35 + t0 * 2.15;
            double r1 = 0.35 + t1 * 2.15;
            double a0 = phase + t0 * TAU * 2.65;
            double a1 = phase + t1 * TAU * 2.65;
            double w0 = 0.10 + t0 * 0.16;
            double w1 = 0.10 + t1 * 0.16;
            Vec3 p0 = b.local(Math.cos(a0) * (r0 - w0), t0 * height, Math.sin(a0) * (r0 - w0));
            Vec3 p1 = b.local(Math.cos(a0) * (r0 + w0), t0 * height, Math.sin(a0) * (r0 + w0));
            Vec3 p2 = b.local(Math.cos(a1) * (r1 + w1), t1 * height, Math.sin(a1) * (r1 + w1));
            Vec3 p3 = b.local(Math.cos(a1) * (r1 - w1), t1 * height, Math.sin(a1) * (r1 - w1));
            quadTwoSided(pose, out, p0, p1, p2, p3, color);
        }
    }

    private static void helixRibbon(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double phase, double radius, double height, int segments, int color) {
        for (int i = 0; i < segments; i++) {
            double t0 = i / (double) segments;
            double t1 = (i + 1) / (double) segments;
            double a0 = phase + t0 * TAU * 1.4;
            double a1 = phase + t1 * TAU * 1.4;
            double y0 = (t0 - 0.5) * height;
            double y1 = (t1 - 0.5) * height;
            Vec3 p0 = b.local(Math.cos(a0) * (radius - 0.05), y0, Math.sin(a0) * (radius - 0.05));
            Vec3 p1 = b.local(Math.cos(a0) * (radius + 0.05), y0, Math.sin(a0) * (radius + 0.05));
            Vec3 p2 = b.local(Math.cos(a1) * (radius + 0.05), y1, Math.sin(a1) * (radius + 0.05));
            Vec3 p3 = b.local(Math.cos(a1) * (radius - 0.05), y1, Math.sin(a1) * (radius - 0.05));
            quadTwoSided(pose, out, p0, p1, p2, p3, color);
        }
    }

    private static void jaggedBolt(
            PoseStack.Pose pose, VertexConsumer out, Vec3 start, Vec3 end,
            int segments, double width, int color, long seed) {
        Random random = new Random(seed);
        Vec3 delta = end.subtract(start);
        Basis b = Basis.from(delta);
        Vec3 previous = start;
        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments;
            Vec3 current = start.add(delta.scale(t));
            if (i < segments) {
                double jitter = (1.0 - Math.abs(t - 0.5) * 1.2) * 0.42;
                current = current.add(b.right.scale((random.nextDouble() - 0.5) * jitter))
                        .add(b.up.scale((random.nextDouble() - 0.5) * jitter));
            }
            prism(pose, out, previous, current, width * (1.0 - t * 0.45), color);
            previous = current;
        }
    }

    private static void crystal(
            PoseStack.Pose pose, VertexConsumer out, Vec3 root,
            double height, double radius, int color) {
        Vec3 tip = root.add(0, height, 0);
        int sides = 5;
        for (int i = 0; i < sides; i++) {
            double a0 = i * TAU / sides;
            double a1 = (i + 1) * TAU / sides;
            Vec3 p0 = root.add(Math.cos(a0) * radius, 0, Math.sin(a0) * radius);
            Vec3 p1 = root.add(Math.cos(a1) * radius, 0, Math.sin(a1) * radius);
            Vec3 m0 = root.add(Math.cos(a0) * radius * 0.72, height * 0.68, Math.sin(a0) * radius * 0.72);
            Vec3 m1 = root.add(Math.cos(a1) * radius * 0.72, height * 0.68, Math.sin(a1) * radius * 0.72);
            quadTwoSided(pose, out, p0, p1, m1, m0, color);
            triangleTwoSided(pose, out, m0, m1, tip, color);
        }
    }

    private static void spike(
            PoseStack.Pose pose, VertexConsumer out, Vec3 root, Vec3 tip,
            double radius, int color) {
        Vec3 axis = tip.subtract(root);
        Basis b = Basis.from(axis);
        int sides = 5;
        for (int i = 0; i < sides; i++) {
            double a0 = i * TAU / sides;
            double a1 = (i + 1) * TAU / sides;
            Vec3 p0 = root.add(b.right.scale(Math.cos(a0) * radius))
                    .add(b.up.scale(Math.sin(a0) * radius));
            Vec3 p1 = root.add(b.right.scale(Math.cos(a1) * radius))
                    .add(b.up.scale(Math.sin(a1) * radius));
            triangleTwoSided(pose, out, p0, p1, tip, color);
        }
    }

    private static void groundCrack(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double angle, double inner, double outer, double width, int color) {
        Vec3 p0 = b.local(Math.cos(angle) * inner, 0.018, Math.sin(angle) * inner);
        Vec3 p1 = b.local(Math.cos(angle + 0.08) * ((inner + outer) * 0.52), 0.02,
                Math.sin(angle + 0.08) * ((inner + outer) * 0.52));
        Vec3 p2 = b.local(Math.cos(angle - 0.045) * outer, 0.018, Math.sin(angle - 0.045) * outer);
        prism(pose, out, p0, p1, width, color);
        prism(pose, out, p1, p2, width * 0.72, color);
    }

    private static void braidedBeam(
            PoseStack.Pose pose, VertexConsumer out, Vec3 start, Vec3 end,
            double age, double radius, int color) {
        Vec3 delta = end.subtract(start);
        Basis b = Basis.from(delta);
        int segments = 20;
        Vec3 previousA = start;
        Vec3 previousB = start;
        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments;
            double a = age * 0.22 + t * TAU * 2.0;
            Vec3 center = start.add(delta.scale(t));
            Vec3 offset = b.right.scale(Math.cos(a) * radius)
                    .add(b.up.scale(Math.sin(a) * radius));
            Vec3 currentA = center.add(offset);
            Vec3 currentB = center.subtract(offset);
            prism(pose, out, previousA, currentA, radius * 0.35, color);
            prism(pose, out, previousB, currentB, radius * 0.35, color);
            previousA = currentA;
            previousB = currentB;
        }
    }

    private static void wing(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            int side, double y, double span, int color) {
        Vec3 root = b.local(side * 0.22, y, -0.05);
        for (int feather = 0; feather < 5; feather++) {
            double f = feather / 4.0;
            Vec3 elbow = b.local(side * (0.65 + f * 0.45), y + 0.35 - f * 0.18,
                    -0.15 - f * 0.16);
            Vec3 tip = b.local(side * (span + f * 0.55), y + 0.25 - f * 0.42,
                    -0.35 - f * 0.35);
            taperedRibbon(pose, out, b, root, elbow, 0.10 - f * 0.015, color);
            taperedRibbon(pose, out, b, elbow, tip, 0.12 - f * 0.018, color);
        }
    }

    private static void verticalPillar(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double radius, double height, int color) {
        verticalPillarAt(pose, out, b, Vec3.ZERO, radius, height, color);
    }

    private static void verticalPillarAt(
            PoseStack.Pose pose, VertexConsumer out, Basis b, Vec3 center,
            double radius, double height, int color) {
        int sides = 12;
        for (int i = 0; i < sides; i++) {
            double a0 = i * TAU / sides;
            double a1 = (i + 1) * TAU / sides;
            Vec3 p0 = center.add(Math.cos(a0) * radius, 0, Math.sin(a0) * radius);
            Vec3 p1 = center.add(Math.cos(a1) * radius, 0, Math.sin(a1) * radius);
            Vec3 p2 = center.add(Math.cos(a1) * radius * 0.55, height, Math.sin(a1) * radius * 0.55);
            Vec3 p3 = center.add(Math.cos(a0) * radius * 0.55, height, Math.sin(a0) * radius * 0.55);
            quadTwoSided(pose, out, p0, p1, p2, p3, color);
        }
    }

    private static void chevron(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double angle, double radius, double y, double size, int color) {
        Vec3 center = b.local(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
        Vec3 tangent = b.local(-Math.sin(angle), 0, Math.cos(angle)).normalize();
        Vec3 radial = b.local(Math.cos(angle), 0, Math.sin(angle)).normalize();
        Vec3 p0 = center.subtract(tangent.scale(size)).subtract(radial.scale(size * 0.35));
        Vec3 p1 = center.add(radial.scale(size * 0.55));
        Vec3 p2 = center.add(tangent.scale(size)).subtract(radial.scale(size * 0.35));
        prism(pose, out, p0, p1, size * 0.09, color);
        prism(pose, out, p1, p2, size * 0.09, color);
    }

    private static void reticleBracket(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double angle, double radius, double y, int color) {
        Vec3 radial = b.right.scale(Math.cos(angle)).add(b.up.scale(Math.sin(angle)));
        Vec3 tangent = b.right.scale(-Math.sin(angle)).add(b.up.scale(Math.cos(angle)));
        Vec3 center = b.local(0, y, 0.7).add(radial.scale(radius));
        prism(pose, out, center.subtract(tangent.scale(0.26)),
                center.add(tangent.scale(0.26)), 0.045, color);
        prism(pose, out, center.subtract(radial.scale(0.28)),
                center.add(radial.scale(0.05)), 0.045, color);
    }

    private static void shieldGlyph(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double angle, double radius, double y, int color) {
        Vec3 center = b.local(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
        Basis face = Basis.from(center.lengthSqr() < 1.0E-5 ? b.forward : center);
        double w = 0.28;
        double h = 0.42;
        Vec3 a = center.add(face.right.scale(-w)).add(0, h, 0);
        Vec3 c = center.add(face.right.scale(w)).add(0, h, 0);
        Vec3 d = center.add(0, -h, 0).add(face.forward.scale(0.08));
        triangleTwoSided(pose, out, a, c, d, color);
    }

    private static void verticalBlade(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            Vec3 root, double height, double width, int color) {
        Vec3 tip = root.add(0, height, 0);
        energyBlade(pose, out, root, tip, width, color);
    }

    private static void taperedRibbon(
            PoseStack.Pose pose, VertexConsumer out, Basis basis,
            Vec3 start, Vec3 end, double width, int color) {
        Vec3 axis = end.subtract(start);
        Basis b = Basis.from(axis);
        Vec3 r0 = b.right.scale(width);
        Vec3 r1 = b.right.scale(width * 0.12);
        quadTwoSided(pose, out, start.subtract(r0), start.add(r0),
                end.add(r1), end.subtract(r1), color);
    }

    private static Vec3 polar(double angle, double radius, double y) {
        return new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
    }

    private static List<Vec3> parsePoints(String encoded, Vec3 fallback) {
        List<Vec3> result = new ArrayList<>();
        if (encoded != null && !encoded.isBlank()) {
            for (String token : encoded.split(";")) {
                String[] values = token.split(",");
                if (values.length != 3) continue;
                try {
                    result.add(new Vec3(
                            Double.parseDouble(values[0]),
                            Double.parseDouble(values[1]),
                            Double.parseDouble(values[2])));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed visual-only coordinates.
                }
            }
        }
        if (result.isEmpty()) result.add(fallback);
        return result;
    }

    private static long stateSeed(Random random, int index, int frame) {
        return random.nextLong() ^ ((long) index << 32) ^ frame * 0x9E3779B97F4A7C15L;
    }

    private static double envelope(double progress, double attack, double release) {
        double in = attack <= 0 ? 1.0 : clamp(progress / attack, 0.0, 1.0);
        double out = release <= 0 ? 1.0 : clamp((1.0 - progress) / release, 0.0, 1.0);
        return Math.min(in, out);
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int rgba(int r, int g, int b, int a) {
        return (clampInt(a) << 24) | (clampInt(r) << 16) | (clampInt(g) << 8) | clampInt(b);
    }

    private static int clampInt(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static void quadTwoSided(
            PoseStack.Pose pose, VertexConsumer out,
            Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int color) {
        quad(pose, out, p0, p1, p2, p3, color);
        quad(pose, out, p3, p2, p1, p0, color);
    }

    private static void triangleTwoSided(
            PoseStack.Pose pose, VertexConsumer out,
            Vec3 p0, Vec3 p1, Vec3 p2, int color) {
        triangle(pose, out, p0, p1, p2, color);
        triangle(pose, out, p2, p1, p0, color);
    }

    private static void quad(
            PoseStack.Pose pose, VertexConsumer out,
            Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int color) {
        Vec3 normal = p1.subtract(p0).cross(p2.subtract(p0));
        if (normal.lengthSqr() < 1.0E-8) normal = new Vec3(0, 1, 0);
        else normal = normal.normalize();
        vertex(pose, out, p0, color, 0, 0, normal);
        vertex(pose, out, p1, color, 1, 0, normal);
        vertex(pose, out, p2, color, 1, 1, normal);
        vertex(pose, out, p3, color, 0, 1, normal);
    }

    private static void triangle(
            PoseStack.Pose pose, VertexConsumer out,
            Vec3 p0, Vec3 p1, Vec3 p2, int color) {
        Vec3 normal = p1.subtract(p0).cross(p2.subtract(p0));
        if (normal.lengthSqr() < 1.0E-8) normal = new Vec3(0, 1, 0);
        else normal = normal.normalize();
        vertex(pose, out, p0, color, 0.5f, 0, normal);
        vertex(pose, out, p1, color, 0, 1, normal);
        vertex(pose, out, p2, color, 1, 1, normal);
        // Duplicate final vertex so triangle data is accepted by quad-oriented entity render types.
        vertex(pose, out, p2, color, 1, 1, normal);
    }

    private static void vertex(
            PoseStack.Pose pose, VertexConsumer out,
            Vec3 point, int color, float u, float v, Vec3 normal) {
        out.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >>> 24) & 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    private record Basis(Vec3 right, Vec3 up, Vec3 forward) {
        private static final Basis DOWN = Basis.from(new Vec3(0, -1, 0));

        static Basis from(Vec3 direction) {
            Vec3 forward = direction == null || direction.lengthSqr() < 1.0E-8
                    ? new Vec3(0, 0, 1) : direction.normalize();
            Vec3 reference = Math.abs(forward.y) > 0.92 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
            Vec3 right = reference.cross(forward);
            if (right.lengthSqr() < 1.0E-8) right = new Vec3(1, 0, 0);
            else right = right.normalize();
            Vec3 up = forward.cross(right).normalize();
            return new Basis(right, up, forward);
        }

        Vec3 local(double x, double y, double z) {
            return right.scale(x).add(up.scale(y)).add(forward.scale(z));
        }
    }
}
