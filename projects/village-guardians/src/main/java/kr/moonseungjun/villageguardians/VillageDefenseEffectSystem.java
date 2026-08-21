package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Shared synchronized visual dispatcher for automated defenses and mercenaries.
 * Uses the same procedural mesh actor pipeline as player skills, with no owner required.
 */
public final class VillageDefenseEffectSystem {
    private VillageDefenseEffectSystem() {}

    public static void turretShot(
            ServerLevel level,
            VillagePlacedTurretSystem.TurretType type,
            Vec3 start,
            Vec3 end) {
        if (level == null || type == null || start == null || end == null) return;
        String kind = switch (type) {
            case BALLISTA -> "turret_ballista_shot";
            case REPEATER -> "turret_repeater_shot";
            case PIERCER -> "turret_piercer_shot";
            case FLAME -> "turret_flame_shot";
            case FROST -> "turret_frost_shot";
            case CHAIN -> "turret_chain_shot";
            case BOMBARD -> "turret_bombard_arc";
            case NULLIFIER -> "turret_nullifier_shot";
            case ANTI_AIR -> "turret_antiair_shot";
            case BEACON -> "turret_beacon_pulse";
        };
        int duration = switch (type) {
            case BOMBARD -> 12;
            case BALLISTA, PIERCER, ANTI_AIR -> 6;
            case REPEATER -> 4;
            case FLAME, FROST, CHAIN, NULLIFIER -> 7;
            case BEACON -> 18;
        };
        spawnLine(level, kind, start, end, duration);
    }

    public static void bombardImpact(ServerLevel level, Vec3 center, double radius) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "turret_bombard_impact", center,
                new Vec3(0.0, 0.0, 1.0), 16, 0.0f,
                String.format(Locale.ROOT, "%.2f", Math.max(1.0, radius)));
    }

    public static void beaconPulse(ServerLevel level, Vec3 center, double radius) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "turret_beacon_pulse", center,
                new Vec3(0.0, 0.0, 1.0), 22, 0.0f,
                String.format(Locale.ROOT, "%.2f", Math.max(1.0, radius)));
    }

    public static void mercenaryRangerShot(ServerLevel level, Vec3 start, Vec3 end) {
        spawnLine(level, "merc_ranger_shot", start, end, 6);
    }

    public static void mercenaryGuardPulse(ServerLevel level, Vec3 center, double radius) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "merc_bastion_guard", center,
                new Vec3(0.0, 0.0, 1.0), 16, 0.0f,
                String.format(Locale.ROOT, "%.2f", Math.max(1.0, radius)));
    }

    public static void mercenaryStrikerPressure(ServerLevel level, Vec3 start, Vec3 end) {
        spawnLine(level, "merc_striker_pressure", start, end, 8);
    }

    public static void mercenaryHealPulse(ServerLevel level, Vec3 center, double radius) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "merc_medic_pulse", center,
                new Vec3(0.0, 0.0, 1.0), 22, 0.0f,
                String.format(Locale.ROOT, "%.2f", Math.max(1.0, radius)));
    }

    public static void structureImpact(ServerLevel level, Vec3 center, boolean heavy) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "siege_structure_impact", center,
                new Vec3(0.0, 0.0, 1.0), heavy ? 18 : 12, 0.0f,
                heavy ? "2.4" : "1.5");
    }

    public static void turretPlacementPreview(
            ServerLevel level, Vec3 center, VillagePlacedTurretSystem.TurretType type) {
        if (level == null || center == null || type == null) return;
        VillageSkillEffectEntity.spawn(level, null, "turret_placement_preview", center,
                new Vec3(0.0, 0.0, 1.0), 18, 0.0f, Integer.toString(type.ordinal()));
    }

    public static void turretDeployPulse(
            ServerLevel level, Vec3 center, VillagePlacedTurretSystem.TurretType type) {
        if (level == null || center == null || type == null) return;
        VillageSkillEffectEntity.spawn(level, null, "turret_deploy_pulse", center,
                new Vec3(0.0, 0.0, 1.0), 26, 0.0f, Integer.toString(type.ordinal()));
    }

    public static void turretRepairPulse(ServerLevel level, Vec3 center) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "defense_repair_pulse", center,
                new Vec3(0.0, 0.0, 1.0), 24, 0.0f, "");
    }

    public static void turretUpgradePulse(ServerLevel level, Vec3 center, int levelValue) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "turret_upgrade_burst", center,
                new Vec3(0.0, 0.0, 1.0), 30, 0.0f, Integer.toString(Math.max(1, levelValue)));
    }

    public static void breachAlarm(ServerLevel level, Vec3 center) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "defense_breach_alarm", center,
                new Vec3(0.0, 0.0, 1.0), 34, 0.0f, "5.0");
    }

    public static void aerialAssaultWarning(ServerLevel level, Vec3 center, boolean structure) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "raid_aerial_warning", center,
                new Vec3(0.0, 0.0, 1.0), 18, 0.0f, structure ? "1" : "0");
    }

    public static void aerialAssaultImpact(ServerLevel level, Vec3 center, boolean structure) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "raid_aerial_impact", center,
                new Vec3(0.0, 0.0, 1.0), 16, 0.0f, structure ? "1" : "0");
    }

    public static void raidFrontWarning(ServerLevel level, Vec3 center, boolean mainFront) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "raid_front_warning", center,
                new Vec3(0.0, 0.0, 1.0), 44, 0.0f, mainFront ? "1" : "0");
    }

    public static void raidFrontArrival(ServerLevel level, Vec3 center, boolean mainFront) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "raid_front_arrival", center,
                new Vec3(0.0, 0.0, 1.0), 30, 0.0f, mainFront ? "1" : "0");
    }

    private static void spawnLine(ServerLevel level, String kind, Vec3 start, Vec3 end, int duration) {
        if (level == null || start == null || end == null) return;
        Vec3 delta = end.subtract(start);
        if (delta.lengthSqr() < 1.0E-6) return;
        VillageSkillEffectEntity.spawn(level, null, kind, start, delta.normalize(),
                Math.max(2, duration), 0.0f, encode(start, end));
    }

    private static String encode(Vec3 start, Vec3 end) {
        return String.format(Locale.ROOT, "%.3f,%.3f,%.3f;%.3f,%.3f,%.3f",
                start.x, start.y, start.z, end.x, end.y, end.z);
    }
}
