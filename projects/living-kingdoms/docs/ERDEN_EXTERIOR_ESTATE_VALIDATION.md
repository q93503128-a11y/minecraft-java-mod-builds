# Erden Exterior Household Estate Validation

## Authority boundary

The exterior household estate system is a third save-compatible overlay. It does not rewrite the
existing workforce or genealogy codecs. The original 74 household IDs and their authoritative home
coordinates remain the source of truth, while `erden_exterior_estates` stores ownership,
maintenance, condition, occupancy and inheritance state.

## Persistent estate state

Each of the 74 exterior households owns one persistent home record containing:

- authoritative household, settlement and home coordinates;
- current steward and named heir from the genealogy ledger;
- living occupancy and a five-person capacity;
- maintenance reserve and physical condition from 0 to 100;
- vacancy and overcrowding state;
- succession counters proving that ownership transferred without creating a replacement house;
- the last processed day for deterministic catch-up.

A succession changes the steward and heir only. The home coordinates, accumulated reserve and
condition remain attached to the estate, so inheritance cannot duplicate property or reset debt and
maintenance consequences.

## Daily maintenance

Active workers and the settlement's attendance-driven production create household maintenance
income. Occupied homes pay an obligation based on household size. Vacant homes deteriorate faster,
underfunded homes lose condition, and overcrowding adds wear. Properly funded occupied homes recover
slowly. All values are clamped and persisted.

## Birth and interaction integration

Normal yearly births require a real occupied home that is not overcrowded, has condition at least
45, and retains at least two maintenance units. The non-persistent CI future projection bypasses
this live estate gate only to exercise the demographic transition algorithms without rewriting the
world save.

Interacting with a lifecycle resident reports their age, generation, household standing and work,
then appends the authoritative home condition, reserve and occupancy state.

## Required regression proof

The permanent estate audit must compile under Java 25 and create a fresh realm proving:

- exactly 74 estates linked to exactly 74 workforce home coordinates;
- exactly 74 steward/heir pairs linked to the genealogy ledger;
- positive occupied homes, maintenance reserves and minimum condition;
- persistent ownership and preservation of home and reserve through inheritance;
- live birth gating by vacancy, overcrowding, condition and reserve;
- successful 20-year non-persistent birth, succession and replacement-labour projection;
- the existing 104 exterior chunks, 18 storage yards, workforce and supply-chain proofs;
- no watchdog, invalid block entity, estate reconciliation or lifecycle errors.
