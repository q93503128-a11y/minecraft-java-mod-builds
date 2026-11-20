# 계약술사 3위계 4종 심화·성취 시스템 — 2026-11-20

## 작업 정본 및 범위
- Repository: q93503128-a11y/minecraft-java-mod-builds main, project projects/anomalous-classes/, Minecraft Java 1.21.1 / NeoForge / Java 21.
- 기준: AGENTS.md, docs/BUILD_STANDARD.md, docs/QUALITY_STANDARD.md, PROJECT.md, PLANNING_DECISION_REGISTER_V1.md, classes/PACT_TIER_3_4_DESIGN.md, classes/PACT_DEEPENING_AND_COVENANTS.md, classes/PACT_16_COMBAT_DESIGN_BASELINE_V3.md, 1·2위계 심화 체크포인트.
- 이미 구현한 16종 계약, 1·2위계 심화, 16제단 생성, 금기·채무, 3위계 최초 수호자 시련·증명·제물 2단계 승인은 삭제·변경하지 않는다.
- 세부 수치와 일반 소재 봉납, 한 위계당 2회 심화는 **플레이테스트 전 조정 가능한 시험값**. 4위계에서 사용자가 확정한 “최초 계약에만 초희귀 제물, 심화는 강적·성취 중심, 네더의 별/메아리 조각 반복 강제 금지” 지침을 침범하지 않는다. 고위계의 특별 현현은 심화의 자동 보상에 포함하지 않는다.

## 3위계 고유 성취 → 1·2차 심화
- **칠익의 재판관 / 공정성:** 실제 적대자를 선고 표식으로 표시한 뒤 `최후의 판결`을 표식 유효 시간 안에 적중. 1차에서 판결 사거리 7→8블록 및 피해 후 보호 지속시간, 2차에서 판결 사거리 9블록·표식 판결 성공 후 짧은 보호와 한정 다중 표적 후속타 증가. 표식 없는 적에게도 기본 판결 사용 가능.
- **메르자드 / 야망:** 무기 또는 왕의 화염으로 실제 적대자에게 화염 표식을 누적하고, 표식 지속 중 `왕의 화염`을 적중시켜 실제 연쇄를 발생시킨 사건. 1차에서 화염 표식 유지·연쇄 피해 증가, 2차에서 연쇄 폭발 최대 대상 3→4 및 화염 피해 추가 강화. 기본 화염 파동은 사전 표식 없이도 정상 사용. 월드 블록 지속 방화 추가하지 않음.
- **타론 / 위엄:** `왕의 석갑`이 실제 활성화된 상태에서 `산맥 분쇄`를 적대자에게 명중시킨 연계. 1차에서 석갑 내구·유지시간, 분쇄 밀침 개선. 2차에서 실제 적대자가 석갑 내구를 완전히 부순 뒤 한시적으로 산맥 분쇄가 추가 피해를 내는 반격. 단순 시간 만료·자기 피해·허수아비 타격은 새로운 반격을 만들지 않음.
- **아르켄 / 이해도:** 실제 적대자의 유효 공격으로 지원 프로필(melee/projectile/fire/impact)을 관측한 짧은 구간 내 `기록 재현`을 유효 적대자에게 적중. 1차에서 기록별 재현 피해·일시 저항 개선, 2차에서 실제 관측 유형이 달라진 뒤 기록 재현 시 제한된 추가타. 외부 모드의 임의 기술 코드를 복사하거나 사용 불가능한 유형을 성공했다고 주장하지 않음.
- **증명 횟수 시험값:** 각 계약의 1차 3회, 2차 5회. 다른 시련의 진행도와 독립. 동일 플레이어/계약은 8초, 동일 적대 개체는 60초 간격 제한이며 실제 유효 적대 공격/권능 적중만 서버에서 기록. 계약 서명을 해제하면 획득 심화는 사라지고, 다시 체결해도 소비한 제물이 돌아오지 않는다.
- **일반 재료 시험값:** `src/main/resources/data/anomalousclasses/pact_deepening_tier3_v1.json`에 네 계약별 2세트의 다른 소재. 심화는 재료만 소비하여 공짜 승급하는 방식이 아니며, 심화 비용에 최초 계약의 1/2/4/8배를 재적용하지 않는다. 메아리 조각·네더의 별 같은 초희귀 입문 소재 반복 강제는 없다.
- **효과 배율:** 해당 3위계 계약이 제공하는 가호의 “증가분”과 주력 공격에 0→1→2단계 `1.00 / 1.08 / 1.18`. 기존 1위계 `1 / 1.12 / 1.25`, 2위계 `1 / 1.10 / 1.22` 및 동종 스탯 최고100%+그 외 각50% 소프트캡 유지. 플레이어 기본 HP/기본 무기 공격력에 무작정 직접 곱하지 않음. 4위계 별도 보정은 미구현.
- **계약 UI 흐름:** `/pact deepen <id>`에서 목표·개별 진행·정확한 재료·다음 단계 실제 강화 내용을 한국어/영어로 선열람 후 조건 충족 시 `/pact confirm`으로 최종 동의. 서버가 소유 계약·심화 단계·게임 내 전투상태·revision·실제 인벤토리·제물 수량을 재확인 후 1회 소비한다. 3위계 *최초 계약* 때 필요했던 수호자 시련 증명은 보존되며, 심화마다 최초 수호자 시련을 재요구하지 않음.

## 검증 전략과 경계
- 신규 `tools/PactTierThreeDeepeningVerifier.java` / Gradle `verifyPactTierThreeDeepening`. 1~3위계 12개 가동 심화, 4종 별개 행동 키, 단계별 제물/금지 초희귀 반복 제물, 3위계 가호 배율, 한글·영어 목표/보상/관계 키 완비를 검사한다.
- NBT: 서명 없이 증명 불가, 기준 횟수 위조·단계 건너뛰기·중복 심화 거부, 다른 계약 3종과 관계/진행도 분리, 1차/2차 저장 및 재접속 라운드트립, 영구 수호자 시련 증명의 독립 보존, 해약 후 재체결 때 심화 초기화.
- 기존 `verifyPactTierOneDeepening`, `verifyPactTierTwoDeepening`에서 총 등록 의식 8→12만 변경하고 4위계 미승인 심화 비활성 검증을 유지. 기존 16종 전체 계약·16제단/모드 기능 회귀 검사는 그대로 둔다.
- 실전 검증 명령: `/pact` → 3위계 해당 제단에 접근해 수호자 시련 증명 및 최초 계약 체결 → 각 권능 조합으로 서버 증명을 채우기 → `/pact deepen sevenwing`(또는 `merzad`/`taron`/`arken`) → 필요한 재료 준비 → `/pact confirm` → 계약서 심화 단계/가호/기술 변화 확인.
- **실제 Minecraft 클라이언트 피격/애니메이션/콤보 유효 시간·보스/멀티·발동 VFX/사운드/UI 이미지 검사, 패시브 270랭크의 전수 기능 검수는 아직 하지 않았다.** 컴파일·Gradle·JAR 검증 결과는 이번 checkpoint 아래 CI 결과로만 확정한다.

## 실제 통합 CI 및 JAR 결과
- **검증 대상 코드 SHA:** `00960d0cfa55f8346b5bf449ea6aeeddd3182071`.
- [GitHub Actions #44689561180](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/44689561180) **SUCCESS**. Java 21 clean build + Gradle check + 기존 1·2위계 심화·3/4위계 계약자·16제단·금기/채무·계약/기술 데이터 회귀 검사, 신규 `verifyPactTierThreeDeepening` 모두 PASS. JAR ZIP 구조·class 수·SHA-256 생성·artifact 업로드 통과.
- **신규 검사 실제 출력:** `Four tier-three earned rites, no repeated rare relics, separate guardian proof, NBT upgrades, multi-bond isolation and no replay: PASS`.
- **기존 회귀:** `Four earned first-tier rites ... PASS`, `Tier-two distinct rites ... PASS`, `Sixteen shrines ... PASS`, `Sixteen localized covenant terms ... PASS`, `One-time celestial breach ... PASS`. 16계약 정본·제단 ID와 NBT를 유지했다.
- **생성 JAR:** `build/libs/anomalous-classes-0.1.0-alpha.1.jar`, **32,258,806 bytes, 241 classes**. [실행용 JAR + SHA-256 artifact](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/44689561180/artifacts/14608271228), [빌드 로그 artifact](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/44689561180/artifacts/14608405659).
- **검증 구분:** 코드/명세/NBT/Gradle/JAR PASS. Minecraft 클라이언트/실제 몹 AI·마법 판정/콤보 타격감·실제 제물 소비 UI·멀티플레이 및 실시간 전투/최종 3D·VFX·사운드 **NOT TESTED**. 최초 수호자 통합 시련과 심화 전투 실전 QA도 별도다. 현현과 4위계 심화 미구현.
- CI 통과 뒤 문서만 `[skip ci]`로 업데이트하며 게임 코드 변경 없음.

**CODE REVIEWED / BUILD VERIFIED / JAR PRODUCED. CLIENT / PLAYTEST / MULTIPLAYER NOT TESTED.**
