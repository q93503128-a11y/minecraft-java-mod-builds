# TURNBOUND 2D Summon Presentation v1

Status: **production implementation specification**

This document records the current user-approved direction for TURNBOUND summon presentation after the 2026-10-13 visual review.

It is subordinate to the latest explicit user decision, `PROJECT.md`, and `UI_DESIGN_SYSTEM.md`. It does not import TURNBOUND: RE canon.

---

## 1. Decision

TURNBOUND summon presentation is **2D-first**.

Production summon reveal must not use a live 3D character model as the final presentation surface.

The adopted direction is:

> high-quality 2D composition + reusable layered FX + portrait generated from the actual TURNBOUND character visual + player-paced HOLD

The goal is not to reproduce an external gacha game's artwork. The goal is to use proven gacha presentation principles while keeping the result visually consistent with TURNBOUND and maintainable as the roster expands.

### 1.1 Explicitly rejected final forms

Do not ship the summon presentation as:

- a large Foozle panel containing a live 3D inventory-style actor
- a Minecraft settings/menu-like screen with rarity text and buttons as the main spectacle
- hand-drawn Java rectangles/lines pretending to be the reveal VFX
- a separate bespoke illustration for every character that no longer resembles the in-game character
- a video playback pipeline
- a per-character full cinematic that makes new-character production prohibitively expensive
- an automatic carousel that advances from one revealed character to the next without player input

### 1.2 What remains valid from the current implementation

Keep:

- server-authoritative committed summon result
- current ★1~★5 Standard Archive canon
- explicit skip to committed summary
- `HOLD` with no timed exit
- one batch omen followed by per-result reveal
- single-pull and ten-pull support
- existing duplicate / +Level / Star Essence reward semantics

The current pure `GachaPresentationFlow` state separation is structurally useful. The presentation visuals around it must be replaced rather than discarded merely because the current drawing is weak.

---

## 2. Visual identity

### 2.1 Theme

The summon service is **정령의 기록**.

Visual vocabulary:

- dark archive / observatory
- suspended crystal / memory shard
- concentric rune circle
- thin orbit rings
- floating archive pages
- teal / cyan normal resonance
- violet high-rarity omen
- gold ★5 breakthrough
- deep navy / near-black negative space

This should feel like one authored game screen, not a stack of generic UI cards.

### 2.2 Relationship with Foozle

`UI_DESIGN_SYSTEM.md` keeps Foozle as the production chrome for physical service UI.

Therefore:

- **service entry controls** may use Foozle-compatible button/frame language
- **cinematic summon stages** should remove almost all panel chrome
- **summary cards** may use a restrained TURNBOUND/Foozle-derived border treatment
- the cinematic itself must not be enclosed in a giant Foozle panel

### 2.3 Character consistency

The summon portrait must depict the same character the player receives in battle.

Do not invent:

- different hair
- different face identity
- different costume
- extra equipment that does not exist in production
- exaggerated illustration-only body proportions that make the in-game character look unrelated

Lighting, crop, pose, background and post-processing may be more dramatic than normal gameplay.

---

## 3. Rendering strategy

### 3.1 Runtime screen is a layered 2D compositor

The final summon screen should render a small reusable set of textured layers.

Target common layer budget: **8–12 major layers**, plus lightweight particles.

Recommended order:

1. background
2. background vignette / haze
3. rear rune circle
4. crystal / resonance core
5. orbit ring A
6. orbit ring B
7. radial glow
8. rarity omen / crack layer
9. character portrait or silhouette
10. foreground sweep / shards
11. typography
12. controls

The compositor animates:

- position
- scale
- rotation
- opacity
- tint
- UV/frame selection where needed

The compositor does not improvise visual design through arbitrary filled rectangles.

### 3.2 Portrait source

Production gacha screen must not call `TurnboundPortraitRenderer.extract(...)` or `extractSilhouette(...)` every frame as the final reveal.

Preferred asset contract:

`assets/turnbound/textures/gui/summon/portraits/<character_id>.png`

Portrait characteristics:

- transparent background
- same production character visual
- approximately 3/4 presentation angle
- readable head, torso, costume silhouette and signature item/prop where relevant
- no tight crop that turns the model into large unrecognizable color blocks
- enough transparent padding to permit scale overshoot during reveal

The silhouette uses the same portrait alpha mask. It should be generated/tinted from the portrait, not approximated by placing a dark rectangle over a live 3D actor.

### 3.3 Portrait production pipeline

The intended long-term workflow is:

production actor/model → controlled portrait bake scene → transparent portrait → gacha compositor

The user should not manually render every character.

The bake pipeline must own per-character framing data, for example:

```text
characterId
visualId
scale
offsetX
offsetY
pitch
yaw
pose/animation frame
```

A new playable character should normally require only:

1. production character visual exists
2. portrait bake preset is added or uses sane defaults
3. portrait is generated
4. summon/catalog integration uses the resulting asset

If automated image export is not technically reliable on the client toolchain, the implementation must fail visibly and retain a clearly tracked generation step; it must not silently fall back to the old live-3D gacha reveal as final production art.

---

## 4. Virtual layout contract

Design on a normalized **1000 × 625 virtual canvas** (16:10) and scale to the actual GUI surface.

All primary positions below are normalized. At narrower aspect ratios, preserve focal order rather than simply shrinking text until unreadable.

Safe margin target:

- horizontal: 3% of width
- vertical: 3% of height

Do not reduce native Minecraft bitmap text to fractional sizes that cause broken glyph readability.

---

## 5. Screen A — summon entry / banner

Purpose: choose 1 pull or 10 pulls. It is an entrance to a spectacle, not the spectacle itself.

### 5.1 Composition

- title block: x 55–360, y 40–135
- central crystal focal region: x 330–700, y 80–490
- compact resource HUD: x 690–965, y 35–110
- primary pull controls: x 275–760, y 520–600
- `상세 확률`: secondary control at the right of pull controls

### 5.2 Visible information

Always visible:

- `정령의 기록`
- short one-line subtitle
- Crystal balance
- ★5 pity progress
- `1회 소환`
- `10회 소환`
- costs

Secondary:

- `첫 10회 · ★4+` if available
- `상세 확률`

Do not permanently display a giant five-tier probability table on the banner.

### 5.3 Central object

One large memory crystal is the dominant focal object.

It may use:

- one crystal sprite
- two thin orbit-ring sprites
- one rear rune-circle sprite
- one radial glow
- 2–5 floating page sprites with slow drift

This is enough. Do not build the banner from dozens of bespoke background objects.

### 5.4 Idle motion

The banner should feel alive before the player pulls:

- crystal: ±1–2% slow vertical float
- orbit A: very slow clockwise rotation
- orbit B: slower counter-rotation
- glow: subtle pulse 0.85 → 1.0 alpha
- pages: tiny drift, no distracting fast motion

---

## 6. Screen B — batch omen / ★5 breakthrough

Purpose: create anticipation before the first character is shown.

The batch omen reflects the **highest rarity committed in the batch**.

### 6.1 ★5 rule

A ★5 batch must initially read as a strong ★4 omen.

Flow:

violet resonance → short silence / visual hold → one gold fracture → expanding gold breakthrough → silhouette

Do not show `★★★★★` before the breakthrough.

### 6.2 Composition

- darkened background
- crystal centered near x 500, y 300
- violet rune circle behind
- silhouette centered/slightly right, occupying roughly 50–65% of screen height
- `건너뛰기` top-right only
- pull progress small at bottom-right

No large information panel.

### 6.3 ★5 timing target

At 20 ticks/sec:

| Time | Tick | Event |
|---|---:|---|
| 0.00s | 0 | near-black start |
| 0.15s | 3 | low resonance begins |
| 0.30s | 6 | crystal/rune wake |
| 0.70s | 14 | violet omen clearly established |
| 1.00s | 20 | motion and sound briefly compress |
| 1.15s | 23 | first gold fracture |
| 1.28s | 26 | breakthrough flash |
| 1.45s | 29 | silhouette resolves |

The final implementation may tune a few ticks during playtest, but the rhythm must preserve the anticipation beat.

### 6.4 1–4 star variation

Use the same system, not four unrelated cinematics.

- ★1: silver/neutral, shortest omen
- ★2: green/teal accent, short omen
- ★3: cyan/blue, visible rune response
- ★4: violet, stronger glow and longer silhouette
- ★5: violet false read → gold breakthrough

---

## 7. Screen C — character reveal / HOLD

Purpose: make the acquired character the visual event.

### 7.1 Composition

Preferred landscape composition:

- left information region: x 55–390
- character portrait region: x 390–960
- portrait occupies about 60–70% of screen height
- title/rarity/name never cover the face
- pull progress stays visually quiet

### 7.2 Required information

Reveal/HOLD should contain only information that improves the reward moment:

- `새로운 동료` when newly owned, otherwise duplicate outcome
- ★ rarity
- character name
- short role/identity subtitle only if canonical data supplies one
- duplicate reward summary if duplicate
- `4 / 10` style progress
- `건너뛰기`

Do not put probability, pity, currency ledger, long lore, or debug information on this screen.

### 7.3 Reveal motion

Portrait reveal should use restrained motion rather than a still image popping on:

- silhouette opacity 1 → 0
- portrait opacity 0 → 1
- portrait scale about 1.08 → 0.985 → 1.00
- slight horizontal settle of 8–16 virtual pixels
- one foreground light sweep crossing the portrait
- short background parallax response
- brief screen shake only for ★4/★5

Avoid constant looping movement after HOLD begins. Once the result is readable, the screen should calm down.

### 7.4 HOLD contract

`HOLD` has **no duration**.

No tick path may select the next pull.

Only explicit input may proceed:

- left click on primary action
- Enter
- Space

If another pull remains: `다음`.

If the last pull of a multi is held: `결과 보기`.

If it is a single pull: `확인`.

`건너뛰기` goes directly to committed summary.

---

## 8. Screen D — ten-pull summary

Purpose: compare the batch at a glance and close/repeat quickly.

### 8.1 Layout

Default desktop layout: **2 × 5** cards.

Normalized target region:

- title: x 55–300, y 35–100
- cards: x 120–900, y 150–500
- buttons: x 310–690, y 535–600

For small GUI surfaces, change card size/grid intelligently; do not preserve 5 columns by making names unreadably tiny.

### 8.2 Card content

Each card:

- portrait crop from the same production portrait asset
- character name
- rarity stars
- `NEW` if newly owned
- compact duplicate outcome otherwise

Rarity treatment:

- ★1–3: restrained neutral/blue family
- ★4: violet border/accent
- ★5: gold border/accent + slightly stronger glow

The frame must not overpower the portrait.

### 8.3 Controls

Bottom:

- `닫기`
- `다시 10회` when valid/affordable and service state permits

A repeat pull is a real currency-spending action and must still use the server-authoritative summon path.

---

## 9. Common texture asset set

Recommended initial production paths:

```text
assets/turnbound/textures/gui/summon/
  bg_archive.png
  crystal.png
  rune_circle.png
  orbit_a.png
  orbit_b.png
  radial_glow.png
  omen_violet.png
  break_gold.png
  light_sweep.png
  vignette.png
  page.png
  card_common.png
  card_rare.png
  card_legendary.png
  new_tag.png
  portraits/
```

The exact files can be atlas-packed later if profiling justifies it.

### 9.1 What can be procedural

Acceptable lightweight procedural helpers:

- a few point particles
- eased transform math
- tint/alpha
- tiny screen shake offsets

Not acceptable as the main art source:

- large `fill()` bars pretending to be magical beams
- arbitrary rectangles used as the central rarity effect
- huge flat color panels where an authored texture is expected

---

## 10. Animation easing

Do not use linear interpolation for every movement.

Minimum easing helpers:

- easeOutCubic
- easeInCubic
- easeInOutCubic
- overshoot / back-out for the portrait settle
- smooth pulse for idle glow

Every layer animation must have a reason. More motion is not automatically better.

---

## 11. Audio contract

Audio is part of the summon presentation, not an optional final polish.

Minimum cue families:

- ritual wake
- normal rarity resonance
- ★4 violet omen
- ★5 breakthrough fracture
- ★5 impact
- character reveal sting
- summary settle

★5 should include a short reduction in sound immediately before the gold breakthrough. The contrast is more important than simply making every sound louder.

Do not use unrelated combat skill sounds as the permanent final summon audio if dedicated summon cues are available.

---

## 12. Code structure

### 12.1 Keep pure flow logic

`GachaPresentationFlow` remains the authoritative client presentation state machine unless a later implementation proves a concrete problem.

Required stages remain conceptually:

```text
RITUAL
OMEN
BREAK
SILHOUETTE
REVEAL
HOLD
SUMMARY
```

The timings may move into an explicit presentation spec/data object so visual tuning does not require scattering magic numbers through the screen.

### 12.2 Rewrite presentation renderer

`GachaPresentationScreen` should become a thin compositor/controller.

Responsibilities:

- read `GachaPresentationFlow`
- query rarity presentation parameters
- draw background/common layers
- draw portrait/silhouette texture
- apply eased transforms
- draw minimal text/controls
- dispatch dedicated stage audio

It should not contain dozens of ad-hoc geometry drawing decisions.

### 12.3 Remove live-3D dependency from gacha reveal

`TurnboundPortraitRenderer` may continue serving character profile/battle UI surfaces where the design system still permits it.

The summon cinematic and result cards should use generated 2D portrait assets.

Do not delete `TurnboundPortraitRenderer` as collateral damage.

### 12.4 Suggested new classes

Names are implementation guidance, not player-facing terminology:

```text
SummonVisualSpec
SummonPortraitSpec
SummonLayerRenderer
SummonEasing
SummonAssets
```

Keep rendering constants/data-driven where practical.

---

## 13. Facility screen simplification

`FacilitySummonScreen` should stop looking like a probability dashboard.

Primary state:

- title / short subtitle
- central summon focal art
- Crystal
- pity
- 1 pull
- 10 pull
- starter guarantee badge when relevant
- `상세 확률`

Star Essence exchange remains part of the physical service, but it should be a secondary mode/tab rather than sharing the same visual weight as the summon banner.

---

## 14. Small-screen / GUI-scale behavior

Required review sizes must include at least:

- compact logical GUI comparable to 320×240
- common 16:9 desktop
- the user's observed approximately 851×519 client window

Rules:

- preserve native readable text size before decorative detail
- reduce page particles/side ornament first
- collapse subtitle/quote before shrinking core labels
- portrait may crop slightly more on narrow screens but face/name must stay visible
- summary may reduce columns before reducing text below readability

---

## 15. Acceptance criteria

The implementation is not approved merely because it compiles.

### 15.1 Banner

Pass when:

- the eye goes to the summon focal object first
- 1/10 pull actions are obvious within one glance
- pity/currency are readable but secondary
- no giant probability table dominates the screen
- it no longer resembles the current brown management panel screenshot

### 15.2 Omen

Pass when:

- ★5 can be felt before the character is visible
- the violet → gold upgrade reads instantly
- silhouette is actually derived from the character portrait
- the scene works without explanatory text

### 15.3 Reveal

Pass when:

- character occupies the visual focus
- portrait is recognizably the same character as gameplay
- no live inventory-style actor presentation is visible
- name/rarity/result are readable at normal GUI scale
- HOLD never advances automatically

### 15.4 Summary

Pass when:

- ten results are readable without hunting
- ★4/★5 hierarchy is obvious
- `NEW` and duplicate outcome are understandable
- repeating/closing is obvious

### 15.5 Quality gate

Before calling the visual work complete:

- capture actual client screenshots
- compare to this composition contract
- correct scale/crop/spacing based on real rendering
- test at multiple GUI scales
- confirm no text clipping
- confirm no automatic HOLD progression

---

## 16. Validation labels

Use these labels accurately:

- `CODE REVIEWED`
- `TESTED`
- `BUILD VERIFIED`
- `JAR PRODUCED`
- `CLIENT RUNTIME TESTED`
- `PLAYTESTED`
- `MULTIPLAYER TESTED`

A successful build is not visual approval.

This document itself is a design/implementation contract and requires no build merely for being added.