# 계약술사 2위계 심화 구현 체크포인트 — 2026-11-20

## 정본과 구현 범위
- 프로젝트: projects/anomalous-classes/, GitHub main, Minecraft Java 1.21.1 / NeoForge / Java 21.
- 기준: PROJECT.md, PLANNING_DECISION_REGISTER_V1.md, classes/PACT_DEEPENING_AND_COVENANTS.md, classes/PACT_TIER_1_2_DESIGN.md, classes/PACT_SINGLE_CONTRACT_COMBAT_PASSIVES_V2.md, classes/PACT_SOFTCAP_MULTICONTRACT_POLICY_V1.md, 기존 1위계 심화 체크포인트.
- 사용자 확정: 동시계약 3종 이상, 최초 계약 제물 1/2/4/8배, 동일 능력치 최고100%+그 외 각50% 가호, 정기 유지비 없음, 별도 사건으로서의 심화·금기·현현. 이 작업은 2위계의 각 2회 심화 **시험값**을 구현하며, 미승인 3·4위계 심화나 특별 현현은 만들지 않음.

## 신규 2위계 전투 심화
- 카르다 / 투지: 폭풍 도약 후 남은 강화 창에서 천둥 강타가 실제 적을 맞혀야 증명. 안전 도약 최대거리 기본8→1차9→2차10블록, 2차 복수 적 강타에서 한 대상에 제한된 추가 번개6HP. 뇌우 기다리기 필요 없음.
- 네르아 / 심해의 친화: 해류 붙잡기 후 심해 압력 명중 증명. 해류 이동 강도와 압력 범위 증가, 2차 후속 압력 추가 피해20%. 심화 없이도 압력 직접 사용 가능, 지상에서도 연계 가능.
- 셰로 / 관측 일치도: 자신을 노리는 적이나 실제 회피한 투사체 공격자에게 관측 응징 적중 증명. 직접 권능 강화 및 2차에서 실제 투사체 회피 직후에만 지연 추가타 5HP. 미지원 몹 공격 선예측 주장 금지.
- 하스텔 / 신용: 선지급으로 생긴 채무를 유효 적을 공격해 완납한 사건 증명. 선지급 공격/보호 지속·효율 개선, 2차 완납 후 반격 강화. 기존 채무·상환 제한·서버 저장 보존, 신용과 채무 분리.
- 데이터: src/main/resources/data/anomalousclasses/pact_deepening_tier2_v1.json. 각 계약 1·2차 증명 3회/6회, 독립된 제물 재료. 유지비 없음, 신규 계약 제물의 1/2/4/8배를 심화 비용에 일괄 적용하지 않음.
- 서버 상태: PactDeepeningService는 1·2위계 두 파일에서 총 8종을 로드하고, 기존 Bond.deepeningProgress 저장을 재사용. 검증된 실제 적대 공격·성취만 기록, 플레이어8초·개체60초 제한, 진행도/관계는 계약마다 독립.
- 해당 계약의 가호 증가분 및 권능 피해에 2위계 단계별 1.00/1.10/1.22배. 1위계 1.00/1.12/1.25와 모든 다른 위계의 기존 값 유지. 기본 스탯 전체에 무단 곱연산 없음.
- /pact deepen <id>에서 목표/진행도/재료/보상 확인 뒤 /pact confirm. 서버가 인벤토리·직업·전투상태·서명계약·진행도·revision 재검증 후 재료 1회 소비, 심화 승인. 취소/실패 무소모.
- 아직 전체 금기 충돌 상태/면책/강행 파기 및 모든 관계 회복 완성은 아님. 기존 이사엘 신앙·하스텔 채무와 2위계의 제한적 실제 성취 성장만 적용. 기존 전투 기본기는 심화 선행 없이 사용 가능.

## 검증
- 새 tools/PactTierTwoDeepeningVerifier.java / Gradle verifyPactTierTwoDeepening. 총8종, 4개 고유 목적, 2단계, 진입 제물과 심화 제물의 분리, 임계값 위조 및 재지급 방지, 채무 별도 보존, 각 계약 진행도 NBT, 해약 후 재계약 상태 초기화 확인.
- 기존 PactTierOneDeepeningVerifier에서 전체 심화 등록 수 4→8만 갱신, 1위계 테스트 삭제 안 함. 2위계 한·영 번역과 보상 사전 고지 추가.
- 수동 확인: 계약서에서 카르다/네르아/셰로/하스텔 2위계 계약 후 해당 연계 기술로 증명 축적 → /pact deepen <id> → 제물 지급 → /pact confirm. 통상적인 멀티/실게임/시드·전투 피드백 검증은 필요.
- 자동빌드/JAR/Actions 결과는 실제 실행 완료 후 문서에 추가함.

## GitHub Actions 최종 검증
- **검증 대상 코드 SHA:** e2508cdb2dcdd11e576dfa428a347c8e78d8f1b7.
- [Actions #44660488870](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/44660488870) **SUCCESS**. Java21 Gradle clean build, 컴파일, 1~4위계 계약 회귀 검증, 16제단, 신규 2위계 심화/저장·채무 격리·증명 위조 방지 테스트, JAR 구조 검사, SHA256 생성, 산출물 업로드 PASS.
- **신규 검사 실제 출력:** Tier-two distinct rites, canon thresholds, multi-bond debt, NBT roundtrip, proof cap and paid stage transitions: PASS.
- **기존 1위계 심화 검사:** Four earned first-tier rites, separate evidence, staged costs, NBT and no free replay: PASS.
- **검증된 JAR:** build/libs/anomalous-classes-0.1.0-alpha.1.jar, 32,247,944바이트 / 240 classes. [JAR+SHA-256 아티팩트](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/44660488870/artifacts/14595416893), [실행 로그 아티팩트](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/44660488870/artifacts/14595955869).
- **검증 경계:** 정적·데이터·저장 회귀 검사와 JAR 빌드는 성공했으나 실제 Minecraft 클라이언트에서 콤보 명중·채무 완납·시간창·실제 PvE 밸런스, 월드 시드·멀티플레이는 아직 실행 안 함. 모델·VFX·UI 최종 디자인 없음. 성공 코드 뒤 [skip ci] 문서만 수정.

**CODE REVIEWED / BUILD VERIFIED / JAR PRODUCED. CLIENT / PLAYTEST / MULTIPLAYER NOT TESTED.**
