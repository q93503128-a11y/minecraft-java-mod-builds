package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.content.FrontierContent;
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
        SettlementLegacyWorkerMigrationService.tick(server, data);
        boolean explorationChanged = SettlementExplorationService.tick(server, data);
        // Building construction owns its per-tick presentation cadence. Roads, outposts and civil work
        // share the bounded 5-tick infrastructure scheduler below.
        if (data.construction().active()) SettlementConstructionService.tick(server, data);
        if (tick % 5 == 0) {
            if (data.roadConstruction().active()) SettlementRoadService.tick(server, data);
            if (data.outpostConstruction().active()) SettlementOutpostService.tick(server, data);
            if (SettlementCivilWorkData.get(server).project().active()) SettlementCivilWorkService.tick(server, data);
        }
        SettlementCoreService.tick(server, data);
        if (tick % 40 == 0) SettlementStorageService.ensureManagedStorage(server.overworld(), data);
        if (tick % 20 == 0) SettlementConstructionService.settleIdleBuilders(server, data);
        SettlementConstructionOfficeService.tick(server, data);
        SettlementBarracksService.tick(server, data);
        SettlementMilitaryOutpostService.tick(server, data);
        SettlementAdvancedWorkshopService.tick(server, data);
        // Civilian appearance does not imply a villager lifestyle simulation. Frontier work/logistics
        // remains the only runtime authority at every time of day; no bed/jobsite/night schedule layer.
        SettlementWorkerService.tick(server, data);
        SettlementWaterfrontService.tick(server, data);
        SettlementOutpostProductionService.tick(server, data);
        SettlementFishingOutpostService.tick(server, data);
        SettlementMarketService.tick(server, data);
        SettlementWorkshopService.tick(server, data);
        SettlementDeferredOutpostService.tick(server, data);
        SettlementTierInfrastructureService.tick(server, data);
        SettlementBenefitService.tick(server, data);
        if (tick % 20 == 0) {
            boolean changed = refreshResources(server, data);
            boolean activeProject = data.construction().active() || data.roadConstruction().active() || data.outpostConstruction().active();
            boolean civilProject = SettlementCivilWorkData.get(server).project().active();
            if (changed || activeProject) broadcast(server, data);
            else if (civilProject) broadcast(server, data);
            else if (explorationChanged) broadcast(server, data);
        }
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (data.founded()) {
            // A joining player may make town storage loaded again. Refresh once, then publish the
            // same authoritative snapshot to every connected player so existing HUDs cannot stay stale.
            refreshResources(server, data);
            broadcast(server, data);
        } else {
            sync(player, data);
        }
    }

    public static boolean found(ServerPlayer founder) { return foundInternal(founder, founder.blockPosition(), true).founded(); }
    public static FoundResult foundAt(ServerPlayer founder, BlockPos markerPos) { return foundInternal(founder, markerPos, true); }

    private static FoundResult foundInternal(ServerPlayer founder, BlockPos center, boolean placeMarker) {
        MinecraftServer server = founder.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (data.founded()) return new FoundResult(false, "이미 이 월드에 공동 마을이 있습니다.");
        if (founder.level() != server.overworld()) return new FoundResult(false, "개척 표식은 오버월드에서만 사용할 수 있습니다.");
        if (founder.blockPosition().distSqr(center) > MAX_MARKER_DISTANCE_SQR) return new FoundResult(false, "개척 표식은 플레이어 가까운 곳에 설치해 주세요.");
        ServerLevel level = server.overworld();
        if (placeMarker && !isSafeMarkerPosition(level, center)) return new FoundResult(false, "표식을 세울 2블록 높이의 빈 공간과 단단한 지면이 필요합니다.");
        BlockPos stockpile = findStockpilePosition(level, center);
        if (stockpile == null) return new FoundResult(false, "표식 주변에 공용 보급고를 둘 안전한 자리가 없습니다.");
        BlockState oldCenter = level.getBlockState(center);
        BlockState oldAbove = level.getBlockState(center.above());
        BlockState oldStockpile = level.getBlockState(stockpile);
        boolean centerChanged = false;
        boolean aboveChanged = false;
        if (placeMarker) {
            if (!level.setBlock(center, Blocks.OAK_FENCE.defaultBlockState(), 3)) {
                return new FoundResult(false, "개척 표식을 월드에 설치하지 못했습니다. 마을은 생성되지 않았습니다.");
            }
            centerChanged = true;
            if (!level.setBlock(center.above(), Blocks.TORCH.defaultBlockState(), 3)) {
                level.setBlock(center, oldCenter, 3);
                return new FoundResult(false, "개척 표식 횃불을 설치하지 못했습니다. 마을은 생성되지 않았습니다.");
            }
            aboveChanged = true;
        }
        if (!level.setBlock(stockpile, FrontierContent.SUPPLY_DEPOT.get().defaultBlockState(), 3)
                || !(level.getBlockEntity(stockpile) instanceof net.minecraft.world.Container)) {
            if (aboveChanged) level.setBlock(center.above(), oldAbove, 3);
            if (centerChanged) level.setBlock(center, oldCenter, 3);
            if (!level.getBlockState(stockpile).equals(oldStockpile)) level.setBlock(stockpile, oldStockpile, 3);
            return new FoundResult(false, "공용 보급고를 월드에 설치하지 못했습니다. 마을은 생성되지 않았습니다.");
        }
        SupplyDepotRegistryService.tryRegister(level, stockpile);
        data.found(center, stockpile);
        SettlementStorageService.ensureManagedStorage(level, data);
        SettlementConstructionService.ensureBuilder(level, data);
        refreshResources(server, data);
        broadcast(server, data);
        return new FoundResult(true, "공동 개척지가 시작되었습니다. 자원을 공용 보급고에 넣고 건설 위치를 정해 마을을 키우세요.");
    }

    private static boolean isSafeMarkerPosition(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.above()) || !level.hasChunkAt(pos.below())) return false;
        BlockState current = level.getBlockState(pos), above = level.getBlockState(pos.above()), below = level.getBlockState(pos.below());
        if (level.getBlockEntity(pos) != null || level.getBlockEntity(pos.above()) != null) return false;
        if (!current.getFluidState().isEmpty() || !above.getFluidState().isEmpty() || !below.getFluidState().isEmpty()) return false;
        if ((!current.isAir() && !current.canBeReplaced()) || (!above.isAir() && !above.canBeReplaced())) return false;
        return !below.isAir() && !below.canBeReplaced();
    }

    public static boolean refreshResources(MinecraftServer server, SettlementData data) { return data.founded() && data.updateResources(SettlementStorageService.scan(server.overworld(), data)); }

    private static BlockPos findStockpilePosition(ServerLevel level, BlockPos center) {
        BlockPos[] candidates = {center.offset(2,0,0),center.offset(-2,0,0),center.offset(0,0,2),center.offset(0,0,-2),center.offset(2,0,2),center.offset(-2,0,2),center.offset(2,0,-2),center.offset(-2,0,-2)};
        for (BlockPos candidate : candidates) if (isSafeStockpilePosition(level, candidate)) return candidate;
        return null;
    }

    private static boolean isSafeStockpilePosition(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.below())) return false;
        BlockState current=level.getBlockState(pos), below=level.getBlockState(pos.below());
        if(level.getBlockEntity(pos)!=null || !current.getFluidState().isEmpty() || !below.getFluidState().isEmpty()) return false;
        if (!current.isAir() && !current.canBeReplaced()) return false;
        return !below.isAir() && !below.canBeReplaced();
    }

    private static int buildingUnlockMask(SettlementData data) {
        int mask = 0;
        for (BuildingType type : BuildingType.values()) {
            String locked;
            if (type == BuildingType.WORKSHOP) locked = SettlementWorkshopService.lockedReason(data);
            else if (type == BuildingType.ADVANCED_WORKSHOP) locked = SettlementAdvancedWorkshopService.lockedReason(data);
            else if (type == BuildingType.CART_STATION) locked = SettlementCartStationService.lockedReason(data);
            else if (type == BuildingType.WATCHTOWER) locked = SettlementWatchtowerService.lockedReason(data);
            else if (type == BuildingType.BARRACKS) locked = SettlementBarracksService.lockedReason(data);
            else if (type == BuildingType.CONSTRUCTION_OFFICE) locked = SettlementConstructionOfficeService.lockedReason(data);
            else locked = SettlementConstructionService.lockedReason(data, type);
            if (locked == null) mask |= 1 << type.ordinal();
        }
        return mask;
    }

    public static void sync(ServerPlayer player, SettlementData data) {
        SettlementResources r=data.resources();
        SettlementNetwork.sendSnapshot(player,new SettlementSnapshotPayload(
                data.founded(),r.wood(),r.stone(),r.metal(),r.food(),data.population(),
                SettlementTier.current(data).displayName(),buildingUnlockMask(data),SettlementGuidanceService.nextGoal(player.level().getServer(), data),
                SettlementContextService.snapshot(player.level().getServer(), data)));
    }
    public static void broadcast(MinecraftServer server, SettlementData data) { for(ServerPlayer player:server.getPlayerList().getPlayers()) sync(player,data); }
}
