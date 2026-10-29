package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.FinalAugmentTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.ExpansionTuning;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Server-owned 3x3 rescue platform integrated with the common non-mythic active rules. */
public final class TemporaryPlatformService {
    public static final String TEMPORARY_PLATFORM_ID = id("temporary_platform");
    private static final String ABILITY_HASTE_ID = id("ability_haste");
    private static final String DOUBLE_CHARGE_ID = id("double_charge");
    private static final String COMBAT_CYCLE_ID = id("combat_cycle");
    private static final String OVERTIME_ID = id("overtime");
    private static final String REPLAY_ID = id("replay");

    private static final Block PLATFORM_BLOCK = TemporaryPlatformBlocks.TEMPORARY_PLATFORM;
    private static final BlockState PLATFORM_STATE = PLATFORM_BLOCK.defaultBlockState();
    private static final Map<UUID, List<ReplayAction>> REPLAYS = new HashMap<>();

    private TemporaryPlatformService() {
    }

    public static void initialize() {
        for (String augmentId : new String[]{
                TEMPORARY_PLATFORM_ID, ABILITY_HASTE_ID, DOUBLE_CHARGE_ID,
                COMBAT_CYCLE_ID, OVERTIME_ID, REPLAY_ID
        }) {
            AugmentCatalog.require(augmentId);
        }
        ServerPlayerEvents.JOIN.register(player -> REPLAYS.remove(player.getUUID()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> REPLAYS.remove(newPlayer.getUUID()));
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(TemporaryPlatformService::afterKill);
        ServerTickEvents.END_SERVER_TICK.register(TemporaryPlatformService::tickRuntime);
    }

    public static boolean tryUse(ServerPlayer player) {
        if (player.isDeadOrDying()) {
            return false;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, TEMPORARY_PLATFORM_ID) <= 0) {
            return false;
        }
        ChargeReservation reservation = reserveCharge(player, state);
        if (reservation == null || !execute(player, 1.0D)) {
            return false;
        }
        AugmentationPlayerState latest = AugmentationPlayerStateService.state(player);
        AugmentationPlayerStateService.replace(
                player,
                latest.withCooldownReadyTick(reservation.key, reservation.readyTick)
        );
        latest = AugmentationPlayerStateService.state(player);
        if (boundedLevel(latest, REPLAY_ID) > 0) {
            REPLAYS.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>()).add(
                    new ReplayAction(
                            player.level().getGameTime() + ExpansionTuning.current().replayDelayTicks(),
                            ExpansionTuning.current().replayEfficiency()
                    )
            );
        }
        return true;
    }

    public static boolean execute(ServerPlayer player, double efficiency) {
        if (!(efficiency > 0.0D) || player.isDeadOrDying()) {
            return false;
        }
        ServerLevel level = player.level();
        FinalAugmentTuning.Snapshot tuning = FinalAugmentTuning.current();
        BlockPos center = BlockPos.containing(
                player.getX(),
                Math.floor(player.getBoundingBox().minY) - 1.0D,
                player.getZ()
        );
        int radius = tuning.temporaryPlatformRadius();
        int placed = 0;
        int duration = Math.max(1, (int) Math.round(tuning.temporaryPlatformDurationTicks() * efficiency));
        long now = level.getGameTime();
        long expiresAt = now + duration;
        long maxExpiresAt = expiresAt + Math.round(
                duration * AdvancedActiveTuning.current().overtimeMaximumExtensionFraction()
        );
        TemporaryPlatformWorldState saved = worldState(level.getServer());
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                if (!level.getBlockState(pos).isAir()) {
                    continue;
                }
                if (!level.setBlock(pos, PLATFORM_STATE, Block.UPDATE_ALL)) {
                    continue;
                }
                saved = saved.withPlatform(
                        TemporaryPlatformWorldState.key(level.dimension(), pos.asLong()),
                        new TemporaryPlatformWorldState.PlatformRecord(
                                level.dimension().identifier().toString(), pos.asLong(), player.getUUID().toString(),
                                expiresAt, maxExpiresAt
                        )
                );
                placed++;
            }
        }
        if (placed <= 0) {
            return false;
        }
        setWorldState(level.getServer(), saved);
        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                0.8F,
                1.25F
        );
        return true;
    }

    public static boolean cancelsFallDamage(ServerPlayer player) {
        ServerLevel level = player.level();
        long now = level.getGameTime();
        AABB box = player.getBoundingBox();
        int y = (int) Math.floor(box.minY) - 1;
        int minX = (int) Math.floor(box.minX + 1.0E-4D);
        int maxX = (int) Math.floor(box.maxX - 1.0E-4D);
        int minZ = (int) Math.floor(box.minZ + 1.0E-4D);
        int maxZ = (int) Math.floor(box.maxZ - 1.0E-4D);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos pos = new BlockPos(x, y, z);
                TemporaryPlatformWorldState.PlatformRecord record = worldState(level.getServer()).platforms().get(
                        TemporaryPlatformWorldState.key(level.dimension(), pos.asLong())
                );
                if (record != null && record.expiresAtTick() >= now
                        && level.getBlockState(pos).is(PLATFORM_BLOCK)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ChargeReservation reserveCharge(ServerPlayer player, AugmentationPlayerState state) {
        long now = player.level().getGameTime();
        int cooldown = FinalAugmentTuning.current().temporaryPlatformCooldownTicks();
        if (boundedLevel(state, ABILITY_HASTE_ID) > 0) {
            cooldown = Math.max(1, (int) Math.round(
                    cooldown * (1.0D - ExpansionTuning.current().abilityHasteReduction())
            ));
        }
        if (boundedLevel(state, DOUBLE_CHARGE_ID) <= 0) {
            return state.cooldownReadyTick(TEMPORARY_PLATFORM_ID) > now
                    ? null
                    : new ChargeReservation(TEMPORARY_PLATFORM_ID, now + cooldown);
        }
        String first = chargeKey(0);
        String second = chargeKey(1);
        long firstReady = state.cooldownReadyTick(first);
        long secondReady = state.cooldownReadyTick(second);
        String selected;
        if (firstReady <= now) selected = first;
        else if (secondReady <= now) selected = second;
        else return null;
        long queueTail = Math.max(now, Math.max(firstReady, secondReady));
        return new ChargeReservation(selected, queueTail + cooldown);
    }

    private static void afterKill(ServerLevel level, Entity killer, LivingEntity killed, DamageSource source) {
        if (!(killer instanceof ServerPlayer player)
                || !CombatProvenanceContext.resolve(source).isRootDirect()) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        boolean boss = EntityCompatibilityProfileService.isBoss(killed);
        int cycle = boundedLevel(state, COMBAT_CYCLE_ID);
        if (cycle > 0 && boundedLevel(state, TEMPORARY_PLATFORM_ID) > 0) {
            reduceCooldown(player, state, 20 * cycle * (boss ? 5 : 1));
            state = AugmentationPlayerStateService.state(player);
        }
        if (boundedLevel(state, OVERTIME_ID) > 0) {
            int extension = boss
                    ? AdvancedActiveTuning.current().overtimeBossExtensionTicks()
                    : AdvancedActiveTuning.current().overtimeNormalExtensionTicks();
            extendPlatforms(player.level().getServer(), player.getUUID(), extension);
        }
    }

    private static void reduceCooldown(ServerPlayer player, AugmentationPlayerState state, int ticks) {
        if (ticks <= 0) return;
        long now = player.level().getGameTime();
        AugmentationPlayerState next = state;
        if (boundedLevel(state, DOUBLE_CHARGE_ID) <= 0) {
            long ready = state.cooldownReadyTick(TEMPORARY_PLATFORM_ID);
            if (ready > now) {
                next = state.withCooldownReadyTick(TEMPORARY_PLATFORM_ID, Math.max(now, ready - ticks));
            }
        } else {
            String first = chargeKey(0);
            String second = chargeKey(1);
            String earlier = state.cooldownReadyTick(first) <= state.cooldownReadyTick(second) ? first : second;
            String later = earlier.equals(first) ? second : first;
            int remaining = ticks;
            for (String key : new String[]{earlier, later}) {
                if (remaining <= 0) break;
                long ready = next.cooldownReadyTick(key);
                if (ready <= now) continue;
                long applied = Math.min(ready - now, remaining);
                next = next.withCooldownReadyTick(key, ready - applied);
                remaining -= (int) applied;
            }
        }
        if (!next.equals(state)) {
            AugmentationPlayerStateService.replace(player, next);
        }
    }

    private static void extendPlatforms(MinecraftServer server, UUID ownerId, int ticks) {
        if (ticks <= 0) return;
        TemporaryPlatformWorldState current = worldState(server);
        TemporaryPlatformWorldState next = current;
        for (Map.Entry<String, TemporaryPlatformWorldState.PlatformRecord> entry : current.platforms().entrySet()) {
            TemporaryPlatformWorldState.PlatformRecord record = entry.getValue();
            if (record.ownerId().equals(ownerId.toString())) {
                long expiry = Math.min(record.maxExpiresAtTick(), record.expiresAtTick() + ticks);
                next = next.withPlatform(entry.getKey(), record.withExpiry(expiry));
            }
        }
        if (!next.equals(current)) setWorldState(server, next);
    }

    private static void tickRuntime(MinecraftServer server) {
        tickReplays(server);
        tickCleanup(server);
    }

    private static void tickReplays(MinecraftServer server) {
        Iterator<Map.Entry<UUID, List<ReplayAction>>> ownerIterator = REPLAYS.entrySet().iterator();
        while (ownerIterator.hasNext()) {
            Map.Entry<UUID, List<ReplayAction>> entry = ownerIterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.isDeadOrDying()) {
                ownerIterator.remove();
                continue;
            }
            long now = player.level().getGameTime();
            entry.getValue().removeIf(replay -> {
                if (replay.dueTick > now) return false;
                execute(player, replay.efficiency);
                return true;
            });
            if (entry.getValue().isEmpty()) {
                ownerIterator.remove();
            }
        }
    }

    private static void tickCleanup(MinecraftServer server) {
        TemporaryPlatformWorldState current = worldState(server);
        TemporaryPlatformWorldState next = current;
        for (Map.Entry<String, TemporaryPlatformWorldState.PlatformRecord> entry : current.platforms().entrySet()) {
            TemporaryPlatformWorldState.PlatformRecord record = entry.getValue();
            ResourceKey<Level> dimension;
            try {
                dimension = ResourceKey.create(Registries.DIMENSION, Identifier.parse(record.dimension()));
            } catch (RuntimeException invalid) {
                next = next.withoutPlatform(entry.getKey());
                continue;
            }
            ServerLevel level = server.getLevel(dimension);
            if (level == null) {
                // A dimension may be unloaded while its chunk still contains our block. Keep
                // the record so cleanup can resume if that dimension is loaded again.
                continue;
            }
            if (level.getGameTime() < record.expiresAtTick()) continue;
            BlockPos pos = BlockPos.of(record.packedPosition());
            if (level.getBlockState(pos).is(PLATFORM_BLOCK)) {
                level.removeBlock(pos, false);
            }
            next = next.withoutPlatform(entry.getKey());
        }
        if (!next.equals(current)) setWorldState(server, next);
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String chargeKey(int index) {
        return TEMPORARY_PLATFORM_ID + "#charge" + index;
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private static TemporaryPlatformWorldState worldState(MinecraftServer server) {
        return server.overworld().getAttachedOrSet(
                TemporaryPlatformWorldAttachments.STATE,
                TemporaryPlatformWorldState.initial()
        );
    }

    private static void setWorldState(MinecraftServer server, TemporaryPlatformWorldState state) {
        server.overworld().setAttached(TemporaryPlatformWorldAttachments.STATE, state);
    }

    private record ReplayAction(long dueTick, double efficiency) {
    }

    private record ChargeReservation(String key, long readyTick) {
    }
}
