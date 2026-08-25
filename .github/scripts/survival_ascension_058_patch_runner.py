#!/usr/bin/env python3
from pathlib import Path

path = Path(__file__).with_name("survival_ascension_058_patch.py")
source = path.read_text(encoding="utf-8")
old = '''def replace_once(rel: str, old: str, new: str) -> None:\n    text = read(rel)\n    count = text.count(old)\n    if count != 1:\n        raise RuntimeError(f"{rel}: expected one anchor, got {count}: {old[:100]!r}")\n    write(rel, text.replace(old, new, 1))\n'''
new = '''def replace_once(rel: str, old: str, new: str) -> None:\n    text = read(rel)\n    count = text.count(old)\n    if count == 1:\n        write(rel, text.replace(old, new, 1))\n        return\n    if (\n        rel.endswith("ExpeditionIncidentSystem.java")\n        and count == 2\n        and old == "        if (active.incident.kind() == ExpeditionIncident.Kind.AMBUSH) {\\n"\n        and "renderBoundary(active)" in new\n    ):\n        index = text.rfind(old)\n        write(rel, text[:index] + new + text[index + len(old):])\n        return\n    raise RuntimeError(f"{rel}: expected one anchor, got {count}: {old[:100]!r}")\n'''
count = source.count(old)
if count != 1:
    raise RuntimeError(f"replace_once helper definition drifted: {count}")
patched = source.replace(old, new, 1)
namespace = {"__file__": str(path), "__name__": "__main__"}
exec(compile(patched, str(path), "exec"), namespace)
