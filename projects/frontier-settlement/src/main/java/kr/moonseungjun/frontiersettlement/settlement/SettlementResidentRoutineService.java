package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import kr.moonseungjun.frontiersettlement.content.FrontierWorkerEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class SettlementResidentRoutineService {
    private static final Set<String> TOWN_WORKER_NAMES = Set.of(
            "벌목 주민", "농사 주민", "채석 주민", "광산 주민", "작업장 주민");
    private static final int[] REST_X = {2, 6, 2, 6};
    private static final int[] REST_Z = {3, 3, 5, 5};
    private static final double OUTPOST_REST_RADIUS_SQR = 32.0D * 32.0D;
    private static final double TOWN_REST_RADIUS_SQR = 96.0D * 96.0D;

    private SettlementResidentRoutineService() {}

    public static boolean isRestTime(ServerLevel level) {
        var defaultClock = level.dimensionType().defaultClock();
        if (defaultClock.isEmpty()) return false;
        long time = Math.floorMod(level.clockManager().getTotalTicks(defaultClock.get()), 24000L);
        return time >= 13000L && time < 23000L;
    }

    public static void tick(MinecraftServer server, SettlementData data) {
        ServerLevel level = server.overworld();
        if (!isRestTime(level) || server.getTickCount() % 10 != 0) return;

        List<BuildingRecord> houses = houses(data);
        restTownWorkers(level, data, houses);
        restTransportWorkers(level, data, houses);
        restOutpostProductionWorkers(level, data);
    }

    private static List<BuildingRecord> houses(SettlementData data) {
        List<BuildingRecord> houses = new ArrayList<>();
        for (BuildingRecord building : data.buildings()) {
            if (building.buildingType() == BuildingType.HOUSE) houses.add(building);
        }
        return houses;
    }

    private static void restTownWorkers(ServerLevel level, SettlementData data, List<BuildingRecord> houses) {
        if (houses.isEmpty()) return;
        List<FrontierWorkerEntity> residents = townWorkers(level, data.centerPos());
        for (int i = 0; i < residents.size(); i++) {
            moveToHouseSlot(residents.get(i), houses, i);
        }
    }

    private static void restTransportWorkers(ServerLevel level, SettlementData data, List<BuildingRecord> houses) {
        AABB bounds = settlementBounds(data);
        List<FrontierWorkerEntity> transports = level.getEntitiesOfClass(FrontierWorkerEntity.class, bounds,
                villager -> villager.entityTags().contains(SettlementOutpostLogisticsService.TRANSPORT_WORKER_TAG));
        for (FrontierWorkerEntity villager : transports) {
            OutpostRecord assigned = assignedTransportOutpost(data, villager);
            if (assigned != null && level.hasChunkAt(assigned.center())
                    && villager.distanceToSqr(assigned.center().getX() + 0.5D, assigned.center().getY(),
                    assigned.center().getZ() + 0.5D) <= OUTPOST_REST_RADIUS_SQR) {
                moveOrStop(villager, assigned.center().above(), 0.72D);
                continue;
            }
            if (!houses.isEmpty() && villager.distanceToSqr(data.centerPos().getX() + 0.5D,
                    data.centerPos().getY(), data.centerPos().getZ() + 0.5D) <= TOWN_REST_RADIUS_SQR) {
                int slot = Math.floorMod(villager.getUUID().hashCode(), houses.size() * 4);
                moveToHouseSlot(villager, houses, slot);
                continue;
            }
            // A remote hauler already between loaded rest anchors sleeps where it is instead of
            // receiving a cross-territory navigation order that would undermine chunk-safe logistics.
            villager.getNavigation().stop();
        }
    }

    private static void restOutpostProductionWorkers(ServerLevel level, SettlementData data) {
        for (OutpostRecord outpost : data.outposts()) {
            if (!level.hasChunkAt(outpost.center())) continue;
            String assignment = SettlementOutpostProductionService.PRODUCTION_OUTPOST_TAG_PREFIX + outpost.id();
            AABB search = new AABB(outpost.center()).inflate(48.0D, 24.0D, 48.0D);
            List<FrontierWorkerEntity> workers = level.getEntitiesOfClass(FrontierWorkerEntity.class, search,
                    villager -> villager.entityTags().contains(SettlementOutpostProductionService.PRODUCTION_WORKER_TAG)
                            && villager.entityTags().contains(assignment));
            for (FrontierWorkerEntity worker : workers) moveOrStop(worker, outpost.center().above(), 0.68D);
        }
    }

    private static OutpostRecord assignedTransportOutpost(SettlementData data, FrontierWorkerEntity villager) {
        for (OutpostRecord outpost : data.outposts()) {
            if (villager.entityTags().contains(
                    SettlementOutpostLogisticsService.TRANSPORT_OUTPOST_TAG_PREFIX + outpost.id())) {
                return outpost;
            }
        }
        return null;
    }

    private static void moveToHouseSlot(FrontierWorkerEntity villager, List<BuildingRecord> houses, int residentIndex) {
        BuildingRecord house = houses.get((residentIndex / 4) % houses.size());
        int slot = residentIndex % 4;
        BlockPos rest = house.localToWorld(REST_X[slot], 1, REST_Z[slot]);
        moveOrStop(villager, rest, 0.75D);
    }

    private static void moveOrStop(FrontierWorkerEntity villager, BlockPos target, double speed) {
        double distance = villager.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        if (distance > 4.0D) {
            villager.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, speed);
        } else {
            villager.getNavigation().stop();
        }
    }

    private static List<FrontierWorkerEntity> townWorkers(ServerLevel level, BlockPos center) {
        AABB search = new AABB(
                center.getX() - 256.0D, center.getY() - 96.0D, center.getZ() - 256.0D,
                center.getX() + 257.0D, center.getY() + 97.0D, center.getZ() + 257.0D);
        List<FrontierWorkerEntity> result = level.getEntitiesOfClass(FrontierWorkerEntity.class, search, villager -> {
            if (villager.getCustomName() == null) return false;
            return TOWN_WORKER_NAMES.contains(villager.getCustomName().getString());
        });
        result.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return result;
    }

    private static AABB settlementBounds(SettlementData data) {
        BlockPos center = data.centerPos();
        double minX = center.getX();
        double minY = center.getY();
        double minZ = center.getZ();
        double maxX = center.getX();
        double maxY = center.getY();
        double maxZ = center.getZ();
        for (RoadSegment road : data.roads()) {
            for (BlockPos pos : road.centers()) {
                minX = Math.min(minX, pos.getX());
                minY = Math.min(minY, pos.getY());
                minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX());
                maxY = Math.max(maxY, pos.getY());
                maxZ = Math.max(maxZ, pos.getZ());
            }
        }
        for (OutpostRecord outpost : data.outposts()) {
            minX = Math.min(minX, outpost.centerX());
            minY = Math.min(minY, outpost.centerY());
            minZ = Math.min(minZ, outpost.centerZ());
            maxX = Math.max(maxX, outpost.centerX());
            maxY = Math.max(maxY, outpost.centerY());
            maxZ = Math.max(maxZ, outpost.centerZ());
        }
        return new AABB(minX - 32.0D, minY - 48.0D, minZ - 32.0D,
                maxX + 33.0D, maxY + 49.0D, maxZ + 33.0D);
    }
}
