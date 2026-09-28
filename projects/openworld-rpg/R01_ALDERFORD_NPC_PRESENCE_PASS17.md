# Open-World RPG — R01 Alderford NPC Presence Pass 17

> Status: **NON-VISUAL NPC PRESENCE / SERVICE-RELEVANCE / PATHFINDER-BUDGET AUTHORITY IMPLEMENTED**
>
> Date: 2026-09-28

## Closed in this pass

The exact R01 Alderford presence contract is now represented as server-side rules:

- service relevance radius: 12 blocks;
- on-duty service override when a legal service has a relevant nearby player;
- personal quest return/turn-in relevance can force the NPC to the service/turn-in position independently of ordinary ambient scheduling;
- service override remains latched for 20 seconds after the last relevant player leaves;
- committed story/event scenes may temporarily supersede the service position;
- after the scene stabilizes, a still-relevant service/turn-in immediately regains priority;
- Alderford loaded-core humanoid pathfinder cap: 15;
- activation priority:
  1. required scene/quest/service named NPC;
  2. Alderford Watch guards;
  3. other nearby named ambient NPCs;
  4. unnamed townsfolk;
- Kest Arden remains event/hunt-owned and never becomes a generic looping Alderford service NPC;
- no vanilla villager/golem fallback is introduced.

## Schedule handling

The four canon time bands are represented against Minecraft day time:

- Morning 06:00–10:00;
- Day 10:00–18:00;
- Evening 18:00–22:00;
- Night 22:00–06:00.

Named schedules are preserved for Mara Venn, Elian Rook, Daren Holt, Lysa Fen, Toma Reed, Brin Hale, Nessa Bell, Oren Quill, Sera Wren, Ilyan Voss and Kest Arden.

Where canon deliberately leaves a choice unresolved, this pass keeps a semantic schedule slot instead of inventing a coordinate. Examples:

- Daren Evening = Forge exterior **or** Copper Kettle;
- Mara Evening/Night = Copper Kettle / Wayfarers interior;
- Sera Evening = Copper Kettle / route board;
- post-clear Ilyan Morning/Night = not explicitly fixed by canon.

Those exact ambient transforms remain presentation/spatial binding work.

## Current boundary

This is not physical NPC implementation yet.

The runtime still requires:

- accepted NPC actor/model bindings;
- accepted animation clips;
- production service interaction anchors from Pass 16;
- authored ambient/service/quest anchor coordinates;
- actual pathfinding/entity adapter;
- real multiplayer relevance testing.

```text
NPC PRESENCE RULES: IMPLEMENTED
SERVICE OVERRIDE/HYSTERESIS: IMPLEMENTED
QUEST TURN-IN OVERRIDE: IMPLEMENTED
15-PATHFINDER PRIORITY RULE: IMPLEMENTED
NAMED SCHEDULE SEMANTICS: IMPLEMENTED

PHYSICAL NPC ACTORS: NO
PRODUCTION NPC ANCHORS: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

## Asset acquisition note

The official OpenGameArt pages for the Quaternius Medieval Village MegaKit Standard and Fantasy Props MegaKit Standard were re-verified in this session as creator uploads under CC0 with 176 and 94 Standard models respectively.

The current execution environment still could not materialize the official ZIP binaries. Therefore archive acquisition and SHA-256 remain unclaimed; no mirror is promoted as an official acquisition source.
