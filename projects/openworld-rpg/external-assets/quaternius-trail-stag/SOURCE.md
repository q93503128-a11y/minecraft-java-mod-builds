# Quaternius Trail Stag — source / license evidence

> Recorded: 2026-10-13
> Intended use: R01 first permanent mount visual/animation source
> Runtime asset status: **NOT YET PINNED**

## Source-specific license evidence

The selected identity is Quaternius' animated **Stag** from the Animated Animal Pack family.

Exact public model page observed:

```text
publisher: Quaternius
model: Stag
source: https://poly.pizza/m/tQdzbZ1Cmw
published: 2021-09-07
formats shown: FBX / GLTF
license shown: Public Domain (CC0)
```

The containing Quaternius Animated Animal Pack page was also observed with:

```text
source: https://poly.pizza/bundle/Animated-Animal-Pack-ILAPXeUYiS
publisher: Quaternius
updated: 2022-07-09
published roster: 12 animated animals
published animation statement: more than 12 unique animations per animal
license shown: Public Domain (CC0)
```

This is source-specific CC0 evidence for this historical/public Stag distribution and therefore does not rely on the current central Quaternius license page.

## Transport/mirror evidence

A public GitHub showcase mirror currently exposes a converted GLB at:

```text
repository: trebeljahr/quaternius-showcase
commit: 478a184d29af65637ba335ea3127995066fe11b6
path: public/glb/animals_pack/Stag.glb
Git blob: 18927223c253a8ac497a442c1a419d3c287fb84c
reported size: 685252 bytes
```

A separate public repository also exposes a textual glTF Stag artifact:

```text
repository: WaelYasmina/cloningmodels
commit: 7a3f88b56c948f26c9c3b58f3363359f60a550a3
path: src/assets/Stag.gltf
Git blob: 9bf3b64fdb88aa7d50645a32ed6c2828e8085a03
reported size: 3067762 bytes
```

These mirrors are transport/identity evidence only. Their presence does not replace byte-level comparison against the exact accepted source artifact.

## Admission boundary

Closed:

- selected creature identity;
- source-specific author attribution;
- source-specific CC0/public-domain evidence;
- public formats and animation-family evidence;
- two independently located transport artifacts.

Still required before player-facing runtime admission:

1. acquire the exact accepted Stag artifact bytes;
2. record SHA-256 of the bytes actually used;
3. inspect mesh, material, skeleton/joints and embedded animation clip names/durations;
4. pin the accepted runtime file in the project;
5. bind the project-owned rideable renderer without converting it into a vanilla horse presentation;
6. accept Minecraft scale, ground contact, rider seat, collision width/height and animation/movement agreement in the integrated R01 client pass.

Do not expose a vanilla horse, static substitute or invisible rideable while this gate remains open.
