package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Slow countryside recovery for Erden. Managed urban and agricultural zones are never decorated. */
public final class RegionalEcologyManager {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;
    private static final long UPDATE_INTERVAL = 400L;
    private static final int CAPITAL_MANAGED_RADIUS = 3_200;

    private RegionalEcologyManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel realm = event.getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null || realm.getGameTime() % UPDATE_INTERVAL != 0L) return;
        for (ServerPlayer player : realm.players()) restoreCountrysideNear(realm, player);
    }

    private static void restoreCountrysideNear(ServerLevel level, ServerPlayer player) {
        long cycle = level.getGameTime() / UPDATE_INTERVAL;
        long seed = player.getUUID().getMostSignificantBits() ^ player.getUUID().getLeastSignificantBits()
                ^ cycle * 0x9E3779B97F4A7C15L;
        for (int attempt = 0; attempt < 6; attempt++) {
            seed = mix(seed + attempt * 0x632BE59BD9B4E019L);
            int dx = (int) Math.floorMod(seed, 129L) - 64;
            seed = mix(seed);
            int dz = (int) Math.floorMod(seed, 129L) - 64;
            int x = player.getBlockX() + dx;
            int z = player.getBlockZ() + dz;
            if (insideManagedCapital(x, z)) continue;

            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos ground = new BlockPos(x, y - 1, z);
            BlockPos plant = ground.above();
            if (!level.getBlockState(plant).isAir() || !level.getFluidState(plant).isEmpty()) continue;
            if (!isNaturalGround(level.getBlockState(ground).getBlock())) continue;

            BlockState flora = countrysideFlora(seed, season(level));
            if (flora.isAir() || !flora.canSurvive(level, plant)) continue;
            level.setBlock(plant, flora, FLAGS);
            if ((seed & 127L) == 0L) growSmallNativeTree(level, plant, seed);
        }
    }

    private static void growSmallNativeTree(ServerLevel level, BlockPos base, long seed) {
        int height = 4 + (int) (seed & 3L);
        for (int dy = 0; dy < height + 2; dy++) {
            if (!level.getBlockState(base.above(dy)).isAir()) return;
        }
        for (int dy = 0; dy < height; dy++) {
            level.setBlock(base.above(dy), Blocks.OAK_LOG.defaultBlockState(), FLAGS);
        }
        BlockPos crown = base.above(height - 1);
        for (int dy = -1; dy <= 2; dy++) {
            int radius = dy >= 2 ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && ((seed + dy) & 1L) == 0L) continue;
                    BlockPos pos = crown.offset(dx, dy, dz);
                    if (level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.OAK_LEAVES.defaultBlockState(), FLAGS);
                    }
                }
            }
        }
    }

    private static boolean insideManagedCapital(int x, int z) {
        long dx = x;
        long dz = z;
        return dx * dx + dz * dz <= (long) CAPITAL_MANAGED_RADIUS * CAPITAL_MANAGED_RADIUS;
    }

    private static boolean isNaturalGround(Block ground) {
        return ground == Blocks.GRASS_BLOCK || ground == Blocks.DIRT || ground == Blocks.PODZOL
                || ground == Blocks.MOSS_BLOCK || ground == Blocks.MUD;
    }

    private static BlockState countrysideFlora(long seed, int season) {
        int pick = (int) Math.floorMod(seed, 16L);
        if (season == 3) {
            return pick < 4 ? Blocks.FERN.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
        return switch (pick) {
            case 0 -> Blocks.CORNFLOWER.defaultBlockState();
            case 1 -> Blocks.OXEYE_DAISY.defaultBlockState();
            case 2, 3 -> Blocks.FERN.defaultBlockState();
            case 4, 5, 6 -> Blocks.SHORT_GRASS.defaultBlockState();
            default -> Blocks.AIR.defaultBlockState();
        };
    }

    private static int season(ServerLevel level) {
        return (int) ((level.getGameTime() / 24_000L / 24L) & 3L);
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }
}
