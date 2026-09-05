from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
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
require("SettlementGuidanceService.nextGoal(player.level().getServer(), data)" in service, "guidance is missing server authority")
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

retired_mutators = (
    REPO / ".github/workflows/apply-frontier-alpha89.yml",
    REPO / ".github/workflows/apply-frontier-alpha90.yml",
    REPO / ".github/workflows/apply-frontier-alpha90-retry.yml",
    REPO / ".github/workflows/apply-frontier-alpha91.yml",
    REPO / ".github/workflows/apply-starter-shared-supply-depot.yml",
    ROOT / "tools/apply_alpha89.py",
    ROOT / "tools/apply_alpha91_patch.py",
    ROOT / "tools/patch_production_shared_depot_routing.py",
    ROOT / "tools/patch_worksite_output_provenance.py",
    REPO / "tools/apply_starter_shared_supply_depot.py",
    REPO / "tools/fix_starter_depot_migration_gate.py",
)
for retired in retired_mutators:
    require(not retired.exists(), f"obsolete Frontier mutator returned: {retired.relative_to(REPO)}")

integrity = text(SETTLEMENT / "SettlementBuildingIntegrityService.java")
require("RUIN_INTACT_PERCENT = 45" in integrity and "removeCompletedBuilding" in integrity and "clearKnownHouseRemnants" in integrity, "Alpha98 house integrity authority regressed")

print("CURRENT SOURCE CHECK PASS: alpha99 authority cleanup invariants")
