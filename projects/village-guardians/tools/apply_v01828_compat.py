#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
role_path = ROOT / "src/main/java/kr/moonseungjun/villageguardians/VillageRole.java"
text = role_path.read_text(encoding="utf-8")
old = "공중 적에게 화살 피해가 18% 증가하고 처치 시 화살을 회수합니다."
new = "공중 적에게 화살 피해가 18% 증가하고 화살로 처치하면 사용 화살을 회수합니다."
if old not in text:
    raise SystemExit("missing v0.18.28 ranger compatibility wording")
role_path.write_text(text.replace(old, new, 1), encoding="utf-8")
trigger_note = ROOT / ".ci/v01828-trigger-note.txt"
if trigger_note.exists():
    trigger_note.unlink()
Path(__file__).unlink()
