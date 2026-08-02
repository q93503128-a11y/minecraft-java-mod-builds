package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Final layout integrity pass for authored settlements.
 *
 * <p>This does not reshape terrain. It corrects known lot conflicts that cannot be allowed to survive
 * into a playable capital, such as a house footprint occupying a primary avenue.</p>
 */
public final class RealmLayoutIntegrity {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;

    private RealmLayoutIntegrity() {
    }

    public static void apply(ServerLevel level, String homelandId,
                             RealmSiteLayoutSavedData.RealmSite site) {
        if ("erden_kingdom".equals(homelandId)) repairErdenMainAvenue(level, site);
    }

    private static void repairErdenMainAvenue(ServerLevel level,
                                               RealmSiteLayoutSavedData.RealmSite site) {
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = Math.max(68, Math.min(104, site.baseY()));

        HouseLot[] obsolete = {
                new HouseLot(cx - 35, cz - 39, 12, 11, 6, 1),
                new HouseLot(cx - 11, cz - 39, 13, 9, 5, 2),
                new HouseLot(cx - 34, cz + 32, 11, 10, 5, 6),
                new HouseLot(cx - 9, cz + 32, 12, 11, 6, 7)
        };
        HouseLot[] corrected = {
                new HouseLot(cx - 39, cz - 39, 12, 11, 6, 1),
                new HouseLot(cx - 21, cz - 39, 13, 9, 5, 2),
                new HouseLot(cx - 39, cz + 32, 11, 10, 5, 6),
                new HouseLot(cx - 22, cz + 32, 12, 11, 6, 7)
        };

        // Remove every part of the conflicting houses first. Doing all removals before rebuilding
        // prevents an old roof overhang from clipping a newly positioned neighbour.
        for (HouseLot lot : obsolete) {
            clearHouseEnvelope(level, y, lot);
            restorePlateauAndAvenue(level, cx, y, lot);
        }
        for (HouseLot lot : corrected) buildTownHouse(level, y, lot);

        verifyAvenue(level, cx, y, cz - 48, cz + 48);
        LivingKingdoms.LOGGER.info(
                "Relocated four Erden town houses and verified the central avenue remains unobstructed"
        );
    }

    private static void clearHouseEnvelope(ServerLevel level, int y, HouseLot lot) {
        fill(level,
                lot.x - 2, y, lot.z - 2,
                lot.x + lot.width + 1, y + lot.height + 10, lot.z + lot.depth + 1,
                Blocks.AIR);
    }

    private static void restorePlateauAndAvenue(ServerLevel level, int avenueX, int y, HouseLot lot) {
        for (int x = lot.x - 1; x <= lot.x + lot.width; x++) {
            for (int z = lot.z - 1; z <= lot.z + lot.depth; z++) {
                set(level, x, y - 1, z, Blocks.DIRT);
                int side = x - avenueX;
                Block surface = Math.abs(side) <= 4
                        ? (Math.abs(side) == 4 ? Blocks.STONE_BRICKS : Blocks.PACKED_MUD)
                        : Blocks.GRASS_BLOCK;
                set(level, x, y, z, surface);
            }
        }
    }

    private static void buildTownHouse(ServerLevel level, int y, HouseLot lot) {
        int x = lot.x;
        int z = lot.z;
        int width = lot.width;
        int depth = lot.depth;
        int height = lot.height;

        fill(level, x - 1, y + 1, z - 1,
                x + width, y + height + 10, z + depth, Blocks.AIR);
        fill(level, x - 1, y - 1, z - 1,
                x + width, y - 1, z + depth, Blocks.STONE_BRICKS);
        fill(level, x, y, z, x + width - 1, y, z + depth - 1, Blocks.SPRUCE_PLANKS);

        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                if (dx != 0 && dx != width - 1 && dz != 0 && dz != depth - 1) continue;
                for (int dy = 1; dy <= height; dy++) {
                    boolean corner = (dx == 0 || dx == width - 1)
                            && (dz == 0 || dz == depth - 1);
                    boolean beam = corner || dy == 1 || dy == height
                            || (dx % 5 == 0 && dz % 5 == 0);
                    Block wall = beam ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.BIRCH_PLANKS;
                    if (!beam && dy >= 3 && dy <= 4 && ((dx + dz) % 7 == 0)) {
                        wall = Blocks.GLASS_PANE;
                    }
                    set(level, x + dx, y + dy, z + dz, wall);
                }
            }
        }

        fill(level, x + width / 2 - 1, y + 1, z,
                x + width / 2 + 1, y + 3, z, Blocks.AIR);
        roof(level, x - 2, y + height + 1, z - 2,
                width + 4, depth + 4, Blocks.DARK_OAK_PLANKS);

        set(level, x + 2, y + 1, z + depth - 3, Blocks.BARREL);
        set(level, x + width - 3, y + 1, z + depth - 3, Blocks.CRAFTING_TABLE);
        set(level, x + width / 2, y + 2, z + depth - 2, Blocks.LANTERN);
    }

    private static void roof(ServerLevel level, int x, int y, int z,
                             int width, int depth, Block block) {
        int layers = Math.max(3, Math.min(6, depth / 2));
        for (int layer = 0; layer < layers; layer++) {
            int front = z + layer;
            int back = z + depth - 1 - layer;
            fill(level, x, y + layer, front, x + width - 1, y + layer, front, block);
            fill(level, x, y + layer, back, x + width - 1, y + layer, back, block);
        }
        int middleStart = z + layers;
        int middleEnd = z + depth - 1 - layers;
        if (middleStart <= middleEnd) {
            fill(level, x, y + layers, middleStart,
                    x + width - 1, y + layers, middleEnd, block);
        }
    }

    private static void verifyAvenue(ServerLevel level, int cx, int y, int minZ, int maxZ) {
        for (int z = minZ; z <= maxZ; z++) {
            // The market square intentionally uses stone brick paving in the central section.
            if (Math.abs(z) <= 22) continue;
            for (int side = -4; side <= 4; side++) {
                Block expected = Math.abs(side) == 4 ? Blocks.STONE_BRICKS : Blocks.PACKED_MUD;
                BlockPos ground = new BlockPos(cx + side, y, z);
                if (level.getBlockState(ground).getBlock() != expected) {
                    set(level, ground.getX(), ground.getY(), ground.getZ(), expected);
                }
                fill(level, ground.getX(), y + 1, z,
                        ground.getX(), y + 3, z, Blocks.AIR);
            }
        }
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
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).getBlock() == block) return;
        level.setBlock(pos, block.defaultBlockState(), FLAGS);
    }

    private record HouseLot(int x, int z, int width, int depth, int height, int variant) {
    }
}
