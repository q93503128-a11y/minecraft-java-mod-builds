#!/usr/bin/env python3
from __future__ import annotations

import base64
import hashlib
import io
import subprocess
import sys
import zipfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
PROJECT = REPO_ROOT / "projects" / "countryside-days"
TOOLS = PROJECT / "tools"

PARTS = [
    ("alpha16_patch_00.part", 20000, "de32a146ec061889572bda6fd77e1fd3ceb83ab279eec778fb02d7e6f5044c52"),
    ("alpha16_patch_01.part", 20000, "2324090e1d25151a22a47b62229911c90b4713a876169a8333aa0299a3fc45fc"),
    ("alpha16_patch_02.part", 20000, "80182dad1af4bab46bf7253940c0fbd534501d8b66ef2e8a860c20e53c53c1f6"),
    ("alpha16_patch_03.part", 19884, "fb7829d9b28d4effb2f50438179a8d642d8c0e77a19d3d2d996ced68d731991e"),
]
ENCODED_LEN = 79884
ENCODED_SHA = "a2c8b2177646c165e488a7958fa414742a8daf1d9f40f63308cfcc5c31773d54"
ARCHIVE_SHA = "e5942fbae1c58ab7cbb6808971f7cb7d4c24da5b1618472e698a058b44c29faa"


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def restore_alpha15() -> None:
    subprocess.run(
        [sys.executable, str(TOOLS / "prepare_alpha15_exact.py")],
        cwd=REPO_ROOT,
        check=True,
    )


def apply_alpha16_patch() -> None:
    chunks: list[str] = []
    for name, expected_len, expected_sha in PARTS:
        path = TOOLS / name
        if not path.is_file():
            raise FileNotFoundError(f"missing alpha.16 patch part: {path}")
        text = "".join(path.read_text("ascii").split())
        actual_sha = sha256(text.encode("ascii"))
        if len(text) != expected_len or actual_sha != expected_sha:
            raise RuntimeError(
                f"bad alpha.16 patch part {name}: len={len(text)} sha={actual_sha}"
            )
        chunks.append(text)

    encoded = "".join(chunks)
    if len(encoded) != ENCODED_LEN:
        raise RuntimeError(f"bad alpha.16 patch text length: {len(encoded)}")
    if sha256(encoded.encode("ascii")) != ENCODED_SHA:
        raise RuntimeError("bad alpha.16 patch text sha")

    archive = base64.b64decode(encoded, validate=True)
    if sha256(archive) != ARCHIVE_SHA:
        raise RuntimeError("bad alpha.16 patch archive sha")

    with zipfile.ZipFile(io.BytesIO(archive)) as zf:
        bad = zf.testzip()
        if bad:
            raise RuntimeError(f"bad alpha.16 ZIP member: {bad}")
        for name in zf.namelist():
            member = Path(name)
            if member.is_absolute() or ".." in member.parts:
                raise RuntimeError(f"unsafe alpha.16 ZIP member: {name}")
        zf.extractall(PROJECT)

    props = (PROJECT / "gradle.properties").read_text("utf-8")
    if "mod_version=0.1.0-alpha.16" not in props:
        raise RuntimeError("patched project is not alpha.16")


if __name__ == "__main__":
    restore_alpha15()
    apply_alpha16_patch()
    print("Countryside Days alpha.16 exact source prepared successfully")
