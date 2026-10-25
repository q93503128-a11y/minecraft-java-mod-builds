package dev.moonseungjun.augmentationsurvival.augment.utility;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.companion.ReviveAugmentService;
import dev.moonseungjun.augmentationsurvival.network.PerceptionStatePayload;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/** Shared server scan boundary for perception augments and revive markers. */
public final class PerceptionAugmentService {
    private static final String ORE_SENSE_ID = id("ore_sense");
    private static final String CHEST_FINDER_ID = id("chest_finder");
    private static final String DANGER_SENSE_ID = id("danger_sense");
    private static final String ENCHANT_REFRESH_ID = id("enchant_refresh");
    private static final Map<UUID, PerceptionStatePayload> LAST_SENT = new HashMap<>();
    private static int ticks;

    private PerceptionAugmentService() {
    }

    public static void initialize() {
        for (String augmentId : new String[]{ORE_SENSE_ID, CHEST_FINDER_ID, DANGER_SENSE_ID, ENCHANT_REFRESH_ID}) {
            AugmentCatalog.require(augmentId);
        }
        PayloadTypeRegistry.clientboundPlay().register(PerceptionStatePayload.TYPE, PerceptionStatePayload.CODEC);
        ServerPlayerEvents.JOIN.register(player -> {
            LAST_SENT.remove(player.getUUID());
            sync(player, true);
        });
        ServerPlayerEvents.LEAVE.register(player -> LAST_SENT.remove(player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> LAST_SENT.clear());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks % 10 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) sync(player, false);
        });
    }

    public static void syncNow(ServerPlayer player) {
        LAST_SENT.remove(player.getUUID());
        sync(player, true);
    }

    private static void sync(ServerPlayer player, boolean force) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int oreLevel = boundedLevel(state, ORE_SENSE_ID);
        int chestLevel = boundedLevel(state, CHEST_FINDER_ID);
        int dangerLevel = boundedLevel(state, DANGER_SENSE_ID);
        boolean enchantRefresh = boundedLevel(state, ENCHANT_REFRESH_ID) > 0;
        PerceptionStatePayload payload = new PerceptionStatePayload(
                oreLevel,
                enchantRefresh,
                chestLevel > 0 ? findNaturalLootContainers(player, 6 + chestLevel * 6) : List.of(),
                dangerLevel > 0 ? findHostiles(player, 4 + dangerLevel * 4) : List.of(),
                ReviveAugmentService.markerPositionsFor(player)
        );
        PerceptionStatePayload previous = LAST_SENT.get(player.getUUID());
        if (!force && payload.equals(previous)) return;
        LAST_SENT.put(player.getUUID(), payload);
        ServerPlayNetworking.send(player, payload);
    }

    private static List<Long> findNaturalLootContainers(ServerPlayer player, int radius) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos center = player.blockPosition();
        int minChunkX = Math.floorDiv(center.getX() - radius, 16);
        int maxChunkX = Math.floorDiv(center.getX() + radius, 16);
        int minChunkZ = Math.floorDiv(center.getZ() - radius, 16);
        int maxChunkZ = Math.floorDiv(center.getZ() + radius, 16);
        double radiusSquared = (double) radius * radius;
        List<Long> result = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX && result.size() < 512; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ && result.size() < 512; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) continue;
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof RandomizableContainerBlockEntity container)
                            || container.getLootTable() == null) continue;
                    BlockPos pos = blockEntity.getBlockPos();
                    if (center.distSqr(pos) <= radiusSquared) result.add(pos.asLong());
                    if (result.size() >= 512) break;
                }
            }
        }
        return result;
    }

    private static List<Integer> findHostiles(ServerPlayer player, int radius) {
        double radiusSquared = (double) radius * radius;
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                candidate -> CompanionAugmentService.isHostileTarget(player, candidate)
                        && player.distanceToSqr(candidate) <= radiusSquared
        );
        List<Integer> result = new ArrayList<>(Math.min(256, candidates.size()));
        for (LivingEntity candidate : candidates) {
            result.add(candidate.getId());
            if (result.size() >= 256) break;
        }
        return result;
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String id(String path) { return AugmentationSurvival.MOD_ID + ":" + path; }
}
