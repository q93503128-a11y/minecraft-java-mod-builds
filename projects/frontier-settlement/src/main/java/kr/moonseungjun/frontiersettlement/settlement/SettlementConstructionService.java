package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
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

    private SettlementConstructionService() {}

    public record StartResult(boolean started, String message) {}

    private record Site(BlockPos origin) {}

    public static StartResult start(ServerPlayer player, BuildingType type) {
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return new StartResult(false, "먼저 /frontier found로 공동 마을을 시작해야 합니다.");
        if (player.level() != server.overworld()) return new StartResult(false, "건설은 현재 오버월드 공동 마을에서만 시작할 수 있습니다.");
        if (data.construction().active()) {
            BuildingType active = BuildingType.fromId(data.construction().type());
            String name = active == null ? data.construction().type() : active.displayName();
            return new StartResult(false, "이미 " + name + " 건설이 진행 중입니다.");
        }

        SettlementService.refreshResources(server, data);
        SettlementResources resources = data.resources();
        if (resources.wood() < type.woodCost() || resources.stone() < type.stoneCost()) {
            return new StartResult(false, type.displayName() + " 필요 자원: 목재 " + type.woodCost()
                    + ", 석재 " + type.stoneCost() + " | 현재 목재 " + resources.wood()
                    + ", 석재 " + resources.stone());
        }

        ServerLevel level = server.overworld();
        Site site = findBuildSite(level, data.centerPos(), type);
        if (site == null) {
            return new StartResult(false, "주변 40블록 안에서 안전한 건설 부지를 찾지 못했습니다. 높이 차가 2블록 이하이고 물·나무·기존 건축물이 없는 공간이 필요합니다.");
        }

        if (!consumeCost(level, data, type)) {
            SettlementService.refreshResources(server, data);
            return new StartResult(false, "공동 창고 자원이 착공 직전에 변경되어 건설을 시작하지 못했습니다.");
        }

        prepareSite(level, site.origin(), type);
        data.beginConstruction(type, site.origin());
        ensureBuilder(level, data.centerPos());
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        return new StartResult(true, type.displayName() + " 착공. 건설 주민이 현장으로 이동합니다."
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
        if (construction.step() >= plan.size()) {
            return finishIfValid(server, data, type, plan);
        }

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
                // Never destroy a player-placed or newly appeared obstruction. Construction pauses
                // instead of replacing it, which also guarantees that the construction loop itself
                // cannot create item drops by breaking blocks.
                builder.getNavigation().stop();
                return false;
            }

            level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE);
            data.advanceConstruction();
            placed++;
        }

        if (data.construction().step() >= plan.size()) {
            return finishIfValid(server, data, type, plan);
        }
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
                // Do not declare a half-broken building complete. Missing blocks are retried by
                // rewinding to the first missing planned position rather than silently accepting it.
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
        int missing = -1;
        for (int i = 0; i < plan.size(); i++) {
            BuildingBlueprints.Placement placement = plan.get(i);
            if (!level.getBlockState(placement.pos()).is(placement.state().getBlock())) {
                missing = i;
                break;
            }
        }
        if (missing < 0) return;
        data.replaceConstructionStep(missing);
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
                villager -> villager.getCustomName() != null
                        && BUILDER_NAME.equals(villager.getCustomName().getString()));
        return builders.isEmpty() ? null : builders.getFirst();
    }

    private static Site findBuildSite(ServerLevel level, BlockPos center, BuildingType type) {
        int[][] directions = new int[][] {
                {1, 0}, {0, 1}, {-1, 0}, {0, -1},
                {1, 1}, {-1, 1}, {-1, -1}, {1, -1}
        };
        int[] radii = new int[] {12, 18, 24, 30, 36, 40};

        for (int radius : radii) {
            for (int[] direction : directions) {
                int centerX = center.getX() + direction[0] * radius;
                int centerZ = center.getZ() + direction[1] * radius;
                int originX = centerX - type.width() / 2;
                int originZ = centerZ - type.depth() / 2;
                Site site = assessSite(level, originX, originZ, type);
                if (site != null) return site;
            }
        }
        return null;
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

        // Include the one-block roof overhang in safety checks. We never clear containers, ores,
        // player blocks, tree trunks, or fluids. Mild natural terrain bumps up to two blocks are OK.
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
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY);
    }

    private static void prepareSite(ServerLevel level, BlockPos origin, BuildingType type) {
        // Clear top-down and use client-only block updates. This avoids support-neighbour break events,
        // so construction preparation never turns cleared vegetation/terrain into dropped item entities.
        for (int y = type.clearHeight(); y >= 0; y--) {
            for (int x = -1; x <= type.width(); x++) {
                for (int z = -1; z <= type.depth(); z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE);
                    }
                }
            }
        }

        // Continuous stone foundation at y=-1. If a low spot exists, extend down until terrain is met;
        // with the two-block site variance cap this guarantees that no floor or roof can visually float.
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
        if (data.resources().wood() < type.woodCost() || data.resources().stone() < type.stoneCost()) return false;

        long woodLeft = consume(container, type.woodCost(), true);
        long stoneLeft = consume(container, type.stoneCost(), false);
        container.setChanged();
        return woodLeft == 0L && stoneLeft == 0L;
    }

    private static long consume(Container container, long amount, boolean wood) {
        long left = amount;
        for (int slot = 0; slot < container.getContainerSize() && left > 0L; slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            boolean match = wood
                    ? (stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS))
                    : isStone(stack);
            if (!match) continue;
            int take = (int) Math.min(left, stack.getCount());
            stack.shrink(take);
            left -= take;
        }
        return left;
    }

    private static boolean isStone(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.STONE)
                || stack.is(net.minecraft.world.item.Items.COBBLESTONE)
                || stack.is(net.minecraft.world.item.Items.DEEPSLATE)
                || stack.is(net.minecraft.world.item.Items.COBBLED_DEEPSLATE)
                || stack.is(net.minecraft.world.item.Items.ANDESITE)
                || stack.is(net.minecraft.world.item.Items.DIORITE)
                || stack.is(net.minecraft.world.item.Items.GRANITE);
    }
}
