from pathlib import Path
import json

ROOT = Path('projects/frontier-settlement')
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
SETTLEMENT = JAVA / 'settlement'


def read(path):
    return path.read_text(encoding='utf-8')


def write(path, text):
    path.write_text(text, encoding='utf-8')


def replace_once(path, old, new, label):
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected 1 match, found {count}')
    write(path, text.replace(old, new, 1))


military_service = r'''package kr.moonseungjun.frontiersettlement.settlement;

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
'''
write(SETTLEMENT / 'SettlementMilitaryUpgradeService.java', military_service)

# Settlement runtime migration + interaction registration.
service = SETTLEMENT / 'SettlementService.java'
replace_once(service,
    '        SettlementLogisticsUpgradeService.tick(server, data);\n',
    '        SettlementLogisticsUpgradeService.tick(server, data);\n        SettlementMilitaryUpgradeService.tick(server, data);\n',
    'service military tick')

entry = JAVA / 'FrontierSettlement.java'
replace_once(entry,
    'import kr.moonseungjun.frontiersettlement.settlement.SettlementMilitaryOutpostService;\n',
    'import kr.moonseungjun.frontiersettlement.settlement.SettlementMilitaryOutpostService;\nimport kr.moonseungjun.frontiersettlement.settlement.SettlementMilitaryUpgradeService;\n',
    'entry military import')
replace_once(entry,
    '        NeoForge.EVENT_BUS.addListener(SettlementLogisticsUpgradeService::onRightClickBlock);\n',
    '        NeoForge.EVENT_BUS.addListener(SettlementLogisticsUpgradeService::onRightClickBlock);\n        NeoForge.EVENT_BUS.addListener(SettlementMilitaryUpgradeService::onRightClickBlock);\n',
    'entry military interaction')
replace_once(entry,
    '    // Alpha.123 canonical/integrated CI retrigger after central logistics investment.\n',
    '    // Alpha.124 military/territory investment: paid defense grades and physical post-combat resupply.\n',
    'entry alpha marker')

# Ordinary military rations/common metal are a separate payment authority from high-value resources.
inv = SETTLEMENT / 'SettlementInventory.java'
replace_once(inv,
    '    public static boolean consumeMetalAndFood(Container container, long metal, long food) {\n',
    '''    public static long countCommonMilitaryMetal(Container container) {\n        return count(container, SettlementEquipmentUpgradeService::isBlacksmithMetal);\n    }\n\n    public static long countMilitaryFood(Container container) {\n        return countValue(container, SettlementInventory::militaryFoodValue);\n    }\n\n    /** Combat recovery never silently spends gold/diamond or golden apples. */\n    public static boolean consumeCommonMilitarySupply(Container container, long commonMetalItems, long foodValue) {\n        if (commonMetalItems < 0L || foodValue < 0L) return false;\n        if (countCommonMilitaryMetal(container) < commonMetalItems || countMilitaryFood(container) < foodValue) return false;\n        consumeMatching(container, commonMetalItems, SettlementEquipmentUpgradeService::isBlacksmithMetal);\n        consumeValue(container, foodValue, SettlementInventory::militaryFoodValue);\n        container.setChanged();\n        return true;\n    }\n\n    public static boolean consumeMetalAndFood(Container container, long metal, long food) {\n''', 'inventory common military supply')
replace_once(inv,
    '    public static int foodValue(ItemStack stack) {\n        return resourceMask(stack) == RESOURCE_FOOD ? rawFoodUnitValue(stack) : 0;\n    }\n',
    '''    public static int foodValue(ItemStack stack) {\n        return resourceMask(stack) == RESOURCE_FOOD ? rawFoodUnitValue(stack) : 0;\n    }\n\n    private static int militaryFoodValue(ItemStack stack) {\n        if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return 0;\n        return foodValue(stack);\n    }\n''', 'inventory military ration value')

# Physical post-combat recovery is owned by the existing armory/storage walk path.
armory = SETTLEMENT / 'SettlementMilitaryArmoryService.java'
replace_once(armory,
    '    public static final double ARMORY_WALK_SPEED = 0.95D;\n',
    '    public static final double ARMORY_WALK_SPEED = 0.95D;\n    public static final long RECOVERY_FOOD_COST = 4L;\n    public static final long RECOVERY_METAL_COST = 1L;\n    private static final float RECOVERY_MISSING_HEALTH_THRESHOLD = 8.0F;\n',
    'armory recovery constants')
replace_once(armory,
    '    /**\n     * Local final leg for an already road-delivered outpost weapon.',
    '''    /** Walk to one real loaded settlement container and pay ordinary rations/common metal after combat. */\n    public static boolean tickRecovery(ServerLevel level, SettlementData data, BlockPos routeAnchor,\n                                       FrontierSoldierEntity soldier, int healAmount) {\n        if (!needsRecovery(soldier) || !SettlementStorageService.storageAvailable(level, data)) return false;\n        BlockPos source = nearestRecoverySource(level, data, routeAnchor, soldier);\n        if (source == null) return false;\n        double distance = soldier.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D);\n        if (distance > STORAGE_INTERACTION_RANGE_SQR) {\n            return SettlementWorkerStorageNavigation.moveToInteraction(\n                    level, soldier, source, ARMORY_WALK_SPEED, STORAGE_INTERACTION_RANGE_SQR);\n        }\n        if (!(level.getBlockEntity(source) instanceof Container container)\n                || !SettlementInventory.consumeCommonMilitarySupply(container, RECOVERY_METAL_COST, RECOVERY_FOOD_COST)) return false;\n        soldier.heal(Math.max(1, healAmount));\n        soldier.getNavigation().stop();\n        return true;\n    }\n\n    /** Local outpost recovery pays only from the road-delivered physical stockpile. */\n    public static boolean tickOutpostRecovery(ServerLevel level, OutpostRecord outpost,\n                                              FrontierSoldierEntity soldier, int healAmount) {\n        if (!needsRecovery(soldier)) return false;\n        BlockPos source = outpost.stockpile();\n        if (!level.hasChunkAt(source) || !(level.getBlockEntity(source) instanceof Container container)\n                || !containsRecoverySupply(container)) return false;\n        if (!SettlementWorkerStorageNavigation.canReachInteraction(level, soldier, source, STORAGE_INTERACTION_RANGE_SQR)) return false;\n        double distance = soldier.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D);\n        if (distance > STORAGE_INTERACTION_RANGE_SQR) {\n            return SettlementWorkerStorageNavigation.moveToInteraction(\n                    level, soldier, source, ARMORY_WALK_SPEED, STORAGE_INTERACTION_RANGE_SQR);\n        }\n        if (!SettlementInventory.consumeCommonMilitarySupply(container, RECOVERY_METAL_COST, RECOVERY_FOOD_COST)) return false;\n        soldier.heal(Math.max(1, healAmount));\n        soldier.getNavigation().stop();\n        return true;\n    }\n\n    private static boolean needsRecovery(FrontierSoldierEntity soldier) {\n        return soldier != null && soldier.isAlive()\n                && soldier.getHealth() <= soldier.getMaxHealth() - RECOVERY_MISSING_HEALTH_THRESHOLD;\n    }\n\n    private static BlockPos nearestRecoverySource(ServerLevel level, SettlementData data, BlockPos routeAnchor,\n                                                  FrontierSoldierEntity soldier) {\n        BlockPos best = null;\n        double bestDistance = MAX_ARMORY_ROUTE_SQR + 1.0D;\n        for (BlockPos pos : SettlementStorageService.storagePositions(data)) {\n            if (pos.distSqr(routeAnchor) > MAX_ARMORY_ROUTE_SQR || !level.hasChunkAt(pos)) continue;\n            if (!(level.getBlockEntity(pos) instanceof Container container) || !containsRecoverySupply(container)) continue;\n            if (!SettlementWorkerStorageNavigation.canReachInteraction(level, soldier, pos, STORAGE_INTERACTION_RANGE_SQR)) continue;\n            double distance = soldier.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);\n            if (distance <= MAX_ARMORY_ROUTE_SQR && distance < bestDistance) { bestDistance = distance; best = pos; }\n        }\n        return best;\n    }\n\n    private static boolean containsRecoverySupply(Container container) {\n        return SettlementInventory.countCommonMilitaryMetal(container) >= RECOVERY_METAL_COST\n                && SettlementInventory.countMilitaryFood(container) >= RECOVERY_FOOD_COST;\n    }\n\n    /**\n     * Local final leg for an already road-delivered outpost weapon.''', 'armory recovery methods')

# Barracks grades: real supplied slots plus stronger bounded local response.
barracks = SETTLEMENT / 'SettlementBarracksService.java'
text = read(barracks)
text = text.replace('    public static final int SOLDIERS_PER_BARRACKS = 3;\n', '    public static final int BASE_SOLDIERS_PER_BARRACKS = 3;\n')
if text.count('slot < SOLDIERS_PER_BARRACKS') != 4:
    raise SystemExit(f'barracks loops drifted: {text.count("slot < SOLDIERS_PER_BARRACKS")}')
text = text.replace('slot < SOLDIERS_PER_BARRACKS', 'slot < soldierSlots(barracks)')
text = text.replace('        return data.buildingCount(BuildingType.BARRACKS) * SOLDIERS_PER_BARRACKS;\n',
                    '        int total = 0;\n        for (BuildingRecord barracks : barracks(data)) total += soldierSlots(barracks);\n        return total;\n')
text = text.replace('        if (SettlementMilitaryArmoryService.tickArmament(level, data, barracks.workCenter(), soldier)) return;\n\n        double homeDistance',
                    '        if (SettlementMilitaryArmoryService.tickRecovery(level, data, barracks.workCenter(), soldier,\n                SettlementMilitaryUpgradeService.barracksRecoveryHeal(barracks))) return;\n        if (SettlementMilitaryArmoryService.tickArmament(level, data, barracks.workCenter(), soldier)) return;\n\n        double homeDistance')
text = text.replace('        int patrolRadius = data.buildingCount(BuildingType.CITADEL) > 0 ? CITADEL_PATROL_RADIUS : BASE_PATROL_RADIUS;\n',
                    '        int patrolRadius = SettlementMilitaryUpgradeService.barracksPatrolRadius(data, barracks);\n')
text = text.replace('        double threatRadius = SettlementExplorationBenefitService.barracksThreatRadius(level.getServer()) + (data.buildingCount(BuildingType.CITADEL) > 0 ? CITADEL_THREAT_RADIUS_BONUS : 0.0D);\n',
                    '        double threatRadius = SettlementExplorationBenefitService.barracksThreatRadius(level.getServer())\n                + SettlementMilitaryUpgradeService.barracksThreatRadiusBonus(data, barracks);\n')
old_home = '    private static BlockPos soldierHome(BuildingRecord barracks, int slot) { return barracks.localToWorld(5 + slot * 2, 1, 8); }\n'
new_home = '''    private static int soldierSlots(BuildingRecord barracks) {\n        return SettlementMilitaryUpgradeService.barracksSlots(barracks);\n    }\n\n    private static BlockPos soldierHome(BuildingRecord barracks, int slot) {\n        int x = switch (slot) {\n            case 0 -> 5; case 1 -> 7; case 2 -> 9;\n            case 3 -> 3; case 4 -> 11; default -> 13;\n        };\n        return barracks.localToWorld(x, 1, 8);\n    }\n'''
if old_home not in text: raise SystemExit('barracks soldier home drifted')
text = text.replace(old_home, new_home)
write(barracks, text)

# Civic guard/watch benefits read each building's paid grade; Citadel command grade stacks on watch range.
benefit = SETTLEMENT / 'SettlementBenefitService.java'
text = read(benefit)
text = text.replace('    private static final double WATCHTOWER_ALERT_RADIUS = 40.0D;\n    private static final double CITADEL_WATCH_RADIUS_BONUS = 16.0D;\n', '')
text = text.replace('    private static final double GUARD_POST_HOME_RADIUS_SQR = 24.0D * 24.0D;\n', '')
text = text.replace('                if (homeDistance > GUARD_POST_HOME_RADIUS_SQR) {',
                    '                double homeRadius = SettlementMilitaryUpgradeService.guardHomeRadius(post);\n                if (homeDistance > homeRadius * homeRadius) {')
text = text.replace('            Monster threat = nearestWatchThreat(level, home, data);',
                    '            Monster threat = nearestWatchThreat(level, home, data, tower);')
text = text.replace('    private static Monster nearestWatchThreat(ServerLevel level, BlockPos home, SettlementData data) {\n        double radius = WATCHTOWER_ALERT_RADIUS\n                + (data.buildingCount(BuildingType.CITADEL) > 0 ? CITADEL_WATCH_RADIUS_BONUS : 0.0D);',
                    '    private static Monster nearestWatchThreat(ServerLevel level, BlockPos home, SettlementData data, BuildingRecord tower) {\n        double radius = SettlementMilitaryUpgradeService.watchAlertRadius(data, tower);')
write(benefit, text)

# Dangerous remote territory consumes real road-delivered ordinary supplies only after combat damage.
outpost_mil = SETTLEMENT / 'SettlementMilitaryOutpostService.java'
text = read(outpost_mil)
text = text.replace('    private static final double LEASH_RADIUS_SQR = PATROL_RADIUS * PATROL_RADIUS;\n', '')
text = text.replace('                    if (!SettlementMilitaryArmoryService.tickOutpostArmament(level, outpost, sentry)) {',
                    '                    if (SettlementMilitaryArmoryService.tickOutpostRecovery(level, outpost, sentry,\n                            SettlementMilitaryUpgradeService.remoteRecoveryHeal(data))) continue;\n                    if (!SettlementMilitaryArmoryService.tickOutpostArmament(level, outpost, sentry)) {')
text = text.replace('            if (sentry != null && tick % PATROL_INTERVAL_TICKS == 0) patrol(level, outpost, sentry);',
                    '            if (sentry != null && tick % PATROL_INTERVAL_TICKS == 0) patrol(level, data, outpost, sentry);')
text = text.replace('        return Math.max(0, TARGET_FOOD_RESERVE - (int) Math.min(Integer.MAX_VALUE, SettlementInventory.countFood(container)));',
                    '        int target = SettlementMilitaryUpgradeService.remoteFoodReserve(SettlementData.get(level.getServer()));\n        return Math.max(0, target - (int) Math.min(Integer.MAX_VALUE, SettlementInventory.countFood(container)));')
text = text.replace('        return Math.max(0, TARGET_METAL_RESERVE - (int) Math.min(Integer.MAX_VALUE, present));',
                    '        int target = SettlementMilitaryUpgradeService.remoteMetalReserve(SettlementData.get(level.getServer()));\n        return Math.max(0, target - (int) Math.min(Integer.MAX_VALUE, present));')
text = text.replace('    private static void patrol(ServerLevel level, OutpostRecord outpost, FrontierSoldierEntity sentry) {\n        BlockPos home = outpost.center().above();\n        double homeDistance = sentry.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);\n        if (homeDistance > LEASH_RADIUS_SQR) {',
                    '    private static void patrol(ServerLevel level, SettlementData data, OutpostRecord outpost, FrontierSoldierEntity sentry) {\n        BlockPos home = outpost.center().above();\n        double homeDistance = sentry.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);\n        int patrolRadius = SettlementMilitaryUpgradeService.remotePatrolRadius(data);\n        if (homeDistance > (double) patrolRadius * patrolRadius) {')
text = text.replace('        if (SettlementMilitaryArmoryService.tickOutpostArmament(level, outpost, sentry)) return;\n        standDown(outpost, sentry);',
                    '        if (SettlementMilitaryArmoryService.tickOutpostRecovery(level, outpost, sentry,\n                SettlementMilitaryUpgradeService.remoteRecoveryHeal(data))) return;\n        if (SettlementMilitaryArmoryService.tickOutpostArmament(level, outpost, sentry)) return;\n        standDown(outpost, sentry);')
write(outpost_mil, text)

# Context UI exposes military investment instead of adding another screen.
context = SETTLEMENT / 'SettlementContextService.java'
text = read(context)
text = text.replace('            case GUARD_POST -> "완공 · 근거리 경비";\n            case WATCHTOWER -> "완공 · 로드 위협 대응";\n            case BARRACKS -> "완공 · 정식 주둔 3슬롯";\n',
'''            case GUARD_POST -> "완공 · 군사 " + SettlementMilitaryUpgradeService.roman(SettlementMilitaryUpgradeService.grade(building))\n                    + " · 경비 활동 반경 " + (int) SettlementMilitaryUpgradeService.guardHomeRadius(building) + " · "\n                    + SettlementMilitaryUpgradeService.upgradeHint(data, building);\n            case WATCHTOWER -> "완공 · 군사 " + SettlementMilitaryUpgradeService.roman(SettlementMilitaryUpgradeService.grade(building))\n                    + " · 감시 반경 " + (int) SettlementMilitaryUpgradeService.watchAlertRadius(data, building) + " · "\n                    + SettlementMilitaryUpgradeService.upgradeHint(data, building);\n            case BARRACKS -> "완공 · 군사 " + SettlementMilitaryUpgradeService.roman(SettlementMilitaryUpgradeService.grade(building))\n                    + " · 정식 주둔 " + SettlementMilitaryUpgradeService.barracksSlots(building) + "슬롯 · 순찰 "\n                    + SettlementMilitaryUpgradeService.barracksPatrolRadius(data, building) + " · "\n                    + SettlementMilitaryUpgradeService.upgradeHint(data, building);\n''')
text = text.replace('            case CITADEL -> "완공 · 감시망 반경 56 · 병영 감지 +12 · 순찰 반경 32 · 주거 +" + type.housingGain();',
'''            case CITADEL -> "완공 · 군사 " + SettlementMilitaryUpgradeService.roman(SettlementMilitaryUpgradeService.grade(building))\n                    + " · 감시 지휘 +" + (int) SettlementMilitaryUpgradeService.citadelWatchBonus(data)\n                    + " · 전초 군수 목표 식량 " + SettlementMilitaryUpgradeService.remoteFoodReserve(data)\n                    + " / 금속 " + SettlementMilitaryUpgradeService.remoteMetalReserve(data)\n                    + " · 주거 +" + type.housingGain() + " · " + SettlementMilitaryUpgradeService.upgradeHint(data, building);''')
write(context, text)

# Version + canonical docs.
props = ROOT / 'gradle.properties'
replace_once(props, 'mod_version=0.1.0-alpha.123', 'mod_version=0.1.0-alpha.124', 'gradle version')

readme = ROOT / 'README.md'
text = read(readme)
text = text.replace('## Current version: 0.1.0-alpha.123', '## Current version: 0.1.0-alpha.124', 1)
marker = '## Functional building families\n'
section = '''## Alpha.124 military and territory investment\n\n- Guard posts, watchtowers, barracks and the citadel now have paid military grades I-IV.\n- Grade II unlocks at Frontier Town, III at Domain and IV at Frontier Capital.\n- II/III/IV cost 256/192/32, 512/384/96 and 1024/768/192 wood/stone/common copper-or-iron items respectively.\n- Barracks scale from 3 to 6 physically recruited/supplied soldiers; added slots use real positions inside the barracks drill yard.\n- Watch and patrol ranges scale from the upgraded local building, while the citadel adds bounded settlement-wide command range.\n- Dangerous remote outposts request larger physical food/metal reserves as citadel command improves; the existing road transporter remains the only long-distance supply authority.\n- Wounded barracks soldiers and remote sentries recover only after immediate combat pressure clears. Each recovery batch consumes 4 ordinary food value and 1 copper/iron item from a real reachable storage container.\n- Recovery never silently spends gold, diamond, golden apples or enchanted golden apples.\n- There is no daily tax, virtual troop currency, free weapon minting, teleport logistics or chunk force-loading. Resource sink follows explicit investment and actual combat damage.\n\n'''
if marker not in text: raise SystemExit('README marker missing')
text = text.replace(marker, section + marker, 1)
write(readme, text)

canonical = ROOT / 'CANONICAL_PLAN.md'
with canonical.open('a', encoding='utf-8') as f:
    f.write('''\n\n## Alpha.124 RTS military / territory economy lock\n\nDefense growth is paid physical infrastructure, not an abstract troop-point layer. Guard post, watchtower, barracks and citadel grades use the shared BuildingRecord grade field and real settlement wood/stone/common copper-or-iron payment. The tier only unlocks the ceiling; it never grants the upgrade for free. Barracks remain the sole town-soldier authority and the existing outpost logistics worker remains the sole long-distance freight authority. Post-combat recovery may repeatedly consume ordinary physical food plus common copper/iron, but only after real damage and only from reachable loaded storage. Hidden daily upkeep, high-value implicit payment, virtual inventory, teleport freight and force-loading remain forbidden.\n''')

# Keep source audit authoritative for the new production-level feature.
test_source = ROOT / 'tools/test_current_source.py'
text = read(test_source).replace('mod_version=0.1.0-alpha.123', 'mod_version=0.1.0-alpha.124', 1)
insert = '''\nmilitary_upgrade = text(SETTLEMENT / "SettlementMilitaryUpgradeService.java")\nrequire("new UpgradeCost(256L, 192L, 32L)" in military_upgrade\n        and "new UpgradeCost(512L, 384L, 96L)" in military_upgrade\n        and "new UpgradeCost(1024L, 768L, 192L)" in military_upgrade,\n        "military RTS investment ladder drifted")\nrequire("barracksSlots" in military_upgrade and "return 2 + grade(barracks)" in military_upgrade,\n        "paid barracks capacity ladder missing")\nrequire("remoteFoodReserve" in military_upgrade and "remoteMetalReserve" in military_upgrade\n        and "remotePatrolRadius" in military_upgrade, "citadel territory-command benefits missing")\nrequire("SettlementMilitaryUpgradeService.tick(server, data)" in service, "military grade migration not wired")\nrequire("SettlementMilitaryUpgradeService::onRightClickBlock" in entry, "military improvement interaction not registered")\narmory = text(SETTLEMENT / "SettlementMilitaryArmoryService.java")\nrequire("tickRecovery" in armory and "tickOutpostRecovery" in armory\n        and "consumeCommonMilitarySupply" in armory, "physical post-combat recovery missing")\nrequire("countCommonMilitaryMetal" in inventory and "countMilitaryFood" in inventory\n        and "GOLDEN_APPLE" in inventory and "ENCHANTED_GOLDEN_APPLE" in inventory,\n        "military recovery can consume protected high-value supplies")\nrequire("soldierSlots(barracks)" in text(SETTLEMENT / "SettlementBarracksService.java")\n        and "case 3 -> 3; case 4 -> 11; default -> 13" in text(SETTLEMENT / "SettlementBarracksService.java"),\n        "expanded barracks slots are not bounded inside the drill yard")\n'''
anchor = 'guide = text(JAVA / "client/SettlementGuideScreen.java")\n'
if anchor not in text: raise SystemExit('test source insertion anchor missing')
text = text.replace(anchor, insert + '\n' + anchor, 1)
write(test_source, text)

# Companion runtime lock target follows the project-owned Frontier JAR only; third-party pins are unchanged.
for rel in ('COMPANION_LOCK.json', 'companion-testpack/resolved-lock.client.json', 'companion-testpack/resolved-lock.server.json'):
    path = ROOT / rel
    data = json.loads(read(path))
    old = data['target'].get('frontier_settlement')
    if old not in ('0.1.0-alpha.123', '0.1.0-alpha.124'):
        raise SystemExit(f'unexpected companion target {rel}: {old}')
    data['target']['frontier_settlement'] = '0.1.0-alpha.124'
    if rel == 'COMPANION_LOCK.json':
        note = ('Frontier Alpha.124 changes only the project-owned Frontier runtime JAR: paid military grades, '
                'bounded barracks/watch/citadel command growth and physical post-combat military resupply. '
                'Third-party companion pins/hashes remain unchanged.')
        if note not in data.setdefault('notes', []): data['notes'].append(note)
    write(path, json.dumps(data, ensure_ascii=False, indent=2) + '\n')

print('Alpha.124 patch applied')
