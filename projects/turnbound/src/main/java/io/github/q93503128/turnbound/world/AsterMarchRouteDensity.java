package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Authored micro-landmarks placed between the major chapter beats.
 *
 * This class intentionally owns no rewards, quest flags or encounter truth. Its only job is to keep the canonical
 * routes from feeling like empty corridors between battle triggers: short rest pockets, abandoned work sites,
 * sight-line landmarks and boss-foreshadowing architecture give each region a readable exploration rhythm.
 */
public final class AsterMarchRouteDensity {
    private static final BlockPos MARKER_A = new BlockPos(-480, 44, 480);
    private static final BlockPos MARKER_B = new BlockPos(-479, 44, 480);
    private static final BlockPos MARKER_C = new BlockPos(-478, 44, 480);

    private AsterMarchRouteDensity() {}

    public static void build(ServerLevel level) {
        if (hasMarker(level)) return;
        southgate(level);
        gloamwood(level);
        aqueduct(level);
        quarry(level);
        relay(level);
        writeMarker(level);
    }

    private static void southgate(ServerLevel level) {
        patrolCamp(level, 58, 66, 200);
        brokenWagon(level, 145, 66, 220);
        meadowMemorial(level, 252, 66, 258);
        graulOmen(level, 326, 67, 261);
    }

    private static void gloamwood(ServerLevel level) {
        lanternFork(level, -12, 67, -161);
        abandonedForestCamp(level, -68, 69, -226);
        swallowedCauseway(level, -8, 69, -286);
        thornArch(level, -98, 70, -392);
        vernaGrove(level, -57, 71, -424);
    }

    private static void aqueduct(ServerLevel level) {
        serviceAlcove(level, -162, 65, 4);
        pipeBridge(level, -219, 65, 27);
        floodLookout(level, -292, 65, 61);
        maintenanceNiche(level, -362, 64, -8);
        oroSecurityArch(level, -413, 63, 53);
    }

    private static void quarry(ServerLevel level) {
        quarryRestCamp(level, -95, 68, 345);
        coolingGantry(level, 2, 68, 389);
        railSwitch(level, 35, 65, 417);
        workerLocker(level, -22, 64, 459);
        kolvakWarningGantry(level, 54, 62, 438);
    }

    private static void relay(ServerLevel level) {
        relaySignalArch(level, 282, 67, -196);
        relayTriageBay(level, 336, 67, -258);
        brokenSignalFork(level, 386, 66, -320);
        relayMaintenanceBay(level, 444, 65, -291);
        serakObservationSpine(level, 411, 65, -347);
    }

    private static void patrolCamp(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 5, Blocks.DIRT_PATH, Blocks.GRASS_BLOCK);
        set(l, cx, y + 1, cz, Blocks.CAMPFIRE);
        set(l, cx - 3, y + 1, cz - 2, Blocks.BARREL);
        set(l, cx + 3, y + 1, cz + 2, Blocks.HAY_BLOCK);
        for (int dz = -3; dz <= 3; dz += 3) {
            set(l, cx - 4, y + 1, cz + dz, Blocks.OAK_FENCE);
            set(l, cx + 4, y + 1, cz + dz, Blocks.OAK_FENCE);
        }
        lanternPost(l, cx - 5, y, cz + 1, false);
    }

    private static void brokenWagon(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 4, Blocks.DIRT_PATH, Blocks.GRASS_BLOCK);
        for (int dx = -3; dx <= 2; dx++) set(l, cx + dx, y + 1, cz, Blocks.OAK_PLANKS);
        set(l, cx - 2, y + 2, cz, Blocks.OAK_FENCE);
        set(l, cx + 2, y + 2, cz, Blocks.OAK_FENCE);
        set(l, cx + 3, y + 1, cz + 1, Blocks.OAK_LOG);
        set(l, cx - 3, y + 1, cz - 1, Blocks.OAK_LOG);
        set(l, cx + 1, y + 1, cz + 2, Blocks.BARREL);
        set(l, cx - 1, y + 1, cz - 2, Blocks.CHEST);
    }

    private static void meadowMemorial(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 5, Blocks.COARSE_DIRT, Blocks.GRASS_BLOCK);
        for (int dy = 1; dy <= 4; dy++) set(l, cx, y + dy, cz, dy == 4 ? Blocks.CHISELED_STONE_BRICKS : Blocks.STONE_BRICKS);
        for (int a = 0; a < 8; a++) {
            double r = a * Math.PI / 4.0;
            int x = cx + (int)Math.round(Math.cos(r) * 4);
            int z = cz + (int)Math.round(Math.sin(r) * 4);
            set(l, x, y + 1, z, a % 2 == 0 ? Blocks.COBBLESTONE_WALL : Blocks.MOSSY_COBBLESTONE);
        }
        set(l, cx, y + 5, cz, Blocks.LANTERN);
    }

    private static void graulOmen(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 6, Blocks.COARSE_DIRT, Blocks.GRASS_BLOCK);
        for (int i = -5; i <= 5; i++) {
            if (Math.abs(i) <= 1) continue;
            set(l, cx + i, y + 1, cz - 4, Blocks.OAK_FENCE);
            if ((i & 1) == 0) set(l, cx + i, y + 2, cz - 4, Blocks.CHAIN);
        }
        for (int dz = -3; dz <= 3; dz++) {
            int height = 1 + Math.abs(dz) % 3;
            for (int dy = 1; dy <= height; dy++) set(l, cx + 3, y + dy, cz + dz, Blocks.MOSSY_COBBLESTONE);
        }
        set(l, cx, y + 1, cz, Blocks.BONE_BLOCK);
        set(l, cx + 1, y + 1, cz, Blocks.BONE_BLOCK);
        set(l, cx - 1, y + 1, cz + 1, Blocks.BONE_BLOCK);
    }

    private static void lanternFork(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 5, Blocks.MOSS_BLOCK, Blocks.GRASS_BLOCK);
        for (int i = -4; i <= 4; i++) {
            set(l, cx + i, y, cz, i % 3 == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.MOSS_BLOCK);
            if (Math.abs(i) == 4) lanternPost(l, cx + i, y, cz, true);
        }
        for (int i = 1; i <= 4; i++) set(l, cx, y, cz - i, Blocks.MOSSY_STONE_BRICKS);
    }

    private static void abandonedForestCamp(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 5, Blocks.PODZOL, Blocks.MOSS_BLOCK);
        set(l, cx, y + 1, cz, Blocks.CAMPFIRE);
        set(l, cx - 3, y + 1, cz + 2, Blocks.BARREL);
        set(l, cx + 3, y + 1, cz - 2, Blocks.CAULDRON);
        for (int dy = 1; dy <= 4; dy++) set(l, cx - 5, y + dy, cz - 3, Blocks.DARK_OAK_LOG);
        for (int dx = -4; dx <= 1; dx++) set(l, cx + dx, y + 4, cz - 3, Blocks.DARK_OAK_PLANKS);
        set(l, cx - 4, y + 3, cz - 2, Blocks.SOUL_LANTERN);
    }

    private static void swallowedCauseway(ServerLevel l, int cx, int y, int cz) {
        for (int i = -10; i <= 10; i++) {
            set(l, cx + i, y, cz, i % 4 == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.MOSSY_STONE_BRICKS);
            if (i % 3 == 0) set(l, cx + i, y + 1, cz + 2, Blocks.DARK_OAK_LOG);
            if (i % 4 == 0) set(l, cx + i, y + 1, cz - 2, Blocks.MOSS_BLOCK);
        }
        set(l, cx - 8, y + 2, cz + 2, Blocks.RED_MUSHROOM);
        set(l, cx + 7, y + 2, cz - 2, Blocks.BROWN_MUSHROOM);
    }

    private static void thornArch(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 5, Blocks.MOSS_BLOCK, Blocks.PODZOL);
        for (int side : new int[]{-4, 4}) {
            for (int dy = 1; dy <= 7; dy++) set(l, cx + side, y + dy, cz, Blocks.DARK_OAK_LOG);
        }
        for (int dx = -4; dx <= 4; dx++) set(l, cx + dx, y + 7, cz, dx % 2 == 0 ? Blocks.DARK_OAK_LOG : Blocks.HANGING_ROOTS);
        for (int dx = -3; dx <= 3; dx += 2) set(l, cx + dx, y + 6, cz, Blocks.VINE);
        set(l, cx, y + 1, cz + 3, Blocks.SOUL_LANTERN);
    }

    private static void vernaGrove(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 7, Blocks.MOSS_BLOCK, Blocks.PODZOL);
        for (int a = 0; a < 12; a++) {
            double r = a * Math.PI / 6.0;
            int x = cx + (int)Math.round(Math.cos(r) * 6);
            int z = cz + (int)Math.round(Math.sin(r) * 6);
            set(l, x, y + 1, z, a % 3 == 0 ? Blocks.FLOWERING_AZALEA : Blocks.AZALEA);
        }
        for (int dy = 1; dy <= 3; dy++) set(l, cx, y + dy, cz, Blocks.AMETHYST_BLOCK);
        set(l, cx, y + 4, cz, Blocks.END_ROD);
    }

    private static void serviceAlcove(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 5, Blocks.STONE_BRICKS, Blocks.ANDESITE);
        for (int dx = -5; dx <= 5; dx++) for (int dy = 1; dy <= 4; dy++) {
            if (Math.abs(dx) == 5 || dy == 4) set(l, cx + dx, y + dy, cz + 4, Blocks.STONE_BRICKS);
        }
        set(l, cx - 2, y + 1, cz + 3, Blocks.BARREL);
        set(l, cx + 2, y + 1, cz + 3, Blocks.CRAFTING_TABLE);
        set(l, cx, y + 3, cz + 3, Blocks.LANTERN);
    }

    private static void pipeBridge(ServerLevel l, int cx, int y, int cz) {
        for (int dx = -8; dx <= 8; dx++) {
            set(l, cx + dx, y, cz, dx % 5 == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS);
            set(l, cx + dx, y + 1, cz - 3, Blocks.IRON_BARS);
            set(l, cx + dx, y + 1, cz + 3, Blocks.IRON_BARS);
        }
        for (int dx : new int[]{-8, 0, 8}) {
            for (int dy = 1; dy <= 5; dy++) set(l, cx + dx, y + dy, cz + 4, Blocks.IRON_BLOCK);
        }
        set(l, cx, y + 2, cz + 4, Blocks.REDSTONE_LAMP);
    }

    private static void floodLookout(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 6, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS);
        for (int dx = -5; dx <= 5; dx++) {
            set(l, cx + dx, y + 1, cz + 5, Blocks.IRON_BARS);
            if (dx % 5 == 0) set(l, cx + dx, y + 2, cz + 5, Blocks.LANTERN);
        }
        for (int dz = -2; dz <= 2; dz++) set(l, cx - 4, y + 1, cz + dz, Blocks.CHAIN);
        set(l, cx + 3, y + 1, cz - 2, Blocks.BARREL);
    }

    private static void maintenanceNiche(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 5, Blocks.CRACKED_STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS);
        for (int dy = 1; dy <= 5; dy++) {
            set(l, cx - 4, y + dy, cz, Blocks.STONE_BRICKS);
            set(l, cx + 4, y + dy, cz, Blocks.STONE_BRICKS);
        }
        for (int dx = -4; dx <= 4; dx++) set(l, cx + dx, y + 5, cz, Blocks.STONE_BRICKS);
        set(l, cx, y + 1, cz, Blocks.LECTERN);
        set(l, cx, y + 2, cz, Blocks.REDSTONE_LAMP);
    }

    private static void oroSecurityArch(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 6, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS);
        for (int side : new int[]{-6, 6}) for (int dy = 1; dy <= 8; dy++) set(l, cx + side, y + dy, cz, Blocks.IRON_BLOCK);
        for (int dx = -6; dx <= 6; dx++) set(l, cx + dx, y + 8, cz, dx % 3 == 0 ? Blocks.REDSTONE_LAMP : Blocks.STONE_BRICKS);
        for (int dx = -4; dx <= 4; dx += 2) set(l, cx + dx, y + 6, cz, Blocks.IRON_BARS);
    }

    private static void quarryRestCamp(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 5, Blocks.TUFF, Blocks.BASALT);
        set(l, cx, y + 1, cz, Blocks.CAMPFIRE);
        set(l, cx - 3, y + 1, cz + 2, Blocks.BARREL);
        set(l, cx + 3, y + 1, cz - 2, Blocks.BLAST_FURNACE);
        for (int dx = -4; dx <= 4; dx += 4) for (int dy = 1; dy <= 4; dy++) set(l, cx + dx, y + dy, cz + 4, Blocks.OAK_FENCE);
        for (int dx = -4; dx <= 4; dx++) set(l, cx + dx, y + 4, cz + 4, Blocks.SPRUCE_SLAB);
    }

    private static void coolingGantry(ServerLevel l, int cx, int y, int cz) {
        for (int side : new int[]{-6, 6}) {
            for (int dy = 1; dy <= 7; dy++) set(l, cx + side, y + dy, cz, Blocks.IRON_BARS);
        }
        for (int dx = -6; dx <= 6; dx++) set(l, cx + dx, y + 7, cz, Blocks.IRON_BLOCK);
        for (int dx = -4; dx <= 4; dx++) set(l, cx + dx, y + 5, cz, Blocks.CHAIN);
        set(l, cx, y + 6, cz, Blocks.WATER);
        set(l, cx - 2, y + 6, cz, Blocks.WATER);
        set(l, cx + 2, y + 6, cz, Blocks.WATER);
    }

    private static void railSwitch(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 6, Blocks.TUFF, Blocks.BASALT);
        for (int dx = -8; dx <= 8; dx++) set(l, cx + dx, y + 1, cz, dx % 5 == 0 ? Blocks.POWERED_RAIL : Blocks.RAIL);
        for (int dz = -5; dz <= 5; dz++) set(l, cx, y + 1, cz + dz, dz % 5 == 0 ? Blocks.POWERED_RAIL : Blocks.RAIL);
        set(l, cx + 4, y + 1, cz + 3, Blocks.BARREL);
        lanternPost(l, cx - 5, y, cz - 4, false);
    }

    private static void workerLocker(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 5, Blocks.BLACKSTONE, Blocks.TUFF);
        for (int dx = -4; dx <= 4; dx += 2) {
            for (int dy = 1; dy <= 3; dy++) set(l, cx + dx, y + dy, cz + 4, Blocks.IRON_BLOCK);
            set(l, cx + dx, y + 4, cz + 4, Blocks.IRON_BARS);
        }
        set(l, cx - 2, y + 1, cz, Blocks.BARREL);
        set(l, cx + 2, y + 1, cz, Blocks.ANVIL);
    }

    private static void kolvakWarningGantry(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 7, Blocks.BLACKSTONE, Blocks.BASALT);
        for (int side : new int[]{-7, 7}) for (int dy = 1; dy <= 9; dy++) set(l, cx + side, y + dy, cz, Blocks.POLISHED_BLACKSTONE_BRICKS);
        for (int dx = -7; dx <= 7; dx++) set(l, cx + dx, y + 9, cz, dx % 3 == 0 ? Blocks.MAGMA_BLOCK : Blocks.POLISHED_BLACKSTONE_BRICKS);
        set(l, cx, y + 1, cz + 4, Blocks.BLAST_FURNACE);
        set(l, cx, y + 2, cz + 4, Blocks.REDSTONE_LAMP);
    }

    private static void relaySignalArch(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 6, Blocks.DEEPSLATE_TILES, Blocks.DEEPSLATE_BRICKS);
        for (int side : new int[]{-6, 6}) for (int dy = 1; dy <= 8; dy++) set(l, cx + side, y + dy, cz, Blocks.DEEPSLATE_BRICKS);
        for (int dx = -6; dx <= 6; dx++) set(l, cx + dx, y + 8, cz, dx % 3 == 0 ? Blocks.CRYING_OBSIDIAN : Blocks.POLISHED_DEEPSLATE);
        set(l, cx, y + 6, cz, Blocks.AMETHYST_BLOCK);
    }

    private static void relayTriageBay(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 5, Blocks.DEEPSLATE_TILES, Blocks.DEEPSLATE_BRICKS);
        for (int dx = -4; dx <= 4; dx++) set(l, cx + dx, y + 1, cz + 4, Blocks.IRON_BARS);
        set(l, cx - 3, y + 1, cz, Blocks.BARREL);
        set(l, cx, y + 1, cz, Blocks.CAULDRON);
        set(l, cx + 3, y + 1, cz, Blocks.CHEST);
        set(l, cx, y + 3, cz + 4, Blocks.SOUL_LANTERN);
    }

    private static void brokenSignalFork(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 6, Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS);
        for (int branch = -1; branch <= 1; branch += 2) {
            for (int i = 0; i <= 7; i++) {
                set(l, cx + branch * i, y, cz - i, i % 3 == 0 ? Blocks.CRYING_OBSIDIAN : Blocks.DEEPSLATE_TILES);
            }
        }
        for (int dy = 1; dy <= 5; dy++) set(l, cx, y + dy, cz, Blocks.IRON_BARS);
        set(l, cx, y + 6, cz, Blocks.REDSTONE_LAMP);
    }

    private static void relayMaintenanceBay(ServerLevel l, int cx, int y, int cz) {
        pad(l, cx, y, cz, 5, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_TILES);
        for (int dx = -5; dx <= 5; dx++) for (int dy = 1; dy <= 5; dy++) {
            if (Math.abs(dx) == 5 || dy == 5) set(l, cx + dx, y + dy, cz + 4, Blocks.DEEPSLATE_BRICKS);
        }
        set(l, cx - 2, y + 1, cz + 3, Blocks.ANVIL);
        set(l, cx + 2, y + 1, cz + 3, Blocks.IRON_BLOCK);
        set(l, cx, y + 3, cz + 3, Blocks.SOUL_LANTERN);
    }

    private static void serakObservationSpine(ServerLevel l, int cx, int y, int cz) {
        for (int dz = -9; dz <= 9; dz++) {
            set(l, cx, y, cz + dz, dz % 4 == 0 ? Blocks.CRYING_OBSIDIAN : Blocks.DEEPSLATE_TILES);
            set(l, cx - 3, y + 1, cz + dz, Blocks.IRON_BARS);
            set(l, cx + 3, y + 1, cz + dz, Blocks.IRON_BARS);
        }
        for (int dz : new int[]{-8, 0, 8}) {
            set(l, cx - 3, y + 2, cz + dz, Blocks.SOUL_LANTERN);
            set(l, cx + 3, y + 2, cz + dz, Blocks.SOUL_LANTERN);
        }
        set(l, cx, y + 1, cz, Blocks.AMETHYST_BLOCK);
        set(l, cx, y + 2, cz, Blocks.END_ROD);
    }

    private static void lanternPost(ServerLevel l, int x, int y, int z, boolean soul) {
        set(l, x, y + 1, z, Blocks.COBBLESTONE_WALL);
        set(l, x, y + 2, z, Blocks.OAK_FENCE);
        set(l, x, y + 3, z, soul ? Blocks.SOUL_LANTERN : Blocks.LANTERN);
    }

    private static void pad(ServerLevel l, int cx, int y, int cz, int r, Block primary, Block secondary) {
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (dx * dx + dz * dz > r * r) continue;
            Block block = Math.floorMod(dx * 17 + dz * 31, 5) == 0 ? secondary : primary;
            set(l, cx + dx, y, cz + dz, block);
        }
    }

    private static boolean hasMarker(ServerLevel level) {
        return level.getBlockState(MARKER_A).is(Blocks.LODESTONE)
                && level.getBlockState(MARKER_B).is(Blocks.POLISHED_ANDESITE)
                && level.getBlockState(MARKER_C).is(Blocks.CRYING_OBSIDIAN);
    }

    private static void writeMarker(ServerLevel level) {
        level.setBlock(MARKER_A, Blocks.LODESTONE.defaultBlockState(), 2);
        level.setBlock(MARKER_B, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
        level.setBlock(MARKER_C, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 2);
    }
}
