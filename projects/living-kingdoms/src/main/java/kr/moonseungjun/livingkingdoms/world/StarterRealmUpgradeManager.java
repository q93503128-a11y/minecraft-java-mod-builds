package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Idempotent authored-world migrations for early playtest saves.
 *
 * <p>The first test realm was intentionally tiny and used a flat generator. This repair pass keeps
 * player profiles intact while replacing buried lots, widening each settlement and blending the
 * hand-authored terrain back into the base plane.</p>
 */
public final class StarterRealmUpgradeManager {
    private static final int CURRENT_REVISION = 2;

    private StarterRealmUpgradeManager() {
    }

    public static void ensureForPlayer(ServerPlayer player, OriginProfile profile) {
        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) {
            return;
        }
        ensureRegion(realm, profile.homelandId());
    }

    public static synchronized void ensureRegion(ServerLevel level, String homelandId) {
        StarterRealmUpgradeSavedData state = level.getDataStorage().computeIfAbsent(StarterRealmUpgradeSavedData.TYPE);
        if (state.revision(homelandId) >= CURRENT_REVISION) {
            return;
        }

        switch (homelandId) {
            case "silvana_forest" -> upgradeSilvana(level);
            case "kardum_league" -> upgradeKardum(level);
            default -> upgradeErden(level);
        }

        state.setRevision(homelandId, CURRENT_REVISION);
        LivingKingdoms.LOGGER.info("Upgraded authored homeland {} to revision {}", homelandId, CURRENT_REVISION);
    }

    private static void upgradeErden(ServerLevel level) {
        sculptErdenOutskirts(level);

        // Replace the first prototype houses with level, drained lots and a coherent frontier palette.
        erdenHouse(level, 7, 65, 5, 11, 9);
        erdenHouse(level, -30, 65, -26, 10, 9);
        erdenHouse(level, 21, 65, -28, 11, 9);
        erdenHouse(level, -32, 65, 22, 10, 9);
        erdenHouse(level, 43, 65, 18, 10, 8);
        erdenHouse(level, -58, 65, 10, 10, 8);
        erdenHouse(level, 40, 65, -55, 10, 8);
        erdenHouse(level, -54, 65, -52, 10, 8);

        erdenInn(level, 23, 65, 36);
        erdenSmithy(level, -24, 65, 39);
        erdenBarracksAndJail(level, -13, 65, -66);
        erdenMarket(level, 0, 65, 0);
        erdenPalisade(level, 76);

        // Existing remote starts are rebuilt on broad pads rather than left inside dirt cuttings.
        erdenHouse(level, 104, 65, 66, 12, 10);
        farmland(level, 83, 65, 61, 18, 18);
        erdenHouse(level, -112, 65, 85, 9, 8);
        fishingPier(level, -116, 65, 95);
        travellerCamp(level, 82, 65, -116);

        road(level, 0, 0, 110, 72, 3, Blocks.GRAVEL, Blocks.COBBLESTONE);
        road(level, 0, 0, -108, 90, 3, Blocks.GRAVEL, Blocks.COBBLESTONE);
        road(level, 0, 0, 84, -112, 3, Blocks.GRAVEL, Blocks.COBBLESTONE);

        for (int i = 0; i < 54; i++) {
            double angle = i * 2.399963229728653;
            int radius = 92 + (i % 7) * 16;
            int x = (int) Math.round(Math.cos(angle) * radius);
            int z = (int) Math.round(Math.sin(angle) * radius);
            if (protectedErden(x, z)) continue;
            int y = surfaceY(level, x, z);
            if (y >= 65 && y <= 80) {
                tree(level, x, y + 1, z, i % 4 == 0 ? Blocks.BIRCH_LOG : Blocks.OAK_LOG,
                        i % 4 == 0 ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES, 4 + i % 3);
            }
        }
    }

    private static void sculptErdenOutskirts(ServerLevel level) {
        int radius = 220;
        for (int x = -radius; x <= radius; x += 2) {
            for (int z = -radius; z <= radius; z += 2) {
                double distance = Math.sqrt((double) x * x + (double) z * z);
                if (distance < 78 || distance > radius || protectedErden(x, z)) continue;
                double inner = Math.min(1.0, (distance - 78.0) / 42.0);
                double edge = Math.min(1.0, (radius - distance) / 48.0);
                double wave = 4.8 + Math.sin(x * 0.033) * 3.0 + Math.cos(z * 0.029) * 2.4
                        + Math.sin((x + z) * 0.018) * 2.0;
                int top = 64 + Math.max(0, (int) Math.round(wave * inner * edge));
                terrainPatch(level, x, z, top, Blocks.GRASS_BLOCK, Blocks.DIRT);
            }
        }
    }

    private static boolean protectedErden(int x, int z) {
        if (Math.abs(x) <= 84 && Math.abs(z) <= 84) return true;
        if (x >= 76 && x <= 132 && z >= 48 && z <= 96) return true;
        if (x >= -132 && x <= -86 && z >= 60 && z <= 116) return true;
        if (x >= 62 && x <= 108 && z >= -136 && z <= -92) return true;
        return Math.abs(x + 120) <= 14 && z >= 42 && z <= 136;
    }

    private static void upgradeSilvana(ServerLevel level) {
        int cx = 1240;
        int cz = 35;
        for (int x = cx - 190; x <= cx + 190; x += 2) {
            for (int z = cz - 190; z <= cz + 190; z += 2) {
                int dx = x - cx;
                int dz = z - cz;
                double distance = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (distance < 72 || distance > 190) continue;
                double edge = Math.min(1.0, (190.0 - distance) / 45.0);
                double inner = Math.min(1.0, (distance - 72.0) / 38.0);
                double wave = 7.0 + Math.sin(x * 0.026) * 4.0 + Math.cos(z * 0.031) * 3.0;
                int top = 64 + Math.max(0, (int) Math.round(wave * edge * inner));
                terrainPatch(level, x, z, top, Blocks.MOSS_BLOCK, Blocks.DIRT);
            }
        }

        elvenLodge(level, 1268, 66, 28);
        elvenLodge(level, 1190, 67, 62);
        elvenLodge(level, 1308, 66, -4);
        elvenPlatform(level, 1240, 78, 35, 13);
        elvenPlatform(level, 1188, 76, 4, 9);
        elvenPlatform(level, 1300, 77, 54, 9);
        canopyBridge(level, 1240, 78, 35, 1188, 76, 4);
        canopyBridge(level, 1240, 78, 35, 1300, 77, 54);

        for (int i = 0; i < 72; i++) {
            double angle = i * 2.399963229728653;
            int radius = 76 + (i % 9) * 13;
            int x = cx + (int) Math.round(Math.cos(angle) * radius);
            int z = cz + (int) Math.round(Math.sin(angle) * radius);
            int y = surfaceY(level, x, z);
            if (y >= 65 && y <= 86) {
                tree(level, x, y + 1, z, Blocks.DARK_OAK_LOG,
                        i % 3 == 0 ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.DARK_OAK_LEAVES,
                        6 + i % 5);
            }
        }
    }

    private static void upgradeKardum(ServerLevel level) {
        int cx = -1170;
        int cz = 38;
        for (int x = cx - 175; x <= cx + 175; x += 2) {
            for (int z = cz - 175; z <= cz + 175; z += 2) {
                int dx = x - cx;
                int dz = z - cz;
                double distance = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (distance < 74 || distance > 175) continue;
                double ridgeA = Math.max(0.0, 1.0 - Math.abs(distance - 118.0) / 48.0);
                double ridgeB = Math.max(0.0, Math.sin((x - z) * 0.025));
                int top = 65 + (int) Math.round(ridgeA * (14.0 + ridgeB * 12.0));
                terrainPatch(level, x, z, top, top <= 70 ? Blocks.GRASS_BLOCK : Blocks.STONE,
                        top <= 70 ? Blocks.DIRT : Blocks.STONE);
            }
        }

        dwarvenHall(level, -1238, 67, 46, 15, 12);
        dwarvenHall(level, -1108, 67, 54, 15, 12);
        dwarvenForge(level, -1168, 67, -10);
        dwarvenTerrace(level, -1170, 66, 38, 48);
        road(level, -1248, 38, -1092, 38, 4, Blocks.POLISHED_ANDESITE, Blocks.DEEPSLATE_BRICKS);
    }

    private static void erdenHouse(ServerLevel level, int x, int y, int z, int width, int depth) {
        prepareLot(level, x - 4, z - 4, x + width + 3, z + depth + 3, y, Blocks.GRASS_BLOCK);
        fill(level, x, y, z, x + width - 1, y, z + depth - 1, Blocks.STONE_BRICKS);
        for (int ix = 0; ix < width; ix++) {
            for (int iz = 0; iz < depth; iz++) {
                if (ix != 0 && iz != 0 && ix != width - 1 && iz != depth - 1) continue;
                for (int dy = 1; dy <= 4; dy++) {
                    boolean beam = ix == 0 || ix == width - 1 || iz == 0 || iz == depth - 1;
                    boolean corner = (ix == 0 || ix == width - 1) && (iz == 0 || iz == depth - 1);
                    Block wall = corner || (beam && (dy == 1 || dy == 4))
                            ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.WHITE_TERRACOTTA;
                    set(level, x + ix, y + dy, z + iz, wall);
                }
            }
        }
        int doorX = x + width / 2;
        set(level, doorX, y + 1, z, Blocks.AIR);
        set(level, doorX, y + 2, z, Blocks.AIR);
        set(level, x + 2, y + 2, z, Blocks.GLASS_PANE);
        set(level, x + width - 3, y + 2, z, Blocks.GLASS_PANE);
        set(level, x + 2, y + 2, z + depth - 1, Blocks.GLASS_PANE);
        set(level, x + width - 3, y + 2, z + depth - 1, Blocks.GLASS_PANE);

        int layers = Math.max(3, Math.min(5, depth / 2 + 1));
        for (int layer = 0; layer < layers; layer++) {
            int front = z - 1 + layer;
            int back = z + depth - layer;
            for (int ix = -1; ix <= width; ix++) {
                set(level, x + ix, y + 5 + layer, front, Blocks.DARK_OAK_PLANKS);
                set(level, x + ix, y + 5 + layer, back, Blocks.DARK_OAK_PLANKS);
            }
        }
        fill(level, x + 1, y + 1, z + 1, x + width - 2, y + 1, z + depth - 2, Blocks.SPRUCE_PLANKS);
        set(level, x + width - 2, y + 2, z + depth - 2, Blocks.LANTERN);
        for (int dy = 1; dy <= 7; dy++) set(level, x + width - 2, y + dy, z + depth - 2, Blocks.BRICKS);
    }

    private static void erdenInn(ServerLevel level, int x, int y, int z) {
        erdenHouse(level, x, y, z, 17, 12);
        fill(level, x + 1, y + 5, z + 1, x + 15, y + 5, z + 10, Blocks.SPRUCE_PLANKS);
        for (int dx = 2; dx <= 14; dx += 4) {
            set(level, x + dx, y + 3, z, Blocks.GLASS_PANE);
            set(level, x + dx, y + 3, z + 11, Blocks.GLASS_PANE);
        }
        set(level, x + 8, y + 2, z + 3, Blocks.CAMPFIRE);
    }

    private static void erdenSmithy(ServerLevel level, int x, int y, int z) {
        erdenHouse(level, x, y, z, 12, 9);
        prepareLot(level, x + 12, z, x + 22, z + 10, y, Blocks.STONE_BRICKS);
        fill(level, x + 14, y + 1, z + 2, x + 20, y + 1, z + 8, Blocks.COBBLESTONE);
        set(level, x + 16, y + 2, z + 4, Blocks.BLAST_FURNACE);
        set(level, x + 18, y + 2, z + 4, Blocks.ANVIL);
        set(level, x + 20, y + 2, z + 7, Blocks.LAVA);
    }

    private static void erdenBarracksAndJail(ServerLevel level, int x, int y, int z) {
        prepareLot(level, x - 4, z - 4, x + 28, z + 18, y, Blocks.STONE_BRICKS);
        fill(level, x, y, z, x + 24, y, z + 14, Blocks.STONE_BRICKS);
        for (int dx = 0; dx <= 24; dx++) {
            for (int dz = 0; dz <= 14; dz++) {
                if (dx != 0 && dx != 24 && dz != 0 && dz != 14) continue;
                for (int dy = 1; dy <= 6; dy++) {
                    set(level, x + dx, y + dy, z + dz,
                            dy <= 2 ? Blocks.STONE_BRICKS : Blocks.SPRUCE_PLANKS);
                }
            }
        }
        fill(level, x - 1, y + 7, z - 1, x + 25, y + 7, z + 15, Blocks.DARK_OAK_PLANKS);
        set(level, x + 12, y + 1, z + 14, Blocks.AIR);
        set(level, x + 12, y + 2, z + 14, Blocks.AIR);
        for (int dz = 2; dz <= 12; dz += 5) {
            for (int dy = 1; dy <= 4; dy++) set(level, x + 18, y + dy, z + dz, Blocks.IRON_BARS);
        }
        set(level, x + 21, y + 2, z + 7, Blocks.LANTERN);
    }

    private static void erdenMarket(ServerLevel level, int cx, int y, int cz) {
        prepareLot(level, cx - 18, cz - 18, cx + 18, cz + 18, y, Blocks.STONE_BRICKS);
        for (int[] offset : new int[][]{{-12, -8}, {7, -8}, {-12, 8}, {7, 8}}) {
            int x = cx + offset[0];
            int z = cz + offset[1];
            fill(level, x, y + 1, z, x + 5, y + 1, z + 3, Blocks.SPRUCE_PLANKS);
            for (int dx : new int[]{0, 5}) {
                for (int dy = 2; dy <= 4; dy++) set(level, x + dx, y + dy, z, Blocks.OAK_FENCE);
            }
            fill(level, x, y + 5, z - 1, x + 5, y + 5, z + 4, Blocks.RED_WOOL);
            set(level, x + 2, y + 2, z + 2, Blocks.BARREL);
        }
        for (int dy = 1; dy <= 5; dy++) set(level, cx, y + dy, cz, Blocks.CHISELED_STONE_BRICKS);
        set(level, cx, y + 6, cz, Blocks.LANTERN);
    }

    private static void erdenPalisade(ServerLevel level, int radius) {
        for (int x = -radius; x <= radius; x += 3) {
            palisadePost(level, x, 65, -radius);
            palisadePost(level, x, 65, radius);
        }
        for (int z = -radius; z <= radius; z += 3) {
            palisadePost(level, -radius, 65, z);
            palisadePost(level, radius, 65, z);
        }
        // Four open gates keep the frontier town connected to its satellite livelihoods.
        clear(level, -5, 66, -radius, 5, 72, -radius);
        clear(level, -5, 66, radius, 5, 72, radius);
        clear(level, -radius, 66, -5, -radius, 72, 5);
        clear(level, radius, 66, -5, radius, 72, 5);
    }

    private static void palisadePost(ServerLevel level, int x, int y, int z) {
        for (int dy = 1; dy <= 5; dy++) set(level, x, y + dy, z, Blocks.STRIPPED_SPRUCE_LOG);
    }

    private static void farmland(ServerLevel level, int x, int y, int z, int width, int depth) {
        prepareLot(level, x - 2, z - 2, x + width + 1, z + depth + 1, y, Blocks.GRASS_BLOCK);
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                if (dx % 6 == 0) {
                    set(level, x + dx, y, z + dz, Blocks.WATER);
                } else {
                    set(level, x + dx, y, z + dz, Blocks.FARMLAND);
                    if ((dx + dz) % 3 != 0) set(level, x + dx, y + 1, z + dz, Blocks.WHEAT);
                }
            }
        }
    }

    private static void fishingPier(ServerLevel level, int x, int y, int z) {
        prepareLot(level, x - 3, z - 3, x + 18, z + 12, y, Blocks.GRASS_BLOCK);
        for (int dx = 0; dx <= 18; dx++) {
            set(level, x + dx, y, z, Blocks.SPRUCE_PLANKS);
            if (dx % 4 == 0) set(level, x + dx, y - 1, z, Blocks.SPRUCE_LOG);
        }
        for (int dz = 1; dz <= 10; dz++) {
            set(level, x + 18, y, z + dz, Blocks.SPRUCE_PLANKS);
        }
        set(level, x + 6, y + 1, z + 1, Blocks.BARREL);
    }

    private static void travellerCamp(ServerLevel level, int x, int y, int z) {
        prepareLot(level, x - 10, z - 10, x + 10, z + 10, y, Blocks.COARSE_DIRT);
        set(level, x, y + 1, z, Blocks.CAMPFIRE);
        for (int[] p : new int[][]{{-7, -5}, {6, -4}, {-4, 6}}) {
            int px = x + p[0];
            int pz = z + p[1];
            for (int dy = 1; dy <= 4; dy++) {
                set(level, px, y + dy, pz, Blocks.SPRUCE_LOG);
                set(level, px + 5, y + dy, pz, Blocks.SPRUCE_LOG);
            }
            fill(level, px, y + 5, pz, px + 5, y + 5, pz + 4, Blocks.GREEN_WOOL);
        }
    }

    private static void elvenLodge(ServerLevel level, int x, int y, int z) {
        prepareLot(level, x - 10, z - 10, x + 10, z + 10, y, Blocks.MOSS_BLOCK);
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

    private static void elvenPlatform(ServerLevel level, int x, int y, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius) {
                    set(level, x + dx, y, z + dz, Blocks.STRIPPED_BIRCH_WOOD);
                }
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

    private static void canopyBridge(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            for (int side = -1; side <= 1; side++) set(level, x + side, y, z, Blocks.DARK_OAK_PLANKS);
        }
    }

    private static void dwarvenHall(ServerLevel level, int x, int y, int z, int width, int depth) {
        prepareLot(level, x - 4, z - 4, x + width + 3, z + depth + 3, y, Blocks.POLISHED_ANDESITE);
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
        for (int layer = 0; layer <= 4; layer++) {
            fill(level, x - 1 + layer, y + 8 + layer, z - 1,
                    x + width - layer, y + 8 + layer, z + depth, Blocks.DEEPSLATE_TILES);
        }
        clear(level, x + width / 2 - 1, y + 1, z, x + width / 2 + 1, y + 4, z);
        set(level, x + 3, y + 3, z + 2, Blocks.LANTERN);
        set(level, x + width - 4, y + 3, z + 2, Blocks.LANTERN);
    }

    private static void dwarvenForge(ServerLevel level, int x, int y, int z) {
        dwarvenHall(level, x, y, z, 18, 13);
        fill(level, x + 4, y + 1, z + 4, x + 13, y + 1, z + 8, Blocks.CUT_COPPER);
        set(level, x + 6, y + 2, z + 6, Blocks.BLAST_FURNACE);
        set(level, x + 9, y + 2, z + 6, Blocks.ANVIL);
        set(level, x + 12, y + 2, z + 6, Blocks.LAVA);
    }

    private static void dwarvenTerrace(ServerLevel level, int cx, int y, int cz, int radius) {
        for (int r = radius; r >= 12; r -= 12) {
            int terraceY = y + (radius - r) / 12 * 3;
            for (int x = cx - r; x <= cx + r; x++) {
                for (int z = cz - r; z <= cz + r; z++) {
                    if (Math.abs(x - cx) == r || Math.abs(z - cz) == r) {
                        set(level, x, terraceY, z, Blocks.DEEPSLATE_BRICKS);
                    }
                }
            }
        }
    }

    private static void prepareLot(ServerLevel level, int x1, int z1, int x2, int z2, int y, Block surface) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        clear(level, minX, y + 1, minZ, maxX, y + 18, maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                fill(level, x, 61, z, x, y - 1, z, Blocks.DIRT);
                set(level, x, y, z, surface);
            }
        }
        // A two-block retaining shoulder prevents the sheer dirt walls seen in the first playtest.
        for (int ring = 1; ring <= 4; ring++) {
            int shoulderY = Math.max(64, y - (ring + 1) / 2);
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
        clear(level, x, y + 1, z, x, y + 8, z);
        fill(level, x, 61, z, x, y - 1, z, Blocks.DIRT);
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
                clear(level, px, 66, pz, px, 70, pz);
                set(level, px, 65, pz, Math.abs(side) == halfWidth ? edge : center);
            }
        }
    }

    private static void terrainPatch(ServerLevel level, int x, int z, int topY, Block surface, Block filler) {
        for (int px = x; px <= x + 1; px++) {
            for (int pz = z; pz <= z + 1; pz++) {
                clear(level, px, topY + 1, pz, px, Math.max(topY + 1, 86), pz);
                if (topY > 64) fill(level, px, 65, pz, px, topY - 1, pz, filler);
                set(level, px, topY, pz, surface);
            }
        }
    }

    private static int surfaceY(ServerLevel level, int x, int z) {
        for (int y = 120; y >= 60; y--) {
            if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) return y;
        }
        return 64;
    }

    private static void tree(ServerLevel level, int x, int y, int z, Block log, Block leaves, int height) {
        for (int dy = 0; dy < height; dy++) set(level, x, y + dy, z, log);
        int crownY = y + height - 1;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    if (dx * dx + dz * dz + dy * dy <= 7) set(level, x + dx, crownY + dy, z + dz, leaves);
                }
            }
        }
    }

    private static void clear(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2) {
        fill(level, x1, y1, z1, x2, y2, z2, Blocks.AIR);
    }

    private static void fill(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    set(level, x, y, z, block);
                }
            }
        }
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 3);
    }
}
