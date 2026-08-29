# TURNBOUND Project Contract

## Identity
- Path: `projects/turnbound/`
- Mod ID: `turnbound`
- Display name: `TURNBOUND`
- Package: `io.github.q93503128.turnbound`
- Version: `0.1.0-alpha.4`

## Toolchain
- Minecraft Java 26.2
- NeoForge 26.2.0.62 stable artifact
- Java 25
- Gradle 9.2.1
- ModDevGradle 2.0.143
- UI Lib 21.1.1 is retained as an optional client-side UI dependency for later interface phases.
- GeckoLib 5.5.3 is retained for the later character model/animation phase.

## First playable P0 acceptance
- Turn threshold 1000.
- Gauge overflow survives an action.
- Natural consecutive actions are allowed without an arbitrary count cap.
- Decision time does not advance logical combat time.
- Cooldowns tick on the owner’s later regular turns.
- Basic actions may be non-damaging.
- 1–4 allies / 1–5 enemies are supported; the playable test scenario is 4 allies vs 5 enemies.
- Damage, heal, barrier, gauge manipulation, death, revive, redirect, counter and simple status effects are represented.
- P01–P04 core kits are playable.
- Manual skill selection and manual target selection work through a client battle screen.
- Skill slots 1–5 and mouse clicks share the same local selection path.
- Tab/Shift+Tab cycle only valid targets and Enter commits the highlighted target.
- Esc cancels local skill/target selection and must never leave a live server battle hidden behind a closed client screen.
- Enemy turns are server-authoritative; AUTO uses deterministic role-aware target priorities.
- Battle speed x1/x2 changes presentation delay only, not logical outcomes.
- Battle movement and ordinary world interactions are locked while combat is live.
- P0 exposes no normal mid-battle flee button; `/turnbound leave` remains the test/emergency cleanup path.
- A result state is shown after victory/defeat and can explicitly return the player to the field.
- Deterministic timeline preview does not mutate battle state.
- `/turnbound p0` runs the deterministic diagnostic simulation.
- `/turnbound battle` starts the playable P0 battle session.
- Battle widgets and decorative panels must keep positive, in-viewport bounds at high GUI scales and low logical resolutions.
- Java 25 clean test/build must be green before a test JAR is handed off.

## P0 presentation boundary
- The battle scene uses temporary ArmorStand actors solely as 3D placement/motion stand-ins.
- Temporary actors already occupy the intended 4v5 battlefield positions and perform short lunge/return motions.
- The battle UI uses vendored Kenney CC0 RPG UI sprites instead of an improvised Minecraft-grey-box skin.
- The P0 battle UI uses responsive target grids and action widths when the logical GUI viewport is small; it must not place the fifth enemy or any action widget below/outside the render viewport.
- The world remains visible behind the battle UI; arrow keys can adjust view yaw/pitch while the mouse is reserved for battle UI selection.
- Aggressive per-action camera following is intentionally deferred; P0 keeps a stable overview while interaction and pacing are validated.
- Final character meshes, rigs, animation clips, VFX and polished camera direction belong to the next character/presentation phase and will replace the ArmorStand stand-ins.

## Reference boundary
- `REFERENCE_STUDY_ALPHA4.md` records R_PG/R_PG X, TurnBasedMC, Soulbound, Cobblemon and Craftics findings.
- Proprietary game UIs/assets are reference-only.
- Open-source references inform architecture and UX but are not wholesale transplanted.

## UI/asset rule
Do not ship an improvised Minecraft-grey-box UI as the final interface. Prefer licensed/open external assets registered in `EXTERNAL_ASSETS.md`; proprietary game UIs are reference-only.
