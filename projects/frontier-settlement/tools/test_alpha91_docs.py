#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
alpha90 = (ROOT / "WORKER_MAINTENANCE_ALPHA90.md").read_text(encoding="utf-8")
alpha91 = (ROOT / "WORKER_NAVIGATION_STORAGE_ALPHA91.md").read_text(encoding="utf-8")

# Preserve the previous canonical maintenance contract without requiring the live
# project version to remain Alpha.90 forever.
for token in (
    "`0.1.0-alpha.90`",
    "`/frontier normalize`",
    "100%-step",
    "농장 공사 100% · 마감 확인",
    "lowest contiguous same-species trunk base",
    "greatest physical trunk-log supply",
    "One shared `건설 주민` remains by design",
    "No virtual resource ledger",
):
    if token not in alpha90:
        raise SystemExit(f"alpha.90 canonical doc regression: {token}")

for token in (
    "Version: `0.1.0-alpha.91`",
    "Lumber: 48 -> 128 horizontal blocks",
    "Quarry: 40 -> 96 horizontal blocks",
    "Mine scan: 24 -> 48 horizontal blocks and 48 -> 80 blocks downward",
    "`Path.canReach()`",
    "temporarily blacklisted",
    "Lumber, farm, quarry and mine output first returns",
    "rotation-aware `BuildingRecord.localToWorld`",
    "four-barrel public cluster",
    "108 slots",
    "No virtual resource ledger or item minting",
    "No force-load or teleport",
):
    if token not in alpha91:
        raise SystemExit(f"alpha.91 docs missing: {token}")

if "mod_version=0.1.0-alpha.91" not in props:
    raise SystemExit("alpha.91 docs/version mismatch")
if "minecraft_version=26.2" not in props:
    raise SystemExit("alpha.91 Minecraft version mismatch")
if "neo_version=26.2.0.38-beta" not in props:
    raise SystemExit("alpha.91 NeoForge version mismatch")

print("Frontier Settlement alpha.91 canonical docs audit: PASS")
