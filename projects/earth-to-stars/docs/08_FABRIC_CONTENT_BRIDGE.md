# EARTH TO STARS — Fabric 26.2 Content Bridge

This document records the first player-visible Fabric content slice after the standalone rebase. It is a migration status note, not a replacement for `PROJECT.md`, `docs/00_MASTER_GAME_DESIGN.md`, or `docs/07_FABRIC_26_2_STANDALONE_REBASE.md`.

## Verified source

- source commit: `7455f005f688d249a3d67743c590cbfa7f24dc0e`
- Minecraft: `26.2`
- Fabric Loader: `0.19.5`
- Fabric API: `0.160.0+26.2`
- Java: `25`
- Gradle: `9.5.1`

## Implemented

The first safe Fabric content bridge now registers three behavior-free construction components directly on Fabric:

- `earth_to_stars:reinforced_frame` — stack 32
- `earth_to_stars:avionics_unit` — stack 16
- `earth_to_stars:life_support_unit` — stack 8

Their Fabric-side item definitions, item/model JSONs, Korean/English translations, and the ETS data namespace bootstrap marker are packaged in `src/fabric/resources`. The items are exposed through the vanilla Ingredients creative tab for development access.

This intentionally does **not** port the old NeoForge `launch_craft_kit`, propellant/oxygen supply items, sensor-core upgrade item, or render-only vehicle tokens as inert placeholders. Those items have gameplay authority or rendering behavior and must only return when their Fabric behavior is real.

The Fabric entrypoint now initializes the construction-component registry while continuing to boot the loader-neutral ship kernel. `tools/validate_fabric_standalone.py` also guards these Fabric resources and keeps whole external ship/space mods out of the runtime dependency contract.

## Failure found and corrected

The first content-bridge CI (`35048236331`) correctly failed compilation because the initial import used the pre-26.1 Fabric package `net.fabricmc.fabric.api.itemgroup.v1.CreativeModeTabEvents`.

Fabric 26.2 uses `net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents`. The import was corrected in commit `7455f005f688d249a3d67743c590cbfa7f24dc0e`; no feature was removed or stubbed to make the build pass.

## Verification

`Build earth-to-stars Fabric 26.2` run `35048370766`: **PASS**

- standalone contract validator: PASS
- loader-neutral kernel tests: PASS
- clean test/build: PASS
- production JAR structure verification: PASS
- dedicated Fabric server boot: PASS
- production JAR: `earth_to_stars-0.3.0-alpha.1.jar`
- SHA-256: `e83e8dc62a9db3287d803a26a90f4ddb0d10a7048a23de8bc08eef0fa7cb9a72`

`Smoke earth-to-stars Fabric client` run `35048370777`: **PASS**

- Fabric client preparation: PASS
- Xvfb `runClient` smoke: PASS
- no fatal ETS client initialization failure in the smoke gate

Verification state:

- `CODE REVIEWED`: YES for this registry/resource slice
- `TESTED`: YES for automated kernel/build/server/client gates
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `PLAYTESTED`: NO — no player-facing Fabric starter craft exists yet
- `EARTH→SPACE CONTINUITY`: NOT TESTED
- `MULTIPLAYER TESTED`: NO

A successful client smoke proves loading/initialization, not visual quality or flight feel.

## Next migration unit

The next meaningful unit is the **server-authoritative Fabric ship authority bridge**, not more decorative items.

Target sequence:

```text
Fabric networking contract
→ server-owned pilot/control session
→ Fabric save bridge around existing ShipRepository / ShipStateCodec
→ real launch/deploy transaction
→ starter craft entity/visual path
→ seat/input/camera
→ collision + movement
→ real power/propellant/oxygen consumption
```

Rules for the next unit:

- reuse the existing loader-neutral `ShipRepository`, `ShipState`, permissions, system simulation, and `ShipStateCodec` rather than duplicating ship state in Fabric glue;
- client packets contain requests/input only; the server validates player, ship, role/session, ranges, and state before mutation;
- old NeoForge `ship/networking/**`, `ship/persistence/minecraft/**`, and `ship/runtime/minecraft/**` are migration references only;
- do not restore the failed old display-entity cockpit/flight assumptions merely because Minecraft 26.2 is back;
- before choosing the physical vehicle implementation, inspect permissively licensed Fabric/current-Minecraft vehicle or entity movement implementations and record exact source, commit/version, license, and what is reused;
- do not expand Moon/asteroid content before the starter-craft vertical slice passes live play acceptance.
