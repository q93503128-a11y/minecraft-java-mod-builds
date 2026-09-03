package io.github.q93503128.turnbound.presentation;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Authored battle VFX language. Presentation never decides gameplay results. */
public final class BattleVfx {
    private BattleVfx() { }

    public static void skill(ServerLevel level, String combatantId, String skillId, Vec3 source, Vec3 target, boolean damaging) {
        Vec3 aim = target == null ? source : target;
        switch (combatantId) {
            case "P01", "P02", "P03", "P04", "P05", "P06", "P07", "P08" ->
                    CoreHeroVfx.skill(level, combatantId, skillId, source, aim, damaging);
            case "P07_SUMMON" -> toto(level, source, aim);
            case "F01", "F02", "F03", "F04" -> filler(level, combatantId, skillId, source, aim, damaging);
            case "E001", "E002", "E003", "E004", "E005", "E006", "E007", "E008", "E009", "E010",
                    "E011", "E012", "E013", "E014" -> enemy(level, combatantId, skillId, source, aim, damaging);
            case "EL01", "EL02", "EL03", "EL04" -> elite(level, combatantId, skillId, source, aim, damaging);
            case "B01" -> bossGraoul(level, source, aim, skillId);
            case "B02" -> bossVerna(level, source, aim, skillId);
            case "B03" -> bossOro(level, source, aim, skillId);
            case "B04" -> bossKolvak(level, source, aim, skillId);
            case "B05" -> bossSerak(level, source, aim, skillId);
            default -> generic(level, source, aim, damaging);
        }
    }

    public static void warning(ServerLevel level, String combatantId, Vec3 center) {
        ParticleOptions primary = switch (combatantId) {
            case "E003", "B04" -> ParticleTypes.FLAME;
            case "B01" -> ParticleTypes.CLOUD;
            case "B05" -> ParticleTypes.PORTAL;
            default -> ParticleTypes.CRIT;
        };
        ring(level, primary, center.add(0, .12, 0), 1.45, 24);
        ring(level, ParticleTypes.END_ROD, center.add(0, 1.1, 0), .85, 16);
        burst(level, ParticleTypes.SMOKE, center.add(0, .4, 0), 14, .7, .4, .7, .015);
    }

    public static void phase(ServerLevel level, String combatantId, Vec3 center, int phase) {
        ParticleOptions primary = switch (combatantId) {
            case "B01" -> ParticleTypes.CLOUD;
            case "B02" -> ParticleTypes.ENCHANT;
            case "B03" -> ParticleTypes.ELECTRIC_SPARK;
            case "B04" -> ParticleTypes.FLAME;
            case "B05" -> ParticleTypes.PORTAL;
            default -> ParticleTypes.END_ROD;
        };
        double radius = phase >= 3 ? 1.9 : 1.45;
        int count = phase >= 3 ? 36 : 26;
        ring(level, primary, center.add(0, .2, 0), radius, count);
        ring(level, ParticleTypes.END_ROD, center.add(0, 1.15, 0), radius * .65, count - 8);
        burst(level, primary, center.add(0, 1.15, 0), phase >= 3 ? 30 : 20, .9, 1, .9, .12);
    }

    /** Neutral revive feedback: character-specific heal grammar is handled by the caster skill VFX. */
    public static void revive(ServerLevel level, Vec3 center) {
        ring(level, ParticleTypes.END_ROD, center.add(0, .2, 0), .9, 20);
        ring(level, ParticleTypes.ENCHANT, center.add(0, .85, 0), .62, 14);
        burst(level, ParticleTypes.END_ROD, center.add(0, 1.0, 0), 10, .45, .7, .45, .03);
    }

    public static void down(ServerLevel level, Vec3 center) {
        burst(level, ParticleTypes.SMOKE, center.add(0, .9, 0), 14, .45, .55, .45, .02);
    }

    public static void victory(ServerLevel level, Vec3 center) {
        ring(level, ParticleTypes.END_ROD, center.add(0, .3, 0), 1, 18);
        burst(level, ParticleTypes.ENCHANT, center.add(0, 1.4, 0), 22, .8, .9, .8, .15);
    }

    private static void toto(ServerLevel level, Vec3 source, Vec3 target) {
        line(level, ParticleTypes.END_ROD, source.add(0, .65, 0), target.add(0, .8, 0), 9);
        burst(level, ParticleTypes.CRIT, target.add(0, .8, 0), 8, .35, .3, .35, .08);
    }

    private static void filler(ServerLevel level, String id, String skillId, Vec3 source, Vec3 target, boolean damaging) {
        switch (id) {
            case "F01" -> {
                line(level, ParticleTypes.CRIT, source.add(0, 1, 0), target.add(0, .95, 0), 7);
                slashArc(level, ParticleTypes.CLOUD, target.add(0, .9, 0), .62, 9);
            }
            case "F02" -> {
                ring(level, ParticleTypes.END_ROD, target.add(0, .25, 0), .58, 12);
                burst(level, ParticleTypes.HEART, target.add(0, .95, 0), 5, .3, .45, .3, .02);
            }
            case "F03" -> {
                boolean focus = "f03_focus_shot".equals(skillId);
                line(level, focus ? ParticleTypes.END_ROD : ParticleTypes.CRIT,
                        source.add(0, 1.3, 0), target.add(0, 1, 0), focus ? 18 : 12);
                burst(level, ParticleTypes.CRIT, target.add(0, 1, 0), focus ? 10 : 5,
                        focus ? .38 : .22, .25, focus ? .38 : .22, .06);
            }
            case "F04" -> {
                if ("f04_endure".equals(skillId)) {
                    ring(level, ParticleTypes.CLOUD, source.add(0, .25, 0), .78, 16);
                    ring(level, ParticleTypes.END_ROD, source.add(0, .95, 0), .62, 12);
                } else {
                    line(level, ParticleTypes.CRIT, source.add(0, 1, 0), target.add(0, .9, 0), 7);
                    burst(level, ParticleTypes.CLOUD, target.add(0, .65, 0), 10, .5, .25, .5, .07);
                }
            }
            default -> generic(level, source, target, damaging);
        }
    }

    private static void enemy(ServerLevel level, String id, String skillId, Vec3 source, Vec3 target, boolean damaging) {
        switch (id) {
            case "E001" -> {
                burst(level, ParticleTypes.SMOKE, source.add(0, .8, 0), 7, .4, .35, .4, .01);
                slashArc(level, ParticleTypes.CRIT, target.add(0, .9, 0), .62, 9);
            }
            case "E002" -> {
                boolean aimed = "e002_aimed".equals(skillId);
                line(level, ParticleTypes.CRIT, source.add(0, 1.55, 0), target.add(0, 1.05, 0), aimed ? 22 : 15);
                burst(level, ParticleTypes.END_ROD, target.add(0, 1.05, 0), aimed ? 9 : 4, .22, .22, .22, .04);
            }
            case "E003" -> {
                if ("e003_arm".equals(skillId)) {
                    ring(level, ParticleTypes.FLAME, source.add(0, .65, 0), .82, 18);
                    burst(level, ParticleTypes.SMOKE, source.add(0, 1, 0), 15, .55, .65, .55, .03);
                } else if ("e003_explode".equals(skillId)) {
                    ring(level, ParticleTypes.FLAME, source.add(0, .15, 0), 1.8, 30);
                    burst(level, ParticleTypes.FLAME, source.add(0, .9, 0), 34, 1.2, .9, 1.2, .12);
                    burst(level, ParticleTypes.SMOKE, source.add(0, 1, 0), 26, 1.35, .9, 1.35, .08);
                } else {
                    line(level, ParticleTypes.SMOKE, source.add(0, .9, 0), target.add(0, .9, 0), 7);
                    burst(level, ParticleTypes.CRIT, target.add(0, .8, 0), 7, .35, .25, .35, .06);
                }
            }
            case "E004" -> {
                line(level, ParticleTypes.CRIT, source.add(0, 1.1, 0), target.add(0, 1, 0), 8);
                boolean stab = "e004_stab".equals(skillId);
                slashArc(level, stab ? ParticleTypes.END_ROD : ParticleTypes.CRIT,
                        target.add(0, 1, 0), stab ? .95 : .72, 13);
            }
            case "E005" -> {
                boolean reform = "e005_reform".equals(skillId);
                ring(level, ParticleTypes.END_ROD, target.add(0, .35, 0), reform ? 1.15 : .65, 16);
                burst(level, reform ? ParticleTypes.ENCHANT : ParticleTypes.HEART,
                        target.add(0, 1, 0), reform ? 16 : 7, .55, .6, .55, .035);
            }
            case "E006" -> {
                ring(level, ParticleTypes.CLOUD, source.add(0, .15, 0), .8, 14);
                line(level, ParticleTypes.CRIT, source.add(0, .7, 0), target.add(0, .75, 0),
                        "e006_charge".equals(skillId) ? 15 : 8);
                burst(level, ParticleTypes.CLOUD, target.add(0, .35, 0), 10, .65, .25, .65, .06);
            }
            case "E007" -> {
                ring(level, ParticleTypes.ENCHANT, source.add(0, .8, 0), .72, 15);
                if ("e007_slow_spores".equals(skillId)) {
                    ring(level, ParticleTypes.SOUL, target.add(0, .35, 0), 1.25, 22);
                    burst(level, ParticleTypes.ENCHANT, target.add(0, 1, 0), 14, .85, .65, .85, .025);
                } else {
                    line(level, ParticleTypes.ENCHANT, source.add(0, 1.2, 0), target.add(0, 1, 0), 10);
                }
            }
            case "E008" -> {
                if ("e008_barrier".equals(skillId)) {
                    ring(level, ParticleTypes.END_ROD, target.add(0, .45, 0), .95, 22);
                    ring(level, ParticleTypes.ENCHANT, target.add(0, 1, 0), .72, 16);
                } else {
                    burst(level, ParticleTypes.CLOUD, target.add(0, .35, 0), 10, .55, .2, .55, .025);
                    slashArc(level, ParticleTypes.CRIT, target.add(0, .9, 0), .82, 11);
                }
            }
            case "E009" -> {
                line(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, 1.25, 0), target.add(0, 1, 0), damaging ? 10 : 7);
                ring(level, ParticleTypes.END_ROD, target.add(0, .3, 0), "e009_delay".equals(skillId) ? .95 : .55, 13);
            }
            case "E010" -> {
                line(level, ParticleTypes.SOUL, source.add(0, .65, 0), target.add(0, .75, 0), 9);
                burst(level, ParticleTypes.SMOKE, target.add(0, .7, 0), "e010_flood_rot".equals(skillId) ? 15 : 7,
                        .48, .35, .48, .02);
            }
            case "E011" -> {
                ring(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, .85, 0), .72, 15);
                if ("e011_support".equals(skillId)) {
                    burst(level, ParticleTypes.ELECTRIC_SPARK, target.add(0, 1, 0), 12, .5, .55, .5, .08);
                } else {
                    burst(level, ParticleTypes.CRIT, target.add(0, .95, 0), 6, .3, .3, .3, .05);
                }
            }
            case "E012" -> {
                burst(level, ParticleTypes.ASH, source.add(0, .65, 0), 10, .5, .25, .5, .02);
                line(level, ParticleTypes.CRIT, source.add(0, .7, 0), target.add(0, .8, 0),
                        "e012_pounce".equals(skillId) ? 14 : 8);
                slashArc(level, ParticleTypes.ASH, target.add(0, .8, 0), .75, 10);
            }
            case "E013" -> {
                line(level, ParticleTypes.FLAME, source.add(0, 1.25, 0), target.add(0, 1, 0), 11);
                if ("e013_embers".equals(skillId)) {
                    ring(level, ParticleTypes.FLAME, target.add(0, .3, 0), 1.25, 24);
                    burst(level, ParticleTypes.ASH, target.add(0, 1, 0), 18, .85, .65, .85, .04);
                } else {
                    burst(level, ParticleTypes.FLAME, target.add(0, 1, 0), 8, .35, .35, .35, .07);
                }
            }
            case "E014" -> {
                boolean crush = "e014_crush".equals(skillId);
                burst(level, ParticleTypes.SMOKE, source.add(0, .8, 0), 9, .45, .45, .45, .025);
                line(level, ParticleTypes.FLAME, source.add(0, 1, 0), target.add(0, .85, 0), crush ? 13 : 8);
                burst(level, ParticleTypes.CRIT, target.add(0, .55, 0), crush ? 18 : 9, .7, .35, .7, .09);
            }
            default -> generic(level, source, target, damaging);
        }
    }

    private static void elite(ServerLevel level, String id, String skillId, Vec3 source, Vec3 target, boolean damaging) {
        switch (id) {
            case "EL01" -> {
                ring(level, ParticleTypes.SMOKE, source.add(0, .35, 0), 1.05, 20);
                if ("el01_command".equals(skillId)) {
                    ring(level, ParticleTypes.CRIT, source.add(0, 1.1, 0), 1.35, 26);
                    burst(level, ParticleTypes.END_ROD, source.add(0, 1.25, 0), 12, .8, .55, .8, .05);
                } else {
                    line(level, ParticleTypes.CRIT, source.add(0, 1.2, 0), target.add(0, 1, 0), 11);
                    slashArc(level, ParticleTypes.CRIT, target.add(0, 1, 0), 1, 15);
                }
            }
            case "EL02" -> {
                boolean piercingHorn = "el02_piercing_horn".equals(skillId);
                ring(level, ParticleTypes.ENCHANT, source.add(0, .3, 0), 1, 20);
                line(level, ParticleTypes.CRIT, source.add(0, .95, 0), target.add(0, .9, 0), piercingHorn ? 18 : 11);
                burst(level, ParticleTypes.END_ROD, target.add(0, .95, 0), piercingHorn ? 13 : 7, .45, .4, .45, .08);
            }
            case "EL03" -> {
                ring(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, .35, 0), 1.05, 20);
                if ("el03_barrier".equals(skillId)) {
                    ring(level, ParticleTypes.END_ROD, source.add(0, .85, 0), 1.2, 26);
                    ring(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, 1.25, 0), .82, 18);
                } else {
                    slashArc(level, ParticleTypes.CRIT, target.add(0, 1, 0), 1, 14);
                }
            }
            case "EL04" -> {
                ring(level, ParticleTypes.FLAME, source.add(0, .2, 0), 1.25, 24);
                burst(level, ParticleTypes.ASH, source.add(0, 1, 0), 14, .75, .75, .75, .025);
                if ("el04_collapse".equals(skillId)) {
                    ring(level, ParticleTypes.FLAME, target.add(0, .25, 0), 1.6, 30);
                    burst(level, ParticleTypes.CRIT, target.add(0, .65, 0), 22, 1, .45, 1, .12);
                } else {
                    line(level, ParticleTypes.FLAME, source.add(0, 1, 0), target.add(0, .9, 0), 12);
                    burst(level, ParticleTypes.CRIT, target.add(0, .75, 0), 12, .55, .4, .55, .08);
                }
            }
            default -> generic(level, source, target, damaging);
        }
    }

    private static void bossGraoul(ServerLevel level, Vec3 source, Vec3 target, String skillId) {
        ring(level, ParticleTypes.CLOUD, source.add(0, .15, 0), "b01_charge".equals(skillId) ? 1.7 : 1, 24);
        line(level, ParticleTypes.CRIT, source.add(0, .9, 0), target.add(0, .9, 0), 14);
    }

    private static void bossVerna(ServerLevel level, Vec3 source, Vec3 target, String skillId) {
        ring(level, ParticleTypes.ENCHANT, source.add(0, .35, 0), 1.45, 26);
        if ("b02_summon".equals(skillId)) {
            burst(level, ParticleTypes.PORTAL, source.add(0, 1, 0), 28, 1.1, .8, 1.1, .12);
        } else {
            burst(level, ParticleTypes.CRIT, target.add(0, 1, 0), 14, .7, .6, .7, .08);
        }
    }

    private static void bossOro(ServerLevel level, Vec3 source, Vec3 target, String skillId) {
        ring(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, .5, 0), 1.3, 24);
        if ("b03_overclock".equals(skillId)) {
            ring(level, ParticleTypes.END_ROD, source.add(0, 1.25, 0), 1.65, 30);
            burst(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, 1.1, 0), 24, 1, .8, 1, .18);
        } else {
            line(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, 1.1, 0), target.add(0, 1, 0), 10);
        }
    }

    private static void bossKolvak(ServerLevel level, Vec3 source, Vec3 target, String skillId) {
        ring(level, ParticleTypes.FLAME, source.add(0, .2, 0), 1.5, 28);
        burst(level, ParticleTypes.ASH, source.add(0, 1.1, 0), 22, 1, .9, 1, .03);
        if ("b04_eruption".equals(skillId)) {
            burst(level, ParticleTypes.FLAME, target.add(0, .8, 0), 30, 1.2, .8, 1.2, .12);
        }
    }

    private static void bossSerak(ServerLevel level, Vec3 source, Vec3 target, String skillId) {
        ring(level, ParticleTypes.PORTAL, source.add(0, .35, 0), 1.45, 30);
        line(level, ParticleTypes.SOUL, source.add(0, 1.3, 0), target.add(0, 1, 0), 12);
        if ("b05_relay_collapse".equals(skillId)) {
            ring(level, ParticleTypes.END_ROD, source.add(0, 1, 0), 2, 36);
            burst(level, ParticleTypes.PORTAL, source.add(0, 1, 0), 36, 1.3, 1.1, 1.3, .18);
        }
    }

    private static void generic(ServerLevel level, Vec3 source, Vec3 target, boolean damaging) {
        ParticleOptions type = damaging ? ParticleTypes.CRIT : ParticleTypes.ENCHANT;
        line(level, type, source.add(0, 1, 0), target.add(0, 1, 0), damaging ? 8 : 5);
    }

    private static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, int steps) {
        if (steps <= 0) return;
        Vec3 delta = to.subtract(from);
        for (int i = 0; i <= steps; i++) {
            double progress = i / (double) steps;
            Vec3 point = from.add(delta.scale(progress));
            level.sendParticles(particle, point.x, point.y, point.z, 1, .02, .02, .02, 0);
        }
    }

    private static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, center.y, z, 1, .01, .01, .01, 0);
        }
    }

    private static void slashArc(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double progress = i / (double) Math.max(1, count - 1);
            double angle = -1.2 + progress * 2.4;
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + (progress - .5) * 1.2;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, y, z, 1, .01, .01, .01, 0);
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
