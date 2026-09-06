#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    if new in text:
        return
    if old not in text:
        raise SystemExit(f"{label}: expected anchor not found in {path.relative_to(ROOT)}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


incident = ROOT / "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncidentSystem.java"
replace_once(
    incident,
    '''        notify(level, active.participants, Component.literal(prefix + "§f" + region.koreanName() + " · §e" + incident.koreanName()\n                + " §7· 참가 " + active.participantCountSnapshot + "명 · 공동 목표: §f" + objective), false);''',
    '''        String reinforcementNote = active.reinforcementCount > 0 ? " §7· 이변 개체 1체 포함" : "";\n        notify(level, active.participants, Component.literal(prefix + "§f" + region.koreanName() + " · §e" + incident.koreanName()\n                + " §7· 참가 " + active.participantCountSnapshot + "명 · 공동 목표: §f" + objective + reinforcementNote), false);''',
    "rare incident player-facing reinforcement state",
)

# The 0.58 regression wrapper is intentionally translated to the current 0.61 player-flow copy.
# Keep the historical test immutable while ensuring the current wrapper expects the actual menu wording.
release_test = ROOT / "tools/test_release_content_pack.py"
text = release_test.read_text(encoding="utf-8")
text = text.replace("산업 가공소 완공 → 통 4블록 이내", "산업 가공소 완공 → 통/공용 보급고 4블록 이내")
release_test.write_text(text, encoding="utf-8")

# Fail closed if either current contract vanished.
incident_text = incident.read_text(encoding="utf-8")
release_text = release_test.read_text(encoding="utf-8")
assert 'active.reinforcementCount > 0 ? " §7· 이변 개체 1체 포함" : ""' in incident_text
assert "산업 가공소 완공 → 통/공용 보급고 4블록 이내" in release_text
assert "산업 가공소 완공 → 통 4블록 이내" not in release_text

print("Applied Survival Ascension 0.61.21 content/runtime hardening")
