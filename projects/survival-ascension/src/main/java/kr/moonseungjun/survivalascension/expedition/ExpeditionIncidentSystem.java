package kr.moonseungjun.survivalascension.expedition;

import kr.moonseungjun.survivalascension.progress.SkillProgressionService;
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
    private static final double EVENT_RADIUS = 48.0D;
    private static final Map<UUID, ActiveIncident> ACTIVE = new HashMap<>();

    private ExpeditionIncidentSystem() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        if (player.tickCount % 5 != 0) return;

        removeStaleServerIncidents(level.getServer());
        ActiveIncident active = ACTIVE.get(player.getUUID());
        if (active != null) {
            tickActive(player, active);
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

        start(player, level, region, ExpeditionIncident.random(region, level.getRandom()));
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ActiveIncident active = ACTIVE.remove(event.getEntity().getUUID());
        if (active != null) {
            cleanupMobs(active);
            closeBossBar(active);
        }
    }

    public static void recordAction(ServerPlayer player, ExpeditionAction action, int amount) {
        if (amount <= 0) return;
        ActiveIncident active = ACTIVE.get(player.getUUID());
        if (active == null || active.incident.kind() != ExpeditionIncident.Kind.ACTION_RUSH) return;
        if (active.incident.action() != action || player.level() != active.level) return;
        if (ExpeditionProgression.currentRegion(player) != active.incident.region()) return;
        active.actionProgress = Math.min(active.incident.actionTarget(), active.actionProgress + amount);
        updateBossBar(active);
        if (active.actionProgress >= active.incident.actionTarget()) complete(player, active);
    }

    private static void start(ServerPlayer player, ServerLevel level, ExpeditionRegion region, ExpeditionIncident incident) {
        long now = level.getGameTime();
        player.getPersistentData().putLong(READY_TICK_KEY, now + START_COOLDOWN_TICKS);
        ActiveIncident active = new ActiveIncident(player.getUUID(), level, player.blockPosition(), incident, now + incident.durationTicks());

        if (incident.kind() == ExpeditionIncident.Kind.AMBUSH) {
            Set<UUID> spawned = spawnAmbush(player, active);
            int minimum = Math.max(3, incident.spawnCount() * 2 / 3);
            if (spawned.size() < minimum) {
                for (UUID id : spawned) {
                    Entity entity = level.getEntity(id);
                    if (entity != null) entity.discard();
                }
                player.getPersistentData().putLong(READY_TICK_KEY, now + 1200);
                return;
            }
            active.mobIds.addAll(spawned);
            active.initialMobCount = spawned.size();
        }

        ACTIVE.put(player.getUUID(), active);
        active.bossBar.addPlayer(player);
        active.bossBar.setVisible(true);
        updateBossBar(active);
        if (incident.kind() == ExpeditionIncident.Kind.AMBUSH) {
            player.sendSystemMessage(Component.literal("§c[현장 사건] §f" + region.koreanName() + " · §e" + incident.koreanName()
                    + " §7· 반경 48블록 안에서 습격대 " + active.initialMobCount + "체를 정리하세요."));
        } else {
            player.sendSystemMessage(Component.literal("§6[현장 사건] §f" + region.koreanName() + " · §e" + incident.koreanName()
                    + " §7· 제한시간 안에 " + incident.action().koreanName() + " §e" + incident.actionTarget() + "§7을 수행하세요."));
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

        if (active.incident.kind() == ExpeditionIncident.Kind.AMBUSH) {
            Set<UUID> alive = new HashSet<>();
            for (UUID id : active.mobIds) {
                Entity entity = active.level.getEntity(id);
                if (!(entity instanceof Mob mob) || !mob.isAlive()) continue;
                alive.add(id);
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
        for (int i = 0; i < active.incident.spawnCount(); i++) {
            String typeId = types.get(i % types.size());
            Mob mob = spawnOne(active.level, active.center, active.incident.region() == ExpeditionRegion.OCEAN,
                    typeId, i, active.incident.spawnCount());
            if (mob == null) continue;
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
        closeBossBar(active);
        if (!firstResolution) return;

        int stage = active.incident.region().requiredWorldStage();
        int skillXp = 100 + stage * 50;
        SkillProgressionService.award(player, active.incident.region().rewardSkill(), skillXp);
        if (stage == 0) {
            giveOrDrop(player, new ItemStack(Items.EMERALD, 4));
            giveOrDrop(player, new ItemStack(Items.AMETHYST_SHARD, 8));
        } else if (stage == 1) {
            giveOrDrop(player, new ItemStack(Items.DIAMOND, 2));
            giveOrDrop(player, new ItemStack(Items.ECHO_SHARD, 4));
        } else {
            giveOrDrop(player, new ItemStack(Items.DIAMOND, 4));
            giveOrDrop(player, new ItemStack(Items.ECHO_SHARD, 8));
        }

        player.sendSystemMessage(Component.literal("§a[현장 사건 해결] §f" + active.incident.region().koreanName() + " · §e"
                + active.incident.koreanName() + " §7· " + active.incident.region().rewardSkill().koreanName()
                + " 숙련 XP +" + skillXp));

        ExpeditionDirective.Task bonusTask = data.firstIncompleteTask(player, active.incident.region());
        if (bonusTask != null) {
            int bonus = Math.max(1, bonusTask.target() / 5);
            player.sendSystemMessage(Component.literal("§6[현장 사건 보너스] §f현재 지령의 §e" + bonusTask.action().koreanName()
                    + " §f진행도에 최대 §e" + bonus + "§f를 추가합니다."));
            ExpeditionProgression.grantIncidentBonus(player, active.incident.region(), bonusTask.action(), bonus);
        }
    }

    private static void fail(ServerPlayer player, ActiveIncident active, String reason) {
        if (ACTIVE.remove(player.getUUID()) != active) return;
        cleanupMobs(active);
        closeBossBar(active);
        player.sendSystemMessage(Component.literal("§c[현장 사건 실패] §f" + active.incident.koreanName() + " · " + reason
                + " §7· 원정 지령 진행도는 잃지 않으며 이후 다시 발생할 수 있습니다."));
    }

    private static void updateBossBar(ActiveIncident active) {
        long remain = Math.max(0L, active.deadline - active.level.getGameTime());
        long seconds = (remain + 19L) / 20L;
        if (active.incident.kind() == ExpeditionIncident.Kind.AMBUSH) {
            active.bossBar.setName(Component.literal("§c현장 사건 §7[" + active.incident.koreanName() + "] §f적 "
                    + active.mobIds.size() + " · " + seconds + "초"));
            float progress = active.initialMobCount <= 0 ? 0.0F : (float) active.mobIds.size() / active.initialMobCount;
            active.bossBar.setProgress(Math.max(0.0F, Math.min(1.0F, progress)));
        } else {
            active.bossBar.setName(Component.literal("§6현장 사건 §7[" + active.incident.koreanName() + "] §f"
                    + active.actionProgress + "/" + active.incident.actionTarget() + " · " + seconds + "초"));
            float progress = active.incident.actionTarget() <= 0 ? 0.0F
                    : (float) active.actionProgress / active.incident.actionTarget();
            active.bossBar.setProgress(Math.max(0.0F, Math.min(1.0F, progress)));
        }
    }

    private static void removeStaleServerIncidents(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        List<UUID> stale = new ArrayList<>();
        for (Map.Entry<UUID, ActiveIncident> entry : ACTIVE.entrySet()) {
            if (entry.getValue().level.getServer() == server) continue;
            closeBossBar(entry.getValue());
            stale.add(entry.getKey());
        }
        for (UUID uuid : stale) ACTIVE.remove(uuid);
    }

    private static void cleanupMobs(ActiveIncident active) {
        for (UUID id : active.mobIds) {
            Entity entity = active.level.getEntity(id);
            if (entity != null) entity.discard();
        }
        active.mobIds.clear();
    }

    private static void closeBossBar(ActiveIncident active) {
        active.bossBar.setVisible(false);
        for (ServerPlayer viewer : List.copyOf(active.bossBar.getPlayers())) active.bossBar.removePlayer(viewer);
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

    private static final class ActiveIncident {
        final UUID owner;
        final ServerLevel level;
        final BlockPos center;
        final ExpeditionIncident incident;
        final long deadline;
        final ServerBossEvent bossBar;
        final Set<UUID> mobIds = new HashSet<>();
        int initialMobCount;
        int actionProgress;
        int outsideTicks;

        ActiveIncident(UUID owner, ServerLevel level, BlockPos center, ExpeditionIncident incident, long deadline) {
            this.owner = owner;
            this.level = level;
            this.center = center.immutable();
            this.incident = incident;
            this.deadline = deadline;
            this.bossBar = new ServerBossEvent(UUID.randomUUID(), Component.literal("현장 사건"),
                    BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
        }
    }
}
