from pathlib import Path
import re

ROOT = Path('projects/frontier-settlement')
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
SETTLEMENT = JAVA / 'settlement'
CLIENT = JAVA / 'client'


def read(path: Path) -> str:
    return path.read_text(encoding='utf-8')


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding='utf-8')


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {count}')
    return text.replace(old, new, 1)


def remove_method(text: str, signature_fragment: str) -> str:
    start = text.find(signature_fragment)
    if start < 0:
        raise SystemExit(f'method signature not found: {signature_fragment}')
    brace = text.find('{', start)
    if brace < 0:
        raise SystemExit(f'method opening brace not found: {signature_fragment}')
    depth = 0
    i = brace
    in_string = False
    in_char = False
    escape = False
    line_comment = False
    block_comment = False
    while i < len(text):
        c = text[i]
        n = text[i + 1] if i + 1 < len(text) else ''
        if line_comment:
            if c == '\n':
                line_comment = False
        elif block_comment:
            if c == '*' and n == '/':
                block_comment = False
                i += 1
        elif in_string:
            if escape:
                escape = False
            elif c == '\\':
                escape = True
            elif c == '"':
                in_string = False
        elif in_char:
            if escape:
                escape = False
            elif c == '\\':
                escape = True
            elif c == "'":
                in_char = False
        else:
            if c == '/' and n == '/':
                line_comment = True
                i += 1
            elif c == '/' and n == '*':
                block_comment = True
                i += 1
            elif c == '"':
                in_string = True
            elif c == "'":
                in_char = True
            elif c == '{':
                depth += 1
            elif c == '}':
                depth -= 1
                if depth == 0:
                    end = i + 1
                    while end < len(text) and text[end] in ' \t':
                        end += 1
                    if end < len(text) and text[end] == '\r':
                        end += 1
                    if end < len(text) and text[end] == '\n':
                        end += 1
                    return text[:start] + text[end:]
        i += 1
    raise SystemExit(f'unclosed method: {signature_fragment}')

# Version: this cleanup is a runtime/source-authority change, not just docs.
gradle = ROOT / 'gradle.properties'
g = read(gradle)
g = replace_once(g, 'mod_version=0.1.0-alpha.98', 'mod_version=0.1.0-alpha.99', 'version')
write(gradle, g)

# Current scheduler: remove obsolete Alpha.85-era cadence prose and route guidance through server context.
service_path = SETTLEMENT / 'SettlementService.java'
s = read(service_path)
s = replace_once(
    s,
    '// Building construction is presentation-sensitive and must not be quantized behind the\n        // 5-tick infrastructure scheduler: e.g. an 8-tick grading gate sampled every 5 ticks\n        // only fires every LCM(5, 8)=40 ticks. Roads/outposts/civil work keep their old cadence.',
    '// Building construction owns its per-tick presentation cadence. Roads, outposts and civil work\n        // share the bounded 5-tick infrastructure scheduler below.',
    'scheduler comment')
s = replace_once(s, 'SettlementGuidanceService.nextGoal(data)', 'SettlementGuidanceService.nextGoal(server, data)', 'guidance server authority')
write(service_path, s)

# One canonical arrival-food value and active civil-work guidance.
worker_path = SETTLEMENT / 'SettlementWorkerService.java'
w = read(worker_path)
anchor = '    private SettlementWorkerService() {}\n'
insert = '    private SettlementWorkerService() {}\n\n    public static long arrivalFoodCost() { return ARRIVAL_FOOD_COST; }\n'
w = replace_once(w, anchor, insert, 'arrival food accessor')
write(worker_path, w)

guidance_path = SETTLEMENT / 'SettlementGuidanceService.java'
gd = read(guidance_path)
gd = replace_once(gd,
    'package kr.moonseungjun.frontiersettlement.settlement;\n',
    'package kr.moonseungjun.frontiersettlement.settlement;\n\nimport net.minecraft.server.MinecraftServer;\n',
    'guidance import')
gd = replace_once(gd, 'public static String nextGoal(SettlementData data)', 'public static String nextGoal(MinecraftServer server, SettlementData data)', 'guidance signature')
gd = replace_once(gd,
    '        if (data.outpostConstruction().active()) return "진행 중 · " + SettlementOutpostService.phaseLabel(data.outpostConstruction());\n\n',
    '        if (data.outpostConstruction().active()) return "진행 중 · " + SettlementOutpostService.phaseLabel(data.outpostConstruction());\n        if (SettlementCivilWorkData.get(server).project().active()) return "진행 중 · " + SettlementCivilWorkService.phaseLabel(server);\n\n',
    'civil guidance')
gd = replace_once(gd, 'if (data.resources().food() < 8L)', 'if (data.resources().food() < SettlementWorkerService.arrivalFoodCost())', 'arrival food stale guidance')
write(guidance_path, gd)

# Client does not own a partial civil-work unlock rule. Server preview/start remains the single authority.
palette_path = CLIENT / 'BuildingPaletteScreen.java'
p = read(palette_path)
old = '''        boolean civilUnlocked = data.tier().equals("영지") || data.tier().equals("개척 수도");
        Button civil = Button.builder(Component.literal(
                        civilUnlocked ? "토목 평탄화   · 절토/성토" : "토목 평탄화   · 영지에서 해금"),
                b -> { CivilWorkPlacementClient.beginPlacement(); this.minecraft.gui.setScreen(null); })
                .bounds(x, y + 62, width, 23).build();
        civil.active = civilUnlocked;
        addRenderableWidget(civil);
'''
new = '''        addRenderableWidget(Button.builder(Component.literal("토목 평탄화   · 절토/성토"),
                b -> { CivilWorkPlacementClient.beginPlacement(); this.minecraft.gui.setScreen(null); })
                .bounds(x, y + 62, width, 23).build());
'''
p = replace_once(p, old, new, 'civil client authority')
write(palette_path, p)

# Shared project-builder readiness. This is the single start-time authority for every project type.
construction_path = SETTLEMENT / 'SettlementConstructionService.java'
c = read(construction_path)
for line in (
    '    private static final double DIRECT_HIGH_WORK_RANGE_SQR = 25.0D;\n',
    '    private static final double HIGH_WORK_RANGE_SQR = 196.0D;\n',
    '    private static final double SCAFFOLD_POSITION_REACHED_SQR = 2.25D;\n',
):
    if line not in c:
        raise SystemExit('retired scaffold constant missing: ' + line.strip())
    c = c.replace(line, '', 1)

for signature in (
    '    private static boolean hasFreshScaffoldCoverage(',
    '    private static void ensureConstructionScaffolds(',
    '    private static boolean canClaimFreshTower(',
    '    private static boolean placeClaimedTower(',
    '    private static void repairClaimedTower(',
    '    private static boolean hasWalkableScaffoldEntry(',
):
    c = remove_method(c, signature)

# Retain only the geometry needed to tear down pre-retirement scaffold blocks.
c = c.replace('        List<BlockPos> steps = new ArrayList<>();\n', '')
c = c.replace('            steps.add(treadPos);\n', '')
c = replace_once(c,
    '        return new ScaffoldTower(center, List.copyOf(pieces), List.copyOf(steps));',
    '        return new ScaffoldTower(List.copyOf(pieces));',
    'legacy scaffold tower return')
c = replace_once(c,
    '    private record ScaffoldTower(BlockPos anchor, List<ScaffoldPiece> pieces, List<BlockPos> steps) {}',
    '    private record ScaffoldTower(List<ScaffoldPiece> pieces) {}',
    'legacy scaffold tower record')

builder_signature = '    public static FrontierWorkerEntity ensureBuilder(ServerLevel level, SettlementData data) {'
if builder_signature not in c:
    raise SystemExit('ensureBuilder signature missing')
helper = '''    public static FrontierWorkerEntity ensureProjectBuilder(ServerLevel level, SettlementData data) {
        FrontierWorkerEntity builder = ensureBuilder(level, data);
        if (builder == null) return null;
        if (builder.isNoAi()) builder.setNoAi(false);
        builder.setInvulnerable(false);
        return builder;
    }

'''
c = c.replace(builder_signature, helper + builder_signature, 1)
c = replace_once(c, 'FrontierWorkerEntity builder = ensureBuilder(level, data);\n        if (builder == null) {\n            data.clearConstruction();',
                     'FrontierWorkerEntity builder = ensureProjectBuilder(level, data);\n        if (builder == null) {\n            data.clearConstruction();', 'building project builder')
# The start path no longer needs a second invulnerability write after the shared helper.
start_marker = 'return new StartResult(false, "건설 작업자를 안전하게 확보할 수 없어 착공하지 않았습니다. 주변 마을·공동 창고 청크를 로드한 뒤 다시 시도해 주세요. 자원은 차감되지 않았습니다.");\n        }\n        builder.setInvulnerable(false);\n        SettlementService.broadcast(server, data);'
c = replace_once(c, start_marker,
    'return new StartResult(false, "건설 작업자를 안전하게 확보할 수 없어 착공하지 않았습니다. 주변 마을·공동 창고 청크를 로드한 뒤 다시 시도해 주세요. 자원은 차감되지 않았습니다.");\n        }\n        SettlementService.broadcast(server, data);',
    'building duplicate builder state')
write(construction_path, c)

# Road, outpost and civil starts must obey the same transaction invariant as building starts.
road_path = SETTLEMENT / 'SettlementRoadService.java'
r = read(road_path)
r = replace_once(r,
    '        data.beginRoadConstruction(chosen.centers(), chosen.profile(), chosen.supports());\n        SettlementConstructionService.ensureBuilder(level, data);\n        SettlementService.broadcast(server, data);',
    '        data.beginRoadConstruction(chosen.centers(), chosen.profile(), chosen.supports());\n        if (SettlementConstructionService.ensureProjectBuilder(level, data) == null) {\n            data.clearRoadConstruction();\n            SettlementService.broadcast(server, data);\n            return new StartResult(false, "건설 작업자를 안전하게 확보할 수 없어 도로 착공을 취소했습니다. 주변 마을·공동 창고 청크를 로드한 뒤 다시 시도해 주세요. 자원은 차감되지 않았습니다.");\n        }\n        SettlementService.broadcast(server, data);',
    'road transactional builder')
write(road_path, r)

outpost_path = SETTLEMENT / 'SettlementOutpostService.java'
o = read(outpost_path)
o = replace_once(o,
    '        data.beginOutpostConstruction(roadIndex, gate, road.directionX(), road.directionZ());\n        data.replaceOutpostConstructionStep(OutpostConstructionState.GRADE_STEP_OFFSET);\n        SettlementConstructionService.ensureBuilder(level, data);\n        SettlementService.broadcast(server, data);',
    '        data.beginOutpostConstruction(roadIndex, gate, road.directionX(), road.directionZ());\n        data.replaceOutpostConstructionStep(OutpostConstructionState.GRADE_STEP_OFFSET);\n        if (SettlementConstructionService.ensureProjectBuilder(level, data) == null) {\n            data.clearOutpostConstruction();\n            SettlementService.broadcast(server, data);\n            return new StartResult(false, "건설 작업자를 안전하게 확보할 수 없어 전초기지 착공을 취소했습니다. 주변 마을·공동 창고 청크를 로드한 뒤 다시 시도해 주세요. 자원은 차감되지 않았습니다.");\n        }\n        SettlementService.broadcast(server, data);',
    'outpost transactional builder')
write(outpost_path, o)

civil_path = SETTLEMENT / 'SettlementCivilWorkService.java'
cv = read(civil_path)
old_civil = '''        data.begin(new CivilWorkState(true, check.minX(), check.maxX(), check.minZ(), check.maxZ(), check.gradeY(),
                CivilWorkState.PHASE_CUT, 0, 0, check.cutBlocks(), check.fillBlocks(), check.retainingBlocks()));
        FrontierWorkerEntity builder = SettlementConstructionService.ensureBuilder(server.overworld(), settlement);
        if (builder != null) {
            builder.setNoAi(false);
            builder.setInvulnerable(false);
            builder.setCustomName(Component.literal("건설 주민 · 토목"));
        }
        SettlementService.broadcast(server, settlement);
'''
new_civil = '''        data.begin(new CivilWorkState(true, check.minX(), check.maxX(), check.minZ(), check.maxZ(), check.gradeY(),
                CivilWorkState.PHASE_CUT, 0, 0, check.cutBlocks(), check.fillBlocks(), check.retainingBlocks()));
        FrontierWorkerEntity builder = SettlementConstructionService.ensureProjectBuilder(server.overworld(), settlement);
        if (builder == null) {
            data.clear();
            SettlementService.broadcast(server, settlement);
            return new StartResult(false, "건설 작업자를 안전하게 확보할 수 없어 토목 착공을 취소했습니다. 주변 마을·공동 창고 청크를 로드한 뒤 다시 시도해 주세요. 자재는 차감되지 않았습니다.");
        }
        builder.setCustomName(Component.literal("건설 주민 · 토목"));
        SettlementService.broadcast(server, settlement);
'''
cv = replace_once(cv, old_civil, new_civil, 'civil transactional builder')
write(civil_path, cv)

# Status output must show the actual exploration-modified values, not old base constants.
commands_path = JAVA / 'command/SettlementCommands.java'
cmd = read(commands_path)
cmd = cmd.replace('SettlementAdvancedWorkshopService.REFORGE_POWER', 'SettlementExplorationBenefitService.reforgePower(data)')
cmd = cmd.replace('SettlementAdvancedWorkshopService.ENCHANTMENT_POWER', 'SettlementExplorationBenefitService.forgePower(data)')
cmd = cmd.replace('SettlementBarracksService.RECRUIT_FOOD_COST+" 금속 "', 'SettlementExplorationBenefitService.barracksRecruitFoodCost(server)+" 금속 "')
write(commands_path, cmd)

# The old night-life navigation layer is intentionally retired: work/logistics owns worker navigation.
resident = SETTLEMENT / 'SettlementResidentRoutineService.java'
if not resident.exists():
    raise SystemExit('retired resident routine source missing')
resident.unlink()

# Ignore and remove generated Python cache artifacts from source control.
gitignore = ROOT / '.gitignore'
gi = read(gitignore)
if '__pycache__/' not in gi:
    gi += '__pycache__/\n'
if '*.py[cod]' not in gi:
    gi += '*.py[cod]\n'
write(gitignore, gi)
for cache in (ROOT / 'tools').rglob('*.pyc'):
    cache.unlink()
for cache_dir in sorted((ROOT / 'tools').rglob('__pycache__'), reverse=True):
    try:
        cache_dir.rmdir()
    except OSError:
        pass

# Replace the misleading Alpha.37 "current" verifier with invariants for the actual current source.
current_test = ROOT / 'tools/test_current_source.py'
current_test.write_text(r'''from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
SETTLEMENT = JAVA / "settlement"


def text(path):
    return path.read_text(encoding="utf-8")


def require(condition, message):
    if not condition:
        raise AssertionError(message)


gradle = text(ROOT / "gradle.properties")
require("mod_version=0.1.0-alpha.99" in gradle, "current verifier/version drift")

construction = text(SETTLEMENT / "SettlementConstructionService.java")
for retired in (
    "DIRECT_HIGH_WORK_RANGE_SQR",
    "HIGH_WORK_RANGE_SQR",
    "SCAFFOLD_POSITION_REACHED_SQR",
    "hasFreshScaffoldCoverage(",
    "ensureConstructionScaffolds(",
    "canClaimFreshTower(",
    "placeClaimedTower(",
    "repairClaimedTower(",
    "hasWalkableScaffoldEntry(",
):
    require(retired not in construction, f"retired scaffold authority returned: {retired}")
require("retireLegacyConstructionScaffolds(" in construction, "legacy scaffold teardown compatibility was removed")
require("ensureProjectBuilder(" in construction, "shared project-builder authority missing")

road = text(SETTLEMENT / "SettlementRoadService.java")
outpost = text(SETTLEMENT / "SettlementOutpostService.java")
civil = text(SETTLEMENT / "SettlementCivilWorkService.java")
require("ensureProjectBuilder" in road and "clearRoadConstruction" in road, "road start is not transactional")
require("ensureProjectBuilder" in outpost and "clearOutpostConstruction" in outpost, "outpost start is not transactional")
require("ensureProjectBuilder" in civil and "data.clear();" in civil, "civil start is not transactional")

worker = text(SETTLEMENT / "SettlementWorkerService.java")
guidance = text(SETTLEMENT / "SettlementGuidanceService.java")
require("arrivalFoodCost()" in worker, "canonical worker arrival-food accessor missing")
require("SettlementWorkerService.arrivalFoodCost()" in guidance, "guidance duplicates arrival-food balance")
require("data.resources().food() < 8L" not in guidance, "stale arrival-food value returned")
require("SettlementCivilWorkData.get(server).project().active()" in guidance, "active civil work absent from next-goal authority")

service = text(SETTLEMENT / "SettlementService.java")
require("SettlementGuidanceService.nextGoal(server, data)" in service, "guidance is missing server authority")
require("8-tick grading gate" not in service, "obsolete construction cadence prose remains")

palette = text(JAVA / "client/BuildingPaletteScreen.java")
require("civilUnlocked" not in palette, "client still owns partial civil unlock logic")

commands = text(JAVA / "command/SettlementCommands.java")
require("SettlementExplorationBenefitService.barracksRecruitFoodCost(server)" in commands, "status shows stale barracks food cost")
require("SettlementExplorationBenefitService.forgePower(data)" in commands, "status shows stale forge power")
require("SettlementExplorationBenefitService.reforgePower(data)" in commands, "status shows stale reforge power")

require(not (SETTLEMENT / "SettlementResidentRoutineService.java").exists(), "retired night navigation authority still exists")
require("__pycache__/" in text(ROOT / ".gitignore"), "Python cache ignore missing")
require(not any((ROOT / "tools").rglob("*.pyc")), "generated pyc committed in tools")

integrity = text(SETTLEMENT / "SettlementBuildingIntegrityService.java")
require("0.45D" in integrity and "removeCompletedBuilding" in integrity, "Alpha98 house integrity authority regressed")

print("CURRENT SOURCE CHECK PASS: alpha99 authority cleanup invariants")
''', encoding='utf-8')

print('Frontier alpha99 authority cleanup applied')
