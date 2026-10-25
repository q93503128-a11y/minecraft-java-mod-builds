package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.augment.FinalAugmentTuning;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Server-owned 3x3 rescue platform with timed cleanup and tracked fall-damage immunity. */
public final class TemporaryPlatformService {
    private static final BlockState PLATFORM_STATE = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
    private static final Map<PlatformKey, Long> PLATFORMS = new HashMap<>();

    private TemporaryPlatformService() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(TemporaryPlatformService::tickCleanup);
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
        long expiresAt = level.getGameTime()
                + Math.max(1, (int) Math.round(tuning.temporaryPlatformDurationTicks() * efficiency));
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                if (!level.getBlockState(pos).isAir()) {
                    continue;
                }
                level.setBlock(pos, PLATFORM_STATE, Block.UPDATE_ALL);
                PLATFORMS.put(new PlatformKey(level.dimension(), pos.immutable()), expiresAt);
                placed++;
            }
        }
        if (placed <= 0) {
            return false;
        }
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
                PlatformKey key = new PlatformKey(level.dimension(), new BlockPos(x, y, z));
                Long expiry = PLATFORMS.get(key);
                if (expiry != null && expiry >= now && level.getBlockState(key.pos()).is(Blocks.LIGHT_BLUE_STAINED_GLASS)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void tickCleanup(MinecraftServer server) {
        Iterator<Map.Entry<PlatformKey, Long>> iterator = PLATFORMS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<PlatformKey, Long> entry = iterator.next();
            PlatformKey key = entry.getKey();
            ServerLevel level = server.getLevel(key.dimension());
            if (level == null || level.getGameTime() < entry.getValue()) {
                continue;
            }
            if (level.getBlockState(key.pos()).is(Blocks.LIGHT_BLUE_STAINED_GLASS)) {
                level.removeBlock(key.pos(), false);
            }
            iterator.remove();
        }
    }

    private record PlatformKey(ResourceKey<Level> dimension, BlockPos pos) {
    }
}
