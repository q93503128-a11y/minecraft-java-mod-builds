# TURNBOUND — Summon Gacha and Currency Reward Design

## Confirmed native draw probabilities and flexible initial roster size — 2026-11-06
**Latest user decision, overriding earlier unresolved odds and provisional market comparisons:**

| Native rarity | Base chance per non-guaranteed draw |
| --- | ---: |
| ★1 | 15% |
| ★2 | 25% |
| ★3 | 40% |
| ★4 | 15% |
| ★5 | 5% |
| **Total** | **100%** |

- These are the **base-rarity odds before applying guarantees**. The 10-pull guarantees at least one ★4+ in that batch; cumulative individual draws #50, #100, #150, ... guarantee a ★5 regardless of earlier lucky ★5 outcomes. The exact server draw/guarantee algorithm must preserve the promised minimum and not downgrade a naturally or milestone rolled ★5.
- The initial **overall character roster size and the number per rarity are intentionally NOT fixed**. Character identities, designs, role balance, assets, skills, and available pool counts should evolve with content production and playtest. Do not manufacture filler units merely to fill an artificial quota.
- The first-start selection remains exactly one from **three ordinary ★3 candidates**, all available from the normal summon pool. This fixed starter-choice count does NOT set the total number of ★3 characters.
- Low rarity must have purposeful roles and an attainable promotion path to growth rank ★6; native ★5 should offer appealing identities without making low-rarity strategic choices irrelevant. Exact individual kits and raw stat/power budgets remain to be designed.
- Main summon resources principally come from optional contracts and dungeons. **Exact summon cost and currency pacing are STILL undecided.**
- This is a **planning-only decision**. GachaService is not connected to the rebuilt empty-runtime playable content; no code/build or gameplay tests were run.


Date: 2026-11-06
Status: Confirmed guarantees and primary reward sources; other details provisional / NOT IMPLEMENTED
Branch: `turnbound-content-rebuild`
Project: `projects/turnbound/` (NOT TURNBOUND: RE)

## Confirmed summon guarantee and reward sources — 2026-11-06
- **Every 10-pull batch guarantees at least one native ★4-or-★5 character.** The batch may contain more high-rarity results naturally. A ★5 counts toward this guaranteed ★4+ slot; do not silently promise an additional ★4 on top of a ★5.
- **Every 50th individual draw is guaranteed native ★5**: 50, 100, 150, 200, ...; this is a **fixed milestone** guarantee, not a conventional pity counter that resets after obtaining ★5.
- **An early ★5 never resets or advances the fixed 50-draw schedule**. Example: ★5 on draw 27 still guarantees draw 50; ★5 on draw 49 still guarantees draw 50. Within a 10-pull, a scheduled guaranteed ★5 also satisfies the ★4+ batch guarantee.
- Both single and batch pulls contribute their actual individual draws to the same cumulative milestone count. The batch-specific ★4+ guarantee is explicitly associated with the **10-pull action**; no separate ten-consecutive-single-pulls ★4+ promise has been authorized.
- Gacha's possible native ranks remain **★1–★5**; rank ★6 exists only through character promotion. Initial starter choice is one of three ordinary native ★3 characters in the normal obtainable roster.
- **Summon currency comes principally from NPC/job-board contracts and dungeons.** Elite/boss first-clears and substantial discoveries are viable proposed secondary sources; exact payout amounts and whether repeat-clears award summon currency remain undecided.
- Prior frozen legacy 65-soft/80-hard pity and any ★5-on-early-draw reset logic **DO NOT apply** to this new approved scheme. Do not copy old numerical summon odds or 300-crystal costs without a separate explicit balance decision.
- Retain earlier duplicate character handling: reward choice of a +1 duplicate bonus level (up to +10) OR native-star essence, unless later explicitly changed.
- Still unapproved: native-rarity probability weights, exact summons cost, currency/item names, how/where the UI opens, cross-pool/banner account rules if multiple pools ever exist, any additional ★4 guarantee on singles, and precise repeatable reward anti-grind policy.
- Save the per-player cumulative draw count and enforce reward settlement server-side when implementing. Never award guaranteed draws twice on interrupted transactions. These are design/implementation criteria, **not evidence that summons are live**.
- This is **documentation only**, not new summon code, resource drops, tests, server runtime, JAR or playtest. Full draft: `SUMMON_GACHA_AND_REWARD_ECONOMY_DRAFT.md`.

## Guaranteed outcomes: exact examples

| Draw context | Correct outcome |
| --- | --- |
| 10-pull with no naturally rolled ★4 or ★5 | Upgrade/force at least one slot to ★4 or better by a server-authoritative deterministic guarantee policy |
| 10-pull already containing a ★4 or ★5 | Batch guarantee is satisfied, no second guaranteed high-rarity character owed |
| Single draw #27 happens to yield ★5 | Draw #50 remains guaranteed ★5 |
| Single draw #49 yields ★5 | Draw #50 still yields guaranteed ★5 |
| Draw #50 falls in a 10-pull | That draw yields ★5 and also fulfills that 10-pull's ★4+ condition |
| Draw #100, #150, #200, etc. | Each guaranteed native ★5 independent of lucky ★5 results between milestones |

The particular slot-selection and RNG-call procedure must not let the ten-pull guarantee rewrite/downgrade a scheduled ★5 at positions 50k. A single batch can naturally include multiple ★5 results. Use one server-authoritative state machine for draw sequencing so client display does not determine results.

## Reward economy: agreed and suggested sources

**User-approved primary sources**
- **Contracts / job board:** hunting, surveying, dungeon commissions and meaningful rotating *in-world objectives* (not daily-login chores). Exact job types, completion awards and repeatability still require detailed design.
- **Dungeons:** meaningful clear rewards; design first clear separately from repeat clears to protect replay value without making one corridor the optimal gacha printer.

**Other candidate sources (NOT user-approved payout policy)**
- First defeat of elites, dungeon guardians and world bosses.
- Discovery of notable structures or hidden exploration rewards.
- Optional achievement/milestone awards that encourage varied survival play.

Do not assume that mining plain ore itself yields summon currency. Mining is already important to the equipment forge. Retain coherent reasons to engage with contracts and exploration rather than flooding every mundane interaction with summon resources.

**Balance principles to test**
- Earned in-game summon resources, with no monetization presumed.
- Respect survival time: avoid compulsory repeat-grinding of one low-level dungeon just to fill a mandatory roster.
- First-clear rewards and meaningful contracts can have a higher value than repetitive trivial mob hunting.
- A new player can start with the already-approved free ordinary ★3 choice; the time to fill the four slots through summon rewards is a design target yet to be set.
- Costs per draw and probability weights will be set against measured contract/dungeon duration rather than copied uncritically from the frozen old game.

## State and save requirements for eventual implementation

- Server-owned monotonically increasing `totalIndividualDraws` count for the appropriate summon pool scope. Milestone ★5 when `totalIndividualDraws % 50 == 0`. **No reset on ★5**, regardless of lucky or guaranteed origin.
- Process each 10-pull as ten ordered draw events so milestones are precisely the correct individual positions. Enforce at least one ★4+ outcome by the end of that batch and avoid displacing existing fixed ★5 milestones.
- Record acquired characters, duplicate pending choices, cost transactions and awarded currency atomically or with idempotent recovery, covering logout, reconnect and accidental request retry.
- Multiple rotating banners/limited pools are **not authorized**; if ever introduced, clarify whether counts are account-wide or separate before implementation. The design currently assumes one primary normal draw sequence.
- Avoid conflating immutable native ★ rarity with current growth/ascended ★ rank, including the promotion-only ★6 rank.

## Historical contrast (NOT current binding design)

Frozen `turnbound-alpha17-full-v04:projects/turnbound/02_BALANCE_RULES_v1.md` had 10-pull ★4+ guarantee, ★5 probability baseline 3%, soft pity at 65 and hard pity at 80, with historical 300-crystal draw costs. **The new game only inherits the 10-pull ★4+ principle, not those old rates or the old pity logic.**

The latest user decision supersedes any old ★5 pity-reset behavior. This file contains decisions and examples only: no summon transaction, persistence migration, server test, loot table, UI or playable game content has been implemented.

## External rate benchmarks reviewed — 2026-11-06
- See `GACHA_RATES_MARKET_COMPARISON_2026-11-06.md` for cited comparisons (Genshin, Star Rail, Wuthering Waves, Arknights, NIKKE, Azur Lane, AFK Journey), fixed 50-draw scenario arithmetic and rate design tradeoffs.
- **SUPSERSEDED BY CURRENT APPROVAL ABOVE:** The earlier market study preceded the user's explicit acceptance of base odds ★1/★2/★3/★4/★5 = 15/25/40/15/5. The 50-draw non-resetting milestone and 10-pull ★4+ guarantee remain unchanged.
