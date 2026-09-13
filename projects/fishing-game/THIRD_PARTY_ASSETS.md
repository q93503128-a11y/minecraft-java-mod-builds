# Third-party assets and references

## Directly bundled / adapted

### Kenney UI Pack
- Type: directly usable UI asset
- Source: https://kenney.nl/assets/ui-pack
- Reproducible mirror: https://github.com/ereborstudios/kenney-ui-pack
- License: CC0 1.0 Universal
- Bundled: grey_panel.png, blue_button00.png, grey_sliderHorizontal.png
- Purpose: HUD panel, catch-bag panel/button language, reel/progress track

### Sea Life fish model geometry and selected textures
- Type: editable code/model-geometry base plus directly usable texture assets
- Repository: https://github.com/Fuzss/sea-life
- Source line: branch `26.2.x`, inspected commit `7beb94907dd39236502e5de5def6f2528adb4340`
- License in inspected source: MIT, Copyright (c) 2021 joshiejack
- Adapted source geometry: `SmallFishModel.java`, `TallFishModel.java`, `FatFishModel.java`, `LongFishModel.java`, `AnglerfishModel.java`; tail-animation structure informed by `FishModel.java`
- Fishing Game use: five encounter-only silhouette families in `client/fish/`, renamed and integrated with Fishing Game's entity/render-state architecture
- License notice bundled at `META-INF/licenses/fishinggame/sea-life-mit.txt`
- Direct binary texture reuse: Sea Life `bass.png` -> Fishing Game `largemouth.png`, `carp.png` -> `carp.png`, `catfish.png` -> `catfish.png`, `perch.png` -> `perch.png`, `tuna.png` -> `tuna.png`, and `anglerfish.png` -> `angler.png`.
- Remaining catalog textures under `assets/fishinggame/textures/entity/fish/` are project-authored and remain eligible for replacement when a clearly licensed species-appropriate source improves quality.

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

Alpha.8 expands the encounter presentation from three generic silhouettes to five purposeful families. Bluegill uses a taller/deeper-bodied silhouette, while the deep-sea angler gets a dedicated anglerfish silhouette and the licensed Sea Life anglerfish texture. Encounter fish remain transient visual/session entities, not ambient ecosystem AI and not save data.
