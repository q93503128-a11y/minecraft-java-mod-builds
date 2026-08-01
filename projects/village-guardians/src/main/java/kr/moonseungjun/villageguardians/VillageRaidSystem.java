package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class VillageRaidSystem {
    private static final Set<UUID> ACTIVE_ENEMIES = new HashSet<>();
    private static final int FIRST_WAVE_COUNTDOWN_TICKS = 240;
    private static final int BETWEEN_WAVE_TICKS = 120;
    private static final int STRUCTURE_ATTACK_INTERVAL = 20;
    private static final double PLAYER_PRIORITY_RANGE = 16.0;
    private static final String RAID_TEAM_NAME = "vg_raid";

    private static boolean active;
    private static int wave;
    private static int maxWaves;
    private static int countdownTicks;
    private static int betweenWaveTicks;
    private static int structureAttackTicks;

    private VillageRaidSystem() {
    }

    public static void resetTransientState(MinecraftServer server) {
        clearState();
        ensureRaidTeam(server);
        if (VillageCouncilState.currentPhase() == VillageTimePhase.NIGHT
                && !VillageProgressionSystem.isGameOver()) {
            scheduleRaid(server);
        }
    }

    public static void resetAfterRestart(MinecraftServer server) {
        discardEnemies(server);
        clearState();
    }

    public static void onPhaseChanged(MinecraftServer server, VillageTimePhase phase) {
        if (phase == VillageTimePhase.NIGHT) {
            scheduleRaid(server);
        }
    }

    public static void tick(MinecraftServer server) {
        if (VillageProgressionSystem.isGameOver()) {
            return;
        }
        if (countdownTicks > 0) {
            countdownTicks--;
            if (countdownTicks == 0) {
                active = true;
                wave = 1;
                spawnWave(server);
            }
            return;
        }
        if (!active) {
            return;
        }

        purgeMissingEnemies(server);
        directEnemies(server);
        if (!ACTIVE_ENEMIES.isEmpty()) {
            return;
        }

        if (wave >= maxWaves) {
            finishVictory(server);
            return;
        }
        if (betweenWaveTicks <= 0) {
            betweenWaveTicks = BETWEEN_WAVE_TICKS;
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§e[습격] §f다음 웨이브까지 6초입니다."), false);
            return;
        }
        betweenWaveTicks--;
        if (betweenWaveTicks == 0) {
            wave++;
            VillageProgressionSystem.healRaidParty(server, false);
            spawnWave(server);
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        UUID uuid = event.getEntity().getUUID();
        if (!ACTIVE_ENEMIES.remove(uuid)) {
            return;
        }
        MinecraftServer server = event.getEntity().level().getServer();
        if (server != null) {
            releaseEnemy(server, uuid, event.getEntity());
        }
    }

    public static boolean isActiveEnemy(UUID uuid) {
        return ACTIVE_ENEMIES.contains(uuid);
    }

    public static boolean isRaidLocked() {
        return active || countdownTicks > 0;
    }

    public static String status() {
        if (VillageProgressionSystem.isGameOver()) {
            return "§c마을 방어 실패 상태";
        }
        if (countdownTicks > 0) {
            return "습격 시작까지 " + Math.max(1, (countdownTicks + 19) / 20) + "초";
        }
        if (active) {
            return "웨이브 " + wave + "/" + maxWaves + " | 남은 적 " + ACTIVE_ENEMIES.size();
        }
        return "현재 안전합니다. 밤으로 전환하면 북쪽 성문 습격이 시작됩니다.";
    }

    public static void triggerGameOver(MinecraftServer server) {
        discardEnemies(server);
        clearState();
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§4[게임 오버] §f모든 핵심 시설이 파괴되었습니다."), false);
        VillageUiService.openGameOverForAll(server);
    }

    private static void scheduleRaid(MinecraftServer server) {
        if (isRaidLocked() || VillageProgressionSystem.isGameOver()) {
            return;
        }
        maxWaves = Math.min(6, 3 + Math.max(0, VillageCouncilState.currentDay() - 1) / 2);
        countdownTicks = FIRST_WAVE_COUNTDOWN_TICKS;
        wave = 0;
        betweenWaveTicks = 0;
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§c[야간 습격] §f12초 뒤 북쪽 외곽에서 " + maxWaves + "개 웨이브가 접근합니다."),
                false);
    }

    private static void spawnWave(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos origin = VillageWorldSystem.northSpawnOrigin();
        int players = Math.max(1, server.getPlayerList().getPlayerCount());
        int day = VillageCouncilState.currentDay();
        int count = 5 + wave * 2 + players * 2 + Math.min(10, day - 1);
        ACTIVE_ENEMIES.clear();
        PlayerTeam raidTeam = ensureRaidTeam(server);

        for (int index = 0; index < count; index++) {
            Mob mob = createRaidMob(level, wave, index);
            if (mob == null) {
                continue;
            }
            int row = index / 9;
            int spread = (index % 9) - 4;
            BlockPos spawn = origin.offset(spread * 2, 0, -row * 3);
            mob.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn), EntitySpawnReason.EVENT, null);
            mob.setCustomName(Component.literal("웨이브 " + wave + " · ").append(mob.getType().getDescription()));
            mob.setCustomNameVisible(true);
            VillageWorldSystem.markAllowedGameMob(mob);
            server.getScoreboard().addPlayerToTeam(mob.getScoreboardName(), raidTeam);
            if (level.addFreshEntity(mob)) {
                ACTIVE_ENEMIES.add(mob.getUUID());
            } else {
                releaseEnemy(server, mob.getUUID(), mob);
            }
        }
        structureAttackTicks = 0;
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§c[웨이브 " + wave + "/" + maxWaves + "] §f적 "
                        + ACTIVE_ENEMIES.size() + "명이 북쪽 성문으로 진군합니다."), false);
    }

    private static Mob createRaidMob(ServerLevel level, int currentWave, int index) {
        if (currentWave >= 3 && index % 4 == 0) {
            return EntityTypes.HUSK.create(level, EntitySpawnReason.EVENT);
        }
        return EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
    }

    private static void directEnemies(MinecraftServer server) {
        ServerLevel level = server.overworld();
        structureAttackTicks++;
        boolean attackTick = structureAttackTicks >= STRUCTURE_ATTACK_INTERVAL;
        if (attackTick) {
            structureAttackTicks = 0;
        }

        BlockPos villageCenter = VillageCouncilState.villageCenter().orElse(null);
        boolean gatePassable = VillageWorldSystem.isNorthGatePassable(level)
                || !VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.WALLS);

        for (UUID id : ACTIVE_ENEMIES) {
            Entity entity = level.getEntity(id);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                continue;
            }
            updateEnemyOutline(server, mob);

            ServerPlayer nearbyPlayer = nearestPriorityPlayer(server, mob);
            if (nearbyPlayer != null) {
                mob.setTarget(nearbyPlayer);
                mob.getNavigation().moveTo(
                        nearbyPlayer.getX(), nearbyPlayer.getY(), nearbyPlayer.getZ(), 1.18);
                continue;
            }
            mob.setTarget(null);

            if (gatePassable && villageCenter != null
                    && mob.getZ() < villageCenter.getZ() - VillageWorldSystem.FORTRESS_RADIUS + 8) {
                BlockPos approach = VillageWorldSystem.northInnerApproach();
                mob.getNavigation().moveTo(
                        approach.getX() + 0.5,
                        approach.getY(),
                        approach.getZ() + 0.5,
                        1.12);
                continue;
            }

            VillageProgressionSystem.Building targetBuilding = chooseTarget(mob.blockPosition(), gatePassable);
            if (targetBuilding == null) {
                continue;
            }
            BlockPos target = VillageWorldSystem.buildingCenter(targetBuilding);
            mob.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5);
            mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.08);
            if (attackTick && distanceSquared(mob.blockPosition(), target) <= 36L) {
                mob.swing(InteractionHand.MAIN_HAND);
                int damage = 9 + wave * 2 + Math.min(8, VillageCouncilState.currentDay() / 2);
                VillageProgressionSystem.damageBuilding(server, targetBuilding, damage);
            }
        }
    }

    private static ServerPlayer nearestPriorityPlayer(MinecraftServer server, Mob mob) {
        ServerPlayer chosen = null;
        double chosenDistance = PLAYER_PRIORITY_RANGE * PLAYER_PRIORITY_RANGE;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() != mob.level() || !player.isAlive() || player.isSpectator()) {
                continue;
            }
            double distance = player.distanceToSqr(mob);
            if (distance <= chosenDistance) {
                chosenDistance = distance;
                chosen = player;
            }
        }
        return chosen;
    }

    private static VillageProgressionSystem.Building chooseTarget(BlockPos enemyPos, boolean gatePassable) {
        if (!gatePassable && VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.WALLS)) {
            return VillageProgressionSystem.Building.WALLS;
        }
        VillageProgressionSystem.Building chosen = null;
        long chosenDistance = Long.MAX_VALUE;
        for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
            if (building == VillageProgressionSystem.Building.WALLS
                    || !VillageProgressionSystem.isOperational(building)) {
                continue;
            }
            long distance = distanceSquared(enemyPos, VillageWorldSystem.buildingCenter(building));
            if (distance < chosenDistance) {
                chosenDistance = distance;
                chosen = building;
            }
        }
        return chosen;
    }

    private static void updateEnemyOutline(MinecraftServer server, Mob mob) {
        boolean visibleToAnyPlayer = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == mob.level()
                    && player.isAlive()
                    && player.distanceToSqr(mob) <= 160.0 * 160.0
                    && player.hasLineOfSight(mob)) {
                visibleToAnyPlayer = true;
                break;
            }
        }
        mob.setGlowingTag(!visibleToAnyPlayer);
    }

    private static PlayerTeam ensureRaidTeam(MinecraftServer server) {
        PlayerTeam team = server.getScoreboard().getPlayerTeam(RAID_TEAM_NAME);
        if (team == null) {
            team = server.getScoreboard().addPlayerTeam(RAID_TEAM_NAME);
        }
        team.setColor(Optional.of(TeamColor.byName("red")));
        team.setAllowFriendlyFire(false);
        return team;
    }

    private static void purgeMissingEnemies(MinecraftServer server) {
        Iterator<UUID> iterator = ACTIVE_ENEMIES.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Entity entity = server.overworld().getEntity(uuid);
            if (entity == null || !entity.isAlive()) {
                releaseEnemy(server, uuid, entity);
                iterator.remove();
            }
        }
    }

    private static void finishVictory(MinecraftServer server) {
        int day = VillageCouncilState.currentDay();
        int supplies = (150 + day * 35) * VillageProgressionSystem.raidRewardMultiplierPercent() / 100;
        int xp = 110 + day * 32 + VillageProgressionSystem.barracksLevel() * 15;
        int coins = 55 + day * 12;

        clearState();
        VillageProgressionSystem.addSupplies(server, supplies, "제 " + day + "일 방어 성공");
        VillageProgressionSystem.awardRaidCoins(server, coins);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            VillageCouncilState.grantExperience(player, xp);
            VillageRpgSystem.refreshPlayerPassive(player);
        }
        VillageProgressionSystem.healRaidParty(server, true);
        VillageCouncilState.completeRaid(server);
        VillageUiService.openRepairSummaryForAll(server);
    }

    private static void discardEnemies(MinecraftServer server) {
        for (UUID id : new HashSet<>(ACTIVE_ENEMIES)) {
            Entity entity = server.overworld().getEntity(id);
            releaseEnemy(server, id, entity);
            if (entity != null) {
                entity.discard();
            }
        }
    }

    private static void releaseEnemy(MinecraftServer server, UUID uuid, Entity entity) {
        VillageWorldSystem.unmarkAllowedGameMob(uuid);
        VillageHealthDisplaySystem.forgetEnemy(uuid);
        if (entity != null) {
            entity.setGlowingTag(false);
            PlayerTeam team = server.getScoreboard().getPlayerTeam(RAID_TEAM_NAME);
            if (team != null) {
                server.getScoreboard().removePlayerFromTeam(entity.getScoreboardName(), team);
            }
        }
    }

    private static void clearState() {
        ACTIVE_ENEMIES.clear();
        active = false;
        wave = 0;
        maxWaves = 0;
        countdownTicks = 0;
        betweenWaveTicks = 0;
        structureAttackTicks = 0;
    }

    private static long distanceSquared(BlockPos first, BlockPos second) {
        long dx = (long) first.getX() - second.getX();
        long dy = (long) first.getY() - second.getY();
        long dz = (long) first.getZ() - second.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
