# Augmentation Survival — World Bosses raid cache core bonus

Date: 2026-11-06
State: APPROVED DESIGN / RUNTIME INTEGRATION NOT IMPLEMENTED

## User gameplay decision
The 2.0.2 World Bosses raid-cache opening experience should occasionally award an additional Augmentation core. Preserve the exact third-party rewards, their GUI, consumption behavior, and rarity roll. Do not turn an ordinary structure chest or a reusable menu-open action into a core farm.

## Drop contract
Current World Bosses 11 profiles are `boss_major`: guaranteed personal core with Epic 80% / Legendary 20%, plus first-kill bonus.

The cache should be able to roll **one grade above** that direct reward ceiling: Mythic. Conservative starting configuration per actually consumed/claimed World Bosses raid cache:

- Bonus-core chance per successful claim: 10% (not a guaranteed core).
- Grade within successful bonus: Epic 72.0%; Legendary 27.5%; Mythic 0.5%.
- Unconditional Mythic probability: 0.05% per eligible cache (approximately 1 in 2,000), before any future measured tuning.
- Exactly one bonus roll per consumed cache; bonus cores are additional to rather than replacements for the original World Bosses loot.
- Never give a core merely for opening, previewing, closing, or reopening the loot GUI.
- No second draw when the same transaction is replayed, including network retries, item transfers, disconnects and reconnects.
- Server alone decides the result and persists any pending grant necessary for crash-safe exact-once behavior.
- Existing personal boss-kill core reward is unchanged.

## Required integration gate
World Bosses 2.0.2 is All Rights Reserved; do not bundle or rewrite its JAR. Its raid cache is an item with a custom reward-opening flow, not necessarily a vanilla loot-table chest. To wire the bonus in production, inspect the precise pinned external binary or a supported public extension point and verify the **successful server-side claim/consumption boundary**.

The current repo does not contain this confirmed callback or a verified safe mixin target. A generic LootTableEvents hook, right-click event, inventory-count polling, or GUI-open hook must **not** be presented as a successful raid-cache integration: they can miss the actual claim or duplicate rewards.

## Verification before release
1. Confirm the exact 2.0.2 cache item identifiers and server claim method, with and without a loot result.
2. Check successful claim exactly once, preview-only, canceled claim, inventory full, offhand, death/disconnect, and multiplayer concurrency.
3. Verify fixed-seed core bonus odds and all six grades' item IDs; normal direct boss rewards stay untouched.
4. Check actual GUI presentation with the user's installed version before calling the reward added to the cache.
5. Produce new JAR and complete pinned pack only after the runtime callback is demonstrably correct.

No game code has been changed to grant this cache bonus. This document records the user's decision and the remaining integration work without claiming a playable feature.
