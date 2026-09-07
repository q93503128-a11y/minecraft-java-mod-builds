package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

/**
 * Player-directed RTS investment for central storage and physical outpost freight.
 *
 * This service never creates virtual inventory or a second transport authority. Warehouse grades
 * only add bounded physical barrels; cart-station grades enlarge the existing road transporter's
 * productive pickup cap. Military/waterfront reverse supply deliberately keeps its old cap.
 */
public final class SettlementLogisticsUpgradeService {
    private SettlementLogisticsUpgradeService() {}

    public record UpgradeCost(long wood, long stone, long copperIronItems) {}

    public static void tick(MinecraftServer server, SettlementData data) {
        if (server.getTickCount() % 20 != 0) return;
        if (!migrateLegacyGrades(data)) return;
        SettlementStorageService.ensureManagedStorage(server.overworld(), data);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
    }

    /** Pre-Alpha.123 logistics buildings keep their existing base storage and begin at grade I. */
    static boolean migrateLegacyGrades(SettlementData data) {
        boolean changed = false;
        for (BuildingRecord building : List.copyOf(data.buildings())) {
            BuildingType type = building.buildingType();
            if (!isLogisticsBuilding(type) || building.upgradeGrade() > 0) continue;
            changed |= data.replaceCompletedBuilding(building, building.withUpgradeGrade(1));
        }
        return changed;
    }

    public static boolean isLogisticsBuilding(BuildingType type) {
        return type == BuildingType.WAREHOUSE || type == BuildingType.CART_STATION;
    }

    public static int grade(BuildingRecord building) {
        if (building == null || !isLogisticsBuilding(building.buildingType())) return 0;
        return Math.max(1, Math.min(3, building.upgradeGrade()));
    }

    public static int bestGrade(SettlementData data, BuildingType type) {
        int best = 0;
        if (!isLogisticsBuilding(type)) return best;
        for (BuildingRecord building : data.buildings()) {
            if (building.buildingType() == type) best = Math.max(best, grade(building));
        }
        return best;
    }

    public static int maxGrade(SettlementData data) {
        return switch (SettlementTier.current(data)) {
            case CAMP, HAMLET, VILLAGE -> 1;
            case FRONTIER_TOWN -> 2;
            case DOMAIN, FRONTIER_CAPITAL -> 3;
        };
    }

    public static String gradeLabel(int grade) {
        return switch (Math.max(1, Math.min(3, grade))) {
            case 1 -> "I";
            case 2 -> "II";
            default -> "III";
        };
    }

    public static UpgradeCost costFor(BuildingType type, int grade) {
        int normalized = Math.max(2, Math.min(3, grade));
        if (type == BuildingType.WAREHOUSE) {
            return normalized == 2
                    ? new UpgradeCost(256L, 192L, 24L)
                    : new UpgradeCost(512L, 384L, 64L);
        }
        if (type == BuildingType.CART_STATION) {
            return normalized == 2
                    ? new UpgradeCost(320L, 224L, 32L)
                    : new UpgradeCost(640L, 448L, 96L);
        }
        return new UpgradeCost(0L, 0L, 0L);
    }

    public static int warehouseFreightBonus(SettlementData data) {
        return switch (bestGrade(data, BuildingType.WAREHOUSE)) {
            case 2 -> 4;
            case 3 -> 8;
            default -> 0;
        };
    }

    public static int warehouseStorageCount(BuildingRecord building) {
        return WarehouseLayout.activeStoragePositions(building).size();
    }

    public static int cartFreightStorageCount(BuildingRecord building) {
        return CartStationLayout.activeFreightPositions(building).size();
    }

    public static String storageSummary(ServerLevel level, BuildingRecord building) {
        List<BlockPos> positions = switch (building.buildingType()) {
            case WAREHOUSE -> WarehouseLayout.activeStoragePositions(building);
            case CART_STATION -> CartStationLayout.activeFreightPositions(building);
            default -> List.of();
        };
        if (positions.isEmpty()) return "저장 없음";
        int used = 0;
        int total = 0;
        for (BlockPos pos : positions) {
            if (!level.hasChunkAt(pos)) return "저장 미로드";
            if (!(level.getBlockEntity(pos) instanceof Container container)) return "저장 복구 대기";
            total += container.getContainerSize();
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                if (!container.getItem(slot).isEmpty()) used++;
            }
        }
        return "사용 슬롯 " + used + "/" + total + (used >= total ? " · 포화" : "");
    }

    public static String upgradeHint(SettlementData data, BuildingRecord building) {
        int current = grade(building);
        if (current >= 3) return "최대 확장";
        int next = current + 1;
        if (next > maxGrade(data)) return "다음 " + gradeLabel(next) + " · " + requiredTier(next) + " 필요";
        UpgradeCost cost = costFor(building.buildingType(), next);
        return "다음 " + gradeLabel(next) + " · 목 " + cost.wood() + " / 돌 " + cost.stone()
                + " / 구리·철 " + cost.copperIronItems() + " · 빈손 웅크려 저장통 우클릭";
    }

    public static void ensureManagedStorage(ServerLevel level, SettlementData data) {
        for (BuildingRecord building : data.buildings()) {
            if (!isLogisticsBuilding(building.buildingType())) continue;
            for (BlockPos pos : activeStoragePositions(building)) {
                if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.BARREL)
                        && level.getBlockEntity(pos) instanceof Container) continue;
                if (!SettlementStorageService.canSafelyCreateManagedBarrel(level, pos)) continue;
                level.setBlock(pos, Blocks.BARREL.defaultBlockState(), 3);
            }
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()) return;
        if (!event.getItemStack().isEmpty()) return;
        if (!(event.getLevel() instanceof ServerLevel level) || !level.getBlockState(event.getPos()).is(Blocks.BARREL)) return;
        MinecraftServer server = level.getServer();
        if (level != server.overworld()) return;
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return;

        BuildingRecord building = logisticsBuildingAt(data, event.getPos());
        if (building == null) return;
        if (building.upgradeGrade() <= 0) {
            BuildingRecord migrated = building.withUpgradeGrade(1);
            if (data.replaceCompletedBuilding(building, migrated)) building = migrated;
        }

        int current = grade(building);
        if (current >= 3) {
            player.sendSystemMessage(Component.literal("§6[마을] §f이 물류시설은 이미 최대 확장 III입니다."));
            finish(event); return;
        }
        int next = current + 1;
        if (next > maxGrade(data)) {
            player.sendSystemMessage(Component.literal("§6[마을] §f물류 확장 " + gradeLabel(next)
                    + "은(는) " + requiredTier(next) + " 단계에서 열립니다."));
            finish(event); return;
        }
        if (!canProvision(level, building, next)) {
            player.sendSystemMessage(Component.literal("§6[마을] §f추가 물류 저장통 자리가 막혀 있습니다. 시설 내부 확장 공간을 비워 주세요."));
            finish(event); return;
        }
        if (!SettlementStorageService.storageAvailable(level, data)) {
            player.sendSystemMessage(Component.literal("§6[마을] §f공동 저장소가 모두 로드되어야 물류시설을 확장할 수 있습니다."));
            finish(event); return;
        }

        UpgradeCost cost = costFor(building.buildingType(), next);
        SettlementResources resources = SettlementStorageService.scan(level, data);
        long commonMetal = SettlementStorageService.countCommonUpgradeMetal(level, data);
        if (resources.wood() < cost.wood() || resources.stone() < cost.stone()
                || commonMetal < cost.copperIronItems()) {
            player.sendSystemMessage(Component.literal("§6[마을] §f물류 확장 재료 부족 · 필요 목재 " + cost.wood()
                    + " / 석재 " + cost.stone() + " / 구리·철 " + cost.copperIronItems()
                    + " · 현재 목 " + resources.wood() + " / 돌 " + resources.stone()
                    + " / 구리·철 " + Math.max(0L, commonMetal)));
            finish(event); return;
        }

        if (!SettlementStorageService.consumeLogisticsUpgrade(level, data,
                cost.wood(), cost.stone(), cost.copperIronItems())) {
            player.sendSystemMessage(Component.literal("§6[마을] §f물류 확장 재료를 안전하게 인출하지 못했습니다. 자원 상태를 다시 확인해 주세요."));
            finish(event); return;
        }
        BuildingRecord upgraded = building.withUpgradeGrade(next);
        if (!data.replaceCompletedBuilding(building, upgraded)) {
            throw new IllegalStateException("Logistics building disappeared during same-thread upgrade transaction");
        }
        ensureManagedStorage(level, data);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        String effect = upgraded.buildingType() == BuildingType.WAREHOUSE
                ? "중앙 저장 " + warehouseStorageCount(upgraded) + "통"
                : "화물 저장 " + cartFreightStorageCount(upgraded) + "통 · 생산 운송 "
                        + SettlementOutpostLogisticsService.productiveTransportBatchSize(data) + "개/회";
        player.sendSystemMessage(Component.literal("§6[마을] §f" + upgraded.buildingType().displayName()
                + " 물류 " + gradeLabel(next) + " 완료 · " + effect));
        finish(event);
    }

    private static boolean canProvision(ServerLevel level, BuildingRecord building, int grade) {
        BuildingRecord preview = building.withUpgradeGrade(grade);
        for (BlockPos pos : activeStoragePositions(preview)) {
            if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.BARREL)
                    && level.getBlockEntity(pos) instanceof Container) continue;
            if (!SettlementStorageService.canSafelyCreateManagedBarrel(level, pos)) return false;
        }
        return true;
    }

    private static List<BlockPos> activeStoragePositions(BuildingRecord building) {
        return switch (building.buildingType()) {
            case WAREHOUSE -> WarehouseLayout.activeStoragePositions(building);
            case CART_STATION -> CartStationLayout.activeFreightPositions(building);
            default -> List.of();
        };
    }

    private static BuildingRecord logisticsBuildingAt(SettlementData data, BlockPos clicked) {
        for (BuildingRecord building : data.buildings()) {
            if (!isLogisticsBuilding(building.buildingType())) continue;
            List<BlockPos> base = building.buildingType() == BuildingType.WAREHOUSE
                    ? WarehouseLayout.storagePositions(building)
                    : CartStationLayout.freightPositions(building);
            if (base.contains(clicked)) return building;
        }
        return null;
    }

    private static String requiredTier(int grade) {
        return grade <= 2 ? SettlementTier.FRONTIER_TOWN.displayName() : SettlementTier.DOMAIN.displayName();
    }

    private static void finish(PlayerInteractEvent.RightClickBlock event) {
        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
        event.setCanceled(true);
    }
}
