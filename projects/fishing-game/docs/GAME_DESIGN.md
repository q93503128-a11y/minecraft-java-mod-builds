# Fishing Game — Game Design

## One-line goal

Catch increasingly large and rare fish, sell them, improve your fishing gear, open better fishing locations, fill your collection, then rebirth into a faster progression cycle.

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
11. Reach Bluewater and the current rebirth coin target.
12. Empty the bag, rebirth, and replay the same proven loop with a permanent sale-speed bonus while collection history remains intact.

## Content rules

- One main currency initially. Rebirth does not add a prestige currency.
- No pets, eggs, gacha, runes, crafting chains or unrelated management systems in the core release.
- Common fish may share body families and animation rigs with texture/scale/part variation.
- Legendary fish deserve unique visual treatment and stronger catch feedback.
- Hundreds of fish must not imply hundreds of Java classes; species data belongs in a catalog/data layer.
- Meta-progression should make the core fishing loop faster/deeper, not create a second chore loop.

## Catch bag

The catch bag is separate from Minecraft's survival inventory. Each catch stores:
- species
- rarity (from species definition)
- weight
- length
- sell value

Initial capacity: 40 catches. Selling is intentionally simple: Sell All converts the bag into coins. The displayed/sold total includes the permanent rebirth sale multiplier; individual catch values remain the base fish values.

## Rod progression

Initial tiers:
1. 갈대 낚싯대 — baseline
2. 호수 전문가 — more strength/control, faster bites, better luck
3. 블루워터 — substantially stronger, wider control margin, higher rare chance

Rod upgrades are profile progression, not crafting recipes. Rod tier resets on rebirth, but a reborn profile keeps a visible rod glint and rebirth count so permanent progression is not represented only by a hidden numeric modifier.

## Rebirth progression

Rebirth is the long-term simulator loop layered directly onto existing rod/coin progression.

- Requirement: maximum rod tier, empty catch bag, no active fishing, and the current rebirth coin target.
- First target: 10,000 C.
- Later target: +1,500 C for each completed rebirth.
- Reset: coins -> 0, rod tier -> 0, catch bag -> empty, location -> Cheongram Lakeside.
- Permanent: species discovery, catch counts, personal-best weight/length, rebirth count.
- Permanent benefit: ordinary fish sale income +30% per completed rebirth.
- First-discovery and location-completion rewards do not reset or scale with rebirth; they remain one-time collection milestones.
- Rebirth is requested by the client but validated and committed by the server.
- The existing catch-bag progression panel becomes the rebirth surface at maximum rod tier; do not add a separate prestige menu just to host one button.

This is intentionally an accelerating loop. Later cycles should return the player to Bluewater faster, while higher rebirth costs prevent the loop from becoming a zero-effort click reset immediately.

## Location model

Dedicated fishing maps/locations replace generic survival exploration.

Pools prepared in data/code:
- 청람 호수 — freshwater starter pool
- 갈매기 항구 — coastal pool
- 심해 수로 — high-tier pool

Bundling a community map requires explicit use/redistribution permission. Attractive maps with unclear terms remain reference-only until permission is resolved.

## UI language

Functional structure follows proven fishing-game patterns: persistent compact status HUD, catch bag/collection view, obvious Sell All action, rod card/progression information, and a focused reeling HUD only while hooked.

Visual components use external UI assets rather than improvised black translucent panels. The initial asset family is Kenney UI Pack (CC0). Rebirth reuses the same progression card and button slot instead of adding another visual language or menu.

## Next quality gate

The next user-facing build is worth testing only as a meaningful loop checkpoint: cast -> catch -> bag -> sell -> upgrade -> travel -> collection -> Bluewater -> rebirth -> visibly accelerated second cycle. Code/build success alone does not certify rebirth pacing, UI readability or game feel.
