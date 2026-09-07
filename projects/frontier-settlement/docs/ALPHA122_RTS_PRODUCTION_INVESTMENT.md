# Frontier Settlement Alpha.122 — RTS Production Investment Canon

This document locks the production-investment direction introduced in `0.1.0-alpha.122` so later work does not drift back to free tier-wide production upgrades.

## Core rule

Settlement tier is an unlock ceiling, not a free production buff. Each completed lumber camp, farm, quarry and mine owns a persistent improvement grade and must be improved individually with real settlement resources.

- CAMP / HAMLET: grade I
- VILLAGE: grade II ceiling
- FRONTIER_TOWN: grade III ceiling
- DOMAIN / FRONTIER_CAPITAL: grade IV ceiling

New production buildings start at grade I. Pre-Alpha.122 production buildings inherit the grade that the old tier-derived system was already granting when the save first migrates, then become persistent individual facilities. Paid grades are not removed if a settlement later temporarily drops a tier condition.

## Physical investment costs

| Upgrade | Wood | Stone | Copper / iron items |
| --- | ---: | ---: | ---: |
| I → II | 128 | 96 | 0 |
| II → III | 256 | 192 | 32 |
| III → IV | 512 | 384 | 96 |

Facility improvement must not silently consume diamonds, gold, rare modded metals, or value-overpay substitutes. Grade III/IV metal payment is exact physical copper/iron item count. Wood, stone and copper/iron payment is prechecked and then removed in one server-thread transaction.

## Player interaction

A player upgrades a completed production facility by sneak-right-clicking one of that facility's worksite barrels with an empty main hand. The interaction reports the required settlement tier, cost, missing resources, blocked extra-buffer space, or successful improvement.

No new currency, virtual upgrade token, or separate debug menu is authoritative.

## Local buffer vs warehouse authority

Local production storage scales with the individual facility grade:

- grade I: 1 worksite barrel
- grade II: 2 worksite barrels
- grade III: 3 worksite barrels
- grade IV: 3 worksite barrels

Only the unlocked/paid local barrels participate in normal production hauling and the shared settlement resource ledger. Manually placing a barrel in a future locked buffer position must not bypass the investment grade.

The local buffers are short-term production staging. Warehouse/public supply storage remains the settlement's durable central logistics authority and is the target of a later RTS logistics-growth pass.

## Compatibility / safety rules

- Existing physical production, crop ecology, tree replanting, finite quarry/mine consumption, cargo recovery and no-force-load rules remain authoritative.
- Facility improvement never mints resources or items.
- Existing saves migrate once without losing the production grade they already had before Alpha.122.
- Improvement is per building, not per profession globally and not per settlement globally.
- UI/context may display grade and next cost, but presentation is not simulation authority.

## Next RTS passes

Alpha.122 is not the final RTS economy pass. The remaining major work is intentionally grouped into:

1. warehouse / logistics growth and storage-pressure progression,
2. late-game repeatable resource sinks plus military / territorial economy,
3. integrated balancing, management UX and real-play regression cleanup.
