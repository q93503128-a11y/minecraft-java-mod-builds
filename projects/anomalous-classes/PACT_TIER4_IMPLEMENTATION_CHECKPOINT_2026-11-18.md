# 계약술사 4위계 서버 구현 체크포인트 — 2026-11-18

## 정본·구현 경계
- 프로젝트: projects/anomalous-classes/. 최신 main, PROJECT.md, PLANNING_DECISION_REGISTER_V1.md, classes/PACT_TIER_3_4_DESIGN.md, classes/PACT_16_COMBAT_DESIGN_BASELINE_V3.md, classes/PACT_TIER4_DEEPENING_CHALLENGES_V1.md, PACT_ALTAR_WORLDGEN_FREQUENCY_V2.md를 우선.
- 이전 결정 유지: 6직업 주요 게임플레이 → 통합 플레이테스트 → Codex 최종 모델·VFX·UI 디자인. ChatGPT는 게임 시스템·서버 로직, Codex는 디자인 담당.
- 본 체크포인트는 기능성 전투 구현이며 실게임 품질·고유 수호자 모델·시련 페이즈·특별 현현 완성이 아니다.

## 4위계 서버 주력 권능·패시브
- 나하르: 검은 별의 재림, 전방 18블록 좁은 광역 65HP/14초. 자기 실제 무기 적중 후 20초 기록이 있으면 추가16.25HP. 무기 추가6HP/4초, 피격 후 다음 피해20% 감면. 기록 없더라도 단독 발동, 원시 피해 무한 복사 금지.
- 라키엘: 첫 서약의 검 9블록 52HP/8초, 주변 적 추가10HP. 자동 무기 추가6HP/4초, 피격 후 다음 피해15% 감면. 운명의 거부는 현재 자기 자신 보호만 구현하며, 토템이 먼저 작동하고 사망 사건 도달 시 인벤토리 금 블록2개를 서버에서 1회 소비해 HP20% 구제·180초 영구 쿨다운. 동료 지정·정식 관리 UI는 보류.
- 오르마스: 대륙의 피부 10초·피해 흡수55HP(관계·매개체 조정), 근접+20%/22초. 대륙 분쇄 8블록 60HP/13초, 피부 선행 불필요. 상시 넉백 저항55%, 낙하 피해55% 완화, 근접 후 주변 적2기에 8HP 여진/5초. 지형 무단 파괴 없음.
- 비아르: 첫 뿌리 11블록 54HP/11초와 짧은 둔화. 유효 적 공격 피해의12% 자기 회복(5초당 최대7HP), 전투 중 HP 부족 시 추가 1HP/5초, 적 피격 시 7HP 뿌리 반격/10초. 아군·비적대 개체·자해로 반복 수급 불가.
- 기존 단축키 페이지와 계약별 서버 영구 쿨다운 라우팅. 전직·해약·사망·차원 변경·로그아웃 시 일시 상태 제거. 권능 피해는 기존 관계·매개체 서버 배율 적용. 네 권능의 정확한 기본 수치는 플레이테스트 전 시험값.

## 제단·시련·제물
- 4종 제단에서 /pact challenge로 수호자 시련(4분)을 1회 완료한 뒤, 별도의 제물/해약/계약서 확인과 /pact confirm으로만 실제 체결. 성공 증명은 기존 completedTrials NBT에 보존. 시련만으로 계약·제물 소비 없음.
- 수호자는 나하르 Enderman 130HP, 라키엘 Vindicator 145HP, 오르마스 Ravager 180HP, 비아르 Husk 160HP의 기능성 전투 AI. 고유 애니메이션/전용 패턴/보스급 전투는 아직 미완성. 반복 수호자 일반 전리품·바닐라 XP·직업 처치 XP 악용 차단.
- 최초 계약 잠정 제물: 나하르 네더의 별1+메아리 조각8, 라키엘 네더의 별1+금 블록12, 오르마스 고대 잔해6+네더라이트 주괴1, 비아르 마법이 부여된 황금 사과1+이끼 블록32+에메랄드16. 해약 비용도 임시 조정값. 정기 유지비 없음, 새 계약 수 1·2·4·8배 규칙 보존.
- 기존 공유 40청크 격자 사용. 육상 2·3·4위계 임시 배분 50:33:17, 수중은 네르아만. 1위계 자연제단 미구현이므로 전16종 40:30:20:10 정식 비율 완성이 아님. 흑성/서약/거석/숲의 서로 다른 블록 형상과 전 직업 사용 상자 추가. 이는 고품질 대형 성역 건축의 대체물이 아니라 기능성 구조다.

## 검증·미완성
- PactTierFourCombatService, PactCatalog, PactSavedData, PactTrialService, PactCombatService, PactBlessingService, PactRuinFeature, PactAltarPlacement, 번역, 보상·회귀 검증기 코드 수정.
- Gradle check에 신규 PactTierFourVerifier 연결(16종 정체성/재료/권능/증명 저장/쿨다운 NBT/단일 격자 분포 검증). 빌드 성공 여부는 뒤이어 GitHub Actions 결과로 별도 확인할 것.
- 남은 범위: 1위계 유적, 공통 관계·금기·심화/현현, 고유 수호자 전투 페이즈/최종 모델·애니메이션·VFX, 정식 제단 건축·시드 실측·외부 모드/멀티플레이 실전, 6직업 공통 패시브 실효과 및 UI 검수.
## 자동 빌드·수정 이력 (2026-11-18)
- 검증 대상 기능 코드 HEAD: `4b0817c4a561267c51d3e2913c73a00ddbfb4908`. [Actions #44274849567](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/44274849567) **SUCCESS**: Java/Gradle clean build, 자동 회귀 검사, 4위계 계약 증명/서버 재사용 NBT, 2·3위계 회귀 검사, JAR 파일 내부 구조 검사, SHA-256 생성 및 업로드까지 성공.
- `PactTierFourVerifier` 결과: `Tier4 offerings, powers, proof/cooldown NBT, placement distribution: PASS 1848/1240/633` (2/3/4위계 표본). 기존 2위계 카르다 쏠림을 수정해 같은 2위계 내 종류 비중을 균등화.
- 최초 [Actions #44273816087](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/44273816087) 실패는 Minecraft Bootstrap 전 게임 아이템 레지스트리를 호출한 **검증기 자체 결함**. 게임 로직의 컴파일 오류가 아니었고, 검증기가 ResourceLocation을 검사하도록 좁게 수정해 해결.
- 치명적인 예외 수정: `PactService.isPact()`의 `player.isAlive()` 검사는 실제 사망 이벤트에서는 false가 될 수 있으므로, 라키엘의 구제에서는 살아 있는지에 무관하게 **서버 직업과 서명한 계약을 직접 조회**한다. 아직 실제 게임 사망 시나리오는 미검증.
- **검증 결과물 JAR 및 SHA:** [Actions artifact #14413584440](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/44274849567/artifacts/14413584440). 검증기에서 236개 class, JAR 32,230,066바이트 확인. [원본 로그 artifact #14413433488](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/44274849567/artifacts/14413433488).
- 이후 이 체크포인트 및 PROJECT.md 문서 갱신은 `[skip ci]`로 기록하므로 **검증된 코드/JAR 대상 SHA와 최신 문서 HEAD는 다를 수 있다.**

**검증 상태: CODE REVIEWED / BUILD VERIFIED / JAR PRODUCED. 실제 Minecraft CLIENT, WORLD-SEED, PLAYTEST, MULTIPLAYER: NOT TESTED.**
