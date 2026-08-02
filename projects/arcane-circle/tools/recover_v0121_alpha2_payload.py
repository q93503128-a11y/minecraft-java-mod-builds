#!/usr/bin/env python3
from pathlib import Path
import base64
import hashlib

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
TARGET = ROOT / "src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java"
EXPECTED = "f0791cb317960032ae36500128977cfc3fb7992d695899ebb4660b98cfbb9334"

parts = sorted(TOOLS.glob("v0121_alpha2_tracker.part-*"))
if not parts:
    raise SystemExit("renderer staging parts are missing")

def unpack(path: Path) -> bytes:
    text = "".join(path.read_text(encoding="utf-8").split()).rstrip("=")
    text += "=" * (-len(text) % 4)
    return base64.b64decode(text, validate=True)

payload = b"".join(unpack(path) for path in parts)
digest = hashlib.sha256(payload).hexdigest()
if digest != EXPECTED:
    raise SystemExit(f"renderer recovery digest mismatch: {digest}")
TARGET.write_bytes(payload)
for path in parts:
    path.unlink()
print("Arcane Circle renderer payload recovered and verified")
