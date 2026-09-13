# M6 World Encounter Lifecycle

This document records the implemented server-authoritative lifecycle contract for authored world Encounter anchors.

## Repeatability contract

An authored world anchor is repeatable only when both data layers allow it:

```text
RegionDefinition.EncounterAnchor.repeatable
AND
EncounterDefinition.repeatable
```

If either value is false, the locator is a one-time encounter for that player.

Completion identity is the logical anchor `locator`, not the EncounterDefinition id. This allows one EncounterDefinition to be reused by multiple fixed-world anchors without clearing unrelated placements.

## Victory settlement

A one-time anchor is completed only after its battle reaches victory and the reward claim settles successfully.

The reward grant and completed locator are composed into one immutable `PlayerProgress` state and written through `TurnboundProgressSavedData.put` once. There is no reward-only persisted intermediate state and no separate world-completion SavedData.

Defeat does not clear the anchor. Repeatable anchors never add a completion locator.

## Save schema

`PlayerProgress` schema 2 adds:

```text
completedEncounterLocators: Set<String>
```

Schema 1 remains readable. Missing completion data defaults to an empty set. Any accepted progression/reward mutation upgrades the produced immutable state to the current schema while preserving completion locators.

## Server validation

Preview and final confirm both consult the same access policy. A completed one-time anchor returns `ANCHOR_CLEARED`; the Challenge action is unavailable and the client shows a localized cleared message.

Final confirm still revalidates locator, dimension, entity UUID/tag, distance, encounter identity, active battle state, party state, and completion state on the server.

## Current production data

The current representative locators remain repeatable and were not changed for this implementation pass:

- `turnbound_re:region_01/overworld_patrol`
- `turnbound_re:region_01/rift_elite`

One-time behavior is covered by synthetic contract tests until authored fixed-world content intentionally declares a non-repeatable anchor.

## Validation boundary

Automated validation can prove codec compatibility, state preservation, access policy, captured battle source metadata, build success, and JAR integrity.

Actual fixed-world placement, one-time encounter feel, and post-victory world presentation still require Minecraft playtest once a production non-repeatable anchor exists.
