# Drehmal official first-run bootstrap

- Purpose: first-run download source for the opt-in TURNBOUND one-click test pack
- Classification: direct official download source / reference implementation
- Upstream project: https://github.com/Drehmal-Team/installer
- Upstream installer commit inspected: `f4906cabd6cf99f175f5082a343612f94b7aa113` ("hotfix v2.2.2f")
- Official map release: https://github.com/Drehmal-Team/map/releases/tag/v2.2.2f
- Official downloads page: https://www.drehmal.net/downloads
- Upstream content license: ARR (All Rights Reserved)
- TURNBOUND does **not** vendor or redistribute the map or resource-pack bytes.

Pinned upstream manifest facts:
- map version: `2.2.2f`
- official assembled-directory SHA-256: `2e6232dc3e97c77eaa006b09e3ee09b246c46e3df68359dcc1495ce19d1e8053`
- official uncompressed size: `5,139,218,299` bytes
- shard 1: `1,484,178,230` bytes, SHA-256 `378f8bea9c88371c44d3a6fd6c6d91a886f14b6dfce97e6e6e994789b979b358`
- shard 2: `1,641,289,974` bytes, SHA-256 `2d250f04259404d81a3e9bd83a5592f1c26545f45f9c8a22a2277c9e523c1331`
- shard 3: `861,863,079` bytes, SHA-256 `8c892c77c7ab9ef03aac7b517e5448490f55690163085b38c380523dd2d29b67`
- resource pack release asset: `resources.zip`, `190,255,507` bytes

TURNBOUND implementation:
- the one-click pack contains only a small opt-in marker; the standalone TURNBOUND JAR does not initiate a multi-gigabyte download;
- on first launch, TURNBOUND downloads the three map shards and resource pack directly from the official Drehmal GitHub release;
- shard files are size/SHA-256 checked and the assembled world is checked using the same recursive directory-hash semantics published by the official installer;
- archive extraction rejects paths that escape the temporary installation directory;
- only after validation succeeds does TURNBOUND write its own world-profile marker and move the world into `saves/`;
- the resource pack is placed as the world's `resources.zip`, keeping it local to the installed map instead of changing unrelated global resource-pack settings;
- the official map/resource-pack download cache is deleted after successful installation;
- future TURNBOUND JAR updates reuse the installed save and do not redownload the map unless the pinned world version changes or required installed files are removed.

No upstream Electron/Vue installer source is bundled into TURNBOUND. The public upstream manifest/release metadata is used only to pin official URLs, sizes and validation values.
