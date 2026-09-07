package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

/** Player-directed RTS investment for physical town production buildings. */
public final class SettlementProductionUpgradeService {
    private SettlementProductionUpgradeService() {}
    public record UpgradeCost(long wood, long stone, long copperIronItems) {}

    public static void tick(MinecraftServer server, SettlementData data) {
        if (server.getTickCount() % 20 != 0) return;
        if (!migrateLegacyGrades(data)) return;
        SettlementStorageService.ensureManagedStorage(server.overworld(), data);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
    }

    /** Pre-Alpha.122 buildings inherit exactly the free tier-derived grade they already had, once. */
    static boolean migrateLegacyGrades(SettlementData data) {
        int inherited = SettlementProductionEfficiencyService.maxGrade(data);
        boolean changed = false;
        for (BuildingRecord building : List.copyOf(data.buildings())) {
            if (!SettlementProductionEfficiencyService.isProductionBuilding(building.buildingType())
                    || building.upgradeGrade() > 0) continue;
            changed |= data.replaceCompletedBuilding(building, building.withUpgradeGrade(inherited));
        }
        return changed;
    }

    public static UpgradeCost costForGrade(int grade) {
        return switch (Math.max(2, Math.min(4, grade))) {
            case 2 -> new UpgradeCost(128L, 96L, 0L);
            case 3 -> new UpgradeCost(256L, 192L, 32L);
            default -> new UpgradeCost(512L, 384L, 96L);
        };
    }

    public static String upgradeHint(SettlementData data, BuildingRecord building) {
        int current = SettlementProductionEfficiencyService.grade(data, building);
        if (current >= 4) return "최대 개량";
        int next = current + 1;
        int ceiling = SettlementProductionEfficiencyService.maxGrade(data);
        if (current >= ceiling) return "다음 " + SettlementProductionEfficiencyService.gradeLabel(next) + " · " + requiredTier(next) + " 필요";
        UpgradeCost cost = costForGrade(next);
        return "다음 " + SettlementProductionEfficiencyService.gradeLabel(next) + " · 목 " + cost.wood()
                + " / 돌 " + cost.stone() + (cost.copperIronItems() > 0 ? " / 구리·철 " + cost.copperIronItems() : "")
                + " · 빈손 웅크려 현장 저장통 우클릭";
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

        BuildingRecord building = productionBuildingAt(data, event.getPos());
        if (building == null) return;
        if (building.upgradeGrade() <= 0) {
            BuildingRecord migrated = building.withUpgradeGrade(SettlementProductionEfficiencyService.maxGrade(data));
            if (data.replaceCompletedBuilding(building, migrated)) building = migrated;
        }

        int current = SettlementProductionEfficiencyService.grade(data, building);
        if (current >= 4) {
            player.sendSystemMessage(Component.literal("§6[마을] §f이 생산시설은 이미 최대 개량 IV입니다."));
            finish(event); return;
        }
        int next = current + 1;
        int ceiling = SettlementProductionEfficiencyService.maxGrade(data);
        if (next > ceiling) {
            player.sendSystemMessage(Component.literal("§6[마을] §f개량 " + SettlementProductionEfficiencyService.gradeLabel(next)
                    + "은(는) " + requiredTier(next) + " 단계에서 열립니다."));
            finish(event); return;
        }
        if (!SettlementStorageService.canProvisionWorksiteBuffers(level, building, next)) {
            player.sendSystemMessage(Component.literal("§6[마을] §f추가 현장 저장통 자리가 막혀 있습니다. 기존 저장통 위 공간을 비워 주세요."));
            finish(event); return;
        }
        if (!SettlementStorageService.storageAvailable(level, data)) {
            player.sendSystemMessage(Component.literal("§6[마을] §f공동 저장소가 모두 로드되어야 시설 개량을 진행할 수 있습니다."));
            finish(event); return;
        }

        UpgradeCost cost = costForGrade(next);
        SettlementResources resources = SettlementStorageService.scan(level, data);
        long copperIron = SettlementStorageService.countProductionUpgradeMetal(level, data);
        if (resources.wood() < cost.wood() || resources.stone() < cost.stone() || copperIron < cost.copperIronItems()) {
            player.sendSystemMessage(Component.literal("§6[마을] §f시설 개량 재료 부족 · 필요 목재 " + cost.wood()
                    + " / 석재 " + cost.stone() + (cost.copperIronItems() > 0 ? " / 구리·철 " + cost.copperIronItems() : "")
                    + " · 현재 목 " + resources.wood() + " / 돌 " + resources.stone()
                    + (cost.copperIronItems() > 0 ? " / 구리·철 " + Math.max(0L, copperIron) : "")));
            finish(event); return;
        }

        if (!SettlementStorageService.consumeProductionUpgrade(level, data,
                cost.wood(), cost.stone(), cost.copperIronItems())) {
            player.sendSystemMessage(Component.literal("§6[마을] §f시설 개량 재료를 안전하게 인출하지 못했습니다. 자원 상태를 다시 확인해 주세요."));
            finish(event); return;
        }
        BuildingRecord upgraded = building.withUpgradeGrade(next);
        if (!data.replaceCompletedBuilding(building, upgraded)) {
            throw new IllegalStateException("Production building disappeared during same-thread upgrade transaction");
        }
        SettlementStorageService.ensureManagedStorage(level, data);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        player.sendSystemMessage(Component.literal("§6[마을] §f" + upgraded.buildingType().displayName() + " 개량 "
                + SettlementProductionEfficiencyService.gradeLabel(next) + " 완료 · 생산성과 현장 저장 여유가 상승했습니다."));
        finish(event);
    }

    private static BuildingRecord productionBuildingAt(SettlementData data, BlockPos clicked) {
        for (BuildingRecord building : data.buildings()) {
            if (!SettlementProductionEfficiencyService.isProductionBuilding(building.buildingType())) continue;
            for (BlockPos storage : SettlementStorageService.worksiteStoragePositions(building)) {
                if (storage.equals(clicked)) return building;
            }
        }
        return null;
    }

    private static String requiredTier(int grade) {
        return switch (grade) {
            case 2 -> SettlementTier.VILLAGE.displayName();
            case 3 -> SettlementTier.FRONTIER_TOWN.displayName();
            default -> SettlementTier.DOMAIN.displayName();
        };
    }

    private static void finish(PlayerInteractEvent.RightClickBlock event) {
        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
        event.setCanceled(true);
    }
}
