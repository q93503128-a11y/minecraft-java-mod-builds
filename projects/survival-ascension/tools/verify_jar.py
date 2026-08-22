#!/usr/bin/env python3
from pathlib import Path
import hashlib, re, sys, zipfile

if len(sys.argv) != 2: raise SystemExit("usage: verify_jar.py <jar>")
jar = Path(sys.argv[1]).resolve()
if not jar.is_file() or jar.stat().st_size == 0: raise SystemExit(f"missing/empty jar: {jar}")
match = re.fullmatch(r"survivalascension-(.+)\.jar", jar.name)
if not match: raise SystemExit(f"unexpected jar name: {jar.name}")
expected_version = match.group(1)

with zipfile.ZipFile(jar) as zf:
    names = zf.namelist()
    if len(names) != len(set(names)): raise SystemExit("duplicate ZIP entries detected")
    for prefix in ["kr/moonseungjun/survivalascension/", "assets/survivalascension/", "data/survivalascension/"]:
        if not any(name.startswith(prefix) for name in names): raise SystemExit(f"required JAR prefix missing: {prefix}")
    for name in [
        "META-INF/neoforge.mods.toml",
        "META-INF/third-party/SKILL_PROFICIENCIES_MIT.txt",
        "META-INF/third-party/VEINMINER_PLUS_PLUS_MIT.txt",
        "META-INF/third-party/MINEMENU_MIT.txt",
        "META-INF/third-party/BUILDING_GADGETS_2_MIT.txt",
        "META-INF/third-party/MOB_CHAMPIONS_MIT.txt",
        "META-INF/third-party/APOTHEOSIS_MIT.txt",
        "kr/moonseungjun/survivalascension/SurvivalAscension.class",
        "kr/moonseungjun/survivalascension/client/AscensionRadialMenuScreen.class",
        "kr/moonseungjun/survivalascension/client/ConstructionRadialMenuScreen.class",
        "kr/moonseungjun/survivalascension/client/GuideScreen.class",
        "kr/moonseungjun/survivalascension/client/SkillsScreen.class",
        "kr/moonseungjun/survivalascension/progress/SkillProgressData.class",
        "kr/moonseungjun/survivalascension/mining/MiningProgression.class",
        "kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.class",
        "kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.class",
        "kr/moonseungjun/survivalascension/combat/CombatProgression.class",
        "kr/moonseungjun/survivalascension/construction/ConstructionProgression.class",
        "kr/moonseungjun/survivalascension/mobility/MobilityProgression.class",
        "kr/moonseungjun/survivalascension/elite/EliteMobSystem.class",
        "kr/moonseungjun/survivalascension/equipment/AscensionAffixes.class",
    ]:
        if name not in names: raise SystemExit(f"required JAR entry missing: {name}")
    for notice, line in [
        ("META-INF/third-party/SKILL_PROFICIENCIES_MIT.txt", "Copyright (c) 2026 balovich-matje"),
        ("META-INF/third-party/VEINMINER_PLUS_PLUS_MIT.txt", "Copyright (c) 2026 Kestalkayden"),
        ("META-INF/third-party/MINEMENU_MIT.txt", "Copyright (c) 2013 Dylan Miller"),
        ("META-INF/third-party/BUILDING_GADGETS_2_MIT.txt", "Copyright (c) 2023 Direwolf20-MC"),
        ("META-INF/third-party/MOB_CHAMPIONS_MIT.txt", "Copyright (c) 2024 Wendall Cada"),
        ("META-INF/third-party/APOTHEOSIS_MIT.txt", "Copyright (c) 2018-2025 Stormraven Studios, LLC"),
    ]:
        text = zf.read(notice).decode("utf-8")
        if line not in text or "MIT" not in text: raise SystemExit(f"invalid packaged notice: {notice}")
    forbidden = [name for name in names if name.endswith(".java") or name.startswith("tools/") or name.startswith(".github/")]
    if forbidden: raise SystemExit("development files leaked into JAR: " + ", ".join(forbidden[:10]))
    metadata = zf.read("META-INF/neoforge.mods.toml").decode("utf-8")
    if 'modId="survivalascension"' not in metadata: raise SystemExit("wrong mod id in metadata")
    if f'version="{expected_version}"' not in metadata: raise SystemExit(f"wrong mod version in metadata: expected {expected_version}")

sha = hashlib.sha256(jar.read_bytes()).hexdigest()
jar.with_name(jar.name + ".sha256").write_text(f"{sha}  {jar.name}\n", encoding="utf-8")
print("JAR VERIFY PASS")
print(f"version={expected_version}")
print(f"size={jar.stat().st_size}")
print(f"sha256={sha}")
