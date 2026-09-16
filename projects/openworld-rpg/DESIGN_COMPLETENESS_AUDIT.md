# Open-World RPG — Design Completeness & Game Quality Audit

> Date: 2026-09-16  
> Status: **CURRENT DESIGN AUDIT / PRE-CODE QUALITY GATE**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Region canon: `REGIONS.md`, `REGION_CROSS_AUDIT.md`, `R01_VERTICAL_SLICE.md`, `R02_CONTENT_BIBLE.md` through `R12_CONTENT_BIBLE.md`  
> Rule: this document reports current closure, detected conflicts and pre-code blockers. It does not override `GAME_DESIGN.md`.

This audit replaces the earlier snapshot that still described the main story, R03–R12 and the ending as mostly unwritten. Those statements are obsolete. Git history is the archive; they are not alternate current plans.

The project is now in **late pre-production**: the reusable gameplay systems and all twelve regional content packages are substantially authored, but gameplay source bootstrap remains blocked by exact presentation, spatial and technical gates. `design written` is not the same as `implementation-ready`, `playtested` or `finished`.

---

# 1. Audit basis

The current active design corpus was cross-read against:

- repository `AGENTS.md`, `docs/BUILD_STANDARD.md`, `docs/QUALITY_STANDARD.md`;
- the Minecraft high-quality playbook;
- `GAME_DESIGN.md`, `PROJECT.md`, `README.md`;
- combat/class/status/equipment/loot/recovery/field-system/mount/UI/quest-state documents;
- `WORLD_STORY_CANON.md`, `REGION_CROSS_AUDIT.md`, `REGIONS.md`;
- R01 vertical-slice/asset work and R02–R12 implementation-package + content-bible pairs.

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
- R01–R12 settlement identities, named casts, regional quest chains, rewards, reconnect rules, story evidence and aftermath;
- server-authority requirements and the rule that real multiplayer testing is still required.

## 2.2 Genuine pre-code blockers

Source bootstrap is still blocked by work that an implementer must not improvise:

1. **final player-facing title / branding string** — the production slug may remain `openworld-rpg`, but a player-visible build must not ship `TBD` or the internal slug as an accidental title;
2. **exact external asset binding** — unresolved boss/creature models, NPC outfits, weapon/item families, structures, Anchor machinery, important VFX, animation, SFX/BGM and exact provenance/hash records;
3. **Azari spatial closure** — actual coordinates, route relationships, sightlines, settlement/POI/dungeon/boss placement, travel times and content-density validation;
4. **R11 aquatic presentation matrix** — every frequent action tagged `AQUATIC_NATIVE`, `AQUATIC_ADAPTED` or `AQUATIC_DISABLED_WITH_FALLBACK`, with accepted locomotion/attack/cast/guard animations;
5. **global presentation/comfort contract** — final key map, accessibility, subtitles/non-audio cues, difficulty/assist behavior and complete music/audio-state coverage;
6. **asset-gated final boss sheets** — exact player-facing names, anatomy-supported attacks/weak points and signature materials after model acceptance where regional documents explicitly gate them;
7. **final stale-document cleanup** — older package wording must not offer obsolete alternatives to later content bibles.

These are pre-code gates, not permission to `decide during coding`.

---

# 3. Major consistency findings

## 3.1 Old completeness snapshot was materially wrong

The previous version of this file still described:

- main narrative as D0–D1;
- final act/ending as D0;
- R03–R12 as mostly broad regional direction;
- whole-project planning around 65–75%.

That was true before `WORLD_STORY_CANON.md`, the later regional packages and R02–R12 content bibles. It is false on current `main` and has been removed.

Current state is better represented as:

```text
system rules: substantially closed
regional/narrative authoring: substantially closed
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

The old `REGIONS.md` still listed R03 Basalt Wyvern/Rocky Roller direction even after later canon moved Basalt Wyvern to R10 and established Griffin + Rock Golem direction for R03. It also still called the region levels `working targets` pending a later EXP pass even though the EXP pass has already happened and retained the region progression.

`REGIONS.md` must therefore be treated as a current index, not a reservoir of old candidates. Its refreshed version removes those conflicts.

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

The global EXP design is internally coherent:

```text
EXP_to_next(L) = round_to_10(100 + 50L + 4L^2)
launch cap = 80
```

The documented mixed-play target of roughly **20–30 hours** to approach Lv80 is plausible because combat is only one contribution source and regional/main/dungeon rewards are authored as percentages of the receiving player's next-level requirement.

Current reward hierarchy is healthy:

```text
ordinary kill: small
exploration/event: small-to-medium
elite/miniboss: meaningful
regional quest: large
first dungeon clear: very large
field/world boss first clear: large
```

Public Guild Wars 2 tables provide a useful structural comparison: successful dynamic events award about 7% of a same-level progression requirement, storyline instances about 27%, and dungeon story/explorable completions much larger. The project intentionally runs somewhat more generous regional/main/dungeon percentages because it targets a finite authored 20–30 hour action-RPG progression rather than an MMO leveling ecosystem.

Audit rule:

- ordinary event/discovery rewards should stay around the existing 5–10% family;
- normal regional/main steps generally stay around 35–45%;
- major regional climax steps may reach roughly 45–55% when they replace several smaller objectives;
- a dungeon first clear + boss may be a substantial level fraction, but repeated farming must fall back to repeat rewards and anti-overlevel rules;
- do not increase every later-region percentage merely because the absolute EXP requirement is larger; the percentage model already scales.

No global EXP curve change is recommended before playtesting.

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
- the publicly documented Van Buren Denver package is an 83-page area design covering places, characters and quests. The project's package+bible pairs are now in the same **kind** of production-document territory: named people, exact objective conditions, rewards, state and aftermath exist. However the project is still behind a true production-ready area package in one important respect: **actual Azari spatial placement, sightlines, travel time and accepted final assets are not yet bound.**

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

Required pre-code exit condition:

```text
all active player-facing rules have one current authority
no unresolved gameplay-affecting TBD / decide-later option
all gated visible content has an accepted external source direction/binding
Azari major content has spatial coordinates + route/sightline/travel targets
R11 aquatic action compatibility is bound
accessibility / input / audio global contracts are closed
stale conflicting design text is removed or explicitly historical
```

After source exists, implementation still proceeds iteratively. A document cannot prove combat feel, UI readability, traversal comfort, performance or multiplayer correctness.

Verification state for this audit:

```text
DESIGN/CANON REVIEWED: YES
EXTERNAL PRODUCTION/BALANCE REFERENCES REVIEWED: YES
CODE REVIEWED: N/A — gameplay source does not exist
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
