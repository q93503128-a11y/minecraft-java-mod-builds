# Erden living economy validation

## Authoritative scope

The Erden capital living economy is now driven by the same persistent worksite inventories, household wallets and cargo settlement records used by the runtime. It is not a visual-only simulation.

- 156 persistent economic sites
- 15 warehouses
- 50 shops
- 21 bakeries
- 77 household wallets and daily purchase outcomes
- authoritative cargo escrow from departure to unloading
- aggregate settlement only for unloaded districts

## Shop and household behaviour

- Every shop has deterministic opening and closing times.
- Weekly holidays are distributed across all seven days instead of closing every shop together.
- Bread, goods and household bundle prices react to remaining stock and sustained stockouts within bounded ranges.
- Each household receives one daily outcome: success, closed, stockout or unaffordable.
- Households search alternative shops when the nearest shop is unavailable.
- Loaded dependent residents walk toward the recorded shop during their errand window.
- Interacting with a household resident reports the actual stored result for that day.

## Fresh-world deterministic audit

The validated first-day audit produced:

- deliveries: 291
- crafted or serviced units: 720
- sales: 608 coins
- wages: 308 coins
- successful household purchases: 63
- failed household purchases: 14
- total household outcomes: 77
- preserved bakery reserve: 42 bread across 21 bakeries

A failed purchase is a supported market result, not an audit failure. The required invariant is:

`purchase_successes + purchase_failures = 77`

All recorded failures must use one of the recognized reasons, and the synthetic branch audit must independently prove success, closure, stockout and affordability paths.

## Permanent regression markers

The fresh-world server audit must emit all of the following before it can pass:

- `LK_ERDEN_LIVING_ECONOMY_PASS revision=1 households=77 shops=50`
- `purchase_outcomes=77`
- `schedules=true`
- `holidays=7`
- `dynamic_prices=true`
- `stockouts_persist=true`
- `shopping_routines=true`
- `success_path=true`
- `closed_path=true`
- `stockout_path=true`
- `unaffordable_path=true`
- `LK_ERDEN_PHYSICAL_ECONOMY_PASS sites=156 warehouses=15 wallets=77`
- `LK_ERDEN_TRANSPORT_PASS revision=2`
- `LK_ERDEN_ESCROW_AUDIT_PASS`

The full build additionally checks Java 25 compilation, client startup and UI diagnostics, licensed external assets, JAR contents and cargo/reserve conservation.
