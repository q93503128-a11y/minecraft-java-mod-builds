package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.compat.ExternalContentTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Late-game physical forging bridge for companion weapons.
 *
 * Player intent is explicit: an unenchanted recognized external weapon and one expedition relic must
 * be placed in this building's commission barrel. The assigned artisan physically fetches four real
 * metal items from ordinary settlement storage. Only after a valid enchant result can be produced are
 * the relic and four metal consumed; the same weapon remains in the barrel, fully repaired and forged.
 */
public final class SettlementAdvancedWorkshopService {
    public static final String ADVANCED_WORKER_TAG = "frontier_settlement_advanced_workshop_worker";
    public static final String ADVANCED_ASSIGNMENT_PREFIX = "frontier_settlement_advanced_workshop_";
    public static final int RELIC_COST = 1;
    public static final int METAL_COST = 4;
    public static final int ENCHANTMENT_POWER = 30;

    private static final String WORKER_NAME = "고급 제작 주민";
    private static final int SERVICE_PERIOD_TICKS = 160;
    private static final double INTERACTION_RANGE_SQR = 9.0D;
    private static final double ASSIGNMENT_SEARCH_RADIUS = 192.0D;

    private SettlementAdvancedWorkshopService() {}

    public static String lockedReason(SettlementData data) {
        if (SettlementTier.current(data).ordinal() < SettlementTier.FRONTIER_TOWN.ordinal()) {
            return "고급 제작소는 개척 도시 단계에 도달하면 열립니다.";
        }
        if (data.buildingCount(BuildingType.WORKSHOP) < 1) {
            return "고급 제작소는 작업장 1곳을 먼저 완성하면 열립니다.";
        }
        if (data.buildingCount(BuildingType.MARKET) < 1) {
            return "고급 제작소는 시장 1곳을 먼저 완성하면 열립니다.";
        }
        return null;
    }

    public static void tick(MinecraftServer server, SettlementData data) {
        if (server.getTickCount() % 10 != 0) return;
        ServerLevel level = server.overworld();
        boolean rest = SettlementResidentRoutineService.isRestTime(level);
        for (BuildingRecord workshop : data.buildings()) {
            if (workshop.buildingType() != BuildingType.ADVANCED_WORKSHOP) continue;
            BlockPos cratePos = AdvancedWorkshopLayout.commissionCrate(workshop);
            BlockPos home = AdvancedWorkshopLayout.artisanHome(workshop);
            if (!level.hasChunkAt(workshop.workCenter()) || !level.hasChunkAt(cratePos) || !level.hasChunkAt(home)) continue;
            if (!(level.getBlockEntity(cratePos) instanceof Container crate)) continue;

            Villager worker = findAssignedWorker(level, data, workshop);
            if (worker == null) continue;
            ItemStack carried = worker.getMainHandItem();
            if (rest) {
                if (!carried.isEmpty()) returnCarriedItem(level, data, worker, carried);
                else moveOrStop(worker, home, 0.68D);
                continue;
            }
            runService(server, level, data, workshop, cratePos, crate, worker);
        }
    }

    public static boolean allAssignmentsLoaded(ServerLevel level, SettlementData data) {
        if (!SettlementStorageService.storageAvailable(level, data)) return false;
        for (BuildingRecord workshop : data.buildings()) {
            if (workshop.buildingType() != BuildingType.ADVANCED_WORKSHOP) continue;
            if (!level.hasChunkAt(workshop.workCenter())
                    || !level.hasChunkAt(AdvancedWorkshopLayout.commissionCrate(workshop))) return false;
        }
        return true;
    }

    public static int loadedAssignedWorkerCount(ServerLevel level, SettlementData data) {
        Set<java.util.UUID> ids = new HashSet<>();
        for (BuildingRecord workshop : data.buildings()) {
            if (workshop.buildingType() != BuildingType.ADVANCED_WORKSHOP) continue;
            Villager worker = findAssignedWorker(level, data, workshop);
            if (worker != null) ids.add(worker.getUUID());
        }
        return ids.size();
    }

    public static BuildingRecord firstMissingLoadedAssignment(ServerLevel level, SettlementData data) {
        if (!allAssignmentsLoaded(level, data)) return null;
        for (BuildingRecord workshop : data.buildings()) {
            if (workshop.buildingType() != BuildingType.ADVANCED_WORKSHOP) continue;
            if (findAssignedWorker(level, data, workshop) == null) return workshop;
        }
        return null;
    }

    public static void spawnAssignedWorker(ServerLevel level, BuildingRecord workshop) {
        if (workshop == null || workshop.buildingType() != BuildingType.ADVANCED_WORKSHOP
                || !level.hasChunkAt(workshop.workCenter())) return;
        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        BlockPos spawn = AdvancedWorkshopLayout.artisanHome(workshop);
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(WORKER_NAME));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        worker.addTag(ADVANCED_WORKER_TAG);
        worker.addTag(assignmentTag(workshop));
        level.addFreshEntity(worker);
    }

    public static int readyCommissionCount(ServerLevel level, SettlementData data) {
        int ready = 0;
        for (BuildingRecord workshop : data.buildings()) {
            if (workshop.buildingType() != BuildingType.ADVANCED_WORKSHOP) continue;
            BlockPos cratePos = AdvancedWorkshopLayout.commissionCrate(workshop);
            if (!(level.getBlockEntity(cratePos) instanceof Container crate)) continue;
            if (findCommissionWeapon(crate) >= 0 && findRelicSlot(crate) >= 0) ready++;
        }
        return ready;
    }

    private static void runService(MinecraftServer server, ServerLevel level, SettlementData data,
                                   BuildingRecord workshop, BlockPos cratePos, Container crate, Villager worker) {
        int weaponSlot = findCommissionWeapon(crate);
        int relicSlot = findRelicSlot(crate);
        ItemStack carried = worker.getMainHandItem();
        if (weaponSlot < 0 || relicSlot < 0) {
            if (!carried.isEmpty()) returnCarriedItem(level, data, worker, carried);
            else moveOrStop(worker, AdvancedWorkshopLayout.artisanHome(workshop), 0.68D);
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
            ItemStack remaining = SettlementInventory.insert(crate, carried);
            worker.setItemSlot(EquipmentSlot.MAINHAND, remaining);
            crate.setChanged();
            SettlementService.refreshResources(server, data);
            SettlementService.broadcast(server, data);
            return;
        }

        int metal = countMatching(crate, SettlementStorageService::isMetalStack);
        if (metal >= METAL_COST) {
            if (!workDue(server, workshop)) return;
            if (forgeOne(level, crate, weaponSlot, relicSlot)) {
                worker.swing(InteractionHand.MAIN_HAND);
                SettlementService.refreshResources(server, data);
                SettlementService.broadcast(server, data);
            }
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
        ItemStack metalStack = SettlementStorageService.extract(level, source, SettlementStorageService::isMetalStack,
                Math.min(METAL_COST - metal, 4));
        if (metalStack.isEmpty()) return;
        worker.setItemSlot(EquipmentSlot.MAINHAND, metalStack);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
    }

    private static boolean forgeOne(ServerLevel level, Container crate, int weaponSlot, int relicSlot) {
        ItemStack weapon = crate.getItem(weaponSlot);
        ItemStack relic = crate.getItem(relicSlot);
        if (!isForgeableWeapon(weapon) || relic.isEmpty() || !relic.is(ExternalContentTags.EXPEDITION_RELICS)) return false;
        if (countMatching(crate, SettlementStorageService::isMetalStack) < METAL_COST) return false;

        ItemStack forged = weapon.copy();
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemStack enchanted = EnchantmentHelper.enchantItem(level.getRandom(), forged, ENCHANTMENT_POWER,
                enchantments.listElements()
                        .<Holder<Enchantment>>map(holder -> holder)
                        .filter(holder -> holder.is(EnchantmentTags.IN_ENCHANTING_TABLE))
                        .filter(forged::supportsEnchantment));
        if (EnchantmentHelper.getEnchantmentsForCrafting(enchanted).isEmpty()) return false;

        enchanted.setDamageValue(0);
        if (!consumeMatching(crate, SettlementStorageService::isMetalStack, METAL_COST)) return false;
        relic.shrink(RELIC_COST);
        if (relic.isEmpty()) crate.setItem(relicSlot, ItemStack.EMPTY);
        crate.setItem(weaponSlot, enchanted);
        crate.setChanged();
        return true;
    }

    private static int findCommissionWeapon(Container crate) {
        for (int slot = 0; slot < crate.getContainerSize(); slot++) {
            if (isForgeableWeapon(crate.getItem(slot))) return slot;
        }
        return -1;
    }

    private static boolean isForgeableWeapon(ItemStack stack) {
        return SettlementExternalContentService.isExternalWeapon(stack)
                && EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty();
    }

    private static int findRelicSlot(Container crate) {
        for (int slot = 0; slot < crate.getContainerSize(); slot++) {
            ItemStack stack = crate.getItem(slot);
            if (!stack.isEmpty() && stack.is(ExternalContentTags.EXPEDITION_RELICS)) return slot;
        }
        return -1;
    }

    private static int countMatching(Container container, Predicate<ItemStack> predicate) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) total += stack.getCount();
        }
        return total;
    }

    private static boolean consumeMatching(Container container, Predicate<ItemStack> predicate, int amount) {
        if (countMatching(container, predicate) < amount) return false;
        int left = amount;
        for (int slot = 0; slot < container.getContainerSize() && left > 0; slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty() || !predicate.test(stack)) continue;
            int take = Math.min(left, stack.getCount());
            stack.shrink(take);
            if (stack.isEmpty()) container.setItem(slot, ItemStack.EMPTY);
            left -= take;
        }
        container.setChanged();
        return left == 0;
    }

    private static boolean workDue(MinecraftServer server, BuildingRecord workshop) {
        int salt = Math.floorMod(workshop.originX() * 37 + workshop.originZ() * 23, SERVICE_PERIOD_TICKS);
        return Math.floorMod(server.getTickCount() + salt, SERVICE_PERIOD_TICKS) < 10;
    }

    private static void returnCarriedItem(ServerLevel level, SettlementData data, Villager worker, ItemStack carried) {
        BlockPos target = SettlementStorageService.findDepositTarget(level, data, carried);
        if (!level.hasChunkAt(target)) { worker.getNavigation().stop(); return; }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D)
                > INTERACTION_RANGE_SQR) {
            worker.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.8D);
            return;
        }
        if (!(level.getBlockEntity(target) instanceof Container container)) { worker.getNavigation().stop(); return; }
        ItemStack remaining = SettlementInventory.insert(container, carried);
        worker.setItemSlot(EquipmentSlot.MAINHAND, remaining);
        if (remaining.isEmpty()) worker.getNavigation().stop();
    }

    private static Villager findAssignedWorker(ServerLevel level, SettlementData data, BuildingRecord workshop) {
        String assignment = assignmentTag(workshop);
        BlockPos center = data.centerPos();
        AABB area = new AABB(center.getX() - ASSIGNMENT_SEARCH_RADIUS, center.getY() - 96.0D,
                center.getZ() - ASSIGNMENT_SEARCH_RADIUS, center.getX() + ASSIGNMENT_SEARCH_RADIUS + 1.0D,
                center.getY() + 97.0D, center.getZ() + ASSIGNMENT_SEARCH_RADIUS + 1.0D);
        List<Villager> assigned = level.getEntitiesOfClass(Villager.class, area,
                villager -> villager.entityTags().contains(ADVANCED_WORKER_TAG)
                        && villager.entityTags().contains(assignment));
        return assigned.isEmpty() ? null : assigned.getFirst();
    }

    private static String assignmentTag(BuildingRecord workshop) {
        return ADVANCED_ASSIGNMENT_PREFIX + encode(workshop.originX()) + "_" + encode(workshop.originZ());
    }

    private static String encode(int value) {
        return value < 0 ? "n" + Math.abs((long) value) : "p" + value;
    }

    private static void moveOrStop(Villager worker, BlockPos target, double speed) {
        double distance = worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        if (distance > 4.0D) worker.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, speed);
        else worker.getNavigation().stop();
    }

    public static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (level != server.overworld()) return;
        SettlementData data = SettlementData.get(server);
        if (!data.founded() || !level.getBlockState(event.getPos()).is(Blocks.BARREL)) return;
        for (BuildingRecord workshop : data.buildings()) {
            if (workshop.buildingType() == BuildingType.ADVANCED_WORKSHOP
                    && event.getPos().equals(AdvancedWorkshopLayout.commissionCrate(workshop))) {
                event.setCanceled(true);
                event.setNotifyClient(true);
                return;
            }
        }
    }
}
