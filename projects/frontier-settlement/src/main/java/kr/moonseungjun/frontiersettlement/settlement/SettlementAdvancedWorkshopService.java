package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.content.FrontierContent;
import kr.moonseungjun.frontiersettlement.compat.ExternalContentTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import kr.moonseungjun.frontiersettlement.content.FrontierWorkerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Late-game physical forging bridge for companion weapons.
 *
 * Alpha.39's original commission remains intact: an unenchanted recognized external weapon and one
 * expedition relic are placed in the commission barrel, then the assigned artisan physically fetches
 * four real metal before a validated power-30 enchant result can consume anything. Alpha.47 adds a
 * domain-only second pass for an already-enchanted external weapon: two relics and eight real metal can
 * add only new table-compatible enchantments. Existing enchantments are never removed or downgraded,
 * and a no-improvement result consumes nothing.
 *
 * The current endgame bridge adds an optional masterwork catalyst seam. A successful domain reforge with
 * one tagged catalyst compares two bounded enchantment rolls, keeps the stronger candidate and clears the
 * resulting stack's standard prior-work repair cost. The catalyst is consumed only after a real improvement
 * has been validated, and Frontier never links the providing mod's Java classes.
 */
public final class SettlementAdvancedWorkshopService {
    public static final String ADVANCED_WORKER_TAG = "frontier_settlement_advanced_workshop_worker";
    public static final String ADVANCED_ASSIGNMENT_PREFIX = "frontier_settlement_advanced_workshop_";
    public static final int RELIC_COST = 1;
    public static final int METAL_COST = 4;
    public static final int ENCHANTMENT_POWER = 30;
    public static final int REFORGE_RELIC_COST = 2;
    public static final int REFORGE_METAL_COST = 8;
    public static final int REFORGE_POWER = 40;
    public static final int MASTERWORK_CATALYST_COST = 1;

    private static final String WORKER_NAME = "고급 제작 주민";
    private static final int SERVICE_PERIOD_TICKS = 160;
    private static final int METAL_HAUL_BATCH = 4;
    private static final int MASTERWORK_REFORGE_POWER_BONUS = 8;
    private static final int MASTERWORK_REFORGE_ROLLS = 2;
    private static final double INTERACTION_RANGE_SQR = 9.0D;

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

    public static boolean reforgeUnlocked(SettlementData data) {
        return SettlementTier.current(data).ordinal() >= SettlementTier.DOMAIN.ordinal();
    }

    public static void tick(MinecraftServer server, SettlementData data) {
        if (server.getTickCount() % 10 != 0) return;
        ServerLevel level = server.overworld();
        for (BuildingRecord workshop : data.buildings()) {
            if (workshop.buildingType() != BuildingType.ADVANCED_WORKSHOP) continue;
            BlockPos cratePos = AdvancedWorkshopLayout.commissionCrate(workshop);
            BlockPos home = AdvancedWorkshopLayout.artisanHome(workshop);
            if (!level.hasChunkAt(workshop.workCenter()) || !level.hasChunkAt(cratePos) || !level.hasChunkAt(home)) continue;
            if (!(level.getBlockEntity(cratePos) instanceof Container crate)) continue;

            FrontierWorkerEntity worker = findAssignedWorker(level, data, workshop);
            if (worker == null) continue;
            runService(server, level, data, workshop, cratePos, crate, worker);
        }
    }

    public static boolean allAssignmentsLoaded(ServerLevel level, SettlementData data) {
        if (!SettlementStorageService.storageAvailable(level, data)) return false;
        for (BuildingRecord workshop : data.buildings()) {
            if (workshop.buildingType() != BuildingType.ADVANCED_WORKSHOP) continue;
            if (!SettlementWorkerService.workerRouteEvidenceLoaded(level, data, workshop.workCenter(), 12)
                    || !level.hasChunkAt(AdvancedWorkshopLayout.commissionCrate(workshop))
                    || !level.hasChunkAt(AdvancedWorkshopLayout.artisanHome(workshop))) return false;
        }
        return true;
    }

    /** One completed advanced workshop owns exactly one assigned artisan. */
    public static int reconcileLoadedAssignmentDuplicates(ServerLevel level, SettlementData data) {
        int removed = 0;
        for (BuildingRecord workshop : data.buildings()) {
            if (workshop.buildingType() != BuildingType.ADVANCED_WORKSHOP) continue;
            List<FrontierWorkerEntity> assigned = findAssignedWorkers(level, data, workshop);
            if (!assigned.isEmpty()) {
                FrontierWorkerEntity survivor = assigned.getFirst();
                survivor.setNoAi(false);
                survivor.setInvulnerable(false);
            }
            for (int i = 1; i < assigned.size(); i++) {
                if (SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i))) removed++;
            }
        }
        return removed;
    }

    public static int loadedAssignedWorkerCount(ServerLevel level, SettlementData data) {
        Set<java.util.UUID> ids = new HashSet<>();
        for (BuildingRecord workshop : data.buildings()) {
            if (workshop.buildingType() != BuildingType.ADVANCED_WORKSHOP) continue;
            for (FrontierWorkerEntity worker : findAssignedWorkers(level, data, workshop)) ids.add(worker.getUUID());
        }
        return ids.size();
    }

    public static BuildingRecord firstMissingLoadedAssignment(ServerLevel level, SettlementData data) {
        if (!allAssignmentsLoaded(level, data)) return null;
        for (BuildingRecord workshop : data.buildings()) {
            if (workshop.buildingType() != BuildingType.ADVANCED_WORKSHOP) continue;
            if (findAssignedWorkers(level, data, workshop).isEmpty()) return workshop;
        }
        return null;
    }

    public static FrontierWorkerEntity spawnAssignedWorker(ServerLevel level, SettlementData data, BuildingRecord workshop) {
        if (workshop == null || workshop.buildingType() != BuildingType.ADVANCED_WORKSHOP
                || !SettlementWorkerService.workerRouteEvidenceLoaded(level, data, workshop.workCenter(), 12)
                || !level.hasChunkAt(AdvancedWorkshopLayout.artisanHome(workshop))
                || !findAssignedWorkers(level, data, workshop).isEmpty()) return null;
        FrontierWorkerEntity worker = new FrontierWorkerEntity(FrontierContent.FRONTIER_WORKER.get(), level);
        BlockPos spawn = AdvancedWorkshopLayout.artisanHome(workshop);
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(WORKER_NAME));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        worker.addTag(ADVANCED_WORKER_TAG);
        worker.addTag(assignmentTag(workshop));
        if (!level.addFreshEntity(worker)) return null;
        return worker;
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

    public static int readyReforgeCommissionCount(ServerLevel level, SettlementData data) {
        if (!reforgeUnlocked(data)) return 0;
        int ready = 0;
        for (BuildingRecord workshop : data.buildings()) {
            if (workshop.buildingType() != BuildingType.ADVANCED_WORKSHOP) continue;
            BlockPos cratePos = AdvancedWorkshopLayout.commissionCrate(workshop);
            if (!(level.getBlockEntity(cratePos) instanceof Container crate)) continue;
            if (findReforgeWeapon(crate) >= 0 && countRelics(crate) >= REFORGE_RELIC_COST) ready++;
        }
        return ready;
    }

    private static void runService(MinecraftServer server, ServerLevel level, SettlementData data,
                                   BuildingRecord workshop, BlockPos cratePos, Container crate, FrontierWorkerEntity worker) {
        int weaponSlot = findCommissionWeapon(crate);
        boolean reforge = false;
        if (weaponSlot < 0 && reforgeUnlocked(data)) {
            weaponSlot = findReforgeWeapon(crate);
            reforge = weaponSlot >= 0;
        }
        int relicSlot = findRelicSlot(crate);
        int relicRequired = reforge ? REFORGE_RELIC_COST : RELIC_COST;
        int metalRequired = reforge ? REFORGE_METAL_COST : METAL_COST;
        ItemStack carried = worker.getMainHandItem();
        if (weaponSlot < 0 || relicSlot < 0 || countRelics(crate) < relicRequired) {
            if (!carried.isEmpty()) returnCarriedItem(level, data, worker, carried);
            else moveOrStop(worker, AdvancedWorkshopLayout.artisanHome(workshop), 0.68D);
            return;
        }

        if (!carried.isEmpty()) {
            if (!isForgeMetal(carried)) {
                returnCarriedItem(level, data, worker, carried);
                return;
            }
            if (worker.distanceToSqr(cratePos.getX() + 0.5D, cratePos.getY() + 0.5D, cratePos.getZ() + 0.5D)
                    > INTERACTION_RANGE_SQR) {
                SettlementWorkerStorageNavigation.moveToInteraction(
                        level, worker, cratePos, 0.78D, INTERACTION_RANGE_SQR);
                return;
            }
            ItemStack remaining = SettlementInventory.insert(crate, carried);
            worker.setItemSlot(EquipmentSlot.MAINHAND, remaining);
            crate.setChanged();
            SettlementService.refreshResources(server, data);
            SettlementService.broadcast(server, data);
            return;
        }

        int metal = countMatching(crate, SettlementAdvancedWorkshopService::isForgeMetal);
        if (metal >= metalRequired) {
            if (!workDue(server, workshop)) return;
            boolean completed = reforge
                    ? reforgeOne(level, crate, weaponSlot, data)
                    : forgeOne(level, crate, weaponSlot, relicSlot, data);
            if (completed) {
                worker.swing(InteractionHand.MAIN_HAND);
                SettlementService.refreshResources(server, data);
                SettlementService.broadcast(server, data);
            }
            return;
        }

        if (!SettlementStorageService.storageAvailable(level, data)) return;
        BlockPos source = SettlementWorkerStorageNavigation.findReachableExtractionTarget(
                level, data, worker, SettlementAdvancedWorkshopService::isForgeMetal, INTERACTION_RANGE_SQR);
        if (source == null) {
            worker.getNavigation().stop();
            return;
        }
        if (worker.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > INTERACTION_RANGE_SQR) {
            SettlementWorkerStorageNavigation.moveToInteraction(
                    level, worker, source, 0.82D, INTERACTION_RANGE_SQR);
            return;
        }
        ItemStack metalStack = SettlementStorageService.extract(level, source, SettlementAdvancedWorkshopService::isForgeMetal,
                Math.min(metalRequired - metal, METAL_HAUL_BATCH));
        if (metalStack.isEmpty()) return;
        worker.setItemSlot(EquipmentSlot.MAINHAND, metalStack);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
    }

    /** Alpha.39 original forge path. Keep its validation-before-consumption order intact. */
    private static boolean forgeOne(ServerLevel level, Container crate, int weaponSlot, int relicSlot, SettlementData data) {
        ItemStack weapon = crate.getItem(weaponSlot);
        ItemStack relic = crate.getItem(relicSlot);
        if (!isForgeableWeapon(weapon) || !isExpeditionRelic(relic)) return false;
        if (countMatching(crate, SettlementAdvancedWorkshopService::isForgeMetal) < METAL_COST) return false;

        ItemStack forged = weapon.copy();
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemStack enchanted = EnchantmentHelper.enchantItem(level.getRandom(), forged, SettlementExplorationBenefitService.forgePower(data),
                enchantments.listElements()
                        .<Holder<Enchantment>>map(holder -> holder)
                        .filter(holder -> holder.is(EnchantmentTags.IN_ENCHANTING_TABLE))
                        .filter(forged::supportsEnchantment));
        if (EnchantmentHelper.getEnchantmentsForCrafting(enchanted).isEmpty()) return false;

        enchanted.setDamageValue(0);
        if (!consumeMatching(crate, SettlementAdvancedWorkshopService::isForgeMetal, METAL_COST)) return false;
        relic.shrink(RELIC_COST);
        if (relic.isEmpty()) crate.setItem(relicSlot, ItemStack.EMPTY);
        crate.setItem(weaponSlot, enchanted);
        crate.setChanged();
        return true;
    }

    /**
     * Domain reforge. Candidate selection excludes every enchantment already present and rejects anything
     * incompatible with the existing set. A tagged masterwork catalyst is optional: when present it compares
     * two bounded higher-power candidates and clears the successful output's standard prior-work repair cost.
     * Existing enchantments are never removed or downgraded, and all physical costs mutate only after a valid
     * improvement has been selected.
     */
    private static boolean reforgeOne(ServerLevel level, Container crate, int weaponSlot, SettlementData data) {
        ItemStack weapon = crate.getItem(weaponSlot);
        if (!isReforgeableWeapon(weapon)) return false;
        if (countRelics(crate) < REFORGE_RELIC_COST) return false;
        if (countMatching(crate, SettlementAdvancedWorkshopService::isForgeMetal) < REFORGE_METAL_COST) return false;

        boolean masterwork = countMatching(crate, SettlementAdvancedWorkshopService::isMasterworkCatalyst)
                >= MASTERWORK_CATALYST_COST;
        ItemStack reforged = weapon.copy();
        var existing = EnchantmentHelper.getEnchantmentsForCrafting(reforged);
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int power = SettlementExplorationBenefitService.reforgePower(data)
                + (masterwork ? MASTERWORK_REFORGE_POWER_BONUS : 0);
        int rolls = masterwork ? MASTERWORK_REFORGE_ROLLS : 1;
        List<EnchantmentInstance> additions = List.of();
        int bestScore = -1;
        for (int roll = 0; roll < rolls; roll++) {
            List<EnchantmentInstance> candidate = EnchantmentHelper.selectEnchantment(level.getRandom(), reforged, power,
                    enchantments.listElements()
                            .<Holder<Enchantment>>map(holder -> holder)
                            .filter(holder -> holder.is(EnchantmentTags.IN_ENCHANTING_TABLE))
                            .filter(reforged::supportsEnchantment)
                            .filter(holder -> existing.getLevel(holder) == 0)
                            .filter(holder -> EnchantmentHelper.isEnchantmentCompatible(existing.keySet(), holder)));
            int score = candidate.size();
            for (EnchantmentInstance addition : candidate) score += addition.level() * 10;
            if (score > bestScore) {
                additions = candidate;
                bestScore = score;
            }
        }
        if (additions.isEmpty()) return false;
        for (EnchantmentInstance addition : additions) {
            reforged.enchant(addition.enchantment(), addition.level());
        }
        var result = EnchantmentHelper.getEnchantmentsForCrafting(reforged);
        if (result.equals(existing)) return false;
        for (Holder<Enchantment> holder : existing.keySet()) {
            if (result.getLevel(holder) < existing.getLevel(holder)) return false;
        }

        reforged.setDamageValue(0);
        if (masterwork) reforged.set(DataComponents.REPAIR_COST, 0);
        if (!consumeMatching(crate, SettlementAdvancedWorkshopService::isForgeMetal, REFORGE_METAL_COST)) return false;
        if (!consumeMatching(crate, SettlementAdvancedWorkshopService::isExpeditionRelic, REFORGE_RELIC_COST)) return false;
        if (masterwork && !consumeMatching(crate, SettlementAdvancedWorkshopService::isMasterworkCatalyst,
                MASTERWORK_CATALYST_COST)) return false;
        crate.setItem(weaponSlot, reforged);
        crate.setChanged();
        return true;
    }

    private static int findCommissionWeapon(Container crate) {
        for (int slot = 0; slot < crate.getContainerSize(); slot++) {
            if (isForgeableWeapon(crate.getItem(slot))) return slot;
        }
        return -1;
    }

    private static int findReforgeWeapon(Container crate) {
        for (int slot = 0; slot < crate.getContainerSize(); slot++) {
            if (isReforgeableWeapon(crate.getItem(slot))) return slot;
        }
        return -1;
    }

    private static boolean isForgeableWeapon(ItemStack stack) {
        return SettlementExternalContentService.isExternalWeapon(stack)
                && EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty();
    }

    private static boolean isReforgeableWeapon(ItemStack stack) {
        return SettlementExternalContentService.isExternalWeapon(stack)
                && !EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty();
    }

    /**
     * Metal, relic and masterwork catalyst are intentionally disjoint physical cost domains. Companion
     * datapacks can extend each tag independently; if one stack is accidentally cross-tagged, catalyst wins,
     * then relic, so a later sequential consume can never invalidate a cost that was already committed.
     */
    private static boolean isForgeMetal(ItemStack stack) {
        return !stack.isEmpty()
                && SettlementStorageService.isMetalStack(stack)
                && !isExpeditionRelic(stack)
                && !isMasterworkCatalyst(stack);
    }

    private static boolean isExpeditionRelic(ItemStack stack) {
        return !stack.isEmpty()
                && stack.is(ExternalContentTags.EXPEDITION_RELICS)
                && !isMasterworkCatalyst(stack);
    }

    private static boolean isMasterworkCatalyst(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ExternalContentTags.MASTERWORK_CATALYSTS);
    }

    private static int findRelicSlot(Container crate) {
        for (int slot = 0; slot < crate.getContainerSize(); slot++) {
            ItemStack stack = crate.getItem(slot);
            if (isExpeditionRelic(stack)) return slot;
        }
        return -1;
    }

    private static int countRelics(Container crate) {
        return countMatching(crate, SettlementAdvancedWorkshopService::isExpeditionRelic);
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

    private static void returnCarriedItem(ServerLevel level, SettlementData data, FrontierWorkerEntity worker, ItemStack carried) {
        BlockPos target = SettlementWorkerStorageNavigation.findReachableDepositTarget(
                level, data, worker, carried, INTERACTION_RANGE_SQR);
        if (target == null) { worker.getNavigation().stop(); return; }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                > INTERACTION_RANGE_SQR) {
            SettlementWorkerStorageNavigation.moveToInteraction(
                    level, worker, target, 0.8D, INTERACTION_RANGE_SQR);
            return;
        }
        if (!(level.getBlockEntity(target) instanceof Container container)) { worker.getNavigation().stop(); return; }
        ItemStack remaining = SettlementInventory.insert(container, carried);
        worker.setItemSlot(EquipmentSlot.MAINHAND, remaining);
        if (remaining.isEmpty()) worker.getNavigation().stop();
    }

    private static FrontierWorkerEntity findAssignedWorker(ServerLevel level, SettlementData data, BuildingRecord workshop) {
        List<FrontierWorkerEntity> assigned = findAssignedWorkers(level, data, workshop);
        return assigned.isEmpty() ? null : assigned.getFirst();
    }

    private static List<FrontierWorkerEntity> findAssignedWorkers(ServerLevel level, SettlementData data, BuildingRecord workshop) {
        String assignment = assignmentTag(workshop);
        AABB area = SettlementWorkerService.workerRouteBounds(data, workshop.workCenter(), 12);
        List<FrontierWorkerEntity> assigned = level.getEntitiesOfClass(FrontierWorkerEntity.class, area,
                villager -> villager.entityTags().contains(ADVANCED_WORKER_TAG)
                        && villager.entityTags().contains(assignment));
        assigned.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return assigned;
    }

    private static String assignmentTag(BuildingRecord workshop) {
        return ADVANCED_ASSIGNMENT_PREFIX + encode(workshop.originX()) + "_" + encode(workshop.originZ());
    }

    private static String encode(int value) {
        return value < 0 ? "n" + Math.abs((long) value) : "p" + value;
    }

    private static void moveOrStop(FrontierWorkerEntity worker, BlockPos target, double speed) {
        double distance = worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        if (distance > 4.0D) worker.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, speed);
        else worker.getNavigation().stop();
    }

    public static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (level != server.overworld()) return;
        if (!event.getState().is(Blocks.BARREL)) return;
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
