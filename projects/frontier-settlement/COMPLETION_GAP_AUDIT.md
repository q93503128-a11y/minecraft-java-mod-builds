# Frontier Settlement — v0.2 완성도 갭 감사

기준 문서: `ORIGINAL_DESIGN_v0.2.md`
현재 구현 기준: `0.1.0-alpha.38`

상태:
- `완료`: 원본 핵심 요구가 실제 구현됨
- `부분`: 기능은 있으나 원본 범위가 더 남음
- `미구현`: 원본 요구가 아직 게임 기능으로 없음
- `외부`: Frontier 자체 재구현보다 companion이 콘텐츠를 공급
- `후보검증`: 버전/구성은 고정했으나 풀스택 실런타임 검증 필요

이 문서는 현재 코드에 맞춰 원본 범위를 줄이는 문서가 아니다. 핵심 `부분/미구현`이 남아 있는 동안 제품을 완성이라고 부르지 않는다.

## 1. 핵심 정체성 / 멀티 / 조작

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 한 월드 하나의 공동 마을 | 완료 | SavedData 기반 공유 정착지 |
| 서버 authoritative | 완료 | 자원/건설/도로/전초/진행 서버 권위 |
| 핵심 직접 조작 소수 유지 | 완료 | B / R / Enter / Backspace 중심 |
| 플레이어별 개별 마을 금지 | 완료 | 단일 공동 정착지 |
| 세금/행복도/가족/거대 연구 UI 금지 | 완료 | 해당 미시관리 없음 |
| 외부 모드를 콘텐츠 생산 수단으로 사용 | 완료/부분 | 후보 스택/라이선스 레지스터/시장·작업장 연동. 풀런타임 미검증 |

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
| 건설소 자재도 실제 자원 사용 | 완료 | 동일 ledger의 목재/석재 ItemStack staging |

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
| 높은/큰 건물 물리 시공 | 완료/부분 | 감시탑·병영·건설소가 기존 scaffold/haul 사용. 실동선 검증 필요 |
| 플레이어 건축물/컨테이너 보호 | 완료/부분 | 주요 공사경로 보호. 폭발/피스톤 전면 검증 남음 |
| 자동 건설 물류 지원 | **완료/부분** | Alpha.38 건설소 4배럴 + 물리 보급 주민. 중형 토목/복수 프로젝트는 미완 |

### Alpha.38 건설소 감사

- 마을 단계 + 창고 1곳 후 해금.
- 13×9, 목재112/석재64.
- 회전 대응 전용 자재 배럴 4개가 물리 settlement storage에 포함된다.
- 목재/석재는 건설소 자재칸을 먼저 보지만 식량/금속/무관한 전리품 자동 입고는 건설소를 건너뛴다.
- 보급 주민은 건물 프로젝트가 있을 때만 움직이며 ordinary storage에서 실제 목재/석재를 꺼내 손에 든다.
- 1회 최대32, 목표 reserve 목재96/석재96, source radius24.
- source→office 구간의 sampled chunk가 이미 로드되어야 한다.
- 원거리 `insert()` 순간이동이 아니라 도착 후 `insertAt()` 한다.
- 보급 주민은 서비스 유닛으로 민간 population/housing을 늘리지 않는다.
- 기존 builder가 office-first storage ordering을 이용하며 grading/placement authority는 그대로 `SettlementConstructionService` 하나다.
- 추상 `+건설속도%`, force-load, 두 번째 builder, 가상 construction point는 없다.
- 실제 이동 이득/병목은 실플레이에서 확인해야 한다.

## 4. 주민 / 생산 / 방어

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 건설가 | 완료 | 전용 builder |
| 건설 보급 역할 | 완료/부분 | Alpha.38 건설소별 physical supply runner. 장기 duplicate/pathfinding 실플레이 필요 |
| 벌목꾼 | 완료 | 실제 나무 작업 |
| 농부 | 완료 | 실제 작물 작업 |
| 광부 | 완료 | 실제 유한 광석 작업 |
| 채석공 | 완료 | 노출 석재 작업 |
| 대장장이 | 부분 | 수리 기능은 있으나 주민/제작 연출 약함 |
| 작업장 전문 제작자 | 부분 | 외부 무기 물리 정비 장인. 고급 제작 미완 |
| 근거리 경비 | 완료/부분 | 경비초소 + 기본 경비 |
| 감시/장거리 대응 | 완료/부분 | 감시탑별 response golem, 40블록 loaded 대응. 경보/실전 검증 남음 |
| 정식 주둔 병력 | 완료/부분 | 병영별 3슬롯, 식량/금속 충원, 영구 귀속, loaded patrol. 사람형 병사/병과 미완 |
| 운송업자 | 완료 | 전초별 영구 태그 + 도로 물류 |
| 상인 | 부분 | 시장 방문 상인/물리 판매, 구매·고급 교역 미완 |
| 로드 지역 실제 이동·작업 | 완료 | 생산/운송/시장/작업장/건설소/방어 |
| 언로드 저빈도 논리 시뮬레이션 | 미구현 | 현재 안전 정지, coarse 생산/물류 없음 |
| 자동 직업 배치 | 완료/부분 | 주요 역할 자동화. 본진 동일 역할 장기 귀속 추가 검증 필요 |

병영은 민간 population/housing과 분리되며 공짜 고티어 증원 백엔드는 제거된 상태를 유지한다. 현재 병사 Iron Golem proxy는 최종 프레젠테이션이 아니다.

## 5. 성장 단계

| 단계 | 원본 주요 해금 | 상태 |
| --- | --- | --- |
| 개척 캠프 | 창고·주택·벌목·소농장 | 완료 |
| 촌락 | 채석·광산·대장간·경비 | 완료/부분 |
| 마을 | 도로·다리·시장·첫 전초·건설 물류 | **완료/부분** — 도로/시장/전초/정거장/건설소 + 작은 계단·교량 구현. 실동선 검증 남음 |
| 개척 도시 | 병영·고급 제작·여러 전초 | 완료/부분 — 병영/여러 전초 구현, 고급 제작 미구현 |
| 영지 | 전문 거점·후반 방어·고급 교역 | 부분 — 도로망/공공사업/감시망/주둔 병력 일부. 전문 거점·고급 교역 미완 |

## 6. 건물/인프라 계열

원본 목표: 약 `15~20개 계열`.
현재 functional `BuildingType`: **14개** + 도로 + 전초기지.

| 원본 계열 | 상태 |
| --- | --- |
| 마을 중심 | 부분 — civic core 성장, 별도 선택 건물은 아님 |
| 창고 | 완료 |
| 주택 | 완료 |
| **건설소** | **완료/부분** — Alpha.38 4자재 배럴 + 물리 보급 주민. 고급 토목/다중 공사 지원은 미완 |
| 벌목소 | 완료 |
| 농장 | 완료 |
| 채석장 | 완료 |
| 광산 | 완료 |
| 대장간 | 완료/부분 |
| 작업장 | 완료/부분 — 외부 무기 정비 완료, 일반/특수 제작 확장 여지 |
| 도로 | 완료 |
| 작은 다리 | 완료/부분 — 최대6칸 수로 3폭 석재 데크. 대형/협곡 교량 미완 |
| 전초기지 중심 | 완료 |
| 수레 정거장 | 완료/부분 — 4배럴 물리 허브, 적재16→32. 실제 wagon 없음 |
| 경비초소 | 완료 |
| 감시탑 | 완료/부분 — loaded response guard. 경보 UI/실전 검증 남음 |
| 병영 | 완료/부분 — 군사3슬롯, 실물 보급 충원/순찰. 최종 병사 외형/병과 미완 |
| 시장 | 완료/부분 — 유물→실물 에메랄드, 구매/고급 교역 미완 |
| **고급 제작소** | **미구현** |

단순히 15개 숫자를 채우기 위해 의미 없는 건물을 추가하지 않는다. 다음 건물은 고급 제작/외부 희귀재료 루프를 실제로 확장해야 한다.

## 7. 전초기지 / 영토

| 요구사항 | 상태 | 현재 |
| --- | --- | --- |
| 산악 광산 거점 | 완료 | mining specialization |
| 숲 벌목 거점 | 완료 | lumber specialization |
| 평야 농업 거점 | 완료 | agriculture specialization |
| 채석 거점 | 완료 | quarry specialization |
| 해안/강 어업·교역 | 미구현 | 수변 전문화 없음 |
| 위험 지역 군사 거점 | 미구현 | 병영은 본진 계층이며 전초 군사 전문화는 별도 |
| 연결 전 독립 재고 | 완료 | outpost stockpile |
| 연결 후 실제 운송 | 완료/부분 | 도로 운송 + 본진 화물 정거장. wagon 표현 없음 |
| 순간이동 금지 | 완료 | 도로 기반 이동 |
| Terralith biome 기반 전문화 | 부분 | 일반 환경 스캔 기반, companion-aware mapping 보강 필요 |

## 8. 도로 / 토목

| 요구사항 | 상태 |
| --- | --- |
| 시작·끝·필요 시 경로 선택 | 완료/부분 — 실제 경로 UX 추가 검증 필요 |
| 실제 주민 grading/paving | 완료 |
| 건물 회피 | 완료/부분 |
| 급경사 회피 | 부분 — 2블록 이상 급단차 거부, 대형 토목 우회 미완 |
| 작은 계단 자동 포함 | 완료/부분 — 1블록 종단 경사를 cobblestone stair 처리 |
| 작은 교량 자동 포함 | 완료/부분 — 최대6칸 물길, 3폭 stone-brick deck |
| 도로가 실제 물류 의미를 가짐 | 완료 — 전초 운송 + 정거장 위치/효율에 영향 |
| 대형 교량/터널/옹벽 | 미구현 | 의도적 후반 토목 범위 |

## 9. Alpha.31~38 성장 연결

- **Alpha.31**: 외부 재료/relic/Weapons Expanded 인식, soft integration.
- **Alpha.32**: 전용 시장 배럴의 relic만 실물 emerald로 교역.
- **Alpha.33**: 작업장 장인이 실제 금속을 운반해 외부 무기 정비.
- **Alpha.34**: 수레 정거장 4화물 배럴, 운송16→32, 기존 transport authority 유지.
- **Alpha.35**: 자동 cobblestone stair + bounded stone-brick small bridge, 실제 석재 비용.
- **Alpha.36**: 감시탑 physical tall build + tower-assigned loaded response guard.
- **Alpha.37**: 병영 3군사 슬롯 + 실물 식량/금속 충원, 공짜 고티어 증원 제거.
- **Alpha.38**: 건설소 4자재 배럴 + physical supply runner, 기존 builder authority 유지.

## 10. 전체 companion 런타임

`COMPANION_LOCK.json`에는 Terralith/Lithostitched, DnT, Repurposed Structures, Better Combat 및 의존 라이브러리, Weapons Expanded, Lootr, Sophisticated, Jade, Xaero를 고정했다.

상태: **후보검증** — 버전/의존성과 Frontier CI는 확인하지만 전부 함께 같은 26.2 인스턴스에서 실제 부팅/새 월드/멀티를 아직 끝내지 않았다.

| 외부 축 | Frontier 연결 상태 |
| --- | --- |
| Terralith | 부분 — 바이옴 기반 전초 판정 보강 미완 |
| Dungeons and Taverns | 부분 — relic 시장 소비 가능, 구조물 발견 progression 미연결 |
| Repurposed Structures | 부분 — 탐험 콘텐츠 공급, 발견 progression 미연결 |
| Better Combat | 외부/후보검증 — 풀스택 회귀 필요 |
| Weapons Expanded | 부분 강화 — 외부 무기 인식 + 작업장 정비 |
| Lootr | 외부/후보검증 — 멀티 던전 보상 확인 필요 |
| Sophisticated Backpacks | 외부/후보검증 |
| Jade | 미구현/후보검증 — Frontier provider 미작성 |
| Xaero's Minimap | 미구현/후보검증 — Frontier waypoint/territory 연동 미작성 |

## 11. UI / 정보

| 요구사항 | 상태 |
| --- | --- |
| compact resource HUD | 완료 |
| 건물 3D ghost/회전 | 완료 |
| 단일 B 팔레트 | 완료 |
| 건설 자재/진척 물리 가시성 | 부분 — 현장 crate/scaffold + `/frontier status` 건설소 staging. 더 좋은 world/UI 표현 여지 |
| 건물별 상태 패널 | 미구현/부분 |
| compact notifications | 미구현 |
| Jade provider | 미구현 |
| Xaero integration | 미구현 |

## 12. 아직 큰 미완성

- 고급 제작소 / 외부 희귀 재료 제작;
- 강/해안 어업·교역 전초;
- 위험 지역 군사 전초;
- 언로드 저빈도 논리 시뮬레이션;
- Jade provider / Xaero 연동;
- 외부 구조물 발견/보스 결과의 progression 연결;
- 중형 건물 지형공사/옹벽/대형 토목;
- 사람형 병사/장비/병과 프레젠테이션;
- 건설/재료/알림 UI 보강;
- 전체 companion stack 실제 client/server 부팅 및 멀티 회귀;
- 최종 장시간 생존/밸런스/동선 검사.

## 13. 다음 작업 우선순위

1. Alpha.38 최종 source audit / Java25 clean build / JAR verify를 최종 source/docs SHA로 고정.
2. 고급 제작소를 추가하고 외부 희귀 재료·탐험 결과를 후반 제작에 실제 연결.
3. 강/해안 교역·어업 및 위험 지역 군사 전초 전문화.
4. full `COMPANION_LOCK` fresh-world runtime test.
5. 언로드 coarse simulation을 물리 아이템 권위를 깨지 않는 방식으로 설계.
6. Jade/Xaero 및 compact 상태/알림 UI 남은 축을 닫기.
7. 전체 실플레이 acceptance에서 건설소 보급 동선, 기존 건설 pacing, 도로/물류/방어 회귀를 검증.

실플레이 피드백이 생기면 이 우선순위보다 실제 회귀/근본 원인 수정이 우선한다.
