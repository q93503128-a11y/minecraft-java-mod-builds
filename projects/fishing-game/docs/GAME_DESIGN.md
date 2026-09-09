# Fishing Game — Game Design

## One-line goal

Catch increasingly large and rare fish, sell them, improve your fishing gear, and open better fishing locations while filling your collection.

## Product shape

This is a standalone arcade/progression fishing game. Survival systems are deliberately removed from the player's decision space.

## Core loop

1. Pick a fishing location.
2. Cast.
3. Wait for a bite.
4. Reel with hold/release tension control.
5. Catch a fish with species, rarity, weight, length and value.
6. The fish goes into the persistent catch bag.
7. Sell catches for one currency: coins.
8. Buy the next rod tier.
9. Better rods improve strength, control, luck and lure speed.
10. Move into harder location pools and chase rarer fish.

## Content rules

- One main currency initially.
- No pets, eggs, gacha, runes, crafting chains or unrelated management systems in the core release.
- Common fish may share body families and animation rigs with texture/scale/part variation.
- Legendary fish deserve unique visual treatment and stronger catch feedback.
- Hundreds of fish must not imply hundreds of Java classes; species data belongs in a catalog/data layer.

## Catch bag

The catch bag is separate from Minecraft's survival inventory. Each catch stores:
- species
- rarity (from species definition)
- weight
- length
- sell value

Initial capacity: 40 catches. Selling is intentionally simple: Sell All converts the bag into coins.

## Rod progression

Initial tiers:
1. 갈대 낚싯대 — baseline
2. 호수 전문가 — more strength/control, faster bites, better luck
3. 블루워터 — substantially stronger, wider control margin, higher rare chance

Rod upgrades are profile progression, not crafting recipes.

## Location model

Dedicated fishing maps/locations replace generic survival exploration.

Pools prepared in data/code:
- 청람 호수 — freshwater starter pool
- 갈매기 항구 — coastal pool
- 심해 수로 — high-tier pool

Bundling a community map requires explicit use/redistribution permission. Attractive maps with unclear terms remain reference-only until permission is resolved.

## UI language

Functional structure follows proven fishing-game patterns: persistent compact status HUD, catch bag/collection view, obvious Sell All action, rod card/progression information, and a focused reeling HUD only while hooked.

Visual components use external UI assets rather than improvised black translucent panels. The initial asset family is Kenney UI Pack (CC0).

## Next quality gate

The next user-facing build is only worth testing after the entire cast -> catch -> bag -> sell -> rod upgrade loop is visible and at least one credible dedicated location plus fish-on-hook presentation exists.
