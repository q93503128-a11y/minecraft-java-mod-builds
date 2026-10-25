# Augmentation Survival — Implementation Status

Date: 2026-10-25
Supersedes: `IMPLEMENTATION_STATUS_2026-10-24.md`
Status: M1C FUNCTIONAL BASELINE / 133-OF-146 GAMEPLAY RUNTIME / RESONANCE + TAMING + TREASURE + LOOT PROVENANCE VERIFIED

## 146-roster implementation count

This count measures whether the canonical gameplay effect has a runtime implementation. It does **not** mean final VFX/UI/audio polish or live gameplay testing is complete.

- Fully implemented gameplay runtime: **133 / 146**
- Partially implemented: **1 / 146** — `풍요`
- Not fully implemented: **13 / 146**
- No gameplay runtime yet: **12 / 146**
- This pass advances **4 augments to FULL**: `마법 공명`, `길들이기 요령`, `보물 낚시`, `전리품 챙기기`.

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

The former next-tick death-position item scan has been removed. `LivingEntity.dropFromLootTable` now wraps only the killed entity's final entity-loot-table `Consumer<ItemStack>`, so one canonical 10% / 20% / 30% roll duplicates that exact loot-table bundle.

This closes the provenance issue: nearby drops, another mob's drops, equipment drops and unrelated custom death drops cannot be captured accidentally. Player victims, dominated companions, configured bosses and augment cores remain excluded.

## Verified gameplay checkpoint

- Gameplay-code checkpoint: `1511458f1012a84aa586263119a7968de78b1e3a`
- Commit: `fix(augmentation-survival): bind loot scoop to exact death loot`
- Parent combined pass: `75ea70bc643b75b4fa55faa6d861d03b95a9510f` — `feat(augmentation-survival): complete resonance taming and treasure fishing pass`
- GitHub Actions: `40159116605` / Build Augmentation Survival Pack #86
- Build and pinned-pack inspection: **PASS**
- Pack dependency inspection: **PASS — 23 pinned external artifacts**
- Runtime classpath verification: **PASS — 150 files**
- Dedicated server smoke: **PASS — 92 mods**
- Client boot smoke: **PASS — 108 mods**
- Verification artifact upload: **PASS**
- Artifact: `augmentation-survival-pack-smoke-86`
- Artifact ID: `12586095325`
- Artifact ZIP size: `957234` bytes
- Artifact ZIP SHA-256: `7874a21859dcd8dd37a96afc9324372b337de1a644f9010925df1f7ebd6a6d28`

The headless client reported expected environment-only narrator/audio warnings (`flite` / ALSA) and existing external-mod translation/texture warnings, but no fatal augmentation/mixin startup signature occurred and the client remained healthy through the smoke timeout.

## Remaining implementation set

### Fixed-semantics no-runtime — 4

- `분신`
- `공간 연결`
- `권능 탈취`
- `휩쓸기 강화`

`분신` was deliberately not faked with a vanilla zombie/armor-stand shell. Minecraft 26.2 does not provide the later player-like mannequin entity foundation, so its presentation/entity architecture still needs a proper implementation path that meets the project's visual-quality bar.

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

### Partial — 1

- `풍요`

## Validation labels

- CODE REVIEWED: **YES**
- BUILD VERIFIED: **YES — `1511458f1012a84aa586263119a7968de78b1e3a`**
- JAR PRODUCED: **YES**
- PACK DEPENDENCY INSPECTION: **PASS — 23**
- RUNTIME CLASSPATH: **PASS — 150 files**
- DEDICATED SERVER SMOKE: **PASS — 92 mods**
- CLIENT BOOT SMOKE: **PASS — 108 mods**
- LIVE GAMEPLAY TESTED: **NO**
- STATISTICAL PROBABILITY PLAYTESTED: **NO**
- MULTIPLAYER TESTED: **NO**

Documentation commits after the gameplay checkpoint are docs-only and must not be treated as a separately build-verified gameplay SHA.
