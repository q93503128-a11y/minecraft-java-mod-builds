package kr.moonseungjun.survivalascension.expedition;

import kr.moonseungjun.survivalascension.progress.SkillProgressionService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ExpeditionIncidentSystem {
    private static final String READY_TICK_KEY = "survivalascension_expedition_incident_ready";
    private static final String TRIAL_READY_TICK_KEY = "survivalascension_ascension_trial_ready";
    private static final int CHECK_INTERVAL_TICKS = 600;
    private static final double START_CHANCE = 0.10D;
    private static final int START_COOLDOWN_TICKS = 3600;
    private static final int TRIAL_EXCLUSION_AFTER_READY_TICKS = 3600;
    private static final int OUTSIDE_GRACE_TICKS = 200;
    private static final int PRE_ALERT_TICKS = 200;
    private static final int PRE_ALERT_ACTIONBAR_INTERVAL = 20;
    private static final double EVENT_RADIUS = 48.0D;
    private static final double RARE_CHANCE = 0.15D;
    private static final int RARE_EXTRA_TIME_TICKS = 300;
    private static final int OVERLAP_RETRY_TICKS = 600;
    private static final double INCIDENT_CENTER_CLEARANCE = EVENT_RADIUS * 2.0D + 16.0D;
    private static final int BOUNDARY_POINTS = 32;
    private static final int BOUNDARY_SCAN_UP = 12;
    private static final int BOUNDARY_SCAN_DOWN = 16;
    private static final Map<UUID, PendingIncident> PENDING = new HashMap<>();
    private static final Map<UUID, ActiveIncident> ACTIVE = new HashMap<>();

    private ExpeditionIncidentSystem() {}

    public static boolean isActive(ServerPlayer player) {
        UUID id = player.getUUID();
        return PENDING.containsKey(id) || ACTIVE.containsKey(id);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        if (player.tickCount % 5 != 0) return;

        removeStaleServerIncidents(level.getServer());
        ActiveIncident active = ACTIVE.get(player.getUUID());
        if (active != null) {
            tickActive(player, active);
            return;
        }
        PendingIncident pending = PENDING.get(player.getUUID());
        if (pending != null) {
            tickPending(player, pending);
            return;
        }

        if (player.tickCount % CHECK_INTERVAL_TICKS != 0 || player.isCreative() || player.isSpectator() || !player.isAlive()) return;
        long now = level.getGameTime();
        if (now < player.getPersistentData().getLongOr(READY_TICK_KEY, 0L)) return;
        long trialReady = player.getPersistentData().getLongOr(TRIAL_READY_TICK_KEY, 0L);
        if (trialReady > 0L && now < trialReady + TRIAL_EXCLUSION_AFTER_READY_TICKS) return;

        ExpeditionRegion region = ExpeditionProgression.currentRegion(player);
        if (region == null) return;
        ExpeditionData data = ExpeditionData.get(player);
        if (!data.isDiscovered(player, region) || data.incidentResolved(player, region)) return;
        if (level.getRandom().nextDouble() >= START_CHANCE) return;

        queueStart(player, level, region, ExpeditionIncident.random(region, level.getRandom()),
                level.getRandom().nextDouble() < RARE_CHANCE);
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID owner = event.getEntity().getUUID();
        PendingIncident pending = PENDING.remove(owner);
        if (pending != null) closeBossBar(pending.bossBar);
        ActiveIncident active = ACTIVE.remove(owner);
        if (active != null) {
            cleanupMobs(active);
            closeBossBar(active.bossBar);
        }
    }

    public static void recordAction(ServerPlayer player, ExpeditionAction action, int amount) {
        if (amount <= 0) return;
        ActiveIncident active = ACTIVE.get(player.getUUID());
        if (active == null || active.incident.kind() != ExpeditionIncident.Kind.ACTION_RUSH) return;
        if (active.incident.action() != action || player.level() != active.level) return;
        if (ExpeditionProgression.currentRegion(player) != active.incident.region()) return;
        active.actionProgress = Math.min(active.actionTarget(), active.actionProgress + amount);
        updateBossBar(active);
        if (active.actionProgress >= active.actionTarget()) complete(player, active);
    }

    private static void queueStart(ServerPlayer player, ServerLevel level, ExpeditionRegion region,
                                   ExpeditionIncident incident, boolean rare) {
        long now = level.getGameTime();
        BlockPos center = player.blockPosition().immutable();
        if (overlapsReservedIncident(level, center, player.getUUID())) {
            player.getPersistentData().putLong(READY_TICK_KEY, now + OVERLAP_RETRY_TICKS);
            return;
        }

        PendingIncident pending = new PendingIncident(player.getUUID(), level, center, region, incident,
                now + PRE_ALERT_TICKS, rare);
        PENDING.put(player.getUUID(), pending);
        pending.bossBar.addPlayer(player);
        pending.bossBar.setVisible(true);
        updatePendingBossBar(pending);
        renderBoundary(level, center, rare, true);
        int seconds = PRE_ALERT_TICKS / 20;
        player.sendSystemMessage(Component.literal((rare ? "§d[희귀 현장 사건 예고] " : "§e[현장 사건 예고] ")
                + "§f" + region.koreanName() + " · §e" + incident.koreanName() + "§f이 §e" + seconds
                + "초 후§f 열립니다. §7표시된 반경 48블록 안에서 준비하세요. 제한시간은 개방 뒤부터 시작합니다."));
        player.sendSystemMessage(Component.literal("§e[현장 사건 예고] §f" + seconds + "초 후 개방 · 반경 48블록 유지"), true);
    }

    private static void tickPending(ServerPlayer player, PendingIncident pending) {
        if (PENDING.get(player.getUUID()) != pending) return;
        long now = pending.level.getGameTime();
        boolean invalid = !player.isAlive() || player.isCreative() || player.isSpectator()
                || player.level() != pending.level
                || ExpeditionProgression.currentRegion(player) != pending.region
                || distanceToCenterSqr(player, pending.center) > EVENT_RADIUS * EVENT_RADIUS;
        if (invalid) {
            cancelPending(player, pending, "예고 지점 또는 현재 원정권을 벗어나 개방이 취소되었습니다.");
            return;
        }

        if (now >= pending.openTick) {
            if (PENDING.remove(player.getUUID()) != pending) return;
            closeBossBar(pending.bossBar);
            start(player, pending.level, pending.region, pending.incident, pending.rare, pending.center);
            return;
        }

        if (now % 20L == 0L) {
            renderBoundary(pending.level, pending.center, pending.rare, true);
            updatePendingBossBar(pending);
        }
        if (now % PRE_ALERT_ACTIONBAR_INTERVAL == 0L) {
            long seconds = Math.max(1L, (pending.openTick - now + 19L) / 20L);
            player.sendSystemMessage(Component.literal((pending.rare ? "§d[희귀 사건 예고] " : "§e[사건 예고] ")
                    + "§f" + pending.incident.koreanName() + " §7· §e" + seconds + "초 후 개방"), true);
            if (seconds == 3L) {
                player.sendSystemMessage(Component.literal("§c[현장 사건 임박] §f" + pending.incident.koreanName()
                        + " §7· 3초 후 제한시간이 시작됩니다."));
            }
        }
    }

    private static void cancelPending(ServerPlayer player, PendingIncident pending, String reason) {
        if (PENDING.remove(player.getUUID()) != pending) return;
        closeBossBar(pending.bossBar);
        long now = pending.level.getGameTime();
        player.getPersistentData().putLong(READY_TICK_KEY, now + OVERLAP_RETRY_TICKS);
        player.sendSystemMessage(Component.literal("§7[현장 사건 예고 취소] §f" + reason + " §7잠시 뒤 다시 감지될 수 있습니다."));
    }

    private static void start(ServerPlayer player, ServerLevel level, ExpeditionRegion region,
                              ExpeditionIncident incident, boolean rare, BlockPos center) {
        long now = level.getGameTime();
        if (!player.isAlive() || player.isCreative() || player.isSpectator() || player.level() != level
                || ExpeditionProgression.currentRegion(player) != region
                || distanceToCenterSqr(player, center) > EVENT_RADIUS * EVENT_RADIUS) {
            player.getPersistentData().putLong(READY_TICK_KEY, now + OVERLAP_RETRY_TICKS);
            return;
        }
        if (overlapsReservedIncident(level, center, player.getUUID())) {
            player.getPersistentData().putLong(READY_TICK_KEY, now + OVERLAP_RETRY_TICKS);
            player.sendSystemMessage(Component.literal("§7[현장 사건 보류] §f근처 사건 구역과 겹쳐 이번 개방을 건너뜁니다."));
            return;
        }
        player.getPersistentData().putLong(READY_TICK_KEY, now + START_COOLDOWN_TICKS);
        ActiveIncident active = new ActiveIncident(player.getUUID(), level, center, incident,
                now + incident.durationTicks() + (rare ? RARE_EXTRA_TIME_TICKS : 0), rare);

        if (incident.kind() == ExpeditionIncident.Kind.AMBUSH) {
            Set<UUID> spawned = spawnAmbush(player, active);
            int minimum = Math.max(3, active.spawnTarget() * 2 / 3);
            if (spawned.size() < minimum) {
                for (UUID id : spawned) {
                    Entity entity = level.getEntity(id);
                    if (entity != null) entity.discard();
                }
                player.getPersistentData().putLong(READY_TICK_KEY, now + 1200);
                player.sendSystemMessage(Component.literal("§7[현장 사건 보류] §f습격대를 안전하게 배치할 공간이 부족해 개방을 미뤘습니다."));
                return;
            }
            active.mobIds.addAll(spawned);
            active.initialMobCount = spawned.size();
        }

        ACTIVE.put(player.getUUID(), active);
        active.bossBar.addPlayer(player);
        active.bossBar.setVisible(true);
        updateBossBar(active);
        renderBoundary(active.level, active.center, active.rare, false);
        String prefix = rare ? "§d[희귀 현장 사건] " : (incident.kind() == ExpeditionIncident.Kind.AMBUSH ? "§c[현장 사건] " : "§6[현장 사건] ");
        if (incident.kind() == ExpeditionIncident.Kind.AMBUSH) {
            player.sendSystemMessage(Component.literal(prefix + "§f" + region.koreanName() + " · §e" + incident.koreanName()
                    + " §7· 표시된 반경 48블록 안에서 빛나는 습격대 " + active.initialMobCount + "체를 정리하세요."
                    + (rare ? " §d· 강화 보상" : "")));
        } else {
            player.sendSystemMessage(Component.literal(prefix + "§f" + region.koreanName() + " · §e" + incident.koreanName()
                    + " §7· 표시된 반경 48블록 안에서 제한시간 내 " + incident.action().koreanName() + " §e" + active.actionTarget() + "§7을 수행하세요."
                    + (rare ? " §d· 강화 보상" : "")));
        }
    }

    private static void tickActive(ServerPlayer player, ActiveIncident active) {
        if (ACTIVE.get(player.getUUID()) != active) return;
        long now = active.level.getGameTime();
        boolean invalid = !player.isAlive() || player.isCreative() || player.isSpectator()
                || player.level() != active.level
                || ExpeditionProgression.currentRegion(player) != active.incident.region()
                || distanceToCenterSqr(player, active.center) > EVENT_RADIUS * EVENT_RADIUS;
        if (invalid) active.outsideTicks += 5;
        else active.outsideTicks = 0;

        if (active.outsideTicks >= OUTSIDE_GRACE_TICKS) {
            fail(player, active, "사건 지역을 너무 오래 벗어났습니다.");
            return;
        }
        if (now >= active.deadline) {
            fail(player, active, "제한시간이 끝났습니다.");
            return;
        }

        if (now % 20L == 0L) {
            renderBoundary(active.level, active.center, active.rare, false);
            long seconds = Math.max(1L, (active.deadline - now + 19L) / 20L);
            if (active.outsideTicks > 0) {
                long grace = Math.max(1L, (OUTSIDE_GRACE_TICKS - active.outsideTicks + 19L) / 20L);
                player.sendSystemMessage(Component.literal("§c[사건 경계 이탈] §f48블록 안으로 복귀하세요. §7· 실패까지 약 §c"
                        + grace + "초"), true);
            } else if (seconds <= 10L || seconds == 30L) {
                player.sendSystemMessage(Component.literal((seconds <= 10L ? "§c" : "§e") + "[현장 사건] §f"
                        + active.incident.koreanName() + " §7· 남은 시간 §e" + seconds + "초"), true);
            }
        }

        if (active.incident.kind() == ExpeditionIncident.Kind.AMBUSH) {
            Set<UUID> alive = new HashSet<>();
            for (UUID id : active.mobIds) {
                Entity entity = active.level.getEntity(id);
                if (!(entity instanceof Mob mob) || !mob.isAlive()) continue;
                alive.add(id);
                mob.setGlowingTag(true);
                if (mob.getTarget() == null) mob.setTarget(player);
                if (distanceToCenterSqr(mob, active.center) > EVENT_RADIUS * EVENT_RADIUS) {
                    mob.getNavigation().moveTo(active.center.getX() + 0.5D, active.center.getY(), active.center.getZ() + 0.5D, 1.25D);
                }
            }
            active.mobIds.clear();
            active.mobIds.addAll(alive);
            if (active.mobIds.isEmpty()) {
                complete(player, active);
                return;
            }
        }
        updateBossBar(active);
    }

    private static Set<UUID> spawnAmbush(ServerPlayer player, ActiveIncident active) {
        Set<UUID> spawned = new HashSet<>();
        List<String> types = active.incident.mobTypeIds();
        int spawnTarget = active.spawnTarget();
        for (int i = 0; i < spawnTarget; i++) {
            String typeId = types.get(i % types.size());
            Mob mob = spawnOne(active.level, active.center, active.incident.region() == ExpeditionRegion.OCEAN,
                    typeId, i, spawnTarget);
            if (mob == null) continue;
            mob.setPersistenceRequired();
            mob.setGlowingTag(true);
            mob.setTarget(player);
            spawned.add(mob.getUUID());
        }
        return spawned;
    }

    private static Mob spawnOne(ServerLevel level, BlockPos center, boolean water, String typeId, int index, int count) {
        Identifier identifier = Identifier.parse(typeId);
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(identifier)) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(identifier);
        if (type == null) return null;
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = Math.PI * 2.0D * (index + attempt * 0.37D) / Math.max(1, count);
            int radius = 7 + level.getRandom().nextInt(7);
            BlockPos base = center.offset((int) Math.round(Math.cos(angle) * radius), 0,
                    (int) Math.round(Math.sin(angle) * radius));
            BlockPos pos = water ? findWaterSpawn(level, base) : findOpenSpawn(level, base);
            if (pos == null) continue;
            Entity entity = type.spawn(level, pos, EntitySpawnReason.TRIGGERED);
            if (entity instanceof Mob mob) return mob;
            if (entity != null) entity.discard();
        }
        return null;
    }

    private static BlockPos findOpenSpawn(ServerLevel level, BlockPos base) {
        for (int dy = 4; dy >= -5; dy--) {
            BlockPos pos = base.offset(0, dy, 0);
            if (!level.hasChunkAt(pos)) continue;
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
            if (level.getBlockState(pos.below()).isAir()) continue;
            if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()) continue;
            return pos;
        }
        return null;
    }

    private static BlockPos findWaterSpawn(ServerLevel level, BlockPos base) {
        for (int dy = 3; dy >= -8; dy--) {
            BlockPos pos = base.offset(0, dy, 0);
            if (!level.hasChunkAt(pos)) continue;
            if (level.getFluidState(pos).isEmpty()) continue;
            if (level.getFluidState(pos.above()).isEmpty()) continue;
            return pos;
        }
        return null;
    }

    private static void complete(ServerPlayer player, ActiveIncident active) {
        if (ACTIVE.remove(player.getUUID()) != active) return;
        ExpeditionData data = ExpeditionData.get(player);
        boolean firstResolution = data.claimIncidentReward(player, active.incident.region());
        closeBossBar(active.bossBar);
        if (!firstResolution) return;

        int stage = active.incident.region().requiredWorldStage();
        int skillXp = (100 + stage * 50) * (active.rare ? 2 : 1);
        SkillProgressionService.award(player, active.incident.region().rewardSkill(), skillXp);
        if (stage == 0) {
            giveOrDrop(player, new ItemStack(Items.EMERALD, active.rare ? 10 : 4));
            giveOrDrop(player, new ItemStack(Items.AMETHYST_SHARD, active.rare ? 20 : 8));
        } else if (stage == 1) {
            giveOrDrop(player, new ItemStack(Items.DIAMOND, active.rare ? 5 : 2));
            giveOrDrop(player, new ItemStack(Items.ECHO_SHARD, active.rare ? 10 : 4));
        } else {
            giveOrDrop(player, new ItemStack(Items.DIAMOND, active.rare ? 8 : 4));
            giveOrDrop(player, new ItemStack(Items.ECHO_SHARD, active.rare ? 16 : 8));
        }

        player.sendSystemMessage(Component.literal((active.rare ? "§d[희귀 현장 사건 해결] " : "§a[현장 사건 해결] ")
                + "§f" + active.incident.region().koreanName() + " · §e" + active.incident.koreanName() + " §7· "
                + active.incident.region().rewardSkill().koreanName() + " 숙련 XP +" + skillXp));

        ExpeditionDirective.Task bonusTask = data.firstIncompleteTask(player, active.incident.region());
        if (bonusTask != null) {
            int bonus = Math.max(1, bonusTask.target() / (active.rare ? 3 : 5));
            player.sendSystemMessage(Component.literal("§6[현장 사건 보너스] §f현재 지령의 §e" + bonusTask.action().koreanName()
                    + " §f진행도에 최대 §e" + bonus + "§f를 추가합니다."));
            ExpeditionProgression.grantIncidentBonus(player, active.incident.region(), bonusTask.action(), bonus);
        }
    }

    private static void fail(ServerPlayer player, ActiveIncident active, String reason) {
        if (ACTIVE.remove(player.getUUID()) != active) return;
        cleanupMobs(active);
        closeBossBar(active.bossBar);
        player.sendSystemMessage(Component.literal("§c[현장 사건 실패] §f" + active.incident.koreanName() + " · " + reason
                + " §7· 원정 지령 진행도는 잃지 않으며 이후 다시 발생할 수 있습니다."));
    }

    private static void updatePendingBossBar(PendingIncident pending) {
        long remain = Math.max(0L, pending.openTick - pending.level.getGameTime());
        long seconds = Math.max(1L, (remain + 19L) / 20L);
        pending.bossBar.setName(Component.literal((pending.rare ? "§d희귀 현장 사건 예고 " : "§e현장 사건 예고 ")
                + "§7[" + pending.incident.koreanName() + "] §f" + seconds + "초 후 개방"));
        pending.bossBar.setProgress(Math.max(0.0F, Math.min(1.0F, remain / (float) PRE_ALERT_TICKS)));
    }

    private static void updateBossBar(ActiveIncident active) {
        long remain = Math.max(0L, active.deadline - active.level.getGameTime());
        long seconds = (remain + 19L) / 20L;
        if (active.incident.kind() == ExpeditionIncident.Kind.AMBUSH) {
            active.bossBar.setName(Component.literal((active.rare ? "§d희귀 현장 사건 " : "§c현장 사건 ")
                    + "§7[" + active.incident.koreanName() + "] §f적 " + active.mobIds.size() + " · " + seconds + "초"));
            float progress = active.initialMobCount <= 0 ? 0.0F : (float) active.mobIds.size() / active.initialMobCount;
            active.bossBar.setProgress(Math.max(0.0F, Math.min(1.0F, progress)));
        } else {
            active.bossBar.setName(Component.literal((active.rare ? "§d희귀 현장 사건 " : "§6현장 사건 ")
                    + "§7[" + active.incident.koreanName() + "] §f" + active.actionProgress + "/" + active.actionTarget()
                    + " · " + seconds + "초"));
            float progress = active.actionTarget() <= 0 ? 0.0F
                    : (float) active.actionProgress / active.actionTarget();
            active.bossBar.setProgress(Math.max(0.0F, Math.min(1.0F, progress)));
        }
    }

    private static boolean overlapsReservedIncident(ServerLevel level, BlockPos center, UUID ignoredOwner) {
        double clearanceSqr = INCIDENT_CENTER_CLEARANCE * INCIDENT_CENTER_CLEARANCE;
        for (ActiveIncident active : ACTIVE.values()) {
            if (active.owner.equals(ignoredOwner) || active.level != level) continue;
            double dx = center.getX() - active.center.getX();
            double dz = center.getZ() - active.center.getZ();
            if (dx * dx + dz * dz < clearanceSqr) return true;
        }
        for (PendingIncident pending : PENDING.values()) {
            if (pending.owner.equals(ignoredOwner) || pending.level != level) continue;
            double dx = center.getX() - pending.center.getX();
            double dz = center.getZ() - pending.center.getZ();
            if (dx * dx + dz * dz < clearanceSqr) return true;
        }
        return false;
    }

    private static void renderBoundary(ServerLevel level, BlockPos center, boolean rare, boolean preview) {
        ParticleOptions particle = rare ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.END_ROD;
        for (int i = 0; i < BOUNDARY_POINTS; i++) {
            double angle = Math.PI * 2.0D * i / BOUNDARY_POINTS;
            double x = center.getX() + 0.5D + Math.cos(angle) * EVENT_RADIUS;
            double z = center.getZ() + 0.5D + Math.sin(angle) * EVENT_RADIUS;
            double y = visibleBoundaryY(level, center, x, z);
            if (Double.isNaN(y)) continue;
            level.sendParticles(particle, x, y, z, preview ? 1 : 2, 0.0D, 0.10D, 0.0D, 0.0D);
            level.sendParticles(particle, x, y + 1.7D, z, 1, 0.0D, 0.10D, 0.0D, 0.0D);
        }
        double centerY = center.getY() + 1.1D;
        level.sendParticles(particle, center.getX() + 0.5D, centerY, center.getZ() + 0.5D,
                rare ? 5 : 3, 0.35D, 0.15D, 0.35D, 0.0D);
        level.sendParticles(particle, center.getX() + 0.5D, centerY + 2.0D, center.getZ() + 0.5D,
                preview ? 2 : 4, 0.25D, 0.20D, 0.25D, 0.0D);
    }

    private static double visibleBoundaryY(ServerLevel level, BlockPos center, double x, double z) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        BlockPos column = new BlockPos(blockX, center.getY(), blockZ);
        if (!level.hasChunkAt(column)) return Double.NaN;
        for (int dy = BOUNDARY_SCAN_UP; dy >= -BOUNDARY_SCAN_DOWN; dy--) {
            BlockPos pos = column.offset(0, dy, 0);
            if (!level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty()) continue;
            BlockPos below = pos.below();
            if (level.getBlockState(below).isAir() && level.getFluidState(below).isEmpty()) continue;
            return pos.getY() + 0.15D;
        }
        return center.getY() + 1.1D;
    }

    private static void removeStaleServerIncidents(MinecraftServer server) {
        if (ACTIVE.isEmpty() && PENDING.isEmpty()) return;
        List<UUID> staleActive = new ArrayList<>();
        for (Map.Entry<UUID, ActiveIncident> entry : ACTIVE.entrySet()) {
            if (entry.getValue().level.getServer() == server) continue;
            cleanupMobs(entry.getValue());
            closeBossBar(entry.getValue().bossBar);
            staleActive.add(entry.getKey());
        }
        for (UUID uuid : staleActive) ACTIVE.remove(uuid);

        List<UUID> stalePending = new ArrayList<>();
        for (Map.Entry<UUID, PendingIncident> entry : PENDING.entrySet()) {
            if (entry.getValue().level.getServer() == server) continue;
            closeBossBar(entry.getValue().bossBar);
            stalePending.add(entry.getKey());
        }
        for (UUID uuid : stalePending) PENDING.remove(uuid);
    }

    private static void cleanupMobs(ActiveIncident active) {
        for (UUID id : active.mobIds) {
            Entity entity = active.level.getEntity(id);
            if (entity != null) entity.discard();
        }
        active.mobIds.clear();
    }

    private static void closeBossBar(ServerBossEvent bossBar) {
        bossBar.setVisible(false);
        for (ServerPlayer viewer : List.copyOf(bossBar.getPlayers())) bossBar.removePlayer(viewer);
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private static double distanceToCenterSqr(Entity entity, BlockPos center) {
        double dx = entity.getX() - (center.getX() + 0.5D);
        double dz = entity.getZ() - (center.getZ() + 0.5D);
        return dx * dx + dz * dz;
    }

    private static final class PendingIncident {
        final UUID owner;
        final ServerLevel level;
        final BlockPos center;
        final ExpeditionRegion region;
        final ExpeditionIncident incident;
        final long openTick;
        final boolean rare;
        final ServerBossEvent bossBar;

        PendingIncident(UUID owner, ServerLevel level, BlockPos center, ExpeditionRegion region,
                        ExpeditionIncident incident, long openTick, boolean rare) {
            this.owner = owner;
            this.level = level;
            this.center = center.immutable();
            this.region = region;
            this.incident = incident;
            this.openTick = openTick;
            this.rare = rare;
            this.bossBar = new ServerBossEvent(UUID.randomUUID(), Component.literal(rare ? "희귀 현장 사건 예고" : "현장 사건 예고"),
                    rare ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
        }
    }

    private static final class ActiveIncident {
        final UUID owner;
        final ServerLevel level;
        final BlockPos center;
        final ExpeditionIncident incident;
        final long deadline;
        final boolean rare;
        final ServerBossEvent bossBar;
        final Set<UUID> mobIds = new HashSet<>();
        int initialMobCount;
        int actionProgress;
        int outsideTicks;

        ActiveIncident(UUID owner, ServerLevel level, BlockPos center, ExpeditionIncident incident, long deadline, boolean rare) {
            this.owner = owner;
            this.level = level;
            this.center = center.immutable();
            this.incident = incident;
            this.deadline = deadline;
            this.rare = rare;
            this.bossBar = new ServerBossEvent(UUID.randomUUID(), Component.literal(rare ? "희귀 현장 사건" : "현장 사건"),
                    rare ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
        }

        int actionTarget() {
            int base = incident.actionTarget();
            return rare && base > 0 ? Math.max(base + 1, (base * 3 + 1) / 2) : base;
        }

        int spawnTarget() {
            int base = incident.spawnCount();
            return rare && base > 0 ? Math.max(base + 2, (base * 3 + 1) / 2) : base;
        }
    }
}
