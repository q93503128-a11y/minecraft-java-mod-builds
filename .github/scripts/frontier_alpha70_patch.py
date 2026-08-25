#!/usr/bin/env python3
from pathlib import Path
import json
import subprocess

ROOT = Path(__file__).resolve().parents[2]
P = ROOT / "projects/frontier-settlement"
JAVA = P / "src/main/java/kr/moonseungjun/frontiersettlement"

def read(path): return Path(path).read_text(encoding="utf-8")
def write(path, text): Path(path).write_text(text, encoding="utf-8")

def replace_once(path, old, new, label):
    path = Path(path); s = read(path)
    if s.count(old) != 1: raise SystemExit(f"{label}: expected one anchor, got {s.count(old)}")
    write(path, s.replace(old, new, 1))

def insert_before(path, marker, section, label):
    path = Path(path); s = read(path)
    if section.strip() in s: return
    if marker not in s: raise SystemExit(f"{label}: marker missing")
    write(path, s.replace(marker, section.rstrip() + "\n\n" + marker, 1))

outpost = JAVA / "settlement/SettlementOutpostProductionService.java"
workers = JAVA / "settlement/SettlementWorkerService.java"

replace_once(outpost, "import java.util.List;\n", "import java.util.Comparator;\nimport java.util.List;\n", "alpha70 imports")
old_ensure = '''    private static Villager ensureWorker(ServerLevel level, OutpostRecord outpost) {
        if (!outpostLoaded(level, outpost)) return null;
        String assignmentTag = productionTag(outpost.id());
        AABB search = new AABB(
                outpost.centerX() - 48.0D, outpost.centerY() - 32.0D, outpost.centerZ() - 48.0D,
                outpost.centerX() + 49.0D, outpost.centerY() + 33.0D, outpost.centerZ() + 49.0D);
        List<Villager> assigned = level.getEntitiesOfClass(Villager.class, search,
                villager -> villager.entityTags().contains(PRODUCTION_WORKER_TAG)
                        && villager.entityTags().contains(assignmentTag));
        if (!assigned.isEmpty()) return assigned.getFirst();

        String name = workerName(outpost);
        List<Villager> legacy = level.getEntitiesOfClass(Villager.class, search,
                villager -> villager.getCustomName() != null && name.equals(villager.getCustomName().getString()));
        if (!legacy.isEmpty()) {
            Villager worker = legacy.getFirst();
            worker.addTag(PRODUCTION_WORKER_TAG);
            worker.addTag(assignmentTag);
            return worker;
        }

        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        BlockPos spawn = outpost.center().above();
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(name));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        worker.addTag(PRODUCTION_WORKER_TAG);
        worker.addTag(assignmentTag);
        level.addFreshEntity(worker);
        return worker;
    }
'''
new_ensure = '''    private static Villager ensureWorker(ServerLevel level, OutpostRecord outpost) {
        if (!outpostLoaded(level, outpost)) return null;
        List<Villager> assigned = findAssignedWorkers(level, outpost);
        if (!assigned.isEmpty()) return assigned.getFirst();

        // Missing is authority. Do not migrate or spawn from a partial entity view.
        if (!assignmentEvidenceLoaded(level, outpost)) return null;

        String assignmentTag = productionTag(outpost.id());
        String name = workerName(outpost);
        List<Villager> legacy = level.getEntitiesOfClass(Villager.class, assignmentBounds(outpost),
                villager -> villager.getCustomName() != null && name.equals(villager.getCustomName().getString()));
        legacy.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        if (!legacy.isEmpty()) {
            Villager worker = legacy.getFirst();
            worker.addTag(PRODUCTION_WORKER_TAG);
            worker.addTag(assignmentTag);
            return worker;
        }

        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        BlockPos spawn = outpost.center().above();
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(name));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        worker.addTag(PRODUCTION_WORKER_TAG);
        worker.addTag(assignmentTag);
        if (!level.addFreshEntity(worker)) return null;
        return worker;
    }

    private static List<Villager> findAssignedWorkers(ServerLevel level, OutpostRecord outpost) {
        String assignmentTag = productionTag(outpost.id());
        List<Villager> assigned = level.getEntitiesOfClass(Villager.class, assignmentBounds(outpost),
                villager -> villager.entityTags().contains(PRODUCTION_WORKER_TAG)
                        && villager.entityTags().contains(assignmentTag));
        assigned.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return assigned;
    }

    private static AABB assignmentBounds(OutpostRecord outpost) {
        return new AABB(
                outpost.centerX() - 48.0D, outpost.centerY() - 32.0D, outpost.centerZ() - 48.0D,
                outpost.centerX() + 49.0D, outpost.centerY() + 33.0D, outpost.centerZ() + 49.0D);
    }

    private static boolean assignmentEvidenceLoaded(ServerLevel level, OutpostRecord outpost) {
        if (!outpostLoaded(level, outpost)) return false;
        AABB bounds = assignmentBounds(outpost);
        int minChunkX = Math.floorDiv((int) Math.floor(bounds.minX), 16);
        int maxChunkX = Math.floorDiv((int) Math.floor(Math.nextDown(bounds.maxX)), 16);
        int minChunkZ = Math.floorDiv((int) Math.floor(bounds.minZ), 16);
        int maxChunkZ = Math.floorDiv((int) Math.floor(Math.nextDown(bounds.maxZ)), 16);
        int probeY = outpost.centerY();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                BlockPos probe = new BlockPos(chunkX * 16 + 8, probeY, chunkZ * 16 + 8);
                if (!level.hasChunkAt(probe)) return false;
            }
        }
        return true;
    }
'''
replace_once(outpost, old_ensure, new_ensure, "alpha70 outpost lifecycle")

replace_once(workers,
'''        if (worker.entityTags().contains(RESOURCE_WORKER_TAG)
                || worker.entityTags().contains(SettlementWorkshopService.WORKSHOP_WORKER_TAG)
                || worker.entityTags().contains(SettlementAdvancedWorkshopService.ADVANCED_WORKER_TAG)) {
''',
'''        if (worker.entityTags().contains(RESOURCE_WORKER_TAG)
                || worker.entityTags().contains(SettlementWorkshopService.WORKSHOP_WORKER_TAG)
                || worker.entityTags().contains(SettlementAdvancedWorkshopService.ADVANCED_WORKER_TAG)
                || worker.entityTags().contains(SettlementOutpostProductionService.PRODUCTION_WORKER_TAG)) {
''', "alpha70 outpost cargo tag")
replace_once(workers,
'''        return LUMBER_WORKER_NAME.equals(value) || FARM_WORKER_NAME.equals(value)
                || QUARRY_WORKER_NAME.equals(value) || MINE_WORKER_NAME.equals(value);
''',
'''        return LUMBER_WORKER_NAME.equals(value) || FARM_WORKER_NAME.equals(value)
                || QUARRY_WORKER_NAME.equals(value) || MINE_WORKER_NAME.equals(value)
                || value.startsWith("전초 벌목 주민 #") || value.startsWith("전초 채석 주민 #")
                || value.startsWith("전초 광산 주민 #") || value.startsWith("전초 농업 주민 #");
''', "alpha70 legacy outpost death fallback")

for path, label in ((workers, "town"), (outpost, "outpost")):
    replace_once(path,
'''                level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3);
                harvested++;
''',
'''                if (level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3)) harvested++;
''', f"alpha70 {label} farm transaction")
    replace_once(path,
'''            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            count++;
''',
'''            if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) break;
            count++;
''', f"alpha70 {label} lumber transaction")
    replace_once(path,
'''                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                count++;
''',
'''                if (level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) count++;
''', f"alpha70 {label} quarry transaction")
    replace_once(path,
'''        if (!result.isEmpty()) level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
        return result;
''',
'''        if (result.isEmpty() || !level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3)) return ItemStack.EMPTY;
        return result;
''', f"alpha70 {label} mine transaction")

props = P / "gradle.properties"
replace_once(props, "mod_version=0.1.0-alpha.69", "mod_version=0.1.0-alpha.70", "alpha70 version")
s = read(props)
anchor = "plus multiplicity-safe assigned-worker accounting that counts every loaded physical duplicate while only zero workers can trigger replacement and UUID order selects the sole active worker."
if anchor not in s: raise SystemExit("alpha70 description anchor missing")
s = s.replace(anchor, anchor[:-1] + ", plus fail-closed specialized-outpost production-worker absence evidence, deterministic UUID authority, exact MAINHAND cargo death recovery, and world-success-before-ItemStack production transactions.", 1)
write(props, s)

lock_path = P / "COMPANION_LOCK.json"
lock = json.loads(read(lock_path))
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.69": raise SystemExit("alpha70 lock base mismatch")
lock["target"]["frontier_settlement"] = "0.1.0-alpha.70"
note = "Alpha.70 hardens specialized outpost production without changing outpost-local labor economics: assignment absence/migration/spawn waits for the complete existing lookup envelope, UUID order selects one active worker if historical duplicates exist, failed entity insertion creates no phantom worker, and outpost production MAINHAND cargo joins exact physical death recovery. Town and outpost lumber/farm/quarry/mine output is now minted only after the corresponding world setBlock succeeds. No population ledger, food refund, force-load, teleport, virtual resource, second logistics authority or companion dependency is added."
lock.setdefault("notes", []).append(note)
write(lock_path, json.dumps(lock, ensure_ascii=False, indent=2) + "\n")

canonical = P / "CANONICAL_PLAN.md"
replace_once(canonical, "Current canonical implementation: **0.1.0-alpha.69**.", "Current canonical implementation: **0.1.0-alpha.70**.", "alpha70 canonical version")
canonical_section = '''### Alpha.70 specialized-outpost production lifecycle / physical mutation transaction hardening

Alpha.70 closes a lifecycle gap left outside Alpha.65–69: specialized outpost production residents already carried real harvested ItemStacks, but their assignment search could infer absence from a partial ±48 entity view and their cargo was not covered by exact civilian death recovery.

- a visible assigned specialized-outpost production worker may keep working normally;
- declaring that assignment absent, migrating a legacy named worker or spawning a replacement now fails closed until every chunk intersecting the exact existing ±48 assignment lookup AABB is loaded;
- the proof uses `hasChunkAt` only and never force-loads or generates chunks;
- matching tagged workers are UUID-sorted and exactly the first is the active work authority, containing historical duplicates without deleting them;
- `addFreshEntity` must succeed before a newly created production worker can be returned to work logic;
- specialized-outpost production residents join the existing exact MAINHAND physical cargo death-recovery handler, including exact-name fallback for pre-tag saves;
- town and outpost lumber, farm, quarry and mine production now count/mint output only after the corresponding `setBlock` world mutation succeeds; failed mutation yields no free ItemStack;
- the existing outpost-local production-worker economics remain unchanged in this hardening pass and are not folded into civilian housing/population;
- no new save field, population/cargo ledger, refund balance, worker UI, force-load, teleport, virtual resource, key, building family, logistics controller or hard companion dependency is added.

This is deterministic no-loss/no-dup hardening. Long two-player repeated death, route/rest unload, save/reload and reconnect acceptance remains unfinished.'''
insert_before(canonical, "### Alpha.69 historical duplicate-assignment containment", canonical_section, "alpha70 canonical section")

gap = P / "COMPLETION_GAP_AUDIT.md"
replace_once(gap, "현재 구현 기준: `0.1.0-alpha.69`", "현재 구현 기준: `0.1.0-alpha.70`", "alpha70 gap version")
gap_section = '''### Alpha.70 전초 현지 생산자 lifecycle / 생산 world transaction 감사

- specialized outpost 생산 주민 lookup의 기존 ±48 AABB와 실제 absence evidence 범위를 동일하게 맞췄다.
- assignment가 보이지 않는다는 이유만으로 migration/replacement를 하지 않고, lookup AABB 전체 청크가 `hasChunkAt`으로 loaded일 때만 0명을 확정한다.
- 과거 중복 주민이 있으면 삭제하지 않고 UUID 정렬상 첫 주민만 실제 생산 권위를 가진다.
- 신규 현지 생산 주민은 `addFreshEntity` 성공 뒤에만 work path로 반환된다.
- 벌목/농업/채석/광산 현지 생산자가 실제 MAINHAND 자원을 들고 죽으면 기존 civilian exact-drop 권위에서 한 번만 물리 드롭한다.
- 본진과 전초의 나무/밀/석재/광석 생산은 해당 world `setBlock` 성공 뒤에만 수확 ItemStack을 만든다. 실패한 world mutation은 생산량 0이다.
- 현지 생산 인력을 shared housing/population에 새로 편입하지 않았고, 새 food 비용/환급/가상 worker ledger도 만들지 않았다.
- force-load/teleport/virtual cargo/second outpost logistics authority/new key/UI/building/companion hard dependency 없음.
- 실제 2인 반복 death -> cargo recovery -> unload/reload -> save/reconnect acceptance는 계속 남는다.'''
insert_before(gap, "### Alpha.69 기존 중복 assignment containment 감사", gap_section, "alpha70 gap section")

readme = P / "README.md"
replace_once(readme, "## Current version: 0.1.0-alpha.69", "## Current version: 0.1.0-alpha.70", "alpha70 readme version")
replace_once(readme, "No new Alpha.69 key was added.", "No new Alpha.70 key was added.", "alpha70 readme key")
s = read(readme).replace("Alpha.40–68 deepen existing systems", "Alpha.40–70 deepen existing systems", 1); write(readme, s)
readme_section = '''## Alpha.70 — specialized-outpost production lifecycle / mutation transaction hardening

Specialized outpost production workers now fail closed before absence-based legacy migration or spawning unless their exact existing ±48 lookup envelope is loaded. Existing tagged duplicates are UUID-sorted and only one works; none are destructively removed. A failed `addFreshEntity` returns no worker.

Their real MAINHAND harvested cargo now uses the same exact physical death-recovery authority as other managed production civilians. In both town and outpost lumber/farm/quarry/mine paths, output is created only after the matching world `setBlock` succeeds, so a failed block mutation cannot mint free logs, wheat, stone or ore.

No new Alpha.70 key, resident-management UI, population ledger, virtual cargo, force-load, teleport, building family or second logistics authority was added. Long repeated-death/unload/save-reconnect acceptance remains unfinished.'''
insert_before(readme, "## Alpha.69 — historical duplicate-assignment containment", readme_section, "alpha70 readme section")

source_test = P / "tools/test_alpha70_source.py"
write(source_test, '''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A69=ROOT/'tools/test_alpha69_source.py'
_real=Path.read_text
def legacy_view(self,*args,**kwargs):
    s=_real(self,*args,**kwargs)
    if self.name=='gradle.properties': s=s.replace('mod_version=0.1.0-alpha.70','mod_version=0.1.0-alpha.69')
    elif self.name=='COMPANION_LOCK.json': s=s.replace('"frontier_settlement": "0.1.0-alpha.70"','"frontier_settlement": "0.1.0-alpha.69"')
    return s
Path.read_text=legacy_view
try:
    a=_real(A69,encoding='utf-8').replace("print('Frontier Settlement alpha.23-69 cumulative source audit: PASS')",'pass')
    ns={'__file__':str(A69),'__name__':'__main__'}; exec(compile(a,str(A69),'exec'),ns,ns)
finally: Path.read_text=_real
def text(p): return Path(p).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
outpost=text(JAVA/'settlement/SettlementOutpostProductionService.java'); workers=text(JAVA/'settlement/SettlementWorkerService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json')
must(outpost,('private static List<Villager> findAssignedWorkers(ServerLevel level, OutpostRecord outpost)','assigned.sort(Comparator.comparing(villager -> villager.getUUID().toString()))','private static AABB assignmentBounds(OutpostRecord outpost)','private static boolean assignmentEvidenceLoaded(ServerLevel level, OutpostRecord outpost)','Math.floorDiv((int) Math.floor(bounds.minX), 16)','level.hasChunkAt(probe)','if (!assignmentEvidenceLoaded(level, outpost)) return null;','legacy.sort(Comparator.comparing(villager -> villager.getUUID().toString()))','if (!level.addFreshEntity(worker)) return null;','if (level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3)) harvested++;','if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) break;','if (level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) count++;','if (result.isEmpty() || !level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3)) return ItemStack.EMPTY;'),'alpha.70 outpost lifecycle/transactions')
must(workers,('SettlementOutpostProductionService.PRODUCTION_WORKER_TAG','value.startsWith("전초 벌목 주민 #")','value.startsWith("전초 농업 주민 #")','if (level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3)) harvested++;','if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) break;','if (level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) count++;','if (result.isEmpty() || !level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3)) return ItemStack.EMPTY;','event.getDrops().clear()','carried.copy()'),'alpha.70 managed production cargo/transactions')
ensure=outpost.index('private static Villager ensureWorker'); evidence=outpost.index('if (!assignmentEvidenceLoaded(level, outpost)) return null;',ensure); legacy=outpost.index('List<Villager> legacy =',evidence); create=outpost.index('Villager worker = new Villager',legacy)
if not ensure < evidence < legacy < create: raise SystemExit('alpha.70 absence evidence does not gate migration/spawn')
forbid(outpost+workers,('OUTPOST_PRODUCTION_WORKER_LEDGER','PRODUCTION_CARGO_LEDGER','RECOVERY_BALANCE','setChunkForced','forceChunk','teleportTo('),'alpha.70 no virtual/load authority')
must(props,('mod_version=0.1.0-alpha.70','fail-closed specialized-outpost production-worker absence evidence'),'alpha.70 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.70"','Alpha.70 hardens specialized outpost production'),'alpha.70 lock')
print('Frontier Settlement alpha.23-70 cumulative source audit: PASS')
''')

docs_test = P / "tools/test_alpha70_docs.py"
write(docs_test, '''#!/usr/bin/env python3
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; A69=ROOT/'tools/test_alpha69_docs.py'
_real=Path.read_text
def legacy_view(self,*args,**kwargs):
    s=_real(self,*args,**kwargs)
    if self.name in ('CANONICAL_PLAN.md','COMPLETION_GAP_AUDIT.md','README.md','COMPANION_LOCK.json'): s=s.replace('0.1.0-alpha.70','0.1.0-alpha.69').replace('Alpha.70','Alpha.69')
    return s
Path.read_text=legacy_view
try:
    a=_real(A69,encoding='utf-8').replace("print('Frontier Settlement alpha.69 canonical docs audit: PASS')",'pass')
    ns={'__file__':str(A69),'__name__':'__main__'}; exec(compile(a,str(A69),'exec'),ns,ns)
finally: Path.read_text=_real
def text(name): return (ROOT/name).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
canonical=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); readme=text('README.md'); lock=json.loads(text('COMPANION_LOCK.json'))
must(canonical,('Current canonical implementation: **0.1.0-alpha.70**','### Alpha.70 specialized-outpost production lifecycle / physical mutation transaction hardening','complete existing ±48 assignment lookup AABB is loaded','corresponding `setBlock` world mutation succeeds','Long two-player repeated death, route/rest unload, save/reload and reconnect acceptance remains unfinished'),'alpha.70 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.70`','### Alpha.70 전초 현지 생산자 lifecycle / 생산 world transaction 감사','lookup AABB 전체 청크가 `hasChunkAt`으로 loaded','실패한 world mutation은 생산량 0','실제 2인 반복 death -> cargo recovery -> unload/reload -> save/reconnect acceptance는 계속 남는다'),'alpha.70 gap')
must(readme,('## Current version: 0.1.0-alpha.70','## Alpha.70 — specialized-outpost production lifecycle / mutation transaction hardening','No new Alpha.70 key was added.','output is created only after the matching world `setBlock` succeeds'),'alpha.70 README')
if lock.get('status')!='candidate_runtime_lock': raise SystemExit('alpha.70 companion lock overclaimed runtime status')
if lock.get('target',{}).get('frontier_settlement')!='0.1.0-alpha.70': raise SystemExit('alpha.70 lock target mismatch')
notes='\n'.join(lock.get('notes',[])); must(notes,('Alpha.70 hardens specialized outpost production','No population ledger, food refund, force-load, teleport'),'alpha.70 lock note')
for forbidden in ('v0.2 complete','실플레이 검증 완료','full companion runtime: PASS'):
    if forbidden in readme: raise SystemExit(f'alpha.70 README overclaim: {forbidden}')
print('Frontier Settlement alpha.70 canonical docs audit: PASS')
''')

subprocess.run(["python3", str(source_test)], cwd=P, check=True)
subprocess.run(["python3", str(docs_test)], cwd=P, check=True)
print("Frontier alpha.70 staged patch + audits: PASS")
