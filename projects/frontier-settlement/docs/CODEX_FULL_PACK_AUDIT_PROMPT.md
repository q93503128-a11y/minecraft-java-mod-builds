# Codex Audit Prompt — Frontier Settlement + Survival Ascension FULL

Repository: `https://github.com/q93503128-a11y/minecraft-java-mod-builds`
Branch: `main`

Primary scope only:
- `projects/frontier-settlement/`
- `projects/survival-ascension/`
- the FULL-pack build/localization workflows that directly serve those two projects

Do not modify unrelated projects in this monorepo.

Target runtime:
- Minecraft Java 26.2
- NeoForge 26.2.0.38-beta
- Java 25

Expected integration baseline at the start of this audit:
- Frontier Settlement 0.1.0-alpha.91
- Survival Ascension 0.61.0-alpha.1
- Frontier menu key: M
- Survival Ascension menu key: K in the combined compatibility build
- FULL new-world companion/content stack
- Korean overlays for the pinned FULL client stack
- JEI 30.13.0.77 as a client-only recipe/usage viewer

## 0. Start correctly

This is not a new project and not a redesign pass.

First:
1. fetch current `main` and record the real HEAD
2. read the current source, locks, manifests, verifier scripts and relevant workflows
3. identify the newest successful `Build Frontier Survival Full KO JEI New World Pack` run and inspect its artifact/logs if available
4. do not trust version/SHA notes in this document if the repository has moved
5. preserve concurrent unrelated main changes; before any final push, fetch/rebase latest `main` and never overwrite unrelated work

The mission is: find real integration defects, fix them at the root, rebuild, verify, smoke-test, and commit only validated fixes directly to `main`.

Do not create a PR or temporary branch unless the repository tooling makes direct-main repair impossible. Do not stop after merely listing defects.

## 1. Frontier Settlement Alpha.91 deep audit

Audit the actual implementation of:
- lumber resource search radius 128
- quarry resource search radius 96
- mine horizontal radius 48 / depth 80
- complete-path acceptance
- water avoidance
- stuck-target cooldown/blacklist
- bounded search and target caching
- rotated farm coordinate handling
- profession worksite barrels
- four-barrel public stockpile
- full-storage handling

Look specifically for:
- unloaded-chunk reads/writes or accidental chunk force-load
- repeated expensive path generation
- large BlockPos scans in hot tick paths
- permanent blacklist deadlocks
- fence/wall/water infinite retry loops
- target cache becoming stale after block changes or reload
- workers losing cargo when all destinations are full
- ItemStack duplication or deletion
- cross-profession barrel contamination
- the same inventory being counted twice by profession/public logistics
- rotation bugs for every building facing
- player blocks being overwritten by managed storage placement
- teleport or virtual-resource shortcuts that bypass physical logistics

Do not weaken the physical worker/ItemStack design to make tests easier.

## 2. Survival Ascension deep audit

Audit the real runtime path of:
- skills and levels
- equipment and affixes
- combat rewards
- expeditions
- incidents
- bosses/apex encounters
- freight/logistics
- civil works
- fortification
- final ascension
- optional external-content integrations

Look for:
- duplicate XP/reward delivery from multiple events
- projectile owner attribution bugs
- duplicated death handlers
- boss lifecycle persistence failures
- restart/reconnect state corruption
- attribute modifier collisions
- level/XP overflow
- item destruction or duplication during imprint/upgrade flows
- invalid affixes on external weapons
- client-authoritative outcomes
- Survival logistics touching containers owned/managed by Frontier

## 3. Frontier + Survival cross-mod audit

Run both project mods together and inspect:
- key mappings
- HUD overlap
- GUI/input routing
- attack/damage events
- Better Combat interaction
- attack speed and movement modifiers
- death/loot event duplication
- storage/logistics ownership
- block break/place protection
- commands
- save data
- dimension changes
- logout/reconnect
- multiplayer-safe server authority

Do not introduce a shared virtual ledger for resources. Physical ItemStacks and server authority remain the source of truth.

## 4. FULL third-party stack audit

Read the actual current pinned manifests/locks instead of assuming versions.

The important FULL stack includes Terralith, Biomes O' Plenty, TerraBlender, GlitchCore, Dungeons & Taverns, Repurposed Structures, The Birth of Steve, Amethyst Resonance, Better Combat, Weapons Expanded, Lootr, Sophisticated Backpacks/Core, Variants & Ventures, Jade, Xaero's Minimap, JEI and their pinned libraries.

Check:
- hard class references to optional mods
- missing-class behavior
- registry/tag assumptions
- worldgen assumptions
- external entity classification
- Lootr/container misclassification
- Sophisticated storage ownership conflicts
- Better Combat damage or attack-speed double application
- JEI client-only safety
- client-only classes leaking onto a dedicated server

`ClassNotFoundException` text alone is not enough to mark a run failed. Optional compatibility probes can emit it and continue. Classify severity from stack context, mod-loading outcome, server readiness, real world save, and subsequent errors.

Also inspect the Jade early-registry warning seen during prior FULL smoke runs. Determine whether it is a harmless early lookup or a real integration defect. Do not patch it merely because the log contains the word `error`.

## 5. Korean localization quality audit

This section is important. Do not treat key coverage alone as success.

Compare the effective `en_us.json` and `ko_kr.json` for every targeted namespace in the pinned FULL stack.

Requirements:
- no missing English localization key in targeted namespaces
- no empty Korean value where the English source contains meaningful user-facing text
- no ordinary English UI sentence left untranslated unless it is intentionally a proper noun/identifier
- preserve `%s`, `%1$s`, `%d`, MessageFormat/ICU fields, formatting codes, newline escapes and substitution order exactly
- Dungeons & Taverns' malformed upstream Korean JSON must be safely superseded by a valid complete overlay
- Sophisticated Backpacks Mob Catcher / Advanced Mob Catcher names and tooltips must be natural Korean
- technical identifiers that must remain identifiers must not be translated

The previous low-quality translator produced nonsense such as unrelated words, broken fragments and strings resembling `doggystyle`, `FileReport`, `CompositeSON`, repeated hearts, or meaningless Korean. None of that is acceptable even if coverage tests pass.

Inspect semantic quality, not only syntax. Sample and review all important gameplay-facing categories: item names, upgrade names/tooltips, status/error messages, menus, HUD labels, config labels visible in UI, structure/content names, commands shown to users, and JEI-visible text.

If a generated translation is meaningless or semantically wrong:
- replace it with a natural Korean translation
- prefer consistent Minecraft/mod terminology
- do not keep nonsense merely to satisfy a no-English check

## 6. JEI recipe/usage audit

Verify JEI 30.13.0.77 is present client-side in the combined MRPack and excluded from the dedicated server stack.

Check recipe visibility for craftable content, especially:
- Sophisticated Backpacks Mob Catcher Upgrade
- Advanced Mob Catcher Upgrade
- other backpack upgrades visible in the creative inventory

Confirm normal recipe/usage lookup works and that intentionally non-craftable/admin-only content is not presented as craftable.

Check R/U or the currently configured JEI recipe/usage keys for conflicts with project or companion key mappings.

## 7. Client initialization NPE investigation

Investigate this observed client log exactly:

`java.lang.NullPointerException: Cannot invoke "com.mojang.blaze3d.platform.FramerateLimitTracker.onInputReceived()" because the return value of "net.minecraft.client.Minecraft.getFramerateLimitTracker()" is null`

Observed stack path includes:
- `Minecraft 26.2 KeyboardHandler.keyPress`
- `DisplayWindow.periodicTick`
- `ModLoader.waitForFuture`
- `ClientModLoader.finish`
- `Minecraft.<init>`

Determine whether it is:
- a vanilla/NeoForge early-loading keyboard-input race
- triggered/amplified by one of the installed mods
- caused by a Frontier/Survival client initializer or key-registration path

Do not add a Minecraft keyboard mixin merely to hide the trace.

Reproduce first. Where feasible test:
- launch without keyboard input during loading
- key input during the mod-loading screen
- input after the main menu has fully initialized
- repeated launches

Only patch project code if project code is actually causal.

## 8. Performance audit

Inventory tick-driven services in both project mods.

Find expensive patterns such as:
- all-entity scans every tick
- large block scans every tick
- worker × building nested scans
- repeated path generation
- registry iteration in hot paths
- save-data dirtying every tick without state changes
- excessive network snapshots

Fix structural problems using bounded scans, caching, staggering, dirty tracking or ownership indexes where appropriate. Do not simply increase intervals to hide a bad algorithm.

## 9. Save/load and multiplayer boundaries

Even if full multi-client playtesting is unavailable, inspect code structure for:
- server-authoritative state
- logout/reconnect correctness
- dimension change correctness
- second-player join behavior
- simultaneous container/building interaction
- duplicate packet processing
- stale client caches
- integrated-server versus dedicated-server differences
- server stop/save and reload restoration

Do not invent a fake multiplayer test result if only code/static/server validation is possible. State clearly what was and was not actually tested.

## 10. Required validation after fixes

Use the repository's actual current scripts; do not guess command names if they changed.

At minimum:

Frontier:
- run all current Alpha.91/source audits
- Java 25 clean build
- JAR verifier

Survival Ascension:
- run all current source audits
- Java 25 clean build
- JAR verifier

Combined FULL profile:
- build both project JARs together
- ensure Frontier menu remains M and combined Survival menu remains K
- build the FULL KO + JEI MRPack
- validate MRPack JSON, unique filenames, hashes and client/server environment flags
- run the complete server-side pinned stack in a fresh dedicated-server world
- confirm mod discovery
- confirm datapack/registry loading
- confirm world creation
- confirm `Done`/ready state
- cleanly `stop` and verify the world actually saved
- reject actual server tick crashes, fatal mod-loading failures, fatal datapack failures and fatal registry failures

Where client automation is feasible, also verify Korean resources, JEI, key mappings and the reported startup NPE.

## 11. Fixing rules

- fix root causes, not log strings
- no blanket regex mass replacement
- no feature deletion to make CI green
- no teleport/force-load shortcuts
- no virtual-resource ledger replacing physical logistics
- keep optional integrations optional
- keep server authority
- preserve current gameplay direction
- do not copy third-party source/assets unless licensing and necessity are explicitly verified
- do not modify unrelated projects in the monorepo
- do not overwrite concurrent `main` changes
- if main moves while working, fetch/rebase before the final push

## 12. Final deliverables

Create or update a concise audit report under the relevant Frontier documentation directory and report in the final Codex response:
1. starting HEAD
2. newest successful FULL KO + JEI build/run inspected
3. all defects found, grouped by severity
4. fixes applied
5. warnings intentionally left and why
6. Korean localization coverage and semantic-quality result
7. JEI/recipe result
8. FramerateLimitTracker NPE conclusion
9. performance findings
10. Frontier clean-build/JAR-verify result
11. Survival clean-build/JAR-verify result
12. FULL dedicated-server smoke result
13. client-side checks actually completed versus not possible
14. files changed
15. final commit SHA
16. remaining manual in-game test checklist

If a real defect is found, do not stop at reporting it. Fix it, rerun the relevant validation, and only then finalize.
