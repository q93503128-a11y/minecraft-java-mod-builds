# Fishing Game — Game Design

## One-line game

Catch fish, sell catches, improve fishing equipment, and hunt larger and rarer species across Minecraft waters while filling a collection.

## Core pillars

1. Fishing feel — casting, bite, reel, tension and landing must feel good.
2. Collection — species, size and rarity create repeat hunts.
3. Progression — rods change what fish are controllable, not only a number.
4. Exploration — water type, biome, depth, time and weather can alter species pools.

## Core loop

```text
find water
-> cast
-> bite
-> reel
-> catch result
-> collection/value
-> sell
-> rod improvement
-> harder/rarer fish
-> repeat
```

## Complexity budget

Initial economy uses one money currency. Do not add pets, eggs, gacha, rune systems, multiple upgrade materials, forced inventory-clearing chores or unrelated crafting until playtesting demonstrates a real need.

## Rod stats

The planned maximum is four primary stats:

- Strength: tolerable fish resistance/weight
- Control: safe tension width and stability
- Luck: rare species/variant chance
- Lure Speed: bite wait time

## Fish production

Common fish should be mass-producible from a small number of body/rig families plus texture, scale, proportion and part variants. Rare and legendary fish receive the expensive unique silhouettes, animation, VFX and sound budget.

## World model

Use Minecraft water rather than building a separate linear map first. Initial environment groups are river/pond, swamp and ocean, later splitting warm/cold/deep water when the collection is large enough.

## Multiplayer authority

Server finalizes:

- valid species pool
- species/size/rarity roll
- reel state and catch success
- catch storage
- money and purchases
- bestiary records

Client owns input, HUD, animation and visual interpolation only.

## Vertical slice

A meaningful first playable slice must eventually include:

- one complete cast/bite/reel/catch loop
- 3 rods
- 12–20 fish species
- 3 body families
- rarity, weight and length
- a visible hooked fish
- one currency and selling
- simple bestiary
- at least 3 environment pools
- multiplayer-safe per-player sessions

The first technical unit in alpha.1 intentionally implements only the session loop and tiny species catalog so the input/tension model can be corrected before content multiplication.
