# Third-party assets and references

## Directly bundled

### Kenney UI Pack
- Type: directly usable UI asset
- Source: https://kenney.nl/assets/ui-pack
- Reproducible mirror: https://github.com/ereborstudios/kenney-ui-pack
- License: CC0 1.0 Universal
- Bundled: grey_panel.png, blue_button00.png, grey_sliderHorizontal.png
- Purpose: HUD panel, catch-bag panel/button language, reel/progress track

## Code / behavior references

### Simple Fishing Overhaul
- Type: code/behavior reference
- Repository: https://github.com/pajicadvance/simple-fishing-overhaul
- License: MIT for mod source
- Useful reference: real fish lure/caught goals, hook interaction, fishing feel
- Rule: preserve attribution/license notices for any copied code; prefer adapting concepts unless a file is intentionally reused.

## World/map status

### Cheongram Lakeside
- Type: project-authored location, not a third-party map
- Bundled form: `data/fishinggame/dimension/lakeside.json` plus Java-authored environment construction
- Reason: no external starter fishing map with both the desired fit and sufficiently explicit redistribution rights was accepted for bundling in this slice.
- External references may still be used to improve composition and detailing in later visual passes.

### Community map candidates — not bundled

Community fishing maps on PlanetMinecraft and similar sites, including downloadable fishing lakes/islands/harbors, remain useful visual/world references. Download permission does not automatically equal redistribution permission. No community world is bundled until its author/license explicitly permits the intended reuse or permission is obtained.

Current search direction:
- redistributable harbor/coast location or environment kit
- redistributable deep-sea/island location or environment kit
- stronger lake environment assets that can legally replace or augment the authored starter location

## Minecraft-native visual proxies

Alpha.3 uses Minecraft's own cod/salmon/tropical-fish/pufferfish entities as temporary hooked-fish visual proxies. These are runtime game entities, not bundled third-party assets. They establish the required visible fish behavior but are not the final per-species model/texture solution.
