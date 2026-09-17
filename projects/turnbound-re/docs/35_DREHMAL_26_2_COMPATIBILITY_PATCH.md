# 35 — DREHMAL 26.2 DATAPACK COMPATIBILITY PATCH

Date: 2026-09-17

## Runtime finding

The first real Modrinth-pack migration attempt reached Minecraft 26.2's world-storage upgrader, but world load then stopped while loading `file/hi_drehmal.zip`.

The supplied client logs identify three concrete legacy-data incompatibilities:

- `minecraft:space_type` is missing the 26.2-required `has_ender_dragon_fight` field.
- biome `carvers` still uses the legacy carving-step object form such as `{"air":[...]}`, while 26.2 expects one holder list/set.
- some biomes use `creature_spawn_probability: 1.0`, while 26.2 accepts at most `0.9999999`.

The load terminates with a registry-loading failure before the integrated server starts. This is therefore a confirmed Drehmal 1.20.1 datapack migration blocker rather than a TURNBOUND/NeoForge bootstrap failure.

## Implemented compatibility boundary

`Drehmal26_2DatapackMigrator` performs a local migration only on TURNBOUND's installed copy of the pinned external world. TURNBOUND still does not redistribute the Drehmal map or resource pack.

The migrator:

1. runs only after the existing pinned-world installation/verification path,
2. preserves the original `datapacks/hi_drehmal.zip` under `.turnbound-re-backup/`,
3. flattens legacy biome `carvers` maps into the 26.2 holder-list form,
4. clamps out-of-range creature spawn probability to the 26.2 maximum,
5. adds `has_ender_dragon_fight` to legacy dimension types,
6. maps legacy End/Nether `effects` to the matching optional 26.2 `skybox` where applicable,
7. validates the rewritten archive before replacing the working datapack,
8. writes `.turnbound_re_26_2_compat` only after validation succeeds.

The installer now treats an already-downloaded pinned Drehmal world with no compatibility marker as an in-place repair case. It does not require the multi-gigabyte map shards to be downloaded again.

## Automated regression contract

`Drehmal26_2DatapackMigratorTest` locks the observed failure shapes with a synthetic datapack fixture. It verifies:

- legacy carver-map flattening,
- `1.0 -> 0.9999999` spawn-probability migration,
- dimension-type dragon-fight flag insertion,
- End skybox migration,
- exact preservation of the original datapack backup,
- unrelated datapack entries remaining unchanged,
- repeat migration being idempotent.

## Verification checkpoint

Implementation commit:

`32c4c5314b577ff77978a023fdc344d931c4bb95`

Regression-test commit:

`a502c37a2ba571d6edc94849fa0550dfdd97316e`

GitHub Actions `Build turnbound-re` run `35176808054` / run #274 completed successfully for the regression-test commit.

Current state:

- LOG ROOT CAUSE REVIEWED: YES
- CODE REVIEWED: YES
- MIGRATION TRANSFORM TESTED: YES — JUnit synthetic datapack contract
- BUILD VERIFIED: YES — workflow run #274
- JAR PRODUCED: YES — workflow deliverable pending extraction for playtest handoff
- WORLD LOAD RETESTED: NO
- DREHMAL LANDMARKS INSPECTED UNDER 26.2: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

A successful build and unit test do not prove the migrated 1.20.1 world is fully compatible with 26.2. The next decisive gate is reopening the already-downloaded TURNBOUND Drehmal save and checking whether registry loading advances past the former `hi_drehmal.zip` failure. Any later migration-layer failure must be diagnosed from the next real client log rather than hidden or bypassed.
