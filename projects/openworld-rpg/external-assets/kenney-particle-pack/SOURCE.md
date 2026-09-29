# Kenney Particle Pack — Rebuke support textures

Player-facing use: Cleric `Rebuke` short-lived 3D holy fan support material.

Authoritative source page: https://kenney.nl/assets/particle-pack

Authoritative source page state checked on 2026-09-29:

- title: Kenney Particle Pack
- category: 2D / VFX
- tile size: 512 x 512
- files: 80
- license: Creative Commons CC0

Exact byte corroboration source used for the committed PNGs:

- repository: `shorepine/kenney`
- pinned commit: `3694c6879e487c108f55677be7dd2ca75b07cc3b`
- `2d/Particle Pack/PNG (Transparent)/light_03.png`
  - Git blob: `2ca26ca733eb4d2ff70b9e04aacf606a9e0222c0`
  - SHA-256 previously recorded by the project's Phase-F audit: `b68627734d08aab3eaf3f94718521553628c75aace1de6c74de28501c1a4c147`
- `2d/Particle Pack/PNG (Transparent)/magic_03.png`
  - Git blob: `65f69d649a290e5c4e292efb288f457f8323d6b3`
  - SHA-256 previously recorded by the project's Phase-F audit: `c2ef5fe86cd2fd08e5b9768389ab6580b346c391db3c7c09fc9b0a63e00ec4ce`

Runtime outputs:

- `assets/openworld_rpg/textures/spell/rebuke_light_kenney.png`
- `assets/openworld_rpg/textures/spell/rebuke_magic_kenney.png`

The PNGs are used as texture/support layers only. Rebuke's attack identity is the project-authored 3D fan volume whose length/width constants are shared with the server hit geometry. These generic Kenney inputs are not promoted as a reusable signature-boss VFX family.
