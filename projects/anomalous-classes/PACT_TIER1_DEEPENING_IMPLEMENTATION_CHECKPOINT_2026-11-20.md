# 계약술사 1위계 심화·관계 진행도 체크포인트 — 2026-11-20

## 정본과 승인 경계
최신 main의 PROJECT.md, PLANNING_DECISION_REGISTER_V1.md, classes/PACT_DEEPENING_AND_COVENANTS.md, classes/PACT_SOFTCAP_MULTICONTRACT_POLICY_V1.md, classes/PACT_TIER4_DEEPENING_CHALLENGES_V1.md를 우선한다.

이번 묶음은 **기존 1위계 4종의 서약 심화 1·2단계**를 실제 서버 권능과 연계한다. 1위계 심화 횟수·재료·증명 횟수는 플레이테스트 전 시험값이며, 사용자 승인 없이 2~4위계의 구체 퀘스트·현현을 자동 확정하지 않는다. 기존 체결 가호 합산, 정기 유지비 없음, 1/2/4/8 배수는 유지한다.

## 서버 심화 기능
- 새로운 데이터: src/main/resources/data/anomalousclasses/pact_deepening_tier1_v1.json. 4계약 각각 초기 성취 목표, 단계별 증명 요구 3회·6회, 구분되는 2세트의 일반 재료.
- 이사엘: 수호의 빛으로 실제 적대자 공격을 자신이나 아군에게서 감소시킨다. 주민을 보호하면 기존 신앙심 상승 규칙도 적용.
- 벨칸: 실제 HP를 바치고 재화의 일격으로 적대자를 명중시킨다. 강화 후에도 HP 대가 유지.
- 우르겐: 지반 파쇄를 적대자에게 명중시킨다. 2차 심화에서 거인의 주먹→지반 파쇄 연계 타격 강화.
- 리드나: 안전한 성간 도약 후 강화 시간 내 별빛 궤적을 적대자에게 명중시킨다.
- 증명은 서버에서만 기록하며 같은 플레이어는 8초, 같은 적대 개체는 60초 간격으로 제한. 일시 제한 상태는 로그아웃·사망·직업 교체 등에 제거하고, 완료 진행도는 서버에 저장. 전 존재 관계/금기 전체가 이 작업으로 완성된 것은 아니다.
- 1차/2차 계약별 가호 증가분·권능 위력을 기본 대비 1.12/1.25로 보정. 기본 플레이어 스탯은 보정하지 않으며 기존 다중 가호 합산 순서 보존. 이사엘 표식·보호, 벨칸 화염 확산, 우르겐 넉백·연계, 리드나 안전 도약 거리·공격 연계에 단계별 차이를 추가.
- 이사엘은 기존 주민 방호/무고한 살해 관계 증감 규칙 유지. 벨칸/우르겐/리드나는 자신의 적법한 계약 권능 성취로 천천히 계약 관계 증가. 회복은 주기적 접속비·일일 기도·새 화폐와 무관.

## 서버 최종 승인
- /pact deepen <patron>에서 다음 목표·증명 수·단계별 강화·재료를 확인. 증명이 충분하면 /pact confirm으로 별도 동의.
- 실제 아이템 지불·심화 저장은 기존 서버 확인 후 단일 트랜잭션 흐름을 재사용. 전투 상태·직업·서명된 계약·심화 단계·진행도·revision·실제 재료를 재확인. 요청 취소·실패는 무소모. 심화 재료에는 새 계약의 1/2/4/8배를 곱하지 않는다.
- 심화 진행도는 기존 Bond의 새 deepeningProgress 정수 필드, deepening은 0~2. 미구현 고위계 퀘스트·현현은 생성하지 않는다. 자발적 해약으로 심화 제물은 돌려받지 않으며 새 계약을 재체결하면 기본부터 시작.
- UI는 현재 명령 기반의 기능성 계약서. 정식 계약 GUI·특별 현현·4위계 시련과 실게임 디자인은 별도.

## 검사 계획 및 미확인
- tools/PactTierOneDeepeningVerifier.java와 Gradle verifyPactTierOneDeepening을 추가. 4종 고유 성취·재료·단계, 임의 증명/단계 건너뛰기 차단, 진행도 NBT/서버 재시작, 다른 계약 분리, 해약·재계약 및 가호 보정을 확인한다.
- Java 빌드·JAR·새 검사기는 CI 완료 결과로만 판정한다. Minecraft 클라이언트/실전 적대 개체 테스트/멀티플레이는 NOT TESTED.
- 확인용: /pact contact isael → /pact confirm; /pact deepen isael에서 증명 목표 확인; 실제 적에게 수호의 빛 사용 후 증명 채우기; 요구 재료 확보 → /pact deepen isael → /pact confirm → 단계·가호 확인.

## 실제 자동 빌드 결과
- 코드 기준 HEAD: `3b6d1f1523152c9b9d3625046a9b1203cc28c0a0`.
- [GitHub Actions #44644586601](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/44644586601) **SUCCESS**: Java 21 Gradle clean build, 기존 1~4위계 계약과 16제단 검사, 신규 1위계 심화/저장 검증, JAR 구조 확인, SHA-256·아티팩트 업로드.
- 신규 검증기 실제 출력: `Four earned first-tier rites, separate evidence, staged costs, NBT and no free replay: PASS`. 16제단 분포 및 고위계 NBT 관련 기존 검증도 PASS.
- JAR: 32,245,097 bytes, 240 compiled classes. [JAR 및 SHA-256 아티팩트](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/44644586601/artifacts/14588525487), [빌드 로그](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/44644586601/artifacts/14587724178).
- 이후 문서만 [skip ci] 반영. 빌드 대상 코드 SHA와 최신 문서 HEAD는 다를 수 있다. 실제 플레이/적대 개체 발동/서버 다중 플레이/최종 UI 시각 확인은 수행하지 않았다.

**검증 상태: CODE REVIEWED / BUILD VERIFIED / JAR PRODUCED. CLIENT/PLAYTEST/MULTIPLAYER: NOT TESTED.**
