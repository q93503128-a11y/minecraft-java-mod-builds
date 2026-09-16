# TURNBOUND: RE Modrinth distribution

The production playtest distribution is a **Modrinth `.mrpack`**, not a multi-gigabyte mod JAR and not a Prism-only instance.

## Why

- `Drehmal: APOTHEOSIS v2.2.2f` is the selected external authored-world base.
- Its official manifest reports roughly 4 GB compressed / 5.1 GB uncompressed.
- TURNBOUND has not verified redistribution permission for vendoring the original world/resource pack.
- A plain mod JAR should not silently download several gigabytes when somebody installs it by itself.
- A `.mrpack` can install the TURNBOUND JAR and a small distribution marker, but the pack format cannot merge-extract the three official Drehmal world shards into one save before launch.

For that reason the **official TURNBOUND Modrinth pack marker** enables a client-side first-run installer inside the mod. A standalone TURNBOUND JAR remains inert and never starts the external-world download by itself.

## Package contract

CI stages `packaging/modrinth/`, places the verified TURNBOUND JAR under `overrides/mods/`, and emits `TURNBOUND_RE.mrpack`.

The pack contains:

- root `modrinth.index.json` declaring Minecraft `26.2` and NeoForge `26.2.0.38-beta`,
- `overrides/mods/turnbound_re-*.jar`,
- `overrides/config/turnbound_re_distribution.properties` with the explicit Modrinth auto-install marker.

On first game launch, while the player is still at the title screen, TURNBOUND:

1. checks that the explicit Modrinth distribution marker is present,
2. requires at least 12 GiB of free disk space for a fresh install,
3. downloads the three pinned official map shards from the Drehmal Team GitHub release,
4. verifies every shard by exact size, readable ZIP structure, and published SHA-256,
5. merge-extracts shard 1 → 2 → 3 into the dedicated TURNBOUND save,
6. reproduces the official Drehmal recursive directory SHA-256 algorithm and requires the pinned final map hash,
7. downloads the official `resources.zip`, validates its published size and ZIP integrity, and installs it as the Minecraft 26.1+ world resource pack at `saves/<world>/resourcepacks/resources.zip`,
8. writes `.turnbound_re_profile` only after the map and world resource pack are verified,
9. removes temporary download cache after success.

Interrupted TURNBOUND-owned installs are resumable/rebuildable. An unrelated save that merely has the same folder name is **not deleted or overwritten**; it must exactly match the pinned clean Drehmal map before TURNBOUND adopts it.

The server-side external-world binder trusts only the exact profile marker. Once the prepared world is opened, the existing TURNBOUND bootstrap service registers the enabled semantic anchors and moves a new player to the Hub without a manual `/bind_drehmal` step.

## Current validation boundary

Packaging/JUnit/JAR validation can prove the installer contract and `.mrpack` structure, but it does **not** prove that the 1.20.1-era APOTHEOSIS save migrates correctly under Minecraft Java 26.2 + NeoForge.

The first real Modrinth App import, multi-gigabyte download, 26.2 world load, New Drabyel/Stasis landmark preservation, resource-pack appearance, UI readability, and integrated gameplay loop remain real-client playtest gates until they are actually executed.
