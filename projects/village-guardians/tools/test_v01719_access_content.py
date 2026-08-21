#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    keys = read("VillageClientKeys.java")
    ability = read("VillageRoleAbilitySystem.java")
    controller = read("VillageUiController.java")
    client_ui = read("VillageClientUi.java")
    test_system = read("VillageSkillTestSystem.java")
    password = read("VillageSkillTestPasswordScreen.java")
    intel = read("VillageWaveIntelSystem.java")
    equipment = read("VillageEquipmentShop.java")
    rarity = read("VillageEquipmentRaritySystem.java")
    enemies = read("VillageEnemyArchetypeSystem.java")
    traits = read("VillageWaveTrait.java")
    warfront = read("VillageWarfrontSystem.java")
    raid = read("VillageRaidSystem.java")
    role_skills = read("VillageRoleSkillSystem.java")
    relics = read("VillageRelicSystem.java")
    mercenaries = read("VillageMercenarySystem.java")
    towers = read("VillagePlacedTurretSystem.java")

    assert "mod_version=" in props

    assert "fireOrbContacts" in ability
    assert "target.getBoundingBox().inflate(padding).contains(position)" in ability
    assert "1.55" in ability and "1.95" in ability
    assert "fireOrbContactRadius" not in ability
    print("[PASS] 홍염탄은 소폭 넓어진 여유값과 적별 실제 충돌 상자를 사용합니다")

    for token in ["GLFW.GLFW_KEY_Z", "GLFW.GLFW_KEY_V", "GLFW.GLFW_KEY_B",
                  "GLFW.GLFW_KEY_H", "GLFW.GLFW_KEY_J", "GLFW.GLFW_KEY_K"]:
        assert token in keys, token
    for token in ["GLFW.GLFW_KEY_E", "GLFW.GLFW_KEY_Q", "GLFW.GLFW_KEY_F",
                  "GLFW.GLFW_KEY_P", "GLFW.GLFW_KEY_L", "GLFW.GLFW_KEY_C",
                  "GLFW.GLFW_KEY_V"]:
        assert token in keys, token
    assert "VANILLA_RESERVED" in keys and "migrateBindings" in keys
    assert "!used.add(value)" in keys and "minecraft.options.save()" in keys
    print("[PASS] Z/X/V/H/J/K 안전 기본키와 바닐라·중복 키 자동 이관이 연결됩니다")

    assert '"skill_test_password" -> new VillageSkillTestPasswordScreen(payload)' in client_ui
    assert '"1557".equals(code)' in controller
    assert '"skill_test_password:" + input' in password
    assert "VillageSkillTestSystem.enable(player)" in controller
    assert "test_choose:" not in controller
    assert "Password-gated outdoor arena" in test_system
    print("[PASS] 기술 시험장 진입은 1557 인증을 거치며 구형 시험 액션은 제거됩니다")

    barracks = controller.split("case BARRACKS -> add", 1)[1].split("case STOREHOUSE", 1)[0]
    walls = controller.split("case WALLS -> add", 1)[1].split("case SMITHY", 1)[0]
    assert '"open_wave_intel"' in barracks and '"open_wave_intel"' in walls
    assert "성벽 또는 병영 단말기" in intel
    print("[PASS] 다음 밤 웨이브 정찰은 성벽과 병영 양쪽에서 접근할 수 있습니다")

    offer_block = equipment.split("public enum Offer", 1)[1].split("private final String id", 1)[0]
    offers = re.findall(r"^\s{8}([A-Z][A-Z0-9_]+)\(\"", offer_block, re.M)
    archetype_block = enemies.split("public enum Archetype", 1)[1]
    archetypes = re.findall(r"^\s{8}([A-Z][A-Z0-9_]+)\(", archetype_block, re.M)
    trait_block = traits.split("public enum VillageWaveTrait", 1)[1].split("private static final", 1)[0]
    wave_traits = re.findall(r"^\s{4}([A-Z][A-Z0-9_]+)\(", trait_block, re.M)
    skill_block = role_skills.split("public enum ActiveSkill", 1)[1]
    active_skills = re.findall(r"^\s{8}([A-Z][A-Z0-9_]+)\(", skill_block, re.M)
    turret_block = towers.split("public enum TurretType", 1)[1].split("private final String id", 1)[0]
    turret_types = re.findall(r"^\s{8}([A-Z][A-Z0-9_]+)\(\"", turret_block, re.M)

    assert len(offers) == 24, offers
    assert len(archetypes) == 14, archetypes
    assert len(wave_traits) == 12, wave_traits
    assert len(active_skills) == 20, active_skills
    assert len(turret_types) == 10, turret_types
    assert "Rarity.LEGENDARY" in rarity and "MAX_ENHANCEMENT = 5" in rarity
    assert relics.count("public static final Relic ") == 0 or "WAR_SIGIL" in relics
    assert "BASTION" in mercenaries and "MEDIC" in mercenaries
    assert "BALLISTA" in towers and "ANTI_AIR" in towers and "BEACON" in towers
    assert "VillageTowerSpecializationSystem" not in towers
    assert "The campaign never hard-ends" in warfront
    assert "return Math.min(8, 3 + Math.max(0, day - 1) / 2);" in raid
    print("[PASS] 콘텐츠 감사: 장비 24, 적 14, 웨이브 특성 12, 액티브 기술 20, 현행 배치 포탑 10종")

    audit = (ROOT / "CONTENT-AUDIT-v0.18.0.md").read_text(encoding="utf-8")
    for token in ["일반 적 병과 10종", "기본 보스 4종", "웨이브 특성 12종",
                  "포탑 4종", "용병 4병과", "유물 11종", "고정 마지막 날은 없으며 무한 진행"]:
        assert token in audit, token
    print("[PASS] v0.18.0 당시 수량과 장기 반복 구간은 역사적 콘텐츠 감사 문서로 유지됩니다")


if __name__ == "__main__":
    main()
