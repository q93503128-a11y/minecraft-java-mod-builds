package dev.moonseungjun.fishinggame.world;

import java.util.Set;

import dev.moonseungjun.fishinggame.FishingGameMod;
import dev.moonseungjun.fishinggame.fishing.FishingLocation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class FishingWorldManager {
    public static final ResourceKey<Level> LAKESIDE_LEVEL = ResourceKey.create(
            Registries.DIMENSION,
            FishingGameMod.id("lakeside")
    );

    private static final BlockPos BUILD_MARKER = new BlockPos(0, 58, 27);
    private static final int WATER_Y = 63;
    private static final int LAND_Y = 64;
    private static final int NOON_TICKS = 6000;
    private static final int OUTER_X = 44;
    private static final int OUTER_Z = 38;
    private static final int INNER_X = 21;
    private static final int INNER_Z = 17;
    private static int maintenanceTicks;

    private FishingWorldManager() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            maintenanceTicks++;
            if (maintenanceTicks < 100) return;
            maintenanceTicks = 0;

            ServerLevel lakeside = server.getLevel(LAKESIDE_LEVEL);
            if (lakeside == null) return;
            lockLakesideClock(lakeside);

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!player.level().dimension().equals(LAKESIDE_LEVEL)) continue;
                if (outsidePlayableArea(player) || player.getY() < 54.0 || player.getY() > 96.0) {
                    teleportToArrival(player, lakeside);
                }
            }
        });
    }

    public static boolean prepareAndPlacePlayer(ServerPlayer player, MinecraftServer server) {
        ServerLevel lakeside = server.getLevel(LAKESIDE_LEVEL);
        if (lakeside == null) {
            FishingGameMod.LOGGER.error("Dedicated lakeside dimension is unavailable; player remains in current level");
            return false;
        }

        buildIfNeeded(lakeside);
        lockLakesideClock(lakeside);
        return teleportToArrival(player, lakeside);
    }

    public static FishingLocation locationFor(ServerPlayer player) {
        if (player.level().dimension().equals(LAKESIDE_LEVEL)) {
            return FishingLocation.LAKESIDE;
        }
        return FishingLocation.LAKESIDE;
    }

    private static void lockLakesideClock(ServerLevel lakeside) {
        Holder<WorldClock> clock = lakeside.dimensionType().defaultClock().orElseThrow(() ->
                new IllegalStateException("Lakeside dimension type is missing fishinggame:lakeside default_clock")
        );
        lakeside.clockManager().setTotalTicks(clock, NOON_TICKS);
        lakeside.clockManager().setPaused(clock, true);
    }

    private static boolean teleportToArrival(ServerPlayer player, ServerLevel lakeside) {
        return player.teleportTo(
                lakeside,
                0.5, 65.0, 28.5,
                Set.of(),
                180.0f, 0.0f,
                true
        );
    }

    private static boolean outsidePlayableArea(ServerPlayer player) {
        double nx = player.getX() / 49.0;
        double nz = player.getZ() / 43.0;
        return nx * nx + nz * nz > 1.0;
    }

    private static void buildIfNeeded(ServerLevel level) {
        if (level.getBlockState(BUILD_MARKER).is(Blocks.LODESTONE)) return;

        FishingGameMod.LOGGER.info("Authoring Cheongram Lakeside fishing location");
        buildIslandBase(level);
        buildMainDock(level);
        buildSidePiers(level);
        buildLakesidePavilion(level);
        buildTackleShelter(level);
        buildPaths(level);
        buildTrees(level);
        buildRocksAndPlants(level);
        set(level, BUILD_MARKER, Blocks.LODESTONE);
    }

    private static void buildIslandBase(ServerLevel level) {
        for (int x = -48; x <= 48; x++) {
            for (int z = -42; z <= 42; z++) {
                double outer = ellipse(x, z, OUTER_X, OUTER_Z);
                double inner = ellipse(x, z, INNER_X, INNER_Z);
                if (outer > 1.0 || inner < 1.0) continue;

                int surface = surfaceYAt(x, z);
                for (int y = 56; y <= surface - 2; y++) {
                    set(level, new BlockPos(x, y, z), Blocks.STONE);
                }
                set(level, new BlockPos(x, surface - 1, z), Blocks.DIRT);

                Block top;
                if (inner < 1.24) {
                    top = ((x * 17 + z * 31) & 7) == 0 ? Blocks.GRAVEL : Blocks.SAND;
                } else if (outer > 0.93) {
                    top = ((x + z) & 3) == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.STONE;
                } else {
                    top = Blocks.GRASS_BLOCK;
                }
                set(level, new BlockPos(x, surface, z), top);
            }
        }

        for (int x = -INNER_X; x <= INNER_X; x++) {
            for (int z = -INNER_Z; z <= INNER_Z; z++) {
                double inner = ellipse(x, z, INNER_X, INNER_Z);
                if (inner > 1.0 || inner < 0.72) continue;
                set(level, new BlockPos(x, WATER_Y - 4, z), Blocks.SAND);
                if (((x * 13 + z * 7) & 15) == 0) {
                    set(level, new BlockPos(x, WATER_Y - 3, z), Blocks.GRAVEL);
                }
            }
        }
    }

    private static int surfaceYAt(int x, int z) {
        double outer = ellipse(x, z, OUTER_X, OUTER_Z);
        double inner = ellipse(x, z, INNER_X, INNER_Z);
        if (inner < 1.24) return LAND_Y;

        double ripple = Math.sin(x * 0.23) * 0.55 + Math.cos(z * 0.19) * 0.45;
        int rise = ripple > 0.75 ? 1 : 0;
        if (outer > 0.76) rise++;
        if (outer > 0.91) rise++;
        return LAND_Y + rise;
    }

    private static void buildMainDock(ServerLevel level) {
        for (int z = 7; z <= 27; z++) {
            for (int x = -2; x <= 2; x++) {
                set(level, new BlockPos(x, LAND_Y, z), Blocks.SPRUCE_PLANKS);
            }
            if (z <= 20 && z % 4 == 0) {
                for (int y = WATER_Y - 4; y < LAND_Y; y++) {
                    set(level, new BlockPos(-2, y, z), Blocks.SPRUCE_LOG);
                    set(level, new BlockPos(2, y, z), Blocks.SPRUCE_LOG);
                }
            }
            if (z >= 8 && z <= 19 && z % 3 == 1) {
                set(level, new BlockPos(-2, LAND_Y + 1, z), Blocks.SPRUCE_FENCE);
                set(level, new BlockPos(2, LAND_Y + 1, z), Blocks.SPRUCE_FENCE);
            }
        }

        for (int x = -5; x <= 5; x++) {
            for (int z = 5; z <= 8; z++) {
                set(level, new BlockPos(x, LAND_Y, z), Blocks.SPRUCE_PLANKS);
            }
        }
        set(level, new BlockPos(-5, LAND_Y + 1, 6), Blocks.SPRUCE_FENCE);
        set(level, new BlockPos(5, LAND_Y + 1, 6), Blocks.SPRUCE_FENCE);
        set(level, new BlockPos(-5, LAND_Y + 2, 6), Blocks.LANTERN);
        set(level, new BlockPos(5, LAND_Y + 2, 6), Blocks.LANTERN);
    }

    private static void buildSidePiers(ServerLevel level) {
        for (int x = -27; x <= -10; x++) {
            for (int z = -1; z <= 1; z++) {
                set(level, new BlockPos(x, LAND_Y, z), Blocks.OAK_PLANKS);
            }
            if (x % 5 == 0) {
                for (int y = WATER_Y - 3; y < LAND_Y; y++) {
                    set(level, new BlockPos(x, y, -1), Blocks.OAK_LOG);
                    set(level, new BlockPos(x, y, 1), Blocks.OAK_LOG);
                }
            }
        }

        for (int x = 10; x <= 27; x++) {
            for (int z = -1; z <= 1; z++) {
                set(level, new BlockPos(x, LAND_Y, z), Blocks.OAK_PLANKS);
            }
            if (x % 5 == 0) {
                for (int y = WATER_Y - 3; y < LAND_Y; y++) {
                    set(level, new BlockPos(x, y, -1), Blocks.OAK_LOG);
                    set(level, new BlockPos(x, y, 1), Blocks.OAK_LOG);
                }
            }
        }
    }

    private static void buildLakesidePavilion(ServerLevel level) {
        flattenPad(level, -17, -7, 24, 34, LAND_Y);
        for (int x = -16; x <= -8; x++) {
            for (int z = 25; z <= 33; z++) {
                set(level, new BlockPos(x, LAND_Y + 1, z), Blocks.SPRUCE_PLANKS);
            }
        }

        int[][] posts = {{-16, 25}, {-8, 25}, {-16, 33}, {-8, 33}};
        for (int[] post : posts) {
            for (int y = LAND_Y + 2; y <= LAND_Y + 6; y++) {
                set(level, new BlockPos(post[0], y, post[1]), Blocks.SPRUCE_LOG);
            }
        }

        for (int x = -17; x <= -7; x++) {
            for (int z = 24; z <= 34; z++) {
                set(level, new BlockPos(x, LAND_Y + 7, z), Blocks.SPRUCE_SLAB);
            }
        }
        for (int z = 27; z <= 31; z += 4) {
            set(level, new BlockPos(-12, LAND_Y + 2, z), Blocks.OAK_SLAB);
        }
    }

    private static void buildTackleShelter(ServerLevel level) {
        flattenPad(level, 7, 17, 24, 34, LAND_Y);
        for (int x = 8; x <= 16; x++) {
            for (int z = 25; z <= 33; z++) {
                set(level, new BlockPos(x, LAND_Y + 1, z), Blocks.OAK_PLANKS);
            }
        }

        for (int y = LAND_Y + 2; y <= LAND_Y + 6; y++) {
            set(level, new BlockPos(8, y, 25), Blocks.OAK_LOG);
            set(level, new BlockPos(16, y, 25), Blocks.OAK_LOG);
            set(level, new BlockPos(8, y, 33), Blocks.OAK_LOG);
            set(level, new BlockPos(16, y, 33), Blocks.OAK_LOG);
        }
        for (int x = 7; x <= 17; x++) {
            for (int z = 24; z <= 34; z++) {
                set(level, new BlockPos(x, LAND_Y + 7, z), Blocks.OAK_SLAB);
            }
        }

        for (int x = 10; x <= 14; x++) {
            set(level, new BlockPos(x, LAND_Y + 2, 31), Blocks.SPRUCE_PLANKS);
            set(level, new BlockPos(x, LAND_Y + 3, 31), Blocks.SPRUCE_SLAB);
        }
        set(level, new BlockPos(12, LAND_Y + 2, 27), Blocks.OAK_SLAB);
    }

    private static void buildPaths(ServerLevel level) {
        for (int x = -20; x <= 20; x++) {
            for (int z = 22; z <= 24; z++) {
                int y = surfaceYAt(x, z);
                set(level, new BlockPos(x, y, z), ((x + z) & 3) == 0 ? Blocks.COARSE_DIRT : Blocks.GRAVEL);
            }
        }
        for (int z = 18; z <= 30; z++) {
            for (int x = -1; x <= 1; x++) {
                int y = surfaceYAt(x, z);
                set(level, new BlockPos(x, y, z), Blocks.GRAVEL);
            }
        }
    }

    private static void buildTrees(ServerLevel level) {
        int[][] trees = {
                {-30, 5}, {30, 7}, {-27, -20}, {27, -19},
                {-36, 19}, {35, 20}, {-9, -31}, {11, -31},
                {-22, 29}, {22, 28}
        };
        for (int i = 0; i < trees.length; i++) {
            int x = trees[i][0];
            int z = trees[i][1];
            buildTree(level, x, surfaceYAt(x, z), z, 4 + (i % 3));
        }
    }

    private static void buildTree(ServerLevel level, int x, int groundY, int z, int trunkHeight) {
        for (int y = 1; y <= trunkHeight; y++) {
            set(level, new BlockPos(x, groundY + y, z), Blocks.SPRUCE_LOG);
        }
        int topY = groundY + trunkHeight;
        for (int dy = -2; dy <= 2; dy++) {
            int radius = dy == 2 ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > radius + 1) continue;
                    if (dx == 0 && dz == 0 && dy <= 0) continue;
                    set(level, new BlockPos(x + dx, topY + dy, z + dz), Blocks.SPRUCE_LEAVES);
                }
            }
        }
    }

    private static void buildRocksAndPlants(ServerLevel level) {
        int[][] rocks = {
                {-25, 15}, {25, 14}, {-31, -9}, {32, -8}, {-18, -27}, {19, -27}
        };
        for (int i = 0; i < rocks.length; i++) {
            int x = rocks[i][0];
            int z = rocks[i][1];
            int y = surfaceYAt(x, z) + 1;
            Block rock = i % 2 == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE;
            set(level, new BlockPos(x, y, z), rock);
            set(level, new BlockPos(x + 1, y, z), rock);
            if ((i & 1) == 0) set(level, new BlockPos(x, y + 1, z), rock);
        }

        int[][] plants = {
                {-20, 18}, {-24, 12}, {21, 18}, {27, 12},
                {-18, -21}, {18, -22}, {-31, 24}, {30, 25}
        };
        for (int i = 0; i < plants.length; i++) {
            int x = plants[i][0];
            int z = plants[i][1];
            int y = surfaceYAt(x, z) + 1;
            Block plant = i % 3 == 0 ? Blocks.DANDELION : (i % 3 == 1 ? Blocks.FERN : Blocks.POPPY);
            set(level, new BlockPos(x, y, z), plant);
        }
    }

    private static void flattenPad(ServerLevel level, int minX, int maxX, int minZ, int maxZ, int y) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                set(level, new BlockPos(x, y - 1, z), Blocks.DIRT);
                set(level, new BlockPos(x, y, z), Blocks.GRASS_BLOCK);
                for (int clearY = y + 1; clearY <= y + 8; clearY++) {
                    set(level, new BlockPos(x, clearY, z), Blocks.AIR);
                }
            }
        }
    }

    private static double ellipse(int x, int z, double radiusX, double radiusZ) {
        double nx = x / radiusX;
        double nz = z / radiusZ;
        return nx * nx + nz * nz;
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), 2);
    }
}
