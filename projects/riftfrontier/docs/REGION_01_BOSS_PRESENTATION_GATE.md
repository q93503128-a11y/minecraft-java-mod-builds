# Region 01 Boss Presentation Gate

Status: **REFERENCE / ASSET-SELECTION GATE LOCKED — FINAL PRODUCTION ASSETS NOT YET SELECTED**

This document is the design gate between the verified M3 presentation runtime and any real Region 01 boss model, animation, VFX, sound, or renderer integration. It intentionally does not invent final art.

## 1. Why this gate exists

The M3 runtime can already express authoritative `telegraph -> ACTIVE -> recovery` semantics, resolve logical presentation keys, validate selected physical assets, publish them atomically on client resource reload, and refuse stale content/asset generations. That architecture must now be fed by a coherent boss presentation instead of more plumbing.

A production asset is accepted only when it improves combat readability and belongs to the Region 01 package. License compatibility alone is insufficient.

## 2. Combat identity requirements

The first Region 01 boss must be readable as a **position-control / commitment-punish** encounter rather than a high-HP version of the M2 proxies.

Required combat language:

- at least one broad committed melee/impact action with an unmistakable pre-hit pose;
- at least one displacement or line-pressure action that forces lateral or range-management movement;
- at least one arena-pressure action whose danger area is visible before activation;
- recovery poses long enough for the player to recognize a counterattack window;
- phase change must alter attack composition or arena pressure, not merely damage/health multipliers;
- every damaging action must remain driven by the authoritative `AttackPattern` timing source;
- animation, VFX and sound may emphasize timing but may not create a second independent hit clock.

This locks encounter communication, not species, name, palette, anatomy, faction origin, or final lore.

## 3. Silhouette and Minecraft-fit requirements

A candidate model must pass all of the following before bundling:

1. **Readable facing** — front/back and attack-bearing side remain obvious at typical third-person combat distance.
2. **Attack-bearing mass** — limbs, weapon, horn, tail, body mass, or equivalent geometry that performs a hit must visually support the real hit volume.
3. **Telegraph headroom** — the rig can create materially different anticipation, active, and recovery poses instead of only translating the whole body.
4. **Minecraft scale fit** — dimensions can be adapted without becoming a tiny low-detail toy or a screen-filling model whose weak points and feet disappear.
5. **Texture/style fit** — the asset can be brought into Riftfrontier's eventual Region 01 visual language without simply importing an unrelated low-poly game aesthetic.
6. **Variant stability** — base/phase variants can share a stable skeleton or deterministic resolver contract.
7. **Performance fit** — geometry, material count and animation complexity are reasonable for a boss plus surrounding encounter actors.

No model passes merely because it is animated or free.

## 4. Animation acceptance matrix

Minimum production clips or equivalent state-driven poses:

| Semantic role | Minimum visual requirement |
|---|---|
| idle / locomotion | facing and movement direction remain readable |
| telegraph: committed strike | unmistakable anticipation pose before ACTIVE |
| active: committed strike | visible strike path aligned to hit volume |
| recovery: committed strike | explicit vulnerable/reset pose |
| telegraph: line/displacement | direction and travel lane readable before movement |
| active: line/displacement | movement/action weight communicates current danger |
| telegraph: area pressure | future danger area is understandable before activation |
| phase transition | cannot be mistaken for ordinary hit-stun or idle |
| hurt / stagger where used | does not cancel authoritative attack state unless server combat state says so |
| death | terminal, non-looping presentation |

One generic `attack` animation copied across mechanically different attacks is not production-complete.

## 5. VFX and sound contract

VFX and sound are timing channels, not decoration.

- Telegraph VFX must appear early enough to communicate the same danger that `AttackPattern.telegraph` represents.
- ACTIVE VFX must not visually claim damage outside the authoritative hit window.
- Recovery VFX/sound must decay instead of continuing to signal an active threat.
- High-priority boss cues need frequency separation from ambient Region 01 audio.
- Camera shake, flash, bloom, particles and screen effects are optional and must not become the only source of readability.
- Color-only telegraphs are insufficient; shape, motion, placement or sound must provide redundant information.

## 6. External asset survey — 2026-09-09

### Quaternius — Ultimate Monsters

Source: https://quaternius.com/packs/ultimatemonsters.html

Observed source facts:

- 50 monster models;
- animated;
- FBX / OBJ / Blend / glTF distributions;
- page declares CC0 and permits personal/commercial use.

Decision: **LICENSE-ELIGIBLE SOURCE FAMILY, NOT SELECTED AS FINAL BOSS ASSET.**

Reason: the pack is valuable as a legal animated source and rig/conversion study, but selecting one merely because it is CC0 would violate the project quality gate. Exact silhouette, texture language, attack-bearing rig, Minecraft scale and Region 01 identity still need visual inspection in Blockbench/Minecraft before a production model can be accepted.

### Poly Pizza mirrors / individual Quaternius monsters

Examples inspected include public-domain/CC0 animated entries such as `Blue Demon` and `Mushroom King`.

Decision: **DISCOVERY/PROVENANCE CROSS-CHECK ONLY, NOT SELECTED.**

Reason: individual mirror pages can help identify source assets, but the immutable original author/source should be preferred when bundling. No individual model has yet passed the Riftfrontier silhouette and presentation matrix.

### GeckoLib 5

Source: https://wiki.geckolib.com/docs/geckolib5/

Observed source fact: GeckoLib's current support table lists Minecraft 26.2 with GeckoLib 5.5.1 and active support.

Decision: **TECHNOLOGY CANDIDATE APPROVED FOR THE NEXT ASSET-INTEGRATION SPIKE, NOT YET A REQUIRED DEPENDENCY.**

The dependency is added only after an accepted production animated asset demonstrates that vanilla animation/rendering would materially reduce quality or maintainability.

## 7. Asset-selection decision

No production Region 01 boss model, texture, animation, VFX, or sound asset is selected in this gate.

This is an intentional **NO-GO on premature bundling**, not a blocked task:

- a legally usable animated source family has been identified;
- technical animation compatibility has been re-verified;
- exact acceptance criteria are now locked;
- the project must next inspect real candidate geometry/animations visually before committing bytes to the repository.

Do not create placeholder production files or a fake `presentation_assets` manifest to bypass this decision.

## 8. Exact next implementation boundary

1. Inspect a bounded set of real candidate models/rigs from license-eligible sources in Blockbench or an equivalent viewer.
2. Record candidate name, immutable/original source, license, formats, skeleton/animation inventory, approximate bounds/poly/material cost, and which required semantic clips can be authored without breaking the rig.
3. Reject candidates that fail silhouette, style, timing, scale, or performance requirements even if licensing is perfect.
4. Once one candidate is accepted, record it in `THIRD_PARTY_ASSETS.md` as `SELECTED`, preserve original provenance, and add only the actually used source/derived files permitted by that license.
5. Re-verify GeckoLib 5.5.1 coordinates for NeoForge 26.2 at the moment of dependency addition.
6. Author the first real selected-asset manifest only after the physical files exist.
7. Connect `BossPresentationRenderResolver` to the chosen renderer and validate animation/VFX/sound against authoritative telegraph/ACTIVE/recovery hit windows in Minecraft.

## 9. Explicitly still NOT TESTED

- final Region 01 boss silhouette in Minecraft;
- final model scale/hitbox alignment;
- actual production animation playback;
- VFX and sound timing;
- renderer integration;
- resource-pack reload with real production boss assets;
- human combat readability and field-play.
