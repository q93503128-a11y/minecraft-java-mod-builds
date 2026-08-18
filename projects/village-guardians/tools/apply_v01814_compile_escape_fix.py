#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MESH = ROOT / "src/main/java/kr/moonseungjun/villageguardians/VillageSkillMeshLibrary.java"


def main() -> None:
    text = MESH.read_text(encoding="utf-8")
    old = r'extra.split("\|", -1)'
    new = r'extra.split("\\|", -1)'
    if text.count(old) != 1:
        raise SystemExit(f"expected one invalid Java regex escape, got {text.count(old)}")
    text = text.replace(old, new, 1)
    MESH.write_text(text, encoding="utf-8")

    # Guard the entire Java tree against the same invalid one-backslash regex escape.
    offenders = []
    for path in (ROOT / "src/main/java").rglob("*.java"):
        source = path.read_text(encoding="utf-8")
        if r'.split("\|"' in source:
            offenders.append(str(path.relative_to(ROOT)))
    if offenders:
        raise SystemExit("invalid Java regex escape remains: " + ", ".join(offenders))
    print("[PASS] Java regex pipe escape corrected and no one-backslash split remains")


if __name__ == "__main__":
    main()
