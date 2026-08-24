#!/usr/bin/env python3
from pathlib import Path
root = Path(__file__).resolve().parent

p = root / 'test_alpha57_source.py'
s = p.read_text(encoding='utf-8')
old = "forbid(military,('SettlementMilitaryArmoryService','setItemSlot(EquipmentSlot.MAINHAND)'),'alpha.57 must not fake remote sentry armory')"
new = "# Alpha.62 supersedes the Alpha.57 remote-armory absence check; current transport authority is enforced by test_alpha62_source.py."
if old in s:
    s = s.replace(old, new, 1)
elif new not in s:
    raise SystemExit('alpha.62 legacy audit patch target missing')
p.write_text(s, encoding='utf-8')

p = root / 'test_alpha62_docs.py'
s = p.read_text(encoding='utf-8')
old = "'existing `MILITARY_RETURN_TRIP_TAG` / `MILITARY_SUPPLY_TRIP_TAG`'"
new = "'same `MILITARY_RETURN_TRIP_TAG` / `MILITARY_SUPPLY_TRIP_TAG`'"
if old in s:
    s = s.replace(old, new, 1)
elif new not in s:
    raise SystemExit('alpha.62 docs semantic audit patch target missing')
p.write_text(s, encoding='utf-8')
print('Updated superseded Alpha.57 assertion and semantic Alpha.62 docs audit wording.')
