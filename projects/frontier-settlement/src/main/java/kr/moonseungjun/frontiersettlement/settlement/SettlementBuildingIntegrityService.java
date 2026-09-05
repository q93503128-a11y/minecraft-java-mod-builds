package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Keeps completed-building authority tied to the physical world. */
public final class SettlementBuildingIntegrityService {
    private static final int REPAIR_INTERVAL_TICKS = 20;
    private static final int CHECK_INTERVAL_TICKS = 100;
    private static final int MAX_REPAIR_BLOCKS_PER_PASS = 12;
    private static final int RUIN_INTACT_PERCENT = 45;
    private static final Set<BuildingType> PRODUCTION_TYPES = Set.of(
            BuildingType.LUMBER_CAMP, BuildingType.FARM, BuildingType.QUARRY, BuildingType.MINE);

    private enum RepairMaterial { WOOD, STONE, METAL }
    private record RepairCandidate(BlockPos pos, BlockState expected, RepairMaterial material) {}

    private SettlementBuildingIntegrityService() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();
        boolean repairPass = tick % REPAIR_INTERVAL_TICKS == 0;
        boolean integrityPass = tick % CHECK_INTERVAL_TICKS == 0;
        if (!repairPass && !integrityPass) return;

        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return;
        ServerLevel level = server.overworld();

        // Repair comes first on the shared 100-tick boundary. A creeper-damaged house with real
        // settlement materials should be restored instead of being retired from one stale snapshot.
        if (repairPass) {
            int repaired = repairDamagedHouses(level, data);
            if (repaired > 0) {
                SettlementService.refreshResources(server, data);
                SettlementService.broadcast(server, data);
            }
        }
        if (!integrityPass) return;

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

    private static int repairDamagedHouses(ServerLevel level, SettlementData data) {
        // Repair is a real physical resource transaction. If any authoritative settlement storage
        // is unloaded, fail closed rather than repairing from a stale/partial ledger.
        if (!SettlementStorageService.storageAvailable(level, data)) return 0;
        SettlementResources available = SettlementStorageService.scan(level, data);
        long woodBudget = available.wood();
        long stoneBudget = available.stone();
        long metalBudget = available.metal();
        long woodCost = 0L;
        long stoneCost = 0L;
        long metalCost = 0L;
        List<RepairCandidate> selected = new ArrayList<>();

        outer:
        for (BuildingRecord building : List.copyOf(data.buildings())) {
            if (building.buildingType() != BuildingType.HOUSE
                    || !fullyLoaded(level, BuildingType.HOUSE, building)) continue;
            for (BuildingBlueprints.Placement placement : RotatedBlueprints.create(
                    BuildingType.HOUSE, building.origin(), building.rotation())) {
                if (selected.size() >= MAX_REPAIR_BLOCKS_PER_PASS) break outer;
                BlockPos pos = placement.pos();
                BlockState current = level.getBlockState(pos);
                if (current.is(placement.state().getBlock()) || !canRepairVacancy(level, pos, current)) continue;
                RepairMaterial material = repairMaterial(placement.state());
                switch (material) {
                    case WOOD -> {
                        if (woodCost >= woodBudget) continue;
                        woodCost++;
                    }
                    case STONE -> {
                        if (stoneCost >= stoneBudget) continue;
                        stoneCost++;
                    }
                    case METAL -> {
                        if (metalCost >= metalBudget) continue;
                        metalCost++;
                    }
                }
                selected.add(new RepairCandidate(pos.immutable(), placement.state(), material));
            }
        }
        if (selected.isEmpty()) return 0;

        if ((woodCost > 0L || stoneCost > 0L)
                && !SettlementStorageService.consume(level, data, woodCost, stoneCost, 0L)) return 0;
        if (metalCost > 0L && !SettlementStorageService.consumeMetal(level, data, metalCost)) return 0;

        int repaired = 0;
        for (RepairCandidate candidate : selected) {
            BlockState current = level.getBlockState(candidate.pos());
            if (!canRepairVacancy(level, candidate.pos(), current)) continue;
            if (level.setBlock(candidate.pos(), candidate.expected(), 3)) repaired++;
        }
        return repaired;
    }

    private static RepairMaterial repairMaterial(BlockState expected) {
        // Houses are overwhelmingly timber. Glass spends one stone unit as the existing mineral
        // abstraction; lanterns spend one canonical metal unit. Everything else in the house
        // blueprint (planks/logs/stairs/slabs/door/crafting table) spends one wood unit.
        if (expected.is(Blocks.LANTERN)) return RepairMaterial.METAL;
        if (expected.is(Blocks.GLASS)) return RepairMaterial.STONE;
        return RepairMaterial.WOOD;
    }

    private static boolean canRepairVacancy(ServerLevel level, BlockPos pos, BlockState current) {
        if (level.getBlockEntity(pos) != null || !current.getFluidState().isEmpty()) return false;
        return current.isAir() || current.canBeReplaced();
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
