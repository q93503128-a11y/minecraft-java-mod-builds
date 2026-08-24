package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Physical workshop maintenance for companion weapons.
 *
 * Players opt in by putting a damaged external weapon in the workshop's dedicated service barrel.
 * An assigned artisan walks to real town storage, carries one real metal stack item back to the
 * workshop, and consumes that item while repairing the queued weapon. Shared storage weapons are
 * never auto-selected or moved.
 */
public final class SettlementWorkshopService {
    public static final String WORKSHOP_WORKER_TAG = "frontier_settlement_workshop_worker";
    public static final String WORKSHOP_ASSIGNMENT_PREFIX = "frontier_settlement_workshop_";

    private static final String WORKER_NAME = "작업장 주민";
    private static final int SERVICE_PERIOD_TICKS = 100;
    private static final int REPAIR_PER_METAL = 64;
    private static final double INTERACTION_RANGE_SQR = 9.0D;
    private static final double ASSIGNMENT_SEARCH_RADIUS = 192.0D;

    private SettlementWorkshopService() {}

    public static String lockedReason(SettlementData data) {
        return data.buildingCount(BuildingType.BLACKSMITH) < 1
                ? "작업장은 대장간 1곳을 완성하면 열립니다."
                : null;
    }

    public static void tick(MinecraftServer server, SettlementData data) {
        if (server.getTickCount() % 10 != 0) return;
        ServerLevel level = server.overworld();
        for (BuildingRecord workshop : data.buildings()) {
            if (workshop.buildingType() != BuildingType.WORKSHOP) continue;
            BlockPos work = workshop.workCenter();
            BlockPos cratePos = WorkshopLayout.serviceCrate(workshop);
            if (!level.hasChunkAt(work) || !level.hasChunkAt(cratePos)) continue;
            if (!(level.getBlockEntity(cratePos) instanceof Container crate)) continue;

            Villager worker = findAssignedWorker(level, data, workshop);
            if (worker == null) continue;
            runService(server, level, data, workshop, cratePos, crate, worker);
        }
    }

    /** Population reconciliation/spawning is allowed only when all local workshop evidence is visible. */
    public static boolean allAssignmentsLoaded(ServerLevel level, SettlementData data) {
        if (!SettlementStorageService.storageAvailable(level, data)) return false;
        for (BuildingRecord workshop : data.buildings()) {
            if (workshop.buildingType() != BuildingType.WORKSHOP) continue;
            if (!level.hasChunkAt(workshop.workCenter()) || !level.hasChunkAt(WorkshopLayout.serviceCrate(workshop))) {
                return false;
            }
        }
        return true;
    }

    public static int loadedAssignedWorkerCount(ServerLevel level, SettlementData data) {
        Set<java.util.UUID> ids = new HashSet<>();
        for (BuildingRecord workshop : data.buildings()) {
            if (workshop.buildingType() != BuildingType.WORKSHOP) continue;
            Villager worker = findAssignedWorker(level, data, workshop);
            if (worker != null) ids.add(worker.getUUID());
        }
        return ids.size();
    }

    public static BuildingRecord firstMissingLoadedAssignment(ServerLevel level, SettlementData data) {
        if (!allAssignmentsLoaded(level, data)) return null;
        for (BuildingRecord workshop : data.buildings()) {
            if (workshop.buildingType() != BuildingType.WORKSHOP) continue;
            if (findAssignedWorker(level, data, workshop) == null) return workshop;
        }
        return null;
    }

    public static Villager spawnAssignedWorker(ServerLevel level, SettlementData data, BuildingRecord workshop) {
        if (workshop == null || workshop.buildingType() != BuildingType.WORKSHOP
                || !level.hasChunkAt(workshop.workCenter())
                || findAssignedWorker(level, data, workshop) != null) return null;
        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        BlockPos spawn = workshop.workCenter();
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(WORKER_NAME));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        worker.addTag(WORKSHOP_WORKER_TAG);
        worker.addTag(assignmentTag(workshop));
        if (!level.addFreshEntity(worker)) return null;
        return worker;
    }

    private static void runService(MinecraftServer server, ServerLevel level, SettlementData data,
                                   BuildingRecord workshop, BlockPos cratePos, Container crate, Villager worker) {
        int targetSlot = findDamagedExternalWeapon(crate);
        ItemStack carried = worker.getMainHandItem();

        if (targetSlot < 0) {
            if (!carried.isEmpty()) returnCarriedItem(level, data, worker, carried);
            else moveOrStop(worker, workshop.workCenter(), 0.68D);
            return;
        }

        if (!carried.isEmpty()) {
            if (!SettlementStorageService.isMetalStack(carried)) {
                returnCarriedItem(level, data, worker, carried);
                return;
            }
            if (worker.distanceToSqr(cratePos.getX() + 0.5D, cratePos.getY() + 0.5D, cratePos.getZ() + 0.5D)
                    > INTERACTION_RANGE_SQR) {
                worker.getNavigation().moveTo(cratePos.getX() + 0.5D, cratePos.getY(), cratePos.getZ() + 0.5D, 0.78D);
                return;
            }
            if (!workDue(server, workshop)) return;

            ItemStack weapon = crate.getItem(targetSlot);
            if (weapon.isEmpty() || !SettlementExternalContentService.isExternalWeapon(weapon)
                    || weapon.getDamageValue() <= 0) {
                returnCarriedItem(level, data, worker, carried);
                return;
            }
            weapon.setDamageValue(Math.max(0, weapon.getDamageValue() - REPAIR_PER_METAL));
            carried.shrink(1);
            if (carried.isEmpty()) worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            crate.setChanged();
            worker.swing(InteractionHand.MAIN_HAND);
            SettlementService.refreshResources(server, data);
            SettlementService.broadcast(server, data);
            return;
        }

        if (!SettlementStorageService.storageAvailable(level, data)) return;
        BlockPos source = SettlementStorageService.findExtractionTarget(level, data, SettlementStorageService::isMetalStack);
        if (source == null) return;
        if (worker.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > INTERACTION_RANGE_SQR) {
            worker.getNavigation().moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D, 0.82D);
            return;
        }

        ItemStack metal = SettlementStorageService.extract(level, source, SettlementStorageService::isMetalStack, 1);
        if (metal.isEmpty()) return;
        worker.setItemSlot(EquipmentSlot.MAINHAND, metal);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
    }

    private static int findDamagedExternalWeapon(Container crate) {
        int bestSlot = -1;
        int bestDamage = 0;
        for (int slot = 0; slot < crate.getContainerSize(); slot++) {
            ItemStack stack = crate.getItem(slot);
            if (stack.isEmpty() || !SettlementExternalContentService.isExternalWeapon(stack)) continue;
            int damage = stack.getDamageValue();
            if (damage > bestDamage) {
                bestDamage = damage;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private static boolean workDue(MinecraftServer server, BuildingRecord workshop) {
        int salt = Math.floorMod(workshop.originX() * 29 + workshop.originZ() * 19, SERVICE_PERIOD_TICKS);
        return Math.floorMod(server.getTickCount() + salt, SERVICE_PERIOD_TICKS) < 10;
    }

    private static void returnCarriedItem(ServerLevel level, SettlementData data, Villager worker, ItemStack carried) {
        BlockPos target = SettlementStorageService.findDepositTarget(level, data, carried);
        if (!level.hasChunkAt(target)) {
            worker.getNavigation().stop();
            return;
        }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                > INTERACTION_RANGE_SQR) {
            worker.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.8D);
            return;
        }
        if (!(level.getBlockEntity(target) instanceof Container container)) {
            worker.getNavigation().stop();
            return;
        }
        ItemStack remaining = SettlementInventory.insert(container, carried);
        worker.setItemSlot(EquipmentSlot.MAINHAND, remaining);
        if (remaining.isEmpty()) worker.getNavigation().stop();
    }

    private static Villager findAssignedWorker(ServerLevel level, SettlementData data, BuildingRecord workshop) {
        String assignment = assignmentTag(workshop);
        BlockPos center = data.centerPos();
        AABB area = new AABB(
                center.getX() - ASSIGNMENT_SEARCH_RADIUS, center.getY() - 96.0D, center.getZ() - ASSIGNMENT_SEARCH_RADIUS,
                center.getX() + ASSIGNMENT_SEARCH_RADIUS + 1.0D, center.getY() + 97.0D,
                center.getZ() + ASSIGNMENT_SEARCH_RADIUS + 1.0D);
        List<Villager> assigned = level.getEntitiesOfClass(Villager.class, area,
                villager -> villager.entityTags().contains(WORKSHOP_WORKER_TAG)
                        && villager.entityTags().contains(assignment));
        return assigned.isEmpty() ? null : assigned.getFirst();
    }

    private static String assignmentTag(BuildingRecord workshop) {
        return WORKSHOP_ASSIGNMENT_PREFIX + encode(workshop.originX()) + "_" + encode(workshop.originZ());
    }

    private static String encode(int value) {
        return value < 0 ? "n" + Math.abs((long) value) : "p" + value;
    }

    private static void moveOrStop(Villager worker, BlockPos target, double speed) {
        double distance = worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        if (distance > 4.0D) worker.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, speed);
        else worker.getNavigation().stop();
    }

    /** The service barrel is functional settlement infrastructure, so breaking it is blocked. */
    public static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (level != server.overworld()) return;
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return;
        BlockPos pos = event.getPos();
        if (!level.getBlockState(pos).is(Blocks.BARREL)) return;
        for (BuildingRecord workshop : data.buildings()) {
            if (workshop.buildingType() == BuildingType.WORKSHOP && pos.equals(WorkshopLayout.serviceCrate(workshop))) {
                event.setCanceled(true);
                event.setNotifyClient(true);
                return;
            }
        }
    }
}
