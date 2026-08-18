package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class VillageRaidSystem {
    private static final Set<UUID> ACTIVE_ENEMIES = new HashSet<>();
    private static final Map<UUID, VillageEnemyArchetypeSystem.Archetype> ACTIVE_ARCHETYPES = new HashMap<>();
    private static final Map<UUID, Integer> ACTIVE_WAVES = new HashMap<>();
    private static final int FIRST_WAVE_COUNTDOWN_TICKS = 240;
    private static final int BETWEEN_WAVE_TICKS = 120;
    private static final int FORCED_NEXT_WAVE_TICKS = 20 * 60;
    private static final int MAX_ACTIVE_ENEMIES = 100;
    private static final int STRUCTURE_ATTACK_INTERVAL = 30;
    private static final double PLAYER_PRIORITY_RANGE = 16.0;
    private static final String RAID_TEAM_NAME = "vg_raid";
    private static final String RAID_ENEMY_TAG = "villageguardians_raid_enemy";

    private static boolean active;
    private static int wave;
    private static int maxWaves;
    private static int countdownTicks;
    private static int betweenWaveTicks;
    private static int waveElapsedTicks;
    private static int structureAttackTicks;
    private static int abilityTicks;
    private static VillageWaveTrait currentTrait = VillageWaveTrait.STANDARD;

    private VillageRaidSystem() {}

    public static void resetTransientState(MinecraftServer server) {
        discardTaggedRaidEnemies(server);
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
        if (phase == VillageTimePhase.NIGHT) scheduleRaid(server);
    }

    public static void tick(MinecraftServer server) {
        if (VillageProgressionSystem.isGameOver()) return;
        if (countdownTicks > 0) {
            countdownTicks--;
            if (countdownTicks == 0) {
                active = true;
                wave = 1;
                spawnWave(server);
            }
            return;
        }
        if (!active) return;

        purgeMissingEnemies(server);
        directEnemies(server);
        if (VillageProgressionSystem.isGameOver()) return;
        waveElapsedTicks++;

        if (wave >= maxWaves) {
            if (ACTIVE_ENEMIES.isEmpty()) finishVictory(server);
            return;
        }

        if (ACTIVE_ENEMIES.isEmpty()) {
            if (betweenWaveTicks <= 0) {
                betweenWaveTicks = BETWEEN_WAVE_TICKS;
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("§e[습격] §f현재 웨이브 정리 완료. 다음 웨이브까지 6초입니다."), false);
                return;
            }
            betweenWaveTicks--;
            if (betweenWaveTicks == 0) {
                wave++;
                VillageProgressionSystem.healRaidParty(server, false);
                spawnWave(server);
            }
            return;
        }

        betweenWaveTicks = 0;
        if (waveElapsedTicks >= FORCED_NEXT_WAVE_TICKS) {
            wave++;
            VillageProgressionSystem.healRaidParty(server, false);
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§6[강제 진군] §f잔존 적 " + ACTIVE_ENEMIES.size()
                            + "명이 남았지만 다음 웨이브가 합류합니다."), false);
            spawnWave(server);
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        UUID uuid = event.getEntity().getUUID();
        if (!ACTIVE_ENEMIES.remove(uuid)) return;
        MinecraftServer server = event.getEntity().level().getServer();
        if (server != null) releaseEnemy(server, uuid, event.getEntity());
    }

    public static boolean isActiveEnemy(UUID uuid) {
        return ACTIVE_ENEMIES.contains(uuid);
    }

    public static boolean isRaidEnemy(Entity entity) {
        if (entity == null) return false;
        if (ACTIVE_ENEMIES.contains(entity.getUUID()) || ACTIVE_ARCHETYPES.containsKey(entity.getUUID())) return true;
        Component name = entity.getCustomName();
        if (name == null) return false;
        String text = name.getString();
        if (text.contains("웨이브 ")) return true;
        for (VillageEnemyArchetypeSystem.Archetype archetype : VillageEnemyArchetypeSystem.Archetype.values()) {
            if (VillageEnemyArchetypeSystem.isBoss(archetype) && text.contains(archetype.displayName())) return true;
        }
        return false;
    }

    public static boolean isBossEnemy(Mob mob) {
        VillageEnemyArchetypeSystem.Archetype archetype = archetypeOf(mob);
        return archetype != null && VillageEnemyArchetypeSystem.isBoss(archetype);
    }

    public static VillageEnemyArchetypeSystem.Archetype archetypeOf(Mob mob) {
        return mob == null ? null : ACTIVE_ARCHETYPES.get(mob.getUUID());
    }

    public static int waveOf(Mob mob) {
        if (mob == null) return Math.max(1, wave);
        return Math.max(1, ACTIVE_WAVES.getOrDefault(mob.getUUID(), Math.max(1, wave)));
    }

    public static VillageWaveTrait currentTrait() { return currentTrait; }
    public static boolean isActive() { return active; }
    public static boolean isRaidLocked() { return active || countdownTicks > 0; }

    public static String status() {
        if (VillageProgressionSystem.isGameOver()) return "§c마을 방어 실패 상태";
        if (countdownTicks > 0) {
            return "습격 시작까지 " + Math.max(1, (countdownTicks + 19) / 20) + "초";
        }
        if (active) {
            String next = "";
            if (wave < maxWaves) {
                int ticks = ACTIVE_ENEMIES.isEmpty() && betweenWaveTicks > 0
                        ? betweenWaveTicks
                        : Math.max(0, FORCED_NEXT_WAVE_TICKS - waveElapsedTicks);
                next = " | 다음 " + Math.max(1, (ticks + 19) / 20) + "초";
            }
            return "웨이브 " + wave + "/" + maxWaves
                    + " · " + currentTrait.displayName()
                    + " | 남은 적 " + ACTIVE_ENEMIES.size() + next;
        }
        return "현재 안전합니다. 밤으로 전환하면 북쪽 성문 습격이 시작됩니다.";
    }

    public static Mob nearestActiveEnemy(ServerLevel level, BlockPos origin, double radius) {
        List<Mob> nearby = activeEnemiesNear(level, Vec3.atCenterOf(origin), radius, 1, null);
        return nearby.isEmpty() ? null : nearby.getFirst();
    }

    public static List<Mob> activeEnemiesNear(
            ServerLevel level,
            Vec3 origin,
            double radius,
            int limit,
            UUID excluded) {
        double radiusSquared = radius * radius;
        List<Mob> result = new ArrayList<>();
        for (UUID id : ACTIVE_ENEMIES) {
            if (excluded != null && excluded.equals(id)) continue;
            Entity entity = level.getEntity(id);
            if (entity instanceof Mob mob
                    && mob.isAlive()
                    && mob.position().distanceToSqr(origin) <= radiusSquared) {
                result.add(mob);
            }
        }
        result.sort(Comparator.comparingDouble(mob -> mob.position().distanceToSqr(origin)));
        if (result.size() > Math.max(0, limit)) {
            return new ArrayList<>(result.subList(0, Math.max(0, limit)));
        }
        return result;
    }

    public static void triggerGameOver(MinecraftServer server) {
        discardEnemies(server);
        clearState();
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§4[게임 오버] §f마을 회관이 파괴되었습니다."), false);
        VillageUiService.openGameOverForAll(server);
    }

    public static int previewMaxWaves(int day) {
        return Math.min(8, 3 + Math.max(0, day - 1) / 2);
    }

    public static int previewWaveCount(int day, int previewWave, int players, VillageWaveTrait trait) {
        int soloBase = 4 + previewWave * 2 + Math.min(30, day * 2) + VillageWarfrontSystem.countBonus(day);
        int soloCount = trait.adjustedCount(soloBase);
        return VillageDifficultyTuning.scaleEnemyCount(soloCount, Math.max(1, players));
    }

    public static int previewBossCount(int day, int previewWave, int maximumWaves, int count) {
        return Math.min(Math.max(0, count),
                VillageWarfrontSystem.bonusBossCount(day, previewWave, maximumWaves));
    }

    private static void scheduleRaid(MinecraftServer server) {
        if (isRaidLocked() || VillageProgressionSystem.isGameOver()) return;
        int day = VillageCouncilState.currentDay();
        maxWaves = previewMaxWaves(day);
        countdownTicks = FIRST_WAVE_COUNTDOWN_TICKS;
        wave = 0;
        betweenWaveTicks = 0;
        waveElapsedTicks = 0;
        currentTrait = VillageWaveTrait.STANDARD;
        String milestone = VillageWarfrontSystem.milestoneHint(day);
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§c[야간 습격] §f제 " + day + "일 · " + VillageWarfrontSystem.dayTitle(day)
                        + "\n§f12초 뒤 북쪽 외곽에서 " + maxWaves
                        + "개 웨이브가 접근합니다. 각 웨이브는 늦어도 60초 뒤 이어집니다."
                        + (milestone.isBlank() ? "" : "\n§6" + milestone)), false);
    }

    private static void spawnWave(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos origin = VillageWorldSystem.northSpawnOrigin();
        int players = VillageProgressionSystem.plannedRaidPlayerCount(server);
        int day = VillageCouncilState.currentDay();
        currentTrait = VillageWaveTrait.select(day, wave);
        int requested = previewWaveCount(day, wave, players, currentTrait);
        int capacity = Math.max(0, MAX_ACTIVE_ENEMIES - ACTIVE_ENEMIES.size());
        int count = Math.min(requested, capacity);
        int bossCount = Math.min(count, VillageWarfrontSystem.bonusBossCount(day, wave, maxWaves));
        int before = ACTIVE_ENEMIES.size();
        PlayerTeam raidTeam = ensureRaidTeam(server);

        for (int index = 0; index < count; index++) {
            boolean boss = index < bossCount;
            VillageEnemyArchetypeSystem.SpawnedEnemy spawned = VillageEnemyArchetypeSystem.create(
                    level, day, wave, index, boss, currentTrait);
            if (spawned == null || spawned.mob() == null) continue;
            Mob mob = spawned.mob();
            int row = index / 9;
            int spread = (index % 9) - 4;
            BlockPos spawn = origin.offset(spread * 2, 0, -row * 3);
            mob.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn), EntitySpawnReason.EVENT, null);
            applyScaling(mob, day, wave, boss);
            VillageEnemyArchetypeSystem.configure(
                    level, mob, spawned.archetype(), currentTrait, day, wave, boss);
            if (boss) VillageBossAspectSystem.configure(level, mob, day, wave, index);
            mob.addTag(RAID_ENEMY_TAG);
            VillageWorldSystem.markAllowedGameMob(mob);
            server.getScoreboard().addPlayerToTeam(mob.getScoreboardName(), raidTeam);
            // Register authoritative combat metadata before addFreshEntity fires EntityJoinLevelEvent.
            ACTIVE_ARCHETYPES.put(mob.getUUID(), spawned.archetype());
            ACTIVE_WAVES.put(mob.getUUID(), wave);
            if (level.addFreshEntity(mob)) {
                ACTIVE_ENEMIES.add(mob.getUUID());
            } else {
                releaseEnemy(server, mob.getUUID(), mob);
            }
        }
        waveElapsedTicks = 0;
        betweenWaveTicks = 0;
        structureAttackTicks = 0;
        int spawned = ACTIVE_ENEMIES.size() - before;
        String capped = count < requested ? " §7(전장 개체 상한 적용)" : "";
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§c[웨이브 " + wave + "/" + maxWaves + "] §f"
                        + currentTrait.displayName() + " · 신규 적 " + spawned
                        + "명 · 전장 총 " + ACTIVE_ENEMIES.size() + "명" + capped
                        + "\n§7" + currentTrait.description()
                        + "\n§b대응: " + currentTrait.counterHint()), false);
    }

    private static void applyScaling(Mob mob, int day, int currentWave, boolean boss) {
        int duration = 20 * 60 * 30;
        int healthTier = Math.min(9, Math.max(0, (day - 1) / 3 + currentWave / 4
                + Math.max(0, day - 20) / 8));
        int strengthTier = Math.min(5, Math.max(0, (day - 1) / 5 + Math.max(0, day - 25) / 10));
        if (healthTier > 0 || boss) {
            mob.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, duration,
                    Math.min(11, healthTier + (boss ? 3 : 0))));
        }
        if (strengthTier > 0 || boss) {
            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration,
                    Math.min(6, strengthTier + (boss ? 1 : 0))));
        }
        if (day >= 5) {
            mob.addEffect(new MobEffectInstance(MobEffects.SPEED, duration,
                    Math.min(3, Math.max(0, (day - 3) / 4))));
        }
        if (day >= 18) {
            mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration,
                    Math.min(4, (day - 15) / 8)));
        }
        if (boss) {
            mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, 1));
            mob.setGlowingTag(true);
        }
    }

    private static void directEnemies(MinecraftServer server) {
        ServerLevel level = server.overworld();
        structureAttackTicks = Math.floorMod(structureAttackTicks + 1, STRUCTURE_ATTACK_INTERVAL);
        abilityTicks++;

        BlockPos villageCenter = VillageCouncilState.villageCenter().orElse(null);
        boolean gatePassable = VillageWorldSystem.isNorthGatePassable(level)
                || !VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.WALLS);

        for (UUID id : new HashSet<>(ACTIVE_ENEMIES)) {
            Entity entity = level.getEntity(id);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) continue;
            VillageEnemyArchetypeSystem.Archetype archetype = ACTIVE_ARCHETYPES.getOrDefault(
                    id, VillageEnemyArchetypeSystem.Archetype.GRUNT);
            updateEnemyOutline(server, mob);
            VillageEnemyArchetypeSystem.tickAbility(
                    level, server, mob, archetype, currentTrait, abilityTicks);
            if (VillageEnemyArchetypeSystem.isBoss(archetype)) {
                VillageBossAspectSystem.tick(level, server, mob, abilityTicks);
            }

            if (archetype == VillageEnemyArchetypeSystem.Archetype.TOWER_HUNTER) {
                VillagePlacedTurretSystem.TurretState turret =
                        VillagePlacedTurretSystem.nearestActiveTurret(mob.position(), 48.0);
                if (turret != null) {
                    Vec3 turretCenter = Vec3.atCenterOf(turret.pos());
                    mob.setTarget(null);
                    mob.getLookControl().setLookAt(turretCenter.x, turretCenter.y + 1.0, turretCenter.z);
                    mob.getNavigation().moveTo(turretCenter.x, turretCenter.y, turretCenter.z, 1.14);
                    continue;
                }
            }

            ServerPlayer nearbyPlayer = gatePassable
                    && !VillageEnemyArchetypeSystem.ignoresNearbyPlayersUntilInside(archetype)
                    ? nearestPriorityPlayer(server, mob)
                    : null;
            if (nearbyPlayer != null) {
                mob.setTarget(nearbyPlayer);
                mob.getNavigation().moveTo(nearbyPlayer.getX(), nearbyPlayer.getY(), nearbyPlayer.getZ(), 1.18);
                continue;
            }
            mob.setTarget(null);

            if (gatePassable && villageCenter != null
                    && mob.getZ() < villageCenter.getZ() - VillageWorldSystem.FORTRESS_RADIUS + 8) {
                BlockPos approach = VillageWorldSystem.northInnerApproach();
                mob.getNavigation().moveTo(approach.getX() + 0.5, approach.getY(), approach.getZ() + 0.5, 1.12);
                continue;
            }

            VillageProgressionSystem.Building targetBuilding = chooseTarget(
                    villageCenter, mob.blockPosition(), gatePassable, archetype);
            if (targetBuilding == null || villageCenter == null) continue;
            BlockPos target = VillageFortressBuildings.attackPoint(villageCenter, targetBuilding, mob.blockPosition());
            mob.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5);
            mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.08);
            boolean attackTick = Math.floorMod(structureAttackTicks + id.hashCode(), STRUCTURE_ATTACK_INTERVAL) == 0;
            if (attackTick && VillageFortressBuildings.isTouchingStructure(
                    villageCenter, targetBuilding, mob.blockPosition())) {
                mob.swing(InteractionHand.MAIN_HAND);
                int day = VillageCouncilState.currentDay();
                float multiplier = currentTrait.structureDamageMultiplier()
                        * VillageWarfrontSystem.structureDamageMultiplier(day)
                        * VillageEnemyArchetypeSystem.structureDamageMultiplier(archetype)
                        * VillageBossAspectSystem.structureMultiplier(mob)
                        * VillageDifficultyTuning.earlyStructureMultiplier(day)
                        * VillageDifficultyTuning.defenderStateStructureMultiplier(server);
                int damage = Math.max(1, Math.round((7 + wave * 2 + Math.min(24, day)
                        + (VillageEnemyArchetypeSystem.isBoss(archetype) ? 18 : 0)) * multiplier));
                VillageProgressionSystem.damageBuilding(server, targetBuilding, damage);
                VillageDefenseEffectSystem.structureImpact(level, Vec3.atCenterOf(target),
                        VillageEnemyArchetypeSystem.isBoss(archetype) || archetype == VillageEnemyArchetypeSystem.Archetype.SAPPER);
                VillageEnemyArchetypeSystem.onStructureHit(level, mob, archetype);
            }
        }
    }

    private static ServerPlayer nearestPriorityPlayer(MinecraftServer server, Mob mob) {
        ServerPlayer chosen = null;
        double chosenDistance = PLAYER_PRIORITY_RANGE * PLAYER_PRIORITY_RANGE;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() != mob.level() || !player.isAlive() || player.isSpectator()
                    || VillageRespawnSystem.isDowned(player)) continue;
            if (VillageLocationRules.isEnemyIgnoredElevation(player)
                    || Math.abs(player.getY() - mob.getY()) > 3.5) continue;
            double distance = player.distanceToSqr(mob);
            if (distance <= chosenDistance) {
                chosenDistance = distance;
                chosen = player;
            }
        }
        return chosen;
    }

    private static VillageProgressionSystem.Building chooseTarget(
            BlockPos villageCenter,
            BlockPos enemyPos,
            boolean gatePassable,
            VillageEnemyArchetypeSystem.Archetype archetype) {
        if (!gatePassable && VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.WALLS)) {
            return VillageProgressionSystem.Building.WALLS;
        }
        if (VillageEnemyArchetypeSystem.prefersTower(archetype)
                && VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.WALLS)) {
            return VillageProgressionSystem.Building.WALLS;
        }
        if (villageCenter == null) return null;
        VillageProgressionSystem.Building chosen = null;
        long chosenDistance = Long.MAX_VALUE;
        for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
            if (building == VillageProgressionSystem.Building.WALLS
                    || !VillageProgressionSystem.isOperational(building)) continue;
            long distance = VillageFortressBuildings.distanceSquaredToStructure(villageCenter, building, enemyPos);
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
        mob.setGlowingTag(isBossEnemy(mob) || !visibleToAnyPlayer);
    }

    private static PlayerTeam ensureRaidTeam(MinecraftServer server) {
        PlayerTeam team = server.getScoreboard().getPlayerTeam(RAID_TEAM_NAME);
        if (team == null) team = server.getScoreboard().addPlayerTeam(RAID_TEAM_NAME);
        team.setColor(Optional.of(TeamColor.byName("red")));
        team.setAllowFriendlyFire(false);
        return team;
    }

    private static void purgeMissingEnemies(MinecraftServer server) {
        Iterator<UUID> iterator = ACTIVE_ENEMIES.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Entity entity = server.overworld().getEntity(uuid);
            if (entity == null) {
                releaseEnemy(server, uuid, null);
                iterator.remove();
            } else if (!entity.isAlive()) {
                releaseEnemy(server, uuid, entity);
                iterator.remove();
            }
        }
    }

    private static void finishVictory(MinecraftServer server) {
        int day = VillageCouncilState.currentDay();
        float campaignReward = VillageWarfrontSystem.rewardMultiplier(day);
        int supplies = Math.round((140 + day * 32)
                * VillageProgressionSystem.raidRewardMultiplierPercent() / 100.0f * campaignReward);
        int xp = Math.round((52 + day * 18 + VillageProgressionSystem.barracksLevel() * 10) * campaignReward);
        int coins = Math.round((42 + day * 9) * campaignReward);

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

    public static boolean shouldDiscardStaleRaidEnemy(Mob mob) {
        return mob != null
                && mob.entityTags().contains(RAID_ENEMY_TAG)
                && !ACTIVE_ENEMIES.contains(mob.getUUID())
                && !VillageWorldSystem.isAllowedGameMob(mob);
    }

    private static void discardTaggedRaidEnemies(MinecraftServer server) {
        if (server == null) return;
        ServerLevel level = server.overworld();
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        double radius = VillageWorldSystem.BATTLEFIELD_RADIUS + 160.0;
        AABB area = new AABB(center).inflate(radius, 128.0, radius);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area,
                entity -> entity.entityTags().contains(RAID_ENEMY_TAG))) {
            releaseEnemy(server, mob.getUUID(), mob);
            mob.discard();
        }
    }

    private static void discardEnemies(MinecraftServer server) {
        for (UUID id : new HashSet<>(ACTIVE_ENEMIES)) {
            Entity entity = server.overworld().getEntity(id);
            releaseEnemy(server, id, entity);
            if (entity != null) entity.discard();
        }
    }

    private static void releaseEnemy(MinecraftServer server, UUID uuid, Entity entity) {
        ACTIVE_ARCHETYPES.remove(uuid);
        ACTIVE_WAVES.remove(uuid);
        VillageBossAspectSystem.forget(uuid);
        VillageWorldSystem.unmarkAllowedGameMob(uuid);
        VillageHealthDisplaySystem.forgetEnemy(uuid);
        if (entity != null) {
            entity.setGlowingTag(false);
            PlayerTeam team = server.getScoreboard().getPlayerTeam(RAID_TEAM_NAME);
            if (team != null) server.getScoreboard().removePlayerFromTeam(entity.getScoreboardName(), team);
        }
    }

    private static void clearState() {
        ACTIVE_ENEMIES.clear();
        ACTIVE_ARCHETYPES.clear();
        ACTIVE_WAVES.clear();
        VillageBossAspectSystem.reset();
        active = false;
        wave = 0;
        maxWaves = 0;
        countdownTicks = 0;
        betweenWaveTicks = 0;
        waveElapsedTicks = 0;
        structureAttackTicks = 0;
        abilityTicks = 0;
        currentTrait = VillageWaveTrait.STANDARD;
    }
}
