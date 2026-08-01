package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Adds the unglamorous infrastructure that makes a fantasy settlement believable: water access,
 * controlled gates, drainage, public lighting, storage, ventilation and maintained work yards.
 * Every placement is audited against PlannedRealmBuilder's occupied lots.
 */
public final class RealmCivicRealismFinisher {
    public static final int REVISION = 2;
    private static final int FLAGS = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;

    private RealmCivicRealismFinisher() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel realm = event.getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null || realm.getGameTime() % 200L != 0L) return;
        CivicInfrastructureSavedData saved = realm.getDataStorage().computeIfAbsent(CivicInfrastructureSavedData.TYPE);
        for (String homeland : List.of("erden_kingdom", "silvana_forest", "kardum_league")) {
            RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(realm, homeland);
            if (site == null || !site.built() || !saved.needs(homeland, REVISION)) continue;
            switch (homeland) {
                case "silvana_forest" -> finishSilvana(realm, site);
                case "kardum_league" -> finishKardum(realm, site);
                default -> finishErden(realm, site);
            }
            saved.mark(homeland, REVISION);
            LivingKingdoms.LOGGER.info("Applied civic realism revision {} to {}", REVISION, homeland);
        }
    }

    private static void finishErden(ServerLevel level, RealmSiteLayoutSavedData.RealmSite site) {
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = clamp(site.baseY(), 68, 112);

        // East of the market lot and west of the inn frontage: visible, central and not destructive.
        publicWell(level, cx + 32, y, cz + 8, Blocks.STONE_BRICKS, Blocks.SMOOTH_STONE);
        customsHouse(level, cx - 13, y, cz - 97);
        customsHouse(level, cx + 12, y, cz + 94);

        for (int z = cz - 82; z <= cz + 82; z += 18) {
            streetLamp(level, cx - 7, z, Blocks.SPRUCE_FENCE);
            streetLamp(level, cx + 7, z, Blocks.SPRUCE_FENCE);
        }
        for (int x = cx - 100; x <= cx + 100; x += 20) {
            streetLamp(level, x, cz - 7, Blocks.SPRUCE_FENCE);
            streetLamp(level, x, cz + 7, Blocks.SPRUCE_FENCE);
        }

        // Covered runoff channels follow the central road, but stop before the market square.
        for (int z = cz - 88; z <= cz + 88; z++) {
            if (Math.abs(z - cz) <= 27) continue;
            set(level, cx - 6, y, z, Blocks.SMOOTH_STONE);
            set(level, cx + 6, y, z, Blocks.SMOOTH_STONE);
            if (Math.floorMod(z - cz, 12) == 0) {
                set(level, cx - 6, y + 1, z, Blocks.IRON_BARS);
                set(level, cx + 6, y + 1, z, Blocks.IRON_BARS);
            }
        }

        // Rear service spaces behind the southern house row, before the east-west service road.
        int[] houseOrigins = {-56, -32, -8, 20, 49};
        for (int houseX : houseOrigins) {
            int serviceX = cx + houseX + 4;
            set(level, serviceX, y + 1, cz + 45, Blocks.COMPOSTER);
            set(level, serviceX + 2, y + 1, cz + 45, Blocks.BARREL);
            for (int z = cz + 43; z <= cz + 51; z++) {
                set(level, cx + houseX - 3, y + 1, z, Blocks.OAK_LEAVES);
            }
        }

        // South of the existing granary, clear of houses and the inner wall.
        granaryServiceYard(level, cx + 82, y, cz + 91);
    }

    private static void finishSilvana(ServerLevel level, RealmSiteLayoutSavedData.RealmSite site) {
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = clamp(site.baseY(), 70, 122);

        publicWell(level, cx + 28, y, cz - 55, Blocks.MOSSY_COBBLESTONE, Blocks.MOSS_BLOCK);
        for (int[] point : new int[][]{{22, 18}, {-24, 22}, {38, -18}, {-42, -9}, {61, 42}, {-62, 42}}) {
            livingLantern(level, cx + point[0], cz + point[1]);
        }

        // A small rain pond between the central tree and southern lodge/council lots.
        rainCatchment(level, cx - 16, y, cz + 70);
        herbDryingYard(level, cx + 24, y, cz + 69);
        for (int i = 0; i < 5; i++) {
            set(level, cx + 4 + i * 4, y + 1, cz + 61, Blocks.COMPOSTER);
        }
    }

    private static void finishKardum(ServerLevel level, RealmSiteLayoutSavedData.RealmSite site) {
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = clamp(site.baseY(), 74, 138);

        mountainCistern(level, cx + 28, y + 2, cz + 67);
        for (int[] point : new int[][]{{-96, -82}, {96, -82}, {-96, 88}, {96, 88}}) {
            ventilationStack(level, cx + point[0], cz + point[1]);
        }
        for (int z = cz - 70; z <= cz + 78; z += 18) {
            basaltLamp(level, cx - 7, cz + (z - cz));
            basaltLamp(level, cx + 7, cz + (z - cz));
        }

        // Complete the existing ore yard rather than placing a second yard on top of it.
        oreYardLedger(level, cx + 94, y + 1, cz + 72);

        // Raised forge begins at y+3; a base-level nonflammable apron and outside water points are safe.
        for (int x = cx - 24; x <= cx + 10; x++) {
            set(level, x, y, cz + 34, Blocks.POLISHED_DEEPSLATE);
            set(level, x, y, cz + 59, Blocks.POLISHED_DEEPSLATE);
        }
        for (int z = cz + 34; z <= cz + 59; z++) {
            set(level, cx - 24, y, z, Blocks.POLISHED_DEEPSLATE);
            set(level, cx + 10, y, z, Blocks.POLISHED_DEEPSLATE);
        }
        for (int z : new int[]{cz + 40, cz + 48, cz + 56}) {
            set(level, cx + 13, y + 1, z, Blocks.CAULDRON);
        }
    }

    private static void publicWell(ServerLevel level, int cx, int y, int cz, Block rim, Block floor) {
        fill(level, cx - 3, y, cz - 3, cx + 3, y, cz + 3, floor);
        for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) {
            boolean edge = Math.abs(x - cx) == 2 || Math.abs(z - cz) == 2;
            set(level, x, y + 1, z, edge ? rim : Blocks.WATER);
        }
        for (int[] corner : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            set(level, cx + corner[0], y + 2, cz + corner[1], Blocks.OAK_FENCE);
            set(level, cx + corner[0], y + 3, cz + corner[1], Blocks.OAK_FENCE);
        }
        fill(level, cx - 3, y + 4, cz - 3, cx + 3, y + 4, cz + 3, Blocks.DARK_OAK_SLAB);
        set(level, cx, y + 3, cz, Blocks.IRON_CHAIN);
        set(level, cx, y + 2, cz, Blocks.CAULDRON);
    }

    private static void customsHouse(ServerLevel level, int x, int y, int z) {
        fill(level, x - 4, y, z - 3, x + 4, y, z + 3, Blocks.STONE_BRICKS);
        for (int px = x - 4; px <= x + 4; px++) for (int pz = z - 3; pz <= z + 3; pz++) {
            if (px != x - 4 && px != x + 4 && pz != z - 3 && pz != z + 3) continue;
            for (int py = y + 1; py <= y + 4; py++) set(level, px, py, pz, Blocks.STRIPPED_SPRUCE_LOG);
        }
        clear(level, x - 1, y + 1, z - 3, x + 1, y + 3, z - 3);
        fill(level, x - 5, y + 5, z - 4, x + 5, y + 5, z + 4, Blocks.DARK_OAK_SLAB);
        set(level, x + 2, y + 1, z, Blocks.BARREL);
        set(level, x - 2, y + 1, z, Blocks.LECTERN);
    }

    private static void granaryServiceYard(ServerLevel level, int x, int y, int z) {
        fill(level, x - 7, y, z - 4, x + 7, y, z + 4, Blocks.PACKED_MUD);
        for (int px = x - 5; px <= x + 5; px += 5) {
            set(level, px, y + 1, z - 2, Blocks.BARREL);
            set(level, px, y + 1, z + 2, Blocks.HAY_BLOCK);
        }
        streetLamp(level, x - 7, z - 4, Blocks.SPRUCE_FENCE);
        streetLamp(level, x + 7, z + 4, Blocks.SPRUCE_FENCE);
    }

    private static void rainCatchment(ServerLevel level, int cx, int y, int cz) {
        for (int x = cx - 4; x <= cx + 4; x++) {
            for (int z = cz - 4; z <= cz + 4; z++) {
                boolean edge = Math.abs(x - cx) == 4 || Math.abs(z - cz) == 4;
                set(level, x, y, z, edge ? Blocks.MOSSY_STONE_BRICKS : Blocks.WATER);
            }
        }
    }

    private static void herbDryingYard(ServerLevel level, int x, int y, int z) {
        for (int i = 0; i < 4; i++) {
            int px = x + i * 4;
            set(level, px, y + 1, z, Blocks.SPRUCE_FENCE);
            set(level, px, y + 2, z, Blocks.SPRUCE_FENCE);
            set(level, px, y + 3, z, Blocks.DARK_OAK_SLAB);
            set(level, px, y + 1, z + 2, Blocks.BARREL);
        }
    }

    private static void livingLantern(ServerLevel level, int x, int z) {
        int ground = RealmSitePlanner.surfaceY(level, x, z) + 1;
        for (int py = ground; py <= ground + 3; py++) set(level, x, py, z, Blocks.DARK_OAK_FENCE);
        set(level, x, ground + 4, z, Blocks.SOUL_LANTERN);
        set(level, x - 1, ground + 3, z, Blocks.FLOWERING_AZALEA_LEAVES);
        set(level, x + 1, ground + 3, z, Blocks.FLOWERING_AZALEA_LEAVES);
    }

    private static void mountainCistern(ServerLevel level, int cx, int y, int cz) {
        fill(level, cx - 5, y - 2, cz - 5, cx + 5, y - 1, cz + 5, Blocks.POLISHED_DEEPSLATE);
        fill(level, cx - 4, y, cz - 4, cx + 4, y, cz + 4, Blocks.WATER);
        for (int x = cx - 5; x <= cx + 5; x++) for (int z = cz - 5; z <= cz + 5; z++) {
            if (Math.abs(x - cx) == 5 || Math.abs(z - cz) == 5) set(level, x, y, z, Blocks.DEEPSLATE_BRICKS);
        }
        set(level, cx, y + 1, cz, Blocks.IRON_CHAIN);
    }

    private static void ventilationStack(ServerLevel level, int x, int z) {
        int ground = RealmSitePlanner.surfaceY(level, x, z) + 1;
        fill(level, x - 2, ground, z - 2, x + 2, ground, z + 2, Blocks.POLISHED_DEEPSLATE);
        for (int py = ground + 1; py <= ground + 8; py++) {
            for (int[] edge : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
                set(level, x + edge[0], py, z + edge[1], Blocks.POLISHED_BASALT);
            }
        }
        fill(level, x - 2, ground + 9, z - 2, x + 2, ground + 9, z + 2, Blocks.DEEPSLATE_TILE_SLAB);
    }

    private static void basaltLamp(ServerLevel level, int x, int z) {
        int ground = RealmSitePlanner.surfaceY(level, x, z) + 1;
        for (int py = ground; py <= ground + 3; py++) set(level, x, py, z, Blocks.POLISHED_BASALT);
        set(level, x, ground + 4, z, Blocks.LANTERN);
    }

    private static void oreYardLedger(ServerLevel level, int x, int y, int z) {
        set(level, x, y + 1, z, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE);
        set(level, x - 2, y + 1, z + 2, Blocks.LECTERN);
        set(level, x + 2, y + 1, z + 2, Blocks.BARREL);
        set(level, x + 4, y + 1, z + 2, Blocks.RAW_IRON_BLOCK);
    }

    private static void streetLamp(ServerLevel level, int x, int z, Block post) {
        int ground = RealmSitePlanner.surfaceY(level, x, z) + 1;
        for (int py = ground; py <= ground + 3; py++) set(level, x, py, z, post);
        set(level, x, ground + 4, z, Blocks.LANTERN);
    }

    private static void fill(ServerLevel level, int x1, int y1, int z1,
                             int x2, int y2, int z2, Block block) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(level, x, y, z, block);
            }
        }
    }

    private static void clear(ServerLevel level, int x1, int y1, int z1,
                              int x2, int y2, int z2) {
        fill(level, x1, y1, z1, x2, y2, z2, Blocks.AIR);
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        BlockPos pos = new BlockPos(x, y, z);
        if (y < level.getMinY() || y >= level.getMaxY()) return;
        if (level.getBlockState(pos).getBlock() == block) return;
        level.setBlock(pos, block.defaultBlockState(), FLAGS);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
