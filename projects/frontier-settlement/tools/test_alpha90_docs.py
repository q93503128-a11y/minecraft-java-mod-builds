#!/usr/bin/env python3
import shutil
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
ALPHA89_SHA = "0536cf2e2d554005ca0683f3ec3ff6f9b0a91d26"

tmp = Path(tempfile.mkdtemp(prefix="frontier-alpha89-docs-"))
try:
    subprocess.run(["git", "worktree", "add", "--detach", str(tmp), ALPHA89_SHA], cwd=REPO, check=True,
                   stdout=subprocess.DEVNULL)
    subprocess.run(["python3", str(tmp / "projects/frontier-settlement/tools/test_alpha89_docs.py")],
                   cwd=tmp, check=True)
finally:
    subprocess.run(["git", "worktree", "remove", "--force", str(tmp)], cwd=REPO,
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    shutil.rmtree(tmp, ignore_errors=True)

doc = (ROOT / "WORKER_MAINTENANCE_ALPHA90.md").read_text(encoding="utf-8")
props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
for token in (
    "`0.1.0-alpha.90`",
    "`/frontier normalize`",
    "100%-step",
    "농장 공사 100% · 마감 확인",
    "lowest contiguous same-species trunk base",
    "greatest physical trunk-log supply",
    "One shared `건설 주민` remains by design",
    "No virtual resource ledger",
):
    if token not in doc:
        raise SystemExit(f"alpha.90 docs missing: {token}")
if "mod_version=0.1.0-alpha.90" not in props:
    raise SystemExit("alpha.90 docs/version mismatch")
print("Frontier Settlement alpha.90 canonical docs audit: PASS")
