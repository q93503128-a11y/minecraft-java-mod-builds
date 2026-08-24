# Frontier Settlement — v0.2 완성도 갭 감사

기준 문서: `ORIGINAL_DESIGN_v0.2.md`
현재 구현 기준: `0.1.0-alpha.45`

상태:
- `완료`: 원본 핵심 요구가 실제 구현됨
- `부분`: 기능은 있으나 원본 범위가 더 남음
- `미구현`: 원본 요구가 아직 게임 기능으로 없음
- `외부`: Frontier 자체 재구현보다 companion이 콘텐츠를 공급
- `후보검증`: 버전/구성은 고정했으나 풀스택 실런타임 검증 필요

이 문서는 현재 코드에 맞춰 원본 범위를 줄이는 문서가 아니다. 기능 건물 수가 15개에 도달했고 수변/위험지역 전초, bounded unloaded-work 1차, compact Jade/status 1차, bounded medium terrain 1차, exploration/conquest progression 1차가 들어갔더라도 핵심 `부분/미구현`이 남아 있는 동안 제품을 완성이라고 부르지 않는다.

## 1. 핵심 정체성 / 멀티 / 조작

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 한 월드 하나의 공동 마을 | 완료 | SavedData 기반 공유 정착지 |
| 서버 authoritative | 완료 | 자원/건설/도로/전초/진행 서버 권위 |
| 핵심 직접 조작 소수 유지 | 완료 | B / R / Enter / Backspace 중심 |
| 플레이어별 개별 마을 금지 | 완료 | 단일 공동 정착지 |
| 세금/행복도/가족/거대 연구 UI 금지 | 완료 | 해당 미시관리 없음 |
| 탐험/전투가 정착 성장으로 되먹임 | **완료/부분** | Alpha.45 외부 구조물/정복 milestone이 shared tier accelerator로 연결. 희귀 NPC/보스별 특수 보상 breadth는 남음 |
| 외부 모드를 콘텐츠 생산 수단으로 사용 | 완료/부분/후보검증 | 후보 스택 + 시장/작업장/고급 제작 + Jade presentation + Alpha.45 structure registry bridge. 풀런타임 미검증 |

Alpha.45 탐험 진척은 공유 진행 metadata이며 재화가 아니다. 같은 구조물/강적 종류 반복으로 무한 파밍할 수 없고 기존 성장 루트를 새 점수로 막지 않는다.

## 2. 시작 / 자원 / 창고 / HUD

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 개척 표식 + 공동 창고 + 건설 주민 | 완료 | 구현 |
| 실제 아이템이 자원 권위 | 완료 | ItemStack/물리 컨테이너 |
| HUD 목재/석재/금속/식량/인구 | 완료 | compact HUD |
| 외부 재료 범주/태그 수용 | 완료/부분 | additive Frontier + `c:` 태그. 풀 companion 실플레이 미검증 |
| 대량 저장 | 완료/부분 | 창고 + 수레 정거장 + 건설소 자재 배럴. 장기 부하 검증 필요 |
| 임의 플레이어 상자 과잉 스캔 금지 | 완료 | 정해진 settlement storage만 권위에 포함 |
| 멀티 창고/HUD 장시간 정합 | 부분 | 실제 2인 장시간 검증 필요 |
| 병영 충원도 실제 자원 사용 | 완료 | 식량8 + 금속2 물리 소비 |
| 위험지역 전초 충원도 실제 자원 사용 | 완료/부분 | 현지 stockpile 식량6 + 금속2, 기존 도로 운송자가 역방향 보급. 실동선 검증 필요 |
| 건설소 자재도 실제 자원 사용 | 완료 | 동일 ledger의 목재/석재 staging |
| 중간 지형 옹벽도 실제 자원 사용 | **완료/부분** | Alpha.44 깊은 노출 가장자리 cobblestone support는 real stone haul/stage/consume 후 배치. 실지형 검증 필요 |
| 고급 제작 재료도 실제 자원 사용 | 완료/부분 | 의뢰 배럴 유물1 + 전문 주민이 운반한 금속4. 외부 무기별 실전 호환 검증 필요 |
| 수변 전초 생산도 실제 아이템 | 완료/부분 | Alpha.40 실제 대구/연어 → 전초 stockpile → 기존 도로 운송 |
| 언로드 보정이 자원 권위를 침범하지 않음 | 완료/부분 | Alpha.42는 work-time debt만 저장. 실제 물리 동작 성공 뒤에만 credit 소모. 실플레이 악용·재로드 검증 남음 |
| UI context가 새 자원 권위가 되지 않음 | 완료 | Alpha.43 표시 전용 context |
| 탐험 score가 새 자원 권위가 되지 않음 | 완료 | Alpha.45 score는 구조/정복 unique 기록에서 파생된 non-spendable progression metadata |

## 3. 건설 / 지형 공사

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 3D 완성 미리보기/회전/검사 | 완료 | ghost placement |
| 작은 높이 차 자동 정리 | 완료 | 물리 worker grading |
| 기초 처리 | 완료 | 단계 건설 |
| 중간 높이 차 `지형 공사 포함` 명시 | **완료/부분** | Alpha.44 footprint span3–4 명시/허용, >4 거부. 실제 다양한 지형 acceptance 남음 |
| 옹벽/건물용 지형 적응 | **완료/부분** | Alpha.44 exposed edge 깊은 support에 실제 stone cobblestone retaining/foundation. 더 복잡한 건물 계단/대형 옹벽은 남음 |
| 큰/위험 지형 거부 | 완료 | span>4 + block entity/fluid/unsafe/support 검사 |
| 선택 영역 절토/성토 후반 보조 | **미구현** | 임의 선택영역 토목 도구 없음 |
| 대형 협곡/터널/기념비급 토목 | 미구현 | 현재 small road bridge + bounded building terrain까지만 |
| 부지→운반→기초→골조→벽→지붕→마감 | 완료 | persisted physical phases |
| 승인 순간 전체 비용 삭제 금지 | 완료 | 실제 배치 진척과 자재 소비 연결 |
| 높은/큰 건물 물리 시공 | 완료/부분 | 감시탑·병영·건설소·고급 제작소도 scaffold/haul 사용. 실동선 검증 필요 |
| 플레이어 건축물/컨테이너 보호 | 완료/부분 | 주요 공사경로 보호. 폭발/피스톤 전면 검증 남음 |
| 자동 건설 물류 지원 | 완료/부분 | 건설소 4배럴 + 물리 보급 주민 + Alpha.44 retaining stone. 복수 프로젝트/대형 토목 미완 |
| 공사 상태 compact 표시 | 완료/부분 | Alpha.43 HUD active project label/progress. 세부 재료/terrain surcharge 표시 breadth는 제한적 |

### Alpha.44 bounded medium-terrain 감사

- `SettlementConstructionService` 하나가 계속 grading/terrain/build authority다.
- small terrain span은0–2, medium span은3–4, 그 이상은 reject한다.
- natural cut은 project grade 기준 최대3블록으로 제한한다.
- support depth는 기존 최대3블록 범위를 넘지 않는다.
- exposed outer edge + support depth>=2에서만 retaining stone이 필요하다.
- retaining extra stone은 프로젝트당 최대96이다.
- placement/start resource check에 terrain surcharge가 포함된다.
- retaining cell 적용 전에 builder가 real stone을 loaded settlement storage에서 추출해 main hand로 운반하고 site barrel에 넣으며, 해당 stone을 실제 consume한 뒤 cobblestone support가 놓인다.
- shallow fill은 coarse dirt이고 회수 가능한 free economic block 생성 경로가 아니다.
- `destroyBlock`, loose drops, force-load, teleport inventory가 없다.
- 선택 영역 절토/성토 기능까지 완성했다고 주장하지 않는다.

## 4. 주민 / 생산 / 방어

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 건설가 | 완료 | 전용 builder |
| 건설 보급 역할 | 완료/부분 | 건설소별 physical supply runner. 장기 duplicate/pathfinding 실플레이 필요 |
| 벌목꾼 | 완료 | 실제 나무 작업 |
| 농부 | 완료 | 실제 작물 작업 |
| 광부 | 완료 | 실제 유한 광석 작업 |
| 채석공 | 완료 | 노출 석재 작업 |
| 어업 주민 | 완료/부분 | Alpha.40 qualifying general outpost별 visible rod/shore 이동/실제 어획. 부두·선박 presentation 미완 |
| 대장장이 | 부분 | 수리 기능은 있으나 주민/제작 연출 약함 |
| 작업장 전문 제작자 | 완료/부분 | 외부 무기 물리 정비 장인 |
| 고급 제작 전문 역할 | 완료/부분 | Alpha.39 건물 귀속 visible specialist. 민간 population/job 통합은 후속 정리 |
| 근거리 경비 | 완료/부분 | 경비초소 + 기본 경비 |
| 감시/장거리 대응 | 완료/부분 | 감시탑별 response golem, loaded 위협 대응. 경보/실전 검증 남음 |
| 정식 주둔 병력 | 완료/부분 | 병영별3슬롯, 식량/금속 충원, 영구 귀속. 사람형 병사/병과 미완 |
| 위험지역 전초 수비대 | 완료/부분 | Alpha.41 loaded 다중 위험 근거 → 전초당1 수비대, 현지 실물 보급 충원, 안전화 시 stand-down. 실전 pathfinding/balance 미검증 |
| 운송업자 | 완료 | 전초별 영구 태그 + 도로 물류 + Alpha.41 동일 운송자의 군사 전초 역방향 보급 |
| 상인 | 부분 | 시장 방문 상인/물리 판매. 구매·고급 교역 미완 |
| 로드 지역 실제 이동·작업 | 완료 | 주요 생산/어업/운송/제작/건설/방어 |
| 언로드 저빈도 논리 시뮬레이션 | **완료/부분** | Alpha.42 최대1일 bounded work-time debt + 로드 후 실제 물리 작업 catch-up. 실제 장시간/재로드/악용 검증 전에는 완전 종료로 보지 않음 |
| 자동 직업 배치 | 완료/부분 | 주요 역할 자동화. 서비스 전문직과 민간 job 통합 정리 여지 |

병영은 민간 population/housing과 분리되며 공짜 고티어 증원 백엔드는 제거된 상태를 유지한다. 현재 병사/전초 수비대 Iron Golem proxy는 최종 프레젠테이션이 아니다.

## 5. 성장 단계 / 탐험 되먹임

| 단계 | 원본 주요 해금 | 상태 |
| --- | --- | --- |
| 개척 캠프 | 창고·주택·벌목·소농장 | 완료 |
| 촌락 | 채석·광산·대장간·경비 | 완료/부분 |
| 마을 | 도로·다리·시장·첫 전초·건설 물류 | 완료/부분 — 실동선/토목 검증 남음 |
| 개척 도시 | 병영·고급 제작·여러 전초 | 완료/부분 — Alpha.45 탐험 가속 루트 포함, 실런타임 검증 남음 |
| 영지 | 전문 거점·후반 방어·고급 교역 | 부분 — 수변/군사/언로드/compact UI/탐험 progression 1차 포함. 항구형 표현·고급 교역·최종 companion runtime 미완 |

### Alpha.45 탐험/정복 progression 감사

- 서버는100틱마다 온라인 플레이어의 **현재 이미 로드된 위치**만 structure manager로 확인한다.
- 원거리 구조물을 locate하거나 새 청크를 생성하지 않는다.
- registry namespace가 `minecraft`, `frontier_settlement`, `neoforge`가 아닌 실제 structure piece만 external discovery 후보가 된다.
- Dungeons and Taverns/Repurposed Structures 등 특정 companion Java class를 import하지 않는다.
- 동일 structure type은 generated instance가 몇 개든1회만 기록한다.
- direct player kill만 conquest에 들어간다.
- Ender Dragon/Wither는 명시 대상이다.
- 외부 `Mob`은 max health>=80일 때 generic strong-enemy/boss milestone 후보이며 동일 entity type은1회만 기록한다.
- discovered external structure types max64, conquest types max32로 SavedData가 bounded다.
- score = `min(8, structure type count + conquest type count*3)`이다.
- score는 ItemStack/재화/교역 포인트가 아니다.
- frontier town 기존 루트(pop8, outpost2, mine, quarry)는 그대로 유지된다. 추가 가속 루트는 pop7 + 동일 핵심 인프라 + score2다.
- domain 기존 루트(pop16, outpost4, mine, farm2)는 그대로 유지된다. 추가 가속 루트는 pop14 + outpost3 + 동일 생산 인프라 + score5다.
- 오래된 save는 빈 discovery 리스트로 로드되어도 기존 tier 루트를 잃지 않는다.
- `/frontier status`에 structure종/강적종/score를 compact하게 표시한다.

## 6. 건물 / 경제 / 전문 기능

현재 functional family는 정확히 **15**다.

| 계열 | 상태 | 비고 |
| --- | --- | --- |
| 주택 | 완료 | housing |
| 벌목소 | 완료 | 자동 목재 생산 |
| 농장 | 완료 | 자동 식량 생산 |
| 채석장 | 완료 | 실제 노출 석재 |
| 광산 | 완료 | 실제 유한 광석 |
| 창고 | 완료/부분 | physical storage |
| 건설소 | 완료/부분 | 자재 staging/runner |
| 대장간 | 부분 | 기능 breadth/연출 확장 여지 |
| 작업장 | 완료/부분 | external weapon repair |
| 고급 제작소 | 완료/부분 | relic+metal+weapon forge 1차 |
| 경비초소 | 완료/부분 | 기본 defense |
| 감시탑 | 완료/부분 | loaded response |
| 병영 | 완료/부분 | supplied 3-slot garrison |
| 시장 | 부분 | physical relic sale, 구매/고급 교역 breadth 남음 |
| 수레 정거장 | 완료/부분 | physical freight hub, moving wagon presentation 없음 |

새 family는 원본 역할을 채울 때만 추가한다. 숫자 맞추기용16~20번째 건물을 만들지 않는다.

## 7. 도로 / 전초 / 영토

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 물리 도로 시공 | 완료 | grading + hauling |
| 1블록 단차 계단 | 완료 | cobblestone stairs |
| 짧은 물길 다리 | 완료/부분 | max6 centerline stone-brick deck |
| 대형 협곡 다리/터널 | 미구현 | later civil engineering |
| 전초기지 물리 시공 | 완료 | persisted |
| 전초 특화 | 완료/부분 | lumber/quarry/mining/agriculture + dynamic fishing/military |
| 수변 특화 | 완료/부분 | Alpha.40 물리 어획/도로 물류, harbor/boat/trader 미완 |
| 위험지역 군사 특화 | 완료/부분 | Alpha.41 one supplied sentry, 실전 검증 남음 |
| 전초 물류 | 완료 | Alpha.27 single road-bound transporter authority |
| 군사 역보급 | 완료/부분 | 동일 transporter가 food/metal town→outpost 운반 |
| 언로드 작업 보정 | 완료/부분 | Alpha.42 bounded debt, 실플레이 검증 남음 |
| biome-aware companion 특화 | 부분/미구현 | 일부 world state inference만, richer companion biome roles 미완 |

`single authority for outpost transport` 계약은 유지한다. 수변/군사/향후 wagon 표현도 별도 long-distance economy controller를 만들면 안 된다.

## 8. 외부 콘텐츠 / 탐험

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| Terralith terrain breadth | 외부/후보검증 | lock 완료, fresh-world runtime 미검증 |
| Dungeons and Taverns 구조/던전 | 외부/후보검증 | lock 완료. Alpha.45 generic external structure registry detection에 참여 가능하지만 실제 pack detection 미검증 |
| Repurposed Structures | 외부/후보검증 | 동일 |
| Better Combat | 외부/후보검증 | lock 완료, soldier/player full runtime 미검증 |
| Weapons Expanded | 외부/부분/후보검증 | 외부 무기 인식/repair/forge, full breadth 미검증 |
| Lootr | 외부/후보검증 | loot breadth provider, runtime 미검증 |
| external structure 발견 → settlement growth | **완료/부분** | Alpha.45 unique loaded structure-type milestones → tier accelerator. rare NPC/구조별 보상 breadth 남음 |
| boss/강적 정복 → settlement growth | **완료/부분/후보검증** | dragon/wither + generic external max-health>=80 player kill milestone. 실제 companion boss pack breadth는 현재 lock/런타임에서 검증되지 않음 |
| 탐험 희귀재료 → 시장/제작 | 완료/부분 | relic market + advanced forging. broader recipe/trade 미완 |

Alpha.45는 companion adventure 콘텐츠를 Frontier가 소유한다고 주장하지 않는다. 외부 구조/보스는 registry/entity observation만 하고 생성·전리품·AI는 원래 모드의 권위다.

## 9. Companion UI / Jade / Xaero

### Jade

상태: **완료/부분/후보검증**

- exact candidate artifact `26.2.2+neoforge`, version ID `HLYMycSr`에 compileOnly.
- `snownee.jade` import는 `compat/jade` 아래로 격리.
- 서버는 Jade 전용 resource/state를 만들지 않는다.
- provider는 synchronized presentation context + crosshair position으로 title/detail/progress만 표시.
- Jade 미설치가 core boot/resource/jobs/construction/logistics/progression에 영향을 주면 안 된다.

### Xaero

상태: **미구현/후보검증 (marker), 완료/부분 (HUD collision avoidance)**

- Alpha.43은 `xaerominimap` 존재 시 Frontier top-left HUD를 아래로 이동시킨다.
- locked Xaero's Minimap26.4.2 exact compile 조사에서 과거 public `WaypointsManager` class/API가 존재하지 않음을 확인했다.
- 최신 내부 waypoint set/mixin/reflection을 강제로 쓰는 방식은 companion version drift가 core boot를 깨뜨릴 위험 때문에 채택하지 않았다.
- 따라서 본진/전초/도로 marker synchronization은 아직 미구현이다.
- 안정적인 supported seam이 생기기 전에는 “완료”로 표시하지 않는다.

## 10. UI / 정보 구조

| 요구사항 | 상태 |
| --- | --- |
| 상시 compact resource HUD | 완료 |
| 월드형 건물 placement | 완료 |
| 회전/재료/가능 여부 preview | 완료 |
| road/outpost placement | 완료/부분 |
| 건물 상태/작업 진행 정보 | 완료/부분 — Alpha.43 active project label/% + Jade target context |
| 수변 전초 상태 노출 | 완료/부분 — `/frontier status` + Jade outpost role context |
| 위험지역 군사 전초 상태 노출 | 완료/부분 — status/Jade role context, 세부 supply/sentry HUD는 없음 |
| 언로드 보정 상태 노출 | 완료/부분 — status deferred ticks, `가상 자원·가상 화물 0` |
| 탐험/정복 진척 상태 노출 | **완료/부분** — Alpha.45 rare event message + `/frontier status` 한 줄. 별도 quest dashboard 없음 |
| 물리 자재 흐름 가시화 | 완료/부분 |
| compact side notification | 완료/부분 — tier/project/building/outpost, 우측 max3/6초 |
| Jade 기반 최소 상태 노출 | 완료/부분/후보검증 |
| Xaero 본진/전초/도로망 연동 | 미구현/후보검증 — HUD collision avoidance만 완료 |

새 기능마다 새 키나 새 대형 dashboard를 만드는 방식은 금지한다.

## 11. 현재 가장 큰 남은 갭

우선순위는 실플레이 회귀가 생기면 즉시 그쪽이 우선이다. 사용자가 완성 시 테스트하기로 했으므로 그 전까지 자동/코드 검증 가능한 개발을 계속한다.

1. **full companion fresh-world client/server runtime + 멀티 검증** — 최종 테스트 단계에서 실제 실행.
2. **수변 전초 presentation/교역 breadth** — 부두·선박·수상 상인 등은 기존 도로 물류 권위를 깨지 않을 때만.
3. **고급 제작 breadth** — 실제 탐험 희귀재료가 충분히 생길 때만 recipe 추가.
4. **사람형 병사/외부 무기 장비 프레젠테이션** — companion 전투 stack과 함께 가치 검증 후.
5. **선택영역 절토/성토 + 대형 civil engineering** — player build/자원 악용 보호를 유지해야 함.
6. **탐험 bridge 2차** — rare NPC/구조별·보스별 의미 있는 settlement reward는 soft/non-farmable일 때만.
7. **장시간 survival + 2인 multiplayer acceptance**.
8. **Alpha.42 bounded unloaded-work 실플레이 검증** — pacing/save-reload/exploit/중복 여부 확인 전 완전 종료 아님.
9. **Alpha.43 UI/Jade/Xaero visual/runtime acceptance**.
10. **Xaero marker 연동** — 안정 공개/supported seam이 생길 때만. 내부 waypoint injection으로 억지 완료 금지.

## 12. Alpha.44–45 추가 실플레이 acceptance

기존 Alpha.23–43 acceptance에 아래를 추가한다.

### Alpha.44

- 실제 footprint span0–2 부지는 기존처럼 정상 grading되는지;
- span3–4 부지에서 preview/착공 메시지가 `지형 공사 포함`으로 명시되는지;
- >4 또는 fluid/block entity/unsafe support 부지가 거부되는지;
- medium cut이 자연 지형 범위에서만 bounded하게 작동하는지;
- 깊은 outer edge support가 cobblestone retaining/foundation으로 보이는지;
- 해당 cobblestone 전에 builder가 real stone을 실제 창고에서 들고 site barrel까지 이동하는지;
- extra retaining stone이 실제로 감소하며 max96을 넘지 않는지;
- save/reload mid-terrain-work에서 stone duplication/free support가 없는지;
- player container/structure가 silent 삭제되지 않는지.

### Alpha.45

- fresh companion world에서 서로 다른 실제 external structure type에 들어갈 때만 discovery가 늘어나는지;
- 같은 type의 다른 generated instance를 반복 방문해도 type count가 늘지 않는지;
- 멀리 있는 구조를 자동 locate/generate하지 않는지;
- dragon/wither 직접 player kill이 conquest로1회 기록되는지;
- qualifying external strong enemy가 있다면 direct player kill만 기록되고 같은 entity type 반복 처치가 중복되지 않는지;
- 구조물/강적 id와 score가 save/reload 후 보존되는지;
- score가8을 넘지 않는지;
- score가 wood/stone/metal/food나 ItemStack으로 소비되지 않는지;
- 기존 no-exploration save도 old frontier-town/domain route를 그대로 달성할 수 있는지;
- score2/score5 alternate accelerator가 나머지 인구/전초/광산/농장 조건 없이 tier를 공짜로 주지 않는지;
- two-player에서 어느 플레이어가 발견/정복해도 shared settlement progress가 한 번만 증가하는지;
- `/frontier status`의 구조물종/강적종/score가 실제 persisted state와 맞는지.

### 기존 핵심 acceptance 유지

- founding → 초기 핵심 건물;
- save/reload building grading/hauling;
- 건설소 runner staging/builder preference;
- road stair/short bridge;
- road → outpost → production → transporter → cart station;
- Jade 설치/미설치 boot/context;
- Xaero 설치/미설치 HUD collision, 현재 marker 미생성이 문서와 일치;
- fishing shoreline/invalid shoreline;
- military danger/sentry/reverse supply;
- deferred work debt cap24,000, real resource gating, no server-offline catch-up, max64 catch-up pickup;
- market vs normal workshop vs advanced forge intent 분리;
- external weapon enchant compatibility/no-loss failure;
- watchtower/barracks replacement cost;
- 2인 shared storage/construction/logistics;
- full companion lock fresh-world launch.

자동 감사/빌드/JAR 검증은 소스 정합과 API 컴파일을 보장하지만 실제 Minecraft 동선·밸런스·비주얼·companion runtime·Jade/Xaero 화면 조합·catch-up/terrain/exploration 체감을 대신하지 않는다.
