package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class SettlementResidentRoutineService {
    private static final Set<String> TOWN_WORKER_NAMES = Set.of(
            "벌목 주민", "농사 주민", "채석 주민", "광산 주민", "운송 주민");
    private static final int[] REST_X = {2, 6, 2, 6};
    private static final int[] REST_Z = {3, 3, 5, 5};

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

        List<BuildingRecord> houses = new ArrayList<>();
        for (BuildingRecord building : data.buildings()) {
            if (building.buildingType() == BuildingType.HOUSE) houses.add(building);
        }
        if (houses.isEmpty()) return;

        List<Villager> residents = townWorkers(level, data.centerPos());
        for (int i = 0; i < residents.size(); i++) {
            BuildingRecord house = houses.get((i / 4) % houses.size());
            int slot = i % 4;
            BlockPos rest = house.localToWorld(REST_X[slot], 1, REST_Z[slot]);
            Villager villager = residents.get(i);
            double distance = villager.distanceToSqr(rest.getX() + 0.5D, rest.getY(), rest.getZ() + 0.5D);
            if (distance > 4.0D) {
                villager.getNavigation().moveTo(rest.getX() + 0.5D, rest.getY(), rest.getZ() + 0.5D, 0.75D);
            } else {
                villager.getNavigation().stop();
            }
        }
    }

    private static List<Villager> townWorkers(ServerLevel level, BlockPos center) {
        AABB search = new AABB(
                center.getX() - 256.0D, center.getY() - 96.0D, center.getZ() - 256.0D,
                center.getX() + 257.0D, center.getY() + 97.0D, center.getZ() + 257.0D);
        List<Villager> result = level.getEntitiesOfClass(Villager.class, search, villager -> {
            if (villager.getCustomName() == null) return false;
            return TOWN_WORKER_NAMES.contains(villager.getCustomName().getString());
        });
        result.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return result;
    }
}
