package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SettlementConstructionService {
    static final String BUILDER_TAG = "frontier_settlement_builder";
    private static final String BUILDER_NAME = "건설 주민";
    private static final int DIRECT_BLOCK_UPDATE = 2;
    private static final int NORMAL_BLOCK_UPDATE = 3;
    private static final double BUILDER_WORK_RANGE_SQR = 22.0D;
    private static final int COMMAND_PLACEMENT_DISTANCE = 10;
    private static final int MAX_MAIN_SETTLEMENT_RADIUS = 72;

    private SettlementConstructionService() {}

    public record StartResult(boolean started, String message) {}
    public record PlacementCheck(boolean valid, BlockPos origin, String message) {}
    private record Site(BlockPos origin) {}

    /**
     * Temporary command seam. The final client placement UI will call startAt with its selected
     * ghost-preview center. For now the player simply faces the intended area and the command uses
     * a point ten blocks ahead, so the game no longer chooses a random nearby lot on its own.
     */
    public static StartResult start(ServerPlayer player, BuildingType type) {
        Direction facing = player.getDirection();
        BlockPos selectedCenter = player.blockPosition().relative(facing, COMMAND_PLACEMENT_DISTANCE);
        return startAt(player, type, selectedCenter);
    }

    public static PlacementCheck checkPlacement(ServerPlayer player, BuildingType type, BlockPos selectedCenter) {
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return new PlacementCheck(false, BlockPos.ZERO, "공동 마을이 없습니다.");
        if (player.level() != server.overworld()) return new PlacementCheck(false, BlockPos.ZERO, "오버월드에서만 배치할 수 있습니다.");

        long dx = (long) selectedCenter.getX() - data.centerPos().getX();
        long dz = (long) selectedCenter.getZ() - data.centerPos().getZ();
        if (dx * dx + dz * dz > (long) MAX_MAIN_SETTLEMENT_RADIUS * MAX_MAIN_SETTLEMENT_RADIUS) {
            return new PlacementCheck(false, BlockPos.ZERO, "본진 기능 건물은 마을 중심 72블록 안에 배치해 주세요. 먼 지역은 전초기지를 사용합니다.");
        }

        int originX = selectedCenter.getX() - type.width() / 2;
        int originZ = selectedCenter.getZ() - type.depth() / 2;
        ServerLevel level = server.overworld();
        Site site = assessSite(level, originX, originZ, type);
        if (site == null) {
            return new PlacementCheck(false, BlockPos.ZERO,
                    "선택한 부지가 안전하지 않습니다. 높이 차 2블록 이하의 물·나무·기존 건축물이 없는 곳을 선택해 주세요.");
        }
        if (overlapsInfrastructure(data, site.origin(), type)) {
            return new PlacementCheck(false, BlockPos.ZERO, "선택한 부지가 기존 건물·도로·전초기지 또는 공동 창고와 겹칩니다.");
        }
        return new PlacementCheck(true, site.origin(), "배치 가능");
    }

    public static StartResult startAt(ServerPlayer player, BuildingType type, BlockPos selectedCenter) {
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return new StartResult(false, "먼저 /frontier found로 공동 마을을 시작해야 합니다.");
        if (player.level() != server.overworld()) return new StartResult(false, "건설은 현재 오버월드 공동 마을에서만 시작할 수 있습니다.");
        if (data.construction().active()) {
            BuildingType active = BuildingType.fromId(data.construction().type());
            String name = active == null ? data.construction().type() : active.displayName();
            return new StartResult(false, "이미 " + name + " 건설이 진행 중입니다.");
        }
        if (data.roadConstruction().active() || data.outpostConstruction().active()) {
            return new StartResult(false, "현재 인프라 공사가 끝난 뒤 건물을 시작해 주세요.");
        }

        PlacementCheck check = checkPlacement(player, type, selectedCenter);
        if (!check.valid()) return new StartResult(false, check.message());

        SettlementService.refreshResources(server, data);
        SettlementResources resources = data.resources();
        if (resources.wood() < type.woodCost() || resources.stone() < type.stoneCost()) {
            return new StartResult(false, type.displayName() + " 필요 자원: 목재 " + type.woodCost()
                    + ", 석재 " + type.stoneCost() + " | 현재 목재 " + resources.wood()
                    + ", 석재 " + resources.stone());
        }

        ServerLevel level = server.overworld();
        if (!consumeCost(level, data, type)) {
            SettlementService.refreshResources(server, data);
            return new StartResult(false, "공동 창고 자원이 착공 직전에 변경되어 건설을 시작하지 못했습니다. 자원은 차감되지 않았습니다.");
        }

        prepareSite(level, check.origin(), type);
        data.beginConstruction(type, check.origin());
        ensureBuilder(level, data.centerPos());
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        return new StartResult(true, type.displayName() + " 착공. 선택한 위치에서 건설 주민이 작업을 시작합니다."
                + " (목재 -" + type.woodCost() + ", 석재 -" + type.stoneCost() + ")");
    }

    public static boolean tick(MinecraftServer server, SettlementData data) {
        ConstructionState construction = data.construction();
        if (!construction.active()) return false;

        BuildingType type = BuildingType.fromId(construction.type());
        if (type == null) {
            data.clearConstruction();
            return true;
        }

        ServerLevel level = server.overworld();
        List<BuildingBlueprints.Placement> plan = BuildingBlueprints.create(type, construction.origin());
        if (construction.step() >= plan.size()) return finishIfValid(server, data, type, plan);

        Villager builder = ensureBuilder(level, data.centerPos());
        if (builder == null) return false;
        if (builder.isNoAi()) builder.setNoAi(false);

        int placed = 0;
        while (placed < 2 && data.construction().step() < plan.size()) {
            BuildingBlueprints.Placement placement = plan.get(data.construction().step());
            BlockPos target = placement.pos();
            BlockPos work = new BlockPos(target.getX(), construction.originY(), target.getZ());
            double distance = builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D);
            if (distance > BUILDER_WORK_RANGE_SQR) {
                builder.getNavigation().moveTo(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D, 0.85D);
                break;
            }

            BlockState current = level.getBlockState(target);
            if (current.is(placement.state().getBlock())) {
                data.advanceConstruction();
                continue;
            }
            if (!current.isAir()) {
                builder.getNavigation().stop();
                return false;
            }

            level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE);
            data.advanceConstruction();
            placed++;
        }
        if (data.construction().step() >= plan.size()) return finishIfValid(server, data, type, plan);
        return false;
    }

    public static int totalSteps(BuildingType type, BlockPos origin) {
        return BuildingBlueprints.create(type, origin).size();
    }

    private static boolean finishIfValid(MinecraftServer server, SettlementData data, BuildingType type,
                                         List<BuildingBlueprints.Placement> plan) {
        ServerLevel level = server.overworld();
        for (BuildingBlueprints.Placement placement : plan) {
            if (!level.getBlockState(placement.pos()).is(placement.state().getBlock())) {
                rewindToFirstMissing(data, plan, level);
                return false;
            }
        }
        data.completeConstruction(type);
        Villager builder = findBuilder(level, data.centerPos());
        if (builder != null) {
            builder.getNavigation().stop();
            builder.setCustomName(Component.literal(BUILDER_NAME));
        }
        SettlementService.broadcast(server, data);
        return true;
    }

    private static void rewindToFirstMissing(SettlementData data, List<BuildingBlueprints.Placement> plan,
                                             ServerLevel level) {
        for (int i = 0; i < plan.size(); i++) {
            BuildingBlueprints.Placement placement = plan.get(i);
            if (!level.getBlockState(placement.pos()).is(placement.state().getBlock())) {
                data.replaceConstructionStep(i);
                return;
            }
        }
    }

    public static Villager ensureBuilder(ServerLevel level, BlockPos center) {
        Villager existing = findBuilder(level, center);
        if (existing != null) {
            if (existing.isNoAi()) existing.setNoAi(false);
            return existing;
        }
        Villager builder = new Villager(EntityTypes.VILLAGER, level);
        BlockPos spawn = center.offset(1, 0, 1);
        builder.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        builder.setYRot(0.0F);
        builder.setXRot(0.0F);
        builder.setCustomName(Component.literal(BUILDER_NAME));
        builder.setCustomNameVisible(true);
        builder.setPersistenceRequired();
        builder.setNoAi(false);
        builder.addTag(BUILDER_TAG);
        level.addFreshEntity(builder);
        return builder;
    }

    private static Villager findBuilder(ServerLevel level, BlockPos center) {
        AABB search = new AABB(
                center.getX() - 96.0D, center.getY() - 48.0D, center.getZ() - 96.0D,
                center.getX() + 97.0D, center.getY() + 49.0D, center.getZ() + 97.0D);
        List<Villager> builders = level.getEntitiesOfClass(Villager.class, search,
                villager -> villager.getCustomName() != null && BUILDER_NAME.equals(villager.getCustomName().getString()));
        return builders.isEmpty() ? null : builders.getFirst();
    }

    private static Site assessSite(ServerLevel level, int originX, int originZ, BuildingType type) {
        List<Integer> heights = new ArrayList<>(type.width() * type.depth());
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int x = 0; x < type.width(); x++) {
            for (int z = 0; z < type.depth(); z++) {
                int worldX = originX + x;
                int worldZ = originZ + z;
                int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ);
                BlockPos surfaceBlock = new BlockPos(worldX, height - 1, worldZ);
                if (!level.getFluidState(surfaceBlock).isEmpty()) return null;
                heights.add(height);
                min = Math.min(min, height);
                max = Math.max(max, height);
            }
        }
        if (max - min > 2) return null;
        Collections.sort(heights);
        int baseY = heights.get(heights.size() / 2);
        BlockPos origin = new BlockPos(originX, baseY, originZ);

        for (int x = -1; x <= type.width(); x++) {
            for (int z = -1; z <= type.depth(); z++) {
                for (int y = -3; y <= type.clearHeight(); y++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (level.getBlockEntity(pos) != null) return null;
                    BlockState state = level.getBlockState(pos);
                    if (!state.getFluidState().isEmpty()) return null;
                    if (y >= 0 && !isSafeAboveGround(state, y)) return null;
                    if (y == -1 && !state.isAir() && !isNaturalGround(state)) return null;
                }
            }
        }
        return new Site(origin);
    }

    private static boolean overlapsInfrastructure(SettlementData data, BlockPos origin, BuildingType type) {
        int minX = origin.getX() - 1;
        int maxX = origin.getX() + type.width();
        int minZ = origin.getZ() - 1;
        int maxZ = origin.getZ() + type.depth();

        BlockPos stock = data.stockpilePos();
        if (stock.getX() >= minX && stock.getX() <= maxX && stock.getZ() >= minZ && stock.getZ() <= maxZ) return true;

        for (BuildingRecord existing : data.buildings()) {
            BuildingType oldType = existing.buildingType();
            if (oldType == null) continue;
            int oldMinX = existing.originX() - 1;
            int oldMaxX = existing.originX() + oldType.width();
            int oldMinZ = existing.originZ() - 1;
            int oldMaxZ = existing.originZ() + oldType.depth();
            if (minX <= oldMaxX && maxX >= oldMinX && minZ <= oldMaxZ && maxZ >= oldMinZ) return true;
        }
        for (RoadSegment road : data.roads()) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (road.containsXZ(new BlockPos(x, origin.getY(), z))) return true;
                }
            }
        }
        for (OutpostRecord outpost : data.outposts()) {
            if (outpost.centerX() + 6 >= minX && outpost.centerX() - 6 <= maxX
                    && outpost.centerZ() + 6 >= minZ && outpost.centerZ() - 6 <= maxZ) return true;
        }
        return false;
    }

    private static boolean isSafeAboveGround(BlockState state, int relativeY) {
        if (state.isAir() || state.canBeReplaced() || state.is(BlockTags.LEAVES)) return true;
        return relativeY <= 1 && isNaturalGround(state);
    }

    private static boolean isNaturalGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.STONE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK);
    }

    private static void prepareSite(ServerLevel level, BlockPos origin, BuildingType type) {
        for (int y = type.clearHeight(); y >= 0; y--) {
            for (int x = -1; x <= type.width(); x++) {
                for (int z = -1; z <= type.depth(); z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE);
                }
            }
        }
        for (int x = 0; x < type.width(); x++) {
            for (int z = 0; z < type.depth(); z++) {
                BlockPos top = origin.offset(x, -1, z);
                level.setBlock(top, Blocks.COBBLESTONE.defaultBlockState(), DIRECT_BLOCK_UPDATE);
                for (int down = 2; down <= 4; down++) {
                    BlockPos support = origin.offset(x, -down, z);
                    if (!level.getBlockState(support).isAir()) break;
                    level.setBlock(support, Blocks.COBBLESTONE.defaultBlockState(), DIRECT_BLOCK_UPDATE);
                }
            }
        }
    }

    private static boolean consumeCost(ServerLevel level, SettlementData data, BuildingType type) {
        if (!(level.getBlockEntity(data.stockpilePos()) instanceof Container container)) return false;
        if (count(container, true) < type.woodCost() || count(container, false) < type.stoneCost()) return false;
        long woodLeft = consume(container, type.woodCost(), true);
        long stoneLeft = consume(container, type.stoneCost(), false);
        if (woodLeft != 0L || stoneLeft != 0L) return false;
        container.setChanged();
        return true;
    }

    private static long count(Container container, boolean wood) {
        long total = 0L;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            boolean match = wood ? (stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS)) : SettlementInventory.isStone(stack);
            if (match) total += stack.getCount();
        }
        return total;
    }

    private static long consume(Container container, long amount, boolean wood) {
        long left = amount;
        for (int slot = 0; slot < container.getContainerSize() && left > 0L; slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            boolean match = wood ? (stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS)) : SettlementInventory.isStone(stack);
            if (!match) continue;
            int take = (int) Math.min(left, stack.getCount());
            stack.shrink(take);
            left -= take;
        }
        return left;
    }
}
