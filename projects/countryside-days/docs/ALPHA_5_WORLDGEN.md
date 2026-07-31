# Countryside Days alpha.5 world-generation contract

- Superflat Overworld only.
- Countryside terrain is produced during chunk world generation, before a chunk reaches the client.
- Runtime player-proximity terrain repainting is prohibited.
- Vanilla superflat village fragments inside generated chunks are cleared before countryside decoration.
- Roads, river, bridges, fields, hedges, meadow vegetation and trees are deterministic from world coordinates.
- The central village contains a restaurant, kitchen garden, well, cottages, barn, market, orchard, paddock and pond.
- All `Enemy` entities, including slimes, are rejected in the superflat countryside.
- Death keeps player inventory and equipment.
- The objective HUD remains a compact one-line ribbon.

A new superflat world is required when moving from alpha.4 because already-saved malformed chunks are not regenerated.
