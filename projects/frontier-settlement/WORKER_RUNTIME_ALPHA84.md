# Frontier Settlement 0.1.0-alpha.84 — independent worker runtime

Alpha.84 is a runtime hotfix driven by graphical playtest evidence: a first HOUSE could remain at
`부지 정리 0%` indefinitely while wood/stone never began physical staging.

## Root cause and correction

All civilian work bodies were real vanilla `Villager` entities. Frontier issued navigation orders,
but the vanilla villager Brain/POI/schedule system remained another movement authority. Grading also
required the builder to reach a tight per-cell position and had no alternate path target.

Alpha.84 registers `frontier_settlement:frontier_worker`, a `PathfinderMob` with no autonomous goals.
Frontier services are now the only server-side movement/work authority. The client renderer reuses the
vanilla villager model and base texture only. It does not attach profession rendering or create a
server-side Villager.

Loaded pre-Alpha.84 Frontier-managed villagers are migrated one-way only when they carry Frontier tags
or known Frontier worker names. Their position, name, tags and equipment/cargo are copied before the old
entity is discarded. Ordinary Minecraft villagers are excluded.

Construction grading uses a wider visible work reach and alternate nearby walkable path targets; there
is still no teleport or force-load fallback. Building wood/stone remains physically authoritative and
starts staging after grading, while retaining stone is consumed during grading only when actually needed.

## Acceptance

Automated audits/build/JAR checks are not graphical acceptance. In game, verify a fresh settlement can
start a HOUSE, progress grading above 0%, stage real wood/stone, complete the house, and show a villager-
shaped worker that has no vanilla profession/trade/POI behaviour.

## Graphical playtest follow-up audit

The same pass also fixes three adjacent failure modes: a project now rolls back instead of silently
starting when no authoritative builder can be secured; builder absence proof ignores completed remote
infrastructure and covers only town/storage plus the active project; and HUD/Jade progress includes
grading cells instead of reporting a hard-coded 0% until grading ends.
