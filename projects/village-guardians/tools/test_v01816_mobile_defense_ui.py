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
    frame = read("VillageDefenseHudFrame.java")
    theme = read("VillageDefenseUiTheme.java")
    hud = read("VillageHudSystem.java")
    main_hud = read("VillageMainHudOverlay.java")
    skill_hud = read("VillageSkillHudOverlay.java")
    structure = read("VillageStructureHud.java")
    raid = read("VillageRaidSystem.java")
    command = read("VillageCommandCenterScreen.java")
    town = read("VillageTownHallGridScreen.java")
    effects = read("VillageDefenseEffectSystem.java")
    turret = read("VillagePlacedTurretSystem.java")
    segment = read("VillageSiegeSegmentSystem.java")
    mesh = read("VillageSkillMeshLibrary.java")

    require("version is v0.18.16-alpha.1", "mod_version=0.18.19-alpha.1" in props)
    require("main HUD uses a structured fixed-field frame rather than styled human-text parsing",
            "record VillageDefenseHudFrame" in frame
            and "FIELD_COUNT = 21" in frame
            and "VillageDefenseHudFrame.from(player).encode()" in hud
            and "VillageDefenseHudFrame.decode" in main_hud
            and 'split(" §8│ "' not in main_hud)
    require("authoritative raid snapshot exposes wave enemy countdown and four live pressure regions",
            "record RaidHudSnapshot" in raid
            and "ACTIVE_ENEMIES" in raid
            and "VillageAttackPlanSystem.frontOf(id)" in raid
            and "northPressure" in frame
            and "rearPressure" in frame)
    require("weakest defense compares siege segments and non-wall facilities directly",
            "VillageSiegeSegmentSystem.Segment.values()" in frame
            and "VillageProgressionSystem.Building.values()" in frame
            and "building == VillageProgressionSystem.Building.WALLS" in frame)
    require("combat HUD renders command ribbon weakest-defense bar and four lane pips",
            "renderWideTop" in main_hud
            and "renderCompactTop" in main_hud
            and "renderDefenseCard" in main_hud
            and "renderFrontPressure" in main_hud
            and "VillageDefenseUiTheme.progressBar" in main_hud
            and all(label in main_hud for label in ("북문", "서측", "동측", "후방")))
    require("skill HUD uses two readiness cards above the vanilla hotbar safe zone",
            "abilityCard" in skill_hud
            and 'value.contains("준비")' in skill_hud
            and "cooldownSeconds" in skill_hud
            and "graphics.guiHeight() - 112" in skill_hud
            and "READY" in skill_hud)
    require("structure boss bar is emergency-only and no longer cycles facilities",
            "DAMAGE_FOCUS_TICKS" in structure
            and "CYCLE_INTERVAL_TICKS" not in structure
            and "nextBuilding" not in structure
            and "focusTicks <= 0" in structure)
    require("command center and town hall share the centralized defense visual language",
            "VillageDefenseUiTheme" in command
            and "VillageDefenseUiTheme" in town
            and "VillageDefenseUiTheme" in theme)
    require("turret placement repair and upgrade actions have synchronized world-space feedback",
            "turretPlacementPreview" in effects
            and "turretDeployPulse" in effects
            and "turretRepairPulse" in effects
            and "turretUpgradePulse" in effects
            and "VillageDefenseEffectSystem.turretPlacementPreview" in turret
            and "VillageDefenseEffectSystem.turretDeployPulse" in turret
            and "VillageDefenseEffectSystem.turretRepairPulse" in turret
            and "VillageDefenseEffectSystem.turretUpgradePulse" in turret)
    require("wall breach receives a synchronized defense alarm instead of relying on chat only",
            "breachAlarm" in effects
            and "VillageDefenseEffectSystem.breachAlarm" in segment
            and "defense_breach_alarm" in mesh)
    for token in ("turret_placement_preview", "turret_deploy_pulse", "defense_repair_pulse", "turret_upgrade_burst"):
        require(f"procedural mesh library includes {token}", token in mesh)
    require("mobile-defense theme exposes subdued surfaces and semantic danger colors",
            all(token in theme for token in ("PANEL_SOFT", "PANEL_ACTIVE", "GREEN", "AMBER", "RED", "pressureColor")))

    print("[PASS] v0.18.16 mobile defense HUD and presentation contracts complete")


if __name__ == "__main__":
    main()
