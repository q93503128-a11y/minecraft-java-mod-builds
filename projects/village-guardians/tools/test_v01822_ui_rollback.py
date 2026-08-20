#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def require(label: str, condition: bool) -> None:
    if not condition:
        raise AssertionError(label)
    print(f"[PASS] {label}")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    hud = read("VillageHudSystem.java")
    main_hud = read("VillageMainHudOverlay.java")
    skill_hud = read("VillageSkillHudOverlay.java")
    structure = read("VillageStructureHud.java")
    command = read("VillageCommandCenterScreen.java")
    town = read("VillageTownHallGridScreen.java")
    raid = read("VillageRaidSystem.java")
    effects = read("VillageDefenseEffectSystem.java")
    turret = read("VillagePlacedTurretSystem.java")
    segment = read("VillageSiegeSegmentSystem.java")
    mesh = read("VillageSkillMeshLibrary.java")
    merc = read("VillageMercenarySystem.java")
    research = read("VillageDefenseResearchSystem.java")
    consumables = read("VillageCombatConsumableSystem.java")

    require("version is v0.18.22-alpha.1", "mod_version=0.18.22-alpha.1" in props)

    all_java = "\n".join(p.read_text(encoding="utf-8") for p in JAVA.glob("*.java"))
    require("rejected centralized defense HUD frame and theme are physically removed",
            not (JAVA / "VillageDefenseHudFrame.java").exists()
            and not (JAVA / "VillageDefenseUiTheme.java").exists()
            and "VillageDefenseHudFrame" not in all_java
            and "VillageDefenseUiTheme" not in all_java)

    require("main HUD is restored to the compact pre-defense-pass two-line status card",
            "String text = \"\"" in main_hud
            and 'text.split(" §8│ ", -1)' in main_hud
            and "VillageQuickChatSafeScreen.drawDiamond" in main_hud
            and "int maxWidth = Math.min(330" in main_hud
            and "renderFrontPressure" not in main_hud
            and "renderDefenseCard" not in main_hud)

    require("server HUD payload is again human-readable status text rather than a fixed mobile-defense frame",
            "String text = buildText(player);" in hud
            and "private static String buildText" in hud
            and "VillageDefenseHudFrame" not in hud
            and "VillageProgressionSystem.coins(player)" in hud
            and "VillageProgressionSystem.supplies()" in hud)

    require("skill HUD is restored to the low-profile two-slot strip above the vanilla hotbar",
            "Low-profile combat skill HUD" in skill_hud
            and "graphics.guiHeight() - 98" in skill_hud
            and "int gap = 16" in skill_hud
            and "abilityCard" not in skill_hud
            and "READY" not in skill_hud
            and "VillageQuickChatSafeScreen.drawDiamond" in skill_hud)

    require("structure durability bar behavior is restored to the pre-defense-pass raid display",
            "CYCLE_INTERVAL_TICKS = 50" in structure
            and "nextBuilding" in structure
            and "방어 시설 내구도" in structure
            and "시설 피해 경보" not in structure)

    require("command center keeps current functionality but owns its pre-defense-pass local palette",
            "VillageDefenseUiTheme" not in command
            and "private static final int OVERLAY = 0x70070A0D" in command
            and "private static final int CYAN = 0xFF52D9C2" in command
            and "private static final int GOLD = 0xFFFFC65C" in command)

    require("town hall keeps current functionality but owns its pre-defense-pass local palette",
            "VillageDefenseUiTheme" not in town
            and "private static final int OVERLAY = 0x7805090C" in town
            and "private static final int PANEL = 0xF00B1217" in town
            and "private static final int CYAN = 0xFF50D9C1" in town)

    require("rejected HUD-only raid snapshot is removed without removing live raid status",
            "record RaidHudSnapshot" not in raid
            and "hudSnapshot()" not in raid
            and "public static String status()" in raid
            and "ACTIVE_ENEMIES" in raid)

    require("all 0.18.16 world-space defense feedback explicitly survives the UI rollback",
            all(token in effects for token in (
                "turretPlacementPreview", "turretDeployPulse", "turretRepairPulse", "turretUpgradePulse", "breachAlarm"))
            and "VillageDefenseEffectSystem.turretPlacementPreview" in turret
            and "VillageDefenseEffectSystem.breachAlarm" in segment
            and all(token in mesh for token in (
                "turret_placement_preview", "turret_deploy_pulse", "defense_repair_pulse",
                "turret_upgrade_burst", "defense_breach_alarm")))

    require("post-0.18.16 progression systems survive the rollback",
            "MAX_LEVEL = 60" in merc
            and "MAX_LEVEL = 10" in research
            and "VillageCombatConsumableSystem" in consumables)

    rejected = ROOT / "tools/test_v01816_mobile_defense_ui.py"
    require("rejected v0.18.16 mobile-defense UI test is retired", not rejected.exists())

    print("[PASS] v0.18.22 UI rollback keeps later gameplay while restoring the accepted HUD language")


if __name__ == "__main__":
    main()
