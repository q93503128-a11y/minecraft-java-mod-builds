package io.github.q93503128.turnbound.presentation;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Authored presentation grammar for the eight v0.4 core heroes.
 *
 * <p>This class is intentionally presentation-only. Combat data owns targeting, damage,
 * healing, gauge changes and status application; this layer only mirrors the canonical
 * skill identity with readable VFX.</p>
 */
final class CoreHeroVfx {
    private CoreHeroVfx() { }

    static void skill(
            ServerLevel level,
            String heroId,
            String skillId,
            Vec3 source,
            Vec3 target,
            boolean damaging
    ) {
        switch (HeroSkillVfxStyle.resolve(heroId, skillId)) {
            case P01_CHASE_SLASH -> {
                line(level, ParticleTypes.CRIT, source.add(0, 1.15, 0), target.add(0, 1.0, 0), 10);
                slashArc(level, ParticleTypes.CRIT, target.add(0, 0.92, 0), 0.72, 11);
                slashArc(level, ParticleTypes.END_ROD, target.add(0, 1.12, 0), 0.86, 11);
            }
            case P01_BREAKER_STRIKE -> {
                line(level, ParticleTypes.END_ROD, source.add(0, 1.15, 0), target.add(0, 1.0, 0), 14);
                slashArc(level, ParticleTypes.END_ROD, target.add(0, 1.0, 0), 1.18, 20);
                burst(level, ParticleTypes.CRIT, target.add(0, 1.0, 0), 18, 0.58, 0.58, 0.58, 0.17);
            }
            case P01_DUEL_LOCK -> {
                // v1: a real 110% cut that also establishes/advances the duel target and grants self Gauge.
                line(level, ParticleTypes.END_ROD, source.add(0, 1.10, 0), target.add(0, 1.05, 0), 12);
                slashArc(level, ParticleTypes.CRIT, target.add(0, 1.00, 0), 0.72, 11);
                burst(level, ParticleTypes.CRIT, target.add(0, 0.98, 0), 7, 0.28, 0.28, 0.28, 0.05);
                ring(level, ParticleTypes.CRIT, target.add(0, 0.24, 0), 0.82, 18);
                ring(level, ParticleTypes.END_ROD, target.add(0, 1.02, 0), 0.46, 12);
                ring(level, ParticleTypes.END_ROD, source.add(0, 0.28, 0), 0.58, 12);
            }

            case P02_ACCELERATE -> {
                // v1: ally Gauge +120, with an extra +40 for an ally slower than Lumea. Lumea stays anchored.
                line(level, ParticleTypes.END_ROD, source.add(0, 1.18, 0), target.add(0, 1.05, 0), 8);
                ring(level, ParticleTypes.ELECTRIC_SPARK, target.add(0, 0.34, 0), 0.66, 16);
                ring(level, ParticleTypes.END_ROD, target.add(0, 1.10, 0), 0.42, 12);
            }
            case P02_TIME_LEAP -> {
                ring(level, ParticleTypes.END_ROD, source.add(0, 0.92, 0), 0.72, 16);
                line(level, ParticleTypes.END_ROD, source.add(0, 1.20, 0), target.add(0, 1.15, 0), 9);
                ring(level, ParticleTypes.PORTAL, target.add(0, 0.30, 0), 0.92, 22);
                ring(level, ParticleTypes.ELECTRIC_SPARK, target.add(0, 1.00, 0), 0.68, 18);
                burst(level, ParticleTypes.END_ROD, target.add(0, 1.15, 0), 14, 0.45, 0.60, 0.45, 0.06);
            }
            case P02_DELAY_FIELD -> {
                // v1: single-target 85% hit plus Gauge -180; distortion remains the primary read.
                line(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, 1.16, 0), target.add(0, 1.02, 0), 10);
                ring(level, ParticleTypes.PORTAL, target.add(0, 0.24, 0), 1.08, 24);
                ring(level, ParticleTypes.END_ROD, target.add(0, 0.92, 0), 0.72, 18);
                burst(level, ParticleTypes.ELECTRIC_SPARK, target.add(0, 0.95, 0), 10, 0.44, 0.45, 0.44, 0.035);
            }

            case P03_GUARD_STANCE -> {
                // v1 Shield Bash: visible 80% contact hit, then Guard +15 and a small self Barrier.
                ring(level, ParticleTypes.CLOUD, source.add(0, 0.18, 0), 0.82, 18);
                line(level, ParticleTypes.CRIT, source.add(0, 0.96, 0), target.add(0, 0.92, 0), 8);
                burst(level, ParticleTypes.CRIT, target.add(0, 0.86, 0), 9, 0.38, 0.28, 0.38, 0.07);
                ring(level, ParticleTypes.END_ROD, source.add(0, 0.86, 0), 0.64, 16);
                burst(level, ParticleTypes.CLOUD, source.add(0, 0.72, 0), 10, 0.40, 0.45, 0.40, 0.03);
            }
            case P03_GUARD_TRANSFER -> {
                line(level, ParticleTypes.END_ROD, source.add(0, 1.0, 0), target.add(0, 1.0, 0), 8);
                ring(level, ParticleTypes.END_ROD, target.add(0, 0.45, 0), 0.92, 22);
                ring(level, ParticleTypes.CLOUD, target.add(0, 1.0, 0), 0.68, 16);
                burst(level, ParticleTypes.END_ROD, target.add(0, 1.0, 0), 10, 0.45, 0.55, 0.45, 0.03);
            }
            case P03_SHIELD_PRESSURE -> {
                // Canon: 0.90x hit plus Gauge -120.
                ring(level, ParticleTypes.CLOUD, source.add(0, 0.18, 0), 0.72, 14);
                line(level, ParticleTypes.CRIT, source.add(0, 0.95, 0), target.add(0, 0.90, 0), 10);
                slashArc(level, ParticleTypes.END_ROD, target.add(0, 0.90, 0), 0.82, 13);
                ring(level, ParticleTypes.ELECTRIC_SPARK, target.add(0, 0.28, 0), 0.58, 12);
            }

            case P04_HEAL -> {
                // Canon: no hearts. Thin ivory/green-ish light gathers inward toward the wound.
                line(level, ParticleTypes.END_ROD, source.add(0, 1.15, 0), target.add(0, 1.05, 0), 8);
                ring(level, ParticleTypes.END_ROD, target.add(0, 0.30, 0), 0.56, 13);
                ring(level, ParticleTypes.ENCHANT, target.add(0, 0.82, 0), 0.34, 10);
                inwardStrands(level, target.add(0, 1.02, 0), 0.54, 8);
            }
            case P04_RETURNED_BREATH -> {
                line(level, ParticleTypes.END_ROD, source.add(0, 1.20, 0), target.add(0, 1.05, 0), 10);
                ring(level, ParticleTypes.END_ROD, target.add(0, 0.28, 0), 0.82, 18);
                ring(level, ParticleTypes.ENCHANT, target.add(0, 1.12, 0), 0.94, 22);
                ring(level, ParticleTypes.END_ROD, target.add(0, 1.58, 0), 0.50, 13);
                inwardStrands(level, target.add(0, 1.05, 0), 0.86, 12);
                burst(level, ParticleTypes.END_ROD, target.add(0, 1.0, 0), 18, 0.38, 0.62, 0.38, 0.025);
            }
            case P04_RESTING_LIGHT -> {
                // Canon: party-wide heal, same thin-light grammar for every resolved ally; never heart particles.
                ring(level, ParticleTypes.END_ROD, target.add(0, 0.22, 0), 0.76, 18);
                ring(level, ParticleTypes.ENCHANT, target.add(0, 0.92, 0), 0.58, 14);
                inwardStrands(level, target.add(0, 1.02, 0), 0.66, 10);
                burst(level, ParticleTypes.END_ROD, target.add(0, 1.08, 0), 8, 0.28, 0.48, 0.28, 0.018);
            }

            case P05_SUPPRESSIVE_SHOT -> {
                line(level, ParticleTypes.CRIT, source.add(0, 1.35, 0), target.add(0, 1.05, 0), 18);
                ring(level, ParticleTypes.END_ROD, target.add(0, 1.05, 0), 0.30, 9);
                burst(level, ParticleTypes.CRIT, target.add(0, 1.05, 0), 6, 0.24, 0.24, 0.24, 0.05);
            }
            case P05_PIERCING_SHOT -> {
                line(level, ParticleTypes.END_ROD, source.add(0, 1.38, 0), target.add(0, 1.05, 0), 28);
                line(level, ParticleTypes.CRIT, source.add(0.08, 1.32, 0.08), target.add(0, 1.05, 0), 20);
                burst(level, ParticleTypes.CRIT, target.add(0, 1.05, 0), 18, 0.50, 0.42, 0.50, 0.14);
                burst(level, ParticleTypes.END_ROD, target.add(0, 1.05, 0), 10, 0.32, 0.32, 0.32, 0.08);
            }
            case P05_HUNT_SIGNAL -> {
                // v1: 100% direct shot, switch Sightline immediately, and gain Shot +1.
                line(level, ParticleTypes.END_ROD, source.add(0, 1.34, 0), target.add(0, 1.05, 0), 16);
                burst(level, ParticleTypes.CRIT, target.add(0, 1.05, 0), 7, 0.28, 0.24, 0.28, 0.06);
                ring(level, ParticleTypes.ENCHANT, target.add(0, 0.26, 0), 0.92, 22);
                ring(level, ParticleTypes.END_ROD, target.add(0, 1.05, 0), 0.54, 14);
                ring(level, ParticleTypes.CRIT, target.add(0, 1.05, 0), 0.28, 8);
            }

            case P06_ECHO -> {
                ring(level, ParticleTypes.ENCHANT, source.add(0, 0.95, 0), 0.62, 14);
                line(level, ParticleTypes.SOUL, source.add(0, 1.15, 0), target.add(0, 1.05, 0), 10);
                slashArc(level, ParticleTypes.SOUL, target.add(0, 1.0, 0), 0.78, 12);
            }
            case P06_CONDOLENCE -> {
                ring(level, ParticleTypes.SOUL, source.add(0, 0.95, 0), 0.88, 20);
                line(level, ParticleTypes.ENCHANT, source.add(0, 1.20, 0), target.add(0, 1.08, 0), 14);
                slashArc(level, ParticleTypes.END_ROD, target.add(0, 1.02, 0), 1.02, 17);
                burst(level, ParticleTypes.SOUL, target.add(0, 1.0, 0), 14, 0.55, 0.58, 0.55, 0.05);
            }
            case P06_FUNERAL_ORDER -> {
                ring(level, ParticleTypes.SOUL, target.add(0, 0.25, 0), 1.18, 26);
                ring(level, ParticleTypes.ENCHANT, target.add(0, 1.05, 0), 0.78, 20);
                line(level, ParticleTypes.SOUL, source.add(0, 1.12, 0), target.add(0, 1.05, 0), 16);
                slashArc(level, ParticleTypes.END_ROD, target.add(0, 1.02, 0), 1.12, 18);
                burst(level, ParticleTypes.END_ROD, target.add(0, 1.0, 0), 18, 0.62, 0.72, 0.62, 0.07);
            }

            case P07_COMMAND -> {
                // Command may resolve as Marion's light hit or Toto's immediate reaction.
                ring(level, ParticleTypes.ENCHANT, source.add(0, 0.72, 0), 0.58, 13);
                line(level, damaging ? ParticleTypes.CRIT : ParticleTypes.END_ROD,
                        source.add(0, 1.0, 0), target.add(0, 0.90, 0), 9);
                burst(level, ParticleTypes.CRIT, target.add(0, 0.85, 0), 7, 0.30, 0.25, 0.30, 0.06);
            }
            case P07_SUMMON_TOTO -> {
                // The legacy skill ID now means Guard Command: Toto already exists and protects the chosen ally.
                line(level, ParticleTypes.END_ROD, source.add(0, 0.94, 0), target.add(0, 0.94, 0), 9);
                ring(level, ParticleTypes.ENCHANT, target.add(0, 0.28, 0), 0.82, 18);
                ring(level, ParticleTypes.END_ROD, target.add(0, 0.92, 0), 0.58, 14);
                burst(level, ParticleTypes.CLOUD, target.add(0, 0.82, 0), 8, 0.42, 0.36, 0.42, 0.025);
            }
            case P07_JOINT_ATTACK -> {
                ring(level, ParticleTypes.ENCHANT, source.add(0, 0.72, 0), 0.72, 16);
                line(level, ParticleTypes.END_ROD, source.add(-0.16, 1.05, 0), target.add(0, 0.95, 0), 14);
                line(level, ParticleTypes.CRIT, source.add(0.16, 0.72, 0), target.add(0, 0.82, 0), 12);
                burst(level, ParticleTypes.END_ROD, target.add(0, 0.90, 0), 12, 0.42, 0.36, 0.42, 0.08);
                burst(level, ParticleTypes.CRIT, target.add(0, 0.82, 0), 12, 0.48, 0.32, 0.48, 0.10);
            }

            case P08_FRENZY -> {
                burst(level, ParticleTypes.FLAME, source.add(0, 1.0, 0), 6, 0.32, 0.45, 0.32, 0.02);
                line(level, ParticleTypes.CRIT, source.add(0, 1.22, 0), target.add(0, 1.0, 0), 11);
                slashArc(level, ParticleTypes.FLAME, target.add(0, 1.0, 0), 0.86, 13);
            }
            case P08_BLOOD_CHARGE -> {
                ring(level, ParticleTypes.FLAME, source.add(0, 0.25, 0), 0.72, 16);
                burst(level, ParticleTypes.FLAME, source.add(0, 1.0, 0), 11, 0.50, 0.60, 0.50, 0.04);
                line(level, ParticleTypes.FLAME, source.add(0, 1.15, 0), target.add(0, 0.95, 0), 17);
                slashArc(level, ParticleTypes.CRIT, target.add(0, 0.95, 0), 1.12, 18);
                burst(level, ParticleTypes.FLAME, target.add(0, 0.85, 0), 20, 0.72, 0.48, 0.72, 0.13);
            }
            case P08_BATTLE_MANIA -> {
                ring(level, ParticleTypes.FLAME, source.add(0, 0.18, 0), 1.08, 24);
                ring(level, ParticleTypes.CRIT, source.add(0, 0.88, 0), 0.78, 18);
                burst(level, ParticleTypes.FLAME, source.add(0, 1.0, 0), 22, 0.75, 0.85, 0.75, 0.08);
                burst(level, ParticleTypes.ASH, source.add(0, 1.20, 0), 14, 0.70, 0.65, 0.70, 0.025);
            }

            case GENERIC -> generic(level, source, target, damaging);
        }
    }

    /** Readable payoff beats for signature mechanics; paired with authored additive model animation. */
    static void payoff(ServerLevel level, HeroSignatureBeat.Kind kind, Vec3 source, Vec3 target) {
        switch (kind) {
            case KYREN_FOLLOWUP -> {
                line(level, ParticleTypes.END_ROD, source.add(0, 1.16, 0), target.add(0, 1.02, 0), 12);
                slashArc(level, ParticleTypes.CRIT, target.add(0, 1.02, 0), 0.76, 12);
                slashArc(level, ParticleTypes.END_ROD, target.add(0, 1.16, 0), 0.96, 15);
            }
            case BRAM_REDIRECT_COUNTER -> {
                ring(level, ParticleTypes.CLOUD, source.add(0, 0.22, 0), 0.88, 18);
                line(level, ParticleTypes.END_ROD, source.add(0, 1.00, 0), target.add(0, 0.94, 0), 9);
                burst(level, ParticleTypes.CRIT, target.add(0, 0.90, 0), 14, 0.46, 0.34, 0.46, 0.09);
            }
            case ELYSIA_SANCTUARY -> {
                ring(level, ParticleTypes.ENCHANT, source.add(0, 0.84, 0), 0.62, 14);
                line(level, ParticleTypes.END_ROD, source.add(0, 1.20, 0), target.add(0, 1.04, 0), 10);
                inwardStrands(level, target.add(0, 1.02, 0), 0.72, 10);
                ring(level, ParticleTypes.END_ROD, target.add(0, 0.30, 0), 0.74, 16);
            }
            case LYNETTE_CROSS_SHOT -> {
                line(level, ParticleTypes.END_ROD, source.add(-0.10, 1.34, 0), target.add(0, 1.04, 0), 20);
                line(level, ParticleTypes.CRIT, source.add(0.10, 1.28, 0), target.add(0, 0.96, 0), 16);
                burst(level, ParticleTypes.CRIT, target.add(0, 1.00, 0), 12, 0.38, 0.30, 0.38, 0.08);
            }
            case MORWEN_RECORD_SPEND -> {
                ring(level, ParticleTypes.SOUL, source.add(0, 0.88, 0), 0.72, 16);
                ring(level, ParticleTypes.ENCHANT, source.add(0, 1.20, 0), 0.46, 12);
                line(level, ParticleTypes.SOUL, source.add(0, 1.18, 0), target.add(0, 1.00, 0), 12);
            }
            case MORWEN_LAST_PAGE -> {
                ring(level, ParticleTypes.SOUL, source.add(0, 0.18, 0), 1.05, 24);
                ring(level, ParticleTypes.ENCHANT, source.add(0, 0.86, 0), 0.72, 18);
                burst(level, ParticleTypes.END_ROD, source.add(0, 1.12, 0), 20, 0.55, 0.88, 0.55, 0.045);
            }
            case MARION_PARTNER_STRIKE -> {
                line(level, ParticleTypes.ENCHANT, source.add(0, 0.72, 0), target.add(0, 0.90, 0), 10);
                burst(level, ParticleTypes.CRIT, target.add(0, 0.86, 0), 9, 0.34, 0.26, 0.34, 0.07);
            }
            case MARION_JOINT_STRIKE -> {
                ring(level, ParticleTypes.ENCHANT, source.add(0, 0.52, 0), 0.70, 15);
                line(level, ParticleTypes.END_ROD, source.add(0, 0.78, 0), target.add(0, 0.96, 0), 15);
                slashArc(level, ParticleTypes.CRIT, target.add(0, 0.92, 0), 0.94, 16);
                burst(level, ParticleTypes.END_ROD, target.add(0, 1.02, 0), 10, 0.42, 0.42, 0.42, 0.07);
            }
            case RAZE_HIGH_FURY -> {
                ring(level, ParticleTypes.FLAME, source.add(0, 0.20, 0), 0.86, 18);
                line(level, ParticleTypes.CRIT, source.add(0, 1.20, 0), target.add(0, 0.96, 0), 11);
                slashArc(level, ParticleTypes.FLAME, target.add(0, 0.94, 0), 1.08, 18);
                burst(level, ParticleTypes.CRIT, target.add(0, 0.88, 0), 14, 0.58, 0.38, 0.58, 0.10);
            }
            case RAZE_OVERHEAT -> {
                ring(level, ParticleTypes.FLAME, source.add(0, 0.18, 0), 1.18, 28);
                burst(level, ParticleTypes.ASH, source.add(0, 1.05, 0), 18, 0.76, 0.78, 0.76, 0.035);
                burst(level, ParticleTypes.FLAME, source.add(0, 1.00, 0), 12, 0.48, 0.62, 0.48, 0.06);
            }
        }
    }

    /** Short state-change accent for signature resources; persistent readability lives in the world-space HUD. */
    static void resource(ServerLevel level, String heroId, Vec3 center, int value, int max) {
        double ratio = Math.max(0.0, Math.min(1.0, value / (double)Math.max(1, max)));
        switch (heroId) {
            case "P01" -> {
                ring(level, ParticleTypes.CRIT, center.add(0, 0.34, 0), 0.46 + ratio * 0.18, 8 + (int)Math.round(ratio * 6));
                if (ratio >= 0.99) ring(level, ParticleTypes.END_ROD, center.add(0, 1.05, 0), 0.42, 10);
            }
            case "P03" -> {
                ring(level, ParticleTypes.CLOUD, center.add(0, 0.22, 0), 0.54 + ratio * 0.24, 10 + (int)Math.round(ratio * 8));
                if (ratio >= 0.50) burst(level, ParticleTypes.END_ROD, center.add(0, 0.92, 0), 5, 0.34, 0.42, 0.34, 0.02);
            }
            case "P05" -> {
                int shots = Math.max(0, Math.min(2, value));
                burst(level, ParticleTypes.CRIT, center.add(0, 1.34, 0), 3 + shots * 3, 0.20, 0.16, 0.20, 0.03);
                if (shots >= 2) ring(level, ParticleTypes.END_ROD, center.add(0, 1.16, 0), 0.34, 9);
            }
            case "P06" -> {
                burst(level, ParticleTypes.SOUL, center.add(0, 1.18, 0), 3 + (int)Math.round(ratio * 7), 0.30, 0.42, 0.30, 0.018);
                if (ratio >= 0.60) ring(level, ParticleTypes.ENCHANT, center.add(0, 0.42, 0), 0.52, 10);
            }
            case "P07" -> {
                ring(level, ParticleTypes.ENCHANT, center.add(0, 0.30, 0), 0.50 + ratio * 0.20, 8 + (int)Math.round(ratio * 8));
                if (ratio >= 0.50) burst(level, ParticleTypes.END_ROD, center.add(0, 0.92, 0), 6, 0.34, 0.38, 0.34, 0.025);
            }
            case "P08" -> {
                burst(level, ratio >= 0.80 ? ParticleTypes.FLAME : ParticleTypes.ASH,
                        center.add(0, 0.90, 0), 4 + (int)Math.round(ratio * 10), 0.38, 0.50, 0.38, 0.025 + ratio * 0.035);
                if (ratio >= 0.80) ring(level, ParticleTypes.CRIT, center.add(0, 0.24, 0), 0.72, 14);
            }
            default -> { }
        }
    }

    private static void generic(ServerLevel level, Vec3 source, Vec3 target, boolean damaging) {
        ParticleOptions type = damaging ? ParticleTypes.CRIT : ParticleTypes.ENCHANT;
        line(level, type, source.add(0, 1.0, 0), target.add(0, 1.0, 0), damaging ? 8 : 5);
    }

    /** Radial thin strands converge toward Elysia's heal target; avoids icon-like heart feedback. */
    private static void inwardStrands(ServerLevel level, Vec3 center, double radius, int spokes) {
        for (int i = 0; i < spokes; i++) {
            double angle = Math.PI * 2 * i / spokes;
            Vec3 outer = center.add(Math.cos(angle) * radius, (i % 2 == 0 ? 0.34 : -0.22), Math.sin(angle) * radius);
            line(level, i % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.ENCHANT, outer, center, 4);
        }
    }

    private static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, int steps) {
        if (steps <= 0) return;
        Vec3 delta = to.subtract(from);
        for (int i = 0; i <= steps; i++) {
            double progress = i / (double) steps;
            Vec3 point = from.add(delta.scale(progress));
            PersonalPresentationIsolation.particles(level, particle, point.x, point.y, point.z,
                    1, 0.02, 0.02, 0.02, 0);
        }
    }

    private static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            PersonalPresentationIsolation.particles(level, particle, x, center.y, z,
                    1, 0.01, 0.01, 0.01, 0);
        }
    }

    private static void slashArc(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double progress = i / (double) Math.max(1, count - 1);
            double angle = -1.2 + progress * 2.4;
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + (progress - 0.5) * 1.2;
            double z = center.z + Math.sin(angle) * radius;
            PersonalPresentationIsolation.particles(level, particle, x, y, z,
                    1, 0.01, 0.01, 0.01, 0);
        }
    }

    private static void burst(
            ServerLevel level,
            ParticleOptions particle,
            Vec3 center,
            int count,
            double dx,
            double dy,
            double dz,
            double speed
    ) {
        PersonalPresentationIsolation.particles(level, particle, center.x, center.y, center.z,
                count, dx, dy, dz, speed);
    }
}
