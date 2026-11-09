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
