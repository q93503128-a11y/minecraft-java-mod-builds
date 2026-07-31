package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Fast authored-world repair pipeline.
 *
 * <p>Revision three replaces the prototype flat staging areas without rewriting every block column
 * from scratch. Terrain writes are differential, four-by-four patches are blended at their edges,
 * and every homeland owns a distinct building palette.</p>
 */
public final class AuthoredRealmManager {
    public static final int CURRENT_REVISION = 3;

    private AuthoredRealmManager() {
    }

    public static void ensureForPlayer(ServerPlayer player, OriginProfile profile) {
        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm != null) ensureRegion(realm, profile.homelandId());
    }

    public static synchronized void ensureRegion(ServerLevel level, String homelandId) {
        StarterRealmUpgradeSavedData data = level.getDataStorage().computeIfAbsent(StarterRealmUpgradeSavedData.TYPE);
        if (data.revision(homelandId) >= CURRENT_REVISION) return;

        switch (homelandId) {
            case "silvana_forest" -> silvana(level);
            case "kardum_league" -> kardum(level);
            default -> erden(level);
        }
        data.setRevision(homelandId, CURRENT_REVISION);
        LivingKingdoms.LOGGER.info("Authored homeland {} upgraded to revision {}", homelandId, CURRENT_REVISION);
    }

    private static void erden(ServerLevel level) {
        terrainField(level, 0, 0, 80, 220, 64, 12, Blocks.GRASS_BLOCK, Blocks.DIRT, 0.031, 0.024);

        int[][] homes = {
                {7, 5, 11, 9}, {-30, -26, 10, 9}, {21, -28, 11, 9}, {-32, 22, 10, 9},
                {43, 18, 10, 8}, {-58, 10, 10, 8}, {40, -55, 10, 8}, {-54, -52, 10, 8},
                {104, 66, 12, 10}, {-112, 85, 9, 8}
        };
        for (int[] home : homes) erdenHouse(level, home[0], 65, home[1], home[2], home[3]);

        erdenMarket(level, 0, 65, 0);
        erdenInn(level, 23, 65, 36);
        erdenSmithy(level, -24, 65, 39);
        erdenBarracks(level, -13, 65, -66);
        farm(level, 83, 65, 61, 18, 18);
        pier(level, -116, 65, 95);
        camp(level, 82, 65, -116);
        palisade(level, 76);
        road(level, 0, 0, 110, 72, 3, Blocks.GRAVEL, Blocks.COBBLESTONE);
        road(level, 0, 0, -108, 90, 3, Blocks.GRAVEL, Blocks.COBBLESTONE);
        road(level, 0, 0, 84, -112, 3, Blocks.GRAVEL, Blocks.COBBLESTONE);
        trees(level, 0, 0, 90, 210, 54, Blocks.OAK_LOG, Blocks.OAK_LEAVES, 4);
    }

    private static void silvana(ServerLevel level) {
        int cx = 1240;
        int cz = 35;
        terrainField(level, cx, cz, 72, 205, 64, 19, Blocks.MOSS_BLOCK, Blocks.DIRT, 0.025, 0.031);
        giantTree(level, 1240, 65, 35, 5, 21);
        elvenLodge(level, 1268, 66, 28);
        elvenLodge(level, 1190, 67, 62);
        elvenLodge(level, 1308, 66, -4);
        platform(level, 1240, 82, 35, 13);
        platform(level, 1188, 78, 4, 9);
        platform(level, 1300, 79, 54, 9);
        bridge(level, 1240, 82, 35, 1188, 78, 4);
        bridge(level, 1240, 82, 35, 1300, 79, 54);
        moonGarden(level, 1275, 66, 82);
        trees(level, cx, cz, 74, 200, 76, Blocks.DARK_OAK_LOG, Blocks.FLOWERING_AZALEA_LEAVES, 7);
    }

    private static void kardum(ServerLevel level) {
        int cx = -1170;
        int cz = 38;
        mountainField(level, cx, cz, 72, 190);
        terraces(level, cx, 66, cz, 54);
        dwarvenHall(level, -1238, 67, 46, 15, 12);
        dwarvenHall(level, -1108, 67, 54, 15, 12);
        dwarvenForge(level, -1168, 67, -10);
        dwarvenGate(level, -1202, 67, 2);
        road(level, -1248, 38, -1092, 38, 4, Blocks.POLISHED_ANDESITE, Blocks.DEEPSLATE_BRICKS);
    }

    private static void terrainField(ServerLevel level, int cx, int cz, int inner, int outer, int baseY,
                                     int amplitude, Block surface, Block filler, double fx, double fz) {
        for (int x = cx - outer; x <= cx + outer; x += 4) {
            for (int z = cz - outer; z <= cz + outer; z += 4) {
                int dx = x - cx;
                int dz = z - cz;
                double distance = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (distance < inner || distance > outer) continue;
                double innerBlend = Math.min(1.0, (distance - inner) / 48.0);
                double outerBlend = Math.min(1.0, (outer - distance) / 55.0);
                double wave = 0.50 + Math.sin(x * fx) * 0.25 + Math.cos(z * fz) * 0.20
                        + Math.sin((x + z) * 0.016) * 0.15;
                int top = baseY + Math.max(0, (int) Math.round(amplitude * wave * innerBlend * outerBlend));
                deltaPatch(level, x, z, top, surface, filler);
            }
        }
    }

    private static void mountainField(ServerLevel level, int cx, int cz, int inner, int outer) {
        for (int x = cx - outer; x <= cx + outer; x += 4) {
            for (int z = cz - outer; z <= cz + outer; z += 4) {
                int dx = x - cx;
                int dz = z - cz;
                double distance = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (distance < inner || distance > outer) continue;
                double ridge = Math.max(0.0, 1.0 - Math.abs(distance - 124.0) / 56.0);
                double fracture = 0.60 + Math.max(0.0, Math.sin((x - z) * 0.028)) * 0.78;
                int top = 65 + (int) Math.round(ridge * 27.0 * fracture);
                deltaPatch(level, x, z, top, top <= 70 ? Blocks.GRASS_BLOCK : Blocks.STONE,
                        top <= 70 ? Blocks.DIRT : Blocks.STONE);
            }
        }
    }

    private static void deltaPatch(ServerLevel level, int startX, int startZ, int targetY,
                                   Block surface, Block filler) {
        for (int x = startX; x < startX + 4; x++) {
            for (int z = startZ; z < startZ + 4; z++) {
                int oldY = surfaceY(level, x, z);
                if (oldY > targetY) clear(level, x, targetY + 1, z, x, oldY, z);
                if (oldY < targetY) fill(level, x, oldY + 1, z, x, targetY - 1, z, filler);
                set(level, x, targetY, z, surface);
            }
        }
    }

    private static void erdenHouse(ServerLevel level, int x, int y, int z, int width, int depth) {
        lot(level, x - 4, z - 4, x + width + 3, z + depth + 3, y, Blocks.GRASS_BLOCK);
        fill(level, x, y, z, x + width - 1, y, z + depth - 1, Blocks.STONE_BRICKS);
        for (int ix = 0; ix < width; ix++) {
            for (int iz = 0; iz < depth; iz++) {
                if (ix != 0 && ix != width - 1 && iz != 0 && iz != depth - 1) continue;
                for (int dy = 1; dy <= 4; dy++) {
                    boolean corner = (ix == 0 || ix == width - 1) && (iz == 0 || iz == depth - 1);
                    boolean beam = dy == 1 || dy == 4 || ix % 4 == 0;
                    set(level, x + ix, y + dy, z + iz,
                            corner || beam ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.BIRCH_PLANKS);
                }
            }
        }
        clear(level, x + width / 2, y + 1, z, x + width / 2, y + 2, z);
        set(level, x + 2, y + 2, z, Blocks.GLASS_PANE);
        set(level, x + width - 3, y + 2, z, Blocks.GLASS_PANE);
        int roofLayers = Math.max(3, Math.min(5, depth / 2 + 1));
        for (int layer = 0; layer < roofLayers; layer++) {
            int front = z - 1 + layer;
            int back = z + depth - layer;
            for (int ix = -1; ix <= width; ix++) {
                set(level, x + ix, y + 5 + layer, front, Blocks.DARK_OAK_PLANKS);
                set(level, x + ix, y + 5 + layer, back, Blocks.DARK_OAK_PLANKS);
            }
        }
        fill(level, x + 1, y + 1, z + 1, x + width - 2, y + 1, z + depth - 2, Blocks.SPRUCE_PLANKS);
        set(level, x + width - 2, y + 2, z + depth - 2, Blocks.LANTERN);
    }

    private static void erdenInn(ServerLevel level, int x, int y, int z) {
        erdenHouse(level, x, y, z, 17, 12);
        fill(level, x + 1, y + 5, z + 1, x + 15, y + 5, z + 10, Blocks.SPRUCE_PLANKS);
        set(level, x + 8, y + 2, z + 3, Blocks.CAMPFIRE);
        set(level, x + 3, y + 2, z + 3, Blocks.BARREL);
    }

    private static void erdenSmithy(ServerLevel level, int x, int y, int z) {
        erdenHouse(level, x, y, z, 12, 9);
        lot(level, x + 12, z, x + 22, z + 10, y, Blocks.STONE_BRICKS);
        fill(level, x + 14, y + 1, z + 2, x + 20, y + 1, z + 8, Blocks.COBBLESTONE);
        set(level, x + 16, y + 2, z + 4, Blocks.BLAST_FURNACE);
        set(level, x + 18, y + 2, z + 4, Blocks.ANVIL);
        set(level, x + 20, y + 2, z + 7, Blocks.LAVA);
    }

    private static void erdenBarracks(ServerLevel level, int x, int y, int z) {
        lot(level, x - 4, z - 4, x + 28, z + 18, y, Blocks.STONE_BRICKS);
        fill(level, x, y, z, x + 24, y, z + 14, Blocks.STONE_BRICKS);
        for (int dx = 0; dx <= 24; dx++) {
            for (int dz = 0; dz <= 14; dz++) {
                if (dx != 0 && dx != 24 && dz != 0 && dz != 14) continue;
                for (int dy = 1; dy <= 6; dy++) set(level, x + dx, y + dy, z + dz,
                        dy <= 2 ? Blocks.STONE_BRICKS : Blocks.SPRUCE_PLANKS);
            }
        }
        fill(level, x - 1, y + 7, z - 1, x + 25, y + 7, z + 15, Blocks.DARK_OAK_PLANKS);
        clear(level, x + 11, y + 1, z + 14, x + 13, y + 3, z + 14);
        for (int dz = 2; dz <= 12; dz += 5) {
            for (int dy = 1; dy <= 4; dy++) set(level, x + 18, y + dy, z + dz, Blocks.IRON_BARS);
        }
    }

    private static void erdenMarket(ServerLevel level, int cx, int y, int cz) {
        lot(level, cx - 18, cz - 18, cx + 18, cz + 18, y, Blocks.STONE_BRICKS);
        for (int[] offset : new int[][]{{-12, -8}, {7, -8}, {-12, 8}, {7, 8}}) {
            int x = cx + offset[0];
            int z = cz + offset[1];
            fill(level, x, y + 1, z, x + 5, y + 1, z + 3, Blocks.SPRUCE_PLANKS);
            for (int dx : new int[]{0, 5}) {
                for (int dy = 2; dy <= 4; dy++) set(level, x + dx, y + dy, z, Blocks.OAK_FENCE);
            }
            fill(level, x, y + 5, z - 1, x + 5, y + 5, z + 4, Blocks.BRICKS);
            set(level, x + 2, y + 2, z + 2, Blocks.BARREL);
        }
        for (int dy = 1; dy <= 5; dy++) set(level, cx, y + dy, cz, Blocks.CHISELED_STONE_BRICKS);
        set(level, cx, y + 6, cz, Blocks.LANTERN);
    }

    private static void farm(ServerLevel level, int x, int y, int z, int width, int depth) {
        lot(level, x - 2, z - 2, x + width + 1, z + depth + 1, y, Blocks.GRASS_BLOCK);
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

    private static void pier(ServerLevel level, int x, int y, int z) {
        lot(level, x - 3, z - 3, x + 18, z + 12, y, Blocks.GRASS_BLOCK);
        for (int dx = 0; dx <= 18; dx++) {
            set(level, x + dx, y, z, Blocks.SPRUCE_PLANKS);
            if (dx % 4 == 0) set(level, x + dx, y - 1, z, Blocks.SPRUCE_LOG);
        }
        for (int dz = 1; dz <= 10; dz++) set(level, x + 18, y, z + dz, Blocks.SPRUCE_PLANKS);
        set(level, x + 6, y + 1, z + 1, Blocks.BARREL);
    }

    private static void camp(ServerLevel level, int x, int y, int z) {
        lot(level, x - 10, z - 10, x + 10, z + 10, y, Blocks.COARSE_DIRT);
        set(level, x, y + 1, z, Blocks.CAMPFIRE);
        for (int[] p : new int[][]{{-7, -5}, {6, -4}, {-4, 6}}) {
            int px = x + p[0];
            int pz = z + p[1];
            for (int dy = 1; dy <= 4; dy++) {
                set(level, px, y + dy, pz, Blocks.SPRUCE_LOG);
                set(level, px + 5, y + dy, pz, Blocks.SPRUCE_LOG);
            }
            fill(level, px, y + 5, pz, px + 5, y + 5, pz + 4, Blocks.MOSS_BLOCK);
        }
    }

    private static void palisade(ServerLevel level, int radius) {
        for (int x = -radius; x <= radius; x += 3) {
            post(level, x, 65, -radius);
            post(level, x, 65, radius);
        }
        for (int z = -radius; z <= radius; z += 3) {
            post(level, -radius, 65, z);
            post(level, radius, 65, z);
        }
        clear(level, -5, 66, -radius, 5, 72, -radius);
        clear(level, -5, 66, radius, 5, 72, radius);
        clear(level, -radius, 66, -5, -radius, 72, 5);
        clear(level, radius, 66, -5, radius, 72, 5);
    }

    private static void post(ServerLevel level, int x, int y, int z) {
        for (int dy = 1; dy <= 5; dy++) set(level, x, y + dy, z, Blocks.STRIPPED_SPRUCE_LOG);
    }

    private static void giantTree(ServerLevel level, int x, int y, int z, int radius, int height) {
        for (int dy = 0; dy <= height; dy++) {
            int currentRadius = Math.max(2, radius - dy / 7);
            for (int dx = -currentRadius; dx <= currentRadius; dx++) {
                for (int dz = -currentRadius; dz <= currentRadius; dz++) {
                    if (dx * dx + dz * dz <= currentRadius * currentRadius) set(level, x + dx, y + dy, z + dz, Blocks.DARK_OAK_LOG);
                }
            }
        }
        int crownY = y + height;
        for (int dx = -11; dx <= 11; dx++) {
            for (int dz = -11; dz <= 11; dz++) {
                for (int dy = -3; dy <= 4; dy++) {
                    if (dx * dx + dz * dz + dy * dy * 2 <= 125) set(level, x + dx, crownY + dy, z + dz, Blocks.AZALEA_LEAVES);
                }
            }
        }
    }

    private static void elvenLodge(ServerLevel level, int x, int y, int z) {
        lot(level, x - 10, z - 10, x + 10, z + 10, y, Blocks.MOSS_BLOCK);
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 <= 64) set(level, x + dx, y, z + dz, Blocks.STRIPPED_BIRCH_WOOD);
                if (d2 >= 42 && d2 <= 64) {
                    for (int dy = 1; dy <= 5; dy++) set(level, x + dx, y + dy, z + dz, Blocks.BIRCH_PLANKS);
                }
            }
        }
        for (int dy = 6; dy <= 10; dy++) {
            int radius = 11 - dy;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz <= radius * radius) set(level, x + dx, y + dy, z + dz, Blocks.AZALEA_LEAVES);
                }
            }
        }
        clear(level, x - 1, y + 1, z - 8, x + 1, y + 3, z - 8);
        set(level, x, y + 3, z, Blocks.SOUL_LANTERN);
    }

    private static void platform(ServerLevel level, int x, int y, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius) set(level, x + dx, y, z + dz, Blocks.STRIPPED_BIRCH_WOOD);
            }
        }
        for (int i = 0; i < 10; i++) {
            double angle = i * Math.PI * 2.0 / 10.0;
            int px = x + (int) Math.round(Math.cos(angle) * (radius - 1));
            int pz = z + (int) Math.round(Math.sin(angle) * (radius - 1));
            set(level, px, y + 1, pz, Blocks.OAK_FENCE);
            set(level, px, y + 2, pz, Blocks.LANTERN);
        }
    }

    private static void bridge(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            for (int side = -1; side <= 1; side++) set(level, x + side, y, z, Blocks.DARK_OAK_PLANKS);
        }
    }

    private static void moonGarden(ServerLevel level, int x, int y, int z) {
        lot(level, x - 11, z - 11, x + 11, z + 11, y, Blocks.MOSS_BLOCK);
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 <= 49) set(level, x + dx, y, z + dz, Blocks.WATER);
                else if (d2 <= 64) set(level, x + dx, y, z + dz, Blocks.MOSSY_STONE_BRICKS);
            }
        }
        set(level, x, y - 1, z, Blocks.GLOWSTONE);
    }

    private static void dwarvenHall(ServerLevel level, int x, int y, int z, int width, int depth) {
        lot(level, x - 4, z - 4, x + width + 3, z + depth + 3, y, Blocks.POLISHED_ANDESITE);
        fill(level, x, y, z, x + width - 1, y, z + depth - 1, Blocks.POLISHED_ANDESITE);
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                if (dx != 0 && dx != width - 1 && dz != 0 && dz != depth - 1) continue;
                for (int dy = 1; dy <= 7; dy++) set(level, x + dx, y + dy, z + dz,
                        dy <= 2 ? Blocks.DEEPSLATE_BRICKS : Blocks.STONE_BRICKS);
            }
        }
        for (int layer = 0; layer <= 4; layer++) fill(level, x - 1 + layer, y + 8 + layer, z - 1,
                x + width - layer, y + 8 + layer, z + depth, Blocks.DEEPSLATE_TILES);
        clear(level, x + width / 2 - 1, y + 1, z, x + width / 2 + 1, y + 4, z);
        set(level, x + 3, y + 3, z + 2, Blocks.LANTERN);
        set(level, x + width - 4, y + 3, z + 2, Blocks.LANTERN);
    }

    private static void dwarvenForge(ServerLevel level, int x, int y, int z) {
        dwarvenHall(level, x, y, z, 18, 13);
        fill(level, x + 4, y + 1, z + 4, x + 13, y + 1, z + 8, Blocks.IRON_BLOCK);
        set(level, x + 6, y + 2, z + 6, Blocks.BLAST_FURNACE);
        set(level, x + 9, y + 2, z + 6, Blocks.ANVIL);
        set(level, x + 12, y + 2, z + 6, Blocks.LAVA);
    }

    private static void dwarvenGate(ServerLevel level, int x, int y, int z) {
        lot(level, x - 12, z - 4, x + 12, z + 28, y, Blocks.POLISHED_ANDESITE);
        clear(level, x - 5, y + 1, z, x + 5, y + 10, z + 24);
        for (int dz = 0; dz <= 24; dz++) {
            for (int dx = -7; dx <= 7; dx++) {
                set(level, x + dx, y, z + dz, Blocks.POLISHED_DEEPSLATE);
                if (Math.abs(dx) >= 5) {
                    for (int dy = 1; dy <= 9; dy++) set(level, x + dx, y + dy, z + dz, Blocks.DEEPSLATE_BRICKS);
                }
            }
            if (dz % 6 == 0) {
                set(level, x - 4, y + 4, z + dz, Blocks.LANTERN);
                set(level, x + 4, y + 4, z + dz, Blocks.LANTERN);
            }
        }
    }

    private static void terraces(ServerLevel level, int cx, int y, int cz, int radius) {
        for (int r = radius; r >= 14; r -= 12) {
            int terraceY = y + (radius - r) / 12 * 3;
            for (int x = cx - r; x <= cx + r; x++) {
                for (int z = cz - r; z <= cz + r; z++) {
                    if (Math.abs(x - cx) == r || Math.abs(z - cz) == r) set(level, x, terraceY, z, Blocks.DEEPSLATE_BRICKS);
                }
            }
        }
    }

    private static void trees(ServerLevel level, int cx, int cz, int minRadius, int maxRadius, int count,
                              Block log, Block leaves, int baseHeight) {
        int span = Math.max(1, maxRadius - minRadius);
        for (int i = 0; i < count; i++) {
            double angle = i * 2.399963229728653;
            int radius = minRadius + Math.floorMod(i * 37, span);
            int x = cx + (int) Math.round(Math.cos(angle) * radius);
            int z = cz + (int) Math.round(Math.sin(angle) * radius);
            int y = surfaceY(level, x, z);
            if (y < 64 || y > 96) continue;
            for (int dy = 1; dy <= baseHeight + i % 4; dy++) set(level, x, y + dy, z, log);
            int crown = y + baseHeight + i % 4;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = -1; dy <= 2; dy++) {
                        if (dx * dx + dz * dz + dy * dy <= 7) set(level, x + dx, crown + dy, z + dz, leaves);
                    }
                }
            }
        }
    }

    private static void lot(ServerLevel level, int x1, int z1, int x2, int z2, int y, Block surface) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        clear(level, minX, y + 1, minZ, maxX, y + 18, maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int oldY = surfaceY(level, x, z);
                if (oldY > y) clear(level, x, y + 1, z, x, oldY, z);
                if (oldY < y) fill(level, x, oldY + 1, z, x, y - 1, z, Blocks.DIRT);
                set(level, x, y, z, surface);
            }
        }
        for (int ring = 1; ring <= 5; ring++) {
            int shoulderY = Math.max(63, y - (ring + 1) / 2);
            for (int x = minX - ring; x <= maxX + ring; x++) {
                shoulder(level, x, shoulderY, minZ - ring, surface);
                shoulder(level, x, shoulderY, maxZ + ring, surface);
            }
            for (int z = minZ - ring; z <= maxZ + ring; z++) {
                shoulder(level, minX - ring, shoulderY, z, surface);
                shoulder(level, maxX + ring, shoulderY, z, surface);
            }
        }
    }

    private static void shoulder(ServerLevel level, int x, int y, int z, Block surface) {
        int oldY = surfaceY(level, x, z);
        if (oldY > y) clear(level, x, y + 1, z, x, oldY, z);
        if (oldY < y) fill(level, x, oldY + 1, z, x, y - 1, z, Blocks.DIRT);
        set(level, x, y, z, surface);
    }

    private static void road(ServerLevel level, int x1, int z1, int x2, int z2, int halfWidth,
                             Block center, Block edge) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            boolean xMajor = Math.abs(x2 - x1) >= Math.abs(z2 - z1);
            for (int side = -halfWidth; side <= halfWidth; side++) {
                int px = xMajor ? x : x + side;
                int pz = xMajor ? z + side : z;
                clear(level, px, 66, pz, px, Math.max(72, surfaceY(level, px, pz)), pz);
                set(level, px, 65, pz, Math.abs(side) == halfWidth ? edge : center);
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
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.getBlockState(pos).is(block)) level.setBlock(pos, block.defaultBlockState(), 2);
    }
}
