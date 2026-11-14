# 직업 매개체 인벤토리 슬롯 — 서버 계약 및 검증 상태 (2026-11-14)

> 사용자 확정: 매개체는 **Minecraft E 인벤토리 내 공통 장비 슬롯 1개**에서 장착·해제한다. 별도 직업 관리 창에서 장착하지 않는다. 원본 기준: [CLASS_FOCUS_SLOT.md](../CLASS_FOCUS_SLOT.md), [UI 수정 정본](./CLASS_MANAGEMENT_CORRECTIONS_2026-11-13.md).

## 서버 구현 현황

- `state/ClassFocusEquipmentContainer.java`: 기존 `PlayerClassProfile.equippedFocus()`를 **단일 저장 원천**으로 사용하는 서버 `Container` 1칸. 클래스 일치·실제 매개체 아이템·1개 스택을 검증하고 기존 revision/dirty 저장을 사용한다. `Player` 상위 생성자에서 `InventoryMenu`가 먼저 생성되므로 서버 참조는 **지연 접근**으로 처리한다.
- `mixin/InventoryMenuFocusMixin.java`: 바닐라 `InventoryMenu`의 기존 **0~45** 슬롯 이후에 **46번** 매개체 슬롯을 서버와 클라이언트 양쪽에 동일하게 추가한다. 서버는 영속 `Container`, 클라이언트는 1칸 `SimpleContainer`를 사용한다. 인벤토리의 기본 UI 좌표 기준 **x=77, y=35**에 임시 논리적 앵커를 둔다. UI 아트/슬롯 프레임은 Codex가 담당한다.
- 기본 클릭·커서 이동은 바닐라 `Slot` 경로를 사용한다. Shift+Click은 46번↔일반 인벤토리 9~44번 이동을 구현하며, 가득 찬 인벤토리/기존 장착/직업 불일치 시 원본 장비를 보존한다.
- `anomalousclasses.mixins.json`과 `neoforge.mods.toml`의 `[[mixins]]`에 등록했다.
- 이미 장착한 아이템은 기존 세이브 스키마와 동일한 값으로 표시되므로 마이그레이션 중 아이템을 임의 생성/삭제하지 않는다. 기존 레시피 30종, 정신 지배자 효과·서버 서비스, 프로필·스킬트리는 변경하지 않았다.

## 확인된 검증

- GitHub `main` 커밋 `f74146fd6e32d019e3e036c74e2b89942a8b20f3`에서 `Build anomalous-classes` workflow **SUCCESS** (Gradle clean build, JAR 검사, SHA-256 생성, 아티팩트 업로드). [빌드 실행](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43559779644).
- 실제 실행 중인 Minecraft `InventoryScreen`에 슬롯 46이 그려지는지, 바닐라/NeoForge/Mixin 런타임 충돌이 없는지, 클릭·드래그·번호키·더블클릭·크리에이티브·다른 모드와의 상호작용은 **NOT TESTED**. 단순 컴파일과 JAR 성공이 실제 메뉴 안전성을 입증하지 않는다.

## Codex UI/UX 담당 작업

1. **CUA/Computer Use/마우스·키보드 자동 조작은 사용하지 않는다.** 코드·정적 렌더·CLI만 수행. 실제 GUI 화면은 사용자가 직접 검수한다.
2. 바닐라 `InventoryScreen`의 추가 슬롯(논리 인덱스 46, x=77/y=35)이 창의 176×166 기본 배치와 겹치지 않는지 정적 구조를 확인하고, 슬롯 테두리/아이콘·호버 힌트를 픽셀 스타일에 맞춰 그린다. 추가 패킷/서버 상태/가짜 슬롯은 구현하지 않는다.
3. `ClassFocusScreen`의 **장착/해제 버튼 제거**, 대신 실제 30종 3×3 레시피를 보여주는 **읽기 전용 제작법 조회**로 정리한다. 이미 구현된 실제 서버 슬롯을 먼저 확인하고 제거한다. 해당 화면의 장착 버튼 관련 서버 서비스는 ChatGPT 담당으로 남긴다.
4. 직업 관리의 포커스 장착 이동 CTA는 제거하거나 **제작법 조회임을 명확히** 한다. 관리창에는 프로필·스킬트리 중복 버튼을 다시 넣지 않는다.
5. 슬롯 기능 검수: 처음 장착, 교체, 제거, Shift+Click, 꽉 찬 인벤토리, 잘못된 직업, 사망/재접속, 슬롯 표시 및 다른 화면과 충돌. **Codex는 정적/코드 검수만 수행하며 실제 게임 검수는 사용자가 한다.**

## 남은 게임 시스템 검증 (ChatGPT 담당)

- 실제 `InventoryMenu` 주입 런타임 검증과 슬롯 클릭 거래: 커서 스왑·이동·드래그·더블클릭·숫자키·Q 버리기·Creative·다른 모드 메뉴 경계.
- 서버/클라이언트 동기화, 재로그·차원 이동·사망·`keepInventory`, 멀티플레이/악의적 클릭 시도.
- 문제가 재현되면 새 우회 상태를 만들지 않고 `InventoryMenuFocusMixin`의 슬롯 거래와 `ClassFocusEquipmentContainer`의 기존 프로필 접근을 근본 수정.

## 2026-11-14 실제 사용자 스크린샷 검수 — 신규 결함

**검증 수준: 사용자가 실제 Minecraft 1.21.1 NeoForge 싱글플레이에서 촬영한 E 인벤토리 / 정신 지배자 U 화면 관측.** 스크린샷의 실행 JAR이 최신 빌드인지는 아직 입증되지 않았다.

1. **BLOCKER / 매개체 슬롯 표시되지 않음:** E 인벤토리에서 예상되는 46번 슬롯의 프레임/빈 칸 표식이 보이지 않는다. `InventoryMenuFocusMixin`의 슬롯 추가와 `InventoryScreenFocusMixin`의 렌더 주입이 실제 실행 JAR에 포함되고 적용됐는지 `latest.log` 및 실제 실행 파일부터 확인한다. `neoforge.mods.toml`의 `[[mixins]]` 구문은 NeoForge 공식 지원 형식이므로, 레이아웃만 변경하거나 중복 슬롯을 추가하는 방식으로 덮지 않는다. 개발 클라이언트가 구버전일 가능성도 개방한다. 좌표 `(77,35)`는 플레이어 모델 영역 오른쪽 경계와 시각적으로 매우 가깝다. 실제 주입 확인 후 겹침 없는 위치로 UI 담당이 조정한다.
2. **UI / J 직접 진입의 Esc 복귀 오류:** `SkillTreeScreen.onClose()`가 항상 `requestProfile()`을 호출한다. `J` 직접 진입이면 Esc는 월드로 닫혀야 하고, 프로필 내부 진입이면 프로필로 돌아가야 한다. 뒤로가기 버튼과 Esc 모두 동일한 진입 출처 규칙을 따른다. 이 경로는 Codex UI 담당이다.
3. **UI / U 정신 지배자:** 서버 기능은 있으나 무거운 버튼 나열·빈 상태의 정보 밀도가 낮은 화면이어서 최종 게임 디자인 승인 전이다. 프로필/스킬트리 중복은 재추가하지 않는다.
4. **ART / 매개체 5등급:** `textures/item/focus_mental_dominator_{standard,altered,anomalous,eldritch,incomprehensible}.png`는 실제로 다른 파일이지만 형태가 거의 같은 소형 아이콘이다. 1~5등급은 제작법 UI 레이아웃을 공유한다. 등급별 분명한 시각적 구분은 Codex의 아트 개선 과제로 남긴다. 해당 아트 수정은 서버 효과·제작법을 변경하지 않는다.

**검수 기준:** 서버 46번 슬롯과 클라이언트 46번 슬롯 둘 다 존재함을 확인하고, 실제 UI에서 빈 슬롯의 프레임/마커가 보이는지 확인한다. 슬롯이 보인 뒤에만 사용자에게 클릭·드래그·Shift+Click 테스트를 요청한다. 아이템이 보이지 않는 상태에서 장착 테스트를 강요하지 않는다. **CUA / GUI 자동화는 금지**. 실제 인게임 테스트는 사용자 담당, Codex는 CLI/파일/정적 검증만 수행.
