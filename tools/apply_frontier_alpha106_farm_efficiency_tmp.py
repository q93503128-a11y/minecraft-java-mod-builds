from pathlib import Path

REPO = Path('.')
ROOT = REPO / 'projects/frontier-settlement'
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
SETTLEMENT = JAVA / 'settlement'


def read(path):
    return path.read_text(encoding='utf-8')


def write(path, text):
    path.write_text(text, encoding='utf-8')


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected 1 match, got {count}')
    return text.replace(old, new, 1)

# Version + runtime lock.
p = ROOT / 'gradle.properties'
s = read(p)
s = replace_once(s, 'mod_version=0.1.0-alpha.105', 'mod_version=0.1.0-alpha.106', 'gradle version')
s += '\n# Alpha.106 production density: existing production buildings auto-improve with settlement tier; farms actively tend crop growth instead of waiting only on vanilla random ticks, and Domain no longer forces a second farm when a warehouse supports one mature farm.\n'
write(p, s)

p = ROOT / 'COMPANION_LOCK.json'
s = read(p)
s = replace_once(s, '"frontier_settlement": "0.1.0-alpha.105"', '"frontier_settlement": "0.1.0-alpha.106"', 'companion lock')
write(p, s)

# One derived authority for no-footprint production upgrades.
service = '''package kr.moonseungjun.frontiersettlement.settlement;

/**
 * Existing production buildings scale vertically with settlement maturity instead of requiring
 * duplicate footprints. This state is deliberately derived from the canonical settlement tier:
 * no parallel save ledger, upgrade currency, hidden worker count or extra placement authority.
 */
public final class SettlementProductionEfficiencyService {
    private SettlementProductionEfficiencyService() {}

    public static int grade(SettlementData data) {
        return switch (SettlementTier.current(data)) {
            case CAMP, HAMLET -> 1;
            case VILLAGE -> 2;
            case FRONTIER_TOWN -> 3;
            case DOMAIN, FRONTIER_CAPITAL -> 4;
        };
    }

    public static String gradeLabel(int grade) {
        return switch (clampGrade(grade)) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "IV";
        };
    }

    public static int lumberWorkPeriod(int grade) {
        return switch (clampGrade(grade)) { case 1 -> 100; case 2 -> 90; case 3 -> 80; default -> 70; };
    }

    public static int lumberBatch(int grade) {
        return switch (clampGrade(grade)) { case 1 -> 16; case 2 -> 20; case 3 -> 24; default -> 32; };
    }

    public static int farmWorkPeriod(int grade) {
        return switch (clampGrade(grade)) { case 1 -> 120; case 2 -> 100; case 3 -> 80; default -> 80; };
    }

    /** Number of deterministic crop cohorts; one cohort receives +1 age each tending pass. */
    public static int farmGrowthModulo(int grade) {
        return switch (clampGrade(grade)) { case 1 -> 3; case 2, 3 -> 2; default -> 1; };
    }

    public static int quarryWorkPeriod(int grade) {
        return switch (clampGrade(grade)) { case 1 -> 80; case 2 -> 70; case 3 -> 60; default -> 50; };
    }

    public static int quarryBatch(int grade) {
        return switch (clampGrade(grade)) { case 1 -> 16; case 2 -> 20; case 3 -> 24; default -> 32; };
    }

    public static int mineWorkPeriod(int grade) {
        return switch (clampGrade(grade)) { case 1 -> 160; case 2 -> 130; case 3 -> 100; default -> 80; };
    }

    public static String detail(BuildingType type, SettlementData data) {
        int grade = grade(data);
        String prefix = "개량 " + gradeLabel(grade) + " · ";
        return switch (type) {
            case LUMBER_CAMP -> prefix + "자동 벌목 · 작업 묶음 " + lumberBatch(grade);
            case FARM -> prefix + "자동 식량 생산 · 작물 성장 관리 "
                    + (farmGrowthModulo(grade) == 1 ? "전 구획" : farmGrowthModulo(grade) + "구획 순환");
            case QUARRY -> prefix + "자동 채석 · 작업 묶음 " + quarryBatch(grade);
            case MINE -> prefix + "유한 광석 채굴 · 작업 주기 " + mineWorkPeriod(grade) + "틱";
            default -> "";
        };
    }

    private static int clampGrade(int grade) {
        return Math.max(1, Math.min(4, grade));
    }
}
'''
write(SETTLEMENT / 'SettlementProductionEfficiencyService.java', service)

# Production worker pacing + active crop tending.
p = SETTLEMENT / 'SettlementWorkerService.java'
s = read(p)
s = replace_once(s,
'''    private static final int MAX_LOGS_PER_WORK = 16;
    private static final int MAX_STONE_PER_WORK = 16;
    private static final int LUMBER_WORK_PERIOD_TICKS = 100;
    private static final int FARM_WORK_PERIOD_TICKS = 120;
    private static final int QUARRY_WORK_PERIOD_TICKS = 80;
    private static final int MINING_WORK_PERIOD_TICKS = 160;
''', '', 'retire fixed production pacing')
s = replace_once(s,
'''        if (!workDue(level, camp, LUMBER_WORK_PERIOD_TICKS)) return;
        Item item = level.getBlockState(target).getBlock().asItem();
        int room = cargoRoom(worker, item);
        if (room <= 0) {
            deliverToWorksiteStorage(level, data, worker, camp, carried);
            return;
        }
        ItemStack harvested = harvestVerticalTrunk(level, data, target, item, Math.min(MAX_LOGS_PER_WORK, room));''',
'''        int efficiencyGrade = SettlementProductionEfficiencyService.grade(data);
        if (!workDue(level, camp, SettlementProductionEfficiencyService.lumberWorkPeriod(efficiencyGrade))) return;
        Item item = level.getBlockState(target).getBlock().asItem();
        int room = cargoRoom(worker, item);
        if (room <= 0) {
            deliverToWorksiteStorage(level, data, worker, camp, carried);
            return;
        }
        ItemStack harvested = harvestVerticalTrunk(level, data, target, item,
                Math.min(SettlementProductionEfficiencyService.lumberBatch(efficiencyGrade), room));''', 'lumber tier pacing')
old_farm = '''        if (!workDue(level, farm, FARM_WORK_PERIOD_TICKS)) return;
        BuildingType type = farm.buildingType();
        if (type == null) return;
        int room = cargoRoom(worker, Items.WHEAT);
        int harvested = 0;
        int replanted = 0;
        for (int x = 0; x < type.width(); x++) {
            for (int z = 0; z < type.depth(); z++) {
                BlockPos crop = farm.localToWorld(x, 1, z);
                if (!level.hasChunkAt(crop) || !level.hasChunkAt(crop.below())) continue;
                BlockState state = level.getBlockState(crop);
                BlockState soil = level.getBlockState(crop.below());
                if (state.isAir() && soil.is(Blocks.FARMLAND)) {
                    if (level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3)) replanted++;
                    continue;
                }
                if (harvested >= room || !state.is(Blocks.WHEAT)
                        || !state.hasProperty(BlockStateProperties.AGE_7)
                        || state.getValue(BlockStateProperties.AGE_7) < 7) continue;
                if (level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3)) harvested++;
            }
        }
        if (harvested > 0) {
            if (appendCargo(worker, new ItemStack(Items.WHEAT, harvested))) worker.swing(InteractionHand.MAIN_HAND);
            return;
        }
        if (!worker.getMainHandItem().isEmpty() && replanted == 0) {
            deliverToWorksiteStorage(level, data, worker, farm, worker.getMainHandItem());
        }'''
new_farm = '''        int efficiencyGrade = SettlementProductionEfficiencyService.grade(data);
        int farmPeriod = SettlementProductionEfficiencyService.farmWorkPeriod(efficiencyGrade);
        if (!workDue(level, farm, farmPeriod)) return;
        BuildingType type = farm.buildingType();
        if (type == null) return;
        int room = cargoRoom(worker, Items.WHEAT);
        int harvested = 0;
        int replanted = 0;
        int grown = 0;
        int growthModulo = SettlementProductionEfficiencyService.farmGrowthModulo(efficiencyGrade);
        long tendingCycle = level.getGameTime() / Math.max(10L, farmPeriod);
        for (int x = 0; x < type.width(); x++) {
            for (int z = 0; z < type.depth(); z++) {
                BlockPos crop = farm.localToWorld(x, 1, z);
                if (!level.hasChunkAt(crop) || !level.hasChunkAt(crop.below())) continue;
                BlockState state = level.getBlockState(crop);
                BlockState soil = level.getBlockState(crop.below());
                if (state.isAir() && soil.is(Blocks.FARMLAND)) {
                    if (level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3)) replanted++;
                    continue;
                }
                if (!state.is(Blocks.WHEAT) || !state.hasProperty(BlockStateProperties.AGE_7)) continue;
                int age = state.getValue(BlockStateProperties.AGE_7);
                if (age < 7) {
                    // A staffed Frontier farm actively tends crops. Vanilla random ticks still help,
                    // but are no longer the sole production clock. Deterministic cohorts avoid a burst
                    // of 80+ block updates on early farms while higher settlement tiers tend more rows.
                    long cohortKey = (long)x * 31L + (long)z * 17L
                            + (long)farm.originX() * 7L + (long)farm.originZ() * 13L + tendingCycle;
                    if (Math.floorMod(cohortKey, growthModulo) == 0L
                            && level.setBlock(crop, state.setValue(BlockStateProperties.AGE_7, Math.min(7, age + 1)), 2)) {
                        grown++;
                    }
                    continue;
                }
                if (harvested >= room) continue;
                if (level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3)) harvested++;
            }
        }
        if (harvested > 0) {
            if (appendCargo(worker, new ItemStack(Items.WHEAT, harvested))) worker.swing(InteractionHand.MAIN_HAND);
            return;
        }
        if (grown > 0) worker.swing(InteractionHand.MAIN_HAND);
        if (!worker.getMainHandItem().isEmpty() && replanted == 0) {
            deliverToWorksiteStorage(level, data, worker, farm, worker.getMainHandItem());
        }'''
s = replace_once(s, old_farm, new_farm, 'farm active tending')
s = replace_once(s,
'''        if (!workDue(level, quarry, QUARRY_WORK_PERIOD_TICKS)) return;
        Item item = level.getBlockState(target).getBlock().asItem();
        int room = cargoRoom(worker, item);
        if (room <= 0) {
            deliverToWorksiteStorage(level, data, worker, quarry, carried);
            return;
        }
        ItemStack stone = harvestStoneCluster(level, data, target, item, Math.min(MAX_STONE_PER_WORK, room));''',
'''        int efficiencyGrade = SettlementProductionEfficiencyService.grade(data);
        if (!workDue(level, quarry, SettlementProductionEfficiencyService.quarryWorkPeriod(efficiencyGrade))) return;
        Item item = level.getBlockState(target).getBlock().asItem();
        int room = cargoRoom(worker, item);
        if (room <= 0) {
            deliverToWorksiteStorage(level, data, worker, quarry, carried);
            return;
        }
        ItemStack stone = harvestStoneCluster(level, data, target, item,
                Math.min(SettlementProductionEfficiencyService.quarryBatch(efficiencyGrade), room));''', 'quarry tier pacing')
s = replace_once(s, '        if (!workDue(level, mine, MINING_WORK_PERIOD_TICKS)) return;',
                 '        int efficiencyGrade = SettlementProductionEfficiencyService.grade(data);\n        if (!workDue(level, mine, SettlementProductionEfficiencyService.mineWorkPeriod(efficiencyGrade))) return;', 'mine tier pacing')
write(p, s)

# Domain progression no longer requires duplicate farm footprint if storage/logistics supports one mature farm.
p = SETTLEMENT / 'SettlementTier.java'
s = read(p)
s = replace_once(s,
'''        boolean legacyDomain = data.population() >= 16
                && data.outposts().size() >= 4
                && data.buildingCount(BuildingType.MINE) >= 1
                && data.buildingCount(BuildingType.FARM) >= 2;
        boolean explorationDomain = data.population() >= 14
                && data.outposts().size() >= 3
                && data.buildingCount(BuildingType.MINE) >= 1
                && data.buildingCount(BuildingType.FARM) >= 2
                && data.explorationScore() >= 5;''',
'''        boolean matureFoodBase = hasMatureFoodBase(data);
        boolean legacyDomain = data.population() >= 16
                && data.outposts().size() >= 4
                && data.buildingCount(BuildingType.MINE) >= 1
                && matureFoodBase;
        boolean explorationDomain = data.population() >= 14
                && data.outposts().size() >= 3
                && data.buildingCount(BuildingType.MINE) >= 1
                && matureFoodBase
                && data.explorationScore() >= 5;''', 'domain farm density')
insert = '''\n    private static boolean hasMatureFoodBase(SettlementData data) {\n        int farms = data.buildingCount(BuildingType.FARM);\n        // Existing two-farm saves remain valid, but a warehouse-backed upgraded farm now represents\n        // the same mature food-economy milestone without consuming a second 13x11 footprint.\n        return farms >= 2 || (farms >= 1 && data.buildingCount(BuildingType.WAREHOUSE) >= 1);\n    }\n'''
s = replace_once(s, '\n        return CAMP;\n    }\n}', '\n        return CAMP;\n    }\n' + insert + '}', 'domain helper insertion')
write(p, s)

# Context/UI/guide visibility.
p = SETTLEMENT / 'SettlementContextService.java'
s = read(p)
s = replace_once(s,
'''            case LUMBER_CAMP -> "완공 · 자동 벌목";
            case FARM -> "완공 · 자동 식량 생산";
            case QUARRY -> "완공 · 자동 채석";
            case MINE -> "완공 · 유한 광석 생산";''',
'''            case LUMBER_CAMP, FARM, QUARRY, MINE -> "완공 · " + SettlementProductionEfficiencyService.detail(type, data);''', 'context production detail')
write(p, s)

p = JAVA / 'client/BuildingPaletteScreen.java'
s = read(p)
s = replace_once(s,
'        PRODUCTION("생산", "목재·식량·석재·광물", List.of(BuildingType.LUMBER_CAMP, BuildingType.FARM, BuildingType.QUARRY, BuildingType.MINE)),',
'        PRODUCTION("생산", "목재·식량·석재·광물 · 마을 단계에 따라 기존 시설 자동 개량", List.of(BuildingType.LUMBER_CAMP, BuildingType.FARM, BuildingType.QUARRY, BuildingType.MINE)),', 'palette production copy')
write(p, s)

p = JAVA / 'client/SettlementGuideScreen.java'
s = read(p)
s = replace_once(s,
'''                    "주택은 인구를, 생산 건물은 자동 생산 기반을 늘립니다.",
                    "HUD의 노란 ‘다음 목표’를 따라가면 해금 흐름이 이어집니다.",
                    "주민 직업을 한 명씩 지정할 필요는 없습니다.");''',
'''                    "생산시설은 반복 건설만 강요하지 않고 마을 단계에 따라 기존 시설이 자동 개량됩니다.",
                    "농장 주민은 작물을 직접 관리해 바닐라 랜덤 성장만 기다리지 않습니다.",
                    "HUD의 노란 ‘다음 목표’를 따라가면 해금 흐름이 이어집니다.");''', 'guide production copy')
write(p, s)

# Current-source verifier owns the new invariant.
p = ROOT / 'tools/test_current_source.py'
s = read(p)
s = s.replace('mod_version=0.1.0-alpha.105', 'mod_version=0.1.0-alpha.106')
anchor = 'require("DUPLICATE_MAINTENANCE_INTERVAL_TICKS = 200" in worker, "maintenance duplicate scans regressed to hot-path cadence")\n'
if anchor not in s:
    raise SystemExit('verifier worker anchor missing')
add = '''production_efficiency = text(SETTLEMENT / "SettlementProductionEfficiencyService.java")
require("SettlementTier.current(data)" in production_efficiency, "production upgrades are not derived from canonical settlement tier")
require("case CAMP, HAMLET -> 1" in production_efficiency and "case DOMAIN, FRONTIER_CAPITAL -> 4" in production_efficiency, "production efficiency grade ladder drifted")
require("farmGrowthModulo" in production_efficiency and "mineWorkPeriod" in production_efficiency, "production efficiency parameters incomplete")
require("SettlementProductionEfficiencyService.farmWorkPeriod" in worker, "farm still uses fixed work cadence")
require("state.setValue(BlockStateProperties.AGE_7, Math.min(7, age + 1))" in worker, "staffed farm does not actively tend crop growth")
for stale in ("FARM_WORK_PERIOD_TICKS", "LUMBER_WORK_PERIOD_TICKS", "QUARRY_WORK_PERIOD_TICKS", "MINING_WORK_PERIOD_TICKS"):
    require(stale not in worker, f"stale fixed production pacing authority returned: {stale}")
'''
s = s.replace(anchor, anchor + add, 1)
anchor = 'palette = text(JAVA / "client/BuildingPaletteScreen.java")\nrequire("civilUnlocked" not in palette, "client still owns partial civil unlock logic")\n'
if anchor not in s:
    raise SystemExit('verifier palette anchor missing')
s = s.replace(anchor, anchor + 'require("기존 시설 자동 개량" in palette, "production vertical progression is hidden from the build palette")\n', 1)
s = s.replace('print("CURRENT SOURCE CHECK PASS: alpha105 runtime optimization + alpha104 full flatten + prior authority invariants")',
              'tier = text(SETTLEMENT / "SettlementTier.java")\nrequire("hasMatureFoodBase" in tier and "BuildingType.WAREHOUSE" in tier, "Domain still forces duplicate farm footprint")\n\nprint("CURRENT SOURCE CHECK PASS: alpha106 production density + active farm tending + prior authority invariants")')
write(p, s)

print('Frontier Alpha.106 farm/production efficiency patch applied')
