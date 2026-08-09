from __future__ import annotations

from pathlib import Path
import subprocess
import sys

ROOT = Path.cwd()
WORKSPACE = ROOT.parent.parent
BASE = WORKSPACE / ".github/scripts/arcane-circle/apply_v0121_alpha18_visual_polish.py"


def main() -> None:
    subprocess.run([sys.executable, str(BASE)], cwd=ROOT, check=True)

    rel = Path("src/main/java/kr/moonseungjun/arcanecircle/magic/WorldMagicService.java")
    text = rel.read_text(encoding="utf-8")
    text = text.replace(
        "SpellPresentationProfile.MotionStyle.PRISON\\n                    ? target.add(0.0, 0.055, 0.0) : target;",
        "SpellPresentationProfile.MotionStyle.PRISON\n                    ? target.add(0.0, 0.055, 0.0) : target;",
    )
    rel.write_text(text, encoding="utf-8")

    print("Arcane Circle alpha.18 visual polish migration v2: PASS")


if __name__ == "__main__":
    main()
