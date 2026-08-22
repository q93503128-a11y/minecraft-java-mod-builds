#!/usr/bin/env python3
from pathlib import Path
import hashlib, sys, zipfile

if len(sys.argv) != 2: raise SystemExit("usage: verify_jar.py <jar>")
jar = Path(sys.argv[1]).resolve()
if not jar.is_file() or jar.stat().st_size == 0: raise SystemExit(f"missing/empty jar: {jar}")

with zipfile.ZipFile(jar) as zf:
    names = zf.namelist()
    if len(names) != len(set(names)): raise SystemExit("duplicate ZIP entries detected")
    for prefix in ["kr/moonseungjun/survivalascension/", "assets/survivalascension/", "data/survivalascension/"]:
        if not any(name.startswith(prefix) for name in names): raise SystemExit(f"required JAR prefix missing: {prefix}")
    for name in [
        "META-INF/neoforge.mods.toml",
        "META-INF/third-party/SKILL_PROFICIENCIES_MIT.txt",
        "kr/moonseungjun/survivalascension/SurvivalAscension.class",
        "kr/moonseungjun/survivalascension/client/SurvivalAscensionClient.class",
        "kr/moonseungjun/survivalascension/client/SkillsScreen.class",
        "kr/moonseungjun/survivalascension/progress/SkillProgressData.class",
        "kr/moonseungjun/survivalascension/network/SkillNetwork.class",
        "kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.class",
        "kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.class",
    ]:
        if name not in names: raise SystemExit(f"required JAR entry missing: {name}")
    notice = zf.read("META-INF/third-party/SKILL_PROFICIENCIES_MIT.txt").decode("utf-8")
    if "Copyright (c) 2026 balovich-matje" not in notice or "MIT License" not in notice:
        raise SystemExit("packaged Skill Proficiencies MIT notice invalid")
    forbidden = [name for name in names if name.endswith(".java") or name.startswith("tools/") or name.startswith(".github/")]
    if forbidden: raise SystemExit("development files leaked into JAR: " + ", ".join(forbidden[:10]))
    metadata = zf.read("META-INF/neoforge.mods.toml").decode("utf-8")
    if 'modId="survivalascension"' not in metadata: raise SystemExit("wrong mod id in metadata")
    if 'version="0.3.0-alpha.1"' not in metadata: raise SystemExit("wrong mod version in metadata")

sha = hashlib.sha256(jar.read_bytes()).hexdigest()
jar.with_name(jar.name + ".sha256").write_text(f"{sha}  {jar.name}\n", encoding="utf-8")
print("JAR VERIFY PASS")
print(f"size={jar.stat().st_size}")
print(f"sha256={sha}")
