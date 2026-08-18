package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/** Synchronized procedural-mesh scenes for elite doctrine identity and telegraphed abilities. */
public final class VillageEnemyEffectSystem {
    private VillageEnemyEffectSystem() {}

    public static void eliteAura(
            ServerLevel level,
            Mob mob,
            VillageEnemyEliteSystem.EliteDoctrine doctrine) {
        if (level == null || mob == null || doctrine == null) return;
        VillageSkillEffectEntity.spawn(level, mob,
                "elite_aura_" + doctrine.name().toLowerCase(Locale.ROOT),
                mob.position(), horizontal(mob.getLookAngle()), 20 * 60 * 30, 0.0f, "");
    }

    public static void grappleLine(ServerLevel level, Mob mob, Vec3 start, Vec3 end, int duration) {
        if (level == null || mob == null || start == null || end == null) return;
        VillageSkillEffectEntity.spawn(level, mob, "elite_grapple_line", start,
                normalized(end.subtract(start)), Math.max(8, duration), 0.0f, encode(List.of(start, end)));
    }

    public static void firebrandThrow(ServerLevel level, Mob mob, Vec3 impact, int duration) {
        if (level == null || mob == null || impact == null) return;
        Vec3 start = mob.position().add(0.0, Math.max(1.0, mob.getBbHeight() * 0.65), 0.0);
        VillageSkillEffectEntity.spawn(level, mob, "elite_firebrand_throw", start,
                normalized(impact.subtract(start)), Math.max(8, duration), 0.0f, encode(List.of(start, impact)));
    }

    public static void firebrandImpact(ServerLevel level, Vec3 impact, double radius) {
        if (level == null || impact == null) return;
        VillageSkillEffectEntity.spawn(level, null, "elite_firebrand_impact", impact,
                new Vec3(0.0, 0.0, 1.0), 18, 0.0f,
                String.format(Locale.ROOT, "%.2f", Math.max(1.0, radius)));
    }

    public static void plagueWarning(ServerLevel level, Mob mob, Vec3 center, double radius, int duration) {
        if (level == null || mob == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, mob, "elite_plague_warning", center,
                new Vec3(0.0, 0.0, 1.0), Math.max(10, duration), 0.0f,
                String.format(Locale.ROOT, "%.2f", Math.max(1.0, radius)));
    }

    public static void plagueImpact(ServerLevel level, Vec3 center, double radius) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "elite_plague_impact", center,
                new Vec3(0.0, 0.0, 1.0), 24, 0.0f,
                String.format(Locale.ROOT, "%.2f", Math.max(1.0, radius)));
    }

    private static Vec3 horizontal(Vec3 value) {
        Vec3 source = value == null ? Vec3.ZERO : new Vec3(value.x, 0.0, value.z);
        return source.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : source.normalize();
    }

    private static Vec3 normalized(Vec3 value) {
        Vec3 source = value == null ? Vec3.ZERO : value;
        return source.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : source.normalize();
    }

    private static String encode(List<Vec3> points) {
        StringBuilder result = new StringBuilder();
        for (Vec3 point : points) {
            if (result.length() > 0) result.append(';');
            result.append(String.format(Locale.ROOT, "%.3f,%.3f,%.3f", point.x, point.y, point.z));
        }
        return result.toString();
    }
}
