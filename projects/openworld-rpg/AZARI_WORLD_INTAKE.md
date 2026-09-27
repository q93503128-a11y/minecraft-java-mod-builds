# Open-World RPG — Azari World Intake

> Status: **INTAKE + LOCAL R01 REVIEW-SAVE TOOLING READY / R01 EXTRACTED WORLD SLICE ACQUIRED + PARSED / FULL CREATOR ARCHIVE LOCAL HASH + CLIENT LOAD STILL REQUIRED**
>
> Spatial master: `AZARI_SPATIAL_CLOSURE_PASS1.md`
>
> Client review: `AZARI_R01_CLIENT_REVIEW.md`
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

## Local R01 client-review save

The intake tool can now create a new local-only save containing only the R01 review rectangle while leaving the source archive untouched:

```text
region X = -8 .. 4
region Z =  0 .. 12
```

Example from `projects/openworld-rpg/`:

```powershell
py tools\azari_world_intake.py `
  ".local\azari\source\azari.zip" `
  --output ".local\azari\intake\azari-world-report.json" `
  --extract-r01-review "<YOUR_OPENWORLD_RPG_INSTANCE>\saves\Azari_R01_Review"
```

The destination must not already exist. The tool deliberately refuses to overwrite a world save.

The review copy includes the 169 required overworld terrain region files, matching entity/POI region files that exist in the archive, `level.dat`, datapacks and a small set of optional world-level files. It does not copy creator playerdata or unrelated map regions.

A missing terrain region makes extraction fail and removes the partial destination so Minecraft cannot silently generate substitute terrain inside the R01 review rectangle.

Exact inspection order, `/tp` commands and evidence fields are in `AZARI_R01_CLIENT_REVIEW.md`.

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

During this pass the existing region-filename regex was found to be over-escaped and therefore
unable to match normal Anvil names such as `region/r.-8.12.mca`. The current tool fixes that parser
bug and was re-tested rather than relying on the older verification note.

The corrected parser was tested against a synthetic Java-world ZIP spanning region coordinates
`-29..29` on both axes. It reported **30,208 × 30,208** block coverage. The R01 review-save path
was tested against a synthetic archive with the full `-8..4 × 0..12` terrain rectangle: it copied
exactly **169** terrain regions, copied only present entity/POI regions and datapacks, refused an
existing destination, and removed a partial destination when a required terrain region was missing.

```text
AZARI CREATOR DOWNLOAD LOCATOR: VERIFIED
AZARI R01 EXTRACTED SLICE ACQUIRED + PARSED: YES
LOCAL REVIEW-SAVE TOOLING TESTED: YES
FULL 19.2 GB CREATOR ZIP HASHED IN THIS CHAT WORKSPACE: NO
ACTUAL AZARI WORLD LOADED IN MINECRAFT CLIENT: NO
R01 SPATIAL_BINDING COMPLETE: NO
PLAYTESTED: NO
```
