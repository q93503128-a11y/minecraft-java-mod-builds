#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'


def text(path):
    return path.read_text(encoding='utf-8')


def write(path, content):
    path.write_text(content, encoding='utf-8')


def repl(path, old, new):
    source = text(path)
    if old not in source:
        raise SystemExit(f'missing replacement anchor in {path}: {old[:120]!r}')
    write(path, source.replace(old, new, 1))

# Central service-level authority: every preview/start path must reject while any shared project is active.
write(JAVA / 'settlement/SettlementProjectAuthority.java', '''package kr.moonseungjun.frontiersettlement.settlement;\n\nimport net.minecraft.server.MinecraftServer;\n\n/**\n * One server-side gate for the shared construction authority. UI/network/commands may pre-check,\n * but they are never trusted as the final invariant: every project service reuses this gate before\n * preview/start mutation so stale previews or future direct callers cannot create parallel projects.\n */\npublic final class SettlementProjectAuthority {\n    private SettlementProjectAuthority() {}\n\n    public static boolean anyActive(MinecraftServer server, SettlementData data) {\n        return data.construction().active()\n                || data.roadConstruction().active()\n                || data.outpostConstruction().active()\n                || SettlementCivilWorkData.get(server).project().active();\n    }\n}\n''')

construction = JAVA / 'settlement/SettlementConstructionService.java'
repl(construction,
'''        if (!data.founded()) return invalidPlacement("공동 마을이 없습니다.");\n        if (player.level() != server.overworld()) return invalidPlacement("오버월드에서만 배치할 수 있습니다.");\n        String locked = lockedReason(data, type);''',
'''        if (!data.founded()) return invalidPlacement("공동 마을이 없습니다.");\n        if (player.level() != server.overworld()) return invalidPlacement("오버월드에서만 배치할 수 있습니다.");\n        if (SettlementProjectAuthority.anyActive(server, data)) {\n            return invalidPlacement("현재 공동 공사가 끝난 뒤 새 건물을 배치해 주세요.");\n        }\n        String locked = lockedReason(data, type);''')
repl(construction,
'''        if (data.construction().active()) {\n            BuildingType active = BuildingType.fromId(data.construction().type());\n            String name = active == null ? data.construction().type() : active.displayName();\n            return new StartResult(false, "이미 " + name + " 건설이 진행 중입니다.");\n        }\n        if (data.roadConstruction().active() || data.outpostConstruction().active()) {\n            return new StartResult(false, "현재 인프라 공사가 끝난 뒤 건물을 시작해 주세요.");\n        }''',
'''        if (SettlementProjectAuthority.anyActive(server, data)) {\n            return new StartResult(false, "현재 공동 공사가 끝난 뒤 건물을 시작해 주세요.");\n        }''')

road = JAVA / 'settlement/SettlementRoadService.java'
repl(road,
'''        if (data.construction().active() || data.roadConstruction().active() || data.outpostConstruction().active()) {\n            return invalid("현재 공사가 끝난 뒤 새 도로를 계획해 주세요.");\n        }''',
'''        if (SettlementProjectAuthority.anyActive(server, data)) {\n            return invalid("현재 공동 공사가 끝난 뒤 새 도로를 계획해 주세요.");\n        }''')

outpost = JAVA / 'settlement/SettlementOutpostService.java'
repl(outpost,
'''        if (data.construction().active() || data.roadConstruction().active() || data.outpostConstruction().active()) {\n            return PlacementCheck.invalid("현재 공사가 끝난 뒤 전초기지를 배치해 주세요.");\n        }''',
'''        if (SettlementProjectAuthority.anyActive(server, data)) {\n            return PlacementCheck.invalid("현재 공동 공사가 끝난 뒤 전초기지를 배치해 주세요.");\n        }''')
repl(outpost,
'''        if (data.construction().active() || data.roadConstruction().active() || data.outpostConstruction().active()) {\n            return new StartResult(false, "현재 공사가 끝난 뒤 전초기지를 시작해 주세요.");\n        }''',
'''        if (SettlementProjectAuthority.anyActive(server, data)) {\n            return new StartResult(false, "현재 공동 공사가 끝난 뒤 전초기지를 시작해 주세요.");\n        }''')

civil = JAVA / 'settlement/SettlementCivilWorkService.java'
repl(civil,
'''        String locked = lockedReason(settlement);\n        if (locked != null) return invalid(locked);\n        if (first == null || second == null) return invalid("두 모서리를 선택해 주세요.");''',
'''        String locked = lockedReason(settlement);\n        if (locked != null) return invalid(locked);\n        if (SettlementProjectAuthority.anyActive(server, settlement)) {\n            return invalid("현재 공동 공사가 끝난 뒤 선택영역 토목을 계획해 주세요.");\n        }\n        if (first == null || second == null) return invalid("두 모서리를 선택해 주세요.");''')
repl(civil,
'''        SettlementCivilWorkData data = SettlementCivilWorkData.get(server);\n        if (settlement.construction().active() || settlement.roadConstruction().active() || settlement.outpostConstruction().active()) {\n            return new StartResult(false, "현재 건물·도로·전초 공사가 끝난 뒤 토목을 시작해 주세요.");\n        }\n        if (data.project().active()) return new StartResult(false, "이미 선택영역 토목이 진행 중입니다.");\n        Check check = check(player, first, second);''',
'''        SettlementCivilWorkData data = SettlementCivilWorkData.get(server);\n        if (SettlementProjectAuthority.anyActive(server, settlement)) {\n            return new StartResult(false, "현재 공동 공사가 끝난 뒤 토목을 시작해 주세요.");\n        }\n        Check check = check(player, first, second);''')

# Version / candidate lock.
props = ROOT / 'gradle.properties'
repl(props, 'mod_version=0.1.0-alpha.58', 'mod_version=0.1.0-alpha.59')
repl(props,
     'plus multiplayer snapshot/session pre-acceptance hardening.',
     'plus multiplayer snapshot/session pre-acceptance hardening and one centralized service-level shared-project authority gate.')

lock = ROOT / 'COMPANION_LOCK.json'
repl(lock, '"frontier_settlement": "0.1.0-alpha.58"', '"frontier_settlement": "0.1.0-alpha.59"')
repl(lock,
'''    "Alpha.58 is multiplayer pre-acceptance hardening rather than new gameplay: serverbound placement handlers explicitly stay on NeoForge MAIN handling, a founded-world login republishes one refreshed authoritative settlement snapshot to all connected players, and client logout clears cached settlement/context/placement/notice state before another world/server can reuse it. Long two-player runtime acceptance is still not claimed.",''',
'''    "Alpha.58 is multiplayer pre-acceptance hardening rather than new gameplay: serverbound placement handlers explicitly stay on NeoForge MAIN handling, a founded-world login republishes one refreshed authoritative settlement snapshot to all connected players, and client logout clears cached settlement/context/placement/notice state before another world/server can reuse it. Long two-player runtime acceptance is still not claimed.",\n    "Alpha.59 centralizes building, road, outpost and civil-work exclusivity in one server-side SettlementProjectAuthority gate reused by every preview/start service path. UI, commands and stale client previews are not trusted as the final exclusivity authority; no new save field, worker, currency, key or companion dependency is added.",''')
repl(lock, 'so Alpha.58 keeps only HUD collision avoidance', 'so Alpha.59 keeps only HUD collision avoidance')

# README.
readme = ROOT / 'README.md'
repl(readme, '## Current version: 0.1.0-alpha.58', '## Current version: 0.1.0-alpha.59')
repl(readme, 'No new Alpha.58 key was added.', 'No new Alpha.59 key was added.')
repl(readme, 'Alpha.40–58 deepen existing systems rather than inventing meaningless 16th–20th buildings.',
             'Alpha.40–59 deepen existing systems rather than inventing meaningless 16th–20th buildings.')
alpha59_readme = '''## Alpha.59 — centralized single-project authority hardening\n\nAlpha.59 fixes a service-layer exclusivity gap found while auditing the required two-player acceptance path. It does **not** claim that long two-player runtime testing is complete.\n\n- new `SettlementProjectAuthority.anyActive(server, data)` is the single server-side gate for building, road, outpost and selected-area civil projects;\n- the gate reads all four shared project states, including `SettlementCivilWorkData`, so civil work cannot be accidentally omitted from another service's internal guard;\n- building `checkPlacement` and `startAt`, road `checkRoute`, outpost `checkPlacement` and `startAt`, and civil `check` and `start` all reuse the same authority;\n- therefore a stale client preview, `/frontier` command path, or future direct service caller cannot create a second project merely because an outer UI/network pre-check was bypassed;\n- NeoForge MAIN-thread serialization from Alpha.58 remains in place, so simultaneous confirms are processed sequentially and the later request sees the first request's newly active shared project;\n- the change adds no project save format, no project queue, no second builder, no resource reservation ledger, no key/UI and no new companion dependency;\n- existing physical ItemStack hauling and one shared construction worker remain unchanged.\n\nThis is another **pre-acceptance hardening** slice. The actual two-client long-survival, reconnect/save-reload and simultaneous-confirm play session is still required.\n\n'''
repl(readme, '## Alpha.58 — multiplayer snapshot/session pre-acceptance hardening\n', alpha59_readme + '## Alpha.58 — multiplayer snapshot/session pre-acceptance hardening\n')

# Canonical plan.
canonical = ROOT / 'CANONICAL_PLAN.md'
repl(canonical, 'Current canonical implementation: **0.1.0-alpha.58**.', 'Current canonical implementation: **0.1.0-alpha.59**.')
repl(canonical, 'The original target was roughly 15–20 meaningful families. Alpha.40–58 deepen systems rather than adding fake families.',
                'The original target was roughly 15–20 meaningful families. Alpha.40–59 deepen systems rather than adding fake families.')
repl(canonical,
'''- only one building/road/outpost/civil construction project may occupy the shared construction authority at once;''',
'''- only one building/road/outpost/civil construction project may occupy the shared construction authority at once;\n- Alpha.59 makes that invariant service-local rather than UI-local: every building/road/outpost/civil preview/start path calls the same `SettlementProjectAuthority.anyActive` gate before mutation;''')
alpha59_can = '''### Alpha.59 centralized single-project authority hardening\n\nAlpha.59 is a multiplayer correctness pass, not a new construction feature.\n\n- `SettlementProjectAuthority` reads building, road, outpost and civil active state from the two existing SavedData authorities;\n- it creates no fifth project state, queue, reservation ledger or new save field;\n- building preview/start, road preview/start, outpost preview/start and civil preview/start all reuse the same gate;\n- outer network/command checks remain convenience feedback only and are not trusted as the final invariant;\n- with Alpha.58 `HandlerThread.MAIN`, simultaneous confirmations are serialized and the later request rechecks this shared gate after the earlier request mutates state;\n- stale client previews therefore cannot authorize a second concurrent shared project;\n- one shared construction worker, physical ItemStack hauling, B/R/Enter/Backspace controls and all existing save formats remain unchanged;\n- actual long-survival/two-client/reconnect runtime acceptance remains unfinished.\n\n'''
repl(canonical, '## 4. Founding and growth\n', alpha59_can + '## 4. Founding and growth\n')
repl(canonical, '## 14. Current playable slice after Alpha.58', '## 14. Current playable slice after Alpha.59')
repl(canonical,
'''- Alpha.58 shared-login snapshot rebroadcast + explicit MAIN-thread request serialization + client session reset pre-hardening;''',
'''- Alpha.58 shared-login snapshot rebroadcast + explicit MAIN-thread request serialization + client session reset pre-hardening;\n- Alpha.59 centralized service-level single-project authority for building/road/outpost/civil preview and start;''')
repl(canonical, '## 15. Unfinished original-scope priorities after Alpha.58', '## 15. Unfinished original-scope priorities after Alpha.59')
repl(canonical,
'''1. long survival + two-player multiplayer acceptance; Alpha.58 only closes pre-acceptance deterministic state holes and does not satisfy this runtime item;''',
'''1. long survival + two-player multiplayer acceptance; Alpha.58–59 close deterministic state/exclusivity holes but do not satisfy this runtime item;''')
repl(canonical,
'''16. Alpha.58 two-client shared-login refresh, simultaneous confirmation, logout/server-switch reset and reconnect acceptance;\n17. full companion lock fresh-world client/server runtime;''',
'''16. Alpha.58 two-client shared-login refresh, logout/server-switch reset and reconnect acceptance;\n17. Alpha.59 simultaneous building/road/outpost/civil confirmation exclusivity acceptance;\n18. full companion lock fresh-world client/server runtime;''')
repl(canonical,
'''18. true Xaero markers only if a stable supported API appears;\n19. moving boat/waterborne merchant only if presentation value justifies it and it never becomes a second logistics authority.''',
'''19. true Xaero markers only if a stable supported API appears;\n20. moving boat/waterborne merchant only if presentation value justifies it and it never becomes a second logistics authority.''')
repl(canonical,
'''- no building/road/outpost project starts concurrently;''',
'''- no building/road/outpost/civil project starts concurrently, including simultaneous confirmations from two clients or direct command/service entry paths;''')

# Completion gap audit.
gap = ROOT / 'COMPLETION_GAP_AUDIT.md'
repl(gap, '현재 구현 기준: `0.1.0-alpha.58`', '현재 구현 기준: `0.1.0-alpha.59`')
repl(gap,
'''| 2인 snapshot/session 정합 pre-hardening | **완료/부분** | Alpha.58 MAIN-thread request + login rebroadcast + client logout reset; 장시간 실제 2인 acceptance는 남음 |''',
'''| 2인 snapshot/session 정합 pre-hardening | **완료/부분** | Alpha.58 MAIN-thread request + login rebroadcast + client logout reset; 장시간 실제 2인 acceptance는 남음 |\n| shared 공사 단일 authority | **완료/부분** | Alpha.59 building/road/outpost/civil preview+start가 하나의 service-level gate 재사용; 실제 2-client 동시 confirm acceptance는 남음 |''')
alpha59_gap = '''### Alpha.59 shared project authority 감사\n\n- 새 `SettlementProjectAuthority`는 기존 building/road/outpost/civil active state만 읽고 새 save state를 만들지 않음;\n- building `checkPlacement` + `startAt`가 central gate 재사용;\n- road `checkRoute`가 central gate 재사용하고 `startAt`은 checkRoute 재검증을 통과해야 함;\n- outpost `checkPlacement` + `startAt`가 central gate 재사용;\n- civil `check` + `start`가 central gate 재사용;\n- stale preview, command outer guard 누락, future direct service caller가 있어도 service mutation 직전 shared authority 재검사;\n- Alpha.58 MAIN-thread serialization과 결합해 동시 요청은 순차 처리되고 뒤 요청은 이미 active가 된 state를 봄;\n- 새 project queue/reservation ledger/worker/key/UI/currency/companion dependency 없음;\n- 실제 2-client 동시 confirm 및 save/reconnect 장시간 acceptance는 여전히 별도 실플레이 항목.\n\n'''
repl(gap, '## 2. 자원 / 물류 / 경제\n', alpha59_gap + '## 2. 자원 / 물류 / 경제\n')
repl(gap,
'''1. long survival + two-player multiplayer acceptance; Alpha.58은 pre-hardening만 완료했고 실제 runtime acceptance는 남음;''',
'''1. long survival + two-player multiplayer acceptance; Alpha.58–59는 snapshot/session + shared-project exclusivity pre-hardening만 완료했고 실제 runtime acceptance는 남음;''')
repl(gap,
'''15. Alpha.58 simultaneous player confirm/login-refresh/logout-reset/reconnect acceptance;\n16. full companion lock fresh-world client/server runtime;''',
'''15. Alpha.58 login-refresh/logout-reset/reconnect acceptance;\n16. Alpha.59 simultaneous building/road/outpost/civil confirm exclusivity acceptance;\n17. full companion lock fresh-world client/server runtime;''')
repl(gap,
'''17. true Xaero marker는 stable supported API가 생길 때만;\n18. moving boat/waterborne merchant는 두 번째 logistics authority가 되지 않는 경우에만 선택적 presentation.''',
'''18. true Xaero marker는 stable supported API가 생길 때만;\n19. moving boat/waterborne merchant는 두 번째 logistics authority가 되지 않는 경우에만 선택적 presentation.''')

# Cumulative audits.
write(ROOT / 'tools/test_alpha59_source.py', '''#!/usr/bin/env python3\nfrom pathlib import Path\nROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A58=ROOT/'tools/test_alpha58_source.py'\ndef text(p): return p.read_text(encoding='utf-8')\ndef must(s,tokens,label):\n    for t in tokens:\n        if t not in s: raise SystemExit(f'{label} missing: {t}')\ndef forbid(s,tokens,label):\n    for t in tokens:\n        if t in s: raise SystemExit(f'{label}: {t}')\na=text(A58).replace("print('Frontier Settlement alpha.23-58 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.58','0.1.0-alpha.59'); ns={'__file__':str(A58),'__name__':'__main__'}; exec(compile(a,str(A58),'exec'),ns,ns)\nauth=text(JAVA/'settlement/SettlementProjectAuthority.java'); construction=text(JAVA/'settlement/SettlementConstructionService.java'); road=text(JAVA/'settlement/SettlementRoadService.java'); outpost=text(JAVA/'settlement/SettlementOutpostService.java'); civil=text(JAVA/'settlement/SettlementCivilWorkService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json'); building=text(JAVA/'settlement/BuildingType.java')\nmust(auth,('public final class SettlementProjectAuthority','public static boolean anyActive(MinecraftServer server, SettlementData data)','data.construction().active()','data.roadConstruction().active()','data.outpostConstruction().active()','SettlementCivilWorkData.get(server).project().active()','UI/network/commands may pre-check'),'alpha.59 central project authority')\nforbid(auth,('SavedData','Codec','ItemStack','setBlock(','new Thread(','CompletableFuture','parallelStream('),'alpha.59 authority must be a read-only shared gate')\nif construction.count('SettlementProjectAuthority.anyActive(server, data)') < 2: raise SystemExit('alpha.59 construction preview/start must both use central authority')\nif road.count('SettlementProjectAuthority.anyActive(server, data)') < 1: raise SystemExit('alpha.59 road preview/start path must use central authority')\nif outpost.count('SettlementProjectAuthority.anyActive(server, data)') < 2: raise SystemExit('alpha.59 outpost preview/start must both use central authority')\nif civil.count('SettlementProjectAuthority.anyActive(server, settlement)') < 2: raise SystemExit('alpha.59 civil preview/start must both use central authority')\nmust(construction,('현재 공동 공사가 끝난 뒤 새 건물을 배치해 주세요.','현재 공동 공사가 끝난 뒤 건물을 시작해 주세요.'),'alpha.59 building feedback')\nmust(road,('현재 공동 공사가 끝난 뒤 새 도로를 계획해 주세요.'),'alpha.59 road feedback')\nmust(outpost,('현재 공동 공사가 끝난 뒤 전초기지를 배치해 주세요.','현재 공동 공사가 끝난 뒤 전초기지를 시작해 주세요.'),'alpha.59 outpost feedback')\nmust(civil,('현재 공동 공사가 끝난 뒤 선택영역 토목을 계획해 주세요.','현재 공동 공사가 끝난 뒤 토목을 시작해 주세요.'),'alpha.59 civil feedback')\nenum_block=building.split('public enum BuildingType {',1)[1].split(';',1)[0]; actual=[line.strip().split('(',1)[0] for line in enum_block.splitlines() if '(' in line]; expected=['HOUSE','LUMBER_CAMP','FARM','QUARRY','MINE','WAREHOUSE','CONSTRUCTION_OFFICE','BLACKSMITH','WORKSHOP','ADVANCED_WORKSHOP','GUARD_POST','WATCHTOWER','BARRACKS','MARKET','CART_STATION']\nif actual!=expected: raise SystemExit(f'alpha.59 expected exact 15 functional building families, got: {actual}')\nmust(props,('mod_version=0.1.0-alpha.59','centralized service-level shared-project authority gate'),'alpha.59 props')\nmust(lock,('"frontier_settlement": "0.1.0-alpha.59"','Alpha.59 centralizes building, road, outpost and civil-work exclusivity','no new save field, worker, currency, key or companion dependency','"status": "candidate_runtime_lock"'),'alpha.59 lock')\nprint('Frontier Settlement alpha.23-59 cumulative source audit: PASS')\n''')

write(ROOT / 'tools/test_alpha59_docs.py', '''#!/usr/bin/env python3\nfrom pathlib import Path\nROOT=Path(__file__).resolve().parents[1]\ndef text(n): return (ROOT/n).read_text(encoding='utf-8')\ndef must(s,tokens,label):\n    for t in tokens:\n        if t not in s: raise SystemExit(f'{label} missing: {t}')\nreadme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md')\nmust(readme,('## Current version: 0.1.0-alpha.59','## Alpha.59 — centralized single-project authority hardening','SettlementProjectAuthority.anyActive(server, data)','stale client preview','MAIN-thread request','actual two-client long-survival'),'alpha.59 README')\nmust(can,('Current canonical implementation: **0.1.0-alpha.59**','### Alpha.59 centralized single-project authority hardening','every building/road/outpost/civil preview/start path calls the same `SettlementProjectAuthority.anyActive` gate','## 14. Current playable slice after Alpha.59','## 15. Unfinished original-scope priorities after Alpha.59','actual long-survival/two-client/reconnect runtime acceptance remains unfinished'),'alpha.59 canonical')\nmust(gap,('현재 구현 기준: `0.1.0-alpha.59`','shared 공사 단일 authority | **완료/부분**','### Alpha.59 shared project authority 감사','실제 2-client 동시 confirm 및 save/reconnect 장시간 acceptance','Alpha.59 simultaneous building/road/outpost/civil confirm exclusivity acceptance'),'alpha.59 gap')\nprint('Frontier Settlement alpha.59 canonical docs audit: PASS')\n''')

print('Applied Frontier Settlement 0.1.0-alpha.59 centralized single-project authority hardening.')
