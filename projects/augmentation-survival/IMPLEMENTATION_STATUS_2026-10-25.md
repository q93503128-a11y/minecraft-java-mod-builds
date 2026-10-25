# Augmentation Survival — Implementation Status

Date: 2026-10-25
Supersedes: `IMPLEMENTATION_STATUS_2026-10-24.md`
Status: M1C FUNCTIONAL BASELINE / 138-OF-146 FULL GAMEPLAY RUNTIME / AUTHORITY THEFT BOSS-COVERAGE CHECKPOINT VERIFIED

## 146-roster implementation count

This count measures whether the canonical gameplay effect has a runtime implementation. It does **not** mean final VFX/UI/audio polish or live gameplay testing is complete.

- Fully implemented gameplay runtime: **138 / 146**
- Partially implemented: **0 / 146**
- Not fully implemented: **8 / 146**
- No gameplay runtime yet: **8 / 146**
- `공간 연결` and `권능 탈취` are now FULL for canonical gameplay runtime coverage.
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

Vanilla sweep expansion and the optional Better Combat 3.2.2 adapter were already implemented and full-pack smoke-verified in Build #81. Live combat feel and multiplayer behavior remain untested.

### `분신`

`분신` has the canonical gameplay runtime rather than the earlier partial attack-attribute approximation.

- Registered custom `CombatCloneEntity`; no zombie or armor-stand shell.
- Server-owned autonomous hostile-target melee AI.
- 20-second lifetime, activation-time 50% max-health snapshot and 60-second base cooldown.
- Activation-time offensive snapshot includes the player's current attack attribute plus the project's target-independent outgoing combat package used by `keen_edge`, `last_strength`, `speed_conversion`, `brawl`, `glass_cannon` and the currently active Berserk bonus before applying the canonical 60% clone multiplier.
- Target-dependent future-enemy conditions are deliberately not frozen into the activation snapshot.
- Copied hand/armor stacks are presentation copies: functional attribute modifiers and enchantments are removed from the clone copy while visual glint can be retained, preventing double application of combat stats.
- Owner UUID and player-model-part mask are synchronized; actual owner skin lookup, wide/slim `PlayerModel` selection, hat/jacket/sleeve/pants toggles, visible equipment and handedness are retained.
- Equipment drop chance is zero and item pickup is disabled.
- Clone damage provenance remains non-player-root, so clone combat cannot satisfy root-direct kill requirements such as boss-first-kill or authority theft.

### `공간 연결`

`공간 연결` has both the persistent server-owned network and a world-readable permanent gate object.

- Multiple permanent gates with stable IDs and save-wide persistent Fabric attachment state.
- Unlimited-distance and cross-dimension travel.
- Personal gates plus explicit per-player sharing/revocation.
- Naming, search, favorites and per-player default destination.
- Dedicated rebindable `K` key and server-authoritative snapshot/action protocol.
- Search-first control screen with own/shared distinction, favorite/default markers, pagination and register/rename/delete/share/revoke/default/teleport workflows.
- Destination chunk loading and collision-safe landing; obstructed destinations fail rather than forcing entities into blocks.
- Root-vehicle/passenger transfer plus nearby dominated and player-tamed companion transfer with individual safe-placement checks.
- Registering creates an actual `augmentation_survival:spatial_gate` world anchor beside the player rather than replacing the occupied block.
- The anchor uses a dedicated multi-element world model with dark frame, amethyst pylons and luminous core and is system-created rather than exposed as a normal placeable item.
- Right-clicking the gate opens the Spatial Link network.
- Gate deletion removes the physical anchor; travel can restore a missing saved anchor when its saved block is clear.
- Registration, deletion and travel have portal/beacon/teleport feedback.

`공간 연결` is **FULL for canonical gameplay runtime coverage**. Physical model quality, teleport feel, UI composition, mount/companion behavior and multiplayer permissions still require real playtesting.

### `권능 탈취`

`권능 탈취` now covers every boss identity authored by the current compatibility profile and the pinned boss-content dependencies, so the Mythic runtime is no longer partial.

Core runtime:

- Exact root-direct player kill provenance is required.
- The first supported direct boss kill permanently adds a stable authority ID to `stolen_authority_boss_ids`; repeated kills do not duplicate it.
- Dedicated rebindable `J` key; authorities do not consume any of the ten normal active slots.
- One activation releases **all currently stolen supported boss packages in sequence**.
- One overall 120-second server-owned cooldown is applied after a successful release.
- Authority damage runs under `CombatProvenance.AUTHORITY`, so stolen-power damage cannot recursively satisfy root-direct boss-kill or authority-theft requirements.
- Damage analogs reuse the project's combat snapshot boundary instead of introducing a separate unscaled damage progression path.

Current supported package coverage: **24 boss packages**.

#### Vanilla — 2

- Wither — three-target Wither pressure, close detonation and regeneration pulse.
- Ender Dragon — charge/knockback pressure and dragon-breath frontal cone.

#### Chrono Dawn — 6

- Time Guardian
- Chronos Warden
- Clockwork Colossus
- Temporal Phantom
- Entropy Keeper
- Time Tyrant

These packages mirror the inspected pinned 26.2 boss identities: temporal slow/blink/AoE pressure, stone stance/ground slam, overcharge/repair/gear pressure/shockwave, phase/warp/blink/temporary echoes, decay/rot/burst and Time Tyrant stop/acceleration/AoE/reversal-style recovery.

#### World Bosses — 11

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

#### MCEndgame 1.7.0+26.2 — 5

The pinned MCEndgame release was inspected directly. Its authored dungeon boss pool is Arachne, Bonecrusher, Elf Duelist, Beakburn and Beastweaver. The compatibility layer still uses MCEndgame's runtime `mcendgame$isDungeonBoss` accessor as the boss gate, but Authority Theft now resolves the killed entity's registry ID into a stable per-boss authority package rather than storing one generic MCEndgame ID.

- Arachne — web-shot pressure, source three-pull hook behavior and 4x3 frontal melee sweep identity.
- Bonecrusher — teleport pressure, unblockable slam and spin pressure.
- Elf Duelist — rapid blade chain/backflip motion plus the source 50% projectile-deflection behavior with the inspected 100-degree threshold.
- Beakburn — pounce, Wind Burst, 70-degree Flame Breath and source-style fire-geyser field.
- Beastweaver — beast swipe, elephant stomp, vine/control pressure, wolf-pack echoes and rhino-charge pressure; summoned player analog echoes use the inspected wolf health/damage identity.

The MCEndgame package mapping is based on release commit `fb562d8ae1562b266908e556e1cba3573b5b1d7f` (`Release 1.7.0`) corresponding to the project's pinned 26.2 generation, not the later 26.3 port.

`권능 탈취` is therefore **FULL for canonical gameplay runtime coverage**. This does not claim exact original boss-entity replication, final VFX/audio polish, or live combat feel; these are player-usable authority analogs preserving the inspected boss identities and major combat boundaries.

## Verified gameplay checkpoint

- Gameplay-code checkpoint: `0c87eb22fa4fb581008db0408a0fe94d8617c9c7`
- Feature commit: `4bacef6a6673cdb5e679ca897f42dbe0c5e5b8a3` — `feat(augmentation-survival): complete authority boss package coverage`
- Final gameplay fix: `0c87eb22fa4fb581008db0408a0fe94d8617c9c7` — `fix(augmentation-survival): align authority effects with 26.2 APIs`
- GitHub Actions: `40169415333` / Build Augmentation Survival Pack #139
- Build and pinned-pack inspection: **PASS**
- Dedicated server smoke: **PASS**
- Client boot smoke: **PASS**
- Verification artifact upload: **PASS**
- Artifact: `augmentation-survival-pack-smoke-139`
- Artifact ID: `12588997703`
- Artifact ZIP size: `1068027` bytes
- Artifact ZIP SHA-256: `38957ef89b32963ec9136e871d81714e0bed9e7d59b3b46cb73436c5448dac03`

Build #138 failed during compile on two Minecraft 26.2 API type boundaries: `DRAGON_BREATH` requires a `PowerParticleOption`, and `GENERIC_EXPLODE` is a registry holder requiring `.value()` at this call site. The first real failure was read from the job log, both issues were fixed together, and Build #139 passed the complete pack verification path.

## CI latency correction

The smoke workflow optimization from `71a54560e18cd60dd1b3f572c1d6d332845f0c07` is now **workflow-runtime verified by Build #139**.

- Server smoke watches the existing fatal signatures and readiness markers, keeps a short post-ready soak, and terminates the isolated game process group after readiness.
- Client smoke likewise exits after a real client-initialization marker and short soak.
- The original 180s/120s values remain startup deadlines rather than mandatory waits.
- Future implementation batches should use one meaningful gameplay checkpoint for CI rather than repeatedly triggering the full pack smoke for tiny edits.

## Remaining implementation set

### Partial — 0

There are no remaining partially implemented augments in the 146-entry roster.

### Explicitly underspecified / blocked — 8

These are the only remaining no-runtime augments. Their missing canonical boundaries must be fixed before implementation; do not invent values merely to increase the completion count.

- `손재주` — gathering whitelist
- `물약 확산` — radius
- `탄성 저장` — block distance → propulsion mapping
- `탄도 장막` — period / range / per-cycle projectile-destruction count
- `임시 발판` — duration / cooldown
- `생명 연결` — auto-link conditions / target count / range / duration / share ratio
- `작아지기` — crouch hold duration
- `벽치기` — collision time / criteria

Once those eight boundaries are canonically fixed, they can be implemented as one large batch rather than one augment per checkpoint.

## Validation labels

- CODE REVIEWED: **YES**
- TESTED: **NO — no dedicated automated gameplay test suite exists for this checkpoint**
- BUILD VERIFIED: **YES — `0c87eb22fa4fb581008db0408a0fe94d8617c9c7`**
- JAR PRODUCED: **YES**
- DEDICATED SERVER SMOKE: **PASS**
- CLIENT BOOT SMOKE: **PASS**
- LIVE GAMEPLAY TESTED: **NO**
- STATISTICAL PROBABILITY PLAYTESTED: **NO**
- MULTIPLAYER TESTED: **NO**

Documentation commits after the gameplay checkpoint are docs-only and must not be treated as separately gameplay-build-verified SHAs.
