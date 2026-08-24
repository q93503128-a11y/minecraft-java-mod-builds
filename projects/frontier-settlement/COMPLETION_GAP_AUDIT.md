# Frontier Settlement — v0.2 완성도 갭 감사

기준 문서: `ORIGINAL_DESIGN_v0.2.md`
현재 구현 기준: `0.1.0-alpha.46`

상태:
- `완료`: 원본 핵심 요구가 실제 구현됨
- `부분`: 기능은 있으나 원본 범위가 더 남음
- `미구현`: 원본 요구가 아직 게임 기능으로 없음
- `외부`: Frontier 자체 재구현보다 companion이 콘텐츠를 공급
- `후보검증`: 버전/구성은 고정했으나 풀스택 실런타임 검증 필요

이 문서는 현재 코드에 맞춰 원본 범위를 줄이는 문서가 아니다. 기능 건물 수가 15개에 도달했고 수변/위험지역 전초, bounded unloaded-work 1차, compact Jade/status 1차, bounded medium terrain 1차, exploration/conquest progression 1차, Alpha.46 물리 수변 계류장/교역 1차가 들어갔더라도 핵심 `부분/미구현`이 남아 있는 동안 제품을 완성이라고 부르지 않는다.

## 1. 핵심 정체성 / 멀티 / 조작

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 한 월드 하나의 공동 마을 | 완료 | SavedData 기반 공유 정착지 |
| 서버 authoritative | 완료 | 자원/건설/도로/전초/진행 서버 권위 |
| 실제 ItemStack이 자원 권위 | 완료 | HUD/context는 표시 계층일 뿐 권위가 아님 |
| 핵심 직접 조작 소수 유지 | 완료 | B / R / Enter / Backspace 중심 |
| 플레이어별 개별 마을 금지 | 완료 | 단일 공동 정착지 |
| 세금/행복도/가족/거대 연구 UI 금지 | 완료 | 해당 미시관리 없음 |
| 탐험/전투가 정착 성장으로 되먹임 | **완료/부분** | Alpha.45 unique structure/conquest milestone이 tier accelerator로 연결. 희귀 NPC/보스별 특수 보상 breadth 남음 |
| 외부 모드를 콘텐츠 생산 수단으로 사용 | 완료/부분/후보검증 | 후보 스택 + 시장/작업장/고급 제작 + Jade + Alpha.45 registry bridge. 풀런타임 미검증 |

Alpha.45 탐험 진척은 공유 진행 metadata이며 재화가 아니다. 같은 구조물/강적 종류 반복으로 무한 파밍할 수 없고 기존 성장 루트를 새 점수로 막지 않는다.

## 2. 시작 / 자원 / 창고 / HUD

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 개척 표식 + 공동 창고 + 건설 주민 | 완료 | 구현 |
| HUD 목재/석재/금속/식량/인구 | 완료 | compact HUD |
| 외부 재료 범주/태그 수용 | 완료/부분 | additive Frontier + `c:` 태그. 풀 companion 실플레이 미검증 |
| 대량 저장 | 완료/부분 | 창고 + 수레 정거장 + 건설소 자재 배럴. 장기 부하 검증 필요 |
| 임의 플레이어 상자 과잉 스캔 금지 | 완료 | 정해진 settlement storage만 권위에 포함 |
| 멀티 창고/HUD 장시간 정합 | 부분 | 실제 2인 장시간 검증 필요 |
| 병영 충원도 실제 자원 사용 | 완료 | 식량8 + 금속2 물리 소비 |
| 위험지역 전초 충원도 실제 자원 사용 | 완료/부분 | 현지 stockpile 식량6 + 금속2, 기존 도로 운송자가 역방향 보급. 실동선 검증 필요 |
| 건설소 자재도 실제 자원 사용 | 완료 | 동일 ledger의 목재/석재 staging |
| 중간 지형 옹벽도 실제 자원 사용 | **완료/부분** | Alpha.44 깊은 노출 가장자리 support는 real stone haul/stage/consume 후 cobblestone 배치. 실지형 검증 필요 |
| 고급 제작 재료도 실제 자원 사용 | 완료/부분 | 유물1 + 전문 주민이 운반한 금속4. 외부 무기별 실전 호환 검증 필요 |
| 수변 전초 생산도 실제 아이템 | 완료/부분 | Alpha.40 실제 대구/연어 → 전초 stockpile → 기존 도로 운송 |
| 수변 계류장 공사도 실제 자원 사용 | **완료/부분** | Alpha.46 fishing worker가 전초 stockpile의 실제 목재를 들고 블록별 소비. 부족 시 같은 road transporter가 town wood 역보급. 실동선 검증 필요 |
| 수변 교역이 일반 stockpile을 자동판매하지 않음 | 완료 | dedicated trade barrel만 16 fish→1 emerald |
| 언로드 보정이 자원 권위를 침범하지 않음 | 완료/부분 | Alpha.42 work-time debt만 저장. 실제 물리 동작 성공 뒤에만 credit 소모. 실플레이 악용·재로드 검증 남음 |
| UI context가 새 자원 권위가 되지 않음 | 완료 | Alpha.43 표시 전용 context |
| 탐험 score가 새 자원 권위가 되지 않음 | 완료 | Alpha.45 non-spendable progression metadata |

## 3. 건설 / 지형 공사

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 3D 완성 미리보기/회전/검사 | 완료 | ghost placement |
| 작은 높이 차 자동 정리 | 완료 | 물리 worker grading |
| 기초 처리 | 완료 | 단계 건설 |
| 중간 높이 차 `지형 공사 포함` 명시 | **완료/부분** | Alpha.44 footprint span3–4 허용/명시, >4 거부. 다양한 실제 지형 acceptance 남음 |
| 옹벽/건물용 지형 적응 | **완료/부분** | real-stone exposed-edge retaining/foundation. 대형 옹벽/복잡한 건물 계단 남음 |
| 큰/위험 지형 거부 | 완료 | span>4 + block entity/fluid/unsafe/support 검사 |
| 선택 영역 절토/성토 후반 보조 | **미구현** | 임의 선택영역 토목 도구 없음 |
| 대형 협곡/터널/기념비급 토목 | 미구현 | small road bridge + bounded building terrain까지만 |
| 부지→운반→기초→골조→벽→지붕→마감 | 완료 | persisted physical phases |
| 승인 순간 전체 비용 삭제 금지 | 완료 | 실제 배치 진척과 자재 소비 연결 |
| 높은/큰 건물 물리 시공 | 완료/부분 | 감시탑·병영·건설소·고급 제작소 scaffold/haul. 실동선 검증 필요 |
| 플레이어 건축물/컨테이너 보호 | 완료/부분 | 주요 공사경로 보호. 폭발/피스톤 전면 검증 남음 |
| 자동 건설 물류 지원 | 완료/부분 | 건설소 4배럴 + 물리 보급 주민 + retaining stone. 복수 프로젝트/대형 토목 미완 |
| 공사 상태 compact 표시 | 완료/부분 | Alpha.43 HUD active project label/progress |

### Alpha.44 bounded medium-terrain 감사

- `SettlementConstructionService` 하나가 계속 grading/terrain/build authority다.
- small span 0–2, medium span 3–4, 그 이상 reject.
- natural cut은 project grade 기준 최대3블록.
- support depth 최대3블록.
- exposed outer edge + support depth>=2에서 retaining stone 필요.
- retaining extra stone 프로젝트당 최대96.
- start resource check에 surcharge 포함.
- retaining cell 전에 builder가 real stone을 loaded settlement storage에서 운반하고 site barrel에 넣으며 실제 consume.
- shallow fill은 coarse dirt이고 free economic material 생성 경로가 아니다.
- `destroyBlock`, loose drops, force-load, teleport inventory가 없다.
- 선택 영역 절토/성토 기능까지 완성했다고 주장하지 않는다.

## 4. 주민 / 생산 / 방어

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 건설가 | 완료 | 전용 builder |
| 건설 보급 역할 | 완료/부분 | 건설소별 physical supply runner. 장기 duplicate/pathfinding 검증 필요 |
| 벌목꾼 | 완료 | 실제 나무 작업 |
| 농부 | 완료 | 실제 작물 작업 |
| 광부 | 완료 | 실제 유한 광석 작업 |
| 채석공 | 완료 | 노출 석재 작업 |
| 어업 주민 | 완료/부분 | Alpha.40 rod/shore 이동/실제 어획 + Alpha.46 계류장 공사 우선 처리. 실동선 검증 필요 |
| 수변 상인 | **완료/부분** | Alpha.46 persistent local trader + dedicated barrel 16 fish→1 emerald. 이동/중복/밸런스 실검증 필요 |
| 대장장이 | 부분 | 기능 breadth/연출 확장 여지 |
| 작업장 전문 제작자 | 완료/부분 | 외부 무기 물리 정비 장인 |
| 고급 제작 전문 역할 | 완료/부분 | Alpha.39 visible specialist. 민간 population/job 통합은 후속 정리 |
| 근거리 경비 | 완료/부분 | 경비초소 + 기본 경비 |
| 감시/장거리 대응 | 완료/부분 | 감시탑별 response golem. 경보/실전 검증 남음 |
| 정식 주둔 병력 | 완료/부분 | 병영별3슬롯, 식량/금속 충원. 사람형 병사/병과 미완 |
| 위험지역 전초 수비대 | 완료/부분 | Alpha.41 loaded 다중 위험 근거 → 전초당1 수비대. 실전 pathfinding/balance 미검증 |
| 운송업자 | 완료 | 전초별 영구 태그 + 도로 물류 + 군사 food/metal 역보급 + Alpha.46 waterfront wood 역보급 |
| 로드 지역 실제 이동·작업 | 완료 | 주요 생산/어업/운송/제작/건설/방어/수변 공사 |
| 언로드 저빈도 논리 시뮬레이션 | **완료/부분** | Alpha.42 최대1일 work-time debt + 로드 후 실제 물리 catch-up. 장시간/재로드/악용 검증 필요 |
| 자동 직업 배치 | 완료/부분 | 주요 역할 자동화. 서비스 전문직/민간 job 통합 정리 여지 |

병영은 민간 population/housing과 분리되며 공짜 고티어 증원 백엔드는 제거된 상태를 유지한다. 현재 병사/전초 수비대 Iron Golem proxy는 최종 프레젠테이션이 아니다.

## 5. 성장 단계 / 탐험 되먹임

| 단계 | 원본 주요 해금 | 상태 |
| --- | --- | --- |
| 개척 캠프 | 창고·주택·벌목·소농장 | 완료 |
| 촌락 | 채석·광산·대장간·경비 | 완료/부분 |
| 마을 | 도로·다리·시장·첫 전초·건설 물류 | 완료/부분 — 실동선/토목 검증 남음 |
| 개척 도시 | 병영·고급 제작·여러 전초 | 완료/부분 — Alpha.45 탐험 가속 루트 포함 |
| 영지 | 전문 거점·후반 방어·고급 교역 | 부분 — 수변/군사/언로드/compact UI/탐험 progression/계류장 trade 1차 포함. 고급 교역 breadth·최종 runtime 미완 |

### Alpha.45 탐험/정복 progression 감사

- 100틱마다 온라인 플레이어의 **현재 이미 로드된 위치**만 structure manager로 확인.
- 원거리 locate/새 청크 생성 없음.
- `minecraft`, `frontier_settlement`, `neoforge` 외 namespace의 실제 structure piece만 external discovery 후보.
- companion Java class hard import 없음.
- 동일 structure type은 generated instance가 몇 개든1회만 기록.
- direct player kill만 conquest.
- Ender Dragon/Wither 명시 대상.
- 외부 Mob max health>=80 generic milestone, 동일 entity type1회.
- discovered structure max64, conquest max32.
- score=`min(8, structures + conquests*3)`.
- score는 ItemStack/재화가 아님.
- frontier town legacy pop8/outpost2/mine/quarry 유지 + alternate pop7/동일 인프라/score2.
- domain legacy pop16/outpost4/mine/farm2 유지 + alternate pop14/outpost3/동일 생산 인프라/score5.

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
| 수변 특화 | **완료/부분** | Alpha.40 실제 어획/도로 물류 + Alpha.46 persisted real-wood landing + local trader + dedicated trade barrel. moving boat/waterborne motion은 선택적 presentation breadth |
| 위험지역 군사 특화 | 완료/부분 | Alpha.41 one supplied sentry, 실전 검증 남음 |
| 전초 물류 | 완료 | Alpha.27 `single authority for outpost transport` |
| 군사 역보급 | 완료/부분 | 동일 transporter가 food/metal town→outpost 운반 |
| 수변 공사 역보급 | **완료/부분** | 동일 transporter가 military job 없을 때 real wood town→outpost 운반. 실동선 검증 남음 |
| 언로드 작업 보정 | 완료/부분 | Alpha.42 bounded debt, 실플레이 검증 남음 |
| biome-aware companion 특화 | 부분/미구현 | richer companion-biome roles 미완 |

`single authority for outpost transport` 계약은 유지한다. 수변/군사/향후 wagon·boat 표현도 별도 long-distance economy controller를 만들면 안 된다.

### Alpha.46 waterfront 감사

- qualifying general/fishing outpost + loaded safe waterfront geometry만 대상.
- `WaterfrontState`/`SettlementWaterfrontData`는 anchor/direction/build step만 저장하며 resource ledger가 아니다.
- bounded landing은 기존 water를 지우지 않고 spruce slab/fence/barrel로 구성.
- fishing worker가 전초 stockpile에서 real wood를 꺼내 main hand로 들고 블록당1개씩 소비.
- 공사 시작 전 이미 물고기를 들고 있으면 stockpile에 실제 반납한 뒤 construction 우선, deadlock/item loss 방지.
- local wood shortage는 같은 Alpha.27 transporter가 town storage에서 real wood를 추출해 같은 road로 역보급.
- active military reverse supply가 항상 waterfront wood supply보다 우선.
- 완공 waterfront block break 보호로 공사자원 회수 exploit 차단.
- dedicated trade barrel만 판매 대상; ordinary outpost stockpile 자동판매 없음.
- recipe: real cod/salmon16 → real emerald1, 같은 barrel에 output, 공간 없으면 stall.
- persistent local waterfront trader + Jade/status context.
- boat logistics/teleport/force-load/virtual trade points/second transporter 없음.

## 8. 외부 콘텐츠 / 탐험

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| Terralith terrain breadth | 외부/후보검증 | lock 완료, fresh-world runtime 미검증 |
| Dungeons and Taverns 구조/던전 | 외부/후보검증 | lock 완료. Alpha.45 generic detection 가능, 실제 pack detection 미검증 |
| Repurposed Structures | 외부/후보검증 | 동일 |
| Better Combat | 외부/후보검증 | lock 완료, soldier/player full runtime 미검증 |
| Weapons Expanded | 외부/부분/후보검증 | 외부 무기 repair/forge, full breadth 미검증 |
| Lootr | 외부/후보검증 | loot breadth provider, runtime 미검증 |
| external structure 발견 → settlement growth | **완료/부분** | Alpha.45 unique loaded structure-type milestones → tier accelerator. rare NPC/구조별 보상 breadth 남음 |
| boss/강적 정복 → settlement growth | **완료/부분/후보검증** | dragon/wither + generic external max-health>=80 player kill milestone. 실제 companion boss breadth 미검증 |
| 탐험 희귀재료 → 시장/제작 | 완료/부분 | relic market + advanced forging. broader recipe/trade 미완 |

Frontier는 companion adventure 콘텐츠를 소유하지 않는다. 구조/보스는 registry/entity observation만 하고 생성·전리품·AI는 원래 모드의 권위다.

## 9. Companion UI / Jade / Xaero

### Jade

상태: **완료/부분/후보검증**

- exact candidate artifact `26.2.2+neoforge`, version ID `HLYMycSr`에 compileOnly.
- `snownee.jade` import는 `compat/jade` 아래 격리.
- 서버는 Jade 전용 resource/state를 만들지 않는다.
- provider는 synchronized presentation context + crosshair position으로 title/detail/progress만 표시.
- Alpha.46 dedicated waterfront trade barrel도 compact context에 포함.
- Jade 미설치가 core boot/resource/jobs/construction/logistics/progression에 영향을 주면 안 된다.

### Xaero

상태: **미구현/후보검증 (marker), 완료/부분 (HUD collision avoidance)**

- Alpha.43은 `xaerominimap` 존재 시 Frontier top-left HUD를 아래로 이동.
- locked Xaero's Minimap26.4.2 exact compile 조사에서 과거 public `WaypointsManager` class/API 부재 확인.
- 최신 내부 waypoint set/mixin/reflection 강제 사용은 채택하지 않음.
- 본진/전초/도로 marker synchronization은 아직 미구현.
- stable supported seam이 생기기 전에는 완료로 표시하지 않는다.

## 10. UI / 정보 구조

| 요구사항 | 상태 |
| --- | --- |
| 상시 compact resource HUD | 완료 |
| 월드형 건물 placement | 완료 |
| 회전/재료/가능 여부 preview | 완료 |
| road/outpost placement | 완료/부분 |
| 건물 상태/작업 진행 정보 | 완료/부분 — Alpha.43 active project label/% + Jade target context |
| 수변 전초 상태 노출 | **완료/부분** — status + Jade outpost role + Alpha.46 dedicated trade-barrel context |
| 위험지역 군사 전초 상태 노출 | 완료/부분 — status/Jade role context |
| 언로드 보정 상태 노출 | 완료/부분 — deferred ticks, `가상 자원·가상 화물 0` |
| 탐험/정복 진척 상태 노출 | 완료/부분 — rare event message + status 한 줄 |
| 물리 자재 흐름 가시화 | 완료/부분 |
| compact side notification | 완료/부분 — tier/project/building/outpost, 우측 max3/6초 |
| Jade 기반 최소 상태 노출 | 완료/부분/후보검증 |
| Xaero 본진/전초/도로망 연동 | 미구현/후보검증 — HUD collision avoidance만 완료 |

새 기능마다 새 키나 새 대형 dashboard를 만드는 방식은 금지한다.

## 11. 현재 가장 큰 남은 갭

우선순위는 실플레이 회귀가 생기면 즉시 그쪽이 우선이다. 완성/test-worthy 지점 전까지 자동/코드 검증 가능한 개발을 계속한다.

1. **고급 제작 breadth** — 실제 탐험 희귀재료가 충분히 생길 때만 recipe 추가.
2. **사람형 병사/외부 무기 장비 프레젠테이션** — companion combat stack과 함께 가치 검증.
3. **선택영역 절토/성토 + 대형 civil engineering** — player build/자원 악용 보호 유지.
4. **탐험 bridge 2차** — rare NPC/구조별·보스별 의미 있는 settlement reward, soft/non-farmable 조건.
5. **biome-aware companion 전초 특화** — stable data seam이 있을 때만.
6. **Alpha.42 bounded unloaded-work 실플레이 검증** — pacing/save-reload/exploit/중복.
7. **Alpha.43 UI/Jade/Xaero visual/runtime acceptance**.
8. **Alpha.46 waterfront 실플레이 검증** — site/pathfinding, fish-cargo return, real-wood reverse supply, military precedence, break protection, trader duplication, 16→1 balance.
9. **장시간 survival + 2인 multiplayer acceptance**.
10. **full companion fresh-world client/server runtime** — 최종 테스트 단계에서 실제 실행.
11. **Xaero marker 연동** — 안정 공개/supported seam이 생길 때만.
12. **moving boat/waterborne merchant 표현** — 실제 가치가 있을 때 presentation/local behavior로만; 별도 물류 권위 금지.

## 12. Alpha.44–46 추가 실플레이 acceptance

기존 Alpha.23–43 acceptance에 아래를 추가한다.

### Alpha.44

- span0–2 기존 grading;
- span3–4 `지형 공사 포함` 표시;
- >4/fluid/block entity/unsafe support 거부;
- bounded natural cut/fill;
- deep outer edge cobblestone retaining/foundation;
- builder real-stone haul/site-barrel consume;
- max96 surcharge;
- save/reload 중복/free support 없음;
- player container/structure silent 삭제 없음.

### Alpha.45

- 서로 다른 actual external structure type만 discovery 증가;
- 같은 type 다른 instance 반복은 증가 없음;
- 원거리 locate/generate 없음;
- direct player dragon/wither/external strong-enemy kill만 conquest;
- same entity type 중복 없음;
- structure/boss id와 score save/reload 보존;
- score<=8, ItemStack/resource 소비 불가;
- legacy no-exploration save old tier routes 유지;
- alternate accelerator는 나머지 인프라/인구 조건 유지;
- 2인 shared progress 한 번만 증가;
- `/frontier status`와 persisted state 일치.

### Alpha.46

- qualifying shoreline만 persisted landing 생성;
- blocked/unsafe shoreline는 player/world block을 덮어쓰지 않음;
- landing build step save/reload 보존;
- fishing worker가 actual outpost wood를 운반하고 placement당1개 소비;
- worker 손에 기존 fish가 있으면 먼저 stockpile로 반납하고 공사 시작;
- local wood 부족 시 같은 road transporter가 town에서 wood를 실제 추출/운반;
- military reverse supply 활성 시 military food/metal이 waterfront wood보다 우선;
- completed landing block normal break로 자원 회수 불가;
- dedicated trade barrel 외 ordinary outpost stock 자동판매 없음;
- fish16→emerald1 정확, output full이면 consume 없이 stall;
- waterfront trader save/reload duplicate 없음;
- Jade/status의 landing/trade 정보와 실제 상태 일치;
- no boat logistics/teleport/force-load/virtual trade points/second transport authority.

### 기존 핵심 acceptance 유지

- founding → 초기 핵심 건물;
- save/reload building grading/hauling;
- construction-office runner staging/builder preference;
- road stair/short bridge;
- road → outpost → production → transporter → cart station;
- Jade 설치/미설치 boot/context;
- Xaero 설치/미설치 HUD collision, marker 미생성이 문서와 일치;
- fishing shoreline/invalid shoreline;
- military danger/sentry/reverse supply;
- deferred work debt cap24,000, real resource gating, no server-offline catch-up, max64 catch-up pickup;
- market vs normal workshop vs advanced forge intent 분리;
- external weapon enchant compatibility/no-loss failure;
- watchtower/barracks replacement cost;
- 2인 shared storage/construction/logistics/exploration/waterfront;
- full companion lock fresh-world launch.

자동 감사/빌드/JAR 검증은 소스 정합과 API 컴파일을 보장하지만 실제 Minecraft 동선·밸런스·비주얼·companion runtime·Jade/Xaero 화면 조합·catch-up/terrain/exploration/waterfront 체감을 대신하지 않는다.
