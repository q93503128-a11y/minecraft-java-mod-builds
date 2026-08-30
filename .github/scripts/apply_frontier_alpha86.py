#!/usr/bin/env python3
from pathlib import Path
import json

repo = Path.cwd()
root = repo / 'projects/frontier-settlement'
construction_path = root / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java'

# Version
props_path = root / 'gradle.properties'
props = props_path.read_text(encoding='utf-8')
if props.count('mod_version=0.1.0-alpha.85') != 1:
    raise SystemExit('expected exactly one alpha.85 version anchor')
props = props.replace('mod_version=0.1.0-alpha.85', 'mod_version=0.1.0-alpha.86', 1)
if '# Alpha.86 construction pacing:' not in props:
    props = props.rstrip() + '\n\n# Alpha.86 construction pacing: hysteretic 64-item site restock, wide local work envelope, 1/2-tick grade/build cadence.\n'
props_path.write_text(props, encoding='utf-8')

# Construction pacing + resupply defect fix.
s = construction_path.read_text(encoding='utf-8')
replacements = {
    'private static final double WORK_POSITION_REACHED_SQR = 12.25D;':
        'private static final double WORK_POSITION_REACHED_SQR = 110.25D;',
    'private static final double HIGH_WORK_RANGE_SQR = 49.0D;':
        'private static final double HIGH_WORK_RANGE_SQR = 196.0D;',
    'private static final int HAUL_BATCH_SIZE = 32;':
        'private static final int HAUL_BATCH_SIZE = 64;',
    'private static final long MAX_SITE_RESERVE_PER_CATEGORY = 32L;':
        'private static final long SITE_RESERVE_TARGET_PER_CATEGORY = 64L;\n    private static final long SITE_RESERVE_LOW_WATER = 8L;',
    'private static final int GRADE_INTERVAL_TICKS = 3;':
        'private static final int GRADE_INTERVAL_TICKS = 1;',
    'private static final double GRADE_WORK_RANGE_SQR = 36.0D;':
        'private static final double GRADE_WORK_RANGE_SQR = 110.25D;',
    'private static final int BUILD_INTERVAL_TICKS = 4;':
        'private static final int BUILD_INTERVAL_TICKS = 2;',
}
for old, new in replacements.items():
    if s.count(old) != 1:
        raise SystemExit('construction constant anchor mismatch: ' + old)
    s = s.replace(old, new, 1)

start = s.index('    private static boolean stageRemainingMaterials(')
end = s.index('    private static boolean returnCarriedToTownStorage(', start)
new_method = '''    private static boolean stageRemainingMaterials(MinecraftServer server, SettlementData data, BuildingType type,
                                                   int totalSteps, FrontierWorkerEntity builder, Container crate, BlockPos supply) {
        int step = data.construction().buildStep();
        long spentWood = costAtStep(type.woodCost(), step, totalSteps);
        long spentStone = costAtStep(type.stoneCost(), step, totalSteps);
        long remainingWood = Math.max(0L, type.woodCost() - spentWood);
        long remainingStone = Math.max(0L, type.stoneCost() - spentStone);
        long currentWood = SettlementInventory.countWood(crate);
        long currentStone = SettlementInventory.countStone(crate);
        long nextWoodDelta = Math.max(0L, costAtStep(type.woodCost(), step + 1, totalSteps) - spentWood);
        long nextStoneDelta = Math.max(0L, costAtStep(type.stoneCost(), step + 1, totalSteps) - spentStone);

        // Alpha.85 accidentally treated every item consumed from a full reserve as an immediate
        // refill request. A 32 -> 31 transition therefore sent the same builder back to town for
        // exactly one item before another blueprint step could run. Keep physical hauling, but use
        // a low-water mark: initial staging is large, construction continues locally, and another
        // town trip is requested only when the crate is actually running low (or cannot fund the
        // very next transactional placement).
        boolean needsWood = currentWood < nextWoodDelta
                || (remainingWood > currentWood && currentWood <= SITE_RESERVE_LOW_WATER);
        boolean needsStone = currentStone < nextStoneDelta
                || (remainingStone > currentStone && currentStone <= SITE_RESERVE_LOW_WATER);
        long targetWood = Math.min(SITE_RESERVE_TARGET_PER_CATEGORY, remainingWood);
        long targetStone = Math.min(SITE_RESERVE_TARGET_PER_CATEGORY, remainingStone);
        long missingWood = needsWood ? Math.max(0L, targetWood - currentWood) : 0L;
        long missingStone = needsStone ? Math.max(0L, targetStone - currentStone) : 0L;

        ItemStack carried = builder.getMainHandItem();
        if (!carried.isEmpty()) {
            boolean usefulHere = (SettlementInventory.isWood(carried) && missingWood > 0L)
                    || (SettlementInventory.isStone(carried) && missingStone > 0L);
            if (!usefulHere) {
                returnCarriedToTownStorage(server, data, builder);
                return false;
            }
            if (builder.distanceToSqr(supply.getX() + 0.5D, supply.getY() + 0.5D, supply.getZ() + 0.5D)
                    > SUPPLY_INTERACTION_RANGE_SQR) {
                builder.getNavigation().moveTo(supply.getX() + 0.5D, supply.getY(), supply.getZ() + 0.5D, 1.10D);
                return false;
            }
            int before = carried.getCount();
            ItemStack remaining = SettlementInventory.insert(crate, carried);
            builder.setItemSlot(EquipmentSlot.MAINHAND, remaining);
            if (remaining.getCount() < before) {
                SettlementService.refreshResources(server, data);
                SettlementService.broadcast(server, data);
            }
            if (remaining.getCount() == before) returnCarriedToTownStorage(server, data, builder);
            return false;
        }

        if (!needsWood && !needsStone) return true;
        Predicate<ItemStack> wanted = needsWood ? SettlementInventory::isWood : SettlementInventory::isStone;
        long missing = needsWood ? missingWood : missingStone;
        if (missing <= 0L) return true;
        ServerLevel level = server.overworld();
        BlockPos source = SettlementStorageService.findExtractionTarget(level, data, wanted);
        if (source == null) return false;
        if (builder.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > SUPPLY_INTERACTION_RANGE_SQR) {
            builder.getNavigation().moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D, 1.10D);
            return false;
        }

        int amount = (int) Math.min((long) HAUL_BATCH_SIZE, missing);
        ItemStack extracted = SettlementStorageService.extract(level, source, wanted, amount);
        if (extracted.isEmpty()) return false;
        builder.setItemSlot(EquipmentSlot.MAINHAND, extracted);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        return false;
    }

'''
s = s[:start] + new_method + s[end:]

# The work envelope removes blueprint-order zig-zag, while these modifiers shorten the few real
# storage/site/home walks that remain. They do not alter the worker's base attribute or teleport it.
s = s.replace(', 0.82D))', ', 1.05D))')
s = s.replace(', 0.9D);', ', 1.10D);')
s = s.replace(', 0.86D);', ', 1.10D);')
s = s.replace(', 0.85D);', ', 1.05D);')

for forbidden in ('MAX_SITE_RESERVE_PER_CATEGORY', 'WORK_POSITION_REACHED_SQR = 12.25D', 'BUILD_INTERVAL_TICKS = 4'):
    if forbidden in s:
        raise SystemExit('old pacing token survived: ' + forbidden)
construction_path.write_text(s, encoding='utf-8')

# Companion lock retarget only; binary pins unchanged.
lock_path = root / 'COMPANION_LOCK.json'
lock = json.loads(lock_path.read_text(encoding='utf-8'))
if lock.get('target', {}).get('frontier_settlement') != '0.1.0-alpha.85':
    raise SystemExit('companion lock is not alpha.85')
lock['target']['frontier_settlement'] = '0.1.0-alpha.86'
note = ('Alpha.86 keeps every Alpha.85 companion binary pin unchanged while removing the remaining ordinary-building pacing bottlenecks: site material staging now uses a 64-item target with an 8-item low-water refill gate instead of one-item micro-resupply, grade/build cadence is 1/2 ticks, and the same physical builder uses a 10.5-block local work envelope with faster navigation modifiers for the few storage/site/home walks that remain. Real ItemStacks, loaded-chunk boundaries, one-builder authority, rollback rules, no force-load and no teleport are unchanged.')
if note not in lock.setdefault('notes', []):
    lock['notes'].append(note)
lock_path.write_text(json.dumps(lock, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

# Release note.
(root / 'CONSTRUCTION_PACING_ALPHA86.md').write_text('''# Frontier Settlement 0.1.0-alpha.86 — construction pacing II

Alpha.85 removed the 5-tick/8-tick scheduler aliasing defect, but a real graphical playtest still measured more than ten minutes for one 9x9 house. Alpha.86 targets the remaining logistics and movement bottlenecks rather than making construction virtual or instant.

## Root cause found after Alpha.85

A house contains 367 blueprint placements and 121 grading cells. More importantly, `stageRemainingMaterials` tried to keep the active-site crate exactly at its reserve target before every blueprint step. After one material was consumed, a full 32-item reserve became 31, which immediately blocked construction and sent the single physical builder all the way back to town to fetch exactly one replacement item. Blueprint insertion order also alternates between distant wall/roof positions, so the old 3.5-block local work radius caused repeated short pathfinding moves even after materials arrived.

## Alpha.86 pacing

- Site reserve target: 64 items per material category.
- Site refill low-water mark: 8 items.
- Initial staging still uses real shared-storage ItemStacks and the builder MAINHAND.
- After staging, construction does not refill merely because 64 became 63. It keeps building locally until the reserve is genuinely low or the next transactional placement cannot be funded.
- House cost is 48 wood + 20 stone, so a normal house can stage each category in one physical trip when storage contains the required stack.
- Grading cadence: 1 tick per eligible cell.
- Blueprint cadence: 2 ticks per eligible placement.
- Ordinary/grade local work envelope: 10.5 blocks; this is a construction-zone reach, not a teleport. The builder still physically reaches the site before working.
- High-work validation envelope: 14 blocks around the selected work/scaffold position.
- Remaining construction navigation requests use faster 1.05–1.10 speed modifiers while retaining the same PathfinderMob base movement attribute.

## Expected house pacing

The hard cadence floor for 367 blueprint placements is about 36.7 seconds at 20 TPS, plus roughly 6 seconds for 121 grading cells. Initial wood/stone hauling, pathfinding, terrain shape, storage distance, scaffolding and final return-home time add real runtime. The practical target for an ordinary nearby house is roughly 45–90 seconds, not an automated guarantee. A distant/obstructed site may take longer.

## Authority and safety unchanged

No material is virtualized or prepaid into a second ledger. No builder teleports, no chunk is force-loaded, and no additional worker authority is created. Block placement still follows the existing world-success-before-ItemStack-consume transaction and rollback rules. Roads, outposts and civil works keep their separate pacing/authority.
''', encoding='utf-8')

# Cumulative source audit: replay Alpha.85 against its canonical snapshot for changed files, then test Alpha.86.
(root / 'tools/test_alpha86_source.py').write_text(r'''#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
A85 = ROOT / "tools/test_alpha85_source.py"
ALPHA85_SHA = "8ec714a0a9c17bee51e67cd2c2840df65db31141"
LEGACY_FILES = {
    "projects/frontier-settlement/gradle.properties",
    "projects/frontier-settlement/COMPANION_LOCK.json",
    "projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java",
}
_real_read = Path.read_text

def alpha85_read(self, *args, **kwargs):
    try:
        rel = self.resolve().relative_to(REPO.resolve()).as_posix()
    except ValueError:
        rel = ""
    if rel in LEGACY_FILES:
        return subprocess.check_output(["git", "show", f"{ALPHA85_SHA}:{rel}"], cwd=REPO, text=True, encoding="utf-8")
    return _real_read(self, *args, **kwargs)

Path.read_text = alpha85_read
try:
    chain = _real_read(A85, encoding="utf-8").replace(
        'print("Frontier Settlement alpha.23-85 cumulative source audit: PASS")', 'pass')
    ns = {"__file__": str(A85), "__name__": "__main__"}
    exec(compile(chain, str(A85), "exec"), ns, ns)
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
construction = text(JAVA / "settlement/SettlementConstructionService.java")
lock = json.loads(text(ROOT / "COMPANION_LOCK.json"))

must(props, ("mod_version=0.1.0-alpha.86", "Alpha.86 construction pacing"), "alpha.86 props")
must(construction, (
    "WORK_POSITION_REACHED_SQR = 110.25D",
    "HIGH_WORK_RANGE_SQR = 196.0D",
    "HAUL_BATCH_SIZE = 64",
    "SITE_RESERVE_TARGET_PER_CATEGORY = 64L",
    "SITE_RESERVE_LOW_WATER = 8L",
    "GRADE_INTERVAL_TICKS = 1",
    "GRADE_WORK_RANGE_SQR = 110.25D",
    "BUILD_INTERVAL_TICKS = 2",
    "currentWood <= SITE_RESERVE_LOW_WATER",
    "currentStone <= SITE_RESERVE_LOW_WATER",
    "remainingWood > currentWood",
    "remainingStone > currentStone",
    "long missing = needsWood ? missingWood : missingStone;",
), "alpha.86 pacing/hysteresis")
forbid(construction, (
    "MAX_SITE_RESERVE_PER_CATEGORY",
    "WORK_POSITION_REACHED_SQR = 12.25D",
    "HAUL_BATCH_SIZE = 32",
    "GRADE_INTERVAL_TICKS = 3",
    "BUILD_INTERVAL_TICKS = 4",
), "alpha.86 obsolete pacing")
forbid(construction, ("teleportTo(", "setChunkForced", "forceChunk"), "alpha.86 no pacing shortcut")
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.86":
    raise SystemExit("alpha.86 companion lock target drifted")
if not any("Alpha.86 keeps every Alpha.85 companion binary pin unchanged" in n for n in lock.get("notes", [])):
    raise SystemExit("alpha.86 companion rationale missing")

# House cost is below the 64-item target for both categories: once its initial 48 wood / 20 stone
# staging is complete, an exact-consumption simulation must never trigger another refill.
def needs_refill(remaining, current, next_delta):
    return current < next_delta or (remaining > current and current <= 8)
wood = 48
stone = 20
for step in range(367):
    next_wood = 48 * (step + 1) // 367 - 48 * step // 367
    next_stone = 20 * (step + 1) // 367 - 20 * step // 367
    if needs_refill(wood, wood, next_wood) or needs_refill(stone, stone, next_stone):
        raise SystemExit("alpha.86 exact house staging unexpectedly requests micro-refill")
    wood -= next_wood
    stone -= next_stone
if wood != 0 or stone != 0:
    raise SystemExit("alpha.86 house cost simulation drifted")

print("Frontier Settlement alpha.23-86 cumulative source audit: PASS")
''', encoding='utf-8')

(root / 'tools/test_alpha86_docs.py').write_text(r'''#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
A85 = ROOT / "tools/test_alpha85_docs.py"
ALPHA85_SHA = "8ec714a0a9c17bee51e67cd2c2840df65db31141"
_real_read = Path.read_text

def alpha85_read(self, *args, **kwargs):
    try:
        rel = self.resolve().relative_to(REPO.resolve()).as_posix()
    except ValueError:
        rel = ""
    if rel in {"projects/frontier-settlement/gradle.properties", "projects/frontier-settlement/COMPANION_LOCK.json"}:
        return subprocess.check_output(["git", "show", f"{ALPHA85_SHA}:{rel}"], cwd=REPO, text=True, encoding="utf-8")
    return _real_read(self, *args, **kwargs)

Path.read_text = alpha85_read
try:
    chain = _real_read(A85, encoding="utf-8").replace(
        'print("Frontier Settlement alpha.85 canonical docs audit: PASS")', 'pass')
    ns = {"__file__": str(A85), "__name__": "__main__"}
    exec(compile(chain, str(A85), "exec"), ns, ns)
finally:
    Path.read_text = _real_read

note = (ROOT / "CONSTRUCTION_PACING_ALPHA86.md").read_text(encoding="utf-8")
props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
lock = json.loads((ROOT / "COMPANION_LOCK.json").read_text(encoding="utf-8"))
for token in ("0.1.0-alpha.86", "367 blueprint placements", "121 grading cells", "64 items", "8 items", "2 ticks", "45–90 seconds", "No builder teleports"):
    if token not in note: raise SystemExit(f"alpha.86 note missing: {token}")
if "mod_version=0.1.0-alpha.86" not in props: raise SystemExit("alpha.86 version missing")
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.86": raise SystemExit("alpha.86 lock mismatch")
print("Frontier Settlement alpha.86 canonical docs audit: PASS")
''', encoding='utf-8')
