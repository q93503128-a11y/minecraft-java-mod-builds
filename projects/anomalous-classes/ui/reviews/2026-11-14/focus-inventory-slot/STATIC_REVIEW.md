# Focus inventory slot — static review

This is a static composite, not an in-game screenshot. It uses the Minecraft 1.21.1 vanilla inventory background/slot art, the project's 16×16 focus marker, and the same relative coordinates used by the UI code.

- Real menu slot and click/item bounds: index 46 at `(77, 35)`, size `16×16`.
- Vanilla frame: `(76, 34)`, size `18×18`.
- The empty marker is drawn only when the synchronized menu slot is empty. An occupied stack remains rendered by the native container UI.
- The checker compares the slot frame to the vanilla player-preview bounds, armor/offhand/crafting/result slots, inventory, and hotbar.
- The centered vanilla inventory placement was checked at 16:9, 16:10, and 4:3 viewport sizes with GUI Scale 2, 3, and 4; all nine logical viewports keep the frame visible and the marker aligned to the clickable slot.

The composite can catch coordinate and visual-scale mistakes in the source textures. It does not exercise GPU sampling/blending, live entity rendering, item transactions, tooltips, GUI input, or Minecraft runtime mixins. The user should review the built JAR in-game.
