#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement"
WORKER = JAVA / "SettlementWorkerService.java"
CONSTRUCTION = JAVA / "SettlementConstructionService.java"
PROPS = ROOT / "gradle.properties"
LOCK = ROOT / "COMPANION_LOCK.json"


def replace_once(path: Path, old: str, new: str, label: str):
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, got {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


# Alpha.89 version metadata.
replace_once(PROPS, "mod_version=0.1.0-alpha.88", "mod_version=0.1.0-alpha.89", "version")
with PROPS.open("a", encoding="utf-8") as f:
    f.write("\n# Alpha.89 duplicate-worker recovery: loaded-evidence-safe physical cleanup for production workers and the shared builder.\n")

# Production duplicate cleanup runs before the 30-second recruitment gate.
old_tick = '''    public static void tick(MinecraftServer server, SettlementData data) {
        ServerLevel level = server.overworld();
        if (server.getTickCount() % 10 == 0) {
            SettlementOutpostLogisticsService.migrateLegacyWorkers(level, data);
        }
        if (server.getTickCount() % 600 == 0) tryAttractWorker(server, level, data);
        if (server.getTickCount() % 10 != 0) return;

        runBuildingWorkers(level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME, SettlementWorkerService::workLumber);
'''
new_tick = '''    public static void tick(MinecraftServer server, SettlementData data) {
        ServerLevel level = server.overworld();
        if (server.getTickCount() % 10 == 0) {
            SettlementOutpostLogisticsService.migrateLegacyWorkers(level, data);
            SettlementConstructionService.reconcileBuilderDuplicates(level, data);
            reconcileProductionDuplicates(server, level, data);
        }
        // Duplicate reconciliation must run first on the same 600-tick boundary so an excess
        // historical worker can never be removed and immediately replaced from stale population state.
        if (server.getTickCount() % 600 == 0) tryAttractWorker(server, level, data);
        if (server.getTickCount() % 10 != 0) return;

        runBuildingWorkers(level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME, SettlementWorkerService::workLumber);
'''
replace_once(WORKER, old_tick, new_tick, "worker tick")

anchor = '''    @FunctionalInterface
    private interface BuildingWork {
'''
methods = '''    /**
     * Save-recovery cleanup for historical Alpha.84-88 local production duplicates.
     *
     * Cleanup is destructive only after the complete local production/storage evidence envelope is
     * loaded. UUID order is already deterministic in workersByName(), so exactly one physical worker
     * per completed production building remains authoritative. No unloaded resident is treated as dead.
     */
    private static void reconcileProductionDuplicates(MinecraftServer server, ServerLevel level, SettlementData data) {
        if (!localProductionEvidenceLoaded(level, data)) return;
        int removed = 0;
        removed += trimExcessProductionWorkers(level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME);
        removed += trimExcessProductionWorkers(level, data, BuildingType.FARM, FARM_WORKER_NAME);
        removed += trimExcessProductionWorkers(level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME);
        removed += trimExcessProductionWorkers(level, data, BuildingType.MINE, MINE_WORKER_NAME);
        if (removed <= 0) return;

        repairPopulationAfterDuplicateCleanup(level, data);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
    }

    private static int trimExcessProductionWorkers(ServerLevel level, SettlementData data,
                                                    BuildingType type, String workerName) {
        int allowed = buildings(data, type).size();
        List<FrontierWorkerEntity> workers = workersByName(level, data, type, workerName);
        if (workers.size() <= allowed) return 0;
        int removed = 0;
        for (int i = allowed; i < workers.size(); i++) {
            if (removeDuplicateWorkerPreservingCargo(level, workers.get(i))) removed++;
        }
        return removed;
    }

    private static boolean removeDuplicateWorkerPreservingCargo(ServerLevel level, FrontierWorkerEntity worker) {
        worker.getNavigation().stop();
        worker.setNoAi(false);
        worker.setInvulnerable(false);
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty()) {
            ItemEntity physical = new ItemEntity(level, worker.getX(), worker.getY(), worker.getZ(), carried.copy());
            if (!level.addFreshEntity(physical)) return false;
            worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        worker.discard();
        return true;
    }

    private static void repairPopulationAfterDuplicateCleanup(ServerLevel level, SettlementData data) {
        if (!SettlementOutpostLogisticsService.allRoutesLoaded(level, data)
                || !SettlementWorkshopService.allAssignmentsLoaded(level, data)
                || !SettlementAdvancedWorkshopService.allAssignmentsLoaded(level, data)) return;
        int transport = SettlementOutpostLogisticsService.loadedAssignedWorkerCount(level, data);
        int workshop = SettlementWorkshopService.loadedAssignedWorkerCount(level, data);
        int advanced = SettlementAdvancedWorkshopService.loadedAssignedWorkerCount(level, data);
        int actualPopulation = 1
                + workersByName(level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME).size()
                + workersByName(level, data, BuildingType.FARM, FARM_WORKER_NAME).size()
                + workersByName(level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME).size()
                + workersByName(level, data, BuildingType.MINE, MINE_WORKER_NAME).size()
                + transport + workshop + advanced;
        if (data.population() != actualPopulation) data.setPopulation(actualPopulation);
    }

    @FunctionalInterface
    private interface BuildingWork {
'''
replace_once(WORKER, anchor, methods, "worker duplicate helpers")

# Builder duplicates: remove the old permanent NoAI+invulnerable quarantine and physically clean extras.
replace_once(
    CONSTRUCTION,
    "import net.minecraft.world.entity.EquipmentSlot;\n",
    "import net.minecraft.world.entity.EquipmentSlot;\nimport net.minecraft.world.entity.item.ItemEntity;\n",
    "builder ItemEntity import",
)
old_builder = '''    public static FrontierWorkerEntity ensureBuilder(ServerLevel level, SettlementData data) {
        List<FrontierWorkerEntity> existing = findBuilders(level, data);
        if (!existing.isEmpty()) {
            FrontierWorkerEntity active = existing.getFirst();
            if (!active.entityTags().contains(BUILDER_TAG)) active.addTag(BUILDER_TAG);
            active.setNoAi(false);
            for (int i = 1; i < existing.size(); i++) {
                FrontierWorkerEntity duplicate = existing.get(i);
                duplicate.getNavigation().stop();
                duplicate.setNoAi(true);
                duplicate.setInvulnerable(true);
            }
            return active;
        }
'''
new_builder = '''    public static FrontierWorkerEntity ensureBuilder(ServerLevel level, SettlementData data) {
        reconcileBuilderDuplicates(level, data);
        List<FrontierWorkerEntity> existing = findBuilders(level, data);
        if (!existing.isEmpty()) {
            FrontierWorkerEntity active = existing.getFirst();
            if (!active.entityTags().contains(BUILDER_TAG)) active.addTag(BUILDER_TAG);
            active.setNoAi(false);
            active.setInvulnerable(false);
            return active;
        }
'''
replace_once(CONSTRUCTION, old_builder, new_builder, "builder duplicate quarantine")

builder_anchor = '''    static boolean returnBuilderHome(ServerLevel level, SettlementData data, FrontierWorkerEntity builder) {
'''
builder_helpers = '''    /**
     * Reclaims historical duplicate shared builders once their complete legal lookup envelope is loaded.
     * The first UUID-ordered builder remains authoritative. Extras are never frozen or made invulnerable;
     * their exact MAINHAND cargo is first materialized as an ItemEntity, and only then are they discarded.
     */
    public static int reconcileBuilderDuplicates(ServerLevel level, SettlementData data) {
        if (!builderAssignmentEvidenceLoaded(level, data)) return 0;
        List<FrontierWorkerEntity> builders = findBuilders(level, data);
        if (builders.isEmpty()) return 0;
        FrontierWorkerEntity active = builders.getFirst();
        if (!active.entityTags().contains(BUILDER_TAG)) active.addTag(BUILDER_TAG);
        active.setNoAi(false);
        active.setInvulnerable(false);
        int removed = 0;
        for (int i = 1; i < builders.size(); i++) {
            if (removeDuplicateBuilderPreservingCargo(level, builders.get(i))) removed++;
        }
        return removed;
    }

    private static boolean removeDuplicateBuilderPreservingCargo(ServerLevel level, FrontierWorkerEntity duplicate) {
        duplicate.getNavigation().stop();
        duplicate.setNoAi(false);
        duplicate.setInvulnerable(false);
        ItemStack carried = duplicate.getMainHandItem();
        if (!carried.isEmpty()) {
            ItemEntity physical = new ItemEntity(level, duplicate.getX(), duplicate.getY(), duplicate.getZ(), carried.copy());
            if (!level.addFreshEntity(physical)) return false;
            duplicate.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        duplicate.discard();
        return true;
    }

    static boolean returnBuilderHome(ServerLevel level, SettlementData data, FrontierWorkerEntity builder) {
'''
replace_once(CONSTRUCTION, builder_anchor, builder_helpers, "builder cleanup helpers")

# Companion lock is retarget only; companion binaries stay pinned.
lock = json.loads(LOCK.read_text(encoding="utf-8"))
lock["generated_at"] = "2026-08-30"
lock["target"]["frontier_settlement"] = "0.1.0-alpha.89"
note = (
    "Alpha.89 keeps every Alpha.88 companion binary pin unchanged. It performs loaded-evidence-safe "
    "save recovery for historical duplicate local production workers and the shared builder: UUID order "
    "keeps only the building-authorized physical count, exact MAINHAND cargo is materialized before an "
    "excess entity is discarded, stale builder NoAI/invulnerable quarantine is removed, population is "
    "repaired only under complete civilian evidence, and no unloaded resident, virtual item, force-load, "
    "teleport, companion code, or second worker authority is introduced."
)
if note not in lock.get("notes", []): lock.setdefault("notes", []).append(note)
LOCK.write_text(json.dumps(lock, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

# Human-readable acceptance boundary.
(ROOT / "DUPLICATE_WORKER_RECOVERY_ALPHA89.md").write_text('''# Frontier Settlement Alpha.89 — duplicate worker recovery

Version: `0.1.0-alpha.89`

Alpha.89 is a save-compatible lifecycle correction over Alpha.88. It adds no required settlement SavedData field.

## Local production workers
- A completed lumber camp, farm, quarry, or mine authorizes exactly one local production worker of that role.
- Historical excess workers are cleaned only while the complete work/storage lookup envelope is loaded.
- UUID order deterministically selects the workers that remain; an unloaded resident is never treated as missing or excess.
- Cleanup runs before the existing 600-tick worker-attraction gate, preventing an excess worker from being removed and immediately recreated.
- If an excess worker carries a real MAINHAND ItemStack, an exact ItemEntity copy must successfully enter the world before the hand is cleared and the worker is discarded.
- Population is recomputed after cleanup only when the existing transporter/workshop/advanced-worker evidence is also fully loaded.

## Shared builder
- The historical `NoAI=true` + `invulnerable=true` duplicate-builder quarantine is removed.
- Once the complete legal builder envelope is loaded, UUID order keeps one shared builder and excess builders are physically cleaned with the same exact MAINHAND cargo preservation rule.
- The retained builder is normalized to `NoAI=false` and `invulnerable=false`.
- Builder cleanup also runs from the ordinary settlement worker tick, so a duplicate can recover even when no new building project is currently active.

## Boundaries
- Natural Minecraft villagers are untouched; cleanup only targets FrontierWorkerEntity role/name evidence.
- No teleport, chunk force-load, virtual refund, duplicate cargo minting, or second resident ledger is introduced.
- Same-world update: fully close Minecraft, back up the world, install Alpha.89, and reopen the same save.
''', encoding="utf-8")

# Cumulative source audit wrapper: replay Alpha.88 against its canonical candidate, then assert Alpha.89.
(ROOT / "tools/test_alpha89_source.py").write_text(r'''#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
A88 = ROOT / "tools/test_alpha88_source.py"
ALPHA88_SHA = "4ca821aa75ba23dcce5e3348d4a8b35c37ced643"
LEGACY_FILES = {
    "projects/frontier-settlement/gradle.properties",
    "projects/frontier-settlement/COMPANION_LOCK.json",
    "projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementWorkerService.java",
    "projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java",
}
_real_read = Path.read_text
def alpha88_read(self, *args, **kwargs):
    try: rel = self.resolve().relative_to(REPO.resolve()).as_posix()
    except ValueError: rel = ""
    if rel in LEGACY_FILES:
        return subprocess.check_output(["git", "show", f"{ALPHA88_SHA}:{rel}"], cwd=REPO, text=True, encoding="utf-8")
    return _real_read(self, *args, **kwargs)
Path.read_text = alpha88_read
try:
    chain = _real_read(A88, encoding="utf-8").replace('print("Frontier Settlement alpha.23-88 cumulative source audit: PASS")', 'pass')
    ns = {"__file__": str(A88), "__name__": "__main__"}
    exec(compile(chain, str(A88), "exec"), ns, ns)
finally: Path.read_text = _real_read

def text(path): return Path(path).read_text(encoding="utf-8")
def must(src, tokens, label):
    for token in tokens:
        if token not in src: raise SystemExit(f"{label} missing: {token}")
def forbid(src, tokens, label):
    for token in tokens:
        if token in src: raise SystemExit(f"{label} forbidden: {token}")

props = text(ROOT / "gradle.properties")
worker = text(JAVA / "settlement/SettlementWorkerService.java")
construction = text(JAVA / "settlement/SettlementConstructionService.java")
lock = json.loads(text(ROOT / "COMPANION_LOCK.json"))

must(props, ("mod_version=0.1.0-alpha.89", "Alpha.89 duplicate-worker recovery"), "alpha.89 props")
must(worker, (
    "SettlementConstructionService.reconcileBuilderDuplicates(level, data);",
    "reconcileProductionDuplicates(server, level, data);",
    "trimExcessProductionWorkers",
    "removeDuplicateWorkerPreservingCargo",
    "physical = new ItemEntity",
    "if (!level.addFreshEntity(physical)) return false;",
    "worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);",
    "worker.discard();",
    "repairPopulationAfterDuplicateCleanup",
), "alpha.89 production duplicate cleanup")
if worker.index("reconcileProductionDuplicates(server, level, data);") > worker.index("if (server.getTickCount() % 600 == 0) tryAttractWorker"):
    raise SystemExit("alpha.89 duplicate cleanup must precede recruitment")
must(construction, (
    "public static int reconcileBuilderDuplicates",
    "removeDuplicateBuilderPreservingCargo",
    "active.setInvulnerable(false);",
    "duplicate.setInvulnerable(false);",
    "duplicate.discard();",
), "alpha.89 builder duplicate cleanup")
forbid(construction, ("duplicate.setNoAi(true);", "duplicate.setInvulnerable(true);"), "alpha.89 stale builder quarantine")
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.89": raise SystemExit("alpha.89 lock mismatch")
if not any("Alpha.89 keeps every Alpha.88 companion binary pin unchanged" in n for n in lock.get("notes", [])):
    raise SystemExit("alpha.89 lock rationale missing")
print("Frontier Settlement alpha.23-89 cumulative source audit: PASS")
''', encoding="utf-8")

(ROOT / "tools/test_alpha89_docs.py").write_text(r'''#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
A88 = ROOT / "tools/test_alpha88_docs.py"
ALPHA88_SHA = "4ca821aa75ba23dcce5e3348d4a8b35c37ced643"
_real_read = Path.read_text
def alpha88_read(self, *args, **kwargs):
    try: rel = self.resolve().relative_to(REPO.resolve()).as_posix()
    except ValueError: rel = ""
    if rel in {"projects/frontier-settlement/gradle.properties", "projects/frontier-settlement/COMPANION_LOCK.json"}:
        return subprocess.check_output(["git", "show", f"{ALPHA88_SHA}:{rel}"], cwd=REPO, text=True, encoding="utf-8")
    return _real_read(self, *args, **kwargs)
Path.read_text = alpha88_read
try:
    chain = _real_read(A88, encoding="utf-8").replace('print("Frontier Settlement alpha.88 canonical docs audit: PASS")', 'pass')
    ns = {"__file__": str(A88), "__name__": "__main__"}
    exec(compile(chain, str(A88), "exec"), ns, ns)
finally: Path.read_text = _real_read
note = (ROOT / "DUPLICATE_WORKER_RECOVERY_ALPHA89.md").read_text(encoding="utf-8")
props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
lock = json.loads((ROOT / "COMPANION_LOCK.json").read_text(encoding="utf-8"))
for token in ("0.1.0-alpha.89", "exactly one", "UUID order", "MAINHAND", "ItemEntity", "600-tick", "NoAI=true", "invulnerable=true", "Natural Minecraft villagers are untouched", "Same-world update"):
    if token not in note: raise SystemExit(f"alpha.89 note missing: {token}")
if "mod_version=0.1.0-alpha.89" not in props: raise SystemExit("alpha.89 version missing")
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.89": raise SystemExit("alpha.89 lock mismatch")
print("Frontier Settlement alpha.89 canonical docs audit: PASS")
''', encoding="utf-8")

print("Applied Frontier Settlement Alpha.89 duplicate-worker recovery patch")
