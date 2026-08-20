package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;

public final class VillageDefenseSystem {
    private static final String MERCENARY_NAME = "마을 용병";
    private static final EnumMap<VillageTowerSpecializationSystem.TowerKind, Integer> TOWER_TICKS =
            new EnumMap<>(VillageTowerSpecializationSystem.TowerKind.class);

    private VillageDefenseSystem() {}

    public static void reset() {
        TOWER_TICKS.clear();
        VillageTowerSpecializationSystem.resetTransientState();
    }

    public static boolean recognizeDefenseMob(Mob mob) {
        return VillageMercenarySystem.adoptLegacy(mob);
    }

    /** Compatibility facade: production hiring is owned by VillageMercenarySystem. */
    public static int mercenaryHireCost() {
        return VillageMercenarySystem.hireCost(VillageMercenarySystem.MercenaryClass.BASTION);
    }

    public static String hireMercenary(ServerPlayer player) {
        return VillageMercenarySystem.hire(player, VillageMercenarySystem.MercenaryClass.BASTION);
    }

    public static String status(ServerLevel level) {
        StringBuilder towers = new StringBuilder();
        for (VillageTowerSpecializationSystem.TowerKind kind : VillageTowerSpecializationSystem.TowerKind.values()) {
            if (!towers.isEmpty()) towers.append(" | ");
            towers.append(kind.displayName()).append(' ').append(VillageTowerSpecializationSystem.summary(kind));
        }
        return towers + " | " + VillageMercenarySystem.status(level.getServer());
    }

    public static void tick(MinecraftServer server) {
        VillageTowerSpecializationSystem.tick();
        if (!VillageRaidSystem.isActive()
                || !VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.WALLS)
                || VillageProgressionSystem.wallLevel() <= 0) {
            TOWER_TICKS.clear();
            return;
        }

        ServerLevel level = server.overworld();
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        int wall = VillageProgressionSystem.wallLevel();
        boolean warden = hasActiveWarden(server);
        float command = warden ? 1.25f : 1.0f;

        for (VillageTowerSpecializationSystem.TowerKind kind : VillageTowerSpecializationSystem.TowerKind.values()) {
            if (!VillageTowerSpecializationSystem.installed(kind)
                    || VillageTowerSpecializationSystem.disabled(kind)) continue;
            int ticks = TOWER_TICKS.getOrDefault(kind, 0) + 1;
            int interval = fireInterval(kind, wall, warden);
            if (ticks >= interval) {
                TOWER_TICKS.put(kind, 0);
                fire(level, center, kind, wall, command);
            } else {
                TOWER_TICKS.put(kind, ticks);
            }
        }
    }

    private static int fireInterval(
            VillageTowerSpecializationSystem.TowerKind kind,
            int wall,
            boolean warden) {
        int base = switch (kind) {
            case BALLISTA -> 46;
            case FLAME -> 42;
            case FROST -> 48;
            case ARCANE -> 52;
        };
        int interval = Math.max(13, base - wall * 4 - (warden ? 4 : 0));
        VillageTowerSpecializationSystem.Branch branch = VillageTowerSpecializationSystem.branch(kind);
        int rank = VillageTowerSpecializationSystem.rank(kind);
        if (branch == VillageTowerSpecializationSystem.Branch.ARCANE_OVERCHARGE) interval += 10 - rank * 2;
        if (branch == VillageTowerSpecializationSystem.Branch.BALLISTA_SPLIT) interval = Math.max(12, interval - rank * 2);
        return interval;
    }

    private static void fire(
            ServerLevel level,
            BlockPos center,
            VillageTowerSpecializationSystem.TowerKind kind,
            int wall,
            float command) {
        int radius = VillageWorldSystem.FORTRESS_RADIUS - 4;
        BlockPos tower = switch (kind) {
            case BALLISTA -> center.offset(radius, 18, -radius);
            case FLAME -> center.offset(-radius, 18, -radius);
            case FROST -> center.offset(radius, 18, radius);
            case ARCANE -> center.offset(-radius, 18, radius);
        };
        switch (kind) {
            case BALLISTA -> fireBallista(level, tower, wall, command);
            case FLAME -> fireFlame(level, tower, wall, command);
            case FROST -> fireFrost(level, tower, wall, command);
            case ARCANE -> fireArcane(level, tower, wall, command);
        }
    }

    private static void fireBallista(ServerLevel level, BlockPos tower, int wall, float command) {
        Mob target = VillageRaidSystem.nearestActiveEnemy(level, tower, 120.0);
        if (target == null) return;
        VillageTowerSpecializationSystem.Branch branch = VillageTowerSpecializationSystem.branch(
                VillageTowerSpecializationSystem.TowerKind.BALLISTA);
        int rank = VillageTowerSpecializationSystem.rank(VillageTowerSpecializationSystem.TowerKind.BALLISTA);
        float base = (6.0f + wall * 2.1f) * command;
        trail(level, Vec3.atCenterOf(tower.above(4)), target.position().add(0, target.getBbHeight() * 0.55, 0),
                ParticleTypes.CRIT);

        if (branch == VillageTowerSpecializationSystem.Branch.BALLISTA_TITAN) {
            float multiplier = VillageRaidSystem.isBossEnemy(target) ? 1.75f + rank * 0.35f : 1.08f + rank * 0.08f;
            hit(level, target, base * multiplier, ParticleTypes.CRIT, 14);
            return;
        }
        if (branch == VillageTowerSpecializationSystem.Branch.BALLISTA_PIERCE) {
            List<Mob> targets = VillageRaidSystem.activeEnemiesNear(level, target.position(), 6.0 + rank, 3 + rank, null);
            for (Mob mob : targets) hit(level, mob, base * (0.72f + rank * 0.08f), ParticleTypes.CRIT, 8);
            return;
        }
        if (branch == VillageTowerSpecializationSystem.Branch.BALLISTA_SPLIT) {
            List<Mob> targets = VillageRaidSystem.activeEnemiesNear(level, Vec3.atCenterOf(tower), 120.0, 2 + rank, null);
            for (Mob mob : targets) {
                trail(level, Vec3.atCenterOf(tower.above(4)), mob.position().add(0, mob.getBbHeight() * 0.55, 0),
                        ParticleTypes.CRIT);
                hit(level, mob, base * (0.64f + rank * 0.07f), ParticleTypes.CRIT, 7);
            }
            return;
        }
        hit(level, target, base, ParticleTypes.CRIT, 10);
    }

    private static void fireFlame(ServerLevel level, BlockPos tower, int wall, float command) {
        Mob target = VillageRaidSystem.nearestActiveEnemy(level, tower, 105.0);
        if (target == null) return;
        VillageTowerSpecializationSystem.Branch branch = VillageTowerSpecializationSystem.branch(
                VillageTowerSpecializationSystem.TowerKind.FLAME);
        int rank = VillageTowerSpecializationSystem.rank(VillageTowerSpecializationSystem.TowerKind.FLAME);
        double radius = branch == VillageTowerSpecializationSystem.Branch.FLAME_INFERNO ? 5.5 + rank * 1.5 : 5.5;
        int limit = 5 + wall + (branch == VillageTowerSpecializationSystem.Branch.FLAME_INFERNO ? rank * 2 : 0);
        float damage = (3.0f + wall * 1.1f) * command;
        if (branch == VillageTowerSpecializationSystem.Branch.FLAME_BLAST) damage *= 1.22f + rank * 0.18f;
        trail(level, Vec3.atCenterOf(tower.above(5)), target.position().add(0, target.getBbHeight() * 0.55, 0),
                ParticleTypes.FLAME);
        List<Mob> targets = VillageRaidSystem.activeEnemiesNear(level, target.position(), radius, limit, null);
        for (Mob mob : targets) {
            hit(level, mob, damage, ParticleTypes.FLAME,
                    branch == VillageTowerSpecializationSystem.Branch.FLAME_BLAST ? 14 : 9);
            int fireTicks = 80 + wall * 20 + (branch == VillageTowerSpecializationSystem.Branch.FLAME_INFERNO ? rank * 70 : 0);
            mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), fireTicks));
            if (branch == VillageTowerSpecializationSystem.Branch.FLAME_MELT) {
                mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80 + rank * 40, Math.min(2, rank - 1)));
            }
        }
        if (branch == VillageTowerSpecializationSystem.Branch.FLAME_BLAST) {
            level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.8, target.getZ(),
                    3 + rank, 0.65, 0.45, 0.65, 0.02);
        }
    }

    private static void fireFrost(ServerLevel level, BlockPos tower, int wall, float command) {
        Mob target = VillageRaidSystem.nearestActiveEnemy(level, tower, 100.0);
        if (target == null) return;
        VillageTowerSpecializationSystem.Branch branch = VillageTowerSpecializationSystem.branch(
                VillageTowerSpecializationSystem.TowerKind.FROST);
        int rank = VillageTowerSpecializationSystem.rank(VillageTowerSpecializationSystem.TowerKind.FROST);
        double radius = branch == VillageTowerSpecializationSystem.Branch.FROST_BLIZZARD ? 6.5 + rank * 1.8 : 6.5;
        int limit = 6 + wall + (branch == VillageTowerSpecializationSystem.Branch.FROST_BLIZZARD ? rank * 3 : 0);
        float base = (2.2f + wall * 0.8f) * command;
        trail(level, Vec3.atCenterOf(tower.above(4)), target.position().add(0, target.getBbHeight() * 0.55, 0),
                ParticleTypes.SNOWFLAKE);
        List<Mob> targets = VillageRaidSystem.activeEnemiesNear(level, target.position(), radius, limit, null);
        for (Mob mob : targets) {
            float damage = base;
            if (branch == VillageTowerSpecializationSystem.Branch.FROST_SHATTER
                    && mob.hasEffect(MobEffects.SLOWNESS)) damage *= 1.35f + rank * 0.25f;
            hit(level, mob, damage, ParticleTypes.SNOWFLAKE, 10);
            int amplifier = wall >= 5 ? 2 : 1;
            if (branch == VillageTowerSpecializationSystem.Branch.FROST_DEEP) amplifier += rank;
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 65 + wall * 15 + rank * 20,
                    Math.min(5, amplifier)));
        }
    }

    private static void fireArcane(ServerLevel level, BlockPos tower, int wall, float command) {
        Mob target = VillageRaidSystem.nearestActiveEnemy(level, tower, 115.0);
        if (target == null) return;
        VillageTowerSpecializationSystem.Branch branch = VillageTowerSpecializationSystem.branch(
                VillageTowerSpecializationSystem.TowerKind.ARCANE);
        int rank = VillageTowerSpecializationSystem.rank(VillageTowerSpecializationSystem.TowerKind.ARCANE);
        int limit = wall >= 5 ? 9 : 6;
        if (branch == VillageTowerSpecializationSystem.Branch.ARCANE_CHAIN) limit += rank * 3;
        float damage = (4.0f + wall * 1.25f) * command;
        if (branch == VillageTowerSpecializationSystem.Branch.ARCANE_OVERCHARGE) damage *= 1.28f + rank * 0.24f;
        List<Mob> targets = VillageRaidSystem.activeEnemiesNear(level, target.position(), 8.0 + rank, limit, null);
        Vec3 previous = Vec3.atCenterOf(tower.above(4));
        for (Mob mob : targets) {
            Vec3 impact = mob.position().add(0, mob.getBbHeight() * 0.55, 0);
            trail(level, previous, impact, ParticleTypes.ENCHANT);
            previous = impact;
            hit(level, mob, damage, ParticleTypes.ENCHANT, 12);
            if (wall >= 5 || branch == VillageTowerSpecializationSystem.Branch.ARCANE_NULL) {
                mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60 + rank * 35,
                        branch == VillageTowerSpecializationSystem.Branch.ARCANE_NULL ? Math.min(2, rank - 1) : 0));
            }
            if (branch == VillageTowerSpecializationSystem.Branch.ARCANE_NULL) {
                mob.removeEffect(MobEffects.STRENGTH);
                mob.removeEffect(MobEffects.REGENERATION);
            }
        }
    }

    private static void hit(ServerLevel level, Mob target, float damage, ParticleOptions particle, int count) {
        target.hurtServer(level, level.damageSources().magic(), Math.max(0.1f, damage));
        level.sendParticles(particle, target.getX(), target.getY() + target.getBbHeight() * 0.6,
                target.getZ(), count, 0.28, 0.32, 0.28, 0.04);
    }

    private static void trail(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle) {
        Vec3 delta = to.subtract(from);
        double length = Math.max(0.001, delta.length());
        int steps = Math.max(4, Math.min(28, (int) Math.ceil(length * 1.3)));
        for (int step = 0; step <= steps; step++) {
            double progress = step / (double) steps;
            Vec3 point = from.add(delta.scale(progress));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private static boolean hasActiveWarden(MinecraftServer server) {
        if (server == null) return false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (VillageCouncilState.isInsideVillage(player)
                    && VillageCouncilState.roleOf(player.getUUID()).orElse(null) == VillageRole.WARDEN) return true;
        }
        return false;
    }

    private static int mercenaryCapacity(MinecraftServer server) {
        return 1 + Math.max(0, VillageProgressionSystem.barracksLevel()) / 2
                + (hasActiveWarden(server) ? 1 : 0);
    }

    private static int countMercenaries(ServerLevel level) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return 0;
        AABB area = new AABB(center).inflate(VillageWorldSystem.FORTRESS_RADIUS + 12, 48,
                VillageWorldSystem.FORTRESS_RADIUS + 12);
        int count = 0;
        for (IronGolem golem : level.getEntitiesOfClass(IronGolem.class, area)) {
            Component name = golem.getCustomName();
            if (golem.isAlive() && name != null && MERCENARY_NAME.equals(name.getString())) count++;
        }
        return count;
    }

    private static BlockPos findSpawn(ServerLevel level, BlockPos origin) {
        for (int radius = 3; radius <= 8; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) continue;
                    BlockPos candidate = origin.offset(x, 0, z);
                    if (level.getBlockState(candidate).isAir()
                            && level.getBlockState(candidate.above()).isAir()
                            && !level.getBlockState(candidate.below()).isAir()) return candidate;
                }
            }
        }
        return origin.offset(0, 0, 5);
    }
}
