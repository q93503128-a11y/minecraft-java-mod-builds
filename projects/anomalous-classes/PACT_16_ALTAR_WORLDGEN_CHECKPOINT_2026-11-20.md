# 계약술사 — 16종 제단 월드젠 통합 체크포인트 (2026-11-20)

## 정본과 의도
- 모드: 《이능 직업 서바이벌》, Minecraft Java 1.21.1 / NeoForge 21.1.244 / Java 21.
- 정본: 최신 `main` 및 `PROJECT.md`, `PLANNING_DECISION_REGISTER_V1.md`, `classes/PACT_ALTARS_EXPLORATION.md`, `PACT_ALTAR_WORLDGEN_FREQUENCY_V2.md`, 이전 위계 구현 체크포인트.
- 이번 작업의 목표는 **1위계 4명의 제단도 자연 생성하고, 기존 2·3·4위계와 하나의 공유 월드젠 후보 격자에 통합**하는 것이다. 1위계 입문 계약은 여전히 제단이 없어도 가능해야 한다.
- 기능성 유적은 Codex의 최종 건축/아트/VFX를 대신하지 않는다. 게임 로직과 시각 검수는 구분한다.

## 이번 구현
1. `PactAltarPlacement`는 한 육상 후보를 40:30:20:10으로 선택한다(조건부 육상 후보의 **시험 배분**, 실제 생성 성공률·바이옴·세계 전체 비율 보장 아님). 1위계 4종을 각 균등하게 후보로 추가했고 기존 2위계 육상 3종, 3위계 4종, 4위계 4종의 후보도 유지한다. 해양 후보는 네르아 단독.
2. `PactRuinFeature`에서 1위계 4종의 소규모 지상 제단을 서로 다른 기둥·바닥·표식으로 생성:
   - 이사엘: 깨진 서광 아치, 밝은 쿼츠와 부드러운 사암.
   - 벨칸: 갈라진 흑석 탁자, 부서진 교섭실의 짧은 기둥.
   - 우르겐: 외딴 거석의 주먹 문양, 터프 벽돌.
   - 리드나: 두 높이가 다른 별 관측대, 자수정과 별 표식.
   핵과 상자 접근은 기존 공통 구조 재사용. 소규모 기능형 건축으로 실제 크기와 장식 밀도는 정식 디자인/실게임 평가 전이다.
3. 새 1위계 loot table 4개. 전 직업 모두 계약 없이 직접 상자를 열 수 있다. 희귀 종결 계약 재료를 거저 주지 않는다.
4. `PactService.previewAtAltar`가 1위계의 제단 계약서 조회도 허용한다. 기존 `/pact contact <patron>` 입문 원격 계약, 명시적 `/pact confirm` 승인, 서버 제물 원자 소비, 3·4위계 시련 증명은 그대로다.
5. 비해양 15종은 후보 건축 반경에서 액체 블록을 검사해 물에 잠긴 제단을 피한다. 월드젠 후보는 기존 단일 40청크 격자이며 청크 생성 후 플레이어 기지에 구조물을 재적용하지 않는다. 중심핵의 기존 0~15 blockstate ID는 변경하지 않았다.

## 자동 검증 범위
- `tools/PactSixteenAltarVerifier.java` 및 Gradle `verifyPactSixteenAltars`:
  - 16종 정체성·위계/접근 정책·저장된 blockstate 인덱스 불변 확인.
  - 16개의 월드 공용 loot table 파일·chest 유형·보상 항목 존재 확인.
  - 61×61 공유 격자 산술로 후보 청크 경계·중복 후보 없음·육상 15종 도달 가능성 검증.
  - 해양 후보 네르아 전용 및 육상 40:30:20:10의 통계적 오차 범위 확인.
- 기존 2·3·4위계 회귀 검증기의 구형 생성 비중 가정을 새 배분에 맞게 교정. 기존 기능 축소·기존 테스트 삭제 없음.
- **자동 검증은 실제 마인크래프트 세계에 제단이 생성되는 모습·구조물 충돌·월드 시드의 발견 빈도·멀티 전리품 개봉을 검사하지 않는다.** 4k×4k/8k×8k 실제 시드 비교, 대형 건축, 수몰/입구/상자 접근성은 이후 통합 플레이테스트에서 확인해야 한다.

## 빠른 재현
테스트 월드에서 계약술사 직업을 선택한 뒤 다음처럼 1위계 핵을 배치할 수 있다. 이것은 **제단 계약 기능 검사**이며 구조물 자연 생성 확인은 아니다.

`/setblock ~2 ~ ~ anomalousclasses:pact_altar_core[patron=0]` (이사엘; 1 벨칸, 2 우르겐, 3 리드나)
`/give @s minecraft:gold_ingot 2`
핵을 우클릭해 계약서·제물 확인 → `/pact confirm`. 별도로 제단 없이 `/pact contact isael`도 검증한다. 임의 시점/좌표에 자연 생성된 유적 탐색은 게임에서 새 청크로 이동하여 검사한다. 본 모드는 별도 구조물 레지스트리형이 아니라 바이옴 feature 방식이므로 바닐라 `/locate structure`로 직접 찾을 수 있다는 주장 금지.

## 검증 상태 (빌드 판정은 후속 기록)
**최신 검증 — 2026-11-20**
- 검증 대상 코드 HEAD: `4b16f7ce91053bd73b69b686e47667e6d7e3c689`.
- [GitHub Actions #44639840274](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/44639840274) **SUCCESS**. Java 21 clean build 및 기존 체크, 신규 `verifyPactSixteenAltars`, `verifyPactTierTwo`, `verifyPactTierThree`, `verifyPactTierFour`, JAR 구조 검증, SHA-256, 아티팩트 업로드 모두 통과.
- 검증 로그: `Sixteen shrines, shared loot, ocean-only Nerea, stable block IDs, one placement grid, dry distribution: PASS 1512/1090/732/387`. 61×61 **가상 육상 후보**의 각 위계 카운트이며 실제 월드 성공 배치/바다 분포가 아니다.
- JAR: `build/libs/anomalous-classes-0.1.0-alpha.1.jar` (32,232,736바이트, 236 classes). [JAR + SHA-256 아티팩트](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/44639840274/artifacts/14586785197), [로그 아티팩트](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/44639840274/artifacts/14586345973).
- 성공한 JAR과 코드 HEAD는 위 SHA에 해당한다. 이후 문서 기록은 `[skip ci]` 처리하며 검증된 코드에 영향이 없다.

**검증 상태: CODE REVIEWED / BUILD VERIFIED / JAR PRODUCED. Minecraft CLIENT / REAL WORLD SEED / PLAYTEST / MULTIPLAYER: NOT TESTED.**
