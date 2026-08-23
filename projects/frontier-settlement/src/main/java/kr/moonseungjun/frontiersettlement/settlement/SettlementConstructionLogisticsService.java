package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public final class SettlementConstructionLogisticsService {
    private static final int HAUL_BATCH = 16;
    private static final double PICKUP_RANGE_SQR = 9.0D;
    private static final double DROP_RANGE_SQR = 9.0D;

    private SettlementConstructionLogisticsService() {}

    public static boolean tick(MinecraftServer server, SettlementData data) {
        ServerLevel level = server.overworld();
        ConstructionLogisticsData logistics = ConstructionLogisticsData.get(server);
        ConstructionState construction = data.construction();

        if (!construction.active()) {
            if (logistics.active()) cleanupFinishedSite(level, data, logistics);
            return false;
        }

        BuildingType type = BuildingType.fromId(construction.type());
        if (type == null) return true;

        if (!logistics.matches(construction)) {
            // Alpha.22 worlds may already have a partially built structure. Let those finish under
            // their original rules instead of inventing a staging cache midway through the build.
            if (construction.step() > 0) return true;
            logistics.begin(construction);
        }

        BlockPos stage = stagingPosition(type, construction);
        if (stage == null) return false;
        Container site = ensureSiteBarrel(level, type, construction, stage);
        if (site == null) return false;

        Villager builder = SettlementConstructionService.ensureBuilder(level, data.centerPos());
        if (builder == null) return false;
        builder.setInvulnerable(true);

        if (construction.step() == 0) {
            return stageMaterials(level, data, builder, site, stage, type);
        }
        return hasExpectedMaterials(site, type, construction.step(), totalSteps(type, construction));
    }

    public static void afterPlacementTick(MinecraftServer server, SettlementData data,
                                          ConstructionState before, BuildingType type) {
        if (type == null) return;
        ConstructionLogisticsData logistics = ConstructionLogisticsData.get(server);
        if (!logistics.matches(before)) return;

        ServerLevel level = server.overworld();
        BlockPos stage = stagingPosition(type, before);
        if (stage == null || !(level.getBlockEntity(stage) instanceof Container site)) return;

        int total = totalSteps(type, before);
        int afterStep = total;
        if (data.construction().active() && logistics.matches(data.construction())) {
            afterStep = Math.min(total, Math.max(0, data.construction().step()));
        }
        consumeScheduled(site, type, Math.max(0, before.step()), afterStep, total);

        if (!data.construction().active() || !logistics.matches(data.construction())) {
            cleanupFinishedSite(level, data, logistics);
            return;
        }

        String oldPhase = phaseLabel(type, before);
        String newPhase = phaseLabel(type, data.construction());
        if (!oldPhase.equals(newPhase)) SettlementService.broadcast(server, data);
    }

    public static String phaseLabel(ConstructionState construction) {
        BuildingType type = BuildingType.fromId(construction.type());
        if (type == null) return "건설";
        return phaseLabel(type, construction);
    }

    private static String phaseLabel(BuildingType type, ConstructionState construction) {
        if (construction.step() <= 0) return "자재 운반";
        List<BuildingBlueprints.Placement> plan = RotatedBlueprints.create(type, construction.origin(), construction.rotation());
        if (plan.isEmpty()) return "건설";
        int index = Math.min(construction.step(), plan.size() - 1);
        return switch (plan.get(index).phase()) {
            case FLOOR -> "기초·바닥";
            case FRAME_AND_WALLS -> "골조·벽";
            case ROOF -> "지붕";
            case FINISH -> "마감·조명";
        };
    }

    private static boolean stageMaterials(ServerLevel level, SettlementData data, Villager builder,
                                          Container site, BlockPos stage, BuildingType type) {
        long wood = SettlementInventory.countWood(site);
        long stone = SettlementInventory.countStone(site);
        ItemStack carried = builder.getMainHandItem();

        if (!carried.isEmpty()) {
            if (!SettlementInventory.isWood(carried) && !SettlementInventory.isStone(carried)) return false;
            if (builder.distanceToSqr(stage.getX() + 0.5D, stage.getY() + 0.5D, stage.getZ() + 0.5D) > DROP_RANGE_SQR) {
                builder.getNavigation().moveTo(stage.getX() + 0.5D, stage.getY(), stage.getZ() + 0.5D, 0.85D);
                return false;
            }
            ItemStack remaining = SettlementInventory.insert(site, carried);
            builder.setItemSlot(EquipmentSlot.MAINHAND, remaining);
            return false;
        }

        if (wood >= type.woodCost() && stone >= type.stoneCost()) {
            builder.getNavigation().stop();
            return true;
        }

        BlockPos pickup = data.stockpilePos();
        if (builder.distanceToSqr(pickup.getX() + 0.5D, pickup.getY() + 0.5D, pickup.getZ() + 0.5D) > PICKUP_RANGE_SQR) {
            builder.getNavigation().moveTo(pickup.getX() + 0.5D, pickup.getY(), pickup.getZ() + 0.5D, 0.9D);
            return false;
        }

        if (wood < type.woodCost()) {
            int count = (int) Math.min(HAUL_BATCH, type.woodCost() - wood);
            builder.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.OAK_PLANKS, count));
        } else if (stone < type.stoneCost()) {
            int count = (int) Math.min(HAUL_BATCH, type.stoneCost() - stone);
            builder.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.COBBLESTONE, count));
        }
        return false;
    }

    private static boolean hasExpectedMaterials(Container site, BuildingType type, int step, int total) {
        long expectedWood = type.woodCost() - consumedAt(type.woodCost(), step, total);
        long expectedStone = type.stoneCost() - consumedAt(type.stoneCost(), step, total);
        return SettlementInventory.countWood(site) >= expectedWood
                && SettlementInventory.countStone(site) >= expectedStone;
    }

    private static void consumeScheduled(Container site, BuildingType type, int beforeStep, int afterStep, int total) {
        if (afterStep <= beforeStep || total <= 0) return;
        long wood = consumedAt(type.woodCost(), afterStep, total) - consumedAt(type.woodCost(), beforeStep, total);
        long stone = consumedAt(type.stoneCost(), afterStep, total) - consumedAt(type.stoneCost(), beforeStep, total);
        if (wood > 0L || stone > 0L) SettlementInventory.consume(site, wood, stone, 0L);
    }

    private static long consumedAt(long cost, int step, int total) {
        if (cost <= 0L || total <= 0) return 0L;
        int clamped = Math.max(0, Math.min(step, total));
        return (cost * clamped) / total;
    }

    private static int totalSteps(BuildingType type, ConstructionState construction) {
        return Math.max(1, SettlementConstructionService.totalSteps(type, construction.origin(), construction.rotation()));
    }

    private static Container ensureSiteBarrel(ServerLevel level, BuildingType type,
                                              ConstructionState construction, BlockPos stage) {
        if (level.getBlockState(stage).is(Blocks.BARREL) && level.getBlockEntity(stage) instanceof Container container) {
            return container;
        }
        if (!level.getBlockState(stage).isAir()) return null;

        BuildingBlueprints.Placement floor = floorPlacementAt(type, construction, stage.below());
        if (floor == null) return null;
        if (level.getBlockState(floor.pos()).isAir()) {
            level.setBlock(floor.pos(), floor.state(), 3);
        } else if (!level.getBlockState(floor.pos()).is(floor.state().getBlock())) {
            return null;
        }

        level.setBlock(stage, Blocks.BARREL.defaultBlockState(), 3);
        return level.getBlockEntity(stage) instanceof Container container ? container : null;
    }

    private static BuildingBlueprints.Placement floorPlacementAt(BuildingType type,
                                                                  ConstructionState construction,
                                                                  BlockPos floorPos) {
        for (BuildingBlueprints.Placement placement : RotatedBlueprints.create(
                type, construction.origin(), construction.rotation())) {
            if (placement.phase() == BuildingBlueprints.Phase.FLOOR && placement.pos().equals(floorPos)) return placement;
        }
        return null;
    }

    private static BlockPos stagingPosition(BuildingType type, ConstructionState construction) {
        List<BuildingBlueprints.Placement> plan = RotatedBlueprints.create(type, construction.origin(), construction.rotation());
        int centerX = construction.originX() + construction.buildingRotation().rotatedWidth(type) / 2;
        int centerZ = construction.originZ() + construction.buildingRotation().rotatedDepth(type) / 2;
        BlockPos best = null;
        long bestDistance = Long.MAX_VALUE;

        for (BuildingBlueprints.Placement floor : plan) {
            if (floor.phase() != BuildingBlueprints.Phase.FLOOR || floor.pos().getY() != construction.originY()) continue;
            BlockPos candidate = floor.pos().above();
            boolean occupied = false;
            for (BuildingBlueprints.Placement placement : plan) {
                if (placement.pos().equals(candidate)) {
                    occupied = true;
                    break;
                }
            }
            if (occupied) continue;
            long dx = candidate.getX() - centerX;
            long dz = candidate.getZ() - centerZ;
            long distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static void cleanupFinishedSite(ServerLevel level, SettlementData data, ConstructionLogisticsData logistics) {
        ConstructionState identity = new ConstructionState("", 0, 0, 0, 0, 0);
        // Reconstruct the finished build identity from the logistics record through the last active
        // construction when possible. If there is no active build, scan for the nearby temporary
        // barrel is intentionally avoided; a player-owned barrel must never be guessed and deleted.
        if (data.construction().active() && logistics.matches(data.construction())) identity = data.construction();

        if (identity.active()) {
            BuildingType type = BuildingType.fromId(identity.type());
            BlockPos stage = type == null ? null : stagingPosition(type, identity);
            if (stage != null && level.getBlockEntity(stage) instanceof Container container) {
                drainReservedMaterials(container);
                if (containerIsEmpty(container)) level.setBlock(stage, Blocks.AIR.defaultBlockState(), 2);
            }
        }

        Villager builder = SettlementConstructionService.ensureBuilder(level, data.centerPos());
        if (builder != null) builder.setInvulnerable(false);
        logistics.clear();
    }

    private static void drainReservedMaterials(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            if (SettlementInventory.isWood(stack) || SettlementInventory.isStone(stack)) stack.setCount(0);
        }
        container.setChanged();
    }

    private static boolean containerIsEmpty(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) if (!container.getItem(slot).isEmpty()) return false;
        return true;
    }
}
