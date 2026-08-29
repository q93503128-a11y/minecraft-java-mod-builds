package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Aster March / Southgate Meadow A01: one authored 64x64 field cell inside the v0.1 coordinate plan. */
public final class FieldCellA01 {
    public static final String ID = "aster_southgate_a01";
    public static final int ORIGIN_X = -32;
    public static final int ORIGIN_Z = 128;
    public static final int BASE_Y = 64;
    public static final int SIZE = 64;

    private FieldCellA01() {}

    public record BuiltCell(int baseY, Vec3 entry, Vec3 encounterHome, Vec3 encounterPatrolEnd) {}

    public static BuiltCell build(ServerLevel level) {
        clearAndLevel(level);
        layRoad(level);
        layStreamAndBridge(level);
        buildEncounterClearing(level);
        buildSouthContinuation(level);
        buildEdges(level);
        addVegetation(level);
        addRoadFurniture(level);
        return new BuiltCell(BASE_Y, local(32.5, 1.0, 5.5), local(24.5, 1.0, 35.5), local(43.5, 1.0, 39.5));
    }

    public static boolean contains(Vec3 position) {
        return position.x >= ORIGIN_X + 1 && position.x < ORIGIN_X + SIZE - 1
                && position.z >= ORIGIN_Z + 1 && position.z < ORIGIN_Z + SIZE - 1;
    }

    public static boolean containsXZ(double x, double z) {
        return x >= ORIGIN_X && x < ORIGIN_X + SIZE && z >= ORIGIN_Z && z < ORIGIN_Z + SIZE;
    }

    static int roadCenterX(int localZ) { return 32 + (int) Math.round(Math.sin(localZ / 9.0) * 4.0); }
    static boolean isRadiaGate(int localX, int localZ) { return localZ <= 2 && Math.abs(localX - 32) <= 4; }
    static boolean isFutureSouthGate(int localX, int localZ) { return localZ >= SIZE - 3 && Math.abs(localX - 32) <= 3; }

    private static Vec3 local(double x, double y, double z) { return new Vec3(ORIGIN_X + x, BASE_Y + y, ORIGIN_Z + z); }

    private static void clearAndLevel(ServerLevel level) {
        for (int lx = 0; lx < SIZE; lx++) {
            for (int lz = 0; lz < SIZE; lz++) {
                int x = ORIGIN_X + lx;
                int z = ORIGIN_Z + lz;
                for (int y = BASE_Y - 3; y <= BASE_Y - 1; y++) set(level, x, y, z, Blocks.DIRT);
                set(level, x, BASE_Y, z, Blocks.GRASS_BLOCK);
                for (int y = BASE_Y + 1; y <= BASE_Y + 14; y++) set(level, x, y, z, Blocks.AIR);
            }
        }
    }

    private static void layRoad(ServerLevel level) {
        for (int lz = 0; lz < SIZE; lz++) {
            int center = roadCenterX(lz);
            for (int dx = -3; dx <= 3; dx++) {
                int lx = center + dx;
                if (lx < 2 || lx >= SIZE - 2) continue;
                Block block;
                if (Math.abs(dx) <= 1) block = ((lx + lz) & 3) == 0 ? Blocks.COARSE_DIRT : Blocks.GRAVEL;
                else block = ((lx * 13 + lz * 7) & 3) == 0 ? Blocks.COARSE_DIRT : Blocks.DIRT_PATH;
                set(level, ORIGIN_X + lx, BASE_Y, ORIGIN_Z + lz, block);
            }
        }
    }

    private static void layStreamAndBridge(ServerLevel level) {
        for (int lz = 8; lz < SIZE - 7; lz++) {
            int streamX = 13 + (int) Math.round(Math.sin(lz / 7.0) * 2.0);
            for (int dx = -2; dx <= 2; dx++) {
                int x = ORIGIN_X + streamX + dx;
                int z = ORIGIN_Z + lz;
                set(level, x, BASE_Y, z, Blocks.WATER);
                set(level, x, BASE_Y - 1, z, ((lz + dx) & 1) == 0 ? Blocks.GRAVEL : Blocks.CLAY);
            }
        }
        int bridgeZ = 25;
        for (int lx = 10; lx <= 19; lx++) {
            for (int dz = -2; dz <= 2; dz++) set(level, ORIGIN_X + lx, BASE_Y + 1, ORIGIN_Z + bridgeZ + dz, Blocks.OAK_PLANKS);
        }
        for (int lx = 10; lx <= 19; lx++) {
            if ((lx & 1) == 0) {
                set(level, ORIGIN_X + lx, BASE_Y + 2, ORIGIN_Z + bridgeZ - 2, Blocks.OAK_FENCE);
                set(level, ORIGIN_X + lx, BASE_Y + 2, ORIGIN_Z + bridgeZ + 2, Blocks.OAK_FENCE);
            }
        }
    }

    private static void buildEncounterClearing(ServerLevel level) {
        int cx = 34;
        int cz = 37;
        for (int lx = cx - 12; lx <= cx + 12; lx++) {
            for (int lz = cz - 10; lz <= cz + 10; lz++) {
                double d = Math.pow((lx - cx) / 12.0, 2) + Math.pow((lz - cz) / 10.0, 2);
                if (d > 1.0) continue;
                Block block = ((lx * 17 + lz * 31) & 7) == 0 ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK;
                set(level, ORIGIN_X + lx, BASE_Y, ORIGIN_Z + lz, block);
            }
        }
        for (int lx = 47; lx <= 53; lx++) {
            if (lx == 50) continue;
            set(level, ORIGIN_X + lx, BASE_Y + 1, ORIGIN_Z + 31, Blocks.MOSSY_STONE_BRICKS);
            if ((lx & 1) == 0) set(level, ORIGIN_X + lx, BASE_Y + 2, ORIGIN_Z + 31, Blocks.CRACKED_STONE_BRICKS);
        }
    }

    private static void buildSouthContinuation(ServerLevel level) {
        int z = ORIGIN_Z + 55;
        for (int lx = 22; lx <= 42; lx++) {
            if (lx >= 29 && lx <= 36) continue;
            Block block = (lx % 3 == 0) ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS;
            set(level, ORIGIN_X + lx, BASE_Y + 1, z, block);
            if ((lx & 1) == 0) set(level, ORIGIN_X + lx, BASE_Y + 2, z, block);
        }
        for (int lx : new int[]{27, 28, 37, 38}) {
            for (int y = 1; y <= 4; y++) set(level, ORIGIN_X + lx, BASE_Y + y, z, y == 4 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS);
        }
        for (int lx = 29; lx <= 36; lx++) {
            set(level, ORIGIN_X + lx, BASE_Y + 1, ORIGIN_Z + 60, Blocks.COBBLESTONE);
            if ((lx & 1) == 0) set(level, ORIGIN_X + lx, BASE_Y + 2, ORIGIN_Z + 60, Blocks.MOSSY_COBBLESTONE);
        }
    }

    private static void buildEdges(ServerLevel level) {
        for (int i = 0; i < SIZE; i++) {
            if (!isRadiaGate(i, 0)) roughEdge(level, ORIGIN_X + i, BASE_Y, ORIGIN_Z, i * 19 + 3);
            if (!isFutureSouthGate(i, SIZE - 1)) roughEdge(level, ORIGIN_X + i, BASE_Y, ORIGIN_Z + SIZE - 1, i * 23 + 9);
            roughForestEdge(level, ORIGIN_X, BASE_Y, ORIGIN_Z + i, i * 29 + 5);
            roughForestEdge(level, ORIGIN_X + SIZE - 1, BASE_Y, ORIGIN_Z + i, i * 31 + 7);
        }
        for (int x : new int[]{27, 28, 36, 37}) {
            for (int y = 1; y <= 5; y++) set(level, ORIGIN_X + x, BASE_Y + y, ORIGIN_Z + 2, Blocks.STONE_BRICKS);
        }
    }

    private static void roughEdge(ServerLevel level, int x, int baseY, int z, int seed) {
        int h = 2 + Math.floorMod(seed, 3);
        for (int y = 1; y <= h; y++) set(level, x, baseY + y, z, ((seed + y) & 2) == 0 ? Blocks.COBBLESTONE : Blocks.MOSSY_COBBLESTONE);
    }

    private static void roughForestEdge(ServerLevel level, int x, int baseY, int z, int seed) {
        for (int y = 1; y <= 3; y++) set(level, x, baseY + y, z, Blocks.SPRUCE_LEAVES);
        if (Math.floorMod(seed, 5) == 0) for (int y = 1; y <= 5; y++) set(level, x, baseY + y, z, Blocks.SPRUCE_LOG);
    }

    private static void addVegetation(ServerLevel level) {
        int[][] trees = {{7,10},{10,18},{8,36},{11,48},{21,10},{50,9},{54,18},{53,46},{46,51}};
        for (int i = 0; i < trees.length; i++) buildTree(level, trees[i][0], trees[i][1], i % 3 == 0);
        for (int lx = 4; lx < SIZE - 4; lx++) {
            for (int lz = 4; lz < SIZE - 4; lz++) {
                if (Math.abs(lx - roadCenterX(lz)) <= 5) continue;
                if (lx >= 21 && lx <= 48 && lz >= 26 && lz <= 47) continue;
                int hash = Math.floorMod(lx * 71 + lz * 47 + lx * lz * 3, 97);
                if (hash == 0) set(level, ORIGIN_X + lx, BASE_Y + 1, ORIGIN_Z + lz, Blocks.DANDELION);
                else if (hash == 1) set(level, ORIGIN_X + lx, BASE_Y + 1, ORIGIN_Z + lz, Blocks.POPPY);
                else if (hash == 2) set(level, ORIGIN_X + lx, BASE_Y + 1, ORIGIN_Z + lz, Blocks.FERN);
            }
        }
    }

    private static void buildTree(ServerLevel level, int lx, int lz, boolean spruce) {
        Block log = spruce ? Blocks.SPRUCE_LOG : Blocks.OAK_LOG;
        Block leaves = spruce ? Blocks.SPRUCE_LEAVES : Blocks.OAK_LEAVES;
        int trunk = spruce ? 6 : 5;
        int x = ORIGIN_X + lx;
        int z = ORIGIN_Z + lz;
        for (int y = 1; y <= trunk; y++) set(level, x, BASE_Y + y, z, log);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = trunk - 2; dy <= trunk + 1; dy++) {
            if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy - trunk) > 4) continue;
            if (dx == 0 && dz == 0 && dy <= trunk) continue;
            set(level, x + dx, BASE_Y + dy, z + dz, leaves);
        }
    }

    private static void addRoadFurniture(ServerLevel level) {
        int[][] lanternPosts = {{27,13},{39,21},{27,48},{40,50}};
        for (int[] p : lanternPosts) {
            int x = ORIGIN_X + p[0];
            int z = ORIGIN_Z + p[1];
            set(level, x, BASE_Y + 1, z, Blocks.COBBLESTONE_WALL);
            set(level, x, BASE_Y + 2, z, Blocks.OAK_FENCE);
            set(level, x, BASE_Y + 3, z, Blocks.LANTERN);
        }
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 2);
    }
}
