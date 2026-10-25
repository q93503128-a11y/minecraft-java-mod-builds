# Augmentation Survival — Implementation Status

Date: 2026-10-25
Status: M1C FUNCTIONAL BASELINE / 127-OF-146 GAMEPLAY RUNTIME / PERCEPTION + COMPANION + HEALING + ENCHANT PASS VERIFIED

## 146-roster implementation count

The count below measures whether the canonical gameplay effect has a runtime implementation. It does **not** mean final VFX/UI polish or live playtesting is complete.

- Fully implemented gameplay runtime: **127 / 146**
- Partially implemented: **3 / 146** — `전리품 챙기기`, `보물 낚시`, `풍요`
- Not fully implemented: **19 / 146**
- No gameplay runtime yet: **16 / 146**
- **This combined source pass advances 8 augments to FULL compared with the prior documented 119-runtime checkpoint.**
- Current-source FULL reclassification / prior-main completion: `지배`, `영역 가속`
- Newly implemented in the current gameplay batch: `광석 눈썰미`, `상자 찾기`, `위험 감지`, `소생`, `마법 새로고침`
- Partial -> FULL completion: `회복 증폭`
- Newly partial: **0**
- Shared systems materially extended: normalized companion hostility, area-acceleration machine/growth routing, server-authoritative perception snapshots, bounded client ore scanning, entity/block outline presentation, permanent-companion death snapshot/revival, external non-natural healing interception, and enchant-table refresh transaction/UI.

The earlier 106-runtime checkpoint understated the implementation count by one: `피해 적응` was already fully consumed by `ProtectionAugmentService` and `CombatTuning`. That correction remains part of the total; it was a source-consumer reclassification, not new gameplay code.

## Verified code checkpoint

- Verified gameplay-code checkpoint: `952ccf0e82ff2126da97d3d50f8005e253477363`
- Prior-main subsystem commit: `9106253660a4bbb826b353468c59ade94a67d037` — `feat(augmentation-survival): normalize domination hostility and add area acceleration`
- Prior-main cleanup commit: `9b2319a0b2ea0cea3a64409c938eb1f05fe4044f` — `fix(augmentation-survival): remove obsolete domination filter mixin`
- Primary feature commit: `33af5d1b69aa0ef7ac39052218057283d5f76fb5` — `feat(augmentation-survival): implement perception revive healing and enchant refresh batch`
- Minecraft 26.2 client-API alignment: `952ccf0e82ff2126da97d3d50f8005e253477363` — `fix(augmentation-survival): align 26.2 perception and enchantment client APIs`
- GitHub Actions: `40151151557` / Build Augmentation Survival Pack #76
- Build and pinned-pack inspection: PASS
- Pack dependency inspection: PASS — 23 pinned external artifacts
- Runtime classpath verification: PASS — 150 files
- Dedicated server smoke: PASS — 92 mods
- Client boot smoke: PASS — 108 mods
- Verification artifacts upload: PASS
- Artifact: `augmentation-survival-pack-smoke-76`
- Artifact ID: `12582606783`
- Artifact ZIP size: 862779 bytes
- Artifact ZIP SHA-256: `51adb1defd47de53af5b33e7985372ae67277c1eeade5e7c1a41959710922c6f`
- Canonical augment catalog initialization: PASS — 146 definitions
- Entity compatibility contract initialization: PASS — 8 exact selectors / 11 token selectors / 1 dynamic adapter
- Enemy scaling contract initialization: PASS — 9 stages / 4 native exact profiles / 0 namespace profiles
- Stat / survival / harvest / consumption / combat / expansion / movement / projectile / reactive / utility contracts: BUILD + FULL-PACK BOOT VERIFIED
- Advanced utility contract: BUILD + FULL-PACK BOOT VERIFIED
- Special active contract and custom damage type: BUILD + FULL-PACK BOOT VERIFIED
- Advanced active contract: BUILD + FULL-PACK BOOT VERIFIED
- Advanced world/control contract: BUILD + FULL-PACK BOOT VERIFIED
- Area acceleration / companion hostility / perception / revive / external-heal / enchant-refresh contracts: BUILD + FULL-PACK BOOT VERIFIED

Build #75 failed during compile because the first client presentation draft used two stale Minecraft 26.2 boundaries: removed `ShapeRenderer` usage and container-screen position getters not present in the project's mapped `EnchantmentScreen`. No gameplay feature was removed. The corrective commit switched block highlighting to the 26.2 submit-node shape-outline path and placed the enchant refresh control from the screen-init dimensions. Build #76 then passed compile, pinned-pack inspection, dedicated-server smoke, client boot and artifact upload.

Documentation commits after this checkpoint do not change Java gameplay code and must not be treated as new gameplay verification targets.

## Newly verified perception / companion / healing / enchant pass

### `광석 눈썰미` — FULL runtime

The local client performs a bounded scan only when the player owns the augment, every ten ticks, using the canonical 6 / 8 / 10-block radii. Only blocks in the common ore tag with at least one exposed adjacent face are selected. The result uses the shared thin cyan outline presentation rather than changing blocks or spawning entities. The scan never expands beyond the local maximum ten-block sphere, avoiding a server-wide or chunk-wide tick search.

### `상자 찾기` — FULL runtime

The server scans only already-loaded nearby chunks and returns natural randomizable loot containers whose loot table is still present, which corresponds to loot that has not yet been consumed/unpacked. Canonical radii are 12 / 18 / 24 blocks. The client renders the returned positions with the shared gold outline. No new chunks are force-loaded for scanning.

### `위험 감지` — FULL runtime

Every half second the server resolves hostile living entities inside the canonical 8 / 12 / 16-block radius through the shared companion-aware hostility predicate. Dominated allies are therefore excluded. The server sends only entity IDs, and the client applies the red outline through the entity render-state path so walls do not require a gameplay-side glowing/team mutation.

### `소생` — FULL runtime

When a permanent tamed or dominated companion actually dies within twenty blocks of an online owner who owns the augment, the server snapshots the entity state before normal death cleanup and keeps a twenty-second revive marker. The active selects a looked-at marker and runs the canonical three-second focus window. Successful completion recreates the original entity type/state at a collision-safe nearby position with 50% maximum HP. Persistent domination ownership is serialized with the entity snapshot, and the domination occupancy/loaded-companion tracking is restored after revival.

The successful-use base cooldown is 120 seconds and participates in `능력 가속`, sequential `이중 충전` and `전투 순환`. Failed/cancelled focus does not spend the cooldown. The marker excludes the player and temporary clone path by construction. `재연` is excluded because resurrection is not replay-safe.

### `회복 증폭` — FULL runtime

The generic `LivingEntity.heal` boundary now intercepts external non-natural healing received by a server player rather than only healing originated by this mod. Managed augmentation heals carry a recursion guard so they are amplified exactly once. Vanilla hunger/natural regeneration executes under an explicit natural-heal context and remains excluded. Actual HP restored then flows through the existing overflow/share/healing-trigger pipeline once, allowing potion and compatible external-mod heals to use the canonical amplification without duplicate waves or absorption recursion.

### `마법 새로고침` — FULL runtime

Ownership is mirrored to the client only to expose the in-context enchantment-table refresh control; the server is authoritative. Clicking refresh is handled as a distinct menu button transaction. The server verifies ownership, an enchantable item/current offers, at least one lapis and at least five actual XP points, consumes exactly one lapis plus five XP points, replaces the player/menu enchantment seed, and asks the vanilla menu to regenerate the three offers. It does not consume levels as a proxy for five XP points.

## Current-source completions retained from the same combined pass

### `지배` — FULL runtime

The permanent domination runtime attaches an owner UUID to a valid nonboss hostile, marks the mob persistent, stores save-wide occupied companion counts per owner UUID, and releases the slot on actual companion death even when the owner is offline. Caps are 3 / 5 / 10. Dominated entities are allied with the owner and same-owner companions, normal friendly fire is rejected, and loaded dominated mobs are tracked through `ServerEntityEvents` rather than an every-tick full-world scan. Their server AI automatically seeks nearby valid hostile targets.

The former partial limitation is closed: combat, projectile, control and nearby-hostile consumers now route through the shared companion-aware hostility predicate instead of independently treating every `Enemy` implementation as a valid target. Dominated companions no longer consume pull/chain/tracking/nearby-enemy slots merely because their original entity class remains hostile. Boss/script exclusions continue to use the shared compatibility profile.

### `영역 가속` — FULL runtime

The active creates a thirty-second, 24-block server-owned acceleration field. Canonical multipliers are ×3 / ×7 for compatible crop/sapling growth and supported production/processing paths, including furnace/smoker/blast-furnace/brewing and compatible passive growth/production consumers. It does not speed player actions, hostile AI or world time. The base cooldown is 120 seconds and participates in the ordinary non-mythic cooldown/charge/combat-cycle rules. Runtime consumers query the active field at their existing processing/growth boundary rather than running one giant every-tick block-volume search.

## Prior verified advanced world / control pass retained

### `위상 이동` — FULL runtime

Activation creates a five-second phase window. Horizontal and upward solid collision are bypassed while downward collision remains active, so the player cannot fall through floors. Attack, mining, placement, item use and entity/block interaction are locked while phased. The server remains authoritative, and an S2C phase-state payload mirrors the state to the local client so movement prediction uses the same collision rule instead of stopping at the wall first. Ending the effect searches for a nearby non-colliding position and falls back to the recorded start position if necessary.

The twenty-second base cooldown is a non-mythic cooldown active and therefore participates in `능력 가속`, sequential `이중 충전` and `전투 순환`. ROOT-direct hostile kills can also extend the currently running manual phase instance through `연장전`, with the normal per-instance +50% base-duration cap. `재연` is excluded because this is a mobility/state active rather than a replay-safe combat action.

### `거대화` — FULL runtime

The mythic active lasts twenty seconds with a sixty-second cooldown. Player scale, maximum/current health, interaction/melee reach and attack damage use transient attribute modifiers at the canonical level values; health ratio is preserved both when entering and leaving the state. Outgoing player melee knockback is doubled at the vanilla knockback boundary. Level 2 applies the canonical two-second fear to nearby normal nonboss hostiles in an eight-block radius by clearing their target and driving navigation away from the player. Dominated companions and configured bosses are excluded. End-of-state collision is corrected to a safe position when necessary.

As a mythic active, `거대화` deliberately does not participate in ability haste, double charge or replay.

### `금단의 마법` — FULL runtime

The anvil keeps normal item/enchantment applicability rules but relaxes mutual enchantment exclusivity for a player who owns the augment. The vanilla Too Expensive result cap is removed, pickup is allowed when the player actually has the required levels, and both input items' prior-work repair-cost contribution is removed from the calculated level cost. The produced item does not inherit a new prior-work penalty. Base repair, enchantment and naming costs remain; this does not turn arbitrary invalid enchantment/item pairs into valid combinations.

## Prior verified advanced active / interact pass retained

- `되감기`: restores the canonical historical player snapshot without rewinding inventory/world/cooldowns; 60-second cooldown participates in haste/double-charge/combat-cycle; replay excluded.
- `시간 가속`: three-second concentration, twenty-second sustained acceleration, client use-duration prediction and owned-projectile launch-speed scaling; haste/double-charge/combat-cycle integrated.
- `연장전`: ROOT-direct kill extends eligible manual non-mythic sustained actives by +1 second, configured bosses +4 seconds, capped at +50% base duration per use instance.
- `방패 던지기`: weapon-side raw melee basis, canonical target/range/damage scaling, haste/double-charge/combat-cycle and replay integration.
- `흥정`: post-vanilla final emerald price discount 10% / 20% / 30%, minimum one emerald.
- `매달리기`: server collision-validated one-block ledge mantle.

## Prior verified special temporal / mythic pass retained

- `귀환`: five-second cancellable concentration then validated personal respawn teleport, no cooldown.
- `시간 정지`: 32-block radius, nine-second hostile/control freeze, projectile/status-timer pause, boss profile gates.
- `천벌`: 32-block special percent-HP damage, normal 60% / 90%, boss 10% / 25%, Lv.2 two-second stun with boss policy gates.

## Implemented foundations retained

### Runtime / progression / device

- persistent server-owned player augment state with schema-v2 optional `persistent_ints`
- persistent per-augment cooldown-ready ticks
- save-wide threat high-water state
- strict 146-entry canonical runtime catalog
- six core tiers and data-driven core acquisition/source profiles
- structure, hostile, raid, fishing and vanilla-boss core reward baselines
- basic/advanced augment workstations
- three-candidate server transaction
- `두 번째 선택` atomic two-pick
- 3:1 one/bulk core upgrade
- functional client card-selection UI baseline

### Combat / protection / healing / lethal

- unconditional stat contract: 7 definitions / 7 modifier groups
- combat provenance with ROOT/derived/reflected/path/special-percent-HP boundaries
- post-mitigation `피해 적응` / delayed-damage / protection-pool pipeline
- generic non-natural healing amplification + augment-generated primary-healing pipeline
- lethal order: `한 번 버티기` -> vanilla held Totem -> `비상 토템` -> `재림`
- broad combat expansion including `연쇄 폭발`, `벼락`, `메아리 타격`, `약점 표식`, `속도 전환`, `육중한 일격`, `생명 파동`, `난전`, `유리 대포`, `우두머리 사냥`
- reactive/status runtime including `돌려보내기`, `상태 반사`, `강인한 정신`, `보호막 폭발`, `되밟기`, `무적`
- advanced combat bridge includes `방어 무시` and `사냥 기록`

### Movement / projectile / active

- shared movement/aerial runtime includes `숨 고르기`, `야행성`, `햇빛 체질`, `성큼걸음`, `멀리뛰기`, `관성 전환`, `두 번 점프`, `공중 연계`, `물갈퀴`, `발 안 묶이기`, `물살 타기`, `기수`, `물 위 달리기`, `나무타기`, `매달리기`
- projectile shot-action / flight-segment runtime includes `화살 아끼기`, `관통 화살`, `연쇄 화살`, `분산 사격`, `추적 화살`, `도탄`, `집중 조준`
- common 10-slot active runtime with server-authoritative ownership/cooldown/charge/target/result validation
- ordinary cooldown actives include `정화`, `끌어당기기`, `자리 바꾸기`, `보호막`, `광폭화`, `치유 파동`, `방패 던지기`, `되감기`, `시간 가속`, `위상 이동`, `소생`, `영역 가속`
- toggles: `위협 억제`, `순간 채굴`
- one-use death active: `사망 지점 귀환`
- no-cooldown concentration active: `귀환`
- mythic standalone actives include `시간 정지`, `천벌`, `지배`, `거대화`
- active meta runtime: `능력 가속`, `이중 충전`, `재연`, `전투 순환`, `연장전`

### Survival / resource / harvest / consumption / utility

- `광부의 손`, `벌목 요령`, `연속 채굴`, `도구 관리`, `방어구 관리`, `자동 보충`
- `적게 먹기`, `숨 오래 참기`, `추위 적응`, `밤눈`, `회복력`
- `풍작`, `씨앗 챙기기`, `한 조각 더`, `다시 심기`, `싹쓸이 수확`, `나무 베기`, `굴파기`
- `든든한 한 끼`, `낚시 감각`, `빨리 먹기`, `쌍둥이`, `먹으면서 걷기`, `비축 식사`
- `줍는 손`, `방패잡이`, `연료 절약`, `빠른 양조`, `경험 보존`, `소지품 보존`, `흥정`
- perception utility includes `광석 눈썰미`, `상자 찾기`, `위험 감지`
- item/enchantment utility includes `진주 숙련`, `휴대 수류`, `금단의 마법`, `마법 새로고침`
- companion runtime includes permanent `지배` ownership plus `소생` death marker/revival
- `전리품 챙기기`, `보물 낚시`, `풍요` retain functional partial baselines

## Partial augments still open

- `전리품 챙기기`: crowded simultaneous nearby-item provenance remains spatial rather than exact source-owned capture.
- `보물 낚시`: vanilla luck-equivalent adjustment is not mathematically identical to canonical +2 / +4 / +6 percentage points.
- `풍요`: exact final-output provenance and comprehensive unique/progression-item exclusions remain incomplete.

`손재주` remains unimplemented because its non-combat gathering whitelist is not fixed. `물약 확산` remains blocked because the canonical text says “주변” without a fixed radius. `탄성 저장` still lacks a canonical block-distance-to-propulsion mapping. Other still-underspecified mechanics such as `탄도 장막`, `임시 발판`, `생명 연결`, `작아지기`, `벽치기` must not receive invented values.

## Deliberately not claimed complete

- Full roster gameplay completion is **127 / 146**, not 146 / 146.
- This combined eight-augment advancement is build/full-pack boot verified but **not live-player playtested**.
- `광석 눈썰미` / `상자 찾기` / `위험 감지` need in-game visual validation for outline visibility, clutter and modded-content coverage.
- `소생` needs live validation of marker selection, three-second focus feel, save/load ownership restoration, irregular collision-safe revival and double-charge/combat-cycle ordering.
- `회복 증폭` needs a live heal matrix across food natural regeneration exclusion, potions, beacon/external-mod healing, overflow and healing-share recursion guards.
- `마법 새로고침` needs live enchantment-table validation for exact XP-point cost, lapis consumption, candidate regeneration and repeated use.
- `영역 가속` needs live validation across crops, saplings, animals, furnace-family machines, brewing and modded machine compatibility.
- `지배` needs AI/owner-offline/death/save-reload and multiplayer authority testing despite selector normalization being complete.
- `위상 이동`, `거대화` and `금단의 마법` retain their prior live-test gaps.
- Prior active, projectile, movement, threat and external-boss live-test gaps remain.
- Final device/active/combat/resource/threat VFX, sound, models, HUD and UI visual polish are not complete.
- The pre-existing `ProjectileAugmentService` deprecated-API warning remains non-fatal.
- Multiplayer is not tested.

## Batching rule

Do not return to one-augment-at-a-time implementation except for isolated bugs or genuinely unique mechanics.

For every next batch:

1. recompute the locked 146-roster implementation inventory from actual runtime consumers, not only stale docs;
2. report full / partial / not-fully / no-runtime counts;
3. reuse or create one meaningful shared runtime boundary;
4. connect every canonically fixed easy/medium augment that safely fits it;
5. validate once after the meaningful batch;
6. leave only genuinely unique or underspecified mechanics for specialized systems.

## Next implementation direction

The remaining no-runtime set is now **16**. Recompute those 16 from the locked roster and actual runtime consumers before coding. The remaining fixed-semantics items are increasingly system-heavy, so prefer one shared subsystem that can consume several at once — inventory/storage (`주머니`, `원격 창고`), enchantment calculation (`마법 공명`), companion/entity systems (`분신`), world-network/authority systems (`공간 연결`, `권능 탈취`) — while keeping the eight explicitly underspecified/blocked mechanics untouched. `길들이기 요령` must cover the real species-specific tame-probability boundaries rather than faking a generic post-success roll.

## Validation labels

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `952ccf0e82ff2126da97d3d50f8005e253477363`
- JAR PRODUCED: YES — Actions run `40151151557`
- PACK DEPENDENCY INSPECTION: PASS — 23 pinned external artifacts
- PACK RUNTIME CLASSPATH: PASS — 150 files
- PACK SERVER SMOKE: PASS — 92 mods
- PACK CLIENT SMOKE: PASS — 108 mods
- VERIFICATION ARTIFACT: `augmentation-survival-pack-smoke-76` / ID `12582606783`
- VERIFICATION ARTIFACT SHA-256: `51adb1defd47de53af5b33e7985372ae67277c1eeade5e7c1a41959710922c6f`
- CANONICAL AUGMENT CATALOG INITIALIZATION: PASS — 146 definitions
- ENTITY COMPATIBILITY CONTRACT INITIALIZATION: PASS — 8 exact / 11 token / 1 dynamic
- WORLD BOSSES LIVE REGISTRY PROFILE BINDING: PASS — 11 / 11
- MCENDGAME DYNAMIC BOSS ADAPTER BINDING: PASS — 1 / 1
- CHRONO DAWN EXACT BOSS PROFILE DEFINITIONS: PASS — 6
- ENEMY SCALING CONTRACT INITIALIZATION: PASS — 9 stages / 4 native exact profiles / 0 namespace profiles
- DOMINATION + AREA-ACCELERATION RUNTIME: BUILD/BOOT VERIFIED
- PERCEPTION / REVIVE / EXTERNAL-HEAL / ENCHANT-REFRESH RUNTIME: BUILD/BOOT VERIFIED
- NEW COMBINED 8-AUGMENT ADVANCEMENT LIVE-PLAYER TESTED: NO
- PERCEPTION VISUALS LIVE-PLAYER TESTED: NO
- REVIVE LIVE-PLAYER TESTED: NO
- HEALING-AMPLIFICATION LIVE-PLAYER TESTED: NO
- ENCHANT-REFRESH LIVE-PLAYER TESTED: NO
- AREA-ACCELERATION LIVE-PLAYER TESTED: NO
- DOMINATION LIVE-PLAYER TESTED: NO
- EXTERNAL BOSS LIVE ENCOUNTER TESTED: NO
- ACTIVE ENGINE LIVE-PLAYER TESTED: NO
- PROJECTILE LIVE-PLAYER TESTED: NO
- MOVEMENT LIVE-PLAYER TESTED: NO
- DEVICE FLOW PLAYTESTED: NO
- UI VISUAL REVIEW: NO
- BETTER COMBAT GAMEPLAY TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO
