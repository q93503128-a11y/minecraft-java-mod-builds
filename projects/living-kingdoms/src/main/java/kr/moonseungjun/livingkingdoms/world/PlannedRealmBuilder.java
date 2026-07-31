package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Creates ordered edit plans for complete, distinct regional capitals without writing blocks immediately. */
public final class PlannedRealmBuilder {
    private PlannedRealmBuilder() {
    }

    public static IncrementalWorldEditPlan create(ServerLevel level, String homelandId,
                                                   RealmSiteLayoutSavedData.RealmSite site) {
        IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan();
        switch (homelandId) {
            case "silvana_forest" -> silvana(plan, level, site);
            case "kardum_league" -> kardum(plan, level, site);
            default -> erden(plan, level, site);
        }
        return plan;
    }

    private static void erden(IncrementalWorldEditPlan p, ServerLevel level,
                              RealmSiteLayoutSavedData.RealmSite site) {
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = clamp(site.baseY(), 68, 112);
        Style town = new Style(Blocks.STONE_BRICKS, Blocks.STRIPPED_SPRUCE_LOG,
                Blocks.BIRCH_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_PLANKS);

        grade(p, level, cx - 124, cz - 104, cx + 124, cz + 104, y, Blocks.GRASS_BLOCK, Blocks.DIRT);
        edgeBlend(p, level, cx - 124, cz - 104, cx + 124, cz + 104, y);
        walls(p, cx, y, cz, 120, 100, town);
        road(p, level, cx, cz - 100, cx, cz + 100, y, 4, Blocks.PACKED_MUD, Blocks.STONE_BRICKS);
        road(p, level, cx - 120, cz, cx + 120, cz, y, 4, Blocks.PACKED_MUD, Blocks.STONE_BRICKS);
        road(p, level, cx - 92, cz - 54, cx + 92, cz - 54, y, 3, Blocks.GRAVEL, Blocks.COBBLESTONE);
        road(p, level, cx - 92, cz + 58, cx + 92, cz + 58, y, 3, Blocks.GRAVEL, Blocks.COBBLESTONE);

        keep(p, level, cx - 34, y, cz - 91, town);
        building(p, level, cx + 18, y, cz - 82, 28, 18, 7, town);
        temple(p, level, cx - 74, y, cz - 78);
        market(p, level, cx, y, cz);
        inn(p, level, cx + 47, y, cz + 14, town);
        guild(p, level, cx - 77, y, cz + 17, town);
        smithy(p, level, cx + 78, y, cz - 35, town);
        barracks(p, level, cx - 106, y, cz - 39, town);
        granary(p, level, cx + 82, y, cz + 72, town);
        stable(p, level, cx - 98, y, cz + 71);

        int[][] homes = {
                {-56, -38}, {-34, -38}, {-11, -38}, {18, -38}, {46, -39},
                {-56, 31}, {-32, 32}, {-8, 32}, {20, 31}, {49, 34},
                {-111, 22}, {-109, 49}, {95, 21}, {98, 48},
                {-59, 78}, {-32, 78}, {-5, 79}, {27, 78}, {53, 78}
        };
        for (int i = 0; i < homes.length; i++) {
            building(p, level, cx + homes[i][0], y, cz + homes[i][1],
                    10 + i % 3, 8 + (i + 1) % 3, 5, town);
        }

        canal(p, cx - 164, y, cz - 142, cz + 142);
        bridge(p, cx - 172, y + 1, cz, cx - 118, y + 1, cz);
        road(p, level, cx - 164, cz, cx - 120, cz, y + 1, 3, Blocks.GRAVEL, Blocks.COBBLESTONE);
        dock(p, cx - 166, y, cz + 67);
        farm(p, level, cx + 146, y, cz + 57, 34, 27);
        farm(p, level, cx + 146, y, cz - 18, 34, 27);
        farm(p, level, cx - 196, y, cz + 78, 32, 25);
        orchard(p, level, cx + 150, y, cz - 88, 6, 5, Blocks.OAK_LOG, Blocks.OAK_LEAVES);
        outerTrees(p, level, cx, cz, 146, 28, Blocks.OAK_LOG, Blocks.OAK_LEAVES);

        cottage(p, level, cx + 170, y, cz + 100, Blocks.STRIPPED_OAK_LOG, Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS);
        cottage(p, level, cx - 176, y, cz + 110, Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS);
        residencePier(p, cx - 172, y, cz + 116);
        camp(p, level, cx + 133, y, cz - 167);
    }

    private static void silvana(IncrementalWorldEditPlan p, ServerLevel level,
                                RealmSiteLayoutSavedData.RealmSite site) {
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = clamp(site.baseY(), 70, 122);
        gradeEllipse(p, level, cx, cz, 92, 78, y, Blocks.MOSS_BLOCK, Blocks.DIRT);
        giantTree(p, cx, y, cz, 6, 27);
        platform(p, level, cx, y + 23, cz, 18);
        elvenLodge(p, level, cx - 58, y + 2, cz - 30, 11);
        elvenLodge(p, level, cx + 56, y + 1, cz - 21, 10);
        elvenLodge(p, level, cx - 39, y + 1, cz + 55, 10);
        elvenLodge(p, level, cx + 46, y + 2, cz + 58, 12);
        platform(p, level, cx - 58, y + 16, cz - 30, 10);
        platform(p, level, cx + 56, y + 15, cz - 21, 9);
        canopyBridge(p, cx, y + 23, cz, cx - 58, y + 16, cz - 30);
        canopyBridge(p, cx, y + 23, cz, cx + 56, y + 15, cz - 21);
        moonGarden(p, level, cx + 86, y, cz + 80);
        council(p, level, cx - 82, y, cz + 72);
        road(p, level, cx, cz, cx + 86, cz + 80, y, 2, Blocks.ROOTED_DIRT, Blocks.MOSS_BLOCK);
        road(p, level, cx, cz, cx - 82, cz + 72, y, 2, Blocks.ROOTED_DIRT, Blocks.MOSS_BLOCK);
        road(p, level, cx, cz, cx, cz - 105, y, 2, Blocks.ROOTED_DIRT, Blocks.MOSS_BLOCK);
        outerTrees(p, level, cx, cz, 108, 64, Blocks.DARK_OAK_LOG, Blocks.FLOWERING_AZALEA_LEAVES);
        cottage(p, level, cx + 82, y, cz + 82, Blocks.STRIPPED_BIRCH_LOG,
                Blocks.BIRCH_PLANKS, Blocks.FLOWERING_AZALEA_LEAVES);
    }

    private static void kardum(IncrementalWorldEditPlan p, ServerLevel level,
                               RealmSiteLayoutSavedData.RealmSite site) {
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = clamp(site.baseY(), 74, 138);
        Style stone = new Style(Blocks.DEEPSLATE_BRICKS, Blocks.POLISHED_BASALT,
                Blocks.POLISHED_ANDESITE, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE);
        grade(p, level, cx - 108, cz - 98, cx + 108, cz + 102, y, Blocks.STONE, Blocks.STONE);
        terraces(p, cx, y, cz);
        dwarvenGate(p, level, cx, y, cz - 88);
        building(p, level, cx - 38, y + 6, cz - 34, 27, 18, 8, stone);
        building(p, level, cx + 19, y + 9, cz - 20, 29, 19, 9, stone);
        forge(p, level, cx - 16, y + 3, cz + 42, stone);
        quarter(p, level, cx - 82, y, cz + 34, stone);
        quarter(p, level, cx + 64, y + 3, cz + 38, stone);
        vault(p, level, cx + 58, y + 10, cz - 70, stone);
        stoneRoad(p, cx, y, cz - 88, cx, y + 6, cz + 80);
        stoneRoad(p, cx - 94, y, cz + 34, cx + 94, y + 3, cz + 34);
        mine(p, level, cx - 105, y + 2, cz - 58);
        oreYard(p, level, cx + 94, y + 1, cz + 72);
        stoneRoom(p, level, cx - 78, y + 1, cz + 38);
        stoneRoom(p, level, cx - 10, y + 1, cz - 77);
    }

    private static void keep(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z, Style style) {
        building(p, level, x, y, z, 33, 24, 9, style);
        tower(p, level, x - 4, y, z - 4, 8, 15, style);
        tower(p, level, x + 29, y, z - 4, 8, 15, style);
        tower(p, level, x - 4, y, z + 20, 8, 15, style);
        tower(p, level, x + 29, y, z + 20, 8, 15, style);
    }

    private static void building(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z,
                                 int width, int depth, int height, Style style) {
        lot(p, level, x - 3, z - 3, x + width + 2, z + depth + 2, y, style.foundation());
        fill(p, x, y, z, x + width - 1, y, z + depth - 1, style.floor());
        for (int dx = 0; dx < width; dx++) for (int dz = 0; dz < depth; dz++) {
            if (dx != 0 && dx != width - 1 && dz != 0 && dz != depth - 1) continue;
            for (int dy = 1; dy <= height; dy++) {
                boolean beam = ((dx == 0 || dx == width - 1) && (dz == 0 || dz == depth - 1))
                        || dy == 1 || dy == height || dx % 5 == 0;
                set(p, x + dx, y + dy, z + dz, beam ? style.frame() : style.wall());
            }
        }
        clear(p, x + width / 2 - 1, y + 1, z, x + width / 2 + 1, y + 4, z);
        roof(p, x - 2, y + height + 1, z - 2, width + 4, depth + 4, style.roof());
        set(p, x + width / 2, y + 3, z + depth - 3, Blocks.LANTERN);
    }

    private static void roof(IncrementalWorldEditPlan p, int x, int y, int z, int width, int depth, Block block) {
        int layers = Math.max(3, Math.min(6, depth / 2));
        for (int layer = 0; layer < layers; layer++) {
            fill(p, x, y + layer, z + layer, x + width - 1, y + layer, z + layer, block);
            fill(p, x, y + layer, z + depth - 1 - layer,
                    x + width - 1, y + layer, z + depth - 1 - layer, block);
        }
        if (z + layers <= z + depth - 1 - layers) {
            fill(p, x, y + layers, z + layers, x + width - 1, y + layers,
                    z + depth - 1 - layers, block);
        }
    }

    private static void temple(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z) {
        Style temple = new Style(Blocks.SMOOTH_STONE, Blocks.CHISELED_STONE_BRICKS,
                Blocks.CALCITE, Blocks.STONE_BRICKS, Blocks.SMOOTH_STONE);
        building(p, level, x, y, z, 22, 17, 7, temple);
        set(p, x + 11, y + 2, z + 12, Blocks.BELL);
    }

    private static void market(IncrementalWorldEditPlan p, ServerLevel level, int cx, int y, int cz) {
        lot(p, level, cx - 25, cz - 21, cx + 25, cz + 21, y, Blocks.STONE_BRICKS);
        for (int[] offset : new int[][]{{-20, -14}, {-7, -14}, {7, -14}, {-20, 9}, {-7, 9}, {7, 9}}) {
            int x = cx + offset[0];
            int z = cz + offset[1];
            fill(p, x, y + 1, z, x + 10, y + 1, z + 5, Blocks.SPRUCE_PLANKS);
            for (int postX : new int[]{x, x + 10}) for (int py = 2; py <= 5; py++) {
                set(p, postX, y + py, z, Blocks.OAK_FENCE);
            }
            fill(p, x, y + 6, z - 1, x + 10, y + 6, z + 6, Blocks.DARK_OAK_SLAB);
            set(p, x + 5, y + 2, z + 3, Blocks.BARREL);
        }
        for (int dy = 1; dy <= 6; dy++) set(p, cx, y + dy, cz, Blocks.CHISELED_STONE_BRICKS);
        set(p, cx, y + 7, cz, Blocks.LANTERN);
    }

    private static void inn(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z, Style style) {
        building(p, level, x, y, z, 21, 14, 6, style);
        set(p, x + 5, y + 2, z + 5, Blocks.BARREL);
        set(p, x + 10, y + 2, z + 7, Blocks.CAMPFIRE);
        set(p, x + 16, y + 2, z + 5, Blocks.CHEST);
    }

    private static void guild(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z, Style style) {
        building(p, level, x, y, z, 23, 15, 7, style);
        set(p, x + 5, y + 2, z + 5, Blocks.CARTOGRAPHY_TABLE);
        set(p, x + 10, y + 2, z + 5, Blocks.LECTERN);
        set(p, x + 15, y + 2, z + 5, Blocks.FLETCHING_TABLE);
    }

    private static void smithy(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z, Style style) {
        building(p, level, x, y, z, 16, 11, 6, style);
        lot(p, level, x + 16, z, x + 28, z + 12, y, Blocks.STONE_BRICKS);
        set(p, x + 20, y + 1, z + 5, Blocks.BLAST_FURNACE);
        set(p, x + 23, y + 1, z + 5, Blocks.ANVIL);
        set(p, x + 26, y + 1, z + 8, Blocks.LAVA);
    }

    private static void barracks(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z, Style style) {
        building(p, level, x, y, z, 29, 18, 7, style);
        fill(p, x + 20, y + 1, z + 3, x + 26, y + 5, z + 14, Blocks.IRON_BARS);
        clear(p, x + 23, y + 1, z + 3, x + 23, y + 3, z + 3);
    }

    private static void granary(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z, Style style) {
        building(p, level, x, y, z, 19, 12, 6, style);
        for (int dx = 3; dx <= 15; dx += 4) set(p, x + dx, y + 2, z + 7, Blocks.BARREL);
    }

    private static void stable(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z) {
        lot(p, level, x - 2, z - 2, x + 24, z + 16, y, Blocks.COARSE_DIRT);
        for (int dx = 0; dx <= 22; dx++) {
            set(p, x + dx, y + 1, z, Blocks.OAK_FENCE);
            set(p, x + dx, y + 1, z + 14, Blocks.OAK_FENCE);
        }
        fill(p, x, y + 5, z, x + 22, y + 5, z + 14, Blocks.SPRUCE_SLAB);
        for (int dx = 2; dx <= 20; dx += 6) set(p, x + dx, y + 1, z + 7, Blocks.HAY_BLOCK);
    }

    private static void walls(IncrementalWorldEditPlan p, int cx, int y, int cz, int rx, int rz, Style style) {
        for (int x = cx - rx; x <= cx + rx; x++) {
            wallColumn(p, x, y, cz - rz, style.foundation());
            wallColumn(p, x, y, cz + rz, style.foundation());
        }
        for (int z = cz - rz; z <= cz + rz; z++) {
            wallColumn(p, cx - rx, y, z, style.foundation());
            wallColumn(p, cx + rx, y, z, style.foundation());
        }
        for (int[] corner : new int[][]{{-rx, -rz}, {rx, -rz}, {-rx, rz}, {rx, rz}}) {
            tower(p, null, cx + corner[0] - 4, y, cz + corner[1] - 4, 9, 13, style);
        }
        gate(p, cx, y, cz - rz, true, style);
        gate(p, cx, y, cz + rz, true, style);
        gate(p, cx - rx, y, cz, false, style);
        gate(p, cx + rx, y, cz, false, style);
    }

    private static void wallColumn(IncrementalWorldEditPlan p, int x, int y, int z, Block block) {
        fill(p, x, y + 1, z, x, y + 6, z, block);
        if (Math.floorMod(x + z, 2) == 0) set(p, x, y + 7, z, Blocks.STONE_BRICK_WALL);
    }

    private static void gate(IncrementalWorldEditPlan p, int x, int y, int z, boolean northSouth, Style style) {
        int x1 = northSouth ? x - 10 : x - 5;
        int z1 = northSouth ? z - 5 : z - 10;
        int x2 = northSouth ? x + 10 : x + 5;
        int z2 = northSouth ? z + 5 : z + 10;
        for (int px = x1; px <= x2; px++) for (int pz = z1; pz <= z2; pz++) {
            if (px != x1 && px != x2 && pz != z1 && pz != z2) continue;
            fill(p, px, y + 1, pz, px, y + 9, pz, style.foundation());
        }
        if (northSouth) clear(p, x - 4, y + 1, z1, x + 4, y + 6, z2);
        else clear(p, x1, y + 1, z - 4, x2, y + 6, z + 4);
        fill(p, x1 - 1, y + 10, z1 - 1, x2 + 1, y + 10, z2 + 1, style.roof());
        set(p, x, y + 9, z, Blocks.BELL);
    }

    private static void tower(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z,
                              int size, int height, Style style) {
        if (level != null) lot(p, level, x - 1, z - 1, x + size, z + size, y, style.foundation());
        for (int dx = 0; dx < size; dx++) for (int dz = 0; dz < size; dz++) {
            if (dx != 0 && dx != size - 1 && dz != 0 && dz != size - 1) continue;
            fill(p, x + dx, y + 1, z + dz, x + dx, y + height, z + dz, style.foundation());
        }
        fill(p, x - 1, y + height + 1, z - 1, x + size, y + height + 1, z + size, style.roof());
        clear(p, x + size / 2, y + 1, z, x + size / 2, y + 3, z);
    }

    private static void canal(IncrementalWorldEditPlan p, int x, int y, int z1, int z2) {
        for (int z = z1; z <= z2; z++) {
            int bend = (int) Math.round(Math.sin(z * 0.045) * 6.0);
            for (int dx = -8; dx <= 8; dx++) {
                int px = x + bend + dx;
                clear(p, px, y - 1, z, px, y + 6, z);
                fill(p, px, y - 4, z, px, y - 1, z, Blocks.WATER);
                set(p, px, y - 5, z, Math.abs(dx) > 5 ? Blocks.CLAY : Blocks.GRAVEL);
            }
        }
    }

    private static void bridge(IncrementalWorldEditPlan p, int x1, int y1, int z1, int x2, int y2, int z2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : i / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            fill(p, x, y, z - 3, x, y, z + 3, Blocks.STONE_BRICKS);
            set(p, x, y + 1, z - 4, Blocks.STONE_BRICK_WALL);
            set(p, x, y + 1, z + 4, Blocks.STONE_BRICK_WALL);
        }
    }

    private static void dock(IncrementalWorldEditPlan p, int x, int y, int z) {
        for (int dx = -4; dx <= 18; dx++) {
            fill(p, x + dx, y, z - 4, x + dx, y, z + 4, Blocks.SPRUCE_PLANKS);
            if (dx % 5 == 0) {
                fill(p, x + dx, y - 3, z - 4, x + dx, y, z - 4, Blocks.SPRUCE_LOG);
                fill(p, x + dx, y - 3, z + 4, x + dx, y, z + 4, Blocks.SPRUCE_LOG);
            }
        }
        set(p, x + 8, y + 1, z, Blocks.BARREL);
    }

    private static void farm(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z, int width, int depth) {
        lot(p, level, x - 2, z - 2, x + width + 1, z + depth + 1, y, Blocks.GRASS_BLOCK);
        for (int dx = 0; dx < width; dx++) for (int dz = 0; dz < depth; dz++) {
            if (dx % 7 == 0) set(p, x + dx, y, z + dz, Blocks.WATER);
            else {
                set(p, x + dx, y, z + dz, Blocks.FARMLAND);
                if ((dx + dz) % 4 != 0) set(p, x + dx, y + 1, z + dz, Blocks.WHEAT);
            }
        }
        for (int dx = -1; dx <= width; dx++) {
            set(p, x + dx, y + 1, z - 1, Blocks.OAK_FENCE);
            set(p, x + dx, y + 1, z + depth, Blocks.OAK_FENCE);
        }
    }

    private static void orchard(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z,
                                int columns, int rows, Block log, Block leaves) {
        for (int col = 0; col < columns; col++) for (int row = 0; row < rows; row++) {
            tree(p, level, x + col * 8, y, z + row * 8, log, leaves, 5 + (col + row) % 2);
        }
    }

    private static void outerTrees(IncrementalWorldEditPlan p, ServerLevel level, int cx, int cz,
                                   int radius, int count, Block log, Block leaves) {
        for (int i = 0; i < count; i++) {
            double angle = i * 2.399963229728653;
            int r = radius + (i % 5) * 10;
            int x = cx + (int) Math.round(Math.cos(angle) * r);
            int z = cz + (int) Math.round(Math.sin(angle) * r);
            tree(p, level, x, RealmSitePlanner.surfaceY(level, x, z), z, log, leaves, 5 + i % 4);
        }
    }

    private static void cottage(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z,
                                Block frame, Block wall, Block roofBlock) {
        Style cottage = new Style(Blocks.STONE_BRICKS, frame, wall, roofBlock, Blocks.SPRUCE_PLANKS);
        building(p, level, x, y, z, 11, 10, 5, cottage);
        set(p, x + 3, y + 2, z + 7, Blocks.BARREL);
    }

    private static void residencePier(IncrementalWorldEditPlan p, int x, int y, int z) {
        for (int dz = 0; dz <= 22; dz++) {
            fill(p, x - 2, y, z + dz, x + 2, y, z + dz, Blocks.SPRUCE_PLANKS);
            if (dz % 5 == 0) {
                fill(p, x - 2, y - 3, z + dz, x - 2, y, z + dz, Blocks.SPRUCE_LOG);
                fill(p, x + 2, y - 3, z + dz, x + 2, y, z + dz, Blocks.SPRUCE_LOG);
            }
        }
    }

    private static void camp(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z) {
        lot(p, level, x - 9, z - 9, x + 9, z + 9, y, Blocks.COARSE_DIRT);
        set(p, x, y + 1, z, Blocks.CAMPFIRE);
        for (int[] post : new int[][]{{-6, -5}, {5, -5}, {-6, 5}, {5, 5}}) {
            fill(p, x + post[0], y + 1, z + post[1], x + post[0], y + 4, z + post[1], Blocks.SPRUCE_FENCE);
        }
        fill(p, x - 7, y + 5, z - 6, x + 6, y + 5, z + 6, Blocks.DARK_OAK_SLAB);
        set(p, x - 3, y + 1, z + 2, Blocks.CHEST);
        set(p, x + 3, y + 1, z + 2, Blocks.HAY_BLOCK);
    }

    private static void giantTree(IncrementalWorldEditPlan p, int x, int y, int z, int radius, int height) {
        for (int dy = 0; dy <= height; dy++) {
            int r = Math.max(2, radius - dy / 8);
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz <= r * r) set(p, x + dx, y + dy, z + dz, Blocks.DARK_OAK_LOG);
            }
        }
        int crown = y + height;
        for (int dx = -14; dx <= 14; dx++) for (int dz = -14; dz <= 14; dz++) for (int dy = -4; dy <= 6; dy++) {
            if (dx * dx + dz * dz + dy * dy * 2 <= 190) set(p, x + dx, crown + dy, z + dz, Blocks.AZALEA_LEAVES);
        }
    }

    private static void platform(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            if (dx * dx + dz * dz <= radius * radius) set(p, x + dx, y, z + dz, Blocks.STRIPPED_BIRCH_WOOD);
        }
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI * 2.0 / 16.0;
            int px = x + (int) Math.round(Math.cos(angle) * (radius - 1));
            int pz = z + (int) Math.round(Math.sin(angle) * (radius - 1));
            set(p, px, y + 1, pz, Blocks.OAK_FENCE);
            if (i % 4 == 0) set(p, px, y + 2, pz, Blocks.LANTERN);
        }
        int ground = RealmSitePlanner.surfaceY(level, x, z);
        fill(p, x, ground + 1, z, x, y, z, Blocks.DARK_OAK_LOG);
    }

    private static void elvenLodge(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z, int radius) {
        lot(p, level, x - radius - 2, z - radius - 2, x + radius + 2, z + radius + 2, y, Blocks.MOSS_BLOCK);
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            int distance = dx * dx + dz * dz;
            if (distance <= radius * radius) set(p, x + dx, y, z + dz, Blocks.STRIPPED_BIRCH_WOOD);
            if (distance >= (radius - 2) * (radius - 2) && distance <= radius * radius) {
                fill(p, x + dx, y + 1, z + dz, x + dx, y + 5, z + dz, Blocks.BIRCH_PLANKS);
            }
        }
        for (int dy = 6; dy <= 10; dy++) {
            int r = Math.max(2, radius - (dy - 5) * 2);
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz <= r * r) set(p, x + dx, y + dy, z + dz, Blocks.FLOWERING_AZALEA_LEAVES);
            }
        }
        clear(p, x - 1, y + 1, z - radius, x + 1, y + 3, z - radius);
        set(p, x, y + 3, z, Blocks.SOUL_LANTERN);
    }

    private static void canopyBridge(IncrementalWorldEditPlan p, int x1, int y1, int z1, int x2, int y2, int z2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : i / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            fill(p, x - 1, y, z, x + 1, y, z, Blocks.DARK_OAK_PLANKS);
            if (i % 4 == 0) {
                set(p, x - 2, y + 1, z, Blocks.OAK_FENCE);
                set(p, x + 2, y + 1, z, Blocks.OAK_FENCE);
            }
        }
    }

    private static void moonGarden(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z) {
        lot(p, level, x - 14, z - 14, x + 14, z + 14, y, Blocks.MOSS_BLOCK);
        for (int dx = -10; dx <= 10; dx++) for (int dz = -10; dz <= 10; dz++) {
            int distance = dx * dx + dz * dz;
            if (distance <= 64) set(p, x + dx, y, z + dz, Blocks.WATER);
            else if (distance <= 100) set(p, x + dx, y, z + dz, Blocks.MOSSY_STONE_BRICKS);
        }
        set(p, x, y - 1, z, Blocks.SEA_LANTERN);
    }

    private static void council(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z) {
        lot(p, level, x - 16, z - 16, x + 16, z + 16, y, Blocks.MOSS_BLOCK);
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI * 2.0 / 12.0;
            int px = x + (int) Math.round(Math.cos(angle) * 12);
            int pz = z + (int) Math.round(Math.sin(angle) * 12);
            set(p, px, y + 1, pz, Blocks.OAK_LOG);
            set(p, px, y + 2, pz, Blocks.LANTERN);
        }
        set(p, x, y + 1, z, Blocks.LECTERN);
    }

    private static void terraces(IncrementalWorldEditPlan p, int cx, int y, int cz) {
        for (int ring = 94; ring >= 36; ring -= 20) {
            int terraceY = y + (94 - ring) / 20 * 3;
            fill(p, cx - ring, terraceY, cz - ring, cx + ring, terraceY, cz - ring, Blocks.DEEPSLATE_BRICKS);
            fill(p, cx - ring, terraceY, cz + ring, cx + ring, terraceY, cz + ring, Blocks.DEEPSLATE_BRICKS);
            fill(p, cx - ring, terraceY, cz - ring, cx - ring, terraceY, cz + ring, Blocks.DEEPSLATE_BRICKS);
            fill(p, cx + ring, terraceY, cz - ring, cx + ring, terraceY, cz + ring, Blocks.DEEPSLATE_BRICKS);
        }
    }

    private static void dwarvenGate(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z) {
        lot(p, level, x - 18, z - 8, x + 18, z + 18, y, Blocks.POLISHED_DEEPSLATE);
        for (int dx = -16; dx <= 16; dx++) {
            int height = 12 + Math.max(0, 8 - Math.abs(dx) / 2);
            fill(p, x + dx, y + 1, z, x + dx, y + height, z, Blocks.DEEPSLATE_BRICKS);
        }
        clear(p, x - 5, y + 1, z, x + 5, y + 11, z + 16);
        fill(p, x - 5, y, z, x + 5, y, z + 16, Blocks.POLISHED_DEEPSLATE);
        set(p, x - 7, y + 7, z + 2, Blocks.LANTERN);
        set(p, x + 7, y + 7, z + 2, Blocks.LANTERN);
    }

    private static void forge(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z, Style style) {
        building(p, level, x, y, z, 30, 20, 8, style);
        fill(p, x + 4, y + 1, z + 5, x + 25, y + 1, z + 14, Blocks.IRON_BLOCK);
        set(p, x + 7, y + 2, z + 9, Blocks.BLAST_FURNACE);
        set(p, x + 14, y + 2, z + 9, Blocks.ANVIL);
        set(p, x + 22, y + 2, z + 9, Blocks.LAVA);
        fill(p, x + 26, y + 1, z + 16, x + 26, y + 11, z + 16, Blocks.RAW_COPPER_BLOCK);
    }

    private static void quarter(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z, Style style) {
        for (int i = 0; i < 4; i++) {
            building(p, level, x + (i % 2) * 22, y + (i / 2) * 2,
                    z + (i / 2) * 18, 18, 14, 7, style);
        }
    }

    private static void vault(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z, Style style) {
        building(p, level, x, y, z, 24, 18, 8, style);
        fill(p, x + 6, y + 1, z + 5, x + 18, y + 6, z + 13, Blocks.IRON_BARS);
        set(p, x + 12, y + 2, z + 9, Blocks.CHEST);
    }

    private static void stoneRoad(IncrementalWorldEditPlan p, int x1, int y1, int z1, int x2, int y2, int z2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : i / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            for (int side = -3; side <= 3; side++) {
                set(p, x + side, y, z, Math.abs(side) == 3 ? Blocks.DEEPSLATE_BRICKS : Blocks.POLISHED_ANDESITE);
            }
            clear(p, x - 3, y + 1, z, x + 3, y + 4, z);
        }
    }

    private static void mine(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z) {
        lot(p, level, x - 12, z - 6, x + 12, z + 20, y, Blocks.STONE);
        for (int dx = -10; dx <= 10; dx++) for (int dy = 1; dy <= 12; dy++) {
            if (Math.abs(dx) >= 7 || dy >= 10) set(p, x + dx, y + dy, z, Blocks.DEEPSLATE_BRICKS);
        }
        clear(p, x - 5, y + 1, z, x + 5, y + 8, z + 18);
        fill(p, x - 3, y, z, x + 3, y, z + 22, Blocks.IRON_BLOCK);
    }

    private static void oreYard(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z) {
        lot(p, level, x - 18, z - 15, x + 18, z + 15, y, Blocks.POLISHED_ANDESITE);
        for (int[] offset : new int[][]{{-10, -7}, {0, -7}, {10, -7}, {-5, 6}, {7, 6}}) {
            fill(p, x + offset[0] - 2, y + 1, z + offset[1] - 2,
                    x + offset[0] + 2, y + 4, z + offset[1] + 2,
                    (offset[0] + offset[1]) % 2 == 0 ? Blocks.RAW_IRON_BLOCK : Blocks.RAW_COPPER_BLOCK);
        }
    }

    private static void stoneRoom(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z) {
        Style style = new Style(Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS,
                Blocks.POLISHED_ANDESITE, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE);
        building(p, level, x, y, z, 13, 11, 6, style);
        set(p, x + 9, y + 2, z + 7, Blocks.LANTERN);
        set(p, x + 3, y + 2, z + 7, Blocks.BARREL);
    }

    private static void road(IncrementalWorldEditPlan p, ServerLevel level, int x1, int z1, int x2, int z2,
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
                column(p, level, px, pz, y, Math.abs(side) == halfWidth ? edge : center, Blocks.DIRT);
            }
        }
    }

    private static void grade(IncrementalWorldEditPlan p, ServerLevel level, int x1, int z1, int x2, int z2,
                              int y, Block surface, Block filler) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) column(p, level, x, z, y, surface, filler);
        }
    }

    private static void gradeEllipse(IncrementalWorldEditPlan p, ServerLevel level, int cx, int cz,
                                     int radiusX, int radiusZ, int y, Block surface, Block filler) {
        for (int x = cx - radiusX; x <= cx + radiusX; x++) for (int z = cz - radiusZ; z <= cz + radiusZ; z++) {
            double nx = (x - cx) / (double) radiusX;
            double nz = (z - cz) / (double) radiusZ;
            if (nx * nx + nz * nz <= 1.0) column(p, level, x, z, y, surface, filler);
        }
    }

    private static void edgeBlend(IncrementalWorldEditPlan p, ServerLevel level, int x1, int z1, int x2, int z2, int y) {
        for (int ring = 1; ring <= 12; ring++) {
            int target = y - Math.min(6, (ring + 1) / 2);
            for (int x = x1 - ring; x <= x2 + ring; x++) {
                column(p, level, x, z1 - ring, target, Blocks.GRASS_BLOCK, Blocks.DIRT);
                column(p, level, x, z2 + ring, target, Blocks.GRASS_BLOCK, Blocks.DIRT);
            }
            for (int z = z1 - ring; z <= z2 + ring; z++) {
                column(p, level, x1 - ring, z, target, Blocks.GRASS_BLOCK, Blocks.DIRT);
                column(p, level, x2 + ring, z, target, Blocks.GRASS_BLOCK, Blocks.DIRT);
            }
        }
    }

    private static void lot(IncrementalWorldEditPlan p, ServerLevel level, int x1, int z1, int x2, int z2,
                            int y, Block surface) {
        grade(p, level, x1, z1, x2, z2, y, surface, Blocks.DIRT);
        for (int ring = 1; ring <= 4; ring++) {
            int target = y - (ring + 1) / 2;
            for (int x = x1 - ring; x <= x2 + ring; x++) {
                column(p, level, x, z1 - ring, target, surface, Blocks.DIRT);
                column(p, level, x, z2 + ring, target, surface, Blocks.DIRT);
            }
            for (int z = z1 - ring; z <= z2 + ring; z++) {
                column(p, level, x1 - ring, z, target, surface, Blocks.DIRT);
                column(p, level, x2 + ring, z, target, surface, Blocks.DIRT);
            }
        }
    }

    private static void column(IncrementalWorldEditPlan p, ServerLevel level, int x, int z, int targetY,
                               Block surface, Block filler) {
        int oldY = RealmSitePlanner.surfaceY(level, x, z);
        if (oldY < targetY) fill(p, x, oldY + 1, z, x, targetY - 1, z, filler);
        if (oldY > targetY) clear(p, x, targetY + 1, z, x, oldY + 1, z);
        set(p, x, targetY, z, surface);
    }

    private static void tree(IncrementalWorldEditPlan p, ServerLevel level, int x, int y, int z,
                             Block log, Block leaves, int height) {
        column(p, level, x, z, y, Blocks.GRASS_BLOCK, Blocks.DIRT);
        fill(p, x, y + 1, z, x, y + height, z, log);
        int crown = y + height;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = -1; dy <= 2; dy++) {
            if (dx * dx + dz * dz + dy * dy <= 7) set(p, x + dx, crown + dy, z + dz, leaves);
        }
    }

    private static void fill(IncrementalWorldEditPlan p, int x1, int y1, int z1,
                             int x2, int y2, int z2, Block block) {
        p.addFill(x1, y1, z1, x2, y2, z2, block);
    }

    private static void clear(IncrementalWorldEditPlan p, int x1, int y1, int z1,
                              int x2, int y2, int z2) {
        p.addFill(x1, y1, z1, x2, y2, z2, Blocks.AIR);
    }

    private static void set(IncrementalWorldEditPlan p, int x, int y, int z, Block block) {
        p.addSet(x, y, z, block);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Style(Block foundation, Block frame, Block wall, Block roof, Block floor) {
    }
}
