# Augmentation Survival — Implementation Status

Date: 2026-10-25
Supersedes: `IMPLEMENTATION_STATUS_2026-10-24.md`
Status: M1C FUNCTIONAL BASELINE / 137-OF-146 FULL GAMEPLAY RUNTIME / PHYSICAL SPATIAL GATES + 17 AUTHORITY PACKAGES

## 146-roster implementation count

This count measures whether the canonical gameplay effect has a runtime implementation. It does **not** mean final VFX/UI/audio polish or live gameplay testing is complete.

- Fully implemented gameplay runtime: **137 / 146**
- Partially implemented: **1 / 146** — `권능 탈취`
- Not fully implemented: **9 / 146**
- No gameplay runtime yet: **8 / 146**
- `공간 연결` moved from PARTIAL to FULL in this checkpoint.
- `분신`, `풍요` and `휩쓸기 강화` remain FULL.

## Verified FULL runtime notes

### `전리품 챙기기`

`LivingEntity.dropFromLootTable` wraps only the killed entity's final entity-loot-table `Consumer<ItemStack>`. One canonical **5% / 10% / 15%** roll duplicates that exact loot-table bundle. Nearby drops, another mob's drops, equipment drops and unrelated custom death drops cannot be captured accidentally. Player victims, dominated companions, configured bosses and augment cores remain excluded.

### `풍요`

`풍요` duplicates the exact final compatible ordinary-drop stack at the producer boundary rather than scanning nearby `ItemEntity` objects.

- Direct mining / direct harvest: final stacks emitted through `Block.popResource` are duplicated for the server player owning the active direct `destroyBlock` call. Derived ResourceHarvestService breaks are excluded.
- Fishing: the actual final fishing loot list is duplicated without re-rolling the fishing loot table.
- Normal-mob direct kill: the killed entity's exact final loot-table consumer is wrapped and root-direct player provenance is required.
- Cores, explicit `augmentation_survival:abundance_excluded` entries, non-stackable/damageable equipment and custom-data/name/lore stacks remain excluded.

### `휩쓸기 강화`

This augment is not in the remaining no-runtime set. Vanilla sweep expansion and the optional Better Combat 3.2.2 adapter were already implemented and full-pack smoke-verified in Build #81. Live combat feel and multiplayer behavior remain untested.

### `분신`

`분신` has the canonical gameplay runtime rather than the earlier partial attack-attribute approximation.

- Registered custom `CombatCloneEntity`; no zombie or armor-stand shell.
- Server-owned autonomous hostile-target melee AI.
- 20-second lifetime, activation-time 50% max-health snapshot and 60-second base cooldown.
- Activation-time offensive snapshot includes the player's current attack attribute plus the project's target-independent outgoing combat package used by `keen_edge`, `last_strength`, `speed_conversion`, `brawl`, `glass_cannon` and the currently active Berserk bonus before applying the canonical 60% clone multiplier.
- Target-dependent future-enemy conditions are deliberately not frozen into the activation snapshot.
- Copied hand/armor stacks are presentation copies: functional attribute modifiers and enchantments are removed from the clone copy while visual glint can be retained. This prevents the displayed weapon/armor from re-applying combat stats on top of the fixed snapshot.
- Owner UUID and player-model-part mask are synchronized; actual owner skin lookup, wide/slim `PlayerModel` selection, hat/jacket/sleeve/pants toggles, visible equipment and handedness are retained.
- Equipment drop chance remains zero and item pickup remains disabled.
- Clone damage provenance remains non-player-root, so clone combat cannot satisfy root-direct kill requirements such as boss-first-kill or authority theft.

This FULL label is for canonical gameplay runtime coverage. Owner-skin fidelity, animation/combat feel and visual quality still require a real client playtest; no visual-playtest claim is made from smoke testing.

### `공간 연결`

`공간 연결` now has both the persistent server-owned network and an actual world-readable permanent gate object.

Runtime coverage:

- Multiple permanent gates with stable IDs and save-wide persistent Fabric attachment state.
- Unlimited-distance and cross-dimension travel.
- Personal gates plus explicit per-player sharing and revocation.
- Naming, search, favorites and per-player default destination.
- Dedicated rebindable `K` key and server-authoritative client/server snapshot/action protocol.
- Search-first control screen with own/shared distinction, favorite/default markers, pagination and register/rename/delete/share/revoke/default/teleport workflows.
- Destination chunk loading and collision-safe landing search; obstructed destinations fail rather than forcing entities into blocks.
- Root-vehicle/passenger transfer plus nearby dominated and player-tamed companion transfer with individual safe-placement checks.
- Registering a gate now creates an actual `augmentation_survival:spatial_gate` world anchor beside the player rather than replacing the player's occupied block.
- The physical anchor has a dedicated multi-element world model using a dark frame, amethyst pylons and luminous core; it is system-created rather than exposed as a normal placeable item.
- Right-clicking the physical gate opens the same server-authoritative Spatial Link network.
- Gate deletion removes the matching physical anchor. If a saved destination anchor is missing and its saved block is clear, travel restores the anchor.
- Registration, deletion and travel have portal/beacon/teleport presentation feedback.

`공간 연결` is therefore **FULL for canonical gameplay runtime coverage**. The physical model, teleport feel, UI composition, mount/companion behavior and multiplayer permissions still require real playtesting before final visual/UX polish can be considered complete.

## `권능 탈취` — PARTIAL, 17 fixed boss packages implemented

The dedicated Mythic authority channel remains separate from the ten normal active slots and uses one 120-second server-owned release cooldown.

Core runtime:

- Exact root-direct player kill provenance is required.
- Supported boss identity resolves through the existing compatibility-profile layer.
- The first supported direct boss kill permanently adds that authority to `stolen_authority_boss_ids`; repeated kills do not duplicate it.
- Dedicated rebindable `J` key.
- One activation releases every currently stolen supported boss package in sequence.
- Authority-package damage now executes under `CombatProvenance.AUTHORITY`, so stolen-power damage cannot recursively satisfy root-direct boss-kill or authority-theft requirements.

Current fixed-package coverage: **17 bosses**.

### Chrono Dawn — 6

- Time Guardian
- Chronos Warden
- Clockwork Colossus
- Temporal Phantom
- Entropy Keeper
- Time Tyrant

These packages continue to mirror the inspected 26.2 boss identities: temporal slow/blink/AoE pressure, stone stance/ground slam, overcharge/repair/gear pressure/shockwave, phase/warp/blink/temporary echoes, decay/rot/burst and Time Tyrant's stop/acceleration/AoE/reversal-style recovery.

### World Bosses — 11

- Sylvaron — root/spore pressure
- Aureon — Thunder Cage/storm pressure
- Nekhraz — sandstorm/burrow pressure
- Varkhul — Infernal Chains/Flame Prison pressure
- Morvath — Fear Pulse/Soul Drain
- Elyndra — void tether/rift pressure
- Astrael — Primordial Chains/elemental shift/Cataclysm Nova
- Shadow King — soul bolt/Shadow Step/shadow-warrior/darkness pressure
- Dragon Mother — fire breath/tail sweep/aerial-fire/brood pressure
- Storm Griffin — Wing Gust/lightning pressure
- Shadow Weaver — binding threads/clones/puppet-style pressure/protective veil

The World Bosses player packages preserve the documented boss identities rather than collapsing them into one generic damage burst. Exact numeric/JAR-level fidelity and live VFX/combat-feel validation are not claimed yet.

### Why `권능 탈취` remains PARTIAL

Pinned MCEndgame does not expose one fixed boss class or one authored boss ability package. Its source marks arbitrary `Mob` instances as dungeon bosses and then enhances them dynamically. The inspected MCEndgame source defines progression per previously killed dungeon boss as **+25% loot, -10% damage taken and +5% damage dealt**. Because the actual dungeon boss can be a different underlying mob, a faithful stolen package needs a persistent dynamic combat snapshot/translation rather than a fabricated generic spell. That dynamic MCEndgame authority representation remains the final major runtime gap.

## Verified gameplay checkpoint

- Gameplay-code checkpoint: `3066907ead902e48af72bc2ac30081b94752888d`
- Final gameplay commit: `fix(augmentation-survival): place spatial anchors beside players`
- GitHub Actions: `40168014824` / Build Augmentation Survival Pack #137
- Build and pinned-pack inspection: **PASS**
- Dedicated server smoke: **PASS**
- Client boot smoke: **PASS**
- Verification artifact upload: **PASS**
- Artifact: `augmentation-survival-pack-smoke-137`
- Artifact ID: `12589555475`
- Artifact ZIP size: `1062511` bytes
- Artifact ZIP SHA-256: `35b6f05538880d651d59dfd37b7f8699626cc2f4dc0271171b84a14c99686314`

Build #135 failed during compile because Minecraft 26.2 does not expose `BlockBehaviour.Properties.noCollission()`. The first real failure was read from the job log, the unsupported property was removed, and the subsequent build path compiled. Build #137 is the authoritative final gameplay checkpoint for this batch and passed build, dedicated-server smoke and client-boot smoke.

## CI latency correction

The old pack-smoke workflow intentionally waited the entire 180-second server timeout and entire 120-second client timeout even after readiness had already been proven. Tooling commit `71a54560e18cd60dd1b3f572c1d6d332845f0c07` changes future runs to:

- poll for the same fatal signatures and readiness markers,
- keep a short post-ready soak,
- terminate the isolated game process group after readiness,
- retain the original 180s/120s values only as startup deadlines rather than mandatory waits.

That tooling commit used `[skip ci]`; Build #137 still used the previous deliberately slow smoke implementation. The optimized workflow must therefore be considered **CODE REVIEWED, NOT YET WORKFLOW-RUNTIME VERIFIED** until the next meaningful gameplay checkpoint uses it.

Future Augmentation Survival batches should also use `[skip ci]` on intermediate file commits and allow only the final meaningful checkpoint commit to trigger the pack smoke workflow.

## Remaining implementation set

### Partial — 1

- `권능 탈취`
  - Add a faithful persistent MCEndgame dynamic-boss combat snapshot/translation instead of inventing a generic fixed spell.
  - Then perform live combat/VFX validation across Chrono Dawn, World Bosses and MCEndgame packages.

### Explicitly underspecified / blocked — 8

Do not invent values for these until canon fixes the missing boundaries:

- `손재주`
- `물약 확산`
- `탄성 저장`
- `탄도 장막`
- `임시 발판`
- `생명 연결`
- `작아지기`
- `벽치기`

## Validation labels

- CODE REVIEWED: **YES**
- TESTED: **NO — workflow test task is `NO-SOURCE`; no dedicated automated gameplay test was executed**
- BUILD VERIFIED: **YES — `3066907ead902e48af72bc2ac30081b94752888d`**
- JAR PRODUCED: **YES**
- DEDICATED SERVER SMOKE: **PASS**
- CLIENT BOOT SMOKE: **PASS**
- LIVE GAMEPLAY TESTED: **NO**
- STATISTICAL PROBABILITY PLAYTESTED: **NO**
- MULTIPLAYER TESTED: **NO**

Documentation and CI-only commits after the gameplay checkpoint are not separately gameplay-build-verified SHAs.
