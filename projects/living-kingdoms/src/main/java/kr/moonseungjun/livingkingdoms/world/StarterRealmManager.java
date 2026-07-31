package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

public final class StarterRealmManager {
    public static final ResourceKey<Level> REALM_KEY = ResourceKey.create(
            Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "living_realm")
    );

    private StarterRealmManager() {
    }

    public static boolean placePlayer(ServerPlayer player, OriginProfile profile) {
        PlayableOriginCatalog.ResidenceOption residence = PlayableOriginCatalog.residences().get(profile.residenceId());
        if (residence == null) {
            LivingKingdoms.LOGGER.error("Missing playable residence {} for player {}", profile.residenceId(), player.getGameProfile().name());
            return false;
        }

        ServerLevel realm = player.level().getServer().getLevel(REALM_KEY);
        if (realm == null) {
            LivingKingdoms.LOGGER.error("Living Kingdoms realm is not loaded");
            player.sendSystemMessage(Component.literal("판타지 세계를 불러오지 못했습니다. 서버 로그를 확인하십시오."));
            return false;
        }

        ensureHomeland(realm, residence.homelandId());
        prepareSpawn(realm, residence);

        boolean moved = player.teleportTo(
                realm,
                residence.spawnX() + 0.5,
                residence.spawnY(),
                residence.spawnZ() + 0.5,
                Set.<Relative>of(),
                player.getYRot(),
                player.getXRot(),
                true
        );
        if (!moved) {
            LivingKingdoms.LOGGER.error("Failed to teleport player {} into Living Kingdoms realm", player.getGameProfile().name());
            return false;
        }

        player.sendSystemMessage(Component.literal(
                "§6[살아있는 왕국] §f" + residence.displayName() + "에서 새로운 삶을 시작합니다."
        ));
        return true;
    }

    private static synchronized void ensureHomeland(ServerLevel realm, String homelandId) {
        StarterRealmSavedData state = realm.getDataStorage().computeIfAbsent(StarterRealmSavedData.TYPE);
        if (state.isGenerated(homelandId)) {
            return;
        }

        switch (homelandId) {
            case "silvana_forest" -> buildSilvana(realm);
            case "kardum_league" -> buildKardum(realm);
            default -> buildErden(realm);
        }

        state.markGenerated(homelandId);
        LivingKingdoms.LOGGER.info("Generated authored starter homeland {}", homelandId);
    }

    private static void buildErden(ServerLevel level) {
        int cx = 0;
        int cz = 0;
        sculptRollingGround(level, cx, cz, 150, Blocks.GRASS_BLOCK, Blocks.DIRT, 3);
        carveRiver(level, -118, -25, -118, 125, 7);
        buildRoad(level, cx, cz, 0, 126, true);
        buildRoad(level, cx, cz, -126, 126, false);
        buildPlaza(level, cx, cz, 15, Blocks.STONE_BRICKS);
        buildTimberHouse(level, -28, 67, -24, 9, 8);
        buildTimberHouse(level, 22, 67, -26, 10, 8);
        buildTimberHouse(level, -30, 67, 24, 9, 8);
        buildTimberHouse(level, 24, 67, 25, 10, 8);
        buildTimberHouse(level, 104, 66, 68, 11, 9);
        buildFishingHut(level, -108, 66, 88);
        buildCamp(level, 82, 66, -116);
        buildWatchTower(level, 0, 66, -58);
        addErdenTrees(level, cx, cz);
    }

    private static void buildSilvana(ServerLevel level) {
        int cx = 1240;
        int cz = 35;
        sculptRollingGround(level, cx, cz, 145, Blocks.MOSS_BLOCK, Blocks.DIRT, 5);
        buildMoonwell(level, 1290, 66, 84);
        buildGiantTree(level, 1210, 65, 8, 7, 25);
        buildGiantTree(level, 1160, 65, -42, 5, 20);
        buildGiantTree(level, 1280, 65, -54, 5, 21);
        buildGiantTree(level, 1320, 65, 28, 5, 19);
        buildCanopyWalk(level, 1210, 84, 8, 1290, 74, 84);
        buildElvenLodge(level, 1284, 67, 78);
        addSilvanaTrees(level, cx, cz);
    }

    private static void buildKardum(ServerLevel level) {
        int cx = -1170;
        int cz = 38;
        sculptMountainGround(level, cx, cz, 150);
        carveMountainGate(level, -1200, 67, 0);
        buildStoneHall(level, -1214, 67, 4);
        buildStoneHall(level, -1132, 71, 88);
        buildRoad(level, -1170, 38, -1300, -1040, false);
        buildForgeYard(level, -1165, 67, 45);
        buildWatchTower(level, -1080, 67, 18);
    }

    private static void sculptRollingGround(
            ServerLevel level,
            int centerX,
            int centerZ,
            int radius,
            Block surface,
            Block filler,
            int amplitude
    ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = centerX - radius; x <= centerX + radius; x += 2) {
            for (int z = centerZ - radius; z <= centerZ + radius; z += 2) {
                double distance = Math.sqrt((double) (x - centerX) * (x - centerX) + (double) (z - centerZ) * (z - centerZ));
                if (distance > radius) continue;
                int height = 64 + (int) Math.round(
                        Math.sin(x * 0.045) * amplitude * 0.55
                                + Math.cos(z * 0.038) * amplitude * 0.45
                                + Math.sin((x + z) * 0.018) * amplitude * 0.35
                );
                paintColumn2x2(level, pos, x, z, height, surface, filler);
            }
        }
    }

    private static void sculptMountainGround(ServerLevel level, int centerX, int centerZ, int radius) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = centerX - radius; x <= centerX + radius; x += 2) {
            for (int z = centerZ - radius; z <= centerZ + radius; z += 2) {
                double dx = x - centerX;
                double dz = z - centerZ;
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius) continue;
                int height = 65 + (int) Math.max(0, 24 - distance * 0.15)
                        + (int) Math.round(Math.abs(Math.sin(x * 0.04) * 7 + Math.cos(z * 0.05) * 5));
                paintColumn2x2(level, pos, x, z, height, Blocks.STONE, Blocks.STONE);
                if (distance > 95) {
                    set(level, x, height, z, Blocks.GRASS_BLOCK);
                }
            }
        }
    }

    private static void paintColumn2x2(
            ServerLevel level,
            BlockPos.MutableBlockPos pos,
            int x,
            int z,
            int topY,
            Block surface,
            Block filler
    ) {
        for (int ox = 0; ox < 2; ox++) {
            for (int oz = 0; oz < 2; oz++) {
                int px = x + ox;
                int pz = z + oz;
                for (int y = 65; y <= topY; y++) {
                    set(level, pos, px, y, pz, y == topY ? surface : filler);
                }
                for (int y = topY + 1; y <= 90; y++) {
                    set(level, pos, px, y, pz, Blocks.AIR);
                }
            }
        }
    }

    private static void carveRiver(ServerLevel level, int x, int z1, int ignoredX2, int z2, int halfWidth) {
        for (int z = z1; z <= z2; z++) {
            int bendX = x + (int) Math.round(Math.sin(z * 0.05) * 10);
            for (int dx = -halfWidth; dx <= halfWidth; dx++) {
                int depth = 4 - Math.min(3, Math.abs(dx) / 2);
                for (int y = 65; y >= 65 - depth; y--) {
                    set(level, bendX + dx, y, z, y <= 63 ? Blocks.WATER : Blocks.AIR);
                }
                set(level, bendX + dx, 60, z, Blocks.GRAVEL);
            }
        }
    }

    private static void buildRoad(ServerLevel level, int centerX, int centerZ, int min, int max, boolean alongZ) {
        for (int value = min; value <= max; value++) {
            for (int width = -2; width <= 2; width++) {
                int x = alongZ ? centerX + width : value;
                int z = alongZ ? value : centerZ + width;
                set(level, x, 65, z, Math.abs(width) == 2 ? Blocks.COBBLESTONE : Blocks.GRAVEL);
                set(level, x, 66, z, Blocks.AIR);
                set(level, x, 67, z, Blocks.AIR);
            }
        }
    }

    private static void buildPlaza(ServerLevel level, int cx, int cz, int radius, Block floor) {
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                if ((x - cx) * (x - cx) + (z - cz) * (z - cz) <= radius * radius) {
                    set(level, x, 65, z, floor);
                    set(level, x, 66, z, Blocks.AIR);
                    set(level, x, 67, z, Blocks.AIR);
                }
            }
        }
        for (int y = 66; y <= 71; y++) set(level, cx, y, cz, Blocks.CHISELED_STONE_BRICKS);
        set(level, cx, 72, cz, Blocks.LANTERN);
    }

    private static void buildTimberHouse(ServerLevel level, int x, int y, int z, int width, int depth) {
        clearBox(level, x - 1, y, z - 1, x + width, y + 8, z + depth);
        fillFloor(level, x, y, z, width, depth, Blocks.SPRUCE_PLANKS);
        for (int ix = 0; ix < width; ix++) {
            for (int iz = 0; iz < depth; iz++) {
                boolean wall = ix == 0 || iz == 0 || ix == width - 1 || iz == depth - 1;
                if (!wall) continue;
                for (int iy = 1; iy <= 4; iy++) {
                    Block block = (ix == 0 || ix == width - 1) && (iz == 0 || iz == depth - 1)
                            ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.SPRUCE_PLANKS;
                    set(level, x + ix, y + iy, z + iz, block);
                }
            }
        }
        int doorX = x + width / 2;
        set(level, doorX, y + 1, z, Blocks.AIR);
        set(level, doorX, y + 2, z, Blocks.AIR);
        for (int layer = 0; layer <= 3; layer++) {
            for (int ix = -1 + layer; ix <= width - layer; ix++) {
                set(level, x + ix, y + 5 + layer, z - 1, Blocks.DARK_OAK_PLANKS);
                set(level, x + ix, y + 5 + layer, z + depth, Blocks.DARK_OAK_PLANKS);
            }
        }
        set(level, x + 2, y + 2, z, Blocks.GLASS_PANE);
        set(level, x + width - 3, y + 2, z, Blocks.GLASS_PANE);
        set(level, x + width / 2, y + 2, z + depth - 2, Blocks.LANTERN);
    }

    private static void buildFishingHut(ServerLevel level, int x, int y, int z) {
        buildTimberHouse(level, x, y, z, 8, 7);
        for (int dx = -3; dx <= 11; dx++) {
            set(level, x + dx, y, z + 10, Blocks.SPRUCE_PLANKS);
            if (dx % 4 == 0) set(level, x + dx, y - 1, z + 10, Blocks.SPRUCE_LOG);
        }
        set(level, x + 4, y + 1, z + 8, Blocks.BARREL);
    }

    private static void buildCamp(ServerLevel level, int x, int y, int z) {
        clearBox(level, x - 8, y, z - 8, x + 8, y + 8, z + 8);
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                set(level, x + dx, y, z + dz, Blocks.COARSE_DIRT);
            }
        }
        set(level, x, y + 1, z, Blocks.CAMPFIRE);
        for (int side : new int[]{-5, 5}) {
            for (int height = 1; height <= 4; height++) {
                set(level, x + side, y + height, z - 4, Blocks.SPRUCE_LOG);
                set(level, x + side, y + height, z + 4, Blocks.SPRUCE_LOG);
            }
        }
        for (int dx = -5; dx <= 5; dx++) {
            set(level, x + dx, y + 5, z - 4, Blocks.GREEN_WOOL);
            set(level, x + dx, y + 5, z + 4, Blocks.GREEN_WOOL);
        }
    }

    private static void buildWatchTower(ServerLevel level, int x, int y, int z) {
        for (int dy = 0; dy <= 12; dy++) {
            for (int ox : new int[]{-3, 3}) {
                for (int oz : new int[]{-3, 3}) {
                    set(level, x + ox, y + dy, z + oz, Blocks.STRIPPED_DARK_OAK_LOG);
                }
            }
        }
        for (int ox = -4; ox <= 4; ox++) {
            for (int oz = -4; oz <= 4; oz++) {
                set(level, x + ox, y + 10, z + oz, Blocks.DARK_OAK_PLANKS);
            }
        }
        set(level, x, y + 11, z, Blocks.LANTERN);
    }

    private static void buildGiantTree(ServerLevel level, int x, int y, int z, int radius, int height) {
        for (int dy = 0; dy <= height; dy++) {
            double taper = 1.0 - (double) dy / (height * 1.5);
            int currentRadius = Math.max(2, (int) Math.round(radius * taper));
            for (int dx = -currentRadius; dx <= currentRadius; dx++) {
                for (int dz = -currentRadius; dz <= currentRadius; dz++) {
                    if (dx * dx + dz * dz <= currentRadius * currentRadius) {
                        set(level, x + dx, y + dy, z + dz, Blocks.DARK_OAK_LOG);
                    }
                }
            }
        }
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                for (int dy = -3; dy <= 5; dy++) {
                    if (dx * dx + dz * dz + dy * dy * 2 <= 145) {
                        set(level, x + dx, y + height + dy, z + dz, Blocks.AZALEA_LEAVES);
                    }
                }
            }
        }
        clearBox(level, x - 2, y + 2, z - 2, x + 2, y + 8, z + 2);
        fillFloor(level, x - 2, y + 1, z - 2, 5, 5, Blocks.MOSS_BLOCK);
        set(level, x, y + 3, z, Blocks.LANTERN);
    }

    private static void buildMoonwell(ServerLevel level, int x, int y, int z) {
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 <= 49) {
                    set(level, x + dx, y, z + dz, d2 >= 35 ? Blocks.MOSSY_STONE_BRICKS : Blocks.WATER);
                    if (d2 < 25) set(level, x + dx, y - 1, z + dz, Blocks.GLOWSTONE);
                }
            }
        }
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            int px = x + (int) Math.round(Math.cos(angle) * 9);
            int pz = z + (int) Math.round(Math.sin(angle) * 9);
            for (int dy = 1; dy <= 5; dy++) set(level, px, y + dy, pz, Blocks.MOSSY_STONE_BRICKS);
            set(level, px, y + 6, pz, Blocks.SOUL_LANTERN);
        }
    }

    private static void buildCanopyWalk(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            for (int width = -1; width <= 1; width++) {
                set(level, x + width, y, z, Blocks.DARK_OAK_PLANKS);
            }
        }
    }

    private static void buildElvenLodge(ServerLevel level, int x, int y, int z) {
        clearBox(level, x - 7, y, z - 7, x + 7, y + 11, z + 7);
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                if (dx * dx + dz * dz <= 36) {
                    set(level, x + dx, y, z + dz, Blocks.STRIPPED_BIRCH_WOOD);
                    if (dx * dx + dz * dz >= 25) {
                        for (int dy = 1; dy <= 5; dy++) set(level, x + dx, y + dy, z + dz, Blocks.BIRCH_PLANKS);
                    }
                }
            }
        }
        for (int dy = 6; dy <= 10; dy++) {
            int r = 11 - dy;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz <= r * r) set(level, x + dx, y + dy, z + dz, Blocks.AZALEA_LEAVES);
                }
            }
        }
        set(level, x, y + 1, z - 6, Blocks.AIR);
        set(level, x, y + 2, z - 6, Blocks.AIR);
        set(level, x, y + 3, z, Blocks.SOUL_LANTERN);
    }

    private static void buildStoneHall(ServerLevel level, int x, int y, int z) {
        clearBox(level, x - 1, y, z - 1, x + 13, y + 10, z + 11);
        fillFloor(level, x, y, z, 12, 10, Blocks.POLISHED_ANDESITE);
        for (int dx = 0; dx < 12; dx++) {
            for (int dz = 0; dz < 10; dz++) {
                if (dx == 0 || dz == 0 || dx == 11 || dz == 9) {
                    for (int dy = 1; dy <= 6; dy++) set(level, x + dx, y + dy, z + dz, Blocks.STONE_BRICKS);
                }
            }
        }
        for (int dx = -1; dx <= 12; dx++) {
            for (int dz = -1; dz <= 10; dz++) set(level, x + dx, y + 7, z + dz, Blocks.DEEPSLATE_TILES);
        }
        int doorX = x + 6;
        set(level, doorX, y + 1, z, Blocks.AIR);
        set(level, doorX, y + 2, z, Blocks.AIR);
        set(level, doorX, y + 3, z, Blocks.AIR);
        set(level, x + 6, y + 4, z + 5, Blocks.LANTERN);
    }

    private static void carveMountainGate(ServerLevel level, int x, int y, int z) {
        clearBox(level, x - 7, y, z - 2, x + 7, y + 12, z + 24);
        for (int dx = -9; dx <= 9; dx++) {
            for (int dy = 0; dy <= 14; dy++) {
                boolean frame = Math.abs(dx) >= 7 || dy >= 11;
                if (frame) set(level, x + dx, y + dy, z, Blocks.DEEPSLATE_BRICKS);
            }
        }
        for (int dz = 0; dz <= 28; dz++) {
            for (int dx = -6; dx <= 6; dx++) {
                set(level, x + dx, y, z + dz, Blocks.POLISHED_DEEPSLATE);
                if (Math.abs(dx) == 6) {
                    for (int dy = 1; dy <= 9; dy++) set(level, x + dx, y + dy, z + dz, Blocks.STONE_BRICKS);
                }
            }
            if (dz % 7 == 0) {
                set(level, x - 5, y + 4, z + dz, Blocks.LANTERN);
                set(level, x + 5, y + 4, z + dz, Blocks.LANTERN);
            }
        }
    }

    private static void buildForgeYard(ServerLevel level, int x, int y, int z) {
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -8; dz <= 8; dz++) set(level, x + dx, y, z + dz, Blocks.COBBLED_DEEPSLATE);
        }
        for (int dx : new int[]{-6, 0, 6}) {
            set(level, x + dx, y + 1, z, Blocks.BLAST_FURNACE);
            set(level, x + dx, y + 1, z + 3, Blocks.ANVIL);
            set(level, x + dx, y + 1, z - 3, Blocks.LAVA);
        }
    }

    private static void addErdenTrees(ServerLevel level, int cx, int cz) {
        for (int i = 0; i < 24; i++) {
            double angle = i * 2.399963229728653;
            int radius = 58 + (i % 5) * 15;
            simpleTree(level, cx + (int) Math.round(Math.cos(angle) * radius), 66, cz + (int) Math.round(Math.sin(angle) * radius), Blocks.OAK_LOG, Blocks.OAK_LEAVES);
        }
    }

    private static void addSilvanaTrees(ServerLevel level, int cx, int cz) {
        for (int i = 0; i < 36; i++) {
            double angle = i * 2.399963229728653;
            int radius = 42 + (i % 8) * 13;
            simpleTree(level, cx + (int) Math.round(Math.cos(angle) * radius), 68, cz + (int) Math.round(Math.sin(angle) * radius), Blocks.DARK_OAK_LOG, Blocks.FLOWERING_AZALEA_LEAVES);
        }
    }

    private static void simpleTree(ServerLevel level, int x, int y, int z, Block log, Block leaves) {
        for (int dy = 0; dy < 6; dy++) set(level, x, y + dy, z, log);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    if (dx * dx + dz * dz + dy * dy <= 11) set(level, x + dx, y + 6 + dy, z + dz, leaves);
                }
            }
        }
    }

    private static void prepareSpawn(ServerLevel level, PlayableOriginCatalog.ResidenceOption residence) {
        int x = residence.spawnX();
        int y = residence.spawnY();
        int z = residence.spawnZ();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                set(level, x + dx, y - 1, z + dz, Blocks.SMOOTH_STONE);
                for (int dy = 0; dy <= 3; dy++) set(level, x + dx, y + dy, z + dz, Blocks.AIR);
            }
        }
        set(level, x + 2, y, z + 2, Blocks.LANTERN);
    }

    private static void clearBox(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) set(level, x, y, z, Blocks.AIR);
            }
        }
    }

    private static void fillFloor(ServerLevel level, int x, int y, int z, int width, int depth, Block block) {
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) set(level, x + dx, y, z + dz, block);
        }
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 3);
    }

    private static void set(ServerLevel level, BlockPos.MutableBlockPos pos, int x, int y, int z, Block block) {
        pos.set(x, y, z);
        level.setBlock(pos, block.defaultBlockState(), 3);
    }
}
