#!/usr/bin/env python3
from __future__ import annotations
import hashlib, sys, zipfile
from pathlib import Path

jar = Path(sys.argv[1])
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
forbidden_prefixes = (
    "kr/moonseungjun/arcanecircle/block/",
    "kr/moonseungjun/arcanecircle/gameplay/MagicCircleInteractionHandler",
    "kr/moonseungjun/arcanecircle/registry/",
    "assets/arcanecircle/blockstates/",
    "assets/arcanecircle/textures/block/magic_circle",
)
with zipfile.ZipFile(jar) as archive:
    names = archive.namelist()
    missing = sorted(required - set(names))
    if missing: raise SystemExit(f"missing required entries: {missing}")
    forbidden = [name for name in names if name.startswith(forbidden_prefixes)]
    if forbidden: raise SystemExit(f"obsolete entries remain: {forbidden}")
    if len(names) != len(set(names)): raise SystemExit("duplicate ZIP entries")
    if any(name.endswith('.java') or name.startswith('tools/') for name in names):
        raise SystemExit("development files leaked into JAR")
digest = hashlib.sha256(jar.read_bytes()).hexdigest()
jar.with_name(jar.name + '.sha256').write_text(f"{digest}  {jar.name}\n", encoding='utf-8')
print(f"Arcane Circle Ninefold JAR verification: PASS ({len(names)} entries)")
print(f"SHA-256: {digest}")
