package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Aster March / South Road A02: first Chapter 1 clear-gated continuation cell. */
public final class FieldCellA02 {
    public static final String ID = "aster_southroad_a02";
    public static final int ORIGIN_X = -32;
    public static final int ORIGIN_Z = 192;
    public static final int BASE_Y = 64;
    public static final int SIZE = 64;

    private FieldCellA02() {}

    public record BuiltCell(int baseY, Vec3 entry, Vec3 relay, Vec3 southExit) {}

    public static BuiltCell build(ServerLevel level) {
        clearAndLevel(level);
        layRoad(level);
        shapeRockyEdges(level);
        buildFoothold(level);
        buildRelayDais(level);
        buildNorthLock(level);
        addVegetation(level);
        return new BuiltCell(BASE_Y, local(32.5, 1.0, 5.5), local(19.5, 3.0, 27.5), local(32.5, 1.0, 58.5));
    }

    public static boolean containsXZ(double x, double z) {
        return x >= ORIGIN_X && x < ORIGIN_X + SIZE && z >= ORIGIN_Z && z < ORIGIN_Z + SIZE;
    }

    public static void unlockNorthGate(ServerLevel level) {
        for (int lx = 28; lx <= 36; lx++) {
            for (int y = 1; y <= 4; y++) {
                set(level, ORIGIN_X + lx, BASE_Y + y, ORIGIN_Z, Blocks.AIR);
            }
        }
    }

    private static Vec3 local(double x, double y, double z) {
        return new Vec3(ORIGIN_X + x, BASE_Y + y, ORIGIN_Z + z);
    }

    private static int roadCenterX(int localZ) {
        return 32 + (int) Math.round(Math.sin((localZ + 11) / 10.0) * 3.0);
    }

    private static void clearAndLevel(ServerLevel level) {
        for (int lx = 0; lx < SIZE; lx++) {
            for (int lz = 0; lz < SIZE; lz++) {
                int x = ORIGIN_X + lx;
                int z = ORIGIN_Z + lz;
                int rise = Math.max(0, (lz - 36) / 12);
                int groundY = BASE_Y + rise;
                for (int y = BASE_Y - 3; y < groundY; y++) set(level, x, y, z, Blocks.DIRT);
                set(level, x, groundY, z, ((lx + lz) & 7) == 0 ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK);
                for (int y = groundY + 1; y <= BASE_Y + 16; y++) set(level, x, y, z, Blocks.AIR);
            }
        }
    }

    private static void layRoad(ServerLevel level) {
        for (int lz = 0; lz < SIZE; lz++) {
            int center = roadCenterX(lz);
            int rise = Math.max(0, (lz - 36) / 12);
            int y = BASE_Y + rise;
            for (int dx = -3; dx <= 3; dx++) {
                int lx = center + dx;
                if (lx < 2 || lx >= SIZE - 2) continue;
                Block block = Math.abs(dx) <= 1
                        ? (((lx + lz) & 3) == 0 ? Blocks.COBBLESTONE : Blocks.GRAVEL)
                        : (((lx * 5 + lz * 11) & 3) == 0 ? Blocks.COARSE_DIRT : Blocks.DIRT_PATH);
                set(level, ORIGIN_X + lx, y, ORIGIN_Z + lz, block);
            }
        }
    }

    private static void shapeRockyEdges(ServerLevel level) {
        for (int lz = 5; lz < SIZE - 3; lz++) {
            int left = 8 + Math.floorMod(lz * 7, 5);
            int right = 54 - Math.floorMod(lz * 11, 5);
            int hLeft = 1 + Math.floorMod(lz, 3);
            int hRight = 1 + Math.floorMod(lz + 1, 3);
            for (int y = 1; y <= hLeft; y++) set(level, ORIGIN_X + left, BASE_Y + y, ORIGIN_Z + lz, Blocks.ANDESITE);
            for (int y = 1; y <= hRight; y++) set(level, ORIGIN_X + right, BASE_Y + y, ORIGIN_Z + lz, Blocks.STONE);
            if (lz % 9 == 0) {
                set(level, ORIGIN_X + left + 1, BASE_Y + 1, ORIGIN_Z + lz, Blocks.MOSSY_COBBLESTONE);
                set(level, ORIGIN_X + right - 1, BASE_Y + 1, ORIGIN_Z + lz, Blocks.MOSSY_COBBLESTONE);
            }
        }
    }

    private static void buildFoothold(ServerLevel level) {
        for (int lx = 13; lx <= 27; lx++) {
            for (int lz = 20; lz <= 34; lz++) {
                if ((lx - 20) * (lx - 20) + (lz - 27) * (lz - 27) > 58) continue;
                set(level, ORIGIN_X + lx, BASE_Y, ORIGIN_Z + lz, ((lx + lz) & 5) == 0 ? Blocks.COARSE_DIRT : Blocks.PODZOL);
            }
        }
        for (int x = 15; x <= 24; x++) {
            set(level, ORIGIN_X + x, BASE_Y + 1, ORIGIN_Z + 18, Blocks.SPRUCE_PLANKS);
            if (x == 15 || x == 24) {
                for (int y = 2; y <= 4; y++) set(level, ORIGIN_X + x, BASE_Y + y, ORIGIN_Z + 18, Blocks.SPRUCE_LOG);
            }
        }
        for (int x = 16; x <= 23; x++) set(level, ORIGIN_X + x, BASE_Y + 4, ORIGIN_Z + 18, Blocks.SPRUCE_SLAB);
        set(level, ORIGIN_X + 18, BASE_Y + 1, ORIGIN_Z + 31, Blocks.CAMPFIRE);
        set(level, ORIGIN_X + 23, BASE_Y + 1, ORIGIN_Z + 31, Blocks.BARREL);
    }

    private static void buildRelayDais(ServerLevel level) {
        int cx = 19;
        int cz = 27;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > 3) continue;
                set(level, ORIGIN_X + cx + dx, BASE_Y + 1, ORIGIN_Z + cz + dz,
                        ((dx + dz) & 1) == 0 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS);
            }
        }
        set(level, ORIGIN_X + cx, BASE_Y + 2, ORIGIN_Z + cz, Blocks.AMETHYST_BLOCK);
    }

    private static void buildNorthLock(ServerLevel level) {
        for (int lx = 28; lx <= 36; lx++) {
            set(level, ORIGIN_X + lx, BASE_Y + 1, ORIGIN_Z, Blocks.IRON_BARS);
            set(level, ORIGIN_X + lx, BASE_Y + 2, ORIGIN_Z, Blocks.IRON_BARS);
            if (lx == 28 || lx == 36) {
                set(level, ORIGIN_X + lx, BASE_Y + 3, ORIGIN_Z, Blocks.STONE_BRICKS);
                set(level, ORIGIN_X + lx, BASE_Y + 4, ORIGIN_Z, Blocks.LANTERN);
            }
        }
    }

    private static void addVegetation(ServerLevel level) {
        int[][] trees = {{7,12},{10,42},{13,52},{48,11},{53,29},{49,49}};
        for (int i = 0; i < trees.length; i++) buildSpruce(level, trees[i][0], trees[i][1], 5 + (i & 1));
        for (int lx = 4; lx < SIZE - 4; lx++) {
            for (int lz = 4; lz < SIZE - 4; lz++) {
                if (Math.abs(lx - roadCenterX(lz)) <= 5) continue;
                int hash = Math.floorMod(lx * 37 + lz * 67 + lx * lz, 113);
                if (hash == 0) set(level, ORIGIN_X + lx, BASE_Y + 1, ORIGIN_Z + lz, Blocks.FERN);
                else if (hash == 1) set(level, ORIGIN_X + lx, BASE_Y + 1, ORIGIN_Z + lz, Blocks.SWEET_BERRY_BUSH);
            }
        }
    }

    private static void buildSpruce(ServerLevel level, int lx, int lz, int trunk) {
        int x = ORIGIN_X + lx;
        int z = ORIGIN_Z + lz;
        for (int y = 1; y <= trunk; y++) set(level, x, BASE_Y + y, z, Blocks.SPRUCE_LOG);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = trunk - 2; dy <= trunk + 1; dy++) {
            if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy - trunk) > 4) continue;
            if (dx == 0 && dz == 0 && dy <= trunk) continue;
            set(level, x + dx, BASE_Y + dy, z + dz, Blocks.SPRUCE_LEAVES);
        }
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 2);
    }
}
