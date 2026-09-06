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

# The 0.58 regression wrapper is intentionally translated to the current 0.61 contracts.
# Keep the historical test immutable while making its runtime copy compare against current canonical values.
release_test = ROOT / "tools/test_release_content_pack.py"
text = release_test.read_text(encoding="utf-8")
text = text.replace("산업 가공소 완공 → 통 4블록 이내", "산업 가공소 완공 → 통/공용 보급고 4블록 이내")

translation_anchor = "    ('Math.min(13, base + bonus)', 'Math.min(21, base + bonus)'),\n"
skill_translations = """    ('WOODCUTTING -> { early = 1.60D; late = 1.25D; }', 'WOODCUTTING -> { early = 2.50D; late = 2.00D; }'),
    ('HARVESTING -> { early = 1.50D; late = 1.20D; }', 'HARVESTING -> { early = 3.00D; late = 2.50D; }'),
    ('COMBAT -> { early = 1.25D; late = 1.15D; }', 'COMBAT -> { early = 4.00D; late = 3.50D; }'),
    ('CONSTRUCTION -> { early = 2.75D; late = 1.75D; }', 'CONSTRUCTION -> { early = 5.00D; late = 3.50D; }'),
    ('MOBILITY -> { early = 2.10D; late = 1.40D; }', 'MOBILITY -> { early = 4.00D; late = 3.00D; }'),
"""
if "('WOODCUTTING -> { early = 1.60D; late = 1.25D; }', 'WOODCUTTING -> { early = 2.50D; late = 2.00D; }')" not in text:
    if translation_anchor not in text:
        raise SystemExit("content audit skill translation anchor missing")
    text = text.replace(translation_anchor, translation_anchor + skill_translations)
release_test.write_text(text, encoding="utf-8")

# Fail closed if current contracts vanished.
incident_text = incident.read_text(encoding="utf-8")
release_text = release_test.read_text(encoding="utf-8")
assert 'active.reinforcementCount > 0 ? " §7· 이변 개체 1체 포함" : ""' in incident_text
assert "산업 가공소 완공 → 통/공용 보급고 4블록 이내" in release_text
assert "산업 가공소 완공 → 통 4블록 이내" not in release_text
for current in (
    "WOODCUTTING -> { early = 2.50D; late = 2.00D; }",
    "HARVESTING -> { early = 3.00D; late = 2.50D; }",
    "COMBAT -> { early = 4.00D; late = 3.50D; }",
    "CONSTRUCTION -> { early = 5.00D; late = 3.50D; }",
    "MOBILITY -> { early = 4.00D; late = 3.00D; }",
):
    assert current in release_text

print("Applied Survival Ascension 0.61.21 content/runtime hardening")
