package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

/**
 * High-tier settlement benefits without extra micromanagement screens.
 * Existing warehouses and guard posts gain stronger roles as the settlement grows.
 */
public final class SettlementTierInfrastructureService {
    private static final String TRANSPORT_WORKER_NAME = "운송 주민";
    private static final int LOGISTICS_INTERVAL_TICKS = 10;
    private static final int GARRISON_INTERVAL_TICKS = 200;

    private SettlementTierInfrastructureService() {}

    public static void tick(MinecraftServer server, SettlementData data) {
        SettlementTier tier = SettlementTier.current(data);
        if (tier.ordinal() < SettlementTier.FRONTIER_TOWN.ordinal()) return;

        ServerLevel level = server.overworld();
        int tick = server.getTickCount();
        if (!SettlementResidentRoutineService.isRestTime(level)
                && tick % LOGISTICS_INTERVAL_TICKS == 0
                && data.buildingCount(BuildingType.WAREHOUSE) > 0) {
            reinforcePhysicalLogistics(level, data, tier);
        }
        if (tick % GARRISON_INTERVAL_TICKS == 0
                && data.buildingCount(BuildingType.BLACKSMITH) > 0) {
            maintainTierGarrison(level, data, tier);
        }
    }

    private static void reinforcePhysicalLogistics(ServerLevel level, SettlementData data, SettlementTier tier) {
        int carryLimit = tier == SettlementTier.DOMAIN ? 48 : 32;
        double moveSpeed = tier == SettlementTier.DOMAIN ? 1.15D : 1.05D;

        List<Villager> transports = transportWorkers(level, data.centerPos());
        int count = Math.min(transports.size(), data.outposts().size());
        for (int i = 0; i < count; i++) {
            Villager worker = transports.get(i);
            OutpostRecord outpost = data.outposts().get(i);
            ItemStack carried = worker.getMainHandItem();
            if (carried.isEmpty()) continue;

            BlockPos outpostStock = outpost.stockpile();
            if (worker.distanceToSqr(outpostStock.getX() + 0.5D, outpostStock.getY() + 0.5D,
                    outpostStock.getZ() + 0.5D) <= 9.0D
                    && level.getBlockEntity(outpostStock) instanceof Container container) {
                topUpMatching(container, carried, carryLimit);
            }

            if (outpost.roadIndex() >= 0 && outpost.roadIndex() < data.roads().size()) {
                BlockPos roadStart = data.roads().get(outpost.roadIndex()).start().above();
                if (worker.distanceToSqr(roadStart.getX() + 0.5D, roadStart.getY(), roadStart.getZ() + 0.5D) > 16.0D) {
                    worker.getNavigation().moveTo(roadStart.getX() + 0.5D, roadStart.getY(), roadStart.getZ() + 0.5D, moveSpeed);
                    continue;
                }
            }

            BlockPos townStorage = SettlementStorageService.findDepositTarget(level, data, carried);
            if (worker.distanceToSqr(townStorage.getX() + 0.5D, townStorage.getY(), townStorage.getZ() + 0.5D) > 9.0D) {
                worker.getNavigation().moveTo(townStorage.getX() + 0.5D, townStorage.getY(), townStorage.getZ() + 0.5D, moveSpeed);
            }
        }
    }

    private static void topUpMatching(Container container, ItemStack carried, int carryLimit) {
        int hardLimit = Math.min(carryLimit, carried.getMaxStackSize());
        int needed = hardLimit - carried.getCount();
        if (needed <= 0) return;

        for (int slot = 0; slot < container.getContainerSize() && needed > 0; slot++) {
            ItemStack stock = container.getItem(slot);
            if (stock.isEmpty() || !ItemStack.isSameItemSameComponents(stock, carried)) continue;
            int take = Math.min(needed, stock.getCount());
            stock.shrink(take);
            carried.grow(take);
            needed -= take;
        }
        container.setChanged();
    }

    private static void maintainTierGarrison(ServerLevel level, SettlementData data, SettlementTier tier) {
        int reinforcementsPerPost = tier == SettlementTier.DOMAIN ? 2 : 1;
        for (BuildingRecord post : data.buildings()) {
            if (post.buildingType() != BuildingType.GUARD_POST) continue;
            for (int index = 1; index <= reinforcementsPerPost; index++) {
                maintainReinforcement(level, post, index);
            }
        }
    }

    private static void maintainReinforcement(ServerLevel level, BuildingRecord post, int index) {
        BlockPos center = post.workCenter();
        String identity = reinforcementIdentity(post, index);
        AABB search = new AABB(center).inflate(24.0D, 10.0D, 24.0D);
        List<IronGolem> existing = level.getEntitiesOfClass(IronGolem.class, search,
                guard -> guard.getCustomName() != null && identity.equals(guard.getCustomName().getString()));
        if (!existing.isEmpty()) return;

        IronGolem guard = new IronGolem(EntityTypes.IRON_GOLEM, level);
        guard.setPos(center.getX() + 0.5D + index, center.getY(), center.getZ() + 0.5D);
        guard.setCustomName(Component.literal(identity));
        guard.setCustomNameVisible(false);
        guard.setPersistenceRequired();
        guard.setPlayerCreated(true);
        level.addFreshEntity(guard);
    }

    private static String reinforcementIdentity(BuildingRecord post, int index) {
        return "개척 수비대 [" + post.originX() + "," + post.originZ() + "] #" + index;
    }

    private static List<Villager> transportWorkers(ServerLevel level, BlockPos center) {
        AABB search = new AABB(
                center.getX() - 256.0D, center.getY() - 96.0D, center.getZ() - 256.0D,
                center.getX() + 257.0D, center.getY() + 97.0D, center.getZ() + 257.0D);
        List<Villager> result = level.getEntitiesOfClass(Villager.class, search,
                villager -> villager.getCustomName() != null
                        && TRANSPORT_WORKER_NAME.equals(villager.getCustomName().getString()));
        result.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return result;
    }
}
