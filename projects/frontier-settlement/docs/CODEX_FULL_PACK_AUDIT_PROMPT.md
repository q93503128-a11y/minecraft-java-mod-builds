# Codex Audit Prompt — Frontier Settlement + Survival Ascension FULL

Use this only after the FULL KO + JEI build has completed and its final MRPack/artifact is available.

Repository: `https://github.com/q93503128-a11y/minecraft-java-mod-builds`
Branch: `main`

Primary projects:
- `projects/frontier-settlement/`
- `projects/survival-ascension/`

Target runtime:
- Minecraft Java 26.2
- NeoForge 26.2.0.38-beta
- Java 25

Current integration baseline:
- Frontier Settlement 0.1.0-alpha.91
- Survival Ascension 0.61.0-alpha.1
- Frontier menu: M
- Survival Ascension menu: K
- FULL new-world companion/content stack
- Korean overlays for the pinned FULL client stack
- JEI 30.13.0.77 client-side recipe viewer

## Mission

Perform a repository-wide integration audit and fix real defects. This is not a feature-development pass. Do not redesign systems or delete features to make tests pass.

Start by fetching current `main` and verifying the actual HEAD. Never assume the SHA or versions in this document are still current.

Then audit, reproduce where possible, fix, rebuild, verify JARs, run dedicated-server smoke tests, and commit validated fixes directly to `main`. Do not create a PR or temporary branch.

## 1. Frontier Settlement audit

Deep-audit Alpha.91 navigation, worker and storage behavior:
- lumber search radius 128
- quarry search radius 96
- mine horizontal 48 / depth 80
- complete-path acceptance
- water avoidance
- stuck-target cooldown/blacklist
- bounded search/caching
- rotated farm coordinate handling
- profession worksite barrels
- four-barrel public stockpile
- full-storage handling

Look for:
- unloaded-chunk access
- repeated expensive pathfinding
- per-tick large scans
- permanent blacklist deadlocks
- fence/wall/water infinite retries
- item duplication/loss
- cross-profession storage contamination
- duplicate accounting between profession/public storage
- worker cargo loss when every destination is full
- rotation bugs for every building facing
- player block overwrite
- stale target state after reload
- force-loading or teleport-based shortcuts

## 2. Survival Ascension audit

Audit all progression/runtime systems, including:
- skills/levels
- equipment/affixes
- combat rewards
- expeditions
- incidents
- bosses/apex encounters
- freight/logistics
- civil works
- fortification
- final ascension
- optional external-content integration

Look for:
- duplicate XP/rewards
- projectile-owner attribution bugs
- duplicated death handlers
- boss lifecycle persistence failures
- restart/reconnect state corruption
- attribute modifier collisions
- level/XP overflow
- item destruction or duplication during imprint/upgrade flows
- external weapons receiving invalid affixes
- Survival logistics touching Frontier-managed barrels/containers
- client-authoritative outcomes

## 3. Cross-mod integration

Audit simultaneous Frontier + Survival behavior:
- key mappings
- HUD overlap
- GUI/input routing
- attack/damage events
- Better Combat interaction
- attack speed and movement modifiers
- death/loot events
- storage/logistics ownership
- block break/place protection
- commands
- save data
- dimension changes
- reconnect/logout
- multiplayer-safe server authority

Do not introduce a shared global ledger or virtualize physical ItemStack logistics.

## 4. FULL third-party stack

Read the actual pinned manifests/locks and audit integrations with the versions currently used by the FULL pack.

Important stack includes Terralith, Biomes O' Plenty, TerraBlender, GlitchCore, Dungeons & Taverns, Repurposed Structures, The Birth of Steve, Amethyst Resonance, Better Combat, Weapons Expanded, Lootr, Sophisticated Backpacks/Core, Variants & Ventures, Jade, Xaero's Minimap, JEI and their pinned libraries.

Check:
- hard references to optional mods
- missing-class behavior
- registry/tag assumptions
- worldgen assumptions
- external entity classification
- Lootr/container misclassification
- Sophisticated storage ownership conflicts
- Better Combat damage/attack-speed duplication
- JEI client-only safety

`ClassNotFoundException` text alone is NOT sufficient to mark a run failed. Optional compatibility probes can emit it and continue. Classify severity using stack context, mod loading outcome, server readiness, actual world save, and subsequent errors.

## 5. Korean localization audit

Compare every `en_us.json` and effective `ko_kr.json` for the pinned FULL stack.

Requirements:
- no missing English localization key in the targeted FULL namespaces
- no obviously untranslated ordinary English UI string where a Korean translation should exist
- preserve `%s`, `%1$s`, MessageFormat/ICU fields, formatting codes and other substitution syntax exactly
- verify Dungeons & Taverns' malformed upstream Korean resource is safely superseded by the project overlay
- verify screenshot-visible Sophisticated Backpacks Mob Catcher strings are Korean
- do not translate technical identifiers that must remain identifiers

Flag awkward or semantically wrong machine translations and fix important gameplay/UI terminology manually.

## 6. JEI recipe visibility

Verify JEI 30.13.0.77 is client-only in the MRPack and works with the installed stack.

Audit recipe visibility for craftable items, especially Sophisticated Backpacks upgrades such as Mob Catcher and Advanced Mob Catcher. Check that normal craftable recipes appear and that intentionally non-craftable/admin-only content is not misrepresented as craftable.

Check standard JEI recipe/usage interactions and conflicts with other key mappings.

## 7. Client initialization NPE investigation

Investigate this observed client log:

`java.lang.NullPointerException: Cannot invoke "com.mojang.blaze3d.platform.FramerateLimitTracker.onInputReceived()" because the return value of "net.minecraft.client.Minecraft.getFramerateLimitTracker()" is null`

Stack path includes:
- `Minecraft 26.2 KeyboardHandler.keyPress`
- `DisplayWindow.periodicTick`
- `ModLoader.waitForFuture`
- `ClientModLoader.finish`
- `Minecraft.<init>`

Determine whether this is:
- vanilla/NeoForge early-loading keyboard-input race
- triggered or amplified by one of the installed mods
- caused by a project client initializer/key-registration path

Do not add a Minecraft keyboard mixin just to suppress the stack trace. Reproduce first, identify ownership, and only patch project code if project code is actually causal.

Test at least:
- launch with no keyboard input during loading
- keyboard input during mod-loading screen
- post-main-menu key input
- repeated launches if feasible

## 8. Performance

Inventory tick-driven services in both project mods. Find expensive patterns such as:
- all-entity scans per tick
- large BlockPos scans per tick
- nested worker × building scans
- repeated path generation
- registry iteration in hot paths
- unnecessary save dirtying
- excessive network snapshots

Fix with bounded scans, caching, staggering, dirty tracking or ownership indexes where appropriate. Do not hide structural problems merely by increasing tick intervals.

## 9. Validation

After fixes, run all project-provided source audits/tests and then clean builds with Java 25.

Frontier:
- Alpha.91 source tests/audits
- `./gradlew clean build --no-daemon`
- Frontier JAR verifier

Survival Ascension:
- current source audits
- `./gradlew clean build --no-daemon`
- Survival JAR verifier

Then test the two built JARs together with the FULL server-side pinned stack in a fresh dedicated-server world.

Validate:
- mod discovery
- datapack/registry loading
- fresh world creation
- ready state
- clean stop/save
- no actual server tick crash
- no fatal mod loading error
- no fatal datapack/registry failure

Where feasible also perform client-side launch/resource validation for Korean resources, JEI, key bindings and the reported FramerateLimitTracker NPE.

## 10. Fixing rules

- fix root causes, not log strings
- no blanket regex mass replacement
- no feature removal to make CI green
- no teleport/force-load shortcuts
- no virtual-resource ledger replacing physical logistics
- keep optional integrations optional
- keep server authority
- preserve existing gameplay direction
- do not copy third-party source/assets into project code unless licensing and necessity are explicitly verified

## 11. Final report

Report:
1. current HEAD before work
2. all defects found, grouped by severity
3. fixes applied
4. warnings intentionally left and why
5. Korean coverage result
6. JEI/recipe result
7. FramerateLimitTracker NPE conclusion
8. performance findings
9. Frontier clean-build/JAR-verify result
10. Survival clean-build/JAR-verify result
11. FULL dedicated-server smoke result
12. client-side checks that were/weren't possible
13. files changed
14. final commit SHA
15. remaining manual in-game test checklist

If a real defect is found, do not stop at reporting it: fix it, rerun the relevant tests, and only then finalize the report.
