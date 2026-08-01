#!/usr/bin/env python3
from __future__ import annotations
import hashlib, sys, zipfile
from pathlib import Path
jar_path = Path(sys.argv[1])
if not jar_path.is_file(): raise SystemExit(f"missing JAR: {jar_path}")
with zipfile.ZipFile(jar_path) as jar:
    names = jar.namelist()
    required = {
        "META-INF/neoforge.mods.toml", "assets/arcanecircle/blockstates/magic_circle.json",
        "assets/arcanecircle/textures/block/magic_circle.png", "data/arcanecircle/recipe/magic_circle.json",
        "kr/moonseungjun/arcanecircle/ArcaneCircle.class", "kr/moonseungjun/arcanecircle/magic/SpellRecipe.class",
    }
    missing = sorted(required - set(names))
    if missing: raise SystemExit("missing JAR entries: " + ", ".join(missing))
    if len(names) != len(set(names)): raise SystemExit("duplicate ZIP entries detected")
    if any(name.endswith(".java") for name in names): raise SystemExit("source files leaked into runtime JAR")
    if any(name.startswith(("tools/", ".github/")) for name in names): raise SystemExit("development files leaked into runtime JAR")
digest = hashlib.sha256(jar_path.read_bytes()).hexdigest()
jar_path.with_name(jar_path.name + ".sha256").write_text(f"{digest}  {jar_path.name}\n", encoding="utf-8")
print(f"Arcane Circle JAR verification: PASS ({len(names)} entries)")
print(f"SHA-256: {digest}")
