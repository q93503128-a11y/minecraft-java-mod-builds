#!/usr/bin/env python3
from pathlib import Path
import base64
import hashlib
import json

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"
TARGET = JAVA / "client/WorldMagicTracker.java"
OLD_VERSION = "0.12.1-alpha.1"
NEW_VERSION = "0.12.1-alpha.2"
SOURCE_SHA256 = "f0791cb317960032ae36500128977cfc3fb7992d695899ebb4660b98cfbb9334"
TOKENS = (
    "RELEASE_FRAME_NS", "meteorGeometry", "tornadoGeometry", "portalGeometry",
    "forceCageGeometry", "barrierWallGeometry", "barrierDomeGeometry",
    '"meteor_swarm"', '"control_weather"', '"forcecage"', "withAlpha",
    "for (int ring = 0; ring < spell.circle(); ring++)", "Math.max(0.85F"
)


def replace_version(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    if NEW_VERSION in text:
        return
    if OLD_VERSION not in text:
        raise RuntimeError(f"unexpected version in {path}")
    path.write_text(text.replace(OLD_VERSION, NEW_VERSION), encoding="utf-8")


def unpack(path: Path) -> bytes:
    text = "".join(path.read_text(encoding="utf-8").split()).rstrip("=")
    text += "=" * (-len(text) % 4)
    return base64.b64decode(text, validate=True)


parts = sorted(TOOLS.glob("v0121_alpha2_tracker.part-*"))
if parts:
    source_bytes = b"".join(unpack(path) for path in parts)
    digest = hashlib.sha256(source_bytes).hexdigest()
    if digest != SOURCE_SHA256:
        raise RuntimeError(f"renderer payload digest mismatch: {digest}")
    source = source_bytes.decode("utf-8")
    missing = [token for token in TOKENS if token not in source]
    if missing:
        raise RuntimeError(f"renderer payload missing tokens: {missing}")
    TARGET.write_bytes(source_bytes)
    for path in parts:
        path.unlink()
else:
    source_bytes = TARGET.read_bytes()
    source = source_bytes.decode("utf-8")
    digest = hashlib.sha256(source_bytes).hexdigest()
    if digest != SOURCE_SHA256:
        raise RuntimeError(f"installed renderer digest mismatch: {digest}")
    missing = [token for token in TOKENS if token not in source]
    if missing:
        raise RuntimeError(f"alpha.2 renderer is not installed: {missing}")

helper = TOOLS / "recover_v0121_alpha2_payload.py"
if helper.exists():
    helper.unlink()

replace_version(ROOT / "gradle.properties")
replace_version(JAVA / "ArcaneCircle.java")

index_path = ROOT / "src/main/resources/data/arcanecircle/spell_catalog/index.json"
index = json.loads(index_path.read_text(encoding="utf-8"))
index["version"] = NEW_VERSION
index["release_geometry"] = "animated_signature_meshes"
index["signature_models"] = ["meteor", "tornado", "portal", "force_cage", "barrier_wall", "barrier_dome"]
index_path.write_text(json.dumps(index, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

for name in ("test_magic_contract.py", "test_v12_overhaul.py", "test_v12_1_cleanup.py"):
    path = TOOLS / name
    text = path.read_text(encoding="utf-8").replace(OLD_VERSION, NEW_VERSION)
    if name == "test_magic_contract.py":
        text = text.replace("apply_v12_1_particle_cleanup.py", "apply_v0121_alpha2_signature_geometry.py")
    path.write_text(text, encoding="utf-8")

print("Arcane Circle v0.12.1-alpha.2 signature geometry installed")
