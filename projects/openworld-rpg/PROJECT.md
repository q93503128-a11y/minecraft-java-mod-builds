# Open-World RPG — Project Contract

> Working project slug: `openworld-rpg`
> Final player-facing title: **TBD**
> Current phase: **DESIGN / CANON BUILDING**

## Repository contract

This project follows the repository root `AGENTS.md`, `docs/BUILD_STANDARD.md`, and `docs/QUALITY_STANDARD.md`.
When this file or the design canon conflicts with older chat history, current `main` wins.

## Technical identity

- Slug: `openworld-rpg`
- Mod ID: TBD before source bootstrap
- Namespace: TBD before source bootstrap
- Mod version: not started
- Minecraft: target 26.2 unless design/dependency audit requires a change
- Java: TBD from the selected Fabric toolchain
- Loader: **Fabric — locked for this project**
- Loader version: TBD at M0
- Gradle: TBD at M0
- Build plugin: Fabric Loom / exact version TBD at M0
- Final JAR: TBD
- Existing-world compatibility: no implementation exists yet
- Required dependencies: TBD; prioritize current Fabric 26.2 dependencies that materially improve quality
- Optional external mods: Essential compatibility is a major goal
- Forbidden bundled dependencies: anything whose license/terms do not permit repository redistribution
- Datagen task: TBD
- GameTest task: TBD
- Server smoke-test task: TBD
- Client smoke-test task: TBD

Fabric is no longer a provisional loader candidate. Do not reopen the loader choice during ordinary planning. Re-evaluate only if a hard technical blocker appears that prevents a required canonical feature from being delivered on Fabric 26.2.

## Product identity

This is not a vanilla-plus mod.
The intended product is a large open-world action RPG that uses Minecraft primarily as the world/runtime foundation while replacing most player-facing RPG systems with project-owned systems.

Core pillars:

1. Open-world exploration and discovery
2. Fast action combat with dodge / guard / parry / skills
3. Deep character growth through stats, equipment, classes and repeated class advancement
4. Distinct regions, monsters, bosses and dungeons
5. Strong external-first art/UI/structure direction from the first implementation

## Personal-use scope

The intended gameplay build is for the owner's private play and is not planned for public distribution.
However, this GitHub repository is public. Therefore:

- private-use-only or non-redistributable third-party assets MUST NOT be committed to this public repository;
- the repository may record their source and local installation/import instructions;
- assets/code committed here must still be redistributable under their actual terms;
- no paywall/access-control/DRM bypass is allowed;
- if public distribution is ever planned, all third-party material must be re-audited first.

## Canon files

- `GAME_DESIGN.md` — single gameplay/design source of truth
- `EXTERNAL_SOURCES.md` — external code/assets/UI/map/structure provenance and adoption status
- `PROJECT.md` — technical/build identity and project-wide contracts

Do not create parallel competing design documents. If the design grows, split subordinate reference files only when `GAME_DESIGN.md` explicitly points to them and remains the master index.

## Implementation cleanliness contract

Git history is the archive. The live source tree is not.

After a replacement is verified at the appropriate risk level, remove:

- dead code;
- unused prototype systems;
- superseded managers/handlers;
- duplicate implementations;
- obsolete fallback paths;
- stale test-only registries/resources that are no longer used;
- commented-out old implementations.

Do not delete code that is still required for save migration, compatibility, or an active feature merely because it looks old. Replacement flow is: implement replacement → migrate callers/data if needed → narrow verification → remove superseded code.

## Design cleanliness contract

There is no "temporary ugly UI/model because this is only a test" stage for player-facing content.

From the first player-visible implementation:

- UI/HUD uses selected external final-quality design/reference/assets;
- characters, monsters, bosses, structures, workstations and major props use selected external design/assets/reference;
- no improvised AI black-panel/card/glow UI;
- no placeholder vanilla entity + particles for important enemies;
- no vanilla crafting-screen reskin for major RPG workstations unless the selected external reference genuinely uses that structure.

Technical debug commands/logging may exist during development, but must not become player-facing design.
