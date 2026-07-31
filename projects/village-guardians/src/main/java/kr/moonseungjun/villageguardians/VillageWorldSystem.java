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

public final class VillageWorldSystem {
    public static final String ALLOWED_MOB_TAG = "villageguardians_allowed";

    private static final int FORTRESS_RADIUS = 46;
    private static final int TERRAFORM_RADIUS = 52;
    private static final int CLEAR_HEIGHT = 15;
    private static boolean generationInProgress;

    private VillageWorldSystem() {
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

        if (isFortressBuilt(level, center)) {
            removeUnauthorizedMobs(level, center);
            return;
        }

        generationInProgress = true;
        try {
            player.sendSystemMessage(Component.literal("§6[마을 건설] §f전용 요새 마을을 준비합니다. 잠시만 기다려 주세요."));
            buildFortress(level, center);
            removeUnauthorizedMobs(level, center);
            player.sendSystemMessage(Component.literal("§a[마을 건설 완료] §f성벽·성문·감시탑·회관·병영·대장간·창고·의무소가 배치되었습니다."));
        } finally {
            generationInProgress = false;
        }
    }

    public static boolean isAllowedGameMob(Mob mob) {
        return mob.getTags().contains(ALLOWED_MOB_TAG);
    }

    public static void markAllowedGameMob(Mob mob) {
        mob.addTag(ALLOWED_MOB_TAG);
    }

    private static boolean isFortressBuilt(ServerLevel level, BlockPos center) {
        return level.getBlockState(center.below(2)).is(Blocks.GOLD_BLOCK);
    }

    private static void buildFortress(ServerLevel level, BlockPos center) {
        int baseY = center.getY() - 1;
        terraform(level, center, baseY);
        buildRoadNetwork(level, center, baseY);
        buildCurtainWall(level, center, baseY);
        buildTower(level, center.offset(-FORTRESS_RADIUS, 0, -FORTRESS_RADIUS), baseY);
        buildTower(level, center.offset(FORTRESS_RADIUS, 0, -FORTRESS_RADIUS), baseY);
        buildTower(level, center.offset(-FORTRESS_RADIUS, 0, FORTRESS_RADIUS), baseY);
        buildTower(level, center.offset(FORTRESS_RADIUS, 0, FORTRESS_RADIUS), baseY);

        buildHall(level, center.offset(-8, 0, -8), baseY, 17, 15, 8);
        buildHouse(level, center.offset(-39, 0, -23), baseY, 15, 11, 6, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS);
        furnishBarracks(level, center.offset(-39, 0, -23), baseY, 15, 11);
        buildHouse(level, center.offset(25, 0, -23), baseY, 14, 11, 6, Blocks.BRICKS, Blocks.DEEPSLATE_TILES);
        furnishSmithy(level, center.offset(25, 0, -23), baseY, 14, 11);
        buildHouse(level, center.offset(-39, 0, 13), baseY, 15, 11, 6, Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS);
        furnishStorehouse(level, center.offset(-39, 0, 13), baseY, 15, 11);
        buildHouse(level, center.offset(25, 0, 13), baseY, 14, 11, 6, Blocks.WHITE_TERRACOTTA, Blocks.BRICKS);
        furnishInfirmary(level, center.offset(25, 0, 13), baseY, 14, 11);

        buildHouse(level, center.offset(-38, 0, -40), baseY, 11, 9, 5, Blocks.STRIPPED_SPRUCE_WOOD, Blocks.DARK_OAK_PLANKS);
        buildHouse(level, center.offset(-12, 0, -40), baseY, 11, 9, 5, Blocks.OAK_PLANKS, Blocks.BRICKS);
        buildHouse(level, center.offset(16, 0, -40), baseY, 11, 9, 5, Blocks.SPRUCE_PLANKS, Blocks.DEEPSLATE_TILES);
        buildHouse(level, center.offset(-38, 0, 32), baseY, 11, 9, 5, Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS);
        buildHouse(level, center.offset(-12, 0, 32), baseY, 11, 9, 5, Blocks.STRIPPED_OAK_WOOD, Blocks.BRICKS);
        buildHouse(level, center.offset(16, 0, 32), baseY, 11, 9, 5, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS);

        buildLamp(level, center.offset(-12, 0, -12), baseY);
        buildLamp(level, center.offset(12, 0, -12), baseY);
        buildLamp(level, center.offset(-12, 0, 12), baseY);
        buildLamp(level, center.offset(12, 0, 12), baseY);
        buildLamp(level, center.offset(0, 0, -34), baseY);
        buildLamp(level, center.offset(0, 0, 34), baseY);
        buildLamp(level, center.offset(-34, 0, 0), baseY);
        buildLamp(level, center.offset(34, 0, 0), baseY);

        set(level, center.offset(0, -2, 0), Blocks.GOLD_BLOCK);
        set(level, center.offset(0, -1, 0), Blocks.CHISELED_STONE_BRICKS);
    }

    private static void terraform(ServerLevel level, BlockPos center, int baseY) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -TERRAFORM_RADIUS; dx <= TERRAFORM_RADIUS; dx++) {
            for (int dz = -TERRAFORM_RADIUS; dz <= TERRAFORM_RADIUS; dz++) {
                set(level, center.offset(dx, -3, dz), Blocks.DIRT);
                set(level, center.offset(dx, -2, dz), Blocks.DIRT);
                set(level, center.offset(dx, -1, dz), Blocks.GRASS_BLOCK);
                for (int y = 0; y <= CLEAR_HEIGHT; y++) {
                    level.setBlockAndUpdate(new BlockPos(center.getX() + dx, baseY + 1 + y, center.getZ() + dz), air);
                }
            }
        }
    }

    private static void buildRoadNetwork(ServerLevel level, BlockPos center, int baseY) {
        for (int i = -FORTRESS_RADIUS + 2; i <= FORTRESS_RADIUS - 2; i++) {
            for (int width = -2; width <= 2; width++) {
                setAbsolute(level, center.getX() + width, baseY, center.getZ() + i, Blocks.PACKED_MUD);
                setAbsolute(level, center.getX() + i, baseY, center.getZ() + width, Blocks.PACKED_MUD);
            }
        }

        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                if (dx * dx + dz * dz <= 144) {
                    setAbsolute(level, center.getX() + dx, baseY, center.getZ() + dz, Blocks.STONE_BRICKS);
                }
            }
        }
    }

    private static void buildCurtainWall(ServerLevel level, BlockPos center, int baseY) {
        for (int dx = -FORTRESS_RADIUS; dx <= FORTRESS_RADIUS; dx++) {
            for (int dz = -FORTRESS_RADIUS; dz <= FORTRESS_RADIUS; dz++) {
                boolean perimeter = Math.abs(dx) >= FORTRESS_RADIUS - 1 || Math.abs(dz) >= FORTRESS_RADIUS - 1;
                if (!perimeter || isGateOpening(dx, dz)) {
                    continue;
                }
                for (int y = 1; y <= 7; y++) {
                    setAbsolute(level, center.getX() + dx, baseY + y, center.getZ() + dz,
                            y == 1 || y == 7 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE);
                }
                if (((dx + dz) & 1) == 0) {
                    setAbsolute(level, center.getX() + dx, baseY + 8, center.getZ() + dz, Blocks.STONE_BRICKS);
                }
            }
        }
        buildGatehouse(level, center.offset(0, 0, -FORTRESS_RADIUS), baseY, true);
        buildGatehouse(level, center.offset(0, 0, FORTRESS_RADIUS), baseY, true);
        buildGatehouse(level, center.offset(-FORTRESS_RADIUS, 0, 0), baseY, false);
        buildGatehouse(level, center.offset(FORTRESS_RADIUS, 0, 0), baseY, false);
    }

    private static boolean isGateOpening(int dx, int dz) {
        return Math.abs(dx) <= 3 && Math.abs(dz) >= FORTRESS_RADIUS - 1
                || Math.abs(dz) <= 3 && Math.abs(dx) >= FORTRESS_RADIUS - 1;
    }

    private static void buildGatehouse(ServerLevel level, BlockPos gate, int baseY, boolean northSouth) {
        for (int side : new int[]{-6, 6}) {
            for (int a = -2; a <= 2; a++) {
                for (int y = 1; y <= 10; y++) {
                    int x = gate.getX() + (northSouth ? side : a);
                    int z = gate.getZ() + (northSouth ? a : side);
                    setAbsolute(level, x, baseY + y, z, y >= 8 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE);
                }
            }
        }
        for (int across = -6; across <= 6; across++) {
            for (int y = 7; y <= 9; y++) {
                int x = gate.getX() + (northSouth ? across : 0);
                int z = gate.getZ() + (northSouth ? 0 : across);
                setAbsolute(level, x, baseY + y, z, Blocks.STONE_BRICKS);
            }
        }
    }

    private static void buildTower(ServerLevel level, BlockPos corner, int baseY) {
        int radius = 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                boolean shell = Math.abs(dx) == radius || Math.abs(dz) == radius;
                if (!shell) {
                    setAbsolute(level, corner.getX() + dx, baseY, corner.getZ() + dz, Blocks.SPRUCE_PLANKS);
                    continue;
                }
                for (int y = 1; y <= 11; y++) {
                    Block block = y == 1 || y >= 9 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE;
                    setAbsolute(level, corner.getX() + dx, baseY + y, corner.getZ() + dz, block);
                }
                if (((dx + dz) & 1) == 0) {
                    setAbsolute(level, corner.getX() + dx, baseY + 12, corner.getZ() + dz, Blocks.STONE_BRICKS);
                }
            }
        }
    }

    private static void buildHall(ServerLevel level, BlockPos origin, int baseY, int width, int depth, int height) {
        buildHouse(level, origin, baseY, width, depth, height, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_TILES);
        int cx = origin.getX() + width / 2;
        int cz = origin.getZ() + depth / 2;
        for (int y = 1; y <= 3; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                level.setBlockAndUpdate(new BlockPos(cx + dx, baseY + y, origin.getZ()), Blocks.AIR.defaultBlockState());
            }
        }
        setAbsolute(level, cx, baseY + 1, cz, Blocks.CRAFTING_TABLE);
        setAbsolute(level, cx - 3, baseY + 1, cz, Blocks.BOOKSHELF);
        setAbsolute(level, cx + 3, baseY + 1, cz, Blocks.BOOKSHELF);
    }

    private static void buildHouse(ServerLevel level, BlockPos origin, int baseY, int width, int depth, int height,
                                   Block wall, Block roof) {
        int minX = origin.getX();
        int minZ = origin.getZ();
        int maxX = minX + width - 1;
        int maxZ = minZ + depth - 1;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                setAbsolute(level, x, baseY, z, Blocks.COBBLESTONE);
                setAbsolute(level, x, baseY + 1, z, Blocks.SPRUCE_PLANKS);
            }
        }

        for (int y = 2; y <= height; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z : new int[]{minZ, maxZ}) {
                    setAbsolute(level, x, baseY + y, z, wall);
                }
            }
            for (int z = minZ; z <= maxZ; z++) {
                for (int x : new int[]{minX, maxX}) {
                    setAbsolute(level, x, baseY + y, z, wall);
                }
            }
        }

        for (int y = 2; y <= 4; y++) {
            setAbsolute(level, minX + width / 2, baseY + y, minZ, Blocks.AIR);
        }
        for (int x = minX + 3; x <= maxX - 3; x += 4) {
            setAbsolute(level, x, baseY + 4, minZ, Blocks.GLASS_PANE);
            setAbsolute(level, x, baseY + 4, maxZ, Blocks.GLASS_PANE);
        }
        for (int z = minZ + 3; z <= maxZ - 3; z += 4) {
            setAbsolute(level, minX, baseY + 4, z, Blocks.GLASS_PANE);
            setAbsolute(level, maxX, baseY + 4, z, Blocks.GLASS_PANE);
        }

        for (int offset = 0; offset <= width / 2; offset++) {
            int left = minX - 1 + offset;
            int right = maxX + 1 - offset;
            int roofY = baseY + height + offset;
            if (left > right) {
                break;
            }
            for (int z = minZ - 1; z <= maxZ + 1; z++) {
                setAbsolute(level, left, roofY, z, roof);
                setAbsolute(level, right, roofY, z, roof);
            }
        }
    }

    private static void furnishBarracks(ServerLevel level, BlockPos origin, int baseY, int width, int depth) {
        for (int i = 2; i < depth - 2; i += 3) {
            setAbsolute(level, origin.getX() + 2, baseY + 2, origin.getZ() + i, Blocks.RED_BED);
            setAbsolute(level, origin.getX() + width - 3, baseY + 2, origin.getZ() + i, Blocks.RED_BED);
        }
    }

    private static void furnishSmithy(ServerLevel level, BlockPos origin, int baseY, int width, int depth) {
        setAbsolute(level, origin.getX() + 2, baseY + 2, origin.getZ() + 2, Blocks.BLAST_FURNACE);
        setAbsolute(level, origin.getX() + 3, baseY + 2, origin.getZ() + 2, Blocks.ANVIL);
        setAbsolute(level, origin.getX() + width - 3, baseY + 2, origin.getZ() + depth - 3, Blocks.SMITHING_TABLE);
    }

    private static void furnishStorehouse(ServerLevel level, BlockPos origin, int baseY, int width, int depth) {
        for (int x = 2; x < width - 2; x += 3) {
            setAbsolute(level, origin.getX() + x, baseY + 2, origin.getZ() + 2, Blocks.BARREL);
            setAbsolute(level, origin.getX() + x, baseY + 2, origin.getZ() + depth - 3, Blocks.BARREL);
        }
    }

    private static void furnishInfirmary(ServerLevel level, BlockPos origin, int baseY, int width, int depth) {
        for (int z = 2; z < depth - 2; z += 3) {
            setAbsolute(level, origin.getX() + 2, baseY + 2, origin.getZ() + z, Blocks.WHITE_BED);
            setAbsolute(level, origin.getX() + width - 3, baseY + 2, origin.getZ() + z, Blocks.WHITE_BED);
        }
        setAbsolute(level, origin.getX() + width / 2, baseY + 2, origin.getZ() + depth - 3, Blocks.BREWING_STAND);
    }

    private static void buildLamp(ServerLevel level, BlockPos pos, int baseY) {
        for (int y = 1; y <= 4; y++) {
            setAbsolute(level, pos.getX(), baseY + y, pos.getZ(), Blocks.SPRUCE_FENCE);
        }
        setAbsolute(level, pos.getX(), baseY + 5, pos.getZ(), Blocks.LANTERN);
    }

    private static void removeUnauthorizedMobs(ServerLevel level, BlockPos center) {
        AABB area = new AABB(
                center.getX() - TERRAFORM_RADIUS,
                center.getY() - 8,
                center.getZ() - TERRAFORM_RADIUS,
                center.getX() + TERRAFORM_RADIUS,
                center.getY() + 32,
                center.getZ() + TERRAFORM_RADIUS);
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
