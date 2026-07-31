package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Destructive repair pass for early playtest settlements.
 *
 * <p>Revision three correctly introduced authored regional palettes, but it rebuilt structures over
 * earlier prototypes. Full-block gable rows also touched only diagonally, which looked like floating
 * roof fragments. Revision four clears only known authored construction footprints, preserves player
 * origin data, rebuilds solid structures and blends lots into the surrounding ground.</p>
 */
public final class RealmRevisionFourManager {
    public static final int CURRENT_REVISION = 4;

    private RealmRevisionFourManager() {
    }

    public static void ensureForPlayer(ServerPlayer player, OriginProfile profile) {
        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) return;

        StarterRealmUpgradeSavedData data = realm.getDataStorage().computeIfAbsent(StarterRealmUpgradeSavedData.TYPE);
        boolean needsRepair = data.revision(profile.homelandId()) < CURRENT_REVISION;
        ensureRegion(realm, profile.homelandId());
        if (needsRepair) {
            StarterRealmManager.placePlayer(player, profile);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§6[세계 복구] §f초기 시험 구조물을 정리하고 지역을 새 설계로 복구했습니다."
            ));
        }
    }

    public static synchronized void ensureRegion(ServerLevel level, String homelandId) {
        AuthoredRealmManager.ensureRegion(level, homelandId);
        StarterRealmUpgradeSavedData data = level.getDataStorage().computeIfAbsent(StarterRealmUpgradeSavedData.TYPE);
        if (data.revision(homelandId) >= CURRENT_REVISION) return;

        switch (homelandId) {
            case "silvana_forest" -> rebuildSilvana(level);
            case "kardum_league" -> rebuildKardum(level);
            default -> rebuildErden(level);
        }
        data.setRevision(homelandId, CURRENT_REVISION);
        LivingKingdoms.LOGGER.info("Authored homeland {} rebuilt at revision {}", homelandId, CURRENT_REVISION);
    }

    private static void rebuildErden(ServerLevel level) {
        // Remove every prototype structure and floating fragment inside the authored town envelope.
        clear(level, -88, 66, -88, 88, 108, 88);
        clear(level, 94, 66, 54, 132, 96, 98);
        clear(level, -126, 66, 72, -92, 96, 116);
        clear(level, 68, 66, -132, 104, 96, -96);

        flattenSquare(level, 0, 0, 82, 65, Blocks.GRASS_BLOCK);
        blendSquareEdge(level, 0, 0, 82, 106, 65, Blocks.GRASS_BLOCK);

        stoneRoad(level, 0, -76, 0, 76, 4);
        stoneRoad(level, -76, 0, 76, 0, 4);
        stoneRoad(level, 0, 0, 110, 72, 3);
        stoneRoad(level, 0, 0, -108, 90, 3);
        stoneRoad(level, 0, 0, 84, -112, 3);
        plaza(level, 0, 65, 0, 15);

        erdenHouse(level, 10, 65, 8, 12, 10);
        erdenHouse(level, -34, 65, -30, 11, 9);
        erdenHouse(level, 25, 65, -32, 12, 9);
        erdenHouse(level, -38, 65, 24, 11, 9);
        erdenHouse(level, 44, 65, 20, 11, 9);
        erdenHouse(level, -62, 65, 10, 11, 9);
        erdenInn(level, 24, 65, 40);
        erdenSmithy(level, -30, 65, 42);
        erdenBarracks(level, -14, 65, -72);
        erdenMarket(level, 0, 65, 0);
        connectedPalisade(level, 78);

        flattenLot(level, 96, 54, 130, 96, 65, Blocks.GRASS_BLOCK);
        erdenHouse(level, 106, 65, 67, 13, 10);
        farm(level, 83, 65, 60, 18, 20);

        flattenLot(level, -126, 72, -92, 116, 65, Blocks.GRASS_BLOCK);
        erdenHouse(level, -114, 65, 84, 10, 9);
        fishingPier(level, -119, 65, 97);

        flattenLot(level, 68, -132, 104, -96, 65, Blocks.COARSE_DIRT);
        travellerCamp(level, 84, 65, -114);

        plantedTree(level, 68, 65, 57, 6, Blocks.OAK_LOG, Blocks.OAK_LEAVES);
        plantedTree(level, -69, 65, -56, 7, Blocks.OAK_LOG, Blocks.OAK_LEAVES);
        plantedTree(level, 62, 65, -65, 6, Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES);
        plantedTree(level, -64, 65, 60, 7, Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES);
    }

    private static void rebuildSilvana(ServerLevel level) {
        clear(level, 1150, 66, -70, 1335, 112, 115);
        blendCircle(level, 1240, 35, 96, 66, Blocks.MOSS_BLOCK);

        giantSupportedTree(level, 1240, 65, 35, 5, 24);
        elvenLodge(level, 1268, 66, 28, 9);
        elvenLodge(level, 1190, 67, 62, 8);
        elvenLodge(level, 1308, 66, -4, 8);
        supportedPlatform(level, 1240, 82, 35, 13);
        supportedPlatform(level, 1188, 78, 4, 9);
        supportedPlatform(level, 1300, 79, 54, 9);
        canopyBridge(level, 1240, 82, 35, 1188, 78, 4);
        canopyBridge(level, 1240, 82, 35, 1300, 79, 54);
        moonGarden(level, 1275, 66, 82);
    }

    private static void rebuildKardum(ServerLevel level) {
        clear(level, -1260, 67, -30, -1070, 116, 118);
        flattenLot(level, -1255, -22, -1080, 108, 67, Blocks.POLISHED_ANDESITE);
        dwarvenHall(level, -1238, 67, 46, 16, 13);
        dwarvenHall(level, -1108, 67, 54, 16, 13);
        dwarvenHall(level, -1215, 67, 82, 14, 11);
        dwarvenForge(level, -1170, 67, -10);
        dwarvenGate(level, -1202, 67, 2);
        stoneRoad(level, -1248, 38, -1092, 38, 4);
    }

    private static void erdenHouse(ServerLevel level, int x, int y, int z, int width, int depth) {
        clear(level, x - 5, y + 1, z - 5, x + width + 4, y + 18, z + depth + 4);
        flattenLot(level, x - 4, z - 4, x + width + 3, z + depth + 3, y, Blocks.GRASS_BLOCK);
        fill(level, x, y, z, x + width - 1, y, z + depth - 1, Blocks.STONE_BRICKS);

        for (int ix = 0; ix < width; ix++) {
            for (int iz = 0; iz < depth; iz++) {
                if (ix != 0 && ix != width - 1 && iz != 0 && iz != depth - 1) continue;
                for (int dy = 1; dy <= 4; dy++) {
                    boolean corner = (ix == 0 || ix == width - 1) && (iz == 0 || iz == depth - 1);
                    boolean frame = dy == 1 || dy == 4 || (ix % 4 == 0 && (iz == 0 || iz == depth - 1));
                    set(level, x + ix, y + dy, z + iz,
                            corner || frame ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.BIRCH_PLANKS);
                }
            }
        }

        int doorX = x + width / 2;
        clear(level, doorX, y + 1, z, doorX, y + 2, z);
        set(level, x + 2, y + 2, z, Blocks.GLASS_PANE);
        set(level, x + width - 3, y + 2, z, Blocks.GLASS_PANE);
        set(level, x + 2, y + 2, z + depth - 1, Blocks.GLASS_PANE);
        set(level, x + width - 3, y + 2, z + depth - 1, Blocks.GLASS_PANE);
        fill(level, x + 1, y + 1, z + 1, x + width - 2, y + 1, z + depth - 2, Blocks.SPRUCE_PLANKS);
        solidGableRoof(level, x, y + 5, z, width, depth, Blocks.DARK_OAK_PLANKS);
        set(level, x + width - 2, y + 2, z + depth - 2, Blocks.LANTERN);
    }

    private static void solidGableRoof(ServerLevel level, int x, int y, int z, int width, int depth, Block roof) {
        int layers = Math.max(3, (depth + 2) / 2);
        for (int layer = 0; layer < layers; layer++) {
            int frontA = z - 1 + layer;
            int frontB = Math.min(z + depth, frontA + 1);
            int backB = z + depth - layer;
            int backA = Math.max(z - 1, backB - 1);
            fill(level, x - 1, y + layer, frontA, x + width, y + layer, frontB, roof);
            fill(level, x - 1, y + layer, backA, x + width, y + layer, backB, roof);
        }
    }

    private static void erdenInn(ServerLevel level, int x, int y, int z) {
        erdenHouse(level, x, y, z, 18, 13);
        fill(level, x + 2, y + 2, z + 3, x + 15, y + 2, z + 4, Blocks.SPRUCE_PLANKS);
        set(level, x + 4, y + 3, z + 5, Blocks.BARREL);
        set(level, x + 13, y + 3, z + 5, Blocks.CAMPFIRE);
    }

    private static void erdenSmithy(ServerLevel level, int x, int y, int z) {
        erdenHouse(level, x, y, z, 13, 10);
        flattenLot(level, x + 12, z - 1, x + 24, z + 11, y, Blocks.COBBLESTONE);
        for (int dx : new int[]{14, 22}) {
            for (int dy = 1; dy <= 5; dy++) set(level, x + dx, y + dy, z + 2, Blocks.STRIPPED_SPRUCE_LOG);
        }
        fill(level, x + 14, y + 6, z + 1, x + 22, y + 6, z + 9, Blocks.DARK_OAK_PLANKS);
        set(level, x + 16, y + 1, z + 5, Blocks.BLAST_FURNACE);
        set(level, x + 19, y + 1, z + 5, Blocks.ANVIL);
        set(level, x + 22, y + 1, z + 8, Blocks.COBBLESTONE);
    }

    private static void erdenBarracks(ServerLevel level, int x, int y, int z) {
        clear(level, x - 5, y + 1, z - 5, x + 30, y + 18, z + 20);
        flattenLot(level, x - 4, z - 4, x + 29, z + 19, y, Blocks.STONE_BRICKS);
        fill(level, x, y, z, x + 25, y, z + 15, Blocks.STONE_BRICKS);
        for (int dx = 0; dx <= 25; dx++) {
            for (int dz = 0; dz <= 15; dz++) {
                if (dx != 0 && dx != 25 && dz != 0 && dz != 15) continue;
                for (int dy = 1; dy <= 6; dy++) {
                    set(level, x + dx, y + dy, z + dz,
                            dy <= 2 ? Blocks.STONE_BRICKS : Blocks.SPRUCE_PLANKS);
                }
            }
        }
        solidGableRoof(level, x, y + 7, z, 26, 16, Blocks.DARK_OAK_PLANKS);
        clear(level, x + 11, y + 1, z + 15, x + 14, y + 3, z + 15);
        for (int dz = 3; dz <= 12; dz += 4) {
            for (int dy = 1; dy <= 4; dy++) set(level, x + 19, y + dy, z + dz, Blocks.IRON_BARS);
        }
    }

    private static void erdenMarket(ServerLevel level, int cx, int y, int cz) {
        int[][] stalls = {{-12, -9}, {7, -9}, {-12, 7}, {7, 7}};
        for (int[] offset : stalls) {
            int x = cx + offset[0];
            int z = cz + offset[1];
            fill(level, x, y + 1, z, x + 6, y + 1, z + 4, Blocks.SPRUCE_PLANKS);
            for (int dx : new int[]{0, 6}) {
                for (int dz : new int[]{0, 4}) {
                    for (int dy = 2; dy <= 5; dy++) set(level, x + dx, y + dy, z + dz, Blocks.OAK_FENCE);
                }
            }
            fill(level, x - 1, y + 6, z - 1, x + 7, y + 6, z + 5, Blocks.DARK_OAK_PLANKS);
            fill(level, x, y + 7, z, x + 6, y + 7, z + 4, Blocks.SPRUCE_PLANKS);
            set(level, x + 3, y + 2, z + 2, Blocks.BARREL);
        }
        for (int dy = 1; dy <= 5; dy++) set(level, cx, y + dy, cz, Blocks.CHISELED_STONE_BRICKS);
        set(level, cx, y + 6, cz, Blocks.LANTERN);
    }

    private static void connectedPalisade(ServerLevel level, int radius) {
        clear(level, -radius - 2, 66, -radius - 2, radius + 2, 74, -radius + 2);
        clear(level, -radius - 2, 66, radius - 2, radius + 2, 74, radius + 2);
        clear(level, -radius - 2, 66, -radius - 2, -radius + 2, 74, radius + 2);
        clear(level, radius - 2, 66, -radius - 2, radius + 2, 74, radius + 2);

        for (int x = -radius; x <= radius; x++) {
            wallPost(level, x, 65, -radius);
            wallPost(level, x, 65, radius);
        }
        for (int z = -radius; z <= radius; z++) {
            wallPost(level, -radius, 65, z);
            wallPost(level, radius, 65, z);
        }
        for (int gate = -5; gate <= 5; gate++) {
            clear(level, gate, 66, -radius, gate, 72, -radius);
            clear(level, gate, 66, radius, gate, 72, radius);
            clear(level, -radius, 66, gate, -radius, 72, gate);
            clear(level, radius, 66, gate, radius, 72, gate);
        }
        gateTower(level, -8, 65, -radius);
        gateTower(level, 8, 65, -radius);
        gateTower(level, -8, 65, radius);
        gateTower(level, 8, 65, radius);
    }

    private static void wallPost(ServerLevel level, int x, int y, int z) {
        int height = ((x + z) & 3) == 0 ? 5 : 4;
        for (int dy = 1; dy <= height; dy++) set(level, x, y + dy, z, Blocks.STRIPPED_SPRUCE_LOG);
    }

    private static void gateTower(ServerLevel level, int x, int y, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    for (int dy = 1; dy <= 7; dy++) set(level, x + dx, y + dy, z + dz, Blocks.STONE_BRICKS);
                }
            }
        }
        fill(level, x - 3, y + 8, z - 3, x + 3, y + 8, z + 3, Blocks.DARK_OAK_PLANKS);
        set(level, x, y + 9, z, Blocks.LANTERN);
    }

    private static void plaza(ServerLevel level, int cx, int y, int cz, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                Block block = ((dx + dz) & 3) == 0 ? Blocks.COBBLESTONE : Blocks.STONE_BRICKS;
                set(level, cx + dx, y, cz + dz, block);
            }
        }
    }

    private static void farm(ServerLevel level, int x, int y, int z, int width, int depth) {
        flattenLot(level, x - 2, z - 2, x + width + 1, z + depth + 1, y, Blocks.GRASS_BLOCK);
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                if (dx % 6 == 0) set(level, x + dx, y, z + dz, Blocks.WATER);
                else {
                    set(level, x + dx, y, z + dz, Blocks.FARMLAND);
                    if ((dx + dz) % 3 != 0) set(level, x + dx, y + 1, z + dz, Blocks.WHEAT);
                }
            }
        }
    }

    private static void fishingPier(ServerLevel level, int x, int y, int z) {
        for (int dx = 0; dx <= 22; dx++) {
            set(level, x + dx, y, z, Blocks.SPRUCE_PLANKS);
            if (dx % 4 == 0) {
                for (int dy = 1; dy <= 4; dy++) set(level, x + dx, y - dy, z, Blocks.SPRUCE_LOG);
            }
        }
        for (int dz = 1; dz <= 10; dz++) set(level, x + 22, y, z + dz, Blocks.SPRUCE_PLANKS);
        set(level, x + 5, y + 1, z + 1, Blocks.BARREL);
    }

    private static void travellerCamp(ServerLevel level, int x, int y, int z) {
        set(level, x, y + 1, z, Blocks.CAMPFIRE);
        int[][] tents = {{-7, -5}, {5, -4}, {-4, 6}};
        for (int[] tent : tents) {
            int tx = x + tent[0];
            int tz = z + tent[1];
            for (int dx : new int[]{0, 6}) {
                for (int dy = 1; dy <= 4; dy++) set(level, tx + dx, y + dy, tz, Blocks.SPRUCE_LOG);
            }
            fill(level, tx, y + 5, tz - 1, tx + 6, y + 5, tz + 5, Blocks.MOSS_BLOCK);
        }
    }

    private static void giantSupportedTree(ServerLevel level, int x, int y, int z, int radius, int height) {
        for (int dy = 0; dy <= height; dy++) {
            int r = Math.max(2, radius - dy / 8);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz <= r * r) set(level, x + dx, y + dy, z + dz, Blocks.DARK_OAK_LOG);
                }
            }
        }
        int crown = y + height;
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                for (int dy = -4; dy <= 5; dy++) {
                    if (dx * dx + dz * dz + dy * dy * 2 <= 150) set(level, x + dx, crown + dy, z + dz, Blocks.AZALEA_LEAVES);
                }
            }
        }
    }

    private static void elvenLodge(ServerLevel level, int x, int y, int z, int radius) {
        flattenLot(level, x - radius - 3, z - radius - 3, x + radius + 3, z + radius + 3, y, Blocks.MOSS_BLOCK);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 <= radius * radius) set(level, x + dx, y, z + dz, Blocks.STRIPPED_BIRCH_WOOD);
                if (d2 >= (radius - 2) * (radius - 2) && d2 <= radius * radius) {
                    for (int dy = 1; dy <= 5; dy++) set(level, x + dx, y + dy, z + dz, Blocks.BIRCH_PLANKS);
                }
            }
        }
        for (int dy = 6; dy <= 10; dy++) {
            int r = Math.max(2, radius + 4 - dy);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz <= r * r) set(level, x + dx, y + dy, z + dz, Blocks.AZALEA_LEAVES);
                }
            }
        }
        clear(level, x - 1, y + 1, z - radius, x + 1, y + 3, z - radius);
        set(level, x, y + 3, z, Blocks.SOUL_LANTERN);
    }

    private static void supportedPlatform(ServerLevel level, int x, int y, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius) set(level, x + dx, y, z + dz, Blocks.STRIPPED_BIRCH_WOOD);
            }
        }
        int floorY = surfaceY(level, x, z);
        for (int[] support : new int[][]{{-radius / 2, 0}, {radius / 2, 0}, {0, -radius / 2}, {0, radius / 2}}) {
            for (int sy = floorY + 1; sy < y; sy++) set(level, x + support[0], sy, z + support[1], Blocks.DARK_OAK_LOG);
        }
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI * 2.0 / 12.0;
            int px = x + (int) Math.round(Math.cos(angle) * (radius - 1));
            int pz = z + (int) Math.round(Math.sin(angle) * (radius - 1));
            set(level, px, y + 1, pz, Blocks.OAK_FENCE);
            if ((i & 2) == 0) set(level, px, y + 2, pz, Blocks.LANTERN);
        }
    }

    private static void canopyBridge(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            for (int side = -1; side <= 1; side++) set(level, x + side, y, z, Blocks.DARK_OAK_PLANKS);
            if (i % 4 == 0) {
                set(level, x - 2, y + 1, z, Blocks.OAK_FENCE);
                set(level, x + 2, y + 1, z, Blocks.OAK_FENCE);
            }
        }
    }

    private static void moonGarden(ServerLevel level, int x, int y, int z) {
        flattenLot(level, x - 12, z - 12, x + 12, z + 12, y, Blocks.MOSS_BLOCK);
        for (int dx = -9; dx <= 9; dx++) {
            for (int dz = -9; dz <= 9; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 <= 49) set(level, x + dx, y, z + dz, Blocks.WATER);
                else if (d2 <= 81) set(level, x + dx, y, z + dz, Blocks.MOSSY_STONE_BRICKS);
            }
        }
        set(level, x, y - 1, z, Blocks.GLOWSTONE);
    }

    private static void dwarvenHall(ServerLevel level, int x, int y, int z, int width, int depth) {
        flattenLot(level, x - 4, z - 4, x + width + 3, z + depth + 3, y, Blocks.POLISHED_ANDESITE);
        fill(level, x, y, z, x + width - 1, y, z + depth - 1, Blocks.POLISHED_ANDESITE);
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                if (dx != 0 && dx != width - 1 && dz != 0 && dz != depth - 1) continue;
                for (int dy = 1; dy <= 7; dy++) {
                    set(level, x + dx, y + dy, z + dz,
                            dy <= 2 ? Blocks.DEEPSLATE_BRICKS : Blocks.STONE_BRICKS);
                }
            }
        }
        solidGableRoof(level, x, y + 8, z, width, depth, Blocks.DEEPSLATE_TILES);
        clear(level, x + width / 2 - 1, y + 1, z, x + width / 2 + 1, y + 4, z);
        set(level, x + 3, y + 3, z + 2, Blocks.LANTERN);
        set(level, x + width - 4, y + 3, z + 2, Blocks.LANTERN);
    }

    private static void dwarvenForge(ServerLevel level, int x, int y, int z) {
        dwarvenHall(level, x, y, z, 19, 14);
        fill(level, x + 4, y + 1, z + 4, x + 14, y + 1, z + 9, Blocks.IRON_BLOCK);
        set(level, x + 6, y + 2, z + 6, Blocks.BLAST_FURNACE);
        set(level, x + 10, y + 2, z + 6, Blocks.ANVIL);
        set(level, x + 14, y + 2, z + 7, Blocks.LAVA);
    }

    private static void dwarvenGate(ServerLevel level, int x, int y, int z) {
        flattenLot(level, x - 12, z - 4, x + 12, z + 28, y, Blocks.POLISHED_ANDESITE);
        clear(level, x - 5, y + 1, z, x + 5, y + 11, z + 24);
        for (int dz = 0; dz <= 24; dz++) {
            for (int dx = -7; dx <= 7; dx++) {
                set(level, x + dx, y, z + dz, Blocks.POLISHED_DEEPSLATE);
                if (Math.abs(dx) >= 5) {
                    for (int dy = 1; dy <= 10; dy++) set(level, x + dx, y + dy, z + dz, Blocks.DEEPSLATE_BRICKS);
                }
            }
            if (dz % 6 == 0) {
                set(level, x - 4, y + 4, z + dz, Blocks.LANTERN);
                set(level, x + 4, y + 4, z + dz, Blocks.LANTERN);
            }
        }
    }

    private static void stoneRoad(ServerLevel level, int x1, int z1, int x2, int z2, int halfWidth) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            boolean xMajor = Math.abs(x2 - x1) >= Math.abs(z2 - z1);
            for (int side = -halfWidth; side <= halfWidth; side++) {
                int px = xMajor ? x : x + side;
                int pz = xMajor ? z + side : z;
                clear(level, px, 66, pz, px, 74, pz);
                set(level, px, 65, pz, Math.abs(side) == halfWidth ? Blocks.COBBLESTONE : Blocks.STONE_BRICKS);
            }
        }
    }

    private static void flattenSquare(ServerLevel level, int cx, int cz, int radius, int y, Block surface) {
        flattenLot(level, cx - radius, cz - radius, cx + radius, cz + radius, y, surface);
    }

    private static void blendSquareEdge(ServerLevel level, int cx, int cz, int inner, int outer, int y, Block surface) {
        for (int x = cx - outer; x <= cx + outer; x++) {
            for (int z = cz - outer; z <= cz + outer; z++) {
                int d = Math.max(Math.abs(x - cx), Math.abs(z - cz));
                if (d <= inner || d > outer) continue;
                int target = y - Math.min(1, (d - inner + 11) / 12);
                reshapeColumn(level, x, z, target, surface);
            }
        }
    }

    private static void blendCircle(ServerLevel level, int cx, int cz, int radius, int y, Block surface) {
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                int dx = x - cx;
                int dz = z - cz;
                if (dx * dx + dz * dz > radius * radius) continue;
                double wave = Math.sin(x * 0.075) * 1.6 + Math.cos(z * 0.067) * 1.4;
                reshapeColumn(level, x, z, y + (int) Math.round(wave), surface);
            }
        }
    }

    private static void flattenLot(ServerLevel level, int x1, int z1, int x2, int z2, int y, Block surface) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) reshapeColumn(level, x, z, y, surface);
        }
        for (int ring = 1; ring <= 8; ring++) {
            int target = Math.max(64, y - (ring + 2) / 3);
            for (int x = minX - ring; x <= maxX + ring; x++) {
                reshapeColumn(level, x, minZ - ring, target, surface);
                reshapeColumn(level, x, maxZ + ring, target, surface);
            }
            for (int z = minZ - ring; z <= maxZ + ring; z++) {
                reshapeColumn(level, minX - ring, z, target, surface);
                reshapeColumn(level, maxX + ring, z, target, surface);
            }
        }
    }

    private static void reshapeColumn(ServerLevel level, int x, int z, int targetY, Block surface) {
        int oldY = surfaceY(level, x, z);
        if (oldY > targetY) clear(level, x, targetY + 1, z, x, oldY, z);
        if (oldY < targetY) fill(level, x, oldY + 1, z, x, targetY - 1, z, Blocks.DIRT);
        set(level, x, targetY, z, surface);
        clear(level, x, targetY + 1, z, x, Math.max(targetY + 1, Math.min(108, oldY + 18)), z);
    }

    private static void plantedTree(ServerLevel level, int x, int y, int z, int height, Block log, Block leaves) {
        for (int dy = 1; dy <= height; dy++) set(level, x, y + dy, z, log);
        int crown = y + height;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    if (dx * dx + dz * dz + dy * dy <= 7) set(level, x + dx, crown + dy, z + dz, leaves);
                }
            }
        }
    }

    private static int surfaceY(ServerLevel level, int x, int z) {
        for (int y = 120; y >= 60; y--) {
            if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) return y;
        }
        return 64;
    }

    private static void clear(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2) {
        fill(level, x1, y1, z1, x2, y2, z2, Blocks.AIR);
    }

    private static void fill(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        if (y2 < y1) return;
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) set(level, x, y, z, block);
            }
        }
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 3);
    }
}
