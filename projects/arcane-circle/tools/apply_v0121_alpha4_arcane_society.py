#!/usr/bin/env python3
from pathlib import Path
import base64, gzip
TOOLS = Path(__file__).resolve().parent
parts = [TOOLS / "alpha4_installer.part-01", TOOLS / "alpha4_installer.part-02"]
payload = "".join("".join(path.read_text(encoding="utf-8").split()) for path in parts)
source = gzip.decompress(base64.b64decode(payload)).decode("utf-8")
old_helper = '''    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"replacement anchor mismatch in {path}: expected 1, got {count}: {old[:100]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
'''
new_helper = '''    count = text.count(old)
    if old == OLD_VERSION:
        if count < 1:
            raise RuntimeError(f"version anchor missing in {path}: {old}")
        path.write_text(text.replace(old, new), encoding="utf-8")
        return
    if count != 1:
        raise RuntimeError(f"replacement anchor mismatch in {path}: expected 1, got {count}: {old[:100]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
'''
if old_helper not in source:
    raise RuntimeError("installer helper patch anchor missing")
source = source.replace(old_helper, new_helper, 1)
exec(compile(source, __file__, "exec"))
contract = TOOLS / "test_magic_contract.py"
text = contract.read_text(encoding="utf-8").replace(
    "apply_v0121_alpha3_freeze_hotfix.py", "apply_v0121_alpha4_arcane_society.py")
contract.write_text(text, encoding="utf-8")
for path in parts:
    if path.exists(): path.unlink()
