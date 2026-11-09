# TURNBOUND — Terminus dependency review

Based on the verified official v2.2.2f audit from GitHub Actions run 42568764777. This is analysis of function identifiers, edges and command-block references; no source/world bytes were modified in this stage.

- 82 unique referenced function IDs, 83 command-block matched invocations. This is incomplete physical coverage (at least 737 region scan warnings).
- 29 direct command-referenced tower-arrival IDs and 32 tower-departure IDs.
- All 32 original tower-departure functions directly call players:gamemode/set_survival, which conflicts with TURNBOUND's no-survival contract.
- All 32 original tower-arrival functions directly call players:gamemode/set_adventure and the original unlock chain. The unlock chain reaches terminus:count_towers and some original gift/progression functions.
- Structure/scene/shuttle command blocks must be reviewed individually; namespace names alone do not establish safe removal.
- The reviewed graph does not contain exact block coordinates, original commands or redstone invocation conditions. Do not infer actual destination positions or declare teleport functionality safe.

Decision: retain existing tower structures but implement server-authoritative TURNBOUND travel, location discovery and unlock state separately. Do not directly invoke the original movement, story or gift functions. The next task is to capture a position-aware metadata-only inventory of those direct command-block invocations, then implement copy-only reversible changes after explicit classification.

The dedicated Terminus graph workflow reads and verifies the previous audit artifact without redownloading the ~4 GB official world. Neither the analysis nor its artifacts redistribute map bytes.

## Actual reviewed call graph — verified checkpoint

[Terminus graph review #1](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42573686150) **SUCCESS**, using only the pinned previous official map-audit artifact. Full map not downloaded again and source world not modified.

- **82 distinct function IDs / 83 matched commands** from 2,232 scanned command blocks.
- **29 tower-arrival calls**, **33 tower-departure calls** (32 distinct departure functions; one used twice).
- **8 structure-scene calls**, **12 original player/drop/dialogue calls**, **1 other Terminus function call**. These are counts of regex-matched invocations, not whole-world device counts.
- Three referenced IDs have **no matching function body in the parsed graph**: `abilities:lowparticlemode`, `shuttle:reset`, `shuttle:start`. No automatic deletion or replacement is authorized for unresolved calls.
- The fixed source graph proves that all 29 directly referenced tower-arrival functions reach original adventure-mode switching, tower unlock and reward paths. All 32 distinct directly referenced tower-departure functions directly reach vanilla survival mode switching.
- The output is review metadata and **does not determine physical warp positions**. Coordinates, invocation conditions and missing region/command block coverage remain unverified.

The next needed evidence is [physical command-block inventory](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/workflows/turnbound-drehmal-trigger-locations.yml), which inspects the verified official original world and emits only the redacted function-reference, dimension, coordinate and command hash metadata. Its output is not yet a permission to modify any scene mechanism.

## Architecture boundary for replacement

For each confirmed original tower, maintain a data-driven TURNBOUND record with exact dimension, entry/exit coordinates, unlock state owned by the server, and a verified interaction surface. Discover/unlock state is independent of Drehmal scoreboard, original gifts, survival mode and original item drops. No gameplay action may be enabled merely from the presence of a function ID. Decisions about world access, initial towers and costs remain subject to existing TURNBOUND design.
