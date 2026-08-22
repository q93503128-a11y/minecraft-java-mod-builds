package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

public final class SettlementRoadService {
    public static final int ROAD_LENGTH = 16;
    public static final int ROAD_WIDTH = 3;
    public static final long ROAD_STONE_COST = 24L;
    private static final int MAX_ROUTE_HEIGHT_VARIANCE = 1;
    private static final int DIRECT_BLOCK_UPDATE = 2;
    private static final int NORMAL_BLOCK_UPDATE = 3;
    private static final double BUILDER_WORK_RANGE_SQR = 18.0D;

    private SettlementRoadService() {}

    public record StartResult(boolean started, String message) {}
    private record Route(BlockPos start, int directionX, int directionZ) {}
    private record Placement(BlockPos pos, BlockState state) {}

    public static StartResult start(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return new StartResult(false, "먼저 /frontier found로 공동 마을을 시작해야 합니다.");
        if (player.level() != server.overworld()) return new StartResult(false, "도로는 현재 오버월드 공동 마을에서만 건설할 수 있습니다.");
        if (data.construction().active()) return new StartResult(false, "건물 공사가 끝난 뒤 도로를 시작해 주세요.");
        if (data.roadConstruction().active()) return new StartResult(false, "이미 도로 공사가 진행 중입니다.");
        if (data.houseCount() < 1 || data.lumberCampCount() < 1) {
            return new StartResult(false, "첫 도로는 주택 1채와 벌목소 1곳을 완성한 뒤 열립니다.");
        }

        int[] direction = horizontalDirection(player.getYRot());
        int directionX = direction[0];
        int directionZ = direction[1];
        BlockPos center = data.centerPos();
        BlockPos playerPos = player.blockPosition();
        long relX = (long) playerPos.getX() - center.getX();
        long relZ = (long) playerPos.getZ() - center.getZ();
        long distanceSqr = relX * relX + relZ * relZ;
        if (distanceSqr < 25L || distanceSqr > 1024L) {
            return new StartResult(false, "도로 시작점은 마을 중심에서 5~32블록 떨어진 곳에 서서 지정해 주세요.");
        }
        if (relX * directionX + relZ * directionZ <= 0L) {
            return new StartResult(false, "마을 바깥쪽을 바라본 상태에서 도로를 시작해 주세요.");
        }

        ServerLevel level = server.overworld();
        Route route = assessRoute(level, data, playerPos.getX(), playerPos.getZ(), directionX, directionZ);
        if (route == null) {
            return new StartResult(false, "앞쪽 16블록에 안전한 3칸 폭 도로를 낼 수 없습니다. 물·기존 건축물·기존 도로가 없어야 하고 전체 높이 차는 1블록 이하여야 합니다.");
        }

        SettlementService.refreshResources(server, data);
        if (data.resources().stone() < ROAD_STONE_COST) {
            return new StartResult(false, "도로 필요 석재 " + ROAD_STONE_COST + " | 현재 석재 " + data.resources().stone());
        }
        if (!SettlementStorageService.consume(level, data, 0L, ROAD_STONE_COST, 0L)) {
            SettlementService.refreshResources(server, data);
            return new StartResult(false, "공동 창고 자원이 착공 직전에 변경되어 도로를 시작하지 못했습니다. 자원은 차감되지 않았습니다.");
        }

        prepareRoute(level, route);
        data.beginRoadConstruction(route.start(), route.directionX(), route.directionZ(), ROAD_LENGTH);
        SettlementConstructionService.ensureBuilder(level, data.centerPos());
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        return new StartResult(true, "16×3 개척 도로 착공. 건설 주민이 현장으로 이동합니다. (석재 -" + ROAD_STONE_COST + ")");
    }

    public static boolean tick(MinecraftServer server, SettlementData data) {
        RoadConstructionState road = data.roadConstruction();
        if (!road.active()) return false;

        List<Placement> plan = createPlan(road);
        if (road.step() >= plan.size()) return finishIfValid(server, data, road, plan);

        ServerLevel level = server.overworld();
        Villager builder = SettlementConstructionService.ensureBuilder(level, data.centerPos());
        if (builder == null) return false;
        if (builder.isNoAi()) builder.setNoAi(false);

        Placement placement = plan.get(road.step());
        BlockPos target = placement.pos();
        BlockPos work = target.above();
        double distance = builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D);
        if (distance > BUILDER_WORK_RANGE_SQR) {
            builder.getNavigation().moveTo(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D, 0.85D);
            return false;
        }

        BlockState current = level.getBlockState(target);
        if (current.is(placement.state().getBlock())) {
            data.advanceRoadConstruction();
            return false;
        }
        if (!current.isAir() || !level.getBlockState(target.above()).isAir()) {
            builder.getNavigation().stop();
            return false;
        }
        BlockState support = level.getBlockState(target.below());
        if (support.isAir() || support.canBeReplaced() || !support.getFluidState().isEmpty()) {
            builder.getNavigation().stop();
            return false;
        }

        level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE);
        data.advanceRoadConstruction();
        if (data.roadConstruction().step() >= plan.size()) {
            return finishIfValid(server, data, data.roadConstruction(), plan);
        }
        return false;
    }

    public static int totalSteps(RoadConstructionState road) {
        return road.active() ? createPlan(road).size() : ROAD_LENGTH * ROAD_WIDTH;
    }

    private static boolean finishIfValid(MinecraftServer server, SettlementData data,
                                         RoadConstructionState road, List<Placement> plan) {
        ServerLevel level = server.overworld();
        for (int i = 0; i < plan.size(); i++) {
            Placement placement = plan.get(i);
            if (!level.getBlockState(placement.pos()).is(placement.state().getBlock())) {
                data.replaceRoadConstructionStep(i);
                return false;
            }
        }

        data.completeRoad(new RoadSegment(
                road.startX(), road.startY(), road.startZ(),
                road.directionX(), road.directionZ(), road.length()));
        Villager builder = SettlementConstructionService.ensureBuilder(level, data.centerPos());
        if (builder != null) builder.getNavigation().stop();
        SettlementService.broadcast(server, data);
        return true;
    }

    private static Route assessRoute(ServerLevel level, SettlementData data,
                                     int startX, int startZ, int directionX, int directionZ) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for (int along = 0; along < ROAD_LENGTH; along++) {
            for (int side = -1; side <= 1; side++) {
                int x = startX + directionX * along - directionZ * side;
                int z = startZ + directionZ * along + directionX * side;
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                BlockPos surface = new BlockPos(x, surfaceY, z);
                if (overlapsExistingRoad(data.roads(), surface)) return null;
                if (level.getBlockEntity(surface) != null) return null;
                BlockState surfaceState = level.getBlockState(surface);
                if (!surfaceState.getFluidState().isEmpty() || !isRoadGround(surfaceState)) return null;

                for (int y = surfaceY + 1; y <= surfaceY + 2; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockEntity(pos) != null) return null;
                    BlockState state = level.getBlockState(pos);
                    if (!state.getFluidState().isEmpty()) return null;
                    if (!state.isAir() && !state.canBeReplaced() && !state.is(BlockTags.LEAVES)) return null;
                }

                min = Math.min(min, surfaceY);
                max = Math.max(max, surfaceY);
            }
        }
        if (max - min > MAX_ROUTE_HEIGHT_VARIANCE) return null;

        int roadY = min;
        for (int along = 0; along < ROAD_LENGTH; along++) {
            for (int side = -1; side <= 1; side++) {
                int x = startX + directionX * along - directionZ * side;
                int z = startZ + directionZ * along + directionX * side;
                BlockPos support = new BlockPos(x, roadY - 1, z);
                BlockState supportState = level.getBlockState(support);
                if (level.getBlockEntity(support) != null || supportState.isAir()
                        || supportState.canBeReplaced() || !supportState.getFluidState().isEmpty()) return null;

                for (int y = roadY; y <= roadY + 2; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockEntity(pos) != null) return null;
                    BlockState state = level.getBlockState(pos);
                    if (!state.getFluidState().isEmpty() || !isClearableForRoad(state)) return null;
                }
            }
        }
        return new Route(new BlockPos(startX, roadY, startZ), directionX, directionZ);
    }

    private static boolean overlapsExistingRoad(List<RoadSegment> roads, BlockPos pos) {
        for (RoadSegment segment : roads) {
            if (segment.containsXZ(pos)) return true;
        }
        return false;
    }

    private static void prepareRoute(ServerLevel level, Route route) {
        for (int along = 0; along < ROAD_LENGTH; along++) {
            for (int side = -1; side <= 1; side++) {
                int x = route.start().getX() + route.directionX() * along - route.directionZ() * side;
                int z = route.start().getZ() + route.directionZ() * along + route.directionX() * side;
                for (int y = route.start().getY() + 2; y >= route.start().getY(); y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE);
                    }
                }
            }
        }
    }

    private static List<Placement> createPlan(RoadConstructionState road) {
        List<Placement> placements = new ArrayList<>(road.length() * ROAD_WIDTH);
        for (int along = 0; along < road.length(); along++) {
            for (int side = -1; side <= 1; side++) {
                int x = road.startX() + road.directionX() * along - road.directionZ() * side;
                int z = road.startZ() + road.directionZ() * along + road.directionX() * side;
                BlockState state = side == 0
                        ? Blocks.GRAVEL.defaultBlockState()
                        : Blocks.COBBLESTONE.defaultBlockState();
                placements.add(new Placement(new BlockPos(x, road.startY(), z), state));
            }
        }
        return placements;
    }

    private static boolean isClearableForRoad(BlockState state) {
        return state.isAir()
                || state.canBeReplaced()
                || state.is(BlockTags.LEAVES)
                || isRoadGround(state);
    }

    private static boolean isRoadGround(BlockState state) {
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

    private static int[] horizontalDirection(float yaw) {
        int quadrant = Math.floorMod((int) Math.floor(yaw / 90.0F + 0.5F), 4);
        return switch (quadrant) {
            case 0 -> new int[] {0, 1};
            case 1 -> new int[] {-1, 0};
            case 2 -> new int[] {0, -1};
            default -> new int[] {1, 0};
        };
    }
}
