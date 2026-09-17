# Open-World RPG — Accessibility, Difficulty, Input & Audio Contract

> Status: **DESIGN CANON — GLOBAL PRESENTATION / COMFORT CONTRACT CLOSED BEFORE SOURCE BOOTSTRAP**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> UI: `UI_DIRECTION.md`  
> Combat: `COMBAT_BALANCE.md`  
> Multiplayer: `PARTY_MULTIPLAYER.md`  
> Dependencies: `M0_DEPENDENCY_AUDIT.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins.

This document closes the remaining global design decisions around challenge settings, accessibility/assist behavior, final frequent-action input mapping, subtitles/non-audio cues, camera comfort and audio/music state behavior. Exact music/SFX files remain external-asset bindings; the **behavioral contract** no longer waits for implementation-time invention.

The goal is not to make the game play itself. The goal is to let different players perceive the same authored information, operate the game comfortably and choose an appropriate challenge without destroying the combat/economy/world design.

---

# 1. External precedents and what is adopted

## Horizon Forbidden West

Official PlayStation accessibility documentation demonstrates a useful separation between broad difficulty presets and granular player assists: multiple difficulty settings, custom damage tuning, remappable controls with conflict guidance, hold/toggle choices, aim assist, screen-shake/camera options and configurable subtitles.

Project adoption:

- multiple challenge presets plus bounded custom tuning;
- accessibility assists are not hidden behind the easiest preset;
- fully rebindable project actions;
- hold/toggle behavior where the action supports both cleanly;
- aim assistance as an optional player setting;
- subtitle size/background controls;
- camera shake and other comfort effects separately adjustable.

Not adopted:

- no resource/loot simplification that bypasses this project's authored boss-material/equipment economy;
- no difficulty-specific superior loot or EXP reward multiplier.

Reference: `https://blog.playstation.com/2022/02/10/accessibility-features-in-horizon-forbidden-west/`

## Minecraft Dungeons

Official Minecraft Dungeons accessibility documentation exposes subtitles, enemy outline color, alternate chat-wheel interaction, screen-shake control and keyboard/controller accessible menu navigation.

Project adoption:

- important menu states remain navigable without precise mouse-only interaction;
- screen shake can be reduced to zero;
- interaction mode can use hold/release or press/select where a radial/menu action needs it;
- outlines/high-contrast aids are optional and never carry information by color alone.

Reference: `https://www.minecraft.net/en-us/accessibility/dungeons`

## Xbox Accessibility Guidelines

The current Xbox accessibility guidance recommends multiple difficulty choices, changing difficulty without losing progress, granular difficulty adjustments, remappable/toggle/auto input support and text equivalents for important audio information.

Project adoption:

- difficulty/assist changes do not erase progress;
- critical information has a visual/text equivalent when audio cannot be relied on;
- challenge and accessibility are separate controls rather than one "easy mode" switch.

References:

- `https://learn.microsoft.com/en-us/gaming/accessibility/xbox-accessibility-guidelines/108`
- `https://learn.microsoft.com/en-us/gaming/accessibility/xbox-accessibility-guidelines/104`

## Essential / Minecraft input constraints

Minecraft's standard keyboard/mouse layout keeps movement on WASD, jump on Space, sprint on Ctrl, sneak on Shift, inventory on E, hotbar on 1–9, attack/use on mouse buttons and chat on T. Essential's current documented defaults occupy `C` (Zoom), `Z` (Chat Peek), `R` (Emote Wheel), `O`, `B`, `H` and `I` for its own features.

Project adoption:

- preserve the familiar movement/inventory/hotbar/chat core;
- do not place project combat actions on Essential's documented default keys;
- deliberately retire two low-value vanilla quick actions from the RPG preset where they conflict with safer project interaction: quick-drop and swap-offhand.

References:

- `https://www.minecraft.net/article/minecraft-controls`
- `https://essential.gg/wiki/key-binds`

---

# 2. Challenge model

Difficulty exists to change combat pressure, not reward entitlement.

Rules shared by every preset:

- quest structure, enemy move sets, boss phases, loot tables, EXP/Gold rates and signature-material counts do **not** improve on harder settings;
- there is no exclusive item, achievement-critical reward or progression gate for selecting a harder preset;
- harder presets do not create giant HP sponges;
- authored telegraphs and attack animation timings remain the same across challenge presets unless a specific encounter explicitly owns a separate mechanic;
- dungeon/world layout and puzzle answers do not change by preset;
- difficulty can be changed without deleting or resetting character/world progress;
- during an active boss/encounter lock, a world-preset change queues until the encounter ends so it cannot be used as a one-frame exploit.

`COMBAT_BALANCE.md` Standard values remain the canonical balance benchmark.

## 2.1 World challenge presets

The world save owns one shared **World Challenge** preset.

| Preset | Enemy HP | Enemy outgoing damage | Enemy poise/stagger resistance | Intended feel |
|---|---:|---:|---:|---|
| Story | 90% | 70% | 90% | story/exploration first; combat still uses the real mechanics |
| Relaxed | 95% | 85% | 95% | forgiving action-RPG play |
| **Standard** | **100%** | **100%** | **100%** | canonical authored balance |
| Veteran | 105% | 115% | 105% | mistakes matter more without changing the encounter script |
| Relentless | 110% | 130% | 110% | high-pressure combat; still not an HP-sponge mode |

Important:

- `Standard` is the default and the only preset used when documents quote baseline TTK/damage assumptions;
- even `Relentless` adds only 10% enemy HP; most added pressure comes from damage and reduced stagger dominance rather than time inflation;
- co-op scaling from `COMBAT_BALANCE.md` is applied to the baseline encounter first, then the World Challenge modifiers are applied consistently;
- world challenge never modifies personal reward eligibility.

The host/world owner may change World Challenge from Settings outside an active encounter. Connected players receive a clear non-intrusive notice of the new preset.

## 2.2 Custom world challenge

`Custom` exposes the same bounded world variables rather than adding secret rules:

```text
enemy HP: 90% .. 110%
enemy outgoing damage: 60% .. 150%
enemy poise/stagger resistance: 90% .. 110%
```

Custom mode does not expose EXP/Gold/loot multipliers. It also cannot alter boss mechanics, AI scripts or phase conditions independently, because doing so would make encounter validation and multiplayer behavior unnecessarily fragmented.

---

# 3. Personal assist profile

Accessibility assists are **per player**, saved with that player's settings and server-validated where they affect gameplay. They do not alter another player's experience.

Personal assists never increase outgoing damage against shared enemies and never increase personal loot/EXP/Gold. This prevents one player's accessibility choice from shortening a shared boss for everyone else or becoming an optimization meta.

## 3.1 Combat assists

| Assist | Values | Server/gameplay rule |
|---|---|---|
| Incoming-damage assist | 100% / 85% / 70% | multiplies only damage finally received by that player after World Challenge |
| Defensive-timing assist | Off / Light / Strong | adds +0 / +1 / +2 server ticks to eligible dodge i-frame / perfect-guard / parry acceptance windows, never beyond the authored action duration |
| Guard-stamina assist | 100% / 85% / 70% | reduces only that player's Stamina loss from successful guard impact |
| Aim assist | Off / Light / Strong | target magnetism/reticle assistance only; never fires or casts automatically |
| Target readability | Standard / High contrast | strengthens legal target/weak-point outline/reticle treatment without revealing undiscovered entities through walls |

The same assist is valid in solo and co-op. The server stores/validates gameplay-affecting values so a modified client cannot request arbitrary multipliers outside these allowed values.

## 3.2 Input assists

Available independently of difficulty:

- Sprint: `Hold` / `Toggle`.
- Sneak: use Minecraft's supported behavior where possible; project actions must not break it.
- Guard: `Hold` baseline, optional `Toggle`.
- Aim/channel: `Hold` baseline, optional `Toggle` when the skill's behavior remains unambiguous.
- Radial/selection UI: `Hold & Release` or `Press & Select`.
- Repeated gathering/work input: no rapid-mash accessibility gate; interactions use bounded hold/timing actions instead of requiring high-frequency clicking.
- Double-tap-only essential actions are forbidden; every important action has a direct bind.

No assist requires choosing Story/Relaxed difficulty.

---

# 4. Final keyboard / mouse action map

Every project action is rebindable. The defaults prioritize a normal keyboard + two-button mouse and deliberately leave Essential's documented `C/Z/R/O/B/H/I` bindings untouched.

## 4.1 Preserve familiar Minecraft controls

| Action | Default |
|---|---|
| Move | `W A S D` |
| Jump | `Space` |
| Sprint | `Left Ctrl` |
| Sneak / mount dismount | `Left Shift` |
| Basic attack | `Mouse Left` |
| Use / interact / guard where equipped behavior permits | `Mouse Right` |
| RPG inventory / equipment | `E` |
| Hotbar | `1`–`9` / mouse wheel |
| Chat | `T` |
| Player list | `Tab` |
| Pause / close | `Esc` |
| Perspective | `F5` |

`E` opens the project RPG inventory/equipment screen rather than exposing vanilla inventory as the primary character UI.

## 4.2 Project combat actions

| Project action | Default | Note |
|---|---|---|
| Dodge / roll | `Q` | `Drop Item` is deliberately not a moment-to-moment RPG control |
| Active Skill 1 | `F` | vanilla quick `Swap Item With Offhand` is not the project equipment model |
| Active Skill 2 | `G` | free in the required baseline |
| Active Skill 3 | `V` | avoids Essential defaults |
| Active Skill 4 | `X` | avoids Essential defaults |
| Ultimate | `Y` | distinct from the four-skill cluster |
| Recovery Belt | `N` | deliberate heal/recovery input, not accidental hotbar use |

### Vanilla keys intentionally retired in the RPG profile

- vanilla `Drop Item` quick key is **unbound by the recommended RPG control profile**; deliberate discard occurs through the project inventory with confirmation for protected/valuable items;
- vanilla `Swap Item With Offhand` quick key is **unbound by the recommended RPG control profile**; Main Weapon/Off-hand are explicit equipment state and weapon-set behavior is project-owned.

This avoids binding two actions to one key while also reducing accidental item loss. If a player wants the old sandbox actions, both remain rebindable to another free key.

The mod must not silently overwrite an existing user's unrelated custom control scheme on every launch. On first profile creation it offers/apply-once the project control preset and reports conflicts; afterward the normal controls screen is authoritative.

## 4.3 Navigation / world actions

| Project action | Default |
|---|---|
| World map | `M` |
| Quest journal / current objectives | `J` |
| Character / class / progression | `K` |
| Mount summon / dismiss | `U` |
| Party panel | `P` |

Wardrobe, Material Pouch, Key Items, crafting/services, housing and Fish Codex are reached from their parent screen/world interaction rather than each consuming another global hotkey.

Gathering, fishing, NPC talk, doors, chests, shrines and workstations use contextual interaction rather than a separate unique key per system.

## 4.4 Dependency input ownership

- Better Combat continues to present normal melee through attack/use; it does not get a competing project skill key layer.
- Spell Engine executes project skill requests received from the five project skill/ultimate bindings; donor/default spell hotbar presentation does not become the player-facing control canon.
- Essential keeps its documented default keys (`C/Z/R/O/B/H/I`) untouched by the project defaults.
- any future dependency introducing a collision with a project frequent-action key is reconfigured/adapter-isolated during integration; the project does not make the player resolve a surprise collision as normal UX.

---

# 5. Controller / alternate-device contract

Keyboard/mouse is the first implementation target, but project UI/action architecture may not hard-code keyboard-only assumptions.

Requirements:

- every gameplay action is represented by an abstract action ID before physical binding;
- UI shows the current binding glyph/text, never a hard-coded `Press F` string;
- menus support keyboard/controller-style focus traversal in addition to mouse hover/click;
- `Back` consistently closes the current layer before closing the parent screen;
- sliders/tabs/lists can be operated without precise pixel clicking;
- radial selection can use hold/release or press/select;
- when controller support is enabled by the selected Minecraft/control stack, the project exposes equivalent bindings rather than introducing a second gameplay rule set.

Do not delay core PC implementation waiting for perfect controller support, but do not architect screens so controller navigation later requires a rewrite.

---

# 6. Subtitles, captions and non-audio cues

No progression-critical or combat-critical information may exist only in audio.

## 6.1 Dialogue subtitles

Default: **ON**.

Options:

- subtitle size: `Small / Medium / Large / Extra Large`;
- background: `Off / Light / Strong`;
- speaker name: `On / Off`;
- line duration uses spoken duration plus a readable minimum; fast dialogue never flashes a full sentence for a fraction of a second;
- subtitles wrap inside a safe screen width and never overlap the central combat reticle/HUD cluster.

## 6.2 Sound captions

Two layers:

1. **Critical captions** — boss offscreen cue, dangerous environmental cue or story interaction whose source is not already obvious. Default **ON**.
2. **Ambient captions** — wildlife/ambient/environmental sound labels. Default **OFF** to prevent caption spam; optional.

Where direction matters, captions may show a simple left/center/right source indicator. They do not reveal exact hidden enemy coordinates or bypass intended discovery.

## 6.3 Combat non-audio equivalence

Every dangerous attack must communicate through at least one reliable non-audio channel:

- readable body animation / pose;
- ground/volume telegraph matching the actual server hit area;
- UI/reticle cue where appropriate;
- optional high-contrast telegraph treatment.

A boss roar may strengthen a tell but may never be the only warning.

Perfect guard/parry, guard break, dodge success, heal resolution and status application use combined visual + sound feedback rather than relying on pitch alone.

---

# 7. Visual / motion comfort

These settings are independent of challenge:

| Setting | Range/default |
|---|---|
| Screen shake | 0–100%, default 60% |
| Camera hit impulse | 0–100%, default 70% |
| Sprint/FOV kick | 0–100%, default 50% |
| Flash intensity | 0–100%, default 70% |
| Combat VFX density | Low / Standard / High, default Standard |
| High-contrast telegraphs | Off / On, default Off |
| Enemy/target outline | Off / Standard / High Contrast, default Standard |

Rules:

- setting VFX density to Low may remove decorative particles but never removes required boss telegraphs, projectile bodies, weak-point reads or hazard boundaries;
- color is never the only signal for HP/Mana/Stamina, item grade, status type, selected/disabled UI or hostile warning state; shape/icon/border/text carries a second signal;
- flashing effects cannot be used as the only confirmation of a major action;
- screens remain readable at the GUI scales/resolutions required by `UI_DIRECTION.md`.

---

# 8. Objective clarity / replayable information

The game remains exploration-led and does not turn every discovery into a GPS checklist, but the player can always recover from forgetting what they were doing.

The Quest Journal provides:

- current main/regional objectives in plain language;
- completed major steps;
- short "story so far" summaries at act/regional-chain boundaries;
- last known relevant settlement/region when the objective is location-based;
- clear distinction between exact known location, search area and intentionally undiscovered target.

Tutorial/help entries for core mechanics remain reviewable after their first contextual appearance. Dismissing a tutorial does not permanently remove the information from the help/reference screen.

Navigation assist may strengthen an already-known objective marker/search radius, but it does not reveal undiscovered bosses, hidden POIs or puzzle answers.

---

# 9. Audio mixer / information contract

Exact SFX/BGM assets remain external-first and are selected through provenance intake. This section defines how those assets behave.

## 9.1 Player controls

Project settings expose at least:

- Master;
- Music;
- Combat / impact SFX;
- Environment / ambience;
- Creature / NPC;
- UI;
- Dialogue / voice when voiced material exists.

Minecraft/engine master categories remain usable underneath; project sliders are not permission to create a second contradictory global audio stack.

No important mechanic assumes stereo direction is audible. A player listening in mono or with one channel unavailable can still read critical gameplay through the non-audio channels in §6.

## 9.2 Dynamic music state matrix

Music uses layered/state-based transitions instead of restarting a track on every small state change.

| State | Role |
|---|---|
| Settlement / safe hub | local identity, lower urgency, supports long service sessions |
| Open exploration | region identity and travel; room for ambient world sound |
| Suspicion / nearby authored threat | light tension layer, not full combat music |
| Normal combat | clear action intensity without overpowering repeated fights |
| Elite / miniboss | stronger variant/stem; distinguish from trash combat |
| Field/world boss | dedicated high-priority combat identity |
| Dungeon exploration | location identity, lower than boss state |
| Dungeon boss | dedicated boss priority |
| Anchor / major mystery scene | recurring narrative motif; does not replace region identity everywhere |
| Finale / ending choice | bespoke highest narrative priority |
| Downed / defeat | short state/stinger; no long repetitive sad track during menus |
| Clear / major reward | short stinger layered over the state the player returns to |

Priority:

```text
finale / authored cutscene
> boss
> elite/miniboss
> normal combat
> suspense
> dungeon/exploration/settlement baseline
```

## 9.3 Transition timing

Baseline mixing targets:

- normal combat enter: about 1.0–1.5 s crossfade;
- normal combat exit: about 3–5 s decay so one stray enemy does not cause music flicker;
- boss start/phase transition: 0.5–1.0 s or authored musical transition when the encounter provides one;
- returning from boss clear: stinger first, then 3–5 s transition to local baseline;
- entering a settlement from exploration: 2–4 s blend rather than hard reset at a border.

Repeated aggro/de-aggro within a short window does not restart the track from the beginning. Music state keeps position/stems where the implementation format permits it.

## 9.4 Dialogue and critical cue ducking

When important spoken dialogue exists:

- music is normally ducked about `-6 dB` while dialogue is active;
- low-value ambience may duck slightly where masking occurs;
- dangerous combat SFX/telegraphs are not muted simply to make dialogue clean during active danger;
- UI confirm/error sounds remain short and do not punch through dialogue at excessive volume.

Exact mix is tuned by real client listening. The values here are starting targets, not proof of final mastering.

---

# 10. Multiplayer behavior

World Challenge belongs to the shared world; Personal Assists belong to individual players.

Examples:

- one player may use Strong Defensive Timing Assist while another uses Off;
- one player may receive 70% personal incoming damage while another receives 100%;
- neither assist changes shared boss HP, outgoing party damage, loot rolls, EXP or signature-material rewards;
- a player changing subtitle/camera/VFX settings never changes another player's presentation;
- host changing World Challenge affects the shared encounter baseline after the current locked encounter ends.

Revive/down timing remains authored by the combat/multiplayer canon. If a future accessibility review needs a personal rescue-window extension, it must be server-owned and may not create duplicate reward/revive transactions.

---

# 11. Save / authority / anti-exploit

Store separately:

```text
world save:
  worldChallengePreset
  customChallengeValues (when Custom)

player/profile settings:
  personalAssist values
  key bindings
  subtitle/caption settings
  camera/VFX comfort settings
  audio mix preferences
```

Gameplay-affecting assists are range-validated server-side. Client-only presentation settings do not enter world gameplay state.

Changing challenge or assist settings:

- never rerolls merchant stock;
- never resets a boss;
- never refreshes loot eligibility;
- never resets resource nodes;
- never duplicates first-clear rewards;
- never modifies accumulated EXP/Gold/items retroactively.

---

# 12. Implementation acceptance

This design gate is considered implemented only after the eventual client/server build demonstrates:

1. all project actions are rebindable and the current binding is shown in UI prompts;
2. project defaults do not collide with Essential's documented default keys;
3. first-use RPG control preset cleanly resolves retired vanilla `Q/F` actions without repeatedly overwriting later user changes;
4. all five World Challenge presets apply server-authoritatively and do not modify reward tables;
5. two multiplayer clients can use different Personal Assists without changing each other's outgoing damage/rewards;
6. subtitle size/background and Critical Caption settings work with long Korean/English strings;
7. screen shake can reach zero and required telegraphs remain visible at Low VFX;
8. boss tells remain understandable with music/SFX muted;
9. audio states do not flicker/restart during rapid aggro changes;
10. dialogue ducking and boss priority are verified by actual listening;
11. UI remains navigable through keyboard/focus flow as well as mouse;
12. no development/internal identifiers appear in any player-facing setting label.

Until those checks are run, this document is `DESIGN REVIEWED`, not `PLAYTESTED`.

---

# 13. Current verification state

- `DESIGN REVIEWED`: YES
- `EXTERNAL PRECEDENT REVIEWED`: YES
- `DIFFICULTY / ASSIST CONTRACT`: CLOSED
- `FREQUENT-ACTION INPUT MAP`: CLOSED FOR IMPLEMENTATION
- `ESSENTIAL DEFAULT KEY COLLISION AUDIT`: CLOSED AGAINST CURRENT DOCUMENTED DEFAULTS
- `SUBTITLE / NON-AUDIO CUE CONTRACT`: CLOSED
- `CAMERA / VFX COMFORT CONTRACT`: CLOSED
- `AUDIO / MUSIC STATE COVERAGE`: CLOSED AT BEHAVIOR LEVEL
- `EXACT MUSIC / SFX ASSET BINDING`: NOT COMPLETE
- `CODE REVIEWED`: N/A — gameplay source not started
- `TESTED`: NO
- `BUILD VERIFIED`: NO
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO
