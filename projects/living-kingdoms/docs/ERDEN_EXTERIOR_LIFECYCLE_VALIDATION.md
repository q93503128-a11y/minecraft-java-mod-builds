# Erden exterior lifecycle validation

Erden's exterior population is now a persistent multi-generation society rather than a fixed list
of workers that can only shrink. The original workforce save remains authoritative for its 216
founding residents, while a separate lifecycle overlay records age, genealogy, retirement, natural
death, births, household succession and replacement labour.

## Save compatibility

`ErdenExteriorWorkforceSavedData` is not rewritten. Its existing resident IDs, names, households,
node assignments and permanent death records continue to load unchanged. The new
`ErdenExteriorLifecycleSavedData` maps the same founding IDs into a separate genealogy ledger and
stores descendants there. Existing worlds therefore migrate by adding lifecycle data rather than
replacing the old workforce codec.

The lifecycle overlay stores:

- birth day and current age,
- two parent IDs for descendants,
- generation number,
- household and production-node membership,
- current work role, shift and weekly rest day,
- retirement day and death day,
- household steward and designated heir,
- recorded births and completed successions.

## Calendar and life stages

One Erden year is 112 Minecraft days. Residents become eligible for full work at age 18 and retire
at age 58. Founding workers begin between ages 24 and 43, founding children between ages 6 and 13,
and founding elders between ages 62 and 72. Natural mortality begins after age 65 and becomes more
likely at older ages. Death remains permanent and is written to both ledgers for founding residents.

Long-unloaded settlements use bounded catch-up processing. Age is derived from the saved birth day,
so retirement and adulthood still occur correctly even when intermediate days were not loaded.

## Births and households

Births are evaluated at the beginning of each 112-day year. A household must have:

- at least two living adults between ages 20 and 44,
- fewer than five living members,
- no birth in the previous two years,
- at least 65 percent production attendance at its assigned node.

Eligible births are deterministic for the world state and household year. A child receives a unique
persistent ID, unique display name, both parent IDs, the same household and node, and a generation
one higher than its parents. Descendants materialise as persistent villagers whenever their home
chunk is loaded.

## Succession and replacement labour

Each household has one steward and one heir. If the steward dies and a living adult heir exists, the
heir becomes steward. Otherwise the oldest living adult member takes over. The next eligible member
is then designated as heir.

Production sites retain their original required staffing rather than receiving free population.
When a worker dies or retires, the vacancy reduces attendance and production until a living adult
member of that node's households becomes available. The oldest eligible unassigned adult receives
the vacant role, a real shift and a distributed weekly rest day. This replacement worker is then
included exactly once in the same attendance percentage used by farms, ranches, mines, mills,
wharves and the kingdom shipment system.

## Physical routines and interaction

The lifecycle overlay owns movement for residents whose static founding role has changed. Retired
workers return home instead of continuing their old work route, and founding children or descendants
who inherit a vacancy travel to the assigned production node. The old workforce routine skips those
residents, preventing two navigation systems from pulling one villager in opposite directions.

Interacting with an exterior resident reports the saved generation, age, household standing
(steward, heir or member) and current work status. Recorded dead descendants are removed when their
settlement next loads and are never respawned.

## Non-persistent future projection

The CI proof does not advance or rewrite the actual world save. It clones the current people and
household-line lists in memory, then runs the production methods used by normal gameplay over a
20-year projection. The projection must create at least one real birth through `processBirths`,
complete at least one steward succession through `processSuccession`, and assign at least one adult
dependent or descendant to a forced labour vacancy through `fillVacancies`. Failure of any actual
transition suppresses the lifecycle pass marker. The projected people and household lines are then
discarded, leaving the live save on its original day.

## Required regression proof

A fresh-world validation must emit all of the following without a watchdog, invalid block entity,
level-load exception or lifecycle invariant failure:

- `LK_ERDEN_EXTERIOR_LIFECYCLE_PASS`
- `LK_ERDEN_EXTERIOR_WORKFORCE_PASS`
- `LK_ERDEN_EXTERIOR_TICKETS_PASS`
- `LK_ERDEN_KINGDOM_EXTERIOR_PASS`
- `LK_ERDEN_KINGDOM_SUPPLY_PASS`

The lifecycle marker must prove 216 migrated founders, 74 household lines, future adults, scheduled
retirements, succession-capable households, parent-ready households, a 112-day year, adulthood at
18, retirement at 58, births, inheritance, replacement labour, permanent death and save-overlay
migration. Source validation also requires exactly one lifecycle labour contribution in the
workforce production formula.

The permanent workflow is
`.github/workflows/audit-living-kingdoms-exterior-lifecycle.yml`.
