package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.network.SettlementNetwork;
import kr.moonseungjun.frontiersettlement.network.SettlementSnapshotPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class SettlementService {
    private SettlementService() {}

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

    public static boolean found(ServerPlayer founder) {
        MinecraftServer server = founder.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (data.founded()) return false;
        if (founder.level() != server.overworld()) return false;

        ServerLevel level = server.overworld();
        BlockPos center = founder.blockPosition();
        BlockPos stockpile = findStockpilePosition(level, center);
        if (stockpile == null) return false;

        level.setBlock(stockpile, Blocks.BARREL.defaultBlockState(), 3);
        data.found(center, stockpile);
        SettlementConstructionService.ensureBuilder(level, center);
        refreshResources(server, data);
        broadcast(server, data);
        return true;
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
            if (level.getBlockState(candidate).isAir()) return candidate;
        }
        return null;
    }

    public static void sync(ServerPlayer player, SettlementData data) {
        SettlementResources r = data.resources();
        SettlementNetwork.sendSnapshot(player, new SettlementSnapshotPayload(
                data.founded(), r.wood(), r.stone(), r.metal(), r.food(), data.population(),
                SettlementTier.current(data).displayName()));
    }

    public static void broadcast(MinecraftServer server, SettlementData data) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sync(player, data);
    }
}
