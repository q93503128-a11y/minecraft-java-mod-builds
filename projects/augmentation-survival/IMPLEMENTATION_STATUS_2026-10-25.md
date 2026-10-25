# Augmentation Survival — Implementation Status

Date: 2026-10-25
Supersedes: `IMPLEMENTATION_STATUS_2026-10-24.md`
Status: **146-OF-146 FULL GAMEPLAY RUNTIME / FINAL-EIGHT CHECKPOINT VERIFIED**

## 146-roster implementation count

This count measures whether each canonical augment has a gameplay runtime implementation. It does **not** mean final VFX/UI/audio polish or live gameplay testing is complete.

- Fully implemented gameplay runtime: **146 / 146**
- Partially implemented: **0 / 146**
- No gameplay runtime: **0 / 146**
- Not fully implemented: **0 / 146**

The previous `138 / 146` status was held back by stale documentation that incorrectly described the final eight as unresolved. The implementation now has runtime coverage for all eight and the old blocked classification is retired.

## Final eight — verified runtime coverage

### `손재주` (`dexterity`) — FULL

- Server-side direct block-break boundary.
- Applies only to the data-driven `augmentation_survival:dexterity_gathering` whitelist tag.
- Eligible non-combat gathering drops receive exactly one additional stackable drop at **10% / 20% / 30%**.
- The whitelist is data-driven so compatible gatherables can be extended without changing Java code.

### `물약 확산` (`potion_diffusion`) — FULL

- Hooks the Minecraft 26.2 `LivingEntity.completeUsingItem` consumable boundary rather than the removed old `PotionItem.finishUsingItem` path.
- Only a directly consumed stack carrying `POTION_CONTENTS` can start diffusion.
- Only beneficial effects are copied.
- Radius: **8 blocks**.
- Continuous effect duration: **25% / 40% / 55%** of the source duration.
- Instant beneficial effect magnitude: **25% / 40% / 55%**.
- Valid recipients are allied players/teams plus player-owned or dominated combat companions.
- Copied effects do not recurse because diffusion is only triggered by the direct item-consumption boundary.

### `탄성 저장` (`elastic_storage`) — FULL

- Tracks actual server fall distance rather than post-mitigation fall damage.
- A real fall of at least **4 blocks** stores the fall for **3 seconds**.
- The next ordinary ground jump consumes the stored value.
- Level coefficients return **30% / 60% / 100%** of the stored fall-force conversion.
- Water/climb interruptions clear the current fall observation rather than fabricating a landing.
- The velocity conversion is data-driven in `final_augment_contract.json` so physical feel can be tuned without changing the gameplay contract.
- Landing on `임시 발판` still records the actual fall before fall damage is cancelled, preserving the intended interaction.

### `탄도 장막` (`ballistic_barrier`) — FULL

- Server-owned periodic projectile scan only; no every-tick full-radius scan.
- Radius: **6 blocks**.
- Pulse interval: **3.0 / 2.5 / 2.0 seconds**.
- Destroys at most **1 / 2 / 3** nearest hostile projectiles per pulse.
- Projectile hostility is based on the owner relationship. Player/allied/reflected projectiles are not destroyed as hostile shots.
- Includes restrained shield-block feedback when a projectile is intercepted.

### `임시 발판` (`temporary_platform`) — FULL

- Registered in the shared active-slot router.
- Creates a **3×3** temporary platform beneath the player without replacing occupied world blocks.
- Base duration: **6 seconds**.
- Base cooldown: **8 seconds**.
- Landing on a tracked platform cancels fall damage only; actual fall distance remains observable to `관성 전환` / `탄성 저장`.
- Exact placed positions are server-tracked and removed on expiry only when the temporary platform block is still present.
- Integrates with the existing non-Mythic active rules instead of inventing a second cooldown system:
  - `능력 가속`
  - `이중 충전`
  - `전투 순환`
  - `연장전`
  - `재연`

### `생명 연결` (`life_link`) — FULL

- A valid root-direct hit creates one current link group for that player.
- Group = directly hit hostile plus nearest compatible hostiles, maximum **4 total**.
- Link radius: **8 blocks**.
- Duration: **6 seconds**.
- When any linked target takes compatible damage, **40%** of that actual damage is copied to the other living linked targets.
- Shared damage runs under dedicated `CombatProvenance.LIFE_LINK`, so it cannot recursively re-share itself or satisfy root-direct kill requirements.
- `SPECIAL_PERCENT_HP` damage is explicitly excluded from sharing, preserving the `천벌` contract.

### `작아지기` (`shrink`) — FULL

- Holding crouch for the configured short hold changes the player's actual `SCALE` attribute to **50%** and refreshes dimensions.
- The default data contract currently uses a **1.0-second / 20-tick** hold boundary.
- Visual size, collision box and combat hitbox therefore use the same underlying Minecraft scale.
- Releasing crouch attempts to restore normal size.
- If the restored hitbox would collide, the player remains safely shrunk instead of suffocating inside blocks.
- `거대화` and `작아지기` are prevented from persisting as stacked scale modifiers; entering gigantification removes the shrink modifier and performs a nearby vertical safety correction when required.

### `벽치기` (`wall_slam`) — FULL

- A root-direct melee hit records one server-owned knockback collision window for that target.
- Only the player who created that qualifying knockback owns the window.
- Horizontal solid-block collision within the window consumes it once.
- Additional damage is **30% / 60% / 100%** of the originating direct attack reference damage.
- The default data contract currently uses a **1.0-second / 20-tick** collision window and a small minimum horizontal-motion threshold to reject stationary/environmental contacts.
- Derived wall damage is non-root provenance and cannot create another wall-slam event.
- Increased knockback such as `거대화` naturally improves the chance of reaching a wall without creating a special-case duplicate damage path.

## Shared final-eight data contract

`src/main/resources/data/augmentation_survival/augmentation/final_augment_contract.json` now owns the repeatedly tuneable boundaries for the final eight. Runtime code validates the contract at startup through `FinalAugmentTuning`.

This avoids burying balance/feel constants across independent services and allows later playtest tuning without redesigning the effects.

## Other major FULL runtime checkpoints retained

### `분신`

- Custom `CombatCloneEntity`, server AI, 20-second lifetime, 50% activation max-health snapshot, 60% combat snapshot, 60-second cooldown.
- Actual owner skin/model-parts, slim/wide model selection, visible presentation-only equipment and handedness.
- Clone damage remains non-player-root, so it cannot satisfy direct-kill progression or authority-theft requirements.

### `공간 연결`

- Persistent multi-gate network, cross-dimension travel, personal/shared permissions, naming/search/favorites/default destination, physical world anchors and server-authoritative UI protocol.
- Root vehicle/passengers and compatible nearby companions transfer with safe-placement checks.

### `권능 탈취`

- Stable first-direct-kill authority unlocks, dedicated authority key, one 120-second global release cooldown and sequential release of every stolen package.
- Current supported packages: **24** = Vanilla 2 + Chrono Dawn 6 + World Bosses 11 + pinned MCEndgame 1.7.0+26.2 bosses 5.
- Authority damage is non-root `AUTHORITY` provenance and cannot recursively steal new authority.

### `풍요` / `전리품 챙기기` / `휩쓸기 강화`

These remain FULL from their earlier verified checkpoints. Their producer-boundary drop contracts and optional Better Combat sweep adapter were not removed or reduced by the final-eight batch.

## Final-eight verification checkpoint

- Gameplay-code checkpoint: `202ac8cd8119e0353273a88b0ea407e87291d2cc`
- Final gameplay fix: `fix(augmentation-survival): hook potion diffusion at 26.2 consumption boundary`
- GitHub Actions: `40171679262` / Build Augmentation Survival Pack **#142**
- Build and pinned-pack inspection: **PASS**
- Dedicated server smoke: **PASS**
- Client boot smoke: **PASS**
- Verification artifact upload: **PASS**
- Artifact: `augmentation-survival-pack-smoke-142`
- Artifact ID: `12590247465`
- Artifact ZIP size: `1118758` bytes
- Artifact ZIP SHA-256: `536bb116d802ba353f06e3d52eab483d30df280465b4b568d525d92eb574f6e7`

### Failed checkpoints read and corrected

- Build #140 failed during compile on Minecraft 26.2 API differences: corrected instant-effect spelling, the 26.2 dye-indexed stained-glass registry family and the existing replay-tuning owner.
- Build #141 then compiled successfully but failed dedicated-server startup because Minecraft 26.2 no longer executes drinking through `PotionItem.finishUsingItem`. The first real server failure was a Mixin target error. The hook was moved to the verified 26.2 `LivingEntity.completeUsingItem` consumption boundary.
- Build #142 is the authoritative final gameplay checkpoint for this batch and passed the complete pack verification path.

## Current implementation set

There are **no remaining NO-RUNTIME or PARTIAL augments in the 146-entry roster**.

Future work is therefore no longer “implement the missing augment roster.” Remaining work is quality work: actual playtesting, balance tuning, VFX/audio/UI refinement, edge-case validation and multiplayer verification.

## Validation labels

- CODE REVIEWED: **YES**
- TESTED: **NO — Gradle test is `NO-SOURCE`; no dedicated automated gameplay test suite was executed for these eight behaviors**
- BUILD VERIFIED: **YES — `202ac8cd8119e0353273a88b0ea407e87291d2cc`**
- JAR PRODUCED: **YES**
- DEDICATED SERVER SMOKE: **PASS**
- CLIENT BOOT SMOKE: **PASS**
- LIVE GAMEPLAY TESTED: **NO**
- STATISTICAL PROBABILITY PLAYTESTED: **NO**
- MULTIPLAYER TESTED: **NO**

Documentation commits after the gameplay checkpoint are docs-only and must not be treated as separately gameplay-build-verified SHAs.
