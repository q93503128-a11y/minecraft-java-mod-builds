# Alpha.128 — Construction stall recovery

Real play showed a warehouse project remaining at 0% while settlement resources were available. The visible construction worker was elevated on settlement architecture.

## Root cause

Builder spawn/recovery selected the highest walkable collision surface around the settlement. In a dense settlement that can be a roof or log pillar rather than ground. A builder on a disconnected elevated surface can exist successfully, allowing construction to start, but have no descending navigation path to the first grading cell. Placement also deliberately accepts natural trees, while the old grading approach fallback checked only a very narrow neighbor ring.

## Fix

- builder home/spawn recovery accepts natural ground or dirt-path support and searches a bounded 24-block ring;
- an existing builder at least three blocks above natural ground is relocated only when no real path back to safe ground exists; carried ItemStacks are preserved;
- grading uses a bounded three-block terrain-ground-aware approach set;
- construction HUD exposes the immediate stall class instead of only generic `막힘`;
- no force loading, virtual materials, repeated teleport loop, or new save field is introduced.

## Validation

Source/docs regression audits and Java 25 compile run before commit. Canonical build/JAR and companion validation run from committed Alpha.128. Real graphical play remains separately required.
