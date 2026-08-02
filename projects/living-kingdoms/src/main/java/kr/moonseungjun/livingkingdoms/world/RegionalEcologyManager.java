package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Region-owned flora layer. World edits suppress drops and are season-aware. */
public final class RegionalEcologyManager {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;
    private static final long UPDATE_INTERVAL = 200L;

    private RegionalEcologyManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel realm = event.getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null || realm.getGameTime() % UPDATE_INTERVAL != 0L) return;
        for (ServerPlayer player : realm.players()) decorateNear(realm, player);
    }

    private static void decorateNear(ServerLevel level, ServerPlayer player) {
        long cycle = level.getGameTime() / UPDATE_INTERVAL;
        long seed = player.getUUID().getMostSignificantBits() ^ player.getUUID().getLeastSignificantBits()
                ^ cycle * 0x9E3779B97F4A7C15L;
        for (int attempt = 0; attempt < 12; attempt++) {
            seed = mix(seed + attempt * 0x632BE59BD9B4E019L);
            int dx = (int) Math.floorMod(seed, 97L) - 48;
            seed = mix(seed);
            int dz = (int) Math.floorMod(seed, 97L) - 48;
            int x = player.getBlockX() + dx;
            int z = player.getBlockZ() + dz;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos ground = new BlockPos(x, y - 1, z);
            BlockPos plant = ground.above();
            if (!level.getBlockState(plant).isAir() || !level.getFluidState(plant).isEmpty()) continue;
            Ecology ecology = ecologyAt(x, z);
            BlockState flora = ecology.flora(seed, season(level));
            if (flora.isAir() || !validGround(level.getBlockState(ground).getBlock(), ecology)) continue;
            level.setBlock(plant, flora, FLAGS);
            if ((seed & 31L) == 0L) growAuthoredTree(level, plant, ecology, seed);
        }
    }

    private static void growAuthoredTree(ServerLevel level, BlockPos base, Ecology ecology, long seed) {
        if (ecology == Ecology.SAHAR || ecology == Ecology.KARDUM || ecology == Ecology.DRAGONLANDS) return;
        int height = 4 + (int) (seed & 3L);
        Block log = ecology == Ecology.SILVANA ? Blocks.DARK_OAK_LOG
                : ecology == Ecology.ARCHIPELAGO ? Blocks.JUNGLE_LOG : Blocks.OAK_LOG;
        Block leaves = ecology == Ecology.SILVANA ? Blocks.FLOWERING_AZALEA_LEAVES
                : ecology == Ecology.ARCHIPELAGO ? Blocks.JUNGLE_LEAVES : Blocks.OAK_LEAVES;
        for (int dy = 0; dy < height; dy++) {
            if (!level.getBlockState(base.above(dy)).isAir()) return;
        }
        for (int dy = 0; dy < height; dy++) level.setBlock(base.above(dy), log.defaultBlockState(), FLAGS);
        BlockPos crown = base.above(height - 1);
        for (int dy = -1; dy <= 2; dy++) {
            int radius = dy >= 2 ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && ((seed + dy) & 1L) == 0L) continue;
                    BlockPos pos = crown.offset(dx, dy, dz);
                    if (level.getBlockState(pos).isAir()) level.setBlock(pos, leaves.defaultBlockState(), FLAGS);
                }
            }
        }
    }

    private static boolean validGround(Block ground, Ecology ecology) {
        if (ecology == Ecology.SAHAR) return ground == Blocks.SAND || ground == Blocks.RED_SAND;
        if (ecology == Ecology.KARDUM || ecology == Ecology.DRAGONLANDS) {
            return ground == Blocks.STONE || ground == Blocks.GRAVEL || ground == Blocks.CALCITE;
        }
        return ground == Blocks.GRASS_BLOCK || ground == Blocks.DIRT || ground == Blocks.PODZOL
                || ground == Blocks.MOSS_BLOCK || ground == Blocks.MUD;
    }

    private static Ecology ecologyAt(int x, int z) {
        if (inside(x, z, -2_400, -1_200, 1_500)) return Ecology.SILVANA;
        if (inside(x, z, 2_200, -1_500, 1_600)) return Ecology.KARDUM;
        if (inside(x, z, 3_200, 2_600, 1_650)) return Ecology.SAHAR;
        if (inside(x, z, 0, -4_200, 1_900)) return Ecology.DRAGONLANDS;
        if (x < -3_000 && z > 150) return Ecology.ARCHIPELAGO;
        if (inside(x, z, 3_400, 300, 1_500)) return Ecology.STEPPE;
        return Ecology.CENTRAL;
    }

    private static boolean inside(int x, int z, int cx, int cz, int radius) {
        long dx = x - (long) cx;
        long dz = z - (long) cz;
        return dx * dx + dz * dz <= (long) radius * radius;
    }

    private static int season(ServerLevel level) {
        return (int) ((level.getDayTime() / 24_000L / 24L) & 3L);
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }

    private enum Ecology {
        CENTRAL, SILVANA, KARDUM, SAHAR, STEPPE, DRAGONLANDS, ARCHIPELAGO;

        BlockState flora(long seed, int season) {
            int pick = (int) Math.floorMod(seed, 8L);
            return switch (this) {
                case SILVANA -> switch (pick) {
                    case 0, 1 -> Blocks.FERN.defaultBlockState();
                    case 2 -> Blocks.BROWN_MUSHROOM.defaultBlockState();
                    case 3 -> Blocks.RED_MUSHROOM.defaultBlockState();
                    case 4 -> Blocks.AZALEA.defaultBlockState();
                    default -> Blocks.MOSS_CARPET.defaultBlockState();
                };
                case KARDUM -> pick < 3 ? Blocks.SHORT_GRASS.defaultBlockState()
                        : pick == 3 ? Blocks.DEAD_BUSH.defaultBlockState() : Blocks.AIR.defaultBlockState();
                case SAHAR -> pick < 2 ? Blocks.DEAD_BUSH.defaultBlockState()
                        : pick == 2 ? Blocks.CACTUS.defaultBlockState() : Blocks.AIR.defaultBlockState();
                case STEPPE -> pick < 3 ? Blocks.SHORT_GRASS.defaultBlockState()
                        : pick == 3 ? Blocks.ALLIUM.defaultBlockState() : Blocks.AIR.defaultBlockState();
                case DRAGONLANDS -> pick == 0 ? Blocks.WITHER_ROSE.defaultBlockState() : Blocks.AIR.defaultBlockState();
                case ARCHIPELAGO -> pick < 3 ? Blocks.FERN.defaultBlockState()
                        : pick == 3 ? Blocks.BLUE_ORCHID.defaultBlockState() : Blocks.SHORT_GRASS.defaultBlockState();
                case CENTRAL -> {
                    if (season == 3 && pick > 2) yield Blocks.AIR.defaultBlockState();
                    yield switch (pick) {
                        case 0 -> Blocks.CORNFLOWER.defaultBlockState();
                        case 1 -> Blocks.OXEYE_DAISY.defaultBlockState();
                        case 2 -> Blocks.FERN.defaultBlockState();
                        default -> Blocks.SHORT_GRASS.defaultBlockState();
                    };
                }
            };
        }
    }
}
