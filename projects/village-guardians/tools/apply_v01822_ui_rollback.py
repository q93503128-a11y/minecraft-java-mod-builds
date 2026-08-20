#!/usr/bin/env python3
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
BASELINE = "b899b03ce80fe57b5524494e8a20d2025e60d7a0"
REPO_PREFIX = "projects/village-guardians/"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(path: Path, old: str, new: str) -> None:
    text = read(path)
    if old not in text:
        raise SystemExit(f"missing anchor in {path}: {old[:180]!r}")
    write(path, text.replace(old, new, 1))


def baseline_file(relative: str) -> str:
    repo_path = REPO_PREFIX + relative
    result = subprocess.run(
        ["git", "show", f"{BASELINE}:{repo_path}"],
        cwd=ROOT.parents[1],
        check=True,
        capture_output=True,
        text=True,
        encoding="utf-8",
    )
    return result.stdout


# Release version.
props = ROOT / "gradle.properties"
replace_once(props, "mod_version=0.18.21-alpha.1", "mod_version=0.18.22-alpha.1")

# Restore the exact pre-defense-pass HUD behavior the player explicitly preferred.
for name in (
    "VillageHudSystem.java",
    "VillageMainHudOverlay.java",
    "VillageSkillHudOverlay.java",
    "VillageStructureHud.java",
):
    write(JAVA / name, baseline_file("src/main/java/kr/moonseungjun/villageguardians/" + name))

# Keep all post-0.18.15 functionality in the large command screens, but restore their local palette
# rather than the rejected centralized mobile-defense theme.
command = JAVA / "VillageCommandCenterScreen.java"
replace_once(command,
'''    private static final int OVERLAY = VillageDefenseUiTheme.BACKDROP;\n    private static final int TEXT = VillageDefenseUiTheme.TEXT;\n    private static final int MUTED = VillageDefenseUiTheme.MUTED;\n    private static final int CYAN = VillageDefenseUiTheme.CYAN;\n    private static final int GOLD = VillageDefenseUiTheme.GOLD;\n    private static final int RED = VillageDefenseUiTheme.RED;\n    private static final int SURFACE = VillageDefenseUiTheme.PANEL_SOFT;\n    private static final int SURFACE_2 = VillageDefenseUiTheme.PANEL_ACTIVE;\n    private static final int LINE = VillageDefenseUiTheme.EDGE;\n''',
'''    private static final int OVERLAY = 0x70070A0D;\n    private static final int TEXT = 0xFFF1F4F5;\n    private static final int MUTED = 0xFFAAB5BA;\n    private static final int CYAN = 0xFF52D9C2;\n    private static final int GOLD = 0xFFFFC65C;\n    private static final int RED = 0xFFE06E64;\n    private static final int SURFACE = 0xD1131B1F;\n    private static final int SURFACE_2 = 0xD51A252A;\n    private static final int LINE = 0xA34B6873;\n''')

town = JAVA / "VillageTownHallGridScreen.java"
replace_once(town,
'''    private static final int OVERLAY = VillageDefenseUiTheme.BACKDROP;\n    private static final int PANEL = VillageDefenseUiTheme.PANEL;\n    private static final int PANEL_2 = VillageDefenseUiTheme.PANEL_SOFT;\n    private static final int PANEL_3 = VillageDefenseUiTheme.PANEL_ACTIVE;\n    private static final int LINE = VillageDefenseUiTheme.EDGE;\n    private static final int TEXT = VillageDefenseUiTheme.TEXT;\n    private static final int MUTED = VillageDefenseUiTheme.MUTED;\n    private static final int CYAN = VillageDefenseUiTheme.CYAN;\n    private static final int GOLD = VillageDefenseUiTheme.GOLD;\n    private static final int RED = VillageDefenseUiTheme.RED;\n    private static final int GREEN = VillageDefenseUiTheme.GREEN;\n    private static final int BLUE = VillageDefenseUiTheme.BLUE;\n''',
'''    private static final int OVERLAY = 0x7805090C;\n    private static final int PANEL = 0xF00B1217;\n    private static final int PANEL_2 = 0xE9142027;\n    private static final int PANEL_3 = 0xE91B2A32;\n    private static final int LINE = 0xB04F6873;\n    private static final int TEXT = 0xFFF3F5F5;\n    private static final int MUTED = 0xFFA8B4B9;\n    private static final int CYAN = 0xFF50D9C1;\n    private static final int GOLD = 0xFFF2C35D;\n    private static final int RED = 0xFFE56A64;\n    private static final int GREEN = 0xFF76D39A;\n    private static final int BLUE = 0xFF7AA9E8;\n''')

# Remove the rejected HUD-only snapshot contract. Raid gameplay/status remains authoritative elsewhere.
raid = JAVA / "VillageRaidSystem.java"
text = read(raid)
start = text.find("    /** Compact authoritative snapshot for the defense HUD; never reparses formatted status text. */")
end = text.find("    public static String status()", start)
if start < 0 or end < 0:
    raise SystemExit("missing RaidHudSnapshot rollback anchors")
write(raid, text[:start] + text[end:])

# The two 0.18.16 UI-only classes are deliberately retired. World-space presentation from that release stays.
for name in ("VillageDefenseHudFrame.java", "VillageDefenseUiTheme.java"):
    path = JAVA / name
    if path.exists():
        path.unlink()

# The rejected UI test must not remain as a permanent requirement. Its world/VFX guarantees are retained by 0.18.22.
rejected_test = ROOT / "tools/test_v01816_mobile_defense_ui.py"
if rejected_test.exists():
    rejected_test.unlink()

# v0.18.21 is now historical: it keeps testing its feature contract but no longer owns the current release string.
v01821 = ROOT / "tools/test_v01821_raid_lifecycle_presentation.py"
replace_once(v01821,
'''    require("version is v0.18.21-alpha.1", "mod_version=0.18.21-alpha.1" in props)\n''',
'''    require("release metadata remains present", "mod_version=" in props)\n''')

# Guard against any surviving Java dependency on the rejected theme/frame.
for path in JAVA.glob("*.java"):
    source = read(path)
    if "VillageDefenseHudFrame" in source or "VillageDefenseUiTheme" in source:
        raise SystemExit(f"rejected UI dependency remains in {path.name}")

print("[PASS] v0.18.22 pre-defense-pass UI restored without reverting post-0.18.16 gameplay")
