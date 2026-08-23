package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Barracks-owned regular garrison. Recruitment consumes real settlement food and metal and each
 * soldier is persistently assigned to one barracks slot. This replaces the old free tier-garrison
 * reinforcement path; guard posts/watchtowers keep their own small baseline defense roles.
 */
public final class SettlementBarracksService {
    public static final String SOLDIER_TAG = "frontier_settlement_barracks_soldier";
    public static final String BARRACKS_ASSIGNMENT_PREFIX = "frontier_settlement_barracks_";
    public static final String SOLDIER_SLOT_PREFIX = "frontier_settlement_barracks_slot_";
    public static final int SOLDIERS_PER_BARRACKS = 3;
    public static final long RECRUIT_FOOD_COST = 8L;
    public static final long RECRUIT_METAL_COST = 2L;

    private static final int PATROL_INTERVAL_TICKS = 40;
    private static final int PATROL_RADIUS = 24;
    private static final double THREAT_RADIUS = 28.0D;
    private static final double SOLDIER_SEARCH_RADIUS = 36.0D;
    private static final double HOME_RADIUS_SQR = 12.0D * 12.0D;

    private SettlementBarracksService() {}

    public record Assignment(BuildingRecord barracks, int slot) {}

    public static String lockedReason(SettlementData data) {
        if (SettlementTier.current(data).ordinal() < SettlementTier.FRONTIER_TOWN.ordinal()) {
            return "병영은 개척 도시 단계에 도달하면 열립니다.";
        }
        if (data.buildingCount(BuildingType.WATCHTOWER) < 1) {
            return "병영은 감시탑 1곳을 먼저 완성하면 열립니다.";
        }
        if (data.buildingCount(BuildingType.BLACKSMITH) < 1) {
            return "병영은 대장간 1곳을 먼저 완성하면 열립니다.";
        }
        return null;
    }

    public static void tick(MinecraftServer server, SettlementData data) {
        if (server.getTickCount() % PATROL_INTERVAL_TICKS != 0) return;
        ServerLevel level = server.overworld();
        for (BuildingRecord barracks : barracks(data)) {
            if (!patrolAreaLoaded(level, barracks)) continue;
            for (int slot = 0; slot < SOLDIERS_PER_BARRACKS; slot++) {
                IronGolem soldier = findSoldier(level, barracks, slot);
                if (soldier != null) patrol(level, barracks, slot, soldier);
            }
        }
    }

    public static boolean allAssignmentsLoaded(ServerLevel level, SettlementData data) {
        for (BuildingRecord barracks : barracks(data)) {
            if (!patrolAreaLoaded(level, barracks)) return false;
        }
        return true;
    }

    public static int loadedAssignedSoldierCount(ServerLevel level, SettlementData data) {
        Set<UUID> counted = new HashSet<>();
        for (BuildingRecord barracks : barracks(data)) {
            if (!patrolAreaLoaded(level, barracks)) continue;
            for (int slot = 0; slot < SOLDIERS_PER_BARRACKS; slot++) {
                IronGolem soldier = findSoldier(level, barracks, slot);
                if (soldier != null) counted.add(soldier.getUUID());
            }
        }
        return counted.size();
    }

    public static Assignment firstMissingLoadedAssignment(ServerLevel level, SettlementData data) {
        for (BuildingRecord barracks : barracks(data)) {
            if (!patrolAreaLoaded(level, barracks)) continue;
            for (int slot = 0; slot < SOLDIERS_PER_BARRACKS; slot++) {
                if (findSoldier(level, barracks, slot) == null) return new Assignment(barracks, slot);
            }
        }
        return null;
    }

    public static int missingLoadedAssignmentCount(ServerLevel level, SettlementData data) {
        int missing = 0;
        for (BuildingRecord barracks : barracks(data)) {
            if (!patrolAreaLoaded(level, barracks)) continue;
            for (int slot = 0; slot < SOLDIERS_PER_BARRACKS; slot++) {
                if (findSoldier(level, barracks, slot) == null) missing++;
            }
        }
        return missing;
    }

    /** Recruit exactly one missing slot, charging only after an entity can actually be spawned. */
    public static boolean tryRecruit(ServerLevel level, SettlementData data, Assignment assignment) {
        if (assignment == null || !patrolAreaLoaded(level, assignment.barracks())) return false;
        if (findSoldier(level, assignment.barracks(), assignment.slot()) != null) return false;
        if (!SettlementStorageService.storageAvailable(level, data)) return false;
        SettlementResources resources = SettlementStorageService.scan(level, data);
        if (resources.food() < RECRUIT_FOOD_COST || resources.metal() < RECRUIT_METAL_COST) return false;

        BlockPos home = soldierHome(assignment.barracks(), assignment.slot());
        if (!level.hasChunkAt(home)) return false;
        IronGolem soldier = createSoldier(level, assignment.barracks(), assignment.slot(), home);
        if (!level.addFreshEntity(soldier)) return false;
        if (!SettlementStorageService.consumeMetalAndFood(level, data, RECRUIT_METAL_COST, RECRUIT_FOOD_COST)) {
            soldier.discard();
            return false;
        }
        return true;
    }

    public static void onLivingDrops(LivingDropsEvent event) {
        if (!event.getEntity().entityTags().contains(SOLDIER_TAG)) return;
        event.getDrops().clear();
    }

    private static void patrol(ServerLevel level, BuildingRecord barracks, int slot, IronGolem soldier) {
        BlockPos home = soldierHome(barracks, slot);
        Monster threat = nearestThreat(level, barracks.workCenter());
        if (threat != null) {
            soldier.setTarget(threat);
            return;
        }
        if (soldier.getTarget() != null) soldier.setTarget(null);
        if (soldier.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D) > HOME_RADIUS_SQR) {
            soldier.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 0.9D);
        }
    }

    private static Monster nearestThreat(ServerLevel level, BlockPos center) {
        AABB area = new AABB(center).inflate(THREAT_RADIUS, 12.0D, THREAT_RADIUS);
        return level.getEntitiesOfClass(Monster.class, area,
                        monster -> monster.isAlive() && !(monster instanceof Creeper))
                .stream()
                .min(Comparator.comparingDouble(monster -> monster.distanceToSqr(
                        center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D)))
                .orElse(null);
    }

    private static IronGolem createSoldier(ServerLevel level, BuildingRecord barracks, int slot, BlockPos home) {
        IronGolem soldier = new IronGolem(EntityTypes.IRON_GOLEM, level);
        soldier.setPos(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
        soldier.setCustomName(Component.literal("주둔 병사 #" + (slot + 1)));
        soldier.setCustomNameVisible(true);
        soldier.setPersistenceRequired();
        soldier.setPlayerCreated(true);
        soldier.addTag(SOLDIER_TAG);
        soldier.addTag(barracksAssignment(barracks));
        soldier.addTag(slotTag(slot));
        return soldier;
    }

    private static IronGolem findSoldier(ServerLevel level, BuildingRecord barracks, int slot) {
        BlockPos center = barracks.workCenter();
        String assignment = barracksAssignment(barracks);
        String slotTag = slotTag(slot);
        AABB search = new AABB(center).inflate(SOLDIER_SEARCH_RADIUS, 16.0D, SOLDIER_SEARCH_RADIUS);
        List<IronGolem> soldiers = level.getEntitiesOfClass(IronGolem.class, search,
                soldier -> soldier.entityTags().contains(SOLDIER_TAG)
                        && soldier.entityTags().contains(assignment)
                        && soldier.entityTags().contains(slotTag));
        return soldiers.isEmpty() ? null : soldiers.getFirst();
    }

    private static boolean patrolAreaLoaded(ServerLevel level, BuildingRecord barracks) {
        BlockPos center = barracks.workCenter();
        if (!level.hasChunkAt(center)) return false;
        int[] offsets = {-PATROL_RADIUS, PATROL_RADIUS};
        for (int dx : offsets) {
            for (int dz : offsets) {
                if (!level.hasChunkAt(center.offset(dx, 0, dz))) return false;
            }
        }
        return true;
    }

    private static BlockPos soldierHome(BuildingRecord barracks, int slot) {
        int x = 5 + slot * 2;
        return barracks.localToWorld(x, 1, 8);
    }

    private static String barracksAssignment(BuildingRecord barracks) {
        return BARRACKS_ASSIGNMENT_PREFIX + barracks.originX() + "_" + barracks.originY() + "_" + barracks.originZ();
    }

    private static String slotTag(int slot) {
        return SOLDIER_SLOT_PREFIX + slot;
    }

    private static List<BuildingRecord> barracks(SettlementData data) {
        return data.buildings().stream().filter(building -> building.buildingType() == BuildingType.BARRACKS).toList();
    }
}
