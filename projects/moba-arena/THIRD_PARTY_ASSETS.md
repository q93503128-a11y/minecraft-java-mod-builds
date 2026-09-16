# MOBA Arena — Third-Party Asset & Code Ledger

This ledger tracks material that moves from candidate status into actual project use.

`EXTERNAL_SOURCES.md` is the research/candidate catalog. This file is the adoption/provenance record.

## Repository boundary

The project is intended for private personal play, but the GitHub repository is public.

Therefore:

- do not commit third-party bytes unless redistribution is actually permitted;
- for local-only maps/assets, store source and installation metadata only;
- do not upload paid or access-controlled content;
- preserve required notices/licenses for redistributable material;
- re-audit everything before any future public game distribution.

## Record template

```text
Name:
Category: code / map / UI / model / texture / animation / VFX / audio / other
Author:
Source:
Version / Commit / File:
License / Usage Terms:
Use Mode: dependency / port / bundled asset / local-only asset / reference
Modified:
Modification Summary:
Redistributable in this public repo: yes / no / unclear
Attribution Required:
Used In:
Local Install Path (if applicable):
Checksum (if applicable):
Notes:
```

## Adopted material

None yet. This project has only completed the initial design/source-audit setup.

Candidates listed in `EXTERNAL_SOURCES.md` are **not** considered adopted merely by being named there.

## Visual-production rule

For final player-facing visual categories, this ledger must identify a usable external source before production implementation is accepted.

Required categories include:

- arena map;
- HUD/menu art;
- icons;
- playable-character visuals;
- animations;
- VFX;
- audio;
- custom minion/structure visuals when applicable.

If a category remains unresolved, do not replace it with project-invented final art.
