#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[2]
PROJECT = ROOT / "projects/frontier-settlement"
CLIENT = PROJECT / "src/main/java/kr/moonseungjun/frontiersettlement/client"


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"expected patch anchor missing: {path}: {old[:100]!r}")
    if text.count(old) != 1:
        raise SystemExit(f"patch anchor not unique: {path}: {old[:100]!r}")
    path.write_text(text.replace(old, new), encoding="utf-8")


def append_once(path: Path, marker: str, block: str) -> None:
    text = path.read_text(encoding="utf-8")
    if marker in text:
        return
    if not text.endswith("\n"):
        text += "\n"
    path.write_text(text + "\n" + block.strip() + "\n", encoding="utf-8")


# Version and canonical current-version label.
replace_once(PROJECT / "gradle.properties", "mod_version=0.1.0-alpha.124", "mod_version=0.1.0-alpha.125")
replace_once(PROJECT / "README.md", "## Current version: 0.1.0-alpha.124", "## Current version: 0.1.0-alpha.125")
replace_once(
    PROJECT / "tools/test_current_source.py",
    'require("mod_version=0.1.0-alpha.124" in gradle, "current verifier/version drift")',
    'require("mod_version=0.1.0-alpha.125" in gradle, "current verifier/version drift")'
)

# Companion lock target is version metadata only; third-party pins/hashes stay byte-identical.
lock_path = PROJECT / "COMPANION_LOCK.json"
lock = json.loads(lock_path.read_text(encoding="utf-8"))
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.124":
    raise SystemExit("unexpected companion Frontier version before Alpha.125")
lock["target"]["frontier_settlement"] = "0.1.0-alpha.125"
note = ("Frontier Alpha.125 changes only the project-owned Frontier runtime JAR and presentation: "
        "RTS operations summary, bottleneck/upgrade backlog visibility, and corrected per-building upgrade UI. "
        "Third-party companion pins/hashes remain unchanged.")
if note not in lock.setdefault("notes", []):
    lock["notes"].append(note)
lock_path.write_text(json.dumps(lock, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

for name in ("resolved-lock.client.json", "resolved-lock.server.json"):
    path = PROJECT / "companion-testpack" / name
    data = json.loads(path.read_text(encoding="utf-8"))
    if data.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.124":
        raise SystemExit(f"unexpected {name} Frontier version before Alpha.125")
    data["target"]["frontier_settlement"] = "0.1.0-alpha.125"
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

# Add the operations entry point without adding a new hotkey or top-level gameplay authority.
palette = CLIENT / "BuildingPaletteScreen.java"
replace_once(
    palette,
    '''        int closeWidth = compact ? 42 : 48;\n        int guideWidth = compact ? 46 : 58;\n        int y = panelY + FrontierUiTheme.S;\n        int closeX = panelX + panelWidth - FrontierUiTheme.S - closeWidth;\n        int guideX = closeX - FrontierUiTheme.XS - guideWidth;\n        addRenderableWidget(Button.builder(Component.literal(compact ? "도움" : "가이드"),\n                b -> this.minecraft.gui.setScreen(new SettlementGuideScreen(this, 0)))\n                .bounds(guideX, y, guideWidth, 18).build());\n        addRenderableWidget(Button.builder(Component.literal("닫기"), b -> this.onClose())\n                .bounds(closeX, y, closeWidth, 18).build());''',
    '''        int closeWidth = compact ? 42 : 48;\n        int guideWidth = compact ? 46 : 58;\n        int operationsWidth = compact ? 42 : 52;\n        int y = panelY + FrontierUiTheme.S;\n        int closeX = panelX + panelWidth - FrontierUiTheme.S - closeWidth;\n        int guideX = closeX - FrontierUiTheme.XS - guideWidth;\n        int operationsX = guideX - FrontierUiTheme.XS - operationsWidth;\n        addRenderableWidget(Button.builder(Component.literal("운영"),\n                b -> this.minecraft.gui.setScreen(new SettlementOperationsScreen(this)))\n                .bounds(operationsX, y, operationsWidth, 18).build());\n        addRenderableWidget(Button.builder(Component.literal(compact ? "도움" : "가이드"),\n                b -> this.minecraft.gui.setScreen(new SettlementGuideScreen(this, 0)))\n                .bounds(guideX, y, guideWidth, 18).build());\n        addRenderableWidget(Button.builder(Component.literal("닫기"), b -> this.onClose())\n                .bounds(closeX, y, closeWidth, 18).build());'''
)

old_effect = '''    private static String specialEffect(SettlementSnapshotPayload data, BuildingType type) {\n        if (type == BuildingType.CIVIC_HALL) return "주민 유입 20초 · 건설 인력 +2";\n        if (type == BuildingType.LUMBER_CAMP || type == BuildingType.FARM\n                || type == BuildingType.QUARRY || type == BuildingType.MINE) {\n            int rank = tierRank(data.tier());\n            int grade = rank <= 2 ? 1 : rank == 3 ? 2 : rank == 4 ? 3 : 4;\n            int buffers = grade == 1 ? 1 : grade == 2 ? 2 : 3;\n            String label = switch (grade) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; default -> "IV"; };\n            return "개량 " + label + " · 현장 버퍼 " + buffers + "통";\n        }\n        return "";\n    }\n'''
new_effect = '''    private static String specialEffect(SettlementSnapshotPayload data, BuildingType type) {\n        if (type == BuildingType.CIVIC_HALL) return "주민 유입 20초 · 건설 인력 +2";\n        if (type == BuildingType.LUMBER_CAMP || type == BuildingType.FARM\n                || type == BuildingType.QUARRY || type == BuildingType.MINE) {\n            int ceiling = productionCeiling(data.tier());\n            return "신규 개량 I · 현재 상한 " + gradeLabel(ceiling) + " · 완공 후 현장 저장통에서 수동 개량";\n        }\n        if (type == BuildingType.WAREHOUSE || type == BuildingType.CART_STATION) {\n            int ceiling = logisticsCeiling(data.tier());\n            return "신규 물류 I · 현재 상한 " + gradeLabel(ceiling) + " · 완공 후 저장통에서 수동 확장";\n        }\n        if (type == BuildingType.GUARD_POST || type == BuildingType.WATCHTOWER\n                || type == BuildingType.BARRACKS || type == BuildingType.CITADEL) {\n            int ceiling = militaryCeiling(data.tier());\n            return "신규 군사 I · 현재 상한 " + gradeLabel(ceiling) + " · 완공 후 지휘 지점에서 수동 개량";\n        }\n        return "";\n    }\n\n    private static int productionCeiling(String tier) {\n        return switch (tier) {\n            case "마을" -> 2;\n            case "개척 도시" -> 3;\n            case "영지", "개척 수도" -> 4;\n            default -> 1;\n        };\n    }\n\n    private static int logisticsCeiling(String tier) {\n        return switch (tier) {\n            case "개척 도시" -> 2;\n            case "영지", "개척 수도" -> 3;\n            default -> 1;\n        };\n    }\n\n    private static int militaryCeiling(String tier) {\n        return switch (tier) {\n            case "개척 도시" -> 2;\n            case "영지" -> 3;\n            case "개척 수도" -> 4;\n            default -> 1;\n        };\n    }\n\n    private static String gradeLabel(int grade) {\n        return switch (Math.max(1, Math.min(4, grade))) {\n            case 1 -> "I";\n            case 2 -> "II";\n            case 3 -> "III";\n            default -> "IV";\n        };\n    }\n'''
replace_once(palette, old_effect, new_effect)

# Canonical documentation: one compact management surface, no new resource/simulation authority.
readme_section = '''
## Alpha.125 RTS operations visibility

- The existing M settlement palette now has an `운영` entry; no new global hotkey or separate management framework is introduced.
- The operations screen summarizes population/housing, production staffing/runtime blockers, physical resource stock, warehouse/cart capacity, defense infrastructure and outpost count from the snapshot/context already synchronized for HUD/Jade/location presentation.
- Opening the screen never launches a second server entity/world scan and never creates a save ledger, tax, virtual currency, happiness meter or family simulation.
- Production/logistics/military upgrade backlog is derived from each completed physical building's persisted grade versus the settlement tier's current unlock ceiling, making late-game wood/stone/copper/iron investment visible instead of silently granting upgrades.
- The screen surfaces one deterministic priority such as blocked construction, housing saturation, missing production workers, storage saturation, production blockers or available facility investment.
- The construction palette now states the real rule for newly completed facilities: production/logistics/military buildings start at grade I. Settlement tier unlocks only the maximum purchasable grade.
- Existing world interaction remains authoritative for upgrades: empty-hand sneak-right-click at the appropriate worksite/storage/command point, with real loaded settlement resources consumed by the existing server service.
'''
readme = PROJECT / "README.md"
text = readme.read_text(encoding="utf-8")
anchor = "\n## Functional building families\n"
if "## Alpha.125 RTS operations visibility" not in text:
    if anchor not in text:
        raise SystemExit("README Alpha.125 insertion anchor missing")
    text = text.replace(anchor, readme_section + anchor, 1)
    readme.write_text(text, encoding="utf-8")

plan_section = '''
## Alpha.125 operations-surface lock

The RTS-management layer remains presentation-first and low-micromanagement:

- `M -> 운영` is the single compact settlement operations summary; do not create a second hotkey-heavy dashboard family.
- Existing authoritative snapshot/context data is reused. Opening operations UI must not trigger a second server entity/resource/world scan.
- Show actionable bottlenecks and paid upgrade backlog before inventing taxes, happiness, families, abstract workforce points or virtual resources.
- Facility grades remain physical-building state. Tier unlocks a ceiling; the player pays real settlement wood/stone/copper/iron at the existing world interaction point.
- A new production/logistics/military building starts at grade I; UI must never imply that settlement tier grants its grade for free.
- Long-term RTS pressure should come from visible construction, storage, equipment, military, logistics and facility investment sinks rather than passive daily deletion of resources.
'''
append_once(PROJECT / "CANONICAL_PLAN.md", "## Alpha.125 operations-surface lock", plan_section)

# Extend the cumulative source audit so future changes cannot silently reintroduce the stale free-tier UI.
audit_block = '''
# Alpha.125 RTS operations visibility.
operations_summary = text(JAVA / "client" / "SettlementOperationsSummary.java")
operations_screen = text(JAVA / "client" / "SettlementOperationsScreen.java")
palette_screen = text(JAVA / "client" / "BuildingPaletteScreen.java")
require("Presentation-only RTS summary" in operations_summary and "snapshot.context().targets()" in operations_summary,
        "operations summary stopped reusing the existing presentation snapshot/context")
require("productionUpgradeBacklog" in operations_summary and "logisticsUpgradeBacklog" in operations_summary
        and "militaryUpgradeBacklog" in operations_summary, "RTS paid-upgrade backlog visibility missing")
require("ClientSettlementState.snapshot()" in operations_screen and "SettlementOperationsSummary.from(snapshot)" in operations_screen,
        "operations screen is not driven by synchronized client presentation state")
require("new SettlementOperationsScreen(this)" in palette_screen, "M palette operations entry point missing")
require("신규 개량 I" in palette_screen and "신규 물류 I" in palette_screen and "신규 군사 I" in palette_screen,
        "construction palette returned to misleading free tier-derived facility grades")
require("완공 후 현장 저장통에서 수동 개량" in palette_screen,
        "production investment interaction guidance missing from construction palette")
'''
append_once(PROJECT / "tools/test_current_source.py", "# Alpha.125 RTS operations visibility.", audit_block)

print("Alpha.125 RTS operations patch applied")
