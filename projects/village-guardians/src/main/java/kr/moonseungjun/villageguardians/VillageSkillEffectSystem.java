package kr.moonseungjun.villageguardians;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Server-side scene dispatcher for the custom mesh renderer.
 *
 * It only spawns synchronized VillageSkillEffectEntity actors. The client
 * renders those actors through original procedural vertex meshes.
 */
public final class VillageSkillEffectSystem {
    private VillageSkillEffectSystem() {}

    public static void reset() {
        // Effect actors are no-save and self-discard at the end of their duration.
    }

    public static void startCast(
            ServerLevel level,
            ServerPlayer player,
            VillageRoleSkillSystem.ActiveSkill skill,
            int calculatedDuration,
            Vec3 direction) {
        if (level == null || player == null || skill == null) return;
        Vec3 forward = horizontal(direction);
        Vec3 sight = normalized(direction);
        switch (skill) {
            case VANGUARD_WHIRLWIND -> {
                int duration = Math.max(48, calculatedDuration / 2);
                spawn(level, player, "vanguard_spin", player.position(), forward, duration, 0.0f, "");
                VillageNetwork.sendSkillMotion(level, player, "vanguard_spin", duration + 8);
            }
            case VANGUARD_BREAKER -> spawn(level, player, "vanguard_rally",
                    player.position(), forward, 60, 0.0f, "");
            case VANGUARD_CRY -> spawn(level, player, "vanguard_blade_charge",
                    player.position(), forward, 26, 0.0f, "");
            case VANGUARD_STORM -> spawn(level, player, "vanguard_slam_charge",
                    player.position(), forward, 44, 0.0f, "");

            case RANGER_VOLLEY -> spawn(level, player, "ranger_rapid",
                    player.position(), forward, 30, 0.0f, "");
            case RANGER_PIERCE -> spawn(level, player, "ranger_focus",
                    player.position(), forward, 28, 0.0f, "");
            case RANGER_RICOCHET -> spawn(level, player, "ranger_focus",
                    player.position(), forward, 28, 0.0f, "");
            case RANGER_FIRE_RAIN -> spawn(level, player, "ranger_energy_charge",
                    player.position(), forward, 36, 0.0f, "");

            case ARCANIST_FIRE_ORB, ARCANIST_FROST_RING, ARCANIST_CHAIN, ARCANIST_NOVA -> {
                // Spawned at the real raycast origin/target by VillageRoleAbilitySystem.
            }

            case LUMINAR_HEAL -> spawn(level, player, "luminar_heal_cast",
                    player.position(), forward, 32, 0.0f, "");
            case LUMINAR_CLEANSE -> spawn(level, player, "luminar_cleanse_cast",
                    player.position(), forward, 44, 0.0f, "");
            case LUMINAR_VEIL -> {
                // Radius-aware healing field is spawned by the gameplay system.
            }
            case LUMINAR_SANCTUARY -> spawn(level, player, "luminar_miracle_cast",
                    player.position(), forward, 72, 0.0f, "");

            case WARDEN_TAUNT -> {
                // Short charge shield is spawned with the actual dash.
            }
            case WARDEN_BASH -> spawn(level, player, "warden_taunt",
                    player.position(), forward, 48, 0.0f, "");
            case WARDEN_FORMATION -> spawn(level, player, "warden_fortress",
                    player.position(), forward, Math.max(120, calculatedDuration), 0.0f, "");
            case WARDEN_FIELD -> spawn(level, player, "warden_aegis",
                    player.position(), forward, Math.max(180, calculatedDuration * 2), 0.0f, "");
        }
    }

    public static void tick(MinecraftServer server) {
        // Custom effect entities own their lifetime and movement.
    }

    public static void bladeWave(ServerLevel level, ServerPlayer player, Vec3 direction) {
        Vec3 forward = horizontal(direction);
        spawn(level, player, "vanguard_blade_wave",
                player.position().add(0.0, 0.82, 0.0).add(forward.scale(1.0)),
                forward, 24, 1.75f, "");
    }

    public static void slamImpact(
            ServerLevel level, ServerPlayer player, double radius, int specialRank) {
        spawn(level, player, "vanguard_slam_impact",
                player.position(), horizontal(player.getLookAngle()), 30, 0.0f,
                meta(radius, specialRank));
    }

    public static void energyArrow(ServerLevel level, ServerPlayer player, Vec3 direction) {
        Vec3 sight = normalized(direction);
        energyArrow(level, player, player.getEyePosition().add(sight.scale(2.8)), sight);
    }

    public static void energyArrow(
            ServerLevel level, ServerPlayer player, Vec3 origin, Vec3 direction) {
        spawn(level, player, "ranger_energy_projectile",
                origin, normalized(direction), 55, 2.65f, "");
    }

    public static void arrowRainImpact(
            ServerLevel level, ServerPlayer player, Vec3 center,
            double radius, int specialRank) {
        spawn(level, player, "ranger_rain_impact",
                center, horizontal(player.getLookAngle()), 10, 0.0f,
                meta(radius, specialRank));
    }

    public static void shieldCharge(ServerLevel level, ServerPlayer player, Vec3 direction) {
        spawn(level, player, "warden_charge_cast",
                player.position(), horizontal(direction), 12, 0.0f, "");
    }

    public static void trackingReticle(
            ServerLevel level, ServerPlayer player, Vec3 target, Vec3 direction) {
        spawn(level, player, "ranger_lock", target, normalized(direction), 7, 0.0f, "");
    }

    public static void arrowRainField(
            ServerLevel level, ServerPlayer player, Vec3 center,
            int duration, double radius, int specialRank) {
        spawn(level, player, "ranger_rain_field", center, horizontal(player.getLookAngle()),
                Math.max(20, duration), 0.0f, meta(radius, specialRank));
    }


    public static VillageSkillEffectEntity fireOrb(
            ServerLevel level, ServerPlayer player, Vec3 origin, Vec3 direction,
            int duration, float speed, int specialRank) {
        return spawn(level, player, "arcanist_fire_orb", origin, normalized(direction),
                duration, speed, meta(0.0, specialRank));
    }

    public static void frostField(
            ServerLevel level, ServerPlayer player, Vec3 center,
            int duration, double radius, int specialRank) {
        spawn(level, player, "arcanist_frost", center, horizontal(player.getLookAngle()),
                duration, 0.0f, meta(radius, specialRank));
    }

    public static void tornadoField(
            ServerLevel level, ServerPlayer player, Vec3 center, Vec3 direction,
            int duration, double radius, int specialRank) {
        spawn(level, player, "arcanist_tornado", center, horizontal(direction),
                duration, 0.24f, meta(radius, specialRank));
    }

    public static void lightningField(
            ServerLevel level, ServerPlayer player, Vec3 center,
            int duration, double radius, int specialRank) {
        spawn(level, player, "arcanist_lightning", center, horizontal(player.getLookAngle()),
                duration, 0.0f, meta(radius, specialRank));
    }

    public static void healingField(
            ServerLevel level, ServerPlayer player, Vec3 center,
            int duration, double radius, int specialRank) {
        spawn(level, player, "luminar_healing_field", center, horizontal(player.getLookAngle()),
                duration, 0.0f, meta(radius, specialRank));
    }

    public static void fireImpact(
            ServerLevel level, ServerPlayer player, Vec3 center, double radius) {
        spawn(level, player, "arcanist_fire_impact", center, normalized(player.getLookAngle()),
                24, 0.0f, String.format(Locale.ROOT, "%.2f", Math.max(1.0, radius)));
    }

    public static void ricochet(
            ServerLevel level,
            ServerPlayer player,
            Mob primary,
            List<Mob> chained) {
        if (primary == null) return;
        List<Vec3> points = new ArrayList<>();
        points.add(player.getEyePosition());
        points.add(primary.getEyePosition());
        if (chained != null) {
            for (Mob target : chained) {
                if (target != null) points.add(target.getEyePosition());
            }
        }
        spawn(level, player, "ranger_ricochet_path", points.get(0),
                horizontal(points.get(1).subtract(points.get(0))),
                9 + points.size() * 3, 0.0f, encode(points));
    }

    public static void healLink(ServerLevel level, ServerPlayer caster, ServerPlayer target) {
        if (target == null) return;
        List<Vec3> points = List.of(caster.getEyePosition(), target.getEyePosition());
        spawn(level, caster, "luminar_heal_link", points.get(0),
                horizontal(points.get(1).subtract(points.get(0))), 32, 0.0f, encode(points));
    }

    public static void cleanse(ServerLevel level, ServerPlayer caster, List<ServerPlayer> allies) {
        List<Vec3> points = positions(allies, caster.position());
        spawn(level, caster, "luminar_cleanse_wave", caster.position(),
                horizontal(caster.getLookAngle()), 44, 0.0f, encode(points));
    }

    public static void miracle(ServerLevel level, ServerPlayer caster, List<ServerPlayer> allies) {
        List<Vec3> points = positions(allies, caster.position());
        spawn(level, caster, "luminar_miracle_wave", caster.position(),
                horizontal(caster.getLookAngle()), 72, 0.0f, encode(points));
    }

    private static VillageSkillEffectEntity spawn(
            ServerLevel level,
            ServerPlayer owner,
            String kind,
            Vec3 position,
            Vec3 direction,
            int duration,
            float speed,
            String extra) {
        return VillageSkillEffectEntity.spawn(
                level, owner, kind, position, direction, duration, speed, extra);
    }

    private static String meta(double radius, int specialRank) {
        return String.format(Locale.ROOT, "%.2f|%d",
                Math.max(0.0, radius), Math.max(0, specialRank));
    }

    private static List<Vec3> positions(List<ServerPlayer> players, Vec3 fallback) {
        List<Vec3> result = new ArrayList<>();
        if (players != null) {
            for (ServerPlayer player : players) {
                if (player != null) result.add(player.position());
            }
        }
        if (result.isEmpty()) result.add(fallback);
        return result;
    }

    private static String encode(List<Vec3> points) {
        StringBuilder result = new StringBuilder();
        for (Vec3 point : points) {
            if (result.length() > 0) result.append(';');
            result.append(String.format(Locale.ROOT, "%.3f,%.3f,%.3f",
                    point.x, point.y, point.z));
        }
        return result.toString();
    }

    private static Vec3 normalized(Vec3 value) {
        Vec3 source = value == null ? Vec3.ZERO : value;
        return source.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : source.normalize();
    }

    private static Vec3 horizontal(Vec3 value) {
        Vec3 source = value == null ? Vec3.ZERO : value;
        Vec3 result = new Vec3(source.x, 0.0, source.z);
        return result.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : result.normalize();
    }
}
