package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Set;

/** Keeps completed-building authority tied to the physical world. */
public final class SettlementBuildingIntegrityService {
    private static final int CHECK_INTERVAL_TICKS = 100;
    private static final int RUIN_INTACT_PERCENT = 45;
    private static final Set<BuildingType> PRODUCTION_TYPES = Set.of(
            BuildingType.LUMBER_CAMP, BuildingType.FARM, BuildingType.QUARRY, BuildingType.MINE);

    private SettlementBuildingIntegrityService() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return;
        ServerLevel level = server.overworld();
        for (BuildingRecord building : List.copyOf(data.buildings())) {
            BuildingType type = BuildingType.fromId(building.type());
            if (!tracksIntegrity(type) || !fullyLoaded(level, type, building)) continue;
            List<BuildingBlueprints.Placement> plan = RotatedBlueprints.create(
                    type, building.origin(), building.rotation());
            int intact = 0;
            for (BuildingBlueprints.Placement placement : plan) {
                if (level.getBlockState(placement.pos()).is(placement.state().getBlock())) intact++;
            }
            if ((long) intact * 100L >= (long) plan.size() * RUIN_INTACT_PERCENT) continue;

            // Retire authority first. Production remnants/containers become ordinary recoverable world blocks;
            // only houses keep the Alpha.98 matching non-container remnant cleanup.
            if (data.removeCompletedBuilding(building)) {
                if (type == BuildingType.HOUSE) clearKnownHouseRemnants(level, plan);
                SettlementService.refreshResources(server, data);
                SettlementService.broadcast(server, data);
            }
            break;
        }
    }

    private static boolean tracksIntegrity(BuildingType type) {
        return type == BuildingType.HOUSE || PRODUCTION_TYPES.contains(type);
    }

    private static boolean fullyLoaded(ServerLevel level, BuildingType type, BuildingRecord building) {
        for (BuildingBlueprints.Placement placement : RotatedBlueprints.create(
                type, building.origin(), building.rotation())) {
            if (!level.hasChunkAt(placement.pos())) return false;
        }
        return true;
    }

    private static void clearKnownHouseRemnants(ServerLevel level, List<BuildingBlueprints.Placement> plan) {
        for (BuildingBlueprints.Placement placement : plan) {
            BlockPos pos = placement.pos();
            if (level.getBlockEntity(pos) != null) continue;
            BlockState current = level.getBlockState(pos);
            if (!current.is(placement.state().getBlock())) continue;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
