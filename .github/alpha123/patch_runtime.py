from pathlib import Path
import shutil

ROOT = Path("projects/frontier-settlement")
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
SETTLEMENT = JAVA / "settlement"
PAYLOAD = Path(".github/alpha123")

def replace_once(path, old, new, label):
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 match, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")

shutil.copyfile(PAYLOAD / "SettlementLogisticsUpgradeService.java.txt", SETTLEMENT / "SettlementLogisticsUpgradeService.java")
shutil.copyfile(PAYLOAD / "WarehouseLayout.java.txt", SETTLEMENT / "WarehouseLayout.java")
shutil.copyfile(PAYLOAD / "CartStationLayout.java.txt", SETTLEMENT / "CartStationLayout.java")

storage = SETTLEMENT / "SettlementStorageService.java"
replace_once(storage,
    "    private static boolean canSafelyCreateManagedBarrel(ServerLevel level, BlockPos pos) {",
    "    public static boolean canSafelyCreateManagedBarrel(ServerLevel level, BlockPos pos) {",
    "expose safe barrel placement")
replace_once(storage,
'''        for (BuildingRecord building : data.buildings()) {
            for (BlockPos pos : desiredWorksiteStoragePositions(building, data)) {
                if (!canSafelyCreateManagedBarrel(level, pos)) continue;
                level.setBlock(pos, Blocks.BARREL.defaultBlockState(), 3);
            }
        }
    }
''',
'''        for (BuildingRecord building : data.buildings()) {
            for (BlockPos pos : desiredWorksiteStoragePositions(building, data)) {
                if (!canSafelyCreateManagedBarrel(level, pos)) continue;
                level.setBlock(pos, Blocks.BARREL.defaultBlockState(), 3);
            }
        }
        SettlementLogisticsUpgradeService.ensureManagedStorage(level, data);
    }
''', "wire logistics storage maintenance")
replace_once(storage,
'''        for (BlockPos candidate : worksiteStoragePositions(data)) {
            if (candidate.equals(pos)) return true;
        }
        return false;
''',
'''        for (BlockPos candidate : worksiteStoragePositions(data)) {
            if (candidate.equals(pos)) return true;
        }
        for (BuildingRecord building : data.buildings()) {
            if (building.buildingType() == BuildingType.WAREHOUSE) {
                for (BlockPos candidate : WarehouseLayout.activeStoragePositions(building)) {
                    if (candidate.equals(pos)) return true;
                }
            } else if (building.buildingType() == BuildingType.CART_STATION) {
                for (BlockPos candidate : CartStationLayout.activeFreightPositions(building)) {
                    if (candidate.equals(pos)) return true;
                }
            }
        }
        return false;
''', "managed logistics position authority")
replace_once(storage,
'''            if (building.buildingType() == BuildingType.WAREHOUSE) {
                positions.addAll(WarehouseLayout.storagePositions(building));
            } else if (building.buildingType() == BuildingType.CART_STATION) {
                positions.addAll(CartStationLayout.freightPositions(building));
            }
''',
'''            if (building.buildingType() == BuildingType.WAREHOUSE) {
                positions.addAll(WarehouseLayout.activeStoragePositions(building));
            } else if (building.buildingType() == BuildingType.CART_STATION) {
                positions.addAll(CartStationLayout.activeFreightPositions(building));
            }
''', "grade-aware central storage ledger")
replace_once(storage,
'''            if (building.buildingType() == BuildingType.CART_STATION) {
                positions.addAll(CartStationLayout.freightPositions(building));
            }
''',
'''            if (building.buildingType() == BuildingType.CART_STATION) {
                positions.addAll(CartStationLayout.activeFreightPositions(building));
            }
''', "grade-aware freight positions")
replace_once(storage,
'''    public static ItemStack insert(ServerLevel level, SettlementData data, ItemStack stack) {''',
'''    /** Common copper/iron item count used by deterministic infrastructure investment. */
    public static long countCommonUpgradeMetal(ServerLevel level, SettlementData data) {
        return countProductionUpgradeMetal(level, data);
    }

    /** Alias keeps infrastructure semantics explicit while preserving the Alpha.122 atomic payment authority. */
    public static boolean consumeLogisticsUpgrade(ServerLevel level, SettlementData data,
                                                  long wood, long stone, long copperIronItems) {
        return consumeProductionUpgrade(level, data, wood, stone, copperIronItems);
    }

    public static ItemStack insert(ServerLevel level, SettlementData data, ItemStack stack) {''',
    "generic logistics payment aliases")

cart_service = SETTLEMENT / "SettlementCartStationService.java"
replace_once(cart_service,
    "            if (!CartStationLayout.freightPositions(station).contains(pos)) continue;",
    "            if (!CartStationLayout.activeFreightPositions(station).contains(pos)) continue;",
    "protect active freight expansion")

logistics = SETTLEMENT / "SettlementOutpostLogisticsService.java"
replace_once(logistics,
'''    private static final int CART_STATION_TRANSPORT_STACK = 32;
    private static final int TERRITORY_NETWORK_TRANSPORT_BONUS_PER_LEVEL = 4;
    private static final int MAX_PRODUCTIVE_TRANSPORT_STACK = 44;
''',
'''    private static final int CART_STATION_TRANSPORT_STACK = 32;
    private static final int CART_STATION_GRADE_II_TRANSPORT_STACK = 40;
    private static final int CART_STATION_GRADE_III_TRANSPORT_STACK = 48;
    private static final int TERRITORY_NETWORK_TRANSPORT_BONUS_PER_LEVEL = 4;
    private static final int MAX_PRODUCTIVE_TRANSPORT_STACK = 64;
''', "expand productive freight ladder")
replace_once(logistics,
'''    public static int productiveTransportBatchSize(SettlementData data) {
        if (data.buildingCount(BuildingType.CART_STATION) <= 0) return BASE_TRANSPORT_STACK;
        int networkLevel = SettlementExplorationBenefitService.territoryNetworkLevel(data);
        return Math.min(MAX_PRODUCTIVE_TRANSPORT_STACK, CART_STATION_TRANSPORT_STACK
                + networkLevel * TERRITORY_NETWORK_TRANSPORT_BONUS_PER_LEVEL);
    }
''',
'''    public static int productiveTransportBatchSize(SettlementData data) {
        if (data.buildingCount(BuildingType.CART_STATION) <= 0) return BASE_TRANSPORT_STACK;
        int cartGrade = SettlementLogisticsUpgradeService.bestGrade(data, BuildingType.CART_STATION);
        int cartCapacity = switch (cartGrade) {
            case 2 -> CART_STATION_GRADE_II_TRANSPORT_STACK;
            case 3 -> CART_STATION_GRADE_III_TRANSPORT_STACK;
            default -> CART_STATION_TRANSPORT_STACK;
        };
        int networkLevel = SettlementExplorationBenefitService.territoryNetworkLevel(data);
        return Math.min(MAX_PRODUCTIVE_TRANSPORT_STACK, cartCapacity
                + SettlementLogisticsUpgradeService.warehouseFreightBonus(data)
                + networkLevel * TERRITORY_NETWORK_TRANSPORT_BONUS_PER_LEVEL);
    }
''', "grade-aware productive freight throughput")

service = SETTLEMENT / "SettlementService.java"
replace_once(service,
    "        SettlementProductionUpgradeService.tick(server, data);\n        boolean explorationChanged = SettlementExplorationService.tick(server, data);",
    "        SettlementProductionUpgradeService.tick(server, data);\n        SettlementLogisticsUpgradeService.tick(server, data);\n        boolean explorationChanged = SettlementExplorationService.tick(server, data);",
    "wire logistics grade migration")

entry = JAVA / "FrontierSettlement.java"
replace_once(entry,
    "import kr.moonseungjun.frontiersettlement.settlement.SettlementEquipmentUpgradeService;\nimport kr.moonseungjun.frontiersettlement.settlement.SettlementProductionUpgradeService;\n",
    "import kr.moonseungjun.frontiersettlement.settlement.SettlementEquipmentUpgradeService;\nimport kr.moonseungjun.frontiersettlement.settlement.SettlementLogisticsUpgradeService;\nimport kr.moonseungjun.frontiersettlement.settlement.SettlementProductionUpgradeService;\n",
    "import logistics upgrade service")
replace_once(entry,
    "        NeoForge.EVENT_BUS.addListener(SettlementProductionUpgradeService::onRightClickBlock);\n        NeoForge.EVENT_BUS.addListener(SettlementBenefitService::onRightClickBlock);",
    "        NeoForge.EVENT_BUS.addListener(SettlementProductionUpgradeService::onRightClickBlock);\n        NeoForge.EVENT_BUS.addListener(SettlementLogisticsUpgradeService::onRightClickBlock);\n        NeoForge.EVENT_BUS.addListener(SettlementBenefitService::onRightClickBlock);",
    "register logistics upgrade interaction")

print("runtime patch applied")
