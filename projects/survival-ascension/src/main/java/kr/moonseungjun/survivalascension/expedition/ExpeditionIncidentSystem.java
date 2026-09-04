package kr.moonseungjun.survivalascension.expedition;

import kr.moonseungjun.survivalascension.compat.ContentPackCompatibility;
import kr.moonseungjun.survivalascension.endgame.FinalAscensionSystem;
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
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
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
    private static final String INCIDENT_OWNER_KEY = "survivalascension_expedition_incident_owner";
    private static final int CHECK_INTERVAL_TICKS = 600;
    private static final double START_CHANCE = 0.10D;
    private static final int START_COOLDOWN_TICKS = 3600;
    private static final int TRIAL_EXCLUSION_AFTER_READY_TICKS = 3600;
    private static final int OUTSIDE_GRACE_TICKS = 200;
    private static final int PRE_ALERT_TICKS = 200;
    private static final int PRE_ALERT_ACTIONBAR_INTERVAL = 20;
    private static final double EVENT_RADIUS = 48.0D;
    private static final double COOP_JOIN_RADIUS = 72.0D;
    private static final double RARE_CHANCE = 0.15D;
    private static final double CONTENT_REPLACEMENT_CHANCE = 0.55D;
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
        if (PENDING.containsKey(id) || ACTIVE.containsKey(id)) return true;
        for (PendingIncident pending : PENDING.values()) if (pending.participants.contains(id)) return true;
        for (ActiveIncident active : ACTIVE.values()) if (active.participants.contains(id)) return true;
        return false;
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
        if (isActive(player)) return;
        // Auto incidents must respect the same one-major-encounter rule as manual activities.
        if (FinalAscensionSystem.hasOtherMajorActivity(player)) return;

        if (player.tickCount % CHECK_INTERVAL_TICKS != 0 || player.isCreative() || player.isSpectator() || !player.isAlive()) return;
        if (FinalAscensionSystem.isFinalSequenceActive(player)) return;
        long now = level.getGameTime();
        if (now < player.getPersistentData().getLongOr(READY_TICK_KEY, 0L)) return;
        long trialReady = player.getPersistentData().getLongOr(TRIAL_READY_TICK_KEY, 0L);
        if (trialReady > 0L && now < trialReady + TRIAL_EXCLUSION_AFTER_READY_TICKS) return;

        ExpeditionRegion region = ExpeditionProgression.currentRegion(player);
        if (region == null) return;
        ExpeditionData data = ExpeditionData.get(player);
        if (!data.isDiscovered(player, region) || data.incidentResolved(player, region)) return;
        if (level.getRandom().nextDouble() >= START_CHANCE) return;

        boolean integrationTagged = region.matchesIntegrationTag(level.getBiome(player.blockPosition()));
        queueStart(player, level, region, ExpeditionIncident.random(region, level.getRandom(), integrationTagged),
                level.getRandom().nextDouble() < RARE_CHANCE);
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID leaving = event.getEntity().getUUID();
        for (PendingIncident pending : PENDING.values()) {
            pending.participants.remove(leaving);
            if (event.getEntity() instanceof ServerPlayer sp) pending.bossBar.removePlayer(sp);
        }
        for (ActiveIncident active : ACTIVE.values()) {
            active.participants.remove(leaving);
            if (event.getEntity() instanceof ServerPlayer sp) active.bossBar.removePlayer(sp);
        }

        PendingIncident pending = PENDING.get(leaving);
        if (pending != null) {
            ServerPlayer replacement = replacementController(pending.level, pending.center, pending.region, pending.participants);
            if (replacement != null) {
                PENDING.remove(leaving);
                pending.owner = replacement.getUUID();
                PENDING.put(pending.owner, pending);
            } else {
                PENDING.remove(leaving);
                closeBossBar(pending.bossBar);
            }
        }

        ActiveIncident active = ACTIVE.get(leaving);
        if (active != null) {
            ServerPlayer replacement = replacementController(active.level, active.center, active.incident.region(), active.participants);
            if (replacement != null) {
                ACTIVE.remove(leaving);
                active.owner = replacement.getUUID();
                for (UUID id : active.mobIds) {
                    Entity entity = active.level.getEntity(id);
                    if (entity instanceof Mob mob) mob.getPersistentData().putString(INCIDENT_OWNER_KEY, active.owner.toString());
                }
                ACTIVE.put(active.owner, active);
            } else {
                ACTIVE.remove(leaving);
                cleanupMobs(active);
                closeBossBar(active.bossBar);
            }
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        String ownerText = mob.getPersistentData().getStringOr(INCIDENT_OWNER_KEY, "");
        if (ownerText.isEmpty()) return;
        try {
            UUID owner = UUID.fromString(ownerText);
            ActiveIncident active = ACTIVE.get(owner);
            if (active != null && active.level == level) {
                active.mobIds.remove(mob.getUUID());
                if (event.getSource().getEntity() instanceof ServerPlayer killer && qualifiesParticipant(killer, active.level, active.center, active.incident.region(), EVENT_RADIUS)) {
                    active.participants.add(killer.getUUID());
                }
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob mob)) return;
        String ownerText = mob.getPersistentData().getStringOr(INCIDENT_OWNER_KEY, "");
        if (ownerText.isEmpty()) return;
        try {
            UUID owner = UUID.fromString(ownerText);
            ActiveIncident active = ACTIVE.get(owner);
            if (active == null || active.level != level || !active.mobIds.contains(mob.getUUID())) event.setCanceled(true);
        } catch (IllegalArgumentException ignored) {
            event.setCanceled(true);
        }
    }

    public static void recordAction(ServerPlayer player, ExpeditionAction action, int amount) {
        if (amount <= 0) return;
        ActiveIncident active = findActiveFor(player, action);
        if (active == null) return;
        active.participants.add(player.getUUID());
        if (!active.bossBar.getPlayers().contains(player)) active.bossBar.addPlayer(player);
        active.actionProgress = Math.min(active.actionTarget(), active.actionProgress + amount);
        updateBossBar(active);
        if (active.actionProgress >= active.actionTarget()) complete(active);
    }

    private static void queueStart(ServerPlayer player, ServerLevel level, ExpeditionRegion region,
                                   ExpeditionIncident incident, boolean rare) {
        if (FinalAscensionSystem.isFinalSequenceActive(player)) return;
        long now = level.getGameTime();
        BlockPos center = player.blockPosition().immutable();
        if (overlapsActiveIncident(level, center, player.getUUID())) {
            player.getPersistentData().putLong(READY_TICK_KEY, now + OVERLAP_RETRY_TICKS);
            return;
        }

        PendingIncident pending = new PendingIncident(player.getUUID(), level, center, region, incident,
                now + PRE_ALERT_TICKS, rare);
        PENDING.put(player.getUUID(), pending);
        List<ServerPlayer> party = eligiblePlayers(level, center, region, COOP_JOIN_RADIUS);
        if (party.isEmpty()) party = List.of(player);
        for (ServerPlayer member : party) {
            pending.participants.add(member.getUUID());
            pending.bossBar.addPlayer(member);
        }
        pending.bossBar.setVisible(true);
        updatePendingBossBar(pending);
        renderBoundary(level, center, rare, true);
        int seconds = PRE_ALERT_TICKS / 20;
        Component message = Component.literal((rare ? "§d[희귀 공동 사건 예고] " : "§e[공동 사건 예고] ")
                + "§f" + region.koreanName() + " · §e" + incident.koreanName() + " §7· 참가 " + party.size() + "명"
                + " §7· X " + center.getX() + " Z " + center.getZ() + " §7· " + seconds + "초 후 개방");
        notify(pending.level, pending.participants, message, false);
    }

    private static void tickPending(ServerPlayer player, PendingIncident pending) {
        if (PENDING.get(player.getUUID()) != pending) return;
        long now = pending.level.getGameTime();
        syncPendingParticipants(pending);
        List<ServerPlayer> inside = eligiblePlayers(pending.level, pending.center, pending.region, EVENT_RADIUS);
        if (inside.isEmpty()) {
            cancelPending(player, pending, "참가자가 사건 구역을 벗어나 개방이 취소되었습니다.");
            return;
        }

        if (now >= pending.openTick) {
            if (PENDING.remove(pending.owner) != pending) return;
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
            notify(pending.level, pending.participants, Component.literal((pending.rare ? "§d[희귀 공동 사건] " : "§e[공동 사건] ")
                    + "§f" + pending.incident.koreanName() + " §7· §e" + seconds + "초 후 개방"), true);
        }
    }

    private static void cancelPending(ServerPlayer player, PendingIncident pending, String reason) {
        if (PENDING.remove(pending.owner) != pending) return;
        closeBossBar(pending.bossBar);
        long now = pending.level.getGameTime();
        player.getPersistentData().putLong(READY_TICK_KEY, now + OVERLAP_RETRY_TICKS);
        notify(pending.level, pending.participants, Component.literal("§7[공동 사건 예고 취소] §f" + reason), false);
    }

    private static void start(ServerPlayer player, ServerLevel level, ExpeditionRegion region,
                              ExpeditionIncident incident, boolean rare, BlockPos center) {
        long now = level.getGameTime();
        List<ServerPlayer> party = eligiblePlayers(level, center, region, EVENT_RADIUS);
        if (party.isEmpty()) {
            player.getPersistentData().putLong(READY_TICK_KEY, now + OVERLAP_RETRY_TICKS);
            return;
        }
        if (overlapsActiveIncident(level, center, player.getUUID())) {
            player.getPersistentData().putLong(READY_TICK_KEY, now + OVERLAP_RETRY_TICKS);
            player.sendSystemMessage(Component.literal("§7[공동 사건 보류] §f근처 사건 구역과 겹쳐 이번 개방을 건너뜁니다."));
            return;
        }
        player.getPersistentData().putLong(READY_TICK_KEY, now + START_COOLDOWN_TICKS);
        ActiveIncident active = new ActiveIncident(player.getUUID(), level, center, incident,
                now + incident.durationTicks() + (rare ? RARE_EXTRA_TIME_TICKS : 0), rare, party.size());
        for (ServerPlayer member : party) active.participants.add(member.getUUID());
        ServerPlayer target = party.get(0);

        if (incident.kind() == ExpeditionIncident.Kind.AMBUSH) {
            Set<UUID> spawned = spawnAmbush(target, active);
            int minimum = Math.max(3, active.spawnTarget() * 2 / 3);
            if (spawned.size() < minimum) {
                for (UUID id : spawned) {
                    Entity entity = level.getEntity(id);
                    if (entity != null) entity.discard();
                }
                player.getPersistentData().putLong(READY_TICK_KEY, now + 1200);
                notify(level, active.participants, Component.literal("§7[공동 사건 보류] §f습격대를 배치할 공간이 부족합니다."), false);
                return;
            }
            active.mobIds.addAll(spawned);
            Mob reinforcement = spawnRareReinforcement(target, active);
            if (reinforcement != null) {
                active.mobIds.add(reinforcement.getUUID());
                active.reinforcementCount = 1;
            }
            active.initialMobCount = active.mobIds.size();
        }

        ACTIVE.put(active.owner, active);
        for (ServerPlayer member : party) active.bossBar.addPlayer(member);
        active.bossBar.setVisible(true);
        updateBossBar(active);
        renderBoundary(active.level, active.center, active.rare, false);
        String prefix = rare ? "§d[희귀 공동 사건] " : "§6[공동 사건] ";
        String objective = incident.kind() == ExpeditionIncident.Kind.AMBUSH
                ? "습격대 " + active.initialMobCount + "체 격파"
                : incident.action().koreanName() + " " + active.actionTarget();
        notify(level, active.participants, Component.literal(prefix + "§f" + region.koreanName() + " · §e" + incident.koreanName()
                + " §7· 참가 " + active.participantCountSnapshot + "명 · 공동 목표: §f" + objective), false);
    }

    private static void tickActive(ServerPlayer player, ActiveIncident active) {
        if (ACTIVE.get(player.getUUID()) != active) return;
        long now = active.level.getGameTime();
        syncActiveParticipants(active);
        List<ServerPlayer> inside = eligiblePlayers(active.level, active.center, active.incident.region(), EVENT_RADIUS);
        if (inside.isEmpty()) active.outsideTicks += 5;
        else active.outsideTicks = 0;

        if (active.outsideTicks >= OUTSIDE_GRACE_TICKS) {
            fail(active, "모든 참가자가 사건 지역을 너무 오래 벗어났습니다.");
            return;
        }
        if (now >= active.deadline) {
            fail(active, "제한시간이 끝났습니다.");
            return;
        }

        if (now % 20L == 0L) {
            renderBoundary(active.level, active.center, active.rare, false);
            long seconds = Math.max(1L, (active.deadline - now + 19L) / 20L);
            if (active.outsideTicks > 0) {
                notify(active.level, active.participants, Component.literal("§c[공동 사건 경계] §f48블록 안으로 한 명이라도 복귀하세요."), true);
            } else if (seconds <= 10L || seconds == 30L) {
                notify(active.level, active.participants, Component.literal((seconds <= 10L ? "§c" : "§e") + "[공동 사건] §f"
                        + active.incident.koreanName() + " §7· 남은 시간 §e" + seconds + "초"), true);
            }
        }

        if (active.incident.kind() == ExpeditionIncident.Kind.AMBUSH) {
            Set<UUID> unresolved = new HashSet<>();
            ServerPlayer target = inside.isEmpty() ? null : inside.get(0);
            for (UUID id : active.mobIds) {
                Entity entity = active.level.getEntity(id);
                if (entity == null) {
                    unresolved.add(id);
                    continue;
                }
                if (!(entity instanceof Mob mob) || !mob.isAlive()) continue;
                unresolved.add(id);
                mob.setGlowingTag(true);
                if (target != null && (mob.getTarget() == null || !mob.getTarget().isAlive())) mob.setTarget(target);
                if (distanceToCenterSqr(mob, active.center) > EVENT_RADIUS * EVENT_RADIUS) {
                    mob.getNavigation().moveTo(active.center.getX() + 0.5D, active.center.getY(), active.center.getZ() + 0.5D, 1.25D);
                }
            }
            active.mobIds.clear();
            active.mobIds.addAll(unresolved);
            if (active.mobIds.isEmpty()) {
                complete(active);
                return;
            }
        }
        updateBossBar(active);
    }

    private static Set<UUID> spawnAmbush(ServerPlayer player, ActiveIncident active) {
        Set<UUID> spawned = new HashSet<>();
        List<String> types = active.incident.mobTypeIds();
        int spawnTarget = active.spawnTarget();

        String contentTypeId = null;
        int contentSlot = -1;
        if (active.incident.region() != ExpeditionRegion.OCEAN
                && active.level.getRandom().nextDouble() < CONTENT_REPLACEMENT_CHANCE) {
            contentTypeId = ContentPackCompatibility.randomIncidentReinforcementId(
                    active.level.getRandom(), active.incident.region().requiredWorldStage());
            if (contentTypeId != null) contentSlot = active.level.getRandom().nextInt(Math.max(1, spawnTarget));
        }

        for (int i = 0; i < spawnTarget; i++) {
            String vanillaTypeId = types.get(i % types.size());
            boolean contentAttempt = i == contentSlot && contentTypeId != null;
            Mob mob = contentAttempt
                    ? spawnOne(active.level, active.center, false, contentTypeId, i, spawnTarget)
                    : spawnOne(active.level, active.center, active.incident.region() == ExpeditionRegion.OCEAN,
                            vanillaTypeId, i, spawnTarget);
            boolean contentSpawned = contentAttempt && mob != null;
            if (mob == null && contentAttempt) {
                mob = spawnOne(active.level, active.center, false, vanillaTypeId, i, spawnTarget);
            }
            if (mob == null) continue;
            markIncidentMob(mob, active);
            mob.setGlowingTag(true);
            mob.setTarget(player);
            spawned.add(mob.getUUID());
            if (contentSpawned) active.contentReplacementCount = 1;
        }
        return spawned;
    }

    private static Mob spawnRareReinforcement(ServerPlayer player, ActiveIncident active) {
        if (!active.rare || active.incident.kind() != ExpeditionIncident.Kind.AMBUSH
                || active.incident.region() == ExpeditionRegion.OCEAN) {
            return null;
        }
        String typeId = ContentPackCompatibility.randomIncidentReinforcementId(
                active.level.getRandom(), active.incident.region().requiredWorldStage());
        if (typeId == null) return null;
        int baseCount = Math.max(1, active.spawnTarget());
        Mob mob = spawnOne(active.level, active.center, false, typeId, baseCount, baseCount + 1);
        if (mob == null) return null;
        markIncidentMob(mob, active);
        mob.setGlowingTag(true);
        mob.setTarget(player);
        return mob;
    }

    private static void markIncidentMob(Mob mob, ActiveIncident active) {
        mob.setPersistenceRequired();
        mob.getPersistentData().putString(INCIDENT_OWNER_KEY, active.owner.toString());
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

    private static void complete(ActiveIncident active) {
        if (ACTIVE.remove(active.owner) != active) return;
        cleanupMobs(active);
        closeBossBar(active.bossBar);
        for (UUID id : new HashSet<>(active.participants)) {
            ServerPlayer member = active.level.getServer().getPlayerList().getPlayer(id);
            if (member == null || member.level() != active.level) continue;
            ExpeditionData data = ExpeditionData.get(member);
            if (!data.claimIncidentReward(member, active.incident.region())) continue;
            int stage = active.incident.region().requiredWorldStage();
            int skillXp = (100 + stage * 50) * (active.rare ? 2 : 1);
            SkillProgressionService.award(member, active.incident.region().rewardSkill(), skillXp);
            if (stage == 0) {
                giveOrDrop(member, new ItemStack(Items.EMERALD, active.rare ? 10 : 4));
                giveOrDrop(member, new ItemStack(Items.AMETHYST_SHARD, active.rare ? 20 : 8));
            } else if (stage == 1) {
                giveOrDrop(member, new ItemStack(Items.DIAMOND, active.rare ? 5 : 2));
                giveOrDrop(member, new ItemStack(Items.ECHO_SHARD, active.rare ? 10 : 4));
            } else {
                giveOrDrop(member, new ItemStack(Items.DIAMOND, active.rare ? 8 : 4));
                giveOrDrop(member, new ItemStack(Items.ECHO_SHARD, active.rare ? 16 : 8));
            }
            member.sendSystemMessage(Component.literal((active.rare ? "§d[희귀 공동 사건 해결] " : "§a[공동 사건 해결] ")
                    + "§f" + active.incident.koreanName() + " §7· " + active.incident.region().rewardSkill().koreanName() + " 숙련 XP +" + skillXp));
            ExpeditionDirective.Task bonusTask = data.firstIncompleteTask(member, active.incident.region());
            if (bonusTask != null) {
                int bonus = Math.max(1, bonusTask.target() / (active.rare ? 3 : 5));
                ExpeditionProgression.grantIncidentBonus(member, active.incident.region(), bonusTask.action(), bonus);
            }
        }
    }

    private static void fail(ActiveIncident active, String reason) {
        if (ACTIVE.remove(active.owner) != active) return;
        cleanupMobs(active);
        closeBossBar(active.bossBar);
        notify(active.level, active.participants, Component.literal("§c[공동 사건 실패] §f" + active.incident.koreanName() + " · " + reason
                + " §7· 개인 원정 지령 진행도는 잃지 않습니다."), false);
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

    private static boolean overlapsActiveIncident(ServerLevel level, BlockPos center, UUID ignoredOwner) {
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

    private static ActiveIncident findActiveFor(ServerPlayer player, ExpeditionAction action) {
        for (ActiveIncident active : ACTIVE.values()) {
            if (active.level != player.level() || active.incident.kind() != ExpeditionIncident.Kind.ACTION_RUSH) continue;
            if (active.incident.action() != action) continue;
            if (qualifiesParticipant(player, active.level, active.center, active.incident.region(), EVENT_RADIUS)) return active;
        }
        return null;
    }

    private static boolean qualifiesParticipant(ServerPlayer player, ServerLevel level, BlockPos center,
                                                ExpeditionRegion region, double radius) {
        return player.level() == level && player.isAlive() && !player.isCreative() && !player.isSpectator()
                && !FinalAscensionSystem.isFinalSequenceActive(player)
                && ExpeditionProgression.currentRegion(player) == region
                && ExpeditionData.get(player).isDiscovered(player, region)
                && distanceToCenterSqr(player, center) <= radius * radius;
    }

    private static List<ServerPlayer> eligiblePlayers(ServerLevel level, BlockPos center, ExpeditionRegion region, double radius) {
        List<ServerPlayer> result = new ArrayList<>();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (qualifiesParticipant(player, level, center, region, radius)) result.add(player);
        }
        return result;
    }

    private static void syncPendingParticipants(PendingIncident pending) {
        Set<ServerPlayer> viewers = new HashSet<>(eligiblePlayers(pending.level, pending.center, pending.region, COOP_JOIN_RADIUS));
        for (ServerPlayer viewer : viewers) {
            if (distanceToCenterSqr(viewer, pending.center) <= EVENT_RADIUS * EVENT_RADIUS) pending.participants.add(viewer.getUUID());
            if (!pending.bossBar.getPlayers().contains(viewer)) pending.bossBar.addPlayer(viewer);
        }
        for (ServerPlayer viewer : List.copyOf(pending.bossBar.getPlayers())) if (!viewers.contains(viewer)) pending.bossBar.removePlayer(viewer);
    }

    private static void syncActiveParticipants(ActiveIncident active) {
        Set<ServerPlayer> viewers = new HashSet<>(eligiblePlayers(active.level, active.center, active.incident.region(), COOP_JOIN_RADIUS));
        for (ServerPlayer viewer : viewers) {
            if (distanceToCenterSqr(viewer, active.center) <= EVENT_RADIUS * EVENT_RADIUS) active.participants.add(viewer.getUUID());
            if (!active.bossBar.getPlayers().contains(viewer)) active.bossBar.addPlayer(viewer);
        }
        for (ServerPlayer viewer : List.copyOf(active.bossBar.getPlayers())) if (!viewers.contains(viewer)) active.bossBar.removePlayer(viewer);
    }

    private static ServerPlayer replacementController(ServerLevel level, BlockPos center, ExpeditionRegion region, Set<UUID> participants) {
        for (UUID id : participants) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null && qualifiesParticipant(player, level, center, region, COOP_JOIN_RADIUS)) return player;
        }
        return null;
    }

    private static void notify(ServerLevel level, Set<UUID> participants, Component message, boolean actionbar) {
        for (UUID id : participants) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null && player.level() == level) player.sendSystemMessage(message, actionbar);
        }
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        List<UUID> activeOwners = new ArrayList<>();
        for (Map.Entry<UUID, ActiveIncident> entry : ACTIVE.entrySet()) {
            ActiveIncident active = entry.getValue();
            if (active.level.getServer() != event.getServer()) continue;
            cleanupMobs(active);
            closeBossBar(active.bossBar);
            activeOwners.add(entry.getKey());
        }
        for (UUID owner : activeOwners) ACTIVE.remove(owner);

        List<UUID> pendingOwners = new ArrayList<>();
        for (Map.Entry<UUID, PendingIncident> entry : PENDING.entrySet()) {
            PendingIncident pending = entry.getValue();
            if (pending.level.getServer() != event.getServer()) continue;
            closeBossBar(pending.bossBar);
            pendingOwners.add(entry.getKey());
        }
        for (UUID owner : pendingOwners) PENDING.remove(owner);
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
        UUID owner;
        final ServerLevel level;
        final BlockPos center;
        final ExpeditionRegion region;
        final ExpeditionIncident incident;
        final long openTick;
        final boolean rare;
        final ServerBossEvent bossBar;
        final Set<UUID> participants = new HashSet<>();

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
        UUID owner;
        final ServerLevel level;
        final BlockPos center;
        final ExpeditionIncident incident;
        final long deadline;
        final boolean rare;
        final ServerBossEvent bossBar;
        final Set<UUID> mobIds = new HashSet<>();
        final Set<UUID> participants = new HashSet<>();
        final int participantCountSnapshot;
        int initialMobCount;
        int contentReplacementCount;
        int reinforcementCount;
        int actionProgress;
        int outsideTicks;

        ActiveIncident(UUID owner, ServerLevel level, BlockPos center, ExpeditionIncident incident, long deadline, boolean rare, int participantCountSnapshot) {
            this.owner = owner;
            this.level = level;
            this.center = center.immutable();
            this.incident = incident;
            this.deadline = deadline;
            this.rare = rare;
            this.participantCountSnapshot = Math.max(1, participantCountSnapshot);
            this.bossBar = new ServerBossEvent(UUID.randomUUID(), Component.literal(rare ? "희귀 현장 사건" : "현장 사건"),
                    rare ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
        }

        int actionTarget() {
            int base = incident.actionTarget();
            if (rare && base > 0) base = Math.max(base + 1, (base * 3 + 1) / 2);
            double coop = 1.0D + Math.min(4, participantCountSnapshot - 1) * 0.55D;
            return base <= 0 ? base : Math.max(base, (int)Math.ceil(base * coop));
        }

        int spawnTarget() {
    int base = incident.spawnCount();
    if (rare && base > 0) base = Math.max(base + 2, (base * 3 + 1) / 2);
    double coop = 1.0D + Math.min(4, participantCountSnapshot - 1) * 0.65D;
    return base <= 0 ? base : Math.max(base, (int)Math.ceil(base * coop));
}

    }
}
