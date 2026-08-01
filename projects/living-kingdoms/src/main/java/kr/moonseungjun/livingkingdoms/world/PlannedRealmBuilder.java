package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Produces authored regional capitals in strict phases: terrain, infrastructure, then structures.
 * Terrain operations are never scheduled after a building, so later lots cannot erase walls and
 * leave floating roofs. Every plateau also blends back to the generator surface over a broad rim.
 */
public final class PlannedRealmBuilder {
    private PlannedRealmBuilder() {
    }

    public static IncrementalWorldEditPlan create(ServerLevel level, String homelandId,
                                                   RealmSiteLayoutSavedData.RealmSite site) {
        IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan();
        switch (homelandId) {
            case "silvana_forest" -> buildSilvana(plan, level, site);
            case "kardum_league" -> buildKardum(plan, level, site);
            default -> buildErden(plan, level, site);
        }
        return plan;
    }

    private static void buildErden(IncrementalWorldEditPlan p, ServerLevel level,
                                   RealmSiteLayoutSavedData.RealmSite site) {
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = clamp(site.baseY(), 68, 104);
        Style town = new Style(Blocks.STONE_BRICKS, Blocks.STRIPPED_SPRUCE_LOG,
                Blocks.BIRCH_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_PLANKS);
        Style civic = new Style(Blocks.STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS,
                Blocks.CALCITE, Blocks.DEEPSLATE_TILES, Blocks.SMOOTH_STONE);

        // Phase 1: one coherent city terrace and four rounded outer estates.
        sculptPlateau(p, level, cx, cz, 132, 112, 52, y, Blocks.GRASS_BLOCK, Blocks.DIRT);
        prepareEstate(p, level, cx + 132, cz + 67, 35, 26, y, Blocks.GRASS_BLOCK);
        prepareEstate(p, level, cx + 132, cz - 26, 35, 26, y, Blocks.GRASS_BLOCK);
        prepareEstate(p, level, cx - 160, cz + 73, 32, 24, y, Blocks.GRASS_BLOCK);
        prepareEstate(p, level, cx + 112, cz - 136, 22, 18, y, Blocks.COARSE_DIRT);
        prepareEstate(p, level, cx - 146, cz + 91, 20, 18, y, Blocks.GRASS_BLOCK);

        // Phase 2: infrastructure only. No terrain grading occurs after this point.
        road(p, cx, cz - 108, cx, cz + 108, y, 4, Blocks.PACKED_MUD, Blocks.STONE_BRICKS);
        road(p, cx - 128, cz, cx + 128, cz, y, 4, Blocks.PACKED_MUD, Blocks.STONE_BRICKS);
        road(p, cx - 96, cz - 55, cx + 96, cz - 55, y, 3, Blocks.GRAVEL, Blocks.COBBLESTONE);
        road(p, cx - 96, cz + 57, cx + 96, cz + 57, y, 3, Blocks.GRAVEL, Blocks.COBBLESTONE);
        road(p, cx + 128, cz + 55, cx + 132, cz + 67, y, 2, Blocks.DIRT_PATH, Blocks.COBBLESTONE);
        road(p, cx - 128, cz + 55, cx - 146, cz + 91, y, 2, Blocks.DIRT_PATH, Blocks.COBBLESTONE);
        walls(p, cx, y, cz, 124, 104, town);
        canal(p, cx - 151, y, cz - 128, cz + 128);
        bridge(p, cx - 158, y + 1, cz, cx - 123, y + 1, cz);

        // Phase 3: buildings. Their footprints only place floors; they never re-grade terrain.
        keep(p, cx - 35, y, cz - 92, town);
        building(p, cx + 20, y, cz - 83, 28, 18, 7, civic);
        temple(p, cx - 75, y, cz - 79);
        market(p, cx, y, cz);
        buildingWithInterior(p, cx + 48, y, cz + 13, 22, 15, 6, town, Interior.INN);
        buildingWithInterior(p, cx - 78, y, cz + 16, 24, 16, 7, town, Interior.GUILD);
        buildingWithInterior(p, cx + 79, y, cz - 36, 18, 13, 6, town, Interior.SMITHY);
        buildingWithInterior(p, cx - 108, y, cz - 41, 30, 19, 7, town, Interior.BARRACKS);
        buildingWithInterior(p, cx + 82, y, cz + 72, 20, 13, 6, town, Interior.GRANARY);
        stable(p, cx - 101, y, cz + 69);

        int[][] homes = {
                {-58, -39}, {-35, -39}, {-11, -39}, {20, -39}, {48, -40},
                {-58, 31}, {-34, 32}, {-9, 32}, {20, 31}, {50, 33},
                {-113, 20}, {-112, 48}, {96, 20}, {98, 47},
                {-61, 77}, {-34, 78}, {-6, 78}, {26, 78}, {54, 77}
        };
        for (int i = 0; i < homes.length; i++) {
            int width = 11 + i % 3;
            int depth = 9 + (i + 1) % 3;
            house(p, cx + homes[i][0], y, cz + homes[i][1], width, depth, town, i);
        }

        field(p, cx + 132, y, cz + 67, 35, 26, 0);
        field(p, cx + 132, y, cz - 26, 35, 26, 1);
        field(p, cx - 160, y, cz + 73, 32, 24, 2);
        farmhouse(p, cx + 132, y, cz + 98, town);
        fishingHut(p, cx - 146, y, cz + 91, town);
        camp(p, cx + 112, y, cz - 136);
        dock(p, cx - 151, y, cz + 66);
        lamps(p, cx, y, cz);
    }

    private static void buildSilvana(IncrementalWorldEditPlan p, ServerLevel level,
                                     RealmSiteLayoutSavedData.RealmSite site) {
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = clamp(site.baseY(), 70, 112);

        sculptPlateau(p, level, cx, cz, 86, 72, 46, y, Blocks.MOSS_BLOCK, Blocks.DIRT);
        prepareEstate(p, level, cx + 70, cz + 68, 24, 22, y, Blocks.MOSS_BLOCK);
        prepareEstate(p, level, cx - 67, cz + 58, 24, 22, y, Blocks.MOSS_BLOCK);

        windingPath(p, cx, y, cz, cx + 70, y, cz + 68, Blocks.ROOTED_DIRT);
        windingPath(p, cx, y, cz, cx - 67, y, cz + 58, Blocks.ROOTED_DIRT);
        windingPath(p, cx, y, cz, cx, y, cz - 96, Blocks.ROOTED_DIRT);

        giantTree(p, cx, y, cz, 6, 28);
        platform(p, cx, y + 23, cz, 17);
        elvenLodge(p, cx - 45, y + 1, cz - 28, 10);
        elvenLodge(p, cx + 47, y + 1, cz - 22, 10);
        elvenLodge(p, cx - 37, y + 1, cz + 47, 10);
        elvenLodge(p, cx + 40, y + 1, cz + 49, 11);
        platform(p, cx - 45, y + 16, cz - 28, 9);
        platform(p, cx + 47, y + 16, cz - 22, 9);
        canopyBridge(p, cx, y + 23, cz, cx - 45, y + 16, cz - 28);
        canopyBridge(p, cx, y + 23, cz, cx + 47, y + 16, cz - 22);
        moonGarden(p, cx + 70, y, cz + 68);
        council(p, cx - 67, y, cz + 58);
        forestWorkshop(p, cx, y, cz - 79);
        grove(p, cx, y, cz, 112, 54, Blocks.DARK_OAK_LOG, Blocks.FLOWERING_AZALEA_LEAVES);
    }

    private static void buildKardum(IncrementalWorldEditPlan p, ServerLevel level,
                                    RealmSiteLayoutSavedData.RealmSite site) {
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = clamp(site.baseY(), 74, 124);
        Style stone = new Style(Blocks.POLISHED_DEEPSLATE, Blocks.POLISHED_BASALT,
                Blocks.POLISHED_ANDESITE, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE);

        sculptPlateau(p, level, cx, cz, 112, 98, 54, y, Blocks.STONE, Blocks.STONE);
        terraceTerrain(p, level, cx, cz, y);

        stoneRoad(p, cx, y, cz - 90, cx, y + 6, cz + 80);
        stoneRoad(p, cx - 96, y, cz + 34, cx + 96, y + 3, cz + 34);
        dwarvenGate(p, cx, y, cz - 88);
        building(p, cx - 39, y + 6, cz - 35, 28, 19, 8, stone);
        building(p, cx + 20, y + 9, cz - 21, 30, 20, 9, stone);
        forge(p, cx - 17, y + 3, cz + 42, stone);
        quarter(p, cx - 84, y, cz + 33, stone);
        quarter(p, cx + 63, y + 3, cz + 37, stone);
        vault(p, cx + 58, y + 10, cz - 71, stone);
        mine(p, cx - 106, y + 2, cz - 59);
        oreYard(p, cx + 92, y + 1, cz + 71);
        stoneRoom(p, cx - 78, y + 1, cz + 38);
        stoneRoom(p, cx - 10, y + 1, cz - 77);
        basaltLamps(p, cx, y, cz);
    }

    private static void sculptPlateau(IncrementalWorldEditPlan p, ServerLevel level,
                                      int cx, int cz, int radiusX, int radiusZ, int blend,
                                      int plateauY, Block surface, Block filler) {
        int totalX = radiusX + blend;
        int totalZ = radiusZ + blend;
        for (int x = cx - totalX; x <= cx + totalX; x++) {
            for (int z = cz - totalZ; z <= cz + totalZ; z++) {
                double nx = Math.abs(x - cx) / (double) radiusX;
                double nz = Math.abs(z - cz) / (double) radiusZ;
                double distance = Math.max(nx, nz);
                if (distance > 1.0 + blend / (double) Math.max(radiusX, radiusZ)) continue;
                int natural = p.originalSurfaceY(level, x, z);
                double edge = Math.max(0.0, distance - 1.0);
                double blendSpan = blend / (double) Math.max(radiusX, radiusZ);
                double t = blendSpan <= 0.0 ? 1.0 : smoothstep(Math.min(1.0, edge / blendSpan));
                int target = distance <= 1.0 ? plateauY : (int) Math.round(lerp(plateauY, natural, t));
                terrainColumn(p, level, x, z, target, surface, filler, distance <= 1.02);
            }
        }
    }

    private static void prepareEstate(IncrementalWorldEditPlan p, ServerLevel level,
                                      int cx, int cz, int radiusX, int radiusZ,
                                      int y, Block surface) {
        int blend = 18;
        for (int x = cx - radiusX - blend; x <= cx + radiusX + blend; x++) {
            for (int z = cz - radiusZ - blend; z <= cz + radiusZ + blend; z++) {
                int dx = Math.max(0, Math.abs(x - cx) - radiusX);
                int dz = Math.max(0, Math.abs(z - cz) - radiusZ);
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > blend) continue;
                int current = p.plannedSurfaceY(level, x, z);
                double t = smoothstep(distance / blend);
                int target = (int) Math.round(lerp(y, current, t));
                terrainColumn(p, level, x, z, target, surface, Blocks.DIRT, distance < 1.0);
            }
        }
    }

    private static void terrainColumn(IncrementalWorldEditPlan p, ServerLevel level, int x, int z,
                                      int targetY, Block surface, Block filler, boolean clearVegetation) {
        int current = p.plannedSurfaceY(level, x, z);
        if (current < targetY) fill(p, x, current + 1, z, x, targetY - 1, z, filler);
        if (current > targetY) clear(p, x, targetY + 1, z, x, current + 18, z);
        else if (clearVegetation) clear(p, x, targetY + 1, z, x, targetY + 18, z);
        set(p, x, targetY, z, surface);
        p.setPlannedSurfaceY(x, z, targetY);
    }

    private static void surfaceColumn(IncrementalWorldEditPlan p, ServerLevel level, int x, int z,
                                      int targetY, Block surface, Block filler) {
        int current = p.plannedSurfaceY(level, x, z);
        if (current < targetY) fill(p, x, current + 1, z, x, targetY - 1, z, filler);
        if (current > targetY) clear(p, x, targetY + 1, z, x, current, z);
        set(p, x, targetY, z, surface);
        p.setPlannedSurfaceY(x, z, targetY);
    }

    private static void road(IncrementalWorldEditPlan p, int x1, int z1, int x2, int z2,
                             int y, int halfWidth, Block center, Block edge) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        boolean xMajor = Math.abs(x2 - x1) >= Math.abs(z2 - z1);
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : i / (double) steps;
            int x = (int) Math.round(lerp(x1, x2, t));
            int z = (int) Math.round(lerp(z1, z2, t));
            for (int side = -halfWidth; side <= halfWidth; side++) {
                int px = xMajor ? x : x + side;
                int pz = xMajor ? z + side : z;
                set(p, px, y, pz, Math.abs(side) == halfWidth ? edge : center);
                clear(p, px, y + 1, pz, px, y + 3, pz);
            }
        }
    }

    private static void windingPath(IncrementalWorldEditPlan p, int x1, int y1, int z1,
                                    int x2, int y2, int z2, Block block) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : i / (double) steps;
            int x = (int) Math.round(lerp(x1, x2, t) + Math.sin(t * Math.PI * 4.0) * 2.0);
            int y = (int) Math.round(lerp(y1, y2, t));
            int z = (int) Math.round(lerp(z1, z2, t));
            fill(p, x - 1, y, z - 1, x + 1, y, z + 1, block);
            clear(p, x - 1, y + 1, z - 1, x + 1, y + 3, z + 1);
        }
    }

    private static void building(IncrementalWorldEditPlan p, int x, int y, int z,
                                 int width, int depth, int height, Style style) {
        clear(p, x - 1, y + 1, z - 1, x + width, y + height + 10, z + depth);
        fill(p, x - 1, y - 1, z - 1, x + width, y - 1, z + depth, style.foundation());
        fill(p, x, y, z, x + width - 1, y, z + depth - 1, style.floor());
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                if (dx != 0 && dx != width - 1 && dz != 0 && dz != depth - 1) continue;
                for (int dy = 1; dy <= height; dy++) {
                    boolean corner = (dx == 0 || dx == width - 1) && (dz == 0 || dz == depth - 1);
                    boolean beam = corner || dy == 1 || dy == height || (dx % 5 == 0 && dz % 5 == 0);
                    Block wall = beam ? style.frame() : style.wall();
                    if (!beam && dy >= 3 && dy <= 4 && ((dx + dz) % 7 == 0)) wall = Blocks.GLASS_PANE;
                    set(p, x + dx, y + dy, z + dz, wall);
                }
            }
        }
        clear(p, x + width / 2 - 1, y + 1, z, x + width / 2 + 1, y + 3, z);
        roof(p, x - 2, y + height + 1, z - 2, width + 4, depth + 4, style.roof());
    }

    private static void house(IncrementalWorldEditPlan p, int x, int y, int z,
                              int width, int depth, Style style, int variant) {
        building(p, x, y, z, width, depth, 5 + variant % 2, style);
        set(p, x + 2, y + 1, z + depth - 3, Blocks.BARREL);
        set(p, x + width - 3, y + 1, z + depth - 3, Blocks.CRAFTING_TABLE);
        set(p, x + width / 2, y + 2, z + depth - 2, Blocks.LANTERN);
    }

    private static void buildingWithInterior(IncrementalWorldEditPlan p, int x, int y, int z,
                                             int width, int depth, int height, Style style,
                                             Interior interior) {
        building(p, x, y, z, width, depth, height, style);
        switch (interior) {
            case INN -> {
                set(p, x + 5, y + 1, z + 5, Blocks.BARREL);
                set(p, x + 10, y + 1, z + 7, Blocks.CAMPFIRE);
                set(p, x + 16, y + 1, z + 5, Blocks.CHEST);
            }
            case GUILD -> {
                set(p, x + 5, y + 1, z + 5, Blocks.CARTOGRAPHY_TABLE);
                set(p, x + 11, y + 1, z + 5, Blocks.LECTERN);
                set(p, x + 17, y + 1, z + 5, Blocks.FLETCHING_TABLE);
            }
            case SMITHY -> {
                fill(p, x + width - 7, y + 1, z + 3, x + width - 3, y + 1, z + 8, Blocks.STONE_BRICKS);
                set(p, x + width - 5, y + 2, z + 5, Blocks.BLAST_FURNACE);
                set(p, x + width - 3, y + 2, z + 7, Blocks.ANVIL);
            }
            case BARRACKS -> {
                fill(p, x + width - 8, y + 1, z + 3, x + width - 3, y + 5, z + depth - 4, Blocks.IRON_BARS);
                clear(p, x + width - 6, y + 1, z + 3, x + width - 6, y + 3, z + 3);
            }
            case GRANARY -> {
                for (int dx = 3; dx < width - 2; dx += 4) set(p, x + dx, y + 1, z + depth - 4, Blocks.BARREL);
            }
        }
    }

    private static void roof(IncrementalWorldEditPlan p, int x, int y, int z,
                             int width, int depth, Block block) {
        int layers = Math.max(3, Math.min(6, depth / 2));
        for (int layer = 0; layer < layers; layer++) {
            int front = z + layer;
            int back = z + depth - 1 - layer;
            fill(p, x, y + layer, front, x + width - 1, y + layer, front, block);
            fill(p, x, y + layer, back, x + width - 1, y + layer, back, block);
        }
        int middleStart = z + layers;
        int middleEnd = z + depth - 1 - layers;
        if (middleStart <= middleEnd) fill(p, x, y + layers, middleStart, x + width - 1, y + layers, middleEnd, block);
    }

    private static void keep(IncrementalWorldEditPlan p, int x, int y, int z, Style style) {
        building(p, x, y, z, 34, 25, 9, style);
        tower(p, x - 4, y, z - 4, 9, 15, style);
        tower(p, x + 29, y, z - 4, 9, 15, style);
        tower(p, x - 4, y, z + 20, 9, 15, style);
        tower(p, x + 29, y, z + 20, 9, 15, style);
    }

    private static void tower(IncrementalWorldEditPlan p, int x, int y, int z,
                              int size, int height, Style style) {
        clear(p, x - 1, y + 1, z - 1, x + size, y + height + 4, z + size);
        fill(p, x, y, z, x + size - 1, y, z + size - 1, style.floor());
        for (int dx = 0; dx < size; dx++) for (int dz = 0; dz < size; dz++) {
            if (dx != 0 && dx != size - 1 && dz != 0 && dz != size - 1) continue;
            fill(p, x + dx, y + 1, z + dz, x + dx, y + height, z + dz, style.foundation());
        }
        fill(p, x - 1, y + height + 1, z - 1, x + size, y + height + 1, z + size, style.roof());
        clear(p, x + size / 2, y + 1, z, x + size / 2, y + 3, z);
    }

    private static void walls(IncrementalWorldEditPlan p, int cx, int y, int cz,
                              int rx, int rz, Style style) {
        for (int x = cx - rx; x <= cx + rx; x++) {
            wallColumn(p, x, y, cz - rz, style.foundation());
            wallColumn(p, x, y, cz + rz, style.foundation());
        }
        for (int z = cz - rz; z <= cz + rz; z++) {
            wallColumn(p, cx - rx, y, z, style.foundation());
            wallColumn(p, cx + rx, y, z, style.foundation());
        }
        gate(p, cx, y, cz - rz, true, style);
        gate(p, cx, y, cz + rz, true, style);
        gate(p, cx - rx, y, cz, false, style);
        gate(p, cx + rx, y, cz, false, style);
        for (int[] corner : new int[][]{{-rx, -rz}, {rx, -rz}, {-rx, rz}, {rx, rz}}) {
            tower(p, cx + corner[0] - 4, y, cz + corner[1] - 4, 9, 13, style);
        }
    }

    private static void wallColumn(IncrementalWorldEditPlan p, int x, int y, int z, Block block) {
        fill(p, x, y + 1, z, x, y + 6, z, block);
        if (Math.floorMod(x + z, 2) == 0) set(p, x, y + 7, z, Blocks.STONE_BRICK_WALL);
    }

    private static void gate(IncrementalWorldEditPlan p, int x, int y, int z,
                             boolean northSouth, Style style) {
        int x1 = northSouth ? x - 10 : x - 5;
        int z1 = northSouth ? z - 5 : z - 10;
        int x2 = northSouth ? x + 10 : x + 5;
        int z2 = northSouth ? z + 5 : z + 10;
        for (int px = x1; px <= x2; px++) for (int pz = z1; pz <= z2; pz++) {
            if (px != x1 && px != x2 && pz != z1 && pz != z2) continue;
            fill(p, px, y + 1, pz, px, y + 9, pz, style.foundation());
        }
        if (northSouth) clear(p, x - 4, y + 1, z1, x + 4, y + 7, z2);
        else clear(p, x1, y + 1, z - 4, x2, y + 7, z + 4);
        fill(p, x1 - 1, y + 10, z1 - 1, x2 + 1, y + 10, z2 + 1, style.roof());
        set(p, x, y + 9, z, Blocks.BELL);
    }

    private static void temple(IncrementalWorldEditPlan p, int x, int y, int z) {
        Style style = new Style(Blocks.SMOOTH_STONE, Blocks.CHISELED_STONE_BRICKS,
                Blocks.CALCITE, Blocks.STONE_BRICKS, Blocks.SMOOTH_STONE);
        building(p, x, y, z, 23, 18, 8, style);
        set(p, x + 11, y + 1, z + 12, Blocks.BELL);
    }

    private static void market(IncrementalWorldEditPlan p, int cx, int y, int cz) {
        fill(p, cx - 26, y, cz - 22, cx + 26, y, cz + 22, Blocks.STONE_BRICKS);
        for (int[] offset : new int[][]{{-21, -15}, {-7, -15}, {8, -15}, {-21, 9}, {-7, 9}, {8, 9}}) {
            int x = cx + offset[0];
            int z = cz + offset[1];
            fill(p, x, y + 1, z, x + 10, y + 1, z + 5, Blocks.SPRUCE_PLANKS);
            for (int postX : new int[]{x, x + 10}) {
                fill(p, postX, y + 2, z, postX, y + 5, z, Blocks.OAK_FENCE);
            }
            fill(p, x, y + 6, z - 1, x + 10, y + 6, z + 6, Blocks.DARK_OAK_SLAB);
            set(p, x + 5, y + 2, z + 3, Blocks.BARREL);
        }
        fill(p, cx, y + 1, cz, cx, y + 6, cz, Blocks.CHISELED_STONE_BRICKS);
        set(p, cx, y + 7, cz, Blocks.LANTERN);
    }

    private static void stable(IncrementalWorldEditPlan p, int x, int y, int z) {
        fill(p, x, y, z, x + 24, y, z + 16, Blocks.COARSE_DIRT);
        for (int dx = 0; dx <= 24; dx++) {
            set(p, x + dx, y + 1, z, Blocks.OAK_FENCE);
            set(p, x + dx, y + 1, z + 16, Blocks.OAK_FENCE);
        }
        for (int dx = 2; dx <= 22; dx += 6) set(p, x + dx, y + 1, z + 8, Blocks.HAY_BLOCK);
        fill(p, x, y + 5, z, x + 24, y + 5, z + 16, Blocks.SPRUCE_SLAB);
    }

    private static void canal(IncrementalWorldEditPlan p, int x, int y, int z1, int z2) {
        for (int z = z1; z <= z2; z++) {
            int bend = (int) Math.round(Math.sin(z * 0.045) * 5.0);
            for (int dx = -6; dx <= 6; dx++) {
                int px = x + bend + dx;
                clear(p, px, y - 1, z, px, y + 5, z);
                fill(p, px, y - 3, z, px, y - 1, z, Blocks.WATER);
                set(p, px, y - 4, z, Math.abs(dx) >= 5 ? Blocks.CLAY : Blocks.GRAVEL);
                p.setPlannedSurfaceY(px, z, y - 1);
            }
        }
    }

    private static void bridge(IncrementalWorldEditPlan p, int x1, int y1, int z1,
                               int x2, int y2, int z2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : i / (double) steps;
            int x = (int) Math.round(lerp(x1, x2, t));
            int y = (int) Math.round(lerp(y1, y2, t));
            int z = (int) Math.round(lerp(z1, z2, t));
            fill(p, x, y, z - 3, x, y, z + 3, Blocks.STONE_BRICKS);
            set(p, x, y + 1, z - 4, Blocks.STONE_BRICK_WALL);
            set(p, x, y + 1, z + 4, Blocks.STONE_BRICK_WALL);
        }
    }

    private static void dock(IncrementalWorldEditPlan p, int x, int y, int z) {
        for (int dz = -4; dz <= 4; dz++) fill(p, x - 3, y, z + dz, x + 18, y, z + dz, Blocks.SPRUCE_PLANKS);
        for (int dx = -3; dx <= 18; dx += 5) {
            fill(p, x + dx, y - 3, z - 4, x + dx, y, z - 4, Blocks.SPRUCE_LOG);
            fill(p, x + dx, y - 3, z + 4, x + dx, y, z + 4, Blocks.SPRUCE_LOG);
        }
        set(p, x + 8, y + 1, z, Blocks.BARREL);
    }

    private static void field(IncrementalWorldEditPlan p, int x, int y, int z,
                              int width, int depth, int variant) {
        for (int dx = -width / 2; dx <= width / 2; dx++) {
            for (int dz = -depth / 2; dz <= depth / 2; dz++) {
                int px = x + dx;
                int pz = z + dz;
                if (Math.abs(dx) == width / 2 || Math.abs(dz) == depth / 2) {
                    set(p, px, y + 1, pz, Blocks.OAK_FENCE);
                } else if (Math.floorMod(dx + width, 7) == 0) {
                    set(p, px, y, pz, Blocks.WATER);
                } else {
                    set(p, px, y, pz, Blocks.FARMLAND);
                    if (Math.floorMod(dx + dz + variant, 4) != 0) set(p, px, y + 1, pz, Blocks.WHEAT);
                }
            }
        }
    }

    private static void farmhouse(IncrementalWorldEditPlan p, int x, int y, int z, Style style) {
        house(p, x - 6, y, z - 5, 13, 11, style, 1);
        set(p, x, y + 1, z + 3, Blocks.BARREL);
    }

    private static void fishingHut(IncrementalWorldEditPlan p, int x, int y, int z, Style style) {
        house(p, x - 5, y, z - 5, 11, 10, style, 0);
        dock(p, x - 2, y, z + 8);
    }

    private static void camp(IncrementalWorldEditPlan p, int x, int y, int z) {
        fill(p, x - 8, y, z - 8, x + 8, y, z + 8, Blocks.COARSE_DIRT);
        set(p, x, y + 1, z, Blocks.CAMPFIRE);
        for (int[] post : new int[][]{{-6, -5}, {5, -5}, {-6, 5}, {5, 5}}) {
            fill(p, x + post[0], y + 1, z + post[1], x + post[0], y + 4, z + post[1], Blocks.SPRUCE_FENCE);
        }
        fill(p, x - 7, y + 5, z - 6, x + 6, y + 5, z + 6, Blocks.DARK_OAK_SLAB);
        set(p, x - 3, y + 1, z + 2, Blocks.CHEST);
        set(p, x + 3, y + 1, z + 2, Blocks.HAY_BLOCK);
    }

    private static void lamps(IncrementalWorldEditPlan p, int cx, int y, int cz) {
        for (int x = cx - 104; x <= cx + 104; x += 26) {
            lamp(p, x, y, cz - 60);
            lamp(p, x, y, cz + 62);
        }
        for (int z = cz - 86; z <= cz + 86; z += 24) {
            lamp(p, cx - 72, y, z);
            lamp(p, cx + 72, y, z);
        }
    }

    private static void lamp(IncrementalWorldEditPlan p, int x, int y, int z) {
        fill(p, x, y + 1, z, x, y + 3, z, Blocks.SPRUCE_FENCE);
        set(p, x, y + 4, z, Blocks.LANTERN);
    }

    private static void giantTree(IncrementalWorldEditPlan p, int x, int y, int z, int radius, int height) {
        for (int dy = 0; dy <= height; dy++) {
            int r = Math.max(2, radius - dy / 8);
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz <= r * r) set(p, x + dx, y + dy, z + dz, Blocks.DARK_OAK_LOG);
            }
        }
        int crown = y + height;
        for (int dx = -14; dx <= 14; dx++) for (int dz = -14; dz <= 14; dz++) {
            for (int dy = -4; dy <= 6; dy++) {
                if (dx * dx + dz * dz + dy * dy * 2 <= 190) set(p, x + dx, crown + dy, z + dz, Blocks.AZALEA_LEAVES);
            }
        }
    }

    private static void platform(IncrementalWorldEditPlan p, int x, int y, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            if (dx * dx + dz * dz <= radius * radius) set(p, x + dx, y, z + dz, Blocks.STRIPPED_BIRCH_WOOD);
        }
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI * 2.0 / 16.0;
            int px = x + (int) Math.round(Math.cos(angle) * (radius - 1));
            int pz = z + (int) Math.round(Math.sin(angle) * (radius - 1));
            set(p, px, y + 1, pz, Blocks.OAK_FENCE);
            if (i % 4 == 0) set(p, px, y + 2, pz, Blocks.SOUL_LANTERN);
        }
    }

    private static void elvenLodge(IncrementalWorldEditPlan p, int x, int y, int z, int radius) {
        clear(p, x - radius - 1, y + 1, z - radius - 1, x + radius + 1, y + 12, z + radius + 1);
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

    private static void canopyBridge(IncrementalWorldEditPlan p, int x1, int y1, int z1,
                                     int x2, int y2, int z2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : i / (double) steps;
            int x = (int) Math.round(lerp(x1, x2, t));
            int y = (int) Math.round(lerp(y1, y2, t));
            int z = (int) Math.round(lerp(z1, z2, t));
            fill(p, x - 1, y, z, x + 1, y, z, Blocks.DARK_OAK_PLANKS);
            if (i % 4 == 0) {
                set(p, x - 2, y + 1, z, Blocks.OAK_FENCE);
                set(p, x + 2, y + 1, z, Blocks.OAK_FENCE);
            }
        }
    }

    private static void moonGarden(IncrementalWorldEditPlan p, int x, int y, int z) {
        for (int dx = -13; dx <= 13; dx++) for (int dz = -13; dz <= 13; dz++) {
            int d = dx * dx + dz * dz;
            if (d <= 64) set(p, x + dx, y, z + dz, Blocks.WATER);
            else if (d <= 150) set(p, x + dx, y, z + dz, Blocks.MOSSY_STONE_BRICKS);
        }
        set(p, x, y - 1, z, Blocks.SEA_LANTERN);
        elvenLodge(p, x + 3, y + 1, z + 19, 8);
    }

    private static void council(IncrementalWorldEditPlan p, int x, int y, int z) {
        fill(p, x - 14, y, z - 14, x + 14, y, z + 14, Blocks.MOSS_BLOCK);
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI * 2.0 / 12.0;
            int px = x + (int) Math.round(Math.cos(angle) * 12);
            int pz = z + (int) Math.round(Math.sin(angle) * 12);
            fill(p, px, y + 1, pz, px, y + 4, pz, Blocks.OAK_LOG);
            set(p, px, y + 5, pz, Blocks.SOUL_LANTERN);
        }
        set(p, x, y + 1, z, Blocks.LECTERN);
    }

    private static void forestWorkshop(IncrementalWorldEditPlan p, int x, int y, int z) {
        Style style = new Style(Blocks.MOSSY_STONE_BRICKS, Blocks.STRIPPED_DARK_OAK_LOG,
                Blocks.BIRCH_PLANKS, Blocks.AZALEA_LEAVES, Blocks.STRIPPED_BIRCH_WOOD);
        buildingWithInterior(p, x - 12, y, z - 8, 24, 16, 6, style, Interior.GUILD);
    }

    private static void grove(IncrementalWorldEditPlan p, int cx, int y, int cz,
                              int radius, int count, Block log, Block leaves) {
        for (int i = 0; i < count; i++) {
            double angle = i * 2.399963229728653;
            int r = radius + (i % 4) * 9;
            int x = cx + (int) Math.round(Math.cos(angle) * r);
            int z = cz + (int) Math.round(Math.sin(angle) * r);
            int height = 6 + i % 4;
            fill(p, x, y + 1, z, x, y + height, z, log);
            int crown = y + height;
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dz * dz <= 6) fill(p, x + dx, crown, z + dz, x + dx, crown + 2, z + dz, leaves);
            }
        }
    }

    private static void terraceTerrain(IncrementalWorldEditPlan p, ServerLevel level, int cx, int cz, int y) {
        for (int ring = 88; ring >= 36; ring -= 18) {
            int target = y + (88 - ring) / 18 * 3;
            for (int x = cx - ring; x <= cx + ring; x++) {
                surfaceColumn(p, level, x, cz - ring, target, Blocks.STONE, Blocks.STONE);
                surfaceColumn(p, level, x, cz + ring, target, Blocks.STONE, Blocks.STONE);
            }
            for (int z = cz - ring; z <= cz + ring; z++) {
                surfaceColumn(p, level, cx - ring, z, target, Blocks.STONE, Blocks.STONE);
                surfaceColumn(p, level, cx + ring, z, target, Blocks.STONE, Blocks.STONE);
            }
        }
    }

    private static void stoneRoad(IncrementalWorldEditPlan p, int x1, int y1, int z1,
                                  int x2, int y2, int z2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : i / (double) steps;
            int x = (int) Math.round(lerp(x1, x2, t));
            int y = (int) Math.round(lerp(y1, y2, t));
            int z = (int) Math.round(lerp(z1, z2, t));
            fill(p, x - 3, y, z, x + 3, y, z, Blocks.POLISHED_ANDESITE);
            clear(p, x - 3, y + 1, z, x + 3, y + 4, z);
        }
    }

    private static void dwarvenGate(IncrementalWorldEditPlan p, int x, int y, int z) {
        for (int dx = -16; dx <= 16; dx++) {
            int height = 12 + Math.max(0, 8 - Math.abs(dx) / 2);
            fill(p, x + dx, y + 1, z, x + dx, y + height, z, Blocks.DEEPSLATE_BRICKS);
        }
        clear(p, x - 5, y + 1, z, x + 5, y + 11, z + 16);
        fill(p, x - 5, y, z, x + 5, y, z + 16, Blocks.POLISHED_DEEPSLATE);
        set(p, x - 7, y + 7, z + 2, Blocks.LANTERN);
        set(p, x + 7, y + 7, z + 2, Blocks.LANTERN);
    }

    private static void forge(IncrementalWorldEditPlan p, int x, int y, int z, Style style) {
        building(p, x, y, z, 30, 20, 8, style);
        fill(p, x + 4, y + 1, z + 5, x + 25, y + 1, z + 14, Blocks.IRON_BLOCK);
        set(p, x + 7, y + 2, z + 9, Blocks.BLAST_FURNACE);
        set(p, x + 14, y + 2, z + 9, Blocks.ANVIL);
        set(p, x + 22, y + 2, z + 9, Blocks.LAVA);
    }

    private static void quarter(IncrementalWorldEditPlan p, int x, int y, int z, Style style) {
        for (int i = 0; i < 4; i++) {
            building(p, x + (i % 2) * 22, y + (i / 2) * 2,
                    z + (i / 2) * 18, 18, 14, 7, style);
        }
    }

    private static void vault(IncrementalWorldEditPlan p, int x, int y, int z, Style style) {
        building(p, x, y, z, 24, 18, 8, style);
        fill(p, x + 6, y + 1, z + 5, x + 18, y + 6, z + 13, Blocks.IRON_BARS);
        set(p, x + 12, y + 2, z + 9, Blocks.CHEST);
    }

    private static void mine(IncrementalWorldEditPlan p, int x, int y, int z) {
        for (int dx = -10; dx <= 10; dx++) for (int dy = 1; dy <= 12; dy++) {
            if (Math.abs(dx) >= 7 || dy >= 10) set(p, x + dx, y + dy, z, Blocks.DEEPSLATE_BRICKS);
        }
        clear(p, x - 5, y + 1, z, x + 5, y + 8, z + 18);
        fill(p, x - 3, y, z, x + 3, y, z + 22, Blocks.POLISHED_DEEPSLATE);
    }

    private static void oreYard(IncrementalWorldEditPlan p, int x, int y, int z) {
        fill(p, x - 18, y, z - 15, x + 18, y, z + 15, Blocks.POLISHED_ANDESITE);
        for (int[] offset : new int[][]{{-10, -7}, {0, -7}, {10, -7}, {-5, 6}, {7, 6}}) {
            fill(p, x + offset[0] - 2, y + 1, z + offset[1] - 2,
                    x + offset[0] + 2, y + 4, z + offset[1] + 2,
                    Math.floorMod(offset[0] + offset[1], 2) == 0 ? Blocks.RAW_IRON_BLOCK : Blocks.RAW_COPPER_BLOCK);
        }
    }

    private static void stoneRoom(IncrementalWorldEditPlan p, int x, int y, int z) {
        Style style = new Style(Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS,
                Blocks.POLISHED_ANDESITE, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE);
        building(p, x, y, z, 13, 11, 6, style);
        set(p, x + 9, y + 1, z + 7, Blocks.LANTERN);
        set(p, x + 3, y + 1, z + 7, Blocks.BARREL);
    }

    private static void basaltLamps(IncrementalWorldEditPlan p, int cx, int y, int cz) {
        for (int z = cz - 70; z <= cz + 70; z += 20) {
            fill(p, cx - 20, y + 1, z, cx - 20, y + 4, z, Blocks.POLISHED_BASALT);
            set(p, cx - 20, y + 5, z, Blocks.SOUL_LANTERN);
            fill(p, cx + 20, y + 1, z, cx + 20, y + 4, z, Blocks.POLISHED_BASALT);
            set(p, cx + 20, y + 5, z, Blocks.SOUL_LANTERN);
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

    private static double smoothstep(double value) {
        double x = Math.max(0.0, Math.min(1.0, value));
        return x * x * (3.0 - 2.0 * x);
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum Interior {
        INN, GUILD, SMITHY, BARRACKS, GRANARY
    }

    private record Style(Block foundation, Block frame, Block wall, Block roof, Block floor) {
    }
}
