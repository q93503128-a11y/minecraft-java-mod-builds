# Frontier Settlement — v0.2 완성도 갭 감사

기준 문서: `ORIGINAL_DESIGN_v0.2.md`
현재 구현 기준: `0.1.0-alpha.48`

상태:
- `완료`: 원본 핵심 요구가 실제 구현됨
- `부분`: 기능은 있으나 원본 범위가 더 남음
- `미구현`: 원본 요구가 아직 게임 기능으로 없음
- `외부`: Frontier 자체 재구현보다 companion이 콘텐츠를 공급
- `후보검증`: 버전/구성은 고정했으나 풀스택 실런타임 검증 필요

이 문서는 현재 구현에 맞춰 원본 v0.2 범위를 축소하지 않는다. Alpha.48까지 기능이 늘어났어도 selected-area 토목, 실물 군사 armory, 일부 탐험/전초 breadth, 장시간 multiplayer 및 full companion runtime 등이 남아 있는 동안 완성이라고 부르지 않는다.

## 1. 핵심 정체성 / 멀티 / 조작

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 한 월드 하나의 공동 마을 | 완료 | SavedData 기반 공유 정착지 |
| 서버 authoritative | 완료 | 자원/건설/도로/전초/진행 서버 권위 |
| 실제 ItemStack이 자원 권위 | 완료 | HUD/context는 표시 계층 |
| 핵심 직접 조작 소수 유지 | 완료 | B / R / Enter / Backspace |
| 플레이어별 개별 마을 금지 | 완료 | 단일 공동 정착지 |
| 세금/행복도/가족/거대 연구 UI 금지 | 완료 | 미시관리 없음 |
| 탐험/전투 → 정착 성장 | 완료/부분 | Alpha.45 unique structure/conquest milestone + tier accelerator, rare NPC/구조별 보상 breadth 남음 |
| companion 콘텐츠 활용 | 완료/부분/후보검증 | 후보 lock + soft bridges, full runtime 미검증 |

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
| 수변 교역 실제 아이템 | 완료 | dedicated barrel 16 fish→1 emerald, ordinary stockpile auto-sale 없음 |
| Alpha.39 first forge 실제 자원 | 완료/부분 | relic1 + physically hauled metal4 |
| Alpha.47 domain reforge 실제 자원 | 완료/부분 | relic2 + physically hauled metal8 |
| Alpha.42 언로드 보정이 가상 자원화되지 않음 | 완료/부분 | work-time debt only, 실플레이 pacing/악용 검증 남음 |
| Alpha.48 군사 시각 무기가 실제 자원으로 위장되지 않음 | **완료/부분** | visual service sword is client render state only; server ItemStack/loot/storage 없음. 실런타임 확인 남음 |

`single authority for outpost transport` 계약은 유지한다. 수변/군사/향후 wagon·boat 표현이 두 번째 장거리 물류 권위가 되어서는 안 된다.

## 3. 건설 / 지형 공사

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 3D preview/회전/배치 검사 | 완료 | ghost placement |
| 작은 높이 차 자동 grading | 완료 | 물리 worker grading |
| 중간 높이 차 `지형 공사 포함` | 완료/부분 | Alpha.44 span3–4 |
| real-stone retaining/foundation | 완료/부분 | exposed deep edge support |
| 큰/위험 지형 거부 | 완료 | span>4/fluid/block entity/unsafe support |
| 선택 영역 절토/성토 | **미구현** | 다음 주요 기능 갭 |
| 대형 협곡 다리/터널/기념비급 토목 | 미구현 | bounded small road/building terrain까지만 |
| 물리 단계 건설 | 완료 | grading→haul→foundation/frame/walls/roof/finish |
| 플레이어 건축/컨테이너 보호 | 완료/부분 | 주요 경로 보호, 더 넓은 토목 전면 검증 필요 |
| 건설소 자동 물류 지원 | 완료/부분 | physical staging runner |

### Alpha.44 감사 유지

- span0–2 기존 grading;
- span3–4 bounded medium work;
- cut/support depth 최대3;
- deep exposed edge에 real retaining stone;
- extra stone max96/project;
- builder가 storage→carry→site barrel→consume 순서로 실제 석재 사용;
- no free cobble, `destroyBlock`, loose-drop excavation, force-load, teleport inventory;
- selected-area cut/fill까지 완료했다고 주장하지 않는다.

## 4. 주민 / 생산 / 방어

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 건설가/벌목/농부/광부/채석공 | 완료 | physical loaded work |
| 어업 주민 | 완료/부분 | loaded shoreline fishing + pier construction |
| 수변 상인 | 완료/부분 | local trader + dedicated barrel |
| 작업장/고급 제작 전문 주민 | 완료/부분 | repair + first forge + domain reforge |
| 근거리 경비 | 완료/부분 | guard post |
| 감시/장거리 대응 | 완료/부분 | watchtower response body |
| 병영 정식 주둔 병력 | **완료/부분** | 3 supplied slots, Alpha.48 humanoid visible body. 실전 animation/armory 남음 |
| 위험지역 전초 수비대 | **완료/부분** | one supplied sentry, Alpha.48 same humanoid body. 실전 검증 남음 |
| 사람형 군사 presentation | **완료/부분** | `FrontierSoldierEntity extends IronGolem` + humanoid client renderer + visual service sword |
| 실물 외부무기 군사 armory/loadout | **미구현/부분** | Alpha.48 sword는 client-only visual. Weapons Expanded ItemStack을 실제 보급/장비하는 경제 루프는 아직 없음 |
| 자동 직업 배치 | 완료/부분 | 주요 역할 자동화, 서비스 전문직 정리 여지 |
| 언로드 저빈도 보정 | 완료/부분 | bounded time debt, no virtual item authority |

### Alpha.48 supplied humanoid military 감사

- Frontier 전용 `frontier_soldier` 엔티티 타입을 사용.
- `FrontierSoldierEntity`는 `IronGolem`을 상속해 검증된 서버 AI/공격/목표 시스템을 유지.
- entity attributes도 `IronGolem.createAttributes()` 기반.
- 인간형 크기/클라이언트 humanoid model로 presentation 변경.
- **visual service sword is never a server ItemStack**.
- renderer만 iron sword ItemStack을 render state에 넣고 서버 military service는 장비 slot에 검을 넣지 않음.
- 따라서 죽여도 visual sword를 획득할 수 없고 settlement storage에도 존재하지 않음.
- 병영은 여전히 정확히 3 slots/barracks.
- 신규 병영 병사 비용은 food8 + metal2 유지.
- 위험지역 전초는 여전히 max1 sentry, 비용 food6 + metal2 유지.
- tagged military drops clear 유지.
- old tagged Iron Golem soldier/sentry는 loaded 상태에서 1:1로 Frontier soldier로 migration.
- migration은 name/tags/rotation/health를 보존하고 old entity를 discard.
- migration은 recruit consume 함수를 호출하지 않으므로 이중 과금 없음.
- migration은 새 슬롯/민간 population을 만들지 않음.
- Better Combat/Weapons Expanded Java class hard dependency 없음.
- actual external-weapon physical armory는 아직 완료가 아님.

## 5. 성장 단계 / 탐험 되먹임

| 단계 | 상태 | 현재 |
| --- | --- | --- |
| 개척 캠프 | 완료 | starter storage/builder/basic production |
| 촌락 | 완료/부분 | quarry/mine/blacksmith/guard |
| 마을 | 완료/부분 | roads/market/outpost/construction logistics |
| 개척 도시 | 완료/부분 | barracks/advanced workshop/multiple outposts |
| 영지 | 완료/부분 | exploration accelerator + Alpha.47 reforge + specialized remote roles, breadth/final runtime 남음 |

Alpha.45는 already-loaded external structure type 및 direct-player conquest type을 unique milestone로 기록한다. score는 capped8 non-spendable metadata이며 legacy tier route를 폐기하지 않는다.

## 6. 건물 / 전문 기능

현재 functional family는 정확히 **15**다.

| 계열 | 상태 | 비고 |
| --- | --- | --- |
| 주택 | 완료 | housing |
| 벌목소 | 완료 | wood |
| 농장 | 완료 | food |
| 채석장 | 완료 | exposed stone |
| 광산 | 완료 | finite ore |
| 창고 | 완료/부분 | physical storage |
| 건설소 | 완료/부분 | material staging/runner |
| 대장간 | 부분 | breadth 여지 |
| 작업장 | 완료/부분 | external weapon repair |
| 고급 제작소 | 완료/부분 | Alpha.39 first forge + Alpha.47 domain reforge |
| 경비초소 | 완료/부분 | local defense |
| 감시탑 | 완료/부분 | loaded response |
| 병영 | 완료/부분 | supplied 3-slot garrison + Alpha.48 humanoid body |
| 시장 | 부분 | physical relic sale, broader purchase/trade breadth 여지 |
| 수레 정거장 | 완료/부분 | freight hub, moving wagon optional |

새 family는 원본 역할을 채울 때만 추가한다. 숫자 맞추기용16~20번째 건물을 만들지 않는다.

### Alpha.47 domain reforge 감사 유지

- DOMAIN only;
- already-enchanted recognized external weapon + relic2 + metal8;
- same protected commission barrel/specialist;
- candidate must be new + item-supported + compatible with existing set;
- copy first, improvement/preservation validation first;
- existing enchantments are never removed or downgraded;
- no-compatible/no-improvement면 weapon/relic/metal 소비 없음;
- Alpha.39 first forge relic1+metal4/power30 path 유지;
- no hard Weapons Expanded class/item reference.

## 7. 도로 / 전초 / 영토

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 물리 도로 시공 | 완료 | grading + hauling |
| 1블록 단차 계단 | 완료 | cobblestone stairs |
| 짧은 물길 다리 | 완료/부분 | max6 centerline bridge |
| 대형 협곡/터널 | 미구현 | future civil engineering |
| 전초기지 물리 시공 | 완료 | persisted |
| 전초 특화 | 완료/부분 | lumber/quarry/mining/agriculture + dynamic fishing/military |
| 수변 특화 | 완료/부분 | fishing + real-wood landing + dedicated trade |
| 위험지역 군사 특화 | 완료/부분 | one supplied Alpha.48 humanoid sentry |
| 전초 물류 | 완료 | Alpha.27 one authority |
| 군사 역보급 | 완료/부분 | same transporter food/metal |
| 수변 역보급 | 완료/부분 | same transporter wood after military priority |
| biome-aware companion specialization | 부분/미구현 | stable data seam 필요 |

## 8. 외부 콘텐츠 / 탐험 / companion

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| Terralith terrain breadth | 외부/후보검증 | candidate lock |
| Dungeons and Taverns | 외부/후보검증 | generic Alpha.45 observation 가능 |
| Repurposed Structures | 외부/후보검증 | generic observation 가능 |
| Better Combat | 외부/후보검증 | candidate; Alpha.48 core hard-link 없음 |
| Weapons Expanded | 외부/부분/후보검증 | repair/forge/reforge recognition, soldier physical armory 미구현 |
| Lootr | 외부/후보검증 | loot provider |
| structure 발견→growth | 완료/부분 | unique type accelerator |
| boss/강적→growth | 완료/부분/후보검증 | dragon/wither + generic qualifying external Mob type |
| 희귀재료→시장/제작 | 완료/부분 | relic market + two advanced forge paths |

Frontier는 companion adventure 콘텐츠 생성/전리품/AI를 소유하지 않는다.

## 9. UI / Jade / Xaero

- compact resource/project HUD: 완료/부분;
- side notices: 완료/부분;
- Jade crosshair context: 완료/부분/후보검증;
- Xaero HUD collision avoidance: 완료/부분;
- true Xaero settlement/outpost marker sync: 미구현/후보검증;
- Alpha.48 soldier renderer: 완료/부분, 실화면 품질/attack animation acceptance 필요.

Locked Xaero 26.4.2에는 historical public `WaypointsManager` API가 없으므로 brittle internal/mixin/reflection marker integration은 하지 않는다.

## 10. 현재 가장 큰 남은 갭

실플레이 회귀가 생기면 즉시 회귀 수정이 우선이다. 그 외 현재 우선순위:

1. **선택영역 절토/성토 + 대형 civil engineering** — player build/resource exploit 보호 필수.
2. **실물 군사 armory / 외부 무기 loadout** — actual ItemStack 공급이 가능할 때만; free Weapons Expanded weapon minting 금지.
3. **탐험 bridge 2차** — rare NPC/구조별/보스별 의미 있는 settlement reward, soft/non-farmable.
4. **biome-aware companion 전초 특화** — stable data seam 필요.
5. 추가 high-tier crafting — distinct exploration material/use-case가 생길 때만.
6. Alpha.42 unloaded catch-up pacing/save-reload/exploit acceptance.
7. Alpha.43 Jade/Xaero/HUD visual/runtime acceptance.
8. Alpha.46 waterfront site/path/trade acceptance.
9. Alpha.47 reforge external-weapon breadth/no-loss acceptance.
10. **Alpha.48 humanoid render/attack animation + legacy migration acceptance**.
11. 장시간 survival + 2인 multiplayer acceptance.
12. full companion fresh-world client/server runtime.
13. stable supported API가 생길 때만 Xaero marker.
14. moving wagon/boat은 presentation/local behavior로만.

## 11. Alpha.48 추가 실플레이 acceptance

기존 Alpha.23–47 acceptance에 다음을 추가한다.

- 신규 barracks soldier가 인간형으로 렌더링됨;
- dangerous-region sentry도 같은 인간형 military body를 사용함;
- 공격 중 팔/검 pose가 허용 가능한 수준으로 보임;
- humanoid collision/pathing이 병영/전초 구조와 충돌하지 않음;
- 서버 damage/health/target selection이 이전 supplied body와 의도대로 유지됨;
- 병영 3 slot과 food8+metal2 충원비 유지;
- remote max1 sentry와 local food6+metal2 충원비 유지;
- visual sword가 entity inventory, settlement storage, death drop 어디에도 생성되지 않음;
- visual sword가 server attack attribute를 바꾸지 않음;
- old tagged barracks Iron Golem은 loaded 시 정확히 1 Frontier soldier로 migration;
- old tagged remote sentry도 loaded 시 정확히 1개로 migration;
- migration이 name/tags/health를 보존;
- migration에 food/metal 재소비 없음;
- reload 반복으로 duplicate military body가 생기지 않음;
- civilian population/housing 수치 변동 없음;
- Better Combat/Weapons Expanded 설치/미설치 모두 Frontier core boot가 깨지지 않음;
- actual external companion weapon을 장착한 것처럼 문서/UI에서 허위 주장하지 않음.

## 12. 기존 핵심 acceptance 유지

- founding→early growth;
- physical grading/hauling/reload;
- construction-office staging;
- road stair/short bridge;
- outpost production/transport/cart station;
- fishing/waterfront trade and military reverse supply priority;
- no virtual cargo/resource unloaded catch-up;
- exploration milestone dedupe/legacy tier routes;
- market vs repair vs first forge vs domain reforge intent separation;
- Alpha.47 no-loss/no-improvement behavior;
- Jade optional boot/context;
- Xaero HUD offset but no false marker completion claim;
- two-player shared state;
- full companion candidate fresh-world launch at final test point.

자동 감사/Java25 build/JAR verify는 코드·API·정본 정합을 보장하지만 실제 Minecraft 동선·렌더 품질·밸런스·full companion runtime을 대신하지 않는다.
