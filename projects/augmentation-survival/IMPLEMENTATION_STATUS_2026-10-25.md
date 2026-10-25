# Augmentation Survival — Implementation Status

Date: 2026-10-25
Supersedes: `IMPLEMENTATION_STATUS_2026-10-24.md`
Status: M1C FUNCTIONAL BASELINE / 135-OF-146 GAMEPLAY RUNTIME / ABUNDANCE EXACT-DROP BOUNDARIES VERIFIED

## 146-roster implementation count

This count measures whether the canonical gameplay effect has a runtime implementation. It does **not** mean final VFX/UI/audio polish or live gameplay testing is complete.

- Fully implemented gameplay runtime: **135 / 146**
- Partially implemented: **1 / 146** — `분신`
- Not fully implemented: **11 / 146**
- No gameplay runtime yet: **10 / 146**
- This pass advances **`풍요` to FULL** and reconciles **`휩쓸기 강화` as already FULL** from its earlier verified runtime.

The previous status count was stale in two places. `휩쓸기 강화` already had its vanilla sweep expansion and Better Combat adapter runtime implemented and full-pack smoke-verified in Build #81, so it must not remain in the no-runtime set. `분신` gained a real custom entity/server-AI runtime after the previous status checkpoint, but its current presentation still uses a fixed Steve humanoid renderer and does not yet reproduce the owner's skin/equipment presentation, so it is tracked as PARTIAL rather than FULL.

## Newly verified FULL runtime

### `마법 공명`

Runtime enchantment-effect calculations see the stored enchantment level as actual level +1 / +2 / +3 according to augment level without rewriting the item enchantment data. The elevated level may exceed vanilla maximums. Vanilla enchantments are eligible by default; external enchantments remain opt-in through an explicit safe allow-list.

The 26.2 runtime boundaries include direct equipment enchantment queries, equipment attribute modifier collection, combat/armor modifiers, mining/Fortune context, durability/XP-repair helpers, projectile count/spread, fishing enchant effects, crossbow and trident consumers.

### `길들이기 요령`

Actual species-specific taming probability boundaries are modified rather than applying a generic post-success reroll. Wolf, cat and nautilus use their real `tryToTame` zero-roll boundary; parrot uses the tame roll inside `mobInteract`; horse-family taming uses the shared `RunAroundLikeCrazyGoal` temper/max-temper check.

The probability transform preserves each species' original rule while multiplying final success probability by ×1.25 / ×1.50 / ×2.00, capped at 100%.

### `보물 낚시`

The previous generic luck-equivalent approximation has been removed. The runtime now computes the vanilla 26.2 fishing selection probability using the current fishing luck and open-water condition, then applies an extra pre-roll so the final treasure probability is exactly the canonical **+2 / +4 / +6 percentage points**.

A successful correction runs the vanilla `FISHING_TREASURE` loot table itself, so treasure contents remain vanilla/datapack-defined. This stays separate from `낚시 감각`, `풍요`, player luck and Luck of the Sea.

### `전리품 챙기기`

The former next-tick death-position item scan has been removed. `LivingEntity.dropFromLootTable` now wraps only the killed entity's final entity-loot-table `Consumer<ItemStack>`, so one canonical **5% / 10% / 15%** roll duplicates that exact loot-table bundle.

This closes the provenance issue: nearby drops, another mob's drops, equipment drops and unrelated custom death drops cannot be captured accidentally. Player victims, dominated companions, configured bosses and augment cores remain excluded.

### `풍요`

The former delayed position scan is removed. `풍요` now duplicates the exact final compatible ordinary-drop stack at the producer boundary rather than finding nearby `ItemEntity` objects after the fact.

- Direct mining / direct harvest: the server player owning the active `destroyBlock` call is tracked, and eligible final stacks emitted through `Block.popResource` are copied exactly once. `ResourceHarvestService`-marked derived breaks such as additional tree-felling, tunneling and sweeping-harvest breaks are excluded so the canonical direct-action boundary is preserved.
- Fishing: the actual final fishing loot list is duplicated after the existing treasure-selection correction. The fishing loot table is not re-rolled.
- Normal-mob direct kill: the killed entity's exact final loot-table `Consumer<ItemStack>` is wrapped. Root-direct player provenance is required; player victims, dominated companions and configured bosses are excluded.
- Ordinary-drop eligibility excludes augment cores, explicit `augmentation_survival:abundance_excluded` tagged items, non-stackable/damageable equipment and stacks carrying custom data/name/lore. The empty exclusion tag is a data-driven extension point for compatible progression or unique rewards.

This removes nearby-drop contamination and preserves the already-computed final stack itself. Third-party content that bypasses the normal block/fishing/entity-loot producer boundaries still requires an explicit compatibility adapter rather than being silently guessed.

## Existing verified FULL runtime clarification

### `휩쓸기 강화`

`휩쓸기 강화` is not a remaining no-runtime augment. Its runtime was already implemented before this status pass and full-pack smoke-verified in Build #81.

- Vanilla: only an already-valid vanilla sweep is expanded; non-sweep attacks do not gain a new arbitrary AoE. Horizontal candidate range is increased by +0.5 / +1.0 / +1.5 and secondary sweep damage is multiplied by ×1.25 / ×1.50 / ×1.75.
- Better Combat 3.2.2: optional/reflection-based adapter expands client target selection and server range/angle validation, revalidates secondary geometry server-side, preserves direct-vs-secondary classification and applies the same secondary damage multiplier.
- Canonical angle bonus remains +20° / +40° / +60°.

Live combat feel and multiplayer behavior are still not playtested; that does not make the gameplay runtime absent.

## Verified gameplay checkpoint

- Gameplay-code checkpoint: `c315f2d110005e151f683997f1de65fa8a33bfe2`
- Commit: `fix(augmentation-survival): bind abundance to exact final drops`
- Includes the preceding custom clone/server-AI runtime present at `51ea6ce660a8f8b5390bdc2f91fc602abadd5f50`
- GitHub Actions: `40162348266` / Build Augmentation Survival Pack #96
- Build and pinned-pack inspection: **PASS**
- Pack dependency inspection: **PASS — 23 pinned external artifacts**
- Runtime classpath verification: **PASS — 150 files**
- Dedicated server smoke: **PASS — 92 mods**
- Client boot smoke: **PASS — 108 mods**
- Verification artifact upload: **PASS**
- Artifact: `augmentation-survival-pack-smoke-96`
- Artifact ID: `12587231044`
- Artifact ZIP size: `974619` bytes
- Artifact ZIP SHA-256: `bf503703598a64499a9f634842bc1a6bf037c6f7c8e51f6dd1f1af74adf5079d`

The headless client reported expected environment-only narrator/audio warnings (`flite` / ALSA/OpenAL) and existing external-mod translation/texture warnings, but no fatal augmentation/mixin startup signature occurred and the client remained healthy through the smoke timeout. The dedicated server reached ready state and the client reached the expected renderer/resource initialization markers.

## Remaining implementation set

### Fixed-semantics no-runtime — 2

- `공간 연결`
- `권능 탈취`

These remain large systems and must not be reduced to temporary commands or generic fake boss powers.

### Partial — 1

- `분신`

Current clone runtime is real rather than a zombie/armor-stand shell: it uses a registered custom `CombatCloneEntity`, server-owned autonomous targeting/melee AI, 20-second lifetime, activation-time 50% max-health snapshot, 60% attack-damage snapshot, 60-second cooldown, owner/companion ally checks and companion damage provenance so clone damage does not count as the player's root direct hit.

It remains PARTIAL because the current client renderer is still a fixed Steve humanoid baseline and does not yet reproduce the owner's actual skin/equipment presentation. Its combat snapshot/presentation must also be reviewed against the full intended player combat read before the augment is promoted to FULL. Build #95 and the later Build #96 prove pack startup/build compatibility, not visual or combat playtest quality.

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
- BUILD VERIFIED: **YES — `c315f2d110005e151f683997f1de65fa8a33bfe2`**
- JAR PRODUCED: **YES**
- PACK DEPENDENCY INSPECTION: **PASS — 23**
- RUNTIME CLASSPATH: **PASS — 150 files**
- DEDICATED SERVER SMOKE: **PASS — 92 mods**
- CLIENT BOOT SMOKE: **PASS — 108 mods**
- LIVE GAMEPLAY TESTED: **NO**
- STATISTICAL PROBABILITY PLAYTESTED: **NO**
- MULTIPLAYER TESTED: **NO**

Documentation commits after the gameplay checkpoint are docs-only and must not be treated as a separately build-verified gameplay SHA.
