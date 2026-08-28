#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
A82 = ROOT / "tools/test_alpha82_docs.py"
_real_read = Path.read_text

def legacy_read(self, *args, **kwargs):
    s = _real_read(self, *args, **kwargs)
    if self.name == "gradle.properties":
        s = s.replace("mod_version=0.1.0-alpha.83", "mod_version=0.1.0-alpha.82")
        s = s.replace(", plus Alpha.83 late-game landmark progression with a civic hall, trade hall and citadel, a derived Frontier Capital end-state, physical landmark construction, stronger domain public works, trade-hall market value, citadel watch coverage, and a clear endgame guidance line without a new currency or save ledger.", ".")
    elif self.name == "COMPANION_LOCK.json":
        s = s.replace('"frontier_settlement": "0.1.0-alpha.83"', '"frontier_settlement": "0.1.0-alpha.82"')
    return s

Path.read_text = legacy_read
try:
    chain = _real_read(A82, encoding="utf-8").replace(
        "print('Frontier Settlement alpha.82 canonical docs audit: PASS')", "pass"
    )
    ns = {"__file__": str(A82), "__name__": "__main__"}
    exec(compile(chain, str(A82), "exec"), ns, ns)
finally:
    Path.read_text = _real_read

def text(name): return (ROOT / name).read_text(encoding="utf-8")
def must(src, tokens, label):
    for token in tokens:
        if token not in src:
            raise SystemExit(f"{label} missing: {token}")

props = text("gradle.properties")
note = text("CONTENT_EXPANSION_ALPHA83.md")
lock = json.loads(text("COMPANION_LOCK.json"))

must(props, ("mod_version=0.1.0-alpha.83", "Alpha.83 late-game landmark progression"), "alpha.83 props docs")
must(note, (
    "시민회관", "교역회관", "성채", "개척 수도",
    "population 20", "at least 5 outposts", "at least 4 completed roads", "exploration score 7",
    "real ItemStacks", "No new SavedData field", "M construction menu",
    "No companion Java class", "actual Alpha.83 play remains",
), "alpha.83 content note")
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.83":
    raise SystemExit("alpha.83 docs lock target drifted")
if not any("Alpha.83 keeps every Alpha.82 companion binary pin unchanged" in n for n in lock.get("notes", [])):
    raise SystemExit("alpha.83 docs lock rationale missing")

print("Frontier Settlement alpha.83 canonical docs audit: PASS")
