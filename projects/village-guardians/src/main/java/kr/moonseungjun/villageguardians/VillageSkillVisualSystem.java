package kr.moonseungjun.villageguardians;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Readable combat telegraphs. Every role has a stable visual language: melee
 * rings, arrow trails, elemental geometry, healing columns, and guard waves.
 */
public final class VillageSkillVisualSystem {
    private VillageSkillVisualSystem() {}

    public static void render(ServerPlayer player, VillageRoleSkillSystem.ActiveSkill skill) {
        if (player == null || skill == null || !(player.level() instanceof ServerLevel level)) return;
        switch (skill) {
            case VANGUARD_WHIRLWIND -> {
                ring(level, player.position().add(0, 0.25, 0), 4.8, ParticleTypes.SWEEP_ATTACK, 18);
                ring(level, player.position().add(0, 0.55, 0), 3.3, ParticleTypes.CRIT, 24);
            }
            case VANGUARD_BREAKER -> {
                forwardLane(level, player, 7.0, ParticleTypes.CRIT, 22);
                burst(level, player.position().add(player.getLookAngle().scale(3.0)).add(0, 0.6, 0),
                        ParticleTypes.EXPLOSION, 5);
            }
            case VANGUARD_CRY -> {
                ring(level, player.position().add(0, 0.3, 0), 4.0, ParticleTypes.CRIT, 20);
                ring(level, player.position().add(0, 1.0, 0), 7.0, ParticleTypes.CLOUD, 30);
                allyColumns(player, ParticleTypes.CRIT, 8);
            }
            case VANGUARD_STORM -> {
                ring(level, player.position().add(0, 0.35, 0), 7.5, ParticleTypes.SWEEP_ATTACK, 30);
                ring(level, player.position().add(0, 1.0, 0), 5.5, ParticleTypes.CRIT, 36);
                targetBursts(level, player, 8.5, 14, ParticleTypes.CRIT, 8);
            }

            case RANGER_VOLLEY -> targetTrails(level, player, 12.0, 5, ParticleTypes.CRIT, false);
            case RANGER_PIERCE -> {
                targetTrails(level, player, 15.0, 4, ParticleTypes.CRIT, true);
                forwardLane(level, player, 16.0, ParticleTypes.CRIT, 32);
            }
            case RANGER_RICOCHET -> chainTargets(level, player, 13.0, 9, ParticleTypes.ENCHANT);
            case RANGER_FIRE_RAIN -> {
                List<Mob> targets = targets(level, player, 14.0, 14);
                for (Mob target : targets) {
                    verticalRain(level, target.position().add(0, target.getBbHeight() * 0.5, 0), ParticleTypes.FLAME);
                    burst(level, target.position().add(0, 0.7, 0), ParticleTypes.FLAME, 12);
                }
            }

            case ARCANIST_FIRE_ORB -> {
                targetTrails(level, player, 9.0, 7, ParticleTypes.FLAME, false);
                targetBursts(level, player, 9.0, 7, ParticleTypes.EXPLOSION, 3);
            }
            case ARCANIST_FROST_RING -> {
                ring(level, player.position().add(0, 0.25, 0), 6.8, ParticleTypes.SNOWFLAKE, 42);
                targetBursts(level, player, 7.0, 10, ParticleTypes.SNOWFLAKE, 14);
            }
            case ARCANIST_CHAIN -> chainTargets(level, player, 12.0, 12, ParticleTypes.ENCHANT);
            case ARCANIST_NOVA -> {
                ring(level, player.position().add(0, 0.4, 0), 8.5, ParticleTypes.WITCH, 48);
                ring(level, player.position().add(0, 1.2, 0), 6.0, ParticleTypes.ENCHANT, 36);
                targetBursts(level, player, 9.0, 16, ParticleTypes.WITCH, 16);
            }

            case LUMINAR_HEAL -> {
                ring(level, player.position().add(0, 0.3, 0), 9.0, ParticleTypes.HEART, 24);
                allyColumns(player, ParticleTypes.HEART, 10);
            }
            case LUMINAR_CLEANSE -> {
                ring(level, player.position().add(0, 0.3, 0), 10.0, ParticleTypes.HAPPY_VILLAGER, 30);
                allyColumns(player, ParticleTypes.HAPPY_VILLAGER, 12);
            }
            case LUMINAR_VEIL -> {
                ring(level, player.position().add(0, 0.6, 0), 11.5, ParticleTypes.END_ROD, 38);
                allyColumns(player, ParticleTypes.END_ROD, 16);
            }
            case LUMINAR_SANCTUARY -> {
                ring(level, player.position().add(0, 0.25, 0), 13.5, ParticleTypes.END_ROD, 52);
                ring(level, player.position().add(0, 1.5, 0), 9.0, ParticleTypes.HEART, 34);
                allyColumns(player, ParticleTypes.HEART, 18);
            }

            case WARDEN_TAUNT -> {
                ring(level, player.position().add(0, 0.35, 0), 8.5, ParticleTypes.DAMAGE_INDICATOR, 34);
                targetBursts(level, player, 9.0, 14, ParticleTypes.CLOUD, 10);
            }
            case WARDEN_BASH -> {
                forwardLane(level, player, 5.5, ParticleTypes.DAMAGE_INDICATOR, 22);
                ring(level, player.position().add(player.getLookAngle().scale(2.0)).add(0, 0.3, 0),
                        3.0, ParticleTypes.CLOUD, 22);
            }
            case WARDEN_FORMATION -> {
                ring(level, player.position().add(0, 0.35, 0), 9.5, ParticleTypes.END_ROD, 30);
                allyColumns(player, ParticleTypes.DAMAGE_INDICATOR, 10);
            }
            case WARDEN_FIELD -> {
                ring(level, player.position().add(0, 0.35, 0), 13.5, ParticleTypes.END_ROD, 46);
                ring(level, player.position().add(0, 1.0, 0), 10.0, ParticleTypes.CLOUD, 38);
                allyColumns(player, ParticleTypes.END_ROD, 18);
            }
        }
    }

    private static List<Mob> targets(ServerLevel level, ServerPlayer player, double radius, int limit) {
        return VillageRaidSystem.activeEnemiesNear(level, player.position(), radius, limit, null);
    }

    private static void targetTrails(
            ServerLevel level,
            ServerPlayer player,
            double radius,
            int limit,
            ParticleOptions particle,
            boolean thick) {
        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.55));
        for (Mob target : targets(level, player, radius, limit)) {
            Vec3 end = target.position().add(0, target.getBbHeight() * 0.58, 0);
            line(level, start, end, particle, thick ? 2 : 1);
            burst(level, end, particle, thick ? 12 : 7);
        }
    }

    private static void chainTargets(
            ServerLevel level,
            ServerPlayer player,
            double radius,
            int limit,
            ParticleOptions particle) {
        Vec3 previous = player.getEyePosition().add(player.getLookAngle().scale(0.5));
        for (Mob target : targets(level, player, radius, limit)) {
            Vec3 end = target.position().add(0, target.getBbHeight() * 0.58, 0);
            line(level, previous, end, particle, 2);
            burst(level, end, particle, 10);
            previous = end;
        }
    }

    private static void targetBursts(
            ServerLevel level,
            ServerPlayer player,
            double radius,
            int limit,
            ParticleOptions particle,
            int count) {
        for (Mob target : targets(level, player, radius, limit)) {
            burst(level, target.position().add(0, target.getBbHeight() * 0.55, 0), particle, count);
        }
    }

    private static void allyColumns(ServerPlayer player, ParticleOptions particle, int heightSteps) {
        MinecraftServer server = player.level().getServer();
        if (server == null || !(player.level() instanceof ServerLevel level)) return;
        for (ServerPlayer ally : server.getPlayerList().getPlayers()) {
            if (ally.level() != player.level() || ally.distanceToSqr(player) > 14.5 * 14.5) continue;
            Vec3 base = ally.position().add(0, 0.2, 0);
            for (int step = 0; step < heightSteps; step++) {
                double angle = step * 0.85;
                Vec3 point = base.add(Math.cos(angle) * 0.5, step * 0.18, Math.sin(angle) * 0.5);
                level.sendParticles(particle, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    private static void forwardLane(
            ServerLevel level,
            ServerPlayer player,
            double length,
            ParticleOptions particle,
            int steps) {
        Vec3 direction = player.getLookAngle().multiply(1.0, 0.25, 1.0).normalize();
        Vec3 start = player.position().add(0, 0.75, 0);
        for (int step = 1; step <= steps; step++) {
            double distance = length * step / steps;
            Vec3 center = start.add(direction.scale(distance));
            Vec3 side = new Vec3(-direction.z, 0, direction.x).scale(0.55 + distance * 0.04);
            Vec3 left = center.add(side);
            Vec3 right = center.subtract(side);
            level.sendParticles(particle, left.x, left.y, left.z, 1, 0.02, 0.02, 0.02, 0.0);
            level.sendParticles(particle, right.x, right.y, right.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private static void verticalRain(ServerLevel level, Vec3 target, ParticleOptions particle) {
        for (int step = 0; step < 9; step++) {
            double angle = step * Math.PI * 2.0 / 9.0;
            Vec3 top = target.add(Math.cos(angle) * 2.0, 5.5 + (step % 3), Math.sin(angle) * 2.0);
            line(level, top, target, particle, 1);
        }
    }

    private static void ring(
            ServerLevel level,
            Vec3 center,
            double radius,
            ParticleOptions particle,
            int points) {
        for (int index = 0; index < Math.max(8, points); index++) {
            double angle = index * Math.PI * 2.0 / Math.max(8, points);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, center.y, z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private static void line(
            ServerLevel level,
            Vec3 from,
            Vec3 to,
            ParticleOptions particle,
            int thickness) {
        Vec3 delta = to.subtract(from);
        int steps = Math.max(5, Math.min(36, (int) Math.ceil(delta.length() * 1.5)));
        for (int step = 0; step <= steps; step++) {
            Vec3 point = from.add(delta.scale(step / (double) steps));
            level.sendParticles(particle, point.x, point.y, point.z,
                    Math.max(1, thickness), 0.025 * thickness, 0.025 * thickness, 0.025 * thickness, 0.0);
        }
    }

    private static void burst(ServerLevel level, Vec3 position, ParticleOptions particle, int count) {
        level.sendParticles(particle, position.x, position.y, position.z,
                Math.max(1, count), 0.35, 0.35, 0.35, 0.05);
    }
}
