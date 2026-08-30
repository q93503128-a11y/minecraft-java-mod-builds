# Frontier Settlement Alpha.87 — production logistics and finalization

Version: `0.1.0-alpha.87`

Alpha.87 is a save-compatible runtime pass over Alpha.86. It adds no required settlement SavedData field.

## Production logistics
- Lumber, farm, quarry, and mine workers keep real output in MAINHAND and can accumulate one compatible 64-item stack before an ordinary storage return.
- Lumber search expands from 18 to 48 blocks, quarry search from 14 to 40 blocks, and mine search expands to 24 blocks horizontally / 48 blocks deep.
- Search is loaded-only. Frontier never force-loads a remote resource chunk and never teleports a worker.
- Tree and quarry search uses expanding rings so nearby targets are selected without always scanning the maximum radius first.
- The farm blueprint already starts planted with wheat. Harvest returns mature wheat to age 0, and Alpha.87 additionally restores an AIR crop cell over the farm's own FARMLAND to age-0 wheat. Players do not need to supply seeds.

## Population and ordinary civilian cost
- Founding starts at population 1.
- A HOUSE adds 4 housing capacity; capacity is a ceiling, not an instant resident spawn.
- Once every 600 server ticks (30 seconds), at most one vacant loaded worker assignment may attract a resident if population is below housing capacity.
- A successful ordinary arrival consumes 4 real food exactly once. Failed entity insertion does not charge food or population.
- There is no periodic ordinary-civilian food/tax/upkeep drain in Alpha.87.

## 99% construction recovery
Alpha.86 validated the finished blueprint and then required the shared builder to reach the town anchor before completeConstruction. A path failure could leave a physically complete building at 99% forever.

Alpha.87 makes structural validation the completion authority. Scaffolds are cleaned locally, exact leftover physical cargo remains in the site barrel or builder hand, an empty site barrel is removed, the building commits immediately, and builder return becomes a best-effort physical navigation order. Active progress can display 100% during final cleanup rather than being capped at 99%.

A pre-existing Alpha.86 save already stuck at 99% should finalize after loading Alpha.87 as soon as the completed site is loaded and validates.

## Update boundary
This is a same-world update, but not a JVM hot reload. Fully close Minecraft before replacing Alpha.86 / installing Alpha.87. Back up the world before its first Alpha.87 launch.
