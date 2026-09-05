package io.github.q93503128.turnbound.presentation;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Presentation-only visual grammar for the five v0.4 story bosses.
 * Combat rules stay authoritative in the combat package; this class only makes the
 * already-resolved boss actions visually readable and boss-specific.
 */
public final class BossBattleVfx {
    private BossBattleVfx() { }

    public static boolean handles(String visualId) {
        return switch (visualId) {
            case "B01", "B02", "B03", "B04", "B05" -> true;
            default -> false;
        };
    }

    public static void skill(ServerLevel level, String visualId, String skillId, Vec3 source, Vec3 target) {
        switch (visualId) {
            case "B01" -> graul(level, skillId, source, target);
            case "B02" -> verna(level, skillId, source, target);
            case "B03" -> oro7(level, skillId, source, target);
            case "B04" -> kolvak(level, skillId, source, target);
            case "B05" -> serak(level, skillId, source, target);
            default -> { }
        }
    }

    /** ORO-7's barrier break is a real punish window, so it gets a distinct armor-shear beat. */
    public static void oroBarrierBreak(ServerLevel level, Vec3 center) {
        burst(level, ParticleTypes.CRIT, center.add(0, 1.45, 0), 22, .9, .55, .9, .16);
        burst(level, ParticleTypes.ELECTRIC_SPARK, center.add(0, 1.35, 0), 18, .75, .45, .75, .09);
        ring(level, ParticleTypes.CLOUD, center.add(0, .25, 0), 1.35, 24);
        radial(level, ParticleTypes.CRIT, center.add(0, 1.25, 0), .25, 1.75, 8, 5);
    }

    /** Extra identity accent layered onto the shared phase transition effect. */
    public static void phaseAccent(ServerLevel level, String visualId, Vec3 center, int phase) {
        if (phase < 2) return;
        switch (visualId) {
            case "B01" -> {
                ring(level, ParticleTypes.ASH, center.add(0, .25, 0), phase >= 3 ? 2.0 : 1.45, 28);
                burst(level, ParticleTypes.END_ROD, center.add(0, 2.0, 0), phase >= 3 ? 18 : 10, .6, .8, .6, .035);
            }
            case "B02" -> {
                ring(level, ParticleTypes.ENCHANT, center.add(0, 1.25, 0), phase >= 3 ? 1.7 : 1.2, phase >= 3 ? 32 : 22);
                if (phase >= 3) burst(level, ParticleTypes.END_ROD, center.add(0, 1.65, 0), 20, .75, .75, .75, .04);
            }
            case "B03" -> {
                ring(level, ParticleTypes.ELECTRIC_SPARK, center.add(0, 1.55, 0), phase >= 3 ? 1.6 : 1.15, phase >= 3 ? 32 : 22);
                burst(level, ParticleTypes.CLOUD, center.add(0, 2.0, 0), phase >= 3 ? 18 : 10, .7, .7, .7, .07);
            }
            case "B04" -> {
                // Core exposure is localized to the chest; never turn the whole body into a lava silhouette.
                burst(level, ParticleTypes.FLAME, center.add(0, 1.95, 0), phase >= 3 ? 24 : 10, .35, .35, .35, .035);
                ring(level, ParticleTypes.ASH, center.add(0, .18, 0), phase >= 3 ? 1.7 : 1.15, phase >= 3 ? 30 : 20);
            }
            case "B05" -> {
                ring(level, ParticleTypes.PORTAL, center.add(0, 1.15, 0), phase >= 3 ? 1.6 : 1.15, phase >= 3 ? 34 : 22);
                burst(level, ParticleTypes.SOUL, center.add(.45, 1.55, 0), phase >= 3 ? 18 : 10, .35, .7, .35, .03);
            }
            default -> { }
        }
    }

    private static void graul(ServerLevel level, String skillId, Vec3 source, Vec3 target) {
        switch (skillId) {
            case "b01_basic" -> {
                line(level, ParticleTypes.CRIT, source.add(0, 1.15, 0), target.add(0, 1.0, 0), 10);
                burst(level, ParticleTypes.CRIT, target.add(0, 1.0, 0), 12, .48, .35, .48, .11);
            }
            case "b01_scratch" -> {
                ring(level, ParticleTypes.CLOUD, source.add(0, .16, 0), 1.15, 22);
                radial(level, ParticleTypes.ASH, source.add(0, .18, 0), .4, 2.1, 7, 5);
                burst(level, ParticleTypes.END_ROD, source.add(0, 1.75, 0), 8, .45, .45, .45, .025);
            }
            case "b01_warn" -> {
                ring(level, ParticleTypes.ASH, source.add(0, .18, 0), 1.55, 28);
                line(level, ParticleTypes.END_ROD, source.add(0, 1.55, 0), source.add(0, 2.55, 0), 7);
            }
            case "b01_charge" -> {
                chargeLane(level, ParticleTypes.CLOUD, source, target, 1.0, 18);
                line(level, ParticleTypes.CRIT, source.add(0, 1.0, 0), target.add(0, 1.0, 0), 22);
                burst(level, ParticleTypes.CRIT, target.add(0, .95, 0), 28, .9, .6, .9, .19);
                ring(level, ParticleTypes.ASH, target.add(0, .18, 0), 1.6, 28);
            }
            default -> generic(level, source, target);
        }
    }

    private static void verna(ServerLevel level, String skillId, Vec3 source, Vec3 target) {
        switch (skillId) {
            case "b02_basic" -> {
                line(level, ParticleTypes.ENCHANT, source.add(0, 1.55, 0), target.add(0, 1.05, 0), 15);
                slashArc(level, ParticleTypes.CRIT, target.add(0, 1.0, 0), .8, 13);
            }
            case "b02_root_prison" -> {
                ring(level, ParticleTypes.ENCHANT, target.add(0, .15, 0), .72, 16);
                ring(level, ParticleTypes.END_ROD, target.add(0, .72, 0), .64, 16);
                ring(level, ParticleTypes.ENCHANT, target.add(0, 1.28, 0), .52, 14);
                burst(level, ParticleTypes.ASH, target.add(0, .25, 0), 12, .45, .2, .45, .025);
            }
            case "b02_summon" -> {
                ring(level, ParticleTypes.ENCHANT, source.add(0, .22, 0), 1.25, 24);
                ring(level, ParticleTypes.END_ROD, source.add(0, 1.35, 0), .9, 20);
                burst(level, ParticleTypes.ASH, source.add(0, 1.0, 0), 18, .75, .65, .75, .035);
            }
            case "b02_thorn_wave" -> {
                radial(level, ParticleTypes.ENCHANT, source.add(0, .25, 0), .65, 3.0, 10, 7);
                ring(level, ParticleTypes.CRIT, source.add(0, .3, 0), 2.2, 32);
                burst(level, ParticleTypes.ASH, source.add(0, 1.2, 0), 18, 1.2, .65, 1.2, .04);
            }
            default -> generic(level, source, target);
        }
    }

    private static void oro7(ServerLevel level, String skillId, Vec3 source, Vec3 target) {
        switch (skillId) {
            case "b03_basic" -> {
                line(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, 1.4, 0), target.add(0, 1.0, 0), 12);
                burst(level, ParticleTypes.CRIT, target.add(0, 1.0, 0), 14, .52, .4, .52, .12);
            }
            case "b03_drain" -> {
                ring(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, .25, 0), 1.15, 22);
                ring(level, ParticleTypes.END_ROD, source.add(0, 1.45, 0), 1.0, 20);
                radial(level, ParticleTypes.CLOUD, source.add(0, .3, 0), .65, 2.7, 8, 6);
            }
            case "b03_barrier" -> {
                ring(level, ParticleTypes.END_ROD, source.add(0, .35, 0), 1.4, 28);
                ring(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, 1.35, 0), 1.25, 24);
                ring(level, ParticleTypes.END_ROD, source.add(0, 2.3, 0), 1.0, 20);
            }
            case "b03_overclock" -> {
                ring(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, 1.45, 0), 1.45, 30);
                burst(level, ParticleTypes.CLOUD, source.add(0, 2.1, 0), 22, .8, .8, .8, .09);
                radial(level, ParticleTypes.END_ROD, source.add(0, 1.4, 0), .35, 1.8, 10, 5);
            }
            default -> generic(level, source, target);
        }
    }

    private static void kolvak(ServerLevel level, String skillId, Vec3 source, Vec3 target) {
        switch (skillId) {
            case "b04_basic" -> {
                line(level, ParticleTypes.ASH, source.add(0, 1.4, 0), target.add(0, 1.0, 0), 9);
                burst(level, ParticleTypes.CRIT, target.add(0, .9, 0), 20, .7, .45, .7, .17);
                burst(level, ParticleTypes.ASH, target.add(0, .45, 0), 16, .75, .35, .75, .07);
            }
            case "b04_collapse" -> {
                ring(level, ParticleTypes.ASH, source.add(0, .15, 0), 1.35, 24);
                radial(level, ParticleTypes.CRIT, source.add(0, .16, 0), .6, 3.15, 10, 8);
                burst(level, ParticleTypes.FLAME, source.add(0, .35, 0), 14, 1.15, .25, 1.15, .035);
            }
            case "b04_fury" -> {
                // Heat is concentrated around the chest core, matching the non-lava-body silhouette canon.
                ring(level, ParticleTypes.FLAME, source.add(0, 1.95, 0), .52, 18);
                burst(level, ParticleTypes.FLAME, source.add(0, 1.95, 0), 18, .38, .38, .38, .035);
                burst(level, ParticleTypes.ASH, source.add(0, 1.65, 0), 12, .55, .55, .55, .025);
            }
            case "b04_warn" -> {
                ring(level, ParticleTypes.FLAME, source.add(0, .18, 0), 1.65, 30);
                ring(level, ParticleTypes.ASH, source.add(0, .18, 0), 2.1, 34);
                line(level, ParticleTypes.END_ROD, source.add(0, 1.9, 0), source.add(0, 2.75, 0), 6);
            }
            case "b04_eruption" -> {
                ring(level, ParticleTypes.FLAME, source.add(0, .2, 0), 1.1, 24);
                ring(level, ParticleTypes.FLAME, source.add(0, .2, 0), 2.0, 32);
                ring(level, ParticleTypes.ASH, source.add(0, .2, 0), 2.8, 38);
                vertical(level, ParticleTypes.FLAME, source.add(0, .2, 0), 3.4, 18);
                burst(level, ParticleTypes.CRIT, source.add(0, .45, 0), 26, 1.35, .55, 1.35, .15);
            }
            default -> generic(level, source, target);
        }
    }

    private static void serak(ServerLevel level, String skillId, Vec3 source, Vec3 target) {
        switch (skillId) {
            case "b05_basic" -> {
                line(level, ParticleTypes.CRIT, source.add(0, 1.25, 0), target.add(0, 1.05, 0), 12);
                slashArc(level, ParticleTypes.PORTAL, target.add(0, 1.05, 0), .82, 14);
            }
            case "b05_time_cut" -> {
                line(level, ParticleTypes.PORTAL, source.add(.25, 1.45, 0), target.add(0, 1.05, 0), 20);
                slashArc(level, ParticleTypes.END_ROD, target.add(0, 1.05, 0), 1.05, 18);
                burst(level, ParticleTypes.SOUL, target.add(0, 1.05, 0), 14, .55, .5, .55, .055);
            }
            case "b05_mark" -> {
                line(level, ParticleTypes.SOUL, source.add(.35, 1.5, 0), target.add(0, 1.15, 0), 13);
                ring(level, ParticleTypes.PORTAL, target.add(0, .28, 0), .8, 20);
                ring(level, ParticleTypes.END_ROD, target.add(0, 1.15, 0), .42, 12);
            }
            case "b05_order_collapse" -> {
                ring(level, ParticleTypes.PORTAL, source.add(0, .25, 0), 1.0, 20);
                ring(level, ParticleTypes.SOUL, source.add(0, .9, 0), 1.55, 28);
                ring(level, ParticleTypes.END_ROD, source.add(0, 1.55, 0), 2.0, 32);
            }
            case "b05_rift_wave" -> {
                ring(level, ParticleTypes.PORTAL, source.add(0, .25, 0), 1.0, 22);
                ring(level, ParticleTypes.PORTAL, source.add(0, .25, 0), 2.0, 30);
                ring(level, ParticleTypes.SOUL, source.add(0, .25, 0), 3.0, 38);
                burst(level, ParticleTypes.END_ROD, source.add(0, 1.1, 0), 18, 1.25, .6, 1.25, .045);
            }
            case "b05_warn" -> {
                ring(level, ParticleTypes.PORTAL, source.add(0, .2, 0), 1.65, 30);
                ring(level, ParticleTypes.SOUL, source.add(0, 1.35, 0), 1.05, 24);
                vertical(level, ParticleTypes.END_ROD, source.add(.45, .4, 0), 2.6, 12);
            }
            case "b05_relay_collapse" -> {
                ring(level, ParticleTypes.PORTAL, source.add(0, .18, 0), 1.2, 26);
                ring(level, ParticleTypes.SOUL, source.add(0, .18, 0), 2.25, 36);
                ring(level, ParticleTypes.END_ROD, source.add(0, .18, 0), 3.35, 46);
                radial(level, ParticleTypes.PORTAL, source.add(0, .3, 0), .55, 3.8, 12, 8);
                burst(level, ParticleTypes.SOUL, source.add(.5, 1.45, 0), 30, 1.25, 1.0, 1.25, .09);
            }
            default -> generic(level, source, target);
        }
    }

    private static void generic(ServerLevel level, Vec3 source, Vec3 target) {
        line(level, ParticleTypes.CRIT, source.add(0, 1.0, 0), target.add(0, 1.0, 0), 8);
    }

    private static void chargeLane(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, double halfWidth, int steps) {
        Vec3 delta = to.subtract(from);
        Vec3 flat = new Vec3(delta.x, 0, delta.z);
        if (flat.lengthSqr() < .001) return;
        Vec3 right = new Vec3(-flat.z, 0, flat.x).normalize().scale(halfWidth);
        line(level, particle, from.add(right).add(0, .18, 0), to.add(right).add(0, .18, 0), steps);
        line(level, particle, from.subtract(right).add(0, .18, 0), to.subtract(right).add(0, .18, 0), steps);
    }

    private static void radial(ServerLevel level, ParticleOptions particle, Vec3 center, double inner, double outer, int rays, int steps) {
        for (int ray = 0; ray < rays; ray++) {
            double angle = Math.PI * 2.0 * ray / rays;
            Vec3 direction = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            line(level, particle, center.add(direction.scale(inner)), center.add(direction.scale(outer)), steps);
        }
    }

    private static void vertical(ServerLevel level, ParticleOptions particle, Vec3 bottom, double height, int steps) {
        line(level, particle, bottom, bottom.add(0, height, 0), steps);
    }

    private static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, int steps) {
        if (steps <= 0) return;
        Vec3 delta = to.subtract(from);
        for (int i = 0; i <= steps; i++) {
            double progress = i / (double)steps;
            Vec3 point = from.add(delta.scale(progress));
            PersonalPresentationIsolation.particles(level, particle, point.x, point.y, point.z,
                    1, .02, .02, .02, 0);
        }
    }

    private static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            PersonalPresentationIsolation.particles(level, particle, x, center.y, z,
                    1, .01, .01, .01, 0);
        }
    }

    private static void slashArc(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double progress = i / (double)Math.max(1, count - 1);
            double angle = -1.15 + progress * 2.3;
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + (progress - .5) * 1.3;
            double z = center.z + Math.sin(angle) * radius;
            PersonalPresentationIsolation.particles(level, particle, x, y, z,
                    1, .01, .01, .01, 0);
        }
    }

    private static void burst(ServerLevel level, ParticleOptions particle, Vec3 center, int count,
                              double dx, double dy, double dz, double speed) {
        PersonalPresentationIsolation.particles(level, particle, center.x, center.y, center.z,
                count, dx, dy, dz, speed);
    }
}
