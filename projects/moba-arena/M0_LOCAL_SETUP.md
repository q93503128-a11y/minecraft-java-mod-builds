# MOBA Arena — M0 Local Runtime Preflight

This is the execution handoff for the donor-only M0 gate.

M0 does **not** bootstrap `moba-arena` gameplay code. Its only purpose is to prove that the adopted external runtime actually works before any bridge or port is written.

## 1. Required platform

- Minecraft: `1.19.2`
- Forge: `43.3.13`
- Java runtime for the Forge instance: **Java 17**
- Project runtime lock: `M0_RUNTIME_LOCK.json`

The exact donor JAR pins are defined only in `M0_RUNTIME_LOCK.json`. Do not substitute newer or similarly named files during M0.

## 2. Local-only directory

Create this directory under `projects/moba-arena/`:

```text
local/m0/mods/
```

Put the five M0 donor JARs there with their original filenames:

```text
AnimeAssembly+1.1.4.jar
geckolib-forge-1.19-3.1.40.jar
player-animation-lib-forge-1.0.2.jar
Pehkui-3.8.2+1.19.2-forge.jar
Kleiders Custom Renderer API 6.0.0 1.19.2.jar
```

`SmartBrainLib-forge-1.19.2-1.9.jar` is pinned for M2, not required for the donor-only M0 boot.

The `local/` directory is intentionally ignored by Git. Do not commit third-party JARs or restricted map bytes.

## 3. Original source pages

Use the source URLs already pinned in `M0_RUNTIME_LOCK.json`.

The current M0 files are CurseForge file IDs:

- Anime Assembly: `7514535`
- GeckoLib: `4407241`
- playerAnimator: `4418149`
- Pehkui: `5393090`
- Kleiders Custom Renderer API: `5083496`

Do not use mirrors when the original source remains available.

## 4. Run the automated intake audit

From `projects/moba-arena/` on Windows:

```bat
tools\m0-preflight.cmd
```

Or directly:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\m0-preflight.ps1
```

The audit does not modify the donor JARs. It will:

- verify every required M0 filename exists;
- compute SHA-256 and size;
- verify each file is a readable JAR/ZIP;
- capture `META-INF/mods.toml` and manifest metadata when present;
- inventory mod IDs and versions visible in `mods.toml`;
- record the active Java version;
- extract Anime Assembly class-name candidates related to MOBA/team/shop/start/ready/skills/health bars/minimap/network/UI;
- write reports under `local/m0/report/`.

Expected outputs:

```text
local/m0/report/M0_LOCAL_FINGERPRINTS.json
local/m0/report/ANIME_ASSEMBLY_MODS_TOML.txt
local/m0/report/ANIME_ASSEMBLY_MANIFEST.txt
local/m0/report/ANIME_ASSEMBLY_SYMBOL_CANDIDATES.txt
```

A successful fingerprint audit is **not** a successful runtime smoke test.

## 5. Donor-only Forge instance

Create a clean Minecraft `1.19.2` instance with Forge `43.3.13` and Java 17. Put only the five M0 donor JARs in that instance's `mods/` folder.

Do not add OptiFine, performance mods, shaders, unrelated content mods, SmartBrainLib, or project source during the first boot. The point is to isolate donor compatibility.

If the clean profile fails to boot, preserve:

- `latest.log`;
- crash report if generated;
- the fingerprint JSON;
- exact Java version;
- exact Forge version.

Do not randomly upgrade dependencies until the first failure is understood.

## 6. Primary local MOBA map

Anime Assembly links a modified Summoner's Rift map. The parent map credits Shinkiroo and states that editing/distribution is not allowed. No separate permission for redistribution of the modified copy has been verified.

Therefore:

- use the map only as a local runtime file for this project;
- do not commit the world archive/folder;
- after obtaining it, place the local copy under `local/m0/world/` or import it into the M0 instance;
- record its SHA-256 and actual world-folder name in the local report before map binding work;
- never rebuild or crop the map for player count.

## 7. Manual M0 acceptance

After a clean donor-only boot, execute every item in `M0_FEATURE_CHECKLIST.md`.

M0 passes only when the profile proves the externally supplied player-facing layer that the project plans to depend on. A menu existing is not enough; the relevant interaction must work in-game.

## 8. After M0 passes

Only after the fingerprint audit and donor feature checklist pass:

1. copy the verified SHA-256 values into `M0_RUNTIME_LOCK.json`;
2. create `docs/ANIME_ASSEMBLY_SYMBOL_MAP.md` from the actual JAR inventory and observed runtime state;
3. bootstrap the Forge project skeleton;
4. implement one narrow `AnimeAssemblyBridge` against verified symbols;
5. then proceed to M2 minion work using the already selected external donors.

Do not write a second character engine, shop, health-bar system, skill engine, or minimap to bypass an M0 problem.