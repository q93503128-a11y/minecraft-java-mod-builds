# Frontier Settlement — v0.2 완성도 갭 감사

기준 문서: `ORIGINAL_DESIGN_v0.2.md`
현재 구현 기준: `0.1.0-alpha.41`

상태:
- `완료`: 원본 핵심 요구가 실제 구현됨
- `부분`: 기능은 있으나 원본 범위가 더 남음
- `미구현`: 원본 요구가 아직 게임 기능으로 없음
- `외부`: Frontier 자체 재구현보다 companion이 콘텐츠를 공급
- `후보검증`: 버전/구성은 고정했으나 풀스택 실런타임 검증 필요

이 문서는 현재 코드에 맞춰 원본 범위를 줄이는 문서가 아니다. 기능 건물 수가 15개에 도달했고 수변 전초와 위험지역 군사 전초 1차가 들어갔더라도 핵심 `부분/미구현`이 남아 있는 동안 제품을 완성이라고 부르지 않는다.

## 1. 핵심 정체성 / 멀티 / 조작

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 한 월드 하나의 공동 마을 | 완료 | SavedData 기반 공유 정착지 |
| 서버 authoritative | 완료 | 자원/건설/도로/전초/진행 서버 권위 |
| 핵심 직접 조작 소수 유지 | 완료 | B / R / Enter / Backspace 중심 |
| 플레이어별 개별 마을 금지 | 완료 | 단일 공동 정착지 |
| 세금/행복도/가족/거대 연구 UI 금지 | 완료 | 해당 미시관리 없음 |
| 외부 모드를 콘텐츠 생산 수단으로 사용 | 완료/부분 | 후보 스택 + 시장/작업장/고급 제작 연동. 풀런타임 미검증 |

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
| 위험지역 전초 충원도 실제 자원 사용 | 완료/부분 | 현지 stockpile 식량6 + 금속2. 기존 도로 운송자가 본진에서 역방향 보급. 실플레이 동선 검증 필요 |
| 건설소 자재도 실제 자원 사용 | 완료 | 동일 ledger의 목재/석재 staging |
| 고급 제작 재료도 실제 자원 사용 | 완료/부분 | 의뢰 배럴 유물1 + 전문 주민이 운반한 금속4. 외부 무기별 실전 호환 검증 필요 |
| 수변 전초 생산도 실제 아이템 | 완료/부분 | Alpha.40 실제 대구/연어 → 전초 stockpile → 기존 도로 운송. 오프라인 생산 없음 |

## 3. 건설 / 지형 공사

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 3D 완성 미리보기/회전/검사 | 완료 | ghost placement |
| 작은 높이 차 자동 정리 | 완료 | 물리 worker grading |
| 기초 처리 | 완료 | 단계 건설 |
| 중간 높이 차 `지형 공사 포함` 명시 | 미구현 | 안전 범위 밖은 주로 거부 |
| 옹벽/건물용 계단 지형 적응 | 미구현 | 일반 건물 부지 적응 부족 |
| 큰/위험 지형 거부 | 완료 | block entity/fluid/unsafe 검사 |
| 선택 영역 절토/성토 후반 보조 | 미구현 | 없음 |
| 부지→운반→기초→골조→벽→지붕→마감 | 완료 | persisted physical phases |
| 승인 순간 전체 비용 삭제 금지 | 완료 | 실제 배치 진척과 자재 소비 연결 |
| 높은/큰 건물 물리 시공 | 완료/부분 | 감시탑·병영·건설소·고급 제작소도 scaffold/haul 사용. 실동선 검증 필요 |
| 플레이어 건축물/컨테이너 보호 | 완료/부분 | 주요 공사경로 보호. 폭발/피스톤 전면 검증 남음 |
| 자동 건설 물류 지원 | 완료/부분 | 건설소 4배럴 + 물리 보급 주민. 중형 토목/복수 프로젝트는 미완 |

## 4. 주민 / 생산 / 방어

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 건설가 | 완료 | 전용 builder |
| 건설 보급 역할 | 완료/부분 | 건설소별 physical supply runner. 장기 duplicate/pathfinding 실플레이 필요 |
| 벌목꾼 | 완료 | 실제 나무 작업 |
| 농부 | 완료 | 실제 작물 작업 |
| 광부 | 완료 | 실제 유한 광석 작업 |
| 채석공 | 완료 | 노출 석재 작업 |
| 어업 주민 | 완료/부분 | Alpha.40 qualifying general outpost별 visible rod/shore 이동/실제 어획. 부두·선박·offline 생산 미완 |
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
| 언로드 저빈도 논리 시뮬레이션 | 미구현 | 현재 안전 정지, coarse 생산/물류 없음 |
| 자동 직업 배치 | 완료/부분 | 주요 역할 자동화. 서비스 전문직과 민간 job 통합 정리 여지 |

병영은 민간 population/housing과 분리되며 공짜 고티어 증원 백엔드는 제거된 상태를 유지한다. 현재 병사/전초 수비대 Iron Golem proxy는 최종 프레젠테이션이 아니다. Alpha.40 어업 주민과 Alpha.41 전초 수비대는 기존 outpost/service-unit 관례를 따라 민간 인구를 부풀리지 않는다.

## 5. 성장 단계

| 단계 | 원본 주요 해금 | 상태 |
| --- | --- | --- |
| 개척 캠프 | 창고·주택·벌목·소농장 | 완료 |
| 촌락 | 채석·광산·대장간·경비 | 완료/부분 |
| 마을 | 도로·다리·시장·첫 전초·건설 물류 | 완료/부분 — 실동선/토목 검증 남음 |
| 개척 도시 | 병영·고급 제작·여러 전초 | 완료/부분 — 병영 + Alpha.39 고급 제작소 + 여러 전초 구현. 실런타임 검증 남음 |
| 영지 | 전문 거점·후반 방어·고급 교역 | 부분 — Alpha.40 수변 어업·교역 + Alpha.41 위험지역 군사 거점 1차 포함. 항구형 표현·고급 교역·coarse unloaded breadth 미완 |

## 6. 건물/인프라 계열

원본 목표: 약 `15~20개 계열`.
현재 functional `BuildingType`: **15개** + 도로 + 전초기지.

| 원본 계열 | 상태 |
| --- | --- |
| 마을 중심 | 부분 — civic core 성장, 별도 선택 건물은 아님 |
| 창고 | 완료 |
| 주택 | 완료 |
| 건설소 | 완료/부분 — 물리 자재 staging/runner 구현. 고급 토목/다중 공사 지원은 미완 |
| 벌목소 | 완료 |
| 농장 | 완료 |
| 채석장 | 완료 |
| 광산 | 완료 |
| 대장간 | 완료/부분 |
| 작업장 | 완료/부분 — 외부 무기 정비 완료 |
| 고급 제작소 | 완료/부분 — 외부 무기 + 유물1 + 금속4 → 호환 power30 인챈트 + 완전 수리. 레시피 breadth/실전 호환 검증 남음 |
| 도로 | 완료 |
| 작은 다리 | 완료/부분 — 최대6칸 수로 3폭 석재 데크. 대형/협곡 교량 미완 |
| 전초기지 중심 | 완료 |
| 수레 정거장 | 완료/부분 — 4배럴 물리 허브, 적재16→32. 실제 wagon 없음 |
| 경비초소 | 완료 |
| 감시탑 | 완료/부분 — loaded response guard. 경보 UI/실전 검증 남음 |
| 병영 | 완료/부분 — 군사3슬롯, 실물 보급 충원/순찰. 최종 병사 외형/병과 미완 |
| 시장 | 완료/부분 — 유물→실물 에메랄드. 구매/고급 교역 미완 |

15개 숫자를 채웠다는 이유로 의미 없는 16~20번째 건물을 만들지 않는다. 다음 확장은 원본 영토/전초/교역/토목 루프를 실제로 늘릴 때만 추가한다.

### Alpha.39 고급 제작소 감사

- 개척 도시 단계 + 작업장1 + 시장1 후 해금.
- 15×11, 목재168/석재120.
- 전용 commission barrel은 일반 shared storage에 포함하지 않는다.
- 플레이어가 직접 `인챈트 없는 인식 외부 무기 + expedition relic`을 넣어야 의뢰가 성립한다.
- 공유 창고의 무기/유물을 자동으로 가져오지 않는다.
- 전문 제작 주민은 공유 저장소에서 **금속만** 실제 추출해 손에 들고 의뢰 배럴로 이동한다.
- 한 번의 forge 비용은 유물1 + 금속4.
- power30 enchanting-table 후보 중 해당 외부 무기가 지원하는 enchantment만 사용한다.
- enchant 결과가 실제 생성됐는지 검증한 뒤에만 금속과 유물을 소모한다.
- 호환 enchant가 없으면 재료 소실 없이 멈춘다.
- 성공하면 같은 외부 무기를 완전 수리하고 enchant 결과를 적용한다.
- hard companion class/item dependency, force-load, teleport inventory, 가상 crafting point는 없다.
- 현재 고급 제작 주민은 건물 귀속 service specialist라 민간 population을 올리지 않는다. 향후 citizen-job 통합 시 중복 인구를 만들면 안 된다.

## 7. 전초기지 / 영토

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 산악 광산 거점 | 완료 | mining specialization |
| 숲 벌목 거점 | 완료 | lumber specialization |
| 평야 농업 거점 | 완료 | agriculture specialization |
| 채석 거점 | 완료 | quarry specialization |
| 해안/강 어업·교역 | **완료/부분** | Alpha.40 general 전초가 loaded radius12에서 open surface water 24+와 안전한 둑을 만족하면 어업·수변교역 overlay. 실제 대구/연어 → stockpile → 기존 도로 물류. 부두/선박/수상상인 미완 |
| 위험 지역 군사 거점 | **완료/부분** | Alpha.41 general 전초가 loaded 다중 threat/environment evidence를 만족하면 군사 overlay. 전초당1 수비대 + 현지 food6/metal2 충원 + 기존 도로 운송자 역방향 보급. 사람형 병사/실전 밸런스 미완 |
| 연결 전 독립 재고 | 완료 | outpost stockpile |
| 연결 후 실제 운송 | 완료/부분 | 도로 운송 + 본진 화물 정거장 + 같은 운송자의 군사 역방향 보급. wagon 표현 없음 |
| 순간이동 금지 | 완료 | 도로 기반 이동 |
| Terralith biome 기반 전문화 | 부분 | 일반 환경 스캔 기반. companion-aware mapping 보강 필요 |

### Alpha.40 수변 전초 감사

- 기존 specialization이 `general`인 전초만 현재 수변 overlay 후보가 된다.
- 전초 중심 반경12의 이미 로드된 지역에서 open surface-water column 24개 이상 + 안전한 마른 둑이 필요하다.
- 작은 웅덩이나 언로드 해안은 생산 근거가 되지 않는다.
- 전초별 영구 tag를 가진 `전초 어업 주민 #id`가 낚싯대를 offhand에 들고 실제 둑까지 이동한다.
- 140틱 작업 cadence에서 물이 계속 유효할 때 1~3개의 실제 COD/SALMON ItemStack을 만든다.
- 어획물은 주민 main hand에 들린 뒤 기존 outpost stockpile에 물리 입고된다.
- 물고기는 food ItemStack이므로 기존 Alpha.27 `single authority for outpost transport`가 그대로 본진/수레 정거장까지 운반한다.
- 수변 시스템은 자체 도로 AI, emerald 생성, virtual trade point, teleport cargo, force-load/offline catch를 만들지 않는다.
- 현재 `수변교역`은 어획물이 기존 물리 경제에 들어간다는 뜻이다. 항구 건물/선박/수상 상인/직접 fish-for-emerald 계약은 아직 아니다.

### Alpha.41 위험지역 군사 전초 감사

- 기존 specialization이 `general`인 전초만 현재 military overlay 후보가 된다.
- 전초 주변 bounded area의 관련 청크가 이미 로드되어 있어야 위험 판정을 신뢰한다. 언로드는 사망/안전으로 오인하지 않는다.
- 위험 판정은 단일 몹 수가 아니라 `Monster` 총압박, 16블록 근접압박, hostile class 다양성, 가려진 저조도 표본을 조합한다.
- 외부 hostile이 표준 `Monster` 계층을 쓰면 hard dependency 없이 위협 근거에 포함된다.
- active military outpost에는 영구 assignment tag를 가진 `전초 수비대 #id` 1명만 유지한다.
- 현재 수비대 body는 Iron Golem proxy이며 Creeper 강제추적은 제외한다.
- 수비대가 없을 때 전초 stockpile의 실제 식량6 + 금속2를 원자적으로 확인/소비한 경우에만 충원한다.
- 전초 목표 reserve는 식량12 + 금속4다.
- 해당 combat proxy의 death drop은 비워 Iron Golem 자원 복제를 막는다.
- 위험 근거가 사라지면 기존 수비대를 삭제하지 않고 target을 해제해 전초로 복귀/대기시킨다. 안전 상태에서는 자동 재충원하지 않는다.
- 기존 Alpha.27 outpost transporter 하나가 empty return → 본진 storage 실제 추출 → main hand 운반 → persisted road → 전초 stockpile 입고 순서로 식량/금속을 역방향 보급한다.
- military 상태가 해제되면 supply/return trip tag를 정리하고 기존 outpost→town 화물 동작으로 복귀한다.
- active military role은 동일 general 전초의 fishing보다 우선한다.
- 별도 transporter, teleport cargo, fast travel, force-load, virtual supply, free troop point, offline combat은 없다.

## 8. 도로 / 토목

| 요구사항 | 상태 |
| --- | --- |
| 시작·끝·필요 시 경로 선택 | 완료/부분 — 실제 경로 UX 추가 검증 필요 |
| 실제 주민 grading/paving | 완료 |
| 건물 회피 | 완료/부분 |
| 급경사 회피 | 부분 — 2블록 이상 급단차 거부, 대형 우회/토목 미완 |
| 작은 계단 자동 포함 | 완료/부분 — 1블록 종단 경사를 실제 cobblestone stair로 처리 |
| 작은 교량 자동 포함 | 완료/부분 — 최대6칸 물길, 3폭 stone-brick deck |
| 도로가 실제 물류 의미를 가짐 | 완료 |
| 대형 교량/터널/옹벽 | 미구현 |

## 9. 외부 모드 / 탐험 루프

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| Terralith 지형 활용 | 후보검증 | lock 완료, fresh-world 풀런타임 미검증 |
| Dungeons and Taverns / Repurposed Structures | 후보검증 | 탐험 구조물 공급 예정, 풀런타임 미검증 |
| Better Combat | 후보검증 | 전투 체감 외부 공급, 풀런타임 미검증 |
| Weapons Expanded | 완료/부분 | 외부 무기 인식 + repair + Alpha.39 forge seam. 실제 전체 무기 호환 테스트 필요 |
| Lootr | 후보검증 | 던전 전리품 보조, 풀런타임 미검증 |
| Sophisticated Backpacks | 후보검증 | 원거리 탐험 편의, 풀런타임 미검증 |
| Jade | 미구현/후보검증 | 모드는 lock됐으나 Frontier 상태 provider 미구현 |
| Xaero 지도 | 미구현/후보검증 | 모드는 lock됐으나 본진/전초 marker 연동 미구현 |
| 탐험 전리품→마을 가치 | 완료/부분 | 시장 판매 / 작업장 repair / 고급 forge 구현. 보스/구조 발견 자체의 progression 연결은 약함 |

`COMPANION_LOCK.json`은 `candidate_runtime_lock`이며 실제 full client/server launch 전에는 runtime-tested로 올리지 않는다.

## 10. UI / 정보 구조

| 요구사항 | 상태 |
| --- | --- |
| 상시 compact resource HUD | 완료 |
| 월드형 건물 placement | 완료 |
| 회전/재료/가능 여부 preview | 완료 |
| road/outpost placement | 완료/부분 |
| 건물 상태/작업 진행 정보 | 부분 — HUD/가이드/status 존재, 별도 compact 상태 표현 보강 필요 |
| 수변 전초 상태 노출 | 완료/부분 — `/frontier status` loaded 어업·수변교역 수 + 최근 전초 유효 역할. HUD/Jade 표현은 미완 |
| 위험지역 군사 전초 상태 노출 | 완료/부분 — `/frontier status` active 수/로드 sentry/현지 supply + 최근 전초 유효 역할. HUD/Jade 표현은 미완 |
| 물리 자재 흐름 가시화 | 완료/부분 — 실제 운반 존재, 정보 표현은 더 개선 가능 |
| compact side notification | 미구현/부분 |
| Jade 기반 최소 상태 노출 | 미구현 |
| Xaero 본진/전초/도로망 연동 | 미구현 |

새 기능마다 새 키나 새 대형 dashboard를 만드는 방식은 금지한다.

## 11. 현재 가장 큰 남은 갭

우선순위는 실플레이 회귀가 생기면 즉시 그쪽이 우선이다. 그렇지 않으면:

1. **full companion fresh-world client/server runtime + 멀티 검증**.
2. **언로드 저빈도 production/logistics simulation** — 물리 item authority를 깨지 않는 coarse 모델 필요.
3. **Jade/Xaero 실제 Frontier integration + compact 상태/notification UX**.
4. **중간급 지형 공사** — 옹벽/명시적 terrain work/절토·성토 보조.
5. **외부 구조·보스 발견이 progression에 더 직접 연결되는 루프**.
6. **수변 전초 presentation/교역 breadth** — 부두·선박·수상 상인 등은 기존 도로 물류 권위를 깨지 않을 때만.
7. **고급 제작 breadth** — 실제 탐험 희귀재료가 충분히 생길 때만 recipe 추가.
8. **사람형 병사/외부 무기 장비 프레젠테이션** — companion 전투 stack과 함께 가치 검증 후.
9. **장시간 survival + 2인 multiplayer acceptance**.

## 12. Alpha.41 실플레이 acceptance

반드시 실제 게임에서 확인할 것:

- founding → 초기 5개 핵심 건물 진행;
- save/reload 중 building grading/hauling;
- 건설소 보급 주민 source 선택/운반/staging/builder preference;
- road stair/short bridge 실제 걷기;
- road → outpost → 생산 → transporter → cart station;
- 기존 다른 특화가 잡히지 않은 전초를 실제 강/해안 근처에 세워 `어업·수변교역` 판정이 뜨는지;
- 어업 주민이 낚싯대를 보이고 마른 둑까지 걸어간 뒤 실제 fish ItemStack을 stockpile에 넣는지;
- stockpile의 fish가 별도 물류 AI 없이 기존 transporter를 통해 본진/수레 정거장으로 이동하는지;
- 작은 웅덩이, 언로드 해안, 물이 사라진 위치에서 원격 어획이 일어나지 않는지;
- 로드/언로드 반복으로 같은 전초 어업 주민이 중복 생성되지 않는지;
- 실제 hostile pressure가 높은 loaded general 전초에서 `위험지역 군사거점`이 뜨고 단순 1마리/불완전 로딩에는 활성화되지 않는지;
- external hostile Monster도 위협 판정/수비대 target 후보로 자연스럽게 들어가는지;
- 전초 수비대가 1명만 유지되고 Creeper를 강제 추적하지 않으며 사망 시 철/자원 drop이 생기지 않는지;
- 본진 창고에서 기존 운송 주민이 식량/금속을 실제 손에 들고 도로를 따라 전초 stockpile까지 역방향 보급하는지;
- 수비대 사망 후 현지 food6 + metal2가 없으면 무료 재충원되지 않고, 보급 후에만 재충원되는지;
- 위험을 정리하면 수비대가 전초로 stand-down하고 군사 보급 trip 상태가 정리되며 normal/fishing 역할이 다시 가능한지;
- 위험지역 전초/운송 경로를 언로드했다가 돌아왔을 때 수비대나 운송 주민이 복제되지 않는지;
- dungeon/loot에서 얻은 relic을 **시장 판매 vs 고급 제작 재료**로 실제 선택 가능한지;
- normal workshop repair와 advanced workshop forge가 서로 의뢰 배럴을 침범하지 않는지;
- 외부 무기별 power30 enchant 호환, 호환 불가 시 재료 무손실;
- watchtower/barracks 전투와 병력 교체 비용;
- 2인 shared storage/construction/logistics 정합;
- full companion lock fresh-world launch.

자동 감사/빌드/JAR 검증은 소스 정합을 보장하지만 실제 Minecraft 동선·밸런스·비주얼·companion runtime 품질을 대신하지 않는다.
