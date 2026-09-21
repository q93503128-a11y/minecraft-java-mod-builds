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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
    private static final Map<UUID, VillageEnemyArchetypeSystem.AerialRole> ACTIVE_AERIAL_ROLES = new HashMap<>();
    private static final Map<UUID, AerialStrike> AERIAL_STRIKES = new HashMap<>();
    private static final Map<UUID, TauntState> FORCED_TAUNTS = new HashMap<>();
    private static final Map<UUID, EnemyProgress> FINAL_ENEMY_PROGRESS = new HashMap<>();
    private static final int FIRST_WAVE_COUNTDOWN_TICKS = 240;
    private static final int BETWEEN_WAVE_TICKS = 120;
    private static final int FORCED_NEXT_WAVE_TICKS = 20 * 60;
    private static final int FINAL_ENEMY_STALL_TICKS = 20 * 12;
    private static final int MAX_STALL_RECOVERIES_PER_PASS = 4;
    private static final double FINAL_ENEMY_PROGRESS_DISTANCE_SQR = 0.25 * 0.25;
    private static final int MAX_ACTIVE_ENEMIES = 100;
    private static final int STRUCTURE_ATTACK_INTERVAL = 30;
    private static final int AERIAL_ASSAULT_CADENCE = 90;
    private static final int AERIAL_WARNING_TICKS = 18;
    private static final int AERIAL_RECOVERY_TICKS = 34;
    private static final double AERIAL_PLAYER_STRIKE_RADIUS = 2.75;
    private static final double PLAYER_PRIORITY_RANGE = 16.0;
    private static final String RAID_TEAM_NAME = "vg_raid";
    private static final String RAID_ENEMY_TAG = "villageguardians_raid_enemy";
    private static final String AERIAL_ENEMY_TAG = "villageguardians_aerial_enemy";

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
        repairInvalidEnemyFlags(server);
        directEnemies(server);
        if (VillageProgressionSystem.isGameOver()) return;
        waveElapsedTicks++;

        // A broken last actor can happen on any wave. Waiting until maxWaves let an unresponsive
        // wave-2 mob survive into later waves and keep the entire night locked.
        recoverFrozenFinalEnemies(server);

        if (wave >= maxWaves) {
            if (ACTIVE_ENEMIES.isEmpty()) finishVictory(server);
            return;
        }

        if (ACTIVE_ENEMIES.isEmpty()) {
            if (betweenWaveTicks <= 0) {
                VillageProgressionSystem.awardRaidCoins(server,
                        waveClearCoinReward(VillageCouncilState.currentDay(), wave));
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

    public static int tauntEnemies(
            ServerLevel level, LivingEntity taunter, Vec3 center, double radius, int durationTicks, int limit) {
        if (level == null || taunter == null || center == null || radius <= 0.0 || durationTicks <= 0) return 0;
        boolean playerTaunter = taunter instanceof ServerPlayer;
        double effectiveRadius = playerTaunter ? Math.max(radius, 44.0) : radius;
        double radiusSquared = effectiveRadius * effectiveRadius;
        int maximum = playerTaunter ? Math.max(160, limit) : Math.max(1, limit);
        long until = level.getGameTime() + durationTicks;
        boolean mercenaryTaunter = taunter instanceof Mob taunterMob
                && VillageMercenarySystem.isCombatMercenary(taunterMob);
        List<Mob> candidates = activeEnemies(level).stream()
                .filter(mob -> !mercenaryTaunter || !isAerialEnemy(mob))
                .filter(mob -> mob.position().distanceToSqr(center) <= radiusSquared)
                .sorted(Comparator.comparingDouble(mob -> mob.position().distanceToSqr(center)))
                .limit(maximum)
                .toList();
        for (Mob enemy : candidates) {
            FORCED_TAUNTS.put(enemy.getUUID(), new TauntState(taunter.getUUID(), until));
            enemy.setTarget(taunter);
        }
        return candidates.size();
    }

    public static boolean isAerialEnemy(Entity entity) {
        return entity != null && (entity.entityTags().contains(AERIAL_ENEMY_TAG)
                || ACTIVE_AERIAL_ROLES.containsKey(entity.getUUID()));
    }

    public static boolean hasActiveTaunt(ServerLevel level, Mob mob) {
        return level != null && mob != null && activeTauntTarget(level, mob) != null;
    }

    public static boolean isRaidEnemy(Entity entity) {
        if (entity == null) return false;
        UUID uuid = entity.getUUID();
        return ACTIVE_ENEMIES.contains(uuid)
                || ACTIVE_ARCHETYPES.containsKey(uuid)
                || ACTIVE_WAVES.containsKey(uuid)
                || ACTIVE_AERIAL_ROLES.containsKey(uuid)
                || entity.entityTags().contains(RAID_ENEMY_TAG);
    }

    public static boolean isBossEnemy(Mob mob) {
        VillageEnemyArchetypeSystem.Archetype archetype = archetypeOf(mob);
        return archetype != null && VillageEnemyArchetypeSystem.isBoss(archetype);
    }

    public static VillageEnemyArchetypeSystem.Archetype archetypeOf(Mob mob) {
        return mob == null ? null : ACTIVE_ARCHETYPES.get(mob.getUUID());
    }

    public static int experienceForEnemy(Mob mob) {
        if (mob == null) return 0;
        int day = Math.max(1, VillageCouncilState.currentDay());
        int targetLevel = Math.max(1, Math.min(RpgProgress.MAX_LEVEL - 1,
                VillageCampaignProgression.targetPlayerLevel(day)));
        int required = RpgProgress.experienceRequiredAtLevel(targetLevel);
        VillageEnemyArchetypeSystem.Archetype archetype = archetypeOf(mob);

        // Preserve the established Lv.1-30 opening curve. From day 21 onward the campaign has
        // seven much larger waves, so XP is budgeted against the whole planned night instead of
        // treating the old per-level threat count as if it were still one night's roster.
        if (day <= 20) {
            float baseline = required * 0.72f / VillageCampaignProgression.expectedThreatsPerLevel(day);
            float healthWeight = (float) Math.sqrt(Math.max(1.0f, mob.getMaxHealth()) / 24.0f);
            healthWeight = Math.max(0.72f, Math.min(1.80f, healthWeight));
            float roleWeight = isBossEnemy(mob) ? 4.5f
                    : VillageEnemyEliteSystem.isElite(mob) ? 1.8f
                    : VillageEnemyArchetypeSystem.isTacticalThreat(archetype) ? 1.25f : 1.0f;
            return Math.max(1, Math.round(baseline * healthWeight * roleWeight));
        }

        MinecraftServer server = mob.level().getServer();
        int players = server == null ? 1 : VillageProgressionSystem.plannedRaidPlayerCount(server);
        int expectedActors = previewTotalEnemyCount(day, players);
        int targetLevels = VillageCampaignProgression.targetLevelsForDay(day);
        float baseline = required * 0.60f * targetLevels / Math.max(1, expectedActors);

        // Late HP scaling already makes enemies slower to kill; it must not also double their XP.
        // Keep a small durability premium, while preserving distinct rewards for tactical threats,
        // elites and bosses without letting those multipliers break the day-100 level target.
        float healthWeight = (float) Math.sqrt(Math.max(1.0f, mob.getMaxHealth()) / 24.0f);
        healthWeight = Math.max(0.90f, Math.min(1.15f, healthWeight));
        float roleWeight = isBossEnemy(mob) ? 3.0f
                : VillageEnemyEliteSystem.isElite(mob) ? 1.35f
                : VillageEnemyArchetypeSystem.isTacticalThreat(archetype) ? 1.12f : 1.0f;
        return Math.max(1, Math.round(baseline * healthWeight * roleWeight));
    }

    public static VillageEnemyArchetypeSystem.AerialRole aerialRoleOf(Mob mob) {
        if (mob == null || !isAerialEnemy(mob)) return null;
        return ACTIVE_AERIAL_ROLES.getOrDefault(mob.getUUID(), VillageEnemyArchetypeSystem.AerialRole.RAIDER);
    }

    /** Higher values are selected first by dedicated anti-air defenders. */
    public static int aerialThreatPriority(Mob mob) {
        return aerialThreatPriority(aerialRoleOf(mob));
    }

    public static int aerialThreatPriority(VillageEnemyArchetypeSystem.AerialRole role) {
        if (role == null) return 0;
        return switch (role) {
            case BOMBARDIER -> 300;
            case HARRIER -> 220;
            case RAIDER -> 140;
        };
    }

    public static int waveOf(Mob mob) {
        if (mob == null) return Math.max(1, wave);
        return Math.max(1, ACTIVE_WAVES.getOrDefault(mob.getUUID(), Math.max(1, wave)));
    }

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

    /** Resolves the authoritative raid roster without scanning the surrounding world volume. */
    public static List<Mob> activeEnemies(ServerLevel level) {
        if (level == null) return List.of();
        List<Mob> result = new ArrayList<>();
        for (UUID id : ACTIVE_ENEMIES) {
            Entity entity = level.getEntity(id);
            if (entity instanceof Mob mob && mob.isAlive()) result.add(mob);
        }
        return List.copyOf(result);
    }

    public static List<Mob> activeEnemiesNear(
            ServerLevel level,
            Vec3 origin,
            double radius,
            int limit,
            UUID excluded) {
        double radiusSquared = radius * radius;
        List<Mob> result = new ArrayList<>();
        for (Mob mob : activeEnemies(level)) {
            if (excluded != null && excluded.equals(mob.getUUID())) continue;
            if (mob.position().distanceToSqr(origin) <= radiusSquared) {
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

    private static int waveClearCoinReward(int day, int clearedWave) {
        return Math.max(6, 8 + Math.max(1, day) * 2 + Math.max(1, clearedWave) * 2);
    }

    public static int previewMaxWaves(int day) {
        return Math.min(7, 3 + Math.max(0, day - 1) / 4);
    }

    public static int previewWaveCount(int day, int previewWave, int players, VillageWaveTrait trait) {
        int soloBase = 4 + previewWave * 2 + VillageCampaignProgression.rosterDayBonus(day)
                + VillageWarfrontSystem.countBonus(day);
        int soloCount = trait.adjustedCount(soloBase);
        return VillageDifficultyTuning.scaleEnemyCount(soloCount, Math.max(1, players));
    }

    public static int previewBossCount(int day, int previewWave, int maximumWaves, int count) {
        return Math.min(Math.max(0, count),
                VillageWarfrontSystem.bonusBossCount(day, previewWave, maximumWaves));
    }

    public static int previewTotalEnemyCount(int day, int players) {
        int total = 0;
        int maximumWaves = previewMaxWaves(day);
        for (int previewWave = 1; previewWave <= maximumWaves; previewWave++) {
            VillageWaveTrait trait = VillageWaveTrait.select(day, previewWave);
            total += previewWaveCount(day, previewWave, players, trait);
        }
        return Math.max(1, total);
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
        if (VillagePlacedTurretSystem.count() == 0) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(
                    "§6[포탑 안내] §f현재 설치 포탑이 0기입니다. 낮에 북문 성벽 지휘 레버 → 새 포탑 배치에서 "
                            + "계열을 선택한 뒤 지정 포좌 또는 마을 지면을 두 번 우클릭해 설치할 수 있습니다."), false);
        }
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
        int flyingSpawned = 0;
        PlayerTeam raidTeam = ensureRaidTeam(server);
        VillageAttackPlanSystem.renderWaveArrival(level, day, wave, count);

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
            applyScaling(mob, spawned.archetype(), day, wave, boss);
            VillageEnemyArchetypeSystem.configure(
                    level, mob, spawned.archetype(), currentTrait, day, wave, boss);
            // Raid enemies are always interactive combat actors. A frozen/invulnerable flag is never
            // an authored raid state and must not survive spawn or runtime recovery.
            mob.setNoAi(false);
            mob.setInvulnerable(false);
            if (VillageEnemyArchetypeSystem.isFlying(mob)) {
                mob.addTag(AERIAL_ENEMY_TAG);
                VillageEnemyArchetypeSystem.AerialRole aerialRole =
                        VillageEnemyArchetypeSystem.aerialRole(day, wave, index, currentTrait);
                ACTIVE_AERIAL_ROLES.put(mob.getUUID(), aerialRole);
                mob.setCustomName(Component.literal("§b웨이브 " + wave + " · " + aerialRole.displayName()
                        + " §8[" + aerialRole.combatRole() + " · 성벽 우회]"));
            }
            if (boss) VillageBossAspectSystem.configure(level, mob, day, wave, index);
            mob.addTag(RAID_ENEMY_TAG);
            VillageWorldSystem.markAllowedGameMob(mob);
            server.getScoreboard().addPlayerToTeam(mob.getScoreboardName(), raidTeam);
            // Register authoritative combat metadata before addFreshEntity fires EntityJoinLevelEvent.
            ACTIVE_ARCHETYPES.put(mob.getUUID(), spawned.archetype());
            ACTIVE_WAVES.put(mob.getUUID(), wave);
            if (level.addFreshEntity(mob)) {
                ACTIVE_ENEMIES.add(mob.getUUID());
                if (VillageEnemyArchetypeSystem.isFlying(mob)) flyingSpawned++;
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
                        + "명 · 전장 총 " + ACTIVE_ENEMIES.size() + "명"
                        + (flyingSpawned > 0 ? " · §b공중 위협 " + flyingSpawned + "기§f" : "") + capped
                        + "\n§7" + currentTrait.description()
                        + "\n§b대응: " + currentTrait.counterHint()), false);
    }

    private static void applyScaling(
            Mob mob, VillageEnemyArchetypeSystem.Archetype archetype, int day, int currentWave, boolean boss) {
        int duration = 20 * 60 * 30;
        boolean sapper = archetype == VillageEnemyArchetypeSystem.Archetype.SAPPER;
        int healthTier = VillageCampaignProgression.enemyHealthTier(day, currentWave);
        int strengthTier = VillageCampaignProgression.enemyStrengthTier(day, currentWave);
        if (sapper) {
            healthTier = Math.max(0, healthTier - 2);
            strengthTier = Math.max(0, strengthTier - 2);
        }
        if (healthTier > 0 || boss) {
            mob.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, duration,
                    Math.min(17, healthTier + (boss ? 3 : 0))));
        }
        if (strengthTier > 0 || boss) {
            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration,
                    Math.min(6, strengthTier + (boss ? 1 : 0))));
        }
        // Movement speed is authored by enemy role and wave doctrine, never by raw campaign day.
        if (day >= 30) {
            mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration,
                    Math.min(2, Math.max(0, (day - 20) / 30))));
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
        Map<UUID, Integer> mercenaryPressure = new HashMap<>();

        for (UUID id : new HashSet<>(ACTIVE_ENEMIES)) {
            Entity entity = level.getEntity(id);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) continue;
            VillageEnemyArchetypeSystem.Archetype archetype = ACTIVE_ARCHETYPES.getOrDefault(
                    id, VillageEnemyArchetypeSystem.Archetype.GRUNT);
            updateEnemyOutline(server, mob);

            LivingEntity tauntTarget = activeTauntTarget(level, mob);
            if (tauntTarget != null) {
                mob.setTarget(tauntTarget);
                if (isAerialEnemy(mob)) {
                    directTauntedFlyingEnemy(server, level, mob, tauntTarget);
                } else {
                    directTauntedGroundEnemy(mob, tauntTarget);
                }
                continue;
            }

            VillageEnemyArchetypeSystem.tickAbility(
                    level, server, mob, archetype, currentTrait, abilityTicks);
            if (VillageEnemyArchetypeSystem.isBoss(archetype)) {
                VillageBossAspectSystem.tick(level, server, mob, abilityTicks);
            }

            if (isAerialEnemy(mob)) {
                directFlyingEnemy(server, level, mob, archetype, villageCenter);
                continue;
            }

            // Side/rear exterior movement is owned by VillageAttackPlanSystem on the same server tick.
            // Do not briefly retarget these mobs toward the north gate, a player, or a placed turret first.
            if (VillageAttackPlanSystem.ownsExteriorRouting(id, mob.blockPosition())) {
                mob.setTarget(null);
                continue;
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

            if (mob.getTarget() instanceof net.minecraft.world.entity.animal.golem.IronGolem mercenary
                    && VillageMercenarySystem.isCombatMercenary(mercenary)
                    && mob.distanceToSqr(mercenary) <= 24.0 * 24.0) {
                int currentPressure = mercenaryPressure.getOrDefault(mercenary.getUUID(), 0);
                if (currentPressure < VillageMercenarySystem.aggroCapacity(mercenary)) {
                    mercenaryPressure.put(mercenary.getUUID(), currentPressure + 1);
                    mob.getNavigation().moveTo(mercenary, 1.10);
                    continue;
                }
                mob.setTarget(null);
            }
            boolean objectiveLocked = archetype == VillageEnemyArchetypeSystem.Archetype.SAPPER
                    || archetype == VillageEnemyArchetypeSystem.Archetype.TOWER_HUNTER;
            if (!objectiveLocked) {
                net.minecraft.world.entity.animal.golem.IronGolem mercenary =
                        selectMercenaryTarget(level, mob, 18.0, mercenaryPressure);
                if (mercenary != null) {
                    mercenaryPressure.merge(mercenary.getUUID(), 1, Integer::sum);
                    mob.setTarget(mercenary);
                    mob.getNavigation().moveTo(mercenary, 1.10);
                    continue;
                }
            }

            boolean fortressAccess = gatePassable
                    || VillageAttackPlanSystem.hasInteriorAccess(id, mob.blockPosition());
            ServerPlayer nearbyPlayer = fortressAccess
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
                    villageCenter, mob.blockPosition(), fortressAccess, archetype);
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

    private static net.minecraft.world.entity.animal.golem.IronGolem selectMercenaryTarget(
            ServerLevel level, Mob enemy, double range, Map<UUID, Integer> pressure) {
        if (level == null || enemy == null || range <= 0.0) return null;
        double rangeSquared = range * range;
        return VillageMercenarySystem.loadedMercenaries(level).stream()
                .filter(mercenary -> enemy.distanceToSqr(mercenary) <= rangeSquared)
                .filter(enemy::hasLineOfSight)
                .filter(mercenary -> pressure.getOrDefault(mercenary.getUUID(), 0)
                        < VillageMercenarySystem.aggroCapacity(mercenary))
                .min(Comparator.comparingDouble(mercenary -> {
                    double distance = enemy.distanceToSqr(mercenary);
                    VillageMercenarySystem.MercenaryClass kind = VillageMercenarySystem.classOf(mercenary);
                    double weight = switch (kind == null ? VillageMercenarySystem.MercenaryClass.STRIKER : kind) {
                        case BASTION -> 0.52;
                        case STRIKER -> 0.90;
                        case RANGER -> 1.18;
                        case MEDIC -> 1.38;
                    };
                    return distance * weight;
                }))
                .orElse(null);
    }

    private static LivingEntity activeTauntTarget(ServerLevel level, Mob enemy) {
        TauntState state = FORCED_TAUNTS.get(enemy.getUUID());
        if (state == null) return null;
        if (level.getGameTime() > state.untilGameTime()) {
            FORCED_TAUNTS.remove(enemy.getUUID());
            return null;
        }
        Entity entity = level.getEntity(state.target());
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            FORCED_TAUNTS.remove(enemy.getUUID());
            return null;
        }
        if (target instanceof ServerPlayer player
                && (player.isSpectator() || VillageRespawnSystem.isDowned(player))) {
            FORCED_TAUNTS.remove(enemy.getUUID());
            return null;
        }
        if (target instanceof Mob mercenary && !VillageMercenarySystem.isCombatMercenary(mercenary)) {
            FORCED_TAUNTS.remove(enemy.getUUID());
            return null;
        }
        return target;
    }

    private static void directTauntedGroundEnemy(Mob mob, LivingEntity target) {
        mob.setTarget(target);
        mob.getLookControl().setLookAt(target, 45.0f, 45.0f);
        mob.getNavigation().moveTo(target, 1.32);
        Vec3 horizontal = new Vec3(target.getX() - mob.getX(), 0.0, target.getZ() - mob.getZ());
        if (horizontal.lengthSqr() > 16.0) {
            Vec3 push = horizontal.normalize().scale(0.055);
            Vec3 current = mob.getDeltaMovement();
            mob.setDeltaMovement(current.x * 0.78 + push.x, current.y, current.z * 0.78 + push.z);
        }
    }

    private static void directTauntedFlyingEnemy(
            MinecraftServer server, ServerLevel level, Mob mob, LivingEntity target) {
        UUID id = mob.getUUID();
        VillageEnemyArchetypeSystem.AerialRole role =
                Optional.ofNullable(aerialRoleOf(mob)).orElse(VillageEnemyArchetypeSystem.AerialRole.RAIDER);
        AerialStrike strike = AERIAL_STRIKES.get(id);
        if (strike != null) {
            if (abilityTicks < strike.impactTick()) {
                Vec3 dive = strike.point().add(0.0, 3.0, 0.0);
                moveFlyingToward(mob, strike.point(), dive, aerialDiveSpeed(role));
                return;
            }
            if (!strike.resolved()) {
                resolveAerialStrike(server, level, mob, strike);
                strike = strike.resolvedCopy();
                AERIAL_STRIKES.put(id, strike);
            }
            if (abilityTicks < strike.recoveryUntilTick()) {
                Vec3 recover = strike.point().add(0.0, 9.0, 0.0);
                moveFlyingToward(mob, strike.point(), recover, aerialRecoverySpeed(role));
                return;
            }
            AERIAL_STRIKES.remove(id);
        }

        Vec3 targetPoint = target.position().add(0.0, Math.max(1.0, target.getBbHeight() * 0.55), 0.0);
        double angle = abilityTicks * 0.080 + Math.floorMod(id.hashCode(), 360) * Math.PI / 180.0;
        Vec3 cruise = targetPoint.add(Math.cos(angle) * 4.5, 5.0, Math.sin(angle) * 4.5);
        int cadence = 54;
        if (target instanceof ServerPlayer
                && Math.floorMod(abilityTicks + id.hashCode(), cadence) == 0
                && mob.position().distanceToSqr(targetPoint) <= 18.0 * 18.0) {
            beginAerialStrike(level, mob, role, target.position(), null);
            return;
        }
        moveFlyingToward(mob, targetPoint, cruise, 1.42);
    }

    private static void directFlyingEnemy(
            MinecraftServer server, ServerLevel level, Mob mob,
            VillageEnemyArchetypeSystem.Archetype archetype, BlockPos villageCenter) {
        if (villageCenter == null) return;
        UUID id = mob.getUUID();
        VillageEnemyArchetypeSystem.AerialRole role = aerialRoleOf(mob);
        if (role == null) role = VillageEnemyArchetypeSystem.AerialRole.RAIDER;
        // One owner only: vanilla Phantom targeting and all ground-elite navigation stay disabled.
        mob.setTarget(null);

        AerialStrike strike = AERIAL_STRIKES.get(id);
        if (strike != null) {
            if (abilityTicks < strike.impactTick()) {
                Vec3 dive = strike.point().add(0.0, strike.targetsBuilding() ? 4.5 : 3.0, 0.0);
                moveFlyingToward(mob, strike.point(), dive, aerialDiveSpeed(role));
                return;
            }
            if (!strike.resolved()) {
                resolveAerialStrike(server, level, mob, strike);
                strike = strike.resolvedCopy();
                AERIAL_STRIKES.put(id, strike);
            }
            if (abilityTicks < strike.recoveryUntilTick()) {
                Vec3 recover = strike.point().add(0.0, strike.targetsBuilding() ? 12.5 : 11.0, 0.0);
                moveFlyingToward(mob, strike.point(), recover, aerialRecoverySpeed(role));
                return;
            }
            AERIAL_STRIKES.remove(id);
        }

        double ingressRadius = Math.max(10.0, VillageWorldSystem.FORTRESS_RADIUS - 10.0);
        double centerDx = mob.getX() - (villageCenter.getX() + 0.5);
        double centerDz = mob.getZ() - (villageCenter.getZ() + 0.5);
        if (centerDx * centerDx + centerDz * centerDz > ingressRadius * ingressRadius) {
            Vec3 ingress = new Vec3(villageCenter.getX() + 0.5,
                    villageCenter.getY() + 12.0, villageCenter.getZ() + 0.5);
            moveFlyingToward(mob, ingress, ingress,
                    role == VillageEnemyArchetypeSystem.AerialRole.HARRIER ? 1.55 : 1.38);
            return;
        }

        int phase = Math.floorMod(abilityTicks + id.hashCode(), aerialCadence(role));
        // Bombardiers deliberately ignore nearby defenders while an internal facility still exists.
        if (role == VillageEnemyArchetypeSystem.AerialRole.BOMBARDIER) {
            VillageProgressionSystem.Building building = chooseTarget(
                    villageCenter, mob.blockPosition(), true, archetype);
            if (building != null && building != VillageProgressionSystem.Building.WALLS) {
                BlockPos targetBlock = VillageWorldSystem.buildingCenter(building);
                Vec3 target = Vec3.atCenterOf(targetBlock).add(0.0, 1.0, 0.0);
                double angle = abilityTicks * 0.034 + Math.floorMod(id.hashCode(), 360) * Math.PI / 180.0;
                Vec3 cruise = target.add(Math.cos(angle) * 9.5, 11.5, Math.sin(angle) * 9.5);
                if (phase == 0 && mob.position().distanceToSqr(cruise) <= 24.0 * 24.0) {
                    beginAerialStrike(level, mob, role, target, building);
                    return;
                }
                moveFlyingToward(mob, target, cruise, 1.08);
                return;
            }
        }

        ServerPlayer player = nearestFlyingPriorityPlayer(server, mob, aerialPlayerSearchRange(role));
        if (player != null) {
            if (phase == 0) {
                beginAerialStrike(level, mob, role, player.position(), null);
                return;
            }
            double turn = role == VillageEnemyArchetypeSystem.AerialRole.HARRIER ? 0.078 : 0.055;
            double angle = abilityTicks * turn + Math.floorMod(id.hashCode(), 360) * Math.PI / 180.0;
            double radius = role == VillageEnemyArchetypeSystem.AerialRole.HARRIER ? 6.0
                    : 7.0 + Math.floorMod(id.hashCode(), 4);
            double altitude = role == VillageEnemyArchetypeSystem.AerialRole.HARRIER ? 6.5
                    : 7.5 + Math.floorMod(id.hashCode() >>> 4, 4);
            Vec3 cruise = player.position().add(Math.cos(angle) * radius, altitude, Math.sin(angle) * radius);
            moveFlyingToward(mob, player.position().add(0.0, 1.0, 0.0), cruise,
                    role == VillageEnemyArchetypeSystem.AerialRole.HARRIER ? 1.38 : 1.20);
            return;
        }

        VillageProgressionSystem.Building targetBuilding = chooseTarget(
                villageCenter, mob.blockPosition(), true, archetype);
        if (targetBuilding == null || targetBuilding == VillageProgressionSystem.Building.WALLS) return;
        BlockPos targetBlock = VillageWorldSystem.buildingCenter(targetBuilding);
        Vec3 target = Vec3.atCenterOf(targetBlock).add(0.0, 1.0, 0.0);
        double angle = abilityTicks * 0.042 + Math.floorMod(id.hashCode(), 360) * Math.PI / 180.0;
        Vec3 cruise = target.add(Math.cos(angle) * 8.0, 9.5, Math.sin(angle) * 8.0);
        if (phase == 0 && mob.position().distanceToSqr(cruise) <= 22.0 * 22.0) {
            beginAerialStrike(level, mob, role, target, targetBuilding);
            return;
        }
        moveFlyingToward(mob, target, cruise, 1.16);
    }

    private static int aerialCadence(VillageEnemyArchetypeSystem.AerialRole role) {
        return switch (role) {
            case BOMBARDIER -> 112;
            case HARRIER -> 62;
            case RAIDER -> AERIAL_ASSAULT_CADENCE;
        };
    }

    private static int aerialWarningTicks(VillageEnemyArchetypeSystem.AerialRole role) {
        return switch (role) {
            case BOMBARDIER -> 24;
            case HARRIER -> 12;
            case RAIDER -> AERIAL_WARNING_TICKS;
        };
    }


    private static double aerialStrikeRadius(VillageEnemyArchetypeSystem.AerialRole role) {
        return switch (role) {
            case BOMBARDIER -> 3.10;
            case HARRIER -> 2.15;
            case RAIDER -> AERIAL_PLAYER_STRIKE_RADIUS;
        };
    }

    private static int aerialRecoveryTicks(VillageEnemyArchetypeSystem.AerialRole role) {
        return switch (role) {
            case BOMBARDIER -> 44;
            case HARRIER -> 24;
            case RAIDER -> AERIAL_RECOVERY_TICKS;
        };
    }

    private static double aerialPlayerSearchRange(VillageEnemyArchetypeSystem.AerialRole role) {
        return role == VillageEnemyArchetypeSystem.AerialRole.HARRIER ? 38.0 : 28.0;
    }

    private static double aerialDiveSpeed(VillageEnemyArchetypeSystem.AerialRole role) {
        return switch (role) {
            case BOMBARDIER -> 1.34;
            case HARRIER -> 1.78;
            case RAIDER -> 1.52;
        };
    }

    private static double aerialRecoverySpeed(VillageEnemyArchetypeSystem.AerialRole role) {
        return switch (role) {
            case BOMBARDIER -> 1.20;
            case HARRIER -> 1.58;
            case RAIDER -> 1.38;
        };
    }

    private static void beginAerialStrike(
            ServerLevel level, Mob mob, VillageEnemyArchetypeSystem.AerialRole role,
            Vec3 point, VillageProgressionSystem.Building building) {
        int impactTick = abilityTicks + aerialWarningTicks(role);
        AerialStrike strike = new AerialStrike(point, building, impactTick,
                impactTick + aerialRecoveryTicks(role), false);
        AERIAL_STRIKES.put(mob.getUUID(), strike);
        double dangerRadius = building == null ? aerialStrikeRadius(role) : 0.0;
        VillageDefenseEffectSystem.aerialAssaultWarning(
                level, point, role, building != null, aerialWarningTicks(role), dangerRadius);
        Vec3 dive = point.add(0.0, building == null ? 3.0 : 4.5, 0.0);
        moveFlyingToward(mob, point, dive, aerialDiveSpeed(role));
    }

    private static void resolveAerialStrike(
            MinecraftServer server, ServerLevel level, Mob mob, AerialStrike strike) {
        VillageEnemyArchetypeSystem.AerialRole role = aerialRoleOf(mob);
        if (role == null) role = VillageEnemyArchetypeSystem.AerialRole.RAIDER;
        if (strike.targetsBuilding()) {
            VillageProgressionSystem.Building building = strike.building();
            if (building != null && VillageProgressionSystem.isOperational(building)) {
                int day = VillageCouncilState.currentDay();
                float multiplier = currentTrait.structureDamageMultiplier()
                        * VillageWarfrontSystem.structureDamageMultiplier(day)
                        * VillageBossAspectSystem.structureMultiplier(mob)
                        * VillageDifficultyTuning.earlyStructureMultiplier(day)
                        * VillageDifficultyTuning.defenderStateStructureMultiplier(server);
                float roleMultiplier = switch (role) {
                    case BOMBARDIER -> 1.75f;
                    case HARRIER -> 0.58f;
                    case RAIDER -> 1.0f;
                };
                int damage = Math.max(1, Math.round((4 + wave + Math.min(16, day) * 0.45f)
                        * multiplier * roleMultiplier));
                VillageProgressionSystem.damageBuilding(server, building, damage);
            }
            VillageDefenseEffectSystem.aerialAssaultImpact(level, strike.point(), role, true, 0.0);
            return;
        }

        double radius = aerialStrikeRadius(role);
        double radiusSquared = radius * radius;
        float roleMultiplier = switch (role) {
            case BOMBARDIER -> 0.85f;
            case HARRIER -> 0.82f;
            case RAIDER -> 1.0f;
        };
        float damage = Math.max(2.0f, (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * roleMultiplier);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() != level || !player.isAlive() || player.isSpectator()
                    || VillageRespawnSystem.isDowned(player)) continue;
            if (player.position().distanceToSqr(strike.point()) <= radiusSquared) {
                player.hurtServer(level, level.damageSources().mobAttack(mob), damage);
            }
        }
        VillageDefenseEffectSystem.aerialAssaultImpact(level, strike.point(), role, false, radius);
    }

    private static void moveFlyingToward(Mob mob, Vec3 lookAt, Vec3 wanted, double speed) {
        mob.getLookControl().setLookAt(lookAt.x, lookAt.y, lookAt.z, 45.0f, 45.0f);
        mob.getMoveControl().setWantedPosition(wanted.x, wanted.y, wanted.z, speed);
        Vec3 delta = wanted.subtract(mob.position());
        if (delta.lengthSqr() > 0.04) {
            double steeringSpeed = 0.13 + Math.min(1.8, Math.max(0.5, speed)) * 0.045;
            Vec3 steering = delta.normalize().scale(steeringSpeed);
            mob.setDeltaMovement(mob.getDeltaMovement().scale(0.55).add(steering.scale(0.45)));
        }
    }

    private static ServerPlayer nearestFlyingPriorityPlayer(MinecraftServer server, Mob mob, double range) {
        ServerPlayer chosen = null;
        double chosenDistance = range * range;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() != mob.level() || !player.isAlive() || player.isSpectator()
                    || VillageRespawnSystem.isDowned(player)) continue;
            double distance = player.distanceToSqr(mob);
            if (distance <= chosenDistance && mob.hasLineOfSight(player)) {
                chosenDistance = distance;
                chosen = player;
            }
        }
        return chosen;
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
                    && !player.isSpectator()
                    && !VillageRespawnSystem.isDowned(player)
                    && player.distanceToSqr(mob) <= 160.0 * 160.0
                    && player.hasLineOfSight(mob)) {
                visibleToAnyPlayer = true;
                break;
            }
        }
        // Fortress walls should not turn cleanup into hide-and-seek. Every occluded raid enemy is
        // outlined in the raid team's red color; bosses stay outlined even when directly visible.
        mob.setGlowingTag(isBossEnemy(mob) || !visibleToAnyPlayer);
    }

    private static PlayerTeam ensureRaidTeam(MinecraftServer server) {
        PlayerTeam team = server.getScoreboard().getPlayerTeam(RAID_TEAM_NAME);
        if (team == null) team = server.getScoreboard().addPlayerTeam(RAID_TEAM_NAME);
        team.setColor(Optional.of(TeamColor.byName("red")));
        team.setAllowFriendlyFire(false);
        return team;
    }

    private static void repairInvalidEnemyFlags(MinecraftServer server) {
        ServerLevel level = server.overworld();
        for (UUID id : new HashSet<>(ACTIVE_ENEMIES)) {
            Entity entity = level.getEntity(id);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) continue;
            boolean hadNoAi = mob.isNoAi();
            boolean hadInvulnerable = mob.isInvulnerable();
            if (!hadNoAi && !hadInvulnerable) continue;

            // No raid archetype intentionally uses NoAI or vanilla invulnerability. If either flag
            // appears, the entity has entered an invalid combat state: clear it before routing.
            mob.setNoAi(false);
            mob.setInvulnerable(false);
            mob.getNavigation().stop();
            ServerPlayer nearest = nearestAnyCombatPlayer(server, mob);
            if (nearest != null) {
                mob.setTarget(nearest);
                mob.getNavigation().moveTo(nearest, 1.16);
            }
            FINAL_ENEMY_PROGRESS.put(id, progressSnapshot(mob, 0));
            VillageGuardians.LOGGER.warn(
                    "Repaired invalid raid enemy state: uuid={}, type={}, hadNoAi={}, hadInvulnerable={}",
                    id, mob.getType(), hadNoAi, hadInvulnerable);
        }
    }

    private static void recoverFrozenFinalEnemies(MinecraftServer server) {
        if (waveElapsedTicks % 20 != 0) return;

        ServerLevel level = server.overworld();
        Set<UUID> activeNow = new HashSet<>(ACTIVE_ENEMIES);
        FINAL_ENEMY_PROGRESS.keySet().removeIf(id -> !activeNow.contains(id));
        BlockPos villageCenter = VillageCouncilState.villageCenter().orElse(null);
        BlockPos rally = VillageWorldSystem.northInnerApproach();
        boolean repaired = false;
        int offsetIndex = 0;
        int recovered = 0;

        for (UUID id : activeNow) {
            if (recovered >= MAX_STALL_RECOVERIES_PER_PASS) break;
            Entity entity = level.getEntity(id);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) continue;

            EnemyProgress previous = FINAL_ENEMY_PROGRESS.get(id);
            if (previous == null) {
                FINAL_ENEMY_PROGRESS.put(id, progressSnapshot(mob, 0));
                continue;
            }

            LivingEntity currentTarget = mob.getTarget();
            UUID targetId = currentTarget == null ? null : currentTarget.getUUID();
            float targetHealth = currentTarget == null ? -1.0f : currentTarget.getHealth();
            boolean moved = mob.position().distanceToSqr(previous.position())
                    > FINAL_ENEMY_PROGRESS_DISTANCE_SQR;
            boolean healthChanged = Math.abs(mob.getHealth() - previous.health()) > 0.01f;
            boolean targetChanged = !java.util.Objects.equals(previous.targetId(), targetId);
            boolean targetHealthChanged = !targetChanged && targetId != null
                    && Math.abs(targetHealth - previous.targetHealth()) > 0.01f;
            if (moved || healthChanged || targetChanged || targetHealthChanged) {
                FINAL_ENEMY_PROGRESS.put(id, progressSnapshot(mob, 0));
                continue;
            }
            if (waveElapsedTicks - previous.lastProgressTick() < FINAL_ENEMY_STALL_TICKS) continue;

            VillageEnemyArchetypeSystem.Archetype archetype = ACTIVE_ARCHETYPES.getOrDefault(
                    id, VillageEnemyArchetypeSystem.Archetype.GRUNT);
            if (!shouldRecoverStalledEnemy(mob, archetype, villageCenter)) continue;

            int slot = offsetIndex++;
            double xOffset = (slot - 1.5) * 2.0;
            double zOffset = (slot & 1) == 0 ? 0.0 : 2.0;
            mob.setNoAi(false);
            mob.setInvulnerable(false);
            mob.stopRiding();
            mob.getNavigation().stop();

            // snapTo is suitable before addFreshEntity, but using it on an already tracked mob can
            // leave the client looking at a stale body while the server moved the hitbox elsewhere.
            // teleportTo emits an authoritative tracked-entity relocation.
            mob.teleportTo(level,
                    rally.getX() + 0.5 + xOffset, rally.getY(), rally.getZ() + 0.5 + zOffset,
                    Set.of(), mob.getYRot(), mob.getXRot(), true);

            ServerPlayer nearest = nearestAnyCombatPlayer(server, mob);
            if (nearest != null) {
                mob.setTarget(nearest);
                mob.getNavigation().moveTo(nearest, 1.16);
            } else {
                mob.setTarget(null);
            }
            FINAL_ENEMY_PROGRESS.put(id, progressSnapshot(mob, previous.recoveryAttempts() + 1));
            recovered++;
            repaired = true;
        }

        if (repaired) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(
                    "§e[전투 상태 복구] §f마지막 적의 AI 진행이 멈춰 북문 전선에서 다시 교전시켰습니다."), false);
        }
    }

    private static boolean shouldRecoverStalledEnemy(
            Mob mob,
            VillageEnemyArchetypeSystem.Archetype archetype,
            BlockPos villageCenter) {
        if (!mob.getNavigation().isDone()) return false;

        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive()) {
            double distance = mob.distanceToSqr(target);
            if (isMeleePursuer(archetype)) {
                // We arrive here only after 12 seconds with no movement, no self-health change and
                // no target-health change. "Close to a target" is therefore not proof of combat;
                // this was exactly the state of the frozen baby-zombie/GRUNT seen in playtest.
                return true;
            }
            // A stationary ranged actor is legitimate only while it has a usable firing solution.
            return distance > 48.0 * 48.0 || !mob.hasLineOfSight(target);
        }

        if (villageCenter == null) return true;
        boolean fortressAccess = VillageAttackPlanSystem.hasInteriorAccess(mob.getUUID(), mob.blockPosition())
                || VillageWorldSystem.isNorthGatePassable((ServerLevel) mob.level())
                || !VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.WALLS);
        VillageProgressionSystem.Building building = chooseTarget(
                villageCenter, mob.blockPosition(), fortressAccess, archetype);
        return building == null || !VillageFortressBuildings.isTouchingStructure(
                villageCenter, building, mob.blockPosition());
    }

    private static boolean isMeleePursuer(VillageEnemyArchetypeSystem.Archetype archetype) {
        return switch (archetype) {
            case GRUNT, RUSHER, BULWARK, SAPPER, SHIELDBREAKER,
                    SIEGE_BEAST, IRON_WARLORD, DREAD_KNIGHT -> true;
            default -> false;
        };
    }

    private static ServerPlayer nearestAnyCombatPlayer(MinecraftServer server, Mob mob) {
        ServerPlayer chosen = null;
        double chosenDistance = Double.MAX_VALUE;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() != mob.level() || !player.isAlive() || player.isSpectator()
                    || VillageRespawnSystem.isDowned(player)) continue;
            double distance = player.distanceToSqr(mob);
            if (distance < chosenDistance) {
                chosenDistance = distance;
                chosen = player;
            }
        }
        return chosen;
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
        boolean finalCampaignVictory = VillageCampaignProgression.isFinalSiege(day);
        float campaignReward = VillageWarfrontSystem.rewardMultiplier(day);
        int supplies = Math.round((140 + day * 32)
                * VillageProgressionSystem.raidRewardMultiplierPercent() / 100.0f * campaignReward);
        int targetLevel = Math.max(1, Math.min(RpgProgress.MAX_LEVEL - 1,
                VillageCampaignProgression.targetPlayerLevel(day)));
        int targetLevels = VillageCampaignProgression.targetLevelsForDay(day);
        int xp = Math.max(1, Math.round(
                RpgProgress.experienceRequiredAtLevel(targetLevel) * 0.18f * targetLevels));
        int coins = Math.round((60 + day * 14) * campaignReward);

        clearState();
        VillageProgressionSystem.addSupplies(server, supplies, "제 " + day + "일 방어 성공");
        java.util.Set<UUID> participants = VillageProgressionSystem.nightParticipants(server);
        VillageProgressionSystem.awardRaidCoins(server, coins);
        for (UUID playerId : participants) {
            VillageCouncilState.grantExperience(server, playerId, xp);
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) VillageRpgSystem.refreshPlayerPassive(player);
        }
        VillageProgressionSystem.healRaidParty(server, true);
        VillageCouncilState.completeRaid(server);
        if (finalCampaignVictory) {
            VillageUiService.openCampaignVictoryForAll(server);
        } else {
            VillageUiService.openRepairSummaryForAll(server);
            VillageRelicSystem.openPendingChoicesForParty(server);
        }
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
        ACTIVE_AERIAL_ROLES.remove(uuid);
        AERIAL_STRIKES.remove(uuid);
        FORCED_TAUNTS.remove(uuid);
        FINAL_ENEMY_PROGRESS.remove(uuid);
        VillageAttackPlanSystem.forget(uuid);
        VillageEnemyEliteSystem.forget(uuid);
        VillageSiegeBossSystem.forget(uuid);
        VillageBossAspectSystem.forget(uuid);
        VillageEnemyArchetypeSystem.forget(uuid);
        VillageWorldSystem.unmarkAllowedGameMob(uuid);
        VillageHealthDisplaySystem.forgetEnemy(uuid);
        if (entity != null) {
            entity.setGlowingTag(false);
            PlayerTeam team = server.getScoreboard().getPlayerTeam(RAID_TEAM_NAME);
            if (team != null) server.getScoreboard().removePlayerFromTeam(entity.getScoreboardName(), team);
        }
    }

    private record TauntState(UUID target, long untilGameTime) {}

    private static EnemyProgress progressSnapshot(Mob mob, int recoveryAttempts) {
        LivingEntity target = mob == null ? null : mob.getTarget();
        return new EnemyProgress(
                mob == null ? Vec3.ZERO : mob.position(),
                mob == null ? 0.0f : mob.getHealth(),
                target == null ? null : target.getUUID(),
                target == null ? -1.0f : target.getHealth(),
                waveElapsedTicks,
                Math.max(0, recoveryAttempts));
    }

    private record EnemyProgress(
            Vec3 position,
            float health,
            UUID targetId,
            float targetHealth,
            int lastProgressTick,
            int recoveryAttempts) {}

    private record AerialStrike(
            Vec3 point, VillageProgressionSystem.Building building, int impactTick,
            int recoveryUntilTick, boolean resolved) {
        boolean targetsBuilding() { return building != null; }
        AerialStrike resolvedCopy() {
            return new AerialStrike(point, building, impactTick, recoveryUntilTick, true);
        }
    }

    private static void clearState() {
        ACTIVE_ENEMIES.clear();
        ACTIVE_ARCHETYPES.clear();
        ACTIVE_WAVES.clear();
        ACTIVE_AERIAL_ROLES.clear();
        AERIAL_STRIKES.clear();
        FORCED_TAUNTS.clear();
        FINAL_ENEMY_PROGRESS.clear();
        VillageAttackPlanSystem.clearRaidState();
        VillageEnemyEliteSystem.clearRaidState();
        VillageSiegeBossSystem.clearRaidState();
        VillageBossAspectSystem.reset();
        VillageEnemyArchetypeSystem.resetRaidState();
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
