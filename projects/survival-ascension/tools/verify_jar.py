#!/usr/bin/env python3
from pathlib import Path
import hashlib
import sys
import zipfile

if len(sys.argv) != 2:
    raise SystemExit("usage: verify_jar.py <jar>")

jar = Path(sys.argv[1]).resolve()
if not jar.is_file() or jar.stat().st_size == 0:
    raise SystemExit(f"missing/empty jar: {jar}")

with zipfile.ZipFile(jar) as zf:
    names = zf.namelist()
    if len(names) != len(set(names)):
        raise SystemExit("duplicate ZIP entries detected")
    required_prefixes = [
        "kr/moonseungjun/survivalascension/",
        "assets/survivalascension/",
        "data/survivalascension/",
    ]
    if "META-INF/neoforge.mods.toml" not in names:
        raise SystemExit("META-INF/neoforge.mods.toml missing")
    for prefix in required_prefixes:
        if not any(name.startswith(prefix) for name in names):
            raise SystemExit(f"required JAR prefix missing: {prefix}")
    if not any(name.startswith("kr/moonseungjun/survivalascension/") and name.endswith(".class") for name in names):
        raise SystemExit("compiled Survival Ascension classes missing")
    forbidden = [name for name in names if name.endswith(".java") or name.startswith("tools/") or name.startswith(".github/")]
    if forbidden:
        raise SystemExit("development files leaked into JAR: " + ", ".join(forbidden[:10]))
    metadata = zf.read("META-INF/neoforge.mods.toml").decode("utf-8")
    if 'modId="survivalascension"' not in metadata:
        raise SystemExit("wrong mod id in metadata")
    if 'version="0.1.0-alpha.1"' not in metadata:
        raise SystemExit("wrong mod version in metadata")

sha = hashlib.sha256(jar.read_bytes()).hexdigest()
sha_file = jar.with_name(jar.name + ".sha256")
sha_file.write_text(f"{sha}  {jar.name}\n", encoding="utf-8")
print("JAR VERIFY PASS")
print(f"size={jar.stat().st_size}")
print(f"sha256={sha}")
