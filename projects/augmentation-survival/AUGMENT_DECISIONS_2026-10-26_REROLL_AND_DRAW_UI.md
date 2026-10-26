# Augmentation Survival — Reroll + Draw UI Decision

Date: 2026-10-26
Status: current decision; this document supersedes older roster-count/UI statements where they conflict.

## 1. Roster update

The canonical roster is now **147 augments**.

- Common 20
- Uncommon 27
- Rare 31
- Epic 40
- Legendary 20
- Mythic 9

New Rare augment:

| Name | Max Lv | Effect |
|---|---:|---|
| 재추첨 | 3 | Each core draw grants 1 / 2 / 3 single-card rerolls. One reroll replaces only the chosen candidate card with another eligible augment of the same tier; the other cards stay unchanged. The replacement cannot duplicate another currently displayed candidate. Candidates already seen in the same offer are avoided while unseen eligible candidates remain. If no replacement exists, the reroll charge is not consumed. |

Rules:

- Rerolls are granted per core offer and do not consume extra cores.
- Closing and reopening the device preserves the pending candidates and remaining rerolls.
- A selected card must be deselected before it can be rerolled.
- `두 번째 선택` remains independent: it changes how many candidates may be acquired, while `재추첨` changes candidate quality.
- Damage, items, augment ownership, candidate replacement, and reroll charges remain server-authoritative.

## 2. Tier-selection screen

The tier-selection screen is not a fixed six-card row anymore.

- Normal width: 3 columns × 2 rows.
- Narrow width: 2 columns × 3 rows; extremely narrow layouts may fall back to one column.
- Cell width/height and gaps are derived from the visible GUI-scaled viewport.
- Each tier cell prioritizes: core icon → tier name → owned core count → selectable/unavailable state.
- The screen explicitly explains that one core produces three candidates of the chosen tier.
- Owned draw modifiers (`재추첨`, `두 번째 선택`) are surfaced without crowding the tier cards.
- Render coordinates and click hitboxes must share the same layout calculation.

## 3. Draw-card visual hierarchy

The existing design contract is now treated as a concrete rendering requirement:

- Common / Uncommon / Rare: tier-color border/accent only.
- Epic: tier color + restrained neon edge.
- Legendary: neon edge + visible halo behind the card.
- Mythic: red-centered palette + stronger animated aura than Legendary.

The aura is decorative only and must not change card hitboxes, cover text, or overlap adjacent cards at compact sizes. Compact layouts reduce the aura rather than allowing collisions.

## 4. Draw interaction

- Three candidates remain the base offer.
- Card body click selects the augment.
- When `재추첨` is owned, each card receives its own reroll control; using it replaces only that card.
- The screen shows remaining reroll charges.
- Shift continues to reveal exact values/details.
- Long names and descriptions are clipped/wrapped using actual Minecraft font width and available card height.
