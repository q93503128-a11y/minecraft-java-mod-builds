# Augmentation Survival — Implementation Status

Date: 2026-10-25
Supersedes: `IMPLEMENTATION_STATUS_2026-10-24.md`
Status: M1C FUNCTIONAL BASELINE / 136-OF-146 FULL GAMEPLAY RUNTIME / SPATIAL LINK + AUTHORITY THEFT ACTIVE

## 146-roster implementation count

This count measures whether the canonical gameplay effect has a runtime implementation. It does **not** mean final VFX/UI/audio polish or live gameplay testing is complete.

- Fully implemented gameplay runtime: **136 / 146**
- Partially implemented: **2 / 146** — `공간 연결`, `권능 탈취`
- Not fully implemented: **10 / 146**
- No gameplay runtime yet: **8 / 146**
- `분신` moved from PARTIAL to FULL in this checkpoint.
- `풍요` is FULL and `휩쓸기 강화` remains FULL from its earlier verified runtime.

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

`분신` now has the canonical gameplay runtime rather than the earlier partial attack-attribute approximation.

- Registered custom `CombatCloneEntity`; no zombie or armor-stand shell.
- Server-owned autonomous hostile-target melee AI.
- 20-second lifetime, activation-time 50% max-health snapshot and 60-second base cooldown.
- Activation-time offensive snapshot now includes the player's current attack attribute plus the project's target-independent outgoing combat package used by `keen_edge`, `last_strength`, `speed_conversion`, `brawl`, `glass_cannon` and the currently active Berserk bonus before applying the canonical 60% clone multiplier.
- Target-dependent future-enemy conditions are deliberately not frozen into the activation snapshot.
- Copied hand/armor stacks are presentation copies: functional attribute modifiers and enchantments are removed from the clone copy while visual glint can be retained. This prevents the displayed weapon/armor from re-applying combat stats on top of the fixed snapshot.
- Owner UUID and player-model-part mask are synchronized; actual owner skin lookup, wide/slim `PlayerModel` selection, hat/jacket/sleeve/pants toggles, visible equipment and handedness are retained.
- Equipment drop chance remains zero and item pickup remains disabled.
- Clone damage provenance remains non-player-root, so clone combat cannot satisfy root-direct kill requirements such as boss-first-kill or authority theft.

This FULL label is for canonical gameplay runtime coverage. Owner-skin fidelity, animation/combat feel and visual quality still require a real client playtest; no visual-playtest claim is made from smoke testing.

## `공간 연결` — PARTIAL major runtime

`공간 연결` now has a real persistent server-owned network rather than a temporary command placeholder.

Implemented in this checkpoint:

- Multiple permanent saved destinations with stable IDs.
- Save-wide persistent Fabric attachment stored on the server.
- Unlimited-distance and cross-dimension destination lookup.
- Personal gates plus explicit per-player sharing/revocation.
- Gate naming, search, favorites and per-player default destination.
- Dedicated rebindable `K` key and client/server snapshot protocol.
- Search-first dedicated control screen with own/shared distinction, favorite/default markers, pagination, register/rename/delete/share/revoke/default/teleport actions and separate registration/rename workflows.
- Server authority for every mutation and teleport action.
- Destination chunk loading and collision-safe vertical landing search; obstructed destinations fail instead of forcing the player into blocks.
- Mount transfer attempts the full root-vehicle passenger graph first.
- Nearby dominated companions and player-owned tame companions are carried across dimensions and placed around the destination with individual collision checks.

`공간 연결` remains **PARTIAL** because the permanent network currently represents gates as saved world locations and a control surface rather than a finished in-world gate object/presentation. A physical/world-readable gate treatment and actual UI/teleport/mount/companion playtest are still required before calling the system visually and experientially complete.

## `권능 탈취` — PARTIAL source-backed runtime

`권능 탈취` now has a dedicated Mythic runtime instead of being no-runtime.

Implemented in this checkpoint:

- Exact root-direct player kill provenance is required.
- Supported boss identity is resolved through the existing compatibility-profile layer.
- The first supported direct boss kill permanently appends that boss authority to the player's existing `stolen_authority_boss_ids` save field; repeated kills do not duplicate it.
- Dedicated rebindable `J` key; the authority channel does not occupy any of the ten normal active slots.
- One activation releases **all currently stolen supported boss packages in sequence**.
- One overall 120-second server-owned cooldown is applied after a successful release.
- Current source-backed package coverage includes all six pinned Chrono Dawn boss identities: Time Guardian, Chronos Warden, Clockwork Colossus, Temporal Phantom, Entropy Keeper and Time Tyrant.
- The player analogs are based on the actual Chrono Dawn 26.2 boss implementations inspected for this checkpoint: temporal slow/blink/AoE pressure, Warden stone-stance/ground-slam pressure, Colossus overcharge/repair/gear-strike/shockwave, Phantom phase/warp/blink/two temporary combat echoes, Entropy decay/rot/burst and Tyrant time-stop/acceleration/AoE/reversal-style recovery.

`권능 탈취` remains **PARTIAL**, not FULL. The pinned World Bosses 26.2 dependency and MCEndgame boss packages still need their real combat implementations inspected deeply enough to reproduce their powers without inventing generic replacements. World Bosses identity detection already exists, but unsupported identities are deliberately not stolen yet. The Chrono Dawn player analogs also require live VFX/combat-feel review.

## Verified gameplay checkpoint

- Gameplay-code checkpoint: `c20282429542395655176dc31996a1fd66e05022`
- Final gameplay commit: `feat(augmentation-survival): localize authority theft fallback`
- GitHub Actions: `40166912027` / Build Augmentation Survival Pack #129
- Build and pinned-pack inspection: **PASS**
- Dedicated server smoke: **PASS**
- Client boot smoke: **PASS**
- Verification artifact upload: **PASS**
- Artifact: `augmentation-survival-pack-smoke-129`
- Artifact ID: `12588753501`
- Artifact ZIP size: `1050898` bytes
- Artifact ZIP SHA-256: `3c26e6df1743f0b173e129e17467bedd6f7a29296f60fbd4064ddbc689b6428e`

Major gameplay commits in this checkpoint include the clone combat snapshot/presentation isolation, persistent Spatial Link state/service/network/UI/mount-and-companion transfer, permanent Authority Theft save mutation, dedicated authority networking/input and the six Chrono Dawn authority packages. Intermediate push-triggered runs are not used as the final verification checkpoint; Build #129 is the authoritative full-pack result for this batch.

## Remaining implementation set

### Partial — 2

- `공간 연결`
  - Finish physical/world-readable gate presentation and real gameplay/UX validation.
- `권능 탈취`
  - Inspect and reproduce the pinned World Bosses and MCEndgame boss power packages without generic fabrication; then perform live combat/VFX validation.

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
- BUILD VERIFIED: **YES — `c20282429542395655176dc31996a1fd66e05022`**
- JAR PRODUCED: **YES**
- DEDICATED SERVER SMOKE: **PASS**
- CLIENT BOOT SMOKE: **PASS**
- LIVE GAMEPLAY TESTED: **NO**
- STATISTICAL PROBABILITY PLAYTESTED: **NO**
- MULTIPLAYER TESTED: **NO**

Documentation commits after the gameplay checkpoint are docs-only and must not be treated as a separately build-verified gameplay SHA.
