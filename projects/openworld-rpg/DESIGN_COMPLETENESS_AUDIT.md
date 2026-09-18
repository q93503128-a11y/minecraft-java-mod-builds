# Open-World RPG — Design Completeness & Game Quality Audit

> Date: 2026-09-17  
> Status: **CURRENT DESIGN AUDIT / PRE-CODE QUALITY GATE**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Main quest: `WORLD_STORY_CANON.md`, `MAIN_QUEST_SCENE_PACKAGE.md`  
> Region canon: `REGIONS.md`, `REGION_CROSS_AUDIT.md`, `R01_VERTICAL_SLICE.md`, `R02_CONTENT_BIBLE.md` through `R12_CONTENT_BIBLE.md`  
> Global presentation: `ACCESSIBILITY_DIFFICULTY_INPUT_AUDIO.md`  
> Aquatic compatibility: `R11_AQUATIC_ACTION_MATRIX.md`  
> Rule: this document reports current closure, detected conflicts and pre-code blockers. It does not override `GAME_DESIGN.md` or `PROJECT.md`.

This audit replaces the earlier snapshot that still described the main story, R03–R12 and the ending as mostly unwritten. Those statements are obsolete. Git history is the archive; they are not alternate current plans.

The project is now in **late pre-production**: the reusable gameplay systems, main quest route package and all twelve regional content packages are substantially authored. Gameplay source bootstrap remains blocked by exact presentation binding, actual Azari placement, asset-dependent boss closure and final canon cleanup. Final player-facing branding remains required before branded player-facing release/presentation, but it does not block gameplay source bootstrap. `design written` is not the same as `source-ready`, `playtested` or `finished`.

---

# 1. Audit basis

The current active design corpus was cross-read against:

- repository `AGENTS.md`, `docs/BUILD_STANDARD.md`, `docs/QUALITY_STANDARD.md`;
- the Minecraft high-quality playbook;
- `GAME_DESIGN.md`, `PROJECT.md`, `README.md`;
- combat/class/status/equipment/loot/recovery/field-system/mount/UI/quest-state documents;
- `ACCESSIBILITY_DIFFICULTY_INPUT_AUDIO.md` and `R11_AQUATIC_ACTION_MATRIX.md`;
- `WORLD_STORY_CANON.md`, `MAIN_QUEST_SCENE_PACKAGE.md`, `REGION_CROSS_AUDIT.md`, `REGIONS.md`;
- R01 vertical-slice/content-bible/UI/player-text work plus the later R01 asset-intake narrowing and R02–R12 implementation-package + content-bible pairs.

The audit specifically searched for:

```text
stale canon / superseded wording
implementation-time design choices
numeric contradictions
progression or economy deadlocks
boss HP / intended TTK disagreement
quest / reward / multiplayer ownership disagreement
region repetition
player-facing development terminology
asset/license assumptions
world-placement gaps
closed gates still incorrectly listed as open
```

Historical evidence snapshots remain evidence snapshots. They do not become gameplay authority merely because they still exist in the repository.

---

# 2. Current closure verdict

## 2.1 Substantially closed before source bootstrap

These areas are sufficiently specified that coding should implement their rules rather than redesign them:

- product identity and core loop;
- Lv1–80 EXP model;
- VIT / END / STR / DEX / INT / WIL stat model;
- damage, mitigation, dodge, guard, perfect guard, poise and multiplayer boss scaling;
- five root classes, skills, first specialization branches and deeper class progression;
- equipment grades/base curves/affix philosophy/reforge/signature-item protection;
- Gold economy direction and merchant cadence;
- recovery belt, healing, food, alchemy/cooking direction;
- inventory / Material Pouch / Key Item ownership;
- Tool Pouch, gathering, fishing, Field Camp and one-residence housing model;
- mount roster and economy;
- quest personal/shared/encounter-state ownership and idempotent reward delivery;
- Anchor main story, recurring cast functions, Act structure and Restore / Release / Partition endings;
- cross-region main-quest route, scene/rejoin ownership and sequence-break handling in `MAIN_QUEST_SCENE_PACKAGE.md`;
- R01–R12 settlement identities, named casts, regional quest chains, rewards, reconnect rules, story evidence and aftermath;
- World Challenge, personal assists, frequent-action input map, subtitles/captions, non-audio cues, camera/VFX comfort and dynamic music/audio behavior;
- R11 aquatic action compatibility, including `AQUATIC_NATIVE / AQUATIC_ADAPTED / AQUATIC_DISABLED_WITH_FALLBACK` classification and swim-base layering strategy;
- server-authority requirements and the rule that real multiplayer testing is still required.

## 2.2 Genuine pre-code blockers

Source bootstrap is still blocked by work that an implementer must not improvise:

1. **exact external asset binding** — unresolved boss/creature models, NPC outfits, weapon/item families, structures, Anchor machinery, important VFX, animation, SFX/BGM and exact provenance/hash records; later R01 intake has narrowed the fish queue and several outfit/prop candidates but none of that equals binary/hash/3D/Minecraft acceptance;
2. **Azari spatial closure** — actual coordinates, route relationships, sightlines, settlement/POI/dungeon/boss placement, travel times and content-density validation;
3. **asset-gated boss/final-guardian sheets** — exact player-facing names, anatomy-supported attacks/weak points and signature materials after model acceptance where regional documents explicitly gate them;
4. **final stale-document / hidden-choice cleanup** — active files must not retain obsolete alternatives, already-closed blockers or implementation-time gameplay choices.

These are pre-code gates, not permission to `decide during coding`.

Separate player-facing release/presentation gate:

- **final player-facing title / branding string** — the production slug may remain `openworld-rpg` during gameplay bootstrap, but a branded player-facing release/presentation must not ship `TBD`, the internal slug or an unapproved candidate as the final title.

The following former blockers are now **closed at design-contract level** and must not be re-listed as open merely because runtime validation has not happened yet:

```text
R11 aquatic action/animation compatibility — CLOSED in R11_AQUATIC_ACTION_MATRIX.md
accessibility / difficulty / assist behavior — CLOSED
subtitles / non-audio cues / camera comfort — CLOSED
frequent-action input map — CLOSED
music/audio state behavior — CLOSED
cross-region main quest route/rejoin package — CLOSED in MAIN_QUEST_SCENE_PACKAGE.md
```

Exact music/SFX files still belong to blocker 1. Aquatic retarget/render quality still requires implementation/playtest validation, but neither is a missing gameplay-design decision.

---

# 3. Major consistency findings

## 3.1 Old completeness snapshot was materially wrong

The previous version of this file still described:

- main narrative as D0–D1;
- final act/ending as D0;
- R03–R12 as mostly broad regional direction;
- whole-project planning around 65–75%.

That was true before `WORLD_STORY_CANON.md`, the later regional packages, R02–R12 content bibles and the cross-region main-quest package. It is false on current `main` and is not a current alternative.

Current state is better represented as:

```text
system rules: substantially closed
regional/narrative authoring: substantially closed
cross-region main quest flow: substantially closed
accessibility/input/audio behavior: closed at design level
R11 aquatic compatibility: closed at design level
exact presentation binding: incomplete
actual world placement: incomplete
source implementation: not started
play quality: unproven until implementation/playtest
```

No single percentage is used because it creates false precision and is easy to confuse with implementation progress.

## 3.2 `implementation-ready` wording was too broad

Several older regional-package headers say their flow is `implementation-ready` while also naming exact external-asset or technical gates.

Current interpretation is stricter:

> a regional package can be **content/mechanics closed** while gameplay source for gated visible content remains **blocked**.

No package-level `implementation-ready` phrase overrides the project-wide pre-code completion contract.

## 3.3 Region index had stale encounter assignments

The old `REGIONS.md` listed R03 Basalt Wyvern/Rocky Roller direction even after later canon moved Basalt Wyvern to R10 and established Griffin + Rock Golem direction for R03. It also called region levels `working targets` pending a later EXP pass even though the EXP pass had already happened and retained the progression.

`REGIONS.md` is now treated as a current index, not a reservoir of old candidates.

The same stale R03 identities later survived in `CLASS_PROGRESSION.md` as Seismic Lunge/Hidden Technique/Insight sources. The 2026-09-17 cleanup replaces those references with the current R03 collapsed-mine/lift-route/Griffin package and removes `wyvern defense` residue. This is a canon alignment, not a class redesign.

## 3.4 Laviathan placement is closed

Older mount/R10 wording allowed an R10 or R11 unlock. Current launch canon is:

```text
Laviathan acquisition: R11 Inner Sea only
registration: 3,000 Gold after the authored handler/route trial
```

R10 may foreshadow maritime/volcanic-water travel but does not own the launch unlock.

## 3.5 R01 Verdant Crystal tool gate must not remain open

R01 Superior recipes consume Verdant Crystal. Therefore leaving `maybe Refined Pick later` in the R01 gathering table creates an avoidable progression ambiguity.

Canonical closure for source implementation:

```text
R01 ordinary Verdant Crystal nodes: Field Pick accessible
later dense regional minerals/crystals: may require Refined or Masterwork Pick as explicitly authored
```

This keeps R01 crafting non-circular and reserves meaningful tool gating for later regions.

## 3.6 Natural HP recovery had a numeric conflict

`COMBAT_BALANCE.md` retained an older `0.30% MaxHP/s` value while the dedicated recovery canon uses:

```text
8.0 s out-of-combat delay
0.40% MaxHP/s natural recovery
```

The recovery document is the dedicated authority and the combat document must align to **0.40%**. This remains deliberately slow: normal recovery tools are still materially faster.

## 3.7 Main-quest spine versus implementation package

`WORLD_STORY_CANON.md` correctly owns the premise, acts, recurring roles and ending philosophy, but its older wording still listed exact quest-by-quest scripting as later work.

`MAIN_QUEST_SCENE_PACKAGE.md` now closes the implementation-level route/rejoin structure without turning every regional quest into a duplicate main quest:

```text
R01 common opening
→ R02 or R03: at least one Act-I evidence route
→ any two of R04–R07
→ R08 or R09
→ R10 or the authored late-R11 investigation
→ R12 Central Anchor finale
→ personal Restore / Release / Partition choice
```

Unchosen regions remain fully playable and supply optional evidence, relationships, progression and epilogue state. Already-completed eligible regional content resolves by checking durable evidence/state rather than forcing a fake replay.

## 3.8 R01 asset intake is narrowed, not accepted

The current R01 intake has advanced beyond the older Pass-4 snapshot:

- River Scholar Garb: exact Wizard modular candidate family pinned;
- Ironbound Guard: exact Knight armor/pauldron candidate family pinned;
- Trail Skewers: Kenney Food Kit `skewerVegetables` editable-base candidate pinned;
- fish broad search is stopped at the four-role direct-review set: **Small Fish Common Minnow / CDmir Fish / Quaternius Armored Catfish / CDmir Esox**; old clownfish/Sea-Life/tuna candidates are explicitly rejected for Heartland;
- revive/help-up and Trail Stag mount/dismount still require accepted external clip/runtime review.

This reduces unknown-source risk but does **not** permit `R01 ASSET READY = YES` until exact acquisition/hash, 3D review, conversion/retarget and actual Minecraft acceptance happen.

## 3.9 Closed design versus runtime proof

A design gate being closed means the implementer no longer chooses the gameplay behavior while coding. It does not mean the feature is proven good in-game.

Examples:

- the input map is closed, but conflict/readability must still be tested in the real client;
- R11 aquatic layering is closed, but animation quality must still be retargeted and viewed in Minecraft;
- dynamic audio state behavior is closed, but actual clips/mix require listening;
- boss TTK budgets are authored baselines, not playtested truth.

This distinction must remain explicit in all completion claims.

---

# 4. Combat / boss balance audit

`COMBAT_BALANCE.md` explicitly states that this is not an MMO HP-sponge game. Its benchmark model provides a useful consistency test:

```text
GearScale(L) = 1 + 0.055 * (L - 1)
WeaponBudget(L) = 22 * GearScale(L)
BenchmarkDPS(L) = WeaponBudget(L) * (1.15 + 0.0075 * (L - 1))
```

For a boss authored around a target **active solo TTK**, its starting HP should be approximately:

```text
BossHP_start = BenchmarkDPS(L) * target_active_TTK
```

Then real play may adjust HP after animation, defense, phase downtime and vulnerable uptime are measured. **Do not add HP to compensate for long untargetable phases; shorten/fix the downtime instead.**

The audit found that several late-region draft HP ranges drifted well above their own stated TTK targets.

| Encounter | Lv | Intended active TTK | Audited starting HP band | Result |
|---|---:|---:|---:|---|
| R04 Ferox Iceworm | 24 | 200–225 s | ~13.0–15.0k | current band only slightly high |
| R04 Icebroodmother | 25 | 170–195 s | ~11.5–13.0k | current band slightly high |
| R05 mature Earthloong | 26 | 200–225 s | ~14.0–15.5k | current band high |
| R06 Hydra | 35 | 190–225 s | ~17.0–20.0k | current band good |
| R07 Ferox Deathworm | 39 | 200–235 s | ~19.5–23.0k | current band high |
| R08 Titan Rabbit | 49 | 210–245 s | ~25.5–29.5k | current 33–38k is too high |
| R09 optional major hunt | 49 | 195–230 s | ~23.5–28.0k | use after model selection |
| R10 Basalt Wyvern | 63 | 205–240 s | ~32.0–37.5k | current 42–48k is too high |
| R10 Inferno/final guardian role | 64 | 220–270 s | ~35.0–43.0k | current 48–58k is too high |
| R11 Riptooth | 51 | 195–225 s | ~24.5–28.5k | current 28–33k is high |
| R11 Abyss Fang | 69 | 235–290 s | ~40.5–50.0k | current 55–65k is too high |
| R12 Terradragon | 78 | 250–290 s | ~49.5–57.5k | current 48–58k is coherent |
| R12 final systemic guardian | 80 | 285–320 s | ~58.5–65.5k | current 58–66k is coherent |

These are **authoring baselines**, not final playtested values. Defense, MR, movement, target uptime and multiplayer scaling still require actual measurement.

Important conclusion:

- the global DPS/TTK framework is not the problem;
- R06 and R12 already demonstrate that the formula can produce coherent boss budgets;
- the correction is to realign the drifted regional boss HP ranges, not to inflate global player damage and destabilize the entire combat model.

---

# 5. Progression / EXP audit

The **curve itself** remains internally coherent:

```text
EXP_to_next(L) = round_to_10(100 + 50L + 4L^2)
launch cap = 80
```

However, the later R01 closure audit found a real pacing contradiction that this earlier audit had missed.

Old R01 authored one-time EXP budget was only roughly **284% of a current-level requirement before ordinary combat and the previously-undefined Earthloong boss layer**, yet the same canon expected a Lv1 character to approach a Suggested-Lv8 first dungeon/boss inside 55–75 minutes. Because the rewards are expressed as percentages of the receiver's current next-level requirement, the absolute quadratic curve cannot solve that mismatch: roughly seven level gains still require roughly seven level-equivalents of reward.

The correction now in canon is deliberately narrow:

- keep the global Lv1–80 EXP curve;
- correct the mixed-play Lv1–10 target from the impossible 4–6 min/Lv to **9–13 min/Lv**;
- front-load **R01 one-time** quest/discovery/first-boss/first-dungeon percentages;
- keep repeat events, repeat bosses and ordinary combat on the normal global percentage family;
- revise the ordinary R01 first-clear expectation to roughly **Lv6–8**, with Suggested Lv8 remaining a safety/readability recommendation rather than a hard gate.

The corrected R01 one-time authored budget is now large enough that a broad-path player can reach the first dungeon without mob grinding, while a completionist route approaches the upper end of the band.

Current later-region reward hierarchy remains healthy:

```text
ordinary kill: small
exploration/event: small-to-medium
elite/miniboss: meaningful
regional quest: large
first dungeon clear: very large
field/world boss first clear: large
```

Public Guild Wars 2 tables remain a useful structural comparison: successful dynamic events award a small fraction of same-level progression, story instances more, and dungeon completions substantially more. The project uses a stronger **opening-region exception** because R01 must establish the whole combat/class/equipment loop quickly without grinding; this exception must not leak into repeat farming or become a blanket multiplier for R02+.

Audit rule:

- ordinary repeat event/discovery rewards generally stay around the existing 5–10% family;
- later normal regional/main steps generally stay around their existing authored bands;
- R01 first-completion values are an explicit onboarding exception and are written directly in the R01 canon rather than inferred during coding;
- a dungeon first clear + boss may grant more than one level-equivalent in R01 only because it is a one-time opening climax;
- repeated farming always falls back to repeat rewards and anti-overlevel rules;
- do not increase every later-region percentage merely because the absolute EXP requirement is larger.

No global EXP-curve change is recommended before playtesting.

---

# 6. Economy / loot audit

The economy is structurally strong because Gold is one primary currency and systems share it rather than inventing regional tokens.

Current useful anchors remain coherent with same-tier income:

- Small / Town / Large / Prestige housing: 2,400 / 9,000 / 25,000 / 65,000+ Gold;
- Jungle Komodo: 600 Gold registration;
- Caravan Elephant: 1,500 Gold;
- Laviathan: 3,000 Gold;
- Sky Drake: 6,000 Gold;
- recovery and normal merchant purchases stay small relative to those savings goals.

The boss signature-material system is intentionally generous but suitable for a private authored RPG:

```text
first eligible clear: 2 signature materials
repeat clear: 1
4 materials + Gold service fee: craft one chosen known Mythic
```

This prevents an unlucky player from grinding one boss indefinitely and is consistent with the project's anti-chore philosophy.

No generic enhancement treadmill, salvage currency, boss token or fishing currency should be added without a new real design need.

Stale loot-document blockers claiming that weapon curves, affix curves or forge/reforge rules are still unknown are obsolete; `EQUIPMENT_BALANCE.md` already closes those rules. Remaining equipment work is primarily exact regional catalog/model/icon/source binding and model-linked Mythic completion.

---

# 7. World / quest / narrative comparison

Public production material supports the project's current direction:

- Guerrilla's Horizon quest-system material emphasizes non-linearity and a robust quest-state language; this project already has explicit personal/shared/encounter ownership, late join, split-party progress and idempotent rewards in `QUEST_WORLD_STATE.md`.
- Bethesda's Skyrim/Fallout production material emphasizes plan → implement → test → polish for huge quantities of open-world content. The project therefore must not confuse completed documents with proven gameplay.
- CD Projekt RED's Witcher/Cyberpunk quest-design lessons emphasize engagement, brevity, fun, emotional impact, consequences, novelty and production effectiveness; the regional bibles deliberately avoid a repeated `town → three chores → boss → ancient device` grammar and specify visible aftermath.
- the publicly documented Van Buren Denver package is an 83-page area design covering places, characters and quests. The project's package+bible pairs plus `MAIN_QUEST_SCENE_PACKAGE.md` are now in the same **kind** of production-document territory: named people, objective conditions, route/rejoin behavior, rewards, state and aftermath exist. However the project is still behind a true production-ready area package in one important respect: **actual Azari spatial placement, sightlines, travel time and accepted final assets are not yet bound.**

This comparison means the next useful work is not adding more systems. It is binding the already-authored design to the actual world and actual presentation sources.

---

# 8. Region anti-repetition audit

The twelve major regions now have different gameplay/story theses rather than palette-swapped Anchor incidents:

- R01: grounded introduction and first relay discovery;
- R02: continuous environmental trail / memory relay;
- R03: vertical infrastructure and first continental network geometry proof;
- R04: bounded restoration visibly helps ordinary people;
- R05: a modern ecology/society successfully adapted after old regulation faded;
- R06: retain useful infrastructure while severing remote authority into local stewardship;
- R07: centralized optimization can sacrifice peripheral communities under scarcity;
- R08: technically successful old-style stabilization can suppress valuable modern magical ecology;
- R09: people can build redundant roads/signals/depots without restoring centralized authority;
- R10: restoration works locally, then remote coupling creates a fast cascade;
- R11: the same success creates a delayed consequence far away and far below;
- R12: the previous regional truths are physically reconciled at the over-coupled Central Anchor.

This is a strong anti-repetition foundation. Actual Azari placement must preserve it: if every region is laid out as hub → straight road → dungeon at far edge, the documents' variety will still be lost in play.

---

# 9. Player-facing development-language audit

Production documents may use internal identifiers, gates, status labels and test terminology.

The game may not.

Forbidden player-facing examples include:

```text
P0 / P1
alpha / beta
prototype / placeholder / temporary
TODO / debug / developer
asset intake / license / hash
internal quest-state IDs
acceptance-test labels
milestone names
```

Missing assets/localization are build/content failures. They are never explained to the player with development text.

This rule applies to UI, quests, dialogue, tooltips, item descriptions, loading text, tutorials and system messages.

---

# 10. Final pre-bootstrap acceptance gate

Do not begin gameplay source bootstrap until the remaining blockers are explicitly closed or scoped to a technically unavoidable bootstrap-only tooling choice.

Required remaining pre-code exit condition:

```text
all gated visible gameplay content has an accepted external source direction/binding + provenance boundary
Azari major content has spatial coordinates + route/sightline/travel/content-density targets
asset-dependent boss/final-guardian identities and anatomy-supported kits are closed after model acceptance
stale conflicting design text / hidden implementation-time gameplay choices are removed
```

Separate branding gate:

```text
final player-facing title/branding must be locked before branded player-facing release/presentation
internal production slug may remain during gameplay source bootstrap
```

Already-satisfied design conditions that remain runtime validation work rather than blockers:

```text
main quest route/rejoin package exists
R11 aquatic action compatibility is bound at design level
accessibility / difficulty / assist contract is closed
frequent-action input map is closed
subtitles / non-audio cues / camera comfort are closed
audio/music state behavior is closed
```

After source exists, implementation still proceeds iteratively. A document cannot prove combat feel, UI readability, traversal comfort, performance, animation quality, audio mix or multiplayer correctness.

Verification state for this audit:

```text
DESIGN/CANON REVIEWED: YES
EXTERNAL PRODUCTION/BALANCE REFERENCES REVIEWED: YES
R01 ASSET INTAKE PASS 4 INTEGRATED: YES
MAIN QUEST ROUTE PACKAGE: DESIGN CLOSED
R11 AQUATIC DESIGN GATE: CLOSED
ACCESSIBILITY / INPUT / AUDIO BEHAVIOR DESIGN GATE: CLOSED
CODE REVIEWED: N/A — gameplay source does not exist
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
