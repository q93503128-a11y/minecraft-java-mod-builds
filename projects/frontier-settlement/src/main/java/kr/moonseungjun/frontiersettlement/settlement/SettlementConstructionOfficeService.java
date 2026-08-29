package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.content.FrontierContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import kr.moonseungjun.frontiersettlement.content.FrontierWorkerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Construction offices do not create an abstract speed stat. Their physical supply runner keeps
 * nearby office material bays stocked from real loaded settlement storage, while the existing
 * construction builder remains the only authority that grades and places project blocks.
 */
public final class SettlementConstructionOfficeService {
    public static final String SUPPLY_RUNNER_TAG = "frontier_settlement_construction_supply_runner";
    public static final String OFFICE_ASSIGNMENT_PREFIX = "frontier_settlement_construction_office_";
    private static final String RUNNER_NAME = "건설 보급 주민";
    private static final int SERVICE_INTERVAL_TICKS = 10;
    private static final int HAUL_BATCH_SIZE = 32;
    private static final int TARGET_WOOD_RESERVE = 96;
    private static final int TARGET_STONE_RESERVE = 96;
    private static final int SOURCE_RADIUS = 24;
    private static final double INTERACTION_RANGE_SQR = 9.0D;
    private static final double RUNNER_SEARCH_RADIUS = 36.0D;
    private static final int RUNNER_ROUTE_MARGIN = 16;

    private SettlementConstructionOfficeService() {}

    public record SupplySnapshot(int wood, int stone, int runners) {}

    public static String lockedReason(SettlementData data) {
        if (SettlementTier.current(data).ordinal() < SettlementTier.VILLAGE.ordinal()) {
            return "건설소는 마을 단계에 도달하면 열립니다.";
        }
        if (data.buildingCount(BuildingType.WAREHOUSE) < 1) {
            return "건설소는 창고 1곳을 먼저 완성하면 열립니다.";
        }
        return null;
    }

    public static void tick(MinecraftServer server, SettlementData data) {
        if (server.getTickCount() % SERVICE_INTERVAL_TICKS != 0) return;
        ServerLevel level = server.overworld();
        boolean activeProject = data.construction().active();
        boolean rest = SettlementResidentRoutineService.isRestTime(level);

        for (BuildingRecord office : offices(data)) {
            BlockPos home = office.localToWorld(6, 1, 5);
            if (!localServiceAreaLoaded(level, office)) continue;
            FrontierWorkerEntity runner = ensureSingleRunner(level, data, office, home);
            if (runner == null) continue;
            runner.setInvulnerable(true);

            if (!runner.getMainHandItem().isEmpty()) {
                if (deliverCarried(level, data, office, runner)) continue;
            }
            if (rest || !activeProject) {
                moveOrStop(runner, home, 0.72D);
                continue;
            }
            refillOffice(level, data, office, runner, home);
        }
    }

    public static SupplySnapshot snapshot(ServerLevel level, SettlementData data) {
        int wood = 0;
        int stone = 0;
        int runners = 0;
        for (BuildingRecord office : offices(data)) {
            for (BlockPos pos : ConstructionOfficeLayout.materialPositions(office)) {
                if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
                wood += SettlementInventory.countWood(container);
                stone += SettlementInventory.countStone(container);
            }
            if (localServiceAreaLoaded(level, office)
                    && findRunner(level, data, office, office.localToWorld(6, 1, 5)) != null) runners++;
        }
        return new SupplySnapshot(wood, stone, runners);
    }

    private static void refillOffice(ServerLevel level, SettlementData data, BuildingRecord office,
                                     FrontierWorkerEntity runner, BlockPos home) {
        int wood = countOffice(level, office, SettlementInventory::isWood);
        int stone = countOffice(level, office, SettlementInventory::isStone);
        int missingWood = Math.max(0, TARGET_WOOD_RESERVE - wood);
        int missingStone = Math.max(0, TARGET_STONE_RESERVE - stone);
        if (missingWood == 0 && missingStone == 0) {
            moveOrStop(runner, home, 0.72D);
            return;
        }

        boolean wantWood = missingWood >= missingStone;
        Predicate<ItemStack> wanted = wantWood ? SettlementInventory::isWood : SettlementInventory::isStone;
        int missing = wantWood ? missingWood : missingStone;
        BlockPos source = nearestOrdinarySource(level, data, office.workCenter(), wanted);
        if (source == null) {
            wantWood = !wantWood;
            wanted = wantWood ? SettlementInventory::isWood : SettlementInventory::isStone;
            missing = wantWood ? missingWood : missingStone;
            if (missing <= 0) {
                moveOrStop(runner, home, 0.72D);
                return;
            }
            source = nearestOrdinarySource(level, data, office.workCenter(), wanted);
            if (source == null) {
                moveOrStop(runner, home, 0.72D);
                return;
            }
        }

        if (runner.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > INTERACTION_RANGE_SQR) {
            runner.getNavigation().moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D, 0.86D);
            return;
        }
        ItemStack extracted = SettlementStorageService.extract(level, source, wanted,
                Math.min(HAUL_BATCH_SIZE, Math.max(1, missing)));
        if (!extracted.isEmpty()) runner.setItemSlot(EquipmentSlot.MAINHAND, extracted);
    }

    private static boolean deliverCarried(ServerLevel level, SettlementData data, BuildingRecord office, FrontierWorkerEntity runner) {
        ItemStack carried = runner.getMainHandItem();
        BlockPos target = nearestOfficeRoom(level, office, carried, runner.blockPosition());
        if (target == null) target = nearestOrdinaryDeposit(level, data, office, carried, runner.blockPosition());
        if (target == null) {
            runner.getNavigation().stop();
            return true;
        }
        if (runner.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                > INTERACTION_RANGE_SQR) {
            runner.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.88D);
            return true;
        }
        ItemStack remaining = SettlementStorageService.insertAt(level, target, carried);
        runner.setItemSlot(EquipmentSlot.MAINHAND, remaining);
        return true;
    }

    private static BlockPos nearestOfficeRoom(ServerLevel level, BuildingRecord office, ItemStack carried, BlockPos from) {
        return ConstructionOfficeLayout.materialPositions(office).stream()
                .filter(pos -> SettlementStorageService.hasRoomAt(level, pos, carried))
                .min(Comparator.comparingDouble(pos -> pos.distSqr(from)))
                .orElse(null);
    }

    private static BlockPos nearestOrdinarySource(ServerLevel level, SettlementData data, BlockPos office,
                                                   Predicate<ItemStack> wanted) {
        double maxDistance = (double) SOURCE_RADIUS * SOURCE_RADIUS;
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : SettlementStorageService.ordinaryStoragePositions(data)) {
            if (!level.hasChunkAt(pos) || !corridorLoaded(level, office, pos)) continue;
            double distance = pos.distSqr(office);
            if (distance > maxDistance || distance >= bestDistance) continue;
            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
            boolean found = false;
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (!stack.isEmpty() && wanted.test(stack)) { found = true; break; }
            }
            if (found) { best = pos; bestDistance = distance; }
        }
        return best;
    }

    private static BlockPos nearestOrdinaryDeposit(ServerLevel level, SettlementData data, BuildingRecord officeRecord, ItemStack carried, BlockPos from) {
        BlockPos office = officeRecord.workCenter();
        double maxDistance = (double) SOURCE_RADIUS * SOURCE_RADIUS;
        return SettlementStorageService.ordinaryStoragePositions(data).stream()
                .filter(pos -> office == null || pos.distSqr(office) <= maxDistance)
                .filter(pos -> corridorLoaded(level, from, pos))
                .filter(pos -> SettlementStorageService.hasRoomAt(level, pos, carried))
                .min(Comparator.comparingDouble(pos -> pos.distSqr(from)))
                .orElse(null);
    }

    private static int countOffice(ServerLevel level, BuildingRecord office, Predicate<ItemStack> predicate) {
        int total = 0;
        for (BlockPos pos : ConstructionOfficeLayout.materialPositions(office)) {
            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (!stack.isEmpty() && predicate.test(stack)) total += stack.getCount();
            }
        }
        return total;
    }

    private static FrontierWorkerEntity ensureSingleRunner(ServerLevel level, SettlementData data, BuildingRecord office, BlockPos home) {
        List<FrontierWorkerEntity> existing = findRunners(level, data, office, home);
        if (!existing.isEmpty()) {
            FrontierWorkerEntity keep = existing.getFirst();
            keep.setNoAi(false);
            keep.setInvulnerable(true);
            for (int i = 1; i < existing.size(); i++) {
                FrontierWorkerEntity duplicate = existing.get(i);
                duplicate.getNavigation().stop();
                duplicate.setNoAi(true);
                duplicate.setInvulnerable(true);
            }
            return keep;
        }
        if (!runnerAssignmentEvidenceLoaded(level, data, office)) return null;
        FrontierWorkerEntity runner = new FrontierWorkerEntity(FrontierContent.FRONTIER_WORKER.get(), level);
        runner.setPos(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
        runner.setCustomName(Component.literal(RUNNER_NAME));
        runner.setCustomNameVisible(true);
        runner.setPersistenceRequired();
        runner.setInvulnerable(true);
        runner.addTag(SUPPLY_RUNNER_TAG);
        runner.addTag(assignmentTag(office));
        return level.addFreshEntity(runner) ? runner : null;
    }

    private static FrontierWorkerEntity findRunner(ServerLevel level, SettlementData data, BuildingRecord office, BlockPos home) {
        List<FrontierWorkerEntity> runners = findRunners(level, data, office, home);
        return runners.isEmpty() ? null : runners.getFirst();
    }

    private static List<FrontierWorkerEntity> findRunners(ServerLevel level, SettlementData data, BuildingRecord office, BlockPos home) {
        String assignment = assignmentTag(office);
        List<FrontierWorkerEntity> runners = level.getEntitiesOfClass(FrontierWorkerEntity.class, runnerRouteBounds(data, office),
                villager -> villager.entityTags().contains(SUPPLY_RUNNER_TAG)
                        && villager.entityTags().contains(assignment));
        runners.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return runners;
    }

    private static AABB runnerRouteBounds(SettlementData data, BuildingRecord office) {
        BlockPos center=office.workCenter();
        int minX=center.getX(), minY=center.getY(), minZ=center.getZ();
        int maxX=center.getX()+1, maxY=center.getY()+1, maxZ=center.getZ()+1;
        for (BlockPos pos : ConstructionOfficeLayout.materialPositions(office)) {
            minX=Math.min(minX,pos.getX()); minY=Math.min(minY,pos.getY()); minZ=Math.min(minZ,pos.getZ());
            maxX=Math.max(maxX,pos.getX()+1); maxY=Math.max(maxY,pos.getY()+1); maxZ=Math.max(maxZ,pos.getZ()+1);
        }
        // Pre-Alpha.71 runners could return carried cargo to any ordinary storage endpoint. Keep
        // those concrete endpoints in the absence-proof envelope so old in-flight runners are not replaced.
        for (BlockPos pos : SettlementStorageService.ordinaryStoragePositions(data)) {
            minX=Math.min(minX,pos.getX()); minY=Math.min(minY,pos.getY()); minZ=Math.min(minZ,pos.getZ());
            maxX=Math.max(maxX,pos.getX()+1); maxY=Math.max(maxY,pos.getY()+1); maxZ=Math.max(maxZ,pos.getZ()+1);
        }
        return new AABB(minX-RUNNER_ROUTE_MARGIN,minY-12,minZ-RUNNER_ROUTE_MARGIN,
                maxX+RUNNER_ROUTE_MARGIN,maxY+12,maxZ+RUNNER_ROUTE_MARGIN);
    }

    private static boolean runnerAssignmentEvidenceLoaded(ServerLevel level, SettlementData data, BuildingRecord office) {
        AABB bounds=runnerRouteBounds(data,office);
        int minChunkX=Math.floorDiv((int)Math.floor(bounds.minX),16);
        int maxChunkX=Math.floorDiv((int)Math.floor(Math.nextDown(bounds.maxX)),16);
        int minChunkZ=Math.floorDiv((int)Math.floor(bounds.minZ),16);
        int maxChunkZ=Math.floorDiv((int)Math.floor(Math.nextDown(bounds.maxZ)),16);
        int probeY=office.workCenter().getY();
        for (int chunkX=minChunkX; chunkX<=maxChunkX; chunkX++) {
            for (int chunkZ=minChunkZ; chunkZ<=maxChunkZ; chunkZ++) {
                if (!level.hasChunkAt(new BlockPos(chunkX*16+8,probeY,chunkZ*16+8))) return false;
            }
        }
        return true;
    }

    private static boolean localServiceAreaLoaded(ServerLevel level, BuildingRecord office) {
        BlockPos center = office.workCenter();
        if (!level.hasChunkAt(center)) return false;
        int[] offsets = {-16, 16};
        for (int dx : offsets) for (int dz : offsets) if (!level.hasChunkAt(center.offset(dx, 0, dz))) return false;
        for (BlockPos pos : ConstructionOfficeLayout.materialPositions(office)) if (!level.hasChunkAt(pos)) return false;
        return true;
    }

    private static boolean corridorLoaded(ServerLevel level, BlockPos from, BlockPos to) {
        int steps = Math.max(1, Math.max(Math.abs(to.getX() - from.getX()), Math.abs(to.getZ() - from.getZ())) / 4);
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int x = (int) Math.round(from.getX() + (to.getX() - from.getX()) * t);
            int y = (int) Math.round(from.getY() + (to.getY() - from.getY()) * t);
            int z = (int) Math.round(from.getZ() + (to.getZ() - from.getZ()) * t);
            if (!level.hasChunkAt(new BlockPos(x, y, z))) return false;
        }
        return true;
    }

    private static void moveOrStop(FrontierWorkerEntity runner, BlockPos target, double speed) {
        if (runner.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 4.0D) {
            runner.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, speed);
        } else runner.getNavigation().stop();
    }

    private static String assignmentTag(BuildingRecord office) {
        return OFFICE_ASSIGNMENT_PREFIX + office.originX() + "_" + office.originY() + "_" + office.originZ();
    }

    private static List<BuildingRecord> offices(SettlementData data) {
        return data.buildings().stream().filter(building -> building.buildingType() == BuildingType.CONSTRUCTION_OFFICE).toList();
    }

    public static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        SettlementData data = SettlementData.get(level.getServer());
        if (!data.founded() || !level.getBlockState(event.getPos()).is(Blocks.BARREL)) return;
        for (BuildingRecord office : offices(data)) {
            if (ConstructionOfficeLayout.materialPositions(office).contains(event.getPos())) {
                event.setCanceled(true);
                event.setNotifyClient(true);
                return;
            }
        }
    }
}
