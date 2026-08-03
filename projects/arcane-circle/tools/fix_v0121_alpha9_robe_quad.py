#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneGearRenderer.java"
text = path.read_text(encoding="utf-8")
old = '''            quad(out, pose, -shoulder, top - 0.045F, -0.30F, shoulder, top - 0.045F, -0.30F,
                    waist, chest - 0.035F, -0.325F, -waist, chest - 0.035F, shadow);'''
new = '''            quad(out, pose, -shoulder, top - 0.045F, -0.30F, shoulder, top - 0.045F, -0.30F,
                    waist, chest - 0.035F, -0.325F, -waist, chest - 0.035F, -0.325F, shadow);'''
if new not in text:
    if old not in text:
        raise SystemExit("alpha.9 robe underside quad anchor not found")
    text = text.replace(old, new, 1)
    path.write_text(text, encoding="utf-8")
print("Arcane Circle alpha.9 robe underside quad fixed")
