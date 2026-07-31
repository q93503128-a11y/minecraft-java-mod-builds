package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VillageWorldSystem {
    private static final int FORTRESS_RADIUS = 58;
    private static final int TERRAFORM_RADIUS = 66;
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
        if (level.getBlockState(center.below(2)).is(Blocks.EMERALD_BLOCK)) {
            removeUnauthorizedMobs(level, center);
            return;
        }

        generationInProgress = true;
        try {
            player.sendSystemMessage(Component.literal("§6[마을 재건] §f0.3 임시 건축을 철거하고 전투 성채를 건설합니다."));
            buildFortress(level, center);
            applyAllUpgradeVisuals(level);
            removeUnauthorizedMobs(level, center);
            player.sendSystemMessage(Component.literal("§a[마을 준비 완료] §f회관·병영·무기고·창고·의무소·성벽 관리소를 사용하세요."));
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

    public static void applyUpgradeVisual(ServerLevel level, VillageProgressionSystem.Building building, int upgradeLevel) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null || upgradeLevel <= 0) {
            return;
        }
        int y = center.getY();
        switch (building) {
            case WALLS -> {
                for (int side = -1; side <= 1; side += 2) {
                    for (int i = -40; i <= 40; i += 8) {
                        set(level, new BlockPos(center.getX() + i, y + upgradeLevel, center.getZ() + side * 56), Blocks.IRON_BLOCK);
                        set(level, new BlockPos(center.getX() + side * 56, y + upgradeLevel, center.getZ() + i), Blocks.IRON_BLOCK);
                    }
                }
            }
            case ARMORY -> decoratePedestal(level, center.offset(42, 0, -14), upgradeLevel, Blocks.IRON_BLOCK);
            case INFIRMARY -> decoratePedestal(level, center.offset(42, 0, 26), upgradeLevel, Blocks.QUARTZ_BLOCK);
            case STOREHOUSE -> decoratePedestal(level, center.offset(-42, 0, 26), upgradeLevel, Blocks.GOLD_BLOCK);
            case BARRACKS -> decoratePedestal(level, center.offset(-42, 0, -14), upgradeLevel, Blocks.BRICKS);
            case TOWN_HALL -> {
            }
        }
    }

    private static void buildFortress(ServerLevel level, BlockPos center) {
        int groundY = center.getY() - 1;
        terraform(level, center, groundY);
        buildRoads(level, center, groundY);
        buildWalls(level, center, groundY);

        buildTower(level, center.offset(-58, 0, -58), groundY);
        buildTower(level, center.offset(58, 0, -58), groundY);
        buildTower(level, center.offset(-58, 0, 58), groundY);
        buildTower(level, center.offset(58, 0, 58), groundY);

        buildBuilding(level, center.offset(-13, 0, -50), groundY, 27, 21, 9, Blocks.OAK_PLANKS, Blocks.DEEPSLATE_TILES);
        set(level, center.offset(0, 2, -40), Blocks.BELL);
        set(level, center.offset(-5, 2, -40), Blocks.BOOKSHELF);
        set(level, center.offset(5, 2, -40), Blocks.BOOKSHELF);

        buildBuilding(level, center.offset(-54, 0, -22), groundY, 23, 18, 7, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS);
        set(level, center.offset(-42, 2, -14), Blocks.TARGET);
        lineFurniture(level, center.offset(-51, 2, -18), 5, 4, Blocks.SPRUCE_PLANKS, true);

        buildBuilding(level, center.offset(31, 0, -22), groundY, 23, 18, 7, Blocks.BRICKS, Blocks.DEEPSLATE_TILES);
        set(level, center.offset(42, 2, -14), Blocks.SMITHING_TABLE);
        set(level, center.offset(36, 2, -17), Blocks.BLAST_FURNACE);
        set(level, center.offset(48, 2, -17), Blocks.ANVIL);
        set(level, center.offset(36, 2, -9), Blocks.FLETCHING_TABLE);

        buildBuilding(level, center.offset(-54, 0, 18), groundY, 23, 18, 7, Blocks.OAK_PLANKS, Blocks.BRICKS);
        set(level, center.offset(-42, 2, 26), Blocks.BARREL);
        lineFurniture(level, center.offset(-51, 2, 22), 5, 4, Blocks.BARREL, true);
        lineFurniture(level, center.offset(-51, 2, 31), 5, 4, Blocks.CHEST, true);

        buildBuilding(level, center.offset(31, 0, 18), groundY, 23, 18, 7, Blocks.QUARTZ_BLOCK, Blocks.STONE_BRICKS);
        set(level, center.offset(42, 2, 26), Blocks.BREWING_STAND);
        set(level, center.offset(36, 2, 23), Blocks.CAULDRON);
        set(level, center.offset(48, 2, 23), Blocks.BOOKSHELF);
        lineFurniture(level, center.offset(35, 2, 30), 4, 4, Blocks.QUARTZ_BLOCK, true);

        buildBuilding(level, center.offset(-8, 0, 42), groundY, 17, 12, 6, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_TILES);
        set(level, center.offset(0, 2, 48), Blocks.STONECUTTER);
        set(level, center.offset(-4, 2, 48), Blocks.COBBLESTONE);
        set(level, center.offset(4, 2, 48), Blocks.IRON_BLOCK);

        buildResidence(level, center.offset(-29, 0, -13), groundY, Blocks.OAK_PLANKS, Blocks.BRICKS);
        buildResidence(level, center.offset(14, 0, -13), groundY, Blocks.SPRUCE_PLANKS, Blocks.DEEPSLATE_TILES);
        buildResidence(level, center.offset(-29, 0, 18), groundY, Blocks.SPRUCE_PLANKS, Blocks.BRICKS);
        buildResidence(level, center.offset(14, 0, 18), groundY, Blocks.OAK_PLANKS, Blocks.DARK_OAK_PLANKS);

        buildMarket(level, center, groundY);
        buildLamps(level, center, groundY);
        set(level, center.below(2), Blocks.EMERALD_BLOCK);
        set(level, center.below(), Blocks.CHISELED_STONE_BRICKS);
    }

    private static void terraform(ServerLevel level, BlockPos center, int groundY) {
        for (int dx = -TERRAFORM_RADIUS; dx <= TERRAFORM_RADIUS; dx++) {
            for (int dz = -TERRAFORM_RADIUS; dz <= TERRAFORM_RADIUS; dz++) {
                set(level, new BlockPos(center.getX() + dx, groundY - 2, center.getZ() + dz), Blocks.DIRT);
                set(level, new BlockPos(center.getX() + dx, groundY - 1, center.getZ() + dz), Blocks.DIRT);
                set(level, new BlockPos(center.getX() + dx, groundY, center.getZ() + dz), Blocks.GRASS_BLOCK);
                for (int y = 1; y <= 30; y++) {
                    set(level, new BlockPos(center.getX() + dx, groundY + y, center.getZ() + dz), Blocks.AIR);
                }
            }
        }
    }

    private static void buildRoads(ServerLevel level, BlockPos center, int groundY) {
        for (int i = -55; i <= 55; i++) {
            for (int width = -3; width <= 3; width++) {
                set(level, new BlockPos(center.getX() + width, groundY, center.getZ() + i), Blocks.PACKED_MUD);
                set(level, new BlockPos(center.getX() + i, groundY, center.getZ() + width), Blocks.PACKED_MUD);
            }
        }
        for (int dx = -15; dx <= 15; dx++) {
            for (int dz = -15; dz <= 15; dz++) {
                if (dx * dx + dz * dz <= 225) {
                    set(level, new BlockPos(center.getX() + dx, groundY, center.getZ() + dz),
                            ((dx + dz) & 3) == 0 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS);
                }
            }
        }
    }

    private static void buildWalls(ServerLevel level, BlockPos center, int groundY) {
        for (int dx = -58; dx <= 58; dx++) {
            for (int dz = -58; dz <= 58; dz++) {
                boolean edge = Math.abs(dx) >= 56 || Math.abs(dz) >= 56;
                boolean gate = Math.abs(dx) <= 4 && Math.abs(dz) >= 56
                        || Math.abs(dz) <= 4 && Math.abs(dx) >= 56;
                if (!edge || gate) {
                    continue;
                }
                for (int y = 1; y <= 8; y++) {
                    set(level, new BlockPos(center.getX() + dx, groundY + y, center.getZ() + dz),
                            y <= 2 || y == 8 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE);
                }
                if (((dx + dz) & 1) == 0) {
                    set(level, new BlockPos(center.getX() + dx, groundY + 9, center.getZ() + dz), Blocks.STONE_BRICKS);
                }
            }
        }
        buildGate(level, center.offset(0, 0, -58), groundY, true);
        buildGate(level, center.offset(0, 0, 58), groundY, true);
        buildGate(level, center.offset(-58, 0, 0), groundY, false);
        buildGate(level, center.offset(58, 0, 0), groundY, false);
    }

    private static void buildGate(ServerLevel level, BlockPos gate, int groundY, boolean northSouth) {
        for (int side : new int[]{-8, 8}) {
            for (int across = -3; across <= 3; across++) {
                for (int y = 1; y <= 12; y++) {
                    int x = gate.getX() + (northSouth ? side : across);
                    int z = gate.getZ() + (northSouth ? across : side);
                    set(level, new BlockPos(x, groundY + y, z), y >= 9 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE);
                }
            }
        }
        for (int across = -8; across <= 8; across++) {
            for (int y = 8; y <= 10; y++) {
                int x = gate.getX() + (northSouth ? across : 0);
                int z = gate.getZ() + (northSouth ? 0 : across);
                set(level, new BlockPos(x, groundY + y, z), Blocks.STONE_BRICKS);
            }
        }
    }

    private static void buildTower(ServerLevel level, BlockPos corner, int groundY) {
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                boolean shell = Math.abs(dx) >= 5 || Math.abs(dz) >= 5;
                set(level, new BlockPos(corner.getX() + dx, groundY, corner.getZ() + dz), Blocks.STONE_BRICKS);
                if (!shell) {
                    set(level, new BlockPos(corner.getX() + dx, groundY + 1, corner.getZ() + dz), Blocks.SPRUCE_PLANKS);
                    continue;
                }
                for (int y = 1; y <= 14; y++) {
                    set(level, new BlockPos(corner.getX() + dx, groundY + y, corner.getZ() + dz),
                            y <= 3 || y >= 12 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE);
                }
            }
        }
    }

    private static void buildResidence(ServerLevel level, BlockPos origin, int groundY, Block wall, Block roof) {
        buildBuilding(level, origin, groundY, 16, 13, 6, wall, roof);
        set(level, origin.offset(3, 2, 9), Blocks.CRAFTING_TABLE);
        set(level, origin.offset(12, 2, 9), Blocks.BARREL);
    }

    private static void buildBuilding(ServerLevel level, BlockPos origin, int groundY, int width, int depth, int height, Block panel, Block roof) {
        int minX = origin.getX();
        int minZ = origin.getZ();
        int maxX = minX + width - 1;
        int maxZ = minZ + depth - 1;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                set(level, new BlockPos(x, groundY, z), Blocks.STONE_BRICKS);
                set(level, new BlockPos(x, groundY + 1, z), Blocks.SPRUCE_PLANKS);
            }
        }
        for (int y = 2; y <= height; y++) {
            for (int x = minX; x <= maxX; x++) {
                Block material = (x - minX) % 4 == 0 || y == 2 || y == 5 ? Blocks.STRIPPED_SPRUCE_WOOD : panel;
                set(level, new BlockPos(x, groundY + y, minZ), material);
                set(level, new BlockPos(x, groundY + y, maxZ), material);
            }
            for (int z = minZ; z <= maxZ; z++) {
                Block material = (z - minZ) % 4 == 0 || y == 2 || y == 5 ? Blocks.STRIPPED_SPRUCE_WOOD : panel;
                set(level, new BlockPos(minX, groundY + y, z), material);
                set(level, new BlockPos(maxX, groundY + y, z), material);
            }
        }
        int doorX = minX + width / 2;
        for (int y = 2; y <= 4; y++) {
            set(level, new BlockPos(doorX, groundY + y, minZ), Blocks.AIR);
            set(level, new BlockPos(doorX + 1, groundY + y, minZ), Blocks.AIR);
        }
        for (int x = minX + 3; x <= maxX - 3; x += 4) {
            set(level, new BlockPos(x, groundY + 4, minZ), Blocks.GLASS_PANE);
            set(level, new BlockPos(x, groundY + 4, maxZ), Blocks.GLASS_PANE);
        }
        buildConnectedRoof(level, minX, maxX, minZ, maxZ, groundY + height + 1, roof);
    }

    private static void buildConnectedRoof(ServerLevel level, int minX, int maxX, int minZ, int maxZ, int baseY, Block roof) {
        int layers = (maxX - minX + 3) / 2;
        for (int layer = 0; layer <= layers; layer++) {
            int left = minX - 1 + layer;
            int right = maxX + 1 - layer;
            int y = baseY + layer;
            for (int z = minZ - 1; z <= maxZ + 1; z++) {
                for (int thickness = 0; thickness <= 1; thickness++) {
                    set(level, new BlockPos(left + thickness, y, z), roof);
                    set(level, new BlockPos(right - thickness, y, z), roof);
                }
            }
            if (left >= right - 1) {
                for (int z = minZ - 1; z <= maxZ + 1; z++) {
                    set(level, new BlockPos((left + right) / 2, y, z), roof);
                }
                break;
            }
        }
    }

    private static void buildMarket(ServerLevel level, BlockPos center, int groundY) {
        for (int side : new int[]{-1, 1}) {
            for (int z : new int[]{-10, 10}) {
                int x = center.getX() + side * 10;
                int absoluteZ = center.getZ() + z;
                for (int dx = -3; dx <= 3; dx++) {
                    set(level, new BlockPos(x + dx, groundY + 4, absoluteZ), side > 0 ? Blocks.BRICKS : Blocks.QUARTZ_BLOCK);
                }
                set(level, new BlockPos(x - 3, groundY + 1, absoluteZ), Blocks.SPRUCE_FENCE);
                set(level, new BlockPos(x + 3, groundY + 1, absoluteZ), Blocks.SPRUCE_FENCE);
                set(level, new BlockPos(x, groundY + 1, absoluteZ), Blocks.BARREL);
            }
        }
    }

    private static void buildLamps(ServerLevel level, BlockPos center, int groundY) {
        int[][] positions = {{-18,-18},{18,-18},{-18,18},{18,18},{0,-30},{0,30},{-30,0},{30,0},{-46,-34},{46,-34},{-46,34},{46,34}};
        for (int[] position : positions) {
            int x = center.getX() + position[0];
            int z = center.getZ() + position[1];
            for (int y = 1; y <= 4; y++) {
                set(level, new BlockPos(x, groundY + y, z), Blocks.SPRUCE_FENCE);
            }
            set(level, new BlockPos(x, groundY + 5, z), Blocks.LANTERN);
        }
    }

    private static void lineFurniture(ServerLevel level, BlockPos start, int count, int spacing, Block block, boolean alongX) {
        for (int i = 0; i < count; i++) {
            set(level, start.offset(alongX ? i * spacing : 0, 0, alongX ? 0 : i * spacing), block);
        }
    }

    private static void decoratePedestal(ServerLevel level, BlockPos center, int upgradeLevel, Block material) {
        for (int step = 0; step < upgradeLevel; step++) {
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
}
