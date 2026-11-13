# 직업 관리 UI / 매개체 슬롯 — 2026-11-13 실제 화면 수정 정본

> 사용자 실게임 스크린샷 4장과 GitHub `main`의 실제 Java·PNG를 대조한 **우선 수정 지시**다. 이 문서의 명시적 변경사항은 과거 허브 UI/편의 이동/매개체 장착 메뉴 기획보다 우선한다. 시스템 구현과 UI 작업 책임은 `WORK_OWNERSHIP_AND_UI_HANDOFF_2026-11-13.md`를 따른다.

## 사용자 확정 요구

1. **직업 관리(U)는 현재 직업의 고유 기능만 관리**한다. 기존 프로필·스킬트리 정보(레벨/XP/SP/기본 능력치) 복제, `Profile`/ `Skill Tree` 바로가기, 하단 대형 이동 버튼 제거. `M` 프로필과 `J` 스킬트리는 기존 독립 진입 경로 유지.
2. **돌아가기**는 필요할 경우 하나의 작은 보조 제어만 둔다. 하단 외곽 프레임 위에 겹치는 대형 버튼 금지. 프로필에서 진입했다면 돌아가기, 월드에서 `U`로 직접 열었다면 `Esc`로 닫기. 직업 변경은 기존 기획대로 작고 낮은 우선순위로 우측 하단의 내부 안전 영역에 배치한다.
3. **매개체 장착/해제는 Minecraft 기본 인벤토리의 공통 전용 장비 슬롯 하나**에서 한다. 주손·보조손·갑옷 슬롯을 대체하지 않는다. 실제 서버 소유·저장·아이템 이동/커서·사망 규칙·중복 방지를 유지해야 한다. 별도 `ClassFocusScreen`의 Equip/Unequip 버튼이나 직업 관리의 Focus 장착 버튼은 폐지. 30종 Shaped Recipe 조회는 유지하되 **읽기 전용**, 장착 관리와 혼동하지 않고 직업 관리의 핵심 화면을 차지하지 않는다.
4. **프레임의 형태/아트스타일은 유지하되 손상 픽셀을 실제로 복구**한다. 이전 단순 UV/좌표/크롭 변경으로 해결되지 않은 `frame_parts/corner_*.png`의 검정·회색·색 잡음은 **PNG 소스에 이미 존재**한다(실제 `corner_tl.png`, `corner_br.png` 확인). 장식의 선/그림자를 보존하면서 외부 배경 노이즈 픽셀을 알파 제거·가장자리 정돈해야 한다. 텍스처가 고쳤다고 주장하려면 실제 RGBA·Minecraft 화면의 4모서리를 비교한다.
5. 사용자에게 작업을 완료했다고 말하기 전에 **실제 Minecraft 인게임 화면**의 화면비/GUI Scale, 클릭/스크롤/툴팁/프레임 이음새를 확인한다. 정적 Java2D 리뷰 이미지나 `runClient` 실행만으로 합격 처리하지 않는다.

## 현재 소스에서 확인한 원인

- `client/ProfileScreen.java`의 관리 버튼은 `minecraft.setScreen(new ClassManagementScreen(state))`를 직접 호출한다. 이는 `ProfileService.openManagement`의 **정신 지배자 전용 화면 라우팅을 우회**한다. 관리 진입을 `RequestClassManagementPayload` 서버 요청으로 단일화해야 한다.
- `client/ClassFocusScreen.java`의 돌아가기 버튼도 `new ClassManagementScreen(state.profile())`를 직접 열어 같은 구형 허브를 재노출한다. 현재 직업 관리로 돌아갈 때 서버 라우팅을 이용한다.
- `client/ClassManagementLayout.java` / `ClassManagementScreen.java`는 프로필·스킬트리 대형 버튼과 성장·기본 능력치 요약, 별도의 Focus 버튼을 명시적으로 그린다. **기획 위반**. 현행 직업 고유 관리 화면의 내용으로 대체한다.
- `client/ClassFocusScreen.java`에서 Equip/Unequip를 노출한다. `state/ClassFocusService.java`는 실제 슬롯 GUI가 아닌 인벤토리 배열 검색 + `ClassFocus` enum 기반 교환을 수행한다. 원래 요구인 Minecraft 인벤토리의 실물 슬롯은 **미구현**. 기존 실물 아이템/저장 데이터를 보존하면서 올바르게 이관한다.
- `frame_parts/corner_tl.png`, `corner_br.png` 실제 PNG에 검정/회색 아트 외곽 오염이 확인된다. 네 구석과 다른 이어지는 프레임 조각도 검수한다.
- 실제 스크린샷은 `Class Management` 일반 허브를 보여주며, 서버에 연결된 `MentalDominatorScreen`이 바로 열리지 않은 증상과 일치한다. 현재 실행 JAR이 최신인지도 검사한다.

## 역할 및 연결 순서

- **ChatGPT 게임 시스템 담당:** 현재 `PlayerClassProfile`/SavedData의 1슬롯 데이터를 보존하면서 진짜 인벤토리 클릭·커서·서버 슬롯 거래·드롭/사망·동기화·이관을 구현하고 테스트한다. 커스텀 슬롯 API/메뉴 통합은 NeoForge 1.21.1 호환성 확인 후 선택한다. UI 담당이 임의의 새로운 장착 데이터 모델/서버 규칙을 만들지 않는다.
- **Codex UI 담당:** 실제 정신 지배자 메뉴로 이동하는 라우팅, 직업 관리의 프로필/스킬트리/장착 조작 제거, 컨텍스트별 작은 돌아가기, 프레임 PNG 노이즈 수정 및 실제 인게임 시각 검증을 수행한다. ChatGPT가 제공하는 진짜 인벤토리 슬롯 서버 계약에 맞춰 **바닐라 인벤토리 안에** 공통 매개체 슬롯을 렌더링/연결한다.
- UI가 아직 서버 계약을 기다릴 때는 임시로 `ClassFocusScreen`의 장착 버튼을 옮긴 척하거나 인벤토리에 동작하지 않는 가짜 슬롯을 만들지 않는다. 실제 기능·UI 양쪽이 준비된 후 교체한다. 현재 UI에 잘못된 기능을 무작정 삭제하여 접근 자체를 잃는 작업은 피한다.

## 시각 검수 대상

- 사용자 제공 2026-11-13 Minecraft 실게임 스크린샷 4장: 구형 관리 허브 / 프로필 / 규격 매개체 / 불가해 매개체.
- 고칠 대상: 검정/회색 모서리 노이즈, 하단 프레임과 버튼 충돌, 왼쪽 아래 Back/Management 잘림, 하단 전체 커다란 이동 버튼, 직업별 정보 대신 성장·스탯 중복, 툴팁 내용 가림, 장비 장착 메뉴 위치.
- 이미 적용된 정신 지배자 서버 payload와 U→전용 관리자 진입 기능을 회귀시키지 않는다.
- 역할 분리와 UI 성능·플레이 품질 원칙을 지킨다. `BUILD VERIFIED`와 `CLIENT TESTED`를 혼동하지 않는다.
