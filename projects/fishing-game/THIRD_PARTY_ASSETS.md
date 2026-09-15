# Third-party assets and references

## Directly bundled / adapted

### Kenney UI Pack
- Type: directly usable UI asset
- Source: https://kenney.nl/assets/ui-pack
- Reproducible mirror: https://github.com/ereborstudios/kenney-ui-pack
- License: CC0 1.0 Universal
- Bundled: grey_panel.png, blue_button00.png, grey_sliderHorizontal.png
- Purpose: HUD, catch-bag controls, travel screen and reel/progress tracks

### Sea Life fish model geometry and selected textures
- Type: editable code/model-geometry base plus directly usable texture assets
- Repository: https://github.com/Fuzss/sea-life
- Source line: branch `26.2.x`, inspected commit `7beb94907dd39236502e5de5def6f2528adb4340`
- License in inspected source: MIT, Copyright (c) 2021 joshiejack
- Adapted source geometry: `SmallFishModel.java`, `TallFishModel.java`, `FatFishModel.java`, `LongFishModel.java`, `AnglerfishModel.java`; tail-animation structure informed by `FishModel.java`
- Fishing Game use: five encounter-only silhouette families in `client/fish/`, integrated with Fishing Game's own entity/render-state architecture
- License notice bundled at `META-INF/licenses/fishinggame/sea-life-mit.txt`
- Direct binary texture reuse: Sea Life `bass.png` -> `largemouth.png`, `carp.png` -> `carp.png`, `catfish.png` -> `catfish.png`, `perch.png` -> `perch.png`, `tuna.png` -> `tuna.png`, `anglerfish.png` -> `angler.png`
- Remaining catalog textures are project-authored and may later be replaced when a clearly licensed, species-appropriate source improves quality.

### Fishing Frenzy deluxe fishing rod
- Type: editable item-art/model base
- Repository: https://github.com/Vg34100/Minecraft-FishingFrenzy
- Source branch: `master`
- Inspected commit: `b1409d38f19cdf4222f20863a2d699fbfb84121c`
- License: MIT, Copyright (c) 2024 Vg34100
- Upstream assets: `deluxe_fishing_rod.png`, `deluxe_fishing_rod_cast.png`, and their handheld-rod model convention
- Upstream texture blobs: normal `4e78d447c97b7eb345f0638f704f1d18b217d084`, cast `3d400366184d8cc12bc51034f909f9e50737f340`
- Fishing Game use: source silhouette/pixel structure recolored into `reed`, `lake_pro`, and `bluewater` rod families; Bluewater additionally uses Minecraft's enchantment-glint presentation rather than baking a fake glow into the source art.
- License notice bundled at `META-INF/licenses/fishinggame/fishing-frenzy-mit.txt`.
- Future prestige/rebirth-grade rods should keep using clearly licensed external bases or purpose-built final assets; do not ship placeholder rods merely to fill a tier.

## Code / behavior references

### Simple Fishing Overhaul
- Type: code/behavior reference
- Repository: https://github.com/pajicadvance/simple-fishing-overhaul
- License: MIT for mod source
- Useful reference: real fish lure/caught goals, hook interaction, fishing feel
- Fishing Game use: high-level idea of a fish visibly moving toward the hook. Curved approach, burst movement, scale math and server-side burst/tension coupling are project-authored.

## World/map status

### Current bundled locations
- `Cheongram Lakeside`: project-authored lake location.
- `Gull Harbor`: project-authored coast/harbor location added in alpha.9.
- `Deepwater Channel`: project-authored offshore/deep-sea location added in alpha.9.
- Bundled form: data-driven dedicated dimensions plus Java-authored environment construction.

### Community map candidates — not bundled

Community fishing maps remain useful visual/world references, but download permission is not automatically redistribution permission. No community world is bundled until the author/license explicitly permits intended reuse or permission is obtained.

Current external search direction remains:
- redistributable harbor/coast environment kit that can legally improve or replace project-authored structures
- redistributable deep-sea/island platform/environment kit
- higher-quality lake dressing assets with clear reuse terms

## Encounter fish status

Fishing encounters use five purposeful transient silhouette families. They are visual/session entities only, not ambient ecosystem AI and not save data.
