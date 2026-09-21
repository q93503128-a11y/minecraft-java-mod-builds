#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    mastery = read("VillageRoleMasterySystem.java")
    role = read("VillageRoleSkillSystem.java")
    ability = read("VillageRoleAbilitySystem.java")
    rpg = read("VillageRpgSystem.java")
    ui = read("VillageUiController.java")

    # Lv.60+ has five real mastery milestones without a new saved progression currency.
    for level in (90, 150, 210, 270, 300):
        assert f"= {level};" in mastery
    assert "rankForLevel" in mastery
    assert "VillageCouncilState.levelOf" in mastery
    assert "VillageRoleProgressData" not in mastery
    assert "VillageSkillTreeData" not in mastery

    # The build decision is the existing two-skill loadout.
    assert "equippedSkill(player, 0)" in mastery
    assert "equippedSkill(player, 1)" in mastery
    assert "first.promotionTier() == second.promotionTier()" in mastery
    assert "FOCUSED" in mastery and "MIXED" in mastery
    assert "previous != skill" in mastery

    # Focused pairs strengthen effects; mixed pairs extend the chain/refund instead.
    assert "style == PairStyle.FOCUSED ? 1.25f : 1.0f" in mastery
    assert "style == PairStyle.MIXED ? 2L : 0L" in mastery
    assert "if (style == PairStyle.MIXED) refund++;" in mastery

    # Each role owns a distinct combat behavior rather than a shared flat-stat proc.
    for token in (
        "triggerVanguard",
        "armRangerFocus",
        "scheduleMasteryEcho",
        "triggerLuminar",
        "triggerWarden",
    ):
        assert token in mastery
    assert "MobEffects.WEAKNESS" in mastery
    assert "MobEffects.GLOWING" in mastery and "MobEffects.SLOWNESS" in mastery
    assert "MobEffects.ABSORPTION" in mastery
    assert "VillageRaidSystem.tauntEnemies" in mastery
    assert "MobEffects.RESISTANCE" in mastery

    # Runtime connection: mastery modifies resolved skill use and stays above cooldown floor.
    assert "VillageRoleMasterySystem.prepareCast(player, role, skill)" in role
    assert "* mastery.powerMultiplier()" in role
    assert "* mastery.durationMultiplier()" in role
    assert "VillageRoleMasterySystem.finishCast(" in role
    assert "effectiveCooldownSeconds(player, role, skill) - mastery.cooldownRefundSeconds()" in role
    assert "Math.round(skill.baseCooldownSeconds() * 0.20f)" in role

    # Arcanist echoes reuse the non-recursive ARCANE_ECHO path.
    assert "public static void scheduleMasteryEcho" in ability
    assert "ActionKind.ARCANE_ECHO" in ability
    assert "replayingEcho = true;" in ability

    # Ranger focus is consumed once by the normal projectile damage path.
    assert "consumeRangerProjectileMultiplier(attacker, masteryTarget)" in rpg
    assert "RANGER_FOCUS.remove(player.getUUID())" in mastery

    # Combat transient state is reset and the existing role screens explain the mastery state.
    assert "VillageRoleMasterySystem.reset();" in rpg
    assert "VillageRoleMasterySystem.summary(player, role)" in ui
    assert "서로 다른 두 기술을 연계하면 Lv.90+ 전투 숙련이 발동" in ui

    print("[PASS] Lv.90/150/210/270/300 create five late-game mastery milestones")
    print("[PASS] existing two-skill loadouts create focused vs mixed mastery builds")
    print("[PASS] all five roles gain distinct non-flat mastery behavior")
    print("[PASS] mastery respects cooldown floors and avoids echo/projectile multiplication loops")


if __name__ == "__main__":
    main()
