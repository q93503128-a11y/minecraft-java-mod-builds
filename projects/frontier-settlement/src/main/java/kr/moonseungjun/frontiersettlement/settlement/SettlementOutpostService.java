package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.Tags;

import java.util.List;

public final class SettlementOutpostService {
    public static final long WOOD_COST = 72L;
    public static final long STONE_COST = 48L;
    public static final int MAX_TARGET_DISTANCE_FROM_ROAD_END = 8;
    public static final int MAX_PLAYER_DISTANCE_FROM_ROAD_END = 48;
    private static final int DIRECT_BLOCK_UPDATE = 2;
    private static final int NORMAL_BLOCK_UPDATE = 3;
    private static final double BUILDER_WORK_RANGE_SQR = 22.0D;

    private SettlementOutpostService() {}

    public record StartResult(boolean started, String message) {}
    public record PlacementCheck(boolean valid, int roadIndex, BlockPos gate,
                                 int directionX, int directionZ,
                                 String specialization, String message) {
        public static PlacementCheck invalid(String message) {
            return new PlacementCheck(false, -1, BlockPos.ZERO, 0, 0, "general", message);
        }
    }

    public static StartResult start(ServerPlayer player) {
        SettlementData data = SettlementData.get(player.level().getServer());
        int roadIndex = latestUnclaimedRoad(data);
        if (roadIndex < 0) return new StartResult(false, "연결되지 않은 완성 도로가 필요합니다.");
        return startAt(player, roadIndex);
    }

    public static PlacementCheck checkPlacement(ServerPlayer player, BlockPos selected) {
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return PlacementCheck.invalid("먼저 공동 마을을 시작해야 합니다.");
        if (player.level() != server.overworld()) return PlacementCheck.invalid("전초기지는 오버월드에만 건설할 수 있습니다.");
        if (data.construction().active() || data.roadConstruction().active() || data.outpostConstruction().active()) {
            return PlacementCheck.invalid("현재 공사가 끝난 뒤 전초기지를 배치해 주세요.");
        }

        int roadIndex = nearestUnclaimedRoad(data, selected);
        if (roadIndex < 0) return PlacementCheck.invalid("사용하지 않은 완성 도로 끝을 가리켜 주세요.");

        RoadSegment road = data.roads().get(roadIndex);
        BlockPos roadEnd = road.end();
        double playerDistance = player.blockPosition().distSqr(roadEnd);
        if (playerDistance > (double) MAX_PLAYER_DISTANCE_FROM_ROAD_END * MAX_PLAYER_DISTANCE_FROM_ROAD_END) {
            return new PlacementCheck(false, roadIndex, gateFor(road), road.directionX(), road.directionZ(),
                    "general", "전초기지를 세울 도로 끝에서 48블록 안으로 이동해 주세요.");
        }

        BlockPos gate = gateFor(road);
        ServerLevel level = server.overworld();
        if (!assessSite(level, data, roadIndex, gate, road.directionX(), road.directionZ())) {
            return new PlacementCheck(false, roadIndex, gate, road.directionX(), road.directionZ(),
                    "general", "도로 끝의 9×9 부지가 안전하지 않습니다. 물·기존 건축물·큰 경사를 피해주세요.");
        }

        BlockPos center = outpostCenter(gate, road.directionX(), road.directionZ());
        String specialization = detectSpecialization(level, center);
        SettlementService.refreshResources(server, data);
        if (data.resources().wood() < WOOD_COST || data.resources().stone() < STONE_COST) {
            return new PlacementCheck(false, roadIndex, gate, road.directionX(), road.directionZ(), specialization,
                    "전초기지 필요 자원: 목재 " + WOOD_COST + " · 석재 " + STONE_COST);
        }

        return new PlacementCheck(true, roadIndex, gate, road.directionX(), road.directionZ(), specialization,
                "배치 가능 · " + specializationDisplayName(specialization) + " 후보");
    }

    public static StartResult startAt(ServerPlayer player, int roadIndex) {
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return new StartResult(false, "먼저 공동 마을을 시작해야 합니다.");
        if (player.level() != server.overworld()) return new StartResult(false, "전초기지는 오버월드에만 건설할 수 있습니다.");
        if (data.construction().active() || data.roadConstruction().active() || data.outpostConstruction().active()) {
            return new StartResult(false, "현재 공사가 끝난 뒤 전초기지를 시작해 주세요.");
        }
        if (roadIndex < 0 || roadIndex >= data.roads().size() || isRoadClaimed(data, roadIndex)) {
            return new StartResult(false, "선택한 도로는 전초기지에 연결할 수 없습니다.");
        }

        RoadSegment road = data.roads().get(roadIndex);
        BlockPos gate = gateFor(road);
        if (player.blockPosition().distSqr(road.end())
                > (double) MAX_PLAYER_DISTANCE_FROM_ROAD_END * MAX_PLAYER_DISTANCE_FROM_ROAD_END) {
            return new StartResult(false, "선택한 도로 끝에서 48블록 안으로 이동해 주세요.");
        }

        ServerLevel level = server.overworld();
        if (!assessSite(level, data, roadIndex, gate, road.directionX(), road.directionZ())) {
            return new StartResult(false, "도로 끝의 전초기지 부지가 더 이상 안전하지 않습니다.");
        }

        SettlementService.refreshResources(server, data);
        if (!SettlementStorageService.consume(level, data, WOOD_COST, STONE_COST, 0L)) {
            SettlementService.refreshResources(server, data);
            return new StartResult(false, "전초기지 필요 자원이 부족하거나 착공 직전에 재고가 변경되었습니다. 자원은 부분 차감되지 않습니다.");
        }

        prepareSite(level, gate, road.directionX(), road.directionZ());
        data.beginOutpostConstruction(roadIndex, gate, road.directionX(), road.directionZ());
        SettlementConstructionService.ensureBuilder(level, data.centerPos());
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        return new StartResult(true, "전초기지 착공. 완공 시 주변 자원을 읽어 역할이 자동 결정됩니다.");
    }

    public static boolean tick(MinecraftServer server, SettlementData data) {
        OutpostConstructionState state = data.outpostConstruction();
        if (!state.active()) return false;
        List<OutpostBlueprints.Placement> plan = OutpostBlueprints.create(state);
        if (state.step() >= plan.size()) return finishIfValid(server, data, state, plan);

        ServerLevel level = server.overworld();
        Villager builder = SettlementConstructionService.ensureBuilder(level, data.centerPos());
        if (builder == null) return false;
        int placed = 0;
        while (placed < 2 && data.outpostConstruction().step() < plan.size()) {
            OutpostBlueprints.Placement placement = plan.get(data.outpostConstruction().step());
            BlockPos target = placement.pos();
            BlockPos work = target.above();
            double distance = builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D);
            if (distance > BUILDER_WORK_RANGE_SQR) {
                builder.getNavigation().moveTo(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D, 0.85D);
                break;
            }
            BlockState current = level.getBlockState(target);
            if (current.is(placement.state().getBlock())) { data.advanceOutpostConstruction(); continue; }
            if (!current.isAir()) { builder.getNavigation().stop(); return false; }
            level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE);
            data.advanceOutpostConstruction();
            placed++;
        }
        if (data.outpostConstruction().step() >= plan.size()) return finishIfValid(server, data, data.outpostConstruction(), plan);
        return false;
    }

    public static int totalSteps(OutpostConstructionState state) {
        return state.active() ? OutpostBlueprints.create(state).size() : 0;
    }

    private static boolean finishIfValid(MinecraftServer server, SettlementData data,
                                         OutpostConstructionState state,
                                         List<OutpostBlueprints.Placement> plan) {
        ServerLevel level = server.overworld();
        for (int i = 0; i < plan.size(); i++) {
            OutpostBlueprints.Placement placement = plan.get(i);
            if (!level.getBlockState(placement.pos()).is(placement.state().getBlock())) {
                data.replaceOutpostConstructionStep(i);
                return false;
            }
        }
        BlockPos stockpile = OutpostBlueprints.stockpile(state);
        if (!(level.getBlockEntity(stockpile) instanceof Container)) {
            data.replaceOutpostConstructionStep(Math.max(0, plan.size() - 2));
            return false;
        }

        BlockPos center = OutpostBlueprints.center(state);
        String specialization = detectSpecialization(level, center);
        OutpostRecord outpost = new OutpostRecord(
                data.outposts().size() + 1,
                center.getX(), center.getY(), center.getZ(),
                stockpile.getX(), stockpile.getY(), stockpile.getZ(),
                state.roadIndex(), specialization);
        data.completeOutpost(outpost);
        Villager builder = SettlementConstructionService.ensureBuilder(level, data.centerPos());
        if (builder != null) builder.getNavigation().stop();
        SettlementService.broadcast(server, data);
        return true;
    }

    private static BlockPos gateFor(RoadSegment road) {
        return road.end().offset(road.directionX(), 0, road.directionZ());
    }

    private static BlockPos outpostCenter(BlockPos gate, int directionX, int directionZ) {
        return gate.offset(directionX * 4, 0, directionZ * 4);
    }

    private static String detectSpecialization(ServerLevel level, BlockPos center) {
        int ores = 0;
        int logs = 0;
        int fieldGround = 0;
        int exposedStone = 0;
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                if (dx * dx + dz * dz > 144) continue;
                for (int dy = -12; dy <= 8; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Tags.Blocks.ORES)) ores++;
                    if (dy >= -2 && dy <= 7 && state.is(BlockTags.LOGS)) logs++;
                    if (dy >= -1 && dy <= 1 && (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT))) fieldGround++;
                    if (dy >= -3 && dy <= 3 && isStone(state) && level.getBlockState(pos.above()).isAir()) exposedStone++;
                }
            }
        }
        if (ores >= 4) return "mining";
        if (logs >= 24) return "lumber";
        if (fieldGround >= 120) return "agriculture";
        if (exposedStone >= 24) return "quarry";
        return "general";
    }

    public static String specializationDisplayName(String specialization) {
        return switch (specialization) {
            case "mining" -> "광업";
            case "lumber" -> "벌목";
            case "agriculture" -> "농업";
            case "quarry" -> "채석";
            default -> "일반";
        };
    }

    private static boolean isStone(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE) || state.is(Blocks.TUFF);
    }

    private static int latestUnclaimedRoad(SettlementData data) {
        for (int i = data.roads().size() - 1; i >= 0; i--) if (!isRoadClaimed(data, i)) return i;
        return -1;
    }

    private static int nearestUnclaimedRoad(SettlementData data, BlockPos selected) {
        double bestDistance = (double) MAX_TARGET_DISTANCE_FROM_ROAD_END * MAX_TARGET_DISTANCE_FROM_ROAD_END + 1.0D;
        int bestIndex = -1;
        for (int i = 0; i < data.roads().size(); i++) {
            if (isRoadClaimed(data, i)) continue;
            double distance = selected.distSqr(data.roads().get(i).end());
            if (distance <= (double) MAX_TARGET_DISTANCE_FROM_ROAD_END * MAX_TARGET_DISTANCE_FROM_ROAD_END
                    && distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static boolean isRoadClaimed(SettlementData data, int roadIndex) {
        return data.outposts().stream().anyMatch(outpost -> outpost.roadIndex() == roadIndex);
    }

    private static boolean assessSite(ServerLevel level, SettlementData data, int roadIndex,
                                      BlockPos gate, int directionX, int directionZ) {
        int roadY = gate.getY();
        for (int forward = 0; forward < OutpostBlueprints.LENGTH; forward++) {
            for (int side = -4; side <= 4; side++) {
                int x = gate.getX() + directionX * forward - directionZ * side;
                int z = gate.getZ() + directionZ * forward + directionX * side;
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                if (Math.abs(surfaceY - roadY) > 1) return false;
                BlockPos surface = new BlockPos(x, surfaceY, z);
                BlockState ground = level.getBlockState(surface);
                if (level.getBlockEntity(surface) != null || !ground.getFluidState().isEmpty() || !isNaturalGround(ground)) return false;
                BlockPos footprint = new BlockPos(x, roadY, z);
                if (overlapsProtectedInfrastructure(data, roadIndex, footprint)) return false;
                for (int y = roadY; y <= roadY + OutpostBlueprints.CLEAR_HEIGHT; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockEntity(pos) != null) return false;
                    BlockState state = level.getBlockState(pos);
                    if (!state.getFluidState().isEmpty() || state.is(BlockTags.LOGS)) return false;
                    if (!state.isAir() && !state.canBeReplaced() && !state.is(BlockTags.LEAVES)
                            && !(y <= roadY + 1 && isNaturalGround(state))) return false;
                }
            }
        }
        return true;
    }

    private static boolean overlapsProtectedInfrastructure(SettlementData data, int connectedRoadIndex, BlockPos pos) {
        for (BuildingRecord building : data.buildings()) if (building.protectsXZ(pos, 1)) return true;
        for (OutpostRecord outpost : data.outposts()) if (outpost.protectsXZ(pos, 1)) return true;
        for (int i = 0; i < data.roads().size(); i++) {
            if (i == connectedRoadIndex) continue;
            if (data.roads().get(i).containsXZ(pos)) return true;
        }
        return false;
    }

    private static void prepareSite(ServerLevel level, BlockPos gate, int directionX, int directionZ) {
        int roadY = gate.getY();
        for (int forward = 0; forward < OutpostBlueprints.LENGTH; forward++) {
            for (int side = -4; side <= 4; side++) {
                int x = gate.getX() + directionX * forward - directionZ * side;
                int z = gate.getZ() + directionZ * forward + directionX * side;
                for (int y = roadY + OutpostBlueprints.CLEAR_HEIGHT; y >= roadY; y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE);
                }
                BlockPos support = new BlockPos(x, roadY - 1, z);
                for (int down = 0; down < 3; down++) {
                    BlockPos fill = support.below(down);
                    BlockState current = level.getBlockState(fill);
                    if (!current.isAir() && !current.canBeReplaced()) break;
                    level.setBlock(fill, Blocks.COBBLESTONE.defaultBlockState(), DIRECT_BLOCK_UPDATE);
                }
            }
        }
    }

    private static boolean isNaturalGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.STONE)
                || state.is(Blocks.ANDESITE) || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE)
                || state.is(Blocks.TUFF) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK);
    }
}
