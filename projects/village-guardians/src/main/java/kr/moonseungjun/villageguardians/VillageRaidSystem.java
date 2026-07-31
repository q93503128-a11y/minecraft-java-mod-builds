package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public final class VillageRaidSystem {
    private static final Set<UUID> ACTIVE_ENEMIES = new HashSet<>();
    private static final int FIRST_WAVE_COUNTDOWN_TICKS = 200;
    private static final int BETWEEN_WAVE_TICKS = 100;

    private static boolean active;
    private static int wave;
    private static int maxWaves;
    private static int countdownTicks;
    private static int betweenWaveTicks;

    private VillageRaidSystem() {
    }

    public static void resetTransientState(MinecraftServer server) {
        ACTIVE_ENEMIES.clear();
        active = false;
        wave = 0;
        maxWaves = 0;
        countdownTicks = 0;
        betweenWaveTicks = 0;
        if (VillageCouncilState.currentPhase() == VillageTimePhase.NIGHT) {
            scheduleRaid(server);
        }
    }

    public static void onPhaseChanged(MinecraftServer server, VillageTimePhase phase) {
        if (phase == VillageTimePhase.NIGHT) {
            scheduleRaid(server);
        } else if (!active) {
            countdownTicks = 0;
            betweenWaveTicks = 0;
        }
    }

    public static void tick(MinecraftServer server) {
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
        if (!ACTIVE_ENEMIES.isEmpty()) {
            return;
        }

        if (betweenWaveTicks <= 0) {
            betweenWaveTicks = BETWEEN_WAVE_TICKS;
            if (wave >= maxWaves) {
                finishVictory(server);
            } else {
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("§e[습격] §f다음 웨이브까지 5초. 창고·의무소를 빠르게 이용하세요."),
                        false);
            }
            return;
        }

        betweenWaveTicks--;
        if (betweenWaveTicks == 0 && active) {
            wave++;
            VillageProgressionSystem.healRaidParty(server, false);
            spawnWave(server);
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        ACTIVE_ENEMIES.remove(event.getEntity().getUUID());
    }

    public static boolean isRaidLocked() {
        return active || countdownTicks > 0;
    }

    public static String status() {
        if (countdownTicks > 0) {
            return "§c[습격] §f시작까지 " + Math.max(1, (countdownTicks + 19) / 20) + "초";
        }
        if (active) {
            return "§c[습격] §f웨이브 " + wave + "/" + maxWaves
                    + " | 남은 적 " + ACTIVE_ENEMIES.size();
        }
        return "§a[습격] §f현재 안전합니다. 밤으로 진행하면 방어전이 시작됩니다.";
    }

    private static void scheduleRaid(MinecraftServer server) {
        if (isRaidLocked()) {
            return;
        }
        int players = Math.max(1, server.getPlayerList().getPlayerCount());
        maxWaves = Math.min(5, 3 + Math.max(0, VillageCouncilState.currentDay() - 1) / 3);
        countdownTicks = FIRST_WAVE_COUNTDOWN_TICKS;
        wave = 0;
        betweenWaveTicks = 0;
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§c[야간 습격 경보] §f10초 뒤 " + maxWaves
                        + "개 웨이브가 시작됩니다. 현재 수비 인원 " + players + "명."),
                false);
    }

    private static void spawnWave(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos center = VillageCouncilState.villageCenter().orElse(level.getSharedSpawnPos());
        int players = Math.max(1, server.getPlayerList().getPlayerCount());
        int day = VillageCouncilState.currentDay();
        int count = 4 + wave * 2 + players * 2 + Math.min(8, day - 1);
        int side = (wave - 1) & 3;

        ACTIVE_ENEMIES.clear();
        for (int index = 0; index < count; index++) {
            BlockPos spawnPos = spawnPosition(center, side, index);
            Mob mob = createRaidMob(level, wave, index);
            if (mob == null) {
                continue;
            }
            mob.moveTo(
                    spawnPos.getX() + 0.5,
                    spawnPos.getY(),
                    spawnPos.getZ() + 0.5,
                    level.getRandom().nextFloat() * 360.0f,
                    0.0f);
            mob.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(spawnPos),
                    EntitySpawnReason.EVENT,
                    null);
            VillageWorldSystem.markAllowedGameMob(mob);
            if (level.addFreshEntity(mob)) {
                ACTIVE_ENEMIES.add(mob.getUUID());
            }
        }

        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§c[습격 웨이브 " + wave + "/" + maxWaves + "] §f적 "
                        + ACTIVE_ENEMIES.size() + "명이 " + sideName(side) + " 성문으로 접근합니다!"),
                false);
    }

    private static Mob createRaidMob(ServerLevel level, int currentWave, int index) {
        if (currentWave >= 3 && index % 5 == 0) {
            return EntityTypes.HUSK.create(level, EntitySpawnReason.EVENT);
        }
        if (currentWave >= 2 && index % 3 == 0) {
            return EntityTypes.SKELETON.create(level, EntitySpawnReason.EVENT);
        }
        return EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
    }

    private static BlockPos spawnPosition(BlockPos center, int side, int index) {
        int spread = (index % 9) - 4;
        int lane = (index / 9) * 2;
        int distance = 54 + lane;
        return switch (side) {
            case 0 -> new BlockPos(center.getX() + spread * 2, center.getY(), center.getZ() - distance);
            case 1 -> new BlockPos(center.getX() + distance, center.getY(), center.getZ() + spread * 2);
            case 2 -> new BlockPos(center.getX() + spread * 2, center.getY(), center.getZ() + distance);
            default -> new BlockPos(center.getX() - distance, center.getY(), center.getZ() + spread * 2);
        };
    }

    private static void purgeMissingEnemies(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Iterator<UUID> iterator = ACTIVE_ENEMIES.iterator();
        while (iterator.hasNext()) {
            UUID id = iterator.next();
            Entity entity = level.getEntity(id);
            if (entity == null || !entity.isAlive()) {
                iterator.remove();
            }
        }
    }

    private static void finishVictory(MinecraftServer server) {
        active = false;
        countdownTicks = 0;
        betweenWaveTicks = 0;
        ACTIVE_ENEMIES.clear();

        int day = VillageCouncilState.currentDay();
        int baseSupplies = 150 + day * 35;
        int supplies = baseSupplies * VillageProgressionSystem.raidRewardMultiplierPercent() / 100;
        int xp = 100 + day * 30 + VillageProgressionSystem.barracksLevel() * 15;

        VillageProgressionSystem.addSupplies(server, supplies, "제 " + day + "일 습격 방어 성공");
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            VillageCouncilState.grantExperience(player, xp);
            VillageRpgSystem.refreshPlayerPassive(player);
        }
        VillageProgressionSystem.healRaidParty(server, true);
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§a[방어 성공] §f모든 웨이브를 막았습니다. 전원 RPG XP " + xp + " 획득."),
                false);
        VillageCouncilState.completeRaid(server);
    }

    private static String sideName(int side) {
        return switch (side) {
            case 0 -> "북쪽";
            case 1 -> "동쪽";
            case 2 -> "남쪽";
            default -> "서쪽";
        };
    }
}
