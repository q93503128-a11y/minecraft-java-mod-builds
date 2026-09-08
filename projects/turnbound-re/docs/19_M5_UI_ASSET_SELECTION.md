# 19 — M5 UI Asset Selection

최종 갱신: 2026-09-08  
상태: **PRIMARY CANDIDATE SELECTED / BINARY IMPORT PENDING / SCREENSHOT GATE PENDING**

이 문서는 M5 구조 구현 이후 실제 시각 자산을 입히는 단계가 다시 AI 즉흥 디자인이나 화면별 임시 texture 조합으로 흐르지 않도록, 자산 선택 이유와 적용 범위를 고정한다.

`17_M5_UI_VISUAL_GATE.md`의 오래된 구현 체크리스트보다 **현재 구현 상태는 `16_CURRENT_IMPLEMENTATION_STATUS.md`가 우선**한다. 이 문서는 그 다음 visual-asset 단계의 계약이다.

## 1. 현재 M5 자동 구현 상태

현재 production 경로에는 이미 다음이 존재한다.

- Battle HUD: turn rail, current actor, player HP/Energy, enemy HP/Poise/Intent/status.
- non-pausing action picker + authoritative target chooser/world marker.
- Party Formation 4-slot + Squad Cost.
- Character Overview / Skills / Growth.
- selected-character 3D entity preview.
- Battle Result / Reward terminal presentation.
- 공통 `UiVisualLanguage` semantic layer.
- FOCUS / WARNING / SUCCESS / PRIMARY / SECONDARY 정보 계층.

따라서 다음 작업은 새 화면을 만드는 것이 아니라 **같은 기능들을 하나의 실제 게임 UI skin으로 묶는 일**이다.

## 2. 조사 후보

### A. Kenney — UI Pack - Pixel Adventure

공식: https://kenney.nl/assets/ui-pack-pixel-adventure  
보조 검증: https://opengameart.org/content/ui-pack-pixel-adventure  
라이선스: **Creative Commons CC0 1.0**

확인된 구성:
- 500+ pixel UI sprites.
- thin / thick outline variants.
- separate PNG tiles + tilesheet.
- panel / button / slider/interface 계열.

평가:
- Minecraft GUI logical pixel scale과 가장 충돌이 적다.
- 한 pack 안에서 frame/button/bar/slot family를 맞출 가능성이 높다.
- CC0라 crop, palette adaptation, 9-slice 재구성, atlas packing이 가능하다.
- 원작 게임 IP의 visual identity를 가져오는 방식이 아니므로 TURNBOUND: RE의 독립성을 유지하기 쉽다.

**결론: M5 primary skin candidate.**

### B. tiopalada — Tiny RPG - Dragon Regalia GUI

공식: https://tiopalada.itch.io/tiny-rpg-dragon-regalia-gui  
보조 검증: https://opengameart.org/content/tiny-rpg-dragon-regalia-gui  
라이선스: **Creative Commons Zero v1.0 Universal**

확인된 구성:
- 5종 9-slice compatible frame.
- button rest / hover / clicked / disabled states.
- item frame 상태군.
- target cursor.
- 3종 bar.
- extendable menu header.
- portrait frame.

평가:
- 상호작용 state coverage는 매우 좋다.
- Party/Character/Growth 같은 RPG 메뉴 구조의 참고 가치가 높다.
- 그러나 원본의 강한 분홍/주황/청색 JRPG 스타일을 Kenney family와 그대로 혼합하면 화면이 서로 다른 게임처럼 보일 위험이 있다.

**결론: secondary structural reference. Primary art family와 무분별하게 혼합하지 않는다.**

## 3. 선택 원칙

### Primary family

M5 1차 실제 skin은 **Kenney UI Pack - Pixel Adventure**를 기준으로 한다.

적용 우선순위:
1. section/title frame.
2. idle / focus / disabled slot frame.
3. HP / Energy / Poise meter shell.
4. party slot / character selection frame.
5. reward row treatment.
6. button family.
7. 이후 input glyph.

### Secondary reference

Dragon Regalia는 다음 문제에만 참고한다.
- selected/hover/click/disabled의 상태 차이를 어느 정도 형태 변화로 주는가.
- 9-slice가 작은 HUD와 큰 메뉴 양쪽에서 어떻게 확장되는가.
- target cursor가 world target과 menu selection 사이에서 어떻게 읽히는가.

색/장식/캐릭터 portrait style 자체를 복제하지 않는다.

## 4. 코드 적용 경계

`UiVisualLanguage`는 계속 semantic API 역할을 한다.

좋은 구조:

```text
Gameplay / server state
        ↓
BattlePresentationModel / ProgressSnapshot
        ↓
Screen / HUD layout
        ↓
UiVisualLanguage semantic state
        ↓
TURNBOUND: RE sprite atlas
```

금지:
- Screen 클래스마다 texture Identifier를 다시 선언.
- Screen마다 selected/warning/success 색을 따로 결정.
- UI pack PNG 파일명을 gameplay 코드가 직접 알게 함.
- 자산 교체 때문에 server/gameplay logic을 수정.

최종 목표는 `UiVisualLanguage`의 backing sprite만 교체해 Battle HUD / Command / Party / Growth / Result가 동시에 같은 family로 바뀌는 것이다.

## 5. 실제 반입 시 파일 계약

외부 PNG를 저장소에 넣는 순간 다음을 모두 수행한다.

1. 원본 pack 배포 버전/파일명을 `THIRD_PARTY_ASSETS.md`에 기록.
2. CC0 출처 URL 기록.
3. 필요한 sprite만 선별한다. 500+ 파일 전체를 무의미하게 vendoring하지 않는다.
4. 수정본이라면 원본 파일명 → production 파일명 mapping을 남긴다.
5. `assets/turnbound_re/textures/gui/` 아래 TURNBOUND: RE 전용 경로에 배치한다.
6. GUI scale에서 nearest/pixel crispness를 확인한다.
7. HUD용 asset은 중앙 world viewport를 더 많이 가리지 않는지 확인한다.
8. atlas/nine-slice 적용 후 1920×1080, 1280×720, minimum-supported logical canvas screenshot을 비교한다.

## 6. Binary import가 아직 pending인 이유

이번 조사에서는 공식/배포 페이지에서 CC0와 구성까지 검증했지만, 현재 연결된 저장소 작업 경로에서 원본 ZIP을 신뢰 가능한 binary reference로 그대로 반입하는 단계까지 완료하지 않았다.

따라서:
- **자산을 사용했다고 주장하지 않는다.**
- 지금 저장소의 vanilla advancement/boss-bar sprite는 여전히 구조 검증 bridge다.
- 다음 binary pass에서 실제 원본을 확보하고 파일 단위 provenance를 기록한 뒤 교체한다.

이 상태를 건너뛰고 비슷하게 보이는 AI 제작 texture를 대신 만드는 것은 금지한다.

## 7. Screenshot acceptance

실제 skin 적용 후 다음이 모두 한 화면 언어로 보여야 한다.

Battle:
- current actor가 즉시 보임.
- enemy Intent가 일반 설명보다 우선함.
- usable/unusable action 구분이 색만 의존하지 않음.
- HP/Energy/Poise bar가 서로 헷갈리지 않음.
- 중앙 전투 장면이 여전히 주인공임.

Party/Growth:
- `◆ selected`와 `● active party`가 구분됨.
- 4 active slots가 하나의 group으로 읽힘.
- selected character 3D preview가 UI 장식에 묻히지 않음.
- 성장 전/후와 비용/부족 상태를 한눈에 비교 가능.

Result:
- outcome → rewards → Continue 순서가 시각적으로도 유지됨.
- reward가 많아져도 텍스트 dump처럼 보이지 않음.

**실제 Minecraft screenshot audit 전까지 M5 visual은 완료 처리하지 않는다.**
