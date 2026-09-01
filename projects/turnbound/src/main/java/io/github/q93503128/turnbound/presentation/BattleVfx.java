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
                    coreHero(level, combatantId, skillId, source, aim, damaging);
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

    private static void coreHero(ServerLevel level, String heroId, String skillId, Vec3 source, Vec3 target, boolean damaging) {
        switch (HeroSkillVfxStyle.resolve(heroId, skillId)) {
            case P01_BASIC -> {
                line(level, ParticleTypes.CRIT, source.add(0, 1.15, 0), target.add(0, 1, 0), 10);
                slashArc(level, ParticleTypes.CRIT, target.add(0, .92, 0), .72, 11);
                slashArc(level, ParticleTypes.END_ROD, target.add(0, 1.12, 0), .86, 11);
            }
            case P01_BREAKER -> {
                line(level, ParticleTypes.END_ROD, source.add(0, 1.15, 0), target.add(0, 1, 0), 14);
                slashArc(level, ParticleTypes.END_ROD, target.add(0, 1, 0), 1.18, 20);
                burst(level, ParticleTypes.CRIT, target.add(0, 1, 0), 18, .58, .58, .58, .17);
            }
            case P01_CHAIN_RUSH -> {
                line(level, ParticleTypes.CRIT, source.add(0, 1.05, 0), target.add(0, .95, 0), 19);
                slashArc(level, ParticleTypes.CRIT, target.add(0, .84, 0), .74, 12);
                slashArc(level, ParticleTypes.END_ROD, target.add(0, 1.18, 0), 1.02, 17);
                burst(level, ParticleTypes.CRIT, target.add(0, 1, 0), 20, .7, .55, .7, .14);
            }
            case P02_BASIC -> {
                line(level, ParticleTypes.END_ROD, source.add(0, 1.2, 0), target.add(0, 1.1, 0), 10);
                ring(level, ParticleTypes.PORTAL, target.add(0, .85, 0), .58, 13);
            }
            case P02_TIME_LEAP -> {
                ring(level, ParticleTypes.END_ROD, source.add(0, 1, 0), .9, 20);
                line(level, ParticleTypes.END_ROD, source.add(0, 1.2, 0), target.add(0, 1.15, 0), 9);
                ring(level, ParticleTypes.ELECTRIC_SPARK, target.add(0, .92, 0), .82, 18);
                burst(level, ParticleTypes.END_ROD, target.add(0, 1.15, 0), 14, .45, .6, .45, .06);
            }
            case P02_CLOCK_REVERSAL -> {
                ring(level, ParticleTypes.PORTAL, target.add(0, .35, 0), 1.02, 24);
                ring(level, ParticleTypes.END_ROD, target.add(0, 1.02, 0), .68, 18);
                ring(level, ParticleTypes.PORTAL, target.add(0, 1.48, 0), .42, 12);
                line(level, ParticleTypes.END_ROD, source.add(0, 1.25, 0), target.add(0, 1.1, 0), 8);
            }
            case P03_BASIC -> {
                ring(level, ParticleTypes.CLOUD, source.add(0, .18, 0), .68, 14);
                line(level, ParticleTypes.CRIT, source.add(0, .95, 0), target.add(0, .9, 0), 8);
                burst(level, ParticleTypes.CLOUD, target.add(0, .65, 0), 10, .5, .25, .5, .07);
            }
            case P03_GUARD_TRANSFER -> {
                line(level, ParticleTypes.END_ROD, source.add(0, 1, 0), target.add(0, 1, 0), 8);
                ring(level, ParticleTypes.END_ROD, target.add(0, .45, 0), .92, 22);
                ring(level, ParticleTypes.CLOUD, target.add(0, 1.0, 0), .68, 16);
                burst(level, ParticleTypes.END_ROD, target.add(0, 1.0, 0), 10, .45, .55, .45, .03);
            }
            case P03_COUNTER_FIELD -> {
                ring(level, ParticleTypes.CLOUD, source.add(0, .18, 0), 1.42, 30);
                ring(level, ParticleTypes.END_ROD, source.add(0, .7, 0), 1.08, 24);
                ring(level, ParticleTypes.CRIT, source.add(0, 1.18, 0), .72, 16);
                burst(level, ParticleTypes.CLOUD, source.add(0, .75, 0), 18, .85, .45, .85, .05);
            }
            case P04_BASIC -> {
                line(level, ParticleTypes.END_ROD, source.add(0, 1.15, 0), target.add(0, 1.05, 0), 8);
                ring(level, ParticleTypes.END_ROD, target.add(0, .3, 0), .56, 13);
                burst(level, ParticleTypes.ENCHANT, target.add(0, .95, 0), 8, .3, .48, .3, .02);
            }
            case P04_RETURNED_BREATH -> {
                line(level, ParticleTypes.END_ROD, source.add(0, 1.2, 0), target.add(0, 1.05, 0), 10);
                ring(level, ParticleTypes.END_ROD, target.add(0, .28, 0), .82, 18);
                ring(level, ParticleTypes.ENCHANT, target.add(0, 1.12, 0), .94, 22);
                ring(level, ParticleTypes.END_ROD, target.add(0, 1.58, 0), .5, 13);
                burst(level, ParticleTypes.END_ROD, target.add(0, 1.0, 0), 18, .38, .62, .38, .025);
            }
            case P04_LAST_PRAYER -> {
                ring(level, ParticleTypes.END_ROD, source.add(0, .2, 0), 1.55, 32);
                ring(level, ParticleTypes.ENCHANT, source.add(0, .85, 0), 1.12, 26);
                ring(level, ParticleTypes.END_ROD, source.add(0, 1.45, 0), .68, 18);
                burst(level, ParticleTypes.ENCHANT, source.add(0, 1.1, 0), 24, .9, .85, .9, .035);
            }
            case P05_BASIC -> {
                line(level, ParticleTypes.CRIT, source.add(0, 1.35, 0), target.add(0, 1.05, 0), 18);
                burst(level, ParticleTypes.END_ROD, target.add(0, 1.05, 0), 8, .3, .3, .3, .06);
            }
            case P05_PURSUIT_MARK -> {
                line(level, ParticleTypes.END_ROD, source.add(0, 1.36, 0), target.add(0, 1.05, 0), 20);
                ring(level, ParticleTypes.ENCHANT, target.add(0, .95, 0), .76, 18);
                ring(level, ParticleTypes.END_ROD, target.add(0, 1.05, 0), .36, 10);
                burst(level, ParticleTypes.END_ROD, target.add(0, 1.05, 0), 9, .28, .28, .28, .04);
            }
            case P05_FINISHER -> {
                line(level, ParticleTypes.END_ROD, source.add(0, 1.38, 0), target.add(0, 1.05, 0), 28);
                line(level, ParticleTypes.CRIT, source.add(.08, 1.32, .08), target.add(0, 1.05, 0), 20);
                burst(level, ParticleTypes.CRIT, target.add(0, 1.05, 0), 18, .5, .42, .5, .14);
                burst(level, ParticleTypes.END_ROD, target.add(0, 1.05, 0), 12, .34, .34, .34, .08);
            }
            case P06_BASIC -> {
                ring(level, ParticleTypes.ENCHANT, source.add(0, .95, 0), .62, 14);
                line(level, ParticleTypes.SOUL, source.add(0, 1.15, 0), target.add(0, 1.05, 0), 10);
                slashArc(level, ParticleTypes.SOUL, target.add(0, 1.0, 0), .78, 12);
            }
            case P06_MEMORY_CUT -> {
                ring(level, ParticleTypes.SOUL, source.add(0, .95, 0), .88, 20);
                line(level, ParticleTypes.ENCHANT, source.add(0, 1.2, 0), target.add(0, 1.08, 0), 14);
                slashArc(level, ParticleTypes.END_ROD, target.add(0, 1.02, 0), 1.02, 17);
                burst(level, ParticleTypes.SOUL, target.add(0, 1.0, 0), 14, .55, .58, .55, .05);
            }
            case P06_GRAVE_RETURN -> {
                ring(level, ParticleTypes.SOUL, target.add(0, .25, 0), 1.18, 26);
                ring(level, ParticleTypes.ENCHANT, target.add(0, 1.05, 0), .78, 20);
                line(level, ParticleTypes.SOUL, source.add(0, 1.12, 0), target.add(0, 1.05, 0), 16);
                burst(level, ParticleTypes.END_ROD, target.add(0, 1.0, 0), 18, .62, .72, .62, .07);
            }
            case P07_BASIC -> {
                ring(level, ParticleTypes.ENCHANT, source.add(0, .72, 0), .58, 13);
                line(level, ParticleTypes.END_ROD, source.add(0, 1.0, 0), target.add(0, .9, 0), 9);
                burst(level, ParticleTypes.CRIT, target.add(0, .85, 0), 7, .3, .25, .3, .06);
            }
            case P07_SUMMON_TOTO -> {
                ring(level, ParticleTypes.ENCHANT, source.add(0, .75, 0), .82, 18);
                ring(level, ParticleTypes.CRIT, source.add(0, .12, 0), 1.15, 28);
                ring(level, ParticleTypes.END_ROD, source.add(0, .58, 0), .68, 16);
                burst(level, ParticleTypes.END_ROD, source.add(0, .7, 0), 16, .7, .5, .7, .08);
            }
            case P07_JOINT_ATTACK -> {
                ring(level, ParticleTypes.ENCHANT, source.add(0, .72, 0), .72, 16);
                line(level, ParticleTypes.END_ROD, source.add(-.16, 1.05, 0), target.add(0, .95, 0), 14);
                line(level, ParticleTypes.CRIT, source.add(.16, .72, 0), target.add(0, .82, 0), 12);
                burst(level, ParticleTypes.END_ROD, target.add(0, .9, 0), 12, .42, .36, .42, .08);
                burst(level, ParticleTypes.CRIT, target.add(0, .82, 0), 12, .48, .32, .48, .1);
            }
            case P08_BASIC -> {
                burst(level, ParticleTypes.FLAME, source.add(0, 1, 0), 6, .32, .45, .32, .02);
                line(level, ParticleTypes.CRIT, source.add(0, 1.22, 0), target.add(0, 1, 0), 11);
                slashArc(level, ParticleTypes.FLAME, target.add(0, 1, 0), .86, 13);
            }
            case P08_BLOOD_CHARGE -> {
                ring(level, ParticleTypes.FLAME, source.add(0, .25, 0), .72, 16);
                burst(level, ParticleTypes.FLAME, source.add(0, 1, 0), 11, .5, .6, .5, .04);
                line(level, ParticleTypes.FLAME, source.add(0, 1.15, 0), target.add(0, .95, 0), 17);
                slashArc(level, ParticleTypes.CRIT, target.add(0, .95, 0), 1.12, 18);
                burst(level, ParticleTypes.FLAME, target.add(0, .85, 0), 20, .72, .48, .72, .13);
            }
            case P08_BATTLE_MANIA -> {
                ring(level, ParticleTypes.FLAME, source.add(0, .18, 0), 1.08, 24);
                ring(level, ParticleTypes.CRIT, source.add(0, .88, 0), .78, 18);
                burst(level, ParticleTypes.FLAME, source.add(0, 1.0, 0), 22, .75, .85, .75, .08);
                burst(level, ParticleTypes.ASH, source.add(0, 1.2, 0), 14, .7, .65, .7, .025);
            }
            case GENERIC -> generic(level, source, target, damaging);
        }
    }

    private static void toto(ServerLevel level, Vec3 source, Vec3 target) {
        line(level, ParticleTypes.END_ROD, source.add(0, .65, 0), target.add(0, .8, 0), 9);
        burst(level, ParticleTypes.CRIT, target.add(0, .8, 0), 8, .35, .3, .35, .08);
    }

    private static void filler(ServerLevel level,String id,String skillId,Vec3 source,Vec3 target,boolean damaging){switch(id){case"F01"->{line(level,ParticleTypes.CRIT,source.add(0,1,0),target.add(0,.95,0),7);slashArc(level,ParticleTypes.CLOUD,target.add(0,.9,0),.62,9);}case"F02"->{ring(level,ParticleTypes.END_ROD,target.add(0,.25,0),.58,12);burst(level,ParticleTypes.HEART,target.add(0,.95,0),5,.3,.45,.3,.02);}case"F03"->{boolean focus="f03_focus_shot".equals(skillId);line(level,focus?ParticleTypes.END_ROD:ParticleTypes.CRIT,source.add(0,1.3,0),target.add(0,1,0),focus?18:12);burst(level,ParticleTypes.CRIT,target.add(0,1,0),focus?10:5,focus?.38:.22,.25,focus?.38:.22,.06);}case"F04"->{if("f04_endure".equals(skillId)){ring(level,ParticleTypes.CLOUD,source.add(0,.25,0),.78,16);ring(level,ParticleTypes.END_ROD,source.add(0,.95,0),.62,12);}else{line(level,ParticleTypes.CRIT,source.add(0,1,0),target.add(0,.9,0),7);burst(level,ParticleTypes.CLOUD,target.add(0,.65,0),10,.5,.25,.5,.07);}}default->generic(level,source,target,damaging);}}

    private static void enemy(ServerLevel level,String id,String skillId,Vec3 source,Vec3 target,boolean damaging){switch(id){
        case"E001"->{burst(level,ParticleTypes.SMOKE,source.add(0,.8,0),7,.4,.35,.4,.01);slashArc(level,ParticleTypes.CRIT,target.add(0,.9,0),.62,9);}case"E002"->{line(level,ParticleTypes.CRIT,source.add(0,1.55,0),target.add(0,1.05,0),"e002_aimed".equals(skillId)?22:15);burst(level,ParticleTypes.END_ROD,target.add(0,1.05,0),"e002_aimed".equals(skillId)?9:4,.22,.22,.22,.04);}case"E003"->{if("e003_arm".equals(skillId)){ring(level,ParticleTypes.FLAME,source.add(0,.65,0),.82,18);burst(level,ParticleTypes.SMOKE,source.add(0,1,0),15,.55,.65,.55,.03);}else if("e003_explode".equals(skillId)){ring(level,ParticleTypes.FLAME,source.add(0,.15,0),1.8,30);burst(level,ParticleTypes.FLAME,source.add(0,.9,0),34,1.2,.9,1.2,.12);burst(level,ParticleTypes.SMOKE,source.add(0,1,0),26,1.35,.9,1.35,.08);}else{line(level,ParticleTypes.SMOKE,source.add(0,.9,0),target.add(0,.9,0),7);burst(level,ParticleTypes.CRIT,target.add(0,.8,0),7,.35,.25,.35,.06);}}case"E004"->{line(level,ParticleTypes.CRIT,source.add(0,1.1,0),target.add(0,1,0),8);slashArc(level,"e004_stab".equals(skillId)?ParticleTypes.END_ROD:ParticleTypes.CRIT,target.add(0,1,0),"e004_stab".equals(skillId)?.95:.72,13);}case"E005"->{ring(level,ParticleTypes.END_ROD,target.add(0,.35,0),"e005_reform".equals(skillId)?1.15:.65,16);burst(level,"e005_reform".equals(skillId)?ParticleTypes.ENCHANT:ParticleTypes.HEART,target.add(0,1,0),"e005_reform".equals(skillId)?16:7,.55,.6,.55,.035);}case"E006"->{ring(level,ParticleTypes.CLOUD,source.add(0,.15,0),.8,14);line(level,ParticleTypes.CRIT,source.add(0,.7,0),target.add(0,.75,0),"e006_charge".equals(skillId)?15:8);burst(level,ParticleTypes.CLOUD,target.add(0,.35,0),10,.65,.25,.65,.06);}case"E007"->{ring(level,ParticleTypes.ENCHANT,source.add(0,.8,0),.72,15);if("e007_slow_spores".equals(skillId)){ring(level,ParticleTypes.SOUL,target.add(0,.35,0),1.25,22);burst(level,ParticleTypes.ENCHANT,target.add(0,1,0),14,.85,.65,.85,.025);}else line(level,ParticleTypes.ENCHANT,source.add(0,1.2,0),target.add(0,1,0),10);}case"E008"->{if("e008_barrier".equals(skillId)){ring(level,ParticleTypes.END_ROD,target.add(0,.45,0),.95,22);ring(level,ParticleTypes.ENCHANT,target.add(0,1,0),.72,16);}else{burst(level,ParticleTypes.CLOUD,target.add(0,.35,0),10,.55,.2,.55,.025);slashArc(level,ParticleTypes.CRIT,target.add(0,.9,0),.82,11);}}case"E009"->{line(level,ParticleTypes.ELECTRIC_SPARK,source.add(0,1.25,0),target.add(0,1,0),damaging?10:7);ring(level,ParticleTypes.END_ROD,target.add(0,.3,0),"e009_delay".equals(skillId)?.95:.55,13);}case"E010"->{line(level,ParticleTypes.SOUL,source.add(0,.65,0),target.add(0,.75,0),9);burst(level,ParticleTypes.SMOKE,target.add(0,.7,0),"e010_flood_rot".equals(skillId)?15:7,.48,.35,.48,.02);}case"E011"->{ring(level,ParticleTypes.ELECTRIC_SPARK,source.add(0,.85,0),.72,15);if("e011_support".equals(skillId))burst(level,ParticleTypes.ELECTRIC_SPARK,target.add(0,1,0),12,.5,.55,.5,.08);else burst(level,ParticleTypes.CRIT,target.add(0,.95,0),6,.3,.3,.3,.05);}case"E012"->{burst(level,ParticleTypes.ASH,source.add(0,.65,0),10,.5,.25,.5,.02);line(level,ParticleTypes.CRIT,source.add(0,.7,0),target.add(0,.8,0),"e012_pounce".equals(skillId)?14:8);slashArc(level,ParticleTypes.ASH,target.add(0,.8,0),.75,10);}case"E013"->{line(level,ParticleTypes.FLAME,source.add(0,1.25,0),target.add(0,1,0),11);if("e013_embers".equals(skillId)){ring(level,ParticleTypes.FLAME,target.add(0,.3,0),1.25,24);burst(level,ParticleTypes.ASH,target.add(0,1,0),18,.85,.65,.85,.04);}else burst(level,ParticleTypes.FLAME,target.add(0,1,0),8,.35,.35,.35,.07);}case"E014"->{burst(level,ParticleTypes.SMOKE,source.add(0,.8,0),9,.45,.45,.45,.025);line(level,ParticleTypes.FLAME,source.add(0,1,0),target.add(0,.85,0),"e014_crush".equals(skillId)?13:8);burst(level,ParticleTypes.CRIT,target.add(0,.55,0),"e014_crush".equals(skillId)?18:9,.7,.35,.7,.09);}default->generic(level,source,target,damaging);}}

    private static void elite(ServerLevel level,String id,String skillId,Vec3 source,Vec3 target,boolean damaging){switch(id){case"EL01"->{ring(level,ParticleTypes.SMOKE,source.add(0,.35,0),1.05,20);if("el01_command".equals(skillId)){ring(level,ParticleTypes.CRIT,source.add(0,1.1,0),1.35,26);burst(level,ParticleTypes.END_ROD,source.add(0,1.25,0),12,.8,.55,.8,.05);}else{line(level,ParticleTypes.CRIT,source.add(0,1.2,0),target.add(0,1,0),11);slashArc(level,ParticleTypes.CRIT,target.add(0,1,0),1,15);}}case"EL02"->{ring(level,ParticleTypes.ENCHANT,source.add(0,.3,0),1,20);line(level,ParticleTypes.CRIT,source.add(0,.95,0),target.add(0,.9,0),"el02_piercing_horn".equals(skillId)?18:11);burst(level,ParticleTypes.END_ROD,target.add(0,.95,0),"el02_piercing_horn".equals(skillId)?13:7,.45,.4,.45,.08);}case"EL03"->{ring(level,ParticleTypes.ELECTRIC_SPARK,source.add(0,.35,0),1.05,20);if("el03_barrier".equals(skillId)){ring(level,ParticleTypes.END_ROD,source.add(0,.85,0),1.2,26);ring(level,ParticleTypes.ELECTRIC_SPARK,source.add(0,1.25,0),.82,18);}else slashArc(level,ParticleTypes.CRIT,target.add(0,1,0),1,14);}case"EL04"->{ring(level,ParticleTypes.FLAME,source.add(0,.2,0),1.25,24);burst(level,ParticleTypes.ASH,source.add(0,1,0),14,.75,.75,.75,.025);if("el04_collapse".equals(skillId)){ring(level,ParticleTypes.FLAME,target.add(0,.25,0),1.6,30);burst(level,ParticleTypes.CRIT,target.add(0,.65,0),22,1,.45,1,.12);}else{line(level,ParticleTypes.FLAME,source.add(0,1,0),target.add(0,.9,0),12);burst(level,ParticleTypes.CRIT,target.add(0,.75,0),12,.55,.4,.55,.08);}}default->generic(level,source,target,damaging);}}

    private static void bossGraoul(ServerLevel l,Vec3 s,Vec3 t,String id){ring(l,ParticleTypes.CLOUD,s.add(0,.15,0),"b01_charge".equals(id)?1.7:1,24);line(l,ParticleTypes.CRIT,s.add(0,.9,0),t.add(0,.9,0),14);}
    private static void bossVerna(ServerLevel l,Vec3 s,Vec3 t,String id){ring(l,ParticleTypes.ENCHANT,s.add(0,.35,0),1.45,26);if("b02_summon".equals(id))burst(l,ParticleTypes.PORTAL,s.add(0,1,0),28,1.1,.8,1.1,.12);else burst(l,ParticleTypes.CRIT,t.add(0,1,0),14,.7,.6,.7,.08);}
    private static void bossOro(ServerLevel l,Vec3 s,Vec3 t,String id){ring(l,ParticleTypes.ELECTRIC_SPARK,s.add(0,.5,0),1.3,24);if("b03_overclock".equals(id)){ring(l,ParticleTypes.END_ROD,s.add(0,1.25,0),1.65,30);burst(l,ParticleTypes.ELECTRIC_SPARK,s.add(0,1.1,0),24,1,.8,1,.18);}else line(l,ParticleTypes.ELECTRIC_SPARK,s.add(0,1.1,0),t.add(0,1,0),10);}
    private static void bossKolvak(ServerLevel l,Vec3 s,Vec3 t,String id){ring(l,ParticleTypes.FLAME,s.add(0,.2,0),1.5,28);burst(l,ParticleTypes.ASH,s.add(0,1.1,0),22,1,.9,1,.03);if("b04_eruption".equals(id))burst(l,ParticleTypes.FLAME,t.add(0,.8,0),30,1.2,.8,1.2,.12);}
    private static void bossSerak(ServerLevel l,Vec3 s,Vec3 t,String id){ring(l,ParticleTypes.PORTAL,s.add(0,.35,0),1.45,30);line(l,ParticleTypes.SOUL,s.add(0,1.3,0),t.add(0,1,0),12);if("b05_relay_collapse".equals(id)){ring(l,ParticleTypes.END_ROD,s.add(0,1,0),2,36);burst(l,ParticleTypes.PORTAL,s.add(0,1,0),36,1.3,1.1,1.3,.18);}}
    private static void generic(ServerLevel l,Vec3 s,Vec3 t,boolean damaging){ParticleOptions type=damaging?ParticleTypes.CRIT:ParticleTypes.ENCHANT;line(l,type,s.add(0,1,0),t.add(0,1,0),damaging?8:5);}
    private static void line(ServerLevel l,ParticleOptions p,Vec3 from,Vec3 to,int steps){if(steps<=0)return;Vec3 d=to.subtract(from);for(int i=0;i<=steps;i++){double q=i/(double)steps;Vec3 v=from.add(d.scale(q));l.sendParticles(p,v.x,v.y,v.z,1,.02,.02,.02,0);}}
    private static void ring(ServerLevel l,ParticleOptions p,Vec3 c,double r,int count){for(int i=0;i<count;i++){double a=Math.PI*2*i/count;double x=c.x+Math.cos(a)*r,z=c.z+Math.sin(a)*r;l.sendParticles(p,x,c.y,z,1,.01,.01,.01,0);}}
    private static void slashArc(ServerLevel l,ParticleOptions p,Vec3 c,double r,int count){for(int i=0;i<count;i++){double q=i/(double)Math.max(1,count-1),a=-1.2+q*2.4;double x=c.x+Math.cos(a)*r,y=c.y+(q-.5)*1.2,z=c.z+Math.sin(a)*r;l.sendParticles(p,x,y,z,1,.01,.01,.01,0);}}
    private static void burst(ServerLevel l,ParticleOptions p,Vec3 c,int count,double dx,double dy,double dz,double speed){l.sendParticles(p,c.x,c.y,c.z,count,dx,dy,dz,speed);}
}
