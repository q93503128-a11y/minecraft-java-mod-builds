# Drehmal structured map reference

- classification: reference / unknown-license
- upstream project: `zachaa/DrehmalMap`
- upstream URL: https://github.com/zachaa/DrehmalMap
- inspected commit: `72d82180cbe3f950f068cf2d8e8668c6b09d5c58`
- map-image submodule: `zachaa/drehmal_images`
- inspected image-repo commit: `6e07fa85d7390ae5b1d5f323a961ebddd718d87a`
- upstream target: Drehmal 2.2.2 / Minecraft 1.20.1
- use in TURNBOUND: reference-only spatial analysis for roads, signs, traders, storage, named/source entities, New Drabyel micro-layout, and the Capital Valley first-route search corridor
- raw upstream JSON, map tiles and images: not vendored
- derived data kept in TURNBOUND: small semantic anchors, route corridors/search seeds and simplified road centerline points used to seed live-world placement checks; no raw upstream map database is vendored
- license status: no repository license was identified during this review; therefore no upstream file is treated as redistributable
- validation boundary: source coordinates are never sufficient by themselves to certify migrated Minecraft 26.2 geometry; runtime placement rechecks the actual bound world before spawning a service actor
