package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.content.FrontierSoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Automated loaded-only barracks armament using one real external weapon ItemStack per soldier.
 * Soldiers walk to a concrete shared-storage container themselves; no weapon is minted, teleported,
 * force-loaded or represented by a virtual armory balance.
 */
public final class SettlementMilitaryArmoryService {
    public static final double STORAGE_INTERACTION_RANGE_SQR = 9.0D;
    public static final double MAX_ARMORY_ROUTE_SQR = 160.0D * 160.0D;
    public static final double ARMORY_WALK_SPEED = 0.95D;

    private SettlementMilitaryArmoryService() {}

    /**
     * @return true while this soldier is actively handling an armament trip this tick.
     */
    public static boolean tickArmament(ServerLevel level, SettlementData data, BlockPos routeAnchor, FrontierSoldierEntity soldier) {
        if (soldier == null || !soldier.isAlive()) return false;
        ItemStack carried = soldier.getMainHandItem();
        if (SettlementExternalContentService.isExternalWeapon(carried)) return false;
        if (!carried.isEmpty()) return false;
        if (!SettlementStorageService.storageAvailable(level, data)) return false;

        BlockPos source = nearestWeaponSource(level, data, routeAnchor, soldier);
        if (source == null) return false;
        double distance = soldier.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D);
        if (distance > STORAGE_INTERACTION_RANGE_SQR) {
            return SettlementWorkerStorageNavigation.moveToInteraction(
                    level, soldier, source, ARMORY_WALK_SPEED, STORAGE_INTERACTION_RANGE_SQR);
        }

        ItemStack extracted = SettlementStorageService.extract(
                level, source, SettlementExternalContentService::isExternalWeapon, 1);
        if (extracted.isEmpty()) return false;
        soldier.setItemSlot(EquipmentSlot.MAINHAND, extracted);
        soldier.getNavigation().stop();
        return true;
    }

    /**
     * Local final leg for an already road-delivered outpost weapon. The sentry never reads town
     * storage directly: the existing assigned transporter must first place the exact ItemStack in
     * this outpost's physical stockpile.
     */
    public static boolean tickOutpostArmament(ServerLevel level, OutpostRecord outpost,
                                              FrontierSoldierEntity soldier) {
        if (soldier == null || !soldier.isAlive()) return false;
        ItemStack carried = soldier.getMainHandItem();
        if (SettlementExternalContentService.isExternalWeapon(carried)) return false;
        if (!carried.isEmpty()) return false;

        BlockPos source = outpost.stockpile();
        if (!level.hasChunkAt(source)) return false;
        if (!(level.getBlockEntity(source) instanceof Container container) || !containsExternalWeapon(container)) {
            return false;
        }
        if (!SettlementWorkerStorageNavigation.canReachInteraction(
                level, soldier, source, STORAGE_INTERACTION_RANGE_SQR)) {
            soldier.getNavigation().stop();
            return false;
        }
        double distance = soldier.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D);
        if (distance > STORAGE_INTERACTION_RANGE_SQR) {
            return SettlementWorkerStorageNavigation.moveToInteraction(
                    level, soldier, source, ARMORY_WALK_SPEED, STORAGE_INTERACTION_RANGE_SQR);
        }

        ItemStack extracted = SettlementStorageService.extract(
                level, source, SettlementExternalContentService::isExternalWeapon, 1);
        if (extracted.isEmpty()) return false;
        soldier.setItemSlot(EquipmentSlot.MAINHAND, extracted);
        soldier.getNavigation().stop();
        return true;
    }

    private static BlockPos nearestWeaponSource(ServerLevel level, SettlementData data, BlockPos routeAnchor, FrontierSoldierEntity soldier) {
        BlockPos best = null;
        double bestDistance = MAX_ARMORY_ROUTE_SQR + 1.0D;
        for (BlockPos pos : SettlementStorageService.storagePositions(data)) {
            if (pos.distSqr(routeAnchor) > MAX_ARMORY_ROUTE_SQR || !level.hasChunkAt(pos)) continue;
            if (!(level.getBlockEntity(pos) instanceof Container container) || !containsExternalWeapon(container)) continue;
            if (!SettlementWorkerStorageNavigation.canReachInteraction(
                    level, soldier, pos, STORAGE_INTERACTION_RANGE_SQR)) continue;
            double distance = soldier.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            if (distance <= MAX_ARMORY_ROUTE_SQR && distance < bestDistance) {
                bestDistance = distance;
                best = pos;
            }
        }
        return best;
    }

    private static boolean containsExternalWeapon(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (SettlementExternalContentService.isExternalWeapon(container.getItem(slot))) return true;
        }
        return false;
    }
}
