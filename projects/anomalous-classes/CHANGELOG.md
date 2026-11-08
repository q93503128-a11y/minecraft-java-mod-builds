# Changelog

## UI render-order and layout follow-up — 2026-11-08

- Fixed the selection screen's duplicate background pass: the old custom `render()` called `renderBackground()` and then `super.render()`, whose 1.21.1 implementation called `renderBackground()` again. The screen now draws its background once and renders its still-registered widgets last.
- Kept button registration, click geometry, focus, narration, server state, and selection packets intact.
- Adjusted the 16:9/16:10 split card, 4:3 stacked card, and compact overlays. Kept art proportions, showed side-card art and labels, centered the six tabs, and wrapped held-class labels.
- Added Korean/English static previews and refreshed the 9-viewport coordinate review. Existing class art and icons were not replaced in this follow-up; Minecraft interaction was not run.

## UI visual follow-up — 2026-11-08

- Rebalanced the selection card around a larger, aspect-preserved illustration and centered the text/action group inside the details panel.
- Replaced six geometric emblems with matching transparent voxel-pixel icons and restored previous art under `artwork/archive/`.
- Replaced the plantlike Parasite Morpher art with a host-takeover scene; retained the other five reviewed class scenes.
- Added visible side-card thumbnails and names, measured text fitting, and shared layout geometry for the action button and its hitbox during card transitions.
- Added a nine-viewport static layout review and updated concise Korean/English class descriptions.
- Client interaction and visual verification in Minecraft were not run.

## 0.1.0-alpha.1 — 2026-11-08

- NeoForge 1.21.1/21.1.244용 첫 실행 모드 구조를 추가했다.
- 6개 직업의 공통 캐러셀 화면과 전용 카드 아트를 추가했다.
- 최초 선택, 서버 검증, 비전투 변경, 재접속을 위한 직업별 `SavedData` 프로필 기반을 추가했다.
- 중력술사와 공명술사는 비활성 보류 영역에 표시한다.
- 전투 능력·XP 지급·스킬 트리는 이 릴리스에 포함하지 않는다.
