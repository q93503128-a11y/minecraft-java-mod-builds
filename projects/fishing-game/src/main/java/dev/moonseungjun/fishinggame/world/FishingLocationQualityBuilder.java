package dev.moonseungjun.fishinggame.world;

import dev.moonseungjun.fishinggame.FishingGameMod;
import dev.moonseungjun.fishinggame.fishing.FishingLocation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class FishingLocationQualityBuilder {
    private static final BlockPos BASE_BUILD_MARKER = new BlockPos(52, 40, 52);
    private static final BlockPos QUALITY_MARKER_ALPHA12 = new BlockPos(53, 40, 52);
    private static final int DECK_Y = 64;
    private static int upgradeTicks;

    private FishingLocationQualityBuilder() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            upgradeTicks++;
            if (upgradeTicks < 20) return;
            upgradeTicks = 0;

            upgradeIfReady(server.getLevel(FishingTravelManager.COAST_LEVEL), FishingLocation.COAST);
            upgradeIfReady(server.getLevel(FishingTravelManager.DEEP_SEA_LEVEL), FishingLocation.DEEP_SEA);
        });
    }

    private static void upgradeIfReady(ServerLevel level, FishingLocation location) {
        if (level == null) return;
        if (!level.getBlockState(BASE_BUILD_MARKER).is(Blocks.LODESTONE)) return;
        if (level.getBlockState(QUALITY_MARKER_ALPHA12).is(Blocks.LODESTONE)) return;

        FishingGameMod.LOGGER.info("Applying alpha.12 environment quality pass to {}", location);
        apply(level, location);
        set(level, QUALITY_MARKER_ALPHA12, Blocks.LODESTONE);
    }

    static void apply(ServerLevel level, FishingLocation location) {
        switch (location) {
            case COAST -> enhanceCoast(level);
            case DEEP_SEA -> enhanceDeepSea(level);
            case LAKESIDE -> {
                // Lakeside has its own authored world manager and is not upgraded here.
            }
        }
    }

    private static void enhanceCoast(ServerLevel level) {
        buildHarborPromenade(level);
        buildHarborBreakwaters(level);
        buildHarborFishingStations(level);
        buildHarborShoreRockwork(level);
        enhanceLighthouse(level);
    }

    private static void buildHarborPromenade(ServerLevel level) {
        for (int z = 19; z <= 41; z++) {
            for (int x = -3; x <= 3; x++) {
                Block block;
                if (Math.abs(x) == 3) {
                    block = Blocks.STONE_BRICKS;
                } else if (((x + z) & 7) == 0) {
                    block = Blocks.MOSSY_STONE_BRICKS;
                } else {
                    block = Blocks.GRAVEL;
                }
                set(level, new BlockPos(x, DECK_Y, z), block);
            }
        }

        int[][] bollards = {{-4, 21}, {4, 21}, {-4, 31}, {4, 31}, {-4, 40}, {4, 40}};
        for (int[] bollard : bollards) {
            set(level, new BlockPos(bollard[0], DECK_Y + 1, bollard[1]), Blocks.SPRUCE_LOG);
            set(level, new BlockPos(bollard[0], DECK_Y + 2, bollard[1]), Blocks.LANTERN);
        }
    }

    private static void buildHarborBreakwaters(ServerLevel level) {
        int[][] left = {{-37, 10, 3}, {-40, 5, 3}, {-43, 0, 3}, {-45, -6, 2}};
        int[][] right = {{37, 10, 3}, {40, 5, 3}, {43, 0, 3}, {45, -6, 2}};
        for (int[] cluster : left) rockCluster(level, cluster[0], cluster[1], cluster[2], 11 + cluster[1]);
        for (int[] cluster : right) rockCluster(level, cluster[0], cluster[1], cluster[2], 29 + cluster[1]);

        int[][] beacons = {{-45, -8}, {45, -8}};
        for (int[] beacon : beacons) {
            pillar(level, beacon[0], beacon[1], 61, DECK_Y + 2, Blocks.STONE_BRICKS);
            set(level, new BlockPos(beacon[0], DECK_Y + 3, beacon[1]), Blocks.SEA_LANTERN);
        }
    }

    private static void buildHarborFishingStations(ServerLevel level) {
        int[] centers = {-22, 0, 22};
        for (int centerX : centers) {
            for (int z = -18; z <= -12; z++) {
                for (int x = centerX - 3; x <= centerX + 3; x++) {
                    Block floor = (Math.abs(x - centerX) == 3 || z == -12)
                            ? Blocks.DARK_OAK_PLANKS
                            : Blocks.SPRUCE_PLANKS;
                    set(level, new BlockPos(x, DECK_Y, z), floor);
                }
            }

            for (int z = -17; z <= -12; z++) {
                set(level, new BlockPos(centerX - 3, DECK_Y + 1, z), Blocks.SPRUCE_FENCE);
                set(level, new BlockPos(centerX + 3, DECK_Y + 1, z), Blocks.SPRUCE_FENCE);
            }
            for (int x = centerX - 3; x <= centerX + 3; x++) {
                if (x == centerX) continue;
                set(level, new BlockPos(x, DECK_Y + 1, -12), Blocks.SPRUCE_FENCE);
            }

            set(level, new BlockPos(centerX - 2, DECK_Y + 1, -13), Blocks.BARREL);
            set(level, new BlockPos(centerX + 2, DECK_Y + 1, -13), Blocks.LANTERN);
            set(level, new BlockPos(centerX - 3, 60, -18), Blocks.SEA_LANTERN);
            set(level, new BlockPos(centerX + 3, 60, -18), Blocks.SEA_LANTERN);
        }
    }

    private static void buildHarborShoreRockwork(ServerLevel level) {
        int[][] clusters = {
                {-32, 23, 2}, {-25, 21, 2}, {-18, 19, 1},
                {18, 19, 1}, {25, 21, 2}, {32, 23, 2},
                {-40, 30, 2}, {40, 30, 2}
        };
        for (int i = 0; i < clusters.length; i++) {
            int[] cluster = clusters[i];
            rockCluster(level, cluster[0], cluster[1], cluster[2], 73 + i * 13);
        }
    }

    private static void enhanceLighthouse(ServerLevel level) {
        int cx = 30;
        int cz = 33;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) == 3 || Math.abs(dz) == 3) {
                    set(level, new BlockPos(cx + dx, DECK_Y + 11, cz + dz), Blocks.DARK_OAK_SLAB);
                    set(level, new BlockPos(cx + dx, DECK_Y + 12, cz + dz), Blocks.IRON_BARS);
                }
            }
        }

        set(level, new BlockPos(cx, DECK_Y + 12, cz), Blocks.SEA_LANTERN);
        set(level, new BlockPos(cx - 1, DECK_Y + 12, cz), Blocks.SEA_LANTERN);
        set(level, new BlockPos(cx + 1, DECK_Y + 12, cz), Blocks.SEA_LANTERN);
        set(level, new BlockPos(cx, DECK_Y + 12, cz - 1), Blocks.SEA_LANTERN);
        set(level, new BlockPos(cx, DECK_Y + 12, cz + 1), Blocks.SEA_LANTERN);
    }

    private static void enhanceDeepSea(ServerLevel level) {
        buildDeepSeaFishingPods(level);
        buildDeepSeaHazardGuides(level);
        buildDeepSeaSignalMast(level);
        buildDeepSeaSubsurfaceLights(level);
        reinforceDeepSeaDeck(level);
    }

    private static void buildDeepSeaFishingPods(ServerLevel level) {
        buildNorthPod(level);
        buildWestPod(level);
        buildEastPod(level);
    }

    private static void buildNorthPod(ServerLevel level) {
        for (int x = -5; x <= 5; x++) {
            for (int z = -24; z <= -17; z++) {
                set(level, new BlockPos(x, DECK_Y, z), podFloor(x, z));
            }
        }
        for (int z = -23; z <= -18; z++) {
            set(level, new BlockPos(-5, DECK_Y + 1, z), Blocks.IRON_BARS);
            set(level, new BlockPos(5, DECK_Y + 1, z), Blocks.IRON_BARS);
        }
        for (int x = -5; x <= 5; x++) {
            if (Math.abs(x) <= 2) continue;
            set(level, new BlockPos(x, DECK_Y + 1, -17), Blocks.IRON_BARS);
        }
        set(level, new BlockPos(-4, DECK_Y + 1, -18), Blocks.SEA_LANTERN);
        set(level, new BlockPos(4, DECK_Y + 1, -18), Blocks.SEA_LANTERN);
    }

    private static void buildWestPod(ServerLevel level) {
        for (int x = -38; x <= -31; x++) {
            for (int z = 12; z <= 22; z++) {
                set(level, new BlockPos(x, DECK_Y, z), podFloor(x, z));
            }
        }
        for (int x = -37; x <= -32; x++) {
            set(level, new BlockPos(x, DECK_Y + 1, 12), Blocks.IRON_BARS);
            set(level, new BlockPos(x, DECK_Y + 1, 22), Blocks.IRON_BARS);
        }
        for (int z = 12; z <= 22; z++) {
            if (z >= 15 && z <= 19) continue;
            set(level, new BlockPos(-31, DECK_Y + 1, z), Blocks.IRON_BARS);
        }
        set(level, new BlockPos(-32, DECK_Y + 1, 13), Blocks.SEA_LANTERN);
        set(level, new BlockPos(-32, DECK_Y + 1, 21), Blocks.SEA_LANTERN);
    }

    private static void buildEastPod(ServerLevel level) {
        for (int x = 31; x <= 38; x++) {
            for (int z = 12; z <= 22; z++) {
                set(level, new BlockPos(x, DECK_Y, z), podFloor(x, z));
            }
        }
        for (int x = 32; x <= 37; x++) {
            set(level, new BlockPos(x, DECK_Y + 1, 12), Blocks.IRON_BARS);
            set(level, new BlockPos(x, DECK_Y + 1, 22), Blocks.IRON_BARS);
        }
        for (int z = 12; z <= 22; z++) {
            if (z >= 15 && z <= 19) continue;
            set(level, new BlockPos(31, DECK_Y + 1, z), Blocks.IRON_BARS);
        }
        set(level, new BlockPos(32, DECK_Y + 1, 13), Blocks.SEA_LANTERN);
        set(level, new BlockPos(32, DECK_Y + 1, 21), Blocks.SEA_LANTERN);
    }

    private static Block podFloor(int x, int z) {
        return ((Math.abs(x) + Math.abs(z)) & 3) == 0
                ? Blocks.POLISHED_BLACKSTONE_BRICKS
                : Blocks.DARK_PRISMARINE;
    }

    private static void buildDeepSeaHazardGuides(ServerLevel level) {
        for (int x = -4; x <= 4; x++) {
            Block stripe = ((x + 4) & 1) == 0 ? Blocks.CONCRETE.yellow() : Blocks.CONCRETE.black();
            set(level, new BlockPos(x, DECK_Y, -16), stripe);
        }
        for (int z = 14; z <= 20; z++) {
            Block stripe = ((z - 14) & 1) == 0 ? Blocks.CONCRETE.yellow() : Blocks.CONCRETE.black();
            set(level, new BlockPos(-30, DECK_Y, z), stripe);
            set(level, new BlockPos(30, DECK_Y, z), stripe);
        }
    }

    private static void buildDeepSeaSignalMast(ServerLevel level) {
        pillar(level, 0, 22, DECK_Y + 10, DECK_Y + 17, Blocks.IRON_BLOCK);
        for (int x = -4; x <= 4; x++) {
            set(level, new BlockPos(x, DECK_Y + 16, 22), Blocks.IRON_BARS);
        }
        set(level, new BlockPos(-5, DECK_Y + 16, 22), Blocks.SEA_LANTERN);
        set(level, new BlockPos(5, DECK_Y + 16, 22), Blocks.SEA_LANTERN);
        set(level, new BlockPos(0, DECK_Y + 18, 22), Blocks.SEA_LANTERN);
    }

    private static void buildDeepSeaSubsurfaceLights(ServerLevel level) {
        int[][] lights = {
                {-5, -24}, {5, -24},
                {-38, 12}, {-38, 22},
                {38, 12}, {38, 22},
                {-12, 8}, {12, 8}
        };
        for (int[] light : lights) {
            pillar(level, light[0], light[1], 57, 60, Blocks.IRON_BLOCK);
            set(level, new BlockPos(light[0], 61, light[1]), Blocks.SEA_LANTERN);
        }
    }

    private static void reinforceDeepSeaDeck(ServerLevel level) {
        for (int x = -12; x <= 12; x++) {
            if (x >= -3 && x <= 3) continue;
            set(level, new BlockPos(x, DECK_Y + 1, 8), Blocks.IRON_BARS);
        }
        for (int z = 9; z <= 25; z++) {
            if (z >= 15 && z <= 19) continue;
            set(level, new BlockPos(-12, DECK_Y + 1, z), Blocks.IRON_BARS);
            set(level, new BlockPos(12, DECK_Y + 1, z), Blocks.IRON_BARS);
        }
    }

    private static void rockCluster(ServerLevel level, int centerX, int centerZ, int radius, int seed) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distance = dx * dx + dz * dz;
                if (distance > radius * radius + 1) continue;

                int surfaceY = DECK_Y + Math.max(0, radius - (Math.abs(dx) + Math.abs(dz)) / 2 - 1);
                int bottomY = Math.min(61, surfaceY - 2);
                for (int y = bottomY; y <= surfaceY; y++) {
                    set(level, new BlockPos(centerX + dx, y, centerZ + dz), rockBlock(dx, dz, y, seed));
                }
            }
        }
    }

    private static Block rockBlock(int dx, int dz, int y, int seed) {
        int variant = Math.floorMod(dx * 31 + dz * 17 + y * 7 + seed, 10);
        if (variant <= 1) return Blocks.MOSSY_COBBLESTONE;
        if (variant <= 4) return Blocks.ANDESITE;
        return Blocks.COBBLESTONE;
    }

    private static void pillar(ServerLevel level, int x, int z, int minY, int maxY, Block block) {
        for (int y = minY; y <= maxY; y++) set(level, new BlockPos(x, y, z), block);
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), 2);
    }
}
