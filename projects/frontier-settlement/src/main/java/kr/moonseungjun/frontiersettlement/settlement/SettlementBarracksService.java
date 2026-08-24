package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.content.FrontierContent;
import kr.moonseungjun.frontiersettlement.content.FrontierSoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Barracks-owned supplied garrison. Military capacity is separate from civilian housing/population. */
public final class SettlementBarracksService {
    public static final String SOLDIER_TAG = "frontier_settlement_barracks_soldier";
    public static final String BARRACKS_ASSIGNMENT_PREFIX = "frontier_settlement_barracks_";
    public static final String SOLDIER_SLOT_PREFIX = "frontier_settlement_barracks_slot_";
    public static final int SOLDIERS_PER_BARRACKS = 3;
    public static final long RECRUIT_FOOD_COST = 8L;
    public static final long RECRUIT_METAL_COST = 2L;

    private static final String LEGACY_FREE_GARRISON_NAME_PREFIX = "개척 수비대 [";
    private static final int PATROL_INTERVAL_TICKS = 40;
    private static final int RECRUIT_INTERVAL_TICKS = 600;
    private static final int PATROL_RADIUS = 24;
    private static final double THREAT_RADIUS = 28.0D;
    private static final double SOLDIER_SEARCH_RADIUS = 176.0D;
    private static final double HOME_RADIUS_SQR = 12.0D * 12.0D;
    private static final double PATROL_LEASH_RADIUS_SQR = PATROL_RADIUS * PATROL_RADIUS;

    private SettlementBarracksService() {}

    public record Assignment(BuildingRecord barracks, int slot) {}

    public static String lockedReason(SettlementData data) {
        if (SettlementTier.current(data).ordinal() < SettlementTier.FRONTIER_TOWN.ordinal()) return "병영은 개척 도시 단계에 도달하면 열립니다.";
        if (data.buildingCount(BuildingType.WATCHTOWER) < 1) return "병영은 감시탑 1곳을 먼저 완성하면 열립니다.";
        if (data.buildingCount(BuildingType.BLACKSMITH) < 1) return "병영은 대장간 1곳을 먼저 완성하면 열립니다.";
        return null;
    }

    public static void tick(MinecraftServer server, SettlementData data) {
        ServerLevel level = server.overworld();
        int tick = server.getTickCount();
        if (tick % RECRUIT_INTERVAL_TICKS == 0) {
            cleanupLegacyFreeGarrison(level, data);
            Assignment missing = firstMissingLoadedAssignment(level, data);
            if (missing != null && tryRecruit(level, data, missing)) {
                SettlementService.refreshResources(server, data);
                SettlementService.broadcast(server, data);
            }
        }
        if (tick % PATROL_INTERVAL_TICKS != 0) return;
        for (BuildingRecord barracks : barracks(data)) {
            if (!patrolAreaLoaded(level, barracks)) continue;
            for (int slot = 0; slot < SOLDIERS_PER_BARRACKS; slot++) {
                FrontierSoldierEntity soldier = findSoldier(level, barracks, slot);
                if (soldier != null) patrol(level, data, barracks, slot, soldier);
            }
        }
    }

    public static int militaryCapacity(SettlementData data) {
        return data.buildingCount(BuildingType.BARRACKS) * SOLDIERS_PER_BARRACKS;
    }

    public static int loadedSoldierCount(ServerLevel level, SettlementData data) {
        Set<UUID> counted = new HashSet<>();
        for (BuildingRecord barracks : barracks(data)) {
            if (!patrolAreaLoaded(level, barracks)) continue;
            for (int slot = 0; slot < SOLDIERS_PER_BARRACKS; slot++) {
                FrontierSoldierEntity soldier = findSoldier(level, barracks, slot);
                if (soldier != null) counted.add(soldier.getUUID());
            }
        }
        return counted.size();
    }

    public static int loadedArmedSoldierCount(ServerLevel level, SettlementData data) {
        int count = 0;
        for (BuildingRecord barracks : barracks(data)) {
            if (!patrolAreaLoaded(level, barracks)) continue;
            for (int slot = 0; slot < SOLDIERS_PER_BARRACKS; slot++) {
                FrontierSoldierEntity soldier = findSoldier(level, barracks, slot);
                if (soldier != null && SettlementExternalContentService.isExternalWeapon(soldier.getMainHandItem())) count++;
            }
        }
        return count;
    }

    public static boolean militaryStateLoaded(ServerLevel level, SettlementData data) {
        for (BuildingRecord barracks : barracks(data)) if (!patrolAreaLoaded(level, barracks)) return false;
        return true;
    }

    public static Assignment firstMissingLoadedAssignment(ServerLevel level, SettlementData data) {
        for (BuildingRecord barracks : barracks(data)) {
            if (!patrolAreaLoaded(level, barracks)) continue;
            for (int slot = 0; slot < SOLDIERS_PER_BARRACKS; slot++) if (findSoldier(level, barracks, slot) == null) return new Assignment(barracks, slot);
        }
        return null;
    }

    /** Recruit one missing slot. Real shared food/metal are checked atomically; no abstract troop points. */
    public static boolean tryRecruit(ServerLevel level, SettlementData data, Assignment assignment) {
        if (assignment == null || !patrolAreaLoaded(level, assignment.barracks())) return false;
        if (findSoldier(level, assignment.barracks(), assignment.slot()) != null) return false;
        if (!SettlementStorageService.storageAvailable(level, data)) return false;
        SettlementResources resources = SettlementStorageService.scan(level, data);
        if (resources.food() < RECRUIT_FOOD_COST || resources.metal() < RECRUIT_METAL_COST) return false;
        BlockPos home = soldierHome(assignment.barracks(), assignment.slot());
        if (!level.hasChunkAt(home)) return false;
        FrontierSoldierEntity soldier = createSoldier(level, assignment.barracks(), assignment.slot(), home);
        if (!level.addFreshEntity(soldier)) return false;
        if (!SettlementStorageService.consumeMetalAndFood(level, data, RECRUIT_METAL_COST, RECRUIT_FOOD_COST)) {
            soldier.discard();
            return false;
        }
        return true;
    }

    /** Supplied soldiers never become an iron/body-drop farm; one physically assigned weapon is recoverable. */
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!event.getEntity().entityTags().contains(SOLDIER_TAG)) return;
        ItemStack weapon = event.getEntity().getMainHandItem();
        event.getDrops().clear();
        if (!SettlementExternalContentService.isExternalWeapon(weapon)) return;
        ItemStack recovered = weapon.copy();
        event.getDrops().add(new ItemEntity(
                event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), recovered));
    }

    private static void patrol(ServerLevel level, SettlementData data, BuildingRecord barracks, int slot, FrontierSoldierEntity soldier) {
        BlockPos home = soldierHome(barracks, slot);
        Monster threat = nearestThreat(level, barracks.workCenter());
        if (threat != null) { soldier.setTarget(threat); return; }
        if (soldier.getTarget() != null) soldier.setTarget(null);

        // Defense always wins. Only an idle, loaded garrison walks to real shared storage for one real weapon.
        if (SettlementMilitaryArmoryService.tickArmament(level, data, soldier)) return;

        double homeDistance = soldier.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
        if (homeDistance > PATROL_LEASH_RADIUS_SQR) {
            soldier.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 0.95D);
            return;
        }
        if (homeDistance > HOME_RADIUS_SQR) {
            soldier.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 0.9D);
        }
    }

    private static Monster nearestThreat(ServerLevel level, BlockPos center) {
        AABB area = new AABB(center).inflate(THREAT_RADIUS, 12.0D, THREAT_RADIUS);
        return level.getEntitiesOfClass(Monster.class, area, monster -> monster.isAlive() && !(monster instanceof Creeper)).stream()
                .min(Comparator.comparingDouble(monster -> monster.distanceToSqr(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D))).orElse(null);
    }

    private static FrontierSoldierEntity createSoldier(ServerLevel level, BuildingRecord barracks, int slot, BlockPos home) {
        FrontierSoldierEntity soldier = new FrontierSoldierEntity(FrontierContent.FRONTIER_SOLDIER.get(), level);
        soldier.setPos(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
        soldier.setCustomName(Component.literal("주둔 병사 #" + (slot + 1)));
        soldier.setCustomNameVisible(true);
        soldier.setPersistenceRequired();
        soldier.setPlayerCreated(true);
        soldier.addTag(SOLDIER_TAG);
        soldier.addTag(barracksAssignment(barracks));
        soldier.addTag(SOLDIER_SLOT_PREFIX + slot);
        return soldier;
    }

    private static FrontierSoldierEntity findSoldier(ServerLevel level, BuildingRecord barracks, int slot) {
        BlockPos center = barracks.workCenter();
        String assignment = barracksAssignment(barracks);
        String slotTag = SOLDIER_SLOT_PREFIX + slot;
        AABB search = new AABB(center).inflate(SOLDIER_SEARCH_RADIUS, 16.0D, SOLDIER_SEARCH_RADIUS);
        List<FrontierSoldierEntity> soldiers = level.getEntitiesOfClass(FrontierSoldierEntity.class, search,
                soldier -> soldier.entityTags().contains(SOLDIER_TAG) && soldier.entityTags().contains(assignment) && soldier.entityTags().contains(slotTag));
        if (!soldiers.isEmpty()) return soldiers.getFirst();

        // Save-compatible loaded migration. It changes only entity type/presentation and never recruits or spends again.
        List<IronGolem> legacy = level.getEntitiesOfClass(IronGolem.class, search,
                soldier -> !(soldier instanceof FrontierSoldierEntity)
                        && soldier.entityTags().contains(SOLDIER_TAG)
                        && soldier.entityTags().contains(assignment)
                        && soldier.entityTags().contains(slotTag));
        return legacy.isEmpty() ? null : migrateLegacySoldier(level, legacy.getFirst());
    }

    private static FrontierSoldierEntity migrateLegacySoldier(ServerLevel level, IronGolem legacy) {
        FrontierSoldierEntity replacement = new FrontierSoldierEntity(FrontierContent.FRONTIER_SOLDIER.get(), level);
        replacement.setPos(legacy.getX(), legacy.getY(), legacy.getZ());
        replacement.setYRot(legacy.getYRot());
        replacement.setXRot(legacy.getXRot());
        replacement.setCustomName(legacy.getCustomName());
        replacement.setCustomNameVisible(legacy.isCustomNameVisible());
        replacement.setPersistenceRequired();
        replacement.setPlayerCreated(true);
        for (String tag : legacy.entityTags()) replacement.addTag(tag);
        replacement.setHealth(Math.min(replacement.getMaxHealth(), legacy.getHealth()));
        if (!level.addFreshEntity(replacement)) return null;
        legacy.discard();
        return replacement;
    }

    /** Remove loaded pre-Alpha37 free reinforcement golems so old saves do not bypass barracks economics. */
    private static void cleanupLegacyFreeGarrison(ServerLevel level, SettlementData data) {
        for (BuildingRecord post : data.buildings()) {
            if (post.buildingType() != BuildingType.GUARD_POST) continue;
            BlockPos center = post.workCenter();
            if (!level.hasChunkAt(center)) continue;
            AABB search = new AABB(center).inflate(28.0D, 12.0D, 28.0D);
            List<IronGolem> legacy = level.getEntitiesOfClass(IronGolem.class, search,
                    guard -> guard.getCustomName() != null
                            && guard.getCustomName().getString().startsWith(LEGACY_FREE_GARRISON_NAME_PREFIX));
            for (IronGolem guard : legacy) guard.discard();
        }
    }

    private static boolean patrolAreaLoaded(ServerLevel level, BuildingRecord barracks) {
        BlockPos center = barracks.workCenter();
        if (!level.hasChunkAt(center)) return false;
        int[] offsets = {-PATROL_RADIUS, PATROL_RADIUS};
        for (int dx : offsets) for (int dz : offsets) if (!level.hasChunkAt(center.offset(dx, 0, dz))) return false;
        return true;
    }

    private static BlockPos soldierHome(BuildingRecord barracks, int slot) { return barracks.localToWorld(5 + slot * 2, 1, 8); }
    private static String barracksAssignment(BuildingRecord barracks) { return BARRACKS_ASSIGNMENT_PREFIX + barracks.originX() + "_" + barracks.originY() + "_" + barracks.originZ(); }
    private static List<BuildingRecord> barracks(SettlementData data) { return data.buildings().stream().filter(building -> building.buildingType() == BuildingType.BARRACKS).toList(); }
}
