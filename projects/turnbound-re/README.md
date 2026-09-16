# TURNBOUND: RE

Minecraft Java에서 동작하는 파티 기반 턴제 RPG 프로젝트의 새 정본이다.

> **상태: FIRST INTEGRATED PLAYTEST / M5-M6 AUTOMATED GATES PASS / PRISM DISTRIBUTION WIRED / EXTERNAL-WORLD MIGRATION PENDING**

`projects/turnbound/`의 구 TURNBOUND는 폐기 프로젝트이며 TURNBOUND: RE의 설계 근거로 사용하지 않는다. 이름이 비슷하더라도 자동 승계되는 규칙은 없다.

## 읽는 순서
1. `AGENTS.md`
2. `AGENT_RULES.md`
3. `docs/CANON.md`
4. 작업 분야에 해당하는 세부 문서
5. 저장소 공용 `../../docs/BUILD_STANDARD.md`, `../../docs/QUALITY_STANDARD.md`

## 정본 문서
- `PROJECT.md` — 목표·범위·개발 준비 상태
- `docs/CANON.md` — 변경 시 명시적 합의가 필요한 핵심 결정
- `docs/00_PRODUCTION_PRINCIPLES.md` — 제작 원칙·완료 기준
- `docs/01_GAME_LOOP_WORLD.md` — 월드/탐험/생활/조우
- `docs/02_COMBAT_SYSTEM.md` — 전투 규칙·공식·상태기계
- `docs/03_PROGRESSION_ECONOMY.md` — 별/레벨/승급/재화
- `docs/04_CHARACTER_SYSTEM.md` — 캐릭터 규격·역할·스킬 제작 규칙
- `docs/05_VANILLA_ROSTER_CATALOG.md` — 바닐라 엔티티 전수 등록 규칙
- `docs/06_UI_UX_PRESENTATION.md` — UI/연출 요구사항과 시각 디자인 게이트
- `docs/07_TECHNICAL_ARCHITECTURE.md` — 서버 권한·네트워크·저장·코드 구조
- `docs/08_REFERENCE_CATALOG.md` — 턴제 게임/오픈소스 참고 근거
- `docs/09_WORLD_ASSET_GATE.md` — 맵·건축·외부 자산 도입 절차
- `docs/10_VERTICAL_SLICE_IMPLEMENTATION.md` — 첫 실게임 구현 범위
- `docs/11_CONTENT_PIPELINE_TEST_PLAN.md` — 데이터/CI/게임테스트
- `docs/12_BALANCE_PLAYTEST_PROTOCOL.md` — 밸런스 측정 방법
- `docs/13_DATA_SCHEMA.md` — JSON/Codec 계약
- `docs/14_IMPLEMENTATION_BACKLOG.md` — 개발 순서·완료 조건
- `docs/15_READINESS_AUDIT.md` — 지금 바로 개발 가능한 범위와 아직 콘텐츠 완성 전인 범위
- `docs/30_EXTERNAL_WORLD_BASE.md` — production 외부 월드 정본
- `docs/34_DREHMAL_26_2_MIGRATION_AUDIT.md` — APOTHEOSIS 26.2 migration/playtest 절차
- `packaging/README.md` — TURNBOUND Prism 배포/외부 월드 bootstrap 계약

## 핵심 한 줄
Minecraft의 탐험·채집·제작을 버리지 않으면서, 보이는 조우를 통해 4인 파티가 적의 의도를 읽고 약점/Poise를 공략해 `EXPOSED` 창을 만드는 빠른 턴제 전투를 반복한다.

## 권장 설치 — TURNBOUND Prism 패키지

**평지나 새 바닐라 월드를 만들지 않는다.** TURNBOUND: RE의 production 월드는 외부 authored world인 **Drehmal: APOTHEOSIS v2.2.2f**다.

이제 플레이테스트의 권장 전달물은 단독 JAR이 아니라 CI가 생성하는 **`TURNBOUND_RE_Prism.zip`**이다.

1. Prism Launcher에서 `Add Instance → Import`로 `TURNBOUND_RE_Prism.zip`을 그대로 가져온다.
2. 인스턴스를 실행한다.
3. 첫 실행의 pre-launch bootstrap이 Drehmal Team 공식 GitHub release에서 pinned v2.2.2f map shard 3개와 resource pack을 직접 내려받는다.
4. 세 shard를 전용 save로 합쳐 풀고, 공식 manifest의 directory SHA-256과 일치할 때만 `.turnbound_re_profile` marker를 기록한다.
5. resource pack을 인스턴스에 설치/활성화하고 GUI scale을 읽기 쉬운 범위로 맞춘다.
6. Minecraft가 시작되면 `TURNBOUND RE - Drehmal APOTHEOSIS 2.2.2f` 월드를 연다.
7. 검증된 pack marker가 있는 월드는 첫 player login에서 TURNBOUND anchor를 자동 바인딩하고 Hub로 이동한다. 수동 `/bind_drehmal` 단계는 필요 없다.

외부 월드와 resource pack 원본은 TURNBOUND 저장소나 Prism ZIP에 vendoring하지 않는다. 배포 허가를 추측하지 않고, 사용자의 PC가 공식 release에서 직접 받는다. 공식 manifest 기준 map은 약 4 GB compressed / 5.1 GB uncompressed이므로 첫 설치에는 최소 12 GiB 여유 공간을 권장한다.

현재 bootstrap은 실제 플레이테스트 환경에 맞춰 **Windows-first**다. Java 25는 Prism에서 자동 다운로드/선택할 수 있다.

APOTHEOSIS 공개 본편은 1.20.1-era 월드이므로 **26.2 migration은 아직 PLAYTESTED로 확정되지 않았다.** 패키징과 자동 바인딩이 성공해도 실제 월드 migration 결과까지 성공했다고 간주하지 않는다.

### 수동 외부 월드 fallback
Prism bootstrap이 아닌 수동 설치를 사용할 때만 기존 안전 절차를 유지한다.

1. 공식 Drehmal v2.2.2f 월드 복사본을 Java 26.2에서 연다.
2. New Drabyel `502 67 1801`, Stasis Facility `778 31 668`의 landmark/chunk가 보존됐는지 확인한다.
3. `/turnbound_re_world_slice validate`
4. New Drabyel configured seed 192 blocks 이내에서 `/turnbound_re_world_slice bind_drehmal`

수동 경로는 임의 save 오바인딩을 막기 위해 의도적으로 operator confirmation을 유지한다.

## 현재 구현 경계
M0~M4 전투 코어·네트워크·조우·대표 캐릭터·성장/보상 기반은 구현 정본을 유지한다. M5/M6의 주요 자동 계약과 JAR 검증은 통과했지만, 실제 Minecraft 화면과 외부 월드에서의 첫 통합 실플레이가 현재 최우선 gate다.

현재 player-facing 비전투 흐름에는 다음이 연결되어 있다.
- `M` — TURNBOUND 관리 허브
- 원정 — Expedition Journal / Encounter 정보
- 파티 — 4인 편성 / squad cost
- 캐릭터 상세 — 개요 / 스킬
- 성장 — level up / ascension / Coin·Essence·Shard
- 장비 — 제작 / 강화 / 장착, 실제 Minecraft 재료 및 Smithing Table 근접 gate
- 월드 — fast travel discovery, 생활 자원, authored Encounter는 실제 월드 interaction을 통해 진입

전투 중에는 서버 snapshot 기반 action/target legality, turn order, Enemy Intent, HP/Energy/Poise, EXPOSED, affinity 정보가 production HUD/command flow에 연결되어 있다.

단, 자동 게이트 통과는 **코드/계약 검증**이지 실화면 품질 통과가 아니다. 실제 클라이언트에서 GUI scale, clipping, 입력, 3D 모델 framing, VFX/audio timing, action identity, reward readability를 검증하고 수정해야 한다.

전체 완제품의 캐릭터/지역/아이템/드롭표까지 모두 채워진 `CONTENT COMPLETE` 상태는 아니며, M7 전체 바닐라 Mob roster 확장은 첫 통합 사이클 안정화 이후 진행한다.
