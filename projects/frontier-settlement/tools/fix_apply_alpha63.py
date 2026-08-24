#!/usr/bin/env python3
from pathlib import Path
p = Path(__file__).resolve().parent / 'apply_alpha63.py'
s = p.read_text(encoding='utf-8')
old = "insert_once(logistics, spawn_anchor, spawn_anchor + death_block, 'public static void onLivingDrops(LivingDropsEvent event)', 'transport death recovery')"
new = "insert_once(logistics, spawn_anchor, death_block, 'public static void onLivingDrops(LivingDropsEvent event)', 'transport death recovery')"
if old in s:
    s = s.replace(old, new, 1)
elif new not in s:
    raise SystemExit('alpha.63 staging-fix target missing')
p.write_text(s, encoding='utf-8')
print('Frontier alpha.63 staging applicator insertion fixed.')
