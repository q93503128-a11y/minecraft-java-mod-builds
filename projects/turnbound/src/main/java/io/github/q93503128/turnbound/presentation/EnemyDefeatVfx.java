package io.github.q93503128.turnbound.presentation;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Small identity accents layered over the neutral down smoke for v0.4 enemies and elites. */
public final class EnemyDefeatVfx {
    private EnemyDefeatVfx() { }

    public static void play(ServerLevel level, String visualId, Vec3 center) {
        if (level == null || visualId == null || center == null) return;
        switch (visualId) {
            case "E001" -> burst(level, ParticleTypes.ASH, center, 8, .38, .35, .38, .015);
            case "E002" -> burst(level, ParticleTypes.CRIT, center.add(0, .95, 0), 7, .3, .28, .3, .025);
            case "E003" -> {
                ring(level, ParticleTypes.SMALL_FLAME, center.add(0, .15, 0), .72, 14);
                burst(level, ParticleTypes.SMOKE, center.add(0, .8, 0), 16, .6, .55, .6, .03);
            }
            case "E004" -> burst(level, ParticleTypes.CRIT, center.add(0, .8, 0), 7, .38, .32, .38, .02);
            case "E005" -> {
                ring(level, ParticleTypes.END_ROD, center.add(0, .16, 0), .52, 11);
                burst(level, ParticleTypes.ENCHANT, center.add(0, .8, 0), 8, .4, .4, .4, .015);
            }
            case "E006" -> burst(level, ParticleTypes.CLOUD, center.add(0, .45, 0), 14, .72, .25, .72, .035);
            case "E007" -> {
                ring(level, ParticleTypes.ENCHANT, center.add(0, .28, 0), .62, 13);
                burst(level, ParticleTypes.SOUL, center.add(0, .9, 0), 9, .45, .55, .45, .02);
            }
            case "E008" -> burst(level, ParticleTypes.ENCHANT, center.add(0, .65, 0), 11, .6, .45, .6, .02);
            case "E009" -> burst(level, ParticleTypes.ELECTRIC_SPARK, center.add(0, .9, 0), 12, .5, .5, .5, .08);
            case "E010" -> burst(level, ParticleTypes.SOUL, center.add(0, .45, 0), 10, .58, .28, .58, .018);
            case "E011" -> {
                ring(level, ParticleTypes.ELECTRIC_SPARK, center.add(0, .22, 0), .58, 12);
                burst(level, ParticleTypes.SMOKE, center.add(0, .7, 0), 8, .4, .35, .4, .02);
            }
            case "E012" -> burst(level, ParticleTypes.ASH, center.add(0, .5, 0), 14, .7, .32, .7, .025);
            case "E013" -> {
                ring(level, ParticleTypes.SMALL_FLAME, center.add(0, .16, 0), .62, 14);
                burst(level, ParticleTypes.ASH, center.add(0, .8, 0), 13, .55, .5, .55, .025);
            }
            case "E014" -> {
                ring(level, ParticleTypes.ASH, center.add(0, .16, 0), .82, 16);
                burst(level, ParticleTypes.SMOKE, center.add(0, .9, 0), 15, .68, .5, .68, .03);
            }
            case "EL01" -> elite(level, center, ParticleTypes.CRIT, ParticleTypes.ASH);
            case "EL02" -> elite(level, center, ParticleTypes.ENCHANT, ParticleTypes.CLOUD);
            case "EL03" -> elite(level, center, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.SMOKE);
            case "EL04" -> elite(level, center, ParticleTypes.SMALL_FLAME, ParticleTypes.ASH);
            default -> { }
        }
    }

    private static void elite(ServerLevel level, Vec3 center, ParticleOptions primary, ParticleOptions secondary) {
        ring(level, primary, center.add(0, .18, 0), 1.0, 22);
        burst(level, primary, center.add(0, 1.0, 0), 16, .75, .65, .75, .06);
        burst(level, secondary, center.add(0, .65, 0), 14, .8, .42, .8, .025);
    }

    private static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2.0 * i / count;
            PersonalPresentationIsolation.particles(level, particle, center.x + Math.cos(a) * radius, center.y,
                    center.z + Math.sin(a) * radius, 1, .01, .01, .01, 0);
        }
    }

    private static void burst(ServerLevel level, ParticleOptions particle, Vec3 center, int count,
                              double dx, double dy, double dz, double speed) {
        PersonalPresentationIsolation.particles(level, particle, center.x, center.y, center.z, count, dx, dy, dz, speed);
    }
}
