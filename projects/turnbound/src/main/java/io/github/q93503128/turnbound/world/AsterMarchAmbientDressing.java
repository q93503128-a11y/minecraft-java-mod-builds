package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Presentation-only authored density for the canonical Aster March regions.
 *
 * These props deliberately carry no quest, reward or lore semantics. They make the 1024x1024 campaign shell read
 * like a lived-in RPG field while keeping all progression truth in the v0.4 canon-driven systems.
 */
public final class AsterMarchAmbientDressing {
    private static final BlockPos MARKER_A = new BlockPos(-470, 44, 470);
    private static final BlockPos MARKER_B = new BlockPos(-469, 44, 470);
    private static final BlockPos MARKER_C = new BlockPos(-468, 44, 470);

    private AsterMarchAmbientDressing() {}

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
        timberShelter(level, 132, 67, 300, Blocks.OAK_LOG, Blocks.SPRUCE_PLANKS);
        set(level, 128, 68, 298, Blocks.BARREL);
        set(level, 130, 68, 298, Blocks.CRAFTING_TABLE);
        set(level, 134, 68, 299, Blocks.HAY_BLOCK);
        set(level, 135, 68, 299, Blocks.HAY_BLOCK);
        fencePatch(level, 148, 66, 315, 13, 9, Blocks.OAK_FENCE);
        for (int i = 0; i < 7; i++) {
            int x = 143 + i * 3;
            set(level, x, 66, 312, Blocks.DIRT_PATH);
            set(level, x, 66, 318, Blocks.DIRT_PATH);
        }
        stonePile(level, 177, 66, 282, Blocks.COBBLESTONE, Blocks.ANDESITE);
        stonePile(level, 104, 67, 276, Blocks.MOSSY_COBBLESTONE, Blocks.STONE);
    }

    private static void gloamwood(ServerLevel level) {
        rootCircle(level, 56, 69, -270, 8);
        rootCircle(level, -112, 70, -366, 9);
        sporePatch(level, 20, 69, -318, 7);
        sporePatch(level, -130, 69, -330, 8);
        timberShelter(level, -92, 70, -314, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS);
        set(level, -94, 71, -313, Blocks.BARREL);
        set(level, -90, 71, -313, Blocks.CAULDRON);
    }

    private static void aqueduct(ServerLevel level) {
        brokenChannel(level, -356, 64, 72, 22);
        brokenChannel(level, -382, 64, 112, 18);
        sluiceRuins(level, -410, 63, 15);
        stonePile(level, -302, 65, 158, Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS);
        stonePile(level, -450, 63, -42, Blocks.MOSSY_STONE_BRICKS, Blocks.ANDESITE);
    }

    private static void quarry(ServerLevel level) {
        scaffold(level, 78, 66, 398, 10, 7);
        scaffold(level, -58, 66, 452, 8, 6);
        slagPile(level, 94, 64, 424, 7);
        slagPile(level, -80, 68, 382, 6);
        for (int i = 0; i < 5; i++) {
            set(level, 132 + i * 4, 66, 390, Blocks.RAIL);
            set(level, 132 + i * 4, 67, 394, i % 2 == 0 ? Blocks.BARREL : Blocks.IRON_BLOCK);
        }
    }

    private static void relay(ServerLevel level) {
        maintenanceRib(level, 320, 67, -245, 8);
        maintenanceRib(level, 345, 67, -272, 10);
        maintenanceRib(level, 395, 66, -285, 9);
        for (int i = 0; i < 5; i++) {
            int x = 300 + i * 28;
            int z = -220 - i * 22;
            relayDebris(level, x, 66 + (i / 2), z);
        }
    }

    private static void timberShelter(ServerLevel level, int cx, int groundY, int cz, Block log, Block plank) {
        for (int dx : new int[]{-4, 4}) for (int dz : new int[]{-3, 3}) {
            for (int dy = 1; dy <= 4; dy++) set(level, cx + dx, groundY + dy, cz + dz, log);
        }
        for (int dx = -5; dx <= 5; dx++) for (int dz = -4; dz <= 4; dz++) {
            if ((Math.abs(dx) + Math.abs(dz)) % 2 == 0 || Math.abs(dx) == 5 || Math.abs(dz) == 4) {
                set(level, cx + dx, groundY + 5, cz + dz, plank);
            }
        }
        for (int dx = -3; dx <= 3; dx++) set(level, cx + dx, groundY, cz, Blocks.DIRT_PATH);
        set(level, cx - 3, groundY + 1, cz, Blocks.LANTERN);
        set(level, cx + 3, groundY + 1, cz, Blocks.LANTERN);
    }

    private static void fencePatch(ServerLevel level, int cx, int groundY, int cz, int width, int depth, Block fence) {
        int hw = width / 2, hd = depth / 2;
        for (int dx = -hw; dx <= hw; dx++) {
            if (Math.abs(dx) > 1) set(level, cx + dx, groundY + 1, cz - hd, fence);
            set(level, cx + dx, groundY + 1, cz + hd, fence);
        }
        for (int dz = -hd; dz <= hd; dz++) {
            set(level, cx - hw, groundY + 1, cz + dz, fence);
            set(level, cx + hw, groundY + 1, cz + dz, fence);
        }
    }

    private static void rootCircle(ServerLevel level, int cx, int groundY, int cz, int radius) {
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2.0 * i / 24.0;
            int x = cx + (int)Math.round(Math.cos(angle) * radius);
            int z = cz + (int)Math.round(Math.sin(angle) * radius);
            set(level, x, groundY + 1, z, i % 3 == 0 ? Blocks.DARK_OAK_LOG : Blocks.MOSS_BLOCK);
            if (i % 4 == 0) set(level, x, groundY + 2, z, Blocks.MOSS_CARPET);
        }
        set(level, cx, groundY + 1, cz, Blocks.MOSS_BLOCK);
        set(level, cx, groundY + 2, cz, Blocks.SOUL_LANTERN);
    }

    private static void sporePatch(ServerLevel level, int cx, int groundY, int cz, int radius) {
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            int d2 = dx * dx + dz * dz;
            if (d2 > radius * radius || Math.floorMod(dx * 17 + dz * 23, 5) != 0) continue;
            set(level, cx + dx, groundY, cz + dz, Blocks.MOSS_BLOCK);
            set(level, cx + dx, groundY + 1, cz + dz,
                    Math.floorMod(dx + dz, 3) == 0 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM);
        }
    }

    private static void brokenChannel(ServerLevel level, int cx, int groundY, int cz, int length) {
        for (int i = -length / 2; i <= length / 2; i++) {
            set(level, cx + i, groundY, cz, Blocks.STONE_BRICKS);
            if (Math.floorMod(i, 5) != 0) {
                set(level, cx + i, groundY + 1, cz - 3, Blocks.MOSSY_STONE_BRICKS);
                set(level, cx + i, groundY + 1, cz + 3, Blocks.CRACKED_STONE_BRICKS);
            }
            if (Math.floorMod(i, 4) == 0) set(level, cx + i, groundY, cz + 1, Blocks.WATER);
        }
    }

    private static void sluiceRuins(ServerLevel level, int cx, int groundY, int cz) {
        for (int dx : new int[]{-7, 0, 7}) {
            for (int dy = 1; dy <= 8; dy++) set(level, cx + dx, groundY + dy, cz, Blocks.STONE_BRICKS);
            for (int dz = -4; dz <= 4; dz++) set(level, cx + dx, groundY + 8, cz + dz, Blocks.STONE_BRICKS);
        }
        for (int dz = -4; dz <= 4; dz += 2) set(level, cx, groundY + 1, cz + dz, Blocks.IRON_BARS);
    }

    private static void scaffold(ServerLevel level, int cx, int groundY, int cz, int width, int height) {
        for (int dx : new int[]{-width / 2, 0, width / 2}) {
            for (int dy = 1; dy <= height; dy++) set(level, cx + dx, groundY + dy, cz, Blocks.OAK_FENCE);
        }
        for (int dy = 2; dy <= height; dy += 2) {
            for (int dx = -width / 2; dx <= width / 2; dx++) set(level, cx + dx, groundY + dy, cz, Blocks.SPRUCE_SLAB);
        }
        set(level, cx, groundY + height + 1, cz, Blocks.LANTERN);
    }

    private static void maintenanceRib(ServerLevel level, int cx, int groundY, int cz, int halfWidth) {
        for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            boolean edge = Math.abs(dx) >= halfWidth - 1;
            if (edge) for (int dy = 1; dy <= 7; dy++) set(level, cx + dx, groundY + dy, cz, Blocks.DEEPSLATE_BRICKS);
            set(level, cx + dx, groundY + 7, cz, Blocks.POLISHED_DEEPSLATE);
        }
        for (int dx = -halfWidth + 2; dx <= halfWidth - 2; dx += 3) set(level, cx + dx, groundY + 5, cz, Blocks.IRON_BARS);
    }

    private static void relayDebris(ServerLevel level, int cx, int groundY, int cz) {
        set(level, cx, groundY + 1, cz, Blocks.CRYING_OBSIDIAN);
        set(level, cx + 1, groundY + 1, cz, Blocks.DEEPSLATE_TILES);
        set(level, cx - 1, groundY + 1, cz + 1, Blocks.IRON_BARS);
        set(level, cx + 2, groundY + 1, cz - 1, Blocks.REDSTONE_LAMP);
        set(level, cx, groundY + 2, cz + 2, Blocks.CHAIN);
    }

    private static void slagPile(ServerLevel level, int cx, int groundY, int cz, int radius) {
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            int d2 = dx * dx + dz * dz;
            if (d2 > radius * radius) continue;
            int height = Math.max(1, radius / 2 - (int)Math.sqrt(d2) / 2);
            for (int dy = 0; dy < height; dy++) {
                Block block = Math.floorMod(dx * 13 + dz * 7 + dy, 6) == 0 ? Blocks.MAGMA_BLOCK : Blocks.BASALT;
                set(level, cx + dx, groundY + dy, cz + dz, block);
            }
        }
    }

    private static void stonePile(ServerLevel level, int cx, int groundY, int cz, Block a, Block b) {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            if (dx * dx + dz * dz > 10) continue;
            int height = 1 + Math.floorMod(dx * 11 + dz * 7, 3);
            for (int dy = 1; dy <= height; dy++) set(level, cx + dx, groundY + dy, cz + dz, (dx + dz + dy & 1) == 0 ? a : b);
        }
    }

    private static boolean hasMarker(ServerLevel level) {
        return level.getBlockState(MARKER_A).is(Blocks.LODESTONE)
                && level.getBlockState(MARKER_B).is(Blocks.EMERALD_BLOCK)
                && level.getBlockState(MARKER_C).is(Blocks.AMETHYST_BLOCK);
    }

    private static void writeMarker(ServerLevel level) {
        level.setBlock(MARKER_A, Blocks.LODESTONE.defaultBlockState(), 2);
        level.setBlock(MARKER_B, Blocks.EMERALD_BLOCK.defaultBlockState(), 2);
        level.setBlock(MARKER_C, Blocks.AMETHYST_BLOCK.defaultBlockState(), 2);
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 2);
    }
}
