package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

/**
 * Paid late-game administration/trade investment using the existing persisted building grade.
 *
 * This is deliberately not a new currency, tax, population ledger or logistics authority. Every
 * upgrade removes real wood/stone/common copper-or-iron ItemStacks from the already loaded shared
 * settlement storage. Civic/trade grades are optional power investments and never gate an existing
 * save out of a settlement tier it already reached.
 */
public final class SettlementCityInvestmentService {
    private SettlementCityInvestmentService() {}

    public record UpgradeCost(long wood, long stone, long copperIronItems) {}

    public static void tick(MinecraftServer server, SettlementData data) {
        if (server.getTickCount() % 20 != 0) return;
        if (!migrateLegacyGrades(data)) return;
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
    }

    static boolean migrateLegacyGrades(SettlementData data) {
        boolean changed = false;
        for (BuildingRecord building : List.copyOf(data.buildings())) {
            if (!isCityBuilding(building.buildingType()) || building.upgradeGrade() > 0) continue;
            changed |= data.replaceCompletedBuilding(building, building.withUpgradeGrade(1));
        }
        return changed;
    }

    public static boolean isCityBuilding(BuildingType type) {
        return type == BuildingType.CIVIC_HALL || type == BuildingType.TRADE_HALL;
    }

    public static int grade(BuildingRecord building) {
        if (building == null || !isCityBuilding(building.buildingType())) return 0;
        return Math.max(1, Math.min(4, building.upgradeGrade()));
    }

    public static int bestGrade(SettlementData data, BuildingType type) {
        if (!isCityBuilding(type)) return 0;
        int best = 0;
        for (BuildingRecord building : data.buildings()) {
            if (building.buildingType() == type) best = Math.max(best, grade(building));
        }
        return best;
    }

    public static int maxGrade(SettlementData data) {
        return switch (SettlementTier.current(data)) {
            case CAMP, HAMLET, VILLAGE -> 1;
            case FRONTIER_TOWN -> 2;
            case DOMAIN -> 3;
            case FRONTIER_CAPITAL -> 4;
        };
    }

    public static UpgradeCost costFor(BuildingType type, int grade) {
        int normalized = Math.max(2, Math.min(4, grade));
        if (type == BuildingType.CIVIC_HALL) {
            return switch (normalized) {
                case 2 -> new UpgradeCost(320L, 256L, 48L);
                case 3 -> new UpgradeCost(640L, 512L, 128L);
                default -> new UpgradeCost(1024L, 768L, 256L);
            };
        }
        if (type == BuildingType.TRADE_HALL) {
            return switch (normalized) {
                case 2 -> new UpgradeCost(384L, 288L, 64L);
                case 3 -> new UpgradeCost(768L, 576L, 160L);
                default -> new UpgradeCost(1280L, 896L, 320L);
            };
        }
        return new UpgradeCost(0L, 0L, 0L);
    }

    /** Existing grade-I civic behavior is preserved exactly for old saves. */
    public static int workerAttractionIntervalTicks(SettlementData data) {
        return switch (bestGrade(data, BuildingType.CIVIC_HALL)) {
            case 1 -> 400;
            case 2 -> 320;
            case 3 -> 260;
            case 4 -> 200;
            default -> 600;
        };
    }

    /** Existing grade-I +2 builder bonus is preserved exactly for old saves. */
    public static int civicHallBuilderBonus(SettlementData data) {
        return switch (bestGrade(data, BuildingType.CIVIC_HALL)) {
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 4;
            case 4 -> 5;
            default -> 0;
        };
    }

    /** Replaces only the old flat Trade Hall +4 bonus; all exploration/network bonuses remain. */
    public static int tradeHallMarketBonus(SettlementData data) {
        return switch (bestGrade(data, BuildingType.TRADE_HALL)) {
            case 1 -> 4;
            case 2 -> 6;
            case 3 -> 8;
            case 4 -> 10;
            default -> 0;
        };
    }

    public static String upgradeHint(SettlementData data, BuildingRecord building) {
        int current = grade(building);
        if (current >= 4) return "최대 도시 투자 IV";
        int next = current + 1;
        if (next > maxGrade(data)) return "다음 도시 투자 " + roman(next) + " · " + requiredTier(next) + " 필요";
        UpgradeCost cost = costFor(building.buildingType(), next);
        String anchor = building.buildingType() == BuildingType.CIVIC_HALL ? "강단" : "종";
        return "다음 도시 투자 " + roman(next) + " · 목 " + cost.wood() + " / 돌 " + cost.stone()
                + " / 구리·철 " + cost.copperIronItems() + " · 빈손 웅크려 " + anchor + " 우클릭";
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()) return;
        if (!event.getItemStack().isEmpty()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (level != server.overworld()) return;

        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return;
        BuildingRecord building = cityBuildingAt(data, event.getPos());
        if (building == null) return;

        if (building.upgradeGrade() <= 0) {
            BuildingRecord migrated = building.withUpgradeGrade(1);
            if (data.replaceCompletedBuilding(building, migrated)) building = migrated;
        }

        int current = grade(building);
        if (current >= 4) {
            player.sendSystemMessage(Component.literal("§6[마을] §f이 도시 핵심시설은 이미 최대 투자 IV입니다."));
            finish(event);
            return;
        }

        int next = current + 1;
        if (next > maxGrade(data)) {
            player.sendSystemMessage(Component.literal("§6[마을] §f도시 투자 " + roman(next)
                    + "은(는) " + requiredTier(next) + " 단계에서 열립니다."));
            finish(event);
            return;
        }

        if (!SettlementStorageService.storageAvailable(level, data)) {
            player.sendSystemMessage(Component.literal("§6[마을] §f공동 저장소가 모두 로드되어야 도시 투자를 진행할 수 있습니다."));
            finish(event);
            return;
        }

        UpgradeCost cost = costFor(building.buildingType(), next);
        SettlementResources resources = SettlementStorageService.scan(level, data);
        long commonMetal = SettlementStorageService.countCommonUpgradeMetal(level, data);
        if (resources.wood() < cost.wood() || resources.stone() < cost.stone()
                || commonMetal < cost.copperIronItems()) {
            player.sendSystemMessage(Component.literal("§6[마을] §f도시 투자 재료 부족 · 필요 목재 " + cost.wood()
                    + " / 석재 " + cost.stone() + " / 구리·철 " + cost.copperIronItems()
                    + " · 현재 목 " + resources.wood() + " / 돌 " + resources.stone()
                    + " / 구리·철 " + Math.max(0L, commonMetal)));
            finish(event);
            return;
        }

        if (!SettlementStorageService.consumeLogisticsUpgrade(level, data,
                cost.wood(), cost.stone(), cost.copperIronItems())) {
            player.sendSystemMessage(Component.literal("§6[마을] §f도시 투자 재료를 안전하게 인출하지 못했습니다. 자원 상태를 다시 확인해 주세요."));
            finish(event);
            return;
        }

        BuildingRecord upgraded = building.withUpgradeGrade(next);
        if (!data.replaceCompletedBuilding(building, upgraded)) {
            throw new IllegalStateException("City building disappeared during same-thread investment transaction");
        }
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        player.sendSystemMessage(Component.literal("§6[마을] §f" + upgraded.buildingType().displayName()
                + " 도시 투자 " + roman(next) + " 완료 · " + effectSummary(data, upgraded)));
        finish(event);
    }

    private static BuildingRecord cityBuildingAt(SettlementData data, BlockPos clicked) {
        for (BuildingRecord building : data.buildings()) {
            if (!isCityBuilding(building.buildingType())) continue;
            BlockPos anchor = building.localToWorld(7, 2, 6);
            if (anchor.equals(clicked)) return building;
        }
        return null;
    }

    public static String civicEffectSummary(SettlementData data) {
        int grade = bestGrade(data, BuildingType.CIVIC_HALL);
        if (grade <= 0) return "시민 행정 없음";
        return "주민 유입 " + (workerAttractionIntervalTicks(data) / 20) + "초 · 건설 인력 +"
                + civicHallBuilderBonus(data);
    }

    public static String tradeEffectSummary(SettlementData data) {
        int grade = bestGrade(data, BuildingType.TRADE_HALL);
        if (grade <= 0) return "교역 행정 없음";
        return "유물 교역 보너스 +" + tradeHallMarketBonus(data);
    }

    private static String effectSummary(SettlementData data, BuildingRecord building) {
        return building.buildingType() == BuildingType.CIVIC_HALL
                ? civicEffectSummary(data) : tradeEffectSummary(data);
    }

    private static String requiredTier(int grade) {
        return switch (grade) {
            case 2 -> SettlementTier.FRONTIER_TOWN.displayName();
            case 3 -> SettlementTier.DOMAIN.displayName();
            default -> SettlementTier.FRONTIER_CAPITAL.displayName();
        };
    }

    public static String roman(int grade) {
        return switch (Math.max(1, Math.min(4, grade))) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "IV";
        };
    }

    private static void finish(PlayerInteractEvent.RightClickBlock event) {
        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
        event.setCanceled(true);
    }
}
