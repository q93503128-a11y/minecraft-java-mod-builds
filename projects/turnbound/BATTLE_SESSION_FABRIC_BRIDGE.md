# TURNBOUND — shared battle session transport (Fabric 1.20.1)

## This checkpoint

The existing generic turn gauge and action resolver now have a **server-authoritative session boundary**. Many players can share one encounter, but each regular ally belongs to exactly one participating UUID. The transport verifies current server world, ownership, active turn, turn revision, and session ID. Replayed or cross-world packets cannot spend Gauge or HP.

The battle network carries a bounded action request, a server-side accept/reject receipt, and an immutable snapshot sent separately to each player. The server omits other participants' skill lists. Client state is cleared on disconnect.

No player can create a session through a packet. A **trusted future encounter service** calls `TurnboundBattleBridge.open(server, worldId, state, owners)` after authoring and party membership checks. There is deliberately no experimental player command, hidden roster, authored monster, automatic overworld trigger, or substitute UI.

The runtime registry is server-lifetime **in-memory only** and bounded to 128 concurrent sessions. It survives a participant's disconnect while the server runs so a reconnecting player can receive their view; it does not persist across a server restart. The definitive dungeon disconnect/rejoin/cooldown behavior remains a separate design/implementation gate.

## What this does not do

- Spawn physical characters or monsters, control encounter aggro or camera, render battle HUD, run enemy AI, or issue gold/loot/XP.
- Persist battle sessions, provide battle pause/resume across restarts or implement dungeon session ownership.
- Inspect or disable Drehmal 2.2.2f original datapack. The official world archive is still not in the runtime.
- Claim actual multiplayer playtesting. Tests verify permissions, replay and state changes in pure Java; Fabric build verifies networking API compatibility.

## Validation plan

Run the existing scoped GitHub `Build TURNBOUND` CI workflow once for this coherent network checkpoint. It executes portable authority tests plus Fabric compile/JUnit/JAR inspection. The prior graphical client boot smoke at commit `4bbade5e` does not prove the new battle network is visually verified.

Next: connect the actual field encounter catalog and a fully owned party state, add server AI turn execution, then test an isolated copy of the original Drehmal map. Do not enable the old Drehmal story or ship third-party map bytes.

## Verification (2026-11-09)
- Commit `ff035f9ae6282e359e530f3c28bbc37ba5df9c0a` → [Build TURNBOUND #1072](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42568147041): `core-logic` PASS and `fabric-build` PASS, JAR produced.
- Old `PROJECT.md` and README NeoForge text is historical and no longer active.
- Neither an end-to-end battle launched by an encounter nor a multiplayer client session has been played. No enemy AI or player-facing battle screen is present yet.

## Server AI progression and anti-stall (2026-11-09)

- A session now advances an **enemy turn** on the server once per tick if the next actor is an enemy. Human turns remain paused pending an authenticated action. No client packet is needed to execute an enemy.
- The fallback enemy policy currently uses **only the authored enemy's basic skill**, validated by the same `GenericBattleResolver` as human commands, and respects taunt and target rules. It has no invented enemy skill rotation or artificial odds.
- Per-tick processing is limited to **one enemy action per battle** to avoid synchronous runaway turns. A successful enemy action bumps the authoritative revision and synchronizes the changed snapshot to all participants.
- Invalid authored enemy basics are treated as errors rather than silently skipped. This is still a **technical foundation**: encounter triggers, data catalogs, model/animation, rewards, UI, enemy kit AI strategy, persistence, recovery and multiplayer playtesting remain open.

## Fault handling (same checkpoint)
- Fabric server ticks now process a stable registry snapshot. If data-defined enemy basics cannot be resolved (invalid targets, unsupported effect, arithmetic), log the actual exception once and **close only that combat session**. Other Minecraft/world sessions continue, and no rewards are issued by this layer. It never performs a fake/no-op attack.
- This fault behavior is part of the low-level foundation, not a substitute for authored encounter validation and in-game error feedback.

## Enemy-turn verification
- [Build TURNBOUND #1075](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42569936565) PASS at `e987fc34fac65bf21346b91d6f91b74cda4462e2`: portable Java battle simulation, Python map scanner fixtures, JUnit, Fabric 1.20.1 Gradle compile/build and JAR structure inspection.
- No actual Minecraft field encounter or player-facing battle UI has been PLAYTESTED. No two-client multiplayer session tested. This check does not supersede the separate original Drehmal world analysis.
