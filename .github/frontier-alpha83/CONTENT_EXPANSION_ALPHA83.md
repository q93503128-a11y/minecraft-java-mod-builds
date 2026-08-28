# Frontier Settlement 0.1.0-alpha.83 — Late-game landmark progression

## Goal

Alpha.83 gives the existing settlement loop a real late-game finish line without adding another
management spreadsheet, currency, happiness meter, teleport network, or NPC assignment layer.

The progression remains server-authoritative and derives from physical settlement state that already
exists in the save: completed buildings, population, completed roads, outposts, and exploration
knowledge.

## New physical landmarks

- 시민회관 — opens from Frontier Town with a market and warehouse. It is a large civic build and adds
  8 housing capacity.
- 교역회관 — opens at Domain with a market and cart station. It adds 4 housing and +4 emeralds to each
  existing physical expedition-relic market sale.
- 성채 — opens at Domain with a barracks, watchtower, and exploration score 5. It adds 4 housing and
  extends existing loaded watchtower threat coverage from 40 to 56 blocks.

All three use the existing shared builder. Their wood/stone costs are paid from real ItemStacks staged
through the same construction crate transaction. The blueprints contain no new free storage container
and do not create a second item/resource authority.

## Final tier: 개척 수도

The final tier is derived, not stored separately. Requirements:

- population 20
- at least 5 outposts
- at least 4 completed roads
- 시민회관, 교역회관, 성채 each completed once
- exploration score 7

Reaching the tier upgrades the civic core again and tightens road-lamp spacing from Domain 8 to Capital
6. Domain-only civil works and diversified-territory benefits continue to work at the higher tier.

## UX

The M construction menu gains a dedicated 랜드마크 category.
The in-game guide gains a fifth late-game page.
Server-authored next-goal guidance now continues through the three landmarks and exact Capital
requirements, then reports 개척 수도 완성 instead of falling into an endless generic Domain message.

Alpha.83 also fixes the remaining old guidance text that still said B palette after Alpha.82 moved the
Frontier menu to M.

## Compatibility / authority

- No new SavedData field is added; old worlds derive the new tier from existing records.
- Existing BuildingType ordinals are preserved because all new values are appended.
- Companion versions remain pinned exactly as in Alpha.82.
- No companion Java class, code, texture, UI asset, or world-generation authority is copied.
- No force-load, teleport, virtual resource ledger, auto-selling of ordinary storage, or second worker/
  transport authority is introduced.
- Automated source/build/JAR checks are not graphical-client acceptance; actual Alpha.83 play remains
  to be tested in-game.
