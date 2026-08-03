#!/usr/bin/env python3
from pathlib import Path

path = Path(__file__).with_name("apply.py")
text = path.read_text(encoding="utf-8")
old = '    "    private static List<ServerPlayer> positions(List<ServerPlayer> players, Vec3 fallback) {",'
new = '    "    private static List<Vec3> positions(List<ServerPlayer> players, Vec3 fallback) {",'
count = text.count(old)
if count != 2:
    raise SystemExit(f"effect positions anchor count={count}, expected 2")
path.write_text(text.replace(old, new), encoding="utf-8")
print("Corrected VillageSkillEffectSystem positions return-type anchors")
