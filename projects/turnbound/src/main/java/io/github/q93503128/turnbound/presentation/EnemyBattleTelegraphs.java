package io.github.q93503128.turnbound.presentation;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Readable pre-impact accents for normal-enemy and elite special skills.
 *
 * <p>These are deliberately restrained: the existing BattleVfx class owns the actual impact language, while this
 * class gives important specials a recognizable wind-up shape so the 14 normal enemies and four elites do not read
 * as the same strike/cast with different models.</p>
 */
public final class EnemyBattleTelegraphs {
    private EnemyBattleTelegraphs() { }

    public static void present(ServerLevel level, String visualId, String skillId, Vec3 source, Vec3 target) {
        if (level == null || skillId == null || source == null) return;
        Vec3 aim = target == null ? source : target;
        switch (skillId) {
            case "e002_aimed" -> aimedShot(level, source, aim);
            case "e003_arm" -> bursterArm(level, source);
            case "e003_explode" -> bursterRelease(level, source);
            case "e004_stab" -> stabLane(level, source, aim);
            case "e005_reform" -> medicReform(level, aim);
            case "e006_charge" -> chargeLane(level, source, aim, ParticleTypes.CLOUD, ParticleTypes.CRIT);
            case "e007_slow_spores" -> sporeField(level, aim);
            case "e008_barrier" -> rootBarrier(level, aim);
            case "e009_delay" -> aqueductDelay(level, aim);
            case "e010_flood_rot" -> floodRot(level, aim);
            case "e011_support" -> supportLink(level, source, aim);
            case "e012_pounce" -> pounceLanding(level, source, aim);
            case "e013_embers" -> emberField(level, aim);
            case "e014_crush" -> drillCrush(level, aim);
            case "el01_command" -> captainCommand(level, source);
            case "el02_piercing_horn" -> hornLane(level, source, aim);
            case "el03_barrier" -> centurionGuard(level, source);
            case "el04_collapse" -> EliteVfxTelegraphs.el04CollapseCracks(level, aim);
            default -> { }
        }
    }

    private static void aimedShot(ServerLevel level, Vec3 source, Vec3 target) {
        Vec3 from = source.add(0, 1.45, 0);
        Vec3 to = target.add(0, 1.0, 0);
        dottedLine(level, ParticleTypes.END_ROD, from, to, 12, 2);
        ring(level, ParticleTypes.CRIT, target.add(0, .12, 0), .38, 10);
    }

    private static void bursterArm(ServerLevel level, Vec3 source) {
        Vec3 core = source.add(0, .75, 0);
        ring(level, ParticleTypes.FLAME, source.add(0, .1, 0), .55, 14);
        ring(level, ParticleTypes.FLAME, source.add(0, .18, 0), .92, 18);
        level.sendParticles(ParticleTypes.SMOKE, core.x, core.y, core.z, 10, .35, .45, .35, .015);
    }

    private static void bursterRelease(ServerLevel level, Vec3 source) {
        radial(level, ParticleTypes.FLAME, source.add(0, .2, 0), 8, 1.65, 8);
        ring(level, ParticleTypes.SMOKE, source.add(0, .25, 0), 1.45, 24);
    }

    private static void stabLane(ServerLevel level, Vec3 source, Vec3 target) {
        lane(level, source, target, .28, ParticleTypes.CRIT, 8);
        ring(level, ParticleTypes.END_ROD, target.add(0, .15, 0), .34, 9);
    }

    private static void medicReform(ServerLevel level, Vec3 target) {
        ring(level, ParticleTypes.END_ROD, target.add(0, .18, 0), .65, 14);
        ring(level, ParticleTypes.ENCHANT, target.add(0, .65, 0), .47, 12);
        level.sendParticles(ParticleTypes.END_ROD, target.x, target.y + 1.0, target.z, 6, .28, .45, .28, .02);
    }

    private static void sporeField(ServerLevel level, Vec3 target) {
        ring(level, ParticleTypes.ENCHANT, target.add(0, .12, 0), 1.15, 22);
        ring(level, ParticleTypes.SOUL, target.add(0, .18, 0), 1.55, 26);
        level.sendParticles(ParticleTypes.ENCHANT, target.x, target.y + .7, target.z, 10, .85, .35, .85, .01);
    }

    private static void rootBarrier(ServerLevel level, Vec3 target) {
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2.0 * i / 8.0;
            double x = target.x + Math.cos(a) * .8;
            double z = target.z + Math.sin(a) * .8;
            vertical(level, ParticleTypes.ENCHANT, new Vec3(x, target.y + .1, z), 1.35, 5);
        }
        ring(level, ParticleTypes.END_ROD, target.add(0, .15, 0), .85, 18);
    }

    private static void aqueductDelay(ServerLevel level, Vec3 target) {
        ring(level, ParticleTypes.ELECTRIC_SPARK, target.add(0, .15, 0), .82, 18);
        ring(level, ParticleTypes.END_ROD, target.add(0, .18, 0), 1.15, 22);
        radial(level, ParticleTypes.ELECTRIC_SPARK, target.add(0, .15, 0), 6, 1.1, 6);
    }

    private static void floodRot(ServerLevel level, Vec3 target) {
        ring(level, ParticleTypes.SOUL, target.add(0, .12, 0), .92, 18);
        ring(level, ParticleTypes.SMOKE, target.add(0, .08, 0), 1.25, 22);
        level.sendParticles(ParticleTypes.SOUL, target.x, target.y + .6, target.z, 7, .55, .25, .55, .01);
    }

    private static void supportLink(ServerLevel level, Vec3 source, Vec3 target) {
        dottedLine(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, 1.15, 0), target.add(0, 1.0, 0), 12, 1);
        ring(level, ParticleTypes.END_ROD, target.add(0, .2, 0), .58, 13);
    }

    private static void pounceLanding(ServerLevel level, Vec3 source, Vec3 target) {
        lane(level, source, target, .38, ParticleTypes.ASH, 7);
        slash(level, ParticleTypes.CRIT, target.add(0, .7, 0), .62, -0.9);
        slash(level, ParticleTypes.CRIT, target.add(0, .7, 0), .62, 0.9);
    }

    private static void emberField(ServerLevel level, Vec3 target) {
        ring(level, ParticleTypes.SMALL_FLAME, target.add(0, .12, 0), .92, 20);
        ring(level, ParticleTypes.ASH, target.add(0, .14, 0), 1.35, 24);
        vertical(level, ParticleTypes.SMALL_FLAME, target.add(0, .1, 0), 1.25, 7);
    }

    private static void drillCrush(ServerLevel level, Vec3 target) {
        radial(level, ParticleTypes.CRIT, target.add(0, .08, 0), 6, 1.45, 7);
        ring(level, ParticleTypes.ASH, target.add(0, .12, 0), 1.2, 20);
        level.sendParticles(ParticleTypes.SMOKE, target.x, target.y + .35, target.z, 9, .65, .18, .65, .02);
    }

    private static void captainCommand(ServerLevel level, Vec3 source) {
        Vec3 center = source.add(0, .15, 0);
        ring(level, ParticleTypes.CRIT, center, 1.05, 18);
        radial(level, ParticleTypes.END_ROD, center, 8, 1.25, 5);
        vertical(level, ParticleTypes.END_ROD, source.add(0, .3, 0), 1.35, 6);
    }

    private static void hornLane(ServerLevel level, Vec3 source, Vec3 target) {
        chargeLane(level, source, target, ParticleTypes.ENCHANT, ParticleTypes.END_ROD);
        ring(level, ParticleTypes.CRIT, target.add(0, .12, 0), .46, 11);
    }

    private static void centurionGuard(ServerLevel level, Vec3 source) {
        ring(level, ParticleTypes.ELECTRIC_SPARK, source.add(0, .18, 0), .78, 18);
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2.0 * i / 6.0;
            Vec3 p = source.add(Math.cos(a) * .7, .15, Math.sin(a) * .7);
            vertical(level, ParticleTypes.END_ROD, p, 1.2, 5);
        }
    }

    private static void chargeLane(ServerLevel level, Vec3 source, Vec3 target, ParticleOptions edge, ParticleOptions center) {
        lane(level, source, target, .42, edge, 9);
        dottedLine(level, center, source.add(0, .15, 0), target.add(0, .15, 0), 10, 1);
    }

    private static void lane(ServerLevel level, Vec3 source, Vec3 target, double halfWidth, ParticleOptions particle, int steps) {
        Vec3 delta = target.subtract(source);
        Vec3 flat = new Vec3(delta.x, 0, delta.z);
        if (flat.lengthSqr() < .0001) return;
        Vec3 side = new Vec3(-flat.z, 0, flat.x).normalize().scale(halfWidth);
        dottedLine(level, particle, source.add(side).add(0, .08, 0), target.add(side).add(0, .08, 0), steps, 1);
        dottedLine(level, particle, source.subtract(side).add(0, .08, 0), target.subtract(side).add(0, .08, 0), steps, 1);
    }

    private static void dottedLine(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, int steps, int stride) {
        if (steps <= 0) return;
        Vec3 delta = to.subtract(from);
        for (int i = 0; i <= steps; i++) {
            if (stride > 1 && i % stride != 0) continue;
            Vec3 p = from.add(delta.scale(i / (double)steps));
            level.sendParticles(particle, p.x, p.y, p.z, 1, .01, .01, .01, 0);
        }
    }

    private static void radial(ServerLevel level, ParticleOptions particle, Vec3 center, int rays, double radius, int steps) {
        for (int i = 0; i < rays; i++) {
            double a = Math.PI * 2.0 * i / rays;
            Vec3 end = center.add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            dottedLine(level, particle, center, end, steps, 1);
        }
    }

    private static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2.0 * i / count;
            double x = center.x + Math.cos(a) * radius;
            double z = center.z + Math.sin(a) * radius;
            level.sendParticles(particle, x, center.y, z, 1, .01, .01, .01, 0);
        }
    }

    private static void vertical(ServerLevel level, ParticleOptions particle, Vec3 base, double height, int steps) {
        for (int i = 0; i <= steps; i++) {
            Vec3 p = base.add(0, height * i / Math.max(1.0, steps), 0);
            level.sendParticles(particle, p.x, p.y, p.z, 1, .01, .01, .01, 0);
        }
    }

    private static void slash(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, double yawBias) {
        int count = 9;
        for (int i = 0; i < count; i++) {
            double t = i / (double)(count - 1);
            double a = yawBias - .75 + t * 1.5;
            Vec3 p = center.add(Math.cos(a) * radius, (t - .5) * .75, Math.sin(a) * radius);
            level.sendParticles(particle, p.x, p.y, p.z, 1, .01, .01, .01, 0);
        }
    }
}
