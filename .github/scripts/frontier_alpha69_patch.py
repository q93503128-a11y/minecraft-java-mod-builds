#!/usr/bin/env python3
from pathlib import Path
import json, re, subprocess

ROOT = Path('projects/frontier-settlement')
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement'

def read(path): return path.read_text(encoding='utf-8')
def write(path, text): path.write_text(text, encoding='utf-8')
def replace_once(path, old, new):
    s = read(path)
    if s.count(old) != 1:
        raise SystemExit(f'{path}: expected one match, got {s.count(old)}: {old[:100]!r}')
    write(path, s.replace(old, new, 1))
def sub_once(path, pattern, replacement, flags=0):
    s = read(path)
    n, count = re.subn(pattern, replacement, s, count=1, flags=flags)
    if count != 1:
        raise SystemExit(f'{path}: regex expected one match, got {count}: {pattern[:100]}')
    write(path, n)
def insert_before(path, marker, block):
    s = read(path)
    if block.splitlines()[0] in s: return
    if marker not in s: raise SystemExit(f'{path}: marker missing: {marker}')
    write(path, s.replace(marker, block.rstrip() + '\n\n' + marker, 1))

# Workshop: count every physical duplicate, but one UUID-sorted worker remains active.
p = JAVA / 'SettlementWorkshopService.java'
s = read(p)
if 'import java.util.Comparator;' not in s:
    s = s.replace('import java.util.HashSet;\n', 'import java.util.Comparator;\nimport java.util.HashSet;\n', 1)
    write(p, s)
sub_once(p, r'    public static int loadedAssignedWorkerCount\(ServerLevel level, SettlementData data\) \{.*?^    \}\n\n    public static BuildingRecord firstMissingLoadedAssignment', '''    public static int loadedAssignedWorkerCount(ServerLevel level, SettlementData data) {
        Set<java.util.UUID> ids = new HashSet<>();
        for (BuildingRecord workshop : data.buildings()) {
            if (workshop.buildingType() != BuildingType.WORKSHOP) continue;
            for (Villager worker : findAssignedWorkers(level, data, workshop)) ids.add(worker.getUUID());
        }
        return ids.size();
    }

    public static BuildingRecord firstMissingLoadedAssignment''', re.S | re.M)
replace_once(p, '            if (findAssignedWorker(level, data, workshop) == null) return workshop;\n',
                '            if (findAssignedWorkers(level, data, workshop).isEmpty()) return workshop;\n')
replace_once(p, '                || findAssignedWorker(level, data, workshop) != null) return null;\n',
                '                || !findAssignedWorkers(level, data, workshop).isEmpty()) return null;\n')
sub_once(p, r'    private static Villager findAssignedWorker\(ServerLevel level, SettlementData data, BuildingRecord workshop\) \{.*?^    \}\n\n    private static String assignmentTag', '''    private static Villager findAssignedWorker(ServerLevel level, SettlementData data, BuildingRecord workshop) {
        List<Villager> assigned = findAssignedWorkers(level, data, workshop);
        return assigned.isEmpty() ? null : assigned.getFirst();
    }

    private static List<Villager> findAssignedWorkers(ServerLevel level, SettlementData data, BuildingRecord workshop) {
        String assignment = assignmentTag(workshop);
        AABB area = SettlementWorkerService.workerRouteBounds(data, workshop.workCenter(), 12);
        List<Villager> assigned = level.getEntitiesOfClass(Villager.class, area,
                villager -> villager.entityTags().contains(WORKSHOP_WORKER_TAG)
                        && villager.entityTags().contains(assignment));
        assigned.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return assigned;
    }

    private static String assignmentTag''', re.S | re.M)

# Advanced workshop: same containment semantics.
p = JAVA / 'SettlementAdvancedWorkshopService.java'
s = read(p)
if 'import java.util.Comparator;' not in s:
    s = s.replace('import java.util.HashSet;\n', 'import java.util.Comparator;\nimport java.util.HashSet;\n', 1)
    write(p, s)
sub_once(p, r'    public static int loadedAssignedWorkerCount\(ServerLevel level, SettlementData data\) \{.*?^    \}\n\n    public static BuildingRecord firstMissingLoadedAssignment', '''    public static int loadedAssignedWorkerCount(ServerLevel level, SettlementData data) {
        Set<java.util.UUID> ids = new HashSet<>();
        for (BuildingRecord workshop : data.buildings()) {
            if (workshop.buildingType() != BuildingType.ADVANCED_WORKSHOP) continue;
            for (Villager worker : findAssignedWorkers(level, data, workshop)) ids.add(worker.getUUID());
        }
        return ids.size();
    }

    public static BuildingRecord firstMissingLoadedAssignment''', re.S | re.M)
replace_once(p, '            if (findAssignedWorker(level, data, workshop) == null) return workshop;\n',
                '            if (findAssignedWorkers(level, data, workshop).isEmpty()) return workshop;\n')
replace_once(p, '                || findAssignedWorker(level, data, workshop) != null) return null;\n',
                '                || !findAssignedWorkers(level, data, workshop).isEmpty()) return null;\n')
sub_once(p, r'    private static Villager findAssignedWorker\(ServerLevel level, SettlementData data, BuildingRecord workshop\) \{.*?^    \}\n\n    private static String assignmentTag', '''    private static Villager findAssignedWorker(ServerLevel level, SettlementData data, BuildingRecord workshop) {
        List<Villager> assigned = findAssignedWorkers(level, data, workshop);
        return assigned.isEmpty() ? null : assigned.getFirst();
    }

    private static List<Villager> findAssignedWorkers(ServerLevel level, SettlementData data, BuildingRecord workshop) {
        String assignment = assignmentTag(workshop);
        AABB area = SettlementWorkerService.workerRouteBounds(data, workshop.workCenter(), 12);
        List<Villager> assigned = level.getEntitiesOfClass(Villager.class, area,
                villager -> villager.entityTags().contains(ADVANCED_WORKER_TAG)
                        && villager.entityTags().contains(assignment));
        assigned.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return assigned;
    }

    private static String assignmentTag''', re.S | re.M)

# Outpost transport: count all loaded duplicates, zero alone is missing; one UUID-first worker drives the route.
p = JAVA / 'SettlementOutpostLogisticsService.java'
sub_once(p, r'    public static int loadedAssignedWorkerCount\(ServerLevel level, SettlementData data\) \{.*?^    \}\n\n    /\*\*', '''    public static int loadedAssignedWorkerCount(ServerLevel level, SettlementData data) {
        Set<java.util.UUID> ids = new HashSet<>();
        for (OutpostRecord outpost : data.outposts()) {
            if (!assignmentEvidenceLoaded(level, data, outpost)) continue;
            for (Villager worker : findAssignedWorkers(level, data, outpost)) ids.add(worker.getUUID());
        }
        return ids.size();
    }

    /**''', re.S | re.M)
replace_once(p, '            if (findAssignedWorker(level, data, outpost) == null) return outpost;\n',
                '            if (findAssignedWorkers(level, data, outpost).isEmpty()) return outpost;\n')
replace_once(p, '                || findAssignedWorker(level, data, outpost) != null) return null;\n',
                '                || !findAssignedWorkers(level, data, outpost).isEmpty()) return null;\n')
sub_once(p, r'    private static Villager findAssignedWorker\(ServerLevel level, SettlementData data, OutpostRecord outpost\) \{.*?^    \}\n\n    private static Villager findLegacyWorker', '''    private static Villager findAssignedWorker(ServerLevel level, SettlementData data, OutpostRecord outpost) {
        List<Villager> found = findAssignedWorkers(level, data, outpost);
        return found.isEmpty() ? null : found.getFirst();
    }

    private static List<Villager> findAssignedWorkers(ServerLevel level, SettlementData data, OutpostRecord outpost) {
        List<BlockPos> route = routeFromTown(data, outpost);
        return findAssignedWorkers(level, data, outpost, route);
    }

    private static Villager findAssignedWorker(ServerLevel level, SettlementData data,
                                               OutpostRecord outpost, List<BlockPos> route) {
        List<Villager> found = findAssignedWorkers(level, data, outpost, route);
        return found.isEmpty() ? null : found.getFirst();
    }

    private static List<Villager> findAssignedWorkers(ServerLevel level, SettlementData data,
                                                      OutpostRecord outpost, List<BlockPos> route) {
        if (route.isEmpty()) return List.of();
        String assignment = outpostTag(outpost);
        List<Villager> found = level.getEntitiesOfClass(Villager.class, routeBounds(data, outpost, route),
                villager -> villager.entityTags().contains(TRANSPORT_WORKER_TAG)
                        && villager.entityTags().contains(assignment));
        found.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return found;
    }

    private static Villager findLegacyWorker''', re.S | re.M)

# Version metadata.
p = ROOT / 'gradle.properties'
replace_once(p, 'mod_version=0.1.0-alpha.68', 'mod_version=0.1.0-alpha.69')
s = read(p)
needle = 'plus rest-anchor-aware civilian lifecycle evidence that uses the same work/storage/housing bounds for lookup and absence authority.'
if needle not in s: raise SystemExit('gradle description anchor missing')
s = s.replace(needle, needle[:-1] + ', plus multiplicity-safe assigned-worker accounting that counts every loaded physical duplicate while only zero workers can trigger replacement and UUID order selects the sole active worker.', 1)
write(p, s)

p = ROOT / 'COMPANION_LOCK.json'
lock = json.loads(read(p))
lock['target']['frontier_settlement'] = '0.1.0-alpha.69'
note = ('Alpha.69 contains historical duplicate assigned residents without destructive cleanup: fully loaded workshop, advanced-workshop and outpost assignment queries count every unique physical worker toward shared population, zero alone can authorize a food-funded replacement, and UUID order deterministically selects the sole active worker. No resident is deleted, no cargo is refunded or minted, and no new save ledger, force-load, teleport, worker family or logistics authority is added.')
if note not in lock.get('notes', []): lock.setdefault('notes', []).append(note)
write(p, json.dumps(lock, ensure_ascii=False, indent=2) + '\n')

# Canonical docs. Keep all Alpha.68 history and add Alpha.69 in front of it.
p = ROOT / 'CANONICAL_PLAN.md'
replace_once(p, 'Current canonical implementation: **0.1.0-alpha.68**', 'Current canonical implementation: **0.1.0-alpha.69**')
insert_before(p, '### Alpha.68 rest-anchor-aware civilian lifecycle evidence', '''### Alpha.69 historical duplicate-assignment containment

Alpha.69 hardens saves that may already contain more than one physical resident with the same specialist/outpost assignment tag. Alpha.67/68 prevent the known new false-missing paths, but they did not erase historical duplicates and the previous specialist population counters observed only one worker per assignment.

- workshop, advanced-workshop and outpost assignment queries expose the complete loaded matching-worker set;
- shared population reconciliation counts every unique physical assigned resident under the existing full loaded-evidence gates;
- replacement remains authorized only by **zero** matching workers, so two-or-more residents never consume another food4;
- actual specialist/transport work still uses exactly one worker, chosen deterministically by UUID order;
- ordinary production residents remain pooled and unchanged;
- existing exact MAINHAND death recovery remains unchanged; Alpha.69 does not delete, discard, retag or refund a duplicate resident or its cargo;
- no duplicate ledger, UUID reservation authority, virtual population/cargo, force-load, teleport, new worker family, key, UI or second logistics authority is added.

This is non-destructive containment, not historical-save cleanup. Long two-player repeated-death/night-rest/save-reload/reconnect runtime acceptance remains unfinished.''')

p = ROOT / 'COMPLETION_GAP_AUDIT.md'
replace_once(p, '현재 구현 기준: `0.1.0-alpha.68`', '현재 구현 기준: `0.1.0-alpha.69`')
insert_before(p, '### Alpha.68 야간 휴식 anchor / 민간 assignment evidence 감사', '''### Alpha.69 기존 중복 assignment containment 감사

- Alpha.67/68 이전 save에 동일 workshop / advanced workshop / outpost assignment 주민이 2명 이상 이미 존재할 수 있는 historical edge를 별도 containment했다.
- 완전 loaded evidence에서 동일 assignment의 실제 주민 전부를 UUID 중복 제거 후 population에 계상한다.
- matching resident가 정확히 0명일 때만 food4 replacement가 허용된다.
- 실제 작업/장거리 운송 권위는 UUID 정렬상 첫 주민 한 명만 사용한다.
- 기존 중복 주민 자체를 삭제/retag/refund하지 않으므로 숨은 실물 cargo를 파괴하거나 가상 환급하지 않는다.
- 실제 2인 반복 death/replacement/night-rest/save-reload/reconnect acceptance와 오래된 save의 물리적 중복 cleanup은 계속 미완료다.''')

p = ROOT / 'README.md'
replace_once(p, '## Current version: 0.1.0-alpha.68', '## Current version: 0.1.0-alpha.69')
s = read(p).replace('No new Alpha.68 key was added.', 'No new Alpha.69 key was added.', 1)
write(p, s)
insert_before(p, '## Alpha.68 — rest-anchor-aware civilian lifecycle evidence', '''## Alpha.69 — historical duplicate-assignment containment

Old saves can already contain two physical residents with one specialist/outpost assignment. Alpha.69 counts every loaded matching resident toward real population, authorizes replacement only when the matching set is empty, and chooses one UUID-sorted worker as the active specialist/transport authority. It never deletes a duplicate resident or mints/refunds cargo or food.''')

# Alpha.69 source audit: run Alpha.68 unchanged against a virtual compatibility view, then assert the new real semantics.
source_audit = r'''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A68=ROOT/'tools/test_alpha68_source.py'
def real_text(p): return Path(p).read_text(encoding='utf-8')
_real = Path.read_text
def legacy_view(self,*args,**kwargs):
    s=_real(self,*args,**kwargs)
    name=self.name
    if name=='gradle.properties': s=s.replace('mod_version=0.1.0-alpha.69','mod_version=0.1.0-alpha.68')
    elif name=='COMPANION_LOCK.json': s=s.replace('"frontier_settlement": "0.1.0-alpha.69"','"frontier_settlement": "0.1.0-alpha.68"')
    elif name in ('SettlementWorkshopService.java','SettlementAdvancedWorkshopService.java'):
        s=s.replace('|| !findAssignedWorkers(level, data, workshop).isEmpty()) return null;','|| findAssignedWorker(level, data, workshop) != null) return null;')
    elif name=='SettlementOutpostLogisticsService.java':
        s=s.replace('|| !findAssignedWorkers(level, data, outpost).isEmpty()) return null;','|| findAssignedWorker(level, data, outpost) != null) return null;')
    return s
Path.read_text=legacy_view
try:
    a=_real(A68,encoding='utf-8').replace("print('Frontier Settlement alpha.23-68 cumulative source audit: PASS')",'pass')
    ns={'__file__':str(A68),'__name__':'__main__'}; exec(compile(a,str(A68),'exec'),ns,ns)
finally:
    Path.read_text=_real

def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
workshop=real_text(JAVA/'settlement/SettlementWorkshopService.java'); advanced=real_text(JAVA/'settlement/SettlementAdvancedWorkshopService.java'); outpost=real_text(JAVA/'settlement/SettlementOutpostLogisticsService.java'); props=real_text(ROOT/'gradle.properties')
for label,s,role in (('workshop',workshop,'WORKSHOP_WORKER_TAG'),('advanced',advanced,'ADVANCED_WORKER_TAG')):
    must(s,('private static List<Villager> findAssignedWorkers(ServerLevel level, SettlementData data, BuildingRecord workshop)','assigned.sort(Comparator.comparing(villager -> villager.getUUID().toString()))','for (Villager worker : findAssignedWorkers(level, data, workshop)) ids.add(worker.getUUID());','if (findAssignedWorkers(level, data, workshop).isEmpty()) return workshop;','|| !findAssignedWorkers(level, data, workshop).isEmpty()) return null;',role),f'alpha.69 {label}')
must(outpost,('private static List<Villager> findAssignedWorkers(ServerLevel level, SettlementData data, OutpostRecord outpost)','for (Villager worker : findAssignedWorkers(level, data, outpost)) ids.add(worker.getUUID());','if (findAssignedWorkers(level, data, outpost).isEmpty()) return outpost;','|| !findAssignedWorkers(level, data, outpost).isEmpty()) return null;','found.sort(Comparator.comparing(villager -> villager.getUUID().toString()))'),'alpha.69 outpost')
for label,s in (('workshop',workshop),('advanced',advanced),('outpost',outpost)):
    forbid(s,('DUPLICATE_WORKER_LEDGER','WORKER_UUID_LEDGER','setChunkForced','forceChunk','teleportTo('),f'alpha.69 {label} no virtual/destructive authority')
must(props,('mod_version=0.1.0-alpha.69','multiplicity-safe assigned-worker accounting'),'alpha.69 props')
print('Frontier Settlement alpha.23-69 cumulative source audit: PASS')
'''
write(ROOT/'tools/test_alpha69_source.py', source_audit)

docs_audit = r'''#!/usr/bin/env python3
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; A68=ROOT/'tools/test_alpha68_docs.py'
_real=Path.read_text
def legacy_view(self,*args,**kwargs):
    s=_real(self,*args,**kwargs)
    if self.name in ('CANONICAL_PLAN.md','COMPLETION_GAP_AUDIT.md','README.md','COMPANION_LOCK.json'):
        s=s.replace('0.1.0-alpha.69','0.1.0-alpha.68').replace('Alpha.69','Alpha.68')
    return s
Path.read_text=legacy_view
try:
    a=_real(A68,encoding='utf-8').replace("print('Frontier Settlement alpha.68 canonical docs audit: PASS')",'pass')
    ns={'__file__':str(A68),'__name__':'__main__'}; exec(compile(a,str(A68),'exec'),ns,ns)
finally:
    Path.read_text=_real
def text(name): return (ROOT/name).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
canonical=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); readme=text('README.md'); lock=json.loads(text('COMPANION_LOCK.json'))
must(canonical,('Current canonical implementation: **0.1.0-alpha.69**','### Alpha.69 historical duplicate-assignment containment','replacement remains authorized only by **zero** matching workers','chosen deterministically by UUID order','Long two-player repeated-death/night-rest/save-reload/reconnect runtime acceptance remains unfinished'),'alpha.69 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.69`','### Alpha.69 기존 중복 assignment containment 감사','matching resident가 정확히 0명일 때만 food4 replacement','물리적 중복 cleanup은 계속 미완료'),'alpha.69 gap')
must(readme,('## Current version: 0.1.0-alpha.69','## Alpha.69 — historical duplicate-assignment containment','No new Alpha.69 key was added.'),'alpha.69 readme')
if lock.get('status')!='candidate_runtime_lock': raise SystemExit('alpha.69 companion lock overclaimed runtime status')
if lock.get('target',{}).get('frontier_settlement')!='0.1.0-alpha.69': raise SystemExit('alpha.69 lock target mismatch')
notes='\n'.join(lock.get('notes',[])); must(notes,('Alpha.69 contains historical duplicate assigned residents','No resident is deleted, no cargo is refunded or minted'),'alpha.69 lock note')
for forbidden in ('v0.2 complete','실플레이 검증 완료','full companion runtime: PASS'):
    if forbidden in readme: raise SystemExit(f'alpha.69 README overclaim: {forbidden}')
print('Frontier Settlement alpha.69 canonical docs audit: PASS')
'''
write(ROOT/'tools/test_alpha69_docs.py', docs_audit)

# Canonical workflow now verifies Alpha.69.
p = Path('.github/workflows/build-frontier-settlement.yml')
s = read(p)
s = s.replace('Alpha.68 cumulative source audit', 'Alpha.69 cumulative source audit', 1).replace('python3 tools/test_alpha68_source.py', 'python3 tools/test_alpha69_source.py', 1)
s = s.replace('Alpha.68 canonical docs audit', 'Alpha.69 canonical docs audit', 1).replace('python3 tools/test_alpha68_docs.py', 'python3 tools/test_alpha69_docs.py', 1)
write(p, s)

# Verify audits before committing.
subprocess.run(['python3', str(ROOT/'tools/test_alpha69_source.py')], check=True)
subprocess.run(['python3', str(ROOT/'tools/test_alpha69_docs.py')], check=True)
subprocess.run(['git','diff','--check'], check=True)

# Remove every temporary Alpha.69 patch mechanism; only canonical workflow/audits remain.
for temp in (
    Path('.github/workflows/frontier-alpha69-one-shot.yml'),
    Path('.github/workflows/frontier-alpha69-runner.yml'),
    Path('.github/scripts/frontier_alpha69_patch.py'),
    ROOT/'tools/alpha69_patch.py.gz',
    ROOT/'tools/alpha69.trigger',
):
    if temp.exists(): temp.unlink()
