#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(name):
    return (ROOT / name).read_text(encoding='utf-8')

def write(name, value):
    (ROOT / name).write_text(value, encoding='utf-8')

def replace_once(value, old, new, label):
    count = value.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected one anchor, found {count}: {old}')
    return value.replace(old, new, 1)

def insert_once(value, anchor, block, marker, label):
    if marker in value:
        return value
    if value.count(anchor) != 1:
        raise SystemExit(f'{label}: insertion anchor mismatch: {anchor}')
    return value.replace(anchor, block.rstrip() + '\n\n' + anchor, 1)

canonical = read('CANONICAL_PLAN.md')
canonical = replace_once(canonical,
    'Current canonical implementation: **0.1.0-alpha.66**.',
    'Current canonical implementation: **0.1.0-alpha.67**.',
    'canonical version')
alpha67_canonical = r'''### Alpha.67 fail-closed outpost transporter assignment evidence

Alpha.67 closes the same unloaded-entity false-absence class for road-bound outpost transporters that Alpha.66 closed for local civilians, without changing the physical transport authority itself.

- `findAssignedWorker` and legacy-worker discovery search the persisted road/stockpile `routeBounds` with the existing 32-block route-search margin;
- population reconciliation, loaded transporter counting, missing-assignment inference, legacy reassignment and replacement spawn now require the **exact transporter lookup envelope** to be loaded before absence is authoritative;
- the assignment proof checks every X/Z chunk intersecting that same `routeBounds` AABB using `hasChunkAt` only; it never generates or force-loads a chunk;
- if any lookup-envelope chunk is unloaded, Frontier freezes absence/replacement instead of treating a hidden tagged transporter as dead and consuming food4 for a duplicate assignment;
- normal transporter work deliberately continues to use the existing persisted-road `routeFullyLoaded`/waypoint `hasChunkAt` checks, so **Transport workers belong to a specific outpost** and **pause at unloaded route boundaries** exactly as before;
- Alpha.42 deferred logistics pacing is not widened into a force-loaded simulation and Alpha.63 exact MAINHAND cargo death recovery remains the sole transporter death-cargo authority;
- the dangerous-outpost sentry keeps its separate loaded-area proof whose 32-block evidence margin already matches its 32-block sentry lookup radius;
- Alpha.27 remains the **single authority for outpost transport** and **there is still only one authority for long-distance outpost transport**;
- no new SavedData field, transporter UUID ledger, reservation, route controller, worker family, key, UI, currency, virtual cargo, force-load, teleport or companion dependency is added.

This closes the deterministic `route centers loaded -> transporter in an unloaded lookup-margin chunk -> false missing -> duplicate replacement` path. **Long two-player repeated-death/save-reload runtime acceptance remains unfinished.**'''
canonical = insert_once(canonical,
    '### Alpha.66 loaded-evidence-safe civilian lifecycle authority',
    alpha67_canonical,
    '### Alpha.67 fail-closed outpost transporter assignment evidence',
    'canonical Alpha.67 section')
canonical = replace_once(canonical,
    '## 14. Current playable slice after Alpha.66',
    '## 14. Current playable slice after Alpha.67',
    'canonical current slice heading')
canonical = replace_once(canonical,
    '## 15. Unfinished original-scope priorities after Alpha.66',
    '## 15. Unfinished original-scope priorities after Alpha.67',
    'canonical priorities heading')
canonical = replace_once(canonical,
    '2. Alpha.62–66 physical military/transporter/local-civilian cargo recovery and replacement boundaries are statically hardened; save-reload, route-unload, repeated death/replacement and no-dup/no-loss acceptance remain;',
    '2. Alpha.62–67 physical military/transporter/local-civilian cargo recovery and replacement boundaries are statically hardened; Alpha.67 additionally fails closed when transporter lookup-envelope chunks are unloaded; save-reload, route-unload, repeated death/replacement and no-dup/no-loss acceptance remain;',
    'canonical priority range')
slice_anchor = '- **Alpha.51 DOMAIN 17×17 / ±7 selected-area cut/fill with Alpha.50 earth/imported-dirt authority plus bounded 3–7 block exposed-edge retaining walls made from exact physically hauled COBBLESTONE**.'
slice_insert = '''- Alpha.66 loaded-evidence-safe local civilian population/replacement authority + food-funded advanced-artisan lifecycle;
- Alpha.67 fail-closed outpost transporter assignment evidence matching the exact transporter lookup envelope, without changing normal route-bound physical movement;'''
canonical = insert_once(canonical, slice_anchor, slice_insert,
    '- Alpha.67 fail-closed outpost transporter assignment evidence matching',
    'canonical current slice Alpha.67')
write('CANONICAL_PLAN.md', canonical)

gap = read('COMPLETION_GAP_AUDIT.md')
gap = replace_once(gap, '현재 구현 기준: `0.1.0-alpha.66`', '현재 구현 기준: `0.1.0-alpha.67`', 'gap version')
alpha67_gap = r'''### Alpha.67 전초 운송 주민 assignment evidence 감사

- 기존 전초 운송 주민의 outpost-specific tag, persisted road, MAINHAND cargo와 Alpha.27 단일 물류 권위는 그대로 유지;
- `findAssignedWorker`/legacy lookup은 기존 32블록 margin을 포함한 `routeBounds` AABB를 사용;
- Alpha.66 이전에는 `routeFullyLoaded`가 road center chunk만 확인한 뒤 missing/replacement를 허용해, transporter가 인접 lookup-margin chunk에 있으나 그 chunk만 unloaded인 경우 false missing이 가능했음;
- Alpha.67은 population reconciliation용 count, `allRoutesLoaded`, missing-assignment 판정, legacy reassignment, replacement spawn에 같은 `routeBounds` 전체 chunk evidence를 요구;
- evidence는 `hasChunkAt`만 호출하고 force-load/generation/teleport 없음;
- evidence가 불완전하면 population을 허위로 내리거나 food4를 써서 두 번째 transporter를 만들지 않고 fail closed;
- normal transport tick/movement는 기존 `routeFullyLoaded` 및 waypoint `hasChunkAt` 경계를 유지하므로 **Transport workers belong to a specific outpost** / **pause at unloaded route boundaries** 계약과 Alpha.42 pacing을 바꾸지 않음;
- Alpha.63 exact MAINHAND death recovery, Alpha.62 군사 external-weapon reverse supply, military food→metal→weapon 우선순위는 변경 없음;
- 군사 sentry는 기존 search radius32 / loaded margin32가 일치해 같은 mismatch가 없음을 별도 회귀검사;
- 새 SavedData/UUID ledger/reservation/worker/route authority/virtual cargo/UI/key/force-load/teleport 없음;
- `single authority for outpost transport` / `there is still only one authority for long-distance outpost transport` 유지.

따라서 정적으로 재현 가능한 transporter unload false-missing→duplicate replacement 경계는 닫혔다. **실제 route unload/reload/save-reload/reconnect 반복 acceptance는 계속 남는다.**'''
gap = insert_once(gap,
    '### Alpha.66 민간 주민 lifecycle / 언로드 증거 감사',
    alpha67_gap,
    '### Alpha.67 전초 운송 주민 assignment evidence 감사',
    'gap Alpha.67 section')
gap = replace_once(gap,
    '2. Alpha.62–66 remote weapon/transporter/local-civilian physical cargo recovery + replacement의 route-unload/save-reload/reconnect/repeated-death no-loss/no-dup 실플레이 acceptance;',
    '2. Alpha.62–67 remote weapon/transporter/local-civilian physical cargo recovery + replacement의 route-unload/save-reload/reconnect/repeated-death no-loss/no-dup 실플레이 acceptance; Alpha.67 transporter lookup-envelope evidence도 실제 반복 unload에서 검증 필요;',
    'gap priority range')
write('COMPLETION_GAP_AUDIT.md', gap)

readme = read('README.md')
readme = replace_once(readme, '## Current version: 0.1.0-alpha.66', '## Current version: 0.1.0-alpha.67', 'README version')
readme = replace_once(readme, 'No new Alpha.65 key was added.', 'No new Alpha.67 key was added.', 'README controls')
readme = replace_once(readme, 'Alpha.40–66 deepen existing systems rather than inventing meaningless 16th–20th buildings.', 'Alpha.40–67 deepen existing systems rather than inventing meaningless 16th–20th buildings.', 'README family range')
alpha67_readme = r'''## Alpha.67 — fail-closed outpost transporter assignment evidence

Alpha.67 closes a deterministic route-unload duplicate-replacement edge while keeping the existing physical road logistics design intact.

- transporter entity lookup already covered the persisted road/stockpile bounds plus a 32-block margin, but the old replacement gate proved only the road-center chunks loaded;
- a legitimate tagged transporter could therefore sit in an unloaded adjacent lookup chunk while all road-center chunks remained loaded, making the partial entity query return `missing` and allowing food-funded replacement;
- Alpha.67 makes population reconciliation, missing-assignment inference, legacy reassignment and replacement spawn fail closed until every chunk intersecting the exact transporter lookup `routeBounds` is loaded;
- the proof uses `hasChunkAt` only and never force-loads or generates chunks;
- normal physical travel is intentionally unchanged: **Transport workers belong to a specific outpost**, follow the persisted road, carry actual MAINHAND ItemStacks and **pause at unloaded route boundaries**;
- Alpha.42 deferred pacing and Alpha.63 exact transporter-cargo death recovery remain unchanged;
- **군사 전초도 같은 도로 운송자가 역방향 보급**, **위험지역 군사 역할이 우선**, the **single authority for outpost transport**, and **there is still only one authority for long-distance outpost transport** all remain intact;
- no second transporter, UUID/save ledger, virtual cargo, UI/key, force-load, teleport or hard companion dependency was added.

This is deterministic pre-acceptance hardening. Real two-player route unload/reload, repeated death/replacement, save/reload and reconnect acceptance is still not claimed.'''
readme = insert_once(readme,
    '## Alpha.66 — loaded-evidence-safe civilian lifecycle authority',
    alpha67_readme,
    '## Alpha.67 — fail-closed outpost transporter assignment evidence',
    'README Alpha.67 section')
write('README.md', readme)

# Fail if current-version authority accidentally remained stale.
if 'Current canonical implementation: **0.1.0-alpha.66**' in canonical:
    raise SystemExit('stale canonical current version')
if '현재 구현 기준: `0.1.0-alpha.66`' in gap:
    raise SystemExit('stale gap current version')
if '## Current version: 0.1.0-alpha.66' in readme:
    raise SystemExit('stale README current version')
print('Alpha.67 canonical/gap/README patch prepared')
