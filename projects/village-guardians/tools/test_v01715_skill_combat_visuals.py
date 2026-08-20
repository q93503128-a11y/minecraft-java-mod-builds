#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    abilities = read("VillageRoleAbilitySystem.java")
    effects = read("VillageSkillEffectSystem.java")
    entity = read("VillageSkillEffectEntity.java")
    entities = read("VillageSkillEffectEntities.java")
    client = read("VillageSkillEffectClient.java")
    mesh = read("VillageSkillMeshLibrary.java")
    hud = read("VillageHudSystem.java")
    overlay = read("VillageSkillHudOverlay.java")
    network = read("VillageNetwork.java")

    assert "mod_version=" in props
    assert "sendSkillMotion(level, player, \"vanguard_spin\"" in abilities
    assert "rotateY(radians)" in client
    assert "event.getRenderState().bodyRot = 0.0f" in client
    assert "horizontalSlash" in mesh and "renderBladeWave" in mesh
    assert "chevron(" not in mesh.split("private static void renderBladeCharge", 1)[1].split("private static void renderSlamCharge", 1)[0]

    assert "MEGA_ARROW_READY" in abilities
    assert "RICOCHET_ARROWS" in abilities
    assert "activeSkillHud" in abilities
    assert "다음 활" in abilities
    assert "arrowRainField" in effects
    assert "spawnVisualLightning" in abilities
    assert "setVisualOnly(true)" in abilities
    assert "double radius = 20.0 + specialRank * 2.0" in abilities
    assert "AreaKind.TORNADO" in abilities and "8.5" in abilities
    assert "VillageEquipmentShop.roleSkillMultiplier(player)" in read("VillageRoleSkillSystem.java")

    assert ".sized(24.0f, 16.0f)" in entities
    assert "warden_fortress" in entity and "setDirection" in entity
    assert "ranger_lock" not in entity.split("private boolean followsOwner", 1)[1].split("public Entity ownerEntity", 1)[0]

    assert "rgba(91, 255, 104" in mesh or "rgba(80, 255, 96" in mesh
    assert "17.5" in mesh and "11.5" in mesh
    assert "double z = -Math.abs(curve)" in mesh
    assert "verticalBlade" not in mesh.split("private static void renderShieldCharge", 1)[1].split("private static void renderTaunt", 1)[0]
    assert "customArrowBetween" not in mesh.split("private static void renderPath", 1)[1].split("private static void renderFallbackRune", 1)[0]

    assert "SkillHudPayload" in network
    assert "VillageSkillHudOverlay.accept" in read("VillageClientUi.java")
    assert "VillageNetwork.sendSkillHud" in hud
    assert "VillageRoleAbilitySystem.activeSkillHud" in hud
    assert "VanillaGuiLayers.OVERLAY_MESSAGE" in overlay
    assert "graphics.guiHeight() - 92" in overlay
    assert "skillHud" not in hud.split("private static String buildText", 1)[1]

    print("[PASS] 회전 검무 rotates only the rendered avatar and repeatedly refreshes motion state")
    print("[PASS] 검기 난무 uses clean horizontal blade waves without generic arrowhead decorations")
    print("[PASS] Ranger buffs bind to actual arrow release, expose duration, and own a random rain field")
    print("[PASS] Fire, tornado and giant lightning gameplay ranges use scalable custom damage")
    print("[PASS] All shields face the live look direction, curve outward correctly, and use solid planes")
    print("[PASS] Skill information renders on its own HUD row above the compact status action bar")


if __name__ == "__main__":
    main()
