#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[2]
path = Path(__file__).with_name("apply.py")
text = path.read_text(encoding="utf-8")
old = '    "    private static List<ServerPlayer> positions(List<ServerPlayer> players, Vec3 fallback) {",'
new = '    "    private static List<Vec3> positions(List<ServerPlayer> players, Vec3 fallback) {",'
count = text.count(old)
if count != 2:
    raise SystemExit(f"effect positions anchor count={count}, expected 2")
path.write_text(text.replace(old, new), encoding="utf-8")

legacy_test = root / "tools/test_v01716_ranger_activation.py"
source = legacy_test.read_text(encoding="utf-8")
old_assert = '    assert "i < 18" in rain_mesh\n'
new_assert = '    assert "int arrows = 18 + meta.rank() * 3" in rain_mesh\n'
if source.count(old_assert) != 1:
    raise SystemExit("legacy arrow-rain count assertion was not found exactly once")
legacy_test.write_text(source.replace(old_assert, new_assert, 1), encoding="utf-8")

print("Corrected effect anchors and migrated arrow-rain growth contract")
