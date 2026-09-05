from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT / "projects/survival-ascension"
HUD = PROJECT / "src/main/java/kr/moonseungjun/survivalascension/client/SkillHudOverlay.java"
MAIN = PROJECT / "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java"
GRADLE = PROJECT / "gradle.properties"
VERIFY = PROJECT / "tools/test_current_source.py"
CHANGELOG = PROJECT / "CHANGELOG.md"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, got {count}")
    return text.replace(old, new, 1)

hud = HUD.read_text(encoding="utf-8")
hud = replace_once(
    hud,
    "        int width = Math.max(118, minecraft.font.width(label) + 16);\n"
    "        int left = (graphics.guiWidth() - width) / 2;\n"
    "        int top = 28;\n",
    "        int width = Math.max(118, minecraft.font.width(label) + 16);\n"
    "        int top = 8;\n"
    "        int rightMargin = 8;\n"
    "        int preferredLeft = graphics.guiWidth() - width - rightMargin;\n"
    "        int left;\n"
    "        if (graphics.guiWidth() >= 420) {\n"
    "            // Vanilla/NeoForge boss bars own the top-center lane. Keep the directional tracker\n"
    "            // in the upper-right on normal desktop GUI widths so multiple Mythics cannot cover it.\n"
    "            left = Math.max(6, preferredLeft);\n"
    "        } else {\n"
    "            // On narrow GUI scales there is not enough horizontal separation from a boss bar.\n"
    "            // Fall below the normal multi-boss stack instead of fighting for the center lane.\n"
    "            left = Math.max(6, (graphics.guiWidth() - width) / 2);\n"
    "            top = 78;\n"
    "        }\n",
    "mythic tracker boss-bar-safe placement",
)
HUD.write_text(hud, encoding="utf-8")

main = MAIN.read_text(encoding="utf-8")
main = replace_once(main, 'public static final String VERSION = "0.61.11-alpha.1";', 'public static final String VERSION = "0.61.12-alpha.1";', "source version")
MAIN.write_text(main, encoding="utf-8")

gradle = GRADLE.read_text(encoding="utf-8")
gradle = replace_once(gradle, "mod_version=0.61.11-alpha.1", "mod_version=0.61.12-alpha.1", "gradle version")
GRADLE.write_text(gradle, encoding="utf-8")

verify = VERIFY.read_text(encoding="utf-8")
verify = replace_once(verify, 'require("mod_version=0.61.11-alpha.1" in props, "Survival Ascension version drift")', 'require("mod_version=0.61.12-alpha.1" in props, "Survival Ascension version drift")', "verifier gradle version")
verify = replace_once(verify, 'require(\'VERSION = "0.61.11-alpha.1"\' in main, "source version drift")', 'require(\'VERSION = "0.61.12-alpha.1"\' in main, "source version drift")', "verifier source version")
anchor = 'require("mobility_action\\\", InputConstants.KEY_V" not in client, "old V dash default returned")\n'
addition = (
    anchor
    + '\nhud = text(JAVA / "client/SkillHudOverlay.java")\n'
    + 'require("graphics.guiWidth() - width - rightMargin" in hud, "Mythic tracker is not right-edge anchored")\n'
    + 'require("graphics.guiWidth() >= 420" in hud, "Mythic tracker desktop boss-bar separation missing")\n'
    + 'require("top = 78" in hud, "Mythic tracker narrow-screen boss-bar fallback missing")\n'
)
verify = replace_once(verify, anchor, addition, "HUD regression assertions")
verify = replace_once(
    verify,
    'print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.11 X dash + pacing/runtime invariants")',
    'print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.12 Mythic HUD + X dash + pacing/runtime invariants")',
    "verifier pass text",
)
VERIFY.write_text(verify, encoding="utf-8")

changelog = CHANGELOG.read_text(encoding="utf-8")
section = (
    "# Changelog\n\n"
    "## 0.61.12-alpha.1\n"
    "- Moved the Mythic III directional tracker out of the vanilla/NeoForge top-center boss-bar lane on normal desktop GUI widths, preventing two or more simultaneous Mythic boss bars from covering the arrow/distance HUD.\n"
    "- Added a narrow-GUI fallback below the normal stacked boss-bar region instead of forcing the tracker into an overlapping top-center slot.\n"
    "- No encounter authority, Mythic targeting, boss-bar ownership, combat balance, packets or SavedData changed; this patch is client HUD placement only.\n\n"
)
changelog = replace_once(changelog, "# Changelog\n\n", section, "changelog header")
CHANGELOG.write_text(changelog, encoding="utf-8")

print("SURVIVAL ASCENSION 0.61.12 MYTHIC HUD PATCH APPLIED")
