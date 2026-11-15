# 《이능직업 서바이벌》 — 인형술사 통합 체크포인트 / 다음 채팅 인계

> **이 문서는 새 기획이 아니다.** `q93503128-a11y/minecraft-java-mod-builds`의 최신 원격 `main`을 먼저 읽고 `projects/anomalous-classes/`에서 기존 6직업 구현을 이어간다. 다른 모드나 TURNBOUND·Augmentation Survival과 섞지 않는다. 이 파일에 기록된 빌드 시점 SHA보다 현재 `main`이 우선이다.

## 환경과 작업 계약
- Minecraft Java **1.21.1**, **NeoForge 21.1.x**, **Java 21**. 정확한 loader/Gradle/plugin 의존성은 **현재 build.gradle과 gradle.properties가 정본**.
- 필독: 루트 `AGENTS.md`, `docs/BUILD_STANDARD.md`, `docs/QUALITY_STANDARD.md`; 프로젝트 `PROJECT.md`, `BUILD_AND_RUNTIME_REPORT.md`, `ui/WORK_OWNERSHIP_AND_UI_HANDOFF_2026-11-13.md`.
- 2026-11-08 D11 사용자 결정과 D09 v4 조작 계약을 우선한다. 직업별 900SP, 3SP/레벨, 무료 비전투 패시브 초기화, 전직 시 원래 실체/효과 원상복귀, 6직업 활성/중력·공명 보류, 구형 기획으로 되돌리지 않는다.
- ChatGPT는 서버 전투·AI·경제·저장·모드 시스템 담당, Codex는 화면/3D/텍스처/애니메이션 및 실제 시각 검수 담당. 서버-클라이언트 계약을 무단 바꾸지 않는다.

## 인형술사의 현재 코드 연결
- 저장 구조: `state/PuppetBlueprint.java` (인형 고유 UUID/owner/부품/규격/체력률/공격 대기/원가 장부), `state/PuppetRegistrySavedData.java` (전개/보관/파손/분해, CAS, 동행/대기 앵커), `state/PuppetAbilitySavedData.java` (Z/X/C 쿨다운 및 연계 구동 만료 시각).
- 게임 서비스: `state/PuppetService.java` (한 제작대에서 제작·개조·수리·확장·분해, 전개/회수/전직 안전 회수, `R` 무료 표적, Shift+우클릭 동행/대기), `state/PuppetEconomy.java` (실제 바닐라 재료 비용·부품 장부·분해), `state/PuppetAbilityService.java` (Z 실 견인/X 응급 접합/C 연계 구동), `state/PuppetCombatService.java` (아군·물리 방패·구동핵 피감), `state/PuppeteerRules.java` (전체 크기/부품/레벨/매개체/패시브 계산).
- 엔티티/아이템: `entity/PuppetEntity.java`, `item/PuppetItem.java`, `item/PuppetPartItem.java`; 경량/균형/중장 골격, 칼날/투사기/방패, 기본/신속/안정/고출력 구동핵. 장비 교체로 기존 인형을 복제하지 않으며 구형 월드 레코드 불변.
- 화면 계약: `client/PuppetWorkshopScreen.java`, `network/payload/PuppetWorkshopStatePayload.java`, `PuppetWorkshopActionPayload.java`. 모든 견적/가능 상태는 서버가 제공하고, 클라이언트는 대상 인형/동작/예상 revision만 요청한다. 반드시 서버가 다시 검증한다.
- 바인딩: `R` 무료 표적, `Z` 실 견인, `X` 응급 접합, `C` 연계 구동. 현재 클라이언트는 기존 mental-class Z/X/C 키 매핑과 물리 입력을 공유하면서 두 클래스의 패킷을 전송하고, 서버가 현재 직업 검증 후 부적절한 요청을 버린다. 키 설정의 표시 명칭/명확한 설명은 UI 담당에 남아 있다.
- 인형술사 재설계 금지: 27조합이 27개의 별도 직업/인형 아이템이 아님. 시작 기본핵은 중립. 직업 조종틀은 슬롯+인형 HP/공격 스탯 외 특수 능력을 부여하지 않음. 전투 중 재료 없이 무한 무료 회복, 무조건 벽 통과 견인, 인형 복제, 피해 없이 순간 회수 금지.

## 이 단계의 자동 검증
- 이전 [`#43712379602`](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43712379602): 액티브 빌드 PASS.
- 이전 [`#43713540476`](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43713540476): 부품 개조·원가 보존 자동 검증 PASS, JAR 생성.
- 최종 동행/대기 및 일부 패시브/외형 계약의 CI 상태는 최신 `BUILD_AND_RUNTIME_REPORT.md`에서 확인. 이전 테스트의 빌드 성공으로 이후 커밋을 검증했다고 주장하지 않는다.
- 자동 테스트: `tools/PuppeteerFoundationVerifier.java`, `tools/verify_puppet_assets.py`, `build.gradle`의 관련 `check` 의존성.
- **실제 클라이언트·플레이 감각·다인 멀티 검수는 수행하지 않았다.** 특히 실제 렌더/개조 UI 가독성/실·사운드/충돌/전투 AI/프레임 성능을 플레이테스트 전 통과 처리하지 않는다.

## 새 채팅에서 우선 할 일
1. 원격 최신 `main` HEAD와 이 프로젝트 문서 및 현재 빌드 보고서를 확인해 인형술사 통합 상태를 재평가한다. 성공한 CI/JAR 아티팩트는 기존 것을 재사용.
2. 사용자 요청에 따라 **인형술사 메커니즘을 다시 만들지 않고 다음 미완성 직업**(기본 순서: 기생술사)으로 이동할 수 있다. 다만 인형술사 UI/시각/통합 플레이테스트는 정식 완성 판정 전에 별도 작업 단위로 반드시 해결해야 한다. 다른 직업 개발 중에도 자산/현재 SaveData를 파괴하지 않는다.
3. 해당 직업 기획에서 확정값과 미승인 수치 후보를 구분한다. 게임 재미/플레이 루프 우선으로 한 단위를 구현하고 CI 1회, 가능할 때 실제 게임 화면을 확인한다. 작은 단위마다 사용자 수동 테스트를 요청하지 않는다.

## 새 채팅에 넣을 프롬프트
```text
《이능직업 서바이벌》 개발을 이어서 진행한다. 새 프로젝트가 아니다.
정본 Repo: https://github.com/q93503128-a11y/minecraft-java-mod-builds
Branch: main
Project: projects/anomalous-classes/
먼저 최신 main HEAD와 AGENTS.md, docs/BUILD_STANDARD.md, docs/QUALITY_STANDARD.md,
PROJECT.md, BUILD_AND_RUNTIME_REPORT.md, PUPPETEER_NEW_CHAT_HANDOFF_2026-11-15.md,
현재 기획서 및 실제 소스를 확인하라.

인형술사는 직전 채팅에서 서버 전투/제작/전개/회수/부품 개조/3액티브/동행·대기/
저장·복제 방지까지 연결했다. 이미 구현한 시스템을 무조건 재설계하거나 삭제하지 마라.
기획서 미확정 수치와 사용자 확정 방향을 구분한다.
UI/애니메이션/시각 품질, 실제 게임플레이 및 멀티 테스트는 별도 검증 전 완료 판정 금지.
게임 시스템은 ChatGPT, 고품질 UI/에셋/시각검수는 Codex 담당 경계를 준수.
이번 채팅에서는 다음 미완성 직업의 정본 기획·소스를 확인한 뒤,
플레이 가능한 핵심 게임 루프를 구현하고 의미 있는 체크포인트에서 검증하라.
매 작은 기능마다 사용자의 수동 플레이테스트를 요청하지 말라.
```
