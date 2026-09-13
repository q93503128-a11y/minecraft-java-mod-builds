# Third-party assets and references

## Directly bundled / adapted

### Kenney UI Pack
- Type: directly usable UI asset
- Source: https://kenney.nl/assets/ui-pack
- Reproducible mirror: https://github.com/ereborstudios/kenney-ui-pack
- License: CC0 1.0 Universal
- Bundled: grey_panel.png, blue_button00.png, grey_sliderHorizontal.png
- Purpose: HUD panel, catch-bag panel/button language, reel/progress track

### Sea Life fish model geometry
- Type: editable code/model-geometry base
- Repository: https://github.com/Fuzss/sea-life
- Source line: branch `26.2.x`, inspected commit `7beb94907dd39236502e5de5def6f2528adb4340`
- License in inspected source: MIT, Copyright (c) 2021 joshiejack
- Adapted source geometry: `SmallFishModel.java`, `FatFishModel.java`, `LongFishModel.java`; tail-animation structure informed by `FishModel.java`
- Fishing Game use: three encounter-only body families in `client/fish/`, renamed and integrated with Fishing Game's own entity/render state architecture
- License notice bundled at `META-INF/licenses/fishinggame/sea-life-mit.txt`
- Direct binary texture reuse in alpha.7 is limited to UV-compatible matches: Sea Life `bass.png` -> Fishing Game `largemouth.png`, `carp.png` -> `carp.png`, `catfish.png` -> `catfish.png`, `perch.png` -> `perch.png`, and `tuna.png` -> `tuna.png`.
- The remaining catalog textures under `assets/fishinggame/textures/entity/fish/` are project-authored in alpha.7. They remain eligible for later replacement/adaptation when a clearly licensed, species-appropriate source improves quality.

## Code / behavior references

### Simple Fishing Overhaul
- Type: code/behavior reference
- Repository: https://github.com/pajicadvance/simple-fishing-overhaul
- License: MIT for mod source
- Useful reference: real fish lure/caught goals, hook interaction, fishing feel
- Fishing Game use: high-level idea of a real fish visibly moving toward the hook. Fishing Game's curved approach, burst movement, scale math and server-side burst/tension coupling are project-authored.
- Rule: preserve attribution/license notices for any intentionally copied source; otherwise adapt concepts rather than duplicating files.

## World/map status

### Cheongram Lakeside
- Type: project-authored location, not a third-party map
- Bundled form: `data/fishinggame/dimension/lakeside.json` plus Java-authored environment construction
- Reason: no external starter fishing map with both desired fit and sufficiently explicit redistribution rights has been accepted for bundling.

### Community map candidates — not bundled

Community fishing maps remain useful visual/world references, but download permission is not automatically redistribution permission. No community world is bundled until the author/license explicitly permits intended reuse or permission is obtained.

Current search direction:
- redistributable harbor/coast location or environment kit
- redistributable deep-sea/island location or environment kit
- stronger lake environment assets that can legally replace or augment the authored starter location

## Encounter fish status

Alpha.7 retires the Minecraft-native cod/salmon/tropical-fish encounter proxies. Fishing Game now owns short-lived encounter fish entity types and renders them through the three adapted silhouettes plus a mixed licensed/project-authored species texture set. They are visual/session entities, not ambient ecosystem AI and not save data.
