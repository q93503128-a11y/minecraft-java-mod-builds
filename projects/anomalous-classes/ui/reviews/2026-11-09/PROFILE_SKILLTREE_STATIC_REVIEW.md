# 프로필·스킬 트리 정적 UI 검토

Date: 2026-11-09
Project: `anomalous-classes`
Review type: **STATIC REVIEW** — source and layout calculations; no CUA or in-game input.

## Checked

- Profile, skill tree, and class-management frame use the existing class art and class icons. Skill nodes use vanilla item icons. No class artwork or icon was replaced.
- UI text uses the active Minecraft `Font`: labels and buttons are fitted with measured `font.width`, skill descriptions are wrapped with `font.split`, and the visible description line count follows the remaining panel height. Node-rank badges use measured label width.
- Each node's `MenuUi.Box` is shared by its rendered icon and `AbstractButton` hitbox. The same rule applies to the screen buttons.
- Korean and English keys are present in both locale files. The skill text contains no internal “candidate/proposal” wording.
- The verifier compares canonical node name, order, and max rank against each of the six skill-tree Markdown sources; it also checks per-rank costs, total ranks, and 900 SP per class.
- Bounds and panel/button/node overlaps were checked at these logical screen sizes:

| Aspect | GUI 2 | GUI 3 | GUI 4 |
|---|---:|---:|---:|
| 16:9 | 960×540 | 640×360 | 480×270 |
| 16:10 | 960×600 | 640×400 | 480×300 |
| 4:3 | 800×600 | 534×400 | 400×300 |

All nine source-geometry cases passed. In compact layouts the node detail uses the height left after the graph and controls; no UI bounds exceeded the logical screen.

## Limits

- This is not a Minecraft screenshot or a client interaction test. Actual font rasterization, screen readability, keyboard navigation, narration, mouse clicks, network responses, and SavedData round-trip remain for client/server review.
- RPG XP acquisition and an approved XP threshold curve are absent. The profile displays stored experience numerically and omits a fabricated progress bar.
- Skill descriptions and rank/SP purchases are present, but passive combat effects are not connected. The UI states this explicitly.
- The class-management screen is only the requested shared frame; its center has no class-specific controls.

The development client was launched after the build and remains running. No in-world screen was opened.
