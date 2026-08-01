package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Deterministic final pass for the Erden capital.
 *
 * <p>The original plan read pre-construction surface heights again while placing later lots. On
 * uneven sites those late grading operations erased walls of structures that had already been
 * planned, while their roofs survived. This pass clears only authored bounds, restores one flat
 * floor per lot and rebuilds the central city once.</p>
 */
public final class ErdenCapitalIntegrityFinalizer {
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;
    private static final Block MARKER = Blocks.LODESTONE;
    private static final int[][] HOMES = {
            {-56, -38}, {-34, -38}, {-11, -38}, {18, -38}, {46, -39},
            {-56, 31}, {-32, 32}, {-8, 32}, {20, 31}, {49, 34},
            {-111, 22}, {-109, 49}, {95, 21}, {98, 48},
            {-59, 78}, {-32, 78}, {-5, 79}, {27, 78}, {53, 78}
    };

    private ErdenCapitalIntegrityFinalizer() {
    }

    public static void ensure(ServerLevel level, String homelandId,
                              RealmSiteLayoutSavedData.RealmSite site) {
        if (!"erden_kingdom".equals(homelandId)) return;
        BlockPos marker = new BlockPos(site.centerX(), site.baseY() - 8, site.centerZ());
        if (level.getBlockState(marker).is(MARKER)) return;

        long started = System.nanoTime();
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = Math.max(68, Math.min(112, site.baseY()));

        repairRoads(level, cx, y, cz);
        stoneHall(level, cx - 34, y, cz - 91, 33, 24, 9);
        tower(level, cx - 38, y, cz - 95, 8, 15);
        tower(level, cx - 5, y, cz - 95, 8, 15);
        tower(level, cx - 38, y, cz - 71, 8, 15);
        tower(level, cx - 5, y, cz - 71, 8, 15);

        timberHall(level, cx + 18, y, cz - 82, 28, 18, 7);
        stoneHall(level, cx - 74, y, cz - 78, 22, 17, 7);
        timberHall(level, cx + 47, y, cz + 14, 21, 14, 6);
        timberHall(level, cx - 77, y, cz + 17, 23, 15, 7);
        timberHall(level, cx + 78, y, cz - 35, 16, 11, 6);
        timberHall(level, cx - 106, y, cz - 39, 29, 18, 7);
        timberHall(level, cx + 82, y, cz + 72, 19, 12, 6);
        rebuildMarket(level, cx, y, cz);
        rebuildStable(level, cx - 98, y, cz + 71);

        for (int i = 0; i < HOMES.length; i++) {
            house(level, cx + HOMES[i][0], y, cz + HOMES[i][1],
                    10 + i % 3, 8 + (i + 1) % 3, 5);
        }

        set(level, marker, MARKER);
        ConstructionDebrisCleaner.schedule(level, homelandId, site);
        LivingKingdoms.LOGGER.info(
                "Completed Erden capital integrity finalization at {},{} in {} ms",
                cx, cz, (System.nanoTime() - started) / 1_000_000L
        );
    }

    private static void repairRoads(ServerLevel level, int cx, int y, int cz) {
        road(level, cx, cz - 99, cx, cz + 99, y, 4, Blocks.PACKED_MUD, Blocks.STONE_BRICKS);
        road(level, cx - 119, cz, cx + 119, cz, y, 4, Blocks.PACKED_MUD, Blocks.STONE_BRICKS);
        road(level, cx - 92, cz - 54, cx + 92, cz - 54, y, 3, Blocks.GRAVEL, Blocks.COBBLESTONE);
        road(level, cx - 92, cz + 58, cx + 92, cz + 58, y, 3, Blocks.GRAVEL, Blocks.COBBLESTONE);
        flatten(level, cx - 27, cz - 23, cx + 27, cz + 23, y, Blocks.STONE_BRICKS);
    }

    private static void house(ServerLevel level, int x, int y, int z,
                              int width, int depth, int height) {
        clear(level, x - 3, y + 1, z - 3, x + width + 2, y + height + 8, z + depth + 2);
        flatten(level, x - 2, z - 2, x + width + 1, z + depth + 1, y, Blocks.STONE_BRICKS);
        fill(level, x, y, z, x + width - 1, y, z + depth - 1, Blocks.SPRUCE_PLANKS);
        shell(level, x, y, z, width, depth, height,
                Blocks.STRIPPED_SPRUCE_LOG, Blocks.BIRCH_PLANKS);
        windows(level, x, y, z, width, depth, height);
        doorway(level, x + width / 2, y, z);
        roof(level, x, y + height + 1, z, width, depth, Blocks.DARK_OAK_PLANKS);
        set(level, x + width / 2, y + height - 1, z + depth / 2, Blocks.SPRUCE_FENCE);
        set(level, x + width / 2, y + height - 2, z + depth / 2, Blocks.LANTERN);
    }

    private static void timberHall(ServerLevel level, int x, int y, int z,
                                   int width, int depth, int height) {
        house(level, x, y, z, width, depth, height);
        set(level, x + width / 2, y + 1, z + depth / 2, Blocks.LECTERN);
        set(level, x + 4, y + 1, z + depth - 4, Blocks.BOOKSHELF);
        set(level, x + width - 5, y + 1, z + depth - 4, Blocks.BOOKSHELF);
    }

    private static void stoneHall(ServerLevel level, int x, int y, int z,
                                  int width, int depth, int height) {
        clear(level, x - 3, y + 1, z - 3, x + width + 2, y + height + 7, z + depth + 2);
        flatten(level, x - 2, z - 2, x + width + 1, z + depth + 1, y, Blocks.STONE_BRICKS);
        fill(level, x, y, z, x + width - 1, y, z + depth - 1, Blocks.POLISHED_ANDESITE);
        shell(level, x, y, z, width, depth, height,
                Blocks.CHISELED_STONE_BRICKS, Blocks.STONE_BRICKS);
        windows(level, x, y, z, width, depth, height);
        doorway(level, x + width / 2, y, z);
        fill(level, x - 1, y + height + 1, z - 1,
                x + width, y + height + 1, z + depth, Blocks.DEEPSLATE_TILES);
        battlements(level, x - 1, y + height + 2, z - 1, width + 2, depth + 2);
    }

    private static void shell(ServerLevel level, int x, int y, int z,
                              int width, int depth, int height, Block frame, Block wall) {
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                if (dx != 0 && dx != width - 1 && dz != 0 && dz != depth - 1) continue;
                for (int dy = 1; dy <= height; dy++) {
                    boolean corner = (dx == 0 || dx == width - 1) && (dz == 0 || dz == depth - 1);
                    boolean beam = corner || dy == 1 || dy == height
                            || (dz == 0 || dz == depth - 1) && Math.floorMod(dx, 5) == 0;
                    set(level, x + dx, y + dy, z + dz, beam ? frame : wall);
                }
            }
        }
    }

    private static void roof(ServerLevel level, int x, int baseY, int z,
                             int width, int depth, Block material) {
        int front = z - 1;
        int back = z + depth;
        int layer = 0;
        while (front <= back) {
            int roofY = baseY + layer;
            fill(level, x - 1, roofY, front, x + width, roofY, Math.min(front + 1, back), material);
            if (back > front + 1) {
                fill(level, x - 1, roofY, Math.max(front, back - 1), x + width, roofY, back, material);
            }
            front += 2;
            back -= 2;
            layer++;
        }
        int ridgeY = baseY + Math.max(0, layer - 1);
        int ridgeZ = z + depth / 2;
        fill(level, x - 1, ridgeY, ridgeZ, x + width, ridgeY, ridgeZ, Blocks.DARK_OAK_SLAB);
    }

    private static void tower(ServerLevel level, int x, int y, int z, int size, int height) {
        clear(level, x - 1, y + 1, z - 1, x + size, y + height + 4, z + size);
        flatten(level, x - 1, z - 1, x + size, z + size, y, Blocks.STONE_BRICKS);
        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                if (dx != 0 && dx != size - 1 && dz != 0 && dz != size - 1) continue;
                fill(level, x + dx, y + 1, z + dz, x + dx, y + height, z + dz, Blocks.STONE_BRICKS);
            }
        }
        fill(level, x - 1, y + height + 1, z - 1,
                x + size, y + height + 1, z + size, Blocks.DEEPSLATE_TILES);
        battlements(level, x - 1, y + height + 2, z - 1, size + 2, size + 2);
        doorway(level, x + size / 2, y, z);
    }

    private static void rebuildMarket(ServerLevel level, int cx, int y, int cz) {
        clear(level, cx - 29, y + 1, cz - 25, cx + 29, y + 10, cz + 25);
        flatten(level, cx - 27, cz - 23, cx + 27, cz + 23, y, Blocks.STONE_BRICKS);
        int[][] stalls = {{-20, -14}, {-7, -14}, {7, -14}, {-20, 9}, {-7, 9}, {7, 9}};
        for (int[] offset : stalls) {
            int x = cx + offset[0];
            int z = cz + offset[1];
            fill(level, x, y + 1, z, x + 7, y + 1, z + 4, Blocks.SPRUCE_PLANKS);
            for (int px : new int[]{x, x + 7}) {
                for (int pz : new int[]{z, z + 4}) {
                    fill(level, px, y + 2, pz, px, y + 5, pz, Blocks.STRIPPED_OAK_LOG);
                }
            }
            fill(level, x - 1, y + 6, z - 1, x + 8, y + 6, z + 5, Blocks.DARK_OAK_SLAB);
            set(level, x + 2, y + 2, z + 2, Blocks.BARREL);
        }
        fill(level, cx, y + 1, cz, cx, y + 7, cz, Blocks.CHISELED_STONE_BRICKS);
        set(level, cx, y + 8, cz, Blocks.LANTERN);
    }

    private static void rebuildStable(ServerLevel level, int x, int y, int z) {
        clear(level, x - 3, y + 1, z - 3, x + 27, y + 9, z + 19);
        flatten(level, x - 2, z - 2, x + 25, z + 17, y, Blocks.COARSE_DIRT);
        for (int dx = 0; dx <= 22; dx++) {
            set(level, x + dx, y + 1, z, Blocks.OAK_FENCE);
            set(level, x + dx, y + 1, z + 14, Blocks.OAK_FENCE);
        }
        for (int dz = 0; dz <= 14; dz++) {
            set(level, x, y + 1, z + dz, Blocks.OAK_FENCE);
            set(level, x + 22, y + 1, z + dz, Blocks.OAK_FENCE);
        }
        for (int bay = 0; bay < 3; bay++) {
            int bx = x + 1 + bay * 7;
            for (int px : new int[]{bx, bx + 6}) {
                fill(level, px, y + 1, z + 8, px, y + 4, z + 8, Blocks.STRIPPED_SPRUCE_LOG);
                fill(level, px, y + 1, z + 14, px, y + 4, z + 14, Blocks.STRIPPED_SPRUCE_LOG);
            }
            fill(level, bx - 1, y + 5, z + 7, bx + 7, y + 5, z + 15, Blocks.SPRUCE_SLAB);
            set(level, bx + 3, y + 1, z + 11, Blocks.HAY_BLOCK);
        }
        clear(level, x + 10, y + 1, z, x + 12, y + 3, z);
    }

    private static void windows(ServerLevel level, int x, int y, int z,
                                int width, int depth, int height) {
        if (height < 4) return;
        for (int px = x + 3; px <= x + width - 4; px += 5) {
            set(level, px, y + 3, z, Blocks.GLASS_PANE);
            set(level, px, y + 3, z + depth - 1, Blocks.GLASS_PANE);
        }
        for (int pz = z + 3; pz <= z + depth - 4; pz += 5) {
            set(level, x, y + 3, pz, Blocks.GLASS_PANE);
            set(level, x + width - 1, y + 3, pz, Blocks.GLASS_PANE);
        }
    }

    private static void doorway(ServerLevel level, int x, int y, int z) {
        clear(level, x - 1, y + 1, z, x + 1, y + 3, z);
    }

    private static void battlements(ServerLevel level, int x, int y, int z, int width, int depth) {
        for (int dx = 0; dx < width; dx += 2) {
            set(level, x + dx, y, z, Blocks.STONE_BRICK_WALL);
            set(level, x + dx, y, z + depth - 1, Blocks.STONE_BRICK_WALL);
        }
        for (int dz = 0; dz < depth; dz += 2) {
            set(level, x, y, z + dz, Blocks.STONE_BRICK_WALL);
            set(level, x + width - 1, y, z + dz, Blocks.STONE_BRICK_WALL);
        }
    }

    private static void road(ServerLevel level, int x1, int z1, int x2, int z2,
                             int y, int halfWidth, Block center, Block edge) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        boolean xMajor = Math.abs(x2 - x1) >= Math.abs(z2 - z1);
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : i / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            for (int side = -halfWidth; side <= halfWidth; side++) {
                int px = xMajor ? x : x + side;
                int pz = xMajor ? z + side : z;
                set(level, px, y - 2, pz, Blocks.DIRT);
                set(level, px, y - 1, pz, Blocks.DIRT);
                set(level, px, y, pz, Math.abs(side) == halfWidth ? edge : center);
                clear(level, px, y + 1, pz, px, y + 3, pz);
            }
        }
    }

    private static void flatten(ServerLevel level, int x1, int z1, int x2, int z2,
                                int y, Block surface) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                set(level, x, y - 3, z, Blocks.DIRT);
                set(level, x, y - 2, z, Blocks.DIRT);
                set(level, x, y - 1, z, Blocks.DIRT);
                set(level, x, y, z, surface);
            }
        }
    }

    private static void clear(ServerLevel level, int x1, int y1, int z1,
                              int x2, int y2, int z2) {
        fill(level, x1, y1, z1, x2, y2, z2, Blocks.AIR);
    }

    private static void fill(ServerLevel level, int x1, int y1, int z1,
                             int x2, int y2, int z2, Block block) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(level, x, y, z, block);
                }
            }
        }
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        set(level, new BlockPos(x, y, z), block);
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        if (pos.getY() < level.getMinY() || pos.getY() >= level.getMaxY()) return;
        if (level.getBlockState(pos).getBlock() == block) return;
        level.setBlock(pos, block.defaultBlockState(), UPDATE_FLAGS);
    }
}
