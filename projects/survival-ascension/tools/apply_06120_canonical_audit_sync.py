#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/survivalascension"


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one target, found {count}: {old[:120]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def replace_all_required(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count <= 0:
        raise SystemExit(f"{path}: expected at least one target: {old[:120]!r}")
    path.write_text(text.replace(old, new), encoding="utf-8")


# Restore explicit visibility for optional-content incident participants. A rare incident can
# contain both the normal replacement slot and the extra rare reinforcement, so report the real count.
incident = JAVA / "expedition/ExpeditionIncidentSystem.java"
replace_once(
    incident,
    '''        notify(level, active.participants, Component.literal(prefix + "§f" + region.koreanName() + " · §e" + incident.koreanName()\n                + " §7· 참가 " + active.participantCountSnapshot + "명 · 공동 목표: §f" + objective), false);\n''',
    '''        int anomalyCount = active.contentReplacementCount + active.reinforcementCount;\n        String anomalyNote = anomalyCount == 1\n                ? " §7· §d이변 개체 1체 포함"\n                : anomalyCount > 1 ? " §7· §d이변 개체 " + anomalyCount + "체 포함" : "";\n        notify(level, active.participants, Component.literal(prefix + "§f" + region.koreanName() + " · §e" + incident.koreanName()\n                + " §7· 참가 " + active.participantCountSnapshot + "명 · 공동 목표: §f" + objective + anomalyNote), false);\n''',
)

# The historical audit is a compatibility wrapper. Advance only its current-runtime expectations;
# the immutable 0.58 source audit remains untouched and is translated in-memory.
source_audit = ROOT / "tools/test_release_source.py"
replace_once(source_audit, 'CURRENT_VERSION = "0.61.0-alpha.1"', 'CURRENT_VERSION = "0.61.20-alpha.1"')
replace_once(source_audit, 'PREVIOUS_DOC_VERSION = "0.59.0-alpha.1"', 'PREVIOUS_DOC_VERSION = "0.61.20-alpha.1"')
replace_all_required(source_audit,
                     '산업 가공소 완공 → 통 4블록 이내',
                     '산업 가공소 완공 → 통/공용 보급고 4블록 이내')
replace_all_required(source_audit,
                     '"MAX_PENDING_PER_PLAYER = 384"',
                     '"MAX_PENDING_PER_PLAYER = 1152"')
replace_once(source_audit,
             '"Mod version: `0.59.0-alpha.1`", "## 0.59 Apex Content Escort Integration"',
             '"Mod version: `0.61.20-alpha.1`", "## 0.59 Apex Content Escort Integration"')
replace_once(
    source_audit,
    '''# 0.61 also replaces the player-facing developer term "affix" with "승천 옵션".\n''',
    '''# Current runtime translations for invariants whose authority intentionally advanced after 0.58.\nlegacy = legacy.replace('PROTOCOL = "9"', 'PROTOCOL = "15"')\nlegacy = legacy.replace('MAX_PENDING_PER_PLAYER = 384', 'MAX_PENDING_PER_PLAYER = 1152')\n\n# 0.61 also replaces the player-facing developer term "affix" with "승천 옵션".\n''',
)

# Content-pack audit keeps the locked external pack identity, but its current project/UI needles
# must follow the shared-depot wording. Incident visibility is restored in runtime above.
content_audit = ROOT / "tools/test_release_content_pack.py"
replace_once(content_audit, 'PREVIOUS_DOC_VERSION = "0.59.0-alpha.1"', 'PREVIOUS_DOC_VERSION = "0.61.20-alpha.1"')
replace_all_required(content_audit,
                     '산업 가공소 완공 → 통 4블록 이내',
                     '산업 가공소 완공 → 통/공용 보급고 4블록 이내')

# test_release_content_pack.py executes a nested historical content source audit. Its authored
# skill-XP literals predate the deliberate 0.61 solo-pacing retune, so translate every changed
# skill at both wrapper levels in-memory instead of weakening the immutable baseline audit.
replace_all_required(
    content_audit,
    "    ('Math.min(13, base + bonus)', 'Math.min(21, base + bonus)'),",
    '''    ('WOODCUTTING -> { early = 1.60D; late = 1.25D; }', 'WOODCUTTING -> { early = 2.50D; late = 2.00D; }'),\n    ('HARVESTING -> { early = 1.50D; late = 1.20D; }', 'HARVESTING -> { early = 3.00D; late = 2.50D; }'),\n    ('COMBAT -> { early = 1.25D; late = 1.15D; }', 'COMBAT -> { early = 4.00D; late = 3.50D; }'),\n    ('CONSTRUCTION -> { early = 2.75D; late = 1.75D; }', 'CONSTRUCTION -> { early = 5.00D; late = 3.50D; }'),\n    ('MOBILITY -> { early = 2.10D; late = 1.40D; }', 'MOBILITY -> { early = 4.00D; late = 3.00D; }'),\n    ('Math.min(13, base + bonus)', 'Math.min(21, base + bonus)'),''',
)

# Lock these manual regressions into the current-source checker too.
current = ROOT / "tools/test_current_source.py"
replace_once(
    current,
    '''require("BEHAVIOR_INTERVAL = 20" in warband, "warband broad scan cadence regressed")\n''',
    '''incident = text(JAVA / "expedition/ExpeditionIncidentSystem.java")\nrequire("anomalyCount = active.contentReplacementCount + active.reinforcementCount" in incident\n        and "이변 개체 1체 포함" in incident,\n        "optional-content incident participants are no longer explicitly visible to players")\n\nrequire("BEHAVIOR_INTERVAL = 20" in warband, "warband broad scan cadence regressed")\n''',
)

# Keep the current changelog explicit about the restored player-facing signal and audit authority.
changelog = ROOT / "CHANGELOG.md"
replace_once(
    changelog,
    '''- High-end Harvesting queued work cap rises from 384 to 1152 targets so large Mythic hoe areas/forward lanes are not silently truncated. Execution remains bounded at the existing 12 local / 64 global targets per tick.\n''',
    '''- High-end Harvesting queued work cap rises from 384 to 1152 targets so large Mythic hoe areas/forward lanes are not silently truncated. Execution remains bounded at the existing 12 local / 64 global targets per tick.\n- Optional-content field incidents again state the real number of `이변 개체` included at incident start, including the rare extra reinforcement, while the mobs remain physically spawned/glowing and bounded by the same encounter limits.\n''',
)

print("Applied Survival Ascension 0.61.20 canonical-audit sync")
