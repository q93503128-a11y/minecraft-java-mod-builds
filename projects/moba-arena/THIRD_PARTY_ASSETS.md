# MOBA Arena — Third-Party Asset & Code Ledger

This ledger tracks material that actually moves from research into project/runtime use.

`EXTERNAL_SOURCES.md` is the research catalog. `IMPLEMENTATION_BLUEPRINT.md` is the planned integration map. This file is the intake/provenance record.

## Repository boundary

The game is intended for private personal play, but this GitHub repository is public.

Therefore:

- do not commit third-party bytes unless redistribution is established;
- keep restricted maps/assets/JARs local;
- store source, expected identity and checksums instead of prohibited bytes;
- preserve license/NOTICE/modification requirements for code ports;
- do not assume a mod's project license grants rights to every franchise/trademark/third-party asset represented inside that mod;
- re-audit before any public game distribution.

## Record template

```text
Name:
Category: code / runtime / map / UI / model / texture / animation / VFX / audio / other
Author:
Source:
Version / Commit / File / File ID:
SHA-256:
License / Usage Terms:
Use Mode: dependency / port / bundled asset / local-only asset / reference
Modified:
Modification Summary:
Redistributable in this public repo: yes / no / unclear
Attribution / NOTICE Required:
Used In:
Local Install Path:
Notes:
```

## Locked for M0 intake — bytes not yet admitted

The following are **planned**, not proof that files have been downloaded, checksum-verified or committed:

### Anime Assembly 1.1.4

- Category: runtime / character / combat / UI / animation / VFX
- Source: https://www.curseforge.com/minecraft/mc-mods/anime-assembly/files/7514535
- File: `AnimeAssembly+1.1.4.jar`
- File ID: 7514535
- Project-listed license: AFL-3.0
- Use mode: local/direct dependency
- Public-repo bytes: **NO by default**; dependency metadata only until rights are re-audited
- SHA-256: **PENDING M0**

### GeckoLib 3.1.40

- Category: runtime library
- Source: https://www.curseforge.com/minecraft/mc-mods/geckolib/files/4407241
- File: `geckolib-forge-1.19-3.1.40.jar`
- License: MIT
- Use mode: dependency
- SHA-256: **PENDING M0**

### Pehkui 3.8.2

- Category: runtime library
- Source: https://www.curseforge.com/minecraft/mc-mods/pehkui/files/5393090
- File: `Pehkui-3.8.2+1.19.2-forge.jar`
- License: MIT
- Use mode: dependency
- SHA-256: **PENDING M0**

### playerAnimator 1.0.2

- Category: runtime library
- Author/project: KosmX `playerAnimator`
- Source: https://www.curseforge.com/minecraft/mc-mods/playeranimator/files/4418149
- File: `player-animation-lib-forge-1.0.2.jar`
- CurseForge project/file ID: `658587` / `4418149`
- License: MIT
- Use mode: dependency
- Public-repo bytes: dependency metadata preferred; bundle only if packaging terms and need are deliberately re-audited
- SHA-256: **PENDING M0**

### Kleiders Custom Renderer API 6.0.0

- Category: runtime renderer
- Author: kleiders3010
- Source: https://www.curseforge.com/minecraft/mc-mods/kleiders-custom-renderer-api/files/5083496
- File: `Kleiders Custom Renderer API 6.0.0 1.19.2.jar`
- CurseForge project/file ID: `682065` / `5083496`
- License: All Rights Reserved
- Declared required dependencies: none on the CurseForge relations page
- Use mode: local/direct dependency
- Public-repo bytes: **NO** unless explicit redistribution permission is later established
- SHA-256: **PENDING M0**

### SmartBrainLib 1.9

- Category: AI runtime library
- Source: https://github.com/Tslat/SmartBrainLib
- Branch/commit: `1.19.2` / `3d1263fe39bc96c84fe920632208e8958d24b13f`
- License: MPL-2.0
- Use mode: direct dependency
- SHA-256: **PENDING M0**

### Anime Assembly modified MOBA map

- Category: map
- Modified-copy source: Anime Assembly project page `Moba Mode Map Download`
- Modified-copy Google Drive file ID: `1tL4A1RIjUULe7tJy2tRBjI1AFwGsqipW`
- Parent author: Shinkiroo
- Parent source: https://www.planetminecraft.com/project/re-league-of-legend-summoner-s-rift-download/
- Parent title: `League of Legends Summoner's Rift (Pre-Season 10) [DOWNLOAD]`
- Parent usage terms found on source page: editing and distributing are not allowed; author requests notification for YouTube/public-server use
- Separate permission for Anime Assembly's modified copy: **NOT VERIFIED**
- Use mode: **local-only candidate**
- World folder identity / SHA-256: **PENDING M0**
- Public-repo/package bytes: **NO** under current evidence
- Allowed repo record: source identity/Drive ID, checksum after local intake, expected folder identity, and project-created metadata coordinates

## Code donors approved for port provenance — no donor code copied yet

### SimpleLaneWars / c0mbit/mc-dota

- Category: code
- Source: https://github.com/c0mbit/mc-dota
- Commit: `cacd3625b8a0066d6085bbaa0c81a18ac58254fc`
- License: MIT
- Planned use: narrow wave/minion identity/reward port
- Actual copied/modified files: **NONE YET**

### cadox8/LoM

- Category: code
- Source: https://github.com/cadox8/LoM
- Commit: `5ae2b4b747989dc74ebe1af17869a11879cceecb`
- License: Apache-2.0
- Planned use: structure/team state and fallback shop-data port
- Actual copied/modified files: **NONE YET**

## UI candidates — not yet admitted

### Kenney UI Pack family

- Category: UI assets
- Sources:
  - https://kenney.nl/assets/ui-pack
  - https://kenney.nl/assets/ui-pack-rpg-expansion
  - https://kenney.nl/assets/pixel-ui-pack
- License: CC0
- Planned use: missing map/team/result screens if visual fit passes actual Minecraft screenshot review
- Downloaded/admitted: **NO**

## Validation state

No third-party bytes or ported donor source are claimed to be integrated yet.

Metadata identification is now complete for the previously unresolved playerAnimator and Kleider runtime pins, but that is not a runtime test.

- dependency version/file IDs: **PINNED FOR M0**
- dependency checksums: **PENDING**
- runtime compatibility: **NOT TESTED**
- code ports: **NOT STARTED**
- map source/parent terms: **AUDITED; LOCAL-ONLY UNDER CURRENT EVIDENCE**
- map bytes/folder/checksum: **NOT INTAKEN**
- UI visual acceptance: **NOT DONE**
