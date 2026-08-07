from __future__ import annotations

from pathlib import Path
import runpy

ROOT = Path.cwd()
SCRIPT = Path(__file__).with_name("apply_v0121_alpha15_runtime_quality_v2.py")

# Apply the guarded v2 migration first, then correct the catalog API to its actual Map contract.
runpy.run_path(str(SCRIPT), run_name="__main__")

replacements = {
    "src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java": [
        ("for(SpellDefinition spell:SpellCatalog.spells()){",
         "for(SpellDefinition spell:SpellCatalog.spells().values()){"),
        ("List<SpellDefinition> top=SpellCatalog.spells().stream()",
         "List<SpellDefinition> top=SpellCatalog.spells().values().stream()"),
    ],
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java": [
        ("List<SpellDefinition> all=SpellCatalog.spells().stream()",
         "List<SpellDefinition> all=SpellCatalog.spells().values().stream()"),
    ],
}

for rel, pairs in replacements.items():
    path = ROOT / rel
    text = path.read_text(encoding="utf-8")
    for old, new in pairs:
        if new in text:
            continue
        if old not in text:
            raise SystemExit(f"{rel}: expected catalog migration target missing: {old}")
        text = text.replace(old, new, 1)
    path.write_text(text, encoding="utf-8")

print("Arcane Circle alpha.15 runtime-quality migration v3: PASS")
