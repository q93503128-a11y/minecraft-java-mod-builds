#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"
TARGET = JAVA / "client/WorldMagicTracker.java"
SIGNATURES = JAVA / "client/SignatureGeometry.java"
OLD_VERSION = "0.12.1-alpha.1"
NEW_VERSION = "0.12.1-alpha.2"


def replace_version(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    if NEW_VERSION in text:
        return
    if OLD_VERSION not in text:
        raise RuntimeError(f"unexpected version in {path}")
    path.write_text(text.replace(OLD_VERSION, NEW_VERSION), encoding="utf-8")


signatures = SIGNATURES.read_text(encoding="utf-8")
required_models = ("meteor(", "tornado(", "portal(", "cage(", "wall(", "dome(")
missing = [token for token in required_models if token not in signatures]
if missing:
    raise RuntimeError(f"signature geometry source incomplete: {missing}")

text = TARGET.read_text(encoding="utf-8")
if "SignatureGeometry.build(spell, direction, range)" not in text:
    required = ("private static VoxelShape buildRelease(", "Math.max(1.2F, baseWidth * 1.25F)")
    absent = [token for token in required if token not in text]
    if absent:
        raise RuntimeError(f"unexpected WorldMagicTracker baseline: {absent}")
    text = text.replace("Math.max(1.2F, baseWidth * 1.25F)", "Math.max(0.85F, baseWidth * 0.90F)", 1)
    text = text.replace("int ringPoints = 38 + spell.circle() * 4;", "int ringPoints = 48 + spell.circle() * 5;", 1)
    text = text.replace("localProgress, 0.018)", "localProgress, 0.011)", 1)
    text = text.replace("int spokes = Math.min(14,", "int spokes = Math.min(13,", 1)
    text = text.replace("int satellites = Math.min(8, spell.circle() - 1);",
                        "int satellites = Math.min(6, spell.circle() - 1);", 1)
    start = text.index("    private static VoxelShape buildRelease(")
    end = text.index("    private static Basis basis(", start)
    block = text[start:end]
    marker = "        return shape;"
    insert_at = block.rfind(marker)
    if insert_at < 0:
        raise RuntimeError("buildRelease return marker missing")
    block = block[:insert_at] + "        shape = Shapes.or(shape, SignatureGeometry.build(spell, direction, range));\n" + block[insert_at:]
    text = text[:start] + block + text[end:]
    for old, new in (
        ("case FIRE -> 0xFFFF7048;", "case FIRE -> 0xD8FF7048;"),
        ("case FROST -> 0xFF6DE4FF;", "case FROST -> 0xD86DE4FF;"),
        ("case WIND -> 0xFF76E6BD;", "case WIND -> 0xD876E6BD;"),
        ("case WARD -> 0xFFC595FF;", "case WARD -> 0xD8C595FF;"),
        ("case LIFE -> 0xFF73E38E;", "case LIFE -> 0xD873E38E;"),
        ("case SPACE -> 0xFFA382FF;", "case SPACE -> 0xD8A382FF;"),
        ("default -> 0xFF82A8FF;", "default -> 0xD882A8FF;"),
    ):
        text = text.replace(old, new, 1)
    TARGET.write_text(text, encoding="utf-8")

for path in TOOLS.glob("v0121_alpha2_tracker.part-*"):
    path.unlink()
helper = TOOLS / "recover_v0121_alpha2_payload.py"
if helper.exists():
    helper.unlink()

replace_version(ROOT / "gradle.properties")
replace_version(JAVA / "ArcaneCircle.java")
index_path = ROOT / "src/main/resources/data/arcanecircle/spell_catalog/index.json"
index = json.loads(index_path.read_text(encoding="utf-8"))
index["version"] = NEW_VERSION
index["release_geometry"] = "per_spell_signature_geometry"
index["signature_models"] = ["meteor", "tornado", "portal", "force_cage", "barrier_wall", "barrier_dome"]
index_path.write_text(json.dumps(index, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

for name in ("test_magic_contract.py", "test_v12_overhaul.py", "test_v12_1_cleanup.py"):
    path = TOOLS / name
    source = path.read_text(encoding="utf-8").replace(OLD_VERSION, NEW_VERSION)
    if name == "test_magic_contract.py":
        source = source.replace("apply_v12_1_particle_cleanup.py", "apply_v0121_alpha2_signature_geometry.py")
    path.write_text(source, encoding="utf-8")

print("Arcane Circle v0.12.1-alpha.2 signature geometry installed")
