# 20 — M5 Production UI Skin Import Gate

최종 갱신: 2026-09-08
상태: **PRODUCTION ASSET IMPORTED / STATIC MIGRATION PASS / BUILD VERIFICATION TARGET / SCREENSHOT QA PENDING**

이 문서는 `19_M5_UI_ASSET_SELECTION.md`에서 선택한 M5 공통 UI 자산이 실제 production resource와 코드에 반영된 상태를 기록한다.
`16_CURRENT_IMPLEMENTATION_STATUS.md`의 과거 "final production sprite/icon/frame asset 및 source/license 기록 pending" 항목 중 **asset source/license/import 부분은 이 문서가 최신 상태를 우선한다.** 실제 Minecraft visual quality와 screenshot acceptance는 여전히 pending이다.

## 1. Production implementation commit

- production skin commit: `760fb2143a9dec31e46126212df4da2828fb73ad`
- commit message: `turnbound-re: apply verified M5 UI skin`
- one-time acquisition workflow는 production 반입 후 제거되었으며 지속 dependency가 아니다.

## 2. Verified source

- Asset: Kenney `UI Pack - Pixel Adventure (2.0)`
- Official project reference: https://kenney.nl/assets/ui-pack-pixel-adventure
- Distribution used for deterministic acquisition: https://opengameart.org/sites/default/files/kenney_ui-pack-pixel-adventure.zip
- Verified source ZIP SHA-256: `6ebf462e7f209f5f348419b09be6601a559ef1e1d6b595f0e9f8aa4c00a84048`
- License: **CC0 1.0**
- License text preserved in production resources: `META-INF/licenses/kenney-ui-pack-pixel-adventure-CC0.txt`

No proprietary UI art was copied. No unknown-license binary is present in this gate.

## 3. Imported sprite family

Only one coherent source family is used for frame/title treatment:

`Tiles/Large tiles/Thin outline`

Direct mapping:

- `tile_0009.png` → `turnbound_re:ui/frame_idle`
- `tile_0022.png` → `turnbound_re:ui/frame_focus`
- `tile_0008.png` → `turnbound_re:ui/frame_disabled`
- `tile_0020.png` → `turnbound_re:ui/frame_warning`
- `tile_0021.png` → `turnbound_re:ui/frame_success`
- `tile_0002.png` → `turnbound_re:ui/title_surface`

Each 32×32 frame/title sprite has a 4px `nine_slice` GUI scaling contract so variable-size HUD and screen regions preserve corners/borders instead of stretching them as flat rectangles.

Meters are compact 32×5 derivatives using the selected family palette:

- `turnbound_re:ui/meter_track`
- `turnbound_re:ui/meter_hp`
- `turnbound_re:ui/meter_energy`
- `turnbound_re:ui/meter_poise`

These meter strips are presentation derivatives only. They do not introduce gameplay state or duplicate server values.

## 4. Code migration

`UiVisualLanguage` no longer points at Minecraft advancement frame/title or boss-bar sprites.

Production UI requests semantic state:

- `IDLE`
- `FOCUS`
- `DISABLED`
- `WARNING`
- `SUCCESS`

Current semantic usages include:

- current turn/selected entity → `FOCUS`
- ordinary alive state → `IDLE`
- dead/unavailable/locked state → `DISABLED`
- warning presentation remains semantically separate for warning-capable call sites
- reward result row → `SUCCESS`

The temporary boolean `frame(..., boolean)` compatibility path is removed from the production semantic layer. New UI work must use `FrameState` explicitly.

## 5. Static import gate

The successful production import checked:

- source ZIP SHA-256 exact match
- required six source sprites exist
- license text copied into production resources
- every required `.png.mcmeta` exists
- TURNBOUND namespace meter resources exist
- no vanilla advancement frame reference remains in `UiVisualLanguage`
- no vanilla boss-bar reference remains in `UiVisualLanguage`
- no non-semantic `UiVisualLanguage.frame(...)` call remains in current UI package
- `git diff --check` passes

The asset application workflow run `34192904342` completed every install/migration/provenance/static/commit step successfully and pushed `760fb2143a9dec31e46126212df4da2828fb73ad` to `main`.

## 6. CI trigger note

The production commit above was created from inside a GitHub Actions job using the repository `GITHUB_TOKEN`. GitHub intentionally does not recursively create a new workflow run from such a push, so `Build turnbound-re` was not automatically created for that exact commit.

This document commit is a normal repository write under `projects/turnbound-re/**` and therefore serves as the next integration-build target containing the exact same production skin code/resources plus this audit document. Do not interpret the missing run on `760fb2143` as a code/build failure.

## 7. Visual gate remains open

This gate does **not** declare M5 visual completion.

Still required in a real Minecraft client:

- 1920×1080 default GUI scale
- 1280×720
- minimum supported logical canvas
- Battle HUD normal turn
- command open
- target selection
- Party Overview
- Party Skills
- Party Growth
- Result with no rewards
- Result with multiple rewards

Inspect at minimum:

- nine-slice border/corner integrity
- pixel crispness at Minecraft GUI scales
- text contrast and clipping
- FOCUS / WARNING / DISABLED distinction
- vanilla `Button` widgets versus the imported frame family
- world-center visibility during battle
- entity preview framing
- reward hierarchy/reveal readability

Until that comparison is performed:

- `PLAYTESTED`: **NOT TESTED**
- `MULTIPLAYER TESTED`: **NOT TESTED**
- `M5 PRODUCTION VISUAL PASS`: **NOT DECLARED**
