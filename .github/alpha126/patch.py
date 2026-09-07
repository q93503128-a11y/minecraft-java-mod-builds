#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
P = ROOT / "projects/frontier-settlement"

def read(rel):
    return (P / rel).read_text(encoding="utf-8")

def write(rel, text):
    path = P / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")

def replace_once(rel, old, new):
    text = read(rel)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{rel}: expected one anchor, found {count}: {old[:120]!r}")
    write(rel, text.replace(old, new, 1))

def append_once(rel, marker, block):
    text = read(rel)
    if marker in text:
        return
    if not text.endswith("\n"):
        text += "\n"
    write(rel, text + "\n" + block.strip() + "\n")

replace_once("gradle.properties", "mod_version=0.1.0-alpha.125", "mod_version=0.1.0-alpha.126")
replace_once("README.md", "## Current version: 0.1.0-alpha.125", "## Current version: 0.1.0-alpha.126")
replace_once("COMPANION_LOCK.json", '"frontier_settlement": "0.1.0-alpha.125"', '"frontier_settlement": "0.1.0-alpha.126"')
replace_once("companion-testpack/resolved-lock.client.json", '"frontier_settlement": "0.1.0-alpha.125"', '"frontier_settlement": "0.1.0-alpha.126"')
replace_once("companion-testpack/resolved-lock.server.json", '"frontier_settlement": "0.1.0-alpha.125"', '"frontier_settlement": "0.1.0-alpha.126"')
replace_once("COMPANION_LOCK.json",
'''    "Frontier Alpha.125 changes only the project-owned Frontier runtime JAR and presentation: RTS operations summary, bottleneck/upgrade backlog visibility, and corrected per-building upgrade UI. Third-party companion pins/hashes remain unchanged."''',
'''    "Frontier Alpha.125 changes only the project-owned Frontier runtime JAR and presentation: RTS operations summary, bottleneck/upgrade backlog visibility, and corrected per-building upgrade UI. Third-party companion pins/hashes remain unchanged.",
    "Frontier Alpha.126 changes only the project-owned Frontier runtime JAR and presentation: paid civic/trade city investment grades, stronger physical late-game resource sinks, grade-scaled administration/market benefits, and progression-guidance alignment. Third-party companion pins/hashes remain unchanged."''')

write("src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementCityInvestmentService.java", 'package kr.moonseungjun.frontiersettlement.settlement;\n\nimport net.minecraft.core.BlockPos;\nimport net.minecraft.network.chat.Component;\nimport net.minecraft.server.MinecraftServer;\nimport net.minecraft.server.level.ServerLevel;\nimport net.minecraft.server.level.ServerPlayer;\nimport net.minecraft.world.InteractionHand;\nimport net.minecraft.world.InteractionResult;\nimport net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;\n\nimport java.util.List;\n\n/**\n * Paid late-game administration/trade investment using the existing persisted building grade.\n *\n * This is deliberately not a new currency, tax, population ledger or logistics authority. Every\n * upgrade removes real wood/stone/common copper-or-iron ItemStacks from the already loaded shared\n * settlement storage. Civic/trade grades are optional power investments and never gate an existing\n * save out of a settlement tier it already reached.\n */\npublic final class SettlementCityInvestmentService {\n    private SettlementCityInvestmentService() {}\n\n    public record UpgradeCost(long wood, long stone, long copperIronItems) {}\n\n    public static void tick(MinecraftServer server, SettlementData data) {\n        if (server.getTickCount() % 20 != 0) return;\n        if (!migrateLegacyGrades(data)) return;\n        SettlementService.refreshResources(server, data);\n        SettlementService.broadcast(server, data);\n    }\n\n    static boolean migrateLegacyGrades(SettlementData data) {\n        boolean changed = false;\n        for (BuildingRecord building : List.copyOf(data.buildings())) {\n            if (!isCityBuilding(building.buildingType()) || building.upgradeGrade() > 0) continue;\n            changed |= data.replaceCompletedBuilding(building, building.withUpgradeGrade(1));\n        }\n        return changed;\n    }\n\n    public static boolean isCityBuilding(BuildingType type) {\n        return type == BuildingType.CIVIC_HALL || type == BuildingType.TRADE_HALL;\n    }\n\n    public static int grade(BuildingRecord building) {\n        if (building == null || !isCityBuilding(building.buildingType())) return 0;\n        return Math.max(1, Math.min(4, building.upgradeGrade()));\n    }\n\n    public static int bestGrade(SettlementData data, BuildingType type) {\n        if (!isCityBuilding(type)) return 0;\n        int best = 0;\n        for (BuildingRecord building : data.buildings()) {\n            if (building.buildingType() == type) best = Math.max(best, grade(building));\n        }\n        return best;\n    }\n\n    public static int maxGrade(SettlementData data) {\n        return switch (SettlementTier.current(data)) {\n            case CAMP, HAMLET, VILLAGE -> 1;\n            case FRONTIER_TOWN -> 2;\n            case DOMAIN -> 3;\n            case FRONTIER_CAPITAL -> 4;\n        };\n    }\n\n    public static UpgradeCost costFor(BuildingType type, int grade) {\n        int normalized = Math.max(2, Math.min(4, grade));\n        if (type == BuildingType.CIVIC_HALL) {\n            return switch (normalized) {\n                case 2 -> new UpgradeCost(320L, 256L, 48L);\n                case 3 -> new UpgradeCost(640L, 512L, 128L);\n                default -> new UpgradeCost(1024L, 768L, 256L);\n            };\n        }\n        if (type == BuildingType.TRADE_HALL) {\n            return switch (normalized) {\n                case 2 -> new UpgradeCost(384L, 288L, 64L);\n                case 3 -> new UpgradeCost(768L, 576L, 160L);\n                default -> new UpgradeCost(1280L, 896L, 320L);\n            };\n        }\n        return new UpgradeCost(0L, 0L, 0L);\n    }\n\n    /** Existing grade-I civic behavior is preserved exactly for old saves. */\n    public static int workerAttractionIntervalTicks(SettlementData data) {\n        return switch (bestGrade(data, BuildingType.CIVIC_HALL)) {\n            case 1 -> 400;\n            case 2 -> 320;\n            case 3 -> 260;\n            case 4 -> 200;\n            default -> 600;\n        };\n    }\n\n    /** Existing grade-I +2 builder bonus is preserved exactly for old saves. */\n    public static int civicHallBuilderBonus(SettlementData data) {\n        return switch (bestGrade(data, BuildingType.CIVIC_HALL)) {\n            case 1 -> 2;\n            case 2 -> 3;\n            case 3 -> 4;\n            case 4 -> 5;\n            default -> 0;\n        };\n    }\n\n    /** Replaces only the old flat Trade Hall +4 bonus; all exploration/network bonuses remain. */\n    public static int tradeHallMarketBonus(SettlementData data) {\n        return switch (bestGrade(data, BuildingType.TRADE_HALL)) {\n            case 1 -> 4;\n            case 2 -> 6;\n            case 3 -> 8;\n            case 4 -> 10;\n            default -> 0;\n        };\n    }\n\n    public static String upgradeHint(SettlementData data, BuildingRecord building) {\n        int current = grade(building);\n        if (current >= 4) return "최대 도시 투자 IV";\n        int next = current + 1;\n        if (next > maxGrade(data)) return "다음 도시 투자 " + roman(next) + " · " + requiredTier(next) + " 필요";\n        UpgradeCost cost = costFor(building.buildingType(), next);\n        String anchor = building.buildingType() == BuildingType.CIVIC_HALL ? "강단" : "종";\n        return "다음 도시 투자 " + roman(next) + " · 목 " + cost.wood() + " / 돌 " + cost.stone()\n                + " / 구리·철 " + cost.copperIronItems() + " · 빈손 웅크려 " + anchor + " 우클릭";\n    }\n\n    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {\n        if (event.getHand() != InteractionHand.MAIN_HAND) return;\n        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()) return;\n        if (!event.getItemStack().isEmpty()) return;\n        if (!(event.getLevel() instanceof ServerLevel level)) return;\n        MinecraftServer server = level.getServer();\n        if (level != server.overworld()) return;\n\n        SettlementData data = SettlementData.get(server);\n        if (!data.founded()) return;\n        BuildingRecord building = cityBuildingAt(data, event.getPos());\n        if (building == null) return;\n\n        if (building.upgradeGrade() <= 0) {\n            BuildingRecord migrated = building.withUpgradeGrade(1);\n            if (data.replaceCompletedBuilding(building, migrated)) building = migrated;\n        }\n\n        int current = grade(building);\n        if (current >= 4) {\n            player.sendSystemMessage(Component.literal("§6[마을] §f이 도시 핵심시설은 이미 최대 투자 IV입니다."));\n            finish(event);\n            return;\n        }\n\n        int next = current + 1;\n        if (next > maxGrade(data)) {\n            player.sendSystemMessage(Component.literal("§6[마을] §f도시 투자 " + roman(next)\n                    + "은(는) " + requiredTier(next) + " 단계에서 열립니다."));\n            finish(event);\n            return;\n        }\n\n        if (!SettlementStorageService.storageAvailable(level, data)) {\n            player.sendSystemMessage(Component.literal("§6[마을] §f공동 저장소가 모두 로드되어야 도시 투자를 진행할 수 있습니다."));\n            finish(event);\n            return;\n        }\n\n        UpgradeCost cost = costFor(building.buildingType(), next);\n        SettlementResources resources = SettlementStorageService.scan(level, data);\n        long commonMetal = SettlementStorageService.countCommonUpgradeMetal(level, data);\n        if (resources.wood() < cost.wood() || resources.stone() < cost.stone()\n                || commonMetal < cost.copperIronItems()) {\n            player.sendSystemMessage(Component.literal("§6[마을] §f도시 투자 재료 부족 · 필요 목재 " + cost.wood()\n                    + " / 석재 " + cost.stone() + " / 구리·철 " + cost.copperIronItems()\n                    + " · 현재 목 " + resources.wood() + " / 돌 " + resources.stone()\n                    + " / 구리·철 " + Math.max(0L, commonMetal)));\n            finish(event);\n            return;\n        }\n\n        if (!SettlementStorageService.consumeLogisticsUpgrade(level, data,\n                cost.wood(), cost.stone(), cost.copperIronItems())) {\n            player.sendSystemMessage(Component.literal("§6[마을] §f도시 투자 재료를 안전하게 인출하지 못했습니다. 자원 상태를 다시 확인해 주세요."));\n            finish(event);\n            return;\n        }\n\n        BuildingRecord upgraded = building.withUpgradeGrade(next);\n        if (!data.replaceCompletedBuilding(building, upgraded)) {\n            throw new IllegalStateException("City building disappeared during same-thread investment transaction");\n        }\n        SettlementService.refreshResources(server, data);\n        SettlementService.broadcast(server, data);\n        player.sendSystemMessage(Component.literal("§6[마을] §f" + upgraded.buildingType().displayName()\n                + " 도시 투자 " + roman(next) + " 완료 · " + effectSummary(data, upgraded)));\n        finish(event);\n    }\n\n    private static BuildingRecord cityBuildingAt(SettlementData data, BlockPos clicked) {\n        for (BuildingRecord building : data.buildings()) {\n            if (!isCityBuilding(building.buildingType())) continue;\n            BlockPos anchor = building.localToWorld(7, 2, 6);\n            if (anchor.equals(clicked)) return building;\n        }\n        return null;\n    }\n\n    public static String civicEffectSummary(SettlementData data) {\n        int grade = bestGrade(data, BuildingType.CIVIC_HALL);\n        if (grade <= 0) return "시민 행정 없음";\n        return "주민 유입 " + (workerAttractionIntervalTicks(data) / 20) + "초 · 건설 인력 +"\n                + civicHallBuilderBonus(data);\n    }\n\n    public static String tradeEffectSummary(SettlementData data) {\n        int grade = bestGrade(data, BuildingType.TRADE_HALL);\n        if (grade <= 0) return "교역 행정 없음";\n        return "유물 교역 보너스 +" + tradeHallMarketBonus(data);\n    }\n\n    private static String effectSummary(SettlementData data, BuildingRecord building) {\n        return building.buildingType() == BuildingType.CIVIC_HALL\n                ? civicEffectSummary(data) : tradeEffectSummary(data);\n    }\n\n    private static String requiredTier(int grade) {\n        return switch (grade) {\n            case 2 -> SettlementTier.FRONTIER_TOWN.displayName();\n            case 3 -> SettlementTier.DOMAIN.displayName();\n            default -> SettlementTier.FRONTIER_CAPITAL.displayName();\n        };\n    }\n\n    public static String roman(int grade) {\n        return switch (Math.max(1, Math.min(4, grade))) {\n            case 1 -> "I";\n            case 2 -> "II";\n            case 3 -> "III";\n            default -> "IV";\n        };\n    }\n\n    private static void finish(PlayerInteractEvent.RightClickBlock event) {\n        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);\n        event.setCanceled(true);\n    }\n}\n')

replace_once("src/main/java/kr/moonseungjun/frontiersettlement/FrontierSettlement.java",
'''import kr.moonseungjun.frontiersettlement.settlement.SettlementCivilWorkService;
import kr.moonseungjun.frontiersettlement.settlement.SettlementConstructionOfficeService;''',
'''import kr.moonseungjun.frontiersettlement.settlement.SettlementCivilWorkService;
import kr.moonseungjun.frontiersettlement.settlement.SettlementCityInvestmentService;
import kr.moonseungjun.frontiersettlement.settlement.SettlementConstructionOfficeService;''')
replace_once("src/main/java/kr/moonseungjun/frontiersettlement/FrontierSettlement.java",
'''        NeoForge.EVENT_BUS.addListener(SettlementLogisticsUpgradeService::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(SettlementMilitaryUpgradeService::onRightClickBlock);''',
'''        NeoForge.EVENT_BUS.addListener(SettlementLogisticsUpgradeService::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(SettlementCityInvestmentService::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(SettlementMilitaryUpgradeService::onRightClickBlock);''')
replace_once("src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementService.java",
'''        SettlementProductionUpgradeService.tick(server, data);
        SettlementLogisticsUpgradeService.tick(server, data);
        SettlementMilitaryUpgradeService.tick(server, data);''',
'''        SettlementProductionUpgradeService.tick(server, data);
        SettlementLogisticsUpgradeService.tick(server, data);
        SettlementCityInvestmentService.tick(server, data);
        SettlementMilitaryUpgradeService.tick(server, data);''')

replace_once("src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java",
'''    private static final int OUTPOST_BUILDER_BONUS_CAP = 6;
    private static final int CIVIC_HALL_BUILDER_BONUS = 2;
    private static final int MAX_BUILDER_CREW = 14;''',
'''    private static final int OUTPOST_BUILDER_BONUS_CAP = 6;
    private static final int MAX_BUILDER_CREW = 14;''')
replace_once("src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java",
'''        int civicHallBonus = data.buildingCount(BuildingType.CIVIC_HALL) > 0 ? CIVIC_HALL_BUILDER_BONUS : 0;''',
'''        int civicHallBonus = SettlementCityInvestmentService.civicHallBuilderBonus(data);''')

replace_once("src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementWorkerService.java",
'''    private static final int BASE_WORKER_ATTRACTION_INTERVAL_TICKS = 600;
    private static final int CIVIC_HALL_WORKER_ATTRACTION_INTERVAL_TICKS = 400;''',
'''    // Civic administration cadence is owned by SettlementCityInvestmentService so one grade
    // controls both the UI promise and the actual vacancy-attraction scheduler.''')
replace_once("src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementWorkerService.java",
'''    public static int workerAttractionIntervalTicks(SettlementData data) {
        return data.buildingCount(BuildingType.CIVIC_HALL) > 0
                ? CIVIC_HALL_WORKER_ATTRACTION_INTERVAL_TICKS
                : BASE_WORKER_ATTRACTION_INTERVAL_TICKS;
    }''',
'''    public static int workerAttractionIntervalTicks(SettlementData data) {
        return SettlementCityInvestmentService.workerAttractionIntervalTicks(data);
    }''')

replace_once("src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementExplorationBenefitService.java",
'''    public static final int MARKET_EMERALD_BONUS_PER_NETWORK_LEVEL = 1;
    public static final int MARKET_EMERALD_BONUS_TRADE_HALL = 4;''',
'''    public static final int MARKET_EMERALD_BONUS_PER_NETWORK_LEVEL = 1;''')
replace_once("src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementExplorationBenefitService.java",
'''                + territoryNetworkLevel(data) * MARKET_EMERALD_BONUS_PER_NETWORK_LEVEL
                + (data.buildingCount(BuildingType.TRADE_HALL) > 0 ? MARKET_EMERALD_BONUS_TRADE_HALL : 0);''',
'''                + territoryNetworkLevel(data) * MARKET_EMERALD_BONUS_PER_NETWORK_LEVEL
                + SettlementCityInvestmentService.tradeHallMarketBonus(data);''')

replace_once("src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementContextService.java",
'''            case CIVIC_HALL -> "완공 · 시민 중심 · 주거 +" + type.housingGain() + " · 주민 유입 20초 · 건설 인력 +2";
            case TRADE_HALL -> "완공 · 유물 교역 보너스 +4 · 주거 +" + type.housingGain();''',
'''            case CIVIC_HALL -> "완공 · 도시 " + SettlementCityInvestmentService.roman(SettlementCityInvestmentService.grade(building))
                    + " · 시민 중심 · 주거 +" + type.housingGain() + " · "
                    + SettlementCityInvestmentService.civicEffectSummary(data) + " · "
                    + SettlementCityInvestmentService.upgradeHint(data, building);
            case TRADE_HALL -> "완공 · 도시 " + SettlementCityInvestmentService.roman(SettlementCityInvestmentService.grade(building))
                    + " · " + SettlementCityInvestmentService.tradeEffectSummary(data) + " · 주거 +" + type.housingGain()
                    + " · " + SettlementCityInvestmentService.upgradeHint(data, building);''')

replace_once("src/main/java/kr/moonseungjun/frontiersettlement/client/BuildingPaletteScreen.java",
'''    private static String specialEffect(SettlementSnapshotPayload data, BuildingType type) {
        if (type == BuildingType.CIVIC_HALL) return "주민 유입 20초 · 건설 인력 +2";
        if (type == BuildingType.LUMBER_CAMP || type == BuildingType.FARM''',
'''    private static String specialEffect(SettlementSnapshotPayload data, BuildingType type) {
        if (type == BuildingType.CIVIC_HALL) {
            int ceiling = cityCeiling(data.tier());
            return "신규 도시 I · 주민 유입 20초 · 건설 인력 +2 · 현재 상한 " + gradeLabel(ceiling) + " · 완공 후 강단에서 수동 투자";
        }
        if (type == BuildingType.TRADE_HALL) {
            int ceiling = cityCeiling(data.tier());
            return "신규 도시 I · 유물 교역 보너스 +4 · 현재 상한 " + gradeLabel(ceiling) + " · 완공 후 종에서 수동 투자";
        }
        if (type == BuildingType.LUMBER_CAMP || type == BuildingType.FARM''')
replace_once("src/main/java/kr/moonseungjun/frontiersettlement/client/BuildingPaletteScreen.java",
'''    private static int militaryCeiling(String tier) {
        return switch (tier) {
            case "개척 도시" -> 2;
            case "영지" -> 3;
            case "개척 수도" -> 4;
            default -> 1;
        };
    }

    private static String gradeLabel(int grade) {''',
'''    private static int militaryCeiling(String tier) {
        return switch (tier) {
            case "개척 도시" -> 2;
            case "영지" -> 3;
            case "개척 수도" -> 4;
            default -> 1;
        };
    }

    private static int cityCeiling(String tier) {
        return militaryCeiling(tier);
    }

    private static String gradeLabel(int grade) {''')

replace_once("src/main/java/kr/moonseungjun/frontiersettlement/client/SettlementOperationsSummary.java",
'''        int citadels,
        int militaryUpgradeBacklog,
        int outposts,''',
'''        int citadels,
        int militaryUpgradeBacklog,
        int cityInvestmentBacklog,
        int outposts,''')
replace_once("src/main/java/kr/moonseungjun/frontiersettlement/client/SettlementOperationsSummary.java",
'''        int citadels = 0;
        int militaryUpgradeBacklog = 0;

        int productionCeiling = productionCeiling(snapshot.tier());''',
'''        int citadels = 0;
        int militaryUpgradeBacklog = 0;
        int cityInvestmentBacklog = 0;

        int productionCeiling = productionCeiling(snapshot.tier());''')
replace_once("src/main/java/kr/moonseungjun/frontiersettlement/client/SettlementOperationsSummary.java",
'''        int logisticsCeiling = logisticsCeiling(snapshot.tier());
        int militaryCeiling = militaryCeiling(snapshot.tier());''',
'''        int logisticsCeiling = logisticsCeiling(snapshot.tier());
        int militaryCeiling = militaryCeiling(snapshot.tier());
        int cityCeiling = militaryCeiling;''')
replace_once("src/main/java/kr/moonseungjun/frontiersettlement/client/SettlementOperationsSummary.java",
'''            if (type == BuildingType.GUARD_POST || type == BuildingType.WATCHTOWER
                    || type == BuildingType.BARRACKS || type == BuildingType.CITADEL) {''',
'''            if (type == BuildingType.CIVIC_HALL || type == BuildingType.TRADE_HALL) {
                int grade = gradeAfter(target.detail(), "도시 ");
                if (grade > 0 && grade < cityCeiling) cityInvestmentBacklog++;
                continue;
            }

            if (type == BuildingType.GUARD_POST || type == BuildingType.WATCHTOWER
                    || type == BuildingType.BARRACKS || type == BuildingType.CITADEL) {''')
replace_once("src/main/java/kr/moonseungjun/frontiersettlement/client/SettlementOperationsSummary.java",
'''        if (logisticsUpgradeBacklog > 0) alerts.add("현재 등급에서 물류시설 " + logisticsUpgradeBacklog + "곳을 더 확장할 수 있습니다.");
        if (militaryUpgradeBacklog > 0) alerts.add("현재 등급에서 방어시설 " + militaryUpgradeBacklog + "곳을 더 개량할 수 있습니다.");''',
'''        if (logisticsUpgradeBacklog > 0) alerts.add("현재 등급에서 물류시설 " + logisticsUpgradeBacklog + "곳을 더 확장할 수 있습니다.");
        if (cityInvestmentBacklog > 0) alerts.add("현재 등급에서 도시 핵심시설 " + cityInvestmentBacklog + "곳에 추가 투자할 수 있습니다.");
        if (militaryUpgradeBacklog > 0) alerts.add("현재 등급에서 방어시설 " + militaryUpgradeBacklog + "곳을 더 개량할 수 있습니다.");''')
replace_once("src/main/java/kr/moonseungjun/frontiersettlement/client/SettlementOperationsSummary.java",
'''        } else if (logisticsUpgradeBacklog > 0) {
            priority = "창고·수레 정거장 확장";
        } else if (militaryUpgradeBacklog > 0) {''',
'''        } else if (logisticsUpgradeBacklog > 0) {
            priority = "창고·수레 정거장 확장";
        } else if (cityInvestmentBacklog > 0) {
            priority = "시민회관·교역회관 도시 투자";
        } else if (militaryUpgradeBacklog > 0) {''')
replace_once("src/main/java/kr/moonseungjun/frontiersettlement/client/SettlementOperationsSummary.java",
'''                guardPosts, watchtowers, barracks, citadels, militaryUpgradeBacklog,
                snapshot.context().outpostCount(), priority, alerts);''',
'''                guardPosts, watchtowers, barracks, citadels, militaryUpgradeBacklog, cityInvestmentBacklog,
                snapshot.context().outpostCount(), priority, alerts);''')

replace_once("src/main/java/kr/moonseungjun/frontiersettlement/client/SettlementOperationsScreen.java",
'''                "인구 " + snapshot.population() + " / 주거 " + summary.housingCapacity(),
                "건물 " + snapshot.context().buildingCount() + " · 전초 " + summary.outposts(),
                projectLine(snapshot)));''',
'''                "인구 " + snapshot.population() + " / 주거 " + summary.housingCapacity(),
                "건물 " + snapshot.context().buildingCount() + " · 전초 " + summary.outposts()
                        + " · 도시 투자 " + summary.cityInvestmentBacklog(),
                projectLine(snapshot)));''')

replace_once("src/main/java/kr/moonseungjun/frontiersettlement/client/SettlementGuideScreen.java",
'''            default -> draw(g, x, y, "5. 영지와 개척 수도",
                    "개척 도시부터 시민회관, 영지부터 교역회관·성채가 열립니다.",
                    "교역회관은 교역 가치를 높이고 성채는 영지 감시망을 넓힙니다.",
                    "인구·전초·도로·탐험·랜드마크 조건을 함께 달성해야 합니다.",
                    "조건을 모두 채우면 최종 단계 ‘개척 수도’가 완성됩니다.");''',
'''            default -> draw(g, x, y, "5. 영지와 개척 수도",
                    "개척 도시부터 시민회관, 영지부터 교역회관·성채가 열립니다.",
                    "시민회관 강단·교역회관 종을 빈손 웅크려 우클릭하면 도시 투자를 진행합니다.",
                    "도시 투자는 실물 목재·돌·구리/철을 크게 소비해 행정·교역 효율을 높입니다.",
                    "등급 승격과 도시는 별개입니다. 조건을 채우면 개척 수도가 완성됩니다.");''')

replace_once("src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementGuidanceService.java",
'''        if (data.population() < 4) return populationGoal(data, 4);
        if (data.buildingCount(BuildingType.MARKET) < 1) return buildingGoal(data, BuildingType.MARKET);
        if (data.buildingCount(BuildingType.CART_STATION) < 1) return buildingGoal(data, BuildingType.CART_STATION);
        if (data.buildingCount(BuildingType.CONSTRUCTION_OFFICE) < 1) return buildingGoal(data, BuildingType.CONSTRUCTION_OFFICE);
        if (data.buildingCount(BuildingType.MINE) < 1) return buildingGoal(data, BuildingType.MINE);''',
'''        if (data.population() < 4) return populationGoal(data, 4);
        if (data.buildingCount(BuildingType.MINE) < 1) return buildingGoal(data, BuildingType.MINE);
        if (data.buildingCount(BuildingType.MARKET) < 1) return buildingGoal(data, BuildingType.MARKET);
        if (data.buildingCount(BuildingType.CONSTRUCTION_OFFICE) < 1) return buildingGoal(data, BuildingType.CONSTRUCTION_OFFICE);
        if (data.buildingCount(BuildingType.CART_STATION) < 1) return buildingGoal(data, BuildingType.CART_STATION);''')
replace_once("src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementGuidanceService.java",
'''        if (data.buildingCount(BuildingType.BLACKSMITH) < 1) return buildingGoal(data, BuildingType.BLACKSMITH);
        if (data.buildingCount(BuildingType.WORKSHOP) < 1) return buildingGoal(data, BuildingType.WORKSHOP);
        if (data.buildingCount(BuildingType.FARM) < 2) return "다음 목표 · 농장 2곳으로 식량 기반 확대";
        if (data.buildingCount(BuildingType.CIVIC_HALL) < 1) {''',
'''        if (data.buildingCount(BuildingType.BLACKSMITH) < 1) return buildingGoal(data, BuildingType.BLACKSMITH);
        if (data.buildingCount(BuildingType.WORKSHOP) < 1) return buildingGoal(data, BuildingType.WORKSHOP);
        if (data.buildingCount(BuildingType.CIVIC_HALL) < 1) {''')

replace_once("tools/test_current_source.py",
'''require("mod_version=0.1.0-alpha.125" in gradle, "current verifier/version drift")''',
'''require("mod_version=0.1.0-alpha.126" in gradle, "current verifier/version drift")''')
replace_once("tools/test_current_source.py",
'''require("MAX_BUILDER_CREW = 14" in construction and "CIVIC_HALL_BUILDER_BONUS = 2" in construction,
        "civic-hall bounded construction crew bonus missing")''',
'''require("MAX_BUILDER_CREW = 14" in construction
        and "SettlementCityInvestmentService.civicHallBuilderBonus(data)" in construction,
        "grade-scaled civic-hall bounded construction crew bonus missing")''')
replace_once("tools/test_current_source.py",
'''require("BASE_WORKER_ATTRACTION_INTERVAL_TICKS = 600" in worker
        and "CIVIC_HALL_WORKER_ATTRACTION_INTERVAL_TICKS = 400" in worker
        and "workerAttractionIntervalTicks(data)" in worker,
        "civic-hall civilian attraction cadence missing")''',
'''require("SettlementCityInvestmentService.workerAttractionIntervalTicks(data)" in worker,
        "city-investment civilian attraction cadence missing")''')
replace_once("tools/test_current_source.py",
'''require("productionUpgradeBacklog" in operations_summary and "logisticsUpgradeBacklog" in operations_summary
        and "militaryUpgradeBacklog" in operations_summary, "RTS paid-upgrade backlog visibility missing")''',
'''require("productionUpgradeBacklog" in operations_summary and "logisticsUpgradeBacklog" in operations_summary
        and "militaryUpgradeBacklog" in operations_summary and "cityInvestmentBacklog" in operations_summary,
        "RTS paid-upgrade backlog visibility missing")''')
replace_once("tools/test_current_source.py",
'''require("신규 개량 I" in palette_screen and "신규 물류 I" in palette_screen and "신규 군사 I" in palette_screen,
        "construction palette returned to misleading free tier-derived facility grades")''',
'''require("신규 개량 I" in palette_screen and "신규 물류 I" in palette_screen and "신규 군사 I" in palette_screen
        and "신규 도시 I" in palette_screen,
        "construction palette returned to misleading free tier-derived facility grades")''')
append_once("tools/test_current_source.py", "# Alpha.126 city investment.", r'''
# Alpha.126 city investment.
city_investment = text(SETTLEMENT / "SettlementCityInvestmentService.java")
require("new UpgradeCost(320L, 256L, 48L)" in city_investment
        and "new UpgradeCost(640L, 512L, 128L)" in city_investment
        and "new UpgradeCost(1024L, 768L, 256L)" in city_investment,
        "civic-hall city investment ladder drifted")
require("new UpgradeCost(384L, 288L, 64L)" in city_investment
        and "new UpgradeCost(768L, 576L, 160L)" in city_investment
        and "new UpgradeCost(1280L, 896L, 320L)" in city_investment,
        "trade-hall city investment ladder drifted")
require("case 1 -> 400" in city_investment and "case 4 -> 200" in city_investment
        and "case 1 -> 2" in city_investment and "case 4 -> 5" in city_investment,
        "civic administration grade benefits drifted")
require("case 1 -> 4" in city_investment and "case 4 -> 10" in city_investment,
        "trade-hall market grade benefits drifted")
require("countCommonUpgradeMetal" in city_investment and "consumeLogisticsUpgrade" in city_investment,
        "city investment bypasses physical common-metal payment")
require("player.isShiftKeyDown()" in city_investment and "event.getItemStack().isEmpty()" in city_investment
        and "localToWorld(7, 2, 6)" in city_investment,
        "landmark-local city investment interaction missing")
require("SettlementCityInvestmentService.tick(server, data)" in service,
        "city grade migration is not wired into settlement runtime")
require("SettlementCityInvestmentService::onRightClickBlock" in entry,
        "city investment interaction is not registered")
require("SettlementCityInvestmentService.tradeHallMarketBonus(data)" in text(SETTLEMENT / "SettlementExplorationBenefitService.java"),
        "trade hall still uses a flat presence-only market bonus")
require("농장 2곳으로 식량 기반 확대" not in guidance,
        "guidance still incorrectly forces two farms despite farm+warehouse satisfying mature food base")
''')

append_once("README.md", "## Alpha.126 city investment", r'''
## Alpha.126 city investment

- Civic hall and trade hall now use paid city-investment grades I-IV without adding a currency, tax, happiness meter or second settlement ledger.
- Every newly completed landmark begins at city I. Grade II unlocks at Frontier Town, III at Domain and IV at Frontier Capital; settlement tier remains an unlock ceiling, not a free global upgrade.
- Civic hall II/III/IV cost 320/256/48, 640/512/128 and 1024/768/256 wood/stone/common copper-or-iron items.
- Trade hall II/III/IV cost 384/288/64, 768/576/160 and 1280/896/320 wood/stone/common copper-or-iron items.
- Civic administration preserves the existing grade-I 20-second civilian attraction cadence and +2 builder bonus, then scales to 16s/+3, 13s/+4 and 10s/+5.
- Trade administration preserves the existing grade-I relic-market +4 bonus, then scales to +6/+8/+10. Exploration, conquest and territory-network bonuses still stack through their existing capped authorities.
- Investments are explicit local interactions: empty-hand sneak-right-click the civic-hall lectern or trade-hall bell. All storage must be loaded and the shared physical ItemStacks are removed before the grade commit.
- City grades are optional power/resource sinks and do not become new Frontier Capital prerequisites, so existing saves are never downgraded by this pass.
- The M operations snapshot now reports available city-investment backlog without launching another server scan.
- Progression guidance now places the mine at its real Village-era availability and no longer forces a second farm when farm + warehouse already satisfies the mature-food predicate.
''')
append_once("CANONICAL_PLAN.md", "### Alpha.126 — city-scale physical investment", r'''
### Alpha.126 — city-scale physical investment

The late game must consume the abundance created by physical production instead of solving abundance by silently throttling workers. Civic Hall and Trade Hall therefore become repeatable I-IV investment anchors using their existing persisted `BuildingRecord.upgradeGrade`. The city grade is not a currency or a new tier: Frontier still has one physical ItemStack economy and the settlement tier only unlocks the maximum purchasable grade.

Canonical costs are intentionally city-scale. Civic II/III/IV consumes 320/256/48, 640/512/128 and 1024/768/256 wood/stone/common copper-or-iron items; Trade II/III/IV consumes 384/288/64, 768/576/160 and 1280/896/320. Gold, diamond and arbitrary high-value modded metals cannot silently satisfy these item-count payments.

Civic grade I preserves the existing 20-second civilian vacancy-attraction cadence and +2 builder contribution. II/III/IV improve that to 16s/+3, 13s/+4 and 10s/+5 while the global builder hard cap remains 14. Trade grade I preserves the existing +4 relic-market bonus and II/III/IV raise only that component to +6/+8/+10; exploration/conquest/territory-network bonuses keep their own existing caps. No grade mints residents, emeralds, cargo or resources.

The interaction remains physical and local: empty-hand sneak-right-click the Civic Hall lectern or Trade Hall bell while shared storage is fully loaded. The entire wood/stone/common-metal payment is checked and removed through the existing settlement storage authority before the grade is committed. City grades do not become Frontier Capital requirements, preventing an existing save from being demoted by a new optional investment system.

UI is observational: server context publishes the persisted city grade and next cost, and the Alpha.125 operations summary derives the city-investment backlog from that already-synchronized context instead of starting new entity or world scans.
''')
replace_once("PROJECT.md",
'''Current implementation delta: **0.1.0-alpha.79**. The large historical canonical/gap documents remain the original scope ledger; this file records the newer bounded-NPC and exploration/outpost gameplay direction until the next consolidated documentation pass.''',
'''Current implementation delta: **0.1.0-alpha.126**. The large historical canonical/gap documents remain the original scope ledger; this file records the current bounded-NPC, physical RTS-economy and exploration/outpost direction.''')
append_once("PROJECT.md", "### Alpha.126 city investment direction", r'''
### Alpha.126 city investment direction

Late-game abundance is answered with explicit, repeatable physical investment rather than production nerfs or abstract currencies. Civic Hall and Trade Hall each persist a paid I-IV city grade; the settlement tier only unlocks the ceiling. Civic investment improves bounded resident attraction and builder capacity, Trade investment improves only the existing relic-market component, and every upgrade consumes real loaded wood/stone/common copper-or-iron ItemStacks. City grades are optional and do not gate Frontier Capital, preserving old-save tier stability.
''')

print("Alpha.126 city investment patch applied")
