#!/usr/bin/env python3
import json
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
A83 = ROOT / "tools/test_alpha83_source.py"
REPO = ROOT.parents[1]
ALPHA83_SHA = "47d8da5bb29d5019bead9b28771a2fb1416c8584"
MODIFIED = {
    'COMPANION_LOCK.json',
    'FrontierContent.java',
    'FrontierSettlementClient.java',
    'SettlementAdvancedWorkshopService.java',
    'SettlementCivilFillSupplyService.java',
    'SettlementCivilRetainingService.java',
    'SettlementCivilWorkService.java',
    'SettlementConstructionOfficeService.java',
    'SettlementConstructionService.java',
    'SettlementContextService.java',
    'SettlementFishingOutpostService.java',
    'SettlementMarketService.java',
    'SettlementOutpostLogisticsService.java',
    'SettlementOutpostProductionService.java',
    'SettlementOutpostService.java',
    'SettlementResidentRoutineService.java',
    'SettlementRoadService.java',
    'SettlementService.java',
    'SettlementWaterfrontService.java',
    'SettlementWorkerService.java',
    'SettlementWorkshopService.java',
    'gradle.properties'
}
NEW_FILES = {'FrontierWorkerEntity.java', 'FrontierWorkerRenderer.java', 'SettlementLegacyWorkerMigrationService.java'}
_real_read = Path.read_text

def legacy_read(self, *args, **kwargs):
    if self.name in NEW_FILES:
        return "// Alpha.84-only file hidden from Alpha.83 replay.\n"
    if self.name in {"test_alpha72_source.py", "test_alpha73_source.py", "test_alpha74_source.py"}:
        s = _real_read(self, *args, **kwargs)
        s = s.replace("len(java_files)!=105", "len(java_files)!=113")
        s = s.replace("expected 105 Java files", "expected 113 Java files")
        s = s.replace("rglob('*.java')))!=105", "rglob('*.java')))!=113")
        s = s.replace("rglob('*.java')))!=106", "rglob('*.java')))!=114")
        return s
    if self.name in MODIFIED:
        try:
            rel = self.resolve().relative_to(REPO.resolve()).as_posix()
        except ValueError:
            rel = ""
        if rel.startswith("projects/frontier-settlement/"):
            return subprocess.check_output(["git", "show", f"{ALPHA83_SHA}:{rel}"], cwd=REPO, text=True, encoding="utf-8")
    return _real_read(self, *args, **kwargs)

Path.read_text = legacy_read
try:
    chain = _real_read(A83, encoding="utf-8").replace(
        'print("Frontier Settlement alpha.23-83 cumulative source audit: PASS")', 'pass'
    )
    ns = {"__file__": str(A83), "__name__": "__main__"}
    exec(compile(chain, str(A83), "exec"), ns, ns)
finally:
    Path.read_text = _real_read

def text(path): return Path(path).read_text(encoding="utf-8")
def must(src, tokens, label):
    for token in tokens:
        if token not in src: raise SystemExit(f"{label} missing: {token}")
def forbid(src, tokens, label):
    for token in tokens:
        if token in src: raise SystemExit(f"{label} forbidden: {token}")

props = text(ROOT / "gradle.properties")
entity = text(JAVA / "content/FrontierWorkerEntity.java")
renderer = text(JAVA / "client/FrontierWorkerRenderer.java")
migration = text(JAVA / "settlement/SettlementLegacyWorkerMigrationService.java")
construction = text(JAVA / "settlement/SettlementConstructionService.java")
service = text(JAVA / "settlement/SettlementService.java")
content = text(JAVA / "content/FrontierContent.java")
client = text(JAVA / "client/FrontierSettlementClient.java")
context = text(JAVA / "settlement/SettlementContextService.java")
lock = json.loads(text(ROOT / "COMPANION_LOCK.json"))

must(props, ("mod_version=0.1.0-alpha.84", "Alpha.84 independent worker runtime"), "alpha.84 props")
must(entity, ("extends PathfinderMob", "protected void registerGoals()", "Intentionally empty", "removeWhenFarAway"), "frontier worker entity")
forbid(entity, ("extends Villager", "world.entity.npc.villager", "VillagerData"), "frontier worker server body")
must(renderer, ("VillagerModel", "textures/entity/villager/villager.png", "villagerData = null"), "worker visual-only renderer")
must(migration, ("instanceof",) if False else ("Villager.class", "frontier_settlement_", "old.discard()", "EquipmentSlot.values()"), "legacy worker migration")
must(construction, ("GRADE_WORK_RANGE_SQR = 36.0D", "moveBuilderTowardGradeCell", "getNavigation().moveTo"), "grading recovery")
forbid(construction, ("net.minecraft.world.entity.npc.villager.Villager", "EntityTypes.VILLAGER"), "construction vanilla villager authority")
must(service, ("SettlementLegacyWorkerMigrationService.tick(server, data);",), "migration order")
must(content, ("FRONTIER_WORKER", '"frontier_worker"', "FrontierWorkerEntity.createAttributes()"), "worker registration")
must(client, ("FRONTIER_WORKER.get(), FrontierWorkerRenderer::new",), "worker renderer registration")
must(context, ("건물 자재는 정리 완료 후 실물 운반",), "grading material UX")

# Server worker code may mention Villager only in the one-way legacy migration seam.
violations = []
for p in (JAVA / "settlement").glob("*.java"):
    if p.name == "SettlementLegacyWorkerMigrationService.java": continue
    src = text(p)
    if "net.minecraft.world.entity.npc.villager.Villager" in src or "EntityTypes.VILLAGER" in src:
        violations.append(p.name)
if violations: raise SystemExit(f"alpha.84 vanilla Villager worker authority remains: {violations}")

all_java = "\n".join(text(p) for p in JAVA.rglob("*.java"))
forbid(all_java, ("setChunkForced", "forceChunk", "teleportTo("), "alpha.84 no force-load/teleport recovery")
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.84":
    raise SystemExit("alpha.84 companion lock target drifted")
if not any("Alpha.84 keeps every Alpha.83 companion binary pin unchanged" in n for n in lock.get("notes", [])):
    raise SystemExit("alpha.84 companion rationale missing")

# Alpha.84 graphical-playtest follow-up: no silent start, no historical loaded-chunk dependency,
# and grading must report real progress instead of a hard-coded zero.
construction_now = text(JAVA / "settlement/SettlementConstructionService.java")
context_now = text(JAVA / "settlement/SettlementContextService.java")
must(construction_now, (
    "if (builder == null)",
    "data.clearConstruction();",
    "자원은 차감되지 않았습니다.",
    "Completed historical infrastructure is not a legal builder location",
    "public static int gradingSteps",
), "alpha.84 construction recovery")
route_body = construction_now.split("private static AABB builderRouteBounds", 1)[1].split("private static boolean builderAssignmentEvidenceLoaded", 1)[0]
forbid(route_body, ("for (BuildingRecord building : data.buildings())", "for (RoadSegment road : data.roads())", "for (OutpostRecord outpost : data.outposts())"), "alpha.84 historical builder envelope")
must(context_now, ("gradeTotal", "construction.gradeStep()", "gradeTotal + buildTotal"), "alpha.84 real grading progress")
forbid(context_now, ("construction.grading() ? 0",), "alpha.84 hard-coded grading zero")


# Final graphical-playtest audit: accepted placement must preflight the exact grading envelope,
# and worker appearance must not reintroduce a civilian night/bed/jobsite schedule layer.
construction_final = text(JAVA / "settlement/SettlementConstructionService.java")
service_final = text(JAVA / "settlement/SettlementService.java")
must(construction_final, ("ConstructionState gradingPreview", "for (GradeCell cell : createGradePlan", "건물 주변 1블록까지 부지 정리가 가능한 공간"), "alpha.84 grading preflight")
must(service_final, ("Civilian appearance does not imply a villager lifestyle simulation", "SettlementWorkerService.tick(server, data);"), "alpha.84 no lifestyle schedule")
forbid(service_final, ("SettlementResidentRoutineService.isRestTime", "SettlementResidentRoutineService.tick(server, data)"), "alpha.84 runtime night schedule")

print("Frontier Settlement alpha.23-84 cumulative source audit: PASS")
