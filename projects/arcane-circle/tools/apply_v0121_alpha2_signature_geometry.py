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
PART_SHA256 = (
    "71ec6cb1ab7538410d7a3d1c36abe0d50a28494eccd278239340424043599490",
    "c39b431ea04f4995829404e01ef194c6178b857f39b92ca77fa2f148cf70e5b7",
    "09f6265f4d5efaea50e72edfda58f03ce6dfe47d3f60351b4560d7f096c309ed",
    "d48b20ecad1804f10d0ea08124970afc7101a7f7d747507caf3e9e0aeceeadb6",
    "f1d728d42ae481e12c2f81e4723b59e8ed13330cfff319db6b036ce8cebd3659",
    "01ad0faa93784a3450d0778c5a97f8365aaa6423826ec56054d69232687c36d2",
    "2f8ba426ca93db4ef4be5c4ea336e43a5be38c74b691796318c667ed7842e136",
    "e3da4c734cd98ee5b3006fefda7acc4e16fe87ee559326afbffb4b98260c5e30",
)
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
    decoded = [unpack(path) for path in parts]
    if len(decoded) != len(PART_SHA256):
        raise RuntimeError(f"renderer part count mismatch: {len(decoded)}")
    mismatches = []
    for index, (path, payload, expected) in enumerate(zip(parts, decoded, PART_SHA256)):
        actual = hashlib.sha256(payload).hexdigest()
        if actual != expected:
            mismatches.append(f"{index}:{path.name}:{len(payload)}:{actual}")
    if mismatches:
        raise RuntimeError("renderer part digest mismatch: " + ", ".join(mismatches))
    source_bytes = b"".join(decoded)
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
