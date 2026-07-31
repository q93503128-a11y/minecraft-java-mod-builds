package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VillageWorldSystem {
    private static final int FORTRESS_RADIUS = 58;
    private static final int TERRAFORM_RADIUS = 66;
    private static final int CLEAR_HEIGHT = 32;
    private static final Set<UUID> ALLOWED_GAME_MOBS = ConcurrentHashMap.newKeySet();

    private static boolean generationInProgress;

    private VillageWorldSystem() {
    }

    public static synchronized void resetTransientState() {
        generationInProgress = false;
        ALLOWED_GAME_MOBS.clear();
    }

    public static synchronized void ensureFortifiedVillage(ServerPlayer player) {
        if (generationInProgress || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        var server = level.getServer();
        if (server == null || level != server.overworld()) {
            return;
        }

        if (VillageCouncilState.villageCenter().isEmpty()) {
            VillageCouncilState.setVillageCenter(player);
        }
        BlockPos center = VillageCouncilState.villageCenter().orElse(player.blockPosition()).immutable();

        if (isCurrentFortressBuilt(level, center)) {
            removeUnauthorizedMobs(level, center);
            return;
        }

        generationInProgress = true;
        try {
            player.sendSystemMessage(Component.literal(
                    "§6[마을 재건] §f기존 임시 건축을 철거하고 전투용 성채 마을을 건설합니다."));
            buildFortress(level, center);
            applyAllUpgradeVisuals(level);
            removeUnauthorizedMobs(level, center);
            player.sendSystemMessage(Component.literal(
                    "§a[마을 준비 완료] §f회관·병영·무기고·창고·의무소·성벽 관리소를 직접 사용하세요."));
        } finally {
            generationInProgress = false;
        }
    }

    public static boolean isAllowedGameMob(Mob mob) {
        return ALLOWED_GAME_MOBS.contains(mob.getUUID());
    }

    public static void markAllowedGameMob(Mob mob) {
        ALLOWED_GAME_MOBS.add(mob.getUUID());
    }

    public static void applyUpgradeVisual(
            ServerLevel level,
            VillageProgressionSystem.Building building,
            int upgradeLevel) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null || upgradeLevel <= 0) {
            return;
        }

        int y = center.getY();
        switch (building) {
            case WALLS -> {
                for (int side = -1; side <= 1; side += 2) {
                    for (int i = -40; i <= 40; i += 8) {
                        setAbsolute(level, center.getX() + i, y + upgradeLevel, center.getZ() + side * 56, Blocks.IRON_BLOCK);
                        setAbsolute(level, center.getX() + side * 56, y + upgradeLevel, center.getZ() + i, Blocks.IRON_BLOCK);
                    }
                }
            }
            case ARMORY -> decorateUpgradePedestal(level, center.offset(38, 0, -17), upgradeLevel, Blocks.IRON_BLOCK);
            case INFIRMARY -> decorateUpgradePedestal(level, center.offset(38, 0, 27), upgradeLevel, Blocks.QUARTZ_BLOCK);
            case STOREHOUSE -> decorateUpgradePedestal(level, center.offset(-38, 0, 27), upgradeLevel, Blocks.COPPER_BLOCK);
            case BARRACKS -> decorateUpgradePedestal(level, center.offset(-38, 0, -17), upgradeLevel, Blocks.RED_WOOL);
            case TOWN_HALL -> {
            }
        }
    }

    private static boolean isCurrentFortressBuilt(ServerLevel level, BlockPos center) {
        return level.getBlockState(center.below(2)).is(Blocks.EMERALD_BLOCK);
    }

    private static void buildFortress(ServerLevel level, BlockPos center) {
        int groundY = center.getY() - 1;
        terraform(level, center, groundY);
        buildRoadNetwork(level, center, groundY);
        buildCurtainWall(level, center, groundY);

        buildTower(level, center.offset(-FORTRESS_RADIUS, 0, -FORTRESS_RADIUS), groundY);
        buildTower(level, center.offset(FORTRESS_RADIUS, 0, -FORTRESS_RADIUS), groundY);
        buildTower(level, center.offset(-FORTRESS_RADIUS, 0, FORTRESS_RADIUS), groundY);
        buildTower(level, center.offset(FORTRESS_RADIUS, 0, FORTRESS_RADIUS), groundY);

        buildTownHall(level, center.offset(-13, 0, -48), groundY);
        buildBarracks(level, center.offset(-52, 0, -22), groundY);
        buildArmory(level, center.offset(32, 0, -22), groundY);
        buildStorehouse(level, center.offset(-52, 0, 18), groundY);
        buildInfirmary(level, center.offset(32, 0, 18), groundY);
        buildWallOffice(level, center.offset(-7, 0, 42), groundY);

        buildResidence(level, center.offset(-27, 0, -14), groundY, 15, 13, Blocks.OAK_PLANKS, Blocks.BRICKS);
        buildResidence(level, center.offset(13, 0, -14), groundY, 15, 13, Blocks.SPRUCE_PLANKS, Blocks.DEEPSLATE_TILES);
        buildResidence(level, center.offset(-27, 0, 20), groundY, 15, 13, Blocks.SPRUCE_PLANKS, Blocks.BRICKS);
        buildResidence(level, center.offset(13, 0, 20), groundY, 15, 13, Blocks.OAK_PLANKS, Blocks.DARK_OAK_PLANKS);

        buildMarket(level, center, groundY);
        buildLamps(level, center, groundY);

        set(level, center.offset(0, -2, 0), Blocks.EMERALD_BLOCK);
        set(level, center.offset(0, -1, 0), Blocks.CHISELED_STONE_BRICKS);
    }

    private static void terraform(ServerLevel level, BlockPos center, int groundY) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -TERRAFORM_RADIUS; dx <= TERRAFORM_RADIUS; dx++) {
            for (int dz = -TERRAFORM_RADIUS; dz <= TERRAFORM_RADIUS; dz++) {
                setAbsolute(level, center.getX() + dx, groundY - 2, center.getZ() + dz, Blocks.DIRT);
                setAbsolute(level, center.getX() + dx, groundY - 1, center.getZ() + dz, Blocks.DIRT);
                setAbsolute(level, center.getX() + dx, groundY, center.getZ() + dz, Blocks.GRASS_BLOCK);
                for (int y = 1; y <= CLEAR_HEIGHT; y++) {
                    level.setBlockAndUpdate(
                            new BlockPos(center.getX() + dx, groundY + y, center.getZ() + dz),
                            air);
                }
            }
        }
    }

    private static void buildRoadNetwork(ServerLevel level, BlockPos center, int groundY) {
        for (int i = -FORTRESS_RADIUS + 3; i <= FORTRESS_RADIUS - 3; i++) {
            for (int width = -3; width <= 3; width++) {
                setAbsolute(level, center.getX() + width, groundY, center.getZ() + i, Blocks.PACKED_MUD);
                setAbsolute(level, center.getX() + i, groundY, center.getZ() + width, Blocks.PACKED_MUD);
            }
        }

        for (int dx = -15; dx <= 15; dx++) {
            for (int dz = -15; dz <= 15; dz++) {
                if (dx * dx + dz * dz <= 225) {
                    setAbsolute(level, center.getX() + dx, groundY, center.getZ() + dz,
                            ((dx + dz) & 3) == 0 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS);
                }
            }
        }
    }

    private static void buildCurtainWall(ServerLevel level, BlockPos center, int groundY) {
        for (int dx = -FORTRESS_RADIUS; dx <= FORTRESS_RADIUS; dx++) {
            for (int dz = -FORTRESS_RADIUS; dz <= FORTRESS_RADIUS; dz++) {
                boolean perimeter = Math.abs(dx) >= FORTRESS_RADIUS - 2 || Math.abs(dz) >= FORTRESS_RADIUS - 2;
                if (!perimeter || isGateOpening(dx, dz)) {
                    continue;
                }
                for (int y = 1; y <= 8; y++) {
                    Block material = y <= 2 ? Blocks.STONE_BRICKS
                            : (y == 8 ? Blocks.POLISHED_ANDESITE : Blocks.COBBLESTONE);
                    setAbsolute(level, center.getX() + dx, groundY + y, center.getZ() + dz, material);
                }
                if (((dx + dz) & 1) == 0) {
                    setAbsolute(level, center.getX() + dx, groundY + 9, center.getZ() + dz, Blocks.STONE_BRICKS);
                }
            }
        }

        buildGatehouse(level, center.offset(0, 0, -FORTRESS_RADIUS), groundY, true);
        buildGatehouse(level, center.offset(0, 0, FORTRESS_RADIUS), groundY, true);
        buildGatehouse(level, center.offset(-FORTRESS_RADIUS, 0, 0), groundY, false);
        buildGatehouse(level, center.offset(FORTRESS_RADIUS, 0, 0), groundY, false);
    }

    private static boolean isGateOpening(int dx, int dz) {
        return Math.abs(dx) <= 4 && Math.abs(dz) >= FORTRESS_RADIUS - 2
                || Math.abs(dz) <= 4 && Math.abs(dx) >= FORTRESS_RADIUS - 2;
    }

    private static void buildGatehouse(ServerLevel level, BlockPos gate, int groundY, boolean northSouth) {
        for (int side : new int[]{-8, 8}) {
            for (int a = -3; a <= 3; a++) {
                for (int y = 1; y <= 12; y++) {
                    int x = gate.getX() + (northSouth ? side : a);
                    int z = gate.getZ() + (northSouth ? a : side);
                    setAbsolute(level, x, groundY + y, z,
                            y >= 9 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE);
                }
            }
        }
        for (int across = -8; across <= 8; across++) {
            for (int y = 8; y <= 10; y++) {
                int x = gate.getX() + (northSouth ? across : 0);
                int z = gate.getZ() + (northSouth ? 0 : across);
                setAbsolute(level, x, groundY + y, z, Blocks.STONE_BRICKS);
            }
        }
        for (int across = -4; across <= 4; across++) {
            int x = gate.getX() + (northSouth ? across : 0);
            int z = gate.getZ() + (northSouth ? 0 : across);
            setAbsolute(level, x, groundY, z, Blocks.STONE_BRICKS);
        }
    }

    private static void buildTower(ServerLevel level, BlockPos corner, int groundY) {
        int radius = 6;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                boolean outer = Math.abs(dx) >= radius - 1 || Math.abs(dz) >= radius - 1;
                setAbsolute(level, corner.getX() + dx, groundY, corner.getZ() + dz, Blocks.STONE_BRICKS);
                if (!outer) {
                    setAbsolute(level, corner.getX() + dx, groundY + 1, corner.getZ() + dz, Blocks.SPRUCE_PLANKS);
                    continue;
                }
                for (int y = 1; y <= 14; y++) {
                    setAbsolute(level, corner.getX() + dx, groundY + y, corner.getZ() + dz,
                            y <= 3 || y >= 12 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE);
                }
                if (((dx + dz) & 1) == 0) {
                    setAbsolute(level, corner.getX() + dx, groundY + 15, corner.getZ() + dz, Blocks.STONE_BRICKS);
                }
            }
        }
    }

    private static void buildTownHall(ServerLevel level, BlockPos origin, int groundY) {
        buildTimberBuilding(level, origin, groundY, 27, 21, 9, Blocks.OAK_PLANKS, Blocks.DEEPSLATE_TILES);
        int centerX = origin.getX() + 13;
        int centerZ = origin.getZ() + 10;
        setAbsolute(level, centerX, groundY + 2, centerZ, Blocks.BELL);
        setAbsolute(level, centerX - 5, groundY + 2, centerZ, Blocks.BOOKSHELF);
        setAbsolute(level, centerX + 5, groundY + 2, centerZ, Blocks.BOOKSHELF);
        setAbsolute(level, centerX, groundY + 2, centerZ + 5, Blocks.CRAFTING_TABLE);
    }

    private static void buildBarracks(ServerLevel level, BlockPos origin, int groundY) {
        buildTimberBuilding(level, origin, groundY, 21, 17, 7, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS);
        int terminalX = origin.getX() + 10;
        int terminalZ = origin.getZ() + 9;
        setAbsolute(level, terminalX, groundY + 2, terminalZ, Blocks.TARGET);
        for (int z = 3; z <= 13; z += 5) {
            setAbsolute(level, origin.getX() + 3, groundY + 2, origin.getZ() + z, Blocks.SPRUCE_PLANKS);
            setAbsolute(level, origin.getX() + 17, groundY + 2, origin.getZ() + z, Blocks.SPRUCE_PLANKS);
        }
    }

    private static void buildArmory(ServerLevel level, BlockPos origin, int groundY) {
        buildTimberBuilding(level, origin, groundY, 21, 17, 7, Blocks.BRICKS, Blocks.DEEPSLATE_TILES);
        setAbsolute(level, origin.getX() + 10, groundY + 2, origin.getZ() + 9, Blocks.SMITHING_TABLE);
        setAbsolute(level, origin.getX() + 5, groundY + 2, origin.getZ() + 5, Blocks.BLAST_FURNACE);
        setAbsolute(level, origin.getX() + 15, groundY + 2, origin.getZ() + 5, Blocks.ANVIL);
        setAbsolute(level, origin.getX() + 5, groundY + 2, origin.getZ() + 12, Blocks.FLETCHING_TABLE);
    }

    private static void buildStorehouse(ServerLevel level, BlockPos origin, int groundY) {
        buildTimberBuilding(level, origin, groundY, 21, 17, 7, Blocks.OAK_PLANKS, Blocks.BRICKS);
        setAbsolute(level, origin.getX() + 10, groundY + 2, origin.getZ() + 9, Blocks.BARREL);
        for (int x = 3; x <= 17; x += 4) {
            setAbsolute(level, origin.getX() + x, groundY + 2, origin.getZ() + 5, Blocks.BARREL);
            setAbsolute(level, origin.getX() + x, groundY + 2, origin.getZ() + 13, Blocks.CHEST);
        }
    }

    private static void buildInfirmary(ServerLevel level, BlockPos origin, int groundY) {
        buildTimberBuilding(level, origin, groundY, 21, 17, 7, Blocks.QUARTZ_BLOCK, Blocks.STONE_BRICKS);
        setAbsolute(level, origin.getX() + 10, groundY + 2, origin.getZ() + 9, Blocks.BREWING_STAND);
        setAbsolute(level, origin.getX() + 5, groundY + 2, origin.getZ() + 5, Blocks.CAULDRON);
        setAbsolute(level, origin.getX() + 15, groundY + 2, origin.getZ() + 5, Blocks.BOOKSHELF);
        for (int z = 4; z <= 12; z += 4) {
            setAbsolute(level, origin.getX() + 4, groundY + 2, origin.getZ() + z, Blocks.WHITE_WOOL);
            setAbsolute(level, origin.getX() + 16, groundY + 2, origin.getZ() + z, Blocks.WHITE_WOOL);
        }
    }

    private static void buildWallOffice(ServerLevel level, BlockPos origin, int groundY) {
        buildTimberBuilding(level, origin, groundY, 15, 11, 6, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_TILES);
        setAbsolute(level, origin.getX() + 7, groundY + 2, origin.getZ() + 6, Blocks.STONECUTTER);
        setAbsolute(level, origin.getX() + 3, groundY + 2, origin.getZ() + 5, Blocks.COBBLESTONE);
        setAbsolute(level, origin.getX() + 11, groundY + 2, origin.getZ() + 5, Blocks.IRON_BLOCK);
    }

    private static void buildResidence(
            ServerLevel level,
            BlockPos origin,
            int groundY,
            int width,
            int depth,
            Block wall,
            Block roof) {
        buildTimberBuilding(level, origin, groundY, width, depth, 6, wall, roof);
        setAbsolute(level, origin.getX() + 3, groundY + 2, origin.getZ() + depth - 4, Blocks.CRAFTING_TABLE);
        setAbsolute(level, origin.getX() + width - 4, groundY + 2, origin.getZ() + depth - 4, Blocks.BARREL);
    }

    private static void buildTimberBuilding(
            ServerLevel level,
            BlockPos origin,
            int groundY,
            int width,
            int depth,
            int wallHeight,
            Block panel,
            Block roof) {
        int minX = origin.getX();
        int minZ = origin.getZ();
        int maxX = minX + width - 1;
        int maxZ = minZ + depth - 1;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                setAbsolute(level, x, groundY, z, Blocks.STONE_BRICKS);
                setAbsolute(level, x, groundY + 1, z, Blocks.SPRUCE_PLANKS);
            }
        }

        for (int y = 2; y <= wallHeight; y++) {
            for (int x = minX; x <= maxX; x++) {
                Block material = isBeam(x - minX, width, y) ? Blocks.STRIPPED_SPRUCE_WOOD : panel;
                setAbsolute(level, x, groundY + y, minZ, material);
                setAbsolute(level, x, groundY + y, maxZ, material);
            }
            for (int z = minZ; z <= maxZ; z++) {
                Block material = isBeam(z - minZ, depth, y) ? Blocks.STRIPPED_SPRUCE_WOOD : panel;
                setAbsolute(level, minX, groundY + y, z, material);
                setAbsolute(level, maxX, groundY + y, z, material);
            }
        }

        for (int y = 2; y <= 4; y++) {
            setAbsolute(level, minX + width / 2, groundY + y, minZ, Blocks.AIR);
            setAbsolute(level, minX + width / 2 + 1, groundY + y, minZ, Blocks.AIR);
        }

        for (int x = minX + 3; x <= maxX - 3; x += 4) {
            setAbsolute(level, x, groundY + 4, minZ, Blocks.GLASS_PANE);
            setAbsolute(level, x, groundY + 4, maxZ, Blocks.GLASS_PANE);
        }
        for (int z = minZ + 3; z <= maxZ - 3; z += 4) {
            setAbsolute(level, minX, groundY + 4, z, Blocks.GLASS_PANE);
            setAbsolute(level, maxX, groundY + 4, z, Blocks.GLASS_PANE);
        }

        buildSolidGabledRoof(level, minX, maxX, minZ, maxZ, groundY + wallHeight + 1, roof);
    }

    private static boolean isBeam(int offset, int size, int y) {
        return offset == 0 || offset == size - 1 || offset % 4 == 0 || y == 2 || y == 5;
    }

    private static void buildSolidGabledRoof(
            ServerLevel level,
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            int roofBaseY,
            Block roof) {
        int half = (maxX - minX + 2) / 2;
        for (int layer = 0; layer <= half; layer++) {
            int left = minX - 1 + layer;
            int right = maxX + 1 - layer;
            int y = roofBaseY + layer;
            for (int z = minZ - 1; z <= maxZ + 1; z++) {
                setAbsolute(level, left, y, z, roof);
                setAbsolute(level, left + 1, y, z, roof);
                setAbsolute(level, right, y, z, roof);
                setAbsolute(level, right - 1, y, z, roof);
            }
            for (int fillY = roofBaseY; fillY < y; fillY++) {
                setAbsolute(level, left, fillY, minZ, Blocks.STRIPPED_SPRUCE_WOOD);
                setAbsolute(level, left, fillY, maxZ, Blocks.STRIPPED_SPRUCE_WOOD);
                setAbsolute(level, right, fillY, minZ, Blocks.STRIPPED_SPRUCE_WOOD);
                setAbsolute(level, right, fillY, maxZ, Blocks.STRIPPED_SPRUCE_WOOD);
            }
            if (left >= right - 1) {
                break;
            }
        }
    }

    private static void buildMarket(ServerLevel level, BlockPos center, int groundY) {
        for (int side : new int[]{-1, 1}) {
            for (int z : new int[]{-9, 9}) {
                int x = center.getX() + side * 9;
                int absoluteZ = center.getZ() + z;
                for (int dx = -3; dx <= 3; dx++) {
                    setAbsolute(level, x + dx, groundY + 4, absoluteZ, side > 0 ? Blocks.RED_WOOL : Blocks.WHITE_WOOL);
                }
                setAbsolute(level, x - 3, groundY + 1, absoluteZ, Blocks.SPRUCE_FENCE);
                setAbsolute(level, x + 3, groundY + 1, absoluteZ, Blocks.SPRUCE_FENCE);
                setAbsolute(level, x, groundY + 1, absoluteZ, Blocks.BARREL);
            }
        }
    }

    private static void buildLamps(ServerLevel level, BlockPos center, int groundY) {
        int[][] positions = {
                {-18, -18}, {18, -18}, {-18, 18}, {18, 18},
                {0, -28}, {0, 28}, {-28, 0}, {28, 0},
                {-45, -35}, {45, -35}, {-45, 35}, {45, 35}
        };
        for (int[] position : positions) {
            int x = center.getX() + position[0];
            int z = center.getZ() + position[1];
            for (int y = 1; y <= 4; y++) {
                setAbsolute(level, x, groundY + y, z, Blocks.SPRUCE_FENCE);
            }
            setAbsolute(level, x, groundY + 5, z, Blocks.LANTERN);
        }
    }

    private static void decorateUpgradePedestal(ServerLevel level, BlockPos center, int levelValue, Block material) {
        for (int step = 0; step < levelValue; step++) {
            int offset = 2 + step;
            set(level, center.offset(offset, 1, 0), material);
            set(level, center.offset(-offset, 1, 0), material);
            set(level, center.offset(0, 1, offset), material);
            set(level, center.offset(0, 1, -offset), material);
        }
    }

    private static void applyAllUpgradeVisuals(ServerLevel level) {
        applyUpgradeVisual(level, VillageProgressionSystem.Building.WALLS, VillageProgressionSystem.wallLevel());
        applyUpgradeVisual(level, VillageProgressionSystem.Building.ARMORY, VillageProgressionSystem.armoryLevel());
        applyUpgradeVisual(level, VillageProgressionSystem.Building.INFIRMARY, VillageProgressionSystem.infirmaryLevel());
        applyUpgradeVisual(level, VillageProgressionSystem.Building.STOREHOUSE, VillageProgressionSystem.storehouseLevel());
        applyUpgradeVisual(level, VillageProgressionSystem.Building.BARRACKS, VillageProgressionSystem.barracksLevel());
    }

    private static void removeUnauthorizedMobs(ServerLevel level, BlockPos center) {
        AABB area = new AABB(center).inflate(TERRAFORM_RADIUS + 16, 40, TERRAFORM_RADIUS + 16);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
            if (!isAllowedGameMob(mob)) {
                mob.discard();
            }
        }
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlockAndUpdate(pos, block.defaultBlockState());
    }

    private static void setAbsolute(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlockAndUpdate(new BlockPos(x, y, z), block.defaultBlockState());
    }
}
