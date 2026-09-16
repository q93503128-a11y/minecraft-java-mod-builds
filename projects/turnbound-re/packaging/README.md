# TURNBOUND: RE Prism distribution

The production test distribution is a Prism Launcher instance ZIP, not a multi-gigabyte mod JAR.

## Why

- `Drehmal: APOTHEOSIS v2.2.2f` is the selected external authored-world base.
- Its official manifest reports roughly 4 GB compressed / 5.1 GB uncompressed.
- TURNBOUND has not verified redistribution permission for vendoring the original world/resource pack.
- A standard mod JAR is therefore the wrong delivery boundary.
- A plain `.mrpack` can place files but does not merge-extract the three official world shards into one save before launch.
- Prism pre-launch commands can perform that first-run bootstrap while keeping the original files sourced from the official release.

## Package contract

`packaging/prism/` is copied into a staging directory by CI. The verified TURNBOUND production JAR is then placed under `.minecraft/mods/` and the whole directory is zipped as `TURNBOUND_RE_Prism.zip`.

On Windows first launch, `.minecraft/turnbound-bootstrap.ps1`:

1. downloads the three pinned official map shards and resource pack from the Drehmal Team GitHub release,
2. merge-extracts the shards into the dedicated TURNBOUND save,
3. validates the extracted map against the official directory SHA-256,
4. writes `.turnbound_re_profile` only after that validation succeeds,
5. installs/enables the official resource pack inside the isolated Prism instance,
6. leaves the external bytes out of this repository and out of the distributed instance ZIP.

The mod trusts only the exact profile marker and then self-binds enabled TURNBOUND anchors on first player login. Manual installations still use the operator-confirmed `/turnbound_re_world_slice bind_drehmal` path.

## Current scope

The bootstrapper is intentionally Windows-first because the current playtest environment is Windows. The package should not be advertised as macOS/Linux ready until equivalent launch scripts are implemented and tested.

The first real Java 26.2 migration of the 1.20.1-era world is still a manual playtest gate. Packaging success does not claim world migration success.
