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
            case "P01" -> kyren(level, source, aim, skillId);
            case "P02" -> lumea(level, source, aim);
            case "P03" -> bram(level, source, aim, damaging);
            case "P04" -> elysia(level, source, aim, skillId);
            case "P05" -> lynette(level, source, aim);
            case "P06" -> morwen(level, source, aim);
            case "P07" -> marion(level, source, aim, skillId);
            case "P07_SUMMON" -> toto(level, source, aim);
            case "P08" -> raze(level, source, aim);
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
        ring(level, primary, center.add(0, 0.12, 0), 1.45, 24);
        ring(level, ParticleTypes.END_ROD, center.add(0, 1.1, 0), 0.85, 16);
        burst(level, ParticleTypes.SMOKE, center.add(0, 0.4, 0), 14, 0.7, 0.4, 0.7, 0.015);
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
        ring(level, primary, center.add(0, 0.2, 0), radius, count);
        ring(level, ParticleTypes.END_ROD, center.add(0, 1.15, 0), radius * 0.65, count - 8);
        burst(level, primary, center.add(0, 1.15, 0), phase >= 3 ? 30 : 20, 0.9, 1.0, 0.9, 0.12);
    }

    public static void revive(ServerLevel level, Vec3 center) {
        ring(level, ParticleTypes.END_ROD, center.add(0, 0.2, 0), 0.9, 20);
        burst(level, ParticleTypes.HEART, center.add(0, 1.0, 0), 8, 0.45, 0.7, 0.45, 0.03);
    }

    public static void down(ServerLevel level, Vec3 center) {
        burst(level, ParticleTypes.SMOKE, center.add(0, 0.9, 0), 14, 0.45, 0.55, 0.45, 0.02);
    }

    public static void victory(ServerLevel level, Vec3 center) {
        ring(level, ParticleTypes.END_ROD, center.add(0, 0.3, 0), 1.0, 18);
        burst(level, ParticleTypes.ENCHANT, center.add(0, 1.4, 0), 22, 0.8, 0.9, 0.8, 0.15);
    }

    private static void kyren(ServerLevel level, Vec3 source, Vec3 target, String skillId) {
        line(level, ParticleTypes.CRIT, source.add(0, 1.15, 0), target.add(0, 1.0, 0), 10);
        if ("p01_breaker_strike".equals(skillId)) {
            slashArc(level, ParticleTypes.END_ROD, target.add(0, 1.0, 0), 1.15, 18);
            burst(level, ParticleTypes.CRIT, target.add(0, 1.0, 0), 16, 0.55, 0.55, 0.55, 0.16);
        } else slashArc(level, ParticleTypes.CRIT, target.add(0, 1.0, 0), 0.8, 12);
    }

    private static void lumea(ServerLevel level, Vec3 source, Vec3 target) {
        ring(level, ParticleTypes.END_ROD, source.add(0, 1.0, 0), 0.85, 18);
        ring(level, ParticleTypes.PORTAL, target.add(0, 0.9, 0), 0.75, 14);
        line(level, ParticleTypes.END_ROD, source.add(0, 1.15, 0), target.add(0, 1.15, 0), 7);
    }

    private static void bram(ServerLevel level, Vec3 source, Vec3 target, boolean damaging) {
        ring(level, ParticleTypes.CLOUD, source.add(0, 0.2, 0), 0.85, 16);
        burst(level, damaging ? ParticleTypes.CRIT : ParticleTypes.END_ROD,
                (damaging ? target : source).add(0, 0.9, 0), 14, 0.6, 0.45, 0.6, 0.08);
    }

    private static void elysia(ServerLevel level, Vec3 source, Vec3 target, String skillId) {
        ring(level, ParticleTypes.END_ROD, target.add(0, 0.25, 0), 0.75, 16);
        burst(level, ParticleTypes.HEART, target.add(0, 1.0, 0), "p04_returned_breath".equals(skillId) ? 12 : 7,
                0.45, 0.7, 0.45, 0.025);
        if ("p04_returned_breath".equals(skillId)) ring(level, ParticleTypes.ENCHANT, target.add(0, 1.0, 0), 1.05, 22);
    }

    private static void lynette(ServerLevel level, Vec3 source, Vec3 target) {
        line(level, ParticleTypes.CRIT, source.add(0, 1.35, 0), target.add(0, 1.05, 0), 18);
        burst(level, ParticleTypes.END_ROD, target.add(0, 1.05, 0), 8, 0.3, 0.3, 0.3, 0.06);
    }

    private static void morwen(ServerLevel level, Vec3 source, Vec3 target) {
        ring(level, ParticleTypes.SOUL, source.add(0, 1.0, 0), 0.72, 18);
        line(level, ParticleTypes.ENCHANT, source.add(0, 1.15, 0), target.add(0, 1.1, 0), 9);
        burst(level, ParticleTypes.SOUL, target.add(0, 1.0, 0), 9, 0.45, 0.65, 0.45, 0.035);
    }

    private static void marion(ServerLevel level, Vec3 source, Vec3 target, String skillId) {
        ring(level, ParticleTypes.ENCHANT, source.add(0, 0.75, 0), 0.82, 18);
        if ("p07_summon_toto".equals(skillId)) {
            ring(level, ParticleTypes.PORTAL, source.add(0, 0.25, 0), 1.15, 28);
            burst(level, ParticleTypes.END_ROD, source.add(0, 0.7, 0), 16, 0.7, 0.5, 0.7, 0.08);
        } else line(level, ParticleTypes.END_ROD, source.add(0, 1.0, 0), target.add(0, 1.0, 0), 8);
    }

    private static void toto(ServerLevel level, Vec3 source, Vec3 target) {
        line(level, ParticleTypes.END_ROD, source.add(0, 0.65, 0), target.add(0, 0.8, 0), 9);
        burst(level, ParticleTypes.CRIT, target.add(0, 0.8, 0), 8, 0.35, 0.3, 0.35, 0.08);
    }

    private static void raze(ServerLevel level, Vec3 source, Vec3 target) {
        burst(level, ParticleTypes.FLAME, source.add(0, 1.0, 0), 10, 0.45, 0.65, 0.45, 0.025);
        line(level, ParticleTypes.CRIT, source.add(0, 1.25, 0), target.add(0, 1.0, 0), 11);
        slashArc(level, ParticleTypes.FLAME, target.add(0, 1.0, 0), 1.0, 16);
    }

    /** Low-rarity fillers remain visually simple but no longer share one generic effect. */
    private static void filler(ServerLevel level, String id, String skillId, Vec3 source, Vec3 target, boolean damaging) {
        switch (id) {
            case "F01" -> {
                line(level, ParticleTypes.CRIT, source.add(0, 1.0, 0), target.add(0, 0.95, 0), 7);
                slashArc(level, ParticleTypes.CLOUD, target.add(0, 0.9, 0), 0.62, 9);
            }
            case "F02" -> {
                ring(level, ParticleTypes.END_ROD, target.add(0, 0.25, 0), 0.58, 12);
                burst(level, ParticleTypes.HEART, target.add(0, 0.95, 0), 5, 0.3, 0.45, 0.3, 0.02);
            }
            case "F03" -> {
                boolean focus = "f03_focus_shot".equals(skillId);
                line(level, focus ? ParticleTypes.END_ROD : ParticleTypes.CRIT,
                        source.add(0, 1.3, 0), target.add(0, 1.0, 0), focus ? 18 : 12);
                burst(level, ParticleTypes.CRIT, target.add(0, 1.0, 0), focus ? 10 : 5,
                        focus ? 0.38 : 0.22, 0.25, focus ? 0.38 : 0.22, 0.06);
            }
            case "F04" -> {
                if ("f04_endure".equals(skillId)) {
                    ring(level, ParticleTypes.CLOUD, source.add(0, 0.25, 0), 0.78, 16);
                    ring(level, ParticleTypes.END_ROD, source.add(0, 0.95, 0), 0.62, 12);
                } else {
                    line(level, ParticleTypes.CRIT, source.add(0, 1.0, 0), target.add(0, 0.9, 0), 7);
                    burst(level, ParticleTypes.CLOUD, target.add(0, 0.65, 0), 10, 0.5, 0.25, 0.5, 0.07);
                }
            }
            default -> generic(level, source, target, damaging);
        }
    }

    private static void enemy(ServerLevel level, String id, String skillId, Vec3 source, Vec3 target, boolean damaging) {
        switch (id) {
            case "E001" -> { burst(level, ParticleTypes.SMOKE, source.add(0, 0.8, 0), 7, 0.4, 0.35, 0.4, 0.01); slashArc(level, ParticleTypes.CRIT, target.add(0, 0.9, 0), 0.62, 9); }
            case "E002" -> { line(level, ParticleTypes.CRIT, source.add(0, 1.55, 0), target.add(0, 1.05, 0), "e002_aimed".equals(skillId) ? 22 : 15); burst(level, ParticleTypes.END_ROD, target.add(0, 1.05, 0), "e002_aimed".equals(skillId) ? 9 : 4, 0.22, 0.22, 0.22, 0.04); }
            case "E003" -> {
                if ("e003_arm".equals(skillId)) { ring(level, ParticleTypes.FLAME, source.add(0, 0.65, 0), 0.82, 18); burst(level, ParticleTypes.SMOKE, source.add(0, 1.0, 0), 15, 0.55, 0.65, 0.55, 0.03); }
                else if ("e003_explode".equals(skillId)) { ring(level, ParticleTypes.FLAME, source.add(0, 0.15, 0), 1.8, 30); burst(level, ParticleTypes.FLAME, source.add(0, 0.9, 0), 34, 1.2, 0.9, 1.2, 0.12); burst(level, ParticleTypes.SMOKE, source.add(0, 1.0, 0), 26, 1.35, 0.9, 1.35, 0.08); }
                else { line(level, ParticleTypes.SMOKE, source.add(0, 0.9, 0), target.add(0, 0.9, 0), 7); burst(level, ParticleTypes.CRIT, target.add(0, 0.8, 0), 7, 0.35, 0.25, 0.35, 0.06); }
            }
            case "E004" -> { line(level, ParticleTypes.CRIT, source.add(0, 1.1, 0), target.add(0, 1.0, 0), 8); slashArc(level, "e004_stab".equals(skillId) ? ParticleTypes.END_ROD : ParticleTypes.CRIT, target.add(0, 1.0, 0), "e004_stab".equals(skillId) ? 0.95 : 0.72, 13); }
            case "E005" -> { ring(level, ParticleTypes.END_ROD, target.add(0, 0.35, 0), "e005_reform".equals(skillId) ? 1.15 : 0.65, 16); burst(level, "e005_reform".equals(skillId) ? ParticleTypes.ENCHANT : ParticleTypes.HEART, target.add(0, 1.0, 0), "e005_reform".equals(skillId) ? 16 : 7, 0.55, 0.6, 0.55, 0.035); }
            case "E006" -> { ring(level, ParticleTypes.CLOUD, source.add(0, 0.15, 0), 0.8, 14); line(level, ParticleTypes.CRIT, source.add(0, 0.7, 0), target.add(0, 0.75, 0), "e006_charge".equals(skillId) ? 15 : 8); burst(level, ParticleTypes.CLOUD, target.add(0, 0.35, 0), 10, 0.65, 0.25, 0.65, 0.06); }
            case "E007" -> { ring(level, ParticleTypes.ENCHANT, source.add(0, 0.8, 0), 0.72, 15); if ("e007_slow_spores".equals(skillId)) { ring(level, ParticleTypes.SOUL, target.add(0, 0.35, 0), 1.25, 22); burst(level, ParticleTypes.ENCHANT, target.add(0, 1.0, 0), 14, 0.85, 0.65, 0.85, 0.025); } else line(level, ParticleTypes.ENCHANT, source.add(0, 1.2, 0), target.add(0, 1.0, 0), 10); }
            case "E008" -> { if ("e008_barrier".equals(skillId)) { ring(level, ParticleTypes.END_ROD, target.add(0, 0.45, 0), 0.95, 22); ring(level, ParticleTypes.ENCHANT, target.add(0, 1.0, 0), 0.72, 16); } else { burst(level, ParticleTypes.CLOUD, target.add(0, 0.35, 0), 10, 0.55, 0.2, 0.55, 0.025); slashArc(level, ParticleTypes.CRIT, target.add(0, 0.9, 0), 0.82, 11); } }
            case "E009" -> { line(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, 1.25, 0), target.add(0, 1.0, 0), damaging ? 10 : 7); ring(level, ParticleTypes.END_ROD, target.add(0, 0.3, 0), "e009_delay".equals(skillId) ? 0.95 : 0.55, 13); }
            case "E010" -> { line(level, ParticleTypes.SOUL, source.add(0, 0.65, 0), target.add(0, 0.75, 0), 9); burst(level, ParticleTypes.SMOKE, target.add(0, 0.7, 0), "e010_flood_rot".equals(skillId) ? 15 : 7, 0.48, 0.35, 0.48, 0.02); }
            case "E011" -> { ring(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, 0.85, 0), 0.72, 15); if ("e011_support".equals(skillId)) { burst(level, ParticleTypes.ELECTRIC_SPARK, target.add(0, 1.0, 0), 12, 0.5, 0.55, 0.5, 0.08); } else burst(level, ParticleTypes.CRIT, target.add(0, 0.95, 0), 6, 0.3, 0.3, 0.3, 0.05); }
            case "E012" -> { burst(level, ParticleTypes.ASH, source.add(0, 0.65, 0), 10, 0.5, 0.25, 0.5, 0.02); line(level, ParticleTypes.CRIT, source.add(0, 0.7, 0), target.add(0, 0.8, 0), "e012_pounce".equals(skillId) ? 14 : 8); slashArc(level, ParticleTypes.ASH, target.add(0, 0.8, 0), 0.75, 10); }
            case "E013" -> { line(level, ParticleTypes.FLAME, source.add(0, 1.25, 0), target.add(0, 1.0, 0), 11); if ("e013_embers".equals(skillId)) { ring(level, ParticleTypes.FLAME, target.add(0, 0.3, 0), 1.25, 24); burst(level, ParticleTypes.ASH, target.add(0, 1.0, 0), 18, 0.85, 0.65, 0.85, 0.04); } else burst(level, ParticleTypes.FLAME, target.add(0, 1.0, 0), 8, 0.35, 0.35, 0.35, 0.07); }
            case "E014" -> { burst(level, ParticleTypes.SMOKE, source.add(0, 0.8, 0), 9, 0.45, 0.45, 0.45, 0.025); line(level, ParticleTypes.FLAME, source.add(0, 1.0, 0), target.add(0, 0.85, 0), "e014_crush".equals(skillId) ? 13 : 8); burst(level, ParticleTypes.CRIT, target.add(0, 0.55, 0), "e014_crush".equals(skillId) ? 18 : 9, 0.7, 0.35, 0.7, 0.09); }
            default -> generic(level, source, target, damaging);
        }
    }

    private static void elite(ServerLevel level, String id, String skillId, Vec3 source, Vec3 target, boolean damaging) {
        switch (id) {
            case "EL01" -> { ring(level, ParticleTypes.SMOKE, source.add(0, 0.35, 0), 1.05, 20); if ("el01_command".equals(skillId)) { ring(level, ParticleTypes.CRIT, source.add(0, 1.1, 0), 1.35, 26); burst(level, ParticleTypes.END_ROD, source.add(0, 1.25, 0), 12, 0.8, 0.55, 0.8, 0.05); } else { line(level, ParticleTypes.CRIT, source.add(0, 1.2, 0), target.add(0, 1.0, 0), 11); slashArc(level, ParticleTypes.CRIT, target.add(0, 1.0, 0), 1.0, 15); } }
            case "EL02" -> { ring(level, ParticleTypes.ENCHANT, source.add(0, 0.3, 0), 1.0, 20); line(level, ParticleTypes.CRIT, source.add(0, 0.95, 0), target.add(0, 0.9, 0), "el02_piercing_horn".equals(skillId) ? 18 : 11); burst(level, ParticleTypes.END_ROD, target.add(0, 0.95, 0), "el02_piercing_horn".equals(skillId) ? 13 : 7, 0.45, 0.4, 0.45, 0.08); }
            case "EL03" -> { ring(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, 0.35, 0), 1.05, 20); if ("el03_barrier".equals(skillId)) { ring(level, ParticleTypes.END_ROD, source.add(0, 0.85, 0), 1.2, 26); ring(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, 1.25, 0), 0.82, 18); } else slashArc(level, ParticleTypes.CRIT, target.add(0, 1.0, 0), 1.0, 14); }
            case "EL04" -> { ring(level, ParticleTypes.FLAME, source.add(0, 0.2, 0), 1.25, 24); burst(level, ParticleTypes.ASH, source.add(0, 1.0, 0), 14, 0.75, 0.75, 0.75, 0.025); if ("el04_collapse".equals(skillId)) { ring(level, ParticleTypes.FLAME, target.add(0, 0.25, 0), 1.6, 30); burst(level, ParticleTypes.CRIT, target.add(0, 0.65, 0), 22, 1.0, 0.45, 1.0, 0.12); } else { line(level, ParticleTypes.FLAME, source.add(0, 1.0, 0), target.add(0, 0.9, 0), 12); burst(level, ParticleTypes.CRIT, target.add(0, 0.75, 0), 12, 0.55, 0.4, 0.55, 0.08); } }
            default -> generic(level, source, target, damaging);
        }
    }

    private static void bossGraoul(ServerLevel level, Vec3 source, Vec3 target, String skillId) {
        ring(level, ParticleTypes.CLOUD, source.add(0, 0.15, 0), "b01_charge".equals(skillId) ? 1.7 : 1.0, 24);
        line(level, ParticleTypes.CRIT, source.add(0, 0.9, 0), target.add(0, 0.9, 0), 14);
    }

    private static void bossVerna(ServerLevel level, Vec3 source, Vec3 target, String skillId) {
        ring(level, ParticleTypes.ENCHANT, source.add(0, 0.35, 0), 1.45, 26);
        if ("b02_summon".equals(skillId)) burst(level, ParticleTypes.PORTAL, source.add(0, 1.0, 0), 28, 1.1, 0.8, 1.1, 0.12);
        else burst(level, ParticleTypes.CRIT, target.add(0, 1.0, 0), 14, 0.7, 0.6, 0.7, 0.08);
    }

    private static void bossOro(ServerLevel level, Vec3 source, Vec3 target, String skillId) {
        ring(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, 0.5, 0), 1.3, 24);
        if ("b03_overclock".equals(skillId)) { ring(level, ParticleTypes.END_ROD, source.add(0, 1.25, 0), 1.65, 30); burst(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, 1.1, 0), 24, 1.0, 0.8, 1.0, 0.18); }
        else line(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, 1.1, 0), target.add(0, 1.0, 0), 10);
    }

    private static void bossKolvak(ServerLevel level, Vec3 source, Vec3 target, String skillId) {
        ring(level, ParticleTypes.FLAME, source.add(0, 0.2, 0), 1.5, 28);
        burst(level, ParticleTypes.ASH, source.add(0, 1.1, 0), 22, 1.0, 0.9, 1.0, 0.03);
        if ("b04_eruption".equals(skillId)) burst(level, ParticleTypes.FLAME, target.add(0, 0.8, 0), 30, 1.2, 0.8, 1.2, 0.12);
    }

    private static void bossSerak(ServerLevel level, Vec3 source, Vec3 target, String skillId) {
        ring(level, ParticleTypes.PORTAL, source.add(0, 0.35, 0), 1.45, 30);
        line(level, ParticleTypes.SOUL, source.add(0, 1.3, 0), target.add(0, 1.0, 0), 12);
        if ("b05_relay_collapse".equals(skillId)) { ring(level, ParticleTypes.END_ROD, source.add(0, 1.0, 0), 2.0, 36); burst(level, ParticleTypes.PORTAL, source.add(0, 1.0, 0), 36, 1.3, 1.1, 1.3, 0.18); }
    }

    private static void generic(ServerLevel level, Vec3 source, Vec3 target, boolean damaging) {
        ParticleOptions type = damaging ? ParticleTypes.CRIT : ParticleTypes.ENCHANT;
        line(level, type, source.add(0, 1.0, 0), target.add(0, 1.0, 0), damaging ? 8 : 5);
    }

    private static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, int steps) {
        if (steps <= 0) return;
        Vec3 delta = to.subtract(from);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 p = from.add(delta.scale(t));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2.0 * i / count;
            double x = center.x + Math.cos(a) * radius;
            double z = center.z + Math.sin(a) * radius;
            level.sendParticles(particle, x, center.y, z, 1, 0.01, 0.01, 0.01, 0.0);
        }
    }

    private static void slashArc(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double t = i / (double) Math.max(1, count - 1);
            double a = -1.2 + t * 2.4;
            double x = center.x + Math.cos(a) * radius;
            double y = center.y + (t - 0.5) * 1.2;
            double z = center.z + Math.sin(a) * radius;
            level.sendParticles(particle, x, y, z, 1, 0.01, 0.01, 0.01, 0.0);
        }
    }

    private static void burst(ServerLevel level, ParticleOptions particle, Vec3 center, int count,
                              double dx, double dy, double dz, double speed) {
        level.sendParticles(particle, center.x, center.y, center.z, count, dx, dy, dz, speed);
    }
}
