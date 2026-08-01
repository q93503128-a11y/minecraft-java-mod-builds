#!/usr/bin/env python3
from __future__ import annotations
import hashlib
import sys
import zipfile
from pathlib import Path

jar = Path(sys.argv[1])
if not jar.is_file():
    raise SystemExit(f"missing JAR: {jar}")

required = {
    "META-INF/neoforge.mods.toml",
    "kr/moonseungjun/arcanecircle/ArcaneCircle.class",
    "kr/moonseungjun/arcanecircle/ArcaneCircleClient.class",
    "kr/moonseungjun/arcanecircle/client/GrimoireScreen.class",
    "kr/moonseungjun/arcanecircle/magic/MagicPlayerData.class",
    "kr/moonseungjun/arcanecircle/magic/SpellCastingService.class",
    "kr/moonseungjun/arcanecircle/network/ArcaneNetwork.class",
    "assets/arcanecircle/lang/ko_kr.json",
    "data/arcanecircle/spell_catalog/index.json",
}
forbidden = {"kr/moonseungjun/arcanecircle/network/FuseSpellPayload.class"}
with zipfile.ZipFile(jar) as archive:
    names = archive.namelist()
    missing = sorted(required - set(names))
    if missing:
        raise SystemExit(f"missing required entries: {missing}")
    remains = sorted(forbidden & set(names))
    if remains:
        raise SystemExit(f"obsolete click-research classes remain: {remains}")
    if len(names) != len(set(names)):
        raise SystemExit("duplicate ZIP entries")
    if any(name.endswith(".java") or name.startswith(("tools/", ".github/")) for name in names):
        raise SystemExit("development files leaked into JAR")

digest = hashlib.sha256(jar.read_bytes()).hexdigest()
jar.with_name(jar.name + ".sha256").write_text(f"{digest}  {jar.name}\n", encoding="utf-8")
print(f"Arcane Circle v0.3 JAR verification: PASS ({len(names)} entries)")
print(f"SHA-256: {digest}")
