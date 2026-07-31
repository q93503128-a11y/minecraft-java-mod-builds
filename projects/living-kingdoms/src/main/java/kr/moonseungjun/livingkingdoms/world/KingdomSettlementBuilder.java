package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Terrain-integrated regional capitals built after a real noise-terrain survey. */
public final class KingdomSettlementBuilder {
    private KingdomSettlementBuilder() {
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
        int y = clamp(site.baseY(), 68, 112);
        Palette p = new Palette(Blocks.STONE_BRICKS, Blocks.STRIPPED_SPRUCE_LOG,
                Blocks.BIRCH_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_PLANKS);

        grade(level, cx - 128, cz - 108, cx + 128, cz + 108, y, Blocks.GRASS_BLOCK, Blocks.DIRT, 18);
        slopeShoulder(level, cx - 128, cz - 108, cx + 128, cz + 108, y, Blocks.GRASS_BLOCK);
        walls(level, cx, y, cz, 124, 104, p);
        road(level, cx, cz - 104, cx, cz + 104, y, 4, Blocks.PACKED_MUD, Blocks.STONE_BRICKS);
        road(level, cx - 124, cz, cx + 124, cz, y, 4, Blocks.PACKED_MUD, Blocks.STONE_BRICKS);
        road(level, cx - 96, cz - 55, cx + 96, cz - 55, y, 3, Blocks.GRAVEL, Blocks.COBBLESTONE);
        road(level, cx - 96, cz + 59, cx + 96, cz + 59, y, 3, Blocks.GRAVEL, Blocks.COBBLESTONE);

        citadel(level, cx - 31, y, cz - 92, p);
        hall(level, cx + 20, y, cz - 83, 29, 18, 8, p);
        temple(level, cx - 74, y, cz - 78);
        market(level, cx, y, cz);
        inn(level, cx + 46, y, cz + 14, p);
        guild(level, cx - 75, y, cz + 18, p);
        smithy(level, cx + 79, y, cz - 34, p);
        barracks(level, cx - 106, y, cz - 38, p);
        granary(level, cx + 82, y, cz + 73, p);
        stable(level, cx - 96, y, cz + 73);

        int[][] homes = {
                {-55, -39}, {-33, -38}, {-10, -38}, {19, -38}, {47, -39},
                {-56, 31}, {-31, 32}, {-7, 33}, {21, 31}, {50, 34},
                {-112, 23}, {-110, 50}, {96, 21}, {99, 49},
                {-59, 79}, {-32, 79}, {-4, 80}, {27, 79}, {54, 79}
        };
        for (int i = 0; i < homes.length; i++) {
            house(level, cx + homes[i][0], y, cz + homes[i][1], 10 + i % 3, 8 + (i + 1) % 3, p);
        }

        canal(level, cx - 166, y, cz - 145, cz + 145);
        bridge(level, cx - 173, y + 1, cz, cx - 119, y + 1, cz);
        road(level, cx - 166, cz, cx - 124, cz, y + 1, 3, Blocks.GRAVEL, Blocks.COBBLESTONE);
        dock(level, cx - 168, y, cz + 68);
        farm(level, cx + 150, y, cz + 58, 36, 28);
        farm(level, cx + 150, y, cz - 18, 36, 28);
        farm(level, cx - 198, y, cz + 80, 34, 27);
        orchard(level, cx + 152, y, cz - 88, 6, 5, Blocks.OAK_LOG, Blocks.OAK_LEAVES);
        outerTrees(level, cx, cz, 150, 32, Blocks.OAK_LOG, Blocks.OAK_LEAVES);
    }

    private static void silvana(ServerLevel level, RealmSiteLayoutSavedData.RealmSite site) {
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = clamp(site.baseY(), 70, 122);
        gradeEllipse(level, cx, cz, 95, 80, y, Blocks.MOSS_BLOCK, Blocks.DIRT);
        giantTree(level, cx, y, cz, 6, 27);
        platform(level, cx, y + 23, cz, 18);
        elvenLodge(level, cx - 60, y + 2, cz - 31, 11);
        elvenLodge(level, cx + 58, y + 1, cz - 22, 10);
        elvenLodge(level, cx - 40, y + 1, cz + 57, 10);
        elvenLodge(level, cx + 47, y + 2, cz + 60, 12);
        platform(level, cx - 60, y + 16, cz - 31, 10);
        platform(level, cx + 58, y + 15, cz - 22, 9);
        canopyBridge(level, cx, y + 23, cz, cx - 60, y + 16, cz - 31);
        canopyBridge(level, cx, y + 23, cz, cx + 58, y + 15, cz - 22);
        moonGarden(level, cx + 88, y, cz + 82);
        councilCircle(level, cx - 84, y, cz + 74);
        road(level, cx, cz, cx + 88, cz + 82, y, 2, Blocks.ROOTED_DIRT, Blocks.MOSS_BLOCK);
        road(level, cx, cz, cx - 84, cz + 74, y, 2, Blocks.ROOTED_DIRT, Blocks.MOSS_BLOCK);
        road(level, cx, cz, cx, cz - 108, y, 2, Blocks.ROOTED_DIRT, Blocks.MOSS_BLOCK);
        outerTrees(level, cx, cz, 112, 76, Blocks.DARK_OAK_LOG, Blocks.FLOWERING_AZALEA_LEAVES);
    }

    private static void kardum(ServerLevel level, RealmSiteLayoutSavedData.RealmSite site) {
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = clamp(site.baseY(), 74, 138);
        Palette p = new Palette(Blocks.DEEPSLATE_BRICKS, Blocks.POLISHED_BASALT,
                Blocks.POLISHED_ANDESITE, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE);
        grade(level, cx - 112, cz - 102, cx + 112, cz + 105, y, Blocks.STONE, Blocks.STONE, 14);
        terraces(level, cx, y, cz);
        dwarvenGate(level, cx, y, cz - 90);
        hall(level, cx - 38, y + 6, cz - 35, 27, 18, 8, p);
        hall(level, cx + 19, y + 9, cz - 21, 29, 19, 9, p);
        forge(level, cx - 16, y + 3, cz + 43, p);
        quarter(level, cx - 84, y, cz + 35, p);
        quarter(level, cx + 66, y + 3, cz + 39, p);
        vault(level, cx + 60, y + 10, cz - 72, p);
        stoneRoad(level, cx, y, cz - 90, cx, y + 6, cz + 82);
        stoneRoad(level, cx - 96, y, cz + 35, cx + 96, y + 3, cz + 35);
        mine(level, cx - 107, y + 2, cz - 60);
        oreYard(level, cx + 96, y + 1, cz + 74);
    }

    private static void citadel(ServerLevel level, int x, int y, int z, Palette p) {
        prepareLot(level, x - 5, z - 5, x + 38, z + 32, y, p.foundation());
        hall(level, x, y, z, 33, 24, 9, p);
        tower(level, x - 4, y, z - 4, 8, 15, p);
        tower(level, x + 29, y, z - 4, 8, 15, p);
        tower(level, x - 4, y, z + 20, 8, 15, p);
        tower(level, x + 29, y, z + 20, 8, 15, p);
    }

    private static void hall(ServerLevel level, int x, int y, int z, int w, int d, int h, Palette p) {
        prepareLot(level, x - 3, z - 3, x + w + 2, z + d + 2, y, p.foundation());
        fill(level, x, y, z, x + w - 1, y, z + d - 1, p.floor());
        for (int dx = 0; dx < w; dx++) for (int dz = 0; dz < d; dz++) {
            if (dx != 0 && dx != w - 1 && dz != 0 && dz != d - 1) continue;
            for (int dy = 1; dy <= h; dy++) {
                boolean beam = ((dx == 0 || dx == w - 1) && (dz == 0 || dz == d - 1))
                        || dy == 1 || dy == h || dx % 5 == 0;
                set(level, x + dx, y + dy, z + dz, beam ? p.frame() : p.wall());
            }
        }
        clear(level, x + w / 2 - 1, y + 1, z, x + w / 2 + 1, y + 4, z);
        roof(level, x - 2, y + h + 1, z - 2, w + 4, d + 4, p.roof());
        set(level, x + w / 2, y + 3, z + d - 3, Blocks.LANTERN);
    }

    private static void house(ServerLevel level, int x, int y, int z, int w, int d, Palette p) {
        hall(level, x, y, z, w, d, 5, p);
        set(level, x + 2, y + 3, z, Blocks.GLASS_PANE);
        set(level, x + w - 3, y + 3, z, Blocks.GLASS_PANE);
        fill(level, x + 1, y + 1, z + 1, x + w - 2, y + 1, z + d - 2, p.floor());
    }

    private static void roof(ServerLevel level, int x, int y, int z, int w, int d, Block block) {
        int layers = Math.max(3, Math.min(6, d / 2));
        for (int layer = 0; layer < layers; layer++) {
            int front = z + layer;
            int back = z + d - 1 - layer;
            fill(level, x, y + layer, front, x + w - 1, y + layer, front, block);
            fill(level, x, y + layer, back, x + w - 1, y + layer, back, block);
        }
        if (z + layers <= z + d - 1 - layers) {
            fill(level, x, y + layers, z + layers, x + w - 1, y + layers, z + d - 1 - layers, block);
        }
    }

    private static void temple(ServerLevel level, int x, int y, int z) {
        Palette p = new Palette(Blocks.SMOOTH_STONE, Blocks.CHISELED_STONE_BRICKS,
                Blocks.CALCITE, Blocks.STONE_BRICKS, Blocks.SMOOTH_STONE);
        hall(level, x, y, z, 22, 17, 7, p);
        set(level, x + 11, y + 2, z + 12, Blocks.BELL);
        set(level, x + 11, y + 1, z + 8, Blocks.LANTERN);
    }

    private static void market(ServerLevel level, int cx, int y, int cz) {
        prepareLot(level, cx - 26, cz - 22, cx + 26, cz + 22, y, Blocks.STONE_BRICKS);
        for (int[] o : new int[][]{{-21, -15}, {-7, -15}, {8, -15}, {-21, 10}, {-7, 10}, {8, 10}}) {
            int x = cx + o[0];
            int z = cz + o[1];
            fill(level, x, y + 1, z, x + 10, y + 1, z + 5, Blocks.SPRUCE_PLANKS);
            for (int px : new int[]{x, x + 10}) for (int py = 2; py <= 5; py++) set(level, px, y + py, z, Blocks.OAK_FENCE);
            fill(level, x, y + 6, z - 1, x + 10, y + 6, z + 6, Blocks.DARK_OAK_SLAB);
            set(level, x + 5, y + 2, z + 3, Blocks.BARREL);
        }
        for (int dy = 1; dy <= 6; dy++) set(level, cx, y + dy, cz, Blocks.CHISELED_STONE_BRICKS);
        set(level, cx, y + 7, cz, Blocks.LANTERN);
    }

    private static void inn(ServerLevel level, int x, int y, int z, Palette p) {
        hall(level, x, y, z, 21, 14, 6, p);
        set(level, x + 5, y + 2, z + 5, Blocks.BARREL);
        set(level, x + 10, y + 2, z + 7, Blocks.CAMPFIRE);
        set(level, x + 16, y + 2, z + 5, Blocks.CHEST);
    }

    private static void guild(ServerLevel level, int x, int y, int z, Palette p) {
        hall(level, x, y, z, 23, 15, 7, p);
        set(level, x + 5, y + 2, z + 5, Blocks.CARTOGRAPHY_TABLE);
        set(level, x + 10, y + 2, z + 5, Blocks.LECTERN);
        set(level, x + 15, y + 2, z + 5, Blocks.FLETCHING_TABLE);
    }

    private static void smithy(ServerLevel level, int x, int y, int z, Palette p) {
        hall(level, x, y, z, 16, 11, 6, p);
        prepareLot(level, x + 16, z, x + 28, z + 12, y, Blocks.STONE_BRICKS);
        set(level, x + 20, y + 1, z + 5, Blocks.BLAST_FURNACE);
        set(level, x + 23, y + 1, z + 5, Blocks.ANVIL);
        set(level, x + 26, y + 1, z + 8, Blocks.LAVA);
    }

    private static void barracks(ServerLevel level, int x, int y, int z, Palette p) {
        hall(level, x, y, z, 29, 18, 7, p);
        fill(level, x + 20, y + 1, z + 3, x + 26, y + 5, z + 14, Blocks.IRON_BARS);
        clear(level, x + 23, y + 1, z + 3, x + 23, y + 3, z + 3);
    }

    private static void granary(ServerLevel level, int x, int y, int z, Palette p) {
        hall(level, x, y, z, 19, 12, 6, p);
        for (int dx = 3; dx <= 15; dx += 4) set(level, x + dx, y + 2, z + 7, Blocks.BARREL);
    }

    private static void stable(ServerLevel level, int x, int y, int z) {
        prepareLot(level, x - 2, z - 2, x + 24, z + 16, y, Blocks.COARSE_DIRT);
        for (int dx = 0; dx <= 22; dx++) {
            set(level, x + dx, y + 1, z, Blocks.OAK_FENCE);
            set(level, x + dx, y + 1, z + 14, Blocks.OAK_FENCE);
        }
        fill(level, x, y + 5, z, x + 22, y + 5, z + 14, Blocks.SPRUCE_SLAB);
        for (int dx = 2; dx <= 20; dx += 6) set(level, x + dx, y + 1, z + 7, Blocks.HAY_BLOCK);
    }

    private static void walls(ServerLevel level, int cx, int y, int cz, int rx, int rz, Palette p) {
        for (int x = cx - rx; x <= cx + rx; x++) {
            wallColumn(level, x, y, cz - rz, p.foundation());
            wallColumn(level, x, y, cz + rz, p.foundation());
        }
        for (int z = cz - rz; z <= cz + rz; z++) {
            wallColumn(level, cx - rx, y, z, p.foundation());
            wallColumn(level, cx + rx, y, z, p.foundation());
        }
        for (int[] c : new int[][]{{-rx, -rz}, {rx, -rz}, {-rx, rz}, {rx, rz}}) {
            tower(level, cx + c[0] - 4, y, cz + c[1] - 4, 9, 13, p);
        }
        gate(level, cx, y, cz - rz, true, p);
        gate(level, cx, y, cz + rz, true, p);
        gate(level, cx - rx, y, cz, false, p);
        gate(level, cx + rx, y, cz, false, p);
    }

    private static void wallColumn(ServerLevel level, int x, int y, int z, Block block) {
        for (int dy = 1; dy <= 6; dy++) set(level, x, y + dy, z, block);
        if (Math.floorMod(x + z, 2) == 0) set(level, x, y + 7, z, Blocks.STONE_BRICK_WALL);
    }

    private static void gate(ServerLevel level, int x, int y, int z, boolean northSouth, Palette p) {
        int x1 = northSouth ? x - 10 : x - 5;
        int z1 = northSouth ? z - 5 : z - 10;
        int x2 = northSouth ? x + 10 : x + 5;
        int z2 = northSouth ? z + 5 : z + 10;
        for (int px = x1; px <= x2; px++) for (int pz = z1; pz <= z2; pz++) {
            if (px != x1 && px != x2 && pz != z1 && pz != z2) continue;
            for (int dy = 1; dy <= 9; dy++) set(level, px, y + dy, pz, p.foundation());
        }
        if (northSouth) clear(level, x - 4, y + 1, z1, x + 4, y + 6, z2);
        else clear(level, x1, y + 1, z - 4, x2, y + 6, z + 4);
        fill(level, x1 - 1, y + 10, z1 - 1, x2 + 1, y + 10, z2 + 1, p.roof());
        set(level, x, y + 9, z, Blocks.BELL);
    }

    private static void tower(ServerLevel level, int x, int y, int z, int size, int height, Palette p) {
        prepareLot(level, x - 1, z - 1, x + size, z + size, y, p.foundation());
        for (int dx = 0; dx < size; dx++) for (int dz = 0; dz < size; dz++) {
            if (dx != 0 && dx != size - 1 && dz != 0 && dz != size - 1) continue;
            for (int dy = 1; dy <= height; dy++) set(level, x + dx, y + dy, z + dz, p.foundation());
        }
        fill(level, x - 1, y + height + 1, z - 1, x + size, y + height + 1, z + size, p.roof());
        clear(level, x + size / 2, y + 1, z, x + size / 2, y + 3, z);
    }

    private static void canal(ServerLevel level, int x, int y, int z1, int z2) {
        for (int z = z1; z <= z2; z++) {
            int bend = (int) Math.round(Math.sin(z * 0.045) * 6.0);
            for (int dx = -8; dx <= 8; dx++) {
                int px = x + bend + dx;
                clear(level, px, y - 1, z, px, y + 9, z);
                fill(level, px, y - 4, z, px, y - 1, z, Blocks.WATER);
                set(level, px, y - 5, z, Math.abs(dx) > 5 ? Blocks.CLAY : Blocks.GRAVEL);
            }
        }
    }

    private static void bridge(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            fill(level, x, y, z - 3, x, y, z + 3, Blocks.STONE_BRICKS);
            set(level, x, y + 1, z - 4, Blocks.STONE_BRICK_WALL);
            set(level, x, y + 1, z + 4, Blocks.STONE_BRICK_WALL);
        }
    }

    private static void dock(ServerLevel level, int x, int y, int z) {
        for (int dx = -4; dx <= 18; dx++) {
            fill(level, x + dx, y, z - 4, x + dx, y, z + 4, Blocks.SPRUCE_PLANKS);
            if (dx % 5 == 0) {
                set(level, x + dx, y - 3, z - 4, Blocks.SPRUCE_LOG);
                set(level, x + dx, y - 3, z + 4, Blocks.SPRUCE_LOG);
            }
        }
        set(level, x + 8, y + 1, z, Blocks.BARREL);
    }

    private static void farm(ServerLevel level, int x, int y, int z, int w, int d) {
        prepareLot(level, x - 2, z - 2, x + w + 1, z + d + 1, y, Blocks.GRASS_BLOCK);
        for (int dx = 0; dx < w; dx++) for (int dz = 0; dz < d; dz++) {
            if (dx % 7 == 0) set(level, x + dx, y, z + dz, Blocks.WATER);
            else {
                set(level, x + dx, y, z + dz, Blocks.FARMLAND);
                if ((dx + dz) % 4 != 0) set(level, x + dx, y + 1, z + dz, Blocks.WHEAT);
            }
        }
        for (int dx = -1; dx <= w; dx++) {
            set(level, x + dx, y + 1, z - 1, Blocks.OAK_FENCE);
            set(level, x + dx, y + 1, z + d, Blocks.OAK_FENCE);
        }
    }

    private static void orchard(ServerLevel level, int x, int y, int z, int cols, int rows, Block log, Block leaves) {
        for (int cx = 0; cx < cols; cx++) for (int rz = 0; rz < rows; rz++) {
            tree(level, x + cx * 8, y, z + rz * 8, log, leaves, 5 + (cx + rz) % 2);
        }
    }

    private static void outerTrees(ServerLevel level, int cx, int cz, int radius, int count, Block log, Block leaves) {
        for (int i = 0; i < count; i++) {
            double angle = i * 2.399963229728653;
            int r = radius + (i % 5) * 10;
            int x = cx + (int) Math.round(Math.cos(angle) * r);
            int z = cz + (int) Math.round(Math.sin(angle) * r);
            int y = RealmSitePlanner.surfaceY(level, x, z);
            tree(level, x, y, z, log, leaves, 5 + i % 4);
        }
    }

    private static void giantTree(ServerLevel level, int x, int y, int z, int radius, int height) {
        for (int dy = 0; dy <= height; dy++) {
            int r = Math.max(2, radius - dy / 8);
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz <= r * r) set(level, x + dx, y + dy, z + dz, Blocks.DARK_OAK_LOG);
            }
        }
        int crown = y + height;
        for (int dx = -14; dx <= 14; dx++) for (int dz = -14; dz <= 14; dz++) for (int dy = -4; dy <= 6; dy++) {
            if (dx * dx + dz * dz + dy * dy * 2 <= 190) set(level, x + dx, crown + dy, z + dz, Blocks.AZALEA_LEAVES);
        }
    }

    private static void platform(ServerLevel level, int x, int y, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            if (dx * dx + dz * dz <= radius * radius) set(level, x + dx, y, z + dz, Blocks.STRIPPED_BIRCH_WOOD);
        }
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI * 2.0 / 16.0;
            int px = x + (int) Math.round(Math.cos(a) * (radius - 1));
            int pz = z + (int) Math.round(Math.sin(a) * (radius - 1));
            set(level, px, y + 1, pz, Blocks.OAK_FENCE);
            if (i % 4 == 0) set(level, px, y + 2, pz, Blocks.LANTERN);
        }
        int ground = RealmSitePlanner.surfaceY(level, x, z);
        for (int py = ground + 1; py < y; py++) set(level, x, py, z, Blocks.DARK_OAK_LOG);
    }

    private static void elvenLodge(ServerLevel level, int x, int y, int z, int radius) {
        prepareLot(level, x - radius - 2, z - radius - 2, x + radius + 2, z + radius + 2, y, Blocks.MOSS_BLOCK);
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            int d2 = dx * dx + dz * dz;
            if (d2 <= radius * radius) set(level, x + dx, y, z + dz, Blocks.STRIPPED_BIRCH_WOOD);
            if (d2 >= (radius - 2) * (radius - 2) && d2 <= radius * radius) {
                for (int dy = 1; dy <= 5; dy++) set(level, x + dx, y + dy, z + dz, Blocks.BIRCH_PLANKS);
            }
        }
        for (int dy = 6; dy <= 10; dy++) {
            int r = Math.max(2, radius - (dy - 5) * 2);
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz <= r * r) set(level, x + dx, y + dy, z + dz, Blocks.FLOWERING_AZALEA_LEAVES);
            }
        }
        clear(level, x - 1, y + 1, z - radius, x + 1, y + 3, z - radius);
        set(level, x, y + 3, z, Blocks.SOUL_LANTERN);
    }

    private static void canopyBridge(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            fill(level, x - 1, y, z, x + 1, y, z, Blocks.DARK_OAK_PLANKS);
            if (i % 4 == 0) {
                set(level, x - 2, y + 1, z, Blocks.OAK_FENCE);
                set(level, x + 2, y + 1, z, Blocks.OAK_FENCE);
            }
        }
    }

    private static void moonGarden(ServerLevel level, int x, int y, int z) {
        prepareLot(level, x - 14, z - 14, x + 14, z + 14, y, Blocks.MOSS_BLOCK);
        for (int dx = -10; dx <= 10; dx++) for (int dz = -10; dz <= 10; dz++) {
            int d2 = dx * dx + dz * dz;
            if (d2 <= 64) set(level, x + dx, y, z + dz, Blocks.WATER);
            else if (d2 <= 100) set(level, x + dx, y, z + dz, Blocks.MOSSY_STONE_BRICKS);
        }
        set(level, x, y - 1, z, Blocks.SEA_LANTERN);
    }

    private static void councilCircle(ServerLevel level, int x, int y, int z) {
        prepareLot(level, x - 16, z - 16, x + 16, z + 16, y, Blocks.MOSS_BLOCK);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2.0 / 12.0;
            int px = x + (int) Math.round(Math.cos(a) * 12);
            int pz = z + (int) Math.round(Math.sin(a) * 12);
            set(level, px, y + 1, pz, Blocks.OAK_LOG);
            set(level, px, y + 2, pz, Blocks.LANTERN);
        }
        set(level, x, y + 1, z, Blocks.LECTERN);
    }

    private static void terraces(ServerLevel level, int cx, int y, int cz) {
        for (int ring = 96; ring >= 36; ring -= 20) {
            int ty = y + (96 - ring) / 20 * 3;
            for (int x = cx - ring; x <= cx + ring; x++) {
                set(level, x, ty, cz - ring, Blocks.DEEPSLATE_BRICKS);
                set(level, x, ty, cz + ring, Blocks.DEEPSLATE_BRICKS);
            }
            for (int z = cz - ring; z <= cz + ring; z++) {
                set(level, cx - ring, ty, z, Blocks.DEEPSLATE_BRICKS);
                set(level, cx + ring, ty, z, Blocks.DEEPSLATE_BRICKS);
            }
        }
    }

    private static void dwarvenGate(ServerLevel level, int x, int y, int z) {
        prepareLot(level, x - 18, z - 8, x + 18, z + 18, y, Blocks.POLISHED_DEEPSLATE);
        for (int dx = -16; dx <= 16; dx++) {
            int height = 12 + Math.max(0, 8 - Math.abs(dx) / 2);
            for (int dy = 1; dy <= height; dy++) set(level, x + dx, y + dy, z, Blocks.DEEPSLATE_BRICKS);
        }
        clear(level, x - 5, y + 1, z, x + 5, y + 11, z + 16);
        fill(level, x - 5, y, z, x + 5, y, z + 16, Blocks.POLISHED_DEEPSLATE);
        set(level, x - 7, y + 7, z + 2, Blocks.LANTERN);
        set(level, x + 7, y + 7, z + 2, Blocks.LANTERN);
    }

    private static void forge(ServerLevel level, int x, int y, int z, Palette p) {
        hall(level, x, y, z, 30, 20, 8, p);
        fill(level, x + 4, y + 1, z + 5, x + 25, y + 1, z + 14, Blocks.IRON_BLOCK);
        set(level, x + 7, y + 2, z + 9, Blocks.BLAST_FURNACE);
        set(level, x + 14, y + 2, z + 9, Blocks.ANVIL);
        set(level, x + 22, y + 2, z + 9, Blocks.LAVA);
        for (int dy = 1; dy <= 11; dy++) set(level, x + 26, y + dy, z + 16, Blocks.RAW_COPPER_BLOCK);
    }

    private static void quarter(ServerLevel level, int x, int y, int z, Palette p) {
        for (int i = 0; i < 4; i++) hall(level, x + (i % 2) * 22, y + (i / 2) * 2,
                z + (i / 2) * 18, 18, 14, 7, p);
    }

    private static void vault(ServerLevel level, int x, int y, int z, Palette p) {
        hall(level, x, y, z, 24, 18, 8, p);
        fill(level, x + 6, y + 1, z + 5, x + 18, y + 6, z + 13, Blocks.IRON_BARS);
        set(level, x + 12, y + 2, z + 9, Blocks.CHEST);
    }

    private static void stoneRoad(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            for (int side = -3; side <= 3; side++) set(level, x + side, y, z,
                    Math.abs(side) == 3 ? Blocks.DEEPSLATE_BRICKS : Blocks.POLISHED_ANDESITE);
            clear(level, x - 3, y + 1, z, x + 3, y + 4, z);
        }
    }

    private static void mine(ServerLevel level, int x, int y, int z) {
        prepareLot(level, x - 12, z - 6, x + 12, z + 20, y, Blocks.STONE);
        for (int dx = -10; dx <= 10; dx++) for (int dy = 1; dy <= 12; dy++) {
            if (Math.abs(dx) >= 7 || dy >= 10) set(level, x + dx, y + dy, z, Blocks.DEEPSLATE_BRICKS);
        }
        clear(level, x - 5, y + 1, z, x + 5, y + 8, z + 18);
        fill(level, x - 3, y, z, x + 3, y, z + 22, Blocks.IRON_BLOCK);
    }

    private static void oreYard(ServerLevel level, int x, int y, int z) {
        prepareLot(level, x - 18, z - 15, x + 18, z + 15, y, Blocks.POLISHED_ANDESITE);
        for (int[] p : new int[][]{{-10, -7}, {0, -7}, {10, -7}, {-5, 6}, {7, 6}}) {
            fill(level, x + p[0] - 2, y + 1, z + p[1] - 2, x + p[0] + 2, y + 4, z + p[1] + 2,
                    (p[0] + p[1]) % 2 == 0 ? Blocks.RAW_IRON_BLOCK : Blocks.RAW_COPPER_BLOCK);
        }
    }

    private static void road(ServerLevel level, int x1, int z1, int x2, int z2, int y, int halfWidth,
                             Block center, Block edge) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        boolean xMajor = Math.abs(x2 - x1) >= Math.abs(z2 - z1);
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            for (int side = -halfWidth; side <= halfWidth; side++) {
                int px = xMajor ? x : x + side;
                int pz = xMajor ? z + side : z;
                prepareColumn(level, px, pz, y, Math.abs(side) == halfWidth ? edge : center, Blocks.DIRT, 5);
            }
        }
    }

    private static void grade(ServerLevel level, int x1, int z1, int x2, int z2, int y,
                              Block surface, Block filler, int clearHeight) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                prepareColumn(level, x, z, y, surface, filler, clearHeight);
            }
        }
    }

    private static void gradeEllipse(ServerLevel level, int cx, int cz, int rx, int rz, int y,
                                     Block surface, Block filler) {
        for (int x = cx - rx; x <= cx + rx; x++) for (int z = cz - rz; z <= cz + rz; z++) {
            double nx = (x - cx) / (double) rx;
            double nz = (z - cz) / (double) rz;
            if (nx * nx + nz * nz <= 1.0) prepareColumn(level, x, z, y, surface, filler, 16);
        }
    }

    private static void slopeShoulder(ServerLevel level, int x1, int z1, int x2, int z2, int y, Block surface) {
        for (int ring = 1; ring <= 14; ring++) {
            int target = y - Math.min(7, (ring + 1) / 2);
            for (int x = x1 - ring; x <= x2 + ring; x++) {
                prepareColumn(level, x, z1 - ring, target, surface, Blocks.DIRT, 8);
                prepareColumn(level, x, z2 + ring, target, surface, Blocks.DIRT, 8);
            }
            for (int z = z1 - ring; z <= z2 + ring; z++) {
                prepareColumn(level, x1 - ring, z, target, surface, Blocks.DIRT, 8);
                prepareColumn(level, x2 + ring, z, target, surface, Blocks.DIRT, 8);
            }
        }
    }

    private static void prepareLot(ServerLevel level, int x1, int z1, int x2, int z2, int y, Block surface) {
        grade(level, x1, z1, x2, z2, y, surface, Blocks.DIRT, 24);
        for (int ring = 1; ring <= 5; ring++) {
            int target = y - (ring + 1) / 2;
            for (int x = x1 - ring; x <= x2 + ring; x++) {
                prepareColumn(level, x, z1 - ring, target, surface, Blocks.DIRT, 8);
                prepareColumn(level, x, z2 + ring, target, surface, Blocks.DIRT, 8);
            }
            for (int z = z1 - ring; z <= z2 + ring; z++) {
                prepareColumn(level, x1 - ring, z, target, surface, Blocks.DIRT, 8);
                prepareColumn(level, x2 + ring, z, target, surface, Blocks.DIRT, 8);
            }
        }
    }

    private static void prepareColumn(ServerLevel level, int x, int z, int targetY, Block surface,
                                      Block filler, int clearHeight) {
        int oldY = RealmSitePlanner.surfaceY(level, x, z);
        if (oldY < targetY) fill(level, x, oldY + 1, z, x, targetY - 1, z, filler);
        if (oldY > targetY) clear(level, x, targetY + 1, z, x, oldY + 1, z);
        set(level, x, targetY, z, surface);
        clear(level, x, targetY + 1, z, x, targetY + clearHeight, z);
    }

    private static void tree(ServerLevel level, int x, int y, int z, Block log, Block leaves, int height) {
        prepareColumn(level, x, z, y, Blocks.GRASS_BLOCK, Blocks.DIRT, 2);
        for (int dy = 1; dy <= height; dy++) set(level, x, y + dy, z, log);
        int crown = y + height;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = -1; dy <= 2; dy++) {
            if (dx * dx + dz * dz + dy * dy <= 7) set(level, x + dx, crown + dy, z + dz, leaves);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void clear(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2) {
        fill(level, x1, y1, z1, x2, y2, z2, Blocks.AIR);
    }

    private static void fill(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        if (y2 < y1) return;
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(level, x, y, z, block);
            }
        }
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        if (y < level.getMinY() || y >= level.getMaxY()) return;
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 3);
    }

    private record Palette(Block foundation, Block frame, Block wall, Block roof, Block floor) {
    }
}
