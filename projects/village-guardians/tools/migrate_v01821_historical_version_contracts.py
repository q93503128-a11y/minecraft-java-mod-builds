#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
changed = []
pattern_double = re.compile(r'"mod_version=0\.18\.\d+-alpha\.1"\s+in\s+props')
pattern_single = re.compile(r"'mod_version=0\.18\.\d+-alpha\.1'\s+in\s+props")

for path in sorted((ROOT / "tools").glob("test_*.py")):
    if path.name == "test_v01821_raid_lifecycle_presentation.py":
        continue
    text = path.read_text(encoding="utf-8")
    updated = pattern_double.sub('"mod_version=" in props', text)
    updated = pattern_single.sub("'mod_version=' in props", updated)
    if updated != text:
        path.write_text(updated, encoding="utf-8")
        changed.append(path.name)

print("[PASS] historical version-pin migration:", ", ".join(changed) if changed else "no stale pins")
