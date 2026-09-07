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
 * Player-directed RTS investment for physical settlement defense infrastructure.
 *
 * Grades never mint soldiers, weapons or abstract defense points. The upgrade payment is removed
 * atomically from the same loaded physical settlement storage used by construction. Existing
 * barracks/outpost/guard services remain the only entity and combat authorities.
 */
public final class SettlementMilitaryUpgradeService {
    private SettlementMilitaryUpgradeService() {}

    public record UpgradeCost(long wood, long stone, long copperIronItems) {}

    public static void tick(MinecraftServer server, SettlementData data) {
        if (server.getTickCount() % 20 != 0) return;
        if (!migrateLegacyGrades(data)) return;
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
    }

    /** Pre-Alpha.124 military buildings start at grade I; no existing defense is downgraded. */
    static boolean migrateLegacyGrades(SettlementData data) {
        boolean changed = false;
        for (BuildingRecord building : List.copyOf(data.buildings())) {
            if (!isMilitaryBuilding(building.buildingType()) || building.upgradeGrade() > 0) continue;
            changed |= data.replaceCompletedBuilding(building, building.withUpgradeGrade(1));
        }
        return changed;
    }

    public static boolean isMilitaryBuilding(BuildingType type) {
        return type == BuildingType.GUARD_POST || type == BuildingType.WATCHTOWER
                || type == BuildingType.BARRACKS || type == BuildingType.CITADEL;
    }

    public static int grade(BuildingRecord building) {
        if (building == null || !isMilitaryBuilding(building.buildingType())) return 1;
        return Math.max(1, Math.min(4, building.upgradeGrade()));
    }

    public static int bestGrade(SettlementData data, BuildingType type) {
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

    public static UpgradeCost costForGrade(int grade) {
        return switch (Math.max(2, Math.min(4, grade))) {
            case 2 -> new UpgradeCost(256L, 192L, 32L);
            case 3 -> new UpgradeCost(512L, 384L, 96L);
            default -> new UpgradeCost(1024L, 768L, 192L);
        };
    }

    public static int barracksSlots(BuildingRecord barracks) {
        return 2 + grade(barracks); // I/II/III/IV = 3/4/5/6 real supplied soldiers.
    }

    public static int barracksPatrolRadius(SettlementData data, BuildingRecord barracks) {
        int local = 24 + (grade(barracks) - 1) * 2;
        int citadel = citadelGrade(data);
        return local + (citadel <= 0 ? 0 : 8 + (citadel - 1) * 2);
    }

    public static double barracksThreatRadiusBonus(SettlementData data, BuildingRecord barracks) {
        double local = (grade(barracks) - 1) * 4.0D;
        int citadel = citadelGrade(data);
        return local + (citadel <= 0 ? 0.0D : 12.0D + (citadel - 1) * 4.0D);
    }

    public static int barracksRecoveryHeal(BuildingRecord barracks) {
        return 10 + (grade(barracks) - 1) * 2;
    }

    public static double guardHomeRadius(BuildingRecord post) {
        return 24.0D + (grade(post) - 1) * 4.0D;
    }

    public static double watchAlertRadius(SettlementData data, BuildingRecord tower) {
        double local = 40.0D + (grade(tower) - 1) * 8.0D;
        return local + citadelWatchBonus(data);
    }

    public static double citadelWatchBonus(SettlementData data) {
        int citadel = citadelGrade(data);
        return citadel <= 0 ? 0.0D : 16.0D + (citadel - 1) * 4.0D;
    }

    public static int remoteFoodReserve(SettlementData data) {
        int citadel = citadelGrade(data);
        return 12 + citadel * 6;
    }

    public static int remoteMetalReserve(SettlementData data) {
        int citadel = citadelGrade(data);
        return 4 + citadel * 2;
    }

    public static int remotePatrolRadius(SettlementData data) {
        return 24 + citadelGrade(data) * 2;
    }

    public static int remoteRecoveryHeal(SettlementData data) {
        return 10 + citadelGrade(data) * 2;
    }

    public static String upgradeHint(SettlementData data, BuildingRecord building) {
        int current = grade(building);
        if (current >= 4) return "최대 군사 개량 IV";
        int next = current + 1;
        int ceiling = maxGrade(data);
        if (next > ceiling) return "다음 군사 개량 " + roman(next) + " · " + requiredTier(next) + " 필요";
        UpgradeCost cost = costForGrade(next);
        return "다음 군사 개량 " + roman(next) + " · 목 " + cost.wood() + " / 돌 " + cost.stone()
                + " / 구리·철 " + cost.copperIronItems() + " · 빈손 웅크려 지휘 지점 우클릭";
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

        BuildingRecord building = militaryBuildingAt(data, event.getPos());
        if (building == null) return;
        if (building.upgradeGrade() <= 0) {
            BuildingRecord migrated = building.withUpgradeGrade(1);
            if (data.replaceCompletedBuilding(building, migrated)) building = migrated;
        }

        int current = grade(building);
        if (current >= 4) {
            player.sendSystemMessage(Component.literal("§6[마을] §f이 방어시설은 이미 최대 군사 개량 IV입니다."));
            finish(event); return;
        }
        int next = current + 1;
        if (next > maxGrade(data)) {
            player.sendSystemMessage(Component.literal("§6[마을] §f군사 개량 " + roman(next) + "은(는) "
                    + requiredTier(next) + " 단계에서 열립니다."));
            finish(event); return;
        }
        if (!SettlementStorageService.storageAvailable(level, data)) {
            player.sendSystemMessage(Component.literal("§6[마을] §f공동 저장소가 모두 로드되어야 군사 개량을 진행할 수 있습니다."));
            finish(event); return;
        }

        UpgradeCost cost = costForGrade(next);
        SettlementResources resources = SettlementStorageService.scan(level, data);
        long commonMetal = SettlementStorageService.countCommonUpgradeMetal(level, data);
        if (resources.wood() < cost.wood() || resources.stone() < cost.stone() || commonMetal < cost.copperIronItems()) {
            player.sendSystemMessage(Component.literal("§6[마을] §f군사 개량 재료 부족 · 필요 목재 " + cost.wood()
                    + " / 석재 " + cost.stone() + " / 구리·철 " + cost.copperIronItems()
                    + " · 현재 목 " + resources.wood() + " / 돌 " + resources.stone()
                    + " / 구리·철 " + Math.max(0L, commonMetal)));
            finish(event); return;
        }
        if (!SettlementStorageService.consumeLogisticsUpgrade(level, data,
                cost.wood(), cost.stone(), cost.copperIronItems())) {
            player.sendSystemMessage(Component.literal("§6[마을] §f군사 개량 재료를 안전하게 인출하지 못했습니다. 자원 상태를 다시 확인해 주세요."));
            finish(event); return;
        }

        BuildingRecord upgraded = building.withUpgradeGrade(next);
        if (!data.replaceCompletedBuilding(building, upgraded)) {
            throw new IllegalStateException("Military building disappeared during same-thread upgrade transaction");
        }
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        player.sendSystemMessage(Component.literal("§6[마을] §f" + upgraded.buildingType().displayName()
                + " 군사 개량 " + roman(next) + " 완료 · " + effectSummary(data, upgraded)));
        finish(event);
    }

    private static BuildingRecord militaryBuildingAt(SettlementData data, BlockPos clicked) {
        for (BuildingRecord building : data.buildings()) {
            if (!isMilitaryBuilding(building.buildingType())) continue;
            BlockPos anchor = switch (building.buildingType()) {
                case GUARD_POST -> building.localToWorld(4, 1, 4);      // service barrel
                case WATCHTOWER -> building.localToWorld(3, 10, 1);    // tower bell
                case BARRACKS -> building.localToWorld(7, 1, 9);       // barracks bell
                case CITADEL -> building.localToWorld(8, 1, 7);        // command dais
                default -> null;
            };
            if (clicked.equals(anchor)) return building;
        }
        return null;
    }

    private static String effectSummary(SettlementData data, BuildingRecord building) {
        return switch (building.buildingType()) {
            case GUARD_POST -> "경비 활동 반경 " + (int) guardHomeRadius(building);
            case WATCHTOWER -> "감시 반경 " + (int) watchAlertRadius(data, building);
            case BARRACKS -> "주둔 슬롯 " + barracksSlots(building) + " · 순찰 반경 " + barracksPatrolRadius(data, building);
            case CITADEL -> "전초 군수 목표 식량 " + remoteFoodReserve(data) + " / 금속 " + remoteMetalReserve(data);
            default -> "방어 효율 상승";
        };
    }

    private static int citadelGrade(SettlementData data) {
        return bestGrade(data, BuildingType.CITADEL);
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
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; default -> "IV";
        };
    }

    private static void finish(PlayerInteractEvent.RightClickBlock event) {
        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
        event.setCanceled(true);
    }
}
