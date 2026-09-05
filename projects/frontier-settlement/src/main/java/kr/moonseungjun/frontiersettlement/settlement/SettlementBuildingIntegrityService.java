package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Keeps the settlement ledger tied to the physical world for completed houses.
 *
 * A heavily burned/exploded house must not keep granting housing forever or permanently reserve a
 * dead lot. We only retire a house after every blueprint position is loaded and fewer than 45% of
 * its expected blocks remain. Retirement clears only blocks that still exactly match the Frontier
 * house blueprint, and never deletes block entities or arbitrary player blocks.
 */
public final class SettlementBuildingIntegrityService {
    private static final int CHECK_INTERVAL_TICKS = 100;
    private static final int RUIN_INTACT_PERCENT = 45;

    private SettlementBuildingIntegrityService() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return;
        ServerLevel level = server.overworld();
        for (BuildingRecord building : List.copyOf(data.buildings())) {
            BuildingType type = BuildingType.fromId(building.type());
            if (type != BuildingType.HOUSE) continue;
            if (!fullyLoaded(level, type, building)) continue;
            List<BuildingBlueprints.Placement> plan = RotatedBlueprints.create(
                    type, building.origin(), building.rotation());
            int intact = 0;
            for (BuildingBlueprints.Placement placement : plan) {
                if (level.getBlockState(placement.pos()).is(placement.state().getBlock())) intact++;
            }
            if ((long) intact * 100L >= (long) plan.size() * RUIN_INTACT_PERCENT) continue;

            clearKnownHouseRemnants(level, plan);
            if (data.removeCompletedBuilding(building)) {
                SettlementService.refreshResources(server, data);
                SettlementService.broadcast(server, data);
            }
            break; // bounded: retire at most one ruined house per scan
        }
    }

    private static boolean fullyLoaded(ServerLevel level, BuildingType type, BuildingRecord building) {
        List<BuildingBlueprints.Placement> plan = RotatedBlueprints.create(type, building.origin(), building.rotation());
        for (BuildingBlueprints.Placement placement : plan) {
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
