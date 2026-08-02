#!/usr/bin/env python3
"""Normalize duplicate/mismatched UI targets before the main v0.17.2 patch.

This script is intentionally safe to run repeatedly in CI and on already-patched
source trees.
"""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def replace_all(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old in text:
        path.write_text(text.replace(old, new), encoding="utf-8")
    elif new not in text:
        raise RuntimeError(f"{path.name}: compatibility target not found")


replace_all(
    JAVA / "VillageSkillTreeScreen.java",
    "int top = Math.max(112, height - 88);",
    "int top = Math.max(132, height - 108);",
)
replace_all(
    JAVA / "VillageShopScreen.java",
    "int listWidth = clamp((right - left) * 35 / 100, 190, 310);",
    "int listWidth = clamp((right - left) * 24 / 100, 118, 198);",
)
replace_all(
    JAVA / "VillageRoleProgressScreen.java",
    "savedZoom = 1.0; savedPanX = 0; savedPanY = 0;",
    "savedPanX = 0; savedPanY = 0; savedZoom = 0.86;",
)
replace_all(
    JAVA / "VillageRoleProgressScreen.java",
    """    private enum Branch {
        DURATION, POWER, SPECIAL;
        static Branch parse(String value) {
""",
    """    private enum Branch {
        DURATION, POWER, SPECIAL;

        String displayName() {
            return switch (this) {
                case DURATION -> "지속";
                case POWER -> "위력";
                case SPECIAL -> "특수";
            };
        }

        static Branch parse(String value) {
""",
)

print("[PATCHED] v0.17.2 compatibility prepatch")
