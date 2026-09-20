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

Entrance greeter:
- geometry `packs/Fablecraft_RP/models/entity/guard_bowerstone.geo.json` blob `c380690126ffdca1447605713b8d010b63f04b4a`
- texture `packs/Fablecraft_RP/textures/entity/guard_bowerstone.png` blob `89efdef57078f1069f38b6c170f8390e15a02e9a`

Stable/travel keeper:
- geometry `packs/Fablecraft_RP/models/entity/villager_farmer.geo.json` blob `356673f2746360ee714338afe3a918db175c52cd`
- texture `packs/Fablecraft_RP/textures/entity/villager_farmer.png` blob `096053d3e87fee7700f9844a30177fb2db1ce78c`

Central story keeper:
- geometry `packs/Fablecraft_RP/models/entity/guildmaster.geo.json` blob `c73e2a9bda4d1cf376da42cfb47a1e7e4c6d9c83`
- texture `packs/Fablecraft_RP/textures/entity/guildmaster.png` blob `4bb8728bcefef2e686059896c35368ce5cd6b9bf`

Summon keeper:
- geometry `packs/Fablecraft_RP/models/entity/summoner.geo.json` blob `2475d9c35a36452f8350ab68ed68425481b205ea`
- texture `packs/Fablecraft_RP/textures/entity/summoner.png` blob `7b1edd49e153aebad16a8ec4ba5b2509feb51112`

Motion sources:
- `packs/Fablecraft_RP/animations/fable_npc.animation.json` blob `e595556954930c9c3a4488aa45c96bccf435122b`
- `packs/Fablecraft_RP/animations/fc_shared.animation.json` blob `b42ca32dc439a6572eafef48269a36235d5d55e1`

TURNBOUND modifications:
- geometry identifiers and visible bounds normalized for Java/GeckoLib;
- held-item anchor bones added without replacing the source silhouette;
- idle/greet/work clips adapted from FableCraft NPC motion language into deterministic numeric keyframes;
- final visual bases, not placeholders;
- role-specific silhouettes now cover greeter, travel/stables, market, blacksmith, central story and summon;
- imported PNG textures remain byte-identical to the pinned Apache-2.0 upstream files;
- added geometry files are modified derivatives: identifiers/visible bounds normalized and TURNBOUND hand-item anchors added;
- exact New Drabyel spawn positions remain disabled until Minecraft 26.2 survey.
