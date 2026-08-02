#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"


def replace(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError(f"missing token in {path}: {old!r}")
    path.write_text(text.replace(old, new), encoding="utf-8")


def strip_particle_statements(text: str) -> str:
    while ".sendParticles(" in text:
        pos = text.index(".sendParticles(")
        line_start = text.rfind("\n", 0, pos) + 1
        paren = text.find("(", pos)
        depth = 0
        end = -1
        for i in range(paren, len(text)):
            if text[i] == "(":
                depth += 1
            elif text[i] == ")":
                depth -= 1
                if depth == 0:
                    semi = text.find(";", i)
                    if semi < 0:
                        raise RuntimeError("particle call missing semicolon")
                    end = semi + 1
                    break
        if end < 0:
            raise RuntimeError("unclosed particle call")
        text = text[:line_start] + text[end:]
    return text

version = ROOT / "gradle.properties"
value = version.read_text(encoding="utf-8")
if "mod_version=0.12.1-alpha.1" in value:
    print("Arcane Circle v0.12.1 cleanup already applied")
    raise SystemExit(0)
if "mod_version=0.12.0-alpha.1" not in value:
    raise RuntimeError("v0.12.1 cleanup requires published v0.12 source")
replace(version, "mod_version=0.12.0-alpha.1", "mod_version=0.12.1-alpha.1")
replace(JAVA / "ArcaneCircle.java", 'VERSION = "0.12.0-alpha.1"', 'VERSION = "0.12.1-alpha.1"')
replace(JAVA / "network/ArcaneNetwork.java", 'PROTOCOL_VERSION = "ninefold-arcana-12"',
        'PROTOCOL_VERSION = "ninefold-arcana-12-1"')

index_path = ROOT / "src/main/resources/data/arcanecircle/spell_catalog/index.json"
index = json.loads(index_path.read_text(encoding="utf-8"))
index["version"] = "0.12.1-alpha.1"
index["particle_core_calls"] = 0
index_path.write_text(json.dumps(index, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

for name in ("ExpandedSpellEffects.java", "HighCircleSpellEffects.java"):
    path = JAVA / "magic" / name
    text = strip_particle_statements(path.read_text(encoding="utf-8"))
    if ".sendParticles(" in text:
        raise RuntimeError(f"particle call remains in {name}")
    path.write_text(text, encoding="utf-8")

# Retain the public compatibility surface but permanently disable the old particle sigil implementation.
(JAVA / "magic/SpellSigilService.java").write_text('''package kr.moonseungjun.arcanecircle.magic;\n\nimport net.minecraft.server.level.ServerPlayer;\n\n/** @deprecated WorldMagicTracker now renders all casting seals as multiplayer world geometry. */\n@Deprecated\npublic final class SpellSigilService {\n    public static final int CHARGE_STAGES = 5;\n    private SpellSigilService() {}\n    public static void renderChargeStep(ServerPlayer player, SpellDefinition spell, double effectiveRange, int step) {}\n    public static void renderRelease(ServerPlayer player, SpellDefinition spell, double effectiveRange) {}\n}\n''', encoding="utf-8")

# Strictly forbid any server particle spawn in the full magic package.
remaining = []
for path in (JAVA / "magic").glob("*.java"):
    if ".sendParticles(" in path.read_text(encoding="utf-8"):
        remaining.append(path.name)
if remaining:
    raise RuntimeError(f"server particle calls remain: {remaining}")

verify = ROOT / "tools/verify_jar.py"
text = verify.read_text(encoding="utf-8")
text = text.replace("Arcane Circle v0.9 JAR verification", "Arcane Circle v0.12.1 JAR verification")
verify.write_text(text, encoding="utf-8")
print("Arcane Circle v0.12.1 full magic-package particle cleanup applied")
