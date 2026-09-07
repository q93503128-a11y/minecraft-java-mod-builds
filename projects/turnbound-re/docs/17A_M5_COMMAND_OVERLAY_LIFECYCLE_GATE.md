# 17A — M5 COMMAND OVERLAY LIFECYCLE GATE

최종 갱신: 2026-09-07
상태: **AUTO GATE PASS / VISUAL QA PENDING**

이 문서는 `17_M5_UI_VISUAL_GATE.md`의 Battle command/target chooser 후속 구현 상태를 기록한다. 기존 M5 정보계층·world-first 원칙·server-authoritative 계약을 바꾸지 않으며, 해당 문서 마지막에 남아 있던 “기존 command HUD와 picker 중복 렌더 우선 정리” 작업이 완료되었음을 정본화한다.

## 1. 이번에 고정한 구조

- `BattleHud`는 turn rail, enemy summary, party status를 계속 표시한다.
- `BattleCommandScreen`이 활성화된 동안 passive `BattleHud`의 command strip만 숨긴다.
- 따라서 하단 우측 command 영역은 동시에 두 UI가 겹치지 않고, interactive picker가 단독 소유한다.
- action 선택 전에는 interactive command buttons가 표시된다.
- action 선택 후 같은 command 영역이 target chooser로 전환된다.
- 중앙 3D battle viewport는 계속 비워 둔다.
- action button에는 slot 이름뿐 아니라 concise action name과 Energy cost를 함께 보여 선택 전 정보량을 보강한다.
- target 후보/순서/번호/실행 권위는 기존과 동일하게 server snapshot에서만 온다.

## 2. 화면 생명주기 계약

새 presentation-only `BattleCommandOverlayState`가 command picker의 활성 여부만 보관한다. 전투 상태, target legality, command 결과를 계산하거나 보관하지 않는다.

`BattleCommandScreen.init()`:
- authoritative presentation이 없거나 player command 상태가 아니면 overlay를 열지 않는다.
- 유효한 player command 상태일 때만 overlay를 open한다.

`BattleCommandScreen.removed()`:
- overlay state를 반드시 close한다.
- `BattleTargetMarkerState`를 반드시 clear한다.

이 계약으로 ESC, 다른 Screen으로 교체, command 전송 후 닫힘 등 일반 Screen 종료 경로에서도 passive command HUD가 복구되고 stale world target marker가 남지 않는다.

## 3. 변경 범위

검증 코드 HEAD `3c5e3500fde1ea9155691b42b679755a6a46d825` 기준 변경은 아래 4파일로 제한된다.

- `src/main/java/kr/moonseungjun/turnboundre/client/BattleCommandOverlayState.java`
- `src/main/java/kr/moonseungjun/turnboundre/client/ui/BattleHud.java`
- `src/main/java/kr/moonseungjun/turnboundre/client/ui/BattleCommandScreen.java`
- `src/test/java/kr/moonseungjun/turnboundre/client/M5CommandOverlayStateTest.java`

전투 계산, action resolver, network payload/protocol, progression에는 변경이 없다.

## 4. 자동 검증

GitHub Actions:
- workflow: `Build turnbound-re`
- Run: `34116893379`
- validated commit: `3c5e3500fde1ea9155691b42b679755a6a46d825`
- Java: Temurin 25.0.4+1
- Gradle: 9.2.1
- NeoForge: 26.2.0.38-beta
- dependency resolution + clean build: **PASS**
- compileJava / compileTestJava / JUnit: **PASS**
- production JAR verify: **PASS**
- JAR: `turnbound_re-0.1.0-alpha.1.jar`
- JAR SHA-256: `a42aaa4c51af9d7c38f18164201156fe2cb1a6f360e1289c42edcd5de29275c7`

추가된 자동 계약:
- overlay 기본 close 상태.
- open 시 passive command HUD 억제에 사용할 상태가 true.
- close 시 다시 false.
- Screen API `removed()` override는 실제 Minecraft 26.2 / NeoForge 26.2 clean compile에서 검증됨.

## 5. 현재 판정

### COMMAND OVERLAY STRUCTURE AUTO GATE: PASS

완료:
- passive command HUD와 interactive command picker의 구조적 중복 제거.
- action → target chooser 전환 시 동일 command region 단독 소유.
- ESC/screen replacement 후 stale target marker 생존 경로 차단.
- command screen 종료 후 passive HUD 복귀 경로 확보.
- action button에 concise action identity와 Energy cost 표시.
- Java 25 clean build/JUnit/JAR verify 통과.

### 아직 PASS가 아닌 것

- 실제 Minecraft 화면에서의 겹침/가독성 최종 확인.
- GUI scale별 text clipping과 button 밀도.
- 실제 entity 위 `#N TARGET/FOCUS/SELECTED`와 하단 target slot의 시선 이동 체감.
- production sprite/icon asset 품질.
- animation/transition timing.
- Party Formation / Character / Growth production UI.

따라서 다음 M5 작업은 command overlay 구조를 다시 뜯는 것이 아니라 **실제 screenshot audit에 들어가기 직전의 battle HUD visual refinement와 runtime screenshot-ready 상태 확보**다. 구조적 중복 제거 작업은 완료된 것으로 취급한다.
