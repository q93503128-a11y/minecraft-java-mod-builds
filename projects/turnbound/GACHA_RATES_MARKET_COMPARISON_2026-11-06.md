# TURNBOUND — Gacha Rate Reference Study (2026-11-06)

**Status: External comparison ONLY. No new drop rates are approved or implemented.**
Project: TURNBOUND `turnbound-content-rebuild`; not TURNBOUND: RE.

## Current user direction versus fixed decisions

- CONFIRMED: one of three ordinary native ★3 free starters; summon pool native ★1–★5; ★6 obtainable by character promotion only.
- CONFIRMED: a 10-pull action grants at least one ★4-or-higher; every exact cumulative individual draw number divisible by 50 grants a ★5, with **no reset after an early ★5**; single pulls also increment the fixed 50 counter.
- CONFIRMED: summon currency primarily comes from optional contracts and dungeon rewards.
- PROPOSED, NOT CONFIRMED: user is considering **about a 5% baseline chance for native ★5**. An earlier assistant proposal ★1 15%, ★2 25%, ★3 40%, ★4 15%, ★5 5% is NOT approved.
- Unsettled: all precise base rates, costs, earn speed, pool size, duplicates/roster distribution, and whether ten singles should receive the ★4+ guarantee (the present contract only explicitly guarantees a 10-pull batch).

## Externally verified examples

| Product & pool | Highest relevant base chance | Guarantee | Notes and quality of source |
| --- | ---: | --- | --- |
| Genshin Impact, Character Event Wish | ★5 ~0.6% | ★5 within 90 wishes; resets when any ★5 is won | HoYoverse Help Center confirms the 90/reset behavior; 0.6% is the ordinary in-game wish-detail published base rate, not the rate inclusive of pity. Source https://support.hoyoverse.com/hc/en-us/articles/50333940684953-How-does-the-Wish-guarantee-system-work |
| Honkai: Star Rail, Character Event Warp | ★5 0.6% | ★5 at least once per 90 warps; ★4+ every 10 | HoYoverse official help https://support.hoyoverse.com/hc/en-us/articles/50913710696217-How-does-the-pity-system-work-for-different-banners-and-reruns |
| Wuthering Waves, Featured Resonator Convene | ★5 0.8% | ★5 at least once per 80; ★4+ every 10 | Published game banner rules, https://wutheringwaves.gg/convene-details/ . The current banner's current data should be checked in-game for final claims |
| Arknights, Standard/Orienteering Headhunting | ★6 2%; ★5 8% | ★6 probability increases after 50 consecutive failures | Official event description dated 2026-10-23: https://arknights.global/news/4980 ; official explanation of escalating chance and reset after ★6: https://www.arknights.global/news/393 |
| Goddess of Victory: NIKKE, certain pick-up recruiting pools | SSR 4%, pick-up character 2% | 200 mileage tickets can be exchanged for named SSR; miles persist across events | 2026 update rules reproduced in https://nikke.gg/august-13-patch-notes/ ; distinct from guaranteed random SSR on draw #200 |
| Azur Lane, Heavy/Special permanent construction (post-2024 UR addition) | SSR 7%; UR 1.2% | UR exchange for 400 exchange points; not the same as a standard SSR pity counter | Official developer update https://azurlane.yo-star.com/news/2024/01/17/maintenance-notice-1-18-12-a-m-utc-7-2/ |
| AFK Journey, ordinary All-Hero Recruitment | S tier 0.72% base (2.05% consolidated) | S-tier upper threshold 60, with A-tier character guaranteed for 10-pull | Third-party game reference https://www.prydwen.gg/afk-journey/guides/banners-and-rates/ |
| AFK Journey, special Epic Recruitment | S tier 3.43% base (5.22% consolidated) | S-tier threshold 30 | Distinct, special-currency banner, not comparable as a universal default. Same Prydwen source |

**Comparability warning:** UR, SSR, ★5, ★6 and S-tier are different rarity ladders. A game can also draw weapons/materials, be monetized, have many time-limited banners or require multiple duplicates. Thus copying one percentage is not automatically good design for TURNBOUND's finite, native ★1–★5 roster and duplicate bonus-level choice.

Roblox R_PG was identified as a reference for visible field encounters (official game description: https://www.roblox.com/games/10145990490/R-PG ). No reliable, public, developer-published summon rarity/rate table was verified during this search, so **do not invent R_PG's gacha percentages**.

## Implications of user-suggested 5% base with the confirmed non-resetting 50-draw milestone

Assumptions: 49 ordinary independent ★5 opportunities at 5% each, and the 50th guaranteed ★5. The 10-pull ★4+ policy **does not downgrade any naturally rolled ★5** and fills missing ★4+ with ★4, so it does not change the simple expected ★5 count.

- Chance of at least one *naturally rolled* ★5 in 10 pulls: `1 - 0.95^10 = 40.13%`. This excludes any guaranteed draw #50 falling inside those ten.
- Chance of any naturally rolled ★5 in first 49 attempts: `1 - 0.95^49 ≈ 91.9%`. The milestone at draw #50 is predominantly an additional guaranteed copy, not the player's first ★5.
- Mean ★5 outcomes in each block of 50 individual draws: `49 * 0.05 + 1 = 3.45`. Long-run observed ★5 frequency `3.45 / 50 = 6.90%` (ignoring special banners).
- Scenario sensitivity:

| Baseline ★5 chance | Expected ★5 per 50 including fixed #50 | Effective ★5 rate |
| --- | ---: | ---: |
| 3% | 2.47 | 4.94% |
| 4% | 2.96 | 5.92% |
| 5% | 3.45 | 6.90% |

These are **theoretical expected values**, not a playtest or user-authorized odds. Exact duplicate frequency depends heavily on initial number and distribution of distinct ★5 units.

## Design observations (analysis, not canon)

1. A 5% base is much more generous than common banner rates for Genshin/Star Rail/Wuthering Waves; NIKKE's relevant SSR banner is at 4%; Azur Lane's SSR at 7% exists *below* a rarer UR tier. The chosen fixed non-resetting #50 reward makes TURNBOUND still more generous.
2. If the initial ★5 roster is tiny, 5% + scheduled #50 will generate duplicates quickly, which can make the planned +bonus-level/Star-Essence system dominate new-character collection. Check roster size and character distinctness before pinning 5%.
3. Since every lower native rarity can be promoted to current ★6, power should be balanced across identity/kit and effort as well as stars; an easy ★5 draw need not eliminate the reason to raise ★1/★2 units.
4. The exact cost of 10 draws and rewards per contract/dungeon matters as much as the drop rate. Evaluate **play-hours per 10 pulls**, **time to full functional four-actor party**, and **duplicate value** together.
5. Contrast with commercial games' retention mechanics: TURNBOUND is a Minecraft survival RPG; monetization-driven low odds, time-limited FOMO and daily chores are not automatically appropriate.
6. The guarantee specification and pool-wide draw counter remain binding **regardless of eventual choice of baseline ★5 rate**.

Next co-design choice: present the 3/4/5% alternatives to the user, and establish how many distinct ★5 characters are expected to be in the first release pool before locking the five-level distribution. No change to GachaService, runtime, save, CI or client has been made by this documentation.
