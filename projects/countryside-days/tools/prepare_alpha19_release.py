#!/usr/bin/env python3
from pathlib import Path
import base64
import hashlib

HERE = Path(__file__).resolve().parent
PARTS = [HERE / f"alpha19_release_{index:02d}.part" for index in range(6)]
normalized = []
for path in PARTS:
    content = "".join(path.read_text("ascii").split())
    normalized.append(content)
    print(path.name, len(content), hashlib.sha256(content.encode("ascii")).hexdigest())
encoded = "".join(normalized)
print("alpha19 encoded", len(encoded), hashlib.sha256(encoded.encode("ascii")).hexdigest())
payload = base64.b64decode(encoded, validate=True)
expected = "36e556f6f1efca58fb5b2066d96a7c5c1206cea4b0cd12e571a6ab3f89872370"
actual = hashlib.sha256(payload).hexdigest()
if actual != expected:
    raise SystemExit(f"alpha19 payload sha mismatch: {actual}")
namespace = {
    "__name__": "__main__",
    "__file__": str(HERE / "prepare_alpha19_release_payload.py"),
}
exec(compile(payload, namespace["__file__"], "exec"), namespace)
