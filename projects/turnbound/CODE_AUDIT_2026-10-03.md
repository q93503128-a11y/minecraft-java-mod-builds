# TURNBOUND code audit — 2026-10-03

Baseline:
- branch: `turnbound-alpha17-full-v04`
- starting HEAD: `43cec9747f376d9be8be88672c84925d9058152a`
- verified implementation commit: `900a80d04738707cd566d8b6679bce8e854c110a`
- Build TURNBOUND #1007 / run `37112974341`

## Scope

The audit followed the current branch canon and reviewed the high-risk surfaces requested in the handoff:
combat/targeting, progression, session lifecycle, networking, client routing, world runtime,
campaign persistence/save migration, equipment, gacha, quest/map projection, field runtime,
JourneyMap integration, temporary entity cleanup, multiplayer authority boundaries, performance
hot paths, deprecated API warnings, stale production paths, reconnect/save/reset edges and
registry/network/serialization risk.

This is a high-risk domain audit, not a claim that every line of all 354 main Java files was manually read.
Core files and the paths implicated by the first playtest were read directly; automated tests/build/server smoke
provided the broad regression net.

## Fixed actual bugs / player-facing defects

1. **M still opened the retired TURNBOUND map**
   - `MetaMenuKeyHandler` sent `HUB_ROUTE_REVIEW`, the server emitted `O|MAP`, and
     `ClientMetaNetwork` instantiated `DrehmalWorldMapScreen`.
   - M now records the server-side route-review flag but opens JourneyMap directly.
   - Legacy `O|MAP` hints also route to JourneyMap instead of the retired screen.

2. **System-menu minimap toggle controlled a dead local setting**
   - N already toggled JourneyMap, but E > 설정 read/wrote `TurnboundClientSettings.minimap`.
   - The settings screen now reads/toggles the live JourneyMap minimap state.

3. **Committed summon could be reported as failed when only presentation failed**
   - Progression and currency spend were already committed before presentation.
   - Resolution/persistence failure and presentation failure are now separate error paths.
   - A presentation-only failure tells the player the summon result was saved and can be checked in records,
     avoiding a misleading retry instruction after currency was spent.

4. **Ten-pull presentation repeated long ceremonies for every newly-owned low-rarity result**
   - Single pulls always receive a reveal.
   - Multi-pulls reserve the full 3D spotlight for ★4/★5 results.
   - If a multi-pull contains no ★4+, the best result gets one short preview, then the batch summary handles all ten.
   - Low-rarity new characters remain visible in the final result summary.

5. **Summon timing did not meaningfully communicate rarity**
   - Server actor timing and client camera now consume one rarity-aware timing contract.
   - ★1/★2 reveals stay short; ★4/★5 receive stronger anticipation and longer final character hold.
   - New ownership adds only a small name-hold bonus rather than inflating the whole sequence.

6. **Summon stage relied on continuous particle decoration**
   - The always-on summon-stage particle ring was removed.
   - Presentation particles are now event accents only.
   - The focal hierarchy is camera → actual GeckoLib character → authored ready/victory pose → audio → limited VFX.

## Revalidated as correct in current code

### Selected-two combat
- manual first/second target selection exists;
- two targets are distinct when two living enemies exist;
- one-enemy fallback resolves to one target;
- server engine validates target count, identity and living state again at commit;
- a death between selection and action commit rejects/resyncs rather than applying an invalid target;
- AUTO/shared battle paths reach the server-authoritative engine.

### Duplicate progression
- first acquisition unlocks the character;
- duplicates at +0…+9 increase +Level only;
- Star Essence is granted only when the character is already +10;
- presentation/result UI chooses either +Level or Essence, not both.

### Equipment
- character-centric slot → compatible inventory → compare/equip flow exists;
- actual equip mutation is server-authoritative;
- global E > 장비 is inventory/reference oriented;
- blacksmith owns enhancement before/after/delta/cost presentation;
- inventory restore validates missing/duplicate equipment references.

### Save/lifecycle
- campaign attachment load failures fail closed without overwriting the original profile;
- legacy migration preserves/quarantines old data rather than deleting it;
- reward journal recovery is separated from canonical profile state;
- summon actor cleanup exists for GACHA_DONE, logout and server stop;
- temporary gacha actors carry TTL cleanup as a final fallback.

### JourneyMap packaging
- JourneyMap API remains client-only;
- one-click pack contains JourneyMap 26.2-6.0.8 as client-required/server-unsupported;
- TURNBOUND waypoints are transient and refreshed from server-authored field/map state;
- dedicated-server smoke passes without loading JourneyMap client API.

## Structural debt / runtime risks not changed in this pass

1. **Legacy map/minimap classes remain in source**
   - `DrehmalWorldMapScreen`, `DrehmalMinimapLayer` and the old local minimap preference remain for compatibility/history.
   - They are no longer the intended production route.
   - Delete only after a separate reference/save-compatibility audit.

2. **Battle action handlers use broad RuntimeException recovery**
   - This is useful for stale target/race recovery but can hide unrelated combat defects by only resyncing.
   - Future cleanup should distinguish expected invalid-action errors from unexpected runtime faults and log the latter.

3. **GM `/turnbound archive` textual summon summary is stale for pre-cap duplicates**
   - Gameplay gacha UI is correct.
   - The admin/test command still describes non-new pulls as Essence even when +Level consumed the duplicate.
   - Low severity; do not change gameplay code solely for this test-only wording in this verified pass.

4. **True in-world silhouette / purpose-built ritual prop is still absent**
   - The presentation now has better pacing, camera hierarchy and reduced particle dependence,
     but the pre-reveal silhouette is not yet a bespoke 3D asset.
   - Do not fake this with an arbitrary AI-coded block ring. Add it only with a reviewed model/Display-Entity asset.

5. **Reconnect/client presentation remains an actual-client test gap**
   - Server logout/TTL cleanup is present.
   - JourneyMap M/N behavior and camera restoration must still be checked in a real client.

## Performance review

- No new per-tick full-world scan was found in the reviewed production paths.
- `DrehmalVisibleEncounterService` is globally tick-deduplicated and its heavier entity adoption scans are conditional.
- Removing the permanent summon-stage particle loop reduces continuous presentation work and visual clutter.
- No profiler claim is made: no spark/JFR capture was run in this pass.

## Warnings classified

Build #1007 still reports:
- `DrehmalVisibleEncounterService` uses/overrides a deprecated API — **maintenance debt, not a current build blocker**.
- Gradle deprecated features incompatible with future Gradle 10 — **tooling debt; inspect with warning-mode when upgrading**.
- GeckoLib `logoFile` metadata deprecation — **dependency metadata warning**.
- missing `server.properties` during CI smoke startup — **CI environment noise; server smoke still completed successfully**.

Do not use these warnings as justification to rewrite unrelated runtime code.

## Build #1007 verification

- Gradle tests/build: PASS
- NeoForge dedicated-server smoke: PASS
- built JAR verification: PASS
- one-click mrpack verification: PASS
- JourneyMap pack entry verification: PASS
- artifact upload: PASS
- artifact: `turnbound-v04-workbranch`
- artifact ID: `11271185405`

Hashes:
- JAR `turnbound-0.1.0-alpha.17.jar`
  - SHA-256 `34152053b4a8943fca89b0507d23ac00e3decee941c9022015fec26d061966e1`
- one-click `TURNBOUND-oneclick-0.1.0-alpha.17.mrpack`
  - SHA-256 `7175d3680f3076f4a8a7049284f37d1769f4a9f36bfecfeff5893086af9e0f6f`
- uploaded artifact ZIP
  - SHA-256 `a84b8bf526b711e1f846be84292994c5f25418085215d28459b238c343b8be9e`

Validation state:
- CODE REVIEWED: YES — handoff-specified high-risk domains; not a 354-file line-by-line claim
- AUTOMATED TESTS: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- DEDICATED SERVER TESTED: YES
- ONE-CLICK PACK VERIFIED: YES
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO for Build #1007
- MULTIPLAYER TESTED: NO

## Next real-client test order

1. Launch the #1007 one-click pack.
2. In New Drabyel, press M and confirm JourneyMap fullscreen opens; press N twice and confirm the JourneyMap minimap toggles.
3. Open E > 설정 and confirm its minimap state matches N/JourneyMap.
4. Use the physical summoner:
   - one low-rarity summon;
   - one ★4/★5 result when available;
   - ten-pull;
   - skip/ESC during presentation.
5. Confirm camera/control restore and that no summon actor/VFX remains afterward.
6. Confirm ten-pull does not replay full ceremonies for low-rarity new results, while all ten still appear in summary.
7. Recheck Character > Equipment slot flow and one selected-two combat skill.
8. Multiplayer remains NOT TESTED until a second client is available.
