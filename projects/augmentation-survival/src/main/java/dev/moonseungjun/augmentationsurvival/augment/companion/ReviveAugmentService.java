package dev.moonseungjun.augmentationsurvival.augment.companion;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.active.ActivePresentationService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.ExpansionTuning;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.network.ActivePresentationPayload;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** Server-owned death-marker and focused resurrection runtime for permanent combat companions. */
public final class ReviveAugmentService {
    private static final String REVIVE_ID = id("revive");
    private static final String ABILITY_HASTE_ID = id("ability_haste");
    private static final String DOUBLE_CHARGE_ID = id("double_charge");
    private static final String COMBAT_CYCLE_ID = id("combat_cycle");
    private static final int MARKER_TICKS = 20 * 20;
    private static final int CHANNEL_TICKS = 3 * 20;
    private static final int BASE_COOLDOWN_TICKS = 120 * 20;
    private static final double RANGE = 20.0D;
    private static final double RANGE_SQUARED = RANGE * RANGE;
    private static final double MIN_LOOK_DOT = Math.cos(Math.toRadians(14.0D));
    private static final Map<UUID, ReviveMarker> MARKERS = new HashMap<>();
    private static final Map<UUID, ReviveChannel> CHANNELS = new HashMap<>();

    private ReviveAugmentService() {
    }

    public static void initialize() {
        for (String augmentId : new String[]{REVIVE_ID, ABILITY_HASTE_ID, DOUBLE_CHARGE_ID, COMBAT_CYCLE_ID}) {
            AugmentCatalog.require(augmentId);
        }
        ServerPlayerEvents.JOIN.register(player -> CHANNELS.remove(player.getUUID()));
        ServerPlayerEvents.LEAVE.register(player -> CHANNELS.remove(player.getUUID()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> CHANNELS.remove(newPlayer.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            MARKERS.clear();
            CHANNELS.clear();
        });
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(ReviveAugmentService::afterKill);
        ServerTickEvents.END_SERVER_TICK.register(ReviveAugmentService::tick);
    }

    /** Called from the actual LivingEntity death boundary before the corpse is removed. */
    public static void onActualDeath(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level) || entity instanceof ServerPlayer) return;
        UUID ownerId = permanentOwner(entity).orElse(null);
        if (ownerId == null) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || owner.level() != level || owner.distanceToSqr(entity) > RANGE_SQUARED) return;
        if (boundedLevel(AugmentationPlayerStateService.state(owner), REVIVE_ID) <= 0) return;
        if (MARKERS.containsKey(entity.getUUID())) return;

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        entity.saveWithoutId(output);
        CompoundTag snapshot = output.buildResult();
        snapshot.remove("UUID");
        // Marker expiry is stored against the save-wide Overworld clock so markers created in
        // dimensions with a different or paused game time are not removed early or retained forever.
        long now = level.getServer().overworld().getGameTime();
        MARKERS.put(entity.getUUID(), new ReviveMarker(
                entity.getUUID(), ownerId, entity.getType(), snapshot,
                level.dimension().identifier().toString(), entity.position(), now + MARKER_TICKS
        ));
    }

    public static boolean tryUse(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, REVIVE_ID) <= 0 || player.isDeadOrDying() || CHANNELS.containsKey(player.getUUID())) {
            return false;
        }
        ReviveMarker marker = findLookMarker(player);
        if (marker == null) return false;
        ChargeReservation reservation = calculateReservation(player, state);
        if (reservation == null) return false;
        CHANNELS.put(player.getUUID(), new ReviveChannel(marker.entityId, player.level().getGameTime() + CHANNEL_TICKS, reservation));
        return true;
    }

    public static List<Long> markerPositionsFor(ServerPlayer player) {
        if (boundedLevel(AugmentationPlayerStateService.state(player), REVIVE_ID) <= 0) return List.of();
        long now = player.level().getServer().overworld().getGameTime();
        String dimension = player.level().dimension().identifier().toString();
        List<Long> positions = new ArrayList<>();
        for (ReviveMarker marker : MARKERS.values()) {
            if (!marker.ownerId.equals(player.getUUID()) || marker.expiresAtTick <= now || !marker.dimension.equals(dimension)) continue;
            if (player.position().distanceToSqr(marker.position) > RANGE_SQUARED) continue;
            positions.add(BlockPos.containing(marker.position).asLong());
        }
        return positions;
    }

    private static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        MARKERS.values().removeIf(marker -> marker.expiresAtTick <= now);
        List<UUID> completed = new ArrayList<>();
        for (Map.Entry<UUID, ReviveChannel> entry : CHANNELS.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            ReviveChannel channel = entry.getValue();
            ReviveMarker marker = MARKERS.get(channel.markerEntityId);
            if (player == null || marker == null || player.isDeadOrDying() || !validFocus(player, marker)) {
                if (player != null) {
                    ActivePresentationService.notifyState(
                            player, REVIVE_ID, ActivePresentationPayload.CANCEL, 0, 0.0F
                    );
                }
                completed.add(entry.getKey());
                continue;
            }
            if (player.level().getGameTime() >= channel.completeAtTick) {
                if (restore(player, marker)) {
                    commitReservation(player, channel.reservation);
                    MARKERS.remove(marker.entityId);
                    ActivePresentationService.notifyStateAndPulse(
                            player, REVIVE_ID, ActivePresentationPayload.END, 0, 0.0F
                    );
                } else {
                    ActivePresentationService.notifyState(
                            player, REVIVE_ID, ActivePresentationPayload.CANCEL, 0, 0.0F
                    );
                }
                completed.add(entry.getKey());
            }
        }
        for (UUID playerId : completed) CHANNELS.remove(playerId);
    }

    private static boolean restore(ServerPlayer owner, ReviveMarker marker) {
        if (!(owner.level() instanceof ServerLevel level) || !marker.dimension.equals(level.dimension().identifier().toString())) return false;
        Entity created = marker.type.create(level, EntitySpawnReason.TRIGGERED);
        if (!(created instanceof LivingEntity living)) return false;
        CompoundTag copy = marker.snapshot.copy();
        created.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), copy));
        Vec3 safe = safePosition(level, created, marker.position);
        if (safe == null) return false;
        created.snapTo(safe.x, safe.y, safe.z);
        living.setHealth(Math.max(1.0F, living.getMaxHealth() * 0.5F));
        if (!level.addFreshEntity(created)) return false;
        if (created instanceof Mob mob && CompanionAugmentService.isDominated(mob)) {
            CompanionAugmentService.onRevivedCompanion(mob);
        }
        return true;
    }

    private static Vec3 safePosition(ServerLevel level, Entity entity, Vec3 origin) {
        for (int dy = 0; dy <= 3; dy++) {
            Vec3 candidate = origin.add(0.0D, dy, 0.0D);
            entity.snapTo(candidate.x, candidate.y, candidate.z);
            if (level.noCollision(entity, entity.getBoundingBox())) return candidate;
        }
        return null;
    }

    private static ReviveMarker findLookMarker(ServerPlayer player) {
        ReviveMarker best = null;
        double bestScore = Double.MAX_VALUE;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        for (ReviveMarker marker : MARKERS.values()) {
            if (!marker.ownerId.equals(player.getUUID()) || !validFocus(player, marker)) continue;
            Vec3 toMarker = marker.position.add(0.0D, 0.6D, 0.0D).subtract(eye);
            double distance = toMarker.length();
            if (distance <= 1.0E-5D) return marker;
            double dot = look.dot(toMarker.scale(1.0D / distance));
            if (dot < MIN_LOOK_DOT) continue;
            double score = (1.0D - dot) * 100.0D + distance * 0.01D;
            if (score < bestScore) {
                bestScore = score;
                best = marker;
            }
        }
        return best;
    }

    private static boolean validFocus(ServerPlayer player, ReviveMarker marker) {
        return marker.ownerId.equals(player.getUUID())
                && marker.expiresAtTick > player.level().getServer().overworld().getGameTime()
                && marker.dimension.equals(player.level().dimension().identifier().toString())
                && player.position().distanceToSqr(marker.position) <= RANGE_SQUARED
                && hasFocusedView(player, marker);
    }

    private static boolean hasFocusedView(ServerPlayer player, ReviveMarker marker) {
        Vec3 toMarker = marker.position.add(0.0D, 0.6D, 0.0D).subtract(player.getEyePosition());
        double distance = toMarker.length();
        if (distance <= 1.0E-5D) return true;
        return player.getLookAngle().normalize().dot(toMarker.scale(1.0D / distance)) >= MIN_LOOK_DOT;
    }

    private static Optional<UUID> permanentOwner(LivingEntity entity) {
        Optional<UUID> dominated = CompanionAugmentService.ownerId(entity);
        if (dominated.isPresent()) return dominated;
        if (entity instanceof TamableAnimal tamable && tamable.isTame() && tamable.getOwner() instanceof ServerPlayer owner) {
            return Optional.of(owner.getUUID());
        }
        return Optional.empty();
    }

    private static void afterKill(ServerLevel level, Entity killer, LivingEntity killed, DamageSource source) {
        if (!(killer instanceof ServerPlayer player)
                || !CompanionAugmentService.isHostileEntity(killed)
                || !CombatProvenanceContext.resolve(source).isRootDirect()) return;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int cycle = boundedLevel(state, COMBAT_CYCLE_ID);
        if (cycle <= 0 || boundedLevel(state, REVIVE_ID) <= 0) return;
        int reduction = 20 * cycle * (EntityCompatibilityProfileService.isBoss(killed) ? 5 : 1);
        reduceCooldown(player, state, reduction);
    }

    private static ChargeReservation calculateReservation(ServerPlayer player, AugmentationPlayerState state) {
        long now = player.level().getGameTime();
        int cooldown = BASE_COOLDOWN_TICKS;
        if (boundedLevel(state, ABILITY_HASTE_ID) > 0) {
            cooldown = Math.max(1, (int) Math.round(cooldown * (1.0D - ExpansionTuning.current().abilityHasteReduction())));
        }
        if (boundedLevel(state, DOUBLE_CHARGE_ID) <= 0) {
            return state.cooldownReadyTick(REVIVE_ID) > now ? null : new ChargeReservation(REVIVE_ID, now + cooldown);
        }
        String first = chargeKey(0);
        String second = chargeKey(1);
        long firstReady = state.cooldownReadyTick(first);
        long secondReady = state.cooldownReadyTick(second);
        String selected = firstReady <= now ? first : secondReady <= now ? second : null;
        if (selected == null) return null;
        return new ChargeReservation(selected, Math.max(now, Math.max(firstReady, secondReady)) + cooldown);
    }

    private static void reduceCooldown(ServerPlayer player, AugmentationPlayerState state, int ticks) {
        long now = player.level().getGameTime();
        AugmentationPlayerState next = state;
        if (boundedLevel(state, DOUBLE_CHARGE_ID) <= 0) {
            long ready = state.cooldownReadyTick(REVIVE_ID);
            if (ready > now) next = state.withCooldownReadyTick(REVIVE_ID, Math.max(now, ready - ticks));
        } else {
            String first = chargeKey(0), second = chargeKey(1);
            String[] keys = state.cooldownReadyTick(first) <= state.cooldownReadyTick(second)
                    ? new String[]{first, second} : new String[]{second, first};
            int remaining = ticks;
            for (String key : keys) {
                long ready = next.cooldownReadyTick(key);
                if (remaining <= 0 || ready <= now) continue;
                long applied = Math.min(ready - now, remaining);
                next = next.withCooldownReadyTick(key, ready - applied);
                remaining -= (int) applied;
            }
        }
        if (!next.equals(state)) AugmentationPlayerStateService.replace(player, next);
    }

    private static void commitReservation(ServerPlayer player, ChargeReservation reservation) {
        AugmentationPlayerState latest = AugmentationPlayerStateService.state(player);
        AugmentationPlayerStateService.replace(player, latest.withCooldownReadyTick(reservation.key, reservation.readyTick));
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String chargeKey(int index) { return REVIVE_ID + "#charge" + index; }
    private static String id(String path) { return AugmentationSurvival.MOD_ID + ":" + path; }

    private record ReviveMarker(UUID entityId, UUID ownerId, EntityType<?> type, CompoundTag snapshot,
                                String dimension, Vec3 position, long expiresAtTick) { }
    private record ChargeReservation(String key, long readyTick) { }
    private record ReviveChannel(UUID markerEntityId, long completeAtTick, ChargeReservation reservation) { }
}
