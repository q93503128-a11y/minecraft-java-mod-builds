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
            LivingKingdoms.LOGGER.error("Missing playable residence {} for player {}",
                    profile.residenceId(), player.getGameProfile().name());
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
            LivingKingdoms.LOGGER.error("Failed to teleport player {} into Living Kingdoms realm",
                    player.getGameProfile().name());
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
        raiseRollingGround(level, 0, 0, 66, 68, 58, Blocks.GRASS_BLOCK, Blocks.DIRT);
        buildRoad(level, 0, -64, 0, 64, 3);
        buildRoad(level, -64, 0, 64, 0, 3);
        buildPlaza(level, 0, 65, 0, 12, Blocks.STONE_BRICKS);

        buildTimberHouse(level, 8, 65, 6, 10, 9);
        buildTimberHouse(level, -28, 65, -24, 9, 8);
        buildTimberHouse(level, 22, 65, -26, 10, 8);
        buildTimberHouse(level, -30, 65, 24, 9, 8);
        buildWatchTower(level, 0, 65, -48);

        buildFarmstead(level, 106, 65, 68);
        buildFishingHut(level, -110, 65, 87);
        carveRiver(level, -120, 48, 126, 6);
        buildCamp(level, 82, 65, -116);

        for (int i = 0; i < 18; i++) {
            double angle = i * 2.399963229728653;
            int radius = 38 + (i % 4) * 11;
            simpleTree(level,
                    (int) Math.round(Math.cos(angle) * radius),
                    66,
                    (int) Math.round(Math.sin(angle) * radius),
                    Blocks.OAK_LOG,
                    Blocks.OAK_LEAVES);
        }
    }

    private static void buildSilvana(ServerLevel level) {
        int cx = 1240;
        int cz = 35;
        raiseRollingGround(level, cx, cz, 67, 72, 62, Blocks.MOSS_BLOCK, Blocks.DIRT);
        buildGiantTree(level, 1210, 65, 8, 6, 22);
        buildGiantTree(level, 1164, 65, -34, 4, 17);
        buildGiantTree(level, 1280, 65, -40, 4, 18);
        buildMoonwell(level, 1290, 65, 84);
        buildElvenLodge(level, 1284, 66, 78);
        buildCanopyWalk(level, 1210, 82, 8, 1288, 74, 82);

        for (int i = 0; i < 24; i++) {
            double angle = i * 2.399963229728653;
            int radius = 32 + (i % 6) * 10;
            simpleTree(level,
                    cx + (int) Math.round(Math.cos(angle) * radius),
                    68,
                    cz + (int) Math.round(Math.sin(angle) * radius),
                    Blocks.DARK_OAK_LOG,
                    Blocks.FLOWERING_AZALEA_LEAVES);
        }
    }

    private static void buildKardum(ServerLevel level) {
        int cx = -1170;
        int cz = 38;
        buildMountainRing(level, cx, cz, 68, 65);
        carveMountainGate(level, -1200, 67, 0);
        buildStoneHall(level, -1216, 67, 3);
        buildStoneHall(level, -1134, 67, 86);
        buildForgeYard(level, -1164, 67, 44);
        buildRoad(level, -1250, 38, -1088, 38, 3);
        buildWatchTower(level, -1092, 67, 18);
    }

    private static void raiseRollingGround(
            ServerLevel level,
            int centerX,
            int centerZ,
            int minTopY,
            int maxTopY,
            int radius,
            Block surface,
            Block filler
    ) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                if (dx * dx + dz * dz > radius * radius) continue;
                double wave = (Math.sin(x * 0.085) + Math.cos(z * 0.073) + 2.0) / 4.0;
                int topY = minTopY + (int) Math.round(wave * (maxTopY - minTopY));
                for (int y = 65; y <= topY; y++) {
                    set(level, x, y, z, y == topY ? surface : filler);
                }
            }
        }
    }

    private static void buildMountainRing(ServerLevel level, int centerX, int centerZ, int radius, int innerRadius) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                double distance = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (distance > radius) continue;
                int topY;
                if (distance < innerRadius * 0.55) {
                    topY = 68;
                } else {
                    double ridge = Math.max(0.0, 1.0 - Math.abs(distance - innerRadius) / 25.0);
                    topY = 68 + (int) Math.round(ridge * 20.0);
                }
                for (int y = 65; y <= topY; y++) {
                    set(level, x, y, z, Blocks.STONE);
                }
                if (topY <= 72) set(level, x, topY, z, Blocks.GRASS_BLOCK);
            }
        }
    }

    private static void buildRoad(ServerLevel level, int x1, int z1, int x2, int z2, int width) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            for (int side = -width; side <= width; side++) {
                int px = Math.abs(x2 - x1) >= Math.abs(z2 - z1) ? x : x + side;
                int pz = Math.abs(x2 - x1) >= Math.abs(z2 - z1) ? z + side : z;
                set(level, px, 65, pz, Math.abs(side) == width ? Blocks.COBBLESTONE : Blocks.GRAVEL);
                clearColumn(level, px, 66, pz, 3);
            }
        }
    }

    private static void buildPlaza(ServerLevel level, int cx, int y, int cz, int radius, Block floor) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                set(level, cx + dx, y, cz + dz, floor);
                clearColumn(level, cx + dx, y + 1, cz + dz, 3);
            }
        }
        for (int dy = 1; dy <= 5; dy++) set(level, cx, y + dy, cz, Blocks.CHISELED_STONE_BRICKS);
        set(level, cx, y + 6, cz, Blocks.LANTERN);
    }

    private static void buildTimberHouse(ServerLevel level, int x, int y, int z, int width, int depth) {
        clearBox(level, x - 1, y + 1, z - 1, x + width, y + 8, z + depth);
        fillFloor(level, x, y, z, width, depth, Blocks.SPRUCE_PLANKS);
        for (int ix = 0; ix < width; ix++) {
            for (int iz = 0; iz < depth; iz++) {
                boolean wall = ix == 0 || iz == 0 || ix == width - 1 || iz == depth - 1;
                if (!wall) continue;
                for (int iy = 1; iy <= 4; iy++) {
                    boolean corner = (ix == 0 || ix == width - 1) && (iz == 0 || iz == depth - 1);
                    set(level, x + ix, y + iy, z + iz,
                            corner ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.SPRUCE_PLANKS);
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

    private static void buildFarmstead(ServerLevel level, int x, int y, int z) {
        buildTimberHouse(level, x, y, z, 11, 9);
        for (int dx = -14; dx <= -3; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                set(level, x + dx, y, z + dz, Blocks.FARMLAND);
                if ((dx + dz) % 5 == 0) set(level, x + dx, y + 1, z + dz, Blocks.WHEAT);
            }
        }
        set(level, x - 9, y, z, Blocks.WATER);
    }

    private static void carveRiver(ServerLevel level, int centerX, int z1, int z2, int halfWidth) {
        for (int z = z1; z <= z2; z++) {
            int bendX = centerX + (int) Math.round(Math.sin(z * 0.07) * 7.0);
            for (int dx = -halfWidth; dx <= halfWidth; dx++) {
                int depth = 2 + Math.max(0, halfWidth - Math.abs(dx)) / 2;
                for (int y = 65; y >= 65 - depth; y--) {
                    set(level, bendX + dx, y, z, y <= 64 ? Blocks.WATER : Blocks.AIR);
                }
                set(level, bendX + dx, 61, z, Blocks.GRAVEL);
            }
        }
    }

    private static void buildFishingHut(ServerLevel level, int x, int y, int z) {
        buildTimberHouse(level, x, y, z, 8, 7);
        for (int dx = -2; dx <= 10; dx++) {
            set(level, x + dx, y, z + 10, Blocks.SPRUCE_PLANKS);
            if (dx % 4 == 0) set(level, x + dx, y - 1, z + 10, Blocks.SPRUCE_LOG);
        }
        set(level, x + 4, y + 1, z + 5, Blocks.BARREL);
    }

    private static void buildCamp(ServerLevel level, int x, int y, int z) {
        clearBox(level, x - 7, y + 1, z - 7, x + 7, y + 7, z + 7);
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) set(level, x + dx, y, z + dz, Blocks.COARSE_DIRT);
        }
        set(level, x, y + 1, z, Blocks.CAMPFIRE);
        for (int side : new int[]{-5, 5}) {
            for (int height = 1; height <= 4; height++) {
                set(level, x + side, y + height, z - 4, Blocks.SPRUCE_LOG);
                set(level, x + side, y + height, z + 4, Blocks.SPRUCE_LOG);
            }
        }
        for (int dx = -5; dx <= 5; dx++) {
            set(level, x + dx, y + 5, z - 4, Blocks.MOSS_BLOCK);
            set(level, x + dx, y + 5, z + 4, Blocks.MOSS_BLOCK);
        }
    }

    private static void buildWatchTower(ServerLevel level, int x, int y, int z) {
        for (int dy = 0; dy <= 10; dy++) {
            for (int ox : new int[]{-3, 3}) {
                for (int oz : new int[]{-3, 3}) set(level, x + ox, y + dy, z + oz, Blocks.STRIPPED_DARK_OAK_LOG);
            }
        }
        for (int ox = -4; ox <= 4; ox++) {
            for (int oz = -4; oz <= 4; oz++) set(level, x + ox, y + 9, z + oz, Blocks.DARK_OAK_PLANKS);
        }
        set(level, x, y + 10, z, Blocks.LANTERN);
    }

    private static void buildGiantTree(ServerLevel level, int x, int y, int z, int radius, int height) {
        for (int dy = 0; dy <= height; dy++) {
            int currentRadius = Math.max(2, radius - dy / 7);
            for (int dx = -currentRadius; dx <= currentRadius; dx++) {
                for (int dz = -currentRadius; dz <= currentRadius; dz++) {
                    if (dx * dx + dz * dz <= currentRadius * currentRadius) {
                        set(level, x + dx, y + dy, z + dz, Blocks.DARK_OAK_LOG);
                    }
                }
            }
        }
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                for (int dy = -3; dy <= 4; dy++) {
                    if (dx * dx + dz * dz + dy * dy * 2 <= 110) {
                        set(level, x + dx, y + height + dy, z + dz, Blocks.AZALEA_LEAVES);
                    }
                }
            }
        }
        clearBox(level, x - 2, y + 2, z - 2, x + 2, y + 8, z + 2);
        fillFloor(level, x - 2, y + 1, z - 2, 5, 5, Blocks.MOSS_BLOCK);
        set(level, x, y + 3, z, Blocks.SOUL_LANTERN);
    }

    private static void buildMoonwell(ServerLevel level, int x, int y, int z) {
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > 36) continue;
                set(level, x + dx, y, z + dz, d2 >= 25 ? Blocks.MOSSY_STONE_BRICKS : Blocks.WATER);
                if (d2 < 20) set(level, x + dx, y - 1, z + dz, Blocks.GLOWSTONE);
            }
        }
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3.0;
            int px = x + (int) Math.round(Math.cos(angle) * 8.0);
            int pz = z + (int) Math.round(Math.sin(angle) * 8.0);
            for (int dy = 1; dy <= 4; dy++) set(level, px, y + dy, pz, Blocks.MOSSY_STONE_BRICKS);
            set(level, px, y + 5, pz, Blocks.SOUL_LANTERN);
        }
    }

    private static void buildElvenLodge(ServerLevel level, int x, int y, int z) {
        clearBox(level, x - 7, y + 1, z - 7, x + 7, y + 10, z + 7);
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 <= 36) {
                    set(level, x + dx, y, z + dz, Blocks.STRIPPED_BIRCH_WOOD);
                    if (d2 >= 25) {
                        for (int dy = 1; dy <= 5; dy++) set(level, x + dx, y + dy, z + dz, Blocks.BIRCH_PLANKS);
                    }
                }
            }
        }
        for (int dy = 6; dy <= 9; dy++) {
            int radius = 10 - dy;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz <= radius * radius) set(level, x + dx, y + dy, z + dz, Blocks.AZALEA_LEAVES);
                }
            }
        }
        set(level, x, y + 1, z - 6, Blocks.AIR);
        set(level, x, y + 2, z - 6, Blocks.AIR);
        set(level, x, y + 3, z, Blocks.SOUL_LANTERN);
    }

    private static void buildCanopyWalk(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            for (int width = -1; width <= 1; width++) set(level, x + width, y, z, Blocks.DARK_OAK_PLANKS);
        }
    }

    private static void carveMountainGate(ServerLevel level, int x, int y, int z) {
        clearBox(level, x - 6, y + 1, z - 2, x + 6, y + 10, z + 22);
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = 0; dy <= 12; dy++) {
                if (Math.abs(dx) >= 6 || dy >= 10) set(level, x + dx, y + dy, z, Blocks.DEEPSLATE_BRICKS);
            }
        }
        for (int dz = 0; dz <= 24; dz++) {
            for (int dx = -5; dx <= 5; dx++) {
                set(level, x + dx, y, z + dz, Blocks.POLISHED_DEEPSLATE);
                if (Math.abs(dx) == 5) {
                    for (int dy = 1; dy <= 8; dy++) set(level, x + dx, y + dy, z + dz, Blocks.STONE_BRICKS);
                }
            }
            if (dz % 6 == 0) {
                set(level, x - 4, y + 4, z + dz, Blocks.LANTERN);
                set(level, x + 4, y + 4, z + dz, Blocks.LANTERN);
            }
        }
    }

    private static void buildStoneHall(ServerLevel level, int x, int y, int z) {
        clearBox(level, x - 1, y + 1, z - 1, x + 13, y + 9, z + 11);
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

    private static void buildForgeYard(ServerLevel level, int x, int y, int z) {
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -7; dz <= 7; dz++) set(level, x + dx, y, z + dz, Blocks.COBBLED_DEEPSLATE);
        }
        for (int dx : new int[]{-5, 0, 5}) {
            set(level, x + dx, y + 1, z, Blocks.BLAST_FURNACE);
            set(level, x + dx, y + 1, z + 3, Blocks.ANVIL);
            set(level, x + dx, y + 1, z - 3, Blocks.LAVA);
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
                clearColumn(level, x + dx, y, z + dz, 4);
            }
        }
        set(level, x + 2, y, z + 2, Blocks.LANTERN);
    }

    private static void clearColumn(ServerLevel level, int x, int y, int z, int height) {
        for (int dy = 0; dy < height; dy++) set(level, x, y + dy, z, Blocks.AIR);
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
}
