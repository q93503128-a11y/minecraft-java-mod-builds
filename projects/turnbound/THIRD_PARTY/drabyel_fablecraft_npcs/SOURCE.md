# New Drabyel service NPC external asset family

- Upstream: https://github.com/hipstereclipse/FableCraft
- Upstream commit: `68bd5f37d7d41753488633071318ea58c9ad6c47`
- License: Apache-2.0
- Upstream license blob: `261eeb9e9f8b2b4b0d119366dda99c6fd7d35c64`
- Repository NOTICE file: none at inspected commit
- Classification: editable_base / direct_asset

Merchant:
- geometry `packs/Fablecraft_RP/models/entity/trader.geo.json` blob `2da8df4f5ff796130e24016db6c4b822327e982d`
- texture `packs/Fablecraft_RP/textures/entity/trader.png` blob `ad558ec45837a54d187538e234731b37a4ee75a3`

Blacksmith:
- geometry `packs/Fablecraft_RP/models/entity/villager_blacksmith.geo.json` blob `7b1d9fd16b8c1c009fc8f91d8b89c5fe598f551b`
- texture `packs/Fablecraft_RP/textures/entity/villager_blacksmith.png` blob `df00f6f8bd82f8344f87b7fd03152d1275d8e9c5`

Motion sources:
- `packs/Fablecraft_RP/animations/fable_npc.animation.json` blob `e595556954930c9c3a4488aa45c96bccf435122b`
- `packs/Fablecraft_RP/animations/fc_shared.animation.json` blob `b42ca32dc439a6572eafef48269a36235d5d55e1`

TURNBOUND modifications:
- geometry identifiers and visible bounds normalized for Java/GeckoLib;
- held-item anchor bones added without replacing the source silhouette;
- idle/greet/work clips adapted from FableCraft NPC motion language into deterministic numeric keyframes;
- final visual bases, not placeholders;
- exact New Drabyel spawn positions remain disabled until Minecraft 26.2 survey.
