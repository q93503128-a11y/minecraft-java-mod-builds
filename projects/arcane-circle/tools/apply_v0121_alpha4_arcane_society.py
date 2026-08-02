#!/usr/bin/env python3
from pathlib import Path
import base64, gzip
TOOLS = Path(__file__).resolve().parent
parts = [TOOLS / "alpha4_installer.part-01", TOOLS / "alpha4_installer.part-02"]
payload = "".join("".join(path.read_text(encoding="utf-8").split()) for path in parts)
source = gzip.decompress(base64.b64decode(payload))
exec(compile(source, __file__, "exec"))
contract = TOOLS / "test_magic_contract.py"
text = contract.read_text(encoding="utf-8").replace(
    "apply_v0121_alpha3_freeze_hotfix.py", "apply_v0121_alpha4_arcane_society.py")
contract.write_text(text, encoding="utf-8")
for path in parts:
    if path.exists(): path.unlink()
