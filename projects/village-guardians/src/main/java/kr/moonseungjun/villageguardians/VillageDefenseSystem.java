package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
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

import java.util.List;

public final class VillageDefenseSystem {
    private static final String MERCENARY_NAME = "마을 용병";
    private static int towerTicks;

    private VillageDefenseSystem() {}

    public static void reset() { towerTicks = 0; }

    public static boolean recognizeDefenseMob(Mob mob) {
        Component name = mob.getCustomName();
        if (!(mob instanceof IronGolem) || name == null || !MERCENARY_NAME.equals(name.getString())) return false;
        VillageWorldSystem.markAllowedGameMob(mob);
        mob.setPersistenceRequired();
        return true;
    }

    public static int mercenaryHireCost() {
        return 140 + VillageProgressionSystem.barracksLevel() * 35;
    }

    public static String hireMercenary(ServerPlayer player) {
        if (!VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.BARRACKS)) {
            return "병영이 파괴되어 용병을 고용할 수 없습니다.";
        }
        if (!(player.level() instanceof ServerLevel level)) return "현재 월드에서는 용병을 고용할 수 없습니다.";
        int cap = mercenaryCapacity(level.getServer());
        int current = countMercenaries(level);
        if (current >= cap) return "용병 정원이 가득 찼습니다. 현재 " + current + " / " + cap;
        int cost = mercenaryHireCost();
        if (!VillageProgressionSystem.spendCoins(player, cost)) {
            return "용병 계약에 수호 주화 " + cost + "이 필요합니다. 현재 "
                    + VillageProgressionSystem.coins(player);
        }

        IronGolem golem = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.EVENT);
        if (golem == null) {
            VillageProgressionSystem.addCoins(player, cost, "용병 계약 취소 환불");
            return "용병을 소환하지 못해 주화를 돌려드렸습니다.";
        }
        BlockPos barracks = VillageWorldSystem.buildingCenter(VillageProgressionSystem.Building.BARRACKS);
        BlockPos spawn = findSpawn(level, barracks);
        golem.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        golem.setCustomName(Component.literal(MERCENARY_NAME));
        golem.setCustomNameVisible(true);
        golem.setPlayerCreated(true);
        golem.setPersistenceRequired();
        if (VillageCouncilState.roleOf(player.getUUID()).orElse(null) == VillageRole.WARDEN) {
            golem.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, -1, 0));
            golem.addEffect(new MobEffectInstance(MobEffects.STRENGTH, -1, 0));
        }
        VillageWorldSystem.markAllowedGameMob(golem);
        if (!level.addFreshEntity(golem)) {
            VillageWorldSystem.unmarkAllowedGameMob(golem.getUUID());
            VillageProgressionSystem.addCoins(player, cost, "용병 배치 실패 환불");
            return "용병 배치에 실패해 주화를 돌려드렸습니다.";
        }
        return "용병 고용 완료 | 주화 " + cost + " 사용 | 현재 " + (current + 1) + " / " + cap
                + " · 사망하지 않는 한 저장과 재접속 후에도 유지됩니다.";
    }

    public static String status(ServerLevel level) {
        int wall = VillageProgressionSystem.wallLevel();
        String towers = wall <= 0 ? "방어탑 비활성" : switch (wall) {
            case 1 -> "노포탑";
            case 2 -> "노포탑·화염탑";
            case 3 -> "노포탑·화염탑·빙결탑";
            default -> "노포탑·화염탑·빙결탑·비전탑" + (wall >= 5 ? " 강화" : "");
        };
        return towers + " | 용병 " + countMercenaries(level) + " / " + mercenaryCapacity(level.getServer());
    }

    public static void tick(MinecraftServer server) {
        if (!VillageRaidSystem.isActive()
                || !VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.WALLS)
                || VillageProgressionSystem.wallLevel() <= 0) {
            towerTicks = 0;
            return;
        }
        int wall = VillageProgressionSystem.wallLevel();
        boolean warden = hasActiveWarden(server);
        int interval = Math.max(13, 46 - wall * 5 - (warden ? 5 : 0));
        if (++towerTicks < interval) return;
        towerTicks = 0;

        ServerLevel level = server.overworld();
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        int radius = VillageWorldSystem.FORTRESS_RADIUS - 4;
        float command = warden ? 1.25f : 1.0f;

        fireBallista(level, center.offset(radius, 18, -radius), wall, command);
        if (wall >= 2) fireFlame(level, center.offset(-radius, 18, -radius), wall, command);
        if (wall >= 3) fireFrost(level, center.offset(radius, 18, radius), wall, command);
        if (wall >= 4) fireArcane(level, center.offset(-radius, 18, radius), wall, command);
    }

    private static void fireBallista(ServerLevel level, BlockPos tower, int wall, float command) {
        Mob target = VillageRaidSystem.nearestActiveEnemy(level, tower, 120.0);
        if (target == null) return;
        target.hurtServer(level, level.damageSources().magic(), (6.0f + wall * 2.1f) * command);
        particles(level, target, ParticleTypes.CRIT, 10);
    }

    private static void fireFlame(ServerLevel level, BlockPos tower, int wall, float command) {
        Mob target = VillageRaidSystem.nearestActiveEnemy(level, tower, 105.0);
        if (target == null) return;
        List<Mob> targets = VillageRaidSystem.activeEnemiesNear(level, target.position(), 5.5, 5 + wall, null);
        for (Mob mob : targets) {
            mob.hurtServer(level, level.damageSources().magic(), (3.0f + wall * 1.1f) * command);
            mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 80 + wall * 20));
        }
        particles(level, target, ParticleTypes.FLAME, 12);
    }

    private static void fireFrost(ServerLevel level, BlockPos tower, int wall, float command) {
        Mob target = VillageRaidSystem.nearestActiveEnemy(level, tower, 100.0);
        if (target == null) return;
        List<Mob> targets = VillageRaidSystem.activeEnemiesNear(level, target.position(), 6.5, 6 + wall, null);
        for (Mob mob : targets) {
            mob.hurtServer(level, level.damageSources().magic(), (2.2f + wall * 0.8f) * command);
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 65 + wall * 15, wall >= 5 ? 2 : 1));
        }
        particles(level, target, ParticleTypes.SNOWFLAKE, 12);
    }

    private static void fireArcane(ServerLevel level, BlockPos tower, int wall, float command) {
        Mob target = VillageRaidSystem.nearestActiveEnemy(level, tower, 115.0);
        if (target == null) return;
        List<Mob> targets = VillageRaidSystem.activeEnemiesNear(level, target.position(), 8.0, wall >= 5 ? 9 : 6, null);
        for (Mob mob : targets) {
            mob.hurtServer(level, level.damageSources().magic(), (4.0f + wall * 1.25f) * command);
            if (wall >= 5) mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        }
        particles(level, target, ParticleTypes.ENCHANT, 14);
    }

    private static void particles(ServerLevel level, Mob target, net.minecraft.core.particles.ParticleOptions type, int count) {
        level.sendParticles(type, target.getX(), target.getY() + target.getBbHeight() * 0.6,
                target.getZ(), count, 0.25, 0.25, 0.25, 0.05);
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
