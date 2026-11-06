#!/usr/bin/env python3
"""Read the *pinned* World Bosses JAR for a reviewable cache integration audit (never package its bytecode)."""
from pathlib import Path
import json
import re
import subprocess
import zipfile

name = "world-bosses-1545677-9254111.jar"
paths = list((Path.home() / ".gradle" / "caches").rglob(name))
out = Path("build/reports/modpack/worldbosses-raid-cache-inspection.txt")
out.parent.mkdir(parents=True, exist_ok=True)
if not paths:
    out.write_text("MISSING: pinned dependency JAR was not resolved\n", encoding="utf-8")
    raise SystemExit("Missing pinned World Bosses JAR")
jar = paths[0]
lines = ["PINNED WORLD BOSSES CACHE INSPECTION", f"JAR: {jar.name}", "LICENSE: THIRD PARTY — READ ONLY"]
with zipfile.ZipFile(jar) as z:
    allnames = z.namelist()
    candidates = [
        n for n in allnames if n.endswith(".class")
        and ("cache" in n.lower() or "loot" in n.lower() or "reward" in n.lower())
        and not n.startswith("META-INF")
    ]
    configs = [
        n for n in allnames if not n.endswith(".class")
        and (n.endswith(".json") or n.endswith(".json5"))
        and re.search(r"raid|cache|loot|reward", n, re.I)
    ]
    lines.append("CANDIDATE CLASS PATHS:\n" + "\n".join(candidates))
    lines.append("POSSIBLE CACHE/LOOT CONFIG PATHS:\n" + "\n".join(configs))
    for path in configs[:35]:
        blob = z.read(path)
        if len(blob) < 32000:
            lines.append(f"CONFIG {path}:\n" + blob.decode("utf-8", "replace")[:32000])
    # Class signature/bytecode calls help identify where the actual server claim
    # succeeds, and whether UI preview and weighted roll share that data.
    focus = [n for n in candidates if re.search(r"cache|loot", n, re.I)]
    for n in focus[:40]:
        classname = n[:-6].replace("/", ".")
        cmd = ["javap", "-classpath", str(jar), "-p", "-c", classname]
        try:
            res = subprocess.run(cmd, capture_output=True, text=True, timeout=12, check=False)
            lines.append(f"CLASS {classname} rc={res.returncode}\n" + res.stdout[:32000] + res.stderr[:500])
        except (OSError, subprocess.TimeoutExpired) as e:
            lines.append(f"CLASS {classname}: {type(e).__name__}: {e}")
out.write_text("\n\n".join(lines) + "\n", encoding="utf-8")
print(f"WORLDBOSSES_CACHE_INSPECTION_READY classes={len(candidates)} configs={len(configs)} report={out}")
