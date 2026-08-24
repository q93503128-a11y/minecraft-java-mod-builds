# Frontier Settlement — v0.2 완성도 갭 감사

기준 문서: `ORIGINAL_DESIGN_v0.2.md`
현재 구현 기준: `0.1.0-alpha.53`

상태:
- `완료`: 원본 핵심 요구가 실제 구현됨
- `부분`: 기능은 있으나 원본 범위/실플레이 검증이 더 남음
- `미구현`: 원본 요구가 아직 게임 기능으로 없음
- `외부`: companion이 콘텐츠 폭을 담당
- `후보검증`: 버전/구성은 고정했으나 풀스택 런타임 검증 필요

이 문서는 현재 구현에 맞춰 원본 v0.2 범위를 축소하지 않는다. Alpha.53에서 bounded 직선 터널 1차까지 추가되어도 더 깊은 기념비급 토목, 실물 군사 armory, 일부 탐험/전초 breadth, 장시간 multiplayer 및 full companion runtime이 남아 있는 동안 완성이라고 부르지 않는다.

## 1. 핵심 정체성 / 멀티 / 조작

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 한 월드 하나의 공동 마을 | 완료 | SavedData 기반 공유 정착지 |
| 서버 authoritative | 완료 | 자원/건설/도로/전초/진행/토목 서버 권위 |
| 실제 ItemStack이 자원 권위 | 완료 | HUD/context/explorationScore/earthBank는 자원 권위 아님 |
| 핵심 직접 조작 소수 유지 | 완료 | B / R / Enter / Backspace |
| 플레이어별 개별 마을 금지 | 완료 | 단일 공동 정착지 |
| 세금/행복도/가족/거대 연구 UI 금지 | 완료 | 미시관리 없음 |
| 탐험/전투 → 정착 성장 | 완료/부분 | Alpha.45 unique 구조/정복 milestone + tier accelerator |
| companion 콘텐츠 활용 | 완료/부분/후보검증 | 후보 lock + soft bridge, full runtime 미검증 |

## 2. 자원 / 물류 / 경제

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 공동 실제 창고 | 완료 | 물리 ItemStack 권위 |
| 창고/HUD 장부 정합 | 완료/부분 | 자동 갱신, 장시간 2인 acceptance 남음 |
| 외부 재료 태그 수용 | 완료/부분 | additive Frontier + `c:` 태그, full pack 미검증 |
| 병영 충원 실제 자원 | 완료 | food8 + metal2 |
| 군사 전초 충원 실제 자원 | 완료/부분 | local food6 + metal2 + reverse supply |
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
| 더 깊은/곡선 기념비급 토목 | **미구현/부분** | Alpha.53 범위 밖 |
| 물리 단계 건설 | 완료 | grading→haul→foundation/frame/walls/roof/finish |
| 플레이어 건축/컨테이너 보호 | 완료/부분 | civil도 block entity/fluid/ore/non-natural/infrastructure 거부 |
| 건설소 자동 물류 지원 | 완료/부분 | physical staging runner |

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

따라서 **bounded straight tunnel은 완료/부분**으로 전진했다. curved/very-long/underground-station/monumental crossing은 여전히 미구현/부분이다.

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
| 실물 외부무기 군사 armory/loadout | **미구현/부분** | visual sword는 client-only, 실제 Weapons Expanded 보급 루프 없음 |
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
- actual external-weapon physical armory는 아직 완료가 아님.

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

## 7. 도로 / 전초 / 영토

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 물리 도로 시공 | 완료 | grading + hauling |
| 1블록 단차 계단 | 완료 | cobblestone stairs |
| 짧은 물길 다리 | 완료/부분 | max6 centerline bridge |
| 대형 협곡/장교량 | 완료/부분 | Alpha.52 max24 + physical persisted piers |
| 터널/더 깊은 대형 횡단 | 미구현/부분 | larger civil engineering next priority |
| 전초기지 물리 시공 | 완료 | persisted |
| 전초 특화 | 완료/부분 | lumber/quarry/mining/agriculture + dynamic fishing/military |
| 수변 특화 | 완료/부분 | fishing + real-wood landing + dedicated trade |
| 위험지역 군사 특화 | 완료/부분 | one supplied humanoid sentry |
| 전초 물류 | 완료 | Alpha.27 one authority |
| 군사 역보급 | 완료/부분 | **군사 전초도 같은 도로 운송자가 역방향 보급** |
| 수변 역보급 | 완료/부분 | same transporter wood after military priority |
| biome-aware companion specialization | 부분/미구현 | stable data seam 필요 |

**tier-visible public works**는 안전하고 loaded/non-farmable일 때만 허용한다. **위험지역 군사 역할이 우선**이다.

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

1. **deeper monumental crossing civil-engineering pass** — Alpha.52 long bridge보다 큰/복잡한 crossing breadth를 실물 자원·player protection 안에서 구현;
2. deeper exploration bridges — rare NPC/structure/boss별 정착 가치;
3. stable seam이 있을 때 companion-biome-aware outpost specialization;
4. per-soldier micromanagement 없이 가능한 physical military armory/loadout;
5. long survival + two-player multiplayer acceptance;
6. Alpha.42 catch-up pacing/save-reload/exploit acceptance;
7. Alpha.43 Jade/Xaero/HUD acceptance;
8. Alpha.46 waterfront pathing/trade acceptance;
9. Alpha.48 humanoid render/attack/migration acceptance;
10. Alpha.51 civil-work pathing/save-reload/retaining-cobble depletion/resupply/cargo-return/terrain-safety acceptance;
11. Alpha.53 tunnel detection/excavation/save-reload/pathing/no-drop/protection acceptance;
12. full companion lock fresh-world client/server runtime;
12. true Xaero marker는 stable supported API가 생길 때만;
13. moving boat/waterborne merchant는 두 번째 logistics authority가 되지 않는 경우에만 선택적 presentation.

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
