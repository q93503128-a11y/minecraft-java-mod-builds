# Frontier Settlement — v0.2 완성도 갭 감사

기준 문서: `ORIGINAL_DESIGN_v0.2.md`
현재 구현 기준: `0.1.0-alpha.79`

상태:
- `완료`: 원본 핵심 요구가 실제 구현됨
- `부분`: 기능은 있으나 원본 범위/실플레이 검증이 더 남음
- `미구현`: 원본 요구가 아직 게임 기능으로 없음
- `외부`: companion이 콘텐츠 폭을 담당
- `후보검증`: 버전/구성은 고정했으나 풀스택 런타임 검증 필요

이 문서는 현재 구현에 맞춰 원본 v0.2 범위를 축소하지 않는다. Alpha.57 본진 병영과 Alpha.62 원격 군사 실물 외부무기 armament가 구현됐어도, 해당 물류의 장시간 acceptance, rare-NPC breadth, 장시간 multiplayer 및 full companion runtime이 남아 있는 동안 완성이라고 부르지 않는다.

## 1. 핵심 정체성 / 멀티 / 조작

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 한 월드 하나의 공동 마을 | 완료 | SavedData 기반 공유 정착지 |
| 2인 snapshot/session 정합 pre-hardening | **완료/부분** | Alpha.58 MAIN-thread request + login rebroadcast + client logout reset; 장시간 실제 2인 acceptance는 남음 |
| shared 공사 단일 authority | **완료/부분** | Alpha.59 building/road/outpost/civil preview+start가 하나의 service-level gate 재사용; 실제 2-client 동시 confirm acceptance는 남음 |
| 서버 authoritative | 완료 | 자원/건설/도로/전초/진행/토목 서버 권위 |
| 실제 ItemStack이 자원 권위 | 완료 | HUD/context/explorationScore/earthBank는 자원 권위 아님 |
| 핵심 직접 조작 소수 유지 | 완료 | B / R / Enter / Backspace |
| 플레이어별 개별 마을 금지 | 완료 | 단일 공동 정착지 |
| 세금/행복도/가족/거대 연구 UI 금지 | 완료 | 미시관리 없음 |
| 탐험/전투 → 정착 성장 | 완료/부분 | Alpha.45 unique 구조/정복 milestone + tier accelerator |
| companion 콘텐츠 활용 | 완료/부분/후보검증 | 후보 lock + soft bridge, full runtime 미검증 |

### Alpha.58 멀티 snapshot/session pre-acceptance 감사

- world/server shared `SettlementData` 유지, per-player settlement/save 없음;
- serverbound play payload registration을 NeoForge `HandlerThread.MAIN`으로 명시;
- building/road/outpost/civil confirm은 MAIN thread에서 직렬화된 뒤 각 service가 current shared state를 다시 검사;
- founded-world player login은 common physical storage refresh 후 모든 connected player에게 같은 authoritative snapshot broadcast;
- joiner 때문에 storage ledger가 갱신돼도 기존 접속자 HUD만 stale로 남는 경로 제거;
- client `ClientPlayerNetworkEvent.LoggingOut`에서 snapshot/context initialized flags + placement modes/previews + notices reset;
- 다른 server/world 진입 때 이전 tier/context와 비교한 가짜 성장/완공 알림 방지;
- 새 payload schema/key/building/currency/per-player authority/async world mutation 없음;
- **실제 장시간 2인 acceptance는 아직 미완료**이며 Alpha.58을 그 완료로 기록하지 않음.

### Alpha.59 shared project authority 감사

- 새 `SettlementProjectAuthority`는 기존 building/road/outpost/civil active state만 읽고 새 save state를 만들지 않음;
- building `checkPlacement` + `startAt`가 central gate 재사용;
- road `checkRoute`가 central gate 재사용하고 `startAt`은 checkRoute 재검증을 통과해야 함;
- outpost `checkPlacement` + `startAt`가 central gate 재사용;
- civil `check` + `start`가 central gate 재사용;
- stale preview, command outer guard 누락, future direct service caller가 있어도 service mutation 직전 shared authority 재검사;
- Alpha.58 MAIN-thread serialization과 결합해 동시 요청은 순차 처리되고 뒤 요청은 이미 active가 된 state를 봄;
- 새 project queue/reservation ledger/worker/key/UI/currency/companion dependency 없음;
- 실제 2-client 동시 confirm 및 save/reconnect 장시간 acceptance는 여전히 별도 실플레이 항목.

## 2. 자원 / 물류 / 경제

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 공동 실제 창고 | 완료 | 물리 ItemStack 권위 |
| 창고/HUD 장부 정합 | 완료/부분 | 자동 갱신, 장시간 2인 acceptance 남음 |
| 외부 재료 태그 수용 | 완료/부분 | additive Frontier + `c:` 태그, full pack 미검증 |
| 병영 충원 실제 자원 | 완료 | food8 + metal2 |
| 군사 전초 충원 실제 자원 | 완료/부분 | local food6 + metal2 + reverse supply |
| 실물 외부무기 군사 armory/loadout | **완료/부분** | Alpha.57 본진 병영 + Alpha.62 원격 전초 exact MAINHAND 물리 보급; Alpha.63 stale-demand/운송자 화물 회수 하드닝, 장시간 acceptance 남음 |
| 건설/옹벽 실제 자원 | 완료 | real wood/stone haul/stage/consume |
| 수변 계류장 실제 자원 | 완료/부분 | real outpost wood + same transporter reverse supply |
| 수변 교역 실제 아이템 | 완료 | dedicated barrel 16 fish→1 emerald, ordinary stockpile 자동판매 없음 |
| Alpha.39 first forge 실제 자원 | 완료/부분 | relic1 + physically hauled metal4 |
| Alpha.47 domain reforge 실제 자원 | 완료/부분 | relic2 + physically hauled metal8 |
| Alpha.42 언로드 보정이 가상 자원화되지 않음 | 완료/부분 | work-time debt only |
| 토목 현장 earthBank가 가상 경제 자원이 되지 않음 | 완료/부분 | project-local relocation only, ItemStack/cargo/currency 변환 없음 |
| Alpha.50 외부 성토 자재 | **완료/부분** | actual DIRT/COARSE_DIRT storage→worker MAINHAND→world placement |
| Alpha.51 옹벽 자재 | **완료/부분** | exact COBBLESTONE storage→worker MAINHAND→retaining wall placement |

`single authority for outpost transport` 계약은 유지한다. **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**이며, **there is still only one authority for long-distance outpost transport**. 군사/수변 reverse supply도 이를 재사용한다.

## 3. 건설 / 지형 공사

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 3D preview/회전/배치 검사 | 완료 | ghost placement |
| 작은 높이 차 자동 grading | 완료 | 물리 worker grading |
| 중간 높이 차 `지형 공사 포함` | 완료/부분 | Alpha.44 span3–4 |
| real-stone retaining/foundation | 완료/부분 | exposed deep edge support |
| 큰/위험 지형 거부 | 완료 | span>4/fluid/block entity/unsafe support |
| 선택 영역 절토/성토 | **완료/부분** | Alpha.51 DOMAIN 17×17 / ±7 current pass |
| 외부 토사 반입/대형 성토 | **완료/부분** | Alpha.50 real dirt/coarse-dirt imported fill first expansion; 더 큰 성토는 남음 |
| 대형 옹벽/테라스 | **완료/부분** | Alpha.51 1-block outer ring, exposed edge 3–7 high, exact cobblestone physical retaining first pass |
| 대형 협곡/장교량 | **완료/부분** | Alpha.52 max24 straight crossing + persisted physical stone piers, real-play breadth 남음 |
| 직선 도로 터널 | **완료/부분** | Alpha.53 max24, width3/clear-height3, no-drop physical excavation first pass |
| 단일굴곡 터널/석재 포털 | **완료/부분** | Alpha.54 max24 유지, 90도 1회, 양 leg 최소3, 5×4 portal 2개/실물 stone22 |
| 더 거대한 기념비급 토목 | **선택/부분** | real-play에서 실제 필요성이 확인될 때만; WorldEdit식 확대는 범위 밖 |
| 물리 단계 건설 | 완료 | grading→haul→foundation/frame/walls/roof/finish |
| 일반 건물 world/item 거래 원자성 | **완료/부분** | Alpha.60 placement/grade rollback transaction; 실제 실패주입·save/reload acceptance 남음 |
| 플레이어 건축/컨테이너 보호 | 완료/부분 | civil도 block entity/fluid/ore/non-natural/infrastructure 거부 |
| 건설소 자동 물류 지원 | 완료/부분 | physical staging runner |

### Alpha.61 전초 grading transaction 감사

- 기존 9×9 전초 footprint/최대 성토2/save phase 유지;
- 한 grade cell에서 실제 변경 전 BlockState를 모두 snapshot;
- clear/support/final grade `setBlock` 성공을 전부 확인;
- 필요한 cell unload 또는 배치 실패 시 이미 성공한 변경을 역순 rollback;
- complete grade cell 성공 뒤에만 persisted outpost step advance;
- grading loose drop/virtual soil/refund/resource consume 없음;
- 새 save field/worker/key/building/force-load/teleport 없음.

실제 실패주입, 청크 경계 unload, save/reload acceptance는 아직 남는다.

### Alpha.60 일반 건설 transaction 감사

- blueprint step delta는 site crate의 real wood/stone에서만 지불;
- empty target은 material availability 확인 -> successful world `setBlock` -> crate consume -> step advance 순서;
- failed `setBlock`은 item/step 손실 0;
- placement 뒤 consume의 예상 밖 실패는 새 block을 이전 state로 rollback하고 step 유지;
- 이미 올바른 blueprint block이 있는 경우 정상 step 비용은 계속 내므로 player pre-fill 무료건설 exploit 없음;
- Alpha.44 grade cell은 clear/floor/support 변경 전 원본 BlockState를 snapshot;
- grade 중 하나라도 `setBlock` 실패하면 앞서 성공한 변경을 역순 rollback;
- retaining stone은 전체 grade cell 성공 뒤에만 consume;
- retaining consume이 예상 밖 실패하면 complete grade mutation rollback + step 유지;
- historical/current final validation repair는 이미 step 비용이 납부된 block 복구이므로 이중과금하지 않음;
- destroyBlock/dropResources/free refund/virtual resource/new worker/save authority 없음.

따라서 ordinary construction도 later road/outpost/civil과 동일한 **world 성공 이후 physical material/state commit** 원칙으로 정렬됐다. 실제 실패주입 및 장시간 save/reload 검증은 남는다.

### Alpha.44 감사 유지

- span0–2 기존 grading;
- span3–4 bounded medium work;
- cut/support depth 최대3;
- deep exposed edge에 real retaining stone;
- extra stone max96/project;
- builder가 storage→carry→site barrel→consume 순서로 실제 석재 사용;
- no free cobble, `destroyBlock`, loose-drop excavation, force-load, teleport inventory.

### Alpha.49 historical selected-area 감사

Alpha.49는 최초 selected-area civil pass의 역사적 기준이다.

- `DOMAIN` + 건설소 1곳 이상;
- 기존 B 팔레트/Enter/Backspace, 새 key/dashboard 없음;
- 첫 모서리 Y grade plane + 두 번째 모서리 X/Z;
- **9×9 / ±4**;
- player28 / settlement80;
- full loaded + infrastructure/block entity/fluid/ore/non-natural protection;
- real cut 성공 뒤 earthBank +1, no item drop;
- fill 전 earthBank 필요, fill 뒤 earthBank -1;
- **fill > cut 거부**;
- shared builder only;
- no force-load/global scan/teleport/destroyBlock/dropResources.

Alpha.50은 이 권위/보호/earthBank 계약을 유지하면서 크기·깊이와 imported fill만 의도적으로 확장한다.

### Alpha.50 physical imported-fill 감사

- unlock은 계속 `DOMAIN` + construction office;
- B/Enter/Backspace 재사용, 새 키/재화/BuildingType/dashboard 없음;
- 최대 **13×13**, column cut/fill 각각 최대 **5**;
- selected corners player36블록 이내, project center settlement96블록 이내;
- 전체 selected area loaded 필요;
- stockpile/functional building/road/outpost overlap 거부;
- block entity/fluid/ore/non-natural/player structure 거부 및 작업 중 target column 재검사;
- initial imported fill = `max(0, fillBlocks - cutBlocks)`;
- imported project 승인 시 shared settlement storage가 모두 loaded이고 실제 DIRT/COARSE_DIRT가 필요량 이상 있어야 함;
- 현장 real cut 성공 뒤에만 earthBank +1;
- 현장 earthBank를 먼저 실제 coarse dirt fill에 사용;
- earthBank가 부족한 fill만 기존 shared `건설 주민`이 actual storage까지 걸어가 최대16개 실제 DIRT/COARSE_DIRT를 MAINHAND로 추출;
- worker가 site까지 이동하고 `setBlock` 성공 뒤에만 carried ItemStack 1개 shrink + project step advance;
- `setBlock` 실패 시 item/step 손실 없음;
- project 중 storage dirt가 부족해지면 공짜 대체 없이 pause, 재공급 후 resume;
- current world height + earthBank에서 남은 imported volume을 재계산해 외부 pre-fill/변경 뒤 stale final haul 방지;
- 조기/정상 완료 시 MAINHAND 잔여 cargo가 있으면 persisted return phase로 전환하고 concrete loaded storage까지 실제 이동→`insertAt` 후에만 project clear;
- storage unload/full이면 real carried stack을 그대로 보존하고 pause;
- save/reload에서 phase/progress/earthBank/physical carried item이 가상 토사로 변환되지 않음;
- active volume break protection 유지;
- building/road/outpost UI/network/command start는 active civil과 동시 실행 불가;
- force-load/teleport/destroyBlock/dropResources/virtual soil 없음;
- compact context/status는 현장 토사 / 외부 흙 필요 / 실제 창고 흙 상태만 추가;
- **가상 토사 0**.

따라서 원본의 선택영역 절토/성토 + 첫 imported-fill 요구는 Alpha.50에서 더 전진했다.

### Alpha.51 retaining-heavy terrace 감사

- 기존 B/Enter/Backspace와 DOMAIN + construction office 조건 재사용;
- current envelope **17×17 / ±7**, player44 / settlement112;
- selected rectangle + one-block outer retaining protection ring loaded 필요;
- retaining ring도 Frontier infrastructure/block entity/fluid/non-natural/player obstruction 거부;
- fill-facing edge가 final grade보다 natural exterior ground 기준 3블록 이상 노출될 때만 retaining 계획;
- retaining height 최대7, 더 깊은 ravine edge는 거부;
- initial retaining block count를 SavedData state에 optional field로 보존하며 old Alpha.50 saves는 default0으로 decode;
- Alpha.50 `PHASE_FILL=1`, `PHASE_RETURN=2` 의미를 보존하고 `PHASE_RETAIN=3`만 추가;
- phase order cut→retaining→fill→return;
- approval 시 loaded shared storage의 exact COBBLESTONE 실제 수량 확인;
- 같은 건설 주민이 exact storage까지 이동→max16 COBBLESTONE MAINHAND 추출→wall cell 이동→successful setBlock→1 item shrink→step advance;
- placement 실패/중간 조약돌 고갈은 item/step 손실 없이 pause;
- generic stone ledger, free cobble conversion, virtual stone, second builder/economy 없음;
- force-load/teleport/destroyBlock/dropResources 없음;
- active break protection은 retaining ring까지 확장;
- compact status/context만 옹벽 잔여/창고 조약돌을 추가.

따라서 **retaining-heavy large terrace는 Alpha.51에서 완료/부분으로 전진**했다. ravine-scale work, long bridge, tunnel, monumental engineering은 여전히 미구현이며 unrestricted WorldEdit나 mountain deletion은 범위 밖이다.


### Alpha.52 long-bridge / ravine crossing 감사

- 기존 road endpoint/preview/approval 흐름과 같은 건설 주민 재사용, 새 key/building/currency 없음;
- Alpha.35 short bridge max6 유지, Alpha.52 straight bridge run max24;
- dry ravine은 shoulder 대비 최소4블록 깊이의 bounded depression만 자동 횡단;
- pier-required bridge는 village 단계부터;
- exact pier block positions를 optional `bridge_supports`에 저장, old saves default empty;
- 장교량 교각은 양쪽 edge column으로 계획되고 자연 지반을 최대12블록 안에서 찾아야 함;
- unloaded / block entity / non-water fluid / non-natural-player obstruction / too-deep support 거부;
- same shared road builder + actual settlement stone ItemStacks만 사용;
- world setBlock 성공 → carried stone consume → road state advance 순서, consume 실패 시 placed block rollback;
- Alpha.25+ physical road의 final validation missing block도 physical stone1개를 가져와 성공 배치 후 소비하며 free repair 없음. 단, 이미 선결제된 historical road save는 이중과금 방지를 위해 기존 prepaid semantics를 유지;
- completed road는 기존 RoadSegment/Alpha.27 transport authority로 귀결, second logistics authority 없음;
- force-load/teleport/virtual stone 없음.

따라서 **bounded long bridge/ravine crossing은 완료/부분**으로 전진했다. 터널과 더 복잡하고 깊은 기념비급 횡단은 여전히 미구현/부분이다.

### Alpha.53 bounded tunnel 감사

- 기존 road endpoint/preview/approval + shared builder authority 재사용;
- straight tunnel max24, entry/exit shoulder 차이<=1, minimum cover4;
- persisted `PROFILE_TUNNEL=2` + `TUNNEL_STEP_OFFSET=1_500_000`, old phase/save meanings 보존;
- width3 / head clear height3;
- loaded natural non-ore solid volume만 허용, block entity/fluid/ore/cave/player/non-natural obstruction 거부;
- previous open floor에서 builder가 물리 이동한 뒤 one-cell successful `setBlock(AIR)` -> no drops -> state advance;
- excavated block은 stone/earthBank/currency/cargo로 credit되지 않음;
- active tunnel cells break protection; unsafe external change pause;
- physical paving/stone hauling authority unchanged + tunnel center surcharge only;
- frontier-town + construction office unlock;
- completed road는 same RoadSegment / Alpha.27 logistics authority; force-load/teleport/second authority 없음.

따라서 **bounded straight tunnel은 완료/부분**으로 전진했다. Alpha.54는 single-bend/portal breadth를 추가하며 very-long/underground-station/WorldEdit-scale bores는 범위 밖으로 유지한다.

### Alpha.54 one-bend tunnel / physical portal 감사

- Alpha.53 max24 ceiling 그대로 유지, 단순 수치 확대 없음;
- 한 tunnel run에서 90도 turn 최대1회;
- bend 양쪽 tunnel leg 최소3 center;
- persisted centers + `PROFILE_TUNNEL=2`만으로 save/reload bend 재구성, 새 save authority 없음;
- 기존 width3 / clear-height3 / loaded-only / non-ore / non-fluid / no-cave / player-block protection 유지;
- tunnel run당 입구/출구 5폭 × 4높이 `STONE_BRICKS` portal frame 2개 결정론적 계획;
- portal frame 전체도 block entity/fluid/non-natural/infrastructure overlap 사전 거부;
- portal 자리 자연 블록은 기존 tunneling phase에서 one-cell `setBlock(AIR)` no-drop 굴착;
- portal stone은 run당22 실제 stone 비용, 같은 road builder가 settlement storage에서 물리 운반;
- portal world placement 성공 뒤 기존 paving ItemStack consume/state advance 권위 사용;
- active interior/floor/portal protection 유지;
- completed road는 same RoadSegment, `single authority for outpost transport` / `there is still only one authority for long-distance outpost transport` 유지;
- **Transport workers belong to a specific outpost** / **pause at unloaded route boundaries** 유지;
- 새 key/building/currency/dashboard/force-load/teleport/second authority 없음.

따라서 Alpha.52–54에서 장교량 + 직선 터널 + bounded 단일굴곡/실물 포털까지 첫 대형 횡단 breadth가 형성됐다. 더 큰 토목은 자동 다음 우선순위가 아니라 실플레이 필요성으로만 재개한다.

### Alpha.55 탐험 지식 / 전초 가치 감사

- Alpha.45 unique structure/boss persistence 그대로 사용, 새 save field 없음;
- 외부 구조물 unique type 최대3단계 survey knowledge, 동일 ID 반복 0;
- 강적 unique type 최대2단계 conquest knowledge, 동일 ID 반복 0;
- survey는 기존 loaded 12-block 전초 특화 증거에 작은 bounded bias만 추가, 자원 생성/광석 생성 없음;
- conquest는 신규 전초 physical total만 level당 wood4/stone2 절감, max wood8/stone4;
- base72/48, 최저64/44이며 placement 승인과 actual builder ItemStack consume가 같은 effective cost를 사용;
- physical outpost는 successful setBlock → carried material consume → state advance, consume 실패 rollback;
- Alpha.26+ missing priced blueprint final repair는 실제 wood/stone 1개를 fetch/place/consume하고 historical prepaid save는 이중과금하지 않음;
- free loot/refund/population/virtual currency 없음;
- loaded-only exploration observation/companion soft dependency 유지;
- `builder walks from actual settlement storage carrying real wood/stone stacks` 유지;
- `single authority for outpost transport` / `there is still only one authority for long-distance outpost transport` 유지;
- **Transport workers belong to a specific outpost** / **pause at unloaded route boundaries** 유지.

따라서 generic exploration-to-settlement value는 **완료/부분**으로 전진했다. 다음 탐험 breadth는 companion biome/NPC의 안정적인 soft seam이 실제로 있을 때만 추가한다.

## 4. 주민 / 생산 / 방어

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 건설가/벌목/농부/광부/채석공 | 완료 | physical loaded work |
| 어업 주민 | 완료/부분 | loaded shoreline fishing + pier construction |
| 수변 상인 | 완료/부분 | local trader + dedicated barrel |
| 작업장/고급 제작 전문 주민 | 완료/부분 | repair + first forge + domain reforge |
| 근거리 경비 | 완료/부분 | guard post |
| 감시/장거리 대응 | 완료/부분 | watchtower response |
| 병영 정식 주둔 병력 | 완료/부분 | 3 supplied slots + Alpha.48 humanoid body |
| 위험지역 전초 수비대 | 완료/부분 | one supplied sentry + same humanoid body |
| 사람형 군사 presentation | 완료/부분 | `FrontierSoldierEntity extends IronGolem` + humanoid renderer |
| 실물 외부무기 군사 armory/loadout | **완료/부분** | Alpha.57 본진 병영 + Alpha.62 원격 위험지역 전초 real external-weapon MAINHAND 물리 보급; 장시간/save-reload acceptance 남음 |
| 자동 직업 배치 | 완료/부분 | 주요 역할 자동화 |
| 언로드 저빈도 보정 | 완료/부분 | bounded time debt, no virtual item authority |

### Alpha.48 supplied humanoid military 감사 유지

- Frontier 전용 `frontier_soldier` entity type;
- server combat body/attributes는 Iron Golem 상속;
- **visual service sword is never a server ItemStack**;
- 병영3 slots/barracks, 신규 병사 food8 + metal2;
- 위험지역 전초 max1 sentry, food6 + metal2;
- tagged military drops clear;
- old tagged Iron Golem soldier/sentry는 loaded 상태에서1:1 migration;
- migration은 recruit consume 함수를 호출하지 않으므로 이중 과금 없음;
- Better Combat/Weapons Expanded Java hard dependency 없음;
- Alpha.48 시점에는 actual external-weapon physical armory가 미완료였고, Alpha.57에서 loaded 본진 병영, Alpha.62에서 기존 road transporter authority를 재사용한 원격 위험지역 전초 real MAINHAND 무장이 구현됨.

## 5. 성장 단계 / 탐험 되먹임

| 단계 | 상태 | 현재 |
| --- | --- | --- |
| 개척 캠프 | 완료 | starter storage/builder/basic production |
| 촌락 | 완료/부분 | quarry/mine/blacksmith/guard |
| 마을 | 완료/부분 | roads/market/outpost/construction logistics |
| 개척 도시 | 완료/부분 | barracks/advanced workshop/multiple outposts |
| 영지 | 완료/부분 | exploration accelerator + reforge + Alpha.51 retaining civil works, breadth/runtime 남음 |

Alpha.45는 already-loaded external structure type 및 direct-player conquest type을 unique milestone로 기록한다. score는 capped8 non-spendable metadata이며 legacy tier route를 폐기하지 않는다.

## 6. 건물 / 전문 기능

현재 functional family는 정확히 **15**다.

1. house
2. lumber camp
3. farm
4. quarry
5. mine
6. warehouse
7. construction office
8. blacksmith
9. workshop
10. advanced workshop
11. guard post
12. watchtower
13. barracks
14. market
15. cart station

Civil work는 infrastructure 보조 기능이며 16번째 가짜 BuildingType이 아니다.

### Alpha.47 domain reforge 감사 유지

- DOMAIN only;
- already-enchanted recognized external weapon + relic2 + metal8;
- same protected commission barrel/specialist;
- 기존 enchantment는 제거/하향되지 않음;
- no-compatible/no-improvement면 무기/금속/유물 소비 없음;
- Alpha.39 first forge relic1+metal4/power30 유지;
- no hard Weapons Expanded class/item reference.

### Alpha.57 본진 병영 실물 무장 감사

- 새 BuildingType/armory UI/장비 재화/개별 병사 메뉴 없음;
- loaded town barracks soldier만 1차 대상, remote military sentry는 의도적으로 제외;
- shared storage 전체 loaded + 실제 recognized external weapon 존재가 전제;
- idle/unarmed soldier가 nearest concrete weapon storage까지 최대160블록 물리 이동;
- 3블록 interaction range 도달 뒤 exact external weapon 1개만 실제 extraction;
- vanilla MAINHAND ItemStack으로 장착되어 damage/enchantment/components + entity save/sync 유지;
- active barracks threat가 armory trip보다 우선;
- renderer는 physical MAINHAND가 있으면 실제 무기를 표시하고 없을 때만 Alpha.48 client service sword fallback;
- death에서 일반 soldier/body drops는 계속 제거하되 실제 장착 external weapon 1개만 recovery drop으로 복원;
- weapon은 사전에 shared storage에서 제거된 동일 stack이므로 free mint/duplication 아님;
- no hard Weapons Expanded/Better Combat class dependency, force-load, teleport, second worker/economy/logistics authority 없음;
- remote weapon supply는 **군사 전초도 같은 도로 운송자가 역방향 보급**할 수 있을 때만 후속 허용;
- **위험지역 군사 역할이 우선**, `single authority for outpost transport`, `there is still only one authority for long-distance outpost transport` 유지;
- **Transport workers belong to a specific outpost** / **pause at unloaded route boundaries** 유지.

따라서 Alpha.57 시점 physical military armory/loadout은 **본진 병영 기준 완료/부분**으로 전진했고, 그 당시 **원격 위험지역 전초 실물 무기 역보급은 남음** 상태였다.

### Alpha.62 원격 군사 실물 무기 역보급 감사

- active dangerous general outpost + existing unarmed sentry에서만 weapon demand1;
- sentry MAINHAND 또는 outpost stockpile에 recognized external weapon이 이미 있으면 demand0으로 과잉 보급 방지;
- 같은 military reverse-supply 선택에서 food shortage -> metal shortage -> external weapon1 순서;
- 본진의 concrete loaded shared storage에서 exact weapon1 실제 extraction;
- 기존 outpost-assigned transporter, 기존 `MILITARY_RETURN_TRIP_TAG` / `MILITARY_SUPPLY_TRIP_TAG`, 기존 persisted road를 그대로 사용;
- transporter MAINHAND의 exact ItemStack이 도로를 따라 outpost stockpile로 실제 이동·삽입;
- sentry는 town storage를 직접 읽지 않고 전투가 끝난 뒤 local stockpile까지 걸어가 exact1을 MAINHAND로 추출;
- sentry death는 body/service drops를 clear한 뒤 실제 장착 external weapon exact copy1만 recovery drop;
- **위험지역 군사 역할이 우선**, food/metal survival reserve가 weapon보다 우선;
- **군사 전초도 같은 도로 운송자가 역방향 보급**;
- `single authority for outpost transport` / `there is still only one authority for long-distance outpost transport` 유지;
- **Transport workers belong to a specific outpost** / **pause at unloaded route boundaries** 유지;
- 새 save field/trip tag/worker/building/key/UI/currency/force-load/teleport/hard weapon class dependency 없음.

따라서 Alpha.62에서 원격 수비대 무기 ItemStack 역보급도 구현 **완료/부분**으로 전진했다. 실제 route unload, save/reload, sentry death/recruit 반복 no-dup acceptance는 남는다.

### Alpha.73 탐험-정착 되먹임 게임성 패스

- unique 외부 구조물 조사3 / 강적 정복2 상한과 중복 방지는 유지한다.
- 조사/정복은 기존 전초 비용/특화 보너스에 더해 시장 유물 판매, 외부무기 수리, 고급 제작/재련 성능을 자동 강화한다.
- 시장은 전용 barrel에 플레이어가 넣은 실제 expedition relic만 소비하고 실제 emerald를 지급하며 최대 추가 보너스는 +7이다.
- 작업장은 여전히 실제 금속1을 소비하지만 수리량은 base64에서 최대128까지 상승한다.
- 고급 제작 물리 비용 relic1+metal4 / 재련 relic2+metal8은 바뀌지 않고 enchant selection power만 최대40/50까지 상승한다.
- 새 연구 UI/탐험 화폐/save field/building/key/물류 권위는 없다. 탐험 성과가 반복 생존 노동과 다음 원정 준비를 직접 줄이는 원본 게임 루프 강화다.
- 밸런스 체감은 사용자 실플레이에서 조정하되, 개발측 구현 갭으로는 기존 hidden exploration score 의존도가 줄었다.

### Alpha.72 전체 소스 authority / transaction 감사

- 현재 Frontier Java 105개를 한 번에 전수검사해 `addFreshEntity` / `discard` / entity lookup / `setBlock` / `getHeight` / ItemStack insert-extract-shrink / SavedData commit 경계를 교차검사했다.
- 공용 건설 주민은 고정 ±96 검색을 폐기하고 실제 settlement storage + 활성 건설/도로/전초/토목 경계를 포함한 UUID 정렬 lookup/evidence를 사용한다. 0명 추론은 전체 evidence가 loaded일 때만 가능하고, 기존 중복 주민은 삭제하지 않고 NoAI 격리한다.
- 모든 공사 종료는 잔여 MAINHAND/crate 실물 자재를 concrete storage에 물리 반환한 뒤 같은 builder가 마을 anchor로 실제 복귀해야 확정된다.
- 건설 crate와 일반/도로/전초 return path의 broad storage insert를 제거하고, 주민이 실제 도착한 exact container에만 `insertAt`한다.
- 도로 grading은 partial mutation snapshot/rollback transaction으로 변경되어 world mutation 실패 시 persisted step이 전진하지 않는다.
- 수변 부두는 `setBlock` 성공 뒤에만 carried wood를 1 감소하고 buildStep을 전진한다.
- 개척 시작은 fence -> torch -> real BARREL Container 전체 성공 뒤에만 `SettlementData.found`를 commit하며 중간 실패는 이전 BlockState로 rollback한다. `/frontier found`도 같은 실제 표식 경로를 사용한다.
- fishing/market/waterfront/civic guard replacement는 lookup AABB 전체 loaded evidence를 요구하고 UUID 첫 개체만 권위를 가진다.
- builder/fishing MAINHAND는 civilian exact death recovery에 포함한다. 경비초소/감시탑의 재생성 Iron Golem은 vanilla drop을 clear해 무한 철 파밍을 막는다.
- 원격 군사 수비대의 historical duplicate도 UUID 첫 수비대만 AI 활성, 나머지는 실물 무기 보존을 위해 삭제 없이 NoAI containment한다.
- 극단적 패킷 좌표는 long/bounded distance 검증으로 반복문/거리 계산 전에 거부하고, audited terrain height query는 unloaded chunk를 읽지 않는다.
- 신규 컨텐츠/UUID save ledger/virtual cargo/force-load/teleport/key/UI/building/second logistics authority 없음.
- 실제 장시간 2인 death -> cargo -> unload/reload -> save/reconnect, fresh companion world acceptance는 계속 남는다.

### Alpha.71 병영/건설소 route-evidence lifecycle 감사

- 병영 병사 lookup과 replacement evidence가 실제 shared-storage 무기 왕복 route envelope를 공유한다.
- route envelope 일부가 unloaded면 해당 슬롯을 dead/missing으로 확정하지 않고 food8 + metal2 재충원을 보류한다.
- 무기 source 선택은 병영 anchor 기준 160블록 이내로 고정되어 이동 중 검색 범위가 연쇄적으로 밀려나지 않는다.
- 동일 슬롯 historical duplicate 병사는 삭제하지 않고 UUID 첫 병사만 AI 활성, 나머지는 NoAI 정지 상태로 보존한다.
- 건설소 보급 주민도 office material bay + old-save ordinary-storage route 전체를 absence evidence로 사용한다.
- 기존 `duplicate.discard()` 제거: 실물 MAINHAND wood/stone을 든 중복 주민을 파괴하지 않는다.
- 새 return trip은 SOURCE_RADIUS 24 안의 ordinary storage로 제한하고, 비정상 사망 시 supply runner MAINHAND도 exact physical drop 대상이다.
- 신규 컨텐츠/ledger/virtual cargo/force-load/teleport/key/UI/building/logistics authority 없음.
- 실제 장시간 2인 반복 death -> unload/reload -> save/reconnect acceptance는 계속 남는다.

### Alpha.70 전초 현지 생산자 lifecycle / 생산 world transaction 감사

- specialized outpost 생산 주민 lookup의 기존 ±48 AABB와 실제 absence evidence 범위를 동일하게 맞췄다.
- assignment가 보이지 않는다는 이유만으로 migration/replacement를 하지 않고, lookup AABB 전체 청크가 `hasChunkAt`으로 loaded일 때만 0명을 확정한다.
- 과거 중복 주민이 있으면 삭제하지 않고 UUID 정렬상 첫 주민만 실제 생산 권위를 가진다.
- 신규 현지 생산 주민은 `addFreshEntity` 성공 뒤에만 work path로 반환된다.
- 벌목/농업/채석/광산 현지 생산자가 실제 MAINHAND 자원을 들고 죽으면 기존 civilian exact-drop 권위에서 한 번만 물리 드롭한다.
- 본진과 전초의 나무/밀/석재/광석 생산은 해당 world `setBlock` 성공 뒤에만 수확 ItemStack을 만든다. 실패한 world mutation은 생산량 0이다.
- 현지 생산 인력을 shared housing/population에 새로 편입하지 않았고, 새 food 비용/환급/가상 worker ledger도 만들지 않았다.
- force-load/teleport/virtual cargo/second outpost logistics authority/new key/UI/building/companion hard dependency 없음.
- 실제 2인 반복 death -> cargo recovery -> unload/reload -> save/reconnect acceptance는 계속 남는다.

### Alpha.69 기존 중복 assignment containment 감사

- Alpha.67/68 이전 save에 동일 workshop / advanced workshop / outpost assignment 주민이 2명 이상 이미 존재할 수 있는 historical edge를 별도 containment했다.
- 완전 loaded evidence에서 동일 assignment의 실제 주민 전부를 UUID 중복 제거 후 population에 계상한다.
- matching resident가 정확히 0명일 때만 food4 replacement가 허용된다.
- 실제 작업/장거리 운송 권위는 UUID 정렬상 첫 주민 한 명만 사용한다.
- 기존 중복 주민 자체를 삭제/retag/refund하지 않으므로 숨은 실물 cargo를 파괴하거나 가상 환급하지 않는다.
- 실제 2인 반복 death/replacement/night-rest/save-reload/reconnect acceptance와 오래된 save의 물리적 중복 cleanup은 계속 미완료다.

### Alpha.68 야간 휴식 anchor / 민간 assignment evidence 감사

- 실제 `SettlementResidentRoutineService`는 벌목/농사/채석/광산/일반 작업장 주민을 야간에 completed HOUSE slot으로 이동시키며, 본진 반경 안의 전초 운송 주민도 HOUSE에서 휴식할 수 있음;
- Alpha.66의 work↔storage evidence에는 HOUSE가 없고 workshop/advanced lookup은 별도 192블록 AABB였으므로, 정상 휴식 주민의 house chunk만 unloaded인 상태에서 false missing→food4 중복 replacement가 가능했음;
- Alpha.68 `workerRouteBounds`는 work center + 모든 concrete shared storage + 모든 completed HOUSE footprint를 기존 margin과 함께 하나의 lifecycle envelope로 정의;
- `workerRouteEvidenceLoaded`와 workshop/advanced assigned-worker query가 같은 envelope를 사용하므로 lookup 범위와 loaded absence proof가 다시 분리되지 않음;
- 일반 생산 주민은 개별 수동 assignment를 추가하지 않고 기존 type/name pool을 유지하며, 각 해당 building lifecycle envelope의 union에서 UUID dedupe 검색; replacement/population reconciliation은 같은 envelope들이 전부 loaded일 때만 수행;
- 일반 workshop `spawnAssignedWorker`도 mutation 직전 same lifecycle evidence + service crate loaded를 재검사해 public/direct caller가 partial evidence를 우회하지 못함;
- transporter `routeBounds`에 completed HOUSE footprint를 포함하고 기존 32-block margin을 적용해 town-rest 중인 운송 주민도 Alpha.67 assignment evidence/lookup 안에 유지;
- 실제 work/haul/road 이동, route waypoint `hasChunkAt`, Alpha.42 debt, Alpha.63 cargo recovery, food4 atomic arrival 순서는 변경 없음;
- 새 SavedData/UUID reservation ledger/수동 주민 관리/가상 resident/cargo/force-load/teleport 없음;
- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, `single authority for outpost transport`, **there is still only one authority for long-distance outpost transport** 유지.

따라서 정적으로 연결되는 `정상 야간 집 이동 -> house unload -> false missing -> duplicate resident/transporter` 경계를 닫았다. **실제 2인 야간 반복 death/replacement/save-reload/reconnect acceptance는 계속 남는다.**

### Alpha.67 전초 운송 주민 assignment evidence 감사

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

따라서 정적으로 재현 가능한 transporter unload false-missing→duplicate replacement 경계는 닫혔다. **실제 route unload/reload/save-reload/reconnect 반복 acceptance는 계속 남는다.**

### Alpha.66 민간 주민 lifecycle / 언로드 증거 감사

- 생산 주민 population 재계산과 신규 대체는 작업지↔실제 공동 저장소 사이 bounded chunk envelope가 전부 loaded일 때만 수행;
- `hasChunkAt` 확인만 사용하며 chunk generation/force-load 없음;
- evidence가 불완전하면 unloaded 주민을 사망자로 간주하지 않고 population 감소/일반 생산 주민 대체를 보류;
- 일반 작업장과 고급 제작소 assignment도 work↔storage loaded evidence가 완전할 때만 missing 판정;
- 고급 제작 주민의 과거 무료 직생성 경로 제거, 앞으로 missing replacement는 기존 housing + real food4 + `addFreshEntity 성공 -> food consume -> population +1` 경로 사용;
- pre-Alpha.66에 이미 존재하는 고급 제작 주민은 삭제/재생성/소급 food 청구 없이 그대로 유지하고 complete evidence 시 population에 포함;
- 고급 제작 주민의 실제 MAINHAND metal도 Alpha.65 local civilian exact recovery 대상에 포함, empty hand는 drop0;
- 새 SavedData/reservation ledger/가상 주민/가상 화물/수동 주민 UI/force-load/teleport 없음.

따라서 정적으로 재현 가능한 `unload -> population 허위 감소 -> 중복 대체`와 고급 제작 주민의 zero-food replacement/cargo-loss 경계를 닫았다. 실제 2인 반복 사망/재접속/save-reload acceptance는 계속 남는다.

### Alpha.65 로컬 주민 실물 화물 사망 경계 감사

- 신규 벌목/농사/채석/광산 주민은 persistent resource-worker entity tag 보유;
- Alpha.65 이전 저장의 일반 생산 주민은 기존 exact custom name으로 계속 식별해 save migration 없이 보호;
- 작업장 주민은 기존 workshop worker tag 재사용;
- 관리 주민 사망 시 vanilla equipment-drop 확률을 신뢰하지 않고 현재 MAINHAND exact ItemStack copy1을 world recovery drop으로 복원;
- empty MAINHAND이면 drop0, 주민 유입 food4는 환불하지 않으며 이미 창고에 넣은 자원도 재생성하지 않음;
- 벌목 log / 농사 wheat / 채석 stone / 광산 ore / 공동창고에서 실제 추출된 workshop metal의 in-flight 물리 copy만 보호;
- outpost transporter는 이 handler에서 명시적으로 제외되어 Alpha.63 transporter handler만 사용하므로 double recovery 없음;
- 활성 건설/도로 공사 builder는 기존 project invulnerable lifecycle 유지;
- 새 SavedData/recovery ledger/virtual cargo/refund currency/worker family/UI/force-load/teleport/logistics authority 없음.

따라서 로컬 생산/작업장 주민이 들고 있던 실물 ItemStack의 정적 silent-loss 경계는 닫혔다. 실제 반복 사망/대체, save/reload, reconnect, 2인 no-loss/no-dup acceptance는 계속 남는다.

### Alpha.64 주민 유입/운송자 대체 원자성 감사

- 기존 주민 유입 비용 food4와 housing/population 규칙은 유지;
- 일반 생산 주민, 작업장 주민, 전초 전담 운송 주민 모두 실제 entity add 성공을 반환;
- loaded shared storage의 실제 food4 존재 확인 -> entity add 성공 -> 실제 food4 consume -> population +1 순서;
- entity add 실패면 food/population 변경 0;
- add 성공 뒤 예상 밖 consume 실패면 방금 생성한 주민을 discard하고 population 변경 0;
- workshop/outpost assigned spawn은 mutation 직전 현재 assignment를 다시 검사해 stale missing 관측으로 의도적인 중복 assignment를 만들지 않음;
- 운송자 사망 화물은 Alpha.63 exact MAINHAND recovery가 담당하며 replacement가 cargo를 복사/재생성하지 않음;
- save field/reservation ledger/virtual food/refund currency/new worker type 없음;
- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, `single authority for outpost transport`, **there is still only one authority for long-distance outpost transport** 유지.

따라서 코드상 재현 가능한 failed-spawn 식량 손실·허위 population 증가 경계는 닫혔다. 실제 반복 사망/대체, route unload/reload, save/reconnect no-dup acceptance는 계속 남는다.

### Alpha.63 운송 트랜잭션 하드닝 감사

- Alpha.62 weapon demand는 출발 시뿐 아니라 실제 전초 창고 삽입 직전 다시 검사;
- 이동 중 sentry가 무장되거나 outpost stockpile에 다른 recognized weapon이 생겨 demand0이면 carried weapon을 두 번째 재고로 삽입하지 않음;
- exact carried weapon은 transporter MAINHAND에 그대로 남고 `MILITARY_SUPPLY_TRIP_TAG`만 해제되어 기존 일반 반환 경로로 본진 concrete storage에 돌아감;
- stale cargo 삭제/teleport/virtual refund/새 return ledger 없음;
- tagged transporter 사망 시 vanilla equipment drop ambiguity를 clear하고 현재 MAINHAND ItemStack exact copy1만 world recovery drop으로 복원;
- empty MAINHAND 사망은 cargo0이라 아이템 생성 없음;
- normal outpost cargo와 military/waterfront reverse-supply cargo 모두 같은 death-recovery 경계를 사용;
- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, **군사 전초도 같은 도로 운송자가 역방향 보급** 유지;
- `single authority for outpost transport` / `there is still only one authority for long-distance outpost transport` 유지;
- 새 save field/trip family/worker/building/key/UI/currency/force-load/teleport/hard companion dependency 없음.

따라서 정적으로 재현 가능한 in-flight 과잉 weapon 보급과 운송자 사망 silent cargo loss 경계는 닫혔다. 실제 route unload/save-reload/reconnect/반복 사망 no-dup acceptance는 계속 남는다.

## 7. 도로 / 전초 / 영토

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 물리 도로 시공 | 완료 | grading + hauling |
| 1블록 단차 계단 | 완료 | cobblestone stairs |
| 짧은 물길 다리 | 완료/부분 | max6 centerline bridge |
| 대형 협곡/장교량 | 완료/부분 | Alpha.52 max24 + physical persisted piers |
| 터널/더 깊은 대형 횡단 | 완료/부분 | Alpha.53 straight + Alpha.54 one-bend/physical portals; 더 거대한 토목은 선택적 |
| 전초기지 물리 시공 | 완료 | persisted |
| 전초기지 grading 원자성 | **완료/부분** | Alpha.61 grade-cell snapshot/rollback; 실제 실패주입·save/reload acceptance 남음 |
| 전초 특화 | 완료/부분 | lumber/quarry/mining/agriculture + dynamic fishing/military |
| 수변 특화 | 완료/부분 | fishing + real-wood landing + dedicated trade |
| 위험지역 군사 특화 | 완료/부분 | one supplied humanoid sentry |
| 전초 물류 | 완료 | Alpha.27 one authority |
| 군사 역보급 | 완료/부분 | food/metal + Alpha.62 external weapon1을 **군사 전초도 같은 도로 운송자가 역방향 보급** |
| 수변 역보급 | 완료/부분 | same transporter wood after military priority |
| biome-aware companion specialization | **완료/부분** | Alpha.56 NeoForge common biome tags + local physical evidence, no hard worldgen dependency |

**tier-visible public works**는 안전하고 loaded/non-farmable일 때만 허용한다. **위험지역 군사 역할이 우선**이다.

### Alpha.56 common-biome-tag 전초 특화 감사

- already-loaded outpost center의 NeoForge common biome tags만 읽음;
- forest/dense vegetation +log8, plains/savanna +field24, mountain/hill +stone8/+ore1, badlands/sandy +stone6;
- 기존 threshold ore4/log24/field120/stone24보다 단독 bias가 작아 biome만으로 특화 확정 불가;
- 기존 12-block physical local survey + Alpha.55 bounded survey knowledge가 계속 주권;
- unloaded center는 biome bias0, chunk generation/force-load 없음;
- Terralith class/id string, reflection, hard dependency 없음;
- biome이 자원/광석/식량을 생성하지 않음;
- 새 specialization family/save field/currency/worker/logistics authority 없음;
- `single authority for outpost transport` / `there is still only one authority for long-distance outpost transport` 유지;
- **Transport workers belong to a specific outpost** / **pause at unloaded route boundaries** 유지.

따라서 generic companion-biome-aware specialization은 **완료/부분**으로 전진했다. rare-NPC 연결은 안정적 soft seam이 실제 확인될 때만 남긴다.

## 8. 외부 콘텐츠 / companion

`COMPANION_LOCK.json`은 계속 `candidate_runtime_lock`이다.

- Terralith + Lithostitched: 외부/후보검증
- Dungeons and Taverns: 외부/후보검증
- Repurposed Structures: 외부/후보검증
- Better Combat + libraries: 외부/후보검증
- Weapons Expanded: 외부/부분/후보검증
- Lootr: 외부/후보검증
- Sophisticated Backpacks/Core: 외부/후보검증
- Jade: 완료/부분/후보검증
- Xaero's Minimap: 후보검증, HUD offset만 구현

Alpha.50는 already-loaded ordinary/companion terrain block state와 loaded physical settlement storage만 읽는다. Terralith/worldgen class hard dependency, chunk generation, force-load는 없다.

Xaero26.4.2의 historical public `WaypointsManager` API는 없으므로 true settlement/outpost marker sync를 완료했다고 주장하지 않는다.

## 9. 현재 남은 우선순위

실플레이 회귀가 우선순위를 바꾸지 않는 한:

1. long survival + two-player multiplayer acceptance; Alpha.58–59는 snapshot/session + shared-project exclusivity pre-hardening만 완료했고 실제 runtime acceptance는 남음;
2. Alpha.62–68 remote weapon/transporter/local-civilian physical cargo recovery + replacement의 route-unload/night-rest/save-reload/reconnect/repeated-death no-loss/no-dup 실플레이 acceptance; Alpha.67 transporter lookup-envelope와 Alpha.68 HOUSE rest-anchor evidence도 실제 반복 unload에서 검증 필요;
3. rare-NPC-specific settlement value는 stable soft seam이 실제 확인될 때만; generic biome-aware specialization은 Alpha.56에서 1차 완료/부분;
4. optional deeper monumental crossing은 Alpha.52–54 실플레이에서 실제 부족이 확인될 때만;
6. Alpha.42 catch-up pacing/save-reload/exploit acceptance;
7. Alpha.43 Jade/Xaero/HUD acceptance;
8. Alpha.46 waterfront pathing/trade acceptance;
9. Alpha.48 humanoid render/attack/migration acceptance;
10. Alpha.51 civil-work pathing/save-reload/retaining-cobble depletion/resupply/cargo-return/terrain-safety acceptance;
11. Alpha.53 tunnel detection/excavation/save-reload/pathing/no-drop/protection acceptance;
12. Alpha.54 one-bend/corner clearance/portal excavation/physical stone22/save-reload acceptance;
13. Alpha.56 common-biome-tag borderline specialization + companion installed/absent acceptance;
14. Alpha.57 shared-storage weapon walk/extract/persistence/render/death-recovery/no-dup acceptance;
15. Alpha.58 login-refresh/logout-reset/reconnect acceptance;
16. Alpha.59 simultaneous building/road/outpost/civil confirm exclusivity acceptance;
17. Alpha.60 building placement failure/rollback + grade retaining consume rollback/pre-fill-cost acceptance;
18. Alpha.61 outpost grade-cell failure/rollback + unload/save-reload acceptance;
19. full companion lock fresh-world client/server runtime;
20. true Xaero marker는 stable supported API가 생길 때만;
21. moving boat/waterborne merchant는 두 번째 logistics authority가 되지 않는 경우에만 선택적 presentation.

## 10. Alpha.51/52 추가 실플레이 acceptance

최종/test-worthy 시점에 최소 확인:

- DOMAIN + construction office 조건이 client 표시뿐 아니라 server에서 강제됨;
- first-corner grade와 opposite-corner area가 실제 ghost/server approval과 일치;
- max17×17, cut/fill ±7 경계값 정확성;
- fluids/block entities/ores/non-natural/player structures/existing infrastructure 거부;
- real cut에서 drop 없음 + successful setBlock 이후에만 earthBank credit;
- local earthBank fill 우선;
- fill>cut project는 loaded common storage의 real dirt/coarse dirt가 충분하면 승인되고 부족하면 거부;
- builder가 exact storage container까지 걸어가 max16 real dirt/coarse를 MAINHAND에 들고 site로 이동;
- mid-project storage dirt depletion은 pause, later physical resupply 후 resume;
- failed fill placement는 carried dirt를 consume하지 않음;
- current site에서 imported remaining이 다시 계산되어 final haul over-pick 없음;
- early/pre-filled finish에서 잔여 carried dirt가 concrete storage로 물리 복귀한 뒤 project clear;
- return 중 storage unload/full이면 ItemStack 보존 + pause;
- save/reload 중 cut/fill/return phase, progress, earthBank, carried item 중복/손실 없음;
- unloaded selected area/storage는 force-load 없이 pause;
- shared builder가 실제 storage/site/return target까지 이동;
- building/road/outpost와 동시 project 시작 불가;
- project 완료 뒤 spendable/transferable virtual earth가 남지 않음;
- retaining ring 높이3 시작/높이7 허용/높이8 거부가 정확함;
- approval의 exact COBBLESTONE 수량과 실제 storage 수량이 일치하고 generic stone만으로는 통과하지 않음;
- builder가 cobblestone을 max16 physical batch로 운반하고 successful wall setBlock 뒤에만 shrink/advance;
- mid-project cobblestone depletion은 pause, later resupply resume;
- PHASE_RETAIN/save-reload에서 retaining count/item 중복·손실 없음;
- 두 플레이어가 같은 civil project/progress/context를 봄.
- Alpha.52 short6/long24 경계, dry-ravine 최소4 깊이, straight-only pier rule 정확성;
- pier support depth12 허용/13 거부, water 허용/non-water fluid·container·player block 거부;
- `bridge_supports` save/reload가 같은 exact support cells를 유지;
- deck/support 모두 real stone depletion에서 pause/resupply resume;
- road placement 성공 전에는 stone consume/state advance가 없고, failed/rollback 경로에서 free block이 남지 않음;
- final missing road/bridge repair도 actual stone을 소비해 free repair가 없음.

## 11. 완료 판정 금지선

다음이 남아 있는 동안 `원본 v0.2 완성`이라고 부르지 않는다.

- tunnel / deeper or more complex monumental crossing civil engineering breadth;
- meaningful companion/exploration breadth gaps;
- physical military armory 여부;
- long multiplayer acceptance;
- full candidate companion-stack fresh-world client/server acceptance.

자동 cumulative source audit / canonical docs audit / Java25 clean build / JAR verify는 필수지만 실제 Minecraft acceptance를 대체하지 않는다.

### Alpha.74 외부 위협 전투지식 게임성 패스

- 외부 적대 몹의 서로 다른 타입을 플레이어가 처음 직접 처치하면 최대 3단계의 `위협정보`로 누적한다. 기존 80HP 이상 강적/보스 정복 기록과는 분리해 의미를 유지한다.
- 위협정보는 재화가 아니며 아이템/드랍/가상자원을 생성하지 않는다.
- 병영의 실제 모집 식량비가 8 -> 7 -> 6 -> 최소 5로 줄고 금속 2는 그대로 소비한다.
- 병영의 로드된 적 탐지 반경은 28 -> 32 -> 36 -> 최대 40블록으로 늘어난다.
- V&V 같은 외부 적대몹을 잡는 행동이 단순 변종 감상이 아니라 정착지 군사 성장으로 되돌아오며, companion class hard dependency는 없다.
- 남은 검증: 사람 클라이언트에서 실제 스폰 밀도/전투 빈도/체감 밸런스는 문승준 실플레이 영역이다.


### Alpha.78 영지망 물리 물류 효율 패스

- `DOMAIN` + cart station + 서로 다른 생산 전문 전초 조합을 기존 Alpha.77 영지망 레벨로 재사용;
- 일반 생산물 전초 -> 본진 회수만 Lv.0/1/2/3에서 32/36/40/44, 최대 44;
- cart station이 없으면 기존 16 유지;
- 군사 food/metal/외부무기 및 수변 wood 역보급은 기존 16/32 상한과 우선순위를 그대로 유지;
- real outpost stockpile -> assigned transporter MAINHAND -> persisted road -> real town storage 권위 유지;
- route unload에서는 정지하며 force-load/teleport/virtual cargo/free resource/두 번째 운송자 시스템 없음;
- Alpha.42 backlog는 다음 실제 픽업만 최대 64까지 보정하며 화물 장부가 아님;
- 장시간 2인, route unload/reload, 운송자 사망 화물 회수, save/reload 실제 acceptance는 여전히 남음.


### Alpha.79 테스트 전 전체 수동검사 하드닝

- 신규 전초 청사진 위치에 같은 블록을 미리 놓아 단계 비용을 건너뛰던 경로를 차단: 동일 블록이어도 해당 단계의 실제 목재/석재 몫을 운반·소비한 뒤에만 진행;
- 과거 prepaid 전초 공사 save는 재과금하지 않음;
- wood/stone/metal/food가 겹치게 태그된 외부 ItemStack은 모든 일반 자원 판정에서 fail-closed;
- expedition relic/recognized external weapon은 잘못된 외부 태그가 붙어도 건설·모집·수리 재료로 소모하지 않음;
- 주민 replacement, MAINHAND cargo, shared-project race, client cache reset, save phase codec, 도로/토목 rollback, force-load/teleport 부재를 수동 재검토;
- 실제 장시간 2인, reconnect, save/reload, 운송자 사망, route unload/reload 등의 실플레이 acceptance는 여전히 별도 검증 필요.
