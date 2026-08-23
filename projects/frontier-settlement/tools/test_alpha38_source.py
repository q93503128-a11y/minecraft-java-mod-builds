#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
LEGACY = ROOT / 'tools/test_current_source.py'


def text(path):
    return path.read_text(encoding='utf-8')


def must(source, tokens, label):
    for token in tokens:
        if token not in source:
            raise SystemExit(f'{label} missing: {token}')


def forbid(source, tokens, label):
    for token in tokens:
        if token in source:
            raise SystemExit(f'{label}: {token}')


# Preserve every Alpha.23-37 invariant instead of replacing the established audit. Only the two
# canonical version expectations and the old PASS print are adapted in-memory for Alpha.38.
legacy_source = text(LEGACY)
legacy_source = legacy_source.replace("'mod_version=0.1.0-alpha.37'", "'mod_version=0.1.0-alpha.38'")
legacy_source = legacy_source.replace("!= '0.1.0-alpha.37'", "!= '0.1.0-alpha.38'")
legacy_source = legacy_source.replace("print('Frontier Settlement alpha.37 source audit: PASS')", "pass")
namespace = {'__file__': str(LEGACY), '__name__': '__main__'}
exec(compile(legacy_source, str(LEGACY), 'exec'), namespace, namespace)

required = [
    JAVA / 'settlement/ConstructionOfficeLayout.java',
    JAVA / 'settlement/ConstructionOfficeBuildingBlueprint.java',
    JAVA / 'settlement/SettlementConstructionOfficeService.java',
]
missing = [str(path.relative_to(ROOT)) for path in required if not path.is_file()]
if missing:
    raise SystemExit('alpha.38 missing required files: ' + ', '.join(missing))

entry = text(JAVA / 'FrontierSettlement.java')
must(entry, ('SettlementConstructionOfficeService',
             'SettlementConstructionOfficeService::onBreakBlock'),
     'alpha.38 construction office event integration')

service = text(JAVA / 'settlement/SettlementService.java')
if service.count('SettlementConstructionOfficeService.tick(server, data)') != 1:
    raise SystemExit('alpha.38 construction office service must have exactly one server tick call')
must(service, ('type == BuildingType.CONSTRUCTION_OFFICE',
               'SettlementConstructionOfficeService.lockedReason(data)'),
     'alpha.38 construction office unlock mask')

building_type = text(JAVA / 'settlement/BuildingType.java')
must(building_type, ('CONSTRUCTION_OFFICE("construction_office", "건설소", 112, 64, 13, 9, 12, 0',),
     'alpha.38 construction office definition')

blueprints = text(JAVA / 'settlement/BuildingBlueprints.java')
must(blueprints, ('case CONSTRUCTION_OFFICE -> ConstructionOfficeBuildingBlueprint.create(origin);',),
     'alpha.38 construction office blueprint routing')

data = text(JAVA / 'settlement/SettlementData.java')
must(data, ('WAREHOUSE, CONSTRUCTION_OFFICE, BLACKSMITH',),
     'alpha.38 construction office completion persistence')

layout = text(JAVA / 'settlement/ConstructionOfficeLayout.java')
must(layout, ('MATERIAL_SLOTS', 'materialPositions(BuildingRecord office)', 'office.localToWorld'),
     'alpha.38 rotation-aware material bay layout')

blueprint = text(JAVA / 'settlement/ConstructionOfficeBuildingBlueprint.java')
must(blueprint, ('Blocks.BARREL.defaultBlockState()', 'Blocks.CRAFTING_TABLE.defaultBlockState()',
                 'Blocks.STONECUTTER.defaultBlockState()', 'Blocks.SCAFFOLDING.defaultBlockState()',
                 'Blocks.CARTOGRAPHY_TABLE.defaultBlockState()', 'Blocks.SPRUCE_DOOR.defaultBlockState()',
                 'BuildingBlueprints.Phase.ROOF', 'BuildingBlueprints.Phase.FINISH'),
     'alpha.38 physical construction office blueprint')

storage = text(JAVA / 'settlement/SettlementStorageService.java')
must(storage, ('constructionOfficeSupplyPositions(data)', 'ordinaryStoragePositions(SettlementData data)',
               'BuildingType.CONSTRUCTION_OFFICE', 'ConstructionOfficeLayout.materialPositions(building)',
               'depositPositions(SettlementData data, ItemStack stack)',
               'SettlementInventory.isWood(stack) || SettlementInventory.isStone(stack)',
               'findExtractionTargetExcluding(', 'hasRoomAt('),
     'alpha.38 physical construction staging storage')

construction_office = text(JAVA / 'settlement/SettlementConstructionOfficeService.java')
must(construction_office, ('SUPPLY_RUNNER_TAG = "frontier_settlement_construction_supply_runner"',
                           'OFFICE_ASSIGNMENT_PREFIX', 'SERVICE_INTERVAL_TICKS = 10',
                           'HAUL_BATCH_SIZE = 32', 'TARGET_WOOD_RESERVE = 96',
                           'TARGET_STONE_RESERVE = 96', 'SOURCE_RADIUS = 24',
                           'SettlementTier.VILLAGE', 'BuildingType.WAREHOUSE',
                           'data.construction().active()', 'SettlementResidentRoutineService.isRestTime(level)',
                           'SettlementStorageService.ordinaryStoragePositions(data)',
                           'SettlementStorageService.extract(level, source, wanted',
                           'EquipmentSlot.MAINHAND', 'SettlementStorageService.insertAt(level, target, carried)',
                           'corridorLoaded(', 'level.hasChunkAt(', 'ensureSingleRunner(',
                           'ConstructionOfficeLayout.materialPositions(office)', 'BreakBlockEvent'),
     'alpha.38 physical construction supply runner')
forbid(construction_office, ('forceChunk', 'setChunkForced', 'destroyBlock(', 'dropResources(',
                             'data.addPopulation(', 'data.setPopulation(',
                             'SettlementConstructionService.tick(', 'level.setBlock('),
       'alpha.38 construction office must not create a second builder or abstract population path')

commands = text(JAVA / 'command/SettlementCommands.java')
must(commands, ('Commands.literal("construction_office")', 'BuildingType.CONSTRUCTION_OFFICE',
                 'SettlementConstructionOfficeService.lockedReason(data)', '건설 보급 | 집결 목재'),
     'alpha.38 command/status integration')

network = text(JAVA / 'network/SettlementNetwork.java')
must(network, ('BuildingType.CONSTRUCTION_OFFICE',
               'SettlementConstructionOfficeService.lockedReason(data)'),
     'alpha.38 network server guard')

palette = text(JAVA / 'client/BuildingPaletteScreen.java')
must(palette, ('BuildingType.CONSTRUCTION_OFFICE', '생산·건설'),
     'alpha.38 compact palette integration')

guidance = text(JAVA / 'settlement/SettlementGuidanceService.java')
must(guidance, ('BuildingType.WAREHOUSE', 'BuildingType.CONSTRUCTION_OFFICE', '건설소 보급'),
     'alpha.38 progression guidance')

print('Frontier Settlement alpha.38 source audit: PASS')
