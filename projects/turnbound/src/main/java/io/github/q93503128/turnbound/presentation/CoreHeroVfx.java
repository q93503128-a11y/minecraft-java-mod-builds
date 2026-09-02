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
                // Canon: no damage. Mark one enemy as the focus target and feed Kyren's gauge.
                line(level, ParticleTypes.END_ROD, source.add(0, 1.10, 0), target.add(0, 1.05, 0), 10);
                ring(level, ParticleTypes.CRIT, target.add(0, 0.24, 0), 0.82, 18);
                ring(level, ParticleTypes.END_ROD, target.add(0, 1.02, 0), 0.46, 12);
                ring(level, ParticleTypes.END_ROD, source.add(0, 0.28, 0), 0.58, 12);
            }

            case P02_ACCELERATE -> {
                // Canon: ally Gauge +180. Lumea stays anchored; time flow moves around the target.
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
                // Canon: all enemies lose Gauge. Keep it a field/clock distortion, not an attack slash.
                ring(level, ParticleTypes.PORTAL, target.add(0, 0.24, 0), 1.08, 24);
                ring(level, ParticleTypes.END_ROD, target.add(0, 0.92, 0), 0.72, 18);
                burst(level, ParticleTypes.ELECTRIC_SPARK, target.add(0, 0.95, 0), 8, 0.44, 0.45, 0.44, 0.025);
            }

            case P03_GUARD_STANCE -> {
                // Canon: self Barrier. No fake melee hit.
                ring(level, ParticleTypes.CLOUD, source.add(0, 0.18, 0), 0.82, 18);
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
                line(level, ParticleTypes.END_ROD, source.add(0, 1.15, 0), target.add(0, 1.05, 0), 8);
                ring(level, ParticleTypes.END_ROD, target.add(0, 0.30, 0), 0.56, 13);
                burst(level, ParticleTypes.HEART, target.add(0, 1.02, 0), 5, 0.28, 0.42, 0.28, 0.02);
            }
            case P04_RETURNED_BREATH -> {
                line(level, ParticleTypes.END_ROD, source.add(0, 1.20, 0), target.add(0, 1.05, 0), 10);
                ring(level, ParticleTypes.END_ROD, target.add(0, 0.28, 0), 0.82, 18);
                ring(level, ParticleTypes.ENCHANT, target.add(0, 1.12, 0), 0.94, 22);
                ring(level, ParticleTypes.END_ROD, target.add(0, 1.58, 0), 0.50, 13);
                burst(level, ParticleTypes.END_ROD, target.add(0, 1.0, 0), 18, 0.38, 0.62, 0.38, 0.025);
            }
            case P04_RESTING_LIGHT -> {
                // Canon: party-wide heal. Each resolved ally receives the same warm grammar.
                ring(level, ParticleTypes.END_ROD, target.add(0, 0.22, 0), 0.76, 18);
                ring(level, ParticleTypes.ENCHANT, target.add(0, 0.92, 0), 0.58, 14);
                burst(level, ParticleTypes.HEART, target.add(0, 1.02, 0), 6, 0.34, 0.48, 0.34, 0.02);
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
                // Canon: no direct damage; immediately max Exposure and mark the hunt target.
                line(level, ParticleTypes.END_ROD, source.add(0, 1.34, 0), target.add(0, 1.05, 0), 14);
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
                ring(level, ParticleTypes.ENCHANT, source.add(0, 0.75, 0), 0.82, 18);
                ring(level, ParticleTypes.CRIT, source.add(0, 0.12, 0), 1.15, 28);
                ring(level, ParticleTypes.END_ROD, source.add(0, 0.58, 0), 0.68, 16);
                burst(level, ParticleTypes.END_ROD, source.add(0, 0.70, 0), 16, 0.70, 0.50, 0.70, 0.08);
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

    private static void generic(ServerLevel level, Vec3 source, Vec3 target, boolean damaging) {
        ParticleOptions type = damaging ? ParticleTypes.CRIT : ParticleTypes.ENCHANT;
        line(level, type, source.add(0, 1.0, 0), target.add(0, 1.0, 0), damaging ? 8 : 5);
    }

    private static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, int steps) {
        if (steps <= 0) return;
        Vec3 delta = to.subtract(from);
        for (int i = 0; i <= steps; i++) {
            double progress = i / (double) steps;
            Vec3 point = from.add(delta.scale(progress));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    private static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, center.y, z, 1, 0.01, 0.01, 0.01, 0);
        }
    }

    private static void slashArc(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double progress = i / (double) Math.max(1, count - 1);
            double angle = -1.2 + progress * 2.4;
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + (progress - 0.5) * 1.2;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, y, z, 1, 0.01, 0.01, 0.01, 0);
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
        level.sendParticles(particle, center.x, center.y, center.z, count, dx, dy, dz, speed);
    }
}
