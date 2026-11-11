# Changelog

## Profile and skill-tree UI visual repair — 2026-11-11

- Split the original menu frame atlas into verified corner, rail, and jewel textures; frame rails now repeat at the same source scale as corners instead of stretching guessed atlas strips across the screen.
- Grouped profile labels and values into measured two-column rows using the active Minecraft font width; kept the class illustration aspect-preserving and clipped.
- Reduced hover cards to the node name, actual effect/stat, SP cost, and one purchase-status line. Repeated prerequisite names and stage/internal text are no longer shown; server-side purchase checks are unchanged.
- Added Korean/English off-screen static review images and stricter graph/UI resource and layout checks. Minecraft client rendering and interaction were not tested.

## Six-class graph skill trees — 2026-11-10

- Replaced the old three-column rank screen with the six approved full-map skill trees: 581 purchasable nodes and 900 SP per class.
- Added pan/zoom map interaction, hover cards, class-specific backgrounds and passive icons, milestone frames, and a compact SP/reset HUD.
- Added server-checked node purchases and path requirements, persistent per-class ownership, vanilla stat-node modifiers, and one-time legacy-rank migration with SP conservation.
- The original passive combat effects were not connected in the previous implementation; this change records their stage ownership and does not represent those effects as active.
- Static graph, migration-model, translation, asset, and viewport checks were run. Minecraft client interaction and multiplayer tests were not run.

## Class selection text placement follow-up — 2026-11-08

- Removed class names from the narrow side previews so they cannot break across letters; thumbnails remain unchanged.
- Hid the compact held-class notice above the reserved hotbar clearance. On wider layouts, held names render only as a complete single line when the active Minecraft font metrics fit the text box.
- Updated the static layout checks and previews for the text-only changes. Selection behavior, card art, icons, and layout proportions were preserved; Minecraft interaction was not run.

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
