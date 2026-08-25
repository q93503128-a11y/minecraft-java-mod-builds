# Frontier Settlement Companion Testpack

This directory turns `COMPANION_LOCK.json` into an installable local test instance without committing or repackaging third-party JARs.

## Target

- Minecraft Java 26.2
- NeoForge 26.2.0.38-beta
- Frontier Settlement 0.1.0-alpha.73
- Java 25

`COMPANION_LOCK.json` remains `candidate_runtime_lock`. The resolver proves exact file/version/hash resolution; it does **not** mean the full pack has passed the user's real client/server gameplay test.

## Client test instance

From `projects/frontier-settlement/`:

```bash
python3 companion-testpack/install.py \
  --profile client \
  --output frontier-companion-client \
  --frontier-jar build/libs/frontier_settlement-0.1.0-alpha.73.jar
```

The client profile resolves every required lock entry, including Xaero's Minimap.

## Dedicated-server test instance

```bash
python3 companion-testpack/install.py \
  --profile server \
  --output frontier-companion-server \
  --frontier-jar build/libs/frontier_settlement-0.1.0-alpha.73.jar
```

The server profile excludes entries marked `client_preferred`; currently this means Xaero's Minimap. The remaining locked content is resolved exactly.

## Resolver-only verification

CI and maintainers can verify every remote binary without retaining third-party JARs:

```bash
python3 companion-testpack/install.py --profile client --output /tmp/frontier-companion-client --resolve-only
python3 companion-testpack/install.py --profile server --output /tmp/frontier-companion-server --resolve-only
```

Each run writes `resolved-lock.json` with the exact source, locked version, filename and SHA-1/SHA-256/SHA-512 observed for every resolved file.

## Distribution rule

Third-party JARs are fetched directly from their official distribution URLs at install/test time. They are **not** committed to this repository and should not be bundled into a Frontier deliverable ZIP unless the original project's redistribution terms explicitly permit that.

This is important for ARR/custom-license dependencies such as Dungeons and Taverns, Better Combat, Sophisticated Backpacks and Xaero's Minimap. Frontier only stores source/version IDs, official source metadata and cryptographic hashes.

## Deferred content

These remain outside the baseline pack until the required stack is stable in real play:

- Variants & Ventures 1.0.26+mc26.2 — add after baseline spawn-density/compatibility smoke.
- Alex's Mobs Continued — large content gain, but needs a separate stability and spawn-balance pass.

The intended gameplay stack is still: terrain/structures/dungeons/combat/weapons/loot breadth from companions, while Frontier owns the shared settlement, citizens, construction, logistics, territory growth and the exploration-to-settlement feedback loop.
