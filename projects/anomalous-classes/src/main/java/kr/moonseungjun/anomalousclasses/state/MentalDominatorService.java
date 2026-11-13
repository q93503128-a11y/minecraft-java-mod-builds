package kr.moonseungjun.anomalousclasses.state;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.network.ClassNetwork;
import kr.moonseungjun.anomalousclasses.network.payload.MentalDominatorActionPayload;
import kr.moonseungjun.anomalousclasses.network.payload.MentalDominatorStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.ProfileStatePayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.network.chat.Component;

/** Server-authoritative mental intrusion, original-entity control, resource drain and management actions. */
public final class MentalDominatorService {
    private static final String ENTITY_TAG = "anomalousclasses_mental_control";
    private static final int TICKS_PER_SECOND = 20;
    private static final Map<UUID, RuntimeGoals> RUNTIME_GOALS = new HashMap<>();
    private static final Map<UUID, Channel> CHANNELS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_INTRUSION_TICK = new HashMap<>();
    private static MinecraftServer tickServer;
    private static long tickCounter;

    private MentalDominatorService() {}

    private record RuntimeGoals(MentalCommandGoal command, MentalAssistTargetGoal assist) {}

    private static final class Channel {
        private UUID target;
        private double progress;
        private boolean active;
    }

    public static void openManagement(ServerPlayer player) {
        PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        if (profile.currentClass().filter(id -> id == ClassId.MENTAL_DOMINATOR).isEmpty()) {
            ClassSelectionService.openMenu(player);
            return;
        }
        sendState(player, 0, false, true);
    }

    public static void refreshManagement(ServerPlayer player) {
        if (player.getServer() == null || !isMental(player)) return;
        sendState(player, 0, false, false);
    }

    public static void handleAction(ServerPlayer player, MentalDominatorActionPayload payload) {
        PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        MentalDominatorSavedData data = MentalDominatorSavedData.get(player.getServer());
        MentalDominatorSavedData.PlayerState ownerState = data.player(player.getUUID());
        if (profile.currentClass().filter(id -> id == ClassId.MENTAL_DOMINATOR).isEmpty()) {
            ProfileService.openManagement(player);
            return;
        }
        if (payload.expectedRevision() < 0 || payload.expectedRevision() != ownerState.revision()) {
            sendState(player, 1, false, false);
            return;
        }

        int result = 0;
        int action = payload.action();
        if (action == MentalDominatorActionPayload.ALL_FOLLOW || action == MentalDominatorActionPayload.ALL_STAY) {
            MentalDominatorSavedData.CommandMode mode = action == MentalDominatorActionPayload.ALL_FOLLOW
                    ? MentalDominatorSavedData.CommandMode.FOLLOW : MentalDominatorSavedData.CommandMode.STAY;
            for (MentalDominatorSavedData.ControlledRecord record : data.controlledBy(player.getUUID())) {
                if (record.linkState() != MentalDominatorSavedData.LinkState.ACTIVE) continue;
                record.setCommand(mode);
                syncEntityMarker(player.getServer(), record);
            }
            ownerState.bumpRevision();
            data.changed();
        } else {
            UUID entityId = parseUuid(payload.entityId());
            MentalDominatorSavedData.ControlledRecord record = entityId == null ? null : data.controlled(entityId);
            if (action < 0 || action > MentalDominatorActionPayload.RELEASE) {
                result = 3;
            } else if (record == null || !record.ownerId().equals(player.getUUID())
                    || record.linkState() != MentalDominatorSavedData.LinkState.ACTIVE) {
                result = 4;
            } else if (action == MentalDominatorActionPayload.FOLLOW || action == MentalDominatorActionPayload.STAY) {
                record.setCommand(action == MentalDominatorActionPayload.FOLLOW
                        ? MentalDominatorSavedData.CommandMode.FOLLOW : MentalDominatorSavedData.CommandMode.STAY);
                syncEntityMarker(player.getServer(), record);
                ownerState.bumpRevision();
                data.changed();
            } else if (action == MentalDominatorActionPayload.TOGGLE_PLAYER_DAMAGE) {
                record.setAllowPlayerDamage(!record.allowPlayerDamage());
                syncEntityMarker(player.getServer(), record);
                ownerState.bumpRevision();
                data.changed();
            } else if (action == MentalDominatorActionPayload.RELEASE) {
                boolean loaded = releaseOrDefer(player.getServer(), data, record);
                ownerState.bumpRevision();
                data.changed();
                result = loaded ? 0 : 5;
            } else {
                result = 3;
            }
        }
        sendState(player, result, false, false);
    }

    public static void intrusionInput(ServerPlayer player, int action) {
        if (player.getServer() == null) return;
        if (action == 1) {
            stopChannel(player);
            return;
        }
        if (action != 0) return;

        PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        if (profile.currentClass().filter(id -> id == ClassId.MENTAL_DOMINATOR).isEmpty()) return;
        long now = tickCounter;
        if (now < NEXT_INTRUSION_TICK.getOrDefault(player.getUUID(), 0L)) {
            tell(player, "message.anomalousclasses.mental.cooldown");
            return;
        }
        Channel channel = CHANNELS.computeIfAbsent(player.getUUID(), ignored -> new Channel());
        if (channel.active) return; // Duplicate/retransmitted key-down does not charge twice.

        Mob target = findAimedMob(player);
        if (target == null) {
            tell(player, "message.anomalousclasses.mental.no_target");
            return;
        }
        MentalDominatorSavedData data = MentalDominatorSavedData.get(player.getServer());
        MentalDominatorSavedData.ControlledRecord existing = data.controlled(target.getUUID());
        if (existing != null) {
            tell(player, existing.ownerId().equals(player.getUUID())
                    ? "message.anomalousclasses.mental.already_controlled"
                    : "message.anomalousclasses.mental.target_owned");
            return;
        }
        int supported = captureSupportResult(player, target);
        if (supported != 0) {
            tell(player, supported == 1 ? "message.anomalousclasses.mental.unsupported_target"
                    : "message.anomalousclasses.mental.target_owned");
            return;
        }
        int level = profile.progress(ClassId.MENTAL_DOMINATOR).level();
        int max = MentalDominatorRules.maxControlled(level, profile);
        if (data.activeCount(player.getUUID()) >= max) {
            tell(player, "message.anomalousclasses.mental.slots_full");
            return;
        }
        double maxPsyche = MentalDominatorRules.maxPsyche(level, profile);
        MentalDominatorSavedData.PlayerState ownerState = data.player(player.getUUID());
        initializePsyche(ownerState, maxPsyche);
        double startCost = MentalDominatorConfig.value(MentalDominatorConfig.VALUES.intrusionStartCost);
        if (ownerState.psyche() + 1.0e-8 < startCost) {
            tell(player, "message.anomalousclasses.mental.insufficient_psyche");
            return;
        }

        ownerState.setPsyche(ownerState.psyche() - startCost);
        ownerState.setLastMaximum(maxPsyche);
        ownerState.bumpRevision();
        data.changed();
        if (!target.getUUID().equals(channel.target)) channel.progress = 0.0;
        channel.target = target.getUUID();
        channel.active = true;
        tell(player, "message.anomalousclasses.mental.intrusion_started");
    }

    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (tickServer != server) {
            RUNTIME_GOALS.clear();
            CHANNELS.clear();
            NEXT_INTRUSION_TICK.clear();
            tickServer = server;
            tickCounter = 0L;
        }
        tickCounter++;
        tickChannels(server);
        if (tickCounter % TICKS_PER_SECOND == 0L) tickResources(server);
    }

    private static void tickChannels(MinecraftServer server) {
        var iterator = CHANNELS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Channel> entry = iterator.next();
            ServerPlayer player = onlineOwner(server, entry.getKey());
            Channel channel = entry.getValue();
            if (player == null || !player.isAlive() || !isMental(player)) {
                iterator.remove();
                continue;
            }
            if (!channel.active) {
                if (channel.target == null) { iterator.remove(); continue; }
                PlayerClassProfile profile = ClassProfileSavedData.get(server).profile(player.getUUID());
                channel.progress = Math.max(0.0, channel.progress
                        - MentalDominatorRules.interruptionDecayPerSecond(profile) / TICKS_PER_SECOND);
                if (channel.progress <= 0.0) iterator.remove();
                continue;
            }
            Mob target = loadedMob(server, channel.target);
            if (target == null || findAimedMob(player) != target || captureSupportResult(player, target) != 0) {
                interrupt(player, channel, "message.anomalousclasses.mental.intrusion_interrupted");
                continue;
            }
            MentalDominatorSavedData data = MentalDominatorSavedData.get(server);
            MentalDominatorSavedData.PlayerState state = data.player(player.getUUID());
            double tickCost = MentalDominatorConfig.value(MentalDominatorConfig.VALUES.intrusionCostPerSecond)
                    / TICKS_PER_SECOND;
            if (state.psyche() + 1.0e-8 < tickCost) {
                interrupt(player, channel, "message.anomalousclasses.mental.insufficient_psyche");
                continue;
            }
            state.setPsyche(state.psyche() - tickCost);
            double threat = MentalDominatorRules.threat(target).score();
            double healthRatio = target.getMaxHealth() <= 0.0F ? 1.0
                    : target.getHealth() / target.getMaxHealth();
            ClassProgress progress = ClassProfileSavedData.get(server).profile(player.getUUID())
                    .progress(ClassId.MENTAL_DOMINATOR);
            channel.progress = Math.min(100.0, channel.progress
                    + MentalDominatorRules.intrusionProgressPerSecond(progress.level(), healthRatio, threat,
                    ClassProfileSavedData.get(server).profile(player.getUUID())) / TICKS_PER_SECOND);
            if (tickCounter % 5L == 0L) {
                player.displayClientMessage(Component.translatable(
                        "message.anomalousclasses.mental.intrusion_progress", Math.round(channel.progress)), true);
            }
            if (channel.progress >= 100.0) {
                if (completeCapture(player, target, threat)) {
                    channel.active = false;
                    channel.progress = 0.0;
                    channel.target = null;
                    NEXT_INTRUSION_TICK.put(player.getUUID(), tickCounter
                            + MentalDominatorConfig.value(MentalDominatorConfig.VALUES.intrusionCooldownTicks));
                    data.changed();
                    iterator.remove();
                } else {
                    interrupt(player, channel, "message.anomalousclasses.mental.capture_failed");
                }
            } else if (tickCounter % TICKS_PER_SECOND == 0L) data.changed();
        }
    }

    private static void tickResources(MinecraftServer server) {
        MentalDominatorSavedData data = MentalDominatorSavedData.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!isMental(player) || !player.isAlive()) continue;
            PlayerClassProfile profile = ClassProfileSavedData.get(server).profile(player.getUUID());
            MentalDominatorSavedData.PlayerState state = data.player(player.getUUID());
            double maxPsyche = MentalDominatorRules.maxPsyche(profile.progress(ClassId.MENTAL_DOMINATOR).level(), profile);
            initializePsyche(state, maxPsyche);

            List<MentalDominatorSavedData.ControlledRecord> loaded = loadedOwned(server, data, player.getUUID());
            int level = profile.progress(ClassId.MENTAL_DOMINATOR).level();
            double upkeep = upkeepFor(loaded, level, profile);
            double regeneration = channelActive(player.getUUID()) ? 0.0
                    : MentalDominatorRules.regenerationPerSecond(level);
            double availableAfterTick = state.psyche() + regeneration - upkeep;

            if (availableAfterTick < -1.0e-8 && !loaded.isEmpty()) {
                // Keep valuable high-threat creatures longest; among similar threats, the newest leaves first.
                List<MentalDominatorSavedData.ControlledRecord> releaseOrder = new ArrayList<>(loaded);
                releaseOrder.sort(Comparator.comparingDouble(MentalDominatorSavedData.ControlledRecord::threat)
                        .thenComparing(Comparator.comparingLong(MentalDominatorSavedData.ControlledRecord::capturedAt).reversed()));
                for (MentalDominatorSavedData.ControlledRecord record : releaseOrder) {
                    if (availableAfterTick >= -1.0e-8) break;
                    releaseOrDefer(server, data, record);
                    loaded.remove(record);
                    upkeep = upkeepFor(loaded, level, profile);
                    availableAfterTick = state.psyche() + regeneration - upkeep;
                }
            }

            state.setPsyche(Math.min(maxPsyche, Math.max(0.0, availableAfterTick)));
            state.setLastMaximum(maxPsyche);
            for (MentalDominatorSavedData.ControlledRecord record : data.controlledBy(player.getUUID())) {
                if (record.linkState() != MentalDominatorSavedData.LinkState.ACTIVE) continue;
                Mob entity = loadedMob(server, record.entityId());
                if (entity != null && entity.isAlive()) {
                    record.updateLocation(entity.level().dimension().location().toString(), entity.getX(), entity.getY(), entity.getZ(),
                            entity.getHealth(), entity.getMaxHealth());
                    record.setThreat(MentalDominatorRules.threat(entity).score());
                }
            }
            data.changed();
        }
        for (MentalDominatorSavedData.ControlledRecord record : data.allControlled()) {
            if (record.linkState() != MentalDominatorSavedData.LinkState.ACTIVE) continue;
            ServerPlayer owner = onlineOwner(server, record.ownerId());
            Mob entity = loadedMob(server, record.entityId());
            if (entity == null) continue;
            if (owner == null || !owner.isAlive() || !isMental(owner)) suspend(entity, record);
            else resume(entity, record, owner);
        }
    }

    private static double upkeepFor(List<MentalDominatorSavedData.ControlledRecord> records, int level,
                                    PlayerClassProfile profile) {
        if (records.isEmpty()) return 0.0;
        double total = 0.0;
        for (MentalDominatorSavedData.ControlledRecord record : records) {
            total += MentalDominatorRules.maintenancePerSecond(record.threat(), level, profile);
        }
        return total + MentalDominatorRules.groupMaintenancePerSecond(records.size(), profile);
    }

    private static List<MentalDominatorSavedData.ControlledRecord> loadedOwned(MinecraftServer server,
                                                                                MentalDominatorSavedData data,
                                                                                UUID ownerId) {
        List<MentalDominatorSavedData.ControlledRecord> loaded = new ArrayList<>();
        for (MentalDominatorSavedData.ControlledRecord record : data.controlledBy(ownerId)) {
            if (record.linkState() != MentalDominatorSavedData.LinkState.ACTIVE) continue;
            Mob mob = loadedMob(server, record.entityId());
            if (mob != null && mob.isAlive()) loaded.add(record);
        }
        return loaded;
    }

    private static boolean completeCapture(ServerPlayer player, Mob target, double threat) {
        if (captureSupportResult(player, target) != 0 || MentalDominatorSavedData.get(player.getServer()).controlled(target.getUUID()) != null) {
            return false;
        }
        PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        MentalDominatorSavedData data = MentalDominatorSavedData.get(player.getServer());
        int level = profile.progress(ClassId.MENTAL_DOMINATOR).level();
        if (data.activeCount(player.getUUID()) >= MentalDominatorRules.maxControlled(level, profile)) return false;

        CompoundTag persistent = target.getPersistentData();
        if (persistent.contains(ENTITY_TAG)) return false;
        data.clearReleased(target.getUUID());
        CompoundTag marker = new CompoundTag();
        marker.putUUID("owner", player.getUUID());
        marker.putBoolean("originalNoAi", target.isNoAi());
        marker.putBoolean("originalPersistence", target.isPersistenceRequired());
        marker.putString("command", MentalDominatorSavedData.CommandMode.FOLLOW.name());
        marker.putBoolean("allowPlayerDamage", data.player(player.getUUID()).allowPlayerDamageByDefault());
        marker.putLong("capturedAt", System.currentTimeMillis());
        marker.putDouble("threat", threat);
        persistent.put(ENTITY_TAG, marker);
        target.setTarget(null);

        String entityType = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        String displayName = target.hasCustomName() ? target.getCustomName().getString() : "";
        MentalDominatorSavedData.ControlledRecord record = new MentalDominatorSavedData.ControlledRecord(
                target.getUUID(), player.getUUID(), entityType, displayName,
                target.level().dimension().location().toString(), target.getX(), target.getY(), target.getZ(),
                target.getHealth(), target.getMaxHealth(), threat, MentalDominatorSavedData.CommandMode.FOLLOW,
                data.player(player.getUUID()).allowPlayerDamageByDefault(), marker.getLong("capturedAt"),
                MentalDominatorSavedData.LinkState.ACTIVE);
        data.put(record);
        data.player(player.getUUID()).recordSpecies(entityType);
        data.player(player.getUUID()).bumpRevision();
        attach(target, record);
        tell(player, "message.anomalousclasses.mental.capture_success");
        return true;
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || event.getLevel().isClientSide()) return;
        MinecraftServer server = event.getLevel().getServer();
        if (server == null) return;
        MentalDominatorSavedData data = MentalDominatorSavedData.get(server);
        if (data.wasReleased(mob.getUUID())) {
            if (mob.getPersistentData().contains(ENTITY_TAG)) {
                CompoundTag releasedMarker = mob.getPersistentData().getCompound(ENTITY_TAG);
                if (releasedMarker.hasUUID("owner")) mob.setNoAi(releasedMarker.getBoolean("originalNoAi"));
                mob.getPersistentData().remove(ENTITY_TAG);
                mob.setTarget(null);
                data.remove(mob.getUUID());
                data.changed();
            } else {
                data.remove(mob.getUUID());
                data.clearReleased(mob.getUUID()); // A chunk reload confirms the cleared entity tag was saved.
            }
            return;
        }
        if (!mob.getPersistentData().contains(ENTITY_TAG)) {
            data.remove(mob.getUUID());
            return;
        }
        CompoundTag marker = mob.getPersistentData().getCompound(ENTITY_TAG);
        if (!marker.hasUUID("owner")) {
            data.remove(mob.getUUID());
            return;
        }
        UUID ownerId = marker.getUUID("owner");
        MentalDominatorSavedData.ControlledRecord record = data.controlled(mob.getUUID());
        if (record != null && !record.ownerId().equals(ownerId)) {
            restoreOriginal(mob, marker, data); // Conflicting indexes are released rather than transferred.
            return;
        }
        if (record == null) {
            record = recordFromMarker(mob, ownerId, marker);
            data.put(record);
            data.player(ownerId).recordSpecies(record.entityType());
        }
        record.updateLocation(mob.level().dimension().location().toString(), mob.getX(), mob.getY(), mob.getZ(),
                mob.getHealth(), mob.getMaxHealth());
        record.setThreat(MentalDominatorRules.threat(mob).score());
        if (record.linkState() == MentalDominatorSavedData.LinkState.RELEASE_PENDING) {
            restoreOriginal(mob, marker, data);
            return;
        }
        ServerPlayer owner = onlineOwner(server, ownerId);
        PlayerClassProfile ownerProfile = ClassProfileSavedData.get(server).profile(ownerId);
        if (owner == null || !owner.isAlive()
                || ownerProfile.currentClass().filter(id -> id == ClassId.MENTAL_DOMINATOR).isEmpty()) {
            suspend(mob, record);
            return;
        }
        resume(mob, record, owner);
        data.changed();
    }

    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || event.getLevel().isClientSide()) return;
        RuntimeGoals goals = RUNTIME_GOALS.remove(mob.getUUID());
        if (goals != null) {
            mob.goalSelector.removeGoal(goals.command());
            if (goals.assist() != null) mob.targetSelector.removeGoal(goals.assist());
        }
        if (mob.level().getServer() == null) return;
        MentalDominatorSavedData data = MentalDominatorSavedData.get(mob.level().getServer());
        MentalDominatorSavedData.ControlledRecord record = data.controlled(mob.getUUID());
        if (record != null && mob.isAlive()) {
            if (!mob.getPersistentData().contains(ENTITY_TAG)) {
                data.remove(mob.getUUID());
            } else {
                record.updateLocation(mob.level().dimension().location().toString(), mob.getX(), mob.getY(), mob.getZ(),
                        mob.getHealth(), mob.getMaxHealth());
                data.changed();
            }
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || entity.level().getServer() == null) return;
        if (entity instanceof ServerPlayer player) suspendOwner(player);
        if (entity instanceof Mob mob) {
            MentalDominatorSavedData data = MentalDominatorSavedData.get(entity.level().getServer());
            data.remove(mob.getUUID());
            data.clearReleased(mob.getUUID());
            RUNTIME_GOALS.remove(mob.getUUID());
        }
    }

    public static void onMobDespawn(MobDespawnEvent event) {
        if (event.getEntity().level().getServer() == null) return;
        MentalDominatorSavedData data = MentalDominatorSavedData.get(event.getEntity().level().getServer());
        MentalDominatorSavedData.ControlledRecord record = data.controlled(event.getEntity().getUUID());
        if (record == null) return;
        CompoundTag marker = event.getEntity().getPersistentData().getCompound(ENTITY_TAG);
        boolean markerMatches = event.getEntity().getPersistentData().contains(ENTITY_TAG)
                && marker.hasUUID("owner") && marker.getUUID("owner").equals(record.ownerId());
        if (record.linkState() == MentalDominatorSavedData.LinkState.ACTIVE && markerMatches) {
            event.setResult(MobDespawnEvent.Result.DENY);
        } else if (markerMatches) {
            restoreOriginal(event.getEntity(), marker, data);
        } else {
            data.remove(record.entityId());
        }
    }

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof ServerPlayer player) {
            Channel channel = CHANNELS.get(player.getUUID());
            if (channel != null && channel.active) interrupt(player, channel,
                    "message.anomalousclasses.mental.intrusion_interrupted");
        }
    }

    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide() || event.getEntity().level().getServer() == null) return;
        MentalDominatorSavedData data = MentalDominatorSavedData.get(event.getEntity().level().getServer());
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) return;
        if (event.getEntity() instanceof Player victim) {
            MentalDominatorSavedData.ControlledRecord attackerRecord = data.controlled(attacker.getUUID());
            if (attackerRecord != null && attackerRecord.linkState() == MentalDominatorSavedData.LinkState.ACTIVE
                    && !canHarmPlayer(attackerRecord, victim)) event.setNewDamage(0.0F);
            return;
        }
        MentalDominatorSavedData.ControlledRecord victimRecord = data.controlled(event.getEntity().getUUID());
        if (victimRecord != null && victimRecord.linkState() == MentalDominatorSavedData.LinkState.ACTIVE
                && isOwnerOrAllyDamage(event.getEntity().level().getServer(), victimRecord, attacker, data)) {
            event.setNewDamage(0.0F);
        }
    }

    private static boolean isOwnerOrAllyDamage(MinecraftServer server,
                                                MentalDominatorSavedData.ControlledRecord victim,
                                                Entity attacker, MentalDominatorSavedData data) {
        if (attacker.getUUID().equals(victim.ownerId())) return true;
        if (attacker instanceof Player player) {
            ServerPlayer owner = onlineOwner(server, victim.ownerId());
            return owner == null || owner.isAlliedTo(player);
        }
        MentalDominatorSavedData.ControlledRecord attackerRecord = data.controlled(attacker.getUUID());
        if (attackerRecord != null && attackerRecord.linkState() == MentalDominatorSavedData.LinkState.ACTIVE) {
            if (attackerRecord.ownerId().equals(victim.ownerId())) return true;
            ServerPlayer victimOwner = onlineOwner(server, victim.ownerId());
            ServerPlayer attackerOwner = onlineOwner(server, attackerRecord.ownerId());
            if (victimOwner == null || attackerOwner == null) return true;
            if (victimOwner.isAlliedTo(attackerOwner)) return true;
        }
        if (attacker instanceof TamableAnimal tame && tame.getOwnerUUID() != null) {
            if (tame.getOwnerUUID().equals(victim.ownerId())) return true;
            ServerPlayer victimOwner = onlineOwner(server, victim.ownerId());
            ServerPlayer tameOwner = onlineOwner(server, tame.getOwnerUUID());
            return victimOwner == null || tameOwner == null || victimOwner.isAlliedTo(tameOwner);
        }
        return false;
    }

    private static boolean canHarmPlayer(MentalDominatorSavedData.ControlledRecord record, Player victim) {
        MinecraftServer server = victim.level().getServer();
        if (!record.allowPlayerDamage() || server == null || !server.isPvpAllowed()) return false;
        ServerPlayer owner = onlineOwner(server, record.ownerId());
        if (owner == null || owner == victim || owner.isAlliedTo(victim)) return false;
        return true;
    }

    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide() || mob.level().getServer() == null) return;
        MentalDominatorSavedData.ControlledRecord record = MentalDominatorSavedData.get(mob.level().getServer())
                .controlled(mob.getUUID());
        if (record == null || record.linkState() != MentalDominatorSavedData.LinkState.ACTIVE) return;
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target != null && !isValidCombatTarget(onlineOwner(mob.level().getServer(), record.ownerId()), target,
                record.allowPlayerDamage())) event.setNewAboutToBeSetTarget(null);
    }

    public static void onPlayerLogout(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            stopChannel(player);
            suspendOwner(player);
        }
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        resumeOwner(player);
    }

    public static boolean prepareClassChange(ServerPlayer player, ClassId previous, ClassId target) {
        if (previous != ClassId.MENTAL_DOMINATOR || target == ClassId.MENTAL_DOMINATOR) return true;
        stopChannel(player);
        MentalDominatorSavedData data = MentalDominatorSavedData.get(player.getServer());
        for (MentalDominatorSavedData.ControlledRecord record : data.controlledBy(player.getUUID())) {
            if (record.linkState() == MentalDominatorSavedData.LinkState.ACTIVE) releaseOrDefer(player.getServer(), data, record);
        }
        data.player(player.getUUID()).bumpRevision();
        data.changed();
        return true;
    }

    private static void suspendOwner(ServerPlayer player) {
        if (player.getServer() == null) return;
        MentalDominatorSavedData data = MentalDominatorSavedData.get(player.getServer());
        for (MentalDominatorSavedData.ControlledRecord record : data.controlledBy(player.getUUID())) {
            if (record.linkState() != MentalDominatorSavedData.LinkState.ACTIVE) continue;
            Mob mob = loadedMob(player.getServer(), record.entityId());
            if (mob != null) suspend(mob, record);
        }
        data.changed();
    }

    private static void resumeOwner(ServerPlayer player) {
        if (player.getServer() == null) return;
        MentalDominatorSavedData data = MentalDominatorSavedData.get(player.getServer());
        if (!isMental(player)) return;
        for (MentalDominatorSavedData.ControlledRecord record : data.controlledBy(player.getUUID())) {
            if (record.linkState() != MentalDominatorSavedData.LinkState.ACTIVE) continue;
            Mob mob = loadedMob(player.getServer(), record.entityId());
            if (mob != null) resume(mob, record, player);
        }
        data.changed();
    }

    private static void suspend(Mob mob, MentalDominatorSavedData.ControlledRecord record) {
        if (!mob.getPersistentData().contains(ENTITY_TAG)) return;
        mob.getNavigation().stop();
        mob.setNoAi(true);
        RuntimeGoals goals = RUNTIME_GOALS.remove(mob.getUUID());
        if (goals != null) {
            mob.goalSelector.removeGoal(goals.command());
            if (goals.assist() != null) mob.targetSelector.removeGoal(goals.assist());
        }
    }

    private static void resume(Mob mob, MentalDominatorSavedData.ControlledRecord record, ServerPlayer owner) {
        CompoundTag marker = mob.getPersistentData().getCompound(ENTITY_TAG);
        if (!marker.hasUUID("owner") || !record.ownerId().equals(marker.getUUID("owner"))) return;
        boolean originalNoAi = marker.getBoolean("originalNoAi");
        if (mob.isNoAi() != originalNoAi) mob.setNoAi(originalNoAi);
        if (mob.isNoAi()) return;
        syncEntityMarker(mob.level().getServer(), record);
        if (!RUNTIME_GOALS.containsKey(mob.getUUID())) attach(mob, record);
    }

    private static void attach(Mob mob, MentalDominatorSavedData.ControlledRecord record) {
        RuntimeGoals previous = RUNTIME_GOALS.remove(mob.getUUID());
        if (previous != null) {
            mob.goalSelector.removeGoal(previous.command());
            if (previous.assist() != null) mob.targetSelector.removeGoal(previous.assist());
        }
        MentalCommandGoal command = new MentalCommandGoal(mob, record);
        MentalAssistTargetGoal assist = hasNativeCombatBehavior(mob) ? new MentalAssistTargetGoal(mob, record) : null;
        mob.goalSelector.addGoal(5, command);
        if (assist != null) mob.targetSelector.addGoal(0, assist);
        else RUNTIME_GOALS.put(mob.getUUID(), new RuntimeGoals(command, null));
        if (assist != null) RUNTIME_GOALS.put(mob.getUUID(), new RuntimeGoals(command, assist));
    }

    private static boolean hasNativeCombatBehavior(Mob mob) {
        if (mob instanceof Monster) return true;
        var attackDamage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        return attackDamage != null && attackDamage.getValue() > 0.0;
    }

    private static boolean releaseOrDefer(MinecraftServer server, MentalDominatorSavedData data,
                                          MentalDominatorSavedData.ControlledRecord record) {
        Mob mob = loadedMob(server, record.entityId());
        if (mob != null && mob.getPersistentData().contains(ENTITY_TAG)) {
            CompoundTag marker = mob.getPersistentData().getCompound(ENTITY_TAG);
            if (marker.hasUUID("owner") && marker.getUUID("owner").equals(record.ownerId())) {
                restoreOriginal(mob, marker, data);
                return true;
            }
        }
        record.setLinkState(MentalDominatorSavedData.LinkState.RELEASE_PENDING);
        data.changed();
        return false;
    }

    private static void restoreOriginal(Mob mob, CompoundTag marker, MentalDominatorSavedData data) {
        data.markReleased(mob.getUUID());
        RuntimeGoals goals = RUNTIME_GOALS.remove(mob.getUUID());
        if (goals != null) {
            mob.goalSelector.removeGoal(goals.command());
            if (goals.assist() != null) mob.targetSelector.removeGoal(goals.assist());
        }
        mob.getNavigation().stop();
        mob.getPersistentData().remove(ENTITY_TAG);
        mob.setNoAi(marker.getBoolean("originalNoAi"));
        mob.setTarget(null);
        data.remove(mob.getUUID());
    }

    private static void syncEntityMarker(MinecraftServer server, MentalDominatorSavedData.ControlledRecord record) {
        Mob mob = loadedMob(server, record.entityId());
        if (mob == null || !mob.getPersistentData().contains(ENTITY_TAG)) return;
        CompoundTag marker = mob.getPersistentData().getCompound(ENTITY_TAG);
        if (!marker.hasUUID("owner") || !marker.getUUID("owner").equals(record.ownerId())) return;
        marker.putString("command", record.command().name());
        marker.putBoolean("allowPlayerDamage", record.allowPlayerDamage());
        mob.getPersistentData().put(ENTITY_TAG, marker);
        if (record.command() == MentalDominatorSavedData.CommandMode.STAY) mob.getNavigation().stop();
    }

    private static MentalDominatorSavedData.ControlledRecord recordFromMarker(Mob mob, UUID ownerId, CompoundTag marker) {
        MentalDominatorSavedData.CommandMode command;
        try { command = MentalDominatorSavedData.CommandMode.valueOf(marker.getString("command")); }
        catch (IllegalArgumentException ignored) { command = MentalDominatorSavedData.CommandMode.FOLLOW; }
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        String name = mob.hasCustomName() ? mob.getCustomName().getString() : "";
        return new MentalDominatorSavedData.ControlledRecord(mob.getUUID(), ownerId, type, name,
                mob.level().dimension().location().toString(), mob.getX(), mob.getY(), mob.getZ(),
                mob.getHealth(), mob.getMaxHealth(), marker.getDouble("threat"), command,
                marker.getBoolean("allowPlayerDamage"), marker.getLong("capturedAt"),
                MentalDominatorSavedData.LinkState.ACTIVE);
    }

    private static void stopChannel(ServerPlayer player) {
        Channel channel = CHANNELS.get(player.getUUID());
        if (channel == null || !channel.active) return;
        channel.active = false;
        NEXT_INTRUSION_TICK.put(player.getUUID(), tickCounter
                + MentalDominatorConfig.value(MentalDominatorConfig.VALUES.intrusionCooldownTicks));
    }

    private static void interrupt(ServerPlayer player, Channel channel, String message) {
        channel.active = false;
        NEXT_INTRUSION_TICK.put(player.getUUID(), tickCounter
                + MentalDominatorConfig.value(MentalDominatorConfig.VALUES.intrusionCooldownTicks));
        tell(player, message);
    }

    private static void initializePsyche(MentalDominatorSavedData.PlayerState state, double maximum) {
        if (!state.initialized()) {
            state.setPsyche(maximum);
            state.setInitialized(true);
        } else if (state.psyche() > maximum) {
            state.setPsyche(maximum);
        }
        state.setLastMaximum(maximum);
    }

    private static boolean channelActive(UUID playerId) {
        Channel channel = CHANNELS.get(playerId);
        return channel != null && channel.active;
    }

    private static Mob findAimedMob(ServerPlayer player) {
        double range = MentalDominatorConfig.value(MentalDominatorConfig.VALUES.maxCaptureRange);
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        BlockHitResult blockHit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        double visibleDistance = blockHit.getType() == HitResult.Type.MISS
                ? range * range : start.distanceToSqr(blockHit.getLocation());
        AABB search = player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1.0);
        Mob nearest = null;
        double nearestDistance = visibleDistance;
        for (Mob candidate : player.level().getEntitiesOfClass(Mob.class, search,
                mob -> mob.isAlive() && mob.isPickable())) {
            Optional<Vec3> intercept = candidate.getBoundingBox().inflate(0.3).clip(start, end);
            if (intercept.isEmpty()) continue;
            double distance = start.distanceToSqr(intercept.get());
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    /** 0 supported, 1 unsupported entity family, 2 protected/owned by another player. */
    private static int captureSupportResult(ServerPlayer player, Mob target) {
        if (!(target instanceof PathfinderMob) || target.isNoAi() || target.isRemoved() || !target.isAlive()) return 1;
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (typeId == null || !"minecraft".equals(typeId.getNamespace())) return 1;
        if (target instanceof TamableAnimal tame && tame.isTame()) {
            UUID owner = tame.getOwnerUUID();
            if (owner != null && !owner.equals(player.getUUID())) return 2;
        }
        if (target.getPersistentData().contains(ENTITY_TAG)) return 2;
        if (MentalDominatorSavedData.get(player.getServer()).controlled(target.getUUID()) != null) return 2;
        return 0;
    }

    public static boolean isValidCombatTarget(ServerPlayer owner, LivingEntity target) {
        return isValidCombatTarget(owner, target, false);
    }

    static boolean isValidCombatTarget(ServerPlayer owner, LivingEntity target, boolean allowPlayers) {
        if (owner == null || owner.getServer() == null || target == null || !target.isAlive()
                || target == owner || target.level() != owner.level()) return false;
        if (owner.isAlliedTo(target)) return false;
        if (target instanceof Player) {
            if (!allowPlayers || !owner.getServer().isPvpAllowed()) return false;
        }
        if (target instanceof TamableAnimal tame && tame.isTame()) {
            UUID tameOwner = tame.getOwnerUUID();
            if (tameOwner != null && !tameOwner.equals(owner.getUUID())) return false;
        }
        MentalDominatorSavedData.ControlledRecord targetRecord = MentalDominatorSavedData.get(owner.getServer())
                .controlled(target.getUUID());
        if (targetRecord != null) {
            if (targetRecord.ownerId().equals(owner.getUUID())) return false;
            ServerPlayer otherOwner = onlineOwner(owner.getServer(), targetRecord.ownerId());
            if (otherOwner == null || owner.isAlliedTo(otherOwner)) return false;
        }
        return true;
    }

    static ServerPlayer onlineOwner(MinecraftServer server, UUID ownerId) {
        return server == null || ownerId == null ? null : server.getPlayerList().getPlayer(ownerId);
    }

    private static boolean isMental(ServerPlayer player) {
        if (player == null || player.getServer() == null) return false;
        return ClassProfileSavedData.get(player.getServer()).profile(player.getUUID()).currentClass()
                .filter(id -> id == ClassId.MENTAL_DOMINATOR).isPresent();
    }

    private static Mob loadedMob(MinecraftServer server, UUID entityId) {
        MentalDominatorSavedData data = MentalDominatorSavedData.get(server);
        MentalDominatorSavedData.ControlledRecord record = data.controlled(entityId);
        if (record == null) return null;
        ResourceLocation dimension = ResourceLocation.tryParse(record.dimension());
        if (dimension == null) return null;
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (level == null) return null;
        Entity entity = level.getEntity(entityId); // Entity lookup only searches loaded entities; it never loads a chunk.
        return entity instanceof Mob mob && !mob.isRemoved() ? mob : null;
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.length() != 36) return null;
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private static void tell(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    private static void sendState(ServerPlayer player, int result, boolean codexView, boolean openScreen) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        ClassProfileSavedData profiles = ClassProfileSavedData.get(server);
        PlayerClassProfile profile = profiles.profile(player.getUUID());
        ClassId active = profile.currentClass().orElse(ClassId.MENTAL_DOMINATOR);
        MentalDominatorSavedData data = MentalDominatorSavedData.get(server);
        MentalDominatorSavedData.PlayerState state = data.player(player.getUUID());
        int level = profile.progress(ClassId.MENTAL_DOMINATOR).level();
        double maximum = MentalDominatorRules.maxPsyche(level, profile);
        initializePsyche(state, maximum);
        List<MentalDominatorSavedData.ControlledRecord> records = data.controlledBy(player.getUUID()).stream()
                .filter(record -> record.linkState() == MentalDominatorSavedData.LinkState.ACTIVE).toList();
        List<MentalDominatorStatePayload.MobView> views = new ArrayList<>(records.size());
        double upkeep = 0.0;
        int loadedCount = 0;
        for (MentalDominatorSavedData.ControlledRecord record : records) {
            Mob entity = loadedMob(server, record.entityId());
            boolean live = entity != null && entity.isAlive();
            int status = entity == null ? 1 : live ? 0 : 2;
            float health = 0.0F, maxHealth = 0.0F;
            String name = record.displayName();
            String dimension = record.dimension();
            int x = (int) Math.floor(record.x()), y = (int) Math.floor(record.y()), z = (int) Math.floor(record.z());
            double mobUpkeep = MentalDominatorRules.maintenancePerSecond(record.threat(), level, profile);
            if (live) {
                health = entity.getHealth(); maxHealth = entity.getMaxHealth();
                name = entity.hasCustomName() ? entity.getCustomName().getString() : "";
                dimension = entity.level().dimension().location().toString();
                x = entity.blockPosition().getX(); y = entity.blockPosition().getY(); z = entity.blockPosition().getZ();
                upkeep += mobUpkeep;
                loadedCount++;
            }
            views.add(new MentalDominatorStatePayload.MobView(record.entityId().toString(), name, record.entityType(),
                    record.command().ordinal(), status, record.allowPlayerDamage(), live, health, maxHealth,
                    mobUpkeep, dimension, x, y, z));
        }
        if (loadedCount > 0) upkeep += MentalDominatorRules.groupMaintenancePerSecond(loadedCount, profile);
        List<String> codex = data.player(player.getUUID()).codex().stream().sorted().toList();
        ClassNetwork.sendToPlayer(player, new MentalDominatorStatePayload(ProfileStatePayload.from(player, profile, active),
                state.psyche(), maximum, upkeep, records.size(), MentalDominatorRules.maxControlled(level, profile),
                state.revision(), openScreen, codexView, result, views, codex));
    }
}
