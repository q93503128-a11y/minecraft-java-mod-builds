package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.network.SettlementNetwork;
import kr.moonseungjun.frontiersettlement.network.SettlementSnapshotPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class SettlementService {
    private static final double MAX_MARKER_DISTANCE_SQR = 8.0D * 8.0D;

    private SettlementService() {}

    public record FoundResult(boolean founded, String message) {}

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return;

        int tick = server.getTickCount();
        if (tick % 5 == 0) {
            if (data.construction().active()) SettlementConstructionService.tick(server, data);
            if (data.roadConstruction().active()) SettlementRoadService.tick(server, data);
            if (data.outpostConstruction().active()) SettlementOutpostService.tick(server, data);
        }

        SettlementCoreService.tick(server, data);
        if (SettlementResidentRoutineService.isRestTime(server.overworld())) {
            SettlementResidentRoutineService.tick(server, data);
        } else {
            SettlementWorkerService.tick(server, data);
            SettlementOutpostProductionService.tick(server, data);
        }
        SettlementTierInfrastructureService.tick(server, data);
        SettlementBenefitService.tick(server, data);

        if (tick % 20 == 0 && refreshResources(server, data)) {
            broadcast(server, data);
        }
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (data.founded()) refreshResources(server, data);
        sync(player, data);
    }

    /** Legacy debug seam. Normal survival play uses the pioneer marker item. */
    public static boolean found(ServerPlayer founder) {
        FoundResult result = foundInternal(founder, founder.blockPosition(), false);
        return result.founded();
    }

    public static FoundResult foundAt(ServerPlayer founder, BlockPos markerPos) {
        return foundInternal(founder, markerPos, true);
    }

    private static FoundResult foundInternal(ServerPlayer founder, BlockPos center, boolean placeMarker) {
        MinecraftServer server = founder.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (data.founded()) return new FoundResult(false, "이미 이 월드에 공동 마을이 있습니다.");
        if (founder.level() != server.overworld()) {
            return new FoundResult(false, "개척 표식은 오버월드에서만 사용할 수 있습니다.");
        }
        if (founder.blockPosition().distSqr(center) > MAX_MARKER_DISTANCE_SQR) {
            return new FoundResult(false, "개척 표식은 플레이어 가까운 곳에 설치해 주세요.");
        }

        ServerLevel level = server.overworld();
        if (placeMarker && !isSafeMarkerPosition(level, center)) {
            return new FoundResult(false, "표식을 세울 2블록 높이의 빈 공간과 단단한 지면이 필요합니다.");
        }

        BlockPos stockpile = findStockpilePosition(level, center);
        if (stockpile == null) {
            return new FoundResult(false, "표식 주변에 공동 창고를 둘 안전한 자리가 없습니다.");
        }

        if (placeMarker) {
            level.setBlock(center, Blocks.OAK_FENCE.defaultBlockState(), 3);
            level.setBlock(center.above(), Blocks.TORCH.defaultBlockState(), 3);
        }
        level.setBlock(stockpile, Blocks.BARREL.defaultBlockState(), 3);
        data.found(center, stockpile);
        SettlementConstructionService.ensureBuilder(level, center);
        refreshResources(server, data);
        broadcast(server, data);
        return new FoundResult(true, "공동 개척지가 시작되었습니다. 자원을 창고에 넣고 건설 위치를 정해 마을을 키우세요.");
    }

    private static boolean isSafeMarkerPosition(ServerLevel level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());
        if (level.getBlockEntity(pos) != null || level.getBlockEntity(pos.above()) != null) return false;
        if (!current.getFluidState().isEmpty() || !above.getFluidState().isEmpty() || !below.getFluidState().isEmpty()) return false;
        if ((!current.isAir() && !current.canBeReplaced()) || (!above.isAir() && !above.canBeReplaced())) return false;
        return !below.isAir() && !below.canBeReplaced();
    }

    public static boolean refreshResources(MinecraftServer server, SettlementData data) {
        if (!data.founded()) return false;
        SettlementResources next = SettlementStorageService.scan(server.overworld(), data);
        return data.updateResources(next);
    }

    private static BlockPos findStockpilePosition(ServerLevel level, BlockPos center) {
        BlockPos[] candidates = new BlockPos[] {
                center.offset(2, 0, 0), center.offset(-2, 0, 0),
                center.offset(0, 0, 2), center.offset(0, 0, -2),
                center.offset(2, 0, 2), center.offset(-2, 0, 2),
                center.offset(2, 0, -2), center.offset(-2, 0, -2)
        };
        for (BlockPos candidate : candidates) {
            if (isSafeStockpilePosition(level, candidate)) return candidate;
        }
        return null;
    }

    private static boolean isSafeStockpilePosition(ServerLevel level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        BlockState below = level.getBlockState(pos.below());
        if (level.getBlockEntity(pos) != null) return false;
        if (!current.getFluidState().isEmpty() || !below.getFluidState().isEmpty()) return false;
        if (!current.isAir() && !current.canBeReplaced()) return false;
        return !below.isAir() && !below.canBeReplaced();
    }

    private static int buildingUnlockMask(SettlementData data) {
        int mask = 0;
        for (BuildingType type : BuildingType.values()) {
            if (SettlementConstructionService.lockedReason(data, type) == null) {
                mask |= 1 << type.ordinal();
            }
        }
        return mask;
    }

    public static void sync(ServerPlayer player, SettlementData data) {
        SettlementResources r = data.resources();
        SettlementNetwork.sendSnapshot(player, new SettlementSnapshotPayload(
                data.founded(), r.wood(), r.stone(), r.metal(), r.food(), data.population(),
                SettlementTier.current(data).displayName(), buildingUnlockMask(data)));
    }

    public static void broadcast(MinecraftServer server, SettlementData data) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sync(player, data);
    }
}
