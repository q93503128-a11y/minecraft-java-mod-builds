# Open-World RPG — Ordinary Equipment Affix Engine Pass 19

> Status: **SHARED ORDINARY AFFIX ROLL PROCEDURE + DATA CATALOG + FAIL-CLOSED MATERIALIZER IMPLEMENTED**
>
> Date: 2026-09-28

## Implemented

The project now has one shared ordinary-equipment affix pipeline for merchant, loot, treasure and forge consumers.

Runtime code owns only the closed roll procedure:

- Standard / Refined / Superior / Exalted use exactly 1 / 2 / 3 / 4 ordinary affixes;
- percentile floors are exactly 60 / 65 / 70 / 75%;
- category weights match LOOT_ECONOMY.md;
- Light / Medium / Heavy armor category overrides match EQUIPMENT_BALANCE.md;
- unavailable categories are filtered before weighted selection;
- eligible identities within the selected category are equal-weight;
- duplicate exact affixes are forbidden;
- at most two affixes may come from one category;
- values use deterministic server-side seeded rolls;
- Primary values round to whole integers;
- Critical Chance / Movement Speed round to 0.1 percentage point;
- other percentage values round to 0.5 percentage point;
- Mythic gear is rejected from this generic pipeline and remains authored.

Item-specific valid-affix pools remain data-owned.

## Data ownership

The bundled ordinary-affix catalog owns:

- the Primary scale curve;
- static canonical affix raw ranges;
- stable affix ids;
- stable runtime-adapter ids;
- whether the effect currently has a live runtime publisher.

The current static catalog contains 29 non-parameterized canonical affixes.

Parameterized entries such as:

- specific weapon-family power;
- specific implemented element/status output;

are not faked as one generic static affix. They require source/item profile data that names the actual family/status.

## Runtime safety

The materializer converts canonical percentage-point values into the runtime fraction used by combat authority.

Example:

```text
+7.5% Physical Power
→ stored rolled value: 7.5 percentage points
→ live combat payload: 0.075
```

A materialization request fails closed when its data-owned eligible pool contains any affix whose gameplay runtime adapter is not implemented.

It does **not** silently remove that affix and renormalize the remaining item pool, because doing so would change the canonical loot probabilities.

Current bundled static catalog state:

```text
STATIC CANONICAL AFFIXES: 29
LIVE RUNTIME-ADAPTED STATIC AFFIXES: 12
```

The live adapters currently cover:

- VIT / END / STR / DEX / INT / WIL;
- Physical Power;
- Magic Power;
- Defense;
- Magic Resistance;
- Guard Strength;
- Poise/Stagger Resistance.

The remaining canonical affixes stay data-known but materialization-gated until their dedicated runtime publishers exist.

## Nessa boundary

Pass 18 already supplies a deterministic per-slot affix seed.

The next Nessa completion step is therefore:

```text
stock base + slot
→ data-owned valid affix pool
→ shared Pass19 roller
→ runtime-complete materialization
→ atomic Gold debit + inventory delivery
→ mark slot SOLD
```

No zero-affix merchant item or probability-altered fallback is permitted.

```text
SHARED ORDINARY AFFIX ROLLER: IMPLEMENTED
DATA-OWNED STATIC AFFIX CATALOG: IMPLEMENTED
RUNTIME AFFIX ADAPTER GATE: IMPLEMENTED
GENERIC ORDINARY EQUIPMENT MATERIALIZER: IMPLEMENTED

R01 ITEM-SPECIFIC VALID AFFIX POOLS: NOT YET BOUND
ALL CANONICAL AFFIX RUNTIME PUBLISHERS: NO
NESSA LIVE EQUIPMENT DELIVERY: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
