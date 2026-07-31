package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Builds the named origin residences outside or above each regional capital. */
public final class RegionalResidenceBuilder {
    private RegionalResidenceBuilder() {
    }

    public static void build(ServerLevel level, String homelandId, RealmSiteLayoutSavedData.RealmSite site) {
        switch (homelandId) {
            case "silvana_forest" -> silvana(level, site);
            case "kardum_league" -> kardum(level, site);
            default -> erden(level, site);
        }
    }

    private static void erden(ServerLevel level, RealmSiteLayoutSavedData.RealmSite site) {
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = Math.max(68, Math.min(112, site.baseY()));
        cottage(level, cx + 170, y, cz + 100, Blocks.STRIPPED_OAK_LOG, Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS);
        cottage(level, cx - 176, y, cz + 110, Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS);
        pier(level, cx - 172, y, cz + 116);
        camp(level, cx + 133, y, cz - 167);
    }

    private static void silvana(ServerLevel level, RealmSiteLayoutSavedData.RealmSite site) {
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = Math.max(70, Math.min(122, site.baseY()));
        supportPlatform(level, cx - 58, y + 16, cz - 30, 8);
        cottage(level, cx + 82, y, cz + 82, Blocks.STRIPPED_BIRCH_LOG, Blocks.BIRCH_PLANKS,
                Blocks.FLOWERING_AZALEA_LEAVES);
    }

    private static void kardum(ServerLevel level, RealmSiteLayoutSavedData.RealmSite site) {
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = Math.max(74, Math.min(138, site.baseY()));
        stoneRoom(level, cx - 78, y + 1, cz + 38);
        stoneRoom(level, cx - 10, y + 1, cz - 77);
    }

    private static void cottage(ServerLevel level, int x, int y, int z, Block frame, Block wall, Block roof) {
        lot(level, x - 3, z - 3, x + 13, z + 12, y, Blocks.GRASS_BLOCK);
        fill(level, x, y, z, x + 10, y, z + 9, Blocks.STONE_BRICKS);
        for (int dx = 0; dx <= 10; dx++) for (int dz = 0; dz <= 9; dz++) {
            if (dx != 0 && dx != 10 && dz != 0 && dz != 9) continue;
            for (int dy = 1; dy <= 5; dy++) {
                boolean beam = (dx == 0 || dx == 10) && (dz == 0 || dz == 9) || dy == 1 || dy == 5;
                set(level, x + dx, y + dy, z + dz, beam ? frame : wall);
            }
        }
        clear(level, x + 5, y + 1, z, x + 5, y + 3, z);
        for (int layer = 0; layer <= 4; layer++) {
            fill(level, x - 1, y + 6 + layer, z - 1 + layer, x + 11, y + 6 + layer, z - 1 + layer, roof);
            fill(level, x - 1, y + 6 + layer, z + 10 - layer, x + 11, y + 6 + layer, z + 10 - layer, roof);
        }
        set(level, x + 8, y + 2, z + 7, Blocks.LANTERN);
        set(level, x + 3, y + 2, z + 7, Blocks.BARREL);
    }

    private static void pier(ServerLevel level, int x, int y, int z) {
        for (int dz = 0; dz <= 22; dz++) {
            fill(level, x - 2, y, z + dz, x + 2, y, z + dz, Blocks.SPRUCE_PLANKS);
            if (dz % 5 == 0) {
                set(level, x - 2, y - 3, z + dz, Blocks.SPRUCE_LOG);
                set(level, x + 2, y - 3, z + dz, Blocks.SPRUCE_LOG);
            }
        }
        set(level, x, y + 1, z + 8, Blocks.BARREL);
    }

    private static void camp(ServerLevel level, int x, int y, int z) {
        lot(level, x - 9, z - 9, x + 9, z + 9, y, Blocks.COARSE_DIRT);
        set(level, x, y + 1, z, Blocks.CAMPFIRE);
        for (int[] p : new int[][]{{-6, -5}, {5, -5}, {-6, 5}, {5, 5}}) {
            for (int dy = 1; dy <= 4; dy++) set(level, x + p[0], y + dy, z + p[1], Blocks.SPRUCE_FENCE);
        }
        fill(level, x - 7, y + 5, z - 6, x + 6, y + 5, z + 6, Blocks.DARK_OAK_SLAB);
        set(level, x - 3, y + 1, z + 2, Blocks.CHEST);
        set(level, x + 3, y + 1, z + 2, Blocks.WHITE_BED);
    }

    private static void supportPlatform(ServerLevel level, int x, int y, int z, int radius) {
        int ground = RealmSitePlanner.surfaceY(level, x, z);
        for (int py = ground + 1; py <= y; py++) set(level, x, py, z, Blocks.DARK_OAK_LOG);
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            if (dx * dx + dz * dz <= radius * radius) set(level, x + dx, y, z + dz, Blocks.STRIPPED_BIRCH_WOOD);
        }
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI * 2.0 / 12.0;
            int px = x + (int) Math.round(Math.cos(angle) * (radius - 1));
            int pz = z + (int) Math.round(Math.sin(angle) * (radius - 1));
            set(level, px, y + 1, pz, Blocks.OAK_FENCE);
        }
        set(level, x + 3, y + 1, z + 2, Blocks.CHEST);
        set(level, x - 3, y + 1, z + 2, Blocks.GREEN_BED);
    }

    private static void stoneRoom(ServerLevel level, int x, int y, int z) {
        lot(level, x - 3, z - 3, x + 15, z + 13, y, Blocks.POLISHED_ANDESITE);
        fill(level, x, y, z, x + 12, y, z + 10, Blocks.POLISHED_DEEPSLATE);
        for (int dx = 0; dx <= 12; dx++) for (int dz = 0; dz <= 10; dz++) {
            if (dx != 0 && dx != 12 && dz != 0 && dz != 10) continue;
            for (int dy = 1; dy <= 6; dy++) set(level, x + dx, y + dy, z + dz,
                    dy <= 2 ? Blocks.DEEPSLATE_BRICKS : Blocks.POLISHED_ANDESITE);
        }
        clear(level, x + 6, y + 1, z, x + 6, y + 3, z);
        fill(level, x - 1, y + 7, z - 1, x + 13, y + 7, z + 11, Blocks.DEEPSLATE_TILES);
        set(level, x + 9, y + 2, z + 7, Blocks.LANTERN);
        set(level, x + 3, y + 2, z + 7, Blocks.BARREL);
    }

    private static void lot(ServerLevel level, int x1, int z1, int x2, int z2, int y, Block surface) {
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            int oldY = RealmSitePlanner.surfaceY(level, x, z);
            if (oldY < y) fill(level, x, oldY + 1, z, x, y - 1, z, Blocks.DIRT);
            if (oldY > y) clear(level, x, y + 1, z, x, oldY + 1, z);
            set(level, x, y, z, surface);
        }
    }

    private static void clear(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2) {
        fill(level, x1, y1, z1, x2, y2, z2, Blocks.AIR);
    }

    private static void fill(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        if (y2 < y1) return;
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(level, x, y, z, block);
        }
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        if (y < level.getMinY() || y >= level.getMaxY()) return;
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).getBlock() == block) return;
        level.setBlock(pos, block.defaultBlockState(), 2);
    }
}
