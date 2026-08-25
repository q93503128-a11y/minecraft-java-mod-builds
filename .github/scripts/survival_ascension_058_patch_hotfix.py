#!/usr/bin/env python3
from pathlib import Path

path = Path(__file__).with_name("survival_ascension_058_patch.py")
text = path.read_text(encoding="utf-8")
old = '''replace_once(rel,
\'\'\'        if (active.incident.kind() == ExpeditionIncident.Kind.AMBUSH) {\n
\'\'\',
\'\'\'        if (now % 20L == 0L) renderBoundary(active);\n
        if (active.incident.kind() == ExpeditionIncident.Kind.AMBUSH) {\n
\'\'\')'''
new = '''replace_once(rel,
\'\'\'        if (now >= active.deadline) {\n
            fail(player, active, "제한시간이 끝났습니다.");\n
            return;\n
        }\n
\n
        if (active.incident.kind() == ExpeditionIncident.Kind.AMBUSH) {\n
\'\'\',
\'\'\'        if (now >= active.deadline) {\n
            fail(player, active, "제한시간이 끝났습니다.");\n
            return;\n
        }\n
\n
        if (now % 20L == 0L) renderBoundary(active);\n
\n
        if (active.incident.kind() == ExpeditionIncident.Kind.AMBUSH) {\n
\'\'\')'''
count = text.count(old)
if count != 1:
    raise RuntimeError(f"expected exactly one ambiguous incident replacement, got {count}")
path.write_text(text.replace(old, new, 1), encoding="utf-8")
print("0.58 patch anchor hotfix applied")
