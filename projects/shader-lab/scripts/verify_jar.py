#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import sys
import zipfile
from pathlib import Path

REQUIRED_EXACT = {
    "META-INF/neoforge.mods.toml",
    "assets/shaderlab/post_effect/lush_grade.json",
    "assets/shaderlab/shaders/post/lush_grade.fsh",
    "assets/shaderlab/lang/ko_kr.json",
    "data/shaderlab/shader_tests/lush_grade.json",
    "pack.mcmeta",
}

REQUIRED_PREFIXES = {
    "kr/moonseungjun/shaderlab/": ".class",
    "assets/shaderlab/": None,
    "data/shaderlab/": None,
}


def fail(message: str) -> None:
    print(f"JAR VERIFICATION FAILED: {message}", file=sys.stderr)
    raise SystemExit(1)


def main() -> None:
    if len(sys.argv) != 2:
        fail("usage: verify_jar.py <jar>")

    jar_path = Path(sys.argv[1])
    if not jar_path.is_file() or jar_path.stat().st_size == 0:
        fail(f"missing or empty JAR: {jar_path}")

    with zipfile.ZipFile(jar_path) as jar:
        names = jar.namelist()
        name_set = set(names)

        if len(names) != len(name_set):
            fail("duplicate ZIP entries detected")

        missing = sorted(REQUIRED_EXACT - name_set)
        if missing:
            fail(f"required entries missing: {', '.join(missing)}")

        for prefix, suffix in REQUIRED_PREFIXES.items():
            matches = [
                name for name in names
                if name.startswith(prefix) and (suffix is None or name.endswith(suffix))
            ]
            if not matches:
                fail(f"no matching entry for prefix {prefix!r} and suffix {suffix!r}")

        forbidden = [
            name for name in names
            if name.endswith(".java")
            or name.startswith(".github/")
            or name.startswith("scripts/")
            or "/raw-logs/" in name
        ]
        if forbidden:
            fail(f"development-only entries found: {', '.join(forbidden[:10])}")

        if jar.testzip() is not None:
            fail("corrupt ZIP member detected")

    digest = hashlib.sha256(jar_path.read_bytes()).hexdigest()
    checksum_path = jar_path.with_name(jar_path.name + ".sha256")
    checksum_path.write_text(f"{digest}  {jar_path.name}\n", encoding="utf-8")

    report_path = jar_path.parent.parent / "reports" / "jar-verification.txt"
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(
        "\n".join(
            [
                "Shader Lab JAR verification: PASS",
                f"JAR: {jar_path.name}",
                f"Bytes: {jar_path.stat().st_size}",
                f"SHA-256: {digest}",
                f"Required exact entries: {len(REQUIRED_EXACT)}",
            ]
        )
        + "\n",
        encoding="utf-8",
    )

    print(report_path.read_text(encoding="utf-8"), end="")


if __name__ == "__main__":
    main()
