package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/** Synchronized procedural-mesh presentation for persistent boss identity and fixed cast telegraphs. */
public final class VillageBossEffectSystem {
    private VillageBossEffectSystem() {}

    public static void presence(
            ServerLevel level,
            Mob boss,
            VillageBossAspectSystem.Aspect aspect,
            VillageSiegeBossSystem.BossDoctrine doctrine) {
        if (level == null || boss == null || aspect == null || doctrine == null) return;
        VillageSkillEffectEntity.spawn(level, boss,
                "boss_presence_" + doctrine.name().toLowerCase(Locale.ROOT),
                boss.position(), horizontal(boss.getLookAngle()), 20 * 60 * 30, 0.0f,
                aspect.name().toLowerCase(Locale.ROOT));
    }

    public static void phaseTwo(
            ServerLevel level,
            Mob boss,
            VillageSiegeBossSystem.BossDoctrine doctrine) {
        if (level == null || boss == null || doctrine == null) return;
        VillageSkillEffectEntity.spawn(level, boss,
                "boss_phase_two_" + doctrine.name().toLowerCase(Locale.ROOT),
                boss.position(), horizontal(boss.getLookAngle()), 20 * 60 * 30, 0.0f, "");
        VillageSkillEffectEntity.spawn(level, null, "boss_phase_two_burst", boss.position(),
                new Vec3(0.0, 0.0, 1.0), 28, 0.0f, "5.0");
    }

    public static void breachWarning(ServerLevel level, Mob boss, Vec3 impact, int duration) {
        if (level == null || boss == null || impact == null) return;
        Vec3 start = boss.position().add(0.0, Math.max(1.0, boss.getBbHeight() * 0.55), 0.0);
        VillageSkillEffectEntity.spawn(level, null, "boss_breach_warning", impact,
                new Vec3(0.0, 0.0, 1.0), Math.max(8, duration), 0.0f, "3.4");
        VillageSkillEffectEntity.spawn(level, boss, "boss_breach_windup", start,
                normalized(impact.subtract(start)), Math.max(8, duration), 0.0f, encode(start, impact));
    }

    public static void breachImpact(ServerLevel level, Vec3 impact, double radius) {
        if (level == null || impact == null) return;
        pulse(level, "boss_breach_impact", impact, radius, 24);
    }

    public static void ritualWarning(ServerLevel level, Vec3 center, double radius, int duration) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "boss_ritual_warning", center,
                new Vec3(0.0, 0.0, 1.0), Math.max(10, duration), 0.0f,
                String.format(Locale.ROOT, "%.2f", radius));
    }

    public static void ritualImpact(ServerLevel level, Vec3 center, double radius) {
        if (level == null || center == null) return;
        pulse(level, "boss_ritual_impact", center, radius, 30);
    }

    public static void duelMark(ServerLevel level, LivingEntity target, int duration) {
        if (level == null || target == null) return;
        VillageSkillEffectEntity.spawn(level, target, "boss_duel_mark", target.position(),
                horizontal(target.getLookAngle()), Math.max(12, duration), 0.0f, "");
    }

    public static void duelImpact(ServerLevel level, LivingEntity target) {
        if (level == null || target == null) return;
        pulse(level, "boss_duel_impact", target.position(), 2.2, 18);
    }

    public static void bloodboundWarning(ServerLevel level, Vec3 center, double radius, int duration) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "boss_bloodbound_warning", center,
                new Vec3(0.0, 0.0, 1.0), Math.max(10, duration), 0.0f,
                String.format(Locale.ROOT, "%.2f", radius));
    }

    public static void bloodboundImpact(ServerLevel level, Vec3 center, double radius) {
        if (level == null || center == null) return;
        pulse(level, "boss_bloodbound_impact", center, radius, 24);
    }

    public static void stormWarning(ServerLevel level, Vec3 center, double radius, int duration) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "boss_storm_warning", center,
                new Vec3(0.0, 0.0, 1.0), Math.max(10, duration), 0.0f,
                String.format(Locale.ROOT, "%.2f", radius));
    }

    private static void pulse(ServerLevel level, String kind, Vec3 center, double radius, int duration) {
        VillageSkillEffectEntity.spawn(level, null, kind, center,
                new Vec3(0.0, 0.0, 1.0), duration, 0.0f,
                String.format(Locale.ROOT, "%.2f", Math.max(0.5, radius)));
    }

    private static String encode(Vec3 start, Vec3 end) {
        return String.format(Locale.ROOT, "%.3f,%.3f,%.3f;%.3f,%.3f,%.3f",
                start.x, start.y, start.z, end.x, end.y, end.z);
    }

    private static Vec3 horizontal(Vec3 value) {
        Vec3 source = value == null ? Vec3.ZERO : new Vec3(value.x, 0.0, value.z);
        return source.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : source.normalize();
    }

    private static Vec3 normalized(Vec3 value) {
        Vec3 source = value == null ? Vec3.ZERO : value;
        return source.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : source.normalize();
    }
}
