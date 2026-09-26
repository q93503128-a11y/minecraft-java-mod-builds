# Open-World RPG — Azari World Intake

> Status: **INTAKE TOOLING READY / CREATOR ARCHIVE BYTES STILL REQUIRED**
>
> Spatial master: `AZARI_SPATIAL_CLOSURE_PASS1.md`
>
> The creator world archive is local-only unless redistribution is separately proven safe.

## Canonical acquisition target

- Planet Minecraft: `https://www.planetminecraft.com/project/azari-30k-x-30k-world-painter-map/`
- creator-linked current build: `https://vmbiemc.gumroad.com/l/azarimap`
- project-pinned map update: **2026-06-18**
- advertised target: **30k × 30k, Minecraft Java 1.21+**
- creator instruction: use the free **$0** checkout option

Do not replace this with an unverified third-party mirror merely to automate acquisition.

## Local-only layout

Place the untouched downloaded ZIP at:

```text
projects/openworld-rpg/.local/azari/source/azari.zip
```

The project `.gitignore` excludes the whole `.local/azari/` tree.

## Archive verification

From `projects/openworld-rpg/`:

```bash
python tools/azari_world_intake.py \
  .local/azari/source/azari.zip \
  --output .local/azari/intake/azari-world-report.json
```

The report records the actual archive SHA-256, byte size, ZIP-entry count, world root,
`level.dat`, overworld Anvil region root, 512-aligned region block envelope, and
datapack/dimension presence.

The 30k sanity band is not spatial proof. Final coordinates still require the real world loaded.

## R01 spatial closure order

Once the archive passes intake and an untouched inspection copy loads:

1. establish at least two render-to-world landmark control points;
2. lock Alderford footprint and first-shrine/service approach;
3. trace Alderford → Old Quarry Road by real path distance;
4. bind Lost Cargo, Broken Road Marker and Roadside Trouble volumes;
5. bind R01 resource nodes;
6. bind Quarry overlook, Waystone and dungeon entrance;
7. bind Upper Gallery / Collapsed Hoist / Root-Breached room anchors;
8. bind Relay Gallery interaction and Earthloong chamber bounds;
9. measure sightlines and actual on-foot/mount travel time;
10. reject or move authored anchors where the terrain creates dead travel or bad combat geometry.

No final coordinate may be inferred from the public overview render.

## Tool verification

Before repository admission the parser was tested against a synthetic Java-world ZIP spanning
region coordinates `-29..29` on both axes. It correctly reported **30,208 × 30,208** block
coverage and the expected region-file count.

```text
AZARI CREATOR DOWNLOAD LOCATOR: VERIFIED
AZARI ARCHIVE ACQUIRED: NO
INTAKE TOOL TESTED: YES
ARCHIVE SHA-256 RECORDED: NO
ACTUAL AZARI WORLD LOADED: NO
R01 COORDINATES VERIFIED: NO
```
