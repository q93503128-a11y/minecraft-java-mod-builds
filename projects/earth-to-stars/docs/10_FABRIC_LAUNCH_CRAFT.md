# EARTH TO STARS — Fabric Physical Launch Craft

This document records the first real player-facing physical ship boundary on the Fabric 26.2 rebase. It closes construction/deploy/entity/seat/control/collision/render initialization as an automated integration unit. It does **not** claim that flight feel, visual scale, Earth-to-space continuity, resource drain, persistence with a populated craft, or multiplayer have been manually playtested.

## Verified source

- physical launch-craft implementation commit: `69bde96fc6fec2ad7c93048e0de853ecbcf13e10`
- Minecraft 26.2 API/safety correction commit: `6ec6e8c860e3f1f52403bd8be3e721667ebaabe2`
- Minecraft: `26.2`
- Fabric Loader: `0.19.5`
- Fabric API: `0.160.0+26.2`
- Java: `25`
- Mod version: `0.3.0-alpha.1`

## Product contract preserved

The Launch Craft remains the small first ship from the master design: a practical one-pilot initial implementation that exists to make the first Earth launch real. It is not a freeform moving-block ship and it does not introduce a second copy of ETS ownership, modules, fuel or progression state.

`ShipState` remains authoritative. The world entity carries a stable `ShipId` and represents the ship physically.

The starter blueprint reuses the retained authored module contract:

- `command_core_mk1`
- `engine_mk1`
- `battery_mk1`
- `cargo_mk1`
- `life_support_mk1`

Weapon and sensor capability remain intentionally absent from the initial craft so later orbital progression can unlock new capabilities rather than only larger numbers.

## Implemented physical path

### Launch / deploy transaction

`earth_to_stars:launch_craft_kit` now performs a real server-side deployment path:

- Overworld-only deployment;
- one owned ship guard;
- `5 × 5 × 3` deployment clearance check;
- fluid rejection;
- collision-bearing blocks reject deployment;
- authoritative `ShipState` creation and starter-module installation;
- persistent repository/save insertion;
- authoritative `ShipFlightRuntime` activation;
- real `LaunchCraftEntity` spawn with the same stable `ShipId`;
- rollback of logical ship state/runtime when physical entity creation fails;
- kit consumption only after successful deployment.

The API-alignment correction removed the earlier pre-spawn replaceable-block clearing. Deployment therefore no longer edits grass/flowers or other replaceable world state before the logical + physical transaction succeeds.

### Real vehicle entity

The physical craft is a Minecraft 26.2 `VehicleEntity`, not the old Display-entity-style vehicle.

Current physical responsibilities are deliberately narrow:

- stable `ShipId` synchronization/persistence;
- world position / rotation;
- one pilot passenger;
- collision resolution through Minecraft entity movement;
- reconciliation of collision-resolved motion back into `ShipFlightRuntime`;
- physical-entity uniqueness binding by `ShipId`;
- runtime/control cleanup when the bound physical entity is removed.

The entity does not own a duplicate module inventory, economy, propellant store, oxygen store or permissions model.

### Server-authoritative pilot control

Interaction validates the physical craft before issuing control:

- player must be a server player;
- player must be within the physical interaction range;
- authoritative ship must exist;
- player must have `PILOT` permission;
- pilot seat must be available;
- player must actually mount the craft;
- only then can `EarthToStarsFabricShipAuthority.grantControl` issue the session.

The client sends only input tied to the server-issued session and monotonic sequence.

Current controls:

- `W / S`: forward / reverse throttle
- `A / D`: yaw
- jump: lift up
- sprint: lift down

These controls are implementation state, not yet accepted flight UX. Live playtest may change them.

### Collision / movement authority

The existing loader-neutral `ShipFlightRuntime` remains the deterministic movement simulation. Each server tick:

1. runtime advances the authoritative flight transform;
2. the physical entity requests the corresponding Minecraft movement;
3. Minecraft resolves world collision;
4. the entity position/velocity after collision is reconciled back into the runtime.

This avoids inventing a second Fabric-only flight model while still allowing real Minecraft collision to constrain the craft.

The current entity uses one broad physical body. Detailed auxiliary hitboxes are intentionally deferred until actual play shows they are required; a large car-style hitbox graph is not imported blindly for a spacecraft.

### Visual path

The existing approved Kenney Space Kit CC0 starter-craft source asset is reused directly. The old NeoForge OBJ adapter is not restored.

`LaunchCraftEntityRenderer` uses the Minecraft 26.2 submit/render-state pipeline and loads the vendored triangular OBJ source. The current client smoke actually initialized the renderer resource path and parsed **280 source triangles** successfully.

This proves the resource/parser initialization path, not that the craft has been visually accepted in a live world. Scale, orientation, material treatment, camera relation and silhouette still require player-facing inspection.

## First build failure and correction

Initial physical-craft commit `69bde96fc6fec2ad7c93048e0de853ecbcf13e10` triggered `Build earth-to-stars Fabric 26.2` run `35054175541`.

The standalone contract validator passed, while Java compilation exposed Minecraft 26.2 API differences in the new integration boundary:

- `CameraRenderState` package moved to `net.minecraft.client.renderer.state.level`;
- entity `ValueInput` / `ValueOutput` save hooks are abstract and do not call a superclass implementation;
- `ServerPlayer.displayClientMessage` is not the current API path;
- the selected `startRiding` overload was obsolete;
- the old removal hook name was not a vanilla/Fabric 26.2 override.

Commit `6ec6e8c860e3f1f52403bd8be3e721667ebaabe2` aligned only those API boundaries and added the deployment/removal safety fixes described above. No ship feature was deleted, excluded or stubbed to obtain a green build.

## Verification

`Build earth-to-stars Fabric 26.2` run `35054576328` / run number `#42`: **PASS**

- standalone Fabric contract validator: PASS
- loader-neutral ship kernel tests: PASS
- clean `test build`: PASS
- production JAR structure verification: PASS
- dedicated Fabric server boot: PASS
- server loaded ETS `physical_launch_craft=registered` and `ship_authority_bridge=registered`
- production JAR: `earth_to_stars-0.3.0-alpha.1.jar`
- SHA-256: `23f652d32f5053a83c2ecfb468d5fdcbaa6b98ae51a421aaa89eee3404d88122`

`Smoke earth-to-stars Fabric client` run `35054576366` / run number `#17`: **PASS**

- Xvfb/client preparation: PASS
- Minecraft 26.2 client startup: PASS
- ETS client initialization: PASS
- resource reload: PASS
- Kenney launch-craft OBJ parse: PASS (`280` triangles)
- 150-second `runClient` smoke: PASS

The headless Linux runner reports missing narrator `flite` and missing audio-device/OpenAL errors. Minecraft continued running and the ETS smoke completed successfully; these are CI-environment limitations, not evidence of an ETS entrypoint failure.

## Verification labels

- `CODE REVIEWED`: YES for this implementation unit
- `TESTED`: YES for automated contract/kernel/build/server/client initialization gates
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `DEDICATED FABRIC BOOT`: YES
- `CLIENT FABRIC SMOKE`: YES
- `KENNEY MESH PARSE`: YES
- `PHYSICAL STARTER CRAFT PLAYTESTED`: **NO**
- `LIVE FLIGHT FEEL`: **NOT TESTED**
- `CRAFT VISUAL ACCEPTANCE IN WORLD`: **NOT TESTED**
- `POPULATED CRAFT SAVE/RELOAD`: **NOT TESTED**
- `POWER / PROPELLANT / OXYGEN FLIGHT CONSUMPTION`: **NOT CONNECTED**
- `EARTH→SPACE CONTINUITY`: **NOT TESTED**
- `MULTIPLAYER TESTED`: **NO**

Build/server/client smoke proves integration and startup, not the quality of actual flying.

## Immediate live acceptance test

Use a normal Fabric 26.2 client with the verified JAR and enter an Overworld test world.

```mcfunction
/give @s earth_to_stars:launch_craft_kit
```

Expected first test:

1. Use the kit on clear, dry ground with roughly `5 × 5 × 3` free space.
2. A Kenney-based `발사정` should appear and the kit should be consumed only on success.
3. Right-click the craft. The owner should mount the pilot seat and receive a server-issued control session.
4. Test `W/S`, `A/D`, jump and sprint.
5. Fly into terrain deliberately and check for tunnelling, sticking, violent snapping or mismatch between visible hull and collision.
6. Dismount, leave/re-enter the area, save/reload the world and report whether the physical craft and control path remain coherent.

Important observations to report with screenshots if possible:

- craft scale relative to Steve;
- model orientation and whether forward movement matches the nose;
- seat/player position;
- third-person camera obstruction;
- acceleration/deceleration feel;
- yaw responsiveness;
- vertical-control feel;
- collision fairness versus the visible hull;
- jitter or server correction;
- whether the model or materials look wrong;
- save/reload or chunk-unload disappearance/duplication.

## Next engineering gate

Do not expand Moon/asteroid content yet.

After live acceptance of the physical craft boundary:

```text
actual craft feel / scale / camera / collision corrections
→ connect authoritative power + propellant + oxygen consumption to real flight
→ launch-readiness failure/recovery UX
→ continuous-feeling atmosphere → space transition
→ first orbit milestone
→ Earth return
→ then orbital salvage / first weapon / first sensor loop
```

The first playable launch is the product gate. More content does not compensate for a craft that looks or feels wrong.
