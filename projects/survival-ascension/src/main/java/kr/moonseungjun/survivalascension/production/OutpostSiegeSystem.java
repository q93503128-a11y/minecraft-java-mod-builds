package kr.moonseungjun.survivalascension.production;

import kr.moonseungjun.survivalascension.apex.ApexHuntSystem;
import kr.moonseungjun.survivalascension.endgame.AscensionTrialSystem;
import kr.moonseungjun.survivalascension.expedition.ExpeditionIncidentSystem;
import kr.moonseungjun.survivalascension.expedition.ExpeditionOperationSystem;
import kr.moonseungjun.survivalascension.progress.SkillProgressionService;
import kr.moonseungjun.survivalascension.progress.SkillType;
import kr.moonseungjun.survivalascension.world.WorldAscensionData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Optional physical-outpost defense encounter. The objective is not a kill counter alone:
 * siege mobs that hold the six-block command perimeter build breach pressure and can fail the defense.
 * All spawning and structure checks are server-authoritative and loaded-chunk only.
 */
public final class OutpostSiegeSystem {
    public static final int START_RADIUS = 4;
    public static final int DEFENSE_RADIUS = 64;
    public static final int BREACH_RADIUS = 6;
    public static final int BREACH_LIMIT = 200;
    public static final int SUPPLY_CHARGE_COST = 1;

    private static final String READY_TICK_KEY = "survivalascension_outpost_siege_ready";
    private static final String INCIDENT_READY_TICK_KEY = "survivalascension_expedition_incident_ready";
    private static final String APEX_READY_TICK_KEY = "survivalascension_apex_hunt_ready";
    private static final String TRIAL_READY_TICK_KEY = "survivalascension_ascension_trial_ready";
    private static final String SIEGE_OWNER_KEY = "survivalascension_outpost_siege_owner";
    private static final String SIEGE_WAVE_KEY = "survivalascension_outpost_siege_wave";
    private static final int TOTAL_WAVES = 3;
    private static final int START_COOLDOWN_TICKS = 4800;
    private static final int SIEGE_TIMEOUT_TICKS = 4800;
    private static final int ENCOUNTER_EXCLUSION_TICKS = SIEGE_TIMEOUT_TICKS + 200;
    private static final int OWNER_GRACE_TICKS = 200;
    private static final int WAVE_DELAY_TICKS = 60;
    private static final int ENGAGE_RADIUS = 16;
    private static final int RECALL_RADIUS = 80;
    private static final int SPAWN_MIN_RADIUS = 26;
    private static final int SPAWN_RADIUS_SPAN = 9;
    private static final int EXCLUSION_RADIUS = 96;

    private static final Map<UUID, Siege> ACTIVE = new HashMap<>();
    private static int ticker;

    private OutpostSiegeSystem() {}

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static void startOrStatus(ServerPlayer player) {
        Siege current = ACTIVE.get(player.getUUID());
        if (current != null) {
            sendStatus(player);
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) return;
        if (player.isCreative() || player.isSpectator()) {
            player.sendSystemMessage(Component.literal("§c[전초 방어전] §f크리에이티브/관전자 상태에서는 시작할 수 없습니다."));
            return;
        }
        if (ApexHuntSystem.isActive(player) || AscensionTrialSystem.isActive(player)
                || ExpeditionOperationSystem.isActive(player) || ExpeditionIncidentSystem.isActive(player)) {
            player.sendSystemMessage(Component.literal("§c[전초 방어전] §f현장 사건·원정 작전·정점 사냥·승천 시련 중에는 방어전을 시작할 수 없습니다."));
            return;
        }

        long now = level.getGameTime();
        long ready = player.getPersistentData().getLongOr(READY_TICK_KEY, 0L);
        if (now < ready) {
            long seconds = Math.max(1L, (ready - now + 19L) / 20L);
            player.sendSystemMessage(Component.literal("§c[전초 방어전] §f다음 방어 준비까지 §e" + seconds + "초§f 남았습니다."));
            return;
        }

        OutpostData.OutpostEntry outpost = OutpostService.nearestActiveOutpost(player, START_RADIUS);
        if (outpost == null) {
            player.sendSystemMessage(Component.literal("§c[전초 방어전] §f활성 전초기지의 앵커 배럴 §e4블록 안§f에서 시작해야 합니다."));
            return;
        }
        for (Siege active : ACTIVE.values()) {
            if (active.level == level && active.anchor.distSqr(outpost.pos()) < EXCLUSION_RADIUS * EXCLUSION_RADIUS) {
                player.sendSystemMessage(Component.literal("§c[전초 방어전] §f근처 전초에서 다른 방어전이 진행 중입니다. §7(96블록 간격 필요)"));
                return;
            }
        }

        ProductionData production = ProductionData.get(player);
        if (production.supplyCharges(player) < SUPPLY_CHARGE_COST) {
            player.sendSystemMessage(Component.literal("§c[전초 방어전] §f방어 준비에는 §e현장 보급권 1개§f가 필요합니다."));
            return;
        }

        int stage = Math.max(1, WorldAscensionData.get(level.getServer()).stage());
        Siege siege = new Siege(player.getUUID(), level, outpost.dimension(), outpost.pos(), stage,
                now + SIEGE_TIMEOUT_TICKS);
        siege.wave = 1;
        if (!spawnWave(siege)) {
            cleanupMobs(siege);
            closeBossBar(siege);
            player.sendSystemMessage(Component.literal("§c[전초 방어전] §f전초 외곽에 충분한 습격대를 배치할 열린 로딩 지형이 없습니다. §7보급권은 소비하지 않았습니다."));
            return;
        }
        if (!production.consumeSupplyCharge(player)) {
            cleanupMobs(siege);
            closeBossBar(siege);
            player.sendSystemMessage(Component.literal("§c[전초 방어전] §f보급권 상태가 바뀌어 시작을 취소했습니다."));
            return;
        }

        var persistent = player.getPersistentData();
        persistent.putLong(READY_TICK_KEY, now + START_COOLDOWN_TICKS);
        long exclusion = now + ENCOUNTER_EXCLUSION_TICKS;
        persistent.putLong(INCIDENT_READY_TICK_KEY, Math.max(persistent.getLongOr(INCIDENT_READY_TICK_KEY, 0L), exclusion));
        persistent.putLong(APEX_READY_TICK_KEY, Math.max(persistent.getLongOr(APEX_READY_TICK_KEY, 0L), exclusion));
        persistent.putLong(TRIAL_READY_TICK_KEY, Math.max(persistent.getLongOr(TRIAL_READY_TICK_KEY, 0L), exclusion));

        ACTIVE.put(player.getUUID(), siege);
        siege.bossBar.addPlayer(player);
        siege.bossBar.setVisible(true);
        updateBossBar(siege);
        player.sendSystemMessage(Component.literal("§c[전초 방어전 시작] §f보급권1 소비 · 3개 공세를 막으세요."));
        player.sendSystemMessage(Component.literal("§7적이 앵커 배럴 반경 §e" + BREACH_RADIUS + "블록§7을 점유하면 돌파 압력이 상승합니다. §c"
                + BREACH_LIMIT + "§7에 도달하면 방어 실패입니다."));
        player.sendSystemMessage(Component.literal("§7습격대는 플레이어를 멀리 쫓기보다 전초 앵커를 향합니다. 앵커 근처에서 맞서 싸워 진입을 끊으세요."));
    }

    public static void sendStatus(ServerPlayer player) {
        Siege siege = ACTIVE.get(player.getUUID());
        if (siege == null) {
            player.sendSystemMessage(Component.literal("§c[전초 방어전] §f진행 중인 방어전 없음 §7· 활성 전초4블록 안에서 보급권1로 시작"));
            return;
        }
        long remain = Math.max(0L, siege.deadline - siege.level.getGameTime());
        player.sendSystemMessage(Component.literal("§c[전초 방어전] §f공세 §e" + siege.wave + "/" + TOTAL_WAVES
                + " §7· 적 §f" + siege.mobIds.size() + " §7· 돌파압력 §c" + siege.breachPressure + "/" + BREACH_LIMIT
                + " §7· 남은시간 §f" + ((remain + 19L) / 20L) + "초"));
        player.sendSystemMessage(Component.literal("  §7앵커 " + siege.anchor.getX() + ", " + siege.anchor.getY() + ", " + siege.anchor.getZ()
                + " · 방어권64 · 돌파권6 · 구조 유지 필수"));
    }

    public static void onServerTick(ServerTickEvent.Pre event) {
        removeStaleServerSieges(event.getServer());
        if (++ticker < 5) return;
        ticker = 0;
        for (Siege siege : new ArrayList<>(ACTIVE.values())) tickSiege(event.getServer(), siege);
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        Siege siege = ACTIVE.get(player.getUUID());
        if (siege != null) fail(siege, player, "방어전 중 사망했습니다.");
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Siege siege = ACTIVE.remove(event.getEntity().getUUID());
        if (siege != null) {
            cleanupMobs(siege);
            closeBossBar(siege);
        }
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob mob)) return;
        String ownerText = mob.getPersistentData().getStringOr(SIEGE_OWNER_KEY, "");
        if (ownerText.isEmpty()) return;
        UUID owner;
        try {
            owner = UUID.fromString(ownerText);
        } catch (IllegalArgumentException ignored) {
            event.setCanceled(true);
            return;
        }
        Siege siege = ACTIVE.get(owner);
        if (siege == null || siege.level != level || !siege.mobIds.contains(mob.getUUID())) event.setCanceled(true);
    }

    private static void tickSiege(MinecraftServer server, Siege siege) {
        if (ACTIVE.get(siege.owner) != siege) return;
        long now = siege.level.getGameTime();
        ServerPlayer owner = server.getPlayerList().getPlayer(siege.owner);
        boolean valid = owner != null && owner.isAlive() && !owner.isCreative() && !owner.isSpectator()
                && owner.level() == siege.level
                && siege.anchor.distSqr(owner.blockPosition()) <= DEFENSE_RADIUS * DEFENSE_RADIUS
                && OutpostService.isRecoveryOperational(owner, siege.level, siege.dimension, siege.anchor);
        if (valid) siege.ownerAbsentTicks = 0;
        else siege.ownerAbsentTicks += 5;
        syncBossBarPlayers(server, siege);

        if (siege.ownerAbsentTicks >= OWNER_GRACE_TICKS) {
            fail(siege, owner, "소유자가 전초64블록 밖에 머물렀거나 실제 전초 구조가 유지되지 않았습니다.");
            return;
        }
        if (now >= siege.deadline) {
            fail(siege, owner, "전체 방어 제한시간 4분을 초과했습니다.");
            return;
        }

        int breachers = pruneAndDirect(siege, owner);
        if (breachers > 0) {
            siege.breachPressure = Math.min(BREACH_LIMIT, siege.breachPressure + breachers * 5);
            if (owner != null && now - siege.lastBreachWarningTick >= 20L) {
                siege.lastBreachWarningTick = now;
                owner.sendSystemMessage(Component.literal("§c[전초 돌파] §f침투 " + breachers + "체 · 압력 §c"
                        + siege.breachPressure + "/" + BREACH_LIMIT), true);
            }
        } else {
            siege.breachPressure = Math.max(0, siege.breachPressure - 10);
        }
        if (siege.breachPressure >= BREACH_LIMIT) {
            fail(siege, owner, "적이 지휘 배럴 주변을 장악해 돌파 압력이 한계에 도달했습니다.");
            return;
        }

        if (siege.mobIds.isEmpty()) {
            if (siege.wave >= TOTAL_WAVES) {
                complete(siege, owner);
                return;
            }
            if (siege.nextWaveTick == 0L) {
                siege.nextWaveTick = now + WAVE_DELAY_TICKS;
                if (owner != null) owner.sendSystemMessage(Component.literal("§a[전초 공세 격퇴] §f다음 공세까지 §e3초§f. 전초 주변을 다시 정비하세요."));
            } else if (now >= siege.nextWaveTick) {
                siege.wave++;
                siege.nextWaveTick = 0L;
                if (!spawnWave(siege)) {
                    fail(siege, owner, "다음 공세를 배치할 로딩 지형이 부족해 방어전을 안전하게 종료했습니다.");
                    return;
                }
                if (owner != null) owner.sendSystemMessage(Component.literal("§c[전초 공세 " + siege.wave + "/" + TOTAL_WAVES
                        + "] §f새 습격대가 외곽에서 접근합니다."));
            }
        } else {
            siege.nextWaveTick = 0L;
        }
        updateBossBar(siege);
    }

    private static int pruneAndDirect(Siege siege, ServerPlayer owner) {
        Set<UUID> alive = new HashSet<>();
        int breachers = 0;
        double breachSq = BREACH_RADIUS * BREACH_RADIUS;
        double engageSq = ENGAGE_RADIUS * ENGAGE_RADIUS;
        double recallSq = RECALL_RADIUS * RECALL_RADIUS;
        boolean ownerDefendingAnchor = owner != null && siege.anchor.distSqr(owner.blockPosition()) <= engageSq;
        for (UUID id : siege.mobIds) {
            Entity entity = siege.level.getEntity(id);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) continue;
            alive.add(id);
            double anchorDistance = distanceToCenterSqr(mob, siege.anchor);
            if (anchorDistance <= breachSq) breachers++;

            if (ownerDefendingAnchor && mob.distanceToSqr(owner) <= engageSq) {
                if (mob.getTarget() != owner) mob.setTarget(owner);
            } else {
                if (mob.getTarget() != null) mob.setTarget(null);
                mob.getNavigation().moveTo(siege.anchor.getX() + 0.5D, siege.anchor.getY(), siege.anchor.getZ() + 0.5D,
                        anchorDistance > recallSq ? 1.30D : 1.12D);
            }
        }
        siege.mobIds.clear();
        siege.mobIds.addAll(alive);
        return breachers;
    }

    private static boolean spawnWave(Siege siege) {
        List<String> types = waveTypes(siege.stage, siege.wave);
        Set<UUID> spawned = new HashSet<>();
        for (int i = 0; i < types.size(); i++) {
            Mob mob = spawnOne(siege.level, siege.anchor, types.get(i), i, types.size());
            if (mob == null) continue;
            markMob(mob, siege);
            mob.setTarget(null);
            mob.getNavigation().moveTo(siege.anchor.getX() + 0.5D, siege.anchor.getY(), siege.anchor.getZ() + 0.5D, 1.12D);
            spawned.add(mob.getUUID());
        }
        int minimum = Math.max(4, types.size() * 2 / 3);
        if (spawned.size() < minimum) {
            for (UUID id : spawned) {
                Entity entity = siege.level.getEntity(id);
                if (entity != null) entity.discard();
            }
            return false;
        }
        siege.mobIds.addAll(spawned);
        return true;
    }

    private static List<String> waveTypes(int stage, int wave) {
        if (stage >= 2) {
            return switch (wave) {
                case 1 -> List.of(
                        "minecraft:skeleton", "minecraft:skeleton", "minecraft:skeleton", "minecraft:skeleton",
                        "minecraft:spider", "minecraft:spider", "minecraft:spider",
                        "minecraft:zombie", "minecraft:zombie", "minecraft:enderman");
                case 2 -> List.of(
                        "minecraft:pillager", "minecraft:pillager", "minecraft:pillager", "minecraft:pillager",
                        "minecraft:vindicator", "minecraft:vindicator", "minecraft:vindicator", "minecraft:vindicator",
                        "minecraft:witch", "minecraft:witch", "minecraft:spider", "minecraft:spider");
                default -> List.of(
                        "minecraft:ravager", "minecraft:ravager",
                        "minecraft:pillager", "minecraft:pillager", "minecraft:pillager", "minecraft:pillager",
                        "minecraft:vindicator", "minecraft:vindicator", "minecraft:vindicator", "minecraft:vindicator",
                        "minecraft:witch", "minecraft:witch", "minecraft:enderman");
            };
        }
        return switch (wave) {
            case 1 -> List.of(
                    "minecraft:skeleton", "minecraft:skeleton", "minecraft:skeleton", "minecraft:skeleton",
                    "minecraft:spider", "minecraft:spider", "minecraft:spider", "minecraft:spider");
            case 2 -> List.of(
                    "minecraft:zombie", "minecraft:zombie", "minecraft:zombie",
                    "minecraft:pillager", "minecraft:pillager", "minecraft:pillager",
                    "minecraft:vindicator", "minecraft:vindicator", "minecraft:vindicator");
            default -> List.of(
                    "minecraft:ravager",
                    "minecraft:pillager", "minecraft:pillager", "minecraft:pillager",
                    "minecraft:vindicator", "minecraft:vindicator", "minecraft:vindicator",
                    "minecraft:witch", "minecraft:skeleton", "minecraft:skeleton");
        };
    }

    private static Mob spawnOne(ServerLevel level, BlockPos center, String typeId, int index, int count) {
        Identifier identifier = Identifier.parse(typeId);
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(identifier)) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(identifier);
        if (type == null) return null;
        for (int attempt = 0; attempt < 14; attempt++) {
            double angle = Math.PI * 2.0D * (index + attempt * 0.37D) / Math.max(1, count);
            int radius = SPAWN_MIN_RADIUS + level.getRandom().nextInt(SPAWN_RADIUS_SPAN);
            BlockPos base = center.offset((int) Math.round(Math.cos(angle) * radius), 0,
                    (int) Math.round(Math.sin(angle) * radius));
            BlockPos pos = findOpenSpawn(level, base);
            if (pos == null) continue;
            Entity entity = type.spawn(level, pos, EntitySpawnReason.TRIGGERED);
            if (entity instanceof Mob mob) return mob;
            if (entity != null) entity.discard();
        }
        return null;
    }

    private static BlockPos findOpenSpawn(ServerLevel level, BlockPos base) {
        for (int dy = 6; dy >= -8; dy--) {
            BlockPos pos = base.offset(0, dy, 0);
            if (!level.hasChunkAt(pos)) continue;
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()
                    || !level.getBlockState(pos.above(2)).isAir()) continue;
            if (level.getBlockState(pos.below()).isAir()) continue;
            if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()) continue;
            return pos;
        }
        return null;
    }

    private static void markMob(Mob mob, Siege siege) {
        mob.setPersistenceRequired();
        mob.getPersistentData().putString(SIEGE_OWNER_KEY, siege.owner.toString());
        mob.getPersistentData().putInt(SIEGE_WAVE_KEY, siege.wave);
    }

    private static void complete(Siege siege, ServerPlayer owner) {
        if (!ACTIVE.remove(siege.owner, siege)) return;
        closeBossBar(siege);
        if (owner == null) return;

        if (siege.stage >= 2) {
            SkillProgressionService.award(owner, SkillType.COMBAT, 500);
            giveOrDrop(owner, new ItemStack(Items.DIAMOND, 4));
            giveOrDrop(owner, new ItemStack(Items.ECHO_SHARD, 6));
            giveOrDrop(owner, new ItemStack(Items.DRAGON_BREATH, 2));
            owner.giveExperiencePoints(200);
        } else {
            SkillProgressionService.award(owner, SkillType.COMBAT, 350);
            giveOrDrop(owner, new ItemStack(Items.DIAMOND, 2));
            giveOrDrop(owner, new ItemStack(Items.AMETHYST_SHARD, 24));
            giveOrDrop(owner, new ItemStack(Items.ECHO_SHARD, 3));
            owner.giveExperiencePoints(120);
        }
        owner.sendSystemMessage(Component.literal("§a[전초 방어 성공] §f3개 공세를 모두 격퇴했습니다. §7전초 위치를 지켜낸 보상 지급 완료."));
        for (ServerPlayer ally : siege.level.getServer().getPlayerList().getPlayers()) {
            if (ally == owner || ally.level() != siege.level || !ally.isAlive() || ally.isSpectator()) continue;
            if (distanceToCenterSqr(ally, siege.anchor) <= 48.0D * 48.0D) {
                ally.giveExperiencePoints(40);
                ally.sendSystemMessage(Component.literal("§c[전초 방어전] §f협동 방어 보상 경험치 §e+40"));
            }
        }
    }

    private static void fail(Siege siege, ServerPlayer owner, String reason) {
        if (!ACTIVE.remove(siege.owner, siege)) return;
        cleanupMobs(siege);
        closeBossBar(siege);
        if (owner != null) owner.sendSystemMessage(Component.literal("§c[전초 방어 실패] §f" + reason + " §7· 투입한 보급권은 반환되지 않습니다."));
    }

    private static void updateBossBar(Siege siege) {
        long remain = Math.max(0L, siege.deadline - siege.level.getGameTime());
        long seconds = (remain + 19L) / 20L;
        siege.bossBar.setName(Component.literal("§c전초 방어 §7[공세 " + siege.wave + "/" + TOTAL_WAVES + "] §f적 "
                + siege.mobIds.size() + " §7· 돌파 §c" + siege.breachPressure + "/" + BREACH_LIMIT + " §7· " + seconds + "초"));
        float integrity = 1.0F - (float) siege.breachPressure / BREACH_LIMIT;
        siege.bossBar.setProgress(Math.max(0.0F, Math.min(1.0F, integrity)));
    }

    private static void syncBossBarPlayers(MinecraftServer server, Siege siege) {
        Set<ServerPlayer> shouldSee = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == siege.level && player.isAlive() && !player.isSpectator()
                    && distanceToCenterSqr(player, siege.anchor) <= DEFENSE_RADIUS * DEFENSE_RADIUS) {
                shouldSee.add(player);
            }
        }
        for (ServerPlayer viewer : List.copyOf(siege.bossBar.getPlayers())) {
            if (!shouldSee.contains(viewer)) siege.bossBar.removePlayer(viewer);
        }
        for (ServerPlayer viewer : shouldSee) {
            if (!siege.bossBar.getPlayers().contains(viewer)) siege.bossBar.addPlayer(viewer);
        }
    }

    private static void removeStaleServerSieges(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        List<UUID> stale = new ArrayList<>();
        for (Map.Entry<UUID, Siege> entry : ACTIVE.entrySet()) {
            if (entry.getValue().level.getServer() == server) continue;
            cleanupMobs(entry.getValue());
            closeBossBar(entry.getValue());
            stale.add(entry.getKey());
        }
        for (UUID owner : stale) ACTIVE.remove(owner);
    }

    private static void cleanupMobs(Siege siege) {
        for (UUID id : siege.mobIds) {
            Entity entity = siege.level.getEntity(id);
            if (entity != null) entity.discard();
        }
        siege.mobIds.clear();
    }

    private static void closeBossBar(Siege siege) {
        siege.bossBar.setVisible(false);
        for (ServerPlayer viewer : List.copyOf(siege.bossBar.getPlayers())) siege.bossBar.removePlayer(viewer);
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private static double distanceToCenterSqr(Entity entity, BlockPos center) {
        double dx = entity.getX() - (center.getX() + 0.5D);
        double dy = entity.getY() - (center.getY() + 0.5D);
        double dz = entity.getZ() - (center.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    private static final class Siege {
        final UUID owner;
        final ServerLevel level;
        final String dimension;
        final BlockPos anchor;
        final int stage;
        final long deadline;
        final ServerBossEvent bossBar;
        final Set<UUID> mobIds = new HashSet<>();
        int wave;
        int breachPressure;
        int ownerAbsentTicks;
        long nextWaveTick;
        long lastBreachWarningTick;

        Siege(UUID owner, ServerLevel level, String dimension, BlockPos anchor, int stage, long deadline) {
            this.owner = owner;
            this.level = level;
            this.dimension = dimension;
            this.anchor = anchor.immutable();
            this.stage = stage;
            this.deadline = deadline;
            this.bossBar = new ServerBossEvent(UUID.randomUUID(), Component.literal("전초 방어전"),
                    BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        }
    }
}
