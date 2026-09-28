# Open-World RPG — R01 Furniture / Property Permission Pass 15

> Status: **R01 FURNITURE SEMANTICS + PROPERTY PERMISSIONS CLOSED / PLAYER-FACING FURNITURE STILL VISUAL+SPATIAL GATED**
>
> Date: 2026-09-28

## Closed in this pass

The exact R01 Household catalogue is now represented in code:

- Alderford Bed — 180 Gold
- Storage Cabinet — 140
- Plain Table — 90
- Alderford Chair — 60
- Iron Lantern — 35
- Wall Shelf — 60
- Trophy Stand — 90
- Wardrobe — 160
- Cooking Hearth — 220
- Woven Rug — 70
- Wooden Bench — 85
- Side Table — 50

The optional starter package remains exactly 750 Gold and resolves to:

- bed x1
- Storage Cabinet x1
- Plain Table x1
- Alderford Chair x2
- Iron Lantern x2
- Wall Shelf x1
- Trophy Stand x1

The 60-Gold `shelf/cabinet` package line resolves to the 60-Gold Wall Shelf catalogue entry because the separate Storage Cabinet is already its own 140-Gold package line.

## Placement contract

The non-visual placement authority now enforces the closed R01 rules:

- 0.25-block horizontal snap;
- exactly 0/90/180/270-degree rotation;
- per-furniture authored support-surface classes;
- accepted-model collision box with 0.05-block horizontal inflation;
- owned furnishing-volume containment;
- protected shell collision rejection;
- placed-furniture collision rejection;
- doorway/critical-interaction clearance rejection;
- functional interaction-approach preservation.

It deliberately requires production shell geometry and an accepted external furniture visual/collision binding. It does not invent placeholder model boxes.

## Multiplayer permission contract

Shared property authority now distinguishes:

- Owner
- Trusted Decorator
- Guest

Rules:

- Owner may furnish and access private Home Storage.
- Trusted Decorator may furnish only after explicit owner grant.
- Trusted Decorator does not receive private Home Storage access automatically.
- Private Home Storage access is a separate explicit grant and requires Trusted Decorator role.
- Guest may enter/use non-private furniture but may not mutate furnishing or access private storage.
- Removing Trusted Decorator also removes that player's storage grant.
- Moving residence clears permissions on the old property and the newly acquired vacant property starts clean.

Permission fields were added with backward-compatible defaults for existing housing saves.

## Still gated

The following are intentionally not claimed:

- accepted exact furniture models/material variants;
- production property interior/furnishing volumes;
- real placement collision boxes/approach sockets;
- live Household purchase UI;
- starter-package live purchase transaction;
- placed furniture entity/block runtime;
- real two-client permission/grief tests.

```text
FURNITURE CATALOGUE RULES: IMPLEMENTED
STARTER PACKAGE SEMANTICS: IMPLEMENTED
PLACEMENT VALIDATION CONTRACT: IMPLEMENTED
OWNER/TRUSTED/GUEST AUTHORITY: IMPLEMENTED
PRIVATE STORAGE GRANT SEPARATION: IMPLEMENTED

FURNITURE VISUAL BINDING: NO
LIVE FURNITURE PLACEMENT: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
