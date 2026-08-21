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

        if (state.kind.startsWith("turret_wreck_")) {
            renderTurretWreck(pose, out, basis, age, state.kind, state.extra);
            return;
        }
        if (state.kind.startsWith("boss_presence_")) {
            renderBossPresence(pose, out, basis, age, state.kind, state.extra, false);
            return;
        }
        if (state.kind.startsWith("boss_phase_two_")) {
            renderBossPresence(pose, out, basis, age, state.kind, state.extra, true);
            return;
        }

        switch (state.kind) {
            case "vanguard_spin" -> renderVanguardSpin(pose, out, basis, age, progress);
            case "vanguard_rally" -> renderVanguardRally(pose, out, basis, age, progress);
            case "vanguard_blade_charge" -> renderBladeCharge(pose, out, basis, age, progress);
            case "vanguard_slam_charge" -> renderSlamCharge(pose, out, basis, age, progress);
            case "vanguard_blade_wave" -> renderBladeWave(pose, out, basis, age, progress);
            case "vanguard_slam_impact" -> renderSlamImpact(pose, out, basis, age, progress, random, state.extra);

            case "ranger_rapid" -> renderRapidFire(pose, out, basis, age, progress);
            case "ranger_focus" -> renderRangerFocus(pose, out, basis, age, progress);
            case "ranger_lock" -> renderTargetLock(pose, out, basis, age, progress);
            case "ranger_rain_field" -> renderArrowRainField(pose, out, basis, age, progress, random, state.extra);
            case "ranger_rain_impact" -> renderArrowRainImpact(pose, out, basis, age, progress, random, state.extra);
            case "ranger_energy_charge" -> renderEnergyCharge(pose, out, basis, age, progress);
            case "ranger_energy_projectile" -> renderEnergyProjectile(pose, out, basis, age, progress);
            case "ranger_ricochet_path" -> renderPath(pose, out, state, age, progress, 0x91D7FF, false);

            case "arcanist_fire_orb" -> renderFireOrb(pose, out, basis, age, progress);
            case "arcanist_fire_impact" -> renderFireImpact(pose, out, basis, age, progress, state.extra);
            case "arcanist_frost" -> renderFrostField(pose, out, basis, age, progress, state.extra);
            case "arcanist_tornado" -> renderTornado(pose, out, basis, age, progress, state.extra);
            case "arcanist_lightning" -> renderLightningField(pose, out, basis, age, progress, random, state.extra);

            case "luminar_heal_cast" -> renderHealCast(pose, out, basis, age, progress);
            case "luminar_heal_link" -> renderPath(pose, out, state, age, progress, 0xFFF2A8, true);
            case "luminar_cleanse_cast" -> renderCleanseCast(pose, out, basis, age, progress);
            case "luminar_cleanse_wave" -> renderCleanseWave(pose, out, state, age, progress);
            case "luminar_healing_field" -> renderHealingField(pose, out, basis, age, progress, state.extra);
            case "luminar_miracle_cast" -> renderMiracleCast(pose, out, basis, age, progress);
            case "luminar_miracle_wave" -> renderMiracleWave(pose, out, state, age, progress);

            case "warden_charge_cast" -> renderShieldCharge(pose, out, basis, age, progress);
            case "warden_taunt" -> renderTaunt(pose, out, basis, age, progress);
            case "warden_fortress" -> renderFortress(pose, out, basis, age, progress, true);
            case "warden_aegis" -> renderFortress(pose, out, basis, age, progress, false);

            case "turret_ballista_shot" -> renderDefenseShot(pose, out, state, age, progress, 0);
            case "turret_repeater_shot" -> renderDefenseShot(pose, out, state, age, progress, 1);
            case "turret_piercer_shot" -> renderDefenseShot(pose, out, state, age, progress, 2);
            case "turret_flame_shot" -> renderDefenseShot(pose, out, state, age, progress, 3);
            case "turret_frost_shot" -> renderDefenseShot(pose, out, state, age, progress, 4);
            case "turret_chain_shot" -> renderDefenseShot(pose, out, state, age, progress, 5);
            case "turret_bombard_arc" -> renderBombardArc(pose, out, state, age, progress);
            case "turret_bombard_impact" -> renderDefensePulse(pose, out, basis, age, progress, state.extra, 0);
            case "turret_nullifier_shot" -> renderDefenseShot(pose, out, state, age, progress, 6);
            case "turret_antiair_shot" -> renderDefenseShot(pose, out, state, age, progress, 7);
            case "turret_beacon_pulse" -> renderDefensePulse(pose, out, basis, age, progress, state.extra, 1);
            case "merc_ranger_shot" -> renderDefenseShot(pose, out, state, age, progress, 8);
            case "merc_bastion_guard" -> renderDefensePulse(pose, out, basis, age, progress, state.extra, 2);
            case "merc_striker_pressure" -> renderDefenseShot(pose, out, state, age, progress, 9);
            case "merc_medic_pulse" -> renderDefensePulse(pose, out, basis, age, progress, state.extra, 3);
            case "mercenary_presence_bastion" -> renderMercenaryPresence(pose, out, basis, age, state.extra, 0);
            case "mercenary_presence_striker" -> renderMercenaryPresence(pose, out, basis, age, state.extra, 1);
            case "mercenary_presence_ranger" -> renderMercenaryPresence(pose, out, basis, age, state.extra, 2);
            case "mercenary_presence_medic" -> renderMercenaryPresence(pose, out, basis, age, state.extra, 3);
            case "siege_structure_impact" -> renderDefensePulse(pose, out, basis, age, progress, state.extra, 4);
            case "turret_placement_preview" -> renderDefenseMaintenance(pose, out, basis, age, progress, 0);
            case "turret_deploy_pulse" -> renderDefenseMaintenance(pose, out, basis, age, progress, 1);
            case "defense_repair_pulse" -> renderDefenseMaintenance(pose, out, basis, age, progress, 2);
            case "turret_upgrade_burst" -> renderDefenseMaintenance(pose, out, basis, age, progress, 3);
            case "defense_breach_alarm" -> renderDefenseMaintenance(pose, out, basis, age, progress, 4);
            case "raid_front_warning" -> renderRaidFrontSignal(pose, out, basis, age, progress, state.extra, false);
            case "raid_front_arrival" -> renderRaidFrontSignal(pose, out, basis, age, progress, state.extra, true);
            case "raid_aerial_warning" -> renderAerialAssault(pose, out, basis, age, progress, state.extra, false);
            case "raid_aerial_impact" -> renderAerialAssault(pose, out, basis, age, progress, state.extra, true);

            case "turret_body_ballista" -> renderTurretBody(pose, out, basis, age, state.extra, 0);
            case "turret_body_repeater" -> renderTurretBody(pose, out, basis, age, state.extra, 1);
            case "turret_body_piercer" -> renderTurretBody(pose, out, basis, age, state.extra, 2);
            case "turret_body_flame" -> renderTurretBody(pose, out, basis, age, state.extra, 3);
            case "turret_body_frost" -> renderTurretBody(pose, out, basis, age, state.extra, 4);
            case "turret_body_chain" -> renderTurretBody(pose, out, basis, age, state.extra, 5);
            case "turret_body_bombard" -> renderTurretBody(pose, out, basis, age, state.extra, 6);
            case "turret_body_nullifier" -> renderTurretBody(pose, out, basis, age, state.extra, 7);
            case "turret_body_anti_air" -> renderTurretBody(pose, out, basis, age, state.extra, 8);
            case "turret_body_beacon" -> renderTurretBody(pose, out, basis, age, state.extra, 9);

            case "elite_aura_grappler" -> renderEliteAura(pose, out, basis, age, 0);
            case "elite_aura_firebrand" -> renderEliteAura(pose, out, basis, age, 1);
            case "elite_aura_assassin" -> renderEliteAura(pose, out, basis, age, 2);
            case "elite_aura_plague_weaver" -> renderEliteAura(pose, out, basis, age, 3);
            case "elite_aura_shock_rider" -> renderEliteAura(pose, out, basis, age, 4);
            case "elite_grapple_line" -> renderPath(pose, out, state, age, progress, 0xD9C19A, false);
            case "elite_firebrand_throw" -> renderEliteThrow(pose, out, state, age, progress);
            case "elite_firebrand_impact" -> renderEliteZone(pose, out, basis, age, progress, state.extra, 0);
            case "elite_plague_warning" -> renderEliteZone(pose, out, basis, age, progress, state.extra, 1);
            case "elite_plague_impact" -> renderEliteZone(pose, out, basis, age, progress, state.extra, 2);

            case "boss_phase_two_burst" -> renderBossZone(pose, out, basis, age, progress, state.extra, 0);
            case "boss_breach_warning" -> renderBossZone(pose, out, basis, age, progress, state.extra, 1);
            case "boss_breach_windup" -> renderPath(pose, out, state, age, progress, 0xFFB35E, true);
            case "boss_breach_impact" -> renderBossZone(pose, out, basis, age, progress, state.extra, 2);
            case "boss_ritual_warning" -> renderBossZone(pose, out, basis, age, progress, state.extra, 3);
            case "boss_ritual_impact" -> renderBossZone(pose, out, basis, age, progress, state.extra, 4);
            case "boss_duel_mark" -> renderBossDuelMark(pose, out, basis, age, progress);
            case "boss_duel_impact" -> renderBossZone(pose, out, basis, age, progress, state.extra, 5);
            case "boss_bloodbound_warning" -> renderBossZone(pose, out, basis, age, progress, state.extra, 6);
            case "boss_bloodbound_impact" -> renderBossZone(pose, out, basis, age, progress, state.extra, 7);
            case "boss_storm_warning" -> renderBossZone(pose, out, basis, age, progress, state.extra, 8);
            default -> renderFallbackRune(pose, out, basis, age, progress);
        }
    }

    private static void renderAerialAssault(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, String extra, boolean impact) {
        AerialSignal signal = parseAerialSignal(extra);
        int role = signal.role();
        boolean structure = signal.structure();
        double fade = Math.max(0.0, 1.0 - progress);
        double dangerRadius = structure
                ? (role == 1 ? 2.65 : role == 2 ? 1.75 : 2.15)
                : Math.max(0.6, signal.radius());
        int primary = switch (role) {
            case 1 -> rgba(139, 121, 255, (int) ((impact ? 245 : 215) * Math.max(0.28, fade)));
            case 2 -> rgba(91, 232, 255, (int) ((impact ? 250 : 225) * Math.max(0.28, fade)));
            default -> rgba(116, 207, 255, (int) ((impact ? 238 : 205) * Math.max(0.28, fade)));
        };
        int secondary = switch (role) {
            case 1 -> rgba(231, 218, 255, (int) (160 * Math.max(0.22, fade)));
            case 2 -> rgba(246, 255, 173, (int) (155 * Math.max(0.22, fade)));
            default -> rgba(225, 250, 255, (int) (150 * Math.max(0.22, fade)));
        };

        // The inner warning ring is the actual dodge radius for player-targeted strikes.
        // Animation is kept on secondary geometry so the gameplay boundary never lies.
        double primaryRadius = impact ? dangerRadius + progress * (role == 1 ? 3.4 : role == 2 ? 2.2 : 2.8) : dangerRadius;
        ring(pose, out, b, primaryRadius, 0.055, impact ? 0.15 : 0.105,
                role == 1 ? 64 : 56, primary, age * (role == 2 ? 0.09 : 0.045));
        double pulse = 0.92 + Math.sin(age * (role == 2 ? 0.48 : 0.28)) * 0.07;
        ring(pose, out, b, Math.max(0.55, dangerRadius * 0.66 * pulse), 0.085, 0.05,
                44, secondary, -age * (role == 2 ? 0.12 : 0.065));

        int markers = role == 1 ? 8 : role == 2 ? 3 : 4;
        for (int i = 0; i < markers; i++) {
            double a = i * TAU / markers + age * (role == 2 ? 0.085 : role == 1 ? -0.025 : 0.025);
            double markerRadius = Math.max(0.65, dangerRadius * (role == 1 ? 0.92 : 0.84));
            double rise = impact ? 0.18 + progress * (role == 2 ? 0.48 : 0.65) : 0.12;
            chevron(pose, out, b, a, markerRadius, rise,
                    role == 1 ? 0.42 : role == 2 ? 0.28 : 0.34, primary);
        }
        if (role == 1) {
            ring(pose, out, b, dangerRadius * 0.82, 0.16, 0.035, 40,
                    withAlpha(secondary, Math.max(28, (int) (125 * fade))), age * 0.03);
            verticalPillar(pose, out, b, impact ? 0.55 : 0.24,
                    impact ? 5.2 * fade + 0.5 : 3.6, withAlpha(primary, impact ? 195 : 120));
        } else if (role == 2) {
            verticalPillar(pose, out, b, impact ? 0.30 : 0.12,
                    impact ? 3.2 * fade + 0.35 : 2.1, withAlpha(primary, impact ? 175 : 92));
        } else {
            verticalPillar(pose, out, b, impact ? 0.42 : 0.18,
                    impact ? 4.2 * fade + 0.4 : 2.8, withAlpha(primary, impact ? 185 : 105));
        }
    }

    private static AerialSignal parseAerialSignal(String extra) {
        int role = 0;
        boolean structure = false;
        double radius = 2.75;
        if (extra != null && !extra.isBlank()) {
            String[] parts = extra.split("\\|", -1);
            try { role = Math.max(0, Math.min(2, Integer.parseInt(parts[0]))); }
            catch (NumberFormatException ignored) {}
            structure = parts.length > 1 && "1".equals(parts[1]);
            if (parts.length > 2) {
                try { radius = Math.max(0.0, Double.parseDouble(parts[2])); }
                catch (NumberFormatException ignored) {}
            }
        }
        return new AerialSignal(role, structure, radius);
    }

    private record AerialSignal(int role, boolean structure, double radius) {}

    private static void renderRaidFrontSignal(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, String extra, boolean arrival) {
        boolean main = "1".equals(extra);
        double fade = Math.max(0.0, 1.0 - progress);
        double pulse = 0.82 + Math.sin(age * 0.18) * 0.12;
        int primary = main
                ? rgba(238, 76, 70, (int) ((arrival ? 235 : 190) * Math.max(0.35, fade)))
                : rgba(222, 146, 72, (int) ((arrival ? 210 : 165) * Math.max(0.35, fade)));
        int secondary = main
                ? rgba(255, 174, 105, (int) (150 * Math.max(0.25, fade)))
                : rgba(242, 202, 126, (int) (125 * Math.max(0.25, fade)));
        double radius = (main ? 2.7 : 2.05) * pulse + (arrival ? progress * 1.8 : 0.0);
        ring(pose, out, b, radius, 0.06, arrival ? 0.14 : 0.09, 56, primary, age * 0.035);
        ring(pose, out, b, radius * 0.67, 0.09, 0.055, 44, secondary, -age * 0.05);
        int markers = main ? 8 : 5;
        for (int i = 0; i < markers; i++) {
            double a = i * TAU / markers + (arrival ? -age * 0.018 : age * 0.025);
            chevron(pose, out, b, a, radius * 0.88, 0.12 + (arrival ? progress * 0.55 : 0.0),
                    main ? 0.38 : 0.30, primary);
        }
        if (arrival) {
            verticalPillar(pose, out, b, main ? 0.52 : 0.38, 3.8 * fade + 0.55, primary);
            ring(pose, out, b, Math.max(0.6, radius * (1.18 + progress * 0.28)), 0.04, 0.05,
                    48, withAlpha(secondary, Math.max(18, (int) (110 * fade))), -age * 0.025);
        } else {
            verticalPillar(pose, out, b, main ? 0.20 : 0.14, main ? 2.1 : 1.55,
                    withAlpha(primary, main ? 110 : 82));
        }
    }

    private static void renderDefenseMaintenance(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, int mode) {
        double fade = Math.max(0.0, 1.0 - progress);
        int primary = switch (mode) {
            case 0 -> rgba(82, 222, 197, (int) (165 * fade));
            case 1 -> rgba(244, 197, 95, (int) (210 * fade));
            case 2 -> rgba(112, 214, 155, (int) (195 * fade));
            case 3 -> rgba(255, 213, 112, (int) (220 * fade));
            default -> rgba(232, 72, 72, (int) (225 * fade));
        };
        double radius = mode == 4 ? 2.2 + progress * 3.2 : 0.78 + progress * 1.35;
        ring(pose, out, b, radius, 0.035, mode == 4 ? 0.12 : 0.075,
                mode == 4 ? 64 : 48, primary, age * (mode == 4 ? 0.035 : 0.055));
        ring(pose, out, b, Math.max(0.45, radius * 0.68), 0.055, 0.045,
                40, withAlpha(primary, Math.max(20, (int) (135 * fade))), -age * 0.04);
        if (mode == 0) {
            for (int i = 0; i < 4; i++) {
                double a = i * TAU / 4.0 + age * 0.025;
                chevron(pose, out, b, a, 0.95, 0.12, 0.25, primary);
            }
        } else if (mode == 1) {
            verticalPillar(pose, out, b, 0.34 + progress * 0.18, 2.2 * fade + 0.3, primary);
        } else if (mode == 2) {
            for (int i = 0; i < 3; i++) {
                ring(pose, out, b, 0.72 + i * 0.24, 0.28 + i * 0.34 + progress * 0.55,
                        0.045, 36, withAlpha(primary, Math.max(20, (int) ((170 - i * 22) * fade))), age * 0.02);
            }
        } else if (mode == 3) {
            for (int i = 0; i < 6; i++) {
                double a = i * TAU / 6.0;
                chevron(pose, out, b, a, 0.82 + progress * 0.45,
                        0.45 + progress * 1.25, 0.30, primary);
            }
            verticalPillar(pose, out, b, 0.22, 2.8 * fade + 0.35, primary);
        } else {
            for (int i = 0; i < 8; i++) {
                double a = i * TAU / 8.0 + age * 0.015;
                groundCrack(pose, out, b, a, 0.65, 2.4 + progress * 1.8, 0.08, primary);
            }
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
        double fade = envelope(progress, 0.08, 0.18);
        for (int i = 0; i < 3; i++) {
            ring(pose, out, b, 0.55 + i * 0.46 + progress * 0.42,
                    0.05 + i * 0.11, 0.075, 42,
                    rgba(255, 92 + i * 26, 66, (int) ((155 - i * 18) * fade)),
                    -age * (0.045 + i * 0.012));
        }
        for (int i = 0; i < 8; i++) {
            double a = i * TAU / 8.0 + age * 0.025;
            Vec3 start = b.local(Math.cos(a) * 0.58,
                    2.35 - progress * 1.28, Math.sin(a) * 0.58);
            Vec3 end = b.local(Math.cos(a) * 1.22,
                    0.22, Math.sin(a) * 1.22);
            prism(pose, out, start, end, 0.055,
                    rgba(238, 67, 54, (int) (150 * fade)));
        }
    }

    private static void renderBladeWave(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double fade = 1.0 - progress * 0.70;
        horizontalSlash(pose, out, b, 3.6, 0.0, 0.15, 0.07,
                rgba(120, 215, 255, (int) (225 * fade)));
        horizontalSlash(pose, out, b, 3.0, 0.01, 0.065, 0.035,
                rgba(236, 252, 255, (int) (190 * fade)));
    }

    private static void renderSlamImpact(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, Random random, String extra) {
        EffectMeta meta = effectMeta(extra, 8.5);
        double radius = 0.8 + progress * Math.max(0.2, meta.radius() - 0.8);
        ring(pose, out, b, radius, 0.035, 0.18 + progress * 0.22, 72,
                rgba(255, 74, 48, (int) (210 * (1.0 - progress))), age * 0.01);
        if (meta.rank() >= 3) {
            ring(pose, out, b, radius * 0.72, 0.042, 0.07, 56,
                    rgba(255, 161, 77, (int) (145 * (1.0 - progress))), -age * 0.014);
        }
        random.setSeed(random.nextLong() ^ 0x5A17L);
        int cracks = 18 + meta.rank() * 2;
        for (int i = 0; i < cracks; i++) {
            double a = i * TAU / cracks + random.nextDouble() * 0.16;
            double inner = 0.35 + random.nextDouble() * 0.4;
            double outer = radius * (0.72 + random.nextDouble() * 0.28);
            groundCrack(pose, out, b, a, inner, outer, 0.05 + random.nextDouble() * 0.06,
                    rgba(255, 103, 48, (int) (190 * (1.0 - progress))));
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
        Vec3 core = b.local(0.0, 0.0, 0.18);
        sphere(pose, out, core, 0.18 + Math.sin(age * 0.25) * 0.035,
                8, 12, rgba(255, 211, 88, (int) (210 * fade)));
        ringVertical(pose, out, b, 0.48, 0.0, 0.035, 44,
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
        ringVertical(pose, out, b, radius, 0.0, 0.045, 56,
                rgba(255, 207, 76, (int) (220 * fade)), -age * 0.05);
        ringVertical(pose, out, b, radius * 0.45, 0.0, 0.028, 40,
                rgba(255, 247, 180, (int) (175 * fade)), age * 0.07);
        for (int i = 0; i < 4; i++) {
            reticleBracket(pose, out, b, i * Math.PI / 2.0, radius + 0.22, 0.0,
                    rgba(255, 224, 112, (int) (205 * fade)));
        }
    }

    private static void renderArrowRainField(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, Random random, String extra) {
        EffectMeta meta = effectMeta(extra, 8.5);
        double radius = meta.radius();
        ring(pose, out, b, radius, 0.012, 0.11, 112,
                rgba(88, 188, 255, 180), 0.0);
        ring(pose, out, b, radius * 0.72, 0.018, 0.045, 88,
                rgba(149, 223, 255, 95), -age * 0.008);
        if (meta.rank() >= 3) {
            ring(pose, out, b, radius * 0.42, 0.022, 0.035, 64,
                    rgba(210, 242, 255, 90), age * 0.012);
        }
        int arrows = 18 + meta.rank() * 3;
        for (int i = 0; i < arrows; i++) {
            double a = i * 2.399963229728653 + (i % 3) * 0.17;
            double r = Math.sqrt((i + 0.5) / arrows) * radius * 0.92;
            double cycle = fract(progress * 5.8 + i * 0.173);
            double y = 8.5 - cycle * 9.5;
            Vec3 p = b.local(Math.cos(a) * r, y, Math.sin(a) * r);
            double fadeOut = 1.0 - Math.max(0.0, cycle - 0.72) / 0.28;
            customArrow(pose, out, Basis.DOWN, p, 0.82 + (i % 4) * 0.07, 0.06,
                    rgba(164, 228, 255, (int) (195 * fadeOut)));
        }
    }

    private static void renderArrowRainImpact(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, Random random, String extra) {
        EffectMeta meta = effectMeta(extra, 8.5);
        double pulse = Math.min(meta.radius(), 0.45 + progress * meta.radius());
        ring(pose, out, b, pulse, 0.012, 0.08, 72,
                rgba(124, 211, 255, (int) (145 * (1.0 - progress))), 0.0);
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
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, String extra) {
        EffectMeta meta = effectMeta(extra, 7.5);
        double radius = meta.radius();
        runeDisc(pose, out, b, radius, 0.022, age * 0.012,
                rgba(112, 218, 255, 105));
        ring(pose, out, b, radius, 0.028, 0.10, 96,
                rgba(189, 246, 255, 165), -age * 0.018);
        if (meta.rank() >= 3) {
            ring(pose, out, b, radius * 0.62, 0.034, 0.055, 72,
                    rgba(220, 251, 255, 105), age * 0.024);
        }
        int crystals = 12 + meta.rank() * 2;
        for (int i = 0; i < crystals; i++) {
            double a = i * TAU / crystals + (i % 2) * 0.12;
            double r = radius * (0.28 + 0.58 * ((i % 5) / 4.0));
            double h = 0.7 + (i % 4) * 0.33 + Math.sin(age * 0.12 + i) * 0.12;
            crystal(pose, out, b.local(Math.cos(a) * r, 0.02, Math.sin(a) * r),
                    h, 0.18 + (i % 3) * 0.04,
                    rgba(146, 228, 255, 175));
        }
    }

    private static void renderTornado(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, String extra) {
        EffectMeta meta = effectMeta(extra, 8.5);
        double scale = meta.radius() / 8.5;
        ring(pose, out, b, meta.radius(), 0.018, 0.085, 112,
                rgba(150, 155, 164, 115), age * 0.012);
        if (meta.rank() >= 3) {
            ring(pose, out, b, meta.radius() * 0.68, 0.024, 0.048, 88,
                    rgba(190, 194, 202, 90), -age * 0.018);
        }
        int strands = 8 + Math.min(4, meta.rank());
        for (int strand = 0; strand < strands; strand++) {
            double phase = strand * TAU / strands + age * (0.19 + strand * 0.006);
            int shade = 118 + (strand % 4) * 22;
            tornadoRibbon(pose, out, b, phase, 5.8 * Math.min(1.28, scale), 46,
                    rgba(shade, shade + 4, shade + 9, 125 + Math.min(8, strand) * 8));
        }
        int fragments = 24 + meta.rank() * 4;
        for (int i = 0; i < fragments; i++) {
            double cycle = fract(age * 0.035 + i * 0.117);
            double y = 0.18 + cycle * 5.4;
            double radius = (0.65 + cycle * 2.7 + (i % 3) * 0.14) * Math.min(1.35, scale);
            double angle = age * 0.17 + i * 2.399963229728653;
            Vec3 start = b.local(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            Vec3 end = start.add(b.local(-Math.sin(angle) * 0.28, 0.10,
                    Math.cos(angle) * 0.28));
            int shade = 105 + (i % 5) * 18;
            prism(pose, out, start, end, 0.06 + (i % 3) * 0.018,
                    rgba(shade, shade, shade + 5, 155));
        }
    }

    private static void renderLightningField(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, Random random, String extra) {
        EffectMeta meta = effectMeta(extra, 18.0);
        double radius = meta.radius();
        ring(pose, out, b, radius, 0.018, 0.15, 128,
                rgba(188, 128, 255, 145), -age * 0.012);
        ring(pose, out, b, radius * 0.55, 0.026, 0.06, 96,
                rgba(229, 207, 255, 105), age * 0.018);
        if (meta.rank() >= 3) {
            ring(pose, out, b, radius * 0.78, 0.032, 0.035, 112,
                    rgba(218, 183, 255, 85), -age * 0.026);
        }
        if (meta.rank() >= 5) {
            for (int i = 0; i < 12; i++) {
                double a = i * TAU / 12.0;
                Vec3 start = b.local(Math.cos(a) * radius * 0.90, 0.025,
                        Math.sin(a) * radius * 0.90);
                Vec3 end = b.local(Math.cos(a) * radius, 0.028,
                        Math.sin(a) * radius);
                prism(pose, out, start, end, 0.035,
                        rgba(238, 220, 255, 115));
            }
        }
        // Vertical worm-like procedural bolts were removed. Actual visual-only
        // Minecraft lightning entities now provide every strike column.
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
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, String extra) {
        EffectMeta meta = effectMeta(extra, 7.5);
        double radius = meta.radius();
        runeDisc(pose, out, b, radius, 0.018, age * 0.008,
                rgba(255, 239, 153, 105));
        ring(pose, out, b, radius, 0.024, 0.09, 112,
                rgba(255, 248, 188, 145), age * 0.015);
        ring(pose, out, b, radius * 0.62, 0.032, 0.06, 88,
                rgba(255, 248, 188, 110), -age * 0.018);
        if (meta.rank() >= 3) {
            ring(pose, out, b, radius * 0.34, 0.040, 0.045, 64,
                    rgba(255, 255, 220, 100), age * 0.025);
        }
        int pillars = 8 + meta.rank() * 2;
        for (int i = 0; i < pillars; i++) {
            double a = i * TAU / pillars + age * 0.014;
            Vec3 root = b.local(Math.cos(a) * radius * 0.62, 0.05,
                    Math.sin(a) * radius * 0.62);
            verticalBlade(pose, out, b, root,
                    1.0 + 0.35 * Math.sin(age * 0.13 + i),
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

    private static EffectMeta effectMeta(String encoded, double fallbackRadius) {
        double radius = fallbackRadius;
        int rank = 0;
        if (encoded != null && !encoded.isBlank()) {
            String[] parts = encoded.split("\\|", -1);
            try { if (parts.length > 0) radius = Double.parseDouble(parts[0]); }
            catch (NumberFormatException ignored) {}
            try { if (parts.length > 1) rank = Integer.parseInt(parts[1]); }
            catch (NumberFormatException ignored) {}
        }
        return new EffectMeta(Math.max(0.25, radius), Math.max(0, rank));
    }

    private record EffectMeta(double radius, int rank) {}

    private static void renderDefenseShot(
            PoseStack.Pose pose, VertexConsumer out, VillageSkillEffectRenderState state,
            double age, double progress, int style) {
        Vec3 origin = new Vec3(state.x, state.y, state.z);
        List<Vec3> points = parsePoints(state.extra, origin);
        if (points.size() < 2) return;
        Vec3 a = points.get(0).subtract(origin);
        Vec3 z = points.get(1).subtract(origin);
        Vec3 delta = z.subtract(a);
        if (delta.lengthSqr() < 1.0E-6) return;
        Basis b = Basis.from(delta);
        double travel = clamp(progress * 1.18, 0.0, 1.0);
        Vec3 center = a.lerp(z, travel);
        int color = switch (style) {
            case 2 -> rgba(255, 235, 180, 230);
            case 3 -> rgba(255, 92, 34, 220);
            case 4 -> rgba(121, 220, 255, 225);
            case 5 -> rgba(126, 208, 255, 230);
            case 6 -> rgba(207, 135, 255, 225);
            case 7 -> rgba(255, 214, 109, 230);
            case 8 -> rgba(151, 224, 255, 230);
            case 9 -> rgba(255, 151, 63, 210);
            default -> rgba(234, 239, 224, 225);
        };
        double fade = 1.0 - progress * 0.58;
        if (style == 3) {
            Vec3 back = center.subtract(b.forward.scale(1.55));
            braidedBeam(pose, out, back, center.add(b.forward.scale(0.45)), age, 0.18,
                    rgba(255, 103, 35, (int) (210 * fade)));
            sphere(pose, out, center, 0.24, 7, 10, color);
        } else if (style == 5 || style == 6) {
            braidedBeam(pose, out, a, z, age * (style == 5 ? 1.8 : 0.7),
                    style == 5 ? 0.10 : 0.13,
                    withAlpha(color, (int) (205 * fade)));
        } else if (style == 9) {
            prism(pose, out, center.subtract(b.forward.scale(1.0)), center.add(b.forward.scale(0.35)),
                    0.10, color);
        } else {
            double length = style == 2 ? 2.05 : style == 7 ? 1.65 : style == 1 ? 0.72 : 1.25;
            double thickness = style == 2 ? 0.105 : style == 1 ? 0.045 : 0.07;
            customArrow(pose, out, b, center, length, thickness, color);
            prism(pose, out, center.subtract(b.forward.scale(length * 1.15)),
                    center.subtract(b.forward.scale(length * 0.25)), thickness * 0.45,
                    withAlpha(color, (int) (120 * fade)));
        }
    }

    private static void renderBombardArc(
            PoseStack.Pose pose, VertexConsumer out, VillageSkillEffectRenderState state,
            double age, double progress) {
        Vec3 origin = new Vec3(state.x, state.y, state.z);
        List<Vec3> points = parsePoints(state.extra, origin);
        if (points.size() < 2) return;
        Vec3 a = points.get(0).subtract(origin);
        Vec3 z = points.get(1).subtract(origin);
        double horizontal = Math.hypot(z.x - a.x, z.z - a.z);
        Vec3 control = a.lerp(z, 0.5).add(0.0, Math.max(4.5, horizontal * 0.16), 0.0);
        Vec3 previous = a;
        for (int i = 1; i <= 18; i++) {
            double t = i / 18.0;
            Vec3 current = bezier(a, control, z, t);
            prism(pose, out, previous, current, 0.045,
                    rgba(255, 137, 58, (int) (110 * (1.0 - progress * 0.6))));
            previous = current;
        }
        Vec3 shell = bezier(a, control, z, clamp(progress * 1.05, 0.0, 1.0));
        sphere(pose, out, shell, 0.24, 8, 12, rgba(255, 202, 93, 235));
        sphere(pose, out, shell, 0.11, 7, 10, rgba(255, 245, 191, 245));
    }

    private static Vec3 bezier(Vec3 a, Vec3 control, Vec3 z, double t) {
        double u = 1.0 - t;
        return a.scale(u * u).add(control.scale(2.0 * u * t)).add(z.scale(t * t));
    }

    private static void renderDefensePulse(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress,
            String encodedRadius, int style) {
        double maxRadius = 4.0;
        try { maxRadius = Double.parseDouble(encodedRadius); }
        catch (NumberFormatException ignored) {}
        maxRadius = Math.max(0.8, maxRadius);
        double radius = 0.35 + progress * maxRadius;
        int color = switch (style) {
            case 1 -> rgba(115, 235, 182, (int) (200 * (1.0 - progress)));
            case 2 -> rgba(108, 186, 255, (int) (210 * (1.0 - progress)));
            case 3 -> rgba(255, 223, 126, (int) (205 * (1.0 - progress)));
            case 4 -> rgba(255, 111, 67, (int) (220 * (1.0 - progress)));
            default -> rgba(255, 165, 72, (int) (220 * (1.0 - progress)));
        };
        ring(pose, out, b, radius, 0.06, style == 4 ? 0.18 : 0.11, 64, color, age * 0.015);
        if (style == 2 || style == 3) {
            ring(pose, out, b, radius * 0.64, 0.82, 0.06, 48, color, -age * 0.025);
        }
        if (style == 0 || style == 4) {
            sphere(pose, out, Vec3.ZERO, Math.max(0.22, radius * 0.32), 8, 12,
                    withAlpha(color, Math.max(20, (int) (90 * (1.0 - progress)))));
        }
    }

    private static void renderTurretBody(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, String extra, int style) {
        TurretPresentation presentation = parseTurretPresentation(extra);
        double scale = 1.0 + (presentation.level() - 1) * 0.075;
        boolean disabled = presentation.disabled();
        int metal = disabled ? rgba(94, 91, 86, 210) : rgba(166, 174, 177, 230);
        int dark = rgba(64, 69, 72, 235);
        int bright = switch (style) {
            case 3 -> rgba(255, 101, 39, disabled ? 90 : 225);
            case 4 -> rgba(113, 218, 255, disabled ? 90 : 220);
            case 5 -> rgba(120, 202, 255, disabled ? 90 : 225);
            case 7 -> rgba(207, 127, 255, disabled ? 90 : 225);
            case 9 -> rgba(119, 244, 183, disabled ? 90 : 225);
            default -> rgba(255, 213, 126, disabled ? 90 : 215);
        };

        verticalPillarAt(pose, out, b, b.local(0, 0.72, 0), 0.34 * scale, 0.72 * scale, dark);
        ring(pose, out, b, 0.46 * scale, 1.40, 0.07, 40, metal, age * 0.002);

        switch (style) {
            case 0 -> {
                Vec3 railA = b.local(0, 1.48, -0.28);
                Vec3 railB = b.local(0, 1.48, 1.28 * scale);
                prism(pose, out, railA, railB, 0.105 * scale, metal);
                prism(pose, out, b.local(-1.00 * scale, 1.48, 0.34), b.local(1.00 * scale, 1.48, 0.34), 0.085, metal);
                prism(pose, out, b.local(-1.00 * scale, 1.48, 0.34), railB, 0.025, bright);
                prism(pose, out, b.local(1.00 * scale, 1.48, 0.34), railB, 0.025, bright);
            }
            case 1 -> {
                for (int i = -1; i <= 1; i++) {
                    Vec3 a = b.local(i * 0.24, 1.43 + Math.abs(i) * 0.08, 0.02);
                    Vec3 z = b.local(i * 0.24, 1.43 + Math.abs(i) * 0.08, 1.12 * scale);
                    prism(pose, out, a, z, 0.072, i == 0 ? bright : metal);
                }
                sphere(pose, out, b.local(0, 1.45, -0.08), 0.35 * scale, 8, 12, dark);
            }
            case 2 -> {
                prism(pose, out, b.local(0, 1.52, -0.34), b.local(0, 1.52, 1.62 * scale), 0.145 * scale, metal);
                ring(pose, out, b, 0.30 * scale, 1.52, 0.09, 32, bright, 0.0);
                prism(pose, out, b.local(-0.48, 1.32, 0.08), b.local(0.48, 1.32, 0.08), 0.09, dark);
            }
            case 3 -> {
                sphere(pose, out, b.local(-0.38, 1.26, -0.05), 0.30 * scale, 8, 12, dark);
                sphere(pose, out, b.local(0.38, 1.26, -0.05), 0.30 * scale, 8, 12, dark);
                prism(pose, out, b.local(0, 1.48, 0.02), b.local(0, 1.48, 1.18 * scale), 0.16, metal);
                sphere(pose, out, b.local(0, 1.48, 1.24 * scale), 0.19, 7, 10, bright);
            }
            case 4 -> {
                prism(pose, out, b.local(0, 1.43, -0.08), b.local(0, 1.43, 0.96 * scale), 0.11, metal);
                crystal(pose, out, b.local(0, 1.25, 0.84 * scale), 0.88 * scale, 0.28 * scale, bright);
                for (int side : new int[]{-1, 1}) {
                    crystal(pose, out, b.local(side * 0.42, 1.12, 0.08), 0.55, 0.16, withAlpha(bright, 180));
                }
            }
            case 5 -> {
                verticalPillarAt(pose, out, b, b.local(0, 1.34, 0), 0.17, 1.18 * scale, bright);
                sphere(pose, out, b.local(0, 2.48 * scale, 0), 0.24, 8, 12, bright);
                ring(pose, out, b, 0.64 * scale, 1.74, 0.055, 44, bright, age * 0.055);
                ring(pose, out, b, 0.46 * scale, 2.08, 0.045, 36, withAlpha(bright, 165), -age * 0.075);
            }
            case 6 -> {
                Vec3 a = b.local(0, 1.18, -0.26);
                Vec3 z = b.local(0, 2.22 * scale, 0.92 * scale);
                prism(pose, out, a, z, 0.22 * scale, metal);
                sphere(pose, out, a, 0.36 * scale, 8, 12, dark);
                ring(pose, out, Basis.from(z.subtract(a)), 0.27, 0.02, 0.08, 32, bright, 0.0);
            }
            case 7 -> {
                sphere(pose, out, b.local(0, 1.62, 0), 0.33 * scale, 9, 14, bright);
                ring(pose, out, b, 0.70 * scale, 1.62, 0.055, 48, bright, age * 0.045);
                ring(pose, out, b, 0.52 * scale, 1.62, 0.045, 40, withAlpha(bright, 160), -age * 0.07);
                prism(pose, out, b.local(0, 1.58, 0.18), b.local(0, 1.58, 0.96), 0.08, metal);
            }
            case 8 -> {
                for (int side : new int[]{-1, 1}) {
                    Vec3 a = b.local(side * 0.26, 1.24, -0.18);
                    Vec3 z = b.local(side * 0.26, 2.25 * scale, 1.18 * scale);
                    prism(pose, out, a, z, 0.095, side < 0 ? metal : bright);
                }
                prism(pose, out, b.local(-0.56, 1.24, -0.12), b.local(0.56, 1.24, -0.12), 0.09, dark);
            }
            case 9 -> {
                verticalPillarAt(pose, out, b, b.local(0, 1.28, 0), 0.20, 1.42 * scale, bright);
                crystal(pose, out, b.local(0, 2.42 * scale, 0), 0.62, 0.24, bright);
                for (int i = 0; i < 3; i++) {
                    ring(pose, out, b, (0.44 + i * 0.18) * scale, 1.78 + i * 0.34,
                            0.045, 42, withAlpha(bright, 185 - i * 25), age * (0.035 + i * 0.012));
                }
            }
        }

        if (presentation.level() >= 3) {
            ring(pose, out, b, 0.58 * scale, 1.06, 0.035, 40,
                    withAlpha(bright, 110 + presentation.level() * 15), -age * 0.025);
        }
        if (presentation.disabled()) {
            ring(pose, out, b, 0.66 * scale, 1.46, 0.06, 44,
                    rgba(255, 67, 78, (int) (120 + 70 * (0.5 + 0.5 * Math.sin(age * 0.34)))), age * 0.08);
        }
    }

    private static void renderTurretWreck(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, String kind, String extra) {
        TurretPresentation presentation = parseTurretPresentation(extra);
        double scale = 0.92 + presentation.level() * 0.045;
        int dark = rgba(61, 58, 55, 225);
        int ember = kind.contains("flame") || kind.contains("bombard")
                ? rgba(255, 92, 43, 110) : rgba(137, 151, 158, 100);
        prism(pose, out, b.local(-0.46 * scale, 0.25, -0.34), b.local(0.52 * scale, 0.88, 0.26), 0.13, dark);
        prism(pose, out, b.local(0.44 * scale, 0.22, -0.25), b.local(-0.34 * scale, 0.66, 0.62), 0.10, dark);
        prism(pose, out, b.local(-0.18, 0.18, 0.52), b.local(0.65, 0.42, 0.86), 0.075, dark);
        sphere(pose, out, b.local(0.04, 0.55, 0.04), 0.20, 7, 10, ember);
    }

    private static TurretPresentation parseTurretPresentation(String extra) {
        int level = 1;
        boolean disabled = false;
        if (extra != null && !extra.isBlank()) {
            String[] parts = extra.split("\\|", -1);
            try { level = Math.max(1, Math.min(5, Integer.parseInt(parts[0]))); }
            catch (NumberFormatException ignored) {}
            disabled = parts.length > 1 && "1".equals(parts[1]);
        }
        return new TurretPresentation(level, disabled);
    }

    private record TurretPresentation(int level, boolean disabled) {}

    private static void renderMercenaryPresence(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, String encodedTier, int style) {
        int tier = 0;
        try { tier = Math.max(0, Math.min(3, Integer.parseInt(encodedTier == null ? "0" : encodedTier))); }
        catch (NumberFormatException ignored) {}
        double scale = 1.0 + tier * 0.085;
        double pulse = 0.96 + 0.04 * Math.sin(age * 0.10);
        int color = switch (style) {
            case 0 -> rgba(112, 190, 255, 150 + tier * 12);
            case 1 -> rgba(255, 119, 72, 150 + tier * 12);
            case 2 -> rgba(137, 226, 156, 150 + tier * 12);
            default -> rgba(255, 224, 135, 150 + tier * 12);
        };
        int pale = withAlpha(color, 92 + tier * 10);
        ring(pose, out, b, 0.78 * scale * pulse, 0.035, 0.038, 44, pale, age * 0.010);

        if (style == 0) {
            Vec3 shieldCenter = b.local(0.0, 1.10, 0.78 * scale);
            curvedShield(pose, out, b, shieldCenter, 1.32 * scale, 1.72 * scale, 0.30,
                    withAlpha(color, 92 + tier * 12));
            shieldFrame(pose, out, b, shieldCenter.add(b.forward.scale(0.035)),
                    1.32 * scale, 1.72 * scale, 0.30, color);
            for (int side : new int[]{-1, 1}) {
                prism(pose, out, b.local(side * 0.58 * scale, 0.55, 0.15),
                        b.local(side * 0.70 * scale, 1.65 * scale, 0.26), 0.075 + tier * 0.01, color);
            }
        } else if (style == 1) {
            prism(pose, out, b.local(-0.62 * scale, 0.52, -0.06),
                    b.local(0.58 * scale, 1.74 * scale, 0.64), 0.065 + tier * 0.012, color);
            prism(pose, out, b.local(0.62 * scale, 0.52, -0.06),
                    b.local(-0.58 * scale, 1.74 * scale, 0.64), 0.065 + tier * 0.012, color);
            for (int i = 0; i < 2 + tier; i++) {
                slashArc(pose, out, b, age * 0.018 + i * TAU / (2.0 + tier),
                        0.84 + i * 0.08, 0.88 + i * 0.10, 0.58, 0.035, pale);
            }
        } else if (style == 2) {
            prism(pose, out, b.local(-0.48 * scale, 0.46, -0.42),
                    b.local(-0.48 * scale, 1.82 * scale, -0.30), 0.09, pale);
            int arrows = 2 + tier;
            for (int i = 0; i < arrows; i++) {
                customArrow(pose, out, b, b.local(-0.50 * scale + i * 0.14, 1.44 + i * 0.10, -0.38),
                        0.86 + tier * 0.06, 0.035, color);
            }
            ringVertical(pose, out, b, 0.62 * scale, 1.12, 0.032, 42, pale, -age * 0.012);
        } else {
            verticalPillarAt(pose, out, b, b.local(0.56 * scale, 0.34, 0.02),
                    0.07 + tier * 0.008, 1.66 * scale, color);
            crystal(pose, out, b.local(0.56 * scale, 2.03 * scale, 0.02),
                    0.42 + tier * 0.06, 0.15 + tier * 0.015, color);
            ring(pose, out, b, 0.66 * scale, 1.94 * scale, 0.038, 46,
                    pale, age * 0.020);
            if (tier >= 2) ring(pose, out, b, 0.46 * scale, 1.48 * scale, 0.028, 38,
                    withAlpha(color, 78), -age * 0.026);
        }

        if (tier >= 1) {
            ring(pose, out, b, (0.92 + tier * 0.10) * scale, 0.09, 0.026, 48,
                    withAlpha(color, 70 + tier * 10), -age * 0.014);
        }
        if (tier >= 3) {
            crystal(pose, out, b.local(0.0, 1.50, 0.18), 0.34, 0.115,
                    withAlpha(color, 130));
        }
    }

    private static void renderEliteAura(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, int style) {
        int color = switch (style) {
            case 0 -> rgba(220, 196, 145, 165);
            case 1 -> rgba(255, 99, 43, 175);
            case 2 -> rgba(155, 128, 214, 145);
            case 3 -> rgba(122, 218, 104, 160);
            default -> rgba(105, 196, 255, 175);
        };
        double pulse = 0.92 + 0.08 * Math.sin(age * 0.16);
        ring(pose, out, b, 0.72 * pulse, 0.06, 0.045, 42, color, age * 0.025);
        if (style == 0) {
            for (int side : new int[]{-1, 1}) {
                spike(pose, out, b.local(side * 0.54, 0.74, 0.0), b.local(side * 0.86, 1.12, 0.25), 0.055, color);
            }
        } else if (style == 1) {
            for (int i = 0; i < 3; i++) {
                double a = age * 0.05 + i * TAU / 3.0;
                sphere(pose, out, b.local(Math.cos(a) * 0.48, 1.05 + i * 0.16, Math.sin(a) * 0.48),
                        0.10, 6, 8, color);
            }
        } else if (style == 2) {
            for (int i = 0; i < 3; i++) {
                slashArc(pose, out, b, age * 0.045 + i * TAU / 3.0,
                        0.78 + i * 0.12, 0.82 + i * 0.18, 0.66, 0.045, color);
            }
        } else if (style == 3) {
            helixRibbon(pose, out, b, age * 0.035, 0.46, 1.55, 18, withAlpha(color, 95));
            ring(pose, out, b, 0.92 * pulse, 0.08, 0.055, 46, color, -age * 0.018);
        } else {
            jaggedBolt(pose, out, b.local(-0.42, 0.20, 0), b.local(0.42, 1.65, 0.12),
                    7, 0.035, color, (long) age / 3L + 17L);
        }
    }

    private static void renderEliteThrow(
            PoseStack.Pose pose, VertexConsumer out, VillageSkillEffectRenderState state,
            double age, double progress) {
        Vec3 origin = new Vec3(state.x, state.y, state.z);
        List<Vec3> points = parsePoints(state.extra, origin);
        if (points.size() < 2) return;
        Vec3 a = points.get(0).subtract(origin);
        Vec3 z = points.get(1).subtract(origin);
        double horizontal = Math.hypot(z.x - a.x, z.z - a.z);
        Vec3 control = a.lerp(z, 0.5).add(0.0, Math.max(2.0, horizontal * 0.16), 0.0);
        Vec3 previous = a;
        for (int i = 1; i <= 14; i++) {
            double t = i / 14.0;
            Vec3 current = bezier(a, control, z, t);
            prism(pose, out, previous, current, 0.035,
                    rgba(255, 83, 34, (int) (90 * (1.0 - progress * 0.5))));
            previous = current;
        }
        Vec3 projectile = bezier(a, control, z, clamp(progress * 1.04, 0.0, 1.0));
        sphere(pose, out, projectile, 0.19, 7, 10, rgba(255, 102, 39, 230));
        sphere(pose, out, projectile, 0.08, 6, 8, rgba(255, 231, 143, 245));
    }

    private static void renderEliteZone(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress,
            String encodedRadius, int style) {
        double radius = 4.0;
        try { radius = Double.parseDouble(encodedRadius); }
        catch (NumberFormatException ignored) {}
        radius = Math.max(1.0, radius);
        int color = style == 0
                ? rgba(255, 86, 38, (int) (205 * (1.0 - progress * 0.75)))
                : rgba(110, 214, 91, (int) ((style == 1 ? 155 : 210) * (1.0 - progress * 0.72)));
        double visibleRadius = style == 1 ? radius : 0.45 + radius * Math.min(1.0, progress * 2.2);
        ring(pose, out, b, visibleRadius, 0.045, style == 1 ? 0.11 : 0.18, 72, color, age * 0.012);
        if (style == 1) {
            ring(pose, out, b, radius * 0.72, 0.05, 0.045, 56, withAlpha(color, 100), -age * 0.018);
            for (int i = 0; i < 8; i++) {
                double a = i * TAU / 8.0 + age * 0.006;
                chevron(pose, out, b, a, radius * 0.88, 0.06, 0.44, withAlpha(color, 120));
            }
        } else if (style == 2) {
            sphere(pose, out, Vec3.ZERO, Math.max(0.3, visibleRadius * 0.22), 8, 12, withAlpha(color, 80));
        }
    }

    private static void renderBossPresence(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age,
            String kind, String aspectName, boolean phaseTwo) {
        int aspect = switch (aspectName == null ? "" : aspectName) {
            case "berserker" -> 0;
            case "bulwark" -> 1;
            case "bloodbound" -> 2;
            case "stormcaller" -> 3;
            case "warleader" -> 4;
            case "wallbreaker" -> 5;
            default -> -1;
        };
        int color = switch (aspect) {
            case 0 -> rgba(255, 72, 52, phaseTwo ? 225 : 165);
            case 1 -> rgba(242, 207, 112, phaseTwo ? 220 : 160);
            case 2 -> rgba(210, 49, 86, phaseTwo ? 225 : 165);
            case 3 -> rgba(96, 191, 255, phaseTwo ? 230 : 170);
            case 4 -> rgba(255, 153, 61, phaseTwo ? 225 : 165);
            case 5 -> rgba(194, 172, 143, phaseTwo ? 225 : 165);
            default -> rgba(199, 102, 255, phaseTwo ? 220 : 155);
        };
        double amp = phaseTwo ? 1.22 : 1.0;
        double pulse = 0.94 + 0.06 * Math.sin(age * (phaseTwo ? 0.22 : 0.13));
        ring(pose, out, b, 1.28 * amp * pulse, 0.08, 0.07, 72, color, age * 0.018);
        ring(pose, out, b, 0.94 * amp, 2.15, 0.055, 56, withAlpha(color, 125), -age * 0.026);

        if (kind.contains("breach_colossus")) {
            for (int side : new int[]{-1, 1}) {
                spike(pose, out, b.local(side * 0.74, 0.18, 0.0),
                        b.local(side * 1.22, 1.44 * amp, 0.18), 0.11 * amp, color);
            }
            prism(pose, out, b.local(-0.72, 0.15, 0.68), b.local(0.72, 0.15, 0.68), 0.10, withAlpha(color, 150));
        } else if (kind.contains("bone_hierophant")) {
            for (int i = 0; i < 4; i++) {
                double a = age * 0.012 + i * TAU / 4.0;
                Vec3 base = b.local(Math.cos(a) * 0.88, 0.25, Math.sin(a) * 0.88);
                crystal(pose, out, base, 1.10 * amp, 0.16, color);
            }
        } else {
            for (int side : new int[]{-1, 1}) {
                prism(pose, out, b.local(side * 0.62, 0.56, -0.22),
                        b.local(-side * 0.18, 2.16 * amp, 0.78), 0.065, color);
            }
            slashArc(pose, out, b, age * 0.018, 1.10 * amp, 1.12, 0.96, 0.055, withAlpha(color, 145));
        }

        if (phaseTwo) {
            jaggedBolt(pose, out, b.local(-0.9, 0.18, 0.0), b.local(0.9, 2.55, 0.12),
                    8, 0.035, withAlpha(color, 190), (long) age / 3L + 551L);
        }
    }

    private static void renderBossDuelMark(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        int color = rgba(255, 76, 83, (int) (205 * (1.0 - progress * 0.55)));
        double radius = 0.82 + 0.08 * Math.sin(age * 0.25);
        ring(pose, out, b, radius, 0.05, 0.07, 48, color, age * 0.04);
        for (int side : new int[]{-1, 1}) {
            prism(pose, out, b.local(side * 0.45, 0.28, -0.22),
                    b.local(-side * 0.22, 1.90, 0.28), 0.045, color);
        }
    }

    private static void renderBossZone(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress,
            String encodedRadius, int style) {
        double radius = 4.0;
        try { radius = Double.parseDouble(encodedRadius); }
        catch (NumberFormatException ignored) {}
        radius = Math.max(0.8, radius);
        int base = switch (style) {
            case 1, 2 -> rgba(255, 157, 67, 220);
            case 3, 4 -> rgba(128, 220, 255, 205);
            case 5 -> rgba(255, 72, 82, 225);
            case 6, 7 -> rgba(207, 48, 86, 220);
            case 8 -> rgba(103, 198, 255, 220);
            default -> rgba(255, 94, 67, 220);
        };
        boolean warning = style == 1 || style == 3 || style == 6 || style == 8;
        double visible = warning ? radius : 0.35 + radius * Math.min(1.0, progress * 2.2);
        int alpha = warning
                ? (int) (115 + 65 * (0.5 + 0.5 * Math.sin(age * 0.28)))
                : (int) (220 * (1.0 - progress * 0.74));
        int color = withAlpha(base, alpha);
        ring(pose, out, b, visible, 0.055, warning ? 0.11 : 0.20, 80, color, age * 0.014);
        ring(pose, out, b, visible * 0.72, 0.06, 0.045, 60, withAlpha(color, Math.max(30, alpha - 55)), -age * 0.022);
        if (warning) {
            for (int i = 0; i < 8; i++) {
                double a = i * TAU / 8.0 + age * 0.004;
                chevron(pose, out, b, a, radius * 0.90, 0.06, 0.46, withAlpha(color, Math.max(45, alpha - 20)));
            }
        } else {
            sphere(pose, out, Vec3.ZERO, Math.max(0.28, visible * 0.20), 8, 12, withAlpha(color, 70));
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

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (clampInt(alpha) << 24);
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
