#!/usr/bin/env python3
from pathlib import Path
p = Path(__file__).resolve().parent / 'test_alpha57_source.py'
s = p.read_text(encoding='utf-8')
old = "forbid(military,('SettlementMilitaryArmoryService','setItemSlot(EquipmentSlot.MAINHAND)'),'alpha.57 must not fake remote sentry armory')"
new = "# Alpha.62 supersedes the Alpha.57 remote-armory absence check; current transport authority is enforced by test_alpha62_source.py."
if old not in s:
    if new in s:
        raise SystemExit(0)
    raise SystemExit('alpha.62 legacy audit patch target missing')
s = s.replace(old, new, 1)
p.write_text(s, encoding='utf-8')
print('Updated obsolete Alpha.57 remote-armory absence assertion for Alpha.62 cumulative audit.')
